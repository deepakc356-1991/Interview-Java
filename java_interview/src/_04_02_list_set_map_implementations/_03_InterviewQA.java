package _04_02_list_set_map_implementations;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;
import java.util.stream.Collectors;

/*
 Q: What does this file contain?
 A: A practical, runnable interview "cheat sheet" for Java List, Set, and Map implementations (basic → advanced),
    with Q&A as comments and code demos showing behavior, performance notes, pitfalls, and best practices.

 How to run:
 - Compile and run. Each demo prints concise output for the concept it explains.
*/

public class _03_InterviewQA {
    public static void main(String[] args) throws Exception {
        demo01_Basics_List_Set_Map();
        demo02_List_Implementations();
        demo03_Set_Implementations();
        demo04_Map_Implementations();
        demo05_Equals_HashCode_Importance();
        demo06_TreeSet_Comparator_Defines_Equality();
        demo07_Null_Handling_Across_Collections();
        demo08_FailFast_Iterators();
        demo09_WeaklyConsistent_Iterators();
        demo10_Remove_While_Iterating_Correctly();
        demo11_SubList_Is_A_View();
        demo12_Unmodifiable_Views_vs_Immutable_Collections();
        demo13_LinkedHashMap_As_LRU_Cache();
        demo14_Map_Compute_Merge_APIs();
        demo15_ConcurrentHashMap_ComputeIfAbsent_Is_Atomic();
        demo16_IdentityHashMap_Uses_Reference_Equality();
        demo17_EnumSet_EnumMap_Are_Specialized();
        demo18_Remove_Duplicates_Preserve_Order();
        demo19_Stable_Sorting_Demo();
        demo20_ArraysAsList_Gotchas();

        // Quick selection guideline (read comments only)
        // see method: chooseImplementationGuide (not invoked)
    }

    private static void demo15_ConcurrentHashMap_ComputeIfAbsent_Is_Atomic() {

    }

    private static void demo19_Stable_Sorting_Demo() {
    }

    // Q: Core conceptual difference between List, Set, and Map?
    // A:
    // - List: ordered, index-based, allows duplicates, allows null (most impls).
    // - Set: no duplicates (based on equals/hashCode or Comparator), may have at most one null (HashSet/LinkedHashSet).
    // - Map: key→value association; keys unique, values can duplicate; null support varies by implementation.
    private static void demo01_Basics_List_Set_Map() {
        System.out.println("\n--- demo01: Basics List vs Set vs Map ---");
        List<String> list = new ArrayList<>();
        list.add("A"); list.add("A"); // duplicates allowed
        System.out.println("List (duplicates allowed): " + list);

        Set<String> set = new HashSet<>();
        set.add("A"); set.add("A"); // duplicate ignored
        System.out.println("Set (no duplicates): " + set);

        Map<String, Integer> map = new HashMap<>();
        map.put("A", 1);
        map.put("A", 2); // overwrites same key
        System.out.println("Map (key unique, values overwrite): " + map);
    }

    // Q: When to use ArrayList vs LinkedList vs Vector vs Stack vs CopyOnWriteArrayList?
    // A:
    // - ArrayList: random access O(1), append amortized O(1), insert/remove middle O(n). Most common.
    // - LinkedList: insert/remove at ends O(1), random access O(n). Rarely beats ArrayList in practice.
    // - Vector: legacy synchronized ArrayList. Prefer ArrayList or thread-safe alternatives.
    // - Stack: legacy LIFO (extends Vector). Prefer Deque (ArrayDeque) for stack/queue.
    // - CopyOnWriteArrayList: iteration w/o locking, writes copy the array; great for mostly-read scenarios.
    private static void demo02_List_Implementations() {
        System.out.println("\n--- demo02: List Implementations ---");
        List<Integer> arrayList = new ArrayList<>(List.of(1,2,3));
        arrayList.add(0, 99);
        System.out.println("ArrayList (indexable): " + arrayList + " (get(2)="+arrayList.get(2)+")");

        Deque<Integer> dequeAsStack = new ArrayDeque<>();
        dequeAsStack.push(1); dequeAsStack.push(2); dequeAsStack.push(3);
        System.out.println("Deque as Stack (LIFO): " + dequeAsStack + " pop=" + dequeAsStack.pop());

        List<Integer> cowList = new CopyOnWriteArrayList<>(List.of(1,2,3));
        for (Integer i : cowList) { if (i == 2) cowList.add(4); }
        System.out.println("CopyOnWriteArrayList iteration doesn't throw; snapshot seen: " + cowList);
    }

