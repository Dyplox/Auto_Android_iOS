package com.reto.qa.pages;

import com.reto.qa.core.Waiter;
import com.reto.qa.driver.MobileSession;
import org.openqa.selenium.WebElement;

/** Pantalla completa de la app: se identifica por un elemento raíz único. */
public abstract class BasePage extends BaseUiObject {

    private static final int MAX_SCROLLS = 3;

    protected BasePage(MobileSession session) {
        super(session);
    }

    /** Elemento que identifica inequívocamente la pantalla (su contenedor raíz). */
    protected abstract WebElement root();

    public void waitUntilLoaded() {
        waiter.untilVisible(root());
    }

    public boolean isLoaded() {
        return Waiter.isDisplayedNow(root());
    }

    protected void hideKeyboard() {
        session.actions().hideKeyboard(keyboardDismissArea());
    }

    /**
     * Zona sin interacción que se toca para cerrar el teclado en iOS. Por defecto el contenedor raíz;
     * las pantallas con controles en el centro la sobrescriben.
     */
    protected WebElement keyboardDismissArea() {
        return root();
    }

    protected void scrollTo(WebElement element) {
        session.gestures().scrollUntilVisible(element, MAX_SCROLLS);
    }
}
