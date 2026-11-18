package com.reto.qa.pages;

import com.reto.qa.data.UserCredentials;
import com.reto.qa.driver.MobileSession;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;
import org.openqa.selenium.WebElement;

/**
 * Pantalla Login / Sign up.
 *
 * <p>Los campos con el mismo accessibility id en ambas plataformas se declaran igual dos veces a propósito:
 * el locator de cada SO queda explícito y puede cambiar sin tocar la lógica. Los mensajes de validación
 * muestran el caso en que el locator sí difiere (UiSelector vs. NSPredicate).</p>
 */
public final class LoginPage extends BasePage {

    static final String INVALID_EMAIL_MESSAGE = "Please enter a valid email address";
    static final String SHORT_PASSWORD_MESSAGE = "Please enter at least 8 characters";

    @AndroidFindBy(accessibility = "Login-screen")
    @iOSXCUITFindBy(accessibility = "Login-screen")
    private WebElement screen;

    @AndroidFindBy(accessibility = "button-login-container")
    @iOSXCUITFindBy(accessibility = "button-login-container")
    private WebElement loginTab;

    @AndroidFindBy(accessibility = "button-sign-up-container")
    @iOSXCUITFindBy(accessibility = "button-sign-up-container")
    private WebElement signUpTab;

    @AndroidFindBy(accessibility = "input-email")
    @iOSXCUITFindBy(accessibility = "input-email")
    private WebElement emailInput;

    @AndroidFindBy(accessibility = "input-password")
    @iOSXCUITFindBy(accessibility = "input-password")
    private WebElement passwordInput;

    @AndroidFindBy(accessibility = "input-repeat-password")
    @iOSXCUITFindBy(accessibility = "input-repeat-password")
    private WebElement repeatPasswordInput;

    @AndroidFindBy(accessibility = "button-LOGIN")
    @iOSXCUITFindBy(accessibility = "button-LOGIN")
    private WebElement loginButton;

    @AndroidFindBy(accessibility = "button-SIGN UP")
    @iOSXCUITFindBy(accessibility = "button-SIGN UP")
    private WebElement signUpButton;

    @AndroidFindBy(uiAutomator = "new UiSelector().text(\"" + INVALID_EMAIL_MESSAGE + "\")")
    @iOSXCUITFindBy(iOSNsPredicate = "label == '" + INVALID_EMAIL_MESSAGE + "'")
    private WebElement invalidEmailError;

    @AndroidFindBy(uiAutomator = "new UiSelector().text(\"" + SHORT_PASSWORD_MESSAGE + "\")")
    @iOSXCUITFindBy(iOSNsPredicate = "label == '" + SHORT_PASSWORD_MESSAGE + "'")
    private WebElement shortPasswordError;

    public LoginPage(MobileSession session) {
        super(session);
    }

    @Override
    protected WebElement root() {
        return screen;
    }

    @Step("Iniciar sesión con {credentials}")
    public void loginWith(UserCredentials credentials) {
        tap(loginTab);
        type(emailInput, credentials.email());
        type(passwordInput, credentials.password());
        submit(loginButton);
    }

    @Step("Registrarse con {credentials}")
    public void signUpWith(UserCredentials credentials) {
        tap(signUpTab);
        type(emailInput, credentials.email());
        type(passwordInput, credentials.password());
        type(repeatPasswordInput, credentials.passwordConfirmation());
        submit(signUpButton);
    }

    public boolean isValidationErrorDisplayed(LoginValidationError error) {
        return isVisible(errorElement(error), waiter.defaultTimeout());
    }

    private void submit(WebElement button) {
        hideKeyboard();
        scrollTo(button);
        tap(button);
    }

    private WebElement errorElement(LoginValidationError error) {
        return switch (error) {
            case INVALID_EMAIL -> invalidEmailError;
            case SHORT_PASSWORD -> shortPasswordError;
        };
    }
}
