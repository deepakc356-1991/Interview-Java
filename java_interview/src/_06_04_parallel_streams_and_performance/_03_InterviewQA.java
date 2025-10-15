package _06_04_parallel_streams_and_performance;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.*;

/*
Interview Q&A: Parallel Streams & Performance (Basic → Advanced)

Q1. What is a parallel stream?
A: A Stream that splits work across multiple threads from the ForkJoin common pool.

Q2. How to create a parallel stream?
A: collection.parallelStream() or collection.stream().parallel().

Q3. When should you use parallel streams?
A: CPU-bound, stateless, associative operations, with sufficient data size and good splitability.

Q4. When should you NOT use parallel streams?
A: Small data, blocking I/O, shared mutable state, strong ordering constraints, in thread-starved servers.

Q5. What thread pool do parallel streams use?
A: The ForkJoin common pool. Parallelism typically equals availableProcessors() - 1.

Q6. Can you change the pool used by parallel streams?
A: Not directly. They always use the common pool. You can set system property before first use, or avoid parallel streams and use your own executors/ForkJoin tasks.

Q7. Are stream operations thread-safe?
A: Pipelines are thread-safe IF they avoid shared mutable state. Avoid side effects (e.g., adding to shared lists).

Q8. Does ordering affect performance?
A: Yes. Maintaining encounter order (e.g., forEachOrdered, findFirst, sorted) limits parallel speedups; unordered() can help.

Q9. Why is associativity important in parallel reductions?
A: Partial results are combined arbitrarily. Non-associative ops (e.g., subtraction) lead to incorrect results.

Q10. Does boxing/unboxing hurt performance?
A: Yes. Prefer primitive streams (IntStream/LongStream/DoubleStream) for numeric pipelines.

Q11. Do short-circuiting ops behave differently?
A: findAny/anyMatch may complete much faster in parallel; findFirst preserves order and may be slower.

Q12. What about Collectors and parallel?
A: Use thread-safe/concurrent collectors: groupingByConcurrent/toConcurrentMap for parallel-friendly collection.

Q13. How to detect if a stream is parallel?
A: stream.isParallel().

Q14. Do limit/skip interact poorly with parallelism?
A: Yes when ordered; they force global coordination. unordered() can mitigate in some cases.

Q15. Parallel streams and blocking tasks?
A: Blocking in common pool can starve other tasks (e.g., CompletableFutures). Avoid blocking or use ManagedBlocker/own executors.

Q16. Can parallel be slower than sequential?
A: Yes—overhead dominates on small data or cheap operations.

Q17. How to benchmark correctly?
A: Warm up JIT, avoid dead-code elimination, run multiple iterations. Microbenchmarks are sensitive; JMH is preferred.

Below, each demo method includes small code snippets and comments addressing these questions.
*/
public class _03_InterviewQA {

    // Blackholes to prevent dead-code elimination in naive benchmarks
    private static volatile double BH_DOUBLE;
    private static volatile long BH_LONG;
    private static volatile Object BH_OBJ;

    public static void main(String[] args) throws Exception {
        System.out.println("Parallel Streams & Performance - Interview Q&A Demo");
        System.out.println("Java: " + System.getProperty("java.version"));
        System.out.println("CPU cores: " + Runtime.getRuntime().availableProcessors());
        System.out.println("ForkJoin common parallelism: " + ForkJoinPool.getCommonPoolParallelism());
        System.out.println();

        demoBasicCreationAndIsParallel();              // Q1, Q2, Q13
        demoCPUHeavyOperationPerformance();            // Q3, Q16, Q17
        demoSmallCollectionOverhead();                 // Q4, Q16
        demoFindAnyVsFindFirst();                      // Q8, Q11
        demoUnorderedPerformanceHint();                // Q8
        demoSideEffectsPitfallAndFix();                // Q7
        demoReductionAssociativity();                  // Q9
        demoConcurrentCollectors();                    // Q12
        demoCommonPoolAndCustomPoolReality();          // Q5, Q6, Q15
        demoPrimitiveStreamsBenefit();                 // Q10
        demoLimitSkipPitfall();                        // Q14

        System.out.println("\nDone.");
    }

    // Q1, Q2, Q13
    private static void demoBasicCreationAndIsParallel() {
        System.out.println("[Demo] Basic creation and isParallel()");
        List<Integer> list = IntStream.range(0, 10).boxed().toList();

        Stream<Integer> s1 = list.parallelStream();
        System.out.println("list.parallelStream().isParallel() = " + s1.isParallel());

        Stream<Integer> s2 = list.stream().parallel();
        System.out.println("list.stream().parallel().isParallel() = " + s2.isParallel());

        Stream<Integer> s3 = s2.sequential();
        System.out.println("s2.sequential().isParallel() = " + s3.isParallel());

        System.out.println();
    }

