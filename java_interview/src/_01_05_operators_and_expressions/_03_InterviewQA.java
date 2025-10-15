package _01_05_operators_and_expressions;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

/**
 * Operators & Expressions — Interview Q&A (Basic → Advanced)
 * Run this file to see concise demonstrations and answers inside comments and prints.
 */
public class _03_InterviewQA {

    public static void main(String[] args) {
        System.out.println("Operators & Expressions — Interview Q&A (Basic → Advanced)");
        q01_arithmeticBasics();
        q02_integerDivision();
        q03_moduloNegatives();
        q04_prePostIncrement();
        q05_assignmentAndEquality();
        q06_compoundAssignmentCasting();
        q07_objectEquality();
        q08_logicalVsBitwise();
        q09_shortCircuitSideEffects();
        q10_bitwiseBasics();
        q11_shiftOperators();
        q12_shiftCountMasking();
        q13_numericPromotion();
        q14_operatorPrecedence();
        q15_evaluationOrder();
        q16_sideEffectsInExpressions();
        q17_overflowUnderflow();
        q18_floatingPointPrecision();
        q19_divisionByZero();
        q20_ternaryOperator();
        q21_stringConcatVsArithmetic();
        q22_charAndUnicodeArithmetic();
        q23_booleanBitwiseOperators();
        q24_xorUseCases();
        q25_signExtensionAndMasking();
        q26_bitFlagsPatterns();
        q27_nanAndComparisons();
        q28_unboxingPitfall();
        q29_stringInterningAndPlus();
        q30_practicalTips();
        System.out.println("\nDone.");
    }

    // Helper for headings
    private static void header(String title) {
        System.out.println("\nQ) " + title);
    }

    private static String bin(int v) {
        return "0b" + Integer.toBinaryString(v);
    }

    private static String bin(long v) {
        return "0b" + Long.toBinaryString(v);
    }

    // 1) Arithmetic basics
    private static void q01_arithmeticBasics() {
        header("What are basic arithmetic operators and their behavior?");
        int a = 7, b = 3;
        System.out.println("a+b=" + (a + b) + ", a-b=" + (a - b) + ", a*b=" + (a * b));
        System.out.println("a/b (int)=" + (a / b) + ", a%b=" + (a % b));
        System.out.println("a/(double)b=" + (a / (double) b));
    }

    // 2) Integer vs floating division
    private static void q02_integerDivision() {
        header("How does integer vs floating-point division work?");
        System.out.println("7/2 (int): " + (7 / 2) + ", 7/2.0 (double): " + (7 / 2.0));
        System.out.println("(double)(7/2): " + ((double) (7 / 2)) + " vs ((double)7)/2: " + (((double) 7) / 2));
    }

    // 3) Modulo with negatives
    private static void q03_moduloNegatives() {
        header("How does % behave with negatives?");
        System.out.println("5%2=" + (5 % 2) + ", 5%-2=" + (5 % -2));
        System.out.println("-5%2=" + (-5 % 2) + ", -5%-2=" + (-5 % -2));
        System.out.println("Remainder has the same sign as dividend (left operand).");
    }

    // 4) ++ and -- (pre vs post)
    private static void q04_prePostIncrement() {
        header("Difference between ++i and i++?");
        int i = 1;
        int x = i++ + ++i; // i++ returns 1 (i becomes 2), ++i increments to 3 and returns 3 → x=4, i=3
        System.out.println("i++ + ++i with i starting 1: x=" + x + ", final i=" + i);
        int j = 5;
        int y = j++ + j++; // 5 + 6; final j=7
        System.out.println("j++ + j++ with j starting 5: y=" + y + ", final j=" + j);
    }

    // 5) Assignment vs equality
    private static void q05_assignmentAndEquality() {
        header("Difference between = and ==? Can assignments chain?");
        int y = 0;
        int x;
        x = y = 5; // chained assignment
        System.out.println("After x=y=5 → x=" + x + ", y=" + y + ". '=' assigns, '==' compares.");
    }

    // 6) Compound assignment implicit cast and overflow
    private static void q06_compoundAssignmentCasting() {
        header("What is special about compound assignments (+=, -=, ...)?");
        byte b = 120;
        // b = b + 10; // Compile error (int → byte). Demonstrated via comment.
        b += 10; // Implicit cast back to byte; possible overflow
        System.out.println("byte b=120; b+=10 → b=" + b + " (overflow to -126). Compound ops include implicit cast.");
    }

