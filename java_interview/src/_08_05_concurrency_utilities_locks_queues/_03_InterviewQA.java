package _08_05_concurrency_utilities_locks_queues;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.StampedLock;

/*
  Concurrency Utilities: Locks, Queues
  This single file is a guided Q&A (basic -> advanced) with runnable, bite-sized demos.
  Each section contains interview-style Q&A in comments and a tiny example.
  Run main() to see selected demos. Explore others by calling their demo() methods.

  INDEX
  1) ReentrantLock basics, vs synchronized
  2) tryLock + timeouts (non-blocking attempts)
  3) lockInterruptibly (responsive to interruption)
  4) Conditions vs wait/notify; producer-consumer with explicit Lock
  5) BlockingQueue producer-consumer (simpler)
  6) ReadWriteLock; read vs write, downgrading
  7) StampedLock; optimistic read
  8) Deadlock and avoidance (ordering, tryLock)
  9) BlockingQueue API: add/offer/put vs remove/poll/take vs element/peek
  10) SynchronousQueue: rendezvous/handoff
  11) PriorityBlockingQueue: ordering
  12) DelayQueue: time-based scheduling
  13) LinkedTransferQueue: transfer semantics
  14) BlockingDeque: work-stealing pattern
  15) ConcurrentLinkedQueue: non-blocking queue
  16) Poison-pill shutdown
  17) Fairness trade-offs
  18) Backpressure with bounded queues
  19) drainTo for bulk draining
*/
public class _03_InterviewQA {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Concurrency Utilities (Locks, Queues) Demos ===");

        Example1_ReentrantLockBasics.demo();
        Example2_TryLockAndTimeout.demo();
        Example3_InterruptibleLock.demo();

        Example5_BlockingQueueProducerConsumer.demo();
        Example6_ReadWriteLock.demo();
        Example7_StampedLockOptimisticRead.demo();

        Example10_SynchronousQueueHandoff.demo();
        Example11_PriorityBlockingQueue.demo();
        Example12_DelayQueue.demo();
        Example13_LinkedTransferQueue.demo();
        Example16_PoisonPillShutdown.demo();

        Example19_DrainTo.demo();

