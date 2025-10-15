package _04_03_iterator_and_fail_fast_behavior;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Iterator & Fail-Fast Behavior examples.
 *
 * This file demonstrates:
 * - Basic Iterator usage
 * - Enhanced-for loop (internally uses Iterator)
 * - Safe removal with Iterator.remove()
 * - Unsafe removal during iteration (ConcurrentModificationException)
 * - ListIterator add/set and backward traversal
 * - Fail-Fast behavior (single-thread and multi-thread best-effort)
 * - Fail-Safe iterations with concurrent collections
 * - Proper iteration over synchronized collections
 * - Map iteration patterns and safe removal
 * - SubList and structural modification pitfalls
 * - Iterator.remove misuse (IllegalStateException)
 */
public class _02_Examples {

    public static void main(String[] args) {
        separator("1) Basic Iterator");
        example1_basicIterator();

        separator("2) Enhanced-for uses Iterator");
        example2_enhancedForLoop();

        separator("3) Safe removal with Iterator.remove()");
        example3_safeRemovalWithIterator();

        separator("4) Unsafe removal during for-each (CME)");
        example4_unsafeRemovalInForEach();

        separator("5) ListIterator add/set and backward traversal");
        example5_listIteratorModifyAndTraverseBackwards();

        separator("6) Fail-Fast: single-thread structural modification");
        example6_failFast_singleThreadModificationAfterIteratorCreation();

        separator("7) Fail-Fast: multi-thread (best-effort)");
        example7_failFast_multiThreadedDemonstration_bestEffort();

        separator("8) Fail-Safe: concurrent collections (no CME)");
        example8_failSafe_with_ConcurrentCollections();

        separator("9) SynchronizedList: iterate and remove correctly");
        example9_synchronizedList_iteration_and_removal();

        separator("10) Map iteration and safe removal");
        example10_mapIterationAndRemoval();

        separator("11) SubList pitfall (modifying parent during iteration)");
        example11_subListPitfall();

        separator("12) Iterator.remove misuse (IllegalStateException)");
        example12_iteratorRemoveMisuse();
    }

    // Helper to print section separators
    private static void separator(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }

    /**
     * Basic Iterator usage: forward traversal with hasNext()/next().
     */
    private static void example1_basicIterator() {
        List<String> items = new ArrayList<>(Arrays.asList("A", "B", "C"));
        Iterator<String> it = items.iterator();
        while (it.hasNext()) {
            String val = it.next();
            System.out.println("Next: " + val);
        }
    }

    /**
     * Enhanced-for loop uses the collection's Iterator internally.
     * It is syntactic sugar for the Iterator pattern.
     */
    private static void example2_enhancedForLoop() {
        List<Integer> nums = new ArrayList<>(Arrays.asList(10, 20, 30));
        for (Integer n : nums) {
            System.out.println("Value: " + n);
        }
    }

