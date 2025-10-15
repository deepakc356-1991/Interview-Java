package _01_08_arrays;

import java.util.*;
import java.util.function.IntUnaryOperator;

/**
 * Arrays Interview Q&A: Basic → Intermediate → Advanced
 *
 * How to use:
 * - Read Q&A above each method.
 * - Run main() to see a quick demo; extend as needed.
 *
 * Notes:
 * - Arrays are objects, fixed-length, zero-indexed, contiguous memory of references/values.
 * - For performance, prefer primitives over boxed types when possible.
 */
public class _03_InterviewQA {

    public static void main(String[] args) {
        // Minimal demo; extend for practice
        int[] a = {2, 7, 11, 15};
        System.out.println("twoSumIndices: " + Arrays.toString(twoSumIndices(a, 9)));
        int[] nums = {1, 2, 3, 4};
        System.out.println("productExceptSelf: " + Arrays.toString(productExceptSelf(nums)));
        int[] arr = {0, 1, 0, 3, 12};
        moveZeroesToEnd(arr);
        System.out.println("moveZeroesToEnd: " + Arrays.toString(arr));
        int[] h = {0,1,0,2,1,0,1,3,2,1,2,1};
        System.out.println("trapRainWater: " + trapRainWater(h));
        int[] r = {1,2,3,4,5,6,7};
        rotateRight(r, 3);
        System.out.println("rotateRight by 3: " + Arrays.toString(r));
        int[] dup = {1,3,4,2,2};
        System.out.println("findDuplicateFloyd: " + findDuplicateFloyd(dup));
        int[] k = {3,2,1,5,6,4};
        System.out.println("kthLargest k=2: " + kthLargest(k, 2));
        int[][] matrix = {
                {1, 4, 7, 11},
                {2, 5, 8, 12},
                {3, 6, 9, 16}
        };
        System.out.println("searchMatrixSorted2D(5): " + searchMatrixSorted2D(matrix, 5));
    }

    // ============================ BASIC ============================

    /**
     * Q: How to declare and initialize arrays? What are defaults?
     * A:
     * - int[] a; int a[]; both valid.
     * - new int[n] zero-initializes. Object arrays default to nulls.
     * - Arrays are fixed-size; length is final field.
     * Pitfalls: IndexOutOfBoundsException for invalid indices.
     */
    static void demoDeclarationInitialization() {
        int[] a = new int[3];               // [0,0,0]
        int[] b = {1, 2, 3};                // literal init
        String[] s = new String[2];         // [null, null]
        System.out.println(a.length + " " + Arrays.toString(a));
        System.out.println(Arrays.toString(b));
        System.out.println(Arrays.toString(s));
    }

    /**
     * Q: What is arr.length vs length() vs size()?
     * A:
     * - Arrays use arr.length (field).
     * - Strings use str.length() (method).
     * - Collections use coll.size().
     */
    static void demoLengthVsLengthMethod() {
        int[] a = new int[5];
        System.out.println(a.length);
        System.out.println("abc".length());
        System.out.println(Arrays.asList(1,2,3).size());
    }

    /**
     * Q: clone(), equals(), Arrays.equals(), Arrays.deepEquals(), toString()?
     * A:
     * - clone() makes a shallow copy (deep for primitives).
     * - arr.equals compares reference equality. Use Arrays.equals for primitives.
     * - For nested arrays, use Arrays.deepEquals and Arrays.deepToString.
     */
    static void demoCloneEqualsToString() {
        int[] a = {1, 2, 3};
        int[] b = a.clone();
        System.out.println(a == b);                      // false
        System.out.println(Arrays.equals(a, b));         // true
        int[][] m1 = {{1,2},{3,4}};
        int[][] m2 = {{1,2},{3,4}};
        System.out.println(Arrays.equals(m1, m2));       // false (shallow)
        System.out.println(Arrays.deepEquals(m1, m2));   // true
        System.out.println(Arrays.toString(a));          // [1, 2, 3]
        System.out.println(Arrays.deepToString(m1));     // [[1, 2], [3, 4]]
    }

