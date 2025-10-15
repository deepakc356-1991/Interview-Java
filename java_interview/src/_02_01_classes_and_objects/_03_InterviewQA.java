package _02_01_classes_and_objects;

import java.lang.ref.Cleaner;
import java.lang.ref.WeakReference;
import java.util.*;

/*
Classes & Objects Interview Q&A (Basic → Intermediate → Advanced)
Use this file as a compact study + demo guide. Each topic has:
- Q&A in comments
- Minimal runnable demo code in main via Demo* classes

BASICS
Q: What is a class and an object?
A: A class is a blueprint; an object is a runtime instance of that blueprint.

Q: How are fields initialized?
A: Instance fields get default values (0, false, null). Local variables must be explicitly initialized.

Q: What is a constructor? Can you overload it?
A: Special method to initialize objects. Yes, you can overload with different parameter lists.

Q: What is 'this'?
A: Reference to the current object; used to disambiguate names and for constructor/method chaining.

Q: What is 'super'?
A: Reference to the immediate parent; used to call parent constructors/methods.

STATIC VS INSTANCE
Q: Difference between static and instance members?
A: Static belongs to the class (shared); instance belongs to each object.

INITIALIZATION ORDER
Q: What is the order of initialization?
A: Superclass static (once) → subclass static (once) → superclass instance → superclass constructor → subclass instance → subclass constructor.

EQUALITY
Q: Difference between '==' and equals?
A: '==' compares references (identity); equals compares content (equality) if overridden.

Q: Why override hashCode when overriding equals?
A: Contract: equal objects must have equal hashCodes (critical for hash-based collections).

IMMUTABILITY
Q: How to make a class immutable?
A: Make class final, fields private final, no setters, defensive copies for mutable inputs/outputs.

COPYING
Q: Copy constructor vs clone?
A: Copy constructor/factory preferred (clear, type-safe). clone is legacy (shallow by default, tricky).

OVERLOADING VS OVERRIDING
Q: Overloading vs overriding?
A: Overloading: same name, different params, resolved at compile time. Overriding: same signature in subclass, resolved at runtime (polymorphism).

COVARIANT RETURNS
Q: Can overridden methods change return types?
A: Yes, to a more specific (covariant) type.

STATIC METHOD HIDING
Q: Do static methods get overridden?
A: No, they are hidden. Resolution is based on reference type.

NESTED/INNER CLASSES
Q: Types of nested classes?
A: Static nested, member inner, local, anonymous. Inner classes capture outer instance.

OBJECT CLASS
Q: Common methods to override?
A: toString, equals, hashCode (sometimes clone with caution).

GC & CLEANUP
Q: Finalize vs Cleaner?
A: finalize is deprecated; use Cleaner or explicit close methods.

ACCESS MODIFIERS (package-private default)
Q: What are access levels?
A: public > protected > package-private (no modifier) > private.

RECORDS/SEALED (modern Java)
- Records model immutable data (boilerplate-free).
- Sealed classes restrict inheritance.
- Not included as code for compatibility; see comments where relevant.
*/

public class _03_InterviewQA {
    public static void main(String[] args) {
        System.out.println("=== Classes & Objects Interview Q&A Demos ===");
        DemoBasics.run();
        DemoStaticVsInstance.run();
        DemoInitOrder.run();
        DemoEqualityHashCode.run();
        DemoImmutability.run();
        DemoCopying.run();
        DemoOverloadOverride.run();
        DemoNestedClasses.run();
        DemoMethodHiding.run();
        DemoCleanerAndGC.run();
        System.out.println("=== End ===");
    }
}

/* ------------------------------ BASICS ------------------------------ */

final class DemoBasics {
    static void run() {
        System.out.println("-- Basics");
        // Default field values (instance fields only; local vars must be initialized)
        Defaults d = new Defaults();
        d.print();

        // Constructors, 'this' chaining, and encapsulation
        Person p1 = new Person();
        Person p2 = new Person("Alice", 30).setAge(31); // fluent chain using 'this'
        System.out.println(p1);
        System.out.println(p2);

        // Inheritance and 'super'
        Employee e = new Employee("Bob", 25, "E-101");
        System.out.println(e);
        System.out.println("Employee name via getter: " + e.getName());
    }
}

