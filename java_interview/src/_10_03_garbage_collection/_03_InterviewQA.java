package _10_03_garbage_collection;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.ref.Cleaner;
import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/*
INTERVIEW Q&A: JAVA GARBAGE COLLECTION (BASIC -> INTERMEDIATE -> ADVANCED)

BASIC
1) What is Garbage Collection (GC)?
   - Automatic memory management that reclaims heap space of objects that are no longer reachable.

2) What makes an object eligible for GC?
   - When it is not reachable from any GC root.

3) What are GC Roots?
   - Examples:
     - Local variables on thread stacks
     - Active threads
     - Static fields of loaded classes
     - JNI references (native code)
     - System class loader and some JVM internal roots
     - Synchronization monitors
   - If an object can be reached by traversing references from a root, it is live.

4) Does Java use reference counting?
   - No. It uses reachability analysis. Hence cycles do not prevent collection.

5) Are circular references a problem?
   - No, as long as the cycle is unreachable from roots.

6) Heap vs Stack?
   - Heap: objects and arrays; managed by GC.
   - Stack: method frames, local variables, references; not managed by GC.

7) What is the Generational Hypothesis?
   - Most objects die young. Hence generational heaps (young/old) are efficient.

8) Young/Old generation basics
   - Young (Eden + Survivor S0/S1): high allocation rate, frequent minor GCs, cheap.
   - Old (Tenured): holds long-lived objects, collected less often (major/mixed/full collections).

9) What is Stop-The-World (STW)?
   - Phases where all application threads pause so the GC can do specific work (e.g., root scanning, remark).

10) What is finalize()?
   - A legacy hook called once before GC reclaims an object. Deprecated for removal since JDK 9. Unreliable and dangerous; do not use. Prefer AutoCloseable/Cleaner.

11) Difference between finalize and finally?
   - finally: language construct executed predictably at scope exit.
   - finalize: GC mechanism, non-deterministic, may never run.

12) How to request GC?
   - System.gc() / Runtime.getRuntime().gc(). Just a hint. JVM may ignore; some collectors may run a concurrent cycle instead (-XX:+ExplicitGCInvokesConcurrent for supported collectors).

13) Can you predict when GC runs?
   - No. It is non-deterministic and driven by allocation failure, heuristics, and configuration.

14) What gets garbage-collected?
   - Heap objects and arrays. Not directly: thread stacks, most native memory, file descriptors, sockets. Those need explicit close or Cleaners.

15) What is Metaspace?
   - Stores class metadata in native memory (replaced PermGen since Java 8). Tuned by -XX:MaxMetaspaceSize.

INTERMEDIATE
16) Minor vs Major vs Full GC
   - Minor: young generation only; fast, STW.
   - Major/Mixed (G1): includes old regions with young; STW parts.
   - Full: whole heap; often slow; try to avoid.

17) Promotion and Tenuring
   - Surviving objects move Eden -> Survivor -> Old.
   - Tuning knobs: -XX:MaxTenuringThreshold, -XX:TargetSurvivorRatio, -XX:SurvivorRatio.

18) TLAB (Thread-Local Allocation Buffer)
   - Each thread gets a small chunk for fast bump-the-pointer allocation. Reduces contention.

19) Common collectors
   - Serial: single-threaded, small heaps, embedded.
   - Parallel (Throughput): multi-threaded STW, good throughput.
   - G1 (default since JDK 9): region-based, predictable pauses, mixed collections, compacts incrementally.
   - ZGC: low-latency, concurrent relocation, colored pointers, generational since JDK 21.
   - Shenandoah: low-latency, concurrent compaction.

20) Soft/Weak/Phantom references
   - SoftReference: cache-like; cleared under memory pressure.
   - WeakReference: cleared eagerly when only weakly reachable; used by canonical maps/WeakHashMap keys.
   - PhantomReference: enqueued after object becomes phantom reachable (after finalization if any), used for post-mortem cleanup with ReferenceQueue. get() always returns null.

21) WeakHashMap vs HashMap
   - WeakHashMap holds weak keys; entries disappear when keys are only weakly reachable. Not for strong caches.

22) Memory leaks in Java?
   - Yes, when references are unintentionally retained:
     - Static caches/maps
     - Long-lived collections
     - Unremoved listeners/callbacks
     - ThreadLocal values not removed in thread pools
     - ClassLoader leaks (e.g., from static singletons, ThreadLocals)
     - Unclosed resources preventing buffers/native memory reclamation

23) Class unloading
   - A class can be unloaded when its ClassLoader is unreachable and no live classes from it remain loaded. Often relevant in plugin/containers.

24) OutOfMemoryError types
   - Java heap space
   - GC overhead limit exceeded
   - Metaspace
   - Direct buffer memory
   - Unable to create new native thread
   - Requesting too big array/string

25) Direct memory and GC
   - off-heap DirectByteBuffer uses Cleaner to free memory. Still need to release references or close your wrapping resource; otherwise off-heap memory can be retained.

26) Tools to diagnose GC/memory
   - jcmd, jmap, jstat, jconsole, jvisualvm, Java Flight Recorder/Java Mission Control, Eclipse MAT, async-profiler (alloc), GC logs.

27) GC logging (examples)
   - -Xlog:gc* (JDK 9+) or -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:gc.log (pre-9)
   - -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=...

28) Containerized environments
   - JVM respects cgroup limits:
     - -XX:InitialRAMPercentage, -XX:MaxRAMPercentage, -XX:MinRAMPercentage
     - -XX:MaxDirectMemorySize
     - -XX:MaxMetaspaceSize
   - Choose collector per latency/throughput goals.

ADVANCED
29) Barriers and remembered sets
   - Write barriers mark card tables or update remembered sets so young collections can avoid scanning the entire old gen.
   - G1 uses remembered sets per region (card-based) and SATB (snapshot-at-the-beginning) for concurrent marking.

30) SATB vs incremental update
   - SATB records the old value at write time during marking. Used by G1 and Shenandoah to achieve concurrent marking correctness.

31) Humongous objects (G1)
   - Very large objects (relative to region size) occupy contiguous regions specially; can cause more full GCs if abused.

32) ZGC/Shenandoah basics
   - Region-based, concurrent relocation to avoid long pauses.
   - ZGC uses colored pointers and load barriers; Shenandoah uses Brooks pointers and barriers.
   - Generational ZGC (JDK 21+) reduces remembered set pressure and improves throughput.

33) Safepoints
   - Points where threads can be stopped for GC or deoptimization. Excess safepoint polls can impact performance.

34) Finalization pitfalls
   - Non-deterministic, can resurrect objects, runs on a single thread, backlogs, performance hazards. Prefer AutoCloseable/Cleaner.

35) Tuning strategy
   - Start with defaults (G1). Measure.
   - Define SLOs: max pause (p99), throughput, footprint.
   - If pauses too long, consider ZGC/Shenandoah.
   - Reduce allocation rate/object churn (avoid intermed collections, use pooling only when proven).
   - Right-size heap to reduce frequency of GCs but beware of long full GCs on very large heaps (choose low-latency collectors).

36) Common interview gotchas
   - Static reference prevents GC.
   - Setting to null may help earlier collection but JIT’s liveness analysis often makes it unnecessary.
   - System.gc() is a hint; can hurt performance.
   - Circular references do not leak by themselves.
   - WeakHashMap only weakens keys, not values.
   - SoftReference is not a strong cache guarantee.
   - finalize can run at most once; object may revive itself (bad practice).
   - Class unloading requires the loader to be unreachable.

CHEATSHEET: USEFUL FLAGS (VERIFY FOR YOUR JDK)
- Heap sizing
  -Xms<size> -Xmx<size>  e.g., -Xms2g -Xmx2g
- Collector selection
  -XX:+UseG1GC (default since JDK 9)
  -XX:+UseZGC
  -XX:+UseShenandoahGC
  -XX:+UseSerialGC
  -XX:+UseParallelGC
- Logging (JDK 9+)
  -Xlog:gc*,safepoint:file=gc.log:tags,uptime,level,timestamps
- Diagnostics
  -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=heap.hprof
  -XX:MaxMetaspaceSize=256m
  -XX:MaxDirectMemorySize=512m
- G1 knobs (only if you must)
  -XX:MaxGCPauseMillis=200
  -XX:InitiatingHeapOccupancyPercent=45
  -XX:NewRatio=2 or -XX:MaxNewSize / -XX:NewSize (prefer to let ergonomics decide)

CODING Q&A SNIPPETS AND DEMOS BELOW
*/
public class _03_InterviewQA {

