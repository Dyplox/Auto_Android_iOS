package com.reto.qa.config;

/** Error de configuración: se lanza temprano (fail fast) con un mensaje accionable. */
public class ConfigurationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