    /**
     * Safe removal during iteration:
     * Use Iterator.remove() immediately after next().
     */
    private static void example3_safeRemovalWithIterator() {
        List<Integer> nums = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5, 6));
        Iterator<Integer> it = nums.iterator();
        while (it.hasNext()) {
            Integer n = it.next();
            if (n % 2 == 0) {
                it.remove(); // safe
            }
        }
        System.out.println("Odds left: " + nums);
    }

    /**
     * Unsafe removal during for-each triggers ConcurrentModificationException (CME).
     * Do not structurally modify the collection outside the iterator during iteration.
     */
    private static void example4_unsafeRemovalInForEach() {
        List<Integer> nums = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
        try {
            for (Integer n : nums) {
                if (n % 2 == 0) {
                    nums.remove(n); // unsafe: modifies underlying structure
                }
            }
        } catch (ConcurrentModificationException cme) {
            System.out.println("Caught ConcurrentModificationException as expected.");
        }
        System.out.println("List after attempted removals: " + nums);
    }

    /**
     * ListIterator supports:
     * - add(element): insert at current cursor
     * - set(element): replace last returned element
     * - bidirectional traversal (hasPrevious/previous)
     */
    private static void example5_listIteratorModifyAndTraverseBackwards() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        ListIterator<String> li = list.listIterator();
        while (li.hasNext()) {
            String s = li.next();
            if ("b".equals(s)) {
                li.set("B");     // replace "b" with "B"
                li.add("x");     // insert "x" after "B"
            }
        }
        System.out.println("After forward edit: " + list);

        // Traverse backward
        while (li.hasPrevious()) {
            System.out.println("Back: " + li.previous());
        }
        System.out.println("Final list: " + list);
    }

    /**
     * Fail-Fast (single-thread): any structural modification to the collection
     * after an iterator is created causes CME on the iterator's next operation.
     */
    private static void example6_failFast_singleThreadModificationAfterIteratorCreation() {
        List<String> list = new ArrayList<>(Arrays.asList("x", "y", "z"));
        Iterator<String> it = list.iterator();
        System.out.println("First next: " + it.next()); // okay
        list.add("w"); // structural modification outside iterator
        try {
            System.out.println("Next after add: " + it.next()); // triggers CME
        } catch (ConcurrentModificationException cme) {
            System.out.println("Caught ConcurrentModificationException (single-thread).");
        }
    }

    /**
     * Fail-Fast (multi-thread best-effort): iterator may throw CME if another
     * thread modifies the collection concurrently. Not guaranteed, but typical.
     */
    private static void example7_failFast_multiThreadedDemonstration_bestEffort() {
        final List<Integer> list = new ArrayList<>();
        for (int i = 0; i < 10_000; i++) list.add(i);

        AtomicBoolean sawCME = new AtomicBoolean(false);

        Thread iteratorThread = new Thread(() -> {
            try {
                Iterator<Integer> it = list.iterator();
                int sum = 0;
                while (it.hasNext()) {
                    sum += it.next();
                    // slow down to increase chance of overlap
                    if (sum % 1000000 == 0) {
                        try { Thread.sleep(1); } catch (InterruptedException ignored) {}
                    }
                }
                System.out.println("Iterator finished without CME (sum=" + sum + ")");
            } catch (ConcurrentModificationException cme) {
                sawCME.set(true);
                System.out.println("Iterator thread caught ConcurrentModificationException (multi-thread).");
            }
        });

        Thread modifierThread = new Thread(() -> {
            try {
                Thread.sleep(3); // let iterator start
            } catch (InterruptedException ignored) {}
            // Perform structural modifications
            for (int i = 0; i < 1000; i++) {
                list.add(-i); // unsafe concurrent modification
            }
        });

        iteratorThread.start();
        modifierThread.start();
        try {
            iteratorThread.join();
            modifierThread.join();
        } catch (InterruptedException ignored) {}

        System.out.println("CME observed? " + sawCME.get() + " (best-effort)");
    }

    /**
     * Fail-Safe: concurrent collections provide weakly-consistent or snapshot iterators.
     * - CopyOnWriteArrayList: snapshot iterator (no changes visible during iteration)
     * - ConcurrentHashMap: weakly-consistent (no CME; may or may not reflect concurrent changes)
     */
    private static void example8_failSafe_with_ConcurrentCollections() {
        // CopyOnWriteArrayList snapshot
        CopyOnWriteArrayList<Integer> cow = new CopyOnWriteArrayList<>(new Integer[]{1, 2, 3});
        Iterator<Integer> it = cow.iterator();
        cow.add(4); // modification after iterator created
        System.out.print("COW iteration (snapshot): ");
        while (it.hasNext()) {
            System.out.print(it.next() + " "); // prints 1 2 3
        }
        System.out.println("\nCOW list now: " + cow); // shows [1, 2, 3, 4]

        // ConcurrentHashMap weakly-consistent
        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        Iterator<Map.Entry<String, Integer>> mit = map.entrySet().iterator();
        map.put("c", 3); // concurrent modification
        System.out.print("CHM iteration (no CME): ");
        while (mit.hasNext()) {
            Map.Entry<String, Integer> e = mit.next();
            System.out.print(e.getKey() + "=" + e.getValue() + " ");
        }
        System.out.println("\nCHM map now: " + map);
    }

    /**
     * Synchronized collections: iteration must be done within a synchronized block
     * on the collection. Still fail-fast if structurally modified (without iterator).
     */
    private static void example9_synchronizedList_iteration_and_removal() {
        List<Integer> base = new ArrayList<>(Arrays.asList(1, 2, 3, 4));
        List<Integer> syncList = Collections.synchronizedList(base);

        // Proper external synchronization for iteration
        synchronized (syncList) {
            Iterator<Integer> it = syncList.iterator();
            while (it.hasNext()) {
                Integer n = it.next();
                if (n == 2) {
                    it.remove(); // safe even in synchronized list
                }
            }
        }
        System.out.println("After safe removal: " + syncList);

        // Demonstrate that modifying without iterator still fail-fast
        try {
            synchronized (syncList) {
                Iterator<Integer> it2 = syncList.iterator();
                if (it2.hasNext()) {
                    it2.next();
                    // Structural modification outside the iterator
                    syncList.add(99);
                    it2.next(); // triggers CME
                }
            }
        } catch (ConcurrentModificationException cme) {
            System.out.println("Caught CME even with synchronizedList (mod outside iterator).");
        }
    }

    /**
     * Map iteration patterns:
     * - entrySet() with Iterator to remove safely
     * - Direct map.remove during for-each causes CME
     */
    private static void example10_mapIterationAndRemoval() {
        Map<String, Integer> scores = new HashMap<>();
        scores.put("A", 10);
        scores.put("B", 5);
        scores.put("C", 8);

        // Safe removal using iterator
        Iterator<Map.Entry<String, Integer>> it = scores.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Integer> e = it.next();
            if (e.getValue() < 8) {
                it.remove(); // safe
            }
        }
        System.out.println("After safe removal: " + scores);

        // Unsafe removal during for-each
        try {
            for (String k : scores.keySet()) {
                if ("A".equals(k)) {
                    scores.remove(k); // unsafe during iteration
                }
            }
        } catch (ConcurrentModificationException cme) {
            System.out.println("Caught CME removing from map during iteration.");
        }
        System.out.println("Map now: " + scores);
    }

    /**
     * SubList is a view over the parent list (backed by the original).
     * Modifying the parent structurally invalidates subList iterators (CME).
     */
    private static void example11_subListPitfall() {
        List<Integer> parent = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
        List<Integer> view = parent.subList(1, 4); // [2, 3, 4]
        Iterator<Integer> it = view.iterator();
        System.out.println("Parent before: " + parent + ", view: " + view);

        // Modify parent structurally after iterator creation
        parent.add(0, 0);
        try {
            it.next(); // triggers CME due to structural modification in parent
        } catch (ConcurrentModificationException cme) {
            System.out.println("Caught CME due to parent modification affecting subList.");
        }
        System.out.println("Parent after: " + parent + ", view still references underlying list.");
    }

    /**
     * Iterator.remove misuse:
     * - Calling remove() before next() or twice per next() -> IllegalStateException.
     */
    private static void example12_iteratorRemoveMisuse() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        Iterator<String> it = list.iterator();

        try {
            it.remove(); // Illegal: remove before next
        } catch (IllegalStateException ise) {
            System.out.println("Caught IllegalStateException: remove() before next().");
        }

        it = list.iterator();
        String first = it.next(); // "a"
        it.remove(); // OK removes "a"
        try {
            it.remove(); // Illegal: remove twice without intervening next
        } catch (IllegalStateException ise) {
            System.out.println("Caught IllegalStateException: remove() called twice per next().");
        }

        System.out.println("List after misuse demo: " + list);
    }
}