package _04_01_collection_hierarchy;

import java.lang.ref.ReferenceQueue;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;

/*
Collection Hierarchy Interview Q&A (Basic → Intermediate → Advanced)
- Runnable cheat-sheet with concise demos. Read comments, run main() to see outputs.
- Coverage highlights:
  1) Core hierarchy and interfaces; differences between Collection and Map
  2) List vs Set vs Queue; ordering vs sorting; major implementations and their traits
  3) Big-O, ArrayList growth, HashMap internals (hashing, load factor, tree bins)
  4) equals/hashCode contracts; mutable key pitfalls
  5) Iteration: Iterator vs ListIterator; fail-fast vs weakly consistent; Enumeration (legacy)
  6) Synchronized wrappers; unmodifiable vs immutable; subList view pitfalls
  7) HashMap vs Hashtable null-handling; LinkedHashMap LRU cache
  8) Sorted/Navigable collections; Comparator vs Comparable; comparator consistency with equals
  9) PriorityQueue heap semantics vs TreeSet
  10) Deque vs Stack (legacy); Queue method semantics
  11) Concurrency: ConcurrentHashMap; CopyOnWriteArrayList; BlockingQueue
  12) Special maps/sets: EnumMap/EnumSet; WeakHashMap; IdentityHashMap
  13) RandomAccess marker; Spliterator (brief); Streams vs Collections (brief)
Note: Some behaviors (GC, timings) are JVM/OS-dependent and shown illustratively.
*/
public class _03_InterviewQA {

    public static void main(String[] args) throws Exception {
        title("1) Core hierarchy, Collection vs Map");
        basicsHierarchy();
        listVsSetVsQueue();
        mapIsNotCollection();

        title("2) Ordering characteristics of implementations");
        orderingImplementations();

        title("3) ArrayList capacity/growth (commentary + ensureCapacity)");
        arrayListCapacityAndGrowth();

        title("4) HashMap internals (hashing, collisions, tree bins commentary)");
        hashMapInternals();

        title("5) equals/hashCode contract and pitfalls");
        equalsHashCodeContract();
        mutableKeyPitfall();

        title("6) Iteration: fail-fast and safe removal/modification");
        iterationFailFast();
        iteratorRemoveVsListIterator();

        title("7) Synchronized wrappers and concurrent iteration reminder");
        synchronizedWrappers();

        title("8) Unmodifiable views vs immutable snapshots; subList pitfalls");
        unmodifiableViewsVsImmutable();
        subListViewPitfalls();

        title("9) HashMap vs Hashtable (null keys/values)");
        hashMapVsHashtableNulls();

        title("10) LinkedHashMap as LRU cache");
        linkedHashMapLRUCache();

        title("11) TreeSet/TreeMap with Comparator; comparator consistency");
        treeSetComparator();

        title("12) PriorityQueue vs TreeSet");
        priorityQueueBasics();

        title("13) Deque vs legacy Stack");
        dequeVsStack();

        title("14) Queue method semantics (add/offer/remove/poll/element/peek)");
        queueMethodDiffs();

        title("15) BlockingQueue brief demo");
        blockingQueueBrief();

        title("16) ConcurrentHashMap basics");
        concurrentHashMapBasics();

        title("17) CopyOnWriteArrayList demo");
        copyOnWriteArrayListDemo();

        title("18) WeakHashMap demo (GC-dependent)");
        weakHashMapDemo();

        title("19) EnumSet and EnumMap");
        enumCollectionsDemo();

        title("20) IdentityHashMap vs HashMap");
        identityHashMapDemo();

        title("21) RandomAccess marker and iteration strategy");
        randomAccessStrategy();

        title("22) Sorting stability (ordering vs sorting)");
        orderingVsSortingAndStability();

        title("23) Notes: Big-O, Spliterator, Streams vs Collections (comments)");
        notesCommentsOnly();
    }

