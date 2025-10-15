package _03_04_utility_classes_objects_math_uuid_random;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Comparator;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

/**
 * Utility Classes: Objects, Math, UUID, Random
 *
 * This file is a single, self-contained “theory + practice” reference.
 * It explains core ideas in comments and demonstrates each concept with runnable code.
 *
 * Topics covered:
 *  - java.util.Objects: null-safety helpers, equals/deepEquals, hash helpers, compare, toString, requireNonNull, (and notes on index checks)
 *  - java.lang.Math: constants, rounding, exact arithmetic, division/modulo semantics, transcendental functions, nextUp/down, copySign, ulp, random()
 *  - java.util.UUID: nature, versions, randomUUID(), parsing, name-based UUID (v3), variant/version, bytes and bits
 *  - java.util.Random: seeding, determinism, uniform generation, Gaussian, pitfalls, and notes on ThreadLocalRandom/SecureRandom
 *
 * Note: This code targets wide compatibility (e.g., Java 8+). Some newer APIs (e.g., Objects.checkIndex, requireNonNullElse)
 * are mentioned in comments but not used to keep compilation broad.
 */
public class _01_Theory {

    public static void main(String[] args) {
        System.out.println("=== Objects ===");
        demoObjects();

        System.out.println("\n=== Math ===");
        demoMath();

        System.out.println("\n=== UUID ===");
        demoUUID();

        System.out.println("\n=== Random ===");
        demoRandom();
    }

    // -----------------------------
    // Objects (java.util.Objects)
    // -----------------------------
    private static void demoObjects() {
        // Objects is a final utility class (static methods) primarily for:
        //  - Null-safety (requireNonNull, isNull/nonNull)
        //  - Equality and hashing (equals, deepEquals, hash, hashCode)
        //  - Comparison with null-handling (compare)
        //  - toString null-safe
        //  - (Since Java 9) Index checks: checkIndex/checkFromToIndex/checkFromIndexSize (not used here for Java 8 compatibility)

        // equals: null-safe logical equality
        System.out.println("Objects.equals(null, \"x\"): " + Objects.equals(null, "x"));  // false
        System.out.println("Objects.equals(\"x\", \"x\"): " + Objects.equals("x", "x"));  // true

        // deepEquals: recursive equality for arrays (including nested arrays)
        Object[] a1 = {"a", new int[]{1, 2, 3}};
        Object[] a2 = {"a", new int[]{1, 2, 3}};
        System.out.println("Objects.equals(a1, a2): " + Objects.equals(a1, a2));       // false (shallow)
        System.out.println("Objects.deepEquals(a1, a2): " + Objects.deepEquals(a1, a2)); // true (deep)

        // hash / hashCode: build composite hash codes; hashCode(obj) returns 0 for null
        int h1 = Objects.hash("id", 123, true);
        int h2 = Objects.hash("id", 123, true);
        System.out.println("Objects.hash(...) deterministic: " + (h1 == h2));

        // toString: null-safe with default value
        String s = null;
        System.out.println("Objects.toString(null, \"<default>\"): " + Objects.toString(s, "<default>"));

        // requireNonNull: fail fast with NullPointerException when an input must be non-null
        try {
            Objects.requireNonNull(null, "value must not be null"); // throws NPE
        } catch (NullPointerException npe) {
            System.out.println("requireNonNull message: " + npe.getMessage());
        }

        // isNull / nonNull: handy for predicates (e.g., stream filters)
        System.out.println("Objects.isNull(null): " + Objects.isNull(null));     // true
        System.out.println("Objects.nonNull(\"x\"): " + Objects.nonNull("x"));   // true

        // compare: consistent null handling with a comparator (null < non-null)
        Comparator<String> byLength = Comparator.comparingInt(String::length);
        System.out.println("Objects.compare(null, \"bb\", byLength): " + Objects.compare(null, "bb", byLength)); // -1
        System.out.println("Objects.compare(\"aa\", \"bb\", byLength): " + Objects.compare("aa", "bb", byLength)); // 0 (same length)

        // Example: implementing equals/hashCode safely with Objects
        Person p1 = new Person(1, "Ann");
        Person p2 = new Person(1, "Ann");
        System.out.println("Person equals/hashCode: " + (p1.equals(p2) && p1.hashCode() == p2.hashCode()));

        // Note: Since Java 9 (not used here), you can validate index ranges:
        // Objects.checkIndex(index, length), checkFromToIndex(from, to, length), checkFromIndexSize(from, size, length)
        // These throw IndexOutOfBoundsException with clear messages.
    }

