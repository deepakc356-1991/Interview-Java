package _01_03_primitive_types_and_literals;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Arrays;

/**
 * Primitive Types & Literals — Interview Q&A (Basic → Advanced)
 * Run this class to see answers, demos, pitfalls, and best practices.
 */
public class _03_InterviewQA {

    public static void main(String[] args) {
        intro();
        sizesAndRanges();
        literalsBasics();
        underscoresRules();
        intVsDoubleDivision();
        binaryNumericPromotion();
        wideningVsNarrowing();
        constantExpressionsAndNarrowing();
        compoundAssignmentPitfall();
        overflowUnderflow();
        floatingPointBasics();
        nanInfinityAndNegZero();
        hexFloatingPointLiterals();
        charAndUnicode();
        booleanFacts();
        wrappersBasics();
        autoboxingPitfalls();
        parsingAndFormatting();
        unsignedOperations();
        bitwiseAndShifts();
        defaultsForPrimitives();
        bestPracticesSummary();
    }

    // --- Sections ---

    static void intro() {
        title("What are Java primitive types?");
        System.out.println("8 primitives:");
        System.out.println("- boolean (true/false)");
        System.out.println("- byte (8-bit), short (16-bit), int (32-bit), long (64-bit) — integral");
        System.out.println("- float (32-bit), double (64-bit) — floating-point (IEEE 754)");
        System.out.println("- char (16-bit unsigned UTF-16 code unit)");
        line();
    }

    static void sizesAndRanges() {
        title("Sizes and ranges");
        System.out.printf("byte    : %2d bits, range [%d, %d]%n", Byte.SIZE, Byte.MIN_VALUE, Byte.MAX_VALUE);
        System.out.printf("short   : %2d bits, range [%d, %d]%n", Short.SIZE, Short.MIN_VALUE, Short.MAX_VALUE);
        System.out.printf("int     : %2d bits, range [%d, %d]%n", Integer.SIZE, Integer.MIN_VALUE, Integer.MAX_VALUE);
        System.out.printf("long    : %2d bits, range [%d, %d]%n", Long.SIZE, Long.MIN_VALUE, Long.MAX_VALUE);
        System.out.printf("float   : %2d bits, approx range [%s, %s]%n", Float.SIZE, Float.MIN_VALUE, Float.MAX_VALUE);
        System.out.printf("double  : %2d bits, approx range [%s, %s]%n", Double.SIZE, Double.MIN_VALUE, Double.MAX_VALUE);
        System.out.printf("char    : %2d bits, range [%d, %d]%n", Character.SIZE,
                (int) Character.MIN_VALUE, (int) Character.MAX_VALUE);
        System.out.println("boolean : JVM-dependent size, only values: true/false");
        line();
    }

    static void literalsBasics() {
        title("Numeric literals (integral) — decimal, binary, octal, hex; suffixes; examples");
        int dec = 42;             // decimal
        int bin = 0b101010;       // binary (Java 7+)
        int oct = 052;            // octal (leading 0) — 052 == 42
        int hex = 0x2A;           // hex
        long big = 1_000_000_000L; // long suffix L (prefer 'L' not 'l')
        byte b = 127;             // int literal narrowed at compile-time if in range
        short s = 32_767;
        char c = 65;              // 65 == 'A'
        System.out.printf("42 == 0b101010 == 052 == 0x2A -> %d == %d == %d == %d%n", dec, bin, oct, hex);
        System.out.println("Long literal with L: " + big);
        System.out.println("byte b = 127; short s = 32767; char c = 65 ('A'): " + c);

        title("Floating-point literals — default double; suffix f/F for float; scientific notation");
        double d1 = 1.23;     // double by default
        float f1 = 1.23f;     // float requires 'f'/'F'
        double d2 = 1e3;      // 1000.0
        double d3 = 1.23e-2;  // 0.0123
        System.out.printf("double: %s, float: %s, 1e3: %s, 1.23e-2: %s%n", d1, f1, d2, d3);

        title("Octal trap");
        int n = 010; // 8 in octal; leading 0 means octal
        System.out.println("010 (octal) == " + n + " (decimal). Avoid leading zero in integer literals.");
        line();
    }

