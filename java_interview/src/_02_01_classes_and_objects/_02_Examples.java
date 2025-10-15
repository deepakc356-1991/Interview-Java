package _02_01_classes_and_objects;

/*
    _02_Examples.java
    Classes & Objects — Comprehensive, commented examples in one file.
    - Class definition, fields, constructors, methods, overloading
    - this keyword, getters/setters, encapsulation
    - static fields/methods, constants, init blocks
    - toString, equals, hashCode
    - Composition (has-a), copy constructor (deep copy)
    - Inner classes vs static nested classes
    - Immutability and fluent method chaining
    - Object references, null checks
*/
public class _02_Examples {
    public static void main(String[] args) {
        System.out.println("---- Classes & Objects: Examples ----");

        // Example 1: Class, fields, constructors, methods, overloading, this, static
        System.out.println("\n[Example 1] Class basics: fields, constructors, methods, overloading, static");
        System.out.println(Car.describeGeneral()); // static method and constant

        Car car1 = new Car(); // triggers Car static + instance initializers, then constructor
        System.out.println("car1 -> " + car1);
        car1.drive(15.5); // double
        car1.drive(5);    // int (overloaded)
        System.out.println("car1 odometer = " + car1.getOdometer());

        Car car2 = new Car("Tesla", "Model 3", 2023, true);
        car2.drive(120);
        System.out.println("car2 -> " + car2);
        System.out.println("Total cars built = " + Car.getTotalCars());

        // Reference vs object (aliasing)
        Car alias = car2; // same object, new reference
        alias.setModel("Model 3 Performance");
        System.out.println("car2 model (after alias change) = " + car2.getModel());

        // Example 1b: Inheritance (subclass is-a superclass)
        System.out.println("\n[Example 1b] Inheritance (ElectricCar extends Car)");
        ElectricCar ev = new ElectricCar("Nissan", "Leaf", 2020, 40);
        System.out.println("ev -> " + ev);
        System.out.println("Total cars built = " + Car.getTotalCars());

        // Example 2: Encapsulation with getters/setters and validation
        System.out.println("\n[Example 2] Encapsulation: getters/setters with validation");
        BankAccount acct = new BankAccount("ACC-100", "Sam", BankAccount.MIN_OPENING_DEPOSIT);
        acct.deposit(50);
        boolean ok = acct.withdraw(40);
        System.out.println("withdraw ok? " + ok + ", balance=" + acct.getBalance());

        // Example 3: Composition (has-a), static factories, equals/hashCode, deep copy
        System.out.println("\n[Example 3] Composition, static factory, equals/hashCode, deep copy");
        Person2 ada = Person2.of("Ada", 30).moveTo(new Address2("1 Main", "New York", "USA"));
        Person2 ada2 = Person2.of("Ada", 30).moveTo(new Address2("1 Main", "New York", "USA"));
        System.out.println("ada.equals(ada2)? " + ada.equals(ada2));
        System.out.println("ada == ada2? " + (ada == ada2));
        System.out.println("ada hashCode=" + ada.hashCode() + ", ada2 hashCode=" + ada2.hashCode());

        // Deep copy via copy constructor
        Person2 adaCopy = new Person2(ada);
        ada.getAddress2().setCity("Boston"); // mutate original person's address
        System.out.println("ada.address.city = " + ada.getAddress2().getCity());
        System.out.println("adaCopy.address.city (deep copy unaffected) = " + adaCopy.getAddress2().getCity());

        // Example 4: Initialization order: static block -> instance block -> constructor
        System.out.println("\n[Example 4] Initialization order");
        InitDemo demo = new InitDemo();
        System.out.println("InitDemo.staticValue = " + InitDemo.staticValue + ", demo.instanceValue = " + demo.instanceValue);

        // Example 5: Inner class vs static nested class
        System.out.println("\n[Example 5] Inner class vs static nested class");
        Outer2 outer = new Outer2();
        Outer2.Inner inner = outer.new Inner(); // needs an instance of Outer2
        System.out.println("Inner sumWithOuter2(5) = " + inner.sumWithOuter2(5));
        System.out.println("Outer2.Nested.times2(7) = " + Outer2.Nested.times2(7)); // static on nested class
        Outer2.Nested nestedInstance = new Outer2.Nested(); // can create without Outer2 instance
        System.out.println("nestedInstance.triple(3) = " + nestedInstance.triple(3));

        // Example 6: Immutable class
        System.out.println("\n[Example 6] Immutable class");
        ImmutablePoint2 p = new ImmutablePoint2(1, 2);
        ImmutablePoint2 p2 = p.moveBy(5, -1);
        System.out.println("p = " + p + ", p2 = " + p2); // original p unchanged

        // Example 7: Fluent API (method chaining returning this)
        System.out.println("\n[Example 7] Fluent API / Method chaining");
        new FluentOrder()
                .item("Laptop")
                .quantity(2)
                .note("Expedite")
                .submit();

        // Example 8: Null checks with references
        System.out.println("\n[Example 8] Null reference check");
        Person2 maybeNull = null;
        if (maybeNull == null) {
            System.out.println("maybeNull is null; avoid calling methods.");
        }

        System.out.println("\n---- End ----");
    }
}

