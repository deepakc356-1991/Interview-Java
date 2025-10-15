package _01_08_arrays;

import java.util.*;
import java.util.stream.*;

public class _02_Examples {

    public static void main(String[] args) {
        header("Basics: declaration, initialization, access");
        basics();

        header("Iteration: for, enhanced-for, modifying elements");
        iteration();

        header("Copying arrays: clone, Arrays.copyOf, System.arraycopy, deep copy");
        copying();

        header("Sorting and searching: sort, comparators, binarySearch");
        sortingSearching();

        header("Multi-dimensional and ragged arrays");
        multiDimensional();

        header("Passing to methods, returning arrays, varargs");
        passingReturning();

        header("Arrays <-> Lists (Arrays.asList), conversions");
        arraysAndLists();

        header("Utilities and Streams: fill, setAll, equals, deepEquals, toString, streams");
        streamsAndUtilities();

        header("Common patterns: frequency, rotate, merge sorted");
        commonPatterns();

        header("Pitfalls and gotchas");
        pitfalls();
    }

    // -----------------------------------------------------------------------------
    // BASICS
    // -----------------------------------------------------------------------------
    static void basics() {
        // Declaration + creation with default values (primitives -> 0, booleans -> false, refs -> null)
        int[] a = new int[5];
        System.out.println("int[] default: " + Arrays.toString(a)); // [0, 0, 0, 0, 0]

        String[] s = new String[3];
        System.out.println("String[] default: " + Arrays.toString(s)); // [null, null, null]

        // Initializers
        int[] b = { 1, 2, 3 };
        int[] c = new int[] { 4, 5, 6 };
        System.out.println("b: " + Arrays.toString(b));
        System.out.println("c: " + Arrays.toString(c));

        // Access + assignment
        c[0] = 9;
        System.out.println("c after c[0]=9: " + Arrays.toString(c));
        System.out.println("b length: " + b.length);

        // Last element via length - 1
        System.out.println("b last element: " + b[b.length - 1]);

        // Out-of-bounds example
        try {
            int x = b[10];
            System.out.println(x); // never runs
        } catch (ArrayIndexOutOfBoundsException ex) {
            System.out.println("Caught ArrayIndexOutOfBoundsException: " + ex.getMessage());
        }

        // Arrays are objects
        System.out.println("a.getClass().isArray(): " + a.getClass().isArray());
        System.out.println("Array identity hashCode: " + System.identityHashCode(a));

        // 'length' is final; cannot assign: a.length = 10; // compile error if uncommented
    }

    // -----------------------------------------------------------------------------
    // ITERATION
    // -----------------------------------------------------------------------------
    static void iteration() {
        int[] arr = { 1, 2, 3, 4 };

        // Classic for loop (needed to modify elements)
        for (int i = 0; i < arr.length; i++) {
            arr[i] *= 2;
        }
        System.out.println("Modified via index: " + Arrays.toString(arr)); // [2,4,6,8]

        // Enhanced for (read-only variable 'x' copy for primitives; does not write back)
        for (int x : arr) {
            x++; // no effect on arr
        }
        System.out.println("After for-each x++ (no change): " + Arrays.toString(arr));

        // Use indices if you need to modify elements
        for (int i = 0; i < arr.length; i++) {
            arr[i] -= 1;
        }
        System.out.println("After index modification: " + Arrays.toString(arr)); // [1,3,5,7]
    }

