package com.reto.qa.tests;

import com.reto.qa.app.WdioDemoApp;
import com.reto.qa.junit.MobileTest;
import com.reto.qa.navigation.Screen;
import com.reto.qa.pages.FormsPage;
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
@Epic("Componentes de formulario")
@Feature("Forms")
@DisplayName("Forms")
class FormsTest {

    @Test
    @Tag("smoke")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("El texto escrito se refleja en el resultado")
    void shouldEchoTypedText(WdioDemoApp app) {
        String text = "Appium cross-platform";
        FormsPage formsPage = app.open(Screen.FORMS, FormsPage::new);

        formsPage.typeText(text);

        assertThat(formsPage.typedTextResult()).isEqualTo(text);
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("El switch alterna entre encendido y apagado")
    void shouldToggleSwitch(WdioDemoApp app) {
        FormsPage formsPage = app.open(Screen.FORMS, FormsPage::new);
        assertThat(formsPage.switchLabel()).as("Estado inicial").endsWith("ON");

        formsPage.toggleSwitch();
        assertThat(formsPage.switchLabel()).as("Tras encender").endsWith("OFF");

        formsPage.toggleSwitch();
        assertThat(formsPage.switchLabel()).as("Tras apagar").endsWith("ON");
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("El botón 'Active' abre una alerta nativa")
    void shouldOpenNativeAlertFromActiveButton(WdioDemoApp app) {
        FormsPage formsPage = app.open(Screen.FORMS, FormsPage::new);

        formsPage.tapActiveButton();

        NativeAlert alert = app.alert().waitUntilVisible();
        assertThat(alert.title()).contains("This button is");
        alert.accept();
    }
}
