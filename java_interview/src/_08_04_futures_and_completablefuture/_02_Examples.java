package _08_04_futures_and_completablefuture;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Futures & CompletableFuture – practical, commented examples.
 * Run the main method to see output and execution order.
 */
public class _02_Examples {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Futures (java.util.concurrent.Future) ===");
        futureBasics();
        bridgeFutureToCompletableFuture();
        System.out.println();

        System.out.println("=== CompletableFuture: basics (runAsync/supplyAsync, join vs get) ===");
        completableFutureBasics();
        System.out.println();

        System.out.println("=== Chaining: thenApply/thenAccept/thenRun (sync vs async) ===");
        chainingBasics();
        System.out.println();

        System.out.println("=== Composition: thenCompose (dependent async tasks) ===");
        compositionThenCompose();
        System.out.println();

        System.out.println("=== Combination: thenCombine/acceptBoth/runAfterBoth (independent tasks) ===");
        combinationThenCombine();
        System.out.println();

        System.out.println("=== allOf/anyOf and sequence helper ===");
        allOfAnyOfExamples();
        System.out.println();

        System.out.println("=== Exception handling: exceptionally/handle/whenComplete ===");
        exceptionHandlingExamples();
        System.out.println();

        System.out.println("=== Timeouts (Java 9+) and cancellation ===");
        timeoutsAndCancellation();
        System.out.println();

        System.out.println("=== Manual completion and delayedExecutor (Java 9+) ===");
        manualCompletionAndDelayedExecutor();
        System.out.println();

        System.out.println("=== Custom Executors and threading ===");
        customExecutorsAndThreading();
        System.out.println();

        System.out.println("=== Parallel aggregation with streams + CompletableFuture ===");
        parallelAggregation();
        System.out.println();

        System.out.println("=== allOf with failure ===");
        allOfFailure();
        System.out.println();

