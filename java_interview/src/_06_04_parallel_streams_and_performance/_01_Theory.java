package _06_04_parallel_streams_and_performance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LongSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.StringJoiner;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * Parallel Streams & Performance — Theory, Pitfalls, and Best Practices
 *
 * Concepts (quick reference):
 * - What: Stream pipelines can execute sequentially or in parallel (common ForkJoinPool).
 * - When parallel helps:
 *   - Pure, stateless, CPU-bound work per element (map/filter/reduce) with enough work per item.
 *   - Sources that split well (arrays, ArrayList, ranges, primitive streams).
 *   - Large data sets relative to parallel overhead.
 * - When it hurts:
 *   - I/O-bound or blocking operations (disk/network/sleep).
 *   - Shared mutable state, synchronization, locks, contention.
 *   - Small streams, cheap operations, poor splitting sources (LinkedList, iterators).
 *   - Stateful/ordering-heavy ops (sorted, distinct, limit on ORDERED streams).
 *   - Boxing/unboxing overhead (prefer IntStream/LongStream/DoubleStream).
 * - Correctness rules:
 *   - Non-interference (don’t modify the source while streaming).
 *   - Stateless lambda bodies (no mutable shared state).
 *   - Associative reductions/combiners (parallel reduce/collect must be associative).
 * - Ordering and short-circuit:
 *   - forEachOrdered preserves order, may reduce parallelism.
 *   - unordered() can unlock performance when order is irrelevant.
 *   - findAny is cheaper than findFirst on parallel pipelines.
 * - ForkJoin common pool:
 *   - Default parallelism ~ availableProcessors - 1.
 *   - Shared across the JVM; blocking harms others; use ManagedBlocker or different concurrency tools for heavy blocking.
 *   - You can confine a parallel stream to a custom ForkJoinPool by submitting the pipeline from that pool.
 * - Collectors:
 *   - toList/toSet are fine with parallel (they merge per-thread containers).
 *   - Grouping/partitioning: use groupingByConcurrent/toConcurrentMap with unordered if you need throughput and don’t need ordering.
 * - Spliterators and sources:
 *   - Arrays/ArrayList/ranges have SIZED/SUBSIZED and split efficiently.
 *   - LinkedList/IO streams split poorly; parallelism may underperform sequential.
 * - Measuring:
 *   - Microbenchmarks are tricky; prefer JMH for rigor. Warm up before timing.
 * - Alternatives:
 *   - For IO-bound tasks, consider CompletableFuture, virtual threads, or structured concurrency instead of parallel streams.
 */
public class _01_Theory {

    // Tweak these to see timing differences on your machine
    private static final int CORES = Runtime.getRuntime().availableProcessors();
    private static final int N = 1_000_000;         // dataset for CPU-bound demos
    private static final int SMALL_N = 200_000;     // dataset for collection demos

    public static void main(String[] args) throws Exception {
        System.out.println("Cores: " + CORES);
        System.out.println("Note: Times are illustrative only; use JMH for rigorous benchmarking.\n");

        warmUpJIT(); // basic warm-up

        demoSequentialVsParallelMapReduce();
        demoWrongParallelReduction();
        demoPrimitiveStreamsVsBoxed();
        demoOrderingAndUnorderedShortCircuit();
        demoSideEffectsVsCollect();
        demoBlockingWorkWarning();
        demoCustomPoolConfinement();
        demoArraysParallelSort();

        System.out.println("\nDone.");
    }

    // -------------------------
    // 1) CPU-bound map-reduce
    // -------------------------
    private static void demoSequentialVsParallelMapReduce() {
        System.out.println("1) CPU-bound map-reduce: sequential vs parallel");

        // Ensure both produce the same numeric result
        long expected = LongStream.range(0, 1000)
                .map(_01_Theory::cpu)
                .sum();

        // Sequential
        long seqTimeMs;
        long seqSum = timedMs("  sequential", () ->
                LongStream.range(0, N)
                        .map(_01_Theory::cpu)
                        .sum()
        );

        // Parallel
        long parTimeMs;
        long parSum = timedMs("  parallel  ", () ->
                LongStream.range(0, N)
                        .parallel()
                        .map(_01_Theory::cpu)
                        .sum()
        );

        // Validate
        if (seqSum != parSum) {
            throw new AssertionError("Mismatch: seqSum=" + seqSum + " parSum=" + parSum);
        }

        System.out.println();
    }

