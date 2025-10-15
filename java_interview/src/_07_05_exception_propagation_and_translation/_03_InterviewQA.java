package _07_05_exception_propagation_and_translation;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.SQLException;
import java.util.concurrent.*;

/*
Interview Q&A: Exception Propagation & Translation (Basic → Intermediate → Advanced)

BASIC
Q1: What is exception propagation?
A: If an exception isn’t handled in a method, it bubbles up the call stack to the caller. Unchecked exceptions propagate automatically; checked ones require a throws clause or try-catch.
See: demoPropagationUnchecked(), demoPropagationChecked()

Q2: Difference between throw and throws?
A: throw actually throws an exception instance at runtime. throws declares (at compile-time) that a method may throw specific checked exceptions.
See: methodWithThrows(), methodUsingThrow()

Q3: Checked vs unchecked exceptions?
A: Checked (subclasses of Exception excluding RuntimeException) must be declared or handled. Unchecked (RuntimeException and Error) need not be declared or caught.
See: demoPropagationChecked() vs demoPropagationUnchecked()

Q4: When to use try-catch vs throws?
A: Catch when you can recover or add context. Otherwise declare throws and let the caller decide.
See: demoTranslation() adds context; demoPropagationChecked() re-declares.

Q5: What happens if an exception is never caught?
A: On the main thread, the JVM prints a stack trace and exits. On other threads, the thread terminates; results are wrapped when accessed via Future.
See: demoExecutorService(), demoCompletableFuture()

INTERMEDIATE
Q6: What is exception translation?
A: Converting a low-level exception to a higher-level, domain-appropriate exception, typically with the original as the cause.
See: demoTranslation(), UserAlreadyExistsException

Q7: Exception chaining (cause)?
A: Preserve the original failure by passing it as the cause when creating a new exception.
See: UserAlreadyExistsException(String, Throwable)

Q8: Rethrow patterns: throw e vs new WrappedException(e)?
A: throw e preserves the original stack; new WrappedException adds a new stack top and requires proper cause to preserve history.
See: demoRethrowComparison()

Q9: How to add context without losing the cause?
A: Wrap with a domain-specific type and include details in the message and cause.
See: demoTranslation()

Q10: Suppressed exceptions and try-with-resources?
A: Exceptions thrown while closing resources are added to the primary exception as suppressed.
See: demoSuppressed(), demoSuppressedMultipleClosers()

Q11: Pitfall—finally masking the original exception?
A: If finally throws, it hides the try-body exception.
See: demoFinallyMasking()

Q12: Multi-catch and ordering?
A: Catch sibling exceptions using |. Catch more specific types before general ones.
See: demoMultiCatch()

Q13: Custom exceptions?
A: Use meaningful names, extend appropriate base classes, include (msg, cause) constructors.
See: DataAccessException, ServiceException, UserAlreadyExistsException

ADVANCED
Q14: Precise rethrow (Java 7+)?
A: The compiler can infer the exact thrown checked types when rethrowing from a catch(Exception e), if method signature includes them.
See: demoPreciseRethrow()

Q15: Cross-thread propagation?
A: ExecutorService wraps exceptions in ExecutionException (via Future.get). CompletableFuture wraps in CompletionException (join) or ExecutionException (get).
See: demoExecutorService(), demoCompletableFuture()

Q16: Static initializer failures?
A: Exceptions in static initializers are wrapped in ExceptionInInitializerError.
See: demoStaticInitializerError()

Q17: Converting checked to unchecked at boundaries?
A: Use UncheckedIOException or your own RuntimeException at module boundaries.
See: demoUncheckedIOException()

Q18: Sneaky throw (anti-pattern)?
A: Throws checked exceptions without declaring them via generics trick; breaks API contracts.
See: sneakyThrow(), demoSneakyThrow()

Q19: Logging best practices?
A: Log once at the boundary, avoid double logging, preserve cause, add actionable context. Don’t swallow exceptions silently.

Q20: Don’t catch Throwable/Error unless you can handle them (usually you cannot).
A: Catch Exception or specific subtypes; Errors like OutOfMemoryError are not recoverable.

Q21: API design for translation?
A: Keep low-level details hidden, expose stable, high-level exception types for your consumers.

Q22: Framework note (e.g., Spring): Transactions typically roll back on unchecked; configure rollbackFor for checked.

Run main() to see printed summaries of the demonstrations.
*/
public class _03_InterviewQA {