    public static void main(String[] args) throws Exception {
        System.out.println("Garbage Collection Interview Q&A Demo");
        System.out.println("Use optional args to run demos: weakmap, softref, phantom, cleaner, threadlocal, systemgc, oome");
        printGcAndMemorySummary();

        // Run a couple of safe, fast demos by default
        demoWeakHashMap();
        demoCleaner();

        // Optional demos based on args
        for (String arg : args) {
            switch (arg.toLowerCase()) {
                case "weakmap": demoWeakHashMap(); break;
                case "softref": demoSoftReference(); break;
                case "phantom": demoPhantomReference(); break;
                case "cleaner": demoCleaner(); break;
                case "threadlocal": demoThreadLocalLeakPattern(); break;
                case "systemgc": demoSystemGcHint(); break;
                case "oome": demoCatchableOome(); break;
                default: System.out.println("Unknown demo: " + arg);
            }
        }
    }

    // DEMO: WeakHashMap (keys are weak, entries disappear when keys are only weakly reachable)
    static void demoWeakHashMap() throws InterruptedException {
        System.out.println("\n[Demo] WeakHashMap key cleanup");
        Map<Object, String> map = new WeakHashMap<>();
        Object k1 = new Object();
        Object k2 = new Object();
        map.put(k1, "value1");
        map.put(k2, "value2");
        System.out.println("Before GC, size=" + map.size() + " keys=" + map.keySet().size());

        // Drop strong ref to k1
        k1 = null;

        // Encourage cleanup
        System.gc();
        Thread.sleep(100);

        System.out.println("After GC hint, size=" + map.size() + " keys=" + map.keySet().size());
        // k1 entry may be gone; k2 remains
    }

