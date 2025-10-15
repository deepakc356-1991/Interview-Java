package _10_02_jvm_internals_and_class_loading;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.StringJoiner;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * JVM Internals & Class Loading – comprehensive, runnable examples.
 *
 * Run: java _10_02_jvm_internals_and_class_loading._02_Examples
 *
 * Notes:
 * - Requires a JDK (JavaCompiler) for dynamic compilation examples. If unavailable, those are skipped.
 * - Uses temporary directories/jars; they are deleted on JVM exit where possible.
 */
public class _02_Examples {

    public static void main(String[] args) throws Exception {
        header("JVM Internals & Class Loading - Examples (" + LocalTime.now() + ")");
        exampleClassLoaderHierarchy();

        exampleForNameVsLoadClass();
        exampleStaticInitializationOrder();

        if (hasJdkCompiler()) {
            exampleConstantInliningAndInitialization();
            exampleParentDelegationParentFirstVsChildFirst();
            exampleDifferentClassIdentityAcrossLoaders();
            exampleContextClassLoaderWithServiceLoader();
            exampleResourceLoadingFromCustomLoader();
        } else {
            System.out.println("JDK compiler not available. Skipping dynamic class-loading examples.");
        }

        footer("Done");
    }

    // ------------------------------------------------------------------------
    // Example 1: ClassLoader hierarchy
    // ------------------------------------------------------------------------
    private static void exampleClassLoaderHierarchy() {
        section("1) ClassLoader hierarchy");

        printClassLoaderChain("This class", _02_Examples.class.getClassLoader());
        printClassLoaderChain("java.lang.String", String.class.getClassLoader()); // null => Bootstrap
        try {
            Class<?> cipher = Class.forName("javax.crypto.Cipher");
            printClassLoaderChain("javax.crypto.Cipher", cipher.getClassLoader());
        } catch (ClassNotFoundException ignored) {
        }
    }

    private static void printClassLoaderChain(String label, ClassLoader cl) {
        System.out.println("- " + label + ":");
        int depth = 0;
        ClassLoader cur = cl;
        while (cur != null) {
            System.out.println("   [" + depth + "] " + cur);
            cur = cur.getParent();
            depth++;
        }
        System.out.println("   [" + depth + "] <Bootstrap ClassLoader>");
    }

    // ------------------------------------------------------------------------
    // Example 2: Class.forName vs ClassLoader.loadClass (initialization behavior)
    // ------------------------------------------------------------------------
    private static void exampleForNameVsLoadClass() throws Exception {
        section("2) Class.forName vs ClassLoader.loadClass (initialization control)");

        String initDemoName = _02_Examples.InitDemo.class.getName();
        ClassLoader appCl = _02_Examples.class.getClassLoader();

        System.out.println("- Using loadClass (no initialization):");
        appCl.loadClass(initDemoName);
        System.out.println("  Loaded " + initDemoName + " without running static initializer.");

        System.out.println("- Access compile-time constant (no init): " + InitDemo.COMPILE_TIME_CONST);

        System.out.println("- Access non-compile-time constant (triggers init): " + InitDemo.NON_COMPILE_CONST);

        System.out.println("- Class.forName with initialize=false (no init):");
        Class.forName(initDemoName, false, appCl);

        System.out.println("- Class.forName with initialize=true (init already occurred above):");
        Class.forName(initDemoName, true, appCl);
    }

    // Helper for example 2
    static class InitDemo {
        static {
            System.out.println("  [InitDemo] static initializer executed");
        }

        // Compile-time constant => may be inlined by compiler; using it does not trigger class initialization
        static final int COMPILE_TIME_CONST = 42;

        // Not a compile-time constant (wrapper) => accessing triggers initialization
        static final Integer NON_COMPILE_CONST = 99;

        // Non-final static field
        static int nonFinal = 7;
    }

    // ------------------------------------------------------------------------
    // Example 3: Static initialization order (linking & initialization phases)
    // ------------------------------------------------------------------------
    private static void exampleStaticInitializationOrder() {
        section("3) Static initialization order");

        System.out.println("- Trigger InitOrder by accessing a field:");
        int v = InitOrder.b; // triggers class initialization of InitOrder
        System.out.println("  Accessed InitOrder.b = " + v);
    }

    static class InitOrder {
        static int a = printAndGive("setting a (b currently " + b + " due to default before init)", 1);
        static int b = printAndGive("setting b", 2);

        static {
            System.out.println("  [InitOrder] static block (a=" + a + ", b=" + b + ")");
        }

        private static int printAndGive(String msg, int value) {
            System.out.println("  [InitOrder] " + msg);
            return value;
        }
    }

