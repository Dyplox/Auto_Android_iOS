package com.reto.qa.config;

import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable value object that describes the device and the app under test.
 * It can only be created through its {@link Builder} (Builder pattern), which validates the invariants.
 */
public final class DeviceProfile {

    private final Platform platform;
    private final String deviceName;
    private final @Nullable String platformVersion;
    private final @Nullable String udid;
    private final @Nullable String app;
    private final String appId;
    private final Duration newCommandTimeout;
    private final boolean noReset;
    private final Map<String, Object> extraCapabilities;

    private DeviceProfile(Builder builder, String deviceName, String appId) {
        this.platform = builder.platform;
        this.deviceName = deviceName;
        this.platformVersion = builder.platformVersion;
        this.udid = builder.udid;
        this.app = builder.app;
        this.appId = appId;
        this.newCommandTimeout = builder.newCommandTimeout;
        this.noReset = builder.noReset;
        Map<String, Object> capabilities = new LinkedHashMap<>(builder.extraCapabilities);
        builder.vendorOptions.forEach((vendor, options) -> capabilities.put(vendor,
                Collections.unmodifiableMap(new LinkedHashMap<>(options))));
        this.extraCapabilities = Collections.unmodifiableMap(capabilities);
    }

    /**
     * Starts building a profile.
     *
     * @param platform target platform
     * @return a new builder
     */
    public static Builder builder(Platform platform) {
        return new Builder(platform);
    }

    /**
     * Returns the target platform.
     *
     * @return the platform
     */
    public Platform platform() {
        return platform;
    }

    /**
     * Returns the device name (emulator, simulator or cloud device).
     *
     * @return the device name
     */
    public String deviceName() {
        return deviceName;
    }

    /**
     * Returns the OS version, if one was configured.
     *
     * @return the platform version, or empty
     */
    public Optional<String> platformVersion() {
        return Optional.ofNullable(platformVersion);
    }

    /**
     * Returns the device UDID, if one was configured or auto-selected.
     *
     * @return the UDID, or empty
     */
    public Optional<String> udid() {
        return Optional.ofNullable(udid);
    }

    /**
     * Returns the absolute path or remote reference of the app (.apk / .app.zip / .ipa / URL / cloud id).
     *
     * @return the app, or empty when the app is already installed
     */
    public Optional<String> app() {
        return Optional.ofNullable(app);
    }

    /**
     * Returns the app identifier: {@code appPackage} on Android, {@code bundleId} on iOS.
     *
     * @return the app identifier
     */
    public String appId() {
        return appId;
    }

    /**
     * Returns how long Appium waits for a new command before ending the session.
     *
     * @return the new-command timeout
     */
    public Duration newCommandTimeout() {
        return newCommandTimeout;
    }

    /**
     * Returns whether the app state is kept between sessions.
     *
     * @return the {@code noReset} flag
     */
    public boolean noReset() {
        return noReset;
    }

    /**
     * Returns the additional capabilities, already vendor-prefixed (for example {@code appium:appWaitActivity}).
     * Cloud provider options are grouped in a map (for example {@code bstack:options} or {@code sauce:options}).
     *
     * @return an unmodifiable map of capability name to value
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

    /** Fluent builder for {@link DeviceProfile}. Blank values are treated as absent. */
    public static final class Builder {

        private static final Duration DEFAULT_NEW_COMMAND_TIMEOUT = Duration.ofMinutes(4);

        private final Platform platform;
        private @Nullable String deviceName;
        private @Nullable String platformVersion;
        private @Nullable String udid;
        private @Nullable String app;
        private @Nullable String appId;
        private Duration newCommandTimeout = DEFAULT_NEW_COMMAND_TIMEOUT;
        private boolean noReset;
        private final Map<String, Object> extraCapabilities = new LinkedHashMap<>();
        private final Map<String, Map<String, Object>> vendorOptions = new LinkedHashMap<>();

        private Builder(Platform platform) {
            this.platform = Objects.requireNonNull(platform, "platform");
        }

