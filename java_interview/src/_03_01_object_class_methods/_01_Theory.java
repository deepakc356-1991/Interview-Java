package _03_01_object_class_methods;

import java.util.Objects;

/*
Object (java.lang.Object) — the root of the Java class hierarchy.
- Every class implicitly extends Object (unless it explicitly extends another class).
- All reference types inherit these methods (signatures from a current LTS like Java 17/21):

Core instance methods
- public final Class<?> getClass()
- public native int hashCode()
- public boolean equals(Object obj)
- protected Object clone() throws CloneNotSupportedException
- public String toString()

Monitor (thread coordination) methods
- public final void notify()
- public final void notifyAll()
- public final void wait() throws InterruptedException
- public final void wait(long timeoutMillis) throws InterruptedException
- public final void wait(long timeoutMillis, int nanos) throws InterruptedException

Deprecated (for removal) finalization
- @Deprecated(since="9", forRemoval=true)
  protected void finalize() throws Throwable

Key theory and best practices
- getClass()
  - Final method, returns the runtime Class of the object; useful for reflection.
  - Often used in equals implementations for exact type equality checks in final classes.

- equals(Object) and hashCode()
  - Default equals is reference-identity (==). Override to implement value-based equality.
  - equals must be: reflexive, symmetric, transitive, consistent, and handle null.
  - hashCode must be consistent with equals:
    - If a.equals(b) is true, then a.hashCode() == b.hashCode().
    - If a.equals(b) is false, hash codes may still collide.
  - Never use mutable fields (that change while the object is a key in a HashMap/HashSet) in equals/hashCode.
  - For arrays, use Arrays.equals / Arrays.deepEquals (not Object.equals).
  - System.identityHashCode(x) yields an identity-based hash, independent of equals/hashCode overrides.

- toString()
  - Default is: getClass().getName() + "@" + Integer.toHexString(hashCode()).
  - Override to give human-readable debugging info. Avoid logging secrets.

- clone()
  - Protected in Object; classes must implement Cloneable and override clone() to be public to make it usable.
  - Default clone is a shallow field-by-field copy. For deep copies, you must manually clone nested state.
  - Many prefer copy constructors or factory methods over clone due to pitfalls.

- wait/notify/notifyAll()
  - Must be called while holding the monitor of the target object (inside synchronized on the same object).
  - wait() releases the monitor and suspends the thread; upon wakeup, it reacquires the monitor before returning.
  - Spurious wakeups can happen; always wait in a loop with a condition check.
  - notify() wakes one arbitrary waiting thread; notifyAll() wakes all waiting threads.
  - Prefer higher-level concurrency utilities (java.util.concurrent) over low-level wait/notify for production.

- finalize()
  - Unreliable and deprecated for removal. Do not use for resource management.
  - Use try-with-resources (AutoCloseable), Cleaner, or explicit close() instead.

- Misc
  - Methods notify/notifyAll/wait are final; equals/hashCode/toString are not final; clone/finalize are protected.
  - Equality across inheritance hierarchies is tricky. For non-final classes, consider instanceof-based equals,
    but be careful to maintain symmetry. For final classes, getClass() checks are simpler and safer.
*/

/**
 * Single-file theory + runnable demos for Object class methods.
 */
public class _01_Theory {

    public static void main(String[] args) throws Exception {
        // Default toString from Object
        System.out.println("Default toString: " + new Plain());

        // Custom toString, equals, hashCode
        User u1 = new User("u-123", "Alice");
        User u2 = new User("u-123", "Alice B.");
        User u3 = new User("u-999", "Charlie");

        System.out.println("User toString: " + u1);
        System.out.println("equals (same id): " + u1.equals(u2));     // true (value-based on id)
        System.out.println("equals (different id): " + u1.equals(u3)); // false
        System.out.println("hashCode consistent with equals: " + (u1.hashCode() == u2.hashCode()));
        System.out.println("getClass: " + u1.getClass().getName());

        // == vs equals, identityHashCode vs hashCode
        User u4 = new User("u-123", "Alice");
        System.out.println("== vs equals: " + (u1 == u4) + " vs " + u1.equals(u4));
        System.out.println("identityHashCode(u1) vs identityHashCode(u4): "
                + System.identityHashCode(u1) + " vs " + System.identityHashCode(u4));
        System.out.println("hashCode(u1) vs hashCode(u4): " + u1.hashCode() + " vs " + u4.hashCode());

        // clone() demo: deep clone of nested Address
        PersonCloneable p1 = new PersonCloneable("Bob", new Address("NYC"));
        PersonCloneable p2 = p1.clone();
        p2.name = "Bob 2";
        p2.address.city = "LA"; // deep clone ensures p1.address unaffected

        System.out.println("Clone original: " + p1);
        System.out.println("Clone copy    : " + p2);

        // wait/notify demo: simple bounded queue with producer/consumer
        BoundedQueue<Integer> q = new BoundedQueue<>(2);
        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= 5; i++) {
                    q.put(i);
                    System.out.println("Produced " + i);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "producer");

