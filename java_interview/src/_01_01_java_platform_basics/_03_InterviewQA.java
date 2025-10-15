package _01_01_java_platform_basics;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 Interview Q&A (with runnable examples) for Java Platform Basics

 How to compile:
   javac _01_01_java_platform_basics/_03_InterviewQA.java

 How to run:
   java _01_01_java_platform_basics._03_InterviewQA

 Tips to explore bytecode and dependencies:
   javap -p -c _01_01_java_platform_basics._03_InterviewQA
   jdeps --multi-release base _01_01_java_platform_basics/_03_InterviewQA.class

 Q&A index (search in this file for the question prefix to jump):
   [BASICS] What are JDK, JRE, JVM?
   [BASICS] What is bytecode? How does Java achieve "Write Once, Run Anywhere"?
   [BASICS] What is the entry point? Valid main signatures?
   [BASICS] Source file rules: packages, imports, public classes.
   [BASICS] Primitive types, sizes, defaults. Are they platform-dependent?
   [BASICS] Is Java pass-by-reference or pass-by-value?
   [BASICS] What are String, StringBuilder, StringBuffer? String Pool and intern?
   [BASICS] What is the classpath? JAR vs class directory? Running code.
   [BASICS] equals vs ==. hashCode contract.

   [INTERMEDIATE] What is class loading? Bootstrap/Platform/Application loaders, delegation.
   [INTERMEDIATE] Class initialization triggers and constant inlining.
   [INTERMEDIATE] What is the Java Memory Model? Stack vs Heap vs Metaspace.
   [INTERMEDIATE] What is JIT? Interpreter vs C1/C2. Why warm-up?
   [INTERMEDIATE] Garbage collectors (Serial, Parallel, G1, ZGC, Shenandoah), -Xms/-Xmx.
   [INTERMEDIATE] Checked vs unchecked exceptions. try-with-resources and suppressed exceptions.
   [INTERMEDIATE] Default charset and file encodings.
   [INTERMEDIATE] Floating-point pitfalls. BigDecimal note.
   [INTERMEDIATE] Unicode, char vs code points, surrogate pairs.

   [ADVANCED] Modules (JPMS): module path vs class path. Encapsulation.
   [ADVANCED] Tools: javac, java, jar, javap, jlink, jpackage, jcmd, jmap, jstack, jfr.
   [ADVANCED] Multi-release JAR, class file version, UnsupportedClassVersionError.
   [ADVANCED] ServiceLoader, context class loader (brief).
   [ADVANCED] Reflection access and strong encapsulation (illegal-access, --add-opens).
   [ADVANCED] Direct vs heap memory (ByteBuffer), byte order.

 Note: Comments contain concise answers. Methods print small demos.
*/
public class _03_InterviewQA {

    public static void main(String[] args) {
        // Small demonstrations you can run quickly. Feel free to comment others if needed.
        section("Runtime info");
        printRuntimeInfo();

        section("Entry point + main overloading");
        mainNoArgs(); // Overload exists but JVM entry is only public static void main(String[])

        section("Classpath and working directory");
        printEnvInfo();

        section("Primitives and defaults");
        showPrimitiveDefaults();

        section("Pass-by-value semantics");
        demoPassByValue();

        section("String, immutability, pool, equals vs ==");
        demoStrings();

        section("Wrapper caching and autoboxing");
        demoAutoboxingCaching();

        section("Class loading and loaders");
        demoClassLoaders();

        section("Class initialization and constant inlining");
        demoClassInitialization();

        section("Exceptions: checked/unchecked, try-with-resources, suppressed");
        demoExceptionsAndSuppressed();

        section("Floating-point pitfalls");
        demoFloatingPoints();

        section("Unicode and code points");
        demoUnicode();

        section("ByteBuffer and byte order");
        demoByteBufferOrder();

        section("GC hints and memory");
        demoGcAndMemory();

        // Optional: assertion demo (enable with -ea). Will throw when enabled.
        // assert 2 + 2 == 5 : "Assertions enabled: 2+2 != 5";

        section("Done");
        System.out.println("Review comments in the source for Q&A details.");
    }

