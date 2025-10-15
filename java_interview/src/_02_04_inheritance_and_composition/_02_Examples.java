package _02_04_inheritance_and_composition;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/*
  Inheritance & Composition examples in one file.
  Note: No public top-level classes so the file can be named arbitrarily (e.g., "2) _02_Examples.java").
*/
class _02_Examples {
    public static void main(String[] args) {
        BasicInheritance.demo();
        ConstructorChaining.demo();
        Polymorphism.demo();
        AbstractVsInterface.demo();
        CompositionHasA.demo();
        DelegationStrategy.demo();
        AggregationVsComposition.demo();
        InheritanceVsCompositionPitfall.demo();
        ImmutabilityWithComposition.demo();
        InterfaceDefaultMethodConflict.demo();
    }
}

/* ---------- Inheritance: base, overrides, super ---------- */
class BasicInheritance {
    static void demo() {
        System.out.println("\n-- Basic Inheritance --");
        Dog d = new Dog("Rex");
        Cat c = new Cat("Mimi");

        System.out.println(d + " says " + d.speak());
        System.out.println(c + " says " + c.speak());

        d.breathe(); // final method from Animal (cannot be overridden)
        c.breathe();

        d.fetch(); // subclass-specific API
        System.out.println("Dog.speak(): " + d.speak() + ", Dog.super.speak(): " + d.speakAsAnimal()); // using super
    }
}

class Animal {
    protected final String name; // visible to subclasses (prefer private + getters in real code)

    Animal(String name) {
        this.name = name;
    }

    String speak() { // virtual (polymorphic)
        return "<generic sound>";
    }

    final void breathe() { // final: cannot be overridden
        System.out.println(name + " breathes");
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + name + ")";
    }
}

class Dog extends Animal {
    Dog(String name) { super(name); }

    @Override
    String speak() { return "woof"; }

    String speakAsAnimal() { return super.speak(); }

    void fetch() { System.out.println(name + " fetches the ball"); }

    @Override
    public String toString() { return super.toString() + " loves bones"; }

    // @Override void breathe() {} // won't compile (breathe is final)
}

class Cat extends Animal {
    Cat(String name) { super(name); }

    @Override
    String speak() { return "meow"; }
}

/* ---------- Inheritance: constructor chaining ---------- */
class ConstructorChaining {
    static void demo() {
        System.out.println("\n-- Constructor chaining & initialization order --");
        Employee e = new Employee("Alice", "Developer");
        System.out.println("Created: " + e);
    }
}

class Person {
    static { System.out.println("Person: static init"); }
    { System.out.println("Person: instance init"); }

    final String name;

    Person() {
        this("Anonymous");
        System.out.println("Person: no-arg ctor");
    }

    Person(String name) {
        this.name = name;
        System.out.println("Person: arg ctor");
    }

    @Override
    public String toString() { return getClass().getSimpleName() + "(" + name + ")"; }
}

class Employee extends Person {
    static { System.out.println("Employee: static init"); }
    { System.out.println("Employee: instance init"); }

    final String role;

    Employee(String name, String role) {
        super(name); // must be first
        this.role = role;
        System.out.println("Employee: ctor");
    }

    @Override
    public String toString() { return super.toString() + " role=" + role; }
}

/* ---------- Polymorphism: upcast, dynamic dispatch, downcast ---------- */
class Polymorphism {
    static void demo() {
        System.out.println("\n-- Polymorphism (upcast/downcast) --");
        List<Animal> zoo = Arrays.asList(new Dog("Rex"), new Cat("Mimi"), new Dog("Spot"));
        for (Animal a : zoo) {
            System.out.println(a + " speaks: " + a.speak()); // dynamic dispatch
            if (a instanceof Dog) {
                Dog d = (Dog) a; // safe after instanceof
                d.fetch();
            }
        }
    }
}

/* ---------- Abstract classes vs interfaces ---------- */
class AbstractVsInterface {
    static void demo() {
        System.out.println("\n-- Abstract class vs Interface --");
        List<Shape> shapes = Arrays.asList(new Circle(2.0), new Rectangle(3.0, 4.0));
        for (Shape s : shapes) {
            System.out.println(s + " area=" + s.area());
            if (s instanceof Renderable) {
                System.out.println(((Renderable) s).render());
            }
        }
    }
}

