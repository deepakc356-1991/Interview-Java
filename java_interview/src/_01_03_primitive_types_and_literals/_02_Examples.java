package _01_03_primitive_types_and_literals;

public class _02_Examples {

    public static void main(String[] args) {
        primitiveIntegerLiterals();
        integerBasesAndUnderscores();
        integerRangesAndOverflow();
        floatingPointLiterals();
        charLiteralsAndUnicode();
        booleanLiterals();
        numericPromotionAndCasting();
        defaultValuesOfFields();
    }

    // 1) Primitive integer types and basic literals
    private static void primitiveIntegerLiterals() {
        System.out.println("== Primitive Integer Types & Basic Literals ==");
        byte b = 127;                 // max byte
        short s = 32_767;             // underscores for readability
        int i = 2_147_483_647;        // max int
        long l1 = 9_223_372_036_854_775_807L; // max long, needs 'L' suffix
        long l2 = 2_147_483_648L;     // out of int range, so 'L' is required

        System.out.println("byte b = " + b);
        System.out.println("short s = " + s);
        System.out.println("int i = " + i);
        System.out.println("long l1 = " + l1);
        System.out.println("long l2 = " + l2);

        // Invalid examples (do not compile):
        // long badLong = 9223372036854775808;   // missing 'L' and out of int range
        // int tooBigInt = 2_147_483_648;        // out of int range

        System.out.println();
    }

    // 2) Integer bases (decimal, binary, octal, hex) and underscores
    private static void integerBasesAndUnderscores() {
        System.out.println("== Integer Bases & Underscores ==");
        int dec = 255;                 // decimal
        int bin = 0b1111_1111;         // binary (since Java 7)
        int oct = 0377;                // octal (leading 0)
        int hex = 0xFF;                // hexadecimal

        int hexWithUnderscores = 0xCAFE_BABE; // int literal; value is negative when printed as signed int
        long binWithUnderscores = 0b1101_0101_1001_1110_0001_0010_1010_0101L;

        System.out.println("dec = " + dec);
        System.out.println("bin = " + bin);
        System.out.println("oct = " + oct);
        System.out.println("hex = " + hex);
        System.out.println("hexWithUnderscores = " + hexWithUnderscores);
        System.out.println("binWithUnderscores = " + binWithUnderscores);

        // Valid use of underscores: between digits
        int oneMillion = 1_000_000;
        int weirdButValid = 1__2___3; // multiple underscores allowed between digits
        System.out.println("oneMillion = " + oneMillion);
        System.out.println("weirdButValid = " + weirdButValid);

        // Invalid underscore placements (do not compile):
        // int bad1 = _100;
        // int bad2 = 100_;
        // double bad3 = 1_.0;
        // double bad4 = 1._0;
        // int bad5 = 0x_FF;
        // int bad6 = 0b_1010;
        // double bad7 = 1.0_e10;  // underscore adjacent to exponent marker

        // Note: leading 0 means octal
        int eightOctal = 010;  // octal 10 == decimal 8
        System.out.println("octal 010 as decimal = " + eightOctal);

        System.out.println();
    }

    // 3) Ranges and overflow behavior
    private static void integerRangesAndOverflow() {
        System.out.println("== Integer Ranges & Overflow ==");
        System.out.println("Byte.MIN_VALUE = " + Byte.MIN_VALUE + ", MAX_VALUE = " + Byte.MAX_VALUE);
        System.out.println("Short.MIN_VALUE = " + Short.MIN_VALUE + ", MAX_VALUE = " + Short.MAX_VALUE);
        System.out.println("Integer.MIN_VALUE = " + Integer.MIN_VALUE + ", MAX_VALUE = " + Integer.MAX_VALUE);
        System.out.println("Long.MIN_VALUE = " + Long.MIN_VALUE + ", MAX_VALUE = " + Long.MAX_VALUE);

        int max = Integer.MAX_VALUE;
        int overflow = max + 1; // wraps around
        System.out.println("Integer.MAX_VALUE + 1 = " + overflow);

        byte bb = 127;
        byte bbOverflow = (byte) (bb + 1); // wraps to -128
        System.out.println("byte 127 + 1 = " + bbOverflow);

        // For integer division by zero:
        // int x = 1 / 0;        // compile-time error if both are constants
        // int a = 1, z = 0;     // int c = a / z; // compiles but throws at runtime

        System.out.println();
    }

    // 4) Floating-point literals and behavior
    private static void floatingPointLiterals() {
        System.out.println("== Floating-Point Literals ==");
        // double is default for floating literals
        double d1 = 3.141592653589793;     // double literal
        float f1 = 3.1415927F;             // float needs 'F' or 'f'
        double sci = 6.022e23;             // scientific notation
        double withUnderscores = 1_234.56_78; // underscores allowed between digits

        // Hex floating-point literals: 0x1.8p1 == 1.5 * 2^1 == 3.0
        double hexFloat = 0x1.8p1;

        System.out.println("double d1 = " + d1);
        System.out.println("float f1 = " + f1);
        System.out.println("sci = " + sci);
        System.out.println("withUnderscores = " + withUnderscores);
        System.out.println("hexFloat 0x1.8p1 = " + hexFloat);

        // Special values
        double posInf = 1.0 / 0.0;
        double negInf = -1.0 / 0.0;
        double nan = 0.0 / 0.0;

        System.out.println("Positive Infinity = " + posInf);
        System.out.println("Negative Infinity = " + negInf);
        System.out.println("NaN = " + nan);
        System.out.println("NaN == NaN? " + (nan == Double.NaN)); // always false
        System.out.println("Double.isNaN(nan)? " + Double.isNaN(nan));

        // Precision example
        double sum = 0.1 + 0.2;
        System.out.println("0.1 + 0.2 = " + sum + " (not exactly 0.3)");

        // Invalid examples (do not compile):
        // float badFloat = 3.14;      // needs 'F'
        // double badUnderscore = _1.0; // underscores cannot start a literal

        System.out.println();
    }

