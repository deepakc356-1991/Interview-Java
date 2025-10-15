package _09_02_nio2_paths_files;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.FileVisitResult.*;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

/*
NIO.2 (java.nio.file.*)
- Purpose: modern, scalable, platform‑aware file system API (Java 7+).
- Key types:
  - Path: immutable representation of a file system path (does not access the file system on creation).
  - Files: static utility methods that access the file system.
  - FileSystem/FileSystems: abstraction for a file system (default OS FS, zip/jar FS, etc.).
  - FileStore: storage info (total/usable space, attribute support).
  - Attribute views: Basic/DOS/POSIX/Owner, read and change metadata.
  - Channels/Streams: byte and char I/O with options (seekable, buffered, memory-mapped via FileChannel).
  - Directory traversal, matching, and watching: walk, find, PathMatcher, WatchService (not fully detailed here).

Path theory (no I/O on creation)
- Created via Path.of(...) or Paths.get(...). Immutable and thread-safe.
- Absolute vs relative:
  - Absolute has a root (e.g., "/" on UNIX; "C:\" or "\\" on Windows).
  - Relative has no root; toAbsolutePath() prefixes current working directory.
- Elements:
  - getFileName, getRoot, getParent, getNameCount, getName(i), subpath(a,b).
- Normalization/logical operations (no FS access):
  - normalize(): removes ".", ".." when possible.
  - resolve(other): append other if other is relative; returns other if other is absolute.
  - resolveSibling(other): like parent.resolve(other).
  - relativize(other): compute path from this to other (both must be either absolute with same root, or relative).
- Conversions:
  - toAbsolutePath(): syntactic absolute path (no symlink resolution).
  - toRealPath([NOFOLLOW_LINKS]): touches FS; resolves symlinks; fails if path not existing (unless NOFOLLOW_LINKS used but path must still exist).
  - toUri(), toFile() (legacy interop).
- Equality:
  - equals/compareTo: purely lexical.
  - Files.isSameFile(p1, p2): touches FS; true if refer to same file/dir, even with symlinks.

Files theory (does I/O)
- Existence and type:
  - exists, notExists (both can be false due to permissions), isRegularFile, isDirectory, isSymbolicLink, isReadable, isWritable, isExecutable, isHidden.
- Create:
  - createFile, createDirectory, createDirectories (parents), createTempFile/Directory, writeString/write (create if missing by default).
- Delete:
  - delete (throws if missing), deleteIfExists.
- Copy/Move:
  - copy, move with options: REPLACE_EXISTING, COPY_ATTRIBUTES, ATOMIC_MOVE, NOFOLLOW_LINKS (where supported).
- Read/Write (whole file convenience):
  - readAllBytes, readAllLines(charset), readString (Java 11+); write, writeString.
- Streams and Channels:
  - newInputStream/newOutputStream(...OpenOption...), newBufferedReader/Writer(path, charset).
  - newByteChannel(...OpenOption...) returns SeekableByteChannel (read/write/position).
  - FileChannel.open for random access and memory mapping; OpenOptions like READ, WRITE, CREATE, CREATE_NEW, TRUNCATE_EXISTING, APPEND, DELETE_ON_CLOSE, SYNC/DSYNC.
- Attributes:
  - readAttributes(Path, BasicFileAttributes.class), DosFileAttributes, PosixFileAttributes (POSIX systems only).
  - Files.getAttribute/setAttribute("view:attr", value) and attribute views (e.g., BasicFileAttributeView, PosixFileAttributeView).
- Listing and walking:
  - list(dir) returns Stream<Path> (must be closed).
  - newDirectoryStream(dir) returns DirectoryStream<Path> (lazy, must close).
  - walk(start, depth, options...) returns Stream; find(start, depth, BiPredicate); walkFileTree(visitor).
- Path matching:
  - FileSystems.getDefault().getPathMatcher("glob:pattern" | "regex:pattern").
  - Glob basics: "*" any chars except separator, "**" across dirs, "?" single char, {a,b}, [a-z].
- Links:
  - createSymbolicLink(link, target), readSymbolicLink, createLink(hard link). Admin rights may be required on Windows.
  - Caution: NOFOLLOW_LINKS to treat link itself; following symlinks is default in many ops.

Common exceptions
- InvalidPathException (syntactic), IOException, FileAlreadyExistsException, NoSuchFileException, AccessDeniedException, DirectoryNotEmptyException, UnsupportedOperationException, SecurityException.

OS notes
- POSIX attributes only on POSIX file systems (Linux/macOS). DOS attributes only on Windows.
- Windows roots include drive letters and UNC paths (e.g., \\server\share).
- Path operations are lexical; file system semantics vary by platform.

Below: compact, runnable demos plus many commented examples.
*/
public class _01_Theory {

