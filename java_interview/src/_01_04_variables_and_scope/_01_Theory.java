package _01_04_variables_and_scope;

/*
Variables & Scope — Theory (with runnable examples)

Core concepts:
- Variable kinds:
  - Local variables: declared inside methods/blocks. No default values; must be definitely assigned before use.
  - Parameters: local to the method signature; behave like locals.
  - Fields (a.k.a. members):
    - Instance fields: one per object. Default-initialized.
    - Static (class) fields: one per class. Default-initialized.
  - Constants: final variables assigned exactly once; commonly static final for true constants.

- Types:
  - Primitives: byte, short, int, long, float, double, char, boolean.
  - Reference types: classes, interfaces, arrays, enums, etc. Hold references to objects (or null).

- Scope vs lifetime:
  - Scope: where a name is visible (a compile-time concept).
  - Lifetime: how long the data can live (a runtime concept).
    - Locals: live while the stack frame is active.
    - Instance fields: live while the object is reachable.
    - Static fields: live while the class is loaded.

- Default values (for fields only):
  - Numeric primitives: 0/0.0; char: '\u0000'; boolean: false; references: null.
  - Locals/parameters have no defaults; you must initialize before using them.

- Type inference (var):
  - Only for local variables (Java 10+), not for fields/parameters.
  - Requires an initializer; cannot be null-only.

- Conversions:
  - Widening (safe) vs narrowing (needs cast, may lose data).
  - Numeric promotion in expressions (byte/short/char -> int).
  - Overflow wraps around; no automatic errors.

- Scope rules:
  - Inner scope can see outer names unless shadowed.
  - Shadowing: a local/parameter/inner field with same name hides an outer field/variable.
  - Hiding: a static field in a subclass hides a static field of the same name in the superclass.
  - Block scope includes if/for/while/try/catch/switch blocks.
  - Switch cases share one block unless you add braces.

- Final and effectively final:
  - final variable: assigned once.
  - effectively final: not declared final but never reassigned. Required when captured by lambdas/anonymous classes.

- Passing arguments:
  - Java is pass-by-value. For references, the reference value is copied. You can mutate the object via the reference but reassigning the parameter doesn't affect the caller.

- Initialization order:
  - For a class: static fields/blocks run in declaration order when the class is initialized.
  - For an object: instance fields/blocks run in declaration order, then the constructor.

- Access control (public/protected/(package)/private) is about visibility across classes/packages; it's separate from lexical scope (but often related when accessing fields).

Notes:
- Some comments show lines that won't compile; they're intentionally commented to illustrate rules.
- Requires Java 11+ to compile as-is (uses 'var' and 'var' in lambda parameters).
*/
public class _01_Theory {

    // -------- Static/class variables (one per class) --------
    static int staticCounter;                 // default 0
    static final double PI = 3.141592653589793;
    static final int CONST_COMPUTED;          // static blank final, must be set exactly once

    static {
        System.out.println("[static init] staticCounter=" + staticCounter + " (default 0)");
        CONST_COMPUTED = 42;
        // staticCounter and CONST_COMPUTED are in class scope; lifetime is while class is loaded.
    }

    // -------- Instance variables (one per object) --------
    int instanceCounter;   // default 0
    String name;           // default null
    boolean active;        // default false
    final int id;          // blank final instance field; must be initialized in ctor or instance initializer

    // Instance initializer block (runs before constructor, after default init)
    {
        System.out.println("[instance init] before constructor; name=" + name + ", active=" + active + ", instanceCounter=" + instanceCounter);
    }

    public _01_Theory() {
        this(0);
    }

    public _01_Theory(int id) {
        this.id = id; // assign blank final
        System.out.println("[constructor] id=" + this.id + ", name=" + this.name + ", active=" + this.active + ", instanceCounter=" + this.instanceCounter);
    }

