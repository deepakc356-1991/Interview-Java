package _05_01_generic_classes_and_methods;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.UnaryOperator;

/**
 * Interview Q&A walkthrough: Generic Classes & Methods (basic -> advanced)
 *
 * Read the comments next to each example. The main method runs small demos.
 *
 * Topics covered:
 * - What and why: Generics (type-safety, reusability, expressiveness)
 * - Generic classes, interfaces, and methods
 * - Type inference and the diamond operator
 * - Bounded type parameters (extends, super, multiple bounds, recursive bounds)
 * - Wildcards (unbounded, upper-bounded, lower-bounded) and PECS (Producer Extends, Consumer Super)
 * - Invariance, covariance vs arrays, and why List<Integer> is not a List<Number>
 * - Type erasure: implications, instanceof, reflection, bridge methods
 * - Raw types and heap pollution
 * - Generic arrays, varargs, and @SafeVarargs
 * - Restrictions: cannot new T(), cannot catch generic exceptions, no overloaded methods differing only by generic params
 * - Type tokens (Class<T>) and generic factories
 * - Wildcard capture helper methods
 * - Effective patterns: copy with extends/super, identity singleton, comparator contravariance
 */
public class _03_InterviewQA {

    public static void main(String[] args) {
        // 1) Generic class basics
        Box<String> sb = new Box<>();
        sb.set("Hello");
        System.out.println("Box<String> = " + sb.get());

        // 2) Generic factory methods
        Pair<Integer, String> p = Pair.of(1, "one");
        System.out.println("Pair.of = " + p);

        // 3) Generic method with inference
        Integer first = Utils.firstOrDefault(new Integer[]{}, 42);
        System.out.println("firstOrDefault = " + first);

        Number picked = Utils.pick(10, 2.5); // T inferred as Number (common supertype)
        System.out.println("pick(10,2.5) -> " + picked + " (" + picked.getClass().getSimpleName() + ")");

        // 4) Diamond operator inference
        List<String> names = new ArrayList<>();
        names.add("A");
        names.add("B");
        System.out.println("names = " + names);

        // 5) Bounded generics
        NumberBox<Integer> nb = new NumberBox<>(Arrays.asList(1, 2, 3));
        System.out.println("NumberBox<Integer>.sum = " + nb.sum());

        // 6) Wildcards and PECS
        List<Integer> ints = new ArrayList<>(Arrays.asList(1, 2, 3));
        double s = Utils.sumOfList(ints); // ? extends Number (producer)
        System.out.println("sumOfList = " + s);

        List<Number> nums = new ArrayList<>();
        Utils.addIntegers(nums); // ? super Integer (consumer)
        System.out.println("after addIntegers -> " + nums);

        Utils.printList(nums); // ? (unbounded)

        // 7) Invariance (List<Integer> is NOT a List<Number>)
        List<Number> onlyNumbers = new ArrayList<>();
        onlyNumbers.add(1);
        Utils.acceptNumbers(onlyNumbers);
        // The following does NOT compile:
        // Utils.acceptNumbers(ints);

        // 8) Wildcard capture helper (swap via helper method)
        List<String> list = new ArrayList<>(Arrays.asList("x", "y", "z"));
        Utils.swapWildcard(list, 0, 1);
        System.out.println("swapWildcard -> " + list);

        // 9) Raw types pitfall (heap pollution -> ClassCastException at runtime)
        try {
            rawTypePitfallDemo();
        } catch (ClassCastException e) {
            System.out.println("Raw type demo -> ClassCastException caught: " + e.getMessage());
        }

        // 10) Type erasure demo
        List<String> ls = new ArrayList<>();
        List<Integer> li = new ArrayList<>();
        System.out.println("Erasure: ls.getClass()==li.getClass() -> " + (ls.getClass() == li.getClass()));
        // Illegal: if (ls instanceof List<String>) { } // does not compile

        // 11) Arrays are covariant (unsafe) vs Generics (invariant, safe)
        try {
            Object[] oa = new String[1];  // arrays are covariant
            oa[0] = new StringBuilder();  // ArrayStoreException at runtime
        } catch (ArrayStoreException ex) {
            System.out.println("ArrayStoreException (arrays are covariant): " + ex.getMessage());
        }

        // 12) @SafeVarargs for safe generic varargs
        List<String> safe = Utils.asListSafe("a", "b", "c");
        System.out.println("asListSafe -> " + safe);

        // 13) Type tokens: create arrays/instances of T
        Integer[] iarr = Utils.newArray(Integer.class, 3);
        System.out.println("newArray(Integer.class,3) length -> " + iarr.length);
        try {
            Sample created = Utils.newInstance(Sample.class);
            System.out.println("newInstance -> " + created);
        } catch (RuntimeException e) {
            System.out.println("newInstance failed: " + e.getMessage());
        }

        // 14) Multiple bounds and recursive bounds
        Integer max = Utils.max(10, 42);
        System.out.println("max<Integer> -> " + max);
        List<Integer> toMin = Arrays.asList(5, 3, 7);
        System.out.println("min -> " + Utils.min(toMin));

        // 15) Contravariant comparator (? super T)
        List<Integer> sortable = new ArrayList<>(Arrays.asList(3, 1, 2));
        Comparator<Number> cmp = Comparator.comparingDouble(Number::doubleValue); // Comparator<? super Integer>
        Utils.sortWithComparator(sortable, cmp);
        System.out.println("sortWithComparator -> " + sortable);

        // 16) Copy with extends/super (PECS in practice)
        List<Integer> src = Arrays.asList(7, 8, 9);
        List<Number> dst = new ArrayList<>();
        Utils.copy(src, dst);
        System.out.println("copy extends/super -> " + dst);

        // 17) Generic interface example
        Repository<Person> repo = new InMemoryRepository<>();
        repo.save(new Person("Ada"));
        repo.save(new Person("Linus"));
        System.out.println("Repository<Person>.findAll -> " + repo.findAll());

        // 18) Bridge methods (type erasure) demo via comments in Node/MyIntegerNode. See classes below.
        Node<Integer> node = new MyIntegerNode(123);
        System.out.println("MyIntegerNode.get -> " + node.get());

        // 19) Generic singleton factory (Effective Java pattern)
        UnaryOperator<String> idStr = Identity.identityFunction();
        UnaryOperator<Integer> idInt = Identity.identityFunction();
        System.out.println("identityFunction -> " + idStr.apply("ok") + ", " + idInt.apply(99));
    }

