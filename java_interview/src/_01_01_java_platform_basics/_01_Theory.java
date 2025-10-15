package _01_01_java_platform_basics;

import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/*
Java Platform Basics — Theory and Demonstration

What “Java Platform” means:
- JVM: The Java Virtual Machine executes platform-neutral bytecode (.class). It provides a managed runtime with memory management, threading, class loading, and JIT compilation.
- JRE: Java Runtime Environment = JVM + standard class libraries (rt). It runs Java applications.
- JDK: Java Development Kit = JRE + developer tools (javac, jar, javadoc, jdeps, jlink (9+), jshell (9+), etc.). Use it to compile, run, and package code.

Write once, run anywhere (WORA):
- Java source (.java) -> compiled by javac into bytecode (.class).
- Bytecode runs on any platform with a compatible JVM. Platform-dependent parts are in the JVM, not your bytecode.

HotSpot and JIT:
- HotSpot JVM profiles your code at runtime. Frequently executed “hot” code is JIT-compiled to optimized native machine code.
- Tiered compilation: C1 (client) for fast startup, C2 (server) for peak performance. G1 is default GC since Java 9; newer GCs include ZGC and Shenandoah (low-latency).

Class loading:
- Hierarchy: Bootstrap (JDK core, often “null” when queried), Platform/Ext (pre-9), Application (user code). Custom class loaders can alter loading behavior.
- Search order prevents “classes of the same name” from shadowing core Java classes (parent-first by default).

Classpath vs Module path:
- Classpath (legacy): List of JARs/directories to find classes at runtime/compile time.
- Module path (Java 9+): JPMS modules declare dependencies and encapsulation. Most apps still use classpath; modules are optional but useful for strong encapsulation and jlink.

Memory layout (conceptual):
- Heap: Objects live here; managed by GC.
- Stacks: One per thread; stores frames, local variables, references.
- Metaspace (8+): Class metadata (replaces PermGen from 7 and earlier).
- Code cache: Stores JIT-compiled native code.
- Off-heap: Direct byte buffers and native allocations (careful with limits).

Garbage Collection (GC):
- Generational hypothesis: Most objects die young. Minor (young) and major (old) collections.
- G1 GC splits heap into regions; predictable pauses. ZGC/Shenandoah aim for very low pause times.
- System.gc() is a hint, not a command. Don’t depend on it.

Type system basics:
- Primitives: boolean, byte, short, char, int, long, float, double (fixed sizes, stored by value).
- References: Point to objects on the heap (arrays, instances, String, etc.).
- Autoboxing/unboxing: Automatic conversion between primitives and wrapper classes (Integer, Long, etc.).
- String is immutable; StringBuilder/StringBuffer are mutable.

Exceptions:
- Checked: Must be declared or handled (e.g., IOException).
- Unchecked (RuntimeException): Programming errors (e.g., NullPointerException, IllegalArgumentException).
- Finally and try-with-resources ensure cleanup.

Threads and concurrency (high level):
- Thread abstraction plus high-level executors and concurrent collections.
- Synchronization primitives: synchronized, volatile, locks, atomics.
- Don’t share mutable state without proper synchronization.

Security model (high level):
- Bytecode verifier ensures type safety.
- SecurityManager (legacy, deprecated for removal in newer releases) enforced sandboxing policies; now replaced largely by other mechanisms and sandboxed runtimes.

Native interop:
- JNI/JNA allow calling native code when needed (e.g., device drivers, performance, platform APIs).

Packaging and tools:
- JAR: Zip file with classes/resources and MANIFEST.MF.
- Tools: javac, java, jar, javadoc, javap, jcmd, jstack, jmap, jfr (Flight Recorder), jdeps (deps), jlink (custom runtime, 9+), jshell (REPL, 9+).

Versioning and distribution:
- LTS releases (e.g., 8, 11, 17, 21). Non-LTS every 6 months.
- Choose an LTS for production; upgrade regularly.

How to compile/run this file (example):
- Compile: javac -d out src/_01_01_java_platform_basics/_01_Theory.java
- Run:     java -cp out _01_01_java_platform_basics._01_Theory
- Or from project root if sources are in ./src matching the package.

Below: tiny demos printing runtime information. Read comments for theory; code shows basics and safe, short-running examples.
*/
public final class _01_Theory {

    private _01_Theory() {}

    public static void main(String[] args) {
        hr("Java Platform Basics — Quick Tour");

        intro();
        platformIndependence();
        jdkJreJvm();
        bytecodeAndClassLoading();
        memoryAndGC();
        typesAndObjects();
        exceptionsBasics();
        concurrencyBasics();
        systemInfo(args);

        hr("Done");
    }

    private static void intro() {
        hr("Intro");
        println("Java source -> bytecode -> JVM executes on any OS/CPU with a compatible runtime.");
        println("Key tools: javac (compile), java (launch), jar (package), javadoc (docs), jcmd/jfr (diagnostics).");
    }

