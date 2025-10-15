package _06_03_collectors_and_grouping;

import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Collectors & Grouping – comprehensive, commented examples.
 *
 * Notes:
 * - filtering/flatMapping require Java 9+.
 * - toUnmodifiableX require Java 10+.
 * - teeing requires Java 12+.
 */
public class _02_Examples {

    public static void main(String[] args) {
        new _02_Examples().runAll();
    }

    private void runAll() {
        ex0_dataPreview();
        ex1_basicCollectionFactories();
        ex2_toMapVariants();
        ex3_joining();
        ex4_numericAggregations();
        ex5_collectingAndThen();
        ex6_groupingBy_singleLevel();
        ex7_groupingBy_multiLevel_andDownstream();
        ex8_groupingBy_filtering_and_flatMapping();
        ex9_partitioningBy();
        ex10_reducing();
        ex11_teeing(); // Java 12+
        ex12_unmodifiableCollectors(); // Java 10+
        ex13_groupingBy_suppliers_and_concurrent();
    }

    // Sample domain model and data -----------------------------

    static final class Person {
        private final String name;
        private final int age;
        private final String city;
        private final String department;
        private final double salary;
        private final Set<String> skills;

        Person(String name, int age, String city, String department, double salary, Set<String> skills) {
            this.name = name;
            this.age = age;
            this.city = city;
            this.department = department;
            this.salary = salary;
            this.skills = skills;
        }

        public String getName() { return name; }
        public int getAge() { return age; }
        public String getCity() { return city; }
        public String getDepartment() { return department; }
        public double getSalary() { return salary; }
        public Set<String> getSkills() { return skills; }

        @Override
        public String toString() {
            return name + "{" + age + ", " + city + ", " + department + ", $" + (long)salary + "}";
        }
    }

    private static final List<Person> PEOPLE = Arrays.asList(
        new Person("Alice", 23, "New York", "Engineering", 93_000, Set.of("Java", "SQL")),
        new Person("Bob", 34, "New York", "Sales", 71_000, Set.of("Negotiation", "Excel")),
        new Person("Carol", 29, "San Francisco", "Engineering", 120_000, Set.of("Java", "Kubernetes")),
        new Person("Dave", 41, "San Francisco", "HR", 65_000, Set.of("Recruiting")),
        new Person("Eve", 37, "New York", "Engineering", 140_000, Set.of("Go", "Cloud")),
        new Person("Frank", 25, "London", "Marketing", 55_000, Set.of("SEO", "Content")),
        new Person("Grace", 31, "London", "Engineering", 105_000, Set.of("Java", "AWS")),
        new Person("Heidi", 28, "Berlin", "Sales", 68_000, Set.of("German", "Negotiation")),
        new Person("Ivan", 45, "Berlin", "Engineering", 135_000, Set.of("C++", "Linux")),
        new Person("Judy", 22, "San Francisco", "Marketing", 52_000, Set.of("Social", "Content")),
        new Person("Karl", 33, "London", "Engineering", 112_000, Set.of("Python", "Data"))
    );

    private static String ageBand(int age) {
        if (age < 25) return "<25";
        if (age < 30) return "25-29";
        if (age < 35) return "30-34";
        if (age < 40) return "35-39";
        return "40+";
    }

    // Examples -------------------------------------------------

    private void ex0_dataPreview() {
        System.out.println("\n--- Data Preview ---");
        PEOPLE.forEach(System.out::println);
    }

    private void ex1_basicCollectionFactories() {
        System.out.println("\n--- ex1: toList, toSet, toCollection ---");

        // Collect to a List
        List<String> names = PEOPLE.stream()
                .map(Person::getName)
                .collect(Collectors.toList());
        System.out.println("Names (List): " + names);

        // Collect to a Set (unique cities)
        Set<String> cities = PEOPLE.stream()
                .map(Person::getCity)
                .collect(Collectors.toSet());
        System.out.println("Cities (Set): " + cities);

        // Collect to a specific collection type
        LinkedList<Integer> ages = PEOPLE.stream()
                .map(Person::getAge)
                .collect(Collectors.toCollection(LinkedList::new));
        System.out.println("Ages (LinkedList): " + ages);
    }

    private void ex2_toMapVariants() {
        System.out.println("\n--- ex2: toMap variants (key/value, merge, map supplier) ---");

        // Map name -> city (keys must be unique here)
        Map<String, String> nameToCity = PEOPLE.stream()
                .collect(Collectors.toMap(Person::getName, Person::getCity));
        System.out.println("name -> city: " + nameToCity);

        // Count people per city using toMap with merge function
        Map<String, Long> cityCounts = PEOPLE.stream()
                .collect(Collectors.toMap(
                        Person::getCity,
                        p -> 1L,
                        Long::sum // merge duplicate keys by summing counts
                ));
        System.out.println("city -> count (toMap+merge): " + cityCounts);

        // Highest paid person per department using toMap+merge and a map supplier
        Map<String, Person> topByDept = PEOPLE.stream()
                .collect(Collectors.toMap(
                        Person::getDepartment,
                        Function.identity(),
                        (p1, p2) -> p1.getSalary() >= p2.getSalary() ? p1 : p2,
                        LinkedHashMap::new // preserves insertion order
                ));
        System.out.println("department -> highest paid: " + topByDept);
    }