    // DEMO: SoftReference behaves like a cache; cleared under memory pressure
    static void demoSoftReference() {
        System.out.println("\n[Demo] SoftReference cache-like behavior");
        SoftReference<byte[]> soft = new SoftReference<>(new byte[8_000_000]); // ~8MB
        System.out.println("Soft present? " + (soft.get() != null));

        // Try to create mild pressure (size kept small to avoid OOME here)
        List<byte[]> junk = new ArrayList<>();
        for (int i = 0; i < 32; i++) {
            junk.add(new byte[1_000_000]); // ~1MB each
            if (soft.get() == null) break;
        }
        System.out.println("Soft present after pressure? " + (soft.get() != null));
    }

    // DEMO: PhantomReference + ReferenceQueue for post-mortem cleanup
    static void demoPhantomReference() throws InterruptedException {
        System.out.println("\n[Demo] PhantomReference with ReferenceQueue");
        ReferenceQueue<Dummy> queue = new ReferenceQueue<>();
        Dummy d = new Dummy("phantom-demo");
        PhantomReference<Dummy> pr = new PhantomReference<>(d, queue);

        // Drop strong reference
        d = null;

        // Encourage GC
        System.gc();

        // Wait briefly for enqueuing
        Reference<? extends Dummy> polled = queue.remove(Duration.ofSeconds(2).toMillis());
        System.out.println("Phantom enqueued? " + (polled == pr));
    }

    // DEMO: Cleaner as finalize replacement
    static void demoCleaner() throws InterruptedException {
        System.out.println("\n[Demo] Cleaner for releasing non-Java resources");
        try (ResourceWithCleaner r = new ResourceWithCleaner("file-handle-or-offheap")) {
            System.out.println("Using resource: " + r.name);
        } // close() triggers cleanup deterministically

        // Also rely on GC-triggered cleanup if user forgets to close
        ResourceWithCleaner r2 = new ResourceWithCleaner("forgotten");
        r2 = null;
        System.gc();
        Thread.sleep(50);
        System.out.println("Cleaner should run soon if resource unreachable");
    }

