package _02_02_constructors_and_initialization;

/*
Constructors & Initialization theory via executable examples and comments.

Topics covered:
- Default vs explicit constructors; inheritance of constructors (none)
- super(...) vs this(...); chaining rules; access modifiers
- Static and instance initialization order; blocks; field initializers
- Compile-time constants vs runtime constants; class initialization trigger
- Final and blank final; definite assignment; immutability
- Overridable method calls from constructors (pitfall)
- Exceptions in constructors; initializer blocks cannot declare throws
- Enums and their constructors; Records and their constructors
- Anonymous classes (no constructors) and instance initializer
- Constructor method references; array constructors
- Utility classes with private constructors
- Copy constructors; defensive copies
- Forward references; double-brace initialization (pitfall)
- Notes on deserialization/cloning constructing without normal constructors
*/
public class _01_Theory {

    public static void main(String[] args) {
        println("==== Initialization Order Demo ====");
        new InitOrderChild();

        println("\n==== Constructor Overloading/Chaining Demo ====");
        Chained c1 = new Chained();
        Chained c2 = new Chained("ABC", 42);
        println(c1.toString());
        println(c2.toString());

        println("\n==== super() vs this() Demo via inheritance ====");
        new Sub("data");

        println("\n==== Utility class with private constructor ====");
        println("MathUtil.sum(1,2) = " + MathUtil.sum(1, 2));

        println("\n==== Final and blank final Demo ====");
        new BlankFinalExample(7);

        println("\n==== Overridable method in constructor hazard ====");
        new DangerousSub();

        println("\n==== Constructor that throws checked exception ====");
        try {
            new ThrowsChecked("ok");
            new ThrowsChecked(null); // will throw
        } catch (java.io.IOException e) {
            println("Caught: " + e);
        }

        println("\n==== Static init and compile-time constants ====");
        // Reading a compile-time constant does NOT trigger class initialization
        println("CompileTimeConstants.COMPILE_TIME = " + CompileTimeConstants.COMPILE_TIME);
        // Force class initialization (prints static initializer)
        println("Force init via trigger(): " + CompileTimeConstants.trigger());
        // Reading a non-constant static field also triggers (if not already)
        println("CompileTimeConstants.RUNTIME = " + CompileTimeConstants.RUNTIME);

        println("\n==== Record constructors ====");
        PointRecord p = new PointRecord(3, 4);
        println(p.toString());

        println("\n==== Enum constructors ====");
        println(LogLevel.DEBUG.describe());

        println("\n==== Constructor method references ====");
        java.util.function.Supplier<Chained> ref = Chained::new;
        println("Created via ctor ref: " + ref.get());
        java.util.function.IntFunction<int[]> intArrCtor = int[]::new;
        int[] arr = intArrCtor.apply(3);
        println("int[] length via ctor ref: " + arr.length);

        println("\n==== Anonymous class instance initializer ====");
        Runnable r = new Runnable() {
            { println("Anonymous instance initializer runs"); } // runs before constructor body (there is no constructor here)
            @Override public void run() { println("Anonymous run"); }
        };
        r.run();

        println("\n==== Immutability via constructors ====");
        Email email = new Email("a@b.com", java.util.List.of("x", "y"));
        println(email.toString());

        println("\n==== Double-brace initialization (pitfall) ====");
        java.util.List<String> dbl = new java.util.ArrayList<String>() {{
            // Creates an anonymous subclass and runs this instance initializer
            add("one"); add("two");
        }};
        println("Double-brace created class: " + dbl.getClass().getName());

        println("\n==== Copy constructor ====");
        Email copy = new Email(email);
        println("Copied equals original components: " + copy.toString().equals(email.toString()));
    }

    static void println(String s) { System.out.println(s); }

    // 1) Initialization order: static of super -> static of sub; then instance creation:
    // For each class in hierarchy top-down: instance field initializers + instance init blocks -> constructor body
    static class InitOrderBase {
        static { System.out.println("InitOrderBase: static block 1"); }
        static int S1 = log("InitOrderBase: static field S1");
        static { System.out.println("InitOrderBase: static block 2"); }

        int i1 = log("InitOrderBase: instance field i1");
        { System.out.println("InitOrderBase: instance init block"); }

        InitOrderBase() { System.out.println("InitOrderBase: constructor"); }
    }

    static class InitOrderChild extends InitOrderBase {
        static { System.out.println("InitOrderChild: static block 1"); }
        static int CS1 = log("InitOrderChild: static field CS1");
        int ci1 = log("InitOrderChild: instance field ci1");
        { System.out.println("InitOrderChild: instance init block"); }
        InitOrderChild() {
            // implicit super() inserted here (after chaining completes if any)
            System.out.println("InitOrderChild: constructor");
        }
    }

    static int log(String msg) { System.out.println(msg); return 0; }

    // 2) Constructor overloading and chaining with this(...)
    static class Chained {
        private final String name;
        private final int value;

        public Chained() { this("default", 0); }
        public Chained(String name) { this(name, 0); }
        public Chained(int value) { this("default", value); }
        public Chained(String name, int value) {
            this.name = name; // use this.field to disambiguate from parameters
            this.value = value;
            // super(...) would be illegal here (must be first if used)
        }
        @Override public String toString() { return "Chained{name='" + name + "', value=" + value + "}"; }
    }

