package _02_03_encapsulation_and_access_modifiers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/*
  Encapsulation & Access Modifiers – Examples
  - Encapsulation: hide internal state, expose controlled API, enforce invariants, defensive copies.
  - Access modifiers: public, protected, (package-private), private on types, fields, methods, constructors.
  - This single file compiles and demonstrates the concepts. See comments inline.
*/
public class _02_Examples {

    public static void main(String[] args) {
        // 1) Encapsulation: private fields + validation
        BankAccount acc = new BankAccount("Alice", 100);
        acc.deposit(50);
        try {
            acc.withdraw(200); // blocked by invariant: cannot go below 0
        } catch (IllegalArgumentException e) {
            System.out.println("withdraw blocked: " + e.getMessage());
        }
        System.out.println("Balance: " + acc.getBalance());

        // Anti-example: public fields allow invariant violations
        LooseAccount la = new LooseAccount();
        la.owner = "Bob";
        la.balance = -1_000_000; // allowed -> breaks business rules
        System.out.println("LooseAccount broken balance: " + la.balance);

        // 2) Access modifiers within the same package
        AccessShowcase a = new AccessShowcase();
        new SamePackagePeer().demo(a);

        // 3) protected in inheritance
        Sub s = new Sub();
        s.call();
        s.templateMethod(); // calls protected, package-private, and private (private is called inside Base)

        // 4) Immutability + defensive copies
        Address address = new Address("Paris");
        Person p = new Person("Carol", address);
        address.setCity("Berlin"); // external mutation should NOT affect Person
        System.out.println("Person city after external change: " + p.getAddress().getCity());
        Address fromGetter = p.getAddress(); // defensive copy returned
        fromGetter.setCity("Rome"); // should NOT affect Person
        System.out.println("Person city after getter modification: " + p.getAddress().getCity());

        // 5) Read-only view via interface (expose less)
        Wallet wallet = new Wallet(10);
        ReadonlyBalance view = wallet; // only getter visible through interface
        System.out.println("Wallet balance via read-only view: " + view.getBalance());
        wallet.add(5);
        System.out.println("Wallet balance updated: " + view.getBalance());

        // 6) Private constructor + factory method
        Secret secret = Secret.ofRandom();
        System.out.println(secret);

        // Package-private constructor only accessible in the same package
        PackageCtor pc = new PackageCtor(42);
        System.out.println("PackageCtor value: " + pc.value);

        // 7) Centralized validation reused by all modifiers of state
        CelsiusTemperature t = new CelsiusTemperature(0);
        t.add(10);
        System.out.println(t);
        try {
            t.setValue(-300); // blocked by validation
        } catch (IllegalArgumentException e) {
            System.out.println("temp blocked: " + e.getMessage());
        }

        // 8) Static global config guarded by validation
        System.out.println("Endpoint: " + Config.getApiEndpoint());
        try {
            Config.setApiEndpoint("http://insecure"); // blocked
        } catch (IllegalArgumentException e) {
            System.out.println("config blocked: " + e.getMessage());
        }
        Config.setApiEndpoint("https://api.new");
        System.out.println("Endpoint: " + Config.getApiEndpoint());

        // 9) Encapsulate mutable collections with unmodifiable view + snapshots
        Team team = new Team(Arrays.asList("Ann", "Ben"));
        team.add("Cory");
        List<String> viewList = team.getMembersView(); // read-only view
        System.out.println("Team: " + viewList);
        try {
            viewList.add("Dana"); // throws UnsupportedOperationException
        } catch (UnsupportedOperationException e) {
            System.out.println("view is unmodifiable");
        }
        List<String> snap = team.snapshot(); // independent copy
        snap.add("Eva"); // does not affect internal state
        System.out.println("After modifying snapshot, team: " + team.getMembersView());

        // 10) Private nested helper accessing outer's private state
        new Outer().use();

        // 11) final field vs mutable object
        FinalDemo fd = new FinalDemo(new Address("Oslo"));
        fd.mutate(); // allowed; final prevents reassignment, not mutation of the object
        System.out.println("FinalDemo address city (mutated): " + fd.getAddress().getCity());

        // 12) Package-private top-level class usage (not accessible from other packages)
        PkgPrivateUtility util = new PkgPrivateUtility();
        System.out.println("PkgPrivateUtility sum: " + util.add(2, 3));
    }

