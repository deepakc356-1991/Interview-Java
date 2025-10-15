package _01_01_java_platform_basics;

import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/*
Java Platform Basics — Single-file examples with explanatory comments.

What makes up "Java":
- JVM: Runs bytecode (.class), provides memory management (GC), JIT compilation, threading, etc.
- JRE: JVM + core libraries needed to run Java apps.
- JDK: JRE + development tools (javac, jar, javadoc, jshell, etc.).
- Bytecode: Output of javac (.class files) executed by the JVM. Write once, run anywhere.

Basic commands (from project root):
- Check Java:     java -version
- Compile:        javac -d out src/_01_01_java_platform_basics/_02_Examples.java
- Run (classpath):java -cp out _01_01_java_platform_basics._02_Examples
- Run with args:  java -cp out _01_01_java_platform_basics._02_Examples hello runtime
- Package JAR:
  1) Ensure a manifest with Main-Class:
     echo Main-Class: _01_01_java_platform_basics._02_Examples > MANIFEST.MF
  2) jar cfm app.jar MANIFEST.MF -C out .
  3) java -jar app.jar
- Classpath vs Module path:
  - Classpath (-cp): Traditional way to locate classes/JARs.
  - Module path (-p): Since Java 9, to locate named modules.

Environment basics:
- PATH should include your JDK's bin folder to use java/javac.
- JAVA_HOME typically points to the JDK installation directory.

How main works:
- Entry point: public static void main(String[] args)
- Alternative: public static void main(String... args)

Assertions:
- Disabled by default. Enable with: java -ea -cp out _01_01_java_platform_basics._02_Examples assert
*/

public class _02_Examples {

    public static void main(String[] args) {
        // Run all demos if no args or "all" provided. Otherwise run selected demos by name.
        Set<String> run = new HashSet<>(Arrays.asList(args));
        boolean all = run.isEmpty() || run.contains("all");

        header("Java Platform Basics — start");

        if (all || run.contains("hello")) helloWorld();
        if (all || run.contains("args")) commandLineArguments(args);
        if (all || run.contains("runtime")) printJavaRuntimeInfo();
        if (all || run.contains("stdlib")) standardLibraryDemo();
        if (all || run.contains("classpath")) classpathResourceDemo();
        if (all || run.contains("module")) moduleAndPackageInfo();
        if (all || run.contains("env")) environmentAndIO();
        if (all || run.contains("time")) timeMeasurementDemo();
        if (all || run.contains("assert")) assertionDemo();
        if (all || run.contains("classloader")) classLoaderInfo();
        if (all || run.contains("thread")) threadDemo();
        if (all || run.contains("exceptions")) exceptionsDemo();
        if (all || run.contains("paths")) pathSeparatorDemo();

        // Optional: exit with a specific code using arg like exit=7
        if (!run.isEmpty()) {
            for (String a : run) {
                if (a.startsWith("exit=")) {
                    try {
                        int code = Integer.parseInt(a.substring("exit=".length()));
                        System.out.println("Exiting VM with code " + code);
                        System.exit(code);
                    } catch (NumberFormatException ignore) {
                        System.err.println("Invalid exit code: " + a);
                    }
                }
            }
        }

        header("Java Platform Basics — end");
    }

    // 1) Minimal program and basic output
    static void helloWorld() {
        section("helloWorld");
        // Minimal program structure is shown by this class itself. The classic example:
        // public class Main { public static void main(String[] args) { System.out.println("Hello, World"); } }
        System.out.println("Hello, World! こんにちは世界! Hola, mundo! Привет, мир! 你好，世界！");
    }

    // 2) Command-line arguments
    static void commandLineArguments(String[] args) {
        section("commandLineArguments");
        System.out.println("Number of args: " + args.length);
        System.out.println("Args: " + Arrays.toString(args));
        // Access individual args safely:
        if (args.length > 0) {
            System.out.println("First arg: " + args[0]);
        }
    }

