package com.reto.qa.core;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

public final class IosPlatformActions extends AbstractPlatformActions<IOSDriver> {

    /** Diálogo del sistema "¿Abrir en <app>?" que el simulador muestra al abrir un deep link. */
    private static final By OPEN_IN_APP_PROMPT =
            AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND name CONTAINS 'Open'");
    private static final Duration PROMPT_TIMEOUT = Duration.ofSeconds(2);

    private final Waiter waiter;

    public IosPlatformActions(IOSDriver driver, String bundleId, Waiter waiter) {
        super(driver, bundleId);
        this.waiter = Objects.requireNonNull(waiter, "waiter");
    }

    /**
     * {@code hideKeyboard} falla en iPhone cuando el teclado no tiene botón para cerrarse (limitación de XCTest),
     * por eso se toca un área neutra, igual que haría un usuario.
     */
    @Override
    public void hideKeyboard(WebElement neutralArea) {
        if (isKeyboardShown()) {
            neutralArea.click();
        }
    }

    @Override
    public void openDeepLink(String url) {
        driver.executeScript("mobile: deepLink", Map.of("url", url, "bundleId", appId));
        waiter.findVisible(OPEN_IN_APP_PROMPT, PROMPT_TIMEOUT).ifPresent(WebElement::click);
    }
}
