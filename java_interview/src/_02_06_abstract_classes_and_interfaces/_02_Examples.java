package _02_06_abstract_classes_and_interfaces;

public class _02_Examples {

    public static void main(String[] args) {
        // 1) Abstract classes + inheritance
        Shape circle = new Circle("c1", 2.5);
        Shape rect = new Rectangle("r1", 3, 4);

        circle.draw(); // from Shape implements Drawable.draw()
        rect.draw();

        circle.printSummary(); // final method (cannot be overridden)
        rect.printSummary();

        System.out.printf("Total area = %.2f%n", Shape.sumAreas(circle, rect));
        System.out.println();

        // 2) Interfaces + default methods
        System.out.println("circle color (Drawable default) = " + circle.color());
        System.out.println("rectangle color (SmartDrawable override) = " + rect.color());
        System.out.println();

        // 3) Implementing multiple interfaces (Movable)
        Movable mCircle = (Circle) circle;   // variable is Shape, cast to access Movable
        Movable mRect = (Rectangle) rect;

        mCircle.moveBy(10, -5);
        mRect.moveBy(-2, 7);

        System.out.println(circle); // Circle.toString
        System.out.println(rect);   // Rectangle.toString
        System.out.println();

        // 4) Resolving default method conflicts (InterfaceName.super.method())
        ColorWidget widget = new ColorWidget();
        System.out.println("ColorWidget.color() = " + widget.color());
        System.out.println();

        // 5) Interface static factory + functional interfaces + lambda
        Drawable logo = Drawable.of("Logo");  // static factory on interface
        logo.draw();
        System.out.println("logo color = " + logo.color());

        Drawable lambdaDrawable = () -> System.out.println("Drawing from lambda Drawable");
        lambdaDrawable.draw();

        Transformer<String, Integer> len = String::length; // method reference
        System.out.println("length of \"abc\" = " + len.apply("abc"));

        Transformer<Shape, String> describe = s -> s.getName() + " area=" + String.format("%.2f", s.area());
        System.out.println(describe.apply(circle));
        System.out.println();

        // 6) Anonymous class implementing an interface
        Drawable anon = new Drawable() {
            @Override public void draw() { System.out.println("Drawing from anonymous class"); }
            @Override public String color() { return "gold"; } // overriding default
        };
        anon.draw();
        System.out.println("anon color = " + anon.color());
        System.out.println();

        // 7) Template method pattern via abstract class
        DataProcessor p = new ToUpperProcessor();
        p.process("  hello world  ");
        p.process("   "); // invalid after trim
        System.out.println();

        // 8) Interface constants (public static final)
        System.out.println("Drawable.DEFAULT_THICKNESS = " + Drawable.DEFAULT_THICKNESS);
        // Drawable.DEFAULT_THICKNESS = 2; // compile error: cannot assign a value to final variable
        System.out.println();

        // 9) Polymorphism via interface parameter
        render(circle);
        render(rect);
        render(logo);
    }

    // Utility that uses interface polymorphism
    static void render(Drawable d) {
        System.out.print("Render -> ");
        d.draw();
    }

    // INTERFACES

    // Interface with:
    // - constant (implicitly public static final)
    // - abstract method
    // - default method (reusable implementation)
    // - static factory method
    interface Drawable {
        int DEFAULT_THICKNESS = 1;

        void draw();

        default String color() { return "black"; }

        default void print() { System.out.println("Drawable.print color=" + color()); }

        static Drawable of(String name) {
            // Drawable is functional (1 abstract method), so lambda works
            return () -> System.out.println("Drawing " + name);
        }
    }

    // Simple interface; classes can implement many interfaces
    interface Movable {
        void moveBy(int dx, int dy);
    }

    // Interface inheritance: combines Drawable + Movable and adds new behavior
    interface SmartDrawable extends Drawable, Movable {
        void resize(double factor);

        // Override default method from Drawable
        default String color() { return "blue"; }
    }

