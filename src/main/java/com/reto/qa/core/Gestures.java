package com.reto.qa.core;

import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Pause;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Gestures built on W3C Actions: the same code works on Android and iOS
 * (TouchAction was removed in java-client 11).
 */
public final class Gestures {

    private static final Duration SWIPE_DURATION = Duration.ofMillis(600);
    private static final Duration PRESS_DURATION = Duration.ofMillis(100);

    private final AppiumDriver driver;

    /**
     * Creates the gestures for a driver.
     *
     * @param driver the Appium driver
     */
    public Gestures(AppiumDriver driver) {
        this.driver = Objects.requireNonNull(driver, "driver");
    }

    /** Swipes the finger up, from 70% to 30% of the screen height (the content scrolls down). */
    public void swipeUp() {
        Dimension size = driver.manage().window().getSize();
        int x = size.getWidth() / 2;
        swipe(new Point(x, (int) (size.getHeight() * 0.70)), new Point(x, (int) (size.getHeight() * 0.30)));
    }

    /**
     * Performs a one-finger swipe between two viewport points.
     *
     * @param from start point
     * @param to   end point
     */
    public void swipe(Point from, Point to) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence swipe = new Sequence(finger, 0)
                .addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), from.getX(), from.getY()))
                .addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
                .addAction(new Pause(finger, PRESS_DURATION))
                .addAction(finger.createPointerMove(SWIPE_DURATION, PointerInput.Origin.viewport(), to.getX(), to.getY()))
                .addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(List.of(swipe));
    }

    /**
     * Scrolls until the element is visible. On Android, off-screen elements are not in the tree; on iOS they are in
     * the tree but not visible: {@link Waiter#isDisplayedNow(WebElement)} covers both cases.
     *
     * @param element   element to bring into view
     * @param maxSwipes maximum number of swipes
     * @throws NoSuchElementException if the element is still not visible after {@code maxSwipes} swipes
     */
    public void scrollUntilVisible(WebElement element, int maxSwipes) {
        for (int attempt = 0; attempt <= maxSwipes; attempt++) {
            if (Waiter.isDisplayedNow(element)) {
                return;
            }
            if (attempt < maxSwipes) {
                swipeUp();
            }
        }
        throw new NoSuchElementException("Element did not appear after " + maxSwipes + " swipes");
    }
}
