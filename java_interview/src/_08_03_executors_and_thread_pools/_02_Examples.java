package _08_03_executors_and_thread_pools;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/*
 Executors & Thread Pools - Examples

 This file shows practical examples of:
 - Executor vs ExecutorService
 - Fixed/Cached/Single/Scheduled thread pools
 - Callable, Future, FutureTask, timeouts, cancellation
 - invokeAll, invokeAny
 - CompletionService (completion in finish order)
 - Custom ThreadFactory (names, daemon, uncaught handler)
 - ThreadPoolExecutor tuning (core/max/queue/keepAlive), rejection policies
 - Exception handling in pools (afterExecute), removeOnCancelPolicy
 - Work-stealing pool
 - Virtual threads (Java 21+) via reflection
 - CompletableFuture with custom executor
 - Proper shutdown patterns

 Run main to execute demos. Each example cleans up its executors.
*/
public class _02_Examples {

    public static void main(String[] args) throws Exception {
        log("Starting Executors & Thread Pools examples");
        exBasicExecutor();
        exFixedThreadPool();
        exCachedThreadPool();
        exSingleThreadExecutor();
        exScheduledExecutor();
        exCallableAndFuture();
        exInvokeAllInvokeAny();
        exCompletionService();
        exCustomThreadFactory();
        exCustomThreadPoolAndRejection();
        exExceptionHandlingInPools();
        exTimeoutAndCancellation();
        exWorkStealingPool();
        exVirtualThreadsIfAvailable();          // Requires Java 21+ at runtime
        exCompletableFutureWithCustomExecutor();
        log("Done");
    }

    // 1) Executor vs ExecutorService
    private static void exBasicExecutor() {
        section("Executor vs ExecutorService");
        ExecutorService single = Executors.newSingleThreadExecutor(namedThreadFactory("basic", false, null));
        try {
            Executor executor = single; // Executor has only execute(Runnable)
            executor.execute(() -> log("Task A on " + Thread.currentThread().getName()));
            executor.execute(() -> log("Task B on " + Thread.currentThread().getName()));
        } finally {
            shutdownAndAwaitTermination(single);
        }
    }

    // 2) Fixed thread pool
    private static void exFixedThreadPool() {
        section("Fixed thread pool");
        ExecutorService pool = Executors.newFixedThreadPool(3, namedThreadFactory("fixed", false, null));
        try {
            for (int i = 0; i < 8; i++) {
                int id = i;
                pool.submit(() -> {
                    log("Start task " + id);
                    sleep(200);
                    log("Done task " + id);
                });
            }
        } finally {
            shutdownAndAwaitTermination(pool);
        }
    }

    // 3) Cached thread pool (good for many short-lived, bursty tasks)
    private static void exCachedThreadPool() {
        section("Cached thread pool");
        ExecutorService pool = Executors.newCachedThreadPool(namedThreadFactory("cached", false, null));
        try {
            for (int i = 0; i < 10; i++) {
                int id = i;
                pool.submit(() -> {
                    log("Work " + id);
                    sleep(100);
                });
            }
        } finally {
            shutdownAndAwaitTermination(pool);
        }
    }

    // 4) Single-thread executor (serializes task execution)
    private static void exSingleThreadExecutor() {
        section("Single-thread executor (ordered)");
        ExecutorService pool = Executors.newSingleThreadExecutor(namedThreadFactory("single", false, null));
        try {
            for (int i = 1; i <= 5; i++) {
                int id = i;
                pool.submit(() -> {
                    log("In order task " + id);
                    sleep(80);
                });
            }
        } finally {
            shutdownAndAwaitTermination(pool);
        }
    }

    // 5) Scheduled thread pool: schedule, fixed-rate, fixed-delay
    private static void exScheduledExecutor() throws InterruptedException {
        section("ScheduledExecutorService: schedule/fixedRate/fixedDelay");
        ScheduledExecutorService sched = Executors.newScheduledThreadPool(2, namedThreadFactory("sched", false, null));
        ScheduledFuture<?> oneShot = null;
        ScheduledFuture<?> rate = null;
        ScheduledFuture<?> delay = null;
        try {
            oneShot = sched.schedule(() -> log("One-shot after 300ms"), 300, TimeUnit.MILLISECONDS);

            rate = sched.scheduleAtFixedRate(() -> log("rate tick"), 0, 200, TimeUnit.MILLISECONDS);
            delay = sched.scheduleWithFixedDelay(() -> {
                log("delay tick (work ~150ms)");
                sleep(150);
            }, 0, 150, TimeUnit.MILLISECONDS);

            Thread.sleep(900); // Let a few ticks happen
        } finally {
            if (rate != null) rate.cancel(false);
            if (delay != null) delay.cancel(false);
            if (oneShot != null) oneShot.cancel(false); // no-op if already done
            shutdownAndAwaitTermination(sched);
        }
    }

