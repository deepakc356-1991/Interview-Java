package _08_01_thread_lifecycle_and_runnable;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 Thread Lifecycle & Runnable – Interview Q&A (Basic → Advanced)

 Q: What are Java thread states?
 A: NEW, RUNNABLE, BLOCKED, WAITING, TIMED_WAITING, TERMINATED.

 Q: What does RUNNABLE mean in Java?
 A: It covers both “ready-to-run” and “running” (OS-scheduled) states.

 Q: How do you create a thread?
 A: Prefer implementing Runnable (composition) over extending Thread (inheritance). Pass Runnable to new Thread(r).

 Q: start() vs run()?
 A: start() creates a new thread and then calls run() on it. Calling run() directly executes on the current thread.

 Q: Can a thread be restarted?
 A: No. Calling start() twice throws IllegalThreadStateException.

 Q: sleep vs wait vs join vs yield?
 A:
 - sleep(ms): static, doesn’t release monitors, puts thread into TIMED_WAITING, can be interrupted.
 - wait()/wait(ms): must hold monitor; releases it, goes to WAITING/TIMED_WAITING; used for coordination; always call in loop.
 - join()/join(ms): waits for another thread to die; WAITING/TIMED_WAITING; can be interrupted.
 - yield(): hint to scheduler; rarely needed.

 Q: BLOCKED vs WAITING?
 A: BLOCKED is waiting to enter a synchronized block (monitor acquisition). WAITING/TIMED_WAITING are parked while already owning nothing relevant (e.g., wait/join/sleep/park).

 Q: How to stop a thread?
 A: Cooperative cancellation via interruption or a flag; do not use Thread.stop().

 Q: What is interruption?
 A: A mechanism to request a thread to stop what it’s doing; blocking methods typically throw InterruptedException and clear the flag.

 Q: Daemon vs user thread?
 A: JVM exits when only daemon threads remain. Use daemons for background services; avoid relying on finally blocks in daemons at shutdown.

 Q: UncaughtExceptionHandler?
 A: Handles exceptions that escape run().

 Q: One Runnable used by multiple threads?
 A: Legal; shared state requires synchronization or atomics.

 Q: Happens-before with start/join?
 A: Actions before calling start() happen-before actions in the started thread. Thread termination happens-before another thread successfully returns from join().

 Q: Runnable vs Callable?
 A: Runnable returns no result and cannot throw checked exceptions. Callable<V> returns V and can throw checked exceptions; often used with ExecutorService.

 Q: Why Executors over new Thread?
 A: Thread management, pooling, throttling, task submission, futures, lifecycle control.
**/

public class _03_InterviewQA {

    public static void main(String[] args) throws Exception {
        log("Starting Thread Lifecycle & Runnable demos");
        demoStartVsRun();
        demoStartTwiceFails();
        demoRunnableSharedInstance();
        demoLifecycleStates();
        demoJoinWaiting();
        demoInterruptSleep();
        demoUncaughtExceptionHandler();
        demoDaemonThread();
        demoExecutorWithRunnableAndThreadFactory();
        demoHappensBeforeStartAndJoin();
        log("All demos finished");
    }

    // ---------- Helpers ----------

    private static void log(String fmt, Object... args) {
        System.out.printf("%-22s %s%n", "[" + Thread.currentThread().getName() + "]", String.format(fmt, args));
    }

    private static void sleepQuiet(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static boolean awaitState(Thread t, Thread.State expected, long timeoutMs) {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);
        while (System.nanoTime() < deadline) {
            if (t.getState() == expected) return true;
            Thread.yield();
        }
        return t.getState() == expected;
    }

    // ---------- Demos ----------

    // start() vs run()
    private static void demoStartVsRun() throws InterruptedException {
        log("Demo: start() vs run()");
        Runnable r = () -> log("Runnable running on thread: %s", Thread.currentThread().getName());

        log("Calling run() directly (no new thread)");
        r.run(); // runs on main

        Thread t = new Thread(r, "Worker-Start");
        log("Calling start() (new thread)");
        t.start();
        t.join();
    }

