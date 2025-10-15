package _02_05_polymorphism_and_overriding;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/*
Polymorphism & Overriding — Theory via runnable examples

Key ideas:
- Polymorphism: a single reference type (e.g., base class or interface) can point to objects of different concrete types. Method calls are resolved at runtime (dynamic dispatch).
- Overriding: subclass provides its own implementation of a superclass or interface method (same signature, covariant return allowed).
- Overloading vs Overriding: overloading chooses method by compile-time parameter types; overriding chooses by runtime object type.
- Virtual method invoCat1ion: non-private, non-static, non-final instance methods are virtual (dispatched at runtime).
- static, private methods and fields are not polymorphic (static methods are hidden, private methods are not overridden, fields are hidden).
- Rules for overriding:
  - Same name and parameter types (signature).
  - Return type must be the same or covariant (a subclass of the original return type).
  - Access level cannot be reduced (can be widened).
  - Overriding method cannot throw broader checked exceptions; may throw narrower or unchecked exceptions.
  - Use @Override to Cat1ch mistakes.
- Upcasting is implicit and safe; downcasting requires explicit cast and may throw ClassCastException (use instanceof).
- Interfaces: default methods can be overridden; multiple-inheritance conflicts must be resolved explicitly.
- Abstract method must be implemented by concrete subclasses, otherwise the subclass remains abstract.
- Fields are not virtual (polymorphism applies to methods).
- Constructors are not inherited/overridden; they chain via super().
- equals/hashCode/toString overriding for substitutability and useful polymorphic behavior.
- Generics + overriding: covariant overriding with type erasure produces compiler-generated bridge methods (transparent to you).
*/
public class _01_Theory {
    public static void main(String[] args) {
        System.out.println("=== 1) Basic runtime polymorphism (dynamic dispatch) ===");
        Animal1 a1 = new Dog1();      // upcast (implicit)
        Animal1 a2 = new Cat1();

        System.out.println("a1.speak -> " + a1.speak()); // Dog1 version chosen at runtime
        System.out.println("a2.speak -> " + a2.speak()); // Cat1 version chosen at runtime
        System.out.println("a1.move  -> " + a1.move());
        System.out.println("a2.move  -> " + a2.move());
        System.out.println("a1.toString -> " + a1);      // toString overridden in Dog1

        System.out.println("\n=== 2) super keyword (call overridden implementation) ===");
        Dog1 Dog1 = new Dog1();
        System.out.println("Dog1.speak (includes super): " + Dog1.speak());

        System.out.println("\n=== 3) Upcasting and Downcasting ===");
        Animal1 upcast = new Dog1(); // upcast is safe
        if (upcast instanceof Dog1) {
            Dog1 castedDog1 = (Dog1) upcast; // downcast
            System.out.println("Downcast OK: " + castedDog1.fetch());
        }
        try {
            Cat1 wrong = (Cat1) upcast; // runtime error (ClassCastException)
            System.out.println(wrong);
        } catch (ClassCastException ex) {
            System.out.println("Wrong downcast -> " + ex.getClass().getSimpleName());
        }

        System.out.println("\n=== 4) Overloading vs Overriding ===");
        OverloadDemo od = new OverloadDemo();
        Animal1 Animal1RefToDog1 = new Dog1();
        od.foo(Animal1RefToDog1);     // chooses foo(Animal1) at compile time (by variable type)
        od.foo((Dog1) Animal1RefToDog1); // chooses foo(Dog1), because parameter type is Dog1 at compile time

        System.out.println("\n=== 5) static methods are hidden, not overridden ===");
        StaticBase baseRef = new StaticChild();
        baseRef.stat();          // calls StaticBase.stat (resolved by reference type)
        StaticBase.stat();       // StaticBase
        StaticChild.stat();      // StaticChild

        System.out.println("\n=== 6) Fields are not polymorphic (they are hidden) ===");
        FieldBase fb = new FieldChild();
        System.out.println("fb.name -> " + fb.name);                    // Base field (by reference type)
        System.out.println("((FieldChild)fb).name -> " + ((FieldChild) fb).name); // Child field

        System.out.println("\n=== 7) Access modifiers and overriding (cannot reduce visibility) ===");
        AccessBase access = new AccessChild();
        System.out.println("AccessChild overrides with wider visibility: " + access.m());

        System.out.println("\n=== 8) Exceptions and overriding (narrower checked exceptions) ===");
        ThrowsBase t = new ThrowsChild();
        try {
            t.risky(); // ThrowsChild throws narrower FileNotFoundException
        } catch (IOException e) {
            System.out.println("Caught checked exception from override: " + e.getClass().getSimpleName());
        }

        System.out.println("\n=== 9) Covariant return types ===");
        Printer printer = new Dog1Printer();
        Animal1 created = printer.create();                 // actually a Dog1
        System.out.println("Printer.create returned: " + created.getClass().getSimpleName());
        Dog1 Dog1Created = ((Dog1Printer) printer).create();  // covariant return lets us use Dog1 at compile-time in subclass
        System.out.println("Dog1Printer.create (Dog1) -> " + Dog1Created.getClass().getSimpleName());

        System.out.println("\n=== 10) Interface default method conflicts must be resolved ===");
        Athlete athlete = new Athlete();
        System.out.println("Athlete.move -> " + athlete.move());

        System.out.println("\n=== 11) Abstract classes and runtime polymorphism ===");
        Shape[] shapes = { new Circle(2), new Rectangle(3, 4) };
        for (Shape s : shapes) {
            System.out.println(s.getClass().getSimpleName() + ".area -> " + s.area());
        }

        System.out.println("\n=== 12) equals/hashCode and toString overriding ===");
        Person p1 = new Person("Ann", 30);
        Person p2 = new Person("Ann", 30);
        Person p3 = new Person("Bob", 30);
        System.out.println("p1.equals(p2) -> " + p1.equals(p2));
        System.out.println("p1.equals(p3) -> " + p1.equals(p3));
        Set<Person> set = new HashSet<>();
        set.add(p1);
        set.add(p2); // equal -> not added if hashCode/equals consistent
        System.out.println("HashSet size (expect 1 or 2 depending on equals/hashCode) -> " + set.size());
        System.out.println("p1.toString -> " + p1);

        System.out.println("\n=== 13) Generics + overriding (bridge methods under the hood) ===");
        Box<String> bx = new StringBox("hello");
        String val = bx.get(); // covariant override (bridge method ensures binary compatibility)
        System.out.println("Box<String>.get -> " + val);

        System.out.println("\n=== 14) private methods are not overridden (no dynamic dispatch) ===");
        PrivateBase pb = new PrivateChild();
        pb.call(); // calls PrivateBase.secret(), not the child's secret()

        System.out.println("\n=== 15) final methods cannot be overridden (compile-time rule) ===");
        FinalBase fbFinal = new FinalBase();
        fbFinal.cannotOverride();
        // class FinalChild extends FinalBase { @Override void cannotOverride() {} } // <- illegal to compile

        System.out.println("\n=== 16) Constructor chaining (not overriding) ===");
        BullDog1 bullDog1 = new BullDog1(); // Observe printed constructor order
        System.out.println("BullDog1.speak -> " + bullDog1.speak());

        System.out.println("\nLiskov Substitution Principle (LSP): anywhere an Animal1 is expected, a Dog1/Cat1 should work without breaking behavior assumptions. Violating LSP leads to fragile polymorphism.");
    }
}

