package _07_03_try_with_resources;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;

/**
 * Try-with-Resources Interview Q&A (Basic → Intermediate → Advanced)
 *
 * This single file both documents and demonstrates the most asked interview questions
 * about try-with-resources (TWR) with runnable examples and detailed comments.
 *
 * How to run:
 * - javac _03_InterviewQA.java
 * - java _07_03_try_with_resources._03_InterviewQA
 *
 * Contents:
 * 1) Basics: What is TWR? When introduced? Why use it?
 * 2) Requirements: What qualifies as a "resource"? AutoCloseable vs Closeable
 * 3) Syntax patterns: with/without catch/finally, multi-resource, Java 9 improvement
 * 4) Closing order and guarantees (including on return/throw)
 * 5) Suppressed exceptions: what, why, how to access
 * 6) Behavior when try-body throws vs when close() throws
 * 7) Pre-Java 7 manual pattern equivalent
 * 8) Failure during resource construction
 * 9) Null resources in TWR
 * 10) Catching multiple exception types (multi-catch) with TWR
 * 11) TWR and finally: ordering
 * 12) Using TWR beyond IO (any AutoCloseable)
 * 13) Adapting non-closeable resources
 * 14) Best practices, pitfalls, FAQs summarized in comments
 */
public class _03_InterviewQA {

    public static void main(String[] args) throws DemoResource.UseFailException {
        header("1) Basics: What is try-with-resources?");
        basics();

        header("2) Requirements: AutoCloseable vs Closeable");
        autoCloseableVsCloseable();

        header("3) Multi-resource and closing order (reverse)");
        closingOrderDemo();

        header("4) Suppressed exceptions when both try and close throw");
        suppressedWhenBodyAndCloseThrow();

        header("5) Suppressed exceptions when only close() throws");
        suppressedWhenOnlyCloseThrows();

        header("6) finally runs AFTER resources are closed");
        finallyRunsAfterClose();

        header("7) Pre-Java 7 equivalent pattern (manual, verbose)");
        preJava7EquivalentPattern();

        header("8) Java 9 improvement: effectively final variables in TWR");
        java9EffectivelyFinalVariables();

        header("9) Resource construction failure behavior");
        constructionFailure();

        header("10) Null resource behavior");
        nullResourceBehavior();

        header("11) Multi-catch with try-with-resources");
        multiCatchDemo();

        header("12) TWR works beyond IO (any AutoCloseable)");
        beyondIO();

        header("13) Adapting a non-closeable type via wrapper");
        adaptingNonCloseable();

        header("14) Closing happens before return/throw (guarantee)");
        closingOnReturnDemo();

        header("Done");
    }

    // -------------- Q&A 1: Basics ----------------
    /*
     Q: What is try-with-resources? When introduced?
     A:
     - TWR is a try statement that declares one or more resources; each is closed automatically.
     - Introduced in Java 7 to avoid resource leaks and reduce boilerplate.
     - A "resource" must implement java.lang.AutoCloseable.

     Q: Why use it?
     A:
     - Less error-prone than manual try/finally.
     - Correctly handles exceptions during close via suppressed exceptions.
     */
    private static void basics() {
        System.out.println("Basic usage, single resource:");
        try (DemoResource r = new DemoResource("R1", false)) {
            r.use(false);
        } catch (Exception e) {
            System.out.println("Caught: " + e);
        }
    }

    // -------------- Q&A 2: AutoCloseable vs Closeable ----------------
    /*
     Q: What qualifies as a resource in TWR?
     A: Anything that implements java.lang.AutoCloseable.

     Q: Difference between AutoCloseable and Closeable?
     A:
     - Closeable extends AutoCloseable; designed for IO and its close() throws IOException.
     - AutoCloseable.close() can throw Exception (broader).
     - Closeable's contract suggests close should be idempotent and not throw except IOException.
     - Prefer implementing AutoCloseable unless bound to IO APIs that expect Closeable.

     Q: Can I use types that don't implement AutoCloseable?
     A: Not directly. Wrap/adapt them so the wrapper implements AutoCloseable.
     */
    private static void autoCloseableVsCloseable() {
        System.out.println("Using Closeable in TWR:");
        try (CloseableResource cr = new CloseableResource("CR1", false)) {
            cr.doWork();
        } catch (IOException e) {
            System.out.println("Caught IOException: " + e.getMessage());
        }

        System.out.println("Using AutoCloseable in TWR:");
        try (DemoResource r = new DemoResource("R2", false)) {
            r.use(false);
        } catch (Exception e) {
            System.out.println("Caught: " + e);
        }
    }

