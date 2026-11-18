package com.reto.qa.pages;

import com.reto.qa.driver.MobileSession;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;
import org.openqa.selenium.WebElement;

/** Pantalla "Forms": input, switch y botones. */
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

    public FormsPage(MobileSession session) {
        super(session);
    }

    @Override
    protected WebElement root() {
        return screen;
    }

    /** El centro de la pantalla puede caer sobre el switch: se toca el texto de resultado, que no es interactivo. */
    @Override
    protected WebElement keyboardDismissArea() {
        return typedTextResult;
    }

    @Step("Escribir '{text}' en el campo de texto")
    public FormsPage typeText(String text) {
        type(textInput, text);
        hideKeyboard();
        return this;
    }

    public String typedTextResult() {
        return textOf(typedTextResult);
    }

    @Step("Cambiar el estado del switch")
    public FormsPage toggleSwitch() {
        tap(switchToggle);
        return this;
    }

    /**
     * Se valida el texto que acompaña al switch y no el atributo del switch ("checked" en Android, "value" en iOS):
     * así la verificación es idéntica en ambas plataformas.
     */
    public String switchLabel() {
        return textOf(switchLabel);
    }

    @Step("Tocar el botón 'Active'")
    public void tapActiveButton() {
        scrollTo(activeButton);
        tap(activeButton);
    }
}
