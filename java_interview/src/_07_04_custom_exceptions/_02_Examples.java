package _07_04_custom_exceptions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

/**
 * Custom Exceptions - comprehensive examples in one file.
 *
 * What you’ll see:
 * - Creating checked and unchecked custom exceptions
 * - Adding context fields, error codes, and message formatting
 * - Exception hierarchy and multi-catch
 * - Wrapping/translation (preserving cause)
 * - Rethrowing and preserving stack trace
 * - Try-with-resources and suppressed exceptions
 * - When to use standard exceptions vs custom ones (noted in comments)
 */
public class _02_Examples {
    public static void main(String[] args) {
        System.out.println("1) Checked custom exception with context and cause:");
        try {
            OrderService service = new OrderService();
            service.process("IO"); // triggers underlying IOException wrapped into OrderProcessingException
        } catch (OrderProcessingException e) {
            System.out.println("  Caught: " + e);
            System.out.println("  orderId=" + e.getOrderId() + ", reason=" + e.getReason());
            if (e.getCause() != null) {
                System.out.println("  cause=" + e.getCause().getClass().getSimpleName() + ": " + e.getCause().getMessage());
            }
        }

        System.out.println("\n2) Unchecked custom exception with error code:");
        try {
            int port = ConfigLoader.parsePort(Map.of()); // missing 'port' key -> throws DomainConfigException
            System.out.println("  Port parsed: " + port);
        } catch (DomainConfigException e) {
            System.out.println("  Caught: " + e);
            System.out.println("  code=" + e.getCode() + ", key=" + e.getKey());
            if (e.getCause() != null) {
                System.out.println("  cause=" + e.getCause().getClass().getSimpleName() + ": " + e.getCause().getMessage());
            }
        }

        System.out.println("\n3) Exception hierarchy and multi-catch:");
        try {
            ResourceService svc = new ResourceService();
            svc.read("secret", "guest"); // will throw PermissionDeniedException
        } catch (ResourceNotFoundException | PermissionDeniedException e) { // multi-catch of siblings
            System.out.println("  Multi-caught: " + e.getClass().getSimpleName() + " -> " + e.getMessage());
        }

        System.out.println("\n4) Try-with-resources and suppressed exceptions:");
        demoSuppressed();

        System.out.println("\n5) Exception translation/wrapping with cause:");
        try {
            long len = new ThirdPartyTranslator().readFileLength("does_not_exist.txt");
            System.out.println("  File length: " + len);
        } catch (FileServiceException e) {
            System.out.println("  Caught: " + e);
            if (e.getCause() != null) {
                System.out.println("  cause=" + e.getCause().getClass().getSimpleName() + ": " + e.getCause().getMessage());
            }
        }

        System.out.println("\n6) Rethrowing to preserve original stack trace:");
        try {
            demoRethrowPreservingStackTrace();
        } catch (NumberFormatException e) {
            System.out.println("  Upstream caught rethrown: " + e.getClass().getSimpleName() + " -> " + e.getMessage());
        }

        System.out.println("\nDone.");
    }

    // Example 1: Checked custom exception with extra context and formatted message
    static final class OrderProcessingException extends Exception {
        private static final long serialVersionUID = 1L;

        enum Reason { NOT_FOUND, INVALID_STATE, PAYMENT_FAILED, IO_ERROR }

        private final String orderId;
        private final Reason reason;

        public OrderProcessingException(String orderId, Reason reason) {
            super();
            this.orderId = Objects.requireNonNull(orderId, "orderId");
            this.reason = Objects.requireNonNull(reason, "reason");
        }

        public OrderProcessingException(String orderId, Reason reason, Throwable cause) {
            super(cause);
            this.orderId = Objects.requireNonNull(orderId, "orderId");
            this.reason = Objects.requireNonNull(reason, "reason");
        }

        public String getOrderId() { return orderId; }
        public Reason getReason() { return reason; }

        @Override
        public String getMessage() {
            String base = "Order " + orderId + " failed: " + reason;
            String superMsg = super.getMessage();
            return (superMsg != null && !superMsg.isBlank()) ? base + " (" + superMsg + ")" : base;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{orderId='" + orderId + "', reason=" + reason + "}";
        }
    }

    static final class OrderService {
        // Use standard exceptions for programmer errors (e.g., invalid arguments)
        public void process(String orderId) throws OrderProcessingException {
            if (orderId == null || orderId.isBlank()) {
                throw new IllegalArgumentException("orderId must be non-blank");
            }

            if ("404".equals(orderId)) {
                throw new OrderProcessingException(orderId, OrderProcessingException.Reason.NOT_FOUND);
            }
            if ("BAD_STATE".equals(orderId)) {
                throw new OrderProcessingException(orderId, OrderProcessingException.Reason.INVALID_STATE);
            }
            if ("IO".equals(orderId)) {
                // Translate low-level IOException into domain-specific exception
                try {
                    Files.readString(Path.of("nonexistent-" + orderId + ".txt"));
                } catch (IOException ioe) {
                    throw new OrderProcessingException(orderId, OrderProcessingException.Reason.IO_ERROR, ioe);
                }
            }
            // else: success; do nothing
        }
    }

    // Example 2: Unchecked custom exception with error code and context
    static class DomainConfigException extends RuntimeException {
        private static final long serialVersionUID = 2L;

        enum ErrorCode { UNKNOWN, MISSING_PROPERTY, INVALID_VALUE }

        private final ErrorCode code;
        private final String key;

