package _01_05_operators_and_expressions;

public class _02_Examples {

    public static void main(String[] args) {
        section("Literals and basic expressions");
        literalsAndBasicExpressions();

        section("Assignment and compound assignment");
        assignmentOperators();

        section("Arithmetic operators");
        arithmeticOperators();

        section("Unary operators (+, -, ~, !)");
        unaryOperators();

        section("Increment and decrement (prefix vs postfix)");
        incrementDecrement();

        section("Comparison and equality operators");
        comparisonOperators();

        section("Logical operators (&&, ||, !), short-circuit vs non-short-circuit");
        logicalOperators();

        section("Bitwise and shift operators");
        bitwiseAndShiftOperators();

        section("Ternary (conditional) operator");
        ternaryOperator();

        section("Operator precedence and casting nuances");
        precedenceAndAssociativity();

        section("Numeric promotion, casting, and overflow");
        numericPromotionAndCasting();

        section("String concatenation + and order of evaluation");
        stringConcatenation();

        section("Object equality: == vs equals");
        objectEquality();

        section("instanceof and casting");
        instanceofAndCasts();

        section("Division by zero, Infinity, and NaN");
        divisionByZeroAndNaN();

        section("Remainder with negatives");
        remainderWithNegatives();

        section("Side effects and evaluation order");
        sideEffectsAndEvalOrder();

        section("Useful bit tricks with masks");
        bitTricks();

        section("Autoboxing pitfalls");
        autoboxingPitfalls();

        section("Math exact operations to detect overflow");
        mathExactOperations();

        section("Done");
    }

    // --- Sections ---

    static void literalsAndBasicExpressions() {
        // Numeric literals (decimal, binary, hex, with underscores)
        int million = 1_000_000;
        int bin = 0b1010;       // 10
        int hex = 0xFF;         // 255
        System.out.println("million=" + million + ", bin=" + bin + ", hex=" + hex);

        // Basic arithmetic with precedence: * before +
        int expr1 = 2 + 3 * 4;            // 2 + 12 = 14
        double expr2 = (2 + 3) * 4.0;     // 5 * 4.0 = 20.0
        System.out.println("2 + 3 * 4 = " + expr1);
        System.out.println("(2 + 3) * 4.0 = " + expr2);

        // Boolean expressions
        boolean b = true && false || true; // true && false = false; false || true = true
        System.out.println("true && false || true = " + b);

        // Char arithmetic promotes to int
        char ch = 'A';
        char next = (char) (ch + 1); // 'B'
        int codePoint = ch + 1;      // 66 (int)
        System.out.println("'A' + 1 as char = " + next + ", as int = " + codePoint);
    }

    static void assignmentOperators() {
        // Simple and chained assignment (associativity right-to-left)
        int x, y, z;
        x = y = z = 5;
        System.out.println("x=" + x + ", y=" + y + ", z=" + z);

        // Compound assignment
        x += 3;      // x = x + 3
        x *= 2;      // x = x * 2
        y %= 4;      // y = y % 4
        System.out.println("After compounds -> x=" + x + ", y=" + y);

        // Compound assignment with implicit cast
        byte b = 1;
        b += 1; // OK (implicit cast back to byte)
        System.out.println("byte after b+=1: " + b);

        // Without compound, explicit cast required:
        byte c = 1;
        c = (byte) (c + 1);
        System.out.println("byte after c=(byte)(c+1): " + c);
    }

    static void arithmeticOperators() {
        int a = 7 / 2;          // Integer division -> 3
        double b = 7 / 2.0;     // Floating division -> 3.5
        int m = 7 % 2;          // Remainder -> 1
        int neg = -7 % 2;       // -> -1 (same sign as dividend)
        System.out.println("7/2=" + a + ", 7/2.0=" + b + ", 7%2=" + m + ", -7%2=" + neg);

        // Overflow wraps around (2's complement)
        int big = Integer.MAX_VALUE;
        int wrapped = big + 1; // wraps to Integer.MIN_VALUE
        System.out.println("Integer.MAX_VALUE + 1 = " + wrapped);
    }