    // -----------------------------------------------------------------------------
    // COPYING
    // -----------------------------------------------------------------------------
    static void copying() {
        int[] src = { 1, 2, 3, 4, 5 };

        // clone() -> shallow copy (OK for 1D primitives)
        int[] clone = src.clone();
        clone[0] = 99;
        System.out.println("src:   " + Arrays.toString(src));
        System.out.println("clone: " + Arrays.toString(clone));

        // Arrays.copyOf
        int[] first3 = Arrays.copyOf(src, 3); // [1,2,3]
        int[] extended = Arrays.copyOf(src, 8); // [1,2,3,4,5,0,0,0]
        System.out.println("copyOf first3: " + Arrays.toString(first3));
        System.out.println("copyOf extended: " + Arrays.toString(extended));

        // Arrays.copyOfRange (end exclusive)
        int[] mid = Arrays.copyOfRange(src, 1, 4); // [2,3,4]
        System.out.println("copyOfRange(1,4): " + Arrays.toString(mid));

        // System.arraycopy: fast bulk copy between arrays
        int[] dest = new int[7];
        System.arraycopy(src, 1, dest, 2, 3); // copy 2,3,4 into dest starting at idx 2
        System.out.println("System.arraycopy dest: " + Arrays.toString(dest));

        // Shallow vs deep copy for multi-dimensional
        int[][] m = { { 1, 2 }, { 3, 4 } };
        int[][] shallow = m.clone();       // copies top-level only
        shallow[0][0] = 99;                // changes original inner array
        System.out.println("m after shallow[0][0]=99: " + Arrays.deepToString(m));

        int[][] deep = deepCopy2D(m);      // deep copy
        deep[0][0] = -1;
        System.out.println("m after deep modified: " + Arrays.deepToString(m));
        System.out.println("deep: " + Arrays.deepToString(deep));
    }

    // -----------------------------------------------------------------------------
    // SORTING AND SEARCHING
    // -----------------------------------------------------------------------------
    static void sortingSearching() {
        int[] nums = { 5, 3, 8, 1, 4 };
        Arrays.sort(nums);
        System.out.println("Sorted int[]: " + Arrays.toString(nums));

        // Descending sort for objects using Comparator
        Integer[] boxed = { 5, 3, 8, 1, 4 };
        Arrays.sort(boxed, Comparator.reverseOrder());
        System.out.println("Sorted Integer[] desc: " + Arrays.toString(boxed));

        // Sorting strings: default (lexicographic, case-sensitive) vs case-insensitive
        String[] names = { "Bob", "alice", "Álvaro", "charlie" };
        Arrays.sort(names);
        System.out.println("Strings natural: " + Arrays.toString(names));
        Arrays.sort(names, String.CASE_INSENSITIVE_ORDER);
        System.out.println("Strings case-insensitive: " + Arrays.toString(names));

        // binarySearch (on a sorted array)
        int[] sorted = { 1, 3, 4, 6, 9, 13 };
        int found = Arrays.binarySearch(sorted, 6); // >=0 if found
        int notFound = Arrays.binarySearch(sorted, 2); // negative insertion point - 1
        System.out.println("binarySearch 6 idx: " + found);
        System.out.println("binarySearch 2 idx: " + notFound + " (insertionPoint=" + (-notFound - 1) + ")");

        // parallelSort (may use multiple threads)
        int[] small = {9,7,5,3,1,2,4,6,8};
        Arrays.parallelSort(small);
        System.out.println("parallelSort: " + Arrays.toString(small));
    }

