package _05_03_type_erasure_and_bridge_methods;

import java.lang.reflect.*;
import java.util.*;

/*
Interview Q&A: Type Erasure & Bridge Methods (Basic → Advanced)

BASICS
1) What is type erasure?
   - Java generics are a compile-time feature. At runtime, type parameters are erased (removed).
   - List<String> and List<Integer> become the same raw type List.

2) Why did Java choose erasure instead of reified generics?
   - Backward compatibility with pre-Java 5 bytecode and libraries; no runtime overhead.

3) What does a type parameter erase to?
   - Unbounded type parameter T erases to Object.
   - Bounded type parameter <T extends Number & Comparable<T>> erases to first bound (Number).

4) Can you use instanceof with parameterized types?
   - No: instanceof List<String> is illegal.
   - Use instanceof List<?> (reifiable form).

5) Why can’t we create arrays of parameterized types?
   - Arrays are reifiable (know runtime component type), generics are not. Creation like new List<String>[10] is illegal (generic array creation).

6) What is a reifiable vs non-reifiable type?
   - Reifiable: type fully available at runtime (primitive, non-generic class, raw type, array of reifiable type). Non-reifiable: parameterized types like List<String>.

7) What is heap pollution?
   - Occurs when a variable of a parameterized type refers to an object that is not of that parameterized type, typically via raw types or varargs + generics, leading to potential ClassCastException later.

INTERMEDIATE
8) How does erasure affect overloading?
   - Methods that differ only by type parameters become the same after erasure → compile-time "name clash".

9) How does erasure affect overriding?
   - Overriding uses erased signatures. If parameter types change due to type arguments, the compiler may synthesize bridge methods to preserve polymorphism.

10) What are bridge methods?
    - Synthetic methods the compiler generates to maintain polymorphism after erasure. They adapt erased signatures to the concrete generic methods you wrote.

11) Are bridge methods visible?
    - Yes via reflection (Method.isBridge() and Method.isSynthetic()), but you never write or call them directly in source code.

12) Where do bridge methods commonly appear?
    - When a class extends/implements a generic supertype with a more specific type argument (e.g., class S extends Box<String>), and in generic interfaces like Comparable<T>, Comparator<T>.

13) Do covariant return types require a bridge?
    - Not typically. Covariant returns are supported by the JVM. Bridges are usually needed when parameter types change (e.g., set(T) → set(String) vs erased set(Object)).

14) How do bounds erase?
    - <T extends Number & Comparable<T>> → T erases to Number. Reflection can show both erased (Number) and generic (TypeVariable with bounds) views.

ADVANCED
15) Can you retrieve actual type arguments at runtime?
    - Not from variables/fields/method calls due to erasure.
    - You can only read what’s recorded in class/method signatures (e.g., subclass captures T in extends Base<String>), via reflection on getGenericSuperclass or getGenericInterfaces.

16) What is the "name clash" error with erasure?
    - Overloading/overriding methods that erase to the same signature conflicts. Example: void m(List<String>) and void m(List<Integer>) clash to m(List).

17) Why do generics and arrays not mix well?
    - Arrays are covariant and reified; generics are invariant and erased. Mixing leads to unsoundness—hence prohibitions like generic array creation.

18) What about @SafeVarargs?
    - Suppresses unsafe varargs warnings for methods that are actually safe. Only on final, static, or private methods/constructors.

19) Why can’t you catch or throw generic type variables?
    - Due to erasure, the specific exception type would be unknown at runtime (e.g., catch (T e) is illegal).

20) Are bridge methods guaranteed to be generated?
    - When needed to preserve binary compatibility and polymorphism with erased signatures. They are synthetic and bridge-flagged.

Run this class to see practical demos and reflection of bridge/erased behaviors.
*/
public class _03_InterviewQA {

    public static void main(String[] args) throws Exception {
        demoErasureBasics();
        demoErasureOnGenericMethodsAndBounds();
        demoBridgeMethodsWithSubclassing();
        demoBridgeMethodsWithComparable();
        demoErasureNameClashExamples();
        demoHeapPollutionWithRawTypes();
        demoSafeVarargs();
        demoCapturingTypeArgumentsViaSubclassSignatures();
    }

    // -------------------- BASIC: Type Erasure --------------------

