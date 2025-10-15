package _07_02_try_catch_finally;

/*
Try/Catch/Finally – Interview Q&A (Basic → Advanced)

Basic
1) What is try/catch/finally?
   - try encloses code that may throw exceptions.
   - catch handles specific exceptions.
   - finally always executes (for cleanup) whether an exception occurs or not.

2) Can you have try without catch?
   - Yes: try + finally, or try-with-resources (TWR). You must have at least one of catch or finally.

3) Does finally always execute?
   - Generally yes, even if there’s a return or break in try/catch.
   - It does not run on: System.exit(), Runtime.halt(), fatal JVM errors (e.g., OutOfMemoryError in some cases), process kill, power loss.

4) Difference between throw and throws?
   - throw: actually throws an exception instance at runtime.
   - throws: method signature declares possible exceptions the method may throw.

5) Checked vs unchecked exceptions?
   - Checked (e.g., IOException): must be declared or caught.
   - Unchecked (RuntimeException and subclasses): not required to declare or catch.

6) Multiple catch blocks ordering?
   - More specific before more general. Otherwise you get an unreachable catch compile error.
   - Multi-catch (catch (A | B e)) handles multiple unrelated types at once.

7) Finally and return values?
   - If both try and finally return, finally wins (overrides). Don’t return from finally in real code.
   - If try returns a value and finally modifies local variables, the returned value was already computed.

8) What happens if exception is not caught?
   - It propagates up the call stack. If it reaches the thread’s top level, the thread terminates.

9) Stack trace?
   - e.printStackTrace() or logging frameworks to record it.

Intermediate
10) Try-with-resources (TWR)?
   - try (Resource r = ...) where Resource implements AutoCloseable.
   - Resources are closed automatically in reverse (LIFO) order.
   - Exceptions thrown in close() are suppressed and attached to the primary exception (getSuppressed()).

11) Suppressed vs cause?
   - Cause: underlying reason for the exception (one cause via initCause or constructor).
   - Suppressed: additional exceptions (e.g., from closing resources) attached to the primary one.

12) Rethrow and improved rethrow (Java 7+):
   - You can catch, log, and rethrow the same exception type; the compiler may preserve the exact type in some patterns.

13) Exception translation/wrapping:
   - Wrap low-level exceptions into domain-specific ones while preserving cause.

14) Best practices:
   - Keep try scope minimal.
   - Don’t swallow exceptions (empty catch).
   - Prefer specific catch types.
   - Use finally or TWR for cleanup.
   - Don’t use exceptions for control flow.

Advanced
15) Finally vs finalize vs final?
   - finally: block that always executes.
   - finalize(): deprecated; unreliable cleanup.
   - final: modifier for constants, non-overridable methods, or non-reassignable variables.

16) Exceptions in static initializers?
   - Thrown exceptions are wrapped in ExceptionInInitializerError and prevent class initialization.

17) Exceptions across threads?
   - Uncaught exceptions terminate the thread; use UncaughtExceptionHandler or framework-specific handlers.

18) TWR closing order and masks:
   - Body exception is primary; close() exceptions are suppressed (not lost, unlike exceptions from finally blocks pre-Java 7 patterns).

19) When will finally not run?
   - System.exit(), Runtime.halt(), fatal JVM error, process kill, power loss, native crash.

20) Common pitfalls:
   - Returning from finally (overrides or hides original outcome).
   - Throwing from finally (can hide original exception if not using TWR).
   - Catching Throwable or Exception too broadly.
   - Logging and rethrowing without context or duplicating logs.
*/

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public class _03_InterviewQA {

    public static void main(String[] args) {
        System.out.println("Running try/catch/finally interview Q&A demos...\n");

        demoBasicFlow();
        demoFinallyWithReturn();
        demoFinallyModifyingLocal();
        demoExceptionInFinallyOverridesTry();
        demoTryWithResourcesSuppressed();
        demoMultiCatchAndOrder();
        demoCheckedVsUnchecked();
        demoRethrowAndChaining();
        demoTryFinallyWithoutCatch();
        demoStaticInitializerException();
        demoUncaughtExceptionInThread();
        demoCompletableFutureException();

        System.out.println("\nDone.");
    }

    // Q: Basic flow with try/catch/finally
    private static void demoBasicFlow() {
        System.out.println("Demo: basic try/catch/finally flow");
        try {
            System.out.println(" - In try");
            if (true) throw new RuntimeException("boom");
        } catch (RuntimeException e) {
            System.out.println(" - Caught: " + e.getMessage());
        } finally {
            System.out.println(" - finally always runs");
        }
        System.out.println();
    }

    // Q: What happens if both try and finally return?
    private static void demoFinallyWithReturn() {
        System.out.println("Demo: return in try and finally (finally overrides)");
        int v = returnFromTryAndFinally();
        System.out.println(" - Result: " + v + " (finally overrode try)\n");
    }

    private static int returnFromTryAndFinally() {
        try {
            System.out.println("   try returning 1");
            return 1;
        } finally {
            System.out.println("   finally returning 2 (overrides try)");
            return 2;
        }
    }

    // Q: If try returns a value and finally modifies locals, what is returned?
    private static void demoFinallyModifyingLocal() {
        System.out.println("Demo: try returns value, finally modifies local");
        int v = returnWithLocalModification();
        System.out.println(" - Returned: " + v + " (value computed before finally)\n");
    }

    private static int returnWithLocalModification() {
        int x = 1;
        try {
            return x; // value snapshot taken here
        } finally {
            x = 3; // does not affect already computed return value
            System.out.println("   finally changed local x to " + x);
        }
    }

    // Q: What if finally throws after try already threw an exception (no TWR)?
    // A: finally exception hides the try exception (original is lost).
    private static void demoExceptionInFinallyOverridesTry() {
        System.out.println("Demo: exception in finally hides try exception (no TWR)");
        try {
            methodWithExceptionInFinally();
        } catch (Exception e) {
            System.out.println(" - Caught: " + e + " (original try exception lost)");
        }
        System.out.println();
    }

    private static void methodWithExceptionInFinally() {
        try {
            throw new IllegalStateException("try-failure");
        } finally {
            throw new IllegalArgumentException("finally-failure");
        }
    }

    // Q: Try-with-resources: suppressed exceptions and close order
    private static void demoTryWithResourcesSuppressed() {
        System.out.println("Demo: try-with-resources suppressed exceptions and close order");
        try (DemoResource r1 = new DemoResource("R1", true);
             DemoResource r2 = new DemoResource("R2", true)) {

            System.out.println(" - Using resources");
            throw new RuntimeException("body-failure");

        } catch (Exception e) {
            System.out.println(" - Caught primary: " + e);
            System.out.println(" - Suppressed: " + Arrays.toString(e.getSuppressed()));
            // Close order: R2 then R1 (LIFO)
        }
        System.out.println();
    }

    // Q: Multi-catch and catch order
    private static void demoMultiCatchAndOrder() {
        System.out.println("Demo: multi-catch and catch order");
        try {
            throw new IllegalArgumentException("bad arg");
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println(" - Multi-catch caught: " + e.getClass().getSimpleName());
            // Note: in multi-catch, e is effectively final (cannot be reassigned).
        }

        try {
            throw new RuntimeException("runtime");
        } catch (RuntimeException e) {
            System.out.println(" - Specific before general is valid here");
        }
        // Uncompilable example (unreachable catch):
        // try { ... }
        // catch (Exception e) { ... }
        // catch (RuntimeException e) { ... } // unreachable
        System.out.println();
    }

    // Q: Checked vs Unchecked exceptions
    private static void demoCheckedVsUnchecked() {
        System.out.println("Demo: checked vs unchecked");
        try {
            mightThrowChecked();
        } catch (IOException e) {
            System.out.println(" - Caught checked: " + e);
        }

        try {
            mightThrowUnchecked();
        } catch (RuntimeException e) {
            System.out.println(" - Caught unchecked: " + e);
        }
        System.out.println();
    }

    private static void mightThrowChecked() throws IOException {
        throw new IOException("checked IO");
    }

    private static void mightThrowUnchecked() {
        throw new IllegalStateException("unchecked");
    }

    // Q: Rethrow and exception chaining
    private static void demoRethrowAndChaining() {
        System.out.println("Demo: rethrow and exception chaining");
        try {
            try {
                mightThrowChecked();
            } catch (IOException e) {
                System.out.println(" - Wrapping and rethrowing with cause");
                throw new RuntimeException("wrap checked", e);
            }
        } catch (RuntimeException e) {
            System.out.println(" - Caught: " + e + "; cause: " + e.getCause());
        }

        try {
            rethrowSameType();
        } catch (IOException e) {
            System.out.println(" - Rethrown same type preserved: " + e);
        }
        System.out.println();
    }

    private static void rethrowSameType() throws IOException {
        try {
            mightThrowChecked();
        } catch (IOException e) {
            System.out.println("   rethrowing IOException as-is (type preserved)");
            throw e; // improved rethrow keeps checked type
        }
    }

    // Q: try/finally without catch (cleanup + propagate)
    private static void demoTryFinallyWithoutCatch() {
        System.out.println("Demo: try/finally without catch (cleanup then propagate)");
        try {
            methodTryFinallyNoCatch();
        } catch (Exception e) {
            System.out.println(" - Observed propagated exception: " + e.getMessage());
        }
        System.out.println();
    }

    private static void methodTryFinallyNoCatch() throws Exception {
        try {
            System.out.println(" - Doing work that fails");
            throw new Exception("boom");
        } finally {
            System.out.println(" - Cleanup in finally runs before propagate");
        }
    }

    // Q: Exceptions in static initializers
    private static void demoStaticInitializerException() {
        System.out.println("Demo: exception in static initializer => ExceptionInInitializerError");
        try {
            new BadInit();
            System.out.println(" - Should not reach here");
        } catch (Throwable t) {
            System.out.println(" - Caught: " + t.getClass().getSimpleName() + " -> " + t);
            if (t.getCause() != null) {
                System.out.println(" - Underlying cause: " + t.getCause());
            }
        }
        System.out.println();
    }

    // Q: Uncaught exceptions in threads
    private static void demoUncaughtExceptionInThread() {
        System.out.println("Demo: uncaught exception handler for threads");
        Thread t = new Thread(() -> {
            throw new RuntimeException("boom on thread");
        }, "DemoThread");

        t.setUncaughtExceptionHandler((th, ex) ->
                System.out.println(" - Uncaught on " + th.getName() + ": " + ex));
        t.start();
        try {
            t.join();
        } catch (InterruptedException ignored) {
        }
        System.out.println();
    }

    // Q: Exceptions with CompletableFuture
    private static void demoCompletableFutureException() {
        System.out.println("Demo: CompletableFuture exception handling");
        CompletableFuture<Void> cf = CompletableFuture
                .supplyAsync(() -> {
                    throw new RuntimeException("async failure");
                })
                .handle((res, ex) -> {
                    System.out.println(" - CF handled: result=" + res + ", ex=" +
                            (ex == null ? "null" : ex.getClass().getSimpleName() + ": " + ex.getMessage()));
                    return null;
                });

        cf.join();
        System.out.println();
    }

    // Helper: resource for TWR demo
    private static final class DemoResource implements AutoCloseable {
        private final String name;
        private final boolean throwOnClose;

        DemoResource(String name, boolean throwOnClose) {
            this.name = name;
            this.throwOnClose = throwOnClose;
            System.out.println("   Opened " + name);
        }

        @Override
        public void close() throws Exception {
            System.out.println("   Closing " + name);
            if (throwOnClose) {
                throw new Exception("close-failure-" + name);
            }
        }
    }

    // Helper: class with bad static initializer
    private static final class BadInit {
        static {
            init(); // compiler can't prove this never returns
        }
        private static void init() {
            System.out.println("   BadInit static init throwing...");
            throw new RuntimeException("init error");
        }
    }

    /*
    Additional quick Q&A notes:
    - Why avoid catch(Throwable)?
      It catches Errors (serious problems) and disrupts control flow; prefer specific exceptions.
    - Can we assign to the multi-catch parameter?
      No; it’s effectively final.
    - Does TWR require AutoCloseable or Closeable?
      AutoCloseable; Closeable also works (it extends AutoCloseable).
    - Closing order in TWR?
      Reverse declaration order (LIFO).
    - Finally vs TWR?
      Prefer TWR for resource cleanup; it preserves primary exceptions and suppresses close() failures instead of losing them.
    - Will finally run after System.exit?
      No. Avoid System.exit in libraries; it prevents cleanup.
    - Is finalize() a good place for cleanup?
      No; it’s deprecated and unreliable. Use TWR or explicit close.
    - Performance?
      Exceptions are costly; don’t use them for normal control flow.
    - Logging best practice?
      Include context and stack trace; avoid swallowing exceptions silently.
    */
}