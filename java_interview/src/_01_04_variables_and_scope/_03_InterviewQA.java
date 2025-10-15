package _01_04_variables_and_scope;

/**
 * Variables & Scope Interview Q&A (Basic ➜ Intermediate ➜ Advanced)
 *
 * Read the Q&A inline comments. Run main() to see short demos and outputs.
 */
public class _03_InterviewQA {

    public static void main(String[] args) {
        System.out.println("=== Variables & Scope Interview Q&A Demos ===");

        basicKindsAndDefaults();
        scopeRules();
        shadowingAndHiding();
        staticVsInstance();
        finalAndImmutability();
        passingParameters();
        initializationOrder();
        tryWithResourcesScope();
        loopsScope();
        patternMatchingScope();
        interfaceFieldFacts();
        accessModifiersVsScopeNotes();
        concurrencyAndMemoryModelNotes();

        System.out.println("=== Done ===");
    }

    /**
     * Q: What are the kinds of variables in Java?
     * A:
     * - By storage: primitive (8 types) and reference.
     * - By declaration place: local variables, parameters, instance fields, static (class) fields.
     *
     * Q: Default values?
     * A:
     * - Local variables/parameters: no default; must be explicitly initialized before use (compile-time check).
     * - Fields (instance/static): default to type defaults (0, 0.0, false, '\u0000', null).
     */
    static void basicKindsAndDefaults() {
        System.out.println("\n-- Kinds & Defaults --");
        DefaultsDemo dd = new DefaultsDemo();
        System.out.println("Instance int default: " + dd.instanceInt);   // 0
        System.out.println("Instance ref default: " + dd.instanceRef);   // null
        System.out.println("Static int default: "   + DefaultsDemo.staticInt); // 0

        // Local variable: must initialize before use (uncommenting next lines would not compile)
        // int local;
        // System.out.println(local); // compile error: variable local might not have been initialized
    }

    static class DefaultsDemo {
        int instanceInt;        // default 0
        String instanceRef;     // default null
        static int staticInt;   // default 0
    }

    /**
     * Q: What is "scope"?
     * A: The region of code where a name (variable/parameter) is visible and usable.
     *
     * Q: Block scope?
     * A: A variable declared inside a block {...} is visible only within that block and its nested blocks.
     */
    static void scopeRules() {
        System.out.println("\n-- Scope Rules --");
        int outer = 10;
        {
            int inner = 20;
            System.out.println("outer in inner block: " + outer);
            System.out.println("inner in inner block: " + inner);
        }
        System.out.println("outer after block: " + outer);
        // System.out.println(inner); // compile error: cannot find symbol
    }

    /**
     * Q: What is variable shadowing?
     * A: A declaration with the same name in a nested scope makes the outer declaration inaccessible by that name.
     *
     * Q: Field hiding vs shadowing?
     * A:
     * - Local/parameter shadowing: a local/parameter with same name as a field shadows it; use this.field or ClassName.field to refer to the hidden one.
     * - Field hiding in subclass: a field in a subclass with same name as a field in superclass hides it (name resolution by static/declared type).
     */
    static void shadowingAndHiding() {
        System.out.println("\n-- Shadowing & Hiding --");
        Shadowing s = new Shadowing();
        s.demo(99);

        Base b = new Base();
        Base bs = new Sub(); // reference typed as Base
        Sub sub = new Sub();
        System.out.println("Base.x via Base ref: " + b.x);     // 1
        System.out.println("Base.x via Base-typed Sub ref: " + bs.x); // 1 (field resolved by declared type)
        System.out.println("Sub.x via Sub ref: " + sub.x);     // 2
    }

    static class Shadowing {
        int x = 10;
        void demo(int x) { // parameter x shadows field x
            System.out.println("parameter x (shadows field): " + x);         // 99
            System.out.println("field x via this.x: " + this.x);             // 10
            {
                int xBlock = 77; // new name, no conflict
                System.out.println("block var: " + xBlock);
            }
        }
    }

    static class Base {
        int x = 1;
    }
    static class Sub extends Base {
        int x = 2; // hides Base.x (no override for fields)
    }

    /**
     * Q: Difference between instance and static variables?
     * A:
     * - Instance fields: one per object; need object to access (this).
     * - Static fields: one per class; shared across all instances; can access via ClassName.field.
     *
     * Q: When use static?
     * A: Class-level constants/caches/counters/utility state. Beware of unwanted retention (memory leaks).
     */
    static void staticVsInstance() {
        System.out.println("\n-- Static vs Instance --");
        Counter c1 = new Counter();
        Counter c2 = new Counter();
        c1.increment();
        c2.increment();
        c2.increment();
        System.out.println("c1 instance count: " + c1.instanceCount); // 1
        System.out.println("c2 instance count: " + c2.instanceCount); // 2
        System.out.println("shared static count: " + Counter.staticCount); // 3
    }

