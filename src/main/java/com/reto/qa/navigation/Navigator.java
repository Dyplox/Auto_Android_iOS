package com.reto.qa.navigation;

/** Navigation strategy (Strategy pattern): tests do not know whether navigation uses the UI or a deep link. */
@FunctionalInterface
public interface Navigator {

    /**
     * Opens a screen of the app.
     *
     * @param screen the screen to open
     */
    void goTo(Screen screen);
}
