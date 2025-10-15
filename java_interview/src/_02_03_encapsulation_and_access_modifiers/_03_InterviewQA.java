package _02_03_encapsulation_and_access_modifiers;

/*
Encapsulation & Access Modifiers – Interview Q&A with runnable examples.
Requires Java 17+ (records and sealed classes).

How to use:
- Run main() to see small demonstrations.
- Read the Q&A comments embedded throughout the code.

Key topics covered:
- Encapsulation basics, invariants, validation, defensive copies
- Public/protected/package-private/private semantics (+ tricky rules)
- Top-level vs member visibility
- Getters/setters vs behavior methods; immutability; builders
- Records, sealed classes, enums, utility classes
- Arrays/collections exposure and defensive techniques
- Overriding visibility rules, private across instances, private methods not overridden
- Package-private collaboration and testing practices
*/

/*
Q: What is encapsulation?
A: Bundling data and the operations on that data, while hiding implementation details. In Java, it is primarily achieved via access modifiers and designing APIs that preserve class invariants.

Q: Benefits?
- Protect invariants; validation centralized
- Limit the surface area for bugs and breaking changes
- Enable refactoring without affecting clients
- Improve thread-safety and maintainability
*/

public class _03_InterviewQA {
    public static void main(String[] args) {
        System.out.println("Encapsulation & Access Modifiers – Demo");

        // Encapsulation with validation
        Person p = new Person("Alice", 30);
        System.out.println(p.getName() + " age:" + p.getAge());
        try { p.setAge(-10); } catch (IllegalArgumentException e) { System.out.println("Validation works: " + e.getMessage()); }

        // Leaky vs safe encapsulation for mutable objects
        LeakyBag bag = new LeakyBag(new StringBuilder("secret"));
        StringBuilder ref = bag.getSecret(); // BAD: direct internal reference
        ref.append(" exposed");
        System.out.println("LeakyBag secret mutated externally: " + bag.getSecret());

        SafeBag safe = new SafeBag(new StringBuilder("secret"));
        StringBuilder safeRef = safe.getSecret(); // copy; safe
        safeRef.append(" exposed");
        System.out.println("SafeBag secret not mutated externally: " + safe.getSecret());

        // Defensive copies for arrays
        Student s = new Student("Bob", new int[]{90, 85});
        int[] scores = s.getScores(); // returns copy
        scores[0] = 0;
        System.out.println("Student score[0] still " + s.getScores()[0]);

        // Return unmodifiable collections
        Team t = new Team("Dev");
        t.addMember("A"); t.addMember("B");
        var view = t.getMembers();
        System.out.println("Team members view: " + view);
        try { view.add("C"); } catch (UnsupportedOperationException ex) { System.out.println("Unmodifiable view prevents external mutation"); }

        // Private members are accessible across instances of the same class
        PrivateAcrossInstances a1 = new PrivateAcrossInstances(10);
        PrivateAcrossInstances a2 = new PrivateAcrossInstances(20);
        a1.copyFrom(a2);
        System.out.println("Copied private value: " + a1.read());

        // Overriding: you can widen, not narrow, visibility
        BaseAction base = new BaseAction();
        base.doWork();
        SubAction sub = new SubAction();
        sub.doWork();

        // Utility class (no instances)
        System.out.println("Pi from Utility: " + Utility.PI);

        // Record with defensive copies for mutable component
        Credentials creds = new Credentials("user", new char[]{'p', 'a', 's', 's'});
        char[] exposed = creds.password(); // accessor returns clone
        exposed[0] = 'X';
        System.out.println("Record password still starts with: " + creds.password()[0]);

        // Sealed hierarchy constraining subtypes
        Vehicle v1 = new Car("Tesla");
        Vehicle v2 = new Bike("Yamaha");
        System.out.println(v1.name() + " and " + v2.name());

        // Immutable object via Builder
        Account acc = new Account.Builder().id("acc-1").owner("Carol").balance(1000).build();
        System.out.println("Account owner=" + acc.getOwner() + ", balance=" + acc.getBalance());

        // Package-private class: usable inside this package
        PackagePrivateCollaborator collaborator = new PackagePrivateCollaborator();
        collaborator.run();

        // Inner classes can access outer's private members
        Outer outer = new Outer(42);
        System.out.println("Inner sees outer secret: " + outer.revealByInner());

        // Private methods are not overridden
        PrivateMethodBase b1 = new PrivateMethodSub();
        b1.callFoo(); // calls base's private foo()

        System.out.println("Done.");
    }
}

/*
Q: How do access modifiers work?
- public: visible everywhere
- protected: visible in same package; and in subclasses in other packages (but only via subclass types, not base instances)
- (no modifier): package-private; visible only within the same package
- private: visible only within the top-level or nested class where declared (but any instance of that class can access it)

Q: Top-level types can be public or package-private. They cannot be protected or private.
*/