    // -------------- Q&A 3: Closing order ----------------
    /*
     Q: In what order are multiple resources closed?
     A: Reverse order of declaration (LIFO).
     */
    private static void closingOrderDemo() {
        try (DemoResource r1 = new DemoResource("First", false);
             DemoResource r2 = new DemoResource("Second", false);
             DemoResource r3 = new DemoResource("Third", false)) {
            System.out.println("Inside try: using resources");
        } catch (Exception e) {
            System.out.println("Caught: " + e);
        }
        System.out.println("Observe closing order above: Third, Second, First");
    }

    // -------------- Q&A 4: Suppressed exceptions (both throw) ----------------
    /*
     Q: What are suppressed exceptions?
     A:
     - If the try-body throws an exception and closing a resource also throws,
       the close exception(s) don't replace the primary exception. They are attached as "suppressed".
     - Access them via Throwable.getSuppressed().

     Q: Which is the primary exception?
     A:
     - The exception from the try-body (or from an earlier phase) is primary.
       Close exceptions are suppressed onto it.
     */
    private static void suppressedWhenBodyAndCloseThrow() {
        try (DemoResource r1 = new DemoResource("R1", true);
             DemoResource r2 = new DemoResource("R2", true)) {
            System.out.println("Inside try: throwing from body");
            throw new DemoResource.UseFailException("Primary failure from body");
        } catch (Exception e) {
            System.out.println("Caught primary: " + e);
            System.out.println("Suppressed count: " + e.getSuppressed().length);
            Arrays.stream(e.getSuppressed()).forEach(s -> System.out.println("  suppressed -> " + s));
        }
    }

    // -------------- Q&A 5: Suppressed when only close() throws ----------------
    /*
     Q: What happens if the try-body completes normally, but close() throws?
     A:
     - The first close() exception becomes the primary thrown exception.
     - Any further close() exceptions are added as suppressed to that primary.
     */
    private static void suppressedWhenOnlyCloseThrows() {
        try (DemoResource r1 = new DemoResource("R1", true);
             DemoResource r2 = new DemoResource("R2", true)) {
            System.out.println("Inside try: completes normally");
        } catch (Exception e) {
            System.out.println("Caught primary from close(): " + e);
            System.out.println("Suppressed count: " + e.getSuppressed().length);
            Arrays.stream(e.getSuppressed()).forEach(s -> System.out.println("  suppressed -> " + s));
        }
    }

    // -------------- Q&A 6: finally ordering ----------------
    /*
     Q: Does finally run before or after resources are closed?
     A:
     - Resources are closed first, then finally executes.
     - Important if finally tries to use the resource; it's already closed.
     */
    private static void finallyRunsAfterClose() {
        try (DemoResource r = new DemoResource("R", false)) {
            System.out.println("Inside try");
        } catch (Exception e) {
            System.out.println("Caught: " + e);
        } finally {
            System.out.println("Finally after resource closed");
        }
    }

    // -------------- Q&A 7: Pre-Java 7 equivalent ----------------
    /*
     Q: How would you write equivalent code before Java 7?
     A:
     - Use try-finally with careful exception handling and manual suppression.
     - The compiler translates TWR roughly like this pattern.
     */
    private static void preJava7EquivalentPattern() throws DemoResource.UseFailException {
        DemoResource r1 = null;
        Throwable primary = null;
        try {
            r1 = new DemoResource("ManualR", true);
            r1.use(false);
        } catch (Throwable t) {
            primary = t;
            throw t;
        } finally {
            if (r1 != null) {
                try {
                    r1.close();
                } catch (Throwable closeEx) {
                    if (primary != null) {
                        primary.addSuppressed(closeEx);
                    } else {
                        throwAsUnchecked(closeEx);
                    }
                }
            }
        }
        System.out.println("Manual pattern completed (if no exception above).");
    }

