package com.reto.qa.navigation;

import com.reto.qa.config.ConfigurationException;
import com.reto.qa.driver.MobileSession;

import java.util.Locale;

/** Elige la estrategia de navegación según {@code navigation.strategy} (tabbar | deeplink). */
public final class NavigatorFactory {

    private NavigatorFactory() {
    }

    public static Navigator create(MobileSession session) {
        String strategy = session.config().get("navigation.strategy").toLowerCase(Locale.ROOT);
        return switch (strategy) {
            case "tabbar" -> new TabBarNavigator(session);
            case "deeplink" -> new DeepLinkNavigator(session.actions(), session.config().get("app.deeplink.scheme"));
            default -> throw new ConfigurationException(
                    "navigation.strategy no soportada: '" + strategy + "'. Usa tabbar o deeplink");
        };
    }
}
