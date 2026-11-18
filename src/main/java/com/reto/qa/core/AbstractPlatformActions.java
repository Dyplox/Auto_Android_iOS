package com.reto.qa.core;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.HasOnScreenKeyboard;
import io.appium.java_client.InteractsWithApps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Behaviour shared by both platforms. The generic bound guarantees at compile time that the driver can manage apps and
 * the keyboard, so any subclass is substitutable (LSP).
 *
 * @param <D> the platform-specific driver type
 */
abstract class AbstractPlatformActions<D extends AppiumDriver & InteractsWithApps & HasOnScreenKeyboard>
        implements PlatformActions {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractPlatformActions.class);

    /** The platform-specific driver. */
    protected final D driver;
    /** {@code appPackage} on Android, {@code bundleId} on iOS. */
    protected final String appId;

    /**
     * Creates the actions for one driver and app.
     *
     * @param driver the platform-specific driver
     * @param appId  {@code appPackage} on Android, {@code bundleId} on iOS
     */
    protected AbstractPlatformActions(D driver, String appId) {
        this.driver = Objects.requireNonNull(driver, "driver");
        this.appId = Objects.requireNonNull(appId, "appId");
    }

    @Override
    public void restartApp() {
        LOG.debug("Restarting {}", appId);
        driver.terminateApp(appId);
        driver.activateApp(appId);
    }

    /**
     * Checks whether the on-screen keyboard is visible.
     *
     * @return {@code true} if the keyboard is shown
     */
    protected boolean isKeyboardShown() {
        return driver.isKeyboardShown();
    }
}
