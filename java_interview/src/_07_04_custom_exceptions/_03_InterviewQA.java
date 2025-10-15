package _07_04_custom_exceptions;

/*
 Custom Exceptions – Interview Q&A with runnable examples (basic → advanced)

 Q: What is a custom exception and why create one?
 A: A user-defined exception class used to express domain/technical errors with clarity, context, and type-safety.

 Q: Checked vs Unchecked?
 A:
 - Checked (extends Exception): must be declared or caught. Use for recoverable conditions (e.g., validation, I/O).
 - Unchecked (extends RuntimeException): not required to declare/catch. Use for programmer errors or unrecoverable states.

 Q: How to design a custom exception?
 A:
 - Pick checked/unchecked based on recoverability.
 - Provide standard constructors (msg, cause, msg+cause).
 - Optionally add rich context (error code, ids).
 - Avoid losing the original cause (wrap and chain).
 - Avoid catching broad Exception unless necessary.
 - Don’t swallow exceptions; log or propagate appropriately.

 Q: Exception hierarchy best practice?
 A:
 - Create a small hierarchy (base domain exception + specific subtypes).
 - Enables precise catch/handling and clear mapping (e.g., to HTTP status codes).

 Q: When to wrap/translate exceptions?
 A:
 - At module boundaries (e.g., from SQL → DataAccessException) to avoid leaking low-level details.
 - Always keep the cause for diagnostics.

 Q: Suppressed exceptions?
 A:
 - try-with-resources can add suppressed exceptions from close(). Access via getSuppressed().

 Q: Performance considerations?
 A:
 - Stack trace capture is expensive. For high-frequency paths, consider overriding fillInStackTrace().
 - Use sparingly; stack traces aid diagnostics.

 Q: “throw e;” vs “throw new ... (e)”?
 A:
 - throw e preserves the original stack trace.
 - throw new wraps and adds context but changes top-of-stack to the new throw site.

 Q: Multi-catch and rethrow?
 A:
 - catch (A | B e) allows shared handling. Rethrow either directly or as a wrapped type.

 Q: How to test exceptions?
 A:
 - JUnit: assertThrows(...) and assert details (message, cause, fields).

 Q: Avoid anti-patterns?
 A:
 - Do not use exceptions for normal control flow.
 - Don’t empty-catch.
 - Don’t lose context; include key identifiers (not PII).
 - Don’t expose internal details or stack traces to end users.

 Q: Sneaky throws?
 A:
 - Advanced generic trick to throw checked without declaring; avoid in production unless justified.

 See the runnable demos in main() and the methods below.
*/

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;

public class _03_InterviewQA {

    public static void main(String[] args) {
        System.out.println("Custom Exceptions – runnable demos:");

        demoCheckedVsUnchecked();
        demoHierarchyAndWrapping();
        demoTryWithResourcesSuppressed();
        demoMultiCatchAndRethrow();
        demoLightweightExceptionPerformance();
        demoErrorCodeAndContext();
    }

    // BASIC: Checked vs Unchecked
    private static void demoCheckedVsUnchecked() {
        System.out.println("\n1) Checked vs Unchecked:");
        try {
            validateAge(-5); // throws checked ValidationException
        } catch (ValidationException e) {
            System.out.println("Checked caught: " + e.getMessage());
        }

        try {
            enforceInvariant(0); // throws unchecked InvariantViolationException
        } catch (InvariantViolationException e) {
            System.out.println("Unchecked caught: " + e.getMessage());
        }
    }

    // INTERMEDIATE: Hierarchy + Exception translation/wrapping
    private static void demoHierarchyAndWrapping() {
        System.out.println("\n2) Hierarchy + translation:");
        try {
            getUserDisplayName(42); // simulate DAO throwing SQLException translated to DataAccessException
        } catch (DataAccessException e) {
            System.out.println("Translated exception: " + e.getMessage());
            System.out.println("Cause type: " + (e.getCause() != null ? e.getCause().getClass().getSimpleName() : "none"));
        }

        try {
            findResource("order", "abc-123"); // throws checked NotFoundException
        } catch (NotFoundException e) {
            System.out.println("Not found: " + e.getMessage() + " [resource=" + e.getResourceName() + ", id=" + e.getResourceId() + "]");
        }
    }

