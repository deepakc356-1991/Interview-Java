package _05_01_generic_classes_and_methods;

import java.util.*;
import java.util.function.Function;

public class _02_Examples {

    public static void main(String[] args) {
        // 1) Generic class Box<T>
        Box<String> strBox = new Box<>("hello");
        System.out.println(strBox.get().toUpperCase());

        // Diamond operator and type inference
        Box<Integer> intBox = new Box<>(42);
        System.out.println(intBox);

        // Generic instance method map (method has its own <R>)
        Box<Integer> lenBox = strBox.map(String::length);
        System.out.println(lenBox);

        // 2) Multiple type parameters: Pair<K,V> and static factory
        Pair<String, Integer> age = Pair.of("age", 30);
        System.out.println(age);
        System.out.println(Pair.swap(age));

        // 3) Bounded type parameter class: NumberBox<T extends Number>
        NumberBox<Integer> nb = new NumberBox<>();
        nb.add(1); nb.add(2); nb.add(3);
        System.out.println("sum=" + nb.sum());

        // 4) Generic methods
        List<String> words = Arrays.asList("alpha", "beta", "gamma");
        System.out.println("first=" + Utils.first(words));
        System.out.println("identity=" + Utils.identity(123));
        System.out.println("identity explicit=" + Utils.<String>identity("explicit"));

        // 5) Wildcards: extends (producer) and super (consumer)
        double sum = Utils.sumOfList(Arrays.asList(1, 2.5, 3L)); // List<Number> fits <? extends Number>
        System.out.println("sumOfList=" + sum);
        List<Number> nums = new ArrayList<>();
        Utils.addInteger(nums, 10); // List<? super Integer>
        System.out.println("nums=" + nums);
        Utils.copy(Arrays.asList(7, 8, 9), nums); // copy from producer to consumer
        System.out.println("nums(after copy)=" + nums);

        // 6) Unbounded wildcard and wildcard capture
        Utils.printAnyList(words);
        Utils.reverse(words); // capture helper allows mutation
        System.out.println("reversed=" + words);

        // 7) Max with recursive bound and multiple bounds via clamp
        System.out.println("max=" + Utils.max(Arrays.asList(2, 9, 3)));
        System.out.println("clamp int=" + Utils.clamp(5, 1, 4));
        System.out.println("clamp double=" + Utils.clamp(5.5, 1.0, 4.0));

        // 8) Generic varargs
        List<String> listFromVarargs = Utils.varargsToList("x", "y", "z");
        System.out.println(listFromVarargs);

        // 9) Generic method swap
        Utils.swap(listFromVarargs, 0, 2);
        System.out.println("swapped=" + listFromVarargs);

        // 10) Invariance and fixes (won't compile examples shown in comments)
        List<Integer> ints = new ArrayList<>();
        ints.add(1);
        // List<Number> notAllowed = ints; // ERROR: List<Integer> is not a List<Number>
        List<? extends Number> okProducer = ints; // OK: safe producer
        System.out.println("read via extends=" + okProducer.get(0));
        // okProducer.add(2); // ERROR: cannot add to producer

        // 11) Raw type pitfall (unchecked cast) -> runtime ClassCastException
        @SuppressWarnings({"rawtypes","unchecked"})
        Box<String> danger = new Box(); // raw Box assigned to parameterized variable
        ((Box) danger).set(123); // put Integer into raw box (bypasses compile-time checks)
        try {
            String got = danger.get(); // runtime cast fails due to erasure
            System.out.println(got);
        } catch (ClassCastException ex) {
            System.out.println("Caught: " + ex);
        }

        // 12) instanceof and arrays with generics (valid and invalid shown as comments)
        if (words instanceof List<?>) { // OK: wildcard
            System.out.println("words is a List<?>");
        }
        // if (words instanceof List<String>) { } // ERROR: cannot test for parameterized type

        // Pair<String, Integer>[] ps = new Pair<String, Integer>[10]; // ERROR: generic array creation not allowed

        // 13) Restrictions summary (see comments below code)
    }
}

