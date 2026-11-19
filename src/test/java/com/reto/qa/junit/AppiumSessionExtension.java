package com.reto.qa.junit;

import com.reto.qa.app.WdioDemoApp;
import com.reto.qa.config.DeviceProfile;
import com.reto.qa.config.FrameworkConfig;
import com.reto.qa.config.FrameworkSettings;
import com.reto.qa.config.Platform;
import com.reto.qa.driver.AppiumServer;
import com.reto.qa.driver.MobileSession;
import com.reto.qa.driver.MobileSessionFactory;
import io.qameta.allure.Allure;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.openqa.selenium.Capabilities;

import java.util.Optional;

/**
 * Appium session lifecycle built on the JUnit 5 extension model.
 *
 * <ul>
 *     <li><b>One Appium server per test JVM</b> ({@link AppiumServer}), stored in the root
 *     {@link ExtensionContext.Store} and shared by every class. JUnit closes {@link AutoCloseable} values when their
 *     store is discarded, so a server started by this JVM is stopped at the end of the run, with no shutdown hook.</li>
 *     <li><b>One session per class</b>, stored in the class's {@link ExtensionContext.Store}: there is no
 *     {@code ThreadLocal} or static state, so it is safe even when JUnit spreads work across threads.</li>
 *     <li><b>App restarted before each test</b>: every test starts from the same state and does not depend on another
 *     one (Independent and Repeatable in FIRST) without paying for a new session (Fast).</li>
 *     <li><b>Device as an Allure parameter</b> ("Pixel 9 · 15"): when the suite runs on several devices and their
 *     results are merged into one report, each device keeps its own result and history.</li>
 *     <li><b>Parameter injection</b> of {@link WdioDemoApp} and {@link MobileSession} (DIP): tests declare what they
 *     need instead of looking it up.</li>
 *     <li>The platform arrives as the {@code platform} configuration parameter and the environment as
 *     {@code test.env} (the Gradle tasks pass them as system properties). The test code is identical for Android and
 *     iOS and for any environment (local, CI or cloud).</li>
 * </ul>
 */
public final class AppiumSessionExtension
        implements
            BeforeAllCallback,
            AfterAllCallback,
            BeforeEachCallback,
            ParameterResolver {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
            .create(AppiumSessionExtension.class);
    private static final String SESSION_KEY = "mobile-session";
    private static final String SERVER_KEY = "appium-server";
    private static final String DEFAULT_PLATFORM = "android";

    private final MobileSessionFactory sessionFactory = new MobileSessionFactory();

    /** Created by JUnit through {@link MobileTest}. */
    public AppiumSessionExtension() {
        // Stateless apart from the session factory.
    }

    /**
     * Gets (or starts) the JVM's Appium server and opens the class's session, named after the test class on cloud
     * dashboards.
     */
    @Override
    public void beforeAll(ExtensionContext context) {
        Platform platform = Platform.from(context.getConfigurationParameter("platform").orElse(DEFAULT_PLATFORM));
        FrameworkConfig config = FrameworkConfig.forPlatform(platform);
        AppiumServer server = context.getRoot().getStore(NAMESPACE).computeIfAbsent(SERVER_KEY,
                key -> AppiumServer.from(FrameworkSettings.from(config).server(), platform), AppiumServer.class);
        MobileSession session = sessionFactory.start(config, server.url(), context.getDisplayName());
        context.getStore(NAMESPACE).put(SESSION_KEY, session);
    }

    /** Tags the test with the device it runs on and restarts the app so each test starts from the same state. */
    @Override
    public void beforeEach(ExtensionContext context) {
        MobileSession session = session(context);
        Allure.parameter("device", deviceLabel(session.profile(), session.driver().getCapabilities()));
        WdioDemoApp.on(session).restart();
    }

    /**
     * Describes the device as "name · OS version". The version reported by the session wins over the configured one,
     * which can be empty (for example when an iOS simulator is auto-selected). The UDID is left out on purpose: it
     * changes between machines and would break the test history in Allure.
     *
     * @param profile      the configured device
     * @param capabilities the capabilities returned by the Appium session
     * @return the device label
     */
    static String deviceLabel(DeviceProfile profile, Capabilities capabilities) {
        String version = text(capabilities.getCapability("platformVersion"));
        if (version == null) {
            version = text(capabilities.getCapability("appium:platformVersion"));
        }
        if (version == null) {
            version = profile.platformVersion().orElse(null);
        }
        return version == null ? profile.deviceName() : profile.deviceName() + " · " + version;
    }

    private static @Nullable String text(@Nullable Object value) {
        return value == null || value.toString().isBlank() ? null : value.toString();
    }

    /** Closes the class's session. */
    @Override
    public void afterAll(ExtensionContext context) {
        MobileSession session = context.getStore(NAMESPACE).remove(SESSION_KEY, MobileSession.class);
        if (session != null) {
            session.close();
        }
    }

    /** Supports {@link WdioDemoApp} and {@link MobileSession} parameters. */
    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        Class<?> type = parameterContext.getParameter().getType();
        return type == WdioDemoApp.class || type == MobileSession.class;
    }

    /** Resolves the facade or the raw session for the current class. */
    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        MobileSession session = session(extensionContext);
        return parameterContext.getParameter().getType() == WdioDemoApp.class ? WdioDemoApp.on(session) : session;
    }

    /**
     * Returns the session of the current class; a test's Store also looks in its class's Store.
     *
     * @param context test or class context
     * @return the session, or empty if none was opened
     */
    static Optional<MobileSession> findSession(ExtensionContext context) {
        return Optional.ofNullable(context.getStore(NAMESPACE).get(SESSION_KEY, MobileSession.class));
    }

    private static MobileSession session(ExtensionContext context) {
        return findSession(context).orElseThrow(() -> new ExtensionConfigurationException(
                "No Appium session for " + context.getDisplayName() + ". Is the class annotated with @MobileTest?"));
    }
}
