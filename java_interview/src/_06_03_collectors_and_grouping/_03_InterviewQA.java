package _06_03_collectors_and_grouping;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.EnumMap;
import java.util.function.*;
import java.util.stream.*;

import static java.util.Comparator.*;
import static java.util.stream.Collectors.*;

/**
 * Collectors & Grouping — Interview Q&A (Basic → Advanced)
 *
 * How to use:
 * - Read Q/A blocks in comments.
 * - Run main() to see concise, runnable examples for each Q.
 *
 * Notes:
 * - Uses Collectors.filtering/flatMapping (Java 9+) and teeing (Java 12+).
 * - Uses only classic classes (no records) for broader JDK compatibility.
 */
public class _03_InterviewQA {

    public static void main(String[] args) {
        // Basic
        q01_whatIsCollector_and_collect_vs_reduce();
        q02_collect_to_list_set_collection_and_toList_vs_Stream_toList();
        q03_joining_strings();
        q04_counting_summing_averaging_summarizing();
        q05_groupingBy_vs_partitioningBy();
        q06_downstream_collectors_mapping();
        q07_downstream_collectors_filtering();
        q08_downstream_collectors_flatMapping();
        q09_toMap_duplicates_merge_function_and_map_supplier();

        // Intermediate
        q10_collectingAndThen_postProcessing();
        q11_groupingBy_with_custom_Map_supplier();
        q12_concurrent_collectors_groupingByConcurrent_toConcurrentMap();
        q13_custom_collector_basics_Collector_of();
        q14_teeing_combine_two_collectors();
        q15_grouping_with_reducing_vs_specialized_collectors();
        q16_toCollection_choose_specific_collection_and_order();
        q17_count_distinct_values();
        q18_maxBy_minBy_and_Optional_handling();
        q19_grouping_to_EnumMap();
        q20_summarizing_by_group();

        // Advanced
        q21_topN_per_group_collectingAndThen();
        q22_mutability_identity_finish_and_unmodifiable_results();
        q23_ordering_implications_HashMap_TreeMap_LinkedHashMap();
        q24_partitioning_guarantees_two_keys();
        q25_nulls_and_collectors_common_pitfalls();
        q26_performance_tips_and_parallel_considerations();
        q27_bigdecimal_and_precision_hints();
    }

    // Sample domain
    enum Gender { MALE, FEMALE, OTHER }

    static class Person {
        private final String name;
        private final int age;
        private final String city;
        private final double salary;
        private final Gender gender;
        private final List<String> skills;

        Person(String name, int age, String city, double salary, Gender gender, List<String> skills) {
            this.name = name;
            this.age = age;
            this.city = city;
            this.salary = salary;
            this.gender = gender;
            this.skills = skills;
        }

        public String name() { return name; }
        public int age() { return age; }
        public String city() { return city; }
        public double salary() { return salary; }
        public Gender gender() { return gender; }
        public List<String> skills() { return skills; }

        @Override public String toString() {
            return name + "(" + city + ", " + age + ", $" + salary + ")";
        }
    }

    static final List<Person> PEOPLE = List.of(
            new Person("Alice",   30, "New York",      120_000, Gender.FEMALE, List.of("Java", "Kotlin")),
            new Person("Bob",     35, "San Francisco", 150_000, Gender.MALE,   List.of("Java", "Go")),
            new Person("Charlie", 28, "Los Angeles",    95_000, Gender.MALE,   List.of("JavaScript", "React")),
            new Person("Diana",   40, "New York",      175_000, Gender.FEMALE, List.of("Java", "AWS", "Docker")),
            new Person("Eve",     22, "San Francisco",  80_000, Gender.FEMALE, List.of("Python", "ML")),
            new Person("Frank",   30, "Los Angeles",   110_000, Gender.MALE,   List.of("Java", "Spring")),
            new Person("Grace",   26, "New York",      105_000, Gender.FEMALE, List.of("JavaScript", "TypeScript")),
            new Person("Heidi",   33, "San Francisco", 130_000, Gender.FEMALE, List.of("Java", "Kubernetes")),
            new Person("Ivan",    45, "Los Angeles",   160_000, Gender.MALE,   List.of("Go", "Kubernetes")),
            new Person("Judy",    38, "New York",      140_000, Gender.FEMALE, List.of("Python", "Data"))
    );

    static String ageBucket(Person p) {
        if (p.age() < 30) return "Young";
        if (p.age() < 40) return "Mid";
        return "Senior";
    }

    // Utilities
    static void header(String title) {
        System.out.println("\n=== " + title + " ===");
    }
    static <K,V> void printMap(Map<K,V> map) {
        map.forEach((k,v) -> System.out.println(k + " -> " + v));
    }

