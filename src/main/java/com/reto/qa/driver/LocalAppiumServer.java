package com.reto.qa.driver;

import com.reto.qa.config.Platform;
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
import java.util.Objects;

/**
 * An Appium server started by this JVM on a free port, so Android and iOS can run in parallel, each in its own JVM,
 * without clashing. If {@code node_modules/appium} exists in the project, the version pinned in package.json is used.
 * The server log goes to {@code build/appium-server-<platform>.log}.
 */
public final class LocalAppiumServer implements AppiumServer {

    private static final Logger LOG = LoggerFactory.getLogger(LocalAppiumServer.class);
    /** Official entry point of the appium package (its "bin"); in Appium 4, build/lib/main.js is kept only for compatibility. */
    private static final Path LOCAL_APPIUM_MAIN = Paths.get("node_modules", "appium", "index.js");
    private static final Duration STARTUP_TIMEOUT = Duration.ofSeconds(60);

    private final AppiumDriverLocalService service;

    private LocalAppiumServer(AppiumDriverLocalService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    /**
     * Starts a local server for a platform.
     *
     * @param platform platform the server is for (used to name its log file)
     * @return the running server
     */
    public static LocalAppiumServer start(Platform platform) {
        Path projectDir = Paths.get(System.getProperty("user.dir"));
        Path logFile = projectDir.resolve(Paths.get("build", "appium-server-" + platform.key() + ".log"));
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
                .withTimeout(STARTUP_TIMEOUT);
        Path localAppium = projectDir.resolve(LOCAL_APPIUM_MAIN);
        if (Files.exists(localAppium)) {
            builder.withAppiumJS(localAppium.toFile());
        }

        AppiumDriverLocalService service = AppiumDriverLocalService.buildService(builder);
        LOG.info("Starting local Appium server (log in {})", logFile);
        service.start();
        return new LocalAppiumServer(service);
    }

    @Override
    public URL url() {
        return service.getUrl();
    }

    /** Stops the server if it is still running. */
    @Override
    public void close() {
        if (service.isRunning()) {
            LOG.info("Stopping local Appium server");
            service.stop();
        }
    }
}
