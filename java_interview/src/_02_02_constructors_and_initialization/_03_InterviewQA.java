package _02_02_constructors_and_initialization;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

/*
Constructors & Initialization – Interview Q&A (Basic → Advanced)
This file is a self-contained cheat sheet with runnable mini-examples and concise Q&A.

BASICS
Q1: What is a constructor?
A: A special block used to initialize a new object; same name as the class; no return type.

Q2: Types of constructors?
A: Default (compiler-provided no-arg if none declared), no-arg (explicit), parameterized (overloaded).

Q3: When does the compiler give a default constructor?
A: Only if you declare no constructors at all.

Q4: Constructor vs method?
A: Constructors have no return type, are not inherited, cannot be abstract/static/final/synchronized/native, and are invoked only by new/this()/super().

Q5: Can constructors be private?
A: Yes (e.g., singletons, factory patterns, enums).

Q6: Can constructors be generic?
A: Yes. Type params can be declared on constructors separately from the class.

Q7: Are constructors inherited or overridden?
A: Neither. They can be overloaded.

Q8: What’s constructor chaining?
A: A constructor calling another constructor of the same class (this(...)) or the superclass (super(...)). Must be the first statement; you can’t call both.

Q9: Order of initialization?
A: Once per class load: static fields/blocks in textual order. On each object creation: superclass initialization, then subclass instance fields/initializer blocks in textual order, then subclass constructor body.

Q10: Can you do work before super()/this() in a constructor?
A: No. The first statement must be this(...) or super(...).

Q11: What are instance initializer blocks?
A: Blocks { ... } in a class; run for each object before the constructor body (after the super constructor). Rarely used.

Q12: Defaults for fields?
A: Primitives → 0/false; references → null. Local variables have no default and must be assigned before use.

Q13: How to initialize final fields?
A: At declaration or in every constructor. static final can be set at declaration or in a static initializer.

INTERMEDIATE
Q14: super() rules?
A: If omitted, super() (no-arg) is inserted. If the parent has no no-arg constructor, you must explicitly call a matching super(...).

Q15: Can we call a constructor from a method?
A: No—only constructors can call this()/super(). Methods must use new.

Q16: Overloading pitfalls?
A: Ambiguity with null/lambdas/varargs/autoboxing; be explicit to avoid surprises.

Q17: Static initialization pitfalls?
A: Exceptions there cause ExceptionInInitializerError; class may become unusable (NoClassDefFoundError on future loads).

Q18: Double-brace initialization?
A: Creates an anonymous inner class and captures outer; may leak, increase memory footprint, and harm serialization. Avoid in production.

Q19: Calling overridable methods in constructors?
A: Dangerous—subclass state may be uninitialized when the override runs.

Q20: “this” escape during construction?
A: Publishing “this” (e.g., registering listeners, starting threads) from constructor risks observing a partially constructed object.

Q21: Reflection and constructors?
A: Prefer getDeclaredConstructor(...).newInstance(...) over Class.newInstance() (which is deprecated and only calls no-arg).

Q22: Serialization and constructors?
A: For Serializable, no constructor of the class itself is called during deserialization; the first non-Serializable superclass’s no-arg constructor runs. Externalizable requires a public no-arg constructor.

Q23: clone() and constructors?
A: clone() does not call constructors.

Q24: Enum constructors?
A: Implicitly private; run once per constant at class initialization. You cannot new an enum.

Q25: Records (Java 16+)?
A: Provide a canonical constructor; compact constructors can validate and normalize components before assignment.

ADVANCED
Q26: Final fields and the memory model?
A: Properly set final fields are safely published after the constructor completes (with no this-escape).

Q27: Thread-safe lazy initialization using static holder?
A: Class initialization is thread-safe. Use Holder pattern for lazy singletons.

Q28: Order of evaluation of constructor arguments?
A: Left to right.

Q29: Instance initializer vs constructor for final fields?
A: Both can assign final fields; ensure each constructor path assigns them exactly once.

Q30: Can constructors be synchronized/abstract/final/static/native/strictfp?
A: No to all.

Q31: Can you have multiple instance initializer blocks?
A: Yes; they run in textual order.

Q32: Compile-time constants?
A: static final primitives/Strings initialized with constant expressions are inlined; beware of API evolution when recompiled vs runtime.

Q33: Inner classes and constructors?
A: Non-static inner class constructors capture an implicit outer reference; new Outer().new Inner() is required.

Q34: Dependency Injection needs?
A: Frameworks commonly require a no-arg constructor (not always, depending on reflection capabilities).

Below are small, focused examples.
*/
public class _03_InterviewQA {

