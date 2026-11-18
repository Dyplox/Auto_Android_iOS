package com.reto.qa.core;

import org.openqa.selenium.WebElement;

/**
 * Behaviours that each operating system handles differently (Strategy pattern).
 *
 * <p>This is the ONLY place in the framework with platform-specific code outside the locators: Page Objects and tests
 * depend on this abstraction (DIP) and never ask "am I on iOS?".</p>
 */
public interface PlatformActions {

    /**
     * Hides the keyboard if it is visible.
     *
     * @param neutralArea non-interactive element to tap when the platform cannot hide the keyboard through the API
     *                    (a known XCTest limitation on iPhone)
     */
    void hideKeyboard(WebElement neutralArea);

    /**
     * Opens a deep link inside the app under test.
     *
     * @param url full deep link, for example {@code wdio://login}
     */
    void openDeepLink(String url);

    /** Closes and reopens the app: a clean state between tests without recreating the session. */
    void restartApp();
}
