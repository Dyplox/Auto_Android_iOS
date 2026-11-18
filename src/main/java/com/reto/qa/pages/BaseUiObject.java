package com.reto.qa.pages;

import com.reto.qa.core.Waiter;
import com.reto.qa.driver.MobileSession;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;
import io.appium.java_client.support.PageFactory;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.Objects;

/**
 * Common base for screens and components.
 *
 * <p><b>Cross-platform Page Factory:</b> each field declares {@code @AndroidFindBy} and {@code @iOSXCUITFindBy};
 * {@link AppiumFieldDecorator} reads the session's {@code platformName} and resolves the right locator at runtime.
 * That is why the SAME page object and the SAME test work for Android and iOS.</p>
 *
 * <p>The decorator is created with a zero timeout: synchronization is the explicit job of {@link Waiter}, never of
 * implicit waits (faster, more predictable tests).</p>
 */
public abstract class BaseUiObject {

    /** The current session. */
    protected final MobileSession session;
    /** The session's driver. */
    protected final AppiumDriver driver;
    /** The session's explicit waits. */
    protected final Waiter waiter;

    /**
     * Initializes the {@code @AndroidFindBy} / {@code @iOSXCUITFindBy} fields for the session's platform.
     *
     * <p>Passing {@code this} to the Page Factory from the constructor is intended: subclasses only declare
     * annotated fields, which the decorator replaces with lazy proxies.</p>
     *
     * @param session the current session
     */
    @SuppressWarnings("this-escape")
    protected BaseUiObject(MobileSession session) {
        this.session = Objects.requireNonNull(session, "session");
        this.driver = session.driver();
        this.waiter = session.waiter();
        PageFactory.initElements(new AppiumFieldDecorator(driver, Duration.ZERO), this);
    }

    /**
     * Waits until the element is visible and taps it.
     *
     * @param element element to tap
     */
    protected void tap(WebElement element) {
        waiter.untilVisible(element).click();
    }

    /**
     * Waits until the field is visible, clears it and types the text.
     *
     * @param element text field
     * @param text    text to type
     */
    protected void type(WebElement element, String text) {
        WebElement field = waiter.untilVisible(element);
        field.clear();
        field.sendKeys(text);
    }

    /**
     * Waits until the element is visible and returns its text.
     *
     * @param element element to read
     * @return the element's text
     */
    protected String textOf(WebElement element) {
        return waiter.untilVisible(element).getText();
    }

    /**
     * Checks whether the element becomes visible within the timeout, without throwing.
     *
     * @param element element to check
     * @param timeout how long to wait
     * @return {@code true} if it became visible in time
     */
    protected boolean isVisible(WebElement element, Duration timeout) {
        return waiter.isVisible(element, timeout);
    }
}
