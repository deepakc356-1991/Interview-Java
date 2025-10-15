package _07_01_checked_vs_unchecked_exceptions;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.SQLException;

/**
 * Checked vs Unchecked Exceptions — Theory, Rules, and Idioms (with runnable examples).
 *
 * Key ideas:
 * - Hierarchy:
 *   Throwable
 *     - Error               (unchecked; serious problems, do not catch in normal code)
 *     - Exception           (mostly checked)
 *         - RuntimeException (unchecked; programming errors, contract violations)
 *
 * - Checked exceptions:
 *   - Any Exception that is NOT a RuntimeException (e.g., IOException, SQLException).
 *   - Must be either caught or declared with "throws" (compile-time enforced).
 *   - Use for recoverable conditions where callers can reasonably react.
 *
 * - Unchecked exceptions:
 *   - RuntimeException and its subclasses (e.g., NullPointerException, IllegalArgumentException).
 *   - Do not need to be declared or caught (optional handling).
 *   - Use for programming errors, precondition violations, illegal states.
 *
 * - throw vs throws:
 *   - "throw" actually throws an exception object.
 *   - "throws" declares that a method may throw (checked) exceptions, forcing callers to handle/declare.
 *   - You may declare unchecked exceptions, but the compiler does not enforce handling.
 *
 * - try-catch-finally and try-with-resources:
 *   - finally always executes (except in extreme cases like System.exit).
 *   - try-with-resources automatically closes AutoCloseable resources and records suppressed exceptions.
 *
 * - Multi-catch:
 *   - catch (IOException | SQLException e) handles multiple types with one block.
 *
 * - Exception chaining (aka translation):
 *   - Wrap a low-level exception into a higher-level one and include the cause:
 *     throw new DomainException("...", cause);
 *
 * - Overriding and checked exceptions:
 *   - Overriding methods cannot add new/broader checked exceptions.
 *   - They can remove or narrow them (e.g., from IOException to FileNotFoundException).
 *   - Unchecked exceptions are not restricted by overriding.
 *
 * - Errors:
 *   - Unchecked; generally do not catch (OutOfMemoryError, StackOverflowError).
 *
 * - Best practices:
 *   - Choose checked exceptions when callers can recover; unchecked for bugs.
 *   - Prefer specific exception types; do not swallow exceptions.
 *   - Do not use exceptions for normal control flow.
 *   - Catch broad exceptions only at application boundaries; log and fail fast or recover.
 *   - Preserve the cause when rethrowing (exception chaining).
 *   - InterruptedException: either propagate or restore interrupt (Thread.currentThread().interrupt()).
 *   - API evolution: adding a checked exception is a breaking change; adding an unchecked one is not.
 *
 * Notes:
 * - Code snippets that would not compile (to show compiler rules) are included as comments.
 * - Run this class to see prints demonstrating the concepts.
 */
public class _01_Theory {

    public static void main(String[] args) {
        heading("Hierarchy and definitions (see class-level comments)");
        exampleCheckedMustBeHandled();
        exampleUncheckedOptionalHandling();
        exampleThrowVsThrows();
        exampleCustomExceptions();
        exampleExceptionChaining();
        exampleTryWithResourcesAndSuppressed();
        exampleMultiCatch();
        exampleOverridingRules();
        exampleInterruptedBestPractice();
        exampleTranslateCheckedToUnchecked();

        heading("Done");
    }

