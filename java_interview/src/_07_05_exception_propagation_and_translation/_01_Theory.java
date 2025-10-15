package _07_05_exception_propagation_and_translation;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.SQLException;

/*
Exception Propagation & Translation — Theory-by-Code

What this file covers (search for markers like [P1], [T1], etc.):
- [P1] Propagation basics: call stack, checked vs unchecked, throws clause
- [P2] Rethrow vs wrap: preserving origin vs resetting top frame
- [P3] Precise rethrow (Java 7): catching Exception/Throwable and rethrowing specific types
- [T1] Exception translation: converting low-level exceptions into higher-level, domain-specific ones
- [T2] Multi-catch translation; boundaries between layers; best practices
- [F1] finally vs try-with-resources: suppressed exceptions vs overshadowing
- [THR] Thread boundaries: exceptions don’t propagate across threads
- [TU] Tunneling (unchecked wrapping) and when to avoid it
- Misc tips in comments: chaining (cause), initCause, suppressed, sneaky throws (advanced)

Note: The methods here print small summaries to illustrate concepts. Read comments for theory.
*/
public class _01_Theory {

    public static void main(String[] args) {
        print("=== Exception Propagation & Translation demo ===");

        // [P1] Propagation through the call stack (checked vs unchecked)
        propagationDemo();

        // [P2] Rethrow vs wrap: stack trace differences
        try {
            rethrowPreservesOrigin(); // throws IOException; rethrow keeps original throw site
        } catch (Exception e) {
            printStackSummary("rethrowPreservesOrigin", e);
        }
        try {
            wrapResetsTopFrame(); // wrap creates new exception here; cause points to origin
        } catch (Exception e) {
            printStackSummary("wrapResetsTopFrame", e);
        }

        // [P3] Precise rethrow (Java 7+): catch Exception and still declare specific throws
        try {
            preciseRethrow();
        } catch (Exception e) {
            print("preciseRethrow caught: " + e.getClass().getSimpleName());
        }

        // [T1] Translation at boundaries: Repository -> Service -> Controller
        try {
            new Controller(new Service(new Repository()))
                .handleFindById("42", true, true); // simulate SQL failure then translation
        } catch (ServiceException e) {
            print("Controller bubbled ServiceException: " + e.getMessage());
            if (e.getCause() != null) {
                print(" cause: " + e.getCause().getClass().getSimpleName());
            }
        }

        // [T2] Multi-catch translation demo (no output; see code)
        // new Service(new Repository()).unifiedLoad(); // Example pattern in code comments

        // [F1] Suppressed vs overshadowed exceptions
        suppressedDemo();
        overshadowingFinallyDemo();

        // [THR] Thread boundaries demo (UncaughtExceptionHandler)
        threadPropagationDemo();

        // [TU] Tunneling checked to unchecked (use with care)
        sniffTunnelingDemo();

        print("=== Done ===");
    }

    // UTILITIES

    private static void print(String s) {
        System.out.println(s);
    }

    private static String describe(Throwable t) {
        return t.getClass().getSimpleName() + ": " + t.getMessage();
    }

    private static void printStackSummary(String label, Throwable t) {
        print("[" + label + "] " + describe(t));
        if (t.getStackTrace().length > 0) {
            print(" top frame: " + t.getStackTrace()[0]);
        }
        if (t.getCause() != null) {
            Throwable c = t.getCause();
            print(" cause: " + c.getClass().getSimpleName() + " -> " + c.getMessage());
            if (c.getStackTrace().length > 0) {
                print("  cause top frame: " + c.getStackTrace()[0]);
            }
        }
    }

    // [P1] PROPAGATION BASICS

    /*
    Propagation basics:
    - A thrown exception travels ("propagates") up the call stack until a matching catch is found.
    - Checked exceptions (subclasses of Exception, not RuntimeException) must be caught or declared with "throws".
    - Unchecked exceptions (RuntimeException and subclasses) do not need to be declared; they propagate freely.
    */
    private static void propagationDemo() {
        try {
            topChecked();
        } catch (IOException e) {
            print("Handled checked IOException at propagationDemo: " + e.getMessage());
        }
        try {
            topUnchecked();
        } catch (RuntimeException e) {
            print("Handled unchecked RuntimeException at propagationDemo: " + e.getMessage());
        }
    }

