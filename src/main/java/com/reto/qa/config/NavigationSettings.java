package com.reto.qa.config;

import java.util.Objects;

/**
 * How tests navigate between screens.
 *
 * @param strategy       navigation strategy
 * @param deepLinkScheme deep link scheme including {@code ://}, for example {@code wdio://}
 */
public record NavigationSettings(NavigationStrategy strategy, String deepLinkScheme) {

    /**
     * Validates the settings.
     *
     * @param strategy       navigation strategy
     * @param deepLinkScheme deep link scheme
     * @throws ConfigurationException if the scheme does not end with {@code ://}
     */
    public NavigationSettings {
        Objects.requireNonNull(strategy, "strategy");
        Objects.requireNonNull(deepLinkScheme, "deepLinkScheme");
        if (!deepLinkScheme.endsWith("://")) {
            throw new ConfigurationException(
                    "app.deeplink.scheme must end with '://' (for example wdio://) but is '" + deepLinkScheme + "'");
        }
    }

    /**
     * Reads {@code navigation.strategy} and {@code app.deeplink.scheme}.
     *
     * @param config configuration for one platform and environment
     * @return the validated settings
     * @throws ConfigurationException if a value is missing or invalid
     */
    public static NavigationSettings from(FrameworkConfig config) {
        return new NavigationSettings(
                NavigationStrategy.from(config.get("navigation.strategy")), config.get("app.deeplink.scheme"));
    }
}
