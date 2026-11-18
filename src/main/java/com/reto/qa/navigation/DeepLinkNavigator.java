package com.reto.qa.navigation;

import com.reto.qa.core.PlatformActions;

import java.util.Objects;

/**
 * Navigates straight to the screen with a deep link ({@code wdio://login}). It skips UI steps that are not what the
 * test is about: it is the fastest option (the F in FIRST).
 */
public final class DeepLinkNavigator implements Navigator {

    private final PlatformActions actions;
    private final String scheme;

    /**
     * Creates the navigator.
     *
     * @param actions platform actions used to open the link
     * @param scheme  deep link scheme including {@code ://}, for example {@code wdio://}
     */
    public DeepLinkNavigator(PlatformActions actions, String scheme) {
        this.actions = Objects.requireNonNull(actions, "actions");
        this.scheme = Objects.requireNonNull(scheme, "scheme");
    }

    @Override
    public void goTo(Screen screen) {
        actions.openDeepLink(scheme + screen.deepLinkPath());
    }
}
