package _06_01_lambda_expressions_and_functional_interfaces;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;
import java.util.stream.Collectors;

/**
 * Lambda Expressions & Functional Interfaces
 * This file demonstrates:
 * - Lambda syntax and target typing
 * - Custom and built-in functional interfaces
 * - Method references (static, instance, constructor)
 * - Variable capture and effectively-final rule
 * - 'this' reference in lambdas vs anonymous classes
 * - Composition (Function, Predicate, Comparator)
 * - Higher-order functions, currying/partial application
 * - Checked exceptions with lambdas
 * - Streams with lambdas
 * - Overload resolution with lambdas
 */
public class _02_Examples {

    public static void main(String[] args) {
        example01_BasicLambdaSyntax();
        example02_FunctionalInterfaceAndAnnotation();
        example03_BuiltInFunctionalInterfaces();
        example04_MethodReferences();
        example05_VariableCaptureAndEffectivelyFinal();
        new _02_Examples().example06_ThisReferenceDifference();
        example07_FunctionComposition();
        example08_ComparatorWithLambdas();
        example09_HigherOrderFunctions();
        example10_CurryingAndPartialApplication();
        example11_CheckedExceptionsInLambdas();
        example12_StreamsWithLambdas();
        example13_OverloadResolutionAndTargetTyping();
    }

    // Example 1: Basic lambda syntax and target typing
    private static void example01_BasicLambdaSyntax() {
        System.out.println("Example 01: Basic Lambda Syntax");

        // Runnable: no arguments, void return
        Runnable r1 = () -> System.out.println("Hello from lambda Runnable");
        Runnable r2 = new Runnable() { @Override public void run() { System.out.println("Hello from anonymous class"); } };
        r1.run();
        r2.run();

        // Parameter type inference vs explicit types
        IntUnaryOperator inc1 = x -> x + 1;                  // inferred
        IntUnaryOperator inc2 = (int x) -> { return x + 1; }; // explicit type + block body
        System.out.println("inc1(5) = " + inc1.applyAsInt(5));
        System.out.println("inc2(5) = " + inc2.applyAsInt(5));

        // Multiple parameters
        IntBinaryOperator sum = (a, b) -> a + b;
        System.out.println("sum(2,3) = " + sum.applyAsInt(2, 3));

        // Multi-statement body
        IntUnaryOperator calc = x -> {
            int y = x * x;
            y += 10;
            return y;
        };
        System.out.println("calc(3) = " + calc.applyAsInt(3));
        System.out.println();
    }

    // Example 2: Custom functional interface with @FunctionalInterface, default and static methods
    private static void example02_FunctionalInterfaceAndAnnotation() {
        System.out.println("Example 02: Functional Interface & @FunctionalInterface");

        IntOperation add = IntOperation.add();               // static factory method
        IntOperation multiply = (a, b) -> a * b;             // custom lambda
        IntOperation addThenDouble = add.andThen(x -> x * 2);// default method composition

        System.out.println("add(3,4) = " + add.apply(3, 4));
        System.out.println("multiply(3,4) = " + multiply.apply(3, 4));
        System.out.println("addThenDouble(3,4) = " + addThenDouble.apply(3, 4));
        System.out.println();
    }

    // Example 3: Built-in functional interfaces
    private static void example03_BuiltInFunctionalInterfaces() {
        System.out.println("Example 03: Built-in Functional Interfaces");

        Consumer<String> printer = s -> System.out.println("consumed: " + s);
        Supplier<Double> random = Math::random;
        Predicate<String> isBlank = s -> s == null || s.trim().isEmpty();
        Function<String, Integer> length = s -> s == null ? 0 : s.length();
        BiFunction<Integer, Integer, Integer> add = (a, b) -> a + b;
        UnaryOperator<String> upper = String::toUpperCase;
        BinaryOperator<Integer> max = Integer::max;

        printer.accept("hello");
        System.out.println("random = " + random.get());
        System.out.println("isBlank(\"   \") = " + isBlank.test("   "));
        System.out.println("length(\"test\") = " + length.apply("test"));
        System.out.println("add(5,6) = " + add.apply(5, 6));
        System.out.println("upper(\"mixed\") = " + upper.apply("mixed"));
        System.out.println("max(3,7) = " + max.apply(3, 7));
        System.out.println();
    }

