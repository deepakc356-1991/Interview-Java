package _06_03_collectors_and_grouping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.DoubleSummaryStatistics;
import java.util.EnumSet;
import java.util.IntSummaryStatistics;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
 THEORY: Collectors and Grouping (Streams)
 -----------------------------------------
 1) Collector vs Collectors
    - Collector<T, A, R> defines how to fold a stream of T into a result R using a mutable accumulator A.
    - Collectors is a factory/util class with ready-made Collector implementations (toList, groupingBy, counting, etc).

 2) collect terminal operation
    - Stream.collect(Collector) is a general terminal reduction.
    - Many "shortcuts" exist: Collectors.toList(), toSet(), joining(), groupingBy(), partitioningBy(), toMap(), summarizing, averaging, etc.

 3) Common collectors
    - toList/toSet/toCollection: accumulate into collections.
    - joining: join CharSequence elements.
    - counting, summingX, averagingX, summarizingX: numeric aggregation.
    - maxBy/minBy: choose an element by comparator.
    - reducing: general reduction (less used with streams of primitives or specialized collectors).
    - collectingAndThen: post-process a collected result (e.g., wrap unmodifiable).
    - mapping/filtering/flatMapping: transform/filter items within a downstream collector (Java 9+).
    - teeing: combine two collectors into one result (Java 12+).
    - toUnmodifiableX: produce unmodifiable collections (Java 10+).

 4) Grouping
    - groupingBy(classifier) -> Map<K, List<T>>
    - groupingBy(classifier, downstream) -> Map<K, R> where downstream shapes each group's result.
    - groupingBy(classifier, mapFactory, downstream) -> custom Map type (e.g., TreeMap).
    - Multi-level grouping: groupingBy(..., groupingBy(...)) builds nested maps.
    - groupingByConcurrent: concurrent variant for parallel streams using ConcurrentMap (no null keys/values).

 5) Partitioning
    - partitioningBy(predicate) -> Map<Boolean, List<T>>
    - partitioningBy(predicate, downstream) -> Map<Boolean, R>

 6) toMap
    - toMap(keyMapper, valueMapper): requires unique keys; otherwise IllegalStateException.
    - toMap(keyMapper, valueMapper, mergeFn): resolves conflicts.
    - toMap(keyMapper, valueMapper, mergeFn, mapSupplier): choose Map type (LinkedHashMap, TreeMap, etc).
    - toUnmodifiableMap: immutable result (Java 10+).

 7) Collector internals
    - supplier(): create new mutable container (accumulator).
    - accumulator(): incorporate an element into the accumulator.
    - combiner(): merge two accumulators (used in parallel).
    - finisher(): transform accumulator into the final result.
    - characteristics(): Set<Characteristics> flags:
        UNORDERED: order of input doesn't matter.
        CONCURRENT: accumulator can be called from multiple threads concurrently.
        IDENTITY_FINISH: finisher is identity (A == R).
      These guide parallel behavior and performance.

 8) Parallelism
    - In parallel streams, collectors need proper combiner behavior and, if CONCURRENT is used, thread-safe accumulation.
    - groupingByConcurrent yields a ConcurrentMap and can scale in parallel reductions.

 9) Mutability, safety, and pitfalls
    - Do not modify stream sources during collection.
    - Be careful with mutating collected containers after collecting, especially when expecting immutability; prefer toUnmodifiableX or collectingAndThen.
    - groupingBy allows null keys with HashMap; groupingByConcurrent uses ConcurrentMap which forbids null keys/values.
    - Stream.toList() (Java 16+) returns an unmodifiable list, while Collectors.toList() returns a mutable list (implementation-agnostic).

 10) Downstream collector composition
     - Combine groupingBy with mapping, filtering, flatMapping, counting/summing/averaging, etc, to shape results per key.

 Below are practical, runnable examples with inline explanations.
*/
public class _01_Theory {

    enum Gender { MALE, FEMALE, OTHER }

    // Simple domain model for demos.
    record Person(String name, int age, Gender gender, String city, List<String> tags, double salary) {}