class Defaults {
    int i;          // 0
    boolean b;      // false
    Object obj;     // null
    void print() {
        System.out.println("Defaults: i=" + i + ", b=" + b + ", obj=" + obj);
    }
}

// Encapsulation: private fields + getters/setters; constructor overloading/chaining
class Person {
    private String name;
    private int age;

    // static state to show shared data (see DemoStaticVsInstance)
    private static int population;

    static {
        // Runs once when the class loads
        System.out.println("Person: static initializer");
    }
    {
        // Runs before each constructor
        System.out.println("Person: instance initializer");
    }

    public Person() {
        this("Unknown", 0);
    }
    public Person(String name, int age) {
        this.name = name;
        this.age = age;
        population++;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; } // could add validation
    public int getAge() { return age; }

    // Fluent (chainable) setter to demo 'this'
    public Person setAge(int age) { this.age = age; return this; }

    public static int getPopulation() { return population; }

    @Override
    public String toString() {
        return "Person{name='" + name + "', age=" + age + "}";
    }
}

// Inheritance demo with 'super'
class Employee extends Person {
    private final String employeeId;

    public Employee(String name, int age, String employeeId) {
        super(name, age); // call parent constructor
        this.employeeId = employeeId;
    }

    public String getEmployeeId() { return employeeId; }

    @Override
    public String toString() {
        return "Employee{employeeId='" + employeeId + "', base=" + super.toString() + "}";
    }
}

/* ------------------------ STATIC VS INSTANCE ------------------------ */

final class DemoStaticVsInstance {
    static void run() {
        System.out.println("-- Static vs Instance");
        int before = Person.getPopulation();
        Person a = new Person("A", 20);
        Person b = new Person("B", 22);
        System.out.println("Population before=" + before + ", after=" + Person.getPopulation());

        StaticVsInstance o1 = new StaticVsInstance();
        StaticVsInstance o2 = new StaticVsInstance();
        System.out.println("Static count=" + StaticVsInstance.staticCount
                + ", o1.instanceCount=" + o1.instanceCount
                + ", o2.instanceCount=" + o2.instanceCount);
    }
}

class StaticVsInstance {
    static int staticCount; // shared across all instances
    int instanceCount;      // per-object

    StaticVsInstance() {
        staticCount++;
        instanceCount++;
    }

    static void staticMethod() {
        // Cannot access 'instanceCount' here directly
        // System.out.println(instanceCount); // compile error
    }
}

/* ------------------------- INITIALIZATION ORDER ------------------------- */

final class DemoInitOrder {
    static void run() {
        System.out.println("-- Initialization Order");
        System.out.println("Creating first ChildInitOrder");
        new ChildInitOrder();
        System.out.println("Creating second ChildInitOrder (static blocks won't run again)");
        new ChildInitOrder();
    }
}

class BaseInitOrder {
    static int s = print("Base: static field");
    static { print("Base: static block"); }

    int i = print("Base: instance field");
    { print("Base: instance block"); }

    BaseInitOrder() { print("Base: constructor"); }

    static int print(String msg) {
        System.out.println(msg);
        return 1;
    }
}

class ChildInitOrder extends BaseInitOrder {
    static int s2 = print("Child: static field");
    static { print("Child: static block"); }

    int i2 = print("Child: instance field");
    { print("Child: instance block"); }

    ChildInitOrder() { print("Child: constructor"); }
}

/* ----------------------------- EQUALITY ----------------------------- */

final class DemoEqualityHashCode {
    static void run() {
        System.out.println("-- Equality and hashCode");
        String x = new String("hi");
        String y = new String("hi");
        System.out.println("'=='? " + (x == y) + ", equals? " + x.equals(y));

        Book b1 = new Book("ISBN-123", "Title A");
        Book b2 = new Book("ISBN-123", "Title B"); // same ISBN → equal per our definition
        Set<Book> set = new HashSet<>();
        set.add(b1);
        set.add(b2);
        System.out.println("Set size (should be 1 if equals/hashCode consistent): " + set.size());
        System.out.println("b1.hashCode == b2.hashCode? " + (b1.hashCode() == b2.hashCode()));
    }
}