    // 1) Core hierarchy
    // Q: What's the Java Collections Framework hierarchy?
    // A: Root interfaces: Collection and Map (Map is NOT a Collection).
    //    Collection -> List, Set, Queue; Deque extends Queue.
    //    SortedSet/NavigableSet, SortedMap/NavigableMap refine ordering/navigation.
    private static void basicsHierarchy() {
        line("Collection -> List, Set, Queue; Deque extends Queue");
        line("Map is separate (key-value), with SortedMap/NavigableMap");
        line("Common concrete types: ArrayList, LinkedList, HashSet, LinkedHashSet, TreeSet, HashMap, LinkedHashMap, TreeMap, PriorityQueue, ArrayDeque");
    }

    // Q: List vs Set vs Queue?
    // A: List: ordered, allows duplicates; Set: unique elements; Queue: FIFO semantics; Deque: both ends.
    private static void listVsSetVsQueue() {
        List<String> list = new ArrayList<>();
        list.add("A"); list.add("B"); list.add("A");
        Set<String> set = new HashSet<>();
        set.add("A"); set.add("B"); set.add("A");
        Queue<String> q = new ArrayDeque<>();
        q.add("A"); q.add("B"); q.add("A");
        line("List keeps duplicates: " + list);
        line("Set removes duplicates: " + set);
        line("Queue order (FIFO): head=" + q.peek() + ", all=" + q);
    }

    // Q: Is Map part of Collection?
    // A: No. Different abstraction (key-value). You cannot pass Map where Collection is expected.
    private static void mapIsNotCollection() {
        Map<String, Integer> m = new HashMap<>();
        m.put("x", 1);
        line("Map not a Collection: cannot assign Map to Collection<?> (by type system).");
        line("Map views are Collections: keySet(), values(), entrySet()");
    }

    // 2) Ordering of implementations
    // Q: Ordering differences among HashSet, LinkedHashSet, TreeSet?
    // A: HashSet: no predictable order; LinkedHashSet: insertion order; TreeSet: sorted order.
    private static void orderingImplementations() {
        List<String> data = Arrays.asList("B", "A", "C", "A");
        Set<String> hs = new HashSet<>(data);
        Set<String> lhs = new LinkedHashSet<>(data);
        Set<String> ts = new TreeSet<>(data);
        line("HashSet:        " + hs + " (no order)");
        line("LinkedHashSet:  " + lhs + " (insertion order)");
        line("TreeSet:        " + ts + " (sorted)");
        Map<String, Integer> hm = new HashMap<>();
        hm.put("B",2); hm.put("A",1); hm.put("C",3);
        Map<String, Integer> lhm = new LinkedHashMap<>();
        lhm.put("B",2); lhm.put("A",1); lhm.put("C",3);
        line("HashMap keys:   " + hm.keySet() + " (no order)");
        line("LinkedHashMap:  " + lhm.keySet() + " (insertion order)");
    }

    // 3) ArrayList growth
    // Q: How does ArrayList grow?
    // A: Backed by array; grows by ~1.5x when capacity exceeded (JDK8+). ensureCapacity can preallocate.
    private static void arrayListCapacityAndGrowth() {
        ArrayList<Integer> al = new ArrayList<>();
        al.ensureCapacity(1000);
        line("Called ensureCapacity(1000) to reduce resizes; growth factor ~1.5x on expansion (JDK8+).");
    }

    // 4) HashMap internals
    // Q: How does HashMap work?
    // A: Array of buckets; index = (n-1) & hash; separate chaining via nodes; since JDK8, buckets treeify (RB-tree) when collisions > threshold (8) and table >= 64.
    //    loadFactor default 0.75; resize when size > threshold (capacity*loadFactor).
    private static void hashMapInternals() {
        Map<BadHash, String> map = new HashMap<>();
        for (int i = 0; i < 12; i++) map.put(new BadHash(i), "v"+i); // heavy collisions
        line("HashMap handles collisions; retrieval still works: " + map.get(new BadHash(3)));
        line("Tree bins engage after many collisions; worst-case O(log n) in bucket (JDK8+).");
    }