    // [BASICS] What are JDK, JRE, JVM?
    // A:
    // - JVM: The virtual machine that loads class files and executes bytecode with JIT.
    // - JRE: JVM + standard libraries to run apps (runtime). Historically separate; now use a JDK runtime image.
    // - JDK: Compiler (javac) + tools (jar, jlink, jpackage, javadoc, etc.) + runtime to develop and run apps.

    // [BASICS] What is bytecode? How does Java achieve "Write Once, Run Anywhere"?
    // A:
    // - javac compiles .java to platform-neutral .class bytecode.
    // - The JVM for each platform interprets/JITs the same bytecode, enabling portability.

    // [BASICS] What is the entry point? Valid main signatures?
    // A:
    // - Entry point: public static void main(String[] args)
    // - Varargs form public static void main(String... args) is also valid. Overloads are ignored by the launcher.

    private static void mainNoArgs() {
        // Demonstrates that JVM ignores overloaded main methods at launch.
        System.out.println("Called overloaded main() manually from the real main entry.");
    }

    // [BASICS] Source file rules: packages, imports, public classes.
    // A:
    // - At most one public top-level class per .java file; file name must match that class.
    // - You may have multiple package-private top-level classes (see AnotherTopLevel at bottom).
    // - package must match folder structure; imports bring types into scope.

    // [BASICS] Primitive types, sizes, defaults. Are they platform-dependent?
    // A:
    // - Sizes are fixed by the spec (platform independent):
    //   byte(8), short(16), int(32), long(64), char(16, unsigned UTF-16 code unit), float(32), double(64), boolean(virtual machine dependent but treated as true/false).
    // - Default values for fields: 0, 0.0, false, '\u0000', null for refs. Locals have no default (must be assigned before use).

    private static void showPrimitiveDefaults() {
        FieldDefaults fd = new FieldDefaults();
        System.out.println("Default int field: " + fd.i);         // 0
        System.out.println("Default boolean field: " + fd.b);      // false
        System.out.println("Default char field: [" + fd.c + "] as int=" + (int) fd.c); // 0
        System.out.println("Default object field: " + fd.o);       // null
    }

    // [BASICS] Is Java pass-by-reference or pass-by-value?
    // A:
    // - Always pass-by-value. For object parameters, the value passed is the reference value.
    // - Reassigning the parameter doesn't affect caller's reference; mutating the object does.

    private static void demoPassByValue() {
        System.out.println("Primitives and references are passed by value.");
        int x = 1;
        String s = "hi";
        StringBuilder sb = new StringBuilder("A");
        MutableInt mi = new MutableInt(10);

        mutate(x, s, sb, mi);
        System.out.println("After mutate: x=" + x + " (unchanged), s=" + s + " (unchanged), sb=" + sb + " (mutated), mi=" + mi.value + " (mutated)");
    }

    private static void mutate(int x, String s, StringBuilder sb, MutableInt mi) {
        x = 999;                // doesn't affect caller
        s = "bye";              // doesn't affect caller (String immutable, and param reassignment only)
        sb.append("+");         // mutates the same object seen by caller
        mi.value++;             // mutates object's field
    }

    // [BASICS] What are String, StringBuilder, StringBuffer? String Pool and intern?
    // A:
    // - String: immutable, stored in String pool for literals/compile-time constants. Thread-safe by design.
    // - StringBuilder: mutable, not synchronized; prefer for single-threaded concatenation in loops.
    // - StringBuffer: synchronized variant; legacy. Prefer StringBuilder unless you need intrinsic synchronization.
    // - String.intern(): returns pooled instance; use sparingly (pool memory is limited).

    private static void demoStrings() {
        String a = "hello";
        String b = "hello";
        String c = new String("hello");
        String d = c.intern();

        System.out.println("a == b (same pooled literal): " + (a == b));          // true
        System.out.println("a == c (new object): " + (a == c));                    // false
        System.out.println("a.equals(c): " + a.equals(c));                         // true
        System.out.println("a == d (interned): " + (a == d));                      // true

        String x = "ab";
        x.replace("a", "z"); // returns new string, original unchanged
        System.out.println("String immutable demo, still 'ab': " + x);
    }