    // ========== BASIC: Generic Classes, Methods, Interfaces ==========

    /**
     * Q: What are generics and why use them?
     * A:
     * - Compile-time type safety (fewer ClassCastException at runtime)
     * - Better code reusability (write once, use many types)
     * - Self-documenting APIs (express constraints in signatures)
     */
    public static class Box<T> { // Generic class with type parameter T
        private T value;
        public void set(T value) { this.value = value; }
        public T get() { return value; }
        @Override public String toString() { return "Box[" + value + "]"; }
    }

    /**
     * Q: Difference between generic class and generic method?
     * A:
     * - Generic class: type parameter on the class (e.g., class Box<T>).
     * - Generic method: type parameter on the method (e.g., static <T> T pick(T a, T b)).
     * - Static methods cannot use the class's type parameter; they must declare their own if needed.
     */
    public static final class Utils {
        public static <T> T firstOrDefault(T[] arr, T defaultValue) {
            return (arr == null || arr.length == 0) ? defaultValue : arr[0];
        }

        public static <T> T pick(T a, T b) {
            // T inferred by the call site (e.g., Number for (10, 2.5))
            return a != null ? a : b;
        }

        // PECS: Producer Extends, Consumer Super
        public static double sumOfList(List<? extends Number> list) {
            double sum = 0;
            for (Number n : list) sum += n.doubleValue();
            return sum;
        }
        public static void addIntegers(List<? super Integer> list) {
            list.add(1);
            list.add(2);
        }
        public static void printList(List<?> list) {
            for (Object e : list) System.out.println(" - " + e);
        }

