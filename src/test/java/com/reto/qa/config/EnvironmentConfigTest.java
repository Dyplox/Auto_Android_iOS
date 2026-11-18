package com.reto.qa.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Environment layers, secret interpolation and the .env file (no global state). */
class EnvironmentConfigTest {

    private static Properties props(String... keyValues) {
        Properties properties = new Properties();
        for (int i = 0; i < keyValues.length; i += 2) {
            properties.setProperty(keyValues[i], keyValues[i + 1]);
        }
        return properties;
    }

    @Test
    void environmentFilesOverridePlatformAndCommonFiles() {
        FrameworkConfig config = FrameworkConfig.of(Platform.IOS, "saucelabs",
                List.of(props("device.name", "iPhone 17 Simulator"), // env/saucelabs-ios
                        props("timeout.explicit.seconds", "30"), // env/saucelabs
                        props("device.name", "iPhone 17", "app.id", "org.app"), // ios
                        props("timeout.explicit.seconds", "20")), // framework
                Map.of(), props());

        assertThat(config.environment()).isEqualTo("saucelabs");
        assertThat(config.get("device.name")).isEqualTo("iPhone 17 Simulator");
        assertThat(config.getInt("timeout.explicit.seconds")).isEqualTo(30);
        assertThat(config.get("app.id")).isEqualTo("org.app");
    }

    @Test
    void placeholdersResolveFromEnvironmentVariablesWithDefaults() {
        FrameworkConfig config = FrameworkConfig.of(Platform.ANDROID, "saucelabs",
                List.of(props("appium.server.url", "https://ondemand.${SAUCE_REGION:-us-west-1}.saucelabs.com/wd/hub",
                        "cloud.option.username", "${SAUCE_USERNAME}")),
                Map.of("SAUCE_USERNAME", "qa-bot"), props());

        assertThat(config.get("appium.server.url")).isEqualTo("https://ondemand.us-west-1.saucelabs.com/wd/hub");
        assertThat(config.get("cloud.option.username")).isEqualTo("qa-bot");
    }

    @Test
    void systemPropertyWinsWhenResolvingPlaceholders() {
        FrameworkConfig config = FrameworkConfig.of(Platform.ANDROID, "saucelabs",
                List.of(props("appium.server.url", "https://ondemand.${SAUCE_REGION:-us-west-1}.saucelabs.com/wd/hub")),
                Map.of("SAUCE_REGION", "us-west-1"), props("SAUCE_REGION", "eu-central-1"));

        assertThat(config.get("appium.server.url")).contains("eu-central-1");
    }

    @Test
    void missingSecretFailsOnlyWhenTheKeyIsUsedAndNamesTheVariable() {
        FrameworkConfig config = FrameworkConfig.of(Platform.ANDROID, "browserstack",
                List.of(props("cloud.option.accessKey", "${BROWSERSTACK_ACCESS_KEY}", "device.name", "Pixel")),
                Map.of("BROWSERSTACK_ACCESS_KEY", "  "), props());

        assertThat(config.get("device.name")).isEqualTo("Pixel");
        assertThatThrownBy(() -> config.get("cloud.option.accessKey"))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("BROWSERSTACK_ACCESS_KEY")
                .hasMessageContaining(".env");
    }

    @ParameterizedTest(name = "system={0}, variable={1} -> {2}")
    @CsvSource(nullValues = "null", value = {
            "browserstack, saucelabs, browserstack",
            "null,         SauceLabs, saucelabs",
            "'  ',         null,      local",
            "null,         null,      local"
    })
    void environmentIsResolvedFromSystemPropertyThenVariableThenDefault(String system, String variable,
            String expected) {
        Properties systemProperties = system == null ? props() : props(FrameworkConfig.ENVIRONMENT_PROPERTY, system);
        Map<String, String> variables = variable == null
                ? Map.of()
                : Map.of(FrameworkConfig.ENVIRONMENT_VARIABLE, variable);

        assertThat(FrameworkConfig.resolveEnvironment(systemProperties, variables)).isEqualTo(expected);
    }

    @Test
    void parsesDotEnvFiles() {
        Map<String, String> variables = FrameworkConfig.parseDotEnv("""
                # local credentials

                SAUCE_USERNAME=qa-bot
                export SAUCE_ACCESS_KEY = 'abc=123'
                BUILD_NAME="Reto SDET"
                line without equals sign
                """);

        assertThat(variables)
                .containsEntry("SAUCE_USERNAME", "qa-bot")
                .containsEntry("SAUCE_ACCESS_KEY", "abc=123")
                .containsEntry("BUILD_NAME", "Reto SDET")
                .hasSize(3);
    }

    @Test
    void unknownEnvironmentFailsFastListingTheIncludedOnes() {
        assertThatThrownBy(() -> FrameworkConfig.loadLayers(Platform.ANDROID, "staging"))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("staging")
                .hasMessageContaining("local, ci, browserstack, saucelabs");
    }
}