        /**
         * Sets the device name (required).
         *
         * @param deviceName emulator, simulator or cloud device name; blank or {@code null} clears it
         * @return this builder
         */
        public Builder deviceName(@Nullable String deviceName) {
            this.deviceName = blankToNull(deviceName);
            return this;
        }

        /**
         * Sets the OS version.
         *
         * @param platformVersion version such as {@code 16} or {@code 26.0}; blank or {@code null} clears it
         * @return this builder
         */
        public Builder platformVersion(@Nullable String platformVersion) {
            this.platformVersion = blankToNull(platformVersion);
            return this;
        }

        /**
         * Sets the device UDID.
         *
         * @param udid device or simulator UDID; blank or {@code null} clears it
         * @return this builder
         */
        public Builder udid(@Nullable String udid) {
            this.udid = blankToNull(udid);
            return this;
        }

        /**
         * Sets the app to install.
         *
         * @param app absolute path, URL or cloud reference; blank or {@code null} clears it
         * @return this builder
         */
        public Builder app(@Nullable String app) {
            this.app = blankToNull(app);
            return this;
        }

        /**
         * Sets the app identifier (required).
         *
         * @param appId {@code appPackage} on Android, {@code bundleId} on iOS; blank or {@code null} clears it
         * @return this builder
         */
        public Builder appId(@Nullable String appId) {
            this.appId = blankToNull(appId);
            return this;
        }

        /**
         * Sets the Appium new-command timeout (default 240 seconds).
         *
         * @param newCommandTimeout the timeout
         * @return this builder
         */
        public Builder newCommandTimeout(Duration newCommandTimeout) {
            this.newCommandTimeout = Objects.requireNonNull(newCommandTimeout, "newCommandTimeout");
            return this;
        }

        /**
         * Sets whether the app state is kept between sessions.
         *
         * @param noReset the {@code noReset} flag
         * @return this builder
         */
        public Builder noReset(boolean noReset) {
            this.noReset = noReset;
            return this;
        }

        /**
         * Adds a capability. Without a vendor prefix, {@code appium:} is assumed. A name of the form
         * {@code vendor:options.key} (for example {@code bstack:options.userName}) is grouped into the
         * {@code vendor:options} map, as BrowserStack ({@code bstack:options}) and Sauce Labs ({@code sauce:options})
         * require.
         *
         * @param name  capability name
         * @param value capability value
         * @return this builder
         */
        public Builder capability(String name, Object value) {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(value, "value");
            String key = name.contains(":") ? name : "appium:" + name;
            int optionSeparator = key.indexOf('.', key.indexOf(':'));
            if (optionSeparator > 0) {
                String vendor = key.substring(0, optionSeparator);
                vendorOptions.computeIfAbsent(vendor, k -> new LinkedHashMap<>())
                        .put(key.substring(optionSeparator + 1), value);
            } else {
                extraCapabilities.put(key, value);
            }
            return this;
        }

        /**
         * Validates the invariants and builds the profile.
         *
         * @return the immutable profile
         * @throws ConfigurationException if a required value is missing or the capabilities conflict
         */
        public DeviceProfile build() {
            String name = deviceName;
            if (name == null) {
                throw new ConfigurationException("device.name is required for " + platform.w3cName());
            }
            String id = appId;
            if (id == null) {
                throw new ConfigurationException(
                        "app.id is required (appPackage on Android / bundleId on iOS) for " + platform.w3cName());
            }
            if (platform == Platform.IOS && udid == null && platformVersion == null) {
                throw new ConfigurationException("On iOS, set platform.version or device.udid so XCUITest "
                        + "can pick the simulator. List the available ones with: xcrun simctl list devices available");
            }
            vendorOptions.keySet().stream().filter(extraCapabilities::containsKey).findFirst().ifPresent(vendor -> {
                throw new ConfigurationException(
                        "'" + vendor + "' is defined both as a single value and as an options group");
            });
            return new DeviceProfile(this, name, id);
        }

        private static @Nullable String blankToNull(@Nullable String value) {
            return value == null || value.isBlank() ? null : value.trim();
        }
    }
}