    public static void main(String[] args) {
        System.out.println("=== BASIC: Unchecked propagation ===");
        try {
            demoPropagationUnchecked();
        } catch (RuntimeException e) {
            printSummary("Unchecked propagation", e);
        }

        System.out.println("=== BASIC: Checked propagation ===");
        try {
            demoPropagationChecked();
        } catch (Exception e) {
            printSummary("Checked propagation", e);
        }

        System.out.println("=== throw vs throws (see comments) ===");
        try {
            methodWithThrows();
        } catch (IOException e) {
            printSummary("methodWithThrows()", e);
        }
        try {
            methodUsingThrow();
        } catch (IllegalArgumentException e) {
            printSummary("methodUsingThrow()", e);
        }

        System.out.println("=== Rethrow: preserve vs wrap ===");
        demoRethrowComparison();

        System.out.println("=== Translation (domain) ===");
        demoTranslation();

        System.out.println("=== Bad translation (loses cause) ===");
        try {
            demoBadTranslationLosesCause();
        } catch (ServiceException e) {
            printSummary("Bad translation", e);
        }

        System.out.println("=== Try-with-resources suppressed ===");
        demoSuppressed();

        System.out.println("=== Multiple resource closers (suppressed) ===");
        demoSuppressedMultipleClosers();

        System.out.println("=== Finally masking original exception ===");
        demoFinallyMasking();

        System.out.println("=== Multi-catch ===");
        demoMultiCatch();

        System.out.println("=== Precise rethrow (Java 7+) ===");
        try {
            demoPreciseRethrow(true);
        } catch (IOException e) {
            printSummary("Precise rethrow: IOException path", e);
        } catch (SQLException e) {
            printSummary("Precise rethrow: SQLException path", e);
        }
        try {
            demoPreciseRethrow(false);
        } catch (IOException e) {
            printSummary("Precise rethrow: IOException path", e);
        } catch (SQLException e) {
            printSummary("Precise rethrow: SQLException path", e);
        }

        System.out.println("=== CompletableFuture propagation ===");
        demoCompletableFuture();

        System.out.println("=== ExecutorService propagation ===");
        demoExecutorService();

        System.out.println("=== ExceptionInInitializerError (static initializer) ===");
        demoStaticInitializerError();

        System.out.println("=== UncheckedIOException at boundaries ===");
        try {
            demoUncheckedIOException();
        } catch (UncheckedIOException e) {
            printSummary("UncheckedIOException", e);
        }

        System.out.println("=== Sneaky throw (anti-pattern) ===");
        try {
            demoSneakyThrow();
        } catch (Exception e) {
            printSummary("Sneaky throw surfaced", e);
        }

        System.out.println("=== Manual addSuppressed() ===");
        demoAddSuppressed();

        System.out.println("=== END ===");
    }

    // =========================
    // BASIC DEMOS
    // =========================

    private static void demoPropagationUnchecked() {
        uncheckedLevel1();
    }

    private static void uncheckedLevel1() {
        uncheckedLevel2();
    }

    private static void uncheckedLevel2() {
        uncheckedLevel3();
    }

    private static void uncheckedLevel3() {
        throw new IllegalStateException("Boom at uncheckedLevel3()");
    }

    private static void demoPropagationChecked() throws Exception {
        checkedLevel1();
    }

    private static void checkedLevel1() throws Exception {
        checkedLevel2();
    }

    private static void checkedLevel2() throws Exception {
        checkedLevel3();
    }

    private static void checkedLevel3() throws Exception {
        throw new Exception("Checked fail at checkedLevel3()");
    }

    // throw vs throws
    // throws: declares a checked exception to the caller.
    private static void methodWithThrows() throws IOException {
        throw new IOException("Declared via throws");
    }

