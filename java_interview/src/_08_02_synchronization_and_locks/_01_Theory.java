package _08_02_synchronization_and_locks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.StampedLock;

/**
 * Synchronization & Locks — Theory and Practical Patterns (Java)
 *
 * Key goals of synchronization:
 * - Atomicity: execute critical sections as indivisible units.
 * - Visibility: ensure writes by one thread become visible to others.
 * - Ordering: constrain reordering so programs behave predictably.
 *
 * Java Memory Model (JMM) essentials:
 * - Data race: two threads access same variable, at least one is a write, no happens-before ordering -> undefined results.
 * - Happens-before rules (selected):
 *   * An unlock (monitor exit) happens-before every subsequent lock (monitor enter) on the same monitor.
 *   * A write to a volatile happens-before every subsequent read of that same volatile.
 *   * Thread.start() happens-before the started thread’s first action.
 *   * A thread’s last action happens-before another thread’s successful Thread.join() return on it.
 *   * The write of a final field in a constructor happens-before the first read of that field, provided 'this' is not leaked.
 *
 * Intrinsic (monitor) locks:
 * - Every object has an intrinsic lock. 'synchronized' uses it.
 * - Reentrant: a thread can re-acquire the same lock it already holds.
 * - Options:
 *   * synchronized(instanceMethod) locks 'this'.
 *   * synchronized static method locks the Class object.
 *   * synchronized(block) locks the given monitor object.
 *
 * Explicit locks (java.util.concurrent.locks):
 * - ReentrantLock: features tryLock, lockInterruptibly, fairness, multiple Conditions.
 * - ReentrantReadWriteLock: read/write locks for higher concurrency on reads.
 * - StampedLock: optimistic reads; not reentrant; supports conversion/downgrade; no Condition.
 *
 * Waiting/notification:
 * - wait/notify/notifyAll used with intrinsic locks.
 * - Must call wait/notify while holding the monitor.
 * - Always use a loop with wait (spurious wakeups).
 *
 * Common pitfalls:
 * - Deadlock: circular wait on locks; fix via lock ordering and timeouts (tryLock).
 * - Livelock: threads keep responding to others and make no progress.
 * - Starvation: a thread never makes progress due to unfair scheduling.
 * - Synchronizing on publicly reachable or mutable lock objects (e.g., a String literal, autoboxed Integer) can cause unintended lock sharing.
 * - Publishing 'this' from a constructor breaks safe publication.
 *
 * When to use what:
 * - synchronized for simple mutual exclusion + wait/notify.
 * - ReentrantLock when you need tryLock, timed or interruptible locking, or multiple Conditions.
 * - ReadWriteLock for many readers, fewer writers (no upgrade).
 * - StampedLock for high read concurrency with optimistic reads.
 * - Volatile for visibility of single variable independent writes (no compound atomicity).
 * - Atomics (e.g., AtomicInteger) for lock-free single-variable updates with CAS.
 *
 * Always:
 * - Keep critical sections small.
 * - Prefer private final lock objects to 'this' if the object escapes.
 * - Release locks in finally.
 * - Use notifyAll unless you are certain notify is correct.
 * - Document thread-safety guarantees and lock ordering rules.
 */
public class _01_Theory {

    public static void main(String[] args) throws Exception {
        // Minimal, fast demonstration. See comments and classes below for deeper theory.
        System.out.println("Synchronization & Locks — quick demo start");

        RaceVsSync.quickDemo();

        BoundedBufferIntrinsic<String> q1 = new BoundedBufferIntrinsic<>(2);
        q1.put("A");
        q1.put("B");
        System.out.println("Intrinsic buffer took: " + q1.take() + ", " + q1.take());

        BoundedBufferLock<String> q2 = new BoundedBufferLock<>(2);
        q2.put("X");
        q2.put("Y");
        System.out.println("ReentrantLock buffer took: " + q2.take() + ", " + q2.take());

        ReadMostlyCache<String, Integer> cache = new ReadMostlyCache<>();
        cache.put("k", 42);
        System.out.println("ReadWriteLock cache get: " + cache.get("k"));

        StampedPoint p = new StampedPoint(1, 2);
        System.out.println("StampedLock distance: " + p.distanceFromOriginOptimistic());

        Singleton s = Singleton.instance();
        System.out.println("Singleton value: " + s.value());

        System.out.println("Synchronization & Locks — quick demo done");
    }

