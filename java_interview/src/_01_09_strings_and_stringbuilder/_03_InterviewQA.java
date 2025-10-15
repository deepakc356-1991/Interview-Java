package _01_09_strings_and_stringbuilder;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.BreakIterator;
import java.text.Collator;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Arrays;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Strings & StringBuilder — Interview Q&A (basic → intermediate → advanced)
 *
 * Run main to see concise outputs; read comments for deeper explanations.
 * Target: Java 8+ (uses only APIs available in 8, mentions newer ones in comments).
 */
public final class _03_InterviewQA {

    // For compile-time constant folding demonstration:
    static final String CONST_A = "a";
    static final String CONST_B = "b";
    static final String CONST_AB_LITERAL = "ab";
    static final String CONST_AB_COMPILETIME = CONST_A + CONST_B; // compile-time folded, interned

    public static void main(String[] args) throws Exception {
        q1_stringImmutability();
        q2_stringPoolLiteralsVsNew();
        q3_equalsVsDoubleEquals();
        q4_concatPlusAndNull();
        q5_compileTimeFoldingAndFinals();
        q6_switchOnString();
        q7_stringIntern();
        q8_lengthVsCodePoints();
        q9_substringAndIndexes();
        q10_stringBuilderBasicsVsStringBuffer();
        q11_stringBuilderCapacityEnsureTrim();
        q12_stringBuilderApiEssentialOps();
        q13_stringBuilderEqualsPitfall();
        q14_concatInLoopsPerformance();
        q15_formatJoiner();
        q16_splitRegexPitfalls();
        q17_replaceVsReplaceAll();
        q18_trimVsStripAndBlank();
        q19_caseConversionLocale();
        q20_compareToVsCollator();
        q21_containsIndexOfMatches();
        q22_countOccurrences();
        q23_charsetsAndEncoding();
        q24_normalization();
        q25_graphemesAndReverse();
        q26_passwordsCharArrayVsString();
        q27_bestPracticesSummary();
    }

    private static void q1_stringImmutability() {
        header("1) String immutability");
        String s = "Hello";
        String t = s.concat(" World"); // creates a new String
        println("s == 'Hello' ? " + s.equals("Hello"));
        println("t == 'Hello World' ? " + t.equals("Hello World"));
        // Why immutable?
        // - Thread-safe sharing, safe keys in maps, hashCode caching, interning, security.
    }

    private static void q2_stringPoolLiteralsVsNew() {
        header("2) String pool: literals vs new String()");
        String a = "java";
        String b = "java";                   // same pooled literal
        String c = new String("java");       // new object on heap
        println("a == b (same literal) -> " + (a == b));
        println("a == c (new String) -> " + (a == c));
        println("a.equals(c) -> " + a.equals(c));
        // String literals are interned at class load; "new String" is not pooled unless intern() is called.
    }

    private static void q3_equalsVsDoubleEquals() {
        header("3) equals vs ==");
        String x = new String("x");
        String y = new String("x");
        println("x == y -> " + (x == y));           // false, reference comparison
        println("x.equals(y) -> " + x.equals(y));   // true, content comparison
        // Always use equals for content.
    }

    private static void q4_concatPlusAndNull() {
        header("4) + operator, concat, and null behavior");
        String base = "hi";
        // + uses String.valueOf(), safe with null:
        String plusNull = base + null; // -> "hinull"
        println("\"hi\" + null -> " + plusNull);
        // String.concat throws NPE if arg is null:
        try {
            String err = base.concat(null);
            println("Unexpected: " + err);
        } catch (NullPointerException npe) {
            println("base.concat(null) -> NPE");
        }
        // StringBuilder append handles null by appending "null":
        StringBuilder sb = new StringBuilder().append(base).append((String) null);
        println("StringBuilder append(null) -> " + sb.toString());
    }

    private static void q5_compileTimeFoldingAndFinals() {
        header("5) Compile-time constant folding and interning");
        String r1 = "ab";                   // pooled
        String r2 = "a" + "b";              // folded at compile time, pooled
        String r3 = CONST_AB_COMPILETIME;   // folded, pooled (because CONST_A/B are final constants)
        String a = "a";
        String b = "b";
        String r4 = a + b;                  // runtime concatenation, not pooled by default
        println("r1 == r2 -> " + (r1 == r2)); // true
        println("r1 == r3 -> " + (r1 == r3)); // true
        println("r1 == r4 -> " + (r1 == r4)); // false in general
    }