    /**
     * Q: How to copy arrays efficiently?
     * A:
     * - System.arraycopy(src, srcPos, dest, destPos, len) fast, native.
     * - Arrays.copyOf(arr, newLen) returns new array, pads with defaults if larger.
     * - Arrays.copyOfRange(arr, from, to) half-open [from, to).
     */
    static void demoCopying() {
        int[] a = {1,2,3,4,5};
        int[] b = new int[5];
        System.arraycopy(a, 1, b, 0, 3); // b=[2,3,4,0,0]
        System.out.println(Arrays.toString(b));
        int[] c = Arrays.copyOf(a, 3); // [1,2,3]
        int[] d = Arrays.copyOfRange(a, 2, 10); // [3,4,5,0,0,0,0,0]
        System.out.println(Arrays.toString(c));
        System.out.println(Arrays.toString(d));
    }

    /**
     * Q: Are arrays covariant? What is ArrayStoreException?
     * A:
     * - Arrays are covariant: String[] is a subtype of Object[].
     * - But runtime type checks enforce component type; storing wrong type throws ArrayStoreException.
     */
    static void demoArrayCovariance() {
        Object[] objs = new String[2];
        try {
            objs[0] = 42; // runtime ArrayStoreException
        } catch (ArrayStoreException ex) {
            System.out.println("ArrayStoreException caught");
        }
    }

    /**
     * Q: What are varargs? Are they arrays? Do modifications reflect?
     * A:
     * - Varargs compiles to an array parameter.
     * - If caller passes an existing array, callee sees same array (mutations visible).
     * - If caller passes literals, compiler creates a new array (mutations affect only that new array).
     */
    static void demoVarargsArrayRelation() {
        int[] a = {1,2,3};
        mutateVarargs(a); // affects 'a'
        System.out.println(Arrays.toString(a));
        mutateVarargs(7,8,9); // mutates ephemeral array; nothing else referenced it.
    }
    static void mutateVarargs(int... arr) { if (arr.length > 0) arr[0] = -1; }

    /**
     * Q: What does 'final int[] a' mean?
     * A:
     * - The reference is final; you cannot reassign a = new int[] {}.
     * - Elements can still change: a[0] = 5 is allowed.
     */
    static void demoFinalArray() {
        final int[] a = {1,2,3};
        a[0] = 9; // ok
        // a = new int[]{4,5}; // compile error
    }

    /**
     * Q: Multidimensional arrays vs Jagged arrays?
     * A:
     * - Java uses arrays-of-arrays; inner arrays can have different lengths (jagged).
     * - 2D arrays are not guaranteed to be contiguous in memory.
     */
    static void demoMultidimensionalJagged() {
        int[][] rect = new int[2][3];       // 2 rows, 3 cols each
        int[][] jagged = new int[2][];
        jagged[0] = new int[1];
        jagged[1] = new int[5];
        System.out.println(Arrays.deepToString(rect));
        System.out.println(Arrays.deepToString(jagged));
    }

    /**
     * Q: Pitfall: Arrays.asList with primitives?
     * A:
     * - Arrays.asList(int[]) treats the entire int[] as a single element.
     * - Use Arrays.stream(arr).boxed().toList() (Java 16+) or manual boxing.
     * - Arrays.asList returns fixed-size list backed by the array: add/remove not supported.
     */
    static void demoArraysAsListPitfall() {
        int[] a = {1,2,3};
        List<int[]> wrong = Arrays.asList(a);          // size 1
        System.out.println("wrong size=" + wrong.size());
        List<Integer> ok = new ArrayList<>();
        for (int x : a) ok.add(x);
        System.out.println("ok size=" + ok.size());
        String[] s = {"a","b"};
        List<String> backed = Arrays.asList(s);
        // backed.add("c"); // throws UnsupportedOperationException
        s[0] = "z"; // reflects in list
        System.out.println(backed);
    }

    /**
     * Q: Streams with arrays?
     * A:
     * - Arrays.stream(int[]) gives IntStream (primitive).
     * - For Object[], use Arrays.stream(T[]).
     * - Collectors.toList()/toSet() available for boxed types.
     */
    static void demoStreamsFromArray() {
        int sum = Arrays.stream(new int[]{1,2,3}).map(x -> x * x).sum();
        System.out.println(sum);
        List<String> up = Arrays.stream(new String[]{"a","b"})
                .map(String::toUpperCase).toList();
        System.out.println(up);
    }

