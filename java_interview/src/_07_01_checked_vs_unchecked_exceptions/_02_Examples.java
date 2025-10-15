package _07_01_checked_vs_unchecked_exceptions;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/*
Checked vs Unchecked exceptions

- Checked exceptions:
  - Subclasses of Exception, excluding RuntimeException and its subclasses.
  - Must be handled (try/catch) or declared with 'throws'.
  - Examples: IOException, SQLException, ParseException, ClassNotFoundException.

- Unchecked exceptions:
  - Subclasses of RuntimeException.
  - Not required to be caught or declared.
  - Examples: NullPointerException, IllegalArgumentException, ArithmeticException.

This file demonstrates:
1) Handling vs declaring checked exceptions.
2) Unchecked exceptions (optional handling).
3) Custom checked and unchecked exceptions.
4) try-with-resources with checked exceptions.
5) Multi-catch mixing checked and unchecked.
6) Wrapping (rethrowing) checked as unchecked.
7) Overriding rules for checked exceptions.
8) finally for cleanup.
9) Declaring unchecked exceptions (optional).
*/
public class _02_Examples {

    // Custom checked exception (must handle or declare)
    public static class InvalidConfigException extends Exception {
        public InvalidConfigException(String message) { super(message); }
        public InvalidConfigException(String message, Throwable cause) { super(message, cause); }
    }

    // Custom unchecked exception (optional to handle/declare)
    public static class CalculationException extends RuntimeException {
        public CalculationException(String message) { super(message); }
        public CalculationException(String message, Throwable cause) { super(message, cause); }
    }

    // Demo resource to show try-with-resources and checked exceptions on close/read
    static class DemoResource implements AutoCloseable {
        private final String name;
        DemoResource(String name) {
            this.name = name;
            System.out.println("Opening resource " + name);
        }
        public String read() throws IOException { // checked
            if ("bad".equals(name)) {
                throw new IOException("Failed reading from " + name);
            }
            return "data:" + name;
        }
        @Override
        public void close() throws IOException { // checked
            System.out.println("Closing resource " + name);
            if ("bad-close".equals(name)) {
                throw new IOException("Failed closing " + name);
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("== Checked: handle with try/catch ==");
        handleCheckedWithTryCatch();

        System.out.println("\n== Checked: declare with throws and handle at call site ==");
        try {
            String line = readFirstLineFromFile("missing.txt"); // likely throws
            System.out.println("First line: " + line);
        } catch (IOException e) {
            System.out.println("Caught in main: " + e);
        }

        System.out.println("\n== Unchecked: not required to catch ==");
        demonstrateUncheckedNotRequired();

        System.out.println("\n== Custom checked exception ==");
        try {
            loadConfig(null); // triggers InvalidConfigException
        } catch (InvalidConfigException e) {
            System.out.println("Handled InvalidConfigException: " + e.getMessage());
        }

        System.out.println("\n== Custom unchecked exception ==");
        try {
            System.out.println("Sqrt: " + computeSqrt(-1)); // triggers CalculationException
        } catch (CalculationException e) {
            System.out.println("Handled CalculationException: " + e.getMessage());
        }

        System.out.println("\n== try-with-resources (checked close/read) ==");
        useTryWithResources();

        System.out.println("\n== Multi-catch (checked + unchecked) ==");
        multiCatchExample("missing.txt");

        System.out.println("\n== Wrap checked as unchecked (rethrow) ==");
        try {
            readOrWrap("missing.txt");
        } catch (RuntimeException e) {
            System.out.println("Wrapped as RuntimeException: " + e.getMessage());
        }

        System.out.println("\n== Overriding rules with checked exceptions ==");
        demonstrateOverridingRules();

        System.out.println("\n== finally for cleanup even on unchecked ==");
        finallyDemo();

        System.out.println("\n== Declaring unchecked exceptions is optional ==");
        try {
            mayDeclareUnchecked(-5);
        } catch (RuntimeException e) {
            System.out.println("Caught optional unchecked: " + e);
        }
    }

    // Checked example: method declares IOException (caller must handle or declare)
    static String readFirstLineFromFile(String path) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            return br.readLine();
        }
    }

    // Handle a checked exception locally with try/catch
    static void handleCheckedWithTryCatch() {
        try {
            String line = readFirstLineFromFile("missing.txt");
            System.out.println("Read: " + line);
        } catch (IOException e) {
            System.out.println("Handled IOException locally: " + e.getClass().getSimpleName() + " -> " + e.getMessage());
        }
    }

