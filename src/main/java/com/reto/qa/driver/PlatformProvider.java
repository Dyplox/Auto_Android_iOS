package com.reto.qa.driver;

import com.reto.qa.config.DeviceProfile;
import com.reto.qa.config.Platform;
import com.reto.qa.core.PlatformActions;
import com.reto.qa.core.Waiter;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.Capabilities;

import java.net.URL;

/**
 * Abstract Factory por plataforma: crea la "familia" de objetos que dependen del SO
 * (capabilities, driver y acciones específicas). Agregar una plataforma o un proveedor cloud
 * es crear una implementación nueva sin modificar el resto del framework (OCP).
 */
public interface PlatformProvider {

    Platform platform();

    Capabilities capabilities(DeviceProfile profile);

    AppiumDriver createDriver(URL serverUrl, DeviceProfile profile);

    PlatformActions createActions(AppiumDriver driver, DeviceProfile profile, Waiter waiter);

    default void requireSamePlatform(DeviceProfile profile) {
        if (profile.platform() != platform()) {
            throw new IllegalArgumentException(getClass().getSimpleName() + " no puede crear sesiones "
                    + profile.platform().w3cName());
        }
    }
}
