package _05_01_generic_classes_and_methods;

import java.util.*;

/*
Generic Classes & Methods — Theory and Examples

Why generics
- Provide compile-time type safety without casts.
- Enable reusable, type-parameterized classes, interfaces, methods.
- Avoid ClassCastException at runtime by moving checks to compile-time.

Syntax overview
- Generic class: class Box<T> { T value; }
- Generic interface: interface Pair<K,V> { K key(); V value(); }
- Generic method: static <T> T pick(T a, T b) { ... }
- Type parameter naming conventions: T (type), E (element), K/V (key/value), N (number), R (result), S/U (2nd/3rd types).

Bounds
- Upper bound in type parameters: <T extends Number> limits T to Number or its subclasses.
- Multiple bounds: <T extends Number & Comparable<T>>; class bound must come first, interfaces follow with &.
- Wildcard upper bound: List<? extends Number> (read-only as Number).
- Wildcard lower bound: List<? super Integer> (write Integer/its subtypes, read as Object).

Variance (PECS)
- Producer Extends: use ? extends T when you only read T values (producer of T).
- Consumer Super: use ? super T when you only write T values (consumer of T).
- Invariance: List<Integer> is NOT a subtype of List<Number>.

Type erasure (runtime)
- Generic type info is erased at runtime.
- No new T(), no T.class, no new List<String>[10].
- No instanceof with parameterized types (you can use instanceof List<?>).
- Erasure may generate bridge methods to preserve polymorphism.

Raw types and heap pollution
- Using raw types (List) disables type checks and may cause ClassCastException later.
- Heap pollution arises when a variable of a parameterized type refers to an object that is not of that parameterized type.
- Prefer parameterized types; use @SuppressWarnings only when truly safe.

Generic methods and type inference
- Generic methods infer type arguments from call-site.
- Explicit type arguments allowed: Utils.<Number>flatten(ints, doubles).
- Target typing with the diamond operator: new Box<>() infers type from assignment.

Static vs instance in generic classes
- A class-level type parameter T is not available in static context.
- Use method-level type parameters for static methods: static <U> U id(U u).

Arrays vs generics
- Arrays are covariant and reified; generics are invariant and erased.
- Cannot create arrays of parameterized types: new List<String>[10] is illegal.

Wildcard capture
- Helper methods with their own <T> can "capture" ? to safely manipulate elements, e.g., swapping.

Restrictions
- Cannot instantiate type parameter: new T(), T[].
- Cannot use primitive type arguments: List<int> is illegal; use List<Integer>.
- Cannot create generic exception classes; cannot catch a type variable exception.
- Overloading by only generic type parameters leads to erasure conflicts.

Below are minimal, focused examples with inline comments to illustrate each concept.
*/
public class _01_Theory {

    public static void main(String[] args) {
        // Generic class
        Box<String> bs = new Box<>("hello");
        String s = bs.get();

        // Diamond operator (type inference)
        Box<Integer> bi = new Box<>(123);

        // Generic interface and implementation
        Pair<String, Integer> p = new SimplePair<>("age", 42);

        // Bounded type parameter example
        NumericBox<Integer> nb = new NumericBox<>();
        nb.add(10);
        nb.add(20);
        int max = nb.max();          // works due to Comparable bound
        double sum = nb.sum();       // works due to Number bound

        // Generic methods
        Integer first = Generics.pickFirst(1, 2);
        List<String> names = Generics.listOf("A", "B", "C");
        Generics.printList(names);

        // Wildcards and PECS
        List<Integer> ints = Generics.listOf(1, 2, 3);
        List<Number> nums = new ArrayList<>();
        Generics.copy(nums, ints);              // dest <? super T>, src <? extends T>
        Generics.addOneToEach(nums, 2);         // writes Integer into List<? super Integer>
        double total = Generics.sumOf(nums);    // reads Number via <? extends Number>

        // Recursive bound
        Integer maxInt = Generics.max(ints);

        // Wildcard capture helper
        List<String> words = new ArrayList<>(Arrays.asList("x", "y", "z"));
        Generics.swapFirstTwo(words);

        // Safe varargs with generics
        List<Integer> moreInts = Generics.listOf(7, 8, 9);
        List<Double> doubles = Generics.listOf(1.5, 2.5);
        List<Number> flat = Generics.<Number>flatten(ints, moreInts, doubles);

        // Using a Class<T> token to create instances
        StringBuilder sb = Generics.newInstance(StringBuilder.class);

        // Arrays vs generics toArray pattern
        String[] arr = ArraysVsGenerics.toArray(names, new String[0]);

        // Bridge method example
        UserRepo repo = new UserRepo();
        repo.save(new User());

        // Raw types and heap pollution (demonstration; caught to avoid crash)
        HeapPollution.demo();

        // Generic constructor and method
        GenericConstructor gc = new GenericConstructor(42);
        gc.addTwice("hi");
    }

