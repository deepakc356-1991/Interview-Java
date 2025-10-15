package _02_06_abstract_classes_and_interfaces;

/**
 * _03_InterviewQA
 *
 * Abstract Classes & Interfaces - Interview Q&A (Basic → Intermediate → Advanced)
 *
 * This single file contains:
 * - Runnable demos that print observable behavior.
 * - Q&A explained in comments right next to compact examples.
 *
 * Compile and run:
 *   javac _02_06_abstract_classes_and_interfaces/_03_InterviewQA.java
 *   java _02_06_abstract_classes_and_interfaces._03_InterviewQA
 */
public class _03_InterviewQA {

    public static void main(String[] args) {
        section("Basics: Abstract class vs Interface");
        Basics.run();

        section("Constructors, state, access, and rules");
        ConstructorsAndRules.run();

        section("Interface default/static methods");
        DefaultAndStaticMethods.run();

        section("Multiple inheritance & diamond resolution");
        MultipleInheritanceAndConflicts.run();

        section("Anonymous classes and lambdas (functional interfaces)");
        AnonymousAndLambdas.run();

        section("Interface fields (constants) and pitfalls");
        InterfaceFieldsAndConstants.run();

        section("Static methods resolution");
        StaticMethodResolution.run();

        section("Design patterns: Template Method vs Strategy");
        Patterns.run();

        section("Generics with abstract classes and interfaces");
        Generics.run();

        section("Advanced topics and FAQs");
        AdvancedFAQ.run();

        System.out.println("\nDone.");
    }

    private static void section(String title) {
        System.out.println("\n=== " + title + " ===");
    }

    // ========================================
    // BASICS
    // ========================================
    static class Basics {
        /*
         Q: What is an abstract class?
         A: A class that cannot be instantiated directly and may contain abstract (unimplemented) methods.
            It can have state (fields), constructors, concrete methods, static methods, and final methods.

         Q: What is an interface?
         A: A contract of methods (implicitly abstract unless default/static). Cannot have instance state.
            Since Java 8, interfaces can have default and static methods; fields are public static final.

         Q: Key differences?
         A:
         - Abstract class: single inheritance; can hold state; can have constructors; can define access levels.
         - Interface: multiple inheritance of types; no instance state; methods are public by default (unless private for helpers in Java 9+).
        */

        // Abstract class example
        static abstract class Shape {
            abstract double area();               // abstract method
            String describe() { return "I am a " + getClass().getSimpleName(); } // concrete method
        }

        // Interface example
        interface Drawable {
            // 'public abstract' is implicit here
            void draw();
        }

        // Concrete class combining both
        static class Circle extends Shape implements Drawable {
            private final double radius;
            Circle(double radius) { this.radius = radius; }

            @Override double area() { return Math.PI * radius * radius; }
            @Override public void draw() {
                System.out.println("Drawing a circle of radius " + radius);
            }
        }

        static void run() {
            Circle c = new Circle(2.0);
            c.draw();
            System.out.println(c.describe() + " with area=" + c.area());

            // Q: Can you instantiate an abstract class?
            // A: No. The following would not compile:
            // Shape s = new Shape(); // compile error: Shape is abstract; cannot be instantiated
        }
    }

    // ========================================
    // CONSTRUCTORS, STATE, ACCESS, AND RULES
    // ========================================
    static class ConstructorsAndRules {
        /*
         Q: Can abstract classes have constructors?
         A: Yes. Used to initialize state when subclasses are created.

         Q: Can abstract class have final methods?
         A: Yes. Final methods cannot be overridden by subclasses.

         Q: Can abstract methods be private/static/final?
         A: No. It's a compile-time error. Abstract methods may be public, protected, or package-private.

         Q: Can interfaces have constructors?
         A: No.

         Q: Can interfaces have static initialization blocks?
         A: No. But constants may have initializing expressions and interfaces may have static methods.

         Q: Can an abstract class be final?
         A: No. 'abstract' and 'final' are contradictory for a class.

         Q: Visibility of constructors in abstract classes?
         A: Any (public/protected/package/private). Protected is common for frameworks/APIs.
        */

        static abstract class Appliance {
            protected final int voltage;

            protected Appliance(int voltage) { // constructors are allowed
                this.voltage = voltage;
            }

