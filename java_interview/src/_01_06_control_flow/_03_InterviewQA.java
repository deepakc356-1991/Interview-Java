package _01_06_control_flow;

/*
 Control Flow Interview Q&A (Basic → Intermediate → Advanced)
 - This single file demonstrates questions, answers, and runnable mini-experiments.
 - It is Java 8+ compatible. Newer features (switch expressions, pattern matching) are documented in comments.
 - Run main() to see outputs and behavior notes.

 Coverage highlights:
 - if/else vs ternary, short-circuiting, operator precedence, loops, break/continue (with labels), switch/fallthrough/null
 - try/catch/finally control flow, finally overriding return, try-with-resources closing order and suppressed exceptions
 - pitfalls: assignment in conditions, semicolon traps, dangling else, ternary unboxing NPE, break in switch inside loop
 - notes on unreachable code rules, labels, and modern switch/pattern matching (commented for older JDK compatibility)
*/

import java.util.Arrays;
import java.util.List;

import static _01_02_program_structure._03_InterviewQA.basic;
import static _01_02_program_structure._03_InterviewQA.intermediate;

public class _03_InterviewQA {

    public static void main(String[] args) {
        System.out.println("=== Control Flow Interview Q&A Demo ===");
        basic();
        intermediate();
        advancedNotes();
        System.out.println("=== End ===");
    }

    // --------------------------- BASIC --------------------------------

    /*
     Q: if-else vs ternary (?:). Any pitfalls?
     A:
     - Both choose between branches. Ternary is an expression (returns a value), if/else is a statement.
     - Pitfall: Mixed primitive/reference in ternary can cause auto-unboxing NPE.
     - Prefer readability; use ternary for simple value selection.
    */
    private static void q_ifElseVsTernary() {
        title("if-else vs ternary");
        int a = 3, b = 5;
        int min = (a < b) ? a : b;
        System.out.println("min via ternary = " + min);

        Integer maybeNull = null;
        try {
            int x = false ? 1 : maybeNull; // Mixed int and Integer -> unboxing -> NPE at runtime (false branch chosen)
            System.out.println("won't print: " + x);
        } catch (NullPointerException e) {
            System.out.println("Caught NPE from ternary auto-unboxing of null");
        }
    }

    /*
     Q: What happens if you assign in an if condition (e.g., if (flag = true))?
     A:
     - It's legal for booleans and assigns the value, then evaluates it.
     - Common bug: you likely meant '==' (comparison).
     - Prefer 'if (flag)' or use explicit comparisons to avoid mistakes.
    */
    private static void q_assignmentInIf() {
        title("assignment in if condition");
        boolean flag = false;
        if (flag = true) { // assignment, not comparison
            System.out.println("Assignment in condition set flag to true");
        }
        System.out.println("flag is now: " + flag);
    }

    /*
     Q: Difference between &&/|| (short-circuit) and &/| (non-short-circuit) on booleans?
     A:
     - && and || skip evaluating the right side if the result is already determined.
     - & and | always evaluate both sides (when used with booleans).
     - Prefer short-circuit for guards to avoid NPE/expensive calls.
    */
    private static void q_shortCircuitOperators() {
        title("short-circuit vs non-short-circuit");
        sideEffects = 0;

        boolean left = false;
        if (left && call("&& right")) { /* not called */ }
        System.out.println("sideEffects after &&: " + sideEffects);

        if (left & call("& right")) { /* called */ }
        System.out.println("sideEffects after &: " + sideEffects);

        left = true;
        if (left || call("|| right")) { /* right not called */ }
        System.out.println("sideEffects after ||: " + sideEffects);

        if (left | call("| right")) { /* right called */ }
        System.out.println("sideEffects after |: " + sideEffects);
    }

    /*
     Q: Operator precedence (&& vs ||)?
     A:
     - && has higher precedence than ||.
     - Example: a || b && c is a || (b && c).
    */
    private static void q_operatorPrecedence() {
        title("operator precedence");
        boolean a = false, b = true, c = false;
        boolean result = a || b && c; // false || (true && false) -> false
        System.out.println("a || b && c = " + result);
        boolean result2 = (a || b) && c; // (false || true) && false -> false
        System.out.println("(a || b) && c = " + result2);
    }

