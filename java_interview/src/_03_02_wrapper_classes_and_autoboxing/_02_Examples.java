package _03_02_wrapper_classes_and_autoboxing;

import java.util.*;
import java.util.stream.*;

/**
 * Wrapper Classes & Autoboxing examples.
 * Covers:
 * - Wrapper creation and basics
 * - Autoboxing/unboxing
 * - Collections and generics
 * - Null unboxing pitfalls
 * - Equality, caching, and identity vs value equality
 * - Comparisons and sorting
 * - Parsing/formatting and radix
 * - Constants and metadata
 * - Unsigned operations (Integer/Long)
 * - Overload resolution with boxing/widening/varargs
 * - Primitive vs boxed streams
 * - Varargs with wrappers and null
 * - Numeric promotions with wrappers
 * - Map counting idioms (boxing in practice)
 * - Character and Boolean wrapper utilities
 * - Arrays.asList with primitives vs wrappers
 * - OptionalInt vs Optional<Integer>
 */
public class _02_Examples {

    public static void main(String[] args) {
        demoBasics();
        demoBoxingUnboxing();
        demoCollectionsAndGenerics();
        demoNullUnboxingPitfall();
        demoEqualityAndCaching();
        demoComparisons();
        demoParsingAndFormatting();
        demoConstantsAndMeta();
        demoUnsignedOps();
        demoOverloadResolution();
        demoStreamsPrimitiveVsBoxed();
        demoVarargsAndNull();
        demoNumericPromotionWithWrappers();
        demoMapCounting();
        demoCharacterWrapper();
        demoBooleanWrapper();
        demoArrayPitfall();
        demoOptionalPrimitiveVsBoxed();
    }

    // 1) Basics: primitives vs wrappers, creation, immutability
    static void demoBasics() {
        System.out.println("\n--- Basics ---");
        int p = 42;                         // primitive
        Integer w = Integer.valueOf(42);    // wrapper via valueOf (uses cache for small values)

        // All wrapper types (immutable):
        Byte wb = Byte.valueOf((byte) 10);
        Short ws = Short.valueOf((short) 20);
        Integer wi = Integer.valueOf(30);
        Long wl = Long.valueOf(40L);
        Float wf = Float.valueOf(1.5f);
        Double wd = Double.valueOf(2.5);
        Boolean wz = Boolean.valueOf(true);
        Character wc = Character.valueOf('A');

        // Deprecated: 'new Integer(42)' etc. Prefer valueOf/factories.
        System.out.println("primitive p=" + p + ", wrapper w=" + w);
        System.out.println("Wrappers are immutable; operations create new instances.");
    }

    // 2) Autoboxing/unboxing in assignments and expressions
    static void demoBoxingUnboxing() {
        System.out.println("\n--- Autoboxing / Unboxing ---");
        int a = 5;
        Integer A = a;        // autoboxing
        int b = A;            // unboxing
        System.out.println("A=" + A + ", b=" + b);

        Integer x = 10;
        Integer y = 20;
        Integer z = x + y;    // unboxes x,y -> int arithmetic -> boxes result
        System.out.println("x+y=" + z);

        // Compound ops: A++ unboxes, increments, boxes back
        A++;
        System.out.println("A++ -> " + A);

        // Beware: null wrappers cannot be unboxed
    }

    // 3) Collections (generics require reference types, so wrappers are used)
    static void demoCollectionsAndGenerics() {
        System.out.println("\n--- Collections & Generics ---");
        List<Integer> list = new ArrayList<>();
        list.add(1);                  // autobox
        list.add(2);
        int first = list.get(0);      // unbox
        System.out.println("list=" + list + ", first=" + first);
    }

    // 4) Null unboxing pitfall
    static void demoNullUnboxingPitfall() {
        System.out.println("\n--- Null Unboxing Pitfall ---");
        Integer n = null;
        try {
            int m = n; // NPE
            System.out.println(m);
        } catch (NullPointerException e) {
            System.out.println("Unboxing null Integer -> NullPointerException");
        }
    }

    // 5) Equality, identity, and caching
    static void demoEqualityAndCaching() {
        System.out.println("\n--- Equality & Caching ---");
        Integer a1 = 127; // cached
        Integer a2 = 127; // cached
        System.out.println("Integer 127: a1==a2? " + (a1 == a2) + ", a1.equals(a2)? " + a1.equals(a2));

        Integer b1 = 128; // not guaranteed cached
        Integer b2 = 128;
        System.out.println("Integer 128: b1==b2? " + (b1 == b2) + ", b1.equals(b2)? " + b1.equals(b2));

        Long l1 = 127L, l2 = 127L;
        Long l3 = 128L, l4 = 128L;
        System.out.println("Long 127: l1==l2? " + (l1 == l2));
        System.out.println("Long 128: l3==l4? " + (l3 == l4));

        Boolean t1 = Boolean.TRUE, t2 = Boolean.valueOf(true);
        System.out.println("Boolean TRUE cached: t1==t2? " + (t1 == t2));

        Character c1 = 127, c2 = 127;   // likely cached
        Character c3 = 128, c4 = 128;   // likely not cached
        System.out.println("Character 127: c1==c2? " + (c1 == c2));
        System.out.println("Character 128: c3==c4? " + (c3 == c4));

        // Rule of thumb: use equals() for value equality; '==' checks identity (and relies on caching).
    }

