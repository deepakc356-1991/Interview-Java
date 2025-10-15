package _02_07_design_principles_solid;

/**
 * SOLID Principles — concise examples with anti-patterns and improved designs.
 * Run main() to see console output for each principle.
 */
public class _02_Examples {

    public static void main(String[] args) {
        System.out.println("=== SOLID Principles Examples ===\n");
        demoSRP();
        demoOCP();
        demoLSP();
        demoISP();
        demoDIP();
    }

    // ---------------------------- SRP ----------------------------
    // Single Responsibility Principle: a class should have one reason to change.
    private static void demoSRP() {
        System.out.println("---- SRP (Single Responsibility Principle) ----");

        // Anti-example: ReportBad both renders and persists data (two responsibilities).
        ReportBad bad = new ReportBad("Sales", "Total revenue: $10,000");
        String rendered = bad.render();
        System.out.println("[Bad] Rendered: " + rendered);
        bad.saveToFile("sales_report_bad.txt");

        // Improved: split formatting, printing, and persistence.
        Report report = new Report("Sales", "Total revenue: $10,000");
        ReportFormatter formatter = new ReportFormatter();
        ReportPrinter printer = new ReportPrinter();
        ReportRepository repository = new ReportRepository();

        String formatted = formatter.format(report);
        printer.printToConsole(formatted);
        repository.saveToFile(report, "sales_report_good.txt");

        System.out.println();
    }

    // ---------------------------- OCP ----------------------------
    // Open/Closed Principle: software entities should be open for extension, closed for modification.
    private static void demoOCP() {
        System.out.println("---- OCP (Open/Closed Principle) ----");

        // Anti-example: calculator uses instance-of chains; adding a new shape requires modifying the method.
        Object[] badShapes = { new CircleOCPBad(2), new RectangleOCPBad(2, 3) };
        AreaCalculatorOCPBad badCalc = new AreaCalculatorOCPBad();
        System.out.println("[Bad] Total area: " + badCalc.totalArea(badShapes));

        // Improved: extend by adding new implementations without modifying AreaCalculator.
        AreaCalculator goodCalc = new AreaCalculator();
        double total = goodCalc.totalArea(
                new Circle(2),
                new Rectangle(2, 3),
                new RightTriangle(3, 4) // added without touching AreaCalculator
        );
        System.out.println("[Good] Total area: " + total);

        System.out.println();
    }

    // ---------------------------- LSP ----------------------------
    // Liskov Substitution Principle: objects of a superclass should be replaceable with objects of a subclass without breaking correctness.
    private static void demoLSP() {
        System.out.println("---- LSP (Liskov Substitution Principle) ----");

        // Anti-example: Square extends Rectangle; mutators break expectations.
        RectangleLSPBad rect = new RectangleLSPBad();
        SquareLSPBad square = new SquareLSPBad();
        System.out.println("[Bad] Using RectangleLSPBad:");
        useRectangleExpectArea(rect);
        System.out.println("[Bad] Using SquareLSPBad as RectangleLSPBad:");
        useRectangleExpectArea(square); // Violates expectation

        // Improved: remove problematic inheritance; use immutable shapes with common abstraction.
        MeasurableArea r = new RectangleLSPGood(5, 4);
        MeasurableArea s = new SquareLSPGood(5);
        System.out.println("[Good] Rectangle area: " + r.area());
        System.out.println("[Good] Square area: " + s.area());

        System.out.println();
    }

    private static void useRectangleExpectArea(RectangleLSPBad r) {
        r.setWidth(5);
        r.setHeight(4);
        int expected = 20;
        int actual = r.getArea();
        System.out.println("Expected area " + expected + ", actual " + actual);
    }

