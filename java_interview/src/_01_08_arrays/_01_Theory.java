package _01_08_arrays;

import java.util.*;
import java.util.function.IntUnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/*
Topic: Arrays in Java — Theory and Practical Demos

Key concepts:
- What an array is:
  - A fixed-length, ordered, index-based container holding elements of a single type.
  - Arrays are objects; created with new. Indexed from 0 to length-1 (int indices only).
  - An array’s length is immutable once created (via the final instance field length).
  - Arrays can store primitives or references (objects).
- Differences vs collections:
  - Fixed size, can store primitives, no generics, simpler memory layout and faster indexed access.
- Declaration and initialization:
  - Type[] name; name = new Type[size]; or Type[] name = {a, b, c};
  - Anonymous array: new int[]{1,2,3}.
- Default values:
  - Numeric: 0, boolean: false, char: '\u0000', reference: null.
- Bounds and errors:
  - ArrayIndexOutOfBoundsException for invalid indices.
  - NegativeArraySizeException for negative size.
  - OutOfMemoryError if allocation too large.
- Time complexity:
  - Index access O(1); search O(n) unless sorted + binary search O(log n).
  - Inserting/removing at arbitrary positions requires shifting O(n).
- Mutability and pass-by-value:
  - Array reference is passed by value; modifying elements affects caller; reassigning the parameter does not.
  - final int[] arr means the reference is final, but elements are still mutable.
- Equality and string forms:
  - == compares references.
  - Arrays.equals compares element-wise for one-dimensional arrays.
  - Arrays.deepEquals for nested arrays.
  - Arrays.toString vs Arrays.deepToString.
- Copying:
  - Shallow: clone(), Arrays.copyOf, Arrays.copyOfRange, System.arraycopy.
  - Deep copy needed for nested arrays (manual per dimension).
- Sorting and searching:
  - Arrays.sort for primitives (dual-pivot quicksort, not stable).
  - Arrays.sort for objects (TimSort, stable).
  - Arrays.parallelSort (parallel version for large arrays).
  - Arrays.binarySearch on sorted arrays; returns index or negative insertion point (-insertionPoint - 1).
- Multidimensional arrays:
  - Arrays of arrays (jagged possible), not necessarily rectangular/contiguous rows.
- Utility methods:
  - Arrays.fill, Arrays.setAll, Arrays.mismatch, Arrays.parallelPrefix.
- Arrays.asList pitfalls:
  - Fixed-size list backed by array; changes reflect each other; add/remove unsupported.
  - For primitives, Arrays.asList(int[]) yields List<int[]> of size 1 (use streams instead).
- Varargs:
  - T... is an array under the hood; call with array or separate args.
- Type system and safety:
  - Arrays are covariant (String[] is an Object[]), can throw ArrayStoreException at runtime.
  - Generics are invariant; List<String> is not a List<Object>.
- Streams:
  - Arrays.stream for primitives/objects; IntStream, LongStream, DoubleStream for primitives.
- Thread-safety:
  - Arrays not thread-safe; volatile applies to reference, not elements.
- Limits:
  - Max length uses int indexes; practical max ≤ Integer.MAX_VALUE (header overhead applies).
*/

public class _01_Theory {

    public static void main(String[] args) {
        System.out.println("=== Arrays Theory and Demos ===");

        declarationAndInitialization();
        defaultsAndLength();
        boundsChecks();
        negativeSizeAndOOM();
        iterationPatterns();
        mutabilityAndPassByValue();
        multidimensionalAndJagged();
        arraysUtilityBasics();
        sortingAndBinarySearch();
        parallelSortDemo();
        asListPitfallsAndFixes();
        varargsDemo();
        covarianceAndArrayStoreException();
        streamsBasics();
        fillSetAllParallelPrefix();
        cloneShallowVsDeepCopy();
        defensiveCopyPattern();
        zeroLengthVsNull();
    }

    // 1) Declaration and Initialization
    static void declarationAndInitialization() {
        System.out.println("\n-- Declaration and Initialization --");
        int[] a1;                    // declaration
        a1 = new int[3];             // creation: [0,0,0]
        int[] a2 = {1, 2, 3};        // array literal
        String[] s1 = new String[] {"a", "b"}; // explicit new with initializer
        double a3[] = new double[2]; // alternative placement of []

        System.out.println("a1 (new int[3]): " + Arrays.toString(a1));
        System.out.println("a2 ({1,2,3}): " + Arrays.toString(a2));
        System.out.println("s1: " + Arrays.toString(s1));
        System.out.println("a3 (double[2]): " + Arrays.toString(a3));
        System.out.println("Anonymous array: " + Arrays.toString(new int[]{4, 5, 6}));
        // Note: length is fixed; cannot resize an array. Use a List for dynamic sizing.
    }

