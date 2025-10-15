package _01_02_program_structure;

import java.util.*;                // Regular imports bring types into scope (type-only, not subpackages)
import static java.lang.Math.*;    // Static import brings static members (e.g., PI, sqrt) into scope

/**
 * Topic: Program Structure (Java)
 *
 * This file is intentionally rich with comments to explain the "theory" of Java program structure
 * using a single compilable source file as the teaching vehicle.
 *
 * Source file structure (top-to-bottom order):
 * 1) Optional package declaration (highly recommended, avoid default package).
 * 2) Optional import and static import statements.
 * 3) One public top-level type whose simple name must match the file name (_01_Theory).
 * 4) Zero or more package-private top-level types (allowed) in the same file.
 *
 * Package:
 * - Packages provide namespaces and control access. Use reverse-DNS + feature path:
 *   e.g., com.example.app.user, or _01_02_program_structure used here for learning.
 * - Place each package's source types under a corresponding directory path.
 * - Avoid the default (unnamed) package.
 *
 * Imports:
 * - import pkg.Type;           // Single-type import
 * - import pkg.*;              // On-demand import for that package only (not recursive to subpackages)
 * - import static pkg.Type.*;  // Static members on-demand
 * - Fully qualified names can be used instead of imports (e.g., java.util.List).
 *
 * Top-level type rules:
 * - Exactly one public top-level class per file; its name must match the file name.
 * - Additional top-level classes/interfaces/enums in the same file must be package-private (no modifier).
 *
 * Class structure (members order is a style choice, but commonly):
 * - Fields (constants first), static init blocks, instance init blocks, constructors, methods, nested types.
 * - Access modifiers: public, protected, (package-private), private.
 * - Other modifiers: static, final, abstract, synchronized, native, strictfp, transient, volatile.
 *
 * Entry point:
 * - public static void main(String[] args) is the conventional entry point.
 *
 * Blocks, statements, expressions, and scope:
 * - Blocks define scopes { ... }; local variables live within their block.
 * - Statements: declaration, assignment, control flow (if, switch, loops), try-catch-finally, return, throw, assert.
 *
 * Initialization order:
 * - Static fields and static initializer blocks run once when the class is initialized (first active use).
 * - Instance fields and instance initializer blocks run before the constructor body on each instantiation.
 *
 * Nested types:
 * - Static nested class (does not capture outer instance).
 * - Inner (non-static) class (has implicit reference to outer instance).
 * - Local class (declared within a block).
 * - Anonymous class (inline subclass or interface implementation).
 *
 * Interfaces, enums, annotations:
 * - Interfaces may declare abstract, default, static methods, and constants (implicitly public static final).
 * - Enums are types with a fixed set of constants; can have fields and methods.
 * - Annotations add metadata; can target types, methods, fields, etc.
 *
 * Naming conventions:
 * - Packages: all lowercase (e.g., com.example.tools).
 * - Classes/Interfaces/Enums/Annotations: PascalCase (e.g., HttpServer).
 * - Methods/fields/variables: camelCase (e.g., maxCount).
 * - Constants: UPPER_SNAKE_CASE (e.g., MAX_SIZE).
 *
 * Build, run, and classpath (overview):
 * - Compile: javac -d out src/_01_02_program_structure/_01_Theory.java
 * - Run:    java -cp out _01_02_program_structure._01_Theory
 * - Classpath locates compiled classes/jars. Modules (module-info.java) add stronger boundaries (beyond scope here).
 *
 * Documentation:
 * - Use Javadoc comments (/** ... /) on public APIs. Generate with: javadoc ...
 *
 * Notes:
 * - This file demonstrates many concepts; in real projects split code into multiple small cohesive classes/files.
 */
public class _01_Theory {

    // -----------------------------------------
    // Constants, fields, and initialization
    // -----------------------------------------

    /**
     * Public constant (by convention: public static final UPPER_SNAKE_CASE).
     * Constants are inlined at compile time when referenced across modules/jars; avoid changing values post-release.
     */
    public static final String TOPIC = "Program Structure";

    // Static field (class-level state, shared by all instances).
    private static int staticCounter;

    // Instance field (state per object instance).
    private int instanceCounter;

    // Static initializer block: runs once when the class is first initialized.
    static {
        // Good for complex static setups, but prefer inline initializers for simple values.
        staticCounter = 1;
        // System.out.println is side-effectful; keep static init light and deterministic.
    }

    // Instance initializer block: runs before each constructor.
    {
        instanceCounter = 0;
    }

    // -----------------------------------------
    // Constructors
    // -----------------------------------------

    // No-arg constructor
    public _01_Theory() {
        // 'this(...)' may chain to another constructor (must be first statement when used).
        instanceCounter = 42;
    }

    // Overloaded constructor (compile-time selection based on parameter list)
    public _01_Theory(int start) {
        this.instanceCounter = start;
    }

    // -----------------------------------------
    // Entry point
    // -----------------------------------------