    /*
     Q1. What is a Collector? collect() vs reduce()?
     A:
     - Collector: Mutable reduction strategy (Supplier, Accumulator, Combiner, Finisher, Characteristics).
     - collect(): general mutable reduction to a container/result (List/Map/etc.).
     - reduce(): immutable reduction to a single value; not ideal for building collections.
    */
    static void q01_whatIsCollector_and_collect_vs_reduce() {
        header("Q1 Collector vs reduce");
        // collect to list (mutable reduction)
        List<String> names = PEOPLE.stream().map(Person::name).collect(toList());
        System.out.println("Names (collect): " + names);

        // reduce to a single double (sum of salaries)
        double total1 = PEOPLE.stream().map(Person::salary).reduce(0.0, Double::sum);
        double total2 = PEOPLE.stream().collect(summingDouble(Person::salary));
        System.out.println("Total salary reduce: " + total1 + " | collect: " + total2);
    }

    /*
     Q2. Collect to List/Set/Custom Collection. Stream.toList() vs Collectors.toList()?
     A:
     - Collectors.toList(): type/mutability not guaranteed (commonly ArrayList).
     - Stream.toList() (Java 16+): returns an unmodifiable List.
     - To control collection type, use toCollection(Supplier).
    */
    static void q02_collect_to_list_set_collection_and_toList_vs_Stream_toList() {
        header("Q2 toList/toSet/toCollection");
        Set<String> skillSet = PEOPLE.stream().flatMap(p -> p.skills().stream()).collect(toSet());
        System.out.println("Unique skills (toSet): " + skillSet);

        LinkedHashSet<String> orderedNames = PEOPLE.stream()
                .map(Person::name)
                .collect(toCollection(LinkedHashSet::new));
        System.out.println("Names (LinkedHashSet): " + orderedNames);

        // Stream.toList() unmodifiable (Java 16+); shown here as comment:
        // List<String> unmodifiable = PEOPLE.stream().map(Person::name).toList();
    }

    /*
     Q3. joining() collector
     A:
     - joining(delimiter, prefix, suffix) efficiently builds strings.
    */
    static void q03_joining_strings() {
        header("Q3 joining strings");
        String csv = PEOPLE.stream().map(Person::name).collect(joining(", "));
        System.out.println("CSV: " + csv);

        String pretty = PEOPLE.stream().map(Person::name).collect(joining(", ", "[", "]"));
        System.out.println("Pretty: " + pretty);
    }

    /*
     Q4. counting/summing/averaging/summarizing
     A:
     - counting(), summingInt/Long/Double(), averaging..., summarizing... (min, max, sum, count, avg).
     - Beware: summingInt returns Integer and may overflow; summarizingInt sum is long.
    */
    static void q04_counting_summing_averaging_summarizing() {
        header("Q4 Basic numeric collectors");
        long count = PEOPLE.stream().collect(counting());
        double avg = PEOPLE.stream().collect(averagingDouble(Person::salary));
        IntSummaryStatistics ages = PEOPLE.stream().collect(summarizingInt(Person::age));
        DoubleSummaryStatistics salStats = PEOPLE.stream().collect(summarizingDouble(Person::salary));
        System.out.println("count=" + count + ", avgSalary=" + avg);
        System.out.println("ageStats=" + ages);
        System.out.println("salaryStats=" + salStats);
    }

    /*
     Q5. groupingBy vs partitioningBy
     A:
     - groupingBy: keys from classifier function; any key count; missing keys absent.
     - partitioningBy: boolean predicate, always returns 2 groups (true/false).
    */
    static void q05_groupingBy_vs_partitioningBy() {
        header("Q5 groupingBy vs partitioningBy");
        Map<String, List<Person>> byCity = PEOPLE.stream().collect(groupingBy(Person::city));
        printMap(byCity);

        Map<Boolean, List<Person>> highEarners = PEOPLE.stream()
                .collect(partitioningBy(p -> p.salary() >= 130_000));
        printMap(highEarners);
    }

    /*
     Q6. Downstream collectors: mapping
     A:
     - groupingBy(classifier, downstream) to control per-group aggregation.
     - mapping(mapper, downstream) transforms elements before downstream collect.
    */
    static void q06_downstream_collectors_mapping() {
        header("Q6 downstream mapping");
        Map<String, Set<String>> namesByCity = PEOPLE.stream()
                .collect(groupingBy(Person::city, mapping(Person::name, toSet())));
        printMap(namesByCity);
    }

