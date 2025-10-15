package _04_05_immutable_and_synchronized_collections;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
Immutable & Synchronized Collections (Theory + Examples)

Topics covered:
1) Terminology
   - Mutable collection: its structure can change (add/remove/clear).
   - Immutable collection: its structure cannot change after creation.
   - Unmodifiable view: a read-only wrapper around a collection; the wrapper is not modifiable,
     but the underlying collection can still change and those changes are reflected in the view.
   - Shallow immutability: the collection’s structure is immutable, but its elements can still be mutable.
   - Defensive copy: creating a separate copy to avoid exposing internal mutable state.

2) Ways to get read-only/immutable collections (JDK):
   - Unmodifiable views (modifiable backing!):
       Collections.unmodifiableList/Set/Map/SortedSet/NavigableSet/… (JDK 1.2+)
   - Truly immutable factories (new, compact, disallow nulls, do not reflect external changes):
       List.of / Set.of / Map.of / Map.ofEntries (JDK 9+)
   - Snapshot immutables from existing sources:
       List.copyOf / Set.copyOf / Map.copyOf (JDK 10+)
   - Singleton and empty helpers:
       Collections.singleton, singletonList, singletonMap, emptyList/Set/Map
   - Streams (JDK 10+):
       Collectors.toUnmodifiableList/Set/Map

3) Important differences:
   - Unmodifiable view vs immutable:
       Unmodifiable view reflects changes to the backing collection; immutable factories do not.
   - Nulls:
       Unmodifiable wrappers allow nulls if the backing collection does.
       List.of/Set.of/Map.of do not allow nulls (NullPointerException).
   - Duplicates:
       Set.of and Map.of reject duplicate elements/keys (IllegalArgumentException).
   - Arrays.asList:
       Fixed-size view backed by array; set() allowed, add/remove not allowed.
   - subList:
       A view into the original list; certain structural changes to the parent may invalidate the subList.

4) Concurrency and immutability:
   - Immutable collections are inherently thread-safe for reads (no synchronization required).
   - If you replace an immutable collection reference, publish the new reference safely (e.g., volatile).

5) Synchronized collections (coarse-grained locking):
   - Collections.synchronizedXxx provides thread-safe wrappers by synchronizing each method.
   - When iterating them, you must manually synchronize on the wrapper:
       synchronized (syncList) { for (E e : syncList) { ... } }
   - Compound actions (check-then-act) require external synchronization around the entire operation.

6) Concurrent collections (scalable concurrency):
   - Prefer java.util.concurrent collections for concurrency:
       ConcurrentHashMap, CopyOnWriteArrayList, ConcurrentSkipListMap/Set, BlockingQueue, etc.
   - They offer better scalability, atomic operations (e.g., computeIfAbsent), and iterators that are
     weakly consistent (no ConcurrentModificationException) rather than fail-fast.

7) API design best practices:
   - Keep state private and mutable internally for efficiency, but publish as immutable:
       return Collections.unmodifiableList(new ArrayList<>(internal));
   - Accept collections defensively: store a copy, not the caller’s reference.
   - Prefer immutable element types when possible to achieve deeper immutability.

8) Navigable/Sorted variants:
   - Unmodifiable and synchronized wrappers exist for SortedSet/Map and NavigableSet/Map.

Below are runnable examples demonstrating these concepts.
*/
public final class _01_Theory {

    // Example of safe publication with immutable snapshot (JDK 10+ List.copyOf)
    private static volatile List<String> featureFlags = List.of("A", "B");

    public static void main(String[] args) {
        demoUnmodifiableViewVsImmutableFactories();
        demoNullsDuplicatesAndSingletons();
        demoArraysAsListAndSubListCaveats();
        demoElementMutabilityInsideImmutableCollections();
        demoDefensiveCopiesAndAPIAdvice();
        demoSynchronizedWrappersAndIteration();
        demoCompoundActionsWithSynchronizedWrappers();
        demoConcurrentCollectionsBasics();
        demoCopyOnWriteArrayList();
        demoCollectorsToUnmodifiable();
        demoSortedAndNavigableWrappers();
        demoVolatileSnapshotPattern();
    }

