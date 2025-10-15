package _07_03_try_with_resources;

/*
Try-with-Resources (TWR) theory, in one file:
- Purpose: Automatically closes resources that implement AutoCloseable (or Closeable) when the try block exits, regardless of normal completion, return, or exception.
- Resource types: Any type implementing AutoCloseable (java.io.Closeable extends AutoCloseable).
  - AutoCloseable.close() can throw Exception (any checked/unchecked).
  - Closeable.close() narrows to IOException.
- Declaration:
  - Java 7/8: resources must be declared in the try header.
  - Java 9+: you can put effectively-final variables in the try header (try (existingVar) { ... }).
- Order:
  - Resources are created left-to-right.
  - They are closed in reverse order (right-to-left).
- Exceptions:
  - If the try body throws, exceptions thrown during close() are suppressed onto the primary exception (Throwable.getSuppressed()).
  - If the try body does not throw, exceptions from close() are thrown out.
  - With multiple close failures, the first failure during closing is primary; others are suppressed.
  - If a later resource creation fails, previously created resources are closed.
  - If a resource is null, a NullPointerException occurs during closing.
  - If the primary exception has suppression disabled, close() exceptions are not recorded as suppressed.
- Catch/finally:
  - Resources are closed before control enters catch/finally.
- Translation (roughly) for one resource:
    R r = expr;
    Throwable primary = null;
    try {
        body
    } catch (Throwable t) {
        primary = t;
        throw t;
    } finally {
        if (r != null) {
            if (primary != null) {
                try { r.close(); } catch (Throwable closeEx) { primary.addSuppressed(closeEx); }
            } else {
                r.close();
            }
        }
    }
- Not just IO: Any AutoCloseable can be used (e.g., a lock guard).
*/

public class _01_Theory {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Try-with-Resources theory and examples ===");

        demoBasic();
        demoMultipleResourcesAndOrder();
        demoSuppressedWhenBodyThrows();
        demoOnlyCloseThrows();
        demoCloseExceptionsAggregation();
        demoInitFailureClosesPreviouslyCreated();
        demoNullResource();
        demoJava9EffectivelyFinal();           // Requires Java 9+
        demoTryWithResourcesCatchFinallyOrdering();
        demoReturnInTry();
        demoUsingCloseable();
        demoUsingFilesAndBufferedReader();
        demoUsingStreamAutoClosable();
        demoLockWrapper();
        demoSuppressionDisabled();