    static void underscoresRules() {
        title("Underscores in numeric literals (Java 7+): readability");
        int million = 1_000_000;
        int binMask = 0b1111_0000_1010_1010;
        long creditCard = 1234_5678_9012_3456L;
        double pi = 3.141_592_653_589_793;
        System.out.println("1_000_000 -> " + million);
        System.out.println("0b1111_0000_1010_1010 -> " + binMask);
        System.out.println("1234_5678_9012_3456L -> " + creditCard);
        System.out.println("3.141_592_653_589_793 -> " + pi);

        // Invalid underscore placements (do not compile):
        // int bad1 = _1000;
        // int bad2 = 1000_;
        // double bad3 = 1_.0;       // underscore adjacent to decimal point
        // double bad4 = 1._0;       // underscore adjacent to decimal point
        // int bad5 = 0_xFF;         // after base prefix
        // int bad6 = 0x_FF;         // right after base prefix
        // int bad7 = 1234_;         // trailing underscore
        // long bad8 = 99_L;         // right before suffix
        System.out.println("Underscore rules: not at start/end, not next to a dot, base prefix, or suffix.");
        line();
    }

    static void intVsDoubleDivision() {
        title("Why does 1/2 == 0? — integer division vs floating division");
        System.out.println("1/2 (int division) => " + (1 / 2));
        System.out.println("1/2.0 (double division) => " + (1 / 2.0));
        System.out.println("To force floating result, use a floating operand (e.g., 1.0 or cast).");
        line();
    }

    static void binaryNumericPromotion() {
        title("Binary numeric promotion (expression type rules)");
        int i = 5;
        long l = 10L;
        float f = 3.5f;
        double d = 2.0;

        long r1 = i + l;          // int + long -> long
        float r2 = i + f;         // int + float -> float
        double r3 = l + d;        // long + double -> double
        int a = 1, b = 2;
        // byte/short/char are promoted to int in arithmetic expressions:
        byte x = 1, y = 2;
        int r4 = x + y;           // result is int
        // byte z = x + y; // does not compile without cast
        System.out.printf("int + long -> long: %d%n", r1);
        System.out.printf("int + float -> float: %s%n", r2);
        System.out.printf("long + double -> double: %s%n", r3);
        System.out.printf("byte + byte -> int: %d%n", r4);
        line();
    }

    static void wideningVsNarrowing() {
        title("Widening vs narrowing conversions");
        // Widening (safe, no cast required):
        int i = 'A'; // char -> int (65)
        long l = i;  // int -> long
        double d = l; // long -> double (may lose precision if long > 2^53)
        System.out.printf("char 'A' -> int: %d, int -> long: %d, long -> double: %.1f%n", i, l, d);

        // Narrowing (requires cast, may overflow/underflow):
        int big = 130;
        byte b = (byte) big; // 130 -> -126 (overflow, two's complement)
        System.out.println("Narrow int 130 to byte: " + b + " (overflow)");
        char c = (char) -1;  // -1 -> 65535 due to unsigned char range
        System.out.println("Narrow int -1 to char: " + (int) c);

        // char is unsigned 16-bit; assigning literal int within range is ok:
        char c2 = 65; // 'A'
        System.out.println("char from int literal 65: " + c2);
        line();
    }

    static void constantExpressionsAndNarrowing() {
        title("Special case: constant expressions allow safe narrowing");
        // If the RHS is a compile-time constant within target range, narrowing is allowed.
        final int ONE_HUNDRED = 100;
        byte b1 = ONE_HUNDRED; // ok, constant within byte range
        System.out.println("final int 100 -> byte: " + b1);
        // Counterexample (does not compile; out of range):
        // final int TWO_HUNDRED = 200;
        // byte b2 = TWO_HUNDRED; // error: possible lossy conversion from int to byte
        // Non-final or non-constant expressions do not get this special treatment:
        int runtime = 100;
        // byte b3 = runtime; // error without cast
        System.out.println("Only compile-time constants within range can narrow without cast.");
        line();
    }