    // 5) char literals, escapes, and Unicode
    private static void charLiteralsAndUnicode() {
        System.out.println("== char Literals & Unicode ==");
        char c1 = 'A';
        char c2 = 65;             // decimal code point for 'A'
        char c3 = '\u0041';       // Unicode escape for 'A'
        char newline = '\n';
        char tab = '\t';
        char backslash = '\\';
        char singleQuote = '\'';
        char doubleQuote = '\"';
        char copyright = '©';     // U+00A9, within BMP

        System.out.println("c1 = " + c1 + ", as int = " + (int) c1);
        System.out.println("c2 = " + c2 + ", as int = " + (int) c2);
        System.out.println("c3 = " + c3 + ", as int = " + (int) c3);
        System.out.println("newline and tab will format this:\n\tIndented line");
        System.out.println("backslash = " + backslash + ", singleQuote = " + singleQuote + ", doubleQuote = " + doubleQuote);
        System.out.println("copyright = " + copyright + ", code = " + (int) copyright);

        // char is unsigned 16-bit, range 0..65535
        System.out.println("Character.MIN_VALUE (as int) = " + (int) Character.MIN_VALUE);
        System.out.println("Character.MAX_VALUE (as int) = " + (int) Character.MAX_VALUE);

        // char arithmetic and promotion to int
        char ch = 'A';
        int next = ch + 1; // promoted to int
        char nextChar = (char) next;
        System.out.println("'A' + 1 = " + next + " which is '" + nextChar + "'");

        // Supplementary characters (beyond BMP) require surrogate pairs
        char high = '\uD83D'; // high surrogate
        char low = '\uDE03';  // low surrogate
        String emoji = "" + high + low; // U+1F603 😃
        System.out.println("Emoji from surrogate pair: " + emoji);

        // Invalid char literals (do not compile):
        // char empty = '';           // empty
        // char tooMany = 'AB';       // more than one code unit
        // char negative = -1;        // char cannot be negative
        // char tooBig = 65536;       // out of range

        System.out.println();
    }

    // 6) boolean literals
    private static void booleanLiterals() {
        System.out.println("== boolean Literals ==");
        boolean t = true;
        boolean f = false;

        if (t && !f) {
            System.out.println("true && !false is true");
        }

        // Invalid boolean usage (do not compile):
        // boolean b = 1;    // cannot assign numeric to boolean
        // if (1) { }        // condition must be boolean, not int

        System.out.println();
    }

    // 7) Numeric promotion rules and casting
    private static void numericPromotionAndCasting() {
        System.out.println("== Numeric Promotion & Casting ==");
        byte b1 = 10;
        byte b2 = 20;

        // byte + byte -> int (binary numeric promotion)
        // byte b3 = b1 + b2; // does not compile; needs cast
        byte b3 = (byte) (b1 + b2);
        System.out.println("byte b3 = (byte) (b1 + b2) = " + b3);

        // Compile-time constants within range allow assignment without cast
        final byte fb1 = 10;
        final byte fb2 = 20;
        byte fb3 = (byte) (fb1 + fb2); // explicit cast ok
        byte fb4 = 30;                 // same constant as (fb1 + fb2), assignable directly
        System.out.println("final byte fb1+fb2 within range -> fb4 = " + fb4);

        // But if the sum is out of range, still illegal even with finals:
        // final byte fbx = 120, fby = 10;
        // byte fbBad = fbx + fby; // 130 out of byte range, does not compile

        // Widening conversions (safe)
        int i = b1;           // byte -> int
        long l = i;           // int -> long
        float f = l;          // long -> float (may lose precision but widening per spec)
        double d = f;         // float -> double

        System.out.println("Widening: byte->int->long->float->double yields: " + d);

        // Narrowing conversions (require cast, may lose data)
        int bigInt = 1_000_000_000;
        short narrowed = (short) bigInt; // overflow/ truncation
        System.out.println("Narrowing int 1_000_000_000 to short = " + narrowed);

        double dbl = 123.987;
        int truncated = (int) dbl; // fractional part discarded
        System.out.println("Casting double 123.987 to int = " + truncated);

        System.out.println();
    }

    // 8) Default values of primitive fields (not for local variables)
    private static void defaultValuesOfFields() {
        System.out.println("== Default Values of Primitive Fields ==");
        System.out.println("Defaults.byte = " + Defaults.by);
        System.out.println("Defaults.short = " + Defaults.sh);
        System.out.println("Defaults.int = " + Defaults.in);
        System.out.println("Defaults.long = " + Defaults.lo);
        System.out.println("Defaults.float = " + Defaults.fl);
        System.out.println("Defaults.double = " + Defaults.de);
        System.out.println("Defaults.char (as int) = " + (int) Defaults.ch);
        System.out.println("Defaults.boolean = " + Defaults.bo);
        System.out.println();
    }

    static class Defaults {
        static byte by;     // 0
        static short sh;    // 0
        static int in;      // 0
        static long lo;     // 0L
        static float fl;    // 0.0f
        static double de;   // 0.0d
        static char ch;     // '\u0000'
        static boolean bo;  // false
    }
}