    // Generic class with instance-level type parameter T
    static class Box<T> {
        private T value;

        public Box() {}
        public Box(T value) { this.value = value; }
        public T get() { return value; }
        public void set(T value) { this.value = value; }

        // Static methods cannot use the class's T (static context).
        // Use a method-level type parameter instead:
        public static <U> Box<U> of(U value) { return new Box<>(value); }

        // You cannot check the actual T at runtime due to erasure:
        // boolean isString() { return value instanceof String; } // Allowed (uses String literal)
        // But you cannot do: if (value instanceof T) ... // illegal; T not available at runtime
    }

    // Generic interface and implementation
    interface Pair<K, V> {
        K key();
        V value();
        default String describe() { return String.valueOf(key()) + "=" + String.valueOf(value()); }
    }
    static class SimplePair<K, V> implements Pair<K, V> {
        private final K k;
        private final V v;
        SimplePair(K k, V v) { this.k = k; this.v = v; }
        public K key() { return k; }
        public V value() { return v; }
    }

    // Multiple bounds: T must be a Number and Comparable to itself
    static class NumericBox<T extends Number & Comparable<T>> {
        private final List<T> data = new ArrayList<>();
        public void add(T t) { data.add(t); }
        public T max() {
            if (data.isEmpty()) throw new IllegalStateException("empty");
            T m = data.get(0);
            for (T t : data) if (t.compareTo(m) > 0) m = t;
            return m;
        }
        public double sum() {
            double s = 0;
            for (T t : data) s += t.doubleValue();
            return s;
        }
        public List<T> asList() { return data; }
    }

    static class Generics {

        // Generic method: type inferred from arguments
        static <T> T pickFirst(T a, T b) { return a; }

        // Unbounded wildcard: read-only as Object, cannot add (except null)
        static void printList(List<?> list) {
            for (Object o : list) System.out.print(o + " ");
            System.out.println();
        }

        // Upper-bounded wildcard: Producer (reads Number)
        static double sumOf(List<? extends Number> numbers) {
            double s = 0.0;
            for (Number n : numbers) s += n.doubleValue();
            return s;
        }

        // Lower-bounded wildcard: Consumer (writes Integer)
        static void addOneToEach(List<? super Integer> list, int count) {
            for (int i = 0; i < count; i++) list.add(i);
        }

        // PECS example: copy source to destination
        static <T> void copy(List<? super T> dst, List<? extends T> src) {
            for (T t : src) dst.add(t);
        }

        // Recursive bound: common for Comparable types
        static <T extends Comparable<? super T>> T max(Collection<T> coll) {
            Iterator<T> it = coll.iterator();
            if (!it.hasNext()) throw new IllegalArgumentException("empty");
            T m = it.next();
            while (it.hasNext()) {
                T v = it.next();
                if (v.compareTo(m) > 0) m = v;
            }
            return m;
        }

        // Wildcard capture helper method
        static void swapFirstTwo(List<?> list) {
            swapHelper(list);
        }
        private static <T> void swapHelper(List<T> list) {
            if (list.size() >= 2) {
                T tmp = list.get(0);
                list.set(0, list.get(1));
                list.set(1, tmp);
            }
        }

