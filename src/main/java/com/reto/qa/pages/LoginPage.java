package com.reto.qa.pages;

import com.reto.qa.data.UserCredentials;
import com.reto.qa.driver.MobileSession;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;
import org.openqa.selenium.WebElement;

/**
 * The Login / Sign up screen.
 *
 * <p>Fields that share the same accessibility id on both platforms are declared twice on purpose: each OS locator
 * stays explicit and can change without touching the logic. The validation messages show the case where the locator
 * does differ (UiSelector vs. NSPredicate).</p>
 */
public final class LoginPage extends BasePage {

    static final String INVALID_EMAIL_MESSAGE = "Please enter a valid email address";
    static final String MIN_LENGTH_MESSAGE = "Please enter at least 8 characters";

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

    @AndroidFindBy(uiAutomator = "new UiSelector().text(\"" + MIN_LENGTH_MESSAGE + "\")")
    @iOSXCUITFindBy(iOSNsPredicate = "label == '" + MIN_LENGTH_MESSAGE + "'")
    private WebElement shortPasswordError;

    /**
     * Creates the page for the session's platform.
     *
     * @param session the current session
     */
    public LoginPage(MobileSession session) {
        super(session);
    }

    @Override
    protected WebElement root() {
        return screen;
    }

    /**
     * Opens the Login tab, fills in the form and submits it.
     *
     * @param credentials email and password
     */
    @Step("Log in with {credentials}")
    public void loginWith(UserCredentials credentials) {
        tap(loginTab);
        type(emailInput, credentials.email());
        type(passwordInput, credentials.password());
        submit(loginButton);
    }

    /**
     * Opens the Sign up tab, fills in the form and submits it.
     *
     * @param credentials email, password and password confirmation
     */
    @Step("Sign up with {credentials}")
    public void signUpWith(UserCredentials credentials) {
        tap(signUpTab);
        type(emailInput, credentials.email());
        type(passwordInput, credentials.password());
        type(repeatPasswordInput, credentials.passwordConfirmation());
        submit(signUpButton);
    }

    /**
     * Checks whether a validation error appears, waiting up to the default timeout.
     *
     * @param error the expected error
     * @return {@code true} if the error is displayed
     */
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
