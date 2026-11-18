package com.reto.qa.pages.components;

import com.reto.qa.core.Waiter;
import com.reto.qa.driver.MobileSession;
import com.reto.qa.pages.BaseUiObject;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;
import org.openqa.selenium.WebElement;

/**
 * Native OS alert. Here the locators DO differ between platforms
 * (resource-id / UiSelector on Android vs. NSPredicate / accessibility id on iOS),
 * but the component's API and the tests that use it are identical.
 */
public final class NativeAlert extends BaseUiObject {

    /** On iOS, the "label" of the XCUIElementTypeAlert itself is the title. */
    @AndroidFindBy(id = "com.wdiodemoapp:id/alert_title")
    @iOSXCUITFindBy(iOSNsPredicate = "type == 'XCUIElementTypeAlert'")
    private WebElement title;

    @AndroidFindBy(uiAutomator = "new UiSelector().className(\"android.widget.Button\").text(\"OK\")")
    @iOSXCUITFindBy(accessibility = "OK")
    private WebElement okButton;

    /**
     * Creates the component for the session's platform.
     *
     * @param session the current session
     */
    public NativeAlert(MobileSession session) {
        super(session);
    }

    /**
     * Waits until the alert is shown.
     *
     * @return this component
     */
    public NativeAlert waitUntilVisible() {
        waiter.untilVisible(title);
        return this;
    }

    /**
     * Returns the alert title.
     *
     * @return the title
     */
    public String title() {
        return textOf(title);
    }

    /**
     * Checks, without waiting, whether the alert is shown.
     *
     * @return {@code true} if the alert is displayed right now
     */
    public boolean isDisplayed() {
        return Waiter.isDisplayedNow(title);
    }

    /** Taps OK and waits until the alert is gone. */
    @Step("Accept the native alert (OK)")
    public void accept() {
        tap(okButton);
        waiter.untilGone(title);
    }
}