    // ------------------------------------------------------------------------
    // Example 4: Compile-time constant inlining vs non-final field (cross loader)
    // ------------------------------------------------------------------------
    private static void exampleConstantInliningAndInitialization() throws Exception {
        section("4) Compile-time constant inlining vs non-final field across class loaders");

        Path v1 = tempDir("inline_v1");
        Path v2 = tempDir("inline_v2");

        String pkg = "demo.inlining";
        String providerConst = pkg + ".ProviderConst";
        String providerNonFinal = pkg + ".ProviderNonFinal";
        String consumer = pkg + ".Consumer";

        // v1 sources (ProviderConst=1, ProviderNonFinal=100)
        var v1Sources = List.of(
                src(providerConst, "package " + pkg + ";\n" +
                        "public class ProviderConst {\n" +
                        "  public static final int CONST = 1;\n" +
                        "  static { System.out.println(\"  [ProviderConst v1] initialized, CONST=\" + CONST); }\n" +
                        "}\n"),
                src(providerNonFinal, "package " + pkg + ";\n" +
                        "public class ProviderNonFinal {\n" +
                        "  public static int VALUE = 100;\n" +
                        "  static { System.out.println(\"  [ProviderNonFinal v1] initialized, VALUE=\" + VALUE); }\n" +
                        "}\n"),
                src(consumer, "package " + pkg + ";\n" +
                        "public class Consumer {\n" +
                        "  public static String run() {\n" +
                        "    return \"CONST=\" + ProviderConst.CONST + \"; NON_FINAL=\" + ProviderNonFinal.VALUE;\n" +
                        "  }\n" +
                        "}\n")
        );
        compileTo(v1, v1Sources, classPathOfCurrentProcess());

        // v2 sources: only providers (ProviderConst=2, ProviderNonFinal=200)
        var v2Sources = List.of(
                src(providerConst, "package " + pkg + ";\n" +
                        "public class ProviderConst {\n" +
                        "  public static final int CONST = 2;\n" +
                        "  static { System.out.println(\"  [ProviderConst v2] initialized, CONST=\" + CONST); }\n" +
                        "}\n"),
                src(providerNonFinal, "package " + pkg + ";\n" +
                        "public class ProviderNonFinal {\n" +
                        "  public static int VALUE = 200;\n" +
                        "  static { System.out.println(\"  [ProviderNonFinal v2] initialized, VALUE=\" + VALUE); }\n" +
                        "}\n")
        );
        compileTo(v2, v2Sources, classPathOfCurrentProcess());

        try (URLClassLoader parent = new URLClassLoader(new URL[]{v2.toUri().toURL()}, _02_Examples.class.getClassLoader());
             URLClassLoader child = new URLClassLoader(new URL[]{v1.toUri().toURL()}, parent)) {

            System.out.println("- Loading Consumer from child (compiled against v1). Parent has providers v2.");
            Class<?> consumerClass = Class.forName(consumer, true, child);
            Method run = consumerClass.getMethod("run");
            Object result = run.invoke(null);

            System.out.println("  Result: " + result);
            System.out.println("  Expectation: CONST came from inlined v1 (1). NON_FINAL from parent v2 (200).");
            System.out.println("  Note: ProviderConst initializer did not run because CONST was inlined.");
        }
    }

    // ------------------------------------------------------------------------
    // Example 5: Parent-first vs child-first delegation
    // ------------------------------------------------------------------------
    private static void exampleParentDelegationParentFirstVsChildFirst() throws Exception {
        section("5) Parent-first vs Child-first delegation");

        Path parentDir = tempDir("parent_first_parent");
        Path childDir = tempDir("parent_first_child");

        String fqcn = "example.shadowing.Shadowed";

        // Compile parent version
        compileTo(parentDir, List.of(
                src(fqcn, "package example.shadowing;\n" +
                        "public class Shadowed {\n" +
                        "  public String who() { return \"PARENT\"; }\n" +
                        "  public String toString() { return \"Shadowed{\"+who()+\"}\"; }\n" +
                        "}\n")
        ), classPathOfCurrentProcess());

        // Compile child version
        compileTo(childDir, List.of(
                src(fqcn, "package example.shadowing;\n" +
                        "public class Shadowed {\n" +
                        "  public String who() { return \"CHILD\"; }\n" +
                        "  public String toString() { return \"Shadowed{\"+who()+\"}\"; }\n" +
                        "}\n")
        ), classPathOfCurrentProcess());

        try (URLClassLoader parent = new URLClassLoader(new URL[]{parentDir.toUri().toURL()}, _02_Examples.class.getClassLoader());
             URLClassLoader parentFirstChild = new URLClassLoader(new URL[]{childDir.toUri().toURL()}, parent);
             ChildFirstClassLoader childFirst = new ChildFirstClassLoader(new URL[]{childDir.toUri().toURL()}, parent)) {

            // Parent-first (default)
            Class<?> c1 = parentFirstChild.loadClass(fqcn);
            Object o1 = c1.getDeclaredConstructor().newInstance();
            String s1 = (String) c1.getMethod("who").invoke(o1);
            System.out.println("- Parent-first: " + s1 + " (loaded by " + c1.getClassLoader() + ")");

            // Child-first
            Class<?> c2 = childFirst.loadClass(fqcn);
            Object o2 = c2.getDeclaredConstructor().newInstance();
            String s2 = (String) c2.getMethod("who").invoke(o2);
            System.out.println("- Child-first: " + s2 + " (loaded by " + c2.getClassLoader() + ")");
        }
    }

