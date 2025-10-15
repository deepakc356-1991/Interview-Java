package _02_04_inheritance_and_composition;

import java.io.*;
import java.util.*;

/**
 * 3) _03_InterviewQA.java
 * Package: _02_04_inheritance_and_composition
 *
 * Inheritance & Composition interview Q&A from basic to advanced with runnable examples.
 * Run main() to see key outputs. Read comments for Q&A.
 */
public class _03_InterviewQA {

    public static void main(String[] args) {
        System.out.println("=== Basics: Inheritance & Polymorphism ===");
        demoBasicsInheritancePolymorphism();

        System.out.println("\n=== Composition & Delegation ===");
        demoCompositionDelegation();

        System.out.println("\n=== Overriding vs Overloading ===");
        demoOverrideVsOverload();

        System.out.println("\n=== Upcasting, Downcasting, instanceof ===");
        demoUpDownCasting();

        System.out.println("\n=== Field Hiding vs Method Overriding; Static binding ===");
        demoFieldHidingAndStatic();

        System.out.println("\n=== super vs this; Constructors & toString ===");
        demoSuperThis();

        System.out.println("\n=== final (class/method), access & inheritance notes ===");
        demoFinal();

        System.out.println("\n=== Abstract classes, Interfaces, Default method conflict ===");
        demoAbstractInterfaceDefaultConflict();

        System.out.println("\n=== Covariant return types ===");
        demoCovariantReturn();

        System.out.println("\n=== Initialization order in inheritance ===");
        demoInitOrder();

        System.out.println("\n=== Aggregation vs Composition ===");
        demoAggregationVsComposition();

        System.out.println("\n=== LSP: Rectangle-Square problem (why composition) ===");
        demoLSPRectangleSquare();

        System.out.println("\n=== equals/hashCode with inheritance pitfalls ===");
        demoEqualsInheritancePitfall();

        System.out.println("\n=== Template Method (inheritance) vs Strategy (composition) ===");
        demoTemplateVsStrategy();

        System.out.println("\n=== Decorator pattern via composition ===");
        demoDecorator();

        System.out.println("\n=== Favor composition over inheritance: Instrumented Set example ===");
        demoCountingCollectionPitfall();

        System.out.println("\n=== Arrays covariance vs Generics invariance ===");
        demoArraysVsGenerics();

        System.out.println("\n=== Deep vs Shallow copy with composition ===");
        demoDeepVsShallowCopy();

        System.out.println("\n=== Fluent API with self types (CRTP) ===");
        demoCRTPFluent();

        System.out.println("\nDone.");
    }

    // Q: What is inheritance? A: Mechanism for IS-A relationship; code reuse; polymorphism via overriding.
    // Q: What is polymorphism? A: Call resolved at runtime based on actual object type.
    static void demoBasicsInheritancePolymorphism() {
        Animal a = new Animal();
        Animal b = new Dog(); // upcasting
        a.speak();            // Animal speaks
        b.speak();            // Dog barks (dynamic dispatch)
    }

    // Q: What is composition? A: HAS-A relationship; objects contain other objects and delegate work.
    // Q: When to use? A: Prefer when you want to reuse behavior flexibly and avoid tight coupling.
    static void demoCompositionDelegation() {
        Car car = new Car(new Engine());
        car.drive();
    }

    // Q: Overriding vs Overloading?
    // Overriding = same signature in subclass, runtime dispatch. Overloading = same name, different parameters, resolved at compile time.
    static void demoOverrideVsOverload() {
        Dog d = new Dog();
        d.speak();                 // override
        d.speak("Loud:");          // overload
    }

    // Q: Upcasting/Downcasting?
    // Upcasting is safe; Downcasting requires instanceof or can throw ClassCastException.
    static void demoUpDownCasting() {
        Animal a = new Dog(); // upcast
        if (a instanceof Dog) {
            Dog d = (Dog) a;  // safe downcast
            d.fetch();
        }
        Animal catAsAnimal = new Cat();
        // Dog notDog = (Dog) catAsAnimal; // would throw ClassCastException at runtime
    }