    // ---------------------------- ISP ----------------------------
    // Interface Segregation Principle: no client should be forced to depend on methods it does not use.
    private static void demoISP() {
        System.out.println("---- ISP (Interface Segregation Principle) ----");

        // Anti-example: fat interface forces unsupported ops.
        MultiFunctionDeviceBad mfdBad = new SimplePrinterBad();
        mfdBad.print(new Document("Doc A"));
        mfdBad.scan(new Document("Doc A")); // forced, not supported

        // Improved: lean interfaces; clients implement only what they need.
        Printer printer = new SimplePrinter();
        printer.print(new Document("Doc B"));

        MultiFunctionDevice pro = new OfficeAllInOne();
        pro.print(new Document("Doc C"));
        pro.scan(new Document("Doc C"));
        pro.fax(new Document("Doc C"));

        System.out.println();
    }

    // ---------------------------- DIP ----------------------------
    // Dependency Inversion Principle: high-level modules should not depend on low-level modules; both depend on abstractions.
    private static void demoDIP() {
        System.out.println("---- DIP (Dependency Inversion Principle) ----");

        // Anti-example: high-level class depends on concrete DB.
        PasswordReminderBad bad = new PasswordReminderBad();
        bad.remind("alice");

        // Improved: depend on abstraction and inject concrete implementation.
        PasswordReminderGood withMySql = new PasswordReminderGood(new MySqlConnection());
        withMySql.remind("alice");

        PasswordReminderGood withInMemory = new PasswordReminderGood(new InMemoryConnection());
        withInMemory.remind("bob");

        System.out.println();
    }
}

/* ============================== SRP ============================== */

// Anti: two responsibilities (format + persistence) in one class.
class ReportBad {
    private final String title;
    private final String body;

    ReportBad(String title, String body) {
        this.title = title;
        this.body = body;
    }

    String render() {
        return "[Report] " + title + " -> " + body;
    }

    void saveToFile(String path) {
        // Simulating I/O for brevity
        System.out.println("[Bad] Saving report to " + path);
    }
}

// Good: model + formatter + printer + repository (separate responsibilities).
class Report {
    final String title;
    final String body;

    Report(String title, String body) {
        this.title = title;
        this.body = body;
    }
}

class ReportFormatter {
    String format(Report r) {
        return "[Report] " + r.title + " -> " + r.body;
    }
}

class ReportPrinter {
    void printToConsole(String formatted) {
        System.out.println("[Good] Printed: " + formatted);
    }
}

class ReportRepository {
    void saveToFile(Report r, String path) {
        System.out.println("[Good] Saving report '" + r.title + "' to " + path);
    }
}

/* ============================== OCP ============================== */

// Anti: type checks; must modify this class to support new shapes.
class AreaCalculatorOCPBad {
    double totalArea(Object[] shapes) {
        double sum = 0.0;
        for (Object s : shapes) {
            if (s instanceof CircleOCPBad c) {
                sum += Math.PI * c.radius * c.radius;
            } else if (s instanceof RectangleOCPBad r) {
                sum += r.width * r.height;
            }
            // Adding a new shape means adding another else-if (bad).
        }
        return sum;
    }
}
class CircleOCPBad {
    final double radius;
    CircleOCPBad(double radius) { this.radius = radius; }
}
class RectangleOCPBad {
    final double width;
    final double height;
    RectangleOCPBad(double width, double height) {
        this.width = width;
        this.height = height;
    }
}

// Good: rely on abstraction.
interface Shape {
    double area();
}
class Circle implements Shape {
    private final double r;
    Circle(double r) { this.r = r; }
    public double area() { return Math.PI * r * r; }
}
class Rectangle implements Shape {
    private final double w, h;
    Rectangle(double w, double h) { this.w = w; this.h = h; }
    public double area() { return w * h; }
}
class RightTriangle implements Shape {
    private final double base, height;
    RightTriangle(double base, double height) { this.base = base; this.height = height; }
    public double area() { return 0.5 * base * height; }
}
class AreaCalculator {
    double totalArea(Shape... shapes) {
        double sum = 0.0;
        for (Shape s : shapes) sum += s.area();
        return sum;
    }
}

