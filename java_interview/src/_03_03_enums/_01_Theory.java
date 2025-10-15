package _03_03_enums;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/*
Enums in Java - Theory and Practical Examples

Core theory:
- An enum is a special class that defines a fixed set of named constants.
- Each constant is a singleton instance created when the enum type is initialized (class loading time).
- An enum:
  - Implicitly extends java.lang.Enum<T> (so it cannot extend another class).
  - Can implement interfaces.
  - Is final for inheritance purposes (you cannot subclass it).
  - Is inherently Serializable and Comparable.
  - Provides: values(), valueOf(String), name(), ordinal(), compareTo(), toString().
- Equals vs ==: For enums, use == for equality (they are singletons).
- Ordering: Declaration order defines natural order (compareTo uses ordinal). Do not rely on ordinal for persistence or protocols.
- Switch: Enums are ideal for switch; switch expressions (Java 14+) can be exhaustive. A switch statement on a null enum throws NPE.
- Fields/constructors/methods: Enums can have them (constructors are always private). You can add behavior per constant (constant-specific class bodies).
- Performance collections: Prefer EnumSet and EnumMap over HashSet/HashMap when keys are enum types.
- Best practices:
  - Use UPPER_SNAKE_CASE for constants.
  - Don’t persist or expose ordinal; use stable codes/strings instead.
  - Consider an UNKNOWN/FALLBACK constant for forward compatibility.
  - Avoid mutable state inside enums unless you know what you’re doing (singletons are global).
  - Prefer switch expressions without default to enforce exhaustiveness (recompilation fails if new constants are added).

This file demonstrates:
- Simple enums, enums with fields/constructors/methods.
- Constant-specific behavior.
- Implementing interfaces.
- Switch statements and expressions.
- values(), valueOf(), name(), ordinal(), compareTo().
- EnumSet and EnumMap usage.
- Robust parsing and mapping patterns.
- Singleton with enum.
- Nested enums and advanced patterns.
*/
public class _01_Theory {

    public static void main(String[] args) {
        // 1) Simple enum usage
        Color c = Color.RED;
        System.out.println("Color: " + c + ", name(): " + c.name() + ", ordinal(): " + c.ordinal());
        System.out.println("CompareTo: RED vs BLUE = " + c.compareTo(Color.BLUE)); // negative since RED declared before BLUE
        System.out.println("Using == for equality: " + (c == Color.RED));

        // 2) Enums in switch (statement and expression)
        System.out.println("describeColorExhaustive (switch expression): " + describeColorExhaustive(c));
        System.out.println("describeColorWithDefault (switch statement): " + describeColorWithDefault(c));
        // Note: Passing null to a switch on enums throws NPE (handle nulls explicitly).
        System.out.println("describeColorNullSafe(null): " + describeColorNullSafe(null));

        // 3) Enum with fields, constructor, and methods + mapping
        HttpStatus status = HttpStatus.NOT_FOUND;
        System.out.println("HttpStatus: " + status + ", code=" + status.code() + ", reason=" + status.reason());
        System.out.println("HttpStatus.fromCode(200): " + HttpStatus.fromCode(200));
        System.out.println("HttpStatus.safeParse(\"404\"): " + HttpStatus.safeParse("404"));

        // 4) Constant-specific behavior (Strategy pattern)
        double x = 21, y = 7;
        for (Operation op : Operation.values()) {
            System.out.println(op + ": " + x + " ? " + y + " = " + op.apply(x, y));
        }

        // 5) Implementing interfaces with enums
        Printable p = Severity.HIGH;
        System.out.println("Printable from enum: " + p.printable());

        // 6) EnumSet (fast, memory-efficient bit-vector under the hood)
        EnumSet<Color> primary = EnumSet.of(Color.RED, Color.GREEN, Color.BLUE);
        System.out.println("EnumSet primary: " + primary);
        EnumSet<Color> warm = EnumSet.of(Color.RED);
        System.out.println("EnumSet complementOf(warm): " + EnumSet.complementOf(warm));
        System.out.println("EnumSet allOf(Color.class): " + EnumSet.allOf(Color.class));

        // 7) EnumMap (array-indexed by ordinal internally, very efficient)
        EnumMap<Color, String> colorNames = new EnumMap<>(Color.class);
        colorNames.put(Color.RED, "Red");
        colorNames.put(Color.GREEN, "Green");
        colorNames.put(Color.BLUE, "Blue");
        System.out.println("EnumMap: " + colorNames);

        // 8) values(), valueOf(String)
        for (Color color : Color.values()) {
            System.out.println("Iterating Color: " + color);
        }
        System.out.println("Color.valueOf(\"GREEN\"): " + Color.valueOf("GREEN"));

        // 9) Robust parsing (ignore case, fallback)
        System.out.println("parseColorIgnoreCase(\"blue\"): " + parseColorIgnoreCase("blue"));
        System.out.println("parseColorOrDefault(\"purple\", Color.GREEN): " + parseColorOrDefault("purple", Color.GREEN));

        // 10) Enum singleton
        AppSettings.INSTANCE.put("theme", "dark");
        System.out.println("AppSettings theme: " + AppSettings.INSTANCE.get("theme"));

        // 11) Nested enum with behavior
        TrafficLight light = new TrafficLight();
        System.out.println("TrafficLight initial: " + light.state());
        light.next();
        System.out.println("TrafficLight after next: " + light.state());

        // 12) Enum with custom stable codes (don’t use ordinal for persistence)
        System.out.println("Continent.fromCode(\"EU\"): " + Continent.fromCode("EU"));
        System.out.println("Continent.fromCode(\"XX\") fallback: " + Continent.fromCode("XX"));

        // 13) Feature flags: EnumSet vs bit mask (when interacting with external systems)
        EnumSet<FeatureFlag> flags = EnumSet.of(FeatureFlag.ALPHA, FeatureFlag.BETA);
        int mask = FeatureFlag.toBitMask(flags); // For interoperability only; don’t persist ordinal-based masks internally
        System.out.println("FeatureFlag mask: " + mask + " -> " + FeatureFlag.fromBitMask(mask));
    }