    // 7) == vs equals for Objects (and wrapper caching)
    private static void q07_objectEquality() {
        header("== vs equals() for Objects and wrapper caching?");
        Integer a1 = 127, b1 = 127;
        Integer a2 = 128, b2 = 128;
        System.out.println("Integer 127 cache → a1==b1? " + (a1 == b1) + ", a1.equals(b1)? " + a1.equals(b1));
        System.out.println("Integer 128 no-cache → a2==b2? " + (a2 == b2) + ", a2.equals(b2)? " + a2.equals(b2));
        String s1 = "hello", s2 = "he" + "llo", s3 = new String("hello");
        System.out.println("String pool: s1==s2? " + (s1 == s2) + ", s1==s3? " + (s1 == s3) + ", s1.equals(s3)? " + s1.equals(s3));
    }

    // 8) Logical vs bitwise with booleans and short-circuit
    private static void q08_logicalVsBitwise() {
        header("Difference between logical (&&, ||) and bitwise (&, |) on booleans?");
        boolean a = true, b = false;
        System.out.println("true&&false=" + (a && b) + ", true&false=" + (a & b));
        System.out.println("true||false=" + (a || b) + ", true|false=" + (a | b) + ", true^false=" + (a ^ b));
        int n = 0;
        try {
            boolean unsafe = (n != 0) & (10 / n > 1); // Right side evaluated → ArithmeticException
            System.out.println(unsafe);
        } catch (ArithmeticException ex) {
            System.out.println("(n!=0) & (10/n>1) threw " + ex.getClass().getSimpleName() + " (no short-circuit).");
        }
        boolean safe = (n != 0) && (10 / n > 1); // Short-circuits safely
        System.out.println("(n!=0) && (10/n>1) → " + safe);
    }

    // 9) Short-circuit and side effects
    private static void q09_shortCircuitSideEffects() {
        header("How does short-circuiting affect side effects?");
        int x = 0;
        boolean r1 = (x++ > 0) && (++x > 1); // left false → right not evaluated
        System.out.println("x=0; (x++>0)&&(++x>1) → r1=" + r1 + ", x=" + x);
        x = 0;
        boolean r2 = (x++ > 0) & (++x > 1); // both evaluated
        System.out.println("x=0; (x++>0)&(++x>1) → r2=" + r2 + ", x=" + x);
    }

    // 10) Bitwise basics (& | ^ ~)
    private static void q10_bitwiseBasics() {
        header("Bitwise operators (&, |, ^, ~) on ints?");
        int p = 0b1010, q = 0b1100;
        System.out.println("p=" + bin(p) + ", q=" + bin(q));
        System.out.println("p&q=" + bin(p & q) + ", p|q=" + bin(p | q) + ", p^q=" + bin(p ^ q) + ", ~p=" + bin(~p));
    }

    // 11) Shifts: << >> >>>
    private static void q11_shiftOperators() {
        header("Difference between <<, >>, and >>>?");
        int v = -8;
        System.out.println("8<<2=" + (8 << 2) + " (" + bin(8 << 2) + ")");
        System.out.println("-8>>1=" + (v >> 1) + " (" + bin(v >> 1) + ") arithmetic shift keeps sign");
        System.out.println("-8>>>1=" + (v >>> 1) + " (" + bin(v >>> 1) + ") logical shift adds zeros");
    }

    // 12) Shift count masking
    private static void q12_shiftCountMasking() {
        header("How are shift counts handled (>31 for int, >63 for long)?");
        int a = 1 << 33; // 33 & 0x1F = 1
        long b = 1L << 65; // 65 & 0x3F = 1
        System.out.println("1<<33 == 1<<1 → " + a);
        System.out.println("1L<<65 == 1L<<1 → " + b);
    }

    // 13) Numeric promotion and casting
    private static void q13_numericPromotion() {
        header("What is numeric promotion in expressions?");
        byte b1 = 5, b2 = 6;
        int sum = b1 + b2; // promoted to int
        char c = 'A';
        int code = c + 1; // char → int
        char next = (char) (c + 1);
        double mix = 1 + 2.5; // int → double
        long longMix = 1_000_000_000L + 10; // int → long
        System.out.println("byte+byte -> int: " + sum);
        System.out.println("'A'+1 -> int " + code + ", cast back to char '" + next + "'");
        System.out.println("1+2.5 -> double " + mix + ", long+int -> long " + longMix);
    }

    // 14) Operator precedence essentials
    private static void q14_operatorPrecedence() {
        header("Which operators bind tighter? Examples:");
        boolean p1 = true || false && false; // && before ||
        System.out.println("true || false && false → " + p1);
        int p2 = ~1 << 2; // ~ first, then <<
        System.out.println("~1<<2 → " + p2);
        int p3 = 1 + 2 << 3; // + before <<
        System.out.println("1+2<<3 → " + p3 + " (3<<3=24)");
        boolean p4 = true | false == true; // == before |
        System.out.println("true | false == true → " + p4 + " (parsed as true | (false==true))");
        System.out.println("Use parentheses to be explicit.");
    }