    static void unaryOperators() {
        int n = 5;
        int pos = +n;       // no-op
        int neg = -n;       // negation
        System.out.println("+5=" + pos + ", -5=" + neg);

        // Bitwise NOT (~)
        int mask = 0b0000_1111;
        int notMask = ~mask;
        System.out.println("mask=      " + bin(mask));
        System.out.println("~mask=     " + bin(notMask));

        // Logical NOT (!)
        boolean t = true;
        System.out.println("!true = " + (!t));
    }

    static void incrementDecrement() {
        int i = 5;
        System.out.println("i=" + i);
        System.out.println("i++ -> " + (i++)); // returns 5, then i becomes 6
        System.out.println("after i++: i=" + i);
        System.out.println("++i -> " + (++i)); // increments to 7, then returns 7
        System.out.println("after ++i: i=" + i);

        // Using multiple ++ in the same expression is legal but hard to read
        int k = 1;
        int sum = k++ + k++; // left-to-right: uses 1 then 2; k ends as 3; sum=3
        System.out.println("k++ + k++ = " + sum + ", final k=" + k);
    }

    static void comparisonOperators() {
        // Numeric and char comparisons
        System.out.println("3 < 5: " + (3 < 5));
        System.out.println("'A' < 'a': " + ('A' < 'a'));

        // Floating-point: avoid == for non-integers, use epsilon
        double x = 0.1 + 0.2; // not exactly 0.3 in binary floating-point
        boolean eq = Math.abs(x - 0.3) < 1e-9;
        System.out.println("0.1 + 0.2 == 0.3 (epsilon): " + eq);

        // Equality vs inequality
        System.out.println("10 == 10: " + (10 == 10));
        System.out.println("10 != 20: " + (10 != 20));
    }

    static void logicalOperators() {
        // Short-circuit: || and &&
        System.out.println("Short-circuit OR: ");
        System.out.println("Result: " + (t("A") || t("B"))); // B not evaluated

        System.out.println("Short-circuit AND: ");
        System.out.println("Result: " + (f("C") && t("D"))); // D not evaluated

        // Non-short-circuit: | and & with booleans evaluate both sides
        System.out.println("Non-short-circuit OR (|): ");
        System.out.println("Result: " + (t("E") | t("F"))); // both evaluated

        System.out.println("Non-short-circuit AND (&): ");
        System.out.println("Result: " + (f("G") & t("H"))); // both evaluated

        // XOR on booleans
        System.out.println("true ^ false: " + (true ^ false));
        System.out.println("true ^ true: " + (true ^ true));
    }

    static void bitwiseAndShiftOperators() {
        int v = 0b0101; // 5

        // Set bit 1 (0-based)
        v |= (1 << 1); // set bit 1
        // Clear bit 2
        v &= ~(1 << 2); // clear bit 2
        // Toggle bit 0
        v ^= (1 << 0); // toggle bit 0
        System.out.println("v bits: " + bin(v));

        // Shifts
        int p = 8;     // 000...1000
        int p2 = p << 2;  // 32
        int n = -8;    // 111...1000
        int ar = n >> 1;  // arithmetic right shift: preserves sign
        int lr = n >>> 1; // logical right shift: inserts zeros
        System.out.println("8<<2 = " + p2);
        System.out.println("-8>>1 (arith) = " + ar + "  " + bin(ar));
        System.out.println("-8>>>1 (logic) = " + lr + " " + bin(lr));
    }

    static void ternaryOperator() {
        int a = 10, b = 20;
        int max = a > b ? a : b;
        String type = (max % 2 == 0) ? "even" : "odd";
        System.out.println("max=" + max + " which is " + type);

        // Ternary returns a value and can be nested (readability matters)
        int score = 87;
        String grade = score >= 90 ? "A" : score >= 80 ? "B" : "C";
        System.out.println("score=" + score + " -> grade=" + grade);
    }

