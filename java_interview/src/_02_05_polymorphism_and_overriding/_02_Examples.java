package _02_05_polymorphism_and_overriding;

/*
 Polymorphism & Overriding examples

 Topics covered:
 1) Basic overriding and dynamic dispatch (runtime polymorphism)
 2) Upcasting and downcasting with subtype-specific behavior
 3) @Override and visibility rules (cannot reduce visibility)
 4) Covariant return types
 5) Exceptions with overriding (checked exceptions: narrower or none)
 6) Static methods are hidden (not overridden)
 7) final methods cannot be overridden
 8) private methods are not overridden
 9) Fields are not polymorphic (field hiding)
10) Overloading vs Overriding (compile-time vs runtime)
11) Interfaces and dynamic dispatch (+ default methods)
12) Using super to extend parent behavior
*/
public class _02_Examples {

    public static void main(String[] args) {
        System.out.println("1) Basic overriding & dynamic dispatch");
        Animal[] animals = {
                new Animal("Generic"),
                new Dog("Rex"),
                new Cat("Misty")
        };
        for (Animal a : animals) {
            System.out.println(a + " speaks: " + a.speak());
        }

        System.out.println("\n2) Upcasting and downcasting");
        Animal ref = new Dog("Buddy");        // upcast (implicit)
        System.out.println("As Animal, ref.speak(): " + ref.speak()); // Dog.speak() at runtime
        if (ref instanceof Dog) {
            Dog dog = (Dog) ref;              // safe downcast
            System.out.println("Downcast to Dog: " + dog.fetch());
        }

        System.out.println("\n3) @Override and visibility rules");
        Animal a = new Animal("Base");
        Dog d = new Dog("Max");
        System.out.println("Animal.identify(): " + a.identify());
        System.out.println("Dog.identify(): " + d.identify()); // overrides and uses super
        // Animal.onlyInAnimal() is protected; Dog overrides with increased visibility (public)
        System.out.println("Dog.onlyInAnimal() (visibility increased): " + d.onlyInAnimal());
        // The following would NOT compile (cannot reduce visibility):
        // In Dog:
        // @Override
        // private String onlyInAnimal() { return "No"; }

        System.out.println("\n4) Covariant return types");
        Dog d1 = new Dog("Cooper");
        Dog puppy = d1.reproduce();                  // returns Dog (covariant)
        Animal asAnimal = ((Animal) d1).reproduce(); // still OK as Animal
        System.out.println("Dog.reproduce() returned: " + puppy);
        System.out.println("As Animal via covariant return: " + asAnimal);

        System.out.println("\n5) Exceptions in overriding (narrower checked exceptions)");
        Animal riskyRef = new Dog("Risky");
        try {
            riskyRef.riskyAction(); // Dog throws DogCheckedException (a subclass of BaseCheckedException)
        } catch (BaseCheckedException e2) {
            System.out.println("Caught: " + e2.getClass().getSimpleName() + " -> " + e2.getMessage());
        }
        Animal safeCat = new Cat("Careful");
        try {
            safeCat.riskyAction(); // Cat overrides without throwing
            System.out.println("Cat riskyAction() completed without exception");
        } catch (BaseCheckedException e3) {
            // won't happen here
        }

        System.out.println("\n6) Static methods are hidden, not overridden");
        Animal refStatic = new Dog("Static");
        System.out.println("Animal.describeType(): " + Animal.describeType());
        System.out.println("Dog.describeType(): " + Dog.describeType());
        // Using an instance reference to call a static method compiles, but resolves to the reference type (Animal)
        System.out.println("refStatic.describeType() (resolved by reference type): " + refStatic.describeType());

        System.out.println("\n7) final methods cannot be overridden");
        System.out.println("Dog.finalInfo(): " + d.finalInfo());
        // The following would NOT compile in Dog:
        // @Override
        // public String finalInfo() { return "Override final"; }

        System.out.println("\n8) private methods are not overridden");
        System.out.println("Animal.revealSecret(): " + a.revealSecret());
        System.out.println("Dog.revealSecret() (inherited from Animal): " + d.revealSecret()); // still Animal's private
        System.out.println("Dog.revealOwnSecret(): " + d.revealOwnSecret()); // Dog's own private method

        System.out.println("\n9) Fields are not polymorphic (field hiding)");
        Animal af = new Dog("Fieldy");
        System.out.println("af.kind (by reference type Animal): " + af.kind);
        System.out.println("((Dog) af).kind (by reference type Dog): " + ((Dog) af).kind);

        System.out.println("\n10) Overloading vs overriding");
        Feeder feeder = new Feeder();
        Animal dogAsAnimal = new Dog("Overload-Dog");
        Dog dogTyped = new Dog("Overload-Dog");
        feeder.feed(dogAsAnimal); // chooses feed(Animal) at compile time
        feeder.feed(dogTyped);    // chooses feed(Dog) at compile time
        Animal catAsAnimal = new Cat("Overload-Cat");
        feeder.feed(catAsAnimal);           // feed(Animal) (not feed(Cat)), because reference type is Animal
        feeder.feed((Cat) catAsAnimal);     // feed(Cat), after cast

        System.out.println("\n11) Interfaces and dynamic dispatch (+ default methods)");
        Notifier[] notifiers = { new EmailNotifier(), new SmsNotifier() };
        for (Notifier n : notifiers) {
            n.notifyUser("Hello via " + n.getClass().getSimpleName());
        }
        Greeter[] greeters = { new FriendlyDog("Bolt"), new Robot() };
        for (Greeter g : greeters) {
            System.out.println(g.getClass().getSimpleName() + " greets: " + g.greet());
        }

        System.out.println("\n12) Using super to extend parent behavior");
        System.out.println("Dog.identify() uses super: " + d.identify());

        // Additional notes:
        // - final classes cannot be extended:
        //   final class FinalAnimal {}
        //   class TryExtend extends FinalAnimal {} // does not compile
        // - Constructors are not overridden (they are not inherited).
    }
}

