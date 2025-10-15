package _08_02_synchronization_and_locks;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

/**
 * Interview Q&A (Basics → Intermediate → Advanced) on Synchronization & Locks.
 *
 * Read the Q&A comments and run selected demos from main if desired.
 */
public class _03_InterviewQA {

    public static void main(String[] args) throws Exception {
        System.out.println("Synchronization & Locks Interview Q&A Demos");
        System.out.println("Run targeted demos as needed; keeping runtime short.");

        // Basic: race condition and synchronization
        RaceConditionDemo.runShort();

        // ReentrantLock: tryLock/timeout/interruptibility
        ReentrantLockFeatures.demoTryLockTimeout();

        // Wait/notify bounded buffer (short run)
        WaitNotifyBoundedBuffer.demoShort();

        // Condition-based bounded buffer (short run)
        ConditionBoundedBuffer.demoShort();

        // ReadWriteLock cache
        ReadWriteLockCache.demoShort();

        // StampedLock optimistic read
        StampedLockExamples.demoShort();

        System.out.println("Done.");
    }

    // ============================================================
    // BASICS
    // ============================================================

    /*
     Q1: What is synchronization? Why is it needed?
     A: To coordinate thread access to shared state. It provides:
        - Atomicity: operations appear indivisible.
        - Visibility: changes by one thread become visible to others.
        - Ordering: establishes a happens-before relationship.

     Q2: What is a race condition?
     A: Program outcome depends on timing/interleavings. Example: i++ on shared int is not atomic.

     Q3: What does 'synchronized' do?
     A: Acquires the intrinsic lock (monitor) of an object/class.
        - synchronized(instanceMethod) locks 'this'.
        - synchronized static method locks the Class object.
        - synchronized (obj) locks obj's monitor.
        - It is reentrant: the same thread can reacquire the same monitor.

     Q4: What is happens-before in Java Memory Model?
     A: If A happens-before B, then effects of A are visible to B. Unlock happens-before subsequent lock on the same monitor. Volatile write happens-before subsequent volatile read of the same variable. Thread start/join, and completion of constructor for final fields also define relationships.

     Q5: volatile vs synchronized?
     A: volatile gives visibility + ordering for single-variable reads/writes but no atomicity for compound actions (like i++ or check-then-act). synchronized provides mutual exclusion + visibility for the guarded region.

     Q6: Are long/double reads/writes atomic?
     A: Not guaranteed to be atomic when non-volatile in the JLS; declare volatile or use synchronization/atomics. (Most modern JVMs make them atomic, but do not rely on that.)
     */

    static class RaceConditionDemo {
        private int counterUnsafe = 0;

        private int counterSync = 0;
        private final Object lock = new Object();

        void incrementUnsafe() {
            counterUnsafe++; // not atomic
        }

        synchronized void incrementSyncMethod() {
            counterSync++; // synchronized on 'this'
        }

        void incrementSyncBlock() {
            synchronized (lock) { // private lock object (best practice)
                counterSync++;
            }
        }

        static void runShort() throws InterruptedException {
            System.out.println("\n[RaceConditionDemo]");

            int threads = 4;
            int iters = 50_000;

            // Unsafe increments
            RaceConditionDemo d1 = new RaceConditionDemo();
            Thread[] t1 = start(threads, () -> {
                for (int i = 0; i < iters; i++) d1.incrementUnsafe();
            });
            join(t1);
            System.out.println("Unsafe expected=" + (threads * iters) + ", actual=" + d1.counterUnsafe);

            // Safe increments via synchronized
            RaceConditionDemo d2 = new RaceConditionDemo();
            Thread[] t2 = start(threads, () -> {
                for (int i = 0; i < iters; i++) d2.incrementSyncMethod();
            });
            join(t2);
            System.out.println("Synchronized actual=" + d2.counterSync);
        }
    }

    /*
     Q7: What is reentrancy?
     A: A thread can acquire the same lock it already holds. Prevents self-deadlock.
     */
    static class ReentrancyExample {
        private final Object lock = new Object();

