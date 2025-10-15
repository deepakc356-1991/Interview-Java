package _10_02_jvm_internals_and_class_loading;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;

/*
JVM Internals & Class Loading – Interview Q&A (Basic → Advanced)

SECTION A. JVM ARCHITECTURE & MEMORY

Q1: What is a JVM? What are its main components?
A: JVM is the runtime for Java bytecode. Key components:
   - Class Loader Subsystem (loads/verifies/links/initializes classes)
   - Runtime Data Areas (heap, thread stacks, PC registers, native stacks, metaspace, code cache)
   - Execution Engine (interpreter + JIT compiler + GC + deoptimizer)
   - Native Interface (JNI/JNI Invocation)

Q2: Heap vs Stack vs Metaspace vs Code Cache vs Direct Memory?
A:
   - Stack: per-thread frames (locals, operand stack, return addr); no GC; StackOverflowError on deep recursion.
   - Heap: objects/arrays; GC-managed; OOM: Java heap space.
   - Metaspace: class metadata (replaced PermGen since Java 8); OOM: Metaspace.
   - Code Cache: JIT-compiled machine code; OOM: CodeCache is full.
   - Direct Memory: off-heap buffers (ByteBuffer.allocateDirect); OOM: Direct buffer memory.

Q3: What are GC roots?
A: Starting points for reachability: thread stacks' locals, static fields of loaded classes, JNI references, etc.

Q4: What types of OOM can you see?
A: Java heap space, GC overhead limit exceeded, Metaspace, Direct buffer memory, unable to create new native thread, OutOfMemoryError: requested array size exceeds VM limit.

Q5: Interpreter vs JIT? What is tiered compilation?
A: JVM interprets bytecode first; JIT compiles hot methods to machine code with profiling (C1/C2/Graal). Tiered = mix of levels for faster warmup + peak performance.

Q6: What are safepoints and STW?
A: Safepoint = point where all threads can be paused safely (for GC, deopt, etc.). STW = Stop-The-World pauses when JVM halts application threads.

Q7: What is escape analysis, TLAB, compressed oops?
A:
   - Escape analysis: promotes stack allocation and scalar replacement for non-escaping objects.
   - TLAB: per-thread heap buffers to reduce allocation contention.
   - Compressed oops: 32-bit references on 64-bit JVMs to save memory.

SECTION B. CLASS LOADING

Q8: Phases of class loading?
A: Loading → Linking (Verification, Preparation, Resolution) → Initialization (<clinit> execution).

Q9: Parent Delegation Model?
A: ClassLoader.loadClass delegates to parent first; prevents spoofing core classes and ensures one definition. Parents:
   - Bootstrap (C/C++ code; loads java.*). Represented as null in Java.
   - Platform/Extension (JDK modules, jdk.*).
   - Application/System (user classpath).

Q10: How to get class loader hierarchy?
A: See demoClassLoaderHierarchy().

Q11: Class.forName vs ClassLoader.loadClass?
A:
   - Class.forName(name) loads + links + initializes.
   - Class.forName(name, false, loader) loads + links without initialization.
   - loader.loadClass(name) loads + links without initialization. See demoForNameVsLoadClass().

Q12: When does a class initialize?
A: On first active use: new, static field write/read (when not compile-time constant), static method call, reflective/MethodHandle/VarHandle invocation. Not on reference to Foo.class or when inlining compile-time constants. See demoInitializationTriggers().

Q13: Constant inlining?
A: static final primitives/Strings known at compile time are inlined into the caller; accessing them does not trigger class init. See Holder.CONST in demoInitializationTriggers().

Q14: Static init order (inheritance)?
A: Superclass <clinit> runs before subclass <clinit>. See demoInitOrder().

Q15: Interface initialization vs class?
A: Initializing a class does not require initializing its interfaces unless actively used. Interfaces initialize when a non-constant field/method is accessed.

Q16: How to write a custom class loader?
A: Extend ClassLoader, override findClass to read bytes and call defineClass. Often keep parent delegation; sometimes use “child-first” for plugin isolation. See ChildFirstLoader.

Q17: What is class identity? Why can the same class name loaded twice be incompatible?
A: Identity = (binary name, defining ClassLoader). Same name + different loader → different types. Casting across loaders fails. See demoClassIdentityIsLoaderScoped().

Q18: NoClassDefFoundError vs ClassNotFoundException?
A:
   - ClassNotFoundException: checked, thrown by APIs like loadClass when a class cannot be found.
   - NoClassDefFoundError: error when the VM tries to load a class that was present at compile time or earlier but missing/failing during runtime (or after failed init).
   See demoErrors().

Q19: LinkageError examples?
A:
   - IncompatibleClassChangeError, NoSuchMethodError/NoSuchFieldError (binary incompatibilities)
   - UnsupportedClassVersionError (wrong classfile version)
   - ClassFormatError, VerifyError (bad bytecode)
   - LinkageError: duplicate class definition in same loader
   See explanations in code comments.

Q20: When are classes unloaded?
A: When their defining ClassLoader is unreachable and there are no live references to its classes. See demoClassUnloadingAttempt().

Q21: getResource vs getResourceAsStream, absolute vs relative?
A:
   - clazz.getResource("X") is relative to clazz’s package; leading "/" is absolute from classpath root.
   - ClassLoader.getResource("path") is always absolute from classpath root.
   See demoResources().

Q22: Thread Context Class Loader (TCCL)?
A: Used by frameworks/SPI to load implementations. Default = system loader, can be swapped per-thread. See demoContextClassLoader().

Q23: Service Provider Interface (SPI) and breaking parent delegation?
A: Sometimes “child-first” loaders or TCCL needed so plugins can override classes. Risk: classpath leaks, conflicts, ClassCastExceptions. See ChildFirstLoader and notes.

Q24: What are CDS and AppCDS?
A: Class Data Sharing: preloads class metadata into a shared archive to speed startup and reduce memory. AppCDS extends to app classes.

Q25: Module path vs classpath?
A:
   - Modules (Java 9+): strong encapsulation, explicit dependencies, module graph. Split packages disallowed.
   - Classpath: legacy flat namespace, split packages allowed (but problematic).
   See demoModules().

Q26: Reflection/MethodHandles/VarHandles and initialization?
A: Reflective instantiation triggers class init. MethodHandle lookups don’t initialize until invocation. See demoMethodHandlesInit().

Q27: What is “jar hell”?
A: Conflicting versions/duplicates on classpath leading to LinkageError/NoSuchMethodError at runtime. Use shading, dependency convergence, or modules.

Q28: Security aspects?
A: Avoid loading from untrusted sources; SecurityManager is deprecated/removed; rely on modules, minimal privileges, code signing, and review.

Q29: Why does String.class.getClassLoader() return null?
A: Bootstrap loader is implemented in native; represented as null in Java.

Q30: How do resources get found inside JARs?
A: ClassLoader reads entries via URL handlers (e.g., jar:file:/...!/path). getResource returns jar: URLs.

SECTION C. CODE DEMOS (run main)

All demos are self-contained and safe to run. Where behavior depends on the JVM/runtime, comments explain expectations.
*/
public class _03_InterviewQA {

