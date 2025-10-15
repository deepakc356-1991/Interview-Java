package _01_03_primitive_types_and_literals;

/*
 Primitive Types & Literals in Java (theory + runnable demos)

 Java has 8 primitive types:
 - Integers (signed, two's complement): byte (8-bit), short (16-bit), int (32-bit), long (64-bit)
 - Floating-point (IEEE 754): float (32-bit), double (64-bit)
 - Character: char (16-bit, unsigned UTF-16 code unit)
 - Boolean: boolean (true/false)

 Literal basics:
 - Integral literals are int by default (use L/l for long).
 - Floating-point literals are double by default (use F/f for float, D/d is optional).
 - Bases: decimal (no prefix), binary (0b/0B), octal (0 prefix; rarely used), hex (0x/0X).
 - Numeric separators: underscores (_) can improve readability, with restrictions (see below).
 - Char literals use single quotes; support escapes like '\n', '\t', '\\', '\'', '\"', and Unicode '\\uXXXX'.
 - Boolean literals: true and false (no numeric truthiness).

 Important behavior and rules:
 - Integer arithmetic overflows wrap around (two's complement). Use Math.addExact/…Exact to detect overflow.
 - byte, short, and char are promoted to int in arithmetic expressions.
 - Widening conversions happen implicitly; narrowing conversions require an explicit cast unless the RHS is a constant expression within range.
 - Right shift >> is arithmetic (sign-propagating); >>> is logical (zero-fill).
 - float/double follow IEEE 754: NaN, ±Infinity, signed zero (-0.0), rounding errors. Use BigDecimal for exact decimal math (e.g., money).
 - char represents a UTF-16 code unit, not necessarily a full Unicode code point. Supplementary code points need surrogate pairs.
 - No implicit conversion between boolean and numeric types.

 Numeric separator (_) rules:
 - Allowed between digits; not at the start or end; not adjacent to a decimal point; not adjacent to type suffix (L/F/D); not inside 0x/0X/0b/0B prefix itself.
 - Examples valid: 1_000_000, 0b1010_0101, 0xCAFE_BABE, 1.23_45e6_7
 - Examples invalid (won't compile): _1000, 1000_, 1_.0, 1._0, 0x_FF, 1.0e_10

 Hex floating-point literals:
 - Form: 0xH.HHHp±E where 'p' exponent is a power of two. Example: 0x1.0p4 == 16.0
*/
public strictfp class _01_Theory {

    // Default values for primitives (only for fields; local variables have no default and must be initialized)
    static byte    defaultByte;
    static short   defaultShort;
    static int     defaultInt;
    static long    defaultLong;
    static float   defaultFloat;
    static double  defaultDouble;
    static char    defaultChar;
    static boolean defaultBoolean;

    public static void main(String[] args) {
        title("Integer literals (bases), ranges, and suffixes");
        integerLiteralsAndRanges();

        title("Numeric separators (_) examples");
        numericSeparators();

        title("Integer overflow and bit shifts");
        integerOverflowAndShifts();

        title("char, escapes, and Unicode");
        charAndUnicode();

        title("Floating-point literals, precision, and specials");
        floatingPointLiteralsAndSpecials();

        title("boolean basics, logical vs bitwise");
        booleanBasics();

        title("Default values of primitive fields (locals must be initialized)");
        defaultValuesDemo();

        title("Numeric promotions and casting");
        numericPromotionsAndCasting();

        title("Constant expressions and narrowing without casts");
        constantExpressionsNarrowing();
    }

    private static void title(String s) {
        System.out.println();
        System.out.println("=== " + s + " ===");
    }

    private static void integerLiteralsAndRanges() {
        // Decimal (default base)
        int dec = 255;

        // Binary (0b/0B)
        int bin = 0b1111_1111; // 255

        // Octal (leading 0). Rarely used; beware of accidental octal from leading zero.
        int oct = 0377; // 255

        // Hex (0x/0X)
        int hex = 0xFF; // 255

        // long literal uses L (prefer uppercase L to avoid confusion with 1)
        long big = 9_223_372_036_854_775_807L; // Long.MAX_VALUE

        System.out.println("dec = " + dec + ", bin = " + bin + ", oct = " + oct + ", hex = " + hex);
        System.out.println("long literal big = " + big + " (Long.MAX_VALUE)");

        // Ranges using wrapper constants
        System.out.println("byte   range: " + Byte.MIN_VALUE   + " .. " + Byte.MAX_VALUE);
        System.out.println("short  range: " + Short.MIN_VALUE  + " .. " + Short.MAX_VALUE);
        System.out.println("int    range: " + Integer.MIN_VALUE+ " .. " + Integer.MAX_VALUE);
        System.out.println("long   range: " + Long.MIN_VALUE   + " .. " + Long.MAX_VALUE);
    }

    private static void numericSeparators() {
        int oneMillion = 1_000_000;
        int binMask    = 0b1111_0000_1010_1010; // grouping by nibbles or bytes
        int hexMagic   = 0xCAFE_BABE;
        long cardLike  = 1234_5678_9012_3456L;
        double rate    = 3.1415_9265;
        double expVal  = 1.234_5e1_0; // underscores allowed within exponent digits

        System.out.println("oneMillion = " + oneMillion);
        System.out.println("binMask    = " + Integer.toBinaryString(binMask));
        System.out.println("hexMagic   = 0x" + Integer.toHexString(hexMagic));
        System.out.println("cardLike   = " + cardLike);
        System.out.println("rate       = " + rate);
        System.out.println("expVal     = " + expVal);

        // Invalid examples (do not compile), shown as comments:
        // int bad1 = _1000;
        // int bad2 = 1000_;
        // double bad3 = 1_.0;
        // double bad4 = 1._0;
        // int bad5 = 0x_FF;
        // double bad6 = 1.0e_10;
    }

    private static void integerOverflowAndShifts() {
        int max = Integer.MAX_VALUE;
        int overflowed = max + 1; // wrap-around (two's complement)
        System.out.println("Integer.MAX_VALUE = " + max + ", max + 1 = " + overflowed);

        try {
            int x = Math.addExact(max, 1); // throws ArithmeticException on overflow
            System.out.println(x); // not reached
        } catch (ArithmeticException ex) {
            System.out.println("Math.addExact detects overflow: " + ex);
        }

        int n = -8; // 0xFFFFFFF8
        int arithShift = n >> 1;  // arithmetic right shift (sign bit extended)
        int logicShift = n >>> 1; // logical right shift (zero-filled)
        System.out.println("n                = " + n + " -> " + toBits(n));
        System.out.println("n >> 1 (arith)   = " + arithShift + " -> " + toBits(arithShift));
        System.out.println("n >>> 1 (logic)  = " + logicShift + " -> " + toBits(logicShift));
    }

    private static void charAndUnicode() {
        // char is a 16-bit unsigned UTF-16 code unit
        char letterA = 'A';
        char newline = '\n';
        char quote   = '\'';
        char back    = '\\';
        char omega   = '\u03A9'; // Ω
        char fromInt = 65;       // 'A' (int literal within range is allowed)

        System.out.println("letterA = " + letterA + ", (int)letterA = " + (int)letterA);
        System.out.println("quote=" + quote + " backslash=" + back + " omega=" + omega + " fromInt=" + fromInt);
        System.out.print("newline escape follows ->");
        System.out.print(newline);
        System.out.println("<- end");

        // char is unsigned; casting a negative int wraps modulo 65536
        char unsignedWrap = (char) -1; // 65535 (0xFFFF)
        System.out.println("(char)-1 -> code unit " + (int) unsignedWrap);

        // Supplementary code point (outside BMP) needs surrogate pair
        int grinningFace = 0x1F600; // 😀
        char[] pair = Character.toChars(grinningFace);
        String emoji = new String(pair);
        System.out.println("Supplementary code point U+" + Integer.toHexString(grinningFace).toUpperCase()
                + " rendered as: " + emoji + ", length in chars = " + emoji.length());

        // Arithmetic promotes char to int
        char c1 = 'A', c2 = 5;
        int sum = c1 + c2; // int
        System.out.println("'A' + 5 = " + sum + " (char) -> " + (char) sum);
    }

    private static void floatingPointLiteralsAndSpecials() {
        // Defaults to double; suffix f/F for float, d/D optional for double
        double d1 = 1.23;
        float  f1 = 1.23f;

        // Scientific notation
        double d2 = 6.022e23;

        // Hex floating-point literal (power of two exponent with 'p')
        double hex16 = 0x1.0p4; // 16.0
        double hexPi = 0x1.921fb54442d18p1; // ≈ Math.PI
        System.out.println("d1=" + d1 + ", f1=" + f1 + ", d2=" + d2);
        System.out.println("hex16 = " + hex16 + ", hexPi = " + hexPi + " (Double.toHexString(Math.PI) = " + Double.toHexString(Math.PI) + ")");

        // Precision and rounding: 0.1 + 0.2 != 0.3 exactly in binary floating point
        double s = 0.1 + 0.2;
        System.out.println("0.1 + 0.2 = " + s);
        System.out.printf("0.1 + 0.2 (17 dp) = %.17f%n", s);

        // Special values: Infinity, NaN, signed zero
        double posInf = 1.0 / 0.0;
        double negInf = -1.0 / 0.0;
        double nan    = 0.0 / 0.0;
        double negZero = -0.0;

        System.out.println("posInf=" + posInf + ", negInf=" + negInf + ", NaN=" + nan);
        System.out.println("NaN == NaN? " + (nan == Double.NaN) + " (always false; use Double.isNaN)");
        System.out.println("Double.isNaN(nan)? " + Double.isNaN(nan));
        System.out.println("1.0 / -0.0 = " + (1.0 / negZero) + " (demonstrates signed zero)");

        // Note: Use BigDecimal for exact decimal arithmetic (e.g., money) instead of float/double.
        // strictfp on this class ensures consistent FP evaluation across platforms.
    }

    private static void booleanBasics() {
        boolean t = true, f = false;
        System.out.println("t=" + t + ", f=" + f);

        // No implicit numeric <-> boolean conversions in Java:
        // if (1) {} // invalid

        // Short-circuit logical operators (&&, ||) vs bitwise (&, |) on booleans
        System.out.println("false && sideEffect()? (short-circuit, no call): " + (false && sideEffect("&&")));
        System.out.println("false & sideEffect()? (no short-circuit, calls): " + (false & sideEffect("&")));
        System.out.println("true || sideEffect()?  (short-circuit, no call): " + (true || sideEffect("||")));
        System.out.println("true | sideEffect()?   (no short-circuit, calls): " + (true | sideEffect("|")));
    }

    private static boolean sideEffect(String label) {
        System.out.println("  sideEffect invoked by " + label);
        return true;
    }

    private static void defaultValuesDemo() {
        System.out.println("defaultByte    = " + defaultByte);
        System.out.println("defaultShort   = " + defaultShort);
        System.out.println("defaultInt     = " + defaultInt);
        System.out.println("defaultLong    = " + defaultLong);
        System.out.println("defaultFloat   = " + defaultFloat);
        System.out.println("defaultDouble  = " + defaultDouble);
        System.out.println("defaultChar    = [" + defaultChar + "] code unit=" + (int) defaultChar);
        System.out.println("defaultBoolean = " + defaultBoolean);

        // Local variables must be initialized before use:
        // int x; System.out.println(x); // does not compile
    }

    private static void numericPromotionsAndCasting() {
        byte b1 = 40, b2 = 50;
        byte b3 = 100;

        // Arithmetic promotes to int; need cast to assign back to byte/short/char
        int product = b1 * b2;      // int
        byte sum    = (byte) (b1 + b2); // explicit narrowing
        System.out.println("product (int) = " + product + ", sum (byte) = " + sum);

        // Compound assignment does an implicit cast after the operation
        b3 += 50;  // OK; equivalent to b3 = (byte)(b3 + 50)
        System.out.println("b3 after += 50: " + b3);

        short s = 1;
        // s = s + 1; // error (int to short); uncomment to see
        s += 1; // OK
        System.out.println("s after += 1: " + s);

        char c = 'A';
        int ci = c + 1; // char -> int in arithmetic
        System.out.println("'A' + 1 -> int " + ci + " -> char " + (char) ci);

        // Widening conversions (safe, implicit): byte->short->int->long->float->double; char->int->long->float->double
        int iFromByte = b1;
        long lFromInt = iFromByte;
        double dFromLong = lFromInt;
        System.out.println("Widened types: int=" + iFromByte + ", long=" + lFromInt + ", double=" + dFromLong);

        // Narrowing requires explicit cast (may lose information)
        int bigInt = 130;
        byte narrowed = (byte) bigInt; // 130 -> -126 (wrap-around)
        System.out.println("Narrowing 130 to byte -> " + narrowed);
    }

    private static void constantExpressionsNarrowing() {
        // A constant expression within target range can be assigned without a cast
        byte ok1 = 127;          // max byte value
        // byte bad1 = 128;      // error: out of range

        final int CI = 100;      // constant expression
        byte ok2 = CI;           // OK (within range)
        final int CJ = 128;
        // byte bad2 = CJ;       // error: out of range even though final

        System.out.println("ok1=" + ok1 + ", ok2=" + ok2 + " (constant expression narrowing)");
    }

    private static String toBits(int x) {
        String s = Integer.toBinaryString(x);
        String padded = "0".repeat(32 - s.length()) + s;
        // Group by 4 for readability
        return padded.replaceAll("(.{4})(?!$)", "$1_");
    }
}