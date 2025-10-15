package _03_03_enums;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;

/*
Interview Q&A Cheat Sheet (Enums)

Basic
Q1: What is an enum in Java?
A: A special type for a fixed set of constants. Each constant is a singleton instance. Enums implicitly extend java.lang.Enum and cannot extend any other class.

Q2: Why use enums over int constants?
A: Type-safety, readability, namespace, methods/fields per constant, support in switch, EnumSet/EnumMap efficiency.

Q3: How do you declare and use enums?
A: enum Direction { NORTH, EAST, SOUTH, WEST } then Direction d = Direction.NORTH;

Q4: How to iterate all constants?
A: for (Direction d : Direction.values()) { ... } (values() is compiler-generated).

Q5: What is ordinal()? Should you use it?
A: Position index (zero-based). Do not use for persistence/business logic; it’s fragile if order changes.

Q6: Difference between name() and toString()?
A: name() is final, exact identifier; toString() can be overridden (not stable for persistence).

Q7: Can enums have fields, methods, and constructors?
A: Yes. Constructors are implicitly private and run at class init time.

Q8: Can enums implement interfaces?
A: Yes. They cannot extend classes (already extend Enum), but can implement interfaces.

Q9: Are enums Serializable and Comparable?
A: Yes (via Enum). compareTo uses ordinal order.

Q10: Can you create an enum instance via new or reflection?
A: No. Reflection-based instantiation throws an exception. Enums are singletons per constant.

Intermediate
Q11: How to use enum in switch?
A: switch(direction) { case NORTH: ... } Note: switching on null throws NullPointerException.

Q12: What are EnumSet and EnumMap?
A: Highly efficient collections for enum keys. EnumSet uses bit vectors, EnumMap uses arrays. Null keys not allowed.

Q13: How to parse enums safely?
A: Use Enum.valueOf or a safe wrapper (try-catch) or case-insensitive helper.

Q14: Constant-specific behavior?
A: Provide an abstract method and override in each constant body.

Q15: When to use EnumSet vs bit masks?
A: Prefer EnumSet for readability and safety; bit masks only when interoperating with legacy/protocols.

Q16: How are values() and valueOf implemented?
A: values() is compiler-generated and returns a new array copy. valueOf is generated as well.

Q17: How to map external codes to enum?
A: Add a field and a static lookup map; expose fromCode methods.

Advanced
Q18: Enum singleton pattern?
A: enum Singleton { INSTANCE; } It’s serialization- and reflection-safe.

Q19: Are enums thread-safe?
A: Instances are immutable by design if you only use final fields. Don’t store mutable state unless properly synchronized.

Q20: Serialization details?
A: Serialized by name; renaming a constant breaks compatibility; adding new constants is okay.

Q21: JPA persistence recommendations?
A: Use @Enumerated(EnumType.STRING). Never persist by ORDINAL.

Q22: Custom order vs ordinal?
A: Use Comparator instead of relying on ordinal() for business ordering.

Q23: Local enums? Nested enums?
A: Yes, enums can be top-level, member, or local inside a method.

Q24: equals vs == for enums?
A: Use == (safe, same semantics as equals).

Q25: Can enum constructors be public?
A: No, implicitly private. You cannot call them directly.

Q26: Can you add methods to a single enum constant?
A: Yes, via constant-specific class body.

Q27: Null handling with enums?
A: Switch on null throws NPE. Use pre-checks or defaults.

Q28: getDeclaringClass() usage?
A: Returns the enum type for the constant.

Q29: Generic bounds for enum types?
A: Use E extends Enum<E> to constrain generics.

Q30: values() array can be modified?
A: It returns a copy. Modifying it doesn’t affect the enum type.
*/
public class _03_InterviewQA {

    public static void main(String[] args) throws Exception {
        basicsDemo();
        parsingDemo();
        fieldsAndMethodsDemo();
        constantSpecificBehaviorDemo();
        enumSetAndEnumMapDemo();
        singletonEnumDemo();
        pitfallsDemo();
        genericsUtilityDemo();
        interfaceBasedEnumsDemo();
        advancedComparatorDemo();
        localEnumInsideMethodDemo();
        serializationIdentityDemo();
        nullKeysInEnumMapDemo();
        annotationUsageDemo();
    }

    // ====== BASIC EXAMPLES ======