abstract class Shape {
    abstract double area();

    @Override
    public String toString() { return getClass().getSimpleName(); }
}

interface Renderable {
    default String render() { return "Rendering " + getClass().getSimpleName(); }
}

class Circle extends Shape implements Renderable {
    private final double radius;

    Circle(double radius) { this.radius = radius; }

    @Override
    double area() { return Math.PI * radius * radius; }
}

class Rectangle extends Shape implements Renderable {
    private final double width, height;

    Rectangle(double width, double height) { this.width = width; this.height = height; }

    @Override
    double area() { return width * height; }
}

/* ---------- Composition (has-a) & delegation ---------- */
class CompositionHasA {
    static void demo() {
        System.out.println("\n-- Composition (has-a) & delegation --");
        Car car = new Car("Sedan", new Engine(180));
        System.out.println(car);
        car.start();
        System.out.println("Running? " + car.isRunning());
        car.stop();
        System.out.println(car);
    }
}

class Engine {
    private final int horsepower;
    private boolean running;

    Engine(int horsepower) { this.horsepower = horsepower; }

    void start() {
        if (!running) {
            running = true;
            System.out.println("Engine " + horsepower + "hp started");
        }
    }

    void stop() {
        if (running) {
            running = false;
            System.out.println("Engine stopped");
        }
    }

    boolean isRunning() { return running; }

    int getHorsepower() { return horsepower; }

    @Override
    public String toString() {
        return "Engine(" + horsepower + "hp" + (running ? ",running" : "") + ")";
    }
}

class Car {
    private final String model;
    private final Engine engine; // has-a

    Car(String model, Engine engine) {
        this.model = model;
        this.engine = Objects.requireNonNull(engine);
    }

    Car(String model, int horsepower) { this(model, new Engine(horsepower)); }

    void start() { engine.start(); } // delegate
    void stop()  { engine.stop();  } // delegate
    boolean isRunning() { return engine.isRunning(); }

    @Override
    public String toString() { return "Car(" + model + ", " + engine + ")"; }
}

/* ---------- Strategy via composition (change behavior at runtime) ---------- */
class DelegationStrategy {
    static void demo() {
        System.out.println("\n-- Strategy via composition --");
        PaymentProcessor checkout = new PaymentProcessor(new CreditCardPayment("**** 1234"));
        checkout.pay(50);
        checkout.setStrategy(new PayPalPayment("user@example.com"));
        checkout.pay(75);
    }
}

interface PaymentStrategy {
    void pay(int amount);
}

class CreditCardPayment implements PaymentStrategy {
    private final String maskedCard;

    CreditCardPayment(String maskedCard) { this.maskedCard = maskedCard; }

    @Override
    public void pay(int amount) {
        System.out.println("Paid $" + amount + " with Credit Card " + maskedCard);
    }
}

class PayPalPayment implements PaymentStrategy {
    private final String account;

    PayPalPayment(String account) { this.account = account; }

    @Override
    public void pay(int amount) {
        System.out.println("Paid $" + amount + " via PayPal (" + account + ")");
    }
}

class PaymentProcessor {
    private PaymentStrategy strategy;

    PaymentProcessor(PaymentStrategy strategy) { setStrategy(strategy); }

    void setStrategy(PaymentStrategy strategy) {
        this.strategy = Objects.requireNonNull(strategy);
    }

    void pay(int amount) { strategy.pay(amount); }
}

/* ---------- Aggregation vs Composition ---------- */
class AggregationVsComposition {
    static void demo() {
        System.out.println("\n-- Aggregation vs Composition --");
        // Composition: Car owns its Engine (see CompositionHasA). Engine's lifecycle is bound to Car.
        // Aggregation: Team has Players that can outlive the Team.
        Player p = new Player("Sam");
        Team t = new Team("Sharks");
        t.add(p);
        System.out.println(t);
        t.remove(p);
        System.out.println("Team after removal: " + t);
        System.out.println("Player still exists: " + p);
    }
}

