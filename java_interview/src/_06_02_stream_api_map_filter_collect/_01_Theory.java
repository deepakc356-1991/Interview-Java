package _06_02_stream_api_map_filter_collect;

import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.*;

import static java.util.stream.Collectors.*;

/**
 * Stream API theory and practice focused on map, filter, collect.
 *
 * Key ideas:
 * - Stream: a lazy, single-use view over a sequence of elements supporting functional-style operations.
 *   It does not store data; it pulls from a source (collection, array, generator, file), flows through a pipeline of
 *   intermediate operations (like map, filter), then ends with a terminal operation (like collect).
 * - Pipelines are lazy: intermediate operations are not executed until a terminal operation runs.
 * - Functions passed to stream operations should be stateless and side-effect free (especially for parallel streams).
 * - Ordering: streams are usually ordered if the source is ordered (like List). Some operations depend on ordering
 *   (findFirst, forEachOrdered), while others can ignore it for performance (findAny, unordered).
 *
 * Core operations here:
 * - filter(Predicate): intermediate operation, keeps elements that satisfy a boolean condition.
 * - map(Function): intermediate operation, transforms each element into another form (1-to-1).
 *   Related: flatMap(Function) for 1-to-many flattening; mapToInt/Long/Double for primitive streams to avoid boxing.
 * - collect(Collector): terminal operation, folds elements into a container or summary (List, Set, Map, String, stats).
 *   There is also a 3-arg collect(supplier, accumulator, combiner) for custom mutable reductions.
 *
 * Notes on Collectors:
 * - Collectors.toList()/toSet() have unspecified concrete types/mutability. Do not rely on implementation details.
 *   For an unmodifiable result, use collectingAndThen(..., Collections::unmodifiableList/Set) or
 *   (on newer JDKs) Collectors.toUnmodifiableList/Set.
 * - Collectors.toMap requires keys and values to be non-null and keys unique unless you provide a merge function.
 * - groupingBy classifies elements into a Map<K, List<T>> (or other downstream result).
 * - partitioningBy splits into Map<Boolean, List<T>> by a predicate.
 * - joining concatenates CharSequence elements with optional delimiter/prefix/suffix.
 * - summarizingInt/Long/Double returns count, sum, min, average, max in one pass.
 *
 * Performance tips:
 * - Filter early (reduce elements) before expensive map operations.
 * - Prefer primitive streams (mapToInt/Long/Double) to reduce boxing.
 * - Avoid stateful lambdas and shared mutable state; especially important with parallel streams.
 */
public class _01_Theory {

