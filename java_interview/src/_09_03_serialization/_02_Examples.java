package _09_03_serialization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.NotSerializableException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectInputValidation;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/*
    Serialization examples covering:
    - Basics (Serializable, transient, serialVersionUID, static)
    - Inheritance (parent not Serializable), custom writeObject/readObject
    - Custom serialization of transient data
    - readResolve for singletons
    - Serialization Proxy pattern (writeReplace/readResolve)
    - Externalizable and versioning
    - Object graphs and shared references
    - Collections
    - GZIP compression wrappers
    - Controlling persistent fields (serialPersistentFields)
    - NotSerializableException demo
    - ObjectInputValidation for post-read invariants

    Note:
    - Java deserialization can be dangerous for untrusted data. Always validate/filter input.
    - Java 9+ adds ObjectInputFilter; see comment near the end for a sketch.
*/
public class _02_Examples {

    public static void main(String[] args) {
        try {
            System.out.println("1) Basic Serializable, transient, static, serialVersionUID");
            PersonBasic alice = new PersonBasic(1, "Alice", "s3cr3t");
            PersonBasic.STATIC_COUNTER = 42;
            byte[] personBytes = serializeToBytes(alice);
            PersonBasic.STATIC_COUNTER = 0; // static fields are not serialized
            PersonBasic alice2 = deserializeFromBytes(personBytes);
            System.out.println("Original:    " + alice);
            System.out.println("Round-trip:  " + alice2);
            System.out.println("Static after deserialization (unchanged): " + PersonBasic.STATIC_COUNTER);
            System.out.println();

            System.out.println("2) Inheritance: parent NOT Serializable vs. custom persistence of parent state");
            ChildSerializableNoCustom c1 = new ChildSerializableNoCustom("parent-config-X", 7);
            ChildSerializableNoCustom c1b = deserializeFromBytes(serializeToBytes(c1));
            System.out.println("No custom parent handling -> parent 'config' reset by parent no-arg ctor: " + c1b);

            ChildSerializableCustom c2 = new ChildSerializableCustom("parent-config-Y", 8);
            ChildSerializableCustom c2b = deserializeFromBytes(serializeToBytes(c2));
            System.out.println("With custom writeObject/readObject -> parent 'config' preserved:   " + c2b);
            System.out.println();

            System.out.println("3) Custom writeObject/readObject (persist transient safely, demo only)");
            SecureAccount acc = new SecureAccount("user1", new char[]{'1','2','3','4'});
            SecureAccount acc2 = deserializeFromBytes(serializeToBytes(acc));
            System.out.println("Original:   " + acc);
            System.out.println("Round-trip: " + acc2);
            System.out.println();

            System.out.println("4) Singleton with readResolve()");
            AppConfig cfg = AppConfig.getInstance();
            cfg.setEnv("prod");
            AppConfig cfg2 = deserializeFromBytes(serializeToBytes(cfg));
            System.out.println("Singleton identity preserved: " + (cfg == cfg2) + " -> " + cfg2);
            System.out.println();

            System.out.println("5) Serialization Proxy pattern (writeReplace + proxy.readResolve)");
            ImmutableRange r = new ImmutableRange(10, 20);
            ImmutableRange r2 = deserializeFromBytes(serializeToBytes(r));
            System.out.println("Range ok: " + r2);
            System.out.println();

            System.out.println("6) Externalizable with versioning");
            Point3D p3 = new Point3D(1, 2, 3);
            Point3D p3b = deserializeFromBytes(serializeToBytes(p3));
            System.out.println("Externalized Point3D: " + p3b);
            System.out.println();

            System.out.println("7) Object graph identity (shared references are preserved)");
            Node shared = new Node("shared");
            Node root = new Node("root");
            root.left = shared;
            root.right = shared;
            Node root2 = deserializeFromBytes(serializeToBytes(root));
            System.out.println("root2.left == root2.right ? " + (root2.left == root2.right));
            System.out.println();

            System.out.println("8) Collections of serializables");
            List<PersonBasic> people = new ArrayList<>();
            people.add(alice);
            people.add(new PersonBasic(2, "Bob", "pwd"));
            List<PersonBasic> people2 = deserializeFromBytes(serializeToBytes(people));
            System.out.println("People round-trip: " + people2);
            System.out.println();

            System.out.println("9) Compression with GZIP wrappers");
            byte[] gz = serializeToGzipBytes(people);
            List<PersonBasic> people3 = deserializeFromGzipBytes(gz);
            System.out.println("GZIP len=" + gz.length + " -> " + people3);
            System.out.println();

            System.out.println("10) serialPersistentFields to control persisted fields");
            PersonPersistent pp = new PersonPersistent(123, "Eve");
            PersonPersistent pp2 = deserializeFromBytes(serializeToBytes(pp));
            System.out.println("Original:    " + pp);
            System.out.println("Round-trip (name not persisted): " + pp2);
            System.out.println();

            System.out.println("11) NotSerializableException demo (non-transient non-serializable field)");
            try {
                Container bad = new Container(new NonSerializableResource("R"));
                serializeToBytes(bad); // throws
            } catch (NotSerializableException e) {
                System.out.println("Caught expected: " + e);
            }
            System.out.println();

            System.out.println("12) ObjectInputValidation to enforce invariants after read");
            try {
                ValidatedAccount va = new ValidatedAccount(0, "bad"); // invalid id
                deserializeFromBytes(serializeToBytes(va)); // throws during validation
            } catch (InvalidObjectException e) {
                System.out.println("Validation failed as expected: " + e.getMessage());
            }
            System.out.println();

            System.out.println("13) Same object written twice in one stream -> same instance on read");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ObjectOutputStream out = new ObjectOutputStream(baos)) {
                out.writeObject(alice);
                out.writeObject(alice); // back-reference
            }
            try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
                Object o1 = in.readObject();
                Object o2 = in.readObject();
                System.out.println("o1 == o2 ? " + (o1 == o2));
            }
            System.out.println();

