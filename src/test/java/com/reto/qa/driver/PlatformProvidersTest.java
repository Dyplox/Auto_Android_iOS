package com.reto.qa.driver;

import com.reto.qa.config.DeviceProfile;
import com.reto.qa.config.Platform;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.ios.options.XCUITestOptions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies, without a device or a server, that the same {@link DeviceProfile} is turned into the right capabilities
 * for each platform.
 */
class PlatformProvidersTest {

    private static DeviceProfile.Builder android() {
        return DeviceProfile.builder(Platform.ANDROID)
                .deviceName("Android Emulator")
                .appId("com.wdiodemoapp")
                .newCommandTimeout(Duration.ofSeconds(90));
    }

    private static DeviceProfile.Builder ios() {
        return DeviceProfile.builder(Platform.IOS)
                .deviceName("iPhone 17")
                .platformVersion("26.1")
                .appId("org.wdiodemoapp");
    }

    @Test
    void defaultRegistryResolvesBothPlatforms() {
        PlatformProviders providers = PlatformProviders.defaults();

        assertThat(providers.forPlatform(Platform.ANDROID)).isInstanceOf(AndroidPlatformProvider.class);
        assertThat(providers.forPlatform(Platform.IOS)).isInstanceOf(IosPlatformProvider.class);
    }

    @Test
    void duplicatedProvidersAreRejected() {
        assertThatThrownBy(
                () -> new PlatformProviders(List.of(new AndroidPlatformProvider(), new AndroidPlatformProvider())))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void androidCapabilitiesUseUiAutomator2AndTheAppFile() {
        UiAutomator2Options options = new AndroidPlatformProvider().capabilities(android()
                .app("/apps/demo.apk")
                .capability("appWaitActivity", "com.wdiodemoapp.MainActivity")
                .build());

        assertThat(options.getPlatformName()).hasToString("ANDROID");
        assertThat(options.getCapability("appium:automationName")).isEqualTo(Platform.ANDROID.automationName());
        assertThat(options.getCapability("appium:deviceName")).isEqualTo("Android Emulator");
        assertThat(options.getCapability("appium:app")).isEqualTo("/apps/demo.apk");
        assertThat(options.getCapability("appium:autoGrantPermissions")).isEqualTo(true);
        assertThat(options.getCapability("appium:newCommandTimeout")).isEqualTo(90L);
        assertThat(options.getCapability("appium:appWaitActivity")).isEqualTo("com.wdiodemoapp.MainActivity");
        assertThat(options.getCapability("appium:appPackage")).isNull();
    }

    @Test
    void androidWithoutAppFileLaunchesInstalledPackage() {
        UiAutomator2Options options = new AndroidPlatformProvider().capabilities(android().build());

        assertThat(options.getCapability("appium:appPackage")).isEqualTo("com.wdiodemoapp");
        assertThat(options.getCapability("appium:app")).isNull();
    }

    @Test
    void iosCapabilitiesUseXcuiTestAndBundleId() {
        XCUITestOptions options = new IosPlatformProvider().capabilities(ios().udid("ABC").build());

        assertThat(options.getPlatformName()).hasToString("IOS");
        assertThat(options.getCapability("appium:automationName")).isEqualTo(Platform.IOS.automationName());
        assertThat(options.getCapability("appium:platformVersion")).isEqualTo("26.1");
        assertThat(options.getCapability("appium:udid")).isEqualTo("ABC");
        assertThat(options.getCapability("appium:bundleId")).isEqualTo("org.wdiodemoapp");
    }

    @Test
    void providerRefusesProfilesOfAnotherPlatform() {
        assertThatThrownBy(() -> new IosPlatformProvider().capabilities(android().build()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