    public static void main(String[] args) throws Throwable {
        separator("ClassLoader hierarchy");
        demoClassLoaderHierarchy();

        separator("forName vs loadClass");
        demoForNameVsLoadClass();

        separator("Initialization triggers and constant inlining");
        demoInitializationTriggers();

        separator("Static init order (Parent before Child)");
        demoInitOrder();

        separator("Child-first loader: class identity is loader-scoped");
        demoClassIdentityIsLoaderScoped();

        separator("Resources and absolute vs relative lookup");
        demoResources();

        separator("Thread Context Class Loader (TCCL)");
        demoContextClassLoader();

        separator("MethodHandles and initialization on invocation");
        demoMethodHandlesInit();

        separator("Soft/Weak/Phantom references");
        demoReferences();

        separator("Direct memory allocation");
        demoDirectMemory();

        separator("Modules");
        demoModules();

        separator("Typical class loading errors");
        demoErrors();

        separator("Attempt class unloading (best-effort, not guaranteed)");
        demoClassUnloadingAttempt();

        separator("DONE");
    }

    // ===== DEMOS =====

    static void demoClassLoaderHierarchy() {
        ClassLoader app = ClassLoader.getSystemClassLoader();
        ClassLoader platform = ClassLoader.getPlatformClassLoader();
        ClassLoader bootstrap = null; // represented as null
        System.out.println("Application loader: " + app);
        System.out.println("Platform loader   : " + platform);
        System.out.println("Bootstrap loader  : " + bootstrap + " (null means bootstrap)");
        System.out.println("String.class loader (bootstrap): " + String.class.getClassLoader());
        System.out.println("This class loader (app): " + _03_InterviewQA.class.getClassLoader());
    }

