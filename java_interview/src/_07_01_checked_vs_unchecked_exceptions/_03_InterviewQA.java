package _07_01_checked_vs_unchecked_exceptions;

/*
Interview Q&A: Checked vs Unchecked Exceptions (Basic → Advanced)

Basics
- Q: What is a checked exception?
  A: An exception that must be either caught or declared in a method/constructor's throws clause. Subclasses of Exception excluding RuntimeException and its subclasses (e.g., IOException, SQLException).

- Q: What is an unchecked exception?
  A: Subclasses of RuntimeException (e.g., NullPointerException, IllegalArgumentException). They are not forced by the compiler to be caught or declared.

- Q: What about Error?
  A: Subclasses of Error (e.g., OutOfMemoryError). Serious problems you generally should not catch or recover from.

- Q: Why checked exceptions?
  A: To enforce handling/recovery for anticipated, recoverable conditions (e.g., file not found, network issues).

- Q: Do unchecked exceptions need to be declared?
  A: No. You can declare them but it's optional and not enforced.

- Q: Which must be caught or declared?
  A: Only checked exceptions.

- Q: Can I catch Throwable?
  A: Technically yes, but avoid except at the top-level boundary; it also catches Errors.

- Q: Does throws clause affect overloading?
  A: No. Overloading resolution ignores throws. You cannot overload solely by throws differences.

Intermediate
- Q: How do I create custom checked/unchecked exceptions?
  A: Extend Exception for checked; extend RuntimeException for unchecked.

- Q: Overriding and exceptions?
  A: Overriding methods cannot add new/broader checked exceptions; they can declare fewer/narrower or none. Unchecked exceptions are not restricted.

- Q: Multi-catch?
  A: catch (IOException | SQLException e) { ... } Use for sibling exceptions. Order matters when not using multi-catch: catch more specific first.

- Q: try-with-resources and suppressed exceptions?
  A: Exceptions thrown during resource closing are suppressed and accessible via getSuppressed() on the primary exception.

- Q: Exception translation vs chaining?
  A: Translation: convert low-level exceptions to higher-level domain-specific ones. Always chain with the cause (new X(..., cause)) to preserve diagnostics.

- Q: Are exceptions expensive?
  A: Throwing/catching creates stack traces; avoid using exceptions for normal control flow.

- Q: Static initializer exceptions?
  A: Any exception that escapes a static initializer is wrapped in ExceptionInInitializerError.

- Q: Constructors and exceptions?
  A: Constructors can throw checked exceptions. If thrown, object construction fails and no partially constructed instance is returned.

Advanced
- Q: Checked exceptions and lambdas/streams?
  A: Functional interfaces don’t declare checked exceptions; wrap, handle, translate, or use a helper that wraps checked exceptions in unchecked.

- Q: Checked exceptions across threads?
  A: They don’t cross thread boundaries directly. Future.get throws ExecutionException; CompletableFuture.join wraps in CompletionException.

- Q: “Sneaky throw”?
  A: Generic trick to throw checked exceptions without declaring; compiles but is discouraged for production API design.

- Q: API design: when to use checked vs unchecked?
  A: Use checked for recoverable, anticipated conditions the caller can reasonably handle; unchecked for programming errors (preconditions, illegal state, nulls). Many modern libraries prefer unchecked for API evolvability.

- Q: Logging/handling best practices?
  A: Handle exceptions at boundaries, preserve causes, avoid swallowing, avoid double-logging, and translate to meaningful types for callers.

- Q: Can I declare unchecked in throws?
  A: Yes, but it adds little value; can be used for documentation.

- Q: Unreachable catch?
  A: Catching a superclass before subclass causes “unreachable catch block” compile error.

- Q: Performance micro-optimizations?
  A: In hot paths where exceptions are used sparingly but created frequently, a custom RuntimeException overriding fillInStackTrace can reduce cost, but hurts diagnostics. Use sparingly.

*/

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Function;

public class _03_InterviewQA {

