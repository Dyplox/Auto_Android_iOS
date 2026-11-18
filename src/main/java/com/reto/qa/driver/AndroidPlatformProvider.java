package com.reto.qa.driver;

import com.reto.qa.config.DeviceProfile;
import com.reto.qa.config.Platform;
import com.reto.qa.core.AndroidPlatformActions;
import com.reto.qa.core.PlatformActions;
import com.reto.qa.core.Waiter;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;

import java.net.URL;

/** {@link PlatformProvider} for Android: UiAutomator2 capabilities, {@link AndroidDriver} and Android actions. */
public final class AndroidPlatformProvider implements PlatformProvider {

    /** Creates the Android provider. */
    public AndroidPlatformProvider() {
        // Stateless.
    }

    @Override
    public Platform platform() {
        return Platform.ANDROID;
    }

    /** Uses Appium's typed builder: {@code platformName=Android} and {@code automationName=UiAutomator2} are preset. */
    @Override
    public UiAutomator2Options capabilities(DeviceProfile profile) {
        requireSamePlatform(profile);
        UiAutomator2Options options = new UiAutomator2Options()
                .setDeviceName(profile.deviceName())
                .setNewCommandTimeout(profile.newCommandTimeout())
                .setNoReset(profile.noReset())
                .setAutoGrantPermissions(true);
        profile.platformVersion().ifPresent(options::setPlatformVersion);
        profile.udid().ifPresent(options::setUdid);
        profile.app().ifPresentOrElse(options::setApp, () -> options.setAppPackage(profile.appId()));
        profile.extraCapabilities().forEach(options::amend);
        return options;
    }

    @Override
    public AppiumDriver createDriver(URL serverUrl, DeviceProfile profile) {
        return new AndroidDriver(serverUrl, capabilities(profile));
    }

    @Override
    public PlatformActions createActions(AppiumDriver driver, DeviceProfile profile, Waiter waiter) {
        if (!(driver instanceof AndroidDriver androidDriver)) {
            throw new IllegalArgumentException("Expected an AndroidDriver but got " + driver.getClass().getName());
        }
        return new AndroidPlatformActions(androidDriver, profile.appId());
    }
}
