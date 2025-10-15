package _10_01_reflection_and_annotations;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.AnnotatedTypeVariable;
import java.lang.reflect.AnnotatedWildcardType;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/*
Reflection & Annotations — Theory and practice in one self-contained file.

Reflection:
- Runtime API to inspect and (optionally) manipulate classes, interfaces, records, fields, methods, constructors, parameters, annotations, arrays, generics, modules.
- Key types: Class, Field, Method, Constructor, Parameter, AnnotatedElement, Type (and subtypes: ParameterizedType, TypeVariable, WildcardType, GenericArrayType), Array, Proxy, Module.
- Class objects: MyType.class, obj.getClass(), Class.forName("pkg.MyType"), class loader APIs.
- Class loaders: bootstrap (null), platform, application; parent-first delegation.
- Use cases: frameworks (DI/IoC), ORMs, serialization, testing/mocking, plug-ins, dynamic proxies, runtime discovery.
- Risks: encapsulation breaches (setAccessible), performance overhead, brittle to internal changes, module encapsulation (JDK 9+), security constraints, missing parameter names unless compiled with -parameters.
- Best practices: avoid when simple APIs suffice; prefer public contracts; cache reflective lookups; handle exceptions; be module-friendly (add-opens when necessary); consider MethodHandles for performance-critical paths.

Annotations:
- Structured metadata attached to program elements.
- Meta-annotations: @Retention(SOURCE|CLASS|RUNTIME), @Target(...), @Documented, @Inherited, @Repeatable.
- Targets: TYPE, FIELD, METHOD, PARAMETER, CONSTRUCTOR, LOCAL_VARIABLE, ANNOTATION_TYPE, PACKAGE, TYPE_PARAMETER, TYPE_USE, MODULE, RECORD_COMPONENT.
- Retention:
  - SOURCE: discarded by compiler (not in .class; not visible at runtime).
  - CLASS: stored in .class but not visible via reflection.
  - RUNTIME: visible at runtime via reflection (getAnnotation...).
- Allowed attribute types: primitives, String, Class, enum, annotation, arrays of these.
- Annotation forms: marker (no elements), single-value (usually named 'value'), multi-element with defaults.
- Inheritance: only for type annotations with @Inherited and only across class inheritance (not methods/fields).
- Repeatable: @Repeatable(Container.class); read via getAnnotationsByType.
- Type annotations: annotate any use of a type (TYPE_USE), e.g., List<@NotNull String>; retrievable via AnnotatedType API.
- Compile-time processing: javax.annotation.processing (not shown here); RUNTIME retention not required for compile-time processors (SOURCE/CLASS is typical).

This file demonstrates:
- Introspection of classes/members, modifiers, generics, records, modules, class loaders.
- Accessing and mutating private members (with setAccessible; see JDK 9+ notes).
- Reading annotations (including repeatable, inherited, parameter and type-use annotations).
- Dynamic proxies.
- Arrays via reflection.
- MethodHandles as a modern, faster alternative for dynamic invocation.
*/
public class _01_Theory {

    public static void main(String[] args) throws Throwable {
        System.out.println("=== Reflection & Annotations — Theory Demo ===");

        System.out.println("\n1) Class/Type introspection");
        printClassSummary(Derived.class);

        System.out.println("\n2) Members (fields/methods/constructors) and parameters");
        printMembers(Derived.class);

        System.out.println("\n3) Accessing private members (encapsulation caveat)");
        reflectiveAccessPrivate();

        System.out.println("\n4) Annotations: basics, defaults, and lookup");
        annotationBasics();

        System.out.println("\n5) Repeatable and inherited annotations");
        annotationRepeatableAndInheritance();

        System.out.println("\n6) Parameter and TYPE_USE annotations (Java 8+), TYPE_PARAMETER");
        parameterAndTypeUseAnnotations();

        System.out.println("\n7) Generics reflection (Type, ParameterizedType, TypeVariable, AnnotatedType)");
        genericTypeReflection();

        System.out.println("\n8) Array reflection");
        arrayReflection();

        System.out.println("\n9) Dynamic proxies (java.lang.reflect.Proxy)");
        dynamicProxyDemo();

        System.out.println("\n10) Record reflection (Java 16+)");
        recordReflection();

        System.out.println("\n11) Module and class loader information (Java 9+ modules)");
        moduleAndClassLoaderInfo(Derived.class);

        System.out.println("\n12) MethodHandles vs classic reflection (performance-oriented API)");
        methodHandlesVsReflection();

        System.out.println("\n=== Done ===");
    }