    // 15) Evaluation order (left-to-right)
    private static void q15_evaluationOrder() {
        header("What is the evaluation order of operands?");
        System.out.print("Order: ");
        int r = trace("a", 1) + trace("b", 2) * trace("c", 3);
        System.out.println("\nResult: " + r + " (operands eval left-to-right; precedence affects grouping, not order).");
    }

    private static int trace(String label, int value) {
        System.out.print(label + value + " ");
        return value;
    }

    // 16) Side effects in expressions (i = i++;)
    private static void q16_sideEffectsInExpressions() {
        header("Why does i = i++; not increment i?");
        int i = 1;
        i = i++; // post-increment returns old value, which is assigned back
        System.out.println("Start i=1; i=i++; → i=" + i + " (use i++ or ++i on its own line).");
        int j = 1;
        j = ++j;
        System.out.println("Start j=1; j=++j; → j=" + j);
    }

    // 17) Overflow/underflow and detecting it
    private static void q17_overflowUnderflow() {
        header("How to detect/prevent overflow/underflow?");
        int max = Integer.MAX_VALUE, min = Integer.MIN_VALUE;
        System.out.println("MAX_VALUE+1 → " + (max + 1) + " (wraps)");
        System.out.println("MIN_VALUE-1 → " + (min - 1) + " (wraps)");
        try {
            Math.addExact(max, 1);
        } catch (ArithmeticException ex) {
            System.out.println("Math.addExact detects overflow: " + ex.getMessage());
        }
        // BigInteger for arbitrary-size integers
        BigInteger bi = BigInteger.valueOf(max).add(BigInteger.ONE);
        System.out.println("BigInteger handles overflow safely: " + bi);
        char ch = '\uffff';
        ch++;
        System.out.println("char overflow wraps (\\uffff)++ → " + (int) ch);
    }

    // 18) Floating point precision, comparison, BigDecimal
    private static void q18_floatingPointPrecision() {
        header("Floating-point precision pitfalls and comparisons?");
        double d = 0.1 + 0.2;
        System.out.println("0.1+0.2=" + d + " (binary rounding)");
        System.out.println("Direct compare to 0.3 → " + (d == 0.3));
        double eps = 1e-9;
        System.out.println("Compare with epsilon → " + (Math.abs(d - 0.3) < eps));
        BigDecimal bd1 = new BigDecimal(0.1);
        BigDecimal bd2 = new BigDecimal(0.2);
        BigDecimal bd3 = BigDecimal.valueOf(0.1);
        BigDecimal bd4 = BigDecimal.valueOf(0.2);
        System.out.println("new BigDecimal(0.1)+new BigDecimal(0.2) = " + bd1.add(bd2));
        System.out.println("BigDecimal.valueOf(0.1)+valueOf(0.2) = " + bd3.add(bd4));
    }

    // 19) Division by zero (int vs float/double)
    private static void q19_divisionByZero() {
        header("What happens on division by zero?");
        try {
            int x = 1 / 0;
            System.out.println(x);
        } catch (ArithmeticException ex) {
            System.out.println("int/0 → ArithmeticException");
        }
        System.out.println("1.0/0.0 → " + (1.0 / 0.0) + " (Infinity)");
        System.out.println("0.0/0.0 → " + (0.0 / 0.0) + " (NaN)");
    }

    // 20) Ternary operator ?: usage
    private static void q20_ternaryOperator() {
        header("When to use the ternary operator ?: ?");
        int n = -1;
        String sign = n > 0 ? "positive" : (n < 0 ? "negative" : "zero");
        System.out.println("n=-1 → " + sign + " (prefer parentheses for nested ternaries).");
    }

    // 21) String concatenation vs arithmetic
    private static void q21_stringConcatVsArithmetic() {
        header("How does + work with Strings vs numbers?");
        int a = 2, b = 3;
        System.out.println("\"Sum: \"+a+b → " + ("Sum: " + a + b));
        System.out.println("\"Sum: \"+(a+b) → " + ("Sum: " + (a + b)));
        System.out.println("'1'+2 → " + ('1' + 2) + " (char code math), \"1\"+2 → " + ("1" + 2));
    }

