package com.reto.qa.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Traduce la configuración ({@link FrameworkConfig}) a un {@link DeviceProfile}: mapea propiedades, resuelve la
 * ruta de la app (fail fast si no existe), en iOS local elige simulador si hace falta y, en la nube, nombra la sesión.
 */
public final class DeviceProfileFactory {

    static final String CAPABILITY_PREFIX = "capability.";
    static final String UNSUPPORTED_REASON = "unsupported.reason";
    static final String CLOUD_OPTIONS_CAPABILITY = "cloud.options-capability";
    static final String CLOUD_OPTION_PREFIX = "cloud.option.";
    static final String SESSION_NAME_OPTION = "cloud.session-name-option";
    static final String AUTO_SELECT_SIMULATOR = "device.simulator.auto-select";

    private static final Logger LOG = LoggerFactory.getLogger(DeviceProfileFactory.class);
    private static final List<String> LOCAL_APP_EXTENSIONS = List.of(".apk", ".aab", ".zip", ".ipa", ".app");

    private final Path projectDir;
    private final IosSimulatorLocator simulatorLocator;

    public DeviceProfileFactory() {
        this(Paths.get(System.getProperty("user.dir")), new IosSimulatorLocator());
    }

    DeviceProfileFactory(Path projectDir, IosSimulatorLocator simulatorLocator) {
        this.projectDir = Objects.requireNonNull(projectDir, "projectDir");
        this.simulatorLocator = Objects.requireNonNull(simulatorLocator, "simulatorLocator");
    }

    public DeviceProfile create(FrameworkConfig config) {
        return create(config, null);
    }

    /**
     * @param sessionName nombre visible de la sesión en el dashboard del proveedor cloud (p. ej. la clase de prueba);
     *                    se ignora si el ambiente no define {@code cloud.session-name-option}
     */
    public DeviceProfile create(FrameworkConfig config, String sessionName) {
        config.find(UNSUPPORTED_REASON).ifPresent(reason -> {
            throw new ConfigurationException(config.platform().w3cName() + " no está disponible en el ambiente '"
                    + config.environment() + "': " + reason);
        });

        DeviceProfile.Builder builder = DeviceProfile.builder(config.platform())
                .deviceName(config.get("device.name"))
                .platformVersion(config.find("platform.version").orElse(null))
                .udid(config.find("device.udid").orElse(null))
                .app(config.find("app.path").map(this::resolveApp).orElse(null))
                .appId(config.get("app.id"))
                .newCommandTimeout(config.getSeconds("timeout.new-command.seconds"))
                .noReset(config.getBoolean("session.no-reset"));

        if (needsSimulatorLookup(config)) {
            simulatorLocator.locate(config.get("device.name")).ifPresent(simulator -> {
                LOG.info("Simulador iOS elegido automáticamente: {} (iOS {}) {}",
                        simulator.name(), simulator.runtime(), simulator.udid());
                builder.deviceName(simulator.name()).udid(simulator.udid());
            });
        }

        config.withPrefix(CAPABILITY_PREFIX).forEach((name, value) -> builder.capability(name, typed(value)));
        config.find(CLOUD_OPTIONS_CAPABILITY).ifPresent(options -> addCloudOptions(config, builder, options, sessionName));
        return builder.build();
    }

    /**
     * Opciones del proveedor cloud agrupadas en su capability ({@code bstack:options}, {@code sauce:options}…):
     * {@code cloud.option.<clave>=<valor>} se envía como {@code <cloud.options-capability>.<clave>}.
     */
    private static void addCloudOptions(FrameworkConfig config, DeviceProfile.Builder builder, String optionsCapability,
                                        String sessionName) {
        config.withPrefix(CLOUD_OPTION_PREFIX)
                .forEach((name, value) -> builder.capability(optionsCapability + "." + name, typed(value)));
        if (sessionName != null) {
            config.find(SESSION_NAME_OPTION)
                    .ifPresent(option -> builder.capability(optionsCapability + "." + option, sessionName));
        }
    }

    /** Solo en simuladores locales: en la nube el proveedor asigna el dispositivo. */
    private static boolean needsSimulatorLookup(FrameworkConfig config) {
        return config.platform() == Platform.IOS
                && config.find(AUTO_SELECT_SIMULATOR).map(Boolean::parseBoolean).orElse(true)
                && config.find("device.udid").isEmpty()
                && config.find("platform.version").isEmpty();
    }

    /**
     * Un archivo local (.apk, .zip, .ipa…) se resuelve contra la raíz del proyecto y debe existir. Cualquier otra
     * referencia se envía tal cual: URL, {@code storage:filename=…} (Sauce Labs), {@code bs://…} o un custom_id
     * (BrowserStack).
     */
    String resolveApp(String app) {
        String lower = app.toLowerCase(Locale.ROOT);
        boolean remote = lower.startsWith("http://") || lower.startsWith("https://")
                || lower.startsWith("storage:") || lower.startsWith("bs://");
        boolean localFile = LOCAL_APP_EXTENSIONS.stream().anyMatch(lower::endsWith);
        if (remote || !localFile) {
            return app;
        }
        Path path = projectDir.resolve(app).normalize();
        if (!Files.exists(path)) {
            throw new ConfigurationException("No se encontró la app en '" + path + "'. "
                    + "Descárgala con ./scripts/download-apps.sh o ajusta app.path");
        }
        return path.toAbsolutePath().toString();
    }

    /** Las capabilities llegan como texto desde los .properties: convierte booleanos y enteros. */
    static Object typed(String value) {
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.parseBoolean(value);
        }
        if (value.matches("-?\\d{1,9}")) {
            return Integer.parseInt(value);
        }
        return value;
    }
}
