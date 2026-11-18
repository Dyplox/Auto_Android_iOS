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
 * Explicit waits on top of java-client 11's wait engine ({@code io.appium.java_client.support.ui}).
 * {@code Thread.sleep} is never used: every wait is condition-based (fast, repeatable tests).
 */
public final class Waiter {

    private final AppiumDriver driver;
    private final Duration timeout;
    private final Duration polling;

    /**
     * Creates the waiter.
     *
     * @param driver  the Appium driver
     * @param timeout default timeout
     * @param polling interval between condition checks
     */
    public Waiter(AppiumDriver driver, Duration timeout, Duration polling) {
        this.driver = Objects.requireNonNull(driver, "driver");
        this.timeout = Objects.requireNonNull(timeout, "timeout");
        this.polling = Objects.requireNonNull(polling, "polling");
    }

    /**
     * Returns the default timeout.
     *
     * @return the timeout used when none is given
     */
    public Duration defaultTimeout() {
        return timeout;
    }

    /**
     * Waits until the element is visible, using the default timeout.
     *
     * @param element element to wait for
     * @return the same element
     * @throws TimeoutException if it does not become visible in time
     */
    public WebElement untilVisible(WebElement element) {
        return untilVisible(element, timeout);
    }

    /**
     * Waits until the element is visible.
     *
     * @param element       element to wait for
     * @param customTimeout how long to wait
     * @return the same element
     * @throws TimeoutException if it does not become visible in time
     */
    public WebElement untilVisible(WebElement element, Duration customTimeout) {
        return wait(customTimeout)
                .withMessage("element to be visible")
                .until(d -> element.isDisplayed() ? element : null);
    }

    /**
     * Checks whether the element becomes visible within the timeout, without throwing.
     *
     * @param element       element to check
     * @param customTimeout how long to wait
     * @return {@code true} if it became visible in time
     */
    public boolean isVisible(WebElement element, Duration customTimeout) {
        try {
            untilVisible(element, customTimeout);
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    /**
     * Looks for an element by a dynamic locator for up to {@code customTimeout}.
     *
     * @param locator       how to find the element
     * @param customTimeout how long to wait
     * @return the visible element, or empty if it does not appear
     */
    public Optional<WebElement> findVisible(By locator, Duration customTimeout) {
        try {
            return Optional.of(wait(customTimeout)
                    .withMessage(locator + " to appear")
                    .until(d -> {
                        WebElement element = d.findElement(locator);
                        return element.isDisplayed() ? element : null;
                    }));
        } catch (TimeoutException e) {
            return Optional.empty();
        }
    }

    /**
     * Waits until the element is no longer displayed (or no longer exists), using the default timeout.
     *
     * @param element element to wait for
     * @throws TimeoutException if it is still displayed when the timeout expires
     */
    public void untilGone(WebElement element) {
        wait(timeout)
                .withMessage("element to disappear")
                .until(d -> !isDisplayedNow(element));
    }

    /**
     * Waits for a custom condition, using the default timeout.
     *
     * @param condition   returns a non-null, non-false value when satisfied
     * @param description what is being waited for, shown in the timeout message
     * @param <V>         the condition's result type
     * @return the condition's result
     * @throws TimeoutException if the condition is not met in time
     */
    public <V> V until(Function<AppiumDriver, V> condition, String description) {
        return wait(timeout).withMessage(description).until(condition);
    }

    /**
     * Instant check without waiting; does not throw if the element does not exist.
     *
     * @param element element to check
     * @return {@code true} if the element exists and is displayed right now
     */
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
