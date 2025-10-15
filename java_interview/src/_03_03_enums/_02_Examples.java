package _03_03_enums;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

/**
 * Enums: comprehensive examples in one file.
 * - Basics: constants, values(), valueOf(), name(), ordinal(), compareTo()
 * - Fields, constructors, methods
 * - Per-constant behavior (abstract methods)
 * - switch with enums
 * - EnumSet and EnumMap
 * - Parsing helpers
 * - Implementing interfaces
 * - Strategy pattern
 * - Singleton enum
 */
public class _02_Examples {

    public static void main(String[] args) {
        basics();
        dayExample();
        switchExample(Direction.NORTH);
        operationExample();
        enumSetExample();
        enumMapExample();
        parsingExample();
        metaExample();
        strategyExample();
        singletonExample();
        planetExample();
        interfaceExample();
    }

    // --- 1) Basic enum usage -------------------------------------------------
    static void basics() {
        Direction d = Direction.WEST;
        System.out.println("Direction: " + d); // custom toString()
        for (Direction dir : Direction.values()) {
            System.out.println(dir + " ordinal=" + dir.ordinal() + " dx=" + dir.dx() + " dy=" + dir.dy());
        }
        Direction parsed = Direction.valueOf("EAST"); // exact, case-sensitive
        System.out.println("Parsed EAST -> " + parsed);
        System.out.println("NORTH < SOUTH? " + (Direction.NORTH.compareTo(Direction.SOUTH) < 0)); // order = declaration order
    }

    // --- 2) Enum with fields/constructor/methods -----------------------------
    static void dayExample() {
        System.out.println("Is Saturday weekend? " + Day.SATURDAY.isWeekend());
        System.out.println("Short name of Thursday: " + Day.THURSDAY.shortName());
    }

    // --- 3) switch with enums ------------------------------------------------
    static void switchExample(Direction d) {
        switch (d) {
            case NORTH:
                System.out.println("Heading up");
                break;
            case SOUTH:
                System.out.println("Heading down");
                break;
            case EAST:
                System.out.println("Heading right");
                break;
            case WEST:
                System.out.println("Heading left");
                break;
        }
        System.out.println("Turn left -> " + d.turnLeft());
        System.out.println("Turn right -> " + d.turnRight());
    }

    // --- 4) Per-constant behavior via abstract methods ----------------------
    static void operationExample() {
        double a = 10, b = 4;
        for (Operation op : Operation.values()) {
            System.out.println(a + " " + op + " " + b + " = " + op.apply(a, b));
        }
        System.out.println("From symbol '+' -> " + Operation.fromSymbol("+").orElse(null));
    }

    // --- 5) EnumSet: fast, memory-efficient sets for enum types -------------
    static void enumSetExample() {
        EnumSet<Direction> visited = EnumSet.noneOf(Direction.class);
        visited.add(Direction.NORTH);
        visited.add(Direction.EAST);
        System.out.println("Visited: " + visited);

        EnumSet<Direction> all = EnumSet.allOf(Direction.class);
        EnumSet<Direction> unvisited = EnumSet.complementOf(visited);
        System.out.println("All: " + all);
        System.out.println("Unvisited: " + unvisited);

        EnumSet<Direction> horizontal = EnumSet.of(Direction.EAST, Direction.WEST);
        System.out.println("Horizontal: " + horizontal);

        // range requires contiguous constants in declaration order
        EnumSet<Day> workdays = EnumSet.range(Day.MONDAY, Day.FRIDAY);
        System.out.println("Workdays: " + workdays);
    }

    // --- 6) EnumMap: efficient map keyed by enum ----------------------------
    static void enumMapExample() {
        EnumMap<Day, String> schedule = new EnumMap<>(Day.class);
        schedule.put(Day.MONDAY, "Gym");
        schedule.put(Day.WEDNESDAY, "Team meeting");
        schedule.put(Day.FRIDAY, "Movie night");
        System.out.println("Schedule: " + schedule);
        String sunday = schedule.getOrDefault(Day.SUNDAY, "Rest");
        System.out.println("Sunday plan: " + sunday);
    }