    /**
     * Q: How does Arrays.binarySearch work? Return contract?
     * A:
     * - On found: index.
     * - If not found: (-(insertionPoint) - 1). To compute insertion point: ip = -ret - 1.
     * Requires array sorted ascending with same comparator as search.
     */
    static void demoBinarySearchReturnContract() {
        int[] a = {1,3,5,7};
        System.out.println(Arrays.binarySearch(a, 5));   // 2
        int r = Arrays.binarySearch(a, 4);               // -3 => insertion at 2
        System.out.println(r + " insertion=" + (-r - 1));
    }

    /**
     * Q: Sorting arrays: stability and complexity?
     * A:
     * - Arrays.sort(primitives): Dual-Pivot Quicksort (unstable), O(n log n).
     * - Arrays.sort(objects): TimSort (stable), O(n log n), exploits runs.
     * - parallelSort may help on large arrays, uses ForkJoin.
     */
    static void demoSorts() {
        int[] a = {3,1,2};
        Arrays.sort(a);
        System.out.println(Arrays.toString(a));
        String[] s = {"b","aa","a"};
        Arrays.sort(s, Comparator.comparingInt(String::length)); // stable for objects
        System.out.println(Arrays.toString(s));
    }

    // ======================== INTERMEDIATE ========================

    /**
     * Q: Reverse array in-place?
     * A: Two-pointer swap ends until cross. O(n) time, O(1) space.
     */
    public static void reverseInPlace(int[] a) {
        for (int i = 0, j = a.length - 1; i < j; i++, j--) swap(a, i, j);
    }

    private static void swap(int[] a, int i, int j) {
        int t = a[i]; a[i] = a[j]; a[j] = t;
    }

    /**
     * Q: Rotate array right by k steps in-place?
     * A: Reverse whole array, reverse first k, reverse rest. O(n), O(1).
     */
    public static void rotateRight(int[] a, int k) {
        int n = a.length; if (n == 0) return;
        k = ((k % n) + n) % n;
        reverse(a, 0, n - 1);
        reverse(a, 0, k - 1);
        reverse(a, k, n - 1);
    }
    private static void reverse(int[] a, int l, int r) { while (l < r) swap(a, l++, r--); }

    /**
     * Q: Move all zeroes to end maintaining order?
     * A: Two-pointer write index. O(n), O(1).
     */
    public static void moveZeroesToEnd(int[] a) {
        int w = 0;
        for (int x : a) if (x != 0) a[w++] = x;
        while (w < a.length) a[w++] = 0;
    }

    /**
     * Q: Remove duplicates from sorted array, return new length?
     * A: Two-pointer; overwrite in place. O(n), O(1).
     */
    public static int removeDuplicatesSorted(int[] a) {
        if (a.length == 0) return 0;
        int w = 1;
        for (int i = 1; i < a.length; i++) if (a[i] != a[i-1]) a[w++] = a[i];
        return w;
    }

    /**
     * Q: Two Sum indices in unsorted array?
     * A: HashMap value→index. O(n) time, O(n) space.
     */
    public static int[] twoSumIndices(int[] a, int target) {
        Map<Integer, Integer> map = new HashMap<>();
        for (int i = 0; i < a.length; i++) {
            int need = target - a[i];
            if (map.containsKey(need)) return new int[]{map.get(need), i};
            map.put(a[i], i);
        }
        return new int[]{-1,-1};
    }

    /**
     * Q: Merge two sorted arrays in-place: A has size m+n, B size n.
     * A: Merge from the end. O(m+n).
     */
    public static void mergeSortedInPlace(int[] A, int m, int[] B, int n) {
        int i = m - 1, j = n - 1, k = m + n - 1;
        while (j >= 0) {
            if (i >= 0 && A[i] > B[j]) A[k--] = A[i--];
            else A[k--] = B[j--];
        }
    }

    /**
     * Q: Find missing number in array 0..n, size n?
     * A: XOR or sum formula. XOR avoids overflow. O(n), O(1).
     */
    public static int missingNumberXor(int[] a) {
        int xor = a.length;
        for (int i = 0; i < a.length; i++) xor ^= i ^ a[i];
        return xor;
    }

    /**
     * Q: Find duplicate number in [1..n] with n+1 length without modifying array and O(1) extra space.
     * A: Floyd's Tortoise-Hare on value-as-next pointer. O(n).
     */
    public static int findDuplicateFloyd(int[] a) {
        int slow = a[0], fast = a[a[0]];
        while (slow != fast) { slow = a[slow]; fast = a[a[fast]]; }
        slow = 0;
        while (slow != fast) { slow = a[slow]; fast = a[fast]; }
        return slow;
    }

