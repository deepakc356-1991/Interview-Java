package _10_03_garbage_collection;

import java.lang.ref.Cleaner;
import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Garbage Collection examples: reachability, references, Cleaner, WeakHashMap,
 * soft/weak/phantom references, ReferenceQueue, memory leaks, and finalization.
 *
 * Run with:
 * -Xmx256m -Xms256m -Xlog:gc* (or -verbose:gc on older JDKs) to observe GC logs
 * Optional: -Dgc.heavy=true to enable memory pressure parts
 */
public class _02_Examples {

    private static final long MB = 1024L * 1024L;
    private static final boolean HEAVY = Boolean.getBoolean("gc.heavy");

    public static void main(String[] args) throws Exception {
        banner("Java " + System.getProperty("java.version"));
        printMemory("Startup memory");

        example1_BasicReachabilityWithWeakReference();
        example2_ScopesAndNullingReferences();
        example3_SystemGcIsOnlyAHint();
        example4_WeakHashMap_KeysCollected();
        example5_SoftReference_AsCacheUnderMemoryPressure();
        example6_PhantomReference_WithReferenceQueue();
        example7_Cleaner_ForNonMemoryResourceCleanup();
        example8_MemoryLeakPatterns_AndFixes();
        example9_Finalization_Deprecated_Demo();
        example10_ReferenceQueue_WeakValueCache();
    }

    // Example 1: Basic reachability using WeakReference
    private static void example1_BasicReachabilityWithWeakReference() throws InterruptedException {
        banner("1) Basic reachability with WeakReference");

        Object strong = new Object();
        WeakReference<Object> weak = new WeakReference<>(strong);

        System.out.println("While strong ref exists, weak.get() != null: " + (weak.get() != null));
        strong = null; // drop last strong reference

        waitForGc(weak, "Weak referent cleared");
    }

    // Example 2: Release memory earlier by limiting scope or nulling references
    private static void example2_ScopesAndNullingReferences() throws InterruptedException {
        banner("2) Scopes and nulling references to help earlier GC");

        printMemory("Before allocation");
        byte[] big = new byte[(int) ((HEAVY ? 64 : 8) * MB)]; // large object
        printMemory("After allocating big array");

        // Do some work...
        busy(50);

        // Null the reference so the array can become eligible for GC before method ends
        big = null;
        suggestGc("After nulling large array");
    }

    // Example 3: System.gc() is only a hint
    private static void example3_SystemGcIsOnlyAHint() {
        banner("3) System.gc() is a hint, not a guarantee");

        printMemory("Before System.gc()");
        System.gc(); // Hint only
        System.runFinalization(); // Another hint for finalization
        printMemory("After System.gc() + runFinalization");
    }

    // Example 4: WeakHashMap - keys are weakly referenced; entries vanish when keys collected
    private static void example4_WeakHashMap_KeysCollected() throws InterruptedException {
        banner("4) WeakHashMap: entries removed when keys become unreachable");

        Map<Object, String> map = new WeakHashMap<>();
        Object k1 = new Object();
        Object k2 = new Object();
        map.put(k1, "value1");
        map.put(k2, "value2");

        System.out.println("Map size with strong keys: " + map.size()); // likely 2
        k1 = null; // drop strong reference for k1
        suggestGc("Collect key k1");

        System.out.println("Map size after GC (k1 may be gone): " + map.size());
        // Keep k2 strong so entry remains
        k2 = null;
        suggestGc("Collect key k2");
        System.out.println("Map size after GC (k2 may be gone): " + map.size());
    }

    // Example 5: SoftReference - good for caches; collected under memory pressure
    private static void example5_SoftReference_AsCacheUnderMemoryPressure() throws InterruptedException {
        banner("5) SoftReference: cleared under memory pressure (cache-like)");

        SoftReference<byte[]> soft = new SoftReference<>(new byte[(int) (4 * MB)]);
        System.out.println("Soft is present initially? " + (soft.get() != null));

        if (HEAVY) {
            // Try to cause memory pressure to encourage SoftReference clearing
            ArrayList<byte[]> hog = new ArrayList<>();
            try {
                while (soft.get() != null) {
                    hog.add(new byte[(int) (8 * MB)]); // allocate blocks
                }
            } catch (OutOfMemoryError e) {
                // If OOME happens first, that's fine for demo
                System.out.println("Caught OOME while pressuring memory");
            }
            hog.clear();
        }

        suggestGc("After memory pressure");
        System.out.println("Soft cleared? " + (soft.get() == null));
    }