    // Example 1: Properly encapsulated domain model with invariants.
    static class BankAccount {
        private String owner;
        private double balance;

        public BankAccount(String owner, double initialBalance) {
            setOwner(owner);
            if (initialBalance < 0) {
                throw new IllegalArgumentException("Initial balance cannot be negative");
            }
            this.balance = initialBalance;
        }

        public String getOwner() {
            return owner;
        }

        public void setOwner(String owner) {
            if (owner == null || owner.isBlank()) {
                throw new IllegalArgumentException("Owner cannot be blank");
            }
            this.owner = owner;
        }

        public double getBalance() {
            return balance;
        }

        // No setBalance to preserve invariant; use controlled operations instead.

        public void deposit(double amount) {
            if (amount <= 0) throw new IllegalArgumentException("Deposit must be positive");
            balance += amount;
        }

        public void withdraw(double amount) {
            if (amount <= 0) throw new IllegalArgumentException("Withdrawal must be positive");
            if (amount > balance) throw new IllegalArgumentException("Insufficient funds");
            balance -= amount;
        }

        @Override
        public String toString() {
            return "BankAccount{owner='" + owner + "', balance=" + balance + "}";
        }
    }

    // Anti-example: breaks encapsulation (do not do this in production).
    static class LooseAccount {
        public String owner;   // anyone can change
        public double balance; // anyone can set to any value
    }

    // Example 2: Fields/methods with all access modifiers.
    static class AccessShowcase {
        public int pubField = 1;
        protected int protField = 2;
        int pkgField = 3;              // package-private (no modifier)
        private int privField = 4;

        public void pubMethod() {}
        protected void protMethod() {}
        void pkgMethod() {}            // package-private
        private void privMethod() {}

        public int sumAllFields() {     // inside class: all are accessible
            return pubField + protField + pkgField + privField;
        }
    }

    // Same package: can access public, protected, and package-private. Not private.
    static class SamePackagePeer {
        void demo(AccessShowcase a) {
            int x = a.pubField;     // ok
            int y = a.protField;    // ok (same package)
            int z = a.pkgField;     // ok (package-private)
            // int w = a.privField; // not allowed

            a.pubMethod();          // ok
            a.protMethod();         // ok (same package)
            a.pkgMethod();          // ok (package-private)
            // a.privMethod();      // not allowed
            System.out.println("Access in same package ok: " + (x + y + z));
        }
    }

    // Example 3: protected with inheritance (also visible in same package).
    static class Base {
        protected void protectedHook() {
            System.out.println("Base.protectedHook");
        }
        void packageMethod() {
            System.out.println("Base.packageMethod");
        }
        private void privateMethod() {
            System.out.println("Base.privateMethod");
        }
        public void templateMethod() {
            protectedHook(); // ok
            packageMethod(); // ok
            privateMethod(); // ok (within the class)
        }
    }

    static class Sub extends Base {
        void call() {
            protectedHook(); // ok (inheritance)
            packageMethod(); // ok (same package)
            // privateMethod(); // not accessible
        }
    }

    // Example 4: Immutable outer + defensive copies for mutable members.
    static final class Person {
        private final String name;
        private final Address address; // mutable class

        public Person(String name, Address address) {
            if (name == null || name.isBlank()) throw new IllegalArgumentException("name required");
            if (address == null) throw new IllegalArgumentException("address required");
            this.name = name;
            // Defensive copy on write
            this.address = new Address(address.getCity());
        }

        public String getName() {
            return name;
        }

        // Defensive copy on read
        public Address getAddress() {
            return new Address(address.getCity());
        }
    }

