package _01_04_variables_and_scope;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.function.IntUnaryOperator;

/*
  Variables & Scope – comprehensive examples with inline explanations.

  Topics covered:
  - Declaring, initializing, and naming variables
  - Primitive vs reference types, literals, underscores in numerics
  - Default values (fields) vs no defaults (locals)
  - Constants (final) and mutability of referenced objects
  - Type inference with 'var' (where allowed)
  - Scope: method, block, loop, try-with-resources
  - Shadowing (fields by locals/parameters) and 'this'
  - Casting, numeric promotion, integer division, char arithmetic
  - Overflow and range limits
  - Lambdas and "effectively final" capture
  - Arrays and their default element values
*/
public class _02_Examples {

    // ========= Fields (class/static and instance) =========
    // Fields receive default values if not explicitly initialized.
    static int staticCounter;                // default: 0
    int instanceCounter;                     // default: 0

    // Constants: use final (and usually static) with ALL_CAPS naming.
    static final double TAX_RATE = 0.07;
    static final String COURSE_NAME = "Variables & Scope";

    // Static initialization block (runs once when the class is loaded).
    static {
        int initializedInStaticBlock = 42; // block-scoped to this static block
        staticCounter += initializedInStaticBlock % 2; // just to use it
    }

    // Instance initialization block (runs before constructor for each new instance).
    {
        int temp = 10; // block-scoped
        instanceCounter += temp / 10; // adds 1
    }

    public _02_Examples() {
        // Constructor has method scope too.
        int constructorLocal = 5; // local to constructor
        instanceCounter += constructorLocal; // adds 5
    }

    public static void main(String[] args) {
        _02_Examples demo = new _02_Examples();

        demo.exampleDeclarationsAndInitialization();
        demo.examplePrimitiveLiteralsAndUnderscores();
        demo.exampleReferenceTypesAndNull();
        demo.exampleConstantsAndFinal();
        demo.exampleTypeInferenceVar();
        demo.exampleScopeBasics();
        demo.exampleShadowingAndThis();
        demo.exampleForLoopAndEnhancedForScope();
        demo.exampleBlocksLifetimeAndGC();
        demo.exampleCastingPromotionAndDivision();
        demo.exampleOverflowAndRange();
        demo.exampleLambdasAndEffectiveFinal();
        demo.exampleTryWithResourcesScope();
        demo.exampleArraysAndDefaultValues();
    }

    // ========= Declarations & Initialization =========
    void exampleDeclarationsAndInitialization() {
        System.out.println("--- Declarations & Initialization ---");

        // Separate declaration and initialization:
        int a;
        a = 10;

        // Combined declaration and initialization:
        int b = 20;

        // Multiple declarations (legal, but less readable; prefer one per line):
        int x = 1, y = 2, z = 3;

        // Naming rules (examples):
        // int $allowed = 1;
        // int _alsoAllowed = 2;
        // int number9 = 9;
        // Illegal names (examples):
        // int 2cool = 2;       // cannot start with digit
        // int user-name = 3;   // hyphen not allowed
        // int class = 4;       // reserved keyword

        // Local variables must be assigned before use:
        int notUsedYet;
        // System.out.println(notUsedYet); // Compile error: not initialized

        System.out.println("a=" + a + ", b=" + b + ", x+y+z=" + (x + y + z));
    }

    // ========= Primitive Types, Literals, Underscores =========
    void examplePrimitiveLiteralsAndUnderscores() {
        System.out.println("--- Primitive Types, Literals, Underscores ---");

        // Integer literals
        int decimal = 42;
        int binary = 0b101010;          // 42 in binary
        int hex = 0x2A;                  // 42 in hex
        int octal = 052;                 // 42 in octal

        long big = 9_223_372_036_854_775_807L; // long requires 'L' suffix

        // Floating-point literals
        float f = 3.14F;                 // float requires 'F' suffix
        double d = 3.14;                 // double by default
        double sci = 1.23e3;             // scientific notation (1230.0)

        // Underscores for readability (cannot be at start/end or next to decimal point)
        int cc = 1234_5678;
        double money = 1_234.56;         // ok
        // double bad = 1_.234;          // not allowed

        // Characters and booleans
        char letter = 'A';
        char heart = '\u2665';           // Unicode literal
        boolean flag = true;

        System.out.println("decimal=" + decimal + ", binary=" + binary + ", hex=" + hex + ", octal=" + octal);
        System.out.println("big(long)=" + big);
        System.out.println("f=" + f + ", d=" + d + ", sci=" + sci + ", cc=" + cc + ", money=" + money);
        System.out.println("letter=" + letter + ", heart=" + heart + ", flag=" + flag);
    }