/* BASIC: Encapsulation with validation and behavior-oriented API */
class Person {
    /*
    Q: Why prefer private fields with getters/setters over public fields?
    A: Allows validation, lazy computation, invariants, and future-proofing.
    */
    private String name;
    private int age;

    public Person(String name, int age) {
        setName(name);
        setAge(age);
    }

    public String getName() { return name; }

    public void setName(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name required");
        this.name = name;
    }

    public int getAge() { return age; }

    public void setAge(int age) {
        if (age < 0 || age > 150) throw new IllegalArgumentException("invalid age: " + age);
        this.age = age;
    }

    // Prefer domain behavior over setX(): encapsulates how state changes.
    public void haveBirthday() {
        if (age >= 150) throw new IllegalStateException("maximum age reached");
        this.age++;
    }
}

/* BASIC: Anti-pattern – exposing internal mutable state */
class LeakyBag {
    /*
    Q: What's wrong here?
    A: getSecret() exposes the internal StringBuilder, letting callers mutate our state.
    */
    private StringBuilder secret;

    public LeakyBag(StringBuilder secret) {
        if (secret == null) throw new IllegalArgumentException("secret required");
        this.secret = secret; // leaks!
    }

    public StringBuilder getSecret() { // BAD: direct reference
        return secret;
    }
}

/* BASIC: Fix with defensive copies */
class SafeBag {
    private final StringBuilder secret;

    public SafeBag(StringBuilder secret) {
        if (secret == null) throw new IllegalArgumentException("secret required");
        // Defensive copy
        this.secret = new StringBuilder(secret.toString());
    }

    public StringBuilder getSecret() {
        // Return a copy to prevent external mutation
        return new StringBuilder(secret.toString());
    }
}

/* BASIC: Arrays require defensive copies in constructor and getters */
class Student {
    private final String name;
    private final int[] scores;

    public Student(String name, int[] scores) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name required");
        if (scores == null) throw new IllegalArgumentException("scores required");
        this.name = name;
        this.scores = scores.clone(); // defensive copy
    }

    public String getName() { return name; }

    public int[] getScores() {
        return scores.clone(); // defensive copy
    }
}

/* BASIC: Unmodifiable view for collections to prevent external mutation */
class Team {
    private final String name;
    private final java.util.List<String> members = new java.util.ArrayList<>();

    public Team(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name required");
        this.name = name;
    }

    public void addMember(String member) {
        if (member == null || member.isBlank()) throw new IllegalArgumentException("member required");
        members.add(member);
    }

    public java.util.List<String> getMembers() {
        // Returns a read-only view; internal list can only be changed via addMember()
        return java.util.Collections.unmodifiableList(members);
    }
}

/* INTERMEDIATE: Private across instances – same class may access private members of another instance */
class PrivateAcrossInstances {
    /*
    Q: Can you access private fields of another instance of the same class?
    A: Yes. Access control is on the declaring class, not on the instance.
    */
    private int value;

    public PrivateAcrossInstances(int value) { this.value = value; }

    public void copyFrom(PrivateAcrossInstances other) {
        this.value = other.value; // allowed
    }

    public int read() { return value; }
}

/* INTERMEDIATE: Overriding visibility – cannot narrow, can widen */
class BaseAction {
    /*
    Q: Can a subclass reduce the visibility of an overridden method?
    A: No. It can keep or widen (e.g., protected -> public), but not narrow.
    */
    protected void doWork() {
        System.out.println("BaseAction doing work");
    }
}

class SubAction extends BaseAction {
    @Override
    public void doWork() { // widened from protected to public
        System.out.println("SubAction doing work");
    }
}

/* INTERMEDIATE: Utility class with private constructor to prevent instantiation */
final class Utility {
    private Utility() { throw new AssertionError("No instances"); }
    public static final double PI = Math.PI;
}

/* INTERMEDIATE: Records and encapsulation */
record Credentials(String user, char[] password) {
    /*
    Q: Are records immutable?
    A: The reference fields are final, but if they reference mutable objects (e.g., arrays),
       you MUST defensively copy to preserve encapsulation.
    */
    public Credentials {
        if (user == null || user.isBlank()) throw new IllegalArgumentException("user required");
        if (password == null) throw new IllegalArgumentException("password required");
        password = password.clone(); // defensive copy on input
    }

    @Override
    public char[] password() {
        return password.clone(); // defensive copy on output
    }

    public void clearPassword() {
        java.util.Arrays.fill(password, '\0'); // internal mutation is allowed within the record
    }
}

/* INTERMEDIATE: Sealed classes/interfaces restrict extension to known set */
sealed interface Vehicle permits Car, Bike {
    String name();
}