    // Q: Are fields overridden? A: No, fields are hidden; static methods are hidden; only instance methods are overridden.
    static void demoFieldHidingAndStatic() {
        Animal a = new Dog();
        System.out.println("Field via reference type (Animal): " + a.type); // "Animal"
        System.out.println("Static via class Animal:"); Animal.staticInfo();
        System.out.println("Static via class Dog:"); Dog.staticInfo();
        System.out.println("Static via ref of type Animal (calls Animal):"); a.staticInfo();
    }

    // Q: super vs this?
    // super calls parent methods/constructors; this refers to current instance/other constructors.
    // Q: Are constructors inherited? A: No.
    static void demoSuperThis() {
        Employee e = new Employee("Ava", 42);
        System.out.println(e);
    }

    // Q: final class/method? A: final class cannot be extended; final method cannot be overridden.
    // Q: Access: private not inherited; protected accessible in subclasses (even across packages via inheritance); package-private only in same package.
    static void demoFinal() {
        DerivedFromFinal d = new DerivedFromFinal();
        d.can();
        d.cannotOverride(); // final method from base
        // Note: Can't create subclass of a final class; can't override a final method (compile-time error).
    }

    // Q: Abstract classes vs Interfaces?
    // Abstract: can hold state; some implementation. Interface: contract; multiple inheritance of type; default/static methods allowed.
    // Q: Default method conflict? Must override and choose.
    static void demoAbstractInterfaceDefaultConflict() {
        Shape c = new Circle(2);
        Shape r = new Rectangle(3, 4);
        System.out.println("Circle area=" + c.area() + ", Rectangle area=" + r.area());
        DefaultConflict dc = new DefaultConflict();
        System.out.println("Default conflict resolved greet: " + dc.greet());
    }

    // Q: Covariant return types? A: Overriding method may return a subtype of original return type.
    static void demoCovariantReturn() {
        Animal a = new Animal();
        Dog d = new Dog();
        Animal aCopy = a.copy();
        Dog dCopy = d.copy(); // returns Dog, more specific
        System.out.println("Covariant copy types: " + aCopy.getClass().getSimpleName() + ", " + dCopy.getClass().getSimpleName());
    }

    // Q: Order of initialization in inheritance?
    // 1) Parent static 2) Child static 3) Parent instance 4) Parent ctor 5) Child instance 6) Child ctor
    static void demoInitOrder() {
        new InitDerived();
    }

    // Q: Aggregation vs Composition?
    // Aggregation: has-a but weak ownership (objects can outlive container). Composition: strong ownership/lifecycle bound.
    static void demoAggregationVsComposition() {
        Player p = new Player("Alex");
        Team team = new Team("Dream");
        team.addPlayer(p);
        System.out.println(team);
        team = null; // team eligible for GC; player p still exists (aggregation)

        House house = new House("Villa");
        System.out.println("House rooms: " + house.rooms.size());
        // Rooms do not make sense without House (composition)
    }

    // Q: LSP violation example: Square extends Rectangle (bad inheritance).
    // Functions that rely on Rectangle's setWidth/setHeight semantics break for Square.
    static void demoLSPRectangleSquare() {
        RectangleLSP rect = new RectangleLSP();
        rect.setWidth(5);
        rect.setHeight(4);
        System.out.println("Rectangle area expected 20: " + areaAfterResize(rect));

        RectangleLSP square = new SquareLSP();
        square.setWidth(5);
        square.setHeight(4); // forces width=height=4 in Square
        System.out.println("Square area NOT 20 (LSP break): " + areaAfterResize(square));
        // Fix: use composition: Square has a side; no inheritance from Rectangle.
    }

    static int areaAfterResize(RectangleLSP r) {
        r.setWidth(5);
        r.setHeight(4);
        return r.getArea();
    }

