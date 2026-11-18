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
 * Resuelve la URL del Appium server. Por defecto usa un server ya levantado ({@code appium.server.url});
 * con {@code appium.server.autostart=true} arranca uno local por JVM en un puerto libre (así Android e iOS pueden
 * correr en paralelo, cada uno en su JVM, sin chocar).
 * Si existe {@code node_modules/appium} en el proyecto se usa esa versión fijada en package.json.
 */
public final class AppiumServerManager {

    private static final Logger LOG = LoggerFactory.getLogger(AppiumServerManager.class);
    /** Ejecutable oficial del paquete appium (su "bin"); en Appium 4 build/lib/main.js solo se mantiene por compatibilidad. */
    private static final Path LOCAL_APPIUM_MAIN = Paths.get("node_modules", "appium", "index.js");

    private static AppiumDriverLocalService service;
    private static boolean shutdownHookRegistered;

    private AppiumServerManager() {
    }

    public static synchronized URL serverUrl(FrameworkConfig config) {
        if (!config.getBoolean("appium.server.autostart")) {
            return config.getUrl("appium.server.url");
        }
        if (service == null || !service.isRunning()) {
            service = buildService(config);
            LOG.info("Iniciando Appium server local (log en build/appium-server-{}.log)", config.platform().key());
            service.start();
            registerShutdownHook();
        }
        return service.getUrl();
    }

    public static synchronized void stop() {
        if (service != null && service.isRunning()) {
            LOG.info("Deteniendo Appium server local");
            service.stop();
        }
    }

    private static AppiumDriverLocalService buildService(FrameworkConfig config) {
        Path projectDir = Paths.get(System.getProperty("user.dir"));
        Path logFile = projectDir.resolve(Paths.get("build", "appium-server-" + config.platform().key() + ".log"));
        try {
            Files.createDirectories(logFile.getParent());
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo crear " + logFile.getParent(), e);
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