        void a() {
            synchronized (lock) {
                b(); // re-enters same lock
            }
        }

        void b() {
            synchronized (lock) {
                // do work
            }
        }
    }

    /*
     Q8: Best practices for intrinsic locks?
     A:
       - Prefer private final lock objects instead of synchronizing on 'this'.
       - Always release locks in finally (for explicit Lock API).
       - Keep critical sections small.
       - Use higher-level concurrency utilities when possible.
     */

    // ============================================================
    // WAIT/NOTIFY (INTRINSIC CONDITION) AND GUARDED BLOCKS
    // ============================================================

    /*
     Q9: wait/notify/notifyAll rules?
     A:
       - Must hold the monitor of the object you call wait/notify on.
       - wait releases the monitor and suspends the thread; upon wake-up it reacquires the monitor.
       - Always use 'while' loops around wait to guard against spurious wakeups and missed signals.
       - Prefer notifyAll over notify when multiple condition predicates may apply.

     Q10: Spurious wakeups?
     A: A waiting thread can wake up without being notified; always check the condition in a loop.
     */

    static class WaitNotifyBoundedBuffer<T> {
        private final Object lock = new Object();
        private final Object[] items;
        private int head = 0, tail = 0, count = 0;

        WaitNotifyBoundedBuffer(int capacity) {
            this.items = new Object[capacity];
        }

        public void put(T x) throws InterruptedException {
            synchronized (lock) {
                while (count == items.length) lock.wait();
                items[tail] = x;
                tail = (tail + 1) % items.length;
                count++;
                lock.notifyAll();
            }
        }

        @SuppressWarnings("unchecked")
        public T take() throws InterruptedException {
            synchronized (lock) {
                while (count == 0) lock.wait();
                T x = (T) items[head];
                items[head] = null;
                head = (head + 1) % items.length;
                count--;
                lock.notifyAll();
                return x;
            }
        }

        static void demoShort() throws Exception {
            System.out.println("\n[WaitNotifyBoundedBuffer]");
            WaitNotifyBoundedBuffer<Integer> buf = new WaitNotifyBoundedBuffer<>(2);

            Thread producer = new Thread(() -> {
                try {
                    for (int i = 1; i <= 5; i++) {
                        buf.put(i);
                        System.out.print("P" + i + " ");
                    }
                } catch (InterruptedException ignored) {}
            });

            Thread consumer = new Thread(() -> {
                try {
                    for (int i = 1; i <= 5; i++) {
                        Integer v = buf.take();
                        System.out.print("C" + v + " ");
                    }
                } catch (InterruptedException ignored) {}
            });

            producer.start();
            consumer.start();
            producer.join();
            consumer.join();
            System.out.println();
        }
    }

    // ============================================================
    // LOCK API (java.util.concurrent.locks)
    // ============================================================

    /*
     Q11: Lock vs synchronized?
     A:
       - Lock offers tryLock (non-blocking), timed tryLock, lockInterruptibly, fairness option, multiple Condition instances.
       - Lock is not block-structured; must unlock in finally.
       - Generally similar or faster in contention scenarios; otherwise synchronized may be simpler.

     Q12: ReentrantLock features?
     A:
       - Reentrancy, fairness (optional, reduces throughput), interruptible lock acquisition, timed tryLock.
     */

    static class ReentrantLockFeatures {
        private final ReentrantLock lock = new ReentrantLock(); // non-fair by default
        private int value = 0;

        void increment() {
            lock.lock();
            try {
                value++;
            } finally {
                lock.unlock();
            }
        }

