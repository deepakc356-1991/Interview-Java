package _02_07_design_principles_solid;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*
SOLID Design Principles – Interview Q&A (Basic → Advanced) with Java Examples

Overview
- SOLID = Single Responsibility, Open-Closed, Liskov Substitution, Interface Segregation, Dependency Inversion.
- Goal: Improve maintainability, testability, extensibility, and robustness.

Basic Q&A
1) What is SOLID?
   A: A set of 5 OO design principles to create understandable, flexible software.

2) Why use SOLID?
   A: Reduces coupling and duplication; eases testing, refactoring, onboarding, and feature addition.

3) SRP?
   A: A class/module should have only one reason to change (one responsibility).

4) OCP?
   A: Software entities should be open for extension but closed for modification.

5) LSP?
   A: Subtypes must be substitutable for their base types without altering correctness.

6) ISP?
   A: Many client-specific interfaces are better than one general-purpose interface (avoid “fat” interfaces).

7) DIP?
   A: High-level modules shouldn’t depend on low-level modules; both depend on abstractions. Abstractions shouldn’t depend on details; details depend on abstractions.

8) How do SOLID and design patterns relate?
   A: Many patterns implement SOLID: Strategy/Decorator (OCP), Adapter/Facade (ISP), Factory/DI/IoC (DIP), Template Method (OCP), Composite (LSP-friendly).

9) Are SOLID principles always applicable?
   A: They’re guidelines, not laws. Overuse can cause over-engineering. Balance with YAGNI and KISS.

10) Which principle to start with?
   A: SRP first (clarifies responsibilities), then DIP (boundaries), then OCP/ISP, and ensure LSP holds.

Intermediate Q&A
11) Symptoms of SRP violation?
    A: God classes, long classes/methods, frequent reason-to-change churn, scattered responsibilities.

12) Achieving OCP without massive inheritance?
    A: Prefer composition and polymorphic strategies; use configuration and plugin modules.

13) LSP practical checks?
    A: Preserve invariants, don’t strengthen preconditions or weaken postconditions, respect expected behavior and exceptions.

14) ISP in APIs?
    A: Split fat interfaces by roles/use-cases; keep client contracts minimal; use composition for multi-role devices.

15) DIP vs DI vs IoC?
    A: DIP is the principle; DI (constructor/setter) is how you provide dependencies; IoC is the broader idea of externalizing control (containers, frameworks).

16) Refactoring a switch/if-else to OCP?
    A: Replace conditionals with polymorphism: strategy + registry/factory.

17) When to bend OCP?
    A: When extension complexity outweighs benefit (hot code-path performance, early-stage code). Refactor later.

18) Unit testing and SOLID?
    A: SRP and DIP reduce test setup; OCP enables testing new behavior without changing stable code.

19) Exceptions and LSP?
    A: Subtypes should not throw broader/unchecked exceptions than base contract or change error semantics.

20) Does ISP increase class count?
    A: Often yes, but reduces coupling and cognitive load for clients. Group by cohesive roles.

Advanced Q&A
21) Design by Contract and LSP?
    A: Model preconditions, postconditions, invariants. Subtypes can’t strengthen preconditions or weaken postconditions.

22) LSP with state and mutability?
    A: Favor immutability or more abstract contracts; avoid subclassing that changes behavioral expectations.

23) DIP at architecture level?
    A: Hexagonal/Clean Architecture: domain (core) depends only on abstractions; adapters implement details.

24) Managing class explosion from OCP/ISP?
    A: Use registries, discovery (ServiceLoader), composition, and sensible defaults to keep extension points ergonomic.

25) Performance impact of SOLID?
    A: Indirection costs exist but are usually negligible; measure before optimizing. Inline hotpaths judiciously.

26) SOLID and concurrency?
    A: SRP for state/coordination separation; explicit thread-safe abstractions; avoid shared mutable state.

27) Over-generalization smell?
    A: Creating abstractions “just in case”. Wait for two-three concrete use cases before abstracting.

28) Relationship to DDD?
    A: SOLID aligns with bounded contexts and aggregates; apply SRP to domain services/entities.

29) Testing doubles and DIP?
    A: DIP enables injecting fakes/mocks/stubs to isolate tests.

