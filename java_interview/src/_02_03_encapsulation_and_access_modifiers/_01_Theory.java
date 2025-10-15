package _02_03_encapsulation_and_access_modifiers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Encapsulation & Access Modifiers — one-file, commented walkthrough.
 *
 * Topics covered in code and comments:
 * - Encapsulation: hide state, expose behavior, enforce invariants
 * - Access modifiers: public, protected, package-private (no modifier), private
 * - Top-level vs member type access
 * - Getters/setters vs immutability vs factories
 * - Defensive copies for arrays and collections
 * - Privacy leaks and how to prevent them
 * - Overriding and visibility rules (cannot reduce visibility)
 * - Nested classes and their access to outer private members
 * - Package-private top-level classes
 * - Interface member visibility
 *
 * Notes:
 * - Only one public top-level class is allowed per file; it must match the file name.
 * - Top-level classes can be public or package-private only; they cannot be private or protected.
 * - protected: accessible to subclasses and also to any class in the same package.
 * - From a different package, protected members are accessible only to subclasses, and only via the subclass type/instance.
 */
public class _01_Theory {

    public static void main(String[] args) {
        System.out.println("== Encapsulation & Access Modifiers demo ==");

        // 1) No encapsulation: public fields (bad)
        BadPerson bp = new BadPerson();
        bp.name = "Alice";
        bp.age = -10; // breaks invariant
        System.out.println("BadPerson allows invalid age: " + bp.age);

        // 2) Encapsulated: private fields + validation
        Person p = new Person(1L, "Bob", 20);
        try {
            p.setAge(-5); // throws
        } catch (IllegalArgumentException e) {
            System.out.println("Validation stopped invalid age: " + e.getMessage());
        }
        p.setAge(21);
        System.out.println("Person after valid change: " + p);

        // 3) Privacy leak with collections and the fix
        TeamWithLeak tl = new TeamWithLeak();
        tl.getMembers().add(p); // external mutation allowed
        System.out.println("TeamWithLeak size after external add: " + tl.getMembers().size());

        TeamWithDefensiveCopies ts = new TeamWithDefensiveCopies(Arrays.asList(p));
        try {
            ts.getMembers().add(new Person(2L, "Carol", 30)); // throws
        } catch (UnsupportedOperationException e) {
            System.out.println("Unmodifiable view prevents external add.");
        }

        // 4) Defensive copy for arrays (avoid leaking internal representation)
        int[] sizes = {1, 2, 3};
        LibraryUnsafe lu = new LibraryUnsafe(sizes);
        LibrarySafe ls = new LibrarySafe(sizes);
        sizes[0] = 99; // mutates original array
        System.out.println("LibraryUnsafe shelf[0] reflects external change: " + lu.getShelves()[0]);
        System.out.println("LibrarySafe shelf[0] unaffected by external change: " + ls.getShelves()[0]);

        // 5) Access modifiers within same package
        PeerInSamePackage.demonstrate();

        // 6) Nested classes can access outer's private members
        Outer outer = new Outer();
        Outer.StaticNested sn = new Outer.StaticNested();
        Outer.Inner in = outer.new Inner();
        System.out.println("StaticNested reveal: " + sn.reveal(outer));
        System.out.println("Inner reveal: " + in.reveal());

        // 7) Private constructor + factory controls instantiation
        OnlyFactory of = OnlyFactory.of("valid");
        System.out.println("OnlyFactory instance: " + of);

        // 8) Overriding cannot reduce visibility (here we increase to public)
        BaseVisibility base = new BaseVisibility();
        SubVisibility sub = new SubVisibility();
        System.out.println("BaseVisibility whoAmI: " + base.whoAmI());
        System.out.println("SubVisibility whoAmI: " + sub.whoAmI());

        // 9) Package-private top-level class is visible inside the same package
        PackagePrivateHelper helper = new PackagePrivateHelper();
        System.out.println("Helper says: " + helper.help());

        // 10) Immutability example
        ImmutablePoint point = new ImmutablePoint(5, 7);
        System.out.println("ImmutablePoint: " + point);

        // 11) Immutable class with defensive copies for mutable fields
        Date now = new Date();
        ImmutableEvent event = new ImmutableEvent(now);
        now.setTime(0L); // attempt to mutate original input
        System.out.println("ImmutableEvent date unaffected by external input change: " + event.when());
        Date leaked = event.when();
        leaked.setTime(12345L); // attempt to mutate returned instance
        System.out.println("ImmutableEvent date still unaffected after getter defensive copy: " + event.when());

        // Interfaces: members are implicitly public; fields are public static final
        Readable reader = new Reader();
        reader.read();
        System.out.println("Readable.MAX = " + Readable.MAX);

        System.out.println("== Done ==");
    }
}

