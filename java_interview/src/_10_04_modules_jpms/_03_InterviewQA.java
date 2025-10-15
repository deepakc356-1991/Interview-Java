package _10_04_modules_jpms;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Run this class to print JPMS (Java Platform Module System) interview Q&A
 * grouped by Basic, Intermediate, and Advanced.
 */
public class _03_InterviewQA {

    public static void main(String[] args) {
        printHeader("Java Modules (JPMS) - Interview Q&A");
        printLevel("BASIC");
        printLevel("INTERMEDIATE");
        printLevel("ADVANCED");
    }

    // ---------- Data model ----------
    static final class QA {
        final String level;
        final String question;
        final String answer;

        QA(String level, String question, String answer) {
            this.level = level;
            this.question = question;
            this.answer = answer;
        }
    }

    // ---------- Q&A store ----------
    private static final List<QA> QAS = new ArrayList<>();

    private static void add(String level, String q, String a) {
        QAS.add(new QA(level, q, a));
    }

    private static String lines(String... ls) {
        return String.join(System.lineSeparator(), ls);
    }

    private static void printHeader(String title) {
        System.out.println(title);
        System.out.println("=".repeat(title.length()));
        System.out.println();
    }

    private static void printLevel(String level) {
        List<QA> list = QAS.stream()
                .filter(x -> x.level.equals(level))
                .sorted(Comparator.comparing(x -> x.question))
                .collect(Collectors.toList());

        System.out.println(level);
        System.out.println("-".repeat(level.length()));
        for (int i = 0; i < list.size(); i++) {
            QA qa = list.get(i);
            System.out.println((i + 1) + ") Q: " + qa.question);
            System.out.println("   A: " + qa.answer.replace("\n", "\n      "));
            System.out.println();
        }
        System.out.println();
    }