class Player {
    private final String name;

    Player(String name) { this.name = name; }

    @Override
    public String toString() { return "Player(" + name + ")"; }
}

class Team {
    private final String name;
    private final List<Player> roster = new ArrayList<>();

    Team(String name) { this.name = name; }

    void add(Player p) { roster.add(Objects.requireNonNull(p)); }
    void remove(Player p) { roster.remove(p); }

    @Override
    public String toString() { return "Team(" + name + ", roster=" + roster + ")"; }
}

/* ---------- Favor composition over inheritance (pitfall example) ---------- */
class InheritanceVsCompositionPitfall {
    static void demo() {
        System.out.println("\n-- Pitfall: extending concrete classes (Stack vs ArrayList) --");

        BadStack<Integer> bad = new BadStack<>();
        bad.push(1);
        bad.push(2);
        // Inherits all ArrayList mutators, breaking the stack abstraction:
        bad.add(0, 999);
        System.out.println("BadStack contents: " + bad);
        System.out.println("BadStack pop: " + bad.pop()); // Still 2, but 999 polluted the structure

        GoodStack<Integer> good = new GoodStack<>();
        good.push(1);
        good.push(2);
        // good.add(...) // Not possible: API is controlled
        System.out.println("GoodStack pop: " + good.pop());
    }
}

class BadStack<E> extends ArrayList<E> {
    void push(E e) { add(e); }
    E pop() { return remove(size() - 1); }
}

class GoodStack<E> {
    private final Deque<E> stack = new ArrayDeque<>();

    void push(E e) { stack.push(e); }
    E pop() { return stack.pop(); }

    @Override
    public String toString() { return stack.toString(); }
}

/* ---------- Immutability with composition (defensive copies) ---------- */
class ImmutabilityWithComposition {
    static void demo() {
        System.out.println("\n-- Immutability with Composition (defensive copy) --");
        MutableAddress addr = new MutableAddress("New York", "10001");
        ImmutablePerson person = new ImmutablePerson("Ann", addr);

        System.out.println("Person address: " + person.addressString());
        addr.setCity("Los Angeles"); // mutate original; should not affect person
        System.out.println("After external change, person address: " + person.addressString());

        MutableAddress leaked = person.getAddress(); // defensive copy
        leaked.setCity("San Francisco");
        System.out.println("After mutating copy, person address: " + person.addressString());
    }
}

class MutableAddress {
    private String city;
    private String zip;

    MutableAddress(String city, String zip) { this.city = city; this.zip = zip; }

    MutableAddress(MutableAddress other) { this(other.city, other.zip); }

    void setCity(String city) { this.city = city; }
    void setZip(String zip)   { this.zip = zip; }

    String getCity() { return city; }
    String getZip()  { return zip; }

    @Override
    public String toString() { return city + " " + zip; }
}

class ImmutablePerson {
    private final String name;
    private final MutableAddress address; // kept private + copied

    ImmutablePerson(String name, MutableAddress address) {
        this.name = name;
        this.address = new MutableAddress(Objects.requireNonNull(address)); // defensive copy
    }

    String addressString() { return address.toString(); }

    // Defensive copy on getter to protect internal state
    MutableAddress getAddress() { return new MutableAddress(address); }

    @Override
    public String toString() { return "ImmutablePerson(" + name + ", " + address + ")"; }
}

/* ---------- Multiple inheritance via interfaces (default method conflict) ---------- */
class InterfaceDefaultMethodConflict {
    static void demo() {
        System.out.println("\n-- Interface default method conflict resolution --");
        ConflictedGreeter g = new ConflictedGreeter();
        System.out.println(g.greet());
    }
}

interface GreeterA {
    default String greet() { return "Hello from A"; }
}

interface GreeterB {
    default String greet() { return "Hello from B"; }
}

class ConflictedGreeter implements GreeterA, GreeterB {
    // Must override to resolve ambiguity; can delegate to specific super-interface:
    @Override
    public String greet() {
        return GreeterA.super.greet() + " & " + GreeterB.super.greet();
    }
}