    // 6) Comparisons and sorting
    static void demoComparisons() {
        System.out.println("\n--- Comparisons ---");
        Integer a = 10, b = 20;
        System.out.println("a.compareTo(b) = " + a.compareTo(b));
        System.out.println("Integer.compare(10, 20) = " + Integer.compare(10, 20));

        List<Integer> nums = new ArrayList<>(Arrays.asList(3, 1, 2));
        nums.sort(Integer::compare);
        System.out.println("sorted = " + nums);
    }

    // 7) Parsing and formatting (radix, decode, toString)
    static void demoParsingAndFormatting() {
        System.out.println("\n--- Parsing & Formatting ---");
        int n1 = Integer.parseInt("123");                    // base-10
        int n2 = Integer.parseInt("FF", 16);                 // hex -> 255
        int n3 = Integer.decode("0x1a");                     // 26 (also supports 0 and # prefixes)
        String s1 = Integer.toString(255, 16);               // "ff"
        String s2 = Integer.toHexString(255);                // "ff"

        System.out.println("parseInt(\"123\") = " + n1);
        System.out.println("parseInt(\"FF\",16) = " + n2);
        System.out.println("decode(\"0x1a\") = " + n3);
        System.out.println("toString(255,16) = " + s1 + ", toHexString(255) = " + s2);

        try {
            Integer.parseInt("12a"); // NumberFormatException
        } catch (NumberFormatException e) {
            System.out.println("Parsing \"12a\" throws NumberFormatException");
        }

        double d = Double.parseDouble("3.14");
        System.out.println("Double.parseDouble(\"3.14\") = " + d);
    }

    // 8) Constants and metadata fields
    static void demoConstantsAndMeta() {
        System.out.println("\n--- Constants & Meta ---");
        System.out.println("Integer.MIN_VALUE = " + Integer.MIN_VALUE);
        System.out.println("Integer.MAX_VALUE = " + Integer.MAX_VALUE);
        System.out.println("Integer.BYTES = " + Integer.BYTES);
        System.out.println("Integer.SIZE = " + Integer.SIZE);     // bits
        System.out.println("Integer.TYPE == int.class? " + (Integer.TYPE == int.class));
    }

    // 9) Unsigned operations (useful for bit-level and protocol work)
    static void demoUnsignedOps() {
        System.out.println("\n--- Unsigned Ops ---");
        // Parse max unsigned 32-bit value into a signed int representation
        int u = Integer.parseUnsignedInt("4294967295"); // 0xFFFFFFFF -> -1 as signed int
        System.out.println("u (signed) = " + u + ", toUnsignedString(u) = " + Integer.toUnsignedString(u));

        // Unsigned compare: -1 (unsigned 4294967295) > 1
        System.out.println("Integer.compareUnsigned(-1, 1) > 0 ? " + (Integer.compareUnsigned(-1, 1) > 0));

        // Unsigned divide/mod
        int q = Integer.divideUnsigned(-2, 3); // (4294967294 / 3) -> 1431655764
        int r = Integer.remainderUnsigned(-2, 3);
        System.out.println("Unsigned divide/remainder: q=" + q + ", r=" + r);

        // Long variants: parseUnsignedLong, compareUnsigned, toUnsignedString
        long lu = Long.parseUnsignedLong("18446744073709551615"); // max unsigned 64-bit -> -1L in signed
        System.out.println("Long unsigned parse -> signed lu=" + lu + ", toUnsignedString=" + Long.toUnsignedString(lu));
    }

    // 10) Overload resolution: widening > boxing > varargs (no mixed widen+box in one step)
    static void demoOverloadResolution() {
        System.out.println("\n--- Overload Resolution ---");
        m(5);                     // prefers m(long) via widening int->long over boxing to Integer
        m(Integer.valueOf(5));    // exact match m(Integer)
        m((Integer) null);        // selects m(Integer)
        m();                      // selects varargs m(int...)

        // Ambiguity example (compile-time error if uncommented):
        // overloaded(null) with overloaded(Integer) and overloaded(Long) is ambiguous.
        // overloaded(null);
        overloaded(1);            // picks Integer version via boxing (no long version present)
    }

    static void m(long x)        { System.out.println("m(long) called: " + x); }
    static void m(Integer x)     { System.out.println("m(Integer) called: " + x); }
    static void m(int... xs)     { System.out.println("m(int...) called, count=" + xs.length); }

    static void overloaded(Integer x) { System.out.println("overloaded(Integer)"); }
    static void overloaded(int x)     { System.out.println("overloaded(int)"); }

