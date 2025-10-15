package _02_07_design_principles_solid;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * SOLID Design Principles (Quick reference inside code)
 *
 * - S (Single Responsibility): A class should have one reason to change.
 *   Benefits: cohesion, easier testing; Smells: God classes, mixed concerns (logic, IO, persistence).
 *
 * - O (Open/Closed): Open for extension, closed for modification.
 *   Benefits: add behavior without touching stable code; Smells: switch/if chains on type, frequent edits.
 *
 * - L (Liskov Substitution): Subtypes must be substitutable for their base types (no broken expectations/invariants).
 *   Benefits: safe polymorphism; Smells: overridden methods changing behavior/constraints, runtime checks for subtype.
 *
 * - I (Interface Segregation): Many client-specific interfaces are better than one general-purpose interface.
 *   Benefits: avoid forcing clients to depend on methods they don’t use; Smells: fat interfaces, UnsupportedOperationException.
 *
 * - D (Dependency Inversion): High-level modules depend on abstractions, not concretions. Abstractions do not depend on details.
 *   Benefits: decoupling, testability via DI/mocks; Smells: new-ing concretes inside high-level logic, hard-to-test code.
 */
public class _01_Theory {

    public static void main(String[] args) {
        // SRP demo
        SRP.Invoice inv = new SRP.Invoice();
        inv.addItem(new SRP.Item("Pen", new BigDecimal("1.50"), 3));
        inv.addItem(new SRP.Item("Notebook", new BigDecimal("4.00"), 2));
        System.out.println("[SRP] Total: " + inv.total());
        new SRP.InvoicePrinter().print(inv);

        // OCP demo
        List<OCP.Shape> shapes = List.of(
                new OCP.Circle(2.0),
                new OCP.Rectangle(3.0, 4.0)
        );
        System.out.println("[OCP] Total area: " + new OCP.AreaCalculator().totalArea(shapes));

        // LSP demo (shows violation with Square derived from Rectangle with setters)
        System.out.println("[LSP] Rectangle area (expect 50): " + LSP.useIt(new LSP.Rectangle()));
        System.out.println("[LSP] Square area breaks expectation (not 50): " + LSP.useIt(new LSP.Square()));

        // ISP demo
        ISP.Printer basic = new ISP.BasicPrinter();
        basic.print("Hello");
        ISP.MultiFunctionPrinter mfp = new ISP.MultiFunctionPrinter();
        mfp.print("Contract");
        mfp.scan("Photo");
        mfp.fax("+1-202-555-0199", "Contract");

        // DIP demo
        DIP.PaymentService service = new DIP.PaymentService(new DIP.StripeGateway());
        service.pay(new DIP.Order("A-100", new BigDecimal("29.99"), "USD"));
        service = new DIP.PaymentService(new DIP.PaypalGateway());
        service.pay(new DIP.Order("B-200", new BigDecimal("49.99"), "EUR"));
    }

    // ============= S: Single Responsibility Principle (SRP) =============
    static class SRP {

        /**
         * Anti-example: one class doing many things (calculation, persistence, IO).
         * Symptoms:
         * - saveToDatabase handles persistence
         * - print handles presentation
         * - calculate handles business logic
         * Any change in one concern forces changes/risk in others.
         */
        static class GodInvoice {
            final List<Item> items = new ArrayList<>();

            void addItem(Item item) { items.add(item); }

            BigDecimal calculateTotal() {
                return items.stream()
                        .map(i -> i.price.multiply(BigDecimal.valueOf(i.qty)))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }

            void saveToDatabase() {
                // mixes persistence here (violation)
                System.out.println("Saving invoice with total " + calculateTotal() + " to DB...");
            }

            void print() {
                // mixes presentation here (violation)
                System.out.println("Invoice:");
                for (Item i : items) {
                    System.out.println("- " + i.name + " x" + i.qty + " = " + i.price);
                }
                System.out.println("Total = " + calculateTotal());
            }
        }

        /**
         * SRP-compliant design: split concerns.
         * - Invoice: domain model (business logic)
         * - InvoiceRepository: persistence
         * - InvoicePrinter: presentation
         * - TaxPolicy (optional): separate calculation variations
         */
        static class Item {
            final String name;
            final BigDecimal price;
            final int qty;

            Item(String name, BigDecimal price, int qty) {
                this.name = name;
                this.price = price;
                this.qty = qty;
            }
        }

        static class Invoice {
            private final List<Item> items = new ArrayList<>();

            void addItem(Item item) { items.add(item); }

            BigDecimal total() {
                return items.stream()
                        .map(i -> i.price.multiply(BigDecimal.valueOf(i.qty)))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }

            List<Item> items() { return List.copyOf(items); }
        }

        interface TaxPolicy {
            BigDecimal taxFor(Invoice invoice);
        }

        static class FlatRateTax implements TaxPolicy {
            private final BigDecimal rate; // e.g., 0.20 for 20%

            FlatRateTax(BigDecimal rate) { this.rate = rate; }

            public BigDecimal taxFor(Invoice invoice) {
                return invoice.total().multiply(rate);
            }
        }

        static class InvoiceRepository {
            void save(Invoice invoice) {
                // Persist to DB (simulated)
                System.out.println("Persisted invoice with total " + invoice.total());
            }
        }

        static class InvoicePrinter {
            void print(Invoice invoice) {
                System.out.println("Invoice:");
                for (Item i : invoice.items()) {
                    System.out.println("- " + i.name + " x" + i.qty + " @ " + i.price);
                }
                System.out.println("Total = " + invoice.total());
            }
        }
    }

    // ============= O: Open/Closed Principle (OCP) =============
    static class OCP {

        // Anti-example (closed for extension): adding a new shape requires modifying calculator switch/if.
        enum ShapeType { CIRCLE, RECTANGLE }

