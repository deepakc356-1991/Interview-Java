package _04_02_list_set_map_implementations;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * List, Set, Map Implementations — end‑to‑end examples with explanations.
 * All examples are self-contained and safe to run (exceptions are caught and printed).
 *
 * Notes:
 * - Examples avoid Java 9+ factory methods (List.of/Set.of/Map.of) for broad compatibility.
 * - Preferred modern alternatives are shown in comments where legacy types appear.
 */
public class _02_Examples {

    public static void main(String[] args) {
        section("LIST IMPLEMENTATIONS");
        demoArrayListVsLinkedList();
        demoVectorAndStackLegacy();
        demoListViewsAndMutability();
        demoSortingAndSearchingList();
        demoFailFastIteratorOnList();
        demoThreadSafeLists();

        section("SET IMPLEMENTATIONS");
        demoHashSetLinkedHashSetTreeSet();
        demoEnumSet();
        demoSetAlgebra();
        demoNavigableSet();

        section("MAP IMPLEMENTATIONS");
        demoHashMapLinkedHashMapTreeMap();
        demoEnumMap();
        demoMapViewsAndMutability();
        demoMapComputeMergeAndMultiMap();
        demoLRUWithLinkedHashMap();
        demoHashtableAndConcurrentHashMap();

        section("WRAPPERS AND DEFENSIVE COPIES");
        demoUnmodifiableAndSynchronizedWrappers();
    }

    // ===== LISTS =====

    private static void demoArrayListVsLinkedList() {
        subtitle("ArrayList vs LinkedList basics");
        // ArrayList: dynamic array, fast random access, good iteration cache locality.
        ArrayList<Integer> arrayList = new ArrayList<>();
        arrayList.ensureCapacity(10); // hint capacity to reduce resizes
        arrayList.add(10);
        arrayList.add(20);
        arrayList.add(1, 15); // insert shifts subsequent elements
        System.out.println("ArrayList: " + arrayList + " | get(1)=" + arrayList.get(1));
        arrayList.set(1, 16);
        arrayList.remove(Integer.valueOf(20)); // remove by value
        arrayList.remove(0); // remove by index
        System.out.println("ArrayList after removals: " + arrayList);

        Integer[] arr = arrayList.toArray(new Integer[0]); // typed array
        System.out.println("Array from ArrayList: " + Arrays.toString(arr));

        // LinkedList: doubly linked nodes, fast add/remove at ends, implements Deque.
        LinkedList<String> linkedList = new LinkedList<>();
        linkedList.add("A");
        linkedList.addFirst("0");
        linkedList.addLast("B");
        linkedList.offer("C");        // queue-style append
        linkedList.offerFirst("-1");  // queue-style prepend
        System.out.println("LinkedList (as List/Deque): " + linkedList);
        System.out.println("pollFirst=" + linkedList.pollFirst() + ", pollLast=" + linkedList.pollLast());
        System.out.println("LinkedList after polls: " + linkedList);
    }

    private static void demoVectorAndStackLegacy() {
        subtitle("Vector and Stack (legacy, synchronized) vs modern alternatives");
        // Vector: legacy synchronized list; prefer ArrayList or Collections.synchronizedList.
        Vector<String> vector = new Vector<>();
        vector.add("v1");
        vector.addElement("v2"); // legacy method
        System.out.println("Vector: " + vector + " (synchronized, legacy)");

        // Stack: legacy LIFO; prefer Deque (ArrayDeque or LinkedList)
        Stack<Integer> legacyStack = new Stack<>();
        legacyStack.push(1);
        legacyStack.push(2);
        System.out.println("Stack pop=" + legacyStack.pop() + ", peek=" + legacyStack.peek());

        Deque<Integer> modernStack = new ArrayDeque<>();
        modernStack.push(10);
        modernStack.push(20);
        System.out.println("Deque (stack) pop=" + modernStack.pop() + ", peek=" + modernStack.peek());
    }