            final String plugType() {           // final method allowed in abstract classes
                return voltage + "V Plug";
            }

            abstract void start();              // abstract method

            // Illegal examples (won't compile):
            // private abstract void foo();     // abstract method cannot be private
            // static abstract void bar();      // abstract method cannot be static
            // final abstract void baz();       // abstract method cannot be final
            // synchronized abstract void qux();// abstract method cannot be synchronized
        }

        static class Toaster extends Appliance {
            Toaster() { super(120); }

            @Override void start() {
                System.out.println("Toaster started at " + voltage + "V");
            }

            // @Override String plugType() { ... } // compile error: cannot override final method
        }

        static void run() {
            Appliance a = new Toaster();
            a.start();
            System.out.println("Plug: " + a.plugType());

            // Q: Can we instantiate an abstract class via anonymous subclass?
            // A: Yes.
            Appliance custom = new Appliance(240) {
                @Override void start() { System.out.println("Anonymous appliance at " + voltage + "V"); }
            };
            custom.start();

            // Q: Can an abstract class have a static method or main?
            // A: Yes. See AdvancedFAQ.AbstractWithMain below.
        }
    }

    // ========================================
    // INTERFACE DEFAULT/STATIC METHODS
    // ========================================
    static class DefaultAndStaticMethods {
        /*
         Q: What are default methods in interfaces?
         A: Methods with a body in an interface (Java 8+) that provide a default implementation.

         Q: What are static methods in interfaces?
         A: Utility methods (Java 8+) that belong to the interface type and are not inherited by implementers.

         Q: Private methods in interfaces?
         A: Since Java 9, interfaces can have private methods to share code between default methods (helper methods).
            Shown below as commented example to avoid requiring Java 9 to compile this file.
        */

        interface Logger {
            default void log(String msg) {
                System.out.println(now() + " - " + sanitize(msg));
            }

            static String sanitize(String s) { return (s == null) ? "<null>" : s.trim(); }
            static String now() {
                return java.time.LocalTime.now().withNano(0).toString();
            }

            // Java 9+ helper (commented so Java 8 users can compile this file):
            // private String format(String msg) { return "[" + now() + "] " + sanitize(msg); }
        }

        static class PaymentService implements Logger {
            void pay(double amount) {
                log("Paying " + amount);
            }
        }

        static class CustomLoggerService implements Logger {
            @Override public void log(String msg) {
                System.out.println("CUSTOM LOG: " + msg);
            }
        }

        static void run() {
            PaymentService ps = new PaymentService();
            ps.pay(10.5);

            CustomLoggerService cs = new CustomLoggerService();
            cs.log("overridden default");

            // Static method: call via interface, not via instance
            System.out.println("Sanitized: " + Logger.sanitize("  ok  "));
        }
    }

    // ========================================
    // MULTIPLE INHERITANCE & DIAMOND RESOLUTION
    // ========================================
    static class MultipleInheritanceAndConflicts {
        /*
         Q: Does Java support multiple inheritance?
         A: For classes: No. For interfaces: Yes (a class can implement multiple interfaces).

         Q: Diamond problem with default methods?
         A: If two interfaces provide the same default method signature, implementing class must resolve it.

         Q: Rule precedence?
         A:
         - Class wins over interface default methods.
         - More specific interface wins over less specific when inheritance chain applies.
         - If two interfaces provide the same default method and neither is more specific, implementing class must override.

         Q: What if one interface has default method and another declares it abstract?
         A: The abstract declaration wins; implementer must provide an override.
        */

        interface A {
            default String hello() { return "Hello from A"; }
        }

        interface B {
            default String hello() { return "Hello from B"; }
        }

        static class AB implements A, B {
            @Override public String hello() {
                // Must disambiguate:
                return A.super.hello() + " & " + B.super.hello();
            }
        }

        static class Base {
            public String hello() { return "Hello from Base"; }
        }

        static class BaseAndA extends Base implements A {
            // Class method wins; no need to override hello().
        }

        interface X {
            default void ping() { System.out.print("X.ping "); }
        }
        interface Y {
            void ping(); // abstract
        }
        static class XY implements X, Y {
            @Override public void ping() {
                X.super.ping();        // reuse X's default, then add
                System.out.print("| XY.ping");
                System.out.println();
            }
        }