    // 11) Primitive vs boxed streams
    static void demoStreamsPrimitiveVsBoxed() {
        System.out.println("\n--- Streams: Primitive vs Boxed ---");
        int sum1 = IntStream.rangeClosed(1, 10).sum(); // no boxing
        int sum2 = Stream.of(1,2,3,4,5,6,7,8,9,10).mapToInt(Integer::intValue).sum(); // boxing present
        System.out.println("IntStream sum=" + sum1 + ", Stream<Integer> sum=" + sum2);

        // Boxing when collecting
        List<Integer> boxed = IntStream.range(0, 5).boxed().toList();
        System.out.println("boxed list = " + boxed);
    }

    // 12) Varargs with wrappers and null safety
    static void demoVarargsAndNull() {
        System.out.println("\n--- Varargs & Null ---");
        System.out.println("sum(1,2,3) = " + sum((Integer)1,2,3));
        System.out.println("sum() = " + sum());
        System.out.println("sum(1,null,3) = " + sum(1,null,3)); // handle nulls explicitly
    }

    static int sum(Integer... xs) {
        int s = 0;
        if (xs != null) {
            for (Integer v : xs) {
                if (v != null) s += v; // avoid NPE on unboxing
            }
        }
        return s;
    }

    // 13) Numeric promotions with wrappers (byte/short/char -> int)
    static void demoNumericPromotionWithWrappers() {
        System.out.println("\n--- Numeric Promotion with Wrappers ---");
        Byte B = 1, C = 2;
        // B + C -> unbox to byte -> promoted to int for arithmetic -> result int
        Byte D = (byte) (B + C); // cast back to byte, then autobox to Byte
        System.out.println("Byte D = (byte)(B + C) -> " + D);

        Character ch = 'A';
        int codePoint = ch; // unboxes char to int via promotion
        System.out.println("Character 'A' codePoint = " + codePoint);
    }

    // 14) Map counting pattern (boxing in practice)
    static void demoMapCounting() {
        System.out.println("\n--- Map Counting with Boxing ---");
        int[] data = {1,1,2,3,3,3};
        Map<Integer, Integer> freq = new HashMap<>();
        for (int k : data) {
            // getOrDefault returns Integer; unboxing to int for +1; boxing back for put
            freq.put(k, freq.getOrDefault(k, 0) + 1);
        }
        System.out.println("freq = " + freq);

        // Alternative using compute
        Map<Integer, Integer> freq2 = new HashMap<>();
        for (int k : data) {
            freq2.compute(k, (key, old) -> old == null ? 1 : old + 1);
        }
        System.out.println("freq2 = " + freq2);
    }

    // 15) Character wrapper utilities
    static void demoCharacterWrapper() {
        System.out.println("\n--- Character Wrapper ---");
        char a = 'a', d = '5', sp = ' ';
        System.out.println("isLetter('a')=" + Character.isLetter(a));
        System.out.println("isDigit('5')=" + Character.isDigit(d));
        System.out.println("isWhitespace(' ')=" + Character.isWhitespace(sp));
        System.out.println("toUpperCase('a')=" + Character.toUpperCase(a));
    }

    // 16) Boolean wrapper utilities
    static void demoBooleanWrapper() {
        System.out.println("\n--- Boolean Wrapper ---");
        Boolean bTrue = Boolean.valueOf(true);
        Boolean bParsed = Boolean.parseBoolean("TrUe"); // case-insensitive
        System.out.println("Boolean TRUE == parsed? " + (bTrue == bParsed));
        System.out.println("Boolean.toString(false) = " + Boolean.toString(false));
    }

    // 17) Arrays.asList with primitives vs wrappers
    static void demoArrayPitfall() {
        System.out.println("\n--- Arrays.asList Primitive vs Wrapper ---");
        int[] prim = {1,2,3};
        List<int[]> list1 = Arrays.asList(prim); // single element: the int[] itself
        System.out.println("Arrays.asList(int[]) size = " + list1.size());

        Integer[] boxed = {1,2,3};
        List<Integer> list2 = Arrays.asList(boxed); // three elements
        System.out.println("Arrays.asList(Integer[]) size = " + list2.size());

        // Convert primitive array to List<Integer>
        List<Integer> boxedList = Arrays.stream(prim).boxed().toList();
        System.out.println("boxedList from int[] = " + boxedList);
    }

    // 18) OptionalInt vs Optional<Integer>
    static void demoOptionalPrimitiveVsBoxed() {
        System.out.println("\n--- OptionalInt vs Optional<Integer> ---");
        OptionalInt oi = IntStream.of(1,2,3).filter(x -> x > 10).findFirst();
        System.out.println("OptionalInt present? " + oi.isPresent() + ", orElse(-1) -> " + oi.orElse(-1));

        Optional<Integer> o = Stream.of(1,2,3).filter(x -> x > 10).findFirst();
        System.out.println("Optional<Integer> present? " + o.isPresent() + ", orElse(-1) -> " + o.orElse(-1));
        // Prefer OptionalInt/Long/Double in high-throughput primitive pipelines to avoid boxing.
    }
}