    private static void demoListViewsAndMutability() {
        subtitle("List views, fixed-size lists, unmodifiable wrappers, subList pitfalls");
        // Arrays.asList returns a fixed-size list (backed by the array): set OK, add/remove not allowed.
        List<String> fixed = Arrays.asList("x", "y", "z");
        System.out.println("Fixed-size list (Arrays.asList): " + fixed);
        try {
            fixed.add("boom");
        } catch (UnsupportedOperationException e) {
            System.out.println("Caught UnsupportedOperationException: cannot add to fixed-size list");
        }
        fixed.set(1, "Y"); // allowed
        System.out.println("Fixed-size list after set: " + fixed);

        // Unmodifiable wrapper (read-only view) — still backed by the original list, reflects its changes.
        List<String> modifiable = new ArrayList<>(Arrays.asList("A", "B", "C", "D", "E"));
        List<String> unmodifiable = Collections.unmodifiableList(modifiable);
        System.out.println("Unmodifiable view: " + unmodifiable);
        try {
            unmodifiable.set(0, "Z");
        } catch (UnsupportedOperationException e) {
            System.out.println("Caught UnsupportedOperationException: cannot modify unmodifiableList");
        }
        modifiable.add("F"); // underlying modification is reflected
        System.out.println("Unmodifiable view reflects changes: " + unmodifiable);

        // subList is a view; structural changes in parent invalidate subList (fail-fast).
        List<String> base = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        List<String> sub = base.subList(1, 4); // ["b","c","d"]
        System.out.println("subList initial: " + sub);
        sub.set(0, "B"); // writes through to base
        System.out.println("After subList.set: base=" + base + ", sub=" + sub);
        base.add("f"); // structural change invalidates sub's modCount
        try {
            // Any subsequent structural access on 'sub' triggers ConcurrentModificationException
            sub.size();
        } catch (ConcurrentModificationException e) {
            System.out.println("Caught ConcurrentModificationException: subList invalidated by parent change");
        }
    }

