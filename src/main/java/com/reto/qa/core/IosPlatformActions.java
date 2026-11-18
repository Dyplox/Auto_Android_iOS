package com.reto.qa.core;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/** {@link PlatformActions} for iOS (XCUITest). */
public final class IosPlatformActions extends AbstractPlatformActions<IOSDriver> {

    /** System dialog "Open in &lt;app&gt;?" that the simulator shows when a deep link is opened. */
    private static final By OPEN_IN_APP_PROMPT =
            AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND name CONTAINS 'Open'");
    private static final Duration PROMPT_TIMEOUT = Duration.ofSeconds(2);

    private final Waiter waiter;

    /**
     * Creates the iOS actions.
     *
     * @param driver   the iOS driver
     * @param bundleId bundle id of the app under test
     * @param waiter   explicit waits, used to handle the "Open in app" prompt
     */
    public IosPlatformActions(IOSDriver driver, String bundleId, Waiter waiter) {
        super(driver, bundleId);
        this.waiter = Objects.requireNonNull(waiter, "waiter");
    }

    /**
     * {@code hideKeyboard} fails on iPhone when the keyboard has no dismiss button (an XCTest limitation),
     * so a neutral area is tapped instead, just as a user would.
     */
    @Override
    public void hideKeyboard(WebElement neutralArea) {
        if (isKeyboardShown()) {
            neutralArea.click();
        }
    }

    /** Opens the link and accepts the "Open in app" prompt if the simulator shows it. */
    @Override
    public void openDeepLink(String url) {
        driver.executeScript("mobile: deepLink", Map.of("url", url, "bundleId", appId));
        waiter.findVisible(OPEN_IN_APP_PROMPT, PROMPT_TIMEOUT).ifPresent(WebElement::click);
    }
}
