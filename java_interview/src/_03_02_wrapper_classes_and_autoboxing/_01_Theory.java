package _03_02_wrapper_classes_and_autoboxing;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.*;

/*
Wrapper Classes & Autoboxing — Theory & Examples

Primitives and wrappers:
- Primitive types: byte, short, int, long, float, double, char, boolean
- Wrapper classes: Byte, Short, Integer, Long, Float, Double, Character, Boolean
- Other related: Void (represents the keyword void), Number (abstract base of numeric wrappers)

Key properties:
- Wrappers are immutable, nullable, and are Objects (can go in collections, generics, etc.).
- Autoboxing: automatic primitive -> wrapper conversion.
- Unboxing: automatic wrapper -> primitive conversion.

Caveats and pitfalls:
- Unboxing a null reference throws NullPointerException.
- '==' compares object identity for wrappers (unless unboxed); use equals(...) for value equality.
- Wrapper caching exists for some values (e.g., Integer [-128..127]); do not rely on this in logic.
- Floating point quirks: NaN comparisons, +/-0.0, precision issues.
- Constructors of wrappers are deprecated; prefer valueOf(...) and parseXxx(...) methods.
- Overload resolution order: exact match > primitive widening > boxing > varargs.
  Combination boxing+widening is not allowed; unboxing+primitive widening is allowed.

Performance:
- Boxing allocates objects; prefer primitives and primitive specializations (IntStream, OptionalInt, etc.) where possible.
*/

public class _01_Theory {

    public static void main(String[] args) {
        header("Basics: primitives, wrappers, autoboxing/unboxing");
        basicsAndAutoboxing();

        header("Null and unboxing (NPE) pitfall");
        nullUnboxingPitfall();

        header("Equality: == vs equals, caching ranges");
        equalityAndCaching();

        header("valueOf vs parseXxx vs decode");
        parsingValueOfDecode();

        header("Number base class and conversions");
        numberBaseAndConversions();

        header("Overload resolution: widening vs boxing vs varargs");
        overloadResolution();

        header("Switch with wrappers (autounboxing) and NPE risk");
        switchWithWrapper();

        header("Numeric limits, special values, Float/Double quirks");
        numericLimitsAndFloatingQuirks();

        header("Unsigned helpers (int/long)");
        unsignedHelpers();

        header("Character wrapper and Unicode code points");
        characterWrapper();

        header("Boolean wrapper: parseBoolean vs getBoolean");
        booleanWrapper();

        header("Wrappers in collections, streams, and avoiding boxing");
        collectionsAndStreams();

        header("Optionals: Optional<Integer> vs OptionalInt");
        optionalWrappers();

        header("BigInteger/BigDecimal (not wrappers) for precise arithmetic");
        bigNumbersNote();

        header("Void wrapper");
        voidWrapperNote();
    }

    // ------------------------------------------------------------

    private static void basicsAndAutoboxing() {
        // Autoboxing (primitive -> wrapper)
        int p = 42;
        Integer w1 = Integer.valueOf(p); // explicit (preferred over deprecated 'new Integer(p)')
        Integer w2 = p;                  // autoboxing

        // Unboxing (wrapper -> primitive)
        int p2 = w1;       // unboxing
        int p3 = w1 + 1;   // unboxing happens for arithmetic

        System.out.println("p=" + p + ", w1=" + w1 + ", w2=" + w2 + ", p2=" + p2 + ", p3=" + p3);

        // Wrappers are immutable; compound operations create new instances
        Integer a = 10;
        a++; // translates to: a = Integer.valueOf(a.intValue() + 1);
        System.out.println("After a++ (reboxed): a=" + a);

        // Wrappers are nullable; primitives are not
        Integer maybeNull = null;
        System.out.println("Wrapper can be null: " + maybeNull);
        // int cannot be null; default value in fields is 0 (not shown here)
    }

    private static void nullUnboxingPitfall() {
        Integer n = null;
        try {
            int x = n; // NPE here (unboxing null)
            System.out.println(x);
        } catch (NullPointerException npe) {
            System.out.println("Unboxing null -> NullPointerException");
        }

        // Defensive patterns: use Objects.requireNonNull, Optional, or null checks
        Integer safe = null;
        int val = (safe != null) ? safe : 0; // avoid unboxing NPE
        System.out.println("Safe unboxing with default: " + val);
    }

