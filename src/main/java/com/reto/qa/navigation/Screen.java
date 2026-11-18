package com.reto.qa.navigation;

/** Navigable screens of the app and their deep link path ({@code wdio://<path>}). */
public enum Screen {

    /** Home screen. */
    HOME("Home", ""),
    /** Login and sign-up screen. */
    LOGIN("Login", "login"),
    /** Forms screen. */
    FORMS("Forms", "forms");

    private final String displayName;
    private final String deepLinkPath;

    Screen(String displayName, String deepLinkPath) {
        this.displayName = displayName;
        this.deepLinkPath = deepLinkPath;
    }

    /**
     * Returns the path appended to the deep link scheme.
     *
     * @return the path, empty for the home screen
     */
    public String deepLinkPath() {
        return deepLinkPath;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
