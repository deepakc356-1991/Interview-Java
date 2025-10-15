package _02_08_immutability_and_defensive_copying;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * _03_InterviewQA.java
 *
 * Topic: Immutability & Defensive Copying
 * Package: _02_08_immutability_and_defensive_copying
 *
 * This file combines concise interview Q&A (basic → advanced) with runnable code snippets
 * that demonstrate key concepts: immutability, representation exposure, defensive copying,
 * shallow vs deep copy, unmodifiable vs immutable collections, records, clone vs copy,
 * equals/hashCode in immutables, builders, arrays, Date vs java.time, etc.
 *
 * How to use:
 * - Run main() to see selected demos in action and a compact Q&A dump.
 * - Browse inline comments for structured explanations and best practices.
 */
public class _03_InterviewQA {

    public static void main(String[] args) throws Exception {
        printConciseQA();

        System.out.println("\n--- DEMOS ---");

        demoDateDefensiveCopy();
        demoCollectionsCopyVsUnmodifiable();
        demoArrayDefensiveCopy();
        demoShallowVsDeepCopy();
        demoFinalNotImmutable();
        demoRecordWithDefensiveCopy();
        demoClonePitfallAndFix();
        demoImmutableEqualsHashCaching();
    }

    // ------------------------------------------------------------
    // Q&A (Basic → Intermediate → Advanced) in compact printable form
    // ------------------------------------------------------------
    private static void printConciseQA() {
        System.out.println("--- Immutability & Defensive Copying: Interview Q&A (compact) ---");

        // Basic
        say("1) What is immutability? An object whose visible state cannot change after construction.");
        say("2) How to make a class immutable?");
        say("   - Make the class final; all fields private and final; no setters;");
        say("   - Defensively copy mutable inputs; defensively copy on getters;");
        say("   - Do not expose internals; maintain class invariants in constructor.");
        say("3) Why use immutability?");
        say("   - Thread-safety without locks; simpler reasoning; safe sharing; caching;");
        say("   - Can precompute or cache hashCode; easier equals; safe publication.");
        say("4) Are String and wrapper classes immutable? Yes.");
        say("5) Does 'final' make objects immutable? No. It prevents re-assignment, not internal mutation.");
        say("6) What is defensive copying?");
        say("   - Copy incoming mutable data on write (constructor/setter);");
        say("   - Return copies (or immutable snapshots) on read (getter).");
        say("7) Why is java.util.Date problematic? It is mutable. Prefer java.time (e.g., LocalDate).");
        say("8) Unmodifiable vs immutable:");
        say("   - Collections.unmodifiableX(view) reflects source mutations (just a read-only view).");
        say("   - List.copyOf/Set.copyOf/Map.copyOf creates an unmodifiable snapshot.");
        say("9) Shallow vs deep copy: shallow copies references; deep copies nested mutable state.");
        say("10) When to return unmodifiable vs copy?");
        say("   - If you must expose a collection, keep an internal immutable snapshot and return it.");
        say("   - Or always return a fresh copy if structure is small or updates are rare.");

        // Intermediate
        say("11) How to handle mutable fields (Date, arrays, collections)?");
        say("   - Copy in; store unmodifiable snapshot; copy out on getters.");
        say("12) equals/hashCode in immutables?");
        say("   - Implement once; can cache hashCode (lazy or eager).");
        say("13) Performance trade-offs?");
        say("   - Copies cost memory/CPU; mitigate with snapshots, persistent data structures, builders.");
        say("14) Builder for immutable objects?");
        say("   - Collect values in Builder; validate; copy defensively in the constructor or build().");
        say("15) clone() vs copy constructor/factory?");
        say("   - Prefer copy constructor/factory. clone() is tricky (shallow by default).");
        say("16) Are Java records immutable?");
        say("   - Fields are final but immutability is shallow. Defensively copy mutable components in canonical constructor.");
        say("17) Are unmodifiable lists truly immutable?");
        say("   - They are unmodifiable views; they can reflect changes to the underlying list.");
        say("18) What about element mutability?");
        say("   - Immutable collections do not freeze mutable elements; consider deep immutability when needed.");
        say("19) Thread-safety of immutables?");
        say("   - Yes, safely share across threads; final fields get special publication guarantees.");
        say("20) Serialization and immutables?");
        say("   - Usually easy; ensure invariants via readResolve or custom constructors if needed.");

        // Advanced
        say("21) Breaking immutability via reflection?");
        say("   - Possible for non-trusted environments; strong encapsulation (modules) reduces risk.");
        say("22) Copy-on-write patterns?");
        say("   - Useful when reads >> writes; consider List.copyOf or CopyOnWriteArrayList carefully.");
        say("23) Persistent/functional data structures?");
        say("   - Enable cheap structural sharing; consider third-party libs or JDK records + custom trees.");
        say("24) Sensitive data?");
        say("   - Prefer char[] for passwords; avoid storing long-lived secrets in immutable objects; avoid leaking in toString().");
        say("25) Logical immutability?");
        say("   - Externally immutable while using internal caches; ensure caches don't leak mutability.");
    }

