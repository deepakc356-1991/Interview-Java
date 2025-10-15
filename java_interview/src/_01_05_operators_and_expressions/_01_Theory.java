package _01_05_operators_and_expressions;

/*
  Operators & Expressions — Theory and Practical Notes (Java)

  0) What is an expression?
     - An expression computes a value from operands and operators.
     - Examples: 1 + 2, a * b + c, x++, obj instanceof String, (int) 3.14, cond ? a : b, new int[5]
     - Expressions can have side effects (e.g., ++i, assignments, method calls).
     - Java guarantees left-to-right evaluation of operands (strict sequencing), but the grouping
       of operators (associativity) and operator precedence determine how expressions are parsed.

  1) Operator categories
     - Arithmetic: +, -, *, /, %
     - Unary: +, -, ++, --, ~, !
     - Assignment: =, plus compound assignments like +=, -=, *=, /=, %=, <<=, >>=, >>>=, &=, |=, ^=
     - Relational: <, <=, >, >=
     - Equality: ==, !=
     - Logical (boolean): &&, ||, !
     - Bitwise (integral/boolean): &, |, ^, ~; Shifts: <<, >>, >>>
     - Conditional (ternary): ?:
     - Type test and cast: instanceof, (T)expr
     - String concatenation: + (special-cased for String)
     - Member access (.), array index ([]), object creation (new) are also operators in the JLS sense.

  2) Precedence (highest to lowest) and associativity
     1.  Postfix:              expr++, expr--                             (left-to-right)
     2.  Unary:                ++expr, --expr, +, -, ~, !, (cast)          (right-to-left)
     3.  Multiplicative:       *, /, %                                     (left-to-right)
     4.  Additive:             +, -                                        (left-to-right)
     5.  Shift:                <<, >>, >>>                                 (left-to-right)
     6.  Relational:           <, <=, >, >=, instanceof                    (left-to-right)
     7.  Equality:             ==, !=                                      (left-to-right)
     8.  Bitwise AND:          &                                           (left-to-right)
     9.  Bitwise XOR:          ^                                           (left-to-right)
     10. Bitwise OR:           |                                           (left-to-right)
     11. Logical AND:          &&                                          (left-to-right)
     12. Logical OR:           ||                                          (left-to-right)
     13. Ternary:              ?:                                          (right-to-left)
     14. Assignment:           =, +=, -=, *=, /=, %=, <<=, >>=, >>>=,      (right-to-left)
                               &=, ^=, |=

     Notes:
     - Parentheses override precedence/associativity.
     - Operands are evaluated left-to-right even when precedence groups operators differently.

  3) Numeric promotions & conversions
     - Unary numeric promotion: byte, short, char -> int; int stays int; long->long; float->float; double->double.
     - Binary numeric promotion: for a op b, both sides are promoted to a common type:
         If either is double -> both to double;
         else if float -> both to float;
         else if long -> both to long;
         else -> both to int.
     - Widening conversions (safe): byte->short->int->long->float->double; char->int->long->float->double.
     - Narrowing conversions (explicit cast required) may overflow/lose precision.
     - Compound assignments (e.g., s += 1) include an implicit cast to the LHS type.

  4) Integer arithmetic
     - Overflow wraps around using two's complement (no exception). Use Math.addExact, multiplyExact, etc., to detect.
     - Integer division truncates toward zero (e.g., 5/2 = 2; -5/2 = -2).
     - Remainder (%) keeps the sign of the dividend (left operand):
         5 % -3 = 2; -5 % 3 = -2; -5 % -3 = -2.

  5) Floating-point arithmetic (IEEE 754)
     - double and float support NaN, +Infinity, -Infinity.
     - Division by zero: 1.0/0.0 -> +Infinity; -1.0/0.0 -> -Infinity; 0.0/0.0 -> NaN.
     - NaN != NaN (all comparisons with NaN are false except != which is true).
     - +0.0 == -0.0 is true, but 1/+0.0 is +Infinity while 1/-0.0 is -Infinity.

  6) Equality vs identity
     - Primitives: == and != compare values.
     - References: == and != compare object identity (same reference).
     - Use equals() to compare object content (unless identity semantics are intended).
     - Autoboxing pitfalls: '==' on wrappers may compare references; unboxing may throw NPE.

  7) Logical vs bitwise operators
     - && and || short-circuit (skip RHS if result known).
     - &, |, ^ on booleans do not short-circuit (always evaluate both sides).
     - &, |, ^ on integral types perform bitwise operations.
     - ~ is bitwise complement for integral types only.

  8) Shifts
     - << left shift; >> arithmetic right shift (sign-propagating); >>> logical right shift (zero-fill).
     - The shift distance is masked: for int, only low 5 bits used (mod 32); for long, low 6 bits (mod 64).

  9) ++ and --
     - Prefix (++i, --i): increments/decrements then yields the updated value.
     - Postfix (i++, i--): yields the old value then updates.

  10) Conditional operator (?:)
      - Ternary operator: cond ? exprIfTrue : exprIfFalse
      - Result type is determined by conditional expression rules (boxing/unboxing may occur).
      - Evaluate only the chosen branch.

  11) Casts and instanceof
      - (T)expr may be a narrowing or reference cast. Reference casts are checked at runtime (ClassCastException).
      - instanceof returns false for null. Since Java 16+, pattern matching: if (obj instanceof String s) { ... }
        (Use only if your source level supports it.)

  12) String concatenation
      - + concatenates when either operand is a String; non-String operands are converted via String.valueOf.
      - Left-to-right evaluation matters: "res: " + 1 + 2 -> "res: 12", but "res: " + (1 + 2) -> "res: 3".
      - null becomes "null" when concatenated with a String.

  13) Evaluation order & side effects
      - Operands are evaluated left-to-right.
      - Short-circuit operators may skip evaluating RHS (no side effects from RHS then).
      - Method arguments are evaluated left-to-right.

  14) Constant expressions and inlining
      - Compile-time constants (e.g., static final int X = 2 + 3) may be folded by the compiler and inlined across classes.
      - Changing a constant value requires recompilation of dependent classes to see the new value.

  15) No user-defined operator overloading in Java (except built-in String +).
*/

