package _04_04_comparable_vs_comparator;

import java.text.Collator;
import java.util.*;
import java.util.stream.Collectors;

/*
Comparable vs Comparator (comprehensive theory + runnable examples)

Comparable<T> (java.lang)
- Purpose: define a type’s natural order inside the type itself.
- How: implement int compareTo(T other).
- Single strategy: a class can only have one natural order.
- Typical use: when there is an obvious, widely-accepted “natural” ordering (e.g., numeric id, alphabetical name).
- Contract for compareTo:
  - sign(x.compareTo(y)) == -sign(y.compareTo(x)) (anti-symmetric)
  - transitive: if x>y and y>z then x>z
  - consistency: repeated comparisons give the same result (unless objects are mutated)
  - if x.compareTo(y) == 0 then x and y are “equal in ordering” (not necessarily equals()).
- Consistency with equals:
  - Not required, but STRONGLY recommended if instances are used as keys in sorted collections (TreeSet, TreeMap).
  - If not consistent, TreeSet/TreeMap use compareTo==0 to decide “duplicate key,” which may differ from equals(), causing surprises.

Comparator<T> (java.util)
- Purpose: define external ordering strategies; you can have many.
- How: pass to sorting/collection APIs or store as a constant; use lambdas/method references.
- Composition helpers (Java 8+):
  - Comparator.comparing(keyExtractor), .comparingInt, .comparingLong, .comparingDouble
  - thenComparing(...), reversed(), nullsFirst(...), nullsLast(...)
  - naturalOrder(), reverseOrder()
- Typical use:
  - You don’t own the class, or
  - You need multiple different orderings (e.g., by age, by name, by city, etc.), or
  - You need locale-aware text ordering (Collator), or null-friendly ordering.
- Contract for Comparator.compare is the same as Comparable’s compareTo.

APIs using Comparable/Comparator
- List.sort(Comparator), Collections.sort(List), Arrays.sort(T[])
- Stream.sorted(), Stream.sorted(Comparator)
- Sorted collections: TreeSet, TreeMap (keys)
- PriorityQueue (heap ordering)
- For generics:
  - Natural sort methods often use T extends Comparable<? super T>
  - Comparator-taking methods use Comparator<? super T> (consumer-lower bound)

Performance and stability
- Java object sorting is O(n log n) via TimSort; it is stable.
- Prefer key-extractor comparators (comparingInt, comparingDouble) to avoid boxing.
- Don’t subtract ints/longs in compare (risk of overflow). Use Integer.compare/Long.compare, etc.

Nulls and locale
- Comparable#compareTo typically assumes non-null (may NPE).
- Use Comparator.nullsFirst/Last when needed.
- String natural order is Unicode code-point; for language rules use Collator (e.g., Swedish, German).

Mutability hazard
- Do not mutate fields that participate in comparison while instances are stored in sorted collections.
  Doing so may corrupt ordering invariants and cause hard-to-reproduce bugs.

This file demonstrates:
- Implementing Comparable (natural order)
- Building multiple Comparators (age, height, name, null-handling, multi-level)
- Sorting Lists/Arrays/Streams
- Using TreeSet/TreeMap/PriorityQueue
- Locale-aware sorting with Collator
- Generics bounds helpers
- Equals vs compareTo inconsistency effects
*/
public class _01_Theory {