    // --------------------------------------
    // 2) Wrong reduction (non-associative)
    // --------------------------------------
    private static void demoWrongParallelReduction() {
        System.out.println("2) Wrong reduction example (non-associative accumulator)");

        int upTo = 50_000;

        int seq = IntStream.rangeClosed(1, upTo)
                .reduce(0, (a, b) -> a - b); // defined but odd
        int par = IntStream.rangeClosed(1, upTo)
                .parallel()
                .reduce(0, (a, b) -> a - b); // WRONG in parallel due to non-associativity

        System.out.println("  sequential (a-b): " + seq);
        System.out.println("  parallel   (a-b): " + par + "  <-- incorrect semantics for parallel reduce");
        System.out.println("  Fix: Use associative ops (sum, min, max) or proper Collector.\n");
    }

    // -------------------------------------------------------
    // 3) Primitive streams vs boxed and common operations
    // -------------------------------------------------------
    private static void demoPrimitiveStreamsVsBoxed() {
        System.out.println("3) Primitive streams vs boxed");

        // Primitive pipeline: no boxing
        long s1 = timedMs("  LongStream.range sum (primitive)", () ->
                LongStream.range(0, N).sum()
        );

        // Boxed list construction (one-time cost, shows memory overhead)
        List<Long> boxed = LongStream.range(0, SMALL_N)
                .boxed()
                .collect(Collectors.toList());

        // Boxed reduction (autoboxing and object traffic)
        long s2 = timedMs("  boxed stream reduce(Long::sum)   ", () ->
                boxed.stream().reduce(0L, Long::sum)
        );

        // Map to primitive from boxed
        long s3 = timedMs("  boxed -> mapToLong().sum()       ", () ->
                boxed.stream().mapToLong(Long::longValue).sum()
        );

        // Parallel boxed reduce (still boxed, usually not ideal)
        long s4 = timedMs("  boxed parallel reduce(Long::sum) ", () ->
                boxed.parallelStream().reduce(0L, Long::sum)
        );

        System.out.println("  Tip: Prefer IntStream/LongStream/DoubleStream to avoid boxing overhead.\n");
    }

    // --------------------------------------------
    // 4) Ordering and short-circuit opportunities
    // --------------------------------------------
    private static void demoOrderingAndUnorderedShortCircuit() {
        System.out.println("4) Ordering vs unordered and short-circuiting");

        List<Integer> data = IntStream.range(0, 16).boxed().collect(Collectors.toList());

        System.out.print("  forEachOrdered: ");
        data.parallelStream().forEachOrdered(n -> System.out.print(n + " "));
        System.out.println();

        System.out.print("  forEach       : ");
        data.parallelStream().forEach(n -> System.out.print(n + " "));
        System.out.println();

        // findAny can return any matching element faster than findFirst on parallel pipelines
        int anyMultipleOf7 = IntStream.range(0, 10_000)
                .parallel()
                .unordered() // drop ordering constraints to enable faster completion
                .filter(x -> x % 7 == 0)
                .findAny()
                .orElse(-1);

        System.out.println("  findAny (unordered, %7==0): " + anyMultipleOf7 + "\n");
    }

    // -------------------------------------------------------
    // 5) Side-effects vs proper collect/combining
    // -------------------------------------------------------
    private static void demoSideEffectsVsCollect() {
        System.out.println("5) Side-effects (forEach) vs proper collect");

        // BAD (contention & ordering): even with a synchronized list, performance suffers.
        List<Long> syncList = Collections.synchronizedList(new ArrayList<>());
        timedMs("  parallel forEach(add) into shared list", () -> {
            syncList.clear();
            LongStream.range(0, SMALL_N).parallel().forEach(syncList::add);
            return syncList.size();
        });

        // GOOD: use collect/toList (parallel collects use per-thread containers, then merge)
        timedMs("  parallel collect(toList)", () ->
                LongStream.range(0, SMALL_N).parallel().boxed().collect(Collectors.toList()).size()
        );

        System.out.println("  Prefer collect(...) over mutating shared state inside forEach.\n");
    }

    // -------------------------------------------------------
    // 6) Blocking work warning (I/O-like simulation)
    // -------------------------------------------------------
    private static void demoBlockingWorkWarning() {
        System.out.println("6) Blocking work simulation (sleep) — parallel streams may not be ideal");

        int tasks = 80;
        int sleepMs = 5;

        // Sequential
        long seqMs = elapsedMs(() -> {
            IntStream.range(0, tasks).forEach(i -> sleep(sleepMs));
        });
        System.out.println("  sequential sleep: ~" + seqMs + " ms");

        // Parallel (uses common ForkJoinPool; blocking harms pool throughput)
        long parMs = elapsedMs(() -> {
            IntStream.range(0, tasks).parallel().forEach(i -> sleep(sleepMs));
        });
        System.out.println("  parallel   sleep: ~" + parMs + " ms");
        System.out.println("  For I/O: prefer CompletableFuture, virtual threads, or async APIs.\n");
    }

