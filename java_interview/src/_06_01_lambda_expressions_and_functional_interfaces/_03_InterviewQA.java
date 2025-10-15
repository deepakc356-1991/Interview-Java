package _06_01_lambda_expressions_and_functional_interfaces;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;
import java.util.function.LongSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * 03_InterviewQA - Lambda Expressions & Functional Interfaces
 * Package: _06_01_lambda_expressions_and_functional_interfaces
 *
 * This file contains a curated set of interview Q&A (basic -> intermediate -> advanced)
 * explained via comments and executable Java examples.
 *
 * Run main() to see selected demonstrations in action.
 */
public class _03_InterviewQA {

    public static void main(String[] args) {
        System.out.println("=== Lambda & Functional Interfaces: Interview Q&A Demos ===");

        q1_whatIsLambda();
        q2_whatIsFunctionalInterface();
        q3_whyFunctionalInterfaceAnnotation();
        q4_syntaxAndTargetTyping();
        q5_lambdaVsAnonymousClass();
        q6_variableCaptureEffectivelyFinal();
        q7_checkedExceptionsInLambdas();
        q8_methodReferences();
        q9_commonFunctionalInterfaces();
        q10_streamsAndLambdas();
        q11_defaultMethodsAndDiamond();
        q12_overloadingAmbiguity();
        q13_genericLambdasAndVarParams();
        q14_curryingPartialApplication();
        q15_recursionWithLambdas();
        q16_primitiveSpecializationsPerformanceNotes();
        q17_lambdaIdentityEqualitySerializationNotes();
        q18_intersectionTypes();
        q19_comparatorComposition();
        q20_optionalWithLambdas();
        q21_mapEnhancementsWithLambdas();
        q22_groupingAndSummarizing();
        q23_flatMapVsMap();
        q24_sideEffectsParallelStreams();
        q25_operatorFactoriesAndComposition();
        q26_customFunctionalInterfaceFeatures();
        q27_rethrowAndWrappingCheckedExceptions();
        q28_capturingVsNonCapturingNotes();
        q29_streamShortCircuiting();
        q30_streamCollectorsAdvanced();

        System.out.println("=== Done ===");
    }

    // Q1: What is a lambda expression?
    private static void q1_whatIsLambda() {
        /*
         Q: What is a lambda expression in Java?
         A: A concise way to represent an anonymous function (a block of code with parameters and a body)
            that can be passed around, assigned to variables, and executed later. It targets a functional
            interface (an interface with exactly one abstract method).
         Syntax examples:
           - (parameters) -> expression
           - (parameters) -> { statements; return value; }
        */
        Runnable r = () -> System.out.println("[Q1] Hello from a lambda!");
        r.run();

        // With parameters:
        IntBinaryOperator add = (a, b) -> a + b;
        System.out.println("[Q1] 2 + 3 = " + add.applyAsInt(2, 3));
    }

    // Q2: What is a functional interface?
    private static void q2_whatIsFunctionalInterface() {
        /*
         Q: What is a functional interface?
         A: An interface with exactly one abstract method (SAM - Single Abstract Method). It may contain
            default and static methods. Examples: Runnable, Callable, Comparator, java.util.function.* types.
        */
        // Custom functional interface demo:
        MathOp sum = (a, b) -> a + b;
        MathOp mul = (a, b) -> a * b;
        System.out.println("[Q2] 5 + 7 = " + sum.apply(5, 7));
        System.out.println("[Q2] 5 * 7 = " + mul.apply(5, 7));
        System.out.println("[Q2] MathOp.identity(9) = " + MathOp.identity().apply(9, 999));
    }

    // Q3: Why @FunctionalInterface?
    private static void q3_whyFunctionalInterfaceAnnotation() {
        /*
         Q: Why use @FunctionalInterface annotation?
         A: It enforces at compile-time that the interface has exactly one abstract method. It improves
            clarity and prevents accidental addition of abstract methods that would break lambdas.
        */
        // See @FunctionalInterface usage on MathOp below. This compiles only if the contract is respected.
        System.out.println("[Q3] @FunctionalInterface ensures SAM contract at compile time.");
    }

