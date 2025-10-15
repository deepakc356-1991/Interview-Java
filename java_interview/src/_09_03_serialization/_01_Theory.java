package _09_03_serialization;

import java.io.*;
import java.util.Arrays;

/**
 * Serialization (Java Object Serialization) – Theory and Practical Guide
 *
 * What is it?
 * - Mechanism to convert an object graph (an object and everything it references) into a byte stream (serialization),
 *   and reconstruct it later (deserialization).
 * - Primary APIs: java.io.Serializable, ObjectOutputStream, ObjectInputStream.
 *
 * Common use cases
 * - Caching in memory or on disk, session persistence, inter-process communication (historically RMI).
 * - NOT recommended as a long-term, external, or cross-language data format. Prefer JSON/CBOR/Protobuf/Avro/etc.
 *
 * Core concepts
 * - Serializable: Marker interface; classes that implement it can be serialized using ObjectOutputStream.
 * - Externalizable: Alternative to Serializable; you manually control the exact binary format via writeExternal/readExternal.
 * - serialVersionUID: Version identifier for a Serializable class. If absent, JVM computes one; changes to class details
 *   can change this computed value and break compatibility. Always explicitly declare serialVersionUID.
 * - transient: Fields marked transient are not serialized by defaultWriteObject. You may compute/restore them manually.
 * - static: Static fields belong to the class, not to instances; they are not serialized.
 * - Object graph & identity:
 *   - Serialization preserves object identity and handles cycles: the stream contains back-references to objects already written.
 *   - writeObject writes an object graph once; repeated writes of the same reference to the same stream use back-references.
 *   - reset() on ObjectOutputStream clears the handle table so subsequent writes produce fresh instances on read.
 *   - writeUnshared/readUnshared can force distinct copies (advanced, use with care).
 * - Inheritance & constructors:
 *   - If a class implements Serializable and its superclass does not, the superclass’s no-arg constructor is invoked
 *     during deserialization to initialize it. That no-arg constructor must be accessible or deserialization fails.
 *   - For Serializable classes, normal constructors are NOT called on deserialization. Use readObject to restore invariants.
 * - Customization hooks in Serializable classes:
 *   - private void writeObject(ObjectOutputStream oos) throws IOException { ... }
 *   - private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException { ... }
 *   - oos.defaultWriteObject()/ois.defaultReadObject() read/write the default serializable fields of the current class.
 *   - private Object writeReplace() throws ObjectStreamException { ... } – Replace this object with another before writing.
 *   - private Object readResolve() throws ObjectStreamException { ... } – Replace the just-deserialized object with another.
 * - Serialization proxy pattern:
 *   - Robust pattern to serialize a class by writing a simple, stable proxy type. Use writeReplace to emit the proxy and
 *     readResolve in the proxy to recreate the real object. Blocks direct deserialization via readObject in the real class.
 * - serialPersistentFields:
 *   - Allows you to define which field names are serialized regardless of the actual class field layout; helpful for
 *     refactoring and versioning. Use PutField/GetField to write/read.
 * - Externalizable:
 *   - You must implement public no-arg constructor, writeExternal(ObjectOutput), readExternal(ObjectInput).
 *   - You are responsible for versioning and compatibility across versions.
 * - Exceptions you may encounter:
 *   - NotSerializableException: A field (non-transient) is not serializable.
 *   - InvalidClassException: serialVersionUID mismatch or missing no-arg constructor in a non-serializable superclass.
 *   - OptionalDataException: Mismatch between read operations and the actual stream content.
 *   - StreamCorruptedException: Protocol was violated; stream is damaged or not an Object stream.
 *   - EOFException, ClassNotFoundException: End-of-stream or missing class on classpath during deserialization.
 * - Security considerations:
 *   - Treat all incoming serialized data as untrusted. Deserialization vulnerabilities can enable remote code execution
 *     via gadget chains. Enable filtering (ObjectInputFilter, JEP 290) and accept only trusted types.
 *   - Prefer safer data formats for external inputs. If using Java serialization, whitelist types and validate invariants.
 * - Best practices:
 *   - Always declare serialVersionUID.
 *   - Keep the serialized form stable or explicitly control it (serialPersistentFields or proxy pattern).
 *   - Validate invariants in readObject or readResolve; throw InvalidObjectException on bad data.
 *   - Avoid serializing sensitive secrets; if necessary, use transient and handle securely in custom serialization.
 *   - For new systems, prefer other formats unless you specifically need Java’s object identity and graph semantics.
 *
 * Notes on language features
 * - Enums are inherently Serializable; their serialized form uses the enum constant name and preserves singleton semantics.
 * - Records are not implicitly Serializable; implement Serializable explicitly if needed.
 * - @Serial annotation (since Java 14) can be used on serialization members (writeObject, readObject, etc.). Not used here
 *   to maintain compatibility with older Java versions.
 */