    // 1) Unmodifiable view vs truly immutable factories
    private static void demoUnmodifiableViewVsImmutableFactories() {
        System.out.println("\n--- Unmodifiable view vs Immutable factories ---");

        List<String> modifiable = new ArrayList<>(Arrays.asList("A", "B"));
        List<String> unmodifiableView = Collections.unmodifiableList(modifiable); // view of modifiable

        try {
            unmodifiableView.add("X"); // Unsupported
        } catch (UnsupportedOperationException ex) {
            System.out.println("unmodifiableView.add -> UnsupportedOperationException");
        }

        modifiable.add("C"); // affects the view
        System.out.println("Backer changed; unmodifiable view sees: " + unmodifiableView);

        List<String> immutable = List.of("X", "Y"); // truly immutable (JDK 9+)
        try {
            immutable.add("Z");
        } catch (UnsupportedOperationException ex) {
            System.out.println("immutable.add -> UnsupportedOperationException");
        }
        modifiable.add("D"); // does NOT affect 'immutable'
        System.out.println("immutable remains: " + immutable);

        List<String> snapshot = List.copyOf(modifiable); // JDK 10+: immutable snapshot
        modifiable.add("E");
        System.out.println("snapshot remains: " + snapshot + ", modifiable now: " + modifiable);
    }

    // 2) Null handling and duplicates with factories; singleton helpers
    private static void demoNullsDuplicatesAndSingletons() {
        System.out.println("\n--- Nulls and Duplicates in factories; Singletons ---");

        try {
            List.of("a", null); // disallowed
        } catch (NullPointerException ex) {
            System.out.println("List.of with null -> NullPointerException");
        }

        List<String> withNulls = Collections.unmodifiableList(new ArrayList<>(Arrays.asList("a", null)));
        System.out.println("Unmodifiable view can contain nulls if backer does: " + withNulls);

        try {
            Set.of("k", "k"); // duplicates not allowed
        } catch (IllegalArgumentException ex) {
            System.out.println("Set.of duplicates -> IllegalArgumentException");
        }

        try {
            Map.of("k1", "v1", "k1", "v2"); // duplicate key not allowed
        } catch (IllegalArgumentException ex) {
            System.out.println("Map.of duplicate key -> IllegalArgumentException");
        }

        List<String> single = Collections.singletonList("only");
        System.out.println("Singleton list: " + single);
        try {
            single.add("nope");
        } catch (UnsupportedOperationException ex) {
            System.out.println("singleton.add -> UnsupportedOperationException");
        }

        List<String> singletonNull = Collections.singletonList(null); // allowed
        System.out.println("Singleton list with null allowed: " + singletonNull);
    }

    // 3) Arrays.asList and subList caveats
    private static void demoArraysAsListAndSubListCaveats() {
        System.out.println("\n--- Arrays.asList and subList caveats ---");

        List<Integer> fixed = Arrays.asList(1, 2, 3); // fixed-size view backed by array
        fixed.set(1, 42); // allowed
        System.out.println("Arrays.asList fixed-size list after set: " + fixed);
        try {
            fixed.add(99); // not allowed
        } catch (UnsupportedOperationException ex) {
            System.out.println("Arrays.asList add -> UnsupportedOperationException");
        }

        List<Integer> base = new ArrayList<>(List.of(1, 2, 3, 4, 5));
        List<Integer> sub = base.subList(1, 4); // view: [2, 3, 4]
        List<Integer> unmodSub = Collections.unmodifiableList(sub);
        System.out.println("unmodifiable subList initially: " + unmodSub);

        base.set(2, 99); // affects subList view
        System.out.println("After parent set, subList view: " + unmodSub);

        // Structural modifications to parent outside subList may invalidate subList (fail-fast on next use).
        // Example (commented to avoid runtime exception):
        // base.add(6); // may cause future subList operations to throw ConcurrentModificationException
    }