    // Mutable class to showcase defensive copying
    static class Address {
        private String city;

        public Address(String city) {
            if (city == null) throw new IllegalArgumentException("city required");
            this.city = city;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            if (city == null) throw new IllegalArgumentException("city required");
            this.city = city;
        }

        @Override
        public String toString() {
            return "Address{city='" + city + "'}";
        }
    }

    // Example 5: Expose read-only interface, keep mutators hidden.
    interface ReadonlyBalance {
        double getBalance();
    }

    static class Wallet implements ReadonlyBalance {
        private double balance;

        public Wallet(double balance) {
            this.balance = balance;
        }

        public void add(double amount) {
            if (amount <= 0) throw new IllegalArgumentException("amount must be positive");
            balance += amount;
        }

        public void sub(double amount) {
            if (amount <= 0 || amount > balance) throw new IllegalArgumentException("bad amount");
            balance -= amount;
        }

        @Override
        public double getBalance() {
            return balance;
        }
    }

    // Example 6: Private constructor + factory method; controls instantiation.
    static class Secret {
        private final String token;

        private Secret(String token) {
            this.token = token;
        }

        public static Secret ofRandom() {
            return new Secret("RND-" + System.nanoTime());
        }

        @Override
        public String toString() {
            return "Secret(" + token + ")";
        }
    }

    // Package-private constructor: accessible only inside this package.
    static class PackageCtor {
        final int value;

        PackageCtor(int value) {
            this.value = value;
        }
    }

    // Example 7: Centralized validation for all state changes.
    static class CelsiusTemperature {
        private double value;

        public CelsiusTemperature(double value) {
            setValue(value);
        }

        public final void setValue(double value) {
            if (value < -273.15) throw new IllegalArgumentException("Below absolute zero");
            this.value = value;
        }

        public double getValue() {
            return value;
        }

        public void add(double delta) {
            setValue(this.value + delta); // reuse validation
        }

        @Override
        public String toString() {
            return value + " °C";
        }
    }

    // Example 8: Encapsulated static configuration
    static class Config {
        private static String apiEndpoint = "https://api.example.com";

        public static String getApiEndpoint() {
            return apiEndpoint;
        }

        public static void setApiEndpoint(String url) {
            if (url == null || !url.startsWith("https://")) {
                throw new IllegalArgumentException("Must be https");
            }
            apiEndpoint = url;
        }
    }

    // Example 9: Encapsulate internal collection; expose read-only view or copies.
    static class Team {
        private final List<String> members = new ArrayList<>();

        public Team(List<String> initial) {
            if (initial != null) members.addAll(initial);
        }

        public void add(String member) {
            if (member == null || member.isBlank()) throw new IllegalArgumentException("member required");
            members.add(member);
        }

        // Read-only window on internal list
        public List<String> getMembersView() {
            return Collections.unmodifiableList(members);
        }

        // Snapshot copy the caller can mutate safely
        public List<String> snapshot() {
            return new ArrayList<>(members);
        }
    }

    // Example 10: Private nested helper has access to outer's private state.
    public static class Outer {
        private int secret = 42;

        private static class Helper {
            void reveal(Outer o) {
                System.out.println("secret is " + o.secret);
            }
        }

        public void use() {
            new Helper().reveal(this);
        }
    }

    // Example 11: final field vs mutability of the referenced object.
    static class FinalDemo {
        private final Address address;

        FinalDemo(Address addr) {
            if (addr == null) throw new IllegalArgumentException("addr required");
            this.address = addr;
        }

        public Address getAddress() {
            // Note: returns the actual reference (not defensive). This can leak mutability.
            return address;
        }

        void mutate() {
            address.setCity("NewCity"); // allowed; final only prevents reassignment of 'address'
            // address = new Address("X"); // not allowed: final
        }
    }
}

// Example 12 (top-level, package-private): Accessible within this package only.
class PkgPrivateUtility {
    int add(int a, int b) { // package-private method
        return a + b;
    }
}