    // Example 4: Method references (static, instance, constructor)
    private static void example04_MethodReferences() {
        System.out.println("Example 04: Method References");

        // Static method
        Function<String, Integer> parse = Integer::parseInt;
        System.out.println("parse(\"123\") + 1 = " + (parse.apply("123") + 1));

        // Particular object instance method
        Consumer<Object> println = System.out::println;
        println.accept("println via method reference");

        // Instance method of arbitrary type
        Function<String, String> upper = String::toUpperCase;
        System.out.println("upper(\"abc\") = " + upper.apply("abc"));
        Comparator<String> ciCmp = String::compareToIgnoreCase;
        System.out.println("compareToIgnoreCase(\"a\",\"A\") = " + ciCmp.compare("a", "A"));

        // Constructor references
        Supplier<Person> newPerson = Person::new;
        Person p1 = newPerson.get();
        Function<String, Person> personByName = Person::new;
        Person p2 = personByName.apply("Dana");
        BiFunction<String, Integer, Person> personFull = Person::new;
        Person p3 = personFull.apply("Evan", 42);

        System.out.println("Persons: " + p1 + ", " + p2 + ", " + p3);
        System.out.println();
    }

    // Example 5: Variable capture and effectively-final rule
    private static void example05_VariableCaptureAndEffectivelyFinal() {
        System.out.println("Example 05: Variable Capture & Effectively-Final");

        int factor = 2; // must be effectively final
        IntUnaryOperator times = x -> x * factor; // captures factor
        System.out.println("times(10) with factor=2 -> " + times.applyAsInt(10));
        // factor++; // Uncommenting this line would make 'factor' not effectively final and won't compile

        // To vary captured value, wrap it in a mutable holder
        AtomicInteger box = new AtomicInteger(2);
        IntUnaryOperator timesBox = x -> x * box.get();
        System.out.println("timesBox(10) with box=2 -> " + timesBox.applyAsInt(10));
        box.set(3);
        System.out.println("timesBox(10) with box=3 -> " + timesBox.applyAsInt(10));

        // Capturing mutable collection (the reference must be final, contents may change)
        List<String> acc = new ArrayList<>();
        Arrays.asList("a", "b", "c").forEach(acc::add);
        System.out.println("acc = " + acc);
        System.out.println();
    }

    // Example 6: 'this' reference: lambda binds to enclosing instance; anonymous class binds to anonymous instance
    private void example06_ThisReferenceDifference() {
        System.out.println("Example 06: 'this' Reference Differences");

        Runnable asAnon = new Runnable() {
            @Override public void run() {
                System.out.println("anonymous this: " + this.getClass().getName());
            }
        };

        Runnable asLambda = () -> System.out.println("lambda this: " + this.getClass().getName());

        asAnon.run();   // 'this' == anonymous class instance
        asLambda.run(); // 'this' == _02_Examples instance
        System.out.println();
    }

    // Example 7: Function/Predicate composition
    private static void example07_FunctionComposition() {
        System.out.println("Example 07: Composition (Function, Predicate)");

        Function<Integer, Integer> doubleIt = x -> x * 2;
        Function<Integer, Integer> square = x -> x * x;
        Function<Integer, Integer> doubleThenSquare = doubleIt.andThen(square);
        Function<Integer, Integer> squareThenDouble = doubleIt.compose(square);

        System.out.println("doubleThenSquare(3) = " + doubleThenSquare.apply(3)); // (3*2)^2 = 36
        System.out.println("squareThenDouble(3) = " + squareThenDouble.apply(3)); // (3^2)*2 = 18

        Predicate<Integer> isEven = n -> n % 2 == 0;
        Predicate<Integer> isPositive = n -> n > 0;
        Predicate<Integer> evenAndPositive = isEven.and(isPositive);
        System.out.println("evenAndPositive(4) = " + evenAndPositive.test(4));
        System.out.println("evenAndPositive(-2) = " + evenAndPositive.test(-2));
        System.out.println();
    }

    // Example 8: Comparator with lambdas
    private static void example08_ComparatorWithLambdas() {
        System.out.println("Example 08: Comparator with Lambdas");

        List<Person> people = new ArrayList<>(Arrays.asList(
                new Person("Bob", 30),
                new Person("Alice", 30),
                new Person("Charlie", 25)
        ));

        people.sort(Comparator.comparingInt(Person::getAge).thenComparing(Person::getName));
        System.out.println("sorted by age, then name = " + people);

        Comparator<Person> byNameDesc = Comparator.comparing(Person::getName).reversed();
        people.sort(byNameDesc);
        System.out.println("sorted by name desc = " + people);
        System.out.println();
    }

    // Example 9: Higher-order functions (functions taking/returning functions)
    private static void example09_HigherOrderFunctions() {
        System.out.println("Example 09: Higher-Order Functions");

        Predicate<Integer> gt10 = greaterThan(10);
        System.out.println("gt10(11) = " + gt10.test(11));
        System.out.println("gt10(9) = " + gt10.test(9));

        Predicate<String> notBlank = notNullAnd(s -> !s.trim().isEmpty());
        System.out.println("notBlank(\" \") = " + notBlank.test(" "));
        System.out.println("notBlank(\"ok\") = " + notBlank.test("ok"));
        System.out.println();
    }