    // throw: actually throws an exception instance.
    private static void methodUsingThrow() {
        throw new IllegalArgumentException("Actually thrown via throw");
    }

    // =========================
    // INTERMEDIATE DEMOS
    // =========================

    private static void demoRethrowComparison() {
        try {
            rootThrows();
        } catch (RuntimeException e) {
            try {
                rethrowPreserve(e);
            } catch (RuntimeException preserved) {
                printSummary("Rethrow preserve (throw e)", preserved);
            }
            try {
                wrapAndRethrow(e);
            } catch (RuntimeException wrapped) {
                printSummary("Rethrow wrap (new RuntimeException(e))", wrapped);
            }
        }
    }

    private static void rootThrows() {
        // Artificial failure to show stack traces
        helperDepth();
    }

    private static void helperDepth() {
        throw new RuntimeException("Root cause at helperDepth()");
    }

    private static void rethrowPreserve(RuntimeException e) {
        // Preserves original stack trace
        throw e;
    }

    private static void wrapAndRethrow(RuntimeException e) {
        // Adds a new frame; preserves cause via chaining
        throw new RuntimeException("Wrapped at boundary with context", e);
    }

    // Translation example: repository -> service converts SQLException to domain exception
    private static void demoTranslation() {
        UserService service = new UserService(new UserRepository());
        try {
            service.register("alice");
        } catch (UserAlreadyExistsException e) {
            printSummary("Translation example (domain)", e);
        }
    }

    private static void demoBadTranslationLosesCause() {
        UserRepository repo = new UserRepository();
        try {
            repo.save("bob");
        } catch (SQLException e) {
            // Anti-pattern: losing cause
            throw new ServiceException("User operation failed (lost original cause)");
        }
    }

    private static void demoSuppressed() {
        try (ExplodingResource r1 = new ExplodingResource("r1")) {
            throw new RuntimeException("Primary failure in body");
        } catch (RuntimeException e) {
            printSummary("Try-with-resources with suppressed close()", e);
            for (Throwable s : e.getSuppressed()) {
                System.out.println("  suppressed: " + s);
            }
        }
    }

    private static void demoSuppressedMultipleClosers() {
        try (ExplodingResource a = new ExplodingResource("A");
             ExplodingResource b = new ExplodingResource("B")) {
            // No exception in body; closers will throw.
        } catch (Exception e) {
            printSummary("Closer threw primary; others suppressed", e);
            for (Throwable s : e.getSuppressed()) {
                System.out.println("  suppressed: " + s);
            }
        }
    }

    private static void demoFinallyMasking() {
        try {
            methodThatThrowsAndFinallyThrows();
        } catch (Exception e) {
            // Notice: no suppressed here; finally masked the try-body exception entirely
            printSummary("Finally masked original", e);
        }
    }

    private static void methodThatThrowsAndFinallyThrows() {
        try {
            throw new IllegalArgumentException("Try-body exception");
        } finally {
            throw new IllegalStateException("Finally exception (masks try-body)");
        }
    }

    private static void demoMultiCatch() {
        try {
            mightThrow(true);
        } catch (IOException | SQLException e) {
            printSummary("Multi-catch captured", e);
        }

        try {
            mightThrow(false);
        } catch (IOException | SQLException e) {
            printSummary("Multi-catch captured", e);
        }
    }

    // =========================
    // ADVANCED DEMOS
    // =========================

    private static void demoPreciseRethrow(boolean ioPath) throws IOException, SQLException {
        try {
            mightThrow(ioPath);
        } catch (Exception e) {
            // Compiler knows e is either IOException or SQLException, as declared by mightThrow.
            throw e; // precise rethrow
        }
    }

    private static void mightThrow(boolean ioPath) throws IOException, SQLException {
        if (ioPath) {
            throw new IOException("IO path");
        } else {
            throw new SQLException("SQL path");
        }
    }

