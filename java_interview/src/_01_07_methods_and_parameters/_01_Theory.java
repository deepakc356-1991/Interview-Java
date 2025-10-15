package _01_07_methods_and_parameters;

import javax.swing.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/*
 Methods & Parameters — Core Theory (inline notes)

 - Method signature: name + parameter types (order matters). Return type is not part of the signature.
 - Components: modifiers (public/private/protected, static/final/abstract/synchronized/native/strictfp), annotations, type parameters, return type, name, parameters, throws clause, body.
 - Return types: any type or void. Use 'return' to provide a value or early-exit.
 - Parameters vs arguments: parameters are placeholders in the method declaration; arguments are the actual values at call site.
 - Passing style: Java is always pass-by-value (including references). For objects, the reference value is copied; you can mutate the object but reassigning the parameter doesn't affect the caller.
 - Primitives vs references: primitives hold values; references point to objects/arrays.
 - Overloading: same name, different parameter lists. Resolution prefers (in order) exact match > widening > boxing > varargs. Return type alone cannot differentiate overloads.
 - Overriding: subclass replaces superclass method. Rules: same name/parameters; return type may be covariant; cannot narrow visibility; checked exceptions may be the same or narrower; @Override recommended. static methods are hidden, not overridden. final methods cannot be overridden; abstract methods must be implemented by concrete subclasses.
 - Varargs: T... is syntactic sugar for T[]. Must be last parameter. Watch for overloading ambiguity. Generic varargs can cause heap-pollution warnings; use @SafeVarargs if safe.
 - Generics: method type parameters (e.g., <T>) can be declared on methods. Bounds (e.g., <T extends Comparable<T>>). Type erasure may generate bridge methods.
 - main entry point: public static void main(String[] args).
 - Exceptions: declare checked exceptions with 'throws'. Throw with 'throw'. Overriding cannot broaden checked exceptions.
 - Static vs instance: static belongs to class; instance belongs to object and can use 'this'. static methods cannot use instance state unless an instance is provided.
 - this and super: 'this' for current instance; 'super' to refer to superclass members. Interface default method conflicts require disambiguation with X.super.m().
 - Recursion: needs a base case. Java does not do tail-call optimization; deep recursion can overflow the stack.
 - Access control: public, protected, (package-private), private govern visibility. Subclasses cannot reduce visibility of overridden methods.
 - Concurrency: synchronized methods lock on 'this' (instance) or on Class for static. Reentrant.
 - FP and strictfp: strictfp enforces platform-independent floating-point semantics.
 - Docs: Javadoc @param, @return, @throws. Good naming: lowerCamelCase for methods/parameters.
 - No named/default parameters in Java. Simulate defaults via overloading, builders, or Optional parameters.
 - Evaluation order: method arguments are evaluated left-to-right.
 - Interfaces: can have abstract, default, static, and private methods (private since Java 9). Functional interfaces enable lambdas/method references.
 - Reflection and method references: dynamic invocation vs compile-time binding.
*/

public class _01_Theory {

    public static void main(String[] args) throws Exception {
        // Simple call and return
        int s = add(2, 3);
        System.out.println("add(2,3) = " + s);

        // Void method
        log("Hello methods");

        // Pass-by-value: primitives
        int x = 10;
        incrementPrimitive(x);
        System.out.println("x after incrementPrimitive = " + x); // still 10

        // Pass-by-value: references (mutate vs reassign)
        Person p = new Person("Ada");
        renamePerson(p, "Grace"); // mutates object
        System.out.println("Person name after renamePerson = " + p.name);
        reassignPerson(p); // reassigning parameter doesn't affect caller
        System.out.println("Person after reassignPerson = " + p.name);

        // Varargs
        System.out.println("sumVarargs(1,2,3) = " + sumVarargs(1, 2, 3));

        // Overloading resolution
        System.out.println("overload(1) -> " + overload(1));                           // int
        System.out.println("overload(1L) -> " + overload(1L));                         // long
        System.out.println("overload(Integer.valueOf(1)) -> " + overload(Integer.valueOf(1))); // Integer
        System.out.println("overload() -> " + overload());                             // varargs

        // Generic method
        System.out.println("max(3,5) = " + max(3, 5));

        // Recursion
        System.out.println("factorial(5) = " + factorial(5));

        // Evaluation order: left-to-right
        bar(printAndReturn("a", 1), printAndReturn("b", 2), printAndReturn("c", 3));

        // Method references
        List<Integer> nums = Arrays.asList(1, 2, 3, 4, 5);
        long evens = nums.stream().filter(_01_Theory::isEven).count();
        System.out.println("even count = " + evens);
        MethodRefExamples mre = new MethodRefExamples();
        long odds = nums.stream().filter(mre::isOdd).count();
        System.out.println("odd count = " + odds);
        Supplier<String> supplier = mre::toStr;
        System.out.println("bound instance method ref: " + supplier.get());
        Function<String, Person> personCtor = Person::new;
        System.out.println("constructor ref created: " + personCtor.apply("Linus").name);

        // Interface defaults and conflict resolution
        C c = new C();
        c.greet();

        // Abstract, final, override (covariant return)
        Animal a = new Dog();
        Food f = a.provideFood();
        System.out.println("Food class: " + f.getClass().getSimpleName());

        // Static hiding vs overriding
        Animal.staticSpeak();
        Dog.staticSpeak();

        // Checked exception: narrower on override
        try {
            new ReadFileChild().read();
        } catch (IOException ex) {
            System.out.println("Caught IOException: " + ex.getClass().getSimpleName());
        }

        // synchronized methods
        SyncExample se = new SyncExample();
        se.incrementSafely();
        System.out.println("SyncExample counter = " + se.counter);

        // @SafeVarargs generic varargs
        List<String> list = new ArrayList<>();
        addAll(list, "a", "b", "c");
        System.out.println("list after addAll: " + list);

        // Bridge methods via erasure
        GenericParent<String> parent = new GenericChild();
        System.out.println("GenericChild get(): " + parent.get());

        // Default parameters via overloading
        System.out.println("area(5) circle = " + area(5));
        System.out.println("area(4,5) rectangle = " + area(4, 5));

        // Shadowing and 'this'
        ShadowDemo sd = new ShadowDemo();
        sd.setX(42);
        System.out.println("ShadowDemo x = " + sd.x);

        // static vs instance
        Counter c1 = new Counter();
        Counter c2 = new Counter();
        c1.inc();
        c1.inc();
        System.out.println("Counter instances = " + Counter.getInstances() + ", c1=" + c1.get() + ", c2=" + c2.get());

        // strictfp
        System.out.println("strictfpSum(0.1, 0.2) = " + strictfpSum(0.1, 0.2));

        // Local class inside a method (no local methods)
        localClassDemo();

        // Reflection: dynamic invocation
        reflectionDemo();

        // Interface with private helper and static factory
        Greeter english = Greeter.english();
        english.sayHi("Alice");

        // Overload pitfalls
        OverloadPitfalls.demo();

        System.out.println("args length = " + args.length);
    }