    static void precedenceAndAssociativity() {
        System.out.println("1 + 2 * 3 = " + (1 + 2 * 3));
        System.out.println("(1 + 2) * 3 = " + ((1 + 2) * 3));

        // Assignment is right-associative
        int a, b, c;
        a = b = c = 1;
        System.out.println("a=b=c=1 -> a=" + a + ", b=" + b + ", c=" + c);

        // Cast precedence vs multiplication
        int x1 = (int) 1.9 * 2;     // (int)1.9 = 1; 1*2 = 2
        int x2 = (int) (1.9 * 2);   // 1.9*2 = 3.8; (int)3.8 = 3
        System.out.println("(int)1.9 * 2 = " + x1 + ", (int)(1.9*2) = " + x2);
    }

    static void numericPromotionAndCasting() {
        // Promotion of smaller types to int in arithmetic
        byte b1 = 100, b2 = 30;
        int sum = b1 + b2; // promoted to int
        byte sumByte = (byte) (b1 + b2); // may overflow
        System.out.println("b1+b2 as int=" + sum + ", as byte=" + sumByte);

        // Compound assignment performs implicit cast with overflow
        byte b3 = 120;
        b3 += 10; // (byte)(120+10) -> (byte)130 -> -126
        System.out.println("b3 after b3+=10 -> " + b3);

        // Widening conversions are safe (no cast)
        int i = 123;
        long L = i;          // widening int -> long
        double d = L;        // widening long -> double
        System.out.println("int->long->double: " + i + " -> " + L + " -> " + d);

        // Char arithmetic and casting
        char c = 'A';
        int code = c + 2;            // 67
        char c2 = (char) (c + 2);    // 'C'
        System.out.println("'A'+2 -> int=" + code + ", char=" + c2);
    }

    static void stringConcatenation() {
        // + is overloaded for Strings
        System.out.println("\"1 + 2 = \" + 1 + 2 -> " + ("1 + 2 = " + 1 + 2));
        System.out.println("\"1 + 2 = \" + (1 + 2) -> " + ("1 + 2 = " + (1 + 2)));

        // char + char -> int (code point sum)
        System.out.println("'A' + 'B' -> " + ('A' + 'B'));
        // String + char -> concatenation
        System.out.println("\"A\" + 'B' -> " + ("A" + 'B'));
    }

    static void objectEquality() {
        String s1 = "hello";
        String s2 = new String("hello"); // different object
        String s3 = "hello";             // interned same literal as s1

        System.out.println("s1 == s2: " + (s1 == s2));        // false (reference)
        System.out.println("s1.equals(s2): " + s1.equals(s2)); // true (content)
        System.out.println("s1 == s3: " + (s1 == s3));        // true (interned)
    }

    static void instanceofAndCasts() {
        Object obj = "text";
        if (obj instanceof String) {
            String str = (String) obj; // safe after instanceof
            System.out.println("String length: " + str.length());
        }
        System.out.println("obj instanceof Integer: " + (obj instanceof Integer));
    }

    static void divisionByZeroAndNaN() {
        // Integer division by zero -> exception
        try {
            int x = 1 / 0;
            System.out.println(x);
        } catch (ArithmeticException ex) {
            System.out.println("1/0 (int) throws: " + ex);
        }

        // Floating point division by zero -> Infinity/-Infinity
        double posInf = 1.0 / 0.0;
        double negInf = -1.0 / 0.0;
        double nan = 0.0 / 0.0;
        System.out.println("1.0/0.0 = " + posInf + ", -1.0/0.0 = " + negInf + ", 0.0/0.0 = " + nan);
        System.out.println("Double.isNaN(0.0/0.0) = " + Double.isNaN(nan));
        System.out.println("NaN == NaN: " + (nan == nan));
    }