    private static void q6_switchOnString() {
        header("6) switch on String (since Java 7)");
        String cmd = "start";
        String res;
        switch (cmd) {
            case "start": res = "go"; break;
            case "stop":  res = "halt"; break;
            default:      res = "unknown";
        }
        println("switch(\"start\") -> " + res);
        // Compiles to hash-based dispatch + equals check.
    }

    private static void q7_stringIntern() {
        header("7) String.intern(): when and why");
        String a = new String("intern-me");
        String b = a.intern();      // canonical representation from the pool
        String c = "intern-me";     // literal from pool
        println("a == c -> " + (a == c)); // false
        println("b == c -> " + (b == c)); // true
        // Notes:
        // - Since Java 7, the string pool is on the heap (not PermGen).
        // - intern() can reduce duplicates for many repeating strings (e.g., IDs), but has overhead.
        // - G1 has String Deduplication (-XX:+UseStringDeduplication).
    }

    private static void q8_lengthVsCodePoints() {
        header("8) length() vs Unicode code points");
        String musicalSymbol = "𝄞"; // U+1D11E, needs surrogate pair
        println("musicalSymbol length (UTF-16 units) -> " + musicalSymbol.length()); // 2
        println("codePointCount -> " + musicalSymbol.codePointCount(0, musicalSymbol.length())); // 1
        println("First codePoint hex -> " + Integer.toHexString(musicalSymbol.codePointAt(0)));
        // charAt gives 16-bit code unit, not full code point for non-BMP characters.
    }

    private static void q9_substringAndIndexes() {
        header("9) substring indexing");
        String s = "abcdef";
        println("s.substring(2) -> " + s.substring(2));       // "cdef"
        println("s.substring(2, 5) -> " + s.substring(2, 5)); // "cde" (end-exclusive)
        // Note: In very old JDKs, substring could share the backing array; since Java 7u6 it copies (no leak).
        // Complexity: O(n) to copy result.
    }

    private static void q10_stringBuilderBasicsVsStringBuffer() {
        header("10) StringBuilder vs StringBuffer vs String");
        // String: immutable; concatenation in loops => O(n^2)
        // StringBuilder: mutable, not synchronized; prefer in single-threaded contexts.
        // StringBuffer: synchronized (thread-safe), typically slower; rarely needed.
        StringBuilder sb = new StringBuilder("abc");
        sb.append(123).append('-').append(true);
        println("StringBuilder demo -> " + sb.toString()); // abc123-true
    }

    private static void q11_stringBuilderCapacityEnsureTrim() {
        header("11) StringBuilder capacity, ensureCapacity, trimToSize");
        StringBuilder sb = new StringBuilder(); // default capacity 16
        println("Default capacity -> " + sb.capacity());
        sb.append("hello");
        println("Capacity after 'hello' -> " + sb.capacity());
        sb.ensureCapacity(100);
        println("Capacity after ensureCapacity(100) -> " + sb.capacity());
        sb.setLength(2); // truncate
        println("After setLength(2), content -> '" + sb.toString() + "', length=" + sb.length());
        sb.trimToSize();
        println("After trimToSize, capacity -> " + sb.capacity());
        // Growth rule: newCap = oldCap*2 + 2, then at least minCapacity.
    }

    private static void q12_stringBuilderApiEssentialOps() {
        header("12) StringBuilder essential methods");
        StringBuilder sb = new StringBuilder("012345");
        sb.insert(3, "[X]");
        sb.delete(1, 4);
        sb.replace(2, 5, "::");
        sb.reverse();
        println("Ops result -> " + sb);
        // Other useful:
        // - getChars(srcBegin, srcEnd, dst, dstBegin)
        // - codePointAt/Before/Count, offsetByCodePoints
        // - setCharAt, appendCodePoint
    }

    private static void q13_stringBuilderEqualsPitfall() {
        header("13) StringBuilder.equals() is reference identity");
        StringBuilder a = new StringBuilder("abc");
        StringBuilder b = new StringBuilder("abc");
        println("a.equals(b) -> " + a.equals(b));                 // false
        println("a.toString().equals(b.toString()) -> " + a.toString().equals(b.toString())); // true
    }

    private static void q14_concatInLoopsPerformance() {
        header("14) Concatenation in loops: O(n^2) pitfall");
        int n = 500; // small to keep runtime tiny
        String s = "";
        for (int i = 0; i < n; i++) {
            s += i; // each iteration creates a new String; avoid in hot paths
        }
        StringBuilder sb = new StringBuilder(n * 2);
        for (int i = 0; i < n; i++) sb.append(i);
        println("Built with + length=" + s.length() + ", with SB length=" + sb.length());
        // Benchmark properly using JMH (not System.nanoTime) for real performance analysis.
    }

