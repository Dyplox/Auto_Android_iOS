package com.reto.qa.pages;

import com.reto.qa.driver.MobileSession;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import org.openqa.selenium.WebElement;

/** The "Home" screen. */
public final class HomePage extends BasePage {

    @AndroidFindBy(accessibility = "Home-screen")
    @iOSXCUITFindBy(accessibility = "Home-screen")
    private WebElement screen;

    /**
     * Creates the page for the session's platform.
     *
     * @param session the current session
     */
    public HomePage(MobileSession session) {
        super(session);
    }

    @Override
    protected WebElement root() {
        return screen;
    }
}
