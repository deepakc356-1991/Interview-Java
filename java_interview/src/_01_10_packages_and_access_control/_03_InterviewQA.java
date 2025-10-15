package _01_10_packages_and_access_control;

import static java.lang.Math.PI;
import static java.lang.Math.max;
import static java.util.Objects.requireNonNull;

import java.io.InputStream;

/*
Interview Q&A — Packages & Access Control

BASICS
Q1: What is a package?
A: A namespace for organizing types (classes, interfaces, enums, annotations). It prevents name clashes and groups related code.

Q2: How do you declare a package?
A: With a package statement as the first non-comment line:
   package com.example.myapp;
   It must match the directory structure under the source root.

Q3: Why use packages?
A: Encapsulation, collision avoidance, logical grouping, controlled exposure of API, easier builds and modularization.

Q4: What is the default package and why avoid it?
A: Code with no package declaration. Avoid because it cannot be imported by code in named packages, complicating builds/testing.

Q5: How do imports work?
A: - Single-type import: import java.util.List;
   - Type-import-on-demand (wildcard): import java.util.*;
   - Static single import: import static java.lang.Math.PI;
   - Static on-demand: import static java.lang.Math.*;
   Imports are compile-time only; they don’t affect bytecode layout.

Q6: What does import pkg.* include?
A: All public types in pkg (not subpackages). import java.util.* does NOT import java.util.concurrent.*.

Q7: What is always imported?
A: java.lang.* and types in the same package.

Q8: How to handle name conflicts (e.g., java.util.Date vs java.sql.Date)?
A: Use fully qualified names (FQCN) for at least one of them. Java doesn’t support import aliases.

Q9: Does wildcard import hurt performance?
A: No runtime cost. Style preferences and readability dictate usage.

Q10: Can you import from the default package?
A: No. Named packages cannot import from the default package.

ACCESS CONTROL
Q11: What access modifiers exist?
A: public, protected, (package-private, the default when no modifier), private.

Q12: What can top-level types use?
A: Only public or package-private. Top-level classes/interfaces cannot be private or protected.

Q13: Access rules summary:
- Same class: public, protected, package-private, private.
- Same package (non-subclass): public, protected, package-private.
- Subclass in same package: public, protected, package-private.
- Subclass in different package: public, protected (with a nuance, see Q14).
- Unrelated class in different package: public only.

Q14: The protected nuance across packages?
A: In a different package, a subclass can access a protected member only via a reference of the subclass (or its subclass) type, not through a base-class or unrelated instance.
   Example (won’t compile here — conceptual):
   // package a
   // public class A { protected int x; }
   // package b
   // class B extends A {
   //   void ok() { this.x = 1; }          // OK (this is a B, which is an A)
   //   void notOk(A a) { a.x = 1; }       // ERROR (a is not B or a subclass reference)
   // }

Q15: Overriding visibility rules?
A: You cannot reduce visibility when overriding (e.g., public -> protected is illegal). You can widen it (e.g., protected -> public).

Q16: Do constructors follow the same rules?
A: Yes. private constructors are common for singletons and factories; protected constructors are visible to subclasses (and all in-package code).

Q17: Interface member visibility?
A: - Fields: implicitly public static final.
   - Methods: implicitly public (default methods allowed since Java 8).
   - protected is not allowed on interface members.
   - Since Java 9, private methods in interfaces exist (not used here for broader compatibility).

Q18: What is package-private and when to use it?
A: No modifier. Visible only within the same package. Useful for internal APIs and testing without exposing types publicly.

Q19: Nested classes and access?
A: - static nested classes behave like top-level members scoped inside the enclosing type.
   - inner (non-static) classes hold a reference to the outer instance.
   - The compiler allows nested and enclosing classes to access each other’s private members by generating synthetic accessors.

PACKAGE ORGANIZATION AND ADVANCED TOPICS
Q20: package-info.java usage?
A: Holds package-level Javadoc and annotations.
   Example:
   /**
    * Domain classes for billing.
    *\/
   @javax.annotation.ParametersAreNonnullByDefault
   package com.example.billing;

Q21: JPMS (Java Platform Module System — module-info.java)?
A:
   module com.example.app {
     requires com.example.lib;
     exports com.example.api;         // public API packages
     opens com.example.model;         // runtime reflection (e.g., JSON libs)
     requires transitive com.fasterxml.jackson.databind;
     exports com.example.spi to some.consumer.module;
     opens com.example.internal to framework.module;
     uses com.example.spi.Plugin;     // service use
     provides com.example.spi.Plugin with com.example.impl.MyPlugin; // service impl
   }
   - exports: compile-time and runtime visibility to other modules.
   - opens: permits deep reflection at runtime (no compile-time access).
   - Split packages are not allowed across modules (same package in multiple modules).
   - Classpath code is in the “unnamed module” and can still have split packages.

Q22: Automatic modules?
A: A non-modular JAR on the module path becomes an automatic module with a derived name (from JAR file). Be cautious: names can change and cause instability.

Q23: Class loaders and packages?
A: The same FQCN loaded by different class loaders yields distinct types (ClassCastException risk). Packaging and loader boundaries matter in containers/plug-ins.

Q24: Reflection and access control?
A: setAccessible(true) used to bypass access checks, but since Java 9 strong encapsulation applies, especially for JDK internals. Illegal-access may be denied or warned.

Q25: Resources and packages?
A: Load with Class::getResource[AsStream]. Path without leading slash is relative to the class’s package; with leading slash is from the classpath root.

COMMON PITFALLS AND TRICKS
Q26: Only one public top-level class per .java file?
A: Yes, and the file name must match that public class.

Q27: Wildcard import doesn’t import subpackages?
A: Correct. import a.* does not include a.b.*.

Q28: Ambiguous simple names?
A: If two classes with the same simple name are imported or available, using the simple name is ambiguous. Use FQCN.

Q29: The default package and testing?
A: Tests in named packages cannot import code in the default package. Prefer named packages everywhere.

Q30: Is “internal” package hidden?
A: Not on the classpath. Anyone can import it. To truly encapsulate, don’t export the package in a module, or keep it out of the published JAR.

Q31: Protected constructors and different packages?
A: Same rules as protected members: subclasses in other packages can invoke them via super()/this(), but code in unrelated packages cannot.

Q32: Records and access (Java 16+)?
A: Record components are private final with public accessor methods. Use modules/exports to control package exposure.

Q33: “Friend” access like in C++?
A: Java doesn’t have friend. Use package-private, nested classes, or patterns like DI/testing support.

Q34: JAR Hell (duplicate classes)?
A: If two JARs define the same FQCN on the classpath, the class loader’s order decides which one wins. Leads to fragile builds. Prefer dependency management and JPMS.

Q35: Can a top-level type be private or protected?
A: No. Only public or package-private.

Q36: Can you import a nested class?
A: Yes: import java.util.Map.Entry; or import java.util.Map.*; for nested public static members.

Q37: Does import affect runtime size or speed?
A: No. Imports are compile-time only.

USAGE NOTES
- Use package-private for internal APIs that tests in the same package can access.
- Use protected only when you expect subclassing. Prefer composition for public APIs.
- Use modules to enforce boundaries and hide internals.

See the demo code below for practical examples.
*/

