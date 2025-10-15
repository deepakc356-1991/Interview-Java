package _04_02_list_set_map_implementations;

import java.util.*;
import java.util.concurrent.*;

/**
 List, Set, Map Implementations — Theory + Practical snippets

 Interfaces
 - List: ordered, indexed, duplicates allowed, null allowed. Equality is order-sensitive.
 - Set: unique elements, no index. SortedSet/NavigableSet add ordering/range operations. Equality ignores order.
 - Map: key->value, unique keys. SortedMap/NavigableMap add ordering/range. Equality compares mappings.

 Implementations (core characteristics)
 - List:
   * ArrayList: dynamic array; O(1) amortized append, O(1) random access; O(n) middle insert/remove.
   * LinkedList: doubly-linked; O(1) add/remove at ends; O(n) random access; implements Deque.
   * Vector/Stack: legacy synchronized; use ArrayList/Deque instead.
   * CopyOnWriteArrayList: snapshot iterators; O(n) writes, O(1) reads; great for many reads/few writes.
 - Set:
   * HashSet: hash table; O(1) expected ops; allows one null; no order guarantees.
   * LinkedHashSet: HashSet + insertion order (or access order for LinkedHashMap).
   * TreeSet: red-black tree; sorted; O(log n); no null elements; Comparator or natural order.
   * EnumSet: very fast bit-vector for enum types; no nulls.
   * CopyOnWriteArraySet: snapshot iterators (backed by CopyOnWriteArrayList).
   * ConcurrentSkipListSet: concurrent, sorted (skip list); O(log n).
 - Map:
   * HashMap: hash table; O(1) expected; allows one null key, null values; Java 8+ tree-bins on heavy collisions.
   * LinkedHashMap: HashMap + predictable order (insertion or access); supports LRU caches via removeEldestEntry.
   * TreeMap: red-black tree; sorted; O(log n); no null keys; allows null values.
   * Hashtable: legacy synchronized; no nulls; avoid in new code.
   * ConcurrentHashMap: high-concurrency map; no nulls; weakly consistent iterators.
   * WeakHashMap: keys are weakly referenced; entries auto-removed when keys GC’d.
   * IdentityHashMap: keys compared by reference (==), not equals/hashCode.
   * EnumMap: array-backed map for enum keys; very fast; no null keys.

 Null policy (commonly asked)
 - List: usually allows null.
 - Set: HashSet/LinkedHashSet allow one null; TreeSet disallows null.
 - Map: HashMap/LinkedHashMap allow one null key and multiple null values; TreeMap disallows null keys; Hashtable/ConcurrentHashMap disallow null keys/values; Enum* disallow nulls.

 Ordering
 - List preserves insertion order.
 - HashSet/HashMap: no order guarantee.
 - LinkedHashSet/LinkedHashMap: insertion order (or access order for LinkedHashMap with accessOrder=true).
 - TreeSet/TreeMap: sorted by natural order or Comparator.

 Performance notes
 - Hash-based: O(1) expected, degrade to O(n) worst-case; Java 8+ treeify threshold reduces worst-case to O(log n).
 - Tree-based: O(log n) for add/remove/search.
 - CopyOnWrite: reads cheap, writes copy the array.
 - Choose initial capacity/load factor (default 16, 0.75) to reduce resizes.

 Equality
 - List equals compares order and elements.
 - Set equals compares elements irrespective of order.
 - Map equals compares key-value mappings, independent of implementation.

 Iterators
 - Most java.util iterators are fail-fast: ConcurrentModificationException on structural modification.
 - Concurrent collections provide weakly consistent or snapshot iterators (fail-safe).

 Views and immutability
 - subList, keySet, entrySet, values are live views; modifying the backing collection updates the view and vice versa.
 - Collections.unmodifiableX creates read-only views (shallow). Java 9+ List.of/Set.of/Map.of create truly immutable collections.

 Sorting
 - Lists: List.sort/Collections.sort with Comparator.
 - Sets/Maps: use TreeSet/TreeMap for always-sorted or copy to a List and sort.

 Spliterators (streams)
 - Implementations expose characteristics like ORDERED (List), DISTINCT (Set), SORTED (TreeSet), CONCURRENT (ConcurrentHashMap).

**/

public final class _01_Theory {

    enum Day { MON, TUE, WED, THU, FRI, SAT, SUN }
    enum Color { RED, GREEN, BLUE }

