package com.reto.qa.junit;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca una clase como prueba E2E móvil. Composición en lugar de herencia: no hay clase base.
 *
 * <ul>
 *     <li>{@link AppiumSessionExtension}: abre una sesión por clase, reinicia la app antes de cada test
 *     e inyecta {@code WdioDemoApp} / {@code MobileSession} como parámetros.</li>
 *     <li>{@link EvidenceExtension}: adjunta screenshot y page source a Allure cuando un test falla.</li>
 *     <li>{@code @Tag("e2e")}: {@code ./gradlew test} la excluye; las tareas {@code e2eAndroid} / {@code e2eIos} la incluyen.</li>
 * </ul>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Tag("e2e")
@ExtendWith({AppiumSessionExtension.class, EvidenceExtension.class})
public @interface MobileTest {
}