    // DEMO: ThreadLocal retention pattern and remedy
    static void demoThreadLocalLeakPattern() throws InterruptedException {
        System.out.println("\n[Demo] ThreadLocal leak pattern and fix");
        ThreadLocal<byte[]> local = new ThreadLocal<>();
        Runnable task = () -> {
            local.set(new byte[2_000_000]); // ~2MB retained by thread
            // If this thread is pooled and local.remove() is not called,
            // the value can remain reachable as long as the thread lives.
            local.remove(); // Best practice: remove after use
        };
        Thread t = new Thread(task, "demo-thread");
        t.start();
        t.join();
        System.out.println("ThreadLocal demo completed (removed value to avoid retention).");
    }

    // DEMO: System.gc() as a hint
    static void demoSystemGcHint() {
        System.out.println("\n[Demo] System.gc() hint");
        printUsedHeap("Before System.gc()");
        System.gc(); // May trigger a collection; may run concurrently depending on collector and flags
        sleepSilently(100);
        printUsedHeap("After System.gc()");
    }

    // DEMO: Catch OutOfMemoryError safely (do not run in production)
    static void demoCatchableOome() {
        System.out.println("\n[Demo] Catchable OOME (for illustration; do not do this in prod)");
        List<byte[]> sink = new ArrayList<>();
        try {
            for (int i = 0; i < Integer.MAX_VALUE; i++) {
                sink.add(new byte[4_000_000]); // 4MB
                if (i % 10 == 0) System.out.println("Allocated ~" + ((long) sink.size() * 4) + " MB");
            }
        } catch (OutOfMemoryError e) {
            System.out.println("Caught: " + e);
        } finally {
            System.out.println("Clearing references to allow GC...");
            sink.clear();
            System.gc();
        }
    }

    // Interview snippet: finalize pitfalls (do not use; just for discussion)
    @SuppressWarnings("deprecation")
    static class FinalizeResurrector {
        static FinalizeResurrector resurrected;
        @Override protected void finalize() throws Throwable {
            // Bad: resurrection
            resurrected = this;
            System.out.println("finalize() called; object resurrected");
        }
    }

    // Cleaner-based resource
    static final class ResourceWithCleaner implements AutoCloseable {
        private static final Cleaner CLEANER = Cleaner.create();
        final String name;
        final State state;
        final Cleaner.Cleanable cleanable;

        static final class State implements Runnable {
            volatile boolean closed;
            final String name;
            State(String name) { this.name = name; }
            @Override public void run() {
                if (!closed) {
                    // release native/OS/off-heap here
                    System.out.println("Cleaner ran for: " + name);
                    closed = true;
                }
            }
        }

        ResourceWithCleaner(String name) {
            this.name = name;
            this.state = new State(name);
            this.cleanable = CLEANER.register(this, state);
        }

        @Override public void close() {
            state.closed = true;
            cleanable.clean();
            System.out.println("Closed resource: " + name);
        }
    }

    static final class Dummy {
        final String id;
        Dummy(String id) { this.id = id; }
        @Override public String toString() { return "Dummy(" + id + ")"; }
    }

    // Utility: print GC and memory info
    static void printGcAndMemorySummary() {
        System.out.println("\nGCs in use:");
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            System.out.println(" - " + gc.getName() + " [collections=" + gc.getCollectionCount() + ", time(ms)=" + gc.getCollectionTime() + "]");
        }
        MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
        System.out.println("Heap: " + mem.getHeapMemoryUsage());
        System.out.println("Non-heap: " + mem.getNonHeapMemoryUsage());
        for (MemoryPoolMXBean p : ManagementFactory.getMemoryPoolMXBeans()) {
            System.out.println("Pool: " + p.getName() + " " + p.getType() + " " + p.getUsage());
        }
    }

    static void printUsedHeap(String prefix) {
        long used = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
        System.out.println(prefix + " usedHeap=" + human(used));
    }

    static String human(long bytes) {
        String[] u = {"B","KB","MB","GB","TB"};
        int i = 0;
        double b = bytes;
        while (b >= 1024 && i < u.length - 1) { b /= 1024; i++; }
        return String.format("%.2f%s", b, u[i]);
    }

    static void sleepSilently(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}