    private static void say(String s) {
        System.out.println(s);
    }

    // ------------------------------------------------------------
    // Demo 1: Defensive copying with java.util.Date vs java.time.LocalDate
    // ------------------------------------------------------------
    private static void demoDateDefensiveCopy() {
        System.out.println("\nDemo: Defensive copying for java.util.Date");

        // BAD: PersonBad exposes internal Date; caller can mutate it
        Date birth = new Date();
        PersonBad bad = new PersonBad("Alice", birth);
        Date leaked = bad.getBirthDate(); // representation exposure
        leaked.setTime(0L); // mutates inside PersonBad
        System.out.println("PersonBad mutated by external code: " + bad.getBirthDate());

        // GOOD: PersonGood copies on input and output
        Date birth2 = new Date();
        PersonGood good = new PersonGood("Bob", birth2);
        Date out = good.getBirthDate(); // copy
        out.setTime(0L); // does not affect 'good'
        System.out.println("PersonGood protected (unchanged): " + good.getBirthDate());

        // Prefer LocalDate (immutable)
        PersonWithLocalDate better = new PersonWithLocalDate("Carol", LocalDate.now());
        System.out.println("PersonWithLocalDate is inherently safe: " + better.getBirthDate());
    }

    static class PersonBad {
        private final String name;
        private final Date birthDate; // mutable

        PersonBad(String name, Date birthDate) {
            this.name = name;
            this.birthDate = birthDate; // no copy (bad)
        }

        public Date getBirthDate() {
            return birthDate; // leaks reference (bad)
        }
    }

    static final class PersonGood {
        private final String name;
        private final Date birthDate;

        PersonGood(String name, Date birthDate) {
            this.name = Objects.requireNonNull(name);
            // defensive copy IN
            this.birthDate = new Date(Objects.requireNonNull(birthDate).getTime());
        }

        public Date getBirthDate() {
            // defensive copy OUT
            return new Date(birthDate.getTime());
        }
    }

    static final class PersonWithLocalDate {
        private final String name;
        private final LocalDate birthDate; // immutable

        PersonWithLocalDate(String name, LocalDate birthDate) {
            this.name = Objects.requireNonNull(name);
            this.birthDate = Objects.requireNonNull(birthDate);
        }

        public LocalDate getBirthDate() {
            return birthDate; // safe
        }
    }

    // ------------------------------------------------------------
    // Demo 2: Collections.unmodifiableList vs List.copyOf
    // ------------------------------------------------------------
    private static void demoCollectionsCopyVsUnmodifiable() {
        System.out.println("\nDemo: Collections.unmodifiableList vs List.copyOf");
        List<String> source = new ArrayList<>(List.of("A", "B"));
        List<String> unmodifiableView = Collections.unmodifiableList(source); // view
        List<String> immutableSnapshot = List.copyOf(source); // snapshot (structure)

        source.add("C"); // mutate source
        System.out.println("unmodifiableView sees source changes: " + unmodifiableView);
        System.out.println("immutableSnapshot is stable:        " + immutableSnapshot);

        try {
            unmodifiableView.add("X");
        } catch (UnsupportedOperationException ignored) {
            System.out.println("unmodifiableView cannot be mutated via the view API");
        }
        try {
            immutableSnapshot.add("Y");
        } catch (UnsupportedOperationException ignored) {
            System.out.println("immutableSnapshot cannot be mutated at all");
        }

        // Note: If elements themselves are mutable, both can still reflect element-level mutations.
    }

    // ------------------------------------------------------------
    // Demo 3: Defensive copying for arrays (in/out)
    // ------------------------------------------------------------
    private static void demoArrayDefensiveCopy() {
        System.out.println("\nDemo: Defensive copying for arrays");

        char[] original = {'S', 'E', 'C'};
        SecretBoxBad bad = new SecretBoxBad(original);
        System.out.println("Bad snapshot before: " + bad.snapshot());
        original[0] = 'X'; // mutates inside bad
        System.out.println("Bad snapshot after external change: " + bad.snapshot());
        bad.getSecret()[1] = 'Z'; // caller mutates returned array; changes internal
        System.out.println("Bad snapshot after getter change:   " + bad.snapshot());

        char[] original2 = {'S', 'E', 'C'};
        SecretBoxGood good = new SecretBoxGood(original2);
        System.out.println("Good snapshot before: " + good.snapshot());
        original2[0] = 'X'; // no effect inside good
        System.out.println("Good snapshot after external change: " + good.snapshot());
        char[] leak = good.getSecret(); // returns a copy
        leak[1] = 'Z'; // no effect inside good
        System.out.println("Good snapshot after getter change:   " + good.snapshot());
    }

