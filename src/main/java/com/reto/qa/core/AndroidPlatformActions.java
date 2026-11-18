package com.reto.qa.core;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebElement;

import java.util.Map;

/** {@link PlatformActions} for Android (UiAutomator2). */
public final class AndroidPlatformActions extends AbstractPlatformActions<AndroidDriver> {

    /**
     * Creates the Android actions.
     *
     * @param driver     the Android driver
     * @param appPackage package of the app under test
     */
    public AndroidPlatformActions(AndroidDriver driver, String appPackage) {
        super(driver, appPackage);
    }

    /** Android can always hide the keyboard through the API, so {@code neutralArea} is not used. */
    @Override
    public void hideKeyboard(WebElement neutralArea) {
        if (isKeyboardShown()) {
            driver.hideKeyboard();
        }
    }

    @Override
    public void openDeepLink(String url) {
        driver.executeScript("mobile: deepLink", Map.of("url", url, "package", appId));
    }
}
