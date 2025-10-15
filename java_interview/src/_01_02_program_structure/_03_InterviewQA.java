package _01_02_program_structure;

import java.util.Arrays;

/**
 * Program Structure - Interview Q&A (Basic -> Intermediate -> Advanced)
 * Run to print concise interview questions and answers about Java program structure.
 */
public class _03_InterviewQA {

    private static int counter = 1;

    public static void main(String[] args) {
        title("Java Program Structure - Interview Q&A");
        basic();
        intermediate();
        advanced();
        demos();
        footer();
    }

    // ========== Sections ==========

    private static void basic() {
        section("Basic");

        qa("What are the minimal parts of a Java source file?",
                "Optional package declaration, optional imports, one top-level type (class/interface/enum/@interface).",
                "To run as an app, include a class with: public static void main(String[] args).");

        qa("Where must the package statement appear?",
                "At most once and it must be the first non-comment, non-annotation line.");

        qa("What is a package?",
                "A namespace grouping related types. Maps to directories (e.g., com.example maps to com/example).");

        qa("What does import do?",
                "Brings type or static member names into scope to avoid fully qualified names.",
                "Not needed for java.lang.* and same-package types.");

        qa("How many public top-level types can a .java file have?",
                "At most one. Its name must match the file name (e.g., MyClass in MyClass.java).");

        qa("What kinds of top-level types exist?",
                "class, interface, enum, and annotation type (@interface).");

        qa("What is the entry point method?",
                "public static void main(String[] args) or main(String... args).",
                "Return type must be void; overloading main does not change the entry point.");

        qa("Can a class without main be compiled?",
                "Yes. It's a library. Only apps you run need an entry point.");

        qa("How do you run a class in a package?",
                "java fully.qualified.ClassName",
                "Example: java com.example.App");

        qa("What is 'default' (package-private) access?",
                "No modifier: visible within the same package only.");

        qa("Access modifiers in brief?",
                "public (everywhere), protected (package + subclasses), default (package), private (class only).");

        qa("What is a constructor vs a method?",
                "Constructor initializes a new object; no return type; name matches class; not inherited.",
                "Methods have return types, names, and can be inherited/overridden.");

        qa("What is 'static' for fields/methods?",
                "Belongs to the class, not instances. Access via ClassName.member.");

        qa("What are initializer blocks?",
                "static { } runs once when class initializes; { } instance initializer runs before constructor each new instance.");

        qa("What does 'final' mean?",
                "final class: cannot be subclassed; final method: cannot be overridden; final field: cannot be reassigned.");

        qa("What goes in comments vs Javadoc?",
                "Comments: // or /* */ for implementation notes.",
                "Javadoc: /** ... */ for API docs on public types/members.");

        qa("Do imports affect runtime?",
                "No. Imports are compile-time conveniences only.");

        qa("Why should file name match the public class?",
                "Java specification requires it for compilation and class discovery.");

        qa("Can a top-level class be private or protected?",
                "No. Only public or package-private.");

        qa("What is java.lang automatically imported?",
                "Yes. Types like String, System are in scope without import.");

        qa("What are fields and methods?",
                "Fields hold state; methods define behavior. Both can be static or instance.");

        qa("What are conventions for naming?",
                "Packages: lowercase; Classes/Interfaces: PascalCase; methods/fields: camelCase; constants: UPPER_SNAKE_CASE.");

        qa("What is a source file (compilation unit)?",
                "A .java file containing at most one public top-level type plus optional non-public top-level types.");

        qa("Can you place multiple top-level types in one file?",
                "Yes, but only one may be public and its name must match the file.");
    }

