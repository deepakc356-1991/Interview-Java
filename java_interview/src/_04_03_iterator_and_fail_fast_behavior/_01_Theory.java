package _04_03_iterator_and_fail_fast_behavior;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.ConcurrentModificationException;
import java.util.ConcurrentModificationException;
import java.util.ConcurrentModificationException;
import java.util.ConcurrentModificationException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Spliterator;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Iterator & Fail-Fast Behavior (theory + runnable demos)
 *
 * Key concepts covered in code comments and examples:
 * - Iterable vs Iterator; for-each desugaring; external vs internal iteration.
 * - Iterator methods: hasNext, next, remove (optional), and their contracts.
 * - Fail-fast behavior in most java.util collection iterators (best-effort).
 * - Structural vs non-structural modification; ConcurrentModificationException (CME).
 * - Safe removal patterns (iterator.remove, removeIf, iterating a snapshot, index-backwards).
 * - ListIterator: bidirectional iteration; add, set, remove; IllegalStateException rules.
 * - Map iteration via entrySet/keySet/values and fail-fast semantics.
 * - Views/backed collections (subList, keySet, unmodifiable views) share fail-fast state.
 * - Fail-safe / weakly-consistent iterators in concurrent collections:
 *   CopyOnWriteArrayList (snapshot) and ConcurrentHashMap (weakly consistent).
 * - Spliterator fail-fast characteristics in many non-concurrent collections.
 * - Immutable/unmodifiable collections and UnsupportedOperationException.
 * - Legacy Enumeration vs Iterator (Enumeration is not fail-fast).
 *
 * Notes:
 * - Fail-fast is best-effort: detection isn't guaranteed; never rely on CME for program logic.
 * - "Structural modifications" = changes that affect the size or structure (add/remove/clear).
 *   Non-structural changes (e.g., set on List) don't break active iterators.
 * - Do not modify a collection while iterating over it, unless via the iterator's own methods
 *   and only if supported.
 */
public class _01_Theory {
    public static void main(String[] args) throws Exception {
        header("Iterable vs Iterator (and for-each)");
        basicIteratorAndIterable();

        header("Safe removal via Iterator.remove (correct) vs modifying collection (incorrect)");
        safeRemovalWithIterator();
        incorrectRemovalDuringIteration();

        header("Structural vs non-structural modification");
        structuralVsNonStructural();

        header("ListIterator: bidirectional and mutating operations");
        listIteratorOperations();

        header("Map iteration and fail-fast; safe removal of entries");
        mapIterationAndRemoval();

        header("Fail-fast across threads (best-effort demonstration)");
        failFastAcrossThreads();

        header("Fail-safe/weakly-consistent iterators (CopyOnWriteArrayList, ConcurrentHashMap)");
        failSafeIteratorsConcurrentCollections();

        header("Spliterator fail-fast in many non-concurrent collections");
        spliteratorFailFastDemo();

        header("Unmodifiable/immutable collections and iterator.remove()");
        unmodifiableAndImmutable();

        header("Views/backed collections (subList, keySet) share fail-fast state");
        viewAndBackedCollections();

        header("Safe patterns: removeIf; iterate snapshot; index-backwards");
        removeIfAndBulkOps();
        snapshotIterationPattern();
        indexBackwardsRemoval();

        header("Legacy Enumeration vs Iterator");
        enumerationLegacy();

        System.out.println("\nDone.");
    }

    // --------------------------------------------------------------------------------------------
    // Iterable vs Iterator; for-each desugaring
    // --------------------------------------------------------------------------------------------
    private static void basicIteratorAndIterable() {
        // Iterable<T> exposes iterator(): Iterator<T>. For-each uses this under the hood.
        List<String> names = new ArrayList<>(Arrays.asList("Ann", "Bob", "Cat"));

        // For-each = syntactic sugar over Iterator
        System.out.print("for-each: ");
        for (String n : names) {
            System.out.print(n + " ");
        }
        System.out.println();

        // Equivalent external iteration using Iterator
        System.out.print("iterator: ");
        Iterator<String> it = names.iterator();
        while (it.hasNext()) {
            String n = it.next(); // next() moves and returns; throws NoSuchElementException if none
            System.out.print(n + " ");
        }
        System.out.println();

        // Custom Iterable demonstrating contract
        System.out.print("custom Iterable (Range 3..6): ");
        for (int i : new Range(3, 7)) {
            System.out.print(i + " ");
        }
        System.out.println();
    }

