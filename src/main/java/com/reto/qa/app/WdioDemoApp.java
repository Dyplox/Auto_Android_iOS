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
 * Fachada de la app bajo prueba: punto de entrada único y legible para los tests.
 *
 * <pre>{@code
 * LoginPage login = app.open(Screen.LOGIN, LoginPage::new);
 * }</pre>
 */
public final class WdioDemoApp {

    private final MobileSession session;
    private final Navigator navigator;

    public WdioDemoApp(MobileSession session, Navigator navigator) {
        this.session = Objects.requireNonNull(session, "session");
        this.navigator = Objects.requireNonNull(navigator, "navigator");
    }

    public static WdioDemoApp on(MobileSession session) {
        return new WdioDemoApp(session, NavigatorFactory.create(session));
    }

    /** Estado limpio antes de cada test sin pagar el costo de una sesión nueva (la I de FIRST). */
    @Step("Reiniciar la app")
    public WdioDemoApp restart() {
        session.actions().restartApp();
        new TabBar(session).waitUntilVisible();
        return this;
    }

    @Step("Abrir la pantalla {screen}")
    public <P extends BasePage> P open(Screen screen, Function<MobileSession, P> pageConstructor) {
        navigator.goTo(screen);
        P page = pageConstructor.apply(session);
        page.waitUntilLoaded();
        return page;
    }

    public NativeAlert alert() {
        return new NativeAlert(session);
    }
}