    /**
     * Q: Majority element (> n/2 occurrences)?
     * A: Boyer-Moore voting. O(n), O(1). Verify if necessary.
     */
    public static int majorityElementBoyerMoore(int[] a) {
        int cand = 0, cnt = 0;
        for (int x : a) {
            if (cnt == 0) { cand = x; cnt = 1; }
            else if (x == cand) cnt++;
            else cnt--;
        }
        return cand;
    }

    /**
     * Q: Maximum subarray sum?
     * A: Kadane's algorithm. O(n), O(1).
     */
    public static int kadaneMaxSubArray(int[] a) {
        int best = Integer.MIN_VALUE, cur = 0;
        for (int x : a) {
            cur = Math.max(x, cur + x);
            best = Math.max(best, cur);
        }
        return best;
    }

    /**
     * Q: Product of array except self without division?
     * A: Prefix and suffix products. O(n), O(1) extra (excluding output).
     */
    public static int[] productExceptSelf(int[] a) {
        int n = a.length;
        int[] res = new int[n];
        int pref = 1;
        for (int i = 0; i < n; i++) { res[i] = pref; pref *= a[i]; }
        int suff = 1;
        for (int i = n - 1; i >= 0; i--) { res[i] *= suff; suff *= a[i]; }
        return res;
    }

    /**
     * Q: Trapping Rain Water?
     * A: Two-pointer with leftMax/rightMax. O(n), O(1).
     */
    public static int trapRainWater(int[] h) {
        int l = 0, r = h.length - 1, lMax = 0, rMax = 0, water = 0;
        while (l < r) {
            if (h[l] <= h[r]) {
                lMax = Math.max(lMax, h[l]);
                water += lMax - h[l++];
            } else {
                rMax = Math.max(rMax, h[r]);
                water += rMax - h[r--];
            }
        }
        return water;
    }

    /**
     * Q: Longest consecutive sequence (unordered)?
     * A: HashSet starts of sequences. O(n) average.
     */
    public static int longestConsecutive(int[] a) {
        Set<Integer> set = new HashSet<>();
        for (int x : a) set.add(x);
        int best = 0;
        for (int x : set) if (!set.contains(x - 1)) {
            int y = x, len = 1;
            while (set.contains(y + 1)) { y++; len++; }
            best = Math.max(best, len);
        }
        return best;
    }

    /**
     * Q: Kth largest element?
     * A: Quickselect average O(n), worst O(n^2), or heap O(n log k).
     */
    public static int kthLargest(int[] a, int k) {
        return quickSelect(a, 0, a.length - 1, a.length - k);
    }
    private static int quickSelect(int[] a, int l, int r, int k) {
        while (true) {
            int p = partition(a, l, r, medianOfThree(a, l, r));
            if (p == k) return a[p];
            if (p < k) l = p + 1; else r = p - 1;
        }
    }
    private static int partition(int[] a, int l, int r, int pivot) {
        int i = l, j = r;
        while (i <= j) {
            while (a[i] < pivot) i++;
            while (a[j] > pivot) j--;
            if (i <= j) { swap(a, i, j); i++; j--; }
        }
        return i - 1 >= l && a[i - 1] <= pivot ? i - 1 : i;
    }
    private static int medianOfThree(int[] a, int l, int r) {
        int m = l + (r - l) / 2;
        int x = a[l], y = a[m], z = a[r];
        if ((x <= y && y <= z) || (z <= y && y <= x)) return y;
        if ((y <= x && x <= z) || (z <= x && x <= y)) return x;
        return z;
    }

    /**
     * Q: Dutch National Flag (sort 0,1,2)?
     * A: Three pointers: low, mid, high. O(n).
     */
    public static void dutchFlagSort012(int[] a) {
        int low = 0, mid = 0, high = a.length - 1;
        while (mid <= high) {
            if (a[mid] == 0) swap(a, low++, mid++);
            else if (a[mid] == 1) mid++;
            else swap(a, mid, high--);
        }
    }

