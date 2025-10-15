package _01_10_packages_and_access_control;

import java.util.List;
import static java.lang.Math.PI;
import static java.lang.Math.max;
import static java.util.Collections.emptyList;

/*
 Packages & Access Control — Theory and Examples

 Why packages:
 - Namespacing: avoid type name collisions.
 - Organization: group related code, control visibility.
 - Distribution: JAR layout mirrors packages.
 - Modules (Java 9+): exports/opens work at package granularity.

 Package basics:
 - One package declaration per source file, at the top.
 - Directory layout mirrors the package (a/b/c/Foo.java -> package a.b.c;).
 - Subpackages are independent (a.b and a.b.c are different).
 - Prefer reverse-DNS names: com.example.myapp
 - Avoid the unnamed (default) package: cannot be imported, hinders tooling.
 - package-info.java can hold package annotations and Javadoc.

 Imports (compile-time only; they don’t load classes):
 - java.lang.* is implicitly imported.
 - Single-type import: import java.util.List;
 - On-demand import: import java.util.*;
 - Static import: import static java.lang.Math.PI; import static java.lang.Math.*;
 - * does not import subpackages.
 - Name resolution precedence:
     1) Declarations in the same compilation unit
     2) Types in the same package
     3) Single-type imports
     4) java.lang
     5) On-demand (*) imports
 - If two on-demand imports provide the same simple name, use a fully qualified name (FQCN).
 - You cannot import from the unnamed (default) package.

 Access modifiers (visibility):
 - public: everywhere (subject to module exports).
 - protected: within same package; and from subclasses in other packages, but only via a reference
   whose compile-time type is that subclass (or its subtype).
 - package-private (no modifier): only within the same package.
 - private: only within the top-level class and its nested types.

 Top-level types:
 - Only public or package-private allowed (no private/protected for top-level).
 - public means globally accessible (but still may be hidden by modules if not exported).

 Members (fields, methods, constructors, nested types):
 - Can be public/protected/package-private/private.
 - Constructors follow the same rules.
 - Overriding cannot reduce visibility; implementing interface methods must be public.
 - Nested classes and the enclosing class can access each other’s private members
   (compiler may generate synthetic accessors).

 Interfaces:
 - Fields are implicitly public static final.
 - Methods are public abstract unless default or static.
 - Since Java 9, private methods are allowed in interfaces (to share code among default methods).
 - Nested types declared in interfaces are implicitly public static.

 Modules (Java 9+):
 - module-info.java governs package exports/opens and dependencies:
     module com.example.app {
         exports com.example.api;
         opens com.example.model to com.fasterxml.jackson.databind;
         requires transitive com.example.lib;
     }
 - Public types in non-exported packages are not accessible to other modules.
 - On the classpath (unnamed module), traditional package visibility applies.
 - Split packages are not allowed across named modules.

 JARs and packages:
 - JAR layout mirrors packages.
 - “Sealed” packages (Manifest: Sealed: true) restrict classes of a package to a single code source.
 - Split packages (same package across multiple JARs) are allowed on the classpath, discouraged in practice,
   and disallowed across named modules.

 Testing and encapsulation:
 - Use package-private for internal APIs; public for external API.
 - White-box tests can be placed in the same package to access package-private members.
 - Black-box tests in a different package verify only the public API.
*/
public class _01_Theory {
    public static void main(String[] args) {
        // Static imports in action
        double circumference = 2 * PI * 3;
        int bigger = max(2, 5);
        List<String> empty = emptyList();

        // Use FQCN to disambiguate same simple names from different packages
        java.util.Date utilDate = new java.util.Date();
        java.sql.Date sqlDate = new java.sql.Date(System.currentTimeMillis());

        // Package-private top-level type is visible in the same package
        PackagePrivateType ppt = new PackagePrivateType();
        System.out.println("Package-private field: " + ppt.packageVisibleField);

        // Member access demo within same package
        AccessOwner owner = new AccessOwner();
        new AccessFriendSamePackage().tryAccess(owner);

        // Nested types accessing private members both ways
        Outer outer = new Outer();
        Outer.Inner inner = outer.new Inner();
        inner.touchBothWays();

        // Protected usage within same package
        new SubSamePackage().demoProtected();

        // Interface defaults and private methods (Java 9+)
        System.out.println("Interface CONST: " + InterfaceNotes.CONST);
        System.out.println("Interface default method: " + new InterfaceNotes() {
            @Override
            public String get() {
                return "";
            }
        }.publicApi());

        System.out.println("circumference=" + circumference + ", bigger=" + bigger +
                           ", utilDate=" + utilDate + ", sqlDate=" + sqlDate);
    }