    // Immutable sample data.
    static final List<Person> PEOPLE = List.of(
            new Person("Alice", 30, Gender.FEMALE, "NY", List.of("sports", "music"), 90_000),
            new Person("Bob", 40, Gender.MALE, "SF", List.of("music", "tech"), 120_000),
            new Person("Carol", 25, Gender.FEMALE, "NY", List.of("art"), 70_000),
            new Person("David", 35, Gender.MALE, "LA", List.of("sports", "tech"), 110_000),
            new Person("Eve", 30, Gender.FEMALE, "SF", List.of("travel", "food"), 95_000),
            new Person("Frank", 28, Gender.OTHER, "LA", List.of("tech"), 80_000),
            new Person("Grace", 40, Gender.FEMALE, "NY", List.of("food", "tech"), 115_000),
            new Person("Heidi", 50, Gender.FEMALE, "SF", List.of("art", "travel"), 130_000),
            new Person("Ivan", 32, Gender.MALE, "NY", List.of("music", "travel"), 88_000),
            new Person("Judy", 27, Gender.FEMALE, "LA", List.of("food"), 75_000)
    );

    public static void main(String[] args) {
        basics();
        toListSetCollection();
        joiningAndStringCollectors();
        numericAggregations();
        maxMinReducing();
        collectingAndThenExamples();
        toMapExamples();
        partitioningExamples();
        groupingByBasics();
        groupingByDownstream();
        multiLevelGrouping();
        groupingWithCustomMapAndConcurrent();
        flatMappingFilteringMapping();
        teeingExamples();
        customCollectors();
        notes();
    }

    static void basics() {
        System.out.println("=== Basics: collect and simple collectors ===");

        // Collectors.toList(): mutable list (implementation unspecified).
        List<String> names = PEOPLE.stream()
                .map(Person::name)
                .collect(Collectors.toList());

        // Stream.toList(): unmodifiable list (Java 16+). Shown here for awareness; prefer one or the other consistently.
        List<String> namesImmutable = PEOPLE.stream()
                .map(Person::name)
                .toList();

        // Collectors.toSet(): removes duplicates, ordering unspecified.
        Set<String> cities = PEOPLE.stream()
                .map(Person::city)
                .collect(Collectors.toSet());

        // Collectors.toCollection: explicitly choose collection type.
        LinkedHashSet<String> orderedCities = PEOPLE.stream()
                .map(Person::city)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        System.out.println("names size=" + names.size() + ", immutable=" + (namesImmutable.getClass().getName()) +
                ", cities=" + cities + ", orderedCities=" + orderedCities);
    }

    static void toListSetCollection() {
        System.out.println("\n=== toList, toSet, toCollection ===");

        // toList vs toCollection(ArrayList):
        List<Person> list1 = PEOPLE.stream().collect(Collectors.toList());
        ArrayList<Person> list2 = PEOPLE.stream().collect(Collectors.toCollection(ArrayList::new));
        System.out.println("toList impl=" + list1.getClass().getSimpleName() + ", toCollection=ArrayList");

        // toUnmodifiableList (Java 10+): result cannot be modified.
        List<String> unmodifiableNames = PEOPLE.stream()
                .map(Person::name)
                .collect(Collectors.toUnmodifiableList());
        System.out.println("unmodifiableNames=" + unmodifiableNames);
        try {
            unmodifiableNames.add("X");
        } catch (UnsupportedOperationException e) {
            System.out.println("toUnmodifiableList is immutable");
        }

        // toCollection(TreeSet) to both collect and sort by comparator:
        // Here we collect distinct cities sorted lexicographically.
        Set<String> sortedCities = PEOPLE.stream()
                .map(Person::city)
                .collect(Collectors.toCollection(() -> new java.util.TreeSet<>(Comparator.naturalOrder())));
        System.out.println("sortedCities=" + sortedCities);
    }