    // Switch expression (Java 14+). Exhaustive over enum constants; adding a new Color makes this method fail to compile until handled.
    static String describeColorExhaustive(Color c) {
        return switch (c) {
            case RED -> "warm";
            case GREEN, BLUE -> "cool";
        };
    }

    // Switch statement with default (good when you want a future-proof fallback).
    static String describeColorWithDefault(Color c) {
        switch (c) {
            case RED:
                return "warm";
            case GREEN:
            case BLUE:
                return "cool";
            default:
                // This also catches null (switch on null throws NPE before entering, so handle null outside if needed).
                return "unknown";
        }
    }

    // Null-safe wrapper for switches (avoid NPEs when the enum may be null).
    static String describeColorNullSafe(Color c) {
        if (c == null) return "null";
        return describeColorWithDefault(c);
    }

    // Robust parsing patterns
    static Optional<Color> parseColorIgnoreCase(String s) {
        if (s == null) return Optional.empty();
        for (Color c : Color.values()) {
            if (c.name().equalsIgnoreCase(s)) return Optional.of(c);
        }
        return Optional.empty();
    }

    static Color parseColorOrDefault(String s, Color def) {
        return parseColorIgnoreCase(s).orElse(def);
    }
}

/* 1) A simple enum: declaration order defines natural ordering and ordinal. */
enum Color {
    RED, GREEN, BLUE;
    // toString() defaults to the constant name; you can override if needed.
}

/* 2) Enum with fields, constructor, methods, and lookup maps.
   - Do not expose or rely on ordinal for persistence. Use a stable code (string/number).
   - valueOf throws IllegalArgumentException for unknown names; prefer custom parse for user input. */
enum HttpStatus {
    OK(200, "OK"),
    NOT_FOUND(404, "Not Found"),
    INTERNAL_ERROR(500, "Internal Server Error");

    private final int code;
    private final String reason;

    private static final Map<Integer, HttpStatus> BY_CODE;
    private static final Map<String, HttpStatus> BY_CODE_STRING;

    static {
        Map<Integer, HttpStatus> m1 = new HashMap<>();
        Map<String, HttpStatus> m2 = new HashMap<>();
        for (HttpStatus s : values()) {
            m1.put(s.code, s);
            m2.put(Integer.toString(s.code), s);
        }
        BY_CODE = Collections.unmodifiableMap(m1);
        BY_CODE_STRING = Collections.unmodifiableMap(m2);
    }

    HttpStatus(int code, String reason) {
        this.code = code;
        this.reason = reason;
    }

    public int code() { return code; }
    public String reason() { return reason; }

    public static Optional<HttpStatus> fromCode(int code) {
        return Optional.ofNullable(BY_CODE.get(code));
    }

    public static Optional<HttpStatus> safeParse(String codeStr) {
        if (codeStr == null) return Optional.empty();
        return Optional.ofNullable(BY_CODE_STRING.get(codeStr.trim()));
    }

    @Override
    public String toString() {
        return code + " " + reason;
    }
}

/* 3) Constant-specific behavior (Strategy via enum).
   - Each constant supplies its own implementation of the abstract method.
   - Frequently used for operations, state machines, workflows. */
enum Operation {
    PLUS("+")    { public double apply(double x, double y) { return x + y; } },
    MINUS("-")   { public double apply(double x, double y) { return x - y; } },
    TIMES("*")   { public double apply(double x, double y) { return x * y; } },
    DIVIDE("/")  { public double apply(double x, double y) { return x / y; } };

