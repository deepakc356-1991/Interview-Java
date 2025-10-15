package _01_07_methods_and_parameters;

import java.lang.annotation.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Methods & Parameters — Interview Q&A (Basic → Intermediate → Advanced)
 *
 * Run main() to see concise demonstrations. Read Q&A comments inline.
 */
public class _03_InterviewQA {

    public static void main(String[] args) throws Throwable {
        System.out.println("=== Basics ===");
        basics();
        System.out.println("\n=== Parameters & Pass-by-Value ===");
        parametersPassByValue();
        System.out.println("\n=== Overloading Resolution ===");
        overloadingResolution();
        System.out.println("\n=== Overriding & Polymorphism ===");
        overridingPolymorphism();
        System.out.println("\n=== Varargs Gotchas ===");
        varargsGotchas();
        System.out.println("\n=== Generics in Methods (PECS) ===");
        genericsMethods();
        System.out.println("\n=== Lambdas & Method References ===");
        lambdasAndMethodReferences();
        System.out.println("\n=== Exceptions in Signatures ===");
        exceptionsInMethods();
        System.out.println("\n=== Modifiers: static/final/abstract/synchronized ===");
        modifiersDemo();
        System.out.println("\n=== Recursion & Tail Calls ===");
        recursionDemo();
        System.out.println("\n=== Reflection & MethodHandles ===");
        reflectionAndMethodHandles();
        System.out.println("\n=== Optional Parameters Patterns ===");
        optionalParametersPatterns();
        System.out.println("\n=== Bridge Methods & Type Erasure ===");
        bridgeMethodsDemo();
        System.out.println("\n=== Parameter Annotations ===");
        parameterAnnotationsDemo();
        System.out.println("\n=== Interface Default/Static/Private Methods ===");
        interfaceMethodsDemo();
        System.out.println("\n=== Concurrency: synchronized methods ===");
        concurrencyMethodsDemo();
        System.out.println("\n=== Performance Tips ===");
        performanceTips();
    }

    // ========================= BASICS =========================
    /*
     Q: What is a method in Java?
     A: A block of code with a name that can be invoked; it may take parameters and return a value.

     Q: What is a method signature?
     A: Method name + parameter types (order matters). Return type, thrown exceptions, parameter names are NOT part of the signature.

     Q: Return type vs void?
     A: Non-void methods must return a value of exactly the declared type (or compatible via covariance); void methods may still use 'return;' to exit early.

     Q: Can you overload by return type only?
     A: No. Overload requires a different parameter list.

     Q: Access modifiers for methods?
     A: public, protected, (package-private), private.

     Q: static vs instance methods?
     A: static belong to the class; instance require an object. Static methods are hidden, not overridden.
     */
    static void basics() {
        System.out.println(add(2, 3));
        greet("Alex");
    }

    static int add(int a, int b) { // signature: add(int,int)
        return a + b;
    }

    static void greet(String name) {
        if (name == null) return; // early return in void method
        System.out.println("Hello, " + name);
    }

    // ========================= PARAMETERS & PASS-BY-VALUE =========================
    /*
     Q: Is Java pass-by-reference or pass-by-value?
     A: Always pass-by-value. For object parameters, the value copied is the reference (pointer), not the object itself.

     Q: Implications?
     - Reassigning the parameter inside the method does NOT affect the caller's variable.
     - Mutating the object through the reference does affect the caller (same object).

     Q: How to swap two integers or objects?
     A: You cannot swap caller variables via a method in Java. Use a holder/container or return a tuple-like object.
     */
    static void parametersPassByValue() {
        // Primitives
        int x = 5;
        bump(x);
        System.out.println("x after bump(x): " + x); // still 5

        // Objects - mutation visible
        Box b = new Box(10);
        mutate(b);
        System.out.println("b.value after mutate: " + b.value); // 11

        // Reassignment not visible
        reassign(b);
        System.out.println("b.value after reassign(b): " + b.value); // still 11

        // Swap attempt (fails)
        Integer a = 1, c = 2;
        swapAttempt(a, c);
        System.out.println("a,c after swapAttempt: " + a + "," + c); // 1,2
    }

    static void bump(int n) { n++; }
    static void mutate(Box b) { b.value++; }
    static void reassign(Box b) { b = new Box(999); }
    static void swapAttempt(Integer i, Integer j) { Integer tmp = i; i = j; j = tmp; }

    static final class Box { int value; Box(int v) { this.value = v; } }

