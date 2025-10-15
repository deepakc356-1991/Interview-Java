package _08_01_thread_lifecycle_and_runnable;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Thread Lifecycle & Runnable - Theory + Live Demonstrations
 *
 * Concepts covered in comments and code:
 * - Runnable (task) vs Thread (carrier). Prefer Runnable/Callable for separation of concerns.
 * - start() vs run(): start() creates a new thread; run() executes in current thread.
 * - Thread states: NEW, RUNNABLE, BLOCKED, WAITING, TIMED_WAITING, TERMINATED.
 * - Typical transitions and how to reproduce each state safely.
 * - join(), sleep(), wait()/notify(), synchronized monitor/lock.
 * - Interrupts and how blocking methods react to interrupt.
 * - Daemon vs user threads.
 * - Naming, identity, priority (touch on in comments).
 *
 * Notes:
 * - Thread.getState() is a snapshot and can change quickly.
 * - Scheduling is OS/JVM dependent; small timing differences are normal.
 * - Monitors (synchronized) provide mutual exclusion and memory visibility (happens-before).
 * - wait() must be called while holding the same monitor; notify/notifyAll also require it.
 * - InterruptedException clears the thread’s interrupt status; re-set if you need to propagate it.
 */
public class _01_Theory {

    public static void main(String[] args) throws Exception {
        log("=== Runnable vs Thread (start vs run) ===");
        demoRunnableVsThread();

        log("\n=== Thread lifecycle states (NEW, RUNNABLE, BLOCKED, WAITING, TIMED_WAITING, TERMINATED) ===");
        demoThreadLifecycleStates();

        log("\n=== Interrupts (cooperative cancellation) ===");
        demoInterrupts();

        log("\n=== Daemon threads (do not keep JVM alive) ===");
        demoDaemonThread();

        log("\nDone.");
    }

    /**
     * Runnable vs Thread:
     * - Runnable is a functional interface: "what to do".
     * - Thread is a container that executes a Runnable on a new OS thread.
     * - Prefer passing Runnable (or Callable via executors) rather than extending Thread.
     * - start() runs concurrently; run() runs synchronously on the current thread.
     */
    static void demoRunnableVsThread() throws InterruptedException {
        Runnable task = () -> log("Hello from Runnable. Current thread = " + Thread.currentThread().getName());

        log("Calling runnable.run(): executes on current (main) thread");
        task.run(); // No new thread; executes on "main"

        Thread t1 = new Thread(task, "T-FROM-RUNNABLE");
        log("Calling thread.start(): creates a new thread and invokes run()");
        t1.start();
        t1.join();

        // Extending Thread directly (less flexible than passing a Runnable)
        class MyThread extends Thread {
            MyThread() { super("T-EXTENDS-THREAD"); }
            @Override public void run() { log("Hello from Thread subclass. Name = " + getName()); }
        }
        Thread t2 = new MyThread();
        t2.start();
        t2.join();
    }