    // Custom exceptions
    static class CheckedCustomException extends Exception {
        public CheckedCustomException(String message) { super(message); }
        public CheckedCustomException(String message, Throwable cause) { super(message, cause); }
    }
    static class UncheckedCustomException extends RuntimeException {
        public UncheckedCustomException(String message) { super(message); }
        public UncheckedCustomException(String message, Throwable cause) { super(message, cause); }
    }
    static class FatalCustomError extends Error {
        public FatalCustomError(String message) { super(message); }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== Q&A Demo: Checked vs Unchecked Exceptions ===");

        demonstrateBasicDifferences();
        multiCatchDemo();
        tryWithResourcesSuppressedDemo();
        overridingDemo();
        translateExceptionDemo();
        lambdaCheckedDemo();
        asyncCheckedExceptionDemo();
        completableFutureDemo();
        staticInitializerDemo();
        sneakyThrowDemo();
        lightweightExceptionDemo();

        System.out.println("=== End of demo ===");
    }

    // Basic difference demo: checked must be declared/caught; unchecked not required.
    static void demonstrateBasicDifferences() {
        System.out.println("\n-- Basic differences --");
        // Checked: must catch or declare
        try {
            mayThrowChecked();
        } catch (IOException e) {
            System.out.println("Handled checked exception: " + e);
        }

        // Unchecked: not required to declare/catch
        try {
            mayThrowUnchecked();
        } catch (RuntimeException e) {
            System.out.println("Handled unchecked exception (optional): " + e);
        }

        // Error: generally do not catch
        // Uncommenting below is discouraged; shown for completeness.
        // throw new FatalCustomError("Serious problem; typically unrecoverable.");
    }

    static void mayThrowChecked() throws IOException {
        throw new IOException("Simulated I/O problem (checked)");
    }

    static void mayThrowUnchecked() {
        throw new IllegalArgumentException("Bad argument (unchecked)");
    }

    // Multi-catch demo (since Java 7)
    static void multiCatchDemo() {
        System.out.println("\n-- Multi-catch --");
        try {
            simulateTwoKindsOfFailures(1);
            simulateTwoKindsOfFailures(2);
        } catch (IllegalStateException | ArithmeticException e) {
            System.out.println("Caught via multi-catch: " + e.getClass().getSimpleName() + " -> " + e.getMessage());
        }
    }

    static void simulateTwoKindsOfFailures(int mode) {
        if (mode == 1) throw new IllegalStateException("Mode 1 failed");
        if (mode == 2) throw new ArithmeticException("Mode 2 failed");
    }

    // try-with-resources + suppressed exceptions demo
    static void tryWithResourcesSuppressedDemo() {
        System.out.println("\n-- try-with-resources & suppressed exceptions --");
        try {
            useResourcesThatMayFail();
        } catch (Exception e) {
            System.out.println("Primary exception: " + e.getClass().getSimpleName() + " -> " + e.getMessage());
            for (Throwable suppressed : e.getSuppressed()) {
                System.out.println("  suppressed: " + suppressed.getClass().getSimpleName() + " -> " + suppressed.getMessage());
            }
        }
    }

    static class DemoResource implements AutoCloseable {
        private final String name;
        private final boolean throwOnClose;
        DemoResource(String name, boolean throwOnClose) { this.name = name; this.throwOnClose = throwOnClose; }
        @Override
        public void close() throws IOException {
            if (throwOnClose) throw new IOException("close failed for " + name);
            System.out.println("closed " + name);
        }
    }

    static void useResourcesThatMayFail() throws Exception {
        try (DemoResource r1 = new DemoResource("r1", true);
             DemoResource r2 = new DemoResource("r2", true)) {
            throw new CheckedCustomException("Primary failure inside try");
        }
    }

    // Overriding rules demo
    static void overridingDemo() {
        System.out.println("\n-- Overriding rules (checked vs unchecked) --");
        BaseReader r = new ChildReaderOk();
        try {
            // Note: static type is BaseReader, so caller must handle IOException as declared by base.
            r.read();
        } catch (IOException e) {
            System.out.println("Handled IOException from base contract: " + e.getClass().getSimpleName());
        }
        // Unchecked can be thrown freely by overrides; no compile-time enforcement.
        r.compute(); // may throw unchecked; no catch required

        // Non-compiling example (for reference):
        // class BadChild extends BaseReader {
        //     @Override public void read() throws Exception { } // ERROR: broader checked exception not allowed
        // }
    }

