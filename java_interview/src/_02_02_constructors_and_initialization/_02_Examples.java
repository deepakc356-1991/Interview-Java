package _02_02_constructors_and_initialization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*
  Constructors & Initialization
  This single file contains many examples (with console output) that illustrate:
  - Default constructors and default field values
  - Explicit no-arg and parameterized constructors
  - Overloading and constructor chaining (this())
  - super() calls and initialization order across inheritance
  - Static and instance initialization blocks
  - Field initialization order
  - Final fields (blank finals), static finals, and initialization rules
  - Forward references in field initializers
  - Varargs constructors
  - Constructors that throw exceptions
  - Pitfalls: calling overridable methods in constructors, "this" escape
  - Instance initializer blocks
  - Static initializer blocks
  - Copy constructors and defensive copies
  - Private constructors (utility class, singleton)
  - Static factories vs constructors
  - Enum constructors (always private)
  - Double-brace initialization (illustrative only; generally avoid)
*/
public class _02_Examples {

    public static void main(String[] args) throws Exception {
        System.out.println("=== 1) Default constructor & field default values ===");
        NoExplicitConstructor nec = new NoExplicitConstructor(); // implicit default constructor
        System.out.println("int defaults to " + nec.count);     // 0
        System.out.println("String defaults to " + nec.name);    // null
        // Note: local variables are not auto-initialized (uncommenting below won't compile)
        // int local; System.out.println(local);

        System.out.println("\n=== 2) Explicit no-arg constructor ===");
        ExplicitNoArg ena = new ExplicitNoArg();

        System.out.println("\n=== 3) Parameterized constructors & 'this' for shadowing ===");
        Point p = new Point(3, 4);
        System.out.println(p);

        System.out.println("\n=== 4) Overloaded constructors + this() chaining ===");
        Rectangle r1 = new Rectangle();
        Rectangle r2 = new Rectangle(5);
        Rectangle r3 = new Rectangle(2, 3);
        System.out.println(r1);
        System.out.println(r2);
        System.out.println(r3);

        System.out.println("\n=== 5) super() call order across inheritance ===");
        new Dog("Rex"); // observe static blocks, instance blocks, then constructors

        System.out.println("\n=== 6) Static/instance init order within a class ===");
        new InitOrder(); // first time: static field -> static block -> instance field -> instance block -> constructor
        new InitOrder(); // subsequent times: no static init; just instance parts

        System.out.println("\n=== 7) Init order across inheritance ===");
        new Child();

        System.out.println("\n=== 8) Final fields (instance and static) ===");
        FinalDemo fd = new FinalDemo(99);
        System.out.println(fd);
        System.out.println("S_COMPILE_TIME=" + FinalDemo.S_COMPILE_TIME);
        System.out.println("S_RUNTIME=" + FinalDemo.S_RUNTIME);
        System.out.println("S_LATE=" + FinalDemo.S_LATE);

        System.out.println("\n=== 9) Forward references and static field init order ===");
        System.out.println("Forward.A=" + Forward.A + ", Forward.B=" + Forward.B);

        System.out.println("\n=== 10) Varargs constructor ===");
        new VarargsBox("a", "b", "c");
        new VarargsBox(); // empty

        System.out.println("\n=== 11) Constructors can throw exceptions ===");
        try {
            new Failable(true);
        } catch (Exception e) {
            System.out.println("Caught: " + e.getMessage());
        }
        new Failable(false);

        System.out.println("\n=== 12) Avoid calling overridable methods in constructors ===");
        new ChildInitPitfall(); // shows override called before child fields initialized

        System.out.println("\n=== 13) 'this' escape during construction (anti-pattern) ===");
        new ThisEscapeDemo(); // demonstrates callback observing partially-constructed state

        System.out.println("\n=== 14) Instance initializer blocks ===");
        new InstanceInitBlockDemo();
        new InstanceInitBlockDemo(7);

        System.out.println("\n=== 15) Static initializer blocks ===");
        System.out.println("StaticInitBlockDemo.VALUE=" + StaticInitBlockDemo.VALUE);

        System.out.println("\n=== 16) Copy constructor & defensive copies ===");
        Person john = new Person("John", new int[]{1, 2, 3});
        Person johnCopy = new Person(john);
        john.getScores()[0] = 99; // original changed; copy remains unchanged due to defensive copy
        System.out.println("Original: " + john);
        System.out.println("Copy:     " + johnCopy);

        System.out.println("\n=== 17) Private constructors: utility class and singleton ===");
        System.out.println("Math2.add(2,3)=" + Math2.add(2, 3));
        System.out.println("Singleton.getInstance()==Singleton.getInstance()? " + (Singleton.getInstance() == Singleton.getInstance()));

        System.out.println("\n=== 18) Static factory vs constructor ===");
        Range rOpen = Range.of(1, 5);
        Range rClosed = Range.closed(1, 5);
        System.out.println(rOpen);
        System.out.println(rClosed);

        System.out.println("\n=== 19) Enums have private constructors ===");
        for (Color c : Color.values()) {
            System.out.println(c + " rgb=" + c.rgb());
        }

        System.out.println("\n=== 20) Double-brace initialization (illustrative; avoid in production) ===");
        DoubleBraceDemo.demonstrate();
    }