/* ------------------------- Supporting classes ------------------------- */

/*
    Car:
    - Demonstrates instance fields, static fields, constants (static final)
    - Constructor overloading, this(), method overloading, getters/setters
    - Static and instance initializer blocks
    - toString override
*/
class Car {
    // Instance fields (each object has its own copy)
    private String make;
    private String model;
    private int year;
    private double odometer;
    private boolean electric;

    // Static field (shared across all instances)
    private static int totalCars;

    // Constant (shared, cannot change)
    public static final int WHEELS = 4;

    // Static initializer (runs once when class is loaded)
    static {
        System.out.println("[Car] static initializer: class loaded.");
    }

    // Instance initializer (runs before every constructor call)
    {
        System.out.println("[Car] instance initializer: before constructor.");
    }

    // No-arg constructor (delegates to all-args)
    public Car() {
        this("Unknown", "Unknown", 0, false);
        System.out.println("[Car] no-arg constructor.");
    }

    // Overloaded constructor
    public Car(String make, String model, int year) {
        this(make, model, year, false);
    }

    // Full constructor uses 'this' to refer to fields shadowed by parameters
    public Car(String make, String model, int year, boolean electric) {
        this.make = make;
        this.model = model; // 'this.model' refers to the field, 'model' is the parameter
        this.year = year;
        this.electric = electric;
        this.odometer = 0.0;
        totalCars++;
        System.out.println("[Car] all-args constructor created: " + this.getDescription());
    }

    // Instance method
    public void drive(double kilometers) {
        if (kilometers <= 0) return;
        this.odometer += kilometers;
    }

    // Overloaded method (same name, different parameter types)
    public void drive(int kilometers) {
        drive((double) kilometers);
    }

    public String getDescription() {
        return year + " " + make + " " + model + (electric ? " (EV)" : "");
    }

    // Getters and setters (encapsulation)
    public String getMake() { return make; }
    public void setMake(String make) { this.make = make; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public double getOdometer() { return odometer; }

    // Protect invariant: odometer cannot go backwards
    public void setOdometer(double odometer) {
        if (odometer >= this.odometer) {
            this.odometer = odometer;
        }
    }

    public boolean isElectric() { return electric; }
    public void setElectric(boolean electric) { this.electric = electric; }

    // Static methods operate on class-level data
    public static int getTotalCars() { return totalCars; }
    public static String describeGeneral() {
        return "Cars typically have " + WHEELS + " wheels.";
    }

    @Override
    public String toString() {
        return "Car{make='" + make + '\'' +
                ", model='" + model + '\'' +
                ", year=" + year +
                ", odometer=" + odometer +
                ", electric=" + electric +
                '}';
    }
}

/*
    ElectricCar:
    - Demonstrates inheritance (is-a)
    - Calls super(...) to initialize Car part
*/
class ElectricCar extends Car {
    private int batteryCapacityKWh;

    public ElectricCar(String make, String model, int year, int batteryCapacityKWh) {
        super(make, model, year, true); // set 'electric' to true via superclass ctor
        this.batteryCapacityKWh = batteryCapacityKWh;
    }

    public int getBatteryCapacityKWh() { return batteryCapacityKWh; }

    @Override
    public String toString() {
        return super.toString() + ", battery=" + batteryCapacityKWh + "kWh";
    }
}

/*
    BankAccount:
    - Encapsulation with private fields, getters/setters, validation
    - final field (accountNumber) cannot be changed after construction
*/
class BankAccount {
    private final String accountNumber;
    private String owner;
    private double balance;

    public static final double MIN_OPENING_DEPOSIT = 25.0;

    public BankAccount(String accountNumber, String owner) {
        this(accountNumber, owner, 0.0);
    }

    public BankAccount(String accountNumber, String owner, double openingDeposit) {
        this.accountNumber = requireNonEmpty(accountNumber, "accountNumber");
        this.owner = requireNonEmpty(owner, "owner");
        deposit(openingDeposit);
    }

    public void deposit(double amount) {
        if (amount < 0) throw new IllegalArgumentException("amount must be >= 0");
        balance += amount;
    }

    public boolean withdraw(double amount) {
        if (amount <= 0) return false;
        if (amount > balance) return false;
        balance -= amount;
        return true;
    }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = requireNonEmpty(owner, "owner"); }
    public String getAccountNumber() { return accountNumber; }
    public double getBalance() { return balance; }