    // Checked path
    private static void topChecked() throws IOException { midChecked(); }
    private static void midChecked() throws IOException { leafChecked(); }
    private static void leafChecked() throws IOException { throw new IOException("I/O failed at leafChecked"); }

    // Unchecked path
    private static void topUnchecked() { midUnchecked(); }
    private static void midUnchecked() { leafUnchecked(); }
    private static void leafUnchecked() { throw new IllegalArgumentException("Bad argument at leafUnchecked"); }

    // [P2] RETHROW VS WRAP

    /*
    Rethrowing the same exception instance ("throw e;") preserves the original stack trace (origin).
    Wrapping creates a new exception whose top frame is the wrapping site; original is in getCause().
    */
    private static void rethrowPreservesOrigin() throws IOException {
        try {
            leafChecked();
        } catch (IOException e) {
            // Rethrow the same instance: origin (throw site) remains leafChecked, not here.
            throw e;
        }
    }

    private static void wrapResetsTopFrame() {
        try {
            leafChecked();
        } catch (IOException e) {
            // New exception created here; top of stack is this site; original is the cause.
            throw new RuntimeException("Wrapped here for policy/compatibility", e);
        }
    }

    // [P3] PRECISE RETHROW (Java 7+)

    /*
    Catching Exception/Throwable and rethrowing without altering the variable allows the compiler to infer and keep
    specific thrown types ("precise rethrow"). This lets you maintain a precise throws clause.
    */
    private static void preciseRethrow() throws IOException, SQLException {
        try {
            maybeThrowIOExceptionOrSQLException();
        } catch (Exception e) {
            // e is effectively final and is rethrown as-is; compiler infers IOException | SQLException.
            throw e;
        }
    }

    private static void maybeThrowIOExceptionOrSQLException() throws IOException, SQLException {
        if ((System.nanoTime() & 1L) == 0L) {
            throw new IOException("IO branch");
        } else {
            throw new SQLException("SQL branch");
        }
    }

    // [T1] EXCEPTION TRANSLATION (LAYERING)

    /*
    Translation converts lower-level exceptions into higher-level ones meaningful to the caller (domain-oriented),
    while preserving the cause chain. Do this at layer/module boundaries.
    - Repository throws SQLException (low-level)
    - Service catches and translates to DataAccessException / NotFoundException (domain-level or layer-level)
    - Controller may translate further to ServiceException for a presentation/API boundary
    */
    static class Repository {
        String findById(String id, boolean simulateSqlFailure, boolean missing) throws SQLException {
            if (simulateSqlFailure) throw new SQLException("DB connection lost");
            if (missing) return null;
            return "record:" + id;
        }
    }

    static class Service {
        private final Repository repo;

        Service(Repository repo) {
            this.repo = repo;
        }

        String getById(String id, boolean simulateSqlFailure, boolean missing) {
            try {
                String r = repo.findById(id, simulateSqlFailure, missing);
                if (r == null) {
                    // Translate "null means not found" to a domain exception instead of returning null.
                    throw new NotFoundException("Item " + id + " not found");
                }
                return r;
            } catch (SQLException e) {
                // Translate low-level to a domain/layer-level unchecked exception, preserve cause.
                throw new DataAccessException("Failed to load id " + id, e);
            }
        }

        /*
        [T2] Multi-catch translation pattern:
        void unifiedLoad(...) {
            try {
                // code that might throw IOException or SQLException
            } catch (IOException | SQLException e) {
                throw new DataAccessException("Unified data failure", e);
            }
        }
        */
    }

    static class Controller {
        private final Service service;

        Controller(Service service) {
            this.service = service;
        }

        void handleFindById(String id, boolean simulateSqlFailure, boolean missing) {
            try {
                String value = service.getById(id, simulateSqlFailure, missing);
                print("Found: " + value);
            } catch (NotFoundException e) {
                // A domain exception you might choose to propagate as-is (propagation without translation).
                throw e;
            } catch (DataAccessException e) {
                // Boundary translation (service -> controller/API). Keep cause for diagnostics.
                throw new ServiceException("Cannot fulfill request getById(" + id + ")", e);
            }
        }
    }

    // Domain/layer exceptions (unchecked for convenience at upper layers)
    static class DataAccessException extends RuntimeException {
        DataAccessException(String message, Throwable cause) { super(message, cause); }
    }
    static class NotFoundException extends RuntimeException {
        NotFoundException(String message) { super(message); }
    }
    static class ServiceException extends RuntimeException {
        ServiceException(String message, Throwable cause) { super(message, cause); }
    }