    /*
     Q7. Downstream collectors: filtering (Java 9+)
     A:
     - filtering(predicate, downstream) keeps only elements passing predicate inside group.
    */
    static void q07_downstream_collectors_filtering() {
        header("Q7 downstream filtering");
        Map<String, Long> highEarnersCountByCity = PEOPLE.stream()
                .collect(groupingBy(Person::city,
                        filtering(p -> p.salary() >= 130_000, counting())));
        printMap(highEarnersCountByCity);
    }

    /*
     Q8. Downstream collectors: flatMapping (Java 9+)
     A:
     - flatMapping(mapper to Stream, downstream) flattens nested streams before downstream.
    */
    static void q08_downstream_collectors_flatMapping() {
        header("Q8 downstream flatMapping");
        Map<String, Set<String>> skillsByCity = PEOPLE.stream()
                .collect(groupingBy(Person::city,
                        flatMapping(p -> p.skills().stream(), toSet())));
        printMap(skillsByCity);
    }

    /*
     Q9. toMap: duplicates, merge function, Map supplier
     A:
     - toMap(keyMapper, valueMapper) throws IllegalStateException on duplicate keys.
     - Provide merge function to resolve duplicates; provide map supplier for type/order.
    */
    static void q09_toMap_duplicates_merge_function_and_map_supplier() {
        header("Q9 toMap with merge and supplier");
        // Total salary by city, preserve encounter order using LinkedHashMap
        Map<String, Double> totalSalaryByCity = PEOPLE.stream()
                .collect(toMap(
                        Person::city,
                        Person::salary,
                        Double::sum,
                        LinkedHashMap::new
                ));
        printMap(totalSalaryByCity);

        // Join names by city
        Map<String, String> namesCsvByCity = PEOPLE.stream()
                .collect(toMap(
                        Person::city,
                        Person::name,
                        (a, b) -> a + ", " + b,      // merge duplicate keys
                        TreeMap::new                  // sorted by city
                ));
        printMap(namesCsvByCity);
    }

    /*
     Q10. collectingAndThen: post-process collected result (e.g., make unmodifiable, unwrap Optional)
     A:
     - collectingAndThen(downstream, finisher).
    */
    static void q10_collectingAndThen_postProcessing() {
        header("Q10 collectingAndThen");
        List<String> names = PEOPLE.stream().map(Person::name)
                .collect(collectingAndThen(toList(), Collections::unmodifiableList));
        System.out.println("Unmodifiable names: " + names);

        // Top earner per city (unwrap Optional)
        Map<String, Person> topEarnerPerCity = PEOPLE.stream()
                .collect(groupingBy(Person::city,
                        collectingAndThen(
                                maxBy(comparingDouble(Person::salary)),
                                opt -> opt.orElseThrow(NoSuchElementException::new)
                        )));
        printMap(topEarnerPerCity);
    }

    /*
     Q11. groupingBy with custom Map supplier (e.g., TreeMap/LinkedHashMap/EnumMap)
     A:
     - groupingBy(classifier, mapFactory, downstream)
    */
    static void q11_groupingBy_with_custom_Map_supplier() {
        header("Q11 groupingBy with custom Map supplier");
        Map<String, List<Person>> byCitySorted = PEOPLE.stream()
                .collect(groupingBy(Person::city, TreeMap::new, toList()));
        printMap(byCitySorted);
        System.out.println("Map type: " + byCitySorted.getClass().getSimpleName());
    }

    /*
     Q12. Concurrent collectors: groupingByConcurrent, toConcurrentMap
     A:
     - For parallel streams, CONCURRENT collectors may reduce contention.
     - groupingByConcurrent requires concurrent map; order may differ (especially for unordered sources).
    */
    static void q12_concurrent_collectors_groupingByConcurrent_toConcurrentMap() {
        header("Q12 concurrent collectors");
        ConcurrentMap<String, Long> counts = PEOPLE.parallelStream()
                .collect(groupingByConcurrent(Person::city, counting()));
        printMap(counts);
        System.out.println("Map type: " + counts.getClass().getSimpleName());

        ConcurrentMap<String, Double> totals = PEOPLE.parallelStream()
                .collect(toConcurrentMap(Person::city, Person::salary, Double::sum, ConcurrentHashMap::new));
        printMap(totals);
    }