    // ========= Reference Types & null =========
    void exampleReferenceTypesAndNull() {
        System.out.println("--- Reference Types & null ---");

        String s = "hello";                 // reference type
        Person p = new Person("Ada", 32);   // custom reference type

        // null reference (points to nothing)
        Person nobody = null;

        System.out.println(s + ", " + p);
        System.out.println("nobody=" + nobody);

        // Using null safely with a check:
        if (nobody == null) {
            System.out.println("nobody is null");
        }

        // Dereferencing a null would throw NullPointerException:
        try {
            // System.out.println(nobody.name); // would NPE
            // Demonstrate in a controlled way:
            Person q = null;
            // The next line is intentionally commented to avoid runtime error
            // System.out.println(q.name);
        } catch (Exception e) {
            System.out.println("Caught: " + e);
        }

        // Fields (like 'name' and 'age') have defaults when part of an object,
        // but only if the object itself exists. For a new Person with no assignments:
        Person defaulted = new Person(); // fields default: name=null, age=0
        System.out.println("defaulted person: " + defaulted);
    }

    // ========= Constants (final) & Mutability =========
    void exampleConstantsAndFinal() {
        System.out.println("--- Constants (final) & Mutability ---");

        final int maxTries = 3; // cannot be reassigned
        // maxTries = 4; // Compile error if uncommented

        // 'final' on references prevents reassigning the reference,
        // but the object it points to may still be mutable:
        final Person p = new Person("Grace", 28);
        p.age = 29;                      // OK: mutating object state
        // p = new Person("New", 20);    // Not OK: cannot reassign 'final' reference

        System.out.println("maxTries=" + maxTries + ", person=" + p);
    }

    // ========= Type inference with 'var' (Java 10+) =========
    void exampleTypeInferenceVar() {
        System.out.println("--- Type inference with 'var' ---");

        // 'var' can be used for local variables with an initializer.
        var count = 42;                         // inferred int
        var text = "inferred string";           // inferred String
        var list = new ArrayList<String>();     // inferred ArrayList<String>
        var nums = new int[]{1, 2, 3};          // arrays require explicit 'new' in initializer
        // var invalid1;                        // Not allowed: no initializer
        // var invalid2 = null;                 // Not allowed: null alone has no type
        // 'var' is not allowed for fields or method parameters.

        list.add(text + " #" + count);
        System.out.println("count=" + count + ", text=" + text + ", list=" + list + ", numsLen=" + nums.length);
    }

    // ========= Scope Basics (method, block) =========
    void exampleScopeBasics() {
        System.out.println("--- Scope Basics ---");

        int outer = 10; // method scope
        if (outer > 5) {
            int inner = outer * 2; // block scope (only inside this if-block)
            System.out.println("inner=" + inner);
        }
        // System.out.println(inner); // Compile error: inner not visible here

        // You cannot redeclare a local variable name in an inner block of the same method.
        // int outer = 20; // Compile error if uncommented (already defined in this method)
    }

    // ========= Shadowing (fields by locals/parameters) & 'this' =========
    void exampleShadowingAndThis() {
        System.out.println("--- Shadowing & this ---");

        // Local variable shadows the field name if same (you cannot redeclare locals in blocks, but locals can shadow fields):
        int instanceCounter = 999; // shadows the instance field inside this method
        System.out.println("local instanceCounter=" + instanceCounter + ", field instanceCounter=" + this.instanceCounter);

        // Parameter shadowing:
        setInstanceCounter(123);
        System.out.println("after setInstanceCounter(123), field instanceCounter=" + this.instanceCounter);

        // Static fields can be referred via class name:
        System.out.println("_02_Examples.staticCounter=" + _02_Examples.staticCounter);
    }

    void setInstanceCounter(int instanceCounter) {
        // Parameter 'instanceCounter' shadows the field; use 'this' to refer to field.
        this.instanceCounter = instanceCounter;
    }

    // ========= For-loops and Enhanced for-loop scope =========
    void exampleForLoopAndEnhancedForScope() {
        System.out.println("--- For-loop & Enhanced for-loop scope ---");

        for (int i = 0; i < 3; i++) {
            System.out.print(i + " ");
        }
        System.out.println();
        // System.out.println(i); // Compile error: i not visible here

        int[] arr = {10, 20, 30};
        for (int value : arr) { // 'value' is scoped to the loop
            System.out.print(value + " ");
        }
        System.out.println();
    }