        // Framework-style standard constructors (best practice)
        public DomainConfigException() { this(ErrorCode.UNKNOWN, null); }
        public DomainConfigException(String message) { super(message); this.code = ErrorCode.UNKNOWN; this.key = null; }
        public DomainConfigException(String message, Throwable cause) { super(message, cause); this.code = ErrorCode.UNKNOWN; this.key = null; }
        public DomainConfigException(Throwable cause) { super(cause); this.code = ErrorCode.UNKNOWN; this.key = null; }

        // Domain-specific constructors carrying context
        public DomainConfigException(ErrorCode code, String key) {
            super();
            this.code = Objects.requireNonNull(code, "code");
            this.key = key;
        }
        public DomainConfigException(ErrorCode code, String key, Throwable cause) {
            super(cause);
            this.code = Objects.requireNonNull(code, "code");
            this.key = key;
        }

        public ErrorCode getCode() { return code; }
        public String getKey() { return key; }

        @Override
        public String getMessage() {
            String base = "Config error" +
                    (code != null ? " [" + code + "]" : "") +
                    (key != null ? " at key='" + key + "'" : "");
            String superMsg = super.getMessage();
            return (superMsg != null && !superMsg.isBlank()) ? base + ": " + superMsg : base;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{code=" + code + ", key=" + key + ", message=" + super.getMessage() + "}";
        }
    }

    static final class ConfigLoader {
        // Parses "port" from the provided config map. Throws an unchecked DomainConfigException on error.
        static int parsePort(Map<String, String> cfg) {
            String val = cfg.get("port");
            if (val == null) {
                throw new DomainConfigException(DomainConfigException.ErrorCode.MISSING_PROPERTY, "port");
            }
            try {
                int port = Integer.parseInt(val);
                if (port < 1 || port > 65535) {
                    throw new DomainConfigException(DomainConfigException.ErrorCode.INVALID_VALUE, "port",
                            new IllegalArgumentException("out of range"));
                }
                return port;
            } catch (NumberFormatException nfe) {
                throw new DomainConfigException(DomainConfigException.ErrorCode.INVALID_VALUE, "port", nfe);
            }
        }
    }

    // Example 3: Exception hierarchy + multi-catch
    static class ServiceException extends Exception {
        private static final long serialVersionUID = 3L;
        public ServiceException() { super(); }
        public ServiceException(String message) { super(message); }
        public ServiceException(String message, Throwable cause) { super(message, cause); }
        public ServiceException(Throwable cause) { super(cause); }
    }

    static final class ResourceNotFoundException extends ServiceException {
        private static final long serialVersionUID = 4L;
        private final String resourceId;
        public ResourceNotFoundException(String resourceId) {
            super("Resource not found: " + resourceId);
            this.resourceId = Objects.requireNonNull(resourceId, "resourceId");
        }
        public String getResourceId() { return resourceId; }
    }

    static final class PermissionDeniedException extends ServiceException {
        private static final long serialVersionUID = 5L;
        private final String user;
        private final String resourceId;
        public PermissionDeniedException(String user, String resourceId) {
            super("Permission denied for user '" + user + "' on resource '" + resourceId + "'");
            this.user = Objects.requireNonNull(user, "user");
            this.resourceId = Objects.requireNonNull(resourceId, "resourceId");
        }
        public String getUser() { return user; }
        public String getResourceId() { return resourceId; }
    }

    static final class ResourceService {
        public String read(String resourceId, String user)
                throws ResourceNotFoundException, PermissionDeniedException {
            if ("missing".equals(resourceId)) {
                throw new ResourceNotFoundException(resourceId);
            }
            if ("secret".equals(resourceId) && !"admin".equals(user)) {
                throw new PermissionDeniedException(user, resourceId);
            }
            return "data:" + resourceId;
        }
    }

    // Example 4: Try-with-resources with suppressed exceptions
    static final class FailingResource implements AutoCloseable {
        void use() {
            throw new RuntimeException("use failed");
        }
        @Override
        public void close() throws Exception {
            throw new Exception("close failed");
        }
    }

    private static void demoSuppressed() {
        try (FailingResource fr = new FailingResource()) {
            fr.use(); // primary failure
        } catch (Exception e) {
            System.out.println("  Primary: " + e.getClass().getSimpleName() + " -> " + e.getMessage());
            Throwable[] suppressed = e.getSuppressed();
            System.out.println("  Suppressed count: " + suppressed.length);
            for (Throwable s : suppressed) {
                System.out.println("    suppressed: " + s.getClass().getSimpleName() + " -> " + s.getMessage());
            }
        }
    }

    // Example 5: Wrapping/translation of third-party/low-level exceptions
    static final class FileServiceException extends ServiceException {
        private static final long serialVersionUID = 6L;
        private final String path;
        public FileServiceException(String path, Throwable cause) {
            super("Failed to access file: " + path, cause);
            this.path = Objects.requireNonNull(path, "path");
        }
        public String getPath() { return path; }
    }

    static final class ThirdPartyTranslator {
        long readFileLength(String path) throws FileServiceException {
            try {
                return Files.readAllBytes(Path.of(path)).length;
            } catch (IOException ioe) {
                // Translate low-level IOException -> domain-specific checked exception (with cause)
                throw new FileServiceException(path, ioe);
            }
        }
    }

    // Example 6: Catch-and-rethrow preserves the original stack trace when using "throw e;"
    private static void demoRethrowPreservingStackTrace() {
        try {
            Integer.parseInt("not-a-number");
        } catch (NumberFormatException e) {
            // Perform side-effects (e.g., log) then rethrow same exception to preserve stack
            // logger.warn("Parsing failed", e);  // example
            throw e; // preserves original stack trace
        }
    }
}