    // 2) Default values and length
    static void defaultsAndLength() {
        System.out.println("\n-- Default Values and Length --");
        boolean[] b = new boolean[3];     // [false, false, false]
        int[] ints = new int[3];          // [0, 0, 0]
        double[] ds = new double[3];      // [0.0, 0.0, 0.0]
        String[] objs = new String[3];    // [null, null, null]
        char[] cs = new char[3];          // ['\u0000', '\u0000', '\u0000']

        System.out.println("boolean: " + Arrays.toString(b));
        System.out.println("int:     " + Arrays.toString(ints));
        System.out.println("double:  " + Arrays.toString(ds));
        System.out.println("String:  " + Arrays.toString(objs));
        System.out.println("char (as ints to show zeros): " + Arrays.toString(toCodePoints(cs)));

        System.out.println("ints.length = " + ints.length);
        // ints.length is final; cannot assign to it.
    }

    // 3) Bounds checks
    static void boundsChecks() {
        System.out.println("\n-- Bounds Checks --");
        int[] a = {10, 20, 30};
        try {
            int x = a[3]; // invalid index
            System.out.println(x);
        } catch (ArrayIndexOutOfBoundsException ex) {
            System.out.println("Caught: " + ex);
        }
        try {
            int y = a[-1]; // invalid index
            System.out.println(y);
        } catch (ArrayIndexOutOfBoundsException ex) {
            System.out.println("Caught: " + ex);
        }
    }

    // 4) Negative size and OOM
    static void negativeSizeAndOOM() {
        System.out.println("\n-- Negative Size and OOM --");
        try {
            int[] a = new int[-5];
            System.out.println(Arrays.toString(a));
        } catch (NegativeArraySizeException ex) {
            System.out.println("Caught: " + ex);
        }
        // Large allocations can throw OutOfMemoryError at runtime (not demonstrated).
    }

    // 5) Iteration patterns
    static void iterationPatterns() {
        System.out.println("\n-- Iteration Patterns --");
        int[] a = {1, 2, 3, 4};

        // Index-based
        int sum1 = 0;
        for (int i = 0; i < a.length; i++) sum1 += a[i];

        // Enhanced for-each (cannot change structure; element variable is a copy for primitives)
        int sum2 = 0;
        for (int v : a) sum2 += v;

        // Modifying elements requires index-based loop
        for (int i = 0; i < a.length; i++) a[i] *= 2;

        System.out.println("Sum (index-based): " + sum1);
        System.out.println("Sum (for-each):   " + sum2);
        System.out.println("After multiply by 2: " + Arrays.toString(a));
    }

    // 6) Mutability and pass-by-value
    static void mutabilityAndPassByValue() {
        System.out.println("\n-- Mutability and Pass-by-Value --");
        int[] a = {1, 2, 3};
        modifyInPlace(a);
        System.out.println("After modifyInPlace: " + Arrays.toString(a)); // element changed
        reassignInCallee(a);
        System.out.println("After reassignInCallee: " + Arrays.toString(a)); // unchanged reference
        // final int[] f = a; f[0] = 99; // OK; f = new int[1]; // not allowed (reference is final)
    }

    static void modifyInPlace(int[] x) { x[0] = 99; }
    static void reassignInCallee(int[] x) { x = new int[]{7, 8, 9}; }

    // 7) Multidimensional (jagged) arrays
    static void multidimensionalAndJagged() {
        System.out.println("\n-- Multidimensional and Jagged Arrays --");
        int[][] rect = new int[2][3]; // 2 rows, 3 cols
        rect[0][1] = 7;
        System.out.println("rect: " + Arrays.deepToString(rect));
        System.out.println("rect.length (rows): " + rect.length + ", rect[0].length (cols): " + rect[0].length);

        int[][] jag = new int[3][]; // jagged
        jag[0] = new int[1];
        jag[1] = new int[3];
        jag[2] = new int[0];
        jag[1][2] = 42;
        System.out.println("jag:  " + Arrays.deepToString(jag));
        // Note: Each row is an independent array instance; lengths may differ.
    }

