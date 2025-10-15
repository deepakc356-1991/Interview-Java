package _02_06_abstract_classes_and_interfaces;

/*
Abstract Classes & Interfaces (Java theory + runnable examples)
Note: This file uses Java 17 features (sealed types, interface private methods, pattern matching for instanceof).
If you compile with an older JDK, comment those parts as indicated in comments.

Concepts at a glance:
- Abstraction: Expose what-to-do, hide how-to-do.
- Abstract class (is-a base type with shared state/behavior)
  - Can have state (fields), constructors, non-abstract methods, abstract methods.
  - One class can extend at most one class (abstract or concrete).
  - May implement interfaces. If it does not implement all interface methods, it remains abstract.
  - Cannot be final (abstract types must be extensible).
  - Abstract methods cannot be private, static, or final.
  - Choose abstract class when you need shared state, partial implementation, or versioning with protected members.
- Interface (is-a capability/contract)
  - Multiple inheritance of type allowed (a class can implement many interfaces).
  - Members:
    - Fields: implicitly public static final; must be initialized.
    - Methods:
      - abstract (implicitly public)
      - default (since Java 8): provides a body; can be inherited/overridden.
      - static (since Java 8): utility methods; not inherited by implementing classes (call via InterfaceName.m()).
      - private instance/static (since Java 9): helpers for default/static methods within the interface.
  - Cannot have constructors; cannot maintain instance state.
  - Good for API contracts and composition-based design. Eases future refactoring and decoupling.
- Multiple inheritance and the diamond:
  - If a class inherits the same default method from multiple interfaces, you must override and resolve explicitly.
  - "Class wins" rule: if a superclass provides a concrete method, it overrides any interface default.
  - "Most specific" rule: if one interface extends another and both supply a default for the same method, the subinterface default is chosen.
- Functional interfaces (SAM)
  - Exactly one abstract method (Object methods don’t count; default/static don’t count).
  - Use @FunctionalInterface to get compiler checks.
  - Basis for lambdas and method references.
- Marker interfaces
  - Contain no methods (e.g., java.io.Serializable). Used for tagging and framework signaling.
- Interface evolution
  - Adding abstract methods breaks implementers.
  - Adding default methods is usually source/binary compatible.
  - Static methods on interfaces group helpers next to the contract.
- Sealed types (Java 17+)
  - sealed interface/class restricts which classes can implement/extend it (permits).
  - Helps model closed hierarchies explicitly in the type system.
- Best practices
  - Program to interfaces; hide implementations.
  - Prefer composition (interfaces) over inheritance when possible.
  - Use abstract classes when sharing state/implementation/template methods is valuable.
  - Avoid the "constant interface" anti-pattern; prefer a dedicated class or enum for constants.

Run the main method to see examples printed.
*/
public class _01_Theory {