        boolean tryIncrementWithin(Duration timeout) throws InterruptedException {
            if (lock.tryLock(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                try {
                    value++;
                    return true;
                } finally {
                    lock.unlock();
                }
            }
            return false;
        }

        void interruptibleWork() throws InterruptedException {
            lock.lockInterruptibly();
            try {
                // do work
            } finally {
                lock.unlock();
            }
        }

        static void demoTryLockTimeout() throws InterruptedException {
            System.out.println("\n[ReentrantLockFeatures]");
            ReentrantLockFeatures ex = new ReentrantLockFeatures();

            // Hold lock in one thread
            Thread t = new Thread(() -> {
                ex.lock.lock();
                try {
                    sleepSilently(300);
                } finally {
                    ex.lock.unlock();
                }
            });
            t.start();
            Thread.sleep(50);

            boolean got = ex.tryIncrementWithin(Duration.ofMillis(100)); // likely false
            System.out.println("tryLock with timeout obtained? " + got);
            t.join();
        }
    }

    /*
     Q13: Condition vs wait/notify?
     A:
       - Conditions are associated with explicit Locks.
       - Multiple conditions per lock, with await/signal/signalAll methods.
       - Same rules: use while loops; signal appropriate condition.
     */

    static class ConditionBoundedBuffer<T> {
        private final ReentrantLock lock = new ReentrantLock();
        private final Condition notFull = lock.newCondition();
        private final Condition notEmpty = lock.newCondition();
        private final Object[] items;
        private int head = 0, tail = 0, count = 0;

        ConditionBoundedBuffer(int capacity) {
            this.items = new Object[capacity];
        }

        public void put(T x) throws InterruptedException {
            lock.lock();
            try {
                while (count == items.length) notFull.await();
                items[tail] = x;
                tail = (tail + 1) % items.length;
                count++;
                notEmpty.signal();
            } finally {
                lock.unlock();
            }
        }

        @SuppressWarnings("unchecked")
        public T take() throws InterruptedException {
            lock.lock();
            try {
                while (count == 0) notEmpty.await();
                T x = (T) items[head];
                items[head] = null;
                head = (head + 1) % items.length;
                count--;
                notFull.signal();
                return x;
            } finally {
                lock.unlock();
            }
        }

        static void demoShort() throws Exception {
            System.out.println("\n[ConditionBoundedBuffer]");
            ConditionBoundedBuffer<String> buf = new ConditionBoundedBuffer<>(2);

            Thread prod = new Thread(() -> {
                try {
                    for (int i = 0; i < 4; i++) {
                        buf.put("x" + i);
                        System.out.print("P" + i + " ");
                    }
                } catch (InterruptedException ignored) {}
            });

            Thread cons = new Thread(() -> {
                try {
                    for (int i = 0; i < 4; i++) {
                        String v = buf.take();
                        System.out.print("C" + v + " ");
                    }
                } catch (InterruptedException ignored) {}
            });

            prod.start();
            cons.start();
            prod.join();
            cons.join();
            System.out.println();
        }
    }

    // ============================================================
    // READ-WRITE LOCKS
    // ============================================================

    /*
     Q14: ReadWriteLock use case?
     A: Many reads, few writes. Allows multiple concurrent readers, single writer.
        - ReentrantReadWriteLock is reentrant.
        - Do not try to upgrade from read to write while holding read lock; deadlock risk.
        - You can downgrade (write->read) safely.

     Q15: Fairness in locks?
     A: Fair locks reduce starvation but can reduce throughput.
     */

    static class ReadWriteLockCache<K, V> {
        private final Map<K, V> map = new ConcurrentHashMap<>();
        private final ReentrantReadWriteLock rw = new ReentrantReadWriteLock();
        private final Lock r = rw.readLock();
        private final Lock w = rw.writeLock();

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

        public V getOrCompute(K key, Callable<V> computer) throws Exception {
            V v;
            r.lock();
            try {
                v = map.get(key);
                if (v != null) return v;
            } finally {
                r.unlock();
            }
            w.lock();
            try {
                v = map.get(key);
                if (v == null) {
                    v = computer.call();
                    map.put(key, v);
                }
                return v;
            } finally {
                w.unlock();
            }
        }