    // Simple helper to use in field initializers where an int is required.
    private static int log(String msg) {
        System.out.println(msg);
        return 0;
    }

    // 1) Implicit default constructor example
    static class NoExplicitConstructor {
        int count;   // default 0
        String name; // default null
        // No constructors defined => compiler adds public NoExplicitConstructor() {}
    }

    // 2) Explicit no-arg constructor
    static class ExplicitNoArg {
        ExplicitNoArg() {
            System.out.println("ExplicitNoArg(): explicit no-arg constructor runs");
        }
    }

    // 3) Parameterized constructor + 'this' for shadowing
    static class Point {
        final int x, y;

        Point(int x, int y) { // parameters shadow fields
            this.x = x; // 'this' disambiguates field from parameter
            this.y = y;
        }

        @Override
        public String toString() {
            return "Point(" + x + "," + y + ")";
        }
    }

    // 4) Overloading + this() chaining (first statement only!)
    static class Rectangle {
        int width, height;

        Rectangle() {
            this(1, 1); // delegate to the (w,h) constructor
        }

        Rectangle(int size) {
            this(size, size); // square
        }

        Rectangle(int width, int height) {
            // 'this()' calls must be first; you can't execute statements before them.
            this.width = width;
            this.height = height;
            System.out.println("Rectangle(" + width + "," + height + ")");
        }

        @Override
        public String toString() {
            return "Rectangle[" + width + "x" + height + "]";
        }
    }

    // 5) super() and initialization order across inheritance
    static class Animal {
        static {
            System.out.println("Animal: static init");
        }

        {
            System.out.println("Animal: instance init block");
        }

        final String type;

        Animal(String type) {
            System.out.println("Animal(type=" + type + ")");
            this.type = type;
        }
    }

    static class Dog extends Animal {
        static {
            System.out.println("Dog: static init");
        }

        {
            System.out.println("Dog: instance init block");
        }

        String name;

        Dog(String name) {
            super("Dog"); // must be first statement (or this())
            this.name = name;
            System.out.println("Dog(name=" + name + ")");
        }
    }

    // 6) Static/instance init order within a single class
    static class InitOrder {
        static int S1 = _02_Examples.log("InitOrder: static field S1");
        static {
            _02_Examples.log("InitOrder: static block");
        }

        int i1 = _02_Examples.log("InitOrder: instance field i1");
        {
            _02_Examples.log("InitOrder: instance block");
        }

        InitOrder() {
            System.out.println("InitOrder: constructor");
        }
    }

    // 7) Initialization order across inheritance
    static class Parent {
        static int PS1 = _02_Examples.log("Parent: static field");
        static {
            _02_Examples.log("Parent: static block");
        }

        int pi1 = _02_Examples.log("Parent: instance field");
        {
            _02_Examples.log("Parent: instance block");
        }

        Parent() {
            System.out.println("Parent: constructor");
        }
    }

    static class Child extends Parent {
        static int CS1 = _02_Examples.log("Child: static field");
        static {
            _02_Examples.log("Child: static block");
        }

        int ci1 = _02_Examples.log("Child: instance field");
        {
            _02_Examples.log("Child: instance block");
        }

        Child() {
            System.out.println("Child: constructor");
        }
    }

    // 8) Final fields and static finals
    static class FinalDemo {
        final int required; // must be assigned by end of constructor
        final int computed;

        static final int S_COMPILE_TIME = 7 * 6; // constant expression (inlined)
        static final int S_RUNTIME = compute();  // not a compile-time constant
        static final int S_LATE;                 // assigned in static block

        static {
            System.out.println("FinalDemo: static block");
            S_LATE = 123;
        }

        FinalDemo(int required) {
            this.required = required;
            this.computed = required * 2;
            System.out.println("FinalDemo: constructor");
        }

        static int compute() {
            System.out.println("FinalDemo.compute() for S_RUNTIME");
            return (int) (System.currentTimeMillis() % 100);
        }

        @Override
        public String toString() {
            return "FinalDemo(required=" + required + ", computed=" + computed + ")";
        }
    }

    // 9) Forward references (textual order matters for static fields)
    static class Forward {
        static int B = 2;
        static int A = B + 1; // B is 0 here (default) because B's initializer hasn't run yet
    }

    // 10) Varargs constructor
    static class VarargsBox {
        final String[] items;

        VarargsBox(String... items) {
            this.items = items; // could also clone to avoid external mutation
            System.out.println("VarargsBox constructed with " + items.length + " items: " + Arrays.toString(items));
        }
    }

