package _06_01_lambda_expressions_and_functional_interfaces;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/*
   Lambda Expressions & Functional Interfaces — Theory (with runnable examples)

   Core ideas:
   - A lambda expression is an anonymous function (code + parameters) with a type inferred
     from its target "functional interface".
   - A functional interface (a.k.a. SAM type) has exactly one abstract method. It may have
     default and static methods (they do not count as abstract).
   - Lambdas are "target-typed": the context (variable/method parameter) determines the
     functional interface type and drives type inference.
   - Lambdas can capture effectively-final local variables (read-only). You can mutate the
     state of captured objects, but you cannot reassign captured local variables.
   - "this" in a lambda refers to the enclosing instance; lambdas do not create a new "this"
     like anonymous classes do.
   - Method references are a shorthand for certain lambda shapes:
       • Class::staticMethod            e.g., Integer::parseInt
       • instance::instanceMethod       e.g., list::add
       • Class::instanceMethod          e.g., String::compareToIgnoreCase
       • Class::new / Type[]::new       constructor/array constructor references
   - Checked exceptions: A lambda can throw checked exceptions only if the target method
     signature declares them. Otherwise, wrap or rethrow as unchecked.
   - java.util.function provides a rich set of standard functional interfaces (Supplier,
     Consumer, Function, Predicate, UnaryOperator, BinaryOperator, and "Bi"/primitive variants).
   - Composition/chaining: many functional interfaces support combinators like andThen,
     compose, and, or, negate, reversed, etc.
   - Primitive specializations (Int*, Long*, Double*) avoid boxing overhead.
   - Overloading with lambdas can be ambiguous; disambiguate with casts.
   - Performance note: non-capturing lambdas may be optimized to singletons; capturing lambdas
     allocate. Prefer primitive specializations to avoid boxing in hot paths.
   - Java 11+ allows 'var' in lambda parameters (must use 'var' for all parameters consistently).
     Example (Java 11+): (var a, var b) -> a + b

   This file demonstrates these concepts step by step.
*/
public class _01_Theory {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Lambda Expressions & Functional Interfaces - Theory & Examples ===");

        demoBasics();
        demoFunctionalInterfaceAndAnnotation();
        demoMethodReferences();
        demoVariableCaptureAndEffectivelyFinal();
        new _01_Theory().instanceContextDemo();
        demoOverloadingAndTargetTyping();
        demoCheckedExceptions();
        demoGenericsInFunctionalInterfaces();
        demoJavaUtilFunctionBasics();
        demoCompositionAndChaining();
        demoPrimitiveSpecializations();
        demoComparatorAndMethodReferences();
        demoRunnableCallableAndThreads();
        demoStreamsInterplay();
        demoPitfalls();

