package com.reto.qa.junit;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a mobile E2E test. Composition instead of inheritance: there is no base class.
 *
 * <ul>
 *     <li>{@link AppiumSessionExtension}: opens one session per class, restarts the app before each test
 *     and injects {@code WdioDemoApp} / {@code MobileSession} as parameters.</li>
 *     <li>{@link EvidenceExtension}: attaches a screenshot and the page source to Allure when a test fails.</li>
 *     <li>{@code @Tag("e2e")}: {@code ./gradlew test} excludes it; the {@code e2eAndroid} / {@code e2eIos} tasks include it.</li>
 * </ul>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Tag("e2e")
@ExtendWith({AppiumSessionExtension.class, EvidenceExtension.class})
public @interface MobileTest {
}