    // Q4: Syntax, target typing, type inference
    private static void q4_syntaxAndTargetTyping() {
        /*
         Q: How are lambda types inferred?
         A: "Target typing" — the context (assignment, parameter) provides the expected functional interface type.
            Parameter and return types are inferred accordingly.
        */
        // Context expects a Predicate<String>
        Predicate<String> isEmpty = s -> s.isEmpty();
        System.out.println("[Q4] Is empty? " + isEmpty.test(""));

        // Parentheses optional for single parameter; braces/return required for multiple statements:
        Function<Integer, Integer> square = x -> {
            int y = x * x;
            return y;
        };
        System.out.println("[Q4] square(6) = " + square.apply(6));
    }

    // Q5: Lambda vs Anonymous Inner Class
    private static void q5_lambdaVsAnonymousClass() {
        /*
         Q: Differences between lambda and anonymous inner class?
         A:
           - 'this' in a lambda refers to the enclosing instance; in an anonymous class, 'this' refers to the anon instance.
           - Lambdas don't introduce a new scope for 'this' and cannot shadow variables the way inner classes can.
           - Lambdas are more lightweight; compiled using invokedynamic.
        */
        class Demo {
            void show() {
                Runnable lambda = () -> System.out.println("[Q5][lambda] this == Demo? " + (this instanceof Demo));
                Runnable anon = new Runnable() {
                    @Override public void run() {
                        System.out.println("[Q5][anon] this == Demo? " + (this instanceof Demo));
                    }
                };
                lambda.run(); // true
                anon.run();   // false
            }
        }
        new Demo().show();
    }

    // Q6: Variable capture and effectively final
    private static void q6_variableCaptureEffectivelyFinal() {
        /*
         Q: What is variable capture and 'effectively final'?
         A: Lambdas can capture (read) local variables from the enclosing scope, but only if they are effectively final
            (not modified after assignment). To mutate state, use a mutable holder like AtomicInteger or a field.
        */
        int base = 10; // effectively final
        IntUnaryOperator plusBase = x -> x + base;
        System.out.println("[Q6] 5 + base(10) = " + plusBase.applyAsInt(5));

        // To mutate during lambda operations:
        AtomicInteger acc = new AtomicInteger();
        IntStream.rangeClosed(1, 5).forEach(i -> acc.addAndGet(i));
        System.out.println("[Q6] Sum 1..5 via AtomicInteger = " + acc.get());

        // Not allowed (won't compile):
        // int n = 0;
        // Runnable r = () -> n++; // error: local variables referenced from a lambda must be final or effectively final
    }

    // Q7: Checked exceptions in lambdas
    private static void q7_checkedExceptionsInLambdas() {
        /*
         Q: How do lambdas handle checked exceptions?
         A: Standard functional interfaces don't declare checked exceptions. Options:
            - Catch inside the lambda.
            - Create a custom "throwing" functional interface.
            - Wrap and rethrow as unchecked (RuntimeException).
        */
        // Catching inside:
        Function<String, Integer> parseSafely = s -> {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                return -1;
            }
        };
        System.out.println("[Q7] parseSafely('42') = " + parseSafely.apply("42") + ", parseSafely('x') = " + parseSafely.apply("x"));