    static class Counter {
        int instanceCount;
        static int staticCount;
        void increment() {
            instanceCount++;
            staticCount++;
        }
    }

    /**
     * Q: What does 'final' mean on variables?
     * A:
     * - Final primitive: value cannot be reassigned.
     * - Final reference: reference cannot be reassigned, but the object's state can still mutate (unless the object is immutable).
     * - Blank final: final field without initializer; must be assigned exactly once in constructor or instance initializer.
     * - static final typically used for constants (UPPER_SNAKE_CASE).
     * - 'Effectively final': a local variable not declared final but never reassigned; required for lambda/anonymous class capture.
     *
     * Q: Does final make objects immutable?
     * A: No. It prevents reference reassignment, not object mutation.
     */
    static void finalAndImmutability() {
        System.out.println("\n-- final & Immutability --");
        FinalDemo fd1 = new FinalDemo(42);
        FinalDemo fd2 = new FinalDemo();
        System.out.println("fd1.id: " + fd1.id + ", fd2.id: " + fd2.id);
        fd1.holder.value = 999; // allowed (mutating state through final reference)
        System.out.println("Mutated holder through final ref: " + fd1.holder.value);

        // Effectively final capture in lambda:
        int base = 10; // effectively final (we do not reassign it)
        Runnable r = () -> System.out.println("Captured effectively final: " + base);
        r.run();
        // base = 20; // would break "effectively final" rule and cause compile error if above lambda uses it

        // 'var' cannot be used for fields; only local vars. (shown below in var notes)
    }

    static class FinalDemo {
        final int id;                   // blank final, set once
        static final double PI = 3.141592653589793; // constant
        final Holder holder = new Holder(123);      // final reference, mutable object

        FinalDemo() {
            this.id = 0; // blank final set in constructor
        }
        FinalDemo(int id) {
            this.id = id;
        }
    }
    static class Holder {
        int value;
        Holder(int value) { this.value = value; }
    }

    /**
     * Q: Is Java pass-by-value or pass-by-reference?
     * A: Always pass-by-value. The "value" of a reference is the reference itself (an address-like value).
     * - Reassigning the parameter doesn't affect the caller's reference.
     * - Mutating the object through the reference affects the shared object.
     */
    static void passingParameters() {
        System.out.println("\n-- Parameter Passing --");
        int a = 5;
        bumpPrimitive(a);
        System.out.println("primitive after call: " + a); // 5

        Person p = new Person("Ana");
        mutatePerson(p);
        System.out.println("person after mutate: " + p.name); // "Ana+"

        reassignPerson(p);
        System.out.println("person after reassign attempt: " + p.name); // still "Ana+"
    }

    static void bumpPrimitive(int x) { x++; }
    static void mutatePerson(Person p) { p.name = p.name + "+"; }
    static void reassignPerson(Person p) { p = new Person("Bob"); } // only reassigns local copy

    static class Person {
        String name;
        Person(String name) { this.name = name; }
    }

    /**
     * Q: Order of initialization?
     * A:
     * - Static: static fields ➜ static blocks (in textual order), once per class load.
     * - Instance: instance fields ➜ instance initializer blocks ➜ constructor (in textual order), per object creation.
     * - Superclass initialization runs before subclass initialization.
     */
    static void initializationOrder() {
        System.out.println("\n-- Initialization Order --");
        new InitOrder();
    }

    static class InitOrder {
        static int s1 = printS("static field s1");
        static { printS("static block #1"); }
        static int s2 = printS("static field s2");

        int i1 = printI("instance field i1");
        { printI("instance init block #1"); }
        int i2 = printI("instance field i2");

        InitOrder() {
            printI("constructor");
        }

        static int printS(String msg) { System.out.println("InitOrder: " + msg); return 0; }
        int printI(String msg) { System.out.println("InitOrder: " + msg); return 0; }
    }

    /**
     * Q: What is the scope of variables in try-with-resources and catch?
     * A:
     * - Resource variables are implicitly final (effectively final) and scoped to the try block.
     * - Catch parameter is scoped to the catch block.
     */
    static void tryWithResourcesScope() {
        System.out.println("\n-- try-with-resources Scope --");
        try (MyRes res = new MyRes("R1")) {
            res.say();
            // res = new MyRes("R2"); // compile error: resource is final
        } catch (Exception e) {
            System.out.println("caught: " + e.getMessage());
        }
        // System.out.println(res); // compile error: res out of scope
    }

    static class MyRes implements AutoCloseable {
        final String id;
        MyRes(String id) { this.id = id; }
        void say() { System.out.println("MyRes " + id + " says hi"); }
        @Override public void close() { System.out.println("MyRes " + id + " closed"); }
    }