    // Q: Compare Set implementations: HashSet, LinkedHashSet, TreeSet, EnumSet, CopyOnWriteArraySet, ConcurrentSkipListSet?
    // A:
    // - HashSet: hash table, O(1) average, no order guarantee, allows one null.
    // - LinkedHashSet: HashSet + insertion order.
    // - TreeSet: sorted by natural/comparator; O(log n); no null (natural order).
    // - EnumSet: extremely fast, memory-efficient for enums (bit vectors).
    // - CopyOnWriteArraySet: for mostly-read concurrency.
    // - ConcurrentSkipListSet: thread-safe, sorted, scalable concurrency.
    private static void demo03_Set_Implementations() {
        System.out.println("\n--- demo03: Set Implementations ---");
        Set<String> hash = new HashSet<>(List.of("b","a","c","a"));
        System.out.println("HashSet (unordered, unique): " + hash);

        Set<String> linked = new LinkedHashSet<>(List.of("b","a","c","a"));
        System.out.println("LinkedHashSet (insertion-ordered): " + linked);

        Set<String> tree = new TreeSet<>(Comparator.reverseOrder());
        tree.addAll(List.of("b","a","c"));
        System.out.println("TreeSet (sorted desc): " + tree);

        EnumSet<Day> es = EnumSet.of(Day.MON, Day.WED, Day.FRI);
        System.out.println("EnumSet (fast enum set): " + es);

        Set<String> cwas = new CopyOnWriteArraySet<>(List.of("x","y","x"));
        System.out.println("CopyOnWriteArraySet (unique, snapshot semantics): " + cwas);

        Set<Integer> csl = new ConcurrentSkipListSet<>();
        csl.addAll(List.of(3,1,2));
        System.out.println("ConcurrentSkipListSet (sorted, concurrent): " + csl);
    }

    // Q: Compare Map implementations: HashMap, LinkedHashMap, TreeMap, Hashtable, ConcurrentHashMap, WeakHashMap, IdentityHashMap, EnumMap?
    // A:
    // - HashMap: O(1) avg, no order, allows one null key and many null values.
    // - LinkedHashMap: preserves insertion or access order; good for caches.
    // - TreeMap: sorted by key; O(log n); no null key in natural ordering; allows null values.
    // - Hashtable: legacy synchronized, no null keys/values; prefer CHM.
    // - ConcurrentHashMap: high-performance concurrent map; no null keys/values.
    // - WeakHashMap: keys are weakly referenced; entries vanish when keys are GC'd.
    // - IdentityHashMap: keys compared by == (reference equality) rather than equals.
    // - EnumMap: high-performance map with enum keys.
    private static void demo04_Map_Implementations() throws Exception {
        System.out.println("\n--- demo04: Map Implementations ---");
        Map<String,Integer> hm = new HashMap<>();
        hm.put(null, 1); hm.put("a", 1); hm.put("a", 2);
        System.out.println("HashMap (null key ok, overwrite): " + hm);

        Map<String,Integer> lhm = new LinkedHashMap<>();
        lhm.put("b",2); lhm.put("a",1); lhm.put("c",3);
        System.out.println("LinkedHashMap (insertion order): " + lhm);

        Map<String,Integer> tm = new TreeMap<>();
        tm.put("b",2); tm.put("a",1); tm.put("c",3);
        System.out.println("TreeMap (sorted): " + tm);

        Map<String,Integer> chm = new ConcurrentHashMap<>();
        chm.put("a",1); chm.put("b",2);
        System.out.println("ConcurrentHashMap (no nulls): " + chm);

        Map<Object,String> ihm = new IdentityHashMap<>();
        String k1 = new String("k"); String k2 = new String("k");
        ihm.put(k1, "v1"); ihm.put(k2, "v2");
        System.out.println("IdentityHashMap (reference equality, size=2): " + ihm);

        Map<Day,String> em = new EnumMap<>(Day.class);
        em.put(Day.MON,"a"); em.put(Day.TUE,"b");
        System.out.println("EnumMap (enum keys): " + em);

        Map<Object,String> whm = new WeakHashMap<>();
        Object wk = new Object();
        whm.put(wk, "alive");
        System.out.println("WeakHashMap before GC: " + whm);
        wk = null;
        forceGC();
        System.out.println("WeakHashMap after GC (entry may be gone): " + whm);
    }

