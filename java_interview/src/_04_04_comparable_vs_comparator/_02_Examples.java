package _04_04_comparable_vs_comparator;

import java.text.Collator;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Comparable vs Comparator - comprehensive, self-contained examples.
 *
 * Read the comments next to each example to understand the intent.
 */
public class _02_Examples {

    // Domain model: Person implements Comparable to provide a natural order.
    // Natural order here is by name (case-sensitive), then id as a tiebreaker to avoid returning 0 for different ids.
    // NOTE: equals/hashCode are based only on id (identity). This is common, but then Comparable is not "consistent with equals".
    // That is fine for sorting lists, but can cause surprises in sorted sets/maps if compareTo returns 0 for two different ids.
    static class Person implements Comparable<Person> {
        private final int id;
        private final String name;
        private final int age;        // years
        private final double height;  // meters
        private final String city;    // may be null
        private final LocalDate joinDate; // may be null

        Person(int id, String name, int age, double height, String city, LocalDate joinDate) {
            this.id = id;
            this.name = Objects.requireNonNull(name, "name");
            this.age = age;
            this.height = height;
            this.city = city;
            this.joinDate = joinDate;
        }

        // Natural order (Comparable): name ASC, then id ASC
        @Override
        public int compareTo(Person other) {
            Objects.requireNonNull(other, "Cannot compare to null");
            int byName = this.name.compareTo(other.name);
            if (byName != 0) return byName;
            return Integer.compare(this.id, other.id);
        }