    // Q: equals/hashCode with inheritance pitfalls?
    // Using instanceof in base equals allows equality with subclasses, but subclass adding fields breaks symmetry. Prefer composition or favor getClass in equals.
    static void demoEqualsInheritancePitfall() {
        Point p = new Point(1, 2);
        ColorPoint cp = new ColorPoint(1, 2, "red");

        System.out.println("p.equals(cp): " + p.equals(cp));  // true (bad)
        System.out.println("cp.equals(p): " + cp.equals(p));  // false (symmetry broken)

        // Composition fix: ColorPoint2 wraps a Point and compares only to same type
        ColorPoint2 cp2a = new ColorPoint2(new Point(1, 2), "red");
        ColorPoint2 cp2b = new ColorPoint2(new Point(1, 2), "red");
        System.out.println("Composition equals ok: " + cp2a.equals(cp2b));
    }

    // Q: Template Method (inheritance) vs Strategy (composition)?
    static void demoTemplateVsStrategy() {
        String[] lines = {"hello world", "java inheritance", "composition strategy"};
        FileProcessor template = new WordCountProcessor();
        int words = ((WordCountProcessor) template).process(lines);
        System.out.println("Template word count: " + words);

        LineHandler handler = new UniqueWordHandler();
        CompositionProcessor comp = new CompositionProcessor(handler);
        comp.process(lines);
        System.out.println("Strategy unique words: " + ((UniqueWordHandler) handler).unique.size());
    }

    // Q: Decorator pattern via composition (add behavior without inheritance explosion).
    static void demoDecorator() {
        Notifier base = new EmailNotifier();
        Notifier decorated = new SMSDecorator(new SlackDecorator(base));
        decorated.send("Build passed");
    }

    // Q: Favor composition over inheritance. Extending concrete classes is fragile.
    // Example: InstrumentedHashSet double-counts due to internal implementation assumptions.
    static void demoCountingCollectionPitfall() {
        InstrumentedHashSet<Integer> bad = new InstrumentedHashSet<>();
        bad.addAll(List.of(1, 2, 3));
        System.out.println("Bad count (double-counted): " + bad.getAddCount()); // 6

        InstrumentedSet<Integer> good = new InstrumentedSet<>(new HashSet<>());
        good.addAll(List.of(1, 2, 3));
        System.out.println("Good count: " + good.getAddCount()); // 3
    }

    // Q: Arrays covariance vs Generics invariance.
    static void demoArraysVsGenerics() {
        try {
            String[] sa = new String[1];
            Object[] oa = sa;   // arrays are covariant (allowed)
            oa[0] = new Object(); // runtime ArrayStoreException
        } catch (ArrayStoreException ex) {
            System.out.println("ArrayStoreException due to arrays covariance.");
        }

        // Generics are invariant:
        // List<String> ls = new ArrayList<>();
        // List<Object> lo = ls; // compile-time error
        // Use wildcard to read-only:
        List<String> ls = List.of("a", "b");
        List<?> lw = ls; // cannot add to lw (except null), but can read as Object
        System.out.println("First via wildcard read: " + lw.get(0));
    }

    // Q: Deep vs Shallow copy in composition objects.
    static void demoDeepVsShallowCopy() {
        Address addr = new Address("NYC");
        Person2 p1 = new Person2("Bob", addr);
        Person2 shallow = p1.shallowCopy();
        Person2 deep = p1.deepCopy();

        addr.city = "LA"; // mutate shared component
        System.out.println("Shallow shares: " + shallow.address.city); // LA
        System.out.println("Deep isolated: " + deep.address.city);     // NYC
    }

    // Q: Fluent APIs in inheritance with self-types (CRTP) to preserve chaining types.
    static void demoCRTPFluent() {
        UserBuilder ub = new UserBuilder().withName("Eve").withAge(30);
        System.out.println("Built user: " + ub.build());
        // Without CRTP, chaining methods would return base type and lose specific API.
    }
}

/* ====== Basics: Inheritance/Composition demo types ====== */

class Animal {
    String type = "Animal";
    static void staticInfo() { System.out.println("Animal.staticInfo"); }
    void speak() { System.out.println("Animal speaks"); }
    Animal copy() { return new Animal(); }
}