    public static void main(String[] args) throws Exception {
        // Minimal, safe, cross-platform demo in a temporary area
        runMinimalDemo();
        // Explore more examples by calling other methods if desired.
        // pathBasics();
        // filesListingAndWalk();
        // attributesExamples();
        // matchingExamples();
        // channelsExamples();
        // zipFileSystemExample(); // requires a real file if adapted
    }

    // Basic Path creation and operations (lexical)
    static void pathBasics() {
        // Creation (does not touch FS)
        Path p1 = Path.of("logs/app/config.txt");          // relative
        Path p2 = Paths.get("/var", "log", "app.log");     // absolute on UNIX
        // On Windows: Paths.get("C:\\data\\report.txt");

        // From URI (file scheme)
        URI uri = URI.create("file:///tmp/example.txt");   // Use proper encoding for spaces (%20)
        Path pFromUri = Path.of(uri);

        // Inspect components
        System.out.println("fileName=" + p1.getFileName());
        System.out.println("nameCount=" + p1.getNameCount());
        if (p2.getRoot() != null) {
            System.out.println("root=" + p2.getRoot());
        }
        System.out.println("parent=" + p1.getParent());

        // Normalize (removes . and .. where possible)
        Path messy = Path.of("a/./b/../c//d");
        System.out.println("normalized=" + messy.normalize()); // a/c/d

        // Resolve (append if other is relative)
        Path base = Path.of("/opt/app");
        Path resolved = base.resolve("conf/app.yml");      // /opt/app/conf/app.yml
        Path resolvedAbs = base.resolve("/etc/hosts");     // /etc/hosts (absolute wins)

        // Relativize (both relative or both absolute with same root)
        Path a = Path.of("/usr/local/bin");
        Path b = Path.of("/usr/share/doc");
        Path rel = a.relativize(b);                        // ../../share/doc

        // Absolute vs real path
        Path relAbs = p1.toAbsolutePath();                 // no FS access
        try {
            Path real = p1.toRealPath();                   // resolves symlinks; fails if missing
        } catch (IOException ignore) {}

        // Lexical equality vs file identity
        Path x = Path.of("a/../b").normalize();
        Path y = Path.of("b");
        System.out.println("equals lexical=" + x.equals(y)); // true
        try {
            System.out.println("same file=" + Files.isSameFile(x, y)); // touches FS
        } catch (IOException ignore) {}
    }