    // 5) equals/hashCode
    // Q: Why must equals and hashCode be consistent?
    // A: Hash-based collections use hashCode to find bucket and equals to resolve within bucket.
    private static void equalsHashCodeContract() {
        Set<PersonBad> badSet = new HashSet<>();
        badSet.add(new PersonBad(1, "A"));
        badSet.add(new PersonBad(1, "A")); // equals true, but hashCode differs (not overridden) -> duplicates
        line("Bad equals/hashCode set size (should be 1): " + badSet.size());

        Set<PersonGood> goodSet = new HashSet<>();
        goodSet.add(new PersonGood(1, "A"));
        goodSet.add(new PersonGood(1, "A")); // equals true and hashCode consistent -> dedup
        line("Good equals/hashCode set size: " + goodSet.size());
    }

    // Q: Why should keys be immutable?
    // A: Changing fields involved in equals/hashCode after insertion breaks lookup semantics.
    private static void mutableKeyPitfall() {
        Set<MutableKey> set = new HashSet<>();
        MutableKey k = new MutableKey(1, "X");
        set.add(k);
        line("Contains before mutate: " + set.contains(k));
        k.id = 2; // mutating key breaks its hash bucket
        line("Contains after mutate (broken): " + set.contains(k));
    }

    // 6) Iteration fail-fast
    // Q: What is fail-fast iterator?
    // A: Detects concurrent structural modification and throws ConcurrentModificationException.
    private static void iterationFailFast() {
        List<Integer> l = new ArrayList<>(Arrays.asList(1,2,3));
        try {
            for (Integer x : l) {
                if (x == 2) l.add(99); // structural mod while iterating
            }
        } catch (ConcurrentModificationException e) {
            line("Caught ConcurrentModificationException (fail-fast).");
        }
    }

    // Q: How to remove safely while iterating?
    // A: Use Iterator.remove(); ListIterator supports add(), set(), bidirectional traversal.
    private static void iteratorRemoveVsListIterator() {
        List<Integer> l = new ArrayList<>(Arrays.asList(1,2,3,4,5));
        for (Iterator<Integer> it = l.iterator(); it.hasNext(); ) {
            if (it.next() % 2 == 0) it.remove(); // safe removal
        }
        line("After iterator.remove (removed evens): " + l);

        ListIterator<Integer> li = l.listIterator();
        while (li.hasNext()) {
            Integer v = li.next();
            if (v == 3) li.add(99); // insert immediately before next element
            if (v == 5) li.set(50); // replace current
        }
        line("After ListIterator add/set: " + l);
    }

    // 7) Synchronized wrappers
    // Q: Collections.synchronizedXxx?
    // A: Wraps to provide intrinsic-lock synchronization; must manually synchronize during iteration.
    private static void synchronizedWrappers() {
        List<Integer> sync = Collections.synchronizedList(new ArrayList<>(Arrays.asList(1,2,3)));
        synchronized (sync) { for (Integer i : sync) {/* safe iteration */} }
        line("Use synchronized wrappers for legacy-style thread safety; synchronize when iterating.");
    }

    // 8) Unmodifiable vs immutable; subList pitfalls
    // Q: Unmodifiable view vs immutable snapshot?
    // A: unmodifiableXxx returns a read-only view over the backing collection; snapshot = copy + unmodifiable to freeze current state.
    private static void unmodifiableViewsVsImmutable() {
        List<Integer> base = new ArrayList<>(Arrays.asList(1,2,3));
        List<Integer> view = Collections.unmodifiableList(base);
        base.add(4);
        line("Unmodifiable view reflects backing changes: " + view);
        try { view.add(5); } catch (UnsupportedOperationException e) { line("View is read-only (cannot modify)."); }
        List<Integer> snapshot = Collections.unmodifiableList(new ArrayList<>(base));
        base.add(5);
        line("Immutable snapshot unaffected by later base changes: " + snapshot);
    }