        // Safe varargs with generics (only use when truly safe)
        @SafeVarargs
        static <T> List<T> flatten(List<? extends T>... lists) {
            List<T> result = new ArrayList<>();
            for (List<? extends T> l : lists) result.addAll(l);
            return result;
        }

        // Using a Class<T> token to create a T (workaround for new T())
        static <T> T newInstance(Class<T> type) {
            try {
                return type.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        // Convenience: build a typed list (inference from args)
        @SafeVarargs
        static <T> List<T> listOf(T... items) {
            List<T> list = new ArrayList<>(items.length);
            Collections.addAll(list, items);
            return list;
        }

        // Lower-bounded comparator acceptance: typical pattern
        static <T> void sortWithComparator(List<T> list, Comparator<? super T> cmp) {
            list.sort(cmp);
        }
    }

    // Bridge methods
    static class Repo<T> {
        T save(T e) { return e; }
    }
    static class User {}
    static class UserRepo extends Repo<User> {
        // After erasure, Repo.save is Object save(Object).
        // Compiler emits a bridge method so overriding works at runtime.
        @Override User save(User e) { return e; }
    }

    static class ArraysVsGenerics {
        // Cannot do: new T[...]; prefer toArray with an existing array
        static <T> T[] toArray(List<T> list, T[] arr) {
            if (arr.length < list.size()) return list.toArray(Arrays.copyOf(arr, list.size()));
            int i = 0;
            for (T t : list) arr[i++] = t;
            if (arr.length > list.size()) arr[list.size()] = null;
            return arr;
        }
    }

    static class Factory<T> {
        // Cannot do: new T(), T.class
        private final Class<T> type;
        Factory(Class<T> type) { this.type = type; }
        T create() {
            try {
                return type.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        boolean isList(Object o) { return o instanceof List<?>; } // OK
        // boolean isStringList(Object o) { return o instanceof List<String>; } // illegal
    }

    static class GenericConstructor {
        private final List<String> log = new ArrayList<>();

        // Generic constructor (independent of any class type params)
        <T> GenericConstructor(T value) { log.add(String.valueOf(value)); }

        // Generic method with its own T
        <T> void addTwice(T value) {
            log.add(String.valueOf(value));
            log.add(String.valueOf(value));
        }

        List<String> log() { return log; }
    }

    static class BoxUtils {
        // Lower-bounded wildcard: can write Integer
        static void putInteger(Box<? super Integer> b, Integer x) { b.set(x); }
        // Upper-bounded wildcard: can read Integer
        static Integer getInteger(Box<? extends Integer> b) { return b.get(); }

        // Invariance examples:
        // void f(List<Number> xs) { }
        // f(new ArrayList<Integer>()); // does not compile (invariance)
        // Use <? extends Number> to read Numbers, or <? super Integer> to write Integers.
    }

    static class HeapPollution {
        @SuppressWarnings({"rawtypes", "unchecked"})
        static void demo() {
            List<String> strings = new ArrayList<>();
            List raw = strings;     // raw assignment (unsafe)
            raw.add(42);            // heap pollution
            try {
                String s = strings.get(0); // ClassCastException at runtime
            } catch (ClassCastException e) {
                // swallowed for demo
            }
        }
    }

    // Additional notes (invalid code shown in comments):
    // - Name clashes after erasure:
    //   void m(List<String> a) {}
    //   void m(List<Integer> b) {} // compile-time error: same erasure m(List)
    //
    // - Generic exceptions:
    //   class Problem<T> extends Exception {}         // illegal
    //   <T extends Throwable> void x() throws T {}    // allowed to declare, but you cannot catch T
    //
    // - Adding to extends-bounded wildcard:
    //   List<? extends Number> xs = new ArrayList<Integer>();
    //   xs.add(1); // compile-time error (read-only as Number)
    //
    // - Reading from super-bounded wildcard:
    //   List<? super Integer> ys = new ArrayList<Number>();
    //   Integer i = ys.get(0); // compile-time error; type is Object on read
}