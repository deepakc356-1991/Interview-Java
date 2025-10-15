package _03_02_wrapper_classes_and_autoboxing;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Wrapper Classes & Autoboxing — Interview Q&A from basic to advanced.
 *
 * Read the comments (Q&A) and run main to see key demonstrations printed.
 *
 * Topics covered (highlights):
 * - What wrappers are, why they exist, list of types, immutability, finality
 * - Autoboxing/unboxing mechanics, when they happen, pitfalls (NPE, overflow)
 * - == vs equals, caching (-128..127), valueOf vs new, type mixing issues
 * - Parsing, decode, radix, unsigned ops, constants (MIN/MAX), compare/compareTo
 * - Overloading resolution: widening vs boxing vs varargs
 * - Collections/Generics constraints and stream primitives vs boxing
 * - Float/Double NaN and signed zero in equals/compare
 * - Default values, switch with wrappers, Atomic* vs wrappers
 * - Character wrapper quick essentials
 */
public class _03_InterviewQA {

    // Q: What are the default values for fields (not locals)?
    // A: Primitive numeric fields default to 0/0.0; wrappers default to null.
    static Integer defaultWrapperInteger; // null
    static int defaultPrimitiveInt;       // 0

    public static void main(String[] args) {
        section("Basics: What/Why/List/Immutability");
        basics();

        section("Autoboxing/Unboxing: When, How, Pitfalls");
        autoboxingBasics();
        unboxingNullNPE();
        incrementWrapperPitfall();

        section("Identity vs Equality, Caching, valueOf vs new");
        identityVsEquality();
        cachingRanges();
        valueOfVsNew();

        section("Parsing, Decode, Radix, Unsigned");
        parsingAndDecode();
        unsignedOps();

        section("Comparisons: equals/==/compare/compareTo");
        numericComparisons();
        floatDoubleNaNAndZeros();
        crossTypeEqualityPitfall();

        section("Overloading: Widening vs Boxing vs Varargs");
        overloadingResolution();

        section("Collections/Generics/Streams: Performance & Boxing");
        collectionsAndGenerics();
        streamsPrimitiveVsBoxed();

        section("Defaults, Switch with wrappers, Atomic vs Wrapper");
        defaultsAndSwitch();
        atomicVsWrapperCounter();

        section("Character wrapper essentials");
        characterWrapperEssentials();

        section("Misc: Constants, Number hierarchy, BigInteger/BigDecimal");
        miscNotes();

        section("Done");
    }

    // ----------------------------- BASICS -------------------------------------

    // Q: What are wrapper classes and why do they exist?
    // A: They wrap primitives in immutable objects so primitives can be used where
    //    objects are required (collections, generics, reflection), and provide utilities.
    // Q: List of wrappers?
    // A: Boolean, Byte, Short, Integer, Long, Float, Double, Character, and Number (abstract base for numeric wrappers).
    // Q: Are wrapper classes immutable and final?
    // A: Yes, all wrapper classes are final and immutable. Thread-safe to share.
    private static void basics() {
        println("Wrappers: Boolean, Byte, Short, Integer, Long, Float, Double, Character (Number is abstract).");
        println("Immutable & final: true");
        println("Use cases: generics/collections, APIs expecting Object, utility methods (parse/compare).");
    }

    // ------------------------- AUTOBOXING/UNBOXING ----------------------------

    // Q: What is autoboxing/unboxing?
    // A: Automatic conversion between primitive <-> corresponding wrapper by compiler.
    //    int -> Integer (boxing), Integer -> int (unboxing).
    // Q: When does it happen?
    // A: Assignments, method calls/returns, arithmetic, comparisons where a primitive/object is expected.
    private static void autoboxingBasics() {
        int p = 10;            // primitive
        Integer w = p;         // autobox (int -> Integer)
        int q = w;             // unbox (Integer -> int)
        println("Autobox: int->Integer, Unbox: Integer->int; ok: " + (p == q && w.equals(10)));
        // Autoboxing in collections:
        List<Integer> list = new ArrayList<>();
        list.add(1);   // boxes
        int sum = 0;
        for (Integer x : list) sum += x; // unboxes
        println("Autoboxing in collections works: sum=" + sum);
    }