    // 8) Arrays utility basics (equals, deepEquals, toString, deepToString, copyOf, arraycopy, mismatch)
    static void arraysUtilityBasics() {
        System.out.println("\n-- Arrays Utility Methods --");

        int[][] a = {{1, 2}, {3}};
        int[][] b = {{1, 2}, {3}};
        System.out.println("a == b: " + (a == b));
        System.out.println("Arrays.equals(a,b): " + Arrays.equals(a, b)); // compares top-level refs
        System.out.println("Arrays.deepEquals(a,b): " + Arrays.deepEquals(a, b)); // deep content

        System.out.println("Arrays.toString(a):     " + Arrays.toString(a));      // prints ref IDs of sub-arrays
        System.out.println("Arrays.deepToString(a): " + Arrays.deepToString(a));  // prints contents recursively

        int[] src = {1, 2, 3, 4, 5};
        int[] c1 = Arrays.copyOf(src, 3);       // [1,2,3]
        int[] c2 = Arrays.copyOf(src, 8);       // [1,2,3,4,5,0,0,0]
        int[] c3 = Arrays.copyOfRange(src, 1, 4); // [2,3,4]
        System.out.println("copyOf(3): " + Arrays.toString(c1));
        System.out.println("copyOf(8): " + Arrays.toString(c2));
        System.out.println("copyOfRange(1,4): " + Arrays.toString(c3));

        // System.arraycopy: supports overlapping ranges; runtime type checks apply
        int[] overlap = {0, 1, 2, 3, 4, 5};
        System.arraycopy(overlap, 1, overlap, 2, 3); // shift right
        System.out.println("arraycopy overlap: " + Arrays.toString(overlap));

        try {
            Object[] dest = new Number[3];
            String[] srcStr = {"x", "y"};
            System.arraycopy(srcStr, 0, dest, 0, srcStr.length); // runtime ArrayStoreException
            System.out.println(Arrays.toString(dest));
        } catch (ArrayStoreException ex) {
            System.out.println("Caught: " + ex);
        }

        // mismatch: first index where arrays differ, or -1 if equal
        int mi = Arrays.mismatch(new int[]{1, 2, 9}, new int[]{1, 2, 3});
        System.out.println("Arrays.mismatch: " + mi);
    }

    // 9) Sorting and binary search (stability, comparator, insertion point)
    static void sortingAndBinarySearch() {
        System.out.println("\n-- Sorting and Binary Search --");
        int[] p = {5, 1, 3, 7, 2};
        Arrays.sort(p); // primitives: dual-pivot quicksort (unstable)
        System.out.println("Sorted primitives: " + Arrays.toString(p));

        Pair[] pairs = {
                new Pair("A", 1),
                new Pair("A", 2),
                new Pair("B", 3),
                new Pair("A", 3)
        };
        Arrays.sort(pairs, Comparator.comparing(pr -> pr.key)); // Objects: TimSort (stable)
        System.out.println("Sorted by key (stable): " + Arrays.toString(pairs));
        // Observe A(1), A(2), A(3) keep original relative order among equal keys.

        int[] sorted = {1, 3, 5, 7, 9};
        int i1 = Arrays.binarySearch(sorted, 5); // found index
        int i2 = Arrays.binarySearch(sorted, 4); // negative insertion point
        int insertionPoint = -i2 - 1;
        System.out.println("binarySearch 5: idx=" + i1);
        System.out.println("binarySearch 4: idx=" + i2 + ", insertionPoint=" + insertionPoint);
        // Precondition: array must be sorted according to the same comparator/order used by binarySearch.
    }

    // 10) Parallel sort
    static void parallelSortDemo() {
        System.out.println("\n-- Parallel Sort --");
        int[] a = IntStream.rangeClosed(1, 20).map(i -> 21 - i).toArray();
        Arrays.parallelSort(a); // may use ForkJoin pool; generally beneficial for large arrays
        System.out.println("parallelSort: " + Arrays.toString(a));
    }

    // 11) Arrays.asList pitfalls and fixes
    static void asListPitfallsAndFixes() {
        System.out.println("\n-- Arrays.asList Pitfalls and Fixes --");
        String[] arr = {"a", "b"};
        List<String> fixed = Arrays.asList(arr); // fixed-size, backed by array
        fixed.set(0, "x"); // allowed; writes through to array
        System.out.println("arr after fixed.set: " + Arrays.toString(arr));
        try {
            fixed.add("c"); // unsupported
        } catch (UnsupportedOperationException ex) {
            System.out.println("fixed.add -> Caught: " + ex);
        }
        List<String> modifiable = new ArrayList<>(Arrays.asList(arr));
        modifiable.add("c");
        System.out.println("modifiable: " + modifiable);

        int[] prim = {1, 2, 3};
        List<int[]> wrong = Arrays.asList(prim); // size 1, element is int[]
        System.out.println("Arrays.asList on primitives: size=" + wrong.size() + ", element type=" + wrong.get(0).getClass().getSimpleName());
        List<Integer> correct = Arrays.stream(prim).boxed().collect(Collectors.toList());
        System.out.println("Proper primitive to List<Integer>: " + correct);
    }

    // 12) Varargs
    static void varargsDemo() {
        System.out.println("\n-- Varargs --");
        int s1 = sum(1, 2, 3);
        int s2 = sum(new int[]{4, 5, 6}); // varargs accepts array directly
        System.out.println("sum(1,2,3): " + s1 + ", sum(array): " + s2);
        // Note: Varargs is just an array; can allocate per call; watch for performance-sensitive hot paths.
    }

