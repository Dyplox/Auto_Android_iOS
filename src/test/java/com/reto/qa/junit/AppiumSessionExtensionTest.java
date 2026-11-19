package com.reto.qa.junit;

import com.reto.qa.config.DeviceProfile;
import com.reto.qa.config.Platform;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.MutableCapabilities;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AppiumSessionExtensionTest {

    private static DeviceProfile.Builder pixel() {
        return DeviceProfile.builder(Platform.ANDROID).deviceName("Pixel 9").appId("com.app");
    }

    @Test
    void sessionVersionWinsOverTheConfiguredOne() {
        DeviceProfile profile = pixel().platformVersion("14").build();

        String label = AppiumSessionExtension.deviceLabel(profile,
                new MutableCapabilities(Map.of("platformVersion", "15")));

        assertThat(label).isEqualTo("Pixel 9 · 15");
    }

    @Test
    void prefixedSessionVersionIsAccepted() {
        String label = AppiumSessionExtension.deviceLabel(pixel().build(),
                new MutableCapabilities(Map.of("appium:platformVersion", "15")));

        assertThat(label).isEqualTo("Pixel 9 · 15");
    }

    @Test
    void configuredVersionIsUsedWhenTheSessionHasNone() {
        String label = AppiumSessionExtension.deviceLabel(pixel().platformVersion("14").build(),
                new MutableCapabilities());

        assertThat(label).isEqualTo("Pixel 9 · 14");
    }

    @Test
    void onlyTheNameWhenNoVersionIsKnown() {
        String label = AppiumSessionExtension.deviceLabel(pixel().build(),
                new MutableCapabilities(Map.of("platformVersion", " ")));

        assertThat(label).isEqualTo("Pixel 9");
    }
}