    // Q: Why must equals and hashCode be consistent for keys/elements?
    // A: Hash-based collections rely on these to detect duplicates and locate entries.
    private static void demo05_Equals_HashCode_Importance() {
        System.out.println("\n--- demo05: equals/hashCode importance ---");
        // Proper equals/hashCode => duplicates removed in HashSet
        Set<Person> people = new HashSet<>();
        people.add(new Person("Ada","Lovelace",36));
        people.add(new Person("Ada","Lovelace",36)); // same content => duplicate
        System.out.println("HashSet<Person> (proper equals/hashCode) size= " + people.size());

        // Missing equals/hashCode on keys => lookup fails
        Map<NoEqKey, String> map = new HashMap<>();
        NoEqKey k1 = new NoEqKey("42");
        NoEqKey k2 = new NoEqKey("42"); // logically equal, different instance
        map.put(k1, "value");
        System.out.println("Lookup with logically equal but different instance (NoEqKey): " + map.get(k2)); // null

        // Terrible hashCode => performance degrades (collisions)
        Set<BadHashPerson> bad = new HashSet<>();
        for (int i = 0; i < 5; i++) bad.add(new BadHashPerson("P"+i));
        System.out.println("Bad hashCode => many collisions (still works, slower at scale), size=" + bad.size());
    }

    // Q: In TreeSet/TreeMap, does Comparator define "equality"?
    // A: Yes. If comparator.compare(a,b)==0, they are considered duplicates.
    private static void demo06_TreeSet_Comparator_Defines_Equality() {
        System.out.println("\n--- demo06: TreeSet comparator defines equality ---");
        Comparator<Person> byLastNameOnly = Comparator.comparing(p -> p.last);
        Set<Person> set = new TreeSet<>(byLastNameOnly);
        set.add(new Person("Alan","Turing",41));
        set.add(new Person("Alice","Turing",30)); // same last name => considered duplicate
        System.out.println("TreeSet<Person> by last name only, size=" + set.size() + " => " + set);
    }

    // Q: Null handling differences?
    // A:
    // - HashMap/LinkedHashMap: 1 null key allowed; HashSet/LinkedHashSet: 1 null element allowed.
    // - TreeMap/TreeSet (natural order): null key/element not allowed (NPE).
    // - ConcurrentHashMap/Hashtable: no null keys/values.
    private static void demo07_Null_Handling_Across_Collections() {
        System.out.println("\n--- demo07: Null handling ---");
        Map<String,Integer> hm = new HashMap<>();
        hm.put(null, 1);
        System.out.println("HashMap allows null key: " + hm);

        try {
            Map<String,Integer> tm = new TreeMap<>();
            tm.put(null, 1); // NPE
        } catch (NullPointerException e) {
            System.out.println("TreeMap null key => NPE");
        }

        try {
            Map<String,Integer> chm = new ConcurrentHashMap<>();
            chm.put(null, 1); // NPE
        } catch (NullPointerException e) {
            System.out.println("ConcurrentHashMap null key => NPE");
        }

        Set<String> hs = new HashSet<>();
        hs.add(null);
        System.out.println("HashSet allows single null element: " + hs);

        try {
            Set<String> ts = new TreeSet<>();
            ts.add(null); // NPE
        } catch (NullPointerException e) {
            System.out.println("TreeSet null element => NPE");
        }
    }

    // Q: What are fail-fast iterators?
    // A: Iterators over most non-concurrent collections detect structural modification and throw ConcurrentModificationException.
    private static void demo08_FailFast_Iterators() {
        System.out.println("\n--- demo08: Fail-fast iterators ---");
        List<Integer> list = new ArrayList<>(List.of(1,2,3));
        try {
            for (Integer i : list) {
                if (i == 2) list.add(99); // structural change during iteration => CME
            }
        } catch (ConcurrentModificationException e) {
            System.out.println("ArrayList iterator threw ConcurrentModificationException (as expected).");
        }
    }

