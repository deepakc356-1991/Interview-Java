package _08_05_concurrency_utilities_locks_queues;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

/**
 * Concurrency Utilities (Locks, Queues)
 * This file demonstrates core patterns and classes:
 * - Locks: ReentrantLock (basic, tryLock, lockInterruptibly), Condition, ReentrantReadWriteLock, StampedLock, deadlock avoidance
 * - Queues: BlockingQueue (Array/Linked/Priority/Delay/Synchronous/LinkedTransfer), Concurrent queues (ConcurrentLinkedQueue/Deque)
 */
public class _02_Examples {

    public static void main(String[] args) throws Exception {
        System.out.println("== ReentrantLock basics, tryLock, interruptible, reentrancy ==");
        ReentrantLockBasicsDemo.demo();

        System.out.println("\n== Condition + BoundedBuffer (classic producer/consumer) ==");
        ConditionBoundedBufferDemo.demo();

        System.out.println("\n== ReentrantReadWriteLock (read concurrency, write exclusivity, downgrade) ==");
        ReadWriteLockDemo.demo();

        System.out.println("\n== StampedLock (optimistic reads, conversion, write) ==");
        StampedLockDemo.demo();

        System.out.println("\n== Deadlock avoidance with tryLock + backoff ==");
        DeadlockAvoidanceDemo.demo();

        System.out.println("\n== LinkedBlockingQueue producer/consumer ==");
        BlockingQueueProducerConsumerDemo.demo();

        System.out.println("\n== ArrayBlockingQueue (capacity, offer timeout, put blocking) ==");
        ArrayBlockingQueueTimeoutDemo.demo();

        System.out.println("\n== PriorityBlockingQueue (priority scheduling) ==");
        PriorityBlockingQueueDemo.demo();

        System.out.println("\n== DelayQueue (time-based scheduling) ==");
        DelayQueueDemo.demo();

        System.out.println("\n== SynchronousQueue (rendezvous/handoff) ==");
        SynchronousQueueDemo.demo();

        System.out.println("\n== LinkedTransferQueue (direct transfer) ==");
        LinkedTransferQueueDemo.demo();

        System.out.println("\n== ConcurrentLinkedQueue (lock-free, non-blocking) ==");
        ConcurrentLinkedQueueDemo.demo();

        System.out.println("\n== ConcurrentLinkedDeque (double-ended concurrent queue) ==");
        ConcurrentLinkedDequeDemo.demo();

        System.out.println("\nAll demos done.");
    }

