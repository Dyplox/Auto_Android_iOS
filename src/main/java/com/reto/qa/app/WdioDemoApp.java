package com.reto.qa.app;

import com.reto.qa.driver.MobileSession;
import com.reto.qa.navigation.Navigator;
import com.reto.qa.navigation.NavigatorFactory;
import com.reto.qa.navigation.Screen;
import com.reto.qa.pages.BasePage;
import com.reto.qa.pages.components.NativeAlert;
import com.reto.qa.pages.components.TabBar;
import io.qameta.allure.Step;

import java.util.Objects;
import java.util.function.Function;

/**
 * Facade of the app under test: a single, readable entry point for the tests.
 *
 * <pre>{@code
 * LoginPage login = app.open(Screen.LOGIN, LoginPage::new);
 * }</pre>
 */
public final class WdioDemoApp {

    private final MobileSession session;
    private final Navigator navigator;

    /**
     * Creates the facade.
     *
     * @param session   the current session
     * @param navigator navigation strategy
     */
    public WdioDemoApp(MobileSession session, Navigator navigator) {
        this.session = Objects.requireNonNull(session, "session");
        this.navigator = Objects.requireNonNull(navigator, "navigator");
    }

    /**
     * Creates the facade with the navigator configured in {@code navigation.strategy}.
     *
     * @param session the current session
     * @return the facade
     */
    public static WdioDemoApp on(MobileSession session) {
        return new WdioDemoApp(session, NavigatorFactory.create(session));
    }

    /**
     * Restarts the app: a clean state before each test without paying for a new session (the I in FIRST).
     *
     * @return this facade
     */
    @Step("Restart the app")
    public WdioDemoApp restart() {
        session.actions().restartApp();
        new TabBar(session).waitUntilVisible();
        return this;
    }

    /**
     * Navigates to a screen and waits until it is loaded.
     *
     * @param screen          the screen to open
     * @param pageConstructor page object constructor, for example {@code LoginPage::new}
     * @param <P>             the page type
     * @return the loaded page
     */
    @Step("Open the {screen} screen")
    public <P extends BasePage> P open(Screen screen, Function<MobileSession, P> pageConstructor) {
        navigator.goTo(screen);
        P page = pageConstructor.apply(session);
        page.waitUntilLoaded();
        return page;
    }

    /**
     * Returns the native alert component.
     *
     * @return the alert
     */
    public NativeAlert alert() {
        return new NativeAlert(session);
    }
}
