package _09_03_serialization;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/*
Java Serialization Interview Q&A (basic -> intermediate -> advanced)
Use these Q&A with the runnable examples in this file to learn, demo, or revise.

BASICS
Q1: What is Serialization in Java?
A: The process of converting an object graph into a byte stream so it can be saved or sent; deserialization reconstructs it.

Q2: How do you make a class serializable?
A: Implement java.io.Serializable (a marker interface). Example: class X implements Serializable {}

Q3: What is serialVersionUID? Why is it important?
A: A version identifier used during deserialization to verify compatibility. Always explicitly declare: private static final long serialVersionUID = 1L;
   If not declared, JVM computes one; changes in class signature can change it and break compatibility (InvalidClassException).

Q4: What happens to transient and static fields?
A: transient fields are skipped from default serialization; static fields are not part of the instance state and are not serialized.

Q5: What is the default serialization mechanism?
A: ObjectOutputStream/ObjectInputStream walk the object graph and write/read fields for classes implementing Serializable.

Q6: How do you serialize/deserialize?
A: Use ObjectOutputStream.writeObject(obj) and ObjectInputStream.readObject().

Q7: Can constructors run during deserialization?
A: For Serializable classes: no. For the first non-Serializable superclass: its no-arg constructor runs.

Q8: What is NotSerializableException?
A: Thrown if any object in the graph does not implement Serializable and is not otherwise handled.

INTERMEDIATE
Q9: What is Externalizable vs Serializable?
A: Externalizable gives full control via writeExternal/readExternal; requires a public no-arg constructor; more brittle to evolution but can be compact/fast.

Q10: How to customize serialization for part of the object?
A: Define private writeObject(ObjectOutputStream) and readObject(ObjectInputStream), call defaultWriteObject/defaultReadObject, then handle extras.

Q11: How to substitute objects during serialization/deserialization?
A: writeReplace (on the serialized object) and readResolve (on the deserialized object). Also stream-level replaceObject/resolveObject via custom streams.

Q12: What is the Serialization Proxy pattern?
A: Use a private static proxy class to represent the serial form; outer writeReplace returns the proxy; outer readObject throws to enforce proxy usage; proxy readResolve rebuilds outer. Ensures invariants and security.

Q13: How to maintain backward/forward compatibility?
A: Keep serialVersionUID stable; prefer adding new fields with sensible defaults; use serialPersistentFields and PutField/GetField; avoid removing/renaming serialized fields; avoid changing field types.

Q14: How to serialize a Singleton safely?
A: Implement readResolve to return the existing instance. Enums are inherently singleton-safe.

Q15: Can you prevent serialization of a class?
A: Do not implement Serializable; or implement writeReplace/readObject to throw NotSerializableException; or mark class as not intended for serialization.

Q16: How to handle optional data when versions differ?
A: In readObject, catch OptionalDataException or use GetField with defaults: in.readFields().get("field", defaultValue).

Q17: What is Object graph? Cycles?
A: The entire network of reachable objects. Java serialization handles cycles and shared references automatically via object handles.

Q18: What is writeUnshared/readUnshared?
A: Write/read without sharing semantics; a new copy each time; can reduce aliasing but may increase size and break graphs if misused.

Q19: What about serializing inner/anonymous classes?
A: Avoid; their synthetic fields/captures make the serial form fragile. Prefer top-level or static nested classes.

Q20: Is Java serialization safe?
A: Deserialization is dangerous if you accept untrusted data; can lead to RCE gadget chains. Always filter and whitelist types (JEP 290), or avoid native serialization for untrusted inputs.

ADVANCED
Q21: How is serialVersionUID computed if not declared?
A: From class details (name, modifiers, fields, methods). Any signature change can alter it; explicitly declare to avoid surprises.

Q22: What are serialPersistentFields?
A: A white-list of fields that define the serial form, decoupling it from the actual class fields.

Q23: How to enforce invariants during deserialization?
A: Use readObject validations; Serializable with readResolve; or prefer the Serialization Proxy pattern.

Q24: How to compress serialized data?
A: Wrap stream with GZIPOutputStream/GZIPInputStream. Trade CPU for size.

Q25: How to deep copy with serialization?
A: Serialize to bytes and deserialize; only for Serializable graphs; beware performance and transient fields.

Q26: What are common exceptions?
A: NotSerializableException, InvalidClassException (SUID mismatch), OptionalDataException, StreamCorruptedException, ClassNotFoundException.

Q27: What about performance?
A: Use buffering, avoid excessive object graphs, consider Externalizable or alternative serializers (Kryo/Protobuf/JSON) for speed/size, avoid serializing large caches.

Q28: How are enums serialized?
A: By name only; singletons by design; readResolve not needed.

Q29: How to whitelist classes for safe deserialization (JEP 290)?
A: Use ObjectInputFilter (JDK 9+) programmatically or system properties; filter on class names/packages, depth, size, refs.

Q30: How to handle non-serializable superclasses?
A: Ensure they have an accessible no-arg constructor; it runs during deserialization.

Q31: Can final transient fields be restored?
A: Not via normal readObject assignment; require reflection or writeReplace/rebuild; generally avoid final transient if you need to restore.

Q32: How to evolve a serial form safely?
A: Design a stable serial form up-front (serialPersistentFields); maintain SUID; avoid exposing implementation details; consider proxy pattern.

Q33: How does object substitution work at stream level?
A: Override ObjectOutputStream.enableReplaceObject(true) and replaceObject; override ObjectInputStream.resolveObject for inbound substitution.

Q34: Should passwords/keys be serialized?
A: No. Use transient; if absolutely required, encrypt and manage keys securely; prefer derived/hashes only.

Q35: Alternatives to Java native serialization?
A: JSON/Jackson, XML, Protocol Buffers, Avro, Kryo, FST; often safer and faster for cross-language or untrusted contexts.

Q36: How to serialize exceptions?
A: Most Throwable implements Serializable; ensure causes and suppressed exceptions are also serializable or tolerate loss.

Q37: What is readObjectNoData?
A: Called during deserialization if no data is available for a class in the stream (rare versioning edge). Use to set defaults.

Q38: Can you change access modifiers on fields?
A: Changing modifiers may affect default SUID and compatibility; keep SUID fixed or use serialPersistentFields.

Q39: What about custom protocol versions?
A: Write an explicit version header/number in writeObject/writeExternal and branch logic in readObject/readExternal.

Q40: What about ObjectOutputStream.reset?
A: Clears the stream’s object handle table; forces re-serialization of subsequent writes; useful when object state changes mid-stream.

Cheat Sheet
- Always declare serialVersionUID.
- Prefer transient for sensitive/derived fields.
- Validate in readObject; consider serialization proxy.
- Whitelist with ObjectInputFilter (JDK 9+) before readObject.
- Keep serial form stable or define serialPersistentFields.
- Use Externalizable only when you must fully control bytes.
- For singletons: readResolve; for enums: nothing else needed.
- Never deserialize untrusted data without filtering.

Below are concise, runnable examples referenced in answers.
*/
public class _03_InterviewQA {

