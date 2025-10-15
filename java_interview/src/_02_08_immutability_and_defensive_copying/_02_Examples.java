package _02_08_immutability_and_defensive_copying;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Immutability & Defensive Copying
 *
 * This single file demonstrates:
 * - Why mutable state and representation exposure are dangerous
 * - How to make classes immutable
 * - Defensive copying in constructors, getters, and setters
 * - Arrays and collections defensive copying
 * - Shallow vs deep copy
 * - clone() pitfalls vs copy constructors/factories
 * - Prefer immutable types (e.g., java.time) over mutable ones (e.g., Date)
 */
public class _02_Examples {

    public static void main(String[] args) {
        title("1) Mutability and Representation Exposure (Points & Circles)");
        examplePointsAndCircles();

        title("2) Mutable Date vs Immutable LocalDate");
        exampleDates();

        title("3) Defensive Copying: Arrays");
        exampleArrays();

        title("4) Defensive Copying: Collections (List) & Unmodifiable views");
        exampleCollectionsShallow();

        title("5) Deep Copy Elements vs Shallow Copy");
        exampleCollectionsDeep();

        title("6) clone() Pitfall vs Copy Constructor");
        exampleClonePitfall();

        title("7) Defensive Copying in Setters and Invariants");
        exampleSetterDefensiveCopy();
    }

    private static void title(String t) {
        System.out.println();
        System.out.println("=== " + t + " ===");
    }

    // ============================================================
    // 1) Mutability and Representation Exposure (Points & Circles)
    // ============================================================

    private static void examplePointsAndCircles() {
        MutablePoint external = new MutablePoint(1, 1);
        LeakyCircle leaky = new LeakyCircle(external, 5);
        System.out.println("LeakyCircle initial center: " + leaky.getCenter());

        // External alias modification mutates circle
        external.x = 9; external.y = 9;
        System.out.println("After external point modified: " + leaky.getCenter());

        // Getter leak: caller can mutate circle through returned reference
        MutablePoint fromGetter = leaky.getCenter();
        fromGetter.x = -100;
        System.out.println("After caller mutates getter-ref: " + leaky.getCenter());

        // DefensiveCircle copies on input and output
        MutablePoint ext2 = new MutablePoint(1, 1);
        DefensiveCircle safe = new DefensiveCircle(ext2, 5);
        System.out.println("\nDefensiveCircle initial center: " + safe.getCenter());

        ext2.x = 9; ext2.y = 9; // no longer affects safe circle
        System.out.println("After external point modified: " + safe.getCenter());

        MutablePoint copyFromGetter = safe.getCenter();
        copyFromGetter.x = 42; // does not affect internal state
        System.out.println("After caller mutates getter copy: " + safe.getCenter());

        // ImmutablePoint eliminates need for defensive copies (for that field)
        ImmutablePoint ic = new ImmutablePoint(3, 3);
        ImmutableCircle icircle = new ImmutableCircle(ic, 10);
        System.out.println("\nImmutableCircle center: " + icircle.getCenter());
        ImmutableCircle moved = icircle.moveTo(icircle.getCenter().withX(99));
        System.out.println("After moving (new instance): " + moved.getCenter() + " original: " + icircle.getCenter());
    }

    static class MutablePoint {
        int x, y;
        MutablePoint(int x, int y) { this.x = x; this.y = y; }
        MutablePoint(MutablePoint other) { this(other.x, other.y); }
        @Override public String toString() { return "(" + x + "," + y + ")"; }
    }

    static class LeakyCircle {
        private MutablePoint center;
        private final int radius;

        LeakyCircle(MutablePoint center, int radius) {
            // No copy: aliases external object
            this.center = center;
            this.radius = radius;
        }

        // Dangerous: exposes internal mutable state
        public MutablePoint getCenter() { return center; }

        public int getRadius() { return radius; }
    }

    static class DefensiveCircle {
        private final MutablePoint center;
        private final int radius;

        DefensiveCircle(MutablePoint center, int radius) {
            // Defensive copy prevents external aliasing
            this.center = new MutablePoint(Objects.requireNonNull(center));
            this.radius = radius;
        }