    // Child-first class loader (avoid for core JDK classes)
    static class ChildFirstClassLoader extends URLClassLoader {
        public ChildFirstClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            synchronized (getClassLoadingLock(name)) {
                // Already loaded?
                Class<?> c = findLoadedClass(name);
                if (c == null) {
                    if (!name.startsWith("java.") && !name.startsWith("javax.")
                            && !name.startsWith("jdk.") && !name.startsWith("sun.")) {
                        try {
                            c = findClass(name); // try child first
                        } catch (ClassNotFoundException ignored) {
                        }
                    }
                    if (c == null) {
                        c = super.loadClass(name, false); // delegate to parent
                    }
                }
                if (resolve) resolveClass(c);
                return c;
            }
        }
    }

    // ------------------------------------------------------------------------
    // Example 6: Same binary name loaded by different class loaders => different types
    // ------------------------------------------------------------------------
    private static void exampleDifferentClassIdentityAcrossLoaders() throws Exception {
        section("6) Class identity differs across class loaders (same FQCN)");

        Path dirA = tempDir("identity_A");
        Path dirB = tempDir("identity_B");
        String fqcn = "example.identity.Foo";

        String src = "package example.identity;\n" +
                "public class Foo {\n" +
                "  public String id() { return \"" + randomId(6) + "\"; }\n" +
                "  public String toString() { return \"Foo{\"+id()+\"}\"; }\n" +
                "}\n";

        compileTo(dirA, List.of(src(fqcn, src)), classPathOfCurrentProcess());
        compileTo(dirB, List.of(src(fqcn, src)), classPathOfCurrentProcess());

        try (URLClassLoader l1 = new URLClassLoader(new URL[]{dirA.toUri().toURL()}, _02_Examples.class.getClassLoader());
             URLClassLoader l2 = new URLClassLoader(new URL[]{dirB.toUri().toURL()}, _02_Examples.class.getClassLoader())) {

            Class<?> c1 = Class.forName(fqcn, true, l1);
            Class<?> c2 = Class.forName(fqcn, true, l2);

            Object o1 = c1.getDeclaredConstructor().newInstance();
            Object o2 = c2.getDeclaredConstructor().newInstance();

            System.out.println("- c1 == c2? " + (c1 == c2));
            System.out.println("- c1 loader: " + c1.getClassLoader());
            System.out.println("- c2 loader: " + c2.getClassLoader());
            System.out.println("- c1.isInstance(o2)? " + c1.isInstance(o2));
            System.out.println("- c2.isInstance(o1)? " + c2.isInstance(o1));

            try {
                // This will throw ClassCastException if attempted directly
                Method id = c1.getMethod("id");
                System.out.println("- c1#id(): " + id.invoke(o1));
                System.out.println("- c2#id(): " + c2.getMethod("id").invoke(o2));
            } catch (ClassCastException e) {
                System.out.println("  Casting across loaders failed as expected: " + e);
            }
        }
    }

    // ------------------------------------------------------------------------
    // Example 7: Thread Context ClassLoader with ServiceLoader
    // ------------------------------------------------------------------------
    private static void exampleContextClassLoaderWithServiceLoader() throws Exception {
        section("7) Thread Context ClassLoader with ServiceLoader");

        // Prepare provider classes compiled into a JAR with service file
        String svcBinaryName = _02_Examples.GreetingService.class.getName(); // binary name uses $ for nested classes
        String provider1 = "spi.impl.EnglishGreetingService";
        String provider2 = "spi.impl.SpanishGreetingService";

        String srcInterfaceUse = "import " + _02_Examples.class.getName() + ".GreetingService;\n";

        String p1src = "package spi.impl;\n" +
                srcInterfaceUse +
                "public class EnglishGreetingService implements GreetingService {\n" +
                "  public String greet(String name) { return \"Hello, \" + name + \"!\"; }\n" +
                "}\n";

        String p2src = "package spi.impl;\n" +
                srcInterfaceUse +
                "public class SpanishGreetingService implements GreetingService {\n" +
                "  public String greet(String name) { return \"Hola, \" + name + \"!\"; }\n" +
                "}\n";

        Path providersOut = tempDir("spi_providers_classes");
        compileTo(providersOut, List.of(
                src(provider1, p1src),
                src(provider2, p2src)
        ), classPathOfCurrentProcess());

        // Create JAR with META-INF/services/<service binary name> listing providers
        Path jar = providersOut.resolveSibling("spi-providers.jar");
        List<String> services = List.of(provider1, provider2);
        String servicesFile = "META-INF/services/" + svcBinaryName;
        var extraEntries = List.of(
                new JarTextEntry(servicesFile, String.join("\n", services)),
                new JarTextEntry("config/app.properties", "greeting.prefix=Hi\napp.environment=test\n")
        );
        createJarFromDirectoryWithExtras(providersOut, jar, extraEntries);

        try (URLClassLoader providerLoader = new URLClassLoader(new URL[]{jar.toUri().toURL()}, _02_Examples.class.getClassLoader())) {
            // Default (likely system CL) won't see providers in custom loader
            System.out.println("- ServiceLoader without context loader set:");
            int count1 = 0;
            for (GreetingService s : ServiceLoader.load(GreetingService.class)) {
                System.out.println("  Found provider: " + s.getClass().getName());
                count1++;
            }
            System.out.println("  Providers found: " + count1);

            // Set context loader so ServiceLoader locates META-INF/services in our JAR
            ClassLoader prev = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(providerLoader);
                System.out.println("- ServiceLoader with context loader set to providerLoader:");
                int count2 = 0;
                for (GreetingService s : ServiceLoader.load(GreetingService.class)) {
                    System.out.println("  Found provider: " + s.getClass().getName() +
                            " -> " + s.greet("JVM"));
                    count2++;
                }
                System.out.println("  Providers found: " + count2);
            } finally {
                Thread.currentThread().setContextClassLoader(prev);
            }
        }
    }

    // Shared service interface (binary name includes $ due to nesting)
    public interface GreetingService {
        String greet(String name);
    }

    // ------------------------------------------------------------------------
    // Example 8: Resource loading via ClassLoader
    // ------------------------------------------------------------------------
    private static void exampleResourceLoadingFromCustomLoader() throws Exception {
        section("8) Resource loading via ClassLoader");

        // Reuse the JAR created in previous example if possible; else build a quick one
        // For simplicity, rebuild here to ensure resource exists
        String dummy = "example.res.Dummy";
        Path out = tempDir("res_demo_classes");
        compileTo(out, List.of(src(dummy, "package example.res; public class Dummy {}")), classPathOfCurrentProcess());
        Path jar = out.resolveSibling("res-demo.jar");
        var extras = List.of(
                new JarTextEntry("config/app.properties", "region=us-east-1\nfeatureX.enabled=true\n"),
                new JarTextEntry("META-INF/app.name", "ClassLoading Demo\n")
        );
        createJarFromDirectoryWithExtras(out, jar, extras);

        try (URLClassLoader cl = new URLClassLoader(new URL[]{jar.toUri().toURL()}, _02_Examples.class.getClassLoader())) {
            System.out.println("- Attempt to load resource with system loader: config/app.properties");
            try (var is = ClassLoader.getSystemClassLoader().getResourceAsStream("config/app.properties")) {
                System.out.println("  Found? " + (is != null));
            }

            System.out.println("- Load resource with custom loader:");
            try (var is = cl.getResourceAsStream("config/app.properties")) {
                System.out.println("  Found? " + (is != null));
                if (is != null) {
                    System.out.println("  Contents:");
                    try (var br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                        br.lines().forEach(line -> System.out.println("    " + line));
                    }
                }
            }

            System.out.println("- Enumerate resources under META-INF:");
            Enumeration<URL> urls = cl.getResources("META-INF/");
            while (urls.hasMoreElements()) {
                System.out.println("  " + urls.nextElement());
            }
        }
    }

    // ==========================================================
    // Utility: JavaCompiler helpers and file/jar utilities
    // ==========================================================
    private static boolean hasJdkCompiler() {
        return ToolProvider.getSystemJavaCompiler() != null;
    }

    private static Path tempDir(String prefix) throws IOException {
        Path dir = Files.createTempDirectory(prefix + "_");
        dir.toFile().deleteOnExit();
        return dir;
    }

    private static List<Path> classPathOfCurrentProcess() {
        List<Path> cps = new ArrayList<>();
        String cp = System.getProperty("java.class.path", "");
        for (String p : cp.split(System.getProperty("path.separator"))) {
            if (!p.isBlank()) {
                cps.add(Path.of(p));
            }
        }
        return cps;
    }

    private static InMemSource src(String fqcn, String code) {
        return new InMemSource(fqcn, code);
    }

    private static void compileTo(Path outDir, List<InMemSource> sources, List<Path> classpath) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        Objects.requireNonNull(compiler, "No Java compiler available. Use a JDK.");

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager fm = compiler.getStandardFileManager(diagnostics, Locale.getDefault(), StandardCharsets.UTF_8)) {
            List<String> options = new ArrayList<>();
            options.add("-d");
            options.add(outDir.toString());
            if (classpath != null && !classpath.isEmpty()) {
                StringJoiner sj = new StringJoiner(System.getProperty("path.separator"));
                for (Path p : classpath) {
                    sj.add(p.toString());
                }
                options.add("-classpath");
                options.add(sj.toString());
            }
            JavaCompiler.CompilationTask task = compiler.getTask(null, fm, diagnostics, options, null, sources);
            boolean ok = Boolean.TRUE.equals(task.call());
            if (!ok) {
                System.err.println("Compilation failed:");
                for (Diagnostic<?> d : diagnostics.getDiagnostics()) {
                    System.err.println("  " + formatDiagnostic(d));
                }
                throw new IllegalStateException("Compilation failed");
            }
        }
    }

    private static String formatDiagnostic(Diagnostic<?> d) {
        StringBuilder sb = new StringBuilder();
        try (Formatter f = new Formatter(sb, Locale.ROOT)) {
            f.format("%s: %s:%d:%d %s",
                    d.getKind(),
                    d.getSource() != null ? d.getSource().getName() : "<no-source>",
                    d.getLineNumber(), d.getColumnNumber(),
                    d.getMessage(Locale.ROOT));
        }
        return sb.toString();
    }

    private static void createJarFromDirectoryWithExtras(Path classesDir, Path jarFile, List<JarTextEntry> extraEntries) throws IOException {
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jarFile))) {
            // Add compiled classes
            try (var stream = Files.walk(classesDir)) {
                stream.filter(Files::isRegularFile).forEach(p -> {
                    String entryName = classesDir.relativize(p).toString().replace('\\', '/');
                    try {
                        addJarEntry(jos, entryName, Files.readAllBytes(p));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
            // Add extra text entries
            for (JarTextEntry e : extraEntries) {
                addJarEntry(jos, e.path, e.text.getBytes(StandardCharsets.UTF_8));
            }
        }
        jarFile.toFile().deleteOnExit();
    }

    private static void addJarEntry(JarOutputStream jos, String name, byte[] content) throws IOException {
        JarEntry entry = new JarEntry(name);
        jos.putNextEntry(entry);
        jos.write(content);
        jos.closeEntry();
    }

    static final class JarTextEntry {
        final String path;
        final String text;

        JarTextEntry(String path, String text) {
            this.path = path;
            this.text = text;
        }
    }

    // In-memory Java source
    static final class InMemSource extends SimpleJavaFileObject {
        private final String code;

        InMemSource(String fqcn, String code) {
            super(toUri(fqcn), Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }

        private static URI toUri(String fqcn) {
            String rel = fqcn.replace('.', '/') + Kind.SOURCE.extension;
            return URI.create("string:///" + rel);
        }

        @Override
        public String toString() {
            return "InMemSource[" + uri + "]";
        }
    }

    // ------------------------------------------------------------------------
    // Small helpers
    // ------------------------------------------------------------------------
    private static void header(String s) {
        System.out.println();
        System.out.println("=== " + s + " ===");
    }

    private static void footer(String s) {
        System.out.println("=== " + s + " ===");
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("-- " + title + " --");
    }

    private static String randomId(int n) {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            int idx = (int) (Math.random() * alphabet.length());
            sb.append(alphabet.charAt(idx));
        }
        return sb.toString();
    }

    // Cleanup helper (unused currently)
    @SuppressWarnings("unused")
    private static void safeClose(Closeable c) {
        if (c != null) try { c.close(); } catch (IOException ignored) {}
    }
}