package _03_01_object_class_methods;

import java.lang.ref.Cleaner;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Object class methods examples:
 * - equals, hashCode, toString
 * - getClass
 * - clone (shallow vs deep)
 * - wait/notify/notifyAll
 * - System.identityHashCode
 * - finalize (deprecated) and alternatives (AutoCloseable, Cleaner)
 */
public class _02_Examples {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Default Object methods (toString, equals, hashCode, getClass) ===");
        demoDefaultObjectMethods();

        System.out.println("\n=== equals and hashCode override ===");
        demoEqualsAndHashCode();

        System.out.println("\n=== toString override ===");
        demoToString();

        System.out.println("\n=== getClass vs instanceof in equals ===");
        demoGetClassVsInstanceof();

        System.out.println("\n=== clone: shallow vs deep copy ===");
        demoClone();

        System.out.println("\n=== wait/notify/notifyAll (producer-consumer) ===");
        demoWaitNotify();

        System.out.println("\n=== System.identityHashCode ===");
        demoIdentityHashCode();

        System.out.println("\n=== finalize (deprecated) and safer alternatives ===");
        demoFinalizeAndCleaner();
    }

    // 1) Default Object methods demonstration
    private static void demoDefaultObjectMethods() {
        PlainThing a = new PlainThing(1, "Alpha");
        PlainThing b = new PlainThing(1, "Alpha"); // same values, different instance

        // Default toString: className@hexHash (hash from hashCode, identity-based here)
        System.out.println("a.toString(): " + a.toString());
        System.out.println("b.toString(): " + b.toString());

        // Reference equality (==) vs equals (defaults to reference equality in Object)
        System.out.println("a == a: " + (a == a));
        System.out.println("a == b: " + (a == b));
        System.out.println("a.equals(a): " + a.equals(a));
        System.out.println("a.equals(b): " + a.equals(b));

        // Default hashCode generally correlates with identity
        System.out.println("a.hashCode(): " + a.hashCode());
        System.out.println("b.hashCode(): " + b.hashCode());

        // getClass is final; returns exact runtime type
        System.out.println("a.getClass(): " + a.getClass().getName());
    }

    // 2) Proper equals and hashCode overrides
    private static void demoEqualsAndHashCode() {
        Person p1 = new Person(101, "Alice");
        Person p2 = new Person(101, "Alice Cooper"); // same id => equal by our definition

        System.out.println("p1.equals(p2): " + p1.equals(p2));
        System.out.println("p1.hashCode(): " + p1.hashCode() + ", p2.hashCode(): " + p2.hashCode());

        Set<Person> set = new HashSet<>();
        set.add(p1);
        set.add(p2); // should not add a second entry (equals true, same hash)
        System.out.println("HashSet size (should be 1): " + set.size());
    }

    // 3) Overriding toString for readability
    private static void demoToString() {
        PlainThing plain = new PlainThing(99, "X");
        Person person = new Person(202, "Bob");
        System.out.println("Default toString (PlainThing): " + plain);
        System.out.println("Overridden toString (Person): " + person);
    }

    // 4) getClass vs instanceof in equals
    private static void demoGetClassVsInstanceof() {
        // Using getClass: subclass is NEVER equal to base class object
        PointUsingGetClass p = new PointUsingGetClass(1, 2);
        PointUsingGetClass pSame = new PointUsingGetClass(1, 2);
        ColorPointUsingGetClass cp = new ColorPointUsingGetClass(1, 2, "red");
        System.out.println("getClass: p.equals(pSame): " + p.equals(pSame)); // true
        System.out.println("getClass: p.equals(cp): " + p.equals(cp));       // false
        System.out.println("getClass: cp.equals(p): " + cp.equals(p));       // false

        // Using instanceof: subclass CAN be equal if it doesn't add significant state to equality
        PointUsingInstanceof p2 = new PointUsingInstanceof(1, 2);
        ColorPointUsingInstanceof cp2 = new ColorPointUsingInstanceof(1, 2, "red");
        System.out.println("instanceof: p2.equals(cp2): " + p2.equals(cp2)); // true
        System.out.println("instanceof: cp2.equals(p2): " + cp2.equals(p2)); // true

        // getClass for runtime type introspection
        Object obj = new Person(303, "Zoe");
        System.out.println("Runtime class name via getClass(): " + obj.getClass().getName());
    }

    // 5) clone: shallow vs deep copy
    private static void demoClone() {
        User original = new User("Ann", new MutableAddress("Paris"));
        User shallow = original.clone();              // shallow copy (shares address object)
        User deep = original.deepCopy();              // deep copy (new address object)

        System.out.println("Original: " + original);
        System.out.println("Shallow: " + shallow);
        System.out.println("Deep:     " + deep);

        // Mutate shallow's address -> original changes too (shared)
        shallow.address.city = "Lyon";
        System.out.println("After shallow.address.city = \"Lyon\":");
        System.out.println("Original: " + original);  // city changed to Lyon
        System.out.println("Shallow:  " + shallow);
        System.out.println("Deep:     " + deep);      // still Paris

        // Mutate deep's address -> original unaffected
        deep.address.city = "Berlin";
        System.out.println("After deep.address.city = \"Berlin\":");
        System.out.println("Original: " + original);  // still Lyon
        System.out.println("Deep:     " + deep);      // Berlin
    }

    // 6) wait/notify/notifyAll with a simple one-slot mailbox
    private static void demoWaitNotify() throws InterruptedException {
        Mailbox box = new Mailbox();

        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= 3; i++) {
                    String msg = "msg-" + i;
                    box.put(msg);
                    System.out.println("[producer] put " + msg);
                    Thread.sleep(100);
                }
                box.put("END");
                System.out.println("[producer] put END");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "producer");

        Thread consumer = new Thread(() -> {
            try {
                while (true) {
                    String msg = box.take();
                    System.out.println("[consumer] took " + msg);
                    if ("END".equals(msg)) break;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "consumer");

        producer.start();
        consumer.start();
        producer.join();
        consumer.join();
    }

    // 7) System.identityHashCode
    private static void demoIdentityHashCode() {
        Person p = new Person(404, "Ida");
        int overridden = p.hashCode(); // equals/hashCode overridden -> based on id
        int identity = System.identityHashCode(p); // identity-based hash

        System.out.println("p.hashCode() (overridden): " + overridden);
        System.out.println("System.identityHashCode(p): " + identity + " (hex: " + Integer.toHexString(identity) + ")");

        // Default toString uses hashCode(), which may be overridden
        System.out.println("Default-like toString (using overridden hashCode): "
                + p.getClass().getName() + "@" + Integer.toHexString(p.hashCode()));
        System.out.println("Identity-style toString: "
                + p.getClass().getName() + "@" + Integer.toHexString(identity));
    }

    // 8) finalize is deprecated; use AutoCloseable or Cleaner instead
    private static void demoFinalizeAndCleaner() throws InterruptedException {
        // Deprecated finalize example (do NOT rely on this)
        @SuppressWarnings("unused")
        LegacyFinalizable lf = new LegacyFinalizable();
        lf = null;
        System.gc();
        Thread.sleep(100); // finalize may or may not run; never rely on it

        // AutoCloseable with try-with-resources
        try (CloseableResource r = new CloseableResource("try-with-resources")) {
            System.out.println("Using resource safely...");
        }

        // Cleaner-backed resource
        CleanerBackedResource leakProne = new CleanerBackedResource("resource-1 (not closed)");
        CleanerBackedResource safe = new CleanerBackedResource("resource-2");
        safe.close(); // deterministic cleanup

        // Hint GC to possibly trigger Cleaner for leakProne
        leakProne = null;
        System.gc();
        Thread.sleep(100); // Cleaner may or may not run immediately
    }

    // ----- Supporting classes -----

    // Plain class with no overrides -> inherits Object's defaults
    private static class PlainThing {
        int id;
        String name;

        PlainThing(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    // Proper equals/hashCode/toString
    private static final class Person {
        private final int id;
        private final String name;

        Person(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Person)) return false;
            Person other = (Person) o;
            return id == other.id;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(id);
        }

        @Override
        public String toString() {
            return "Person{id=" + id + ", name='" + name + "'}";
        }
    }

    // getClass-based equality (strict type match)
    private static class PointUsingGetClass {
        final int x, y;

        PointUsingGetClass(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public final boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false; // exact class
            PointUsingGetClass that = (PointUsingGetClass) o;
            return x == that.x && y == that.y;
        }

        @Override
        public final int hashCode() {
            return Objects.hash(x, y);
        }

        @Override
        public String toString() {
            return "PointUsingGetClass(" + x + "," + y + ")";
        }
    }

    private static class ColorPointUsingGetClass extends PointUsingGetClass {
        final String color;

        ColorPointUsingGetClass(int x, int y, String color) {
            super(x, y);
            this.color = color;
        }
    }

    // instanceof-based equality (allows subclass equality if subclass doesn't add to equality)
    private static class PointUsingInstanceof {
        final int x, y;

        PointUsingInstanceof(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PointUsingInstanceof)) return false;
            PointUsingInstanceof that = (PointUsingInstanceof) o;
            return x == that.x && y == that.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }

        @Override
        public String toString() {
            return "PointUsingInstanceof(" + x + "," + y + ")";
        }
    }

    private static class ColorPointUsingInstanceof extends PointUsingInstanceof {
        final String color;

        ColorPointUsingInstanceof(int x, int y, String color) {
            super(x, y);
            this.color = color;
        }
        // equals/hashCode inherited; color not part of equality (by design, to keep symmetry)
    }

    // clone example
    private static class MutableAddress {
        String city;

        MutableAddress(String city) {
            this.city = city;
        }

        @Override
        public String toString() {
            return "Address{city='" + city + "'}";
        }
    }

    private static class User implements Cloneable {
        String name;
        MutableAddress address;

        User(String name, MutableAddress address) {
            this.name = name;
            this.address = address;
        }

        @Override
        public User clone() {
            try {
                return (User) super.clone(); // shallow copy
            } catch (CloneNotSupportedException e) {
                throw new AssertionError(e);
            }
        }

        User deepCopy() {
            return new User(this.name, new MutableAddress(this.address.city));
        }

        @Override
        public String toString() {
            return "User{name='" + name + "', address=" + address + "}";
        }
    }

    // wait/notify example: simple one-slot mailbox
    private static class Mailbox {
        private String message; // null if empty

        public synchronized void put(String msg) throws InterruptedException {
            while (this.message != null) {
                wait(); // wait until slot is empty
            }
            this.message = msg;
            notifyAll(); // notify consumers
        }

        public synchronized String take() throws InterruptedException {
            while (this.message == null) {
                wait(); // wait until a message is available
            }
            String m = this.message;
            this.message = null;
            notifyAll(); // notify producers
            return m;
        }
    }

    // Deprecated finalize (do not rely on this)
    private static class LegacyFinalizable {
        @SuppressWarnings("deprecation")
        @Override
        protected void finalize() throws Throwable {
            System.out.println("LegacyFinalizable.finalize executed (deprecated!)");
        }
    }

    // AutoCloseable resource (use with try-with-resources)
    private static class CloseableResource implements AutoCloseable {
        final String name;

        CloseableResource(String name) {
            this.name = name;
        }

        @Override
        public void close() {
            System.out.println("CloseableResource.close called for " + name);
        }
    }

    // Cleaner-backed resource for fallback cleanup
    private static final Cleaner CLEANER = Cleaner.create();

    private static class CleanerBackedResource implements AutoCloseable {
        private static class State implements Runnable {
            volatile boolean closed;
            final String name;

            State(String name) {
                this.name = name;
            }

            @Override
            public void run() {
                if (!closed) {
                    System.out.println("Cleaner: cleaning " + name);
                }
            }
        }

        private final State state;
        private final Cleaner.Cleanable cleanable;

        CleanerBackedResource(String name) {
            this.state = new State(name);
            this.cleanable = CLEANER.register(this, state);
        }

        @Override
        public void close() {
            state.closed = true;
            cleanable.clean();
            System.out.println("Closed " + state.name);
        }
    }
}