        // Equals/HashCode by id (identity).
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Person)) return false;
            Person person = (Person) o;
            return id == person.id;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(id);
        }

        public int getId() { return id; }
        public String getName() { return name; }
        public int getAge() { return age; }
        public double getHeight() { return height; }
        public String getCity() { return city; }
        public LocalDate getJoinDate() { return joinDate; }

        @Override
        public String toString() {
            return String.format("{id=%d, name=%s, age=%d, h=%.2f, city=%s, join=%s}",
                    id, name, age, height, city, String.valueOf(joinDate));
        }
    }

    // Common Comparator examples (static for reuse)
    static final Comparator<Person> BY_ID =
            Comparator.comparingInt(Person::getId);

    static final Comparator<Person> BY_AGE =
            Comparator.comparingInt(Person::getAge); // asc

    static final Comparator<Person> BY_HEIGHT_DESC =
            Comparator.comparingDouble(Person::getHeight).reversed();

    static final Comparator<Person> BY_NAME_CI =
            Comparator.comparing(Person::getName, String.CASE_INSENSITIVE_ORDER);

    static final Comparator<Person> BY_CITY_NULLS_FIRST_CI =
            Comparator.comparing(Person::getCity, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER));

    static final Comparator<Person> BY_JOIN_DATE_NEWEST_FIRST =
            Comparator.comparing(Person::getJoinDate, Comparator.nullsLast(Comparator.naturalOrder()))
                      .reversed();

    static final Comparator<Person> BY_AGE_THEN_NAME =
            Comparator.comparingInt(Person::getAge)
                      .thenComparing(Person::getName);

    // Multi-level comparator with null-safety and case-insensitive tie-breakers.
    static final Comparator<Person> COMPLEX_COMPARATOR =
            Comparator.comparing(Person::getCity, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                      .thenComparingInt(Person::getAge)
                      .thenComparing(Person::getName, String.CASE_INSENSITIVE_ORDER);

    // "Bad" comparator for sets: compares only by name (case-insensitive) and ignores id.
    // Using this in TreeSet/TreeMap will collapse different people with same name as "duplicates".
    static final Comparator<Person> BAD_BY_NAME_ONLY_CI =
            Comparator.comparing(Person::getName, String.CASE_INSENSITIVE_ORDER);

    public static void main(String[] args) {
        // Sample data
        List<Person> people = new ArrayList<>(Arrays.asList(
                new Person(1, "Alice",   30, 1.65, "Berlin",   LocalDate.of(2020, 1, 10)),
                new Person(2, "bob",     25, 1.80, "Paris",    LocalDate.of(2019, 5, 23)),
                new Person(3, "Charlie", 35, 1.75, null,       LocalDate.of(2021, 2, 14)),
                new Person(4, "ALICE",   28, 1.70, "berlin",   LocalDate.of(2018, 3, 1)),
                new Person(5, "Bob",     25, 1.82, "London",   null),
                new Person(6, "Élodie",  29, 1.60, "Paris",    LocalDate.of(2022, 11, 11)),
                new Person(7, "dave",    25, 1.90, null,       LocalDate.of(2017, 8, 8)),
                new Person(8, "张伟",     40, 1.76, "Shanghai", LocalDate.of(2015, 4, 4)),
                new Person(9, "Özil",    31, 1.81, "Istanbul", LocalDate.of(2020, 7, 7)),
                null // include a null to show null-safe comparators
        ));

        List<Person> peopleNoNull = people.stream().filter(Objects::nonNull).collect(Collectors.toList());

        header("Original (with a null at end)");
        printList(people);

        // 1) Comparable: natural order (Person implements Comparable)
        header("Comparable (natural order): name ASC, then id ASC");
        List<Person> byNatural = new ArrayList<>(peopleNoNull);
        Collections.sort(byNatural); // or byNatural.sort(null)
        printList(byNatural);

        // 2) Comparator: simple property (int) using comparingInt
        header("Comparator: by age ASC");
        List<Person> byAge = new ArrayList<>(peopleNoNull);
        byAge.sort(BY_AGE);
        printList(byAge);

        // 3) Comparator: primitive double with reversed (height DESC)
        header("Comparator: by height DESC (using comparingDouble + reversed)");
        List<Person> byHeightDesc = new ArrayList<>(peopleNoNull);
        byHeightDesc.sort(BY_HEIGHT_DESC);
        printList(byHeightDesc);

        // 4) Comparator: case-insensitive by name
        header("Comparator: by name (case-insensitive)");
        List<Person> byNameCI = new ArrayList<>(peopleNoNull);
        byNameCI.sort(BY_NAME_CI);
        printList(byNameCI);

        // 5) Comparator: join date newest first, nulls last
        header("Comparator: by joinDate NEWEST first (nulls last)");
        List<Person> byJoinNewest = new ArrayList<>(peopleNoNull);
        byJoinNewest.sort(BY_JOIN_DATE_NEWEST_FIRST);
        printList(byJoinNewest);

        // 6) Comparator chaining: city (CI, nulls last) -> age -> name (CI)
        header("Comparator chaining: city(CI, nulls last) -> age -> name(CI)");
        List<Person> complex = new ArrayList<>(peopleNoNull);
        complex.sort(COMPLEX_COMPARATOR);
        printList(complex);

        // 7) Reversing entire comparator chain
        header("Reversed chain: (age -> name) reversed");
        List<Person> reversedChain = new ArrayList<>(peopleNoNull);
        reversedChain.sort(BY_AGE_THEN_NAME.reversed());
        printList(reversedChain);

        // 8) Sorting when the list can contain null elements
        header("Sorting with null elements: nulls FIRST, then name(CI)");
        List<Person> withNullsSortedFirst = new ArrayList<>(people);
        withNullsSortedFirst.sort(Comparator.nullsFirst(BY_NAME_CI));
        printList(withNullsSortedFirst);

        // 9) Using TreeSet with natural order (Comparable)
        header("TreeSet with NATURAL order (Comparable)");
        Set<Person> treeSetNatural = new TreeSet<>(peopleNoNull);
        printSet(treeSetNatural);

        // 10) TreeSet with a comparator that ignores id: duplicates by name (CI) collapse
        header("TreeSet with BAD comparator (by name CI only): 'Alice'/'ALICE' collapse");
        Set<Person> treeSetByNameOnly = new TreeSet<>(BAD_BY_NAME_ONLY_CI);
        treeSetByNameOnly.addAll(peopleNoNull);
        printSet(treeSetByNameOnly);

        // 11) HashSet vs TreeSet duplicate behavior (equals vs comparator equality)
        header("HashSet vs TreeSet: two 'Bob' entries (ids 2 and 5)");
        Person bob2 = peopleNoNull.stream().filter(p -> p.getId() == 2).findFirst().get();
        Person bob5 = peopleNoNull.stream().filter(p -> p.getId() == 5).findFirst().get();
        Set<Person> hashSet = new HashSet<>();
        hashSet.add(bob2);
        hashSet.add(bob5);
        System.out.println("HashSet size (equals by id): " + hashSet.size() + " -> keeps both");
        Set<Person> treeSetNameOnly = new TreeSet<>(BAD_BY_NAME_ONLY_CI);
        treeSetNameOnly.add(bob2);
        treeSetNameOnly.add(bob5);
        System.out.println("TreeSet size (comparator by name CI): " + treeSetNameOnly.size() + " -> collapses to 1");

        // 12) Arrays.sort with Comparator
        header("Arrays.sort with Comparator (by id)");
        Person[] arr = peopleNoNull.toArray(new Person[0]);
        Arrays.sort(arr, BY_ID);
        printArray(arr);

        // 13) Streams: sorted + limit
        header("Streams: top 5 oldest (age DESC, then name CI)");
        peopleNoNull.stream()
                .sorted(BY_AGE.reversed().thenComparing(BY_NAME_CI))
                .limit(5)
                .forEach(System.out::println);

        // 14) Pre-Java 8 style: anonymous Comparator class
        header("Anonymous Comparator class (pre-Java 8): by age ASC");
        List<Person> byAgeOldStyle = new ArrayList<>(peopleNoNull);
        Collections.sort(byAgeOldStyle, new Comparator<Person>() {
            @Override
            public int compare(Person a, Person b) {
                return Integer.compare(a.getAge(), b.getAge());
            }
        });
        printList(byAgeOldStyle);

        // 15) Comparator.comparing with key Comparator: name using CASE_INSENSITIVE_ORDER
        header("Comparator.comparing with key-Comparator: name (CASE_INSENSITIVE_ORDER)");
        List<Person> keyComparatorExample = new ArrayList<>(peopleNoNull);
        keyComparatorExample.sort(Comparator.comparing(Person::getName, String.CASE_INSENSITIVE_ORDER));
        printList(keyComparatorExample);

        // 16) Locale-sensitive string comparison (Collator)
        header("Locale-sensitive collation example (Turkish 'I')");
        localeSensitiveStringSort();

        // 17) Nested derived-key comparison: city length ASC (nulls last), then id
        header("Derived key: city length ASC (nulls last), then id");
        List<Person> byCityLength = new ArrayList<>(peopleNoNull);
        byCityLength.sort(
                Comparator.comparingInt((Person p) -> p.getCity() == null ? Integer.MAX_VALUE : p.getCity().length())
                          .thenComparingInt(Person::getId)
        );
        printList(byCityLength);
    }

    // Locale-sensitive ordering example using Collator
    private static void localeSensitiveStringSort() {
        List<String> words = Arrays.asList("I", "İ", "i", "ı"); // Turkish dot/dotless I variants
        List<String> defaultOrder = new ArrayList<>(words);
        Collections.sort(defaultOrder);
        System.out.println("Default locale sort: " + defaultOrder);

        Collator tr = Collator.getInstance(new Locale("tr", "TR"));
        tr.setStrength(Collator.PRIMARY); // ignore accents/case differences at primary level
        List<String> turkishOrder = new ArrayList<>(words);
        turkishOrder.sort(tr::compare);
        System.out.println("Turkish locale sort: " + turkishOrder);
    }

    // Helper printing
    private static void header(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }

    private static void printList(List<Person> list) {
        for (Person p : list) {
            System.out.println(p);
        }
    }

    private static void printSet(Set<Person> set) {
        for (Person p : set) {
            System.out.println(p);
        }
    }

    private static void printArray(Person[] arr) {
        for (Person p : arr) {
            System.out.println(p);
        }
    }
}