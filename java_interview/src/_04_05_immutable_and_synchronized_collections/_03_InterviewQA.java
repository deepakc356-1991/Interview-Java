package _04_05_immutable_and_synchronized_collections;

import java.io.Serializable;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/**
 * Immutable & Synchronized Collections — Interview Q&A (Basic → Advanced)
 *
 * Read this file top-to-bottom. It contains:
 * - Q&A in comments
 * - Minimal runnable examples (main method)
 * - Pitfalls and best practices
 *
 * Tested on Java 17+.
 */
public class _03_InterviewQA {

    public static void main(String[] args) {
        // Minimal runtime to keep console clean; focus is in source comments and examples.
        System.out.println("Immutable & Synchronized Collections — Q&A demo running...");

        basics_immutable_vs_unmodifiable();
        creating_immutable_collections();
        unmodifiable_view_is_shallow_and_backed();
        defensive_copies_and_safe_publication();
        synchronized_wrappers_basics_and_iteration_locking();
        concurrent_vs_synchronized_collections();
        fail_fast_vs_weakly_consistent_iterators();
        null_handling_differences();
        copyOf_factory_methods_snapshot_vs_view();
        copy_on_write_use_cases();
        immutable_class_design_beyond_collections();
        atomic_compound_ops();
        sorted_and_navigable_wrappers();
        vector_vs_synchronizedList();
        subList_and_views_pitfalls();
        serialization_notes();

        System.out.println("Done.");
    }

    // ------------------------------------------------------------
    // Basic: Immutable vs Unmodifiable vs Read-only vs Synchronized
    // ------------------------------------------------------------

    /**
     * Q: Difference between immutable, unmodifiable (read-only) and synchronized collections?
     *
     * A:
     * - Immutable: State cannot change after construction. No structural changes, no element changes (assuming elements are themselves immutable).
     *   Safe to share across threads without external synchronization. Example: List.of(), Set.of(), Map.of() (Java 9+).
     *
     * - Unmodifiable view: A wrapper that forbids modification through that reference but may reflect changes to the underlying collection.
     *   Not inherently thread-safe. Example: Collections.unmodifiableList(backingList).
     *
     * - Synchronized (thread-safe) view: A wrapper that serializes access using a single intrinsic lock. Does not make it immutable.
     *   Example: Collections.synchronizedList(list). You must manually synchronize when iterating.
     *
     * Rule of thumb:
     * - If you never need to mutate: prefer truly immutable collections (List.of, Set.of, Map.of, List.copyOf, …).
     * - If you need thread-safe mutability: prefer concurrent collections (ConcurrentHashMap, CopyOnWriteArrayList, …) over synchronized wrappers.
     * - If you just need to prevent accidental modification by callers: return an unmodifiable copy/view (but understand the shallow and backed aspects).
     */
    private static void basics_immutable_vs_unmodifiable() {
        // Immutable examples:
        List<String> immutableList = List.of("A", "B"); // throws on add/remove, safe to share across threads.

        // Unmodifiable view (backed by original):
        List<String> modifiable = new ArrayList<>(List.of("X", "Y"));
        List<String> readOnlyView = Collections.unmodifiableList(modifiable); // view, not a copy

        // Synchronized wrapper:
        List<String> syncList = Collections.synchronizedList(new ArrayList<>());

        // Quick checks (no exceptions thrown here):
        boolean isImmutableSerializable = (immutableList instanceof Serializable);
        boolean isReadOnlySerializable = (readOnlyView instanceof Serializable);
        boolean isSyncSerializable = (syncList instanceof Serializable); // depends on underlying list; ArrayList is Serializable

        // No printing needed; comments explain semantics.
        assert isImmutableSerializable && isReadOnlySerializable && isSyncSerializable;
    }

    // ------------------------------------------------------------
    // Creating Immutable Collections (Java 9+ and alternatives)
    // ------------------------------------------------------------

    /**
     * Q: How to create immutable collections in Java?
     *
     * A:
     * - Java 9+: List.of, Set.of, Map.of, Map.ofEntries (no nulls allowed, truly immutable).
     * - Java 10+: List.copyOf, Set.copyOf, Map.copyOf produce immutable copies (nulls not allowed).
     * - Pre-Java 9: Use Collections.unmodifiableList(new ArrayList<>(source)) for a snapshot-like unmodifiable result (still shallow).
     */
    private static void creating_immutable_collections() {
        List<String> l1 = List.of("a", "b", "c");
        Set<Integer> s1 = Set.of(1, 2, 3);
        Map<String, Integer> m1 = Map.of("k1", 1, "k2", 2);

        Map<String, Integer> m2 = Map.ofEntries(
                Map.entry("x", 10),
                Map.entry("y", 20)
        );

        // copyOf returns the same instance if the input is already a JDK immutable collection,
        // otherwise it creates a new immutable copy (and disallows nulls).
        List<String> l2 = List.copyOf(l1);
        assert l2 == l1;

        // Pre-Java 9 style snapshot (still shallow and allows nulls if provided):
        List<String> legacySnapshot = Collections.unmodifiableList(new ArrayList<>(Arrays.asList("p", "q")));
        assert legacySnapshot.size() == 2;
    }