    // ----- Demonstrations -----

    static void printClassSummary(Class<?> cls) {
        System.out.println("Type: " + cls.getName());
        System.out.println("Simple: " + cls.getSimpleName());
        System.out.println("Canonical: " + cls.getCanonicalName());
        System.out.println("Package: " + (cls.getPackage() != null ? cls.getPackage().getName() : "(default)"));
        System.out.println("Modifiers: " + Modifier.toString(cls.getModifiers()));
        System.out.println("Kind: "
                + (cls.isInterface() ? "interface"
                : cls.isEnum() ? "enum"
                : cls.isRecord() ? "record"
                : cls.isAnnotation() ? "annotation"
                : "class"));
        System.out.println("Superclass: " + (cls.getSuperclass() != null ? cls.getSuperclass().getName() : "(none)"));
        System.out.println("Generic superclass: " + (cls.getGenericSuperclass() != null ? cls.getGenericSuperclass() : "(none)"));

        Type[] ifaces = cls.getGenericInterfaces();
        System.out.println("Interfaces (" + ifaces.length + "): " + Arrays.toString(ifaces));

        System.out.println("Nested/member class? member=" + cls.isMemberClass()
                + ", local=" + cls.isLocalClass()
                + ", anonymous=" + cls.isAnonymousClass()
                + ", synthetic=" + cls.isSynthetic());

        System.out.println("Annotations on type:");
        printAnnotations(cls);

        Module module = cls.getModule();
        System.out.println("Module: " + (module != null ? (module.getName() + " (named=" + module.isNamed() + ")") : "(none)"));

        System.out.println("Class loader chain:");
        printClassLoaderChain(cls.getClassLoader());
    }

    static void printMembers(Class<?> cls) {
        // Constructors
        System.out.println("\nDeclared constructors:");
        for (Constructor<?> c : cls.getDeclaredConstructors()) {
            System.out.println(" - " + Modifier.toString(c.getModifiers()) + " " + c.getName()
                    + "(" + typesToString(c.getParameterTypes()) + ")"
                    + (c.isVarArgs() ? " /*varargs*/" : ""));
            printExecutableDetails(c);
        }

        // Fields
        System.out.println("\nDeclared fields:");
        for (Field f : cls.getDeclaredFields()) {
            System.out.println(" - " + Modifier.toString(f.getModifiers()) + " " + f.getType().getTypeName() + " " + f.getName()
                    + " ; generic=" + f.getGenericType().getTypeName());
            printAnnotationsIndented(f);
        }

        // Methods
        System.out.println("\nDeclared methods:");
        for (Method m : cls.getDeclaredMethods()) {
            System.out.println(" - " + Modifier.toString(m.getModifiers()) + " " + m.getReturnType().getTypeName() + " " + m.getName()
                    + "(" + typesToString(m.getParameterTypes()) + ")"
                    + (m.isVarArgs() ? " /*varargs*/" : "")
                    + (m.isDefault() ? " /*default (interface)*/" : "")
                    + (m.isSynthetic() ? " /*synthetic*/" : "")
                    + (m.isBridge() ? " /*bridge*/" : ""));
            Class<?>[] ex = m.getExceptionTypes();
            if (ex.length > 0) {
                System.out.println("   throws: " + Arrays.toString(ex));
            }
            printExecutableDetails(m);
        }
    }