    // 3) Java runtime information (JVM/JRE/JDK details)
    static void printJavaRuntimeInfo() {
        section("printJavaRuntimeInfo");

        // Common system properties
        Properties p = System.getProperties();
        printProp(p, "java.version");
        printProp(p, "java.runtime.version");
        printProp(p, "java.vendor");
        printProp(p, "java.vm.name");
        printProp(p, "java.vm.version");
        printProp(p, "java.vm.vendor");
        printProp(p, "java.specification.version");
        printProp(p, "os.name");
        printProp(p, "os.arch");
        printProp(p, "os.version");
        printProp(p, "user.dir");
        printProp(p, "file.separator");
        printProp(p, "path.separator");
        printProp(p, "line.separator");
        printProp(p, "java.class.path");
        printProp(p, "java.library.path");
        printProp(p, "sun.java.command"); // how the app was launched (implementation-specific)

        // Runtime info (heap sizes)
        long mb = 1024L * 1024L;
        Runtime rt = Runtime.getRuntime();
        System.out.printf(Locale.ROOT, "Memory: total=%.1fMB, free=%.1fMB, max=%.1fMB%n",
                rt.totalMemory() / (double) mb,
                rt.freeMemory() / (double) mb,
                rt.maxMemory() / (double) mb);

        // Java 9+ adds Runtime.version(); accessed reflectively for compatibility
        try {
            Method m = Runtime.class.getMethod("version");
            Object v = m.invoke(Runtime.getRuntime());
            System.out.println("Runtime.version(): " + v);
        } catch (Throwable ignore) {
            // On Java 8, method doesn't exist.
        }
    }

    // 4) Standard Library tour (core APIs available across platforms)
    static void standardLibraryDemo() {
        section("standardLibraryDemo");
        // Time and Date (java.time)
        LocalDateTime now = LocalDateTime.now();
        System.out.println("LocalDateTime.now(): " + now);

        // Math utilities
        double pi = Math.PI;
        double sqrt2 = Math.sqrt(2);
        System.out.println("PI=" + pi + ", sqrt(2)=" + sqrt2);

        // Collections
        List<String> list = Arrays.asList("JDK", "JRE", "JVM");
        System.out.println("List: " + list);

        // Optional (avoid NullPointerException with explicit presence)
        Optional<String> maybe = Optional.of("Bytecode");
        System.out.println("Optional present? " + maybe.isPresent() + ", value: " + maybe.orElse("<none>"));

        // Files API (NIO.2)
        try {
            Path tmp = Files.createTempFile("_java_platform_demo_", ".txt");
            Files.write(tmp, "Hello from NIO.2\nLine 2".getBytes(StandardCharsets.UTF_8));
            List<String> lines = Files.readAllLines(tmp, StandardCharsets.UTF_8);
            System.out.println("Temp file: " + tmp);
            System.out.println("First line: " + (lines.isEmpty() ? "<empty>" : lines.get(0)));
            Files.deleteIfExists(tmp);
        } catch (IOException e) {
            System.err.println("File I/O error: " + e.getMessage());
        }
    }

    // 5) Classpath resource loading (works both in exploded classes and inside JARs)
    static void classpathResourceDemo() {
        section("classpathResourceDemo");
        // Place a resource file on the classpath (e.g., in same package folder) to load it.
        // Example expected path in source tree: src/_01_01_java_platform_basics/resource.txt
        try (InputStream in = _02_Examples.class.getResourceAsStream("resource.txt")) {
            if (in == null) {
                System.out.println("Resource not found: resource.txt (add it under the same package to test)");
            } else {
                String text = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                        .lines().reduce("", (a, b) -> a + (a.isEmpty() ? "" : "\n") + b);
                System.out.println("Loaded resource content:\n" + text);
            }
        } catch (IOException e) {
            System.err.println("Error reading resource: " + e.getMessage());
        }
        // The classpath is what java uses to locate classes/resources:
        // java -cp out:path/to/deps/* _01_01_java_platform_basics._02_Examples classpath
    }

    // 6) Modules and package metadata (Java 9+; handled reflectively for Java 8 compatibility)
    static void moduleAndPackageInfo() {
        section("moduleAndPackageInfo");
        Package pkg = _02_Examples.class.getPackage();
        if (pkg != null) {
            System.out.println("Package name: " + pkg.getName());
            System.out.println("Package Specification-Title: " + nullToEmpty(pkg.getSpecificationTitle()));
            System.out.println("Package Specification-Version: " + nullToEmpty(pkg.getSpecificationVersion()));
            System.out.println("Package Implementation-Title: " + nullToEmpty(pkg.getImplementationTitle()));
            System.out.println("Package Implementation-Version: " + nullToEmpty(pkg.getImplementationVersion()));
            System.out.println("Package Implementation-Vendor: " + nullToEmpty(pkg.getImplementationVendor()));
        }

        // Module info via reflection (Java 9+)
        try {
            Method getModule = Class.class.getMethod("getModule");
            Object module = getModule.invoke(_02_Examples.class);
            System.out.println("Module (reflective): " + module);
            // module.getName()
            Method getName = module.getClass().getMethod("getName");
            Object name = getName.invoke(module);
            System.out.println("Module name: " + name);
        } catch (Throwable t) {
            System.out.println("Modules not available (likely running on Java 8).");
        }
    }