    static class BaseReader {
        public void read() throws IOException { /* default impl */ }
        public void compute() { /* may throw unchecked at runtime */ }
    }
    static class ChildReaderOk extends BaseReader {
        @Override public void read() throws FileNotFoundException { throw new FileNotFoundException("Narrower checked"); }
        @Override public void compute() throws RuntimeException { /* unchecked allowed */ }
    }

    // Exception translation and chaining demo
    static void translateExceptionDemo() {
        System.out.println("\n-- Exception translation & chaining --");
        Path missing = Path.of("this_file_does_not_exist_" + System.nanoTime() + ".txt");
        try {
            Files.readString(missing); // throws IOException (checked)
        } catch (IOException e) {
            // Translate to domain-specific unchecked exception, preserving cause
            throw new UncheckedCustomException("Failed to load config: " + missing, e);
        }
    }

    // Lambdas and checked exceptions: wrap helper
    @FunctionalInterface
    interface ThrowingFunction<T, R, E extends Exception> { R apply(T t) throws E; }

    static <T, R, E extends Exception> Function<T, R> wrap(ThrowingFunction<T, R, E> fn) {
        return t -> {
            try {
                return fn.apply(t);
            } catch (Exception e) {
                // Translate to unchecked to satisfy functional interfaces
                throw new UncheckedCustomException("Lambda failed", e);
            }
        };
    }

    static void lambdaCheckedDemo() {
        System.out.println("\n-- Lambdas & checked exceptions (wrapping) --");
        List<Path> paths = Arrays.asList(
                Path.of("nope_" + System.nanoTime() + ".txt"),
                Path.of("also_missing_" + System.nanoTime() + ".txt")
        );

        // Without wrap: Files.readString throws checked IOException and cannot be used directly in map
        // With wrap: convert to Function that rethrows as unchecked
        try {
            paths.stream()
                 .map(wrap(Files::readString)) // if missing, throws UncheckedCustomException
                 .map(String::length)
                 .forEach(len -> System.out.println("len=" + len));
        } catch (UncheckedCustomException e) {
            System.out.println("Wrapped checked exception in stream: " + e.getCause());
        }
    }

    // Checked exceptions across threads
    static void asyncCheckedExceptionDemo() throws InterruptedException {
        System.out.println("\n-- Checked exceptions across threads (ExecutorService/Future) --");
        ExecutorService es = Executors.newSingleThreadExecutor();
        Future<String> f = es.submit(() -> {
            // Can't declare checked exception in Callable's call signature beyond Exception; throw checked internally
            throw new IOException("IO in worker (checked)");
        });
        try {
            f.get(); // wraps in ExecutionException
        } catch (ExecutionException e) {
            System.out.println("ExecutionException cause: " + e.getCause());
        } finally {
            es.shutdownNow();
        }
    }

    static void completableFutureDemo() {
        System.out.println("\n-- CompletableFuture (get vs join, wrapping) --");
        CompletableFuture<String> cf = CompletableFuture.supplyAsync(() -> {
            try {
                mayThrowChecked();
                return "OK";
            } catch (IOException e) {
                throw new UncheckedCustomException("Async IO failed", e);
            }
        });

        try {
            cf.join(); // throws CompletionException (unchecked) on failure
        } catch (CompletionException e) {
            System.out.println("CompletionException cause: " + e.getCause());
        }

        // Alternatively:
        CompletableFuture<String> cf2 = CompletableFuture.supplyAsync(() -> "value");
        try {
            System.out.println("get(): " + cf2.get()); // get throws checked ExecutionException/InterruptedException
        } catch (InterruptedException | ExecutionException e) {
            System.out.println("get() failed: " + e);
        }
    }

