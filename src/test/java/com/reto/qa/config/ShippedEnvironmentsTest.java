package com.reto.qa.config;

import com.reto.qa.driver.PlatformProviders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openqa.selenium.Capabilities;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.MAP;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Valida los archivos de configuración que se entregan (src/test/resources/config): cada combinación
 * ambiente × plataforma debe producir capabilities completas. Detecta un .properties roto antes de pagar una
 * sesión en un dispositivo o en la nube.
 */
class ShippedEnvironmentsTest {

    private static final Map<String, String> FAKE_SECRETS = Map.of(
            "BROWSERSTACK_USERNAME", "bs-user", "BROWSERSTACK_ACCESS_KEY", "bs-key",
            "SAUCE_USERNAME", "sauce-user", "SAUCE_ACCESS_KEY", "sauce-key",
            "BUILD_NAME", "build-42");

    @TempDir
    Path projectDir;

    @BeforeEach
    void createFakeApps() throws IOException {
        Files.createDirectories(projectDir.resolve("apps"));
        Files.createFile(projectDir.resolve("apps/android.wdio.native.app.v2.2.0.apk"));
        Files.createFile(projectDir.resolve("apps/ios.simulator.wdio.native.app.v2.2.0.zip"));
    }

    private Capabilities capabilitiesFor(Platform platform, String environment) {
        FrameworkConfig config = FrameworkConfig.of(platform, environment,
                FrameworkConfig.loadLayers(platform, environment), FAKE_SECRETS, new Properties());
        String simctl = "-- iOS 26.1 --\n    iPhone 17 (33333333-3333-3333-3333-333333333333) (Booted) \n";
        DeviceProfile profile = new DeviceProfileFactory(projectDir, new IosSimulatorLocator(() -> Optional.of(simctl)))
                .create(config, "LoginTest");
        return PlatformProviders.defaults().forPlatform(platform).capabilities(profile);
    }

    @ParameterizedTest(name = "{0} / {1}")
    @CsvSource({"local, ANDROID", "local, IOS", "ci, ANDROID", "ci, IOS"})
    void localEnvironmentsUseTheDownloadedAppFiles(String environment, Platform platform) {
        Capabilities capabilities = capabilitiesFor(platform, environment);

        assertThat((String) capabilities.getCapability("appium:app")).startsWith(projectDir.toString());
        assertThat(capabilities.getCapability("bstack:options")).isNull();
        assertThat(capabilities.getCapability("sauce:options")).isNull();
    }

    @Test
    void ciEnvironmentStartsItsOwnAppiumAndWaitsLonger() {
        FrameworkConfig ci = FrameworkConfig.of(Platform.IOS, "ci", FrameworkConfig.loadLayers(Platform.IOS, "ci"),
                Map.of(), new Properties());

        assertThat(ci.getBoolean("appium.server.autostart")).isTrue();
        assertThat(ci.getInt("timeout.explicit.seconds")).isGreaterThan(20);
        assertThat(ci.withPrefix("capability.")).containsKey("wdaLaunchTimeout");
    }

    @Test
    void browserStackAndroidSendsCredentialsInsideBstackOptions() {
        Capabilities capabilities = capabilitiesFor(Platform.ANDROID, "browserstack");

        assertThat(capabilities.getCapability("appium:app")).isEqualTo("WdioDemoApp-Android");
        assertThat(capabilities.getCapability("bstack:options")).asInstanceOf(MAP)
                .containsEntry("userName", "bs-user")
                .containsEntry("accessKey", "bs-key")
                .containsEntry("buildName", "build-42")
                .containsEntry("sessionName", "LoginTest")
                .containsEntry("debug", true);
    }

    @Test
    void browserStackIosFailsFastExplainingWhy() {
        assertThatThrownBy(() -> capabilitiesFor(Platform.IOS, "browserstack"))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("saucelabs");
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({"ANDROID, storage:filename=android.wdio.native.app.v2.2.0.apk",
            "IOS,     storage:filename=ios.simulator.wdio.native.app.v2.2.0.zip"})
    void sauceLabsUsesAppStorageAndSauceOptions(Platform platform, String app) {
        Capabilities capabilities = capabilitiesFor(platform, "saucelabs");

        assertThat(capabilities.getCapability("appium:app")).isEqualTo(app);
        assertThat(capabilities.getCapability("appium:udid")).as("en la nube no se elige simulador local").isNull();
        assertThat(capabilities.getCapability("sauce:options")).asInstanceOf(MAP)
                .containsEntry("username", "sauce-user")
                .containsEntry("accessKey", "sauce-key")
                .containsEntry("name", "LoginTest")
                .containsEntry("appiumVersion", "stable");
    }

    @Test
    void sauceLabsHubUrlUsesTheConfiguredRegion() {
        FrameworkConfig config = FrameworkConfig.of(Platform.IOS, "saucelabs",
                FrameworkConfig.loadLayers(Platform.IOS, "saucelabs"), Map.of("SAUCE_REGION", "eu-central-1"),
                new Properties());

        assertThat(config.getUrl("appium.server.url").getHost()).isEqualTo("ondemand.eu-central-1.saucelabs.com");
    }
}