    static void basicsDemo() {
        System.out.println("== Basics ==");
        Direction d = Direction.NORTH;
        System.out.println("Direction: name=" + d.name() + ", toString=" + d + ", ordinal=" + d.ordinal());
        for (Direction x : Direction.values()) {
            System.out.println("Iter: " + x + " (" + x.name() + "), ordinal=" + x.ordinal());
        }
        System.out.println("== vs equals: " + (d == Direction.valueOf("NORTH")) + ", " + d.equals(Direction.NORTH));
        System.out.println("getDeclaringClass: " + d.getDeclaringClass().getSimpleName());
        switch (d) {
            case NORTH -> System.out.println("Switch says: going up");
            case EAST -> System.out.println("Switch says: going right");
            case SOUTH -> System.out.println("Switch says: going down");
            case WEST -> System.out.println("Switch says: going left");
        }
        System.out.println();
    }

    // ====== PARSING ======

    static void parsingDemo() {
        System.out.println("== Parsing ==");
        System.out.println("valueOf: " + Direction.valueOf("WEST"));
        System.out.println("tryParse: " + tryParseEnum(Direction.class, "INVALID").orElse(null));
        System.out.println("parseIgnoreCase: " + parseEnumIgnoreCase(Direction.class, "south").orElse(null));
        System.out.println();
    }