    /**
     * Demonstrates each thread state with controlled scenarios:
     * - NEW: not started yet.
     * - RUNNABLE: actively executing or ready to run (including CPU-bound loop).
     * - BLOCKED: trying to enter a synchronized block while another thread holds that monitor.
     * - WAITING: waiting indefinitely via Object.wait() or Thread.join() without timeout.
     * - TIMED_WAITING: sleeping/waiting/joining with a timeout.
     * - TERMINATED: completed execution.
     */
    static void demoThreadLifecycleStates() throws InterruptedException {
        // NEW
        Thread tNew = new Thread(() -> {}, "T-NEW");
        log("NEW     -> " + tNew.getState());

        // RUNNABLE (busy-yield loop to keep it RUNNABLE briefly)
        AtomicBoolean keepRunning = new AtomicBoolean(true);
        Thread tRunnable = new Thread(() -> {
            while (keepRunning.get()) {
                // Keep the thread "alive" and RUNNABLE; yield reduces CPU burn.
                Thread.yield();
            }
        }, "T-RUNNABLE");
        tRunnable.start();
        waitForState(tRunnable, Thread.State.RUNNABLE, 1000);
        log("RUNNABLE-> " + tRunnable.getState());
        keepRunning.set(false);
        tRunnable.join();

        // TIMED_WAITING using sleep
        Thread tSleeping = new Thread(() -> {
            try {
                Thread.sleep(5000); // sleep puts thread into TIMED_WAITING
            } catch (InterruptedException e) {
                // interrupt wakes it up; status cleared by exception
            }
        }, "T-SLEEPING");
        tSleeping.start();
        waitForState(tSleeping, Thread.State.TIMED_WAITING, 1500);
        log("TIMED_WAITING (sleep) -> " + tSleeping.getState());
        tSleeping.interrupt();
        tSleeping.join();

        // BLOCKED: one thread holds a monitor; another tries to enter
        final Object monitor = new Object();
        CountDownLatch holderEntered = new CountDownLatch(1);
        Thread tHolder = new Thread(() -> {
            synchronized (monitor) {
                holderEntered.countDown();
                log("Holder: acquired lock, holding for a moment...");
                sleepSilently(1200);
                log("Holder: releasing lock.");
            }
        }, "T-HOLDER");

        Thread tBlocked = new Thread(() -> {
            synchronized (monitor) {
                // Will run only after holder releases the lock
                log("Blocked thread acquired lock after being unblocked.");
            }
        }, "T-BLOCKED");

        tHolder.start();
        holderEntered.await();
        tBlocked.start();
        waitForState(tBlocked, Thread.State.BLOCKED, 1000);
        log("BLOCKED -> " + tBlocked.getState());
        tHolder.join();
        tBlocked.join();

        // WAITING: wait() without timeout
        final Object waitLock = new Object();
        CountDownLatch readyToWait = new CountDownLatch(1);
        Thread tWaiting = new Thread(() -> {
            synchronized (waitLock) {
                readyToWait.countDown();
                try {
                    waitLock.wait(); // WAITING until notified
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "T-WAITING");
        tWaiting.start();
        readyToWait.await();
        waitForState(tWaiting, Thread.State.WAITING, 1000);
        log("WAITING -> " + tWaiting.getState());

        // Notify to let WAITING thread proceed to completion
        synchronized (waitLock) {
            waitLock.notifyAll();
        }
        tWaiting.join();
        log("TERMINATED (after join) -> " + tWaiting.getState());
    }

    /**
     * Interrupts:
     * - interrupt() sets a flag on the target thread.
     * - Blocking methods like sleep/wait/join throw InterruptedException and clear the flag.
     * - Code should either handle and stop, or re-set the flag to propagate cancellation.
     */
    static void demoInterrupts() throws InterruptedException {
        Thread t = new Thread(() -> {
            try {
                log("Worker: going to sleep (TIMED_WAITING)...");
                Thread.sleep(10_000);
                log("Worker: woke up without interrupt (unlikely here).");
            } catch (InterruptedException e) {
                log("Worker: InterruptedException caught. isInterrupted() now = " + Thread.currentThread().isInterrupted());
                // If you need to propagate, re-set the flag:
                Thread.currentThread().interrupt();
                log("Worker: re-set interrupt flag. isInterrupted() now = " + Thread.currentThread().isInterrupted());
            }
        }, "T-INTERRUPT");
        t.start();

        waitForState(t, Thread.State.TIMED_WAITING, 1500);
        log("Before interrupt: state = " + t.getState());
        t.interrupt();
        t.join();
        log("After join: state = " + t.getState() + ", isInterrupted() = " + t.isInterrupted());
    }

    /**
     * Daemon threads:
     * - setDaemon(true) marks thread as daemon; JVM will not wait for daemon threads on exit.
     * - Use for background housekeeping only; ensure safe shutdown behavior.
     */
    static void demoDaemonThread() throws InterruptedException {
        Thread daemon = new Thread(() -> {
            int i = 0;
            while (i < 3) { // short demo; real daemons may run indefinitely
                sleepSilently(200);
                log("Daemon heartbeat " + (++i));
            }
        }, "T-DAEMON");
        daemon.setDaemon(true);
        daemon.start();
        log("Started daemon. isDaemon = " + daemon.isDaemon());
        // We do not join daemon; program can exit even if it is still running.
        sleepSilently(250); // give it a chance to print at least once
    }

    // Utility: wait for a specific thread state up to timeout
    static boolean waitForState(Thread t, Thread.State desired, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (t.getState() == desired) return true;
            Thread.sleep(10);
        }
        return t.getState() == desired;
    }

    static void sleepSilently(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }

    static void log(String msg) {
        System.out.printf("[%s] %s%n", Thread.currentThread().getName(), msg);
    }
}

/*
Additional notes (theory quick reference):

- Thread identity:
  - getName(), setName(), getId(), isAlive(), isDaemon(), getPriority()/setPriority().
  - Priority is a hint to the scheduler, not a guarantee.

- States recap:
  - NEW: created, not started.
  - RUNNABLE: executing or ready-to-run.
  - BLOCKED: waiting to acquire a monitor (synchronized).
  - WAITING: waiting indefinitely (Object.wait(), Thread.join()).
  - TIMED_WAITING: waiting with timeout (sleep, wait(timeout), join(timeout)).
  - TERMINATED: finished run().

- Synchronization and visibility:
  - synchronized provides mutual exclusion and happens-before on lock release/acquire.
  - volatile ensures visibility and ordering for a single variable.
  - Avoid calling wait()/notify() without owning the monitor.

- start() vs run():
  - start() -> new thread -> calls run().
  - run() -> normal method call in current thread.

- Interrupts:
  - interrupt() sets the flag.
  - Blocking calls respond with InterruptedException and clear the flag.
  - If you catch it and want to propagate cancellation, re-set the flag.

- Runnable vs Callable:
  - Runnable: no return, no checked exception.
  - Callable<V>: returns V, can throw checked exceptions (used with ExecutorService).

- Prefer using ExecutorService (thread pools) for production code; manual Thread is fine for learning/demonstrations.
*/