    // Q3, Q16, Q17
    private static void demoCPUHeavyOperationPerformance() {
        System.out.println("[Demo] CPU-bound workload: sequential vs parallel (expect speedup)");
        // Generate data once
        final double[] data = new Random(0).doubles(200_000, 0.0, 1.0).toArray();

        Runnable seq = () -> {
            double sum = Arrays.stream(data)
                    .map(_03_InterviewQA::heavy)  // heavy CPU-bound
                    .sum();
            BH_DOUBLE = sum;
        };
        Runnable par = () -> {
            double sum = Arrays.stream(data)
                    .parallel()
                    .map(_03_InterviewQA::heavy)
                    .sum();
            BH_DOUBLE = sum;
        };

        long tSeq = benchmark("CPU-bound sequential", 2, 3, seq);
        long tPar = benchmark("CPU-bound parallel", 2, 3, par);
        System.out.println(String.format("Speedup (seq/par): %.2fx", tSeq / (double) Math.max(1, tPar)));
        System.out.println();
    }

    // Q4, Q16
    private static void demoSmallCollectionOverhead() {
        System.out.println("[Demo] Small/cheap workload: sequential can beat parallel");
        final int[] arr = IntStream.range(0, 50_000).toArray();

        Runnable cheapSeq = () -> {
            long s = Arrays.stream(arr).map(x -> x + 1).asLongStream().sum();
            BH_LONG = s;
        };
        Runnable cheapPar = () -> {
            long s = Arrays.stream(arr).parallel().map(x -> x + 1).asLongStream().sum();
            BH_LONG = s;
        };

        long tSeq = benchmark("Cheap sequential", 2, 5, cheapSeq);
        long tPar = benchmark("Cheap parallel", 2, 5, cheapPar);
        System.out.println(String.format("Overhead ratio (par/seq): %.2fx", tPar / (double) Math.max(1, tSeq)));
        System.out.println();
    }

    // Q8, Q11
    private static void demoFindAnyVsFindFirst() {
        System.out.println("[Demo] findAny vs findFirst in parallel");
        int any = IntStream.range(0, 1_000_000).parallel().boxed().findAny().orElse(-1);
        int first = IntStream.range(0, 1_000_000).parallel().boxed().findFirst().orElse(-1);

        // findAny may be non-deterministic; findFirst preserves order.
        System.out.println("findAny (parallel, unordered): " + any);
        System.out.println("findFirst (parallel, ordered): " + first + " (preserves order)");
        System.out.println();
    }

    // Q8
    private static void demoUnorderedPerformanceHint() {
        System.out.println("[Demo] unordered() can relax ordering constraints");
        // For demonstration only: using forEach vs forEachOrdered with small data
        List<Integer> list = IntStream.rangeClosed(1, 10).boxed().toList();

        System.out.print("forEach (parallel, order not guaranteed): ");
        list.parallelStream().forEach(x -> System.out.print(x + " "));
        System.out.println();

        System.out.print("forEachOrdered (parallel, preserves order): ");
        list.parallelStream().forEachOrdered(x -> System.out.print(x + " "));
        System.out.println("\n");
    }

    // Q7
    private static void demoSideEffectsPitfallAndFix() {
        System.out.println("[Demo] Side-effects pitfall and correct collection");
        List<Integer> numbers = IntStream.range(0, 10_000).boxed().toList();

        // Wrong: modifying shared mutable state from parallel stream
        List<Integer> wrong = new ArrayList<>();
        try {
            numbers.parallelStream().forEach(wrong::add); // Data race; may lose elements or throw
        } catch (Exception e) {
            System.out.println("Caught exception (as expected with side-effects): " + e);
        }
        System.out.println("Wrong size (may be < 10000): " + wrong.size());

        // Correct: use collectors (framework handles thread-safety via per-thread containers + combiner)
        List<Integer> correct = numbers.parallelStream().collect(Collectors.toList());
        System.out.println("Correct size: " + correct.size());

        // Also works: thread-safe container, but slower
        List<Integer> safe = Collections.synchronizedList(new ArrayList<>());
        numbers.parallelStream().forEach(safe::add);
        System.out.println("Safe size (synchronizedList): " + safe.size());
        System.out.println();
    }

    // Q9
    private static void demoReductionAssociativity() {
        System.out.println("[Demo] Reduction identity and associativity");
        // Wrong identity in parallel: identity is applied per subtask; non-neutral identities break results
        int wrong = Stream.of(1, 2, 3, 4).parallel().reduce(10, Integer::sum);
        int correct = Stream.of(1, 2, 3, 4).parallel().reduce(0, Integer::sum);
        System.out.println("Wrong reduce with identity=10 (parallel): " + wrong + "  (WRONG)");
        System.out.println("Correct reduce with identity=0: " + correct);

        // Non-associative operation example: subtraction
        int subtractSeq = IntStream.rangeClosed(1, 5).reduce(0, (a, b) -> a - b);        // Deterministic
        int subtractPar = IntStream.rangeClosed(1, 5).parallel().reduce(0, (a, b) -> a - b); // Nondeterministic
        System.out.println("Subtraction (sequential): " + subtractSeq);
        System.out.println("Subtraction (parallel, order varies): " + subtractPar + "  (NOND-KNOWN)");
        System.out.println();
    }

