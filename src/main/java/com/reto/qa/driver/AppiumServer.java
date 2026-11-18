package com.reto.qa.driver;

import com.reto.qa.config.Platform;
import com.reto.qa.config.ServerSettings;

import java.net.URL;

/**
 * The Appium server a session talks to. It is a plain object with an explicit lifecycle, not global state: whoever
 * opens it closes it. In the test suite, the JUnit extension keeps one per test JVM in the root
 * {@code ExtensionContext.Store}, which closes it when the run ends.
 */
public interface AppiumServer extends AutoCloseable {

    /**
     * Returns the URL that sessions must use.
     *
     * @return the server URL
     */
    URL url();

    /** Releases the server: stops it if this JVM started it, otherwise does nothing. */
    @Override
    void close();

    /**
     * Returns the server described by the settings: a new local server on a free port when
     * {@code appium.server.autostart=true}, otherwise the server that is already running at {@code appium.server.url}.
     *
     * @param settings server settings
     * @param platform platform the server is for (used to name its log file)
     * @return the server, already running
     */
    static AppiumServer from(ServerSettings settings, Platform platform) {
        return settings.autostart() ? LocalAppiumServer.start(platform) : new RemoteAppiumServer(settings.url());
    }
}
