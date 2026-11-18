package com.reto.qa.core;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebElement;

import java.util.Map;

public final class AndroidPlatformActions extends AbstractPlatformActions<AndroidDriver> {

    public AndroidPlatformActions(AndroidDriver driver, String appPackage) {
        super(driver, appPackage);
    }

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