    // -----------------------------------------------------------------------------
    // MULTI-DIMENSIONAL
    // -----------------------------------------------------------------------------
    static void multiDimensional() {
        // Rectangular 2D
        int[][] grid = new int[2][3];
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[i].length; j++) {
                grid[i][j] = (i + 1) * (j + 1);
            }
        }
        System.out.println("grid: " + Arrays.deepToString(grid));

        // Ragged (jagged) array
        int[][] triangle = new int[4][];
        for (int i = 0; i < triangle.length; i++) {
            triangle[i] = new int[i + 1];
            for (int j = 0; j < triangle[i].length; j++) {
                triangle[i][j] = j + 1;
            }
        }
        System.out.println("triangle: " + Arrays.deepToString(triangle));
    }

    // -----------------------------------------------------------------------------
    // PASSING, RETURNING, VARARGS
    // -----------------------------------------------------------------------------
    static void passingReturning() {
        int[] data = { 2, 4, 6, 8, 10 };
        System.out.println("sum(data): " + sum(data));

        reverseInPlace(data);
        System.out.println("after reverse: " + Arrays.toString(data));

        int[] appended = append(data, 99);
        System.out.println("after append 99: " + Arrays.toString(appended));

        // varargs (int... is an int[] inside the method)
        System.out.println("average(1,2,3,4): " + average(1, 2, 3, 4));
        System.out.println("average(data): " + average(data)); // passing array to varargs

        // Mutating a passed array via varargs
        mutateFirstElement(data);
        System.out.println("after mutateFirstElement(varargs): " + Arrays.toString(data));
    }

    // -----------------------------------------------------------------------------
    // ARRAYS AND LISTS
    // -----------------------------------------------------------------------------
    static void arraysAndLists() {
        String[] words = { "a", "b", "c" };

        // Arrays.asList: fixed-size list backed by the array
        List<String> fixed = Arrays.asList(words);
        System.out.println("fixed list: " + fixed + ", size=" + fixed.size());

        try {
            fixed.add("x"); // UnsupportedOperationException
        } catch (UnsupportedOperationException ex) {
            System.out.println("Cannot add to fixed-size list from Arrays.asList");
        }

        // set modifies underlying array
        fixed.set(1, "B");
        System.out.println("words after fixed.set: " + Arrays.toString(words));

        // For a resizable list, copy into a new ArrayList
        List<String> modifiable = new ArrayList<>(fixed);
        modifiable.add("d");
        System.out.println("modifiable list: " + modifiable);

        // List -> array
        String[] back = modifiable.toArray(new String[0]);
        System.out.println("back to array: " + Arrays.toString(back));

        // Primitives and Arrays.asList: beware (becomes List<int[]> of length 1)
        int[] ints = { 1, 2, 3 };
        List<int[]> wrong = Arrays.asList(ints);
        System.out.println("Arrays.asList(int[]) size: " + wrong.size() + ", element type: " + wrong.get(0).getClass().getSimpleName());
    }

    // -----------------------------------------------------------------------------
    // UTILITIES AND STREAMS
    // -----------------------------------------------------------------------------
    static void streamsAndUtilities() {
        int[] data = { 1, 2, 3, 4 };

        // Streams from arrays
        int sum = Arrays.stream(data).sum();
        double avg = Arrays.stream(data).average().orElse(Double.NaN);
        System.out.println("sum=" + sum + ", avg=" + avg);

        // Fill and setAll
        int[] filled = new int[5];
        Arrays.fill(filled, 7);
        System.out.println("filled with 7: " + Arrays.toString(filled));

        int[] squares = new int[6];
        Arrays.setAll(squares, i -> i * i);
        System.out.println("squares via setAll: " + Arrays.toString(squares));

        // equals / deepEquals
        int[] p1 = { 1, 2, 3 };
        int[] p2 = { 1, 2, 3 };
        System.out.println("Arrays.equals(p1,p2): " + Arrays.equals(p1, p2));

        int[][] d1 = { {1,2}, {3,4} };
        int[][] d2 = { {1,2}, {3,4} };
        System.out.println("Arrays.deepEquals(d1,d2): " + Arrays.deepEquals(d1, d2));

        // toString vs deepToString, and char[] to String
        System.out.println("p1 toString: " + Arrays.toString(p1));
        System.out.println("d1 deepToString: " + Arrays.deepToString(d1));

        char[] chars = { 'J', 'a', 'v', 'a' };
        System.out.println("char[] Arrays.toString: " + Arrays.toString(chars));
        System.out.println("new String(char[]): " + new String(chars));

        // Boxing / unboxing with streams
        Integer[] boxed = Arrays.stream(data).boxed().toArray(Integer[]::new);
        System.out.println("boxed: " + Arrays.toString(boxed));
    }

    // -----------------------------------------------------------------------------
    // COMMON PATTERNS
    // -----------------------------------------------------------------------------
    static void commonPatterns() {
        // Frequency of letters in a word (lowercase a-z)
        String word = "mississippi";
        int[] freq = new int[26];
        for (char ch : word.toCharArray()) {
            if (ch >= 'a' && ch <= 'z') freq[ch - 'a']++;
        }
        System.out.println("freq['i']=" + freq['i' - 'a'] + ", freq['s']=" + freq['s' - 'a'] + ", freq['p']=" + freq['p' - 'a']);

        // Rotate right by k (in-place) using 3 reversals
        int[] r = { 1, 2, 3, 4, 5, 6, 7 };
        rotateRight(r, 3);
        System.out.println("rotateRight by 3: " + Arrays.toString(r));

        // Merge two sorted arrays into a new sorted array
        int[] a = { 1, 3, 5, 7 };
        int[] b = { 2, 4, 6, 8, 9 };
        int[] merged = mergeSorted(a, b);
        System.out.println("merged: " + Arrays.toString(merged));

        // Unique digits (domain 0..9) using boolean seen[]
        int[] digits = { 1, 2, 2, 3, 9, 0, 1 };
        boolean[] seen = new boolean[10];
        int[] unique = Arrays.stream(digits).filter(d -> !seen[d] && (seen[d] = true)).toArray();
        System.out.println("unique digits: " + Arrays.toString(unique));
    }

    // -----------------------------------------------------------------------------
    // PITFALLS
    // -----------------------------------------------------------------------------
    static void pitfalls() {
        // Null reference
        int[] maybeNull = null;
        System.out.println("maybeNull is null? " + (maybeNull == null));

        // Enhanced-for does not let you change array length and does not write primitive changes back
        int[] arr = { 1, 2, 3 };
        for (int x : arr) { x = 42; }
        System.out.println("after for-each x=42: " + Arrays.toString(arr)); // unchanged

        // Mutating arrays used in Collections views
        String[] base = { "A", "B", "C" };
        List<String> view = Arrays.asList(base);
        base[0] = "Z"; // reflected in view
        System.out.println("view after base[0]='Z': " + view);
    }

    // -----------------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------------
    static void header(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }

    static long sum(int[] arr) {
        long s = 0;
        for (int v : arr) s += v;
        return s;
    }

    static void reverseInPlace(int[] arr) {
        for (int i = 0, j = arr.length - 1; i < j; i++, j--) {
            int tmp = arr[i]; arr[i] = arr[j]; arr[j] = tmp;
        }
    }

    static int[] append(int[] arr, int value) {
        int[] out = Arrays.copyOf(arr, arr.length + 1);
        out[out.length - 1] = value;
        return out;
    }

    static double average(int... values) {
        if (values == null || values.length == 0) return Double.NaN;
        return Arrays.stream(values).average().orElse(Double.NaN);
    }

    static void mutateFirstElement(int... values) {
        if (values != null && values.length > 0) values[0] = 12345;
    }

    static int[][] deepCopy2D(int[][] src) {
        if (src == null) return null;
        int[][] out = new int[src.length][];
        for (int i = 0; i < src.length; i++) {
            out[i] = src[i] != null ? Arrays.copyOf(src[i], src[i].length) : null;
        }
        return out;
    }

    static void rotateRight(int[] arr, int k) {
        if (arr == null || arr.length == 0) return;
        k = ((k % arr.length) + arr.length) % arr.length;
        reverse(arr, 0, arr.length - 1);
        reverse(arr, 0, k - 1);
        reverse(arr, k, arr.length - 1);
    }

    static void reverse(int[] arr, int lo, int hi) {
        while (lo < hi) {
            int t = arr[lo]; arr[lo] = arr[hi]; arr[hi] = t;
            lo++; hi--;
        }
    }

    static int[] mergeSorted(int[] a, int[] b) {
        int[] out = new int[a.length + b.length];
        int i = 0, j = 0, k = 0;
        while (i < a.length && j < b.length) {
            out[k++] = (a[i] <= b[j]) ? a[i++] : b[j++];
        }
        while (i < a.length) out[k++] = a[i++];
        while (j < b.length) out[k++] = b[j++];
        return out;
    }
}