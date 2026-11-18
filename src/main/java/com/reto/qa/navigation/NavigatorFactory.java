package com.reto.qa.navigation;

import com.reto.qa.config.NavigationSettings;
import com.reto.qa.driver.MobileSession;

/** Picks the navigation strategy from {@code navigation.strategy} (tabbar | deeplink). */
public final class NavigatorFactory {

    private NavigatorFactory() {
    }

    /**
     * Creates the configured navigator.
     *
     * @param session the current session
     * @return the navigator
     */
    public static Navigator create(MobileSession session) {
        NavigationSettings navigation = session.settings().navigation();
        return switch (navigation.strategy()) {
            case TABBAR -> new TabBarNavigator(session);
            case DEEPLINK -> new DeepLinkNavigator(session.actions(), navigation.deepLinkScheme());
        };
    }
}
