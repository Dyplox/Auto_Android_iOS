package com.reto.qa.config;

import java.time.Duration;
import java.util.Objects;

/**
 * Timeouts used by explicit waits and by the Appium session.
 *
 * @param explicit   default timeout of explicit waits
 * @param polling    interval between condition checks
 * @param newCommand how long Appium waits for a new command before ending the session
 */
public record TimeoutSettings(Duration explicit, Duration polling, Duration newCommand) {

    /**
     * Validates that every timeout is positive and that polling is shorter than the explicit timeout.
     *
     * @param explicit   default timeout of explicit waits
     * @param polling    interval between condition checks
     * @param newCommand Appium new-command timeout
     * @throws ConfigurationException if a value is not valid
     */
    public TimeoutSettings {
        requirePositive("timeout.explicit.seconds", Objects.requireNonNull(explicit, "explicit"));
        requirePositive("timeout.polling.millis", Objects.requireNonNull(polling, "polling"));
        requirePositive("timeout.new-command.seconds", Objects.requireNonNull(newCommand, "newCommand"));
        if (polling.compareTo(explicit) >= 0) {
            throw new ConfigurationException("timeout.polling.millis (" + polling.toMillis()
                    + " ms) must be shorter than timeout.explicit.seconds (" + explicit.toSeconds() + " s)");
        }
    }

    /**
     * Reads the {@code timeout.*} keys.
     *
     * @param config configuration for one platform and environment
     * @return the validated settings
     * @throws ConfigurationException if a value is missing or invalid
     */
    public static TimeoutSettings from(FrameworkConfig config) {
        return new TimeoutSettings(
                config.getSeconds("timeout.explicit.seconds"),
                config.getMillis("timeout.polling.millis"),
                config.getSeconds("timeout.new-command.seconds"));
    }

    private static void requirePositive(String key, Duration value) {
        if (value.isNegative() || value.isZero()) {
            throw new ConfigurationException(key + " must be greater than zero but is " + value);
        }
    }
}