    public static void main(String[] args) {
        System.out.println("== Order Of Initialization Demo ==");
        OrderOfInitializationDemo.run();

        System.out.println("\n== this()/super() Chaining Demo ==");
        ChainingDemo.run();

        System.out.println("\n== Final Fields Demo ==");
        FinalFieldDemo.run();

        System.out.println("\n== Overridable Call Pitfall Demo ==");
        OverridableInCtorPitfall.run();

        System.out.println("\n== Enum Constructor Demo ==");
        System.out.println(EnumWithCtor.ALPHA.describe());
        System.out.println(EnumWithCtor.BETA.describe());

        System.out.println("\n== Generic Constructor Demo ==");
        GenericConstructorDemo.run();

        System.out.println("\n== Static Holder Singleton Demo ==");
        System.out.println(HolderSingleton.get().id);

        System.out.println("\n== Constructor Reference Demo ==");
        ConstructorReferenceDemo.run();

        System.out.println("\n== Varargs & Overloading Demo ==");
        VarargsOverloadDemo.run();

        System.out.println("\n== Reflection Constructor Demo ==");
        ReflectionCtorDemo.run();

        System.out.println("\n== Serialization Constructor Note Demo ==");
        SerializationCtorNote.run();
    }

    // Q3/Q2/Q1: Default/no-arg/parameterized constructors
    static class DefaultConstructorDemo {
        // No explicit constructors → compiler provides a default no-arg constructor equivalent to:
        // DefaultConstructorDemo() {}
        int x; // defaults to 0
    }

    static class NoDefaultConstructorWhenAnyExists {
        // Parameterized constructor present → no implicit no-arg is generated.
        NoDefaultConstructorWhenAnyExists(int x) {}
        // Attempting: new NoDefaultConstructorWhenAnyExists() → compile error.
    }

    // Q8/Q10: this()/super() chaining and first-statement rule
    static class ChainingDemo {
        static class Base {
            Base() { System.out.println("Base()"); }
            Base(int x) { System.out.println("Base(int): " + x); }
        }
        static class Child extends Base {
            Child() {
                // Implicit: super();
                System.out.println("Child()");
            }
            Child(int x) {
                this(); // must be first
                System.out.println("Child(int): " + x);
            }
        }
        static class Child2 extends Base {
            Child2() { super(42); System.out.println("Child2()"); }
        }
        static void run() {
            new Child();
            new Child(7);
            new Child2();
        }
    }

    // Q9/Q11: Order of initialization
    static class OrderOfInitializationDemo {
        static class Super {
            static int S1 = log("Super.S1");
            static { log("Super.static block"); }
            int i1 = log("Super.i1");
            { log("Super.instance block"); }
            Super() { log("Super.<init>"); }
        }
        static class Sub extends Super {
            static int S2 = log("Sub.S2");
            static { log("Sub.static block"); }
            int i2 = log("Sub.i2");
            { log("Sub.instance block"); }
            Sub() { log("Sub.<init>"); }
        }
        static int log(String msg) { System.out.println(msg); return 0; }
        static void run() { new Sub(); }
    }

    // Q13/Q29: Final fields initialization
    static class FinalFieldDemo {
        final int a;              // blank final
        final int b = 2;          // initialized at declaration
        static final int C;       // static blank final
        static { C = 3; }         // or: static final int C = 3;

        FinalFieldDemo() { this.a = 1; }

        static void run() {
            FinalFieldDemo d = new FinalFieldDemo();
            System.out.println("a=" + d.a + ", b=" + d.b + ", C=" + C);
        }
    }

    // Q14: super() rules and missing no-arg in parent
    static class ParentNoNoArg {
        ParentNoNoArg(int x) {}
    }
    static class ChildNeedsExplicitSuper extends ParentNoNoArg {
        ChildNeedsExplicitSuper() { super(10); } // must call matching super(...)
    }

    // Q18: Double-brace initialization pitfalls
    static class DoubleBraceDemo {
        // Anti-pattern example (avoid in production):
        // Set<String> s = new HashSet<String>() {{
        //    add("A"); // this creates an anonymous inner class, captures outer, can leak
        // }};
    }

    // Q19: Overridable methods in constructors pitfall
    static class OverridableInCtorPitfall {
        static class Base {
            Base() { onInit(); } // BAD: calls overridable method
            void onInit() { System.out.println("Base.onInit"); }
        }
        static class Sub extends Base {
            String mustBeInitialized;
            Sub() { mustBeInitialized = "OK"; }
            @Override void onInit() {
                System.out.println("Sub.onInit; mustBeInitialized=" + mustBeInitialized);
            }
        }
        static void run() {
            // Output shows onInit runs before Sub() assigns mustBeInitialized
            new Sub();
        }
    }

    // Q20: this-escape in constructor
    static class ThisEscapeDemo {
        interface Listener { void onEvent(); }
        static class Publisher {
            final List<Listener> listeners = new ArrayList<>();
            void register(Listener l) { listeners.add(l); }
        }
        static class Escaping {
            Escaping(Publisher p) {
                // this escapes; other threads could see partially constructed state
                p.register(this::onEvent);
                // start a thread here also can publish this too early
            }
            void onEvent() {}
        }
    }

