package com.reto.qa.navigation;

import com.reto.qa.driver.MobileSession;
import com.reto.qa.pages.components.TabBar;

import java.util.Objects;

/** Navega como un usuario real: tocando la barra de pestañas. */
public final class TabBarNavigator implements Navigator {

    private final MobileSession session;

    public TabBarNavigator(MobileSession session) {
        this.session = Objects.requireNonNull(session, "session");
    }

    @Override
    public void goTo(Screen screen) {
        new TabBar(session).open(screen);
    }
}
