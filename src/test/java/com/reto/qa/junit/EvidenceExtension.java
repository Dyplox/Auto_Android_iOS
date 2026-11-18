package com.reto.qa.junit;

import com.reto.qa.driver.MobileSession;
import io.qameta.allure.Allure;
import io.qameta.allure.AttachmentOptions;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.openqa.selenium.OutputType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Adjunta evidencia (screenshot + page source) cuando un test falla y vuelve a lanzar el error.
 *
 * <p>Se usa {@link TestExecutionExceptionHandler} y no {@code TestWatcher}: corre mientras el test sigue "en
 * ejecución", así los adjuntos quedan dentro del test fallido en Allure. También deja el PNG en
 * {@code build/screenshots/<plataforma>/} para CI sin Allure.</p>
 */
public final class EvidenceExtension implements TestExecutionExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(EvidenceExtension.class);
    private static final Path SCREENSHOTS_DIR = Paths.get("build", "screenshots");

    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        AppiumSessionExtension.findSession(context).ifPresent(session -> captureEvidence(session, context));
        throw throwable;
    }

    private static void captureEvidence(MobileSession session, ExtensionContext context) {
        // El hash del uniqueId distingue las invocaciones de un @ParameterizedTest.
        String testName = context.getRequiredTestClass().getSimpleName() + "." + context.getRequiredTestMethod().getName()
                + "-" + Integer.toHexString(context.getUniqueId().hashCode());
        try {
            byte[] screenshot = session.driver().getScreenshotAs(OutputType.BYTES);
            Allure.attachment("Screenshot", "image/png", new ByteArrayInputStream(screenshot),
                    AttachmentOptions.withFileExtension("png"));
            Allure.attachment("Page source", "text/xml", session.driver().getPageSource(),
                    AttachmentOptions.withFileExtension("xml"));

            Path file = SCREENSHOTS_DIR.resolve(session.platform().key()).resolve(testName + ".png");
            Files.createDirectories(file.getParent());
            Files.write(file, screenshot);
            LOG.info("Evidencia de '{}' guardada en {}", context.getDisplayName(), file.toAbsolutePath());
        } catch (IOException | RuntimeException e) {
            // La evidencia nunca debe ocultar la causa real del fallo.
            LOG.warn("No se pudo capturar evidencia de '{}': {}", context.getDisplayName(), e.getMessage());
        }
    }
}
