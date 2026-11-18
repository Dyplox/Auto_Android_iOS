package com.reto.qa.navigation;

import com.reto.qa.config.ConfigurationException;
import com.reto.qa.driver.MobileSession;

import java.util.Locale;

/** Picks the navigation strategy from {@code navigation.strategy} (tabbar | deeplink). */
public final class NavigatorFactory {

    private NavigatorFactory() {
    }

    /**
     * Creates the configured navigator.
     *
     * @param session the current session
     * @return the navigator
     * @throws ConfigurationException if the strategy is not supported
     */
    public static Navigator create(MobileSession session) {
        String strategy = session.config().get("navigation.strategy").toLowerCase(Locale.ROOT);
        return switch (strategy) {
            case "tabbar" -> new TabBarNavigator(session);
            case "deeplink" -> new DeepLinkNavigator(session.actions(), session.config().get("app.deeplink.scheme"));
            default -> throw new ConfigurationException(
                    "Unsupported navigation.strategy: '" + strategy + "'. Use tabbar or deeplink");
        };
    }
}