class Dog extends Animal {
    String type = "Dog"; // field hiding, not overriding
    static void staticInfo() { System.out.println("Dog.staticInfo"); }
    @Override void speak() { System.out.println("Dog barks"); }
    void speak(String prefix) { System.out.println(prefix + " Dog barks"); } // overload
    void fetch() { System.out.println("Dog fetches"); }
    @Override Dog copy() { return new Dog(); } // covariant return
}

class Cat extends Animal {
    @Override void speak() { System.out.println("Cat meows"); }
}

class Engine {
    void start() { System.out.println("Engine starting"); }
}

class Car {
    private final Engine engine; // composition (HAS-A)
    Car(Engine engine) { this.engine = engine; }
    void drive() {
        engine.start(); // delegation
        System.out.println("Car driving");
    }
}

/* ====== super/this, constructors, access, final ====== */

class Person {
    protected final String name;
    Person(String name) { this.name = name; }
    @Override public String toString() { return "Person{name='" + name + "'}"; }
}

class Employee extends Person {
    private final int id;
    Employee(String name, int id) {
        super(name); // call parent ctor
        this.id = id;
    }
    @Override public String toString() {
        return "Employee{id=" + id + ", super=" + super.toString() + "}";
    }
}

class BaseWithFinal {
    public final void cannotOverride() { System.out.println("BaseWithFinal.final method"); }
    public void can() { System.out.println("BaseWithFinal.can"); }
}

class DerivedFromFinal extends BaseWithFinal {
    @Override public void can() { System.out.println("DerivedFromFinal.can"); }
    // @Override public void cannotOverride() {} // compile error
}

/* ====== Abstract classes, interfaces, default methods ====== */

abstract class Shape {
    abstract double area();
}

class Circle extends Shape {
    final double r;
    Circle(double r) { this.r = r; }
    @Override double area() { return Math.PI * r * r; }
}

class Rectangle extends Shape {
    final double w, h;
    Rectangle(double w, double h) { this.w = w; this.h = h; }
    @Override double area() { return w * h; }
}

// Interface default method conflict resolution
interface ADefault {
    default String greet() { return "A"; }
}
interface BDefault {
    default String greet() { return "B"; }
}
class DefaultConflict implements ADefault, BDefault {
    @Override public String greet() {
        return ADefault.super.greet() + "+" + BDefault.super.greet();
    }
}

/* ====== Initialization order ====== */

class InitBase {
    static { System.out.println("InitBase.static"); }
    { System.out.println("InitBase.instance"); }
    InitBase() { System.out.println("InitBase.ctor"); }
}
class InitDerived extends InitBase {
    static { System.out.println("InitDerived.static"); }
    { System.out.println("InitDerived.instance"); }
    InitDerived() { System.out.println("InitDerived.ctor"); }
}

/* ====== Aggregation vs Composition ====== */

class Player {
    final String name;
    Player(String name) { this.name = name; }
    @Override public String toString() { return name; }
}
class Team {
    final String name;
    final List<Player> players = new ArrayList<>();
    Team(String name) { this.name = name; }
    void addPlayer(Player p) { players.add(p); }
    @Override public String toString() { return "Team{" + name + ", players=" + players + "}"; }
}
class House {
    final String name;
    final List<Room> rooms = new ArrayList<>();
    House(String name) {
        this.name = name;
        rooms.add(new Room("Living"));
        rooms.add(new Room("Kitchen"));
    }
    class Room { // strongly owned by House
        final String kind;
        Room(String kind) { this.kind = kind; }
    }
}

/* ====== LSP: Rectangle/Square problem ====== */

class RectangleLSP {
    protected int width, height;
    void setWidth(int w) { width = w; }
    void setHeight(int h) { height = h; }
    int getArea() { return width * height; }
}
class SquareLSP extends RectangleLSP {
    @Override void setWidth(int w) { width = height = w; }
    @Override void setHeight(int h) { width = height = h; }
}

/* ====== equals/hashCode pitfalls ====== */