    private void ex3_joining() {
        System.out.println("\n--- ex3: joining ---");

        // Join names with delimiter/prefix/suffix
        String names = PEOPLE.stream()
                .map(Person::getName)
                .collect(Collectors.joining(", ", "[", "]"));
        System.out.println("Joined names: " + names);

        // Distinct sorted skills across all people
        String skillsCsv = PEOPLE.stream()
                .flatMap(p -> p.getSkills().stream())
                .distinct()
                .sorted()
                .collect(Collectors.joining(" | ", "Skills: [", "]"));
        System.out.println(skillsCsv);
    }

    private void ex4_numericAggregations() {
        System.out.println("\n--- ex4: counting, summing, averaging, summarizing ---");

        long count = PEOPLE.stream().collect(Collectors.counting());
        double totalSalary = PEOPLE.stream().collect(Collectors.summingDouble(Person::getSalary));
        double avgSalary = PEOPLE.stream().collect(Collectors.averagingDouble(Person::getSalary));
        DoubleSummaryStatistics stats = PEOPLE.stream().collect(Collectors.summarizingDouble(Person::getSalary));

        System.out.println("count: " + count);
        System.out.println("sum salary: " + (long) totalSalary);
        System.out.println("avg salary: " + Math.round(avgSalary));
        System.out.println("salary stats: " + stats);
    }

