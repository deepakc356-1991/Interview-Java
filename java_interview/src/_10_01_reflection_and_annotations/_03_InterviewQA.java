package _10_01_reflection_and_annotations;

import java.lang.annotation.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.*;
import java.util.*;

/*
Reflection & Annotations Interview Q&A (Basic → Intermediate → Advanced)

BASICS
Q1: What is Reflection?
A: Reflection is the ability of a program to inspect and manipulate itself at runtime (types, fields, methods, constructors, annotations).

Q2: How do you get a Class<?> object?
A: 1) Type literal: MyType.class  2) From instance: obj.getClass()  3) By name: Class.forName("pkg.MyType")  4) ClassLoader: loader.loadClass(name).

Q3: What can you do with Reflection?
A: Inspect members (fields, methods, constructors), instantiate objects, access/modify fields, invoke methods, handle arrays, read annotations.

Q4: getXxx vs getDeclaredXxx
A: getXxx returns public members including inherited. getDeclaredXxx returns all members declared in the class (any access), excluding inherited.

Q5: How to instantiate reflectively?
A: Use Constructor<T>.newInstance(args...) after obtaining with getConstructor or getDeclaredConstructor.

Q6: How to access private members?
A: Use setAccessible(true) on Field/Method/Constructor, then get/set or invoke. Use with caution (modules/security).

Q7: Performance considerations?
A: Reflection is slower and bypasses compile-time checks. Cache reflective lookups and prefer MethodHandles for faster invocation when possible.

Q8: What are Annotations?
A: Structured metadata attached to program elements. Defined via @interface. Elements can be primitives, String, Class, enum, annotation, or arrays thereof.

Q9: Retention policies?
A: SOURCE (discarded at compile time), CLASS (in bytecode, not visible at runtime), RUNTIME (available to reflection at runtime).

Q10: Targets?
A: @Target controls where an annotation can appear: TYPE, FIELD, METHOD, PARAMETER, CONSTRUCTOR, LOCAL_VARIABLE, ANNOTATION_TYPE, PACKAGE, TYPE_USE, TYPE_PARAMETER, etc.

INTERMEDIATE
Q11: @Inherited?
A: Allows class-level annotations to be inherited by subclasses (retrieved via getAnnotation, not getDeclaredAnnotation).

Q12: Repeatable Annotations?
A: Use @Repeatable(Container.class). Retrieve with getAnnotationsByType.

Q13: Type-use Annotations?
A: Annotations that can be applied to any use of a type (e.g., List<@NonNull String>). Access with AnnotatedType APIs.

Q14: Parameter reflection?
A: getParameters() returns Parameter[] with annotations. Parameter names require compilation with -parameters to be present at runtime.

Q15: Generic type info at runtime?
A: Use Field.getGenericType / Method.getGenericReturnType -> Type: ParameterizedType, TypeVariable, WildcardType, GenericArrayType.

Q16: Dynamic Proxies?
A: Proxy.newProxyInstance creates runtime implementations of interfaces. Handled by InvocationHandler. Useful for AOP, logging, transactions.

Q17: Difference: reflection vs MethodHandles?
A: MethodHandles are faster and more strongly-typed, introduced for invokedynamic. Reflection is more general but slower.

Q18: Exceptions in reflection?
A: Typical: ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException (wraps the target exception), InstantiationException.

Q19: Reading default values from annotations?
A: Use AnnotationType.class.getMethod("element").getDefaultValue().

Q20: isBridge/isSynthetic?
A: Bridge methods are compiler-generated to preserve polymorphism with generics. Synthetic members are compiler-generated artifacts.

ADVANCED
Q21: Reflection with modules (Java 9+)?
A: Accessing non-exported packages via setAccessible may require --add-opens or module openness. Illegal reflective access may warn/fail.

Q22: equals/hashCode with proxies?
A: Dynamic proxies implement equals, hashCode, toString via InvocationHandler; you should handle them appropriately.

Q23: Annotation processing vs runtime reflection?
A: Compile-time processing uses javax.annotation.processing. Runtime reflection reads RUNTIME-retained annotations.

Q24: Field modifiers and final fields?
A: You can reflectively attempt to modify final fields, but semantics are not guaranteed and may be restricted. Avoid.

Q25: Default methods and reflection?
A: Interface default methods are detected via Method.isDefault() on the interface Method object.

Q26: Security implications?
A: Reflection can break encapsulation and should be used judiciously. Respect security policies, modules, and APIs.

Q27: Avoid memory leaks with class loaders?
A: Don’t keep strong refs to Class, ClassLoader, or Method objects unnecessarily, especially in containers.

Q28: Package and module annotations?
A: Package annotations are placed in package-info.java and read via Package.getAnnotations. Module annotations via Module API (Java 9+).

Q29: Arrays via reflection?
A: java.lang.reflect.Array can create and manipulate arrays of unknown component types at runtime.

Q30: When to use reflection?
A: Frameworks, DI, ORM, serialization, testing, tooling. Not for normal business logic.

The rest of this file demonstrates the above with runnable examples and inline comments.
*/
public class _03_InterviewQA {

