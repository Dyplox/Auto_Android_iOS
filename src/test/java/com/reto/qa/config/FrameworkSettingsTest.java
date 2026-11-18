package com.reto.qa.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Typed settings: parsing and fail-fast validation, with no global state. */
class FrameworkSettingsTest {

    private static FrameworkConfig config(String... overrides) {
        Properties base = new Properties();
        base.setProperty("appium.server.autostart", "false");
        base.setProperty("appium.server.url", "http://127.0.0.1:4723");
        base.setProperty("timeout.explicit.seconds", "20");
        base.setProperty("timeout.polling.millis", "250");
        base.setProperty("timeout.new-command.seconds", "240");
        base.setProperty("navigation.strategy", "tabbar");
        base.setProperty("app.deeplink.scheme", "wdio://");
        base.setProperty("session.no-reset", "false");
        Properties top = new Properties();
        for (int i = 0; i < overrides.length; i += 2) {
            top.setProperty(overrides[i], overrides[i + 1]);
        }
        return FrameworkConfig.of(Platform.ANDROID, "local", List.of(top, base), Map.of(), new Properties());
    }

    @Test
    void readsEveryGroupWithItsType() {
        FrameworkSettings settings = FrameworkSettings.from(config());

        assertThat(settings.server().autostart()).isFalse();
        assertThat(settings.server().url()).hasHost("127.0.0.1").hasPort(4723);
        assertThat(settings.timeouts().explicit()).isEqualTo(Duration.ofSeconds(20));
        assertThat(settings.timeouts().polling()).isEqualTo(Duration.ofMillis(250));
        assertThat(settings.timeouts().newCommand()).isEqualTo(Duration.ofMinutes(4));
        assertThat(settings.navigation().strategy()).isEqualTo(NavigationStrategy.TABBAR);
        assertThat(settings.navigation().deepLinkScheme()).isEqualTo("wdio://");
        assertThat(settings.noReset()).isFalse();
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({"tabbar, TABBAR", "' DeepLink ', DEEPLINK"})
    void parsesNavigationStrategyIgnoringCaseAndSpaces(String value, NavigationStrategy expected) {
        assertThat(FrameworkSettings.from(config("navigation.strategy", value)).navigation().strategy())
                .isEqualTo(expected);
    }

    @Test
    void unknownNavigationStrategyListsTheValidOnes() {
        assertThatThrownBy(() -> FrameworkSettings.from(config("navigation.strategy", "swipe")))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("swipe")
                .hasMessageContaining("tabbar, deeplink");
    }

    @Test
    void deepLinkSchemeMustEndWithSeparator() {
        assertThatThrownBy(() -> FrameworkSettings.from(config("app.deeplink.scheme", "wdio")))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("app.deeplink.scheme");
    }

    @Test
    void timeoutsMustBePositive() {
        assertThatThrownBy(() -> FrameworkSettings.from(config("timeout.explicit.seconds", "0")))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("timeout.explicit.seconds");
    }

    @Test
    void pollingMustBeShorterThanTheExplicitTimeout() {
        assertThatThrownBy(() -> FrameworkSettings.from(config("timeout.polling.millis", "30000")))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("timeout.polling.millis");
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({"local, ANDROID", "local, IOS", "ci, ANDROID", "ci, IOS", "saucelabs, ANDROID", "saucelabs, IOS",
            "browserstack, ANDROID"})
    void shippedEnvironmentsProduceValidSettings(String environment, Platform platform) {
        FrameworkConfig shipped = FrameworkConfig.of(platform, environment,
                FrameworkConfig.loadLayers(platform, environment), Map.of(), new Properties());

        assertThat(FrameworkSettings.from(shipped).timeouts().explicit()).isGreaterThan(Duration.ZERO);
    }
}
