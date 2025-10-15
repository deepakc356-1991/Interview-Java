package _08_03_executors_and_thread_pools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 Executors & Thread Pools — Theory, Design, and Practical Usage

 0) Why Executors?
    - Creating a new Thread per task is costly (stack, scheduling, GC pressure), hard to manage (lifecycle, errors), and limits throughput.
    - Executors decouple "task submission" (Runnable/Callable) from "thread management" (creation, reuse, scheduling).
    - Thread pools amortize thread creation cost and provide queuing, throttling, scheduling, and instrumentation.

 1) Core Terminology
    - Task: A unit of work. Runnable (no result), Callable<V> (produces V or throws).
    - Executor: Functional interface with void execute(Runnable).
    - ExecutorService: Extends Executor; adds submit(..), invokeAll/Any(..), shutdown(), Future, etc.
    - ScheduledExecutorService: Adds delayed and periodic scheduling.
    - Future<V>: Represents pending result of an asynchronous computation (get, cancel, isDone).
    - ThreadPoolExecutor: Flexible, tunable thread pool with queue and saturation policy.
    - ScheduledThreadPoolExecutor: Periodic scheduling on top of ThreadPoolExecutor.
    - ForkJoinPool / Work-stealing: Parallelism via work-stealing for fine-grained tasks.

 2) Executors factory methods (java.util.concurrent.Executors)
    - newFixedThreadPool(n): Bound threads (n), unbounded queue (LinkedBlockingQueue). Risk: unbounded queue may grow large.
    - newCachedThreadPool(): Unbounded threads (Integer.MAX_VALUE) with SynchronousQueue (direct handoff). Risk: thread explosion.
    - newSingleThreadExecutor(): One worker, FIFO order, unbounded queue.
    - newScheduledThreadPool(n): For delayed/periodic tasks; threads = n.
    - newWorkStealingPool(): Uses ForkJoinPool; parallelism based on CPU cores; tasks should be small, non-blocking.
    - Note: Prefer constructing ThreadPoolExecutor directly for production to control queue and rejection policy.

 3) ThreadPoolExecutor — core parameters
    - corePoolSize: Minimum threads kept alive (unless allowCoreThreadTimeOut(true)).
    - maximumPoolSize: Upper bound of threads.
    - keepAliveTime: Idle timeout for non-core threads (or core if allowCoreThreadTimeOut).
    - workQueue: Task queue (ArrayBlockingQueue, LinkedBlockingQueue, SynchronousQueue, PriorityBlockingQueue).
    - threadFactory: Naming, daemon, priority, UncaughtExceptionHandler.
    - handler (RejectedExecutionHandler): Strategy when saturated.

    Thread creation/queuing algorithm (simplified):
      - If pool size < corePoolSize: create new thread.
      - Else try to enqueue. If queue full and pool size < maximumPoolSize: create new thread.
      - Else reject via RejectedExecutionHandler.

 4) Work queues
    - SynchronousQueue: No capacity; each insert waits for a remove. Favors thread growth up to maximum.
    - ArrayBlockingQueue: Bounded; good backpressure; predictable memory.
    - LinkedBlockingQueue: Potentially unbounded (if default ctor); risk of OOM under load.
    - PriorityBlockingQueue: Ordered tasks; use with care (non-FIFO).

 5) Rejection policies (RejectedExecutionHandler)
    - AbortPolicy (default): throws RejectedExecutionException.
    - CallerRunsPolicy: caller runs the task (slows producer → backpressure).
    - DiscardPolicy: silently drops the task.
    - DiscardOldestPolicy: drops oldest queued, retries insertion.

 6) Lifecycle and shutdown
    - shutdown(): Stop accepting new tasks; complete queued tasks.
    - shutdownNow(): Attempts to cancel queued tasks and interrupt workers; returns tasks that never commenced.
    - awaitTermination(timeout): Await pool termination.
    - States: RUNNING → SHUTDOWN → STOP → TIDYING → TERMINATED.

 7) Exceptions from tasks
    - execute(Runnable): Uncaught exceptions go to thread's UncaughtExceptionHandler (or default).
    - submit(Callable/Runnable): Exceptions captured inside Future; not thrown to handler; retrieve via Future.get().
    - afterExecute hook: ThreadPoolExecutor.afterExecute(Runnable, Throwable) to centralize logging/error handling.

 8) Cancellation & interrupts
    - Future.cancel(true) interrupts the executing thread; task must be interruptible (sleep, wait, blocking I/O, or check Thread.interrupted()).
    - Tasks should handle InterruptedException properly and exit promptly.

 9) Scheduling
    - schedule(Runnable/Callable, delay).
    - scheduleAtFixedRate(initialDelay, period): Attempts to keep a constant rate; if a run overruns, next runs may happen immediately (catching up).
    - scheduleWithFixedDelay(initialDelay, delay): Delay measured from end of previous run; no catch-up (natural for variable-length tasks).
    - Periodic tasks ignore returned value; thrown exceptions cancel that repeating task.

 10) Sizing a thread pool (rules of thumb)
    - CPU-bound: ~ number of cores (Runtime.getRuntime().availableProcessors()).
    - I/O-bound or blocking: cores × (1 + waitTime/computeTime). Measure and tune.
    - Favor bounded queues and backpressure to avoid OOM and latency spikes.

 11) Observability and tuning
    - ThreadPoolExecutor metrics: getPoolSize, getActiveCount, getQueue().size, getTaskCount, getCompletedTaskCount, getLargestPoolSize.
    - Name threads; add logging hooks; monitor saturation and rejections; instrument with JMX or metrics.

 12) Virtual threads (Project Loom; Java 21+)
    - Executors.newVirtualThreadPerTaskExecutor() offers thread-per-task without pool exhaustion; excellent for blocking I/O.
    - Still use Executor/ExecutorService APIs; but many thread-pool tuning concerns reduce. Use where available.

 13) Common pitfalls
    - Unbounded queues (Fixed/Single from Executors) combined with slow consumers → memory bloat.
    - Cached thread pool + blocking tasks → thread explosion.
    - Ignoring interrupts, not shutting down executors.
    - Periodic tasks that throw exceptions silently cancel.
    - Using default thread names and no logging → hard to debug.

 Below are concise examples illustrating these concepts. They are short-lived and self-terminating for safe execution.