    public static void main(String[] args) throws Throwable {
        println("=== Reflection & Annotations Interview Q&A Demo ===");

        showClassObjectWays();
        showMembersBasics();
        showAccessingPrivateMembers();
        showArraysReflection();

        showAnnotationsBasics();
        showRepeatableAndInherited();
        showReadingAnnotationDefaults();

        showTypeUseAndGenerics();
        showParameterReflection();

        showDynamicProxyDemo();
        showMethodHandlesDemo();

        showSimpleValidationFramework();
        showBridgeAndSynthetic();

        println("=== Done ===");
    }

    // ========= BASIC DEMOS =========

    /*
    Q: How to obtain Class<?>?
    A: Demonstrates 4 common ways.
    */
    static void showClassObjectWays() throws Exception {
        println("\n-- Class object acquisition --");
        // 1) Type literal
        Class<User> c1 = User.class;
        // 2) From instance
        User u = new User(42, "Alice");
        Class<?> c2 = u.getClass();
        // 3) By fully qualified name
        Class<?> c3 = Class.forName("_10_01_reflection_and_annotations._03_InterviewQA$User");
        // 4) ClassLoader
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Class<?> c4 = loader.loadClass("_10_01_reflection_and_annotations._03_InterviewQA$User");

        println("1) " + c1.getName());
        println("2) " + c2.getName());
        println("3) " + c3.getName());
        println("4) " + c4.getName());
    }

    /*
    Q: How to inspect members and instantiate reflectively?
    A: Shows constructors, fields, methods, modifiers, and invocation.
    */
    static void showMembersBasics() throws Exception {
        println("\n-- Members basics --");
        Class<User> clazz = User.class;

        // Constructors
        for (Constructor<?> cons : clazz.getDeclaredConstructors()) {
            println("Constructor: " + cons + " | modifiers=" + Modifier.toString(cons.getModifiers()));
        }

        // Instantiate with public constructor
        Constructor<User> ctor = clazz.getConstructor(long.class, String.class);
        User user = ctor.newInstance(7L, "Bob");
        println("Instantiated: " + user.getClass().getSimpleName());

        // Fields
        println("Public fields (incl inherited):");
        for (Field f : clazz.getFields()) {
            println("  " + f.getName() + " : " + f.getType().getSimpleName());
        }

        println("Declared fields (this class only):");
        for (Field f : clazz.getDeclaredFields()) {
            println("  " + f.getName() + " : " + f.getType().getSimpleName() + " | modifiers=" + Modifier.toString(f.getModifiers()));
        }

        // Methods
        println("Public methods (incl inherited):");
        for (Method m : clazz.getMethods()) {
            if (m.getDeclaringClass() == Object.class) continue; // skip
            println("  " + m.getName() + sig(m) + " | declaredIn=" + m.getDeclaringClass().getSimpleName());
        }

        // Static invocation
        Method add = clazz.getMethod("add", int.class, int.class);
        Object sum = add.invoke(null, 3, 5);
        println("Invoked static add: " + sum);
    }