    /*
     Q13. Custom Collector via Collector.of
     A:
     - Define Supplier, Accumulator, Combiner, Finisher (+ Characteristics).
     - Example: average salary collector.
    */
    static void q13_custom_collector_basics_Collector_of() {
        header("Q13 custom Collector.of (average salary)");
        Collector<Person, SalaryAccumulator, Double> avgSalaryCollector =
                Collector.of(
                        SalaryAccumulator::new,
                        (acc, p) -> { acc.sum += p.salary(); acc.count++; },
                        (a, b) -> { a.sum += b.sum; a.count += b.count; return a; },
                        acc -> acc.count == 0 ? 0.0 : acc.sum / acc.count,
                        Collector.Characteristics.UNORDERED
                );
        double avg = PEOPLE.stream().collect(avgSalaryCollector);
        System.out.println("Average salary (custom collector): " + avg);
    }
    static class SalaryAccumulator { double sum; long count; }

    /*
     Q14. teeing (Java 12+): combine two collectors in one pass
     A:
     - teeing(c1, c2, merger) to merge results of two collectors.
    */
    static void q14_teeing_combine_two_collectors() {
        header("Q14 teeing");
        // Average via teeing (sum + count)
        double avg = PEOPLE.stream()
                .collect(teeing(summingDouble(Person::salary), counting(),
                        (sum, count) -> count == 0 ? 0.0 : sum / count));
        System.out.println("Average salary (teeing): " + avg);

        // Min & Max salary in one pass
        var minMax = PEOPLE.stream().collect(teeing(
                minBy(comparingDouble(Person::salary)),
                maxBy(comparingDouble(Person::salary)),
                (min, max) -> Map.of(
                        "min", min.map(Person::salary).orElse(0.0),
                        "max", max.map(Person::salary).orElse(0.0)
                )));
        System.out.println("Min/Max (teeing): " + minMax);
    }

    /*
     Q15. grouping with reducing vs specialized collectors
     A:
     - Prefer specialized collectors (summingDouble) for readability and potential efficiency.
    */
    static void q15_grouping_with_reducing_vs_specialized_collectors() {
        header("Q15 grouping with reducing vs summingDouble");
        Map<String, Double> g1 = PEOPLE.stream().collect(groupingBy(Person::city, summingDouble(Person::salary)));
        Map<String, Double> g2 = PEOPLE.stream().collect(groupingBy(Person::city,
                reducing(0.0, Person::salary, Double::sum)));
        System.out.println("summingDouble: " + g1);
        System.out.println("reducing:      " + g2);
    }

    /*
     Q16. toCollection: choose specific collection type & ordering
     A:
     - toCollection(TreeSet::new) for sorted sets; toCollection(LinkedHashSet::new) for insertion order.
    */
    static void q16_toCollection_choose_specific_collection_and_order() {
        header("Q16 toCollection");
        TreeSet<String> sortedCities = PEOPLE.stream().map(Person::city).collect(toCollection(TreeSet::new));
        System.out.println("Sorted cities (TreeSet): " + sortedCities);

        LinkedHashSet<String> namesOrder = PEOPLE.stream().map(Person::name).collect(toCollection(LinkedHashSet::new));
        System.out.println("Names (in encounter order): " + namesOrder);
    }

    /*
     Q17. Count distinct values (e.g., skills)
     A:
     - Collect to Set then size; or downstream flatMapping to Set then size.
    */
    static void q17_count_distinct_values() {
        header("Q17 count distinct values");
        Set<String> allSkills = PEOPLE.stream().flatMap(p -> p.skills().stream()).collect(toSet());
        System.out.println("Distinct skills count: " + allSkills.size());
    }

    /*
     Q18. maxBy/minBy with grouping; Optional handling
     A:
     - Use collectingAndThen(Optional::orElseThrow or orElse(null)) to unwrap if non-empty.
    */
    static void q18_maxBy_minBy_and_Optional_handling() {
        header("Q18 maxBy/minBy in groups");
        Map<String, Person> maxPerCity = PEOPLE.stream().collect(groupingBy(Person::city,
                collectingAndThen(maxBy(comparingDouble(Person::salary)), opt -> opt.orElse(null))));
        printMap(maxPerCity);
    }

    /*
     Q19. Grouping to EnumMap
     A:
     - Use new EnumMap<>(EnumClass) as map supplier for optimal performance/memory when keys are enums.
    */
    static void q19_grouping_to_EnumMap() {
        header("Q19 grouping to EnumMap");
        Map<Gender, List<Person>> byGender = PEOPLE.stream()
                .collect(groupingBy(Person::gender, () -> new EnumMap<>(Gender.class), toList()));
        printMap(byGender);
        System.out.println("Map type: " + byGender.getClass().getSimpleName());
    }

    /*
     Q20. Summarizing by group
     A:
     - summarizingDouble downstream gives min, max, sum, count, avg per group.
    */
    static void q20_summarizing_by_group() {
        header("Q20 summarizing by group");
        Map<String, DoubleSummaryStatistics> statsByCity = PEOPLE.stream()
                .collect(groupingBy(Person::city, summarizingDouble(Person::salary)));
        printMap(statsByCity);
    }