        System.out.println("=== Done. Explore more by invoking other demo() methods. ===");
    }

    // Utility sleep helper
    private static void sleepMs(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    // Utility print with thread
    private static void log(String msg) {
        System.out.printf("[%s][%s] %s%n", LocalTime.now(), Thread.currentThread().getName(), msg);
    }

    // 1) ReentrantLock basics, vs synchronized
    static class Example1_ReentrantLockBasics {
        /*
          Q: ReentrantLock vs synchronized?
          A:
            - Both provide mutual exclusion and memory visibility (happens-before on unlock->lock).
            - ReentrantLock features:
              * tryLock(), tryLock(timeout), lockInterruptibly()
              * Multiple Condition objects
              * Optional fairness
              * Introspection (isLocked, isHeldByCurrentThread, getHoldCount)
            - synchronized is simpler, auto-release on exit, JVM-optimized, no explicit unlock required.

          Best practice: Always unlock in finally to avoid leaks.
        */
        static void demo() {
            System.out.println("\n[1] ReentrantLock basics demo:");
            ReentrantLock lock = new ReentrantLock();
            Runnable critical = () -> {
                lock.lock();
                try {
                    log("Entered critical section (reentrant count=" + lock.getHoldCount() + ")");
                    // Reentrancy demo
                    lock.lock();
                    try {
                        log("Reacquired lock (reentrant count=" + lock.getHoldCount() + ")");
                    } finally {
                        lock.unlock();
                    }
                    sleepMs(50);
                } finally {
                    lock.unlock();
                    log("Exited critical section");
                }
            };

            Thread t1 = new Thread(critical, "Lock-T1");
            Thread t2 = new Thread(critical, "Lock-T2");
            t1.start(); t2.start();
            joinQuietly(t1); joinQuietly(t2);
        }
    }

    // 2) tryLock + timeout
    static class Example2_TryLockAndTimeout {
        /*
          Q: When use tryLock?
          A: For non-blocking attempts to avoid deadlock or wait-free fast paths.
             With timeouts, you can back off or fail gracefully instead of blocking forever.
        */
        static void demo() {
            System.out.println("\n[2] tryLock + timeout demo:");
            ReentrantLock lock = new ReentrantLock();

            Thread holder = new Thread(() -> {
                lock.lock();
                try {
                    log("Holding the lock briefly...");
                    sleepMs(120);
                } finally {
                    lock.unlock();
                }
            }, "TryLock-Holder");

            Thread contender = new Thread(() -> {
                boolean acquired = false;
                try {
                    acquired = lock.tryLock(80, TimeUnit.MILLISECONDS);
                    log("tryLock(80ms) -> " + acquired);
                    if (!acquired) {
                        log("Falling back later...");
                        acquired = lock.tryLock(200, TimeUnit.MILLISECONDS);
                        log("tryLock(200ms) -> " + acquired);
                    }
                    if (acquired) sleepMs(20);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    if (acquired) lock.unlock();
                }
            }, "TryLock-Contender");

            holder.start(); sleepMs(10); contender.start();
            joinQuietly(holder); joinQuietly(contender);
        }
    }

    // 3) lockInterruptibly
    static class Example3_InterruptibleLock {
        /*
          Q: What is lockInterruptibly?
          A: It lets a thread wait for a lock but remain responsive to interruption (throws InterruptedException).
             Good for cancelable tasks and shutdowns.
        */
        static void demo() {
            System.out.println("\n[3] lockInterruptibly demo:");
            ReentrantLock lock = new ReentrantLock();

            Thread t1 = new Thread(() -> {
                lock.lock();
                try {
                    log("Acquired lock, simulating work...");
                    sleepMs(200);
                } finally {
                    lock.unlock();
                }
            }, "Interruptible-Holder");

            Thread t2 = new Thread(() -> {
                try {
                    log("Attempting lockInterruptibly...");
                    lock.lockInterruptibly();
                    try {
                        log("Acquired lock (unexpected if interrupted early)");
                    } finally {
                        lock.unlock();
                    }
                } catch (InterruptedException e) {
                    log("Interrupted while waiting for the lock. Cleaning up...");
                }
            }, "Interruptible-Waiter");

            t1.start();
            sleepMs(10);
            t2.start();
            sleepMs(40);
            t2.interrupt();

            joinQuietly(t1); joinQuietly(t2);
        }
    }

    // 4) Conditions vs wait/notify; producer-consumer with explicit Lock
    static class Example4_ConditionProducerConsumer {
        /*
          Q: Condition vs Object.wait/notify?
          A:
            - await/signal on a Condition is like wait/notify but:
              * Multiple conditions per lock.
              * Explicit control; must hold the Lock to await/signal.
            - Spurious wakeups can occur: always await in a loop.

          Below is a bounded buffer using a Lock + two Conditions.
          Note: For real code prefer BlockingQueue; this is educational.
        */
        static class BoundedBuffer<T> {
            private final Object[] items;
            private int head = 0, tail = 0, count = 0;
            private final ReentrantLock lock = new ReentrantLock();
            private final Condition notEmpty = lock.newCondition();
            private final Condition notFull = lock.newCondition();

            BoundedBuffer(int capacity) { this.items = new Object[capacity]; }

            void put(T t) throws InterruptedException {
                lock.lock();
                try {
                    while (count == items.length) notFull.await();
                    items[tail] = t;
                    tail = (tail + 1) % items.length;
                    count++;
                    notEmpty.signal();
                } finally {
                    lock.unlock();
                }
            }

            @SuppressWarnings("unchecked")
            T take() throws InterruptedException {
                lock.lock();
                try {
                    while (count == 0) notEmpty.await();
                    T t = (T) items[head];
                    items[head] = null;
                    head = (head + 1) % items.length;
                    count--;
                    notFull.signal();
                    return t;
                } finally {
                    lock.unlock();
                }
            }
        }

        static void demo() {
            System.out.println("\n[4] Condition-based producer-consumer (not executed in main).");
            // For brevity not executed in main; call Example4_ConditionProducerConsumer.demo() to try.
            BoundedBuffer<Integer> buf = new BoundedBuffer<>(2);
            Thread p = new Thread(() -> {
                try { for (int i = 0; i < 5; i++) { buf.put(i); log("Produced " + i); } }
                catch (InterruptedException ignored) {}
            }, "Cond-Producer");

            Thread c = new Thread(() -> {
                try { for (int i = 0; i < 5; i++) { Integer v = buf.take(); log("Consumed " + v); } }
                catch (InterruptedException ignored) {}
            }, "Cond-Consumer");

            p.start(); c.start();
            joinQuietly(p); joinQuietly(c);
        }
    }

    // 5) BlockingQueue producer-consumer (simpler)
    static class Example5_BlockingQueueProducerConsumer {
        /*
          Q: Why prefer BlockingQueue for producer-consumer?
          A: Simpler, less error-prone than manual Conditions; handles waiting and wake-ups correctly.
             put/take are interruptible; offer/poll support timeouts.
        */
        static void demo() {
            System.out.println("\n[5] BlockingQueue producer-consumer demo:");
            BlockingQueue<String> q = new ArrayBlockingQueue<>(2, true); // fairness=true: FIFO producers/consumers

            Thread producer = new Thread(() -> {
                try {
                    for (int i = 1; i <= 5; i++) {
                        String item = "job-" + i;
                        q.put(item); // blocks if full
                        log("Produced " + item);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "BQ-Producer");

            Thread consumer = new Thread(() -> {
                try {
                    for (int i = 1; i <= 5; i++) {
                        String item = q.take(); // blocks if empty
                        log("Consumed " + item);
                        sleepMs(30);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "BQ-Consumer");

            producer.start(); consumer.start();
            joinQuietly(producer); joinQuietly(consumer);
        }
    }

    // 6) ReadWriteLock; read vs write, downgrading
    static class Example6_ReadWriteLock {
        /*
          Q: When to use ReadWriteLock?
          A: Many concurrent readers, infrequent writers. Allows concurrent reads, exclusive writes.

          Q: Upgrading and downgrading?
          A:
            - Upgrading (read->write) is unsafe and can deadlock. Avoid.
            - Downgrading (write->read) is safe: acquire read lock before releasing write lock.
        */
        static class RWCache {
            private final ReadWriteLock rw = new ReentrantReadWriteLock();
            private final java.util.Map<String, String> map = new java.util.HashMap<>();

            String get(String k) {
                rw.readLock().lock();
                try { return map.get(k); }
                finally { rw.readLock().unlock(); }
            }

            void put(String k, String v) {
                rw.writeLock().lock();
                try { map.put(k, v); }
                finally { rw.writeLock().unlock(); }
            }

            // Downgrading example
            String upsertWithDowngrade(String k, java.util.function.Function<String, String> compute) {
                rw.writeLock().lock();
                try {
                    String v = compute.apply(map.get(k));
                    map.put(k, v);
                    // downgrade: acquire read lock before releasing write lock
                    rw.readLock().lock();
                    try {
                        // now safe to release write lock while holding read lock
                    } finally {
                        rw.writeLock().unlock();
                    }
                    try {
                        return map.get(k);
                    } finally {
                        rw.readLock().unlock();
                    }
                } catch (RuntimeException e) {
                    throw e;
                }
            }
        }

        static void demo() {
            System.out.println("\n[6] ReadWriteLock demo:");
            RWCache cache = new RWCache();
            cache.put("k", "v0");
            AtomicBoolean stop = new AtomicBoolean(false);

            Runnable reader = () -> {
                while (!stop.get()) {
                    String v = cache.get("k");
                    if (ThreadLocalRandom.current().nextInt(10) == 0) log("Read " + v);
                }
            };
            Runnable writer = () -> {
                int i = 1;
                while (!stop.get() && i <= 3) {
                    cache.put("k", "v" + i++);
                    log("Wrote new value");
                    sleepMs(40);
                }
            };

            Thread r1 = new Thread(reader, "RW-Reader1");
            Thread r2 = new Thread(reader, "RW-Reader2");
            Thread w = new Thread(writer, "RW-Writer");

            r1.start(); r2.start(); w.start();
            joinQuietly(w);
            stop.set(true);
            joinQuietly(r1); joinQuietly(r2);
        }
    }

    // 7) StampedLock; optimistic read
    static class Example7_StampedLockOptimisticRead {
        /*
          Q: StampedLock vs ReadWriteLock?
          A:
            - Supports optimistic reads (no actual locking if no write occurs).
            - Can convert between modes; not reentrant; no Condition support.
            - Must validate optimistic read; if invalid, fallback to read or write lock.

          Caution: Always unlock with the exact stamp; handle finally blocks carefully.
        */
        static class Point {
            private final StampedLock sl = new StampedLock();
            private double x, y;

            void move(double dx, double dy) {
                long stamp = sl.writeLock();
                try { x += dx; y += dy; }
                finally { sl.unlockWrite(stamp); }
            }

            double distanceFromOrigin() {
                long stamp = sl.tryOptimisticRead();
                double cx = x, cy = y; // read without locking
                if (!sl.validate(stamp)) {
                    stamp = sl.readLock();
                    try { cx = x; cy = y; }
                    finally { sl.unlockRead(stamp); }
                }
                return Math.hypot(cx, cy);
            }
        }

        static void demo() {
            System.out.println("\n[7] StampedLock optimistic read demo:");
            Point p = new Point();
            Thread writer = new Thread(() -> {
                for (int i = 0; i < 3; i++) {
                    p.move(1, 1);
                    log("Moved point");
                    sleepMs(30);
                }
            }, "Stamped-Writer");

            Thread reader = new Thread(() -> {
                for (int i = 0; i < 6; i++) {
                    double d = p.distanceFromOrigin();
                    log("Distance " + d);
                    sleepMs(15);
                }
            }, "Stamped-Reader");

            writer.start(); reader.start();
            joinQuietly(writer); joinQuietly(reader);
        }
    }

    // 8) Deadlock and avoidance
    static class Example8_DeadlockAndAvoidance {
        /*
          Q: What is deadlock? How to avoid?
          A: Deadlock occurs when threads hold resources and wait cyclically. Avoid by:
            - Consistent lock ordering
            - tryLock with timeout and backoff
            - Using higher-level constructs (BlockingQueue, concurrent collections)

          Below: Demonstration patterns. Not executed by default to avoid hangs.
        */
        static class Pair {
            final ReentrantLock a = new ReentrantLock();
            final ReentrantLock b = new ReentrantLock();

            // Unsafe: can deadlock if two threads call in opposite order
            void unsafeBoth() {
                a.lock();
                try {
                    sleepMs(10);
                    b.lock();
                    try { /* critical */ }
                    finally { b.unlock(); }
                } finally { a.unlock(); }
            }

            // Safe: establish total order (e.g., by identity hash)
            void safeOrdered(ReentrantLock first, ReentrantLock second) {
                first.lock();
                try {
                    second.lock();
                    try { /* critical */ }
                    finally { second.unlock(); }
                } finally { first.unlock(); }
            }

            // Safe: tryLock with timeout + retry/backoff
            boolean safeTryLockBoth(long timeout, TimeUnit unit) throws InterruptedException {
                long deadline = System.nanoTime() + unit.toNanos(timeout);
                while (System.nanoTime() < deadline) {
                    if (a.tryLock(10, TimeUnit.MILLISECONDS)) {
                        try {
                            if (b.tryLock(10, TimeUnit.MILLISECONDS)) {
                                try { return true; }
                                finally { b.unlock(); }
                            }
                        } finally { a.unlock(); }
                    }
                    Thread.yield();
                }
                return false;
            }
        }

        static void demo() {
            System.out.println("\n[8] Deadlock avoidance demo (not executed in main).");
            Pair p = new Pair();
            Thread t1 = new Thread(() -> p.unsafeBoth(), "Deadlock-T1");
            Thread t2 = new Thread(() -> p.unsafeBoth(), "Deadlock-T2");
            // t1.start(); t2.start(); // Potential deadlock; keep commented.
        }
    }

    // 9) BlockingQueue API semantics
    static class Example9_BlockingQueueAPIDifferences {
        /*
          Q: add vs offer vs put?
          A:
            - add(e): throws IllegalStateException if full
            - offer(e): returns false if full (non-blocking)
            - put(e): blocks until space

          Q: remove vs poll vs take vs element vs peek?
          A:
            - remove(): removes head, throws NoSuchElementException if empty
            - poll(): returns head or null if empty (non-blocking)
            - take(): blocks until element present
            - element(): returns head, throws if empty
            - peek(): returns head or null if empty

          Iterators are weakly consistent; not fail-fast; may miss concurrent changes.
        */
        static void demo() {
            System.out.println("\n[9] BlockingQueue API differences (docs only).");
        }
    }

    // 10) SynchronousQueue: rendezvous
    static class Example10_SynchronousQueueHandoff {
        /*
          Q: What is SynchronousQueue?
          A: A zero-capacity queue; each put must wait for a take and vice versa.
             Useful for handoff designs and direct producer->consumer transfer.
        */
        static void demo() {
            System.out.println("\n[10] SynchronousQueue handoff demo:");
            SynchronousQueue<String> sq = new SynchronousQueue<>();

            Thread consumer = new Thread(() -> {
                try {
                    String v = sq.take(); // waits until producer puts
                    log("Got: " + v);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "SyncQ-Consumer");

            Thread producer = new Thread(() -> {
                try {
                    log("About to put...");
                    sq.put("hello"); // blocks until a consumer takes
                    log("Put completed");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "SyncQ-Producer");

            consumer.start(); sleepMs(20); producer.start();
            joinQuietly(producer); joinQuietly(consumer);
        }
    }

    // 11) PriorityBlockingQueue
    static class Example11_PriorityBlockingQueue {
        /*
          Q: PriorityBlockingQueue characteristics?
          A:
            - Unbounded (grows as needed).
            - Orders elements by natural order or Comparator.
            - No fairness; multiple equal-priority elements order is not stable.
            - take() blocks if empty.

          Use for prioritized tasks, but beware unbounded growth and O(log n) operations.
        */
        static void demo() {
            System.out.println("\n[11] PriorityBlockingQueue demo:");
            PriorityBlockingQueue<Job> pq = new PriorityBlockingQueue<>(11, Comparator.comparingInt(j -> j.priority));
            pq.add(new Job("low", 5));
            pq.add(new Job("high", 1));
            pq.add(new Job("mid", 3));

            System.out.println("Polling by priority (lower int = higher priority):");
            System.out.println(pq.poll());
            System.out.println(pq.poll());
            System.out.println(pq.poll());
        }

        static class Job {
            final String name;
            final int priority;
            Job(String name, int priority) { this.name = name; this.priority = priority; }
            public String toString() { return "Job{" + name + ", p=" + priority + "}"; }
        }
    }

    // 12) DelayQueue
    static class Example12_DelayQueue {
        /*
          Q: DelayQueue?
          A:
            - An unbounded queue of Delayed elements; elements emerge only when their delay has expired.
            - take() blocks until the head is ready.

          Typical use: in-memory scheduler, cache expiry, retry queues with backoff.
        */
        static class DelayedTask implements Delayed {
            final String id;
            final long runAtNanos;
            DelayedTask(String id, long delay, TimeUnit unit) {
                this.id = id;
                this.runAtNanos = System.nanoTime() + unit.toNanos(delay);
            }
            public long getDelay(TimeUnit unit) {
                return unit.convert(runAtNanos - System.nanoTime(), TimeUnit.NANOSECONDS);
            }
            public int compareTo(Delayed o) {
                long diff = this.getDelay(TimeUnit.NANOSECONDS) - o.getDelay(TimeUnit.NANOSECONDS);
                return (diff == 0) ? 0 : (diff < 0 ? -1 : 1);
            }
            public String toString() { return "Task(" + id + ")"; }
        }

        static void demo() {
            System.out.println("\n[12] DelayQueue demo:");
            DelayQueue<DelayedTask> dq = new DelayQueue<>();
            dq.add(new DelayedTask("A", 80, TimeUnit.MILLISECONDS));
            dq.add(new DelayedTask("B", 30, TimeUnit.MILLISECONDS));
            dq.add(new DelayedTask("C", 50, TimeUnit.MILLISECONDS));

            Thread consumer = new Thread(() -> {
                try {
                    for (int i = 0; i < 3; i++) {
                        DelayedTask t = dq.take();
                        log("Took " + t);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "DelayQ-Consumer");

            consumer.start();
            joinQuietly(consumer);
        }
    }

    // 13) LinkedTransferQueue
    static class Example13_LinkedTransferQueue {
        /*
          Q: LinkedTransferQueue?
          A:
            - Unbounded, non-blocking queue supporting transfer: producer can wait for consumer to receive element.
            - transfer(e): blocks until a consumer receives e.
            - tryTransfer(e, timeout): waits bounded time.

          Useful for direct handoff when consumers are known to be present or to enforce synchronous delivery.
        */
        static void demo() {
            System.out.println("\n[13] LinkedTransferQueue demo:");
            LinkedTransferQueue<String> ltq = new LinkedTransferQueue<>();

            Thread consumer = new Thread(() -> {
                sleepMs(30);
                try {
                    String v = ltq.take();
                    log("Consumed " + v);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "LTQ-Consumer");

            Thread producer = new Thread(() -> {
                try {
                    log("Transferring item...");
                    ltq.transfer("ping"); // waits until consumed
                    log("Transfer finished (consumer received)");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "LTQ-Producer");

            consumer.start(); producer.start();
            joinQuietly(producer); joinQuietly(consumer);
        }
    }

    // 14) BlockingDeque: work-stealing
    static class Example14_BlockingDequeWorkStealing {
        /*
          Q: BlockingDeque uses?
          A: Double-ended operations; can implement work-stealing (workers take from head; steal from tail).

          Below is a sketch; not executed in main.
        */
        static void demo() {
            System.out.println("\n[14] BlockingDeque work-stealing (sketch, not executed in main):");
            BlockingDeque<String> deque = new LinkedBlockingDeque<>();
            Runnable worker = () -> {
                try {
                    while (true) {
                        // Prefer local work
                        String task = deque.pollFirst(100, TimeUnit.MILLISECONDS);
                        if (task == null) {
                            // steal from tail (another deque in a real pool)
                            break;
                        }
                        log("Processing " + task);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            };
        }
    }

    // 15) ConcurrentLinkedQueue: non-blocking queue
    static class Example15_ConcurrentLinkedQueueNonBlocking {
        /*
          Q: ConcurrentLinkedQueue?
          A:
            - Lock-free, unbounded, non-blocking queue.
            - Operations do not block; offer/poll return immediately.
            - Good for high-throughput, best-effort buffering, not for backpressure.

          Note: To impose backpressure, use a bounded BlockingQueue.
        */
        static void demo() {
            System.out.println("\n[15] ConcurrentLinkedQueue (not executed in main).");
            ConcurrentLinkedQueue<String> q = new ConcurrentLinkedQueue<>();
            q.offer("a");
            q.offer("b");
            System.out.println(q.poll());
            System.out.println(q.poll());
            System.out.println(q.poll()); // null
        }
    }

    // 16) Poison pill shutdown
    static class Example16_PoisonPillShutdown {
        /*
          Q: How to shutdown consumers cleanly?
          A: Use a poison pill sentinel in a queue to signal termination.
        */
        static void demo() {
            System.out.println("\n[16] Poison pill shutdown demo:");
            BlockingQueue<String> q = new LinkedBlockingQueue<>(10);
            String POISON = "STOP";

            Thread consumer = new Thread(() -> {
                try {
                    while (true) {
                        String s = q.take();
                        if (Objects.equals(s, POISON)) {
                            log("Got poison pill, exiting.");
                            break;
                        }
                        log("Processing " + s);
                    }
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }, "Poison-Consumer");

            Thread producer = new Thread(() -> {
                try {
                    for (int i = 0; i < 3; i++) {
                        q.put("task-" + i);
                    }
                    q.put(POISON);
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }, "Poison-Producer");

            consumer.start(); producer.start();
            joinQuietly(producer); joinQuietly(consumer);
        }
    }

    // 17) Fairness trade-offs
    static class Example17_FairnessTradeoffs {
        /*
          Q: What is fairness in locks/queues?
          A:
            - Fair locks/queues grant access roughly in arrival order (FIFO).
            - Reduces starvation risk but can lower throughput due to less batching and more context switches.
            - Non-fair is default and often faster.

          Many implementations expose fairness flag (e.g., ReentrantLock(true), ArrayBlockingQueue(cap, true)).
        */
        static void demo() {
            System.out.println("\n[17] Fairness trade-offs (docs only).");
        }
    }

    // 18) Backpressure with bounded queues
    static class Example18_BackpressureWithBoundedQueue {
        /*
          Q: How to implement backpressure?
          A: Use bounded BlockingQueue. Producers block on put/offer(timeout) when full; consumers catch up.
        */
        static void demo() {
            System.out.println("\n[18] Backpressure with bounded queue (not executed in main).");
            BlockingQueue<String> q = new ArrayBlockingQueue<>(1);
            Thread prod = new Thread(() -> {
                try {
                    for (int i = 0; i < 3; i++) {
                        if (!q.offer("item-" + i, 50, TimeUnit.MILLISECONDS)) {
                            log("Backpressure: could not enqueue item-" + i + " in time");
                        } else {
                            log("Enqueued item-" + i);
                        }
                    }
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
            Thread cons = new Thread(() -> {
                try {
                    while (true) {
                        String v = q.poll(100, TimeUnit.MILLISECONDS);
                        if (v == null) break;
                        log("Consumed " + v);
                        sleepMs(70);
                    }
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
            // prod.start(); cons.start(); // optional
        }
    }

    // 19) drainTo bulk draining
    static class Example19_DrainTo {
        /*
          Q: What does drainTo do?
          A:
            - Removes as many elements as possible and adds them to the provided collection.
            - Useful for batch processing and minimizing lock acquisitions.
        */
        static void demo() {
            System.out.println("\n[19] drainTo demo:");
            BlockingQueue<String> q = new LinkedBlockingQueue<>();
            for (int i = 0; i < 7; i++) q.add("x" + i);
            List<String> batch = new ArrayList<>();
            int n = q.drainTo(batch, 5);
            System.out.println("Drained " + n + ": " + batch);
            System.out.println("Remaining size: " + q.size());
        }
    }

    // Helper to join threads quietly
    private static void joinQuietly(Thread t) {
        try { t.join(2000); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }
}