    // ------------------------------------------------------------
    // Unmodifiable views are shallow and backed
    // ------------------------------------------------------------

    /**
     * Q: Are unmodifiable collections shallow? Do they reflect backing mutations?
     *
     * A:
     * - They are shallow views and backed by the original collection.
     *   If the backing collection changes, the unmodifiable view reflects those changes.
     * - They cannot prevent mutation of the elements themselves if elements are mutable objects.
     */
    private static void unmodifiable_view_is_shallow_and_backed() {
        List<MutableUser> backing = new ArrayList<>(List.of(new MutableUser("Ann"), new MutableUser("Bob")));
        List<MutableUser> ro = Collections.unmodifiableList(backing);

        // Mutate the backing structure:
        backing.add(new MutableUser("Cody"));
        assert ro.size() == 3; // reflected in the view

        // Mutate an element:
        backing.get(0).setName("Anna"); // ro sees this change too because it's the same object

        // Attempting structural change via the view throws UnsupportedOperationException:
        boolean threw = false;
        try {
            ro.remove(0);
        } catch (UnsupportedOperationException e) {
            threw = true;
        }
        assert threw;
    }

    // ------------------------------------------------------------
    // Defensive Copies and Safe Publication
    // ------------------------------------------------------------

    /**
     * Q: How to safely expose internal collections to callers?
     *
     * A:
     * - Return immutable copies (List.copyOf) for snapshot semantics that cannot change behind the scenes.
     * - Or return unmodifiable views (Collections.unmodifiableList) if you accept that future internal mutations are reflected.
     * - For arrays, always return a copy (defensive copy).
     *
     * Q: Safe publication?
     * - Immutable objects with final fields are safely published once the constructor finishes.
     * - Unmodifiable views are not immutable — safe publication depends on the backing and concurrent mutations.
     */
    private static void defensive_copies_and_safe_publication() {
        // Defensive copy example in a class API:
        Team team = new Team(List.of("A", "B", "C")); // stores as unmodifiable snapshot
        List<String> roster = team.getPlayers(); // immutable snapshot
        assert roster.size() == 3;

        // Array defensive copy example:
        int[] a = {1, 2, 3};
        int[] copy = Team.defensiveCopy(a);
        a[0] = 99; // doesn't affect copy
        assert copy[0] == 1;
    }

    // ------------------------------------------------------------
    // Synchronized wrappers: basics and iteration locking
    // ------------------------------------------------------------

    /**
     * Q: What do Collections.synchronizedXxx wrappers do?
     *
     * A:
     * - They provide a thread-safe wrapper using a single intrinsic lock (per wrapped collection).
     * - They DO NOT make compound actions (like iteration + remove) atomic by default.
     * - When iterating, you MUST manually synchronize on the wrapper:
     *
     *   List<String> syncList = Collections.synchronizedList(new ArrayList<>());
     *   synchronized (syncList) {
     *       Iterator<String> it = syncList.iterator();
     *       while (it.hasNext()) {
     *           process(it.next());
     *       }
     *   }
     *
     * - For SortedMap/NavigableMap/SortedSet/NavigableSet, use the specific wrappers.
     */
    private static void synchronized_wrappers_basics_and_iteration_locking() {
        List<String> syncList = Collections.synchronizedList(new ArrayList<>());
        syncList.add("x");
        syncList.add("y");

        synchronized (syncList) {
            for (String s : syncList) {
                // iteration safely guarded by syncList's lock
            }
        }

        // The same applies to synchronizedMap, synchronizedSet, etc.
    }

    // ------------------------------------------------------------
    // Concurrent collections vs Synchronized wrappers
    // ------------------------------------------------------------