    private static void demoErasureBasics() {
        System.out.println("-- Erasure basics --");
        List<String> ls = new ArrayList<>();
        List<Integer> li = new ArrayList<>();

        // Same runtime class due to erasure
        System.out.println("ArrayList<String>.class == ArrayList<Integer>.class ? " + (ls.getClass() == li.getClass()));
        System.out.println("Runtime class: " + ls.getClass().getName()); // java.util.ArrayList

        // instanceof parameterized type is illegal (won't compile):
        // if (ls instanceof List<String>) { } // compile-time error
        // Legal:
        System.out.println("instanceof List<?> is legal: " + (ls instanceof List<?>));

        // Generic array creation is illegal (won't compile):
        // List<String>[] arr = new List<String>[10]; // compile-time error: generic array creation

        // Raw array is legal (reifiable):
        List[] rawArray = new List[2];
        rawArray[0] = new ArrayList(); // raw
        System.out.println("Created raw List[] of length " + rawArray.length);
        System.out.println();
    }

    // -------------------- INTERMEDIATE: Erasure on methods and bounds --------------------

    static class BoundsDemo {
        // <T> erases to Object; with bounds, erases to first bound (Number)
        static <T extends Number & Comparable<T>> T max(T a, T b) {
            return a.compareTo(b) >= 0 ? a : b;
        }

        static <T> T echo(T t) {
            return t;
        }
    }

    private static void demoErasureOnGenericMethodsAndBounds() throws Exception {
        System.out.println("-- Erasure on generic methods & bounds --");

        Method echo = BoundsDemo.class.getDeclaredMethod("echo", Object.class); // erased param is Object
        System.out.println("echo erased return type: " + echo.getReturnType().getTypeName()); // java.lang.Object
        System.out.println("echo generic return type: " + echo.getGenericReturnType().getTypeName()); // T

        Method max = BoundsDemo.class.getDeclaredMethod("max", Number.class, Number.class); // erased params Number
        System.out.println("max erased return type: " + max.getReturnType().getTypeName()); // java.lang.Number
        System.out.println("max generic return type: " + max.getGenericReturnType().getTypeName()); // T

        TypeVariable<?> t = (TypeVariable<?>) max.getGenericReturnType();
        System.out.println("max<T> bounds: " + Arrays.toString(t.getBounds())); // [class java.lang.Number, java.lang.Comparable<T>]
        System.out.println();
    }

    // -------------------- ADVANCED: Bridge Methods (Subclassing) --------------------

    static class Box<T> {
        private T value;
        public T get() {
            return value;
        }
        public void set(T value) {
            this.value = value;
        }
    }

    static class StringBox extends Box<String> {
        @Override
        public String get() {
            return super.get(); // covariant return (String)
        }
        @Override
        public void set(String value) {
            super.set(value);   // parameter narrowed to String
        }
        // The compiler generates a synthetic bridge method:
        // public void set(Object o) { set((String) o); }  // isBridge() == true, isSynthetic() == true
        // Typically no bridge is needed for get() due to covariant returns support.
    }

    private static void demoBridgeMethodsWithSubclassing() {
        System.out.println("-- Bridge methods (subclassing) --");

        // Calls go through erased types transparently
        Box<String> box = new StringBox();
        box.set("hello");
        System.out.println("Box<String> get(): " + box.get());

        // Raw type + bridge ensures runtime type check (cast in bridge), showing safety preservation
        Box raw = new StringBox(); // raw usage
        try {
            raw.set(123); // compiles with unchecked warning; bridge does cast to String and fails
        } catch (ClassCastException ex) {
            System.out.println("Bridge enforced type safety at runtime (ClassCastException caught)");
        }

        // Reflect to see the bridge method
        for (Method m : StringBox.class.getDeclaredMethods()) {
            if (m.getName().equals("set")) {
                System.out.printf("StringBox.set sig=%s bridge=%s synthetic=%s%n",
                        methodSig(m), m.isBridge(), m.isSynthetic());
            }
        }
        System.out.println();
    }

    // -------------------- ADVANCED: Bridge Methods (Interfaces like Comparable) --------------------

    static class Person implements Comparable<Person> {
        final String name;
        Person(String name) { this.name = name; }
        @Override
        public int compareTo(Person other) {
            return this.name.compareTo(other.name);
        }
        // Compiler generates:
        // public int compareTo(Object o) { return compareTo((Person) o); } // bridge method
    }

    private static void demoBridgeMethodsWithComparable() {
        System.out.println("-- Bridge methods (Comparable) --");
        for (Method m : Person.class.getDeclaredMethods()) {
            if (m.getName().equals("compareTo")) {
                System.out.printf("Person.compareTo sig=%s bridge=%s synthetic=%s%n",
                        methodSig(m), m.isBridge(), m.isSynthetic());
            }
        }

        // Polymorphism works via erased signature compareTo(Object)
        Comparable rawComparable = new Person("Ann");
        System.out.println("Comparable.compareTo (raw): " + rawComparable.compareTo(new Person("Bob")));
        System.out.println();
    }

    // -------------------- INTERMEDIATE: Name clash due to erasure --------------------

