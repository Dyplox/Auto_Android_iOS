package com.reto.qa.config;

import io.appium.java_client.remote.AutomationName;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Supported platforms. Each one knows its W3C {@code platformName} and the Appium driver that automates it.
 */
public enum Platform {

    /** Android, automated with the UiAutomator2 driver. */
    ANDROID("Android", AutomationName.ANDROID_UIAUTOMATOR2),
    /** iOS, automated with the XCUITest driver. */
    IOS("iOS", AutomationName.IOS_XCUI_TEST);

    private final String w3cName;
    private final String automationName;

    Platform(String w3cName, String automationName) {
        this.w3cName = w3cName;
        this.automationName = automationName;
    }

    /**
     * Returns the value sent as the W3C {@code platformName} capability.
     *
     * @return {@code Android} or {@code iOS}
     */
    public String w3cName() {
        return w3cName;
    }

    /**
     * Returns the value sent as the {@code appium:automationName} capability.
     *
     * @return the Appium driver name for this platform
     */
    public String automationName() {
        return automationName;
    }

    /**
     * Lower-case key used in configuration files and overrides.
     *
     * @return {@code android} or {@code ios}
     */
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * Parses a platform name, ignoring case and surrounding whitespace.
     *
     * @param value platform name, for example {@code android} or {@code IOS}
     * @return the matching platform
     * @throws IllegalArgumentException if the value is blank or not supported
     */
    public static Platform from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Platform must not be blank. Valid values: " + validValues());
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(platform -> platform.name().equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unsupported platform: '" + value + "'. Valid values: " + validValues()));
    }

    private static String validValues() {
        return Arrays.stream(values()).map(Platform::key).collect(Collectors.joining(", "));
    }
}
