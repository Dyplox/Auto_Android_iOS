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
 * Base común de pantallas y componentes.
 *
 * <p><b>Page Factory multiplataforma:</b> cada campo declara {@code @AndroidFindBy} y {@code @iOSXCUITFindBy};
 * {@link AppiumFieldDecorator} lee el {@code platformName} de la sesión y resuelve el locator correcto en
 * tiempo de ejecución. Por eso el MISMO page object y el MISMO test sirven para Android e iOS.</p>
 *
 * <p>El decorador se crea con timeout cero: la sincronización es responsabilidad explícita de {@link Waiter},
 * nunca de esperas implícitas (pruebas más rápidas y predecibles).</p>
 */
public abstract class BaseUiObject {

    protected final MobileSession session;
    protected final AppiumDriver driver;
    protected final Waiter waiter;

    protected BaseUiObject(MobileSession session) {
        this.session = Objects.requireNonNull(session, "session");
        this.driver = session.driver();
        this.waiter = session.waiter();
        PageFactory.initElements(new AppiumFieldDecorator(driver, Duration.ZERO), this);
    }

    protected void tap(WebElement element) {
        waiter.untilVisible(element).click();
    }

    protected void type(WebElement element, String text) {
        WebElement field = waiter.untilVisible(element);
        field.clear();
        field.sendKeys(text);
    }

    protected String textOf(WebElement element) {
        return waiter.untilVisible(element).getText();
    }

    protected boolean isVisible(WebElement element, Duration timeout) {
        return waiter.isVisible(element, timeout);
    }
}