    // Simple static method: has parameters and return type
    public static int add(int a, int b) {
        return a + b;
    }

    // Void method (procedure): performs side effects
    public static void log(String message) {
        System.out.println("[LOG] " + message);
    }

    // Pass-by-value with primitives
    static void incrementPrimitive(int x) {
        x++;
    }

    // Pass-by-value with references: can mutate the object
    static void renamePerson(Person person, String newName) {
        person.name = newName;
    }

    // Reassigning parameter doesn't change caller's reference
    static void reassignPerson(Person person) {
        person = new Person("New Person"); // has no effect outside
    }

    // Varargs (T... is T[])
    static int sumVarargs(int... values) {
        int sum = 0;
        for (int v : values) sum += v;
        return sum;
    }

    // Overloaded methods: demonstrate resolution
    static String overload(int x) { return "int"; }
    static String overload(long x) { return "long"; }
    static String overload(Integer x) { return "Integer"; }
    static String overload(int... xs) { return "varargs"; }
    static String overload() { return overload(new int[]{}); }

    // Generic method with bounded type parameter
    public static <T extends Comparable<T>> T max(T a, T b) {
        return a.compareTo(b) >= 0 ? a : b;
    }

    // Recursion (no TCO in Java)
    static long factorial(int n) {
        if (n < 0) throw new IllegalArgumentException("n must be >= 0");
        if (n <= 1) return 1;
        return n * factorial(n - 1);
    }

    // Arguments evaluated left-to-right
    static int printAndReturn(String label, int value) {
        System.out.println("evaluating " + label);
        return value;
    }
    static void bar(int x, int y, int z) {
        System.out.println("bar called with: " + x + "," + y + "," + z);
    }

    static boolean isEven(int n) { return n % 2 == 0; }

    // Checked exception in signature ('throws') vs 'throw' in body
    static void mightThrow(boolean fail) throws IOException {
        if (fail) throw new IOException("Failure");
    }

    // strictfp: consistent floating-point behavior across platforms
    public static strictfp double strictfpSum(double a, double b) {
        return a + b;
    }

    // synchronized instance methods: intrinsic lock is 'this' (reentrant)
    static class SyncExample {
        int counter = 0;
        public synchronized void incrementSafely() {
            counter++;
            nested(); // reentrant: same thread re-enters
        }
        private synchronized void nested() {
            counter++;
        }
    }

    /**
     * Formats a full name.
     * @param first non-null first name
     * @param last non-null last name
     * @return "first last"
     * @throws IllegalArgumentException if any blank
     */
    public static String formatName(String first, String last) {
        if (first == null || last == null || first.isBlank() || last.isBlank()) {
            throw new IllegalArgumentException("first/last required");
        }
        return first + " " + last;
    }

    // Default parameters simulated via overloading
    static double area(double radius) { return Math.PI * radius * radius; }
    static double area(double width, double height) { return width * height; }

    // @SafeVarargs to promise no heap pollution
    @SafeVarargs
    public static <T> void addAll(List<T> list, T... items) {
        for (T t : items) list.add(t);
    }

