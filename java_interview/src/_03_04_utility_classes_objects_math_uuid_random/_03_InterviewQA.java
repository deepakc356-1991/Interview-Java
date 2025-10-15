package _03_04_utility_classes_objects_math_uuid_random;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;
import java.util.Random;
import java.util.SplittableRandom;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

public class _03_InterviewQA {

    public static void main(String[] args) {
        sep("Utility Classes: Objects, Math, UUID, Random - Interview Q&A");
        qaObjects();
        qaMath();
        qaRandom();
        qaUuid();
        sep("Done");
    }

    // ---------- OBJECTS ----------
    static void qaObjects() {
        sep("java.util.Objects");

        QA qa = new QA();

        qa.q("What is java.util.Objects and why use it?",
             "A null-safe utility with static helpers: equals, deepEquals, hash, toString, compare, requireNonNull*, isNull/nonNull, and index checks.");

        qa.q("Objects.equals vs a.equals(b)?",
             "Objects.equals(a,b) is null-safe; a.equals(b) throws NPE if a is null.");

        // Demo: equals vs arrays
        int[] arr1 = {1, 2};
        int[] arr2 = {1, 2};
        out("Objects.equals(arr1, arr2) -> " + Objects.equals(arr1, arr2) + " (shallow, arrays compare by reference)");
        out("Arrays.equals(arr1, arr2)   -> " + Arrays.equals(arr1, arr2) + " (correct for primitive arrays)");
        Object[] nested1 = {new int[]{1, 2}, "x"};
        Object[] nested2 = {new int[]{1, 2}, "x"};
        out("Objects.deepEquals(nested1, nested2) -> " + Objects.deepEquals(nested1, nested2) + " (deep for nested arrays)");

        qa.q("When to use Objects.hash?",
             "Convenient but allocates varargs array and is relatively slow. Prefer manual hash or record hash, especially in hot paths.");

        // Demo: hash
        String s1 = "A", s2 = "B";
        out("Objects.hash(s1, s2) -> " + Objects.hash(s1, s2));

        qa.q("Objects.requireNonNull: why and how?",
             "Fail-fast null checks with optional custom message; also requireNonNullElse/ElseGet to supply defaults without branching.");

        // Demo: requireNonNull, requireNonNullElse
        String maybeNull = null;
        String def = Objects.requireNonNullElse(maybeNull, "default");
        out("Objects.requireNonNullElse(null, \"default\") -> " + def);
        try {
            Objects.requireNonNull(maybeNull, "id must not be null");
        } catch (NullPointerException npe) {
            out("requireNonNull threw: " + npe.getMessage());
        }

        qa.q("Objects.toString with default?",
             "Objects.toString(obj, default) avoids NPE and prints default when obj is null.");
        out("Objects.toString(null, \"<none>\") -> " + Objects.toString(null, "<none>"));

        qa.q("Null predicates isNull/nonNull use-case?",
             "Useful for method references in streams: filter(Objects::nonNull)");

        // Demo: nonNull filter
        var list = Arrays.asList("a", null, "b");
        var filtered = list.stream().filter(Objects::nonNull).collect(Collectors.toList());
        out("Filter non-null -> " + filtered);

        qa.q("Objects.compare usage?",
             "Null-safe comparison with a Comparator; great for custom ordering.");
        Comparator<String> nullsLastCaseInsensitive = Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER);
        out("Objects.compare(\"a\",\"B\", nullsLastCI) -> " + Objects.compare("a", "B", nullsLastCaseInsensitive));

        qa.q("Bounds checking helpers (since Java 9)?",
             "Objects.checkIndex, checkFromToIndex, checkFromIndexSize validate indices with standard exceptions.");
        out("Objects.checkIndex(3, 5) -> " + Objects.checkIndex(3, 5));
        out("Objects.checkFromToIndex(1, 4, 4) -> " + Objects.checkFromToIndex(1, 4, 4));

        qa.q("Arrays and Objects.hash pitfalls?",
             "Objects.hash is shallow for array elements; use Arrays.hashCode / deepHashCode for arrays.");