    // Q12
    private static void demoConcurrentCollectors() {
        System.out.println("[Demo] Concurrent collectors in parallel");

        List<String> words = Arrays.asList("a", "bb", "ccc", "dd", "e", "ff", "ggg", "hh", "bb");
        try {
            // Will throw due to duplicate keys (length 2 -> "bb" and "dd"/"ff"/"hh")
            Map<Integer, String> m = words.parallelStream()
                    .collect(Collectors.toMap(String::length, Function.identity()));
            BH_OBJ = m;
        } catch (IllegalStateException dup) {
            System.out.println("toMap duplicate-key error (expected): " + dup.getMessage());
        }

        // Resolve duplicates with merge function
        Map<Integer, String> toMapMerged = words.parallelStream()
                .collect(Collectors.toMap(String::length, Function.identity(), (a, b) -> a));
        System.out.println("toMap with merge OK, keys=" + toMapMerged.keySet());

        // groupingBy vs groupingByConcurrent
        Map<Integer, List<String>> gb = words.parallelStream()
                .collect(Collectors.groupingBy(String::length));
        Map<Integer, List<String>> gbc = words.parallelStream()
                .collect(Collectors.groupingByConcurrent(String::length));
        System.out.println("groupingBy keys: " + gb.keySet());
        System.out.println("groupingByConcurrent keys: " + gbc.keySet());

        // Concurrent map collector
        Map<Integer, String> conc = words.parallelStream()
                .collect(Collectors.toConcurrentMap(String::length, Function.identity(), (a, b) -> a));
        System.out.println("toConcurrentMap keys: " + conc.keySet());
        System.out.println();
    }

    // Q5, Q6, Q15
    private static void demoCommonPoolAndCustomPoolReality() throws Exception {
        System.out.println("[Demo] Common pool vs custom ForkJoinPool (parallel stream uses common pool)");
        ForkJoinPool custom = new ForkJoinPool(2);
        Set<String> threadNames = Collections.synchronizedSet(new HashSet<>());

        // Even when run inside a custom pool, parallel streams use the common pool
        custom.submit(() -> {
            IntStream.range(0, 1000).parallel().forEach(i -> threadNames.add(Thread.currentThread().getName()));
        }).get();

        System.out.println("Threads observed in parallel stream: " + threadNames);
        boolean usesCommonPool = threadNames.stream().anyMatch(n -> n.contains("ForkJoinPool.commonPool"));
        System.out.println("Uses common pool? " + usesCommonPool + " (expected: true)");

        // Tip: Avoid blocking in common pool; consider ManagedBlocker or different executors for I/O.
        System.out.println();
    }

    // Q10
    private static void demoPrimitiveStreamsBenefit() {
        System.out.println("[Demo] Primitive streams avoid boxing overhead");
        List<Integer> list = IntStream.range(0, 1_000_00).boxed().toList(); // 100k

        Runnable boxedSum = () -> {
            long s = list.parallelStream().map(x -> x + 1).mapToLong(Integer::longValue).sum();
            BH_LONG = s;
        };
        Runnable primitiveSum = () -> {
            long s = IntStream.range(0, 100_000).parallel().map(x -> x + 1).asLongStream().sum();
            BH_LONG = s;
        };

        long tBoxed = benchmark("Boxed parallel sum", 2, 5, boxedSum);
        long tPrim = benchmark("Primitive parallel sum", 2, 5, primitiveSum);
        System.out.println(String.format("Primitive/boxed time ratio: %.2fx", tPrim / (double) Math.max(1, tBoxed)));
        System.out.println();
    }

    // Q14
    private static void demoLimitSkipPitfall() {
        System.out.println("[Demo] limit/skip with ordering can hinder parallelism");
        // Ordered pipeline forces coordination; unordered hint can help in some cases
        int N = 2_000_000;

        Runnable ordered = () -> {
            long sum = IntStream.range(0, N).parallel().skip(N - 10).limit(10).asLongStream().sum();
            BH_LONG = sum;
        };
        Runnable unordered = () -> {
            long sum = IntStream.range(0, N).parallel().unordered().skip(N - 10).limit(10).asLongStream().sum();
            BH_LONG = sum;
        };

        long tOrdered = benchmark("Ordered skip/limit", 1, 3, ordered);
        long tUnordered = benchmark("unordered() + skip/limit", 1, 3, unordered);
        System.out.println(String.format("unordered/ordered time ratio: %.2fx", tUnordered / (double) Math.max(1, tOrdered)));
        System.out.println();
    }

    // --------- Utilities ---------

    private static long benchmark(String label, int warmups, int runs, Runnable r) {
        for (int i = 0; i < warmups; i++) r.run();
        long best = Long.MAX_VALUE;
        for (int i = 0; i < runs; i++) {
            long t = timeMillis(r);
            best = Math.min(best, t);
        }
        System.out.println(String.format("%s: best-of-%d = %d ms", label, runs, best));
        return best;
    }

    private static long timeMillis(Runnable r) {
        long t0 = System.nanoTime();
        r.run();
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
    }

    private static double heavy(double x) {
        // CPU-bound math to simulate work
        double y = x;
        for (int i = 0; i < 12; i++) {
            y = Math.sin(y) * Math.cos(y) + Math.sqrt(Math.abs(y) + 1.0);
        }
        return y;
    }
}