/* ============================
   1) Base hierarchy for runtime polymorphism
   ============================ */
class Animal1 {
    public Animal1() {
        System.out.println("Constructing Animal1");
    }

    public String speak() {
        return "generic Animal1 sound";
    }

    public String move() {
        return "Animal1 moves";
    }
}

class Dog1 extends Animal1 {
    public Dog1() {
        System.out.println("Constructing Dog1");
    }

    @Override
    public String speak() {
        // Calls super to demonstrate chaining; not required in normal overriding
        return "Dog1 barks; also " + super.speak();
    }

    @Override
    public String move() {
        return "Dog1 runs";
    }

    public String fetch() {
        return "Dog1 fetches a ball";
    }

    @Override
    public String toString() {
        return "Dog1{}";
    }
}

class Cat1 extends Animal1 {
    public Cat1() {
        System.out.println("Constructing Cat1");
    }

    @Override
    public String speak() {
        return "Cat1 meows";
    }

    @Override
    public String move() {
        return "Cat1 stalks";
    }
}

class BullDog1 extends Dog1 {
    public BullDog1() {
        System.out.println("Constructing BullDog1");
    }

    @Override
    public String speak() {
        return "bullDog1 gruffs";
    }
}

/* ============================
   2) Overloading vs Overriding
   ============================ */