    // -------------- Q&A 8: Java 9 improvement ----------------
    /*
     Q: What changed in Java 9 for TWR?
     A:
     - You can use effectively final variables declared outside the try header.
     - Before Java 9, resources had to be declared in the try-with-resources header.
     */
    private static void java9EffectivelyFinalVariables() {
        DemoResource r1 = new DemoResource("OuterR1", false);
        DemoResource r2 = new DemoResource("OuterR2", false);
        // r1 and r2 must be effectively final (not reassigned) for this to compile on Java 9+.
        try (r1; r2) {
            r1.use(false);
            r2.use(false);
        } catch (Exception e) {
            System.out.println("Caught: " + e);
        }
    }

    // -------------- Q&A 9: Construction failure ----------------
    /*
     Q: What if resource construction itself fails?
     A:
     - If a resource initializer throws, that resource is never acquired and thus not closed.
     - Already-acquired resources to its left will still be closed.
     */
    private static void constructionFailure() {
        try (DemoResource r1 = new DemoResource("Acquired", false);
             DemoResource r2 = DemoResource.failingConstructor("FailsDuringConstruction")) {
            System.out.println("This line is never reached");
        } catch (Exception e) {
            System.out.println("Caught construction failure: " + e);
        }
        // Note: r1 was acquired and then closed; r2 was never acquired.
    }

    // -------------- Q&A 10: Null resource behavior ----------------
    /*
     Q: What happens if a resource variable is null?
     A:
     - If the resource expression results in null, the close() call at the end will NPE.
     - Example: try (AutoCloseable r = null) { } -> NPE on closing.
     */
    private static void nullResourceBehavior() {
        try (DemoResource r = null) {
            System.out.println("Inside try (won't matter)");
        } catch (NullPointerException npe) {
            System.out.println("Caught NPE during close of null resource: " + npe);
        } catch (DemoResource.CloseFailException e) {
            throw new RuntimeException(e);
        }
    }

    // -------------- Q&A 11: Multi-catch ----------------
    /*
     Q: Can we use multi-catch with TWR?
     A:
     - Yes. TWR is just a try; use catch (A | B e) as usual.
     */
    private static void multiCatchDemo() {
        try (DemoResource r = new DemoResource("R", true)) {
            r.use(true); // throws UseFailException
        } catch (DemoResource.UseFailException | DemoResource.CloseFailException e) {
            System.out.println("Caught with multi-catch: " + e);
            System.out.println("Suppressed count: " + e.getSuppressed().length);
        } catch (Exception e) {
            System.out.println("Fallback catch: " + e);
        }
    }

    // -------------- Q&A 12: Beyond IO ----------------
    /*
     Q: Is TWR only for IO types?
     A:
     - No. Any AutoCloseable can be used (DB connections, locks, timers, instrumentation handles, etc.).
     */
    private static void beyondIO() {
        try (TimerResource t = new TimerResource("Timer")) {
            Thread.sleep(10);
        } catch (Exception e) {
            System.out.println("Caught: " + e);
        }
    }

    // -------------- Q&A 13: Adapting non-closeable ----------------
    /*
     Q: How to use TWR for a type that doesn't implement AutoCloseable?
     A:
     - Write an adapter that implements AutoCloseable and closes/releases the underlying resource.
     */
    private static void adaptingNonCloseable() {
        NonCloseableThing thing = new NonCloseableThing("Thing");
        try (NonCloseableAdapter adapter = new NonCloseableAdapter(thing)) {
            adapter.doWork();
        } catch (Exception e) {
            System.out.println("Caught: " + e);
        }
    }

