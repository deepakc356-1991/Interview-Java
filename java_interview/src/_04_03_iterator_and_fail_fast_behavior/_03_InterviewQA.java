package _04_03_iterator_and_fail_fast_behavior;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/*
Interview Q&A: Iterator & Fail-Fast Behavior (Basic → Advanced)

Core concepts
- Iterator: A cursor to traverse a collection one element at a time. Methods: hasNext(), next(), remove(), forEachRemaining(Consumer).
- Fail-fast iterator: Detects structural modification and throws ConcurrentModificationException (CME). Best-effort only.
- Structural modification: Changes size or structure (add/remove/clear). Non-structural: e.g., set(index, value) on List.
- Safe removal during iteration: Use Iterator.remove() or ListIterator.remove()/add()/set() as appropriate.
- For-each (enhanced for) uses Iterator under the hood — same fail-fast rules apply.
- Map iteration: Use map.entrySet().iterator() and iterator.remove() to safely remove during iteration.
- Fail-safe/weakly-consistent iterators:
  - CopyOnWriteArrayList: snapshot iterator (no CME; doesn’t reflect concurrent adds/removes).
  - ConcurrentHashMap: weakly consistent iterator (no CME; may reflect some concurrent updates).

Common interview Q&A (summaries)
1) What triggers ConcurrentModificationException?
   - Detected structural changes made outside the iterator after the iterator is created (single or multi-thread). Detection usually occurs on next()/remove()/forEachRemaining(), not necessarily on hasNext().

2) How to remove elements while iterating without CME?
   - Use iterator.remove() right after next() returns the element to remove.
   - For Lists, ListIterator can add/remove/set during iteration.

3) Difference: Iterator vs ListIterator vs Enumeration vs Spliterator?
   - Iterator: forward-only; remove() optional.
   - ListIterator: bidirectional over List; add(), set(), remove() supported with rules.
   - Enumeration: legacy, forward-only, no remove(); not fail-fast.
   - Spliterator: supports efficient parallel traversal; has characteristics flags; used by streams.

4) Fail-fast vs fail-safe/weakly consistent?
   - Fail-fast: throws CME upon detection; best-effort; not guaranteed.
   - Fail-safe/weakly consistent: never throws CME; may or may not see concurrent changes; CopyOnWrite is snapshot; ConcurrentHashMap is weakly consistent.

5) Structural vs non-structural modification?
   - Structural: add/remove/clear (affects size/structure).
   - Non-structural: set on List (replaces element) — doesn’t affect modCount.

6) Why “best-effort”?
   - No hard memory/synchronization guarantees; checks rely on internal counters (modCount), visible “enough” but not strictly required to always detect.

7) How to iterate Maps safely and remove entries?
   - map.entrySet().iterator(); call iterator.remove() after checking a condition.

8) Sublist pitfalls?
   - subList is a view; structural changes to parent or sublist while iterating the other cause CME.

9) Synchronized wrappers and iteration?
   - Collections.synchronizedList/map wrap methods with synchronization; external iteration must synchronize on the wrapper during iteration; still fail-fast.

10) Streams and fail-fast?
   - Stream over fail-fast collections inherits the underlying iterator semantics; concurrent modification usually results in CME or undefined behavior unless using concurrent collections.

11) When does hasNext() throw CME?
   - Depends on implementation. Commonly, next() performs the fail-fast check; hasNext() may not. Do not rely on hasNext() to detect CME.

12) UnsupportedOperationException vs CME?
   - remove() may be unsupported by an iterator (e.g., unmodifiable collections) → UnsupportedOperationException (not CME).

13) Concurrent access patterns to avoid CME?
   - Single-threaded: use iterator.remove/listIterator.
   - Multi-threaded: use concurrent collections (ConcurrentHashMap/CopyOnWriteArrayList), or control synchronization, or pre-copy to snapshot, or use immutable data.

14) Iterator contract edge cases?
   - remove() must be called at most once per next() and only after next(); otherwise IllegalStateException.

15) CopyOnWriteArrayList trade-offs?
   - Reads are fast and non-blocking; writes copy the array (expensive). Iterators are snapshot and never throw CME.

16) ConcurrentHashMap iteration behavior?
   - Weakly consistent: reflects some updates, not all; no CME; safe for concurrent access.

17) Detect CME timing?
   - Typically on next()/remove()/forEachRemaining(); not necessarily at mutation time.

18) Multiple iterators over same collection?
   - If one iterator structurally modifies the collection, other iterators created earlier will typically see CME on next().

19) Spliterator characteristics?
   - ORDERED, SORTED, DISTINCT, SIZED, SUBSIZED, CONCURRENT, IMMUTABLE, NONNULL. Streams use spliterators to split work for parallelism.

20) Best practices?
   - Use iterator.remove() for in-loop removal; prefer concurrent collections for multi-threading; avoid mutating source collection inside loops; prefer removeIf/replaceAll for clarity; be cautious with subList; understand snapshot vs weakly consistent iterators.


This class prints compact Q&A summaries and runs short demos that you can study.
*/
public class _03_InterviewQA {