    // --------------------------------------------------------------------------------------------
    // Safe removal with Iterator.remove()
    // --------------------------------------------------------------------------------------------
    private static void safeRemovalWithIterator() {
        List<Integer> nums = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
        System.out.println("Before safe removal: " + nums);

        Iterator<Integer> it = nums.iterator();
        while (it.hasNext()) {
            Integer n = it.next();
            if (n % 2 == 0) {
                // Safe: removes the element returned by the last next(); allowed once per next().
                it.remove();
            }
        }
        System.out.println("After safe removal (removed even): " + nums);
    }

    // --------------------------------------------------------------------------------------------
    // Incorrect structural modification during iteration -> fail-fast CME
    // --------------------------------------------------------------------------------------------
    private static void incorrectRemovalDuringIteration() {
        List<Integer> nums = new ArrayList<>(Arrays.asList(10, 20, 30, 40));
        System.out.println("Before incorrect removal: " + nums);

        try {
            Iterator<Integer> it = nums.iterator();
            while (it.hasNext()) {
                Integer n = it.next();
                if (n == 30) {
                    // Incorrect: modifies the list directly (structural change) while iterating.
                    // Most java.util collections are fail-fast and will throw CME on next iterator op.
                    nums.remove(n); // triggers CME soon (best-effort)
                }
            }
            System.out.println("After incorrect removal (unexpected): " + nums);
        } catch (ConcurrentModificationException cme) {
            System.out.println("Caught ConcurrentModificationException (expected with direct removal).");
        }
    }

    // --------------------------------------------------------------------------------------------
    // Structural vs Non-structural modification
    // --------------------------------------------------------------------------------------------
    private static void structuralVsNonStructural() {
        List<String> list = new ArrayList<>(Arrays.asList("A", "B", "C"));
        Iterator<String> it = list.iterator();

        // Non-structural modification: set (replaces element; size unchanged)
        list.set(1, "B*"); // safe w.r.t. active iterator

        // Structural modification: add/remove/clear, which invalidates iterators
        try {
            list.add("D"); // structural; will invalidate iterator
            it.hasNext();  // touching iterator after structural change may throw CME
            it.next();     // likely throws CME here
            System.out.println("Unexpected: iterator survived structural change.");
        } catch (ConcurrentModificationException cme) {
            System.out.println("CME after structural modification (add) with active iterator (expected).");
        }
    }

    // --------------------------------------------------------------------------------------------
    // ListIterator: bidirectional iteration and more operations
    // --------------------------------------------------------------------------------------------
    private static void listIteratorOperations() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        ListIterator<String> li = list.listIterator();