class OverloadDemo {
    void foo(Animal1 a) {
        System.out.println("OverloadDemo.foo(Animal1)");
    }

    void foo(Dog1 d) {
        System.out.println("OverloadDemo.foo(Dog1)");
    }
}

/* ============================
   3) static: method hiding, not overriding
   ============================ */
class StaticBase {
    static void stat() {
        System.out.println("StaticBase.stat");
    }
}

class StaticChild extends StaticBase {
    static void stat() {
        System.out.println("StaticChild.stat");
    }
}

/* ============================
   4) Fields are hidden (not virtual)
   ============================ */
class FieldBase {
    String name = "Base";
}

class FieldChild extends FieldBase {
    String name = "Child"; // hides, not overrides
}

/* ============================
   5) Access modifiers and overriding
   - Cannot reduce visibility; can widen (e.g., protected -> public).
   ============================ */
class AccessBase {
    protected String m() {
        return "AccessBase.m";
    }
}

class AccessChild extends AccessBase {
    @Override
    public String m() { // widened from protected to public
        return "AccessChild.m";
    }
}

/* ============================
   6) Exceptions in overriding:
   - Overriding method cannot throw new/broader checked exceptions.
   - It may throw fewer/narrower checked exceptions.
   ============================ */
class ThrowsBase {
    public Animal1 risky() throws IOException {
        return new Animal1();
    }
}

class ThrowsChild extends ThrowsBase {
    @Override
    public Animal1 risky() throws FileNotFoundException { // narrower than IOException
        throw new FileNotFoundException("Simulated narrow checked exception");
    }
}

/* ============================
   7) Covariant return types
   ============================ */
class Printer {
    public Animal1 create() {
        return new Animal1();
    }
}

class Dog1Printer extends Printer {
    @Override
    public Dog1 create() { // covariant return
        return new Dog1();
    }
}

/* ============================
   8) Interfaces: default methods and conflict resolution
   ============================ */
interface Walker {
    default String move() {
        return "walk";
    }
}

interface Runner {
    default String move() {
        return "run";
    }
}

class Athlete implements Walker, Runner {
    @Override
    public String move() {
        // Must resolve conflict explicitly; can choose one or define new behavior.
        return Walker.super.move();
    }
}

/* ============================
   9) Abstract classes: abstract methods must be overridden
   ============================ */
abstract class Shape {
    public abstract double area();
}

class Circle extends Shape {
    private final double r;
    Circle(double r) { this.r = r; }
    @Override public double area() { return Math.PI * r * r; }
}

class Rectangle extends Shape {
    private final double w, h;
    Rectangle(double w, double h) { this.w = w; this.h = h; }
    @Override public double area() { return w * h; }
}

/* ============================
   10) equals/hashCode/toString overriding
   ============================ */
class Person {
    private final String name;
    private final int age;

    Person(String name, int age) {
        this.name = name; this.age = age;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Person p)) return false;
        return age == p.age && Objects.equals(name, p.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, age);
    }

    @Override
    public String toString() {
        return "Person{name='" + name + "', age=" + age + "}";
    }
}

/* ============================
   11) Generics + overriding (bridge method concept)
   ============================ */
class Box<T> extends javax.swing.Box {
    private final T value;
    Box(T value) {
        super(1);
        this.value = value; }
    public T get() { return value; }
}

class StringBox extends Box<String> {
    StringBox(String value) { super(value); }
    @Override public String get() { return super.get(); } // covariant, compiler emits bridge for Box#get():Object
}

/* ============================
   12) private methods are not overridden
   - PrivateBase.call() invokes PrivateBase.secret() even if subclass defines secret() with same signature.
   ============================ */
class PrivateBase {
    public void call() {
        secret(); // binds to PrivateBase.secret at compile-time (not virtual)
    }

    private void secret() {
        System.out.println("PrivateBase.secret");
    }
}

class PrivateChild extends PrivateBase {
    // Not an override; it's a new method unrelated to PrivateBase.secret (which is private).
    void secret() {
        System.out.println("PrivateChild.secret");
    }
}

/* ============================
   13) final methods cannot be overridden
   ============================ */
class FinalBase {
    public final void cannotOverride() {
        System.out.println("FinalBase.cannotOverride");
    }
}
// class FinalChild extends FinalBase {
//     @Override public void cannotOverride() {} // Illegal: cannot override final method
// }