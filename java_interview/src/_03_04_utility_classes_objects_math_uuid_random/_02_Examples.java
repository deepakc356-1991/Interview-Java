package _03_04_utility_classes_objects_math_uuid_random;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class _02_Examples {

    public static void main(String[] args) {
        objectsExamples();
        mathExamples();
        uuidExamples();
        randomExamples();
    }

    // ------------------------------------------------------------
    // Objects
    // ------------------------------------------------------------
    private static void objectsExamples() {
        System.out.println("\n=== java.util.Objects examples ===");

        // 1) Null-safe equality
        String s1 = null;
        String s2 = "hello";
        System.out.println("Objects.equals(null, \"hello\") = " + Objects.equals(s1, s2)); // false
        System.out.println("Objects.equals(null, null) = " + Objects.equals(s1, null));   // true

        // 2) Deep equals (arrays)
        int[] a1 = {1, 2, 3};
        int[] a2 = {1, 2, 3};
        System.out.println("Objects.deepEquals(int[], int[]) = " + Objects.deepEquals(a1, a2)); // true

        Object[] left = { new int[]{1,2}, "X" };
        Object[] right = { new int[]{1,2}, "X" };
        System.out.println("Objects.deepEquals(Object[], Object[]) = " + Objects.deepEquals(left, right)); // true

        // 3) Hash helpers
        System.out.println("Objects.hash(null, \"hello\", 123) = " + Objects.hash(null, "hello", 123));
        System.out.println("Objects.hashCode(null) = " + Objects.hashCode(null)); // 0

        // 4) Null-safe toString
        System.out.println("Objects.toString(null) = " + Objects.toString(null)); // "null"
        System.out.println("Objects.toString(null, \"<default>\") = " + Objects.toString(null, "<default>"));

        // 5) requireNonNull with custom message
        try {
            Objects.requireNonNull(null, "Argument must not be null");
        } catch (NullPointerException e) {
            System.out.println("requireNonNull threw: " + e.getMessage());
        }

        // 6) isNull / nonNull (useful with Streams)
        List<String> names = Arrays.asList("Ann", null, "Bob", null, "Cat");
        long nonNullCount = names.stream().filter(Objects::nonNull).count();
        System.out.println("Non-null count = " + nonNullCount);

        // 7) compare using a Comparator
        int cmp = Objects.compare("Alice", "Bob", String::compareTo);
        System.out.println("Objects.compare(\"Alice\", \"Bob\") = " + cmp); // negative => "Alice" < "Bob"

        // 8) Implement equals and hashCode easily
        Book b1 = new Book("978-1", "Effective Java", 2018);
        Book b2 = new Book("978-1", "Effective Java", 2018);
        System.out.println("b1.equals(b2) = " + b1.equals(b2));
        System.out.println("b1.hashCode() = " + b1.hashCode() + ", b2.hashCode() = " + b2.hashCode());

        // 9) Index checks (Java 9+)
        // Uncomment if using Java 9+
        // int idx = Objects.checkIndex(2, names.size());                 // returns 2
        // int range = Objects.checkFromToIndex(1, 3, names.size());      // returns 1
        // int size = Objects.checkFromIndexSize(1, 2, names.size());     // returns 1

        // 10) Null defaults (Java 9+)
        // String result = Objects.requireNonNullElse(null, "fallback");
        // String lazy = Objects.requireNonNullElseGet(null, () -> "computed");
    }

    // Example domain class showing Objects.equals/hash
    static final class Book {
        final String isbn;
        final String title;
        final Integer year;

        Book(String isbn, String title, Integer year) {
            this.isbn = isbn;
            this.title = title;
            this.year = year;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Book)) return false;
            Book that = (Book) o;
            return Objects.equals(this.isbn, that.isbn)
                    && Objects.equals(this.title, that.title)
                    && Objects.equals(this.year, that.year);
        }

        @Override
        public int hashCode() {
            return Objects.hash(isbn, title, year);
        }

        @Override
        public String toString() {
            return "Book{isbn=" + Objects.toString(isbn, "<none>") +
                    ", title=" + Objects.toString(title, "<none>") +
                    ", year=" + Objects.toString(year) + "}";
        }
    }

    // ------------------------------------------------------------
    // Math
    // ------------------------------------------------------------
    private static void mathExamples() {
        System.out.println("\n=== java.lang.Math examples ===");

        // 1) Constants
        System.out.println("PI = " + Math.PI + ", E = " + Math.E);

        // 2) Basic min/max/abs
        System.out.println("max(10, 20) = " + Math.max(10, 20));
        System.out.println("min(-5, 3)  = " + Math.min(-5, 3));
        System.out.println("abs(-42)    = " + Math.abs(-42));

        // 3) Powers and roots
        System.out.println("pow(2, 10)  = " + Math.pow(2, 10));
        System.out.println("sqrt(81)    = " + Math.sqrt(81));
        System.out.println("cbrt(27)    = " + Math.cbrt(27));
        System.out.println("hypot(3,4)  = " + Math.hypot(3, 4)); // 5.0

        // 4) Logarithms and exponent
        System.out.println("log(E)      = " + Math.log(Math.E));   // natural log
        System.out.println("log10(1000) = " + Math.log10(1000));   // base 10
        System.out.println("exp(1)      = " + Math.exp(1));        // e^1

        // 5) Trigonometry and angle conversion
        double deg = 30.0;
        double rad = Math.toRadians(deg);
        System.out.println("toRadians(30) = " + rad);
        System.out.println("sin(30°) = " + Math.sin(rad) + ", cos(30°) = " + Math.cos(rad));

        // 6) Rounding
        System.out.println("floor(2.7) = " + Math.floor(2.7));
        System.out.println("ceil(2.1)  = " + Math.ceil(2.1));
        System.out.println("round(2.5) = " + Math.round(2.5));   // 3 (returns long for double)
        System.out.println("round(-2.5) = " + Math.round(-2.5)); // -2 (round half away from zero)
        System.out.println("rint(2.5) = " + Math.rint(2.5));     // Banker's rounding to nearest even

        // 7) Signs and next representable values
        System.out.println("signum(-3.14) = " + Math.signum(-3.14));
        System.out.println("copySign(3.0, -2.0) = " + Math.copySign(3.0, -2.0));
        System.out.println("nextUp(1.0) = " + Math.nextUp(1.0));
        System.out.println("nextDown(1.0) = " + Math.nextDown(1.0));
        System.out.println("ulp(1.0) = " + Math.ulp(1.0));

        // 8) Exact arithmetic with overflow checks (Java 8+)
        try {
            int x = Math.addExact(Integer.MAX_VALUE, 1);
            System.out.println("addExact result: " + x);
        } catch (ArithmeticException ex) {
            System.out.println("addExact overflow caught: " + ex.getMessage());
        }
        System.out.println("multiplyExact(1000, 1000) = " + Math.multiplyExact(1000, 1000));

        // 9) floorDiv and floorMod (useful with negatives)
        int a = -7, b = 4;
        System.out.println("-7 / 4 = " + (-7 / 4) + " (trunc toward 0)");
        System.out.println("floorDiv(-7,4) = " + Math.floorDiv(a, b)); // -2
        System.out.println("-7 % 4 = " + (-7 % 4));
        System.out.println("floorMod(-7,4) = " + Math.floorMod(a, b)); // 1

        // 10) Clamp helper using min/max
        System.out.println("clamp(15, 0, 10) = " + clamp(15, 0, 10));
        System.out.println("clamp(-3, 0, 10) = " + clamp(-3, 0, 10));

        // 11) Math.random() produces [0.0, 1.0)
        double r = Math.random();
        System.out.println("Math.random() = " + r);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    // ------------------------------------------------------------
    // UUID
    // ------------------------------------------------------------
    private static void uuidExamples() {
        System.out.println("\n=== java.util.UUID examples ===");

        // 1) Random (version 4) UUID
        UUID u1 = UUID.randomUUID();
        System.out.println("randomUUID = " + u1);
        System.out.println("version = " + u1.version() + ", variant = " + u1.variant());

        // 2) Name-based (version 3, MD5) UUID: deterministic for same input
        byte[] nameBytes = "user:alice@example.com".getBytes();
        UUID u2a = UUID.nameUUIDFromBytes(nameBytes);
        UUID u2b = UUID.nameUUIDFromBytes(nameBytes);
        System.out.println("nameUUIDFromBytes (same input) equal? " + u2a.equals(u2b)); // true
        System.out.println("name-based UUID = " + u2a);

        // 3) Parse from/to string
        UUID parsed = UUID.fromString(u1.toString());
        System.out.println("parsed equals original? " + parsed.equals(u1));

        // 4) Access raw bits and rebuild
        long msb = u1.getMostSignificantBits();
        long lsb = u1.getLeastSignificantBits();
        UUID rebuilt = new UUID(msb, lsb);
        System.out.println("rebuilt equals original? " + rebuilt.equals(u1));

        // 5) Hyphenless (if needed for compact storage)
        String hyphenless = u1.toString().replace("-", "");
        System.out.println("hyphenless = " + hyphenless);

        // 6) Quick uniqueness check
        Set<UUID> set = new HashSet<>();
        for (int i = 0; i < 1000; i++) set.add(UUID.randomUUID());
        System.out.println("Generated " + set.size() + " unique UUIDs out of 1000");
    }

    // ------------------------------------------------------------
    // Random
    // ------------------------------------------------------------
    private static void randomExamples() {
        System.out.println("\n=== java.util.Random (and friends) examples ===");

        // 1) Reproducible sequences with seed
        Random r1 = new Random(42);
        System.out.println("Seeded nextInt() = " + r1.nextInt());
        System.out.println("Seeded nextDouble() = " + r1.nextDouble());
        System.out.println("Seeded nextInt(10) [0..9] = " + r1.nextInt(10));

        // Same seed => same sequence
        Random r2 = new Random(42);
        System.out.println("Same-seed nextInt() matches? " + (r2.nextInt() == new Random(42).nextInt()));

        // 2) Range helpers
        int min = 5, maxInclusive = 10;
        int inRange = r1.nextInt((maxInclusive - min) + 1) + min; // [min..max]
        System.out.println("Random int in [" + min + "," + maxInclusive + "] = " + inRange);

        // 3) Gaussian (normal) distribution: mean 0, stddev 1, transform to mean=170, stddev=10
        double z = r1.nextGaussian(); // N(0,1)
        double height = 170 + z * 10;
        System.out.println("Gaussian sample (height ~ N(170,10^2)) = " + String.format(Locale.US, "%.2f", height));

        // 4) Streams of random numbers (Java 8+)
        int sum = r1.ints(5, 0, 100).sum(); // 5 ints in [0,100)
        System.out.println("Sum of 5 random ints in [0,100) = " + sum);

        // 5) ThreadLocalRandom for concurrent contexts
        int th = ThreadLocalRandom.current().nextInt(100); // [0,100)
        long tlLong = ThreadLocalRandom.current().nextLong(0L, 1_000_000L); // [0,1_000_000)
        System.out.println("ThreadLocalRandom int [0,100) = " + th + ", long [0,1e6) = " + tlLong);

        // 6) SecureRandom for security tokens
        SecureRandom secure = new SecureRandom();
        byte[] token = new byte[16]; // 128-bit token
        secure.nextBytes(token);
        System.out.println("Secure token (hex) = " + toHex(token));

        // 7) Random strings
        System.out.println("Random alphanumeric (16) = " + randomAlphaNumeric(16, secure));

        // 8) Shuffle and sample
        List<Integer> list = IntStream.rangeClosed(1, 10).boxed().collect(Collectors.toList());
        Collections.shuffle(list, new Random(123)); // deterministic shuffle with seed
        System.out.println("Shuffled 1..10 (seed=123) = " + list);
        List<Integer> sample = list.subList(0, 3);
        System.out.println("Sample 3 without replacement = " + sample);

        // 9) Weighted random selection
        Map<String, Integer> weights = new LinkedHashMap<>();
        weights.put("Common", 70);
        weights.put("Uncommon", 20);
        weights.put("Rare", 9);
        weights.put("Legendary", 1);
        String pick = pickWeighted(weights, new Random());
        System.out.println("Weighted pick = " + pick);

        // 10) Math.random vs Random
        System.out.println("Math.random() = " + Math.random() + ", new Random().nextDouble() = " + new Random().nextDouble());
    }

    // Weighted random selection (weights must be non-negative; at least one > 0)
    private static <T> T pickWeighted(Map<T, Integer> weights, Random rnd) {
        int total = 0;
        for (int w : weights.values()) {
            if (w < 0) throw new IllegalArgumentException("Negative weight");
            total += w;
        }
        if (total <= 0) throw new IllegalArgumentException("Total weight must be > 0");
        int r = rnd.nextInt(total);
        int acc = 0;
        for (Map.Entry<T, Integer> e : weights.entrySet()) {
            acc += e.getValue();
            if (r < acc) return e.getKey();
        }
        throw new IllegalStateException("Unreachable");
    }

    private static String toHex(byte[] bytes) {
        char[] hex = "0123456789abcdef".toCharArray();
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(hex[(b >>> 4) & 0xF]).append(hex[b & 0xF]);
        }
        return sb.toString();
    }

    private static String randomAlphaNumeric(int length, Random rng) {
        final char[] alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(alphabet[rng.nextInt(alphabet.length)]);
        }
        return sb.toString();
    }
}