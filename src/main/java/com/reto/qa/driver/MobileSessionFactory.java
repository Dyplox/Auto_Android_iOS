package com.reto.qa.driver;

import com.reto.qa.config.DeviceProfile;
import com.reto.qa.config.DeviceProfileFactory;
import com.reto.qa.config.FrameworkConfig;
import com.reto.qa.config.FrameworkSettings;
import com.reto.qa.config.TimeoutSettings;
import com.reto.qa.core.Gestures;
import com.reto.qa.core.Waiter;
import io.appium.java_client.AppiumDriver;
import org.jspecify.annotations.Nullable;
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
     * Starts a session. The settings and the device profile are validated before any driver is created; if anything
     * fails after the driver is created, the driver is quit before rethrowing.
     *
     * @param config      configuration for one platform and environment
     * @param server      URL of the Appium server to open the session on (see {@link AppiumServer})
     * @param sessionName session name on the cloud provider's dashboard (for example the test class);
     *                    optional, only used when the environment supports it
     * @return the started session
     * @throws com.reto.qa.config.ConfigurationException if the configuration is invalid
     */
    public MobileSession start(FrameworkConfig config, URL server, @Nullable String sessionName) {
        FrameworkSettings settings = FrameworkSettings.from(config);
        DeviceProfile profile = profileFactory.create(config, sessionName);
        PlatformProvider provider = providers.forPlatform(profile.platform());

        LOG.info("Opening session [{}] in the '{}' environment ({})", profile, config.environment(), server.getHost());
        AppiumDriver driver = provider.createDriver(server, profile);
        try {
            TimeoutSettings timeouts = settings.timeouts();
            Waiter waiter = new Waiter(driver, timeouts.explicit(), timeouts.polling());
            return new MobileSession(driver, profile, settings, waiter, new Gestures(driver),
                    provider.createActions(driver, profile, waiter));
        } catch (RuntimeException e) {
            driver.quit();
            throw e;
        }
    }
}