    static void demoForNameVsLoadClass() throws Exception {
        String name = _03_InterviewQA.class.getName() + "$InitDemo";

        System.out.println("Class.forName(name, false, loader) - no init expected:");
        Class<?> c1 = Class.forName(name, false, ClassLoader.getSystemClassLoader());
        System.out.println("Initialized? " + wasInitDemoInitialized());

        System.out.println("Now Class.forName(name) - initialization expected:");
        Class<?> c2 = Class.forName(name);
        System.out.println("Same class object? " + (c1 == c2));
        System.out.println("Initialized? " + wasInitDemoInitialized());

        System.out.println("loadClass(name) on already loaded class - does not re-init:");
        Class<?> c3 = _03_InterviewQA.class.getClassLoader().loadClass(name);
        System.out.println("Same class object? " + (c2 == c3));
    }

    static void demoInitializationTriggers() throws Exception {
        // Compile-time constant: should NOT trigger <clinit>
        int v = Holder.CONST;
        System.out.println("Access Holder.CONST=" + v + " (no <clinit> expected)");
        System.out.println("Holder initialized? " + wasHolderInitialized());

        // Class literal: should NOT trigger <clinit>
        Class<?> h = Holder.class;
        System.out.println("Holder.class taken (still no <clinit>)");
        System.out.println("Holder initialized? " + wasHolderInitialized());

        // Access non-constant static field: triggers <clinit>
        System.out.println("Access Holder.BOXED -> triggers <clinit>: " + Holder.BOXED);
        System.out.println("Holder initialized? " + wasHolderInitialized());
    }

    static void demoInitOrder() {
        System.out.println("Access Child.X -> expect Parent <clinit> then Child <clinit>");
        System.out.println("Child.X = " + Child.X);
    }

    // Demonstrate class identity: same binary name, different loaders => different types
    static void demoClassIdentityIsLoaderScoped() throws Exception {
        String dupName = _03_InterviewQA.class.getName() + "$Duplicate";
        ChildFirstLoader l1 = new ChildFirstLoader(_03_InterviewQA.class.getClassLoader());
        ChildFirstLoader l2 = new ChildFirstLoader(_03_InterviewQA.class.getClassLoader());

        Class<?> a = l1.loadClass(dupName);
        Object oa = a.getDeclaredConstructor().newInstance();

        Class<?> b = l2.loadClass(dupName);
        Object ob = b.getDeclaredConstructor().newInstance();

        System.out.println("a == b? " + (a == b));
        System.out.println("a.getClassLoader(): " + a.getClassLoader());
        System.out.println("b.getClassLoader(): " + b.getClassLoader());
        System.out.println("a.isInstance(ob)? " + a.isInstance(ob));
        try {
            a.cast(ob); // will throw ClassCastException
            System.out.println("Unexpected: cast succeeded");
        } catch (ClassCastException ex) {
            System.out.println("Expected ClassCastException: " + ex.getMessage());
        }

        // Now show plugin impl loaded child-first can still be used via parent-defined interface
        String implName = _03_InterviewQA.class.getName() + "$PluginImpl";
        Class<?> impl = l1.loadClass(implName);
        Object pluginObj = impl.getDeclaredConstructor().newInstance();
        Plugin p = (Plugin) pluginObj; // OK: interface Plugin comes from parent loader and is visible to child
        System.out.println("Plugin.greet(): " + p.greet());
    }

    static void demoResources() throws Exception {
        String abs = "/" + _03_InterviewQA.class.getName().replace('.', '/') + ".class";
        String rel = _03_InterviewQA.class.getSimpleName() + ".class"; // relative to package

        URL absUrl = _03_InterviewQA.class.getResource(abs);
        URL relUrl = _03_InterviewQA.class.getResource(rel);
        URL clUrl = _03_InterviewQA.class.getClassLoader()
                .getResource(_03_InterviewQA.class.getName().replace('.', '/') + ".class");

        System.out.println("Class.getResource(abs) -> " + absUrl);
        System.out.println("Class.getResource(rel) -> " + relUrl);
        System.out.println("ClassLoader.getResource -> " + clUrl);
    }

