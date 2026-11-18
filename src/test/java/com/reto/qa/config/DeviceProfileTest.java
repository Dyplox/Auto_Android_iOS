package com.reto.qa.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeviceProfileTest {

    @Test
    void buildsImmutableProfileWithDefaults() {
        DeviceProfile profile = DeviceProfile.builder(Platform.ANDROID)
                .deviceName("Pixel 9")
                .appId("com.wdiodemoapp")
                .app("/apps/demo.apk")
                .capability("appWaitActivity", "com.wdiodemoapp.MainActivity")
                .capability("myvendor:flag", true)
                .build();

        assertThat(profile.platform()).isEqualTo(Platform.ANDROID);
        assertThat(profile.platformVersion()).isEmpty();
        assertThat(profile.udid()).isEmpty();
        assertThat(profile.app()).contains("/apps/demo.apk");
        assertThat(profile.newCommandTimeout()).isEqualTo(Duration.ofSeconds(240));
        assertThat(profile.extraCapabilities())
                .containsEntry("appium:appWaitActivity", "com.wdiodemoapp.MainActivity")
                .containsEntry("myvendor:flag", true);
        assertThatThrownBy(() -> profile.extraCapabilities().put("x", 1))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void cloudOptionsAreGroupedUnderTheirVendorCapability() {
        DeviceProfile profile = DeviceProfile.builder(Platform.ANDROID)
                .deviceName("Google Pixel 9")
                .appId("com.wdiodemoapp")
                .capability("bstack:options.userName", "qa")
                .capability("bstack:options.debug", true)
                .capability("appWaitActivity", "com.wdiodemoapp.MainActivity")
                .build();

        assertThat(profile.extraCapabilities())
                .containsEntry("bstack:options", java.util.Map.of("userName", "qa", "debug", true))
                .containsEntry("appium:appWaitActivity", "com.wdiodemoapp.MainActivity");
    }

    @Test
    void sameCapabilityCannotBeSimpleAndGroupedAtOnce() {
        DeviceProfile.Builder builder = DeviceProfile.builder(Platform.ANDROID).deviceName("Pixel").appId("com.app")
                .capability("sauce:options", "x")
                .capability("sauce:options.name", "y");

        assertThatThrownBy(builder::build).isInstanceOf(ConfigurationException.class).hasMessageContaining("sauce:options");
    }

    @Test
    void blankOptionalValuesAreTreatedAsAbsent() {
        DeviceProfile profile = DeviceProfile.builder(Platform.ANDROID)
                .deviceName("Android Emulator").appId("com.app").platformVersion(" ").udid("").build();

        assertThat(profile.platformVersion()).isEmpty();
        assertThat(profile.udid()).isEmpty();
    }

    @Test
    void deviceNameIsMandatory() {
        assertThatThrownBy(() -> DeviceProfile.builder(Platform.ANDROID).appId("com.app").build())
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("device.name");
    }

    @Test
    void appIdIsMandatory() {
        assertThatThrownBy(() -> DeviceProfile.builder(Platform.ANDROID).deviceName("Pixel").build())
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("app.id");
    }

    @Test
    void iosRequiresUdidOrPlatformVersion() {
        assertThatThrownBy(() -> DeviceProfile.builder(Platform.IOS).deviceName("iPhone 17").appId("org.app").build())
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("xcrun simctl");

        assertThat(DeviceProfile.builder(Platform.IOS).deviceName("iPhone 17").appId("org.app")
                .platformVersion("26.0").build().platformVersion()).contains("26.0");
    }
}