    // Example 6: PhantomReference with ReferenceQueue - detect post-mortem cleanup
    private static void example6_PhantomReference_WithReferenceQueue() throws InterruptedException {
        banner("6) PhantomReference + ReferenceQueue: know exactly when object is reclaimed");

        ReferenceQueue<Object> q = new ReferenceQueue<>();
        Object strong = new Object();
        PhantomReference<Object> phantom = new PhantomReference<>(strong, q);

        System.out.println("phantom.get() is always null: " + (phantom.get() == null));
        strong = null; // drop strong ref

        // Wait up to a short timeout for the phantom to enqueue after GC
        System.gc();
        Reference<?> ref = q.remove(HEAVY ? 3000 : 1000); // waits until enqueued or timeout
        System.out.println("Phantom enqueued? " + (ref == phantom));
    }

    // Example 7: Cleaner for non-memory resources, optionally combined with AutoCloseable
    private static void example7_Cleaner_ForNonMemoryResourceCleanup() throws InterruptedException {
        banner("7) Cleaner: safety net for non-memory resource cleanup");

        // Deterministic cleanup with try-with-resources
        try (CleanableResource r = new CleanableResource("deterministic")) {
            r.use();
        } // close() calls clean()

        // Non-deterministic cleanup (forgot to close): rely on Cleaner
        new CleanableResource("non-deterministic (Cleaner)").use();
        suggestGc("Encourage Cleaner to run");
        // Allow background Cleaner thread a moment
        busy(100);
    }

    // Example 8: Memory leak patterns and fixes
    private static final ArrayList<byte[]> LEAK_BUCKET = new ArrayList<>();

    private static void example8_MemoryLeakPatterns_AndFixes() throws InterruptedException {
        banner("8) Memory leak patterns and fixes");

        // Pattern A: Static collection retains references forever (leak)
        printMemory("Before leaking");
        int blocks = HEAVY ? 16 : 4; // modest for demo
        for (int i = 0; i < blocks; i++) {
            LEAK_BUCKET.add(new byte[(int) (4 * MB)]); // retain strongly
        }
        printMemory("After leaking into static list");
        suggestGc("GC won't reclaim because references are still reachable");
        printMemory("Still high (leaked)");

        // Fix: Clear or use bounded cache/eviction
        LEAK_BUCKET.clear();
        suggestGc("After clearing leak");
        printMemory("After cleanup");

        // Pattern B: Map with strong keys causing listener leaks; use WeakHashMap
        Map<Object, byte[]> listeners = new HashMap<>();
        Object tmpKey = new Object();
        listeners.put(tmpKey, new byte[(int) (2 * MB)]);
        tmpKey = null; // but entry remains because key is in the map
        suggestGc("Strong-key HashMap listener leak");
        System.out.println("HashMap size (leak persists): " + listeners.size());

        Map<Object, byte[]> weakListeners = new WeakHashMap<>();
        Object wk = new Object();
        weakListeners.put(wk, new byte[(int) (2 * MB)]);
        wk = null; // drop strong key
        suggestGc("WeakHashMap allows GC of key/entry");
        System.out.println("WeakHashMap size after GC: " + weakListeners.size());
    }

    // Example 9: Finalization is deprecated and unreliable (demo only)
    private static void example9_Finalization_Deprecated_Demo() throws InterruptedException {
        banner("9) Finalization (deprecated): may never run; do not rely on it");

        new Finalizable("finalizable-demo");
        System.gc();
        System.runFinalization();
        busy(200); // give it a moment if enabled
        System.out.println("If you saw finalize() print, it happened; if not, that's expected.");
    }

