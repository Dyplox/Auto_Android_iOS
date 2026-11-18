package com.reto.qa.driver;

import com.reto.qa.config.DeviceProfile;
import com.reto.qa.config.DeviceProfileFactory;
import com.reto.qa.config.FrameworkConfig;
import com.reto.qa.core.Gestures;
import com.reto.qa.core.Waiter;
import io.appium.java_client.AppiumDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Objects;

/** Orquesta la creación de una {@link MobileSession}; cada pieza la construye su propio responsable. */
public final class MobileSessionFactory {

    private static final Logger LOG = LoggerFactory.getLogger(MobileSessionFactory.class);

    private final PlatformProviders providers;
    private final DeviceProfileFactory profileFactory;

    public MobileSessionFactory() {
        this(PlatformProviders.defaults(), new DeviceProfileFactory());
    }

    public MobileSessionFactory(PlatformProviders providers, DeviceProfileFactory profileFactory) {
        this.providers = Objects.requireNonNull(providers, "providers");
        this.profileFactory = Objects.requireNonNull(profileFactory, "profileFactory");
    }

    public MobileSession start(FrameworkConfig config) {
        return start(config, null);
    }

    /**
     * @param sessionName nombre de la sesión en el dashboard del proveedor cloud (p. ej. la clase de prueba);
     *                    opcional, solo se usa si el ambiente lo soporta
     */
    public MobileSession start(FrameworkConfig config, String sessionName) {
        DeviceProfile profile = profileFactory.create(config, sessionName);
        PlatformProvider provider = providers.forPlatform(profile.platform());
        URL server = AppiumServerManager.serverUrl(config);

        LOG.info("Abriendo sesión [{}] en el ambiente '{}' ({})", profile, config.environment(), server.getHost());
        AppiumDriver driver = provider.createDriver(server, profile);
        try {
            Waiter waiter = new Waiter(driver,
                    config.getSeconds("timeout.explicit.seconds"),
                    config.getMillis("timeout.polling.millis"));
            return new MobileSession(driver, profile, config, waiter, new Gestures(driver),
                    provider.createActions(driver, profile, waiter));
        } catch (RuntimeException e) {
            driver.quit();
            throw e;
        }
    }
}
