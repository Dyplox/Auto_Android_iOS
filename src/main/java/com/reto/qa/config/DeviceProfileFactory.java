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
 * Turns the configuration ({@link FrameworkConfig}) into a {@link DeviceProfile}: maps the properties, resolves the
 * app path (failing fast if the file does not exist), picks a local iOS simulator when needed and, in the cloud,
 * names the session.
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

    /** Creates a factory that resolves app paths against the working directory and looks up real simulators. */
    public DeviceProfileFactory() {
        this(Paths.get(System.getProperty("user.dir")), new IosSimulatorLocator());
    }

    /**
     * Creates a factory with explicit collaborators (used by unit tests).
     *
     * @param projectDir       directory that relative app paths are resolved against
     * @param simulatorLocator strategy that picks an iOS simulator
     */
    DeviceProfileFactory(Path projectDir, IosSimulatorLocator simulatorLocator) {
        this.projectDir = Objects.requireNonNull(projectDir, "projectDir");
        this.simulatorLocator = Objects.requireNonNull(simulatorLocator, "simulatorLocator");
    }

    /**
     * Builds the device profile without a cloud session name.
     *
     * @param config configuration for one platform and environment
     * @return the validated device profile
     * @throws ConfigurationException if a required property is missing or invalid
     */
    public DeviceProfile create(FrameworkConfig config) {
        return create(config, null);
    }

    /**
     * Builds the device profile.
     *
     * @param config      configuration for one platform and environment
     * @param sessionName name shown for the session on the cloud provider's dashboard (for example the test class);
     *                    ignored when the environment does not define {@code cloud.session-name-option}
     * @return the validated device profile
     * @throws ConfigurationException if the platform is not available in the environment, or a required property is
     *                                missing or invalid
     */
    public DeviceProfile create(FrameworkConfig config, String sessionName) {
        config.find(UNSUPPORTED_REASON).ifPresent(reason -> {
            throw new ConfigurationException(config.platform().w3cName() + " is not available in the '"
                    + config.environment() + "' environment: " + reason);
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
                LOG.info("Auto-selected iOS simulator: {} (iOS {}) {}",
                        simulator.name(), simulator.runtime(), simulator.udid());
                builder.deviceName(simulator.name()).udid(simulator.udid());
            });
        }

        config.withPrefix(CAPABILITY_PREFIX).forEach((name, value) -> builder.capability(name, typed(value)));
        config.find(CLOUD_OPTIONS_CAPABILITY).ifPresent(options -> addCloudOptions(config, builder, options, sessionName));
        return builder.build();
    }

    /**
     * Groups the cloud provider options under their capability ({@code bstack:options}, {@code sauce:options}…):
     * {@code cloud.option.<key>=<value>} is sent as {@code <cloud.options-capability>.<key>}.
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

    /** Local simulators only: in the cloud the provider assigns the device. */
    private static boolean needsSimulatorLookup(FrameworkConfig config) {
        return config.platform() == Platform.IOS
                && config.find(AUTO_SELECT_SIMULATOR).map(Boolean::parseBoolean).orElse(true)
                && config.find("device.udid").isEmpty()
                && config.find("platform.version").isEmpty();
    }

    /**
     * Resolves the {@code app.path} value. A local file (.apk, .zip, .ipa…) is resolved against the project root and
     * must exist. Any other reference is passed through as is: a URL, {@code storage:filename=…} (Sauce Labs),
     * {@code bs://…} or a custom_id (BrowserStack).
     *
     * @param app configured app reference
     * @return the absolute path of a local file, or the reference unchanged
     * @throws ConfigurationException if a local file does not exist
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
            throw new ConfigurationException("App not found at '" + path + "'. "
                    + "Download it with ./scripts/download-apps.sh or adjust app.path");
        }
        return path.toAbsolutePath().toString();
    }

    /**
     * Capabilities arrive as text from the {@code .properties} files; this converts booleans and integers.
     *
     * @param value raw text value
     * @return a {@link Boolean}, an {@link Integer} or the original string
     */
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
