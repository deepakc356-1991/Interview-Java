package _05_02_wildcards_and_bounds;

import java.util.*;
import java.util.function.Consumer;

/**
 * Interview Q&A: Wildcards and Bounds (? extends / ? super)
 *
 * This file mixes concise Q&A with runnable examples from basic to advanced.
 * Focus: invariance, covariance/contravariance via wildcards, PECS, capture, API design.
 */
public class _03_InterviewQA {

    public static void main(String[] args) {
        q1_wildcardBasics();
        q2_listObjectVsWildcard();
        q3_upperBoundReadOnly();
        q4_lowerBoundWriteOnly();
        q5_PECS_copy();
        q6_maxAndMinExamples();
        q7_wildcardCaptureSwap();
        q8_moveAllBetween();
        q9_rawVsWildcardRuntimeHazard();
        q10_arraysVsGenerics();
        q11_stackProducerConsumer();
        q12_comparatorContravariance();
        System.out.println("Done.");
    }

    // Q: What is a wildcard in Java generics?
    // A: It's an unknown type placeholder used at use-site: ?, ? extends T (upper bound), ? super T (lower bound).
    //    Helps accept a family of parameterized types without reifying a single exact type.
    static void q1_wildcardBasics() {
        List<?> any = Arrays.asList("A", "B", "C");    // unbounded wildcard
        for (Object o : any) {                          // you can only read as Object
            // System.out.println(o);
        }
        // any.add("X");         // compile error: cannot add to List<?>
        // any.add(1);           // compile error
        // any.add(null);        // the only value you can add is null (but generally avoid)
    }

    // Q: Difference between List<Object> and List<?>?
    // A:
    // - List<Object> means "a list that can hold Objects", requires exactly List<Object>.
    // - List<?> means "a list of some unknown type", accepts List<String>, List<Integer>, etc.
    static void q2_listObjectVsWildcard() {
        List<String> strings = new ArrayList<>(Arrays.asList("a", "b"));

        showListAny(strings);     // OK: List<?> accepts List<String>
        // showListObject(strings); // compile error: List<Object> != List<String>

        List<Object> objects = new ArrayList<>();
        showListObject(objects);  // OK
    }

    static void showListAny(List<?> list) {
        for (Object o : list) {
            // read-only view
        }
        // list.add(new Object()); // cannot add
    }

    static void showListObject(List<Object> list) {
        list.add("can add string");
        list.add(123);
    }

    // Q: What is an upper-bounded wildcard (? extends T)? When to use it?
    // A: Use ? extends T when the parameter is a producer of T values (read), not a consumer (write).
    //    You can read safely as T, cannot add anything but null.
    static void q3_upperBoundReadOnly() {
        List<Integer> ints = Arrays.asList(1, 2, 3);
        List<Double> doubles = Arrays.asList(1.5, 2.5);

        double s1 = sum(ints);
        double s2 = sum(doubles);
        // System.out.println(s1 + " " + s2);

        List<? extends Number> nums = new ArrayList<Integer>();
        // nums.add(10);    // compile error
        // nums.add(3.14);  // compile error
        // nums.add(null);  // allowed but discouraged
    }

    static double sum(Collection<? extends Number> nums) {
        double s = 0.0;
        for (Number n : nums) s += n.doubleValue();
        return s;
    }

    // Q: What is a lower-bounded wildcard (? super T)? When to use it?
    // A: Use ? super T when the parameter is a consumer (sink) of T values (write).
    //    You can add T (and its subtypes), but reads come out as Object.
    static void q4_lowerBoundWriteOnly() {
        List<Number> nums = new ArrayList<>();
        addSomeIntegers(nums);
        // for (Object o : nums) { /* read as Object */ }

        List<Object> objs = new ArrayList<>();
        addSomeIntegers(objs); // also valid: Object is a super of Integer
    }

    static void addSomeIntegers(Collection<? super Integer> dest) {
        dest.add(42);
        dest.add(7);
        // Integer x = dest.iterator().next(); // compile error: only safe to read as Object
        Object first = dest.iterator().next(); // OK
    }