/**
 * Anti-pattern: public fields expose representation. No invariants are enforced.
 */
class BadPerson {
    public String name;
    public int age;
}

/**
 * Encapsulated class: state is private. Invariants enforced in constructor and mutators.
 * Demonstrates:
 * - private fields
 * - read-only id (final)
 * - validation in constructor and setter
 * - no setter for id to keep identity stable
 */
class Person {
    private final long id;
    private String name;
    private int age;

    public Person(long id, String name, int age) {
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("name required");
        if (age < 0) throw new IllegalArgumentException("age must be >= 0");
        this.id = id;
        this.name = name.trim();
        this.age = age;
    }

    public long getId() { return id; }

    public String getName() { return name; }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("name required");
        this.name = name.trim();
    }

    public int getAge() { return age; }

    public void setAge(int age) {
        if (age < 0) throw new IllegalArgumentException("age must be >= 0");
        this.age = age;
    }

    @Override public String toString() {
        return "Person{id=" + id + ", name='" + name + "', age=" + age + "}";
    }
}

/**
 * Privacy leak: returning internal, mutable collection allows callers to mutate your state.
 */
class TeamWithLeak {
    private final List<Person> members = new ArrayList<>();

    public List<Person> getMembers() {
        // BAD: exposes internal list; callers can add/remove directly.
        return members;
    }

    public void addMember(Person p) {
        if (p == null) throw new IllegalArgumentException("member required");
        members.add(p);
    }
}

/**
 * Fix: defensive copy on input; unmodifiable view on output.
 * Note: The list is unmodifiable, but Person objects inside are still mutable.
 * Deep immutability would require immutable element type as well.
 */
class TeamWithDefensiveCopies {
    private final List<Person> members;

    public TeamWithDefensiveCopies(List<Person> initialMembers) {
        if (initialMembers == null) throw new IllegalArgumentException("list required");
        this.members = new ArrayList<>(initialMembers); // defensive copy
    }

    public List<Person> getMembers() {
        return Collections.unmodifiableList(members); // read-only view
    }

    public void addMember(Person p) {
        if (p == null) throw new IllegalArgumentException("member required");
        members.add(p);
    }
}

/**
 * Demonstrates array privacy leak: arrays are mutable and should be defensively copied.
 */
class LibraryUnsafe {
    private final int[] shelves;

    public LibraryUnsafe(int[] shelves) {
        if (shelves == null) throw new IllegalArgumentException("shelves required");
        this.shelves = shelves; // stores external array reference (leak)
    }

    public int[] getShelves() {
        return shelves; // returns internal array directly (leak)
    }
}

/**
 * Fix for arrays: clone on input and output.
 */
class LibrarySafe {
    private final int[] shelves;

    public LibrarySafe(int[] shelves) {
        if (shelves == null) throw new IllegalArgumentException("shelves required");
        this.shelves = shelves.clone(); // defensive copy
    }

    public int[] getShelves() {
        return shelves.clone(); // defensive copy
    }
}

/**
 * Fields with different access levels.
 * - public: everywhere
 * - protected: same package + subclasses
 * - (no modifier) package-private: same package only
 * - private: enclosing class only
 */
class AccessShowcase {
    public int aPublic = 1;
    protected int aProtected = 2;
    int aPackagePrivate = 3; // package-private (default)
    private int aPrivate = 4;

    public int sumVisibleInside() {
        return aPublic + aProtected + aPackagePrivate + aPrivate;
    }
}

/**
 * Same-package peer can access public, protected, and package-private; not private.
 */
class PeerInSamePackage {
    static void demonstrate() {
        AccessShowcase s = new AccessShowcase();
        System.out.println("Peer sees public: " + s.aPublic);
        System.out.println("Peer sees protected: " + s.aProtected);
        System.out.println("Peer sees package-private: " + s.aPackagePrivate);
        // System.out.println(s.aPrivate); // won't compile: private
        new SubclassInSamePackage().demonstrate();
    }
}