    public static void main(String[] args) throws InterruptedException {
        header("Iterator & Fail-Fast Behavior — Interview Q&A Demos");

        q("What is an Iterator? Basic usage");
        demoBasicIteratorUsage();

        q("How to remove while iterating safely?");
        demoSafeRemovalWithIteratorRemove();

        q("What is fail-fast? Show CME on same thread modification");
        demoFailFastSameThread();

        q("When does CME trigger relative to hasNext()/next()?");
        demoHasNextVsNextCME();

        q("Non-structural modification (set) during iteration");
        demoNonStructuralModification();

        q("ListIterator can add/set during iteration");
        demoListIteratorAddSet();

        q("Map iteration + safe removal via iterator");
        demoMapIterationAndRemoval();

        q("Fail-fast across threads (best-effort; demo coordinated)");
        demoFailFastCrossThreadCoordinated();

        q("Fail-safe snapshot iterator: CopyOnWriteArrayList");
        demoCopyOnWriteArrayList();

        q("Weakly consistent iterator: ConcurrentHashMap");
        demoConcurrentHashMapIterator();

        q("Unmodifiable collections: remove() throws UnsupportedOperationException");
        demoUnmodifiableIteratorRemove();

        q("Bulk ops removeIf/replaceAll use iterators safely");
        demoRemoveIfReplaceAll();

        q("subList view: cross-modification during iteration causes CME");
        demoSubListPitfall();

        q("Multiple iterators: one modifies, the other sees CME");
        demoTwoIteratorsCME();

        q("Spliterator basics and characteristics");
        demoSpliteratorBasics();

        q("Enhanced for-loop uses Iterator under the hood");
        demoForeachIteratorRelation();

        q("Best practices cheat-sheet");
        bestPractices();

        footer("End of demos");
    }

    /* ========================= DEMOS ========================= */

    private static void demoBasicIteratorUsage() {
        List<String> list = new ArrayList<>(Arrays.asList("A", "B", "C"));
        Iterator<String> it = list.iterator();
        System.out.print("Traverse: ");
        while (it.hasNext()) {
            System.out.print(it.next() + " ");
        }
        System.out.println();
    }

    private static void demoSafeRemovalWithIteratorRemove() {
        List<Integer> nums = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
        Iterator<Integer> it = nums.iterator();
        while (it.hasNext()) {
            if (it.next() % 2 == 0) {
                it.remove(); // Safe structural removal during iteration
            }
        }
        System.out.println("After removing evens via iterator.remove(): " + nums); // [1, 3, 5]
    }