    // ========================= OVERLOADING RESOLUTION =========================
    /*
     Q: What is method overloading?
     A: Same method name, different parameter lists. Resolved at compile time (static binding).

     Q: Resolution order nuance?
     A (simplified): Exact match > widening primitive > boxing > varargs.

     Q: Ambiguities?
     - null with overloaded reference types can be ambiguous.
     - boxing + varargs may compete.
     - Overloading with generics can be confusing.

     Q: Is varargs just an array?
     A: Yes; foo(String...) compiles to foo(String[]).
     */
    static void overloadingResolution() {
        System.out.println(pick(1));          // int -> exact
        System.out.println(pick(1L));         // long -> exact
        System.out.println(pick(Integer.valueOf(2))); // boxing exact

        // null ambiguity example
        // System.out.println(resolve(null)); // Compile error: reference to resolve is ambiguous
        System.out.println(resolve((String) null)); // disambiguate via cast

        // Boxing vs varargs
        System.out.println(score(5));     // picks score(Integer) over varargs
        System.out.println(score());      // only varargs matches
        System.out.println(score(1, 2));  // varargs
    }

    static String pick(int x) { return "int"; }
    static String pick(long x) { return "long"; }
    static String pick(Integer x) { return "Integer"; }

    static String resolve(Object o) { return "Object"; }
    static String resolve(String s) { return "String"; }

    static String score(Integer i) { return "boxed"; }
    static String score(Integer... arr) { return "varargs:" + arr.length; }

    // ========================= OVERRIDING & POLYMORPHISM =========================
    /*
     Q: What is method overriding?
     A: Subclass provides a method with same signature. Resolved at runtime (dynamic dispatch).

     Q: Rules?
     - Same name and parameter types; return type can be covariant (more specific).
     - Cannot reduce visibility.
     - Cannot throw broader checked exceptions than the overridden method.
     - Use @Override to avoid mistakes.

     Q: Static methods?
     A: They are hidden (not overridden). Dispatch is based on reference type.
     */
    static void overridingPolymorphism() {
        Animal a = new Dog();
        System.out.println(a.speak()); // dynamic dispatch -> Dog
        System.out.println(a.kind());  // static method -> Animal.kind because call via class recommended

        Dog d = new Dog();
        System.out.println(d.kind());  // Dog.kind hides Animal.kind

        // Covariant return type
        Animal aa = new Cat();
        Pet p = aa.bestFriend();
        System.out.println(p.name());
    }

    static class Pet { String name() { return "Buddy"; } }
    static class CatFriend extends Pet { @Override public String name() { return "Whiskers"; } }

    static class Animal {
        String speak() { return "..." ;}
        static String kind() { return "Animal"; }
        Pet bestFriend() { return new Pet(); }
    }
    static class Dog extends Animal {
        @Override String speak() { return "Woof"; }
        static String kind() { return "Dog"; } // hides
        @Override Pet bestFriend() { return new Pet(); } // covariant allowed if subtype; here same
    }
    static class Cat extends Animal {
        @Override String speak() { return "Meow"; }
        @Override CatFriend bestFriend() { return new CatFriend(); } // covariant
    }

    // ========================= VARARGS GOTCHAS =========================
    /*
     Q: Constraints?
     - At most one vararg, must be last parameter.
     - Overloading vararg vs array is the same signature at bytecode; cannot differ only by vararg annotation.

     Q: Pitfalls?
     - Passing null: varargs method foo(null) is ambiguous with overloads; cast to type.
     - Performance: creates an array each call (unless optimized by JIT). Avoid in hot paths or provide non-vararg overloads.

     Q: Generics + varargs?
     - Heap pollution warnings. Use @SafeVarargs on final/static/private methods when safe.
     */
    static void varargsGotchas() {
        System.out.println(join("-"));                 // 0 args
        System.out.println(join("-", "a", "b", "c"));  // multiple

        // Passing a single null as element:
        System.out.println(join(",", (String) null)); // prints "null"

        // @SafeVarargs demo
        List<String> a = Arrays.asList("x","y");
        List<String> b = Arrays.asList("z");
        System.out.println(flattenToList(a, b));
    }

    static String join(String sep, String... parts) {
        if (parts == null) return "null-array";
        return String.join(sep, Arrays.stream(parts).map(String::valueOf).toList());
    }

    @SafeVarargs
    static <T> List<T> flattenToList(List<T>... lists) {
        List<T> out = new ArrayList<>();
        for (List<T> l : lists) out.addAll(l);
        return out;
    }

