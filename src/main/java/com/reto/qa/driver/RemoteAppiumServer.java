package com.reto.qa.driver;

import java.net.URL;
import java.util.Objects;

/**
 * A server this JVM does not own: a local Appium started with {@code npm run appium}, a grid or a cloud hub.
 * Closing it does nothing.
 *
 * @param url the server URL
 */
public record RemoteAppiumServer(URL url) implements AppiumServer {

    /**
     * Validates the URL.
     *
     * @param url the server URL
     */
    public RemoteAppiumServer {
        Objects.requireNonNull(url, "url");
    }

    /** Does nothing: this JVM did not start the server. */
    @Override
    public void close() {
        // Not owned by this JVM.
    }
}
