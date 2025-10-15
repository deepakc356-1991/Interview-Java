package _10_04_modules_jpms;

/*
This single source file explains JPMS (Java Platform Module System) via concise, copy-paste-ready examples.
Each example shows realistic module-info.java files and minimal classes. Use it as a reference when creating
multi-module projects.

Index
- Example 1: Hello, Modules (exports/requires)
- Example 2: requires transitive
- Example 3: Qualified exports (exports ... to)
- Example 4: opens vs exports (reflection)
- Example 5: open module (open ...)
- Example 6: Services (uses/provides + ServiceLoader)
- Example 7: Automatic modules and Automatic-Module-Name
- Example 8: Unnamed module (classpath) and --add-reads
- Example 9: Resources in modules
- Example 10: Dynamic modules (ModuleLayer)
- Example 11: Command-line overrides (--add-exports/--add-opens/--add-reads)

Note: module-info.java files live at the root of a module (not inside a package).
*/

public class _02_Examples {

    public static void main(String[] args) {
        System.out.println("JPMS Examples. Open this file and copy the snippets you need.");
        System.out.println();
        basicModuleInfo();
        dynamicLayerSkeleton();
    }

    private static void basicModuleInfo() {
        System.out.println("Example: Inspecting modules at runtime");
        Module m = _02_Examples.class.getModule(); // likely unnamed unless you compile this file in a named module
        System.out.println("This class is in module: " + (m.isNamed() ? m.getName() : "UNNAMED"));
        System.out.println("java.base module is named: " + Object.class.getModule().getName());
        System.out.println("Boot layer contains modules: " + ModuleLayer.boot().modules().size());
        System.out.println();
    }

    private static void dynamicLayerSkeleton() {
        System.out.println("Example: Dynamic module layers (skeleton). See comments in Example 10 for full code.");
        System.out.println();
    }

    /*
    ============================================================================================
    Example 1: Hello, Modules (exports/requires)
    Two modules: com.example.greetings and com.example.app

    Directory layout (one dir per module):
    - com.example.greetings/
        module-info.java
        com/example/greetings/Greeter.java
    - com.example.app/
        module-info.java
        com/example/app/Main.java

    // File: com.example.greetings/module-info.java
    module com.example.greetings {
        exports com.example.greetings; // makes the package accessible to other modules
    }

    // File: com.example.greetings/com/example/greetings/Greeter.java
    package com.example.greetings;
    public class Greeter {
        public String greet(String name) { return "Hello, " + name + "!"; }
    }

    // File: com.example.app/module-info.java
    module com.example.app {
        requires com.example.greetings; // declares a read edge to greetings
    }

    // File: com.example.app/com/example/app/Main.java
    package com.example.app;
    import com.example.greetings.Greeter;

    public class Main {
        public static void main(String[] args) {
            System.out.println(new Greeter().greet(args.length > 0 ? args[0] : "World"));
        }
    }

    Compile and run:
    javac -d out/greetings com.example.greetings/module-info.java com.example.greetings/com/example/greetings/Greeter.java
    javac --module-path out/greetings -d out/app com.example.app/module-info.java com.example.app/com/example/app/Main.java
    java --module-path out/app:out/greetings --module com.example.app/com.example.app.Main
    ============================================================================================
    */

    /*
    ============================================================================================
    Example 2: requires transitive
    Three modules: api -> impl -> app
    app only requires impl, but sees API types thanks to requires transitive in impl.

    // File: com.example.api/module-info.java
    module com.example.api {
        exports com.example.api;
    }

    // File: com.example.api/com/example/api/Api.java
    package com.example.api;
    public interface Api {
        String ping();
    }

    // File: com.example.impl/module-info.java
    module com.example.impl {
        requires transitive com.example.api; // consumers of impl can also see com.example.api
        exports com.example.impl;
    }

    // File: com.example.impl/com/example/impl/ApiImpl.java
    package com.example.impl;
    import com.example.api.Api;
    public class ApiImpl implements Api {
        public String ping() { return "pong"; }
    }

    // File: com.example.app/module-info.java
    module com.example.app {
        requires com.example.impl; // no explicit requires com.example.api needed
    }

    // File: com.example.app/com/example/app/Main.java
    package com.example.app;
    import com.example.api.Api;
    import com.example.impl.ApiImpl;
    public class Main {
        public static void main(String[] args) {
            Api api = new ApiImpl();
            System.out.println(api.ping());
        }
    }
    ============================================================================================
    */

