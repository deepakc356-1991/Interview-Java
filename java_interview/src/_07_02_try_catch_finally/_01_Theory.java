package _07_02_try_catch_finally;

/*
Topic: try/catch/finally theory in Java

Summary
- The try statement captures exceptional control flow.
- Legal forms:
  1) try { ... } catch (T e) { ... }
  2) try { ... } finally { ... }
  3) try { ... } catch (T1 e1) { ... } catch (T2 e2) { ... } finally { ... }
  4) try (resources) { ... } catch (...) { ... } finally { ... } // try-with-resources (TWR), related but distinct
- At least one of catch or finally is required with try.
- Control flow:
  - If no exception is thrown, control skips catch blocks; finally always executes.
  - If an exception is thrown:
    * The runtime searches for the first matching catch (most specific first).
    * If none matches here, the exception propagates outward; finally still executes before propagation.
- Checked vs unchecked:
  - Checked exceptions (non-RuntimeException) must be caught or declared.
  - Unchecked exceptions (RuntimeException and Error) need not be caught/declared.
- Multiple catch:
  - Order matters: catch more specific types before general ones, or you get a compile error (unreachable catch).
- Multi-catch (Java 7+):
  - catch (A | B e) when A and B are unrelated (no subclassing relationship). The catch parameter is effectively final.
- finally:
  - Runs even if:
    * The try/catch returns, breaks, or continues.
    * The exception is not caught here and is propagating outward.
  - May not run if:
    * System.exit(...) is invoked, the JVM crashes, the process is killed, or the machine loses power.
  - Avoid returning or throwing from finally; it overrides earlier returns/exceptions and can lose information.
- Rethrow:
  - You can rethrow caught exceptions; with multi-catch, rethrow can retain precise types.
- Cleanup:
  - Use finally to release non-AutoCloseable resources.
  - Prefer try-with-resources for AutoCloseable resources; it uses an implicit finally and records suppressed exceptions.
- Best practices:
  - Catch the most specific type you can handle.
  - Don’t swallow exceptions; at least log or rethrow.
  - Preserve causes when translating exceptions (new X(msg, cause)).
  - If you catch InterruptedException, restore the interrupt flag (Thread.currentThread().interrupt()).

Below are executable demonstrations and commented examples for the above concepts.
*/
public class _01_Theory {

    public static void main(String[] args) {
        System.out.println("== Basic try/catch/finally ==");
        basicTryCatchFinally();

        System.out.println("\n== Multiple catch ordering (specific to general) ==");
        multipleCatchOrdering();

        System.out.println("\n== Multi-catch (disjoint types) ==");
        multiCatch();

        System.out.println("\n== finally runs even with return ==");
        int ret = demoReturnInTry();
        System.out.println("Returned: " + ret);

        System.out.println("\n== return in finally overrides previous return (avoid) ==");
        int override = returnFromFinallyOverrides();
        System.out.println("Returned: " + override);

        System.out.println("\n== finally runs even when exception propagates ==");
        try {
            finallyRunsWhenPropagating();
        } catch (RuntimeException e) {
            System.out.println("Caught outside: " + e);
        }

        System.out.println("\n== Exception lost if finally throws (avoid) ==");
        try {
            exceptionLostWhenFinallyThrows();
        } catch (Exception e) {
            System.out.println("Caught: " + e);
            System.out.println("Suppressed count: " + e.getSuppressed().length);
        }

        System.out.println("\n== Preserving original by adding it as suppressed when finally throws ==");
        try {
            preserveOriginalWhenFinallyThrows();
        } catch (Exception e) {
            System.out.println("Caught: " + e);
            for (Throwable s : e.getSuppressed()) {
                System.out.println("  suppressed: " + s);
            }
        }

        System.out.println("\n== Cleanup in finally for non-AutoCloseable ==");
        cleanupInFinally();

        System.out.println("\n== Checked vs unchecked exceptions ==");
        try {
            throwsChecked();
        } catch (java.io.IOException e) {
            System.out.println("Caught checked: " + e);
        }
        try {
            throwsUnchecked();
        } catch (RuntimeException e) {
            System.out.println("Caught unchecked: " + e);
        }

        System.out.println("\n== Rethrow with precise types (multi-catch) ==");
        try {
            refinedRethrow(true);
        } catch (java.io.IOException | java.sql.SQLException ex) {
            System.out.println("Caught: " + ex.getClass().getSimpleName());
        }
        try {
            refinedRethrow(false);
        } catch (java.io.IOException | java.sql.SQLException ex) {
            System.out.println("Caught: " + ex.getClass().getSimpleName());
        }

        System.out.println("\n== try-with-resources adds suppressed exceptions ==");
        try {
            twrSuppressedDemo();
        } catch (Exception e) {
            System.out.println("Primary: " + e);
            for (Throwable s : e.getSuppressed()) {
                System.out.println("  suppressed: " + s);
            }
        }

        System.out.println("\n== Object mutation in finally after return ==");
        StringBuilder sb = objectMutationInFinally();
        System.out.println("Returned content: " + sb.toString()); // "AB"

        System.out.println("\n== break/continue still run finally ==");
        breakContinueFinallyDemo();

        System.out.println("\n== Variable scope across try/catch/finally ==");
        variableScopeDemo();

        System.out.println("\n== Interrupt handling best practice (demonstration only) ==");
        interruptHandlingDemo();

        System.out.println("\n== See code comments for additional guidance and anti-patterns ==");
    }