    static int sum(int... values) {
        int s = 0;
        for (int v : values) s += v;
        return s;
    }

    // 13) Covariance and ArrayStoreException
    static void covarianceAndArrayStoreException() {
        System.out.println("\n-- Covariance and ArrayStoreException --");
        String[] strings = new String[2];
        Object[] objs = strings; // arrays are covariant
        try {
            objs[0] = 42; // runtime check -> ArrayStoreException
        } catch (ArrayStoreException ex) {
            System.out.println("Caught: " + ex);
        }
        // Generics are invariant (List<String> is not a List<Object>); disallows this issue at compile time.
    }

    // 14) Streams with arrays
    static void streamsBasics() {
        System.out.println("\n-- Streams with Arrays --");
        int[] a = {1, 2, 3, 4, 5};
        int sum = Arrays.stream(a).filter(x -> x % 2 == 1).sum();
        System.out.println("Sum of odds: " + sum);

        String[] s = {"a", "bb", "ccc"};
        List<Integer> lengths = Arrays.stream(s).map(String::length).collect(Collectors.toList());
        System.out.println("String lengths: " + lengths);

        // Conversions
        int[] doubled = Arrays.stream(a).map(x -> x * 2).toArray();
        System.out.println("Doubled: " + Arrays.toString(doubled));
    }

    // 15) fill, setAll, parallelPrefix
    static void fillSetAllParallelPrefix() {
        System.out.println("\n-- fill, setAll, parallelPrefix --");
        int[] a = new int[5];
        Arrays.fill(a, 42);
        System.out.println("fill(42): " + Arrays.toString(a));

        Arrays.setAll(a, i -> i * i); // i^2
        System.out.println("setAll(i->i*i): " + Arrays.toString(a));

        Arrays.parallelPrefix(a, Integer::sum); // prefix sums in place
        System.out.println("parallelPrefix (prefix sums): " + Arrays.toString(a));
    }

    // 16) clone, shallow vs deep copy
    static void cloneShallowVsDeepCopy() {
        System.out.println("\n-- clone: Shallow vs Deep Copy --");
        int[][] m = {{1, 2}, {3, 4}};
        int[][] shallow = m.clone(); // shallow copy of top-level only
        shallow[0][0] = 99; // affects m[0][0] too
        System.out.println("After shallow change, m: " + Arrays.deepToString(m));

        int[][] deep = deepCopy2D(m);
        deep[1][1] = -7; // does not affect m
        System.out.println("After deep change, m: " + Arrays.deepToString(m));
        System.out.println("Deep copy: " + Arrays.deepToString(deep));
    }

    static int[][] deepCopy2D(int[][] src) {
        if (src == null) return null;
        int[][] copy = new int[src.length][];
        for (int i = 0; i < src.length; i++) {
            copy[i] = src[i] == null ? null : Arrays.copyOf(src[i], src[i].length);
        }
        return copy;
    }

    // 17) Defensive copies pattern
    static void defensiveCopyPattern() {
        System.out.println("\n-- Defensive Copies --");
        Scoreboard sb = new Scoreboard(new int[]{10, 20, 30});
        int[] publicView = sb.getScores(); // returns a copy
        publicView[0] = 999; // does not affect internal state
        System.out.println("Original after external mutation: " + Arrays.toString(sb.getScores()));

        sb.setScores(new int[]{1, 2, 3}); // setter copies input
        System.out.println("After setScores: " + Arrays.toString(sb.getScores()));
    }

    // 18) Zero-length arrays vs null
    static void zeroLengthVsNull() {
        System.out.println("\n-- Zero-length Arrays vs null --");
        int[] empty = new int[0];
        System.out.println("sum(empty) = " + sumNullable(empty));
        System.out.println("sum(null)  = " + sumNullable(null));
        // Prefer zero-length arrays over null to avoid NPE checks and simplify client code.
    }

    static int sumNullable(int[] a) {
        return a == null ? 0 : Arrays.stream(a).sum();
    }

    // Helpers
    static int[] toCodePoints(char[] chars) {
        int[] cps = new int[chars.length];
        for (int i = 0; i < chars.length; i++) cps[i] = chars[i];
        return cps;
    }

    static class Pair {
        final String key;
        final int order;

        Pair(String key, int order) {
            this.key = key;
            this.order = order;
        }

        @Override public String toString() { return key + "(" + order + ")"; }
    }

    static class Scoreboard {
        private int[] scores;

        Scoreboard(int[] scores) {
            this.scores = scores == null ? new int[0] : Arrays.copyOf(scores, scores.length);
        }

        public int[] getScores() {
            return Arrays.copyOf(scores, scores.length); // defensive copy
        }

        public void setScores(int[] scores) {
            this.scores = scores == null ? new int[0] : Arrays.copyOf(scores, scores.length);
        }
    }
}