    private static void equalityAndCaching() {
        // '==' compares object identity for wrappers, unless a side is primitive (then unboxing occurs)
        Integer i1 = 127; // cached
        Integer i2 = 127; // cached instance
        Integer i3 = 128; // likely not cached
        Integer i4 = 128;

        System.out.println("Integer cache [-128..127]");
        System.out.println("Integer.valueOf(127) == Integer.valueOf(127) ? " + (i1 == i2)); // true (same cached object)
        System.out.println("Integer.valueOf(128) == Integer.valueOf(128) ? " + (i3 == i4)); // false (different objects)
        System.out.println("Use equals for value equality: " + i3.equals(i4));               // true

        // Unboxing in comparisons with primitives
        System.out.println("i3 == 128 (unboxes) ? " + (i3 == 128)); // true (compares primitives)

        // Other caches:
        // - Byte, Short, Long: cache [-128..127]
        // - Character: cache [0..127] (ASCII)
        // - Boolean: two singletons (TRUE/FALSE)
        Character c1 = 127, c2 = 127, c3 = 128, c4 = 128;
        System.out.println("Character.valueOf(127) == Character.valueOf(127) ? " + (c1 == c2)); // true
        System.out.println("Character.valueOf(128) == Character.valueOf(128) ? " + (c3 == c4)); // false

        // Floating quirks:
        Double d0 = 0.0, dm0 = -0.0;
        System.out.println("0.0 == -0.0 (primitive) ? " + (0.0 == -0.0));           // true
        System.out.println("Double.valueOf(0.0).equals(-0.0)? " + d0.equals(dm0));  // false (different bit patterns)

        Double dn1 = Double.valueOf(Double.NaN);
        Double dn2 = Double.valueOf(Double.NaN);
        System.out.println("Double.NaN equals via equals()? " + dn1.equals(dn2));   // true
        System.out.println("Double.isNaN primitive compare: Double.NaN == Double.NaN ? " + (Double.NaN == Double.NaN)); // false (primitive rule)
    }

    private static void parsingValueOfDecode() {
        // parseXxx returns primitive
        int pi = Integer.parseInt("255");
        // valueOf returns wrapper
        Integer wi = Integer.valueOf("255");
        // radix variants
        int bin = Integer.parseInt("1010", 2);
        int hex = Integer.parseInt("FF", 16);

        // decode handles sign and 0x, 0, #
        int dec1 = Integer.decode("0xFF");  // 255
        int dec2 = Integer.decode("#FF");   // 255
        int dec3 = Integer.decode("0377");  // octal 377 -> 255

        // Boolean parsing
        boolean pb = Boolean.parseBoolean("TrUe"); // true if string equalsIgnoreCase("true")
        Boolean wb = Boolean.valueOf("false");     // wrapper

        System.out.println("parseInt/valueOf/decode: pi=" + pi + ", wi=" + wi + ", bin=" + bin + ", hex=" + hex
                + ", decode=" + dec1 + "/" + dec2 + "/" + dec3 + ", parseBoolean=" + pb + ", valueOf=" + wb);

        // Prefer valueOf/parseXxx over constructors (constructors are deprecated since Java 9)
        // e.g., avoid 'new Integer("10")'
    }

    private static void numberBaseAndConversions() {
        // All numeric wrappers extend Number and provide xxxValue() conversions
        Number n = Integer.valueOf(42);
        System.out.println("Number conversions: int=" + n.intValue() + ", long=" + n.longValue() + ", double=" + n.doubleValue());

        // Beware of narrowing conversions (truncation)
        Double big = 123456789.987;
        int narrowed = big.intValue(); // truncates
        System.out.println("Narrowing Double->int truncates: " + narrowed);
    }

