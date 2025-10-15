package _02_05_polymorphism_and_overriding;

import java.io.*;
import java.lang.reflect.Method;
import java.util.Objects;

/*
Interview Q&A: Polymorphism & Overriding (basic → advanced)
- What is polymorphism? Runtime vs compile-time (overriding vs overloading)
- Dynamic dispatch; reference type vs object type
- Upcasting and downcasting (ClassCastException safety)
- Overriding rules: same signature, covariant return, cannot reduce visibility
- Exceptions: cannot throw broader checked exceptions; unchecked allowed
- final/static/private/constructors: not overridden; static/fields are hidden
- super to access parent behavior
- Virtual calls in constructors are dangerous
- @Override helps catch errors
- equals/hashCode pitfalls with inheritance (symmetry, LSP)
- Interfaces: default methods; conflict resolution with Interface.super
- Overload resolution: boxing vs varargs vs widening; null calls
- Generics: type erasure and bridge methods ensure polymorphism

Run main() to see concise demos. See inline comments for Q&A per topic.
*/
public class _03_InterviewQA {
    public static void main(String[] args) {
        System.out.println("1) Runtime polymorphism (overriding) and dynamic dispatch");
        Animal a1 = new Animal();
        Animal a2 = new Dog();
        Animal a3 = new Cat();
        a1.speak();
        a2.speak();
        a3.speak();

        System.out.println("2) Upcasting and downcasting");
        Animal up = new Dog(); // upcast (implicit)
        if (up instanceof Dog) {
            Dog down = (Dog) up; // safe downcast
            down.speak("happily");
        }
        Animal justCat = new Cat();
        if (justCat instanceof Dog) {
            ((Dog) justCat).speak("oops");
        } else {
            System.out.println("Safe: justCat is not a Dog");
        }

        System.out.println("3) Overloading vs overriding (compile-time vs runtime)");
        Greeter g = new Greeter();
        Dog realDog = new Dog();
        Animal dogAsAnimal = realDog;
        g.greet(realDog);      // greet(Dog) chosen at compile time
        g.greet(dogAsAnimal);  // greet(Animal) chosen at compile time

        System.out.println("4) Access modifiers: cannot reduce visibility; can widen");
        AccessBase ab = new AccessDerived();
        ab.doWork();

        System.out.println("5) Static methods are hidden, not overridden");
        Animal.staticSpeak();
        Dog.staticSpeak();
        Animal ref = new Dog();
        ref.staticSpeak(); // compile-time binding -> Animal.staticSpeak

        System.out.println("6) final/private/constructor cannot be overridden");
        Dog d = new Dog();
        d.finalMethod();  // from Animal
        d.callPrivate();  // calls Animal.privateMethod, not Dog.privateMethod

        System.out.println("7) Checked exception rules in overriding");
        BaseService s1 = new NarrowingService();
        try {
            s1.ioOperation();
        } catch (IOException e) {
            System.out.println("Caught: " + e.getClass().getSimpleName());
        }
        BaseService s2 = new NoThrowService();
        try {
            s2.ioOperation();
        } catch (IOException e) {
            System.out.println("Won't happen");
        }

        System.out.println("8) Covariant return types");
        CreatorBase cb = new CreatorDerived();
        Number n = cb.create();
        System.out.println("cb.create() returns " + n.getClass().getSimpleName());
        CreatorDerived cd = new CreatorDerived();
        Integer in = cd.create();
        System.out.println("cd.create() returns " + in.getClass().getSimpleName());

        System.out.println("9) Fields are hidden, not polymorphic");
        Animal dogAsAnimal2 = new Dog();
        System.out.println("animal.name=" + dogAsAnimal2.name);          // AnimalField
        System.out.println("dog.name=" + ((Dog) dogAsAnimal2).name);     // DogField

        System.out.println("10) super in overridden methods");
        d.speak();

        System.out.println("11) Calling overridable from constructor is dangerous");
        new CtorDerived();

        System.out.println("12) @Override catches mistakes (see comments)");

        System.out.println("13) equals/hashCode and polymorphism pitfalls");
        Point p = new Point(1, 2);
        ColorPoint cp = new ColorPoint(1, 2, "red");
        System.out.println("p.equals(cp) = " + p.equals(cp)); // true
        System.out.println("cp.equals(p) = " + cp.equals(p)); // false (breaks symmetry)

        System.out.println("14) Interface default methods and conflict resolution");
        Multi m = new Multi();
        m.hello();

        System.out.println("15) Overload resolution: boxing vs varargs vs widening");
        OverloadResolution or = new OverloadResolution();
        or.m(5);                 // m(Integer) via boxing (beats varargs)
        or.m(Integer.valueOf(5));// m(Integer)
        or.m();                  // m(int...) varargs
        or.m((Object) null);     // m(Object)
        or.m((Integer) null);    // m(Integer) (more specific than Object)
        or.m(1, 2, 3);           // m(int...)

        System.out.println("16) Generics and bridge methods");
        for (Method method : GenericDerived.class.getDeclaredMethods()) {
            if (method.getName().equals("get")) {
                System.out.println("method: " + method + " bridge=" + method.isBridge() + " synthetic=" + method.isSynthetic());
            }
        }

        System.out.println("17) Interface polymorphism");
        Speakable sp1 = new Dog();
        Speakable sp2 = new Cat();
        sp1.speak();
        sp2.speak();

        System.out.println("18) Overriding and synchronized/throws not part of signature (see comments)");

        System.out.println("Done");
    }

    // Q: What is polymorphism? A: Same interface, different implementations chosen at runtime via overriding.
    static class Animal implements Speakable {
        public String name = "AnimalField";

