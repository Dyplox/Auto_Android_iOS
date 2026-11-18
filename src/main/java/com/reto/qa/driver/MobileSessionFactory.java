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

/** Orchestrates the creation of a {@link MobileSession}; each part is built by the class responsible for it. */
public final class MobileSessionFactory {

    private static final Logger LOG = LoggerFactory.getLogger(MobileSessionFactory.class);

    private final PlatformProviders providers;
    private final DeviceProfileFactory profileFactory;

    /** Creates a factory with the default Android and iOS providers. */
    public MobileSessionFactory() {
        this(PlatformProviders.defaults(), new DeviceProfileFactory());
    }

    /**
     * Creates a factory with explicit collaborators.
     *
     * @param providers      registry of platform providers
     * @param profileFactory turns configuration into a device profile
     */
    public MobileSessionFactory(PlatformProviders providers, DeviceProfileFactory profileFactory) {
        this.providers = Objects.requireNonNull(providers, "providers");
        this.profileFactory = Objects.requireNonNull(profileFactory, "profileFactory");
    }

    /**
     * Starts a session without a cloud session name.
     *
     * @param config configuration for one platform and environment
     * @return the started session
     */
    public MobileSession start(FrameworkConfig config) {
        return start(config, null);
    }

    /**
     * Starts a session. If anything fails after the driver is created, the driver is quit before rethrowing.
     *
     * @param config      configuration for one platform and environment
     * @param sessionName session name on the cloud provider's dashboard (for example the test class);
     *                    optional, only used when the environment supports it
     * @return the started session
     */
    public MobileSession start(FrameworkConfig config, String sessionName) {
        DeviceProfile profile = profileFactory.create(config, sessionName);
        PlatformProvider provider = providers.forPlatform(profile.platform());
        URL server = AppiumServerManager.serverUrl(config);

        LOG.info("Opening session [{}] in the '{}' environment ({})", profile, config.environment(), server.getHost());
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