    static void reflectiveAccessPrivate() throws Exception {
        Constructor<PrivateDemo> c = PrivateDemo.class.getDeclaredConstructor(String.class);
        if (!c.canAccess(null)) c.setAccessible(true); // May require --add-opens in Java 9+ across modules
        PrivateDemo instance = c.newInstance("init");
        System.out.println("Constructed PrivateDemo via reflection.");

        Field secret = PrivateDemo.class.getDeclaredField("secret");
        if (!secret.canAccess(instance)) secret.setAccessible(true);
        System.out.println("secret (before): " + secret.get(instance));
        secret.set(instance, "changed");
        System.out.println("secret (after):  " + secret.get(instance));

        Method concat = PrivateDemo.class.getDeclaredMethod("concat", String.class);
        if (!concat.canAccess(instance)) concat.setAccessible(true);
        Object result = concat.invoke(instance, " +suffix");
        System.out.println("Invoked private method result: " + result);

        Field count = PrivateDemo.class.getDeclaredField("count");
        if (!count.canAccess(null)) count.setAccessible(true);
        int before = count.getInt(null);
        count.setInt(null, before + 1);
        System.out.println("Static field 'count': " + before + " -> " + count.getInt(null));
    }

    static void annotationBasics() throws Exception {
        // Accessing type-level annotation
        Demo baseDemo = Base.class.getAnnotation(Demo.class);
        System.out.println("Base @Demo: " + baseDemo);

        // Default values of annotation members
        Object defaultVersion = Demo.class.getMethod("version").getDefaultValue();
        System.out.println("@Demo.version() default: " + defaultVersion);

        // Method-level annotation
        Method baseSayHello = Base.class.getDeclaredMethod("sayHello", String.class);
        Demo demoOnMethod = baseSayHello.getAnnotation(Demo.class);
        System.out.println("Base.sayHello @Demo: " + demoOnMethod);

        // Field-level annotation
        Field secretField = Base.class.getDeclaredField("secretNumber");
        System.out.println("Field 'secretNumber' annotations: " + Arrays.toString(secretField.getAnnotations()));

        // SOURCE-retention annotation is not visible at runtime
        System.out.println("Derived @SourceOnly (runtime): " + Arrays.toString(Derived.class.getAnnotationsByType(SourceOnly.class)));
    }

    static void annotationRepeatableAndInheritance() {
        // Repeatable annotations
        Tag[] baseTags = Base.class.getAnnotationsByType(Tag.class);
        System.out.println("Base @Tag[]: " + Arrays.toString(baseTags));

        Tag[] derivedTags = Derived.class.getAnnotationsByType(Tag.class);
        System.out.println("Derived @Tag[] (not inherited): " + Arrays.toString(derivedTags));

        // @Inherited applies only to type annotations
        Demo derivedDemoViaInheritance = Derived.class.getAnnotation(Demo.class);
        System.out.println("Derived @Demo (inherited): " + derivedDemoViaInheritance);
        System.out.println("Derived declared @Demo: " + Derived.class.getDeclaredAnnotation(Demo.class)); // null

        // Method annotations are NOT inherited
        try {
            Method m = Derived.class.getMethod("sayHello", String.class); // overridden
            System.out.println("Derived.sayHello @Demo (not inherited): " + m.getAnnotation(Demo.class));
        } catch (NoSuchMethodException e) {
            // ignore
        }
    }