    static void demoContextClassLoader() throws Exception {
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        try {
            ChildFirstLoader pluginLoader = new ChildFirstLoader(original);
            Thread.currentThread().setContextClassLoader(pluginLoader);

            // Frameworks often do: ServiceLoader.load(Interface.class)
            ServiceLoader<Plugin> loader = ServiceLoader.load(Plugin.class);
            // We likely have no META-INF/services entry in this single-file demo, so none found:
            int count = 0;
            for (Plugin p : loader) {
                System.out.println("Found provider via TCCL: " + p.getClass() + " -> " + p.greet());
                count++;
            }
            System.out.println("Providers found via ServiceLoader: " + count + " (expected 0 in this demo)");
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }

    static void demoMethodHandlesInit() throws Throwable {
        // Lookup does not initialize; invocation does
        MethodHandle mh = MethodHandles.lookup().findStatic(MH.class, "foo", MethodType.methodType(void.class));
        System.out.println("Lookup MH done. MH initialized? " + wasMHInitialized());
        System.out.println("Invoking MH.foo() -> triggers <clinit>:");
        mh.invokeExact();
        System.out.println("MH initialized? " + wasMHInitialized());
    }

    static void demoReferences() {
        SoftReference<byte[]> sr = new SoftReference<>(new byte[1024]);
        WeakReference<Object> wr = new WeakReference<>(new Object());
        ReferenceQueue<Object> q = new ReferenceQueue<>();
        PhantomReference<Object> pr = new PhantomReference<>(new Object(), q);

        System.out.println("SoftReference.get()  != null? " + (sr.get() != null));
        System.out.println("WeakReference.get()  != null? " + (wr.get() != null));
        System.out.println("PhantomReference.get()== null? " + (pr.get() == null) + " (always null)");
    }

    static void demoDirectMemory() {
        ByteBuffer buf = ByteBuffer.allocateDirect(256);
        System.out.println("Allocated direct buffer, capacity: " + buf.capacity());
    }

    static void demoModules() {
        Module javaBase = Object.class.getModule();
        Module thisModule = _03_InterviewQA.class.getModule();
        System.out.println("Object.class module: " + javaBase.getName());
        System.out.println("This class module  : " + thisModule.getName() + " (null means unnamed/classpath)");
    }

    static void demoErrors() {
        try {
            _03_InterviewQA.class.getClassLoader().loadClass("com.example.DoesNotExist");
        } catch (ClassNotFoundException e) {
            System.out.println("ClassNotFoundException example: " + e.getMessage());
        }

        // NoClassDefFoundError typically requires a class present at compile-time but missing at runtime,
        // or a failure during initialization. We can simulate by a failing <clinit>:
        try {
            Class.forName(_03_InterviewQA.class.getName() + "$FailsToInit");
        } catch (Throwable t) {
            System.out.println("Initialization failed with: " + t);
            try {
                // Second attempt can yield NoClassDefFoundError for the same binary name in same loader
                Class.forName(_03_InterviewQA.class.getName() + "$FailsToInit");
            } catch (Throwable t2) {
                System.out.println("Subsequent load error (may be NoClassDefFoundError): " + t2);
            }
        }

        // LinkageError types are often observed only with version/binary conflicts; see comments in header.
    }

    static void demoClassUnloadingAttempt() throws Exception {
        WeakReference<ClassLoader> ref;
        WeakReference<Class<?>> clazzRef;
        {
            ChildFirstLoader loader = new ChildFirstLoader(_03_InterviewQA.class.getClassLoader());
            String name = _03_InterviewQA.class.getName() + "$Duplicate";
            Class<?> c = loader.loadClass(name);
            ref = new WeakReference<>(loader);
            clazzRef = new WeakReference<>(c);
            // Drop strong refs
            loader = null;
            c = null;
        }
        System.gc();
        System.out.println("After GC: loader collected? " + (ref.get() == null) +
                ", class collected? " + (clazzRef.get() == null) +
                " (not guaranteed)");
    }

    // ===== SUPPORT: Custom child-first loader =====

    /*
    Child-first loader to demonstrate:
    - Breaking parent delegation (for our demo package only)
    - Defining classes from the same bytes on the classpath to create loader-isolated types
    CAUTION: Child-first is dangerous outside controlled plugin use-cases (classpath leaks/conflicts).
    */
    static class ChildFirstLoader extends ClassLoader {
        private final String childFirstPkg = _03_InterviewQA.class.getPackage().getName();

        ChildFirstLoader(ClassLoader parent) { super(parent); }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            synchronized (getClassLoadingLock(name)) {
                // Already loaded?
                Class<?> c = findLoadedClass(name);
                if (c != null) {
                    if (resolve) resolveClass(c);
                    return c;
                }

                boolean childFirst = name.startsWith(childFirstPkg) && !name.startsWith("java.");
                if (childFirst) {
                    try {
                        c = findClass(name);
                        if (resolve) resolveClass(c);
                        return c;
                    } catch (ClassNotFoundException ignore) {
                        // fallback to parent
                    }
                }

                ClassLoader parent = getParent();
                c = (parent != null) ? parent.loadClass(name) : super.loadClass(name, resolve);
                if (resolve) resolveClass(c);
                return c;
            }
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            String resource = name.replace('.', '/') + ".class";
            try (InputStream in = getParentResourceAsStream(resource)) {
                if (in == null) throw new ClassNotFoundException(name);
                byte[] bytes = readAllBytes(in);
                return defineClass(name, bytes, 0, bytes.length);
            } catch (IOException e) {
                throw new ClassNotFoundException(name, e);
            }
        }

        private InputStream getParentResourceAsStream(String resource) {
            // Use parent or system to avoid calling our own load path recursively
            ClassLoader parent = getParent();
            InputStream in = (parent != null)
                    ? parent.getResourceAsStream(resource)
                    : ClassLoader.getSystemResourceAsStream(resource);
            if (in == null) {
                // As a fallback, try the current class's loader
                in = _03_InterviewQA.class.getClassLoader().getResourceAsStream(resource);
            }
            return in;
            }
    }