public class _01_Theory {

    public static void main(String[] args) {
        arithmeticBasics();
        incrementDecrementDemo();
        assignmentAndCompoundDemo();
        comparisonAndEqualityDemo();
        logicalVsBitwiseDemo();
        shiftDemo();
        stringConcatenationDemo();
        castingAndPromotionDemo();
        floatingPointCornerCases();
        evaluationOrderDemo();
        ternaryDemo();
        overflowDetectionDemo();
        instanceofAndCastsDemo();
        precedencePitfallDemo();
    }

    // 1) Arithmetic basics, integer vs floating point, remainder sign
    private static void arithmeticBasics() {
        System.out.println("== Arithmetic basics ==");
        int a = 7, b = 3;
        System.out.println("7 + 3 = " + (a + b)); // 10
        System.out.println("7 - 3 = " + (a - b)); // 4
        System.out.println("7 * 3 = " + (a * b)); // 21
        System.out.println("7 / 3 = " + (a / b)); // 2 (integer division truncates toward zero)
        System.out.println("7 % 3 = " + (a % b)); // 1

        // Remainder sign follows dividend (left operand)
        System.out.println("5 % -3 = " + (5 % -3));   // 2
        System.out.println("-5 % 3 = " + (-5 % 3));   // -2
        System.out.println("-5 % -3 = " + (-5 % -3)); // -2

        // Floating-point division
        System.out.println("7.0 / 3 = " + (7.0 / 3)); // 2.3333333333...
    }

    // 2) ++ / -- prefix vs postfix
    private static void incrementDecrementDemo() {
        System.out.println("\n== ++ / -- Demo ==");
        int i = 5;
        System.out.println("i = " + i);           // 5
        System.out.println("i++ yields " + (i++)); // yields 5, then i becomes 6
        System.out.println("after i++: " + i);     // 6
        System.out.println("++i yields " + (++i)); // increments to 7, yields 7
        System.out.println("after ++i: " + i);     // 7
    }

    // 3) Assignment and compound assignment
    private static void assignmentAndCompoundDemo() {
        System.out.println("\n== Assignment & Compound ==");
        int x = 10;
        x += 5;   // x = (int)(x + 5); implicit cast may apply
        System.out.println("x after x += 5: " + x); // 15

        short s = 1;
        // s = s + 1; // Compilation error: s+1 is int. Needs cast.
        s += 1; // OK: implicit cast to short after addition
        System.out.println("short s after s += 1: " + s); // 2

        int a, b, c;
        a = b = c = 42; // right-to-left
        System.out.println("chained assignment a=b=c=42 -> a=" + a + ", b=" + b + ", c=" + c);

        final int K;
        K = 100; // final can be assigned once
        // K = 200; // compile error if uncommented
    }

