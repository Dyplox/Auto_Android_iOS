package com.reto.qa.core;

import org.openqa.selenium.WebElement;

/**
 * Comportamientos que el sistema operativo resuelve de forma distinta (patrón Strategy).
 *
 * <p>Es el ÚNICO punto del framework donde existe código específico de plataforma fuera de los locators:
 * los Page Objects y los tests dependen de esta abstracción (DIP) y nunca preguntan "¿estoy en iOS?".</p>
 */
public interface PlatformActions {

    /**
     * Oculta el teclado si está visible.
     *
     * @param neutralArea elemento sin interacción donde tocar si la plataforma no puede ocultarlo por API
     *                    (limitación conocida de XCTest en iPhone)
     */
    void hideKeyboard(WebElement neutralArea);

    /** Abre un deep link dentro de la app bajo prueba. */
    void openDeepLink(String url);

    /** Cierra y vuelve a abrir la app: estado limpio entre tests sin recrear la sesión. */
    void restartApp();
}
