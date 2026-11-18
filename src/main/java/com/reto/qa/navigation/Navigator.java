package com.reto.qa.navigation;

/** Estrategia de navegación (patrón Strategy): los tests no saben si se navega por UI o por deep link. */
@FunctionalInterface
public interface Navigator {

    void goTo(Screen screen);
}