    private static void demoSortingAndSearchingList() {
        subtitle("Sorting, comparators, binary search, de-duplication");
        List<Person> people = new ArrayList<>(Arrays.asList(
                new Person("Alice", 30),
                new Person("bob", 25),
                new Person("Charlie", 35),
                new Person("bob", 20)
        ));

        // Sort by name (case-insensitive), then by age
        Comparator<Person> byNameAge = Comparator.comparing(Person::getName, String.CASE_INSENSITIVE_ORDER)
                                                 .thenComparingInt(Person::getAge);
        people.sort(byNameAge);
        System.out.println("Sorted people (name,age): " + people);

        // Binary search requires same comparator and sorted order
        int idx = Collections.binarySearch(people, new Person("BOB", 25), byNameAge);
        System.out.println("Binary search for (BOB,25): index=" + idx + ", elem=" + (idx >= 0 ? people.get(idx) : null));

        // Sorting with nulls using nullsFirst/nullsLast
        List<String> names = new ArrayList<>(Arrays.asList("z", null, "A", "m", null, "b"));
        names.sort(Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
        System.out.println("Strings sorted (nulls last, case-insensitive): " + names);

        // De-duplicate a list preserving insertion order using LinkedHashSet
        List<String> withDupes = Arrays.asList("a", "b", "A", "b", "c", "a");
        List<String> dedup = new ArrayList<>(new LinkedHashSet<>(withDupes));
        System.out.println("De-duplicated (preserve order): " + dedup);
    }

    private static void demoFailFastIteratorOnList() {
        subtitle("Fail-fast iteration and correct removal");
        List<Integer> nums = new ArrayList<>(Arrays.asList(1, 2, 3, 4));
        try {
            for (Integer n : nums) {
                if (n % 2 == 0) {
                    nums.remove(n); // structural change during foreach -> ConcurrentModificationException
                }
            }
        } catch (ConcurrentModificationException e) {
            System.out.println("Caught ConcurrentModificationException during foreach removal");
        }

        // Correct removal via explicit Iterator.remove()
        nums = new ArrayList<>(Arrays.asList(1, 2, 3, 4));
        for (Iterator<Integer> it = nums.iterator(); it.hasNext();) {
            if (it.next() % 2 == 0) {
                it.remove();
            }
        }
        System.out.println("After iterator-based removal (evens removed): " + nums);
    }

    private static void demoThreadSafeLists() {
        subtitle("Thread-safe lists: synchronized wrappers and CopyOnWriteArrayList");
        // Synchronized wrapper
        List<String> syncList = Collections.synchronizedList(new ArrayList<>());
        syncList.add("a");
        syncList.add("b");
        // Iterate under the same lock
        synchronized (syncList) {
            for (String s : syncList) {
                System.out.print(s + " ");
            }
        }
        System.out.println("<= iterated syncList under lock");

        // CopyOnWriteArrayList: safe iteration while mutating (copies backing array on write)
        CopyOnWriteArrayList<String> cow = new CopyOnWriteArrayList<>(Arrays.asList("x", "y", "z"));
        for (String s : cow) {
            if ("y".equals(s)) cow.add("y2"); // allowed, iterator works on snapshot
            System.out.print(s + " ");
        }
        System.out.println("<= iterated snapshot; final cow=" + cow);
    }

    // ===== SETS =====

    private static void demoHashSetLinkedHashSetTreeSet() {
        subtitle("HashSet, LinkedHashSet, TreeSet differences");
        // HashSet: no order guarantee, allows a single null, unique by equals/hashCode
        Set<String> hashSet = new HashSet<>(Arrays.asList("b", "a", "a", "c", null));
        System.out.println("HashSet (unordered, allows null): " + hashSet);

        // LinkedHashSet: preserves insertion order
        Set<String> linkedHashSet = new LinkedHashSet<>(Arrays.asList("b", "a", "c", "a"));
        System.out.println("LinkedHashSet (insertion order): " + linkedHashSet);

        // TreeSet: sorted, no nulls, uses natural order or comparator
        Set<String> treeSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        treeSet.addAll(Arrays.asList("b", "A", "c", "a"));
        System.out.println("TreeSet (sorted, case-insensitive): " + treeSet + " (note: 'A' and 'a' considered equal)");

        try {
            Set<Integer> ts = new TreeSet<>();
            ts.add(null);
        } catch (NullPointerException e) {
            System.out.println("Caught NullPointerException: TreeSet does not allow null elements");
        }
    }

    private static void demoEnumSet() {
        subtitle("EnumSet (space/time efficient for enum types)");
        EnumSet<Day> weekend = EnumSet.of(Day.SAT, Day.SUN);
        EnumSet<Day> workdays = EnumSet.complementOf(weekend);
        EnumSet<Day> midweek = EnumSet.range(Day.TUE, Day.THU);
        System.out.println("Weekend: " + weekend);
        System.out.println("Workdays: " + workdays);
        System.out.println("Midweek: " + midweek);
    }

    private static void demoSetAlgebra() {
        subtitle("Set algebra: union, intersection, difference");
        Set<Integer> a = new LinkedHashSet<>(Arrays.asList(1, 2, 3, 4));
        Set<Integer> b = new LinkedHashSet<>(Arrays.asList(3, 4, 5));

        Set<Integer> union = new LinkedHashSet<>(a);
        union.addAll(b);

        Set<Integer> intersection = new LinkedHashSet<>(a);
        intersection.retainAll(b);

        Set<Integer> difference = new LinkedHashSet<>(a);
        difference.removeAll(b);

        System.out.println("A=" + a + ", B=" + b);
        System.out.println("Union: " + union);
        System.out.println("Intersection: " + intersection);
        System.out.println("Difference A\\B: " + difference);
    }

    private static void demoNavigableSet() {
        subtitle("NavigableSet/TreeSet navigation");
        NavigableSet<Integer> ns = new TreeSet<>(Arrays.asList(1, 3, 5, 7, 9));
        System.out.println("Set: " + ns);
        System.out.println("floor(6)=" + ns.floor(6) + ", ceiling(6)=" + ns.ceiling(6));
        System.out.println("lower(3)=" + ns.lower(3) + ", higher(3)=" + ns.higher(3));
        System.out.println("headSet(5, true)=" + ns.headSet(5, true));
        System.out.println("tailSet(5, false)=" + ns.tailSet(5, false));
        System.out.println("descendingSet=" + ns.descendingSet());
        System.out.println("pollFirst=" + ns.pollFirst() + ", pollLast=" + ns.pollLast() + ", remaining=" + ns);
    }

    // ===== MAPS =====

    private static void demoHashMapLinkedHashMapTreeMap() {
        subtitle("HashMap, LinkedHashMap, TreeMap differences");
        // HashMap: no order guarantee, allows 1 null key and many null values
        Map<String, Integer> hashMap = new HashMap<>();
        hashMap.put("a", 1);
        hashMap.put(null, 99);
        hashMap.put("b", null);
        hashMap.put("c", 3);
        System.out.println("HashMap (unordered, null key+values allowed): " + hashMap);

        // LinkedHashMap: preserves insertion order (or access order if enabled)
        LinkedHashMap<String, Integer> linkedHashMap = new LinkedHashMap<>();
        linkedHashMap.put("a", 1);
        linkedHashMap.put("b", 2);
        linkedHashMap.put("c", 3);
        System.out.println("LinkedHashMap (insertion order): " + linkedHashMap);

        // LinkedHashMap configured for access order (useful for LRU-like behavior)
        LinkedHashMap<String, Integer> accessOrdered = new LinkedHashMap<>(16, 0.75f, true);
        accessOrdered.put("x", 1);
        accessOrdered.put("y", 2);
        accessOrdered.put("z", 3);
        accessOrdered.get("x"); // access moves 'x' to end
        System.out.println("LinkedHashMap (access order after get 'x'): " + accessOrdered);

        // TreeMap: sorted by keys, no null keys, allows null values
        TreeMap<String, Integer> treeMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        treeMap.put("b", 2);
        treeMap.put("A", 1);
        treeMap.put("c", null); // null values are allowed
        System.out.println("TreeMap (sorted, case-insensitive): " + treeMap);
        try {
            treeMap.put(null, 5);
        } catch (NullPointerException e) {
            System.out.println("Caught NullPointerException: TreeMap does not allow null keys");
        }

        // NavigableMap operations
        System.out.println("floorKey('B')=" + treeMap.floorKey("B") + ", ceilingKey('B')=" + treeMap.ceilingKey("B"));
        System.out.println("subMap('A', true, 'c', false)=" + treeMap.subMap("A", true, "c", false));
    }

    private static void demoEnumMap() {
        subtitle("EnumMap (fast, compact map with enum keys)");
        EnumMap<Day, String> schedule = new EnumMap<>(Day.class);
        schedule.put(Day.MON, "Gym");
        schedule.put(Day.TUE, "Study");
        schedule.put(Day.FRI, "Movie");
        System.out.println("EnumMap: " + schedule + " (iteration ordered by enum ordinal)");
    }

    private static void demoMapViewsAndMutability() {
        subtitle("Map views: keySet, values, entrySet (live views)");
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        System.out.println("Map: " + map);

        // Remove via iterating keys view
        Iterator<String> it = map.keySet().iterator();
        while (it.hasNext()) {
            if ("b".equals(it.next())) {
                it.remove(); // removes entry "b" from the map
            }
        }
        System.out.println("After keySet iteration remove: " + map);

        // Values view (removing removes the first matching entry)
        map.values().remove(3);
        System.out.println("After values().remove(3): " + map);

        // entrySet allows in-place value updates
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            e.setValue(e.getValue() * 10);
        }
        System.out.println("After entrySet in-place updates: " + map);

        // Unmodifiable map view
        Map<String, Integer> unmod = Collections.unmodifiableMap(map);
        try {
            unmod.put("x", 42);
        } catch (UnsupportedOperationException e) {
            System.out.println("Caught UnsupportedOperationException: cannot modify unmodifiableMap");
        }
        map.put("d", 4); // reflected in the view
        System.out.println("Unmodifiable view reflects underlying changes: " + unmod);
    }