    // Unchecked examples: no requirement to catch or declare
    static void demonstrateUncheckedNotRequired() {
        try {
            String s = null;
            System.out.println(s.length()); // NullPointerException (unchecked)
        } catch (NullPointerException e) {
            System.out.println("Caught unchecked: " + e);
        }

        // Input validation typically throws unchecked IllegalArgumentException
        try {
            setAge(-1);
        } catch (IllegalArgumentException e) {
            System.out.println("Validation failure (unchecked): " + e.getMessage());
        }
    }

    static void setAge(int age) {
        if (age < 0) throw new IllegalArgumentException("age must be >= 0");
        System.out.println("Age set to " + age);
    }

    // Custom checked: caller must handle or declare
    static void loadConfig(String path) throws InvalidConfigException {
        if (path == null || path.isBlank()) {
            throw new InvalidConfigException("Config path is required");
        }
        if ("bad.conf".equals(path)) {
            IOException cause = new IOException("Disk read error");
            throw new InvalidConfigException("Could not load config: " + path, cause);
        }
        System.out.println("Config loaded from " + path);
    }

    // Custom unchecked: caller may handle or ignore
    static double computeSqrt(int n) {
        if (n < 0) throw new CalculationException("Cannot sqrt negative: " + n);
        return Math.sqrt(n);
    }

    // try-with-resources automatically calls close(), which may throw checked exceptions
    static void useTryWithResources() {
        try (DemoResource r = new DemoResource("ok")) {
            System.out.println("Read: " + r.read());
        } catch (IOException e) {
            System.out.println("Caught: " + e.getMessage());
        }

        try (DemoResource r = new DemoResource("bad")) { // read throws IOException
            System.out.println("Read: " + r.read());
        } catch (IOException e) {
            System.out.println("Caught during read: " + e.getMessage());
        }

        try (DemoResource r = new DemoResource("bad-close")) { // close throws IOException
            System.out.println("Using resource that fails on close");
        } catch (IOException e) {
            System.out.println("Caught during close: " + e.getMessage());
        }
    }

    // Multi-catch showing handling of both checked (IOException) and unchecked (NumberFormatException)
    static void multiCatchExample(String path) {
        try {
            String line = readFirstLineFromFile(path); // checked
            int value = Integer.parseInt(line.trim()); // unchecked
            System.out.println("Parsed value: " + value);
        } catch (IOException | NumberFormatException e) {
            System.out.println("Multi-catch handled: " + e.getClass().getSimpleName() + " -> " + e.getMessage());
        }
    }

    // Wrap checked exception into an unchecked to avoid propagating 'throws' (use judiciously)
    static String readOrWrap(String path) {
        try {
            return readFirstLineFromFile(path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read " + path, e);
        }
    }

    // Overriding rule: overriding method cannot broaden checked exceptions
    static class BaseRepo {
        public void save(String path) throws IOException {
            if (path == null) throw new IOException("Path is null");
            // Pretend to save
        }
    }
    static class FileRepo extends BaseRepo {
        // Allowed: narrower checked exception (FileNotFoundException is a subclass of IOException)
        @Override
        public void save(String path) throws FileNotFoundException {
            throw new FileNotFoundException("Cannot save to " + path);
        }

        // Not allowed (would not compile): throwing unrelated checked exception
        // @Override
        // public void save(String path) throws java.sql.SQLException { }
    }

    static void demonstrateOverridingRules() {
        BaseRepo repo = new FileRepo();
        try {
            repo.save("data.txt");
        } catch (IOException e) { // Handler matches the base declaration
            System.out.println("Caught from overridden method: " + e.getClass().getSimpleName());
        }
    }

    // finally runs regardless of exception; note: closing may itself throw checked exceptions
    static void finallyDemo() {
        DemoResource r = null;
        try {
            r = new DemoResource("ok");
            int x = 10 / 0; // unchecked ArithmeticException
            System.out.println("This won't print: " + x);
        } catch (ArithmeticException e) {
            System.out.println("Caught unchecked in try: " + e);
        } finally {
            if (r != null) {
                try {
                    r.close(); // checked in finally must be handled
                } catch (IOException e) {
                    System.out.println("Caught checked during finally-close: " + e.getMessage());
                }
            }
        }
    }

    // Declaring an unchecked exception is optional; compiles with or without 'throws'
    static void mayDeclareUnchecked(int age) throws IllegalArgumentException {
        if (age < 0) throw new IllegalArgumentException("age must be non-negative");
        System.out.println("Age OK: " + age);
    }
}