        Thread consumer = new Thread(() -> {
            try {
                for (int i = 1; i <= 5; i++) {
                    int v = q.take();
                    System.out.println("Consumed " + v);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "consumer");

        producer.start();
        consumer.start();
        producer.join();
        consumer.join();
        System.out.println("Done.");
    }

    // A class that does not override toString() -> uses Object.toString()
    static class Plain { }

    /*
    Value-based equals/hashCode best-practice example
    - final class -> equals can safely use getClass() for exact type
    - equals uses only immutable identifier field; hashCode matches equals
    - toString gives concise, safe representation
    */
    static final class User {
        private final String id;   // identity (immutable, used for equality)
        private final String name; // not used for equality

        User(String id, String name) {
            this.id = Objects.requireNonNull(id, "id");
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;              // quick path
            if (o == null || getClass() != o.getClass()) return false; // exact type
            User user = (User) o;
            return Objects.equals(this.id, user.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return "User{id='" + id + "', name='" + name + "'}";
        }
    }

    /*
    clone() best-practices
    - Implement Cloneable and override clone() to be public.
    - Call super.clone() for a shallow copy, then manually deep-clone mutable fields.
    - Many codebases prefer copy constructors/factories over clone().
    */
    static class PersonCloneable implements Cloneable {
        String name;
        Address address; // mutable nested object

        PersonCloneable(String name, Address address) {
            this.name = name;
            this.address = address;
        }

        @Override
        public PersonCloneable clone() {
            try {
                PersonCloneable copy = (PersonCloneable) super.clone(); // shallow copy
                copy.address = address.clone(); // deep copy mutable field
                return copy;
            } catch (CloneNotSupportedException e) {
                throw new AssertionError("Should be Cloneable", e);
            }
        }

        @Override
        public String toString() {
            return "PersonCloneable{name='" + name + "', address=" + address + "}";
        }
    }

    static final class Address implements Cloneable {
        String city;

        Address(String city) {
            this.city = city;
        }

        @Override
        public Address clone() {
            try {
                return (Address) super.clone(); // fields are primitives/immutable String -> shallow ok
            } catch (CloneNotSupportedException e) {
                throw new AssertionError("Should be Cloneable", e);
            }
        }

        @Override
        public String toString() {
            return "Address{city='" + city + "'}";
        }
    }

    /*
    wait/notify/notifyAll demo with intrinsic lock (object monitor)
    - All methods synchronized on the same monitor (this)
    - wait inside while loop to handle spurious wakeups
    - notifyAll used to wake both producers/consumers appropriately
    - In production, consider java.util.concurrent.ArrayBlockingQueue instead
    */
    static class BoundedQueue<T> {
        private final Object[] items;
        private int head = 0;
        private int tail = 0;
        private int count = 0;

        BoundedQueue(int capacity) {
            if (capacity <= 0) throw new IllegalArgumentException("capacity must be > 0");
            this.items = new Object[capacity];
        }

        public synchronized void put(T item) throws InterruptedException {
            while (count == items.length) {
                wait(); // releases monitor; may throw InterruptedException
            }
            items[tail] = item;
            tail = (tail + 1) % items.length;
            count++;
            notifyAll(); // wake takers
        }

        @SuppressWarnings("unchecked")
        public synchronized T take() throws InterruptedException {
            while (count == 0) {
                wait(); // releases monitor
            }
            T item = (T) items[head];
            items[head] = null;
            head = (head + 1) % items.length;
            count--;
            notifyAll(); // wake putters
            return item;
        }
    }

    /*
    Additional notes:
    - For equals in hierarchies:
      - Using getClass() prevents equality across subclasses (often desirable).
      - Using instanceof allows subclass instances to compare equal with superclass instances, but can break symmetry
        if subclasses add significant state. If you choose instanceof, consider patterns like "canEqual".
    - finalize() is deprecated for removal; do not use it. Prefer:
      - try-with-resources (AutoCloseable) for deterministic cleanup
      - java.lang.ref.Cleaner for non-critical cleanup tasks
    - Default implementations:
      - equals in Object: reference equality (this == obj)
      - hashCode in Object: typically identity-based; not specified to be address-derived
      - toString in Object: getClass().getName() + "@" + Integer.toHexString(hashCode())
    */
}