    // Q: subList is a view; pitfalls?
    // A: Structural change in parent invalidates subList and can cause ConcurrentModificationException.
    private static void subListViewPitfalls() {
        List<Integer> l = new ArrayList<>(Arrays.asList(0,1,2,3,4,5));
        List<Integer> sub = l.subList(1, 4); // [1,2,3]
        sub.set(0, 10);
        line("Parent reflects subList changes: " + l);
        l.add(99); // structural change on parent
        try { sub.size(); } catch (ConcurrentModificationException e) { line("subList invalidated after parent structural change."); }
    }

    // 9) HashMap vs Hashtable
    // Q: Null keys/values?
    // A: HashMap allows one null key and many null values; Hashtable allows none.
    private static void hashMapVsHashtableNulls() {
        Map<String, Integer> hm = new HashMap<>();
        hm.put(null, 1);
        hm.put("x", null);
        line("HashMap allows nulls: " + hm);
        Hashtable<String, Integer> ht = new Hashtable<>();
        try { ht.put(null, 1); } catch (NullPointerException e) { line("Hashtable rejects null key."); }
        try { ht.put("x", null); } catch (NullPointerException e) { line("Hashtable rejects null value."); }
    }

    // 10) LRU cache with LinkedHashMap
    // Q: How to build LRU?
    // A: LinkedHashMap with accessOrder=true and override removeEldestEntry.
    private static void linkedHashMapLRUCache() {
        LRUCache<String, Integer> lru = new LRUCache<>(3);
        lru.put("A",1); lru.put("B",2); lru.put("C",3);
        lru.get("A"); // access A to make it most-recent
        lru.put("D",4); // evicts B
        line("LRU content (expect C, A, D): " + lru.keySet());
    }

    // 11) Comparator in TreeSet/TreeMap
    // Q: Duplicate in SortedSet determined by comparator (compare==0), not equals.
    private static void treeSetComparator() {
        Comparator<String> ci = String::compareToIgnoreCase;
        Set<String> cs = new TreeSet<>(ci);
        cs.add("a"); cs.add("B"); cs.add("b");
        line("Case-insensitive TreeSet (a,B,b -> duplicates by comparator): " + cs);
    }

    // 12) PriorityQueue
    // Q: PriorityQueue is a heap; peek/poll are O(1)/O(log n); iteration order is not sorted.
    private static void priorityQueueBasics() {
        PriorityQueue<Integer> pq = new PriorityQueue<>();
        pq.addAll(Arrays.asList(5,1,3,2));
        line("PriorityQueue peek (min): " + pq.peek());
        line("PriorityQueue iteration not sorted: " + pq);
        line("Poll sequence: " + pq.poll() + "," + pq.poll() + "," + pq.poll() + "," + pq.poll());
    }

    // 13) Deque vs Stack
    // Q: Prefer ArrayDeque over legacy Stack (synchronized, obsolete).
    private static void dequeVsStack() {
        Deque<Integer> stack = new ArrayDeque<>();
        stack.push(1); stack.push(2); stack.push(3);
        line("ArrayDeque as stack (LIFO pop): " + stack.pop() + ", " + stack.pop() + ", " + stack.pop());
    }

    // 14) Queue method semantics
    // add throws on capacity breach; offer returns false.
    // remove throws on empty; poll returns null.
    // element throws on empty; peek returns null.
    private static void queueMethodDiffs() {
        ArrayDeque<Integer> q = new ArrayDeque<>(1);
        q.add(1);
        boolean offered = q.offer(2);
        line("offer on full queue returns: " + offered);
        try { q.add(2); } catch (IllegalStateException e) { line("add on full queue throws IllegalStateException."); }
        q.clear();
        Integer polled = q.poll();
        line("poll on empty returns: " + polled);
        try { q.remove(); } catch (NoSuchElementException e) { line("remove on empty throws NoSuchElementException."); }
        line("peek on empty returns: " + q.peek());
        try { q.element(); } catch (NoSuchElementException e) { line("element on empty throws NoSuchElementException."); }
    }

