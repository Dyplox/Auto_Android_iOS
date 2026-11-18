package com.reto.qa.pages.components;

import com.reto.qa.driver.MobileSession;
import com.reto.qa.navigation.Screen;
import com.reto.qa.pages.BaseUiObject;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;
import org.openqa.selenium.WebElement;

/** Bottom navigation bar (a reusable component: composition over inheritance). */
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

    /**
     * Creates the component for the session's platform.
     *
     * @param session the current session
     */
    public TabBar(MobileSession session) {
        super(session);
    }

    /**
     * Waits until the tab bar is shown.
     *
     * @return this component
     */
    public TabBar waitUntilVisible() {
        waiter.untilVisible(homeTab);
        return this;
    }

    /**
     * Taps the tab of a screen.
     *
     * @param screen the screen to open
     */
    @Step("Open the {screen} tab")
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
