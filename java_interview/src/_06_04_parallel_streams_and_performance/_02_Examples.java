package _06_04_parallel_streams_and_performance;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Parallel Streams & Performance
 *
 * This file contains a collection of small, focused examples demonstrating:
 * - When parallel streams help or hurt performance
 * - Source/spliterator characteristics and their impact
 * - Boxing costs and primitive streams
 * - Ordering constraints (forEachOrdered vs forEach, unordered)
 * - Side-effects and thread-safety pitfalls
 * - Associativity requirement for reduce
 * - Concurrent collectors
 * - reduce vs collect for mutable accumulation
 * - Short-circuiting operations
 * - Unbalanced workloads
 * - Arrays.parallelSort
 * - iterate vs range
 * - Custom ForkJoinPool for parallel streams
 *
 * Notes:
 * - This is not a benchmarking framework. Numbers are illustrative only.
 * - Keep input sizes modest so the examples run quickly.
 */
public class _02_Examples {

    // Used to prevent dead-code elimination during ad-hoc timing.
    private static volatile Object BLACKHOLE;

    public static void main(String[] args) throws Exception {
        System.out.println("Parallel Streams & Performance — examples");
        System.out.println("Available processors: " + Runtime.getRuntime().availableProcessors());
        System.out.println("CommonPool parallelism: " + ForkJoinPool.getCommonPoolParallelism());
        System.out.println();

        cpuBoundSequentialVsParallel();
        smallVsLargeData();
        sourceMattersArrayListVsLinkedList();
        primitiveStreamsAvoidBoxing();
        nonAssociativeReduce();
        forEachOrderedVsForEach();
        sideEffectsSharedMutability();
        concurrentCollectors();
        reduceVsCollectStringBuilder();
        unorderedHint();
        shortCircuitAnyMatch();
        unbalancedWorkload();
        arraysParallelSort();
        iterateVsRange();
        customForkJoinPoolExample();
    }

    // ------------------------------------------------------------
    // Example 1: CPU-bound work — parallel often helps
    // ------------------------------------------------------------
    private static void cpuBoundSequentialVsParallel() {
        System.out.println("[1] CPU-bound: count primes (sequential vs parallel)");
        final int N = 200_000;

        final long[] resultSeq = new long[1];
        bench("Primes (sequential)", 1, 2, () -> {
            long c = IntStream.rangeClosed(2, N).filter(_02_Examples::isPrime).count();
            resultSeq[0] = c;
            BLACKHOLE = c;
        });

        final long[] resultPar = new long[1];
        bench("Primes (parallel)", 1, 2, () -> {
            long c = IntStream.rangeClosed(2, N).parallel().filter(_02_Examples::isPrime).count();
            resultPar[0] = c;
            BLACKHOLE = c;
        });

        System.out.println("Counts equal: " + (resultSeq[0] == resultPar[0]) + " (count=" + resultSeq[0] + ")");
        System.out.println();
    }

    // ------------------------------------------------------------
    // Example 2: Data size matters — small data may be slower in parallel
    // ------------------------------------------------------------
    private static void smallVsLargeData() {
        System.out.println("[2] Data size: small vs large arrays");
        int[] small = randomIntArray(10_000);
        int[] large = randomIntArray(1_000_000);

        bench("Small sum (sequential)", 1, 3, () -> {
            long s = Arrays.stream(small).asLongStream().sum();
            BLACKHOLE = s;
        });
        bench("Small sum (parallel)", 1, 3, () -> {
            long s = Arrays.stream(small).parallel().asLongStream().sum();
            BLACKHOLE = s;
        });

        bench("Large sum (sequential)", 1, 2, () -> {
            long s = Arrays.stream(large).asLongStream().sum();
            BLACKHOLE = s;
        });
        bench("Large sum (parallel)", 1, 2, () -> {
            long s = Arrays.stream(large).parallel().asLongStream().sum();
            BLACKHOLE = s;
        });
        System.out.println();
    }

    // ------------------------------------------------------------
    // Example 3: Source matters — ArrayList vs LinkedList
    // ------------------------------------------------------------
    private static void sourceMattersArrayListVsLinkedList() {
        System.out.println("[3] Source characteristics: ArrayList vs LinkedList");
        final int N = 200_000;
        List<Integer> arrayList = new ArrayList<>(N);
        for (int i = 0; i < N; i++) arrayList.add(i);
        List<Integer> linkedList = new LinkedList<>();
        for (int i = 0; i < N; i++) linkedList.add(i);

        bench("ArrayList sum (sequential)", 1, 3, () -> {
            long s = arrayList.stream().mapToLong(i -> i).sum();
            BLACKHOLE = s;
        });
        bench("ArrayList sum (parallel)", 1, 3, () -> {
            long s = arrayList.parallelStream().mapToLong(i -> i).sum();
            BLACKHOLE = s;
        });

        bench("LinkedList sum (sequential)", 1, 3, () -> {
            long s = linkedList.stream().mapToLong(i -> i).sum();
            BLACKHOLE = s;
        });
        bench("LinkedList sum (parallel)", 1, 3, () -> {
            long s = linkedList.parallelStream().mapToLong(i -> i).sum();
            BLACKHOLE = s;
        });
        System.out.println();
    }

