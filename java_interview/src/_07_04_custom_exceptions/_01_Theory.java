package _07_04_custom_exceptions;

/**
 * Custom Exceptions: theory + practical examples in one file.
 *
 * Contents:
 * - Checked vs unchecked exceptions and when to use which
 * - Designing a domain exception hierarchy with error codes
 * - Cause chaining, adding context, multi-catch
 * - try-with-resources and suppressed exceptions
 * - Overriding rules for "throws" in inheritance
 * - Performance notes (no control-flow with exceptions, lightweight exceptions)
 * - Minimal runnable demos in main()
 */
public class _01_Theory {

    public static void main(String[] args) {
        // 1) Checked exceptions: validation fails (OrderValidationException)
        Order bad = new Order("42", -1.0);
        try {
            placeOrder(bad);
        } catch (OrderValidationException e) {
            System.out.println("[Validation] " + e.getMessage() + " | code=" + e.getErrorCode());
        } catch (PaymentFailedException e) {
            System.out.println("[Payment] " + e.getMessage());
        } catch (OrderException e) {
            System.out.println("[Order] " + e);
        }

        // 2) Multi-catch for related checked exceptions
        Order ok = new Order("99", 10.0);
        try {
            placeOrder(ok);
        } catch (OrderValidationException | PaymentFailedException e) {
            System.out.println("[MultiCatch] caught=" + e.getClass().getSimpleName() + " | msg=" + e.getMessage());
        } catch (OrderException e) {
            throw new RuntimeException(e);
        }

        // 3) Suppressed exceptions in try-with-resources
        try {
            useFaultyResource();
        } catch (DemoOperationException e) {
            System.out.println("[TWR] primary=" + e.getMessage());
            for (Throwable s : e.getSuppressed()) {
                System.out.println("[TWR] suppressed=" + s.getClass().getSimpleName() + ": " + s.getMessage());
            }
        }

        // 4) Unchecked exception example
        try {
            loadConfig(null);
        } catch (ConfigurationException e) {
            System.out.println("[Unchecked] " + e.getMessage());
        }
    }

    /**
     * Illustrates: declaring and throwing checked custom exceptions,
     * wrapping lower-level runtime exceptions into domain-specific checked ones,
     * and propagating meaningful context.
     */
    static void placeOrder(Order order) throws OrderException {
        validate(order); // may throw OrderValidationException (checked)

        try {
            processPayment(order); // may throw a runtime exception, we wrap it
        } catch (RuntimeException ex) {
            // Wrap and add context; preserve root cause for diagnostics
            throw new PaymentFailedException(
                "Payment failed for orderId=" + order.id,
                ErrorCode.PAYMENT_DECLINED,
                ex
            );
        }
    }

    /**
     * Illustrates: checked exception for expected, recoverable client errors.
     */
    static void validate(Order order) throws OrderValidationException {
        if (order == null) {
            throw new OrderValidationException("order is null", ErrorCode.ORDER_NULL);
        }
        if (order.total <= 0) {
            // Include context, avoid secrets
            throw new OrderValidationException(
                "total must be positive for orderId=" + order.id,
                ErrorCode.INVALID_TOTAL
            );
        }
    }

    /**
     * Simulates a technical failure; runtime exceptions represent programmer/technical issues.
     */
    static void processPayment(Order order) {
        // Simulate a transient technical failure from a gateway/library
        throw new IllegalStateException("Gateway unreachable");
    }

    /**
     * Demonstrates suppressed exceptions: the close() failure is attached to the primary exception.
     */
    static void useFaultyResource() {
        try (FaultyResource r = new FaultyResource("R1")) {
            throw new DemoOperationException("Primary failure during work");
        }
    }

    /**
     * Illustrates unchecked exceptions for non-recoverable/precondition violations.
     */
    static void loadConfig(String path) {
        if (path == null) {
            throw new ConfigurationException("Config path cannot be null");
        }
    }

    // --- Support types used in the examples ---

    static final class Order {
        final String id;
        final double total;
        Order(String id, double total) { this.id = id; this.total = total; }
    }

    static final class FaultyResource implements AutoCloseable {
        final String name;
        FaultyResource(String name) { this.name = name; }
        @Override public void close() {
            // This exception will be suppressed if a primary exception occurred in the try block
            throw new ResourceCloseException("Error closing " + name);
        }
    }

    /**
     * Base checked exception for the "Order" domain.
     * Guidelines:
     * - Provide standard constructors.
     * - Keep extra fields immutable (final).
     * - Preserve the cause to maintain diagnostic chains.
     * - Include serialVersionUID (exceptions are Serializable).
     */
    static class OrderException extends Exception {
        private static final long serialVersionUID = 1L;
        private final ErrorCode errorCode;

        public OrderException() { this.errorCode = ErrorCode.UNKNOWN; }
        public OrderException(String message) {
            super(message);
            this.errorCode = ErrorCode.UNKNOWN;
        }
        public OrderException(String message, Throwable cause) {
            super(message, cause);
            this.errorCode = ErrorCode.UNKNOWN;
        }
        public OrderException(String message, ErrorCode code) {
            super(message);
            this.errorCode = code == null ? ErrorCode.UNKNOWN : code;
        }
        public OrderException(String message, ErrorCode code, Throwable cause) {
            super(message, cause);
            this.errorCode = code == null ? ErrorCode.UNKNOWN : code;
        }
        public ErrorCode getErrorCode() { return errorCode; }
    }