    private static void q15_formatJoiner() {
        header("15) String.format vs joiners");
        String who = "Alice";
        int age = 30;
        String f = String.format("Name=%s, Age=%d", who, age); // slower, locale-aware
        StringJoiner joiner = new StringJoiner(", ", "{", "}")
                .add("A").add("B").add("C");
        String joined = String.join(" | ", "x", "y", "z"); // Java 8+
        println("format -> " + f);
        println("StringJoiner -> " + joiner);
        println("String.join -> " + joined);
    }

    private static void q16_splitRegexPitfalls() {
        header("16) split and regex pitfalls");
        String ip = "192.168.0.1";
        String[] wrong = ip.split(".");             // dot matches any char -> BAD
        String[] right = ip.split("\\.");           // escape dot
        println("wrong split length -> " + wrong.length);
        println("right split length -> " + right.length);

        String pipe = "a|b|c";
        String[] pipeWrong = pipe.split("|");       // splits every char (| is regex alternation)
        String[] pipeRight = pipe.split("\\|");
        println("pipeWrong length -> " + pipeWrong.length);
        println("pipeRight length -> " + pipeRight.length);

        String trail = "a,b,";
        println("split default drops trailing empties -> " + Arrays.toString(trail.split(",")));
        println("split with limit -1 keeps -> " + Arrays.toString(trail.split(",", -1)));
        // Prefer Pattern.quote(delimiter) for dynamic delimiters.
    }

    private static void q17_replaceVsReplaceAll() {
        header("17) replace vs replaceAll vs replaceFirst");
        String s = "foo$bar$baz";
        String a = s.replace("$", "#");              // literal replace
        String b = s.replaceAll("\\$", "#");         // regex replace (needs escaping)
        String c = s.replaceFirst("\\$", "#");       // first match only
        println("replace -> " + a);
        println("replaceAll -> " + b);
        println("replaceFirst -> " + c);
        // Prefer replace for literals (no regex engine overhead).
    }

    private static void q18_trimVsStripAndBlank() {
        header("18) trim vs strip, isEmpty vs isBlank");
        String s = "\u2003abc\u2003"; // EM space (not <= U+0020)
        println("trim does not remove EM space -> '" + s.trim() + "'");
        // In Java 11+: strip() removes all Unicode whitespace; isBlank() checks whitespace-only.
        // println("'" + s.strip() + "'");
        // println("'   '.isBlank() -> " + "   ".isBlank());
        println("'   '.isEmpty() -> " + "   ".isEmpty()); // false
        println("'   '.trim().isEmpty() -> " + "   ".trim().isEmpty()); // true as workaround pre-11
    }

    private static void q19_caseConversionLocale() {
        header("19) Case conversion and locale");
        String turkish = "i";
        println("default toUpperCase -> " + turkish.toUpperCase()); // depends on default Locale
        println("ROOT toUpperCase -> " + turkish.toUpperCase(Locale.ROOT));
        // Use Locale.ROOT for case-folding in protocol/IDs; locale-specific for UI.
        println("equalsIgnoreCase 'straße' vs 'STRASSE' -> " + "straße".equalsIgnoreCase("STRASSE"));
        // equalsIgnoreCase uses Unicode case mapping; for locale-sensitive comparison use Collator.
    }

    private static void q20_compareToVsCollator() {
        header("20) compareTo vs Collator for human sorting");
        String eAcute1 = "\u00E9cole";      // "école" (precomposed)
        String eAcute2 = "e\u0301cole";     // "e" + combining acute
        println("binary equals -> " + eAcute1.equals(eAcute2)); // false
        Collator coll = Collator.getInstance(Locale.FRENCH);
        coll.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
        println("Collator compare (treat accents) -> " + coll.compare(eAcute1, eAcute2)); // 0 (equal)
        // compareTo is lexicographic by UTF-16 code units; use Collator for user-facing sorts.
    }

    private static void q21_containsIndexOfMatches() {
        header("21) contains/indexOf vs matches/find");
        String s = "abc123xyz";
        println("s.contains(\"123\") -> " + s.contains("123"));
        println("s.indexOf(\"123\") -> " + s.indexOf("123"));
        println("s.matches(\"\\\\d+\") -> " + s.matches("\\d+")); // matches whole string -> false
        Matcher m = Pattern.compile("\\d+").matcher(s);
        println("Pattern.find (substring) -> " + m.find());       // true
    }