    public static void main(String[] args) {
        System.out.println("=== Abstract Classes & Interfaces: demo ===");

        // Programming to the interface
        Animal dog = new Dog("Rex");
        Animal cat = new Cat("Misty");

        dog.speak();               // polymorphism
        cat.speak();

        dog.live();                // default method using a private helper inside Animal
        cat.live();

        // Abstract class with shared state and template method
        AbstractAnimal wolf = new Wolf("Grey Wind");
        wolf.feed();               // concrete method from abstract class
        wolf.sleep();              // concrete method from abstract class
        wolf.dailyRoutine();       // template method (final) calling a mix of abstract/concrete

        // Interface static methods (not inherited)
        System.out.println("Animal.parseSpecies('canine') = " + Animal.parseSpecies("canine"));
        // Dog.parseSpecies("canine"); // Compile error: static methods in interfaces are not inherited
        System.out.println("MAX_AGE from Animal = " + Animal.MAX_AGE);

        // Default method conflict and resolution
        System.out.println("--- Default method conflict resolution ---");
        GreeterA g1 = new BothGreeter();
        g1.greet();

        // "Class wins" rule: base class method overrides interface defaults automatically
        System.out.println("--- Class wins over interface defaults ---");
        ClassWinsGreeter g2 = new ClassWinsGreeter();
        g2.greet();

        // "Most specific" rule with interface inheritance
        System.out.println("--- Most specific default method (Child over Parent) ---");
        SpecificGreeter sg = new SpecificGreeter();
        sg.greet();

        // Functional interface + lambda
        System.out.println("--- Functional interface + lambda ---");
        Transformer<String, Integer> length = s -> s.length();
        System.out.println("len('abc') = " + length.apply("abc"));
        Transformer<String, String> toUpper = s -> s.toUpperCase();
        System.out.println("andThen -> length of upper('hello') = " + toUpper.andThen(Transformer.identity()).andThen(String::length).apply("hello"));

        // Anonymous class implementing an interface
        System.out.println("--- Anonymous class ---");
        Animal anonymous = new Animal() {
            @Override public String species() { return "AnonymousSpecies"; }
            @Override public void speak() { System.out.println("Anonymous speaks"); }
        };
        anonymous.speak();
        anonymous.live();

        // Marker interface demonstration
        if (dog instanceof Taggable) {
            System.out.println("Dog is Taggable (marker interface).");
        }

        // Sealed types: only permitted classes can implement Pet
        Pet p = dog; // Dog is a permitted implementor of Pet
        System.out.println("Pet kind = " + p.kind());

        // Pattern matching for instanceof (Java 16+)
        if (wolf instanceof Animal asAnimal) {
            asAnimal.speak();
        }

        // Interface evolution with default method
        Evolvable e = new Evolvable() {}; // empty anonymous implementor
        e.newOperation();                  // works due to default method (API evolution-friendly)

        // Abstract class instantiation via anonymous subclass (cannot instantiate directly)
        AbstractAnimal anonAbstract = new AbstractAnimal("Shadow") {
            @Override public String species() { return "AnonymousAbstract"; }
            @Override public void speak() { System.out.println(name + " whispers"); }
        };
        anonAbstract.speak();
    }
}

/* =========================
   Interfaces (contracts)
   ========================= */

interface Animal {
    // Interface fields are implicitly public static final
    int MAX_AGE = 200;
    String KINGDOM = "Animalia";

    // Abstract methods are implicitly public abstract
    String species();
    void speak();

    // Default method (since Java 8): provides behavior
    default void live() {
        System.out.println(formatName() + " lives in " + KINGDOM);
    }

    // Static method (since Java 8): utilities colocated with the contract
    static String parseSpecies(String raw) {
        return raw == null ? "UNKNOWN" : raw.trim().toUpperCase();
    }

    // Private method (since Java 9): helper for default/static methods in this interface
    private String formatName() {
        return "[" + species() + "]";
    }
}

// Marker interface (no members) often used as a tag for frameworks or policies
interface Taggable {}

/*
Default method conflicts:
- If a class implements two interfaces that supply the same default method signature, the class must override it.
- You can call a specific interface default via InterfaceName.super.method().
*/
interface GreeterA {
    default void greet() { System.out.println("GreeterA.greet()"); }
}

interface GreeterB {
    default void greet() { System.out.println("GreeterB.greet()"); }
}

// Must resolve the ambiguity explicitly
class BothGreeter implements GreeterA, GreeterB {
    @Override
    public void greet() {
        GreeterA.super.greet(); // choose or combine
        GreeterB.super.greet();
        System.out.println("BothGreeter resolved the conflict.");
    }
}

/*
"Class wins" rule:
- If a superclass provides a concrete method, it overrides any interface default of the same signature.
*/
class BaseGreeter {
    public void greet() { System.out.println("BaseGreeter.greet() (class wins)"); }
}
class ClassWinsGreeter extends BaseGreeter implements GreeterA, GreeterB {
    // No need to override; BaseGreeter.greet() wins over interface defaults
}