    // Overload resolution demonstration
    private static void overloadResolution() {
        f(1);                 // exact int
        f((short) 1);         // primitive widening short -> int (beats boxing)
        f(1L);                // calls long overload
        f(Integer.valueOf(1));// exact wrapper
        f((Object) 1);        // picks Object overload (no unboxing when explicit cast)
        f();                  // varargs (int...) with zero arguments

        // Widening beats boxing:
        g(1);                 // int -> long (widening) preferred over boxing to Long

        // Unboxing + widening is allowed:
        Integer boxedInt = 7;
        m(boxedInt);          // unbox to int then widen to long

        // Boxing + widening not allowed in one step:
        // h(1);              // would need int -> Integer (boxing) then Integer -> Number (widening ref) — not chosen over direct primitive
        // Demonstrate pref order with competing signatures:
        h(5);                 // picks h(int) exact
        h(Integer.valueOf(5));// picks h(Integer)
        h(5L);                // picks h(long) over h(Integer) because exact primitive matches first
        h();                  // varargs
    }

    // Overloads for demonstration
    static void f(int x)          { System.out.println("f(int)"); }
    static void f(long x)         { System.out.println("f(long)"); }
    static void f(Integer x)      { System.out.println("f(Integer)"); }
    static void f(Object x)       { System.out.println("f(Object)"); }
    static void f(int... xs)      { System.out.println("f(int...) length=" + xs.length); }

    static void g(Long x)         { System.out.println("g(Long) [boxing]"); }
    static void g(Object x)       { System.out.println("g(Object)"); }
    static void g(long x)         { System.out.println("g(long) [widening preferred]"); }

    static void m(long x)         { System.out.println("m(long) [unbox+ widen allowed]"); }

    static void h()               { System.out.println("h() varargs alt not called"); }
    static void h(int x)          { System.out.println("h(int) [exact]"); }
    static void h(long x)         { System.out.println("h(long)"); }
    static void h(Integer x)      { System.out.println("h(Integer)"); }

    private static void switchWithWrapper() {
        Integer month = 1; // autounboxes in switch
        switch (month) {
            case 1: System.out.println("Jan"); break;
            case 2: System.out.println("Feb"); break;
            default: System.out.println("Other");
        }

        Integer mayBeNull = null;
        try {
            switch (mayBeNull) { // unboxing null -> NPE
                case 0: System.out.println("Zero"); break;
            }
        } catch (NullPointerException npe) {
            System.out.println("Switch on null wrapper -> NPE");
        }
    }

    private static void numericLimitsAndFloatingQuirks() {
        System.out.println("Integer.MIN_VALUE=" + Integer.MIN_VALUE + ", MAX_VALUE=" + Integer.MAX_VALUE + ", BYTES=" + Integer.BYTES + ", SIZE(bits)=" + Integer.SIZE);
        System.out.println("Long.MIN_VALUE=" + Long.MIN_VALUE + ", MAX_VALUE=" + Long.MAX_VALUE);

        // Note: Float.MIN_VALUE is the smallest positive non-zero float.
        System.out.println("Float.MIN_VALUE (smallest positive) = " + Float.MIN_VALUE + ", -Float.MAX_VALUE (most negative) = " + (-Float.MAX_VALUE));
        System.out.println("Double.isFinite(1.0/0.0)? " + Double.isFinite(1.0/0.0)); // Infinity
        System.out.println("Double.isNaN(0.0/0.0)? " + Double.isNaN(0.0/0.0));       // NaN

        // Precision surprise:
        double d = 0.1 + 0.2;
        System.out.println("0.1 + 0.2 = " + d + " (not exactly 0.3 due to binary FP)");
        // Use BigDecimal for exact decimal arithmetic:
        BigDecimal bd = new BigDecimal("0.1").add(new BigDecimal("0.2"));
        System.out.println("BigDecimal(\"0.1\").add(BigDecimal(\"0.2\")) = " + bd);
    }

    private static void unsignedHelpers() {
        // Unsigned parse and formatting for int
        int ui = Integer.parseUnsignedInt("4294967295"); // 2^32-1 fits in unsigned int; stored as -1 in signed int
        System.out.println("Unsigned parse -> signed int value: " + ui + ", toUnsignedString: " + Integer.toUnsignedString(ui));

        // Unsigned operations
        int cmp = Integer.compareUnsigned(-1, 1); // (-1 as unsigned) > 1
        System.out.println("compareUnsigned(-1, 1) = " + cmp + " (positive means first is greater)");

        int div = Integer.divideUnsigned(-2, 2); // (2^32-2)/2 unsigned
        System.out.println("divideUnsigned(-2, 2) = " + div);

        long ul = Long.parseUnsignedLong("18446744073709551615"); // 2^64-1 into signed long (-1)
        System.out.println("Unsigned long parsed (signed printed): " + ul + ", unsigned string: " + Long.toUnsignedString(ul));
    }

