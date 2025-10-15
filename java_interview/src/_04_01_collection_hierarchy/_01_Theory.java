package _04_01_collection_hierarchy;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * Java Collections Framework (JCF) – Collection Hierarchy Theory + Practical Cheatsheet
 *
 * How to use:
 * - Read comments for theory.
 * - Run main(...) to see short demos of key behaviors.
 *
 * Hierarchy (interfaces):
 * - Iterable<T>  -> root of enhanced-for ("for-each")
 *   - Collection<T>
 *     - List<T>         (ordered, indexed; duplicates allowed)   e.g., ArrayList, LinkedList, CopyOnWriteArrayList
 *     - Set<T>          (no duplicates)                         e.g., HashSet, LinkedHashSet, TreeSet, EnumSet
 *       - SortedSet<T>  (sorted by natural order or Comparator)
 *         - NavigableSet<T> (floor/ceiling, lower/higher, pollFirst/Last)
 *     - Queue<T>        (FIFO)                                   e.g., PriorityQueue, ArrayDeque
 *       - Deque<T>      (double-ended queue)                     e.g., ArrayDeque, LinkedList
 *
 * - Map<K,V>       (not a Collection; key->value associations)
 *   - SortedMap<K,V>
 *     - NavigableMap<K,V> (floorKey/ceilingKey, subMap ranges)
 *   - ConcurrentMap<K,V> (atomic ops)
 *   - Special maps: HashMap, LinkedHashMap, TreeMap, EnumMap, WeakHashMap, IdentityHashMap, ConcurrentHashMap
 *
 * Abstract helpers:
 * - AbstractCollection, AbstractList, AbstractSequentialList, AbstractSet, AbstractQueue, AbstractMap
 *
 * Marker/auxiliary interfaces:
 * - RandomAccess (for O(1) index access; e.g., ArrayList)
 * - Cloneable, Serializable (many impls are serializable)
 *
 * Big-O characteristics (typical):
 * - ArrayList: get/set O(1), add at end amortized O(1), insert/remove middle O(n)
 * - LinkedList: add/remove at ends O(1), get(index) O(n)
 * - HashSet/HashMap: add/get/remove O(1) average; O(n) worst with bad hashing
 * - LinkedHashMap/LinkedHashSet: as Hash*, but predictable iteration order (insertion or access)
 * - TreeSet/TreeMap: add/get/remove O(log n); sorted
 * - PriorityQueue: add/poll O(log n); peek O(1); not globally sorted
 * - ArrayDeque: add/remove at ends amortized O(1)
 *
 * Ordering:
 * - Hash* no order; LinkedHash* insertion (or access) order; Tree* sorted by natural order or Comparator; Enum* natural enum order.
 *
 * Equality vs ordering:
 * - Hash-based collections use equals/hashCode.
 * - Tree/Sorted use compareTo/Comparator; Comparator should be consistent with equals.
 * - IdentityHashMap uses == (identity) and System.identityHashCode.
 *
 * Mutability:
 * - Unmodifiable views via Collections.unmodifiableXxx(...) (backed views; reflect changes to backing).
 * - Fixed-size views via Arrays.asList(...) (backed by array).
 * - Java 9+: List.of/Set.of/Map.of create truly unmodifiable collections (no nulls). (Shown in comments only for Java 8 compatibility.)
 *
 * Views (backed):
 * - List.subList, Map.keySet/values/entrySet, Sorted/Navigable range views: changes reflect in backing.
 *
 * Iteration:
 * - Iterators are fail-fast (throw ConcurrentModificationException on structural concurrent modification).
 * - Concurrent collections have weakly-consistent iterators (no fail-fast; may or may not see concurrent changes).
 *
 * Concurrency:
 * - Prefer java.util.concurrent collections to manual synchronization.
 * - Collections.synchronizedXxx wraps for coarse-grained synchronization; synchronize during iteration.
 * - CopyOnWriteArrayList/Set: iterations are snapshot; good for many reads, few writes.
 * - BlockingQueue, BlockingDeque for producer/consumer.
 *
 * Nulls policy (selected):
 * - HashMap/HashSet: allow one null key / null elements.
 * - LinkedHashMap: allows null key/values.
 * - TreeMap/TreeSet: generally disallow null with natural ordering; comparator may allow but discouraged.
 * - ConcurrentHashMap: disallows null keys/values.
 * - EnumMap/EnumSet: disallow nulls.
 * - List.of/Set.of/Map.of (Java 9+): disallow nulls.
 *
 * Utilities:
 * - Collections: sort/shuffle/reverse/frequency/unmodifiable/checked/synchronized
 * - Arrays: sort/binarySearch/asList
 *
 * Legacy (avoid in new code):
 * - Vector, Stack, Hashtable, Dictionary, Enumeration
 */
