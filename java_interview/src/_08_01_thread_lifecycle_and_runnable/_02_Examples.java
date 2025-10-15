package _08_01_thread_lifecycle_and_runnable;

/**
 * Thread Lifecycle & Runnable examples.
 *
 * Shows:
 * - Creating threads via Runnable and by extending Thread
 * - Life-cycle states: NEW, RUNNABLE, TIMED_WAITING (sleep), WAITING (wait/join), BLOCKED (monitor contention), TERMINATED
 * - Interrupting a sleeping thread
 * - Using Runnable with ExecutorService
 */
public class _02_Examples {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Thread Lifecycle & Runnable examples ===");

        example1_CreateThreads_basic();
        line();

        example2_TimedWaiting_with_sleep();
        line();

        example3_Waiting_with_wait_notify();
        line();

        example4_Blocked_via_synchronized_contention();
        line();

        example5_Waiting_with_join();
        line();

        example6_Interrupting_a_sleeping_thread();
        line();

        example7_Runnable_with_ExecutorService();
        line();

        System.out.println("=== Done ===");
    }

    // Utility: small sleep without throwing
    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    // Utility: wait until a thread reaches a state (best-effort, for demos)
    private static void awaitState(Thread t, Thread.State expected, long timeoutMs) {
        long end = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < end) {
            if (t.getState() == expected) return;
            sleep(2);
        }
    }

    private static void line() {
        System.out.println("------------------------------------------------");
    }

    // Example 1: Creating threads (Runnable vs Thread) and observing NEW -> RUNNABLE -> TERMINATED
    static class HelloRunnable implements Runnable {
        private final String label;
        HelloRunnable(String label) { this.label = label; }
        @Override public void run() {
            for (int i = 0; i < 3; i++) {
                System.out.println(label + " running in " + Thread.currentThread().getName());
                sleep(50);
            }
        }
    }

    static class HelloThread extends Thread {
        HelloThread(String name) { super(name); }
        @Override public void run() {
            for (int i = 0; i < 3; i++) {
                System.out.println("Hello from " + getName());
                try {
                    sleep(50);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private static void example1_CreateThreads_basic() throws InterruptedException {
        System.out.println("Example 1: Creating threads (Runnable vs Thread) and basic states");

        Thread t1 = new Thread(new HelloRunnable("RunnableTask"), "T-1");
        HelloThread t2 = new HelloThread("T-2");

        // NEW state: created but not started
        System.out.println(t1.getName() + " state (before start): " + t1.getState());
        System.out.println(t2.getName() + " state (before start): " + t2.getState());

        t1.start();
        t2.start();

        // Shortly after start they may be RUNNABLE (or TIMED_WAITING if already sleeping)
        sleep(10);
        System.out.println(t1.getName() + " state (after start): " + t1.getState());
        System.out.println(t2.getName() + " state (after start): " + t2.getState());

        t1.join();
        t2.join();

        // After completion: TERMINATED
        System.out.println(t1.getName() + " state (after join): " + t1.getState());
        System.out.println(t2.getName() + " state (after join): " + t2.getState());
    }

    // Example 2: TIMED_WAITING via Thread.sleep()
    private static void example2_TimedWaiting_with_sleep() throws InterruptedException {
        System.out.println("Example 2: TIMED_WAITING via sleep()");
        Thread sleepy = new Thread(() -> {
            try {
                System.out.println("Sleepy: going to sleep");
                Thread.sleep(400);
                System.out.println("Sleepy: woke up");
            } catch (InterruptedException e) {
                System.out.println("Sleepy: interrupted while sleeping");
                Thread.currentThread().interrupt();
            }
        }, "Sleepy");

        sleepy.start();
        awaitState(sleepy, Thread.State.TIMED_WAITING, 200);
        System.out.println("Sleepy state (expected TIMED_WAITING): " + sleepy.getState());
        sleepy.join();
        System.out.println("Sleepy state (after join): " + sleepy.getState());
    }

    // Example 3: WAITING via wait()/notify()
    private static void example3_Waiting_with_wait_notify() throws InterruptedException {
        System.out.println("Example 3: WAITING via wait()/notify()");
        final Object lock = new Object();

        Thread waiter = new Thread(() -> {
            synchronized (lock) {
                try {
                    System.out.println("Waiter: waiting on lock");
                    lock.wait(); // becomes WAITING until notified
                    System.out.println("Waiter: resumed after notify");
                } catch (InterruptedException e) {
                    System.out.println("Waiter: interrupted while waiting");
                    Thread.currentThread().interrupt();
                }
            }
        }, "Waiter");

        Thread notifier = new Thread(() -> {
            sleep(150); // ensure waiter is waiting
            synchronized (lock) {
                System.out.println("Notifier: notifying");
                lock.notifyAll();
            }
        }, "Notifier");

        waiter.start();
        awaitState(waiter, Thread.State.WAITING, 500);
        System.out.println("Waiter state (expected WAITING): " + waiter.getState());

        notifier.start();
        waiter.join();
        notifier.join();
        System.out.println("Waiter state (after join): " + waiter.getState());
    }

    // Example 4: BLOCKED via synchronized contention
    private static void example4_Blocked_via_synchronized_contention() throws InterruptedException {
        System.out.println("Example 4: BLOCKED via synchronized contention");
        final Object lock = new Object();

        Thread holder = new Thread(() -> {
            synchronized (lock) {
                System.out.println("Holder: acquired lock");
                sleep(300);
                System.out.println("Holder: releasing lock");
            }
        }, "Holder");

        Thread contender = new Thread(() -> {
            sleep(50); // start after holder
            synchronized (lock) {
                System.out.println("Contender: acquired lock");
            }
        }, "Contender");

        holder.start();
        contender.start();

        awaitState(contender, Thread.State.BLOCKED, 300);
        System.out.println("Contender state (expected BLOCKED): " + contender.getState());

        holder.join();
        contender.join();
        System.out.println("Contender state (after join): " + contender.getState());
    }

    // Example 5: WAITING via join()
    private static void example5_Waiting_with_join() throws InterruptedException {
        System.out.println("Example 5: WAITING via join()");
        Thread worker = new Thread(() -> {
            System.out.println("Worker: doing work");
            sleep(200);
            System.out.println("Worker: done");
        }, "Worker");

        Thread joiner = new Thread(() -> {
            try {
                System.out.println("Joiner: joining on Worker");
                worker.join(); // Joiner enters WAITING until Worker terminates
                System.out.println("Joiner: Worker finished");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Joiner");

        worker.start();
        joiner.start();

        awaitState(joiner, Thread.State.WAITING, 300);
        System.out.println("Joiner state (expected WAITING): " + joiner.getState());

        worker.join();
        joiner.join();
        System.out.println("Joiner state (after join): " + joiner.getState());
    }

    // Example 6: Interrupting a sleeping thread
    private static void example6_Interrupting_a_sleeping_thread() throws InterruptedException {
        System.out.println("Example 6: Interrupting a sleeping thread");
        Thread sleeper = new Thread(() -> {
            try {
                System.out.println("Sleeper: sleeping for a long time");
                Thread.sleep(5_000);
                System.out.println("Sleeper: woke normally");
            } catch (InterruptedException e) {
                System.out.println("Sleeper: caught InterruptedException (interrupt flag cleared by JVM)");
                // restore interrupt status if needed
                Thread.currentThread().interrupt();
                System.out.println("Sleeper: interrupt flag after restore = " + Thread.currentThread().isInterrupted());
            }
        }, "Sleeper");

        sleeper.start();
        awaitState(sleeper, Thread.State.TIMED_WAITING, 200);
        System.out.println("Sleeper state (expected TIMED_WAITING): " + sleeper.getState());
        System.out.println("Main: interrupting Sleeper");
        sleeper.interrupt();
        sleeper.join();
        System.out.println("Sleeper state (after join): " + sleeper.getState());
    }

    // Example 7: Using Runnable with ExecutorService
    private static void example7_Runnable_with_ExecutorService() throws InterruptedException {
        System.out.println("Example 7: Runnable with ExecutorService");

        java.util.concurrent.ExecutorService pool =
                java.util.concurrent.Executors.newFixedThreadPool(2);

        Runnable task = () -> {
            System.out.println("Pool task in " + Thread.currentThread().getName());
            sleep(100);
        };

        pool.execute(task);
        pool.execute(task);
        pool.execute(task);

        pool.shutdown();
        pool.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS);
        System.out.println("ExecutorService terminated: " + pool.isTerminated());
    }
}