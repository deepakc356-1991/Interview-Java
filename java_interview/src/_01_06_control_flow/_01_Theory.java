package _01_06_control_flow;

/*
    Control Flow in Java — Theory + Practical Examples

    Covered:
    - Statements, blocks, and scope
    - if, else-if, else
    - Ternary (conditional) operator
    - Short-circuit logical operators (&&, ||) vs bitwise (&, |) on booleans
    - switch statement (int, String, enum), fall-through
    - Loops: for, enhanced for-each, while, do-while, infinite loops
    - break, continue, and labels
    - return and guard clauses
    - Exception-driven control flow: throw, try-catch-finally, multi-catch
    - try-with-resources and suppressed exceptions
    - Assertions (assert)
    - instanceof with traditional cast (and note on pattern matching)
    - Notes on newer features (switch expressions, pattern matching for switch) in comments
*/
public class _01_Theory {

    public static void main(String[] args) {
        System.out.println("=== Java Control Flow: Theory and Examples ===");
        blocksAndScope();
        ifElseExamples();
        ternaryOperator();
        shortCircuitOperators();
        classicSwitch();
        loops();
        labeledBreakAndContinue();
        earlyReturnAndGuardClauses();
        tryCatchFinally();
        tryWithResources();
        assertions();
        traditionalInstanceof();

        // Note: Newer features (Java 14+/17+/21) are shown in comments inside methods:
        // - switch expressions (Java 14+)
        // - instanceof pattern variables (Java 16+)
        // - pattern matching for switch (Java 21)
    }

    // ---------------------------
    // 1) Statements, Blocks, Scope
    // ---------------------------
    static void blocksAndScope() {
        System.out.println("\n-- Blocks and Scope --");

        int x = 10; // 'x' is in scope from here to the end of this block (method)
        System.out.println("x (outer) = " + x);

        {
            int y = 20; // 'y' exists only inside this inner block
            System.out.println("Inside inner block: y = " + y + ", can also access x = " + x);

            // Note: You cannot re-declare 'x' here; local variables cannot be shadowed in the same method scope.
            // int x = 99; // Compile error: variable x is already defined
        }
        // System.out.println(y); // Compile error: y cannot be resolved

        // For-loop variables are scoped to the loop
        int sum = 0;
        for (int i = 0; i < 3; i++) {
            sum += i;
        }
        System.out.println("Sum after for-loop = " + sum);
        // System.out.println(i); // Compile error: i not in scope here
    }

    // ---------------------------
    // 2) if / else-if / else
    // ---------------------------
    static void ifElseExamples() {
        System.out.println("\n-- if / else-if / else --");

        int temperatureC = 18;
        if (temperatureC < 0) {
            System.out.println("Freezing");
        } else if (temperatureC < 20) {
            System.out.println("Chilly");
        } else {
            System.out.println("Warm");
        }

        // Single-statement if (braces optional but recommended to prevent bugs)
        int n = 5;
        if (n > 0)
            System.out.println("n is positive");

        // "Dangling else" pairs with the closest unmatched if
        if (n > 0)
            if (n % 2 == 0)
                System.out.println("positive even");
            else
                System.out.println("positive odd"); // attaches to inner if
        // Better with braces to avoid ambiguity.
    }

    // ---------------------------
    // 3) Ternary (conditional) operator
    // ---------------------------
    static void ternaryOperator() {
        System.out.println("\n-- Ternary (?:) --");

        int a = 10, b = 20;
        int min = (a < b) ? a : b;
        System.out.println("min = " + min);

        int n = 7;
        String parity = (n % 2 == 0) ? "even" : "odd";
        System.out.println("n is " + parity);

        // Ternary is an expression (has a value); both branches must be compatible types.
    }

    // --------------------------------------------
    // 4) Short-circuit logical operators vs bitwise
    // --------------------------------------------
    static void shortCircuitOperators() {
        System.out.println("\n-- Short-circuit operators (&&, ||) vs (&, |) on booleans --");

        boolean r1 = check("A(false)", false) && check("B(true)", true); // Second operand skipped
        System.out.println("Result r1 (&&) = " + r1);

        boolean r2 = check("C(false)", false) & check("D(true)", true);  // Both evaluated
        System.out.println("Result r2 (&) = " + r2);

        boolean r3 = check("E(true)", true) || check("F(false)", false); // Second operand skipped
        System.out.println("Result r3 (||) = " + r3);

        boolean r4 = check("G(false)", false) | check("H(true)", true);  // Both evaluated
        System.out.println("Result r4 (|) = " + r4);

        // '!' negates a boolean; there is no short-circuit with '!'
        boolean r5 = !check("I(true)", true);
        System.out.println("Result r5 (!) = " + r5);
    }

    static boolean check(String label, boolean value) {
        System.out.println("Evaluating " + label);
        return value;
    }