class Point {
    final int x, y;
    Point(int x, int y) { this.x = x; this.y = y; }

    // Using instanceof allows equality with subclasses (dangerous if subclasses add state)
    @Override public boolean equals(Object o) {
        if (!(o instanceof Point)) return false;
        Point other = (Point) o;
        return x == other.x && y == other.y;
    }
    @Override public int hashCode() { return Objects.hash(x, y); }
    @Override public String toString() { return "Point(" + x + "," + y + ")"; }
}

class ColorPoint extends Point {
    final String color;
    ColorPoint(int x, int y, String color) { super(x, y); this.color = color; }

    // Tries to remain equal to Point ignoring color for Point, but include color for ColorPoint
    @Override public boolean equals(Object o) {
        if (!(o instanceof Point)) return false;
        // If comparing to plain Point, ignore color
        if (!(o instanceof ColorPoint)) return super.equals(o);
        // If both ColorPoints, include color
        ColorPoint other = (ColorPoint) o;
        return super.equals(o) && Objects.equals(color, other.color);
    }
    @Override public int hashCode() { return Objects.hash(super.hashCode(), color); }
}

class ColorPoint2 {
    final Point point; // composition
    final String color;
    ColorPoint2(Point point, String color) { this.point = point; this.color = color; }
    @Override public boolean equals(Object o) {
        if (!(o instanceof ColorPoint2)) return false; // only equal to same type
        ColorPoint2 other = (ColorPoint2) o;
        return point.equals(other.point) && Objects.equals(color, other.color);
    }
    @Override public int hashCode() { return Objects.hash(point, color); }
}

/* ====== Template Method vs Strategy ====== */

// Template Method (inheritance)
abstract class FileProcessor {
    // final template method defines steps
    final int process(String[] lines) {
        init();
        int result = 0;
        for (String line : lines) result += handleLine(line);
        cleanup();
        return result;
    }
    void init() {}
    void cleanup() {}
    abstract int handleLine(String line);
}

class WordCountProcessor extends FileProcessor {
    @Override int handleLine(String line) { return line.split("\\s+").length; }
}

// Strategy (composition)
interface LineHandler {
    void handle(String line);
}

class UniqueWordHandler implements LineHandler {
    final Set<String> unique = new HashSet<>();
    @Override public void handle(String line) {
        for (String w : line.split("\\s+")) unique.add(w.toLowerCase());
    }
}

class CompositionProcessor {
    private final LineHandler handler;
    CompositionProcessor(LineHandler handler) { this.handler = handler; }
    void process(String[] lines) { for (String line : lines) handler.handle(line); }
}

/* ====== Decorator (composition) ====== */

interface Notifier { void send(String message); }

class EmailNotifier implements Notifier {
    @Override public void send(String message) { System.out.println("[Email] " + message); }
}

abstract class NotifierDecorator implements Notifier {
    protected final Notifier wrap;
    protected NotifierDecorator(Notifier wrap) { this.wrap = wrap; }
    @Override public void send(String message) { wrap.send(message); }
}

class SMSDecorator extends NotifierDecorator {
    SMSDecorator(Notifier wrap) { super(wrap); }
    @Override public void send(String message) {
        super.send(message);
        System.out.println("[SMS] " + message);
    }
}

class SlackDecorator extends NotifierDecorator {
    SlackDecorator(Notifier wrap) { super(wrap); }
    @Override public void send(String message) {
        super.send(message);
        System.out.println("[Slack] " + message);
    }
}

/* ====== Favor composition over inheritance example ====== */

// Broken: relies on HashSet implementation details; double counts because super.addAll internally calls add
class InstrumentedHashSet<E> extends HashSet<E> {
    private int addCount = 0;
    public InstrumentedHashSet() { super(); }
    @Override public boolean add(E e) { addCount++; return super.add(e); }
    @Override public boolean addAll(Collection<? extends E> c) {
        addCount += c.size(); // will double count
        return super.addAll(c);
    }
    public int getAddCount() { return addCount; }
}