    // -------------- Q&A 14: Close on return/throw guarantee ----------------
    /*
     Q: Are resources closed if we return or throw from inside try?
     A:
     - Yes, resources are always closed, even if the try block returns or throws.
     */
    private static void closingOnReturnDemo() {
        System.out.println("Before method that returns inside try:");
        String result = methodThatReturnsInsideTry();
        System.out.println("After call, result: " + result);
    }

    private static String methodThatReturnsInsideTry() {
        try (DemoResource r = new DemoResource("ReturnR", false)) {
            System.out.println("Inside try, about to return");
            return "returned";
        } catch (Exception e) {
            return "caught: " + e;
        }
    }

    // --------------------------------- Helpers ---------------------------------

    private static void header(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }

    // Utility to rethrow as unchecked when demonstrating manual pattern
    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwAsUnchecked(Throwable t) throws T {
        throw (T) t;
    }

    // --------------------------------- Demo Resources ---------------------------------

    /**
     * Demo resource for illustrating AutoCloseable behavior, exceptions, and ordering.
     */
    static class DemoResource implements AutoCloseable {
        private final String name;
        private final boolean throwOnClose;
        private boolean closed;

        DemoResource(String name, boolean throwOnClose) {
            this.name = name;
            this.throwOnClose = throwOnClose;
            System.out.println("Acquired " + name);
        }

        static DemoResource failingConstructor(String name) throws Exception {
            System.out.println("Attempting to acquire (will fail) " + name);
            throw new Exception("Constructor failed for " + name);
        }

        void use(boolean throwOnUse) throws UseFailException {
            if (closed) {
                throw new IllegalStateException("Already closed: " + name);
            }
            System.out.println("Using " + name);
            if (throwOnUse) {
                throw new UseFailException("Use failed in " + name);
            }
        }

        @Override
        public void close() throws CloseFailException {
            if (!closed) {
                System.out.println("Closing " + name);
                closed = true;
                if (throwOnClose) {
                    throw new CloseFailException("Close failed in " + name);
                }
            }
        }

        static class UseFailException extends Exception {
            UseFailException(String msg) { super(msg); }
        }

        static class CloseFailException extends Exception {
            CloseFailException(String msg) { super(msg); }
        }
    }

    /**
     * Example Closeable resource (IO-like), close() throws IOException.
     */
    static class CloseableResource implements Closeable {
        private final String name;
        private final boolean throwOnClose;
        private boolean closed;

        CloseableResource(String name, boolean throwOnClose) {
            this.name = name;
            this.throwOnClose = throwOnClose;
            System.out.println("Acquired (Closeable) " + name);
        }

        void doWork() {
            if (closed) throw new IllegalStateException("Already closed: " + name);
            System.out.println("Working with " + name);
        }

        @Override
        public void close() throws IOException {
            if (!closed) {
                System.out.println("Closing (Closeable) " + name);
                closed = true;
                if (throwOnClose) {
                    throw new IOException("Close failed in " + name);
                }
            }
        }
    }

    /**
     * Non-closeable third-party-like class.
     */
    static class NonCloseableThing {
        private final String id;
        NonCloseableThing(String id) { this.id = id; }
        void open() { System.out.println("NonCloseableThing open: " + id); }
        void release() { System.out.println("NonCloseableThing release: " + id); }
        void work() { System.out.println("NonCloseableThing work: " + id); }
    }

    /**
     * Adapter to use NonCloseableThing in TWR.
     */
    static class NonCloseableAdapter implements AutoCloseable {
        private final NonCloseableThing thing;
        NonCloseableAdapter(NonCloseableThing thing) {
            this.thing = thing;
            thing.open();
        }
        void doWork() { thing.work(); }
        @Override public void close() {
            thing.release();
        }
    }

    /**
     * A simple AutoCloseable representing something like a timer/measurement scope.
     */
    static class TimerResource implements AutoCloseable {
        private final String name;
        private final long start = System.nanoTime();
        TimerResource(String name) {
            this.name = name;
            System.out.println("Timer " + name + " started");
        }
        @Override public void close() {
            long dur = System.nanoTime() - start;
            System.out.println("Timer " + name + " stopped: " + dur + " ns");
        }
    }
}