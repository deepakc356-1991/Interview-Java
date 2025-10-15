package _01_09_strings_and_stringbuilder;

import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.regex.Pattern;

public class _02_Examples {

    public static void main(String[] args) {
        System.out.println("=== Strings & StringBuilder Examples ===");
        stringCreationAndPool();
        equalityAndComparison();
        concatenationAndImmutability();
        commonStringMethods();
        regexVsLiteralReplaceAndSplit();
        formattingAndJoin();
        stringBuilderBasics();
        stringBuilderAdvanced();
        pitfalls();
        performanceDemo();
    }

    // 1) Creation, immutability, and the String pool
    private static void stringCreationAndPool() {
        System.out.println("\n-- String creation & pool --");
        String a = "hello";
        String b = "hello"; // same literal, pooled
        System.out.println("a == b: " + (a == b)); // true

        String c = new String("hello"); // new object, not pooled by default
        System.out.println("a == c: " + (a == c)); // false
        System.out.println("a.equals(c): " + a.equals(c)); // true

        String d = c.intern(); // put/get from pool
        System.out.println("a == c.intern(): " + (a == d)); // true

        // Compile-time concatenation vs runtime
        String x1 = "ab";
        String x2 = "a" + "b"; // compile-time constant, pooled
        System.out.println("x1 == x2: " + (x1 == x2)); // true

        String part = "b";
        String x3 = "a" + part; // runtime concatenation, not pooled automatically
        System.out.println("x1 == x3: " + (x1 == x3)); // false

        final String constPart = "b";
        String x4 = "a" + constPart; // still compile-time, pooled
        System.out.println("x1 == x4: " + (x1 == x4)); // true
    }

    // 2) Equality and comparison
    private static void equalityAndComparison() {
        System.out.println("\n-- Equality & comparison --");
        String s1 = "Java";
        String s2 = "java";
        System.out.println("s1.equals(s2): " + s1.equals(s2)); // false
        System.out.println("s1.equalsIgnoreCase(s2): " + s1.equalsIgnoreCase(s2)); // true

        // Lexicographical comparison
        System.out.println("s1.compareTo(\"JDK\"): " + s1.compareTo("JDK")); // positive (J>J then a> D etc.)
        System.out.println("s1.compareToIgnoreCase(s2): " + s1.compareToIgnoreCase(s2)); // 0

        // Region comparison
        String hay = "Hello World";
        String needle = "WORLD";
        boolean region = hay.regionMatches(true, 6, needle, 0, 5); // ignoreCase, start at index 6
        System.out.println("regionMatches ignoreCase: " + region); // true
    }

    // 3) Concatenation and immutability
    private static void concatenationAndImmutability() {
        System.out.println("\n-- Concatenation & immutability --");
        String s = "Hi";
        s.concat("!"); // ignored (result not stored)
        System.out.println("After s.concat(\"!\") without assignment: " + s); // "Hi"
        s = s + "!"; // creates new String
        System.out.println("After s = s + \"!\": " + s); // "Hi!"

        // Concatenation in a loop - inefficient with '+'
        int n = 2000; // keep small for quick demo
        long t1 = System.nanoTime();
        String slow = "";
        for (int i = 0; i < n; i++) {
            slow += i; // creates many intermediate objects
        }
        long d1 = System.nanoTime() - t1;

        long t2 = System.nanoTime();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append(i);
        }
        String fast = sb.toString();
        long d2 = System.nanoTime() - t2;