    // Utility logger
    static void log(String msg) {
        System.out.printf("[%s] %s%n", Thread.currentThread().getName(), msg);
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    // 1) ReentrantLock: basic lock/unlock, tryLock, lockInterruptibly, reentrancy
    static class ReentrantLockBasicsDemo {
        static void demo() throws InterruptedException {
            ReentrantLock lock = new ReentrantLock();

            // Hold the lock and show tryLock and lockInterruptibly from other threads.
            lock.lock();
            try {
                log("Main acquired lock");

                Thread tTry = new Thread(() -> {
                    try {
                        log("tryLock with timeout...");
                        if (lock.tryLock(100, TimeUnit.MILLISECONDS)) {
                            try {
                                log("tryLock acquired (unexpected if main holds long)");
                            } finally {
                                lock.unlock();
                            }
                        } else {
                            log("tryLock timed out (expected while main holds lock)");
                        }
                    } catch (InterruptedException e) {
                        log("tryLock interrupted");
                        Thread.currentThread().interrupt();
                    }
                }, "tryLock-thread");

                Thread tInt = new Thread(() -> {
                    try {
                        log("lockInterruptibly waiting...");
                        lock.lockInterruptibly(); // responds to interrupts while waiting
                        try {
                            log("lockInterruptibly acquired");
                        } finally {
                            lock.unlock();
                        }
                    } catch (InterruptedException e) {
                        log("Interrupted while waiting for lock (as expected)");
                        Thread.currentThread().interrupt();
                    }
                }, "interruptible-thread");

                tTry.start();
                tInt.start();

                sleep(150); // let tTry timeout
                tInt.interrupt(); // interrupt second waiter
                tTry.join();
                tInt.join();
            } finally {
                lock.unlock();
                log("Main released lock");
            }

            // Now lock is free; tryLock will succeed
            Thread tGot = new Thread(() -> {
                if (lock.tryLock()) {
                    try { log("tryLock acquired after main released"); }
                    finally { lock.unlock(); }
                } else {
                    log("tryLock failed unexpectedly");
                }
            }, "tryLock-after-release");
            tGot.start();
            tGot.join();

            // Reentrancy: the same thread can acquire the same lock multiple times
            lock.lock();
            try {
                log("Acquired lock first time");
                nested(lock); // acquires second time
            } finally {
                lock.unlock();
                log("Released lock final time");
            }
        }

        static void nested(ReentrantLock lock) {
            lock.lock();
            try {
                log("Acquired lock second time (reentrant)");
            } finally {
                lock.unlock();
                log("Released one reentrant level");
            }
        }
    }

    // 2) Condition + BoundedBuffer (producer/consumer)
    static class ConditionBoundedBufferDemo {
        // Classic bounded buffer built using ReentrantLock + two Conditions: notFull, notEmpty
        static class BoundedBuffer<T> {
            private final T[] items;
            private int head = 0, tail = 0, count = 0;
            private final ReentrantLock lock = new ReentrantLock();
            private final Condition notEmpty = lock.newCondition();
            private final Condition notFull = lock.newCondition();

            @SuppressWarnings("unchecked")
            BoundedBuffer(int capacity) {
                this.items = (T[]) new Object[capacity];
            }

            public void put(T item) throws InterruptedException {
                lock.lock();
                try {
                    while (count == items.length) {
                        notFull.await(); // wait until space available
                    }
                    items[tail] = item;
                    tail = (tail + 1) % items.length;
                    count++;
                    notEmpty.signal(); // signal a waiting consumer
                } finally {
                    lock.unlock();
                }
            }

            public T take() throws InterruptedException {
                lock.lock();
                try {
                    while (count == 0) {
                        notEmpty.await(); // wait until an item is available
                    }
                    T item = items[head];
                    items[head] = null;
                    head = (head + 1) % items.length;
                    count--;
                    notFull.signal(); // signal a waiting producer
                    return item;
                } finally {
                    lock.unlock();
                }
            }
        }

        static void demo() throws InterruptedException {
            BoundedBuffer<Integer> buf = new BoundedBuffer<>(2);

            Thread producer1 = new Thread(() -> {
                try {
                    for (int i = 1; i <= 5; i++) {
                        buf.put(i);
                        log("producer-1 put " + i);
                        sleep(50);
                    }
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }, "producer-1");

            Thread producer2 = new Thread(() -> {
                try {
                    for (int i = 6; i <= 10; i++) {
                        buf.put(i);
                        log("producer-2 put " + i);
                        sleep(70);
                    }
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }, "producer-2");

            Thread consumer = new Thread(() -> {
                try {
                    for (int i = 0; i < 10; i++) {
                        Integer v = buf.take();
                        log("consumer took " + v);
                    }
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }, "consumer");

            consumer.start();
            producer1.start();
            producer2.start();

            producer1.join();
            producer2.join();
            consumer.join();
        }
    }

    // 3) ReentrantReadWriteLock: multiple readers, exclusive writer, safe downgrade
    static class ReadWriteLockDemo {
        static class Counter {
            private final ReentrantReadWriteLock rw = new ReentrantReadWriteLock();
            private final Lock r = rw.readLock();
            private final Lock w = rw.writeLock();
            private int value;

            int read() {
                r.lock();
                try { return value; }
                finally { r.unlock(); }
            }

            // Increment under write lock, then downgrade to read lock for a consistent post-read
            void writeWithDowngrade(int delta) {
                w.lock();
                try {
                    value += delta;
                    log("writer updated to " + value + " (holding write)");
                    r.lock(); // acquire read before releasing write (downgrade)
                } finally {
                    w.unlock();
                }
                try {
                    log("writer downgraded to read, sees " + value);
                } finally {
                    r.unlock();
                }
            }
        }

        static void demo() throws InterruptedException {
            Counter c = new Counter();

            Runnable readerTask = () -> {
                for (int i = 0; i < 5; i++) {
                    int v = c.read();
                    log("reader read " + v);
                    sleep(40);
                }
            };

            Thread r1 = new Thread(readerTask, "reader-1");
            Thread r2 = new Thread(readerTask, "reader-2");

            Thread writer = new Thread(() -> {
                for (int i = 0; i < 5; i++) {
                    c.writeWithDowngrade(1);
                    sleep(60);
                }
            }, "writer");

            r1.start();
            r2.start();
            writer.start();

            r1.join();
            r2.join();
            writer.join();
        }
    }

    // 4) StampedLock: optimistic reads, write, and lock conversion
    static class StampedLockDemo {
        static class Point {
            private final StampedLock sl = new StampedLock();
            private double x, y;

            void move(double dx, double dy) {
                long stamp = sl.writeLock();
                try { x += dx; y += dy; }
                finally { sl.unlockWrite(stamp); }
            }

            double distanceFromOrigin() {
                long stamp = sl.tryOptimisticRead(); // optimistic
                double cx = x, cy = y;
                if (!sl.validate(stamp)) { // fall back if write occurred
                    stamp = sl.readLock();
                    try { cx = x; cy = y; }
                    finally { sl.unlockRead(stamp); }
                }
                return Math.hypot(cx, cy);
            }

            void moveIfAtOrigin(double newX, double newY) {
                long stamp = sl.readLock();
                try {
                    while (x == 0.0 && y == 0.0) {
                        long ws = sl.tryConvertToWriteLock(stamp); // attempt upgrade
                        if (ws != 0L) {
                            stamp = ws; // now in write mode
                            x = newX; y = newY;
                            break;
                        } else {
                            sl.unlockRead(stamp);
                            stamp = sl.writeLock(); // acquire write exclusively
                        }
                    }
                } finally {
                    sl.unlock(stamp); // unlock read or write using the stamp
                }
            }
        }

        static void demo() throws InterruptedException {
            Point p = new Point();

            Thread reader1 = new Thread(() -> {
                for (int i = 0; i < 4; i++) {
                    double d = p.distanceFromOrigin();
                    log("distance=" + String.format("%.2f", d));
                    sleep(50);
                }
            }, "stamped-reader-1");

            Thread reader2 = new Thread(() -> {
                p.moveIfAtOrigin(1, 1);
                for (int i = 0; i < 4; i++) {
                    double d = p.distanceFromOrigin();
                    log("distance=" + String.format("%.2f", d));
                    sleep(50);
                }
            }, "stamped-reader-2");

            Thread writer = new Thread(() -> {
                for (int i = 0; i < 3; i++) {
                    p.move(1, 0.5);
                    log("moved point");
                    sleep(70);
                }
            }, "stamped-writer");

            reader1.start();
            reader2.start();
            writer.start();

            reader1.join();
            reader2.join();
            writer.join();
        }
    }

    // 5) Deadlock avoidance using tryLock + backoff
    static class DeadlockAvoidanceDemo {
        static void demo() throws InterruptedException {
            ReentrantLock l1 = new ReentrantLock();
            ReentrantLock l2 = new ReentrantLock();

            Runnable taskA = () -> doWorkWithBoth(l1, l2, "A");
            Runnable taskB = () -> doWorkWithBoth(l2, l1, "B"); // opposite order

            Thread t1 = new Thread(taskA, "deadlock-avoid-A");
            Thread t2 = new Thread(taskB, "deadlock-avoid-B");

            t1.start();
            t2.start();

            t1.join();
            t2.join();
        }

        static void doWorkWithBoth(ReentrantLock first, ReentrantLock second, String name) {
            try {
                while (true) {
                    // try to acquire both with timeouts; release and retry if second fails
                    if (first.tryLock(50, TimeUnit.MILLISECONDS)) {
                        try {
                            if (second.tryLock(50, TimeUnit.MILLISECONDS)) {
                                try {
                                    log("Task " + name + " acquired both locks; doing work");
                                    sleep(80);
                                    return; // done
                                } finally {
                                    second.unlock();
                                    first.unlock();
                                }
                            }
                        } finally {
                            if (first.isHeldByCurrentThread()) first.unlock();
                        }
                    }
                    // backoff
                    Thread.yield();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // 6) LinkedBlockingQueue: producer/consumer
    static class BlockingQueueProducerConsumerDemo {
        static void demo() throws InterruptedException {
            BlockingQueue<Integer> q = new LinkedBlockingQueue<>();

            Thread consumer = new Thread(() -> {
                try {
                    int sum = 0;
                    for (int i = 0; i < 8; i++) {
                        int v = q.take(); // blocks until available
                        log("took " + v);
                        sum += v;
                    }
                    log("sum=" + sum);
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }, "lbq-consumer");

            Thread producer = new Thread(() -> {
                try {
                    for (int i = 1; i <= 8; i++) {
                        q.put(i); // unbounded -> never blocks here
                        log("put " + i);
                        sleep(30);
                    }
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }, "lbq-producer");

            consumer.start();
            producer.start();
            producer.join();
            consumer.join();
        }
    }

    // 7) ArrayBlockingQueue: capacity + offer timeout + put blocking
    static class ArrayBlockingQueueTimeoutDemo {
        static void demo() throws InterruptedException {
            ArrayBlockingQueue<Integer> q = new ArrayBlockingQueue<>(2); // capacity=2

            q.put(1);
            q.put(2);
            log("queue filled (2 items)");

            boolean offered = q.offer(3, 100, TimeUnit.MILLISECONDS); // times out if full
            log("offer(3, 100ms) -> " + offered + " (expected false)");

            Thread putter = new Thread(() -> {
                try {
                    log("putter trying put(3) (will block until space)...");
                    q.put(3); // blocks
                    log("putter succeeded put(3)");
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }, "abq-putter");

            Thread taker = new Thread(() -> {
                try {
                    sleep(150);
                    Integer v = q.take(); // frees a slot
                    log("taker took " + v);
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }, "abq-taker");

            putter.start();
            taker.start();
            putter.join();
            taker.join();
        }
    }

    // 8) PriorityBlockingQueue: items are ordered by priority
    static class PriorityBlockingQueueDemo {
        static class Job {
            final String name;
            final int priority; // higher is more important
            Job(String name, int priority) { this.name = name; this.priority = priority; }
            public String toString() { return name + "(p=" + priority + ")"; }
        }

        static void demo() throws InterruptedException {
            PriorityBlockingQueue<Job> pq =
                new PriorityBlockingQueue<>(11, Comparator.comparingInt((Job j) -> j.priority).reversed());

            Thread consumer = new Thread(() -> {
                try {
                    for (int i = 0; i < 3; i++) {
                        Job j = pq.take(); // waits if empty
                        log("took " + j);
                    }
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }, "pbq-consumer");

            consumer.start();

            pq.put(new Job("low", 1));
            pq.put(new Job("mid", 5));
            sleep(100);
            pq.put(new Job("high", 10));

            consumer.join();
        }
    }

    // 9) DelayQueue: elements available only after their delay has elapsed
    static class DelayQueueDemo {
        static class DelayedTask implements Delayed {
            final String name;
            final long readyAtNanos;
            DelayedTask(String name, long delay, TimeUnit unit) {
                this.name = name;
                this.readyAtNanos = System.nanoTime() + unit.toNanos(delay);
            }
            @Override public long getDelay(TimeUnit unit) {
                long diff = readyAtNanos - System.nanoTime();
                return unit.convert(diff, TimeUnit.NANOSECONDS);
            }
            @Override public int compareTo(Delayed o) {
                return Long.compare(this.readyAtNanos, ((DelayedTask) o).readyAtNanos);
            }
            public String toString() { return name; }
        }

        static void demo() throws InterruptedException {
            DelayQueue<DelayedTask> dq = new DelayQueue<>();
            dq.put(new DelayedTask("task-200ms", 200, TimeUnit.MILLISECONDS));
            dq.put(new DelayedTask("task-50ms", 50, TimeUnit.MILLISECONDS));
            dq.put(new DelayedTask("task-100ms", 100, TimeUnit.MILLISECONDS));

            Thread consumer = new Thread(() -> {
                try {
                    for (int i = 0; i < 3; i++) {
                        DelayedTask t = dq.take(); // waits until delay elapsed
                        log("took " + t);
                    }
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }, "delay-consumer");

            consumer.start();
            consumer.join();
        }
    }

    // 10) SynchronousQueue: no capacity; put waits for a take and vice versa
    static class SynchronousQueueDemo {
        static void demo() throws InterruptedException {
            SynchronousQueue<String> q = new SynchronousQueue<>();

            Thread consumer = new Thread(() -> {
                try {
                    log("consumer waiting to take...");
                    String v = q.take(); // waits for producer
                    log("consumer took " + v);
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }, "sync-consumer");

            consumer.start();
            sleep(100);
            log("producer putting 'hello' (will unblock consumer)...");
            q.put("hello"); // blocks until consumer receives
            log("producer put completed");
            consumer.join();
        }
    }

    // 11) LinkedTransferQueue: transfer waits until consumer receives
    static class LinkedTransferQueueDemo {
        static void demo() throws InterruptedException {
            LinkedTransferQueue<String> q = new LinkedTransferQueue<>();

            boolean immediate = q.tryTransfer("no-consumer");
            log("tryTransfer without consumer -> " + immediate + " (expected false)");

            Thread consumer = new Thread(() -> {
                try {
                    sleep(200);
                    log("consumer ready to take");
                    String v = q.take();
                    log("consumer took " + v);
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }, "ltq-consumer");

            Thread producer = new Thread(() -> {
                try {
                    log("producer transferring 'direct' (waits until consumer takes)...");
                    q.transfer("direct");
                    log("producer transfer completed");
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }, "ltq-producer");

            consumer.start();
            producer.start();
            consumer.join();
            producer.join();
        }
    }

    // 12) ConcurrentLinkedQueue: non-blocking, lock-free
    static class ConcurrentLinkedQueueDemo {
        static void demo() throws InterruptedException {
            ConcurrentLinkedQueue<Integer> q = new ConcurrentLinkedQueue<>();
            CountDownLatch done = new CountDownLatch(2);

            Thread p1 = new Thread(() -> {
                for (int i = 1; i <= 5; i++) {
                    q.add(i);
                    log("p1 added " + i);
                    sleep(20);
                }
                done.countDown();
            }, "clq-p1");

            Thread p2 = new Thread(() -> {
                for (int i = 6; i <= 10; i++) {
                    q.offer(i);
                    log("p2 added " + i);
                    sleep(25);
                }
                done.countDown();
            }, "clq-p2");

            Thread consumer = new Thread(() -> {
                int sum = 0;
                while (true) {
                    Integer v = q.poll(); // returns null if empty
                    if (v != null) {
                        sum += v;
                        log("polled " + v);
                    } else if (done.getCount() == 0) {
                        break;
                    } else {
                        sleep(10);
                    }
                }
                log("sum=" + sum + " (size now " + q.size() + ")");
            }, "clq-consumer");

            consumer.start();
            p1.start();
            p2.start();

            p1.join();
            p2.join();
            consumer.join();
        }
    }

    // 13) ConcurrentLinkedDeque: double-ended concurrent queue
    static class ConcurrentLinkedDequeDemo {
        static void demo() throws InterruptedException {
            ConcurrentLinkedDeque<String> dq = new ConcurrentLinkedDeque<>();
            dq.addFirst("A");
            dq.addLast("B");
            dq.addFirst("C"); // deque now: C, A, B

            Thread t1 = new Thread(() -> {
                String x = dq.pollFirst();
                log("pollFirst -> " + x);
                x = dq.pollFirst();
                log("pollFirst -> " + x);
            }, "cld-t1");

            Thread t2 = new Thread(() -> {
                String x = dq.pollLast();
                log("pollLast -> " + x);
                x = dq.pollLast();
                log("pollLast -> " + x);
            }, "cld-t2");

            t1.start();
            t2.start();
            t1.join();
            t2.join();
        }
    }
}