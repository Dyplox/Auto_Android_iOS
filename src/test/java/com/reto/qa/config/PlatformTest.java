package com.reto.qa.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlatformTest {

    @ParameterizedTest(name = "''{0}'' -> {1}")
    @CsvSource({
            "android,     ANDROID",
            "' Android ', ANDROID",
            "IOS,         IOS",
            "ios,         IOS"
    })
    void parsesPlatformIgnoringCaseAndSpaces(String value, Platform expected) {
        assertThat(Platform.from(value)).isEqualTo(expected);
    }

    @Test
    void rejectsUnknownPlatformListingValidOnes() {
        assertThatThrownBy(() -> Platform.from("windows"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("android, ios");
    }

    @Test
    void exposesAppiumAutomationNames() {
        assertThat(Platform.ANDROID.automationName()).isEqualToIgnoringCase("UiAutomator2");
        assertThat(Platform.IOS.automationName()).isEqualToIgnoringCase("XCUITest");
        assertThat(Platform.IOS.key()).isEqualTo("ios");
    }
}