public class _01_Theory {

    // For EnumSet/EnumMap demo
    enum Day { MON, TUE, WED, THU, FRI, SAT, SUN }

    // For comparator/ordering demos
    static final class Person implements Comparable<Person> {
        final String name;
        final int age;
        Person(String name, int age) { this.name = name; this.age = age; }
        @Override public int compareTo(Person o) { return this.name.compareTo(o.name); } // natural: by name
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Person)) return false;
            Person p = (Person) o;
            return age == p.age && Objects.equals(name, p.name);
        }
        @Override public int hashCode() { return Objects.hash(name, age); }
        @Override public String toString() { return name + "(" + age + ")"; }
    }

    // Simple LRU cache using LinkedHashMap access-order
    static class LRUCache<K,V> extends LinkedHashMap<K,V> {
        private final int maxEntries;
        LRUCache(int maxEntries) {
            super(16, 0.75f, true); // access-order
            this.maxEntries = maxEntries;
        }
        @Override protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
            return size() > maxEntries;
        }
    }

    public static void main(String[] args) {
        System.out.println("=== Java Collections Framework (JCF) – Collection Hierarchy Demos ===");
        demoLists();
        demoSets();
        demoMaps();
        demoQueuesAndDeques();
        demoAlgorithms();
        demoComparators();
        demoViewsAndBackedCollections();
        demoImmutabilityAndWrappers();
        demoFailFastVsConcurrent();
        demoEnumCollections();
        demoIdentityAndWeakMaps();
        demoConcurrentCollections();
        demoSpliteratorsAndStreams();
        System.out.println("=== Done ===");
    }

    private static void demoLists() {
        System.out.println("\n-- List demos --");
        // ArrayList: random-access friendly
        List<Integer> arrayList = new ArrayList<>();
        arrayList.add(1); arrayList.add(2); arrayList.add(3);
        System.out.println("ArrayList: " + arrayList + " (RandomAccess=" + (arrayList instanceof RandomAccess) + ")");

        // LinkedList: good at head/tail operations; also implements Deque
        List<Integer> linkedList = new LinkedList<>(Arrays.asList(10, 20, 30));
        System.out.println("LinkedList: " + linkedList + " (RandomAccess=" + (linkedList instanceof RandomAccess) + ")");

        // subList is a backed view (changes affect both)
        List<Integer> sub = arrayList.subList(1, arrayList.size());
        sub.set(0, 99); // modifies arrayList as well
        System.out.println("subList view changed => arrayList: " + arrayList);

        // Fixed-size view via Arrays.asList (cannot add/remove, but can set)
        List<String> fixed = Arrays.asList("a", "b", "c");
        try { fixed.add("d"); } catch (UnsupportedOperationException e) {
            System.out.println("Arrays.asList is fixed-size: add throws " + e.getClass().getSimpleName());
        }

        // ListIterator can add/remove during iteration
        List<String> names = new ArrayList<>(Arrays.asList("Ann", "Bob", "Cal"));
        ListIterator<String> it = names.listIterator();
        while (it.hasNext()) {
            String n = it.next();
            if (n.startsWith("B")) it.add("Bea");
        }
        System.out.println("ListIterator add during iteration: " + names);

        // Note: Java 9+ factory: List.of(1,2,3) => unmodifiable (no nulls). [Not used here to keep Java 8 compatibility]
    }

    private static void demoSets() {
        System.out.println("\n-- Set demos --");
        Set<Integer> hash = new HashSet<>(Arrays.asList(5, 1, 3, 3, 2));
        System.out.println("HashSet (no order, no duplicates): " + hash);

        Set<Integer> linkedHash = new LinkedHashSet<>(Arrays.asList(5, 1, 3, 3, 2));
        System.out.println("LinkedHashSet (insertion order): " + linkedHash);

        Set<Integer> tree = new TreeSet<>(Arrays.asList(5, 1, 3, 3, 2));
        System.out.println("TreeSet (sorted): " + tree);

        NavigableSet<Integer> nav = new TreeSet<>(Arrays.asList(1,2,3,4,5));
        System.out.println("NavigableSet lower(3)=" + nav.lower(3) + ", floor(3)=" + nav.floor(3) +
                ", ceiling(3)=" + nav.ceiling(3) + ", higher(3)=" + nav.higher(3));

        // Set algebra
        Set<Integer> A = new HashSet<>(Arrays.asList(1,2,3));
        Set<Integer> B = new HashSet<>(Arrays.asList(3,4,5));
        Set<Integer> union = new HashSet<>(A); union.addAll(B);
        Set<Integer> inter = new HashSet<>(A); inter.retainAll(B);
        Set<Integer> diff = new HashSet<>(A); diff.removeAll(B);
        System.out.println("A∪B=" + union + ", A∩B=" + inter + ", A\\B=" + diff);

        // Null policy example (HashSet allows null; TreeSet typically doesn't with natural ordering)
        Set<String> hs = new HashSet<>();
        hs.add(null); hs.add("x");
        System.out.println("HashSet with null: " + hs);
    }

    private static void demoMaps() {
        System.out.println("\n-- Map demos --");
        Map<String,Integer> hm = new HashMap<>();
        hm.put("c", 3); hm.put("a", 1); hm.put("b", 2);
        System.out.println("HashMap (no order): " + hm + ", getOrDefault('x',-1)=" + hm.getOrDefault("x",-1));

        Map<String,Integer> lhm = new LinkedHashMap<>();
        lhm.put("c", 3); lhm.put("a", 1); lhm.put("b", 2);
        System.out.println("LinkedHashMap (insertion order): " + lhm);
        lhm.get("a"); // access
        System.out.println("LinkedHashMap access has no effect unless constructed with accessOrder=true");

        // Access-order LinkedHashMap for LRU
        LRUCache<Integer,String> lru = new LRUCache<>(3);
        lru.put(1,"one"); lru.put(2,"two"); lru.put(3,"three");
        lru.get(1); // access 1 to make it recently used
        lru.put(4,"four"); // evicts least-recently-used (key 2)
        System.out.println("LRU via LinkedHashMap (max=3): " + lru.keySet());

        // TreeMap sorted by custom comparator
        NavigableMap<String,Integer> tm = new TreeMap<>(Comparator.reverseOrder());
        tm.put("c",3); tm.put("a",1); tm.put("b",2);
        System.out.println("TreeMap (reverse order): " + tm + ", floorKey('b')=" + tm.floorKey("b"));

        // Map views are backed: keySet/values/entrySet
        Set<String> keys = hm.keySet();
        keys.remove("a");
        System.out.println("keySet view removes from map: " + hm);

        // Note: Java 9+ Map.of("k","v",...) produces unmodifiable maps (no nulls). [comment only]
    }

    private static void demoQueuesAndDeques() {
        System.out.println("\n-- Queue/Deque demos --");
        Deque<Integer> dq = new ArrayDeque<>();
        dq.addLast(1); dq.addLast(2); dq.addFirst(0);
        System.out.println("ArrayDeque as Deque: " + dq + ", pop=" + dq.pop() + ", after pop: " + dq);

        Queue<Integer> pq = new PriorityQueue<>(Arrays.asList(5,1,3,2,4));
        System.out.print("PriorityQueue poll order: ");
        while (!pq.isEmpty()) System.out.print(pq.poll() + " ");
        System.out.println(" (min-heap)");

        Queue<Integer> maxPQ = new PriorityQueue<>(Comparator.reverseOrder());
        maxPQ.addAll(Arrays.asList(5,1,3,2,4));
        System.out.print("PriorityQueue (reverse comparator) poll order: ");
        while (!maxPQ.isEmpty()) System.out.print(maxPQ.poll() + " ");
        System.out.println();
    }

    private static void demoAlgorithms() {
        System.out.println("\n-- Algorithms (Collections/Arrays) --");
        List<Integer> nums = new ArrayList<>(Arrays.asList(5,1,3,2,4));
        Collections.sort(nums);
        System.out.println("sort: " + nums + ", binarySearch(3) index=" + Collections.binarySearch(nums, 3));
        Collections.reverse(nums);
        System.out.println("reverse: " + nums);
        Collections.shuffle(nums, new Random(42));
        System.out.println("shuffle (seeded): " + nums);

        // Frequency, min/max
        System.out.println("min=" + Collections.min(nums) + ", max=" + Collections.max(nums) +
                ", freq of 3=" + Collections.frequency(nums, 3));
    }

    private static void demoComparators() {
        System.out.println("\n-- Comparator and ordering --");
        List<Person> people = Arrays.asList(
                new Person("Bob", 25),
                new Person("Ann", 30),
                new Person("Cal", 20)
        );
        List<Person> byNameNatural = new ArrayList<>(people);
        Collections.sort(byNameNatural); // uses Comparable (name)
        System.out.println("People sorted by natural (name): " + byNameNatural);

        List<Person> byAgeThenName = new ArrayList<>(people);
        byAgeThenName.sort(Comparator.comparingInt((Person p) -> p.age).thenComparing(p -> p.name));
        System.out.println("People sorted by age then name: " + byAgeThenName);

        // Null-friendly comparator
        List<String> words = new ArrayList<>(Arrays.asList("b","a",null,"c"));
        words.sort(Comparator.nullsFirst(Comparator.naturalOrder()));
        System.out.println("Nulls-first sort: " + words);
    }

    private static void demoViewsAndBackedCollections() {
        System.out.println("\n-- Backed views (List.subList, Map views, range views) --");
        List<Integer> base = new ArrayList<>(Arrays.asList(0,1,2,3,4));
        List<Integer> view = base.subList(1, 4); // [1,2,3]
        view.set(0, 99);
        System.out.println("subList view changed => base: " + base);

        NavigableSet<Integer> set = new TreeSet<>(Arrays.asList(1,2,3,4,5,6));
        NavigableSet<Integer> range = set.subSet(2, true, 5, false); // [2,3,4]
        range.remove(3);
        System.out.println("Range view remove reflects in set: range=" + range + ", set=" + set);

        Map<String,Integer> map = new LinkedHashMap<>();
        map.put("x",1); map.put("y",2);
        Collection<Integer> values = map.values();
        values.remove(1);
        System.out.println("Map.values view removal => map: " + map);
    }

    private static void demoImmutabilityAndWrappers() {
        System.out.println("\n-- Unmodifiable / Synchronized / Checked wrappers --");
        List<String> mod = new ArrayList<>(Arrays.asList("a","b"));
        List<String> unmod = Collections.unmodifiableList(mod);
        try { unmod.add("c"); } catch (UnsupportedOperationException e) {
            System.out.println("unmodifiableList: add throws " + e.getClass().getSimpleName());
        }
        mod.add("c"); // backing list changed; view reflects
        System.out.println("Backed unmodifiable view reflects backing changes: " + unmod);

        List<String> sync = Collections.synchronizedList(new ArrayList<>());
        // Must manually synchronize during iteration:
        synchronized (sync) {
            sync.add("s1"); sync.add("s2");
            for (String s : sync) { /* safe iteration */ }
        }
        System.out.println("synchronizedList created (remember to sync when iterating).");

        // Checked wrapper enforces runtime type safety for raw-typed misuse
        List<Number> checked = Collections.checkedList(new ArrayList<>(), Number.class);
        checked.add(1); // OK
        try {
            @SuppressWarnings({ "rawtypes", "unchecked" })
            List raw = checked; // simulate legacy raw usage
            raw.add("oops");     // runtime type check
        } catch (ClassCastException cce) {
            System.out.println("checkedList prevented wrong runtime type: " + cce.getClass().getSimpleName());
        }

        // Note: Java 9+ List.of/Set.of/Map.of create truly unmodifiable collections (and reject nulls). [comment only]
    }

    private static void demoFailFastVsConcurrent() {
        System.out.println("\n-- Iteration: fail-fast vs concurrent --");
        List<Integer> ff = new ArrayList<>(Arrays.asList(1,2,3,4));
        try {
            for (Integer v : ff) {
                if (v % 2 == 0) ff.remove(v); // structural concurrent modification
            }
        } catch (ConcurrentModificationException cme) {
            System.out.println("Fail-fast iterator detected concurrent modification: " + cme.getClass().getSimpleName());
        }
        // Correct removal during iteration
        ff = new ArrayList<>(Arrays.asList(1,2,3,4));
        Iterator<Integer> it = ff.iterator();
        while (it.hasNext()) {
            if (it.next() % 2 == 0) it.remove();
        }
        System.out.println("Proper removal via Iterator.remove: " + ff);

        // CopyOnWriteArrayList: iteration sees snapshot; modifications are safe but costly for writes
        CopyOnWriteArrayList<Integer> cow = new CopyOnWriteArrayList<>(Arrays.asList(1,2,3));
        for (Integer v : cow) {
            if (v == 1) cow.add(4); // safe; this new element won't appear in this iteration
        }
        System.out.println("CopyOnWrite iteration snapshot; final list: " + cow);

        // ConcurrentHashMap: weakly-consistent iterators (no fail-fast)
        ConcurrentHashMap<String,Integer> chm = new ConcurrentHashMap<>();
        chm.put("a",1); chm.put("b",2);
        for (String k : chm.keySet()) {
            chm.put("c",3); // allowed during iteration; iterator is weakly consistent
        }
        System.out.println("ConcurrentHashMap updated during iteration: " + chm);
    }

    private static void demoEnumCollections() {
        System.out.println("\n-- EnumSet / EnumMap --");
        EnumSet<Day> weekend = EnumSet.of(Day.SAT, Day.SUN);
        EnumSet<Day> weekdays = EnumSet.range(Day.MON, Day.FRI);
        System.out.println("EnumSet weekend: " + weekend + ", weekdays: " + weekdays);

        EnumMap<Day, String> schedule = new EnumMap<>(Day.class);
        schedule.put(Day.MON, "Gym");
        schedule.put(Day.FRI, "Cinema");
        System.out.println("EnumMap: " + schedule);
    }

    private static void demoIdentityAndWeakMaps() {
        System.out.println("\n-- IdentityHashMap vs HashMap; WeakHashMap --");
        String k1 = new String("key");
        String k2 = new String("key"); // equals to k1 but different identity
        Map<String,Integer> hm = new HashMap<>();
        hm.put(k1, 1); hm.put(k2, 2); // overwrites (same equals/hash)
        System.out.println("HashMap with equal keys: " + hm); // one entry

        Map<String,Integer> im = new IdentityHashMap<>();
        im.put(k1, 1); im.put(k2, 2); // two entries (identity-based)
        System.out.println("IdentityHashMap with equal-content keys: " + im);

        // WeakHashMap: entries vanish when keys are only weakly reachable
        Map<Object,String> weak = new WeakHashMap<>();
        Object key = new Object();
        weak.put(key, "value");
        System.out.println("WeakHashMap before GC: " + weak.size());
        key = null; // drop strong reference
        System.gc();
        try { Thread.sleep(50); } catch (InterruptedException ignored) {}
        System.out.println("WeakHashMap after GC (may be 0): " + weak.size());
    }

    private static void demoConcurrentCollections() {
        System.out.println("\n-- Concurrent collections & atomic ops --");
        ConcurrentMap<String,Integer> cm = new ConcurrentHashMap<>();
        cm.putIfAbsent("a", 1);
        cm.merge("a", 2, Integer::sum); // a = 1+2
        cm.computeIfAbsent("b", k -> 5);
        System.out.println("ConcurrentHashMap atomic ops: " + cm);
        try {
            cm.put(null, 1); // not allowed
        } catch (NullPointerException npe) {
            System.out.println("ConcurrentHashMap null disallowed: " + npe.getClass().getSimpleName());
        }

        // BlockingQueue (producer/consumer)
        BlockingQueue<String> q = new ArrayBlockingQueue<>(2);
        q.offer("t1");
        q.offer("t2");
        boolean offered = q.offer("t3"); // false; would block if using put()
        System.out.println("ArrayBlockingQueue: " + q + ", offer when full=" + offered);
        System.out.println("poll: " + q.poll() + ", after poll: " + q);
    }

    private static void demoSpliteratorsAndStreams() {
        System.out.println("\n-- Spliterator & Streams quick peek --");
        List<Integer> list = Arrays.asList(1,2,3,4,5);
        Spliterator<Integer> sp = list.spliterator();
        int ch = sp.characteristics();
        System.out.println("Spliterator characteristics: ORDERED=" + has(ch, Spliterator.ORDERED) +
                ", SIZED=" + has(ch, Spliterator.SIZED) + ", SUBSIZED=" + has(ch, Spliterator.SUBSIZED));

        // Stream from collections (Java 8+)
        int sumSquaresEven = list.stream().filter(x -> x % 2 == 0).mapToInt(x -> x*x).sum();
        System.out.println("Stream example sum of squares of even: " + sumSquaresEven);
    }

    private static boolean has(int characteristics, int flag) {
        return (characteristics & flag) != 0;
    }

    /*
     Best practices:
     - Choose by access pattern: ArrayList for random access; LinkedList for frequent head/tail ops; Tree* when you need sorted range queries; Hash* for fast lookups; LinkedHashMap for LRU; Enum* for enum keys/sets.
     - Avoid modifying collections while iterating; use Iterator.remove or collect to a list then removeAll.
     - Do not mutate keys used in hash-based or sorted collections while stored; it breaks data structures.
     - Prefer ConcurrentHashMap and friends for multithreaded access.
     - Use Collections.unmodifiableXxx for read-only views; Java 9+ List.of/Set.of/Map.of for immutable collections.
     - Use Comparator.comparing / thenComparing for readable, null-safe ordering.
     - Be mindful of null policies (ConcurrentHashMap disallows nulls).
     - Understand views vs copies: subList/keySet/range views are backed; copying creates independent collections (new ArrayList<>(view)).
     */
}