    private void ex5_collectingAndThen() {
        System.out.println("\n--- ex5: collectingAndThen (post-processing) ---");

        // Make an unmodifiable sorted list of unique names
        List<String> sortedUniqueNames = PEOPLE.stream()
                .map(Person::getName)
                .distinct()
                .sorted()
                .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
        System.out.println("sorted unique names (unmodifiable): " + sortedUniqueNames);

        // Highest paid person, unwrapped from Optional
        Person highestPaid = PEOPLE.stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.maxBy(Comparator.comparingDouble(Person::getSalary)),
                        opt -> opt.orElse(null)
                ));
        System.out.println("highest paid: " + highestPaid);
    }

    private void ex6_groupingBy_singleLevel() {
        System.out.println("\n--- ex6: groupingBy (single level) ---");

        // Group people by city -> List<Person>
        Map<String, List<Person>> byCity = PEOPLE.stream()
                .collect(Collectors.groupingBy(Person::getCity));
        System.out.println("by city: " + byCity);

        // Count per city
        Map<String, Long> countByCity = PEOPLE.stream()
                .collect(Collectors.groupingBy(Person::getCity, Collectors.counting()));
        System.out.println("count by city: " + countByCity);

        // Sum salary by department
        Map<String, Double> totalSalaryByDept = PEOPLE.stream()
                .collect(Collectors.groupingBy(Person::getDepartment, Collectors.summingDouble(Person::getSalary)));
        System.out.println("total salary by department: " + totalSalaryByDept);

        // Names by city (as a sorted set)
        Map<String, Set<String>> namesByCity = PEOPLE.stream()
                .collect(Collectors.groupingBy(
                        Person::getCity,
                        Collectors.mapping(Person::getName, Collectors.toCollection(TreeSet::new))
                ));
        System.out.println("names by city (sorted set): " + namesByCity);
    }

    private void ex7_groupingBy_multiLevel_andDownstream() {
        System.out.println("\n--- ex7: groupingBy (multi-level, downstream collectors) ---");

        // Multi-level: city -> age band -> List<Person>
        Map<String, Map<String, List<Person>>> byCityThenAgeBand = PEOPLE.stream()
                .collect(Collectors.groupingBy(
                        Person::getCity,
                        LinkedHashMap::new,
                        Collectors.groupingBy(p -> ageBand(p.getAge()))
                ));
        System.out.println("by city -> age band: " + byCityThenAgeBand);

        // Downstream: summarizing salary by department
        Map<String, DoubleSummaryStatistics> salaryStatsByDept = PEOPLE.stream()
                .collect(Collectors.groupingBy(Person::getDepartment, Collectors.summarizingDouble(Person::getSalary)));
        System.out.println("salary stats by department: " + salaryStatsByDept);

        // Downstream: average age by city
        Map<String, Double> avgAgeByCity = PEOPLE.stream()
                .collect(Collectors.groupingBy(Person::getCity, Collectors.averagingInt(Person::getAge)));
        System.out.println("avg age by city: " + avgAgeByCity);
    }

    private void ex8_groupingBy_filtering_and_flatMapping() {
        System.out.println("\n--- ex8: groupingBy with filtering/flatMapping (Java 9+) ---");

        // filtering: keep only <30 within each city, then map to names
        Map<String, List<String>> youngNamesByCity = PEOPLE.stream()
                .collect(Collectors.groupingBy(
                        Person::getCity,
                        Collectors.filtering(
                                p -> p.getAge() < 30,
                                Collectors.mapping(Person::getName, Collectors.toList())
                        )
                ));
        System.out.println("young (<30) names by city: " + youngNamesByCity);

        // flatMapping: flatten skills per department into a Set<String>
        Map<String, Set<String>> skillsByDept = PEOPLE.stream()
                .collect(Collectors.groupingBy(
                        Person::getDepartment,
                        Collectors.flatMapping(p -> p.getSkills().stream(), Collectors.toSet())
                ));
        System.out.println("skills by department: " + skillsByDept);
    }

    private void ex9_partitioningBy() {
        System.out.println("\n--- ex9: partitioningBy (boolean predicate) ---");

        // Partition people into adults (>=30) and non-adults
        Map<Boolean, List<Person>> adultsPartition = PEOPLE.stream()
                .collect(Collectors.partitioningBy(p -> p.getAge() >= 30));
        System.out.println("adults partition: " + adultsPartition);

        // Partition with downstream: counts per partition
        Map<Boolean, Long> adultCounts = PEOPLE.stream()
                .collect(Collectors.partitioningBy(p -> p.getAge() >= 30, Collectors.counting()));
        System.out.println("adult counts: " + adultCounts);
    }

    private void ex10_reducing() {
        System.out.println("\n--- ex10: reducing ---");

        // Sum salary via reducing(identity, mapper, combiner)
        double totalSalary = PEOPLE.stream()
                .collect(Collectors.reducing(0.0, Person::getSalary, Double::sum));
        System.out.println("total salary (reducing): " + (long) totalSalary);

        // Highest paid person via reducing with BinaryOperator
        Optional<Person> highest = PEOPLE.stream()
                .collect(Collectors.reducing(BinaryOperator.maxBy(Comparator.comparingDouble(Person::getSalary))));
        System.out.println("highest paid (reducing): " + highest.orElse(null));
    }

    private void ex11_teeing() {
        System.out.println("\n--- ex11: teeing (Java 12+) ---");

        // Compute average with teeing: sum + count -> average
        double avgSalaryViaTeeing = PEOPLE.stream().collect(
                Collectors.teeing(
                        Collectors.summingDouble(Person::getSalary),
                        Collectors.counting(),
                        (sum, cnt) -> cnt == 0 ? 0.0 : sum / cnt
                )
        );
        System.out.println("avg salary (teeing): " + Math.round(avgSalaryViaTeeing));

        // Compute min & max paid persons in one pass
        MinMax<Person> minMax = PEOPLE.stream().collect(
                Collectors.teeing(
                        Collectors.minBy(Comparator.comparingDouble(Person::getSalary)),
                        Collectors.maxBy(Comparator.comparingDouble(Person::getSalary)),
                        (minOpt, maxOpt) -> new MinMax<>(minOpt.orElse(null), maxOpt.orElse(null))
                )
        );
        System.out.println("min/max paid (teeing): " + minMax);
    }

    private void ex12_unmodifiableCollectors() {
        System.out.println("\n--- ex12: toUnmodifiableList/Set/Map (Java 10+) ---");

        List<String> namesUnmod = PEOPLE.stream()
                .map(Person::getName)
                .collect(Collectors.toUnmodifiableList());
        System.out.println("names (unmodifiable list): " + namesUnmod);

        Set<String> citiesUnmod = PEOPLE.stream()
                .map(Person::getCity)
                .collect(Collectors.toUnmodifiableSet());
        System.out.println("cities (unmodifiable set): " + citiesUnmod);

        Map<String, String> nameToDeptUnmod = PEOPLE.stream()
                .collect(Collectors.toUnmodifiableMap(Person::getName, Person::getDepartment));
        System.out.println("name -> department (unmodifiable map): " + nameToDeptUnmod);
    }

    private void ex13_groupingBy_suppliers_and_concurrent() {
        System.out.println("\n--- ex13: groupingBy with Map supplier, groupingByConcurrent ---");

        // groupingBy with specific Map supplier (LinkedHashMap preserves insertion order)
        Map<String, Long> countByCityLinked = PEOPLE.stream()
                .collect(Collectors.groupingBy(Person::getCity, LinkedHashMap::new, Collectors.counting()));
        System.out.println("count by city (LinkedHashMap): " + countByCityLinked);

        // Sorted grouping using TreeMap supplier
        Map<String, Long> countByCitySorted = PEOPLE.stream()
                .collect(Collectors.groupingBy(Person::getCity, TreeMap::new, Collectors.counting()));
        System.out.println("count by city (TreeMap): " + countByCitySorted);

        // groupingByConcurrent (demonstrated with parallel stream)
        ConcurrentMap<String, Long> countByDeptConcurrent = PEOPLE.parallelStream()
                .collect(Collectors.groupingByConcurrent(Person::getDepartment, Collectors.counting()));
        System.out.println("count by department (ConcurrentMap): " + countByDeptConcurrent);
    }

    // Helpers --------------------------------------------------

    static final class MinMax<T> {
        final T min;
        final T max;
        MinMax(T min, T max) { this.min = min; this.max = max; }
        @Override public String toString() { return "{min=" + min + ", max=" + max + "}"; }
    }
}