    // 7) Environment variables and Console I/O
    static void environmentAndIO() {
        section("environmentAndIO");
        String javaHome = System.getenv("JAVA_HOME");
        String path = System.getenv("PATH");
        System.out.println("JAVA_HOME=" + (javaHome == null ? "<not set>" : javaHome));
        System.out.println("PATH length=" + (path == null ? 0 : path.length()));

        // Console may be null (e.g., in IDE). Use Scanner(System.in) if needed.
        Console console = System.console();
        System.out.println("Console available? " + (console != null));
        // Example (commented to avoid blocking):
        // if (console != null) {
        //     String name = console.readLine("Enter your name: ");
        //     console.printf("Hello, %s!%n", name);
        // }
    }

    // 8) Measuring time
    static void timeMeasurementDemo() {
        section("timeMeasurementDemo");
        long t0 = System.nanoTime();
        long sum = 0;
        for (int i = 0; i < 1_000_00; i++) sum += i;
        long dt = System.nanoTime() - t0;
        System.out.println("Sum=" + sum + ", elapsed ~" + TimeUnit.NANOSECONDS.toMicros(dt) + " µs");
        // System.nanoTime() is monotonic and preferred for measuring intervals.
    }

    // 9) Assertions (disabled by default; enable with -ea)
    static void assertionDemo() {
        section("assertionDemo");
        boolean enabled = false;
        assert enabled = true; // if assertions are enabled, this assignment runs
        System.out.println("Assertions enabled? " + enabled);
    }

    // 10) ClassLoader hierarchy (how the JVM locates and loads classes)
    static void classLoaderInfo() {
        section("classLoaderInfo");
        ClassLoader cl = _02_Examples.class.getClassLoader();
        int depth = 0;
        while (true) {
            System.out.println("ClassLoader level " + depth + ": " + (cl == null ? "bootstrap (null)" : cl));
            if (cl == null) break;
            cl = cl.getParent();
            depth++;
        }
        // Bootstrap loader is native and represented as null in Java.
    }

    // 11) Threading basics
    static void threadDemo() {
        section("threadDemo");
        Thread t = new Thread(() -> {
            System.out.println("Hello from thread: " + Thread.currentThread().getName());
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
            }
        }, "Worker-1");
        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("Thread isAlive? " + t.isAlive());
    }

    // 12) Exceptions (checked vs unchecked)
    static void exceptionsDemo() {
        section("exceptionsDemo");
        // Checked exception example (IOException)
        try {
            Files.readAllBytes(new File("definitely_missing.file").toPath());
        } catch (IOException e) {
            System.out.println("Caught checked exception: " + e.getClass().getSimpleName());
        }
        // Unchecked exception example (NumberFormatException)
        try {
            Integer.parseInt("NaN");
        } catch (NumberFormatException e) {
            System.out.println("Caught unchecked exception: " + e.getClass().getSimpleName());
        }
        // Try-with-resources auto-closes resources that implement AutoCloseable
        try (InputStream in = new java.io.ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8))) {
            System.out.println("Read bytes: " + in.read());
        } catch (IOException e) {
            System.err.println("I/O: " + e.getMessage());
        }
    }

    // 13) File/path separators across OSes
    static void pathSeparatorDemo() {
        section("pathSeparatorDemo");
        System.out.println("File.separator: " + File.separator + " (used between path segments)");
        System.out.println("File.pathSeparator: " + File.pathSeparator + " (used between multiple paths)");
        // Example: java -cp "lib1.jar" + File.pathSeparator + "lib2.jar" Main
    }

    // Utility helpers

    static void printProp(Properties p, String key) {
        System.out.println(key + "=" + nullToEmpty(p.getProperty(key)));
    }

    static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    static void header(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }

    static void section(String name) {
        System.out.println();
        System.out.println("-- " + name + " --");
    }

    // Nested minimal program example (not used; shows basic structure)
    static final class MinimalHelloWorld {
        public static void main(String[] args) {
            System.out.println("Minimal Hello from nested class.");
        }
    }
}