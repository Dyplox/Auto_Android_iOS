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
 * Attaches evidence (screenshot + page source) when a test fails, then rethrows the error.
 *
 * <p>{@link TestExecutionExceptionHandler} is used instead of {@code TestWatcher}: it runs while the test is still
 * "executing", so the attachments end up inside the failed test in Allure. It also saves the PNG to
 * {@code build/screenshots/<platform>/} for CI runs without Allure.</p>
 */
public final class EvidenceExtension implements TestExecutionExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(EvidenceExtension.class);
    private static final Path SCREENSHOTS_DIR = Paths.get("build", "screenshots");

    /** Created by JUnit through {@link MobileTest}. */
    public EvidenceExtension() {
        // Stateless.
    }

    /** Captures evidence if the class has a session, then rethrows the original error unchanged. */
    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        AppiumSessionExtension.findSession(context).ifPresent(session -> captureEvidence(session, context));
        throw throwable;
    }

    private static void captureEvidence(MobileSession session, ExtensionContext context) {
        // The uniqueId hash tells apart the invocations of a @ParameterizedTest.
        String testName = context.getRequiredTestClass().getSimpleName() + "."
                + context.getRequiredTestMethod().getName()
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
            LOG.info("Evidence for '{}' saved to {}", context.getDisplayName(), file.toAbsolutePath());
        } catch (IOException | RuntimeException e) {
            // Evidence must never hide the real cause of the failure.
            LOG.warn("Could not capture evidence for '{}': {}", context.getDisplayName(), e.getMessage());
        }
    }
}