    // [BASICS] What is the classpath? JAR vs class directory? Running code.
    // A:
    // - Classpath is a list of locations (folders, JARs) where classes/resources are found.
    // - Run: java -cp out:lib/* pkg.Main or java -jar app.jar (if JAR has Main-Class in MANIFEST.MF).
    // - Module path (Java 9+) is separate from class path for JPMS modules.

    private static void printEnvInfo() {
        System.out.println("user.dir (working dir): " + System.getProperty("user.dir"));
        System.out.println("java.class.path: " + System.getProperty("java.class.path"));
        System.out.println("path.separator: " + System.getProperty("path.separator"));
        System.out.println("file.separator: " + System.getProperty("file.separator"));
        System.out.println("line.separator visible as len=" + System.lineSeparator().length());
    }

    // [BASICS] equals vs ==. hashCode contract.
    // A:
    // - == compares references for objects (identity), values for primitives. Use equals for value equality.
    // - If equals is overridden, hashCode must be consistent: equal objects must have equal hash codes.

    // [INTERMEDIATE] What is class loading? Bootstrap/Platform/Application loaders, delegation.
    // A:
    // - JVM loads classes on demand via class loaders. Delegation model: ask parent first, then self.
    // - Loaders: Bootstrap (native, loads core java.*), Platform (loads JDK modules), Application (loads app classpath).
    // - Custom loaders can isolate or hot-load classes; used in containers, plugins, OSGi.

    private static void demoClassLoaders() {
        ClassLoader app = _03_InterviewQA.class.getClassLoader();
        ClassLoader parent = app != null ? app.getParent() : null;
        ClassLoader grand = parent != null ? parent.getParent() : null;

        System.out.println("App class loader: " + app);
        System.out.println("Parent loader: " + parent);
        System.out.println("Grandparent (bootstrap is null): " + grand);

        System.out.println("String.class loader (bootstrap): " + String.class.getClassLoader()); // null => bootstrap
    }

    // [INTERMEDIATE] Class initialization triggers and constant inlining.
    // A:
    // - Accessing a compile-time constant from another class does not trigger its class initialization; the value is inlined into caller's bytecode.
    // - Accessing non-constant static fields or invoking static methods triggers initialization (runs static blocks).
    private static void demoClassInitialization() {
        // Accessing compile-time constant should not initialize the class (InitConstants)
        System.out.println("InitConstants.COMPILE_TIME_INT = " + InitConstants.COMPILE_TIME_INT);
        // This access triggers class initialization (static initializer message should print before following line)
        System.out.println("InitConstants.NON_COMPILE_TIME_INT = " + InitConstants.NON_COMPILE_TIME_INT);

        // Accessing a static method also triggers initialization
        System.out.println("InitConstants.staticMethod() = " + InitConstants.staticMethod());

        // String compile-time constants are also inlined
        System.out.println("InitConstants.CONST_STR = " + InitConstants.CONST_STR);
    }

    // [INTERMEDIATE] What is the Java Memory Model? Stack vs Heap vs Metaspace.
    // A:
    // - Each thread has a call stack (frames, locals). Objects live on the heap.
    // - Class metadata lives in Metaspace (PermGen until Java 8u40). Direct buffers allocate off-heap.
    // - JMM defines happens-before rules (synchronization, volatile, final fields) for visibility and ordering.

    // [INTERMEDIATE] What is JIT? Interpreter vs C1/C2. Why warm-up?
    // A:
    // - JVM starts interpreting bytecode, profiles hotspots, then JIT-compiles hot methods to native code.
    // - C1 (client) and C2 (server) tiers; tiered compilation balances startup vs peak performance.
    // - Warm-up: performance improves after JIT optimizations kick in.

    // [INTERMEDIATE] Garbage collectors (Serial, Parallel, G1, ZGC, Shenandoah), -Xms/-Xmx.
    // A:
    // - Generational heap (young/old). Minor vs major collections.
    // - G1 is default (since Java 9). ZGC/Shenandoah offer low latency and region-based algorithms.
    // - Set heap: -Xms<size> -Xmx<size>. Avoid too many full GCs; measure with -Xlog:gc (9+) or -XX:+PrintGCDetails (8).