    // 4) Shallow immutability: element state can still change
    private static void demoElementMutabilityInsideImmutableCollections() {
        System.out.println("\n--- Shallow immutability (elements may be mutable) ---");

        List<Person> people = List.of(new Person("Ann", 30));
        System.out.println("Before element mutation: " + people);
        people.get(0).setAge(31); // allowed; the list structure is immutable, but element is mutable
        System.out.println("After element mutation (list unchanged, element changed): " + people);

        List<PersonImmutable> immPeople = List.of(new PersonImmutable("Bob", 40));
        // immPeople.get(0).age = 41; // not possible; fields are final and no setters provided
        System.out.println("Use immutable elements to achieve deeper immutability: " + immPeople);
    }

    // 5) Defensive copies (API design)
    private static void demoDefensiveCopiesAndAPIAdvice() {
        System.out.println("\n--- Defensive copy pattern (API design best practice) ---");

        Library lib = new Library();
        lib.addTag("core");
        lib.addTag("public");

        // Bad: exposes internal list (here we provide only safe API)
        // Good: return an unmodifiable copy or view of a private copy
        System.out.println("Safe tags: " + lib.getTags());
        try {
            lib.getTags().add("hack");
        } catch (UnsupportedOperationException ex) {
            System.out.println("Attempt to mutate returned tags -> UnsupportedOperationException");
        }
    }

    // 6) Synchronized wrappers and iteration rule
    private static void demoSynchronizedWrappersAndIteration() {
        System.out.println("\n--- Synchronized wrappers and iteration ---");

        List<Integer> syncList = Collections.synchronizedList(new ArrayList<>());
        syncList.add(1);
        syncList.add(2);

        // Iteration must be done inside synchronized block on the wrapper
        synchronized (syncList) {
            for (Integer i : syncList) {
                System.out.print("Iterating (sync): " + i + " ");
            }
            System.out.println();
        }

        Map<String, Integer> syncMap = Collections.synchronizedMap(new HashMap<>());
        syncMap.put("a", 1);
        syncMap.put("b", 2);
        synchronized (syncMap) {
            for (Map.Entry<String, Integer> e : syncMap.entrySet()) {
                System.out.print("Map entry (sync): " + e + " ");
            }
            System.out.println();
        }
    }

    // 7) Compound actions must be synchronized atomically with synchronized wrappers
    private static void demoCompoundActionsWithSynchronizedWrappers() {
        System.out.println("\n--- Compound actions with synchronized wrappers ---");

        List<String> syncList = Collections.synchronizedList(new ArrayList<>());
        addIfAbsent(syncList, "A");
        addIfAbsent(syncList, "A"); // no duplicate
        synchronized (syncList) {
            System.out.println("syncList after addIfAbsent twice: " + syncList);
        }
    }

    // 8) Concurrent collections basics (weakly consistent iterators, atomic ops)
    private static void demoConcurrentCollectionsBasics() {
        System.out.println("\n--- Concurrent collections (ConcurrentHashMap) ---");

        ConcurrentHashMap<String, Integer> chm = new ConcurrentHashMap<>();
        chm.put("a", 1);
        chm.computeIfAbsent("b", k -> 2); // atomic

        // Iteration is weakly consistent (no ConcurrentModificationException)
        for (String k : chm.keySet()) {
            if (!chm.containsKey("x")) {
                chm.put("x", 99); // allowed during iteration
            }
        }
        System.out.println("ConcurrentHashMap after in-loop put: " + chm);

        try {
            chm.put(null, 1); // nulls not allowed
        } catch (NullPointerException ex) {
            System.out.println("ConcurrentHashMap null key -> NullPointerException");
        }
    }

    // 9) CopyOnWriteArrayList for read-mostly scenarios
    private static void demoCopyOnWriteArrayList() {
        System.out.println("\n--- CopyOnWriteArrayList (read-mostly) ---");

        CopyOnWriteArrayList<String> cow = new CopyOnWriteArrayList<>(List.of("A", "B"));
        for (String s : cow) {
            System.out.print("Iterating COW: " + s + " ");
            // Modifying during iteration does not throw CME (iterator iterates over a snapshot)
            cow.add("Z");
            break; // avoid unbounded growth in this demo
        }
        System.out.println("\nCOW after add during iteration: " + cow);
    }