    // ------------------------------------------------------------
    // Example 4: Boxing overhead — prefer primitive streams
    // ------------------------------------------------------------
    private static void primitiveStreamsAvoidBoxing() {
        System.out.println("[4] Boxing overhead: Stream<Integer> vs IntStream");
        final int N = 1_000_000;
        List<Integer> boxed = IntStream.range(0, N).boxed().collect(Collectors.toList());

        bench("Boxed reduce (sequential)", 1, 2, () -> {
            int s = boxed.stream().map(i -> i * i).reduce(0, Integer::sum);
            BLACKHOLE = s;
        });
        bench("Primitive sum (sequential)", 1, 2, () -> {
            long s = IntStream.range(0, N).map(i -> i * i).asLongStream().sum();
            BLACKHOLE = s;
        });
        bench("Boxed reduce (parallel)", 1, 2, () -> {
            int s = boxed.parallelStream().map(i -> i * i).reduce(0, Integer::sum);
            BLACKHOLE = s;
        });
        bench("Primitive sum (parallel)", 1, 2, () -> {
            long s = IntStream.range(0, N).parallel().map(i -> i * i).asLongStream().sum();
            BLACKHOLE = s;
        });
        System.out.println();
    }

    // ------------------------------------------------------------
    // Example 5: Associativity rule — reduce must be associative
    // ------------------------------------------------------------
    private static void nonAssociativeReduce() {
        System.out.println("[5] Non-associative reduce yields wrong results in parallel");
        final int N = 100_000;
        int seq = IntStream.rangeClosed(1, N).reduce(0, (a, b) -> a - b);
        int par = IntStream.rangeClosed(1, N).parallel().reduce(0, (a, b) -> a - b);
        int expected = -N * (N + 1) / 2; // For sequential (((0-1)-2)-3)...
        System.out.println("Sequential result   : " + seq + " (expected " + expected + ")");
        System.out.println("Parallel result     : " + par + " (nondeterministic, wrong)");
        System.out.println();
    }

    // ------------------------------------------------------------
    // Example 6: Ordering constraints — forEachOrdered vs forEach
    // ------------------------------------------------------------
    private static void forEachOrderedVsForEach() {
        System.out.println("[6] Ordering constraints: forEachOrdered vs forEach");
        final int N = 150_000;
        List<Integer> list = IntStream.range(0, N).boxed().collect(Collectors.toList());

        bench("forEachOrdered (parallel)", 1, 2, () -> {
            list.parallelStream()
                .mapToDouble(i -> busyWork(i % 64))
                .forEachOrdered(v -> BLACKHOLE = v);
        });
        bench("forEach (parallel)", 1, 2, () -> {
            list.parallelStream()
                .mapToDouble(i -> busyWork(i % 64))
                .forEach(v -> BLACKHOLE = v);
        });
        System.out.println();
    }

    // ------------------------------------------------------------
    // Example 7: Side-effects — shared mutable state is unsafe
    // ------------------------------------------------------------
    private static void sideEffectsSharedMutability() {
        System.out.println("[7] Side-effects: shared mutable state is unsafe in parallel");
        final int N = 100_000;
        int expected = (N - 1) * N / 2;

        int[] sum = new int[1];
        IntStream.range(0, N).parallel().forEach(i -> sum[0] += i); // race
        System.out.println("Unsafe sum (race)   : " + sum[0] + " (expected " + expected + ")");

        AtomicInteger safe = new AtomicInteger();
        IntStream.range(0, N).parallel().forEach(i -> safe.addAndGet(i));
        System.out.println("Atomic sum (correct): " + safe.get());

        bench("Atomic addAndGet (parallel)", 1, 2, () -> {
            AtomicInteger ai = new AtomicInteger();
            IntStream.range(0, N).parallel().forEach(i -> ai.addAndGet(i));
            BLACKHOLE = ai.get();
        });
        bench("Proper reduction (parallel)", 1, 2, () -> {
            long s = IntStream.range(0, N).parallel().asLongStream().sum();
            BLACKHOLE = s;
        });
        System.out.println();
    }