    /**
     * Entry point. The JVM searches this signature when running a class.
     * Arguments (args) contain command-line tokens.
     */
    public static void main(String[] args) {
        // Basic I/O
        System.out.println("Topic: " + TOPIC);
        if (args.length > 0) {
            System.out.println("First arg: " + args[0]);
        }

        // Using static import (PI) and FQN alternatives
        double r = 2.0;
        double area = PI * r * r;            // from static import java.lang.Math.*
        double hyp = java.lang.Math.hypot(3, 4); // FQN usage
        System.out.println("Area=" + area + ", Hypotenuse=" + hyp);

        // Instantiate and run examples
        _01_Theory demo = new _01_Theory();
        demo.runExamples();

        // Demonstrate enum usage
        demo.exampleEnumUsage();

        // Show that collections and generics are part of type system/structure
        demo.exampleCollections();

        // Assertions are disabled by default; enable with: java -ea ...
        demo.exampleAssert();

        // Anonymous/local/inner class illustrations
        demo.exampleInnerClasses();

        // Interfaces and lambdas
        demo.exampleInterfaceUsage();

        // Exceptions (checked vs unchecked)
        demo.exampleExceptions();
    }

    // -----------------------------------------
    // Methods (parameters, return, overloading)
    // -----------------------------------------

    /**
     * Demonstrates control flow, statements, scope, and basic constructs.
     */
    private void runExamples() {
        exampleStatementsAndBlocks();
        exampleScopesAndShadowing();
        exampleOverloading(5);
        exampleOverloading(5, 7);
        exampleVarargs("one", "two", "three");
        System.out.println("examplePassByValue before: instanceCounter=" + instanceCounter);
        examplePassByValue(instanceCounter);
        System.out.println("examplePassByValue after: instanceCounter=" + instanceCounter + " (unchanged)");
        System.out.println("toString(): " + this);
    }

    // Overloaded methods (same name, different parameter lists)
    private int exampleOverloading(int a) {
        return a * a;
    }

    private int exampleOverloading(int a, int b) {
        return a + b;
    }

    // Varargs (treated as array inside the method)
    private int exampleVarargs(String... items) {
        int len = 0;
        for (String s : items) len += s.length();
        return len;
    }

    // Parameters are passed by value in Java (including object references as values)
    private void examplePassByValue(int value) {
        value = 999; // Only modifies local copy, caller's variable is unaffected
    }

    // -----------------------------------------
    // Statements, blocks, and control flow
    // -----------------------------------------

    private void exampleStatementsAndBlocks() {
        // Declaration and assignment
        int a = 1;
        a += 2; // a = 3

        // If-else
        if (a > 0) {
            System.out.println("a is positive");
        } else {
            System.out.println("a is not positive");
        }

        // Traditional switch (string/int/etc.). Enhanced switch exists in modern Java; here we keep the classic form.
        int code = 2;
        String label;
        switch (code) {
            case 1: label = "ONE"; break;
            case 2: label = "TWO"; break;
            default: label = "OTHER";
        }
        System.out.println("switch label = " + label);

        // Loops: for, while, do-while
        for (int i = 0; i < 3; i++) {
            System.out.print(i + " ");
        }
        System.out.println();

        int i = 0;
        while (i < 2) {
            System.out.print("w" + i + " ");
            i++;
        }
        System.out.println();

        int j = 0;
        do {
            System.out.print("d" + j + " ");
            j++;
        } while (j < 2);
        System.out.println();

        // try-catch-finally (finally always runs)
        try {
            int x = 10 / 0; // will throw
            System.out.println(x);
        } catch (ArithmeticException ex) {
            System.out.println("Caught: " + ex);
        } finally {
            System.out.println("Finally executed");
        }

        // Synchronized block is a statement form that acquires a monitor; shown for completeness.
        synchronized (this) {
            // Critical section
        }
    }

    // -----------------------------------------
    // Scope and shadowing
    // -----------------------------------------

    private int value = 10; // Field