    private static void intermediate() {
        section("Intermediate");

        qa("Order of class initialization?",
                "On first active use: load -> link -> initialize.",
                "Initialization: static fields and static blocks run in source order once.");

        qa("Order of object construction?",
                "1) Instance fields & instance initializers (in source order), 2) then constructor body.");

        qa("What is a forward reference?",
                "Using a field before it's initialized in the same initializer can be illegal.",
                "Compile-time constant (static final with constant value) can be referenced anywhere.");

        qa("Difference: static nested class vs inner class?",
                "static nested: no reference to outer instance; like a top-level class scoped inside.",
                "inner (non-static): holds implicit reference to outer instance (Outer.this).");

        qa("Local and anonymous classes?",
                "Local: named class inside a block/method. Anonymous: unnamed class instance.",
                "Both can access effectively final variables from the enclosing scope.");

        qa("Interface members and methods?",
                "Fields are implicitly public static final. Methods are public by default.",
                "Since Java 8: default and static methods; Java 9+: private helper methods.");

        qa("Enums structure?",
                "Enums are classes with fixed constants. Can have fields, constructors, methods, and implement interfaces.");

        qa("Constants best practice?",
                "public static final Type NAME = value; in an interface? Prefer a final class with private constructor.");

        qa("Overload vs override (structure-related)?",
                "Overload: same name, different parameters in same scope. Override: subclass provides new implementation.");

        qa("this() vs super() in constructors?",
                "this(...) calls another constructor in same class; super(...) calls parent constructor.",
                "Must be first statement; only one of them can appear.");

        qa("Varargs rules?",
                "Only last parameter can be varargs; compiled as an array. main(String... args) is valid.");

        qa("Package design for encapsulation?",
                "Use package-private types/members to hide internals; expose minimal public API.");

        qa("When is import static useful?",
                "For frequent static members (e.g., Assertions.assertEquals). Use sparingly for readability.");

        qa("Can you overload main?",
                "Yes, but the JVM only calls main(String[]/String...) as entry point.");

        qa("What is package-info.java?",
                "A file to hold package-level Javadoc and annotations: package com.example;");

        qa("Can an interface extend multiple interfaces?",
                "Yes. Classes can implement multiple interfaces but extend only one class.");

        qa("Nested top-level-like types inside interfaces?",
                "Types declared in interfaces are implicitly public static (for classes) and public static final (for fields).");

        qa("What is default access of a top-level interface/enum without modifier?",
                "Package-private (visible within the package).");

        qa("How do resources relate to program structure?",
                "Place resource files on the classpath near code. Load with ClassLoader.getResourceAsStream().");

        qa("What is a static import conflict?",
                "Two static imports exposing same simple name cause ambiguity; qualify or avoid.");

        qa("How do you organize a multi-class file responsibly?",
                "Keep only small helper non-public types together; otherwise create separate files.");

        qa("What is a synthetic member?",
                "Compiler-generated members (e.g., for inner class outer ref). Not written in source but present in bytecode.");

        qa("What makes a class a utility class?",
                "Private constructor and only static methods/fields.");

        qa("Can enums have abstract methods?",
                "Yes; each constant can override them.");

        qa("Interface fields reassignment?",
                "Not allowed; they are implicitly public static final.");
    }

    private static void advanced() {
        section("Advanced");

        qa("What are the phases of class loading?",
                "Loading (ClassLoader reads bytecode), Linking (verify, prepare, resolve), Initialization (run <clinit>).");

        qa("What triggers class initialization?",
                "Active use: new, static field write/read (non-compile-time-constant), reflective use, or main entry.");

        qa("What is <clinit> and <init>?",
                "<clinit> is the class static initializer method; <init> is the constructor for instances.");

        qa("What is a compile-time constant?",
                "A final primitive or String with literal initializer. Inlined at compile time across classes.");

        qa("ClassLoader hierarchy and visibility?",
                "Parents-first delegation by default. Child can load classes not visible to parent.");

        qa("Classpath vs Module path?",
                "Classpath: flat namespace of packages. Module path: modules with explicit dependencies and exports.");

        qa("module-info.java purpose?",
                "Declares module name, requires dependencies, exports/opened packages.");

        qa("How to make a JAR executable?",
                "Add MANIFEST.MF with Main-Class: fqcn, then run: java -jar app.jar");

        qa("What is strong encapsulation in modules?",
                "Only exported packages are accessible at compile/run time; reflection to non-open packages is restricted.");

        qa("Binary vs source compatibility (structure)?",
                "Binary compatibility allows existing binaries to run without recompilation after changes.",
                "Changing method signatures or removing public members breaks it.");

        qa("package-private across modules?",
                "Package-private is per package, regardless of modules; modules control inter-module access via exports.");

        qa("Annotation targets and retention?",
                "Targets: TYPE, METHOD, FIELD, etc. Retention: SOURCE, CLASS, RUNTIME; affects availability to reflection.");

        qa("Annotation processors and structure?",
                "At compile time, processors can generate code based on annotations (e.g., META-INF/services).");

        qa("Type erasure and structure?",
                "Generics are erased at runtime; bytecode contains raw types with synthetic bridges for overrides.");

        qa("Local classes and effectively final rule?",
                "Local/anonymous classes can capture only effectively final variables of the enclosing scope.");

        qa("Circular static initialization pitfall?",
                "Reading a static field of a class still initializing can see default values (0/null). Avoid cycles.");

        qa("Multiple top-level classes with same simple name in different packages?",
                "Allowed. Use fully qualified names or imports to disambiguate.");

        qa("How to hide implementation packages?",
                "Keep them non-exported in modules or non-public in classpath builds; expose only API packages.");

        qa("Multi-release JARs?",
                "A JAR that carries version-specific class files under META-INF/versions/N for newer JDKs.");

        qa("Sealed classes/records (modern structure)?",
                "Records: concise immutable data carriers. Sealed classes/interfaces restrict permitted subclasses.",
                "Use when modeling fixed hierarchies (requires Java 17+).");

        qa("What is package annotation via package-info?",
                "Annotate the package declaration in package-info.java; applies to the entire package.");

        qa("How to place tests relative to structure?",
                "Mirror package structure in test sources; keep test classes in the same package for package-private access.");

        qa("Can you nest packages (like com.example and com.example.api)?",
                "Yes. They are independent; package hierarchy is by name convention, not by nested scopes.");

        qa("How does reflection bypass access?",
                "AccessibleObject#setAccessible(true) can relax checks pre-Java 9; modules may restrict this unless opened.");

        qa("How to load resources from a different module/classloader?",
                "Use the right ClassLoader (Thread.currentThread().getContextClassLoader()) or Module API in JPMS.");
    }