    private static void demoMapComputeMergeAndMultiMap() {
        subtitle("Map compute/merge, getOrDefault, putIfAbsent, MultiMap pattern");

        // getOrDefault and putIfAbsent
        Map<String, Integer> stock = new HashMap<>();
        stock.put("apple", 10);
        int bananas = stock.getOrDefault("banana", 0);
        stock.putIfAbsent("orange", 5);
        System.out.println("stock=" + stock + ", bananas(getOrDefault)= " + bananas);

        // computeIfAbsent: MultiMap (Map<K, List<V>>) pattern
        Map<String, List<Integer>> multi = new HashMap<>();
        addToMultiMap(multi, "grp1", 100);
        addToMultiMap(multi, "grp1", 200);
        addToMultiMap(multi, "grp2", 300);
        System.out.println("MultiMap via computeIfAbsent: " + multi);

        // merge: frequency count example
        List<String> words = Arrays.asList("to", "be", "or", "not", "to", "be");
        Map<String, Integer> freq = new HashMap<>();
        for (String w : words) {
            freq.merge(w, 1, Integer::sum);
        }
        System.out.println("Word frequencies via merge: " + freq);

        // computeIfPresent / compute
        freq.computeIfPresent("be", (k, v) -> v + 10);
        freq.compute("missing", (k, v) -> v == null ? 1 : v + 1);
        System.out.println("After computeIfPresent/compute: " + freq);

        // replaceAll and forEach
        freq.replaceAll((k, v) -> v * 2);
        System.out.print("replaceAll *2 then forEach: ");
        freq.forEach((k, v) -> System.out.print(k + "=" + v + " "));
        System.out.println();
    }