        static abstract class Before_Shape {
            final ShapeType type;
            Before_Shape(ShapeType type) { this.type = type; }
        }

        static class Before_Circle extends Before_Shape {
            final double radius;
            Before_Circle(double radius) { super(ShapeType.CIRCLE); this.radius = radius; }
        }

        static class Before_Rectangle extends Before_Shape {
            final double width, height;
            Before_Rectangle(double width, double height) { super(ShapeType.RECTANGLE); this.width = width; this.height = height; }
        }

        static class Before_AreaCalculator {
            double totalArea(List<Before_Shape> shapes) {
                double sum = 0.0;
                for (Before_Shape s : shapes) {
                    if (s.type == ShapeType.CIRCLE) {
                        sum += Math.PI * ((Before_Circle) s).radius * ((Before_Circle) s).radius;
                    } else if (s.type == ShapeType.RECTANGLE) {
                        sum += ((Before_Rectangle) s).width * ((Before_Rectangle) s).height;
                    }
                }
                return sum;
            }
        }

        // OCP-compliant: add a new shape by adding a class; AreaCalculator stays closed for modification.
        interface Shape {
            double area();
        }

        static class Circle implements Shape {
            private final double radius;
            Circle(double radius) { this.radius = radius; }
            public double area() { return Math.PI * radius * radius; }
        }

        static class Rectangle implements Shape {
            private final double width, height;
            Rectangle(double width, double height) { this.width = width; this.height = height; }
            public double area() { return width * height; }
        }

        static class AreaCalculator {
            double totalArea(List<Shape> shapes) {
                double sum = 0.0;
                for (Shape s : shapes) sum += s.area();
                return sum;
            }
        }
    }

    // ============= L: Liskov Substitution Principle (LSP) =============
    static class LSP {

        /**
         * Violation example: Square extends Rectangle with mutable setters.
         * Client expects width and height to vary independently; Square breaks that.
         */
        static class Rectangle {
            private int width;
            private int height;

            public void setWidth(int width) { this.width = width; }
            public void setHeight(int height) { this.height = height; }
            public int getWidth() { return width; }
            public int getHeight() { return height; }
            public int area() { return width * height; }
        }

        static class Square extends Rectangle {
            @Override public void setWidth(int width) {
                super.setWidth(width);
                super.setHeight(width); // couples height to width
            }
            @Override public void setHeight(int height) {
                super.setHeight(height);
                super.setWidth(height); // couples width to height
            }
        }

        static int useIt(Rectangle r) {
            r.setWidth(5);
            r.setHeight(10);
            return r.area(); // expected 5*10 = 50; Square returns 100 (violation)
        }

        /**
         * Safer alternative (LSP-friendly):
         * - Remove setters (make shapes immutable)
         * - Model Square and Rectangle as separate types implementing a common interface
         */
        interface Shape2D {
            int area();
        }

        static class ImmutableRectangle implements Shape2D {
            private final int width, height;
            ImmutableRectangle(int width, int height) { this.width = width; this.height = height; }
            public int area() { return width * height; }
        }

        static class ImmutableSquare implements Shape2D {
            private final int side;
            ImmutableSquare(int side) { this.side = side; }
            public int area() { return side * side; }
        }
    }

    // ============= I: Interface Segregation Principle (ISP) =============
    static class ISP {

        // Anti-example: fat interface
        interface MultiFunctionDevice {
            void print(String document);
            void scan(String document);
            void fax(String number, String document);
        }

        static class SimplePrinter implements MultiFunctionDevice {
            public void print(String document) { System.out.println("Printing: " + document); }
            public void scan(String document) { throw new UnsupportedOperationException("Not supported"); }
            public void fax(String number, String document) { throw new UnsupportedOperationException("Not supported"); }
        }

        // Segregated interfaces
        interface Printer { void print(String document); }
        interface Scanner { void scan(String document); }
        interface Fax { void fax(String number, String document); }

        static class BasicPrinter implements Printer {
            public void print(String document) { System.out.println("BasicPrinter: " + document); }
        }

        static class MultiFunctionPrinter implements Printer, Scanner, Fax {
            public void print(String document) { System.out.println("MFP printing: " + document); }
            public void scan(String document) { System.out.println("MFP scanning: " + document); }
            public void fax(String number, String document) { System.out.println("MFP faxing to " + number + ": " + document); }
        }
    }

    // ============= D: Dependency Inversion Principle (DIP) =============
    static class DIP {

        // Anti-example: high-level depends on concrete; hard to test and swap.
        static class Before_PaymentService {
            private final StripeGateway stripe = new StripeGateway();
            void pay(Order order) {
                stripe.charge(order.currency, order.amount);
            }
        }

        static class Order {
            final String id;
            final BigDecimal amount;
            final String currency;
            Order(String id, BigDecimal amount, String currency) {
                this.id = id; this.amount = amount; this.currency = currency;
            }
        }

        interface PaymentGateway {
            void charge(String currency, BigDecimal amount);
        }

        static class StripeGateway implements PaymentGateway {
            public void charge(String currency, BigDecimal amount) {
                System.out.println("Stripe charged " + amount + " " + currency);
            }
        }

        static class PaypalGateway implements PaymentGateway {
            public void charge(String currency, BigDecimal amount) {
                System.out.println("PayPal charged " + amount + " " + currency);
            }
        }

        // High-level depends on abstraction; inject implementation.
        static class PaymentService {
            private final PaymentGateway gateway;

            PaymentService(PaymentGateway gateway) {
                this.gateway = gateway;
            }

            void pay(Order order) {
                // Business rules before/after payment would live here
                gateway.charge(order.currency, order.amount);
            }
        }
    }
}