    private static final class Person {
        final int id;
        final String name;

        Person(int id, String name) {
            this.id = id;
            this.name = Objects.requireNonNull(name, "name");
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Person)) return false;
            Person that = (Person) o;
            return id == that.id && Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name);
        }

        @Override
        public String toString() {
            return "Person{id=" + id + ", name=" + Objects.toString(name, "<null>") + "}";
        }
    }

    // -----------------------------
    // Math (java.lang.Math)
    // -----------------------------
    private static void demoMath() {
        // Math is a final utility class of static methods for:
        //  - Constants: PI, E
        //  - Basic ops: abs, max, min, signum
        //  - Rounding: floor, ceil, round, rint (ties-to-even)
        //  - Exact arithmetic (throws on overflow): addExact, subtractExact, multiplyExact, incrementExact, decrementExact, negateExact, toIntExact
        //  - Division & modulo with floor semantics: floorDiv, floorMod
        //  - Transcendentals: pow, sqrt, cbrt, exp, log, log10, log1p, expm1, trig (sin/cos/tan, asin/acos/atan/atan2)
        //  - Floating helpers: nextUp, nextDown, copySign, ulp, scalb
        //  - random(): double in [0.0, 1.0)
        // StrictMath offers strictly deterministic results across platforms (may be slower); Math may use platform-optimized implementations.

        System.out.println("PI: " + Math.PI + ", E: " + Math.E);

        double x = -2.7;
        System.out.println("abs(-2.7): " + Math.abs(x));
        System.out.println("signum(-2.7): " + Math.signum(x));
        System.out.println("floor(-2.7): " + Math.floor(x));
        System.out.println("ceil(-2.7): " + Math.ceil(x));
        System.out.println("round(-2.7): " + Math.round(x)); // long result for double input
        System.out.println("rint(2.5) [ties to even]: " + Math.rint(2.5));
        System.out.println("rint(3.5) [ties to even]: " + Math.rint(3.5));

        // floorDiv and floorMod vs / and %
        int a = -7, b = 3;
        System.out.println("Java / trunc toward zero: -7 / 3 = " + (a / b));           // -2
        System.out.println("Math.floorDiv(-7, 3) = " + Math.floorDiv(a, b));           // -3
        System.out.println("Java % remainder: -7 % 3 = " + (a % b));                    // -1
        System.out.println("Math.floorMod(-7, 3) = " + Math.floorMod(a, b));           // 2

        // Exact arithmetic throws ArithmeticException on overflow
        try {
            int overflow = Math.addExact(Integer.MAX_VALUE, 1);
            System.out.println("should not print: " + overflow);
        } catch (ArithmeticException ex) {
            System.out.println("addExact overflow detected: " + ex.getMessage());
        }

        // Transcendentals
        System.out.println("pow(2, 10): " + Math.pow(2, 10));
        System.out.println("sqrt(2): " + Math.sqrt(2));
        System.out.println("hypot(3, 4): " + Math.hypot(3, 4)); // 5.0

        // Floating utilities
        System.out.println("nextUp(1.0): " + Math.nextUp(1.0));
        System.out.println("nextDown(1.0): " + Math.nextDown(1.0));
        System.out.println("copySign(1.23, -0.0): " + Math.copySign(1.23, -0.0));
        System.out.println("ulp(1.0): " + Math.ulp(1.0));

        // random(): not cryptographically secure; thread-safe but can contend; consider ThreadLocalRandom for concurrency
        double r = Math.random(); // [0.0, 1.0)
        System.out.println("Math.random(): " + r);

        // Conversions
        System.out.println("toDegrees(PI): " + Math.toDegrees(Math.PI));
        System.out.println("toRadians(180): " + Math.toRadians(180));
    }

    // -----------------------------
    // UUID (java.util.UUID)
    // -----------------------------
    private static void demoUUID() {
        // UUID is a 128-bit immutable identifier with standard string form: 8-4-4-4-12 hex digits (36 chars with hyphens).
        // Common versions:
        //  - v4: randomly generated (UUID.randomUUID())
        //  - v3: name-based (MD5) -> UUID.nameUUIDFromBytes(...)
        //  - v5: name-based (SHA-1) not directly in java.util.UUID factory; v1 (time-based) not produced by JDK factory
        // UUID.randomUUID() uses a cryptographically strong PRNG under the hood (SecureRandom) in modern JDKs.

        UUID v4 = UUID.randomUUID();
        System.out.println("randomUUID (v4): " + v4);
        System.out.println("version: " + v4.version() + ", variant: " + v4.variant());

        // Parsing from canonical string
        UUID parsed = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        System.out.println("parsed UUID: " + parsed + " (version " + parsed.version() + ")");

        // Name-based v3 (MD5). Deterministic for the same input bytes.
        byte[] nameBytes = "example.com/myName".getBytes(StandardCharsets.UTF_8);
        UUID v3 = UUID.nameUUIDFromBytes(nameBytes);
        System.out.println("nameUUIDFromBytes (v3): " + v3 + ", version: " + v3.version());

        // Equality/ordering
        System.out.println("equals: " + v4.equals(UUID.fromString(v4.toString())));
        System.out.println("compareTo ordering stable: " + (v3.compareTo(v4) != 0));

        // Access raw 128 bits
        long msb = v4.getMostSignificantBits();
        long lsb = v4.getLeastSignificantBits();
        byte[] bytes = uuidToBytes(v4);
        System.out.println("MSB: 0x" + Long.toHexString(msb) + ", LSB: 0x" + Long.toHexString(lsb));
        System.out.println("Bytes (hex): " + toHex(bytes));

        // Time-based fields (timestamp/clockSequence/node) are only applicable to time-based UUIDs (e.g., v1).
        try {
            System.out.println("timestamp: " + v4.timestamp());
        } catch (UnsupportedOperationException ignored) {
            System.out.println("timestamp: unsupported for non-time-based UUIDs (e.g., v4)");
        }
    }

    private static byte[] uuidToBytes(UUID u) {
        // Pack 128 bits into 16 bytes, big-endian
        byte[] out = new byte[16];
        ByteBuffer bb = ByteBuffer.wrap(out);
        bb.putLong(u.getMostSignificantBits());
        bb.putLong(u.getLeastSignificantBits());
        return out;
    }

    private static String toHex(byte[] bytes) {
        char[] HEX = "0123456789abcdef".toCharArray();
        char[] out = new char[bytes.length * 2];
        for (int i = 0, j = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            out[j++] = HEX[v >>> 4];
            out[j++] = HEX[v & 0x0F];
        }
        return new String(out);
    }

    // -----------------------------
    // Random (java.util.Random)
    // -----------------------------
    private static void demoRandom() {
        // Random is a pseudo-random number generator (PRNG) with a 48-bit seed using an LCG (linear congruential generator).
        // Key points:
        //  - Deterministic for a given seed (repeatable sequences)
        //  - Methods are thread-safe but contend under heavy concurrency; prefer ThreadLocalRandom/SplittableRandom in multi-threaded code
        //  - Not cryptographically secure; DO NOT use for passwords/tokens. Use java.security.SecureRandom for security-sensitive randomness.

        // Determinism with a fixed seed
        Random r1 = new Random(12345L);
        int[] seq1 = { r1.nextInt(10), r1.nextInt(10), r1.nextInt(10) };

        Random r2 = new Random(12345L);
        int[] seq2 = { r2.nextInt(10), r2.nextInt(10), r2.nextInt(10) };

        System.out.println("Deterministic sequences equal: " + (seq1[0]==seq2[0] && seq1[1]==seq2[1] && seq1[2]==seq2[2]));
        System.out.println("Sample bounded ints (0..9): " + seq1[0] + ", " + seq1[1] + ", " + seq1[2]);

        // Correct uniform range: nextInt(bound) is unbiased
        Random r = new Random();
        int roll = r.nextInt(6) + 1; // 1..6
        System.out.println("Dice roll (1..6): " + roll);

        // Gaussian/normal distribution (mean=0, stddev=1)
        double g = r.nextGaussian();
        System.out.println("nextGaussian(): " + g);

        // Resetting seed restarts sequence
        r.setSeed(42L);
        System.out.println("After setSeed(42), nextInt(): " + r.nextInt());

        // Pitfall: Avoid x % bound for ranges; it can be negative and biased if bound doesn't divide 2^32.
        // Use nextInt(bound) for uniform positive ranges. For signed modulus needs, consider Math.floorMod.

        // Security note: Use SecureRandom for tokens/keys
        SecureRandom sr = new SecureRandom();
        byte[] token = new byte[16];
        sr.nextBytes(token);
        System.out.println("SecureRandom token (16B hex): " + toHex(token));

        // Concurrency note (not demonstrated): prefer java.util.concurrent.ThreadLocalRandom.current().nextInt()
        // or java.util.SplittableRandom for parallel streams and fork-join tasks.
    }
}