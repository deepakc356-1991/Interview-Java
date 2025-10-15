package _08_05_concurrency_utilities_locks_queues;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.*;

/**
 Concurrency Utilities: Locks and Queues (theory + examples)

 Overview
 - Synchronized vs explicit locks:
   - synchronized is simple, reentrant, block-structured, and provides happens-before on exit/enter.
   - java.util.concurrent.locks.* adds features: tryLock (with/without timeout), interruptible lock acquisition,
     multiple condition variables, fairness policies, read-write and stamped locks.
   - Always release locks in finally to avoid leaks.

 Memory model (happens-before)
 - Lock unlock happens-before subsequent lock on same lock object.
 - BlockingQueue put/offer happens-before corresponding take/poll of that element.
 - Condition#await/signal obey lock rules; always hold the lock when calling await/signal.

 Failure modes and mitigations
 - Deadlock: cyclic lock ordering. Prevent by global lock order or tryLock with timeout and backoff.
 - Livelock: threads keep retrying but make no progress. Add jitter/backoff.
 - Starvation: fair locks/queues can help but at the cost of throughput.

 Locks

 1) ReentrantLock
 - Reentrant: a thread may acquire the same lock multiple times; maintain a hold count.
 - fair vs non-fair: fairness grants access in FIFO order but reduces throughput.
 - Key methods:
   - lock, unlock
   - tryLock(): non-blocking attempt
   - tryLock(long, TimeUnit): bounded blocking, useful to avoid deadlocks
   - lockInterruptibly(): acquire but respond to interrupts while waiting
   - newCondition(): multiple wait-sets (vs a single wait-set per monitor with wait/notify)
 - Conditions: always await in a while loop to handle spurious wakeups and recheck predicate.

 2) ReadWrite locks (ReentrantReadWriteLock)
 - Many concurrent readers, one writer.
 - Reentrant; supports lock downgrading (write -> read) but not upgrading safely (read -> write can deadlock).
 - Good for read-mostly workloads with short write holds.

 3) StampedLock
 - Not reentrant; uses stamps (long) for read/write/optimistic-read modes.
 - Optimistic read: check validity before using data; if invalid, fall back to read or write lock.
 - Faster than ReadWriteLock in highly contended, read-dominant scenarios, but more error-prone.
 - No Condition support; do not call blocking operations while holding a stamp.

 Queues

 Blocking vs non-blocking
 - BlockingQueue: put/take block until capacity/availability; offer/poll support timeouts.
 - ConcurrentLinkedQueue/Deque: lock-free, non-blocking; operations return immediately; not bounded.

 Backpressure
 - Use bounded BlockingQueue (e.g., ArrayBlockingQueue, LinkedBlockingQueue with capacity) to apply backpressure.
 - Unbounded queues can grow without limit and cause OOM under sustained production.

 BlockingQueue implementations
 - ArrayBlockingQueue: bounded, array-backed, single lock, optional fairness; predictable memory usage.
 - LinkedBlockingQueue: optionally bounded (default Integer.MAX_VALUE); two locks (put/take) for higher concurrency; more GC.
 - LinkedBlockingDeque: double-ended blocking queue.
 - PriorityBlockingQueue: unbounded priority queue; ordering by Comparator or natural order; put never blocks.
 - DelayQueue: unbounded; elements implement Delayed; head becomes available when delay expires.
 - SynchronousQueue: capacity 0; each put waits for a take (direct handoff). Used by cached thread pools.
 - TransferQueue (LinkedTransferQueue): can transfer elements directly to waiting consumers; has tryTransfer/transfer.

 Non-blocking queues
 - ConcurrentLinkedQueue/ConcurrentLinkedDeque: lock-free, linearizable for key ops; no capacity control.

 Patterns
 - Producer/consumer: BlockingQueue decouples rates.
 - Poison pill (sentinel) or interrupt-based shutdown for consumers.
 - drainTo for batch processing.
 - Time-bounded poll to remain responsive to shutdown.

 Pitfalls
 - Never signal conditions without holding the associated lock.
 - Always recheck predicates after await (spurious wakeups).
 - Avoid calling unknown/slow code while holding locks.
 - Be careful with default-boundedness: LinkedBlockingQueue default is effectively unbounded.

 Below are self-contained, commented examples.
 */
public class _01_Theory {

