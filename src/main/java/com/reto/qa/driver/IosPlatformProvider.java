package com.reto.qa.driver;

import com.reto.qa.config.DeviceProfile;
import com.reto.qa.config.Platform;
import com.reto.qa.core.IosPlatformActions;
import com.reto.qa.core.PlatformActions;
import com.reto.qa.core.Waiter;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.options.XCUITestOptions;

import java.net.URL;

public final class IosPlatformProvider implements PlatformProvider {

    @Override
    public Platform platform() {
        return Platform.IOS;
    }

    /** Builder tipado de Appium: platformName=iOS y automationName=XCUITest vienen por defecto. */
    @Override
    public XCUITestOptions capabilities(DeviceProfile profile) {
        requireSamePlatform(profile);
        XCUITestOptions options = new XCUITestOptions()
                .setDeviceName(profile.deviceName())
                .setNewCommandTimeout(profile.newCommandTimeout())
                .setNoReset(profile.noReset());
        profile.platformVersion().ifPresent(options::setPlatformVersion);
        profile.udid().ifPresent(options::setUdid);
        profile.app().ifPresentOrElse(options::setApp, () -> options.setBundleId(profile.appId()));
        profile.extraCapabilities().forEach(options::amend);
        return options;
    }

    @Override
    public AppiumDriver createDriver(URL serverUrl, DeviceProfile profile) {
        return new IOSDriver(serverUrl, capabilities(profile));
    }

    @Override
    public PlatformActions createActions(AppiumDriver driver, DeviceProfile profile, Waiter waiter) {
        if (!(driver instanceof IOSDriver iosDriver)) {
            throw new IllegalArgumentException("Se esperaba IOSDriver y llegó " + driver.getClass().getName());
        }
        return new IosPlatformActions(iosDriver, profile.appId(), waiter);
    }
}
