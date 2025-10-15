package _01_09_strings_and_stringbuilder;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.Collator;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Strings & StringBuilder — theory + runnable examples.
 *
 * Notes:
 * - String is immutable: all "modifying" methods return new String objects.
 * - StringBuilder is mutable and not thread-safe (use StringBuffer for synchronized operations).
 * - Use StringBuilder for repeated concatenation in loops. The compiler only folds constants;
 *   runtime concatenation still needs a builder under the hood.
 * - String literals live in the String pool; equal literals are the same object reference.
 * - Beware regular expressions: String.split, replaceAll, matches use regex; replace is literal.
 * - char is a 16-bit UTF-16 code unit; some Unicode code points use surrogate pairs (length 2).
 * - Locale matters for case conversions and collation (sorting). For passwords prefer char[] over String.
 *
 * This file uses only Java 8 API calls to compile broadly; newer APIs are mentioned in comments.
 */
public class _01_Theory {

    public static void main(String[] args) {
        header("Basics: literals, escapes, concatenation");
        basics();

        header("Identity vs equality and the String pool");
        identityVsEqualityAndPool();

        header("Immutability in practice");
        immutability();

        header("Common String operations");
        commonOps();

        header("Regex vs literal operations");
        regexVsLiteralOps();

        header("Splitting and joining");
        splittingAndJoining();

        header("Formatting (String.format)");
        formatting();

        header("Encoding and charsets");
        encodingAndCharsets();

        header("Unicode: code units vs code points");
        unicodeAndCodePoints();

        header("Locale-sensitive operations");
        localeAndCollation();

        header("Performance: concatenation in loops");
        performanceConcatInLoop();

        header("StringBuilder basics and capacity");
        stringBuilderBasics();

        header("StringBuilder editing operations");
        stringBuilderEditing();

        header("StringBuffer note (thread-safe alternative)");
        stringBufferNote();

        header("Security: char[] for secrets");
        charArrayForSecrets();

        header("Defensive coding tips");
        defensiveCodingTips();

        header("CharSequence polymorphism");
        charSequencePolymorphism();
    }

    // ------------------------------------------------------------
    // Basics
    // ------------------------------------------------------------
    private static void basics() {
        // Literals are pooled
        String hello = "Hello";
        String world = "World";

        // Escape sequences: \n (newline), \t (tab), \" (quote), \\ (backslash), \\uXXXX (unicode)
        String greeting = "Hello,\n\t\"Java\" \\ world!\u263A";
        System.out.println(greeting);

        // Concatenation: + creates a new String object (strings are immutable)
        String concat = hello + " " + world + " " + 2025;
        System.out.println("Concatenation: " + concat);

        // Compile-time constants are folded and interned
        final String A = "Ja";
        final String B = "va";
        String folded = A + B; // constant expression -> "Java"
        System.out.println("Folded constant equals literal? " + (folded == "Java"));

        // Non-constant at compile time => runtime concatenation
        String part = B;
        String notFolded = A + part;
        System.out.println("Runtime concat equals literal by == ? " + (notFolded == "Java"));
        System.out.println("Runtime concat equals literal by equals? " + notFolded.equals("Java"));

        // Since Java 9, the compiler/runtime may use invokedynamic (StringConcatFactory) for +.
        // Still, repeated concatenation in loops should use StringBuilder manually.
    }

    // ------------------------------------------------------------
    // Identity vs Equality and String Pool
    // ------------------------------------------------------------
    private static void identityVsEqualityAndPool() {
        String s1 = "Java";
        String s2 = "Java";                // same pooled literal
        String s3 = new String("Java");    // new object on the heap
        String s4 = s3.intern();           // canonical pooled reference

        System.out.println("s1 == s2 (pooled literals)         : " + (s1 == s2));
        System.out.println("s1 == s3 (new String)               : " + (s1 == s3));
        System.out.println("s1 == s4 (interned)                 : " + (s1 == s4));
        System.out.println("s1.equals(s3) (content equality)    : " + s1.equals(s3));

        // Compile-time folding
        String s5 = "Ja" + "va";
        System.out.println("s1 == s5 (compile-time folded)      : " + (s1 == s5));

        // Runtime concatenation
        String va = "va";
        String s6 = "Ja" + va;
        System.out.println("s1 == s6 (runtime concat)           : " + (s1 == s6));
        System.out.println("s1.equals(s6)                       : " + s1.equals(s6));
    }