    // ========================= GENERICS IN METHODS (PECS) =========================
    /*
     Q: Method-level type parameters?
     A: Define <T> on the method: static <T> T id(T x) { return x; }

     Q: Bounded type parameters?
     A: <T extends Number> restricts to subtypes of Number.

     Q: Wildcards and PECS?
     A: Producer Extends, Consumer Super.
        - Read from ? extends T; write to ? super T.
     */
    static void genericsMethods() {
        Integer i = id(42);
        Number n = sumNumbers(List.of(1, 2.5, 3)); // extends Number
        List<Number> dest = new ArrayList<>();
        copy(List.of(1, 2, 3), dest); // consumer super Number
        System.out.println(i + " " + n + " " + dest);

        // Type inference with diamond and method type inference
        var max = maxOf(3, 7, Integer::compareTo);
        System.out.println("max=" + max);
    }

    static <T> T id(T x) { return x; }

    static double sumNumbers(List<? extends Number> nums) {
        double s = 0;
        for (Number n : nums) s += n.doubleValue();
        return s;
    }

    static <T> void copy(List<? extends T> src, List<? super T> dst) {
        dst.addAll(src);
    }

    static <T> T maxOf(T a, T b, java.util.Comparator<? super T> cmp) {
        return cmp.compare(a, b) >= 0 ? a : b;
    }

    // ========================= LAMBDAS & METHOD REFERENCES =========================
    /*
     Q: What is a functional interface?
     A: An interface with exactly one abstract method (SAM). Lambdas target these.

     Q: Effectively final?
     A: Lambda can capture local variables only if not modified after assignment.

     Q: Overload + lambda ambiguity?
     A: If multiple overloads accept different functional interfaces, cast to target type.

     Q: Method references?
     A: static: Type::method; bound: instance::method; unbound: Type::instanceMethod; constructor: Type::new.
     */
    static void lambdasAndMethodReferences() {
        // Functional interface
        Transformer<String, Integer> lengthFn = String::length;
        System.out.println(lengthFn.apply("hello"));

        // Effectively final capture
        int base = 10;
        // base++; // would break "effectively final"
        Transformer<Integer, Integer> addBase = x -> x + base;
        System.out.println(addBase.apply(5));

        // Overload ambiguity with lambdas
        // choose by cast
        invoke((Predicate<String>) s -> s.isEmpty(), "");

        // Method reference disambiguation
        invoke((Transformer<String, Integer>) String::length, "abc");

        // var in lambda params (for annotations in Java 11+)
        java.util.function.BiFunction<Integer, Integer, Integer> adder = (var x, var y) -> x + y;
        System.out.println(adder.apply(2, 3));
    }

    @FunctionalInterface
    interface Transformer<I, O> { O apply(I in); }

    static void invoke(Predicate<String> pred, String s) {
        System.out.println("Predicate says: " + pred.test(s));
    }

    static <I, O> void invoke(Transformer<I, O> fn, I input) {
        System.out.println("Transformer gives: " + fn.apply(input));
    }

    // ========================= EXCEPTIONS IN METHOD SIGNATURES =========================
    /*
     Q: checked vs unchecked?
     A: Checked (Exception) must be declared or caught. Unchecked (RuntimeException) need not.

     Q: throws vs throw?
     A: throws declares, throw actually throws.

     Q: Overriding exception rules?
     A: Overrider cannot declare broader checked exceptions; can narrow or remove.

     Q: Finally and returns?
     A: finally always runs; avoid returning from finally; it can mask exceptions.
     */
    static void exceptionsInMethods() {
        try {
            mayThrowChecked(true);
        } catch (java.io.IOException e) {
            System.out.println("Caught checked: " + e.getClass().getSimpleName());
        }

        // Overriding rules demo
        BaseSvc svc = new ChildSvc();
        try {
            svc.work();
        } catch (Exception e) {
            System.out.println("work threw: " + e.getClass().getSimpleName());
        }
    }

    static void mayThrowChecked(boolean flag) throws java.io.IOException {
        if (flag) throw new java.io.IOException("IO issue");
    }

    static class BaseSvc {
        void work() throws Exception { throw new Exception("base"); }
    }
    static class ChildSvc extends BaseSvc {
        @Override void work() throws java.io.IOException { throw new java.io.IOException("child narrower"); }
    }

    // ========================= MODIFIERS: static/final/abstract/synchronized =========================
    /*
     Q: final method?
     A: Cannot be overridden.

     Q: abstract method?
     A: Declared without body, must be implemented by subclass (unless subclass is abstract).

     Q: synchronized method?
     A: Acquires the monitor of 'this' (instance) or the Class object (static). Reentrant.
     */
    static void modifiersDemo() {
        AbstractShape s = new Circle(2.0);
        System.out.println("area=" + s.area());
        System.out.println("desc=" + s.describe());
    }