    // Q: What are weakly-consistent (often called 'fail-safe') iterators?
    // A: Iterators that do not throw CME and may or may not reflect concurrent modifications.
    private static void demo09_WeaklyConsistent_Iterators() {
        System.out.println("\n--- demo09: Weakly consistent iterators ---");
        CopyOnWriteArrayList<Integer> cow = new CopyOnWriteArrayList<>(List.of(1,2,3));
        for (Integer i : cow) {
            if (i == 2) cow.add(99); // iterator reads snapshot, no exception
        }
        System.out.println("COW list after loop (mod seen by list, not by iterator snapshot): " + cow);

        ConcurrentHashMap<String,Integer> chm = new ConcurrentHashMap<>();
        chm.put("a",1); chm.put("b",2);
        for (String k : chm.keySet()) {
            if (k.equals("a")) chm.put("c",3); // iterator is weakly consistent, no exception
        }
        System.out.println("ConcurrentHashMap after iteration: " + chm);
    }

    // Q: How to remove while iterating (correctly)?
    // A: Use Iterator.remove() or removeIf(...) outside iteration logic. Don't modify underlying collection directly in loop.
    private static void demo10_Remove_While_Iterating_Correctly() {
        System.out.println("\n--- demo10: Remove while iterating ---");
        List<Integer> list = new ArrayList<>(List.of(1,2,3,4,5));
        for (Iterator<Integer> it = list.iterator(); it.hasNext(); ) {
            if (it.next() % 2 == 0) it.remove();
        }
        System.out.println("Removed evens via Iterator.remove(): " + list);

        Set<Integer> set = new HashSet<>(List.of(1,2,3,4,5));
        set.removeIf(n -> n % 2 == 0);
        System.out.println("Removed evens via removeIf(): " + set);
    }

    // Q: Is subList a view or a copy?
    // A: A view. Structural modification of parent invalidates subList and vice-versa (modCount must match).
    private static void demo11_SubList_Is_A_View() {
        System.out.println("\n--- demo11: subList is a view ---");
        List<Integer> base = new ArrayList<>(List.of(1,2,3,4,5));
        List<Integer> sub = base.subList(1, 4); // [2,3,4]
        System.out.println("subList initially: " + sub);
        base.add(99);
        try {
            sub.size(); // triggers check for concurrent modification
            System.out.println("Should not reach here");
        } catch (ConcurrentModificationException e) {
            System.out.println("subList became invalid after parent modified (CME).");
        }
    }

    // Q: Difference: Collections.unmodifiableXxx vs List.of/Set.of/Map.of?
    // A:
    // - unmodifiableXxx: a read-only view over a mutable collection (backed by it).
    // - Xxx.of: truly immutable collection (no backing changes, no nulls allowed).
    // - Arrays.asList: fixed-size, backed by array; set allowed, add/remove not.
    private static void demo12_Unmodifiable_Views_vs_Immutable_Collections() {
        System.out.println("\n--- demo12: Unmodifiable view vs immutable factory ---");
        List<String> mutable = new ArrayList<>(List.of("a","b"));
        List<String> unmodView = Collections.unmodifiableList(mutable);
        mutable.add("c"); // reflected in the view
        System.out.println("Unmodifiable view reflects backing changes: " + unmodView);
        try {
            unmodView.add("x");
        } catch (UnsupportedOperationException e) {
            System.out.println("Unmodifiable view blocks mutation: UOE");
        }

        List<String> imm = List.of("x","y");
        System.out.println("Immutable List.of: " + imm);
        try {
            imm.set(0, "z");
        } catch (UnsupportedOperationException e) {
            System.out.println("Immutable List.of blocks modification: UOE");
        }

        List<String> fixed = Arrays.asList("p","q");
        fixed.set(0, "r"); // ok
        try {
            fixed.add("s"); // UOE
        } catch (UnsupportedOperationException e) {
            System.out.println("Arrays.asList is fixed-size; add/remove => UOE");
        }
    }

    // Q: How to implement LRU with LinkedHashMap?
    // A: Use accessOrder=true and override removeEldestEntry.
    private static void demo13_LinkedHashMap_As_LRU_Cache() {
        System.out.println("\n--- demo13: LinkedHashMap as LRU ---");
        LRUCache<String,Integer> cache = new LRUCache<>(3);
        cache.put("A",1); cache.put("B",2); cache.put("C",3);
        cache.get("A"); // touch A to make it recent
        cache.put("D",4); // evicts least-recent (B)
        System.out.println("LRU keys after access A then put D: " + cache.keySet());
    }