    // -------------------------------------------
    // 1) Race conditions vs synchronization
    // -------------------------------------------
    static class RaceVsSync {
        static class UnsafeCounter {
            // Not volatile/synchronized: both atomicity and visibility are broken under races.
            private int value;
            void increment() {
                // read, add, write (not atomic).
                value++;
            }
            int get() { return value; }
        }

        static class SafeCounter {
            private int value;

            // Mutex on 'this' provides mutual exclusion + visibility/order guarantees.
            synchronized void increment() {
                value++;
            }
            synchronized int get() { return value; }
        }

        static void quickDemo() throws InterruptedException {
            UnsafeCounter u = new UnsafeCounter();
            SafeCounter s = new SafeCounter();
            int threads = 4;
            int iters = 100_000;

            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(threads);
            for (int t = 0; t < threads; t++) {
                new Thread(() -> {
                    try {
                        start.await();
                        for (int i = 0; i < iters; i++) {
                            u.increment();
                            s.increment();
                        }
                    } catch (InterruptedException ignored) {
                    } finally {
                        done.countDown();
                    }
                }).start();
            }
            start.countDown();
            done.await();

            System.out.println("UnsafeCounter result (expected " + (threads * iters) + "): " + u.get());
            System.out.println("SafeCounter result: " + s.get());
        }
    }

    // ---------------------------------------------------
    // 2) Intrinsic locks: synchronized methods and blocks
    // ---------------------------------------------------
    static class IntrinsicExamples {
        private int state = 0;

        // Locks on 'this'
        public synchronized void inc() {
            state++;
        }

        // Locks on the Class object (per-class lock)
        public static synchronized void classLevelOperation() {
            // ...
        }

        // Block-level lock on a private lock object (preferable to 'this' if object escapes)
        private final Object lock = new Object();
        public void safeInc() {
            synchronized (lock) {
                state++;
            }
        }

        // Reentrant property example: a synchronized method calling another synchronized method on the same object.
        public synchronized void outer() { inner(); }
        private synchronized void inner() { /* reentrant: same thread holds lock twice */ }

        // Avoid synchronizing on:
        // - public objects (can cause external interference)
        // - Strings literals or autoboxed values (may be shared across code you don't control)
        private final String BAD_LOCK = "DO_NOT_LOCK_ON_ME"; // DON'T DO THIS
        public void bad() {
            synchronized (BAD_LOCK) {
                // Might lock with other code using same literal string.
            }
        }
    }

    // ---------------------------------------------------
    // 3) wait/notify/notifyAll with intrinsic locks
    // ---------------------------------------------------
    static class BoundedBufferIntrinsic<E> {
        private final Object lock = new Object();
        private final LinkedList<E> queue = new LinkedList<>();
        private final int capacity;

        public BoundedBufferIntrinsic(int capacity) {
            if (capacity <= 0) throw new IllegalArgumentException("capacity>0 required");
            this.capacity = capacity;
        }

        public void put(E e) throws InterruptedException {
            Objects.requireNonNull(e);
            synchronized (lock) {
                // Always use while to protect against spurious wakeups and re-check invariant
                while (queue.size() == capacity) {
                    lock.wait();
                }
                queue.addLast(e);
                // notifyAll to wake both potential putters and takers fairly
                lock.notifyAll();
            }
        }

        public E take() throws InterruptedException {
            synchronized (lock) {
                while (queue.isEmpty()) {
                    lock.wait();
                }
                E e = queue.removeFirst();
                lock.notifyAll();
                return e;
            }
        }
    }

    // ---------------------------------------------------
    // 4) Explicit locks: ReentrantLock and Conditions
    // ---------------------------------------------------
    static class BoundedBufferLock<E> {
        private final ReentrantLock lock;
        private final Condition notEmpty;
        private final Condition notFull;
        private final LinkedList<E> q = new LinkedList<>();
        private final int capacity;

        public BoundedBufferLock(int capacity) {
            this(capacity, false);
        }

        public BoundedBufferLock(int capacity, boolean fair) {
            if (capacity <= 0) throw new IllegalArgumentException("capacity>0 required");
            this.capacity = capacity;
            this.lock = new ReentrantLock(fair);
            this.notEmpty = lock.newCondition();
            this.notFull = lock.newCondition();
        }