    static abstract class AbstractShape {
        abstract double area();
        final String describe() { return "shape area=" + area(); }
        synchronized void syncOp() { /* lock this */ }
        static synchronized void staticSyncOp() { /* lock AbstractShape.class */ }
    }
    static final class Circle extends AbstractShape {
        final double r;
        Circle(double r) { this.r = r; }
        @Override double area() { return Math.PI * r * r; }
    }

    // ========================= RECURSION & TAIL CALLS =========================
    /*
     Q: Tail-call optimization?
     A: Java does not guarantee TCO. Deep recursion risks StackOverflowError; prefer iteration.

     Q: Example?
     A: Factorial recursion vs loop.
     */
    static void recursionDemo() {
        System.out.println("fact(5) recursive=" + factRec(5));
        System.out.println("fact(5) iterative=" + factIter(5));
    }

    static long factRec(int n) { return n <= 1 ? 1 : n * factRec(n - 1); }
    static long factIter(int n) {
        long r = 1;
        for (int i = 2; i <= n; i++) r *= i;
        return r;
    }

    // ========================= REFLECTION & METHOD HANDLES =========================
    /*
     Q: Reflection?
     A: Introspect and invoke methods at runtime. Slower, bypasses compile-time checks.

     Q: MethodHandle?
     A: Faster, typed, low-level method reference (java.lang.invoke).

     Q: Use cases?
     A: Frameworks, DI containers, serialization, proxies.
     */
    static void reflectionAndMethodHandles() throws Throwable {
        Sample sample = new Sample();
        Method m = Sample.class.getDeclaredMethod("hidden", int.class);
        m.setAccessible(true);
        System.out.println("Reflection: " + m.invoke(sample, 3));

        MethodHandle mh = MethodHandles.lookup()
                .findVirtual(Sample.class, "hidden", MethodType.methodType(String.class, int.class));
        System.out.println("MethodHandle: " + (String) mh.invoke(sample, 4));
    }

    static class Sample {
        private String hidden(int x) { return "x*2=" + (x * 2); }
    }

    // ========================= OPTIONAL PARAMETERS PATTERNS =========================
    /*
     Q: Does Java support default/named parameters?
     A: No. Use overloading, builders, parameter objects.

     Q: When to use Optional as parameter?
     A: Generally avoid. Prefer overloads or null with clear docs; Optional is for return types.

     Q: Telescoping vs builder?
     A: For many optional params, use builder to improve readability.
     */
    static void optionalParametersPatterns() {
        // Overloading pattern
        sendEmail("to@x.com", "hi");
        sendEmail("to@x.com", "hi", "noreply@x.com", true);

        // Builder pattern
        HttpReq req = HttpReq.builder("https://example.com")
                .method("POST")
                .header("X", "1")
                .timeoutMs(5000)
                .build();
        System.out.println(req);
    }

    static void sendEmail(String to, String body) { sendEmail(to, body, null, false); }
    static void sendEmail(String to, String body, String from, boolean urgent) {
        System.out.println("email[to=" + to + ",from=" + from + ",urgent=" + urgent + "]");
    }

    static final class HttpReq {
        final String url;
        final String method;
        final Map<String, String> headers;
        final int timeoutMs;

        private HttpReq(Builder b) {
            this.url = b.url;
            this.method = b.method;
            this.headers = Map.copyOf(b.headers);
            this.timeoutMs = b.timeoutMs;
        }
        static Builder builder(String url) { return new Builder(url); }
        static final class Builder {
            private final String url;
            private String method = "GET";
            private final Map<String, String> headers = new LinkedHashMap<>();
            private int timeoutMs = 1000;
            Builder(String url) { this.url = url; }
            Builder method(String m) { this.method = m; return this; }
            Builder header(String k, String v) { this.headers.put(k, v); return this; }
            Builder timeoutMs(int t) { this.timeoutMs = t; return this; }
            HttpReq build() { return new HttpReq(this); }
        }
        @Override public String toString() {
            return "HttpReq{url='" + url + "', method='" + method + "', headers=" + headers + ", timeoutMs=" + timeoutMs + "}";
        }
    }

    // ========================= BRIDGE METHODS & TYPE ERASURE =========================
    /*
     Q: What is type erasure?
     A: Generic type info is erased at runtime; compiler inserts casts and may generate bridge methods.

     Q: Bridge method?
     A: A synthetic method added by compiler to preserve polymorphism with generics and covariance.

     Example below compiles to add a bridge so that RawUserRepo.get(Object) calls get(String).
     */
    static void bridgeMethodsDemo() {
        RawUserRepo repo = new UserRepo();
        Object o = repo.get("id-1"); // Works via bridge method
        System.out.println(o);
    }

