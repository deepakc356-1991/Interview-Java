package _02_04_inheritance_and_composition;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
 Inheritance and Composition — Theory by Code

 Key ideas covered in this file:
 - Inheritance (is-a), method overriding, dynamic dispatch (runtime polymorphism)
 - super/this, constructor chaining, protected/private, final, static, covariant return types
 - Overloading vs overriding, upcasting/downcasting, instanceof, method hiding (static)
 - Abstract classes vs interfaces, multiple inheritance via interfaces and default methods
 - Default method conflict resolution (diamond via interfaces)
 - Liskov Substitution Principle (LSP) notes (in comments)
 - Composition (has-a), aggregation vs composition, delegation, Strategy and Decorator patterns
 - Favor composition over inheritance (in comments and examples)
*/
public class _01_Theory {
    public static void main(String[] args) {
        // Inheritance: "Dog is an Animal" (IS-A)
        Food kibble = new Food("kibble");
        Animal a1 = new Dog("Rex", "Labrador");
        Animal a2 = new Cat("Mittens");

        // Dynamic dispatch: the most specific overridden method runs at runtime
        a1.speak(); // Dog.speak
        a2.speak(); // Cat.speak

        // Common behavior inherited from Animal
        a1.live();
        a1.eat(kibble);

        // Upcasting is implicit (Dog -> Animal). Downcasting needs checks.
        Animal upcast = new Dog("Buddy", "Beagle");
        if (upcast instanceof Dog) {
            Dog d = (Dog) upcast; // safe downcast after instanceof
            d.fetch("ball");
        }

        // Covariant return: subclass narrows the return type
        Animal someBaby = a1.reproduce(); // returns Animal (compile-time type)
        Dog puppy = ((Dog) a1).reproduce(); // returns Dog (runtime + cast)

        // Static methods are hidden, not overridden (resolved at compile-time)
        System.out.println("Animal.kingdom(): " + Animal.kingdom());
        System.out.println("Dog.kingdom(): " + Dog.kingdom());

        // Interfaces and default methods (multiple inheritance of type/behavior)
        Dog rex = (Dog) a1;
        rex.play();
        rex.cuddle(); // default method from Pet
        System.out.println("Default conflict resolved greet(): " + rex.greet());

        // Abstract classes: cannot instantiate; define contracts + shared code
        Shape s1 = new Circle(2.0);
        Shape s2 = new Rectangle(2.0, 3.0);
        s1.describe();
        s2.describe();

        // Favor composition: Car HAS-A Engine, delegates to it
        Car car = new Car();
        car.start();
        car.stop();

        // Aggregation vs Composition:
        // Aggregation (weak ownership): Team has Players that can outlive the Team
        Player p1 = new Player("Alice");
        Team team = new Team("Tigers");
        team.addMember(p1);
        System.out.println("Team members: " + team.getMembersView());
        team.clear(); // removing players from team does NOT destroy players
        System.out.println("Player still exists after team cleared: " + p1);

        // Composition (strong ownership): House creates and owns rooms
        House house = new House("Kitchen", "Bedroom");
        System.out.println("House rooms: " + house.getRoomsView());
        house.demolish(); // destroys its rooms (ownership)
        System.out.println("After demolish rooms: " + house.getRoomsView());

        // Strategy pattern via composition: behavior can be swapped at runtime
        Printer printer = new Printer(new PlainTextFormatter());
        printer.print("Hello Strategy");
        printer.setFormatter(new UppercaseFormatter());
        printer.print("Hello Strategy");

        // Decorator pattern via composition: wrap to add responsibilities
        TextSource source = new StringText("decorated message");
        TextSource decorated = new TimestampText(new BracketsText(source));
        System.out.println("Decorated: " + decorated.text());

        // Note on LSP (conceptual): functions that accept Animal should work with any subclass
        // without strengthening preconditions or weakening postconditions (see comments below).
    }
}

/* ============================= Inheritance ============================= */

class Animal {
    private final String name;  // prefer private fields for encapsulation
    protected int age;          // protected: visible to subclasses (and same package)
    // Note: protected can leak encapsulation across packages via subclasses.

    Animal(String name) {
        this.name = name;
    }

    public String getName() { return name; }