    /**
     * Q: How do concurrent collections differ from synchronized wrappers?
     *
     * A:
     * - Concurrent collections (ConcurrentHashMap, ConcurrentLinkedQueue, CopyOnWriteArrayList) are designed for high concurrency.
     *   They use fine-grained locking or lock-free algorithms and offer weakly consistent iterators.
     * - Synchronized wrappers serialize all access behind one lock; can become a bottleneck.
     * - Prefer concurrent collections for scalable concurrent apps.
     */
    private static void concurrent_vs_synchronized_collections() {
        Map<String, Integer> chm = new ConcurrentHashMap<>();
        chm.put("a", 1);
        chm.compute("a", (k, v) -> v == null ? 1 : v + 1); // atomic compound operation
        assert chm.get("a") == 2;

        // Synchronized wrapper — no atomic compound methods; must synchronize externally:
        Map<String, Integer> syncMap = Collections.synchronizedMap(new HashMap<>());
        synchronized (syncMap) {
            Integer v = syncMap.get("a");
            syncMap.put("a", v == null ? 1 : v + 1); // not atomic without the synchronized block
        }
    }

    // ------------------------------------------------------------
    // Iterators: fail-fast vs weakly consistent
    // ------------------------------------------------------------

    /**
     * Q: What are fail-fast and weakly consistent iterators?
     *
     * A:
     * - Fail-fast: Most java.util collections' iterators detect concurrent structural modification and throw ConcurrentModificationException.
     * - Weakly consistent: Iterators from concurrent collections (e.g., ConcurrentHashMap, CopyOnWriteArrayList) do not throw;
     *   they reflect some state of the collection during iteration and tolerate concurrent modifications.
     */
    private static void fail_fast_vs_weakly_consistent_iterators() {
        List<String> list = new ArrayList<>(List.of("a", "b", "c"));
        boolean threw = false;
        try {
            for (String s : list) {
                if ("b".equals(s)) list.add("d"); // structural modification during iteration => fail-fast
            }
        } catch (ConcurrentModificationException e) {
            threw = true;
        }
        assert threw;

        CopyOnWriteArrayList<String> cow = new CopyOnWriteArrayList<>(List.of("x", "y"));
        for (String s : cow) {
            cow.add("z"); // allowed; iterator iterates over a snapshot
        }
        assert cow.size() >= 3;
    }

    // ------------------------------------------------------------
    // Null handling differences
    // ------------------------------------------------------------

    /**
     * Q: Do immutable factory methods accept null?
     *
     * A:
     * - List.of/Set.of/Map.of/Map.ofEntries: No. Null elements/keys/values throw NullPointerException.
     * - Collections.unmodifiableXxx: They accept nulls if the backing collection contains nulls.
     * - ConcurrentHashMap: Disallows null keys/values.
     */
    private static void null_handling_differences() {
        boolean npeThrown = false;
        try {
            List.of("a", null); // throws NPE
        } catch (NullPointerException e) {
            npeThrown = true;
        }
        assert npeThrown;

        List<String> allowsNull = new ArrayList<>();
        allowsNull.add(null);
        List<String> ro = Collections.unmodifiableList(allowsNull); // allowed if backing has null
        assert ro.get(0) == null;
    }

    // ------------------------------------------------------------
    // copyOf snapshot vs unmodifiable view
    // ------------------------------------------------------------

    /**
     * Q: Difference between List.copyOf and Collections.unmodifiableList(backing)?
     *
     * A:
     * - List.copyOf creates an immutable snapshot (no backing updates are reflected). Nulls not allowed.
     * - Collections.unmodifiableList(backing) creates an unmodifiable view (backing updates are reflected). Nulls allowed if present.
     */
    private static void copyOf_factory_methods_snapshot_vs_view() {
        List<String> backing = new ArrayList<>(List.of("A"));
        List<String> snapshot = List.copyOf(backing);              // immutable snapshot
        List<String> view = Collections.unmodifiableList(backing); // unmodifiable view

        backing.add("B");
        assert snapshot.size() == 1; // unaffected
        assert view.size() == 2;     // reflects backing
    }

    // ------------------------------------------------------------
    // CopyOnWriteArrayList use cases
    // ------------------------------------------------------------

    /**
     * Q: When to use CopyOnWriteArrayList/CopyOnWriteArraySet?
     *
     * A:
     * - Many reads, very few writes.
     * - Iterators never throw ConcurrentModificationException and are snapshot-based.
     * - Each write copies the entire backing array (O(n)), so avoid when writes are frequent or elements are large.
     */
    private static void copy_on_write_use_cases() {
        CopyOnWriteArrayList<String> listeners = new CopyOnWriteArrayList<>();
        listeners.add("L1");
        for (String l : listeners) {
            // Safe to add/remove during iteration; the iterator is reading from a snapshot
            listeners.add("L2");
        }
        assert listeners.size() >= 2;
    }