    interface RawUserRepo<T> { T get(Object key); } // intentionally odd for demo
    static class UserRepo implements RawUserRepo<String> {
        // Covariant-like signature w.r.t erasure
        public String get(Object key) { return "user:" + String.valueOf(key); }
    }

    // ========================= PARAMETER ANNOTATIONS =========================
    /*
     Q: Can parameters be annotated?
     A: Yes. Common: @Nullable, @NotNull, @Deprecated, custom annotations. Readable via reflection.
     */
    static void parameterAnnotationsDemo() throws Exception {
        Method m = AnnotatedApi.class.getMethod("fetch", String.class, int.class);
        Annotation[][] anns = m.getParameterAnnotations();
        for (int i = 0; i < anns.length; i++) {
            System.out.println("Param " + i + " annotations: " + Arrays.toString(anns[i]));
        }
        System.out.println(new AnnotatedApi().fetch("key", 5));
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    @interface NotEmpty { }

    static class AnnotatedApi {
        String fetch(@NotEmpty String key, @Deprecated int retries) {
            if (key == null || key.isEmpty()) throw new IllegalArgumentException("key empty");
            return "ok:" + key + ":" + retries;
        }
    }

    // ========================= INTERFACE DEFAULT/STATIC/PRIVATE METHODS =========================
    /*
     Q: Default methods (since Java 8)?
     A: Methods with body in interfaces. Classes can override.

     Q: Static methods in interfaces?
     A: Yes; called via InterfaceName.staticMethod().

     Q: Private interface methods (since Java 9)?
     A: Support factoring logic inside interface.
     */
    static void interfaceMethodsDemo() {
        Greeter g = new ConsoleGreeter();
        g.greet("World");
        Greeter.shout("hi");
    }

    interface Greeter {
        default void greet(String name) {
            log("greeting " + name);
            System.out.println("Hello, " + name);
        }
        static void shout(String msg) { System.out.println(msg.toUpperCase()); }
        private void log(String x) { /* private helper in interface */ }
    }
    static class ConsoleGreeter implements Greeter { }

    // ========================= CONCURRENCY: synchronized methods =========================
    /*
     Q: When to use synchronized method?
     A: To serialize access to mutable shared state. Prefer higher-level concurrency utilities when possible.

     Q: Reentrancy?
     A: A thread holding the lock can enter again (no deadlock with same monitor).
     */
    static void concurrencyMethodsDemo() {
        Counter c = new Counter();
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            threads.add(new Thread(() -> { for (int k = 0; k < 1000; k++) c.inc(); }));
        }
        threads.forEach(Thread::start);
        for (Thread t : threads) try { t.join(); } catch (InterruptedException ignored) {}
        System.out.println("counter=" + c.get());
    }

    static class Counter {
        private int v;
        public synchronized void inc() { v++; }
        public synchronized int get() { return v; }
    }

    // ========================= PERFORMANCE TIPS =========================
    /*
     - Avoid unnecessary varargs in tight loops; create overloads for common arities.
     - Prefer primitives over boxed types in hot paths to avoid boxing/unboxing/GC.
     - Avoid reflection on critical code paths; cache MethodHandles when needed.
     - Mark @SafeVarargs thoughtfully; ensure no heap pollution.
     - Design APIs to minimize copying large parameter objects (use views/streams).
     */
    static void performanceTips() {
        // Boxing demo
        long t0 = System.nanoTime();
        int sum = 0;
        for (int i = 0; i < 1_0000; i++) sum += fastAdd(i, i);
        long t1 = System.nanoTime();
        Integer sum2 = 0;
        for (int i = 0; i < 1_0000; i++) sum2 = slowAdd(sum2, i);
        long t2 = System.nanoTime();
        System.out.println("primitive ns=" + (t1 - t0) + ", boxed ns=" + (t2 - t1) + ", sums=" + sum + "," + sum2);
    }

    static int fastAdd(int a, int b) { return a + b; }
    static Integer slowAdd(Integer a, Integer b) { return a + b; } // boxing

    /*
     Additional quick Q&A:
     - Can you overload on generic parameters only? No; type erasure prevents distinguishing.
     - Can you have two methods differing only by thrown exceptions? No; not part of signature.
     - Are parameter names part of signature? No.
     - How to ensure immutability? Make methods pure, avoid mutating params/fields, return new objects.
     - Are constructors methods? Not technically; but they have parameter lists and overloading rules similar to methods.
     - Can interface methods be private? Yes (since Java 9), but only for helper methods inside interface.
     */
}