        // Invariance example (List<Integer> != List<Number>)
        public static void acceptNumbers(List<Number> numbers) {
            numbers.add(3.14);
        }

        // Wildcard capture example: swap via helper
        public static void swapWildcard(List<?> list, int i, int j) {
            swapCapture(list, i, j); // capture helper infers T
        }
        private static <T> void swapCapture(List<T> list, int i, int j) {
            T tmp = list.get(i);
            list.set(i, list.get(j));
            list.set(j, tmp);
        }

        // @SafeVarargs: use only when implementation is provably safe
        @SafeVarargs
        public static <T> List<T> asListSafe(T... items) {
            // We don't store the array into a generic var that could expose it; safe usage.
            return new ArrayList<>(Arrays.asList(items));
        }

        // Type token helpers (overcome erasure for new T[], new T())
        public static <T> T[] newArray(Class<T> type, int length) {
            @SuppressWarnings("unchecked")
            T[] arr = (T[]) Array.newInstance(type, length);
            return arr;
        }
        public static <T> T newInstance(Class<T> type) {
            try {
                return type.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        // Multiple bounds: T must be a Number and Comparable<T>
        public static <T extends Number & Comparable<T>> T max(T a, T b) {
            return (a.compareTo(b) >= 0) ? a : b;
        }

        // Recursive bound: typical Comparable pattern
        public static <T extends Comparable<? super T>> T min(Collection<T> coll) {
            Iterator<T> it = coll.iterator();
            if (!it.hasNext()) throw new NoSuchElementException();
            T best = it.next();
            while (it.hasNext()) {
                T next = it.next();
                if (next.compareTo(best) < 0) best = next;
            }
            return best;
        }

        // Contravariant comparator usage: Comparator<? super T>
        public static <T> void sortWithComparator(List<T> list, Comparator<? super T> comparator) {
            list.sort(comparator);
        }

        // Copy using PECS (src produces T, dest consumes T)
        public static <T> void copy(List<? extends T> src, List<? super T> dest) {
            for (T t : src) dest.add(t);
        }
    }

    /**
     * Q: Generic interface example?
     * A: Define type parameter on the interface and implement with a concrete T.
     */
    public interface Repository<T> {
        void save(T t);
        List<T> findAll();
    }
    public static class InMemoryRepository<T> implements Repository<T> {
        private final List<T> store = new ArrayList<>();
        public void save(T t) { store.add(t); }
        public List<T> findAll() { return Collections.unmodifiableList(store); }
    }

    /**
     * Q: Bounded type parameter on a class?
     * A: Constrain T so only Numbers are allowed.
     */
    public static class NumberBox<T extends Number> {
        private final List<T> list;
        public NumberBox(List<T> list) { this.list = list; }
        public double sum() {
            double s = 0;
            for (T t : list) s += t.doubleValue();
            return s;
        }
    }

    // ========== WILDCARDS, INVARIANCE, PECS ==========

    /**
     * Q: Why List<Integer> is not a List<Number>?
     * A:
     * - Generics are invariant. Allowing List<Integer> where List<Number> is expected would let you add Double into a List<Integer>.
     * - Use wildcards (? extends Number to read; ? super Integer to write).
     */

    // ========== TYPE ERASURE, BRIDGE METHODS, RESTRICTIONS ==========

    /**
     * Q: What is type erasure?
     * A:
     * - Generic type information is erased at compile time.
     * - Effects:
     *   1) No overloading by parameterized type (erasure conflict).
     *   2) Cannot use instanceof with parameterized types (only raw, e.g., List).
     *   3) Bridge methods may be synthesized by the compiler to preserve polymorphism.
     *
     * The following would NOT compile due to erasure:
     *   void f(List<String> x) {}
     *   void f(List<Integer> x) {} // erasure conflict (both are f(List))
     *
     * Also illegal:
     *   if (x instanceof List<String>) { } // cannot test with parameterized type
     */
    public static class Node<T> {
        T value;
        Node(T value) { this.value = value; }
        T get() { return value; }
    }
    public static class MyIntegerNode extends Node<Integer> {
        MyIntegerNode(Integer value) { super(value); }
        @Override Integer get() { return super.get(); }
        // The compiler generates a synthetic bridge method get():Object delegating to get():Integer after erasure
    }

    // ========== RAW TYPES AND HEAP POLLUTION ==========

    private static void rawTypePitfallDemo() {
        @SuppressWarnings({"rawtypes", "unchecked"})
        Box raw = new Box(); // raw type loses type-safety
        raw.set("abc");      // stores a String
        @SuppressWarnings("unchecked")
        Box<Integer> ints = raw; // unchecked assignment
        // Next line compiles but fails at runtime due to inserted cast on return
        Integer x = ints.get();  // ClassCastException here
        System.out.println(x);
    }

    // ========== ARRAYS OF GENERIC TYPES AND VARARGS ==========

    /**
     * Q: Why no generic arrays (e.g., new List<String>[10])?
     * A:
     * - Arrays are covariant and reified (runtime type is known), generics are invariant and erased.
     * - Mixing them allows unsoundness; thus creation is disallowed.
     *
     * Illegal examples:
     *   List<String>[] arr = new List<String>[10]; // compile-time error
     *   T[] a = new T[10]; // cannot create a generic array
     */

    /**
     * Q: Are generic varargs safe?
     * A:
     * - Varargs are arrays, and arrays + generics can cause heap pollution.
     * - If a varargs generic method is provably safe, annotate with @SafeVarargs.
     *
     * Unsafe example (do NOT do this):
     *   @SafeVarargs
     *   static void dangerous(List<String>... lists) {
     *       Object[] o = lists;
     *       o[0] = Arrays.asList(42); // heap pollution
     *       String s = lists[0].get(0); // ClassCastException
     *   }
     */

    // ========== TYPE TOKENS AND FACTORIES ==========

    /**
     * Q: How to create instances/arrays of T (since new T() is illegal)?
     * A:
     * - Pass a Class<T> (type token) and use reflection or Array.newInstance.
     */

    // ========== MORE PATTERNS AND FAQ ==========

    /**
     * Q: Generic singleton factory?
     * A:
     * - Store a single Object-typed instance and cast on demand.
     */
    public static final class Identity {
        private static final UnaryOperator<Object> ID = x -> x;
        @SuppressWarnings("unchecked")
        public static <T> UnaryOperator<T> identityFunction() {
            return (UnaryOperator<T>) ID;
        }
    }

    /**
     * Q: Common restrictions to remember?
     * A:
     * - Cannot use primitives as type args (use wrappers).
     * - Cannot instantiate type parameters: new T(), T.class (use type tokens).
     * - Cannot create arrays of parameterized types: new List<String>[10].
     * - Cannot catch or throw generic type variables: catch (T e) is illegal; generic classes cannot directly extend Throwable.
     * - Cannot overload methods that differ only in generic parameterization (erasure conflict).
     * - Static members cannot use a class's type parameter unless they declare their own type parameter.
     */

    // ========== SUPPORT TYPES ==========

    public static class Person {
        private final String name;
        public Person(String name) { this.name = name; }
        @Override public String toString() { return "Person(" + name + ")"; }
    }

    public static class Sample {
        @Override public String toString() { return "Sample{}"; }
    }

    /**
     * Q: Self-bounded generics pattern (like Enum, Comparable)?
     * A:
     * - Constrains a type parameter to be a subtype of the type itself.
     *   class Self<T extends Self<T>> { }
     * - Often used with Comparable: T extends Comparable<? super T>
     */
    public static class SelfComparable<T extends SelfComparable<T>> implements Comparable<T> {
        @Override public int compareTo(T o) { return 0; }
    }

    /**
     * Q: Why prefer List<?> over raw List?
     * A:
     * - List<?> preserves type-safety (read-only except null) vs raw List loses it.
     */
}