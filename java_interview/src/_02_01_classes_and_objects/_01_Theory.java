package _02_01_classes_and_objects;

import java.util.Objects;

/*
Classes & Objects - Theory (Java)

Core ideas
- Class: blueprint that defines state (fields) and behavior (methods) for objects.
- Object: an instance of a class; created with new; lives on the heap; manipulated via references.
- State vs behavior:
  - State: values stored in fields (e.g., name, age).
  - Behavior: code in methods (e.g., celebrateBirthday()).
- Anatomy of a class:
  - Fields (instance and static), methods, constructors, initializer blocks, nested types, modifiers.
- Access control:
  - public: visible everywhere; protected: package + subclasses; package-private (no modifier): package only; private: within class.
- Modifiers:
  - final (cannot be reassigned/extended), static (belongs to class), abstract, etc.
- Constructors:
  - Initialize objects. If none provided, a default no-arg constructor exists.
  - Overloading: multiple constructors with different parameters.
  - this(...) can chain constructors; this refers to current instance.
- Static vs instance:
  - static fields/methods belong to the class and are shared; instance members belong to each object.
  - static initializer runs once on class load; instance initializer runs for each new object before constructor body.
- Encapsulation:
  - Keep fields private; expose behavior via methods; validate inputs; enforce invariants.
- Immutability:
  - Make fields final; no setters; defensive copies; thread-safe and simpler reasoning.
- Equality:
  - == compares references (same object); equals compares logical equality (override equals and hashCode consistently).
  - toString for human-readable representation.
- Parameters & variables:
  - Java is pass-by-value. Object references are passed by value (the reference is copied).
  - Fields have default values; local variables must be initialized before use.
- Composition:
  - Build complex objects by combining simpler objects (a Person has an Address).
- Nested classes:
  - static nested class: does not capture outer instance; inner class: has implicit reference to outer instance.
- Memory & GC:
  - Objects on heap; references on stack/heap; garbage collector frees unreachable objects. Avoid finalize.
- Conventions:
  - Class names: PascalCase; methods/fields: camelCase; constants: UPPER_SNAKE_CASE.

This file demonstrates these concepts with comments and runnable examples.
*/
public class _01_Theory {

    public static void main(String[] args) {
        System.out.println("=== Classes & Objects: Theory & Examples ===");

        // Static members belong to the class (no instance needed)
        System.out.println("Species: " + Person.SPECIES);
        System.out.println("Population (before creating persons): " + Person.populationCount());

        // Create objects (instances)
        Person p1 = new Person("Alice", 30);
        Person p2 = Person.of("Bob"); // static factory
        p2.setAge(18);
        Person p3 = new Person(); // default constructor -> "Unknown", 0
        p3.setName("Charlie");
        p3.setAge(25);

        System.out.println("Population (after creating persons): " + Person.populationCount());

        // Aliasing (multiple references to the same object)
        Person alias = p1;
        alias.setAge(31); // modifies p1, because alias and p1 reference the same object
        System.out.println("Aliasing affects original: " + p1);

        // Pass-by-value: object reference is copied; method can mutate the object but cannot rebind caller's variable
        haveBirthday(p2);           // mutates p2 (age++)
        tryToReassign(p2);          // attempts to reassign the parameter; caller unaffected
        System.out.println("After birthday and failed reassign, p2: " + p2);

        // Equality: == (reference) vs equals (logical)
        Person p4 = new Person("Alice", 31);
        System.out.println("p1 == p4? " + (p1 == p4));          // false (different objects)
        System.out.println("p1.equals(p4)? " + p1.equals(p4));  // true  (same name+age per equals implementation)

        // Composition and nested classes
        Person.Address addr = new Person.Address("Madrid", "Spain"); // static nested class
        p1.setAddress(addr);
        Person.Pet pet = p1.new Pet("Rex"); // inner class captures outer p1
        System.out.println(pet.introduce());
        System.out.println("p1 with address: " + p1);

        // Immutability example
        Point pt = new Point(3, 4);
        Point moved = pt.translate(1, -2); // returns a new Point; original unchanged
        System.out.println("Immutable Point: " + pt + " -> " + moved);

        // Default field values vs local variables
        Defaults def = new Defaults();
        System.out.println("Default field values: " + def);
        // int localX; // local variables must be initialized before use -> compilation error if used uninitialized

        // Arrays are objects
        int[] numbers = new int[3]; // default elements are 0
        System.out.println("Array length: " + numbers.length);

        // Null references
        Person maybeNull = null;
        System.out.println("maybeNull is null? " + (maybeNull == null));
        // maybeNull.getName(); // would throw NullPointerException

        // Package-private class (no modifier): accessible within the same package
        System.out.println("Package-private: " + PackagePrivateExample.hello());

        // Static-only utility pattern (private constructor)
        Person baby = PersonUtils.newborn("Dana");
        System.out.println("Newborn via utility: " + baby);

        // toString provides human-readable representation
        System.out.println("toString: " + p3);

        System.out.println("=== End ===");
    }

    // Demonstrates pass-by-value mutating the object's state
    static void haveBirthday(Person p) {
        p.celebrateBirthday();
    }

