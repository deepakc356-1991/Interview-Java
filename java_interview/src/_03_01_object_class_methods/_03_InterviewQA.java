package _03_01_object_class_methods;

import java.lang.ref.Cleaner;
import java.util.*;

/*
Java Object Class Methods - Interview Q&A (Basic → Advanced)

BASICS
1) What is java.lang.Object?
   - The root of the class hierarchy. Every class has Object as a superclass.

2) What are the methods of Object?
   - equals(Object), hashCode(), toString(), getClass(), clone(), wait(), wait(long), wait(long,int), notify(), notifyAll(), finalize() [deprecated].

3) Which Object methods are final?
   - getClass(), wait(), wait(long), wait(long,int), notify(), notifyAll() are final. (equals, hashCode, toString, clone, finalize are not final.)

4) Which are native?
   - getClass, hashCode, clone, wait, notify, notifyAll are native. equals and toString are not native.

5) Default behavior:
   - equals: reference equality (o1 == o2)
   - hashCode: typically derived from identity; not specified as memory address
   - toString: getClass().getName() + "@" + Integer.toHexString(hashCode())
   - clone: protected; shallow copy; throws CloneNotSupportedException unless Cloneable is implemented
   - getClass: runtime class object; final
   - wait/notify/notifyAll: thread coordination on monitors

EQUALS / HASHCODE
6) == vs equals?
   - == compares references for objects, values for primitives; equals compares logical equality.

7) equals contract?
   - Reflexive, symmetric, transitive, consistent, and x.equals(null) must be false.

8) hashCode contract?
   - If a.equals(b) then a.hashCode() == b.hashCode(). Must be consistent within an execution. Unequal objects may share hash codes.

9) Why override both equals and hashCode?
   - Collections like HashMap/HashSet rely on both. Overriding one without the other breaks collection behavior.

10) Common pitfalls:
   - Using mutable fields in equals/hashCode (breaks hashing if mutated after insertion).
   - Using Arrays.equals incorrectly (arrays inherit Object.equals; use Arrays.equals/Arrays.deepEquals).
   - Floating point comparison (NaN, -0.0). Consider comparing via Double.compare/Float.compare.

11) Best practices for equals:
   - Start with if (this == o) return true;
   - Use getClass() for exact type equality or instanceof with a well-documented hierarchy strategy.
   - Use Objects.equals for null-safety; Arrays.equals for arrays.

12) Best practices for hashCode:
   - Combine significant fields. Use Objects.hash for simplicity (beware minor allocation), or manual combination for performance.
   - Avoid mutable fields or ensure immutability/discipline when used as map keys.

13) getClass vs instanceof in equals?
   - getClass enforces exact type equality (safer for non-inheritable value types).
   - instanceof permits subclass equality but can break symmetry/transitivity if subclasses add fields and redefine equals.

TOSTRING
14) toString guidance?
   - Return concise, useful info for debugging. Avoid sensitive data. Stable format if used in logs.

CLONE / COPYING
15) clone basics?
   - Protected in Object; must override (often public) and implement Cloneable or else super.clone throws CloneNotSupportedException.
   - super.clone does a shallow copy.

16) Shallow vs Deep copy?
   - Shallow: copies references; shared mutable objects can cause aliasing.
   - Deep: recursively copies contained mutable objects.

17) Why is clone considered tricky?
   - Shallow by default, doesn’t handle deep graphs, final fields, checked exception, confusing semantics. Prefer copy constructors/factory methods, records, or builders.

FINALIZE / CLEANUP
18) finalize status?
   - Deprecated (for removal). Unpredictable, performance impact, no timing guarantees. Do not use.

19) Alternatives to finalize?
   - AutoCloseable with try-with-resources, Cleaner/PhantomReference for last-resort cleanup, explicit close().

WAIT/NOTIFY
20) wait/notify basics?
   - Must hold the monitor (synchronized on the same object) or IllegalMonitorStateException.
   - wait releases the monitor and suspends until notify/notifyAll/interrupt/spurious wakeup, then re-acquires before returning.
   - Always use a loop (while) to recheck condition.

21) wait vs sleep?
   - wait releases the monitor; sleep does not. wait must be in synchronized context; sleep does not require it.

22) notify vs notifyAll?
   - notify wakes one waiter; notifyAll wakes all waiters. Prefer notifyAll unless you can prove a single-condition single-waiter scenario to avoid missed signals/livelocks.

23) Spurious wakeups?
   - They can happen; always guard with a loop checking the condition.

MISC
24) Are Object methods thread-safe?
   - Monitor methods (wait/notify*) are; others like equals/hashCode/toString are as safe as your implementations.

25) getClass()
   - Final; returns runtime class. Cannot be overridden.

26) Arrays behavior
   - Arrays inherit equals/hashCode from Object; use Arrays.equals, Arrays.deepEquals, Arrays.hashCode, Arrays.deepHashCode.

27) Identity vs logical hashing
   - System.identityHashCode(o) returns identity-based hash ignoring overrides; IdentityHashMap uses == and identity hash.

28) Records
   - Java records auto-generate equals/hashCode/toString based on state components; ideal for immutable value types.

29) Null-safe equals
   - Objects.equals(a, b) avoids NPE.

30) Hash-based collections and mutation
   - Never mutate fields that participate in equals/hashCode while the object is a key in a hash-based collection.

The code below demonstrates the key concepts with concise examples.
*/
public class _03_InterviewQA {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Object: identity vs equals ===");
        demoIdentityVsEquality();

