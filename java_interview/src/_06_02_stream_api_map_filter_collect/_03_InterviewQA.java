package _06_02_stream_api_map_filter_collect;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.*;

/*
Interview Q&A: Stream API focus on map, filter, collect

1) What is a Stream?
   - A sequence of elements supporting aggregate operations. Not a data structure; it doesn’t store elements.
   - Single-use: once a terminal operation executes, the stream is consumed.
   - Lazily evaluated: intermediate operations run only when a terminal operation is invoked.

2) map vs filter
   - map(T -> R): transforms each element.
   - filter(T -> boolean): keeps elements that satisfy a predicate.

3) Intermediate vs Terminal operations
   - Intermediate: return a Stream (e.g., map, filter). Lazy.
   - Terminal: produce a non-stream result (e.g., collect, count, findFirst).

4) What does collect do?
   - Terminal reduction into a container/result via a Collector.
   - Two main forms:
     a) collect(Collector): use ready-made collectors (toList, groupingBy, etc.).
     b) collect(supplier, accumulator, combiner): custom mutable reduction.

5) Laziness and short-circuiting
   - Streams process elements on-demand. Short-circuit terminals (findFirst, anyMatch, limit) stop early when possible.

6) Order of operations (performance)
   - Prefer filtering before mapping when possible to process fewer elements.

7) toMap pitfalls
   - Duplicate keys throw IllegalStateException unless a merge function is provided.
   - Use a map supplier for desired Map type (e.g., LinkedHashMap to keep encounter order).

8) Grouping and downstream collectors
   - groupingBy(classifier, downstream): multi-stage reductions (mapping, counting, toSet, etc.).

9) reduce vs collect
   - reduce: for immutable reduction (sum, product). Avoid mutating containers inside reduce.
   - collect: for building mutable containers (lists, maps, sets).

10) Primitive streams
   - mapToInt/mapToLong/mapToDouble avoid boxing; more efficient for numeric reductions (sum, average).

11) Side-effects
   - Avoid mutating external state inside stream operations, especially in parallel.

12) Stateful operations
   - sorted, distinct are stateful (may buffer). Can affect performance.

13) Parallel streams and ordering
   - findAny is faster than findFirst in parallel (less ordering constraints).
   - forEachOrdered preserves order; forEach may not.

14) Handling nulls
   - Streams can contain nulls; many operations/lambdas must guard against them (filter(Objects::nonNull)).

15) Immutability of collected results
   - Collectors.toList() returns a mutable List (unspecified implementation).
   - To make unmodifiable: collectingAndThen(downstream, Collections::unmodifiableList).

Note: Code uses Java 8-compatible APIs. Newer APIs (Stream.toList, Collectors.toUnmodifiableList, teeing, mapMulti) are mentioned only in comments.
*/
public class _03_InterviewQA {

    public static void main(String[] args) {
        basicMapFilterCollect();
        lazyAndShortCircuit();
        orderMattersForPerformance();
        mapVsFlatMap();
        collectBasics();
        collectToMapWithMergeAndSupplier();
        groupingAndPartitioning();
        collectThreeArgForm();
        customCollectorAverageLength();
        reduceVsCollect();
        primitiveStreams();
        avoidSideEffects();
        nullHandling();
        parallelFindFirstVsFindAny();
        statefulOperations();
        immutableCollectResults();
    }

    // Sample data
    private static List<String> names() {
        return Arrays.asList("alice", "bob", "charlie", "d", "eve", "bob", "anna", "brad", "carol", "dave", "eric");
    }

    private static void printSection(String title) {
        System.out.println("\n--- " + title + " ---");
    }

    // Q: Show basic usage of map, filter, collect.
    private static void basicMapFilterCollect() {
        printSection("Basic: map + filter + collect");
        List<String> names = names();
        // Keep names with length >= 3, map to lengths, collect to a List
        List<Integer> lengths = names.stream()
                .filter(s -> s.length() >= 3)
                .map(String::length)
                .collect(Collectors.toList());
        System.out.println("Lengths (len >= 3): " + lengths);
    }