    /*
     Q21. Top-N per group with collectingAndThen
     A:
     - Collect group to list, then sort/limit in finisher.
    */
    static void q21_topN_per_group_collectingAndThen() {
        header("Q21 top-2 earners per city");
        Map<String, List<Person>> top2PerCity = PEOPLE.stream().collect(
                groupingBy(Person::city,
                        collectingAndThen(toList(), list -> list.stream()
                                .sorted(comparingDouble(Person::salary).reversed())
                                .limit(2)
                                .collect(toList()))));
        printMap(top2PerCity);
    }

    /*
     Q22. Mutability, IDENTITY_FINISH, and unmodifiable results
     A:
     - Many built-in collectors are IDENTITY_FINISH and return mutable results (e.g., toList()).
     - Use collectingAndThen(..., Collections::unmodifiableX) or toUnmodifiableList (Java 10+) for immutability.
    */
    static void q22_mutability_identity_finish_and_unmodifiable_results() {
        header("Q22 mutability and identity finish");
        List<String> mutable = PEOPLE.stream().map(Person::name).collect(toList());
        System.out.println("Mutable list: " + mutable.getClass().getSimpleName());

        List<String> immut = PEOPLE.stream().map(Person::name)
                .collect(collectingAndThen(toList(), Collections::unmodifiableList));
        System.out.println("Unmodifiable view created via collectingAndThen");
        // Also available since Java 10:
        // List<String> immut2 = PEOPLE.stream().map(Person::name).collect(toUnmodifiableList());
    }

    /*
     Q23. Ordering implications: HashMap vs TreeMap vs LinkedHashMap
     A:
     - groupingBy default uses HashMap (no order).
     - Use LinkedHashMap to preserve encounter order; TreeMap for sorted keys.
    */
    static void q23_ordering_implications_HashMap_TreeMap_LinkedHashMap() {
        header("Q23 map ordering choices");
        Map<String, List<Person>> linked = PEOPLE.stream()
                .collect(groupingBy(Person::city, LinkedHashMap::new, toList()));
        Map<String, List<Person>> sorted = PEOPLE.stream()
                .collect(groupingBy(Person::city, TreeMap::new, toList()));
        System.out.println("LinkedHashMap order: " + linked.keySet());
        System.out.println("TreeMap order:       " + sorted.keySet());
    }

    /*
     Q24. partitioningBy guarantees two keys?
     A:
     - Yes. It always returns a Map with true and false keys, even if lists are empty.
    */
    static void q24_partitioning_guarantees_two_keys() {
        header("Q24 partitioning guarantees");
        Map<Boolean, List<Person>> partitions = PEOPLE.stream()
                .collect(partitioningBy(p -> p.age() > 100)); // always both keys
        System.out.println("Keys: " + partitions.keySet() + " -> " + partitions);
    }

    /*
     Q25. Nulls and collectors pitfalls
     A:
     - toMap: null keys/values not allowed; throws NPE.
     - groupingBy: classifier may return null -> NPE. Filter or map nulls beforehand.
     - toSet/toList accept nulls, but downstream mapping may NPE if mapper invokes method on null.
    */
    static void q25_nulls_and_collectors_common_pitfalls() {
        header("Q25 null pitfalls (see comments)");
        // Example hints only; not executing null cases.
        System.out.println("Avoid null keys/values in toMap/groupingBy classifiers.");
    }

    /*
     Q26. Performance tips and parallel considerations
     A:
     - Prefer specialized collectors (summingInt, summarizingDouble).
     - Consider groupingByConcurrent for parallel + UNORDERED sources.
     - Avoid unnecessary boxing; use mapToInt/Long/Double terminal ops when fitting use-case.
     - Don’t rely on collector side-effects; ensure thread-safety if you must (discouraged).
    */
    static void q26_performance_tips_and_parallel_considerations() {
        header("Q26 perf & parallel (see comments)");
        long sumAges = PEOPLE.stream().mapToInt(Person::age).sum();
        System.out.println("Sum ages (primitive stream): " + sumAges);
    }

    /*
     Q27. BigDecimal and precision hints
     A:
     - Prefer BigDecimal for money; use reducing with BigDecimal::add or custom collector.
     - Avoid double summing for currency when precision matters.
    */
    static void q27_bigdecimal_and_precision_hints() {
        header("Q27 BigDecimal example");
        BigDecimal total = PEOPLE.stream()
                .map(p -> BigDecimal.valueOf(p.salary()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        System.out.println("Total salary (BigDecimal): " + total);
    }
}