public class _01_Theory {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Basic Serializable demo ===");
        Person p = new Person("Ann", 30, "s3cr3t");
        byte[] data = toBytes(p);
        Person p2 = fromBytes(data);
        System.out.println("Original: " + p);
        System.out.println("Deser:    " + p2);

        System.out.println("\n=== Inheritance demo (non-serializable superclass) ===");
        Derived d = new Derived(42, "payload");
        Derived d2 = fromBytes(toBytes(d));
        System.out.println("Derived original: " + d);
        System.out.println("Derived deser:    " + d2 + " (note: base constructed on read)");

        System.out.println("\n=== Externalizable demo ===");
        ExternalizablePoint pt = new ExternalizablePoint(10, 20);
        ExternalizablePoint pt2 = fromBytes(toBytes(pt));
        System.out.println("Point original: " + pt);
        System.out.println("Point deser:    " + pt2);

        System.out.println("\n=== Singleton with readResolve demo ===");
        GlobalSettings s1 = GlobalSettings.INSTANCE;
        GlobalSettings s2 = fromBytes(toBytes(s1));
        System.out.println("Singleton instance preserved: " + (s1 == s2));

        System.out.println("\n=== Serialization Proxy pattern demo ===");
        Period period = new Period(1, 5);
        Period period2 = fromBytes(toBytes(period));
        System.out.println("Period original: " + period);
        System.out.println("Period deser:    " + period2);

        System.out.println("\n=== Object identity and reset demo ===");
        identityResetDemo();

        System.out.println("\n=== Cyclic graph demo ===");
        Node a = new Node("A");
        a.next = a; // cycle
        Node a2 = fromBytes(toBytes(a));
        System.out.println("Cycle preserved: " + (a2 == a2.next));

        System.out.println("\n=== serialPersistentFields demo ===");
        User u = new User("Alice Example", 28);
        User u2 = fromBytes(toBytes(u));
        System.out.println("User original: " + u);
        System.out.println("User deser:    " + u2);

        System.out.println("\n=== Deep copy via serialization demo (illustrative) ===");
        Person pCopy = deepCopy(p);
        System.out.println("Deep copy equals content: " + p.equals(pCopy) + ", same reference: " + (p == pCopy));

