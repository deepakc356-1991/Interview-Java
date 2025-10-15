package _08_04_futures_and_completablefuture;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Educational interview Q&A code for Futures and CompletableFuture: basic -> intermediate -> advanced.
 * Run main() to see demonstrations and read the Q&A comments above each section.
 */
public class _03_InterviewQA {

    // Utility logging and helpers
    static void log(String msg) {
        System.out.printf("[%s] %s%n", Thread.currentThread().getName(), msg);
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    static String blockingCall(long ms) {
        sleep(ms);
        return "blocking:" + ms + "ms";
    }

    static ExecutorService newNamedFixedPool(String name, int size) {
        AtomicInteger n = new AtomicInteger(1);
        return Executors.newFixedThreadPool(size, r -> {
            Thread t = new Thread(r, name + "-" + n.getAndIncrement());
            t.setDaemon(true);
            return t;
        });
    }

    // Custom pool for blocking/IO-like tasks (avoid blocking the common ForkJoinPool)
    static final ExecutorService IO_POOL = newNamedFixedPool("io-pool", 4);

    public static void main(String[] args) {
        log("Futures & CompletableFuture Interview Q&A Demo - start");
        try {
            q01_future_basics();
            q02_future_limitations_and_bridge_to_completablefuture();
            q03_completablefuture_basics_runAsync_vs_supplyAsync();
            q04_thenApply_thenAccept_thenRun();
            q05_thenCompose_vs_thenCombine();
            q06_applyToEither_acceptEither_anyOf();
            q07_allOf_and_collect_results();
            q08_exception_handling_exceptionally_handle_whenComplete();
            q09_get_vs_join_checked_vs_unchecked();
            q10_timeouts_orTimeout_completeOnTimeout_get_with_timeout();
            q11_cancellation_and_propagation();
            q12_manual_completion_promise_complete_completeExceptionally();
            q13_obtrudeValue_obtrudeException_advanced();
            q14_async_vs_sync_stages_default_vs_custom_executor();
            q15_blocking_work_use_custom_executor_not_common_pool();
            q16_parallel_aggregation_pattern();
            q17_completedFuture_seeding_pipelines();
        } finally {
            IO_POOL.shutdownNow();
        }
        log("Futures & CompletableFuture Interview Q&A Demo - end");
    }

    /*
     Q1: What is a Future? How do you use it?
     A: Future represents the result of an asynchronous computation. You submit a Callable to an ExecutorService and get a Future to retrieve the result later, typically by blocking with get().
     */
    static void q01_future_basics() {
        log("Q1: Future basics");
        ExecutorService pool = newNamedFixedPool("basic-pool", 1);
        try {
            Future<Integer> future = pool.submit(() -> {
                log("Callable running");
                sleep(120);
                return 42;
            });
            log("future.isDone() = " + future.isDone());
            try {
                Integer result = future.get(); // blocks
                log("future.get() = " + result);
            } catch (InterruptedException | ExecutionException e) {
                log("get failed: " + e);
            }
        } finally {
            pool.shutdown();
        }
    }

    /*
     Q2: What are Future limitations vs CompletableFuture? How to bridge old Future?
     A:
      - Future lacks callbacks, composition, and non-blocking completion handling.
      - CompletableFuture (implements Future and CompletionStage) supports chaining, callbacks, manual completion, and more.
      - Bridge: wrap a Future with CompletableFuture.supplyAsync(...) on a dedicated executor to avoid blocking critical pools.
     */
    static void q02_future_limitations_and_bridge_to_completablefuture() {
        log("Q2: Future limitations and bridging to CompletableFuture");
        Future<String> legacy = IO_POOL.submit(() -> {
            sleep(100);
            return "legacy-value";
        });

        CompletableFuture<String> bridged = toCompletableFuture(legacy, IO_POOL);
        bridged.thenAccept(v -> log("bridged future result: " + v)).join();
    }

    static <T> CompletableFuture<T> toCompletableFuture(Future<T> f, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return f.get(); // blocks on a safe executor
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new CompletionException(e);
            } catch (ExecutionException e) {
                throw new CompletionException(e.getCause());
            }
        }, executor);
    }

    /*
     Q3: runAsync vs supplyAsync?
     A: runAsync runs a task that returns no result (Void). supplyAsync runs a task that returns a value.
     */
    static void q03_completablefuture_basics_runAsync_vs_supplyAsync() {
        log("Q3: runAsync vs supplyAsync");
        CompletableFuture<Void> fireAndForget = CompletableFuture.runAsync(() -> {
            log("runAsync side-effect");
            sleep(50);
        });

        CompletableFuture<Integer> valueFuture = CompletableFuture.supplyAsync(() -> {
            log("supplyAsync compute value");
            sleep(70);
            return 7;
        });

        fireAndForget.join();
        int v = valueFuture.thenApply(x -> x * 2).join();
        log("valueFuture thenApply result = " + v);
    }

    /*
     Q4: thenApply vs thenAccept vs thenRun?
     A:
      - thenApply maps the result and returns a new value.
      - thenAccept consumes the result (no new value).
      - thenRun ignores the result and just runs a side-effect.
     */
    static void q04_thenApply_thenAccept_thenRun() {
        log("Q4: thenApply vs thenAccept vs thenRun");
        CompletableFuture<String> source = CompletableFuture.supplyAsync(() -> "A");

        CompletableFuture<String> mapped = source.thenApply(s -> s + "+mapped");
        CompletableFuture<Void> consumed = source.thenAccept(s -> log("consumed: " + s));
        CompletableFuture<Void> sideRun = source.thenRun(() -> log("thenRun: no input"));

        CompletableFuture.allOf(mapped, consumed, sideRun).join();
        log("mapped result = " + mapped.join());
    }

    /*
     Q5: thenCompose vs thenCombine?
     A:
      - thenCompose for dependent async steps (flatMap).
      - thenCombine for independent async steps that produce values to merge.
     */
    static void q05_thenCompose_vs_thenCombine() {
        log("Q5: thenCompose vs thenCombine");
        CompletableFuture<String> userId = CompletableFuture.supplyAsync(() -> {
            sleep(40);
            return "user-42";
        });

        CompletableFuture<String> composed = userId.thenCompose(id ->
            CompletableFuture.supplyAsync(() -> {
                sleep(60);
                return "profile(" + id + ")";
            })
        );

        CompletableFuture<String> permissions = CompletableFuture.supplyAsync(() -> {
            sleep(50);
            return "perm(admin)";
        });
        CompletableFuture<String> combined = userId.thenCombine(permissions, (id, perm) -> id + "+" + perm);

        log("composed = " + composed.join());
        log("combined = " + combined.join());
    }

    /*
     Q6: Racing: applyToEither/acceptEither/anyOf?
     A:
      - applyToEither gets the value of the first completed and maps it.
      - acceptEither consumes the first result.
      - anyOf waits for any to finish (Object-typed).
     */
    static void q06_applyToEither_acceptEither_anyOf() {
        log("Q6: applyToEither/acceptEither/anyOf");
        CompletableFuture<String> slow = CompletableFuture.supplyAsync(() -> { sleep(120); return "slow"; });
        CompletableFuture<String> fast = CompletableFuture.supplyAsync(() -> { sleep(30); return "fast"; });

        String winner = slow.applyToEither(fast, s -> "winner:" + s).join();
        log(winner);

        CompletableFuture.anyOf(
            CompletableFuture.supplyAsync(() -> { sleep(20); return "any-1"; }),
            CompletableFuture.supplyAsync(() -> { sleep(50); return "any-2"; })
        ).thenAccept(o -> log("anyOf first: " + o)).join();
    }

    /*
     Q7: allOf and collecting typed results?
     A: allOf returns CompletableFuture<Void>. Collect results by joining each subfuture after allOf completes.
     */
    static void q07_allOf_and_collect_results() {
        log("Q7: allOf and collect results");
        List<CompletableFuture<Integer>> parts = IntStream.range(0, 5)
            .mapToObj(i -> CompletableFuture.supplyAsync(() -> { sleep(20 + i * 10); return i * i; }))
            .collect(Collectors.toList());

        CompletableFuture<List<Integer>> all =
            CompletableFuture.allOf(parts.toArray(new CompletableFuture[0]))
                .thenApply(v -> parts.stream().map(CompletableFuture::join).collect(Collectors.toList()));

        log("squares = " + all.join());
    }

    /*
     Q8: Exception handling: exceptionally, handle, whenComplete?
     A:
      - exceptionally transforms only on failure providing a fallback value.
      - handle receives (value, throwable) for both success/failure and returns a replacement value.
      - whenComplete observes (value, throwable) but does not change the outcome.
     */
    static void q08_exception_handling_exceptionally_handle_whenComplete() {
        log("Q8: exception handling");
        // exceptionally
        String v1 = CompletableFuture.supplyAsync(() -> { throw new RuntimeException("boom"); })
            .exceptionally(e -> {
                log("exceptionally saw: " + e.getClass().getSimpleName());
                return "fallback";
            }).join();
        log("exceptionally result = " + v1);

        // handle
        String v2 = CompletableFuture.supplyAsync(() -> { sleep(10); return "ok"; })
            .handle((val, ex) -> ex != null ? "fallback2" : val + "-handled")
            .join();
        log("handle result = " + v2);

        // whenComplete
        try {
            CompletableFuture.supplyAsync(() -> { throw new IllegalStateException("oops"); })
                .whenComplete((val, ex) -> log("whenComplete val=" + val + " ex=" + ex))
                .join(); // will throw CompletionException
        } catch (CompletionException ce) {
            log("whenComplete chain ultimately failed: cause=" + ce.getCause());
        }
    }

    /*
     Q9: get() vs join()?
     A:
      - get() throws checked InterruptedException/ExecutionException.
      - join() throws unchecked CompletionException (wraps cause).
     */
    static void q09_get_vs_join_checked_vs_unchecked() {
        log("Q9: get vs join");
        CompletableFuture<Integer> failing = CompletableFuture.supplyAsync(() -> { throw new IllegalArgumentException("bad"); });
        try {
            failing.get();
        } catch (Exception e) {
            log("get threw: " + e.getClass().getSimpleName() + ", cause=" + e.getCause());
        }
        try {
            failing.join();
        } catch (CompletionException e) {
            log("join threw: " + e.getClass().getSimpleName() + ", cause=" + e.getCause());
        }
    }

    /*
     Q10: Timeouts: orTimeout, completeOnTimeout, get(timeout)?
     A:
      - orTimeout completes exceptionally with TimeoutException after duration (Java 9+).
      - completeOnTimeout completes normally with fallback after duration (Java 9+).
      - get(timeout, unit) blocks with timeout.
     */
    static void q10_timeouts_orTimeout_completeOnTimeout_get_with_timeout() {
        log("Q10: timeouts");
        String r1 = CompletableFuture.supplyAsync(() -> { sleep(200); return "OK"; })
            .orTimeout(50, TimeUnit.MILLISECONDS)
            .exceptionally(e -> "timeout-ex")
            .join();
        log("orTimeout result = " + r1);

        String r2 = CompletableFuture.supplyAsync(() -> { sleep(200); return "OK"; })
            .completeOnTimeout("fallback", 60, TimeUnit.MILLISECONDS)
            .join();
        log("completeOnTimeout result = " + r2);

        CompletableFuture<String> cf = CompletableFuture.supplyAsync(() -> { sleep(120); return "late"; });
        try {
            String got = cf.get(30, TimeUnit.MILLISECONDS);
            log("get(timeout) = " + got);
        } catch (TimeoutException te) {
            log("get(timeout) timed out");
        } catch (Exception e) {
            log("get(timeout) failed: " + e);
        }
    }

    /*
     Q11: Cancellation: cancel(true)? How does it propagate?
     A:
      - CompletableFuture.cancel(true) marks it cancelled (exceptional completion with CancellationException).
      - Dependent stages usually see the cancellation as an exceptional completion and won't execute mapping functions.
     */
    static void q11_cancellation_and_propagation() {
        log("Q11: cancellation");
        CompletableFuture<String> cancelMe = CompletableFuture.supplyAsync(() -> { sleep(200); return "done"; });
        CompletableFuture<Void> observer = cancelMe.whenComplete((v, e) -> log("observer: v=" + v + ", e=" + e));
        sleep(40);
        boolean cancelled = cancelMe.cancel(true);
        log("cancelled = " + cancelled);
        observer.join();

        CompletableFuture<Integer> p = new CompletableFuture<>();
        CompletableFuture<Integer> next = p.thenApply(x -> x + 1)
            .whenComplete((v, e) -> log("next sees: v=" + v + ", e=" + e));
        p.cancel(true);
        try { next.join(); } catch (CompletionException ce) { log("downstream cancelled too: " + ce.getCause()); }
    }

    /*
     Q12: Manual completion (promise): complete(), completeExceptionally()
     A: CompletableFuture also acts like a Promise; producers can complete it externally.
     */
    static void q12_manual_completion_promise_complete_completeExceptionally() {
        log("Q12: manual completion / promise");
        CompletableFuture<String> promise = new CompletableFuture<>();
        CompletableFuture<Void> consumer = promise.thenAccept(v -> log("got: " + v));
        new Thread(() -> { sleep(50); promise.complete("value-from-producer"); }, "producer-thread").start();
        consumer.join();

        CompletableFuture<String> p2 = new CompletableFuture<>();
        p2.completeExceptionally(new IllegalArgumentException("bad-input"));
        String fallback = p2.exceptionally(e -> {
            log("saw exception: " + e.getClass().getSimpleName());
            return "fallback";
        }).join();
        log("fallback after exceptional completion = " + fallback);
    }

    /*
     Q13: obtrudeValue/obtrudeException (advanced)
     A: Forcibly overwrite a result/exception. Dangerous; avoid in production without strong reason.
     */
    static void q13_obtrudeValue_obtrudeException_advanced() {
        log("Q13: obtrudeValue/obtrudeException");
        CompletableFuture<String> cf = new CompletableFuture<>();
        cf.complete("first");
        log("before obtrude: " + cf.join());
        cf.obtrudeValue("forced");
        log("after obtrudeValue: " + cf.join());
        cf.obtrudeException(new RuntimeException("forced-ex"));
        try { cf.join(); } catch (CompletionException ce) { log("after obtrudeException: " + ce.getCause()); }
    }

    /*
     Q14: Async vs non-async stages; default vs custom executor?
     A:
      - Non-async (thenApply/thenAccept) runs in the thread that completed the previous stage.
      - Async variants (thenApplyAsync/thenAcceptAsync) submit to the default common pool or a provided executor.
     */
    static void q14_async_vs_sync_stages_default_vs_custom_executor() {
        log("Q14: async vs non-async");
        CompletableFuture<String> chain = CompletableFuture.supplyAsync(() -> {
            log("stage0 supplyAsync");
            return "x";
        }).thenApply(s -> {
            log("thenApply (same thread as completion)");
            return s + "1";
        }).thenApplyAsync(s -> {
            log("thenApplyAsync (common pool)");
            return s + "2";
        }).thenApplyAsync(s -> {
            log("thenApplyAsync (custom IO_POOL)");
            return s + "3";
        }, IO_POOL);

        log("chain result = " + chain.join());
    }

    /*
     Q15: Blocking inside stages? Avoid blocking the common pool.
     A:
      - If you must block, offload to a dedicated/bounded executor to avoid ForkJoinPool starvation.
     */
    static void q15_blocking_work_use_custom_executor_not_common_pool() {
        log("Q15: blocking work on custom executor");
        CompletableFuture<String> cf = CompletableFuture.supplyAsync(() -> "start")
            .thenCompose(s ->
                CompletableFuture.supplyAsync(() -> blockingCall(80), IO_POOL)
            )
            .thenApply(res -> res + " -> processed");
        log("result = " + cf.join());
    }

    /*
     Q16: Parallel aggregation pattern with allOf
     A: Launch independent tasks in parallel and aggregate all results.
     */
    static void q16_parallel_aggregation_pattern() {
        log("Q16: parallel aggregation pattern");
        CompletableFuture<String> a = CompletableFuture.supplyAsync(() -> { sleep(80); return "A"; });
        CompletableFuture<String> b = CompletableFuture.supplyAsync(() -> { sleep(60); return "B"; });
        CompletableFuture<String> c = CompletableFuture.supplyAsync(() -> { sleep(40); return "C"; });

        CompletableFuture<String> aggregated = CompletableFuture
            .allOf(a, b, c)
            .thenApply(v -> a.join() + b.join() + c.join());

        log("aggregated = " + aggregated.join());
    }

    /*
     Q17: completedFuture for seeding pipelines?
     A: Use completedFuture to start a chain without async work or to provide defaults.
     */
    static void q17_completedFuture_seeding_pipelines() {
        log("Q17: completedFuture");
        String result = CompletableFuture.completedFuture(10)
            .thenApply(x -> x + 5)
            .thenCompose(x -> CompletableFuture.supplyAsync(() -> x * 2))
            .join();
        log("completedFuture seeded result = " + result);
    }

    /*
     Additional interview tips (comments only):
     - Differences vs parallel streams: CompletableFuture gives explicit control over async boundaries and executors; parallel streams are data-parallel and use common pool implicitly.
     - ThreadLocal is not propagated across async boundaries by default.
     - Use timeouts and cancellation for robustness; prefer non-blocking chaining; avoid get/join in common-pool threads when blocking.
     - Memory visibility: completion establishes happens-before for dependent actions.
     - For repeated scheduling, use delayedExecutor or ScheduledExecutorService.
     */

    // Bonus: helper to sequence a list of CompletableFutures into one CompletableFuture<List<T>>
    static <T> CompletableFuture<List<T>> sequence(List<CompletableFuture<T>> futures) {
        CompletableFuture<Void> all = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        return all.thenApply(v -> futures.stream().map(CompletableFuture::join).collect(Collectors.toList()));
    }

    // Bonus: delayed task example (Java 9+)
    static CompletableFuture<String> delayedValue(String v, long delayMs) {
        Executor delayed = CompletableFuture.delayedExecutor(delayMs, TimeUnit.MILLISECONDS);
        return CompletableFuture.supplyAsync(() -> v, delayed);
    }
}