    static <E extends Enum<E>> Optional<E> tryParseEnum(Class<E> type, String name) {
        if (name == null) return Optional.empty();
        try {
            return Optional.of(Enum.valueOf(type, name));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    static <E extends Enum<E>> Optional<E> parseEnumIgnoreCase(Class<E> type, String name) {
        if (name == null) return Optional.empty();
        for (E e : type.getEnumConstants()) {
            if (e.name().equalsIgnoreCase(name)) return Optional.of(e);
        }
        return Optional.empty();
    }

    // ====== FIELDS + METHODS ======

    static void fieldsAndMethodsDemo() {
        System.out.println("== Fields & Methods ==");
        Status s = Status.IN_PROGRESS;
        System.out.println("Status: " + s + ", code=" + s.code());
        System.out.println("fromCode(2) = " + Status.fromCode(2));
        System.out.println("fromCode(99) = " + Status.fromCode(99));
        System.out.println("Country from alpha2 'us' = " + Country.fromAlpha2IgnoreCase("us").orElse(null));
        System.out.println();
    }

    // ====== CONSTANT-SPECIFIC BEHAVIOR ======

    static void constantSpecificBehaviorDemo() {
        System.out.println("== Constant-specific behavior ==");
        double a = 7, b = 3;
        for (Operation op : Operation.values()) {
            System.out.println(a + " " + op.symbol() + " " + b + " = " + op.apply(a, b));
        }
        System.out.println("Operation from symbol '*': " + Operation.fromSymbol("*").orElse(null));
        System.out.println();
    }

    // ====== ENUMSET + ENUMMAP ======

    static void enumSetAndEnumMapDemo() {
        System.out.println("== EnumSet & EnumMap ==");
        EnumSet<Permission> perms = EnumSet.of(Permission.READ, Permission.WRITE);
        perms.add(Permission.EXECUTE);
        System.out.println("EnumSet: " + perms);
        int mask = Permission.toMask(perms);
        System.out.println("Bit-mask from EnumSet: " + mask + " (0b" + Integer.toBinaryString(mask) + ")");
        System.out.println("EnumSet from mask: " + Permission.fromMask(mask));
        EnumSet<Permission> all = EnumSet.allOf(Permission.class);
        System.out.println("Complement: " + EnumSet.complementOf(perms));
        System.out.println("Range (READ..EXECUTE): " + EnumSet.range(Permission.READ, Permission.EXECUTE));

        EnumMap<Direction, Integer> visits = new EnumMap<>(Direction.class);
        for (Direction d : Direction.values()) visits.put(d, 0);
        visits.put(Direction.NORTH, visits.get(Direction.NORTH) + 1);
        System.out.println("EnumMap: " + visits);
        System.out.println();
    }

    // ====== SINGLETON ======

    static void singletonEnumDemo() {
        System.out.println("== Singleton enum ==");
        AppConfig cfg = AppConfig.INSTANCE;
        System.out.println("env=" + cfg.get("env") + ", featureX.enabled=" + cfg.get("featureX.enabled"));
        System.out.println();
    }

    // ====== PITFALLS ======

    static void pitfallsDemo() {
        System.out.println("== Pitfalls ==");
        Direction maybeNull = null;
        try {
            switch (maybeNull) {
                case NORTH -> System.out.println("won't reach");
                default -> System.out.println("won't reach");
            }
        } catch (NullPointerException npe) {
            System.out.println("Switch on null throws NPE");
        }
        // values() returns a fresh array copy
        Direction[] arr = Direction.values();
        Arrays.sort(arr, Comparator.reverseOrder());
        System.out.println("Sorted copy (reverse): " + Arrays.toString(arr));
        System.out.println("Original order still: " + Arrays.toString(Direction.values()));

        int ord = Direction.WEST.ordinal();
        System.out.println("Stored ordinal=" + ord + " -> Direction.values()[ord]=" + Direction.values()[ord] + " (fragile if order changes)");
        System.out.println("name() stable=" + Direction.WEST.name() + ", toString() friendly=" + Direction.WEST);
        System.out.println();
    }

    // ====== GENERIC UTILITIES ======

    static void genericsUtilityDemo() {
        System.out.println("== Generic utilities ==");
        Direction d = parseOrDefaultIgnoreCase(Direction.class, "east", Direction.NORTH);
        System.out.println("parseOrDefaultIgnoreCase('east')=" + d);
        Status st = parseOrDefault(Status.class, "INVALID", Status.NEW);
        System.out.println("parseOrDefault('INVALID', NEW)=" + st);
        System.out.println();
    }

    static <E extends Enum<E>> E parseOrDefault(Class<E> type, String name, E defaultVal) {
        try {
            return name == null ? defaultVal : Enum.valueOf(type, name);
        } catch (IllegalArgumentException ex) {
            return defaultVal;
        }
    }

    static <E extends Enum<E>> E parseOrDefaultIgnoreCase(Class<E> type, String name, E defaultVal) {
        if (name == null) return defaultVal;
        for (E e : type.getEnumConstants()) {
            if (e.name().equalsIgnoreCase(name)) return e;
        }
        return defaultVal;
    }

    // ====== INTERFACE-BASED ENUMS ======

    static void interfaceBasedEnumsDemo() {
        System.out.println("== Interface-based enums ==");
        double a = 2, b = 5;
        System.out.println("BasicOp.ADD: " + BasicOp.ADD.apply(a, b));
        System.out.println("ScientificOp.POW: " + ScientificOp.POW.apply(a, b));
        System.out.println();
    }

    interface BinaryOp {
        double apply(double a, double b);
    }

    enum BasicOp implements BinaryOp {
        ADD { public double apply(double a, double b) { return a + b; } },
        MUL { public double apply(double a, double b) { return a * b; } }
    }

    enum ScientificOp implements BinaryOp {
        POW { public double apply(double a, double b) { return Math.pow(a, b); } }
    }

    // ====== CUSTOM ORDERING ======

    static void advancedComparatorDemo() {
        System.out.println("== Custom ordering ==");
        Status[] arr = {Status.DONE, Status.NEW, Status.FAILED, Status.IN_PROGRESS};
        Arrays.sort(arr, Comparator.comparingInt(Status::code)); // by code
        System.out.println("Sorted by code: " + Arrays.toString(arr));
        System.out.println();
    }

    // ====== LOCAL ENUM ======

    static void localEnumInsideMethodDemo() {
        System.out.println("== Local enum ==");
        enum Light { RED, YELLOW, GREEN }
        Light l = Light.RED;
        System.out.println("Local enum value: " + l);
        System.out.println();
    }

    // ====== SERIALIZATION & REFLECTION SAFETY ======

    static void serializationIdentityDemo() throws Exception {
        System.out.println("== Serialization & reflection ==");
        Operation op = Operation.ADD;
        Operation op2 = roundTrip(op);
        System.out.println("Round-trip same reference: " + (op == op2));

        try {
            Constructor<Operation> c = Operation.class.getDeclaredConstructor(String.class, int.class, String.class);
            c.setAccessible(true);
            c.newInstance("FAKE", 999, "%");
        } catch (Throwable t) {
            System.out.println("Cannot create enum via reflection: " + t.getClass().getSimpleName());
        }
        System.out.println();
    }

    static <T> T roundTrip(T obj) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) { oos.writeObject(obj); }
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
            @SuppressWarnings("unchecked")
            T x = (T) ois.readObject();
            return x;
        }
    }

    // ====== ENUMMAP NULL KEY ======

    static void nullKeysInEnumMapDemo() {
        System.out.println("== EnumMap null key ==");
        EnumMap<Direction, String> m = new EnumMap<>(Direction.class);
        try {
            m.put(null, "x");
        } catch (NullPointerException npe) {
            System.out.println("EnumMap does not allow null keys");
        }
        System.out.println();
    }

    // ====== ANNOTATIONS WITH ENUMS ======

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    @interface Alert {
        Level level();
    }

    enum Level { LOW, MEDIUM, HIGH }

    @Alert(level = Level.HIGH)
    static void annotatedMethod() {}

    static void annotationUsageDemo() throws Exception {
        System.out.println("== Annotation & enum ==");
        Alert a = _03_InterviewQA.class.getDeclaredMethod("annotatedMethod").getAnnotation(Alert.class);
        System.out.println("Annotation level: " + (a != null ? a.level() : null));
        System.out.println();
    }

    // ====== EXAMPLE ENUM TYPES ======

    enum Direction {
        NORTH("North"), EAST("East"), SOUTH("South"), WEST("West");

        private final String label;

        Direction(String label) { this.label = label; }

        @Override public String toString() { return label; }

        public Direction opposite() {
            return switch (this) {
                case NORTH -> SOUTH;
                case SOUTH -> NORTH;
                case EAST -> WEST;
                case WEST -> EAST;
            };
        }
    }

    enum Status {
        NEW(0, "New"),
        IN_PROGRESS(1, "In progress"),
        DONE(2, "Done"),
        FAILED(3, "Failed");

        private final int code;
        private final String description;

        Status(int code, String description) {
            this.code = code;
            this.description = description;
        }

        public int code() { return code; }
        public String description() { return description; }

        public static Status fromCode(int code) {
            for (Status s : values()) if (s.code == code) return s;
            return null;
        }

        @Override public String toString() {
            return name() + "(" + code + ":" + description + ")";
        }
    }

    enum Operation {
        ADD("+") { public double apply(double a, double b) { return a + b; } },
        SUB("-") { public double apply(double a, double b) { return a - b; } },
        MUL("*") { public double apply(double a, double b) { return a * b; } },
        DIV("/") { public double apply(double a, double b) { return a / b; } };

        private final String symbol;

        Operation(String symbol) { this.symbol = symbol; }

        public String symbol() { return symbol; }

        public abstract double apply(double a, double b);

        public static Optional<Operation> fromSymbol(String s) {
            for (Operation op : values()) if (op.symbol.equals(s)) return Optional.of(op);
            return Optional.empty();
        }
    }

    enum Permission {
        READ(1), WRITE(1 << 1), EXECUTE(1 << 2), DELETE(1 << 3);

        private final int mask;

        Permission(int mask) { this.mask = mask; }

        public int mask() { return mask; }

        public static int toMask(EnumSet<Permission> set) {
            int m = 0;
            for (Permission p : set) m |= p.mask;
            return m;
        }

        public static EnumSet<Permission> fromMask(int mask) {
            EnumSet<Permission> set = EnumSet.noneOf(Permission.class);
            for (Permission p : values()) if ((mask & p.mask) != 0) set.add(p);
            return set;
        }
    }

    enum Country {
        US("US", "United States"),
        IN("IN", "India"),
        DE("DE", "Germany");

        private final String alpha2;
        private final String display;

        Country(String alpha2, String display) {
            this.alpha2 = alpha2;
            this.display = display;
        }

        public String alpha2() { return alpha2; }
        public String display() { return display; }

        public static Optional<Country> fromAlpha2IgnoreCase(String code) {
            if (code == null) return Optional.empty();
            String c = code.toUpperCase();
            for (Country x : values()) if (x.alpha2.equals(c)) return Optional.of(x);
            return Optional.empty();
        }

        @Override public String toString() { return display + "(" + alpha2 + ")"; }
    }

    enum AppConfig {
        INSTANCE;

        private final java.util.Properties props = new java.util.Properties();

        AppConfig() {
            // Demo defaults
            props.setProperty("env", System.getProperty("env", "dev"));
            props.setProperty("featureX.enabled", "true");
        }

        public String get(String key) { return props.getProperty(key); }
    }
}