    // 'final' parameter: cannot be reassigned, but object may still be mutated
    static void doNotReassign(final Person p) {
        // p = new Person("X"); // compile error if uncommented
        p.name = p.name + " (mutated)";
    }

    // 'this' vs 'super' and shadowing
    static class Base {
        String who() { return "Base"; }
        String callWho() { return this.who(); }
    }
    static class Derived extends Base {
        @Override String who() { return "Derived"; }
        String parentWho() { return super.who(); }
    }

    // Interface defaults and conflict resolution
    interface A {
        default void greet() { System.out.println("Hello from A"); }
    }
    interface B {
        default void greet() { System.out.println("Hello from B"); }
    }
    static class C implements A, B {
        @Override public void greet() {
            A.super.greet();
            B.super.greet();
            System.out.println("Hello from C");
        }
    }

    // Interface with private helper and static factory (Java 9+ private methods in interfaces)
    interface Greeter {
        void hi(String name); // abstract
        default void sayHi(String name) { System.out.println(build("Hi", name)); }
        private String build(String prefix, String name) { return prefix + ", " + name; }
        static Greeter english() { return name -> System.out.println("Hi, " + name); }
    }

    // Overriding: covariant return, checked exception rules
    static class Food {}
    static class DogFood extends Food {}
    static class Animal {
        Food provideFood() { return new Food(); }
        static void staticSpeak() { System.out.println("Animal.staticSpeak"); }
        void read() throws IOException { throw new IOException("Animal read"); }
    }
    static class Dog extends Animal {
        @Override DogFood provideFood() { return new DogFood(); }          // covariant return
        static void staticSpeak() { System.out.println("Dog.staticSpeak"); } // hides, not overrides
        @Override void read() throws FileNotFoundException { throw new FileNotFoundException("Dog read"); } // narrower
    }

    // Checked exception override demo types
    static class ReadFileBase {
        void read() throws IOException { throw new IOException("base"); }
    }
    static class ReadFileChild extends ReadFileBase {
        @Override void read() throws FileNotFoundException { throw new FileNotFoundException("child"); }
    }

    // final and abstract methods
    static abstract class Shape {
        abstract double area();
        public final String unit() { return "square units"; } // cannot be overridden
    }
    static class Circle extends Shape {
        final double r;
        Circle(double r) { this.r = r; }
        @Override double area() { return Math.PI * r * r; }
    }

    // Static vs instance methods and state
    static class Counter {
        private static int instances = 0; // class-level
        private int value = 0;            // instance-level
        Counter() { instances++; }
        static int getInstances() { return instances; }
        void inc() { value++; }
        int get() { return value; }
    }

    // Method reference varieties
    static class MethodRefExamples {
        static boolean isPositive(int x) { return x > 0; } // static
        boolean isOdd(int x) { return x % 2 != 0; }        // unbound instance
        String toStr() { return "X"; }                     // bound instance (no args)
    }

    // Bridge methods from generics erasure
    static class GenericParent<T> {
        T get() { return null; }
    }
    static class GenericChild extends GenericParent<String> {
        @Override String get() { return "child"; } // compiler emits bridge get():Object
    }

    // Overloading pitfalls: widening vs boxing vs varargs
    static class OverloadPitfalls {
        static void m(long x) { System.out.println("widening to long"); }
        static void m(Integer x) { System.out.println("boxing to Integer"); }
        static void m(int... xs) { System.out.println("varargs int..."); }
        static void demo() {
            m(1);                    // widening preferred over boxing/varargs
            m(Integer.valueOf(1));   // exact reference type match
            m();                     // varargs empty
        }
        // Ambiguity examples (compile-time errors if called):
        // static void m(Object o, int... xs) { }
        // m(null); // ambiguous with m(Integer) and m(int...) if both applicable
    }

    // Local class inside a method (Java has no local/nested methods)
    static void localClassDemo() {
        class Helper { int doubleIt(int x) { return x * 2; } }
        Helper h = new Helper();
        System.out.println("Local class doubleIt(3) = " + h.doubleIt(3));
    }

    // Reflection: dynamic method invocation (advanced)
    static void reflectionDemo() throws Exception {
        Method m = _01_Theory.class.getDeclaredMethod("add", int.class, int.class);
        Object result = m.invoke(null, 2, 3);
        System.out.println("Reflection add = " + result);
    }

    // Native method (declaration only; needs JNI implementation)
    // native int nativeCompute(int x);

    // Static synchronized (locks on Class object)
    static class StaticSync {
        static int x = 0;
        public static synchronized void bump() { x++; }
    }

    // Shadowing demonstration: parameter shadows field; use 'this' to disambiguate
    static class ShadowDemo {
        int x = 10;
        void setX(int x) { this.x = x; }
    }

    // Optional parameters are not supported; simulate via overloading/builders/Optional types.
    // Named parameters are not supported; consider records/builders for clarity.
}

// Person class for reference semantics demos
class Person {
    String name;
    Person(String name) { this.name = name; }
}