    private static void demoFailFastSameThread() {
        try {
            List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3, 4));
            for (Integer x : list) { // enhanced for uses an Iterator
                if (x == 2) {
                    list.add(99); // Structural change outside iterator -> CME
                }
            }
        } catch (ConcurrentModificationException cme) {
            System.out.println("CME thrown as expected: " + cme.getClass().getSimpleName());
        }
    }

    private static void demoHasNextVsNextCME() {
        try {
            List<Integer> list = new ArrayList<>(Arrays.asList(10, 20, 30));
            Iterator<Integer> it = list.iterator();
            System.out.println("hasNext() before mutation: " + it.hasNext());
            list.add(40); // Structural change
            // Many iterators check on next(); hasNext() may not detect CME
            it.next(); // Should trigger CME in typical JDK implementations
            System.out.println("Unreached line (if CME not thrown).");
        } catch (ConcurrentModificationException cme) {
            System.out.println("CME detected on next() after hasNext(): " + cme.getClass().getSimpleName());
        }
    }

    private static void demoNonStructuralModification() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        Iterator<String> it = list.iterator();
        list.set(1, "B"); // Non-structural: does not change size — no CME
        System.out.print("Iterate after set(): ");
        while (it.hasNext()) {
            System.out.print(it.next() + " "); // a B c
        }
        System.out.println();
    }

    private static void demoListIteratorAddSet() {
        List<String> list = new ArrayList<>(Arrays.asList("x", "y", "z"));
        ListIterator<String> li = list.listIterator();
        while (li.hasNext()) {
            String val = li.next();
            if ("y".equals(val)) {
                li.add("Y+");  // legal during iteration
                li.set("Y");   // replaces last returned "y"
            }
        }
        System.out.println("ListIterator add/set result: " + list); // [x, Y, Y+, z]
    }

    private static void demoMapIterationAndRemoval() {
        Map<String, Integer> map = new HashMap<>();
        map.put("A", 1);
        map.put("B", 2);
        map.put("C", 3);

        Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Integer> e = it.next();
            if (e.getValue() % 2 == 1) {
                it.remove(); // Safe removal
            }
        }
        System.out.println("After safe removal (odd values removed): " + map); // {B=2}

        try {
            // Demonstrate CME by modifying map directly during iteration
            map.put("D", 4);
            for (Map.Entry<String, Integer> e : map.entrySet()) {
                if ("B".equals(e.getKey())) {
                    map.put("E", 5); // CME
                }
            }
        } catch (ConcurrentModificationException cme) {
            System.out.println("Map iteration CME: " + cme.getClass().getSimpleName());
        }
    }

    private static void demoFailFastCrossThreadCoordinated() throws InterruptedException {
        try {
            List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
            Iterator<Integer> it = list.iterator();

            if (it.hasNext()) {
                System.out.println("Main next(): " + it.next());
            }

            Thread t = new Thread(() -> {
                // This happens-before the next main-thread next() due to join()
                list.add(99); // Structural mutation in another thread
            });
            t.start();
            t.join(); // Ensure visibility to main thread

            // Detect CME on next()
            it.next(); // Should throw CME
            System.out.println("Unreached line if CME thrown.");
        } catch (ConcurrentModificationException cme) {
            System.out.println("Cross-thread CME detected: " + cme.getClass().getSimpleName());
        }
    }

    private static void demoCopyOnWriteArrayList() {
        CopyOnWriteArrayList<Integer> cow = new CopyOnWriteArrayList<>(Arrays.asList(1, 2, 3));
        Iterator<Integer> it = cow.iterator(); // snapshot iterator
        while (it.hasNext()) {
            Integer v = it.next();
            if (v == 2) {
                cow.add(99); // No CME; iterator won't see 99
            }
        }
        System.out.println("COW list after add during iteration: " + cow); // [1,2,3,99]
        System.out.print("Snapshot iterator saw: ");
        it = cow.iterator();
        while (it.hasNext()) {
            System.out.print(it.next() + " ");
        }
        System.out.println();
    }

    private static void demoConcurrentHashMapIterator() {
        ConcurrentHashMap<String, Integer> chm = new ConcurrentHashMap<>();
        chm.put("k1", 1);
        chm.put("k2", 2);
        chm.put("k3", 3);

        Iterator<Map.Entry<String, Integer>> it = chm.entrySet().iterator(); // weakly consistent
        while (it.hasNext()) {
            Map.Entry<String, Integer> e = it.next();
            if ("k2".equals(e.getKey())) {
                chm.put("k4", 4); // No CME; iterator may or may not see k4
            }
        }
        System.out.println("ConcurrentHashMap after concurrent put: " + chm);
    }

    private static void demoUnmodifiableIteratorRemove() {
        List<String> base = new ArrayList<>(Arrays.asList("A", "B", "C"));
        List<String> unmod = Collections.unmodifiableList(base);
        Iterator<String> it = unmod.iterator();
        try {
            it.next();
            it.remove(); // remove() not supported
        } catch (UnsupportedOperationException uoe) {
            System.out.println("Unmodifiable iterator.remove(): " + uoe.getClass().getSimpleName());
        }
    }

    private static void demoRemoveIfReplaceAll() {
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5, 6));
        list.removeIf(n -> n % 2 == 0); // safely removes evens
        System.out.println("removeIf result: " + list); // [1,3,5]

        List<String> words = new ArrayList<>(Arrays.asList("a", "b", "c"));
        words.replaceAll(String::toUpperCase); // safe non-structural
        System.out.println("replaceAll result: " + words); // [A,B,C]
    }

    private static void demoSubListPitfall() {
        try {
            List<Integer> base = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 5));
            List<Integer> view = base.subList(1, 5); // [1,2,3,4]
            Iterator<Integer> it = view.iterator();
            System.out.print("Iterating subList, then modifying base... ");
            if (it.hasNext()) {
                System.out.print(it.next() + " ");
            }
            base.add(99); // Structural change to parent
            it.next(); // Should throw CME
            System.out.println("Unreached line if CME thrown");
        } catch (ConcurrentModificationException cme) {
            System.out.println("| CME via subList view: " + cme.getClass().getSimpleName());
        }
    }

    private static void demoTwoIteratorsCME() {
        try {
            List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3, 4));
            Iterator<Integer> it1 = list.iterator();
            Iterator<Integer> it2 = list.iterator();

            if (it2.hasNext()) {
                it2.next();
                it2.remove(); // Structural modification via it2
            }
            it1.next(); // it1 still expects old modCount -> CME
            System.out.println("Unreached line if CME thrown.");
        } catch (ConcurrentModificationException cme) {
            System.out.println("Two iterators CME: " + cme.getClass().getSimpleName());
        }
    }

    private static void demoSpliteratorBasics() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6);
        Spliterator<Integer> s1 = list.spliterator();
        Spliterator<Integer> s2 = s1.trySplit(); // split work

        System.out.println("Spliterator characteristics: " +
                characteristicsToString(list.spliterator().characteristics()));

        System.out.print("s1: ");
        if (s1 != null) s1.forEachRemaining(x -> System.out.print(x + " "));
        System.out.print("| s2: ");
        if (s2 != null) s2.forEachRemaining(x -> System.out.print(x + " "));
        System.out.println();
    }

    private static void demoForeachIteratorRelation() {
        List<String> list = new ArrayList<>(Arrays.asList("p", "q", "r"));
        try {
            for (String s : list) { // uses Iterator internally
                if ("q".equals(s)) {
                    list.remove(s); // direct structural change -> CME
                }
            }
        } catch (ConcurrentModificationException cme) {
            System.out.println("For-each uses Iterator; CME thrown: " + cme.getClass().getSimpleName());
        }
    }

    /* ========================= HELPERS ========================= */

    private static String characteristicsToString(int ch) {
        List<String> flags = new ArrayList<>();
        if ((ch & Spliterator.ORDERED) != 0) flags.add("ORDERED");
        if ((ch & Spliterator.DISTINCT) != 0) flags.add("DISTINCT");
        if ((ch & Spliterator.SORTED) != 0) flags.add("SORTED");
        if ((ch & Spliterator.SIZED) != 0) flags.add("SIZED");
        if ((ch & Spliterator.NONNULL) != 0) flags.add("NONNULL");
        if ((ch & Spliterator.IMMUTABLE) != 0) flags.add("IMMUTABLE");
        if ((ch & Spliterator.CONCURRENT) != 0) flags.add("CONCURRENT");
        if ((ch & Spliterator.SUBSIZED) != 0) flags.add("SUBSIZED");
        return flags.toString();
    }

    private static void bestPractices() {
        System.out.println("- Use iterator.remove() for in-loop removal (single-threaded).");
        System.out.println("- Prefer ListIterator when you need add/set during traversal.");
        System.out.println("- For multi-threaded iteration, favor concurrent collections or snapshots.");
        System.out.println("- Avoid mutating a collection from inside a for-each over that same collection.");
        System.out.println("- Beware subList views; don’t cross-modify parent and subview mid-iteration.");
        System.out.println("- Unmodifiable collections’ iterators often don’t support remove().");
        System.out.println("- Bulk ops removeIf/replaceAll are concise and correct.");
        System.out.println("- Fail-fast is best-effort; don’t rely on it for thread-safety.");
    }

    private static void header(String msg) {
        System.out.println("=== " + msg + " ===");
    }

    private static void footer(String msg) {
        System.out.println("=== " + msg + " ===");
    }

    private static void q(String question) {
        System.out.println();
        System.out.println("Q: " + question);
    }
}