    // 15) BlockingQueue
    // Q: BlockingQueue put/take block when full/empty.
    private static void blockingQueueBrief() throws InterruptedException {
        ArrayBlockingQueue<Integer> q = new ArrayBlockingQueue<>(1);
        Thread consumer = new Thread(() -> {
            sleep(150);
            try { Integer v = q.take(); line("Consumer took: " + v); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        });
        consumer.start();
        long t0 = System.currentTimeMillis();
        q.put(1); // immediate
        q.put(2); // blocks until consumer takes
        long dt = System.currentTimeMillis() - t0;
        line("BlockingQueue put() blocked approximately ms: " + dt);
        consumer.join();
    }

    // 16) ConcurrentHashMap
    // Q: No null keys/values; weakly consistent iterators (no fail-fast); computeIfAbsent, merge.
    private static void concurrentHashMapBasics() {
        ConcurrentHashMap<String, Integer> chm = new ConcurrentHashMap<>();
        try { chm.put(null, 1); } catch (NullPointerException e) { line("ConcurrentHashMap rejects null key."); }
        try { chm.put("x", null); } catch (NullPointerException e) { line("ConcurrentHashMap rejects null value."); }
        chm.put("a", 1); chm.put("b", 2);
        chm.computeIfAbsent("c", k -> 3);
        line("CHM computeIfAbsent: " + chm);
        // Weakly consistent iteration (tolerates concurrent updates):
        for (String k : chm.keySet()) {
            if ("a".equals(k)) chm.put("d", 4);
        }
        line("CHM after concurrent put during iteration: " + chm);
    }

    // 17) CopyOnWriteArrayList
    // Q: Good for read-mostly; iterators are snapshot-based; writes copy whole array; iterator remove unsupported.
    private static void copyOnWriteArrayListDemo() {
        CopyOnWriteArrayList<Integer> cow = new CopyOnWriteArrayList<>(Arrays.asList(1,2,3));
        Iterator<Integer> it = cow.iterator();
        cow.add(4);
        List<Integer> seen = new ArrayList<>();
        while (it.hasNext()) seen.add(it.next());
        line("COW iterator sees snapshot: " + seen + ", current list: " + cow);
        try { it.remove(); } catch (UnsupportedOperationException e) { line("COW iterator.remove unsupported."); }
    }

    // 18) WeakHashMap
    // Q: Keys are weakly referenced; entries removed when keys GC'd (non-deterministic).
    private static void weakHashMapDemo() {
        WeakHashMap<Object, String> wm = new WeakHashMap<>();
        Object strong = new Object();
        Object weak = new Object();
        wm.put(strong, "strong");
        wm.put(weak, "weak");
        line("WeakHashMap before GC size: " + wm.size());
        weak = null; // drop strong ref to second key
        System.gc();
        sleep(200);
        line("WeakHashMap after GC (may shrink): " + wm.size() + " " + wm);
    }

    // 19) EnumSet / EnumMap
    // Q: Space/time efficient for enum keys; implemented as bit vectors/arrays.
    private static void enumCollectionsDemo() {
        EnumSet<Day> weekend = EnumSet.of(Day.SAT, Day.SUN);
        EnumMap<Day, String> schedule = new EnumMap<>(Day.class);
        schedule.put(Day.MON, "Work");
        schedule.put(Day.SAT, "Hike");
        line("EnumSet: " + weekend);
        line("EnumMap: " + schedule);
    }

    // 20) IdentityHashMap
    // Q: Uses reference-equality (==) for keys; useful for object identity maps, not value equality.
    private static void identityHashMapDemo() {
        String a1 = new String("A");
        String a2 = new String("A");
        Map<String, Integer> h = new HashMap<>();
        h.put(a1, 1); h.put(a2, 2);
        Map<String, Integer> im = new IdentityHashMap<>();
        im.put(a1, 1); im.put(a2, 2);
        line("HashMap (equals-based) size: " + h.size());
        line("IdentityHashMap (== based) size: " + im.size());
    }

    // 21) RandomAccess marker
    // Q: RandomAccess marks fast index access (e.g., ArrayList). Prefer indexed loop; for LinkedList use iterator.
    private static void randomAccessStrategy() {
        iterateSmart(new ArrayList<>(Arrays.asList(1,2,3)));
        iterateSmart(new LinkedList<>(Arrays.asList(1,2,3)));
    }
    private static <T> void iterateSmart(List<T> list) {
        if (list instanceof RandomAccess) {
            for (int i = 0; i < list.size(); i++) { /* index-based */ }
            line("Used index-based loop (RandomAccess): " + list.getClass().getSimpleName());
        } else {
            for (Iterator<T> it = list.iterator(); it.hasNext(); ) { it.next(); }
            line("Used iterator loop (no RandomAccess): " + list.getClass().getSimpleName());
        }
    }

    // 22) Sorting stability (ordering vs sorting)
    // Q: Collections.sort / List.sort are stable (TimSort). Stable means equal keys preserve input order.
    private static void orderingVsSortingAndStability() {
        List<Member> members = new ArrayList<>(Arrays.asList(
                new Member("B", "Ann"),
                new Member("A", "Zoe"),
                new Member("A", "Bob"),
                new Member("B", "Cam")
        ));
        members.sort(Comparator.comparing(m -> m.group)); // stable
        line("Stable sort by group preserves order within equal keys: " + members);
    }

    // 23) Notes only (no runtime)
    private static void notesCommentsOnly() {
        // Big-O (typical, amortized/avg):
        // - ArrayList: add O(1) amortized, get O(1), remove by index O(n)
        // - LinkedList: add/remove at ends O(1), random access O(n)
        // - HashSet/HashMap: add/get/remove O(1) avg; worst O(n), tree bin O(log n)
        // - TreeSet/TreeMap: O(log n) for add/get/remove
        // Streams vs Collections: Stream is data pipeline (functional, one-shot), Collection is in-memory container.
        // Spliterator: supports parallel traversal; characteristics like SIZED, ORDERED, SORTED, CONCURRENT.
        line("See comments: Big-O, Streams vs Collections, Spliterator characteristics.");
    }

    // Helpers
    private static void title(String s) { System.out.println("\n=== " + s + " ==="); }
    private static void line(String s) { System.out.println(s); }
    private static void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }

