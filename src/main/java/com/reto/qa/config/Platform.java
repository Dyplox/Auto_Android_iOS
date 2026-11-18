package com.reto.qa.config;

import io.appium.java_client.remote.AutomationName;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Plataformas soportadas. Cada una conoce su nombre "W3C" y el driver de Appium que la automatiza.
 */
public enum Platform {

    ANDROID("Android", AutomationName.ANDROID_UIAUTOMATOR2),
    IOS("iOS", AutomationName.IOS_XCUI_TEST);

    private final String w3cName;
    private final String automationName;

    Platform(String w3cName, String automationName) {
        this.w3cName = w3cName;
        this.automationName = automationName;
    }

    public String w3cName() {
        return w3cName;
    }

    public String automationName() {
        return automationName;
    }

    /** Clave en minúsculas usada en archivos de configuración y overrides (android / ios). */
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Platform from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("La plataforma no puede ser vacía. Valores válidos: " + validValues());
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(platform -> platform.name().equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Plataforma no soportada: '" + value + "'. Valores válidos: " + validValues()));
    }

    private static String validValues() {
        return Arrays.stream(values()).map(Platform::key).collect(Collectors.joining(", "));
    }
}
