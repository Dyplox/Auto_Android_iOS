package com.reto.qa.tests;

import com.reto.qa.app.WdioDemoApp;
import com.reto.qa.data.CredentialsMother;
import com.reto.qa.junit.MobileTest;
import com.reto.qa.navigation.Screen;
import com.reto.qa.pages.LoginPage;
import com.reto.qa.pages.LoginValidationError;
import com.reto.qa.pages.components.NativeAlert;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@MobileTest
@Epic("Autenticación")
@Feature("Registro")
@DisplayName("Registro")
class SignUpTest {

    @Test
    @Tag("smoke")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Datos válidos muestran la alerta de confirmación")
    void shouldSignUpWithValidData(WdioDemoApp app) {
        LoginPage loginPage = app.open(Screen.LOGIN, LoginPage::new);

        loginPage.signUpWith(CredentialsMother.valid());

        NativeAlert alert = app.alert().waitUntilVisible();
        assertThat(alert.title()).as("Título de la alerta de registro").contains("Signed Up");
        alert.accept();
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Una contraseña corta muestra el error de longitud")
    void shouldRejectShortPasswordOnSignUp(WdioDemoApp app) {
        LoginPage loginPage = app.open(Screen.LOGIN, LoginPage::new);

        loginPage.signUpWith(CredentialsMother.withShortPassword());

        assertThat(loginPage.isValidationErrorDisplayed(LoginValidationError.SHORT_PASSWORD)).isTrue();
    }
}