    // Basic structure and flow
    static void basicTryCatchFinally() {
        String input = "42x";
        int value = -1;

        try {
            // If this throws NumberFormatException, control jumps to the matching catch.
            value = Integer.parseInt(input);
            System.out.println("Parsed value: " + value);
        } catch (NumberFormatException e) {
            // Handle only what you can handle; keep catch blocks specific.
            System.out.println("Could not parse: " + e.getMessage());
        } finally {
            // Always runs, success or failure. Use for cleanup.
            System.out.println("finally: cleanup/closing resources here");
        }
    }

    // Ordering: specific catch before general catch
    static void multipleCatchOrdering() {
        try {
            // Simulate an I/O scenario with a specific IOException subtype
            throw new java.io.FileNotFoundException("demo.txt missing");
        } catch (java.io.FileNotFoundException e) { // specific first
            System.out.println("Specific catch: " + e);
        } catch (java.io.IOException e) { // then general
            System.out.println("General IO catch: " + e);
        }
        // NOTE: Reversing order (IOException before FileNotFoundException) would be a compile error (unreachable catch).
    }

    // Multi-catch with disjoint types
    static void multiCatch() {
        try {
            if (System.nanoTime() % 2 == 0) {
                throw new IllegalArgumentException("bad arg");
            } else {
                throw new IllegalStateException("bad state");
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            // e is effectively final; you can't reassign it (e = new Exception() is illegal).
            System.out.println("Handled either IllegalArgumentException or IllegalStateException: " + e.getMessage());
        }
        // NOTE: You cannot use multi-catch with types in a subclass relationship (e.g., catch (IOException | FileNotFoundException e)).
    }

    // finally runs even when returning from try
    static int demoReturnInTry() {
        try {
            System.out.println("in try: returning 1");
            return 1;
        } finally {
            // Runs before method actually returns
            System.out.println("finally: runs even though try returned");
        }
    }

    // Avoid return in finally; it overrides prior returns and hides exceptions.
    static int returnFromFinallyOverrides() {
        try {
            System.out.println("try: returning 1");
            return 1;
        } finally {
            System.out.println("finally: returning 2 (overrides prior return) [avoid]");
            return 2;
        }
        // The caller will receive 2. If an exception occurred in try, this return would hide it too.
    }

    // finally runs even if exception is not caught here (it propagates outward)
    static void finallyRunsWhenPropagating() {
        try {
            willThrowRuntime();
        } finally {
            System.out.println("finally: executed while exception is propagating");
        }
        // No catch here; the RuntimeException escapes to the caller.
    }

    static void willThrowRuntime() {
        throw new RuntimeException("boom");
    }

    // If finally throws, it replaces the exception from try/catch. The original is lost (avoid).
    static void exceptionLostWhenFinallyThrows() {
        try {
            throw new IllegalStateException("original from try");
        } finally {
            // This throw will replace the original; caller only sees this one.
            throw new IllegalArgumentException("from finally (replaces original)");
        }
    }

    // If finally must throw, you can attach the original as suppressed to retain both signals.
    static void preserveOriginalWhenFinallyThrows() throws Exception {
        Exception original = null;
        try {
            throw new Exception("original from try");
        } catch (Exception e) {
            // Capture original without rethrowing here (we rethrow from finally)
            original = e;
        } finally {
            Exception fin = new Exception("from finally (primary)");
            if (original != null) {
                fin.addSuppressed(original); // Now both are preserved
            }
            throw fin; // The finally-thrown is primary; original is suppressed
        }
    }

    // Use finally to clean up non-AutoCloseable resources
    static void cleanupInFinally() {
        NonAutoCloseableResource res = null;
        try {
            res = new NonAutoCloseableResource();
            res.open();
            res.use();
            if (System.nanoTime() % 2 == 0) {
                throw new RuntimeException("operation failed");
            }
            System.out.println("operation succeeded");
        } catch (RuntimeException e) {
            System.out.println("caught: " + e.getMessage());
        } finally {
            if (res != null) {
                try {
                    res.close();
                } catch (Exception closeEx) {
                    // Swallowing close exceptions is sometimes acceptable during cleanup, but prefer logging.
                    System.out.println("close failed: " + closeEx.getMessage());
                }
            }
        }
    }

    // Checked vs unchecked
    static void throwsChecked() throws java.io.IOException {
        throw new java.io.IOException("checked exception: must be caught or declared");
    }

    static void throwsUnchecked() {
        throw new IllegalArgumentException("unchecked exception: may be un-declared");
    }

    // Rethrow with precise types using multi-catch (Java 7+)
    static void refinedRethrow(boolean io) throws java.io.IOException, java.sql.SQLException {
        try {
            if (io) throw new java.io.IOException("IO problem");
            else throw new java.sql.SQLException("SQL problem");
        } catch (java.io.IOException | java.sql.SQLException e) {
            // Rethrowing keeps precise types; method signature reflects them.
            throw e;
        }
    }

    // try-with-resources: suppressed exceptions recorded automatically
    static void twrSuppressedDemo() throws Exception {
        // The close() exception is suppressed if an exception already occurred in the try block.
        try (FaultyAutoCloseable r = new FaultyAutoCloseable()) {
            throw new Exception("primary from try-with-resources");
        }
        // The thrown exception will be "primary from try-with-resources"
        // and the close failure will appear in getSuppressed().
    }

    // Object mutation after return in finally: the reference is computed before finally,
    // but the object itself can still be mutated in finally.
    static StringBuilder objectMutationInFinally() {
        StringBuilder sb = new StringBuilder("A");
        try {
            return sb; // The reference to sb is prepared for return
        } finally {
            sb.append("B"); // Mutates the same object after return is determined
        }
        // Caller observes "AB"
    }

    // break/continue still run finally
    static void breakContinueFinallyDemo() {
        for (int i = 0; i < 2; i++) {
            try {
                if (i == 0) {
                    System.out.println("loop i=0: breaking");
                    break;
                } else {
                    System.out.println("loop i=1: continuing");
                    continue;
                }
            } finally {
                System.out.println("finally: runs on break/continue");
            }
        }
    }

    // Variable scope demonstration
    static void variableScopeDemo() {
        int result; // must be definitely assigned before use
        try {
            result = Integer.parseInt("123");
        } catch (NumberFormatException e) {
            result = -1; // ensure assignment in both paths
        } finally {
            // Can still access/modify result here
            // finalization logic...
        }
        System.out.println("result = " + result);

        // NOTE:
        // int insideTry;
        // try { int insideOnly = 10; } finally {}
        // System.out.println(insideOnly); // compile error: not visible outside the try block
    }

    // Interrupt handling best practice: restore interrupt flag if you catch InterruptedException
    static void interruptHandlingDemo() {
        Thread t = Thread.currentThread();
        try {
            // Simulate a blocking call
            Thread.sleep(1);
        } catch (InterruptedException e) {
            // Best practice: restore interrupt status so higher layers can notice.
            t.interrupt();
        }
    }

    // Additional notes (see code usage and comments):
    // - Avoid catching Throwable or Error; they represent serious problems the app usually shouldn't handle.
    // - When translating exceptions, preserve the cause: throw new IllegalStateException("...", e).
    // - Don’t swallow exceptions silently: at least log, and prefer rethrowing or handling meaningfully.
    // - finally may not execute in cases like System.exit(0) or VM crash; avoid relying on it for critical persistence.

    // Helper types for demos

    static final class NonAutoCloseableResource {
        private boolean open;

        void open() {
            System.out.println("resource opened");
            this.open = true;
        }

        void use() {
            if (!open) throw new IllegalStateException("resource not open");
            System.out.println("resource in use");
        }

        void close() {
            if (open) {
                System.out.println("resource closed");
                open = false;
            } else {
                System.out.println("resource already closed");
            }
        }
    }

    static final class FaultyAutoCloseable implements AutoCloseable {
        FaultyAutoCloseable() {
            System.out.println("open: FaultyAutoCloseable");
        }

        @Override
        public void close() throws Exception {
            System.out.println("close: FaultyAutoCloseable (throws)");
            throw new Exception("close failure from AutoCloseable.close()");
        }
    }

    // Anti-patterns (for reference; do not use):
    // static void swallow() {
    //     try { risky(); } catch (Exception e) { /* ignored */ } // Bad: swallowed exception
    // }
    // static void badFinallyReturn() {
    //     try { return; } finally { return; } // Hides exceptions/returns; avoid.
    // }
    // static void catchingThrowable() {
    //     try { risky(); } catch (Throwable t) { /* catches everything incl. Error; avoid */ }
    // }
}