    // Q21: Reflection and constructors
    static class ReflectionCtorDemo {
        static class X { private X(int a) { System.out.println("X(" + a + ")"); } }
        static void run() {
            try {
                X x = X.class.getDeclaredConstructor(int.class).newInstance(5);
                // Class.newInstance() is deprecated and only calls no-arg; avoid it.
            } catch (ReflectiveOperationException e) {
                System.out.println("Reflection failed: " + e);
            }
        }
    }

    // Q22: Serialization and constructors
    static class SerializationCtorNote {
        static class NonSerialBase {
            NonSerialBase() { System.out.println("NonSerialBase()"); }
        }
        static class SerialChild extends NonSerialBase implements Serializable {
            int n = 7;
            SerialChild() { System.out.println("SerialChild()"); }
            private static final long serialVersionUID = 1L;
        }
        static void run() {
            // During deserialization of SerialChild:
            // - SerialChild's constructors will NOT run.
            // - The first non-serializable superclass (NonSerialBase) no-arg constructor WILL run.
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                oos.writeObject(new SerialChild());
            } catch (IOException e) { e.printStackTrace(); }

            try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
                Object o = ois.readObject();
                System.out.println("Deserialized: " + o.getClass().getSimpleName());
            } catch (IOException | ClassNotFoundException e) { e.printStackTrace(); }
        }
    }

    // Q17: Exception in static initialization
    static class StaticInitFailure {
        static {
            if (Boolean.getBoolean("causeFail")) {
                // If this runs, any reference to this class will throw ExceptionInInitializerError
                throw new RuntimeException("failing static init");
            }
        }
    }

    // Q24: Enum constructors
    enum EnumWithCtor {
        ALPHA(1), BETA(2);
        private final int code;
        EnumWithCtor(int code) { this.code = code; } // implicitly private
        String describe() { return name() + "(" + code + ")"; }
    }

    // Q27: Static holder singleton (thread-safe lazy init)
    static class HolderSingleton {
        final UUID id = UUID.randomUUID();
        private HolderSingleton() {}
        private static class Holder { static final HolderSingleton INST = new HolderSingleton(); }
        static HolderSingleton get() { return Holder.INST; }
    }

    // Q25: Records (Java 16+) – commented to keep file compatible with older JDKs
    /*
    record Point(int x, int y) {
        // Canonical constructor:
        public Point { // compact form
            if (x < 0 || y < 0) throw new IllegalArgumentException("negative");
        }
    }
    */

    // Q26: Final fields safely published (avoid this-escape)
    static class SafePublication {
        static class Holder {
            final int v;
            Holder(int v) { this.v = v; } // final ensures safe publication after construction
        }
        volatile Holder h;
        void publish(int v) { h = new Holder(v); } // volatile write publishes Holder safely
    }

    // Q28: Argument evaluation order
    static class ArgEvalOrder {
        static int f(String name, int v) { System.out.println(name + "=" + v); return v; }
        static class C {
            C(int a, int b, int c) { System.out.println("C(" + a + "," + b + "," + c + ")"); }
        }
        static void demo() { new C(f("a",1), f("b",2), f("c",3)); } // prints a,b,c in order
    }

    // Q31: Multiple instance initializer blocks
    static class MultipleInitializerBlocks {
        { System.out.println("block 1"); }
        int x = initX();
        { System.out.println("block 2"); }
        MultipleInitializerBlocks() { System.out.println("ctor"); }
        private int initX() { System.out.println("initX"); return 5; }
    }

    // Q32: Compile-time constants and inlining
    static class CompileTimeConstants {
        public static final int CONST = 42;           // inlined into clients
        public static final String STR = "hello";     // inlined
        public static final Integer BOXED = 100;      // not a constant variable → not inlined
        public static final int RUNTIME = new Random().nextInt(); // not inlined
    }

    // Q33: Inner classes capture outer in constructor
    static class Outer {
        int v = 10;
        class Inner {
            Inner() { System.out.println("Inner sees outer.v=" + v); }
        }
        static void demo() { new Outer().new Inner(); } // requires an Outer instance
    }

    // Q34: No-arg constructor usefulness (DI)
    static class Injectable {
        // Some frameworks prefer a public no-arg constructor.
        public Injectable() {}
        // Alternatively, they may use reflection to call parameterized constructors and inject dependencies.
    }

    // Q16: Overloading pitfalls; varargs
    static class VarargsOverloadDemo {
        static class V {
            V(Integer i) { System.out.println("Integer"); }
            V(int... arr) { System.out.println("int varargs"); }
            V(Object o) { System.out.println("Object"); }
        }
        static void run() {
            new V(5);              // prefers Integer (boxing) over varargs? Actually overloading resolution: int literal → picks V(Integer) vs V(int...) – the most specific applicable is V(Integer) (boxing) is more specific than varargs. Outputs "Integer".
            new V((int)5);         // still "Integer"
            new V(new int[]{1,2}); // "int varargs"
            new V((Object)null);   // "Object"
            // new V(null);       // ambiguous between V(Integer) and V(Object); would not compile without cast.
        }
    }

    // Q7: Overloading (not overriding) constructors
    static class OverloadedConstructors {
        OverloadedConstructors() { System.out.println("no-arg"); }
        OverloadedConstructors(int x) { System.out.println("int"); }
        OverloadedConstructors(String s) { System.out.println("String"); }
    }

    // Q6: Generic constructor
    static class GenericConstructorDemo {
        static class Box<T> {
            final T value;
            <U extends T> Box(U u) { this.value = u; } // constructor has its own type param
        }
        static void run() {
            Box<Number> b = new Box<>(10);
            System.out.println("Box holds: " + b.value);
        }
    }

    // Q12: Defaults vs locals
    static class DefaultsVsLocals {
        int x; // defaults to 0
        void demo() {
            // int y; System.out.println(y); // compile error: local variable not initialized
        }
    }

    // Q15: Methods cannot call this()/super()
    static class CtorCalls {
        CtorCalls() { this(1); }
        CtorCalls(int x) {}
        void m() {
            // this(2); // illegal here
            // super(); // illegal here
        }
    }

    // Q23: clone() does not call constructors
    static class CloneDemo implements Cloneable {
        int v;
        CloneDemo() { System.out.println("CloneDemo ctor"); }
        @Override protected Object clone() throws CloneNotSupportedException {
            return super.clone(); // no constructor call
        }
        static void demo() throws CloneNotSupportedException {
            CloneDemo a = new CloneDemo();
            CloneDemo b = (CloneDemo) a.clone();
            System.out.println("Cloned without running constructor");
        }
    }

    // Q17 again: ExceptionInInitializerError demonstration (not executed unless you touch it)
    static class TouchingStaticInit {
        static void tryTouch() {
            try {
                Class.forName(StaticInitFailure.class.getName());
                System.out.println("Touched StaticInitFailure without error (causeFail=false).");
            } catch (Throwable t) {
                System.out.println("Error touching StaticInitFailure: " + t);
            }
        }
    }

    // Q30: Disallowed modifiers on constructors
    static class DisallowedModifiers {
        // synchronized DisallowedModifiers() {} // illegal
        // static DisallowedModifiers() {}      // illegal
        // final DisallowedModifiers() {}       // illegal
        // abstract DisallowedModifiers();      // illegal
        // native DisallowedModifiers();        // illegal
        // strictfp DisallowedModifiers() {}    // illegal
    }

    // Additional demonstration: constructor reference (Supplier)
    static class ConstructorReferenceDemo {
        static class P {
            final String name;
            P() { this("default"); }
            P(String name) { this.name = name; }
        }
        static void run() {
            Supplier<P> sup = P::new; // refers to P()
            P p = sup.get();
            System.out.println("Constructed via method reference: " + p.name);
        }
    }

    // Bonus: Builder vs telescoping constructors (brief sketch)
    static class Telescoping {
        final int a; final int b; final int c;
        Telescoping(int a) { this(a, 0); }
        Telescoping(int a, int b) { this(a, b, 0); }
        Telescoping(int a, int b, int c) { this.a = a; this.b = b; this.c = c; }
    }
    static class BuilderStyle {
        final int a; final int b; final int c;
        private BuilderStyle(Builder b) { this.a = b.a; this.b = b.b; this.c = b.c; }
        static class Builder {
            int a; int b; int c;
            Builder a(int v) { a = v; return this; }
            Builder b(int v) { b = v; return this; }
            Builder c(int v) { c = v; return this; }
            BuilderStyle build() { return new BuilderStyle(this); }
        }
    }

    // Bonus: Varargs constructor best practices
    static class SafeVarargsExample {
        // @SafeVarargs // can be used on final/static/private methods/constructors in newer Java for warnings suppression (constructors: not applicable)
        SafeVarargsExample(List<String>... groups) {
            // Avoid storing groups directly due to heap pollution risks
            for (List<String> g : groups) { /* copy defensively if needed */ }
        }
    }

    // Bonus: DI frameworks might require a no-arg constructor even if not used directly.
    static class NoArgForDI {
        public NoArgForDI() {}
        public NoArgForDI(String dep) {}
    }

    // Bonus: Demonstrate compile-time constant inlining hazard (comment)
    /*
    // If an external client compiles against:
    public static final int API_VERSION = 1; // inlined into client
    // Later you change to 2 without recompiling the client; running old client with new library may still see 1 due to inlining.
    */
}