    static void compoundAssignmentPitfall() {
        title("Compound assignments perform implicit cast");
        byte b = 10;
        // b = b + 200; // does not compile; result is int
        b += 200; // compiles: equivalent to b = (byte)(b + 200), may overflow
        System.out.println("byte b=10; b+=200; => " + b + " (overflow via implicit cast)");
        line();
    }

    static void overflowUnderflow() {
        title("Overflow/underflow in integral types");
        int max = Integer.MAX_VALUE;
        int min = Integer.MIN_VALUE;
        System.out.println("Integer.MAX_VALUE + 1 -> overflow => " + (max + 1));
        System.out.println("Integer.MIN_VALUE - 1 -> underflow => " + (min - 1));
        System.out.println("Use Math.addExact/Math.subtractExact to detect overflow:");
        try {
            Math.addExact(max, 1);
        } catch (ArithmeticException ex) {
            System.out.println("Math.addExact threw: " + ex);
        }
        line();
    }

    static void floatingPointBasics() {
        title("Floating-point: precision and rounding");
        double x = 0.1 + 0.2;
        System.out.println("0.1 + 0.2 == " + x + " (not exactly 0.3 due to binary FP)");
        System.out.println("Compare doubles with an epsilon tolerance:");
        double expected = 0.3;
        double eps = 1e-10;
        System.out.println("Math.abs(x - 0.3) < eps -> " + (Math.abs(x - expected) < eps));

        title("Use BigDecimal for exact decimal math (e.g., money)");
        BigDecimal bd = new BigDecimal("0.1").add(new BigDecimal("0.2"));
        System.out.println("BigDecimal(\"0.1\") + BigDecimal(\"0.2\") == " + bd);

        title("float vs double precision");
        float f = 16777217f; // 2^24 + 1, float cannot represent this exactly
        System.out.println("float 16777217f becomes: " + f + " (precision limit)");
        double dd = 9007199254740993d; // 2^53 + 1, double cannot represent exactly
        System.out.println("double 2^53+1 becomes: " + dd + " (precision limit)");
        line();
    }

    static void nanInfinityAndNegZero() {
        title("Infinity, NaN, and negative zero");
        System.out.println("1.0 / 0.0 = " + (1.0 / 0.0));
        System.out.println("-1.0 / 0.0 = " + (-1.0 / 0.0));
        System.out.println("0.0 / 0.0 = " + (0.0 / 0.0));
        double nan = Double.NaN;
        System.out.println("NaN == NaN? " + (nan == nan));
        System.out.println("Double.isNaN(NaN)? " + Double.isNaN(nan));
        double posZero = 0.0;
        double negZero = -0.0;
        System.out.println("0.0 == -0.0? " + (posZero == negZero));
        System.out.println("Bits 0.0 vs -0.0 differ: "
                + Long.toHexString(Double.doubleToRawLongBits(posZero)) + " vs "
                + Long.toHexString(Double.doubleToRawLongBits(negZero)));
        line();
    }

    static void hexFloatingPointLiterals() {
        title("Hexadecimal floating-point literals (advanced)");
        // Syntax: 0x<hexSignificand>.<hexFraction>p<binaryExponent>
        double hex1 = 0x1.0p3;   // 1 * 2^3 = 8.0
        double hex2 = 0x1.8p1;   // 1.5 * 2^1 = 3.0
        double hex3 = 0xC.0p-2;  // 12 * 2^-2 = 3.0
        System.out.println("0x1.0p3 -> " + hex1);
        System.out.println("0x1.8p1 -> " + hex2);
        System.out.println("0xC.0p-2 -> " + hex3);
        line();
    }

