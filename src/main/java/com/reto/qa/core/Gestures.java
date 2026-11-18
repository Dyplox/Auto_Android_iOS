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
 * Gestos con W3C Actions: el mismo código funciona en Android e iOS
 * (TouchAction fue eliminado en java-client 11).
 */
public final class Gestures {

    private static final Duration SWIPE_DURATION = Duration.ofMillis(600);
    private static final Duration PRESS_DURATION = Duration.ofMillis(100);

    private final AppiumDriver driver;

    public Gestures(AppiumDriver driver) {
        this.driver = Objects.requireNonNull(driver, "driver");
    }

    /** Desliza el dedo hacia arriba (el contenido baja). */
    public void swipeUp() {
        Dimension size = driver.manage().window().getSize();
        int x = size.getWidth() / 2;
        swipe(new Point(x, (int) (size.getHeight() * 0.70)), new Point(x, (int) (size.getHeight() * 0.30)));
    }

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
     * Hace scroll hasta que el elemento sea visible. En Android los elementos fuera de pantalla no existen en el
     * árbol; en iOS existen pero no son visibles: {@link Waiter#isDisplayedNow(WebElement)} cubre ambos casos.
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
        throw new NoSuchElementException("El elemento no apareció tras " + maxSwipes + " swipes");
    }
}
