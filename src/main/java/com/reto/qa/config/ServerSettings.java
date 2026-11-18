package com.reto.qa.config;

import java.net.URL;
import java.util.Objects;

/**
 * Where the Appium server is.
 *
 * @param autostart {@code true} to start a local server on a free port; {@code false} to use {@link #url()}
 * @param url       URL of a server that is already running (local Appium or a cloud hub); ignored with autostart
 */
public record ServerSettings(boolean autostart, URL url) {

    /**
     * Validates the settings.
     *
     * @param autostart whether to start a local server
     * @param url       URL of an existing server
     */
    public ServerSettings {
        Objects.requireNonNull(url, "url");
    }

    /**
     * Reads {@code appium.server.autostart} and {@code appium.server.url}.
     *
     * @param config configuration for one platform and environment
     * @return the validated settings
     * @throws ConfigurationException if a value is missing or invalid
     */
    public static ServerSettings from(FrameworkConfig config) {
        return new ServerSettings(config.getBoolean("appium.server.autostart"), config.getUrl("appium.server.url"));
    }
}
