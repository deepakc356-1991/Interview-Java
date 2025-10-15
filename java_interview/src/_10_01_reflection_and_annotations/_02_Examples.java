package _10_01_reflection_and_annotations;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Reflection & Annotations - coding examples with concise explanations.
 * Run main to see outputs for each section.
 */
public class _02_Examples {

    public static void main(String[] args) throws Exception {
        section("A. Class metadata");
        classMetadata();

        section("B. Fields - reading/writing");
        fieldsExample();

        section("C. Methods - invoking");
        methodsExample();

        section("D. Constructors - instantiation");
        constructorsExample();

        section("E. Parameters & validation via annotations");
        parametersAndValidation();

        section("F. Generics & TYPE_USE annotations");
        genericsAndTypeUse();

        section("G. Annotations: read, repeatable, inherited");
        annotationsExamples();

        section("H. Dynamic proxies");
        dynamicProxyExample();
    }

    // === Sections ===

    static void section(String title) {
        System.out.println("\n=== " + title + " ===");
    }

    // A. Class metadata
    static void classMetadata() throws Exception {
        // 3 ways to obtain Class
        Class<?> c1 = Person.class;                        // compile-time
        Class<?> c2 = new Person().getClass();             // runtime
        Class<?> c3 = Class.forName(Person.class.getName());// by FQN

        System.out.println("Name: " + c1.getName());
        System.out.println("Simple: " + c1.getSimpleName());
        System.out.println("Modifiers: " + Modifier.toString(c1.getModifiers()));
        System.out.println("Package: " + c1.getPackageName());
        System.out.println("Same instance across lookups: " + (c1 == c2 && c2 == c3));

        // Superclass and interfaces
        Class<?> child = ChildComponent.class;
        System.out.println("Child superclass: " + child.getSuperclass().getSimpleName());
        System.out.println("Interfaces: " + Arrays.stream(c1.getInterfaces())
                .map(Class::getSimpleName).collect(Collectors.joining(", ")));

        // Public vs declared members
        System.out.println("getFields (public, incl. inherited): " + Person.class.getFields().length);
        System.out.println("getDeclaredFields (all in class): " + Person.class.getDeclaredFields().length);
    }

    // B. Fields - reading/writing (including private)
    static void fieldsExample() throws Exception {
        Person p = new Person("Ada", 42);

        // Public static field (no setAccessible needed)
        Field sf = Person.class.getField("species");
        System.out.println("Static field species: " + sf.get(null));

        // Private instance field
        Field f = Person.class.getDeclaredField("name");
        f.setAccessible(true); // bypass access checks
        System.out.println("Before: name=" + f.get(p));
        f.set(p, "Grace");
        System.out.println("After: name=" + f.get(p));

        // Field annotations (validation metadata)
        Field ageField = Person.class.getDeclaredField("age");
        Range range = ageField.getAnnotation(Range.class);
        System.out.println("@Range on age: [" + range.min() + ", " + range.max() + "]");
    }

    // C. Methods - invoking (instance, private, static)
    static void methodsExample() throws Exception {
        Person p = new Person("Linus", 30);

        // Private instance method invoke
        Method greet = Person.class.getDeclaredMethod("greet", String.class);
        greet.setAccessible(true);
        String g = (String) greet.invoke(p, "Hello");
        System.out.println("greet result: " + g);

        // Static method invoke
        Method retire = Person.class.getMethod("yearsUntilRetirement", int.class);
        Object yrs = retire.invoke(null, 40);
        System.out.println("yearsUntilRetirement(40): " + yrs);

        // Parameter metadata (requires javac -parameters for real names)
        System.out.println("Method parameters:");
        for (Parameter param : greet.getParameters()) {
            System.out.println(" - " + param.getName() + ", annotations=" +
                    Arrays.stream(param.getAnnotations())
                            .map(a -> "@" + a.annotationType().getSimpleName())
                            .collect(Collectors.joining(", ")));
        }

        // Read method-level annotations
        Method echo = BaseComponent.class.getMethod("echo", String.class);
        Author author = echo.getAnnotation(Author.class);
        System.out.println("echo authored by: " + author.name());
    }

    // D. Constructors - instantiation + annotations
    static void constructorsExample() throws Exception {
        // No-arg constructor
        Constructor<Person> noArg = Person.class.getDeclaredConstructor();
        Person p1 = noArg.newInstance();
        System.out.println("Constructed via no-arg: " + p1);

        // Parameterized constructor with annotations
        Constructor<Person> ctor = Person.class.getDeclaredConstructor(String.class, int.class);
        if (ctor.isAnnotationPresent(Inject.class)) {
            System.out.println("@Inject present on ctor(String,int)");
        }
        Person p2 = ctor.newInstance("Ada", 45);
        System.out.println("Constructed via annotated ctor: " + p2);
    }