    /**
     * Q: Binary search variations?
     * A: exact, lower_bound (first >= x), upper_bound (first > x), first/last occurrence.
     */
    public static int binarySearchExact(int[] a, int x) {
        int l = 0, r = a.length - 1;
        while (l <= r) {
            int m = (l + r) >>> 1;
            if (a[m] == x) return m;
            if (a[m] < x) l = m + 1; else r = m - 1;
        }
        return -1;
    }
    public static int lowerBound(int[] a, int x) {
        int l = 0, r = a.length;
        while (l < r) {
            int m = (l + r) >>> 1;
            if (a[m] < x) l = m + 1; else r = m;
        }
        return l;
    }
    public static int upperBound(int[] a, int x) {
        int l = 0, r = a.length;
        while (l < r) {
            int m = (l + r) >>> 1;
            if (a[m] <= x) l = m + 1; else r = m;
        }
        return l;
    }

    /**
     * Q: Search in rotated sorted array (distinct values)?
     * A: Modified binary search. O(n) worst if duplicates; with distinct O(log n).
     */
    public static int searchInRotatedSorted(int[] a, int target) {
        int l = 0, r = a.length - 1;
        while (l <= r) {
            int m = (l + r) >>> 1;
            if (a[m] == target) return m;
            if (a[l] <= a[m]) { // left sorted
                if (a[l] <= target && target < a[m]) r = m - 1; else l = m + 1;
            } else { // right sorted
                if (a[m] < target && target <= a[r]) l = m + 1; else r = m - 1;
            }
        }
        return -1;
    }

    /**
     * Q: Count inversions (# of pairs i<j with a[i] > a[j])?
     * A: Merge sort with count. O(n log n).
     */
    public static long countInversions(int[] a) {
        int[] tmp = new int[a.length];
        return mergeSortCount(a, tmp, 0, a.length - 1);
    }
    private static long mergeSortCount(int[] a, int[] t, int l, int r) {
        if (l >= r) return 0;
        int m = (l + r) >>> 1;
        long cnt = mergeSortCount(a, t, l, m) + mergeSortCount(a, t, m + 1, r);
        int i = l, j = m + 1, k = l;
        while (i <= m && j <= r) {
            if (a[i] <= a[j]) t[k++] = a[i++];
            else { t[k++] = a[j++]; cnt += (m - i + 1); }
        }
        while (i <= m) t[k++] = a[i++];
        while (j <= r) t[k++] = a[j++];
        System.arraycopy(t, l, a, l, r - l + 1);
        return cnt;
    }

    /**
     * Q: Prefix sums for range sum queries?
     * A: Build prefix P where P[i+1] = P[i] + a[i]. Sum(l..r) = P[r+1]-P[l]. O(1) per query.
     */
    public static long[] prefixSums(int[] a) {
        long[] p = new long[a.length + 1];
        for (int i = 0; i < a.length; i++) p[i + 1] = p[i] + a[i];
        return p;
    }
    public static long sumRange(long[] p, int l, int r) { return p[r + 1] - p[l]; }

    /**
     * Q: Count subarrays with sum == k (allow negatives)?
     * A: Prefix sum + hashmap counts. O(n).
     */
    public static int subarraySumEqualsK(int[] a, int k) {
        Map<Integer, Integer> freq = new HashMap<>();
        freq.put(0, 1);
        int pref = 0, ans = 0;
        for (int x : a) {
            pref += x;
            ans += freq.getOrDefault(pref - k, 0);
            freq.put(pref, freq.getOrDefault(pref, 0) + 1);
        }
        return ans;
    }

    /**
     * Q: Min length subarray with sum >= target (all positive numbers)?
     * A: Sliding window. O(n).
     */
    public static int minSubArrayLenAtLeastTarget(int target, int[] a) {
        int l = 0, sum = 0, best = Integer.MAX_VALUE;
        for (int r = 0; r < a.length; r++) {
            sum += a[r];
            while (sum >= target) {
                best = Math.min(best, r - l + 1);
                sum -= a[l++];
            }
        }
        return best == Integer.MAX_VALUE ? 0 : best;
    }

    /**
     * Q: Range increment updates efficient? (Difference array)
     * A: For updates [l, r] += val: diff[l]+=val; diff[r+1]-=val; prefix to materialize. O(1) per update, O(n) build.
     */
    public static int[] applyRangeIncrements(int n, int[][] updates) {
        int[] diff = new int[n + 1];
        for (int[] u : updates) {
            int l = u[0], r = u[1], v = u[2];
            diff[l] += v;
            if (r + 1 < diff.length) diff[r + 1] -= v;
        }
        int[] res = new int[n];
        int run = 0;
        for (int i = 0; i < n; i++) { run += diff[i]; res[i] = run; }
        return res;
    }