    // --- 7) Parsing helpers: safe alternatives to valueOf --------------------
    static void parsingExample() {
        System.out.println("valueOf('MONDAY'): " + Day.valueOf("MONDAY"));
        try {
            Day.valueOf("monday"); // throws
        } catch (IllegalArgumentException ex) {
            System.out.println("valueOf is case-sensitive: " + ex.getMessage());
        }
        System.out.println("fromNameIgnoreCase('monday'): " + Day.fromNameIgnoreCase("monday").orElse(null));
        System.out.println("fromShortName('Fri'): " + Day.fromShortName("Fri").orElse(null));
        System.out.println("Role from code 'a': " + Role.fromCode('a').orElse(Role.GUEST));
    }

    // --- 8) Meta info: name(), ordinal(), compareTo() -----------------------
    static void metaExample() {
        Day d = Day.THURSDAY;
        System.out.println("name=" + d.name() + ", ordinal=" + d.ordinal());
        System.out.println("compareTo(FRIDAY)=" + d.compareTo(Day.FRIDAY));
        // Note: Persist names, not ordinals (ordinals depend on declaration order).
    }

    // --- 9) Strategy pattern using enum ------------------------------------
    static void strategyExample() {
        BigDecimal price = new BigDecimal("100.00");
        for (TaxType t : TaxType.values()) {
            System.out.println(t + " -> " + t.apply(price).setScale(2, RoundingMode.HALF_UP));
        }
    }

    // --- 10) Singleton enum -------------------------------------------------
    static void singletonExample() {
        AppConfig cfg = AppConfig.INSTANCE;
        System.out.println("App env: " + cfg.get("env"));
        cfg.set("featureX", "on");
        System.out.println("featureX: " + cfg.get("featureX"));
    }

    // --- 11) Numeric example (Effective Java: Planet) -----------------------
    static void planetExample() {
        double earthWeightNewtons = 700.0; // example weight in Newtons on Earth
        double mass = earthWeightNewtons / Planet.EARTH.surfaceGravity();
        for (Planet p : Planet.values()) {
            double weight = p.surfaceWeight(mass);
            System.out.println("Weight on " + p + ": " + String.format("%.2f N", weight));
        }
    }

    // --- 12) Enums implementing interfaces ---------------------------------
    static void interfaceExample() {
        HexColor c = Color.RED;          // interface reference
        System.out.println("Color " + c + " hex=" + c.toHex());
    }

    // ------------------------------------------------------------------------
    // Enums and support types used by the examples
    // ------------------------------------------------------------------------

    // Enum with fields, methods, overridden toString, and utility methods
    static enum Direction {
        NORTH(0, 1), EAST(1, 0), SOUTH(0, -1), WEST(-1, 0);

        private final int dx;
        private final int dy;

        Direction(int dx, int dy) { this.dx = dx; this.dy = dy; }
        public int dx() { return dx; }
        public int dy() { return dy; }

        public Direction turnLeft() {
            switch (this) {
                case NORTH: return WEST;
                case WEST:  return SOUTH;
                case SOUTH: return EAST;
                case EAST:  return NORTH;
                default: throw new AssertionError(this);
            }
        }

        public Direction turnRight() {
            switch (this) {
                case NORTH: return EAST;
                case EAST:  return SOUTH;
                case SOUTH: return WEST;
                case WEST:  return NORTH;
                default: throw new AssertionError(this);
            }
        }

        @Override public String toString() {
            String n = name().toLowerCase();
            return Character.toUpperCase(n.charAt(0)) + n.substring(1);
        }
    }

    // Enum with fields/constructor; static helpers to parse
    static enum Day {
        MONDAY(false, "Mon"),
        TUESDAY(false, "Tue"),
        WEDNESDAY(false, "Wed"),
        THURSDAY(false, "Thu"),
        FRIDAY(false, "Fri"),
        SATURDAY(true, "Sat"),
        SUNDAY(true, "Sun");

        private final boolean weekend;
        private final String shortName;