    // Single place to show a static member that could be statically imported
    static class ImportNotes {
        static int MAX = 10; // could be imported via: import static ..._01_Theory.ImportNotes.MAX;
    }

    // Member accessibility showcase
    static class AccessOwner {
        public int pub = 1;
        protected int prot = 2;
        int pkg = 3;            // package-private
        private int priv = 4;

        public static int PUBLIC_STATIC = 100;
        private static int PRIVATE_STATIC = 200;

        public int readPriv() { return priv; }

        private AccessOwner(String hidden) { /* private ctor */ }
        public AccessOwner() { /* public ctor */ }

        public static AccessOwner of() { return new AccessOwner("factory"); }
    }

    static class AccessFriendSamePackage {
        void tryAccess(AccessOwner o) {
            int a = o.pub;  // ok
            int b = o.prot; // ok (same package)
            int c = o.pkg;  // ok (same package)
            // int d = o.priv; // not allowed
            int d = o.readPriv(); // allowed via public method

            int e = AccessOwner.PUBLIC_STATIC;  // ok
            // int f = AccessOwner.PRIVATE_STATIC; // not allowed
        }
    }

    // Protected across packages rule (illustrated in comment):
    /*
      In another package:
        package other;
        import _01_10_packages_and_access_control._01_Theory.Super;
        class SubOther extends Super {
            void ok(SubOther s) { s.prot = 1; } // ok: accessed via a SubOther reference
            void bad(Super s)   { s.prot = 1; } // error: not via a SubOther reference
        }
    */
    static class Super { protected int prot = 42; }
    static class SubSamePackage extends Super {
        void demoProtected() {
            prot = 7;           // ok: inherited
            Super s = new Super();
            s.prot = 8;         // ok: same package
        }
    }

    // Nested types and private access
    static class Outer {
        private int outerSecret = 10;

        static class NestedStatic {
            private static int nestedSecret = 20;
        }

        class Inner {
            private int innerSecret = 30;
            void touchBothWays() {
                outerSecret = 11;                   // inner sees outer's private
                int x = NestedStatic.nestedSecret;  // inner sees nested static's private
                int y = readInner(this);            // outer sees inner's private via instance method
                assert x + y > 0;
            }
        }

        int readInner(Inner i) {
            return i.innerSecret; // outer sees inner’s private
        }
    }

    // Interface visibility rules
    @FunctionalInterface
    interface InterfaceNotes {
        int CONST = 123; // implicitly public static final

        // Add a SAM so the lambda has a target
        String get();

        default String publicApi() {
            return helper();
        }
        private String helper() { // since Java 9
            return "hello";
        }
        static InterfaceNotes of() {
            return () -> "unused";
        }
    }

}

// Package-private top-level type: visible only within this package
class PackagePrivateType {
    int packageVisibleField = 1; // package-private
}

/*
 Name conflicts and FQCN:
 - When both java.util.Date and java.sql.Date are needed, use FQCN:
     java.util.Date a = new java.util.Date();
     java.sql.Date b = new java.sql.Date(System.currentTimeMillis());
*/

/*
 Build/run quick reference:
   javac -d out src/_01_10_packages_and_access_control/_01_Theory.java
   java -cp out _01_10_packages_and_access_control._01_Theory

 Jar:
   jar --create --file app.jar -C out .
   java -cp app.jar _01_10_packages_and_access_control._01_Theory
*/