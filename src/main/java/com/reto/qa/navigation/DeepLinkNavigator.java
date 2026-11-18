package com.reto.qa.navigation;

import com.reto.qa.core.PlatformActions;

import java.util.Objects;

/**
 * Navega directo a la pantalla con un deep link ({@code wdio://login}). Evita pasos de UI que no son
 * objeto de la prueba: es la opción más rápida (la F de FIRST).
 */
public final class DeepLinkNavigator implements Navigator {

    private final PlatformActions actions;
    private final String scheme;

    public DeepLinkNavigator(PlatformActions actions, String scheme) {
        this.actions = Objects.requireNonNull(actions, "actions");
        this.scheme = Objects.requireNonNull(scheme, "scheme");
    }

    @Override
    public void goTo(Screen screen) {
        actions.openDeepLink(scheme + screen.deepLinkPath());
    }
}