    public static void main(String[] args) throws Exception {
        // Minimal demonstration of a bounded producer/consumer with graceful shutdown.
        BlockingQueue<String> q = new ArrayBlockingQueue<>(4);
        ExecutorService es = Executors.newFixedThreadPool(2);

        Future<?> producer = es.submit(() -> {
            try {
                for (int i = 1; i <= 5; i++) {
                    q.put("item-" + i); // blocks if full (backpressure)
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            } finally {
                // Poison pill to signal end to a single consumer; use as many pills as consumers
                try {
                    q.put(BlockingQueueExamples.POISON_PILL);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        Future<List<String>> consumer = es.submit(() -> {
            List<String> consumed = new ArrayList<>();
            try {
                while (true) {
                    String s = q.take();
                    if (BlockingQueueExamples.POISON_PILL.equals(s)) break;
                    consumed.add(s);
                }
                return consumed;
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return consumed;
            }
        });

        producer.get();
        List<String> items = consumer.get();
        es.shutdownNow();
        // System.out.println(items);
    }

    // --------------- Locks: utilities and examples ---------------

    /**
     Small helper to use try-with-resources with Lock.
     Usage:
       try (LockGuard g = LockGuard.locking(lock)) {
           // critical section
       }
     */
    static final class LockGuard implements AutoCloseable {
        private final Lock lock;
        private LockGuard(Lock lock) { this.lock = lock; }
        public static LockGuard locking(Lock lock) {
            lock.lock();
            return new LockGuard(lock);
        }
        public static Optional<LockGuard> tryLocking(Lock lock, Duration timeout) throws InterruptedException {
            boolean ok = lock.tryLock(timeout.toNanos(), TimeUnit.NANOSECONDS);
            return ok ? Optional.of(new LockGuard(lock)) : Optional.empty();
        }
        @Override public void close() { lock.unlock(); }
    }

    /**
     ReentrantLock basics:
     - tryLock with timeout to avoid deadlock
     - interruptible acquisition
     - Conditions for fine-grained coordination
     */
    static final class ReentrantLockBasics {
        private final ReentrantLock lock = new ReentrantLock(/*fair*/ false);
        private final Condition ready = lock.newCondition();
        private boolean isReady;

        void setReady() {
            lock.lock();
            try {
                isReady = true;
                // Signal one waiter; use signalAll when multiple could be waiting on same predicate
                ready.signal();
            } finally {
                lock.unlock();
            }
        }

        void waitUntilReadyInterruptibly() throws InterruptedException {
            lock.lockInterruptibly();
            try {
                while (!isReady) { // always while, never if
                    ready.await(); // responds to interrupt
                }
            } finally {
                lock.unlock();
            }
        }

        boolean tryDoWorkWithin(Duration timeout) throws InterruptedException {
            // Use tryLock with timeout to avoid deadlocks or long waits
            if (!lock.tryLock(timeout.toNanos(), TimeUnit.NANOSECONDS)) return false;
            try {
                // do work
                return true;
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     Classic bounded buffer implemented with ReentrantLock and two Conditions.
     Shows correct signaling and predicate re-check.
     */
    static final class BoundedBufferWithCondition<E> {
        private final E[] items;
        private int head, tail, count;
        private final ReentrantLock lock;
        private final Condition notEmpty;
        private final Condition notFull;

        @SuppressWarnings("unchecked")
        BoundedBufferWithCondition(int capacity, boolean fair) {
            if (capacity <= 0) throw new IllegalArgumentException("capacity");
            this.items = (E[]) new Object[capacity];
            this.lock = new ReentrantLock(fair);
            this.notEmpty = lock.newCondition();
            this.notFull = lock.newCondition();
        }

        public void put(E e) throws InterruptedException {
            Objects.requireNonNull(e);
            lock.lockInterruptibly();
            try {
                while (count == items.length) notFull.await();
                items[tail] = e;
                tail = (tail + 1) % items.length;
                count++;
                notEmpty.signal(); // at least one element available
            } finally {
                lock.unlock();
            }
        }

        public E take() throws InterruptedException {
            lock.lockInterruptibly();
            try {
                while (count == 0) notEmpty.await();
                E e = items[head];
                items[head] = null;
                head = (head + 1) % items.length;
                count--;
                notFull.signal(); // at least one slot available
                return e;
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     ReadMostly cache using ReentrantReadWriteLock.
     - Many readers don't block each other.
     - Writers exclude both readers and other writers.
     - Demonstrates lock downgrading (write -> read) safely.
     */
    static final class ReadMostlyCache<K, V> {
        private final ReentrantReadWriteLock rw = new ReentrantReadWriteLock();
        private final Lock r = rw.readLock();
        private final Lock w = rw.writeLock();
        private final ConcurrentHashMap<K, V> map = new ConcurrentHashMap<>();

        public V getOrCompute(K key, Callable<V> compute) throws Exception {
            // Fast path: read
            r.lock();
            try {
                V v = map.get(key);
                if (v != null) return v;
            } finally {
                r.unlock();
            }

            // Upgrade by releasing read and acquiring write (not atomic; may compute twice unless guarded)
            w.lock();
            try {
                V existing = map.get(key);
                if (existing != null) return existing;
                V computed = compute.call();
                map.put(key, computed);

                // Downgrade: acquire read before releasing write to keep visibility and exclude other writers
                r.lock();
                try {
                    // Keep r; then release write
                } finally {
                    w.unlock();
                }
                // Do any post-write read under r if needed
                return computed;
            } finally {
                // Ensure the read lock acquired during downgrade is released
                ReentrantReadWriteLock rw = new ReentrantReadWriteLock() /* the same lock that produced r */;
                Lock r = rw.readLock();

                // If you only want to unlock when *this thread* holds the read lock:
                if (rw.getReadHoldCount() > 0) {
                    r.unlock();
                }

                // (If you meant “any readers at all exist”, use rw.getReadLockCount() > 0)

            }
        }
    }

    /**
     StampedLock example with optimistic read.
     - Check validity before trusting the read snapshot.
     - Fallback to read lock if invalid.
     */
    static final class StampedLockPoint {
        private double x, y;
        private final StampedLock sl = new StampedLock();

        public void move(double dx, double dy) {
            long stamp = sl.writeLock();
            try {
                x += dx; y += dy;
            } finally {
                sl.unlockWrite(stamp);
            }
        }

        public double distanceFromOrigin() {
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

        // Convert read lock to write lock (if possible) to update conditionally
        public void moveIfAt(double oldX, double oldY, double newX, double newY) {
            long stamp = sl.readLock();
            try {
                while (x == oldX && y == oldY) {
                    long ws = sl.tryConvertToWriteLock(stamp);
                    if (ws != 0L) {
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

    // --------------- BlockingQueue: patterns and examples ---------------

    static final class BlockingQueueExamples {
        static final String POISON_PILL = "__POISON__";

        // Producer-consumer with backpressure and graceful shutdown using poison pill
        static void basicProducerConsumer() throws InterruptedException {
            BlockingQueue<String> q = new ArrayBlockingQueue<>(100);

            Thread producer = new Thread(() -> {
                try {
                    for (int i = 0; i < 1000; i++) {
                        q.put("task-" + i); // blocks when full
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                } finally {
                    try { q.put(POISON_PILL); } catch (InterruptedException ignored) {}
                }
            });

            Thread consumer = new Thread(() -> {
                try {
                    while (true) {
                        String t = q.take();
                        if (POISON_PILL.equals(t)) break;
                        // process t
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            });

            producer.start();
            consumer.start();
            producer.join();
            consumer.join();
        }

        // Using time-bounded poll so that consumer can check shutdown flags periodically
        static void responsiveConsumer(BlockingQueue<Runnable> q, AtomicInteger stopFlag) {
            Thread consumer = new Thread(() -> {
                while (stopFlag.get() == 0) {
                    try {
                        Runnable r = q.poll(100, TimeUnit.MILLISECONDS);
                        if (r != null) r.run();
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
            consumer.start();
        }

        // Batch draining improves throughput and reduces contention
        static <E> void batchDrain(BlockingQueue<E> q, int maxBatch) {
            List<E> batch = new ArrayList<>(maxBatch);
            q.drainTo(batch, maxBatch);
            for (E e : batch) {
                // process e
            }
            batch.clear();
        }
    }

    /**
     Illustrates different queue types and when to use them.
     Instantiation plus notes in comments.
     */
    static final class QueueVariety {
        BlockingQueue<Integer> abq = new ArrayBlockingQueue<>(1024, /*fair*/ false);
        BlockingQueue<String> lbqBounded = new LinkedBlockingQueue<>(10_000); // bounded
        BlockingQueue<String> lbqUnbounded = new LinkedBlockingQueue<>(); // effectively unbounded (Integer.MAX_VALUE)
        BlockingDeque<String> lbdq = new LinkedBlockingDeque<>(1024);
        PriorityBlockingQueue<Job> pbq = new PriorityBlockingQueue<>(11, Comparator.comparingInt(j -> j.priority));
        DelayQueue<DelayedTask> delayQueue = new DelayQueue<>();
        SynchronousQueue<String> syncQUnfair = new SynchronousQueue<>(false);
        SynchronousQueue<String> syncQFair = new SynchronousQueue<>(true);
        TransferQueue<String> transferQ = new LinkedTransferQueue<>();
        Queue<String> lockFreeQ = new ConcurrentLinkedQueue<>();

        static final class Job { final int priority; final String name; Job(int p, String n){priority=p; name=n;} }

        static final class DelayedTask implements Delayed {
            final long deadlineNanos;
            final String payload;
            DelayedTask(Duration delay, String payload) {
                this.deadlineNanos = System.nanoTime() + delay.toNanos();
                this.payload = payload;
            }
            @Override public long getDelay(TimeUnit unit) {
                long remain = deadlineNanos - System.nanoTime();
                return unit.convert(remain, TimeUnit.NANOSECONDS);
            }
            @Override public int compareTo(Delayed o) {
                return Long.compare(this.getDelay(TimeUnit.NANOSECONDS), o.getDelay(TimeUnit.NANOSECONDS));
            }
        }

        // Examples of operations highlighting semantics
        void examples() throws InterruptedException {
            // ArrayBlockingQueue backpressure example
            abq.put(42); // blocks if full
            Integer x = abq.take(); // blocks if empty

            // PriorityBlockingQueue: unbounded; put never blocks; ordering by comparator
            pbq.put(new Job(5, "medium"));
            pbq.put(new Job(1, "high"));
            Job next = pbq.take(); // returns priority=1 first

            // DelayQueue: take blocks until head delay expired
            delayQueue.put(new DelayedTask(Duration.ofMillis(200), "after-200ms"));
            DelayedTask dt = delayQueue.take(); // waits ~200ms

            // SynchronousQueue: direct handoff
            new Thread(() -> {
                try { syncQFair.put("handoff"); } catch (InterruptedException ignored) {}
            }).start();
            String got = syncQFair.take(); // pairs with put

            // TransferQueue: prefer direct handoff if consumer waits
            transferQ.tryTransfer("maybe"); // returns immediately; true only if consumer waiting
            new Thread(() -> {
                try { String s = transferQ.take(); } catch (InterruptedException ignored) {}
            }).start();
            transferQ.transfer("must-deliver"); // blocks until consumed
        }
    }

    // --------------- Non-blocking queues ---------------

    static final class NonBlockingQueueExample {
        final Queue<String> q = new ConcurrentLinkedQueue<>();

        void use() {
            q.offer("a");
            q.offer("b");
            String h = q.poll(); // null if empty
            boolean removed = q.remove("b"); // best-effort removal
            // Note: no capacity control; add backpressure elsewhere if needed.
        }
    }

    // --------------- Advanced notes ---------------

    /**
     - Fair vs non-fair:
       - Fair locks/queues reduce starvation but increase context switching and reduce throughput.
       - Prefer non-fair unless you have strict fairness requirements.

     - CAS and AQS:
       - Many locks/queues use AQS (AbstractQueuedSynchronizer) with CAS under the hood.
       - CAS enables lock-free or low-lock algorithms (e.g., ConcurrentLinkedQueue).

     - Interruption:
       - Prefer interrupt-friendly methods (lockInterruptibly, await, put/take with timeout) to keep threads responsive.
       - On InterruptedException, restore interrupt status or propagate.

     - Shutdown strategies for consumers:
       - Poison pill: send one per consumer, ensures ordered shutdown via queue.
       - Interrupt: e.g., executor.shutdownNow(); consumer must handle InterruptedException properly.

     - Avoid upgrades:
       - With ReentrantReadWriteLock, avoid trying to acquire write while holding read; release read first.

     - Conditions:
       - Separate wait-sets model different predicates (e.g., notFull vs notEmpty).
       - Never call await/signal without holding the associated lock.
       - Use signalAll when multiple waiters might be eligible or to avoid missed signals in complex predicates.

     - drainTo caveats:
       - With unbounded queues, large drainTo can create large batches; use a maxElements parameter.
    */

    // --------------- Small end-to-end example: bounded work queue with graceful shutdown ---------------

    static final class BoundedExecutor implements AutoCloseable {
        private final BlockingQueue<Runnable> queue;
        private final List<Thread> workers = new ArrayList<>();
        private volatile boolean shuttingDown;

        BoundedExecutor(int capacity, int workers) {
            this.queue = new ArrayBlockingQueue<>(capacity);
            for (int i = 0; i < workers; i++) {
                Thread t = new Thread(this::runWorker, "worker-" + i);
                t.start();
                this.workers.add(t);
            }
        }

        public boolean submit(Runnable r, Duration timeout) throws InterruptedException {
            if (shuttingDown) return false;
            return queue.offer(r, timeout.toNanos(), TimeUnit.NANOSECONDS); // backpressure
        }

        private void runWorker() {
            try {
                while (true) {
                    Runnable r = queue.take(); // interruptible
                    r.run();
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }

        @Override public void close() {
            shuttingDown = true;
            for (Thread t : workers) t.interrupt();
        }
    }
}