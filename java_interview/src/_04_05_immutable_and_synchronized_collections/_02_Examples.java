package _04_05_immutable_and_synchronized_collections;

import java.util.*;
import java.util.concurrent.*;

public class _02_Examples {
    public static void main(String[] args) throws InterruptedException {
        section("Unmodifiable view vs backing collection changes");
        immutable_UnmodifiableViewPitfall();

        section("Unmodifiable is shallow (element state may change)");
        immutable_ShallowUnmodifiable();

        section("Truly immutable by defensive copy + unmodifiable wrapper");
        immutable_TrulyImmutableViaDefensiveCopy();

        section("Arrays.asList is fixed-size and backed by array");
        immutable_ArraysAsListPitfalls();

        section("JDK 9+ factory methods (List.of/Set.of/Map.of) and copyOf");
        immutable_Jdk9FactoryMethods_Commented();

        section("Synchronized wrappers basics and iteration");
        synchronized_WrappersBasics();

        section("Composite operations require external synchronization");
        synchronized_CompositeOperations();

        section("Synchronized Map examples (including Sorted/Navigable)");
        synchronized_MapExamples();

        section("Alternatives: concurrent collections (no external sync)");
        alternatives_ConcurrentCollections();
    }

    private static void section(String title) {
        System.out.println("\n=== " + title + " ===");
    }

    // 1) Unmodifiable view backed by a mutable collection (changes in the backing are visible)
    private static void immutable_UnmodifiableViewPitfall() {
        List<String> mutable = new ArrayList<String>();
        mutable.add("A");
        mutable.add("B");

        List<String> unmodifiableView = Collections.unmodifiableList(mutable);
        System.out.println("unmodifiableView initially: " + unmodifiableView);

        try {
            unmodifiableView.add("C"); // throws
        } catch (UnsupportedOperationException e) {
            System.out.println("unmodifiableView.add -> UnsupportedOperationException (cannot mutate through the view)");
        }

        mutable.add("C"); // mutate the backing list
        System.out.println("After mutating backing list, unmodifiableView sees: " + unmodifiableView);
    }

    // 1b) Unmodifiable wrappers are shallow: element state can still change
    private static void immutable_ShallowUnmodifiable() {
        List<StringBuilder> list = new ArrayList<StringBuilder>();
        list.add(new StringBuilder("Hi"));
        List<StringBuilder> unmod = Collections.unmodifiableList(list);

        System.out.println("Before element mutation: " + unmod.get(0));
        // You cannot add/remove/replace elements via the wrapper, but you can mutate the element itself
        unmod.get(0).append(" there");
        System.out.println("After element mutation via element reference: " + unmod.get(0));

        try {
            unmod.set(0, new StringBuilder("Nope"));
        } catch (UnsupportedOperationException e) {
            System.out.println("unmod.set -> UnsupportedOperationException (cannot replace elements)");
        }
    }

    // 2) Truly immutable by defensive copy + unmodifiable wrapper (and not exposing the mutable reference)
    private static void immutable_TrulyImmutableViaDefensiveCopy() {
        List<String> source = new ArrayList<String>();
        source.add("X");
        source.add("Y");

        // Defensive copy + unmodifiable wrapper. Do not expose or retain the mutable reference.
        List<String> immutable = Collections.unmodifiableList(new ArrayList<String>(source));
        System.out.println("immutable copy: " + immutable);

        // Mutate the original source; immutable is unaffected.
        source.add("Z");
        System.out.println("source mutated: " + source);
        System.out.println("immutable remains: " + immutable);

        // Trying to mutate the 'immutable' list fails.
        try {
            immutable.set(0, "NOPE");
        } catch (UnsupportedOperationException e) {
            System.out.println("immutable.set -> UnsupportedOperationException");
        }
    }

    // 3) Arrays.asList pitfalls: fixed-size view, backed by the array
    private static void immutable_ArraysAsListPitfalls() {
        String[] array = new String[] { "P", "Q" };
        List<String> fixedSize = Arrays.asList(array);

        System.out.println("fixedSize from Arrays.asList: " + fixedSize);

        try {
            fixedSize.add("R"); // throws because it's fixed-size
        } catch (UnsupportedOperationException e) {
            System.out.println("fixedSize.add -> UnsupportedOperationException (fixed-size view)");
        }

        // Changes to the array reflect in the list and vice versa for set()
        array[0] = "CHANGED_IN_ARRAY";
        System.out.println("After array[0] change, fixedSize: " + fixedSize);

        fixedSize.set(1, "CHANGED_IN_LIST");
        System.out.println("After fixedSize.set, array[1]: " + Arrays.toString(array));

        // To get an independent mutable list, copy it:
        List<String> copy = new ArrayList<String>(fixedSize);
        copy.add("INDEPENDENT");
        System.out.println("Independent copy: " + copy);
        System.out.println("Original array unaffected: " + Arrays.toString(array));
    }

    // 4) JDK 9/10+ immutable factory methods and copyOf (commented for Java 8 compatibility)
    private static void immutable_Jdk9FactoryMethods_Commented() {
        System.out.println("The following code targets Java 9+/10+. It is commented out for Java 8 compatibility.");
        System.out.println("Uncomment and compile on Java 11+ to try.");

        // Java 9+ factory methods produce truly immutable collections:
        // List<String> immList = List.of("A", "B", "C");
        // Set<Integer> immSet = Set.of(1, 2, 3);
        // Map<String, Integer> immMap = Map.of("a", 1, "b", 2);

        // Java 10+ copyOf creates an unmodifiable copy. If the source is already unmodifiable, it may be returned as-is.
        // List<String> copyOfList = List.copyOf(Arrays.asList("x", "y"));
        // Set<String> copyOfSet = Set.copyOf(new HashSet<>(Arrays.asList("x", "y")));
        // Map<String, Integer> copyOfMap = Map.copyOf(Collections.singletonMap("k", 1));
    }