    private static void q22_countOccurrences() {
        header("22) Count occurrences of a substring");
        println("countOccurrences('aaaa','aa') -> " + countOccurrences("aaaa", "aa")); // overlapping -> 3? Decide policy
        println("countOccurrencesNonOverlapping('aaaa','aa') -> " + countOccurrencesNonOverlapping("aaaa", "aa")); // 2
    }

    private static void q23_charsetsAndEncoding() {
        header("23) Charsets and encoding pitfalls");
        String s = "€"; // Euro sign
        byte[] b1 = s.getBytes(); // platform default (danger)
        byte[] b2 = s.getBytes(StandardCharsets.UTF_8);
        println("default bytes length -> " + b1.length + " (default=" + Charset.defaultCharset().name() + ")");
        println("UTF-8 bytes length -> " + b2.length);
        String back = new String(b2, StandardCharsets.UTF_8);
        println("round-trip with UTF-8 -> " + back);
        // Always specify charset explicitly; avoid data loss.
    }

    private static void q24_normalization() {
        header("24) Unicode normalization");
        String nfd = Normalizer.normalize("e\u0301", Form.NFD);
        String nfc = Normalizer.normalize("e\u0301", Form.NFC);
        println("NFD vs NFC equals -> " + nfd.equals(nfc)); // false
        println("Normalize both to compare -> " + Normalizer.normalize(nfd, Form.NFC).equals(nfc)); // true
    }

    private static void q25_graphemesAndReverse() {
        header("25) Grapheme clusters and reverse()");
        String emoji = "A\uD83D\uDE03B"; // A + 😀 + B
        StringBuilder sb = new StringBuilder(emoji).reverse();
        println("reverse keeps surrogate pairs -> " + sb); // 😀 stays intact
        String accent = "A" + "e\u0301" + "B"; // A + e + combining acute + B
        String reversed = new StringBuilder(accent).reverse().toString();
        println("reverse with combining marks -> " + reversed + " (visually odd)");
        // For user-perceived characters (graphemes), use BreakIterator to iterate:
        BreakIterator it = BreakIterator.getCharacterInstance(Locale.ROOT);
        it.setText(emoji);
        println("Grapheme boundaries for '" + emoji + "':");
        for (int start = it.first(), end = it.next(); end != BreakIterator.DONE; start = end, end = it.next()) {
            println(" - [" + emoji.substring(start, end) + "]");
        }
    }

    private static void q26_passwordsCharArrayVsString() {
        header("26) Passwords: char[] vs String");
        // Prefer char[] for sensitive data: can be zeroed after use; String may linger in pool/heap.
        char[] pw = "secret".toCharArray();
        try {
            // ... use pw ...
        } finally {
            Arrays.fill(pw, '\0'); // clear
        }
        // In practice, other layers may still log or copy; parameterized APIs are safer.
    }

    private static void q27_bestPracticesSummary() {
        header("27) Best practices (quick checklist)");
        // - Use equals for content; avoid == except for identity checks or known pooled literals.
        // - Avoid + in loops; use StringBuilder with a good initial capacity.
        // - Prefer StringBuilder over StringBuffer unless you truly need synchronization.
        // - Specify Charset explicitly (UTF-8) for getBytes/new String IO.
        // - Be careful with regex in split/replaceAll; escape or use literal methods.
        // - Use Locale.ROOT for case-folding; Collator for locale-aware comparison/sort.
        // - Know length() is UTF-16 units; use codePoint APIs for non-BMP correctness.
        // - Consider intern() only when justified; measure memory and performance (JMH).
        // - For normalization-sensitive equality, normalize to NFC or use Collator with decomposition.
        // - substring indexes are [begin, end); expect O(n) copy.
        // - In Java 11+, prefer strip/isBlank/lines; pre-11, fall back to trim and manual checks.
        println("See comments for checklist.");
    }

    // Utilities and helpers

    private static int countOccurrences(String haystack, String needle) {
        // Overlapping count: slide by one
        if (needle.isEmpty()) return 0;
        int count = 0;
        for (int i = 0; i <= haystack.length() - needle.length(); i++) {
            if (haystack.regionMatches(i, needle, 0, needle.length())) count++;
        }
        return count;
    }

    private static int countOccurrencesNonOverlapping(String haystack, String needle) {
        if (needle.isEmpty()) return 0;
        int count = 0, from = 0, idx;
        while ((idx = haystack.indexOf(needle, from)) != -1) {
            count++;
            from = idx + needle.length();
        }
        return count;
    }

    private static void header(String title) {
        println("\n=== " + title + " ===");
    }

    private static void println(String s) {
        System.out.println(s);
    }
}