        System.out.println("\n=== equals/hashCode contract with HashSet (mutation pitfall) ===");
        demoHashCodeContractMutationPitfall();

        System.out.println("\n=== Arrays equals/hashCode vs Objects ===");
        demoArraysEquals();

        System.out.println("\n=== toString default vs overridden ===");
        demoToString();

        System.out.println("\n=== equals: getClass vs instanceof (symmetry/transitivity) ===");
        demoGetClassVsInstanceofEquals();

        System.out.println("\n=== clone: shallow vs deep ===");
        demoCloneShallowVsDeep();

        System.out.println("\n=== clone without Cloneable (exception) and with Cloneable ===");
        demoCloneWithoutAndWithCloneable();

        System.out.println("\n=== wait/notify with monitor (bounded buffer) ===");
        demoMonitorWaitNotify();

        System.out.println("\n=== IllegalMonitorStateException when calling wait without lock ===");
        demoIllegalMonitorState();

        System.out.println("\n=== finalize alternative: Cleaner and AutoCloseable ===");
        demoCleanerAlternative();

        System.out.println("\nAll demos finished.");
    }

    // === Demo 1: identity vs equals ===
    private static void demoIdentityVsEquality() {
        String a = new String("hello");
        String b = new String("hello");
        System.out.println("a == b? " + (a == b));                 // false (different objects)
        System.out.println("a.equals(b)? " + a.equals(b));          // true (logical equality)

        Object o1 = new Object();
        Object o2 = new Object();
        System.out.println("o1.equals(o2)? " + o1.equals(o2));      // false (default is reference equality)
        System.out.println("o1.toString(): " + o1.toString());      // ClassName@hexHash
        System.out.println("o1.getClass(): " + o1.getClass().getName()); // java.lang.Object
    }

    // === Demo 2: hashCode/equals mutation pitfall ===
    private static void demoHashCodeContractMutationPitfall() {
        Set<PersonMutable> set = new HashSet<>();
        PersonMutable p = new PersonMutable("Bob", 30);
        set.add(p);

        System.out.println("Set contains p before mutation? " + set.contains(p));
        // Mutate a field that participates in equals/hashCode
        p.setName("Robert");
        System.out.println("Set contains p after mutation? " + set.contains(p)); // May be false
        System.out.println("Set contains logically equal new PersonMutable(\"Robert\", 30)? "
                + set.contains(new PersonMutable("Robert", 30))); // Likely false
        System.out.println("HashSet size (object still there but 'lost'): " + set.size());
    }

    // === Demo 3: Arrays equals/hashCode ===
    private static void demoArraysEquals() {
        int[] x = {1, 2, 3};
        int[] y = {1, 2, 3};

        System.out.println("x.equals(y)? " + x.equals(y)); // false; Object.equals
        System.out.println("Arrays.equals(x, y)? " + Arrays.equals(x, y)); // true

        System.out.println("x.hashCode(): " + x.hashCode()); // identity-based
        System.out.println("Arrays.hashCode(x): " + Arrays.hashCode(x)); // element-based
    }

    // === Demo 4: toString default vs overridden ===
    private static void demoToString() {
        Object o = new Object();
        System.out.println("Default toString: " + o.toString());
        PersonToString pt = new PersonToString("Alice", 28);
        System.out.println("Overridden toString: " + pt);
    }

    // === Demo 5: equals: getClass vs instanceof ===
    private static void demoGetClassVsInstanceofEquals() {
        // instanceof approach can break symmetry/transitivity if subclasses add fields
        PersonBase p = new PersonBase("Alex");
        EmployeeSub e = new EmployeeSub("Alex", 1001);

        System.out.println("PersonBase(instanceof) equals EmployeeSub? p.equals(e): " + p.equals(e)); // true
        System.out.println("EmployeeSub(instanceof) equals PersonBase? e.equals(p): " + e.equals(p)); // false -> symmetry broken

        // getClass approach avoids cross-type equality
        PersonExact p2 = new PersonExact("Alex");
        EmployeeExact e2 = new EmployeeExact("Alex", 1001);

        System.out.println("PersonExact(getClass) equals EmployeeExact? p2.equals(e2): " + p2.equals(e2)); // false
        System.out.println("EmployeeExact(getClass) equals PersonExact? e2.equals(p2): " + e2.equals(p2)); // false
    }

    // === Demo 6: clone shallow vs deep ===
    private static void demoCloneShallowVsDeep() throws CloneNotSupportedException {
        // Shallow copy: nested mutable Address is shared
        Address a1 = new Address("NYC");
        PersonShallow ps1 = new PersonShallow("Carol", a1);
        PersonShallow ps2 = ps1.clone(); // shallow
        ps2.address.city = "LA";
        System.out.println("Shallow clone: original address city changed? " + ps1.address.city); // LA -> shared reference

        // Deep copy: nested mutable AddressDeep is copied
        AddressDeep ad1 = new AddressDeep("Berlin");
        PersonDeep pd1 = new PersonDeep("Dave", ad1);
        PersonDeep pd2 = pd1.clone(); // deep
        pd2.address.city = "Munich";
        System.out.println("Deep clone: original address city unchanged? " + pd1.address.city); // Berlin
    }

    // === Demo 7: clone without/with Cloneable ===
    private static void demoCloneWithoutAndWithCloneable() {
        try {
            new WithoutCloneable().publicTryClone();
            System.out.println("Unexpected: clone succeeded without Cloneable.");
        } catch (CloneNotSupportedException e) {
            System.out.println("Expected: CloneNotSupportedException without Cloneable.");
        }

        try {
            WithCloneable obj = new WithCloneable(42);
            WithCloneable copy = obj.clone();
            System.out.println("Clone with Cloneable succeeded: " + copy.value);
        } catch (CloneNotSupportedException e) {
            System.out.println("Unexpected: clone failed with Cloneable.");
        }
    }

    // === Demo 8: wait/notify with monitor ===
    private static void demoMonitorWaitNotify() throws InterruptedException {
        BoundedBuffer<String> buf = new BoundedBuffer<>(2);

        Thread producer = new Thread(() -> {
            try {
                for (String s : new String[]{"A", "B", "C"}) {
                    buf.put(s);
                    System.out.println("Produced " + s);
                }
            } catch (InterruptedException ignored) {}
        });

        Thread consumer = new Thread(() -> {
            try {
                for (int i = 0; i < 3; i++) {
                    String s = buf.take();
                    System.out.println("Consumed " + s);
                }
            } catch (InterruptedException ignored) {}
        });

        producer.start();
        consumer.start();
        producer.join();
        consumer.join();
    }

    // === Demo 9: IllegalMonitorStateException ===
    private static void demoIllegalMonitorState() {
        Object lock = new Object();
        try {
            lock.wait(10); // Not owning monitor -> exception
        } catch (IllegalMonitorStateException e) {
            System.out.println("Expected: IllegalMonitorStateException when calling wait() without synchronized.");
        } catch (InterruptedException ignored) { }
    }

    // === Demo 10: Cleaner/AutoCloseable instead of finalize ===
    private static void demoCleanerAlternative() throws Exception {
        try (ResourceWithCleaner r = new ResourceWithCleaner()) {
            r.use();
            System.out.println("Resource used within try-with-resources.");
        } // close() triggers cleaning deterministically
        // Note: finalize is deprecated; no demo for finalize timing guarantees.
    }

    // ===================== Helper classes for demos =====================

    // For mutation pitfall demo
    static class PersonMutable {
        private String name;
        private int age;

        PersonMutable(String name, int age) {
            this.name = name;
            this.age = age;
        }

        void setName(String name) { this.name = name; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PersonMutable)) return false;
            PersonMutable that = (PersonMutable) o;
            return age == that.age && Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, age);
        }
    }

    // For overridden toString demo
    static class PersonToString {
        final String name;
        final int age;

        PersonToString(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @Override
        public String toString() {
            return "PersonToString{name='" + name + "', age=" + age + "}";
        }
    }

    // instanceof-based equality (potentially problematic for hierarchies)
    static class PersonBase {
        final String name;

        PersonBase(String name) { this.name = name; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PersonBase)) return false; // Allows subclass equality
            PersonBase that = (PersonBase) o;
            return Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() { return Objects.hash(name); }
    }

    static class EmployeeSub extends PersonBase {
        final int id;

        EmployeeSub(String name, int id) {
            super(name);
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof EmployeeSub)) return false; // Stricter than base: breaks symmetry with PersonBase
            if (!super.equals(o)) return false;
            EmployeeSub that = (EmployeeSub) o;
            return id == that.id;
        }

        @Override
        public int hashCode() { return Objects.hash(super.hashCode(), id); }
    }

    // getClass-based equality (safer for value types)
    static class PersonExact {
        final String name;

        PersonExact(String name) { this.name = name; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false; // exact type
            PersonExact that = (PersonExact) o;
            return Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() { return Objects.hash(name); }
    }

    static class EmployeeExact extends PersonExact {
        final int id;

        EmployeeExact(String name, int id) {
            super(name);
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false; // exact type
            if (!super.equals(o)) return false;
            EmployeeExact that = (EmployeeExact) o;
            return id == that.id;
        }

        @Override
        public int hashCode() { return Objects.hash(super.hashCode(), id); }
    }

    // Shallow clone demo
    static class Address {
        String city;
        Address(String city) { this.city = city; }
    }

    static class PersonShallow implements Cloneable {
        String name;
        Address address;

        PersonShallow(String name, Address address) {
            this.name = name;
            this.address = address;
        }

        @Override
        public PersonShallow clone() throws CloneNotSupportedException {
            // Shallow copy: Address reference is shared
            return (PersonShallow) super.clone();
        }
    }

    // Deep clone demo
    static class AddressDeep implements Cloneable {
        String city;
        AddressDeep(String city) { this.city = city; }

        @Override
        public AddressDeep clone() throws CloneNotSupportedException {
            return (AddressDeep) super.clone();
        }
    }

    static class PersonDeep implements Cloneable {
        String name;
        AddressDeep address;

        PersonDeep(String name, AddressDeep address) {
            this.name = name;
            this.address = address;
        }

        @Override
        public PersonDeep clone() throws CloneNotSupportedException {
            PersonDeep copy = (PersonDeep) super.clone();
            copy.address = address.clone(); // deep copy of nested mutable state
            return copy;
        }
    }

    // Clone without Cloneable -> exception
    static class WithoutCloneable {
        public WithoutCloneable publicTryClone() throws CloneNotSupportedException {
            return (WithoutCloneable) super.clone();
        }
    }

    // Clone with Cloneable -> OK
    static class WithCloneable implements Cloneable {
        final int value;

        WithCloneable(int value) { this.value = value; }

        @Override
        public WithCloneable clone() throws CloneNotSupportedException {
            return (WithCloneable) super.clone();
        }
    }

    // Minimal bounded buffer using wait/notifyAll (guarded by 'this')
    static class BoundedBuffer<T> {
        private final Queue<T> q = new ArrayDeque<>();
        private final int capacity;

        BoundedBuffer(int capacity) { this.capacity = capacity; }

        public void put(T item) throws InterruptedException {
            synchronized (this) {
                while (q.size() == capacity) {
                    this.wait(); // releases monitor
                }
                q.add(item);
                this.notifyAll(); // signal consumers
            }
        }

        public T take() throws InterruptedException {
            synchronized (this) {
                while (q.isEmpty()) {
                    this.wait(); // releases monitor
                }
                T item = q.remove();
                this.notifyAll(); // signal producers
                return item;
            }
        }
    }

    // Cleaner-based resource (preferred over finalize)
    static class ResourceWithCleaner implements AutoCloseable {
        private static final Cleaner cleaner = Cleaner.create();

        private static final class State implements Runnable {
            private boolean closed = false;

            @Override
            public void run() {
                if (!closed) {
                    // Perform cleanup of native handles, files, etc.
                    System.out.println("Cleaner: cleaning unmanaged resource.");
                }
            }
        }

        private final State state = new State();
        private final Cleaner.Cleanable cleanable;

        ResourceWithCleaner() {
            this.cleanable = cleaner.register(this, state);
        }

        void use() {
            // Simulate work
        }

        @Override
        public void close() {
            state.closed = true;
            cleanable.clean(); // deterministic cleanup
        }
    }
}