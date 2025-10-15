package _02_08_immutability_and_defensive_copying;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/*
Immutability
- An immutable object’s observable state never changes after construction.
- Benefits: simpler reasoning, thread-safety by design, safe sharing/caching, stable equals/hashCode, safe publication.
- Requirements:
  - Class is final or effectively final (no overridable mutators).
  - All fields private and final.
  - No setters; no methods that mutate observable state.
  - Defensive copies for mutable inputs and outputs (arrays, collections, Date, mutable value objects).
  - Validate and establish invariants in constructor/factory.
  - Do not let this escape during construction.

Defensive Copying
- Protect your representation from external mutation and avoid exposing internal mutable state.
- Copy mutable inputs on entry.
- Return immutable/unmodifiable copies on output (or true snapshots when needed).
- Arrays are always mutable; copy on input and output.
- Collections can be wrapped (unmodifiable view) or copied to an unmodifiable collection (e.g., List.copyOf).
  - View vs Copy: unmodifiable views still reflect underlying changes; copies do not.

Shallow vs Deep Copy
- Shallow copy duplicates references; deep copy duplicates the full object graph where needed.
- Choose based on your invariants. Prefer designing with immutable components to avoid deep copies.

Records
- Records are shallowly immutable by default; their components may be mutable.
- Use defensive copies in the canonical constructor for mutable components; copy on access if needed.

Clone vs Copy Constructor/Factory
- Avoid Cloneable and clone(); prefer copy constructors or static factories that can enforce invariants and perform proper deep copies.

Safe Publication
- Immutable objects with properly constructed final fields are safely published without synchronization.
- For mutable objects, use synchronization/volatile/immutable wrappers.

Collections: unmodifiable vs immutable copy
- Collections.unmodifiableList(list) creates a read-only view backed by the original (mutations to original reflect).
- List.copyOf(list) creates an unmodifiable copy independent from the original.

This file shows patterns, pitfalls, and recommended practices with concise examples.
*/
public final class _01_Theory {
    private _01_Theory() {}

    public static void main(String[] args) {
        demos();
    }

    static void demos() {
        immutableMoneyDemo();
        periodExposureDemo();
        collectionsDefensiveCopyDemo();
        shallowVsDeepCopyDemo();
        recordDefensiveCopyDemo();
        snapshotVsViewDemo();
        copyConstructorVsCloneDemo();
        arrayDefensiveCopyDemo();
        threadSafeCacheDemo();
    }

    // Example: idiomatic immutable value object with operations returning new instances
    static final class ImmutableMoney {
        private final BigDecimal amount;   // BigDecimal is immutable
        private final Currency currency;   // Currency is effectively immutable

        public ImmutableMoney(BigDecimal amount, Currency currency) {
            Objects.requireNonNull(amount, "amount");
            Objects.requireNonNull(currency, "currency");
            if (amount.scale() > currency.getDefaultFractionDigits() && currency.getDefaultFractionDigits() >= 0) {
                // normalize scale to currency fraction digits for invariant clarity
                amount = amount.setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_UP);
            }
            if (amount.signum() < 0) throw new IllegalArgumentException("amount must be >= 0");
            this.amount = amount;
            this.currency = currency;
        }

        public BigDecimal amount() { return amount; }
        public Currency currency() { return currency; }

        public ImmutableMoney add(ImmutableMoney other) {
            requireSameCurrency(other);
            return new ImmutableMoney(this.amount.add(other.amount), this.currency);
        }

        public ImmutableMoney multiply(BigDecimal factor) {
            Objects.requireNonNull(factor, "factor");
            BigDecimal result = this.amount.multiply(factor);
            return new ImmutableMoney(result, this.currency);
        }