    // ========================== ADVANCED ==========================

    /**
     * Q: Rotate square matrix 90° clockwise in-place?
     * A: Transpose then reverse each row. O(n^2).
     */
    public static void rotateMatrix90Clockwise(int[][] m) {
        int n = m.length;
        for (int i = 0; i < n; i++) for (int j = i + 1; j < n; j++) {
            int t = m[i][j]; m[i][j] = m[j][i]; m[j][i] = t;
        }
        for (int i = 0; i < n; i++) reverseRow(m[i]);
    }
    private static void reverseRow(int[] row) { reverse(row, 0, row.length - 1); }

    /**
     * Q: Set Matrix Zeroes: if an element is 0, set its row and column to 0 (in-place, O(1) extra)?
     * A: Use first row/column as markers, with extra flags. O(mn).
     */
    public static void setMatrixZeroes(int[][] mat) {
        int m = mat.length, n = mat[0].length;
        boolean fr0 = false, fc0 = false;
        for (int j = 0; j < n; j++) if (mat[0][j] == 0) fr0 = true;
        for (int i = 0; i < m; i++) if (mat[i][0] == 0) fc0 = true;
        for (int i = 1; i < m; i++)
            for (int j = 1; j < n; j++)
                if (mat[i][j] == 0) { mat[i][0] = 0; mat[0][j] = 0; }
        for (int i = 1; i < m; i++) if (mat[i][0] == 0) Arrays.fill(mat[i], 0);
        for (int j = 1; j < n; j++) if (mat[0][j] == 0) for (int i = 0; i < m; i++) mat[i][j] = 0;
        if (fr0) Arrays.fill(mat[0], 0);
        if (fc0) for (int i = 0; i < m; i++) mat[i][0] = 0;
    }

    /**
     * Q: Spiral order of matrix?
     * A: Traverse bounds and shrink. O(mn).
     */
    public static List<Integer> spiralOrder(int[][] mat) {
        int m = mat.length, n = mat[0].length;
        List<Integer> out = new ArrayList<>(m * n);
        int top = 0, bottom = m - 1, left = 0, right = n - 1;
        while (top <= bottom && left <= right) {
            for (int j = left; j <= right; j++) out.add(mat[top][j]); top++;
            for (int i = top; i <= bottom; i++) out.add(mat[i][right]); right--;
            if (top <= bottom) { for (int j = right; j >= left; j--) out.add(mat[bottom][j]); bottom--; }
            if (left <= right) { for (int i = bottom; i >= top; i--) out.add(mat[i][left]); left++; }
        }
        return out;
    }

    /**
     * Q: Search 2D matrix sorted by rows and columns ascending (LeetCode 240)?
     * A: Start top-right; move left or down. O(m+n).
     */
    public static boolean searchMatrixSorted2D(int[][] mat, int target) {
        int m = mat.length, n = mat[0].length, i = 0, j = n - 1;
        while (i < m && j >= 0) {
            int v = mat[i][j];
            if (v == target) return true;
            if (v > target) j--; else i++;
        }
        return false;
    }

    /**
     * Q: Next Greater Element for each index?
     * A: Monotonic decreasing stack of indices. O(n).
     */
    public static int[] nextGreaterElement(int[] a) {
        int n = a.length;
        int[] res = new int[n];
        Arrays.fill(res, -1);
        Deque<Integer> st = new ArrayDeque<>();
        for (int i = 0; i < n; i++) {
            while (!st.isEmpty() && a[i] > a[st.peek()]) res[st.pop()] = a[i];
            st.push(i);
        }
        return res;
    }

    /**
     * Q: Largest Rectangle in Histogram?
     * A: Monotonic increasing stack. O(n).
     */
    public static int largestRectangleInHistogram(int[] h) {
        int n = h.length, max = 0;
        Deque<Integer> st = new ArrayDeque<>();
        for (int i = 0; i <= n; i++) {
            int cur = (i == n) ? 0 : h[i];
            while (!st.isEmpty() && cur < h[st.peek()]) {
                int height = h[st.pop()];
                int left = st.isEmpty() ? 0 : st.peek() + 1;
                int width = i - left;
                max = Math.max(max, height * width);
            }
            st.push(i);
        }
        return max;
    }

