package _05_03_type_erasure_and_bridge_methods;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

/*
  Type Erasure & Bridge Methods
  - This file demonstrates:
    - What type erasure means at runtime.
    - How raw types can cause heap pollution and ClassCastException.
    - How upper bounds affect erasure (T extends Number -> Number).
    - Why the compiler generates bridge methods and how to see them via reflection.
    - Method overloading clashes due to erasure (commented out).
    - Reifiable vs non-reifiable types (arrays vs generics).
*/
public class _02_Examples {
    public static void main(String[] args) throws Exception {
        System.out.println("== Type erasure basics ==");
        ErasureBasicsDemo.run();

        System.out.println();
        System.out.println("== Raw types and heap pollution ==");
        RawTypePitfallDemo.run();

        System.out.println();
        System.out.println("== Upper-bounded type parameter erasure ==");
        BoundsErasureDemo.run();

        System.out.println();
        System.out.println("== Bridge method: covariant return (Supplier-style) ==");
        BridgeSupplierDemo.run();

        System.out.println();
        System.out.println("== Bridge method: parameter specialization (Processor-style) ==");
        BridgeProcessorDemo.run();

        System.out.println();
        System.out.println("== Bridge method: subclass specializing generic superclass (Node/MyNode) ==");
        BridgeNodeDemo.run();

        System.out.println();
        System.out.println("== Bridge method: Comparable<T> (compareTo) ==");
        BridgeComparableDemo.run();

        System.out.println();
        System.out.println("== Overload clashes due to erasure (commented) ==");
        OverloadClashDemo.explain();

        System.out.println();
        System.out.println("== Reifiable vs non-reifiable types ==");
        ReifiableVsNonReifiableDemo.run();
    }
}

/* ----------------------------------------------------------
   Type Erasure basics
   ---------------------------------------------------------- */
class ErasureBasicsDemo {
    static void run() {
        List<String> strings = new ArrayList<>();
        List<Integer> ints = new ArrayList<>();
        System.out.println("List<String> runtime class:  " + strings.getClass().getName());
        System.out.println("List<Integer> runtime class: " + ints.getClass().getName());
        System.out.println("Same runtime class? " + (strings.getClass() == ints.getClass()));

        // A class knows its type variables, not the concrete type arguments at runtime:
        System.out.println("ArrayList type parameters: " +
                Arrays.toString(strings.getClass().getTypeParameters())); // [E]

        // Illegal at compile time due to erasure:
        // if (strings instanceof List<String>) { } // does not compile
        if (strings instanceof List<?>) {
            System.out.println("instanceof List<?> works at runtime.");
        }
    }
}

/* ----------------------------------------------------------
   Raw types & heap pollution
   ---------------------------------------------------------- */
class RawTypePitfallDemo {
    @SuppressWarnings({ "rawtypes", "unchecked" })
    static void run() {
        List<String> strings = new ArrayList<>();
        strings.add("hello");

        // Raw view into the same list circumvents compile-time checks:
        List raw = strings;
        raw.add(123); // unchecked, compiles due to erasure

        System.out.println("List contents (runtime types): " +
                strings.stream()
                        .map(o -> o == null ? "null" : o.getClass().getSimpleName())
                        .collect(Collectors.joining(", ")));

        try {
            // Compiler inserts a cast to String on get(...)
            String s = strings.get(1); // actually an Integer at runtime
            System.out.println("Retrieved: " + s);
        } catch (ClassCastException e) {
            System.out.println("Caught expected ClassCastException due to heap pollution: " + e.getMessage());
        }
    }
}

/* ----------------------------------------------------------
   Bounded type parameter erasure (T extends Number -> Number)
   ---------------------------------------------------------- */
class Holder<T extends Number> {
    private T value;
    public void set(T value) { this.value = value; }  // erases to set(Number)
    public T get() { return value; }                  // erases to Number get()
}

class BoundsErasureDemo {
    static void run() throws Exception {
        Method get = Holder.class.getDeclaredMethod("get");
        Method set = Holder.class.getDeclaredMethod("set", Number.class);

        System.out.println("Holder.get() return type (erased): " + get.getReturnType().getName());
        System.out.println("Holder.get() generic return type:   " + get.getGenericReturnType());
        System.out.println("Holder.set(...) param type (erased): " + set.getParameterTypes()[0].getName());
        System.out.println("Holder.set(...) generic param type:  " + set.getGenericParameterTypes()[0]);

        ReflectionUtil.printDeclaredMethods(Holder.class);
    }
}

/* ----------------------------------------------------------
   Bridge method: covariant return (Supplier-style)
   ---------------------------------------------------------- */
interface MySupplier<T> {
    T get(); // erases to Object get()
}

class StringSupplier implements MySupplier<String> {
    @Override
    public String get() {
        return "Hello from StringSupplier";
    }
    // Compiler generates a bridge method: public Object get() { return get(); }
}

class BridgeSupplierDemo {
    static void run() {
        StringSupplier s = new StringSupplier();
        System.out.println("StringSupplier.get() -> " + s.get());

        // Show the synthetic bridge method via reflection:
        ReflectionUtil.printDeclaredMethods(StringSupplier.class);
    }
}

/* ----------------------------------------------------------
   Bridge method: parameter specialization (Processor-style)
   ---------------------------------------------------------- */
interface Processor<T> {
    T process(T value); // erases to Object process(Object)
}

class StringProcessor implements Processor<String> {
    @Override
    public String process(String value) {
        return value.toUpperCase();
    }
    // Compiler generates: public Object process(Object o) { return process((String) o); }
}