    // Q: When to use computeIfAbsent/compute/merge/getOrDefault/putIfAbsent?
    // A:
    // - computeIfAbsent(k,f): atomically create value if missing (no call if present).
    // - compute(k,f): recompute even if exists; returning null removes mapping.
    // - merge(k,v,f): combine existing and new values; useful for counters, concatenation.
    // - getOrDefault(k,def): read-only default; doesn't insert.
    // - putIfAbsent(k,v): insert only if missing (returns existing if present).
    private static void demo14_Map_Compute_Merge_APIs() {
        System.out.println("\n--- demo14: Map compute/merge APIs ---");

        Map<String, List<Integer>> map = new HashMap<>();
        map.computeIfAbsent("nums", k -> new ArrayList<>()).add(1);
        map.computeIfAbsent("nums", k -> new ArrayList<>()).add(2);
        System.out.println("computeIfAbsent list aggregation: " + map);

        Map<String,Integer> counts = new HashMap<>();
        for (String w : List.of("a","b","a","c","b","a")) {
            counts.merge(w, 1, Integer::sum); // counting
        }
        System.out.println("merge for counting: " + counts);

        Map<String,String> m = new HashMap<>();
        m.put("x","X");
        m.compute("x", (k,v) -> null); // removes
        System.out.println("compute returning null removes mapping: " + m);

        Map<String,String> n = new HashMap<>();
        String r = n.getOrDefault("missing", "default");
        System.out.println("getOrDefault doesn't insert: present? " + n.containsKey("missing") + ", result=" + r);
    }

    // Q: Is ConcurrentHashMap.computeIfAbsent atomic under concurrency?
    // A: Yes. The mapping function will be invoked at most once per missing key.
    private static void demo15_ConcurrentHashMap_ComputeIfAbsent() throws Exception {
        System.out.println("\n--- demo15: CHM computeIfAbsent is atomic ---");
        ConcurrentHashMap<String,String> chm = new ConcurrentHashMap<>();
        AtomicInteger computes = new AtomicInteger(0);
        ExecutorService pool = Executors.newFixedThreadPool(8);
        int tasks = 16;
        CountDownLatch latch = new CountDownLatch(1);
        for (int i = 0; i < tasks; i++) {
            pool.submit(() -> {
                await(latch);
                chm.computeIfAbsent("key", k -> {
                    computes.incrementAndGet();
                    sleep(50);
                    return "value";
                });
            });
        }
        latch.countDown();
        pool.shutdown();
        pool.awaitTermination(2, TimeUnit.SECONDS);
        System.out.println("computeIfAbsent invoked times = " + computes.get() + ", map=" + chm);
    }

    // Q: What is IdentityHashMap?
    // A: Uses reference equality (==) for keys; two equal() keys map to different entries if different instances.
    private static void demo16_IdentityHashMap_Uses_Reference_Equality() {
        System.out.println("\n--- demo16: IdentityHashMap reference equality ---");
        IdentityHashMap<String,Integer> ihm = new IdentityHashMap<>();
        String a1 = new String("a");
        String a2 = new String("a");
        ihm.put(a1,1);
        ihm.put(a2,2);
        System.out.println("IdentityHashMap size (same contents, different refs): " + ihm.size());
        System.out.println("Get with literal \"a\" (likely distinct ref): " + ihm.get("a"));
    }

    // Q: Why EnumSet/EnumMap?
    // A: Extremely compact and fast for enum keys; preserve natural order of enum constants.
    private static void demo17_EnumSet_EnumMap_Are_Specialized() {
        System.out.println("\n--- demo17: EnumSet & EnumMap ---");
        EnumSet<Day> workdays = EnumSet.range(Day.MON, Day.FRI);
        System.out.println("Workdays EnumSet: " + workdays);

        EnumMap<Day, String> schedule = new EnumMap<>(Day.class);
        schedule.put(Day.MON,"Gym");
        schedule.put(Day.FRI,"Party");
        System.out.println("EnumMap schedule: " + schedule);
    }

    // Q: Remove duplicates from a List while preserving order?
    // A: Use LinkedHashSet (preserves insertion order).
    private static void demo18_Remove_Duplicates_Preserve_Order() {
        System.out.println("\n--- demo18: Remove duplicates preserve order ---");
        List<String> input = List.of("b","a","b","c","a","d");
        List<String> unique = new ArrayList<>(new LinkedHashSet<>(input));
        System.out.println("Unique preserving order: " + unique);
    }