public class _03_InterviewQA {
    public static void main(String[] args) {
        System.out.println("Packages & Access Control — Interview Q&A Demo");

        // Demo: access across modifiers within the same package
        BaseVisibility base = new BaseVisibility();
        System.out.println("public: " + base.publicField);
        System.out.println("protected (same package): " + base.protectedField);
        System.out.println("package-private: " + base.packagePrivateField);
        System.out.println("private via accessor: " + base.getPrivateField());

        // Demo: subclass access in the same package (protected is inherited and directly visible)
        SubInSamePackage sub = new SubInSamePackage();
        System.out.println("Subclass sees protected: " + sub.seeProtected());
        System.out.println("Subclass inherits public: " + sub.publicField);

        // Demo: using static import
        System.out.println("PI (static import): " + PI);
        System.out.println("max(3,7) (static import): " + max(3, 7));

        // Demo: resource loading relative to this package
        InputStream res = _03_InterviewQA.class.getResourceAsStream("resource.txt");
        System.out.println("Resource in same package found? " + (res != null));

        // Demo: private nested class can be used only inside its outer class
        Outer outer = new Outer();
        System.out.println("Outer.secret(): " + outer.secret());

        // Demo: fully qualified names to disambiguate types with same simple name
        java.util.Date utilDate = new java.util.Date();
        java.sql.Date sqlDate = new java.sql.Date(utilDate.getTime());
        System.out.println("java.util.Date vs java.sql.Date: "
                + utilDate.getClass().getSimpleName() + " | " + sqlDate.getClass().getSimpleName());

        // Demo: package-private top-level class is visible within the same package
        PackagePrivateTopLevel pptl = new PackagePrivateTopLevel();
        System.out.println(pptl.explain());

        // Demo: interface default method and static import usage
        Greeter g = new Greeter() {};
        System.out.println(g.greet("world"));

        // Demo: overriding visibility (widening is allowed)
        VisibilityOverrideBase v = new VisibilityOverrideSub();
        v.hook(); // dynamic dispatch, allowed and visible
    }
}

