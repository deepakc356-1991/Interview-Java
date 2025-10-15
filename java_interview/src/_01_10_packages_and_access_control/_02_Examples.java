package _01_10_packages_and_access_control;

import java.util.List;                 // Single-type import
import java.util.*;                    // On-demand (wildcard) import
import static java.util.Collections.*; // Static on-demand import (e.g., emptyList, singletonList)
import static java.lang.Math.*;        // Static import for Math members (e.g., PI, sqrt)

/*
  _02_Examples
  Topic: Packages & Access Control

  This file demonstrates:
  - Declaring a package
  - Imports: single-type, on-demand, static imports, fully qualified names
  - Access modifiers: public, protected, package-private (default), private
  - Top-level vs member type access
  - Constructors and access control
  - protected visibility across packages (explained in comments)
  - Encapsulation with private state
  - Name collision handling via fully qualified names
  - Rules about single public top-level class per source file

  Notes:
  - Some lines are commented out to illustrate code that would NOT compile.
  - Only one public top-level class is allowed and its name must match the file name.
*/
public class _02_Examples {
    public static void main(String[] args) {
        // SECTION 1: Imports and fully qualified names
        List<String> list1 = Arrays.asList("a", "b", "c"); // from java.util.* (Arrays)
        List<String> list2 = singletonList("only");        // from static import of Collections
        List<String> list3 = emptyList();                  // from static import of Collections
        System.out.println("list1: " + list1);
        System.out.println("list2: " + list2);
        System.out.println("list3: " + list3);

        // Fully qualified names avoid import conflicts (e.g., java.util.Date vs java.sql.Date)
        java.util.Date utilDate = new java.util.Date();
        java.sql.Date sqlDate = java.sql.Date.valueOf("2020-01-01");
        System.out.println("utilDate class: " + utilDate.getClass().getName());
        System.out.println("sqlDate class: " + sqlDate.getClass().getName());

        // Wildcard imports do NOT include subpackages (java.util.* does not include java.util.concurrent.*)
        java.util.concurrent.BlockingQueue<String> q = new java.util.concurrent.LinkedBlockingQueue<>();
        q.add("x");
        System.out.println("BlockingQueue head: " + q.peek());

        // Static imports of Math members
        double hyp = sqrt(3 * 3 + 4 * 4);
        System.out.println("hyp: " + hyp + ", PI: " + PI);

        // SECTION 2: Access control within the same package
        AccessBase base = new AccessBase(); // package-private constructor is visible within same package
        System.out.println("base.publicField = " + base.publicField);               // OK
        System.out.println("base.protectedField = " + base.protectedField);         // OK (same package)
        System.out.println("base.packagePrivateField = " + base.packagePrivateField); // OK (same package)
        // System.out.println("base.privateField = " + base.privateField);           // ERROR: private

        base.publicMethod();           // OK
        base.protectedMethod();        // OK (same package)
        base.packagePrivateMethod();   // OK (same package)
        // base.privateMethod();        // ERROR: private

        // SECTION 3: Subclass in the same package
        SamePackageSubclass sps = new SamePackageSubclass();
        sps.testAccess();

        // SECTION 4: Member (nested) types with different access modifiers
        OuterMember.PublicNested pn = new OuterMember.PublicNested();      // OK anywhere
        OuterMember.ProtectedNested prn = new OuterMember.ProtectedNested(); // OK (same package)
        OuterMember.PackageNested dfn = new OuterMember.PackageNested();   // OK (same package)
        // OuterMember.PrivateNested pr = new OuterMember.PrivateNested();  // ERROR: private to OuterMember
        System.out.println("Nested created: " + pn + ", " + prn + ", " + dfn);

        // SECTION 5: Private constructor pattern (factory method)
        PrivateConstructorExample e1 = PrivateConstructorExample.of("abc");
        System.out.println("Factory created: " + e1.getValue());

        // SECTION 6: Package-private top-level is visible inside the same package
        PackagePrivateTopLevel pptl = new PackagePrivateTopLevel("in package");
        System.out.println(pptl.message());

        // SECTION 7: Name collisions and imports
        // Avoid "import java.util.*; import java.sql.*;" together when both define same simple names.
        // Use fully qualified names (as shown above with Date) to disambiguate.

        // SECTION 8: protected nuance across packages (see detailed commented example below)
        System.out.println("Protected across packages: see comment example at bottom.");

        // SECTION 9: Single public top-level class rule
        // Only _02_Examples is public in this file. Another public top-level here would not compile.

        // SECTION 10: Encapsulation via private fields and public API
        BankAccount acc = new BankAccount("A-123", 1000);
        acc.deposit(50);
        // acc.balance = 100000; // ERROR: balance is private
        System.out.println("Balance: " + acc.getBalance());

        // SECTION 11: Visibility in overriding
        VisibilityOverrideBase vb = new VisibilityOverrideBase();
        vb.m();
        VisibilityOverrideGood vg = new VisibilityOverrideGood();
        vg.m();

        // SECTION 12: Singleton via private constructor
        Singleton sA = Singleton.getInstance();
        Singleton sB = Singleton.getInstance();
        System.out.println("Singleton same instance: " + (sA == sB));
    }
}