    // Q: Is sort in Java stable?
    // A: Yes (TimSort). Elements that compare equal keep their original relative order.
    private static void demo19_Stable_Sorting() {
        System.out.println("\n--- demo19: Stable sorting ---");
        List<Labelled> list = new ArrayList<>(List.of(
            new Labelled(10, "first-A"),
            new Labelled(20, "B"),
            new Labelled(10, "second-A")
        ));
        list.sort(Comparator.comparingInt(l -> l.key)); // comparator considers key only
        System.out.println("Stable sort keeps equal keys' relative order: " + list);
    }

    // Q: Arrays.asList pitfalls?
    // A: It returns a fixed-size list backed by the array. set works; add/remove throw UOE; array and list backed each other.
    private static void demo20_ArraysAsList_Gotchas() {
        System.out.println("\n--- demo20: Arrays.asList gotchas ---");
        String[] arr = {"x","y"};
        List<String> fixed = Arrays.asList(arr);
        fixed.set(0, "z"); // reflected in arr
        System.out.println("Arrays.asList reflects to array: arr[0]=" + arr[0]);
        try {
            fixed.add("boom");
        } catch (UnsupportedOperationException e) {
            System.out.println("Arrays.asList is fixed-size: add/remove => UOE");
        }
    }

    // Q: How do ArrayList and HashMap resize?
    // A (notes):
    // - ArrayList grows ~1.5x when needed (ensureCapacity can reduce resizing).
    // - HashMap default capacity 16, load factor 0.75 => resize when size > 12; Java 8 treeifies bins under heavy collision.
    // - Set load factors mirror their backing map (HashSet uses HashMap internally).
    @SuppressWarnings("unused")
    private static void additionalNotesOnResizingAndPerformance() {
        // This is a comment-only section. No runtime demo necessary.
    }

    // Q: How to choose?
    // A (notes):
    // - List: ArrayList for most cases; LinkedList for frequent head insert/removal; CopyOnWriteArrayList for mostly-read concurrency.
    // - Set: HashSet by default; LinkedHashSet to preserve insertion order; TreeSet for sorted; EnumSet for enums; CopyOnWrite/SkipList for concurrency.
    // - Map: HashMap by default; LinkedHashMap for order/LRU; TreeMap for sorted; EnumMap for enums; ConcurrentHashMap for concurrency; WeakHashMap for caches with GC-managed keys; IdentityHashMap for identity semantics.
    @SuppressWarnings("unused")
    private static void chooseImplementationGuide() {
        // Read-only guidance in comments.
    }

    // Helpers and models

    private static void forceGC() throws InterruptedException {
        for (int i = 0; i < 3; i++) {
            System.gc();
            Thread.sleep(50);
        }
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private static void await(CountDownLatch latch) {
        try { latch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    enum Day { MON, TUE, WED, THU, FRI, SAT, SUN }

    static final class Person {
        final String first;
        final String last;
        final int age;
        Person(String first, String last, int age) {
            this.first = first; this.last = last; this.age = age;
        }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Person p)) return false;
            return age == p.age && Objects.equals(first, p.first) && Objects.equals(last, p.last);
        }
        @Override public int hashCode() {
            return Objects.hash(first, last, age);
        }
        @Override public String toString() { return first + " " + last + "(" + age + ")"; }
    }

    static final class NoEqKey {
        final String id;
        NoEqKey(String id) { this.id = id; }
        @Override public String toString() { return "NoEqKey(" + id + ")"; }
        // equals/hashCode NOT overridden on purpose
    }

    static final class BadHashPerson {
        final String name;
        BadHashPerson(String name) { this.name = name; }
        @Override public int hashCode() { return 1; } // worst-case collision
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BadHashPerson b)) return false;
            return Objects.equals(name, b.name);
        }
        @Override public String toString() { return name; }
    }

    static final class LRUCache<K,V> extends LinkedHashMap<K,V> {
        private final int maxSize;
        LRUCache(int maxSize) {
            super(16, 0.75f, true); // access-order
            this.maxSize = maxSize;
        }
        @Override protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
            return size() > maxSize;
        }
    }

    static final class Labelled {
        final int key;
        final String label;
        Labelled(int key, String label) { this.key = key; this.label = label; }
        @Override public String toString() { return "(" + key + "," + label + ")"; }
    }
}