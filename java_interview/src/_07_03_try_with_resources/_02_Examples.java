package _07_03_try_with_resources;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

/**
 * Try-with-resources: comprehensive examples and explanations inline.
 * Each example is small and self-contained; run main() to see output.
 */
public class _02_Examples {

    public static void main(String[] args) {
        System.out.println("=== Example 1: Basic file read with try-with-resources ===");
        example1BasicFileRead();

        System.out.println("\n=== Example 2: Multiple resources (copy a file) ===");
        example2MultipleResourcesCopy();

        System.out.println("\n=== Example 3: Closing order is LIFO ===");
        example3ClosingOrder();

        System.out.println("\n=== Example 4: Suppressed exceptions on close ===");
        example4SuppressedExceptions();

        System.out.println("\n=== Example 5: Traditional try-finally vs try-with-resources ===");
        example5TraditionalVsTWR();

        System.out.println("\n=== Example 6: Java 9+ using an effectively-final external variable ===");
        example6ExternalVariableJava9();

        System.out.println("\n=== Example 7: Streams are AutoCloseable (Files.lines) ===");
        example7StreamResource();

        System.out.println("\n=== Example 8: Scanner in try-with-resources ===");
        example8Scanner();

        System.out.println("\n=== Example 9: Adapting a Lock to AutoCloseable ===");
        example9LockAdapter();

        System.out.println("\n=== Example 10: Constructor failure – resource never enters scope ===");
        example10ConstructorFailure();

        System.out.println("\n=== Example 11: try-with-resources with catch/finally ===");
        example11WithCatchAndFinally();

        System.out.println("\n=== Example 12: Adapting non-closeable resources ===");
        example12AdapterForNonCloseable();

        System.out.println("\n=== Example 13: Closeable vs AutoCloseable ===");
        example13CloseableVsAutoCloseable();

        System.out.println("\n=== Example 14: JDBC with try-with-resources (not executed) ===");
        System.out.println("See method example14Jdbc(); not invoked to avoid requiring a driver.");
    }

    // Example 1: Basic file read with try-with-resources
    private static void example1BasicFileRead() {
        try {
            Path file = Files.createTempFile("twr-basic-", ".txt");
            Files.write(file, java.util.List.of("alpha", "beta", "gamma"), StandardCharsets.UTF_8);

            // BufferedReader implements Closeable -> will be closed automatically
            try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("Read: " + line);
                }
            }