    // ------------------------------------------------------------
    // Immutability
    // ------------------------------------------------------------
    private static void immutability() {
        String s = "abc";
        String upper = s.toUpperCase(); // returns a new String
        System.out.println("Original: " + s + ", upper: " + upper);

        String replaced = s.replace('a', 'z');
        System.out.println("Original: " + s + ", replaced: " + replaced);

        String concat = s + "def"; // creates new objects
        System.out.println("Original: " + s + ", concat: " + concat);

        // Because of immutability:
        // - Strings are thread-safe for reading.
        // - They can be shared and pooled.
        // - You cannot "clear" a String's content.
    }

    // ------------------------------------------------------------
    // Common operations
    // ------------------------------------------------------------
    private static void commonOps() {
        String t = "  Hello, Java! Java?  ";
        System.out.println("t: [" + t + "] len=" + t.length());

        // Whitespace trim (ASCII spaces)
        String trimmed = t.trim(); // Note: Java 11 added strip/stripLeading/stripTrailing for Unicode
        System.out.println("trimmed: [" + trimmed + "]");

        // charAt, indexOf/lastIndexOf
        System.out.println("charAt(2): " + t.charAt(2));
        System.out.println("indexOf(\"Java\"): " + t.indexOf("Java"));
        System.out.println("lastIndexOf(\"Java\"): " + t.lastIndexOf("Java"));

        // substring(begin, end)
        String sub = trimmed.substring(7, 11); // [7, 11): "Java"
        System.out.println("substring(7,11) of trimmed: " + sub);

        // contains, startsWith, endsWith
        System.out.println("contains(\"Hello\"): " + t.contains("Hello"));
        System.out.println("startsWith(\"  He\"): " + t.startsWith("  He"));
        System.out.println("endsWith(\"  \"): " + t.endsWith("  "));

        // equals/equalsIgnoreCase/compareTo
        System.out.println("\"java\".equalsIgnoreCase(\"JaVa\"): " + "java".equalsIgnoreCase("JaVa"));
        System.out.println("\"abc\".compareTo(\"abd\"): " + "abc".compareTo("abd"));
        System.out.println("\"abc\".compareTo(\"abc\"): " + "abc".compareTo("abc"));
        System.out.println("\"b\".compareTo(\"a\"): " + "b".compareTo("a"));

        // regionMatches (with ignoreCase)
        boolean region = "foobar".regionMatches(true, 0, "FOO", 0, 3);
        System.out.println("regionMatches ignoreCase 'foo' in 'foobar': " + region);

        // replace (literal), replaceAll/replaceFirst (regex)
        String dotted = "a.b.c";
        System.out.println("replace literal '.' with '#': " + dotted.replace(".", "#"));
        System.out.println("replaceAll regex '.' with '#': " + dotted.replaceAll("\\.", "#"));
        System.out.println("replaceFirst regex 'a|b' with 'X': " + "a-b-c".replaceFirst("a|b", "X"));

        // matches is full-string regex match
        System.out.println("\"123\".matches(\"\\\\d+\"): " + "123".matches("\\d+"));
    }