        @Override
        public void speak() {
            System.out.println("Animal.speak");
        }

        public static void staticSpeak() {
            System.out.println("Animal.staticSpeak");
        }

        public final void finalMethod() {
            System.out.println("Animal.finalMethod (cannot be overridden)");
        }

        private void privateMethod() {
            System.out.println("Animal.privateMethod (not visible to subclasses)");
        }

        public void callPrivate() {
            privateMethod();
        }
    }

    static class Dog extends Animal {
        public String name = "DogField"; // hides Animal.name

        @Override
        public void speak() {
            System.out.println("Dog.speak (overrides)");
            super.speak(); // access parent behavior
        }

        public void speak(String mood) { // overload, not override
            System.out.println("Dog.speak with mood=" + mood);
        }

        public static void staticSpeak() { // hides, not overrides
            System.out.println("Dog.staticSpeak (hides Animal.staticSpeak)");
        }

        // @Override // compile error: cannot override final method
        // public void finalMethod() {}

        // @Override // compile error: private method in superclass not visible; this is a new method
        // private void privateMethod() {}

        // @Override // compile error: method name typo -> speek
        // public void speek() {}
    }

    static class Cat extends Animal {
        @Override
        public void speak() {
            System.out.println("Cat.speak (overrides)");
        }
    }

    interface Speakable {
        void speak();
    }

    // Q: Overloading vs overriding?
    static class Greeter {
        public void greet(Animal a) { System.out.println("greet(Animal) chosen at compile-time"); }
        public void greet(Dog d) { System.out.println("greet(Dog) chosen at compile-time"); }
    }

    // Q: Access modifier rules when overriding?
    static class AccessBase {
        protected void doWork() { System.out.println("AccessBase.doWork (protected)"); }
    }

    static class AccessDerived extends AccessBase {
        @Override
        public void doWork() { System.out.println("AccessDerived.doWork (public)"); }
        // Cannot reduce visibility to private/protected
    }

    // Q: Checked exceptions rules?
    static class BaseService {
        public void ioOperation() throws IOException {
            // default throws
            throw new IOException("BaseService");
        }
    }

    static class NarrowingService extends BaseService {
        @Override
        public void ioOperation() throws FileNotFoundException { // narrower checked exception OK
            throw new FileNotFoundException("NarrowingService");
        }
    }

    // static class WideningService extends BaseService {
    //     @Override
    //     public void ioOperation() throws Exception { // broader checked exception -> compile error
    //         throw new Exception("illegal");
    //     }
    // }

    static class UncheckedService extends BaseService {
        @Override
        public void ioOperation() throws RuntimeException { // adding unchecked is OK
            throw new RuntimeException("UncheckedService");
        }
    }

    static class NoThrowService extends BaseService {
        @Override
        public void ioOperation() { // throwing nothing is OK
            System.out.println("NoThrowService");
        }
    }

    // Q: Covariant return types?
    static class CreatorBase {
        public Number create() { return 42; }
    }

    static class CreatorDerived extends CreatorBase {
        @Override
        public Integer create() { return 7; } // covariant return
    }

    // Q: Calling overridable method from constructor pitfalls?
    static class CtorBase {
        CtorBase() {
            System.out.println("CtorBase: before init()");
            init(); // virtual call
            System.out.println("CtorBase: after init()");
        }

        void init() { System.out.println("CtorBase.init"); }
    }

    static class CtorDerived extends CtorBase {
        private String text = "ready";

        CtorDerived() {
            System.out.println("CtorDerived: constructor body, text=" + text);
        }

        @Override
        void init() {
            System.out.println("CtorDerived.init: text=" + text); // prints null: subclass fields not yet initialized
        }
    }

    // Q: equals/hashCode and inheritance pitfalls
    static class Point {
        final int x, y;

        Point(int x, int y) { this.x = x; this.y = y; }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Point)) return false; // allows subclasses -> can break symmetry
            Point p = (Point) o;
            return x == p.x && y == p.y;
        }

        @Override
        public int hashCode() { return Objects.hash(x, y); }
    }

    static class ColorPoint extends Point {
        final String color;

        ColorPoint(int x, int y, String color) { super(x, y); this.color = color; }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ColorPoint)) return false; // stricter: breaks symmetry with Point
            ColorPoint cp = (ColorPoint) o;
            return super.equals(o) && Objects.equals(color, cp.color);
        }

        @Override
        public int hashCode() { return Objects.hash(super.hashCode(), color); }
    }

    // Q: Interface default methods and conflicts
    interface A {
        default void hello() { System.out.println("A.hello"); }
    }

    interface B {
        default void hello() { System.out.println("B.hello"); }
    }

    static class Multi implements A, B {
        @Override
        public void hello() {
            A.super.hello();
            B.super.hello();
            System.out.println("Multi.hello (resolves conflict)");
        }
    }

    // Q: Overload resolution details
    static class OverloadResolution {
        public void m(Integer x) { System.out.println("m(Integer)"); }
        public void m(int... xs) { System.out.println("m(int...)"); }
        public void m(Object o) { System.out.println("m(Object)"); }
        // If you also had m(String), then m(null) would be ambiguous -> compile error
    }

    // Q: Generics and bridge methods to preserve polymorphism after type erasure
    static class GenericBase<T> {
        public T get() { return null; }
    }

    static class GenericDerived extends GenericBase<String> {
        @Override
        public String get() { return "generic"; }
        // Compiler emits a synthetic bridge: Object get() delegating to String get()
    }

    // Misc:
    // - synchronized is not part of method signature; overriding method may add/remove it.
    // - Parameters must match exactly; only return types can be covariant in overriding.
}