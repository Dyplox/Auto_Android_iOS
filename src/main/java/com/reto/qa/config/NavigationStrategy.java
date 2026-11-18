package com.reto.qa.config;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/** How tests move between screens ({@code navigation.strategy}). */
public enum NavigationStrategy {

    /** Taps the tab bar, like a real user (default). */
    TABBAR,
    /** Opens the screen directly with a deep link (faster). */
    DEEPLINK;

    /**
     * Parses a strategy name, ignoring case and surrounding whitespace.
     *
     * @param value strategy name, for example {@code tabbar}
     * @return the matching strategy
     * @throws ConfigurationException if the value is not a known strategy
     */
    public static NavigationStrategy from(String value) {
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(strategy -> strategy.name().equals(normalized))
                .findFirst()
                .orElseThrow(() -> new ConfigurationException("Unsupported navigation.strategy: '" + value
                        + "'. Valid values: " + Arrays.stream(values())
                                .map(strategy -> strategy.name().toLowerCase(Locale.ROOT))
                                .collect(Collectors.joining(", "))));
    }
}