/* ============================
   Access and inheritance types
   ============================ */

class AccessBase {
    public String publicField = "public";
    protected String protectedField = "protected";
    String packagePrivateField = "package-private"; // no modifier => package-private
    private String privateField = "private";

    // Constructors with different access levels
    AccessBase() { /* package-private: visible within the same package */ }
    public AccessBase(String info) { System.out.println("public constructor " + info); }

    // Methods with different access levels
    public void publicMethod() { System.out.println("publicMethod"); }
    protected void protectedMethod() { System.out.println("protectedMethod"); }
    void packagePrivateMethod() { System.out.println("packagePrivateMethod"); }
    private void privateMethod() { System.out.println("privateMethod"); }

    protected String protectedCompute() { return "protectedCompute"; }
}

class SamePackageSubclass extends AccessBase {
    public void testAccess() {
        System.out.println("SamePackageSubclass.publicField = " + publicField);         // OK
        System.out.println("SamePackageSubclass.protectedField = " + protectedField);   // OK (same package + inheritance)
        System.out.println("SamePackageSubclass.packagePrivateField = " + packagePrivateField); // OK (same package)
        // System.out.println(privateField); // ERROR: private

        publicMethod();         // OK
        protectedMethod();      // OK
        packagePrivateMethod(); // OK
        // privateMethod();      // ERROR: private
    }
}

class OuterMember {
    public static class PublicNested {
        @Override public String toString() { return "PublicNested"; }
    }
    protected static class ProtectedNested {
        @Override public String toString() { return "ProtectedNested"; }
    }
    static class PackageNested {
        @Override public String toString() { return "PackageNested"; }
    }
    private static class PrivateNested {
        @Override public String toString() { return "PrivateNested"; }
    }

    static Object createPrivateNested() {
        // PrivateNested is accessible here because we're inside OuterMember
        return new PrivateNested();
    }
}

class PrivateConstructorExample {
    private final String value;

    private PrivateConstructorExample(String value) {
        this.value = Objects.requireNonNull(value);
    }

    public static PrivateConstructorExample of(String value) {
        return new PrivateConstructorExample(value);
    }

    public String getValue() {
        return value;
    }
}

class PackagePrivateTopLevel {
    private final String msg;

    PackagePrivateTopLevel(String msg) {
        this.msg = msg;
    }

    String message() {
        return "PackagePrivateTopLevel: " + msg;
    }
}

/* ============================
   Encapsulation example
   ============================ */

class BankAccount {
    private final String id;
    private int balance;

    public BankAccount(String id, int openingBalance) {
        this.id = id;
        this.balance = openingBalance;
    }

    public String getId() { return id; }
    public int getBalance() { return balance; }

    public void deposit(int amount) {
        if (amount <= 0) throw new IllegalArgumentException("amount must be > 0");
        balance += amount;
    }

    public boolean withdraw(int amount) {
        if (amount <= 0) throw new IllegalArgumentException("amount must be > 0");
        if (amount > balance) return false;
        balance -= amount;
        return true;
    }
}

/* ============================
   Overriding and visibility
   ============================ */

class VisibilityOverrideBase {
    public void m() { System.out.println("VisibilityOverrideBase.m (public)"); }
}

// OK: same or wider visibility when overriding
class VisibilityOverrideGood extends VisibilityOverrideBase {
    @Override public void m() { System.out.println("VisibilityOverrideGood.m (public)"); }
}

// NOT OK (would not compile): cannot reduce visibility
// class VisibilityOverrideBad extends VisibilityOverrideBase {
//     @Override protected void m() { } // ERROR: attempting to reduce from public to protected
// }

/* ============================
   Singleton (private constructor)
   ============================ */

class Singleton {
    private static final Singleton INSTANCE = new Singleton();
    private Singleton() { }
    public static Singleton getInstance() { return INSTANCE; }
}

/* ==============================================================
   Cross-package protected access (Illustrative code — do not paste
   into this file; it demonstrates how protected works across packages)

   File: AccessBase is defined in this package as above.

   In a different package (e.g., package otherpkg;):

   package otherpkg;

   import _01_10_packages_and_access_control.AccessBase;

   public class OtherPackageSubclass extends AccessBase {
       public void demo() {
           System.out.println(publicField);      // OK: public
           System.out.println(protectedField);   // OK: protected via inheritance (in subclass)
           // System.out.println(packagePrivateField); // ERROR: not in same package
           // System.out.println(privateField);       // ERROR: private

           protectedMethod();                    // OK in subclass
           // Access via a superclass reference is NOT allowed across packages:
           AccessBase base = new AccessBase("x");
           // System.out.println(base.protectedField); // ERROR
           // base.protectedMethod();                  // ERROR

           // But via subclass reference it is OK:
           OtherPackageSubclass other = new OtherPackageSubclass();
           other.protectedMethod(); // OK: still accessing as a subclass
       }
   }

   Notes:
   - Top-level types can only be public or package-private (default). 'protected' and 'private' are illegal for top-level types.
   - Only one public top-level class per source file, and the filename must match that public class.
   ============================================================== */