    // ========== Mini Demos ==========

    private static void demos() {
        section("Mini Demos (Structure Behavior)");

        println("Demo 1: Initialization order");
        new InitOrderDemo();
        line();

        println("Demo 2: Nested vs inner class");
        NestedVsInnerDemo.run();
        line();

        println("Demo 3: Enum with behavior");
        println(SampleStatus.IN_PROGRESS.describe());
        line();

        println("Demo 4: Interface default/static methods");
        DefaultMethodDemo impl = new DefaultMethodImpl();
        println("Default says: " + impl.greet("Dev"));
        println("Static says: " + DefaultMethodDemo.version());
        line();

        println("Demo 5: Local and anonymous classes");
        LocalAnonymousDemo.run();
        line();

        println("Tip: For records / sealed classes, see printed examples below.");
        code(
                "// Record (Java 16+)",
                "public record Point(int x, int y) { }",
                "",
                "// Sealed (Java 17+)",
                "public sealed abstract class Shape permits Circle, Rectangle { }",
                "public final class Circle extends Shape { }",
                "public non-sealed class Rectangle extends Shape { }"
        );
    }

    // ========== Helper Printing ==========

    private static void qa(String question, String... answerLines) {
        println(counter++ + ") Q: " + question);
        println("   A: " + (answerLines.length > 0 ? answerLines[0] : ""));
        for (int i = 1; i < answerLines.length; i++) {
            println("      " + answerLines[i]);
        }
        line();
    }

    private static void title(String s) {
        line();
        println("====================================================");
        println(s);
        println("====================================================");
        line();
    }

    private static void section(String s) {
        println("----------------------------------------------------");
        println(s);
        println("----------------------------------------------------");
    }

    private static void footer() {
        println("End of Q&A.");
    }

    private static void line() {
        println("");
    }

    private static void println(String s) {
        System.out.println(s);
    }

    private static void code(String... lines) {
        println("Code:");
        Arrays.stream(lines).forEach(l -> println("   " + l));
    }

    // ========== Demo Types (Java 8+ compatible) ==========

    // Demo 1: Initialization order
    static class InitOrderDemo {
        static int STATIC_FIELD = print("  [static field init]");
        static {
            print("  [static block]");
        }

        int instanceField = print("  [instance field init]");
        {
            print("  [instance initializer]");
        }

        InitOrderDemo() {
            print("  [constructor]");
        }

        private static int print(String msg) {
            System.out.println(msg);
            return 0;
        }
    }

    // Demo 2: Nested vs inner
    static class NestedVsInnerDemo {
        static class StaticNested {
            void run() {
                System.out.println("  StaticNested: no outer reference");
            }
        }

        class Inner {
            void run() {
                // Implicit reference to enclosing instance of NestedVsInnerDemo
                System.out.println("  Inner: has outer reference = " + NestedVsInnerDemo.this.getClass().getSimpleName());
            }
        }

        static void run() {
            NestedVsInnerDemo outer = new NestedVsInnerDemo();
            new StaticNested().run();
            outer.new Inner().run();
        }
    }

    // Demo 3: Enum with behavior
    enum SampleStatus {
        NEW {
            @Override String describe() { return "New: not started"; }
        },
        IN_PROGRESS {
            @Override String describe() { return "In progress: working"; }
        },
        DONE {
            @Override String describe() { return "Done: completed"; }
        };
        abstract String describe();
    }

    // Demo 4: Interface default/static methods
    interface DefaultMethodDemo {
        default String greet(String name) {
            return "Hello, " + name;
        }
        static String version() {
            return "1.0";
        }
    }
    static class DefaultMethodImpl implements DefaultMethodDemo { }

    // Demo 5: Local and anonymous classes
    static class LocalAnonymousDemo {
        static void run() {
            final String suffix = "!";
            // Local class
            class Local {
                String say(String s) { return "Local says: " + s + suffix; }
            }
            System.out.println(new Local().say("Hi"));

            // Anonymous class
            Runnable r = new Runnable() {
                @Override public void run() {
                    System.out.println("Anonymous Run" + suffix);
                }
            };
            r.run();
        }
    }
}