        static void demoShort() throws Exception {
            System.out.println("\n[ReadWriteLockCache]");
            ReadWriteLockCache<String, Integer> cache = new ReadWriteLockCache<>();
            cache.put("a", 1);
            System.out.println("get(a)=" + cache.get("a"));
            System.out.println("getOrCompute(b)=" + cache.getOrCompute("b", () -> 42));
        }
    }

    // ============================================================
    // STAMPEDLOCK
    // ============================================================

    /*
     Q16: StampedLock?
     A: Supports optimistic reads, read locks, and write locks via stamps.
        - Not reentrant, not associated with Condition.
        - Optimistic read is fast under low contention; validate before using data.
        - Beware with long critical sections or virtual threads; may spin more than block.
     */

    static class StampedLockExamples {
        static class Point {
            private double x, y;
            private final StampedLock sl = new StampedLock();

            void move(double dx, double dy) {
                long stamp = sl.writeLock();
                try {
                    x += dx; y += dy;
                } finally {
                    sl.unlockWrite(stamp);
                }
            }

            double distanceFromOrigin() {
                long stamp = sl.tryOptimisticRead();
                double cx = x, cy = y;
                if (!sl.validate(stamp)) {
                    stamp = sl.readLock();
                    try {
                        cx = x; cy = y;
                    } finally {
                        sl.unlockRead(stamp);
                    }
                }
                return Math.hypot(cx, cy);
            }
        }

        static void demoShort() {
            System.out.println("\n[StampedLockExamples]");
            Point p = new Point();
            p.move(3, 4);
            System.out.println("dist=" + p.distanceFromOrigin());
        }
    }

    // ============================================================
    // VOLATILE, ATOMICS, AND SAFE PUBLICATION
    // ============================================================

    /*
     Q17: When to use volatile?
     A: For simple flags or one-writer-many-readers scenarios with independent reads/writes.

     Q18: Why is volatile not enough for i++?
     A: i++ is read-modify-write; needs atomicity. Use synchronization or AtomicInteger.

     Q19: Safe publication?
     A: Publish objects via:
        - final fields fully initialized in constructor (and no 'this' escape during construction),
        - volatile reference,
        - holding a lock before/after publication,
        - concurrent collections.

     Q20: Atomic classes?
     A: AtomicInteger/Long/Reference, CAS operations (compareAndSet), and LongAdder for high-contention counters.
     */

    static class AtomicsExamples {
        static void demoAtomics() throws InterruptedException {
            AtomicInteger ai = new AtomicInteger();
            int newVal = ai.incrementAndGet(); // atomic ++
            boolean updated = ai.compareAndSet(newVal, 100);
            // LongAdder for contended counters (better under high contention)
            LongAdder adder = new LongAdder();
            adder.increment();
            long sum = adder.sum();
        }
    }

    // ============================================================
    // DEADLOCK, LIVELOCK, STARVATION
    // ============================================================

    /*
     Q21: What is deadlock?
     A: Threads wait forever for each other due to cyclic lock dependencies.

     Avoidance:
       - Fixed lock ordering.
       - TryLock with timeout and backoff.
       - Minimize lock scope.
       - Use higher-level constructs.

     Detection:
       - Thread dumps (jstack), monitoring tools.

     Q22: Livelock?
     A: Threads are active but make no progress (e.g., both backing off repeatedly). Add randomness or escalation.

     Q23: Starvation?
     A: A thread never gets CPU/lock. Use fair locks, avoid priority abuse, avoid holding locks too long.
     */

    static class DeadlockExamples {
        static final Object L1 = new Object();
        static final Object L2 = new Object();

        static void createDeadlockNotRun() {
            Thread t1 = new Thread(() -> {
                synchronized (L1) {
                    sleepSilently(50);
                    synchronized (L2) { /* never reached */ }
                }
            });
            Thread t2 = new Thread(() -> {
                synchronized (L2) {
                    sleepSilently(50);
                    synchronized (L1) { /* never reached */ }
                }
            });
            t1.start();
            t2.start();
        }

