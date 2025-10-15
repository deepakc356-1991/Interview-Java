package _08_03_executors_and_thread_pools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/*
Executors & Thread Pools Interview Q&A (basic → intermediate → advanced)

1) What is Executor vs Thread?
- Thread is a concrete OS-level execution unit.
- Executor is an abstraction for submitting tasks (Runnable/Callable) for asynchronous execution, decoupling task submission from execution policy.

2) Executor vs ExecutorService vs ScheduledExecutorService?
- Executor: execute(Runnable).
- ExecutorService: adds lifecycle (shutdown, awaitTermination) and task-returning submit methods with Future, plus invokeAll/invokeAny.
- ScheduledExecutorService: schedules tasks with delays/periodic execution.

3) Why prefer Executors over manual threads?
- Reuse threads to cut creation cost.
- Apply policies: queueing, bounding, rejection, timeouts, naming, monitoring.
- Centralized lifecycle control and structured cancellation.

4) Runnable vs Callable?
- Runnable: no result, cannot throw checked exceptions.
- Callable<V>: returns V and may throw checked exceptions. submit returns Future<V>.

5) execute vs submit?
- execute: fire-and-forget; no Future; exceptions from tasks are internally handled by the pool (they do not reach uncaught exception handler by default).
- submit: returns Future; exceptions are captured and rethrown from Future.get() wrapped in ExecutionException.

6) Future basics: get, cancel, timeouts
- get(): blocks for completion; throws ExecutionException, InterruptedException, CancellationException.
- get(timeout): bounded wait.
- cancel(true): attempts to interrupt running task; tasks must cooperate (check interrupts).
- cancel(false): cancels if not started or sets canceled state.

7) ThreadPoolExecutor anatomy (core concepts)
- corePoolSize: threads kept even if idle (unless allowCoreThreadTimeOut true).
- maximumPoolSize: upper bound on threads.
- keepAliveTime: idle time before non-core (and optionally core) threads die.
- workQueue: where tasks wait (ArrayBlockingQueue, LinkedBlockingQueue, SynchronousQueue, PriorityBlockingQueue).
- threadFactory: customize name, daemon, priority, uncaught handler.
- handler (RejectedExecutionHandler): what to do when saturated.
- Task admission flow:
  a) If running threads < core → create thread.
  b) Else try to enqueue.
  c) If queue full and threads < max → create thread.
  d) Else reject.

8) Common Executors factory methods (and when to use)
- newFixedThreadPool(n): fixed size; beware unbounded LinkedBlockingQueue OOM risk under load; prefer custom bounded queue.
- newCachedThreadPool(): unbounded threads + SynchronousQueue; can explode; good for many short-lived I/O if upstream is bounded.
- newSingleThreadExecutor(): single worker with queue; tasks run in order; consider scheduled single for periodic work.
- newScheduledThreadPool(n): scheduleAtFixedRate/WithFixedDelay.
- newWorkStealingPool(): ForkJoinPool with work-stealing; daemon threads; good for many small CPU tasks; avoid blocking inside.

9) Preferred creation style
- Prefer explicit ThreadPoolExecutor with bounded queue, named ThreadFactory, and explicit RejectedExecutionHandler over Executors helpers, for backpressure and observability.

10) RejectedExecutionHandler strategies
- AbortPolicy: throws RejectedExecutionException to caller.
- CallerRunsPolicy: runs task in calling thread (natural backpressure).
- DiscardPolicy: drops tasks silently.
- DiscardOldestPolicy: drops oldest queued task and retries.
- Custom: log, meter, enqueue to alternate, etc.

11) Queue choices
- ArrayBlockingQueue: bounded; optional fairness.
- LinkedBlockingQueue: optionally bounded; default unbounded when using factory newFixedThreadPool (danger).
- SynchronousQueue: zero-capacity handoff; pairs producers with workers; drives thread growth.
- PriorityBlockingQueue: priority tasks; tasks must be Comparable or queue must have Comparator.

12) Exception handling in pooled tasks
- submit swallows in task, rethrows on Future.get.
- execute: still caught by ThreadPoolExecutor; use afterExecute hook to log or wrap tasks yourself.
- UncaughtExceptionHandler set on thread is usually not invoked for task exceptions in ThreadPoolExecutor; it triggers if something escapes worker loop.

13) Shutdown lifecycle
- shutdown(): stop accepting new tasks; run queued tasks; then exit.
- shutdownNow(): attempts to cancel queued tasks and interrupt workers; returns list of never-started tasks.
- awaitTermination(timeout): wait for termination.
- For ScheduledThreadPoolExecutor, periodic/delayed behavior after shutdown is configurable via setExecuteExistingDelayedTasksAfterShutdownPolicy and setContinueExistingPeriodicTasksAfterShutdownPolicy; be explicit.

14) Scheduled: fixed-rate vs fixed-delay
- scheduleAtFixedRate: aims for regular ticks based on start time; if a run overruns period, next runs may happen back-to-back to catch up.
- scheduleWithFixedDelay: next run scheduled after the previous run completes; no catch-up.

15) Deadlock pitfalls inside pools
- Submitting a task to the same limited pool and blocking waiting for its result can deadlock (task cannot start because all workers are blocked).
- Avoid blocking in pool on tasks submitted to that same pool; use separate pool, increase size, or non-blocking completion (CompletionService/CompletableFuture).

16) Sizing a thread pool
- CPU-bound: around number of cores (Ncpu) or Ncpu ± small; measure.
- I/O-bound: larger; rule-of-thumb N = Ncpu * Ucpu * (1 + wait/compute), where Ucpu is target utilization (e.g., 0.8).
- Measure with production-like workload; consider GC and blocking profiles.

17) Scaling and idleness
- prestartAllCoreThreads(): warm the pool to reduce first-request latency.
- allowCoreThreadTimeOut(true): let core threads die when idle to save resources on bursty systems.

18) CompletionService pattern
- Retrieve results as tasks complete instead of in submission order; great for scatter-gather.

19) invokeAll and invokeAny
- invokeAll: submit a collection and wait for all; optional timeout cancels unfinished.
- invokeAny: returns first successful result; cancels others.

20) Backpressure strategies
- Bounded queue + CallerRuns to slow producers.
- SynchronousQueue + bounded max threads, with strict rejection and upstream retry/backoff.
- Combine with timeouts and rate-limiting.

21) Task cancellation and cooperative interrupt
- Always check Thread.currentThread().isInterrupted and handle InterruptedException.
- Clean up and restore interrupt status if catching InterruptedException.

22) Work-stealing (ForkJoinPool)
- Optimized for many small CPU tasks; tasks steal from each other’s deques; not great for long blocking; if blocking, use ForkJoinPool.ManagedBlocker or a dedicated IO pool.

23) Removing canceled tasks from queue
- ThreadPoolExecutor.purge() removes canceled tasks from workQueue.
- ScheduledThreadPoolExecutor.setRemoveOnCancelPolicy(true) to auto-purge.

24) Context propagation and ThreadLocal pitfalls
- ThreadLocals live with threads; with pooling, stale values can leak across requests; always remove() in finally.
- InheritableThreadLocal is not a fix for pools; prefer explicit context passing or libraries supporting context propagation.

25) Monitoring and diagnostics
- Expose pool stats (active count, queue size, completed, largest).
- Name threads.
- Use timeouts, log rejections, and periodically dump pool stats.
- Watch out for unbounded queues and memory pressure.

26) Virtual threads (Java 21+)
- Executors.newVirtualThreadPerTaskExecutor: millions of cheap threads; simplifies concurrency; still use timeouts, cancellation, and structured concurrency; do not block on synchronized monitors for long.
- Keep this in mind when targeting a modern JDK; shown below in comments.

27) When to prefer ScheduledExecutorService over Timer
- Timer has a single thread by default and can be derailed by one task; ScheduledExecutorService is more robust and supports multiple workers and better error handling.

Below are minimal demos illustrating key points. Keep runtimes short and resources cleaned up.
*/
public class _03_InterviewQA {