    /*
     Q: while vs do-while vs for?
     A:
     - while: pre-check loop.
     - do-while: post-check, runs body at least once.
     - for: convenient header for init, condition, update.
    */
    private static void q_loopsWhileDoFor() {
        title("while vs do-while vs for");
        int i = 0;
        while (i > 0) { // won't run
            i++;
        }
        System.out.println("while didn't run when i=0");
        int j = 0;
        do {
            j++;
        } while (j < 0); // runs once despite false condition
        System.out.println("do-while ran once; j=" + j);

        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < 3; k++) {
            sb.append(k);
        }
        System.out.println("for loop appended: " + sb);
    }

    /*
     Q: Semicolon traps in loops?
     A:
     - A stray semicolon creates an empty loop/body.
     - Example: for (...); { block } -> loop does nothing; block runs once.
    */
    private static void q_semicolonTrap() {
        title("semicolon traps");
        int counter = 0;
        for (int k = 0; k < 3; k++) ; // empty loop
        { counter++; } // runs once
        System.out.println("Block ran once due to stray semicolon; counter=" + counter);

        // Another: while(condition); -> empty loop; risky if condition true.
    }

    /*
     Q: break and continue (with and without labels)?
     A:
     - break exits the nearest loop/switch.
     - continue skips to next iteration of the nearest loop.
     - Labeled break/continue target a specific outer loop.
    */
    private static void q_breakContinue() {
        title("break/continue and labels");
        int sum = 0;
        outer:
        for (int i = 0; i < 3; i++) {
            for (int k = 0; k < 3; k++) {
                if (i == 1 && k == 1) {
                    System.out.print("(continue outer) ");
                    continue outer; // jump to next i
                }
                if (i == 2 && k == 0) {
                    System.out.print("(break outer) ");
                    break outer; // exit both loops
                }
                sum += i + k;
            }
        }
        System.out.println("sum=" + sum);
    }

    /*
     Q: switch basics (types, default, order)?
     A:
     - Supports byte/short/char/int (and their wrappers), enum, String (since Java 7).
     - default may appear anywhere; cases must be compile-time constants.
     - long is not allowed in classic switch.
    */
    private static void q_switchBasics() {
        title("switch basics");
        int code = 2;
        switch (code) {
            case 1:
                System.out.println("code=1");
                break;
            case 2:
                System.out.println("code=2");
                break;
            default:
                System.out.println("default");
        }
    }

    /*
     Q: switch fall-through?
     A:
     - Without break/return/throw, control falls through to the next case.
     - Useful intentionally; dangerous accidentally.
    */
    private static void q_switchFallthrough() {
        title("switch fall-through");
        int n = 1;
        switch (n) {
            case 1:
                System.out.print("one ");
                // fall-through
            case 2:
                System.out.print("two ");
                break;
            default:
                System.out.print("default");
        }
        System.out.println();
    }

    /*
     Q: break in switch within a loop exits switch or loop?
     A:
     - break exits only the nearest switch.
     - Use labeled break to exit the loop as well.
    */
    private static void q_breakInSwitchInsideLoop() {
        title("break in switch inside loop");
        int i = 0;
        loop:
        while (i < 3) {
            switch (i) {
                case 0:
                    System.out.print("[i=0 switch break] ");
                    break; // exits switch only
                case 1:
                    System.out.print("[i=1 labeled break to loop] ");
                    break loop; // exits while
                default:
                    System.out.print("[default] ");
            }
            i++;
        }
        System.out.println("| i ended at " + i);
    }

    /*
     Q: Dangling else problem?
     A:
     - else binds to the nearest unmatched if.
     - Use braces to make intent explicit.
    */
    private static void q_danglingElse() {
        title("dangling else");
        boolean outer = false, inner = true;
        if (outer)
            if (inner)
                System.out.print("inner ");
            else
                System.out.print("ELSE binds to inner ");
        System.out.println("| With braces, you control binding explicitly.");
    }

    // ------------------------ INTERMEDIATE -----------------------------

    /*
     Q: switch on String and enum?
     A:
     - Allowed. String cases are matched by equals (case-sensitive).
    */
    private static void q_switchStringEnum() {
        title("switch on String and enum");
        String command = "start";
        switch (command) {
            case "start":
                System.out.print("String matched start ");
                break;
            case "stop":
                System.out.print("stop ");
                break;
            default:
                System.out.print("unknown ");
        }

        Day d = Day.FRI;
        switch (d) {
            case MON:
            case TUE:
            case WED:
            case THU:
            case FRI:
                System.out.print("| Weekday ");
                break;
            case SAT:
            case SUN:
                System.out.print("| Weekend ");
                break;
        }
        System.out.println();
    }

    /*
     Q: What happens if you switch on a null String?
     A:
     - Classic switch throws NullPointerException if the selector is null.
     - Handle null before switch.
    */
    private static void q_switchNull() {
        title("switch on null");
        String s = null;
        try {
            switch (s) {
                case "x":
                    System.out.println("x");
                    break;
                default:
                    System.out.println("default");
            }
        } catch (NullPointerException e) {
            System.out.println("NullPointerException: switch selector was null");
        }
    }

    /*
     Q: Multiple initializations and updates in for?
     A:
     - Allowed via commas in init and update sections (single condition allowed).
    */
    private static void q_multipleInitUpdateInFor() {
        title("multiple init/update in for");
        StringBuilder sb = new StringBuilder();
        for (int i = 0, j = 3; i < j; i++, j--) {
            sb.append("(").append(i).append(",").append(j).append(")");
        }
        System.out.println(sb.toString());
    }

    /*
     Q: for-each with labels/break/continue?
     A:
     - Works like normal loops; you can use labeled break/continue with for-each.
    */
    private static void q_forEachWithLabels() {
        title("for-each with labels");
        List<String> items = Arrays.asList("a", "skip", "b", "stop", "c");
        OUTER:
        for (String it : items) {
            if ("skip".equals(it)) {
                System.out.print("(continue OUTER) ");
                continue OUTER;
            }
            if ("stop".equals(it)) {
                System.out.print("(break OUTER) ");
                break OUTER;
            }
            System.out.print(it + " ");
        }
        System.out.println();
    }

    /*
     Q: try/catch/finally order and flow?
     A:
     - finally always runs (except for System.exit, fatal errors).
     - A return/throw in try or catch still triggers finally.
    */
    private static void q_tryCatchFinallyFlow() {
        title("try/catch/finally flow");
        System.out.println("result=" + returnsWithFinallyMessage());
    }

    private static int returnsWithFinallyMessage() {
        try {
            System.out.print("[try return 1] ");
            return 1;
        } finally {
            System.out.print("[finally runs] ");
        }
    }

    /*
     Q: Can finally override a return from try/catch?
     A:
     - Yes. A return from finally replaces prior returns. Avoid this pattern.
    */
    private static void q_finallyOverridesReturn() {
        title("finally overriding return");
        int r = returnsOverriddenByFinally();
        System.out.println("returned=" + r + " (try returned 1, finally returned 2)");
    }

    private static int returnsOverriddenByFinally() {
        try {
            return 1;
        } finally {
            return 2; // overrides
        }
    }

    /*
     Q: try-with-resources control flow (closing order, suppressed exceptions)?
     A:
     - Resources close in reverse order of creation (LIFO).
     - Exceptions thrown during close are suppressed if another exception is already being thrown.
    */
    private static void q_tryWithResourcesFlow() {
        title("try-with-resources");
        try (Res r1 = new Res("r1", true); Res r2 = new Res("r2", true)) {
            System.out.print("[body throws] ");
            throw new RuntimeException("body");
        } catch (Exception e) {
            System.out.println("caught: " + e.getMessage());
            for (Throwable t : e.getSuppressed()) {
                System.out.println("suppressed: " + t.getMessage());
            }
        }
    }

    /*
     Q: Returning mutable objects and finally?
     A:
     - finally can mutate the object you are returning (object reference already chosen).
     - Avoid side-effects in finally; keep it for cleanup.
    */
    private static void q_mutableReturnModifiedInFinally() {
        title("mutable return modified in finally");
        StringBuilder sb = returnsBuilderMutatedInFinally();
        System.out.println("builder content after return/finally: " + sb);
    }

    private static StringBuilder returnsBuilderMutatedInFinally() {
        StringBuilder sb = new StringBuilder("try");
        try {
            return sb;
        } finally {
            sb.append("+finally");
        }
    }

    /*
     Q: Assertions for control flow?
     A:
     - Use assertions for internal invariants in dev/test; disabled by default at runtime.
     - Enable with: java -ea
    */
    private static void q_assertions() {
        title("assertions");
        try {
            assert 2 + 2 == 5 : "math broken";
            System.out.println("assert not enabled or condition true");
        } catch (AssertionError ae) {
            System.out.println("AssertionError caught: " + ae.getMessage());
        }
    }

    // ---------------------------- ADVANCED NOTES (no new syntax) ----------------------------

    /*
     Q: Unreachable code rules (compile-time)?
     A (notes):
     - A statement after an unconditional return/throw is a compile-time error (e.g., return; System.out.println();).
     - while(false) { ... } is a compile-time error (body unreachable).
     - if(false) { ... } is allowed (the if statement can complete normally).
     - Case labels must be reachable; duplicate case labels are compile-time errors.
    */
    private static void q_unreachableNotes() {
        title("unreachable code (notes)");
        System.out.println("See comments in source for rules and examples.");
        // Examples that would NOT compile (for reference only):
        // return; System.out.println("unreachable");
        // while (false) { System.out.println("unreachable"); }
    }

    /*
     Q: Modern switch (Java 14+): switch expressions, yield, no-fallthrough by default?
     A (notes):
     - switch as expression: int r = switch (x) { case 1 -> 10; case 2 -> 20; default -> 0; };
     - Use 'yield' inside block cases.
     - No implicit fall-through with arrow (->) cases; fall-through requires classic colon style with breaks.
     - Pattern matching for switch (Java 17+): switch on types with patterns and guards; handles null explicitly via 'case null'.
     - Pattern matching for instanceof (Java 16+): if (obj instanceof String s) { ... use s ... }
    */
    private static void q_switchExpressionAndPatternMatchingNotes() {
        title("switch expressions and pattern matching (notes)");
        System.out.println("See comments for Java 14+/17+ examples.");
        // Java 14+:
        // int r = switch (day) {
        //   case MON, TUE, WED, THU, FRI -> 1;
        //   case SAT, SUN -> 2;
        // };
        // Java 17+ pattern matching for switch:
        // Object o = ...;
        // int len = switch (o) {
        //   case String s -> s.length();
        //   case List<?> l -> l.size();
        //   case null -> 0;
        //   default -> -1;
        // };
    }

    /*
     Q: Labels: where are they allowed?
     A (notes):
     - A label can precede a statement; it's only meaningful with break/continue.
     - You can label loops and switch, not if/else directly (label applies to the statement that follows).
    */
    private static void q_labelUsageNotes() {
        title("labels usage (notes)");
        System.out.println("Labels apply to the following statement (often a loop/switch), not directly to if.");
        // Example:
        // OUTER: if (x) { ... } // Valid syntax (label on block), but break OUTER; inside won't compile unless OUTER labels a loop/switch.
    }

    private static void advancedNotes() {
        q_unreachableNotes();
        q_switchExpressionAndPatternMatchingNotes();
        q_labelUsageNotes();
    }

    // --------------------------- Helpers and Types ----------------------------

    private static void title(String s) {
        System.out.println("\nQ: " + s);
        System.out.println("A:");
    }

    private static int sideEffects = 0;

    private static boolean call(String label) {
        sideEffects++;
        System.out.print("call(" + label + ") ");
        return true;
    }

    private enum Day { MON, TUE, WED, THU, FRI, SAT, SUN }

    private static class Res implements AutoCloseable {
        private final String name;
        private final boolean throwOnClose;

        Res(String name, boolean throwOnClose) {
            this.name = name;
            this.throwOnClose = throwOnClose;
            System.out.print("[open " + name + "] ");
        }

        @Override
        public void close() {
            System.out.print("[close " + name + "] ");
            if (throwOnClose) {
                throw new RuntimeException("close:" + name);
            }
        }
    }
}