    private static void demoCompletableFuture() {
        CompletableFuture<String> f1 = CompletableFuture.supplyAsync(() -> {
            throw new IllegalArgumentException("CF failure (join wraps in CompletionException)");
        });

        try {
            f1.join();
        } catch (CompletionException e) {
            printSummary("CompletableFuture join()", e);
            System.out.println("  cause from join(): " + e.getCause());
        }

        CompletableFuture<String> f2 = CompletableFuture.supplyAsync(() -> {
            throw new IllegalStateException("CF failure (get wraps in ExecutionException)");
        });

        try {
            f2.get();
        } catch (ExecutionException e) {
            printSummary("CompletableFuture get()", e);
            System.out.println("  cause from get(): " + e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void demoExecutorService() {
        ExecutorService es = Executors.newSingleThreadExecutor();
        try {
            Future<Void> fut = es.submit(() -> {
                throw new IOException("Callable IO fails inside thread");
            });

            fut.get();
        } catch (ExecutionException e) {
            printSummary("ExecutorService Future.get()", e);
            System.out.println("  cause: " + e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            es.shutdown();
        }
    }

    private static void demoStaticInitializerError() {
        try {
            new BrokenClass();
        } catch (ExceptionInInitializerError e) {
            printSummary("Static initializer failure", e);
            System.out.println("  cause from static init: " + e.getCause());
        }
    }

    private static void demoUncheckedIOException() {
        try {
            methodThatThrowsIOException();
        } catch (IOException e) {
            throw new UncheckedIOException("Converted to unchecked at boundary", e);
        }
    }

    private static void methodThatThrowsIOException() throws IOException {
        throw new IOException("Disk full");
    }

    private static void demoSneakyThrow() {
        // Throws checked IOException without declaring; compiles due to generics erasure trick (anti-pattern).
        sneakyThrow(new IOException("Sneaky checked exception"));
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }

    private static void demoAddSuppressed() {
        Exception primary = new Exception("Primary");
        Exception secondary = new Exception("Secondary (suppressed)");
        primary.addSuppressed(secondary);
        printSummary("Manual addSuppressed", primary);
        for (Throwable s : primary.getSuppressed()) {
            System.out.println("  suppressed: " + s);
        }
    }

    // =========================
    // Helpers & Types
    // =========================

    private static void printSummary(String title, Throwable t) {
        System.out.println(title + " -> " + t.getClass().getSimpleName() + ": " + t.getMessage());
        System.out.println("Cause: " + (t.getCause() == null ? "null" : t.getCause().getClass().getName() + ": " + t.getCause().getMessage()));
        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < Math.min(3, st.length); i++) {
            System.out.println("  at " + st[i]);
        }
        if (t.getSuppressed() != null && t.getSuppressed().length > 0) {
            System.out.println("Suppressed count: " + t.getSuppressed().length);
        }
        System.out.println();
    }

    // Repository and Service demonstrating translation
    static final class UserRepository {
        void save(String username) throws SQLException {
            // Simulate duplicate key error
            throw new SQLException("Duplicate key for " + username, "23505");
        }
    }

    static final class UserService {
        private final UserRepository repo;

        UserService(UserRepository repo) {
            this.repo = repo;
        }

        void register(String username) {
            try {
                repo.save(username);
            } catch (SQLException e) {
                // Translate low-level SQL to domain-specific exception with context
                throw new UserAlreadyExistsException("Username taken: " + username, e);
            }
        }
    }

    static class UserAlreadyExistsException extends RuntimeException {
        UserAlreadyExistsException(String message, Throwable cause) { super(message, cause); }
    }

    static class DataAccessException extends RuntimeException {
        DataAccessException(String message, Throwable cause) { super(message, cause); }
    }

    static class ServiceException extends RuntimeException {
        ServiceException(String message) { super(message); }
        ServiceException(String message, Throwable cause) { super(message, cause); }
    }

    static final class ExplodingResource implements AutoCloseable {
        private final String name;

        ExplodingResource(String name) {
            this.name = name;
        }

        @Override
        public void close() {
            throw new IllegalStateException("close() failed for " + name);
        }
    }

    static final class BrokenClass {
        static {
            if (true) {
                throw new RuntimeException("Static initializer blew up");
            }
        }

        BrokenClass() {
        }
    }
}