        System.out.println("\n=== Filtering note (JEP 290) ===");
        System.out.println("See filterDeserializationWithReflection method for how to set a filter reflectively if available.");
    }

    // Utility: serialize to byte[]
    private static byte[] toBytes(Serializable obj) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(bos);
            oos.writeObject(obj);
            oos.flush();
            return bos.toByteArray();
        } finally {
            if (oos != null) oos.close();
            bos.close();
        }
    }

    // Utility: deserialize from byte[]
    @SuppressWarnings("unchecked")
    private static <T> T fromBytes(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(bis);
            return (T) ois.readObject();
        } finally {
            if (ois != null) ois.close();
            bis.close();
        }
    }

    // Deep copy via serialization (only for Serializable types). Not for production performance-critical code.
    public static <T extends Serializable> T deepCopy(T obj) throws IOException, ClassNotFoundException {
        return fromBytes(toBytes(obj));
    }

    // Demonstrate identity preservation and reset()
    private static void identityResetDemo() throws IOException, ClassNotFoundException {
        Person p = new Person("Bob", 40, "pw");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);

        // Write same object twice into same stream without reset: second is a back-reference
        oos.writeObject(p);
        oos.writeObject(p);
        // Now force reset (clears handle table)
        oos.reset();
        // Write the same object again after reset: becomes a full, independent instance upon read
        oos.writeObject(p);
        oos.flush();

        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bis);

        Person a = (Person) ois.readObject();
        Person b = (Person) ois.readObject();
        Person c = (Person) ois.readObject();

        System.out.println("a == b (back-reference): " + (a == b)); // true
        System.out.println("a == c (after reset):    " + (a == c)); // false

        oos.close();
        ois.close();
    }

    // SECURITY: Demonstrates setting a deserialization filter reflectively (for Java 9+). No-op on older JDKs.
    // This avoids compile-time dependency on java.io.ObjectInputFilter for older Java versions.
    private static Object filterDeserializationWithReflection(byte[] serialized) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(serialized);
        ObjectInputStream ois = new ObjectInputStream(bis);
        try {
            // Try to load ObjectInputFilter and call setObjectInputFilter with a permissive but type-limited lambda.
            Class<?> filterClass = Class.forName("java.io.ObjectInputFilter");
            // Create a proxy filter that only allows our own package and common JDK types (very simplistic example).
            Object filter = java.lang.reflect.Proxy.newProxyInstance(
                    _01_Theory.class.getClassLoader(),
                    new Class[]{filterClass},
                    (proxy, method, args) -> {
                        if ("checkInput".equals(method.getName())) {
                            Object info = args[0];
                            // info.serialClass() -> allowed?
                            Class<?> serialClass = (Class<?>) info.getClass().getMethod("serialClass").invoke(info);
                            if (serialClass == null) return enumConst(filterClass, "Status", "UNDECIDED");
                            String name = serialClass.getName();
                            if (name.startsWith(_01_Theory.class.getPackage().getName())
                                    || name.startsWith("java.lang.")
                                    || name.startsWith("java.util.")
                                    || serialClass.isArray()
                                    || serialClass.isPrimitive()) {
                                return enumConst(filterClass, "Status", "ALLOWED");
                            }
                            return enumConst(filterClass, "Status", "REJECTED");
                        }
                        return null;
                    }
            );
            // ois.setObjectInputFilter(filter)
            ois.getClass().getMethod("setObjectInputFilter", filterClass).invoke(ois, filter);
        } catch (ReflectiveOperationException ignore) {
            // Filter API not available; proceed normally (not secure for untrusted data).
        }
        Object obj = ois.readObject();
        ois.close();
        return obj;
    }

    private static Object enumConst(Class<?> filterClass, String nestedEnumSimpleName, String constant) throws ReflectiveOperationException {
        for (Class<?> nested : filterClass.getDeclaredClasses()) {
            if (nested.getSimpleName().equals(nestedEnumSimpleName) && nested.isEnum()) {
                for (Object c : nested.getEnumConstants()) {
                    if (c.toString().equals(constant)) return c;
                }
            }
        }
        throw new IllegalArgumentException("Enum constant not found: " + nestedEnumSimpleName + "." + constant);
    }

    // --------------------------------------------------------------------------------------------
    // Example classes
    // --------------------------------------------------------------------------------------------

    /**
     * Basic Serializable class demonstrating:
     * - serialVersionUID
     * - transient field
     * - custom writeObject/readObject
     * - validation of invariants
     */
    public static class Person implements Serializable {
        private static final long serialVersionUID = 1L;

        private String name;
        private int age;

        // Sensitive; not serialized by default
        private transient String password;

        // Not serialized (static)
        private static int globalCounter = 0;

        public Person(String name, int age, String password) {
            this.name = name;
            this.age = age;
            this.password = password;
            globalCounter++;
        }

        @Override
        public String toString() {
            return "Person{name='" + name + "', age=" + age + ", password=" + (password == null ? "null" : "******") + "}";
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Person)) return false;
            Person p = (Person) o;
            return age == p.age && (name == null ? p.name == null : name.equals(p.name));
        }

        // Custom serialization: write default fields + a derived hash of password (illustrative only).
        private void writeObject(ObjectOutputStream oos) throws IOException {
            oos.defaultWriteObject();
            Integer passwordHash = (password == null ? null : password.hashCode());
            oos.writeObject(passwordHash);
        }

        // Custom deserialization: read default fields + derived value; validate invariants.
        private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
            ois.defaultReadObject();
            Object hash = ois.readObject(); // Here just illustrative; we don't reconstruct the password.
            // Validate invariants
            if (age < 0 || age > 150) {
                throw new InvalidObjectException("Age out of range: " + age);
            }
            // Post-processing: keep password null on purpose
            this.password = null;
        }
    }

    /**
     * Demonstrates inheritance rules:
     * - Superclass is NOT Serializable.
     * - Subclass is Serializable.
     * - On deserialization, NonSerializableBase no-arg constructor is invoked.
     */
    public static class NonSerializableBase {
        protected int baseValue;

        public NonSerializableBase() {
            // This constructor must exist and be accessible for deserialization when subclass is Serializable.
            this.baseValue = -1; // default
        }

        public NonSerializableBase(int baseValue) {
            this.baseValue = baseValue;
        }

        @Override
        public String toString() {
            return "NonSerializableBase{baseValue=" + baseValue + "}";
        }
    }

    public static class Derived extends NonSerializableBase implements Serializable {
        private static final long serialVersionUID = 1L;
        private String payload;

        public Derived(int baseValue, String payload) {
            super(baseValue);
            this.payload = payload;
        }

        @Override
        public String toString() {
            return "Derived{baseValue=" + baseValue + ", payload='" + payload + "'}";
        }
    }

    /**
     * Externalizable example: you fully control the wire format.
     * - Must have public no-arg constructor.
     * - You are responsible for versioning.
     */
    public static class ExternalizablePoint implements Externalizable {
        private static final long serialVersionUID = 1L;

        private int x;
        private int y;

        // For versioning the external format
        private static final int STREAM_VERSION = 1;

        public ExternalizablePoint() {
            // Required public no-arg constructor
        }

        public ExternalizablePoint(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeInt(STREAM_VERSION);
            out.writeInt(x);
            out.writeInt(y);
        }

        public void readExternal(ObjectInput in) throws IOException {
            int version = in.readInt();
            if (version == 1) {
                this.x = in.readInt();
                this.y = in.readInt();
            } else {
                throw new StreamCorruptedException("Unknown version: " + version);
            }
        }

        @Override
        public String toString() {
            return "ExternalizablePoint{x=" + x + ", y=" + y + "}";
        }
    }

    /**
     * Singleton with readResolve to keep single instance upon deserialization.
     */
    public static final class GlobalSettings implements Serializable {
        private static final long serialVersionUID = 1L;

        public static final GlobalSettings INSTANCE = new GlobalSettings(42);
        private final int threshold;

        private GlobalSettings(int threshold) {
            this.threshold = threshold;
        }

        private Object readResolve() throws ObjectStreamException {
            return INSTANCE;
        }

        @Override
        public String toString() {
            return "GlobalSettings{threshold=" + threshold + "}";
        }
    }

    /**
     * Serialization proxy pattern example.
     * - Ensures invariants are preserved and decouples wire format from internal representation.
     */
    public static final class Period implements Serializable {
        private static final long serialVersionUID = 1L;

        private final int start; // inclusive
        private final int end;   // inclusive

        public Period(int start, int end) {
            if (start > end) {
                throw new IllegalArgumentException("start > end");
            }
            this.start = start;
            this.end = end;
        }

        @Override
        public String toString() {
            return "Period[" + start + ", " + end + "]";
        }

        // Replace real object with proxy for serialization
        private Object writeReplace() throws ObjectStreamException {
            return new SerializationProxy(this);
        }

        // Block direct deserialization
        private void readObject(ObjectInputStream ois) throws InvalidObjectException {
            throw new InvalidObjectException("Proxy required");
        }

        private static class SerializationProxy implements Serializable {
            private static final long serialVersionUID = 1L;
            private final int start;
            private final int end;

            SerializationProxy(Period p) {
                this.start = p.start;
                this.end = p.end;
            }

            private Object readResolve() throws ObjectStreamException {
                return new Period(start, end);
            }
        }
    }

    /**
     * Node demonstrating cyclic references.
     */
    public static class Node implements Serializable {
        private static final long serialVersionUID = 1L;

        String name;
        Node next;

        public Node(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "Node{" + name + " -> " + (next == null ? "null" : next.name) + "}";
        }
    }

    /**
     * serialPersistentFields demo:
     * - Suppose we used to serialize 'name' and 'age'.
     * - We refactored 'name' into 'fullName' in code but want to keep the serialized field named 'name' for compatibility.
     */
    public static class User implements Serializable {
        private static final long serialVersionUID = 1L;

        // Actual fields in the class now
        private String fullName;
        private int age;

        // Declare the logical serialized fields and their types
        private static final ObjectStreamField[] serialPersistentFields = {
                new ObjectStreamField("name", String.class),
                new ObjectStreamField("age", int.class)
        };

        public User(String fullName, int age) {
            this.fullName = fullName;
            this.age = age;
        }

        @Override
        public String toString() {
            return "User{fullName='" + fullName + "', age=" + age + "}";
        }

        private void writeObject(ObjectOutputStream oos) throws IOException {
            ObjectOutputStream.PutField fields = oos.putFields();
            fields.put("name", fullName); // map current fullName to old 'name'
            fields.put("age", age);
            oos.writeFields();
        }

        private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
            ObjectInputStream.GetField fields = ois.readFields();
            this.fullName = (String) fields.get("name", null);
            this.age = fields.get("age", 0);
        }
    }

    // Example of a class that would cause NotSerializableException if serialized:
    // public static class BadContainer implements Serializable {
    //     private Thread t; // Thread is not Serializable; would cause NotSerializableException unless marked transient
    // }

    // Notes on evolution and serialVersionUID:
    // - Adding a new non-transient field: older stream -> field gets default value on read; compatible if SUID unchanged.
    // - Removing a field: extra field data in the stream is ignored by defaultReadObject; compatible if SUID unchanged.
    // - Changing a field type: generally incompatible; consider custom readObject or serialPersistentFields.
    // - Always declare serialVersionUID to avoid accidental IncompatibleChange / InvalidClassException.
}