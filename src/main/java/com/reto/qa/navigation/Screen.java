package com.reto.qa.navigation;

/** Pantallas navegables de la app y su ruta de deep link ({@code wdio://<ruta>}). */
public enum Screen {

    HOME("Home", ""),
    LOGIN("Login", "login"),
    FORMS("Forms", "forms");

    private final String displayName;
    private final String deepLinkPath;

    Screen(String displayName, String deepLinkPath) {
        this.displayName = displayName;
        this.deepLinkPath = deepLinkPath;
    }

    public String deepLinkPath() {
        return deepLinkPath;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
