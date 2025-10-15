package _01_02_program_structure;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import static java.lang.Math.PI;
import static java.lang.Math.sqrt;

/**
 * Demonstrates Java program structure elements in one file:
 * - package and imports
 * - comments and Javadoc
 * - top-level public class and a package-private top-level class
 * - fields, constants, initializer blocks
 * - constructors, methods (instance, static, varargs, generic, throws)
 * - access modifiers: public, protected, package-private, private
 * - nested types: static nested, inner, local, anonymous
 * - interface (with default and static methods), enum, annotation
 * - static imports and main entry point
 */
public class _02_Examples {
    // Constant (public static final)
    public static final String APP_NAME = "Program Structure Examples";
    // Package-private constant (static final) - not visible outside the package
    static final String DEFAULT_OWNER = "default-owner";
    // Private constants
    private static final int VERSION_MAJOR = 1;
    private static final int VERSION_MINOR = 0;

    // Static initialization block: runs once when the class is loaded
    static {
        if (VERSION_MAJOR < 0) {
            throw new IllegalStateException("Invalid VERSION_MAJOR");
        }
    }

    // Instance fields with different access levels
    private int instanceCounter;
    protected String owner;
    String packageNote; // package-private (no modifier)

    // Instance initialization block: runs before each constructor
    {
        instanceCounter = 0;
        packageNote = "Created at " + LocalDate.now();
    }

    // Constructors (overloaded)
    public _02_Examples() {
        this(DEFAULT_OWNER); // constructor chaining
    }

    public _02_Examples(String owner) {
        this.owner = owner;
    }

    // Instance method (overloaded)
    public void greet() {
        greet("World");
    }

    public void greet(String name) {
        System.out.println("Hello, " + name + " from " + owner);
        instanceCounter++;
    }

    // Static method (utility)
    public static void printHeader() {
        System.out.printf("%s v%d.%d%n", APP_NAME, VERSION_MAJOR, VERSION_MINOR);
    }

    // Method with parameters, return value, and throws
    public int divide(int a, int b) throws ArithmeticException {
        if (b == 0) throw new ArithmeticException("b must not be 0");
        return a / b;
    }

    // Varargs method (accepts any number of ints)
    public static int sum(int... nums) {
        int s = 0;
        for (int n : nums) s += n;
        return s;
    }

    // Generic method
    public static <T> T first(List<T> list) {
        return list.isEmpty() ? null : list.get(0);
    }

    // Static nested class (does not capture outer instance)
    public static class StaticNested {
        private final LocalDate created = LocalDate.now();
        public String info() {
            return "StaticNested created " + created;
        }
    }

    // Inner class (captures outer instance)
    public class Inner {
        public String owner() {
            return _02_Examples.this.owner;
        }
    }

    // Functional interface with default and static methods
    @FunctionalInterface
    public interface Describable {
        String description();
        default String shortDescription() {
            String s = description();
            int i = s.indexOf('\n');
            return i >= 0 ? s.substring(0, i) : s;
        }
        static String join(Describable... ds) {
            StringBuilder sb = new StringBuilder();
            for (Describable d : ds) {
                if (sb.length() > 0) sb.append(" | ");
                sb.append(d.shortDescription());
            }
            return sb.toString();
        }
    }

    // Enum with fields, constructor, and method; implements an interface
    public enum Level implements Describable {
        LOW(1), MEDIUM(2), HIGH(3);
        private final int severity;
        Level(int severity) { this.severity = severity; }
        public int severity() { return severity; }
        @Override public String description() { return name() + " severity=" + severity; }
    }

    // Custom annotation (default retention: CLASS)
    public @interface Demo {
        String value();
        boolean enabled() default true;
    }

    // Annotated method
    @Demo("Example of using a custom annotation")
    public void annotatedMethod() { /* no-op */ }

    // Method demonstrating a local class and an anonymous class
    public void demonstrateLocalAndAnonymous() {
        // Local class (visible only within this method)
        class LocalHelper implements Describable {
            private final String note;
            LocalHelper(String note) { this.note = note; }
            @Override public String description() { return "LocalHelper: " + note; }
        }
        Describable local = new LocalHelper("created inside a method");

        // Anonymous class implementing an interface
        Describable anon = new Describable() {
            @Override public String description() { return "Anonymous Describable"; }
        };

        System.out.println(Describable.join(local, anon));
    }

    // Static import usage: compute a value using Math.PI and Math.sqrt
    public static double circleDiagonalFromArea(double area) {
        // area = PI * r^2 -> r = sqrt(area / PI)
        double r = sqrt(area / PI);
        // diagonal of bounding square = 2 * r * sqrt(2)
        return 2 * r * sqrt(2);
    }

    // Override toString to demonstrate method overriding
    @Override
    public String toString() {
        return "_02_Examples{owner='" + owner + "', instanceCounter=" + instanceCounter + "}";
    }

    // Entry point (program starts here)
    public static void main(String[] args) {
        printHeader(); // static method

        // Command-line arguments
        System.out.println("Args: " + Arrays.toString(args));

        // Create an instance and call instance methods
        _02_Examples ex = new _02_Examples("Alice");
        ex.greet();
        ex.greet("Bob");
        System.out.println(ex); // calls toString()

        // Exception handling (try/catch)
        try {
            System.out.println("10 / 2 = " + ex.divide(10, 2));
        } catch (ArithmeticException e) {
            System.err.println("Divide error: " + e.getMessage());
        }

        // Varargs
        System.out.println("sum(1,2,3) = " + sum(1, 2, 3));

        // Static nested and inner classes
        StaticNested sn = new StaticNested();
        System.out.println(sn.info());
        Inner in = ex.new Inner();
        System.out.println("Inner owner = " + in.owner());

        // Enum usage
        for (Level level : Level.values()) {
            System.out.println(level.description());
        }

        // Interface usage: lambda, anonymous class, default and static methods
        Describable d1 = () -> "Lambda Describable example";
        Describable d2 = new Describable() {
            @Override public String description() { return "Anonymous class implementing Describable"; }
        };
        System.out.println("Joined: " + Describable.join(d1, d2));
        System.out.println("d1 short: " + d1.shortDescription());

        // Local and anonymous classes
        ex.demonstrateLocalAndAnonymous();

        // Static import demo
        double area = 50.0;
        double diag = circleDiagonalFromArea(area);
        System.out.printf("Circle area=%.2f -> bounding square diagonal=%.4f%n", area, diag);

        // Assertion (enable with: java -ea ...)
        assert diag > 0 : "Diagonal must be positive";

        // Package-private top-level class in the same file
        Helper helper = new Helper("helper-1");
        System.out.println(helper.describe());
    }
}

// Package-private top-level class (only one public top-level class is allowed per file)
class Helper {
    private final String id;
    Helper(String id) { this.id = id; }
    String describe() { return "Helper[" + id + "] (package-private top-level class)"; }
}