        public void put(E e) throws InterruptedException {
            Objects.requireNonNull(e);
            lock.lockInterruptibly();
            try {
                while (q.size() == capacity) {
                    notFull.await(); // interruptible
                }
                q.addLast(e);
                notEmpty.signal(); // at least one waiter can proceed
            } finally {
                lock.unlock();
            }
        }

        public E take() throws InterruptedException {
            lock.lockInterruptibly();
            try {
                while (q.isEmpty()) {
                    notEmpty.await();
                }
                E e = q.removeFirst();
                notFull.signal();
                return e;
            } finally {
                lock.unlock();
            }
        }

        public boolean tryPut(E e, long timeout, TimeUnit unit) throws InterruptedException {
            long nanos = unit.toNanos(timeout);
            if (!lock.tryLock(nanos, TimeUnit.NANOSECONDS)) return false;
            try {
                while (q.size() == capacity) {
                    if ((nanos = notFull.awaitNanos(nanos)) <= 0L) return false;
                }
                q.addLast(e);
                notEmpty.signal();
                return true;
            } finally {
                lock.unlock();
            }
        }
    }

    // ---------------------------------------------------
    // 5) Read-write locks for read-mostly workloads
    // ---------------------------------------------------
    static class ReadMostlyCache<K, V> {
        private final ReadWriteLock rw = new ReentrantReadWriteLock(); // can be fair
        private final Lock r = rw.readLock();
        private final Lock w = rw.writeLock();
        private final Map<K, V> map = new HashMap<>();

        public V get(K key) {
            r.lock();
            try {
                return map.get(key);
            } finally {
                r.unlock();
            }
        }

        public void put(K key, V value) {
            w.lock();
            try {
                map.put(key, value);
            } finally {
                w.unlock();
            }
        }

        // Downgrade: acquire read before releasing write to ensure visibility to this thread
        public V putIfAbsentAndRead(K key, V value) {
            w.lock();
            try {
                V cur = map.get(key);
                if (cur == null) map.put(key, value);
                r.lock(); // downgrade
            } finally {
                w.unlock();
            }
            try {
                return map.get(key);
            } finally {
                r.unlock();
            }
        }

        // DO NOT try to upgrade read->write by locking write while holding read: deadlock risk.
    }

    // ---------------------------------------------------
    // 6) StampedLock: optimistic reads; not reentrant; no Conditions
    // ---------------------------------------------------
    static class StampedPoint {
        private final StampedLock sl = new StampedLock();
        private double x, y;

        StampedPoint(double x, double y) {
            long stamp = sl.writeLock();
            try {
                this.x = x; this.y = y;
            } finally {
                sl.unlockWrite(stamp);
            }
        }

        public void move(double dx, double dy) {
            long stamp = sl.writeLock();
            try {
                x += dx; y += dy;
            } finally {
                sl.unlockWrite(stamp);
            }
        }

        public double distanceFromOriginOptimistic() {
            long stamp = sl.tryOptimisticRead();
            double cx = x, cy = y; // read without locking
            if (!sl.validate(stamp)) { // if write intervened, fall back
                stamp = sl.readLock();
                try {
                    cx = x; cy = y;
                } finally {
                    sl.unlockRead(stamp);
                }
            }
            return Math.hypot(cx, cy);
        }

        // Convert read -> write via tryConvertToWriteLock
        public void moveIfAt(double oldX, double oldY, double newX, double newY) {
            long stamp = sl.readLock();
            try {
                while (x == oldX && y == oldY) {
                    long ws = sl.tryConvertToWriteLock(stamp);
                    if (ws != 0L) { // success
                        stamp = ws;
                        x = newX; y = newY;
                        return;
                    } else {
                        sl.unlockRead(stamp);
                        stamp = sl.writeLock();
                    }
                }
            } finally {
                sl.unlock(stamp);
            }
        }
    }

    // ---------------------------------------------------
    // 7) Volatile vs synchronized vs atomics
    // ---------------------------------------------------
    static class VolatileFlag {
        // Volatile ensures visibility and ordering wrt this variable.
        // Does NOT make compound actions atomic.
        private volatile boolean running = true;

        public void stop() { running = false; }

