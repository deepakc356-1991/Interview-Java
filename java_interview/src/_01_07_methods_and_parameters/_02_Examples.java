package _01_07_methods_and_parameters;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Methods & Parameters — comprehensive examples.
 * Run main(...) to see outputs and read inline comments for explanations.
 */
public class _02_Examples {

    // Instance state for demonstrating instance methods and shadowing
    private int instanceCounter = 0;
    private String name = "Default";

    // 1) BASIC DECLARATIONS

    // No parameters, no return
    static void sayHello() {
        System.out.println("Hello!");
    }

    // Parameters + return value
    static int add(int a, int b) {
        return a + b;
    }

    // Multiple parameters and a different return type
    static double average(int a, int b, int c) {
        return (a + b + c) / 3.0;
    }

    // 2) OVERLOADING: same name, different parameter lists
    static String greet(String name) {
        return "Hello, " + name + "!";
    }

    static String greet(String name, int times) {
        // Emulates "default parameters" for count via an overload
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times; i++) {
            sb.append(greet(name));
            if (i < times - 1) sb.append(" ");
        }
        return sb.toString();
    }

    // Varargs (String...) is another overload; Java will prefer the most specific match
    static String greet(String... names) {
        // Varargs are treated as an array at call-site
        if (names == null || names.length == 0) {
            return "Hello, nobody!";
        }
        return "Hello, " + String.join(", ", names) + "!";
    }

    // 3) VARARGS: "..."" must be the last parameter
    static int sum(int... numbers) {
        int total = 0;
        for (int n : numbers) total += n;
        return total;
    }

    // Varargs + a normal parameter (varargs must be last)
    static String joinWith(String delimiter, String... parts) {
        return String.join(delimiter, parts);
    }

    // 4) "DEFAULT PARAMETERS" IN JAVA: use overloading
    static void printReport(String title) {
        printReport(title, 1); // delegate with a default page count
    }

    static void printReport(String title, int pageCount) {
        System.out.println("Report: " + title + " (pages=" + pageCount + ")");
    }

    // 5) PASS-BY-VALUE: primitives vs object references

    // Primitives are copied; changes inside don't affect caller
    static void tryIncrement(int x) {
        x++; // caller's variable is unchanged
    }

    // Object references are also passed by value, but you can mutate the object
    static void renamePerson(Person p, String newName) {
        p.name = newName; // mutates the same object seen by the caller
    }

    // Reassigning the parameter reference does NOT affect the caller
    static void reassignPerson(Person p) {
        p = new Person("Reassigned"); // caller still holds original reference
    }

    // Swapping primitives doesn't affect the caller
    static void swapInts(int a, int b) {
        int tmp = a;
        a = b;
        b = tmp; // caller's variables remain unchanged
    }

    // Attempting to swap object references also doesn't affect the caller
    static void trySwapPersons(Person a, Person b) {
        Person tmp = a;
        a = b;
        b = tmp; // caller's references remain unchanged
    }

    // But mutating the two objects does affect the caller
    static void swapNames(Person a, Person b) {
        String tmp = a.name;
        a.name = b.name;
        b.name = tmp;
    }

    // 6) SCOPE & SHADOWING: local variables can shadow fields
    public void setName(String name) {
        // Parameter 'name' shadows the field 'name'; use 'this' to access the field
        this.name = name;
    }

    public int incrementCounter() {
        instanceCounter++;
        return instanceCounter;
    }

    public int incrementCounterBy(int increment) {
        int instanceCounter = 100; // local variable shadows the field
        // only the field is changed here:
        this.instanceCounter += increment;
        // local 'instanceCounter' remains 100 (unused)
        return this.instanceCounter;
    }

    public String getName() {
        return name;
    }

    public int getInstanceCounter() {
        return instanceCounter;
    }

    // 7) EXCEPTIONS & PARAMETER VALIDATION
    static double divide(int a, int b) {
        if (b == 0) throw new ArithmeticException("b must not be zero");
        return (double) a / b;
    }

    static int safeLength(String s) {
        // Validate parameters up-front
        return Objects.requireNonNull(s, "s must not be null").length();
    }

    // 8) RECURSION
    static long factorial(int n) {
        if (n < 0) throw new IllegalArgumentException("n must be >= 0");
        return (n <= 1) ? 1L : n * factorial(n - 1);
    }

    // 9) GENERIC METHOD
    static <T> T firstOrDefault(List<T> list, T defaultValue) {
        if (list == null || list.isEmpty()) return defaultValue;
        return list.get(0);
    }

    // 10) FLUENT API / METHOD CHAINING
    static class UrlBuilder {
        private String protocol = "http";
        private String host;
        private int port = -1;
        private String path = "";

        UrlBuilder protocol(String protocol) { this.protocol = protocol; return this; }
        UrlBuilder host(String host) { this.host = host; return this; }
        UrlBuilder port(int port) { this.port = port; return this; }
        UrlBuilder path(String path) { this.path = path; return this; }

        String build() {
            String p = (port > 0) ? (":" + port) : "";
            String prefix = path.startsWith("/") ? "" : "/";
            return protocol + "://" + host + p + prefix + path;
        }
    }

    // Access modifiers on methods (visibility)
    public void publicMethod() {
        System.out.println("publicMethod(): visible everywhere the class is visible");
    }

    void packagePrivateMethod() {
        System.out.println("packagePrivateMethod(): visible within the same package");
    }

    protected void protectedMethod() {
        System.out.println("protectedMethod(): visible in package + subclasses");
    }

    private void privateMethod() {
        System.out.println("privateMethod(): visible only within this class");
    }

    // Simple type for parameter passing demos
    static class Person {
        // Public for demo purposes (typically use private + getters/setters)
        public String name;

        Person(String name) { this.name = name; }

        @Override public String toString() { return "Person{name='" + name + "'}"; }
    }

    // 11) main: demonstrates all examples
    public static void main(String[] args) {
        System.out.println("== Basic methods ==");
        sayHello();
        System.out.println("add(2, 3) = " + add(2, 3));
        System.out.println("average(3, 5, 8) = " + average(3, 5, 8));

        System.out.println("\n== Overloading & Varargs ==");
        System.out.println(greet("Ada"));
        System.out.println(greet("Grace", 3));
        System.out.println(greet("Linus", "James", "Ken"));
        System.out.println("sum(1,2,3,4) = " + sum(1, 2, 3, 4));
        System.out.println("sum(new int[]{5,6,7}) = " + sum(new int[]{5, 6, 7}));
        System.out.println("joinWith(\"-\", \"a\",\"b\",\"c\") = " + joinWith("-", "a", "b", "c"));

        System.out.println("\n== Emulating default parameters via overloading ==");
        printReport("Quarterly Report");
        printReport("Weekly Report", 42);

        System.out.println("\n== Pass-by-value: primitives vs object references ==");
        int original = 10;
        tryIncrement(original);
        System.out.println("original after tryIncrement = " + original + " (unchanged)");

        Person p = new Person("Alice");
        renamePerson(p, "Bob");
        System.out.println("After renamePerson: " + p);
        reassignPerson(p);
        System.out.println("After reassignPerson: " + p + " (reference unchanged)");

        int x = 1, y = 2;
        swapInts(x, y);
        System.out.println("After swapInts(x,y): x=" + x + ", y=" + y + " (unchanged)");

        Person p1 = new Person("Tom");
        Person p2 = new Person("Jerry");
        trySwapPersons(p1, p2);
        System.out.println("After trySwapPersons: p1=" + p1 + ", p2=" + p2 + " (unchanged)");
        swapNames(p1, p2);
        System.out.println("After swapNames: p1=" + p1 + ", p2=" + p2 + " (names swapped)");

        System.out.println("\n== Scope & shadowing ==");
        _02_Examples ex = new _02_Examples();
        ex.setName("Zoe");
        System.out.println("Instance name: " + ex.getName());
        System.out.println("Counter: " + ex.incrementCounter());
        System.out.println("Counter: " + ex.incrementCounter());
        System.out.println("Counter before incrementBy(5): " + ex.getInstanceCounter());
        System.out.println("Counter after incrementBy(5): " + ex.incrementCounterBy(5));
        System.out.println("Counter now: " + ex.getInstanceCounter());

        System.out.println("\n== Exceptions & validation ==");
        System.out.println("divide(7, 2) = " + divide(7, 2));
        try {
            divide(1, 0);
        } catch (ArithmeticException e) {
            System.out.println("divide(1, 0) threw: " + e.getMessage());
        }
        System.out.println("safeLength(\"abc\") = " + safeLength("abc"));
        try {
            safeLength(null);
        } catch (NullPointerException e) {
            System.out.println("safeLength(null) threw: " + e.getMessage());
        }

        System.out.println("\n== Recursion ==");
        System.out.println("factorial(5) = " + factorial(5));

        System.out.println("\n== Generic method ==");
        List<String> list = Arrays.asList("alpha", "beta");
        System.out.println("firstOrDefault(list, \"none\") = " + firstOrDefault(list, "none"));
        System.out.println("firstOrDefault(empty, \"none\") = " + firstOrDefault(Collections.emptyList(), "none"));

        System.out.println("\n== Fluent API / method chaining ==");
        String url = new UrlBuilder()
                .protocol("https")
                .host("example.com")
                .port(8443)
                .path("docs/index.html")
                .build();
        System.out.println("Built URL = " + url);

        System.out.println("\n== Access modifiers (calls from within same class) ==");
        ex.publicMethod();
        ex.packagePrivateMethod();
        ex.protectedMethod();
        ex.privateMethod();

        System.out.println("\n== main parameters ==");
        System.out.println("args length = " + args.length + ", values = " + Arrays.toString(args));
    }
}