package com.reto.qa.driver;

import com.reto.qa.config.FrameworkConfig;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;
import io.appium.java_client.service.local.flags.GeneralServerFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

/**
 * Resolves the Appium server URL. By default it uses a server that is already running ({@code appium.server.url});
 * with {@code appium.server.autostart=true} it starts one local server per JVM on a free port (so Android and iOS can
 * run in parallel, each in its own JVM, without clashing).
 * If {@code node_modules/appium} exists in the project, the version pinned in package.json is used.
 */
public final class AppiumServerManager {

    private static final Logger LOG = LoggerFactory.getLogger(AppiumServerManager.class);
    /** Official entry point of the appium package (its "bin"); in Appium 4, build/lib/main.js is kept only for compatibility. */
    private static final Path LOCAL_APPIUM_MAIN = Paths.get("node_modules", "appium", "index.js");

    private static AppiumDriverLocalService service;
    private static boolean shutdownHookRegistered;

    private AppiumServerManager() {
    }

    /**
     * Returns the URL of the Appium server to use, starting a local one first if autostart is enabled.
     *
     * @param config configuration for the current platform and environment
     * @return the server URL
     */
    public static synchronized URL serverUrl(FrameworkConfig config) {
        if (!config.getBoolean("appium.server.autostart")) {
            return config.getUrl("appium.server.url");
        }
        if (service == null || !service.isRunning()) {
            service = buildService(config);
            LOG.info("Starting local Appium server (log in build/appium-server-{}.log)", config.platform().key());
            service.start();
            registerShutdownHook();
        }
        return service.getUrl();
    }

    /** Stops the local Appium server, if this JVM started one. Also runs as a shutdown hook. */
    public static synchronized void stop() {
        if (service != null && service.isRunning()) {
            LOG.info("Stopping local Appium server");
            service.stop();
        }
    }

    private static AppiumDriverLocalService buildService(FrameworkConfig config) {
        Path projectDir = Paths.get(System.getProperty("user.dir"));
        Path logFile = projectDir.resolve(Paths.get("build", "appium-server-" + config.platform().key() + ".log"));
        try {
            Files.createDirectories(logFile.getParent());
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create " + logFile.getParent(), e);
        }

        AppiumServiceBuilder builder = new AppiumServiceBuilder()
                .withIPAddress("127.0.0.1")
                .usingAnyFreePort()
                .withArgument(GeneralServerFlag.LOG_LEVEL, "info")
                .withLogFile(logFile.toFile())
                .withTimeout(Duration.ofSeconds(60));

        Path localAppium = projectDir.resolve(LOCAL_APPIUM_MAIN);
        if (Files.exists(localAppium)) {
            builder.withAppiumJS(localAppium.toFile());
        }
        return AppiumDriverLocalService.buildService(builder);
    }

    private static void registerShutdownHook() {
        if (!shutdownHookRegistered) {
            Runtime.getRuntime().addShutdownHook(new Thread(AppiumServerManager::stop, "appium-server-shutdown"));
            shutdownHookRegistered = true;
        }
    }
}
