package _10_02_jvm_internals_and_class_loading;

import java.lang.management.CompilationMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/*
JVM Internals & Class Loading — Theory + Practical Notes (read the comments)

High-level JVM architecture
- Interpreter + JIT compiler(s)
  - Tiered compilation: C1 (client/quick) and C2 (server/optimizing). Hot code is profiled, then compiled.
  - Code Cache stores compiled machine code.
  - Deoptimization allows JVM to fall back to interpreter if assumptions break.
- Runtime data areas (per JLS/JVMS)
  - Heap: all objects; GC-managed.
  - Java Stack (per-thread): stack frames with local vars, operand stack; holds references/primitive values (not objects).
  - Native Method Stack (per-thread): for JNI calls.
  - PC Register (per-thread): current bytecode instruction address.
  - Method Area: class metadata, runtime constant pool, code; in HotSpot stored in Metaspace (native memory).
  - Runtime Constant Pool: per-class symbol table for literals, method/field refs, invokedynamic bootstrap info.
- Garbage collectors (examples; choice depends on JVM/version/flags)
  - Serial, Parallel, CMS (removed in newer JDKs), G1 (default in modern JDKs), ZGC, Shenandoah.
  - Concepts: generations (young/old), pauses/safepoints, write barriers, remembered sets, concurrent phases, compaction.
- Java Memory Model (JMM) basics
  - Happens-before: program order, monitor locks, volatile reads/writes, thread start/join.
  - Visibility vs Atomicity: volatile ensures visibility + ordering but not compound atomicity; synchronized ensures mutual exclusion + HB.

Class File & Linking
- Class file structure (very condensed)
  - Magic cafebabe, version, constant pool, access flags, this/super, interfaces, fields, methods, attributes.
  - Bytecode executed by interpreter/JIT; verification ensures type safety, stack discipline.
- Class life-cycle
  1) Loading: bytes -> Class<?> object (from class loader).
  2) Linking:
     - Verification: bytecode safety check.
     - Preparation: allocate and set default values for static fields.
     - Resolution: resolve symbolic references (may be lazy).
  3) Initialization: run <clinit> (static initializers and static field initializers).
  - A class is initialized when actively used (e.g., static field read/write unless it's a constant variable, static method call, new, reflective use, etc.).

Class Loaders and Delegation
- Bootstrap loader: loads core platform classes; not a java.lang.ClassLoader instance (appears as null).
- Platform/ClassPath loaders (JDK 9+):
  - PlatformClassLoader: loads standard library modules (not all).
  - AppClassLoader: loads application classes from class path/module path.
- Parent delegation
  - loadClass typically delegates parent-first: parent.loadClass(name); only define if parent cannot find.
  - Avoids duplicate core classes and enforces security boundaries.
- Namespaces & identity
  - Two classes with the same binary name but loaded by different class loaders are different types.
- Context Class Loader (TCCL)
  - Thread-local loader used by frameworks (e.g., ServiceLoader, SPI) to locate application classes when code runs in library context.
- Class loading APIs
  - Class.forName(name): load + initialize (by default).
  - Class.forName(name, initialize=false, loader): control initialization.
  - ClassLoader.loadClass(name): load (no initialize); initialize occurs upon first active use.
- Resources
  - getResource/getResources resolve names to URLs via delegation; resources share the class loader namespace.
- Modules (JDK 9+)
  - Module meta-info, strong encapsulation; loaders map to module layers; reflective access may be restricted unless opened.

Debugging/observability tips
- Print class loading: -verbose:class or -Xlog:class+load=info (JDK 9+).
- GC logs: -Xlog:gc* (JDK 9+).
- Tools: jcmd, jmap, jstack, jstat, jfr, jlink, jdeps.
- Flags for code cache/metaspace: -XX:ReservedCodeCacheSize, -XX:MaxMetaspaceSize.

Below: runnable demonstrations. Read console output alongside code comments.
*/
public class _01_Theory {

    public static void main(String[] args) throws Exception {
        System.out.println("=== JVM Internals & Class Loading — Theory + Demos ===");
        printBasicJvmInfo();
        printMemoryAndGCInfo();
        printClassLoaderHierarchy(_01_Theory.class);

        demonstrateInitializationTriggers();
        demonstrateClassLoadingAndDelegation();
        demonstrateContextClassLoader();
        demonstrateResourceLoading();

        System.out.println("=== Done ===");
    }