    // 4) Comparisons and equality (primitives vs references, NaN, wrappers pitfalls)
    private static void comparisonAndEqualityDemo() {
        System.out.println("\n== Comparison & Equality ==");
        System.out.println("3 < 5 -> " + (3 < 5));   // true
        System.out.println("'A' < 'a' -> " + ('A' < 'a')); // true (65 < 97)

        // Primitives: value equality
        int p = 128, q = 128;
        System.out.println("int 128 == 128 -> " + (p == q)); // true

        // References: identity equality
        String s1 = new String("hi");
        String s2 = new String("hi");
        System.out.println("new String(\"hi\") == new String(\"hi\") -> " + (s1 == s2)); // false
        System.out.println("s1.equals(s2) -> " + s1.equals(s2)); // true

        // Wrapper ‘==’ pitfalls (autoboxing & caching)
        Integer i1 = 127, i2 = 127;
        Integer i3 = 128, i4 = 128;
        System.out.println("Integer 127 == 127 -> " + (i1 == i2)); // true (cached)
        System.out.println("Integer 128 == 128 -> " + (i3 == i4)); // false (not cached)
        System.out.println("Integer 128 equals 128 -> " + i3.equals(i4)); // true

        // Unboxing can throw NPE if null
        Integer maybeNull = null;
        try {
            System.out.println("maybeNull == 0 -> " + (maybeNull == 0)); // NPE on unboxing
        } catch (NullPointerException e) {
            System.out.println("maybeNull == 0 threw NPE (unboxing null)");
        }
    }

    // 5) Logical (short-circuit) vs bitwise (non-short-circuit) on booleans
    private static void logicalVsBitwiseDemo() {
        System.out.println("\n== Logical vs Bitwise on booleans ==");
        int[] calls = {0};
        boolean leftFalse = false;
        boolean leftTrue = true;

        boolean rhsEvaluatedWithAndAnd = leftFalse && countAndReturnTrue(calls);
        System.out.println("false && RHS -> RHS evaluated? " + (calls[0] > 0)); // false, short-circuits
        calls[0] = 0;

        boolean rhsEvaluatedWithBitwiseAnd = leftFalse & countAndReturnTrue(calls);
        System.out.println("false & RHS -> RHS evaluated? " + (calls[0] > 0)); // true, no short-circuit
        calls[0] = 0;

        boolean rhsEvaluatedWithOrOr = leftTrue || countAndReturnTrue(calls);
        System.out.println("true || RHS -> RHS evaluated? " + (calls[0] > 0)); // false, short-circuits
        calls[0] = 0;

        boolean rhsEvaluatedWithBitwiseOr = leftTrue | countAndReturnTrue(calls);
        System.out.println("true | RHS -> RHS evaluated? " + (calls[0] > 0)); // true, no short-circuit

        // Bitwise on integers
        int m = 0b0101; // 5
        int n = 0b0011; // 3
        System.out.println("5 & 3 = " + (m & n)); // 1
        System.out.println("5 | 3 = " + (m | n)); // 7
        System.out.println("5 ^ 3 = " + (m ^ n)); // 6
        System.out.println("~5 = " + (~m));       // -6
    }

    private static boolean countAndReturnTrue(int[] calls) {
        calls[0]++;
        return true;
    }

    // 6) Shift operators
    private static void shiftDemo() {
        System.out.println("\n== Shifts ==");
        int x = -8; // 0xFFFFFFF8
        System.out.println("-8 >> 1 = " + (x >> 1));   // arithmetic right shift (-4)
        System.out.println("-8 >>> 1 = " + (x >>> 1)); // logical right shift (large positive)
        System.out.println("8 << 2 = " + (8 << 2));    // 32

        // Shift distance masking
        System.out.println("1 << 33 (int) = " + (1 << 33)); // same as 1 << (33 % 32) = 1 << 1 = 2
        System.out.println("1L << 65 (long) = " + (1L << 65)); // same as 1L << (65 % 64) = 1L << 1 = 2
    }

    // 7) String concatenation rules
    private static void stringConcatenationDemo() {
        System.out.println("\n== String concatenation ==");
        System.out.println("\"res: \" + 1 + 2 = " + ("res: " + 1 + 2));       // "res: 12"
        System.out.println("\"res: \" + (1 + 2) = " + ("res: " + (1 + 2)));   // "res: 3"
        String s = null;
        System.out.println("null + \"x\" = " + (s + "x"));                     // "nullx"
    }

    // 8) Casting and promotions
    private static void castingAndPromotionDemo() {
        System.out.println("\n== Casting & Promotions ==");
        byte b = 10;
        byte b2 = 20;
        // byte sum = b + b2; // compile error: b+b2 promoted to int
        byte sumCast = (byte) (b + b2);
        System.out.println("byte+(byte) -> int; cast back: (byte)(10+20) = " + sumCast);

        char c = 'A'; // 65
        int ci = c + 1; // char promoted to int
        System.out.println("'A' + 1 -> " + ci + " (char code); as char: " + (char) ci); // 66 -> 'B'

        // Narrowing float->int truncates toward zero
        System.out.println("(int) 3.9 = " + (int) 3.9);    // 3
        System.out.println("(int) -3.9 = " + (int) -3.9);  // -3

        // Reference cast (runtime-checked)
        Object obj = "hello";
        String str = (String) obj; // ok
        System.out.println("Cast Object->String ok: " + str);
        try {
            Integer bad = (Integer) obj; // ClassCastException
            System.out.println(bad);
        } catch (ClassCastException e) {
            System.out.println("Casting String to Integer throws ClassCastException");
        }
    }

