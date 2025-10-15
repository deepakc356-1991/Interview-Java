package _10_04_modules_jpms;

/*
Modules (JPMS) — Theory and Practical Notes
Author: Your Name
JDK: 9+ (content aligns with JDK 17+)
--------------------------------------------------------------------------------

0) What is JPMS?
- JPMS (Java Platform Module System), introduced in Java 9, adds modules: named, self-describing units of code and data (classes + resources) with explicit dependencies and strong encapsulation.
- Goals:
  - Reliable configuration: detect missing/conflicting dependencies at compile/run time.
  - Strong encapsulation: only exported packages are accessible; internal APIs are hidden by default.
  - Scalable platform: JDK split into modules; create small custom runtimes with jlink.

1) Core Terms
- Module: Named unit described by module-info.java.
- Named module: Has a module-info descriptor; strongly encapsulated.
- Automatic module: A plain JAR on the module path (no module-info) becomes a module:
  - Gets an automatic name (from Automatic-Module-Name manifest or derived from JAR file name).
  - Reads all other modules on the module path.
  - Exports all its packages (weak encapsulation).
- Unnamed module: Code on the class path belongs to a single implicit unnamed module:
  - Can read all other modules and the class path.
  - Named modules DO NOT read the unnamed module by default.

2) Module Descriptor (module-info.java)
- Basic form:
  module com.example.app {
    requires com.example.lib;              // declare dependency
    requires transitive java.sql;          // re-exported dependency for downstream
    requires static com.example.optional;  // compile-time only; not needed at runtime
    exports com.example.api;               // makes public types in package accessible
    exports com.example.impl to com.example.tests;  // qualified export to specific modules
    opens com.example.model to com.fasterxml.jackson.databind; // deep reflection allowed
    uses com.example.spi.Formatter;        // consumes a service
    provides com.example.spi.Formatter with com.example.impl.JsonFormatter; // service provider
  }
- open module com.example.app { ... }
  - Opens all packages for deep reflection by default (but still not exported by default).
- exports vs opens:
  - exports -> compile- and run-time access to public types (no deep reflection).
  - opens -> deep reflection permitted to the target(s) (setAccessible) but not exported for compile-time use.
  - You can both export and open a package for both compile-time use and deep reflection.

3) Readability and Accessibility
- Readability: M reads N if module M declares requires N (or is implied via requires transitive).
- Accessibility: A public type in package P of module N is accessible to M only if:
  - M reads N AND
  - P is exported by N (unqualified export or qualified export to M).
- Deep reflection (setAccessible) across modules requires the package to be opened (opens).
- Cycles: Modules cannot have cyclic requires dependencies.

4) Services (ServiceLoader)
- SPI pattern in modules:
  - Consumer module: uses com.example.spi.Service
  - Provider module: provides com.example.spi.Service with com.example.provider.Impl
- Providers are NOT pulled in automatically by just uses; ensure provider modules are resolved at runtime by:
  - Declaring requires provider.module in the consumer, or
  - Launching with --add-modules provider.module (or ALL-MODULE-PATH), or
  - Using jlink with service binding.
- Example usage:
  ServiceLoader<MyService> loader = ServiceLoader.load(MyService.class);
  for (MyService s : loader) { s.run(); }

5) Migration Path (Typical)
- Step 0: Keep everything on the class path (unnamed module) — works as before.
- Step 1: Move third-party JARs to module path:
  - Plain JARs become automatic modules (name/exports automatic).
  - Set Automatic-Module-Name in your JAR manifests to stabilize names for consumers.
- Step 2: Modularize your code:
  - Add module-info.java to each top-level component.
  - Fix split packages (not allowed on module path).
  - Replace reflection hacks with opens/--add-opens when needed.
- Step 3: Gradually replace automatic modules with named ones.
- Important rule: Named modules cannot read the unnamed module. Plan dependencies accordingly.

6) Split Packages
- On the module path, the same package cannot be present in more than one module in the same layer (split packages are prohibited).
- Refactor to unique package ownership per module (e.g., com.example.lib.internal vs com.example.lib.api).

7) Module Path vs Class Path
- Module path (-p or --module-path): for modular JARs, automatic JARs, jmods; participates in module resolution.
- Class path (-cp or -classpath): legacy; all code is in the unnamed module.
- A named module cannot see the class path (unnamed module) unless you break encapsulation with --add-reads or refactor.

8) Compiling, Packaging, Running (CLI)
- Single module:
  javac -d out --module-path mods src/com.example.app/module-info.java src/com.example.app/**.java
  jar --create --file target/com.example.app.jar -C out .
  java --module-path target:mods --module com.example.app/com.example.app.Main
  or
  java -p target:mods -m com.example.app
- Multi-module source tree (recommended layout):
  src/
    com.example.app/
      module-info.java
      com/example/app/Main.java
    com.example.lib/
      module-info.java
      com/example/lib/Util.java
  Compile:
    javac --module-source-path src -d out $(find src -name "*.java")
  Package each module from out/com.example.* into a modular JAR with jar.
- Tools:
  - jdeps: dependency analysis; helps create module-info.
    jdeps -s target/com.example.app.jar
  - jlink: create custom runtime image with only required modules.
    jlink --module-path $JAVA_HOME/jmods:target --add-modules com.example.app --output image
  - jmod: package JDK modules; rarely used by app developers.

9) Naming Modules
- Use reverse-DNS style: com.example.app
- Do not use "java.*" or "jdk.*" (reserved).
- For automatic modules:
  - Prefer to set Automatic-Module-Name: com.fasterxml.jackson.databind (in MANIFEST.MF).
  - Derived names replace non-identifier chars with dots and strip versions; may be unstable.
- Keep module name aligned with top-level package naming to avoid confusion.

10) Interacting with Reflection and Frameworks
- Many frameworks (Jackson, JAX-B, Hibernate, Spring, JUnit) use reflection on non-public members:
  - Export alone is not enough for deep reflection; use opens:
    opens com.example.model to com.fasterxml.jackson.databind;
  - Or open the entire module:
    open module com.example.app { ... }
  - As a last resort at launch:
    --add-opens com.example.app/com.example.model=com.fasterxml.jackson.databind
    --add-opens com.example.app/com.example.model=ALL-UNNAMED (for classpath clients)
- add-exports if you must expose non-exported packages:
  --add-exports com.example.lib/internal.pkg=com.example.app
- add-reads to add readability dynamically:
  --add-reads com.example.app=com.example.lib

11) Resolution, Roots, and Flags
- Root modules: the modules explicitly specified to start resolution:
  - The initial module (via -m), plus those named in --add-modules, plus default roots (often java.se).
  - ALL-DEFAULT: default set of root modules.
  - ALL-SYSTEM: all system (JDK) modules.
  - ALL-MODULE-PATH: all modules found on the module path.
- Limit and diagnose:
  --limit-modules java.base,java.sql
  Useful for analyzing minimal sets and detecting accidental deps.

12) Versioning
- module-info.java does not express version constraints.
- JAR/MANIFEST can carry versions; the module system does not resolve by version.
- Version checks are typically enforced by build tools (Maven/Gradle) or by jlink plugin logic.

13) JDK Modules
- The JDK is modularized; minimal module is java.base (always present).
- Examples: java.logging, java.xml, java.sql, java.desktop, java.net.http.
- Aggregate: java.se (includes the standard SE API set).

14) Best Practices
- Keep module boundaries aligned with API boundaries; keep internals unexported.
- Avoid cyclic dependencies; use clearer layering.
- Avoid split packages; establish clear ownership.
- Keep only API packages exported; keep implementation packages internal.
- For public APIs that wrap dependencies, use requires transitive appropriately.
- For optional dependencies (compile-time only), use requires static.
- Minimize --add-opens/--add-exports in production; prefer proper opens in module descriptors.
- Provide Automatic-Module-Name in non-modular JARs intended for module-path consumers.

15) Testing in Modular World
- Test frameworks (JUnit/Jupiter) need reflection into test classes:
  - Option A (test module open): open module com.example.app.test { requires org.junit.jupiter.api; }
  - Option B (open test packages): opens com.example.app to org.junit.platform.commons;
  - Or pass --add-opens during test runs (Maven Surefire/Gradle test config).
- Keep tests as separate modules or use class path with ALL-UNNAMED if simpler.

16) Resources in Modules
- Resources are packaged inside modules as usual.
- Loading from the same module:
  InputStream in = MyClass.class.getResourceAsStream("data.txt");
- Loading across modules:
  - Prefer exposing an API to obtain resources.
  - Or use Module API:
    Module m = MyClass.class.getModule();
    try (InputStream in = m.getResourceAsStream("path/inside/module.txt")) { ... }
- Exports/opens control types and reflection, not resource visibility; but resource access needs a handle to the module.

17) Dynamic Modules and Layers (Advanced)
- You can load modules dynamically using ModuleLayer and Configuration to build plugin systems.
- Layers define sets of modules with class loaders and readability graphs.
- Class loaders and layers interplay: multiple modules may share a class loader; one module may not equal one loader.

18) Interop with Legacy Code
- Named modules cannot directly access the class path (unnamed module); migrate carefully.
- Automatic modules can help bridge, but plan to replace them with proper named modules.
- --illegal-access (deprecated/removed behavior):
  - JDK 9-15 default permitted illegal reflective access with warnings.
  - JDK 16+ disabled by default; use --add-opens to opt in per package.

19) Common Errors and Fixes
- NoClassDefFoundError when modularized: missing requires or module not resolved; add requires or --add-modules.
- java.lang.IllegalAccessError: Accessing non-exported package; export/opens appropriately or add-exports.
- java.lang.LayerInstantiationException: Split package across modules; refactor.
- "module not found": Not on module path; verify -p points to modular JARs/jmods.
- ServiceLoader finds no providers: provider module not resolved; add requires or --add-modules (or jlink service binding).

20) Minimal End-to-End Example (conceptual)
- Module com.example.spi (API):
  module com.example.spi { exports com.example.spi; }
  package com.example.spi;
  public interface Formatter { String format(String s); }
- Module com.example.provider.json (provider):
  module com.example.provider.json {
    requires com.example.spi;
    provides com.example.spi.Formatter with com.example.provider.JsonFormatter;
    exports com.example.provider; // optional (not needed for service loading)
  }
  public class JsonFormatter implements Formatter { public String format(String s){ return "{\"v\":\""+s+"\"}"; } }
- Module com.example.app (consumer):
  module com.example.app {
    requires com.example.spi;
    uses com.example.spi.Formatter;
  }
  public class Main {
    public static void main(String[] args) {
      ServiceLoader<Formatter> loader = ServiceLoader.load(Formatter.class);
      loader.findFirst().ifPresent(f -> System.out.println(f.format("hi")));
    }
  }
- Run:
  java -p mods -m com.example.app/com.example.app.Main --add-modules com.example.provider.json
  (or declare requires com.example.provider.json in com.example.app)

21) Build Tool Notes (quick)
- Maven:
  - Set maven-compiler-plugin release to your target JDK.
  - module-info.java belongs in src/main/java.
  - For multi-module builds, ensure reactor order and module-path support (maven-compiler >= 3.8).
- Gradle:
  - Java toolchains and --patch-module are supported; consider plugins that configure module path.
  - Test tasks often need --add-opens for JUnit.

22) Packaging Types
- Modular JAR: Contains module-info.class at the root.
- Automatic JAR: No module-info; becomes an automatic module on the module path.
- jmod: JDK internal packaging for JDK modules; not for general application distribution.
- Custom runtime image: Created with jlink; can be shipped as a compact distribution.

23) Security and Performance
- Strong encapsulation reduces accidental/malicious use of internals.
- Smaller runtime images (jlink) improve startup and footprint.
- Module graph can help reduce reflective scanning overhead (with services) and improve clarity.

--------------------------------------------------------------------------------
Below: Minimal placeholder code so this file compiles. The comments above contain the theory.
*/

import java.util.ServiceLoader;

public class _01_Theory {

    // Simple SPI just for illustration within a single file (normally across modules)
    public interface GreetingService {
        String greet(String name);
    }

    public static class EnglishGreetingService implements GreetingService {
        @Override
        public String greet(String name) {
            return "Hello, " + name;
        }
    }

    // Illustrative consumer showing ServiceLoader usage.
    // In a real modular setup:
    // - The consumer module declares: uses _10_04_modules_jpms._01_Theory.GreetingService;
    // - The provider module declares: provides ...GreetingService with ...EnglishGreetingService;
    public static void serviceLoaderDemo() {
        // This will only find providers if configured as proper modules/providers on the module path.
        ServiceLoader<GreetingService> loader = ServiceLoader.load(GreetingService.class);
        for (GreetingService s : loader) {
            System.out.println(s.greet("JPMS"));
        }
    }

    public static void main(String[] args) {
        // This file is primarily a theory sheet; the demo does nothing unless providers are present.
        System.out.println("JPMS Theory loaded. See comments for details.");
    }
}