/* ============================== LSP ============================== */

// Anti: Square extends Rectangle with conflicting invariants.
class RectangleLSPBad {
    protected int width, height;
    void setWidth(int w) { this.width = w; }
    void setHeight(int h) { this.height = h; }
    int getArea() { return width * height; }
}
class SquareLSPBad extends RectangleLSPBad {
    @Override
    void setWidth(int w) { this.width = w; this.height = w; }
    @Override
    void setHeight(int h) { this.width = h; this.height = h; }
}

// Good: both shapes adhere to the same contract without fragile inheritance.
interface MeasurableArea {
    int area();
}
class RectangleLSPGood implements MeasurableArea {
    private final int width, height;
    RectangleLSPGood(int width, int height) { this.width = width; this.height = height; }
    public int area() { return width * height; }
}
class SquareLSPGood implements MeasurableArea {
    private final int side;
    SquareLSPGood(int side) { this.side = side; }
    public int area() { return side * side; }
}

/* ============================== ISP ============================== */

// Anti: fat interface forces clients to implement unneeded methods.
class Document {
    final String name;
    Document(String name) { this.name = name; }
}
interface MultiFunctionDeviceBad {
    void print(Document d);
    void scan(Document d);
    void fax(Document d);
}
class SimplePrinterBad implements MultiFunctionDeviceBad {
    public void print(Document d) { System.out.println("[Bad] Printing: " + d.name); }
    public void scan(Document d)  { System.out.println("[Bad] Scan not supported, but forced to implement."); }
    public void fax(Document d)   { System.out.println("[Bad] Fax not supported, but forced to implement."); }
}

// Good: segregated interfaces.
interface Printer { void print(Document d); }
interface Scanner { void scan(Document d); }
interface Fax { void fax(Document d); }

class SimplePrinter implements Printer {
    public void print(Document d) { System.out.println("[Good] Printing: " + d.name); }
}

interface MultiFunctionDevice extends Printer, Scanner, Fax {}

class OfficeAllInOne implements MultiFunctionDevice {
    public void print(Document d) { System.out.println("[Good] MFD Printing: " + d.name); }
    public void scan(Document d)  { System.out.println("[Good] MFD Scanning: " + d.name); }
    public void fax(Document d)   { System.out.println("[Good] MFD Faxing: " + d.name); }
}

/* ============================== DIP ============================== */

// Anti: high-level module depends on concrete low-level module.
class MySqlConnectionBad {
    void connect() { System.out.println("[Bad] Connecting to MySQL..."); }
    String getName() { return "MySQL"; }
}
class PasswordReminderBad {
    private final MySqlConnectionBad connection = new MySqlConnectionBad(); // hard dependency
    void remind(String user) {
        connection.connect();
        System.out.println("[Bad] Retrieving email for " + user + " via " + connection.getName());
    }
}

// Good: depend on abstractions and inject concrete implementations.
interface DatabaseConnection {
    void connect();
    String getName();
    String findEmailByUser(String user);
}
class MySqlConnection implements DatabaseConnection {
    public void connect() { System.out.println("[Good] Connecting to MySQL..."); }
    public String getName() { return "MySQL"; }
    public String findEmailByUser(String user) { return user + "@example.com"; }
}
class InMemoryConnection implements DatabaseConnection {
    public void connect() { System.out.println("[Good] Using in-memory DB..."); }
    public String getName() { return "InMemoryDB"; }
    public String findEmailByUser(String user) { return user + "@in-memory.local"; }
}
class PasswordReminderGood {
    private final DatabaseConnection connection;
    PasswordReminderGood(DatabaseConnection connection) {
        this.connection = connection; // injected
    }
    void remind(String user) {
        connection.connect();
        String email = connection.findEmailByUser(user);
        System.out.println("[Good] Sending reminder to " + email + " via " + connection.getName());
    }
}