/*
Restrictions and notes (see usage above):
- No primitives as type arguments: Box<int> is invalid; use Box<Integer>.
- Cannot create instances of type parameters: new T(), new T[10] are not allowed.
- Cannot use type parameter in static context (e.g., static T field) inside a generic class.
- Cannot catch or throw generic type parameters (e.g., catch T).
- Type erasure: runtime doesn't know actual type arguments (hence raw-type pitfall, instanceof with List<?> only).
*/

final class Box<T> {
    private T value;

    Box() { }
    Box(T value) { this.value = value; }

    public T get() { return value; }
    public void set(T value) { this.value = value; }

    // Instance generic method independent of class's T
    public <R> Box<R> map(Function<? super T, ? extends R> mapper) {
        return new Box<>(mapper.apply(value));
    }

    @Override public String toString() { return "Box[" + value + "]"; }

    // Illegal in Java (shown for explanation only):
    // static T cache;                    // ERROR: T cannot be used in static context
    // T[] array = new T[10];             // ERROR: generic array creation
    // Box<int> bad = new Box<>();        // ERROR: primitive type not allowed
}

final class Pair<K, V> {
    private final K key;
    private final V value;

    Pair(K key, V value) {
        this.key = key; this.value = value;
    }

    public K getKey() { return key; }
    public V getValue() { return value; }

    public static <K, V> Pair<K, V> of(K key, V value) {
        return new Pair<>(key, value);
    }

    public static <K, V> Pair<V, K> swap(Pair<K, V> p) {
        return new Pair<>(p.value, p.key);
    }

    @Override public String toString() { return "Pair(" + key + ", " + value + ")"; }
}

final class NumberBox<T extends Number> {
    private final List<T> values = new ArrayList<>();

    public void add(T value) { values.add(value); }
    public T get(int i) { return values.get(i); }
    public double sum() {
        double s = 0.0;
        for (T v : values) s += v.doubleValue();
        return s;
    }

    @Override public String toString() { return "NumberBox" + values; }
}

final class Utils {
    private Utils() {}

    public static <T> T first(List<T> list) {
        return list.get(0);
    }

    public static <T> T identity(T value) {
        return value;
    }

    // Producer Extends: safe to read, not to write
    public static double sumOfList(List<? extends Number> list) {
        double sum = 0.0;
        for (Number n : list) sum += n.doubleValue();
        return sum;
    }

    // Consumer Super: safe to write values of T (or its subtypes)
    public static void addInteger(List<? super Integer> list, Integer value) {
        list.add(value);
    }

    public static <S> void copy(List<? extends S> src, List<? super S> dst) {
        dst.addAll(src);
    }

    public static void printAnyList(List<?> list) {
        for (Object o : list) System.out.print(o + " ");
        System.out.println();
    }

    // Wildcard capture for mutation
    public static void reverse(List<?> list) { reverseHelper(list); }
    private static <T> void reverseHelper(List<T> list) {
        Collections.reverse(list);
    }

    public static <T extends Comparable<? super T>> T max(Collection<T> c) {
        Iterator<T> it = c.iterator();
        if (!it.hasNext()) throw new IllegalArgumentException("empty");
        T best = it.next();
        while (it.hasNext()) {
            T next = it.next();
            if (next.compareTo(best) > 0) best = next;
        }
        return best;
    }

    public static <T extends Number & Comparable<? super T>> T clamp(T value, T min, T max) {
        if (value.compareTo(min) < 0) return min;
        if (value.compareTo(max) > 0) return max;
        return value;
    }

    @SafeVarargs
    public static <T> List<T> varargsToList(T... items) {
        return new ArrayList<>(Arrays.asList(items));
    }

    public static <T> void swap(List<T> list, int i, int j) {
        T tmp = list.get(i);
        list.set(i, list.get(j));
        list.set(j, tmp);
    }
}