    public static void main(String[] args) {
        System.out.println("=== LIST IMPLEMENTATIONS ===");
        demoLists();
        System.out.println();
        System.out.println("=== SET IMPLEMENTATIONS ===");
        demoSets();
        System.out.println();
        System.out.println("=== MAP IMPLEMENTATIONS ===");
        demoMaps();
        System.out.println();
        System.out.println("=== IMMUTABILITY, VIEWS, WRAPPERS ===");
        demoImmutabilityAndViews();
        System.out.println();
        System.out.println("=== CONCURRENCY & ITERATORS NOTES ===");
        demoConcurrencyNotes();
    }

    private static void demoLists() {
        // ArrayList: ordered, duplicates allowed, allows nulls.
        List<String> arrayList = new ArrayList<>();
        arrayList.add("B");
        arrayList.add("A");
        arrayList.add("B"); // duplicate allowed
        arrayList.add(null); // null allowed
        print("ArrayList keeps insertion order, allows duplicates and null", arrayList);
        print("arrayList.get(2)", arrayList.get(2));
        arrayList.add(1, "X"); // insert in middle (O(n))
        print("After add(1, X)", arrayList);

        // LinkedList: good for deque operations (add/remove at ends).
        LinkedList<String> linkedList = new LinkedList<>();
        linkedList.add("one");
        linkedList.add("two");
        linkedList.addFirst("zero");
        linkedList.addLast("three");
        print("LinkedList as deque", linkedList);
        print("linkedList.removeFirst()", linkedList.removeFirst());
        print("linkedList now", linkedList);

        // Fail-fast iterator demo (will throw on concurrent structural modification).
        try {
            for (String s : arrayList) {
                if ("A".equals(s)) arrayList.add("Y"); // structural modification during iteration
            }
        } catch (ConcurrentModificationException e) {
            System.out.println("ArrayList iterator is fail-fast: " + e.getClass().getSimpleName());
        }

        // CopyOnWriteArrayList: safe to modify while iterating; iterator sees snapshot.
        CopyOnWriteArrayList<String> cow = new CopyOnWriteArrayList<>();
        cow.add("a");
        cow.add("b");
        for (String s : cow) {
            if ("a".equals(s)) cow.add("c"); // allowed; snapshot semantics
        }
        print("CopyOnWriteArrayList after iter+add", cow);

        // Sorting and replacing
        List<String> toSort = new ArrayList<>(Arrays.asList("b", "C", "a"));
        Collections.sort(toSort, String.CASE_INSENSITIVE_ORDER);
        print("Sorted (case-insensitive)", toSort);
        Collections.replaceAll(toSort, "b", "B");
        print("After replaceAll b->B", toSort);

        // subList is a live view; structural changes on parent invalidate view's iterator.
        List<String> base = new ArrayList<>(Arrays.asList("p", "q", "r", "s"));
        List<String> view = base.subList(1, 3); // ["q", "r"]
        print("base", base);
        print("subList(1,3) view", view);
        view.set(0, "Q"); // modifies base
        print("After view.set(0,Q), base", base);
        try {
            base.add("t");    // structural change to base
            view.size();      // touching the view after base changed triggers CME
        } catch (ConcurrentModificationException e) {
            System.out.println("subList is a view; structural change in base invalidates it: " + e.getClass().getSimpleName());
        }

        // Stack (legacy) vs Deque (preferred for stack/queue)
        Stack<Integer> stack = new Stack<>();
        stack.push(1); stack.push(2); stack.push(3);
        print("Stack LIFO pop", stack.pop());
        Deque<Integer> deque = new ArrayDeque<>();
        deque.push(10); deque.push(20);
        print("Deque LIFO pop", deque.pop());

        // Vector (legacy synchronized list)
        Vector<Integer> vector = new Vector<>(Arrays.asList(1, 2, 3));
        print("Vector (synchronized, legacy)", vector);
    }

