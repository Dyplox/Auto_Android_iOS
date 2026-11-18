package com.reto.qa.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeviceProfileFactoryTest {

    /** Isolated directory per test (FIRST: Independent); JUnit deletes it afterwards. */
    @TempDir
    Path projectDir;

    @BeforeEach
    void createFakeApp() throws IOException {
        Files.createDirectories(projectDir.resolve("apps"));
        Files.createFile(projectDir.resolve("apps/demo.apk"));
    }

    private static FrameworkConfig config(Platform platform, String... keyValues) {
        Properties common = new Properties();
        common.setProperty("timeout.new-command.seconds", "120");
        common.setProperty("session.no-reset", "false");
        Properties platformFile = new Properties();
        for (int i = 0; i < keyValues.length; i += 2) {
            platformFile.setProperty(keyValues[i], keyValues[i + 1]);
        }
        return FrameworkConfig.of(platform, common, platformFile, Map.of(), new Properties());
    }

    private DeviceProfileFactory factory(Optional<String> simctlOutput) {
        return new DeviceProfileFactory(projectDir, new IosSimulatorLocator(() -> simctlOutput));
    }

    @Test
    void mapsConfigurationAndResolvesRelativeAppPath() {
        DeviceProfile profile = factory(Optional.empty()).create(config(Platform.ANDROID,
                "device.name", "Android Emulator",
                "app.path", "apps/demo.apk",
                "app.id", "com.wdiodemoapp",
                "capability.appWaitActivity", "com.wdiodemoapp.MainActivity",
                "capability.disableWindowAnimation", "true",
                "capability.systemPort", "8201"));

        assertThat(profile.app()).contains(projectDir.resolve("apps/demo.apk").toAbsolutePath().toString());
        assertThat(profile.newCommandTimeout().toSeconds()).isEqualTo(120);
        assertThat(profile.extraCapabilities())
                .containsEntry("appium:appWaitActivity", "com.wdiodemoapp.MainActivity")
                .containsEntry("appium:disableWindowAnimation", true)
                .containsEntry("appium:systemPort", 8201);
    }

    @Test
    void missingAppFailsFastPointingToDownloadScript() {
        DeviceProfileFactory factory = factory(Optional.empty());
        FrameworkConfig config = config(Platform.ANDROID,
                "device.name", "Android Emulator", "app.path", "apps/missing.apk", "app.id", "com.app");

        assertThatThrownBy(() -> factory.create(config))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("download-apps.sh");
    }

    @Test
    void remoteAppLocationsAreKeptAsIs() {
        DeviceProfileFactory factory = factory(Optional.empty());

        assertThat(factory.resolveApp("https://example.com/app.apk")).isEqualTo("https://example.com/app.apk");
        assertThat(factory.resolveApp("storage:filename=app.apk")).isEqualTo("storage:filename=app.apk");
        assertThat(factory.resolveApp("bs://f7c874f21852ba57957a3fdc33f47514288c4ba4"))
                .isEqualTo("bs://f7c874f21852ba57957a3fdc33f47514288c4ba4");
        assertThat(factory.resolveApp("WdioDemoApp-Android")).as("BrowserStack custom_id")
                .isEqualTo("WdioDemoApp-Android");
    }

    @Test
    void unsupportedPlatformInAnEnvironmentFailsWithTheConfiguredReason() {
        FrameworkConfig config = config(Platform.IOS, "unsupported.reason", "only real iPhones are available",
                "device.name", "iPhone", "app.id", "org.app", "platform.version", "26.0");

        DeviceProfileFactory factory = factory(Optional.empty());

        assertThatThrownBy(() -> factory.create(config))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("only real iPhones are available");
    }

    @Test
    void cloudOptionsAndSessionNameAreGroupedInTheVendorCapability() {
        DeviceProfile profile = factory(Optional.empty()).create(config(Platform.ANDROID,
                "device.name", "Google Pixel 9", "app.path", "WdioDemoApp-Android", "app.id", "com.wdiodemoapp",
                "cloud.options-capability", "bstack:options",
                "cloud.option.projectName", "Reto SDET",
                "cloud.session-name-option", "sessionName"), "LoginTest");

        assertThat(profile.extraCapabilities())
                .containsEntry("bstack:options", Map.of("projectName", "Reto SDET", "sessionName", "LoginTest"));
    }

    @Test
    void iosDoesNotPickALocalSimulatorWhenAutoSelectIsDisabled() {
        String simctl = "-- iOS 26.1 --\n    iPhone 17 (33333333-3333-3333-3333-333333333333) (Booted) \n";
        DeviceProfileFactory factory = factory(Optional.of(simctl));
        FrameworkConfig config = config(Platform.IOS, "device.name", "iPhone 17", "app.id", "org.wdiodemoapp",
                "device.simulator.auto-select", "false");

        assertThatThrownBy(() -> factory.create(config)).isInstanceOf(ConfigurationException.class);
    }

    @Test
    void iosPicksSimulatorAutomaticallyWhenNoVersionOrUdidConfigured() {
        String simctl = "-- iOS 26.1 --\n    iPhone 17 (33333333-3333-3333-3333-333333333333) (Shutdown) \n";

        DeviceProfile profile = factory(Optional.of(simctl)).create(config(Platform.IOS,
                "device.name", "iPhone 17", "app.id", "org.wdiodemoapp"));

        assertThat(profile.udid()).contains("33333333-3333-3333-3333-333333333333");
        assertThat(profile.deviceName()).isEqualTo("iPhone 17");
    }

    @Test
    void iosWithoutSimulatorsFailsWithHelpfulMessage() {
        DeviceProfileFactory factory = factory(Optional.empty());
        FrameworkConfig config = config(Platform.IOS, "device.name", "iPhone 17", "app.id", "org.wdiodemoapp");

        assertThatThrownBy(() -> factory.create(config))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("platform.version");
    }

    @Test
    void typedConvertsBooleansAndIntegersOnly() {
        assertThat(DeviceProfileFactory.typed("TRUE")).isEqualTo(true);
        assertThat(DeviceProfileFactory.typed("8200")).isEqualTo(8200);
        assertThat(DeviceProfileFactory.typed("26.0")).isEqualTo("26.0");
        assertThat(DeviceProfileFactory.typed("com.app.Main")).isEqualTo("com.app.Main");
    }
}