class Book {
    private final String isbn;
    private final String title;

    Book(String isbn, String title) {
        this.isbn = Objects.requireNonNull(isbn);
        this.title = title;
    }

    // Equality based on business key (ISBN)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Book)) return false;
        Book other = (Book) o;
        return isbn.equals(other.isbn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isbn);
    }

    @Override
    public String toString() { return "Book{isbn='" + isbn + "', title='" + title + "'}"; }
}

/* ---------------------------- IMMUTABILITY ---------------------------- */

final class DemoImmutability {
    static void run() {
        System.out.println("-- Immutability");
        ImmutablePoint p = new ImmutablePoint(3, 4);
        System.out.println(p + ", moved: " + p.moveBy(2, -1));

        List<String> tags = new ArrayList<>(List.of("alpha", "beta"));
        ImmutableUser u = new ImmutableUser("Tom", tags);
        tags.add("gamma"); // external list change shouldn't affect u
        System.out.println("ImmutableUser: " + u);

        try {
            u.tags().add("should-fail"); // unmodifiable
        } catch (UnsupportedOperationException ex) {
            System.out.println("Tags are unmodifiable (defensive copy) ✔");
        }
    }
}

final class ImmutablePoint {
    private final int x;
    private final int y;
    ImmutablePoint(int x, int y) { this.x = x; this.y = y; }
    public int x() { return x; }
    public int y() { return y; }
    public ImmutablePoint moveBy(int dx, int dy) { return new ImmutablePoint(x + dx, y + dy); }
    @Override public String toString() { return "ImmutablePoint(" + x + "," + y + ")"; }
}

final class ImmutableUser {
    private final String name;
    private final List<String> tags; // Treat as immutable view

    ImmutableUser(String name, List<String> tags) {
        this.name = Objects.requireNonNull(name);
        // defensive copy + unmodifiable view
        this.tags = List.copyOf(Objects.requireNonNull(tags));
    }

    public String name() { return name; }
    public List<String> tags() { return tags; } // safe to expose (unmodifiable)

    @Override public String toString() { return "ImmutableUser{name='" + name + "', tags=" + tags + "}"; }
}

/* --------------------------- COPYING OBJECTS --------------------------- */

final class DemoCopying {
    static void run() {
        System.out.println("-- Copying Objects: copy constructor vs clone");
        Customer c1 = new Customer("Carl", new Address("NYC"));
        Customer copyCtor = new Customer(c1);      // deep via copy constructor
        Customer cloneCopy = c1.clone();           // deep via clone (here implemented safely)

        c1.name = "Changed";
        c1.address.city = "LA";

        System.out.println("Original: " + c1);
        System.out.println("CopyCtor: " + copyCtor);
        System.out.println("CloneCopy: " + cloneCopy);
    }
}

class Address {
    String city;
    Address(String city) { this.city = city; }
    Address(Address other) { this.city = other.city; }
    @Override public String toString() { return "Address{city='" + city + "'}"; }
}

class Customer implements Cloneable {
    String name;
    Address address;

    Customer(String name, Address address) {
        this.name = name;
        this.address = address;
    }

    // Copy constructor (preferred)
    Customer(Customer other) {
        this(other.name, new Address(other.address));
    }