    private static void demoLRUWithLinkedHashMap() {
        subtitle("LRU cache with LinkedHashMap (accessOrder=true + removeEldestEntry)");
        LruCache<String, Integer> cache = new LruCache<>(3);
        cache.put("a", 1);
        cache.put("b", 2);
        cache.put("c", 3);
        cache.get("a"); // access 'a' moves it to most-recent
        cache.put("d", 4); // exceeds capacity -> evicts least-recent ('b')
        System.out.println("LRU cache content (most-recent last): " + cache);
    }

    private static void demoHashtableAndConcurrentHashMap() {
        subtitle("Hashtable (legacy) vs ConcurrentHashMap (modern concurrent)");
        // Hashtable: synchronized, no null keys/values, legacy
        Hashtable<String, Integer> ht = new Hashtable<>();
        ht.put("x", 1);
        System.out.println("Hashtable: " + ht);
        try {
            ht.put(null, 2);
        } catch (NullPointerException e) {
            System.out.println("Caught NullPointerException: Hashtable disallows null");
        }

        // ConcurrentHashMap: high-performance concurrent map; nulls not allowed
        ConcurrentHashMap<String, Integer> chm = new ConcurrentHashMap<>();
        chm.put("a", 1);
        chm.merge("a", 1, Integer::sum);    // atomic
        chm.computeIfAbsent("b", k -> 10);  // atomic
        System.out.println("ConcurrentHashMap: " + chm);
        try {
            chm.put(null, 3);
        } catch (NullPointerException e) {
            System.out.println("Caught NullPointerException: ConcurrentHashMap disallows null");
        }
    }

    // ===== WRAPPERS, SYNC, DEFENSIVE COPIES =====

    private static void demoUnmodifiableAndSynchronizedWrappers() {
        subtitle("Unmodifiable and synchronized wrappers; defensive copies");
        List<String> original = new ArrayList<>(Arrays.asList("A", "B", "C"));
        List<String> unmodifiable = Collections.unmodifiableList(original);
        List<String> synchronizedList = Collections.synchronizedList(new ArrayList<>(original));

        System.out.println("Unmodifiable: " + unmodifiable);
        System.out.println("Synchronized: " + synchronizedList);

        // Defensive copy for API returns: freeze current snapshot
        List<String> safeSnapshot = Collections.unmodifiableList(new ArrayList<>(original));
        original.add("MUTATION");
        System.out.println("Defensive copy unaffected: " + safeSnapshot + " | original now: " + original);

        // Iterating synchronized wrapper
        synchronized (synchronizedList) {
            for (String s : synchronizedList) {
                System.out.print(s + " ");
            }
        }
        System.out.println("<= iterated synchronizedList under lock");
    }

    // ===== Helpers and Types =====

    private static void addToMultiMap(Map<String, List<Integer>> multi, String key, int value) {
        multi.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }

    private static void subtitle(String title) {
        System.out.println();
        System.out.println("-- " + title + " --");
    }

    private static final class LruCache<K, V> extends LinkedHashMap<K, V> {
        private final int capacity;

        LruCache(int capacity) {
            super(16, 0.75f, true); // access-order
            this.capacity = capacity;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > capacity;
        }
    }

    private static final class Person {
        private final String name;
        private final int age;

        Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        String getName() { return name; }
        int getAge() { return age; }

        @Override
        public String toString() {
            return name + "(" + age + ")";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Person)) return false;
            Person person = (Person) o;
            return age == person.age && Objects.equals(name, person.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, age);
        }
    }

    private enum Day {
        MON, TUE, WED, THU, FRI, SAT, SUN
    }
}