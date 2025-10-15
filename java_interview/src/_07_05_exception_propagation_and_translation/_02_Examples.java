package _07_05_exception_propagation_and_translation;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;

/**
 * Exception Propagation & Translation - Complete, runnable examples with explanations.
 *
 * Run main() and read the console output alongside the comments.
 */
public class _02_Examples {

    public static void main(String[] args) {
        title("Exception Propagation & Translation - Examples");

        demoUncheckedPropagation();
        demoCheckedPropagation();
        demoTranslationToDomainChecked();
        demoRethrowPreserveVsLose();
        demoCheckedToUncheckedTranslation();
        demoTryWithResourcesSuppressed();
        demoFinallyOverridingAndFix();
        demoMultiCatchTranslation();
        demoLayeredTranslation();

        done();
    }

    // ------------------------------------------------------------
    // 1) Unchecked exception propagation (no 'throws' required)
    // ------------------------------------------------------------
    private static void demoUncheckedPropagation() {
        section("1) Unchecked exception propagation");
        try {
            uncheckedLevel1();
        } catch (RuntimeException e) {
            System.out.println("Caught at top-level: " + e);
            System.out.println("Stack shows the path through the call chain:");
            e.printStackTrace(System.out);
        }
    }

    private static void uncheckedLevel1() {
        uncheckedLevel2();
    }

    private static void uncheckedLevel2() {
        uncheckedLevel3();
    }

    private static void uncheckedLevel3() {
        throw new IllegalStateException("Boom at level 3 (unchecked)");
    }

    // ------------------------------------------------------------
    // 2) Checked exception propagation (must declare 'throws')
    // ------------------------------------------------------------
    private static void demoCheckedPropagation() {
        section("2) Checked exception propagation");
        try {
            checkedTop();
        } catch (IOException e) {
            System.out.println("Caught checked exception at top-level: " + e);
            e.printStackTrace(System.out);
        }
    }

    private static void checkedTop() throws IOException {
        checkedMid();
    }

    private static void checkedMid() throws IOException {
        checkedBottom();
    }

    private static void checkedBottom() throws IOException {
        throw new IOException("IO failure at bottom (checked)");
    }

    // ------------------------------------------------------------
    // 3) Exception translation (wrap low-level into domain-specific)
    //    - Preserve cause to retain root context
    // ------------------------------------------------------------
    private static void demoTranslationToDomainChecked() {
        section("3) Exception translation to domain-specific (checked), preserving cause");
        try {
            loadConfig("app.properties");
        } catch (ConfigLoadException e) {
            System.out.println("Translated domain exception: " + e);
            printCauses(e);
        }
    }

    static class ConfigLoadException extends Exception {
        public ConfigLoadException(String message, Throwable cause) { super(message, cause); }
    }

    private static void loadConfig(String path) throws ConfigLoadException {
        try {
            // Simulate lower-level I/O failure
            throw new IOException("Failed to read file: " + path);
        } catch (IOException io) {
            // Translate to a domain-specific checked exception; keep cause
            throw new ConfigLoadException("Unable to load configuration (" + path + ")", io);
        }
    }

    // ------------------------------------------------------------
    // 4) Rethrow preserving stack vs losing original context
    // ------------------------------------------------------------
    private static void demoRethrowPreserveVsLose() {
        section("4) Rethrow preserving stack vs losing original context");
        try {
            rethrowPreservingStack();
        } catch (Exception e) {
            System.out.println("Preserved stack (throw e): " + e);
            e.printStackTrace(System.out);
        }

        try {
            rethrowLosingStack();
        } catch (Exception e) {
            System.out.println("\nLost original context (new Exception without cause): " + e);
            e.printStackTrace(System.out);
        }

        try {
            rethrowWithTranslationPreservingCause();
        } catch (Exception e) {
            System.out.println("\nTranslated with preserved cause: " + e);
            printCauses(e);
        }
    }

    private static void rethrowPreservingStack() throws Exception {
        try {
            failingMethod();
        } catch (Exception e) {
            // Rethrow as-is: preserves original stack trace
            throw e;
        }
    }

    private static void rethrowLosingStack() throws Exception {
        try {
            failingMethod();
        } catch (Exception e) {
            // BAD: New exception without cause loses original context
            throw new Exception("Wrapped but lost original cause");
        }
    }

    private static void rethrowWithTranslationPreservingCause() throws Exception {
        try {
            failingMethod();
        } catch (Exception e) {
            // GOOD: Translate and preserve cause
            throw new Exception("Translated with more context", e);
        }
    }

    private static void failingMethod() throws Exception {
        throw new Exception("Original failure inside failingMethod()");
    }

    // ------------------------------------------------------------
    // 5) Translating checked to unchecked (to avoid throws leakage)
    //    - Common for infrastructure exceptions: wrap in RuntimeException
    // ------------------------------------------------------------
    private static void demoCheckedToUncheckedTranslation() {
        section("5) Translating checked to unchecked (e.g., DataAccessException)");
        try {
            callRepository();
        } catch (DataAccessException e) {
            System.out.println("Caught unchecked translation: " + e);
            printCauses(e);
        }
    }

    static class DataAccessException extends RuntimeException {
        public DataAccessException(String message, Throwable cause) { super(message, cause); }
    }

    private static void callRepository() {
        try {
            repositoryLowLevelCall();
        } catch (SQLException sql) {
            // Translate checked SQLException to unchecked DataAccessException
            throw new DataAccessException("Database access failed", sql);
        }
    }

    private static void repositoryLowLevelCall() throws SQLException {
        // Simulate DB exception
        throw new SQLException("Deadlock detected");
    }