/*
CODING Q&A QUICK EXAMPLES

Q: Show that circular references are collectable in Java.
A: Create two objects referencing each other; drop all external references; run a GC; both are eligible because no path from GC roots exists.

Q: Implement a simple cache with SoftReference.
A: Keep SoftReference<T> to cached values. On get, if ref.get() is null, recompute and replace. Beware soft refs are cleared under pressure; do not rely for correctness.

Q: When would you prefer WeakReference?
A: To allow canonicalized objects to be reclaimed when not in active use (e.g., interning maps) or to avoid memory leaks by keys (WeakHashMap). Remember values remain strong unless also wrapped.

Q: How do you release off-heap resources?
A: Use try-with-resources and AutoCloseable. For safety nets, use Cleaner to schedule cleanup if user forgets to close. Avoid finalize().

Q: How does GC interact with ClassLoaders?
A: A ClassLoader and its classes can be unloaded when the loader is unreachable and no classes from it are in use. Prevent common leaks by not storing loader-specific objects in static singletons or ThreadLocals of system threads.

Q: How to diagnose memory leak?
A:
- Enable heap dump on OOME; analyze with MAT.
- Take periodic heap dumps (jcmd GC.heap_dump).
- Inspect dominator tree and retained sizes to find GC root paths.
- Examine GC logs for frequent full GCs or promotion failures.
- Use JFR or async-profiler (alloc) to find allocation hot spots.

Q: What causes "GC overhead limit exceeded"?
A: JVM spent too much time (default ~98%) doing GC but recovered very little memory (default <2%). Usually a severe memory pressure/leak. Increase heap or fix leaks.

Q: Major differences among collectors?
A:
- Parallel: best throughput, higher pauses.
- G1: balanced, predictable pauses, default since JDK 9.
- ZGC/Shenandoah: very low pauses, slightly lower throughput and higher CPU/footprint; great for large heaps and latency-sensitive services.

Q: How to avoid long pauses?
A:
- Choose ZGC/Shenandoah or tune G1 pause targets.
- Reduce allocation rate and large object churn.
- Avoid humongous allocations (especially in G1).
- Keep live set small by clearing caches, using bounded structures.

Q: What are humongous objects and why care?
A: In region-based collectors, very large objects are handled specially and can cause fragmentation/longer pauses. Split data structures or use off-heap if appropriate.

Q: How do SoftReference and WeakReference differ in GC timing?
A:
- Weak: cleared eagerly as soon as no strong refs remain.
- Soft: retained until memory pressure forces clearing; helpful for caches but not deterministic.

Q: Is setting references to null helpful?
A:
- Sometimes, if the variable would otherwise stay live for a long scope (e.g., large arrays in long methods). The JIT often performs liveness analysis, so explicit nulling is rarely necessary. Prefer smaller scopes.

Q: Can GC collect a static field’s object?
A:
- Not while the class is loaded and the static field holds a reference. Clear static refs when no longer needed.

Q: What triggers class unloading?
A:
- During GC, if the ClassLoader is unreachable and no classes it defined are reachable.

Q: How to read GC logs at a glance?
A:
- Look for: frequency of collections, pause durations, promotion failures, concurrent cycle times, heap occupancy before/after, allocation rates. Continuous full GCs indicate severe pressure or fragmentation.

VERSION NOTES
- Default GC is G1 since JDK 9.
- PermGen removed in Java 8; replaced by Metaspace (native).
- ZGC became production in JDK 15; generational ZGC in JDK 21.
- CMS removed in JDK 14.
- finalize() deprecated for removal since JDK 9; avoid its use.

FURTHER PRACTICE
- Build a small LRU cache and compare behavior with SoftReference-based cache under memory pressure.
- Use JFR to measure allocation hotspots and GC pause distribution.
- Experiment with -Xms/-Xmx, -XX:+UseG1GC vs -XX:+UseZGC, and -Xlog:gc* on a sample workload.

END
*/