    // ------------------------------------------------------------
    // Regex vs literal examples
    // ------------------------------------------------------------
    private static void regexVsLiteralOps() {
        String s = "v1.2.3";
        // Regex dot means "any character", must escape
        String[] wrong = s.split(".");     // BAD: splits every character
        String[] right = s.split("\\.");   // GOOD: split on literal dot
        System.out.println("split wrong (dot regex): " + Arrays.toString(wrong));
        System.out.println("split right (escaped dot): " + Arrays.toString(right));

        // Use Pattern.quote to safely treat any string as a literal in regex
        String delimiter = "|"; // special in regex
        String[] safe = "a|b|c".split(Pattern.quote(delimiter));
        System.out.println("split with Pattern.quote: " + Arrays.toString(safe));

        // Replace literal vs regex
        String p = "price$10$";
        System.out.println("replace literal $ with #: " + p.replace("$", "#"));
        System.out.println("replaceAll regex $ (end) -> '#': " + p.replaceAll("\\$", "#"));
        // Note: In regex, $ means "end of input"; escaping is required to match the literal char.

        // Regex search
        Pattern pattern = Pattern.compile("(\\d+)");
        Matcher m = pattern.matcher("abc123xyz");
        if (m.find()) {
            System.out.println("Found number: " + m.group(1));
        }
    }

    // ------------------------------------------------------------
    // Splitting and joining
    // ------------------------------------------------------------
    private static void splittingAndJoining() {
        // split drops trailing empty strings by default
        System.out.println("Split 'a,b,,c' by ',': " + Arrays.toString("a,b,,c".split(",")));
        // Keep trailing empties with limit < 0
        System.out.println("Split keep trailing empties: " + Arrays.toString("a,b,,c,".split(",", -1)));

        // Join
        String joined = String.join(", ", "one", "two", "three");
        System.out.println("String.join: " + joined);

        // StringJoiner for prefix/suffix
        StringJoiner sj = new StringJoiner(", ", "[", "]");
        sj.add("red").add("green").add("blue");
        System.out.println("StringJoiner: " + sj.toString());
    }

    // ------------------------------------------------------------
    // Formatting
    // ------------------------------------------------------------
    private static void formatting() {
        // Locale affects decimal separators and more
        String us = String.format(Locale.US, "Name: %s, Age: %d, Score: %.2f", "Ana", 30, 98.7654);
        String de = String.format(Locale.GERMANY, "Preis: %.2f €", 1234.5);
        System.out.println(us);
        System.out.println(de);

        // Argument indexes: %2$d refers to second arg
        String order = String.format("Second=%2$d, First=%1$d", 11, 22);
        System.out.println(order);

        // Note: Since Java 15, "Hello %s".formatted("world") exists (not used here for Java 8 compatibility).
    }

    // ------------------------------------------------------------
    // Encoding and charsets
    // ------------------------------------------------------------
    private static void encodingAndCharsets() {
        String euro = "€ and café";
        byte[] utf8 = euro.getBytes(StandardCharsets.UTF_8);
        byte[] latin1 = euro.getBytes(Charset.forName("ISO-8859-1")); // € not representable -> replaced
        System.out.println("UTF-8 bytes length: " + utf8.length);
        System.out.println("ISO-8859-1 bytes length: " + latin1.length);

        String roundtrip = new String(utf8, StandardCharsets.UTF_8);
        System.out.println("Roundtrip UTF-8: " + roundtrip);
    }

    // ------------------------------------------------------------
    // Unicode: code units vs code points
    // ------------------------------------------------------------
    private static void unicodeAndCodePoints() {
        // Contains BMP 'A' (1 unit), emoji '😊' (U+1F60A, 2 units), musical G clef '𝄞' (U+1D11E, 2 units), 'Z' (1 unit)
        String s = "A😊𝄞Z";
        System.out.println("s: " + s);
        System.out.println("length (UTF-16 code units): " + s.length());
        System.out.println("codePointCount: " + s.codePointCount(0, s.length()));

        // Iterate code points (safe for supplementary characters)
        System.out.print("Code points (hex): ");
        s.codePoints().forEach(cp -> System.out.print(String.format("U+%04X ", cp)));
        System.out.println();

        // toCharArray shows code units (may split surrogate pairs)
        System.out.println("char[] length (code units): " + s.toCharArray().length);

        // Substring hazards: cutting inside a surrogate pair yields broken text
        String broken = s.substring(1, 2); // likely half of '😊'
        System.out.println("Potentially broken substring [1,2): " + broken);

        // Safe iteration by code points
        System.out.print("Chars from code points: ");
        int i = 0;
        while (i < s.length()) {
            int cp = s.codePointAt(i);
            char[] chars = Character.toChars(cp);
            System.out.print(new String(chars));
            i += Character.charCount(cp);
        }
        System.out.println();
    }

