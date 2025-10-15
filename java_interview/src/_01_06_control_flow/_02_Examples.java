package _01_06_control_flow;

/**
 * Control Flow Examples
 * - if/else, else-if, nested if, guard clauses
 * - ternary operator
 * - switch (int, String, enum), fall-through, default
 * - loops: for, enhanced for, while, do-while, nested, infinite with break
 * - break, continue, labeled break/continue
 * - short-circuit (&&, ||) vs non–short-circuit (&, |) on booleans
 * - try/catch/finally, early return
 * - assert (optional, enabled with -ea)
 */
public class _02_Examples {

    public static void main(String[] args) {
        System.out.println("--- if / else ---");
        ifExamples();

        System.out.println("\n--- ternary ---");
        ternaryExamples();

        System.out.println("\n--- switch ---");
        switchExamples();

        System.out.println("\n--- for loops ---");
        forLoopExamples();

        System.out.println("\n--- while / do-while ---");
        whileDoWhileExamples();

        System.out.println("\n--- break / continue (labels) ---");
        breakContinueExamples();

        System.out.println("\n--- short-circuit vs non–short-circuit ---");
        shortCircuitExamples();

        System.out.println("\n--- try / catch / finally ---");
        tryCatchFinallyExamples();

        System.out.println("\n--- return / guard clauses ---");
        returnExamples();

        System.out.println("\n--- assert (optional) ---");
        assertExamples();
    }

    static void ifExamples() {
        int temperature = 28;

        // Single-statement if
        if (temperature > 30) System.out.println("It's hot.");

        // if/else with block (use braces even for one-line blocks for safety)
        if (temperature >= 20 && temperature <= 30) {
            System.out.println("Nice weather.");
            System.out.println("Go for a walk.");
        } else {
            System.out.println("Not ideal weather.");
        }

        // else-if chain
        int score = 72;
        if (score >= 90) System.out.println("Grade: A");
        else if (score >= 80) System.out.println("Grade: B");
        else if (score >= 70) System.out.println("Grade: C");
        else if (score >= 60) System.out.println("Grade: D");
        else System.out.println("Grade: F");

        // Nested if and null-check (guard against NullPointerException)
        String user = "alex";
        if (user != null) {
            if (user.length() > 3) {
                System.out.println("User name length > 3");
            } else {
                System.out.println("User name length <= 3");
            }
        }

        // Guard clause to reduce nesting
        String token = null;
        if (token == null) {
            System.out.println("Token missing (guard).");
        } else {
            System.out.println(token.toUpperCase());
        }
    }

    static void ternaryExamples() {
        int x = -5;
        int abs = (x >= 0) ? x : -x;
        System.out.println("abs(" + x + ") = " + abs);

        int a = 7, b = 10;
        int max = (a > b) ? a : b;
        System.out.println("max(" + a + "," + b + ") = " + max);

        // Ternary returns a value; usable inline
        String parity = (a % 2 == 0) ? "even" : "odd";
        System.out.println(a + " is " + parity);

        // Nested ternary (prefer if/else if it harms readability)
        int n = 0;
        String sign = (n > 0) ? "positive" : (n < 0) ? "negative" : "zero";
        System.out.println("n is " + sign);
    }

    enum Weekday { MON, TUE, WED, THU, FRI, SAT, SUN }

    static void switchExamples() {
        // Switch on int
        int dayNumber = 3;
        switch (dayNumber) {
            case 1:
                System.out.println("Mon");
                break;
            case 2:
                System.out.println("Tue");
                break;
            case 3:
                System.out.println("Wed");
                break;
            case 4:
                System.out.println("Thu");
                break;
            case 5:
                System.out.println("Fri");
                break;
            case 6:
            case 7: // fall-through: 6 continues into 7
                System.out.println("Weekend");
                break;
            default:
                System.out.println("Invalid day number");
        }

        // Switch on String
        String command = "start";
        switch (command) {
            case "start":
                System.out.println("Starting...");
                break;
            case "stop":
                System.out.println("Stopping...");
                break;
            case "pause":
            case "hold":
                System.out.println("Pausing...");
                break;
            default:
                System.out.println("Unknown command");
        }

        // Switch on enum
        Weekday today = Weekday.SAT;
        switch (today) {
            case MON:
            case TUE:
            case WED:
            case THU:
            case FRI:
                System.out.println("Weekday: work");
                break;
            case SAT:
            case SUN:
                System.out.println("Weekend: rest");
                break;
        }

        // Demonstrate fall-through by omitting break
        int level = 2;
        System.out.print("Level " + level + ": ");
        switch (level) {
            case 1:
                System.out.print("Bronze ");
            case 2:
                System.out.print("Silver ");
            case 3:
                System.out.print("Gold ");
                break;
            default:
                System.out.print("Unknown");
        }
        System.out.println();

        // Group cases
        char ch = 'e';
        switch (Character.toLowerCase(ch)) {
            case 'a': case 'e': case 'i': case 'o': case 'u':
                System.out.println(ch + " is a vowel");
                break;
            default:
                System.out.println(ch + " is a consonant");
        }

        // Note: newer Java has switch expressions with '->' and 'yield'
    }