    static void joiningAndStringCollectors() {
        System.out.println("\n=== joining ===");

        // Simple join (no delimiter):
        String concatNames = PEOPLE.stream().map(Person::name).collect(Collectors.joining());
        // Join with delimiter:
        String csvNames = PEOPLE.stream().map(Person::name).collect(Collectors.joining(", "));
        // Join with delimiter, prefix, suffix:
        String wrapped = PEOPLE.stream().map(Person::name).collect(Collectors.joining(", ", "[", "]"));

        System.out.println("concat=" + concatNames);
        System.out.println("csv=" + csvNames);
        System.out.println("wrapped=" + wrapped);
    }

    static void numericAggregations() {
        System.out.println("\n=== Numeric aggregations: counting, averaging, summarizing ===");

        long count = PEOPLE.stream().collect(Collectors.counting());
        double avgSalary = PEOPLE.stream().collect(Collectors.averagingDouble(Person::salary));
        int sumAges = PEOPLE.stream().collect(Collectors.summingInt(Person::age));

        DoubleSummaryStatistics salaryStats = PEOPLE.stream()
                .collect(Collectors.summarizingDouble(Person::salary));
        IntSummaryStatistics ageStats = PEOPLE.stream()
                .collect(Collectors.summarizingInt(Person::age));

        System.out.println("count=" + count + ", avgSalary=" + avgSalary + ", sumAges=" + sumAges);
        System.out.println("salaryStats=" + salaryStats);
        System.out.println("ageStats=" + ageStats);
    }

    static void maxMinReducing() {
        System.out.println("\n=== maxBy/minBy and reducing ===");

        // Max by comparator:
        Optional<Person> richest = PEOPLE.stream()
                .collect(Collectors.maxBy(Comparator.comparingDouble(Person::salary)));
        Optional<Person> youngest = PEOPLE.stream()
                .collect(Collectors.minBy(Comparator.comparingInt(Person::age)));

        System.out.println("richest=" + richest.map(Person::name).orElse("?")
                + ", youngest=" + youngest.map(Person::name).orElse("?"));

        // reducing: a general reduction. Example: total salary via reducing.
        double totalSalary = PEOPLE.stream()
                .collect(Collectors.reducing(0.0, Person::salary, Double::sum));
        System.out.println("totalSalary via reducing=" + totalSalary);

        // Note: prefer mapToX().sum() or summingDouble for clarity/perf:
        double totalSalaryFast = PEOPLE.stream().mapToDouble(Person::salary).sum();
        System.out.println("totalSalary via primitive sum=" + totalSalaryFast);
    }