    // Example 10: ReferenceQueue + WeakReference for weak-value cache cleanup
    private static void example10_ReferenceQueue_WeakValueCache() throws InterruptedException {
        banner("10) Weak-value cache with ReferenceQueue cleanup");

        WeakValueCache<String, byte[]> cache = new WeakValueCache<>();
        byte[] v1 = new byte[(int) (2 * MB)];
        byte[] v2 = new byte[(int) (2 * MB)];

        cache.put("A", v1);
        cache.put("B", v2);
        System.out.println("Cache size after put: " + cache.size());

        // Drop strong references to values
        v1 = null;
        v2 = null;

        suggestGc("Allow values to be GC'ed");
        int sizeAfterGc = cache.size(); // processes queue internally
        System.out.println("Cache size after GC (entries with GC'ed values removed): " + sizeAfterGc);
    }

    // ===== Helpers and demo components =====

    private static void banner(String title) {
        System.out.println("\n=== " + title + " ===");
    }

    private static void suggestGc(String note) throws InterruptedException {
        System.out.println("-- GC hint: " + note);
        System.gc();
        System.runFinalization();
        // A few short waits can help the demo be more repeatable without being heavy
        busy(50);
    }

    private static void busy(long millis) throws InterruptedException {
        Thread.sleep(millis);
    }

    private static void printMemory(String label) {
        Runtime rt = Runtime.getRuntime();
        long used = (rt.totalMemory() - rt.freeMemory()) / MB;
        long total = rt.totalMemory() / MB;
        long max = rt.maxMemory() / MB;
        System.out.printf("%s | used=%dMB, total=%dMB, max=%dMB%n", label, used, total, max);
    }

    // Cleaner-based resource
    static final class CleanableResource implements AutoCloseable {
        private static final Cleaner CLEANER = Cleaner.create();

        // State to clean up; e.g., native handle, file descriptor, etc.
        private static final class State implements Runnable {
            private final String name;
            private boolean cleaned;

            State(String name) {
                this.name = name;
            }

            @Override
            public void run() {
                if (!cleaned) {
                    cleaned = true;
                    System.out.println("Cleaner running for resource: " + name);
                }
            }
        }

        private final State state;
        private final Cleaner.Cleanable cleanable;

        CleanableResource(String name) {
            this.state = new State(name);
            this.cleanable = CLEANER.register(this, state);
        }

        void use() {
            // Simulate work
        }

        @Override
        public void close() {
            // Deterministic cleanup
            cleanable.clean();
        }
    }

    // Deprecated finalization demo
    static final class Finalizable {
        private final String name;

        Finalizable(String name) {
            this.name = name;
        }

        @SuppressWarnings("deprecation")
        @Override
        protected void finalize() throws Throwable {
            System.out.println("finalize() called for: " + name);
            super.finalize();
        }
    }

    // Weak-value cache that removes entries when values are GC'ed
    static final class WeakValueCache<K, V> {
        private final Map<K, WeakValue<K, V>> map = new HashMap<>();
        private final ReferenceQueue<V> queue = new ReferenceQueue<>();

        private static final class WeakValue<K, V> extends WeakReference<V> {
            final K key;

            WeakValue(K key, V referent, ReferenceQueue<V> queue) {
                super(referent, queue);
                this.key = key;
            }
        }

        public void put(K key, V value) {
            processQueue();
            map.put(key, new WeakValue<>(key, value, queue));
        }

        public V get(K key) {
            processQueue();
            WeakValue<K, V> wv = map.get(key);
            return wv == null ? null : wv.get();
        }

        public int size() {
            processQueue();
            return map.size();
        }

        @SuppressWarnings("unchecked")
        private void processQueue() {
            Reference<? extends V> ref;
            while ((ref = queue.poll()) != null) {
                WeakValue<K, V> wv = (WeakValue<K, V>) ref;
                map.remove(wv.key, wv); // remove only if mapping still points to this ref
            }
        }
    }

    // Utility: wait for a WeakReference to be cleared (best-effort)
    private static void waitForGc(WeakReference<?> ref, String doneMessage) throws InterruptedException {
        int attempts = HEAVY ? 30 : 10;
        for (int i = 0; i < attempts && ref.get() != null; i++) {
            suggestGc("Waiting for weak reference to clear (" + (i + 1) + "/" + attempts + ")");
        }
        System.out.println(doneMessage + "? " + (ref.get() == null));
    }
}