    // ------------------------------------------------------------
    // 6) Suppressed exceptions with try-with-resources
    //    - Close failures are suppressed on the primary exception
    // ------------------------------------------------------------
    private static void demoTryWithResourcesSuppressed() {
        section("6) Suppressed exceptions (try-with-resources)");
        try {
            useResource();
        } catch (Exception e) {
            System.out.println("Primary exception: " + e);
            for (Throwable sup : e.getSuppressed()) {
                System.out.println("  suppressed: " + sup);
            }
        }
    }

    static class DemoResource implements AutoCloseable {
        @Override
        public void close() throws Exception {
            throw new Exception("close() failure");
        }
    }

    private static void useResource() throws Exception {
        try (DemoResource r = new DemoResource()) {
            throw new Exception("primary failure in try-block");
        }
    }

    // ------------------------------------------------------------
    // 7) Finally block overriding primary exception vs preserving it
    //    - Bad: throwing in finally overwrites the original exception
    //    - Better: attach finally error as suppressed
    // ------------------------------------------------------------
    private static void demoFinallyOverridingAndFix() {
        section("7) Finally overriding original vs preserving it");

        try {
            badFinallyOverwrites();
        } catch (Exception e) {
            System.out.println("Overwritten by finally: " + e);
            System.out.println("Note: the original 'try' exception is LOST here.");
        }

        try {
            goodFinallyPreserves();
        } catch (Exception e) {
            System.out.println("\nPreserved original; finally error added as suppressed: " + e);
            for (Throwable sup : e.getSuppressed()) {
                System.out.println("  suppressed: " + sup);
            }
        }
    }

    private static void badFinallyOverwrites() throws Exception {
        try {
            throw new Exception("try failure");
        } finally {
            // BAD: this replaces the original exception
            throw new Exception("finally failure (overwrites original)");
        }
    }

    private static void goodFinallyPreserves() throws Exception {
        Exception primary = null;
        try {
            throw new Exception("try failure");
        } catch (Exception e) {
            primary = e;
            // Rethrow primary; finally will still execute
            throw e;
        } finally {
            try {
                // Some cleanup that can fail
                throw new Exception("finally failure");
            } catch (Exception fin) {
                if (primary != null) {
                    // Attach cleanup error without overwriting primary
                    primary.addSuppressed(fin);
                } else {
                    // If no primary, rethrow cleanup error
                    throw fin;
                }
            }
        }
    }

    // ------------------------------------------------------------
    // 8) Multi-catch translation (combine multiple checked causes)
    // ------------------------------------------------------------
    private static void demoMultiCatchTranslation() {
        section("8) Multi-catch translation");
        try {
            openUserProvidedResource("://bad-uri");
        } catch (UserRequestException e) {
            System.out.println("Translated: " + e);
            printCauses(e);
        }
    }

    static class UserRequestException extends Exception {
        public UserRequestException(String message, Throwable cause) { super(message, cause); }
    }

    private static void openUserProvidedResource(String uriString) throws UserRequestException {
        try {
            // May throw URISyntaxException
            URI uri = new URI(uriString);

            // Simulate other checked problem; reusing IOException as a placeholder
            if ("trigger-io".equals(uriString)) {
                throw new IOException("Network read error");
            }
        } catch (URISyntaxException | IOException e) {
            throw new UserRequestException("Invalid or inaccessible resource: " + uriString, e);
        }
    }

    // ------------------------------------------------------------
    // 9) Layered translation (Repository -> Service -> Controller)
    //    - Each layer translates to its vocabulary
    // ------------------------------------------------------------
    private static void demoLayeredTranslation() {
        section("9) Layered translation across architecture");
        try {
            controllerGetUser(42);
        } catch (ServiceException e) {
            System.out.println("Controller caught: " + e);
            printCauses(e);
        }
    }

    static class ServiceException extends Exception {
        public ServiceException(String message, Throwable cause) { super(message, cause); }
    }

    private static void controllerGetUser(int userId) throws ServiceException {
        try {
            serviceGetUser(userId);
        } catch (DataAccessException e) {
            // Translate infra/runtime to service-level checked exception
            throw new ServiceException("Service failed to get user " + userId, e);
        }
    }

    private static void serviceGetUser(int userId) {
        // Service delegates to repository which may throw DataAccessException
        repositoryGetUser(userId);
    }

    private static void repositoryGetUser(int userId) {
        try {
            dbFindUser(userId);
        } catch (SQLException e) {
            // Translate checked DB exception to unchecked repository-level exception
            throw new DataAccessException("Repository DB error while fetching user " + userId, e);
        }
    }

    private static void dbFindUser(int userId) throws SQLException {
        // Simulate a database failure
        throw new SQLException("Connection timeout while fetching user " + userId);
    }

    // ------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------
    private static void printCauses(Throwable t) {
        int i = 0;
        Throwable cur = t;
        while (cur != null) {
            System.out.println("cause[" + i + "]: " + cur.getClass().getSimpleName() + ": " + cur.getMessage());
            if (cur.getSuppressed().length > 0) {
                for (Throwable s : cur.getSuppressed()) {
                    System.out.println("  suppressed: " + s.getClass().getSimpleName() + ": " + s.getMessage());
                }
            }
            cur = cur.getCause();
            i++;
        }
    }

    private static void title(String t) {
        System.out.println("\n=== " + t + " ===\n");
    }

    private static void section(String s) {
        System.out.println("\n--- " + s + " ---");
    }

    private static void done() {
        System.out.println("\n=== Done ===");
    }
}