    // 9) Floating-point corner cases (NaN, infinity, signed zero)
    private static void floatingPointCornerCases() {
        System.out.println("\n== Floating-point corner cases ==");
        double posInf = 1.0 / 0.0;
        double negInf = -1.0 / 0.0;
        double nan = 0.0 / 0.0;
        System.out.println("1.0/0.0 = " + posInf);
        System.out.println("-1.0/0.0 = " + negInf);
        System.out.println("0.0/0.0 = " + nan);
        System.out.println("NaN == NaN -> " + (nan == nan)); // false
        System.out.println("Double.isNaN(nan) -> " + Double.isNaN(nan)); // true

        double pz = 0.0, nz = -0.0;
        System.out.println("+0.0 == -0.0 -> " + (pz == nz)); // true
        System.out.println("1/+0.0 = " + (1.0 / pz) + ", 1/-0.0 = " + (1.0 / nz)); // +Inf vs -Inf
    }

    // 10) Evaluation order demo: left-to-right for operands
    private static void evaluationOrderDemo() {
        System.out.println("\n== Evaluation order (left-to-right) ==");
        int r = f("f") + g("g") * h("h"); // Calls occur left-to-right: f, g, h
        System.out.println("Result of f + g*h (ignoring arithmetic value): " + r);
        // Short-circuit demonstration
        boolean v = false && sideEffect("RHS for &&");
        boolean w = true || sideEffect("RHS for ||");
    }

    private static int f(String name) { System.out.println("call " + name); return 1; }
    private static int g(String name) { System.out.println("call " + name); return 2; }
    private static int h(String name) { System.out.println("call " + name); return 3; }
    private static boolean sideEffect(String label) { System.out.println(label + " evaluated"); return true; }

    // 11) Ternary operator and type inference
    private static void ternaryDemo() {
        System.out.println("\n== Ternary (?:) ==");
        int age = 20;
        String label = (age >= 18) ? "adult" : "minor"; // only one branch evaluated
        System.out.println("age>=18 ? 'adult':'minor' -> " + label);

        // Type selection example: int vs double -> promoted to double
        Number num = true ? 10 : 10.5; // result is double 10.5; autoboxed to Double
        System.out.println("true ? 10 : 10.5 -> type double, value: " + num + " (" + num.getClass().getSimpleName() + ")");
    }

    // 12) Overflow detection with exact methods
    private static void overflowDetectionDemo() {
        System.out.println("\n== Overflow detection ==");
        int big = 2_000_000_000;
        int product = big * 2; // overflow, wraps negative
        System.out.println("2_000_000_000 * 2 (wrapped) = " + product);
        try {
            int exact = Math.multiplyExact(big, 2); // throws ArithmeticException
            System.out.println(exact);
        } catch (ArithmeticException e) {
            System.out.println("Math.multiplyExact detected overflow");
        }
    }

    // 13) instanceof and casts (with note on pattern matching)
    private static void instanceofAndCastsDemo() {
        System.out.println("\n== instanceof & casts ==");
        Object o = "text";
        System.out.println("o instanceof String -> " + (o instanceof String)); // true

        // Traditional pattern:
        if (o instanceof String) {
            String s = (String) o; // manual cast
            System.out.println("Length via cast: " + s.length());
        }

        // Pattern matching for instanceof (Java 16+):
        // if (o instanceof String s) {
        //     System.out.println("Length via pattern var: " + s.length());
        // }

        Object n = null;
        System.out.println("null instanceof Object -> " + (n instanceof Object)); // false
    }

    // 14) Precedence pitfalls: reminder to use parentheses
    private static void precedencePitfallDemo() {
        System.out.println("\n== Precedence pitfalls ==");
        int x = 1, y = 2, z = 3;
        int r1 = x + y << 1;   // same as ((x + y) << 1) because + has higher precedence than <<
        int r2 = x + (y << 1); // explicit grouping
        System.out.println("x + y << 1 = " + r1 + " ; x + (y << 1) = " + r2);

        int i = 1;
        int tricky = i++ + ++i; // Evaluate left-to-right: (i++ yields 1; i becomes 2) + (++i makes 3; yields 3) => 4
        System.out.println("i++ + ++i (starting i=1) -> " + tricky);
    }
}