    // Static initializer exceptions
    static void staticInitializerDemo() {
        System.out.println("\n-- Static initializer exceptions --");

        try {
            new BadStaticUnchecked(); // RuntimeException in <clinit> becomes ExceptionInInitializerError
        } catch (ExceptionInInitializerError e) {
            System.out.println("Caught EIIE (unchecked thrown in clinit): cause=" + e.getCause());
        }

        try {
            new BadStaticChecked(); // Checked must be caught/translated inside clinit; we rethrow as EIIE manually
        } catch (ExceptionInInitializerError e) {
            System.out.println("Caught EIIE (checked translated): cause=" + e.getCause());
        }
    }

    static class BadStaticUnchecked {
        static { if (true) throw new RuntimeException("Unchecked from static initializer"); }
    }
    static class BadStaticChecked {
        static {
            try {
                checkedOp();
            } catch (IOException e) {
                // Any Throwable escaping clinit results in EIIE; here we do it explicitly for a checked cause
                throw new ExceptionInInitializerError(e);
            }
        }
        static void checkedOp() throws IOException { throw new IOException("Checked from static initializer"); }
    }

    // Sneaky throw (discouraged)
    static void sneakyThrowDemo() {
        System.out.println("\n-- Sneaky throw (discouraged) --");
        try {
            // “Convince” the compiler that only RuntimeException may be thrown
            _03_InterviewQA.<RuntimeException>sneakyThrow(new IOException("Sneaky checked"));
        } catch (Throwable t) {
            System.out.println("Actually threw at runtime: " + t + " (bypassed compile-time checking)");
        }
    }

    // Generic trick: bypass checked rules. Don’t use in public APIs.
    @SuppressWarnings("unchecked")
    static <E extends Throwable> void sneakyThrow(Throwable t) throws E {
        throw (E) t; // unchecked cast; compiles; will throw at runtime
    }

    // Lightweight exception optimization
    static void lightweightExceptionDemo() {
        System.out.println("\n-- Lightweight RuntimeException (no stack trace) --");
        try {
            throw new LightweightRuntimeException("No stack trace for hot paths");
        } catch (LightweightRuntimeException e) {
            System.out.println("Caught: " + e + " | stackTrace.length=" + e.getStackTrace().length);
        }
    }

    // Not for general use; reduces diagnostic info
    static class LightweightRuntimeException extends RuntimeException {
        LightweightRuntimeException(String message) { super(message); }
        @Override public synchronized Throwable fillInStackTrace() { return this; } // skip stack trace creation
    }

    /*
    Additional interview notes:
    - Best practices:
      - Use checked for recoverable conditions callers can handle; unchecked for programming errors.
      - Don’t swallow exceptions; handle at appropriate boundaries; preserve cause.
      - Avoid catching Exception broadly unless at boundary; prefer specific types.
      - Don’t use exceptions for normal control flow.
      - Document semantics and mapping (e.g., to HTTP status) and prefer domain-specific exceptions.

    - Finally nuances:
      - finally always runs (barring JVM crash). Returns in finally override prior returns; avoid return in finally.
      - try-with-resources preferred to manual finally for closing.

    - Declaring unchecked in throws:
      - Allowed but optional; can help documentation.

    - Errors:
      - Generally don’t catch; if you must log and abort, do it at highest boundary.

    - Unreachable catch:
      - catch(Throwable) before catch(Exception) makes catch(Exception) unreachable.

    - Streams/lambdas:
      - Bridge checked exceptions via wrapping or custom functional interfaces.

    - Concurrency:
      - Future.get → ExecutionException; CompletableFuture.join → CompletionException (unchecked).
      - Always shut down ExecutorService to avoid leaks.

    - API evolvability:
      - Adding new checked exceptions breaks callers; unchecked are more evolvable. Many modern APIs prefer unchecked.

    - Constructors:
      - Can throw checked exceptions; callers must handle. Object isn’t constructed if thrown.

    - Logging:
      - Avoid double-logging before rethrowing; log once at boundary or propagate with cause.

    - Performance:
      - Stack trace capture is costly; throw exceptions sparingly in hot paths.
    */
}