    // Deep clone implementation (safe here)
    @Override
    protected Customer clone() {
        try {
            Customer copy = (Customer) super.clone();
            copy.address = new Address(this.address);
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override public String toString() {
        return "Customer{name='" + name + "', address=" + address + "}";
    }
}

/* -------------------- OVERLOADING, OVERRIDING, POLYMORPHISM -------------------- */

final class DemoOverloadOverride {
    static void run() {
        System.out.println("-- Overloading vs Overriding; Covariant returns");
        Shape s = new Shape();
        System.out.println("Circle area r=3: " + s.area(3));
        System.out.println("Rect area 3x4: " + s.area(3, 4));

        Animal a = new Dog();
        System.out.println("Polymorphic speak: " + a.speak());
        Animal baby = a.reproduce(); // actually Dog at runtime due to covariant override
        System.out.println("Covariant reproduce returns: " + baby.getClass().getSimpleName());
    }
}

class Shape {
    // Overloading: same name, different params
    double area(double radius) { return Math.PI * radius * radius; }
    double area(double w, double h) { return w * h; }
}

class Animal {
    String speak() { return "generic"; }
    Animal reproduce() { return new Animal(); } // may be overridden covariantly
}

class Dog extends Animal {
    @Override String speak() { return "woof"; }
    @Override Dog reproduce() { return new Dog(); } // covariant return
}

/* --------------------------- NESTED/INNER CLASSES --------------------------- */

final class DemoNestedClasses {
    static void run() {
        System.out.println("-- Nested and Inner Classes");
        Outer outer = new Outer(10);
        Outer.Inner inner = outer.new Inner();
        inner.print();

        Outer.Nested nested = new Outer.Nested();
        nested.print();

        outer.localAndAnonymous();
    }
}

// Outer class demonstrating all nested class types
class Outer {
    private int x;

    Outer(int x) { this.x = x; }

    // Member inner class (has reference to outer instance)
    class Inner {
        void print() { System.out.println("Inner sees x=" + x + " via Outer.this"); }
    }

    // Static nested class (no implicit outer reference)
    static class Nested {
        void print() { System.out.println("Static Nested has no outer instance"); }
    }

    void localAndAnonymous() {
        int local = 42; // effectively final for capture
        // Local class
        class Local {
            void show() {
                System.out.println("Local sees outer x=" + x + " and local=" + local);
            }
        }
        new Local().show();

        // Anonymous class
        Runnable r = new Runnable() {
            @Override public void run() {
                System.out.println("Anonymous sees outer x=" + x);
            }
        };
        r.run();
    }
}

/* --------------------------- STATIC METHOD HIDING --------------------------- */

final class DemoMethodHiding {
    static void run() {
        System.out.println("-- Static method hiding vs instance overriding");
        Parent p = new ChildHiding();
        System.out.println("Static call (hiding): " + p.who());          // Parent
        System.out.println("Instance call (override): " + p.nonStaticWho()); // ChildHiding

        // Field hiding demo
        ParentFields pf = new ChildFields();
        System.out.println("Field via parent ref: " + pf.v);             // 1
        System.out.println("Field via child ref: " + ((ChildFields) pf).v); // 2
    }
}

class Parent {
    static String who() { return "Parent"; }            // hidden
    String nonStaticWho() { return "Parent"; }          // overridden
}
class ChildHiding extends Parent {
    static String who() { return "Child"; }             // hiding
    @Override String nonStaticWho() { return "Child"; } // overriding
}

class ParentFields { int v = 1; }
class ChildFields extends ParentFields { int v = 2; }

/* --------------------------- CLEANER AND GC NOTES --------------------------- */

final class DemoCleanerAndGC {
    static void run() {
        System.out.println("-- Cleaner and GC (non-deterministic GC shown carefully)");
        ResourceHolder holder = new ResourceHolder();
        holder.close(); // deterministic cleanup

        // WeakReference demo (object may be collected when only weakly reachable)
        WeakReference<Object> ref = new WeakReference<>(new Object());
        System.gc();
        try { Thread.sleep(50); } catch (InterruptedException ignored) {}
        System.out.println("WeakReference cleared? " + (ref.get() == null));
    }
}

// Prefer explicit close() and/or Cleaner over finalize (finalize is deprecated)
class ResourceHolder {
    private static final Cleaner cleaner = Cleaner.create();

    // State to clean; must be static to avoid capturing ResourceHolder implicitly
    private static final class State implements Runnable {
        private boolean cleaned;
        @Override public void run() {
            cleaned = true;
            System.out.println("ResourceHolder: cleaned by Cleaner");
        }
    }

    private final Cleaner.Cleanable cleanable;

    ResourceHolder() {
        this.cleanable = cleaner.register(this, new State());
    }

    void close() {
        cleanable.clean(); // explicit cleanup
    }
}

/*
EXTRA NOTES (no code):
- Access modifiers: public, protected, (package-private), private.
- final class: cannot be extended. final method: cannot be overridden. final field: assign once.
- enums: type-safe constants with fields/behavior.
- records (Java 16+): concise immutable data carriers: record Point(int x, int y) {}
- sealed classes (Java 17+): restrict which classes can extend a class or implement an interface.
*/