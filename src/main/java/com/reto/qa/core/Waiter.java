package com.reto.qa.core;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.support.ui.FluentWait;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Esperas explícitas sobre el motor de esperas de java-client 11 ({@code io.appium.java_client.support.ui}).
 * Nunca se usa {@code Thread.sleep}: todas las esperas son por condición (pruebas rápidas y repetibles).
 */
public final class Waiter {

    private final AppiumDriver driver;
    private final Duration timeout;
    private final Duration polling;

    public Waiter(AppiumDriver driver, Duration timeout, Duration polling) {
        this.driver = Objects.requireNonNull(driver, "driver");
        this.timeout = Objects.requireNonNull(timeout, "timeout");
        this.polling = Objects.requireNonNull(polling, "polling");
    }

    public Duration defaultTimeout() {
        return timeout;
    }

    public WebElement untilVisible(WebElement element) {
        return untilVisible(element, timeout);
    }

    public WebElement untilVisible(WebElement element, Duration customTimeout) {
        return wait(customTimeout)
                .withMessage("el elemento sea visible")
                .until(d -> element.isDisplayed() ? element : null);
    }

    public boolean isVisible(WebElement element, Duration customTimeout) {
        try {
            untilVisible(element, customTimeout);
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    /** Busca un elemento por locator dinámico durante {@code customTimeout}; vacío si no aparece. */
    public Optional<WebElement> findVisible(By locator, Duration customTimeout) {
        try {
            return Optional.of(wait(customTimeout)
                    .withMessage("aparezca " + locator)
                    .until(d -> {
                        WebElement element = d.findElement(locator);
                        return element.isDisplayed() ? element : null;
                    }));
        } catch (TimeoutException e) {
            return Optional.empty();
        }
    }

    public void untilGone(WebElement element) {
        wait(timeout)
                .withMessage("el elemento desaparezca")
                .until(d -> !isDisplayedNow(element));
    }

    public <V> V until(Function<AppiumDriver, V> condition, String description) {
        return wait(timeout).withMessage(description).until(condition);
    }

    /** Consulta instantánea, sin esperar: no lanza excepción si el elemento no existe. */
    public static boolean isDisplayedNow(WebElement element) {
        try {
            return element.isDisplayed();
        } catch (NoSuchElementException | StaleElementReferenceException e) {
            return false;
        }
    }

    private FluentWait<AppiumDriver> wait(Duration customTimeout) {
        return new FluentWait<>(driver)
                .withTimeout(customTimeout)
                .pollingEvery(polling)
                .ignoring(NoSuchElementException.class)
                .ignoring(StaleElementReferenceException.class);
    }
}