    public static void main(String[] args) {
        List<Person> people = samplePeople();

        // 1) Basic pipeline: filter -> map -> collect
        // Keep adults, transform to uppercase names, collect to a List.
        List<String> adultNamesUpper = people.stream()
                .filter(p -> p.getAge() >= 18)                // filter: keep adults
                .map(Person::getName)                          // map: to name (may be null)
                .filter(Objects::nonNull)                      // filter: remove null names
                .map(String::toUpperCase)                      // map: transform to uppercase
                .collect(toList());                            // collect: fold into a List

        System.out.println("Adults (names upper): " + adultNamesUpper);

        // 2) Laziness and execution order
        // Observe that nothing runs until a terminal operation (collect) is invoked.
        List<String> debugOrder = people.stream()
                .filter(p -> {
                    System.out.println("[filter] " + p.getName());
                    return p.getAge() >= 18;
                })
                .map(p -> {
                    System.out.println("[map] " + p.getName());
                    return p.getName();
                })
                .filter(Objects::nonNull)
                .collect(toList()); // triggers actual work
        System.out.println("Debug order result size: " + debugOrder.size());

        // 3) Filter early vs map early (prefer filter first to do less work)
        List<Integer> nameLengthsFilteredFirst = people.stream()
                .filter(p -> p.getAge() >= 18)
                .map(Person::getName)
                .filter(Objects::nonNull)
                .map(String::length)
                .collect(toList());

        List<Integer> nameLengthsMappedFirst = people.stream()
                .map(Person::getName)               // mapping first may force work on all elements
                .filter(Objects::nonNull)
                .map(String::length)
                .collect(toList());
        System.out.println("Lengths (filtered first): " + nameLengthsFilteredFirst);
        System.out.println("Lengths (mapped first) : " + nameLengthsMappedFirst);

        // 4) Primitive streams: mapToInt avoids boxing/unboxing
        int totalAge = people.stream()
                .mapToInt(Person::getAge)
                .sum();
        OptionalDouble averageAge = people.stream()
                .mapToInt(Person::getAge)
                .average();
        System.out.println("Total age: " + totalAge + ", Average age: " + averageAge.orElse(Double.NaN));

        // 5) collect to common containers
        // - toList() / toSet(): unspecified mutability/type; do not rely on implementation details.
        List<String> namesList = people.stream().map(Person::getName).filter(Objects::nonNull).collect(toList());
        Set<String> citiesSet = people.stream().map(Person::getCity).collect(toSet());
        // - toCollection: specify the exact type
        LinkedHashSet<String> orderedCities = people.stream()
                .map(Person::getCity)
                .collect(toCollection(LinkedHashSet::new)); // preserves encounter order
        System.out.println("Names (list): " + namesList);
        System.out.println("Cities (set): " + citiesSet);
        System.out.println("Cities (linked, ordered): " + orderedCities);

        // 6) toMap: key/value mapping, duplicates, merge, and map type
        // Pitfall: duplicate keys without merge function -> IllegalStateException
        try {
            Map<String, Integer> agesByCityNoMerge = people.stream()
                    .collect(toMap(Person::getCity, Person::getAge)); // duplicate cities exist -> boom
            System.out.println(agesByCityNoMerge); // unreachable
        } catch (IllegalStateException ex) {
            System.out.println("toMap without merge failed (duplicate keys): " + ex.getMessage());
        }

        // Provide a merge function to handle duplicates (e.g., keep max age per city)
        Map<String, Integer> maxAgeByCity = people.stream()
                .collect(toMap(
                        Person::getCity,               // key mapper
                        Person::getAge,                // value mapper
                        Integer::max                   // merge function when keys collide
                ));
        System.out.println("Max age by city: " + maxAgeByCity);

        // Provide a map supplier (e.g., LinkedHashMap to preserve order)
        Map<String, Integer> maxAgeByCityOrdered = people.stream()
                .collect(toMap(
                        Person::getCity,
                        Person::getAge,
                        Integer::max,
                        LinkedHashMap::new
                ));
        System.out.println("Max age by city (ordered map): " + maxAgeByCityOrdered);

        // Pitfall: toMap does not allow null keys or values (Map.merge throws NPE). Filter them out first.
        try {
            Map<String, String> nameToNull = people.stream()
                    .map(Person::getName)
                    .collect(toMap(Function.identity(), n -> null)); // will throw NPE
            System.out.println(nameToNull); // unreachable
        } catch (NullPointerException ex) {
            System.out.println("toMap failed (null values not allowed): " + ex);
        }

        // 7) joining: concatenate strings
        String csvNames = people.stream()
                .map(Person::getName)
                .filter(Objects::nonNull)
                .collect(joining(", "));
        System.out.println("CSV names: " + csvNames);

        // 8) groupingBy: group elements by classifier
        Map<String, List<Person>> byCity = people.stream()
                .collect(groupingBy(Person::getCity));
        System.out.println("Grouped by city: " + byCity);

        // groupingBy with downstream: collect only names per city
        Map<String, List<String>> namesByCity = people.stream()
                .filter(p -> p.getName() != null)
                .collect(groupingBy(Person::getCity, mapping(Person::getName, toList())));
        System.out.println("Names by city: " + namesByCity);

        // Multi-level grouping: department -> city -> names
        Map<String, Map<String, List<String>>> namesByDeptThenCity = people.stream()
                .filter(p -> p.getName() != null)
                .collect(groupingBy(Person::getDepartment,
                        groupingBy(Person::getCity, mapping(Person::getName, toList()))));
        System.out.println("Names by dept then city: " + namesByDeptThenCity);

        // 9) partitioningBy: split by a boolean predicate
        Map<Boolean, List<Person>> partitionAdults = people.stream()
                .collect(partitioningBy(p -> p.getAge() >= 18));
        System.out.println("Partition adults: " + partitionAdults);

        // 10) summarizing: one pass summary stats
        IntSummaryStatistics ageStats = people.stream()
                .collect(summarizingInt(Person::getAge));
        System.out.println("Age stats: " + ageStats);

        // 11) collectingAndThen: post-process the collected result (e.g., make unmodifiable)
        List<String> immutableUpperNames = people.stream()
                .map(Person::getName)
                .filter(Objects::nonNull)
                .map(String::toUpperCase)
                .collect(collectingAndThen(toList(), Collections::unmodifiableList));
        System.out.println("Immutable names: " + immutableUpperNames);

        // 12) Three-arg collect(supplier, accumulator, combiner): custom mutable reduction
        // Example: case-insensitive sorted set of distinct names
        TreeSet<String> distinctNamesCI = people.stream()
                .map(Person::getName)
                .filter(Objects::nonNull)
                .collect(
                        () -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER), // supplier
                        TreeSet::add,                                        // accumulator
                        TreeSet::addAll                                      // combiner (for parallel)
                );
        System.out.println("Distinct names (case-insensitive): " + distinctNamesCI);

        // 13) Parallel streams: be careful with ordering and side effects
        // Note: Functions must be stateless and side-effect free for correctness.
        List<String> parallelUpper = people.parallelStream()
                .map(Person::getName)
                .filter(Objects::nonNull)
                .map(String::toUpperCase)
                .collect(toList());
        System.out.println("Parallel upper names: " + parallelUpper);