    // ------------------------------------------------------------
    // Locale-sensitive operations
    // ------------------------------------------------------------
    private static void localeAndCollation() {
        // Turkish I problem: lowercase of 'I' depends on locale
        String upperI = "I";
        System.out.println("EN lower of 'I': " + upperI.toLowerCase(Locale.ENGLISH)); // "i"
        System.out.println("TR lower of 'I': " + upperI.toLowerCase(new Locale("tr"))); // "ı" (dotless i)

        // Collation (sorting/compare) with locale
        String aUmlaut = "ä";
        String aPlain = "a";

        Collator collGerman = Collator.getInstance(Locale.GERMAN);
        collGerman.setStrength(Collator.PRIMARY); // ignore accents
        int cmp = collGerman.compare(aUmlaut, aPlain);
        System.out.println("German collator PRIMARY compare 'ä' vs 'a': " + cmp + " (0 => considered equal)");
    }

    // ------------------------------------------------------------
    // Performance: concatenation in loops
    // ------------------------------------------------------------
    private static void performanceConcatInLoop() {
        final int N = 5000;

        long t1 = System.nanoTime();
        String s = "";
        for (int i = 0; i < N; i++) {
            s += i; // creates many short-lived Strings
        }
        long t2 = System.nanoTime();

        long t3 = System.nanoTime();
        StringBuilder sb = new StringBuilder(N * 4); // capacity hint
        for (int i = 0; i < N; i++) {
            sb.append(i);
        }
        String s2 = sb.toString();
        long t4 = System.nanoTime();

        System.out.println("Concatenation with '+': " + (t2 - t1) / 1_000_000 + " ms, len=" + s.length());
        System.out.println("StringBuilder append   : " + (t4 - t3) / 1_000_000 + " ms, len=" + s2.length());
    }

    // ------------------------------------------------------------
    // StringBuilder basics and capacity
    // ------------------------------------------------------------
    private static void stringBuilderBasics() {
        // Default capacity is 16, grows as needed (newCap = old*2 + 2)
        StringBuilder sb = new StringBuilder();
        System.out.println("Default capacity: " + sb.capacity() + ", length: " + sb.length());

        sb.append("Hello");
        System.out.println("After append 'Hello': capacity=" + sb.capacity() + ", length=" + sb.length());

        // Ensure capacity to reduce realloc/copies
        sb.ensureCapacity(100);
        System.out.println("After ensureCapacity(100): capacity=" + sb.capacity());

        // Construct with initial capacity
        StringBuilder sb2 = new StringBuilder(8);
        System.out.println("sb2 initial capacity  : " + sb2.capacity());

        // Construct from String => capacity = str.length() + 16
        StringBuilder sb3 = new StringBuilder("Java");
        System.out.println("sb3 capacity from 'Java': " + sb3.capacity());

        // toString creates a new String snapshot of current content
        String snapshot = sb3.toString();
        sb3.setCharAt(0, 'Y');
        System.out.println("snapshot: " + snapshot + ", sb3 now: " + sb3);

        // Clear builder
        sb3.setLength(0);
        System.out.println("sb3 after clear setLength(0): length=" + sb3.length());

        // trimToSize reduces capacity to current length
        sb.ensureCapacity(200);
        sb.trimToSize();
        System.out.println("After trimToSize: capacity=" + sb.capacity());
    }

