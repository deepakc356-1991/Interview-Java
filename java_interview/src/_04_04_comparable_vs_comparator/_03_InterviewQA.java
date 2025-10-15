package _04_04_comparable_vs_comparator;

import java.math.BigDecimal;
import java.text.Collator;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/*
Interview Q&A: Comparable vs Comparator (Basics → Intermediate → Advanced)

Basics
- Q: What is Comparable?
  - A: An interface defining a type’s natural ordering via int compareTo(T other). Implemented by the class itself.
- Q: What is Comparator?
  - A: A strategy object that defines an external/custom ordering via int compare(T a, T b). Multiple comparators per type.
- Q: When to use which?
  - A: Use Comparable when the class has a single obvious natural order. Use Comparator for alternate or multiple orderings.
- Q: How to sort?
  - A: Collections.sort(list) or list.sort(null) use natural order. Use list.sort(comparator) for a Comparator.

Intermediate
- Q: Key differences?
  - Comparable: single natural order; implemented inside type; part of type’s API.
  - Comparator: multiple orders; external to type; flexible; can be swapped at runtime.
- Q: Multi-level sorting?
  - A: Comparator.comparing(key).thenComparing(nextKey) etc.
- Q: Reverse order?
  - A: comparator.reversed() or Comparator.reverseOrder().
- Q: Handling nulls?
  - A: Comparator.nullsFirst(order) / nullsLast(order). compareTo never handles null.
- Q: Consistency with equals?
  - A: If compareTo returns 0, equals should ideally be true. Inconsistent orders can break SortedSet/SortedMap uniqueness.
- Q: Where are they used?
  - A: Sorting (Collections.sort / Arrays.sort), TreeSet/TreeMap/PriorityQueue, Stream.sorted, etc.

Advanced
- Q: Contracts to remember?
  - compareTo/compare must be antisymmetric, transitive, and consistent. sgn(x.compareTo(y)) = -sgn(y.compareTo(x)).
- Q: Common pitfalls?
  - Integer overflow: never do return a - b; use Integer.compare(a, b).
  - BigDecimal: equals vs compareTo differ (1.0 vs 1.00); TreeSet/TreeMap use comparison for uniqueness.
  - Doubles: use Double.compare (handles -0.0 and NaN deterministically).
- Q: Stability?
  - List.sort and Collections.sort (Objects) are stable (TimSort). Arrays.sort(Object[]) is stable; primitive Arrays.sort is not.
- Q: Locale-aware String sorting?
  - Use Collator with Comparator to get language-specific ordering.
- Q: Performance tips?
  - Prefer key-extractor comparators (comparingInt, comparingLong, etc.).
  - Precompute expensive keys outside the comparator if needed.
- Q: Serialization?
  - TreeMap/TreeSet may serialize their comparator; use a serializable comparator if structures are serialized.
- Q: Streams?
  - stream.sorted(Comparator) supports in-pipeline sorting.

See runnable demos below.
*/
public class _03_InterviewQA {

    public static void main(String[] args) {
        demoNaturalVsCustomOrdering();
        demoMultiLevelAndReverse();
        demoNullHandling();
        demoArraysAndStreams();
        demoTreeSetTreeMapAndPriorityQueue();
        demoBigDecimalEqualityTrap();
        demoOverflowPitfall();
        demoLocaleAwareSorting();
        demoStability();
        demoDoublesAndNaN();
        demoSerializableComparatorPattern();
    }

    // ============================================================
    // Domain model
    // ============================================================

    // Natural order by id (ascending)
    static final class Person implements Comparable<Person> {
        private final int id;
        private final String name;
        private final int age;
        private final double salary;

        Person(int id, String name, int age, double salary) {
            this.id = id;
            this.name = name;
            this.age = age;
            this.salary = salary;
        }

        public int getId() { return id; }
        public String getName() { return name; }
        public int getAge() { return age; }
        public double getSalary() { return salary; }

        // Natural order: by id
        @Override
        public int compareTo(Person other) {
            return Integer.compare(this.id, other.id);
        }