    // --------------------------------------------
    // 5) switch (classic statement), fall-through
    // --------------------------------------------
    enum Color { RED, GREEN, BLUE }

    static void classicSwitch() {
        System.out.println("\n-- switch (classic) --");

        int day = 2; // 1=Mon, 2=Tue, ...
        switch (day) {
            case 1:
                System.out.println("Monday");
                break; // prevents fall-through
            case 2:
                System.out.println("Tuesday");
                break;
            default:
                System.out.println("Other day");
                break;
        }

        // switch on String
        String traffic = "green";
        switch (traffic) {
            case "red":
                System.out.println("STOP");
                break;
            case "yellow":
                System.out.println("READY");
                break;
            case "green":
                System.out.println("GO");
                break;
            default:
                System.out.println("UNKNOWN");
        }

        // switch on enum
        Color color = Color.BLUE;
        switch (color) {
            case RED:
                System.out.println("Color is RED");
                break;
            case GREEN:
                System.out.println("Color is GREEN");
                break;
            case BLUE:
                System.out.println("Color is BLUE");
                break;
        }

        // Fall-through example (intentional)
        String msg = "Case: ";
        int n = 1;
        switch (n) {
            case 1:
                msg += "1";
                // No break: execution falls through into case 2
            case 2:
                msg += "2";
                break;
            default:
                msg += "default";
        }
        System.out.println(msg); // "Case: 12"

        // Notes:
        // - Duplicate case labels cause compile errors.
        // - switch on null for String/enum causes NullPointerException.
        // - Scope: colon-style cases share a scope; be careful with variable declarations across cases.
        // - For independent scopes per case, use braces within each case.
        // - Newer: switch expressions (Java 14+) avoid fall-through and can return a value. See below.

        /*
        // Java 14+ switch expression (commented out for older compilers)
        int points = switch (color) {
            case RED -> 1;
            case GREEN, BLUE -> 2;
        };
        System.out.println("points = " + points);

        // Block with 'yield' for complex logic
        String meaning = switch (day) {
            case 1 -> "Mon";
            case 2 -> {
                String label = "Tue";
                yield label.toUpperCase();
            }
            default -> "Other";
        };
        System.out.println("meaning = " + meaning);
        */
    }

    // ---------------------------
    // 6) Loops: for, foreach, while, do-while
    // ---------------------------
    static void loops() {
        System.out.println("\n-- Loops --");

        // Classic for
        int total = 0;
        for (int i = 1; i <= 3; i++) {
            total += i;
        }
        System.out.println("total (1..3) = " + total);

        // Enhanced for-each (readable iteration over arrays/collections)
        String[] names = { "Ada", "Bob", "Cyd" };
        for (String name : names) {
            System.out.println("Hello, " + name);
        }

        // while loop
        int count = 3;
        while (count > 0) {
            System.out.println("count = " + count);
            count--;
        }

        // do-while (executes body at least once)
        int x = 0;
        do {
            System.out.println("do-while x = " + x);
            x++;
        } while (x < 2);

        // Infinite loop with break
        int ticks = 0;
        for (;;) {
            ticks++;
            if (ticks == 2) {
                System.out.println("Breaking infinite loop at ticks=2");
                break;
            }
        }

        // continue to skip current iteration
        for (int i = 0; i < 5; i++) {
            if (i % 2 == 0) continue; // skip even numbers
            System.out.println("Odd i = " + i);
        }
    }