    // Q: What happens if you unbox a null wrapper?
    // A: NullPointerException at runtime.
    private static void unboxingNullNPE() {
        Integer mayBeNull = null;
        try {
            int x = mayBeNull; // NPE
            println("Should not reach: " + x);
        } catch (NullPointerException ex) {
            println("Unboxing null -> NPE");
        }
    }

    // Q: ++/-- on wrappers?
    // A: Compiles via unbox, operate, box back. May overflow (Byte/Short) and NPE if null.
    private static void incrementWrapperPitfall() {
        Byte b = 127; // autobox
        b++; // unbox (byte 127 -> int 127), add 1 -> 128, cast to byte (-128), box -> Byte(-128)
        println("Byte overflow via ++: 127 -> " + b);
        try {
            Integer x = null;
            x++; // NPE
        } catch (NullPointerException e) {
            println("++ on null Integer -> NPE");
        }
    }

    // ----------------------- IDENTITY, CACHING, CONSTRUCTION ------------------

    // Q: == vs equals on wrappers?
    // A: == compares references; equals compares values (and types).
    //    Due to caching, small Integers may appear identical by ==, but do not rely on it.
    private static void identityVsEquality() {
        Integer a1 = 127, a2 = 127;     // cached
        Integer b1 = 128, b2 = 128;     // typically not cached
        println("Integer 127: == " + (a1 == a2) + ", equals " + a1.equals(a2));
        println("Integer 128: == " + (b1 == b2) + ", equals " + b1.equals(b2));
    }

    // Q: What ranges are cached?
    // A:
    // - Integer, Short, Byte, Long: -128..127 (configurable for Integer via -XX:AutoBoxCacheMax)
    // - Character: 0..127
    // - Boolean: true/false
    // - Float/Double: caching not specified by JLS
    private static void cachingRanges() {
        println("Caches: Integer/Short/Byte/Long -128..127; Character 0..127; Boolean true/false.");
        Character c1 = 127, c2 = 127, c3 = 128, c4 = 128;
        println("Character 127 identity: " + (c1 == c2) + ", 128 identity: " + (c3 == c4));
    }

    // Q: valueOf vs new?
    // A: valueOf may return cached instance; new always creates a new object (constructors are deprecated since Java 9).
    @SuppressWarnings("deprecation")
    private static void valueOfVsNew() {
        Integer x = Integer.valueOf(100);
        Integer y = Integer.valueOf(100);
        Integer m = new Integer(100);
        Integer n = new Integer(100);
        println("valueOf(100) cached identity: " + (x == y));
        println("new Integer(100) identity: " + (m == n));
        println("valueOf equals new: " + x.equals(m));
        println("Prefer valueOf/parseXxx. Wrapper constructors are deprecated.");
    }

    // ------------------------- PARSING / DECODE / UNSIGNED --------------------

    // Q: parseXxx vs valueOf?
    // A: parseXxx returns primitive; valueOf returns wrapper (may use cache).
    // Q: Integer.decode?
    // A: Parses decimal, hex (0x, 0X, #), octal (leading 0).
    // Q: Radix support and NumberFormatException.
    private static void parsingAndDecode() {
        int p = Integer.parseInt("42");
        Integer w = Integer.valueOf("42");
        println("parseInt vs valueOf: " + p + " / " + w);
        println("decode hex 0xFF -> " + Integer.decode("0xFF"));
        println("parse with radix 2 of 1010 -> " + Integer.parseInt("1010", 2));
        try {
            Integer.parseInt("1_000"); // underscores not allowed in strings
        } catch (NumberFormatException e) {
            println("parseInt with underscores -> NumberFormatException");
        }
        println("Boolean.parseBoolean(\"TrUe\") -> " + Boolean.parseBoolean("TrUe") + " (non-\"true\" -> false)");
    }

    // Q: Unsigned integer operations (since Java 8)?
    // A: Integer/Long have parseUnsignedXxx, toUnsignedString, compareUnsigned, divideUnsigned, remainderUnsigned, toUnsignedLong.
    private static void unsignedOps() {
        int a = -1; // 0xFFFFFFFF
        long unsigned = Integer.toUnsignedLong(a);
        int cmp = Integer.compareUnsigned(a, 1); // a (as unsigned) > 1
        String us = Integer.toUnsignedString(a);
        println("Unsigned: -1 as unsigned long -> " + unsigned + ", compareUnsigned(-1,1) -> " + cmp + ", toUnsignedString(-1) -> " + us);
    }