    private static void demoGcAndMemory() {
        Runtime rt = Runtime.getRuntime();
        long mb = 1024 * 1024;
        System.out.println("Heap: total=" + (rt.totalMemory() / mb) + "MB, free=" + (rt.freeMemory() / mb) + "MB, max=" + (rt.maxMemory() / mb) + "MB");
        System.gc(); // Just a hint; not guaranteed
        System.out.println("Requested GC (not guaranteed).");
    }

    // [INTERMEDIATE] Checked vs unchecked exceptions. try-with-resources and suppressed exceptions.
    // A:
    // - Checked: must declare or handle (e.g., IOException). Unchecked: RuntimeException and subclasses.
    // - Errors are serious conditions; don't catch unless to log/cleanup.
    // - try-with-resources closes AutoCloseable in reverse order; close exceptions become suppressed if a primary exception exists.
    private static void demoExceptionsAndSuppressed() {
        try (LoudResource r1 = new LoudResource("R1", true);
             LoudResource r2 = new LoudResource("R2", true)) {
            throw new IllegalStateException("Primary failure in try block");
        } catch (Exception e) {
            System.out.println("Caught: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            System.out.println("Suppressed exceptions count: " + e.getSuppressed().length);
            for (Throwable sup : e.getSuppressed()) {
                System.out.println("  suppressed -> " + sup.getClass().getSimpleName() + ": " + sup.getMessage());
            }
        }
    }

    // [INTERMEDIATE] Default charset and file encodings.
    // A:
    // - Charset.defaultCharset() historically depended on OS/locale; since Java 18 default is UTF-8 (JEP 400).
    // - Always specify encodings explicitly (e.g., StandardCharsets.UTF_8).
    private static void printRuntimeInfo() {
        System.out.println("java.version = " + System.getProperty("java.version"));
        System.out.println("java.vendor = " + System.getProperty("java.vendor"));
        System.out.println("java.vm.name = " + System.getProperty("java.vm.name"));
        System.out.println("os.name = " + System.getProperty("os.name") + ", os.arch = " + System.getProperty("os.arch"));
        System.out.println("Default Charset = " + Charset.defaultCharset());
        System.out.println("UTF-8 constant equals default? " + StandardCharsets.UTF_8.equals(Charset.defaultCharset()));
    }

    // [INTERMEDIATE] Floating-point pitfalls. BigDecimal note.
    // A:
    // - Binary floating-point can't represent many decimals exactly (0.1 + 0.2 != 0.3).
    // - Compare with tolerance or use BigDecimal for exact decimal arithmetic.
    private static void demoFloatingPoints() {
        double a = 0.1, b = 0.2;
        double sum = a + b;
        System.out.println("0.1 + 0.2 == 0.3 ? " + (sum == 0.3));
        System.out.println("Actual sum: " + sum);
        // NaN is not equal to itself
        System.out.println("Double.NaN == Double.NaN ? " + (Double.NaN == Double.NaN));
        System.out.println("Double.isNaN(Double.NaN) ? " + Double.isNaN(Double.NaN));
        // -0.0 and 0.0 compare equal but have different bit patterns
        System.out.println("0.0 == -0.0 ? " + (0.0 == -0.0));
    }

    // [INTERMEDIATE] Unicode, char vs code points, surrogate pairs.
    // A:
    // - char is a UTF-16 code unit (16 bits). Some characters need two chars (surrogate pair).
    // - Use codePoint APIs to iterate characters safely.
    private static void demoUnicode() {
        String s = "A😀B"; // '😀' is U+1F600, represented as surrogate pair in UTF-16
        System.out.println("String: " + s);
        System.out.println("length() (UTF-16 code units): " + s.length()); // 4
        System.out.println("codePointCount: " + s.codePointCount(0, s.length())); // 3
        int[] cps = s.codePoints().toArray();
        System.out.println("Code points: " + Arrays.toString(cps));
    }

    // [ADVANCED] Modules (JPMS): module path vs class path. Encapsulation.
    // A:
    // - Java 9+ modules describe dependencies (requires) and exports. module-info.java defines the module.
    // - Strong encapsulation hides non-exported packages by default; reflection may need --add-opens in strict mode.
    // - Run with: java --module-path mods -m my.module/pkg.Main

    // [ADVANCED] Tools: javac, java, jar, javap, jlink, jpackage, jcmd, jmap, jstack, jfr.
    // A:
    // - jar: package classes/resources. Manifest can specify Main-Class. Multi-release JAR can include versioned classes.
    // - javap: disassemble. jlink: create custom runtime image. jpackage: create native installer.
    // - jcmd/jmap/jstack/jstat/jconsole/jvisualvm/jfr: diagnose runtime, heap, threads, profiling.

    // [ADVANCED] Multi-release JAR, class file version, UnsupportedClassVersionError.
    // A:
    // - Class files have major versions (e.g., 52=Java 8, 61=Java 17). Running newer class on older JVM -> UnsupportedClassVersionError.
    // - Compile targeting older platform with: javac --release 11 (preferred) or -source/-target with proper boot classpath.

    // [ADVANCED] ServiceLoader, context class loader (brief).
    // A:
    // - ServiceLoader locates implementations declared under META-INF/services/...
    // - Thread context class loader helps frameworks load classes/resources in container environments.

    // [ADVANCED] Reflection access and strong encapsulation (illegal-access, --add-opens).
    // A:
    // - Since Java 9, accessing non-exported packages reflectively can fail; open modules or use command-line flags.
    // - SecurityManager is deprecated for removal; use modern sandboxing/containerization.

    // [ADVANCED] Direct vs heap memory (ByteBuffer), byte order.
    // A:
    // - Heap buffers live on Java heap; direct buffers off-heap via native memory (good for I/O).
    // - Default ByteBuffer order is BIG_ENDIAN; match protocol explicitly.

    private static void demoByteBufferOrder() {
        ByteBuffer bb = ByteBuffer.allocate(4); // heap buffer
        System.out.println("Default byte order: " + bb.order()); // BIG_ENDIAN
        bb.putInt(0x01020304);
        System.out.println("Array (big endian): " + Arrays.toString(bb.array())); // [1,2,3,4]

        ByteBuffer bbLE = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        bbLE.putInt(0x01020304);
        System.out.println("Array (little endian): " + Arrays.toString(bbLE.array())); // [4,3,2,1]
    }

    // Utility
    private static void section(String title) {
        System.out.println("\n--- " + title + " ---");
    }

    // Helper classes and demos

    private static class FieldDefaults {
        int i;
        boolean b;
        char c;
        Object o;
    }

    private static class MutableInt {
        int value;
        MutableInt(int value) { this.value = value; }
    }

    private static class LoudResource implements AutoCloseable {
        private final String name;
        private final boolean throwOnClose;
        LoudResource(String name, boolean throwOnClose) {
            this.name = name;
            this.throwOnClose = throwOnClose;
            System.out.println("Opened " + name);
        }
        @Override
        public void close() throws Exception {
            System.out.println("Closing " + name);
            if (throwOnClose) {
                throw new IOException("close() failed for " + name);
            }
        }
    }

    private static class InitConstants {
        static {
            System.out.println("InitConstants: static initializer ran");
        }

        // Compile-time constant (inlined at compile time in other classes)
        static final int COMPILE_TIME_INT = 42;
        static final String CONST_STR = "A" + "B" + "C"; // still compile-time constant

        // Not a compile-time constant (requires method call)
        static final int NON_COMPILE_TIME_INT = Integer.valueOf(7); // triggers class init on access

        static String staticMethod() {
            return "hello";
        }
    }
}

// Another top-level class in the same file (not public). Demonstrates that only one public class is allowed.
// Q: Can you have multiple public top-level classes in one file?
// A: No. Only one public top-level type per file, and the file name must match it.
class AnotherTopLevel {
    static String describe() {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.println("I am a package-private top-level class in the same .java file.");
        pw.flush();
        return sw.toString();
    }
}