        // Preserve encounter order when needed with forEachOrdered
        System.out.print("Parallel forEach (order not guaranteed): ");
        people.parallelStream().map(Person::getName).forEach(n -> System.out.print(n + " | "));
        System.out.println();
        System.out.print("Parallel forEachOrdered (order preserved): ");
        people.parallelStream().map(Person::getName).forEachOrdered(n -> System.out.print(n + " | "));
        System.out.println();

        // 14) Stream re-use pitfall: a stream can be consumed only once
        Stream<Person> reusable = people.stream();
        long count = reusable.filter(p -> p.getAge() >= 18).count(); // terminal operation
        System.out.println("Adult count: " + count);
        try {
            // Reusing the same stream after a terminal operation throws IllegalStateException
            reusable.filter(p -> p.isActive()).count();
        } catch (IllegalStateException ex) {
            System.out.println("Stream reuse failed: " + ex.getMessage());
        }

        // 15) AnyMatch/AllMatch/NoneMatch: terminal short-circuiting (related to filter/map usage)
        boolean anyMinor = people.stream().anyMatch(p -> p.getAge() < 18);
        boolean allAdults = people.stream().allMatch(p -> p.getAge() >= 18);
        System.out.println("Any minor? " + anyMinor + ", All adults? " + allAdults);

        // 16) Peek is for debugging only (do not rely on side effects). Prefer map/filter for logic.
        List<String> debugPeek = people.stream()
                .peek(p -> System.out.println("[peek] before: " + p))
                .filter(p -> p.getAge() >= 18)
                .peek(p -> System.out.println("[peek] after filter: " + p))
                .map(Person::getName)
                .filter(Objects::nonNull)
                .collect(toList());
        System.out.println("Debug peek collected: " + debugPeek);

        // 17) Concurrent collectors (advanced): toConcurrentMap with parallel streams
        // Only useful when source is concurrent and collector is CONCURRENT. Shown here for theory.
        ConcurrentMap<String, Integer> concurrentMaxAgeByCity = people.parallelStream()
                .collect(toConcurrentMap(Person::getCity, Person::getAge, Integer::max));
        System.out.println("Concurrent max age by city: " + concurrentMaxAgeByCity);

        // 18) flatMap (related to map): flatten nested collections
        // Here, split names into characters and collect distinct sorted characters.
        SortedSet<String> distinctChars = people.stream()
                .map(Person::getName)
                .filter(Objects::nonNull)
                .flatMap(name -> Arrays.stream(name.split("")))
                .collect(toCollection(TreeSet::new));
        System.out.println("Distinct characters across names: " + distinctChars);

        // 19) Comparing collect vs forEach for accumulation
        // Prefer collect to aggregate into a collection; avoid mutating external state with forEach.
        // Bad style (shared mutable state):
        List<String> ext = new ArrayList<>();
        people.stream().map(Person::getName).filter(Objects::nonNull).forEach(ext::add); // works but not ideal
        // Better:
        List<String> extBetter = people.stream().map(Person::getName).filter(Objects::nonNull).collect(toList());
        System.out.println("External list (forEach): " + ext);
        System.out.println("External list (collect): " + extBetter);
    }

    // Sample domain model
    static class Person {
        private final String name;
        private final int age;
        private final String city;
        private final String department;
        private final boolean active;

        Person(String name, int age, String city, String department, boolean active) {
            this.name = name;
            this.age = age;
            this.city = city;
            this.department = department;
            this.active = active;
        }

        public String getName() { return name; }
        public int getAge() { return age; }
        public String getCity() { return city; }
        public String getDepartment() { return department; }
        public boolean isActive() { return active; }

        @Override
        public String toString() {
            return "Person{" +
                    "name='" + name + '\'' +
                    ", age=" + age +
                    ", city='" + city + '\'' +
                    ", department='" + department + '\'' +
                    ", active=" + active +
                    '}';
        }
    }

    private static List<Person> samplePeople() {
        // Includes duplicate names/cities and a null name for demonstrations
        List<Person> list = new ArrayList<>();
        list.add(new Person("Alice", 30, "New York", "IT", true));
        list.add(new Person("Bob", 17, "Boston", "HR", false));
        list.add(new Person("Charlie", 25, "Boston", "IT", true));
        list.add(new Person("Diana", 30, "Denver", "Finance", true));
        list.add(new Person("Eve", 35, "New York", "Finance", false));
        list.add(new Person(null, 22, "New York", "IT", true));     // null name to show filtering nulls
        list.add(new Person("Bob", 19, "Chicago", "HR", true));     // duplicate name "Bob"
        list.add(new Person("Frank", 40, "Boston", "IT", true));
        list.add(new Person("Grace", 22, "Chicago", "Marketing", false));
        return list;
    }
}