    // Q: Demonstrate laziness and short-circuiting using limit/findFirst.
    private static void lazyAndShortCircuit() {
        printSection("Laziness + Short-circuit (limit)");
        List<String> names = names();
        List<String> result = names.stream()
                .filter(s -> {
                    System.out.println("filter: " + s);
                    return s.length() > 3;
                })
                .map(s -> {
                    System.out.println("map: " + s);
                    return s.toUpperCase();
                })
                .limit(2) // short-circuits after 2 qualifying elements
                .collect(Collectors.toList());
        System.out.println("Collected: " + result);
    }

    // Q: Does operation order matter for performance?
    private static void orderMattersForPerformance() {
        printSection("Order matters (filter before map vs map before filter)");
        List<String> names = names();

        AtomicInteger mapCountFirst = new AtomicInteger();
        AtomicInteger filterCountFirst = new AtomicInteger();
        List<String> a = names.stream()
                .map(s -> {
                    mapCountFirst.incrementAndGet();
                    return s.toUpperCase(); // pretend expensive
                })
                .filter(s -> {
                    filterCountFirst.incrementAndGet();
                    return s.length() > 3;
                })
                .collect(Collectors.toList());

        AtomicInteger mapCountSecond = new AtomicInteger();
        AtomicInteger filterCountSecond = new AtomicInteger();
        List<String> b = names.stream()
                .filter(s -> {
                    filterCountSecond.incrementAndGet();
                    return s.length() > 3;
                })
                .map(s -> {
                    mapCountSecond.incrementAndGet();
                    return s.toUpperCase(); // only for kept elements
                })
                .collect(Collectors.toList());

        System.out.println("map->filter counts: map=" + mapCountFirst.get() + ", filter=" + filterCountFirst.get() + ", size=" + a.size());
        System.out.println("filter->map counts: map=" + mapCountSecond.get() + ", filter=" + filterCountSecond.get() + ", size=" + b.size());
    }

    // Q: map vs flatMap
    private static void mapVsFlatMap() {
        printSection("map vs flatMap");
        List<List<Integer>> nested = Arrays.asList(
                Arrays.asList(1, 2),
                Arrays.asList(3),
                Arrays.asList(4, 5, 6)
        );

        // map produces Stream<Stream<Integer>>
        long countOfInnerStreams = nested.stream().map(List::stream).count();
        // flatMap flattens to Stream<Integer>
        List<Integer> flattened = nested.stream().flatMap(List::stream).collect(Collectors.toList());

        System.out.println("map -> count of inner streams: " + countOfInnerStreams);
        System.out.println("flatMap -> flattened: " + flattened);
    }

    // Q: Common collectors: toList, toSet, joining, counting, averaging, summarizing
    private static void collectBasics() {
        printSection("Collectors basics");
        List<String> names = names();

        List<String> list = names.stream().collect(Collectors.toList());
        Set<String> linkedSet = names.stream().collect(Collectors.toCollection(LinkedHashSet::new)); // preserves encounter order
        String joined = names.stream().filter(s -> s.length() > 3).collect(Collectors.joining(", "));
        long count = names.stream().filter(s -> s.startsWith("b")).collect(Collectors.counting());
        double avgLen = names.stream().collect(Collectors.averagingInt(String::length));
        IntSummaryStatistics stats = names.stream().collect(Collectors.summarizingInt(String::length));

        System.out.println("toList: " + list);
        System.out.println("toCollection(LinkedHashSet): " + linkedSet);
        System.out.println("joining (>3): " + joined);
        System.out.println("count startsWith 'b': " + count);
        System.out.println("averagingInt length: " + avgLen);
        System.out.println("summarizingInt: " + stats);
    }

