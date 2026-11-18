package com.reto.qa.pages;

import com.reto.qa.driver.MobileSession;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;
import org.openqa.selenium.WebElement;

/** The "Forms" screen: text input, switch and buttons. */
public final class FormsPage extends BasePage {

    @AndroidFindBy(accessibility = "Forms-screen")
    @iOSXCUITFindBy(accessibility = "Forms-screen")
    private WebElement screen;

    @AndroidFindBy(accessibility = "text-input")
    @iOSXCUITFindBy(accessibility = "text-input")
    private WebElement textInput;

    @AndroidFindBy(accessibility = "input-text-result")
    @iOSXCUITFindBy(accessibility = "input-text-result")
    private WebElement typedTextResult;

    @AndroidFindBy(accessibility = "switch")
    @iOSXCUITFindBy(accessibility = "switch")
    private WebElement switchToggle;

    @AndroidFindBy(accessibility = "switch-text")
    @iOSXCUITFindBy(accessibility = "switch-text")
    private WebElement switchLabel;

    @AndroidFindBy(accessibility = "button-Active")
    @iOSXCUITFindBy(accessibility = "button-Active")
    private WebElement activeButton;

    /**
     * Creates the page for the session's platform.
     *
     * @param session the current session
     */
    public FormsPage(MobileSession session) {
        super(session);
    }

    @Override
    protected WebElement root() {
        return screen;
    }

    /** The center of the screen may land on the switch, so the non-interactive result text is tapped instead. */
    @Override
    protected WebElement keyboardDismissArea() {
        return typedTextResult;
    }

    /**
     * Types into the text input and hides the keyboard.
     *
     * @param text text to type
     * @return this page
     */
    @Step("Type '{text}' into the text field")
    public FormsPage typeText(String text) {
        type(textInput, text);
        hideKeyboard();
        return this;
    }

    /**
     * Returns the text that the screen echoes below the input.
     *
     * @return the echoed text
     */
    public String typedTextResult() {
        return textOf(typedTextResult);
    }

    /**
     * Toggles the switch.
     *
     * @return this page
     */
    @Step("Toggle the switch")
    public FormsPage toggleSwitch() {
        tap(switchToggle);
        return this;
    }

    /**
     * Returns the label shown next to the switch. The label is checked instead of the switch attribute ("checked" on
     * Android, "value" on iOS), so the assertion is identical on both platforms.
     *
     * @return the switch label
     */
    public String switchLabel() {
        return textOf(switchLabel);
    }

    /** Scrolls to the "Active" button and taps it. */
    @Step("Tap the 'Active' button")
    public void tapActiveButton() {
        scrollTo(activeButton);
        tap(activeButton);
    }
}