        static void lockInOrder(Object a, Object b) {
            Object first = a;
            Object second = b;
            int h1 = System.identityHashCode(a);
            int h2 = System.identityHashCode(b);
            if (h1 > h2) { first = b; second = a; }
            else if (h1 == h2 && a != b) {
                // rare tie-breaker lock
                Object tieLock = DeadlockExamples.class;
                synchronized (tieLock) {
                    synchronized (first) { synchronized (second) { /* work */ } }
                    return;
                }
            }
            synchronized (first) { synchronized (second) { /* work */ } }
        }

        static void safeTryLockBoth(ReentrantLock a, ReentrantLock b) throws InterruptedException {
            while (true) {
                boolean gotA = a.tryLock(50, TimeUnit.MILLISECONDS);
                boolean gotB = b.tryLock(50, TimeUnit.MILLISECONDS);
                if (gotA && gotB) {
                    try { /* work */ return; }
                    finally { b.unlock(); a.unlock(); }
                }
                if (gotA) a.unlock();
                if (gotB) b.unlock();
                Thread.sleep(10 + ThreadLocalRandom.current().nextInt(40));
            }
        }
    }

    // ============================================================
    // INTERRUPTS AND LOCKS
    // ============================================================

    /*
     Q24: Does synchronized respond to interrupt while waiting for a lock?
     A: No, lock acquisition is not interruptible. ReentrantLock.lockInterruptibly is.

     Q25: When does wait throw InterruptedException?
     A: If the thread is interrupted while waiting. Handle by propagating or restoring the interrupt.
     */

    static class InterruptHandling {
        static void properHandling() {
            try {
                // some interruptible operation
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // restore
                // or propagate
            }
        }
    }

    // ============================================================
    // DOUBLE-CHECKED LOCKING (DCL) SINGLETON
    // ============================================================

    /*
     Q26: Is double-checked locking safe?
     A: Yes with volatile instance reference.
     */

    static class DCLSingleton {
        private static volatile DCLSingleton INSTANCE;
        private final int payload;

        private DCLSingleton() { this.payload = 42; }

        public static DCLSingleton getInstance() {
            DCLSingleton local = INSTANCE;
            if (local == null) {
                synchronized (DCLSingleton.class) {
                    local = INSTANCE;
                    if (local == null) {
                        local = new DCLSingleton();
                        INSTANCE = local;
                    }
                }
            }
            return local;
        }
    }

    // ============================================================
    // SEMAPHORE, LATCHES, BARRIERS, PHASER
    // ============================================================

    /*
     Q27: Semaphore vs Lock?
     A: Semaphore limits permits/concurrency; a Lock provides mutual exclusion for a critical section.

     Q28: CountDownLatch vs CyclicBarrier vs Phaser?
     A:
       - Latch: one-shot gate (await until count reaches 0).
       - Barrier: all parties meet at barrier repeatedly.
       - Phaser: flexible barrier with dynamic parties.
     */

    static class SemaphoreExamples {
        static void limitConcurrency() throws InterruptedException {
            Semaphore sem = new Semaphore(3, true);
            ExecutorService pool = Executors.newFixedThreadPool(8);
            CountDownLatch done = new CountDownLatch(8);
            for (int i = 0; i < 8; i++) {
                pool.submit(() -> {
                    try {
                        sem.acquire();
                        try { sleepSilently(100); }
                        finally { sem.release(); }
                    } catch (InterruptedException ignored) {
                    } finally {
                        done.countDown();
                    }
                });
            }
            done.await();
            pool.shutdownNow();
        }
    }

    // ============================================================
    // LOCKSUPPORT
    // ============================================================

    /*
     Q29: LockSupport?
     A: Low-level park/unpark primitives used by higher-level constructs.
        - park returns on unpark/interrupt/spurious return; check state.
     */