    static class SecretBoxBad {
        private final char[] secret;

        SecretBoxBad(char[] secret) {
            this.secret = secret; // no copy (bad)
        }

        public char[] getSecret() {
            return secret; // leaks (bad)
        }

        public String snapshot() {
            return new String(secret);
        }
    }

    static final class SecretBoxGood {
        private final char[] secret;

        SecretBoxGood(char[] secret) {
            this.secret = secret.clone(); // copy IN
        }

        public char[] getSecret() {
            return secret.clone(); // copy OUT
        }

        public String snapshot() {
            return new String(secret);
        }
    }

    // ------------------------------------------------------------
    // Demo 4: Shallow vs deep copy (nested mutable object)
    // ------------------------------------------------------------
    private static void demoShallowVsDeepCopy() {
        System.out.println("\nDemo: Shallow vs Deep copy of nested mutable objects");

        Address mutableAddress = new Address("NYC");
        PersonShallow shallow = new PersonShallow("Dan", mutableAddress); // stores reference
        PersonDeep deep = new PersonDeep("Eve", mutableAddress); // deep-copies

        System.out.println("Shallow city before: " + shallow.city());
        System.out.println("Deep city before:    " + deep.city());

        mutableAddress.setCity("LA"); // external mutation

        System.out.println("Shallow city after external change: " + shallow.city());
        System.out.println("Deep city after external change:    " + deep.city());
    }

    static class Address {
        private String city;

        Address(String city) { this.city = city; }
        Address(Address other) { this.city = other.city; } // copy constructor

        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
    }

    static final class PersonShallow {
        private final String name;
        private final Address address; // mutable, not copied

        PersonShallow(String name, Address address) {
            this.name = name;
            this.address = address; // shallow
        }

        String city() { return address.getCity(); }
    }

    static final class PersonDeep {
        private final String name;
        private final Address address; // deep copy

        PersonDeep(String name, Address address) {
            this.name = name;
            this.address = new Address(address); // deep
        }

        String city() { return address.getCity(); }
    }

    // ------------------------------------------------------------
    // Demo 5: 'final' does not imply immutability
    // ------------------------------------------------------------
    private static void demoFinalNotImmutable() {
        System.out.println("\nDemo: 'final' does not imply immutability");
        final List<Integer> list = new ArrayList<>();
        list.add(1); // allowed; the reference is final but the object is mutable
        System.out.println("final List still mutable: " + list);
    }

    // ------------------------------------------------------------
    // Demo 6: Java Record with defensive copying (Java 16+)
    // ------------------------------------------------------------
    private static void demoRecordWithDefensiveCopy() {
        System.out.println("\nDemo: Record with defensive copying");
        byte[] data = {1, 2, 3};
        List<String> tags = new ArrayList<>(List.of("alpha"));
        Report report = new Report(data, tags);
        // Mutate inputs after construction
        data[0] = 9;
        tags.add("beta");
        System.out.println("Report data: " + Arrays.toString(report.data()));
        System.out.println("Report tags: " + report.tags());
        try {
            report.tags().add("gamma");
        } catch (UnsupportedOperationException ignored) {
            System.out.println("Report.tags() is unmodifiable");
        }
    }

    public record Report(byte[] data, List<String> tags) {
        // Canonical constructor with defensive copies for mutable components
        public Report {
            Objects.requireNonNull(data);
            Objects.requireNonNull(tags);
            this.data = data.clone();          // copy IN
            this.tags = List.copyOf(tags);     // unmodifiable snapshot
        }
        // Accessors data() and tags() return components; both are safe (array not exposed directly)
        // If you expose array directly, callers could mutate it; keep it private or return a copy.
        public byte[] dataCopy() { return data.clone(); } // optional copy OUT
    }

    // ------------------------------------------------------------
    // Demo 7: clone() pitfalls and preferred alternatives
    // ------------------------------------------------------------
    private static void demoClonePitfallAndFix() throws CloneNotSupportedException {
        System.out.println("\nDemo: clone() shallow vs deep");

        DataHolderShallow a = new DataHolderShallow(new int[]{1, 2, 3});
        DataHolderShallow aClone = (DataHolderShallow) a.clone(); // shallow
        aClone.data()[0] = 9; // mutates original
        System.out.println("Shallow clone mutated original: " + Arrays.toString(a.data()));

        DataHolderDeep b = new DataHolderDeep(new int[]{4, 5, 6});
        DataHolderDeep bClone = (DataHolderDeep) b.clone(); // deep
        bClone.data()[0] = 8; // no effect on original
        System.out.println("Deep clone preserved original:   " + Arrays.toString(b.data()));

        // Prefer explicit copy constructor/factory:
        DataHolderCopy c = new DataHolderCopy(new int[]{7, 7, 7});
        DataHolderCopy cCopy = new DataHolderCopy(c); // explicit deep copy
        cCopy.data()[0] = 1;
        System.out.println("Copy constructor preserved original: " + Arrays.toString(c.data()));
    }

