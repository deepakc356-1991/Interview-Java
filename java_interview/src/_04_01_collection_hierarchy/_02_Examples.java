package _04_01_collection_hierarchy;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class _02_Examples {
    public static void main(String[] args) {
        System.out.println("=== Collection basics ===");
        demoCollectionBasics();

        System.out.println("\n=== List (ArrayList, LinkedList, subList, sorting, iterators) ===");
        demoList();

        System.out.println("\n=== Set (HashSet, LinkedHashSet, TreeSet, NavigableSet) ===");
        demoSet();

        System.out.println("\n=== Queue and Deque (LinkedList, PriorityQueue, ArrayDeque) ===");
        demoQueueAndDeque();

        System.out.println("\n=== Map (HashMap, LinkedHashMap, TreeMap, NavigableMap) ===");
        demoMap();

        System.out.println("\n=== Views and wrappers (unmodifiable, synchronized, checked) ===");
        demoViewsAndWrappers();

        System.out.println("\n=== Wildcards and generics (PECS: Producer Extends, Consumer Super) ===");
        demoWildcardsAndGenerics();

        System.out.println("\n=== equals and hashCode impact on sets/maps ===");
        demoEqualsAndHashCode();
    }

    // ---------- Collection basics ----------
    private static void demoCollectionBasics() {
        // Collection is the root interface for List, Set, Queue (Map is separate but part of the framework).
        Collection<String> c = new ArrayList<>();
        c.add("A");
        c.add("B");
        c.addAll(Arrays.asList("C", "D", "E"));
        System.out.println("Collection: " + c + " size=" + c.size());

        System.out.println("Contains 'C'? " + c.contains("C"));
        c.remove("B"); // removes a single matching element
        System.out.println("After remove 'B': " + c);

        // removeIf uses a predicate
        c.removeIf(s -> s.compareTo("D") >= 0);
        System.out.println("After removeIf >= 'D': " + c);

        // retainAll keeps only items also in the given collection
        c.retainAll(Arrays.asList("A", "Z"));
        System.out.println("After retainAll(A, Z): " + c);

        // toArray usage
        c.addAll(Arrays.asList("X", "Y", "Z"));
        Object[] arr = c.toArray();
        String[] arr2 = c.toArray(new String[0]);
        System.out.println("toArray length=" + arr.length + " typed length=" + arr2.length);

        // Iterator for safe removal while iterating
        Iterator<String> it = c.iterator();
        while (it.hasNext()) {
            if (it.next().equals("X")) {
                it.remove();
            }
        }
        System.out.println("After iterator remove 'X': " + c);
        c.clear();
        System.out.println("Cleared collection, isEmpty=" + c.isEmpty());
    }

    // ---------- List ----------
    private static void demoList() {
        // ArrayList: dynamic array, fast random access
        List<String> list = new ArrayList<>(Arrays.asList("A", "B", "C"));
        list.add(1, "X");                 // insert at index
        list.set(2, "Y");                 // replace at index
        System.out.println("ArrayList: " + list + " get(0)=" + list.get(0));
        System.out.println("indexOf 'Y'=" + list.indexOf("Y") + " lastIndexOf 'A'=" + list.lastIndexOf("A"));

        // subList is a view backed by the original list
        List<String> sub = list.subList(1, 3); // elements at index 1 and 2
        sub.set(0, "SUB");                     // changes reflect in original list
        System.out.println("subList: " + sub + " original: " + list);

        // ListIterator can go forward/backward and modify during iteration
        ListIterator<String> li = list.listIterator();
        while (li.hasNext()) {
            String v = li.next();
            if (v.equals("C")) li.add("ADDED_AFTER_C");
        }
        System.out.println("After ListIterator add: " + list);

        // Fixed-size list from Arrays.asList: structural changes (add/remove) not allowed
        List<String> fixed = Arrays.asList("P", "Q", "R");
        try {
            fixed.add("S");
        } catch (UnsupportedOperationException e) {
            System.out.println("Arrays.asList is fixed-size: add throws " + e.getClass().getSimpleName());
        }

        // Sorting and searching
        List<Integer> nums = new ArrayList<>(Arrays.asList(5, 2, 9, 2, 1));
        nums.sort(Comparator.naturalOrder());
        System.out.println("Sorted: " + nums);
        int idx = Collections.binarySearch(nums, 5);
        System.out.println("binarySearch 5 -> index " + idx);
        nums.sort(Comparator.reverseOrder());
        System.out.println("Reverse sorted: " + nums);

        // LinkedList: also implements Deque (fast inserts/removes at ends)
        LinkedList<String> linked = new LinkedList<>();
        linked.addFirst("first");
        linked.addLast("last");
        linked.add(1, "middle");
        System.out.println("LinkedList: " + linked + " first=" + linked.getFirst() + " last=" + linked.getLast());
    }

    // ---------- Set ----------
    private static void demoSet() {
        // HashSet: no ordering guarantees
        Set<String> hashSet = new HashSet<>(Arrays.asList("A", "B", "A", "C", "B"));
        System.out.println("HashSet (dedup, no order): " + hashSet);

        // LinkedHashSet: preserves insertion order
        Set<String> linkedSet = new LinkedHashSet<>(Arrays.asList("B", "A", "C", "A", "D"));
        System.out.println("LinkedHashSet (insertion order): " + linkedSet);

        // TreeSet: sorted set (natural order or custom Comparator)
        NavigableSet<Integer> tree = new TreeSet<>(Arrays.asList(7, 3, 9, 1, 5));
        System.out.println("TreeSet (sorted): " + tree);
        System.out.println("first=" + tree.first() + " last=" + tree.last());
        System.out.println("floor(6)=" + tree.floor(6) + " ceiling(6)=" + tree.ceiling(6));
        System.out.println("lower(3)=" + tree.lower(3) + " higher(3)=" + tree.higher(3));
        System.out.println("headSet(5, true)=" + tree.headSet(5, true));
        System.out.println("tailSet(5, false)=" + tree.tailSet(5, false));
        System.out.println("subSet(3, true, 7, false)=" + tree.subSet(3, true, 7, false));

        // TreeSet with Comparator
        TreeSet<Person> peopleByName = new TreeSet<>(
                Comparator.comparing(Person::getName).thenComparingInt(Person::getId));
        peopleByName.addAll(Arrays.asList(
                new Person(2, "Bob"),
                new Person(1, "Alice"),
                new Person(3, "Bob")
        ));
        System.out.println("TreeSet<Person> by name, then id: " + peopleByName);
    }

    // ---------- Queue and Deque ----------
    private static void demoQueueAndDeque() {
        // Queue (FIFO) via LinkedList
        Queue<String> q = new LinkedList<>();
        q.offer("A"); // offer returns false instead of throwing if it fails
        q.offer("B");
        System.out.println("Queue peek=" + q.peek() + " poll=" + q.poll() + " after poll: " + q);
        q.clear();
        System.out.println("Queue remove on empty throws:");
        try {
            ((LinkedList<String>) q).remove(); // remove throws NoSuchElementException if empty
        } catch (NoSuchElementException e) {
            System.out.println("remove() -> " + e.getClass().getSimpleName());
        }
        System.out.println("poll on empty returns: " + q.poll());

        // PriorityQueue: orders by priority (natural order default)
        PriorityQueue<Integer> minHeap = new PriorityQueue<>();
        minHeap.addAll(Arrays.asList(5, 1, 4, 3, 2));
        System.out.print("PriorityQueue (min-heap) poll order: ");
        while (!minHeap.isEmpty()) System.out.print(minHeap.poll() + " ");
        System.out.println();

        PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Comparator.reverseOrder());
        maxHeap.addAll(Arrays.asList(5, 1, 4, 3, 2));
        System.out.print("PriorityQueue (max-heap) poll order: ");
        while (!maxHeap.isEmpty()) System.out.print(maxHeap.poll() + " ");
        System.out.println();

        // Deque: double-ended queue, also a stack replacement
        Deque<String> dq = new ArrayDeque<>();
        dq.addFirst("front");
        dq.addLast("back");
        System.out.println("Deque: " + dq + " peekFirst=" + dq.peekFirst() + " peekLast=" + dq.peekLast());
        dq.removeFirst();
        dq.removeLast();
        dq.push("stack1"); // push/pop mimic Stack
        dq.push("stack2");
        System.out.println("Stack via Deque pop: " + dq.pop() + ", then " + dq.pop());
    }

    // ---------- Map ----------
    private static void demoMap() {
        // HashMap: O(1) average-time operations, no order
        Map<Integer, String> map = new HashMap<>();
        map.put(1, "A");
        map.put(2, "B");
        String old = map.put(2, "B2"); // returns previous value
        System.out.println("HashMap: " + map + " (replaced '" + old + "' at key 2)");
        System.out.println("get(3) -> " + map.get(3) + " getOrDefault(3,'NA')-> " + map.getOrDefault(3, "NA"));

        map.putIfAbsent(3, "C");
        map.computeIfAbsent(4, k -> "Computed-" + k);
        map.merge(2, "-merge", (prev, val) -> prev + val);
        System.out.println("After putIfAbsent/computeIfAbsent/merge: " + map);

        // Iteration
        System.out.print("Entries: ");
        for (Map.Entry<Integer, String> e : map.entrySet()) {
            System.out.print(e.getKey() + "=" + e.getValue() + " ");
        }
        System.out.println();

        // Views are backed by the map
        Set<Integer> keys = map.keySet();
        keys.remove(1); // removes from the map
        System.out.println("After keys.remove(1): map=" + map);

        // LinkedHashMap: predictable iteration order, accessOrder=true for LRU-like order
        LinkedHashMap<Integer, String> lhm = new LinkedHashMap<>(16, 0.75f, true);
        lhm.put(1, "one");
        lhm.put(2, "two");
        lhm.put(3, "three");
        // Access key 2, then iterate to see it moved to end
        lhm.get(2);
        System.out.println("LinkedHashMap (access order): " + lhm.keySet());

        // TreeMap: sorted map, NavigableMap operations
        NavigableMap<Integer, String> tmap = new TreeMap<>();
        tmap.put(10, "ten");
        tmap.put(5, "five");
        tmap.put(20, "twenty");
        tmap.put(15, "fifteen");
        System.out.println("TreeMap: " + tmap);
        System.out.println("firstEntry=" + tmap.firstEntry() + " lastEntry=" + tmap.lastEntry());
        System.out.println("floorEntry(12)=" + tmap.floorEntry(12) + " ceilingEntry(12)=" + tmap.ceilingEntry(12));
        System.out.println("lowerEntry(15)=" + tmap.lowerEntry(15) + " higherEntry(15)=" + tmap.higherEntry(15));
        System.out.println("subMap(5,true,15,false)=" + tmap.subMap(5, true, 15, false));

        // Note: Map.of and List.of create immutable collections
        Map<String, Integer> small = Map.of("a", 1, "b", 2);
        System.out.println("Map.of immutable: " + small);
        try {
            small.put("c", 3);
        } catch (UnsupportedOperationException e) {
            System.out.println("Map.of put throws " + e.getClass().getSimpleName());
        }
    }

    // ---------- Views and wrappers ----------
    private static void demoViewsAndWrappers() {
        // Unmodifiable view
        List<String> modifiable = new ArrayList<>(Arrays.asList("A", "B"));
        List<String> unmodifiable = Collections.unmodifiableList(modifiable);
        System.out.println("Unmodifiable view: " + unmodifiable);
        try {
            unmodifiable.add("C");
        } catch (UnsupportedOperationException e) {
            System.out.println("unmodifiable.add throws " + e.getClass().getSimpleName());
        }
        modifiable.add("C"); // underlying change is visible
        System.out.println("After changing source: " + unmodifiable);

        // Synchronized wrapper (serialize access; still need external synchronization for iteration)
        List<Integer> syncList = Collections.synchronizedList(new ArrayList<>());
        syncList.addAll(Arrays.asList(1, 2, 3));
        synchronized (syncList) {
            System.out.print("SynchronizedList iterate: ");
            for (int v : syncList) System.out.print(v + " ");
            System.out.println();
        }

        // Checked wrapper (runtime type check for legacy/raw usage)
        List<String> checked = Collections.checkedList(new ArrayList<>(), String.class);
        checked.add("ok");
        @SuppressWarnings({"rawtypes", "unchecked"})
        List raw = checked; // simulate raw usage
        try {
            raw.add(123); // ClassCastException at runtime
        } catch (ClassCastException e) {
            System.out.println("checkedList rejects wrong type: " + e.getClass().getSimpleName());
        }
    }

    // ---------- Wildcards and generics ----------
    private static void demoWildcardsAndGenerics() {
        List<Integer> ints = Arrays.asList(1, 2, 3);
        List<Double> doubles = Arrays.asList(1.5, 2.5);
        List<Object> out = new ArrayList<>();

        // Producer extends: read from ? extends Number
        double total = sum(ints) + sum(doubles);
        System.out.println("Sum ints+doubles = " + total);

        // Consumer super: write to ? super Number
        copy(ints, out);
        copy(doubles, out);
        System.out.println("Copied numbers into ? super Number list: " + out);

        // Lower bound: can add Integer into ? super Integer
        List<Number> numbers = new ArrayList<>();
        addIntegers(numbers, Arrays.asList(7, 8, 9));
        System.out.println("After addIntegers into List<Number>: " + numbers);
    }

    private static double sum(Collection<? extends Number> src) {
        double s = 0.0;
        for (Number n : src) s += n.doubleValue();
        return s;
    }

    private static void copy(Collection<? extends Number> src, Collection<? super Number> dst) {
        for (Number n : src) dst.add(n);
    }

    private static void addIntegers(Collection<? super Integer> dst, Collection<Integer> src) {
        dst.addAll(src);
    }

    // ---------- equals and hashCode ----------
    private static void demoEqualsAndHashCode() {
        // Correct equals/hashCode: set/map treat equal ids as same element/key
        Set<Person> persons = new HashSet<>();
        persons.add(new Person(1, "Alice"));
        persons.add(new Person(1, "Alicia")); // same id => equal
        persons.add(new Person(2, "Bob"));
        System.out.println("HashSet<Person> (equals/hashCode by id): " + persons);

        Map<Person, String> personMap = new HashMap<>();
        personMap.put(new Person(1, "Alice"), "first");
        personMap.put(new Person(1, "Alicia"), "overwrites"); // same key (id)
        System.out.println("HashMap<Person,String> size=" + personMap.size() + " " + personMap);

        // Missing equals/hashCode: duplicates remain in HashSet
        Set<BadPerson> bads = new HashSet<>();
        bads.add(new BadPerson(1, "Alice"));
        bads.add(new BadPerson(1, "Alice"));
        System.out.println("HashSet<BadPerson> (no equals/hash) size=" + bads.size() + " => duplicates kept");
    }

    // ---------- Helper classes ----------
    static final class Person {
        private final int id;
        private final String name;

        Person(int id, String name) {
            this.id = id;
            this.name = name;
        }
        int getId() { return id; }
        String getName() { return name; }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Person)) return false;
            Person p = (Person) o;
            return id == p.id;
        }
        @Override public int hashCode() { return Integer.hashCode(id); }
        @Override public String toString() { return "Person{" + id + "," + name + "}"; }
    }

    static final class BadPerson {
        private final int id;
        private final String name;

        BadPerson(int id, String name) {
            this.id = id;
            this.name = name;
        }
        @Override public String toString() { return "BadPerson{" + id + "," + name + "}"; }
        // equals/hashCode intentionally not overridden
    }
}