    // Example 10: Currying and partial application
    private static void example10_CurryingAndPartialApplication() {
        System.out.println("Example 10: Currying and Partial Application");

        BiFunction<Integer, Integer, Integer> add = Integer::sum;
        Function<Integer, Function<Integer, Integer>> curriedAdd = curry(add);
        Function<Integer, Integer> add10 = curriedAdd.apply(10); // partial application
        System.out.println("add10(5) = " + add10.apply(5));

        // Primitive specializations avoid boxing
        IntFunction<IntUnaryOperator> intCurriedAdd = a -> b -> a + b;
        IntUnaryOperator add3 = intCurriedAdd.apply(3);
        System.out.println("add3(7) = " + add3.applyAsInt(7));
        System.out.println();
    }

    // Example 11: Checked exceptions with lambdas (wrapping to unchecked)
    private static void example11_CheckedExceptionsInLambdas() {
        System.out.println("Example 11: Checked Exceptions in Lambdas");

        Function<String, Integer> safe = unchecked(_02_Examples::mightThrowChecked);
        System.out.println("safe(\"ok\") = " + safe.apply("ok"));

        try {
            safe.apply("bad");
        } catch (RuntimeException e) {
            System.out.println("caught runtime (cause): " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
        }
        System.out.println();
    }

    // Example 12: Streams with lambdas
    private static void example12_StreamsWithLambdas() {
        System.out.println("Example 12: Streams with Lambdas");

        List<String> names = Arrays.asList("alice", "bob", "", "ALICE", "  ", "bob  ");
        List<String> normalized = names.stream()
                .map(s -> s == null ? "" : s)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        System.out.println("normalized = " + normalized);

        int totalLen = normalized.stream().mapToInt(String::length).sum();
        System.out.println("totalLen = " + totalLen);

        List<Person> persons = Arrays.asList(
                new Person("Ann", 21),
                new Person("Ben", 27),
                new Person("Cal", 33),
                new Person("Dan", 39),
                new Person("Eve", 27)
        );
        Map<Integer, List<Person>> byDecade = persons.stream()
                .collect(Collectors.groupingBy(p -> (p.getAge() / 10) * 10));
        System.out.println("grouped by decade = " + byDecade);
        System.out.println();
    }

    // Example 13: Overload resolution and target typing with lambdas
    private static void example13_OverloadResolutionAndTargetTyping() {
        System.out.println("Example 13: Overload Resolution & Target Typing");

        // Chooses Callable because lambda returns a value
        String res1 = use(() -> "done");

        // Chooses Runnable because lambda body is void
        String res2 = use(() -> System.out.println("working"));

        // Ambiguous example (would not compile): use(() -> {}); // both Runnable and Callable could match
        // Disambiguate with a cast
        String res3 = use((Callable<String>) (() -> "casted"));

        System.out.println(res1);
        System.out.println(res2);
        System.out.println(res3);
        System.out.println();
    }

    // Utility: custom functional interface (single abstract method)
    @FunctionalInterface
    interface IntOperation {
        int apply(int a, int b); // SAM

        // Default method allowed in a functional interface
        default IntOperation andThen(IntUnaryOperator after) {
            Objects.requireNonNull(after);
            return (a, b) -> after.applyAsInt(apply(a, b));
        }

        // Static helper factory
        static IntOperation add() {
            return (a, b) -> a + b;
        }
    }

    // Utility: sample class for method-reference examples
    static final class Person {
        private final String name;
        private final int age;

        public Person() { this("Unknown", 0); }
        public Person(String name) { this(name, 0); }
        public Person(String name, int age) { this.name = name; this.age = age; }

        public String getName() { return name; }
        public int getAge() { return age; }
        @Override public String toString() { return name + "(" + age + ")"; }
    }

    // Higher-order function utilities
    static Predicate<Integer> greaterThan(int n) {
        return x -> x > n;
    }

    static <T> Predicate<T> notNullAnd(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate);
        return t -> t != null && predicate.test(t);
    }

    static <A, B, R> Function<A, Function<B, R>> curry(BiFunction<A, B, R> f) {
        Objects.requireNonNull(f);
        return a -> b -> f.apply(a, b);
    }

    // Checked-exception-friendly functional interface and wrapper
    @FunctionalInterface
    interface ThrowingFunction<T, R, E extends Exception> {
        R apply(T t) throws E;
    }

    static <T, R> Function<T, R> unchecked(ThrowingFunction<T, R, ?> fn) {
        Objects.requireNonNull(fn);
        return t -> {
            try {
                return fn.apply(t);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    static int mightThrowChecked(String s) throws Exception {
        if ("bad".equals(s)) throw new Exception("boom");
        return s.length();
    }

    // Overload resolution demo targets
    static String use(Runnable r) {
        r.run();
        return "Runnable chosen";
    }

    static String use(Callable<String> c) {
        try {
            return "Callable chosen: " + c.call();
        } catch (Exception e) {
            return "Callable threw: " + e.getMessage();
        }
    }
}