    /*
    Q: How to access private fields/methods?
    A: Use setAccessible(true) if allowed.
    */
    static void showAccessingPrivateMembers() throws Exception {
        println("\n-- Access private members --");
        User user = new User(1L, "Carol");

        // Private field
        Field id = User.class.getDeclaredField("id");
        id.setAccessible(true);
        long before = id.getLong(user);
        id.setLong(user, 100L);
        long after = id.getLong(user);
        println("Private field 'id' before=" + before + ", after=" + after);

        // Private method
        Method secret = User.class.getDeclaredMethod("secretSauce", int.class, int.class);
        secret.setAccessible(true);
        Object result = secret.invoke(user, 2, 3);
        println("Invoked private method result: " + result);
    }

    /*
    Q: How to reflect on arrays?
    A: Use java.lang.reflect.Array for dynamic creation and access.
    */
    static void showArraysReflection() {
        println("\n-- Arrays via reflection --");
        Object intArray = Array.newInstance(int.class, 3);
        Array.setInt(intArray, 0, 10);
        Array.setInt(intArray, 1, 20);
        Array.setInt(intArray, 2, 30);
        println("intArray[1]=" + Array.getInt(intArray, 1));

        Object twoD = Array.newInstance(String.class, 2, 2);
        Array.set(Array.get(twoD, 0), 0, "a");
        Array.set(Array.get(twoD, 0), 1, "b");
        println("2D[0][1]=" + Array.get(Array.get(twoD, 0), 1));
    }

    // ========= ANNOTATIONS =========