    // 6) Callable, Future, results, exceptions
    private static void exCallableAndFuture() {
        section("Callable, Future, results, exceptions");
        ExecutorService pool = Executors.newFixedThreadPool(2, namedThreadFactory("callable", false, null));
        try {
            Callable<Integer> sumTo100 = () -> {
                log("Summing on " + Thread.currentThread().getName());
                int s = 0;
                for (int i = 1; i <= 100; i++) s += i;
                sleep(120);
                return s;
            };
            Future<Integer> f1 = pool.submit(sumTo100);
            log("Sum result = " + getQuiet(f1));

            // submit Runnable with a constant result
            Future<String> f2 = pool.submit(() -> log("Runnable side-effect"), "OK");
            log("Runnable result = " + getQuiet(f2));

            // Exception from Callable is wrapped in ExecutionException
            Future<Void> f3 = pool.submit(() -> {
                sleep(50);
                throw new IllegalStateException("boom");
            });
            try {
                f3.get();
            } catch (ExecutionException e) {
                log("Caught task exception: " + e.getCause());
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        } finally {
            shutdownAndAwaitTermination(pool);
        }
    }

    // 7) invokeAll/invokeAny
    private static void exInvokeAllInvokeAny() throws InterruptedException, ExecutionException, TimeoutException {
        section("invokeAll and invokeAny");
        ExecutorService pool = Executors.newFixedThreadPool(3, namedThreadFactory("invoke", false, null));
        try {
            List<Callable<String>> tasks = List.of(
                namedCallable("fast", 100),
                namedCallable("medium", 250),
                namedCallable("slow", 500)
            );

            // invokeAll waits for all tasks and returns futures in the same order
            List<Future<String>> futures = pool.invokeAll(tasks);
            for (Future<String> f : futures) {
                log("invokeAll got: " + getQuiet(f));
            }

            // invokeAny returns the first successfully completed result
            String fastest = pool.invokeAny(tasks, 1, TimeUnit.SECONDS);
            log("invokeAny fastest: " + fastest);
        } finally {
            shutdownAndAwaitTermination(pool);
        }
    }

    // 8) CompletionService (consume results as they complete)
    private static void exCompletionService() throws InterruptedException, ExecutionException {
        section("ExecutorCompletionService (finish-order results)");
        ExecutorService pool = Executors.newFixedThreadPool(3, namedThreadFactory("ecs", false, null));
        try {
            CompletionService<String> ecs = new ExecutorCompletionService<>(pool);
            for (int i = 1; i <= 5; i++) {
                int id = i;
                ecs.submit(() -> {
                    sleep(100 * id); // stagger
                    return "task-" + id;
                });
            }
            for (int i = 0; i < 5; i++) {
                Future<String> f = ecs.take(); // blocks until any finishes
                log("Completed: " + f.get());
            }
        } finally {
            shutdownAndAwaitTermination(pool);
        }
    }

    // 9) Custom ThreadFactory (names, daemon, uncaught handler)
    private static void exCustomThreadFactory() {
        section("Custom ThreadFactory");
        Thread.UncaughtExceptionHandler ueh = (t, e) -> log("UEH caught from " + t.getName() + ": " + e);
        ExecutorService pool = Executors.newFixedThreadPool(2, namedThreadFactory("custom", true, ueh));
        try {
            // Note: ThreadPoolExecutor catches task exceptions; UEH typically won't see them in pooled tasks.
            pool.execute(() -> log("daemon=" + Thread.currentThread().isDaemon()));
        } finally {
            shutdownAndAwaitTermination(pool);
        }
    }

    // 10) ThreadPoolExecutor tuning + rejection policies
    private static void exCustomThreadPoolAndRejection() {
        section("ThreadPoolExecutor tuning + Rejection");
        // Small bounded queue: capacity 2; core=2; max=2 => total in-flight capacity 4; 5th concurrent submit will be rejected
        ThreadPoolExecutor abortPool = new ThreadPoolExecutor(
            2, 2, 30, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(2),
            namedThreadFactory("abort", false, null),
            new ThreadPoolExecutor.AbortPolicy()
        );
        try {
            List<Future<?>> fs = new ArrayList<>();
            for (int i = 1; i <= 6; i++) {
                int id = i;
                try {
                    fs.add(abortPool.submit(() -> {
                        log("AbortPool running " + id);
                        sleep(300);
                    }));
                } catch (RejectedExecutionException rex) {
                    log("Rejected (AbortPolicy) task " + id + ": " + rex);
                }
            }
            joinAll(fs);
        } finally {
            shutdownAndAwaitTermination(abortPool);
        }

        // CallerRunsPolicy: rejected tasks run in submitting thread (provides backpressure)
        ThreadPoolExecutor callerRunsPool = new ThreadPoolExecutor(
            2, 2, 30, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(2),
            namedThreadFactory("callerRuns", false, null),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        try {
            List<Future<?>> fs = new ArrayList<>();
            for (int i = 1; i <= 6; i++) {
                int id = i;
                fs.add(callerRunsPool.submit(() -> {
                    log("CallerRuns running " + id + " on " + Thread.currentThread().getName());
                    sleep(200);
                }));
            }
            joinAll(fs);
        } finally {
            shutdownAndAwaitTermination(callerRunsPool);
        }
    }

    // 11) Exception handling in pools: afterExecute hook + Future.get
    private static void exExceptionHandlingInPools() {
        section("Exception handling: afterExecute + Future");
        LoggingThreadPool pool = new LoggingThreadPool(
            2, 2, 30, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            namedThreadFactory("logging", false, null),
            new ThreadPoolExecutor.AbortPolicy()
        );
        pool.setRemoveOnCancelPolicy(true); // remove cancelled tasks from queue
        try {
            // Runnable throwing runtime exception (afterExecute will log)
            pool.execute(() -> {
                log("About to throw from Runnable");
                throw new RuntimeException("runnable-failure");
            });

            // Callable exception (afterExecute will surface via Future.get)
            Future<Integer> f = pool.submit(() -> {
                sleep(50);
                throw new IllegalArgumentException("callable-failure");
            });

            // Optionally also handle via explicit Future.get
            try {
                f.get();
            } catch (ExecutionException e) {
                log("Caught via Future.get: " + e.getCause());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            sleep(150); // allow afterExecute logging to run
        } finally {
            shutdownAndAwaitTermination(pool);
        }
    }

    // 12) Timeouts and cancellation (interrupts)
    private static void exTimeoutAndCancellation() {
        section("Timeouts and cancellation");
        ExecutorService pool = Executors.newFixedThreadPool(1, namedThreadFactory("timeout", false, null));
        try {
            Future<String> f = pool.submit(interruptibleTask(2000));
            try {
                String result = f.get(250, TimeUnit.MILLISECONDS);
                log("Got: " + result);
            } catch (TimeoutException te) {
                log("Timed out; cancelling...");
                f.cancel(true); // true -> interrupt running task
            }
            sleep(300); // let task observe interrupt
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException ignored) {
        } finally {
            shutdownAndAwaitTermination(pool);
        }
    }

    // 13) Work-stealing pool (ForkJoinPool)
    private static void exWorkStealingPool() throws InterruptedException, ExecutionException {
        section("Work-stealing pool");
        ExecutorService pool = Executors.newWorkStealingPool(); // size = number of processors
        // Work-stealing uses daemon workers; wait for results to keep JVM alive
        List<Callable<String>> work = List.of(
            namedCallable("ws-1", 120),
            namedCallable("ws-2", 240),
            namedCallable("ws-3", 180),
            namedCallable("ws-4", 60)
        );
        List<Future<String>> futures = pool.invokeAll(work);
        for (Future<String> f : futures) log("WS done: " + f.get());
        pool.shutdown(); // ForkJoinPool.commonPool would live; here we close this pool
    }

    // 14) Virtual threads (Java 21+) via reflection to avoid compile-time dependency
    private static void exVirtualThreadsIfAvailable() throws Exception {
        section("Virtual threads (Java 21+) if available");
        try {
            // Reflectively call Executors.newVirtualThreadPerTaskExecutor()
            var m = Executors.class.getMethod("newVirtualThreadPerTaskExecutor");
            Object execObj = m.invoke(null);
            ExecutorService vexec = (ExecutorService) execObj;
            try (vexec) { // AutoCloseable since Java 21
                List<Future<?>> fs = new ArrayList<>();
                for (int i = 1; i <= 5; i++) {
                    int id = i;
                    fs.add(vexec.submit(() -> {
                        log("vtask " + id + " on " + Thread.currentThread());
                        sleep(100);
                    }));
                }
                joinAll(fs);
            }
        } catch (NoSuchMethodException nsme) {
            log("Virtual threads not available on this JDK");
        }
    }

    // 15) CompletableFuture with custom executor
    private static void exCompletableFutureWithCustomExecutor() throws Exception {
        section("CompletableFuture with custom Executor");
        ExecutorService pool = Executors.newFixedThreadPool(3, namedThreadFactory("cf", false, null));
        try {
            CompletableFuture<Integer> cf = CompletableFuture
                .supplyAsync(() -> {
                    log("CF compute");
                    sleep(100);
                    return 21;
                }, pool)
                .thenApplyAsync(x -> {
                    log("CF thenApply on " + Thread.currentThread().getName());
                    return x * 2;
                }, pool);

            log("CF result = " + cf.get(1, TimeUnit.SECONDS));
        } finally {
            shutdownAndAwaitTermination(pool);
        }
    }

    // Utilities

    private static Callable<String> namedCallable(String name, long workMs) {
        return () -> {
            log("Start " + name);
            sleep(workMs);
            log("End " + name);
            return name;
        };
    }

    private static Callable<String> interruptibleTask(long workMs) {
        return () -> {
            long end = System.currentTimeMillis() + workMs;
            while (System.currentTimeMillis() < end) {
                if (Thread.currentThread().isInterrupted()) {
                    log("Task observed interrupt; cleaning up");
                    return "cancelled";
                }
                Thread.sleep(50);
            }
            return "done";
        };
    }

    private static void joinAll(List<Future<?>> futures) {
        for (Future<?> f : futures) {
            try {
                f.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (ExecutionException ignored) {
            }
        }
    }

    // Proper shutdown pattern (from Java docs) with small timeouts
    private static void shutdownAndAwaitTermination(ExecutorService pool) {
        if (pool == null) return;
        pool.shutdown(); // Disable new tasks
        try {
            if (!pool.awaitTermination(2, TimeUnit.SECONDS)) {
                pool.shutdownNow(); // Cancel currently executing tasks
                if (!pool.awaitTermination(2, TimeUnit.SECONDS)) {
                    log("Pool did not terminate");
                }
            }
        } catch (InterruptedException ie) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private static <T> T getQuiet(Future<T> f) {
        try {
            return f.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }

    private static void log(String msg) {
        System.out.printf("[%s] [%s] %s%n",
            LocalTime.now().withNano(0),
            Thread.currentThread().getName(),
            msg
        );
    }

    // Custom Named ThreadFactory
    private static ThreadFactory namedThreadFactory(String prefix, boolean daemon, Thread.UncaughtExceptionHandler ueh) {
        Objects.requireNonNull(prefix);
        AtomicInteger seq = new AtomicInteger(1);
        return r -> {
            Thread t = new Thread(r, prefix + "-" + seq.getAndIncrement());
            t.setDaemon(daemon);
            if (ueh != null) t.setUncaughtExceptionHandler(ueh);
            return t;
        };
    }

    // ThreadPool with afterExecute for exception logging
    private static class LoggingThreadPool extends ThreadPoolExecutor {
        LoggingThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                          BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory,
                          RejectedExecutionHandler handler) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
        }

        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            super.afterExecute(r, t);
            // If t is null, try to extract from Future if the task was a Callable
            if (t == null && r instanceof Future<?>) {
                try {
                    ((Future<?>) r).get();
                } catch (CancellationException ce) {
                    t = ce;
                } catch (ExecutionException ee) {
                    t = ee.getCause();
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
            if (t != null) {
                log("afterExecute captured exception: " + t);
            }
        }
    }
}