    static void charAndUnicode() {
        title("char, Unicode, and escape sequences");
        char a = 'A';
        char newline = '\n';
        char tab = '\t';
        char backslash = '\\';
        char quote = '\'';
        char copyright = '\u00A9';
        System.out.println("Chars: 'A'=" + a + ", newline, tab, backslash=" + backslash + ", quote=" + quote + ", ©=" + copyright);

        title("char is a UTF-16 code unit (0..65535), not a Unicode code point");
        String emoji = "\uD83D\uDE00"; // 😀 U+1F600 as surrogate pair
        System.out.println("Emoji 😀 length in chars: " + emoji.length() + " (surrogate pair)");
        int codePoint = emoji.codePointAt(0);
        System.out.println("Code point U+" + Integer.toHexString(codePoint).toUpperCase());
        System.out.println("Character.charCount(codePoint): " + Character.charCount(codePoint));
        System.out.println("Character.isSupplementaryCodePoint? " + Character.isSupplementaryCodePoint(codePoint));

        title("Unicode escapes processed before tokenization (pitfall)");
        // Example (do not do this): "\u000A" is a newline inserted before parsing; can break code.
        // String s = "Hello"; \u000A System.out.println("This is on a new line due to unicode escape");
        System.out.println("Unicode escapes like \\u000A become a newline before parsing (be careful).");
        line();
    }

    static void booleanFacts() {
        title("boolean facts");
        boolean t = true, f = false;
        System.out.println("boolean is not numeric. Only 'true' or 'false'.");
        // Invalid: boolean b = 1; // does not compile
        System.out.println("Cannot assign 0/1 to boolean; cannot cast int<->boolean.");
        line();
    }

    static void wrappersBasics() {
        title("Wrapper types and constants");
        System.out.println("Integer.MIN_VALUE=" + Integer.MIN_VALUE + ", MAX_VALUE=" + Integer.MAX_VALUE);
        System.out.println("Double.MIN_VALUE (smallest positive)=" + Double.MIN_VALUE + ", MAX_VALUE=" + Double.MAX_VALUE);
        System.out.println("Use wrapper constants for bounds, sizes, and useful utils.");
        line();

        title("Wrapper caching and '==' vs 'equals'");
        Integer a = 127, b = 127, c = 128, d = 128;
        System.out.println("Integer.valueOf caches [-128,127]:");
        System.out.println("Integer 127: a==b? " + (a == b) + ", a.equals(b)? " + a.equals(b));
        System.out.println("Integer 128: c==d? " + (c == d) + ", c.equals(d)? " + c.equals(d));
        line();
    }

    static void autoboxingPitfalls() {
        title("Autoboxing/unboxing pitfalls");
        Integer n = null;
        try {
            int z = n + 1; // NullPointerException due to unboxing null
            System.out.println(z);
        } catch (NullPointerException ex) {
            System.out.println("Autounboxing null throws: " + ex);
        }
        // Prefer primitives for arithmetic hot paths. Watch for unintended boxing in loops/streams.
        line();
    }

    static void parsingAndFormatting() {
        title("Parsing and formatting primitives");
        int dec = Integer.parseInt("42");
        int hex = Integer.parseInt("2A", 16);
        int bin = Integer.parseInt("101010", 2);
        long unsignedFromHex = Long.parseUnsignedLong("FFFFFFFFFFFFFFFF", 16);
        System.out.println("parseInt(\"42\") -> " + dec);
        System.out.println("parseInt(\"2A\",16) -> " + hex);
        System.out.println("parseInt(\"101010\",2) -> " + bin);
        System.out.println("parseUnsignedLong(\"FFFFFFFFFFFFFFFF\",16) -> " + unsignedFromHex);

        String s1 = Integer.toString(255);          // "255"
        String s2 = Integer.toHexString(255);       // "ff"
        String s3 = Integer.toBinaryString(255);    // "11111111"
        System.out.println("toString: " + s1 + ", hex: " + s2 + ", bin: " + s3);

        // Byte/Short unsigned helpers:
        int unsignedByte = Byte.toUnsignedInt((byte) 0xFF); // 255
        int unsignedShort = Short.toUnsignedInt((short) 0xFFFF); // 65535
        System.out.println("Byte.toUnsignedInt((byte)0xFF) -> " + unsignedByte);
        System.out.println("Short.toUnsignedInt((short)0xFFFF) -> " + unsignedShort);
        line();
    }