        // Return a defensive copy to prevent callers from mutating internal state
        public MutablePoint getCenter() { return new MutablePoint(center); }

        public int getRadius() { return radius; }
    }

    static final class ImmutablePoint {
        private final int x;
        private final int y;
        ImmutablePoint(int x, int y) { this.x = x; this.y = y; }
        public int x() { return x; }
        public int y() { return y; }
        public ImmutablePoint withX(int newX) { return new ImmutablePoint(newX, this.y); }
        public ImmutablePoint withY(int newY) { return new ImmutablePoint(this.x, newY); }
        @Override public String toString() { return "(" + x + "," + y + ")"; }
    }

    static final class ImmutableCircle {
        private final ImmutablePoint center;
        private final int radius;
        ImmutableCircle(ImmutablePoint center, int radius) {
            this.center = Objects.requireNonNull(center);
            this.radius = radius;
        }
        public ImmutablePoint getCenter() { return center; } // safe (ImmutablePoint)
        public int getRadius() { return radius; }
        public ImmutableCircle moveTo(ImmutablePoint newCenter) { return new ImmutableCircle(newCenter, radius); }
    }

    // ========================================
    // 2) Mutable Date vs Immutable LocalDate
    // ========================================

    private static void exampleDates() {
        // BAD: java.util.Date is mutable, so copy defensively
        Date birth = new Date();
        BadPersonDate bad = new BadPersonDate(birth);
        System.out.println("BadPersonDate: " + bad);
        birth.setTime(0L); // external change modifies person
        System.out.println("After external date modified: " + bad);
        Date leaked = bad.getBirthDate();
        leaked.setTime(1234567890L); // mutate via getter
        System.out.println("After mutating getter-ref: " + bad);

        // GOOD: Defensive copies in ctor and getter
        GoodPersonDate good = new GoodPersonDate(new Date());
        System.out.println("\nGoodPersonDate: " + good);
        Date ext = good.getBirthDate();
        ext.setTime(0L); // has no effect
        System.out.println("After mutating getter copy: " + good);

        // BETTER: Use immutable java.time types (no need to copy)
        PersonLocalDate safer = new PersonLocalDate(LocalDate.of(2000, 1, 2));
        System.out.println("\nPersonLocalDate: " + safer);
        LocalDate d2 = safer.getBirthDate().plusDays(1); // returns a new instance
        System.out.println("After LocalDate.plusDays(1): " + safer + " (unchanged)");
    }

    static class BadPersonDate {
        private final Date birthDate; // mutable type

        BadPersonDate(Date birthDate) {
            // No copy: alias!
            this.birthDate = Objects.requireNonNull(birthDate);
        }
        public Date getBirthDate() { return birthDate; } // Dangerous: exposes mutable reference
        @Override public String toString() { return "birth=" + birthDate; }
    }

    static class GoodPersonDate {
        private final Date birthDate;

        GoodPersonDate(Date birthDate) {
            // Defensive copy: protect internal state
            this.birthDate = new Date(Objects.requireNonNull(birthDate).getTime());
        }
        public Date getBirthDate() {
            // Defensive copy on read
            return new Date(birthDate.getTime());
        }
        @Override public String toString() { return "birth=" + birthDate; }
    }

    static final class PersonLocalDate {
        private final LocalDate birthDate; // immutable type

        PersonLocalDate(LocalDate birthDate) {
            this.birthDate = Objects.requireNonNull(birthDate);
        }
        public LocalDate getBirthDate() { return birthDate; } // safe
        @Override public String toString() { return "birth=" + birthDate; }
    }

    // ===============================
    // 3) Defensive Copying: Arrays
    // ===============================