    private void exampleScopesAndShadowing() {
        int value = 5; // Shadows the field inside this block/method scope
        System.out.println("Local value=" + value + ", field value=" + this.value);

        // Block scope
        {
            int onlyHere = 123;
            System.out.println("Block var=" + onlyHere);
        }
        // 'onlyHere' is out of scope now

        // Labeled break
        outer:
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                if (y == 1) break outer;
            }
        }
    }

    // -----------------------------------------
    // Exceptions (checked vs unchecked)
    // -----------------------------------------

    private void exampleExceptions() {
        // Checked exception must be declared or caught
        try {
            mightThrowChecked();
        } catch (java.io.IOException e) {
            System.out.println("Handled checked exception: " + e.getClass().getSimpleName());
        }

        // Unchecked exception can be thrown without declaration
        try {
            mightThrowUnchecked();
        } catch (RuntimeException e) {
            System.out.println("Handled unchecked exception: " + e.getClass().getSimpleName());
        }
    }

    private void mightThrowChecked() throws java.io.IOException {
        if (System.nanoTime() < 0) { // never true; example only
            throw new java.io.IOException("I/O failed");
        }
    }

    private void mightThrowUnchecked() {
        if (System.nanoTime() < 0) { // never true; example only
            throw new IllegalStateException("Something is wrong");
        }
    }

    // -----------------------------------------
    // Assertions (disabled by default; run with -ea to enable)
    // -----------------------------------------

    private void exampleAssert() {
        int sum = 2 + 2;
        assert sum == 4 : "Math is broken";
    }

    // -----------------------------------------
    // Nested, inner, local, and anonymous classes
    // -----------------------------------------

    // Static nested class: does not capture an outer instance
    static class StaticNested {
        static int s = 1;
        int x = 2;
    }

    // Inner (non-static) class: implicitly holds a reference to the outer _01_Theory instance
    class Inner {
        int y = 3;
        int sumWithOuter() {
            // Access outer's members via _01_Theory.this
            return _01_Theory.this.instanceCounter + this.y;
        }
    }

    private void exampleInnerClasses() {
        StaticNested sn = new StaticNested(); // Outer not required
        System.out.println("StaticNested.s=" + StaticNested.s + ", x=" + sn.x);

        Inner in = new Inner(); // Requires an outer instance
        System.out.println("Inner sumWithOuter=" + in.sumWithOuter());

        // Local class (declared within a method block)
        class Local {
            int val = 7;
        }
        Local local = new Local();
        System.out.println("Local.val=" + local.val);

        // Anonymous class (inline implementation)
        Runnable r = new Runnable() {
            @Override public void run() {
                System.out.println("Anonymous class run()");
            }
        };
        r.run();
    }

    // -----------------------------------------
    // Interfaces and default/static methods, lambdas
    // -----------------------------------------

    interface Greeter {
        String greet(String name);
        default String greetPolitely(String name) { return "Hello, " + name; }
        static Greeter defaultGreeter() { return n -> "Hi " + n; } // Lambda is an instance of a synthetic class
    }

    private void exampleInterfaceUsage() {
        Greeter casual = n -> "Hey " + n; // Lambda for functional interface
        System.out.println(casual.greet("Bob"));
        System.out.println(casual.greetPolitely("Alice"));
        System.out.println(Greeter.defaultGreeter().greet("Eve"));
    }

    // -----------------------------------------
    // Enums
    // -----------------------------------------

    enum Day {
        MON, TUE, WED, THU, FRI, SAT, SUN;
        boolean isWeekend() { return this == SAT || this == SUN; }
    }

    private void exampleEnumUsage() {
        for (Day d : Day.values()) {
            System.out.print(d + (d.isWeekend() ? "(W) " : "(Wk) "));
        }
        System.out.println();
    }

    // -----------------------------------------
    // Annotations (definition and usage)
    // -----------------------------------------

    // Annotation type nested in the class (implicitly static)
    public @interface Demo {
        String value();
    }

    @Demo("metadata-example")
    @Deprecated // Built-in annotation to mark APIs as discouraged
    private void annotatedMethod() {
        // No-op
    }

    // -----------------------------------------
    // Collections and Generics (type system, imports)
    // -----------------------------------------

    private void exampleCollections() {
        List<String> names = new ArrayList<>();
        names.add("Ada");
        names.add("Linus");
        for (String n : names) {
            System.out.print(n + " ");
        }
        System.out.println();

        Map<String, Integer> counts = new HashMap<>();
        counts.put("apples", 3);
        counts.put("oranges", 2);
        System.out.println("Map size=" + counts.size());
    }

    // -----------------------------------------
    // Access modifiers examples (members)
    // -----------------------------------------

    public int publicField;
    protected int protectedField;
    int packagePrivateField; // No modifier = package-private
    private int privateField;

    public void publicMethod() {}
    protected void protectedMethod() {}
    void packagePrivateMethod() {}
    private void privateMethod() {}

    // -----------------------------------------
    // toString override (Object contract)
    // -----------------------------------------

    @Override
    public String toString() {
        return "_01_Theory{topic=" + TOPIC + ", staticCounter=" + staticCounter + ", instanceCounter=" + instanceCounter + "}";
    }
}

/**
 * Additional top-level types in the same file are allowed if they are NOT public.
 * This illustrates file-level structure: only one public class, but multiple package-private types are okay.
 */
class PackagePrivateHelper {
    // Visible only within package _01_02_program_structure
    static String joinWithComma(List<String> items) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(items.get(i));
        }
        return sb.toString();
    }
}

/*
Further notes (theory summary):
- package-info.java can hold package-level annotations and Javadoc.
- Module system (module-info.java) defines module boundaries and exports (Java 9+).
- Typical project layout (Maven/Gradle):
  - src/main/java/...       production sources
  - src/test/java/...       test sources
  - src/main/resources/...  non-code resources on classpath
- Build tools (Maven/Gradle) manage compilation, dependencies, packaging (JAR), and runs.
- Style: consistent formatting, small classes, single responsibility, cohesive packages, clear names, Javadoc for APIs.
*/