    // [F1] FINALLY VS TRY-WITH-RESOURCES: SUPPRESSED VS OVERSHADOWED

    /*
    - try-with-resources (TWR) preserves secondary exceptions from close() as "suppressed" on the primary.
    - Manual finally can accidentally overshadow the primary exception if it throws too.
    */
    private static void suppressedDemo() {
        try {
            try (ExplodingResource r1 = new ExplodingResource("r1", true);
                 ExplodingResource r2 = new ExplodingResource("r2", true)) {
                throw new IllegalStateException("primary failure");
            }
        } catch (Exception e) {
            print("TWR primary: " + describe(e));
            for (Throwable sup : e.getSuppressed()) {
                print(" suppressed: " + describe(sup));
            }
        }
    }

    private static void overshadowingFinallyDemo() {
        try {
            overshadowingFinally();
        } catch (Exception e) {
            // The exception from finally overshadowed the primary one (pre-TWR pitfall)
            print("Finally overshadow demo caught: " + describe(e));
            if (e.getCause() != null) {
                print(" overshadow cause: " + describe(e.getCause()));
            }
        }
    }

    private static void overshadowingFinally() {
        ExplodingResource r = new ExplodingResource("legacy", true);
        try {
            throw new IllegalStateException("primary failure in try");
        } finally {
            try {
                r.close();
            } catch (Exception closeEx) {
                // Throwing here hides the original unless you attach it as suppressed manually.
                RuntimeException overshadow = new RuntimeException("close failure overshadowed", closeEx);
                // If you wanted to preserve the primary, you'd need to handle differently (e.g., remember and addSuppressed).
                throw overshadow;
            }
        }
    }

    static class ExplodingResource implements AutoCloseable {
        final String name;
        final boolean throwOnClose;
        ExplodingResource(String name, boolean throwOnClose) {
            this.name = name; this.throwOnClose = throwOnClose;
        }
        @Override public void close() throws IOException {
            if (throwOnClose) throw new IOException("close failed: " + name);
        }
    }

    // [THR] THREAD BOUNDARIES

    /*
    Exceptions do not propagate across threads. An uncaught exception terminates the thread and is delivered to its
    UncaughtExceptionHandler. The parent thread won't catch it unless the task framework captures it (e.g., futures).
    */
    private static void threadPropagationDemo() {
        Thread t = new Thread(() -> { throw new RuntimeException("boom in worker"); }, "worker-1");
        t.setUncaughtExceptionHandler((th, ex) ->
            print("Uncaught in thread '" + th.getName() + "': " + describe(ex)));
        t.start();
        try { t.join(); } catch (InterruptedException ignored) {}
    }

    // [TU] TUNNELING CHECKED TO UNCHECKED

    /*
    Tunneling converts checked exceptions to unchecked (e.g., UncheckedIOException). Use sparingly:
    - OK at module boundaries where exposing checked types would leak implementation details.
    - Avoid as a convenience inside the same layer; it can hide recoverable conditions.
    */
    private static void sniffTunnelingDemo() {
        try {
            tunnelingCaller();
        } catch (RuntimeException e) {
            print("Tunneling caught: " + describe(e));
            if (e.getCause() != null) print(" tunneled cause: " + describe(e.getCause()));
        }
    }

    private static void tunnelingCaller() {
        try {
            methodWithChecked();
        } catch (IOException e) {
            // Translate to unchecked for callers that cannot reasonably handle IO here.
            throw new UncheckedIOException("tunneled IO", e);
        }
    }

    private static void methodWithChecked() throws IOException {
        throw new IOException("checked fails");
    }

    /*
    Additional notes:
    - Always preserve the cause when translating (new X("context", cause)) or via initCause for legacy types.
    - Avoid swallowing exceptions (catch { } with no action). At least add context/log or rethrow.
    - Consider domain-specific exceptions at API boundaries to avoid leaking low-level details.
    - Precise rethrow requires the caught exception variable to be effectively final and rethrown unchanged.
    - Advanced: "sneaky throws" can rethrow checked exceptions without declaring; generally not recommended:

      @SuppressWarnings("unchecked")
      static <T extends Throwable> void sneakyThrow(Throwable t) throws T { throw (T) t; }
    */
}