    public void speak() {
        System.out.println(this + " makes a generic animal sound.");
    }

    // final: cannot be overridden; good for invariants and template skeletons
    public final void live() {
        System.out.println(this + " is alive.");
    }

    // Demonstrates covariant return. Subclasses may return a subtype.
    public Animal reproduce() {
        System.out.println(this + " reproduces (Animal version).");
        return new Animal("Baby of " + getName());
    }

    // Overloading: same name, different parameters
    public void eat(Food food) {
        System.out.println(this + " eats " + food.name());
    }

    // static methods are hidden (not polymorphic)
    static String kingdom() { return "Animalia"; }

    // Hook usable by subclasses
    protected void protectedHook() {
        System.out.println("Animal protected hook for subclasses.");
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + name + ")";
    }
}

// Pet interface: multiple inheritance of type allowed
interface Pet {
    void play();

    default void cuddle() { // default method: behavior mix-in
        System.out.println("Pet cuddles.");
    }

    static Pet of(Runnable r) { // static factory
        return new Pet() {
            @Override public void play() { r.run(); }
            @Override public String toString() { return "LambdaPet"; }
        };
    }
}

// Two interfaces with same default -> diamond via interfaces
interface WithDefaultA {
    default String greet() { return "Hello from A"; }
}
interface WithDefaultB {
    default String greet() { return "Hello from B"; }
}

class Dog extends Animal implements Pet, WithDefaultA, WithDefaultB {
    private final String breed;

    Dog(String name, String breed) {
        super(name);     // super: constructor chaining to base class
        this.breed = breed;
        this.age = 1;    // protected field inherited; careful with encapsulation
    }

    @Override
    public void speak() { // overriding: same signature, runtime dispatch
        // Optional: call super to keep base behavior
        // super.speak();
        System.out.println(this + " barks!");
    }

    // Overloading vs overriding: different parameter list -> overload
    public void speak(int times) {
        for (int i = 0; i < times; i++) speak();
    }

    // Covariant return type: returns Dog (subtype of Animal)
    @Override
    public Dog reproduce() {
        System.out.println(this + " reproduces (Dog version).");
        return new Dog("Puppy of " + getName(), this.breed);
    }

    // Static method hiding (not overriding)
    static String kingdom() { return "Canidae (static hidden)"; }

    public void fetch(String item) {
        System.out.println(this + " fetches a " + item + ".");
    }

    // Default method conflict resolution: must override and choose
    @Override
    public String greet() {
        return WithDefaultA.super.greet() + " + " + WithDefaultB.super.greet();
    }

    @Override
    public void play() {
        System.out.println(this + " plays fetch.");
    }

    @Override
    public String toString() {
        return super.toString() + "[breed=" + breed + "]";
    }
}

class Cat extends Animal implements Pet {
    Cat(String name) {
        super(name);
    }

    @Override
    public void speak() {
        System.out.println(this + " meows.");
    }

    @Override
    public void play() {
        System.out.println(this + " plays with a string.");
    }
}

/*
 LSP note:
 - A function expecting Animal should not break when given Dog or Cat.
 - Subclasses should not strengthen preconditions (require more) nor weaken postconditions (promise less).
 - For example, overriding eat(Food) to only accept "meat" in a subclass would violate LSP if callers pass any Food.
*/

/* ============================= Abstract classes ============================= */

abstract class Shape {
    public abstract double area();
    public abstract double perimeter();

    // Template method: final algorithm using abstract steps
    public final void describe() {
        System.out.println(this + " area=" + area() + ", perimeter=" + perimeter());
    }
}

class Circle extends Shape {
    private final double r;
    Circle(double r) { this.r = r; }
    @Override public double area() { return Math.PI * r * r; }
    @Override public double perimeter() { return 2 * Math.PI * r; }
    @Override public String toString() { return "Circle(r=" + r + ")"; }
}

class Rectangle extends Shape {
    private final double w, h;
    Rectangle(double w, double h) { this.w = w; this.h = h; }
    @Override public double area() { return w * h; }
    @Override public double perimeter() { return 2 * (w + h); }
    @Override public String toString() { return "Rectangle(w=" + w + ", h=" + h + ")"; }
}

/* ============================= Composition ============================= */