    private static void exampleArrays() {
        String[] authors = {"Alice", "Bob"};
        BadBook bad = new BadBook("Secrets", authors);
        System.out.println("BadBook: " + bad);
        authors[0] = "Mallory"; // external alias modifies book
        System.out.println("After external array modified: " + bad);
        String[] leaked = bad.getAuthors();
        leaked[1] = "Trudy"; // mutate via getter-ref
        System.out.println("After mutating getter-ref: " + bad);

        GoodBook good = new GoodBook("Secrets", authors);
        System.out.println("\nGoodBook: " + good);
        authors[0] = "Eve"; // has no effect
        System.out.println("After external array modified: " + good);
        String[] copy = good.getAuthors();
        copy[0] = "Zoe"; // has no effect
        System.out.println("After mutating getter copy: " + good);
    }

    static class BadBook {
        private final String title;
        private final String[] authors; // mutable array

        BadBook(String title, String[] authors) {
            this.title = Objects.requireNonNull(title);
            this.authors = Objects.requireNonNull(authors); // alias!
        }
        public String[] getAuthors() { return authors; } // exposes internal array
        @Override public String toString() { return title + " " + Arrays.toString(authors); }
    }

    static class GoodBook {
        private final String title;
        private final String[] authors;

        GoodBook(String title, String[] authors) {
            this.title = Objects.requireNonNull(title);
            this.authors = Arrays.copyOf(Objects.requireNonNull(authors), authors.length);
        }
        public String[] getAuthors() { return Arrays.copyOf(authors, authors.length); }
        @Override public String toString() { return title + " " + Arrays.toString(authors); }
    }

    // ===================================================
    // 4) Defensive Copying: Collections (List) & Views
    // ===================================================

    private static void exampleCollectionsShallow() {
        MutablePlayer p1 = new MutablePlayer("Alice");
        MutablePlayer p2 = new MutablePlayer("Bob");
        List<MutablePlayer> roster = new ArrayList<>(List.of(p1, p2));

        BadTeam bad = new BadTeam(roster);
        System.out.println("BadTeam (roster): " + bad);
        // External change mutates team's list
        roster.add(new MutablePlayer("Mallory"));
        System.out.println("After external list add: " + bad);
        // Caller can mutate via getter
        bad.getRoster().add(new MutablePlayer("Trudy"));
        System.out.println("After add via getter: " + bad);

        // Defensive: copy input list; expose unmodifiable view
        GoodTeamShallow good = new GoodTeamShallow(roster);
        System.out.println("\nGoodTeamShallow (roster): " + good);
        // External list mutations do not affect team
        roster.add(new MutablePlayer("Eve"));
        System.out.println("After external list add: " + good);
        // Getter returns unmodifiable list
        try {
            good.getRoster().add(new MutablePlayer("Oscar"));
        } catch (UnsupportedOperationException ex) {
            System.out.println("Add via getter rejected (unmodifiable view).");
        }

        // But elements are still shared (mutable players)
        p1.setName("HackedAlice");
        System.out.println("After mutating element externally: " + good + " (element was shared)");
    }

    static class MutablePlayer {
        private String name;
        MutablePlayer(String name) { this.name = Objects.requireNonNull(name); }
        MutablePlayer(MutablePlayer other) { this.name = other.name; }
        public String getName() { return name; }
        public void setName(String name) { this.name = Objects.requireNonNull(name); }
        @Override public String toString() { return name; }
    }

    static class BadTeam {
        private final List<MutablePlayer> roster;
        BadTeam(List<MutablePlayer> roster) { this.roster = Objects.requireNonNull(roster); } // alias!
        public List<MutablePlayer> getRoster() { return roster; } // exposes internal list
        @Override public String toString() { return "roster=" + roster; }
    }

    static class GoodTeamShallow {
        // Private, unmodifiable copy of the input list
        private final List<MutablePlayer> roster;