        static void run() {
            AB ab = new AB();
            System.out.println(ab.hello());

            BaseAndA ba = new BaseAndA();
            System.out.println(ba.hello()); // Base wins

            new XY().ping();
        }
    }

    // ========================================
    // ANONYMOUS CLASSES AND LAMBDAS
    // ========================================
    static class AnonymousAndLambdas {
        /*
         Q: Can you instantiate an interface?
         A: Not directly, but you can create:
            - An anonymous class that implements it.
            - A lambda if it's a functional interface (exactly one abstract method).

         Q: Do default methods count towards the single abstract method count?
         A: No. Only abstract methods count toward @FunctionalInterface validation.
        */

        @FunctionalInterface
        interface Calculator {
            int apply(int a, int b);

            default Calculator thenAdd(int x) {
                return (a, b) -> apply(a, b) + x;
            }
        }

        static void run() {
            Calculator anon = new Calculator() {
                @Override public int apply(int a, int b) { return a + b; }
            };
            System.out.println("Anonymous: " + anon.apply(2, 3));

            Calculator lambda = (a, b) -> a * b; // functional interface → lambda
            System.out.println("Lambda: " + lambda.apply(2, 3));
            System.out.println("Lambda.thenAdd(10): " + lambda.thenAdd(10).apply(2, 3));
        }
    }

    // ========================================
    // INTERFACE FIELDS (CONSTANTS) AND PITFALLS
    // ========================================
    static class InterfaceFieldsAndConstants {
        /*
         Q: Fields in interfaces?
         A: Implicitly public static final (constants). Must be initialized; cannot be reassigned.

         Q: Constant interface anti-pattern?
         A: Avoid using interfaces solely to hold constants; prefer a final class with public static final constants or enums.

         Q: Same constant names across interfaces?
         A: Use qualifying names to avoid ambiguity.
        */

        interface Config {
            int TIMEOUT_MS = 1_000; // public static final
        }

        interface I1 { int X = 1; }
        interface I2 { int X = 2; }

        static class C implements I1, I2 {
            int x1() { return I1.X; } // disambiguation
            int x2() { return I2.X; }
        }

        static void run() {
            System.out.println("Config.TIMEOUT_MS = " + Config.TIMEOUT_MS);
            C c = new C();
            System.out.println("I1.X=" + c.x1() + ", I2.X=" + c.x2());

            // Config.TIMEOUT_MS = 2000; // compile error: cannot assign a value to final variable
        }
    }

    // ========================================
    // STATIC METHODS RESOLUTION
    // ========================================
    static class StaticMethodResolution {
        /*
         Q: Are interface static methods inherited by implementing classes?
         A: No. They must be called via the interface name.

         Q: What if two interfaces have static methods with the same name?
         A: No conflict; just qualify with the interface name.
        */

        interface IA { static String who() { return "IA"; } }
        interface IB { static String who() { return "IB"; } }

        static class Demo implements IA, IB { }

        static void run() {
            System.out.println("IA.who() = " + IA.who());
            System.out.println("IB.who() = " + IB.who());
            // new Demo().who(); // compile error: static method not inherited
        }
    }

    // ========================================
    // DESIGN PATTERNS: TEMPLATE METHOD (abstract class) vs STRATEGY (interface)
    // ========================================
    static class Patterns {
        /*
         Template Method (abstract class): define a fixed algorithm skeleton; subclasses fill steps.
         Strategy (interface): define interchangeable behaviors; compose at runtime.
        */

        // Template Method
        static abstract class Task {
            public final void execute() {
                start();
                try {
                    doWork();
                } finally {
                    end();
                }
            }
            protected void start() { System.out.println("Start"); }
            protected abstract void doWork();
            protected void end() { System.out.println("End"); }
        }

        static class FileTask extends Task {
            @Override protected void doWork() { System.out.println("Processing file..."); }
        }

        // Strategy
        interface CompressionStrategy {
            byte[] compress(byte[] input);
        }
        static class NoCompression implements CompressionStrategy {
            public byte[] compress(byte[] input) { return input; }
        }
        static class DummyCompression implements CompressionStrategy {
            public byte[] compress(byte[] input) { return new byte[] { (byte) input.length }; }
        }