    static void remainderWithNegatives() {
        System.out.println("5 % 3 = " + (5 % 3));      // 2
        System.out.println("-5 % 3 = " + (-5 % 3));    // -2
        System.out.println("5 % -3 = " + (5 % -3));    // 2
        System.out.println("-5 % -3 = " + (-5 % -3));  // -2
    }

    static void sideEffectsAndEvalOrder() {
        // Operands are evaluated left-to-right in Java
        int result = f("x", 1) + f("y", 2) * f("z", 3);
        System.out.println("result = " + result);
    }

    static void bitTricks() {
        // Unsigned byte to int
        byte b = (byte) 0xF0;        // -16 as signed byte
        int unsigned = b & 0xFF;     // 240
        System.out.println("byte b=(byte)0xF0 -> b=" + b + ", b&0xFF=" + unsigned);

        // Working with flags
        int flags = 0;
        int READ = 1 << 0;   // 0001
        int WRITE = 1 << 1;  // 0010
        int EXEC = 1 << 2;   // 0100

        // Set READ and EXEC
        flags |= READ | EXEC;
        System.out.println("flags after set READ|EXEC: " + bin(flags) + " (" + flags + ")");

        // Test WRITE
        boolean canWrite = (flags & WRITE) != 0;
        System.out.println("WRITE set? " + canWrite);

        // Toggle EXEC
        flags ^= EXEC;
        System.out.println("flags after toggle EXEC: " + bin(flags) + " (" + flags + ")");

        // Clear READ
        flags &= ~READ;
        System.out.println("flags after clear READ: " + bin(flags) + " (" + flags + ")");

        // Pack two bytes into an int (little 16-bit)
        byte lo = 0x12, hi = 0x34;
        int packed = (hi & 0xFF) << 8 | (lo & 0xFF);
        System.out.println("packed 0x34 and 0x12 -> 0x" + Integer.toHexString(packed));
    }

    static void autoboxingPitfalls() {
        Integer a = 127, b = 127;
        Integer c = 128, d = 128;
        System.out.println("Integer 127 == 127: " + (a == b)); // true (cached)
        System.out.println("Integer 128 == 128: " + (c == d)); // false (not cached)
        System.out.println("Integer 128 equals 128: " + c.equals(d)); // true

        // Null unboxing throws NPE
        try {
            Integer x = null;
            int y = x; // unboxing null -> NPE
            System.out.println(y);
        } catch (NullPointerException e) {
            System.out.println("Unboxing null Integer throws: " + e);
        }
    }

    static void mathExactOperations() {
        // Detect overflow with Math.addExact / multiplyExact
        try {
            int ov = Math.addExact(Integer.MAX_VALUE, 1);
            System.out.println(ov);
        } catch (ArithmeticException e) {
            System.out.println("Math.addExact overflow: " + e.getMessage());
        }
        try {
            long ov2 = Math.multiplyExact(1_000_000_000L, 10L);
            System.out.println(ov2);
        } catch (ArithmeticException e) {
            System.out.println("Math.multiplyExact overflow: " + e.getMessage());
        }
    }

    // --- Helpers ---

    static void section(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }

    static boolean t(String label) {
        System.out.println("eval " + label + " -> true");
        return true;
    }

    static boolean f(String label) {
        System.out.println("eval " + label + " -> false");
        return false;
    }

    static int f(String label, int value) {
        System.out.println("eval " + label + " -> " + value);
        return value;
    }

    static String bin(int x) {
        String s = Integer.toBinaryString(x);
        return pad32(s);
    }

    static String pad32(String s) {
        if (s.length() >= 32) return s;
        StringBuilder sb = new StringBuilder(32);
        for (int i = 0; i < 32 - s.length(); i++) sb.append('0');
        sb.append(s);
        return sb.toString();
    }
}