    // Q: toMap: handle duplicates and control Map type
    private static void collectToMapWithMergeAndSupplier() {
        printSection("toMap: merge function + map supplier");
        List<String> names = names();
        // Key: first char; keep the longer name when keys collide; maintain encounter order via LinkedHashMap
        Map<Character, String> bestByInitial = names.stream().collect(
                Collectors.toMap(
                        s -> s.charAt(0),
                        Function.identity(),
                        (a, b) -> a.length() >= b.length() ? a : b,
                        LinkedHashMap::new
                )
        );
        System.out.println("Best by initial (longest): " + bestByInitial);
        // Note: Without the merge function, duplicates would cause IllegalStateException.
    }

    // Q: groupingBy, partitioningBy, downstream collectors
    private static void groupingAndPartitioning() {
        printSection("groupingBy + downstream collectors; partitioningBy");
        List<String> names = names();

        Map<Integer, List<String>> byLength = names.stream().collect(Collectors.groupingBy(String::length));
        Map<Integer, Long> freqByLength = names.stream().collect(Collectors.groupingBy(String::length, Collectors.counting()));
        Map<Character, Set<String>> upperByInitialSorted =
                names.stream().collect(Collectors.groupingBy(
                        s -> s.charAt(0),
                        LinkedHashMap::new,
                        Collectors.mapping(String::toUpperCase, Collectors.toCollection(TreeSet::new))
                ));
        Map<Boolean, List<String>> partition = names.stream().collect(Collectors.partitioningBy(s -> s.length() > 3));

        // Unmodifiable lists per group
        Map<Integer, List<String>> unmodByLen = names.stream().collect(Collectors.groupingBy(
                String::length,
                Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList)
        ));

