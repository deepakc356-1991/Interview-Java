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
        // Inheritance: "Dog1 is an Animal1" (IS-A)
        Food kibble = new Food("kibble");
        Animal1 a1 = new Dog1("Rex", "Labrador");
        Animal1 a2 = new Cat1("Mittens");

        // Dynamic dispatch: the most specific overridden method runs at runtime
        a1.speak(); // Dog1.speak
        a2.speak(); // Cat1.speak

        // Common behavior inherited from Animal1
        a1.live();
        a1.eat(kibble);

        // Upcasting is implicit (Dog1 -> Animal1). Downcasting needs checks.
        Animal1 upcast = new Dog1("Buddy", "Beagle");
        if (upcast instanceof Dog1) {
            Dog1 d = (Dog1) upcast; // safe downcast after instanceof
            d.fetch("ball");
        }

        // Covariant return: subclass narrows the return type
        Animal1 someBaby = a1.reproduce(); // returns Animal1 (compile-time type)
        Dog1 puppy = ((Dog1) a1).reproduce(); // returns Dog1 (runtime + cast)

        // Static methods are hidden, not overridden (resolved at compile-time)
        System.out.println("Animal1.kingdom(): " + Animal1.kingdom());
        System.out.println("Dog1.kingdom(): " + Dog1.kingdom());

        // Interfaces and default methods (multiple inheritance of type/behavior)
        Dog1 rex = (Dog1) a1;
        rex.play();
        rex.cuddle(); // default method from Pet
        System.out.println("Default conflict resolved greet(): " + rex.greet());

        // Abstract classes: cannot instantiate; define contracts + shared code
        Shape1 s1 = new Circle1(2.0);
        Shape1 s2 = new Rectangle1(2.0, 3.0);
        s1.describe();
        s2.describe();

        // Favor composition: Car1 HAS-A Engine1, delegates to it
        Car1 car = new Car1();
        car.start();
        car.stop();

        // Aggregation vs Composition:
        // Aggregation (weak ownership): Team1 has Player1s that can outlive the Team1
        Player1 p1 = new Player1("Alice");
        Team1 team = new Team1("Tigers");
        team.addMember(p1);
        System.out.println("Team1 members: " + team.getMembersView());
        team.clear(); // removing players from team does NOT destroy players
        System.out.println("Player1 still exists after team cleared: " + p1);

        // Composition (strong ownership): House1 creates and owns rooms
        House1 house = new House1("Kitchen", "Bedroom");
        System.out.println("House1 rooms: " + house.getRoomsView());
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

        // Note on LSP (conceptual): functions that accept Animal1 should work with any subclass
        // without strengthening preconditions or weakening postconditions (see comments below).
    }
}

/* ============================= Inheritance ============================= */

class Animal1 {
    private final String name;  // prefer private fields for encapsulation
    protected int age;          // protected: visible to subclasses (and same package)
    // Note: protected can leak encapsulation across packages via subclasses.

    Animal1(String name) {
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
    public Animal1 reproduce() {
        System.out.println(this + " reproduces (Animal1 version).");
        return new Animal1("Baby of " + getName());
    }

    // Overloading: same name, different parameters
    public void eat(Food food) {
        System.out.println(this + " eats " + food.name());
    }

    // static methods are hidden (not polymorphic)
    static String kingdom() { return "Animal1ia"; }

    // Hook usable by subclasses
    protected void protectedHook() {
        System.out.println("Animal1 protected hook for subclasses.");
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

class Dog1 extends Animal1 implements Pet, WithDefaultA, WithDefaultB {
    private final String breed;

    Dog1(String name, String breed) {
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

    // Covariant return type: returns Dog1 (subtype of Animal1)
    @Override
    public Dog1 reproduce() {
        System.out.println(this + " reproduces (Dog1 version).");
        return new Dog1("Puppy of " + getName(), this.breed);
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

class Cat1 extends Animal1 implements Pet {
    Cat1(String name) {
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
 - A function expecting Animal1 should not break when given Dog1 or Cat1.
 - Subclasses should not strengthen preconditions (require more) nor weaken postconditions (promise less).
 - For example, overriding eat(Food) to only accept "meat" in a subclass would violate LSP if callers pass any Food.
*/

/* ============================= Abstract classes ============================= */

abstract class Shape1 {
    public abstract double area();
    public abstract double perimeter();

    // Template method: final algorithm using abstract steps
    public final void describe() {
        System.out.println(this + " area=" + area() + ", perimeter=" + perimeter());
    }
}

class Circle1 extends Shape1 {
    private final double r;
    Circle1(double r) { this.r = r; }
    @Override public double area() { return Math.PI * r * r; }
    @Override public double perimeter() { return 2 * Math.PI * r; }
    @Override public String toString() { return "Circle1(r=" + r + ")"; }
}

class Rectangle1 extends Shape1 {
    private final double w, h;
    Rectangle1(double w, double h) { this.w = w; this.h = h; }
    @Override public double area() { return w * h; }
    @Override public double perimeter() { return 2 * (w + h); }
    @Override public String toString() { return "Rectangle1(w=" + w + ", h=" + h + ")"; }
}

/* ============================= Composition ============================= */

// Favor composition: Car1 HAS-A Engine1 and delegates; do not subclass Engine1.
final class Car1 {
    private final Engine1 engine = new Engine1(); // strong ownership
    public void start() { engine.start(); }
    public void stop() { engine.stop(); }
    // Encapsulation: we don't expose the Engine1 reference publicly.
    @Override public String toString() { return "Car1()"; }
}

class Engine1 {
    private boolean running;
    public void start() {
        if (!running) {
            running = true;
            System.out.println("Engine1 started.");
        }
    }
    public void stop() {
        if (running) {
            running = false;
            System.out.println("Engine1 stopped.");
        }
    }
}

/* ============================= Aggregation vs Composition ============================= */

// Aggregation: Team1 has Player1s, but does not own their lifecycle
class Team1 {
    private final String name;
    private final List<Player1> members = new ArrayList<>();
    Team1(String name) { this.name = name; }
    public void addMember(Player1 p) { members.add(p); }
    public List<Player1> getMembersView() { return Collections.unmodifiableList(members); }
    public void clear() { members.clear(); }
    @Override public String toString() { return "Team1(" + name + ")"; }
}

class Player1 {
    private final String name;
    Player1(String name) { this.name = name; }
    public String name() { return name; }
    @Override public String toString() { return "Player1(" + name + ")"; }
}

// Composition: House1 owns Rooms; Rooms should not outlive House1.
// Note: Java cannot enforce this strictly; the design and usage enforce it.
// We keep Room package-private and construct Rooms only inside House1.
class House1 {
    private final List<Room> rooms = new ArrayList<>();
    House1(String... roomNames) {
        for (String rn : roomNames) {
            rooms.add(new Room(rn)); // created and owned here
        }
    }
    public List<Room> getRoomsView() { return Collections.unmodifiableList(rooms); }
    public void demolish() { rooms.clear(); } // implies rooms are "gone" with the house
    @Override public String toString() { return "House1(" + rooms.size() + " rooms)"; }
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