// Fixed: composition-based wrapper
class InstrumentedSet<E> implements Set<E> {
    private final Set<E> delegate;
    private int addCount = 0;
    InstrumentedSet(Set<E> delegate) { this.delegate = delegate; }
    public int getAddCount() { return addCount; }

    @Override public boolean add(E e) { addCount++; return delegate.add(e); }
    @Override public boolean addAll(Collection<? extends E> c) {
        boolean modified = false;
        for (E e : c) modified |= add(e); // count via add exactly once per element
        return modified;
    }

    // Delegate other Set methods
    @Override public int size() { return delegate.size(); }
    @Override public boolean isEmpty() { return delegate.isEmpty(); }
    @Override public boolean contains(Object o) { return delegate.contains(o); }
    @Override public Iterator<E> iterator() { return delegate.iterator(); }
    @Override public Object[] toArray() { return delegate.toArray(); }
    @Override public <T> T[] toArray(T[] a) { return delegate.toArray(a); }
    @Override public boolean remove(Object o) { return delegate.remove(o); }
    @Override public boolean containsAll(Collection<?> c) { return delegate.containsAll(c); }
    @Override public boolean retainAll(Collection<?> c) { return delegate.retainAll(c); }
    @Override public boolean removeAll(Collection<?> c) { return delegate.removeAll(c); }
    @Override public void clear() { delegate.clear(); }
    @Override public boolean equals(Object o) { return delegate.equals(o); }
    @Override public int hashCode() { return delegate.hashCode(); }
}

/* ====== Arrays covariance vs Generics invariance ====== */
// see demoArraysVsGenerics()

/* ====== Deep vs Shallow copy ====== */

class Address {
    String city;
    Address(String city) { this.city = city; }
    Address(Address other) { this.city = other.city; } // shallow field copy (OK because String is immutable)
}

class Person2 {
    String name;
    Address address;
    Person2(String name, Address address) { this.name = name; this.address = address; }
    Person2 shallowCopy() { return new Person2(this.name, this.address); }
    Person2 deepCopy() { return new Person2(this.name, new Address(this.address.city)); }
}

/* ====== Fluent API with self types (CRTP) ====== */

abstract class BaseBuilder<T extends BaseBuilder<T>> {
    String name;
    int age;
    public T withName(String name) { this.name = name; return self(); }
    public T withAge(int age) { this.age = age; return self(); }
    protected abstract T self();
}

class UserBuilder extends BaseBuilder<UserBuilder> {
    @Override protected UserBuilder self() { return this; }
    public User build() { return new User(name, age); }
}

class User {
    final String name; final int age;
    User(String name, int age) { this.name = name; this.age = age; }
    @Override public String toString() { return "User{name='" + name + "', age=" + age + "}"; }
}

/* ====== Additional Interview Notes (comments only) ======
- IS-A vs HAS-A: Use inheritance for IS-A; composition for HAS-A.
- Avoid inheriting from concrete classes you don't control (fragile base class, versioning, unexpected overrides).
- Access modifiers:
  private: not visible/inherited; protected: visible to subclasses (even different package) via inheritance; package-private: only same package; public: visible everywhere.
- Multiple inheritance of implementation is not supported; interfaces provide multiple inheritance of type; default methods can cause conflicts requiring explicit override.
- final classes (e.g., String, Integer) cannot be extended; records are implicitly final (prefer composition).
- Sealed classes (Java 17+) restrict which classes may extend/implement an API, improving controlled inheritance hierarchies.
- Prefer composition when:
  1) You need to add behavior without changing type identity (Decorator).
  2) You want to switch behavior at runtime (Strategy).
  3) You want to wrap/forward to avoid coupling to superclass internals (Forwarding/Wrapper).
- Downcasting should be minimized; rely on polymorphism or visitors/sealed hierarchies instead.
- Method overriding rules: cannot reduce visibility; can widen (protected -> public). Exceptions: overriding method cannot throw broader checked exceptions.
- Return type covariance allowed; parameter types are not covariant in overriding (would break LSP).
*/