        System.out.println("groupingBy length: " + byLength);
        System.out.println("groupingBy length (counting): " + freqByLength);
        System.out.println("groupingBy initial -> UPPER TreeSet: " + upperByInitialSorted);
        System.out.println("partitioningBy (>3): " + partition);
        System.out.println("groupingBy length (unmodifiable lists): " + unmodByLen);
    }

    // Q: collect(supplier, accumulator, combiner) form
    private static void collectThreeArgForm() {
        printSection("collect(supplier, accumulator, combiner)");
        List<String> names = names();

        // Collect to a LinkedList with uppercase filtered elements
        LinkedList<String> upper = names.stream()
                .filter(s -> s.length() > 3)
                .map(String::toUpperCase)
                .collect(LinkedList::new, List::add, List::addAll);

        // Collect to a TreeSet (naturally sorted, deduplicated)
        TreeSet<String> tree = names.stream()
                .map(String::toUpperCase)
                .collect(TreeSet::new, Set::add, Set::addAll);

        System.out.println("LinkedList via 3-arg collect: " + upper);
        System.out.println("TreeSet via 3-arg collect: " + tree);
    }

    // Q: Build a custom Collector (average length)
    private static void customCollectorAverageLength() {
        printSection("Custom Collector: average length");
        List<String> names = names();

        Collector<String, Avg, Double> avgLenCollector = Collector.of(
                Avg::new,
                (acc, s) -> acc.add(s.length()),
                (a, b) -> { a.combine(b); return a; },
                Avg::average
        );

        double avg = names.stream().collect(avgLenCollector);
        System.out.println("Average length (custom collector): " + avg);
    }

    // Q: reduce vs collect
    private static void reduceVsCollect() {
        printSection("reduce vs collect");
        List<Integer> nums = Arrays.asList(1, 2, 3, 4, 5);

        int sumReduce = nums.stream().reduce(0, Integer::sum); // good use
        // Anti-pattern (mutating container inside reduce) – shown for contrast:
        // String bad = nums.stream().reduce(new StringBuilder(), (sb, n) -> sb.append(n), StringBuilder::append).toString();

        String joinedWithCollector = nums.stream().map(String::valueOf).collect(Collectors.joining(","));
        String joinedWith3ArgCollect = nums.stream().collect(
                StringBuilder::new,
                (sb, n) -> { if (sb.length() > 0) sb.append(","); sb.append(n); },
                StringBuilder::append
        ).toString();

        System.out.println("reduce sum: " + sumReduce);
        System.out.println("joining via collector: " + joinedWithCollector);
        System.out.println("joining via 3-arg collect: " + joinedWith3ArgCollect);
    }

    // Q: Primitive streams (avoid boxing)
    private static void primitiveStreams() {
        printSection("Primitive streams (mapToInt)");
        List<String> names = names();

        int totalLen = names.stream().mapToInt(String::length).sum();
        OptionalDouble avgLen = names.stream().mapToInt(String::length).average();
        IntSummaryStatistics stats = names.stream().mapToInt(String::length).summaryStatistics();

        System.out.println("sum of lengths: " + totalLen);
        System.out.println("avg length: " + (avgLen.isPresent() ? avgLen.getAsDouble() : "n/a"));
        System.out.println("stats: " + stats);
    }

    // Q: Side-effects vs pure reductions
    private static void avoidSideEffects() {
        printSection("Avoid side-effects in stream operations");
        List<String> names = names();

        // Side-effecting approach (works sequentially, risky in parallel):
        List<String> target = new ArrayList<>();
        names.stream().filter(s -> s.length() > 3).map(String::toUpperCase).forEach(target::add);
        // Preferred: use collect
        List<String> safe = names.stream().filter(s -> s.length() > 3).map(String::toUpperCase).collect(Collectors.toList());

        System.out.println("Side-effect list (sequential ok): " + target);
        System.out.println("Collected list (preferred): " + safe);
    }

    // Q: Handling nulls with filter(Objects::nonNull)
    private static void nullHandling() {
        printSection("Null handling");
        List<String> withNulls = Arrays.asList("a", null, "bb", null, "ccc");

        List<Integer> lengths = withNulls.stream()
                .filter(Objects::nonNull)
                .map(String::length)
                .collect(Collectors.toList());

        System.out.println("Lengths (nulls removed): " + lengths);
    }

    // Q: Parallel: findAny vs findFirst; forEach vs forEachOrdered
    private static void parallelFindFirstVsFindAny() {
        printSection("Parallel: findAny vs findFirst");
        List<String> names = names();

        String any = names.parallelStream().filter(s -> s.length() > 1).findAny().orElse("none");
        String first = names.parallelStream().filter(s -> s.length() > 1).findFirst().orElse("none");

        System.out.println("findAny (parallel): " + any);
        System.out.println("findFirst (parallel, ordered): " + first);
    }

    // Q: Stateful operations (distinct, sorted)
    private static void statefulOperations() {
        printSection("Stateful operations: distinct, sorted");
        List<String> names = names(); // contains "bob" twice

        List<String> distinct = names.stream().distinct().collect(Collectors.toList()); // preserves encounter order
        List<String> sortedByLenThenAlpha = names.stream()
                .sorted(Comparator.comparingInt(String::length).thenComparing(Comparator.naturalOrder()))
                .collect(Collectors.toList());

        System.out.println("distinct (ordered): " + distinct);
        System.out.println("sorted by length then alpha: " + sortedByLenThenAlpha);
    }

    // Q: Make collected results unmodifiable
    private static void immutableCollectResults() {
        printSection("Immutable collected results");
        List<String> names = names();

        List<String> unmod = names.stream()
                .map(String::toUpperCase)
                .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));

        System.out.println("Unmodifiable list: " + unmod);
        // Note: Stream.toList() (Java 16+) returns unmodifiable, but is not used here for Java 8 compatibility.
    }

    // Helper for custom collector
    private static class Avg {
        long sum;
        long count;
        void add(int len) { sum += len; count++; }
        void combine(Avg other) { sum += other.sum; count += other.count; }
        double average() { return count == 0 ? 0.0 : (double) sum / count; }
    }
}