    // ------------------------------------------------------------
    // Example 8: Concurrent collectors — groupingByConcurrent
    // ------------------------------------------------------------
    private static void concurrentCollectors() {
        System.out.println("[8] Concurrent collectors: groupingBy vs groupingByConcurrent");
        final int TOTAL = 200_000;
        final int DISTINCT = 1_000;

        List<String> vocab = IntStream.range(0, DISTINCT).mapToObj(i -> "w" + i).collect(Collectors.toList());
        SplittableRandom rnd = new SplittableRandom(42);
        List<String> words = new ArrayList<>(TOTAL);
        for (int i = 0; i < TOTAL; i++) {
            words.add(vocab.get(rnd.nextInt(DISTINCT)));
        }

        final Map<String, Long>[] m1 = new Map[1];
        bench("groupingBy (sequential)", 1, 2, () -> {
            Map<String, Long> r = words.stream()
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
            m1[0] = r;
            BLACKHOLE = r;
        });

        final Map<String, Long>[] m2 = new Map[1];
        bench("groupingBy (parallel)", 1, 2, () -> {
            Map<String, Long> r = words.parallelStream()
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
            m2[0] = r;
            BLACKHOLE = r;
        });

        final Map<String, Long>[] m3 = new Map[1];
        bench("groupingByConcurrent (parallel)", 1, 2, () -> {
            Map<String, Long> r = words.parallelStream()
                    .collect(Collectors.groupingByConcurrent(Function.identity(), Collectors.counting()));
            m3[0] = r;
            BLACKHOLE = r;
        });

        System.out.println("Results equal (seq vs par): " + m1[0].equals(m2[0]));
        System.out.println("Results equal (seq vs conc): " + m1[0].equals(m3[0]));
        System.out.println();
    }

    // ------------------------------------------------------------
    // Example 9: reduce vs collect — mutable accumulation
    // ------------------------------------------------------------
    private static void reduceVsCollectStringBuilder() {
        System.out.println("[9] reduce vs collect for String concatenation");
        List<String> parts = IntStream.range(0, 50_000).mapToObj(i -> "x").collect(Collectors.toList());

        bench("String::concat via reduce (seq)", 1, 1, () -> {
            String s = parts.stream().reduce("", String::concat);
            BLACKHOLE = s;
        });
        bench("StringBuilder via collect (par)", 1, 1, () -> {
            String s = parts.parallelStream()
                    .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                    .toString();
            BLACKHOLE = s;
        });
        bench("Collectors.joining (par)", 1, 1, () -> {
            String s = parts.parallelStream().collect(Collectors.joining());
            BLACKHOLE = s;
        });
        System.out.println();
    }

    // ------------------------------------------------------------
    // Example 10: unordered() hint can reduce overhead
    // ------------------------------------------------------------
    private static void unorderedHint() {
        System.out.println("[10] unordered() to relax ordering constraints");
        final int N = 100_000;
        List<Integer> list = IntStream.range(0, N).boxed().collect(Collectors.toList());

        bench("Collect toList (ordered, par)", 1, 2, () -> {
            List<Integer> r = list.parallelStream().map(i -> i * 2).collect(Collectors.toList());
            BLACKHOLE = r.size();
        });
        bench("Collect toList (unordered, par)", 1, 2, () -> {
            List<Integer> r = list.parallelStream().unordered().map(i -> i * 2).collect(Collectors.toList());
            BLACKHOLE = r.size();
        });
        System.out.println();
    }

    // ------------------------------------------------------------
    // Example 11: Short-circuit operations — anyMatch
    // ------------------------------------------------------------
    private static void shortCircuitAnyMatch() {
        System.out.println("[11] Short-circuiting: anyMatch (seq vs par)");
        final int N = 200_000;
        final int target = N - 1;

        bench("anyMatch (sequential)", 1, 2, () -> {
            boolean found = IntStream.range(0, N).anyMatch(i -> {
                busyWork(16);
                return i == target;
            });
            BLACKHOLE = found;
        });
        bench("anyMatch (parallel)", 1, 2, () -> {
            boolean found = IntStream.range(0, N).parallel().anyMatch(i -> {
                busyWork(16);
                return i == target;
            });
            BLACKHOLE = found;
        });
        System.out.println();
    }