        // Consistent with compareTo: equality by id
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Person)) return false;
            Person person = (Person) o;
            return id == person.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return "Person{" +
                    "id=" + id +
                    ", name=" + name +
                    ", age=" + age +
                    ", salary=" + salary +
                    '}';
        }
    }

    // Common Comparators (external/custom orderings)
    static final class PersonComparators {
        static final Comparator<Person> BY_ID = Comparator.comparingInt(Person::getId);
        static final Comparator<Person> BY_NAME_CASE_INSENSITIVE =
                Comparator.comparing(Person::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
        static final Comparator<Person> BY_AGE = Comparator.comparingInt(Person::getAge);
        static final Comparator<Person> BY_SALARY_DESC =
                Comparator.comparingDouble(Person::getSalary).reversed();
        static final Comparator<Person> BY_NAME_THEN_AGE =
                BY_NAME_CASE_INSENSITIVE.thenComparingInt(Person::getAge);
    }

    // ============================================================
    // Demos
    // ============================================================

    private static List<Person> samplePeople() {
        return new ArrayList<>(Arrays.asList(
                new Person(3, "Alice", 30, 120_000),
                new Person(1, "bob", 25, 80_000),
                new Person(2, "Charlie", 35, 150_000),
                new Person(4, null, 40, 60_000), // null name to demo null handling
                new Person(5, "Bob", 25, 90_000)
        ));
    }

    private static void demoNaturalVsCustomOrdering() {
        header("Natural order (Comparable) vs custom (Comparator)");
        List<Person> people = samplePeople();

        // Natural order (by id, from Comparable<Person>)
        List<Person> natural = new ArrayList<>(people);
        Collections.sort(natural);
        print("Natural (by id via Comparable):", natural);

        // Custom order (by name case-insensitive)
        List<Person> byName = new ArrayList<>(people);
        byName.sort(PersonComparators.BY_NAME_CASE_INSENSITIVE);
        print("Custom (by name, case-insensitive, nulls last):", byName);

        // Custom order (by salary desc)
        List<Person> bySalaryDesc = new ArrayList<>(people);
        bySalaryDesc.sort(PersonComparators.BY_SALARY_DESC);
        print("Custom (by salary desc):", bySalaryDesc);
    }

    private static void demoMultiLevelAndReverse() {
        header("Multi-level sorting and reverse");
        List<Person> people = samplePeople();

        // Multi-level: name (case-insensitive), then age
        List<Person> multi = new ArrayList<>(people);
        multi.sort(PersonComparators.BY_NAME_THEN_AGE);
        print("By name, then age:", multi);

        // Reverse: age descending
        List<Person> ageDesc = new ArrayList<>(people);
        ageDesc.sort(PersonComparators.BY_AGE.reversed());
        print("By age (desc):", ageDesc);
    }

    private static void demoNullHandling() {
        header("Null handling with nullsFirst/nullsLast");
        List<String> words = new ArrayList<>(Arrays.asList("Beta", null, "alpha", "Gamma", null, "delta"));
        // Natural order would NPE; use null-friendly comparators
        words.sort(Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER));
        print("Strings with nullsFirst (case-insensitive):", words);

        words.sort(Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
        print("Strings with nullsLast (case-insensitive):", words);
    }

    private static void demoArraysAndStreams() {
        header("Arrays.sort / Streams.sorted");
        String[] arr = {"z", "A", "b", "C"};
        Arrays.sort(arr, String.CASE_INSENSITIVE_ORDER);
        print("Arrays.sort (case-insensitive):", Arrays.asList(arr));

        List<Person> people = samplePeople();
        List<Person> streamSorted =
                people.stream()
                      .sorted(PersonComparators.BY_NAME_THEN_AGE)
                      .collect(Collectors.toList());
        print("Stream.sorted (by name then age):", streamSorted);
    }

    private static void demoTreeSetTreeMapAndPriorityQueue() {
        header("TreeSet / TreeMap / PriorityQueue with comparator");

        // TreeSet using natural order (by id)
        TreeSet<Person> setById = new TreeSet<>();
        setById.addAll(samplePeople());
        print("TreeSet (natural by id):", setById);

        // TreeSet using comparator by name; elements with same name considered duplicates (compare == 0)
        TreeSet<Person> setByName = new TreeSet<>(PersonComparators.BY_NAME_CASE_INSENSITIVE);
        setByName.addAll(samplePeople());
        print("TreeSet (by name; duplicates by same name collapse):", setByName);

        // TreeMap by salary desc
        TreeMap<Person, String> map = new TreeMap<>(PersonComparators.BY_SALARY_DESC);
        for (Person p : samplePeople()) {
            map.put(p, "Emp#" + p.getId());
        }
        print("TreeMap keys sorted by salary desc:", map.keySet());

        // PriorityQueue: min-heap default; use reversed comparator for max-heap by age
        PriorityQueue<Person> maxHeapByAge = new PriorityQueue<>(PersonComparators.BY_AGE.reversed());
        maxHeapByAge.addAll(samplePeople());
        System.out.println("PriorityQueue (max-heap by age):");
        while (!maxHeapByAge.isEmpty()) {
            System.out.println("  poll -> " + maxHeapByAge.poll());
        }
    }

    private static void demoBigDecimalEqualityTrap() {
        header("BigDecimal equals vs compareTo trap (TreeSet/TreeMap uniqueness)");
        BigDecimal a = new BigDecimal("1.0");
        BigDecimal b = new BigDecimal("1.00");
        System.out.println("a.equals(b) = " + a.equals(b)); // false
        System.out.println("a.compareTo(b) = " + a.compareTo(b)); // 0

        // TreeSet uses compareTo/Comparator for uniqueness => one element retained
        TreeSet<BigDecimal> set = new TreeSet<>();
        set.add(a);
        set.add(b);
        print("TreeSet<BigDecimal> after adding 1.0 and 1.00 (only one remains):", set);

        // HashSet uses equals/hashCode => both retained
        HashSet<BigDecimal> hashSet = new HashSet<>();
        hashSet.add(a);
        hashSet.add(b);
        print("HashSet<BigDecimal> after adding 1.0 and 1.00 (both remain):", hashSet);
    }

    private static void demoOverflowPitfall() {
        header("Overflow pitfall: never do return a - b");
        Person pMax = new Person(Integer.MAX_VALUE, "Max", 50, 200_000);
        Person pMin = new Person(Integer.MIN_VALUE, "Min", 20, 50_000);

        Comparator<Person> badById = (x, y) -> x.getId() - y.getId(); // overflow-prone
        int bad = badById.compare(pMax, pMin);
        System.out.println("Bad comparator compare(MAX_VALUE, MIN_VALUE) -> " + bad + " (overflow leads to wrong sign)");

        Comparator<Person> goodById = Comparator.comparingInt(Person::getId);
        int good = goodById.compare(pMax, pMin);
        System.out.println("Good comparator compare(MAX_VALUE, MIN_VALUE) -> " + good);
    }

    private static void demoLocaleAwareSorting() {
        header("Locale-aware String sorting using Collator");
        List<String> names = new ArrayList<>(Arrays.asList("Zoe", "Zoë", "Åke", "Ake", "éclair", "eclair"));

        Collator fr = Collator.getInstance(Locale.FRENCH);
        fr.setStrength(Collator.PRIMARY); // ignore accents/case for primary level
        Comparator<String> frCollator = fr::compare;

        names.sort(frCollator);
        print("French collation (accents folded):", names);

        // Different locale may produce different order
        Collator sv = Collator.getInstance(new Locale("sv", "SE")); // Swedish
        Comparator<String> svCollator = sv::compare;
        names.sort(svCollator);
        print("Swedish collation:", names);
    }

    private static void demoStability() {
        header("Stability: equal keys preserve original order (Object sorts)");
        // Create records with group key and insertion order
        class Rec {
            final int group;
            final int insertion;
            Rec(int group, int insertion) { this.group = group; this.insertion = insertion; }
            @Override public String toString() { return "{" + group + "," + insertion + "}"; }
        }

        List<Rec> recs = new ArrayList<>();
        // group: 2,1,2,1,2 with insertion order 0..4
        recs.add(new Rec(2,0));
        recs.add(new Rec(1,1));
        recs.add(new Rec(2,2));
        recs.add(new Rec(1,3));
        recs.add(new Rec(2,4));

        // Sort by group only; stable sort preserves order within same group
        recs.sort(Comparator.comparingInt(r -> r.group));
        print("Stable sort by group (insertion order preserved within equal keys):", recs);
    }

    private static void demoDoublesAndNaN() {
        header("Double.compare handles NaN and -0.0 vs 0.0");
        List<Double> nums = new ArrayList<>(Arrays.asList(Double.NaN, 0.0, -0.0, 42.0, -5.0, Double.NaN));
        nums.sort(Double::compare);
        print("Sorted Doubles (NaN last, -0.0 < 0.0):", nums);
    }

    private static void demoSerializableComparatorPattern() {
        header("Serializable comparator pattern (for serializing TreeMap/TreeSet)");
        // Example: a named comparator class is serializable; lambdas typically are not
        TreeSet<Person> serializableSet = new TreeSet<>(new ByNameSerializable());
        serializableSet.addAll(samplePeople());
        print("TreeSet with serializable comparator (by name):", serializableSet);

        // An enum comparator is also serializable by default
        TreeSet<Person> enumComparatorSet = new TreeSet<>(PersonOrder.BY_AGE);
        enumComparatorSet.addAll(samplePeople());
        print("TreeSet with enum comparator (by age):", enumComparatorSet);
    }

    // Serializable comparator class
    static final class ByNameSerializable implements Comparator<Person>, java.io.Serializable {
        private static final long serialVersionUID = 1L;
        @Override public int compare(Person a, Person b) {
            return Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                    .compare(a.getName(), b.getName());
        }
    }

    // Enum comparators (serializable by default)
    enum PersonOrder implements Comparator<Person> {
        BY_AGE {
            @Override public int compare(Person a, Person b) {
                return Integer.compare(a.getAge(), b.getAge());
            }
        },
        BY_SALARY_DESC {
            @Override public int compare(Person a, Person b) {
                return Double.compare(b.getSalary(), a.getSalary());
            }
        }
    }

    // ============================================================
    // Utilities
    // ============================================================

    private static void header(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }

    private static <T> void print(String title, Collection<T> items) {
        System.out.println(title);
        for (T t : items) {
            System.out.println("  " + t);
        }
    }
}