*/
public class _01_Theory {

    public static void main(String[] args) throws Exception {
        demoFixedThreadPool();
        demoSubmitAndFuture();
        demoExceptionHandling();
        demoCustomThreadPoolExecutor();
        demoScheduledExecutor();
        demoInvokeAllAndInvokeAny();
        demoHooks();
        demoWorkStealingPool();
        demoShutdownPattern(); // illustrates the canonical shutdown sequence
        // Note: Virtual threads example is described in comments for portability (JDK 8+ compilation).
    }

    // ---------------------- Utilities ----------------------

    private static void sleepMs(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt(); // propagate interrupt
        }
    }

    private static Callable<Integer> fib(int n) {
        return () -> {
            // naive small fib for demo
            int a = 0, b = 1;
            for (int i = 0; i < n; i++) {
                int t = a + b;
                a = b;
                b = t;
                // tiny spin to mimic work
                if ((i & 1023) == 0) Thread.yield();
            }
            return a;
        };
    }

    // ---------------------- ThreadFactory ----------------------

    /**
     Custom ThreadFactory: names, daemon flag, and UncaughtExceptionHandler.
     Naming threads makes debugging much easier. Daemon threads don't block JVM exit.
    */
    static class NamedThreadFactory implements ThreadFactory {
        private final AtomicInteger seq = new AtomicInteger();
        private final String prefix;
        private final boolean daemon;
        private final Thread.UncaughtExceptionHandler handler;

        NamedThreadFactory(String prefix, boolean daemon) {
            this(prefix, daemon, (t, e) -> {
                System.err.println("Uncaught in " + t.getName() + ": " + e);
                e.printStackTrace(System.err);
            });
        }

        NamedThreadFactory(String prefix, boolean daemon, Thread.UncaughtExceptionHandler handler) {
            this.prefix = Objects.requireNonNull(prefix);
            this.daemon = daemon;
            this.handler = Objects.requireNonNull(handler);
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, prefix + "-" + seq.incrementAndGet());
            t.setDaemon(daemon);
            t.setUncaughtExceptionHandler(handler);
            return t;
        }
    }

    // ---------------------- Rejection Handler ----------------------

    static class CountingRejectedExecutionHandler implements RejectedExecutionHandler {
        private final AtomicInteger rejections = new AtomicInteger();

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            int n = rejections.incrementAndGet();
            System.err.println("Task rejected (" + n + ") by " + executor + " for " + r);
            // This example uses AbortPolicy semantics: explicitly throw
            throw new RejectedExecutionException("Rejected task #" + n);
        }

        int getRejections() {
            return rejections.get();
        }
    }

    // ---------------------- Hookable Executor ----------------------

    /**
     Subclass to instrument before/after execution and exception handling in one place.
    */
    static class LoggingThreadPoolExecutor extends ThreadPoolExecutor {
        LoggingThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                                  BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory,
                                  RejectedExecutionHandler handler) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
        }

        @Override
        protected void beforeExecute(Thread t, Runnable r) {
            super.beforeExecute(t, r);
            System.out.println("beforeExecute: " + r + " on " + t.getName());
        }

        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            super.afterExecute(r, t);
            // Note: t may be null; for tasks submitted via submit(), exceptions are stored in Future.
            Throwable failure = t;
            if (failure == null && r instanceof Future<?> f) {
                try {
                    if (f.isDone()) f.get(); // will throw if task failed
                } catch (CancellationException | InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ee) {
                    failure = ee.getCause();
                }
            }
            if (failure != null) {
                System.err.println("afterExecute caught: " + failure + " from " + r);
            } else {
                System.out.println("afterExecute: " + r + " completed successfully");
            }
        }

        @Override
        protected void terminated() {
            super.terminated();
            System.out.println("Executor terminated. Completed tasks = " + getCompletedTaskCount());
        }
    }

    // ---------------------- Demos ----------------------

    /**
     Fixed thread pool: bounded threads, unbounded queue by default in Executors.newFixedThreadPool.
     Caution: Unbounded LinkedBlockingQueue can grow large under load.
    */
    private static void demoFixedThreadPool() throws InterruptedException {
        System.out.println("\n--- demoFixedThreadPool ---");
        ExecutorService pool = Executors.newFixedThreadPool(
                2,
                new NamedThreadFactory("fixed", false)
        );

        Runnable task = () -> {
            System.out.println("Running on " + Thread.currentThread().getName());
            sleepMs(150);
        };

        for (int i = 0; i < 5; i++) {
            pool.execute(task);
        }

        pool.shutdown();
        boolean done = pool.awaitTermination(2, TimeUnit.SECONDS);
        System.out.println("Fixed pool terminated: " + done);
    }

    /**
     Submit and Future: get results, timeouts, and cancellation (interrupt).
    */
    private static void demoSubmitAndFuture() throws Exception {
        System.out.println("\n--- demoSubmitAndFuture ---");
        ExecutorService pool = Executors.newCachedThreadPool(new NamedThreadFactory("cached", true));

        Future<Integer> f1 = pool.submit(fib(20)); // small
        Future<Integer> f2 = pool.submit(() -> {
            System.out.println("Callable on " + Thread.currentThread().getName());
            sleepMs(100);
            return 42;
        });

        System.out.println("f1 = " + f1.get(500, TimeUnit.MILLISECONDS));
        System.out.println("f2 = " + f2.get());

        Future<?> longTask = pool.submit(() -> {
            try {
                System.out.println("Long task started, interruptible sleep...");
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                System.out.println("Long task was interrupted and will end.");
                Thread.currentThread().interrupt();
            }
        });

        sleepMs(100); // let it start
        boolean cancelled = longTask.cancel(true); // sends interrupt
        System.out.println("Long task cancelled: " + cancelled);

        pool.shutdown();
        pool.awaitTermination(2, TimeUnit.SECONDS);
    }

    /**
     Exception handling:
     - execute(): exception goes to thread's UncaughtExceptionHandler (visible in logs).
     - submit(): exception captured, only visible when calling Future.get().
    */
    private static void demoExceptionHandling() throws Exception {
        System.out.println("\n--- demoExceptionHandling ---");
        ExecutorService pool = Executors.newFixedThreadPool(
                1,
                new NamedThreadFactory("ex", false, (t, e) -> {
                    System.err.println("Custom handler: " + t.getName() + " -> " + e);
                })
        );

        // Using execute: thrown exception hits UncaughtExceptionHandler immediately
        pool.execute(() -> {
            System.out.println("execute() task throwing...");
            throw new RuntimeException("Boom from execute");
        });

        // Using submit: exception is captured; not thrown to UncaughtExceptionHandler
        Future<?> f = pool.submit(() -> {
            System.out.println("submit() task throwing...");
            throw new RuntimeException("Boom from submit");
        });

        try {
            f.get(); // raises ExecutionException
        } catch (ExecutionException ee) {
            System.err.println("Caught via Future.get(): " + ee.getCause());
        }

        pool.shutdown();
        pool.awaitTermination(1, TimeUnit.SECONDS);
    }

    /**
     Custom ThreadPoolExecutor with:
     - Bounded queue (ArrayBlockingQueue) for backpressure
     - CallerRunsPolicy to slow producer on saturation
     - Core prestart and core thread timeout
     Shows pool metrics and rejection/backpressure behavior.
    */
    private static void demoCustomThreadPoolExecutor() throws InterruptedException {
        System.out.println("\n--- demoCustomThreadPoolExecutor ---");
        BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(2);
        ThreadFactory tf = new NamedThreadFactory("custom", false);
        RejectedExecutionHandler handler = new ThreadPoolExecutor.CallerRunsPolicy();
        ThreadPoolExecutor exec = new ThreadPoolExecutor(
                2, // core
                4, // max
                500, TimeUnit.MILLISECONDS, // keepAlive
                queue,
                tf,
                handler
        );
        exec.allowCoreThreadTimeOut(true);
        exec.prestartAllCoreThreads();

        Runnable shortTask = () -> {
            sleepMs(120);
            System.out.println("shortTask on " + Thread.currentThread().getName());
        };

        // Submit more tasks than core + queue to trigger scaling and possibly CallerRuns
        for (int i = 0; i < 10; i++) {
            exec.execute(shortTask);
            System.out.printf("Submitted %d, poolSize=%d, active=%d, queued=%d%n",
                    i + 1, exec.getPoolSize(), exec.getActiveCount(), exec.getQueue().size());
        }

        exec.shutdown();
        exec.awaitTermination(3, TimeUnit.SECONDS);
        System.out.printf("After termination: completed=%d, largestPoolSize=%d%n",
                exec.getCompletedTaskCount(), exec.getLargestPoolSize());

        // Observe core timeout by letting time pass (no tasks)
        // Note: pool is shut down; demonstration of allowCoreThreadTimeOut is conceptual above.
    }

    /**
     Scheduling: schedule, scheduleAtFixedRate vs scheduleWithFixedDelay.
    */
    private static void demoScheduledExecutor() throws InterruptedException {
        System.out.println("\n--- demoScheduledExecutor ---");
        ScheduledExecutorService ses = Executors.newScheduledThreadPool(1, new NamedThreadFactory("sched", true));

        CountDownLatch latch = new CountDownLatch(1);
        ses.schedule(() -> {
            System.out.println("One-shot delayed task on " + Thread.currentThread().getName());
            latch.countDown();
        }, 100, TimeUnit.MILLISECONDS);
        latch.await(1, TimeUnit.SECONDS);

        AtomicInteger runs = new AtomicInteger();
        final ScheduledFuture<?>[] handle = new ScheduledFuture<?>[1];
        handle[0] = ses.scheduleAtFixedRate(() -> {
            int n = runs.incrementAndGet();
            System.out.println("fixed-rate run #" + n + " at " + System.nanoTime());
            // Simulate work that takes ~80ms; with a 100ms period, slight drift may accumulate
            sleepMs(80);
            if (n >= 3) {
                System.out.println("Cancelling fixed-rate");
                handle[0].cancel(false);
            }
        }, 0, 100, TimeUnit.MILLISECONDS);

        // Fixed-delay example
        AtomicInteger delayRuns = new AtomicInteger();
        final ScheduledFuture<?>[] handle2 = new ScheduledFuture<?>[1];
        handle2[0] = ses.scheduleWithFixedDelay(() -> {
            int n = delayRuns.incrementAndGet();
            long start = System.nanoTime();
            System.out.println("fixed-delay run #" + n + " start=" + start);
            sleepMs(80);
            if (n >= 3) {
                System.out.println("Cancelling fixed-delay");
                handle2[0].cancel(false);
            }
        }, 0, 100, TimeUnit.MILLISECONDS);

        // Give time for runs
        sleepMs(1000);

        ses.shutdown();
        ses.awaitTermination(1, TimeUnit.SECONDS);
    }

    /**
     invokeAll and invokeAny: batch submission and coordination.
    */
    private static void demoInvokeAllAndInvokeAny() throws InterruptedException, ExecutionException, TimeoutException {
        System.out.println("\n--- demoInvokeAllAndInvokeAny ---");
        ExecutorService pool = Executors.newFixedThreadPool(3, new NamedThreadFactory("batch", true));

        List<Callable<String>> tasks = Arrays.asList(
                () -> {
                    sleepMs(150);
                    return "A";
                },
                () -> {
                    sleepMs(50);
                    return "B";
                },
                () -> {
                    sleepMs(100);
                    return "C";
                }
        );

        // invokeAll waits for all, returns Futures in same order as input
        List<Future<String>> results = pool.invokeAll(tasks, 500, TimeUnit.MILLISECONDS);
        for (int i = 0; i < results.size(); i++) {
            Future<String> f = results.get(i);
            System.out.println("invokeAll result[" + i + "] done=" + f.isDone() + " cancelled=" + f.isCancelled() +
                    (f.isDone() && !f.isCancelled() ? " value=" + f.get() : ""));
        }

        // invokeAny returns as soon as the first completes; cancels others
        String fastest = pool.invokeAny(tasks, 500, TimeUnit.MILLISECONDS);
        System.out.println("invokeAny fastest = " + fastest);

        pool.shutdown();
        pool.awaitTermination(1, TimeUnit.SECONDS);
    }

    /**
     Demonstrates beforeExecute/afterExecute hooks via a custom ThreadPoolExecutor subclass.
    */
    private static void demoHooks() throws InterruptedException {
        System.out.println("\n--- demoHooks ---");
        LoggingThreadPoolExecutor exec = new LoggingThreadPoolExecutor(
                2, 2, 0, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                new NamedThreadFactory("hook", false),
                new ThreadPoolExecutor.AbortPolicy()
        );

        exec.execute(() -> {
            System.out.println("hook task 1");
            sleepMs(50);
        });
        exec.submit(() -> {
            System.out.println("hook task 2 (throws)");
            throw new IllegalStateException("from hook");
        });

        exec.shutdown();
        exec.awaitTermination(1, TimeUnit.SECONDS);
    }

    /**
     Work-stealing pool: good for many small CPU-bound tasks.
     Internally uses ForkJoinPool; tasks should avoid long blocking.
    */
    private static void demoWorkStealingPool() throws InterruptedException, ExecutionException {
        System.out.println("\n--- demoWorkStealingPool ---");
        ExecutorService pool = Executors.newWorkStealingPool(); // parallelism ~= available processors
        // Note: Work-stealing pools use daemon threads; ensure program waits for results
        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 18; i <= 22; i++) {
            int n = i;
            tasks.add(fib(n));
        }
        List<Future<Integer>> futures = pool.invokeAll(tasks);
        for (int i = 0; i < futures.size(); i++) {
            System.out.println("fib(" + (18 + i) + ") = " + futures.get(i).get());
        }
        pool.shutdown();
        pool.awaitTermination(1, TimeUnit.SECONDS);
    }

    /**
     Canonical shutdown pattern: graceful then forced.
     Use this approach in your application stop hooks.
    */
    private static void demoShutdownPattern() {
        System.out.println("\n--- demoShutdownPattern ---");
        ExecutorService exec = Executors.newFixedThreadPool(2, new NamedThreadFactory("shutdown", false));

        for (int i = 0; i < 3; i++) {
            exec.execute(() -> {
                try {
                    System.out.println("Shutdown demo running on " + Thread.currentThread().getName());
                    Thread.sleep(150);
                } catch (InterruptedException e) {
                    System.out.println("Shutdown demo interrupted");
                    Thread.currentThread().interrupt();
                }
            });
        }

        // Graceful shutdown
        exec.shutdown();
        try {
            if (!exec.awaitTermination(1, TimeUnit.SECONDS)) {
                List<Runnable> dropped = exec.shutdownNow(); // cancel queued tasks, interrupt running
                System.out.println("Forced shutdown; dropped tasks = " + dropped.size());
                if (!exec.awaitTermination(1, TimeUnit.SECONDS)) {
                    System.err.println("Pool did not terminate in time");
                }
            }
        } catch (InterruptedException ie) {
            exec.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("Shutdown pattern complete. isTerminated=" + exec.isTerminated());
    }

    // ---------------------- Additional Notes ----------------------
    // Virtual threads (Java 21+):
    // try (ExecutorService vExec = Executors.newVirtualThreadPerTaskExecutor()) {
    //     Future<Integer> f = vExec.submit(() -> {
    //         // Blocking I/O friendly, lightweight
    //         Thread.sleep(100);
    //         return 123;
    //     });
    //     System.out.println(f.get());
    // }
    // This compiles only on Java 21+. Kept commented for JDK 8+ compatibility.

    // Unbounded vs bounded:
    // - newFixedThreadPool uses unbounded LinkedBlockingQueue -> tasks queue up if slower than producers.
    //   Prefer constructing ThreadPoolExecutor with a bounded queue for production workloads.
    //
    // Rejections and backpressure:
    // - CallerRunsPolicy makes submitter execute tasks when saturated, slowing producers naturally.
    //
    // Exceptions:
    // - submit() swallows exceptions until get(); use logs or afterExecute to centralize error handling.
    //
    // Scheduling pitfalls:
    // - scheduleAtFixedRate can "bunch up" if execution overruns. Use scheduleWithFixedDelay for predictable gaps.
    //
    // Pool sizing:
    // - Start with CPUs for CPU-bound; multiply for blocking based on measured wait/compute ratio; validate with metrics.
}