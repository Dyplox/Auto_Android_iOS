package com.reto.qa.pages.components;

import com.reto.qa.core.Waiter;
import com.reto.qa.driver.MobileSession;
import com.reto.qa.pages.BaseUiObject;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;
import org.openqa.selenium.WebElement;

/**
 * Alerta nativa del sistema operativo. Aquí SÍ difieren los locators entre plataformas
 * (resource-id / UiSelector en Android vs. NSPredicate / accessibility id en iOS),
 * pero la API del componente y los tests que la usan son idénticos.
 */
public final class NativeAlert extends BaseUiObject {

    /** En iOS el "label" del propio XCUIElementTypeAlert es el título. */
    @AndroidFindBy(id = "com.wdiodemoapp:id/alert_title")
    @iOSXCUITFindBy(iOSNsPredicate = "type == 'XCUIElementTypeAlert'")
    private WebElement title;

    @AndroidFindBy(uiAutomator = "new UiSelector().className(\"android.widget.Button\").text(\"OK\")")
    @iOSXCUITFindBy(accessibility = "OK")
    private WebElement okButton;

    public NativeAlert(MobileSession session) {
        super(session);
    }

    public NativeAlert waitUntilVisible() {
        waiter.untilVisible(title);
        return this;
    }

    public String title() {
        return textOf(title);
    }

    public boolean isDisplayed() {
        return Waiter.isDisplayedNow(title);
    }

    @Step("Aceptar la alerta nativa (OK)")
    public void accept() {
        tap(okButton);
        waiter.untilGone(title);
    }
}
