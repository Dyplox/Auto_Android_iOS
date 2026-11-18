package com.reto.qa.core;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.HasOnScreenKeyboard;
import io.appium.java_client.InteractsWithApps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Comportamiento común a ambas plataformas. El bound genérico garantiza en compilación que el driver
 * sabe gestionar apps y teclado, así cualquier subclase es sustituible (LSP).
 */
abstract class AbstractPlatformActions<D extends AppiumDriver & InteractsWithApps & HasOnScreenKeyboard>
        implements PlatformActions {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractPlatformActions.class);

    protected final D driver;
    protected final String appId;

    protected AbstractPlatformActions(D driver, String appId) {
        this.driver = Objects.requireNonNull(driver, "driver");
        this.appId = Objects.requireNonNull(appId, "appId");
    }

    @Override
    public void restartApp() {
        LOG.debug("Reiniciando {}", appId);
        driver.terminateApp(appId);
        driver.activateApp(appId);
    }

    protected boolean isKeyboardShown() {
        return driver.isKeyboardShown();
    }
}
