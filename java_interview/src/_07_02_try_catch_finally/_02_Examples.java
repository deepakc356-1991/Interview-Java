package _07_02_try_catch_finally;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class _02_Examples {

    // Static init can use try/catch to handle startup issues
    static {
        try {
            Integer.parseInt("x");
        } catch (NumberFormatException e) {
            System.out.println("static init caught: " + e.getClass().getSimpleName());
        }
    }

    // Custom exceptions
    static class MyCheckedException extends Exception {
        public MyCheckedException(String message, Throwable cause) { super(message, cause); }
    }
    static class MyUncheckedException extends RuntimeException {
        public MyUncheckedException(String message) { super(message); }
    }

    // Simple AutoCloseable resource to demonstrate try-with-resources
    static class DemoResource implements AutoCloseable {
        private final String name;
        private final boolean throwOnClose;

        DemoResource(String name, boolean throwOnClose) {
            this.name = name;
            this.throwOnClose = throwOnClose;
            System.out.println("open " + name);
        }

        @Override
        public void close() throws Exception {
            System.out.println("close " + name);
            if (throwOnClose) {
                throw new Exception("close failed: " + name);
            }
        }
    }

    public static void main(String[] args) {
        basicTryCatchFinally();
        multipleCatchesOrder();
        multiCatchSingleBlock();

        catchSuperclassVsSubclass();

        System.out.println("computeWithFinally returned: " + computeWithFinally());
        System.out.println("returnInFinally returned: " + returnInFinally());

        System.out.println("mutableReturn -> " + mutableReturn());

        nestedTry();

        try {
            rethrowAsIs();
        } catch (IOException e) {
            System.out.println("caller caught rethrown: " + e);
        }

        try {
            translateException();
        } catch (MyCheckedException e) {
            System.out.println("caller caught translated: " + e + " cause: " + e.getCause());
        }

        tryWithResourcesBasic();
        tryWithResourcesMultiple();
        suppressedExceptionsFromClose();
        suppressedLostInFinally();

        swallowExceptionBad();

        try {
            mayThrowChecked();
        } catch (Exception e) {
            System.out.println("caught checked: " + e);
        }
        try {
            mayThrowUnchecked();
        } catch (RuntimeException e) {
            System.out.println("caught unchecked: " + e);
        }

        try {
            improvedRethrow(true);
        } catch (IOException e) {
            System.out.println("improvedRethrow caught IO: " + e.getMessage());
        } catch (ParseException e) {
            System.out.println("improvedRethrow caught Parse: " + e.getMessage());
        }
        try {
            improvedRethrow(false);
        } catch (IOException e) {
            System.out.println("improvedRethrow caught IO: " + e.getMessage());
        } catch (ParseException e) {
            System.out.println("improvedRethrow caught Parse: " + e.getMessage());
        }

        tryWithResourcesAndFinally();

        breakFinally();

        catchThrowableDemo();

        // Java 9+ example (commented to compile on older JDKs):
        // DemoResource ext = new DemoResource("ext", false);
        // try (ext) { System.out.println("Java 9+: use existing resource in TWR"); } catch (Exception ignored) {}
    }

    // 1) Basic try/catch/finally
    private static void basicTryCatchFinally() {
        System.out.println("\n-- basicTryCatchFinally");
        try {
            int x = 10 / 0; // throws ArithmeticException
            System.out.println(x); // unreachable
        } catch (ArithmeticException e) {
            System.out.println("caught: " + e.getClass().getSimpleName());
        } finally {
            System.out.println("finally always runs");
        }
    }

    // 2) Multiple catch blocks; order matters (specific before general)
    private static void multipleCatchesOrder() {
        System.out.println("\n-- multipleCatchesOrder");
        int[] arr = {1, 2};
        try {
            Integer.parseInt("NaN");
            System.out.println(arr[10]);
        } catch (NumberFormatException e) {
            System.out.println("caught NumberFormatException");
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("caught AIOOBE");
        } catch (Exception e) { // general last
            System.out.println("caught general Exception");
        } finally {
            System.out.println("finally after multiple catches");
        }

        try {
            System.out.println(arr[5]);
        } catch (NumberFormatException e) {
            System.out.println("won't hit");
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("caught AIOOBE (second try)");
        }
    }

    // 3) Multi-catch (single block for multiple unrelated exceptions)
    private static void multiCatchSingleBlock() {
        System.out.println("\n-- multiCatchSingleBlock");
        for (String val : new String[]{"bad-number", null}) {
            try {
                if (val == null) {
                    val.length(); // NullPointerException
                } else {
                    Integer.parseInt(val); // NumberFormatException
                }
            } catch (NumberFormatException | NullPointerException e) {
                System.out.println("multi-catch caught: " + e.getClass().getSimpleName());
            }
        }

        // Invalid (compile error) - types in multi-catch cannot have subclass relationship:
        // catch (Exception | RuntimeException e) { } // RuntimeException is a subclass of Exception
    }

    // 4) Catching superclass vs subclass; wrong order is unreachable (compile error if uncommented)
    private static void catchSuperclassVsSubclass() {
        System.out.println("\n-- catchSuperclassVsSubclass");
        try {
            Integer.parseInt("bad");
        } catch (NumberFormatException e) { // specific first
            System.out.println("caught NumberFormatException (specific)");
        } catch (Exception e) {
            System.out.println("caught general Exception");
        }

        // Wrong order (uncommenting causes compile error: 'unreachable catch block'):
        // try {
        //     Integer.parseInt("bad");
        // } catch (Exception e) {
        //     System.out.println("general first");
        // } catch (NumberFormatException e) {
        //     System.out.println("unreachable");
        // }
    }

    // 5) finally runs even when returning
    private static int computeWithFinally() {
        try {
            return 10;
        } finally {
            System.out.println("finally runs even with return in try");
        }
    }

    // 6) Returning in finally overrides earlier returns (discouraged)
    private static int returnInFinally() {
        try {
            return 1;
        } catch (RuntimeException e) {
            return 2;
        } finally {
            return 3; // overrides previous returns
        }
    }

    // 7) finally can modify a returned mutable object
    private static List<String> mutableReturn() {
        List<String> list = new ArrayList<>();
        try {
            list.add("before");
            return list; // object reference returned
        } finally {
            list.add("after"); // affects caller-visible state
        }
    }

    // 8) Nested try/catch/finally
    private static void nestedTry() {
        System.out.println("\n-- nestedTry");
        try {
            try {
                throw new IllegalArgumentException("inner");
            } catch (IllegalArgumentException e) {
                System.out.println("inner caught: " + e.getMessage());
            } finally {
                System.out.println("inner finally");
            }
        } finally {
            System.out.println("outer finally");
        }
    }

    // 9) Rethrow the same exception (after logging/cleanup)
    private static void rethrowAsIs() throws IOException {
        System.out.println("\n-- rethrowAsIs");
        try {
            fakeIO();
        } catch (IOException e) {
            System.out.println("logging then rethrowing IOException");
            throw e; // preserves original stack trace
        }
    }

    // 10) Translate/wrap exception to a domain-specific checked exception
    private static void translateException() throws MyCheckedException {
        System.out.println("\n-- translateException");
        try {
            fakeIO();
        } catch (IOException e) {
            throw new MyCheckedException("Operation failed", e);
        }
    }

    // 11) Try-with-resources (AutoCloseable) basic
    private static void tryWithResourcesBasic() {
        System.out.println("\n-- tryWithResourcesBasic");
        try (DemoResource r = new DemoResource("R1", false)) {
            System.out.println("using resource");
        } catch (Exception e) {
            System.out.println("caught: " + e);
        }
    }

    // 12) Multiple resources; close order is LIFO (R2 closes before R1)
    private static void tryWithResourcesMultiple() {
        System.out.println("\n-- tryWithResourcesMultiple");
        try (DemoResource r1 = new DemoResource("R1", false);
             DemoResource r2 = new DemoResource("R2", false)) {
            System.out.println("using both resources");
        } catch (Exception e) {
            System.out.println("caught: " + e);
        }
    }

    // 13) Suppressed exceptions from close() are attached to the primary exception
    private static void suppressedExceptionsFromClose() {
        System.out.println("\n-- suppressedExceptionsFromClose");
        try (DemoResource r = new DemoResource("R", true)) {
            System.out.println("body throws");
            throw new IllegalStateException("body failure");
        } catch (Exception e) {
            System.out.println("primary: " + e);
            for (Throwable s : e.getSuppressed()) {
                System.out.println("suppressed: " + s);
            }
        }
    }

    // 14) Exception in finally overrides try exception (original is lost)
    private static void suppressedLostInFinally() {
        System.out.println("\n-- suppressedLostInFinally");
        try {
            try {
                throw new IllegalStateException("try fail");
            } finally {
                throw new RuntimeException("finally fail");
            }
        } catch (RuntimeException e) {
            System.out.println("caught (original lost): " + e);
        }
    }

    // 15) Swallowing exceptions is a bad practice (example only)
    private static void swallowExceptionBad() {
        System.out.println("\n-- swallowExceptionBad");
        try {
            Integer.parseInt("bad");
        } catch (NumberFormatException e) {
            // Bad: silently ignored
        }
        System.out.println("continued after swallow (not recommended)");
    }

    // 16) Checked vs unchecked
    private static void mayThrowChecked() throws Exception {
        throw new Exception("checked example");
    }
    private static void mayThrowUnchecked() {
        throw new MyUncheckedException("unchecked example");
    }

    // 17) Improved rethrow (Java 7+): catch broad type, rethrow preserves specific types
    private static void improvedRethrow(boolean io) throws IOException, ParseException {
        try {
            if (io) {
                throw new IOException("io issue");
            } else {
                throw new ParseException("parse issue", 0);
            }
        } catch (Exception e) {
            // Allowed: compiler infers e is either IOException or ParseException
            throw e;
        }
    }

    // 18) Try-with-resources plus a finally block
    private static void tryWithResourcesAndFinally() {
        System.out.println("\n-- tryWithResourcesAndFinally");
        try (DemoResource r = new DemoResource("TWR+finally", false)) {
            System.out.println("body");
        } catch (Exception e) {
            System.out.println("caught: " + e);
        } finally {
            System.out.println("finally after try-with-resources");
        }
    }

    // 19) finally executes on break/continue as well
    private static void breakFinally() {
        System.out.println("\n-- breakFinally");
        for (int i = 0; i < 1; i++) {
            try {
                System.out.println("before break");
                break;
            } finally {
                System.out.println("finally runs on break");
            }
        }
    }

    // 20) Catching Throwable is last-resort (example only)
    private static void catchThrowableDemo() {
        System.out.println("\n-- catchThrowableDemo");
        try {
            throw new OutOfMemoryError("simulated (not really throwing OOME here)"); // Not recommended in real code
        } catch (Throwable t) { // catches Exception and Error (discouraged)
            System.out.println("caught Throwable: " + t.getClass().getSimpleName());
        }
    }

    // Helper
    private static void fakeIO() throws IOException {
        throw new IOException("simulated IO failure");
    }

    // Note:
    // - finally does NOT run if the JVM exits (e.g., System.exit(0)) or crashes.
    // - catch parameter is effectively final; you cannot assign a new value to it.
    //   Example (won't compile):
    //   catch (Exception e) { e = new Exception(); }
}