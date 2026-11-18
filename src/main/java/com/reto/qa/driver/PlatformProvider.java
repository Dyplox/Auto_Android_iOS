package com.reto.qa.driver;

import com.reto.qa.config.DeviceProfile;
import com.reto.qa.config.Platform;
import com.reto.qa.core.PlatformActions;
import com.reto.qa.core.Waiter;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.Capabilities;

import java.net.URL;

/**
 * Abstract Factory per platform: creates the "family" of OS-dependent objects (capabilities, driver and
 * platform-specific actions). Adding a platform or a cloud provider means adding an implementation without changing
 * the rest of the framework (OCP).
 */
public interface PlatformProvider {

    /**
     * Returns the platform this provider serves.
     *
     * @return the platform
     */
    Platform platform();

    /**
     * Builds the W3C capabilities for a profile.
     *
     * @param profile device and app under test; must belong to {@link #platform()}
     * @return the capabilities
     * @throws IllegalArgumentException if the profile belongs to another platform
     */
    Capabilities capabilities(DeviceProfile profile);

    /**
     * Opens a new Appium session.
     *
     * @param serverUrl Appium server URL
     * @param profile   device and app under test
     * @return the platform-specific driver
     */
    AppiumDriver createDriver(URL serverUrl, DeviceProfile profile);

    /**
     * Creates the platform-specific actions for a driver created by this provider.
     *
     * @param driver  driver returned by {@link #createDriver(URL, DeviceProfile)}
     * @param profile device and app under test
     * @param waiter  explicit waits bound to the driver
     * @return the platform actions
     * @throws IllegalArgumentException if the driver is not of the expected type
     */
    PlatformActions createActions(AppiumDriver driver, DeviceProfile profile, Waiter waiter);

    /**
     * Guards against using this provider with a profile of another platform.
     *
     * @param profile profile to check
     * @throws IllegalArgumentException if the platforms differ
     */
    default void requireSamePlatform(DeviceProfile profile) {
        if (profile.platform() != platform()) {
            throw new IllegalArgumentException(getClass().getSimpleName() + " cannot create "
                    + profile.platform().w3cName() + " sessions");
        }
    }
}