    static void unsignedOperations() {
        title("Unsigned operations (Java 8+)");
        int negOne = -1;
        long asUnsigned = Integer.toUnsignedLong(negOne); // 0xFFFFFFFF -> 4294967295
        System.out.println("Integer.toUnsignedLong(-1) -> " + asUnsigned);
        int div = Integer.divideUnsigned(-2, 2); // treat as unsigned
        int rem = Integer.remainderUnsigned(-2, 2);
        System.out.println("Integer.divideUnsigned(-2,2) -> " + div + ", remainder -> " + rem);
        // Compare unsigned:
        System.out.println("Integer.compareUnsigned(-1, 1) > 0 ? " + (Integer.compareUnsigned(-1, 1) > 0));
        line();
    }

    static void bitwiseAndShifts() {
        title("Bitwise ops and shifts");
        int a = 0b0101_1100;
        int b = 0b0011_0011;
        System.out.println("a & b  -> " + toBinary(a & b));
        System.out.println("a | b  -> " + toBinary(a | b));
        System.out.println("a ^ b  -> " + toBinary(a ^ b));
        System.out.println("~a     -> " + toBinary(~a));

        title("Signed vs unsigned right shift");
        int neg = -8;
        System.out.println("neg     : " + toBinary(neg));
        System.out.println("neg>>1  : " + toBinary(neg >> 1) + " (arithmetic shift, sign-extended)");
        System.out.println("neg>>>1 : " + toBinary(neg >>> 1) + " (logical shift, zero-filled)");
        // Shifts of byte/short/char promote to int first:
        byte x = (byte) 0b1111_0000;
        int shifted = x >> 2; // x promoted to int (sign-extended)
        System.out.println("byte 0b11110000 >> 2 -> int " + toBinary(shifted));
        line();
    }

    static void defaultsForPrimitives() {
        title("Default values (fields/arrays) vs locals");
        System.out.println("Static field defaults -> see Defaults class:");
        System.out.println("Defaults.int: " + Defaults.i);
        System.out.println("Defaults.double: " + Defaults.d);
        System.out.println("Defaults.boolean: " + Defaults.b);
        System.out.println("Defaults.char (as int): " + (int) Defaults.c);

        int[] ints = new int[3];
        boolean[] bools = new boolean[3];
        char[] chars = new char[3];
        System.out.println("new int[3]: " + Arrays.toString(ints));
        System.out.println("new boolean[3]: " + Arrays.toString(bools));
        System.out.print("new char[3] code units: [");
        for (int k = 0; k < chars.length; k++) {
            System.out.print((int) chars[k] + (k < chars.length - 1 ? ", " : ""));
        }
        System.out.println("]");
        // Local variables must be explicitly initialized before use.
        line();
    }

    static void bestPracticesSummary() {
        title("Best practices and common interview answers (summary)");
        System.out.println("- Prefer primitives for performance-critical code; avoid accidental boxing.");
        System.out.println("- Use uppercase 'L' for long literals (avoid confusion with '1').");
        System.out.println("- Avoid leading 0 in integer literals (octal).");
        System.out.println("- Use underscores for readability where allowed.");
        System.out.println("- Beware of int division; use floating operand for floating results.");
        System.out.println("- Understand promotion rules: byte/short/char -> int in expressions.");
        System.out.println("- Use Math.addExact/subtractExact to detect overflow.");
        System.out.println("- Avoid == for floating-point; compare with tolerance.");
        System.out.println("- Use BigDecimal (string ctor) for money/decimal exactness.");
        System.out.println("- Remember: NaN != NaN; use Double.isNaN/Float.isNaN.");
        System.out.println("- char is a UTF-16 code unit; use code points API for full Unicode.");
        System.out.println("- Integer cache: -128..127, so == may appear to work for small values only.");
        System.out.println("- Parse/format with radix as needed; beware unsigned conversions if needed.");
        line();
    }

    // --- Helpers and inner classes ---

    static class Defaults {
        static int i;
        static double d;
        static boolean b;
        static char c;
    }

    static void title(String s) {
        System.out.println();
        System.out.println("Q: " + s);
        System.out.println("A:");
    }

    static void line() {
        System.out.println("----");
    }

    static String toBinary(int x) {
        String s = Integer.toBinaryString(x);
        if (s.length() < 32) s = "0".repeat(32 - s.length()) + s;
        return s;
    }
}