        public void workLoop() {
            while (running) {
                // do work
            }
        }
    }

    static class AtomicCounter {
        private final AtomicInteger ai = new AtomicInteger();
        public int incAndGet() { return ai.incrementAndGet(); } // lock-free CAS
    }

    // ---------------------------------------------------
    // 8) Safe publication, immutability, and final fields
    // ---------------------------------------------------
    static final class PointImmutable {
        final int x, y;
        PointImmutable(int x, int y) { this.x = x; this.y = y; }
        // Immutable objects are thread-safe by construction.
    }

    static class Holder {
        private volatile PointImmutable point; // volatile ensures safe publication
        public void publish(int x, int y) { point = new PointImmutable(x, y); }
        public PointImmutable read() { return point; }
    }

    // ---------------------------------------------------
    // 9) Double-checked locking (correct with volatile)
    // ---------------------------------------------------
    static class Singleton {
        private static volatile Singleton INSTANCE; // volatile is crucial
        private final int value;

        private Singleton() {
            // heavy init
            value = 123;
        }

        public static Singleton instance() {
            Singleton local = INSTANCE;
            if (local == null) {
                synchronized (Singleton.class) {
                    local = INSTANCE;
                    if (local == null) {
                        local = new Singleton();
                        INSTANCE = local; // safe publication
                    }
                }
            }
            return local;
        }

        public int value() { return value; }
    }

    // ---------------------------------------------------
    // 10) Deadlock example and fixes
    // ---------------------------------------------------
    static class Account {
        private final int id;
        private int balance;
        private final Object lock = new Object();
        Account(int id, int balance) { this.id = id; this.balance = balance; }

        void deposit(int amt) { synchronized (lock) { balance += amt; } }
        boolean withdraw(int amt) {
            synchronized (lock) {
                if (balance >= amt) { balance -= amt; return true; }
                return false;
            }
        }

        static boolean transferBad(Account a, Account b, int amt) {
            // POTENTIAL DEADLOCK if two concurrent transfers invert a/b order
            synchronized (a.lock) {
                synchronized (b.lock) {
                    if (a.balance >= amt) { a.balance -= amt; b.balance += amt; return true; }
                    return false;
                }
            }
        }

        static boolean transferWithOrdering(Account a, Account b, int amt) {
            Account first = a.id < b.id ? a : b;
            Account second = a.id < b.id ? b : a;
            synchronized (first.lock) {
                synchronized (second.lock) {
                    if (a.balance >= amt) { a.balance -= amt; b.balance += amt; return true; }
                    return false;
                }
            }
        }