    // ------------------------------------------------------------
    // StringBuilder editing operations
    // ------------------------------------------------------------
    private static void stringBuilderEditing() {
        StringBuilder sb = new StringBuilder("0123456789");
        sb.insert(5, "-INSERT-");
        System.out.println("insert: " + sb);

        sb.delete(5, 13); // remove the inserted part
        System.out.println("delete: " + sb);

        sb.deleteCharAt(0);
        System.out.println("deleteCharAt(0): " + sb);

        sb.setCharAt(0, 'X');
        System.out.println("setCharAt(0,'X'): " + sb);

        sb.replace(1, 3, "ABCD");
        System.out.println("replace(1,3,'ABCD'): " + sb);

        sb.reverse();
        System.out.println("reverse: " + sb);

        // substring returns a new String
        String sub = sb.substring(2, 6);
        System.out.println("substring(2,6) as String: " + sub);

        // Chainable appends
        String chained = new StringBuilder()
                .append("Hello").append(' ').append(123).append(' ').append(true)
                .toString();
        System.out.println("Chained builder: " + chained);
    }

    // ------------------------------------------------------------
    // StringBuffer note
    // ------------------------------------------------------------
    private static void stringBufferNote() {
        // StringBuffer is synchronized (thread-safe) but typically slower than StringBuilder.
        // Use when the same buffer is mutated from multiple threads with shared access.
        StringBuffer buf = new StringBuffer("Safe");
        buf.append("-Buffer");
        System.out.println("StringBuffer: " + buf.toString());
    }

    // ------------------------------------------------------------
    // Security and secrets
    // ------------------------------------------------------------
    private static void charArrayForSecrets() {
        char[] password = {'s', 'e', 'c', 'r', 'e', 't'};
        // Process the password...
        // After use, wipe it:
        Arrays.fill(password, '\0');

        // Do NOT keep secrets in String; they are immutable, pooled, and may stay in memory.
        String secret = "hardcoded-secret";
        System.out.println("Avoid keeping secrets in Strings: " + secret.replaceAll(".", "*"));
    }

    // ------------------------------------------------------------
    // Defensive coding tips
    // ------------------------------------------------------------
    private static void defensiveCodingTips() {
        // Avoid NPE: compare using constant on the left
        String maybeNull = null;
        System.out.println("\"abc\".equals(maybeNull): " + "abc".equals(maybeNull));
        try {
            System.out.println("maybeNull.equals(\"abc\"): " + maybeNull.equals("abc")); // NPE
        } catch (NullPointerException e) {
            System.out.println("Caught NPE when calling equals on null");
        }

        // Distinguish empty vs null
        String empty = "";
        System.out.println("empty.isEmpty(): " + empty.isEmpty());
        System.out.println("maybeNull == null: " + (maybeNull == null));

        // Convert null to "null" text via String.valueOf
        System.out.println("String.valueOf(null): " + String.valueOf((Object) null));

        // Convert null to default with Objects.toString
        System.out.println("Objects.toString(null,\"<none>\"): " + Objects.toString(null, "<none>"));

        // Splitting by literal user-provided delimiter: Pattern.quote it
        String userDelim = ".+*?|^$()[]{}\\";
        String[] safe = "a.+*?|^$()[]{}\\b".split(Pattern.quote(userDelim));
        System.out.println("Safe split by user delimiter: " + Arrays.toString(safe));
    }

    // ------------------------------------------------------------
    // CharSequence polymorphism
    // ------------------------------------------------------------
    private static void charSequencePolymorphism() {
        printFirstChar("String");
        printFirstChar(new StringBuilder("Builder"));
        printFirstChar(new StringBuffer("Buffer"));
    }

    private static void printFirstChar(CharSequence cs) {
        System.out.println("First char of [" + cs + "] is '" + cs.charAt(0) + "'");
    }

    // ------------------------------------------------------------
    // Utilities
    // ------------------------------------------------------------
    private static void header(String title) {
        System.out.println("\n--- " + title + " ---");
    }
}