    // ---------------------------- COMPARISONS ---------------------------------

    // Q: equals vs compareTo?
    // A: equals must match exact wrapper type and value. compareTo provides ordering; compare static methods avoid boxing.
    // Q: float/double special: NaN and signed zeros.
    private static void numericComparisons() {
        Integer a = 5, b = 5;
        Long l = 5L;
        println("Integer.equals(Integer) true: " + a.equals(b));
        println("Integer.equals(Long) false (type mismatch): " + a.equals(l));
        println("Integer.compare(3, 5): " + Integer.compare(3, 5));
        println("Double.compare(0.0, -0.0): " + Double.compare(0.0, -0.0));
    }

    // Q: Float/Double NaN equals?
    // A: == returns false for NaN; equals returns true for NaN with same canonical bits; compare treats NaN > any other number.
    private static void floatDoubleNaNAndZeros() {
        Double dn1 = Double.NaN, dn2 = Double.NaN;
        println("NaN == NaN? " + (Double.NaN == Double.NaN));
        println("Double.valueOf(NaN).equals(Double.valueOf(NaN))? " + dn1.equals(dn2));
        println("Double.compare(NaN, 1.0): " + Double.compare(Double.NaN, 1.0));
        Double pz = 0.0, nz = -0.0;
        println("0.0 == -0.0? " + (0.0 == -0.0));
        println("Double.equals(0.0, -0.0)? " + pz.equals(nz));
    }

    // Q: Pitfall: equals across different numeric wrapper types
    // A: Always false even if numeric values match; use Number.doubleValue() or BigDecimal for cross-type comparison.
    private static void crossTypeEqualityPitfall() {
        println("new Integer(1).equals(new Long(1)) -> " + Integer.valueOf(1).equals(Long.valueOf(1)));
        // Safer cross-type numeric compare (beware floating point precision):
        boolean same = Double.compare(Integer.valueOf(1).doubleValue(), Long.valueOf(1).doubleValue()) == 0;
        println("Cross-type by doubleValue compare -> " + same);
    }

    // -------------------------- OVERLOADING RULES ------------------------------

    // Q: Overloading priority?
    // A: Exact match > primitive widening > boxing > varargs. Boxing and widening together is not allowed in one conversion step.
    private static void overloadingResolution() {
        // Ambiguity demos
        println(call(1));        // exact int
        println(call(1L));       // long method
        println(call(Integer.valueOf(1))); // Integer method
        println(call());         // varargs
        println(call((short) 1)); // short widens to int (no boxing), picks int
        // Ambiguity example: uncommenting below causes compile-time ambiguity between long and Integer
        // println(callLiteral(1)); // int literal -> prefers widening to long over boxing to Integer
        println(callLiteral(1)); // implemented to show rule
    }

    private static String call(int x) { return "call(int)"; }
    private static String call(long x) { return "call(long)"; }
    private static String call(Integer x) { return "call(Integer)"; }
    private static String call(Integer... xs) { return "call(Integer varargs, len=" + xs.length + ")"; }
    private static String call() { return "call() no-arg"; }
    private static String callLiteral(long x) { return "callLiteral(long) chosen over callLiteral(Integer)"; }
    @SuppressWarnings("unused")
    private static String callLiteral(Integer x) { return "callLiteral(Integer)"; }

    // -------------------- COLLECTIONS / GENERICS / STREAMS --------------------

    // Q: Why can't you use List<int>?
    // A: Generics require reference types; use List<Integer> or primitive streams (IntStream) to avoid boxing.
    // Q: Why are wrappers often slower?
    // A: Boxing allocates objects, caches aside; unboxing adds checks; iteration over List<Integer> is slower than int[] or IntStream.
    private static void collectionsAndGenerics() {
        List<Integer> nums = Arrays.asList(1, 2, 3, 4, 5); // boxed
        int sum = 0;
        for (int x : nums) { // unboxing each iteration
            sum += x;
        }
        println("Sum List<Integer> (unboxes): " + sum);

        // Null in collections can cause NPE on unboxing:
        List<Integer> withNull = new ArrayList<>(Arrays.asList(1, null, 3));
        try {
            int s = 0;
            for (int x : withNull) s += x; // NPE when x unboxes
            println("Sum: " + s);
        } catch (NullPointerException e) {
            println("Unboxing null in collection -> NPE");
        }
    }