    /**
     * Q: Merge intervals?
     * A: Sort by start, merge overlaps. O(n log n).
     */
    public static int[][] mergeIntervals(int[][] intervals) {
        if (intervals.length == 0) return new int[0][];
        Arrays.sort(intervals, Comparator.comparingInt(x -> x[0]));
        List<int[]> out = new ArrayList<>();
        int[] cur = intervals[0].clone();
        for (int i = 1; i < intervals.length; i++) {
            int[] it = intervals[i];
            if (it[0] <= cur[1]) cur[1] = Math.max(cur[1], it[1]);
            else { out.add(cur); cur = it.clone(); }
        }
        out.add(cur);
        return out.toArray(new int[0][]);
    }

    /**
     * Q: Max Sliding Window of size k?
     * A: Monotonic deque storing indices. O(n).
     */
    public static int[] maxSlidingWindow(int[] a, int k) {
        if (k <= 0 || a.length == 0) return new int[0];
        int n = a.length;
        int[] res = new int[n - k + 1];
        Deque<Integer> dq = new ArrayDeque<>();
        for (int i = 0; i < n; i++) {
            while (!dq.isEmpty() && dq.peekFirst() <= i - k) dq.pollFirst();
            while (!dq.isEmpty() && a[dq.peekLast()] <= a[i]) dq.pollLast();
            dq.offerLast(i);
            if (i >= k - 1) res[i - k + 1] = a[dq.peekFirst()];
        }
        return res;
    }

    /**
     * Q: Maximum product subarray?
     * A: Track max and min ending here (handle negatives). O(n).
     */
    public static int maxProductSubarray(int[] a) {
        int maxHere = a[0], minHere = a[0], best = a[0];
        for (int i = 1; i < a.length; i++) {
            int x = a[i];
            if (x < 0) { int t = maxHere; maxHere = minHere; minHere = t; }
            maxHere = Math.max(x, maxHere * x);
            minHere = Math.min(x, minHere * x);
            best = Math.max(best, maxHere);
        }
        return best;
    }

    /**
     * Q: Remove element in-place (value val) and return new length?
     * A: Two-pointer overwrite. O(n), O(1). Order not required unless specified.
     */
    public static int removeElement(int[] a, int val) {
        int w = 0;
        for (int x : a) if (x != val) a[w++] = x;
        return w;
    }

    // ======================= DESIGN + PITFALLS =====================

    /**
     * Q: Arrays vs ArrayList?
     * A:
     * - Arrays: fixed-size, can hold primitives, contiguous memory, no add/remove.
     * - ArrayList: dynamic size, Objects only (autoboxing for primitives), add/remove, amortized growth.
     * Performance: primitive arrays avoid boxing and are cache-friendly.
     */

    /**
     * Q: Why "Cannot create a generic array" like new T[]?
     * A:
     * - Arrays are reified (know component type at runtime).
     * - Generics are erased; runtime doesn't know T, so new T[] is illegal.
     * Workarounds: use List<T>; or pass Class<T> and use Array.newInstance.
     */

    /**
     * Q: Memory of arrays?
     * A:
     * - Each array has header + alignment + elements.
     * - Object arrays store references; elements are separate objects.
     * - int[] more compact than Integer[] (boxing overhead).
     */

    /**
     * Q: Thread-safety?
     * A:
     * - Arrays are not thread-safe containers.
     * - Use defensive copies when sharing; for atomics: AtomicIntegerArray, etc.
     */

    // ========================= SMALL UTILITIES =========================

    /**
     * Map array in-place with IntUnaryOperator. Demonstrates setAll/parallelSetAll.
     */
    public static void mapInPlace(int[] a, IntUnaryOperator f) {
        for (int i = 0; i < a.length; i++) a[i] = f.applyAsInt(a[i]);
    }

    /**
     * Fill and setAll utilities.
     */
    static void demoFillAndSetAll() {
        int[] a = new int[5];
        Arrays.fill(a, 7);
        Arrays.setAll(a, i -> i * i);
        // Arrays.parallelSetAll(a, i -> i); // potential parallel init
        System.out.println(Arrays.toString(a));
    }
}