    // Starting a thread twice throws
    private static void demoStartTwiceFails() throws InterruptedException {
        log("Demo: starting a thread twice");
        Thread t = new Thread(() -> log("First run OK"), "StartTwice");
        t.start();
        try {
            t.start();
        } catch (IllegalThreadStateException e) {
            log("Expected error when starting twice: %s", e);
        }
        t.join();
    }

    // One Runnable shared by multiple threads
    private static void demoRunnableSharedInstance() throws InterruptedException {
        log("Demo: single Runnable shared by multiple threads");
        class SharedRunnable implements Runnable {
            private final AtomicInteger counter = new AtomicInteger();
            @Override public void run() {
                int v = counter.incrementAndGet();
                log("SharedRunnable counter=%d", v);
            }
        }
        SharedRunnable task = new SharedRunnable();
        Thread t1 = new Thread(task, "Shared-1");
        Thread t2 = new Thread(task, "Shared-2");
        t1.start(); t2.start();
        t1.join(); t2.join();
        log("Final shared counter is 2 (demonstrates shared state in Runnable)");
    }

    // Lifecycle states: NEW, RUNNABLE, TIMED_WAITING, WAITING, BLOCKED, TERMINATED
    private static void demoLifecycleStates() throws InterruptedException {
        log("Demo: lifecycle states");

        // NEW -> TIMED_WAITING (sleep) -> TERMINATED
        Thread tSleep = new Thread(() -> {
            log("tSleep: going to sleep");
            sleepQuiet(300);
            log("tSleep: woke up");
        }, "Sleepy");
        log("tSleep initial state=%s", tSleep.getState()); // NEW
        tSleep.start();
        if (awaitState(tSleep, Thread.State.TIMED_WAITING, 1000))
            log("tSleep state while sleeping=%s", tSleep.getState());
        tSleep.join();
        log("tSleep final state=%s", tSleep.getState()); // TERMINATED

        // RUNNABLE (busy loop)
        Thread tBusy = new Thread(() -> {
            long end = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(100);
            while (System.nanoTime() < end) { /* busy */ }
        }, "Busy");
        tBusy.start();
        if (awaitState(tBusy, Thread.State.RUNNABLE, 500))
            log("tBusy state during work=%s", tBusy.getState());
        tBusy.join();
        log("tBusy final state=%s", tBusy.getState());

        // WAITING via Object.wait()
        final Object monitor = new Object();
        CountDownLatch waiterReady = new CountDownLatch(1);
        Thread tWait = new Thread(() -> {
            synchronized (monitor) {
                log("tWait: waiting on monitor");
                waiterReady.countDown();
                try {
                    monitor.wait();
                    log("tWait: notified and resumed");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "Waiter");
        tWait.start();
        waiterReady.await();
        if (awaitState(tWait, Thread.State.WAITING, 1000))
            log("tWait state while waiting=%s", tWait.getState());
        synchronized (monitor) { monitor.notify(); }
        tWait.join();
        log("tWait final state=%s", tWait.getState());

        // BLOCKED due to synchronized lock
        final Object lock = new Object();
        CountDownLatch holderEntered = new CountDownLatch(1);
        Thread tHolder = new Thread(() -> {
            synchronized (lock) {
                log("tHolder: acquired lock");
                holderEntered.countDown();
                sleepQuiet(200);
                log("tHolder: releasing lock");
            }
        }, "Holder");
        Thread tBlocked = new Thread(() -> {
            log("tBlocked: trying to acquire lock");
            synchronized (lock) {
                log("tBlocked: acquired lock finally");
            }
        }, "Blocked");
        tHolder.start();
        holderEntered.await();
        tBlocked.start();
        if (awaitState(tBlocked, Thread.State.BLOCKED, 1000))
            log("tBlocked state while blocked=%s", tBlocked.getState());
        tHolder.join(); tBlocked.join();
        log("tBlocked final state=%s", tBlocked.getState());
    }

    // join() puts the caller into WAITING/TIMED_WAITING
    private static void demoJoinWaiting() throws InterruptedException {
        log("Demo: join() WAITING");
        Thread worker = new Thread(() -> {
            log("Worker: doing brief work");
            sleepQuiet(200);
            log("Worker: done");
        }, "Worker");
        Thread joiner = new Thread(() -> {
            log("Joiner: joining on Worker");
            try {
                worker.join();
                log("Joiner: unblocked after Worker finished");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Joiner");

        worker.start();
        joiner.start();
        if (awaitState(joiner, Thread.State.WAITING, 1000))
            log("Joiner state while waiting=%s", joiner.getState());
        worker.join(); joiner.join();
        log("Joiner final state=%s", joiner.getState());
    }

    // Interruption of a sleeping thread
    private static void demoInterruptSleep() throws InterruptedException {
        log("Demo: interruption while sleeping");
        Thread sleeper = new Thread(() -> {
            try {
                log("Sleeper: sleeping 5s");
                Thread.sleep(5000);
                log("Sleeper: woke normally");
            } catch (InterruptedException e) {
                log("Sleeper: interrupted, interrupt flag now cleared=%s", Thread.currentThread().isInterrupted());
                // Optionally restore if upper layers need to see it:
                Thread.currentThread().interrupt();
            }
        }, "InterruptibleSleeper");
        sleeper.start();
        sleepQuiet(100);
        log("Main: interrupting sleeper");
        sleeper.interrupt();
        sleeper.join();
        log("Sleeper final state=%s", sleeper.getState());
    }

    // Uncaught exception handler
    private static void demoUncaughtExceptionHandler() throws InterruptedException {
        log("Demo: UncaughtExceptionHandler");
        Thread t = new Thread(() -> { throw new RuntimeException("boom"); }, "Crasher");
        t.setUncaughtExceptionHandler((thread, ex) ->
                log("Handled uncaught from %s: %s", thread.getName(), ex.toString()));
        t.start();
        t.join();
        log("Crasher final state=%s", t.getState());
    }

    // Daemon thread demo (note: JVM may exit without waiting for daemon)
    private static void demoDaemonThread() throws InterruptedException {
        log("Demo: daemon thread");
        Thread daemon = new Thread(() -> {
            for (int i = 0; i < 3; i++) {
                log("daemon heartbeat %d", i);
                sleepQuiet(50);
            }
        }, "Daemon");
        daemon.setDaemon(true);
        log("Is daemon? %s", daemon.isDaemon());
        daemon.start();
        // Joining here just to keep output tidy; JVM would not wait for daemons otherwise.
        daemon.join();
    }

    // ExecutorService with Runnable and custom ThreadFactory
    private static void demoExecutorWithRunnableAndThreadFactory() throws InterruptedException {
        log("Demo: ExecutorService with Runnable + ThreadFactory");
        AtomicInteger seq = new AtomicInteger(1);
        ThreadFactory factory = r -> {
            Thread t = new Thread(r);
            t.setName("pool-" + seq.getAndIncrement());
            t.setDaemon(false);
            t.setUncaughtExceptionHandler((th, ex) ->
                    log("Pool handler caught from %s: %s", th.getName(), ex.toString()));
            return t;
        };
        ExecutorService pool = Executors.newFixedThreadPool(2, factory);
        pool.execute(() -> log("Task1 on %s", Thread.currentThread().getName()));
        pool.execute(() -> log("Task2 on %s", Thread.currentThread().getName()));
        pool.execute(() -> { throw new RuntimeException("executor boom"); });

        pool.shutdown();
        if (!pool.awaitTermination(2, TimeUnit.SECONDS)) {
            pool.shutdownNow();
        }
        log("Executor terminated");
    }

    // Demonstrate happens-before via start() and join()
    private static void demoHappensBeforeStartAndJoin() throws InterruptedException {
        log("Demo: happens-before start/join visibility");
        class Box { int x; }
        Box box = new Box();

        // Writes before start are visible to the new thread
        box.x = 5;
        Thread reader = new Thread(() -> log("Reader sees x=%d (should be 5)", box.x), "Reader");
        reader.start();
        reader.join();

        // Writes in child before termination are visible to joiner
        Thread writer = new Thread(() -> {
            box.x = 42;
            log("Writer set x=42");
        }, "Writer");
        writer.start();
        writer.join();
        log("After join, main sees x=%d (should be 42)", box.x);
    }
}