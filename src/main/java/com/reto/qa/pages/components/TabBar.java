package com.reto.qa.pages.components;

import com.reto.qa.driver.MobileSession;
import com.reto.qa.navigation.Screen;
import com.reto.qa.pages.BaseUiObject;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;
import org.openqa.selenium.WebElement;

/** Barra inferior de navegación (componente reutilizable, composición sobre herencia). */
public final class TabBar extends BaseUiObject {

    @AndroidFindBy(accessibility = "Home")
    @iOSXCUITFindBy(accessibility = "Home")
    private WebElement homeTab;

    @AndroidFindBy(accessibility = "Login")
    @iOSXCUITFindBy(accessibility = "Login")
    private WebElement loginTab;

    @AndroidFindBy(accessibility = "Forms")
    @iOSXCUITFindBy(accessibility = "Forms")
    private WebElement formsTab;

    public TabBar(MobileSession session) {
        super(session);
    }

    public TabBar waitUntilVisible() {
        waiter.untilVisible(homeTab);
        return this;
    }

    @Step("Abrir la pestaña {screen}")
    public void open(Screen screen) {
        tap(tabFor(screen));
    }

    private WebElement tabFor(Screen screen) {
        return switch (screen) {
            case HOME -> homeTab;
            case LOGIN -> loginTab;
            case FORMS -> formsTab;
        };
    }
}
