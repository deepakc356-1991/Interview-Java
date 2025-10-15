package _08_02_synchronization_and_locks;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.*;

/**
 * Synchronization & Locks: runnable, commented examples in one file.
 * Run main to see each example.
 */
public class _02_Examples {

    public static void main(String[] args) throws Exception {
        banner("Example 1: synchronized methods and blocks (intrinsic locks)");
        Example1_SynchronizedMethodsAndBlocks.run();

        banner("Example 2: instance vs static synchronized (different monitors)");
        Example2_InstanceVsStaticSynchronized.run();

        banner("Example 3: fine-grained locking with private lock objects");
        Example3_FineGrainedLocking.run();

        banner("Example 4: Reentrancy of intrinsic and explicit locks");
        Example4_Reentrancy.run();

        banner("Example 5: wait/notify/notifyAll with intrinsic locks (bounded buffer)");
        Example5_WaitNotify_BoundedBuffer.run();

        banner("Example 6: ReentrantLock basics: tryLock, lockInterruptibly (unfair)");
        Example6_ReentrantLock_Basics.run();

        banner("Example 7: ReentrantLock with Conditions (bounded buffer)");
        Example7_Conditions_BoundedBuffer.run();

        banner("Example 8: ReadWriteLock (multiple readers, single writer)");
        Example8_ReadWriteLock.run();

        banner("Example 9: StampedLock (optimistic read, upgrade to read)");
        Example9_StampedLock.run();

        banner("Example 10: Deadlock demonstration and avoidance");
        Example10_DeadlockAndAvoidance.run();

        banner("Example 11: Fair vs Unfair ReentrantLock");
        Example11_FairVsUnfairLocks.run();

        banner("Example 12: Synchronized collections and external synchronization for iteration");
        Example12_SynchronizedCollections.run();

        banner("Example 13: Double-checked locking with volatile for lazy init (Singleton)");
        Example13_DoubleCheckedLocking.run();

        banner("All examples complete.");
    }