    private static String requireNonEmpty(String s, String name) {
        if (s == null || s.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " must have a value");
        }
        return s;
    }

    @Override
    public String toString() {
        return "BankAccount{accountNumber='" + accountNumber + '\'' +
                ", owner='" + owner + '\'' +
                ", balance=" + balance +
                '}';
    }
}

/*
    Address2:
    - Simple value object used by Person2 (composition)
    - Implements equals and hashCode for deep comparisons
    - Copy constructor for deep copying
*/
class Address2 {
    private String street;
    private String city;
    private String country;

    public Address2(String street, String city, String country) {
        this.street = street;
        this.city = city;
        this.country = country;
    }

    // Copy constructor (deep copy for value fields)
    public Address2(Address2 other) {
        this(other.street, other.city, other.country);
    }

    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Address2)) return false;
        Address2 that = (Address2) o;
        return java.util.Objects.equals(street, that.street)
                && java.util.Objects.equals(city, that.city)
                && java.util.Objects.equals(country, that.country);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(street, city, country);
    }

    @Override
    public String toString() {
        return street + ", " + city + ", " + country;
    }
}

/*
    Person2:
    - Composition (has-a Address2)
    - Static factory method 'of'
    - Equals and hashCode based on significant fields
    - Deep copy via copy constructor
    - Fluent instance method returning 'this'
*/
class Person2 {
    private String name;
    private int age;
    private Address2 address; // composition

    public Person2(String name, int age, Address2 address) {
        this.name = name;
        this.age = age;
        this.address = address; // reference to an Address2 (object graph)
    }

    // Copy constructor (deep copy for address)
    public Person2(Person2 other) {
        this(
                other.name,
                other.age,
                other.address != null ? new Address2(other.address) : null
        );
    }

    public static Person2 of(String name, int age) {
        return new Person2(name, age, null);
    }

    // Fluent method returns 'this' to allow chaining
    public Person2 moveTo(Address2 newAddress2) {
        this.address = newAddress2;
        return this;
    }

    public String getName() { return name; }
    public int getAge() { return age; }
    public Address2 getAddress2() { return address; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Person2)) return false;
        Person2 that = (Person2) o;
        return age == that.age
                && java.util.Objects.equals(name, that.name)
                && java.util.Objects.equals(address, that.address);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(name, age, address);
    }

    @Override
    public String toString() {
        return "Person2{name='" + name + "', age=" + age +
                ", address=" + (address == null ? "<none>" : address) + '}';
    }
}

/*
    InitDemo:
    - Demonstrates initialization order:
      1) static block (once)
      2) instance block (each new object)
      3) constructor
*/
class InitDemo {
    public static int staticValue;
    public int instanceValue;

    static {
        System.out.println("[InitDemo] static block: runs once before any instances.");
        staticValue = 42;
    }

    {
        System.out.println("[InitDemo] instance block: runs before constructor.");
        instanceValue = 7;
    }

    public InitDemo() {
        System.out.println("[InitDemo] constructor: instanceValue was " + instanceValue + ", now setting to 99.");
        instanceValue = 99;
    }
}

/*
    Outer2 and nested types:
    - Inner (non-static): has access to Outer2 instance members
    - Nested (static): independent of any Outer2 instance
*/
class Outer2 {
    private int x = 10;

    class Inner {
        int sumWithOuter2(int y) {
            // Can access Outer2.this.x
            return x + y;
        }
    }

    static class Nested {
        static int times2(int z) { return 2 * z; }
        int triple(int z) { return 3 * z; }
    }
}

/*
    ImmutablePoint2:
    - Immutable class: fields are private final, no setters
    - "Mutating" operations return new instances
*/
final class ImmutablePoint2 {
    private final int x;
    private final int y;

    public ImmutablePoint2(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() { return x; }
    public int getY() { return y; }

    public ImmutablePoint2 moveBy(int dx, int dy) {
        return new ImmutablePoint2(x + dx, y + dy);
    }

    @Override
    public String toString() {
        return "ImmutablePoint2(" + x + ", " + y + ")";
    }
}

/*
    FluentOrder:
    - Fluent API: each mutator returns 'this' to enable chaining
*/
class FluentOrder {
    private String item;
    private int quantity;
    private String note;

    public FluentOrder item(String item) { this.item = item; return this; }
    public FluentOrder quantity(int quantity) { this.quantity = quantity; return this; }
    public FluentOrder note(String note) { this.note = note; return this; }

    public void submit() {
        System.out.println("[FluentOrder] submitted -> " + this);
    }

    @Override
    public String toString() {
        return "FluentOrder{item='" + item + "', quantity=" + quantity + ", note='" + note + "'}";
    }
}