    // 10) Collectors.toUnmodifiableX (JDK 10+)
    private static void demoCollectorsToUnmodifiable() {
        System.out.println("\n--- Collectors.toUnmodifiableX (JDK 10+) ---");

        List<String> list = Stream.of("a", "b", "c").collect(Collectors.toUnmodifiableList());
        System.out.println("toUnmodifiableList: " + list);
        try {
            list.add("x");
        } catch (UnsupportedOperationException ex) {
            System.out.println("toUnmodifiableList add -> UnsupportedOperationException");
        }

        Set<String> set = Stream.of("k1", "k2").collect(Collectors.toUnmodifiableSet());
        System.out.println("toUnmodifiableSet: " + set);

        Map<String, Integer> map = Stream.of("p", "q")
                .collect(Collectors.toUnmodifiableMap(k -> k, String::length));
        System.out.println("toUnmodifiableMap: " + map);
    }

    // 11) Navigable/Sorted wrappers exist too
    private static void demoSortedAndNavigableWrappers() {
        System.out.println("\n--- Sorted/Navigable wrappers ---");

        NavigableSet<Integer> tree = new TreeSet<>(List.of(1, 3, 5));
        NavigableSet<Integer> unmodNav = Collections.unmodifiableNavigableSet(tree);
        System.out.println("Unmodifiable NavigableSet: " + unmodNav);
        tree.add(7); // reflected in the view (backed by same set)
        System.out.println("After backer add, view: " + unmodNav);

        SortedMap<String, Integer> sortedMap = new TreeMap<>();
        sortedMap.put("a", 1);
        SortedMap<String, Integer> syncSortedMap = Collections.synchronizedSortedMap(sortedMap);
        synchronized (syncSortedMap) {
            System.out.println("Synchronized SortedMap snapshot: " + syncSortedMap);
        }
    }

    // 12) Volatile + immutable snapshot pattern for safe publication
    private static void demoVolatileSnapshotPattern() {
        System.out.println("\n--- Volatile + immutable snapshot pattern ---");

        System.out.println("Initial flags (volatile, immutable): " + featureFlags);
        replaceFeatureFlags(Arrays.asList("A", "C", "D")); // new snapshot
        System.out.println("Updated flags: " + featureFlags);

        // Readers can access 'featureFlags' without synchronization; writers replace the entire immutable reference.
    }

    // Helper: compound action with synchronized wrappers
    private static <E> boolean addIfAbsent(List<E> syncList, E e) {
        synchronized (syncList) {
            if (!syncList.contains(e)) {
                return syncList.add(e);
            }
            return false;
        }
    }

    // Helper: safe replacement of immutable snapshot
    private static void replaceFeatureFlags(Collection<String> newFlags) {
        featureFlags = List.copyOf(newFlags); // immutable snapshot (JDK 10+)
    }

    // Example mutable element (to demonstrate shallow immutability)
    private static final class Person {
        private final String name;
        private int age;

        Person(String name, int age) {
            this.name = Objects.requireNonNull(name);
            this.age = age;
        }

        void setAge(int age) {
            this.age = age;
        }

        @Override public String toString() {
            return "Person{" + name + ", age=" + age + '}';
        }
    }

    // Example immutable element
    private static final class PersonImmutable {
        private final String name;
        private final int age;

        PersonImmutable(String name, int age) {
            this.name = Objects.requireNonNull(name);
            this.age = age;
        }

        @Override public String toString() {
            return "PersonImmutable{" + name + ", age=" + age + '}';
        }
    }

    // Example of defensive copy in API
    private static final class Library {
        private final List<String> tags = new ArrayList<>();

        void addTag(String tag) {
            tags.add(Objects.requireNonNull(tag));
        }

        // Good: return an unmodifiable copy of a private copy
        List<String> getTags() {
            return Collections.unmodifiableList(new ArrayList<>(tags));
        }
    }
}