        static class Compressor {
            private final CompressionStrategy strategy;
            Compressor(CompressionStrategy strategy) { this.strategy = strategy; }
            byte[] compress(byte[] data) { return strategy.compress(data); }
        }

        static void run() {
            new FileTask().execute();

            Compressor c1 = new Compressor(new NoCompression());
            System.out.println("NoCompression: " + c1.compress(new byte[] {1,2,3}).length + " bytes");

            Compressor c2 = new Compressor(new DummyCompression());
            System.out.println("DummyCompression: " + c2.compress(new byte[] {1,2,3,4,5}).length + " bytes");
        }
    }

    // ========================================
    // GENERICS WITH ABSTRACT CLASSES AND INTERFACES
    // ========================================
    static class Generics {
        /*
         Q: Generic interfaces and abstract classes?
         A: Common in repository/services APIs. Abstract classes can share partial implementation.

         Q: Can abstract classes implement interfaces and omit implementations?
         A: Yes. Abstract class may leave interface methods abstract for subclasses to complete.
        */

        interface Repository<T> {
            void save(String key, T value);
            T find(String key);
            default boolean exists(String key) { return find(key) != null; }
        }

        static abstract class AbstractRepository<T> implements Repository<T> {
            private final java.util.Map<String, T> store = new java.util.HashMap<>();
            @Override public void save(String key, T value) { store.put(key, value); }
            @Override public T find(String key) { return store.get(key); }
            public int size() { return store.size(); }
        }

        static class UserRepo extends AbstractRepository<String> { }

        static void run() {
            UserRepo repo = new UserRepo();
            repo.save("alice", "Alice");
            System.out.println("Exists alice? " + repo.exists("alice") + ", size=" + repo.size());
        }
    }

    // ========================================
    // ADVANCED FAQ
    // ========================================
    static class AdvancedFAQ {
        /*
         Q: Can an interface extend a class?
         A: No. Interfaces can only extend other interfaces.

         Q: Can a class extend multiple classes?
         A: No (single inheritance for classes), but it can implement multiple interfaces.

         Q: Are interface methods public?
         A: Yes, implicitly public (except private helper methods in Java 9+). 'protected' not allowed.

         Q: Are nested types inside an interface static?
         A: Yes. Any nested type declared in an interface is implicitly public and static.

         Q: Can abstract/interface methods declare throws?
         A: Yes. Implementations may throw the same or narrower checked exceptions.

         Q: Covariant returns with overrides?
         A: Allowed. Overriding method may return a subtype of the original return type.

         Q: Sealed (Java 17+) abstract classes and interfaces?
         A: You can restrict which classes/interfaces can extend/implement them (see commented example below).
        */

        // Marker interface example
        interface Marker { /* no members (e.g., Serializable, Cloneable) */ }
        static class Marked implements Marker { }

        // Nested type inside interface is implicitly public static
        interface Container {
            class Nested {
                public static String name() { return "Container.Nested"; }
            }
        }

        // Multiple bounds on type parameters (class first, then interfaces)
        static <T extends Number & Comparable<T>> T max(T a, T b) {
            return a.compareTo(b) >= 0 ? a : b;
        }

        // Abstract class can have a static main
        static abstract class AbstractWithMain {
            public static void main(String[] args) {
                System.out.println("Abstract class can have static main.");
            }
        }

        // Interface can have a static main
        interface InterfaceWithMain {
            static void main(String[] args) {
                System.out.println("Interface can have static main (Java 8+).");
            }
        }

        static void run() {
            System.out.println("Marked instanceof Marker? " + (new Marked() instanceof Marker));
            System.out.println("Nested type in interface: " + Container.Nested.name());
            System.out.println("max(3, 5) = " + max(3, 5));
            System.out.println("max(2.5, 2.3) = " + max(2.5, 2.3));

            // Interface constructor? No:
            // interface Bad { Bad(); } // compile error: interfaces cannot have constructors

            // Sealed types (Java 17+) example (commented to keep Java 8 compatibility):
            // public sealed interface Vehicle permits Car, Truck {}
            // public final class Car implements Vehicle {}
            // public non-sealed class Truck implements Vehicle {}
        }
    }
}