    private static void demoSets() {
        // HashSet: unique elements, no order guarantee, allows one null.
        Set<String> hs = new HashSet<>();
        hs.add("A"); hs.add("B"); hs.add("A"); hs.add(null);
        print("HashSet: unique elements, no order guarantee", hs);

        // LinkedHashSet: preserves insertion order (also allows one null).
        Set<String> lhs = new LinkedHashSet<>();
        lhs.add("one"); lhs.add("two"); lhs.add("one"); lhs.add(null);
        print("LinkedHashSet: insertion order + uniqueness", lhs);

        // TreeSet: sorted, O(log n); no nulls.
        NavigableSet<String> ts = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        ts.add("b"); ts.add("A"); ts.add("c");
        print("TreeSet sorted (case-insensitive)", ts);
        print("TreeSet.lower(\"b\")", ts.lower("b"));
        print("TreeSet.ceiling(\"b\")", ts.ceiling("b"));

        // EnumSet: extremely compact/fast for enums.
        EnumSet<Day> weekend = EnumSet.of(Day.SAT, Day.SUN);
        EnumSet<Day> workdays = EnumSet.range(Day.MON, Day.FRI);
        print("EnumSet weekend", weekend);
        print("EnumSet workdays", workdays);

        // CopyOnWriteArraySet: snapshot iterators; deduplicates like a Set.
        CopyOnWriteArraySet<Integer> cowSet = new CopyOnWriteArraySet<>();
        cowSet.add(1); cowSet.add(1); cowSet.add(2);
        for (Integer i : cowSet) {
            cowSet.add(3); // safe during iteration; iterator doesn't see this addition
        }
        print("CopyOnWriteArraySet after iter+add", cowSet);

        // ConcurrentSkipListSet: concurrent + sorted set.
        ConcurrentSkipListSet<Integer> css = new ConcurrentSkipListSet<>();
        css.add(3); css.add(1); css.add(2);
        print("ConcurrentSkipListSet sorted", css);

        // Set equality ignores order
        Set<Integer> a1 = new HashSet<>(Arrays.asList(1, 2, 3));
        Set<Integer> a2 = new LinkedHashSet<>(Arrays.asList(3, 2, 1));
        print("Set equals ignores order", a1.equals(a2));
    }

    private static void demoMaps() {
        // HashMap: allows one null key and null values.
        Map<String, Integer> hm = new HashMap<>();
        hm.put("a", 1);
        hm.put("b", 2);
        hm.put("a", 3); // overwrite
        hm.put(null, 99); // null key
        hm.put("nullValue", null);
        print("HashMap allows one null key and null values", hm);
        print("HashMap.get(null)", hm.get(null));
        print("HashMap.getOrDefault(\"x\",42)", hm.getOrDefault("x", 42));

        // computeIfAbsent: build a multimap pattern
        Map<String, List<Integer>> multimap = new HashMap<>();
        for (int i = 0; i < 3; i++) {
            String key = (i % 2 == 0) ? "even" : "odd";
            multimap.computeIfAbsent(key, k -> new ArrayList<>()).add(i);
        }
        print("computeIfAbsent multimap", multimap);

        // merge: aggregate counts
        Map<String, Integer> counts = new HashMap<>();
        for (String s : Arrays.asList("a", "b", "a", "c", "b", "a")) {
            counts.merge(s, 1, Integer::sum);
        }
        print("merge word counts", counts);

        // LinkedHashMap: insertion order
        Map<String, Integer> lhInsertion = new LinkedHashMap<>();
        lhInsertion.put("x", 1); lhInsertion.put("y", 2); lhInsertion.put("z", 3);
        print("LinkedHashMap (insertion order)", lhInsertion);

        // LinkedHashMap as LRU (access order + eviction via removeEldestEntry)
        LinkedHashMap<String, Integer> lru = new LinkedHashMap<String, Integer>(3, 0.75f, true) {
            protected boolean removeEldestEntry(Map.Entry<String, Integer> eldest) {
                return size() > 3;
            }
        };
        lru.put("A", 1); lru.put("B", 2); lru.put("C", 3);
        lru.get("A"); // touch A to make it most-recent
        lru.put("D", 4); // evicts least-recently-used (B)
        print("LinkedHashMap as LRU (capacity 3)", lru);

        // TreeMap: sorted by key; no null keys
        NavigableMap<String, Integer> tm = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        tm.put("b", 2); tm.put("A", 1); tm.put("c", 3);
        print("TreeMap sorted by comparator", tm);
        print("TreeMap.firstEntry()", tm.firstEntry());
        print("TreeMap.ceilingKey(\"b\")", tm.ceilingKey("b"));

        // Hashtable: legacy synchronized map (no nulls); avoid in modern code
        Hashtable<String, Integer> hashtable = new Hashtable<>();
        hashtable.put("k", 1);
        print("Hashtable (legacy, synchronized)", hashtable);

        // ConcurrentHashMap: high concurrency; no nulls; atomic ops
        ConcurrentHashMap<String, Integer> chm = new ConcurrentHashMap<>();
        chm.putIfAbsent("x", 1);
        chm.merge("x", 1, Integer::sum);
        chm.compute("y", (k, v) -> v == null ? 100 : v + 1);
        print("ConcurrentHashMap atomic ops", chm);

        // IdentityHashMap: reference equality for keys
        IdentityHashMap<String, String> ihm = new IdentityHashMap<>();
        String s1 = new String("k");
        String s2 = new String("k");
        ihm.put(s1, "v1");
        ihm.put(s2, "v2"); // distinct keys because different references
        print("IdentityHashMap (two distinct String refs)", ihm);

        // WeakHashMap: entries removed when keys are GC’d (non-deterministic)
        WeakHashMap<Object, String> whm = new WeakHashMap<>();
        Object key = new Object();
        whm.put(key, "alive");
        print("WeakHashMap before GC (size)", whm.size());
        key = null;
        System.gc();
        try { Thread.sleep(50); } catch (InterruptedException ignored) {}
        print("WeakHashMap after GC hint (size, non-deterministic)", whm.size());

        // EnumMap: fast map for enum keys
        EnumMap<Color, String> enumMap = new EnumMap<>(Color.class);
        enumMap.put(Color.RED, "stop");
        enumMap.put(Color.GREEN, "go");
        print("EnumMap", enumMap);

        // Map equals compares mappings, not implementation
        Map<String, Integer> hm2 = new HashMap<>();
        hm2.putAll(lhInsertion);
        print("Map equals by mappings, not impl", lhInsertion.equals(hm2));
    }