        qa.q("equals/hashCode best practices?",
             "- Maintain the contract. - For arrays, use Arrays.*. - For records, generated methods are good. - Keep hash stable and fast.");
    }

    // ---------- MATH ----------
    static void qaMath() {
        sep("java.lang.Math (and StrictMath)");

        QA qa = new QA();

        qa.q("Math vs StrictMath?",
             "Math may use faster, platform-optimized intrinsics (within spec tolerances). StrictMath provides fully deterministic, bit-for-bit results across platforms and is typically slower.");

        qa.q("Random via Math.random()?",
             "Convenience method delegating to an internal Random. Not cryptographically secure; prefer ThreadLocalRandom/SplittableRandom for performance, SecureRandom for security.");

        qa.q("Rounding: round, floor, ceil, rint differences?",
             "round -> nearest integer, ties toward +∞ for float/double (beware negatives). floor -> greatest integer <= x. ceil -> smallest integer >= x. rint -> ties to even as double.");
        out("Math.round( 1.5f) -> " + Math.round(1.5f));
        out("Math.round(-1.5f) -> " + Math.round(-1.5f));
        out("Math.floor(-1.2)  -> " + Math.floor(-1.2));
        out("Math.ceil(-1.2)   -> " + Math.ceil(-1.2));
        out("Math.rint(2.5)    -> " + Math.rint(2.5) + " (ties to even)");

        qa.q("Division and modulo with negatives: floorDiv/floorMod?",
             "Use floorDiv and floorMod for mathematical division/modulo consistent with floor. Plain / and % can surprise with negatives.");
        out("(-7)/3            -> " + (-7 / 3) + " (truncates toward 0)");
        out("Math.floorDiv(-7,3)-> " + Math.floorDiv(-7, 3) + " (floors)");
        out("(-7)%3            -> " + (-7 % 3));
        out("Math.floorMod(-7,3)-> " + Math.floorMod(-7, 3));

        qa.q("Overflow-safe arithmetic?",
             "Use Math.addExact/subtractExact/multiplyExact/incrementExact/decrementExact/negateExact; they throw ArithmeticException on overflow.");
        try {
            int over = Math.addExact(Integer.MAX_VALUE, 1);
            out("addExact MAX+1 -> " + over);
        } catch (ArithmeticException e) {
            out("addExact threw: " + e);
        }

        qa.q("Floating-point edge cases: NaN and signed zero?",
             "Any operation with NaN generally yields NaN. +0.0 and -0.0 are distinct; some ops (min/max) preserve sign.");
        double n1 = Double.NaN;
        out("Math.min(NaN, 1.0) -> " + Math.min(n1, 1.0));
        double plusZero = 0.0, minusZero = -0.0;
        double minZero = Math.min(plusZero, minusZero);
        out("Math.min(0.0, -0.0) -> " + minZero + " (sign preserved: " + (Double.doubleToRawLongBits(minZero) == Double.doubleToRawLongBits(-0.0)) + ")");

        qa.q("Precision helpers: ulp, nextUp/nextDown/nextAfter?",
             "ULP is unit in last place near a value; nextUp/Down give adjacent representable doubles.");
        out("Math.ulp(1.0)        -> " + Math.ulp(1.0));
        out("Math.nextUp(1.0)      -> " + Math.nextUp(1.0));
        out("Math.nextDown(1.0)    -> " + Math.nextDown(1.0));
        out("Math.nextAfter(1.0, 2)-> " + Math.nextAfter(1.0, 2.0));

        qa.q("When to use BigDecimal vs double?",
             "Use BigDecimal for money/precise decimal arithmetic; double is binary floating-point and introduces rounding errors.");
        BigDecimal price = new BigDecimal("0.10");
        BigDecimal sum = price.add(price).add(price);
        out("BigDecimal 0.10*3 -> " + sum + " (exact)");
        out("double 0.1*3      -> " + (0.1 * 3));

        qa.q("Fused multiply-add (fma)?",
             "Math.fma(x,y,z) computes x*y+z with a single rounding improving numerical stability in some cases. Not for overflow avoidance.");
        double a = 1e-16, b = 1e16, c = -1.0;
        double naive = (a * b) + c;
        double fused = Math.fma(a, b, c);
        out("(a*b)+c -> " + naive + ", fma(a,b,c) -> " + fused + " (fma may be more accurate)");

        qa.q("Trigonometry and radians?",
             "Math.sin/cos/tan expect radians. Convert degrees with Math.toRadians/Math.toDegrees.");
        out("sin(90°) -> " + Math.sin(Math.toRadians(90)));
    }

    // ---------- RANDOM ----------
    static void qaRandom() {
        sep("Random, ThreadLocalRandom, SplittableRandom, SecureRandom");

        QA qa = new QA();

        qa.q("Random vs ThreadLocalRandom vs SplittableRandom vs SecureRandom?",
             "- Random: general-purpose, thread-safe via internal CAS but contended when shared.\n" +
             "- ThreadLocalRandom: per-thread, fast in concurrent code.\n" +
             "- SplittableRandom: high-quality, splittable for parallel algorithms, no nextBoolean bias, immutable seed state.\n" +
             "- SecureRandom: cryptographically strong, slower; use for tokens/keys.");

        qa.q("Is Math.random() secure?",
             "No. It uses a PRNG similar to Random. Use SecureRandom for security-sensitive contexts.");

        qa.q("How to get a random int in [0, bound)?",
             "Use nextInt(bound). Avoid x % bound because of modulo bias and negative values.");
        Random r = new Random(123);
        out("new Random(123).nextInt(10) -> " + r.nextInt(10));
        out("ThreadLocalRandom.current().nextInt(10) -> " + ThreadLocalRandom.current().nextInt(10));

        qa.q("Generating long/double in a range?",
             "Use ThreadLocalRandom.nextLong(origin, bound) or SplittableRandom.nextLong(origin, bound) to avoid bias.");
        var tlr = ThreadLocalRandom.current();
        out("TLR nextLong(5, 10) -> " + tlr.nextLong(5, 10));
        var sr = new SplittableRandom(42);
        out("SplittableRandom(42).nextDouble(0.0,1.0) -> " + sr.nextDouble(0.0, 1.0));

        qa.q("Why not new Random() per call?",
             "Creating many instances can be expensive and may cause duplicate seeds if created in a tight loop. Reuse or use ThreadLocalRandom.");

        qa.q("Reproducibility via seeds?",
             "Seeding a PRNG reproduces the same sequence. Useful for tests/simulations; avoid for security.");
        var seeded1 = new Random(7);
        var seeded2 = new Random(7);
        out("Seeded equality -> " + (seeded1.nextInt() == seeded2.nextInt()));

        qa.q("Parallel algorithms: which PRNG?",
             "SplittableRandom is designed for parallelism; use split() to create independent generators in tasks.");
        SplittableRandom base = new SplittableRandom(99);
        SplittableRandom childA = base.split();
        SplittableRandom childB = base.split();
        out("Split children A,B independent -> " + childA.nextInt(1000) + ", " + childB.nextInt(1000));

        qa.q("Streams of random numbers?",
             "Use ints()/longs()/doubles() stream APIs; be mindful of boxing and terminal operations.");
        int sum = sr.ints(5, 0, 10).sum();
        out("SplittableRandom.ints(5,0,10).sum() -> " + sum);

        qa.q("Shuffling a list?",
             "Use Collections.shuffle(list) for default randomness, or pass a specific Random implementation.");
        var list = IntStream.range(1, 6).boxed().collect(Collectors.toList());
        Collections.shuffle(list, ThreadLocalRandom.current());
        out("Shuffled [1..5] -> " + list);

        qa.q("Secure token generation?",
             "Use SecureRandom and encode bytes (Base64/hex). Avoid Random/Math.random for tokens.");
        byte[] token = new byte[16];
        new SecureRandom().nextBytes(token);
        out("SecureRandom 16-byte token (hex) -> " + toHex(token));

        qa.q("Newer Java PRNG APIs?",
             "Since Java 17, java.util.random.RandomGenerator/RandomGeneratorFactory provide pluggable algorithms (e.g., L128X256MixRandom). Prefer them when available.");
    }

    // ---------- UUID ----------
    static void qaUuid() {
        sep("java.util.UUID");

        QA qa = new QA();

        qa.q("What is a UUID and which versions does Java support?",
             "A 128-bit identifier. Java can generate v4 (random) via randomUUID and v3 (name-based, MD5) via nameUUIDFromBytes. It can parse other versions but not generate v5/v6/v7 out of the box.");

        qa.q("How to generate a UUID?",
             "UUID.randomUUID() produces a v4 UUID using a strong RNG in modern JDKs.");
        UUID u = UUID.randomUUID();
        out("randomUUID -> " + u + " (version " + u.version() + ", variant " + u.variant() + ")");

        qa.q("Name-based UUID (v3) example?",
             "Deterministic from input bytes (MD5). Not for security; MD5 is broken for collision resistance.");
        UUID nameBased = UUID.nameUUIDFromBytes("com.acme:customer:42".getBytes());
        out("nameUUIDFromBytes(...) -> " + nameBased + " (version " + nameBased.version() + ")");

        qa.q("Parsing and validation?",
             "UUID.fromString validates format and throws IllegalArgumentException on bad input.");
        try {
            UUID.fromString("not-a-uuid");
        } catch (IllegalArgumentException iae) {
            out("fromString failed as expected: " + iae.getMessage());
        }

        qa.q("Are UUIDs unique? Are they secure?",
             "Uniqueness is probabilistic (v4 ~122 bits randomness). Collisions are astronomically unlikely. v4 from UUID.randomUUID is generally secure for identifiers; do not derive secrets from UUIDs.");

        qa.q("How to store UUIDs in databases?",
             "Prefer binary 16 bytes (BINARY(16)/VARBINARY(16)) over text (CHAR(36)) for space and speed. Consider time-ordered variants for clustered indexes.");

        qa.q("Time-ordered UUIDs (v1/v7)?",
             "Time-ordered UUIDs improve index locality. Java can parse v1 fields (timestamp, node) but does not generate v1/v7 natively. Use libraries if needed.");

        qa.q("Extracting fields and version?",
             "Use uuid.version(), uuid.variant(). timestamp(), clockSequence(), node() work for time-based UUIDs only; they throw for others.");
        try {
            long ts = u.timestamp(); // likely throws for v4
            out("timestamp -> " + ts);
        } catch (UnsupportedOperationException ex) {
            out("timestamp on v4 threw: " + ex);
        }

        qa.q("Convert UUID to bytes and back?",
             "Use ByteBuffer with two longs (MSB, LSB) in big-endian order.");
        byte[] bytes = uuidToBytes(u);
        UUID back = bytesToUuid(bytes);
        out("uuid -> bytes -> uuid roundtrip equal -> " + u.equals(back));

        qa.q("Create UUID from two longs?",
             "UUID(msb, lsb). Ensure you set version/variant bits if you craft your own.");
        UUID crafted = new UUID(u.getMostSignificantBits(), u.getLeastSignificantBits());
        out("crafted equals original -> " + crafted.equals(u));

        qa.q("Can I use UUIDs as secrets?",
             "Not recommended unless you know the RNG source. UUID.randomUUID uses strong RNG in modern JDKs, but dedicated tokens (SecureRandom bytes) are preferable.");
    }

    // ---------- Helpers ----------
    static void sep(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }

    static void out(String s) {
        System.out.println(s);
    }

    static String toHex(byte[] bytes) {
        final char[] HEX = "0123456789abcdef".toCharArray();
        char[] out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            out[i * 2] = HEX[v >>> 4];
            out[i * 2 + 1] = HEX[v & 0x0F];
        }
        return new String(out);
    }

    static byte[] uuidToBytes(UUID uuid) {
        ByteBuffer bb = ByteBuffer.allocate(16);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

    static UUID bytesToUuid(byte[] bytes) {
        if (bytes.length != 16) throw new IllegalArgumentException("UUID bytes must be 16 length");
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        long msb = bb.getLong();
        long lsb = bb.getLong();
        return new UUID(msb, lsb);
    }

    static class QA {
        int n = 1;
        void q(String question, String answer) {
            System.out.println("Q" + (n++) + ": " + question);
            System.out.println("A: " + answer);
        }
    }
}