30) Real-world example of OCP/DIP?
    A: Logging frameworks: Logger depends on Appender abstraction; new appenders extend without modifying Logger.

Run the main() to see small demonstrations for each principle.
*/

public class _03_InterviewQA {

    public static void main(String[] args) {
        System.out.println("=== SOLID Interview Q&A – Demos ===");
        demoSRP();
        demoOCP();
        demoLSPViolation();
        demoLSPFix();
        demoISP();
        demoDIP();
        System.out.println("=== End ===");
    }

    // =========================
    // SRP – Single Responsibility Principle
    // =========================
    /*
    Bad (violation): One class doing validation, persistence, and emailing.
    class UserManager {
        boolean isValid(User u) {...}
        void save(User u) {...}
        void sendWelcomeEmail(User u) {...}
    }

    Good (SRP): Split into cohesive responsibilities:
    - UserValidator    -> validation
    - UserRepository   -> persistence
    - UserEmailer      -> notification
    - UserRegistrationService orchestrates these, but it’s still one reason to change: the “registration” use case flow.
    */
    private static void demoSRP() {
        System.out.println("\n[SRP] Demo");
        SRP_User user = new SRP_User("alice@example.com", "Alice");
        SRP_UserValidator validator = new SRP_UserValidator();
        SRP_UserRepository repository = new SRP_InMemoryUserRepository();
        SRP_UserEmailer emailer = new SRP_ConsoleUserEmailer();

        SRP_UserRegistrationService service =
                new SRP_UserRegistrationService(validator, repository, emailer);

        service.register(user);
        System.out.println("Registered users count: " + repository.count());
    }

    // SRP classes
    static class SRP_User {
        private final String email;
        private final String name;

        SRP_User(String email, String name) {
            this.email = email;
            this.name = name;
        }

        String getEmail() { return email; }
        String getName() { return name; }
    }

    static class SRP_UserValidator {
        boolean isValid(SRP_User user) {
            return user.getEmail() != null && user.getEmail().contains("@")
                    && user.getName() != null && !user.getName().isBlank();
        }
    }

    interface SRP_UserRepository {
        void save(SRP_User user);
        int count();
    }

    static class SRP_InMemoryUserRepository implements SRP_UserRepository {
        private final List<SRP_User> db = new ArrayList<>();
        @Override public void save(SRP_User user) { db.add(user); }
        @Override public int count() { return db.size(); }
    }

    interface SRP_UserEmailer {
        void sendWelcome(SRP_User user);
    }

    static class SRP_ConsoleUserEmailer implements SRP_UserEmailer {
        @Override public void sendWelcome(SRP_User user) {
            System.out.println("Email -> " + user.getEmail() + ": Welcome, " + user.getName() + "!");
        }
    }

    static class SRP_UserRegistrationService {
        private final SRP_UserValidator validator;
        private final SRP_UserRepository repository;
        private final SRP_UserEmailer emailer;

        SRP_UserRegistrationService(SRP_UserValidator validator,
                                    SRP_UserRepository repository,
                                    SRP_UserEmailer emailer) {
            this.validator = validator;
            this.repository = repository;
            this.emailer = emailer;
        }

        void register(SRP_User user) {
            if (!validator.isValid(user)) {
                throw new IllegalArgumentException("Invalid user");
            }
            repository.save(user);
            emailer.sendWelcome(user);
        }
    }

    // =========================
    // OCP – Open-Closed Principle
    // =========================
    /*
    Bad: A PriceCalculator with a long if-else on discount type.
    Good: Use DiscountPolicy interface. New discount = new class; no modification to PriceCalculator.
    Patterns: Strategy, Decorator, Chain of Responsibility.
    */
    private static void demoOCP() {
        System.out.println("\n[OCP] Demo");
        OCP_DiscountPolicy none = new OCP_NoDiscount();
        OCP_DiscountPolicy seasonal10 = new OCP_PercentageDiscount(new BigDecimal("0.10"));
        OCP_DiscountPolicy loyalty5 = new OCP_FixedDiscount(new BigDecimal("5.00"));
        OCP_DiscountPolicy stacked = new OCP_CompositeDiscount(Arrays.asList(seasonal10, loyalty5));

        OCP_PriceCalculator calc = new OCP_PriceCalculator();

        BigDecimal base = new BigDecimal("50.00");
        System.out.println("No discount: " + calc.finalPrice(base, none));           // 50.00
        System.out.println("10% seasonal: " + calc.finalPrice(base, seasonal10));    // 45.00
        System.out.println("$5 loyalty: " + calc.finalPrice(base, loyalty5));        // 45.00
        System.out.println("Stacked: " + calc.finalPrice(base, stacked));            // (50 - 10% = 45) - 5 = 40
    }

