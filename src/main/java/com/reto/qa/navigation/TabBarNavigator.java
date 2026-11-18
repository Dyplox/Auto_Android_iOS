package com.reto.qa.navigation;

import com.reto.qa.driver.MobileSession;
import com.reto.qa.pages.components.TabBar;

import java.util.Objects;

/** Navigates like a real user: by tapping the tab bar. */
public final class TabBarNavigator implements Navigator {

    private final MobileSession session;

    /**
     * Creates the navigator.
     *
     * @param session the current session
     */
    public TabBarNavigator(MobileSession session) {
        this.session = Objects.requireNonNull(session, "session");
    }

    @Override
    public void goTo(Screen screen) {
        new TabBar(session).open(screen);
    }
}