            // Note: For Java 9+ deserialization filters, see comment near the end of this file.

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Helpers: in-memory byte serialization
    static byte[] serializeToBytes(Object obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(baos)) {
            out.writeObject(obj);
        }
        return baos.toByteArray();
    }

    @SuppressWarnings("unchecked")
    static <T> T deserializeFromBytes(byte[] data) throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(data))) {
            return (T) in.readObject();
        }
    }

    static byte[] serializeToGzipBytes(Object obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(baos);
             ObjectOutputStream out = new ObjectOutputStream(gzip)) {
            out.writeObject(obj);
        }
        return baos.toByteArray();
    }

    @SuppressWarnings("unchecked")
    static <T> T deserializeFromGzipBytes(byte[] data) throws IOException, ClassNotFoundException {
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(data));
             ObjectInputStream in = new ObjectInputStream(gzip)) {
            return (T) in.readObject();
        }
    }

    // 1) Basic Serializable with transient and static
    static class PersonBasic implements Serializable {
        private static final long serialVersionUID = 1L; // declare to control versioning

        public static int STATIC_COUNTER; // not serialized

        private int id;
        private String name;
        private transient String password; // not serialized

        PersonBasic(int id, String name, String password) {
            this.id = id;
            this.name = name;
            this.password = password;
        }

        @Override
        public String toString() {
            return "PersonBasic{id=" + id + ", name=" + name + ", password=" + (password == null ? "null" : "***") + "}";
        }
    }

    // 2) Inheritance where parent is NOT Serializable
    static class ParentNotSerializable {
        protected String config;

        public ParentNotSerializable() {
            this.config = "PARENT-DEFAULT";
        }

        public ParentNotSerializable(String config) {
            this.config = config;
        }

        @Override
        public String toString() {
            return "ParentNotSerializable{config=" + config + "}";
        }
    }

    // No custom writeObject/readObject -> parent field resets to default after deserialization
    static class ChildSerializableNoCustom extends ParentNotSerializable implements Serializable {
        private static final long serialVersionUID = 1L;

        private int childValue;

        public ChildSerializableNoCustom(String config, int childValue) {
            super(config);
            this.childValue = childValue;
        }

        @Override
        public String toString() {
            return "ChildSerializableNoCustom{parent.config=" + config + ", childValue=" + childValue + "}";
        }
    }

    // Custom writeObject/readObject to persist parent's non-serializable state
    static class ChildSerializableCustom extends ParentNotSerializable implements Serializable {
        private static final long serialVersionUID = 1L;

        private int childValue;

        public ChildSerializableCustom(String config, int childValue) {
            super(config);
            this.childValue = childValue;
        }

        private void writeObject(ObjectOutputStream out) throws IOException {
            out.defaultWriteObject();              // write own fields
            out.writeUTF(config != null ? config : ""); // manual parent state
        }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();                // read own fields
            this.config = in.readUTF();            // restore parent state
        }

        @Override
        public String toString() {
            return "ChildSerializableCustom{parent.config=" + config + ", childValue=" + childValue + "}";
        }
    }

    // 3) Custom writeObject/readObject for transient secret (demo only)
    static class SecureAccount implements Serializable {
        private static final long serialVersionUID = 1L;

        private String user;
        private transient char[] pin; // do not serialize raw

        public SecureAccount(String user, char[] pin) {
            this.user = user;
            this.pin = pin;
        }

        // Demo "encoding" (not secure) to show persisting transient data
        private static String encode(char[] pin) {
            if (pin == null) return "";
            byte[] bytes = new String(pin).getBytes(StandardCharsets.UTF_8);
            for (int i = 0; i < bytes.length; i++) bytes[i] ^= 0x5A;
            return Base64.getEncoder().encodeToString(bytes);
        }

        private static char[] decode(String s) {
            if (s == null || s.isEmpty()) return null;
            byte[] bytes = Base64.getDecoder().decode(s);
            for (int i = 0; i < bytes.length; i++) bytes[i] ^= 0x5A;
            return new String(bytes, StandardCharsets.UTF_8).toCharArray();
        }

        private void writeObject(ObjectOutputStream out) throws IOException {
            out.defaultWriteObject();       // write user
            out.writeUTF(encode(pin));      // write encoded transient pin
        }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            this.pin = decode(in.readUTF());
        }

        @Override
        public String toString() {
            return "SecureAccount{user=" + user + ", pin=" + (pin == null ? "null" : "****") + "}";
        }
    }

    // 4) Singleton with readResolve
    static class AppConfig implements Serializable {
        private static final long serialVersionUID = 1L;

        private static final AppConfig INSTANCE = new AppConfig();

        private String env = "dev";

        private AppConfig() {}

        public static AppConfig getInstance() {
            return INSTANCE;
        }

        public String getEnv() {
            return env;
        }

        public void setEnv(String env) {
            this.env = env;
        }

        private Object readResolve() throws ObjectStreamException {
            // Replace the deserialized object with the singleton instance
            INSTANCE.env = this.env; // copy state if needed
            return INSTANCE;
        }

        @Override
        public String toString() {
            return "AppConfig{env=" + env + "}";
        }
    }

    // 5) Serialization Proxy pattern (Effective Java)
    static final class ImmutableRange implements Serializable {
        private static final long serialVersionUID = 1L;

        private final int start;
        private final int end;

        public ImmutableRange(int start, int end) {
            if (start > end) throw new IllegalArgumentException("start > end");
            this.start = start;
            this.end = end;
        }

        private Object writeReplace() throws ObjectStreamException {
            return new SerializationProxy(this);
        }

        private void readObject(ObjectInputStream in) throws IOException {
            throw new InvalidObjectException("Use SerializationProxy");
        }

        private static class SerializationProxy implements Serializable {
            private static final long serialVersionUID = 1L;
            private final int start;
            private final int end;

            SerializationProxy(ImmutableRange r) {
                this.start = r.start;
                this.end = r.end;
            }

            private Object readResolve() throws ObjectStreamException {
                return new ImmutableRange(start, end);
            }
        }

        @Override
        public String toString() {
            return "ImmutableRange[" + start + "," + end + "]";
        }
    }

    // 6) Externalizable with simple versioning
    static class Point3D implements Externalizable {
        private static final long serialVersionUID = 1L;
        private static final int VERSION = 2;

        private int x, y, z;

        public Point3D() {
            // Required public no-arg constructor
        }

        public Point3D(int x, int y, int z) {
            this.x = x; this.y = y; this.z = z;
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeInt(VERSION);
            out.writeInt(x);
            out.writeInt(y);
            out.writeInt(z);
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException {
            int ver = in.readInt();
            this.x = in.readInt();
            this.y = in.readInt();
            this.z = (ver >= 2) ? in.readInt() : 0; // backward compatibility
        }

        @Override
        public String toString() {
            return "Point3D(" + x + "," + y + "," + z + ")";
        }
    }

    // 7) Object graph with shared references
    static class Node implements Serializable {
        private static final long serialVersionUID = 1L;

        String name;
        Node left, right;

        Node(String name) { this.name = name; }

        @Override
        public String toString() {
            return "Node{" + name + "}";
        }
    }

    // 10) Control what is persisted (omit 'name')
    static class PersonPersistent implements Serializable {
        private static final long serialVersionUID = 1L;

        private int id;
        private String name;

        // Only 'id' will be serialized
        private static final ObjectStreamField[] serialPersistentFields = {
                new ObjectStreamField("id", int.class)
        };

        public PersonPersistent(int id, String name) {
            this.id = id;
            this.name = name;
        }

        private void writeObject(ObjectOutputStream out) throws IOException {
            ObjectOutputStream.PutField pf = out.putFields();
            pf.put("id", id);
            out.writeFields();
        }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            ObjectInputStream.GetField gf = in.readFields();
            this.id = gf.get("id", 0);
            this.name = null; // not persisted
        }

        @Override
        public String toString() {
            return "PersonPersistent{id=" + id + ", name=" + name + "}";
        }
    }

    // 11) NotSerializableException demo
    static class NonSerializableResource {
        String value;
        NonSerializableResource(String v) { this.value = v; }
    }

    static class Container implements Serializable {
        private static final long serialVersionUID = 1L;
        NonSerializableResource resource; // not transient -> will fail
        Container(NonSerializableResource r) { this.resource = r; }
    }

    // 12) Post-deserialization validation
    static class ValidatedAccount implements Serializable, ObjectInputValidation {
        private static final long serialVersionUID = 1L;

        int id;
        String name;

        ValidatedAccount(int id, String name) {
            this.id = id;
            this.name = name;
        }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            in.registerValidation(this, 0); // highest priority
        }

        @Override
        public void validateObject() throws InvalidObjectException {
            if (id <= 0) throw new InvalidObjectException("id must be positive");
        }

        @Override
        public String toString() {
            return "ValidatedAccount{id=" + id + ", name=" + name + "}";
        }
    }
}

/*
    Java 9+ deserialization filters (sketch, not compiled to keep Java 8 compat):

    import java.io.ObjectInputFilter;

    try (ObjectInputStream in = new ObjectInputStream(inputStream)) {
        ObjectInputFilter filter = ObjectInputFilter.Config.createFilter(
            "_09_03_serialization.*;java.base/*;maxdepth=10;maxbytes=1048576;!*");
        in.setObjectInputFilter(filter);
        Object obj = in.readObject();
    }

    This allows only our package and core JDK types, with depth/size limits, denying others.
*/