        Day(boolean weekend, String shortName) {
            this.weekend = weekend;
            this.shortName = shortName;
        }

        public boolean isWeekend() { return weekend; }
        public String shortName() { return shortName; }

        public static Optional<Day> fromShortName(String s) {
            for (Day d : values()) if (d.shortName.equalsIgnoreCase(s)) return Optional.of(d);
            return Optional.empty();
        }

        public static Optional<Day> fromNameIgnoreCase(String s) {
            for (Day d : values()) if (d.name().equalsIgnoreCase(s)) return Optional.of(d);
            return Optional.empty();
        }
    }

    // Per-constant behavior: each constant overrides apply()
    static enum Operation {
        PLUS   { public double apply(double a, double b) { return a + b; } },
        MINUS  { public double apply(double a, double b) { return a - b; } },
        TIMES  { public double apply(double a, double b) { return a * b; } },
        DIVIDE { public double apply(double a, double b) { return a / b; } };

        public abstract double apply(double a, double b);

        @Override public String toString() {
            switch (this) {
                case PLUS: return "+";
                case MINUS: return "-";
                case TIMES: return "*";
                case DIVIDE: return "/";
                default: throw new AssertionError(this);
            }
        }

        public static Optional<Operation> fromSymbol(String s) {
            for (Operation op : values()) if (op.toString().equals(s)) return Optional.of(op);
            return Optional.empty();
        }
    }

    // Mapping to external codes (e.g., persisted char code)
    static enum Role {
        USER('U'), ADMIN('A'), GUEST('G');

        private final char code;
        Role(char code) { this.code = code; }
        public char code() { return code; }

        public static Optional<Role> fromCode(char c) {
            char up = Character.toUpperCase(c);
            for (Role r : values()) if (r.code == up) return Optional.of(r);
            return Optional.empty();
        }
    }

    // Strategy pattern: compute tax by type
    static enum TaxType {
        NONE {
            public BigDecimal apply(BigDecimal amount) { return amount; }
        },
        REDUCED {
            public BigDecimal apply(BigDecimal amount) { return amount.multiply(new BigDecimal("1.05")); }
        },
        STANDARD {
            public BigDecimal apply(BigDecimal amount) { return amount.multiply(new BigDecimal("1.20")); }
        };

        public abstract BigDecimal apply(BigDecimal amount);
    }

    // Singleton enum
    static enum AppConfig {
        INSTANCE;

        private final Properties props = new Properties();

        AppConfig() {
            props.setProperty("env", "dev");
        }

        public String get(String key) { return props.getProperty(key); }
        public void set(String key, String value) { props.setProperty(key, value); }
    }

    // Effective Java example
    static enum Planet {
        MERCURY(3.302e+23, 2.439e6),
        VENUS  (4.869e+24, 6.052e6),
        EARTH  (5.975e+24, 6.378e6),
        MARS   (6.419e+23, 3.393e6),
        JUPITER(1.899e+27, 7.149e7),
        SATURN (5.685e+26, 6.027e7),
        URANUS (8.683e+25, 2.556e7),
        NEPTUNE(1.024e+26, 2.477e7);

        private final double mass;           // kg
        private final double radius;         // m
        private final double surfaceGravity; // m/s^2

        private static final double G = 6.67300E-11; // universal gravitation constant

        Planet(double mass, double radius) {
            this.mass = mass;
            this.radius = radius;
            this.surfaceGravity = G * mass / (radius * radius);
        }

        public double mass() { return mass; }
        public double radius() { return radius; }
        public double surfaceGravity() { return surfaceGravity; }

        public double surfaceWeight(double otherMass) { // F = m * a
            return otherMass * surfaceGravity;
        }
    }

    // Enums can implement interfaces (polymorphism)
    interface HexColor { String toHex(); }

    static enum Color implements HexColor {
        RED("#FF0000"), GREEN("#00FF00"), BLUE("#0000FF");

        private final String hex;
        Color(String hex) { this.hex = hex; }
        public String toHex() { return hex; }

        @Override public String toString() { return name().toLowerCase(); }
    }
}