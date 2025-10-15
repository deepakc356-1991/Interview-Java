package _05_02_wildcards_and_bounds;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/*
Wildcards & Bounds (? extends / ? super) — Theory and Examples

Core ideas
- Generics are invariant: List<Integer> is NOT a subtype of List<Number>.
- Wildcards introduce use-site variance:
  - ? extends T  (upper-bounded wildcard): some unknown subtype of T (covariant reading)
  - ? super T    (lower-bounded wildcard): some unknown supertype of T (contravariant writing)
  - ?            (unbounded wildcard): unknown type; read as Object; write only null

PECS guideline
- Producer Extends: if a parameter produces T values for you to read, use ? extends T.
- Consumer Super:   if a parameter consumes T values for you to write, use ? super T.
- If you need both read and write strongly typed as the same T, prefer a type parameter: <T>.

What you can do
- List<? extends Number>:
  - Can read elements as Number.
  - Cannot add any specific Number (except null), because the exact subtype is unknown.
- List<? super Integer>:
  - Can add Integer (or its subclasses) safely.
  - Can read only as Object (you don’t know which supertype).

Other rules and tips
- Unbounded wildcard (?) is useful when you only need Object-level ops (e.g., size, iteration).
- Wildcards vs type params:
  - Use wildcards in public APIs to make methods more flexible.
  - Use a type parameter <T> when the same type appears in multiple places (inputs/outputs).
- Wildcards cannot declare multiple bounds (no ? extends A & B). Type variables can: <T extends A & B>.
- You cannot instantiate with wildcards: new ArrayList<?>(); // does not compile
- You cannot write class Foo<?> { } // wildcards are for use-sites, not for declaring type parameters
- Arrays are covariant (String[] is an Object[]), generics are invariant. Prefer generics for type safety.
- Return types with wildcards limit callers; return concrete type params when possible.
- Recursive bounds often use super for comparability: <T extends Comparable<? super T>>.

Below: runnable examples that demonstrate the above.
*/
public class _01_Theory {

    // Simple generic container used in examples
    static class Box<T> {
        private T value;
        Box() {}
        Box(T value) { this.value = value; }
        public T get() { return value; }
        public void set(T value) { this.value = value; }
        @Override public String toString() { return "Box[" + value + "]"; }
    }

    public static void main(String[] args) {
        // Invariance: a List<Integer> is NOT a List<Number>
        List<Integer> ints = new ArrayList<>(Arrays.asList(1, 2, 3));
        List<Number> nums  = new ArrayList<>(Arrays.asList(10, 20, 30));
        List<Object> objs  = new ArrayList<>(Arrays.asList("x", 42, 3.14));

        // acceptNumbers(nums);               // OK
        // acceptNumbers(ints);               // Does NOT compile (invariance)

        // Producer: ? extends Number (we can read as Number)
        double sum = sumProducer(ints);       // List<Integer> fits ? extends Number
        System.out.println("sumProducer(ints) = " + sum);

        // Consumer: ? super Integer (we can add Integer)
        writeConsumer(nums);                  // List<Number> fits ? super Integer
        System.out.println("after writeConsumer(nums): " + nums);

        // Unbounded: only read as Object (printing, size)
        printAll(ints);
        printAll(objs);

        // Copy example: Collections.copy-like signature using PECS
        copy(nums, ints); // dst: ? super T (Number), src: ? extends T (Integer)
        System.out.println("after copy(nums, ints): " + nums);

        // max example using recursive bound Comparable<? super T>
        System.out.println("max of ints = " + max(ints));
        List<String> words = new ArrayList<>(Arrays.asList("pear", "apple", "orange"));
        System.out.println("max of words = " + max(words));

        // Wildcard capture: swap elements in a List<?> via helper method
        wildcardSwap(words, 0, 1);
        System.out.println("after wildcardSwap(words,0,1): " + words);

        // ? extends: can add only 'null' (compile-time); reading is safe as Number
        List<? extends Number> producers = ints; // unknown subtype of Number
        // producers.add(123); // does NOT compile
        producers.add(null); // the only value allowed for add
        System.out.println("ints after add null via ? extends ref: " + ints);

        // ? super: can add Integer, but reading yields Object
        List<? super Integer> consumers = nums; // unknown supertype of Integer
        consumers.add(999);
        Object first = consumers.get(0); // must be Object (type unknown)
        System.out.println("consumers first (as Object) = " + first);

        // Wildcards in returns are of limited use
        List<? extends Number> maybeNums = numbersOrIntegers(true);
        // maybeNums.add(1); // does NOT compile; return with wildcard restricts caller

        // Multiple bounds: allowed for type params, not for wildcards
        // List<? extends Number & Comparable<Number>> x; // does NOT compile
        // Example of type param with multiple bounds:
        System.out.println("sumPositives = " + sumPositives(Arrays.asList(1, 2, 3)));

        // Can't instantiate with wildcards:
        // List<?> cannotInstantiate = new ArrayList<?>(); // does NOT compile

        // When to prefer type parameter over wildcard:
        // Example: two lists must share the same T across parameters and return type
        System.out.println("firstCommon = " + firstCommonPrefix(
                Arrays.asList("car", "cart", "carbon"), Arrays.asList("carbide", "carrot")));

        // Arrays are covariant; generics are invariant:
        // Object[] arr = new Integer[10]; // compiles but can throw ArrayStoreException at runtime
        // List<Object> l = new ArrayList<Integer>(); // does NOT compile (safer than arrays)
    }