        // Forward iteration
        while (li.hasNext()) {
            String val = li.next();
            if (val.equals("b")) {
                // set: replaces last returned element; allowed only after next()/previous(), not after add/remove.
                li.set("B");
                // add: inserts at current cursor position (before next element)
                li.add("X"); // structural via iterator: safe during this iteration
            }
        }
        // Backward iteration
        while (li.hasPrevious()) {
            String val = li.previous();
            if (val.equals("a")) {
                // remove last returned by previous()/next(); only once per cursor move
                li.remove();
            }
        }
        System.out.println("ListIterator result: " + list);
    }

    // --------------------------------------------------------------------------------------------
    // Map iteration and safe removal via iterator
    // --------------------------------------------------------------------------------------------
    private static void mapIterationAndRemoval() {
        Map<String, Integer> map = new HashMap<>();
        map.put("A", 1);
        map.put("B", 2);
        map.put("C", 3);

        // Safe removal via iterator on entrySet
        Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Integer> e = it.next();
            if (e.getValue() % 2 == 1) {
                it.remove(); // safe removal of current entry
            }
        }
        System.out.println("After safe removal of odd values: " + map);

        // Incorrect: modifying the map directly while iterating a view/iterator
        try {
            Iterator<String> kIt = map.keySet().iterator();
            while (kIt.hasNext()) {
                String k = kIt.next();
                if ("B".equals(k)) {
                    map.put("D", 4); // structural change -> CME likely on next iterator op
                }
            }
            System.out.println("Unexpected: map iteration survived structural change.");
        } catch (ConcurrentModificationException cme) {
            System.out.println("Map iteration: CME after structural change (expected).");
        }
    }

    // --------------------------------------------------------------------------------------------
    // Fail-fast across threads (best-effort)
    // --------------------------------------------------------------------------------------------
    private static void failFastAcrossThreads() throws InterruptedException {
        final List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));

        Thread modifier = new Thread(() -> {
            try {
                Thread.sleep(50);
                list.add(6); // structural change from another thread
                Thread.sleep(50);
                list.remove(Integer.valueOf(2));
            } catch (InterruptedException ignored) {}
        });

        modifier.start();

        try {
            Iterator<Integer> it = list.iterator();
            while (it.hasNext()) {
                Integer n = it.next();
                // Slow down to increase overlap probability
                Thread.sleep(30);
                System.out.print(n + " ");
            }
            System.out.println("\nUnexpected: iteration completed without CME (best-effort).");
        } catch (ConcurrentModificationException cme) {
            System.out.println("\nCME thrown while iterating from one thread and modifying from another (expected, best-effort).");
        }

        modifier.join();
    }

    // --------------------------------------------------------------------------------------------
    // Fail-safe (snapshot/weakly-consistent) iterators in concurrent collections
    // --------------------------------------------------------------------------------------------
    private static void failSafeIteratorsConcurrentCollections() {
        // CopyOnWriteArrayList: iterator is over a snapshot; no CME; modifications not seen during current iteration.
        CopyOnWriteArrayList<String> cow = new CopyOnWriteArrayList<>(new String[]{"a", "b", "c"});
        System.out.print("COW iteration (modifying during iteration): ");
        for (String s : cow) {
            System.out.print(s + " ");
            if ("b".equals(s)) {
                cow.add("X"); // does not affect current iteration
            }
        }
        System.out.println("\nCOW after iteration: " + cow);

        // ConcurrentHashMap: weakly-consistent iterator; no CME; may or may not reflect concurrent changes.
        ConcurrentHashMap<String, Integer> chm = new ConcurrentHashMap<>();
        chm.put("k1", 1);
        chm.put("k2", 2);
        System.out.print("CHM iteration (adding during iteration): ");
        for (Map.Entry<String, Integer> e : chm.entrySet()) {
            System.out.print(e + " ");
            if ("k1".equals(e.getKey())) {
                chm.put("k3", 3); // allowed; iterator remains valid
            }
        }
        System.out.println("\nCHM final contents: " + chm);
    }

    // --------------------------------------------------------------------------------------------
    // Spliterator fail-fast example for non-concurrent collections
    // --------------------------------------------------------------------------------------------
    private static void spliteratorFailFastDemo() {
        List<Integer> list = new ArrayList<>(Arrays.asList(10, 20, 30));
        Spliterator<Integer> sp = list.spliterator();
        System.out.print("Spliterator tryAdvance: ");
        sp.tryAdvance(n -> System.out.print(n + " ")); // consumes 10

        try {
            list.add(40); // structural modification
            // Many Spliterator impls in java.util are fail-fast (best-effort) and may throw CME now
            sp.tryAdvance(n -> System.out.print(n + " "));
            System.out.println("(Unexpected: no CME)");
        } catch (ConcurrentModificationException cme) {
            System.out.println("\nCME from Spliterator after structural change (expected).");
        }
    }

    // --------------------------------------------------------------------------------------------
    // Unmodifiable/immutable collections and iterator.remove()
    // --------------------------------------------------------------------------------------------
    private static void unmodifiableAndImmutable() {
        List<String> mutable = new ArrayList<>(Arrays.asList("A", "B", "C"));
        List<String> unmodView = Collections.unmodifiableList(mutable);

        // iterator.remove on an unmodifiable view throws UnsupportedOperationException
        try {
            Iterator<String> it = unmodView.iterator();
            it.next();
            it.remove(); // not supported
            System.out.println("Unexpected: remove succeeded on unmodifiable view.");
        } catch (UnsupportedOperationException uoe) {
            System.out.println("UnsupportedOperationException on iterator.remove() for unmodifiable view (expected).");
        }

        // If you modify the backing list while iterating the view, you still get fail-fast CME.
        try {
            Iterator<String> it = unmodView.iterator();
            System.out.print("Iterating unmodifiable view: ");
            System.out.print(it.next() + " ");
            mutable.add("D"); // structural change in backing list
            it.next();        // likely triggers CME
            System.out.println("(Unexpected: continued)");
        } catch (ConcurrentModificationException cme) {
            System.out.println("\nCME on unmodifiable view due to backing list modification (expected).");
        }

        // Truly immutable lists (e.g., List.of(...)) also do not support iteration removal.
        List<String> imm = List.of("X", "Y", "Z");
        try {
            Iterator<String> it = imm.iterator();
            it.next();
            it.remove();
            System.out.println("Unexpected: remove on immutable List succeeded.");
        } catch (UnsupportedOperationException uoe) {
            System.out.println("UnsupportedOperationException on iterator.remove() for immutable List (expected).");
        }
    }

    // --------------------------------------------------------------------------------------------
    // Views/backed collections (subList, keySet, etc.) share fail-fast state
    // --------------------------------------------------------------------------------------------
    private static void viewAndBackedCollections() {
        List<Integer> base = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
        List<Integer> view = base.subList(1, 4); // [2,3,4], backed by base

        Iterator<Integer> it = view.iterator();
        System.out.print("Iterating subList: ");
        System.out.print(it.next() + " "); // 2
        base.add(99); // modify base; invalidates the subList iterator
        try {
            it.next(); // likely CME
            System.out.println("(Unexpected: continued)");
        } catch (ConcurrentModificationException cme) {
            System.out.println("\nCME on subList iterator after modifying base (expected).");
        }

        // Similarly, modifying a Map while iterating keySet/entrySet/values will throw CME for HashMap.
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        Iterator<String> kIt = map.keySet().iterator();
        map.put("c", 3); // structural modification
        try {
            kIt.hasNext(); // may already throw
            kIt.next();    // likely CME
            System.out.println("Unexpected: map keySet iteration survived structural change.");
        } catch (ConcurrentModificationException cme) {
            System.out.println("CME on keySet iterator after map modification (expected).");
        }
    }

    // --------------------------------------------------------------------------------------------
    // Safe patterns: removeIf outside external iteration; bulk ops are internal iterations
    // --------------------------------------------------------------------------------------------
    private static void removeIfAndBulkOps() {
        List<Integer> list = new ArrayList<>(Arrays.asList(1,2,3,4,5,6));
        // Safe: removeIf performs internal iteration and handles structural changes internally.
        list.removeIf(n -> n % 2 == 0);
        System.out.println("After removeIf (removed even): " + list);

        // Caution: Do NOT call removeIf while also externally iterating the same collection.
        List<Integer> bad = new ArrayList<>(Arrays.asList(1,2,3,4));
        try {
            for (Integer n : bad) {
                if (n == 2) {
                    bad.removeIf(x -> x > 2); // structural change during external iteration -> CME likely
                }
            }
            System.out.println("Unexpected: no CME in external+internal mix.");
        } catch (ConcurrentModificationException cme) {
            System.out.println("CME when mixing external iteration and removeIf (expected).");
        }
    }

    private static void snapshotIterationPattern() {
        List<String> live = new ArrayList<>(Arrays.asList("p", "q", "r", "s"));
        // Safe: iterate a snapshot while modifying the original.
        for (String s : new ArrayList<>(live)) { // snapshot copy
            System.out.print(s + " ");
            if (s.equals("q")) {
                live.add("t"); // modifying the live collection does not affect snapshot iteration
            }
        }
        System.out.println("\nAfter snapshot iteration, live: " + live);
    }

    private static void indexBackwardsRemoval() {
        List<Integer> list = new ArrayList<>(Arrays.asList(1,2,3,4,5,6));
        // Safe for random-access lists when only using index-based loop within same thread:
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i) % 2 == 0) {
                list.remove(i); // removing by index while iterating backwards avoids skipping
            }
        }
        System.out.println("Index-backwards removal result: " + list);
    }

    // --------------------------------------------------------------------------------------------
    // Legacy Enumeration vs Iterator
    // --------------------------------------------------------------------------------------------
    private static void enumerationLegacy() {
        Vector<String> v = new Vector<>(Arrays.asList("A", "B", "C"));
        Enumeration<String> en = v.elements();

        System.out.print("Enumeration over Vector (modify during enumeration): ");
        while (en.hasMoreElements()) {
            String val = en.nextElement();
            System.out.print(val + " ");
            if ("B".equals(val)) {
                v.add("X"); // Enumeration is not fail-fast; behavior is unspecified but typically no CME
            }
        }
        System.out.println("\nVector after enumeration: " + v);
        // Note: Enumeration lacks remove(); modern code should prefer Iterator.
    }

    // --------------------------------------------------------------------------------------------
    // Utilities and helper classes
    // --------------------------------------------------------------------------------------------
    private static void header(String title) {
        System.out.println("\n=== " + title + " ===");
    }

    /**
     * Minimal custom Iterable producing integers in [start, end).
     * Demonstrates how for-each relies on Iterable#iterator().
     */
    static class Range implements Iterable<Integer> {
        private final int startInclusive;
        private final int endExclusive;

        Range(int startInclusive, int endExclusive) {
            this.startInclusive = startInclusive;
            this.endExclusive = endExclusive;
        }

        @Override
        public Iterator<Integer> iterator() {
            return new Iterator<Integer>() {
                private int cursor = startInclusive;
                private boolean canRemove = false;

                @Override
                public boolean hasNext() {
                    return cursor < endExclusive;
                }

                @Override
                public Integer next() {
                    int v = cursor++;
                    canRemove = true;
                    return v;
                }

                @Override
                public void remove() {
                    // Optional operation; we choose not to support it here.
                    // Many JDK iterators also throw UnsupportedOperationException for remove().
                    if (!canRemove) {
                        throw new IllegalStateException("next() not called yet");
                    }
                    throw new UnsupportedOperationException("remove not supported");
                }
            };
        }
    }
}