    static class LockSupportExample {
        static void demo() throws InterruptedException {
            Thread t = new Thread(() -> {
                System.out.println("parking");
                LockSupport.park();
                System.out.println("unparked/interrupted");
            });
            t.start();
            Thread.sleep(100);
            LockSupport.unpark(t);
            t.join();
        }
    }

    // ============================================================
    // THREADLOCAL & THREAD CONFINEMENT
    // ============================================================

    /*
     Q30: Thread confinement?
     A: Keep data accessed by only one thread; no synchronization needed. ThreadLocal assists where each thread needs its own state.
     */

    static class ThreadLocalExample {
        static final ThreadLocal<StringBuilder> TL = ThreadLocal.withInitial(StringBuilder::new);

        static String format(int x) {
            StringBuilder sb = TL.get();
            sb.setLength(0);
            return sb.append("val=").append(x).toString();
        }
    }

    // ============================================================
    // PERFORMANCE, CONTENTION, STRIPING
    // ============================================================

    /*
     Q31: How to reduce contention?
     A:
       - Reduce critical-section size.
       - Use striped locks or LongAdder.
       - Prefer immutable/shared-nothing designs.
       - Avoid false sharing; pad counters if needed.

     Q32: Lock striping?
     A: Use an array of locks/counters to spread contention.
     */

    static class StripedCounter {
        private final LongAdder[] adders;

        StripedCounter(int stripes) {
            adders = new LongAdder[stripes];
            for (int i = 0; i < stripes; i++) adders[i] = new LongAdder();
        }

        void increment() {
            int idx = (int) (Thread.currentThread().getId() % adders.length);
            adders[idx].increment();
        }

        long sum() {
            return Arrays.stream(adders).mapToLong(LongAdder::sum).sum();
        }
    }

    // ============================================================
    // IMMUTABILITY
    // ============================================================

    /*
     Q33: Why immutable objects are thread-safe?
     A: No state changes after construction; publication of fully-constructed immutable objects is safe, and no locks are needed to read them.
     */

    static final class ImmutablePoint {
        final int x, y;
        ImmutablePoint(int x, int y) { this.x = x; this.y = y; }
    }

    // ============================================================
    // VIRTUAL THREADS (Project Loom) NOTE
    // ============================================================

    /*
     Q34: Any lock caveats with virtual threads?
     A:
       - synchronized and ReentrantLock are virtual-thread-friendly in modern JDKs (they park, not block OS threads).
       - Avoid long-held locks and busy-waiting; prefer structured concurrency and high-level primitives.
       - StampedLock may spin more; prefer other locks for long waits.
     */

    // ============================================================
    // COMMON PITFALLS & BEST PRACTICES
    // ============================================================

    /*
     Q35: Common pitfalls?
     A:
       - Using 'notify' instead of 'notifyAll' when multiple conditions exist.
       - Not using 'while' around wait.
       - Synchronizing on publicly accessible locks (risk of external interference).
       - Forgetting to unlock in finally with Lock API.
       - Publishing partially constructed objects.
       - Over-synchronizing (coarse locks cause contention).

     Q36: Monitorenter/monitorexit?
     A: JVM bytecodes underlying synchronized. Compiler inserts try/finally to ensure release.

     Q37: Concurrent collections vs synchronized wrappers?
     A:
       - Prefer concurrent collections (ConcurrentHashMap, CopyOnWriteArrayList, etc.).
       - Collections.synchronizedX wraps are coarse-grained and may still need external synchronization for compound actions.
     */

    // ============================================================
    // UTILS
    // ============================================================

    static Thread[] start(int n, Runnable r) {
        Thread[] arr = new Thread[n];
        for (int i = 0; i < n; i++) {
            arr[i] = new Thread(r);
            arr[i].start();
        }
        return arr;
    }

    static void join(Thread[] arr) throws InterruptedException {
        for (Thread t : arr) t.join();
    }

    static void sleepSilently(long millis) {
        try { Thread.sleep(millis); } catch (InterruptedException ignored) {}
    }
}