    private final String symbol;
    Operation(String symbol) { this.symbol = symbol; }
    public abstract double apply(double x, double y);
    @Override public String toString() { return symbol; }
}

/* 4) Enum implementing an interface. */
interface Printable {
    String printable();
}

enum Severity implements Printable {
    LOW, MEDIUM, HIGH;

    @Override
    public String printable() {
        return "Severity(" + name() + ")";
    }
}

/* 5) Nested enum inside a class: useful for scoping the enum where it is used. */
class TrafficLight {
    enum State {
        RED(30) {
            @Override State next() { return GREEN; }
        },
        GREEN(25) {
            @Override State next() { return YELLOW; }
        },
        YELLOW(5) {
            @Override State next() { return RED; }
        };

        private final int seconds;
        State(int seconds) { this.seconds = seconds; }
        public int seconds() { return seconds; }
        abstract State next();
    }

    private State state = State.RED;
    public State state() { return state; }
    public void next() { state = state.next(); }
}

/* 6) EnumMap/EnumSet are preferred over HashMap/HashSet for enum keys:
   - EnumMap is array-backed (fast, compact).
   - EnumSet is a bit-vector under the hood (fast set ops). */
enum Direction {
    NORTH(0, 1),
    EAST(1, 0),
    SOUTH(0, -1),
    WEST(-1, 0);

    private final int dx;
    private final int dy;
    Direction(int dx, int dy) { this.dx = dx; this.dy = dy; }
    public int dx() { return dx; }
    public int dy() { return dy; }

    public Direction rotateRight() {
        return switch (this) {
            case NORTH -> EAST;
            case EAST -> SOUTH;
            case SOUTH -> WEST;
            case WEST -> NORTH;
        };
    }

    public Direction rotateLeft() {
        return switch (this) {
            case NORTH -> WEST;
            case WEST -> SOUTH;
            case SOUTH -> EAST;
            case EAST -> NORTH;
        };
    }
}

/* 7) Stable-code pattern: never use ordinal in storage; use explicit stable codes. */
enum Continent {
    AFRICA("AF"),
    EUROPE("EU"),
    ASIA("AS"),
    NORTH_AMERICA("NA"),
    SOUTH_AMERICA("SA"),
    OCEANIA("OC"),
    ANTARCTICA("AN"),
    UNKNOWN("UN"); // Fallback for forward compatibility

    private final String code;
    private static final Map<String, Continent> BY_CODE;
    static {
        Map<String, Continent> m = new HashMap<>();
        for (Continent c : values()) {
            m.put(c.code, c);
        }
        BY_CODE = Collections.unmodifiableMap(m);
    }

    Continent(String code) { this.code = code; }
    public String code() { return code; }

    public static Continent fromCode(String code) {
        if (code == null) return UNKNOWN;
        return BY_CODE.getOrDefault(code.trim().toUpperCase(), UNKNOWN);
    }

    @Override public String toString() { return name() + "(" + code + ")"; }
}

/* 8) Enum singleton - simplest, safest singleton in Java.
   - Serialization-safe out of the box.
   - Reflection can’t create another instance.
   - Use for global configuration, registries, etc. */
enum AppSettings {
    INSTANCE;

    private final Properties props = new Properties();

    public void put(String key, String value) { props.setProperty(key, value); }
    public String get(String key) { return props.getProperty(key); }
}

/* 9) Feature flags: EnumSet is ideal. If you must interoperate with bitmasks (external), map carefully.
   - WARNING: This bitmask relies on ordinal positions. Do not persist this mask internally or across versions.
   - Prefer EnumSet internally; only produce/consume masks at the boundary if a protocol requires it. */
enum FeatureFlag {
    ALPHA, BETA, GAMMA, DELTA;

    public static int toBitMask(EnumSet<FeatureFlag> flags) {
        int mask = 0;
        for (FeatureFlag f : flags) {
            mask |= (1 << f.ordinal()); // interop only; unstable if enum changes
        }
        return mask;
    }

    public static EnumSet<FeatureFlag> fromBitMask(int mask) {
        EnumSet<FeatureFlag> set = EnumSet.noneOf(FeatureFlag.class);
        for (FeatureFlag f : values()) {
            if ((mask & (1 << f.ordinal())) != 0) {
                set.add(f);
            }
        }
        return set;
    }
}

/*
Additional notes:
- values() is a synthetic method generated by the compiler. It returns a new array each call; don’t mutate it.
- name() returns the exact identifier used in the source; toString() can be overridden (prefer toString for UI).
- Don’t use enums for open-ended, user-defined sets. Use classes/records instead.
- If you need localization, keep enum values stable and map to localized strings externally.
- For exhaustive handling, prefer switch expressions without default. Adding a new constant will fail compilation.
- Handling null in switches: switch on a null enum throws NPE; handle null before switching. (In newer Java versions with pattern matching for switch, you can use 'case null'.)
*/