    // ========= Blocks, Lifetime, and GC eligibility =========
    void exampleBlocksLifetimeAndGC() {
        System.out.println("--- Blocks, Lifetime, GC eligibility ---");

        // Object reference inside a smaller scope:
        {
            Person p = new Person("Block", 1);
            System.out.println("inside block: " + p);
            // After this block, 'p' is out of scope; if no other references exist, the object is eligible for GC.
        }
        // System.out.println(p); // Compile error: p not visible here
        System.out.println("outside block");
    }

    // ========= Casting, Numeric Promotion, Integer Division =========
    void exampleCastingPromotionAndDivision() {
        System.out.println("--- Casting, Promotion, Integer Division ---");

        int a = 5, b = 2;
        System.out.println("5 / 2 (int) = " + (a / b));               // 2 (integer division)
        System.out.println("5 / 2.0 (double) = " + (a / 2.0));         // 2.5

        // Numeric promotions:
        int i = 100;
        long l = 200L;
        long sum = i + l;   // int promoted to long
        System.out.println("i + l (long) = " + sum);

        char c = 'A';       // 65
        int code = c + 1;   // char promoted to int
        char next = (char) code; // cast back to char
        System.out.println("'A' + 1 -> code=" + code + ", char=" + next);

        // Narrowing cast (possible data loss):
        int big = 130;
        byte small = (byte) big; // overflow into byte range
        System.out.println("int 130 cast to byte = " + small);
    }

    // ========= Overflow and Ranges =========
    void exampleOverflowAndRange() {
        System.out.println("--- Overflow and Ranges ---");

        int max = Integer.MAX_VALUE; // 2147483647
        int overflow = max + 1;      // wraps to Integer.MIN_VALUE
        System.out.println("Integer.MAX_VALUE=" + max + ", max+1=" + overflow);

        // Multiplication overflow if done in int:
        int a = 1_000_000;
        int b = 3_000;
        long wrong = a * b;           // overflow occurs before widening to long
        long correct = 1L * a * b;    // promote first operand to long to avoid overflow
        System.out.println("a*b (wrong int overflow) = " + wrong + ", correct(long) = " + correct);
    }

    // ========= Lambdas & "effectively final" capture =========
    void exampleLambdasAndEffectiveFinal() {
        System.out.println("--- Lambdas & effectively final ---");

        int base = 10; // effectively final (not modified after)
        IntUnaryOperator addBase = x -> x + base;
        System.out.println("addBase(5) = " + addBase.applyAsInt(5)); // 15

        // base++; // Would make 'base' not effectively final; the lambda capture would not compile.

        // Workaround to mutate captured state: wrap in a mutable object (e.g., array).
        int[] box = new int[]{10};
        IntUnaryOperator addBox = x -> x + box[0];
        System.out.println("addBox(5) w/box=10 -> " + addBox.applyAsInt(5)); // 15
        box[0] = 20; // mutate after lambda creation
        System.out.println("addBox(5) w/box=20 -> " + addBox.applyAsInt(5)); // 25
    }

    // ========= Try-with-resources scope =========
    void exampleTryWithResourcesScope() {
        System.out.println("--- Try-with-resources scope ---");

        // Resource variable is scoped to the try-with-resources header/block.
        try (BufferedReader br = new BufferedReader(new StringReader("line1\nline2"))) {
            System.out.println("read: " + br.readLine());
        } catch (IOException e) {
            System.out.println("IO error: " + e.getMessage());
        }
        // System.out.println(br); // Compile error: br out of scope here
    }

    // ========= Arrays & Default Element Values =========
    void exampleArraysAndDefaultValues() {
        System.out.println("--- Arrays & Default Element Values ---");

        int[] ints = new int[3];              // defaults: 0,0,0
        String[] strings = new String[2];     // defaults: null,null
        boolean[] bools = new boolean[2];     // defaults: false,false

        System.out.println("ints: " + ints[0] + "," + ints[1] + "," + ints[2]);
        System.out.println("strings: " + strings[0] + "," + strings[1]);
        System.out.println("bools: " + bools[0] + "," + bools[1]);

        Person[] people = new Person[2];      // defaults: null,null
        System.out.println("people[0]=" + people[0]);

        people[0] = new Person();             // person with default field values
        System.out.println("people[0] after init: " + people[0]);

        // Enhanced for-loop over arrays:
        for (int v : new int[]{1, 2, 3}) {
            System.out.print(v + " ");
        }
        System.out.println();
    }

    // ========= Helper types =========
    static class Person {
        public String name; // defaults to null in a new object
        public int age;     // defaults to 0 in a new object

        Person() {}
        Person(String name, int age) {
            this.name = name;
            this.age = age;
        }
        @Override
        public String toString() {
            return "Person{name=" + name + ", age=" + age + "}";
        }
    }
}