    private static void demoImmutabilityAndViews() {
        // Unmodifiable view: read-only facade (shallow). Backing changes reflect.
        List<String> mutable = new ArrayList<>(Arrays.asList("a", "b"));
        List<String> unmod = Collections.unmodifiableList(mutable);
        print("UnmodifiableList view", unmod);
        try {
            unmod.add("c");
        } catch (UnsupportedOperationException e) {
            System.out.println("UnmodifiableList blocks mutation: " + e.getClass().getSimpleName());
        }
        mutable.add("c"); // backing change is visible in view
        print("Changes in backing list reflect in view", unmod);

        // Arrays.asList returns a fixed-size list (backed by the array)
        List<String> fixed = Arrays.asList("x", "y");
        try {
            fixed.add("z");
        } catch (UnsupportedOperationException e) {
            System.out.println("Arrays.asList is fixed-size: " + e.getClass().getSimpleName());
        }

        // Java 9+ provides truly immutable List.of/Set.of/Map.of (not used here for Java 8 compatibility).
        System.out.println("Note: Java 9+ provides immutable List.of/Set.of/Map.of (not shown here).");

        // Views from Map are live
        Map<String, Integer> m = new HashMap<>();
        m.put("a", 1); m.put("b", 2);
        Set<String> keys = m.keySet(); // live view
        m.put("c", 3);
        print("Map.keySet is a live view", keys);
    }

    private static void demoConcurrencyNotes() {
        // Synchronized wrappers: external locking required during iteration
        List<Integer> syncList = Collections.synchronizedList(new ArrayList<Integer>());
        syncList.add(1); syncList.add(2);
        synchronized (syncList) {
            for (Integer i : syncList) {
                // iterate under lock
            }
        }
        print("Collections.synchronizedX returns wrappers with external locking", syncList);

        // Fail-fast vs fail-safe summary
        System.out.println("Fail-fast: most java.util iterators throw ConcurrentModificationException on structural modification.");
        System.out.println("Fail-safe: concurrent collections iterate over snapshot/weakly consistent views (no CME).");

        // Complexity cheatsheet
        System.out.println("Complexity: ArrayList O(1) amortized append, O(1) get; LinkedList O(1) add/remove at ends, O(n) get.");
        System.out.println("HashSet/HashMap O(1) expected ops; TreeSet/TreeMap O(log n); LinkedHashMap O(1) with predictable order.");
    }

    private static void print(String label, Object value) {
        System.out.println(label + " -> " + value);
    }
}