    // --- JVM basics: properties, modules, JIT ---
    private static void printBasicJvmInfo() {
        System.out.println("\n-- JVM/OS/Module Info --");
        RuntimeMXBean rt = ManagementFactory.getRuntimeMXBean();
        System.out.println("Java version        : " + System.getProperty("java.version"));
        System.out.println("JVM vendor/name/ver : " + rt.getVmVendor() + " / " + rt.getVmName() + " / " + rt.getVmVersion());
        System.out.println("OS name/arch/ver    : " + System.getProperty("os.name") + " / " + System.getProperty("os.arch") + " / " + System.getProperty("os.version"));
        System.out.println("PID                 : " + pidOf(rt.getName()));
        System.out.println("ClassPath entries   : " + countSplit(System.getProperty("java.class.path")));
        System.out.println("ModulePath entries  : " + countSplit(System.getProperty("jdk.module.path")));
        System.out.println("This class module   : " + _01_Theory.class.getModule());
        System.out.println("String class module : " + String.class.getModule());

        CompilationMXBean comp = ManagementFactory.getCompilationMXBean();
        if (comp != null) {
            System.out.println("JIT name            : " + comp.getName() + " (timeMon=" + comp.isCompilationTimeMonitoringSupported() + ")");
            if (comp.isCompilationTimeMonitoringSupported()) {
                System.out.println("JIT total time (ms) : " + comp.getTotalCompilationTime());
            }
        }
    }

    private static String pidOf(String runtimeName) {
        // format: "pid@hostname"
        int at = runtimeName.indexOf('@');
        return (at > 0) ? runtimeName.substring(0, at) : runtimeName;
    }

    private static int countSplit(String value) {
        if (value == null || value.isEmpty()) return 0;
        String sep = System.getProperty("path.separator");
        return value.split(java.util.regex.Pattern.quote(sep), -1).length;
    }