        System.out.println("Done.");
    }

    // --------------------------------------------------------------------------------------------
    // 1) Legacy Future basics
    // --------------------------------------------------------------------------------------------
    private static void futureBasics() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(3, namedThreadFactory("future-pool-%d"));
        try {
            // Submit a Callable and blockingly get result (or timeout)
            Future<Integer> f = pool.submit(() -> {
                log("Callable started");
                sleep(200);
                return 42;
            });

            log("Do other work while Callable runs...");
            Integer result = f.get(1, TimeUnit.SECONDS); // blocks
            log("Future result = " + result + ", isDone=" + f.isDone());

            // Timeout + cancel
            Future<Integer> f2 = pool.submit(() -> {
                sleep(500);
                return 7;
            });
            try {
                f2.get(100, TimeUnit.MILLISECONDS);
            } catch (TimeoutException te) {
                log("Timed out waiting for f2; cancelling...");
                boolean cancelled = f2.cancel(true);
                log("f2 cancelled=" + cancelled + ", isCancelled=" + f2.isCancelled());
            }

            // invokeAll: runs a batch and waits for all
            List<Callable<String>> tasks = Arrays.asList(
                    () -> {
                        sleep(150);
                        return "A";
                    },
                    () -> {
                        sleep(50);
                        return "B";
                    },
                    () -> {
                        sleep(100);
                        return "C";
                    }
            );
            List<Future<String>> futures = pool.invokeAll(tasks);
            for (Future<String> fu : futures) {
                log("invokeAll result = " + fu.get());
            }

            // invokeAny: returns the first successful result
            String fastest = pool.invokeAny(Arrays.asList(
                    () -> {
                        sleep(300);
                        return "slow";
                    },
                    () -> {
                        sleep(80);
                        return "fast";
                    },
                    () -> {
                        sleep(120);
                        return "medium";
                    }
            ));
            log("invokeAny fastest result = " + fastest);

            // Limitation: no easy composition; you must block and then start next work.
            Future<String> f3 = pool.submit(() -> {
                sleep(80);
                return "x";
            });
            String x = f3.get(); // block
            Future<String> f4 = pool.submit(() -> x + "y"); // start only after get()
            log("Manual composition with Future = " + f4.get());
        } finally {
            pool.shutdown();
        }
    }

    // Bridge a legacy Future<T> to CompletableFuture<T> (note: this blocks a thread)
    private static void bridgeFutureToCompletableFuture() throws Exception {
        ExecutorService pool = Executors.newCachedThreadPool(namedThreadFactory("bridge-pool-%d"));
        try {
            Future<Integer> legacy = pool.submit(() -> {
                sleep(80);
                return 5;
            });
            CompletableFuture<Integer> cf = toCompletableFuture(legacy, pool)
                    .thenApply(x -> x * 2);
            log("Bridged Future -> CompletableFuture result = " + cf.join());
        } finally {
            pool.shutdown();
        }
    }

    // --------------------------------------------------------------------------------------------
    // 2) CompletableFuture basics
    // --------------------------------------------------------------------------------------------
    private static void completableFutureBasics() {
        // runAsync: fire-and-forget (Void result)
        CompletableFuture<Void> run = CompletableFuture.runAsync(() -> {
            log("runAsync side effect");
            sleep(100);
        });

        // supplyAsync: produces a value
        CompletableFuture<Integer> supply = CompletableFuture.supplyAsync(() -> {
            log("supplyAsync computing 21");
            sleep(120);
            return 21;
        }).thenApply(x -> {
            log("thenApply multiply by 2");
            return x * 2;
        });

        // Non-blocking continuation
        supply.thenAccept(v -> log("thenAccept saw value=" + v));

        // Wait for completion
        run.join();              // join avoids checked exceptions
        Integer val = supply.join();
        log("supplyAsync final value via join=" + val + " (get would require try/catch)");
    }

    // --------------------------------------------------------------------------------------------
    // 3) thenApply/thenAccept/thenRun, sync vs async
    // --------------------------------------------------------------------------------------------
    private static void chainingBasics() {
        CompletableFuture<String> base = CompletableFuture.supplyAsync(() -> {
            log("base compute");
            sleep(100);
            return "hello";
        });

        // thenApply: transform the result (runs in the same thread that completes prior stage)
        CompletableFuture<String> upper = base.thenApply(s -> {
            log("thenApply (likely same thread as base completion)");
            return s.toUpperCase();
        });

        // thenApplyAsync: transform on a different thread (ForkJoinPool.commonPool by default)
        CompletableFuture<String> upperAsync = base.thenApplyAsync(s -> {
            log("thenApplyAsync (commonPool)");
            return s.toUpperCase() + " ASYNC";
        });

        // thenAccept: consume result, no return
        CompletableFuture<Void> consumed = upper.thenAccept(s -> log("thenAccept consumed: " + s));

        // thenRun: no input, just run after completion
        CompletableFuture<Void> ran = upper.thenRun(() -> log("thenRun side-effect after upper"));

        log("upper=" + upper.join());
        log("upperAsync=" + upperAsync.join());
        consumed.join();
        ran.join();
    }

    // --------------------------------------------------------------------------------------------
    // 4) thenCompose: flat-map dependent async stages
    // --------------------------------------------------------------------------------------------
    private static void compositionThenCompose() {
        CompletableFuture<Integer> userId = getUserIdAsync("alice");
        CompletableFuture<String> profile = userId.thenCompose(_02_Examples::getUserProfileAsync);
        log("Profile: " + profile.join());
    }

    // --------------------------------------------------------------------------------------------
    // 5) thenCombine / acceptBoth / runAfterBoth: combine independent tasks
    // --------------------------------------------------------------------------------------------
    private static void combinationThenCombine() {
        CompletableFuture<Integer> priceA = CompletableFuture.supplyAsync(() -> {
            sleep(120);
            log("priceA ready");
            return 100;
        });
        CompletableFuture<Integer> priceB = CompletableFuture.supplyAsync(() -> {
            sleep(80);
            log("priceB ready");
            return 110;
        });

        // Combine both results when both complete
        CompletableFuture<Integer> best = priceA.thenCombine(priceB, Math::min);
        log("Best price = " + best.join());

        // Accept both (no new result), run after both
        priceA.thenAcceptBoth(priceB, (a, b) -> log("Both prices: A=" + a + ", B=" + b)).join();
        priceA.runAfterBoth(priceB, () -> log("runAfterBoth executed")).join();

        // "Either" family: pick the first to complete
        CompletableFuture<String> slow = CompletableFuture.supplyAsync(() -> {
            sleep(200);
            return "SLOW";
        });
        CompletableFuture<String> fast = CompletableFuture.supplyAsync(() -> {
            sleep(60);
            return "FAST";
        });
        CompletableFuture<String> chosen = slow.applyToEither(fast, v -> "First: " + v);
        log(chosen.join());
    }

    // --------------------------------------------------------------------------------------------
    // 6) allOf / anyOf + sequence helper to get List<T> out
    // --------------------------------------------------------------------------------------------
    private static void allOfAnyOfExamples() {
        List<CompletableFuture<String>> tasks = IntStream.rangeClosed(1, 5)
                .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                    sleep(i * 40L);
                    return "T" + i;
                }))
                .collect(Collectors.toList());

        // allOf returns CompletableFuture<Void>; wrap to List<String> via helper
        CompletableFuture<List<String>> allResults = sequence(tasks);
        log("allOf results (ordered) = " + allResults.join());

        // anyOf returns first result as Object
        CompletableFuture<String> slow = CompletableFuture.supplyAsync(() -> {
            sleep(200);
            return "slow";
        });
        CompletableFuture<String> fast = CompletableFuture.supplyAsync(() -> {
            sleep(80);
            return "fast";
        });
        CompletableFuture<Object> first = CompletableFuture.anyOf(slow, fast);
        log("anyOf first = " + first.join());
    }

    // --------------------------------------------------------------------------------------------
    // 7) Exception handling: exceptionally / handle / whenComplete
    // --------------------------------------------------------------------------------------------
    private static void exceptionHandlingExamples() {
        // exceptionally: recover with fallback value
        CompletableFuture<Integer> failing = CompletableFuture.supplyAsync(() -> {
            log("failing task throws");
            throw new IllegalStateException("boom");
        });
        CompletableFuture<Integer> recovered = failing.exceptionally(ex -> {
            log("exceptionally saw: " + ex);
            return 0; // fallback
        });
        log("Recovered value = " + recovered.join());

        // handle: see both value and exception, choose output
        CompletableFuture<String> handled = CompletableFuture.<String>supplyAsync(() -> {
            sleep(50);
            if (true) throw new RuntimeException("oops");
            return "";
        }).handle((val, ex) -> ex == null ? val : "fallback-from-handle");
        log("handle result = " + handled.join());

        // whenComplete: observe (side-effect), does not change outcome
        String observed = CompletableFuture.supplyAsync(() -> "value")
                .whenComplete((v, ex) -> log("whenComplete observed value=" + v + ", ex=" + ex))
                .thenApply(v -> v + "!")
                .join();
        log("whenComplete final = " + observed);
    }

    // --------------------------------------------------------------------------------------------
    // 8) Timeouts (Java 9+) and cancellation
    // --------------------------------------------------------------------------------------------
    private static void timeoutsAndCancellation() {
        // orTimeout: complete exceptionally if not done by deadline
        CompletableFuture<String> slow = new CompletableFuture<>();
        String timeoutRecovered = slow
                .orTimeout(100, TimeUnit.MILLISECONDS)
                .exceptionally(ex -> {
                    log("orTimeout triggered: " + ex.getClass().getSimpleName());
                    return "timeout-fallback";
                })
                .join();
        log("orTimeout result after recovery = " + timeoutRecovered);

        // completeOnTimeout: complete normally with default value if time runs out
        CompletableFuture<String> slow2 = new CompletableFuture<>();
        String defaulted = slow2
                .completeOnTimeout("default", 100, TimeUnit.MILLISECONDS)
                .join();
        log("completeOnTimeout result = " + defaulted);

        // Cancellation of CompletableFuture
        CompletableFuture<Integer> cancellable = new CompletableFuture<>();
        boolean cancelled = cancellable.cancel(true);
        log("cancelled=" + cancelled + ", isCancelled=" + cancellable.isCancelled());
        try {
            cancellable.join(); // throws CancellationException
        } catch (CancellationException ce) {
            log("join observed CancellationException");
        }
    }

    // --------------------------------------------------------------------------------------------
    // 9) Manual completion and delayedExecutor (Java 9+)
    // --------------------------------------------------------------------------------------------
    private static void manualCompletionAndDelayedExecutor() {
        // Manually completing a CompletableFuture
        CompletableFuture<String> manual = new CompletableFuture<>();
        // Complete after a small delay using delayedExecutor
        Executor delayed50ms = CompletableFuture.delayedExecutor(50, TimeUnit.MILLISECONDS);
        CompletableFuture.runAsync(() -> {
            log("Manually completing with OK");
            manual.complete("OK");
        }, delayed50ms);
        log("Manual completion result = " + manual.join());

        // Manually complete exceptionally
        CompletableFuture<String> manualFail = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            log("Manually failing with RuntimeException");
            manualFail.completeExceptionally(new RuntimeException("manual-boom"));
        }, delayed50ms);
        try {
            manualFail.join();
        } catch (CompletionException ce) {
            log("Caught manual failure: " + ce.getCause());
        }

        // Use delayedExecutor to delay the start of a task
        String delayed = CompletableFuture.supplyAsync(() -> {
            log("Delayed compute");
            return "delayed-result";
        }, CompletableFuture.delayedExecutor(60, TimeUnit.MILLISECONDS)).join();
        log("Delayed executor result = " + delayed);
    }

    // --------------------------------------------------------------------------------------------
    // 10) Custom executors and thread control
    // --------------------------------------------------------------------------------------------
    private static void customExecutorsAndThreading() {
        ExecutorService io = Executors.newFixedThreadPool(2, namedThreadFactory("io-%d"));
        ExecutorService cpu = Executors.newFixedThreadPool(2, namedThreadFactory("cpu-%d"));
        try {
            CompletableFuture<String> cf = CompletableFuture.supplyAsync(() -> {
                log("I/O-bound work on io executor");
                sleep(80);
                return "data";
            }, io).thenApplyAsync(s -> {
                log("CPU-bound transform on cpu executor");
                return s.toUpperCase();
            }, cpu).thenApply(s -> {
                log("thenApply (same thread as previous completion)");
                return "[" + s + "]";
            });

            log("Custom executors result = " + cf.join());
        } finally {
            io.shutdown();
            cpu.shutdown();
        }
    }

    // --------------------------------------------------------------------------------------------
    // 11) Parallel aggregation pattern with streams + CompletableFuture
    // --------------------------------------------------------------------------------------------
    private static void parallelAggregation() {
        List<String> products = Arrays.asList("pencil", "notebook", "eraser", "marker", "ruler");
        ExecutorService io = Executors.newFixedThreadPool(4, namedThreadFactory("fetch-%d"));
        try {
            // Fire off all requests concurrently
            List<CompletableFuture<Integer>> priceFutures = products.stream()
                    .map(p -> CompletableFuture.supplyAsync(() -> fetchPrice(p), io))
                    .collect(Collectors.toList());

            // Gather results preserving order
            List<Integer> prices = sequence(priceFutures).join();
            log("Prices (ordered) = " + prices);
        } finally {
            io.shutdown();
        }
    }

    // --------------------------------------------------------------------------------------------
    // 12) allOf with failure
    // --------------------------------------------------------------------------------------------
    private static void allOfFailure() {
        CompletableFuture<String> ok1 = CompletableFuture.supplyAsync(() -> {
            sleep(50);
            return "ok1";
        });
        CompletableFuture<String> bad = CompletableFuture.supplyAsync(() -> {
            sleep(70);
            throw new RuntimeException("boom-in-batch");
        });
        CompletableFuture<String> ok2 = CompletableFuture.supplyAsync(() -> {
            sleep(90);
            return "ok2";
        });

        CompletableFuture<Void> all = CompletableFuture.allOf(ok1, bad, ok2);
        try {
            all.join(); // completes exceptionally because one failed
        } catch (CompletionException ex) {
            log("allOf failed due to: " + ex.getCause());
        }

        // Individual futures still have their own states
        log("ok1 isDone=" + ok1.isDone() + ", value=" + ok1.getNow("not-ready"));
        log("bad isCompletedExceptionally=" + bad.isCompletedExceptionally());
        log("ok2 isDone=" + ok2.isDone() + ", value=" + ok2.getNow("not-ready"));
    }

    // ============================================================================================
    // Helpers
    // ============================================================================================

    // Convert legacy Future<T> to CompletableFuture<T> by blocking on a provided executor thread.
    // Note: this ties up a thread and is not a true non-blocking bridge, but often acceptable.
    private static <T> CompletableFuture<T> toCompletableFuture(Future<T> future, Executor executor) {
        Objects.requireNonNull(future);
        return CompletableFuture.supplyAsync(() -> {
            try {
                return future.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new CompletionException(e);
            } catch (ExecutionException e) {
                throw new CompletionException(e.getCause());
            }
        }, executor);
    }

    // sequence: turn List<CompletableFuture<T>> into CompletableFuture<List<T>> preserving order
    private static <T> CompletableFuture<List<T>> sequence(List<CompletableFuture<T>> futures) {
        CompletableFuture<?>[] arr = futures.toArray(new CompletableFuture[0]);
        return CompletableFuture.allOf(arr)
                .thenApply(v -> futures.stream().map(CompletableFuture::join).collect(Collectors.toList()));
    }

    private static CompletableFuture<Integer> getUserIdAsync(String username) {
        return CompletableFuture.supplyAsync(() -> {
            log("lookup userId for " + username);
            sleep(60);
            return 42;
        });
    }

    private static CompletableFuture<String> getUserProfileAsync(int userId) {
        return CompletableFuture.supplyAsync(() -> {
            log("load profile for userId=" + userId);
            sleep(80);
            return "User#" + userId + " {profile}";
        });
    }

    private static int fetchPrice(String product) {
        log("Fetching price for " + product);
        sleep(40 + Math.abs(product.hashCode() % 40));
        return product.length() * 10; // stub logic
    }

    private static void log(String msg) {
        System.out.printf("[%s] %s%n", Thread.currentThread().getName(), msg);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private static ThreadFactory namedThreadFactory(String pattern) {
        AtomicInteger seq = new AtomicInteger(1);
        return r -> {
            Thread t = new Thread(r, String.format(pattern, seq.getAndIncrement()));
            t.setDaemon(false);
            return t;
        };
    }
}