package _10_03_garbage_collection;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.ref.Cleaner;
import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 GC THEORY (Java)
 
 WHAT GC DOES
 - Automatically reclaims heap memory of objects that are no longer reachable.
 - Tracing collector: starts at GC Roots, finds reachable objects (object graph), and reclaims the rest.
 - GC Roots include:
   - Local variables and operand stacks of live threads
   - Static fields of loaded classes
   - JNI handles (native references)
   - JVM internal roots (e.g., system class loader)
 - Liveness is approximated by reachability:
   - If an object is not reachable from any root by following references, it is eligible for collection.
 
 MEMORY AREAS (managed vs. not)
 - Managed by GC:
   - Java Heap: where all objects live (Young/Old generations for generational collectors).
   - Some collectors also support class unloading (during GC) which reclaims Class metadata in Metaspace (Metaspace is native memory).
 - Not managed by GC (but can be indirectly influenced):
   - Thread stacks (for Java frames)
   - Native memory (C/C++ allocations, direct ByteBuffers' native pages, Metaspace itself)
   - Code cache (JIT-compiled code)
 
 HEAP LAYOUT (generational GCs: Serial/Parallel/G1)
 - Young Generation:
   - Eden: where new objects are allocated (bump-the-pointer + per-thread TLABs).
   - Survivor spaces (S0, S1): objects copied here during minor GCs; object "age" increments each time it survives.
 - Old/Tenured Generation:
   - Objects promoted here after aging past a threshold or due to survivor space pressure.
 - Benefits of generational design:
   - Most objects die young; young collections can be fast (copying collectors).
 - Remembered sets / card tables:
   - Tracks references from old to young to avoid scanning entire old generation during minor GC.
   - Maintained via write barriers on stores.
 
 GC PHASES AND MECHANICS
 - Stop-The-World (STW): application threads are paused (safepoint).
 - Mark: find live objects by tracing from roots.
 - Sweep: reclaim memory from unmarked objects (mark-sweep).
 - Compact / Evacuate: move live objects to eliminate fragmentation; update references.
 - Copying collection:
   - Young collections often copy live objects from Eden/one survivor to the other survivor or to Old.
 - Safepoints:
   - Places where threads can be paused safely. Long-running native calls or tight loops with no safepoint checks can delay GC.
 
 MAJOR GC FAMILIES (current mainstream)
 - Serial GC (-XX:+UseSerialGC):
   - Single-threaded, simple, good for very small heaps or single-core.
 - Parallel (Throughput) GC (-XX:+UseParallelGC):
   - Multi-threaded STW collector, focuses on high throughput; longer pauses vs. G1/Z/Shenandoah.
 - G1 GC (default since JDK 9) (-XX:+UseG1GC):
   - Region-based, concurrent marking + STW evacuation.
   - Targets predictable short pauses via -XX:MaxGCPauseMillis.
   - Mixed collections: after concurrent mark, evacuates some old regions along with young.
   - Humongous objects (>= ~50% region size) go special paths and can be costly.
 - ZGC (-XX:+UseZGC):
   - Low-latency, concurrent, region-based, colored pointers, load barriers.
   - Concurrent compaction with sub-10ms pause goals, supports very large heaps.
 - Shenandoah (-XX:+UseShenandoahGC):
   - Low-pause, concurrent compaction using Brooks pointers; aims for very short pauses regardless of heap size.
 - CMS (Concurrent Mark-Sweep):
   - Deprecated and removed (JDK 14). Replaced by G1.
 
 YOUNG VS. OLD COLLECTIONS
 - Minor GC:
   - Collects young gen. Generally fast, copying-based, frequent.
 - Major/Old GC:
   - Collects old gen (may be partially concurrent in modern collectors).
 - Full GC:
   - Collects entire heap; usually STW and the most expensive. Causes include:
     - System.gc() (if not disabled), promotion failures, severe fragmentation, metaspace pressure,
       humongous allocation failures (G1), or old gen occupancy thresholds.
 
 OBJECT ALLOCATION
 - Eden allocation is fast (bump-the-pointer).
 - TLABs (Thread-Local Allocation Buffers) give threads private bump-pointer areas for lock-free allocation.
 - Escape analysis (JIT) can allocate some objects on stack or eliminate them entirely (scalar replacement).
 - Large/humongous objects may bypass normal young placement (e.g., G1 humongous).
 
 REFERENCE TYPES AND REACHABILITY
 - Strong (normal): prevents collection as long as reachable.
 - SoftReference<T>:
   - Collected under memory pressure, intended for caches.
   - Policy controlled by -XX:SoftRefLRUPolicyMSPerMB (heuristic; VM may keep soft refs longer when memory is plentiful).
 - WeakReference<T>:
   - Collected eagerly when only weak refs remain.
   - Common use: canonicalization, WeakHashMap keys.
 - PhantomReference<T>:
   - Enqueued after object becomes phantom-reachable (post-finalization, if any).
   - get() always returns null. Use with ReferenceQueue for post-mortem cleanup and to avoid resurrection hazards.
 - ReferenceQueue:
   - Observe when refs are cleared/enqueued to perform cleanup.
 
 FINALIZATION AND CLEANERS
 - finalize():
   - Deprecated for removal (JEP 421, JDK 18+). Unpredictable, slow, can resurrect objects, can deadlock.
   - Avoid finalizers. Do not rely on them for releasing resources.
 - Cleaners (java.lang.ref.Cleaner) and PhantomReferences:
   - Prefer try-with-resources (AutoCloseable).
   - Use Cleaner/PhantomReference only as a safety net for native resources when explicit close might be missed.
 
 STOP-THE-WORLD, CONCURRENCY, AND PARALLELISM
 - Parallel: multiple GC threads work while application is paused.
 - Concurrent: some GC phases run while the application is executing (e.g., marking, remset refinement).
 - Load barriers (ZGC/Shenandoah) vs. store barriers (generational write barriers).
 - SATB (Snapshot-At-The-Beginning) used by G1 for concurrent marking correctness.
 
 FRAGMENTATION AND COMPACTION
 - Without compaction, free space can fragment, causing allocation failures even when enough total space exists.
 - Many collectors evacuate/compact live objects to keep large contiguous free areas (or logical region free-lists).
 
 G1 DETAILS (most common default)
 - Region-sized heap (1–32MB region size; chosen automatically or via -XX:G1HeapRegionSize).
 - Concurrent marking builds liveness info per region.
 - Mixed GCs evacuate selected old regions to meet pause targets.
 - Humongous objects (>= 50% of a region) are placed in special humongous regions; prefer to avoid very large arrays when possible.
 - Key flags:
   - -XX:MaxGCPauseMillis, -XX:InitiatingHeapOccupancyPercent (IHOP),
     -XX:G1NewSizePercent, -XX:G1MaxNewSizePercent,
     -XX:G1ReservePercent, -XX:ConcGCThreads, -XX:ParallelGCThreads.
 
 ZGC/SHENANDOAH DETAILS
 - Both aim for very low pause times by doing relocation concurrently.
 - ZGC uses colored pointers and load barriers; supports multi-terabyte heaps.
 - Shenandoah uses Brooks pointers and concurrent evacuation; available in many OpenJDK builds.
 
 TUNING BASICS
 - Choose collector based on goals:
   - Throughput: Parallel GC
   - Balanced default: G1
   - Ultra-low-latency: ZGC or Shenandoah
 - Size the heap:
   - -Xms (initial) and -Xmx (maximum). Keeping them equal removes resize noise.
   - In containers: -XX:InitialRAMPercentage, -XX:MaxRAMPercentage (UseContainerSupport is on by default in modern JDKs).
 - Young gen sizing:
   - -XX:NewRatio or -XX:NewSize/-XX:MaxNewSize (Parallel/Serial).
   - For G1: -XX:G1NewSizePercent, -XX:G1MaxNewSizePercent.
 - Pause/throughput goals:
   - -XX:MaxGCPauseMillis, -XX:GCTimeRatio.
 - Disable explicit GC calls:
   - -XX:+DisableExplicitGC (blocks System.gc()).
 
 LOGGING AND DIAGNOSTICS
 - Unified logging (JDK 9+):
   - -Xlog:gc  (basic)
   - -Xlog:gc*,safepoint,gc+age=trace,gc+phases=debug
   - To file: -Xlog:gc*:file=gc.log:time,uptime,level,tags
 - Tools:
   - jcmd <pid> GC.run, GC.heap_info, VM.flags, JFR.start/stop, GC.class_histogram
   - jstat -gcutil <pid> 1s
   - jmap -histo:live <pid>, jmap -dump:live,format=b,file=heap.hprof
   - JFR (Java Flight Recorder) + Mission Control (JMC)
   - VisualVM, async-profiler (alloc/CPU), YourKit, Eclipse MAT (heap dump analysis)
 
 COMMON GC TRIGGERS
 - Allocation failure in Eden/Old
 - System.gc() (if not disabled)
 - Metaspace pressure and class unloading cycles
 - Promotion failure (no room in Old during young evacuation)
 - Humongous allocation (G1)
 
 MEMORY LEAKS IN GC LANGUAGES
 - "Leak" = reachable but unnecessary objects (kept alive accidentally).
 - Causes:
   - Unbounded caches/maps, static references, listener/subscriber not removed
   - ThreadLocal values not removed in thread pools
   - Keys with bad equals()/hashCode() preventing removal
   - Inner/lambda capturing outer objects unintentionally
 - Detect with heap dumps, allocation profiling, and GC logs.
 
 DIRECT/NATIVE MEMORY
 - Not on the Java heap; GC does not directly reclaim it.
 - Direct ByteBuffer uses a Cleaner to free native memory when the Buffer becomes unreachable.
 - Limit via -XX:MaxDirectMemorySize (else ~HeapSize by default).
 - Always close native resources deterministically (try-with-resources).
 
 COMPRESSED OOPS/CLASS POINTERS (64-bit)
 - -XX:+UseCompressedOops (default for moderate heaps) reduces pointer size to 32-bit offsets.
 - -XX:+UseCompressedClassPointers compresses class metadata pointers.
 
 DO NOTS
 - Do not rely on System.gc(); it’s a hint only (and often disabled in prod).
 - Do not use finalizers; prefer AutoCloseable and try-with-resources; Cleaner only as last resort.
 
 VERSION NOTES
 - finalize() deprecated for removal since JDK 18 (JEP 421).
 - CMS removed in JDK 14. G1 is default since JDK 9.
 - ZGC and Shenandoah are production-quality in modern LTS JDKs (e.g., 17+).
 
 --------------------------------------------------------------------------------
 Below are small, safe demos. They are illustrative and not proofs of behavior; GC is non-deterministic.
 Run with:
   java _10_03_garbage_collection._01_Theory [refs|weakmap|phantom|cleaner|threadlocalleak|allocate|oome|gc]
*/
public class _01_Theory {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            printUsageAndEnvironment();
            return;
        }
        switch (args[0].toLowerCase()) {
            case "refs" -> demoReferences();
            case "weakmap" -> demoWeakHashMap();
            case "phantom" -> demoPhantomReference();
            case "cleaner" -> demoCleaner();
            case "threadlocalleak" -> demoThreadLocalLeak();
            case "allocate" -> demoAllocate();
            case "oome" -> demoOOME();
            case "gc" -> printGcInfo();
            default -> printUsageAndEnvironment();
        }
    }

    private static void printUsageAndEnvironment() {
        System.out.println("GC Theory demos. Usage:");
        System.out.println("  refs             - demonstrate Strong/Soft/Weak/Phantom basics");
        System.out.println("  weakmap          - show WeakHashMap key collection behavior");
        System.out.println("  phantom          - PhantomReference with ReferenceQueue notification");
        System.out.println("  cleaner          - Cleaner as a safety net (prefer try-with-resources)");
        System.out.println("  threadlocalleak  - example of ThreadLocal leak pattern (safe size)");
        System.out.println("  allocate         - allocate some memory and show used heap deltas");
        System.out.println("  oome             - intentionally trigger OOME (run with small -Xmx)");
        System.out.println("  gc               - print available GC(s) and basic info");
        System.out.println();
        System.out.println("Tips:");
        System.out.println(" - Enable GC logs: -Xlog:gc*,safepoint:file=gc.log:time,uptime,level,tags");
        System.out.println(" - Choose GC: -XX:+UseG1GC (default), -XX:+UseParallelGC, -XX:+UseZGC, -XX:+UseShenandoahGC");
        System.out.println(" - Heap: -Xms -Xmx (often equal), containers: -XX:MaxRAMPercentage");
    }

    // ---------------------------
    // DEMO: References (Strong/Soft/Weak/Phantom)
    // ---------------------------
    private static void demoReferences() throws InterruptedException {
        System.out.println("References demo (non-deterministic; may vary by JVM and heap pressure).");

        // Strong reference: prevents collection
        byte[] strong = new byte[1_000_000]; // 1 MB
        System.out.println("Strong ref alive: " + (strong != null));

        // Soft reference: collected under memory pressure
        SoftReference<byte[]> soft = new SoftReference<>(new byte[1_000_000]);
        System.out.println("Soft ref get() before GC: " + (soft.get() != null));

        // Weak reference: cleared eagerly once only weakly reachable
        WeakReference<byte[]> weak = new WeakReference<>(new byte[1_000_000]);
        System.out.println("Weak ref get() before GC: " + (weak.get() != null));

        // Phantom reference: get() always null; observe enqueue on ReferenceQueue
        Object phantomTarget = new Object();
        ReferenceQueue<Object> q = new ReferenceQueue<>();
        PhantomReference<Object> phantom = new PhantomReference<>(phantomTarget, q);
        System.out.println("Phantom get(): " + phantom.get());
        phantomTarget = null;

        // Drop strong refs
        strong = null;

        // Hint GC (not guaranteed)
        System.gc();
        Thread.sleep(100);

        System.out.println("Soft ref get() after GC hint: " + (soft.get() != null));
        System.out.println("Weak ref get() after GC hint: " + (weak.get() != null));

        // Allocate to create some pressure and help enqueue phantom
        var junk = new ArrayList<byte[]>();
        for (int i = 0; i < 10; i++) {
            junk.add(new byte[200_000]);
            System.gc();
            Thread.sleep(50);
            Reference<?> polled = q.poll();
            if (polled != null) {
                System.out.println("Phantom enqueued: " + (polled == phantom));
                break;
            }
        }
        System.out.println("Demo complete.");
    }

    // ---------------------------
    // DEMO: WeakHashMap behavior
    // ---------------------------
    private static void demoWeakHashMap() throws InterruptedException {
        System.out.println("WeakHashMap demo. Keys are weakly referenced; entries vanish when keys collected.");
        Map<Object, String> map = new WeakHashMap<>();

        Object strongKey = new String("weak-key-" + System.nanoTime()); // ensure not interned
        map.put(strongKey, "value");
        System.out.println("Map contains before drop: " + map.size());

        // Drop the strong reference to the key
        strongKey = null;

        // Encourage GC
        for (int i = 0; i < 20 && !map.isEmpty(); i++) {
            System.gc();
            Thread.sleep(50);
        }
        System.out.println("Map contains after GC hint: " + map.size() + " (0 means key collected)");
    }

    // ---------------------------
    // DEMO: PhantomReference + ReferenceQueue post-mortem cleanup
    // ---------------------------
    private static void demoPhantomReference() throws InterruptedException {
        System.out.println("PhantomReference demo. Use with ReferenceQueue for post-mortem cleanup.");

        class Big {
            byte[] data = new byte[2_000_000];
        }
        ReferenceQueue<Big> queue = new ReferenceQueue<>();
        Big target = new Big();
        PhantomReference<Big> pr = new PhantomReference<>(target, queue);

        target = null;
        for (int i = 0; i < 50; i++) {
            System.gc();
            Reference<? extends Big> ref = queue.poll();
            if (ref != null) {
                System.out.println("Phantom enqueued: " + (ref == pr));
                break;
            }
            Thread.sleep(50);
        }
        System.out.println("get() on phantom is always: " + pr.get());
    }

    // ---------------------------
    // DEMO: Cleaner (safety net; prefer try-with-resources)
    // ---------------------------
    private static final Cleaner CLEANER = Cleaner.create();

    private static class ResourceWithCleaner implements AutoCloseable {
        // State that holds cleanup logic
        private static final class State implements Runnable {
            private volatile boolean closed;
            private final int id;
            State(int id) { this.id = id; }
            @Override public void run() {
                if (!closed) {
                    closed = true;
                    // Perform cleanup of native resource here (demo: log only)
                    System.out.println("Cleaner ran for resource id=" + id);
                }
            }
        }

        private final State state;
        private final Cleaner.Cleanable cleanable;

        ResourceWithCleaner(int id) {
            this.state = new State(id);
            this.cleanable = CLEANER.register(this, state);
        }

        @Override public void close() {
            // Deterministic cleanup
            cleanable.clean();
        }
    }

    private static void demoCleaner() throws InterruptedException {
        System.out.println("Cleaner demo (prefer try-with-resources; Cleaner is last-resort safety net).");
        // Deterministic close
        try (ResourceWithCleaner r = new ResourceWithCleaner(1)) {
            System.out.println("Using resource id=1");
        }

        // Non-deterministic cleanup (GC must discover unreachability)
        new ResourceWithCleaner(2); // no strong ref kept
        System.out.println("Created resource id=2 without closing. Hinting GC...");
        System.gc();
        Thread.sleep(200);
        System.out.println("If GC ran, Cleaner may have executed for id=2.");
    }

    // ---------------------------
    // DEMO: ThreadLocal leak pattern
    // ---------------------------
    private static void demoThreadLocalLeak() throws InterruptedException {
        System.out.println("ThreadLocal leak demo. Thread pools retain threads -> retained ThreadLocal values if not removed.");
        final ThreadLocal<byte[]> TL = new ThreadLocal<>();

        var pool = Executors.newFixedThreadPool(4);
        for (int i = 0; i < 8; i++) {
            pool.submit(() -> {
                // 1 MB per task; pool threads will survive beyond task lifetime
                TL.set(new byte[1_000_000]);
                // Simulate work
                try { Thread.sleep(50); } catch (InterruptedException ignored) {}
                // BAD: forgetting TL.remove() can leak in long-lived pools
                // TL.remove(); // Uncomment to fix leak
            });
        }
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);

        System.out.println("If TL.remove() not called, pool threads may keep ~4MB reachable via ThreadLocalMap.");
        System.out.println("Fix: always call remove() in finally for thread pools.");
    }

    // ---------------------------
    // DEMO: Allocate memory and show used heap delta
    // ---------------------------
    private static void demoAllocate() {
        long before = usedMemory();
        var list = new ArrayList<byte[]>();
        for (int i = 0; i < 20; i++) { // ~20MB
            list.add(new byte[1_000_000]);
        }
        long mid = usedMemory();
        list.clear();
        System.gc(); // hint
        sleepSilently(200);
        long after = usedMemory();
        System.out.printf("Used before=%d MB, after alloc=%d MB, after clear+GC=%d MB%n",
                mb(before), mb(mid), mb(after));
    }

    // ---------------------------
    // DEMO: OutOfMemoryError (run with small -Xmx, e.g., -Xmx32m)
    // ---------------------------
    private static void demoOOME() {
        System.out.println("OOME demo. Run with a small heap, e.g., -Xmx32m. Catching the error...");
        var list = new ArrayList<byte[]>();
        try {
            while (true) {
                // Random blocks to reduce TLAB reuse predictability
                list.add(new byte[ThreadLocalRandom.current().nextInt(512_000, 1_500_000)]);
            }
        } catch (OutOfMemoryError e) {
            System.out.println("Caught OOME: " + e);
            System.out.println("Heap dump tip: use -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=heap.hprof");
        }
    }

    // ---------------------------
    // UTIL: Print GC info
    // ---------------------------
    private static void printGcInfo() {
        System.out.println("GCs available/active:");
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            System.out.printf(" - %s (collections=%d, time(ms)=%d)%n",
                    gc.getName(), gc.getCollectionCount(), gc.getCollectionTime());
        }
        long before = usedMemory();
        System.gc();
        sleepSilently(100);
        long after = usedMemory();
        System.out.printf("Used heap before GC hint=%d MB, after=%d MB%n", mb(before), mb(after));
    }

    // ---------------------------
    // Utilities
    // ---------------------------
    private static long usedMemory() {
        Runtime rt = Runtime.getRuntime();
        return rt.totalMemory() - rt.freeMemory();
    }

    private static long mb(long bytes) {
        return bytes / (1024 * 1024);
    }

    private static void sleepSilently(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    // ---------------------------
    // Deprecated finalization example (do not use in production)
    // ---------------------------
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated
    private static final class FinalizableExample {
        @Override
        protected void finalize() throws Throwable {
            // Extremely discouraged: non-deterministic, can resurrect, hurts GC.
            System.out.println("finalize() called (DEPRECATED FEATURE)");
        }
    }
}