    /*
    ============================================================================================
    Example 3: Qualified exports (exports ... to)
    Only specific modules can access an exported package.

    // File: com.example.secret/module-info.java
    module com.example.secret {
        exports com.example.secret.internal to com.example.friend; // only friend can access
    }

    // File: com.example.secret/com/example/secret/internal/SecretApi.java
    package com.example.secret.internal;
    public class SecretApi {
        public static String token() { return "TOP-SECRET"; }
    }

    // File: com.example.friend/module-info.java
    module com.example.friend {
        requires com.example.secret;
    }

    // File: com.example.friend/com/example/friend/Client.java
    package com.example.friend;
    import com.example.secret.internal.SecretApi;
    public class Client {
        public static void main(String[] args) {
            System.out.println(SecretApi.token()); // OK
        }
    }

    // Another module trying to read SecretApi will fail at compile time:
    // error: package com.example.secret.internal is not visible
    ============================================================================================
    */

    /*
    ============================================================================================
    Example 4: opens vs exports (reflection)
    - exports: allows compile-time and runtime access (public types) by other modules.
    - opens: allows deep reflection (setAccessible) at runtime on non-public members, but not compile-time access.

    // File: com.example.model/module-info.java
    module com.example.model {
        exports com.example.model; // normal API access (no deep reflection)
        opens com.example.model;   // deep reflection allowed
    }

    // File: com.example.model/com/example/model/Person.java
    package com.example.model;
    public class Person {
        private String name = "Alice";
        private int age = 42;
    }

    // File: com.example.reflector/module-info.java
    module com.example.reflector {
        requires com.example.model;
    }

    // File: com.example.reflector/com/example/reflector/ReflectMain.java
    package com.example.reflector;
    import java.lang.reflect.Field;
    public class ReflectMain {
        public static void main(String[] args) throws Exception {
            Class<?> cls = Class.forName("com.example.model.Person");
            Object p = cls.getDeclaredConstructor().newInstance();
            Field f = cls.getDeclaredField("name");
            f.setAccessible(true); // works only if com.example.model opens com.example.model to this module
            System.out.println("name=" + f.get(p));
        }
    }

    If opens is missing, you'll get: java.lang.reflect.InaccessibleObjectException
    You can override at runtime (not recommended for production):
    java --add-opens com.example.model/com.example.model=com.example.reflector ...
    ============================================================================================
    */

    /*
    ============================================================================================
    Example 5: open module
    open module makes all packages open for reflection by default (no compile-time access).

    // File: com.example.model/module-info.java
    open module com.example.model {
        exports com.example.model; // still needed for normal API visibility
    }

    Use when frameworks (e.g., JSON mappers) need to reflectively access many packages.
    ============================================================================================
    */

    /*
    ============================================================================================
    Example 6: Services (uses/provides + ServiceLoader)
    Define an SPI, an implementation module, and a consumer.

    // File: com.example.spi/module-info.java
    module com.example.spi {
        exports com.example.spi;
    }

    // File: com.example.spi/com/example/spi/Formatter.java
    package com.example.spi;
    public interface Formatter { String format(String s); }

    // File: com.example.providers/module-info.java
    module com.example.providers {
        requires com.example.spi;
        provides com.example.spi.Formatter with com.example.providers.UppercaseFormatter,
                                               com.example.providers.ReverseFormatter;
    }

    // File: com.example.providers/com/example/providers/UppercaseFormatter.java
    package com.example.providers;
    import com.example.spi.Formatter;
    public class UppercaseFormatter implements Formatter {
        public String format(String s) { return s.toUpperCase(); }
    }

    // File: com.example.providers/com/example/providers/ReverseFormatter.java
    package com.example.providers;
    import com.example.spi.Formatter;
    public class ReverseFormatter implements Formatter {
        public String format(String s) { return new StringBuilder(s).reverse().toString(); }
    }

    // File: com.example.app/module-info.java
    module com.example.app {
        requires com.example.spi;
        uses com.example.spi.Formatter; // consume via ServiceLoader
        // NOTE: app does not need 'requires com.example.providers' at compile time
    }

    // File: com.example.app/com/example/app/Main.java
    package com.example.app;
    import com.example.spi.Formatter;
    import java.util.ServiceLoader;

    public class Main {
        public static void main(String[] args) {
            ServiceLoader<Formatter> loader = ServiceLoader.load(Formatter.class);
            for (Formatter f : loader) {
                System.out.println(f.getClass().getName() + " -> " + f.format("Abc"));
            }
        }
    }

    Run with all modules on the module-path; ServiceLoader will find providers automatically.
    ============================================================================================
    */