    // ---------------------------
    // 7) Labels with break/continue
    // ---------------------------
    static void labeledBreakAndContinue() {
        System.out.println("\n-- Labeled break/continue --");

        // break with label: exit outer loop from inner loop
        outer:
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                System.out.println("visiting [" + r + "," + c + "]");
                if (c == 2) {
                    System.out.println("Breaking out of outer loop at c==2");
                    break outer;
                }
            }
        }

        // continue with label: skip remaining inner iterations, continue outer loop
        rowLoop:
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                if (c == 1) {
                    System.out.println("continue row " + r + " at c==1");
                    continue rowLoop; // skip to next row
                }
                System.out.println("processing [" + r + "," + c + "]");
            }
        }

        // Note: There is no 'goto' in Java; labels are only for break/continue.
    }

    // ---------------------------
    // 8) return and guard clauses
    // ---------------------------
    static void earlyReturnAndGuardClauses() {
        System.out.println("\n-- Early returns (guard clauses) --");

        System.out.println("safeDivide(10, 2) = " + safeDivide(10, 2));
        System.out.println("safeDivide(10, 0) = " + safeDivide(10, 0)); // early return avoids divide-by-zero
    }

    static int safeDivide(int numerator, int denominator) {
        if (denominator == 0) {
            // Guard clause: fail fast / exit early
            return 0; // or throw new IllegalArgumentException("denominator == 0");
        }
        return numerator / denominator;
    }

    // -----------------------------------------
    // 9) Exceptions: throw, try-catch-finally
    // -----------------------------------------
    static void tryCatchFinally() {
        System.out.println("\n-- try / catch / finally --");

        try {
            mightThrow(-1);
            System.out.println("This line will not execute when exception thrown above.");
        } catch (IllegalArgumentException e) {
            System.out.println("Caught IAE: " + e.getMessage());
        } catch (RuntimeException e) { // multi-catch is also possible: catch (IAE | NPE e) { ... }
            System.out.println("Caught RuntimeException: " + e.getMessage());
        } finally {
            System.out.println("finally always runs (even if exception or return in try)");
        }

        int value = returnsFromTryFinally();
        System.out.println("Value returned (with finally running) = " + value);

        // Catch ordering: subclasses before superclasses
        try {
            throw new NullPointerException("NPE demo");
        } catch (NullPointerException e) {
            System.out.println("Caught NPE first");
        } catch (Exception e) {
            System.out.println("This would catch general exceptions");
        }
    }

    static void mightThrow(int x) {
        if (x < 0) throw new IllegalArgumentException("x must be >= 0");
    }

    static int returnsFromTryFinally() {
        try {
            System.out.println("In try: returning 42");
            return 42;
        } finally {
            System.out.println("In finally: runs even though try returned");
        }
    }

    // -----------------------------------------
    // 10) try-with-resources and suppressed exceptions
    // -----------------------------------------
    static void tryWithResources() {
        System.out.println("\n-- try-with-resources --");
        // Resources are closed automatically in reverse order of creation.
        // Exceptions thrown in close() become SUPPRESSED if another exception is already being thrown.

        try (DemoResource r1 = new DemoResource("r1");
             DemoResource r2 = new DemoResource("r2")) {
            System.out.println("Using resources r1 and r2");
            // Simulate exception in work
            throw new RuntimeException("boom in try body");
        } catch (Exception e) {
            System.out.println("Caught: " + e);
            Throwable[] suppressed = e.getSuppressed();
            System.out.println("Suppressed exceptions during close(): " + suppressed.length);
            for (Throwable s : suppressed) {
                System.out.println("  suppressed -> " + s);
            }
        }
    }

    static class DemoResource implements AutoCloseable {
        private final String name;

        DemoResource(String name) {
            this.name = name;
            System.out.println("Open " + name);
        }

        @Override
        public void close() {
            System.out.println("Close " + name);
            // Throw to demonstrate suppressed exceptions
            throw new RuntimeException("close failure: " + name);
        }
    }

    // ---------------------------
    // 11) Assertions
    // ---------------------------
    static void assertions() {
        System.out.println("\n-- Assertions (assert) --");
        // Assertions are disabled by default; enable with: java -ea ClassName
        // Use for internal invariants, not for argument validation in public APIs.

        int x = 2 + 2;
        assert x == 4;

        // Assertion with message (only evaluated when assertions enabled)
        int y = 5;
        assert y > 0 : "y must be positive";

        System.out.println("Assertions passed (or not evaluated if disabled)");

        // Example of failing assertion (commented out to avoid stopping program when -ea is used)
        // assert false : "This will fail when assertions are enabled";
    }

    // -----------------------------------------
    // 12) instanceof (traditional), casting
    // -----------------------------------------
    static void traditionalInstanceof() {
        System.out.println("\n-- instanceof and casting --");
        Object obj = "hello";

        if (obj instanceof String) {
            String s = (String) obj; // traditional cast after instanceof check
            System.out.println("Length = " + s.length());
        } else {
            System.out.println("Not a String");
        }

        // Newer (Java 16+): pattern matching for instanceof
        // if (obj instanceof String s) {
        //     System.out.println("Length = " + s.length()); // 's' in scope inside 'if' block
        // }
    }

    /*
        Notes on newer control-flow features (commented for compatibility):

        1) switch expressions (Java 14+)
           - An expression that yields a value; no fall-through with '->'.
           - Requires covering all cases or providing 'default'.

           int score = switch (grade) {
               case 'A' -> 5;
               case 'B' -> 4;
               case 'C' -> 3;
               case 'D' -> 2;
               case 'F' -> 0;
               default -> throw new IllegalArgumentException("Unknown grade: " + grade);
           };

           // Use 'yield' for blocks:
           String label = switch (n) {
               case 1 -> "one";
               case 2 -> {
                   String temp = "two";
                   yield temp.toUpperCase();
               }
               default -> "other";
           };

        2) Pattern matching for switch (Java 21)
           - Switch over types with patterns and 'when' guards.
           - Example (requires Java 21):
             static String format(Object o) {
                 return switch (o) {
                     case Integer i -> "int " + i;
                     case String s when s.length() > 5 -> "long string " + s;
                     case String s -> "string " + s;
                     case null -> "null"; // explicit null label allowed
                     default -> "unknown";
                 };
             }
    */
}