    // ------------------------------------------------------------
    // Immutable class design (beyond collections)
    // ------------------------------------------------------------

    /**
     * Q: How to design a truly immutable class that contains collections?
     *
     * A:
     * - Declare class final or make constructor private with factory method.
     * - Make fields private and final.
     * - Store immutable copies (List.copyOf/Set.copyOf/Map.copyOf) of collections in fields.
     * - Defensively copy arrays in and out.
     * - Do not expose mutable internals; return immutable copies in getters.
     * - Ensure elements are immutable too if deep immutability is required.
     * - Immutability implies thread-safety and safe publication of final fields.
     */
    private static void immutable_class_design_beyond_collections() {
        ImmutableOrder order = new ImmutableOrder(
                "ORD-1",
                List.of("SKU-1", "SKU-2"),
                Map.of("SKU-1", 2, "SKU-2", 1),
                Instant.now()
        );

        // Accessors return immutable snapshots:
        boolean threw = false;
        try {
            order.skus().add("SKU-3");
        } catch (UnsupportedOperationException e) {
            threw = true;
        }
        assert threw;
    }

    // ------------------------------------------------------------
    // Atomic compound operations
    // ------------------------------------------------------------

    /**
     * Q: How to perform atomic compound operations on maps?
     *
     * A:
     * - ConcurrentHashMap provides compute/merge/computeIfAbsent atomically.
     * - Synchronized wrappers require external synchronization around the compound logic.
     */
    private static void atomic_compound_ops() {
        ConcurrentHashMap<String, Integer> chm = new ConcurrentHashMap<>();
        chm.merge("hits", 1, Integer::sum);    // atomic
        chm.computeIfAbsent("users", k -> 0);  // atomic
        assert chm.get("hits") == 1;

        Map<String, Integer> sync = Collections.synchronizedMap(new HashMap<>());
        synchronized (sync) {
            sync.put("hits", sync.getOrDefault("hits", 0) + 1);
        }
        assert sync.get("hits") == 1;
    }

    // ------------------------------------------------------------
    // Sorted and Navigable synchronized wrappers
    // ------------------------------------------------------------

    /**
     * Q: How to get synchronized wrappers for ordered collections?
     *
     * A:
     * - Collections.synchronizedSortedMap, synchronizedNavigableMap, synchronizedSortedSet, synchronizedNavigableSet.
     * - Same iteration rule: manually synchronize when iterating.
     */
    private static void sorted_and_navigable_wrappers() {
        SortedMap<Integer, String> sm = Collections.synchronizedSortedMap(new TreeMap<>());
        sm.put(2, "b");
        sm.put(1, "a");
        synchronized (sm) {
            for (Map.Entry<Integer, String> e : sm.entrySet()) {
                // safe iteration
            }
        }
    }

    // ------------------------------------------------------------
    // Vector vs Collections.synchronizedList
    // ------------------------------------------------------------

    /**
     * Q: Vector vs Collections.synchronizedList(new ArrayList<>())?
     *
     * A:
     * - Both are method-level synchronized; Vector is legacy.
     * - Prefer Collections.synchronizedList for modern code, or better, use concurrent collections when needed.
     * - Vector allows nulls; performance differences are generally negligible compared to design impact.
     */
    private static void vector_vs_synchronizedList() {
        Vector<String> vector = new Vector<>();
        vector.add("v");

        List<String> syncList = Collections.synchronizedList(new ArrayList<>());
        syncList.add("s");

        synchronized (syncList) {
            for (String s : syncList) {
                // safe
            }
        }
    }

    // ------------------------------------------------------------
    // subList and views pitfalls
    // ------------------------------------------------------------

    /**
     * Q: Pitfalls with subList and views?
     *
     * A:
     * - subList returns a view backed by the original list. Structural changes outside can invalidate iterators or cause CME.
     * - Wrapping a subList with unmodifiableList still leaves it as a view of a view; mutations to the root list reflect.
     * - For an independent slice, make a copy: new ArrayList<>(list.subList(...)) then possibly wrap as unmodifiable.
     */
    private static void subList_and_views_pitfalls() {
        List<Integer> base = new ArrayList<>(List.of(1, 2, 3, 4, 5));
        List<Integer> view = base.subList(1, 4); // [2, 3, 4]
        List<Integer> unmodView = Collections.unmodifiableList(view);

        base.add(6); // may affect subList internals and can lead to CME on further operations on the view in some scenarios
        // Safer independent slice:
        List<Integer> copySlice = Collections.unmodifiableList(new ArrayList<>(base.subList(1, 4)));
        assert copySlice.size() == 3;
    }