    // ===== DEMO CLASSES FOR INIT/ORDER/CONSTANTS =====

    static class InitDemo {
        static boolean initialized = false;
        static {
            initialized = true;
            System.out.println("InitDemo <clinit>");
        }
    }
    static boolean wasInitDemoInitialized() { return InitDemo.initialized; }

    static class Holder {
        static boolean initialized = false;
        static final int CONST = 123;                // compile-time constant
        static final Integer BOXED = Integer.valueOf(7); // NOT compile-time constant
        static {
            initialized = true;
            System.out.println("Holder <clinit>");
        }
    }
    static boolean wasHolderInitialized() { return Holder.initialized; }

    static class Parent {
        static {
            System.out.println("Parent <clinit>");
        }
    }
    static class Child extends Parent {
        static final int X = 42;
        static {
            System.out.println("Child <clinit>");
        }
    }

    static class MH {
        static boolean initialized = false;
        static {
            initialized = true;
            System.out.println("MH <clinit>");
        }
        static void foo() {
            System.out.println("MH.foo");
        }
    }
    static boolean wasMHInitialized() { return MH.initialized; }

    // Duplicate class for class identity demo
    public static class Duplicate {
        @Override public String toString() { return "Duplicate@" + System.identityHashCode(this); }
    }

    // Plugin demo
    public interface Plugin {
        String greet();
    }
    public static class PluginImpl implements Plugin {
        @Override public String greet() {
            return "hello from " + getClass().getClassLoader();
        }
    }

    // Class that fails to initialize
    static class FailsToInit {
        static {
            if (true) throw new RuntimeException("Boom in <clinit>");
        }
    }

    // ===== UTIL =====

    static void separator(String title) {
        String bar = "===========================";
        System.out.println("\n" + bar + " " + title + " " + bar);
    }

    static byte[] readAllBytes(InputStream in) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(Math.max(32, in.available()));
        byte[] buf = new byte[8192];
        int r;
        while ((r = in.read(buf)) >= 0) bos.write(buf, 0, r);
        return bos.toByteArray();
    }
}