        private void requireSameCurrency(ImmutableMoney other) {
            if (!this.currency.equals(other.currency)) {
                throw new IllegalArgumentException("Currency mismatch");
            }
        }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ImmutableMoney m)) return false;
            return amount.compareTo(m.amount) == 0 && currency.equals(m.currency);
        }

        @Override public int hashCode() {
            // stable hash because fields never change
            return Objects.hash(amount.stripTrailingZeros(), currency);
        }

        @Override public String toString() {
            return amount + " " + currency.getCurrencyCode();
        }
    }

    private static void immutableMoneyDemo() {
        ImmutableMoney a = new ImmutableMoney(new BigDecimal("10.00"), Currency.getInstance("USD"));
        ImmutableMoney b = a.multiply(new BigDecimal("1.1"));
        // a is unchanged; b is a new instance
        System.out.println("Money: a=" + a + ", b=" + b);
    }

    // Pitfall: representation exposure with mutable inputs/outputs (Date as example).
    static final class MutablePeriodBad {
        private final Date start; // Date is mutable
        private final Date end;

        public MutablePeriodBad(Date start, Date end) {
            // No defensive copies: caller can mutate after construction
            if (start.after(end)) throw new IllegalArgumentException("start after end");
            this.start = start;
            this.end = end;
        }

        public Date start() { return start; } // Exposes internal mutable state
        public Date end() { return end; }     // Exposes internal mutable state
    }

    // Better: use immutable time API (Instant/LocalDate), no defensive copies needed
    static final class PeriodGood {
        private final Instant start;
        private final Instant end;

        public PeriodGood(Instant start, Instant end) {
            Objects.requireNonNull(start); Objects.requireNonNull(end);
            if (start.isAfter(end)) throw new IllegalArgumentException("start after end");
            this.start = start;
            this.end = end;
        }
        public Instant start() { return start; }
        public Instant end() { return end; }
    }

    // If you must accept/return mutable types, defensively copy on both input and output
    static final class PeriodWithDefensiveCopy {
        private final Date start;
        private final Date end;

        public PeriodWithDefensiveCopy(Date start, Date end) {
            this.start = new Date(Objects.requireNonNull(start).getTime()); // input copy
            this.end   = new Date(Objects.requireNonNull(end).getTime());
            if (this.start.after(this.end)) throw new IllegalArgumentException("start after end");
        }
        public Date start() { return new Date(start.getTime()); } // output copy
        public Date end()   { return new Date(end.getTime()); }
    }

    private static void periodExposureDemo() {
        Date s = new Date();
        Date e = new Date(s.getTime() + 1000);
        MutablePeriodBad bad = new MutablePeriodBad(s, e);
        // Attacking invariant: mutate original reference
        e.setTime(s.getTime() - 1000);
        System.out.println("Bad period broken: start<=end? " + !bad.start().after(bad.end()));

        PeriodWithDefensiveCopy good = new PeriodWithDefensiveCopy(s, e);
        // Attempt to mutate internal state via accessor
        good.end().setTime(Long.MIN_VALUE);
        System.out.println("Good period safe: start<=end? " + !good.start().after(good.end()));
    }

    // Pitfall: storing and exposing mutable collections without copying
    static final class MutableDocumentBad {
        private final String title;
        private final List<String> tags; // mutable

        MutableDocumentBad(String title, List<String> tags) {
            this.title = Objects.requireNonNull(title);
            this.tags = Objects.requireNonNull(tags); // no copy
        }
        public List<String> tags() { return tags; } // representation exposure
    }

    // Good: capture an unmodifiable copy; return the same (safe)
    static final class ImmutableDocument {
        private final String title;
        private final List<String> tags; // unmodifiable, independent copy

        ImmutableDocument(String title, Collection<String> tags) {
            this.title = Objects.requireNonNull(title);
            // List.copyOf:
            // - null-checks elements
            // - returns unmodifiable list
            // - makes a shallow copy (elements themselves not copied)
            this.tags = List.copyOf(tags);
        }
        public String title() { return title; }
        public List<String> tags() { return tags; } // safe to expose (unmodifiable copy)
        // If you need a snapshot per call: return new ArrayList<>(tags)
    }

    private static void collectionsDefensiveCopyDemo() {
        List<String> source = new ArrayList<>(List.of("a", "b"));
        ImmutableDocument doc = new ImmutableDocument("t", source);
        source.add("c"); // does not affect doc
        System.out.println("ImmutableDocument tags: " + doc.tags());

        // View vs copy difference:
        List<String> backing = new ArrayList<>(List.of("x"));
        List<String> view = Collections.unmodifiableList(backing); // view reflects backing changes
        List<String> copy = List.copyOf(backing); // independent snapshot
        backing.add("y");
        System.out.println("unmodifiable view reflects: " + view);
        System.out.println("copy independent: " + copy);
    }

    // Shallow vs deep copy example
    static final class Address {
        private String city; // mutable
        Address(String city) { this.city = city; }
        public String city() { return city; }
        public void setCity(String city) { this.city = city; }
    }

    static final class Person {
        private final String name;
        private final Address address; // mutable component

        Person(String name, Address address) {
            this.name = Objects.requireNonNull(name);
            this.address = Objects.requireNonNull(address);
        }
        // Shallow copy constructor (NOT safe if you intend immutability)
        Person(Person other) {
            this(other.name, other.address);
        }
        // Deep copy factory when you must isolate state:
        static Person deepCopy(Person other) {
            return new Person(other.name, new Address(other.address.city()));
        }
        public Address address() { return address; } // exposing mutable component leaks mutability
    }

    private static void shallowVsDeepCopyDemo() {
        Address a = new Address("Paris");
        Person p1 = new Person("Alice", a);
        Person p2 = new Person(p1);           // shallow copy
        Person p3 = Person.deepCopy(p1);      // deep copy

        a.setCity("London"); // affects p1 and p2 (shared Address), not p3
        System.out.println("p1=" + p1.address().city() + ", p2=" + p2.address().city() + ", p3=" + p3.address().city());
    }

    // Records: shallow immutability; use defensive copy for mutable components
    record UserRecord(String name, List<String> roles) {
        public UserRecord {
            Objects.requireNonNull(name);
            Objects.requireNonNull(roles);
            roles = List.copyOf(roles); // defensive copy, unmodifiable
        }
        // Accessor roles() returns the defensive copy stored; safe to expose
    }

    record Row(byte[] data) {
        public Row {
            Objects.requireNonNull(data);
            data = Arrays.copyOf(data, data.length); // input copy
        }
        @Override public byte[] data() {             // output copy
            return Arrays.copyOf(data, data.length);
        }
    }

    private static void recordDefensiveCopyDemo() {
        List<String> roles = new ArrayList<>(List.of("USER"));
        UserRecord u = new UserRecord("bob", roles);
        roles.add("ADMIN"); // does not affect record
        System.out.println("UserRecord roles: " + u.roles());

        byte[] raw = new byte[] {1, 2, 3};
        Row r = new Row(raw);
        raw[0] = 9; // does not affect r
        byte[] out = r.data();
        out[1] = 9; // does not affect internal state
        System.out.println("Row immutable: " + Arrays.toString(r.data()));
    }

    // Snapshot vs unmodifiable view demonstration
    static final class Children {
        private final List<String> children = new ArrayList<>();
        public void add(String c) { children.add(c); }

        // Returns a live view: read-only but reflects future mutations
        public List<String> unmodifiableView() {
            return Collections.unmodifiableList(children);
        }
        // Returns a snapshot: independent copy that won’t reflect future mutations
        public List<String> snapshot() {
            return List.copyOf(children);
        }
    }

    private static void snapshotVsViewDemo() {
        Children c = new Children();
        c.add("A");
        List<String> view = c.unmodifiableView();
        List<String> snap = c.snapshot();
        c.add("B");
        System.out.println("view reflects: " + view + ", snapshot independent: " + snap);
    }

    // Prefer copy constructors/factories to clone()
    static final class BadCloneableExample implements Cloneable {
        private final List<String> list;
        BadCloneableExample(List<String> list) { this.list = list; }
        @Override protected BadCloneableExample clone() {
            try {
                // Shallow clone: list reference shared — unsafe
                return (BadCloneableExample) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new AssertionError(e);
            }
        }
        public List<String> list() { return list; }
    }

    static final class GoodCopyExample {
        private final List<String> list;
        GoodCopyExample(Collection<String> list) { this.list = List.copyOf(list); }
        // Copy factory
        public static GoodCopyExample copyOf(GoodCopyExample other) {
            // list is already unmodifiable, but make intent explicit
            return new GoodCopyExample(other.list);
        }
        public List<String> list() { return list; }
    }

    private static void copyConstructorVsCloneDemo() {
        List<String> src = new ArrayList<>(List.of("x"));
        BadCloneableExample bad = new BadCloneableExample(src);
        BadCloneableExample copyBad = bad.clone();
        src.add("y"); // affects both
        System.out.println("Clone shared list: " + copyBad.list());

        GoodCopyExample good = new GoodCopyExample(src);
        src.add("z"); // doesn't affect good
        System.out.println("Copied list: " + good.list());
    }

    // Arrays are always mutable => copy on input and output
    static final class ImmutableByteStore {
        private final byte[] bytes;
        ImmutableByteStore(byte[] bytes) {
            this.bytes = Arrays.copyOf(Objects.requireNonNull(bytes), bytes.length);
        }
        public byte[] bytes() { return Arrays.copyOf(bytes, bytes.length); }
    }

    private static void arrayDefensiveCopyDemo() {
        byte[] b = {1,2,3};
        ImmutableByteStore s = new ImmutableByteStore(b);
        b[0] = 9; // no effect
        s.bytes()[1] = 9; // does not alter internal
        System.out.println("ImmutableByteStore: " + Arrays.toString(s.bytes()));
    }

    // Builder pattern for complex immutable objects (defensive copies in builder and at build time)
    static final class UserProfile {
        private final String id;
        private final String displayName;
        private final LocalDate birthday;
        private final Set<String> interests; // unmodifiable copy

        private UserProfile(Builder b) {
            this.id = b.id;
            this.displayName = b.displayName;
            this.birthday = b.birthday;
            this.interests = Set.copyOf(b.interests);
        }

        static final class Builder {
            private String id;
            private String displayName;
            private LocalDate birthday;
            private final Set<String> interests = new LinkedHashSet<>();

            public Builder id(String id) { this.id = Objects.requireNonNull(id); return this; }
            public Builder displayName(String name) { this.displayName = Objects.requireNonNull(name); return this; }
            public Builder birthday(LocalDate d) { this.birthday = Objects.requireNonNull(d); return this; }
            public Builder addInterest(String i) { interests.add(Objects.requireNonNull(i)); return this; }
            public UserProfile build() {
                if (id == null || displayName == null || birthday == null)
                    throw new IllegalStateException("Missing fields");
                return new UserProfile(this);
            }
        }
    }

    // Thread-safe by design with immutable snapshots and atomic reference (copy-on-write update)
    static final class ThreadSafeCache<K, V> {
        private final AtomicReference<Map<K, V>> ref = new AtomicReference<>(Map.of());

        public V get(K key) { return ref.get().get(key); }

        public void putAll(Map<K, V> updates) {
            Objects.requireNonNull(updates);
            ref.updateAndGet(current -> {
                Map<K, V> next = new LinkedHashMap<>(current);
                next.putAll(updates);
                return Collections.unmodifiableMap(next); // publish new immutable snapshot
            });
        }

        public Set<K> keys() { return ref.get().keySet(); }
    }

    private static void threadSafeCacheDemo() {
        ThreadSafeCache<String, Integer> cache = new ThreadSafeCache<>();
        cache.putAll(Map.of("a", 1));
        System.out.println("Cache keys: " + cache.keys());
    }

    // Additional notes as code comments:
    // - Strings are immutable; concatenating creates new instances; prefer StringBuilder for many appends.
    // - Wrapper classes (Integer, Long, etc.) are immutable value-based; do not synchronize or rely on identity (use equals).
    // - Precompute/caching hashCode can be beneficial for large immutable objects used as Map keys; ensure serialization invariants.
    // - Avoid letting 'this' escape during construction (e.g., registering listeners in constructor). Use factory methods or builders.
    // - Prefer immutable types in your API to reduce need for defensive copying (Instant over Date, List.copyOf over mutable lists).
    // - For performance-sensitive code, measure: copying large collections can be costly; balance with unmodifiable views when appropriate.
    // - If you must expose a view that reflects changes but remains read-only, use Collections.unmodifiableX; document semantics clearly.
    // - For deeply nested graphs, try to design with immutable components to avoid deep copy complexity.
    // - Do not use arrays as keys in hash-based collections unless you wrap them in an immutable value type with proper equals/hashCode.
}