/**
 * Subclass in same package: can see public, protected, and package-private; not private.
 */
class SubclassInSamePackage extends AccessShowcase {
    void demonstrate() {
        System.out.println("Subclass sees public: " + aPublic);
        System.out.println("Subclass sees protected: " + aProtected);
        System.out.println("Subclass sees package-private: " + aPackagePrivate);
        // System.out.println(aPrivate); // won't compile: private
    }
}

/**
 * In a different package (not shown here), access would be:
 *
 * package other;
 * class Other {
 *   void test() {
 *     AccessShowcase s = new AccessShowcase();
 *     s.aPublic;               // OK
 *     // s.aProtected;         // NO (not same package, not subclass)
 *     // s.aPackagePrivate;    // NO (different package)
 *     // s.aPrivate;           // NO
 *
 *     class Sub extends AccessShowcase {
 *       void demo() {
 *         aProtected;          // OK (subclass), but:
 *         // new AccessShowcase().aProtected; // NO (must access via Sub reference)
 *       }
 *     }
 *   }
 * }
 */

/**
 * Nested classes and access to outer private members.
 */
class Outer {
    private int secret = 123;

    static class StaticNested {
        int reveal(Outer o) {
            // static nested class can access private via the instance provided
            return o.secret;
        }
    }

    class Inner {
        int reveal() {
            // inner class implicitly holds a reference to Outer.this
            return secret;
        }
    }

    private class PrivateNested {}
    protected class ProtectedNested {}
    // Note: member classes can be private/protected; top-level classes cannot.
}

/**
 * Private constructor + factory to control object creation and enforce invariants.
 */
final class OnlyFactory {
    private final String value;

    private OnlyFactory(String value) {
        this.value = value;
    }

    public static OnlyFactory of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("value required");
        }
        return new OnlyFactory(value.trim());
    }

    @Override public String toString() {
        return "OnlyFactory{" + value + "}";
    }
}

/**
 * Overriding cannot narrow access:
 * - base method is protected; subclass may make it public, but not private or package-private.
 */
class BaseVisibility {
    protected String whoAmI() { return "base"; }
}
class SubVisibility extends BaseVisibility {
    @Override public String whoAmI() { return "sub"; }
    // @Override private String whoAmI() { ... } // would not compile (reduced visibility)
}

/**
 * Package-private top-level class (no modifier). Visible only within the same package.
 */
class PackagePrivateHelper {
    String help() { return "help from package-private class"; }
}

/**
 * Simple immutable value object: all fields private final, no setters, defensive design.
 */
final class ImmutablePoint {
    private final int x;
    private final int y;

    public ImmutablePoint(int x, int y) {
        this.x = x; this.y = y;
    }

    public int x() { return x; }
    public int y() { return y; }

    @Override public String toString() { return "ImmutablePoint(" + x + ", " + y + ")"; }
}

/**
 * Immutable class with mutable field type (Date). Must perform defensive copies on input and output.
 */
final class ImmutableEvent {
    private final Date when;

    public ImmutableEvent(Date when) {
        if (when == null) throw new IllegalArgumentException("when required");
        this.when = new Date(when.getTime()); // defensive copy
    }

    public Date when() {
        return new Date(when.getTime()); // defensive copy
    }
}

/**
 * Interface members:
 * - Methods are implicitly public (abstract by default; default/static methods also public).
 * - Fields are implicitly public static final (constants).
 */
interface Readable {
    int MAX = 10; // public static final implied

    void read(); // implicitly public abstract

    default void hello() { // implicitly public
        System.out.println("hello from Readable");
    }

    static void util() { // public
        System.out.println("static util in Readable");
    }
}

class Reader implements Readable {
    @Override public void read() {
        hello(); // default method call
        Readable.util(); // static method call
        System.out.println("Reader.read()");
    }
}

/*
Additional notes:
- Prefer composition over inheritance to limit surface area exposed via protected.
- Avoid exposing mutable internals (arrays, lists, maps, Date, etc.).
- For heavy invariants across many fields, consider builders or static factories.
- Records (Java 16+) provide concise immutable carriers; fields are private final, accessors are public.
- Java modules (Java 9+) add module-level visibility via exports/opens, in addition to package/class-level access.
- Reflection can break encapsulation; restrict it in production with modules/security managers where applicable.
*/