        System.out.println("=== Done ===");
    }

    // -----------------------------------------------------------------------------------------
    // 1) Functional interfaces (SAM types)
    // -----------------------------------------------------------------------------------------

    @FunctionalInterface
    interface StringTransformer {
        String apply(String s); // single abstract method (SAM)

        // Default method permitted on functional interfaces (does not count towards SAM).
        default String describe() {
            return "StringTransformer";
        }

        // Static methods are also allowed.
        static String shout(String s) {
            return s.toUpperCase(Locale.ROOT);
        }

        // Methods from Object (equals, hashCode, toString) do not count as abstract here.
    }

    @FunctionalInterface
    interface Transformer<T, R> {
        R apply(T t);
    }

    @FunctionalInterface
    interface ThrowingSupplier<T, E extends Exception> {
        T get() throws E;
    }

    @FunctionalInterface
    interface Add1 {
        int apply(int x);
    }

    @FunctionalInterface
    interface Times2 {
        int apply(int x);
    }

    static class Person {
        private final String name;
        private final int age;
        Person(String name, int age) { this.name = name; this.age = age; }
        String getName() { return name; }
        int getAge() { return age; }
        @Override public String toString() { return "Person{name='" + name + "', age=" + age + '}'; }
    }

    // -----------------------------------------------------------------------------------------
    // 2) Lambda basics
    // -----------------------------------------------------------------------------------------
    private static void demoBasics() {
        System.out.println("\n-- Basics --");

        // Different shapes of lambdas:
        StringTransformer upper = s -> s.toUpperCase(Locale.ROOT); // parameter type inferred
        StringTransformer bracket = (String s) -> "[" + s + "]";   // explicit parameter type
        StringTransformer exclaim = s -> { return s + "!"; };      // block body with return

        // Multiple parameters:
        IntBinaryOperator add = (a, b) -> a + b;                   // primitive specialization

        // No parameters:
        Runnable greeter = () -> System.out.println("Hello from a lambda");

        System.out.println(upper.apply("java"));
        System.out.println(bracket.apply("lambda"));
        System.out.println(exclaim.apply("wow"));
        System.out.println("2 + 3 = " + add.applyAsInt(2,3));
        greeter.run();

        // Java 11+ only (example; commented to keep Java 8 compatibility):
        // IntBinaryOperator add2 = (var x, var y) -> x + y;
    }

    // -----------------------------------------------------------------------------------------
    // 3) FunctionalInterface annotation and default/static methods
    // -----------------------------------------------------------------------------------------
    private static void demoFunctionalInterfaceAndAnnotation() {
        System.out.println("\n-- FunctionalInterface & default/static methods --");

        StringTransformer t = s -> s.trim();
        System.out.println("Desc: " + t.describe());
        System.out.println("Shout: " + StringTransformer.shout("hello"));
        System.out.println("Trim: " + t.apply("  spaced  "));
    }

    // -----------------------------------------------------------------------------------------
    // 4) Method references (Class::staticMethod, instance::method, Class::instanceMethod, Class::new)
    // -----------------------------------------------------------------------------------------
    private static void demoMethodReferences() {
        System.out.println("\n-- Method references --");

        // Class::staticMethod
        Function<String, Integer> parse = Integer::parseInt;
        System.out.println("parse '123' -> " + parse.apply("123"));

        // instance::instanceMethod (return value can be ignored when target expects void)
        List<String> list = new ArrayList<>();
        Consumer<String> addToList = list::add; // List.add returns boolean; Consumer accepts void -> allowed
        addToList.accept("A");
        addToList.accept("B");
        System.out.println("list -> " + list);

        // Class::instanceMethod (first arg is the "receiver")
        Comparator<String> byIgnoreCase = String::compareToIgnoreCase;
        System.out.println("Compare('a','A') -> " + byIgnoreCase.compare("a","A"));

        // Constructor references
        Supplier<List<String>> newList = ArrayList::new;
        List<String> l2 = newList.get();
        l2.add("X");
        System.out.println("constructed list -> " + l2);

        // Array constructor reference
        Function<Integer, String[]> stringArrayCtor = String[]::new;
        System.out.println("array length -> " + stringArrayCtor.apply(5).length);
    }

    // -----------------------------------------------------------------------------------------
    // 5) Variable capture & effectively-final rule
    // -----------------------------------------------------------------------------------------
    private static void demoVariableCaptureAndEffectivelyFinal() {
        System.out.println("\n-- Variable capture & effectively-final --");

        int base = 10;
        IntUnaryOperator plusBase = x -> x + base; // capture "base"
        System.out.println("5 + base(10) = " + plusBase.applyAsInt(5));

        // Illegal (would not compile): captured variable must be effectively final.
        // base++;
        // plusBase = x -> x + base; // error if base is modified

        // You may mutate the state of captured objects:
        List<String> stash = new ArrayList<>();
        Consumer<String> stashIt = s -> stash.add(s); // allowed (mutating object state)
        stashIt.accept("ok");
        System.out.println("stash -> " + stash);

        // Workaround to mutate numeric state: use a one-element array or AtomicInteger
        int[] counter = {0};
        Runnable inc = () -> counter[0]++;
        inc.run();
        System.out.println("counter -> " + counter[0]);
    }

    // -----------------------------------------------------------------------------------------
    // 6) 'this' in lambdas vs anonymous classes
    // -----------------------------------------------------------------------------------------
    private void instanceContextDemo() {
        System.out.println("\n-- 'this' in lambda vs anonymous class --");

        Runnable lambdaR = () -> System.out.println("lambda this: " + this.getClass().getSimpleName());
        Runnable anonR = new Runnable() {
            @Override public void run() {
                System.out.println("anon this: " + this.getClass().getSimpleName());
            }
        };
        lambdaR.run(); // refers to enclosing _01_Theory instance
        anonR.run();   // refers to anonymous class instance
    }

    // -----------------------------------------------------------------------------------------
    // 7) Overloading & target typing (disambiguate with casts)
    // -----------------------------------------------------------------------------------------
    private static void demoOverloadingAndTargetTyping() {
        System.out.println("\n-- Overloading & target typing --");

        // Ambiguous without cast (same shape: int -> int):
        // int amb = process(x -> x + 1); // would not compile (ambiguous)
        int a = process((Add1) (x -> x + 1));
        int b = process((Times2) (x -> x * 2));
        System.out.println("process(Add1): " + a);
        System.out.println("process(Times2): " + b);
    }

    private static int process(Add1 f)   { return f.apply(5); }
    private static int process(Times2 f) { return f.apply(5); }

    // -----------------------------------------------------------------------------------------
    // 8) Checked exceptions in lambdas
    // -----------------------------------------------------------------------------------------
    private static void demoCheckedExceptions() {
        System.out.println("\n-- Checked exceptions --");

        ThrowingSupplier<String, java.io.IOException> mayThrow = () -> {
            if (System.nanoTime() % 2 == 0) throw new java.io.IOException("Simulated IO problem");
            return "OK";
        };

        Supplier<String> safe = unchecked(mayThrow);
        try {
            System.out.println("safe.get(): " + safe.get());
        } catch (RuntimeException ex) {
            System.out.println("wrapped exception: " + ex.getCause());
        }
    }

    // Wrap a throwing lambda into a standard Supplier, converting checked -> unchecked
    private static <T> Supplier<T> unchecked(ThrowingSupplier<T, ?> throwingSupplier) {
        return () -> {
            try {
                return throwingSupplier.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    // -----------------------------------------------------------------------------------------
    // 9) Generics in functional interfaces
    // -----------------------------------------------------------------------------------------
    private static void demoGenericsInFunctionalInterfaces() {
        System.out.println("\n-- Generics in functional interfaces --");

        Transformer<Person, String> personToName = p -> p.getName();
        Transformer<String, Integer> nameLength   = s -> s.length();

        Person p = new Person("Alice", 30);
        System.out.println("name: " + personToName.apply(p));
        System.out.println("name length: " + nameLength.apply(personToName.apply(p)));
    }

    // -----------------------------------------------------------------------------------------
    // 10) java.util.function essentials
    // -----------------------------------------------------------------------------------------
    private static void demoJavaUtilFunctionBasics() {
        System.out.println("\n-- java.util.function essentials --");

        Supplier<UUID> uuid = UUID::randomUUID;
        System.out.println("UUID: " + uuid.get());

        Consumer<String> println = System.out::println;
        println.accept("Consumer printed this line");

        Predicate<String> nonBlank = s -> s != null && !s.trim().isEmpty();
        System.out.println("nonBlank('  '): " + nonBlank.test("  "));

        Function<String, Integer> length = String::length;
        System.out.println("length('abc'): " + length.apply("abc"));

        UnaryOperator<String> trim = String::trim;
        System.out.println("trim('  x  '): '" + trim.apply("  x  ") + "'");

        BinaryOperator<Integer> sum = Integer::sum;
        System.out.println("sum(3,4): " + sum.apply(3,4));

        BiFunction<String, Integer, String> leftPad = (s, n) -> {
            String x = s;
            while (x.length() < n) x = " " + x;
            return x;
        };
        System.out.println("leftPad('7', 3): '" + leftPad.apply("7", 3) + "'");

        BiPredicate<String, String> contains = String::contains;
        System.out.println("contains('banana','ana'): " + contains.test("banana","ana"));

        // Identity function
        Function<String, String> id = Function.identity();
        System.out.println("identity('z'): " + id.apply("z"));
    }

    // -----------------------------------------------------------------------------------------
    // 11) Composition & chaining (andThen, compose, and/or/negate, etc.)
    // -----------------------------------------------------------------------------------------
    private static void demoCompositionAndChaining() {
        System.out.println("\n-- Composition & chaining --");

        // Function composition
        Function<Integer, Integer> times2 = x -> x * 2;
        Function<Integer, String> toString = Object::toString;
        Function<Integer, String> pipeline = times2.andThen(toString);
        System.out.println("pipeline(21) -> " + pipeline.apply(21));

        // compose: f.compose(g) == f(g(x))
        Function<Integer, Integer> plus1 = x -> x + 1;
        Function<Integer, Integer> composed = plus1.compose(times2); // plus1(times2(x))
        System.out.println("compose plus1∘times2 on 10 -> " + composed.apply(10));

        // Predicate chaining
        Predicate<String> nonEmpty = s -> s != null && !s.isEmpty();
        Predicate<String> hasA = s -> s.contains("a");
        System.out.println("nonEmpty and hasA on 'java': " + nonEmpty.and(hasA).test("java"));
        System.out.println("nonEmpty and not hasA on 'hello': " + nonEmpty.and(hasA.negate()).test("hello"));

        // Consumer chaining (note result printing on one line)
        Consumer<String> c1 = s -> System.out.print("[" + s + "]");
        Consumer<String> c2 = s -> System.out.print("{" + s + "}");
        System.out.print("Consumers: ");
        c1.andThen(c2).accept("X");
        System.out.println();
    }

    // -----------------------------------------------------------------------------------------
    // 12) Primitive specializations (avoid boxing)
    // -----------------------------------------------------------------------------------------
    private static void demoPrimitiveSpecializations() {
        System.out.println("\n-- Primitive specializations --");

        IntSupplier dice = () -> 1 + (int)(Math.random() * 6);
        IntPredicate even = x -> x % 2 == 0;
        IntUnaryOperator square = x -> x * x;
        IntBinaryOperator max = Math::max;
        ToIntFunction<String> parseInt = Integer::parseInt;

        int roll = dice.getAsInt();
        System.out.println("roll: " + roll + " (even? " + even.test(roll) + ")");
        System.out.println("square(7): " + square.applyAsInt(7));
        System.out.println("max(3,9): " + max.applyAsInt(3, 9));
        System.out.println("toInt('42'): " + parseInt.applyAsInt("42"));
    }

    // -----------------------------------------------------------------------------------------
    // 13) Comparator helpers (comparing, thenComparing, reversed) with method refs
    // -----------------------------------------------------------------------------------------
    private static void demoComparatorAndMethodReferences() {
        System.out.println("\n-- Comparator & method references --");

        List<Person> people = Arrays.asList(
                new Person("Bob", 25),
                new Person("Alice", 30),
                new Person("Alice", 20),
                new Person("Charlie", 30)
        );

        people.sort(Comparator
                .comparing(Person::getAge)          // primary key
                .thenComparing(Person::getName));   // secondary key

        System.out.println("sorted by age, then name: " + people);

        people.sort(Comparator.comparing(Person::getName).reversed());
        System.out.println("sorted by name desc: " + people);
    }

    // -----------------------------------------------------------------------------------------
    // 14) Runnable, Callable, and executors
    // -----------------------------------------------------------------------------------------
    private static void demoRunnableCallableAndThreads() throws Exception {
        System.out.println("\n-- Runnable, Callable, Thread & Executor --");

        Thread t = new Thread(() -> System.out.println("Thread says hi"));
        t.start();
        t.join();

        ExecutorService es = Executors.newSingleThreadExecutor();
        Future<String> fut = es.submit(() -> "Callable result");
        System.out.println(fut.get());
        es.shutdown();
    }

    // -----------------------------------------------------------------------------------------
    // 15) Streams + lambdas (brief)
    // -----------------------------------------------------------------------------------------
    private static void demoStreamsInterplay() {
        System.out.println("\n-- Streams & lambdas --");

        List<Person> people = Arrays.asList(
                new Person("Bob", 25),
                new Person("Alice", 30),
                new Person("Alice", 20),
                new Person("Charlie", 30)
        );

        List<String> namesOver25 = people.stream()
                .filter(p -> p.getAge() > 25)
                .map(Person::getName)
                .distinct()
                .sorted()
                .toList(); // Java 16+; for Java 8, use .collect(Collectors.toList())

        System.out.println("namesOver25: " + namesOver25);

        // Reduction with BinaryOperator
        int totalAge = people.stream().map(Person::getAge).reduce(0, Integer::sum);
        System.out.println("total age: " + totalAge);
    }

    // -----------------------------------------------------------------------------------------
    // 16) Pitfalls & notes
    // -----------------------------------------------------------------------------------------
    private static void demoPitfalls() {
        System.out.println("\n-- Pitfalls & notes --");

        // Capturing mutable state can lead to side-effects and concurrency issues:
        List<String> log = new ArrayList<>();
        Runnable r = () -> log.add("event"); // mutates shared list
        r.run();
        System.out.println("log -> " + log);

        // Overloading ambiguity (already shown) requires explicit casts.
        // Serialization: Lambdas are not serializable unless the target type is Serializable.
        // 'var' in lambda parameters is Java 11+ and must be used consistently for all params.
    }
}