package com.reto.qa.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pure unit tests: they touch neither real system properties nor files (FIRST: Fast, Independent). */
class FrameworkConfigTest {

    private static Properties props(String... keyValues) {
        Properties properties = new Properties();
        for (int i = 0; i < keyValues.length; i += 2) {
            properties.setProperty(keyValues[i], keyValues[i + 1]);
        }
        return properties;
    }

    private static FrameworkConfig config(Platform platform, Properties common, Properties file,
                                          Map<String, String> env, Properties system) {
        return FrameworkConfig.of(platform, common, file, env, system);
    }

    @Test
    void platformPrefixedSystemPropertyHasHighestPriority() {
        FrameworkConfig config = config(Platform.IOS,
                props("device.name", "common"),
                props("device.name", "file"),
                Map.of("DEVICE_NAME", "env", "IOS_DEVICE_NAME", "env-ios"),
                props("device.name", "system", "ios.device.name", "system-ios"));

        assertThat(config.get("device.name")).isEqualTo("system-ios");
    }

    @Test
    void systemPropertyWinsOverEnvironmentAndFiles() {
        FrameworkConfig config = config(Platform.ANDROID,
                props("device.name", "common"), props("device.name", "file"),
                Map.of("DEVICE_NAME", "env"), props("device.name", "system"));

        assertThat(config.get("device.name")).isEqualTo("system");
    }

    @Test
    void platformEnvironmentVariableWinsOverGenericOne() {
        FrameworkConfig config = config(Platform.ANDROID,
                props(), props("appium.server.url", "http://file:4723"),
                Map.of("APPIUM_SERVER_URL", "http://env:4723", "ANDROID_APPIUM_SERVER_URL", "http://env-android:4723"),
                props());

        assertThat(config.get("appium.server.url")).isEqualTo("http://env-android:4723");
    }

    @Test
    void platformFileWinsOverCommonFile() {
        FrameworkConfig config = config(Platform.ANDROID,
                props("timeout.explicit.seconds", "10"), props("timeout.explicit.seconds", "30"), Map.of(), props());

        assertThat(config.getSeconds("timeout.explicit.seconds")).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void overrideForAnotherPlatformDoesNotLeak() {
        FrameworkConfig android = config(Platform.ANDROID,
                props(), props("device.name", "Pixel"), Map.of(), props("ios.device.name", "iPhone 17"));

        assertThat(android.get("device.name")).isEqualTo("Pixel");
    }

    @Test
    void blankValuesAreIgnored() {
        FrameworkConfig config = config(Platform.IOS,
                props("platform.version", "26.0"), props("platform.version", "   "), Map.of(), props());

        assertThat(config.find("platform.version")).contains("26.0");
    }

    @Test
    void missingRequiredKeyFailsFastWithActionableMessage() {
        FrameworkConfig config = config(Platform.IOS, props(), props(), Map.of(), props());

        assertThatThrownBy(() -> config.get("app.id"))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("app.id")
                .hasMessageContaining("config/ios.properties");
    }

    @Test
    void invalidTypedValuesFailFast() {
        FrameworkConfig config = config(Platform.ANDROID,
                props("flag", "yes", "number", "ten", "url", "not a url"), props(), Map.of(), props());

        assertThatThrownBy(() -> config.getBoolean("flag")).isInstanceOf(ConfigurationException.class);
        assertThatThrownBy(() -> config.getInt("number")).isInstanceOf(ConfigurationException.class);
        assertThatThrownBy(() -> config.getUrl("url")).isInstanceOf(ConfigurationException.class);
    }

    @Test
    void withPrefixCollectsKeysFromAllSourcesRespectingPrecedence() {
        FrameworkConfig config = config(Platform.ANDROID,
                props("capability.language", "en"),
                props("capability.appWaitActivity", "com.app.Main"),
                Map.of(),
                props("android.capability.language", "es", "ios.capability.locale", "CO"));

        assertThat(config.withPrefix("capability."))
                .containsEntry("appWaitActivity", "com.app.Main")
                .containsEntry("language", "es")
                .doesNotContainKey("locale");
    }

    @Test
    void environmentNamesAreDerivedFromKeys() {
        assertThat(FrameworkConfig.toEnvironmentName("ios.timeout.new-command.seconds"))
                .isEqualTo("IOS_TIMEOUT_NEW_COMMAND_SECONDS");
    }
}