    public static void main(String[] args) throws Exception {
        // Basic demo: transient/static behavior and serialVersionUID compatibility
        BasicPerson.counter = 7;
        BasicPerson alice = new BasicPerson("Alice", 30, "p@ss");
        byte[] bytes = toBytes(alice);
        BasicPerson.counter = 99; // static not serialized
        BasicPerson back = fromBytes(bytes, BasicPerson.class);
        System.out.println("Name=" + back.name + ", age=" + back.age + ", password=" + back.password + ", counter=" + BasicPerson.counter);

        // Singleton safe deserialization
        byte[] sbytes = toBytes(Singleton.INSTANCE);
        Singleton s2 = fromBytes(sbytes, Singleton.class);
        System.out.println("Singleton same instance: " + (Singleton.INSTANCE == s2));

        // Externalizable with versioning
        VersionedPoint p1 = new VersionedPoint(3, 4, "A");
        byte[] pbytes = toBytes(p1);
        VersionedPoint p2 = fromBytes(pbytes, VersionedPoint.class);
        System.out.println("Point: (" + p2.x + "," + p2.y + ") label=" + p2.label);

        // Deep copy
        BasicPerson copy = deepCopy(alice);
        System.out.println("Deep copy different ref: " + (copy != alice) + ", same name: " + copy.name);

        // Serialization proxy preserves invariants
        Period period = new Period(new Date(1000), new Date(2000));
        Period roundTrip = fromBytes(toBytes(period), Period.class);
        System.out.println("Valid Period: " + roundTrip);

        // Compressed serialization demo
        byte[] gz = toCompressedBytes(alice);
        BasicPerson fromGz = fromCompressedBytes(gz, BasicPerson.class);
        System.out.println("GZIP round-trip: " + fromGz.name);
    }