    // Types for demos
    static final class BadHash {
        final int id;
        BadHash(int id) { this.id = id; }
        @Override public int hashCode() { return 1; } // force collision
        @Override public boolean equals(Object o) { return (o instanceof BadHash) && ((BadHash)o).id == id; }
        @Override public String toString() { return "BadHash(" + id + ")"; }
    }

    static final class PersonBad {
        final int id; final String name;
        PersonBad(int id, String name) { this.id = id; this.name = name; }
        @Override public boolean equals(Object o) { return (o instanceof PersonBad) && ((PersonBad)o).id == id && Objects.equals(name, ((PersonBad)o).name); }
        // hashCode not overridden -> violates equals/hashCode contract
    }

    static final class PersonGood {
        final int id; final String name;
        PersonGood(int id, String name) { this.id = id; this.name = name; }
        @Override public boolean equals(Object o) { return (o instanceof PersonGood) && ((PersonGood)o).id == id && Objects.equals(name, ((PersonGood)o).name); }
        @Override public int hashCode() { return Objects.hash(id, name); }
    }

    static final class MutableKey {
        int id; String name;
        MutableKey(int id, String name) { this.id = id; this.name = name; }
        @Override public boolean equals(Object o) { return (o instanceof MutableKey) && ((MutableKey)o).id == id && Objects.equals(name, ((MutableKey)o).name); }
        @Override public int hashCode() { return Objects.hash(id, name); }
        @Override public String toString() { return "MutableKey(" + id + "," + name + ")"; }
    }

    static final class LRUCache<K,V> extends LinkedHashMap<K,V> {
        private final int capacity;
        LRUCache(int capacity) { super(capacity, 0.75f, true); this.capacity = capacity; }
        @Override protected boolean removeEldestEntry(Map.Entry<K,V> eldest) { return size() > capacity; }
    }

    static final class Member {
        final String group, name;
        Member(String group, String name) { this.group = group; this.name = name; }
        @Override public String toString() { return group + ":" + name; }
    }

    enum Day { MON, TUE, WED, THU, FRI, SAT, SUN }
}