    public static void main(String[] args) throws Exception {
        System.out.println("Executors & Thread Pools Interview Q&A - demos");
        demoBasics();
        demoSubmitVsExecuteAndExceptions();
        demoCallerRunsBackpressure();
        demoCompletionService();
        demoInvokeAnyAndAll();
        demoCancellationAndInterruption();
        demoScheduledRateVsDelay();
        demoCustomRejectedHandler();
        demoWorkStealingPool();

        // Bonus: Deadlock pattern example (do not run). See method for details.
        // demoDeadlockPattern(); // intentionally not invoked

        // Virtual threads (Java 21+) example in comments below.

        System.out.println("Done.");
    }

    // Demo: Build a bounded ThreadPoolExecutor with naming, prestart, and timeouts
    static void demoBasics() throws Exception {
        System.out.println("\nDemo: basics (bounded pool, futures, timeouts)");
        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                2, 4,
                10, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(8),
                namedFactory("basics", false, null),
                new ThreadPoolExecutor.AbortPolicy()
        );
        pool.prestartAllCoreThreads();
        pool.allowCoreThreadTimeOut(true);

        List<Future<String>> futures = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            int id = i;
            futures.add(pool.submit(() -> {
                Thread.sleep(80 + id * 10L);
                return "task-" + id + " run by " + Thread.currentThread().getName();
            }));
        }
        for (Future<String> f : futures) {
            System.out.println(f.get(1, TimeUnit.SECONDS));
        }
        printPoolStats(pool, "basics");
        pool.shutdown();
        pool.awaitTermination(2, TimeUnit.SECONDS);
    }

    // Demo: Difference between execute and submit when exceptions occur
    static void demoSubmitVsExecuteAndExceptions() throws Exception {
        System.out.println("\nDemo: submit vs execute and exceptions");
        LoggingThreadPool pool = new LoggingThreadPool(
                1, 1, 0, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                namedFactory("exc", false, (t, e) -> System.err.println("Uncaught in " + t.getName() + ": " + e)),
                new ThreadPoolExecutor.AbortPolicy()
        );

        // execute: no Future; exception handled by pool; we log it via afterExecute
        pool.execute(() -> { throw new RuntimeException("boom via execute"); });

        // submit: exception captured in Future; rethrown from get as ExecutionException
        Future<?> f = pool.submit(() -> { throw new RuntimeException("boom via submit"); });
        try {
            f.get(1, TimeUnit.SECONDS);
        } catch (ExecutionException ee) {
            System.out.println("Future captured: " + ee.getCause());
        }
        pool.shutdown();
        pool.awaitTermination(1, TimeUnit.SECONDS);
    }

    // Demo: Backpressure with CallerRunsPolicy
    static void demoCallerRunsBackpressure() throws Exception {
        System.out.println("\nDemo: CallerRuns backpressure");
        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                1, 1,
                5, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1),
                namedFactory("callerRuns", false, null),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        long start = System.currentTimeMillis();
        for (int i = 0; i < 6; i++) {
            final int id = i;
            pool.execute(() -> {
                System.out.println("task " + id + " on " + Thread.currentThread().getName());
                try { Thread.sleep(120); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
        }
        pool.shutdown();
        pool.awaitTermination(2, TimeUnit.SECONDS);
        System.out.println("Elapsed ~" + (System.currentTimeMillis() - start) + "ms");
    }

    // Demo: CompletionService (results as they complete)
    static void demoCompletionService() throws Exception {
        System.out.println("\nDemo: ExecutorCompletionService");
        ExecutorService pool = Executors.newFixedThreadPool(3, namedFactory("ecs", false, null));
        CompletionService<String> ecs = new ExecutorCompletionService<>(pool);
        for (int i = 0; i < 5; i++) {
            int id = i;
            ecs.submit(() -> {
                Thread.sleep(50 + (4 - id) * 30L);
                return "done-" + id + " by " + Thread.currentThread().getName();
            });
        }
        for (int i = 0; i < 5; i++) {
            Future<String> f = ecs.take();
            System.out.println("completed: " + f.get());
        }
        pool.shutdown();
        pool.awaitTermination(1, TimeUnit.SECONDS);
    }

    // Demo: invokeAll and invokeAny
    static void demoInvokeAnyAndAll() throws Exception {
        System.out.println("\nDemo: invokeAll and invokeAny");
        ExecutorService pool = Executors.newFixedThreadPool(3, namedFactory("invoke", false, null));
        List<Callable<String>> tasks = Arrays.asList(
                () -> { Thread.sleep(80); return "A"; },
                () -> { Thread.sleep(20); return "B"; },
                () -> { Thread.sleep(50); return "C"; }
        );
        List<Future<String>> all = pool.invokeAll(tasks);
        for (Future<String> f : all) System.out.print(f.get() + " ");
        System.out.println();
        String any = pool.invokeAny(tasks);
        System.out.println("invokeAny winner: " + any);
        pool.shutdown();
        pool.awaitTermination(1, TimeUnit.SECONDS);
    }

    // Demo: cancellation and cooperative interruption
    static void demoCancellationAndInterruption() throws Exception {
        System.out.println("\nDemo: cancellation and interruption");
        ExecutorService pool = Executors.newSingleThreadExecutor(namedFactory("cancel", false, null));
        Future<?> f = pool.submit(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Thread.sleep(50);
                }
            } catch (InterruptedException e) {
                System.out.println("Interrupted flag observed in " + Thread.currentThread().getName());
                Thread.currentThread().interrupt();
            }
        });
        Thread.sleep(120);
        System.out.println("Cancel now...");
        f.cancel(true);
        pool.shutdown();
        pool.awaitTermination(1, TimeUnit.SECONDS);
    }

    // Demo: scheduled fixed-rate vs fixed-delay
    static void demoScheduledRateVsDelay() throws Exception {
        System.out.println("\nDemo: scheduleAtFixedRate vs scheduleWithFixedDelay");
        ScheduledThreadPoolExecutor ses = new ScheduledThreadPoolExecutor(1, namedFactory("sched", false, null));
        ses.setRemoveOnCancelPolicy(true);

        Runnable workRate = new Runnable() {
            long last = System.nanoTime();
            public void run() {
                long now = System.nanoTime();
                System.out.println("fixedRate  delta=" + ((now - last) / 1_000_000) + "ms on " + Thread.currentThread().getName());
                last = now;
                try { Thread.sleep(70); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        };
        ScheduledFuture<?> fr = ses.scheduleAtFixedRate(workRate, 0, 100, TimeUnit.MILLISECONDS);
        ses.schedule(() -> fr.cancel(false), 450, TimeUnit.MILLISECONDS);

        Runnable workDelay = new Runnable() {
            long last = System.nanoTime();
            public void run() {
                long now = System.nanoTime();
                System.out.println("fixedDelay delta=" + ((now - last) / 1_000_000) + "ms on " + Thread.currentThread().getName());
                last = now;
                try { Thread.sleep(70); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        };
        ScheduledFuture<?> fd = ses.scheduleWithFixedDelay(workDelay, 0, 100, TimeUnit.MILLISECONDS);
        ses.schedule(() -> fd.cancel(false), 450, TimeUnit.MILLISECONDS);

        ses.shutdown();
        ses.awaitTermination(2, TimeUnit.SECONDS);
    }

    // Demo: custom rejection handler
    static void demoCustomRejectedHandler() throws Exception {
        System.out.println("\nDemo: custom RejectedExecutionHandler");
        RejectedExecutionHandler handler = (r, executor) ->
                System.out.println("Rejected task: " + r + " by " + Thread.currentThread().getName());

        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                1, 1, 0, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1),
                namedFactory("rej", false, null),
                handler
        );

        for (int i = 0; i < 5; i++) {
            final int id = i;
            pool.execute(() -> {
                try { Thread.sleep(120); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                System.out.println("done " + id);
            });
        }
        pool.shutdown();
        pool.awaitTermination(1, TimeUnit.SECONDS);
    }

    // Demo: work-stealing pool (ForkJoinPool)
    static void demoWorkStealingPool() throws Exception {
        System.out.println("\nDemo: work-stealing pool (ForkJoinPool)");
        ForkJoinPool pool = (ForkJoinPool) Executors.newWorkStealingPool();
        List<ForkJoinTask<String>> list = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            int id = i;
            list.add(pool.submit(() -> {
                Thread.sleep(60 + (3 - id) * 20L);
                return "ws-" + id + " on " + Thread.currentThread().getName();
            }));
        }
        for (ForkJoinTask<String> t : list) {
            System.out.println(t.join());
        }
        pool.shutdown();
        pool.awaitTermination(1, TimeUnit.SECONDS);
    }

    // Example: deadlock pattern (do not run)
    // Single-thread executor where a task submits another task and waits synchronously for its result.
    // This deadlocks because the only worker is busy waiting.
    static void demoDeadlockPattern() throws Exception {
        ExecutorService single = Executors.newSingleThreadExecutor();
        Future<String> outer = single.submit(() -> {
            Future<String> inner = single.submit(() -> "inner");
            // BAD: waiting inside the same single-threaded pool
            return "outer + " + inner.get();
        });
        System.out.println(outer.get()); // would hang
        single.shutdown();
    }

    // Utility: named thread factory with optional uncaught exception handler
    static ThreadFactory namedFactory(String poolName, boolean daemon, Thread.UncaughtExceptionHandler handler) {
        return new ThreadFactory() {
            final AtomicInteger n = new AtomicInteger();
            @Override public Thread newThread(Runnable r) {
                Thread t = new Thread(r, poolName + "-" + n.incrementAndGet());
                t.setDaemon(daemon);
                if (handler != null) t.setUncaughtExceptionHandler(handler);
                return t;
            }
        };
    }

    // Utility: print pool stats
    static void printPoolStats(ThreadPoolExecutor pool, String label) {
        System.out.println("[" + label + "] poolSize=" + pool.getPoolSize()
                + ", active=" + pool.getActiveCount()
                + ", queued=" + pool.getQueue().size()
                + ", completed=" + pool.getCompletedTaskCount()
                + ", largest=" + pool.getLargestPoolSize());
    }

    // Pool that logs exceptions after task execution
    static class LoggingThreadPool extends ThreadPoolExecutor {
        LoggingThreadPool(int core, int max, long keepAlive, TimeUnit unit,
                          BlockingQueue<Runnable> queue, ThreadFactory factory,
                          RejectedExecutionHandler handler) {
            super(core, max, keepAlive, unit, queue, factory, handler);
        }
        @Override protected void afterExecute(Runnable r, Throwable t) {
            super.afterExecute(r, t);
            Throwable ex = t;
            if (ex == null && (r instanceof Future<?>)) {
                try {
                    Future<?> f = (Future<?>) r;
                    if (f.isDone()) f.get(); // will throw ExecutionException if task threw
                } catch (CancellationException ce) {
                    ex = ce;
                } catch (ExecutionException ee) {
                    ex = ee.getCause();
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
            if (ex != null) {
                System.err.println("afterExecute caught: " + ex + " in " + Thread.currentThread().getName());
            }
        }
    }

    /*
    Bonus: Virtual threads (Java 21+). Not compiled here, for reference only.

    try (ExecutorService vt = Executors.newVirtualThreadPerTaskExecutor()) {
        Future<String> f = vt.submit(() -> {
            // Cheap to block; each task has its own virtual thread
            Thread.sleep(100);
            return "hello from " + Thread.currentThread();
        });
        System.out.println(f.get());
    }
    */
}