    void variableBasics() {
        // ---- Declaration, initialization, assignment ----
        int a;             // declared, not initialized
        a = 10;            // now initialized
        int b = 20;        // declaration + initialization

        // Local variable type inference (Java 10+)
        var inferredInt = 123; // 'var' only for locals; must have initializer; cannot be just 'null'

        // ---- Primitive types and literals ----
        byte by = 1;
        short sh = 2;
        int in = 3;
        long lo = 4L;
        float fl = 1.5f;
        double db = 2.5;
        char ch = 'A';
        boolean bool = true;

        // Numeric literals with underscores and different bases
        int million = 1_000_000;
        int binary = 0b1010_1010;
        int hex = 0xFF_EC_DE_5E;
        long creditCard = 1234_5678_9012_3456L;

        // ---- Widening vs narrowing conversions ----
        long fromInt = in;         // widening OK
        int fromLong = (int) lo;   // narrowing needs cast; may lose data

        // ---- Numeric promotion and overflow ----
        byte b1 = 1, b2 = 2;
        int sum = b1 + b2;                // promoted to int
        byte sumByte = (byte) (b1 + b2);  // explicit cast
        int max = Integer.MAX_VALUE;
        int overflow = max + 1;           // overflow wraps
        System.out.println("overflow demo: " + max + " + 1 => " + overflow);

        // ---- References and null ----
        String s = "hello";
        String t = s;            // two references to same object
        s = s.toUpperCase();     // reassign s to a new String
        String u = null;
        // System.out.println(u.length()); // NPE if executed

        // ---- Default values (fields) vs locals ----
        // int notInitialized; System.out.println(notInitialized); // compile error: not initialized
        System.out.println("Field defaults via 'this': name=" + name + ", active=" + active + ", instanceCounter=" + instanceCounter);

        // ---- final and effectively final ----
        final int FINAL_LOCAL = 99;
        // FINAL_LOCAL = 100; // compile error: cannot assign a value to final variable

        int effectivelyFinal = 77; // remains effectively final if never reassigned
        Runnable r = () -> System.out.println("captured effectivelyFinal = " + effectivelyFinal);
        r.run();
        Runnable anon = new Runnable() {
            @Override public void run() {
                System.out.println("anonymous class captured = " + effectivelyFinal);
            }
        };
        anon.run();
        // effectivelyFinal++; // would break effective finality -> compile error if uncommented

        final StringBuilder finalRef = new StringBuilder("X");
        finalRef.append("Y");           // allowed: object can mutate
        // finalRef = new StringBuilder("Z"); // not allowed: cannot reassign final reference

        // ---- Pass-by-value semantics ----
        int num = 5;
        StringBuilder sb = new StringBuilder("A");
        mutate(num, sb);
        System.out.println("after mutate: num=" + num + " (unchanged), sb=" + sb + " (mutated)");

        // ---- Shadowing and 'this' ----
        int instanceCounter = 999; // shadows the field 'instanceCounter'
        System.out.println("shadowing local instanceCounter=" + instanceCounter + ", field instanceCounter=" + this.instanceCounter);

        // ---- Block scope ----
        if (true) {
            int blockVar = 1;
            System.out.println("blockVar=" + blockVar);
        }
        // System.out.println(blockVar); // out of scope

        // ---- Loop scope ----
        for (int i = 0; i < 2; i++) {
            System.out.println("for i=" + i);
        }
        // System.out.println(i); // not visible here

        for (var ch2 : new char[] {'x','y'}) {
            System.out.println("enhanced for ch2=" + ch2);
        }

        // ---- try-with-resources scope ----
        try (java.io.StringReader reader = new java.io.StringReader("abc")) {
            System.out.println("reader ready: " + (reader != null));
            // reader = new java.io.StringReader("def"); // not allowed: resource is implicitly final
        } // reader closed and out of scope here

        // ---- catch variable scope ----
        try {
            Integer.parseInt("NaN");
        } catch (NumberFormatException ex) {
            System.out.println("caught: " + ex.getMessage());
        }
        // System.out.println(ex); // not visible here

        // ---- 'var' limitations ----
        // var x; // compile error: cannot use 'var' without initializer
        // var nullVar = null; // compile error: cannot infer type from null
        // 'var' not allowed for fields or parameters
    }