    // Q: What is PECS?
    // A: Producer Extends, Consumer Super.
    //    - If your parameter produces T values for you to read: ? extends T
    //    - If your parameter consumes T values you provide:     ? super T
    static void q5_PECS_copy() {
        List<Integer> src = Arrays.asList(10, 20, 30);
        List<Number> dst1 = new ArrayList<>();
        List<Object> dst2 = new ArrayList<>();

        copy(src, dst1); // src produces Integers, dst1 consumes Numbers (super of Integer)
        copy(src, dst2); // src produces Integers, dst2 consumes Objects (super of Integer)
    }

    static <T> void copy(Collection<? extends T> src, Collection<? super T> dest) {
        for (T t : src) dest.add(t);
    }

    // Q: Why can't you add to List<? extends Number>?
    // A: The exact type could be List<Integer>, List<Double>, etc. The compiler can't guarantee type safety.
    //    Only null is universally legal.

    // Q: How to implement max/min correctly with Comparable?
    // A: Use: <T extends Comparable<? super T>> to allow comparing T with its supertypes of Comparable.
    static void q6_maxAndMinExamples() {
        List<Integer> ints = Arrays.asList(5, 2, 9, 1);
        Integer mx = max(ints);
        Integer mn = min(ints);
        // System.out.println(mx + " / " + mn);
    }

    public static <T extends Comparable<? super T>> T max(List<? extends T> list) {
        if (list == null || list.isEmpty()) throw new IllegalArgumentException("empty");
        T best = list.get(0);
        for (int i = 1; i < list.size(); i++) {
            T next = list.get(i);
            if (next.compareTo(best) > 0) best = next;
        }
        return best;
    }

    public static <T extends Comparable<? super T>> T min(List<? extends T> list) {
        if (list == null || list.isEmpty()) throw new IllegalArgumentException("empty");
        T best = list.get(0);
        for (int i = 1; i < list.size(); i++) {
            T next = list.get(i);
            if (next.compareTo(best) < 0) best = next;
        }
        return best;
    }

    // Q: What is wildcard capture? How do you mutate a List<?> safely?
    // A: The compiler treats ? as an unknown type variable. To write, use a helper method with a type parameter.
    static void q7_wildcardCaptureSwap() {
        List<String> a = new ArrayList<>(Arrays.asList("x", "y", "z"));
        swap(a, 0, 2); // works via capture helper
        reverse(a);
    }

    public static void swap(List<?> list, int i, int j) {
        swapHelper(list, i, j);
    }

    private static <T> void swapHelper(List<T> list, int i, int j) {
        T tmp = list.get(i);
        list.set(i, list.get(j));
        list.set(j, tmp);
    }

    public static void reverse(List<?> list) {
        reverseHelper(list, 0, list.size() - 1);
    }

    private static <T> void reverseHelper(List<T> list, int i, int j) {
        while (i < j) swapHelper(list, i++, j--);
    }

    // Q: When to use a wildcard vs a type parameter?
    // A:
    // - Use a wildcard when you don't need to relate multiple parameters/return types.
    // - Use a type parameter when two or more arguments/return types must be the same unknown type.
    static void q8_moveAllBetween() {
        List<Integer> src = new ArrayList<>(Arrays.asList(1, 2, 3));
        List<Number> dst = new ArrayList<>();
        moveAll(src, dst);
        // src now empty, dst has 1,2,3
    }

    public static <T> void moveAll(List<? extends T> src, List<? super T> dst) {
        for (T t : src) dst.add(t);
        src.clear();
    }

    // Q: Raw types vs wildcards?
    // A:
    // - Raw types disable generic checks (unsafe), can cause runtime ClassCastException.
    // - Wildcards retain type safety (compile-time enforced).
    static void q9_rawVsWildcardRuntimeHazard() {
        List<String> strings = new ArrayList<>();
        strings.add("safe");
        List raw = strings; // raw type erases generics
        raw.add(123);       // compiles, breaks type safety

        try {
            String s = strings.get(1); // runtime ClassCastException
        } catch (ClassCastException e) {
            // System.out.println("Caught: " + e);
        }

        // Safer alternative: List<?> prevents adding:
        List<?> any = strings;
        // any.add(123); // compile error, so no runtime hazard
    }

