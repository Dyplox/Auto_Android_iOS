package com.reto.qa.config;

import java.util.Objects;

/**
 * Typed, validated view of the framework settings. It is built once when a session starts, so a configuration error
 * shows up immediately with a clear message, instead of as a string parsing error in the middle of a test.
 *
 * <p>Device and app values ({@code device.*}, {@code app.*}, {@code capability.*}, {@code cloud.*}) are turned into a
 * {@link DeviceProfile} by {@link DeviceProfileFactory}; this record holds everything else.</p>
 *
 * @param server     where the Appium server is
 * @param timeouts   wait and session timeouts
 * @param navigation how tests move between screens
 * @param noReset    whether Appium keeps the app state when the session starts ({@code session.no-reset})
 */
public record FrameworkSettings(ServerSettings server, TimeoutSettings timeouts, NavigationSettings navigation,
        boolean noReset) {

    /**
     * Validates that no group is missing.
     *
     * @param server     where the Appium server is
     * @param timeouts   wait and session timeouts
     * @param navigation how tests move between screens
     * @param noReset    the {@code session.no-reset} flag
     */
    public FrameworkSettings {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(timeouts, "timeouts");
        Objects.requireNonNull(navigation, "navigation");
    }

    /**
     * Reads and validates every framework setting.
     *
     * @param config configuration for one platform and environment
     * @return the validated settings
     * @throws ConfigurationException if a value is missing or invalid
     */
    public static FrameworkSettings from(FrameworkConfig config) {
        return new FrameworkSettings(
                ServerSettings.from(config),
                TimeoutSettings.from(config),
                NavigationSettings.from(config),
                config.getBoolean("session.no-reset"));
    }
}