// Package-private type with all modifiers
class BaseVisibility {
    public String publicField = "publicField";
    protected String protectedField = "protectedField";
    String packagePrivateField = "packagePrivateField"; // no modifier => package-private
    private String privateField = "privateField";

    public String getPrivateField() {
        return privateField;
    }

    public void publicMethod() {}
    protected void protectedMethod() {}
    void packagePrivateMethod() {}
    private void privateMethod() {}
}

// Subclass in the same package
class SubInSamePackage extends BaseVisibility {
    String seeProtected() {
        // protected is directly accessible via inheritance
        return protectedField;
    }
}

// Non-subclass in the same package
class SamePackageNonSub {
    String see(BaseVisibility b) {
        // Same package can access protected and package-private
        return b.protectedField + " | " + b.packagePrivateField + " | " + b.publicField;
        // b.privateField; // not accessible
    }
}

// Demonstrates private nested class access
class Outer {
    private static class PrivateNested {
        String value = "OnlyVisibleInsideOuter";
    }

    String secret() {
        PrivateNested p = new PrivateNested();
        return p.value;
    }
}

// Package-private top-level class
class PackagePrivateTopLevel {
    String explain() {
        return "I am a package-private top-level class; visible only within my package.";
    }
}

// Interface default method demo (Java 8+)
interface Greeter {
    default String greet(String name) {
        return "Hello, " + requireNonNull(name);
    }
}

// Overriding visibility: widening is allowed
class VisibilityOverrideBase {
    protected void hook() {
        System.out.println("Base hook");
    }
}

class VisibilityOverrideSub extends VisibilityOverrideBase {
    @Override
    public void hook() {
        System.out.println("Sub hook (visibility widened to public)");
    }
}

/*
Additional interview-ready snippets (commented; for illustration only):

// PROTECTED nuance across packages:
package a;
public class A {
    protected int x;
}
package b;
import a.A;
public class B extends A {
    void ok() { this.x = 1; }   // OK: accessed via B (subclass) instance
    void bad(A other) { other.x = 2; } // ERROR: not via subclass reference
}

// package-info.java example (in its own file):
/**
 * Utilities for parsing domain expressions.
 *\/
@org.jspecify.annotations.NullMarked
package com.example.expr;

// module-info.java example:
module com.example.expr {
    exports com.example.expr.api;
    opens com.example.expr.model to com.fasterxml.jackson.databind;
    requires transitive com.fasterxml.jackson.databind;
    uses com.example.expr.spi.Extension;
    provides com.example.expr.spi.Extension with com.example.expr.impl.DefaultExtension;
}

// Loading resources:
InputStream in1 = MyClass.class.getResourceAsStream("local.txt");  // same package
InputStream in2 = MyClass.class.getResourceAsStream("/com/acme/app/config.yaml"); // from classpath root
*/