    // Reusable Comparator examples (Java 8+ factory methods and composition):
    static final Comparator<Person> AGE_ASC = Comparator.comparingInt((Person p) -> p.age);
    static final Comparator<Person> AGE_DESC = AGE_ASC.reversed();
    static final Comparator<Person> HEIGHT_ASC = Comparator.comparingDouble((Person p) -> p.height);
    static final Comparator<Person> NAME_CI = Comparator.comparing((Person p) -> p.name, String.CASE_INSENSITIVE_ORDER);
    static final Comparator<Person> CITY_NULLS_LAST =
            Comparator.comparing((Person p) -> p.city, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
    static final Comparator<Person> MULTI =
            Comparator.comparingInt((Person p) -> p.age)
                    .thenComparing(p -> p.name, String.CASE_INSENSITIVE_ORDER)
                    .thenComparingDouble(p -> p.height);

    public static void main(String[] args) {
        // Sample data
        List<Person> people = new ArrayList<>();
        people.add(new Person(2, "zoe", 28, 1.65, null));
        people.add(new Person(1, "Alex", 30, 1.80, "Paris"));
        people.add(new Person(3, "alex", 22, 1.75, "Berlin")); // same name ignoring case as id=1
        people.add(new Person(4, "Mike", 30, 1.70, "Oslo"));
        people.add(new Person(5, "Zoë", 28, 1.66, "Madrid")); // diacritic

        // Using Comparable (natural order) -> Person.compareTo: by name (case-insensitive)
        print("Natural (Comparable) by name, case-insensitive", sortCopy(people, null));

        // Using Comparators
        print("Comparator: age asc", sortCopy(people, AGE_ASC));
        print("Comparator: age asc, then name ci, then height", sortCopy(people, MULTI));
        print("Comparator: city nulls last", sortCopy(people, CITY_NULLS_LAST));
        print("Comparator: age desc", sortCopy(people, AGE_DESC));

        // Arrays.sort with natural and custom comparator
        Person[] arr = people.toArray(new Person[0]);
        Arrays.sort(arr); // natural order (Comparable)
        printArray("Arrays.sort natural", arr);
        Arrays.sort(arr, HEIGHT_ASC);
        printArray("Arrays.sort by height asc", arr);

        // Streams
        List<Person> byNameDesc = people.stream().sorted(NAME_CI.reversed()).collect(Collectors.toList());
        print("Stream.sorted by name desc", byNameDesc);

        // TreeSet/HashSet and equals vs compareTo consistency:
        // Person.equals uses id; Person.compareTo uses name (case-insensitive) -> inconsistent.
        // TreeSet uses compareTo==0 to define duplicates; HashSet uses equals().
        Set<Person> hashSet = new HashSet<>(people);
        Set<Person> treeSetNatural = new TreeSet<>(); // uses Person.compareTo (by name)
        treeSetNatural.addAll(people);
        System.out.println("HashSet size=" + hashSet.size() + " vs TreeSet(natural) size=" + treeSetNatural.size());
        System.out.println("HashSet=" + hashSet);
        System.out.println("TreeSet(natural)=" + treeSetNatural);

        // TreeSet with Comparator by id -> consistent with equals (id-based)
        Set<Person> treeSetById = new TreeSet<>(Comparator.comparingInt(p -> p.id));
        treeSetById.addAll(people);
        System.out.println("TreeSet(by id) size=" + treeSetById.size());

        // TreeMap: uses natural order of keys unless a Comparator is supplied
        Map<User, String> mapByNatural = new TreeMap<>(); // User.compareTo by id
        mapByNatural.put(new User(10, "u10"), "ten");
        mapByNatural.put(new User(2, "u02"), "two");
        mapByNatural.put(new User(5, "u05"), "five");
        System.out.println("TreeMap<User,natural(id)>: " + mapByNatural);

        // TreeMap with Comparator by username (note: keys "equal" by comparator replace)
        Map<User, String> mapByName = new TreeMap<>(Comparator.comparing(u -> u.username));
        mapByName.put(new User(10, "Ann"), "A");
        mapByName.put(new User(2, "Bob"), "B");
        mapByName.put(new User(5, "Bob"), "B2"); // replaces previous "Bob" key because compare==0
        System.out.println("TreeMap<User, by username>: " + mapByName);

        // PriorityQueue orders by comparator
        Queue<Person> pq = new PriorityQueue<>(AGE_ASC);
        pq.addAll(people);
        System.out.print("PriorityQueue poll by age: ");
        while (!pq.isEmpty()) {
            Person p = pq.poll();
            System.out.print(p.name + "(" + p.age + ") ");
        }
        System.out.println();

        // Locale-aware sorting with Collator (e.g., Swedish)
        List<String> strings = new ArrayList<>(Arrays.asList("Å", "Ä", "A", "Z", "Ö", "Zoë", "zoe", "alex", "Alex"));
        Collator sv = Collator.getInstance(new Locale("sv", "SE"));
        Comparator<String> svComparator = sv::compare;
        List<String> seSorted = strings.stream().sorted(svComparator).collect(Collectors.toList());
        print("Locale(SV) collator", seSorted);
        List<String> defaultSorted = strings.stream().sorted(String.CASE_INSENSITIVE_ORDER).collect(Collectors.toList());
        print("Default case-insensitive", defaultSorted);

        // naturalOrder / reverseOrder
        List<Integer> nums = new ArrayList<>(Arrays.asList(5, 1, 3, 2));
        nums.sort(Comparator.naturalOrder());
        print("numbers natural", nums);
        nums.sort(Comparator.reverseOrder());
        print("numbers reverse", nums);

        // null-friendly comparator example
        List<String> s2 = new ArrayList<>(Arrays.asList("b", null, "a"));
        s2.sort(Comparator.nullsFirst(Comparator.naturalOrder()));
        print("nullsFirst", s2);

        // Safe numeric comparison (avoid overflow)
        System.out.println("Safe compare ints example: " + Integer.compare(2_000_000_000, -2_000_000_000));
    }

    // Helper: make a sorted copy with Optional comparator (null => natural order)
    static List<Person> sortCopy(List<Person> people, Comparator<Person> cmp) {
        List<Person> copy = new ArrayList<>(people);
        if (cmp == null) {
            Collections.sort(copy); // natural order (Comparable)
        } else {
            copy.sort(cmp);
        }
        return copy;
    }

    static void print(String label, List<?> list) {
        System.out.println(label + ": " + list);
    }

    static void printArray(String label, Object[] arr) {
        System.out.println(label + ": " + Arrays.toString(arr));
    }

    // Generic helpers to show typical bounds:
    // Note <? super T> for Comparator (consumer lower bound) and <? super T> for Comparable’s type parameter.
    static <T extends Comparable<? super T>> List<T> sortNatural(List<T> list) {
        List<T> copy = new ArrayList<>(list);
        Collections.sort(copy);
        return copy;
    }

    static <T> List<T> sortWith(List<T> list, Comparator<? super T> cmp) {
        List<T> copy = new ArrayList<>(list);
        copy.sort(cmp);
        return copy;
    }

    // Example domain type where natural order (Comparable) is by name (case-insensitive),
    // while equality is by id. This is intentionally inconsistent to demonstrate TreeSet/HashSet differences.
    static final class Person implements Comparable<Person> {
        final int id;
        final String name;
        final int age;
        final double height;
        final String city; // may be null

        Person(int id, String name, int age, double height, String city) {
            this.id = id;
            this.name = Objects.requireNonNull(name, "name");
            this.age = age;
            this.height = height;
            this.city = city;
        }

        @Override
        public int compareTo(Person other) {
            // Natural order: by name, case-insensitive (ties are considered equal in ordering)
            // Anti-pattern to avoid: return this.name.toLowerCase().compareTo(other.name.toLowerCase())
            // Use prebuilt String.CASE_INSENSITIVE_ORDER instead.
            return String.CASE_INSENSITIVE_ORDER.compare(this.name, other.name);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Person)) return false;
            Person person = (Person) o;
            return id == person.id; // equality by id only
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(id);
        }