    // -------------------------------------------------------
    // 7) Confining a parallel stream to a custom pool
    // -------------------------------------------------------
    private static void demoCustomPoolConfinement() throws Exception {
        System.out.println("7) Custom ForkJoinPool confinement (advanced)");

        // Note: Submitting the pipeline from a ForkJoinPool confines work to that pool.
        // This is useful to isolate workloads or cap parallelism.
        ForkJoinPool pool = new ForkJoinPool(Math.max(2, CORES / 2));
        try {
            long seqMs = elapsedMs(() ->
                    LongStream.range(0, N).map(_01_Theory::cpu).sum()
            );

            long poolMs = elapsedMs(() ->
                    pool.submit(() -> LongStream.range(0, N).parallel().map(_01_Theory::cpu).sum()).join()
            );

            System.out.println("  sequential: ~" + seqMs + " ms");
            System.out.println("  custom-pool parallel (size=" + pool.getParallelism() + "): ~" + poolMs + " ms");
            System.out.println("  Tip: You can also tune the common pool via -Djava.util.concurrent.ForkJoinPool.common.parallelism=N (set at JVM start).\n");
        } finally {
            pool.shutdown();
            pool.awaitTermination(1, TimeUnit.SECONDS);
        }
    }

    // -------------------------------------------------------
    // 8) Built-in parallel algorithm example: Arrays.parallelSort
    // -------------------------------------------------------
    private static void demoArraysParallelSort() {
        System.out.println("8) Arrays.parallelSort example");

        int[] a1 = randomArray(1_000_000);
        int[] a2 = Arrays.copyOf(a1, a1.length);

        long t1 = elapsedMs(() -> Arrays.sort(a1));            // sequential
        long t2 = elapsedMs(() -> Arrays.parallelSort(a2));    // parallel

        System.out.println("  Arrays.sort         : ~" + t1 + " ms");
        System.out.println("  Arrays.parallelSort : ~" + t2 + " ms\n");
    }

    // =========================================================================================
    // Helper: a small CPU-bound function to simulate meaningful per-element work (pure function)
    // =========================================================================================
    private static long cpu(long x) {
        long r = x + 0x9E3779B97F4A7C15L; // mix a bit
        for (int i = 0; i < 50; i++) {
            r ^= (r << 13);
            r *= 0xBF58476D1CE4E5B9L;
            r ^= (r >>> 7);
            r *= 0x94D049BB133111EBL;
        }
        return r;
    }

    // =========================================================================================
    // Misc helpers
    // =========================================================================================

    private static void warmUpJIT() {
        // Warm up to reduce first-time JIT effects in demos
        LongStream.range(0, 100_000).map(_01_Theory::cpu).sum();
        IntStream.range(0, 100_000).parallel().map(i -> i * i).sum();
    }

    private static long timedMs(String label, LongSupplier action) {
        long t0 = System.nanoTime();
        long result = action.getAsLong();
        long t1 = System.nanoTime();
        long ms = TimeUnit.NANOSECONDS.toMillis(t1 - t0);
        System.out.printf("  %-35s ~%d ms (result hash=%d)%n", label, ms, (result ^ (result >>> 32)));
        return result;
    }

    private static long elapsedMs(Runnable r) {
        long t0 = System.nanoTime();
        r.run();
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
    }

    private static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private static int[] randomArray(int n) {
        int[] a = new int[n];
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (int i = 0; i < n; i++) a[i] = rnd.nextInt();
        return a;
    }

    // -----------------------------------------------------------------------------------------
    // Additional reference snippets (not invoked by main)
    // -----------------------------------------------------------------------------------------

    // Example: grouping with concurrent collector (use when ordering not required)
    private static Map<Integer, Long> groupingByConcurrentExample(Stream<String> words) {
        // Count words by length concurrently; unordered is best for throughput
        return words.parallel()
                .unordered()
                .collect(Collectors.groupingByConcurrent(String::length, Collectors.counting()));
    }

    // Example: safe parallel collection into a mutable container using a proper Collector
    private static List<Long> collectToListSafelyParallel(long n) {
        return LongStream.range(0, n).parallel().boxed().collect(Collectors.toList());
    }

    // Example: avoid shared mutable state in forEach
    private static List<Long> avoidSharedState(long n) {
        // BAD (don’t do this):
        // List<Long> shared = new ArrayList<>();
        // LongStream.range(0, n).parallel().forEach(shared::add);

        // GOOD:
        return LongStream.range(0, n).parallel().boxed().collect(Collectors.toList());
    }
}