    static void mutate(int x, StringBuilder builder) {
        x += 10;                  // changes only the local copy of the primitive
        builder.append("B");      // mutates object via the copied reference
        builder = new StringBuilder("C"); // reassigns local reference only; caller unaffected
    }

    // -------- Nested classes to illustrate shadowing/hiding and 'this' --------
    class Inner {
        int instanceCounter = 12345; // shadows outer field
        void demo() {
            int instanceCounter = 7; // shadows both inner field and outer field
            System.out.println("local instanceCounter=" + instanceCounter);
            System.out.println("inner field instanceCounter=" + this.instanceCounter);
            System.out.println("outer field instanceCounter=" + _01_Theory.this.instanceCounter);
        }
    }

    static class StaticNested {
        static int staticNestedVar = 1; // class variable within nested class
        void demo() {
            System.out.println("StaticNested.demo: staticCounter=" + staticCounter);
            // Cannot access instance members (like 'name') without an instance of _01_Theory
        }
    }

    // -------- Static hiding vs instance shadowing --------
    static class Base {
        static String type = "Base";
        int value = 1;
        void show() {
            System.out.println("Base.show: type=" + type + ", value=" + value);
        }
    }
    static class Derived extends Base {
        static String type = "Derived"; // hides Base.type (static hiding)
        int value = 2;                  // shadows Base.value (instance shadowing)
        @Override
        void show() {
            System.out.println("Derived.show: type=" + type + ", Base.type=" + Base.type + ", value=" + value + ", super.value=" + super.value);
        }
    }

    // -------- Switch block scope --------
    static void switchScope(int n) {
        switch (n) {
            case 1:
                int x = 10; // declared in switch block (visible across cases unless a new block is created)
                System.out.println("case 1 x=" + x);
                break;
            case 2:
                // int x = 20; // compile error: x already defined in the switch block
                {
                    int x2 = 20; // new block creates a new scope
                    System.out.println("case 2 x2=" + x2);
                }
                break;
            default:
                System.out.println("default");
        }
    }

    // -------- Method and parameter scope --------
    static int methodWithParams(int p1, final int p2) {
        // p1 and p2 exist only within this method; p2 cannot be reassigned
        int local = p1 + p2;
        return local;
    }

    // -------- Initialization order and defaults --------
    static void printDefaultsAndOrder() {
        System.out.println("staticCounter=" + staticCounter + ", CONST_COMPUTED=" + CONST_COMPUTED + ", PI=" + PI);
        _01_Theory obj = new _01_Theory(101);
        System.out.println("obj defaults: name=" + obj.name + ", active=" + obj.active + ", instanceCounter=" + obj.instanceCounter + ", id=" + obj.id);
    }

    // -------- Lifetime and GC (reachability) --------
    static void lifetimeAndGC() {
        Object local = new Object(); // becomes eligible for GC when out of scope or when reference is nulled
        local = null; // now eligible sooner (no strong reference)
        // GC is non-deterministic; this just illustrates reachability vs scope.
    }

    public static void main(String[] args) {
        System.out.println("=== Variables & Scope Theory Demo ===");
        printDefaultsAndOrder();

        _01_Theory t = new _01_Theory(7);
        t.variableBasics();

        Inner in = t.new Inner();
        in.demo();

        StaticNested sn = new StaticNested();
        sn.demo();

        Base b = new Base();
        Derived d = new Derived();
        b.show();
        d.show();
        System.out.println("Accessing hidden static: Base.type=" + Base.type + ", Derived.type=" + Derived.type);

        switchScope(1);
        switchScope(2);

        System.out.println("methodWithParams(3,4)=" + methodWithParams(3,4));

        lifetimeAndGC();

        // 'var' in loops and 'var' in lambda parameters (Java 11+) examples
        for (var i = 0; i < 2; i++) {
            var msg = "loop-" + i; // block scope
            System.out.println(msg);
        }
        java.util.function.Function<Integer, Integer> f = (var x) -> x + 1;
        System.out.println("lambda var demo: f.apply(5)=" + f.apply(5));

        // Note: 'volatile' and 'transient' are field modifiers affecting memory visibility and serialization respectively; they do not change scope.
    }
}