    static void parameterAndTypeUseAnnotations() throws Exception {
        Method baseSayHello = Base.class.getDeclaredMethod("sayHello", String.class);
        printParameterAnnotations(baseSayHello);

        Method greet = ParamDemo.class.getDeclaredMethod("greet", String.class);
        printParameterAnnotations(greet);

        // TYPE_USE on generic field
        Field annotated = GenericHolder.class.getDeclaredField("annotatedStrings");
        AnnotatedType at = annotated.getAnnotatedType(); // should be AnnotatedParameterizedType
        System.out.println("Annotated type of GenericHolder.annotatedStrings: " + at.getType().getTypeName());
        if (at instanceof AnnotatedParameterizedType) {
            AnnotatedParameterizedType apt = (AnnotatedParameterizedType) at;
            AnnotatedType[] args = apt.getAnnotatedActualTypeArguments();
            for (int i = 0; i < args.length; i++) {
                System.out.println(" - Arg[" + i + "] type=" + args[i].getType().getTypeName()
                        + ", annotations=" + Arrays.toString(args[i].getAnnotations()));
            }
        }

        // TYPE_PARAMETER annotation on T
        TypeVariable<Class<GenericHolder>>[] tps = GenericHolder.class.getTypeParameters();
        for (TypeVariable<?> tp : tps) {
            System.out.println("Type parameter: " + tp.getName()
                    + ", annotations=" + Arrays.toString(tp.getAnnotations())
                    + ", bounds=" + Arrays.toString(tp.getBounds()));
            // Annotated bounds detail
            if (tp instanceof AnnotatedTypeVariable) {
                AnnotatedTypeVariable atv = (AnnotatedTypeVariable) tp;
                System.out.println(" - Annotated bounds: " + Arrays.toString(atv.getAnnotatedBounds()));
            }
        }
    }

    static void genericTypeReflection() throws Exception {
        Field names = GenericHolder.class.getDeclaredField("names");
        Field scores = GenericHolder.class.getDeclaredField("scores");
        Field value = GenericHolder.class.getDeclaredField("value");

        System.out.println("names generic type: " + names.getGenericType());
        if (names.getGenericType() instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) names.getGenericType();
            System.out.println(" - raw: " + pt.getRawType() + ", args=" + Arrays.toString(pt.getActualTypeArguments()));
        }