/* =========================
   Model classes and helpers
   ========================= */

class Animal {
    public final String name;
    public String kind = "Animal";

    Animal(String name) {
        this.name = name;
    }

    // Basic virtual method
    String speak() {
        return "generic sound";
    }

    // Visible in package and subclasses; Dog will widen to public
    protected String onlyInAnimal() {
        return "Protected in Animal for " + name;
    }

    // Demonstrates overriding + super
    String identify() {
        return "I am an Animal named " + name;
    }

    // final method: cannot be overridden
    public final String finalInfo() {
        return "Final info from Animal for " + name;
    }

    // private method: not overridden
    private String secret() {
        return "Animal secret of " + name;
    }

    // Calls Animal.secret() even in subclasses (because secret() is private here)
    public String revealSecret() {
        return secret();
    }

    // Static method: hidden (not overridden)
    public static String describeType() {
        return "Animal static description";
    }

    // Covariant return example: subclasses may return a subtype
    Animal reproduce() {
        return new Animal(name + " Jr.");
    }

    // Checked exception in base
    void riskyAction() throws BaseCheckedException {
        throw new BaseCheckedException("Animal riskyAction for " + name);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + name + ")";
    }
}

class Dog extends Animal {
    public String kind = "Dog";

    Dog(String name) {
        super(name);
    }

    @Override
    public String speak() {
        // Overridden; still can use super if we want to extend behavior
        return super.speak() + " -> Woof!";
    }

    // Widening visibility (protected -> public) is allowed
    @Override
    public String onlyInAnimal() {
        return "Now public in Dog for " + name;
    }

    @Override
    public String identify() {
        // Using super to include parent logic
        return "I am a Dog named " + name + " | super: [" + super.identify() + "]";
    }

    public String fetch() {
        return name + " fetches a ball!";
    }

    // Static method hiding
    public static String describeType() {
        return "Dog static description";
    }

    // Covariant return: returns Dog (a subtype of Animal)
    @Override
    public Dog reproduce() {
        return new Dog(name + " Pup");
    }

    // Narrower checked exception than base method
    @Override
    public void riskyAction() throws DogCheckedException {
        throw new DogCheckedException("Dog riskyAction for " + name);
    }

    // This is NOT an override of Animal.secret() (that one is private)
    private String secret() {
        return "Dog secret of " + name;
    }

    public String revealOwnSecret() {
        return secret();
    }
}

class Cat extends Animal {
    public String kind = "Cat";

    Cat(String name) {
        super(name);
    }

    @Override
    String speak() {
        return "Meow!";
    }

    // Static method hiding
    public static String describeType() {
        return "Cat static description";
    }

    @Override
    public Cat reproduce() {
        return new Cat(name + " Kitten");
    }

    // Removes checked exception entirely (narrower than base)
    @Override
    void riskyAction() {
        // safe
    }

    public String scratch() {
        return name + " scratches!";
    }
}

/* Exceptions for example 5 */
class BaseCheckedException extends Exception {
    BaseCheckedException(String message) {
        super(message);
    }
}

class DogCheckedException extends BaseCheckedException {
    DogCheckedException(String message) {
        super(message);
    }
}

/* Overloading example for 10 */
class Feeder {
    public void feed(Animal a) {
        System.out.println("Feeding an Animal generically: " + a.name);
    }

    public void feed(Dog d) {
        System.out.println("Feeding a Dog with bones: " + d.name);
    }

    public void feed(Cat c) {
        System.out.println("Feeding a Cat with fish: " + c.name);
    }
}

/* Interfaces example for 11 */
interface Notifier {
    void notifyUser(String message);
}

class EmailNotifier implements Notifier {
    @Override
    public void notifyUser(String message) {
        System.out.println("[Email] " + message);
    }
}

class SmsNotifier implements Notifier {
    @Override
    public void notifyUser(String message) {
        System.out.println("[SMS] " + message);
    }
}

/* Default methods and overriding default */
interface Greeter {
    default String greet() {
        return "Hello from Greeter";
    }
}

class FriendlyDog extends Dog implements Greeter {
    FriendlyDog(String name) {
        super(name);
    }

    @Override
    public String greet() {
        return "Wag! Woof! Hi, I'm " + name;
    }
}

class Robot implements Greeter {
    // Uses default greet() from Greeter
}