final class Car implements Vehicle {
    private final String name;
    public Car(String name) { this.name = name; }
    @Override public String name() { return name; }
}

non-sealed class Bike implements Vehicle {
    private final String name;
    public Bike(String name) { this.name = name; }
    @Override public String name() { return name; }
}

/* INTERMEDIATE: Immutable object with Builder */
final class Account {
    /*
    Q: Why prefer immutability?
    A: Thread-safety by default, simpler reasoning, safe sharing, stronger invariants.
    */
    private final String id;
    private final String owner;
    private final java.math.BigDecimal balance;

    private Account(Builder b) {
        this.id = b.id;
        this.owner = b.owner;
        this.balance = b.balance; // BigDecimal is immutable
    }

    public String getId() { return id; }
    public String getOwner() { return owner; }
    public java.math.BigDecimal getBalance() { return balance; }

    public static class Builder {
        private String id;
        private String owner;
        private java.math.BigDecimal balance = java.math.BigDecimal.ZERO;

        public Builder id(String id) { this.id = id; return this; }
        public Builder owner(String owner) { this.owner = owner; return this; }
        public Builder balance(double amount) { this.balance = java.math.BigDecimal.valueOf(amount); return this; }

        public Account build() {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("id required");
            if (owner == null || owner.isBlank()) throw new IllegalArgumentException("owner required");
            if (balance.signum() < 0) throw new IllegalArgumentException("negative balance");
            return new Account(this);
        }
    }
}

/* INTERMEDIATE: Package-private class – accessible only within this package */
class PackagePrivateCollaborator {
    /*
    Q: When to use package-private?
    A: For internal collaboration and to keep the public API small. Also handy for tests in the same package.
    */
    void run() {
        System.out.println("I'm package-private: visible inside package only.");
    }
}

/* ADVANCED: Inner classes can access outer's private members */
class Outer {
    /*
    Q: How can an inner class access private members of its enclosing class?
    A: Language allows it; compiler will generate synthetic accessors if needed.
    */
    private int secret;
    Outer(int secret) { this.secret = secret; }

    class Inner {
        int read() { return secret; } // access private
    }

    int revealByInner() { return new Inner().read(); }
}

/* ADVANCED: Private methods are not overridden */
class PrivateMethodBase {
    /*
    Q: Can you override a private method?
    A: No. Private methods are not inherited. A method with the same signature in a subclass is a new method.
    */
    private void foo() { System.out.println("base foo"); }
    public void callFoo() { foo(); }
}

class PrivateMethodSub extends PrivateMethodBase {
    // This does NOT override; it's a new public method.
    public void foo() { System.out.println("sub foo"); }
}

/*
Additional Q&A (read-only, not runnable):

Q: protected across packages?
A: In a different package, a protected member is accessible only to subclasses, and only via the subclass type (this or a reference of the subclass), not via a base-class reference. E.g., new OtherPackageBase().protectedMember is not accessible in a different package unless accessed through a subclass instance.

Q: Interface members visibility?
A: Interface fields are implicitly public static final. Interface methods are implicitly public; default and static methods are public too.

Q: Class-level access:
- Top-level: public or package-private only.
- Member classes (nested): can be public/protected/package-private/private.

Q: Encapsulation pitfalls:
- Returning internal mutable state (arrays, lists, Date, StringBuilder, char[])
- Accepting external mutable references without copying
- Non-final fields in "immutable" classes
- Leaking internal collections by returning them directly (use unmodifiable views or copies)

Q: Defensive copies:
- In constructor: copy incoming mutable inputs
- In getters: return copies or unmodifiable views
- Example with Date:
  class Meeting {
    private final java.util.Date when;
    Meeting(java.util.Date when) { this.when = new java.util.Date(when.getTime()); }
    public java.util.Date when() { return new java.util.Date(when.getTime()); }
  }

Q: Composition vs inheritance for encapsulation?
A: Prefer composition; inheritance can expose internals and tighten coupling. Composition lets you keep a narrower public API.

Q: final and encapsulation?
A: final fields support immutability; final classes prevent unwanted extension that could break invariants; final methods prevent overriding sensitive behavior.

Q: Modules (Java 9+) and encapsulation?
A: Modules add a higher-level boundary. A package is accessible outside its module only if exported. Reflection can be limited across modules (strong encapsulation).

Q: Enums:
A: Enums are implicitly final with private constructors; useful for singletons and fixed sets of constants.

Q: Reflection breaking encapsulation?
A: setAccessible(true) can break encapsulation, though module boundaries and security policies can restrict it. Avoid relying on reflection to access internals.

Q: Overexposing setters?
A: Prefer behavior methods or builders. Setters can make it hard to maintain invariants and thread-safety.

Q: Narrowing vs widening access when overriding?
A: You may widen (protected -> public), but not narrow (public -> protected/private). Compiler enforces this.
*/