        // Custom throwing functional interface + wrapper:
        Function<String, Integer> len = wrap((ThrowingFunction<String, Integer, IOException>) s -> {
            if (s == null) throw new IOException("null!");
            return s.length();
        });
        System.out.println("[Q7] length('abc') via wrapper = " + len.apply("abc"));
        try {
            len.apply(null);
        } catch (RuntimeException ex) {
            System.out.println("[Q7] Wrapped exception class = " + ex.getClass().getSimpleName() + " cause=" + ex.getCause());
        }
    }

    // Q8: Method references
    private static void q8_methodReferences() {
        /*
         Q: Types of method references?
         A:
           - Static: TypeName::staticMethod
           - Instance on particular object: instanceRef::instanceMethod
           - Instance on arbitrary object of a type: TypeName::instanceMethod
           - Constructor: TypeName::new
        */
        // Static
        Function<String, Integer> parseInt = Integer::parseInt;
        // Particular object
        StringJoiner joiner = new StringJoiner(",");
        Consumer<String> adder = joiner::add;
        // Arbitrary object of a type
        Predicate<String> isEmpty = String::isEmpty;
        // Constructor
        Supplier<List<String>> listCtor = ArrayList::new;

        adder.accept("a"); adder.accept("b"); adder.accept("c");
        System.out.println("[Q8] parseInt('123')=" + parseInt.apply("123") + ", isEmpty('')=" + isEmpty.test("") + ", joiner=" + joiner + ", newList.size=" + listCtor.get().size());
    }

    // Q9: Common functional interfaces (Predicate, Function, Consumer, Supplier, Operators)
    private static void q9_commonFunctionalInterfaces() {
        /*
         Q: Name common java.util.function interfaces and usage.
         A: Predicate, Function, Consumer, Supplier, UnaryOperator, BinaryOperator; Bi- variants; primitive specializations.
        */
        Predicate<String> longerThan3 = s -> s.length() > 3;
        Predicate<String> startsWithA = s -> s.startsWith("A");
        Predicate<String> complex = longerThan3.and(startsWithA).negate();
        System.out.println("[Q9] complex('Abcd')=" + complex.test("Abcd") + ", complex('Xbcd')=" + complex.test("Xbcd"));

        Function<String, Integer> length = String::length;
        Function<Integer, String> toString = Object::toString;
        Function<String, String> lenThenToString = length.andThen(toString);
        System.out.println("[Q9] 'hello' -> lenThenToString -> " + lenThenToString.apply("hello"));

        Consumer<String> printer = System.out::println;
        printer.accept("[Q9] Printed via Consumer");

        Supplier<Double> random = Math::random;
        System.out.println("[Q9] Supplier random = " + random.get());

        UnaryOperator<String> upper = String::toUpperCase;
        BinaryOperator<Integer> max = Integer::max;
        System.out.println("[Q9] Upper('hi')=" + upper.apply("hi") + ", max(3,7)=" + max.apply(3, 7));

        // Primitive specializations avoid boxing:
        IntPredicate even = x -> (x & 1) == 0;
        IntUnaryOperator inc = x -> x + 1;
        System.out.println("[Q9] even(10)=" + even.test(10) + ", inc(10)=" + inc.applyAsInt(10));
    }

    // Q10: Streams + Lambdas
    private static void q10_streamsAndLambdas() {
        /*
         Q: How do lambdas integrate with streams?
         A: Provide behaviors to stream operations (map, filter, reduce, collect). Ensure side-effect-free functions for predictability.
        */
        List<String> words = Arrays.asList("apple", "banana", "apricot", "avocado");
        List<String> aWordsUpper =
                words.stream().filter(w -> w.startsWith("a")).map(String::toUpperCase).collect(Collectors.toList());
        System.out.println("[Q10] aWordsUpper = " + aWordsUpper);

        int totalLength = words.stream().mapToInt(String::length).sum();
        System.out.println("[Q10] totalLength = " + totalLength);

        String joined = words.stream().collect(Collectors.joining(","));
        System.out.println("[Q10] joined = " + joined);
    }

    // Q11: Default methods and diamond problem
    private static void q11_defaultMethodsAndDiamond() {
        /*
         Q: How are conflicts between default methods resolved?
         A: If a class implements two interfaces with the same default method signature, it must override and resolve,
            optionally delegating using InterfaceName.super.method().
        */
        ConflictResolver resolver = new ConflictResolver();
        resolver.greet();
    }

    // Q12: Overloading with lambdas, ambiguity
    private static void q12_overloadingAmbiguity() {
        /*
         Q: How is method overloading resolved for lambdas?
         A: Based on target type. If ambiguous, you must cast or provide explicit types to disambiguate.
        */
        System.out.println("[Q12] doSomething with Consumer:");
        doSomething((Consumer<String>) s -> System.out.println("Consumed: " + s));
        System.out.println("[Q12] doSomething with Function:");
        doSomething((Function<String, String>) s -> s.toUpperCase());
    }

    // Q13: Generic lambdas, 'var' in parameters, annotations
    private static void q13_genericLambdasAndVarParams() {
        /*
         Q: Can lambda parameters be annotated and/or use 'var'?
         A: Yes (Java 11+ for var). All parameters must consistently use 'var' or none. Useful for annotations too.
         Note: Not using 'var' here to maintain compatibility with earlier Java versions. Example (commented):
           // BiFunction<Integer, Integer, Integer> add = (@NotNull var a, @NotNull var b) -> a + b;
        */
        BiFunction<Integer, Integer, Integer> add = (a, b) -> a + b;
        System.out.println("[Q13] add(2,3) = " + add.apply(2, 3));
    }

    // Q14: Currying and partial application
    private static void q14_curryingPartialApplication() {
        /*
         Q: How to implement currying/partial application with lambdas?
         A: Return functions from functions.
        */
        BiFunction<Integer, Integer, Integer> add2 = Integer::sum;
        Function<Integer, Function<Integer, Integer>> curriedAdd = a -> (b -> add2.apply(a, b));

        Function<Integer, Integer> addFive = curriedAdd.apply(5);
        System.out.println("[Q14] addFive(10) = " + addFive.apply(10));

        // Partial application using closures:
        Function<Integer, Integer> times10 = partialApplyLeft((a, b) -> a * b, 10);
        System.out.println("[Q14] times10(7) = " + times10.apply(7));
    }

    // Q15: Recursion in lambdas
    private static void q15_recursionWithLambdas() {
        /*
         Q: Can lambdas be recursive?
         A: A lambda cannot directly refer to itself in its own initializer. Use a helper (named method),
            a fixed-point combinator, or a holder reference.
        */
        // Simple approach: use a named method reference
        IntFunction<Long> fact = _03_InterviewQA::factorial;
        System.out.println("[Q15] factorial(5) = " + fact.apply(5));

        // Fixed-point combinator approach (advanced):
        Function<IntFunction<Long>, IntFunction<Long>> F = self -> n -> (n <= 1) ? 1L : n * self.apply(n - 1);
        IntFunction<Long> yFactorial = fix(F);
        System.out.println("[Q15] factorial via fix(6) = " + yFactorial.apply(6));
    }

    // Q16: Primitive specializations and performance notes
    private static void q16_primitiveSpecializationsPerformanceNotes() {
        /*
         Q: Why use primitive specializations (IntStream, IntPredicate, etc.)?
         A: To avoid boxing/unboxing overhead and reduce GC pressure. Prefer them in performance-critical paths.
        */
        IntSummaryStatistics stats = IntStream.rangeClosed(1, 100).filter(x -> (x & 1) == 0).summaryStatistics();
        System.out.println("[Q16] Even 1..100 count=" + stats.getCount() + " sum=" + stats.getSum());
    }

    // Q17: Lambda identity, equality, serialization
    private static void q17_lambdaIdentityEqualitySerializationNotes() {
        /*
         Q: Are lambdas equal or serializable?
         A:
           - Equality: Lambdas have Object identity; equals/hashCode not specified for semantic equality.
           - Serialization: Not guaranteed; unless the targeted functional interface is serializable AND the lambda
             is designed for serialization. Avoid relying on serialization of lambdas.
        */
        Predicate<String> p1 = s -> s.length() > 2;
        Predicate<String> p2 = s -> s.length() > 2;
        System.out.println("[Q17] p1.equals(p2) = " + p1.equals(p2) + " (don't rely on equality)");
    }

    // Q18: Intersection types with lambdas
    private static void q18_intersectionTypes() {
        /*
         Q: What are intersection types for lambdas?
         A: A lambda can be cast to multiple interfaces at once (useful for serialization + behavior).
        */
        boolean result = testSerializablePredicate((Serializable & Predicate<String>) s -> s.contains("X"), "AXY");
        System.out.println("[Q18] Serializable & Predicate test = " + result);
    }

    // Q19: Comparator composition and method references
    private static void q19_comparatorComposition() {
        /*
         Q: How to build complex comparators with lambdas?
         A: Use Comparator.comparing, thenComparing, reversed, nullsFirst/nullsLast, etc.
        */
        record Person(String name, int age, LocalDate joined) {}
        List<Person> people = new ArrayList<>(List.of(
                new Person("Ana", 30, LocalDate.parse("2021-02-10")),
                new Person("Bob", 25, LocalDate.parse("2023-01-02")),
                new Person("Ana", 25, LocalDate.parse("2020-05-05")),
                new Person("Cara", 30, LocalDate.parse("2022-08-08"))
        ));

        people.sort(
                Comparator.comparing(Person::name)
                          .thenComparing(Person::age)
                          .thenComparing(Person::joined, Comparator.reverseOrder())
        );
        System.out.println("[Q19] Sorted people = " + people);
    }

    // Q20: Optional with lambdas
    private static void q20_optionalWithLambdas() {
        /*
         Q: How does Optional use lambdas?
         A: map, flatMap, filter, orElseGet, ifPresent, ifPresentOrElse are lambda-powered.
        */
        Optional<String> maybeName = Optional.of("delta");
        int len = maybeName.filter(s -> s.startsWith("d")).map(String::length).orElseGet(() -> -1);
        System.out.println("[Q20] Optional length = " + len);

        Optional<String> none = Optional.empty();
        none.ifPresentOrElse(
                s -> System.out.println("[Q20] present: " + s),
                () -> System.out.println("[Q20] nothing present")
        );
    }

    // Q21: Map enhancements using lambdas
    private static void q21_mapEnhancementsWithLambdas() {
        /*
         Q: Map methods using lambdas?
         A: computeIfAbsent, computeIfPresent, merge, replaceAll, forEach.
        */
        Map<String, List<Integer>> m = new ConcurrentHashMap<>();
        m.computeIfAbsent("evens", k -> new ArrayList<>()).addAll(List.of(2, 4));
        m.computeIfAbsent("odds", k -> new ArrayList<>()).add(3);
        m.merge("odds", new ArrayList<>(List.of(5, 7)), (oldV, newV) -> { oldV.addAll(newV); return oldV; });
        m.replaceAll((k, v) -> v.stream().sorted().collect(Collectors.toList()));
        m.forEach((k, v) -> System.out.println("[Q21] " + k + " -> " + v));
    }

    // Q22: Grouping and summarizing with Collectors
    private static void q22_groupingAndSummarizing() {
        /*
         Q: How to group and summarize stream results?
         A: Use Collectors.groupingBy, partitioningBy, mapping, counting, summing, summarizing, collectingAndThen.
        */
        List<String> words = List.of("a", "bb", "ccc", "dddd", "ee", "f");
        Map<Integer, Long> byLengthCount = words.stream().collect(Collectors.groupingBy(String::length, Collectors.counting()));
        System.out.println("[Q22] byLengthCount = " + byLengthCount);

        Map<Boolean, List<String>> partition = words.stream().collect(Collectors.partitioningBy(s -> s.length() % 2 == 0));
        System.out.println("[Q22] partition evenLen=" + partition.get(true) + " oddLen=" + partition.get(false));

        DoubleSummaryStatistics stats = Stream.of(1.2, 3.4, 5.6).collect(Collectors.summarizingDouble(Double::doubleValue));
        System.out.println("[Q22] double stats avg=" + stats.getAverage() + " sum=" + stats.getSum());
    }

    // Q23: flatMap vs map
    private static void q23_flatMapVsMap() {
        /*
         Q: Difference between map and flatMap?
         A: map transforms each element to one element; flatMap flattens nested streams (or containers) into one stream.
        */
        List<List<Integer>> nested = List.of(List.of(1, 2), List.of(3), List.of(4, 5));
        List<Integer> flattened = nested.stream().flatMap(List::stream).collect(Collectors.toList());
        System.out.println("[Q23] flattened = " + flattened);
    }

    // Q24: Side effects with parallel streams
    private static void q24_sideEffectsParallelStreams() {
        /*
         Q: Are side-effects safe with parallel streams?
         A: Avoid shared mutable state. Use thread-safe collectors or reduction. Non-deterministic interleaving and ordering can occur.
        */
        List<Integer> data = IntStream.rangeClosed(1, 10).boxed().collect(Collectors.toList());
        List<Integer> out = new ArrayList<>();
        data.parallelStream().forEach(out::add); // Not thread-safe; for demo only.
        System.out.println("[Q24] out size (race-prone) = " + out.size() + " (unsafe example)");
        List<Integer> safe = data.parallelStream().collect(Collectors.toList());
        System.out.println("[Q24] safe collected size = " + safe.size());
    }

    // Q25: Operator factories and composition
    private static void q25_operatorFactoriesAndComposition() {
        /*
         Q: How to compose or build predicates/functions?
         A: Use andThen/compose for Function; and/or/negate for Predicate; minBy/maxBy for BinaryOperator.
        */
        Predicate<String> nonNull = Objects::nonNull;
        Predicate<String> nonEmpty = s -> !s.isEmpty();
        Predicate<String> valid = nonNull.and(nonEmpty);
        System.out.println("[Q25] valid('') = " + valid.test(""));

        BinaryOperator<String> longer = BinaryOperator.maxBy(Comparator.comparingInt(String::length));
        System.out.println("[Q25] longer('aa','bbb') = " + longer.apply("aa", "bbb"));

        Function<Integer, Integer> times2 = x -> x * 2;
        Function<Integer, Integer> minus3 = x -> x - 3;
        System.out.println("[Q25] compose: (x*2)->-3 on 10 = " + minus3.compose(times2).apply(10)); // (10*2)-3
    }

    // Q26: Custom functional interface with default/static methods
    private static void q26_customFunctionalInterfaceFeatures() {
        /*
         Q: Can functional interfaces have default and static methods?
         A: Yes. Only one abstract method is allowed; default/static don't count towards that total.
        */
        StrTransformer trim = String::trim;
        System.out.println("[Q26] trim('  hi  ') = '" + trim.transform("  hi  ") + "'");
        System.out.println("[Q26] StrTransformer.isBlank('  ') = " + StrTransformer.isBlank("  "));
        System.out.println("[Q26] StrTransformer.Upper.INSTANCE.transform('abc') = " + StrTransformer.Upper.INSTANCE.transform("abc"));
    }

    // Q27: Rethrow/wrapping checked exceptions helpers
    private static void q27_rethrowAndWrappingCheckedExceptions() {
        /*
         Q: How to adapt throwing lambdas to standard Function?
         A: Provide a utility to wrap checked exceptions into unchecked at the boundary.
        */
        List<String> inputs = List.of("10", "20", "x", "30");
        List<Integer> parsed = inputs.stream().map(wrap(_03_InterviewQA::parseIntThrows)).collect(Collectors.toList());
        System.out.println("[Q27] parsed with wrap = " + parsed);
    }

    // Q28: Capturing vs non-capturing (notes)
    private static void q28_capturingVsNonCapturingNotes() {
        /*
         Q: Capturing vs non-capturing lambdas?
         A:
           - Non-capturing: don't capture external variables; JVM may instantiate them as singletons.
           - Capturing: carry references to captured vars; generally need new instances. Avoid heavy captures in hot paths.
           - Behavior is implementation detail; don't rely on instance equality or class names.
        */
        String suffix = "!";
        Consumer<String> c = s -> System.out.print(""); // non-capturing: body not using external variables.
        Consumer<String> cCap = s -> System.out.print(s + suffix); // capturing 'suffix'
        System.out.println("[Q28] Demonstrated capturing vs non-capturing (see code).");
    }

    // Q29: Stream short-circuiting ops
    private static void q29_streamShortCircuiting() {
        /*
         Q: Short-circuiting operations?
         A: findFirst, findAny, anyMatch, allMatch, noneMatch, limit, takeWhile/dropWhile are short-circuiting.
        */
        boolean anyLongerThan5 = Stream.of("a", "bbbbbb", "ccc").anyMatch(s -> s.length() > 5);
        System.out.println("[Q29] anyLongerThan5 = " + anyLongerThan5);
    }

    // Q30: Advanced collectors with lambdas
    private static void q30_streamCollectorsAdvanced() {
        /*
         Q: Advanced Collectors usage?
         A: collectingAndThen, toMap with merge function, mapping downstream, grouping with TreeMap, etc.
        */
        List<String> items = List.of("a", "bb", "a", "ccc", "bb");
        Map<String, Long> freq = items.stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        System.out.println("[Q30] freq = " + freq);

        Map<Integer, String> byLenJoined = items.stream().collect(
                Collectors.groupingBy(String::length, TreeMap::new, Collectors.mapping(Function.identity(), Collectors.joining("|")))
        );
        System.out.println("[Q30] byLenJoined(TreeMap) = " + byLenJoined);

        Map<String, Integer> toMapMerged = items.stream().collect(
                Collectors.toMap(Function.identity(), s -> 1, Integer::sum)
        );
        System.out.println("[Q30] toMapMerged counts = " + toMapMerged);
    }

    // ------------------------------------------------------------
    // Utilities and supporting code for demos
    // ------------------------------------------------------------

    @FunctionalInterface
    interface MathOp {
        int apply(int a, int b);

        default MathOp andThen(IntUnaryOperator after) {
            Objects.requireNonNull(after);
            return (x, y) -> after.applyAsInt(apply(x, y));
        }

        static MathOp identity() {
            return (a, b) -> a; // return first
        }
    }

    @FunctionalInterface
    interface ThrowingFunction<T, R, E extends Exception> {
        R apply(T t) throws E;
    }

    static <T, R> Function<T, R> wrap(ThrowingFunction<T, R, ? extends Exception> fn) {
        return t -> {
            try {
                return fn.apply(t);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    private static Integer parseIntThrows(String s) throws IOException {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new IOException("Bad int: " + s, e);
        }
    }

    interface ADefault {
        default void greet() {
            System.out.println("[Q11] Hello from ADefault");
        }
    }

    interface BDefault {
        default void greet() {
            System.out.println("[Q11] Hello from BDefault");
        }
    }

    static class ConflictResolver implements ADefault, BDefault {
        @Override
        public void greet() {
            // resolve conflict explicitly:
            ADefault.super.greet();
            BDefault.super.greet();
            System.out.println("[Q11] Conflict resolved in class.");
        }
    }

    // Overloading ambiguity demo
    static void doSomething(Consumer<String> c) {
        c.accept("hello");
    }
    static void doSomething(Function<String, String> f) {
        System.out.println("Result: " + f.apply("hello"));
    }

    // Fixed-point combinator for recursion with lambdas
    static <T, R> Function<T, R> fix(Function<Function<T, R>, Function<T, R>> f) {
        return new Function<>() {
            @Override public R apply(T t) {
                return f.apply(this).apply(t);
            }
        };
    }

    static long factorial(int n) {
        if (n <= 1) return 1;
        return n * factorial(n - 1);
    }

    static <A, B, R> Function<B, R> partialApplyLeft(BiFunction<A, B, R> f, A a) {
        return b -> f.apply(a, b);
    }

    static <T> boolean testSerializablePredicate(Serializable & Predicate<T> p, T v) {
        return p.test(v);
    }

    @FunctionalInterface
    interface StrTransformer {
        String transform(String s);

        // default method
        default StrTransformer andThen(StrTransformer after) {
            Objects.requireNonNull(after);
            return s -> after.transform(transform(s));
        }

        // static helper
        static boolean isBlank(String s) {
            return s == null || s.trim().isEmpty();
        }

        // nested enum as singleton implementation
        enum Upper implements StrTransformer {
            INSTANCE;
            public String transform(String s) { return s == null ? null : s.toUpperCase(); }
        }
    }

    /*
     Additional Notes (non-executable):
     - You cannot redeclare a lambda parameter with the same name as an effectively final local variable in the same scope.
       Example (won't compile):
         String s = "x";
         Function<String, String> f = (s) -> s; // error: variable s is already defined in scope
     - For parallel streams, prefer stateless, associative, non-interfering operations. Avoid shared mutable state.
     - Streams are lazy; lambdas passed to intermediate ops aren't executed until a terminal op is invoked.
     - Use forEachOrdered to preserve encounter order in parallel streams (may reduce parallel performance).
     - Prefer Comparator.thenComparingInt/Long/Double for primitives to avoid boxing.
    */
}