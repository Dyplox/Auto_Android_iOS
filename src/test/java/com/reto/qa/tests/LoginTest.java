package com.reto.qa.tests;

import com.reto.qa.app.WdioDemoApp;
import com.reto.qa.data.CredentialsMother;
import com.reto.qa.data.UserCredentials;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@MobileTest
@Epic("Autenticación")
@Feature("Login")
@DisplayName("Login")
class LoginTest {

    @Test
    @Tag("smoke")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Credenciales válidas muestran la alerta de éxito")
    void shouldLoginWithValidCredentials(WdioDemoApp app) {
        LoginPage loginPage = app.open(Screen.LOGIN, LoginPage::new);

        loginPage.loginWith(CredentialsMother.valid());

        NativeAlert alert = app.alert().waitUntilVisible();
        assertThat(alert.title()).as("Título de la alerta de login").contains("Success");
        alert.accept();
        assertThat(alert.isDisplayed()).as("La alerta se cierra al aceptar").isFalse();
    }

    @ParameterizedTest(name = "{1} con {0}")
    @MethodSource("invalidCredentials")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Credenciales inválidas muestran el mensaje de validación")
    void shouldShowValidationErrorForInvalidCredentials(UserCredentials credentials,
                                                        LoginValidationError expectedError,
                                                        WdioDemoApp app) {
        LoginPage loginPage = app.open(Screen.LOGIN, LoginPage::new);

        loginPage.loginWith(credentials);

        assertThat(loginPage.isValidationErrorDisplayed(expectedError))
                .as("Se muestra el error %s para %s", expectedError, credentials)
                .isTrue();
    }

    static Stream<Arguments> invalidCredentials() {
        return Stream.of(
                Arguments.of(CredentialsMother.withInvalidEmail(), LoginValidationError.INVALID_EMAIL),
                Arguments.of(CredentialsMother.withShortPassword(), LoginValidationError.SHORT_PASSWORD));
    }
}