        System.out.printf("Loop concat with '+': %.2f ms, with StringBuilder: %.2f ms%n", d1 / 1e6, d2 / 1e6);
        System.out.println("Result lengths equal: " + (slow.length() == fast.length()));
    }

    // 4) Common String methods
    private static void commonStringMethods() {
        System.out.println("\n-- Common String methods --");
        String s = "  Java Strings  ";
        System.out.println("Original: [" + s + "]");
        System.out.println("length(): " + s.length());
        System.out.println("isEmpty(): " + s.isEmpty());
        System.out.println("trim(): [" + s.trim() + "]");

        s = "Java Strings";
        System.out.println("charAt(0): " + s.charAt(0));
        System.out.println("indexOf(\"Str\"): " + s.indexOf("Str"));
        System.out.println("lastIndexOf('a'): " + s.lastIndexOf('a'));
        System.out.println("substring(5): " + s.substring(5));
        System.out.println("substring(5, 11): " + s.substring(5, 11)); // "String"
        System.out.println("contains(\"ring\"): " + s.contains("ring"));
        System.out.println("startsWith(\"Java\"): " + s.startsWith("Java"));
        System.out.println("endsWith(\"ings\"): " + s.endsWith("ings"));
        System.out.println("toUpperCase(): " + s.toUpperCase());
        System.out.println("toLowerCase(): " + s.toLowerCase());

        // replace vs replaceFirst (regex aware) vs literal replace
        String r = "foo bar foo";
        System.out.println("replace(\"foo\",\"baz\"): " + r.replace("foo", "baz")); // all occurrences
        System.out.println("replaceFirst(\"foo\",\"baz\"): " + r.replaceFirst("foo", "baz")); // first occurrence

        // valueOf for conversions
        System.out.println("String.valueOf(123): " + String.valueOf(123));
        System.out.println("String.valueOf(true): " + String.valueOf(true));

        // toCharArray returns a new array (independent)
        char[] chars = s.toCharArray();
        System.out.println("toCharArray length: " + chars.length);
    }

    // 5) Regex vs literal replace and split
    private static void regexVsLiteralReplaceAndSplit() {
        System.out.println("\n-- Regex vs literal: replace & split --");
        String t = "a.b.c";
        System.out.println("t.replace(\".\", \"-\"): " + t.replace(".", "-")); // literal replace
        System.out.println("t.replaceAll(\".\", \"-\"): " + t.replaceAll(".", "-")); // regex '.' matches any char

        // Correct regex to match literal dot:
        System.out.println("t.replaceAll(\"\\\\.\", \"-\"): " + t.replaceAll("\\.", "-"));
        // Or safely escape any literal via Pattern.quote
        System.out.println("t.replaceAll(Pattern.quote(\".\"), \"-\"): " + t.replaceAll(Pattern.quote("."), "-"));

        String nums = "1.2.3";
        System.out.println("Split on '.' incorrectly: " + Arrays.toString(nums.split("."))); // not what you want
        System.out.println("Split on literal dot: " + Arrays.toString(nums.split("\\.")));
        System.out.println("Split using Pattern.quote: " + Arrays.toString(nums.split(Pattern.quote("."))));

        // split with limit: keep trailing empties
        String csv = "a,b,";
        System.out.println("csv.split(\",\"): " + Arrays.toString(csv.split(","))); // trailing empty dropped
        System.out.println("csv.split(\",\", -1): " + Arrays.toString(csv.split(",", -1))); // keep trailing empty
    }

    // 6) Formatting and joining
    private static void formattingAndJoin() {
        System.out.println("\n-- Formatting & join --");
        String name = "Ada";
        double score = 93.4567;
        String f = String.format("Hi %s, you scored %.2f%%", name, score);
        System.out.println("String.format: " + f);

        String joined = String.join(", ", "red", "green", "blue");
        System.out.println("String.join: " + joined);

        StringJoiner joiner = new StringJoiner(" | ", "[", "]");
        joiner.add("alpha").add("beta").add("gamma");
        System.out.println("StringJoiner: " + joiner.toString());
    }

    // 7) StringBuilder basics
    private static void stringBuilderBasics() {
        System.out.println("\n-- StringBuilder basics --");
        StringBuilder sb = new StringBuilder(); // default capacity 16
        sb.append("Hello");
        sb.append(' ');
        sb.append("World");
        System.out.println("append chain: " + sb);

        sb.insert(5, ",");
        System.out.println("insert comma: " + sb);

        sb.delete(5, 6);
        System.out.println("delete comma: " + sb);

        sb.replace(6, sb.length(), "Builder");
        System.out.println("replace tail: " + sb);

        sb.reverse();
        System.out.println("reverse: " + sb);

        sb.reverse();
        sb.setCharAt(0, 'h');
        System.out.println("setCharAt(0, 'h'): " + sb);

        // Capacity management
        System.out.println("capacity(): " + sb.capacity());
        sb.ensureCapacity(100);
        System.out.println("ensureCapacity(100) -> capacity(): " + sb.capacity());
        sb.trimToSize();
        System.out.println("trimToSize() -> capacity(): " + sb.capacity());

        // setLength can truncate or pad with '\0'
        sb.setLength(3);
        System.out.println("setLength(3): [" + sb + "] length=" + sb.length());
        sb.setLength(6); // pads with '\0' (not visible)
        System.out.println("setLength(6): length=" + sb.length() + " charAt(3) is NUL? " + (sb.charAt(3) == '\0'));

        String result = sb.toString(); // creates an immutable String snapshot
        sb.append(" (mutated)");
        System.out.println("String snapshot: [" + result + "]");
        System.out.println("Builder after mutate: [" + sb + "]");
    }

    // 8) StringBuilder advanced
    private static void stringBuilderAdvanced() {
        System.out.println("\n-- StringBuilder advanced --");
        StringBuilder sb = new StringBuilder(4);
        System.out.println("Initial capacity(4): " + sb.capacity());
        sb.append("abcd"); // exactly fills
        sb.append("e");    // triggers growth
        System.out.println("After appends, capacity(): " + sb.capacity() + ", content: " + sb);

        // Append Unicode code point (e.g., 😀 U+1F600)
        StringBuilder emoji = new StringBuilder();
        emoji.append("Face: ").appendCodePoint(0x1F600);
        System.out.println("appendCodePoint: " + emoji + " length=" + emoji.length());

        // Build a CSV efficiently
        String[] items = {"alpha", "beta", "gamma"};
        StringBuilder csv = new StringBuilder();
        for (int i = 0; i < items.length; i++) {
            if (i > 0) csv.append(',');
            csv.append(items[i]);
        }
        System.out.println("CSV via builder: " + csv);
    }

    // 9) Pitfalls and tips
    private static void pitfalls() {
        System.out.println("\n-- Pitfalls & tips --");

        // Null handling
        String maybeNull = null;
        System.out.println("\"prefix\" + null -> " + ("prefix" + maybeNull)); // "prefixnull"
        // Avoid NPE: call equals on the constant, or use Objects.toString
        System.out.println("\"yes\".equals(maybeNull): " + "yes".equals(maybeNull)); // false, no NPE
        System.out.println("Objects.toString(maybeNull, \"<null>\"): " + Objects.toString(maybeNull, "<null>"));

        // Index bounds
        try {
            "abc".substring(1, 5); // error
        } catch (StringIndexOutOfBoundsException e) {
            System.out.println("Caught StringIndexOutOfBoundsException: " + e.getMessage());
        }

        // Regex pitfalls: need escaping when using special chars
        String dotted = "v1.2.3";
        System.out.println("Correct split on dot: " + Arrays.toString(dotted.split("\\.")));

        // Trailing empty tokens when splitting
        String trail = "a,b,";
        System.out.println("Keep trailing empties: " + Arrays.toString(trail.split(",", -1)));

        // char[] is mutable (unlike String). toCharArray() returns a copy.
        String secret = "secret";
        char[] buf = secret.toCharArray();
        Arrays.fill(buf, '*');
        System.out.println("Masked from char[]: " + new String(buf));
        System.out.println("Original String unchanged: " + secret);

        // subSequence vs substring
        CharSequence seq = "abcdef".subSequence(1, 4); // "bcd"
        System.out.println("subSequence(1,4): " + seq);

        // compareTo ordering example
        System.out.println("\"Zoo\".compareTo(\"apple\"): " + "Zoo".compareTo("apple")); // negative/positive depending on case
    }

    // 10) Performance demo: String vs StringBuilder in loops
    private static void performanceDemo() {
        System.out.println("\n-- Performance demo (small loop) --");
        final int n = 5000;

        long t1 = System.nanoTime();
        String s = "";
        for (int i = 0; i < n; i++) {
            s += 'x';
        }
        long d1 = System.nanoTime() - t1;

        long t2 = System.nanoTime();
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            sb.append('x');
        }
        String s2 = sb.toString();
        long d2 = System.nanoTime() - t2;

        System.out.printf("'+=' took: %.2f ms, StringBuilder took: %.2f ms%n", d1 / 1e6, d2 / 1e6);
        System.out.println("Lengths equal: " + (s.length() == s2.length()));
    }
}