        static boolean transferWithTryLock(Account a, Account b, int amt, long timeout, TimeUnit unit) throws InterruptedException {
            ReentrantLock la = new ReentrantLock();
            ReentrantLock lb = new ReentrantLock();
            long deadline = System.nanoTime() + unit.toNanos(timeout);

            while (true) {
                if (la.tryLock()) {
                    try {
                        if (lb.tryLock()) {
                            try {
                                if (a.balance >= amt) { a.balance -= amt; b.balance += amt; return true; }
                                return false;
                            } finally {
                                lb.unlock();
                            }
                        }
                    } finally {
                        la.unlock();
                    }
                }
                if (System.nanoTime() > deadline) return false;
                // backoff
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(2));
            }
        }
    }

    // ---------------------------------------------------
    // 11) Starvation and fairness
    // ---------------------------------------------------
    static class FairnessNote {
        // ReentrantLock(true) provides a fair queue, but reduces throughput.
        final ReentrantLock fairLock = new ReentrantLock(true);
        final ReentrantLock nonFairLock = new ReentrantLock(false);
    }

    // ---------------------------------------------------
    // 12) Conditions vs wait/notify choice
    // ---------------------------------------------------
    static class ConditionVsWait {
        // Choose Conditions when you need:
        // - Multiple wait sets (e.g., notEmpty and notFull).
        // - Timed and interruptible waits more conveniently.
        // Use intrinsic wait/notify for simpler one-condition cases.
    }

    // ---------------------------------------------------
    // 13) Synchronized collections vs concurrent collections
    // ---------------------------------------------------
    static class CollectionsNote {
        final List<Integer> syncList = Collections.synchronizedList(new ArrayList<>());
        final Map<String, Integer> syncMap = Collections.synchronizedMap(new HashMap<>());
        final Map<String, Integer> concurrent = new ConcurrentHashMap<>();

        void iterateSafely() {
            // For synchronizedX wrappers, manual synchronization during iteration is required.
            synchronized (syncList) {
                for (int v : syncList) {
                    // ...
                }
            }
            // For ConcurrentHashMap, iterators are weakly consistent (no ConcurrentModificationException)
            for (Map.Entry<String, Integer> e : concurrent.entrySet()) {
                // ...
            }
        }
    }

    // ---------------------------------------------------
    // 14) Lock splitting/striping to reduce contention
    // ---------------------------------------------------
    static class StripedCounter {
        private static final int STRIPES = 16; // power of two
        private final AtomicInteger[] cells = new AtomicInteger[STRIPES];

        StripedCounter() {
            for (int i = 0; i < STRIPES; i++) cells[i] = new AtomicInteger();
        }

        void add(int x) {
            int idx = ThreadLocalRandom.current().nextInt(STRIPES);
            cells[idx].addAndGet(x);
        }

        int sum() {
            int s = 0;
            for (AtomicInteger ai : cells) s += ai.get();
            return s;
        }
    }

    // ---------------------------------------------------
    // 15) Thread lifecycle happens-before: start/join
    // ---------------------------------------------------
    static class StartJoinHB {
        int data = 0;

        int compute() throws InterruptedException {
            Thread t = new Thread(() -> data = 42);
            t.start();                 // start happens-before t's actions
            t.join();                  // t's completion happens-before join returns
            return data;               // guaranteed to see 42
        }
    }

    // ---------------------------------------------------
    // 16) Interrupts with locks and waits
    // ---------------------------------------------------
    static class InterruptibleExample {
        private final ReentrantLock lock = new ReentrantLock();
        private final Condition condition = lock.newCondition();

        void awaitSomething() throws InterruptedException {
            lock.lockInterruptibly();
            try {
                while (!predicate()) {
                    condition.await(); // responds to interrupts
                }
            } finally {
                lock.unlock();
            }
        }

        private boolean predicate() { return false; }
    }

    // ---------------------------------------------------
    // 17) Semaphore as a simple lock/permit control
    // ---------------------------------------------------
    static class SemaphoreGuard {
        private final Semaphore sem = new Semaphore(3); // allow up to 3 concurrent entries

        void work() throws InterruptedException {
            sem.acquire();
            try {
                // critical work
            } finally {
                sem.release();
            }
        }
    }

    // ---------------------------------------------------
    // 18) Spurious wakeups: always loop around waits
    // ---------------------------------------------------
    static class SpuriousWakeups {
        private final Object lock = new Object();
        private boolean condition;

        void awaitCondition() throws InterruptedException {
            synchronized (lock) {
                while (!condition) {
                    lock.wait(); // could wake spuriously, or due to notify intended for others
                }
            }
        }
    }

    // ---------------------------------------------------
    // 19) Lock scope and granularity — keep critical sections small
    // ---------------------------------------------------
    static class Granularity {
        final Object lock = new Object();
        void slow() {
            synchronized (lock) {
                // Move non-shared computations outside of synchronized blocks.
                // Keep only shared-state interactions inside.
            }
        }
    }

    // ---------------------------------------------------
    // 20) Document lock ownership and invariants
    // ---------------------------------------------------
    static class Ownership {
        // Clearly indicate which lock protects which data
        private final Object sizeLock = new Object();
        private int size; // guarded by sizeLock

        public void inc() { synchronized (sizeLock) { size++; } }
        public int get() { synchronized (sizeLock) { return size; } }
    }

    // ---------------------------------------------------
    // 21) Using private lock objects vs 'this'
    // ---------------------------------------------------
    static class PrivateLock {
        private final Object lock = new Object();
        private int state;
        public void op() {
            synchronized (lock) {
                state++;
            }
        }
        // Using 'lock' prevents external code from accidentally synchronizing on us.
    }

    // ---------------------------------------------------
    // 22) LockSupport and parking (lower-level primitive)
    // ---------------------------------------------------
    static class Parking {
        void demo() {
            Thread t = new Thread(() -> {
                // park current thread
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(10));
            });
            t.start();
        }
    }

    // ---------------------------------------------------
    // 23) Livelock example (conceptual)
    // ---------------------------------------------------
    static class LivelockExample {
        final ReentrantLock left = new ReentrantLock();
        final ReentrantLock right = new ReentrantLock();

        void dancerA() {
            while (true) {
                if (left.tryLock()) {
                    try {
                        if (right.tryLock()) {
                            try { break; } finally { right.unlock(); }
                        }
                    } finally { left.unlock(); }
                }
                Thread.yield(); // both keep yielding and trying -> possible livelock
            }
        }
    }

    // ---------------------------------------------------
    // 24) Identity-based lock ordering to avoid deadlock
    // ---------------------------------------------------
    static class OrderedLocker {
        static void lockBoth(Object a, Object b) {
            int ha = System.identityHashCode(a);
            int hb = System.identityHashCode(b);
            Object first = ha < hb ? a : b;
            Object second = ha < hb ? b : a;
            synchronized (first) {
                synchronized (second) {
                    // safe from cycles (ties need special handling)
                }
            }
        }
    }

    // ---------------------------------------------------
    // 25) Avoiding locking on mutable keys (Map key locks)
    // ---------------------------------------------------
    static class KeyLocking {
        private final Map<Object, Object> locks = new IdentityHashMap<>();
        private final Object global = new Object();

        private Object lockFor(Object key) {
            synchronized (global) {
                return locks.computeIfAbsent(key, k -> new Object());
            }
        }

        public void withKeyLock(Object key, Runnable task) {
            Object l = lockFor(key);
            synchronized (l) {
                task.run();
            }
        }
    }

    // ---------------------------------------------------
    // 26) Producer-Consumer with notifyAll vs notify
    // ---------------------------------------------------
    static class ProducerConsumer {
        private final Object lock = new Object();
        private final LinkedList<Integer> q = new LinkedList<>();
        private final int capacity = 10;

        public void producer() throws InterruptedException {
            synchronized (lock) {
                while (q.size() == capacity) lock.wait();
                q.add(1);
                // notifyAll avoids missed signals when multiple distinct conditions exist
                lock.notifyAll();
            }
        }

        public void consumer() throws InterruptedException {
            synchronized (lock) {
                while (q.isEmpty()) lock.wait();
                q.removeFirst();
                lock.notifyAll();
            }
        }
    }

    // ---------------------------------------------------
    // 27) Synchronized vs volatile: example of broken compound action with volatile
    // ---------------------------------------------------
    static class BrokenCounting {
        private volatile int count = 0; // volatile only ensures visibility, not atomicity
        public void inc() { count++; } // race-prone
        public int get() { return count; }
    }

    // ---------------------------------------------------
    // 28) Reentrancy across call stacks and inheritance
    // ---------------------------------------------------
    static class Parent {
        synchronized void a() { b(); }
        synchronized void b() { /* reentrant */ }
    }
    static class Child extends Parent {
        @Override synchronized void b() { /* still reentrant */ }
    }

    // ---------------------------------------------------
    // 29) Timed waits and time units with Conditions
    // ---------------------------------------------------
    static class TimedConditions {
        private final ReentrantLock lock = new ReentrantLock();
        private final Condition cond = lock.newCondition();

        public boolean awaitFor(long time, TimeUnit unit) throws InterruptedException {
            lock.lock();
            try {
                long nanos = unit.toNanos(time);
                while (!ready()) {
                    if ((nanos = cond.awaitNanos(nanos)) <= 0L) return false;
                }
                return true;
            } finally {
                lock.unlock();
            }
        }
        private boolean ready() { return false; }
    }

    // ---------------------------------------------------
    // 30) Summary guidelines (in-code checklist)
    // ---------------------------------------------------
    static class Guidelines {
        // - Prefer immutable data structures when possible.
        // - Use private final lock objects.
        // - Keep critical sections minimal; avoid blocking calls inside them.
        // - Prefer higher-level constructs (ConcurrentHashMap, queues) over manual locking when possible.
        // - Document which lock guards which field(s).
        // - Consider lock ordering to prevent deadlocks; use tryLock with timeouts for robustness.
        // - Always release locks in finally.
        // - Use while loops on waits (spurious wakeups).
        // - Beware of synchronization on shared constants or mutable keys.
        // - Benchmark: different locks have different performance characteristics under contention.
    }
}