    // ------------------------------------------------------------
    // Example 12: Unbalanced workload — parallel helps balance
    // ------------------------------------------------------------
    private static void unbalancedWorkload() {
        System.out.println("[12] Unbalanced workload: parallel work-stealing can help");
        final int N = 120_000;
        SplittableRandom rnd = new SplittableRandom(123);
        int[] work = new int[N];
        for (int i = 0; i < N; i++) work[i] = 4 + rnd.nextInt(64); // variable cost

        bench("Unbalanced (sequential)", 1, 2, () -> {
            double sum = IntStream.range(0, N).mapToDouble(i -> busyWork(work[i])).sum();
            BLACKHOLE = sum;
        });
        bench("Unbalanced (parallel)", 1, 2, () -> {
            double sum = IntStream.range(0, N).parallel().mapToDouble(i -> busyWork(work[i])).sum();
            BLACKHOLE = sum;
        });
        System.out.println();
    }

    // ------------------------------------------------------------
    // Example 13: Arrays.parallelSort vs Arrays.sort
    // ------------------------------------------------------------
    private static void arraysParallelSort() {
        System.out.println("[13] Arrays.parallelSort vs Arrays.sort");
        final int N = 400_000;
        int[] data = randomIntArray(N);

        bench("Arrays.sort (sequential)", 1, 1, () -> {
            int[] a = data.clone();
            Arrays.sort(a);
            BLACKHOLE = a[0];
        });
        bench("Arrays.parallelSort (parallel)", 1, 1, () -> {
            int[] a = data.clone();
            Arrays.parallelSort(a);
            BLACKHOLE = a[0];
        });
        System.out.println();
    }

    // ------------------------------------------------------------
    // Example 14: iterate vs range — iterate is poor for parallel
    // ------------------------------------------------------------
    private static void iterateVsRange() {
        System.out.println("[14] iterate vs range: iterate splits poorly in parallel");
        final int N = 1_000_000;

        bench("Stream.iterate (sequential)", 1, 1, () -> {
            long s = Stream.iterate(0, i -> i + 1).limit(N).mapToLong(Integer::longValue).sum();
            BLACKHOLE = s;
        });
        bench("Stream.iterate (parallel)", 1, 1, () -> {
            long s = Stream.iterate(0, i -> i + 1).limit(N).parallel().mapToLong(Integer::longValue).sum();
            BLACKHOLE = s;
        });
        bench("IntStream.range (parallel)", 1, 1, () -> {
            long s = IntStream.range(0, N).parallel().asLongStream().sum();
            BLACKHOLE = s;
        });
        System.out.println();
    }

    // ------------------------------------------------------------
    // Example 15: Custom ForkJoinPool for parallel streams
    // ------------------------------------------------------------
    private static void customForkJoinPoolExample() throws Exception {
        System.out.println("[15] Custom ForkJoinPool for parallel streams (isolating parallelism)");
        final int N = 1_000_000;

        // Default common pool
        bench("CommonPool sum of squares (par)", 1, 1, () -> {
            long s = IntStream.range(0, N).parallel().map(i -> i * i).asLongStream().sum();
            BLACKHOLE = s;
        });

        // Custom pool with limited parallelism
        ForkJoinPool pool = new ForkJoinPool(Math.max(1, Runtime.getRuntime().availableProcessors() / 2));
        long res = pool.submit(() ->
                IntStream.range(0, N).parallel().map(i -> i * i).asLongStream().sum()
        ).get();
        System.out.println("Custom pool parallelism: " + pool.getParallelism() + ", result: " + res);
        System.out.println();
        pool.shutdown();
    }

    // ------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------
    private static void bench(String label, int warmups, int runs, Runnable r) {
        for (int i = 0; i < warmups; i++) r.run();
        long total = 0L;
        for (int i = 0; i < runs; i++) {
            long t0 = System.nanoTime();
            r.run();
            long t1 = System.nanoTime();
            total += (t1 - t0);
        }
        double avgMs = total / 1_000_000.0 / Math.max(1, runs);
        System.out.printf("%-35s %8.3f ms%n", label, avgMs);
    }

    private static int[] randomIntArray(int size) {
        SplittableRandom rnd = new SplittableRandom(42);
        int[] a = new int[size];
        for (int i = 0; i < size; i++) a[i] = rnd.nextInt();
        return a;
    }

    private static boolean isPrime(int n) {
        if (n <= 1) return false;
        if ((n & 1) == 0) return n == 2;
        for (int i = 3; i * i <= n; i += 2) {
            if (n % i == 0) return false;
        }
        return true;
    }

    private static double busyWork(int units) {
        // Simple CPU-heavy loop to simulate work; higher units => more work.
        double r = 0.0;
        int reps = 40 + units;
        for (int i = 1; i <= reps; i++) {
            r += Math.sqrt(i + r * 1.000123);
        }
        return r;
    }
}