/*
"Most specific" default:
- If an interface extends another and both provide a default implementation of the same method,
  the subinterface's default is chosen automatically.
*/
interface ParentGreeter {
    default void greet() { System.out.println("ParentGreeter.greet()"); }
}
interface ChildGreeter extends ParentGreeter {
    @Override default void greet() { System.out.println("ChildGreeter.greet()"); }
}
class SpecificGreeter implements ChildGreeter {
    // Inherits ChildGreeter.greet() (more specific than ParentGreeter)
}

/* =========================
   Functional interfaces
   ========================= */

// Exactly one abstract method (SAM). Default/static/Object methods do not count toward the single abstract method.
@FunctionalInterface
interface Transformer<T, R> {
    R apply(T t);

    default <U> Transformer<T, U> andThen(Transformer<? super R, ? extends U> next) {
        return t -> next.apply(apply(t));
    }

    static <T> Transformer<T, T> identity() {
        return t -> t;
    }
}

/* =========================
   Abstract classes (partial implementations)
   ========================= */

/*
Abstract classes:
- Can define state, constructors, concrete and abstract methods.
- Useful for code reuse, template methods, and sharing invariants.
- Cannot be final; abstract methods cannot be private, static, or final.
*/
abstract class AbstractAnimal implements Animal {
    protected final String name; // shared state

    protected AbstractAnimal(String name) { // constructors allowed
        this.name = name;
    }

    // Keep abstract to force subclasses to supply details
    @Override public abstract String species();
    @Override public abstract void speak();

    // Concrete method shared by all subclasses
    public void feed() {
        System.out.println(name + " eats.");
    }

    public void sleep() {
        System.out.println(name + " sleeps.");
    }

    // Template Method pattern: final to prevent altering the sequence
    public final void dailyRoutine() {
        speak();
        feed();
        sleep();
    }

    // Factory method on abstract class (common pattern)
    public static AbstractAnimal ofDog(String name) {
        return new Dog(name);
    }
}

/* =========================
   Sealed interfaces (Java 17+)
   ========================= */

/*
Sealed interface restricts the set of permitted implementors.
This models a closed hierarchy and helps exhaustiveness in pattern matching (switch pattern matching, etc.).
*/
sealed interface Pet permits Dog, Cat {
    String kind();
}

/* =========================
   Concrete classes
   ========================= */

final class Dog extends AbstractAnimal implements Pet, Taggable {
    Dog(String name) { super(name); }

    @Override public String species() { return "Dog"; }
    @Override public void speak() { System.out.println(name + " says: woof!"); }
    @Override public String kind() { return "Canine"; }
}

final class Cat extends AbstractAnimal implements Pet {
    Cat(String name) { super(name); }

    @Override public String species() { return "Cat"; }
    @Override public void speak() { System.out.println(name + " says: meow!"); }
    @Override public String kind() { return "Feline"; }
}

final class Wolf extends AbstractAnimal {
    Wolf(String name) { super(name); }

    @Override public String species() { return "Wolf"; }
    @Override public void speak() { System.out.println(name + " howls: awoo!"); }
}

/* =========================
   Interface evolution example
   ========================= */

/*
Adding new abstract methods to a published interface breaks implementers.
Adding default methods is evolution-friendly (provides fallback behavior).
*/
interface Evolvable {
    default void newOperation() {
        System.out.println("Evolvable.newOperation() default behavior (safe API evolution).");
    }
}

/*
Notes and pitfalls:
- Interface fields are constants (public static final); avoid putting mutable objects as interface constants (constant-interface anti-pattern).
- Static interface methods are not inherited; call them via InterfaceName.method().
- A concrete class cannot contain abstract methods; an abstract class can.
- A method cannot be both abstract and final/static/private.
- Interfaces cannot be instantiated; use implementing classes/anonymous classes/lambdas (for functional interfaces).
- A class: "extends OneClass" and "implements I1, I2, ..." only (single inheritance of classes, multiple inheritance of interfaces).
- Interface methods cannot be protected or package-private. They are implicitly public (or private for helpers in Java 9+).
*/