    // Helpers
    static void banner(String title) { System.out.println("\n=== " + title + " ==="); }
    static void log(String msg) { System.out.printf("[%s] %s%n", Thread.currentThread().getName(), msg); }
    static void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }

    // --------------------------------------------------------------------------------------------

    static class Example1_SynchronizedMethodsAndBlocks {
        // Unsafe counter (race condition)
        static class CounterUnsafe {
            private int count;
            void inc() { count++; } // not synchronized
            int get() { return count; }
        }
        // Synchronized method
        static class CounterSyncMethod {
            private int count;
            synchronized void inc() { count++; }
            synchronized int get() { return count; }
        }
        // Synchronized block on 'this'
        static class CounterSyncBlock {
            private int count;
            void inc() { synchronized (this) { count++; } }
            int get() { synchronized (this) { return count; } }
        }
        static void run() throws InterruptedException {
            int threads = 4, iters = 10_000;

            // Unsafe
            CounterUnsafe cu = new CounterUnsafe();
            runThreads(threads, () -> { for (int i = 0; i < iters; i++) cu.inc(); });
            log("Unsafe count (expected < " + (threads * iters) + " due to lost updates): " + cu.get());

            // Synchronized method
            CounterSyncMethod csm = new CounterSyncMethod();
            runThreads(threads, () -> { for (int i = 0; i < iters; i++) csm.inc(); });
            log("Synchronized method count: " + csm.get());

            // Synchronized block
            CounterSyncBlock csb = new CounterSyncBlock();
            runThreads(threads, () -> { for (int i = 0; i < iters; i++) csb.inc(); });
            log("Synchronized block count: " + csb.get());
        }
        static void runThreads(int n, Runnable r) throws InterruptedException {
            Thread[] ts = new Thread[n];
            for (int i = 0; i < n; i++) ts[i] = new Thread(r, "t" + i);
            for (Thread t : ts) t.start();
            for (Thread t : ts) t.join();
        }
    }

    // --------------------------------------------------------------------------------------------

    static class Example2_InstanceVsStaticSynchronized {
        static class DualLock {
            // Locks on 'this'
            synchronized void instanceMethod() {
                log("entered instanceMethod");
                sleep(200);
                log("exiting instanceMethod");
            }
            // Locks on DualLock.class
            static synchronized void staticMethod() {
                log("entered staticMethod");
                sleep(200);
                log("exiting staticMethod");
            }
        }
        static void run() throws InterruptedException {
            DualLock a = new DualLock();
            DualLock b = new DualLock();

            CountDownLatch start1 = new CountDownLatch(1);
            Thread t1 = new Thread(() -> { await(start1); a.instanceMethod(); }, "inst-A");
            Thread t2 = new Thread(() -> { await(start1); b.instanceMethod(); }, "inst-B");
            t1.start(); t2.start(); start1.countDown(); t1.join(); t2.join();

            CountDownLatch start2 = new CountDownLatch(1);
            Thread t3 = new Thread(() -> { await(start2); DualLock.staticMethod(); }, "static-1");
            Thread t4 = new Thread(() -> { await(start2); DualLock.staticMethod(); }, "static-2");
            t3.start(); t4.start(); start2.countDown(); t3.join(); t4.join();
        }
        static void await(CountDownLatch l) { try { l.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }
    }

    // --------------------------------------------------------------------------------------------

    static class Example3_FineGrainedLocking {
        static class TwoState {
            private int a, b;
            private final Object lockA = new Object();
            private final Object lockB = new Object();

            void incA() { synchronized (lockA) { a++; } }
            void incB() { synchronized (lockB) { b++; } }

            // Snapshot both with consistent lock order to avoid deadlock
            int[] snapshot() {
                synchronized (lockA) {
                    synchronized (lockB) {
                        return new int[]{a, b};
                    }
                }
            }
        }
        static void run() throws InterruptedException {
            TwoState s = new TwoState();
            Thread t1 = new Thread(() -> { for (int i = 0; i < 5_000; i++) s.incA(); }, "incA");
            Thread t2 = new Thread(() -> { for (int i = 0; i < 5_000; i++) s.incB(); }, "incB");
            t1.start(); t2.start(); t1.join(); t2.join();
            int[] snap = s.snapshot();
            log("Final a=" + snap[0] + ", b=" + snap[1] + " (independent locks allow concurrency)");
        }
    }

    // --------------------------------------------------------------------------------------------

    static class Example4_Reentrancy {
        static class Intrinsic {
            synchronized void outer() {
                log("outer acquired");
                inner(); // re-enter same monitor (this)
                log("outer done");
            }
            synchronized void inner() {
                log("inner acquired");
            }
        }
        static class Explicit {
            private final ReentrantLock lock = new ReentrantLock();
            void outer() {
                lock.lock();
                try {
                    log("outer lock holdCount=" + lock.getHoldCount());
                    inner(); // re-enter the same ReentrantLock
                    log("after inner holdCount=" + lock.getHoldCount());
                } finally { lock.unlock(); }
            }
            void inner() {
                lock.lock();
                try { log("inner lock holdCount=" + lock.getHoldCount()); }
                finally { lock.unlock(); }
            }
        }
        static void run() throws InterruptedException {
            Intrinsic i = new Intrinsic();
            Explicit e = new Explicit();
            Thread ti = new Thread(i::outer, "intrinsic");
            Thread te = new Thread(e::outer, "explicit");
            ti.start(); te.start(); ti.join(); te.join();
        }
    }

    // --------------------------------------------------------------------------------------------

    static class Example5_WaitNotify_BoundedBuffer {
        // Bounded buffer using intrinsic locks and wait/notifyAll
        static class BoundedBuffer<E> {
            private final Queue<E> q = new ArrayDeque<>();
            private final int capacity;
            BoundedBuffer(int capacity) { this.capacity = capacity; }

            public synchronized void put(E e) throws InterruptedException {
                while (q.size() == capacity) wait(); // guard with while (spurious wakeups)
                q.add(e);
                notifyAll();
            }

            public synchronized E take() throws InterruptedException {
                while (q.isEmpty()) wait();
                E e = q.remove();
                notifyAll();
                return e;
            }
        }
        static void run() throws InterruptedException {
            BoundedBuffer<Integer> buf = new BoundedBuffer<>(2);
            Thread producer = new Thread(() -> {
                try {
                    for (int i = 1; i <= 5; i++) {
                        buf.put(i);
                        log("produced " + i);
                        sleep(60);
                    }
                } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            }, "producer");

            Thread consumer = new Thread(() -> {
                try {
                    for (int i = 1; i <= 5; i++) {
                        Integer x = buf.take();
                        log("consumed " + x);
                        sleep(100);
                    }
                } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            }, "consumer");

            producer.start(); consumer.start(); producer.join(); consumer.join();
        }
    }

    // --------------------------------------------------------------------------------------------

    static class Example6_ReentrantLock_Basics {
        static void run() throws InterruptedException {
            ReentrantLock lock = new ReentrantLock(); // unfair by default

            CountDownLatch start = new CountDownLatch(1);

            Thread t1 = new Thread(() -> {
                await(start);
                lock.lock();
                try {
                    log("t1 acquired lock");
                    sleep(300);
                } finally {
                    lock.unlock();
                    log("t1 released lock");
                }
            }, "t1");

            Thread t2 = new Thread(() -> {
                await(start);
                try {
                    if (lock.tryLock(100, TimeUnit.MILLISECONDS)) {
                        try { log("t2 acquired lock via tryLock"); }
                        finally { lock.unlock(); log("t2 released lock"); }
                    } else {
                        log("t2 could not acquire lock within timeout");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "t2");

            Thread t3 = new Thread(() -> {
                await(start);
                try {
                    lock.lockInterruptibly(); // responds to interrupt while waiting
                    try { log("t3 acquired lock"); }
                    finally { lock.unlock(); log("t3 released lock"); }
                } catch (InterruptedException e) {
                    log("t3 interrupted while waiting for lock");
                    Thread.currentThread().interrupt();
                }
            }, "t3");

            t1.start(); t2.start(); t3.start();
            start.countDown();
            sleep(120); // ensure t3 is waiting
            t3.interrupt(); // interruptible acquisition

            t1.join(); t2.join(); t3.join();
        }
        static void await(CountDownLatch l) { try { l.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }
    }

    // --------------------------------------------------------------------------------------------

    static class Example7_Conditions_BoundedBuffer {
        // Bounded buffer using ReentrantLock + Conditions (notFull/notEmpty)
        static class BoundedBuffer<E> {
            private final Queue<E> q = new ArrayDeque<>();
            private final int capacity;
            private final ReentrantLock lock = new ReentrantLock();
            private final Condition notFull = lock.newCondition();
            private final Condition notEmpty = lock.newCondition();
            BoundedBuffer(int capacity) { this.capacity = capacity; }

            public void put(E e) throws InterruptedException {
                lock.lock();
                try {
                    while (q.size() == capacity) notFull.await();
                    q.add(e);
                    notEmpty.signal(); // wake a waiter for notEmpty
                } finally { lock.unlock(); }
            }

            public E take() throws InterruptedException {
                lock.lock();
                try {
                    while (q.isEmpty()) notEmpty.await();
                    E e = q.remove();
                    notFull.signal();
                    return e;
                } finally { lock.unlock(); }
            }
        }
        static void run() throws InterruptedException {
            BoundedBuffer<Integer> buf = new BoundedBuffer<>(2);

            Thread producer = new Thread(() -> {
                try {
                    for (int i = 1; i <= 5; i++) {
                        buf.put(i);
                        log("produced " + i);
                        sleep(50);
                    }
                } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            }, "producer2");

            Thread consumer = new Thread(() -> {
                try {
                    for (int i = 1; i <= 5; i++) {
                        Integer x = buf.take();
                        log("consumed " + x);
                        sleep(90);
                    }
                } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            }, "consumer2");

            producer.start(); consumer.start(); producer.join(); consumer.join();
        }
    }

    // --------------------------------------------------------------------------------------------

    static class Example8_ReadWriteLock {
        static class KeyValueStore {
            private final Map<String, Integer> map = new HashMap<>();
            private final ReentrantReadWriteLock rw = new ReentrantReadWriteLock();
            private final Lock r = rw.readLock();
            private final Lock w = rw.writeLock();

            int get(String k) {
                r.lock();
                try { return map.getOrDefault(k, 0); }
                finally { r.unlock(); }
            }
            void put(String k, int v) {
                w.lock();
                try { map.put(k, v); }
                finally { w.unlock(); }
            }
        }
        static void run() throws InterruptedException {
            KeyValueStore store = new KeyValueStore();
            store.put("x", 0);

            Runnable reader = () -> {
                for (int i = 0; i < 5; i++) {
                    int v = store.get("x");
                    log("read x=" + v);
                    sleep(50);
                }
            };

            Runnable writer = () -> {
                for (int i = 1; i <= 5; i++) {
                    store.put("x", i);
                    log("wrote x=" + i);
                    sleep(80);
                }
            };

            Thread r1 = new Thread(reader, "reader-1");
            Thread r2 = new Thread(reader, "reader-2");
            Thread r3 = new Thread(reader, "reader-3");
            Thread w = new Thread(writer, "writer");

            r1.start(); r2.start(); r3.start(); w.start();
            r1.join(); r2.join(); r3.join(); w.join();
        }
    }

    // --------------------------------------------------------------------------------------------

    static class Example9_StampedLock {
        static class Point {
            private double x, y;
            private final StampedLock sl = new StampedLock();

            void move(double dx, double dy) {
                long stamp = sl.writeLock();
                try { x += dx; y += dy; }
                finally { sl.unlockWrite(stamp); }
            }

            double distanceFromOrigin() {
                long stamp = sl.tryOptimisticRead();
                double cx = x, cy = y;
                if (!sl.validate(stamp)) {
                    stamp = sl.readLock();
                    try { cx = x; cy = y; }
                    finally { sl.unlockRead(stamp); }
                }
                return Math.hypot(cx, cy);
            }

            // Example of upgrading read to write (careful with conversion)
            void moveIfAtOrigin(double newX, double newY) {
                long stamp = sl.readLock();
                try {
                    while (x == 0.0 && y == 0.0) {
                        long ws = sl.tryConvertToWriteLock(stamp);
                        if (ws != 0L) { // upgrade succeeded
                            stamp = ws;
                            x = newX; y = newY;
                            break;
                        } else {
                            sl.unlockRead(stamp);
                            stamp = sl.writeLock();
                        }
                    }
                } finally { sl.unlock(stamp); }
            }
        }
        static void run() throws InterruptedException {
            Point p = new Point();
            Thread writer = new Thread(() -> {
                for (int i = 0; i < 5; i++) {
                    p.move(1, 1);
                    log("moved point");
                    sleep(60);
                }
            }, "stamped-writer");

            Thread reader = new Thread(() -> {
                for (int i = 0; i < 8; i++) {
                    double d = p.distanceFromOrigin();
                    log("distance=" + String.format("%.2f", d));
                    sleep(40);
                }
            }, "stamped-reader");

            writer.start(); reader.start(); writer.join(); reader.join();
        }
    }

    // --------------------------------------------------------------------------------------------

    static class Example10_DeadlockAndAvoidance {
        static void run() throws InterruptedException {
            // A) Demonstrate deadlock with intrinsic locks (threads are daemon so program doesn't hang)
            final Object A = new Object();
            final Object B = new Object();

            Thread d1 = new Thread(() -> {
                synchronized (A) {
                    log("d1 locked A");
                    sleep(100);
                    synchronized (B) {
                        log("d1 locked B");
                    }
                }
            }, "deadlock-1");
            d1.setDaemon(true);

            Thread d2 = new Thread(() -> {
                synchronized (B) {
                    log("d2 locked B");
                    sleep(100);
                    synchronized (A) {
                        log("d2 locked A");
                    }
                }
            }, "deadlock-2");
            d2.setDaemon(true);

            d1.start(); d2.start();
            sleep(300);
            log("Deadlock likely occurred (both waiting on each other)");

            // B) Avoid deadlock via consistent lock ordering
            ReentrantLock l1 = new ReentrantLock();
            ReentrantLock l2 = new ReentrantLock();

            Runnable orderedTask = () -> {
                // Order by identity hash to ensure both threads use the same order
                ReentrantLock first = System.identityHashCode(l1) < System.identityHashCode(l2) ? l1 : l2;
                ReentrantLock second = (first == l1) ? l2 : l1;

                first.lock();
                try {
                    log("acquired first");
                    sleep(50);
                    second.lock();
                    try {
                        log("acquired second");
                        sleep(50);
                    } finally {
                        second.unlock();
                        log("released second");
                    }
                } finally {
                    first.unlock();
                    log("released first");
                }
            };

            Thread a = new Thread(orderedTask, "ordered-1");
            Thread b = new Thread(orderedTask, "ordered-2");
            a.start(); b.start(); a.join(); b.join();

            // C) Avoid deadlock using tryLock with timeout (back off and retry)
            ReentrantLock x = new ReentrantLock();
            ReentrantLock y = new ReentrantLock();

            Runnable cautious = () -> {
                for (int attempt = 1; attempt <= 3; attempt++) {
                    boolean gotX = false, gotY = false;
                    try {
                        gotX = x.tryLock(80, TimeUnit.MILLISECONDS);
                        gotY = y.tryLock(80, TimeUnit.MILLISECONDS);
                        if (gotX && gotY) {
                            log("did work with both locks on attempt " + attempt);
                            return;
                        }
                    } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    finally {
                        if (!gotY && gotX) { x.unlock(); }
                        if (!gotX && gotY) { y.unlock(); }
                    }
                    sleep(30);
                }
                log("backed off after retries (avoided deadlock)");
            };
            Thread c1 = new Thread(cautious, "tryLock-1");
            Thread c2 = new Thread(cautious, "tryLock-2");
            c1.start(); c2.start(); c1.join(); c2.join();
        }
    }

    // --------------------------------------------------------------------------------------------

    static class Example11_FairVsUnfairLocks {
        static void run() throws InterruptedException {
            testLockOrder(new ReentrantLock(true), "FAIR");
            testLockOrder(new ReentrantLock(false), "UNFAIR");
        }

        static void testLockOrder(ReentrantLock lock, String label) throws InterruptedException {
            List<String> order = Collections.synchronizedList(new ArrayList<>());

            // Hold the lock so waiting threads queue up
            lock.lock();
            try {
                Thread t1 = waiter(lock, "w1", order);
                Thread t2 = waiter(lock, "w2", order);
                Thread t3 = waiter(lock, "w3", order);
                t1.start(); t2.start(); t3.start();

                // Late arrival that may "barge" on UNFAIR lock
                Thread late = waiter(lock, "late", order);
                sleep(20);
                late.start();

                sleep(50);
            } finally { lock.unlock(); }

            // Wait for all to complete
            sleep(200);

            log(label + " lock acquisition order: " + order);
        }

        static Thread waiter(ReentrantLock lock, String name, List<String> order) {
            return new Thread(() -> {
                lock.lock();
                try {
                    order.add(name);
                    sleep(30);
                } finally {
                    lock.unlock();
                }
            }, name);
        }
    }

    // --------------------------------------------------------------------------------------------

    static class Example12_SynchronizedCollections {
        static void run() throws InterruptedException {
            List<Integer> list = Collections.synchronizedList(new ArrayList<>());
            for (int i = 0; i < 5; i++) list.add(i);

            Thread modifier = new Thread(() -> {
                for (int i = 5; i < 10; i++) {
                    list.add(i);
                    sleep(20);
                }
            }, "modifier");
            modifier.start();

            // Safe iteration requires external synchronization on the list
            int sum = 0;
            synchronized (list) {
                for (int x : list) sum += x;
            }
            log("Iterated safely, partial sum=" + sum);

            modifier.join();
            synchronized (list) {
                log("Final list size=" + list.size());
            }
        }
    }

    // --------------------------------------------------------------------------------------------

    static class Example13_DoubleCheckedLocking {
        static class Singleton {
            private static volatile Singleton INSTANCE; // volatile is required
            private final int value = new Random().nextInt(1000);

            private Singleton() { /* simulate work */ sleep(10); }

            static Singleton getInstance() {
                Singleton local = INSTANCE;
                if (local == null) {                 // first check (no lock)
                    synchronized (Singleton.class) {
                        local = INSTANCE;
                        if (local == null) {         // second check (with lock)
                            INSTANCE = local = new Singleton();
                        }
                    }
                }
                return local;
            }

            int getValue() { return value; }
        }
        static void run() throws InterruptedException {
            Set<Integer> ids = ConcurrentHashMap.newKeySet();
            Runnable r = () -> ids.add(System.identityHashCode(Singleton.getInstance()));
            Thread[] ts = new Thread[8];
            for (int i = 0; i < ts.length; i++) ts[i] = new Thread(r, "dcl-" + i);
            for (Thread t : ts) t.start();
            for (Thread t : ts) t.join();
            log("Unique instance identities count (should be 1): " + ids.size());
        }
    }
}