    /**
     * Q: Loop variable scope?
     * A:
     * - Variables declared in for/while blocks exist only within the loop's body scope.
     * - Enhanced for-loop iteration variable is scoped to the loop.
     * - Avoid reusing the same variable name in nested scopes to prevent confusion.
     */
    static void loopsScope() {
        System.out.println("\n-- Loops Scope --");
        for (int i = 0; i < 2; i++) {
            int inside = i * 10;
            System.out.print("i=" + i + " inside=" + inside + " | ");
        }
        System.out.println();
        // System.out.println(i);      // compile error: i not in scope
        // System.out.println(inside); // compile error: inside not in scope
    }

    /**
     * Q: Pattern variables with instanceof (pattern matching) scope?
     * A:
     * - With 'if (obj instanceof String s)', 's' is only in scope where the pattern is true.
     * - The scope excludes else branches and places where the flow might not have proven the type.
     */
    static void patternMatchingScope() {
        System.out.println("\n-- Pattern Matching Scope --");
        Object obj = "hello";
        if (obj instanceof String s) {
            System.out.println("length = " + s.length()); // s in scope
        } else {
            // s not in scope here
        }
        // s not in scope here
    }

    /**
     * Q: What about interface fields?
     * A:
     * - All fields declared in interfaces are implicitly public static final.
     * - They must be initialized with a constant value expression.
     */
    static void interfaceFieldFacts() {
        System.out.println("\n-- Interface Field Facts --");
        System.out.println("InterfaceConst.VERSION = " + InterfaceConst.VERSION);
        // InterfaceConst.VERSION = 2; // compile error: cannot assign a value to final variable
    }

    interface InterfaceConst {
        int VERSION = 1; // implicitly public static final
    }

    /**
     * Q: Are access modifiers (public/protected/default/private) the same as scope?
     * A:
     * - No. Access modifiers control visibility across types/packages; scope controls visibility within lexical blocks.
     * - A private field still has full scope inside the declaring class.
     */
    static void accessModifiersVsScopeNotes() {
        System.out.println("\n-- Access Modifiers vs Scope Notes --");
        // See comments above. No runtime demo needed.
    }

    /**
     * Advanced Notes: Java Memory Model and Threading related to variables
     *
     * Q: What does 'volatile' guarantee?
     * A:
     * - Visibility and ordering for reads/writes of that variable (happens-before semantics).
     * - Not atomicity for compound actions (e.g., count++ not atomic).
     *
     * Q: When to use Atomic* classes?
     * A: For atomic operations (increment, CAS) on shared state without locks.
     *
     * Q: final field semantics?
     * A:
     * - After a constructor successfully completes, properly constructed objects guarantee visibility of final fields to other threads.
     *
     * Q: Safe publication?
     * A:
     * - Publish objects via volatile fields, final fields, immutable containers, thread-safe collections, or proper synchronization.
     *
     * Q: Double-checked locking (DCL)?
     * A:
     * - Use 'volatile' on the reference for correct DCL.
     *
     * Note: Code below is illustrative only; not executing concurrent races.
     */
    static void concurrencyAndMemoryModelNotes() {
        System.out.println("\n-- Concurrency & Memory Model Notes --");
        // Example (non-atomic increment):
        // volatile int v; // v++ is not atomic even if v is volatile.
        // Use AtomicInteger:
        // AtomicInteger ai = new AtomicInteger();
        // ai.incrementAndGet();

        // Double-checked locking pattern:
        // class Singleton {
        //   private static volatile Singleton INSTANCE;
        //   static Singleton get() {
        //     if (INSTANCE == null) {
        //       synchronized (Singleton.class) {
        //         if (INSTANCE == null) INSTANCE = new Singleton();
        //       }
        //     }
        //     return INSTANCE;
        //   }
        // }
    }

    /* Additional quick Q&A notes (no code):
     *
     * Q: Can 'var' be used for fields, method parameters, or return types?
     * A: No. 'var' is only for local variables (Java 10+). You can use 'var' in lambda parameters (Java 11+) but then all lambda params must use 'var'.
     *
     * Q: Can 'var' be initialized with 'null'?
     * A: No. The compiler cannot infer the type from null alone.
     *
     * Q: Are local variables initialized to defaults?
     * A: No. You must assign before use (compile-time check).
     *
     * Q: Does 'final' on a field make the class immutable?
     * A: No. Immutability requires all fields to be final and all referenced objects to be deeply immutable, with no mutators and defensive copies.
     *
     * Q: What are "blank final" and "effectively final" differences?
     * A: Blank final is a final variable assigned later exactly once (e.g., in constructor). Effectively final is a local variable not declared final but never reassigned.
     *
     * Q: Memory leak via static?
     * A: Long-lived static references can retain large object graphs; null out caches if needed or use weak/soft refs as appropriate.
     *
     * Q: Parameter shadowing best practice?
     * A: Prefer meaningful distinct names or use 'this.field' to avoid ambiguity.
     */

}