    interface OCP_DiscountPolicy {
        BigDecimal apply(BigDecimal base);
    }

    static class OCP_NoDiscount implements OCP_DiscountPolicy {
        @Override public BigDecimal apply(BigDecimal base) { return base; }
    }

    static class OCP_PercentageDiscount implements OCP_DiscountPolicy {
        private final BigDecimal rate; // 0.10 = 10%
        OCP_PercentageDiscount(BigDecimal rate) { this.rate = rate; }
        @Override public BigDecimal apply(BigDecimal base) {
            return base.subtract(base.multiply(rate));
        }
    }

    static class OCP_FixedDiscount implements OCP_DiscountPolicy {
        private final BigDecimal amount; // fixed amount off
        OCP_FixedDiscount(BigDecimal amount) { this.amount = amount; }
        @Override public BigDecimal apply(BigDecimal base) {
            BigDecimal result = base.subtract(amount);
            return result.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : result;
        }
    }

    static class OCP_CompositeDiscount implements OCP_DiscountPolicy {
        private final List<OCP_DiscountPolicy> policies;
        OCP_CompositeDiscount(List<OCP_DiscountPolicy> policies) { this.policies = policies; }
        @Override public BigDecimal apply(BigDecimal base) {
            BigDecimal result = base;
            for (OCP_DiscountPolicy p : policies) result = p.apply(result);
            return result;
        }
    }

    static class OCP_PriceCalculator {
        BigDecimal finalPrice(BigDecimal base, OCP_DiscountPolicy policy) {
            return policy.apply(base);
        }
    }

    // =========================
    // LSP – Liskov Substitution Principle
    // =========================
    /*
    Classic violation: Square extends Rectangle. A method that sets width/height on a Rectangle
    breaks when a Square is passed (expects independent sides).
    Fix: Avoid inheritance that breaks invariants; use separate types/abstractions or immutability.
    */
    private static void demoLSPViolation() {
        System.out.println("\n[LSP] Violation Demo");
        LSP_Rectangle rect = new LSP_Rectangle();
        rect.setWidth(2);
        rect.setHeight(3);
        System.out.println("Rectangle area: " + lspProcess(rect)); // sets to 5x4 -> 20

        LSP_Rectangle sq = new LSP_Square();
        sq.setWidth(2);
        sq.setHeight(3);
        System.out.println("Square area: " + lspProcess(sq)); // sets to 5x4 but becomes 4x4 -> 16 (unexpected)
    }

    private static int lspProcess(LSP_Rectangle r) {
        // Caller expects independent width/height setters:
        r.setWidth(5);
        r.setHeight(4);
        return r.getArea(); // Expect 20; Square returns 16 -> violates LSP
    }

    static class LSP_Rectangle {
        protected int width;
        protected int height;
        void setWidth(int w) { this.width = w; }
        void setHeight(int h) { this.height = h; }
        int getArea() { return width * height; }
    }

    static class LSP_Square extends LSP_Rectangle {
        @Override void setWidth(int w) { this.width = this.height = w; }
        @Override void setHeight(int h) { this.width = this.height = h; }
    }

    private static void demoLSPFix() {
        System.out.println("\n[LSP] Fix Demo");
        LSP_Shape rect = new LSP_Rectangle2(5, 4);
        LSP_Shape square = new LSP_Square2(4);
        System.out.println("Rectangle area (5x4): " + rect.area()); // 20
        System.out.println("Square area (4): " + square.area());    // 16
        // No setters; invariants preserved. Both are proper subtypes of Shape by contract (area()).
    }

    interface LSP_Shape {
        int area();
    }