    /*
    Q: How to declare and read annotations?
    A: Demonstrates retention, targets, presence, and fetching.
    */
    static void showAnnotationsBasics() {
        println("\n-- Annotations basics --");
        Class<User> clazz = User.class;

        // Class-level annotations
        println("All annotations on User (incl inherited): " + Arrays.toString(clazz.getAnnotations()));
        println("@Audited? " + (clazz.getAnnotation(Audited.class) != null));
        println("@ClassRetentionAnno visible at runtime? " + (clazz.getAnnotation(ClassRetentionAnno.class) != null));

        // Field/method annotations
        try {
            Field name = User.class.getDeclaredField("name");
            println("Field 'name' annotations: " + Arrays.toString(name.getAnnotations()));

            Method greet = User.class.getDeclaredMethod("greet", String.class);
            println("Method 'greet' annotations: " + Arrays.toString(greet.getAnnotations()));
            println("Method 'greet' is annotated @Loggable? " + greet.isAnnotationPresent(Loggable.class));
        } catch (NoSuchFieldException | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    /*
    Q: How to use repeatable and inherited annotations?
    A: @Role is repeatable; @Audited is inherited from BaseEntity to User.
    */
    static void showRepeatableAndInherited() {
        println("\n-- Repeatable and Inherited annotations --");
        Class<User> clazz = User.class;

        Role[] roles = clazz.getAnnotationsByType(Role.class);
        println("Roles on User: " + Arrays.toString(Arrays.stream(roles).map(Role::value).toArray()));

        // @Audited is on BaseEntity (superclass), visible on subclass via getAnnotation
        Audited audited = clazz.getAnnotation(Audited.class);
        println("@Audited present via inheritance? " + (audited != null) + " value=" + (audited != null ? audited.value() : "n/a"));

        // Difference: getDeclaredAnnotations ignores inheritance
        println("Declared annotations on User (no inherited): " + Arrays.toString(clazz.getDeclaredAnnotations()));
    }

    /*
    Q: How to read default values from an annotation type?
    A: Use reflection on the annotation interface methods.
    */
    static void showReadingAnnotationDefaults() throws Exception {
        println("\n-- Reading annotation default values --");
        Object defaultFlags = MyRuntimeAnno.class.getMethod("flags").getDefaultValue();
        println("MyRuntimeAnno.flags default = " + Arrays.toString((int[]) defaultFlags));
    }

    // ========= GENERICS & TYPE-USE =========

    /*
    Q: How to inspect generic signatures and type-use annotations?
    A: Use Field.getGenericType and Field.getAnnotatedType with AnnotatedType APIs.
    */
    static void showTypeUseAndGenerics() throws Exception {
        println("\n-- Generics and Type-use annotations --");

        Field tags = User.class.getField("tags");
        printGeneric("User.tags", tags.getGenericType());

        Field cov = User.class.getDeclaredField("numbersCovariant");
        printGeneric("User.numbersCovariant", cov.getGenericType());

        Field contra = User.class.getDeclaredField("numbersContravariant");
        printGeneric("User.numbersContravariant", contra.getGenericType());

        Field score = User.class.getDeclaredField("scoreByName");
        printGeneric("User.scoreByName", score.getGenericType());

        // Type parameters
        println("Box<T> type parameter(s): " + Arrays.toString(Box.class.getTypeParameters()));

        // Type-use annotations
        Field map = TypeUseDemo.class.getDeclaredField("map");
        AnnotatedType at = map.getAnnotatedType();
        println("TypeUseDemo.map annotated type: " + annotatedTypeToString(at));

        Field arrOfList = TypeUseDemo.class.getDeclaredField("arrayOfList");
        println("TypeUseDemo.arrayOfList annotated type: " + annotatedTypeToString(arrOfList.getAnnotatedType()));
    }

    // ========= PARAMETERS =========

    /*
    Q: How to inspect parameters and their annotations?
    A: Use Executable.getParameters. Parameter names need -parameters at compile time.
    */
    static void showParameterReflection() throws Exception {
        println("\n-- Parameter reflection --");
        Method greet = User.class.getDeclaredMethod("greet", String.class);
        for (Parameter p : greet.getParameters()) {
            println("Param: name=" + p.getName() + ", type=" + p.getType().getSimpleName() + ", annos=" + Arrays.toString(p.getAnnotations()));
        }

        Method varargs = User.class.getDeclaredMethod("varargsMethod", String.class, Integer[].class);
        println("varargsMethod isVarArgs? " + varargs.isVarArgs());
    }

    // ========= DYNAMIC PROXIES =========

    /*
    Q: How to build a dynamic proxy?
    A: Use Proxy.newProxyInstance for interfaces; implement behavior in InvocationHandler.
    */
    static void showDynamicProxyDemo() {
        println("\n-- Dynamic Proxy --");
        Calculator target = new CalculatorImpl();
        Calculator proxy = (Calculator) Proxy.newProxyInstance(
                Calculator.class.getClassLoader(),
                new Class<?>[]{Calculator.class},
                new LoggingHandler(target)
        );

        int a = proxy.add(2, 3); // annotated with @Loggable -> logged
        int m = proxy.mul(4, 5); // not annotated -> no log
        println("Proxy results: add=" + a + ", mul=" + m);
    }

    // ========= METHOD HANDLES =========

    /*
    Q: How to invoke with MethodHandles?
    A: Faster, strongly-typed handles (no boxing overhead when exact types match).
    */
    static void showMethodHandlesDemo() throws Throwable {
        println("\n-- MethodHandles --");
        MethodHandles.Lookup lookup = MethodHandles.lookup();

        // Instance method: User#greet(String)
        MethodHandle mhGreet = lookup.findVirtual(User.class, "greet",
                MethodType.methodType(String.class, String.class));
        String greet = (String) mhGreet.invokeExact(new User(9, "Dora"), "Team");
        println("MH greet: " + greet);

        // Static method: User#add(int,int)
        MethodHandle mhAdd = lookup.findStatic(User.class, "add",
                MethodType.methodType(int.class, int.class, int.class));
        int sum = (int) mhAdd.invokeExact(4, 7);
        println("MH add: " + sum);
    }

    // ========= MINI VALIDATION =========

    /*
    Q: How to implement a simple annotation-driven validator?
    A: Scan fields for @NotNull and @Min and collect errors.
    */
    static void showSimpleValidationFramework() throws IllegalAccessException {
        println("\n-- Simple validation via annotations --");
        User u = new User(0, null); // invalid: id < 1 and name null
        SimpleValidator validator = new SimpleValidator();
        List<String> errors = validator.validate(u);
        println("Errors: " + errors);
    }

    // ========= ADVANCED: BRIDGE & SYNTHETIC =========

    /*
    Q: How to detect compiler-generated bridge/synthetic methods?
    A: isBridge() and isSynthetic(). Default interface methods are isDefault() on interface Method.
    */
    static void showBridgeAndSynthetic() throws Exception {
        println("\n-- Bridge and synthetic methods --");
        for (Method m : User.class.getDeclaredMethods()) {
            if (m.isBridge() || m.isSynthetic()) {
                println("Method: " + m.getName() + " | bridge=" + m.isBridge() + ", synthetic=" + m.isSynthetic());
            }
        }
        // Default method reflection
        Method def = Greeter.class.getMethod("greeting");
        println("Greeter#greeting isDefault? " + def.isDefault());
    }

    // ========= UTILITIES =========

    private static void printGeneric(String label, Type t) {
        println(label + " -> " + typeToString(t));
        if (t instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) t;
            Type[] typeArgs = pt.getActualTypeArguments();
            for (int i = 0; i < typeArgs.length; i++) {
                Type arg = typeArgs[i];
                println("  arg[" + i + "] = " + typeToString(arg));
                if (arg instanceof WildcardType) {
                    WildcardType w = (WildcardType) arg;
                    println("    upperBounds=" + Arrays.toString(Arrays.stream(w.getUpperBounds()).map(_03_InterviewQA::typeToString).toArray()));
                    println("    lowerBounds=" + Arrays.toString(Arrays.stream(w.getLowerBounds()).map(_03_InterviewQA::typeToString).toArray()));
                }
            }
        } else if (t instanceof TypeVariable) {
            TypeVariable<?> tv = (TypeVariable<?>) t;
            println("  typeVar name=" + tv.getName() + ", bounds=" + Arrays.toString(Arrays.stream(tv.getBounds()).map(_03_InterviewQA::typeToString).toArray()));
        }
    }

    private static String typeToString(Type t) {
        if (t instanceof Class) return ((Class<?>) t).getTypeName();
        if (t instanceof ParameterizedType) {
            ParameterizedType p = (ParameterizedType) t;
            return p.getRawType().getTypeName() + "<" + Arrays.toString(Arrays.stream(p.getActualTypeArguments()).map(_03_InterviewQA::typeToString).toArray()) + ">";
        }
        if (t instanceof WildcardType) {
            WildcardType w = (WildcardType) t;
            String up = Arrays.toString(Arrays.stream(w.getUpperBounds()).map(_03_InterviewQA::typeToString).toArray());
            String lo = Arrays.toString(Arrays.stream(w.getLowerBounds()).map(_03_InterviewQA::typeToString).toArray());
            return "? extends " + up + " super " + lo;
        }
        if (t instanceof TypeVariable) {
            TypeVariable<?> tv = (TypeVariable<?>) t;
            return tv.getName();
        }
        if (t instanceof GenericArrayType) {
            GenericArrayType ga = (GenericArrayType) t;
            return typeToString(ga.getGenericComponentType()) + "[]";
        }
        return t.getTypeName();
    }

    private static String annotatedTypeToString(AnnotatedType at) {
        String annos = Arrays.toString(at.getAnnotations());
        if (at instanceof AnnotatedParameterizedType) {
            AnnotatedParameterizedType apt = (AnnotatedParameterizedType) at;
            AnnotatedType[] args = apt.getAnnotatedActualTypeArguments();
            String argsStr = Arrays.toString(Arrays.stream(args).map(_03_InterviewQA::annotatedTypeToString).toArray());
            return apt.getType().getTypeName() + annos + "<" + argsStr + ">";
        } else if (at instanceof AnnotatedArrayType) {
            AnnotatedArrayType aat = (AnnotatedArrayType) at;
            return annotatedTypeToString(aat.getAnnotatedGenericComponentType()) + "[]";
        } else {
            return at.getType().getTypeName() + annos;
        }
    }

    private static String sig(Method m) {
        StringBuilder sb = new StringBuilder("(");
        Class<?>[] pts = m.getParameterTypes();
        for (int i = 0; i < pts.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(pts[i].getSimpleName());
        }
        sb.append(")->").append(m.getReturnType().getSimpleName());
        return sb.toString();
    }

    private static void println(String s) {
        System.out.println(s);
    }

    // ========= SAMPLE ANNOTATIONS =========

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Inherited
    public @interface Audited {
        String value() default "default-audit";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Repeatable(Roles.class)
    public @interface Role {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface Roles {
        Role[] value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.PARAMETER})
    public @interface NotNull {
        String message() default "must not be null";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.PARAMETER})
    public @interface Min {
        long value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
    public @interface NonNull { }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface DocumentedAnno { }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD})
    public @interface MyRuntimeAnno {
        String name() default "N/A";
        int[] flags() default {};
    }

    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.TYPE)
    public @interface ClassRetentionAnno { }

    @Retention(RetentionPolicy.SOURCE)
    @Target(ElementType.TYPE)
    public @interface SourceRetentionAnno { }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Loggable { }

    // ========= SAMPLE TYPES =========

    @Audited("entity")
    static class BaseEntity { }

    @Role("USER")
    @Role("ADMIN")
    @MyRuntimeAnno(name = "sample", flags = {1, 2, 3})
    @ClassRetentionAnno
    static class User extends BaseEntity implements Greeter, Comparable<User> {

        @Min(1)
        private long id;

        @NotNull
        private @NonNull String name;

        public List<String> tags = new ArrayList<>();

        protected List<? extends Number> numbersCovariant;

        List<? super Integer> numbersContravariant;

        Map<String, Integer> scoreByName;

        private User() {
            this(0, "anon");
        }

        public User(long id, String name) {
            this.id = id;
            this.name = name;
        }

        @Loggable
        public String greet(@NotNull @NonNull String target) {
            return "Hi " + target + ", I'm " + name;
        }

        private String secretSauce(int a, int b) {
            return "sum=" + (a + b);
        }

        public static int add(int a, int b) {
            return a + b;
        }

        @Override
        public int compareTo(User o) {
            return Long.compare(this.id, o.id);
        }

        public void varargsMethod(String prefix, Integer... nums) { }

        public List<String> getTags() {
            return tags;
        }
    }

    interface Greeter {
        default String greeting() { return "hello"; }
    }

    static class Box<T extends Number> {
        T value;
        public Box(T value) { this.value = value; }
        public T getValue() { return value; }
        public void setValue(T value) { this.value = value; }
    }

    static class TypeUseDemo {
        Map<@NonNull String, List<@NonNull String>> map;
        List<@NonNull String>[] arrayOfList;
    }

    interface Calculator {
        @Loggable int add(int a, int b);
        int mul(int a, int b);
    }

    static class CalculatorImpl implements Calculator {
        public int add(int a, int b) { return a + b; }
        public int mul(int a, int b) { return a * b; }
    }

    static class LoggingHandler implements InvocationHandler {
        private final Object target;
        public LoggingHandler(Object target) { this.target = target; }
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.isAnnotationPresent(Loggable.class)) {
                System.out.println("LOG " + method.getName() + " args=" + Arrays.toString(args));
            }
            return method.invoke(target, args);
        }
    }

    static class SimpleValidator {
        public List<String> validate(Object o) throws IllegalAccessException {
            List<String> errors = new ArrayList<>();
            Class<?> c = o.getClass();
            for (Field f : c.getDeclaredFields()) {
                f.setAccessible(true);
                Object v = f.get(o);

                NotNull nn = f.getAnnotation(NotNull.class);
                if (nn != null && v == null) {
                    errors.add(f.getName() + ": " + nn.message());
                }

                Min min = f.getAnnotation(Min.class);
                if (min != null) {
                    if (v instanceof Number && ((Number) v).longValue() < min.value()) {
                        errors.add(f.getName() + ": must be >= " + min.value());
                    }
                }
            }
            return errors;
        }
    }
}