        @Override
        public String toString() {
            return "Person{" +
                    "id=" + id +
                    ", name='" + name + '\'' +
                    ", age=" + age +
                    ", h=" + String.format(Locale.ROOT, "%.2f", height) +
                    ", city=" + city +
                    '}';
        }
    }

    // Example of a natural order consistent with equals: order by id, equals by id.
    static final class User implements Comparable<User> {
        final int id;
        final String username;

        User(int id, String username) {
            this.id = id;
            this.username = Objects.requireNonNull(username, "username");
        }

        @Override
        public int compareTo(User o) {
            // Correct numeric compare, avoids overflow
            return Integer.compare(this.id, o.id);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof User)) return false;
            User user = (User) o;
            return id == user.id;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(id);
        }

        @Override
        public String toString() {
            return "User{" + id + "," + username + "}";
        }
    }

    // Anti-pattern examples (do not use):
    @SuppressWarnings("unused")
    static int badCompareInt(int a, int b) {
        // Risk of overflow: e.g., a=Integer.MIN_VALUE, b=1 => overflow
        return a - b;
    }

    @SuppressWarnings("unused")
    static int badCompareDouble(double a, double b) {
        // Incorrect handling of NaN and -0.0 vs +0.0; use Double.compare(a, b) instead.
        return (a < b ? -1 : (a == b ? 0 : 1));
    }
}