        System.out.println("=== Done ===");
    }

    // 1) Basic usage: resource closed automatically.
    private static void demoBasic() throws Exception {
        System.out.println("\n-- Basic try-with-resources --");
        try (DemoResource r = new DemoResource("R1")) {
            r.doWork();
        }
        System.out.println("After try block");
    }

    // 2) Multiple resources: created left->right, closed right->left.
    private static void demoMultipleResourcesAndOrder() throws Exception {
        System.out.println("\n-- Multiple resources: creation vs closing order --");
        try (DemoResource r1 = new DemoResource("R1");
             DemoResource r2 = new DemoResource("R2")) {
            System.out.println("Using both resources");
        }
    }

    // 3) Body throws; close() throws: close exception is suppressed on the primary body exception.
    private static void demoSuppressedWhenBodyThrows() {
        System.out.println("\n-- Suppressed exceptions when body throws --");
        try (DemoResource r = new DemoResource("R", true, false)) {
            System.out.println("Throwing from body...");
            throw new IllegalStateException("body fails");
        } catch (Exception e) {
            System.out.println("Caught: " + e);
            printSuppressed(e);
        }
    }

    // 4) Body doesn't throw; close() throws: close exception is thrown out.
    private static void demoOnlyCloseThrows() {
        System.out.println("\n-- Only close() throws --");
        try {
            try (DemoResource r = new DemoResource("R", true, false)) {
                System.out.println("Body completed normally");
            }
        } catch (Exception e) {
            System.out.println("Exception from close only: " + e);
            printSuppressed(e); // Expect 0
        }
    }

    // 5) Multiple close failures: first close failure is primary; others suppressed.
    private static void demoCloseExceptionsAggregation() {
        System.out.println("\n-- Multiple close() failures aggregation --");
        try {
            try (DemoResource r1 = new DemoResource("R1", true, false);
                 DemoResource r2 = new DemoResource("R2", true, false)) {
                System.out.println("Body ok; both close() will fail");
            }
        } catch (Exception e) {
            System.out.println("Primary close exception: " + e);
            printSuppressed(e); // Should include R1's close failure
        }
    }

    // 6) If creating a later resource fails, earlier ones are closed.
    private static void demoInitFailureClosesPreviouslyCreated() {
        System.out.println("\n-- Later resource creation fails: earlier closed --");
        try {
            try (FailableResource r1 = new FailableResource("R1", false);
                 FailableResource r2 = new FailableResource("R2", true)) {
                System.out.println("Unreached");
            }
        } catch (RuntimeException e) {
            System.out.println("Caught constructor failure: " + e.getMessage());
        }
    }

    // 7) Null resource => NPE on close.
    private static void demoNullResource() {
        System.out.println("\n-- Null resource => NullPointerException on close --");
        try {
            try (DemoResource r = maybeNull(true)) {
                System.out.println("Body ran; resource was null");
            }
        } catch (Exception npe) {
            System.out.println("Caught NPE while closing null resource");
        }
    }

    // 8) Java 9+: effectively-final variable used as a resource.
    private static void demoJava9EffectivelyFinal() throws Exception {
        System.out.println("\n-- Java 9+ effectively-final resource --");
        DemoResource external = new DemoResource("External");
        // In Java 9+, you can use an existing effectively-final variable here.
        try (external) {
            external.doWork();
        }
        System.out.println("External closed? " + external.isClosed());
    }

    // 9) Resources are closed before catch/finally execute.
    private static void demoTryWithResourcesCatchFinallyOrdering() {
        System.out.println("\n-- Catch/finally ordering (close happens before) --");
        try {
            try (DemoResource r = new DemoResource("R")) {
                System.out.println("Inside try, throwing IOException");
                throw new java.io.IOException("io problem");
            } catch (java.io.IOException e) {
                System.out.println("In catch: resource already closed");
                printSuppressed(e);
            } finally {
                System.out.println("In finally (after close)");
            }
        } catch (Exception ignored) {
        }
    }

    // 10) Return in try: resource still closes; finally also runs after close.
    private static void demoReturnInTry() throws Exception {
        System.out.println("\n-- Return inside try --");
        int v = methodReturningWithResource();
        System.out.println("Returned value: " + v);
    }

    private static int methodReturningWithResource() throws Exception {
        try (DemoResource r = new DemoResource("R")) {
            System.out.println("Returning 7 from try body");
            return 7;
        } finally {
            System.out.println("Finally after resource closed");
        }
    }

    // 11) Works with java.io.Closeable directly.
    private static void demoUsingCloseable() throws Exception {
        System.out.println("\n-- Using java.io.Closeable --");
        try (CloseableResource cr = new CloseableResource("CR")) {
            cr.write("hello");
        }
    }

    // 12) Typical file IO example.
    private static void demoUsingFilesAndBufferedReader() throws Exception {
        System.out.println("\n-- Files.newBufferedReader example --");
        java.nio.file.Path tmp = java.nio.file.Files.createTempFile("_twr_", ".txt");
        java.nio.file.Files.write(
                tmp,
                java.util.Arrays.asList("alpha", "beta", "gamma"),
                java.nio.charset.StandardCharsets.UTF_8
        );
        try (java.io.BufferedReader br =
                     java.nio.file.Files.newBufferedReader(tmp, java.nio.charset.StandardCharsets.UTF_8)) {
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println("read: " + line);
            }
        }
        java.nio.file.Files.deleteIfExists(tmp);
    }

    // 13) Stream is AutoCloseable (e.g., Files.list). TWR closes OS handles.
    private static void demoUsingStreamAutoClosable() throws Exception {
        System.out.println("\n-- Stream is AutoCloseable --");
        java.nio.file.Path dir = java.nio.file.Files.createTempDirectory("_twr_dir_");
        java.nio.file.Files.createFile(dir.resolve("a.txt"));
        java.nio.file.Files.createFile(dir.resolve("b.txt"));
        java.nio.file.Files.createFile(dir.resolve("c.txt"));
        try (java.util.stream.Stream<java.nio.file.Path> s = java.nio.file.Files.list(dir)) {
            System.out.println("count: " + s.count());
        }
        // Cleanup files
        try (java.util.stream.Stream<java.nio.file.Path> s = java.nio.file.Files.list(dir)) {
            s.forEach(p -> {
                try {
                    java.nio.file.Files.deleteIfExists(p);
                } catch (java.io.IOException e) {
                    e.printStackTrace();
                }
            });
        }
        java.nio.file.Files.deleteIfExists(dir);
    }

    // 14) Non-IO resource: lock guard pattern via AutoCloseable.
    private static void demoLockWrapper() {
        System.out.println("\n-- Non-IO resource via AutoCloseable (lock guard) --");
        java.util.concurrent.locks.ReentrantLock lock = new java.util.concurrent.locks.ReentrantLock();
        try (LockGuard ignored = LockGuard.acquire(lock)) {
            System.out.println("inside critical section");
        }
    }

    // 15) Primary exception with suppression disabled: close() exceptions not recorded as suppressed.
    private static void demoSuppressionDisabled() {
        System.out.println("\n-- Suppression disabled on primary exception --");
        try {
            try (DemoResource r = new DemoResource("R", true, false)) {
                throw new NoSuppressionException("primary (suppression disabled)");
            }
        } catch (Exception e) {
            System.out.println("Caught: " + e);
            printSuppressed(e); // Expect 0
        }
    }

    // define once
    public static final class NoSuppressionException extends Exception {
        public NoSuppressionException(String message) {
            super(message, null, false, true); // enableSuppression=false, writableStackTrace=true
        }
    }

    // Utility: print suppressed exceptions
    private static void printSuppressed(Throwable e) {
        Throwable[] sup = e.getSuppressed();
        System.out.println("Suppressed count: " + sup.length);
        for (Throwable t : sup) {
            System.out.println("  suppressed: " + t);
        }
    }

    // Returns either a new resource or null (to show NPE on close).
    private static DemoResource maybeNull(boolean returnNull) {
        return returnNull ? null : new DemoResource("NonNull");
    }

    // Demo AutoCloseable resource
    static final class DemoResource implements AutoCloseable {
        private final String name;
        private final boolean throwCheckedOnClose;
        private final boolean throwRuntimeOnClose;
        private boolean closed = false;

        DemoResource(String name) {
            this(name, false, false);
        }

        DemoResource(String name, boolean throwCheckedOnClose, boolean throwRuntimeOnClose) {
            this.name = name;
            this.throwCheckedOnClose = throwCheckedOnClose;
            this.throwRuntimeOnClose = throwRuntimeOnClose;
            System.out.println("open " + name);
        }

        void doWork() {
            if (closed) throw new IllegalStateException(name + " already closed");
            System.out.println(name + " working");
        }

        @Override
        public void close() throws Exception {
            System.out.println("closing " + name);
            closed = true; // even if throwing, it attempted to close
            if (throwCheckedOnClose) throw new Exception("checked from close: " + name);
            if (throwRuntimeOnClose) throw new RuntimeException("runtime from close: " + name);
            System.out.println("closed " + name);
        }

        boolean isClosed() { return closed; }

        @Override
        public String toString() { return "DemoResource(" + name + ")"; }
    }

    // Demo Closeable resource (throws IOException)
    static final class CloseableResource implements java.io.Closeable {
        private final String name;
        CloseableResource(String name) {
            this.name = name;
            System.out.println("open " + name);
        }
        void write(String s) throws java.io.IOException {
            System.out.println(name + " write: " + s);
        }
        @Override
        public void close() throws java.io.IOException {
            System.out.println("close (Closeable) " + name);
        }
    }

    // Resource that can fail in constructor (to show partial init cleanup)
    static final class FailableResource implements AutoCloseable {
        private final String name;
        FailableResource(String name, boolean throwOnConstruction) {
            this.name = name;
            if (throwOnConstruction) {
                System.out.println("constructing " + name + " -> throwing");
                throw new RuntimeException("constructor failed for " + name);
            }
            System.out.println("constructed " + name);
        }
        @Override
        public void close() {
            System.out.println("closing " + name);
        }
    }

    // Lock guard pattern using AutoCloseable
    static final class LockGuard implements AutoCloseable {
        private final java.util.concurrent.locks.Lock lock;
        private LockGuard(java.util.concurrent.locks.Lock lock) {
            this.lock = lock;
            this.lock.lock();
            System.out.println("lock acquired");
        }
        static LockGuard acquire(java.util.concurrent.locks.Lock lock) {
            return new LockGuard(lock);
        }
        @Override
        public void close() {
            lock.unlock();
            System.out.println("lock released");
        }
    }

    /*
    Notes:
    - Resource variables declared in the try header are implicitly final (cannot be reassigned).
    - In Java 9+, using an existing effectively-final variable in the header (try (var) { ... }) is allowed.
    - Avoid ignoring suppressed exceptions; they often carry important diagnostics from close().
    */
}