    // E. Parameters & validation via custom annotations
    static void parametersAndValidation() throws Exception {
        Person p = new Person("Alan", 50);

        Method greet = Person.class.getDeclaredMethod("greet", String.class);
        greet.setAccessible(true);
        System.out.println("Validated call (ok): " +
                invokeWithValidation(greet, p, "Hi"));

        try {
            invokeWithValidation(greet, p, (Object) null);
        } catch (IllegalArgumentException e) {
            System.out.println("Validated call (null arg) failed: " + e.getMessage());
        }

        Method echo = BaseComponent.class.getMethod("echo", String.class);
        System.out.println("Validated echo: " + invokeWithValidation(echo, new BaseComponent(), "EchoMe"));
    }

    // F. Generics & TYPE_USE annotations
    static void genericsAndTypeUse() throws Exception {
        // Fields with generics and @TypeUseAnno
        Field items = GenericRepository.class.getDeclaredField("items");
        Field index = GenericRepository.class.getDeclaredField("index");

        System.out.println("items generic type: " + items.getGenericType());
        System.out.println("items annotated type: " + annotatedTypeToString(items.getAnnotatedType()));

        System.out.println("index generic type: " + index.getGenericType());
        System.out.println("index annotated type: " + annotatedTypeToString(index.getAnnotatedType()));

        // Method return type annotations
        Method snapshot = GenericRepository.class.getMethod("snapshot");
        System.out.println("snapshot return generic type: " + snapshot.getGenericReturnType());
        System.out.println("snapshot annotated return type: " + annotatedTypeToString(snapshot.getAnnotatedReturnType()));
    }

    // G. Annotations: read, repeatable, inherited
    static void annotationsExamples() throws Exception {
        // Repeatable
        Tag[] tags1 = WithTags.class.getAnnotationsByType(Tag.class);
        System.out.println("WithTags @Tag values: " + Arrays.stream(tags1).map(Tag::value).collect(Collectors.joining(", ")));

        // Inherited vs declared
        FrameworkComponent onChild = ChildComponent.class.getAnnotation(FrameworkComponent.class);
        FrameworkComponent declaredOnChild = ChildComponent.class.getDeclaredAnnotation(FrameworkComponent.class);
        System.out.println("@FrameworkComponent on child (inherited): " + (onChild != null));
        System.out.println("Declared on child directly: " + (declaredOnChild != null));

        // CLASS-retention annotation is not visible at runtime
        ClassRetentionOnly cro = WithTags.class.getAnnotation(ClassRetentionOnly.class);
        System.out.println("@ClassRetentionOnly visible at runtime? " + (cro != null));

        // Read annotation defaults
        Method m = CalculatorService.class.getMethod("add", int.class, int.class);
        Endpoint ep = m.getAnnotation(Endpoint.class);
        System.out.println("@Endpoint path=" + ep.path() + ", secure=" + ep.secure());
    }

    // H. Dynamic proxies
    static void dynamicProxyExample() {
        CalculatorService real = new CalculatorServiceImpl();
        CalculatorService proxy = (CalculatorService) Proxy.newProxyInstance(
                CalculatorService.class.getClassLoader(),
                new Class<?>[]{CalculatorService.class},
                new LoggingHandler(real));

        System.out.println("Proxy add(3,4): " + proxy.add(3, 4));
        try {
            proxy.add(-1, 2); // violates @Range(min=0)
        } catch (IllegalArgumentException e) {
            System.out.println("Proxy validation failed: " + e.getMessage());
        }
    }

    // === Helpers ===

    static Object invokeWithValidation(Method m, Object target, Object... args) throws Exception {
        validateArgs(m, args);
        m.setAccessible(true);
        return m.invoke(target, args);
    }

    static void validateArgs(Executable exec, Object... args) {
        Parameter[] params = exec.getParameters();
        if (params.length != args.length) {
            throw new IllegalArgumentException("Arity mismatch");
        }
        for (int i = 0; i < params.length; i++) {
            Parameter p = params[i];
            Object arg = args[i];
            NotNull nn = p.getAnnotation(NotNull.class);
            if (nn != null && arg == null) {
                throw new IllegalArgumentException("Param " + p.getName() + " must not be null");
            }
            Range r = p.getAnnotation(Range.class);
            if (r != null) {
                if (!(arg instanceof Number)) {
                    throw new IllegalArgumentException("Param " + p.getName() + " must be numeric");
                }
                long v = ((Number) arg).longValue();
                if (v < r.min() || v > r.max()) {
                    throw new IllegalArgumentException("Param " + p.getName() + " out of range [" + r.min() + "," + r.max() + "]: " + v);
                }
            }
        }
    }