    // ADVANCED: try-with-resources + suppressed exceptions
    private static void demoTryWithResourcesSuppressed() {
        System.out.println("\n3) try-with-resources + suppressed:");
        try (FaultyResource r = new FaultyResource()) {
            // Primary failure in the try block:
            throw new ValidationException("Primary failure during work");
        } catch (ValidationException e) {
            System.out.println("Caught primary: " + e.getMessage());
            for (Throwable s : e.getSuppressed()) {
                System.out.println("Suppressed: " + s.getClass().getSimpleName() + " -> " + s.getMessage());
            }
        }
    }

    // INTERMEDIATE: Multi-catch + rethrow/wrap
    private static void demoMultiCatchAndRethrow() {
        System.out.println("\n4) Multi-catch + rethrow:");
        try {
            parseAndCheck("NaN");
        } catch (BadRequestException e) {
            System.out.println("Wrapped bad input: " + e.getMessage() + "; cause=" +
                    (e.getCause() != null ? e.getCause().getClass().getSimpleName() : "none"));
        }

        // Difference between "throw e" and "throw new ... (e)" shown by stack trace:
        try {
            preserveStackTrace();
        } catch (RuntimeException e) {
            System.out.println("Preserved stack (top shows original throw site):");
            System.out.println(stackTraceOf(e));
        }

        try {
            wrapAndAddContext();
        } catch (RuntimeException e) {
            System.out.println("Wrapped stack (top shows wrap site; original inside cause):");
            System.out.println(stackTraceOf(e));
        }
    }

    // ADVANCED: Performance with lightweight exception
    private static void demoLightweightExceptionPerformance() {
        System.out.println("\n5) Lightweight exception (no stack capture):");
        try {
            throw new LightweightNoStackException("Hot-path signal: do not capture stack");
        } catch (LightweightNoStackException e) {
            // Stacktrace will be nearly empty; use only when truly needed.
            System.out.println("Caught lightweight: " + e.getMessage() + " (stack size=" + e.getStackTrace().length + ")");
        }
    }

    // INTERMEDIATE: Rich context (error code)
    private static void demoErrorCodeAndContext() {
        System.out.println("\n6) Rich context with error code:");
        try {
            authorize(false);
        } catch (WithErrorCode e) {
            System.out.println("Error code: " + e.getCode() + " -> " + e.getMessage());
        }
    }

    // =========================
    // Example domain API methods
    // =========================

    // Checked example: recoverable validation
    static void validateAge(int age) throws ValidationException {
        if (age < 0 || age > 150) {
            throw new ValidationException("Age out of range: " + age);
        }
    }

    // Unchecked example: programming error/invariant
    static void enforceInvariant(int mustBePositive) {
        if (mustBePositive <= 0) {
            throw new InvariantViolationException("Expected positive, got " + mustBePositive);
        }
    }

    // Translation/wrapping: low-level -> domain
    static String getUserDisplayName(int id) {
        try {
            // Simulate lower-level exception (e.g., SQLException)
            throw new SQLException("DB connection timeout");
        } catch (SQLException e) {
            // Translate to domain-level unchecked exception; keep cause
            throw new DataAccessException("Failed to load user " + id, e);
        }
    }

    // Checked not found with context
    static void findResource(String resource, String id) throws NotFoundException {
        // Simulate not found
        throw new NotFoundException(resource, id);
    }

    // Multi-catch and wrap
    static int parseAndCheck(String input) {
        try {
            int value = Integer.parseInt(input);           // may throw NumberFormatException
            if (value < 0) throw new IllegalArgumentException("Value must be non-negative");
            return value;
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid input: " + input, e);
        }
    }

