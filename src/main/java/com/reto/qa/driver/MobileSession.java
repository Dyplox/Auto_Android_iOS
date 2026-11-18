package com.reto.qa.driver;

import com.reto.qa.config.DeviceProfile;
import com.reto.qa.config.FrameworkConfig;
import com.reto.qa.config.Platform;
import com.reto.qa.core.Gestures;
import com.reto.qa.core.PlatformActions;
import com.reto.qa.core.Waiter;
import io.appium.java_client.AppiumDriver;

import java.util.Objects;

/**
 * Todo lo que un Page Object necesita de una sesión, inyectado por constructor (DIP):
 * los pages no conocen singletons, ThreadLocals ni configuración global.
 */
public record MobileSession(AppiumDriver driver,
                            DeviceProfile profile,
                            FrameworkConfig config,
                            Waiter waiter,
                            Gestures gestures,
                            PlatformActions actions) implements AutoCloseable {

    public MobileSession {
        Objects.requireNonNull(driver, "driver");
        Objects.requireNonNull(profile, "profile");
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(waiter, "waiter");
        Objects.requireNonNull(gestures, "gestures");
        Objects.requireNonNull(actions, "actions");
    }

    public Platform platform() {
        return profile.platform();
    }

    @Override
    public void close() {
        driver.quit();
    }
}