    // --- Checked: must be caught or declared
    private static void exampleCheckedMustBeHandled() {
        heading("Checked: must be caught or declared");
        try {
            // FileInputStream constructor declares "throws FileNotFoundException" (checked)
            new FileInputStream("missing.txt");
        } catch (FileNotFoundException e) {
            System.out.println("Caught checked exception: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }

        // If we removed the catch above, the compiler would require:
        // private static void exampleCheckedMustBeHandled() throws FileNotFoundException { ... }
        // or a try-catch. Otherwise: compilation error.
    }

    // --- Unchecked: optional handling
    private static void exampleUncheckedOptionalHandling() {
        heading("Unchecked: optional handling");
        try {
            // NumberFormatException extends RuntimeException (unchecked)
            Integer.parseInt("not-a-number");
        } catch (NumberFormatException e) {
            System.out.println("Caught unchecked exception (optional): " + e.getClass().getSimpleName());
        }

        // No compiler requirement to catch/declare NumberFormatException.
    }

    // --- throw vs throws
    private static void exampleThrowVsThrows() {
        heading("throw vs throws");
        try {
            throwUncheckedNow(); // no "throws" needed on caller
        } catch (IllegalArgumentException e) {
            System.out.println("Handled unchecked thrown explicitly: " + e.getMessage());
        }

        try {
            throwCheckedNow(); // must handle or declare
        } catch (IOException e) {
            System.out.println("Handled checked thrown explicitly: " + e.getMessage());
        }
    }

    private static void throwUncheckedNow() {
        // "throw": actually throws; IllegalArgumentException is unchecked
        throw new IllegalArgumentException("Bad argument");
    }

    private static void throwCheckedNow() throws IOException {
        // "throws": declares possibility; IOException is checked
        throw new IOException("I/O failed");
    }

    // --- Custom exceptions: checked vs unchecked
    private static void exampleCustomExceptions() {
        heading("Custom exceptions (checked vs unchecked)");
        try {
            connectToDatabase(); // checked
        } catch (DatabaseUnavailableException e) {
            System.out.println("Recoverable path (checked): " + e.getMessage());
            // Caller might retry, fall back, or inform user.
        }

        try {
            loadConfiguration(); // unchecked; no compile-time requirement to catch
        } catch (ConfigurationException e) {
            System.out.println("Programming/config error (unchecked): " + e.getMessage());
            // Typically fix configuration or code; not a runtime recovery scenario.
        }
    }

    private static void connectToDatabase() throws DatabaseUnavailableException {
        // Considered recoverable (e.g., retry/backoff), hence checked
        throw new DatabaseUnavailableException("DB unreachable");
    }

    private static void loadConfiguration() {
        // Programming/configuration error; unchecked
        throw new ConfigurationException("Missing key 'host'");
    }

    // --- Exception chaining (translation)
    private static void exampleExceptionChaining() {
        heading("Exception chaining (wrapping a cause)");
        try {
            new FileInputStream("still-missing.txt");
        } catch (FileNotFoundException e) {
            RuntimeException wrapped = new RuntimeException("Service failed to start: cannot open config", e);
            System.out.println("Wrapped exception with cause (stack trace below):");
            wrapped.printStackTrace(System.out);
        }
        // Always keep the original cause so callers and logs can see the root problem.
    }

    // --- try-with-resources and suppressed exceptions
    private static void exampleTryWithResourcesAndSuppressed() {
        heading("try-with-resources and suppressed exceptions");
        try (DemoResource r = new DemoResource("R", true, true)) {
            r.use(); // throws primary exception
        } catch (IOException e) {
            System.out.println("Primary: " + e);
            for (Throwable s : e.getSuppressed()) {
                System.out.println("Suppressed: " + s);
            }
        }

        // Notes:
        // - If both the body and close() throw, the close() exception is suppressed and attached to the primary.
        // - Equivalent manual code would be longer and easier to get wrong.
        // - Avoid throwing in finally; it can hide the original exception.
    }

    // --- Multi-catch
    private static void exampleMultiCatch() {
        heading("Multi-catch");
        for (int i = 1; i <= 2; i++) {
            try {
                riskyIOOrSql(i);
            } catch (IOException | SQLException e) {
                System.out.println("Handled with multi-catch: " + e.getClass().getSimpleName() + " -> " + e.getMessage());
                // e is effectively final in multi-catch; you cannot reassign it.
            }
        }

        // Ordering rule (when using multiple catch blocks):
        // - Catch more specific exceptions before more general ones, else the general catch makes the specific one unreachable.
        //
        // Example (would not compile if reversed):
        // try { ... }
        // catch (FileNotFoundException e) { ... }       // specific first
        // catch (IOException e) { ... }                 // then general
    }

    private static void riskyIOOrSql(int mode) throws IOException, SQLException {
        if (mode == 1) throw new IOException("IO problem");
        else throw new SQLException("SQL problem");
    }

    // --- Overriding rules for checked exceptions
    private static void exampleOverridingRules() {
        heading("Overriding rules for checked exceptions");
        BaseReader b = new NarrowingReader();
        try {
            b.read();
        } catch (IOException e) {
            System.out.println("Caught from overridden method: " + e.getClass().getSimpleName());
        }

        // Illegal example (compile error) - cannot add broader/new checked exception in an override:
        // class BadOverride extends BaseReader {
        //     @Override
        //     public void read() throws SQLException { // ERROR: SQLException not allowed (broader/new checked)
        //         throw new SQLException();
        //     }
        // }
    }

    // --- InterruptedException: checked; best practice to restore interrupt
    private static void exampleInterruptedBestPractice() {
        heading("InterruptedException is checked; restore interrupt");
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                // Best practice: restore interrupt status so upstream code can see it
                Thread.currentThread().interrupt();
                System.out.println("Interrupted and interrupt status restored");
            }
        });
        t.start();
        t.interrupt();
        try {
            t.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // --- Translate checked to unchecked (when appropriate)
    private static void exampleTranslateCheckedToUnchecked() {
        heading("Translate checked to unchecked when appropriate");
        try {
            throwCheckedNow(); // throws IOException (checked)
        } catch (IOException e) {
            UncheckedIOException u = new UncheckedIOException("Wrapping checked as unchecked", e);
            System.out.println(u.getClass().getSimpleName() + ": " + u.getMessage()
                    + " | cause=" + u.getCause().getClass().getSimpleName());
        }

        // This pattern is common in callback/lambda/stream code that cannot declare checked exceptions.
        // Use judiciously and document behavior.
    }

    // Helpers
    private static void heading(String title) {
        System.out.println("\n--- " + title + " ---");
    }

    // Resource to demonstrate try-with-resources + suppressed
    private static class DemoResource implements AutoCloseable {
        private final String name;
        private final boolean throwOnUse;
        private final boolean throwOnClose;

        DemoResource(String name, boolean throwOnUse, boolean throwOnClose) {
            this.name = name;
            this.throwOnUse = throwOnUse;
            this.throwOnClose = throwOnClose;
        }

        void use() throws IOException {
            System.out.println(name + ": using");
            if (throwOnUse) throw new IOException(name + ": use failed");
        }

        @Override
        public void close() throws IOException {
            System.out.println(name + ": closing");
            if (throwOnClose) throw new IOException(name + ": close failed");
        }
    }

    // Base/Derived to demonstrate overriding rules
    private static class BaseReader {
        public void read() throws IOException {
            throw new IOException("Base read failed");
        }
    }

    private static class NarrowingReader extends BaseReader {
        @Override
        public void read() throws FileNotFoundException {
            // Narrowing from IOException to FileNotFoundException is allowed
            throw new FileNotFoundException("Narrower checked exception is allowed");
        }
    }

    // Custom checked exception
    private static class DatabaseUnavailableException extends Exception {
        public DatabaseUnavailableException(String message) {
            super(message);
        }

        public DatabaseUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    // Custom unchecked exception
    private static class ConfigurationException extends RuntimeException {
        public ConfigurationException(String message) {
            super(message);
        }

        public ConfigurationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /*
     Additional notes and tips:

     - try-finally pitfalls:
       - Avoid "return" or throwing from finally; it can hide original exceptions.
       - try-with-resources is preferred for resource management.

     - Precise rethrow (Java 7+):
       - The compiler can infer a narrowed thrown type on rethrow in some patterns
         (not shown here; useful but advanced).

     - Functional interfaces and checked exceptions:
       - Lambdas in streams cannot declare checked exceptions; options:
         1) Wrap checked as unchecked (e.g., UncheckedIOException), or
         2) Write small adapter methods that handle/translate exceptions.

     - Documentation:
       - Document unchecked exceptions in Javadoc even though not enforced.
       - Keep exception messages actionable and include key context.

     - Performance:
       - Throwing/catching is expensive; do not use exceptions for normal control flow.
    */
}