    // Q: Arrays vs generics (variance)?
    // A:
    // - Arrays are covariant: Number[] a = new Integer[...]; but may throw ArrayStoreException at runtime.
    // - Generics are invariant: List<Number> is NOT a supertype of List<Integer> (prevents runtime failures).
    static void q10_arraysVsGenerics() {
        Number[] arr = new Integer[2];
        arr[0] = 1; // OK
        try {
            arr[1] = 2.5; // compiles, but runtime ArrayStoreException
        } catch (ArrayStoreException ignored) {
        }

        // List<Number> list = new ArrayList<Integer>(); // compile error: invariance prevents unsafe assignment
    }

    // Q: Real-world API design with extends/super (Effective Java Stack example)?
    // A:
    // - pushAll(Iterable<? extends E>) for producers
    // - popAll(Collection<? super E>) for consumers
    static void q11_stackProducerConsumer() {
        Stack<Integer> stack = new Stack<>();
        stack.pushAll(Arrays.asList(1, 2, 3));     // producer: ? extends Integer
        List<Number> out = new ArrayList<>();
        stack.popAll(out);                         // consumer: ? super Integer
    }

    static class Stack<E> {
        private final Deque<E> data = new ArrayDeque<>();
        public void push(E e) { data.push(e); }
        public E pop() { return data.pop(); }
        public boolean isEmpty() { return data.isEmpty(); }

        public void pushAll(Iterable<? extends E> src) {
            for (E e : src) push(e);
        }
        public void popAll(Collection<? super E> dst) {
            while (!isEmpty()) dst.add(pop());
        }
    }

    // Q: Why Comparator/Comparable params are contravariant with super?
    // A:
    // - Comparator<? super T>: a comparator of T or any supertype can compare T instances.
    // - Comparable<? super T>: T can be compared to its supertypes.
    static void q12_comparatorContravariance() {
        List<Integer> ints = new ArrayList<>(Arrays.asList(3, 1, 2));
        sort(ints, naturalOrder());           // Comparator<? super Integer>
        sort(ints, numberComparator());       // Comparator<? super Integer> via Comparator<Number>
    }

    public static <T> void sort(List<T> list, Comparator<? super T> cmp) {
        list.sort(cmp);
    }

    static Comparator<Integer> naturalOrder() {
        return Integer::compareTo;
    }

    static Comparator<Number> numberComparator() {
        return (a, b) -> Double.compare(a.doubleValue(), b.doubleValue());
    }

    // Additional concise Q&A (comments only):
    // Q: Can wildcards have multiple bounds?
    // A: No. Wildcards support a single bound (? extends T or ? super T).
    //
    // Q: "extends" in generics vs classes?
    // A: In generics, extends means "extends or implements": ? extends Number, ? extends Runnable are both valid.
    //
    // Q: Can you return a wildcard type?
    // A: You can, but it's often not useful to callers (they can't add). Prefer type parameters when returning.
    //
    // Q: Can you declare fields with wildcards?
    // A: Yes, but they restrict usage (e.g., List<? extends Number> field becomes effectively read-only).
    //
    // Q: When a parameter is used both for reading and writing?
    // A: Prefer an exact type parameter, or split into two params with extends/super as needed.
    //
    // Q: Is List<?> the same as List<? extends Object>?
    // A: Yes. They are equivalent.
    //
    // Q: Can you instantiate a wildcard type?
    // A: You can instantiate the raw generic type (e.g., new ArrayList<Integer>()), but not new ArrayList<?>().
    //
    // Q: Why can't we do new T[] with generics?
    // A: Due to type erasure and array reification; use List<T> or @SuppressWarnings with care.
    //
    // Q: Overload resolution with wildcards?
    // A: Ambiguities can arise; keep APIs simple and prefer type parameters when two methods would be too similar.
    //
    // Q: Unbounded wildcard vs raw type?
    // A: Prefer List<?> over raw List. Wildcards keep type safety; raw loses it.
}