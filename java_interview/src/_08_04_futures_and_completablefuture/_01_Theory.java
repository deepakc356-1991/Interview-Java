package _08_04_futures_and_completablefuture;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Futures & CompletableFuture — Theory and Practical Guide (Java)
 *
 * What is a Future?
 * - A Future<T> is a handle to a result that may become available later.
 * - You submit a task (Callable/Void Runnable) to an ExecutorService and receive a Future.
 * - You can: get() the result (blocking), get(timeout), isDone(), cancel(), isCancelled().
 * - Future is a write-once container: once completed, result is immutable.
 *
 * Limitations of Future
 * - No completion callbacks out of the box (you must block/poll).
 * - No fluent composition: cannot easily chain dependent tasks (A then B).
 * - No combinators: cannot easily wait for "any" or "all" of multiple futures without manual work.
 * - No built-in exception recovery or transformation pipeline.
 * - Cannot manually complete a Future from the outside (unless using FutureTask and controlling its run).
 *
 * CompletableFuture and CompletionStage
 * - CompletableFuture<T> implements Future<T> and CompletionStage<T>.
 * - Adds: completion callbacks, chaining, composition, combination, manual completion, exception handling.
 * - Two key creation methods:
 *   - supplyAsync(Supplier<T>) for tasks that produce a value.
 *   - runAsync(Runnable) for tasks that don't produce a value.
 *   - Both have overloads to specify an Executor; otherwise they use the ForkJoinPool.commonPool().
 *
 * Synchronous vs Asynchronous callbacks
 * - Non-async methods (thenApply/thenAccept/thenRun/thenCompose/etc.) run in the thread that completes the previous stage.
 * - Async variants (thenApplyAsync/thenAcceptAsync/thenRunAsync/thenComposeAsync/etc.) schedule work in an Executor.
 *
 * Composition and Combination
 * - thenApply: transform the result (T -> U).
 * - thenCompose: flatten nested futures (T -> CompletionStage<U>) to avoid Future<Future<U>>.
 * - thenCombine: combine two independent results (A + B -> C).
 * - allOf: wait for all futures to complete.
 * - anyOf: proceed when any one completes.
 * - acceptEither/applyToEither: take the first result among two.
 *
 * Exception handling
 * - exceptionally(fn): recover by mapping Throwable -> T.
 * - handle((value, ex)): observe and transform both success or failure into a new result.
 * - whenComplete((value, ex)): observe side effects; does not change the result.
 * - get() throws checked ExecutionException/InterruptedException; join() throws unchecked CompletionException.
 *
 * Timeouts and Cancellation
 * - Future.get(timeout) works for any Future.
 * - CompletableFuture (Java 9+): orTimeout, completeOnTimeout.
 * - Cancellation: cancel(true) attempts to cancel; CompletableFuture completion becomes exceptional with CancellationException.
 *
 * Threading and Executors
 * - Without an explicit Executor, async stages use the common ForkJoinPool (sized ~#cores).
 * - For blocking I/O or long tasks, prefer a custom Executor to avoid starving the common pool.
 * - Non-async dependent stages may execute inline on the thread completing the prior stage.
 *
 * Memory visibility and happens-before
 * - Completing a CompletableFuture safely publishes the result: dependent stages observe writes "before" completion.
 *
 * Best practices (high level)
 * - Avoid blocking in the common pool; use custom executors for blocking I/O.
 * - Prefer thenCompose over thenApply when a stage returns a future.
 * - Use allOf/anyOf for fan-in/out patterns.
 * - Use exceptionally/handle for robust recovery.
 * - Keep tasks small and non-blocking where possible; avoid deep recursive async chains without strategy.
 *
 * Notes on Java versions
 * - Methods like orTimeout/completeOnTimeout/delayedExecutor appeared in Java 9+. They are shown in comments to avoid compile issues on older JDKs.
 */
public class _01_Theory {

    private static final ScheduledExecutorService SCHEDULER =
            Executors.newScheduledThreadPool(1, r -> {
                Thread t = new Thread(r, "scheduler");
                t.setDaemon(true);
                return t;
            });
    private static final AtomicInteger THREAD_ID = new AtomicInteger(1);

    private static final ExecutorService BLOCKING_IO_POOL =
            Executors.newFixedThreadPool(4, r -> {
                Thread t = new Thread(r, "blocking-io-" + THREAD_ID.getAndIncrement());
                t.setDaemon(true);
                return t;
            });

    private static final ExecutorService CPU_POOL =
            Executors.newWorkStealingPool(); // Good for CPU-bound tasks (ForkJoin under the hood)

    private static final Random RAND = new Random();

    public static void main(String[] args) throws Exception {
        log("==== Future basics ====");
        demoFutureBasics();

        log("\n==== Future limitations (explained) ====");
        demoFutureLimitations();

        log("\n==== CompletableFuture basics ====");
        demoCompletableFutureBasics();

        log("\n==== thenCompose vs thenApply ====");
        demoCompositionThenApplyCompose();

        log("\n==== Combining futures (thenCombine/allOf/anyOf) ====");
        demoCombiningThenCombineAllOfAnyOf();

        log("\n==== Exception handling and recovery ====");
        demoExceptionHandling();

        log("\n==== Timeouts and cancellation ====");
        demoTimeoutsCancellation();

        log("\n==== Custom executors and threading model ====");
        demoCustomExecutorBehavior();

        log("\n==== Manual completion and bridging callbacks ====");
        demoManualCompletionAndBridging();

        log("\n==== Completed futures and convenience APIs ====");
        demoConvenienceAPIs();

        // Cleanup
        BLOCKING_IO_POOL.shutdownNow();
        CPU_POOL.shutdownNow();
        SCHEDULER.shutdownNow();
        log("\nDone.");
    }

    // ---------------------------------- FUTURE ----------------------------------

    private static void demoFutureBasics() {
        ExecutorService ex = Executors.newFixedThreadPool(2, r -> new Thread(r, "future-pool"));

        Future<String> f = ex.submit(() -> {
            sleep(200);
            return "result-from-callable";
        });

        log("Future isDone? " + f.isDone());
        try {
            String v = f.get(); // blocks
            log("Future get(): " + v);
        } catch (Exception e) {
            log("Unexpected: " + e);
        }
        log("Future isDone? " + f.isDone());
        log("Future cancel after completion: " + f.cancel(true)); // false: already done

        // Timeout example
        Future<String> f2 = ex.submit(() -> {
            sleep(400);
            return "slow";
        });
        try {
            f2.get(100, TimeUnit.MILLISECONDS); // will timeout
        } catch (TimeoutException te) {
            log("Timed out as expected: " + te.getClass().getSimpleName());
        } catch (Exception e) {
            log("Unexpected: " + e);
        }

        // FutureTask example (manual execution)
        FutureTask<Integer> ft = new FutureTask<>(() -> {
            sleep(100);
            return 42;
        });
        new Thread(ft, "future-task-thread").start();
        try {
            log("FutureTask get(): " + ft.get());
        } catch (Exception e) {
            log("Unexpected: " + e);
        }

        // ScheduledFuture example
        ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "scheduled"));
        ScheduledFuture<String> sf = ses.schedule(() -> "delayed", 100, TimeUnit.MILLISECONDS);
        try {
            log("ScheduledFuture get(): " + sf.get());
        } catch (Exception e) {
            log("Unexpected: " + e);
        }
        ses.shutdownNow();
        ex.shutdownNow();
    }

    private static void demoFutureLimitations() {
        /*
         Typical patterns with Future force blocking/polling:

         Example scenario: fetch userId, then fetch user profile by id.
         With Future, you'd often do:
           Future<Integer> idF = ex.submit(this::getUserId);
           Integer id = idF.get(); // blocking
           Future<Profile> profF = ex.submit(() -> fetchProfile(id));
           Profile p = profF.get(); // blocking
         - You cannot express "when id completes, then start fetching profile" without manually wiring threads.
         - No simple "combine two futures" or "first completed wins" without extra plumbing.
         - No callback on completion to trigger next steps (unless you poll isDone or block).
        */
    }

    // ----------------------------- COMPLETABLEFUTURE BASICS -----------------------------

    private static void demoCompletableFutureBasics() {
        // supplyAsync produces a value asynchronously
        CompletableFuture<String> cf =
                CompletableFuture.supplyAsync(() -> {
                    log("Supplying value...");
                    sleep(100);
                    return "value";
                });

        // thenApply transforms the result (sync, inline on completing thread)
        CompletableFuture<Integer> lengthCF =
                cf.thenApply(s -> {
                    log("thenApply on: " + s);
                    return s.length();
                });

        // thenAccept consumes the result (no new value)
        CompletableFuture<Void> consumed =
                lengthCF.thenAccept(len -> log("Length: " + len));

        // thenRun runs a Runnable after completion (no value passed)
        CompletableFuture<Void> after =
                consumed.thenRun(() -> log("Done chain 1"));

        after.join(); // wait for chain

        // Async variants run in an executor (default: common pool)
        CompletableFuture<String> cf2 =
                CompletableFuture.supplyAsync(() -> {
                    log("supplyAsync on " + Thread.currentThread().getName());
                    return "X";
                }).thenApplyAsync(x -> {
                    log("thenApplyAsync on " + Thread.currentThread().getName());
                    return x + "Y";
                });

        log("cf2 result via join: " + cf2.join());

        // runAsync for tasks without a value
        CompletableFuture<Void> runOnly =
                CompletableFuture.runAsync(() -> {
                    log("runAsync doing work...");
                    sleep(50);
                });
        runOnly.join();

        // join vs get: join doesn't throw checked exceptions
        CompletableFuture<String> exCF =
                CompletableFuture.supplyAsync(() -> {
                    throw new RuntimeException("boom");
                });
        try {
            exCF.get(); // checked ExecutionException
        } catch (Exception e) {
            log("get() threw: " + e.getClass().getSimpleName());
        }
        try {
            exCF.join(); // unchecked CompletionException
        } catch (CompletionException e) {
            log("join() threw CompletionException (wrapped): " + e.getCause().getClass().getSimpleName());
        }
    }

    // -------------------------- thenApply vs thenCompose --------------------------

    private static void demoCompositionThenApplyCompose() {
        // thenApply -> nested future
        CompletableFuture<CompletableFuture<String>> nested =
                getUserIdAsync().thenApply(_01_Theory::fetchUserProfileAsync);
        // Join twice or flatten manually
        log("Nested join: " + nested.join().join());

        // thenCompose -> flatten
        CompletableFuture<String> flat =
                getUserIdAsync().thenCompose(_01_Theory::fetchUserProfileAsync);
        log("thenCompose result: " + flat.join());
    }

    private static CompletableFuture<Integer> getUserIdAsync() {
        return CompletableFuture.supplyAsync(() -> {
            sleep(80);
            return 101; // pretend to fetch from DB
        });
    }

    private static CompletableFuture<String> fetchUserProfileAsync(Integer id) {
        return CompletableFuture.supplyAsync(() -> {
            sleep(120);
            return "Profile#" + id;
        });
    }

    // ------------------------ Combining: thenCombine/allOf/anyOf ------------------------

    private static void demoCombiningThenCombineAllOfAnyOf() {
        CompletableFuture<Integer> priceA = CompletableFuture.supplyAsync(() -> {
            sleep(120);
            return 50;
        });
        CompletableFuture<Integer> priceB = CompletableFuture.supplyAsync(() -> {
            sleep(150);
            return 70;
        });

        // Combine two independent results
        CompletableFuture<Integer> total =
                priceA.thenCombine(priceB, Integer::sum);
        log("thenCombine total: " + total.join());

        // anyOf: proceed with the first completed
        CompletableFuture<Object> any =
                CompletableFuture.anyOf(
                        CompletableFuture.supplyAsync(() -> {
                            sleep(90);
                            return "fast";
                        }),
                        CompletableFuture.supplyAsync(() -> {
                            sleep(200);
                            return "slow";
                        })
                );
        log("anyOf first: " + any.join());

        // allOf: wait for all, then gather results with join()
        List<CompletableFuture<Integer>> batch = Arrays.asList(
                CompletableFuture.supplyAsync(() -> slowCompute(1)),
                CompletableFuture.supplyAsync(() -> slowCompute(2)),
                CompletableFuture.supplyAsync(() -> slowCompute(3))
        );
        CompletableFuture<Void> all = CompletableFuture.allOf(batch.toArray(new CompletableFuture[0]));
        CompletableFuture<List<Integer>> collected = all.thenApply(v -> {
            List<Integer> list = new ArrayList<>(batch.size());
            for (CompletableFuture<Integer> c : batch) {
                list.add(c.join()); // safe here; all completed
            }
            return list;
        });
        log("allOf -> " + collected.join());
    }

    // ----------------------------- Exception handling -----------------------------

    private static void demoExceptionHandling() {
        // exceptionally: recover with fallback value
        String recovered =
                CompletableFuture.supplyAsync(() -> {
                    throw new IllegalStateException("primary failed");
                }).exceptionally(ex -> {
                    log("Recovering from: " + ex.getClass().getSimpleName());
                    return "fallback";
                }).join().toString();
        log("Recovered: " + recovered);

        // handle: observe and transform either success or failure
        String handled =
                CompletableFuture.supplyAsync(() -> {
                    if (RAND.nextBoolean()) throw new RuntimeException("random fail");
                    return "ok";
                }).handle((val, ex) -> {
                    if (ex != null) {
                        log("handle saw: " + ex.getClass().getSimpleName());
                        return "default";
                    }
                    return val.toUpperCase();
                }).join();
        log("Handled: " + handled);

        // whenComplete: side-effects only, does not change outcome
        try {
            CompletableFuture<Object> cf =
                    CompletableFuture.supplyAsync(() -> {
                        throw new RuntimeException("oops");
                    }).whenComplete((v, ex) -> {
                        log("whenComplete saw exception? " + (ex != null));
                    });
            cf.join(); // throws CompletionException
        } catch (CompletionException ce) {
            log("whenComplete chain still failed: " + ce.getCause().getClass().getSimpleName());
        }

        // applyToEither: take the first success among two
        CompletableFuture<String> fast =
                CompletableFuture.supplyAsync(() -> {
                    sleep(60);
                    return "A";
                });
        CompletableFuture<String> slow =
                CompletableFuture.supplyAsync(() -> {
                    sleep(200);
                    return "B";
                });
        String first = fast.applyToEither(slow, x -> "first=" + x).join();
        log("applyToEither: " + first);
    }

    // ------------------------ Timeouts and cancellation ------------------------

    private static void demoTimeoutsCancellation() {
        // Future.get(timeout)
        CompletableFuture<String> slow = CompletableFuture.supplyAsync(() -> {
            sleep(300);
            return "slow";
        });
        try {
            slow.get(50, TimeUnit.MILLISECONDS);
        } catch (TimeoutException te) {
            log("CompletableFuture get(timeout) timed out");
        } catch (Exception e) {
            log("Unexpected: " + e);
        }

        // Java 9+ convenient timeouts (commented for Java 8 compatibility):
        // CompletableFuture<String> withTimeout =
        //     CompletableFuture.supplyAsync(() -> { sleep(300); return "late"; })
        //                      .orTimeout(100, TimeUnit.MILLISECONDS);
        // try { withTimeout.join(); } catch (CompletionException ce) { log("Timed out via orTimeout"); }

        // Manual timeout using ScheduledExecutorService (works on Java 8)
        CompletableFuture<String> manualTimeout = new CompletableFuture<>();
        SCHEDULER.schedule(() -> manualTimeout.completeExceptionally(new TimeoutException("manual timeout")),
                100, TimeUnit.MILLISECONDS);
        CompletableFuture<String> work = CompletableFuture.supplyAsync(() -> {
            sleep(300);
            return "done";
        });
        work.whenComplete((v, ex) -> {
            // only complete if not already timed out
            manualTimeout.complete(Optional.ofNullable(v).orElse("done"));
        });
        try {
            manualTimeout.join();
        } catch (CompletionException ce) {
            if (ce.getCause() instanceof TimeoutException) {
                log("Manual timeout triggered");
            }
        }

        // Cancellation: cancels and completes exceptionally with CancellationException
        CompletableFuture<String> cancelMe = new CompletableFuture<>();
        CompletableFuture<String> dependent = cancelMe.thenApply(s -> "dep:" + s);
        boolean cancelled = cancelMe.cancel(true);
        log("cancel() returned: " + cancelled);
        try {
            dependent.join(); // will throw
        } catch (CompletionException ce) {
            log("Dependent saw cancellation: " + ce.getCause().getClass().getSimpleName());
        }
    }

    // ------------------ Executors and threading model (async vs non-async) ------------------

    private static void demoCustomExecutorBehavior() {
        // Non-async dependent stage runs in the thread that completes the previous stage
        CompletableFuture<Void> inline =
                CompletableFuture.supplyAsync(() -> {
                    log("Stage1 (common-pool)");
                    return 1;
                }).thenApply(x -> {
                    log("thenApply (same thread likely) x=" + x);
                    return x + 1;
                }).thenAccept(x -> log("thenAccept (inline) x=" + x));
        inline.join();

        // Async dependent stage runs in provided executor
        ExecutorService custom = Executors.newFixedThreadPool(2, r -> new Thread(r, "custom-exec"));
        CompletableFuture<Void> offloaded =
                CompletableFuture.supplyAsync(() -> {
                    log("Stage1 (common-pool)");
                    return 10;
                }).thenApplyAsync(x -> {
                    log("thenApplyAsync (custom) x=" + x);
                    return x * 2;
                }, custom).thenAcceptAsync(x -> log("thenAcceptAsync (custom) x=" + x), custom);
        offloaded.join();
        custom.shutdownNow();

        /*
         Pitfall example (in comments):
         - Deadlock risk if using a single-threaded executor and futures depend on each other.
           Example: submit task A (needs result of task B) and B scheduled in same single-thread executor.
           If A blocks waiting B while occupying the only thread, B never runs => deadlock.
         - Avoid blocking join/get within tasks in the same small executor unless you are sure about capacity.
        */
    }

    // ------------------ Manual completion and bridging callbacks ------------------

    private static void demoManualCompletionAndBridging() {
        // Manual completion
        CompletableFuture<String> manual = new CompletableFuture<>();
        SCHEDULER.schedule(() -> manual.complete("manually-completed"), 80, TimeUnit.MILLISECONDS);
        log("Manual completion result: " + manual.join());

        // Manual exceptional completion
        CompletableFuture<String> manualEx = new CompletableFuture<>();
        SCHEDULER.schedule(() -> manualEx.completeExceptionally(new RuntimeException("fail")), 50, TimeUnit.MILLISECONDS);
        try {
            manualEx.join();
        } catch (CompletionException ce) {
            log("Manual exceptional completion observed: " + ce.getCause().getMessage());
        }

        // Bridge a callback-style API to CompletableFuture
        CompletableFuture<String> bridged = callbackToCompletableFuture(_01_Theory::fakeCallbackApi);
        log("Bridged callback result: " + bridged.join());

        /*
         Note: CompletableFuture also has testing hooks like obtrudeValue/obtrudeException (not recommended for prod),
         and minimal convenience methods like newIncompleteFuture() to subclass behavior.
        */
    }

    // Simulated callback-based async API
    private static void fakeCallbackApi(Consumer<String> onSuccess, Consumer<Throwable> onError) {
        // Simulate success
        SCHEDULER.schedule(() -> onSuccess.accept("from-callback"), 70, TimeUnit.MILLISECONDS);
        // For error, you could instead call: onError.accept(new RuntimeException("callback-error"));
    }

    private static <T> CompletableFuture<T> callbackToCompletableFuture(BiConsumer2<Consumer<T>, Consumer<Throwable>> api) {
        CompletableFuture<T> cf = new CompletableFuture<>();
        api.accept(cf::complete, cf::completeExceptionally);
        return cf;
    }

    @FunctionalInterface
    private interface BiConsumer2<A, B> {
        void accept(A a, B b);
    }

    // ------------------ Convenience APIs ------------------

    private static void demoConvenienceAPIs() {
        // Already-completed future
        CompletableFuture<String> done = CompletableFuture.completedFuture("ready");
        log("completedFuture: " + done.join());

        // Supply with custom executor for blocking I/O
        CompletableFuture<String> viaBlockingPool =
                CompletableFuture.supplyAsync(() -> {
                    log("Blocking I/O simulation on " + Thread.currentThread().getName());
                    sleep(80); // simulate blocking
                    return "io-result";
                }, BLOCKING_IO_POOL);
        log("Blocking pool result: " + viaBlockingPool.join());

        // allOf with dynamic list helper
        List<CompletableFuture<Integer>> list = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            int n = i;
            list.add(CompletableFuture.supplyAsync(() -> slowCompute(n)));
        }
        List<Integer> results = allOfJoin(list);
        log("allOfJoin helper -> " + results);

        // anyOf typed helper (pattern)
        CompletableFuture<String> a = CompletableFuture.supplyAsync(() -> {
            sleep(60);
            return "A1";
        });
        CompletableFuture<String> b = CompletableFuture.supplyAsync(() -> {
            sleep(120);
            return "B1";
        });
        String winner = anyOfTyped(a, b).join();
        log("anyOfTyped winner: " + winner);
    }

    private static <T> List<T> allOfJoin(List<CompletableFuture<T>> futures) {
        CompletableFuture<?>[] arr = futures.toArray(new CompletableFuture[0]);
        return CompletableFuture.allOf(arr).thenApply(v -> {
            List<T> out = new ArrayList<>(futures.size());
            for (CompletableFuture<T> f : futures) out.add(f.join());
            return out;
        }).join();
    }

    @SafeVarargs
    private static <T> CompletableFuture<T> anyOfTyped(CompletableFuture<? extends T>... futures) {
        CompletableFuture<Object> any = CompletableFuture.anyOf(futures);
        return any.thenApply(obj -> (T) obj);
    }

    // ------------------ Helpers ------------------

    private static int slowCompute(int base) {
        sleep(50 + RAND.nextInt(50));
        return base * base;
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private static void log(String msg) {
        System.out.printf("[%s] %s%n", Thread.currentThread().getName(), msg);
    }
}