        System.out.println("scores generic type: " + scores.getGenericType());
        if (scores.getGenericType() instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) scores.getGenericType();
            System.out.println(" - raw: " + pt.getRawType() + ", args=" + Arrays.toString(pt.getActualTypeArguments()));
        }

        System.out.println("value generic type: " + value.getGenericType());
        if (value.getGenericType() instanceof TypeVariable) {
            TypeVariable<?> tv = (TypeVariable<?>) value.getGenericType();
            System.out.println(" - name: " + tv.getName() + ", bounds=" + Arrays.toString(tv.getBounds()));
        }

        // Wildcards example (if present)
        Field wildcardField = WildcardHolder.class.getDeclaredField("list");
        System.out.println("Wildcard field generic type: " + wildcardField.getGenericType());
        AnnotatedType wat = wildcardField.getAnnotatedType();
        if (wat instanceof AnnotatedParameterizedType) {
            AnnotatedType arg = ((AnnotatedParameterizedType) wat).getAnnotatedActualTypeArguments()[0];
            System.out.println(" - Annotated arg: " + arg.getType().getTypeName());
            if (arg instanceof AnnotatedWildcardType) {
                AnnotatedWildcardType awt = (AnnotatedWildcardType) arg;
                System.out.println("   - Upper bounds: " + Arrays.toString(awt.getAnnotatedUpperBounds()));
                System.out.println("   - Lower bounds: " + Arrays.toString(awt.getAnnotatedLowerBounds()));
            }
        }
    }

    static void arrayReflection() {
        int[] ints = (int[]) Array.newInstance(int.class, 3);
        Array.setInt(ints, 0, 10);
        Array.setInt(ints, 1, 20);
        Array.setInt(ints, 2, 30);
        System.out.println("int[]: " + Arrays.toString(ints) + ", componentType=" + ints.getClass().getComponentType());

        String[] strings = (String[]) Array.newInstance(String.class, 2);
        Array.set(strings, 0, "A");
        Array.set(strings, 1, "B");
        System.out.println("String[]: " + Arrays.toString(strings) + ", componentType=" + strings.getClass().getComponentType());
    }

    static void dynamicProxyDemo() {
        GreetingService target = new GreetingServiceImpl();
        GreetingService proxy = (GreetingService) java.lang.reflect.Proxy.newProxyInstance(
                GreetingService.class.getClassLoader(),
                new Class<?>[]{GreetingService.class},
                new LoggingInvocationHandler(target));

        String msg = proxy.greet("World");
        System.out.println("Proxy result: " + msg);
        System.out.println("Proxy class: " + proxy.getClass());
        System.out.println("Is proxy class? " + java.lang.reflect.Proxy.isProxyClass(proxy.getClass()));
    }

    static void recordReflection() {
        PersonRecord pr = new PersonRecord("Ann", 30);
        System.out.println("Record instance: " + pr);
        Class<?> rc = PersonRecord.class;
        System.out.println("isRecord=" + rc.isRecord());
        RecordComponent[] comps = rc.getRecordComponents();
        System.out.println("Record components (" + comps.length + "):");
        for (RecordComponent c : comps) {
            System.out.println(" - " + c.getName() + " : " + c.getType().getTypeName()
                    + ", getter=" + c.getAccessor().getName()
                    + ", annotations=" + Arrays.toString(c.getAnnotations()));
        }
    }

    static void moduleAndClassLoaderInfo(Class<?> type) {
        Module m = type.getModule();
        System.out.println("Module of " + type.getName() + ": " + (m != null ? m.getName() : "(none)") + ", named=" + (m != null && m.isNamed()));
        System.out.println("Is package exported? (to all) -> " + (m != null && m.isExported(type.getPackageName())));
        System.out.println("Is package open? (to all) -> " + (m != null && m.isOpen(type.getPackageName())));

        System.out.println("Application ClassLoader chain:");
        printClassLoaderChain(type.getClassLoader());
        System.out.println("Context ClassLoader: " + Thread.currentThread().getContextClassLoader());
    }

    static void methodHandlesVsReflection() throws Throwable {
        // Classic reflection
        Method toUpper = String.class.getMethod("toUpperCase");
        String s1 = (String) toUpper.invoke("reflection");
        System.out.println("Reflection invoke: " + s1);

        // MethodHandles (lower overhead after warmup, more direct linkage)
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodType mt = MethodType.methodType(String.class);
        var mh = lookup.findVirtual(String.class, "toUpperCase", mt);
        String s2 = (String) mh.invokeExact("methodhandles");
        System.out.println("MethodHandle invokeExact: " + s2);

        // Adapting types example
        MethodType mtSub = MethodType.methodType(String.class, int.class);
        var mhSub = lookup.findVirtual(String.class, "substring", mtSub);
        String s3 = (String) mhSub.invokeExact("substring", 3);
        System.out.println("MethodHandle substring: " + s3);
    }

    // ----- Utilities -----

    static void printClassLoaderChain(ClassLoader cl) {
        int depth = 0;
        while (cl != null) {
            System.out.println("  [" + depth + "] " + cl);
            cl = cl.getParent();
            depth++;
        }
        System.out.println("  [" + depth + "] bootstrap (null)");
    }

    static String typesToString(Class<?>[] types) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < types.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(types[i].getTypeName());
        }
        return sb.toString();
    }

    static void printAnnotations(AnnotatedElement e) {
        Annotation[] anns = e.getAnnotations();
        if (anns.length == 0) {
            System.out.println(" - (none)");
        } else {
            for (Annotation a : anns) {
                System.out.println(" - " + a);
            }
        }
    }

    static void printAnnotationsIndented(AnnotatedElement e) {
        Annotation[] anns = e.getAnnotations();
        if (anns.length == 0) {
            System.out.println("   annotations: (none)");
        } else {
            System.out.println("   annotations:");
            for (Annotation a : anns) {
                System.out.println("     * " + a);
            }
        }
    }

    static void printExecutableDetails(Executable exec) {
        // Parameters
        Parameter[] params = exec.getParameters();
        System.out.println("   params (" + params.length + "):");
        for (int i = 0; i < params.length; i++) {
            Parameter p = params[i];
            System.out.println("     - [" + i + "] type=" + p.getType().getTypeName()
                    + ", name=" + p.getName()
                    + ", isNamePresent=" + p.isNamePresent());
            Annotation[] pa = p.getAnnotations();
            if (pa.length > 0) {
                System.out.println("       annotations=" + Arrays.toString(pa));
            }
            AnnotatedType pat = p.getAnnotatedType();
            if (pat != null && pat.getAnnotations().length > 0) {
                System.out.println("       type-use annotations=" + Arrays.toString(pat.getAnnotations()));
            }
        }
        // Annotations on the executable itself
        Annotation[] anns = exec.getAnnotations();
        if (anns.length > 0) {
            System.out.println("   annotations on executable: " + Arrays.toString(anns));
        }
    }

    static void printParameterAnnotations(Method m) {
        System.out.println("Method: " + m.getDeclaringClass().getSimpleName() + "." + m.getName());
        Parameter[] params = m.getParameters();
        for (int i = 0; i < params.length; i++) {
            Parameter p = params[i];
            System.out.println(" - param[" + i + "]: " + p.getType().getTypeName()
                    + ", name=" + p.getName() + ", isNamePresent=" + p.isNamePresent());
            System.out.println("   annotations: " + Arrays.toString(p.getAnnotations()));
            System.out.println("   type-use annotations: " + Arrays.toString(p.getAnnotatedType().getAnnotations()));
        }
    }

    // ----- Sample types used in demonstrations -----

    @Documented
    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.CONSTRUCTOR})
    public @interface Demo {
        String value() default "Default";
        int version() default 1;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Repeatable(Tags.class)
    public @interface Tag {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface Tags {
        Tag[] value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    public @interface Immutable {
        // Marker annotation: no elements
    }

    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.TYPE})
    public @interface SourceOnly {
        // Not visible at runtime
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({
            ElementType.TYPE_USE,
            ElementType.TYPE_PARAMETER,
            ElementType.PARAMETER,
            ElementType.FIELD,
            ElementType.RECORD_COMPONENT
    })
    public @interface NotNull {
        // Demonstrates TYPE_USE and TYPE_PARAMETER
    }

    @Demo(value = "base-class", version = 2)
    @Tag("alpha")
    @Tag("beta")
    static class Base {
        public int publicField = 1;
        protected String protectedField = "p";
        @Demo("annotated field")
        private double secretNumber = 42.0;

        @Demo("base constructor")
        public Base() {}

        @Deprecated
        public void oldMethod() {}

        @Demo("base method")
        public String sayHello(@NotNull String name) {
            return "Hello " + name;
        }
    }

    @SourceOnly // Not visible at runtime
    static class Derived extends Base {
        @Override
        public String sayHello(String name) {
            return super.sayHello(name) + " from Derived";
        }
    }

    static class PrivateDemo {
        private String secret;
        private static int count = 0;

        private PrivateDemo(String initial) {
            this.secret = initial;
        }

        private String concat(String s) {
            return secret + s;
        }
    }

    static class ParamDemo {
        public void greet(@NotNull String name) {
            // parameter annotated
        }
    }

    static class GenericHolder<@NotNull T> {
        List<String> names = new ArrayList<>();
        HashMap<String, Integer> scores = new HashMap<>();
        T value;
        List<@NotNull String> annotatedStrings = new ArrayList<>();
    }

    static class WildcardHolder {
        List<? extends Number> list = new ArrayList<>();
    }

    @Immutable
    public static record PersonRecord(@NotNull String name, int age) {}

    interface GreetingService {
        @Demo("service method")
        String greet(@NotNull String name);
    }

    static class GreetingServiceImpl implements GreetingService {
        @Override
        public String greet(String name) {
            return "Hello, " + name;
        }
    }

    static class LoggingInvocationHandler implements InvocationHandler {
        private final Object target;

        LoggingInvocationHandler(Object target) {
            this.target = target;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            System.out.println("Proxy intercept -> " + method.getName()
                    + ", annotations=" + Arrays.toString(method.getAnnotations()));
            if (args != null) {
                System.out.println("  args=" + Arrays.toString(args));
            }
            Object result = method.invoke(target, args);
            System.out.println("  result=" + result);
            return result;
        }
    }
}