    static void collectingAndThenExamples() {
        System.out.println("\n=== collectingAndThen ===");

        // Collect, then transform. Example: make result unmodifiable.
        List<String> topEarners = PEOPLE.stream()
                .filter(p -> p.salary() >= 100_000)
                .map(Person::name)
                .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));

        System.out.println("topEarners=" + topEarners);
        try {
            topEarners.add("X");
        } catch (UnsupportedOperationException e) {
            System.out.println("collectingAndThen made list unmodifiable");
        }
    }

    static void toMapExamples() {
        System.out.println("\n=== toMap ===");

        // toMap with unique keys (names are unique in our sample). Otherwise throws IllegalStateException.
        Map<String, Person> byName = PEOPLE.stream()
                .collect(Collectors.toMap(Person::name, Function.identity()));

        // When keys can repeat, provide a merge function. Example: count people per city via toMap:
        Map<String, Integer> cityCounts = PEOPLE.stream()
                .collect(Collectors.toMap(
                        Person::city,
                        p -> 1,
                        Integer::sum)); // merge duplicates by summing
        System.out.println("cityCounts=" + cityCounts);

        // Choose Map type: LinkedHashMap preserves encounter order of first keys.
        Map<String, Double> maxSalaryPerCity = PEOPLE.stream()
                .collect(Collectors.toMap(
                        Person::city,
                        Person::salary,
                        Double::max,
                        LinkedHashMap::new));
        System.out.println("maxSalaryPerCity=" + maxSalaryPerCity);

        // toUnmodifiableMap (Java 10+)
        Map<String, Integer> nameLengths = PEOPLE.stream()
                .collect(Collectors.toUnmodifiableMap(Person::name, p -> p.name().length()));
        System.out.println("nameLengths=" + nameLengths);
        try {
            nameLengths.put("X", 1);
        } catch (UnsupportedOperationException e) {
            System.out.println("toUnmodifiableMap is immutable");
        }
    }

    static void partitioningExamples() {
        System.out.println("\n=== partitioningBy ===");

        // Partition ages: >= 30 vs < 30
        Map<Boolean, List<Person>> byAge30 = PEOPLE.stream()
                .collect(Collectors.partitioningBy(p -> p.age() >= 30));
        System.out.println(">=30 count=" + byAge30.get(true).size() + ", <30 count=" + byAge30.get(false).size());

        // Partition with downstream: count in each partition
        Map<Boolean, Long> byAgeCounts = PEOPLE.stream()
                .collect(Collectors.partitioningBy(p -> p.age() >= 30, Collectors.counting()));
        System.out.println("partition counts=" + byAgeCounts);
    }

    static void groupingByBasics() {
        System.out.println("\n=== groupingBy basics ===");

        // Simple grouping: city -> List<Person>
        Map<String, List<Person>> byCity = PEOPLE.stream()
                .collect(Collectors.groupingBy(Person::city));
        System.out.println("byCity keys=" + byCity.keySet());

        // Grouping with aggregation per group: city -> avg salary
        Map<String, Double> avgSalaryByCity = PEOPLE.stream()
                .collect(Collectors.groupingBy(Person::city, Collectors.averagingDouble(Person::salary)));
        System.out.println("avgSalaryByCity=" + avgSalaryByCity);

        // Counting per group: gender -> count
        Map<Gender, Long> countByGender = PEOPLE.stream()
                .collect(Collectors.groupingBy(Person::gender, Collectors.counting()));
        System.out.println("countByGender=" + countByGender);
    }

    static void groupingByDownstream() {
        System.out.println("\n=== groupingBy with downstream collectors ===");

        // mapping: city -> Set<String> of names
        Map<String, Set<String>> namesByCity = PEOPLE.stream()
                .collect(Collectors.groupingBy(
                        Person::city,
                        Collectors.mapping(Person::name, Collectors.toSet())
                ));
        System.out.println("namesByCity=" + namesByCity);

        // filtering (Java 9+): city -> count of people age >= 30
        Map<String, Long> count30PlusByCity = PEOPLE.stream()
                .collect(Collectors.groupingBy(
                        Person::city,
                        Collectors.filtering(p -> p.age() >= 30, Collectors.counting())
                ));
        System.out.println("count30PlusByCity=" + count30PlusByCity);

        // flatMapping (Java 9+): city -> Set<String> union of tags in that city
        Map<String, Set<String>> tagsByCity = PEOPLE.stream()
                .collect(Collectors.groupingBy(
                        Person::city,
                        Collectors.flatMapping(p -> p.tags().stream(), Collectors.toSet())
                ));
        System.out.println("tagsByCity=" + tagsByCity);

        // Collect a single best element per group: city -> highest-paid person (Optional)
        Map<String, Optional<Person>> richestPerCityOpt = PEOPLE.stream()
                .collect(Collectors.groupingBy(
                        Person::city,
                        Collectors.maxBy(Comparator.comparingDouble(Person::salary))
                ));
        // Often we want to unwrap Optional when we know there's at least one:
        Map<String, Person> richestPerCity = PEOPLE.stream()
                .collect(Collectors.groupingBy(
                        Person::city,
                        Collectors.collectingAndThen(
                                Collectors.maxBy(Comparator.comparingDouble(Person::salary)),
                                Optional::get
                        )
                ));
        System.out.println("richestPerCity=" + richestPerCity.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().name())));
    }

    static void multiLevelGrouping() {
        System.out.println("\n=== Multi-level grouping ===");

        // city -> gender -> List<Person>
        Map<String, Map<Gender, List<Person>>> byCityThenGender = PEOPLE.stream()
                .collect(Collectors.groupingBy(
                        Person::city,
                        Collectors.groupingBy(Person::gender)
                ));
        System.out.println("byCityThenGender keys=" + byCityThenGender.keySet());

        // city -> gender -> Set<String> names
        Map<String, Map<Gender, Set<String>>> namesByCityGender = PEOPLE.stream()
                .collect(Collectors.groupingBy(
                        Person::city,
                        Collectors.groupingBy(
                                Person::gender,
                                Collectors.mapping(Person::name, Collectors.toSet())
                        )
                ));
        System.out.println("namesByCityGender=" + namesByCityGender);
    }

    static void groupingWithCustomMapAndConcurrent() {
        System.out.println("\n=== groupingBy with custom Map, and groupingByConcurrent ===");

        // Custom Map type (sorted): TreeMap for sorted keys
        Map<String, Long> countsSorted = PEOPLE.stream()
                .collect(Collectors.groupingBy(
                        Person::city,
                        TreeMap::new,
                        Collectors.counting()
                ));
        System.out.println("countsSorted(TreeMap)=" + countsSorted);

        // groupingByConcurrent (ConcurrentMap)
        // Note: classifier must not return null (ConcurrentMap forbids null keys).
        ConcurrentMap<String, Long> countsConcurrent = PEOPLE.parallelStream()
                .collect(Collectors.groupingByConcurrent(Person::city, Collectors.counting()));
        System.out.println("countsConcurrent=" + countsConcurrent.getClass().getSimpleName() + " " + countsConcurrent);
    }

    static void flatMappingFilteringMapping() {
        System.out.println("\n=== mapping/filtering/flatMapping downstream utilities ===");

        // mapping: gender -> average age of names starting with vowel per gender (just to show mapping before averaging)
        Map<Gender, Double> avgAgeNamesStartingWithVowel = PEOPLE.stream()
                .collect(Collectors.groupingBy(
                        Person::gender,
                        Collectors.filtering(
                                p -> isVowel(p.name().charAt(0)),
                                Collectors.averagingInt(Person::age)
                        )
                ));
        System.out.println("avgAgeNamesStartingWithVowel=" + avgAgeNamesStartingWithVowel);

        // flatMapping: city -> distinct first letters of tags
        Map<String, Set<Character>> tagFirstLettersByCity = PEOPLE.stream()
                .collect(Collectors.groupingBy(
                        Person::city,
                        Collectors.flatMapping(
                                p -> p.tags().stream().map(s -> s.charAt(0)),
                                Collectors.toSet()
                        )
                ));
        System.out.println("tagFirstLettersByCity=" + tagFirstLettersByCity);
    }

    static boolean isVowel(char c) {
        return "AEIOUaeiou".indexOf(c) >= 0;
    }

    static void teeingExamples() {
        System.out.println("\n=== teeing (Java 12+) ===");

        // Combine two collectors: summarizing age and averaging salary into a single result.
        record DemoStats(int minAge, int maxAge, double avgSalary) {}
        DemoStats stats = PEOPLE.stream().collect(
                Collectors.teeing(
                        Collectors.summarizingInt(Person::age),
                        Collectors.averagingDouble(Person::salary),
                        (ageSummary, avgSal) -> new DemoStats(ageSummary.getMin(), ageSummary.getMax(), avgSal)
                )
        );
        System.out.println("DemoStats=" + stats);
    }

    static void customCollectors() {
        System.out.println("\n=== Custom collectors ===");

        // 1) Using Collector.of to compute average age (accumulator is double[2] => [sum, count])
        Collector<Person, double[], Double> avgAgeCollector = Collector.of(
                () -> new double[2],                                // supplier
                (acc, p) -> { acc[0] += p.age(); acc[1] += 1; },    // accumulator
                (a, b) -> { a[0] += b[0]; a[1] += b[1]; return a; },// combiner
                acc -> acc[1] == 0 ? 0.0 : acc[0] / acc[1]          // finisher
        );
        double avgAge = PEOPLE.stream().collect(avgAgeCollector);
        System.out.println("avgAge via custom Collector.of=" + avgAge);

        // 2) A collector with IDENTITY_FINISH: toLinkedHashSet (preserve encounter order, no finisher transform)
        Collector<String, LinkedHashSet<String>, LinkedHashSet<String>> toLinkedHashSet = new Collector<>() {
            @Override public Supplier<LinkedHashSet<String>> supplier() { return LinkedHashSet::new; }
            @Override public BiConsumer<LinkedHashSet<String>, String> accumulator() { return Set::add; }
            @Override public BinaryOperator<LinkedHashSet<String>> combiner() {
                return (left, right) -> { left.addAll(right); return left; };
            }
            @Override public Function<LinkedHashSet<String>, LinkedHashSet<String>> finisher() { return Function.identity(); }
            @Override public Set<Characteristics> characteristics() {
                return EnumSet.of(Characteristics.IDENTITY_FINISH);
            }
        };
        LinkedHashSet<String> linkedCitySet = PEOPLE.stream().map(Person::city).collect(toLinkedHashSet);
        System.out.println("linkedCitySet=" + linkedCitySet);

        // 3) Custom "joiningUpperCase" collector for CharSequence:
        Collector<CharSequence, StringBuilder, String> joiningUpper = new Collector<>() {
            private final CharSequence delim = ", ";
            private final CharSequence prefix = "[";
            private final CharSequence suffix = "]";

            @Override public Supplier<StringBuilder> supplier() {
                // StringBuilder used as accumulator
                return () -> new StringBuilder(prefix);
            }
            @Override public BiConsumer<StringBuilder, CharSequence> accumulator() {
                return (sb, csq) -> {
                    if (sb.length() > prefix.length()) sb.append(delim);
                    sb.append(csq.toString().toUpperCase());
                };
            }
            @Override public BinaryOperator<StringBuilder> combiner() {
                return (a, b) -> {
                    if (b.length() > prefix.length()) {
                        if (a.length() > prefix.length()) a.append(delim);
                        a.append(b, prefix.length(), b.length()); // append without duplicate prefix
                    }
                    return a;
                };
            }
            @Override public Function<StringBuilder, String> finisher() {
                return sb -> sb.append(suffix).toString();
            }
            @Override public Set<Characteristics> characteristics() {
                // Not IDENTITY_FINISH because finisher returns String
                return Collections.emptySet();
            }
        };
        String joinedUpperNames = PEOPLE.stream().map(Person::name).collect(joiningUpper);
        System.out.println("joinedUpperNames=" + joinedUpperNames);
    }

    static void notes() {
        System.out.println("\n=== Notes / Pitfalls / Tips ===");
        // - Collectors.groupingBy uses a HashMap by default (allows one null key). groupingByConcurrent uses a ConcurrentMap (no nulls).
        // - Stream.toList() is unmodifiable (Java 16+). Collectors.toList() is mutable. Choose one consistently.
        // - toMap without merge fn throws if duplicate keys; supply merge fn when keys can repeat.
        // - Use downstream collectors (mapping, filtering, flatMapping) to avoid intermediate maps/streams per group.
        // - For parallel streams, prefer collectors with good combiner behavior; consider groupingByConcurrent for scalability.
        // - collectingAndThen is useful to wrap results (e.g., unmodifiable) or to unwrap Optionals.
        // - Characteristics:
        //     UNORDERED: input order does not affect result. Set this only if truly order-insensitive.
        //     CONCURRENT: accumulation function is thread-safe and supports parallel concurrent updates.
        //     IDENTITY_FINISH: accumulator type is the final result type (no finisher transform).
        // - Avoid mutating elements used as Map keys during/after collection; keys must be stable.
        // - Prefer primitive specializations (mapToInt/Long/Double and summarizingX) for numeric aggregations.
        System.out.println("Done.");
    }
}