    static String annotatedTypeToString(AnnotatedType at) {
        String anns = Arrays.stream(at.getAnnotations())
                .map(a -> "@" + a.annotationType().getSimpleName())
                .collect(Collectors.joining(" "));
        Type type = at.getType();
        String core = typeToString(type);
        if (at instanceof AnnotatedParameterizedType) {
            AnnotatedParameterizedType apt = (AnnotatedParameterizedType) at;
            ParameterizedType pt = (ParameterizedType) apt.getType();
            AnnotatedType[] args = apt.getAnnotatedActualTypeArguments();
            String argsStr = Arrays.stream(args).map(_02_Examples::annotatedTypeToString).collect(Collectors.joining(", "));
            core = typeToString(pt.getRawType()) + "<" + argsStr + ">";
        }
        if (!anns.isEmpty()) return anns + " " + core;
        return core;
    }

    static String typeToString(Type t) {
        if (t instanceof Class) return ((Class<?>) t).getSimpleName();
        if (t instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) t;
            String raw = typeToString(pt.getRawType());
            String args = Arrays.stream(pt.getActualTypeArguments()).map(_02_Examples::typeToString).collect(Collectors.joining(", "));
            return raw + "<" + args + ">";
        }
        return t.getTypeName();
    }
}

// === Sample annotations ===

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR})
@interface Author {
    String name();
    String date() default "";
    String[] reviewers() default {};
}

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Repeatable(Tags.class)
@interface Tag { String value(); }

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@interface Tags { Tag[] value(); }

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@interface FrameworkComponent { String value() default ""; }

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD})
@interface NotNull {}

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD})
@interface Range { long min() default Long.MIN_VALUE; long max() default Long.MAX_VALUE; }

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@interface TypeUseAnno { String value() default "TU"; }

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
@interface ClassRetentionOnly {}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface Endpoint {
    String path();
    boolean secure() default true;
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.CONSTRUCTOR)
@interface Inject {}

// === Sample classes/interfaces used in examples ===

@FrameworkComponent("base")
class BaseComponent {
    @Range(min = 0, max = 120)
    private int age;

    @Author(name = "Alice", date = "2024-01-01")
    public String echo(@NotNull String s) { return "Echo: " + s; }
}

class ChildComponent extends BaseComponent {}

@Tag("alpha")
@Tag("beta")
@ClassRetentionOnly
class WithTags {}

class Person {
    public static String species = "Homo sapiens";

    private String name;
    private int age;

    public Person() {
        this("Unknown", 0);
    }

    @Inject
    public Person(@NotNull String name, @Range(min = 0, max = 150) int age) {
        this.name = name;
        this.age = age;
    }

    private String greet(@NotNull String prefix) {
        return prefix + " " + name;
    }

    public static int yearsUntilRetirement(@Range(min = 0) int age) {
        return Math.max(65 - age, 0);
    }

    @Override public String toString() { return "Person{name='" + name + "', age=" + age + "}"; }
}

class GenericRepository<T> {
    List<@TypeUseAnno T> items = new ArrayList<>();
    Map<@TypeUseAnno String, List<@TypeUseAnno T>> index = new HashMap<>();

    public void add(@NotNull T t) { items.add(t); }

    @Author(name = "Carol")
    public Map<@TypeUseAnno String, List<@TypeUseAnno T>> snapshot() { return new HashMap<>(index); }
}

interface CalculatorService {
    @Author(name = "Bob")
    @Endpoint(path = "/add")
    int add(@Range(min = 0) int a, @Range(min = 0) int b);
}

class CalculatorServiceImpl implements CalculatorService {
    @Override public int add(int a, int b) { return a + b; }
}

// InvocationHandler that logs and enforces @NotNull/@Range on interface methods
class LoggingHandler implements InvocationHandler {
    private final Object target;

    LoggingHandler(Object target) { this.target = target; }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Endpoint ep = method.getAnnotation(Endpoint.class);
        if (ep != null) {
            System.out.println("Calling " + ep.path() + " (secure=" + ep.secure() + ")");
        }
        if (args == null) args = new Object[0];
        // Validate using parameter annotations on interface method
        _02_Examples.validateArgs(method, args);
        System.out.println("Invoke: " + method.getName() + " args=" + Arrays.toString(args));
        return method.invoke(target, args);
    }
}