// Favor composition: Car HAS-A Engine and delegates; do not subclass Engine.
final class Car {
    private final Engine engine = new Engine(); // strong ownership
    public void start() { engine.start(); }
    public void stop() { engine.stop(); }
    // Encapsulation: we don't expose the Engine reference publicly.
    @Override public String toString() { return "Car()"; }
}

class Engine {
    private boolean running;
    public void start() {
        if (!running) {
            running = true;
            System.out.println("Engine started.");
        }
    }
    public void stop() {
        if (running) {
            running = false;
            System.out.println("Engine stopped.");
        }
    }
}

/* ============================= Aggregation vs Composition ============================= */

// Aggregation: Team has Players, but does not own their lifecycle
class Team {
    private final String name;
    private final List<Player> members = new ArrayList<>();
    Team(String name) { this.name = name; }
    public void addMember(Player p) { members.add(p); }
    public List<Player> getMembersView() { return Collections.unmodifiableList(members); }
    public void clear() { members.clear(); }
    @Override public String toString() { return "Team(" + name + ")"; }
}

class Player {
    private final String name;
    Player(String name) { this.name = name; }
    public String name() { return name; }
    @Override public String toString() { return "Player(" + name + ")"; }
}

// Composition: House owns Rooms; Rooms should not outlive House.
// Note: Java cannot enforce this strictly; the design and usage enforce it.
// We keep Room package-private and construct Rooms only inside House.
class House {
    private final List<Room> rooms = new ArrayList<>();
    House(String... roomNames) {
        for (String rn : roomNames) {
            rooms.add(new Room(rn)); // created and owned here
        }
    }
    public List<Room> getRoomsView() { return Collections.unmodifiableList(rooms); }
    public void demolish() { rooms.clear(); } // implies rooms are "gone" with the house
    @Override public String toString() { return "House(" + rooms.size() + " rooms)"; }
}

class Room {
    private final String name;
    Room(String name) { this.name = name; }
    @Override public String toString() { return "Room(" + name + ")"; }
}

/* ============================= Strategy (delegation) ============================= */

interface Formatter { String format(String input); }

class PlainTextFormatter implements Formatter {
    @Override public String format(String input) { return input; }
}

class UppercaseFormatter implements Formatter {
    @Override public String format(String input) { return input.toUpperCase(); }
}

class Printer {
    private Formatter formatter; // composed behavior (Strategy)
    Printer(Formatter formatter) { this.formatter = formatter; }
    public void setFormatter(Formatter formatter) { this.formatter = formatter; }
    public void print(String input) { System.out.println(formatter.format(input)); }
}

/* ============================= Decorator (composition) ============================= */

interface TextSource { String text(); }

class StringText implements TextSource {
    private final String value;
    StringText(String value) { this.value = value; }
    @Override public String text() { return value; }
}

abstract class TextDecorator implements TextSource {
    protected final TextSource inner;
    protected TextDecorator(TextSource inner) { this.inner = inner; }
}

class TimestampText extends TextDecorator {
    TimestampText(TextSource inner) { super(inner); }
    @Override public String text() { return "[" + Instant.now() + "] " + inner.text(); }
}

class BracketsText extends TextDecorator {
    BracketsText(TextSource inner) { super(inner); }
    @Override public String text() { return "[" + inner.text() + "]"; }
}

/* ============================= Misc helpers ============================= */

class Food {
    private final String name;
    Food(String name) { this.name = name; }
    public String name() { return name; }
}

/*
 When to prefer inheritance vs composition:
 - Use inheritance when you have a strict IS-A relationship and want polymorphism on a stable base API.
 - Use composition to reuse behavior, swap strategies at runtime, avoid tight coupling to base class internals,
   and avoid brittle hierarchies. "Favor composition over inheritance".

 Additional notes:
 - final classes/methods/fields improve safety and reasoning.
 - equals/hashCode and inheritance are tricky (symmetry, transitivity); prefer composition or use sealed classes.
 - Avoid calling overridable methods from constructors; can observe partially constructed state.
 - Protected increases coupling; prefer private + accessors.
 - Multiple inheritance of state is not allowed in Java; multiple inheritance of type/behavior via interfaces is allowed.
*/