    /**
     * Checked exception for user/data validation problems.
     * Consumers can catch this specifically to request corrected input.
     */
    static final class OrderValidationException extends OrderException {
        private static final long serialVersionUID = 1L;
        public OrderValidationException(String message, ErrorCode code) { super(message, code); }
    }

    /**
     * Checked exception signaling a payment operation failure.
     * Wraps lower-level causes while exposing a stable domain error code.
     */
    static final class PaymentFailedException extends OrderException {
        private static final long serialVersionUID = 1L;
        public PaymentFailedException(String message, ErrorCode code, Throwable cause) {
            super(message, code, cause);
        }
    }

    /**
     * Unchecked for configuration/programming errors: not expected to be recovered by callers.
     */
    static class ConfigurationException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public ConfigurationException(String message) { super(message); }
        public ConfigurationException(String message, Throwable cause) { super(message, cause); }
    }

    static class DemoOperationException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public DemoOperationException(String message) { super(message); }
    }

    static class ResourceCloseException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public ResourceCloseException(String message) { super(message); }
    }

    /**
     * Error codes help programmatic handling and stable API contracts.
     */
    enum ErrorCode {
        UNKNOWN,
        ORDER_NULL,
        INVALID_TOTAL,
        PAYMENT_DECLINED
    }

    /**
     * Demonstrates "throws" overriding rules:
     * - Overriding method may throw fewer or narrower checked exceptions.
     * - Cannot add broader/new checked exceptions.
     * - Unchecked exceptions are unrestricted.
     */
    interface DataSource {
        String read() throws java.io.IOException;
    }
    static class FileDataSource implements DataSource {
        @Override
        public String read() throws java.io.FileNotFoundException { // narrower checked is allowed
            throw new java.io.FileNotFoundException("demo");
        }
    }
    static class MemoryDataSource implements DataSource {
        @Override
        public String read() { // removing checked exceptions is allowed
            return "ok";
        }
    }

    /**
     * Lightweight runtime exception with stack trace disabled.
     * Use sparingly in hot paths when diagnostics are known to be unnecessary.
     */
    static class LightweightException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public LightweightException(String message) {
            // message, cause=null, suppression disabled, stack trace disabled
            super(message, null, false, false);
        }
        // Alternative (older style) would be overriding fillInStackTrace(), but the constructor above is clearer.
    }
}

/*
THEORY: Custom Exceptions in Java

1) Why create custom exceptions?
   - Communicate domain intent precisely (e.g., OrderValidationException).
   - Enable programmatic handling via structured data (e.g., ErrorCode).
   - Encapsulate low-level details; avoid leaking internal exceptions across module boundaries.

2) Checked vs Unchecked
   - Checked (extends Exception): caller must declare/handle; use for expected, recoverable conditions at API boundaries.
   - Unchecked (extends RuntimeException): use for programmer errors, invariant violations, system/config problems.
   - Do not extend Error/Throwable.

3) Design guidelines
   - Keep the hierarchy small; use a base domain exception (e.g., OrderException).
   - Provide standard ctors: (), (String), (String, Throwable), plus domain-state ctors as needed.
   - Preserve causes for diagnostics; prefer constructors over initCause().
   - Include contextual info in messages (ids, sizes), but avoid secrets/PII.
   - Exceptions are Serializable; include serialVersionUID.
   - Keep extra fields final to retain immutability and thread-safety.

4) throws and overriding
   - Methods declare checked exceptions via "throws".
   - Overriding may narrow or remove checked exceptions, but not add broader ones.
   - Unchecked exceptions need no declaration and can be added/removed freely.

5) Handling best practices
   - Catch the most specific type possible.
   - Don’t swallow exceptions silently. Either handle, wrap (with context), or rethrow.
   - Avoid "catch (Exception)" unless you rethrow or translate and you truly need a boundary guard.
   - Avoid duplicate logging on multiple layers; log once near the boundary or where action is taken.

6) Multi-catch and try-with-resources
   - Multi-catch reduces duplication: catch (A | B e) { ... }.
   - try-with-resources closes AutoCloseable; close() failures are suppressed on the primary exception (getSuppressed()).

7) Performance notes
   - Throwing is expensive (stack traces). Don’t use exceptions for control flow.
   - For extremely hot paths, consider disabling stack traces in rare/unrecoverable cases (LightweightException).
   - Prefer fast precondition checks (IllegalArgumentException, Objects.requireNonNull).

8) API boundary strategy
   - Translate third-party exceptions to your domain exceptions before crossing module/service boundaries.
   - Use checked exceptions sparingly and deliberately; unchecked for programming errors.
   - Map domain exceptions to transport-layer responses (e.g., HTTP status + error code).

9) Testing
   - Use assertThrows in JUnit to verify types/messages/causes.
   - Ensure error codes and messages remain stable if they are part of the contract.

10) Common pitfalls
   - Don’t expose sensitive data in messages.
   - Don’t mix control flow with exceptions.
   - Don’t create too many fine-grained types; prefer a small, meaningful set plus error codes.
   - Avoid “sneaky throws” tricks; they harm readability and tooling.

*/