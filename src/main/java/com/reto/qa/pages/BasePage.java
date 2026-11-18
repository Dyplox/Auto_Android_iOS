package com.reto.qa.pages;

import com.reto.qa.core.Waiter;
import com.reto.qa.driver.MobileSession;
import org.openqa.selenium.WebElement;

/** A full screen of the app, identified by a unique root element. */
public abstract class BasePage extends BaseUiObject {

    private static final int MAX_SCROLLS = 3;

    /**
     * Initializes the page's elements for the session's platform.
     *
     * @param session the current session
     */
    protected BasePage(MobileSession session) {
        super(session);
    }

    /**
     * Returns the element that unambiguously identifies the screen (its root container).
     *
     * @return the root element
     */
    protected abstract WebElement root();

    /**
     * Waits until the screen is visible.
     *
     * @throws org.openqa.selenium.TimeoutException if it does not load in time
     */
    public void waitUntilLoaded() {
        waiter.untilVisible(root());
    }

    /**
     * Checks, without waiting, whether the screen is displayed.
     *
     * @return {@code true} if the root element is displayed right now
     */
    public boolean isLoaded() {
        return Waiter.isDisplayedNow(root());
    }

    /** Hides the keyboard using the platform strategy. */
    protected void hideKeyboard() {
        session.actions().hideKeyboard(keyboardDismissArea());
    }

    /**
     * Non-interactive area tapped to dismiss the keyboard on iOS. Defaults to the root container;
     * screens with controls in the middle override it.
     *
     * @return the element to tap
     */
    protected WebElement keyboardDismissArea() {
        return root();
    }

    /**
     * Scrolls until the element is visible.
     *
     * @param element element to bring into view
     */
    protected void scrollTo(WebElement element) {
        session.gestures().scrollUntilVisible(element, MAX_SCROLLS);
    }
}