    private static void characterWrapper() {
        // char is 16-bit UTF-16 code unit; some Unicode code points need two char units (surrogate pair)
        String emoji = "😀"; // U+1F600
        System.out.println("String length (char units) of 😀: " + emoji.length()); // likely 2
        System.out.println("Code point count of 😀: " + emoji.codePointCount(0, emoji.length())); // 1

        // Character utilities
        System.out.println("Character.isDigit('7')? " + Character.isDigit('7'));
        System.out.println("Character.toUpperCase('ß') -> " + Character.toUpperCase('ß')); // may not produce "SS" (locale-sensitive complexities)
        // Caching note:
        Character a = 127, b = 127, c = 128, d = 128;
        System.out.println("Character cache [0..127]: 127 cached? " + (a == b) + ", 128 cached? " + (c == d));
    }

    private static void booleanWrapper() {
        // valueOf/parseBoolean
        Boolean t = Boolean.valueOf("true");
        boolean f = Boolean.parseBoolean("FALSE");
        System.out.println("Boolean.valueOf(\"true\")=" + t + ", parseBoolean(\"FALSE\")=" + f);

        // getBoolean reads a system property and parses as boolean
        System.setProperty("feature.enabled", "true");
        boolean enabled = Boolean.getBoolean("feature.enabled"); // true
        boolean missing = Boolean.getBoolean("no.such.property"); // false
        System.out.println("Boolean.getBoolean: feature.enabled=" + enabled + ", missing=" + missing);

        // Only two instances exist: Boolean.TRUE, Boolean.FALSE
        System.out.println("Boolean.TRUE == Boolean.valueOf(true) ? " + (Boolean.TRUE == Boolean.valueOf(true)));
    }

    private static void collectionsAndStreams() {
        // Generics require reference types -> wrappers in collections
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);

        // Summing with boxed Integers (unboxing occurs)
        int sum1 = 0;
        for (Integer x : list) {
            sum1 += x; // unboxing each iteration
        }
        System.out.println("Sum via boxed loop (unboxing) = " + sum1);

        // Prefer primitive streams to avoid boxing
        int sum2 = list.stream().mapToInt(Integer::intValue).sum();
        System.out.println("Sum via mapToInt (no boxing downstream) = " + sum2);

        // Better: start with IntStream to avoid boxing altogether
        int sum3 = IntStream.rangeClosed(1, 5).sum();
        System.out.println("Sum via IntStream.rangeClosed = " + sum3);

        // Comparator helpers avoid boxing in sort keys
        int cmp = Integer.compare(3, 7); // primitive-friendly
        System.out.println("Integer.compare(3, 7) = " + cmp);
    }

    private static void optionalWrappers() {
        // Optional with wrappers allocates objects and can be null inside streams
        Optional<Integer> oi = Optional.of(42);
        System.out.println("Optional<Integer> get=" + oi.get());

        // Primitive specialized Optionals (no boxing)
        OptionalInt oi2 = OptionalInt.of(42);
        System.out.println("OptionalInt getAsInt=" + oi2.getAsInt());
    }

    private static void bigNumbersNote() {
        // BigInteger and BigDecimal are not wrappers; they are arbitrary-precision numeric classes
        BigInteger big = new BigInteger("123456789123456789123456789");
        BigDecimal money = new BigDecimal("12345.67").multiply(new BigDecimal("1.10"));
        System.out.println("BigInteger=" + big);
        System.out.println("BigDecimal precise multiplication=" + money);
    }

    private static void voidWrapperNote() {
        // Void: placeholder for the keyword void; cannot instantiate
        Class<?> primitiveVoid = Void.TYPE; // the Class object representing the primitive type void
        System.out.println("Void.TYPE = " + primitiveVoid);
    }

    // ------------------------------------------------------------

    private static void header(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}