    static class DataHolderShallow implements Cloneable {
        private final int[] data;

        DataHolderShallow(int[] data) { this.data = data; }

        @Override protected Object clone() throws CloneNotSupportedException {
            return super.clone(); // shallow (bad for arrays)
        }

        int[] data() { return data; }
    }

    static class DataHolderDeep implements Cloneable {
        private final int[] data;

        DataHolderDeep(int[] data) { this.data = data.clone(); }

        @Override protected Object clone() throws CloneNotSupportedException {
            DataHolderDeep copy = (DataHolderDeep) super.clone();
            // deep copy the array
            return new DataHolderDeep(this.data); // we already copy in constructor
        }

        int[] data() { return data; }
    }

    static class DataHolderCopy {
        private final int[] data;

        DataHolderCopy(int[] data) { this.data = data.clone(); } // copy IN
        DataHolderCopy(DataHolderCopy other) { this.data = other.data.clone(); } // deep copy
        int[] data() { return data.clone(); } // copy OUT for safety
    }

    // ------------------------------------------------------------
    // Demo 8: equals/hashCode in immutable, with lazy hash caching
    // ------------------------------------------------------------
    private static void demoImmutableEqualsHashCaching() {
        System.out.println("\nDemo: equals/hashCode with lazy caching");

        ImmutablePoint p1 = new ImmutablePoint(10, 20);
        ImmutablePoint p2 = new ImmutablePoint(10, 20);
        System.out.println("p1.equals(p2): " + p1.equals(p2));
        System.out.println("p1.hashCode(): " + p1.hashCode());
        System.out.println("p2.hashCode(): " + p2.hashCode());
    }

    static final class ImmutablePoint {
        private final int x;
        private final int y;
        // cache hash lazily; use volatile for visibility across threads
        private transient volatile int hashCodeCache; // 0 means not computed

        ImmutablePoint(int x, int y) {
            this.x = x; this.y = y;
        }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ImmutablePoint that)) return false;
            return x == that.x && y == that.y;
        }

        @Override public int hashCode() {
            int h = hashCodeCache;
            if (h == 0) {
                h = 31 * Integer.hashCode(x) + Integer.hashCode(y);
                if (h == 0) h = 1; // avoid 0 sentinel
                hashCodeCache = h;
            }
            return h;
        }

        @Override public String toString() { return "Point(" + x + "," + y + ")"; }
    }

    // ------------------------------------------------------------
    // Example: Immutable class with Builder and defensive copies
    // ------------------------------------------------------------
    static final class UserProfile {
        private final String username;
        private final List<String> roles; // immutable snapshot
        private final Map<String, String> attributes; // immutable snapshot

        private UserProfile(Builder b) {
            this.username = Objects.requireNonNull(b.username);
            // snapshots; deep immutability of elements is caller's responsibility
            this.roles = List.copyOf(b.roles);
            this.attributes = Map.copyOf(b.attributes);
        }

        public String username() { return username; }
        public List<String> roles() { return roles; } // unmodifiable snapshot
        public Map<String, String> attributes() { return attributes; } // unmodifiable snapshot

        static Builder builder(String username) { return new Builder(username); }

        static final class Builder {
            private final String username;
            private final List<String> roles = new ArrayList<>();
            private final Map<String, String> attributes = new LinkedHashMap<>();

            Builder(String username) { this.username = username; }
            Builder addRole(String role) { roles.add(role); return this; }
            Builder putAttribute(String k, String v) { attributes.put(k, v); return this; }
            UserProfile build() { return new UserProfile(this); }
        }
    }

    // ------------------------------------------------------------
    // Extra notes (as comments):
    //
    // - Avoid leaking sensitive data in toString(), logs, exceptions.
    // - For passwords, prefer char[] over String to allow zeroing; but immutable objects
    //   holding secrets are risky because you cannot zero them after use.
    // - For concurrency, final fields enjoy safe publication; immutables can be shared freely.
    // - For high-throughput systems, reduce copies by:
    //   * copying at boundaries only, using List.copyOf/Map.copyOf;
    //   * using persistent data structures or copy-on-write when reads >> writes;
    //   * documenting ownership contracts to avoid redundant copies.
    // - Collections.unmodifiableX is a view, not a snapshot; List.copyOf creates a snapshot.
    // - Records provide concise final carriers; still defensively copy mutable components.
    // - clone() is error-prone; prefer copy constructors/factories; if you must clone, deep copy internals.
    // - Shallow vs deep immutability: sometimes you must recursively copy nested mutable members.
    // ------------------------------------------------------------
}