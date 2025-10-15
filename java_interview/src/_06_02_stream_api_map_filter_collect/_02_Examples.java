package _06_02_stream_api_map_filter_collect;

import java.util.*;
import java.util.stream.*;

/**
 * Stream API: map, filter, collect
 * This file demonstrates common patterns and best practices when using map, filter, and collect.
 */
public class _02_Examples {

    public static void main(String[] args) {
        basicMapFilterCollect();
        stringsMapFilterCollect();
        personsMapFilterCollect();
        mapsGroupingPartitioningCollect();
        advancedCollectExamples();
        primitiveStreamsExamples();
        threeArgCollectAndCustomContainers();
        flatMapVsMap();
        peekDebugging();
    }

    private static void advancedCollectExamples() {

    }

    // Sample data
    private static final List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 2, 4, 6);
    private static final List<String> words = Arrays.asList("stream", "api", "map", "filter", "collect", "", " ", "JAVA", "java", "stream");
    private static final List<Person> people = Arrays.asList(
            new Person("Alice", 23, "Berlin", Arrays.asList("Java", "SQL")),
            new Person("Bob", 17, "Berlin", Arrays.asList("JavaScript", "HTML")),
            new Person("Carol", 45, "Paris", Arrays.asList("Java", "Kotlin")),
            new Person("Dave", 30, "London", Arrays.asList("Go", "Docker")),
            new Person("Eve", 30, "Paris", Arrays.asList("Python", "Django")),
            new Person("Frank", 23, "London", Arrays.asList("Java", "Spring")),
            new Person("Grace", 35, "Berlin", Arrays.asList("Go", "Kubernetes")),
            new Person("Heidi", 17, "Paris", Arrays.asList("C++", "OpenGL")),
            new Person("Ivan", 50, "London", Arrays.asList("Java", "Scala")),
            new Person("Judy", 29, "Berlin", Arrays.asList("TypeScript", "React")),
            new Person("Alice", 23, "Berlin", Arrays.asList("Java", "SQL")) // duplicate person to show merges
    );

    // 1) Basics with numbers
    private static void basicMapFilterCollect() {
        System.out.println("=== Basic map/filter/collect with numbers ===");

        // Filter even numbers, map to squares, collect to List
        List<Integer> evenSquares = numbers.stream()
                .filter(n -> n % 2 == 0)      // keep even numbers
                .map(n -> n * n)             // square each
                .collect(Collectors.toList()); // collect into a List
        System.out.println("Even squares (List): " + evenSquares);

        // Distinct even numbers collected into a Set (removes duplicates)
        Set<Integer> distinctEvens = numbers.stream()
                .filter(n -> n % 2 == 0)
                .collect(Collectors.toSet());
        System.out.println("Distinct evens (Set): " + distinctEvens);

        // Preserve collection type explicitly with toCollection
        LinkedList<Integer> evensLinkedList = numbers.stream()
                .filter(n -> n % 2 == 0)
                .collect(Collectors.toCollection(LinkedList::new));
        System.out.println("Evens (LinkedList): " + evensLinkedList);

        // toMap: key -> number, value -> square; handle duplicate keys by keeping the first value
        Map<Integer, Integer> numToSquare = numbers.stream()
                .collect(Collectors.toMap(
                        n -> n,
                        n -> n * n,
                        (oldV, newV) -> oldV,             // merge function: keep old on duplicate
                        LinkedHashMap::new                 // preserve encounter order
                ));
        System.out.println("Number -> Square (LinkedHashMap): " + numToSquare);

        // collectingAndThen: collect to List then wrap into unmodifiable view
        List<Integer> unmodifiable = numbers.stream()
                .filter(n -> n > 5)
                .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
        System.out.println("Unmodifiable >5 (List): " + unmodifiable);
        System.out.println();
    }

    // 2) Strings: cleaning, transforming, joining
    private static void stringsMapFilterCollect() {
        System.out.println("=== Strings map/filter/collect ===");

        // Clean and normalize: trim, remove blanks, lower-case, distinct, sort
        List<String> clean = words.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        System.out.println("Clean distinct sorted (List): " + clean);

        // Collect to LinkedHashSet to keep first-seen order, no duplicates
        Set<String> uniqueOrdered = words.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        System.out.println("Unique ordered (LinkedHashSet): " + uniqueOrdered);

        // Join into a comma-separated String
        String joined = words.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .distinct()
                .sorted()
                .collect(Collectors.joining(", "));
        System.out.println("Joined string: " + joined);

        // toMap: word -> length, handling duplicates via merge, preserving order
        Map<String, Integer> wordLength = words.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toMap(
                        s -> s,
                        String::length,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
        System.out.println("word -> length (LinkedHashMap): " + wordLength);
        System.out.println();
    }

    // 3) Objects: mapping fields, filtering by properties, collecting to collections and maps
    private static void personsMapFilterCollect() {
        System.out.println("=== Persons map/filter/collect ===");

        // Names of people older than 25
        List<String> namesOver25 = people.stream()
                .filter(p -> p.getAge() > 25)
                .map(Person::getName)
                .collect(Collectors.toList());
        System.out.println("Names age>25 (List): " + namesOver25);

        // Unique cities (distinct after map)
        List<String> uniqueCities = people.stream()
                .map(Person::getCity)
                .distinct()
                .collect(Collectors.toList());
        System.out.println("Unique cities (List): " + uniqueCities);

        // Map name -> age (duplicate names resolved by keeping the first)
        Map<String, Integer> nameToAge = people.stream()
                .collect(Collectors.toMap(
                        Person::getName,
                        Person::getAge,
                        (oldV, newV) -> oldV,      // keep first on duplicate name
                        LinkedHashMap::new
                ));
        System.out.println("name -> age (LinkedHashMap): " + nameToAge);

        // Group people by city
        Map<String, List<Person>> byCity = people.stream()
                .collect(Collectors.groupingBy(Person::getCity));
        System.out.println("Grouped by city (Map<String, List<Person>>): " + byCity);

        // Top 3 oldest names (unmodifiable)
        List<String> top3Oldest = people.stream()
                .sorted(Comparator.comparingInt(Person::getAge).reversed())
                .limit(3)
                .map(Person::getName)
                .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
        System.out.println("Top 3 oldest names (unmodifiable List): " + top3Oldest);

        // All unique skills sorted
        List<String> uniqueSkills = people.stream()
                .flatMap(p -> p.getSkills().stream())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        System.out.println("Unique skills sorted (List): " + uniqueSkills);

        // Summary statistics for age
        IntSummaryStatistics ageStats = people.stream()
                .collect(Collectors.summarizingInt(Person::getAge));
        System.out.println("Age stats: " + ageStats);
        System.out.println();
    }

    // 4) Collectors: grouping, mapping, partitioning, aggregations
    private static void mapsGroupingPartitioningCollect() {
        System.out.println("=== Collectors grouping/partitioning/aggregations ===");

        // Count people per city
        Map<String, Long> countByCity = people.stream()
                .collect(Collectors.groupingBy(Person::getCity, Collectors.counting()));
        System.out.println("Count by city: " + countByCity);

        // Average age per city
        Map<String, Double> avgAgeByCity = people.stream()
                .collect(Collectors.groupingBy(Person::getCity, Collectors.averagingInt(Person::getAge)));
        System.out.println("Average age by city: " + avgAgeByCity);

        // Names per city (Set)
        Map<String, Set<String>> namesByCity = people.stream()
                .collect(Collectors.groupingBy(
                        Person::getCity,
                        Collectors.mapping(Person::getName, Collectors.toSet())
                ));
        System.out.println("Names by city (Set): " + namesByCity);

        // Partition into adults and minors
        Map<Boolean, List<Person>> adultsPartition = people.stream()
                .collect(Collectors.partitioningBy(p -> p.getAge() >= 18));
        System.out.println("Partition adults (>=18): " + adultsPartition);

        // Partition with downstream collector: count adults vs minors
        Map<Boolean, Long> adultCounts = people.stream()
                .collect(Collectors.partitioningBy(p -> p.getAge() >= 18, Collectors.counting()));
        System.out.println("Counts adults/minors: " + adultCounts);

        // Join names per city into a single comma-separated string
        Map<String, String> joinedNamesByCity = people.stream()
                .collect(Collectors.groupingBy(
                        Person::getCity,
                        Collectors.mapping(Person::getName, Collectors.collectingAndThen(
                                Collectors.toCollection(LinkedHashSet::new), // keep encounter order unique
                                set -> String.join(", ", set)
                        ))
                ));
        System.out.println("Joined names by city: " + joinedNamesByCity);

        // Sum ages per city
        Map<String, Integer> sumAgesByCity = people.stream()
                .collect(Collectors.groupingBy(Person::getCity, Collectors.summingInt(Person::getAge)));
        System.out.println("Sum ages by city: " + sumAgesByCity);

        // Max age per city -> name of oldest person in that city
        Map<String, String> oldestNameByCity = people.stream()
                .collect(Collectors.groupingBy(
                        Person::getCity,
                        Collectors.collectingAndThen(
                                Collectors.maxBy(Comparator.comparingInt(Person::getAge)),
                                opt -> opt.map(Person::getName).orElse("N/A")
                        )
                ));
        System.out.println("Oldest name by city: " + oldestNameByCity);
        System.out.println();
    }

    // 5) Primitive streams: mapToInt, sum, average, boxing
    private static void primitiveStreamsExamples() {
        System.out.println("=== Primitive streams (mapToInt, sum, average) ===");

        int sum = numbers.stream().mapToInt(Integer::intValue).sum();
        System.out.println("Sum of numbers: " + sum);

        OptionalDouble avg = numbers.stream().mapToInt(Integer::intValue).average();
        System.out.println("Average of numbers: " + (avg.isPresent() ? avg.getAsDouble() : "N/A"));

        // Index elements with IntStream.range and map to formatted strings
        List<String> indexedWords = IntStream.range(0, words.size())
                .mapToObj(i -> i + ":" + words.get(i))
                .collect(Collectors.toList());
        System.out.println("Indexed words: " + indexedWords);
        System.out.println();
    }

    // 6) collect with Supplier/Accumulator/Combiner and custom containers
    private static void threeArgCollectAndCustomContainers() {
        System.out.println("=== collect(Supplier, Accumulator, Combiner) and custom containers ===");

        // Collect evens into a TreeSet (sorted, unique)
        TreeSet<Integer> evenTreeSet = numbers.stream()
                .filter(n -> n % 2 == 0)
                .collect(TreeSet::new, TreeSet::add, TreeSet::addAll);
        System.out.println("Evens (TreeSet): " + evenTreeSet);

        // Manual comma-separated join using 3-arg collect into StringBuilder (educational; prefer Collectors.joining)
        StringBuilder sb = numbers.stream()
                .filter(n -> n > 5)
                .map(Object::toString)
                .collect(
                        StringBuilder::new,
                        (b, s) -> {
                            if (b.length() != 0) b.append(", ");
                            b.append(s);
                        },
                        (b1, b2) -> {
                            if (b1.length() != 0 && b2.length() != 0) b1.append(", ");
                            b1.append(b2);
                        }
                );
        System.out.println("Joined >5 (StringBuilder): " + sb.toString());

        // 3-arg collect equivalent of toList() (demonstration)
        List<Integer> evensList = numbers.stream()
                .filter(n -> n % 2 == 0)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        System.out.println("Evens (ArrayList via 3-arg collect): " + evensList);
        System.out.println();
    }

    // 7) flatMap vs map
    private static void flatMapVsMap() {
        System.out.println("=== flatMap vs map ===");

        // map returns Stream<List<String>> when mapping to skills
        List<List<String>> listOfSkillLists = people.stream()
                .map(Person::getSkills)
                .collect(Collectors.toList());
        System.out.println("map -> List<List<String>>: " + listOfSkillLists);

        // flatMap flattens to Stream<String> of skills
        List<String> flattenedSkills = people.stream()
                .flatMap(p -> p.getSkills().stream())
                .collect(Collectors.toList());
        System.out.println("flatMap -> List<String> skills: " + flattenedSkills);

        // Distinct sorted skills
        List<String> distinctSortedSkills = people.stream()
                .flatMap(p -> p.getSkills().stream())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        System.out.println("Distinct sorted skills: " + distinctSortedSkills);

        // Example splitting sentences to words
        List<String> sentences = Arrays.asList("map filter collect", "java streams", "flatMap flattens");
        List<String> allWords = sentences.stream()
                .flatMap(s -> Arrays.stream(s.split("\\s+")))
                .collect(Collectors.toList());
        System.out.println("Words from sentences (flatMap): " + allWords);
        System.out.println();
    }

    // 8) Using peek for debugging a pipeline (avoid side effects in production)
    private static void peekDebugging() {
        System.out.println("=== Debugging with peek (for troubleshooting) ===");

        List<Integer> result = numbers.stream()
                .peek(n -> System.out.println("start: " + n))
                .filter(n -> n % 2 == 0)
                .peek(n -> System.out.println("after filter even: " + n))
                .map(n -> n * n)
                .peek(n -> System.out.println("after map square: " + n))
                .distinct()
                .collect(Collectors.toList());
        System.out.println("Result after pipeline: " + result);
        System.out.println();
    }

    // Model class
    static class Person {
        private final String name;
        private final int age;
        private final String city;
        private final List<String> skills;

        Person(String name, int age, String city, List<String> skills) {
            this.name = name;
            this.age = age;
            this.city = city;
            this.skills = new ArrayList<>(skills);
        }

        String getName() {
            return name;
        }

        int getAge() {
            return age;
        }

        String getCity() {
            return city;
        }

        List<String> getSkills() {
            return Collections.unmodifiableList(skills);
        }

        @Override
        public String toString() {
            return name + "(" + age + ", " + city + ")";
        }
    }
}