    // Rethrow preserving original stack
    static void preserveStackTrace() {
        try {
            lowLevelThrow();
        } catch (RuntimeException e) {
            // "throw e" preserves original throw site at top of stack
            throw e;
        }
    }

    // Wrap and add context (changes top-of-stack)
    static void wrapAndAddContext() {
        try {
            lowLevelThrow();
        } catch (RuntimeException e) {
            // Add message/context; top-of-stack will be here
            throw new RuntimeException("While processing request XYZ", e);
        }
    }

    static void lowLevelThrow() {
        throw new RuntimeException("Original low-level failure");
    }

    static void authorize(boolean allowed) {
        if (!allowed) {
            throw new WithErrorCode(403, "Forbidden: missing permission");
        }
    }

    // ==============
    // Helper classes
    // ==============

    // Base checked exception
    static class AppCheckedException extends Exception {
        private static final long serialVersionUID = 1L;
        public AppCheckedException() {}
        public AppCheckedException(String message) { super(message); }
        public AppCheckedException(String message, Throwable cause) { super(message, cause); }
        public AppCheckedException(Throwable cause) { super(cause); }
    }

    // Base unchecked exception
    static class AppRuntimeException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public AppRuntimeException() {}
        public AppRuntimeException(String message) { super(message); }
        public AppRuntimeException(String message, Throwable cause) { super(message, cause); }
        public AppRuntimeException(Throwable cause) { super(cause); }
    }

    // Checked validation error
    static class ValidationException extends AppCheckedException {
        private static final long serialVersionUID = 2L;
        public ValidationException(String message) { super(message); }
        public ValidationException(String message, Throwable cause) { super(message, cause); }
    }

    // Checked not-found with rich context
    static class NotFoundException extends AppCheckedException {
        private static final long serialVersionUID = 3L;
        private final String resourceName;
        private final String resourceId;

        public NotFoundException(String resourceName, String resourceId) {
            super(resourceName + " with id " + resourceId + " not found");
            this.resourceName = resourceName;
            this.resourceId = resourceId;
        }

        public String getResourceName() { return resourceName; }
        public String getResourceId() { return resourceId; }
    }

    // Unchecked invariant violation
    static class InvariantViolationException extends AppRuntimeException {
        private static final long serialVersionUID = 4L;
        public InvariantViolationException(String message) { super(message); }
    }

    // Unchecked data access (wraps low-level)
    static class DataAccessException extends AppRuntimeException {
        private static final long serialVersionUID = 5L;
        public DataAccessException(String message, Throwable cause) { super(message, cause); }
    }

    // Unchecked bad request (input errors)
    static class BadRequestException extends AppRuntimeException {
        private static final long serialVersionUID = 6L;
        public BadRequestException(String message, Throwable cause) { super(message, cause); }
    }

    // Performance-oriented: no stack trace capture
    static class LightweightNoStackException extends RuntimeException {
        private static final long serialVersionUID = 7L;
        public LightweightNoStackException(String message) { super(message, null, false, false); }
        // Alternatively, could override fillInStackTrace() to return this.
        @Override public synchronized Throwable fillInStackTrace() { return this; }
    }

    // Unchecked with error code
    static class WithErrorCode extends AppRuntimeException {
        private static final long serialVersionUID = 8L;
        private final int code;
        public WithErrorCode(int code, String message) { super(message); this.code = code; }
        public WithErrorCode(int code, String message, Throwable cause) { super(message, cause); this.code = code; }
        public int getCode() { return code; }
    }

    // Demonstrates suppressed exceptions in try-with-resources
    static class FaultyResource implements AutoCloseable {
        @Override public void close() {
            // This will be suppressed if a primary exception is thrown inside try
            throw new RuntimeException("Close() failure");
        }
    }

    // Advanced: Sneaky throw (avoid in production unless justified)
    // static <E extends Throwable> void sneakyThrow(Throwable t) throws E { throw (E) t; }

    // Utility to show stack trace in examples
    static String stackTraceOf(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}