    private static void platformIndependence() {
        hr("Platform Independence (WORA)");
        println("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version") + " (" + System.getProperty("os.arch") + ")");
        println("Java: " + System.getProperty("java.version") + " | VM: " + System.getProperty("java.vm.name") + " " + System.getProperty("java.vm.version"));
        println("Runtime: " + System.getProperty("java.runtime.version") + " | Vendor: " + System.getProperty("java.vendor"));
        println("Note: Same .class/.jar can run on any platform with a matching JVM.");
    }

    private static void jdkJreJvm() {
        hr("JDK vs JRE vs JVM");
        println("java.home (runtime location): " + System.getProperty("java.home"));
        println("class file version (java.class.version): " + System.getProperty("java.class.version"));
        println("JDK adds developer tools to the runtime. JVM runs bytecode and provides services (GC, JIT, threads).");
    }

    private static void bytecodeAndClassLoading() {
        hr("Bytecode and Class Loading");
        try {
            Class<?> me = _01_Theory.class;
            Class<?> jdkClass = String.class;
            Class<?> coll = Class.forName("java.util.ArrayList");

            println("This class loader: " + clName(me.getClassLoader()));
            println("String class loader (bootstrap is usually null): " + clName(jdkClass.getClassLoader()));
            println("ArrayList class loader: " + clName(coll.getClassLoader()));

            URL myBytecode = me.getResource("_01_Theory.class");
            println("Resource lookup for this class (bytecode URL): " + (myBytecode != null ? myBytecode.toString() : "not found"));

            Thread t = Thread.currentThread();
            println("Context ClassLoader of current thread: " + clName(t.getContextClassLoader()));
            println("Tip: Use -cp (or CLASSPATH env) to point to JARs/directories. Modules (9+) use --module-path.");
        } catch (ClassNotFoundException e) {
            println("Could not load class: " + e.getMessage());
        }
    }

    private static void memoryAndGC() {
        hr("Memory and Garbage Collection");
        Runtime rt = Runtime.getRuntime();
        long used = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
        long total = rt.totalMemory() / (1024 * 1024);
        long max = rt.maxMemory() / (1024 * 1024);
        println("Processors: " + rt.availableProcessors());
        println("Heap MB (used/total/max): " + used + " / " + total + " / " + max);

        // WeakReference demo to show GC behavior (non-deterministic)
        WeakReference<byte[]> wr;
        {
            byte[] block = new byte[10_000_000]; // ~10 MB
            wr = new WeakReference<byte[]>(block);
            println("WeakReference present before GC? " + (wr.get() != null));
        } // 'block' goes out of scope; only weak reference remains
        System.gc(); // hint only
        sleep(150);
        println("WeakReference cleared after GC hint? " + (wr.get() == null));
        println("Note: GC timing is not guaranteed. This is for illustration only.");
    }

    private static void typesAndObjects() {
        hr("Types: Primitives vs References");
        int i = 42;
        Integer boxed = i; // autoboxing
        int unboxed = boxed; // unboxing
        println("Primitive int i=" + i + " | boxed Integer=" + boxed + " | unboxed=" + unboxed);

        String s1 = "java";
        String s2 = "java";
        String s3 = new String("java");
        println("String equality: s1==s2? " + (s1 == s2) + " | s1==s3? " + (s1 == s3) + " | s1.equals(s3)? " + s1.equals(s3));
        println("Strings are immutable; use StringBuilder for many concatenations.");
    }

    private static void exceptionsBasics() {
        hr("Exceptions: Checked vs Unchecked");
        try {
            Integer.parseInt("NaN");
            println("Parsing succeeded unexpectedly.");
        } catch (NumberFormatException e) {
            println("Unchecked exception caught: " + e.getClass().getSimpleName() + " -> " + e.getMessage());
        } finally {
            println("finally block executed for cleanup.");
        }

        try {
            Thread.sleep(5); // checked InterruptedException
            println("Slept briefly without interruption.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            println("Interrupted during sleep.");
        }
    }

    private static void concurrencyBasics() {
        hr("Concurrency: Executors and Tasks");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        pool.submit(namedTask("A"));
        pool.submit(namedTask("B"));
        pool.shutdown();
        try {
            if (!pool.awaitTermination(2, TimeUnit.SECONDS)) {
                println("Pool did not terminate in time; requesting shutdownNow().");
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            pool.shutdownNow();
        }
        println("Tasks completed (demonstrates basic thread usage).");
    }

    private static Runnable namedTask(final String name) {
        return new Runnable() {
            @Override public void run() {
                for (int i = 1; i <= 3; i++) {
                    println("Task " + name + " step " + i + " on " + Thread.currentThread().getName());
                    sleep(50);
                }
            }
        };
    }

    private static void systemInfo(String[] args) {
        hr("System, Launching, and Environment");
        println("main(String[] args) length: " + (args == null ? 0 : args.length));
        if (args != null && args.length > 0) {
            for (int i = 0; i < args.length; i++) println("arg[" + i + "]=" + args[i]);
        }
        println("user.dir (CWD): " + System.getProperty("user.dir"));
        println("file.encoding: " + System.getProperty("file.encoding") + " | Locale: " + Locale.getDefault());
        String cp = System.getProperty("java.class.path");
        println("java.class.path: " + (cp != null && cp.length() > 200 ? cp.substring(0, 200) + "... (truncated)" : cp));
        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome != null) println("JAVA_HOME: " + javaHome);
        println("Tip: Use 'javac' to compile and 'java -cp ... package.ClassName' to launch. Use 'jar' to package into JARs.");
        println("Note: Modules (9+) use --module-path; jlink can build minimal runtimes for deployment.");
    }

    // Utilities

    private static void hr(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }

    private static void println(String s) {
        System.out.println(s);
    }

    private static void sleep(long millis) {
        try { Thread.sleep(millis); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private static String clName(ClassLoader cl) {
        return (cl == null) ? "bootstrap (null)" : cl.getClass().getName();
    }
}