    // Utility: serialize to bytes (trusted types only; apply filter in fromBytes)
    public static byte[] toBytes(Serializable obj) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             BufferedOutputStream buf = new BufferedOutputStream(bos);
             ObjectOutputStream oos = new ObjectOutputStream(buf)) {
            oos.writeObject(obj);
            oos.flush();
            return bos.toByteArray();
        }
    }

    public static <T> T fromBytes(byte[] bytes, Class<T> type) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             BufferedInputStream buf = new BufferedInputStream(bis);
             ObjectInputStream ois = new ObjectInputStream(buf)) {
            applyDeserializationFilter(ois); // JDK9+ only; no-op on older JDKs
            Object obj = ois.readObject();
            return type.cast(obj);
        }
    }

    public static byte[] toCompressedBytes(Serializable obj) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             GZIPOutputStream gz = new GZIPOutputStream(bos);
             ObjectOutputStream oos = new ObjectOutputStream(gz)) {
            oos.writeObject(obj);
            oos.flush();
            gz.finish();
            return bos.toByteArray();
        }
    }

    public static <T> T fromCompressedBytes(byte[] bytes, Class<T> type) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             GZIPInputStream gz = new GZIPInputStream(bis);
             ObjectInputStream ois = new ObjectInputStream(gz)) {
            applyDeserializationFilter(ois);
            return type.cast(ois.readObject());
        }
    }

    // Deep copy via serialization (only for Serializable graphs; beware perf)
    public static <T extends Serializable> T deepCopy(T obj) throws IOException, ClassNotFoundException {
        return fromBytes(toBytes(obj), (Class<T>) obj.getClass());
    }

    // JEP 290: apply a whitelist + limits if running on JDK 9+ (reflection to keep Java 8 compatible)
    static void applyDeserializationFilter(ObjectInputStream ois) {
        try {
            Class<?> filterCls = Class.forName("java.io.ObjectInputFilter");
            Class<?> configCls = Class.forName("java.io.ObjectInputFilter$Config");
            // Allow only core JDK and our package; limit depth/refs/bytes; reject others
            String filterStr = "maxdepth=32;maxrefs=10000;maxbytes=10485760;java.base/*;java.util.*;_09_03_serialization.*;!*";
            Object filter = configCls.getMethod("createFilter", String.class).invoke(null, filterStr);
            ObjectInputStream.class.getMethod("setObjectInputFilter", filterCls).invoke(ois, filter);
        } catch (ClassNotFoundException e) {
            // JDK < 9: no filtering available; in production prefer alternative safe formats or backports
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    // Example 1: Basic Serializable class
    static class BasicPerson implements Serializable {
        private static final long serialVersionUID = 1L;
        static int counter;           // not serialized
        String name;
        int age;
        transient String password;    // skipped

        BasicPerson(String name, int age, String password) {
            this.name = name;
            this.age = age;
            this.password = password;
        }

        // Validate invariants on read
        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            if (name == null || age < 0) throw new InvalidObjectException("Invalid state");
        }
    }

    // Example 2: Custom writeObject/readObject (e.g., store password hash, not password)
    static class UserAccount implements Serializable {
        private static final long serialVersionUID = 2L;
        String username;
        transient char[] password; // do not serialize raw password
        private byte[] passwordHash; // derived field as serial form

        UserAccount(String username, char[] password) {
            this.username = username;
            this.password = password;
        }

        private void writeObject(ObjectOutputStream out) throws IOException {
            out.defaultWriteObject(); // writes username and (current) passwordHash (likely null at first write)
            passwordHash = (password != null) ? sha256(new String(password)) : null;
            out.writeObject(passwordHash); // explicit, stable serial form
        }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            passwordHash = (byte[]) in.readObject();
        }

        private static byte[] sha256(String s) throws IOException {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                return md.digest(s.getBytes(StandardCharsets.UTF_8));
            } catch (NoSuchAlgorithmException e) {
                throw new IOException(e);
            }
        }
    }

    // Example 3: Serialization Proxy pattern
    static class Period implements Serializable {
        private static final long serialVersionUID = 1L;
        private final Date start;
        private final Date end;

        Period(Date start, Date end) {
            if (start == null || end == null || start.after(end)) throw new IllegalArgumentException("Invalid period");
            this.start = new Date(start.getTime());
            this.end = new Date(end.getTime());
        }

        public String toString() { return "Period[" + start + " -> " + end + "]"; }

        // Use proxy
        private Object writeReplace() { return new SerializationProxy(this); }
        private void readObject(ObjectInputStream in) throws InvalidObjectException {
            throw new InvalidObjectException("Use Serialization Proxy");
        }

        private static class SerializationProxy implements Serializable {
            private static final long serialVersionUID = 1L;
            private final long s;
            private final long e;

            SerializationProxy(Period p) {
                this.s = p.start.getTime();
                this.e = p.end.getTime();
            }

            private Object readResolve() throws ObjectStreamException {
                return new Period(new Date(s), new Date(e));
            }
        }
    }

    // Example 4: Singleton safe with readResolve; enums are better
    static class Singleton implements Serializable {
        private static final long serialVersionUID = 1L;
        static final Singleton INSTANCE = new Singleton();
        private Singleton() {}
        private Object readResolve() { return INSTANCE; }
    }

    // Example 5: Externalizable with versioning
    public static class VersionedPoint implements Externalizable {
        private static final long serialVersionUID = 1L;
        private static final int STREAM_VERSION = 2; // bump when format changes

        int x, y;
        String label; // added in v2

        // Mandatory public no-arg constructor
        public VersionedPoint() {}

        public VersionedPoint(int x, int y, String label) {
            this.x = x; this.y = y; this.label = label;
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeInt(STREAM_VERSION);
            out.writeInt(x);
            out.writeInt(y);
            out.writeBoolean(label != null);
            if (label != null) out.writeUTF(label);
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException {
            int ver = in.readInt();
            this.x = in.readInt();
            this.y = in.readInt();
            if (ver >= 2) {
                boolean hasLabel = in.readBoolean();
                this.label = hasLabel ? in.readUTF() : null;
            } else {
                this.label = null;
            }
        }
    }

    // Example 6: serialPersistentFields + PutField/GetField for stable forms
    static class StableForm implements Serializable {
        private static final long serialVersionUID = 1L;

        // Define the serial form fields (names/types) independent of actual fields
        private static final ObjectStreamField[] serialPersistentFields = {
                new ObjectStreamField("x", int.class),
                new ObjectStreamField("name", String.class)
        };

        // Actual fields may differ/evolve
        private int a;
        private String b;
        private transient int derived; // recomputed

        StableForm(int a, String b) {
            this.a = a; this.b = b; this.derived = (a != 0 ? 1 : 0);
        }

        private void writeObject(ObjectOutputStream out) throws IOException {
            ObjectOutputStream.PutField fields = out.putFields();
            fields.put("x", a);
            fields.put("name", b);
            out.writeFields();
        }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            ObjectInputStream.GetField fields = in.readFields();
            this.a = fields.get("x", 0);
            this.b = (String) fields.get("name", "");
            this.derived = (a != 0 ? 1 : 0);
        }
    }

    // Example 7: Inheritance: non-serializable parent with no-arg constructor
    static class NonSerializableParent {
        int parentState = 42;
        public NonSerializableParent() {} // must exist and be accessible
    }
    static class ChildWithSerializable extends NonSerializableParent implements Serializable {
        private static final long serialVersionUID = 1L;
        int childState = 7;
    }

    // Example 8: Stream-level substitution (redaction) via custom streams
    interface SensitiveMarker extends Serializable {}
    static class SensitiveData implements SensitiveMarker {
        private static final long serialVersionUID = 1L;
        String secret;
        SensitiveData(String s) { this.secret = s; }
    }
    static class Redacted implements SensitiveMarker {
        private static final long serialVersionUID = 1L;
        public String toString() { return "[REDACTED]"; }
    }
    static class SubstitutingOOS extends ObjectOutputStream {
        SubstitutingOOS(OutputStream out) throws IOException { super(out); enableReplaceObject(true); }
        @Override
        protected Object replaceObject(Object obj) {
            if (obj instanceof SensitiveData) return new Redacted();
            return obj;
        }
    }
    static class ResolvingOIS extends ObjectInputStream {
        ResolvingOIS(InputStream in) throws IOException { super(in); enableResolveObject(true); }
        @Override
        protected Object resolveObject(Object obj) {
            // Could map legacy types to new ones here
            return obj;
        }
    }

    // Example 9: Preventing serialization of a class (defensive)
    static class NotForSerialization implements Serializable {
        private static final long serialVersionUID = 1L;
        private void writeObject(ObjectOutputStream out) throws IOException {
            throw new NotSerializableException("NotForSerialization is not serializable");
        }
        private void readObject(ObjectInputStream in) throws IOException {
            throw new NotSerializableException("NotForSerialization is not serializable");
        }
    }

    // Example 10: Enum serialization (by name)
    enum Color { RED, GREEN, BLUE }
}