    // Minimal, safe, self-cleaning demo of Files operations
    static void runMinimalDemo() throws IOException {
        Charset cs = StandardCharsets.UTF_8;

        Path tempRoot = Files.createTempDirectory("_nio2_demo_");
        Path dir = tempRoot.resolve("dir/sub");
        Files.createDirectories(dir);

        Path file = dir.resolve("data.txt");
        // Write string (Java 11+); creates the file if missing
        Files.writeString(file, "Hello NIO.2\nLine 2\n", cs);

        // Read back and print
        System.out.println("Content readString:");
        System.out.println(Files.readString(file, cs));

        // Append using options
        Files.write(file, List.of("Appended"), cs, StandardOpenOption.APPEND);

        // List files in parent
        System.out.println("Listing parent:");
        try (Stream<Path> s = Files.list(dir.getParent())) {
            s.forEach(System.out::println);
        }

        // Copy and move
        Path copy = dir.resolve("data-copy.txt");
        Files.copy(file, copy, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);

        Path moved = dir.resolveSibling("moved");
        Files.createDirectories(moved);
        Files.move(copy, moved.resolve(copy.getFileName()), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

        // Attributes
        long size = Files.size(file);
        FileTime lm = Files.getLastModifiedTime(file);
        System.out.println("Size=" + size + " lastModified=" + lm);

        // Walk and find .txt files
        try (Stream<Path> walk = Files.walk(tempRoot)) {
            walk.filter(p -> p.getFileName().toString().endsWith(".txt"))
                .forEach(p -> System.out.println("Found txt: " + p));
        }

        // Clean up recursively
        deleteRecursively(tempRoot);
    }

    // Files listing, DirectoryStream, walk, find, and walkFileTree
    static void filesListingAndWalk() throws IOException {
        Path root = Files.createTempDirectory("walk_demo");
        Path a = root.resolve("a");
        Path b = root.resolve("a/b");
        Files.createDirectories(b);
        Files.writeString(a.resolve("one.txt"), "1");
        Files.writeString(b.resolve("two.log"), "2");

        // list (non-recursive)
        try (Stream<Path> s = Files.list(root)) {
            s.forEach(p -> System.out.println("list: " + p));
        }

        // DirectoryStream (lazy; filters)
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(a, "*.txt")) {
            for (Path p : ds) {
                System.out.println("dirStream txt: " + p);
            }
        }

        // walk (recursive)
        try (Stream<Path> s = Files.walk(root, 5, FileVisitOption.FOLLOW_LINKS)) {
            s.forEach(p -> System.out.println("walk: " + p));
        }

        // find with predicate (file, attrs) -> boolean
        BiPredicate<Path, BasicFileAttributes> pred =
                (p, attrs) -> !attrs.isDirectory() && p.getFileName().toString().endsWith(".log");
        try (Stream<Path> s = Files.find(root, Integer.MAX_VALUE, pred)) {
            s.forEach(p -> System.out.println("find .log: " + p));
        }

        // walkFileTree with visitor (delete recursively example)
        Files.walkFileTree(root, new java.nio.file.SimpleFileVisitor<>() {
            @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    // Attributes: basic/DOS/POSIX and attribute views
    static void attributesExamples() throws IOException {
        Path p = Files.createTempFile("attrs_demo", ".txt");
        Files.writeString(p, "data");

        // Basic attributes (portable)
        BasicFileAttributes basic = Files.readAttributes(p, BasicFileAttributes.class);
        System.out.println("isRegular=" + basic.isRegularFile() + " size=" + basic.size());

        // Modify lastModifiedTime via view (more efficient when updating multiple fields)
        BasicFileAttributeView basicView = Files.getFileAttributeView(p, BasicFileAttributeView.class);
        basicView.setTimes(FileTime.from(Instant.now()), null, null);

        // DOS attributes (Windows only)
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            DosFileAttributes dos = Files.readAttributes(p, DosFileAttributes.class);
            System.out.println("dos hidden=" + dos.isHidden());
            Files.setAttribute(p, "dos:hidden", Boolean.TRUE, LinkOption.NOFOLLOW_LINKS);
        }

        // POSIX attributes (POSIX only)
        try {
            PosixFileAttributes posix = Files.readAttributes(p, PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            Set<PosixFilePermission> perms = posix.permissions();
            System.out.println("posix perms size=" + perms.size());
            // Example: rw-r----- (owner read+write, group read)
            Set<PosixFilePermission> newPerms = Set.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.GROUP_READ
            );
            Files.setPosixFilePermissions(p, newPerms);
        } catch (UnsupportedOperationException ignored) {
            // Not a POSIX file system
        }

        // Owner attribute view
        FileOwnerAttributeView ownerView = Files.getFileAttributeView(p, FileOwnerAttributeView.class);
        UserPrincipal owner = ownerView.getOwner();
        System.out.println("owner=" + owner.getName());
        // Change owner example (requires permissions)
        UserPrincipalLookupService lookup = p.getFileSystem().getUserPrincipalLookupService();
        // UserPrincipal newOwner = lookup.lookupPrincipalByName("someuser");
        // ownerView.setOwner(newOwner);

        Files.deleteIfExists(p);
    }

    // Copy, move, delete examples with options and symlink handling
    static void copyMoveDeleteExamples() throws IOException {
        Path root = Files.createTempDirectory("cmd_demo");
        Path src = Files.writeString(root.resolve("source.txt"), "copy me");
        Path target = root.resolve("target.txt");

        // Copy with attributes, replace
        Files.copy(src, target, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);

        // Move atomically if supported
        Path moved = root.resolve("moved.txt");
        try {
            Files.move(target, moved, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            // Fallback
            Files.move(target, moved, StandardCopyOption.REPLACE_EXISTING);
        }

        // Symlink example (admin may be required on Windows)
        Path link = root.resolve("link_to_src");
        try {
            Files.createSymbolicLink(link, src.getFileName()); // relative target resolved from link's parent
            System.out.println("isSymbolicLink=" + Files.isSymbolicLink(link));
            System.out.println("readSymbolicLink=" + Files.readSymbolicLink(link));
            // NOFOLLOW_LINKS to operate on the link itself, not the target
            boolean hidden = (boolean) Files.getAttribute(link, "dos:hidden", LinkOption.NOFOLLOW_LINKS);
        } catch (UnsupportedOperationException | IOException | SecurityException e) {
            // Ignore if unsupported or permissions insufficient
        }

        deleteRecursively(root);
    }

    // PathMatcher examples (glob and regex)
    static void matchingExamples() {
        PathMatcher globJava = FileSystems.getDefault().getPathMatcher("glob:**/*.java");
        PathMatcher regexTest = FileSystems.getDefault().getPathMatcher("regex:.*\\.(txt|log)");

        Path p1 = Path.of("src/main/_01_Theory.java");
        Path p2 = Path.of("notes/readme.txt");

        System.out.println("glob matches p1=" + globJava.matches(p1));
        System.out.println("regex matches p2=" + regexTest.matches(p2));
    }

    // Channels: SeekableByteChannel and FileChannel (random access)
    static void channelsExamples() throws IOException {
        Path p = Files.createTempFile("chan_demo", ".bin");

        // Write with SeekableByteChannel
        try (SeekableByteChannel ch = Files.newByteChannel(p,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            ByteBuffer buf = ByteBuffer.allocate(16);
            buf.putInt(0xCAFEBABE);
            buf.putLong(123456789L);
            buf.flip();
            ch.write(buf);
        }

        // Read random access with FileChannel
        try (FileChannel fc = FileChannel.open(p, StandardOpenOption.READ)) {
            ByteBuffer header = ByteBuffer.allocate(4);
            fc.read(header);
            header.flip();
            int magic = header.getInt();
            System.out.println("magic=" + Integer.toHexString(magic));
            // Position to read the long
            fc.position(4);
            ByteBuffer num = ByteBuffer.allocate(8);
            fc.read(num);
            num.flip();
            long value = num.getLong();
            System.out.println("value=" + value);
        }

        Files.deleteIfExists(p);
    }

    // Zip/Jar file system example (treat ZIP as a FileSystem). Requires an existing zip or will create one.
    static void zipFileSystemExample() throws IOException {
        Path zip = Files.createTempFile("nio2_zip", ".zip");
        // Initialize empty zip
        try (FileSystem zipfs = newZipFileSystem(zip, true)) {
            Path root = zipfs.getPath("/");
            Path inside = zipfs.getPath("/docs/readme.txt");
            Files.createDirectories(inside.getParent());
            Files.writeString(inside, "Inside ZIP!");
            try (Stream<Path> s = Files.walk(root)) {
                s.forEach(System.out::println);
            }
        }
        Files.deleteIfExists(zip);
    }

    // Helper to create/attach to a ZIP FS
    static FileSystem newZipFileSystem(Path zip, boolean create) throws IOException {
        // jar:file URI works with Zip FS provider included in the JDK
        URI uri = URI.create("jar:" + zip.toUri().toString());
        var env = new java.util.HashMap<String, String>();
        if (create) env.put("create", "true");
        return FileSystems.newFileSystem(uri, env);
    }

    // WatchService example (not called by default; events are coalesced and not guaranteed per change)
    /*
    static void watchServiceExample(Path dir) throws IOException, InterruptedException {
        try (WatchService ws = FileSystems.getDefault().newWatchService()) {
            dir.register(ws, StandardWatchEventKinds.ENTRY_CREATE,
                            StandardWatchEventKinds.ENTRY_DELETE,
                            StandardWatchEventKinds.ENTRY_MODIFY);
            while (true) {
                WatchKey key = ws.take(); // blocks
                for (WatchEvent<?> ev : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = ev.kind();
                    Path context = (Path) ev.context(); // relative to the registered dir
                    System.out.println(kind.name() + ": " + context);
                }
                boolean valid = key.reset();
                if (!valid) break;
            }
        }
    }
    */

    // Utility: delete directory tree safely
    static void deleteRecursively(Path root) throws IOException {
        if (root == null || !Files.exists(root)) return;
        try (Stream<Path> s = Files.walk(root)) {
            s.sorted(Comparator.reverseOrder()).forEach(p -> {
                try { Files.deleteIfExists(p); } catch (IOException ignored) {}
            });
        }
    }

    // Additional notes in code comments:
    // - Files.createFile fails if file exists; Files.writeString with default options creates or truncates unless APPEND used.
    // - CREATE_NEW fails if exists; CREATE creates if missing; TRUNCATE_EXISTING truncates if present.
    // - ATOMIC_MOVE may not be supported across different file stores.
    // - Files.isHidden relies on DOS hidden attribute on Windows; on POSIX, leading dot names are hidden.
    // - DirectoryStream supports a Filter<Path> for efficient server-side filtering on some providers.
    // - Remember to close all Streams/DirectoryStreams/WatchService/FileSystem to free OS resources.
}