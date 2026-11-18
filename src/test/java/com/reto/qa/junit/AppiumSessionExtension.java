package com.reto.qa.junit;

import com.reto.qa.app.WdioDemoApp;
import com.reto.qa.config.FrameworkConfig;
import com.reto.qa.config.Platform;
import com.reto.qa.driver.MobileSession;
import com.reto.qa.driver.MobileSessionFactory;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.util.Optional;

/**
 * Ciclo de vida de la sesión de Appium con el modelo de extensiones de JUnit 5.
 *
 * <ul>
 *     <li><b>Una sesión por clase</b>, guardada en el {@link ExtensionContext.Store} de la clase: no hay
 *     {@code ThreadLocal} ni estado estático, así que es seguro aunque JUnit reparta trabajo entre hilos.</li>
 *     <li><b>App reiniciada antes de cada test</b>: cada test parte del mismo estado y no depende de otro
 *     (Independent y Repeatable de FIRST) sin pagar el costo de una sesión nueva (Fast).</li>
 *     <li><b>Inyección por parámetro</b> de {@link WdioDemoApp} y {@link MobileSession} (DIP): los tests declaran
 *     lo que necesitan en vez de buscarlo.</li>
 *     <li>La plataforma llega como parámetro de configuración {@code platform} y el ambiente como {@code test.env}
 *     (las tareas de Gradle los envían como system properties). El código de los tests es idéntico para Android e
 *     iOS y para cualquier ambiente (local, CI o nube).</li>
 * </ul>
 */
public final class AppiumSessionExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback,
        ParameterResolver {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(AppiumSessionExtension.class);
    private static final String SESSION_KEY = "mobile-session";
    private static final String DEFAULT_PLATFORM = "android";

    private final MobileSessionFactory sessionFactory = new MobileSessionFactory();

    @Override
    public void beforeAll(ExtensionContext context) {
        Platform platform = Platform.from(context.getConfigurationParameter("platform").orElse(DEFAULT_PLATFORM));
        MobileSession session = sessionFactory.start(FrameworkConfig.forPlatform(platform), context.getDisplayName());
        context.getStore(NAMESPACE).put(SESSION_KEY, session);
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        WdioDemoApp.on(session(context)).restart();
    }

    @Override
    public void afterAll(ExtensionContext context) {
        MobileSession session = context.getStore(NAMESPACE).remove(SESSION_KEY, MobileSession.class);
        if (session != null) {
            session.close();
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        Class<?> type = parameterContext.getParameter().getType();
        return type == WdioDemoApp.class || type == MobileSession.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        MobileSession session = session(extensionContext);
        return parameterContext.getParameter().getType() == WdioDemoApp.class ? WdioDemoApp.on(session) : session;
    }

    /** Sesión de la clase en curso; el Store de un test busca también en el de su clase. */
    static Optional<MobileSession> findSession(ExtensionContext context) {
        return Optional.ofNullable(context.getStore(NAMESPACE).get(SESSION_KEY, MobileSession.class));
    }

    private static MobileSession session(ExtensionContext context) {
        return findSession(context).orElseThrow(() -> new ExtensionConfigurationException(
                "No hay sesión de Appium para " + context.getDisplayName() + ". ¿La clase está anotada con @MobileTest?"));
    }
}