    // ---------- Content ----------
    static {
        // BASIC
        add("BASIC",
            "What is JPMS?",
            lines(
                "The Java Platform Module System (introduced in Java 9) adds first-class modules to Java.",
                "A module is a named, self-describing collection of code (packages) and resources with a descriptor (module-info.java).",
                "It enables strong encapsulation, reliable configuration, and smaller runtime images."
            )
        );

        add("BASIC",
            "Why modules instead of the classpath?",
            lines(
                "- Classpath has no dependency metadata, silent conflicts, and no true encapsulation.",
                "- Modules declare dependencies and exported APIs, detect conflicts at compile/run time, and hide internal packages."
            )
        );

        add("BASIC",
            "What is module-info.java?",
            lines(
                "The module descriptor declaring a module's name and directives (requires, exports, opens, uses, provides).",
                "Example:",
                "  module com.example.app {",
                "      requires com.example.lib;",
                "      requires transitive java.sql;",
                "      exports com.example.api;",
                "      opens com.example.internal to com.fasterxml.jackson.databind;",
                "      uses com.example.spi.Greeter;",
                "      provides com.example.spi.Greeter with com.example.impl.FriendlyGreeter;",
                "  }"
            )
        );

        add("BASIC",
            "What is java.base?",
            lines(
                "The fundamental JDK module (Object, String, collections, etc.).",
                "Every module implicitly requires java.base; you do not need to declare it."
            )
        );

        add("BASIC",
            "What is the difference between exports and opens?",
            lines(
                "- exports: makes public types in a package accessible at compile time and run time to other modules.",
                "- opens: allows deep reflective access at run time only; not visible at compile time.",
                "- open module ... { }: opens all packages of the module for reflection (still not exported)."
            )
        );

        add("BASIC",
            "What are qualified exports/opens?",
            lines(
                "Selective exposure to specific modules:",
                "  exports com.example.internal to com.example.tooling;",
                "  opens com.example.model to com.fasterxml.jackson.databind;"
            )
        );

        add("BASIC",
            "What does requires transitive mean?",
            lines(
                "If A requires transitive B, then any module requiring A implicitly reads B.",
                "Use it for API dependencies that your consumers need.",
                "Example: module A exports types that reference types from B."
            )
        );

        add("BASIC",
            "What does requires static mean?",
            lines(
                "Optional dependency needed only at compile time, not at run time.",
                "Common for annotation processors or optional APIs used behind reflection."
            )
        );

        add("BASIC",
            "How do I compile and run a modular project?",
            lines(
                "Assume modules: com.example.app (with Main) and com.example.lib",
                "Compile:",
                "  javac -d out/com.example.lib src/com.example.lib/module-info.java src/com.example.lib/com/example/lib/**/*.java",
                "  javac --module-path out -d out/com.example.app src/com.example.app/module-info.java src/com.example.app/com/example/app/**/*.java",
                "Run:",
                "  java --module-path out --module com.example.app/com.example.app.Main"
            )
        );

        add("BASIC",
            "What is an automatic module?",
            lines(
                "A classpath JAR placed on the module path becomes a named module automatically.",
                "Its name is derived from the JAR name (or Manifest Automatic-Module-Name).",
                "Use to bridge non-modular dependencies while migrating to JPMS."
            )
        );

        add("BASIC",
            "What is the unnamed module?",
            lines(
                "All code on the classpath belongs to the unnamed module.",
                "It reads all other modules and exports all its packages to all modules.",
                "Named modules do NOT read the unnamed module unless you add: --add-reads my.module=ALL-UNNAMED"
            )
        );

        add("BASIC",
            "What are split packages and why are they bad?",
            lines(
                "A split package exists in more than one module (same package name). JPMS forbids it.",
                "Fix by merging modules, renaming packages, or relocating one side."
            )
        );

        add("BASIC",
            "How to list JDK modules and see my runtime?",
            lines(
                "List installed JDK modules: java --list-modules",
                "Print module path resolution at launch: java --show-module-resolution --module-path ... --module ...",
                "Inspect dependencies: jdeps --module-path ... --module my.module"
            )
        );

        // INTERMEDIATE
        add("INTERMEDIATE",
            "How does module readability and accessibility work?",
            lines(
                "- Readability: A reads B if it requires B (directly or transitively).",
                "- Accessibility: A type is accessible only if its package is exported by B.",
                "Both must hold to use a type."
            )
        );

        add("INTERMEDIATE",
            "Can modules have cyclic dependencies?",
            lines(
                "No. Readability cycles are not allowed. The resolver fails if cycles exist."
            )
        );

        add("INTERMEDIATE",
            "How do services (uses/provides) work with ServiceLoader?",
            lines(
                "Define an SPI and providers via module-info:",
                "  // API module",
                "  module com.example.spi { exports com.example.spi; }",
                "  package com.example.spi; public interface Greeter { String greet(); }",
                "",
                "  // Provider module",
                "  module com.example.provider {",
                "      requires com.example.spi;",
                "      provides com.example.spi.Greeter with com.example.provider.FriendlyGreeter;",
                "  }",
                "",
                "  // Consumer module",
                "  module com.example.app {",
                "      requires com.example.spi;",
                "      uses com.example.spi.Greeter;",
                "  }",
                "  // Consumer code:",
                "  ServiceLoader<Greeter> loader = ServiceLoader.load(Greeter.class);",
                "  for (Greeter g : loader) System.out.println(g.greet());"
            )
        );

        add("INTERMEDIATE",
            "How do I package a modular JAR?",
            lines(
                "Place module-info.class at the root of the JAR.",
                "Example:",
                "  jar --create --file libs/com.example.app.jar \\",
                "      --main-class com.example.app.Main -C out/com.example.app ."
            )
        );

        add("INTERMEDIATE",
            "What is an open module and when to use it?",
            lines(
                "open module X { ... } opens all packages for deep reflection.",
                "Use sparingly for frameworks doing pervasive reflection (e.g., JAXB, JSON binding) when fine-grained opens is impractical."
            )
        );

        add("INTERMEDIATE",
            "How do I allow reflective access without opening everything?",
            lines(
                "- Prefer package-level opens: opens com.example.model to some.framework;",
                "- Or use launch flags for tooling/tests:",
                "  --add-opens my.module/com.example.model=some.framework",
                "Note: opens is about reflection; exports is about API access."
            )
        );

        add("INTERMEDIATE",
            "What are common module errors and fixes?",
            lines(
                "- module not found: put dependency on module path, correct name.",
                "- does not read module X: add requires X or --add-reads.",
                "- package ... is not visible: export the package (exports), not opens.",
                "- package present in another module (split package): refactor.",
                "- duplicate module on path: remove one copy."
            )
        );

        add("INTERMEDIATE",
            "What is Automatic-Module-Name?",
            lines(
                "Manifest entry to pin a stable module name for a non-modular JAR:",
                "  Automatic-Module-Name: com.example.lib",
                "Helps avoid derived names and eases migration."
            )
        );

        add("INTERMEDIATE",
            "How do module versions work?",
            lines(
                "JPMS does not do versioned resolution. You can embed a version in the JAR (jar --module-version),",
                "but resolution ignores versions. Build tools handle version selection; at run time only one version is present."
            )
        );

        add("INTERMEDIATE",
            "How to migrate a large project to modules?",
            lines(
                "- Step 1: Clean classpath, fix split packages, add Automatic-Module-Name to libraries.",
                "- Step 2: Move app to module path; use automatic modules for third-party JARs.",
                "- Step 3: Introduce module-info gradually; use --add-exports/--add-opens temporarily.",
                "- Step 4: Replace automatic modules with real modules over time."
            )
        );

        add("INTERMEDIATE",
            "How do resources work in modules?",
            lines(
                "Resources are packaged in modules and can be loaded with Class.getResourceAsStream or Module.getResourceAsStream.",
                "Exports/opens control types and reflection, not basic resource loading.",
                "You still need a handle to a class or module to locate the resource."
            )
        );

        add("INTERMEDIATE",
            "What are aggregator modules (e.g., java.se)?",
            lines(
                "Modules that require transitive a set of modules. Example: java.se requires transitive common JDK modules.",
                "Using them pulls in many modules; prefer fine-grained requires when possible."
            )
        );

        add("INTERMEDIATE",
            "How to run a specific main class in a module?",
            lines(
                "Either define Main-Class in the modular JAR or specify:",
                "  java --module-path libs --module com.example.app/com.example.app.Main"
            )
        );

        add("INTERMEDIATE",
            "How to inspect a module?",
            lines(
                "- jdeps -s --module-path ... --module my.module",
                "- jar --describe-module --file my.jar",
                "- jmod describe $JAVA_HOME/jmods/java.base.jmod"
            )
        );

        // ADVANCED
        add("ADVANCED",
            "How to create a custom runtime image with jlink?",
            lines(
                "1) Identify required modules (your app + JDK modules).",
                "2) Build modular JAR(s).",
                "3) jlink --module-path libs:" + System.getProperty("java.home") + "/jmods \\",
                "       --add-modules com.example.app \\",
                "       --output image",
                "4) Run: image/bin/java -m com.example.app"
            )
        );

        add("ADVANCED",
            "What is ModuleLayer and when to use it?",
            lines(
                "ModuleLayer allows loading modules dynamically into custom layers/class loaders.",
                "Use for plugin systems or isolating multiple versions.",
                "Sketch:",
                "  ModuleFinder finder = ModuleFinder.of(Path.of(\"plugins/my.plugin.jar\"));",
                "  Configuration parent = ModuleLayer.boot().configuration();",
                "  Configuration cf = parent.resolve(finder, ModuleFinder.of(), List.of(\"my.plugin\"));",
                "  ClassLoader scl = ClassLoader.getSystemClassLoader();",
                "  ModuleLayer layer = ModuleLayer.boot().defineModulesWithOneLoader(cf, scl);",
                "  Class<?> c = layer.findLoader(\"my.plugin\").loadClass(\"my.plugin.Main\");"
            )
        );

        add("ADVANCED",
            "Can I load multiple versions of the same module?",
            lines(
                "Not in the same layer. With custom layers you can isolate versions, but they cannot read each other as a single module.",
                "JPMS is not a general multi-version module system."
            )
        );

        add("ADVANCED",
            "What do --add-reads, --add-exports, --add-opens, --patch-module do?",
            lines(
                "- --add-reads A=B: make module A read B (B can be ALL-UNNAMED).",
                "- --add-exports A/p=B: export package p from A to B.",
                "- --add-opens A/p=B: open package p from A to B for deep reflection.",
                "- --patch-module A=path: add classes/resources to module A (useful for tests or hotfixes)."
            )
        );

        add("ADVANCED",
            "How to handle frameworks that need deep reflection (e.g., Hibernate, Jackson)?",
            lines(
                "- Prefer per-package opens to the framework module(s).",
                "- If needed only in dev/test, add JVM flags (--add-opens) in those environments.",
                "- As a last resort, use open module for the app."
            )
        );

        add("ADVANCED",
            "What is the impact of modules on performance?",
            lines(
                "Faster startup due to smaller graphs and less classpath scanning; potential gains from jlink images.",
                "There can be minor costs for additional checks; overall impact is usually positive."
            )
        );

        add("ADVANCED",
            "How to diagnose illegal reflective access warnings?",
            lines(
                "They indicate code using deep reflection into non-open packages.",
                "Fix by: (a) using supported APIs, (b) adding opens in module-info, or (c) --add-opens at launch.",
                "Future JDKs may make such access errors by default."
            )
        );

        add("ADVANCED",
            "How to test code in non-exported packages?",
            lines(
                "- Use --add-exports my.module/com.example.internal=ALL-UNNAMED (for tests on classpath).",
                "- Or --patch-module my.module=target/test-classes to inject test classes into the module.",
                "- Tools (e.g., Maven Surefire) can be configured to pass these flags."
            )
        );

        add("ADVANCED",
            "How to minimize image size with jlink?",
            lines(
                "- Add only required modules.",
                "- Use jlink plugins: --strip-debug, --no-header-files, --no-man-pages, --compress=2.",
                "- Avoid aggregator modules like java.se if not needed."
            )
        );

        add("ADVANCED",
            "How do exports/opens interact with reflection and resources?",
            lines(
                "- exports controls compile/runtime access to public types.",
                "- opens controls deep reflection (setAccessible).",
                "- Resources are not governed by exports/opens, but you need a class/module handle to locate them."
            )
        );

        add("ADVANCED",
            "How to limit the set of observable JDK modules?",
            lines(
                "Use --limit-modules to restrict which system modules are visible/resolvable.",
                "Example: java --limit-modules java.base,java.sql -m com.example.app"
            )
        );

        add("ADVANCED",
            "What are friend modules (qualified exports) good for?",
            lines(
                "They allow exposing internals to specific tooling/tests without making them public API.",
                "Example: exports com.example.internal to com.example.tools;"
            )
        );

        add("ADVANCED",
            "What happens if I put the same package in main and test modules?",
            lines(
                "That's a split package. Prefer --patch-module for tests or refactor packages."
            )
        );

        add("ADVANCED",
            "How to choose module names?",
            lines(
                "Reverse-DNS convention (e.g., com.example.lib).",
                "For third-party JARs, provide Automatic-Module-Name to avoid derived names."
            )
        );

        add("ADVANCED",
            "OSGi vs JPMS differences?",
            lines(
                "- JPMS is language/runtime level with simple dependency model; no versioned wiring or dynamic life-cycle.",
                "- OSGi supports dynamic modules, services, and versions; heavier.",
                "You can run JPMS inside OSGi or vice versa with care, but they solve different problems."
            )
        );

        add("ADVANCED",
            "How to debug module resolution issues?",
            lines(
                "- Use --show-module-resolution at launch.",
                "- Use jdeps to find missing/implicit dependencies.",
                "- Verify module names (jar --describe-module) and path layout."
            )
        );

        add("ADVANCED",
            "Does JPMS change serialization or RMI needs?",
            lines(
                "Reflective frameworks and serialization libraries may need opens to access private fields/constructors.",
                "Add opens for relevant packages to such frameworks."
            )
        );

        add("ADVANCED",
            "Can I export a package but still block reflection?",
            lines(
                "Yes. exports allows compile/runtime access to types, but not deep reflection.",
                "To allow reflection, also open the package (opens) to the desired modules."
            )
        );

        add("ADVANCED",
            "How are automatic module names derived?",
            lines(
                "If Manifest Automatic-Module-Name is absent, the name is derived from the JAR file name:",
                "- Strip version-like suffixes, replace non-alphanumeric with dots, collapse repeats.",
                "Always prefer Automatic-Module-Name for stability."
            )
        );

        add("ADVANCED",
            "How to model optional providers at runtime?",
            lines(
                "Use provides for available providers; consumers iterate ServiceLoader and handle empty results.",
                "For optional compile-time APIs, combine requires static with reflective loading."
            )
        );

        add("ADVANCED",
            "Best practices for modular design?",
            lines(
                "- Keep module boundaries aligned with public APIs.",
                "- Avoid split packages; keep clear internal packages (not exported).",
                "- Use requires transitive only when consumers need the dependency.",
                "- Prefer qualified exports/opens for narrow exposure.",
                "- Keep module graph acyclic and minimal."
            )
        );
    }
}