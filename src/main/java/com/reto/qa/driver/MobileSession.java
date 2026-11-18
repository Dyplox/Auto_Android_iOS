package com.reto.qa.driver;

import com.reto.qa.config.DeviceProfile;
import com.reto.qa.config.FrameworkSettings;
import com.reto.qa.config.Platform;
import com.reto.qa.core.Gestures;
import com.reto.qa.core.PlatformActions;
import com.reto.qa.core.Waiter;
import io.appium.java_client.AppiumDriver;

import java.util.Objects;

/**
 * Everything a Page Object needs from a session, injected through the constructor (DIP):
 * pages know nothing about singletons, ThreadLocals or global configuration.
 *
 * @param driver   the Appium driver
 * @param profile  the device and app under test
 * @param settings the typed framework settings the session was created with
 * @param waiter   explicit waits bound to this driver
 * @param gestures W3C gestures bound to this driver
 * @param actions  platform-specific actions (Strategy)
 */
public record MobileSession(AppiumDriver driver,
        DeviceProfile profile,
        FrameworkSettings settings,
        Waiter waiter,
        Gestures gestures,
        PlatformActions actions) implements AutoCloseable {

    /**
     * Validates that no component is missing.
     *
     * @param driver   the Appium driver
     * @param profile  the device and app under test
     * @param settings the typed framework settings the session was created with
     * @param waiter   explicit waits bound to this driver
     * @param gestures W3C gestures bound to this driver
     * @param actions  platform-specific actions (Strategy)
     * @throws NullPointerException if any component is {@code null}
     */
    public MobileSession {
        Objects.requireNonNull(driver, "driver");
        Objects.requireNonNull(profile, "profile");
        Objects.requireNonNull(settings, "settings");
        Objects.requireNonNull(waiter, "waiter");
        Objects.requireNonNull(gestures, "gestures");
        Objects.requireNonNull(actions, "actions");
    }

    /**
     * Returns the platform of this session.
     *
     * @return the platform
     */
    public Platform platform() {
        return profile.platform();
    }

    /** Ends the Appium session. */
    @Override
    public void close() {
        driver.quit();
    }
}