class BridgeProcessorDemo {
    @SuppressWarnings({ "rawtypes", "unchecked" })
    static void run() {
        Processor<String> p = new StringProcessor();
        System.out.println("Typed call: " + p.process("java"));

        // Raw type call hits the bridge method with erased signature:
        Processor raw = (Processor) p;
        Object result = raw.process("bridge");
        System.out.println("Raw call result type: " +
                (result == null ? "null" : result.getClass().getName()) + ", value=" + result);

        ReflectionUtil.printDeclaredMethods(StringProcessor.class);
    }
}

/* ----------------------------------------------------------
   Bridge method: subclass specializing generic superclass
   ---------------------------------------------------------- */
class Node<T> {
    private T value;
    public void set(T value) {              // erases to set(Object)
        this.value = value;
    }
    public T get() { return value; }        // erases to Object get()
}

class MyNode extends Node<Integer> {
    @Override
    public void set(Integer value) {        // specialized override
        System.out.println("MyNode.set(Integer) called with " + value);
        super.set(value);
    }
    // Compiler generates bridge: void set(Object o) { set((Integer) o); }
}

class BridgeNodeDemo {
    @SuppressWarnings({ "rawtypes", "unchecked" })
    static void run() {
        Node<Integer> n = new MyNode();
        n.set(42);

        // Through raw type, the bridge ensures a ClassCastException rather than corrupting state:
        Node raw = (Node) n;
        try {
            raw.set("not an Integer"); // invokes MyNode's bridge set(Object)
        } catch (ClassCastException e) {
            System.out.println("Bridge method protected type safety; got: " + e.getClass().getSimpleName());
        }

        ReflectionUtil.printDeclaredMethods(MyNode.class);
    }
}

/* ----------------------------------------------------------
   Bridge method: Comparable<T> (compareTo)
   ---------------------------------------------------------- */
class Person implements Comparable<Person> {
    private final String name;
    Person(String name) { this.name = name; }
    @Override
    public int compareTo(Person other) {
        return this.name.compareTo(other.name);
    }
    // Compiler generates bridge: public int compareTo(Object o) { return compareTo((Person) o); }
}

class BridgeComparableDemo {
    @SuppressWarnings({ "rawtypes", "unchecked" })
    static void run() {
        Person a = new Person("Alice");
        Person b = new Person("Bob");
        System.out.println("compareTo(Person): " + a.compareTo(b));

        Comparable raw = a; // erased view
        try {
            raw.compareTo("not a Person"); // calls bridge compareTo(Object)
        } catch (ClassCastException e) {
            System.out.println("Bridge compareTo(Object) enforced runtime check: " + e.getClass().getSimpleName());
        }

        ReflectionUtil.printDeclaredMethods(Person.class);
    }
}

/* ----------------------------------------------------------
   Overload clashes due to erasure (commented)
   ---------------------------------------------------------- */
class OverloadClashDemo {
    static void explain() {
        System.out.println("Two overloads that only differ in type arguments erase to the same signature and fail to compile.");
        System.out.println("See the commented class below for an example.");

        /*
        // This will NOT COMPILE because after erasure both methods are m(List)
        class OverloadClash {
            void m(List<String> xs) { }
            void m(List<Integer> xs) { } // name clash: same erasure
        }
        */

        // One workaround: use different method names or add a type token parameter.
        class OverloadWorkaround {
            void mStrings(List<String> xs) { }
            void mIntegers(List<Integer> xs) { }
            <T> void m(List<T> xs, Class<T> token) { }
        }
    }
}

/* ----------------------------------------------------------
   Reifiable vs non-reifiable types
   ---------------------------------------------------------- */
class ReifiableVsNonReifiableDemo {
    static void run() {
        List<String> ls = new ArrayList<>();
        List<Integer> li = new ArrayList<>();
        System.out.println("List<String>.class == List<Integer>.class ? " + (ls.getClass() == li.getClass())); // true

        String[] sa = new String[0];
        Integer[] ia = new Integer[0];
        System.out.println("String[].class == Integer[].class ? " + (sa.getClass() == ia.getClass())); // false

        // Illegal: cannot test instanceof parameterized type
        // if (ls instanceof List<String>) {} // does not compile

        // Legal:
        if (ls instanceof List<?>) {
            System.out.println("instanceof List<?> is allowed");
        }

        // Illegal in a generic context (erasure): cannot create new T[]
        /*
        class ArrayMaker<T> {
            T[] make(int n) {
                return new T[n]; // does not compile
            }
        }
        */
    }
}

/* ----------------------------------------------------------
   Reflection helper
   ---------------------------------------------------------- */
class ReflectionUtil {
    static void printDeclaredMethods(Class<?> clazz) {
        System.out.println("Declared methods of " + clazz.getName() + ":");
        Method[] methods = clazz.getDeclaredMethods();
        Arrays.sort(methods, Comparator
                .comparing(Method::getName)
                .thenComparing(m -> Arrays.stream(m.getParameterTypes())
                        .map(Class::getName)
                        .collect(Collectors.joining(","))));
        for (Method m : methods) {
            String params = Arrays.stream(m.getParameterTypes())
                    .map(Class::getSimpleName)
                    .collect(Collectors.joining(", "));
            System.out.println("  " +
                    (Modifier.isPublic(m.getModifiers()) ? "public " : "") +
                    (Modifier.isProtected(m.getModifiers()) ? "protected " : "") +
                    (Modifier.isPrivate(m.getModifiers()) ? "private " : "") +
                    (Modifier.isStatic(m.getModifiers()) ? "static " : "") +
                    m.getReturnType().getSimpleName() + " " +
                    m.getName() + "(" + params + ")" +
                    "  [bridge=" + m.isBridge() + ", synthetic=" + m.isSynthetic() + "]");
        }
    }
}