    // ------------------------------------------------------------
    // Serialization notes
    // ------------------------------------------------------------

    /**
     * Q: Are immutable/unmodifiable/synchronized collections Serializable?
     *
     * A:
     * - JDK 9+ immutable collections (List.of, Set.of, Map.of) are Serializable.
     * - Collections.unmodifiableXxx is Serializable if the backing collection is Serializable.
     * - Collections.synchronizedXxx is Serializable if the backing collection is Serializable.
     * - Concurrent collections (ConcurrentHashMap, CopyOnWriteArrayList) are Serializable.
     */
    private static void serialization_notes() {
        List<String> l = List.of("a");
        assert l instanceof Serializable;

        List<String> ro = Collections.unmodifiableList(new ArrayList<>(List.of("a")));
        assert ro instanceof Serializable;

        Map<String, String> chm = new ConcurrentHashMap<>();
        assert chm instanceof Serializable;
    }

    // ------------------------------------------------------------
    // Extra Advanced Notes (in comments)
    // ------------------------------------------------------------

    /**
     * Extra Advanced (read-only comments):
     *
     * - Deep vs shallow immutability:
     *   Even immutable collections are shallow: if elements are mutable, their state can change.
     *   Use immutable element types or defensive copying of elements for deep immutability.
     *
     * - Guava vs JDK:
     *   Guava’s ImmutableList/ImmutableSet predate Java 9; they disallow nulls and are persistent snapshots (not views).
     *   JDK List.copyOf/Set.copyOf/Map.copyOf are generally sufficient in modern Java.
     *
     * - Order guarantees:
     *   Set.of/Map.of do not guarantee iteration order. If you need order, use List.of or LinkedHashMap then wrap immutably.
     *
     * - Map.of limitations:
     *   Overloads exist up to 10 entries; use Map.ofEntries for more.
     *
     * - Publication and the Java Memory Model:
     *   Final fields of immutable objects are safely published after the constructor completes. Unmodifiable views don’t change this; if backing changes concurrently without proper synchronization, readers may observe racy states.
     *
     * - When to wrap immutable collections with synchronizedXxx?
     *   Rarely useful. Immutable collections are already thread-safe to read. Synchronization can be used to perform compound thread-safe sequences that involve multiple collections/operations, not for single read operations.
     *
     * - UnsupportedOperationException:
     *   Removing via iterator from immutable/unmodifiable collections throws UOE.
     *
     * - ConcurrentModificationException with unmodifiable views:
     *   They are still backed; if the backing is modified structurally while iterating through the unmodifiable view, fail-fast rules apply.
     *
     * - ConcurrentHashMap specifics:
     *   No null keys/values. Iterators are weakly consistent. Provides atomic methods like compute/merge/computeIfAbsent/replace.
     */
    @SuppressWarnings("unused")
    private static void advanced_notes_comment_only() {
    }

    // ============================================================
    // Helper classes and examples
    // ============================================================

    // Mutable element to demonstrate shallow immutability issues
    private static class MutableUser {
        private String name;

        MutableUser(String name) { this.name = name; }
        void setName(String name) { this.name = name; }
        @Override public String toString() { return "User(" + name + ")"; }
    }

    // Example of an immutable aggregate that contains collections
    public static final class ImmutableOrder {
        private final String orderId;
        private final List<String> skus;              // immutable copy
        private final Map<String, Integer> quantities; // immutable copy
        private final Instant createdAt;              // Instant is immutable

        public ImmutableOrder(String orderId, Collection<String> skus, Map<String, Integer> quantities, Instant createdAt) {
            this.orderId = Objects.requireNonNull(orderId);
            // copyOf -> immutable snapshot, no nulls allowed
            this.skus = List.copyOf(skus);
            this.quantities = Map.copyOf(quantities);
            this.createdAt = Objects.requireNonNull(createdAt); // Instant is immutable
        }

        public String orderId() { return orderId; }
        public List<String> skus() { return skus; }                 // safe to return; it's immutable
        public Map<String, Integer> quantities() { return quantities; } // safe to return; it's immutable
        public Instant createdAt() { return createdAt; }            // safe
    }

    // Example encapsulating defensive copies and safe publication
    static final class Team {
        private final List<String> players; // immutable snapshot

        Team(Collection<String> players) {
            // snapshot + immutability
            this.players = List.copyOf(players);
        }

        public List<String> getPlayers() {
            // already immutable; returning directly is safe
            return players;
        }

        // Defensive copy for arrays
        static int[] defensiveCopy(int[] src) {
            return Arrays.copyOf(src, src.length);
        }
    }
}