        GoodTeamShallow(List<MutablePlayer> roster) {
            // Copy input and freeze. Elements are still shared here.
            this.roster = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(roster)));
        }
        // Safe to expose: it's our own unmodifiable list
        public List<MutablePlayer> getRoster() { return roster; }
        @Override public String toString() { return "roster=" + roster; }
    }

    // ==========================================
    // 5) Deep Copy Elements vs Shallow Copy
    // ==========================================

    private static void exampleCollectionsDeep() {
        MutablePlayer a = new MutablePlayer("Alice");
        MutablePlayer b = new MutablePlayer("Bob");
        List<MutablePlayer> external = new ArrayList<>(List.of(a, b));

        GoodTeamDeep deep = new GoodTeamDeep(external);
        System.out.println("GoodTeamDeep: " + deep);

        // External element mutation should not affect deep-copied team
        a.setName("HackedAlice");
        System.out.println("After external element mutation: " + deep + " (unchanged)");

        // Getter returns deep immutable snapshot (copy of elements)
        List<MutablePlayer> snapshot = deep.getRoster();
        System.out.println("Snapshot from getter: " + snapshot);
        try {
            snapshot.get(0).setName("TryHacking");
        } catch (UnsupportedOperationException ex) {
            // Won't happen here because elements are copies, not unmodifiable.
            // The list itself is unmodifiable; elements are normal copies in the snapshot.
        }
        System.out.println("After mutating element in snapshot: " + deep + " (unchanged)");
        try {
            snapshot.add(new MutablePlayer("Mallory"));
        } catch (UnsupportedOperationException ex) {
            System.out.println("Add to snapshot rejected (unmodifiable list).");
        }

        // Prefer immutable elements to avoid deep copies:
        ImmutablePlayer ia = new ImmutablePlayer("Alice");
        ImmutablePlayer ib = new ImmutablePlayer("Bob");
        ImmutableTeam immutableTeam = new ImmutableTeam(List.of(ia, ib));
        System.out.println("\nImmutableTeam: " + immutableTeam);
        try {
            immutableTeam.getRoster().add(new ImmutablePlayer("Mallory"));
        } catch (UnsupportedOperationException ex) {
            System.out.println("Add to immutable team rejected.");
        }
    }

    static class GoodTeamDeep {
        private final List<MutablePlayer> roster;

        GoodTeamDeep(List<MutablePlayer> roster) {
            Objects.requireNonNull(roster);
            List<MutablePlayer> deep = new ArrayList<>(roster.size());
            for (MutablePlayer p : roster) {
                deep.add(new MutablePlayer(p)); // deep copy elements
            }
            this.roster = Collections.unmodifiableList(deep);
        }

        // Return deep, unmodifiable snapshot to avoid sharing internal objects
        public List<MutablePlayer> getRoster() {
            List<MutablePlayer> deep = new ArrayList<>(roster.size());
            for (MutablePlayer p : roster) deep.add(new MutablePlayer(p));
            return Collections.unmodifiableList(deep);
        }

        @Override public String toString() { return "roster=" + roster; }
    }

    static final class ImmutablePlayer {
        private final String name;
        ImmutablePlayer(String name) { this.name = Objects.requireNonNull(name); }
        public String name() { return name; }
        @Override public String toString() { return name; }
    }

    static class ImmutableTeam {
        // Unmodifiable copy; safe to expose directly
        private final List<ImmutablePlayer> roster;

        ImmutableTeam(List<ImmutablePlayer> roster) {
            // List.copyOf makes an unmodifiable copy since Java 10
            this.roster = List.copyOf(Objects.requireNonNull(roster));
        }
        public List<ImmutablePlayer> getRoster() { return roster; } // safe
        @Override public String toString() { return "roster=" + roster; }
    }

    // ==========================================
    // 6) clone() Pitfall vs Copy Constructor
    // ==========================================

    private static void exampleClonePitfall() {
        ShallowCloneHolder scA = new ShallowCloneHolder(new MutablePoint(0, 0));
        ShallowCloneHolder scB = scA.clone(); // shallow copy: shares 'point'
        scB.point.x = 77; // mutates shared object
        System.out.println("ShallowCloneHolder: scA.point=" + scA.point + " (changed!) scB.point=" + scB.point);

        // Prefer copy constructor or factory that performs defensive copying
        CopyableHolder c1 = new CopyableHolder(new MutablePoint(0, 0));
        CopyableHolder c2 = new CopyableHolder(c1); // deep copy of point
        // Mutate c2's internal state via method; c1 unaffected
        c2.moveTo(new MutablePoint(50, 50));
        System.out.println("CopyableHolder: c1.point=" + c1.getPoint() + " c2.point=" + c2.getPoint() + " (independent)");
    }

    static class ShallowCloneHolder implements Cloneable {
        MutablePoint point;
        ShallowCloneHolder(MutablePoint p) { this.point = Objects.requireNonNull(p); }

        @Override protected ShallowCloneHolder clone() {
            try {
                // Shallow copy—still shares 'point'
                return (ShallowCloneHolder) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new AssertionError(e);
            }
        }
    }

    static class CopyableHolder {
        private MutablePoint point;

        CopyableHolder(MutablePoint p) {
            // Defensive copy on input
            this.point = new MutablePoint(Objects.requireNonNull(p));
        }

        // Copy constructor: deep copy internal state
        CopyableHolder(CopyableHolder other) {
            this(Objects.requireNonNull(other).point);
        }

        // Mutator that defensively copies
        public void moveTo(MutablePoint newPoint) {
            this.point = new MutablePoint(Objects.requireNonNull(newPoint));
        }

        // Getter that returns a defensive copy
        public MutablePoint getPoint() { return new MutablePoint(point); }
    }

    // ===============================================
    // 7) Defensive Copying in Setters and Invariants
    // ===============================================

    private static void exampleSetterDefensiveCopy() {
        int[] bounds = {0, 10};

        LeakyRange leaky = new LeakyRange(bounds);
        System.out.println("LeakyRange: " + leaky);
        // External alias breaks invariant after construction
        bounds[1] = -5;
        System.out.println("After external array modified (invariant broken): " + leaky);

        // External alias via setter
        int[] newBounds = {5, 15};
        leaky.setBounds(newBounds);
        System.out.println("After setBounds: " + leaky);
        newBounds[0] = 999; // break invariants after validation
        System.out.println("After external modify post-setBounds: " + leaky);

        // SafeRange copies on input and output, validates invariants
        int[] safeIn = {0, 10};
        SafeRange safe = new SafeRange(safeIn);
        System.out.println("\nSafeRange: " + safe);
        safeIn[1] = -5; // no effect
        System.out.println("After external array modified: " + safe);
        int[] getterCopy = safe.getBounds();
        getterCopy[0] = -999; // no effect
        System.out.println("After mutating getter copy: " + safe);

        int[] ok = {5, 15};
        safe.setBounds(ok); // copies and validates
        System.out.println("After setBounds with ok: " + safe);
        ok[1] = -10; // no effect
        System.out.println("After mutating external after setBounds: " + safe);

        // Uncommenting the following will throw due to invalid range
        // safe.setBounds(new int[]{10, 5});
    }

    static class LeakyRange {
        // [min, max], expects min <= max
        private int[] bounds;

        LeakyRange(int[] bounds) {
            validate(Objects.requireNonNull(bounds));
            this.bounds = bounds; // alias! can be mutated externally later
        }

        public void setBounds(int[] bounds) {
            validate(Objects.requireNonNull(bounds));
            this.bounds = bounds; // alias again
        }

        public int[] getBounds() {
            return bounds; // leaks internal array
        }

        private static void validate(int[] b) {
            if (b.length != 2) throw new IllegalArgumentException("bounds length must be 2");
            if (b[0] > b[1]) throw new IllegalArgumentException("min must be <= max");
        }

        @Override public String toString() { return "bounds=" + Arrays.toString(bounds); }
    }

    static class SafeRange {
        private int[] bounds;

        SafeRange(int[] bounds) {
            setBounds(bounds); // reuse setter that copies and validates
        }

        public void setBounds(int[] bounds) {
            Objects.requireNonNull(bounds);
            if (bounds.length != 2) throw new IllegalArgumentException("bounds length must be 2");
            int min = bounds[0];
            int max = bounds[1];
            if (min > max) throw new IllegalArgumentException("min must be <= max");
            // Defensive copy after validation to avoid TOCTOU issues
            this.bounds = new int[]{min, max};
        }

        public int[] getBounds() {
            // Defensive copy on read
            return Arrays.copyOf(bounds, bounds.length);
        }

        @Override public String toString() { return "bounds=" + Arrays.toString(bounds); }
    }
}