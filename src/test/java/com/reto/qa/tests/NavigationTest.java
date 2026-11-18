package com.reto.qa.tests;

import com.reto.qa.app.WdioDemoApp;
import com.reto.qa.junit.MobileTest;
import com.reto.qa.navigation.Screen;
import com.reto.qa.pages.BasePage;
import com.reto.qa.pages.FormsPage;
import com.reto.qa.pages.HomePage;
import com.reto.qa.pages.LoginPage;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

/** E2E tests for navigating between the main screens. */
@MobileTest
@Epic("Navigation")
@Feature("Tab bar")
@DisplayName("Navigation")
class NavigationTest {

    @ParameterizedTest(name = "Opens {0}")
    @EnumSource(Screen.class)
    @Tag("smoke")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Each main screen can be opened")
    void shouldNavigateToScreen(Screen screen, WdioDemoApp app) {
        BasePage page = switch (screen) {
            case LOGIN -> app.open(screen, LoginPage::new);
            case FORMS -> app.open(screen, FormsPage::new);
            case HOME -> app.open(screen, HomePage::new);
        };

        assertThat(page.isLoaded()).as("Screen %s is visible", screen).isTrue();
    }
}