            Files.deleteIfExists(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Example 2: Multiple resources in one try-with-resources (copy file)
    private static void example2MultipleResourcesCopy() {
        try {
            Path src = Files.createTempFile("twr-src-", ".bin");
            Path dst = Files.createTempFile("twr-dst-", ".bin");
            Files.write(src, "Hello, try-with-resources!".getBytes(StandardCharsets.UTF_8));

            // Multiple resources separated by semicolons; closed in reverse order
            try (InputStream in = Files.newInputStream(src);
                 OutputStream out = Files.newOutputStream(dst)) {
                in.transferTo(out);
            }

            long size = Files.size(dst);
            System.out.println("Copied " + size + " bytes");

            Files.deleteIfExists(src);
            Files.deleteIfExists(dst);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Example 3: Closing order is reverse of declaration (LIFO)
    private static void example3ClosingOrder() {
        try (DemoResource a = new DemoResource("A");
             DemoResource b = new DemoResource("B");
             DemoResource c = new DemoResource("C")) {
            System.out.println("Inside try: using resources A, B, C");
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Expected close order: C, then B, then A
    }

    // Example 4: Suppressed exceptions are attached when both body and close throw
    private static void example4SuppressedExceptions() {
        try {
            try (DemoResource r = new DemoResource("R", true /* throwOnClose */)) {
                System.out.println("Work that throws primary exception...");
                throw new RuntimeException("Primary failure");
            }
        } catch (RuntimeException e) {
            System.out.println("Caught primary: " + e);
            for (Throwable sup : e.getSuppressed()) {
                System.out.println("  Suppressed: " + sup);
            }
        } catch (Exception e) {
            // This would be reached only if close() is the only exception.
            System.out.println("Caught close-only exception: " + e);
        }

        // Also show the case where only close throws:
        try {
            try (DemoResource r = new DemoResource("R2", true)) {
                System.out.println("No primary exception thrown.");
            }
        } catch (Exception e) {
            System.out.println("Caught close-only exception: " + e);
            if (e.getSuppressed().length > 0) {
                System.out.println("  (unexpected suppressed present)");
            }
        }
    }

    // Example 5: Traditional try-finally vs try-with-resources
    private static void example5TraditionalVsTWR() {
        // Traditional try-finally: easy to accidentally mask or drop exceptions
        try {
            DemoResource r = null;
            try {
                r = new DemoResource("Traditional", true /* throws on close */);
                throw new RuntimeException("Primary failure (traditional)");
            } finally {
                if (r != null) {
                    try {
                        r.close(); // throws; here we log it and continue
                    } catch (Exception closeEx) {
                        System.out.println("Traditional finally caught close exception: " + closeEx);
                    }
                }
            }
        } catch (RuntimeException e) {
            // Note: no suppressed exceptions attached — you had to manage it manually
            System.out.println("Traditional caught primary: " + e);
            System.out.println("Traditional suppressed count: " + e.getSuppressed().length);
        }

        // Try-with-resources automatically attaches close exceptions as suppressed
        try {
            try (DemoResource r = new DemoResource("TWR", true)) {
                throw new RuntimeException("Primary failure (TWR)");
            }
        } catch (RuntimeException e) {
            System.out.println("TWR caught primary: " + e);
            System.out.println("TWR suppressed count: " + e.getSuppressed().length);
        } catch (Exception e) {
            System.out.println("TWR only close exception: " + e);
        }
    }

    // Example 6: Java 9+ using an effectively final external variable
    private static void example6ExternalVariableJava9() {
        DemoResource external = new DemoResource("ExternalVar");
        try (external) { // Java 9+: use previously declared resource if it's effectively final
            external.work();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("ExternalVar closed? " + external.closed);
    }

    // Example 7: Files.lines returns a Stream which is AutoCloseable
    private static void example7StreamResource() {
        try {
            Path file = Files.createTempFile("twr-lines-", ".txt");
            try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                w.write("one\n");
                w.write("two\n");
                w.write("three\n");
            }

            try (Stream<String> lines = Files.lines(file, StandardCharsets.UTF_8)) {
                long count = lines.count();
                System.out.println("Line count: " + count);
            }

            Files.deleteIfExists(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Example 8: Scanner as a resource
    private static void example8Scanner() {
        byte[] data = "1 2 3 4 5".getBytes(StandardCharsets.UTF_8);
        try (Scanner sc = new Scanner(new ByteArrayInputStream(data), StandardCharsets.UTF_8)) {
            int sum = 0;
            while (sc.hasNextInt()) {
                sum += sc.nextInt();
            }
            System.out.println("Sum: " + sum);
        }
    }

    // Example 9: Adapting a Lock to AutoCloseable for concise unlocking
    private static void example9LockAdapter() {
        ReentrantLock lock = new ReentrantLock();
        try (LockResource ignored = LockResource.lock(lock)) {
            System.out.println("In critical section (lock held)");
        }
        System.out.println("Lock released automatically");
    }

    // Example 10: If construction fails, resource never enters the try scope; close not called
    private static void example10ConstructorFailure() {
        try {
            try (FailingCtorResource r = new FailingCtorResource()) {
                System.out.println("Unreachable");
            }
        } catch (RuntimeException e) {
            System.out.println("Caught construction failure: " + e.getMessage());
        }
    }

    // Example 11: try-with-resources with catch and finally blocks
    private static void example11WithCatchAndFinally() {
        try (DemoResource r = new DemoResource("CatchFinally")) {
            throw new IOException("Simulated I/O problem");
        } catch (IOException e) {
            System.out.println("Caught in catch: " + e.getMessage());
        } catch (Exception e) {
            // From close()
            System.out.println("Caught other exception: " + e.getMessage());
        } finally {
            System.out.println("Finally executes after all resources closed");
        }
    }

    // Example 12: Adapting a non-closeable class to use in try-with-resources
    private static void example12AdapterForNonCloseable() {
        NonCloseable nc = new NonCloseable();
        nc.open();
        try (NonCloseableAdapter adapter = new NonCloseableAdapter(nc)) {
            System.out.println("Using NonCloseable via adapter");
        }
        System.out.println("NonCloseable disposed: " + nc.disposed);
    }

    // Example 13: Closeable vs AutoCloseable
    private static void example13CloseableVsAutoCloseable() {
        // Closeable close() throws IOException (more specific)
        try (CloseableResource c = new CloseableResource("Closeable")) {
            c.work();
        } catch (IOException e) {
            System.out.println("IOException from Closeable: " + e.getMessage());
        }

        // AutoCloseable close() throws Exception (broader)
        try (DemoResource a = new DemoResource("AutoCloseable")) {
            a.work();
        } catch (Exception e) {
            System.out.println("Exception from AutoCloseable: " + e.getMessage());
        }
    }

    // Example 14: JDBC with try-with-resources (compiles; not executed here)
    @SuppressWarnings("unused")
    private static void example14Jdbc() throws SQLException {
        // Requires a JDBC driver on classpath; example shown for illustration:
        try (Connection con = DriverManager.getConnection("jdbc:yourdriver:yourdb");
             PreparedStatement ps = con.prepareStatement("select 1");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int v = rs.getInt(1);
                System.out.println("JDBC value: " + v);
            }
        }
    }

    // ======== Helper classes for demonstrations ========

    // Demo resource to show open/close behavior (AutoCloseable)
    static class DemoResource implements AutoCloseable {
        final String name;
        final boolean throwOnClose;
        boolean closed;

        DemoResource(String name) {
            this(name, false);
        }

        DemoResource(String name, boolean throwOnClose) {
            this.name = name;
            this.throwOnClose = throwOnClose;
            this.closed = false;
            System.out.println("Opened " + name);
        }

        void work() {
            if (closed) throw new IllegalStateException("Resource " + name + " is closed");
            System.out.println("Working with " + name);
        }

        @Override
        public void close() throws Exception {
            if (!closed) {
                System.out.println("Closing " + name);
                closed = true;
                if (throwOnClose) {
                    throw new Exception("close() failed for " + name);
                }
            }
        }
    }

    // Closeable variant (close throws IOException)
    static class CloseableResource implements Closeable {
        final String name;

        CloseableResource(String name) {
            this.name = name;
            System.out.println("Opened (Closeable) " + name);
        }

        void work() {
            System.out.println("Working (Closeable) " + name);
        }

        @Override
        public void close() throws IOException {
            System.out.println("Closing (Closeable) " + name);
            // No error
        }
    }

    // Resource that fails in constructor
    static class FailingCtorResource implements AutoCloseable {
        FailingCtorResource() {
            System.out.println("Constructing FailingCtorResource...");
            throw new RuntimeException("Constructor failed");
        }

        @Override
        public void close() {
            System.out.println("This should never print (constructor failed)");
        }
    }

    // Adapter for Lock -> AutoCloseable; ensures unlock in close()
    static class LockResource implements AutoCloseable {
        private final Lock lock;

        private LockResource(Lock lock) {
            this.lock = lock;
        }

        static LockResource lock(Lock lock) {
            lock.lock();
            return new LockResource(lock);
        }

        @Override
        public void close() {
            lock.unlock();
        }
    }

    // Example non-closeable class
    static class NonCloseable {
        boolean opened;
        boolean disposed;

        void open() {
            opened = true;
            disposed = false;
            System.out.println("NonCloseable opened");
        }

        void dispose() {
            disposed = true;
            System.out.println("NonCloseable disposed");
        }
    }

    // Adapter to make NonCloseable usable in try-with-resources
    static class NonCloseableAdapter implements AutoCloseable {
        private final NonCloseable target;

        NonCloseableAdapter(NonCloseable target) {
            this.target = target;
        }

        @Override
        public void close() {
            target.dispose();
        }
    }

    // Utility to write lines (used by examples)
    @SuppressWarnings("unused")
    private static void writeLines(Path path, Iterable<String> lines) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(path, StandardCharsets.UTF_8);
             PrintWriter pw = new PrintWriter(w)) {
            for (String s : lines) {
                pw.println(s);
            }
        }
    }
}