    // Invariance example: only accepts exactly List<Number>
    static void acceptNumbers(List<Number> numbers) {
        numbers.add(42); // ok
        // List<Integer> cannot be passed here
    }

    // Producer: ? extends Number (covariant read)
    static double sumProducer(List<? extends Number> list) {
        double sum = 0.0;
        for (Number n : list) {
            sum += n.doubleValue();
        }
        // list.add(1);     // does NOT compile
        // list.add(1.0);   // does NOT compile
        // list.add(null);  // compiles (the only allowed value)
        return sum;
    }

    // Consumer: ? super Integer (contravariant write)
    static void writeConsumer(List<? super Integer> list) {
        list.add(1);   // can add Integer
        list.add(2);   // can add Integer
        // Integer has no public subclasses, but generically: list.add(new SubclassOfInteger()) would also be OK
        Object x = list.get(0); // reading is only safe as Object
        // Integer i = list.get(0); // does NOT compile (unknown supertype)
    }

    // PECS in practice: copy from src (producer) to dst (consumer)
    static <T> void copy(List<? super T> dst, List<? extends T> src) {
        for (int i = 0; i < src.size(); i++) {
            if (i < dst.size()) {
                // If dst supports set
                try {
                    dst.set(i, src.get(i));
                } catch (UnsupportedOperationException | IndexOutOfBoundsException e) {
                    // Fallback to add if set unsupported
                    dst.add(src.get(i));
                }
            } else {
                dst.add(src.get(i));
            }
        }
    }

    // Classic example of recursive bound with super:
    // - T must be Comparable to T or any of its supertypes (Comparable<? super T>)
    // - The list may contain T or any subtype of T (? extends T)
    static <T extends Comparable<? super T>> T max(List<? extends T> list) {
        if (list == null || list.isEmpty()) throw new IllegalArgumentException("empty list");
        T best = list.get(0);
        for (int i = 1; i < list.size(); i++) {
            T next = list.get(i);
            if (next.compareTo(best) > 0) best = next;
        }
        return best;
    }

    // Unbounded wildcard: only Object-level operations
    static void printAll(List<?> list) {
        for (Object o : list) {
            System.out.print(o + " ");
        }
        System.out.println();
        // list.add("x"); // does NOT compile (unknown type); only null is allowed
    }

    // Wildcard capture: to write into a List<?> via helper that captures the unknown type
    static void wildcardSwap(List<?> list, int i, int j) {
        swapHelper(list, i, j); // capture occurs here
    }
    private static <T> void swapHelper(List<T> list, int i, int j) {
        T tmp = list.get(i);
        list.set(i, list.get(j));
        list.set(j, tmp);
    }

    // Returning a wildcard-typed list restricts the caller (cannot add)
    static List<? extends Number> numbersOrIntegers(boolean flag) {
        if (flag) return new ArrayList<Integer>(Arrays.asList(5, 6, 7));
        return new ArrayList<Double>(Arrays.asList(1.5, 2.5));
    }

    // Type parameter with multiple bounds example (wildcards cannot do this):
    // T must be a Number and Comparable to itself or a supertype
    static <T extends Number & Comparable<? super T>> T sumPositives(List<T> list) {
        if (list == null || list.isEmpty()) throw new IllegalArgumentException("empty list");
        // For demonstration, pick max positive by compareTo (not real numeric sum)
        T best = null;
        for (T t : list) {
            if (t.doubleValue() > 0) {
                if (best == null || t.compareTo(best) > 0) best = t;
            }
        }
        return best;
    }

    // Prefer type parameter when the same type flows through multiple positions
    static <T> T firstCommonPrefix(List<? extends T> a, List<? extends T> b) {
        // Demo with Strings (T becomes String when called with List<String>)
        // We cannot assume operations on T beyond Object unless we bound T, but here we only return an element.
        return a.isEmpty() ? null : a.get(0);
    }

    // More examples of PECS with JDK APIs (conceptual):
    static void pecsWithCollections() {
        List<Integer> ints = new ArrayList<>(Arrays.asList(3, 1, 2));
        // Collections.max has signature: <T extends Object & Comparable<? super T>> T max(Collection<? extends T> coll)
        Integer m = Collections.max(ints);

        // Collections.copy(List<? super T> dest, List<? extends T> src)
        List<Number> numbers = new ArrayList<>();
        Collections.copy(numbers, ints);

        // List.sort(Comparator<? super T> c)
        List<BigDecimal> prices = new ArrayList<>(Arrays.asList(new BigDecimal("9.99"), new BigDecimal("2.50")));
        Comparator<Number> byDouble = Comparator.comparingDouble(Number::doubleValue);
        // Comparator<? super BigDecimal> accepts Comparator<Number> because Number is a supertype
        prices.sort(byDouble);
    }
}