    // 11) Constructors that throw exceptions
    static class Failable {
        Failable(boolean shouldFail) throws Exception {
            if (shouldFail) {
                throw new Exception("Constructor failed intentionally");
            }
            System.out.println("Failable: constructed successfully");
        }
    }

    // 12) Pitfall: calling overridable methods in constructors
    static class ParentInitPitfall {
        ParentInitPitfall() {
            System.out.println("ParentInitPitfall: ctor start");
            init(); // virtual call (dangerous)
            System.out.println("ParentInitPitfall: ctor end");
        }

        void init() {
            System.out.println("ParentInitPitfall: default init()");
        }
    }

    static class ChildInitPitfall extends ParentInitPitfall {
        String name = "ChildName"; // initialized after super() returns, before this ctor body

        ChildInitPitfall() {
            System.out.println("ChildInitPitfall: ctor; name=" + name);
        }

        @Override
        void init() {
            System.out.println("ChildInitPitfall.init() called; name=" + name); // likely null here
        }
    }

    // 13) 'this' escape during construction (anti-pattern)
    interface Listener {
        void onEvent(String msg);
    }

    static class EventBus {
        private final List<Listener> listeners = new ArrayList<>();

        void register(Listener l) {
            listeners.add(l);
        }

        void fire(String msg) {
            for (Listener l : listeners) {
                l.onEvent(msg);
            }
        }
    }

    static class ThisEscapeDemo implements Listener {
        private final String important;
        private static final EventBus BUS = new EventBus();

        ThisEscapeDemo() {
            BUS.register(this); // 'this' escapes before constructor finishes
            BUS.fire("Event before init complete");
            this.important = "OK"; // initialized too late for the first event
            BUS.fire("Event after init complete");
        }

        @Override
        public void onEvent(String msg) {
            System.out.println("ThisEscapeDemo.onEvent: " + msg + " (important=" + important + ")");
        }
    }

    // 14) Instance initializer blocks
    static class InstanceInitBlockDemo {
        final int value;

        {
            System.out.println("InstanceInitBlockDemo: instance initializer runs");
        }

        InstanceInitBlockDemo() {
            this(0); // this() must be first statement
            System.out.println("InstanceInitBlockDemo: no-arg body");
        }

        InstanceInitBlockDemo(int value) {
            this.value = value;
            System.out.println("InstanceInitBlockDemo: arg ctor; value=" + value);
        }
    }

    // 15) Static initializer block
    static class StaticInitBlockDemo {
        static final int VALUE;

        static {
            System.out.println("StaticInitBlockDemo: static block");
            VALUE = 42;
        }
    }

    // 16) Copy constructor & defensive copies
    static class Person {
        final String name;
        private final int[] scores;

        Person(String name, int[] scores) {
            this.name = name;
            this.scores = scores.clone(); // defensive copy
        }

        Person(Person other) {
            this(other.name, other.scores); // already clones in primary ctor
        }

        int[] getScores() {
            return scores.clone(); // defensive copy on access
        }

        @Override
        public String toString() {
            return "Person(name=" + name + ", scores=" + Arrays.toString(scores) + ")";
        }
    }

    // 17) Private constructors: utility and singleton
    static final class Math2 {
        private Math2() {
            throw new AssertionError("No instances");
        }

        static int add(int a, int b) {
            return a + b;
        }
    }

    static final class Singleton {
        private static final Singleton INSTANCE = new Singleton();

        private Singleton() {
        }

        static Singleton getInstance() {
            return INSTANCE;
        }
    }

    // 18) Static factory vs constructor
    static class Range {
        final int start, end;
        final boolean closed;

        private Range(int start, int end, boolean closed) {
            if (start > end) throw new IllegalArgumentException("start > end");
            this.start = start;
            this.end = end;
            this.closed = closed;
        }

        public static Range of(int start, int end) { // open interval (start, end)
            return new Range(start, end, false);
        }

        public static Range closed(int start, int end) { // closed interval [start, end]
            return new Range(start, end, true);
        }

        @Override
        public String toString() {
            return (closed ? "[" : "(") + start + "," + end + (closed ? "]" : ")");
        }
    }

    // 19) Enum constructors are implicitly private; one per constant construction
    enum Color {
        RED(255, 0, 0),
        GREEN(0, 255, 0),
        BLUE(0, 0, 255);

        private final int r, g, b;

        Color(int r, int g, int b) {
            System.out.println("Color enum ctor for " + name());
            this.r = r;
            this.g = g;
            this.b = b;
        }

        public String rgb() {
            return r + "," + g + "," + b;
        }
    }

    // 20) Double-brace initialization (creates anonymous subclass; retains reference to enclosing instance; generally avoid)
    static class DoubleBraceDemo {
        static void demonstrate() {
            List<String> list = new ArrayList<String>() {{
                add("a");
                add("b");
                System.out.println("Double-brace: instance initializer ran");
            }};
            System.out.println("List=" + list + ", class=" + list.getClass().getName());
        }
    }
}