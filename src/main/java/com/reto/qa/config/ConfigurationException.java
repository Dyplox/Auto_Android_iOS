package com.reto.qa.config;

/**
 * Configuration error. It is thrown early (fail fast) with an actionable message that tells the user what to fix.
 */
public class ConfigurationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception with an actionable message.
     *
     * @param message what is wrong and how to fix it
     */
    public ConfigurationException(String message) {
        super(message);
    }

    /**
     * Creates the exception with an actionable message and its root cause.
     *
     * @param message what is wrong and how to fix it
     * @param cause   the underlying error
     */
    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