    /*
    ============================================================================================
    Example 7: Automatic modules and Automatic-Module-Name
    A regular JAR on the module-path becomes an automatic module.
    - If MANIFEST.MF contains: Automatic-Module-Name: com.google.common
      then its module name is stable (recommended).
    - Otherwise, the name is derived from the JAR name (e.g., guava-31.1-jre.jar -> guava).

    Using Guava as automatic module:

    // File: com.example.app/module-info.java
    module com.example.app {
        requires com.google.common; // or 'guava' if no Automatic-Module-Name
    }

    // File: com.example.app/com/example/app/Main.java
    package com.example.app;
    import com.google.common.base.Joiner;
    public class Main {
        public static void main(String[] args) {
            System.out.println(Joiner.on(", ").join("a", "b", "c"));
        }
    }

    Compile and run with guava JAR on module-path:
    javac --module-path lib:... -d out/app ...
    java  --module-path lib:out/app --module com.example.app/com.example.app.Main
    ============================================================================================
    */

    /*
    ============================================================================================
    Example 8: Unnamed module (classpath) and --add-reads
    - Code on the classpath is in the unnamed module.
    - Named modules do not automatically read the unnamed module.

    If com.example.app (named) needs to use a class OnlyOnClasspath from the classpath:
    java --module-path mods -cp libs/legacy.jar --add-reads com.example.app=ALL-UNNAMED \
         --module com.example.app/com.example.app.Main

    Alternatively, put the classpath JAR on the module-path to make it an automatic module.
    ============================================================================================
    */

    /*
    ============================================================================================
    Example 9: Resources in modules
    Place resources in the same package directory as classes within a module.

    // File: com.example.res/module-info.java
    module com.example.res {
        exports com.example.res;
    }

    // File: com.example.res/com/example/res/ResDemo.java
    package com.example.res;
    import java.io.BufferedReader;
    import java.io.InputStreamReader;
    import java.nio.charset.StandardCharsets;

    public class ResDemo {
        public static void main(String[] args) throws Exception {
            try (var in = ResDemo.class.getResourceAsStream("message.txt")) { // same package
                System.out.println(new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)).readLine());
            }
        }
    }

    // File: com.example.res/com/example/res/message.txt
    Hello from a module resource!
    ============================================================================================
    */

    /*
    ============================================================================================
    Example 10: Dynamic modules (ModuleLayer)
    Load a module at runtime (e.g., a plugin) from a path.

    // File: com.example.host/com/example/host/LayerDemo.java
    package com.example.host;

    import java.lang.module.Configuration;
    import java.lang.module.ModuleFinder;
    import java.nio.file.Path;
    import java.util.Set;

    public class LayerDemo {
        public static void main(String[] args) throws Exception {
            // Assume plugins/my.plugin.jar contains module my.plugin
            ModuleFinder finder = ModuleFinder.of(Path.of("plugins/my.plugin.jar"));

            ModuleLayer parent = ModuleLayer.boot();
            Configuration cf = parent.configuration().resolve(finder, ModuleFinder.of(), Set.of("my.plugin"));

            ModuleLayer.Controller controller = parent.defineModulesWithOneLoader(cf, ClassLoader.getSystemClassLoader());
            ModuleLayer layer = controller.layer();

            Module plugin = layer.findModule("my.plugin").orElseThrow();
            Class<?> pluginMain = Class.forName("my.plugin.Main", true, layer.findLoader("my.plugin"));
            pluginMain.getMethod("run").invoke(pluginMain.getDeclaredConstructor().newInstance());
        }
    }

    Notes:
    - Dynamic layers are advanced; prefer static modules unless you need runtime discovery.
    ============================================================================================
    */

    /*
    ============================================================================================
    Example 11: Command-line overrides
    Useful for migration and debugging; avoid in production when possible.

    --add-reads    <source-module>=<target-module>
    --add-exports  <module>/<pkg>=<target-module>
    --add-opens    <module>/<pkg>=<target-module>

    Examples:
    java --add-reads com.example.app=ALL-UNNAMED ...
    java --add-exports com.example.model/com.example.model=com.example.reflector ...
    java --add-opens   com.example.model/com.example.model=com.fasterxml.jackson.databind ...
    ============================================================================================
    */
}