    // 3) super(...) vs this(...): first statement must be exactly one of them
    static class Super {
        final String base;
        Super() { this("base"); }
        Super(String base) { this.base = base; }
    }
    static class Sub extends Super {
        final String extra;
        Sub() {
            this("default-extra"); // would insert super() here if not chaining with this(...)
        }
        Sub(String extra) {
            super("custom-base"); // must be the first statement in this constructor
            this.extra = extra;
        }
        // The following would not compile:
        // Sub(String x, int y) {
        //    super(); this(x); // error: only one, and it must be first
        // }
    }

    // 4) Utility class with private constructor (prevents instantiation)
    static final class MathUtil {
        private MathUtil() { throw new AssertionError("No instances"); }
        static int sum(int a, int b) { return a + b; }
    }

    // 5) Final and blank final; every constructor must assign all final fields exactly once
    static class BlankFinalExample {
        private final int mustSet; // blank final
        private final java.util.List<String> unmodifiable;

        { /* instance initializer (cannot declare throws) */ }

        BlankFinalExample(int x) {
            this.mustSet = x; // definite assignment
            this.unmodifiable = java.util.Collections.emptyList();
        }

        BlankFinalExample() { this(0); } // delegation ensures finals are set
    }

    // 6) Overridable method call during construction (pitfall)
    static class DangerousBase {
        DangerousBase() { init(); } // calls overridable method
        void init() { System.out.println("DangerousBase.init"); }
    }
    static class DangerousSub extends DangerousBase {
        private final int len = computeLen(); // not yet initialized when super() calls init()
        private int computeLen() { System.out.println("computeLen"); return 42; }
        @Override void init() { System.out.println("DangerousSub.init: len=" + len); } // likely prints 0
    }

    // 7) Constructors can declare throws; initializer blocks must catch checked exceptions
    static class ThrowsChecked {
        private final String value;
        ThrowsChecked(String value) throws java.io.IOException {
            if (value == null) throw new java.io.IOException("value required");
            this.value = value;
        }
    }

    // 8) Static initialization and compile-time constants
    static class CompileTimeConstants {
        // Compile-time constant: primitive or String, static final, initialized by constant expression
        static final int COMPILE_TIME = 123; // may be inlined; reading it doesn't trigger <clinit>()

        static final Integer NOT_COMPILE_TIME = Integer.valueOf(123); // not a constant variable
        static final String RUNTIME = new String("abc"); // not a constant variable

        static { System.out.println("CompileTimeConstants: static initializer running"); }

        static String trigger() { return "trigger"; }
    }

    // 9) Records: immutable data carriers; fields are final; constructors can validate/normalize
    static record PointRecord(int x, int y) {
        // Compact constructor runs after implicit field assignment from parameters, but before body completes
        public PointRecord {
            if (x < 0 || y < 0) throw new IllegalArgumentException("coords must be >= 0");
            // this.x = ...; this.y = ...; // allowed in canonical constructor; in compact, parameters are already assigned
        }
        public PointRecord(int both) { this(both, both); } // must delegate to canonical
    }

    // 10) Enums: constructors are implicitly private; instances created during class initialization
    enum LogLevel {
        DEBUG(1), INFO(2), WARN(3), ERROR(4);
        final int severity;
        LogLevel(int severity) { this.severity = severity; }
        String describe() { return name() + "(" + severity + ")"; }
    }

    // 13) Immutability via constructors; defensive copies; final fields
    static final class Email {
        private final String from;
        private final java.util.List<String> to; // unmodifiable defensive copy

        Email(String from, java.util.List<String> to) {
            if (from == null || to == null) throw new IllegalArgumentException("null");
            this.from = from;
            this.to = java.util.List.copyOf(to); // defensive copy; unmodifiable
        }
        // Copy constructor
        Email(Email other) { this(other.from, other.to); }

        public String from() { return from; }
        public java.util.List<String> to() { return to; }
        @Override public String toString() { return "Email{from='" + from + "', to=" + to + "}"; }
    }

    // Additional theory notes (non-compiling examples are commented):
    /*
    // Forward reference rules:
    static class ForwardRefExamples {
        // static int a = b; // illegal forward reference (b not yet declared and not a constant)
        // static final int b = 10;

        // static final int c = d; // allowed if d is a constant variable
        // static final int d = 20;

        // int x = y; // illegal forward reference (y declared later)
        // int y = 1;
    }

    // Illegal uses:
    // class Bad {
    //     Bad() { super(); this(1); } // error: only one of super()/this(), and must be first
    // }

    // Anonymous classes cannot declare constructors; use instance initializer:
    Runnable anon = new Runnable() {
        { /* setup here */ /* }
        public void run() {}
    };

    // Double-brace initialization pitfall:
    // new ArrayList<String>() {{ add("x"); }}; // creates anonymous subclass, captures outer 'this', potential leaks

    // Deserialization and cloning can instantiate objects without calling normal constructors.
    // For deserialization, consider readObject/readResolve; for clone, implement Cloneable with care.
    */
}