    // 22) char arithmetic and Unicode notes
    private static void q22_charAndUnicodeArithmetic() {
        header("What is char arithmetic?");
        char c = 'A';
        System.out.println("'A'+3 → int " + (c + 3) + ", cast back: '" + (char) (c + 3) + "'");
        // Note: char is a UTF-16 code unit; some characters require two chars (surrogates).
    }

    // 23) Boolean bitwise forms (&, |, ^ on booleans)
    private static void q23_booleanBitwiseOperators() {
        header("Can &, |, ^ be used with booleans?");
        boolean x = true, y = true, z = false;
        System.out.println("true^true=" + (x ^ y) + ", true^false=" + (x ^ z));
        System.out.println("true&true=" + (x & y) + ", true|false=" + (x | z) + " (no short-circuit).");
    }

    // 24) XOR use-cases (toggle, swap)
    private static void q24_xorUseCases() {
        header("Practical XOR uses: toggle bit, swap without temp");
        int flags = 0b1010;
        int bit = 1; // toggle bit 0
        int toggled = flags ^ (1 << bit);
        System.out.println("flags=" + bin(flags) + ", toggle bit1 → " + bin(toggled));
        int a = 5, b = 7;
        a ^= b; b ^= a; a ^= b;
        System.out.println("Swap via XOR → a=7, b=5: a=" + a + ", b=" + b);
    }

    // 25) Sign extension and masking (bytes)
    private static void q25_signExtensionAndMasking() {
        header("Why does casting byte to int sign-extend? How to get unsigned value?");
        byte bb = (byte) 0xF8; // -8
        int ii = bb; // sign-extended
        int unsigned = bb & 0xFF; // mask
        System.out.println("byte 0xF8 → as int: " + ii + ", unsigned via &0xFF: " + unsigned);
    }

    // 26) Bit-flags patterns (set/clear/check)
    private static void q26_bitFlagsPatterns() {
        header("Common bit-flag operations (set, clear, check, toggle).");
        final int READ = 1 << 0, WRITE = 1 << 1, EXEC = 1 << 2;
        int perms = 0;
        perms |= READ | WRITE; // set
        boolean canExec = (perms & EXEC) != 0; // check
        perms &= ~WRITE; // clear
        perms ^= EXEC; // toggle
        System.out.println("perms=" + bin(perms) + ", canExec(before)=" + canExec + ", canExec(after)={(perms&EXEC)!=0}=" + (((perms & EXEC) != 0)));
    }

    // 27) NaN and comparisons
    private static void q27_nanAndComparisons() {
        header("How does NaN behave in comparisons?");
        double n1 = Double.NaN;
        System.out.println("NaN==NaN → " + (n1 == Double.NaN) + ", use Double.isNaN(n1): " + Double.isNaN(n1));
        System.out.println("NaN compared (>, <) is always false; != is true.");
    }

    // 28) Unboxing pitfalls (null wrappers)
    private static void q28_unboxingPitfall() {
        header("What happens when unboxing null?");
        Integer val = null;
        try {
            boolean gt = val > 0; // NPE due to unboxing
            System.out.println(gt);
        } catch (NullPointerException ex) {
            System.out.println("Integer val=null; val>0 → NullPointerException (unboxing).");
        }
        System.out.println("Use Objects.equals, null checks, or Optional.");
    }

    // 29) String interning and + operator notes
    private static void q29_stringInterningAndPlus() {
        header("Is '+' an overloaded operator in Java?");
        System.out.println("Only '+' is overloaded for String concatenation; others are not.");
        String s = "a" + "b" + "c"; // compile-time folded
        String t = "abc";
        System.out.println("\"a\"+\"b\"+\"c\" is interned like \"abc\": s==t → " + (s == t));
        String r = "a" + getDynamic(); // runtime uses StringBuilder
        System.out.println("\"a\"+dynamic built at runtime: " + r);
    }

    private static String getDynamic() {
        return String.valueOf(System.nanoTime()).substring(0, 1);
    }

    // 30) Practical tips / best practices
    private static void q30_practicalTips() {
        header("Best practices (quick tips):");
        System.out.println("- Prefer parentheses to clarify precedence.");
        System.out.println("- Avoid side effects inside complex expressions (especially with ++/--).");
        System.out.println("- Use &&/|| for control flow; avoid &/| unless intentional.");
        System.out.println("- Beware integer division; cast early to double if needed.");
        System.out.println("- Compare doubles with an epsilon; use BigDecimal for money.");
        System.out.println("- Use Math.addExact/multiplyExact to detect overflows.");
        System.out.println("- For bit flags, use named constants and masks.");
        System.out.println("- For Object equality, prefer equals()/Objects.equals().");
        System.out.println("- Know shift counts are masked (int: 0x1F, long: 0x3F).");
    }
}