    static void forLoopExamples() {
        // Classic for loop
        int sum = 0;
        for (int i = 1; i <= 5; i++) {
            sum += i;
        }
        System.out.println("Sum 1..5 = " + sum);

        // Multiple init/update
        StringBuilder sb = new StringBuilder();
        for (int i = 0, j = 10; i <= 3; i++, j -= 2) {
            sb.append("(").append(i).append(",").append(j).append(") ");
        }
        System.out.println("Pairs: " + sb);

        // Enhanced for (for-each) over array
        int[] nums = {2, 4, 6};
        int product = 1;
        for (int n : nums) {
            product *= n;
        }
        System.out.println("Product = " + product);

        // Nested loops
        System.out.println("Multiplication table 1..3:");
        for (int i = 1; i <= 3; i++) {
            for (int j = 1; j <= 3; j++) {
                System.out.print((i * j) + " ");
            }
            System.out.println();
        }

        // Infinite loop with break
        int counter = 0;
        for (;;) { // infinite
            counter++;
            if (counter == 3) {
                System.out.println("Breaking infinite loop at " + counter);
                break;
            }
        }
    }

    static void whileDoWhileExamples() {
        // While loop (0..4)
        int i = 0;
        while (i < 5) {
            System.out.print(i + " ");
            i++;
        }
        System.out.println();

        // Do-while executes at least once
        int attempts = 0;
        boolean success = false;
        do {
            attempts++;
            System.out.println("Attempt " + attempts);
            if (attempts >= 2) {
                success = true; // simulate success on 2nd attempt
            }
        } while (!success);
        System.out.println("Success after " + attempts + " attempts");
    }

    static void breakContinueExamples() {
        // break exits the loop
        for (int i = 1; i <= 5; i++) {
            if (i == 4) {
                System.out.println("Break at i=" + i);
                break;
            }
            System.out.print(i + " ");
        }
        System.out.println();

        // continue skips to next iteration
        for (int i = 1; i <= 5; i++) {
            if (i == 3) {
                System.out.print("(skip " + i + ") ");
                continue;
            }
            System.out.print(i + " ");
        }
        System.out.println();

        // Labeled break: exit both inner and outer loops
        int[][] grid = {
            {1, 2, 3},
            {4, 5, 6},
            {7, 8, 9}
        };
        int target = 5;
        boolean found = false;
        outerLoop:
        for (int r = 0; r < grid.length; r++) {
            for (int c = 0; c < grid[r].length; c++) {
                if (grid[r][c] == target) {
                    found = true;
                    System.out.println("Found " + target + " at (" + r + "," + c + ")");
                    break outerLoop; // exit both loops
                }
            }
        }
        System.out.println("Found? " + found);

        // Labeled continue: skip to next outer iteration
        System.out.println("Labeled continue demo:");
        rowLoop:
        for (int r = 0; r < grid.length; r++) {
            for (int c = 0; c < grid[r].length; c++) {
                if (grid[r][c] % 2 == 0) { // even number
                    System.out.print("[even->next row] ");
                    continue rowLoop; // skip remaining columns in this row
                }
                System.out.print(grid[r][c] + " ");
            }
            System.out.print("| ");
        }
        System.out.println();
    }

    static void shortCircuitExamples() {
        // Short-circuit with &&
        if (left("A", true) && right("B")) {
            System.out.println("Result: true");
        } else {
            System.out.println("Result: false");
        }

        // Short-circuit with ||
        if (left("C", true) || right("D")) {
            System.out.println("OR short-circuit occurred");
        }

        // Non–short-circuit with & (evaluates both sides)
        if (left("E", false) & right("F")) {
            System.out.println("This won't print");
        } else {
            System.out.println("Evaluated both sides with &");
        }

        // Non–short-circuit with | (evaluates both sides)
        if (left("G", true) | right("H")) {
            System.out.println("Evaluated both sides with |");
        }
    }

    // Helper methods to show evaluation order/side effects
    private static boolean left(String label, boolean value) {
        System.out.println("Evaluate left " + label + " -> " + value);
        return value;
    }
    private static boolean right(String label) {
        System.out.println("Evaluate right " + label + " -> true");
        return true;
    }

    static void tryCatchFinallyExamples() {
        // try-catch: handle an exception and continue program flow
        try {
            int x = 10 / 0; // throws ArithmeticException
            System.out.println(x); // not executed
        } catch (ArithmeticException ex) {
            System.out.println("Caught: " + ex.getClass().getSimpleName());
        } finally {
            System.out.println("Finally always runs");
        }

        // Finally runs even when returning inside try
        int value = computeWithFinally();
        System.out.println("computeWithFinally returned " + value);
    }

    private static int computeWithFinally() {
        try {
            System.out.println("In try: preparing to return 42");
            return 42;
        } catch (RuntimeException ex) {
            return -1;
        } finally {
            // Avoid returning from finally; here we just log
            System.out.println("In finally: cleaning up after try");
        }
    }

    static void returnExamples() {
        // Guard clause avoids deep nesting
        System.out.println("isAdult(20) -> " + isAdult(20));
        System.out.println("isAdult(15) -> " + isAdult(15));

        // Early return from search
        int idx = indexOf(new int[]{3, 9, 7, 9}, 7);
        System.out.println("Index of 7 -> " + idx);

        // Returning from void method
        printPositive(5);
        printPositive(-3);
    }

    private static boolean isAdult(int age) {
        if (age < 0) throw new IllegalArgumentException("age < 0"); // control flow via exception
        if (age < 18) return false; // guard/early return
        return true;
    }

    private static int indexOf(int[] arr, int target) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == target) return i; // early return when found
        }
        return -1; // not found
    }

    private static void printPositive(int n) {
        if (n <= 0) {
            System.out.println("Not positive");
            return; // exit early
        }
        System.out.println("Positive: " + n);
    }

    static void assertExamples() {
        int size = 3;
        // Enable assertions with: java -ea _01_06_control_flow._02_Examples
        assert size > 0 : "size must be > 0";
        System.out.println("Assertion checked (if enabled)");
    }
}