    // Demonstrates that reassigning the parameter doesn't affect the caller's reference
    static void tryToReassign(Person p) {
        p = new Person("Reassigned", 0);
        p.setAge(99); // modifies only the new object referenced by local parameter 'p'
    }
}

/*
Person demonstrates:
- Fields (state), methods (behavior), constructors
- Encapsulation (private fields + validation in setters)
- static vs instance members
- Constants (static final), final fields (id)
- Constructor chaining with this(...)
- equals, hashCode, toString
- Nested types: static nested class (Address), inner class (Pet)
- Instance initializer for shared setup across constructors
*/
class Person {

    // Constants (convention: UPPER_SNAKE_CASE)
    public static final String SPECIES = "Homo sapiens";
    private static final int MAX_AGE = 150;

    // Static fields (shared across all instances)
    private static int population; // demo only; not decremented when objects become unreachable
    private static long nextId;

    // Instance fields (state)
    private String name;
    private int age;
    private Address address; // composition
    private final long id;   // unique identifier per instance; final -> assigned once

    // Static initializer (runs once when the class is loaded)
    static {
        population = 0;
        nextId = 0L;
        // System.out.println("[static] Person class initialized"); // demo: side effects on class load
    }

    // Instance initializer (runs before any constructor body for each new instance)
    {
        id = ++nextId;
        population++;
    }

    // Constructors (overloaded)
    public Person() {
        this("Unknown", 0); // constructor chaining
    }

    public Person(String name) {
        this(name, 0);
    }

    public Person(String name, int age) {
        setName(name);
        setAge(age);
    }

    // Accessors with validation (encapsulation)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty())
            throw new IllegalArgumentException("name must not be null/blank");
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        if (age < 0 || age > MAX_AGE)
            throw new IllegalArgumentException("age must be between 0 and " + MAX_AGE);
        this.age = age;
    }

    public long getId() {
        return id;
    }

    public Address getAddress() {
        return address;
    }

    // Returns this for method-chaining demo (fluent style)
    public Person setAddress(Address address) {
        this.address = address;
        return this;
    }

    // Behavior (methods)
    public void celebrateBirthday() {
        this.age++;
    }

    // Overloaded methods (same name, different parameters)
    public String greet() {
        return "Hi, I'm " + name;
    }

    public String greet(String otherName) {
        return "Hi " + otherName + ", I'm " + name;
    }

    // Static members (belong to the class)
    public static int populationCount() {
        return population;
    }

    public static Person of(String name) { // static factory
        return new Person(name);
    }

    // Equality and representation
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;              // same reference
        if (!(o instanceof Person)) return false;
        Person person = (Person) o;
        // Logical equality (by content, not id)
        return age == person.age && Objects.equals(name, person.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, age);
    }

    @Override
    public String toString() {
        return "Person{id=" + id +
                ", name='" + name + '\'' +
                ", age=" + age +
                (address != null ? ", address=" + address : "") +
                '}';
    }

    // Static nested class: does not capture outer Person instance
    public static class Address {
        private final String city;
        private final String country;

        public Address(String city, String country) {
            if (city == null || country == null)
                throw new IllegalArgumentException("city/country must not be null");
            this.city = city;
            this.country = country;
        }

        public String getCity() { return city; }
        public String getCountry() { return country; }

        @Override
        public String toString() {
            return city + ", " + country;
        }
    }

    // Inner class: has implicit reference to outer instance (Person.this)
    public class Pet {
        private final String name;

        public Pet(String name) {
            this.name = Objects.requireNonNull(name, "name");
        }

        public String introduce() {
            return "Woof! I'm " + name + ", human is " + Person.this.name;
        }
    }
}

// Immutable value object example (no setters, final fields, methods return new instances)
final class Point {
    private final int x;
    private final int y;

    public Point(int x, int y) { this.x = x; this.y = y; }

    public int x() { return x; }
    public int y() { return y; }

    public Point translate(int dx, int dy) {
        return new Point(x + dx, y + dy);
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Point)) return false;
        Point p = (Point) o;
        return x == p.x && y == p.y;
    }

    @Override public int hashCode() { return 31 * x + y; }

    @Override public String toString() { return "(" + x + "," + y + ")"; }
}

// Default field values demo (fields get defaults; local variables must be initialized)
class Defaults {
    int i;       // 0
    boolean b;   // false
    String s;    // null
    double d;    // 0.0

    @Override
    public String toString() {
        return "Defaults{i=" + i + ", b=" + b + ", s=" + s + ", d=" + d + "}";
    }
}

// Package-private (no modifier): visible within this package only
class PackagePrivateExample {
    static String hello() {
        return "Hello from package-private class";
    }
}

// Static-only utility class pattern (non-instantiable)
final class PersonUtils {
    private PersonUtils() { } // prevent instantiation

    static Person newborn(String name) {
        return new Person(name, 0);
    }
}

/*
Notes:
- For simple immutable data carriers, consider records (Java 16+): record Point(int x, int y) {}
- Avoid finalize; prefer try-with-resources/AutoCloseable for managed resources.
- Transient/volatile are advanced field modifiers (serialization/concurrency) beyond this intro.
*/