    // --- Memory pools, GC info ---
    private static void printMemoryAndGCInfo() {
        System.out.println("\n-- Memory Pools --");
        MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
        System.out.println("Heap used/committed/max : " + pretty(mem.getHeapMemoryUsage()));
        System.out.println("Non-heap used/comm/max  : " + pretty(mem.getNonHeapMemoryUsage()));
        List<MemoryPoolMXBean> pools = ManagementFactory.getMemoryPoolMXBeans();
        for (MemoryPoolMXBean p : pools) {
            System.out.println("Pool: " + p.getName() + " [" + p.getType() + "] usage=" + pretty(p.getUsage()));
        }

        System.out.println("\n-- Garbage Collectors --");
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            System.out.println("GC: " + gc.getName() + " (collections=" + gc.getCollectionCount() + ", time(ms)=" + gc.getCollectionTime() + ") " +
                    "manages=" + String.join(", ", gc.getMemoryPoolNames()));
        }
    }

    private static String pretty(MemoryUsage u) {
        if (u == null) return "n/a";
        return String.format(Locale.ROOT, "used=%s, committed=%s, max=%s",
                bytes(u.getUsed()), bytes(u.getCommitted()), bytes(u.getMax()));
    }

    private static String bytes(long v) {
        if (v < 0) return "unbounded";
        double x = v;
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int i = 0;
        while (x >= 1024 && i < units.length - 1) { x /= 1024; i++; }
        return String.format(Locale.ROOT, "%.1f%s", x, units[i]);
    }

    // --- Class loader hierarchy ---
    private static void printClassLoaderHierarchy(Class<?> clazz) {
        System.out.println("\n-- Class Loader Hierarchy --");
        printHierarchy("This class", clazz);
        printHierarchy("String", String.class);

        ClassLoader app = ClassLoader.getSystemClassLoader();
        ClassLoader platform = getPlatformClassLoaderSafely();
        System.out.println("System/AppClassLoader    : " + id(app));
        System.out.println("PlatformClassLoader      : " + id(platform));
        System.out.println("BootstrapClassLoader     : null (native, not a Java object)");
    }

    private static void printHierarchy(String label, Class<?> clazz) {
        System.out.println(label + " = " + clazz.getName());
        ClassLoader l = clazz.getClassLoader(); // null => bootstrap
        int depth = 0;
        while (true) {
            System.out.println("  ".repeat(depth) + "-> loader: " + id(l));
            if (l == null) break;
            l = l.getParent();
            depth++;
        }
    }

    private static String id(ClassLoader cl) {
        return (cl == null) ? "bootstrap(null)" : (cl.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(cl)));
    }

    private static ClassLoader getPlatformClassLoaderSafely() {
        try {
            return ClassLoader.getPlatformClassLoader();
        } catch (Throwable t) {
            return null; // Pre-JDK 9
        }
    }

    // --- Initialization triggers demo ---
    private static void demonstrateInitializationTriggers() throws Exception {
        System.out.println("\n-- Initialization Triggers --");

        // 1) Compile-time constant does NOT trigger initialization (gets inlined into caller's constant pool).
        System.out.println("Access COMPILE_TIME_CONST (no init expected): " + InitDemo.COMPILE_TIME_CONST);

        // 2) Loading a class via loadClass does NOT initialize.
        String binaryName = _01_Theory.InitDemo.class.getName();
        ClassLoader app = _01_Theory.class.getClassLoader();
        System.out.println("loadClass (no init): " + app.loadClass(binaryName));

        // 3) Class.forName with initialize=false does NOT initialize.
        System.out.println("Class.forName(init=false) (no init): " + Class.forName(binaryName, false, app));

        // 4) Reading a non-constant static field triggers initialization.
        System.out.println("Access NOT_COMPILE_TIME_CONST (triggers init): " + InitDemo.NOT_COMPILE_TIME_CONST);

        // 5) New instance also requires class initialized (already happened above).
        InitDemo d = new InitDemo();
        System.out.println("Instance created: " + d);
    }

    // Helper class to demonstrate initialization
    static class InitDemo {
        static final int COMPILE_TIME_CONST = 42; // constant variable => inlined
        static final Integer NOT_COMPILE_TIME_CONST = Integer.valueOf(7); // not a compile-time constant
        static int computed = compute();

        static {
            System.out.println("InitDemo.<clinit> running (static initializer)");
        }

        private static int compute() {
            System.out.println("InitDemo.compute() invoked (during <clinit>)");
            return 123;
        }

        @Override public String toString() { return "InitDemo{computed=" + computed + "}"; }
    }

    // --- Delegation demo with a logging class loader ---
    private static void demonstrateClassLoadingAndDelegation() throws Exception {
        System.out.println("\n-- Delegation & loadClass vs forName --");
        LoggingClassLoader logging = new LoggingClassLoader(ClassLoader.getSystemClassLoader());

        // Parent-first: java.lang.String should be resolved by bootstrap via parent chain.
        Class<?> str = logging.loadClass("java.lang.String");
        System.out.println("Loaded String with loader: " + id(str.getClassLoader()) + ", module=" + str.getModule());

        // Loading our own class: already defined by AppClassLoader; delegate finds it.
        Class<?> self = logging.loadClass(_01_Theory.class.getName());
        System.out.println("Loaded this class with loader: " + id(self.getClassLoader()));

        // Class.forName initializes by default
        Class<?> c1 = Class.forName(_01_Theory.InitDemo.class.getName());
        System.out.println("Class.forName (default init) returned: " + c1.getName());
    }

    // Class loader that only logs delegation (keeps parent-first behavior)
    static class LoggingClassLoader extends ClassLoader {
        LoggingClassLoader(ClassLoader parent) { super(parent); }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            System.out.println("[LoggingClassLoader] loadClass('" + name + "'), resolve=" + resolve + " (delegating to parent-first)");
            // Parent-first: let superclass handle delegation (to parent, then findClass)
            Class<?> c = super.loadClass(name, false);
            if (resolve) resolveClass(c);
            return c;
        }

        @Override
        public URL getResource(String name) {
            System.out.println("[LoggingClassLoader] getResource('" + name + "')");
            return super.getResource(name);
        }
    }

    // --- Context Class Loader (TCCL) demo ---
    private static void demonstrateContextClassLoader() throws Exception {
        System.out.println("\n-- Thread Context Class Loader (TCCL) --");
        Thread t = Thread.currentThread();
        ClassLoader original = t.getContextClassLoader();
        System.out.println("Original TCCL: " + id(original));

        URLClassLoader temp = new URLClassLoader(new URL[0], original); // empty but distinct
        t.setContextClassLoader(temp);
        try {
            System.out.println("Updated TCCL : " + id(Thread.currentThread().getContextClassLoader()));
            // Frameworks (e.g., ServiceLoader) often look here to load user-provided implementations.
            // ServiceLoader.load(SomeSpi.class) would use TCCL by default.
        } finally {
            t.setContextClassLoader(original);
        }
    }

    // --- Resource lookup demo ---
    private static void demonstrateResourceLoading() {
        System.out.println("\n-- Resource Loading --");
        // Resources are looked up relative to class or via loader; names use '/' separators.
        String simpleName = "_01_Theory.class";
        String absolutePath = "/" + _01_Theory.class.getName().replace('.', '/') + ".class";

        URL rel = _01_Theory.class.getResource(simpleName);   // relative to this package
        URL abs = _01_Theory.class.getResource(absolutePath); // absolute from root
        URL viaCl = Objects.requireNonNull(_01_Theory.class.getClassLoader())
                .getResource(_01_Theory.class.getName().replace('.', '/') + ".class");

        System.out.println("Resource (relative): " + rel);
        System.out.println("Resource (absolute): " + abs);
        System.out.println("Resource (via CL)  : " + viaCl);
    }
}