    // 5) Synchronized wrappers basics and safe iteration
    private static void synchronized_WrappersBasics() throws InterruptedException {
        List<Integer> syncList = Collections.synchronizedList(new ArrayList<Integer>());
        // Single operations are atomic w.r.t. other operations
        for (int i = 0; i < 5; i++) {
            syncList.add(i);
        }

        // Safe iteration requires holding the list's monitor
        synchronized (syncList) {
            System.out.print("Safe iteration over syncList: ");
            for (Integer n : syncList) {
                System.out.print(n + " ");
            }
            System.out.println();
        }

        // Demonstrate that failing to synchronize iteration is unsafe.
        // We'll start a thread that modifies the list while we iterate without synchronization.
        Thread t = new Thread(new Runnable() {
            @Override public void run() {
                for (int i = 5; i < 10; i++) {
                    syncList.add(i);
                    try { Thread.sleep(5); } catch (InterruptedException ignored) {}
                }
            }
        });
        t.start();

        try {
            // UNSAFE: iterating without synchronizing on syncList while another thread modifies it.
            // This may throw ConcurrentModificationException or print inconsistent data.
            System.out.print("UNSAFE iteration (may throw CME): ");
            for (Integer n : syncList) {
                System.out.print(n + " ");
                Thread.sleep(2);
            }
            System.out.println();
        } catch (ConcurrentModificationException cme) {
            System.out.println("\nCaught ConcurrentModificationException during UNSAFE iteration");
        }
        t.join();

        // After modifications
        synchronized (syncList) {
            System.out.println("After thread finished, syncList size: " + syncList.size());
        }
    }

    // 6) Composite operations must be synchronized explicitly
    private static void synchronized_CompositeOperations() throws InterruptedException {
        final List<String> syncList = Collections.synchronizedList(new ArrayList<String>());

        // Composite operation: "add if absent" must be done under the same lock
        addIfAbsent(syncList, "A");
        addIfAbsent(syncList, "A"); // will be ignored

        synchronized (syncList) {
            System.out.println("syncList after addIfAbsent twice: " + syncList);
        }

        // Demonstrate thread-safety of the composite operation with multiple threads
        Runnable task = new Runnable() {
            @Override public void run() {
                for (int i = 0; i < 1000; i++) {
                    addIfAbsent(syncList, "B");
                }
            }
        };
        Thread t1 = new Thread(task);
        Thread t2 = new Thread(task);
        t1.start(); t2.start();
        t1.join(); t2.join();

        synchronized (syncList) {
            long countB = 0;
            for (String s : syncList) if ("B".equals(s)) countB++;
            System.out.println("Number of 'B' after concurrent addIfAbsent: " + countB + " (should be 1)");
        }
    }

    private static <E> void addIfAbsent(List<E> syncList, E element) {
        synchronized (syncList) {
            if (!syncList.contains(element)) {
                syncList.add(element);
            }
        }
    }

    // 7) Synchronized Map/Set/SortedMap examples
    private static void synchronized_MapExamples() {
        Map<String, Integer> map = new HashMap<String, Integer>();
        Map<String, Integer> syncMap = Collections.synchronizedMap(map);

        syncMap.put("a", 1);
        syncMap.put("b", 2);

        // Iteration must be synchronized on the map's monitor
        synchronized (syncMap) {
            System.out.print("Entries in syncMap: ");
            for (Map.Entry<String, Integer> e : syncMap.entrySet()) {
                System.out.print(e.getKey() + "=" + e.getValue() + " ");
            }
            System.out.println();
        }

        // SortedMap / NavigableMap wrappers
        SortedMap<Integer, String> sorted = new TreeMap<Integer, String>();
        sorted.put(2, "two");
        sorted.put(1, "one");
        SortedMap<Integer, String> syncSorted = Collections.synchronizedSortedMap(sorted);

        synchronized (syncSorted) {
            System.out.println("syncSorted firstKey: " + syncSorted.firstKey() + ", lastKey: " + syncSorted.lastKey());
            System.out.println("syncSorted view: " + syncSorted);
        }

        // Set wrappers
        Set<String> set = new HashSet<String>();
        Set<String> syncSet = Collections.synchronizedSet(set);
        syncSet.add("x"); syncSet.add("y");

        synchronized (syncSet) {
            System.out.println("syncSet contents: " + syncSet);
        }
    }

    // 8) Alternatives: concurrent collections
    private static void alternatives_ConcurrentCollections() throws InterruptedException {
        // ConcurrentHashMap allows concurrent reads/writes without external synchronization
        ConcurrentHashMap<String, Integer> chm = new ConcurrentHashMap<String, Integer>();
        chm.put("a", 1);
        chm.merge("a", 1, new java.util.function.BiFunction<Integer, Integer, Integer>() {
            @Override public Integer apply(Integer oldV, Integer newV) { return oldV + newV; }
        });
        System.out.println("ConcurrentHashMap value for 'a': " + chm.get("a"));

        // CopyOnWriteArrayList allows safe iteration without locking at the cost of write performance
        CopyOnWriteArrayList<String> cow = new CopyOnWriteArrayList<String>(Arrays.asList("m", "n"));
        for (String s : cow) {
            cow.add("x"); // safe: iterator iterates over a snapshot
        }
        System.out.println("CopyOnWriteArrayList contents after iterating+adding: " + cow);
    }
}