    // Q: How to avoid boxing in streams?
    // A: Use primitive streams (IntStream/LongStream/DoubleStream) and specialized OptionalInt/Long/Double.
    private static void streamsPrimitiveVsBoxed() {
        List<Integer> boxed = IntStream.rangeClosed(1, 5).boxed().collect(Collectors.toList());
        int sumBoxed = boxed.stream().mapToInt(Integer::intValue).sum(); // boxing exists in source list
        int sumPrimitive = IntStream.rangeClosed(1, 5).sum(); // no boxing
        println("Stream sum boxed vs primitive: " + sumBoxed + " / " + sumPrimitive);

        Optional<Integer> optBoxed = boxed.stream().findFirst(); // Optional<Integer>
        OptionalInt optInt = IntStream.of(1, 2, 3).findFirst();  // OptionalInt
        println("Optional<Integer> present? " + optBoxed.isPresent() + ", OptionalInt present? " + optInt.isPresent());
    }

    // ----------------- DEFAULTS, SWITCH, ATOMIC VS WRAPPER --------------------

    // Q: Default values?
    // A: Wrapper fields default to null; primitives to zero. Local variables must be explicitly initialized.
    // Q: Can switch work with wrappers?
    // A: Yes, for Byte, Short, Integer, Character; unboxing occurs.
    private static void defaultsAndSwitch() {
        println("Default field values -> Integer: " + defaultWrapperInteger + ", int: " + defaultPrimitiveInt);
        Integer code = 2;
        String label;
        switch (code) { // unboxes
            case 1: label = "ONE"; break;
            case 2: label = "TWO"; break;
            default: label = "OTHER";
        }
        println("Switch on Integer -> " + label);
    }

    // Q: Are wrappers good counters in concurrency?
    // A: No, they are immutable. Use AtomicInteger/LongAdder for concurrent counters.
    private static void atomicVsWrapperCounter() {
        AtomicInteger counter = new AtomicInteger(0);
        counter.incrementAndGet();
        println("AtomicInteger counter -> " + counter.get());
    }

    // --------------------------- CHARACTER WRAPPER ----------------------------

    // Q: Character specifics?
    // A: Utility methods for classification, conversion. char is UTF-16 code unit; for full Unicode code points use int and Character methods.
    private static void characterWrapperEssentials() {
        char ch = 'A';
        println("Character.isLetter('A'): " + Character.isLetter(ch));
        println("Character.toLowerCase('A'): " + Character.toLowerCase(ch));
        // Surrogate pair example: U+1F600 (GRINNING FACE)
        int codePoint = 0x1F600;
        String s = new String(Character.toChars(codePoint));
        println("Code point U+1F600 length UTF-16 units: " + s.length());
    }

    // ------------------------------ MISC NOTES --------------------------------

    // Q: Wrapper constants and Number hierarchy?
    // A: Each numeric wrapper extends Number and provides xxxValue(), MIN_VALUE, MAX_VALUE, SIZE, BYTES.
    // Q: BigInteger/BigDecimal vs wrappers?
    // A: Arbitrary-precision (immutable) classes; not subject to overflow; slower; not involved in autoboxing.
    private static void miscNotes() {
        println("Integer.MIN_VALUE=" + Integer.MIN_VALUE + ", MAX_VALUE=" + Integer.MAX_VALUE + ", SIZE(bits)=" + Integer.SIZE + ", BYTES=" + Integer.BYTES);
        Number num = Integer.valueOf(10);
        println("Number.doubleValue() of Integer(10) -> " + num.doubleValue());
        BigInteger bi = new BigInteger("9223372036854775808"); // > Long.MAX_VALUE
        BigDecimal bd = new BigDecimal("0.1"); // exact decimal
        println("BigInteger > Long.MAX_VALUE: " + bi.toString().substring(0, 6) + "... ok");
        println("BigDecimal(0.1) exact: " + bd.toPlainString());
    }

    // ------------------------------ HELPERS -----------------------------------

    private static void section(String title) {
        println("\n--- " + title + " ---");
    }

    private static void println(String s) {
        System.out.println(s);
    }
}