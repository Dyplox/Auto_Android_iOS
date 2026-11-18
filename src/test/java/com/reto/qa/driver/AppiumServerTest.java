package com.reto.qa.driver;

import com.reto.qa.config.Platform;
import com.reto.qa.config.ServerSettings;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/** Choosing the server needs no Appium: a running server is only referenced, never started. */
class AppiumServerTest {

    @Test
    void withoutAutostartTheConfiguredServerIsUsedAsIs() throws Exception {
        URL hub = URI.create("https://hub-cloud.browserstack.com/wd/hub").toURL();

        try (AppiumServer server = AppiumServer.from(new ServerSettings(false, hub), Platform.ANDROID)) {
            assertThat(server).isInstanceOf(RemoteAppiumServer.class);
            assertThat(server.url()).isEqualTo(hub);
        }
    }

    @Test
    void closingARemoteServerIsANoOp() throws Exception {
        AppiumServer server = new RemoteAppiumServer(URI.create("http://127.0.0.1:4723").toURL());

        assertThatCode(server::close).doesNotThrowAnyException();
        assertThatCode(server::close).as("idempotent").doesNotThrowAnyException();
    }
}
