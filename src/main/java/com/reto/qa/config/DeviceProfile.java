package com.reto.qa.config;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Objeto de valor inmutable que describe el dispositivo y la app bajo prueba.
 * Se construye exclusivamente con {@link Builder} (patrón Builder) y valida sus invariantes al construirse.
 */
public final class DeviceProfile {

    private final Platform platform;
    private final String deviceName;
    private final String platformVersion;
    private final String udid;
    private final String app;
    private final String appId;
    private final Duration newCommandTimeout;
    private final boolean noReset;
    private final Map<String, Object> extraCapabilities;

    private DeviceProfile(Builder builder) {
        this.platform = builder.platform;
        this.deviceName = builder.deviceName;
        this.platformVersion = builder.platformVersion;
        this.udid = builder.udid;
        this.app = builder.app;
        this.appId = builder.appId;
        this.newCommandTimeout = builder.newCommandTimeout;
        this.noReset = builder.noReset;
        Map<String, Object> capabilities = new LinkedHashMap<>(builder.extraCapabilities);
        builder.vendorOptions.forEach((vendor, options) ->
                capabilities.put(vendor, Collections.unmodifiableMap(new LinkedHashMap<>(options))));
        this.extraCapabilities = Collections.unmodifiableMap(capabilities);
    }

    public static Builder builder(Platform platform) {
        return new Builder(platform);
    }

    public Platform platform() {
        return platform;
    }

    public String deviceName() {
        return deviceName;
    }

    public Optional<String> platformVersion() {
        return Optional.ofNullable(platformVersion);
    }

    public Optional<String> udid() {
        return Optional.ofNullable(udid);
    }

    /** Ruta absoluta o URL de la app (.apk / .app.zip / .ipa). Vacío si la app ya está instalada. */
    public Optional<String> app() {
        return Optional.ofNullable(app);
    }

    /** appPackage en Android, bundleId en iOS. */
    public String appId() {
        return appId;
    }

    public Duration newCommandTimeout() {
        return newCommandTimeout;
    }

    public boolean noReset() {
        return noReset;
    }

    /**
     * Capabilities adicionales ya con prefijo de vendor (p. ej. {@code appium:appWaitActivity}); las opciones de un
     * proveedor cloud llegan agrupadas en un mapa (p. ej. {@code bstack:options} o {@code sauce:options}).
     */
    public Map<String, Object> extraCapabilities() {
        return extraCapabilities;
    }

    @Override
    public String toString() {
        return platform.w3cName() + " | " + deviceName
                + platformVersion().map(v -> " " + v).orElse("")
                + udid().map(u -> " (" + u + ")").orElse("")
                + " | " + appId;
    }

    public static final class Builder {

        private static final Duration DEFAULT_NEW_COMMAND_TIMEOUT = Duration.ofSeconds(240);

        private final Platform platform;
        private String deviceName;
        private String platformVersion;
        private String udid;
        private String app;
        private String appId;
        private Duration newCommandTimeout = DEFAULT_NEW_COMMAND_TIMEOUT;
        private boolean noReset;
        private final Map<String, Object> extraCapabilities = new LinkedHashMap<>();
        private final Map<String, Map<String, Object>> vendorOptions = new LinkedHashMap<>();

        private Builder(Platform platform) {
            this.platform = Objects.requireNonNull(platform, "platform");
        }

        public Builder deviceName(String deviceName) {
            this.deviceName = blankToNull(deviceName);
            return this;
        }

        public Builder platformVersion(String platformVersion) {
            this.platformVersion = blankToNull(platformVersion);
            return this;
        }

        public Builder udid(String udid) {
            this.udid = blankToNull(udid);
            return this;
        }

        public Builder app(String app) {
            this.app = blankToNull(app);
            return this;
        }

        public Builder appId(String appId) {
            this.appId = blankToNull(appId);
            return this;
        }

        public Builder newCommandTimeout(Duration newCommandTimeout) {
            this.newCommandTimeout = Objects.requireNonNull(newCommandTimeout, "newCommandTimeout");
            return this;
        }

        public Builder noReset(boolean noReset) {
            this.noReset = noReset;
            return this;
        }

        /**
         * Añade una capability. Sin prefijo de vendor se asume {@code appium:}. Con la forma
         * {@code vendor:opciones.clave} (p. ej. {@code bstack:options.userName}) se agrupa en el mapa
         * {@code vendor:opciones}, como exigen BrowserStack ({@code bstack:options}) o Sauce Labs ({@code sauce:options}).
         */
        public Builder capability(String name, Object value) {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(value, "value");
            String key = name.contains(":") ? name : "appium:" + name;
            int optionSeparator = key.indexOf('.', key.indexOf(':'));
            if (optionSeparator > 0) {
                String vendor = key.substring(0, optionSeparator);
                vendorOptions.computeIfAbsent(vendor, k -> new LinkedHashMap<>()).put(key.substring(optionSeparator + 1), value);
            } else {
                extraCapabilities.put(key, value);
            }
            return this;
        }

        public DeviceProfile build() {
            if (deviceName == null) {
                throw new ConfigurationException("device.name es obligatorio para " + platform.w3cName());
            }
            if (appId == null) {
                throw new ConfigurationException(
                        "app.id es obligatorio (appPackage en Android / bundleId en iOS) para " + platform.w3cName());
            }
            if (platform == Platform.IOS && udid == null && platformVersion == null) {
                throw new ConfigurationException("En iOS define platform.version o device.udid para que XCUITest "
                        + "pueda elegir el simulador. Lista los disponibles con: xcrun simctl list devices available");
            }
            vendorOptions.keySet().stream().filter(extraCapabilities::containsKey).findFirst().ifPresent(vendor -> {
                throw new ConfigurationException("'" + vendor + "' está definida como valor simple y como grupo de opciones");
            });
            return new DeviceProfile(this);
        }

        private static String blankToNull(String value) {
            return value == null || value.isBlank() ? null : value.trim();
        }
    }
}