    static class LSP_Rectangle2 implements LSP_Shape {
        private final int width;
        private final int height;
        LSP_Rectangle2(int width, int height) { this.width = width; this.height = height; }
        @Override public int area() { return width * height; }
    }

    static class LSP_Square2 implements LSP_Shape {
        private final int side;
        LSP_Square2(int side) { this.side = side; }
        @Override public int area() { return side * side; }
    }

    // =========================
    // ISP – Interface Segregation Principle
    // =========================
    /*
    Bad: A giant MultiFunctionDevice interface forces clients to implement unused methods.
    Good: Split into role-specific interfaces (Printer, Scanner, Fax). Compose where needed.
    */
    private static void demoISP() {
        System.out.println("\n[ISP] Demo");
        ISP_Document doc = new ISP_Document("Hello, ISP!");

        ISP_Printer printer = new ISP_SimplePrinter();
        printer.print(doc);

        ISP_MultiFunctionMachine mfd = new ISP_MultiFunctionMachine(
                new ISP_SimplePrinter(),
                new ISP_SimpleScanner(),
                new ISP_SimpleFax()
        );
        mfd.print(doc);
        mfd.scan(doc);
        mfd.fax(doc);
    }

    static class ISP_Document {
        private final String content;
        ISP_Document(String content) { this.content = content; }
        String content() { return content; }
    }

    interface ISP_Printer {
        void print(ISP_Document doc);
    }

    interface ISP_Scanner {
        void scan(ISP_Document doc);
    }

    interface ISP_Fax {
        void fax(ISP_Document doc);
    }

    static class ISP_SimplePrinter implements ISP_Printer {
        @Override public void print(ISP_Document doc) {
            System.out.println("Printing: " + doc.content());
        }
    }

    static class ISP_SimpleScanner implements ISP_Scanner {
        @Override public void scan(ISP_Document doc) {
            System.out.println("Scanning: " + doc.content());
        }
    }

    static class ISP_SimpleFax implements ISP_Fax {
        @Override public void fax(ISP_Document doc) {
            System.out.println("Faxing: " + doc.content());
        }
    }

    static class ISP_MultiFunctionMachine implements ISP_Printer, ISP_Scanner, ISP_Fax {
        private final ISP_Printer printer;
        private final ISP_Scanner scanner;
        private final ISP_Fax fax;

        ISP_MultiFunctionMachine(ISP_Printer printer, ISP_Scanner scanner, ISP_Fax fax) {
            this.printer = printer;
            this.scanner = scanner;
            this.fax = fax;
        }

        @Override public void print(ISP_Document doc) { printer.print(doc); }
        @Override public void scan(ISP_Document doc) { scanner.scan(doc); }
        @Override public void fax(ISP_Document doc) { fax.fax(doc); }
    }

    // =========================
    // DIP – Dependency Inversion Principle
    // =========================
    /*
    Bad: NotificationService directly instantiates Email/SMS; high-level depends on low-level.
    Good DIP: NotificationService depends on NotificationChannel interface; channels are injected.
    */
    private static void demoDIP() {
        System.out.println("\n[DIP] Demo");
        List<DIP_NotificationChannel> channels = Arrays.asList(
                new DIP_EmailChannel(),
                new DIP_SmsChannel()
        );
        DIP_NotificationService service = new DIP_NotificationService(channels);
        service.notifyAll("bob", "Your order shipped!");
    }

    interface DIP_NotificationChannel {
        void send(String to, String message);
    }

    static class DIP_EmailChannel implements DIP_NotificationChannel {
        @Override public void send(String to, String message) {
            System.out.println("[Email] to " + to + ": " + message);
        }
    }

    static class DIP_SmsChannel implements DIP_NotificationChannel {
        @Override public void send(String to, String message) {
            System.out.println("[SMS] to " + to + ": " + message);
        }
    }

    static class DIP_NotificationService {
        private final List<DIP_NotificationChannel> channels;
        DIP_NotificationService(List<DIP_NotificationChannel> channels) {
            this.channels = channels;
        }
        void notifyAll(String to, String message) {
            for (DIP_NotificationChannel c : channels) c.send(to, message);
        }
    }
}