    // Two interfaces with the same default signature -> conflict to resolve
    interface HasColor {
        default String color() { return "black"; }
    }
    interface Tinted {
        default String color() { return "tinted"; }
    }

    // Must override to resolve conflict; can call each default explicitly
    static class ColorWidget implements HasColor, Tinted {
        @Override
        public String color() {
            return HasColor.super.color() + "+" + Tinted.super.color();
        }
    }

    // FUNCTIONAL INTERFACE (exactly one abstract method)
    @FunctionalInterface
    interface Transformer<T, R> {
        R apply(T t);

        // Default method doesn't break functional-ness
        default Transformer<T, R> andThen(Transformer<R, R> after) {
            return t -> after.apply(apply(t));
        }
    }

    // ABSTRACT CLASS HIERARCHY

    // Abstract class can have:
    // - state and constructors
    // - concrete and abstract methods
    // - final methods
    static abstract class Shape implements Drawable {
        private final String name;

        protected Shape(String name) {
            this.name = name;
        }

        public String getName() { return name; }

        public abstract double area();
        public abstract double perimeter();

        // Provide one interface method implementation here
        @Override
        public void draw() {
            System.out.printf("Drawing %s: area=%.2f, perimeter=%.2f%n",
                    name, area(), perimeter());
        }

        // Final: cannot be overridden by subclasses
        public final void printSummary() {
            System.out.printf("Summary [%s] color=%s thickness=%d%n",
                    name, color(), Drawable.DEFAULT_THICKNESS);
        }

        // Static helper allowed on abstract classes
        public static double sumAreas(Shape... shapes) {
            double sum = 0;
            for (Shape s : shapes) sum += s.area();
            return sum;
        }

        public String type() { return getClass().getSimpleName(); }
    }

    // Concrete subclass must implement all abstract methods
    static class Circle extends Shape implements Movable {
        private double radius;
        private int x, y;

        public Circle(String name, double radius) {
            super(name);
            this.radius = radius;
        }

        @Override public double area() { return Math.PI * radius * radius; }

        @Override public double perimeter() { return 2 * Math.PI * radius; }

        public void setRadius(double radius) { this.radius = radius; }

        @Override public void moveBy(int dx, int dy) { x += dx; y += dy; }

        @Override public String toString() {
            return String.format("Circle{name=%s, r=%.2f, center=(%d,%d)}",
                    getName(), radius, x, y);
        }
    }

    // Implements multiple interfaces at once via SmartDrawable
    static class Rectangle extends Shape implements SmartDrawable {
        private double w, h;
        private int x, y;

        public Rectangle(String name, double w, double h) {
            super(name);
            this.w = w;
            this.h = h;
        }

        @Override public double area() { return w * h; }

        @Override public double perimeter() { return 2 * (w + h); }

        @Override public void moveBy(int dx, int dy) { x += dx; y += dy; }

        @Override public void resize(double factor) { w *= factor; h *= factor; }

        // Inherits SmartDrawable's default color() -> "blue"

        @Override public String toString() {
            return String.format("Rectangle{name=%s, w=%.2f, h=%.2f, pos=(%d,%d)}",
                    getName(), w, h, x, y);
        }
    }

    // TEMPLATE METHOD PATTERN using an abstract class
    static abstract class DataProcessor {
        // Template method defines the algorithm skeleton
        public final void process(String in) {
            String normalized = in == null ? "" : in.trim();
            if (!preValidate(normalized)) {
                System.out.println("Invalid input");
                return;
            }
            String out = transform(normalized);
            post(out);
        }

        // Hook with default behavior
        protected boolean preValidate(String s) {
            return s != null && !s.isEmpty();
        }

        // Abstract step to be provided by subclasses
        protected abstract String transform(String s);

        // Optional hook
        protected void post(String s) {
            System.out.println("Processed: " + s);
        }
    }

    static class ToUpperProcessor extends DataProcessor {
        @Override protected String transform(String s) { return s.toUpperCase(); }
    }
}