    private static void demoErasureNameClashExamples() {
        System.out.println("-- Name clash due to erasure (examples) --");

        // Example: overloading by type parameters only (won't compile)
        // void process(List<String> x) {}
        // void process(List<Integer> x) {} // error: name clash: both erase to process(List)

        // Example: overriding clash
        // class A<T> { void set(T t) {} }
        // class B extends A<Integer> { void set(Object o) {} } // error: name clash after erasure set(Object)

        System.out.println("See commented examples for name-clash scenarios caused by erasure.");
        System.out.println();
    }

    // -------------------- INTERMEDIATE: Heap pollution --------------------

    private static void demoHeapPollutionWithRawTypes() {
        System.out.println("-- Heap pollution demo --");
        List<String> strings = new ArrayList<>();
        strings.add("ok");

        List raw = strings; // heap pollution via raw type
        raw.add(42); // unchecked warning; compiles

        try {
            String s = strings.get(1); // triggers ClassCastException at retrieval
            System.out.println("Should not reach: " + s);
        } catch (ClassCastException ex) {
            System.out.println("Heap pollution realized: ClassCastException when reading from List<String>");
        }
        System.out.println();
    }

    // -------------------- ADVANCED: @SafeVarargs --------------------

    @SafeVarargs
    private static <T> void addAll(List<T> dest, T... elements) {
        // This is safe because we don't store the varargs array in a generic location that escapes
        Collections.addAll(dest, elements);
    }

    private static void demoSafeVarargs() {
        System.out.println("-- @SafeVarargs demo --");
        List<Integer> nums = new ArrayList<>();
        addAll(nums, 1, 2, 3);
        System.out.println("Numbers: " + nums);
        System.out.println();
    }

    // -------------------- ADVANCED: Capturing type via subclass signature --------------------

    static abstract class Repo<T> {
        // Captures T if subclass records it in its extends clause, e.g., class UserRepo extends Repo<User>
        Type captured;

        Repo() {
            Type g = getClass().getGenericSuperclass();
            if (g instanceof ParameterizedType) {
                ParameterizedType p = (ParameterizedType) g;
                this.captured = p.getActualTypeArguments()[0];
            } else {
                this.captured = Object.class; // unknown
            }
        }

        Type getCapturedType() {
            return captured;
        }
    }

    static class StringRepo extends Repo<String> { }
    static class ListOfIntegerRepo extends Repo<List<Integer>> { }
    static class RawRepo extends Repo { } // no capture

    private static void demoCapturingTypeArgumentsViaSubclassSignatures() {
        System.out.println("-- Capturing type arguments via subclass signatures --");
        System.out.println("StringRepo captured: " + new StringRepo().getCapturedType());
        System.out.println("ListOfIntegerRepo captured: " + new ListOfIntegerRepo().getCapturedType());
        System.out.println("RawRepo captured: " + new RawRepo().getCapturedType()); // Object (unknown)
        System.out.println();
    }

    // -------------------- Utilities --------------------

    private static String methodSig(Method m) {
        String params = Arrays.toString(Arrays.stream(m.getParameterTypes())
                .map(Class::getTypeName).toArray(String[]::new));
        return m.getName() + params + " -> " + m.getReturnType().getTypeName();
    }

    /*
    EXTRA QUICK Q&A (Flashcards)

    Q: Can a class have two methods m(List<String>) and m(List<Integer>)?
    A: No. Name clash after erasure: both become m(List).

    Q: What does <T extends Number & Runnable> erase to?
    A: Number (first bound).

    Q: Is List<?> reifiable?
    A: It is reifiable enough for instanceof checks (e.g., x instanceof List<?>). Still, you cannot create arrays of parameterized types: new List<?>[10] is illegal.

    Q: Does Java insert runtime type checks for generics?
    A: Not for normal generic operations; checks occur at casts/bridges/array stores. Most type checks are compile-time.

    Q: How to identify bridge methods?
    A: Reflection: Method.isBridge() == true, Method.isSynthetic() == true.

    Q: Why does implementing Comparable<Person> create a bridge?
    A: The interface method erases to compareTo(Object). Your compareTo(Person) needs a bridge compareTo(Object) for legacy polymorphism.

    Q: Can you catch a generic exception type parameter T?
    A: No. Erasure prevents knowing the exact type at runtime.

    Q: What is heap pollution?
    A: A parameterized type reference points to an object of a different type (commonly via raw types or unsafe varargs), causing potential ClassCastException later.

    Q: When do you use @SafeVarargs?
    A: On final/static/private methods/constructors that accept varargs of a generic type and are implemented safely.

    Q: How to inspect actual generic arguments at runtime?
    A: Only if recorded in class/method signatures (e.g., subclass extends Base<String>). Use reflection on getGenericSuperclass/getGenericInterfaces.
    */
}