package _09_02_nio2_paths_files;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.*;

/**
 NIO.2 (Paths, Files) Interview Q&A + runnable demonstrations.

 Key ideas (see each method for Q/A + code):
 - What is NIO.2? Paths vs Files vs File; how to create Path; default vs custom FileSystem.
 - Path operations: normalize, resolve, relativize, equals vs Files.isSameFile, toRealPath, toUri.
 - Files operations: exists, create, copy, move, delete, deleteIfExists, read/write, options.
 - Directory traversal: DirectoryStream, Files.list/find/walk, FileVisitor.
 - Attributes and views: Basic, POSIX, DOS, owner, times, hidden, size.
 - Links: symbolic vs hard links; LinkOption.NOFOLLOW_LINKS.
 - WatchService for filesystem events.
 - Channels: SeekableByteChannel, AsynchronousFileChannel.
 - PathMatcher (glob/regex), FileStore info, temp files, Zip/Jar FileSystem.
 - Common exceptions and caveats.

 Note: Code tries to be safe and self-contained under a temporary demo directory.
 */
public class _03_InterviewQA {

    public static void main(String[] args) throws Exception {
        Path demoRoot = setupDemoRoot();
        log("Demo root: " + demoRoot.toAbsolutePath());

        qa_whatIsNio2();
        qa_pathCreationAndInspection(demoRoot);
        qa_normalizeResolveRelativize(demoRoot);
        qa_equalsVs_isSameFile_vs_toRealPath(demoRoot);
        qa_createFilesAndDirectories(demoRoot);
        qa_copyMoveDelete(demoRoot);
        qa_readWriteAPIs(demoRoot);
        qa_attributesAndViews(demoRoot);
        qa_directoryListingAndWalking(demoRoot);
        qa_fileVisitorTraverseAndDeleteExample(demoRoot);
        qa_symbolicLinksAndHardLinks(demoRoot);
        qa_watchService(demoRoot);
        qa_seekableByteChannel(demoRoot);
        qa_asynchronousFileChannel(demoRoot);
        qa_pathMatcher(demoRoot);
        qa_zipFileSystem(demoRoot);
        qa_fileStoresAndSpace();
        qa_tempFiles(demoRoot);
        qa_probeContentType(demoRoot);
        qa_commonExceptions(demoRoot);

        cleanupDemoRoot(demoRoot);
        log("Cleanup done: " + demoRoot);
    }

    private static void log(String msg) { System.out.println(msg); }

    private static Path setupDemoRoot() throws IOException {
        Path root = Files.createTempDirectory("_nio2_paths_files_demo_");
        root.toFile().deleteOnExit();
        return root;
    }

    private static void cleanupDemoRoot(Path root) {
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.deleteIfExists(file);
                    return FileVisitResult.CONTINUE;
                }
                @Override public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.deleteIfExists(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ignored) {
        }
    }

    /*
    Q: What is NIO.2? How do Paths and Files relate to java.io.File?
    A: NIO.2 (Java 7+) modernizes filesystem APIs. Path models a platform-independent path.
       Files provides static utilities (create, copy, move, delete, read/write, attributes).
       File (java.io) is older; Path + Files supersede most use cases.
    */
    private static void qa_whatIsNio2() {
        Path p = Paths.get("a", "b", "c.txt"); // or Path.of(...) on Java 11+
        log("Path sample: " + p + " isAbsolute? " + p.isAbsolute());
        log("Default FileSystem: " + FileSystems.getDefault().provider());
        log("Supported views: " + FileSystems.getDefault().supportedFileAttributeViews());
    }

    /*
    Q: How do you create and inspect Paths?
    A: Use Paths.get(...). Inspect with getRoot/getParent/getFileName/getNameCount/subpath.
       Convert using toAbsolutePath, toUri.
    */
    private static void qa_pathCreationAndInspection(Path demo) throws IOException {
        Path relative = Paths.get("relative", "demo.txt");
        Path absolute = relative.toAbsolutePath();
        Path inDemo = demo.resolve("nested/inside.txt");

        log("relative=" + relative + ", absolute=" + absolute);
        log("absolute root=" + absolute.getRoot() + ", parent=" + absolute.getParent() + ", fileName=" + absolute.getFileName());
        if (absolute.getNameCount() >= 2)
            log("subpath(0,2)=" + absolute.subpath(0, 2));
        log("toUri=" + inDemo.toUri());

        // Create the nested parent dirs then file
        Files.createDirectories(inDemo.getParent());
        Files.write(inDemo, Arrays.asList("hello from NIO.2"), StandardCharsets.UTF_8);
        log("Created file exists? " + Files.exists(inDemo));
    }

    /*
    Q: How to normalize, resolve, and relativize paths?
    A: normalize removes . and .. segments; resolve joins paths; relativize computes relative path between two.
    */
    private static void qa_normalizeResolveRelativize(Path demo) {
        Path messy = demo.resolve("a/./b/../c.txt");
        Path normalized = messy.normalize();
        log("messy=" + messy);
        log("normalized=" + normalized);

        Path child = demo.resolve("x/y/z.txt");
        Path relativeFromDemo = demo.relativize(child);
        log("resolve demo + x/y/z.txt = " + child);
        log("relativize(demo->child)=" + relativeFromDemo);
    }

    /*
    Q: Difference between Path.equals and Files.isSameFile? What about toRealPath?
    A: Path.equals compares path strings; Files.isSameFile checks filesystem identity (follows links).
       toRealPath resolves symbolic links and returns canonical absolute path (throws if not exists).
    */
    private static void qa_equalsVs_isSameFile_vs_toRealPath(Path demo) throws IOException {
        Path original = Files.write(demo.resolve("samefile.txt"), Arrays.asList("data"), StandardCharsets.UTF_8);
        Path aliasNormalized = demo.resolve("dir/..").resolve("samefile.txt").normalize();
        log("equals? " + original.equals(aliasNormalized)); // likely true after normalize

        // Prefer a hard link demo for isSameFile (if supported)
        Path hardLink = demo.resolve("samefile_hardlink.txt");
        try {
            Files.createLink(hardLink, original);
            log("Hard link created: " + hardLink);
            log("equals(original, hardLink)? " + original.equals(hardLink));
            log("isSameFile(original, hardLink)? " + Files.isSameFile(original, hardLink));
        } catch (IOException e) {
            log("Hard links not supported or failed: " + e.getClass().getSimpleName());
        }

        log("toRealPath(original)=" + original.toRealPath());
        try {
            demo.resolve("missing.txt").toRealPath();
        } catch (IOException e) {
            log("toRealPath missing throws: " + e.getClass().getSimpleName());
        }
    }

    /*
    Q: How to create files and directories? What's the difference between createDirectory and createDirectories?
    A: createFile creates a new file (fails if exists), createDirectory creates a single directory,
       createDirectories creates all nonexistent parents.
    */
    private static void qa_createFilesAndDirectories(Path demo) {
        try {
            Path dir = demo.resolve("dir1/dir2");
            Files.createDirectories(dir);
            Path file = dir.resolve("hello.txt");
            Files.createFile(file);
            log("Created: " + file + ", exists=" + Files.exists(file));
        } catch (IOException e) {
            log("Create operations: " + e);
        }
    }

    /*
    Q: How to copy/move/delete with options? What is ATOMIC_MOVE?
    A: Files.copy/move/delete. Options: REPLACE_EXISTING, COPY_ATTRIBUTES, ATOMIC_MOVE (best-effort atomic rename).
       deleteIfExists doesn't throw if missing.
    */
    private static void qa_copyMoveDelete(Path demo) throws IOException {
        Path src = Files.write(demo.resolve("copy_src.txt"), Arrays.asList("copy me"), StandardCharsets.UTF_8);
        Path dst = demo.resolve("copy_dst.txt");
        Files.copy(src, dst, REPLACE_EXISTING, COPY_ATTRIBUTES);
        log("Copied to: " + dst + ", size=" + Files.size(dst));

        Path moved = demo.resolve("moved.txt");
        try {
            Files.move(dst, moved, REPLACE_EXISTING, ATOMIC_MOVE);
            log("Moved to: " + moved);
        } catch (IOException e) {
            // ATOMIC_MOVE may not be supported cross-volume; fall back without it
            Files.move(dst, moved, REPLACE_EXISTING);
            log("Moved without ATOMIC_MOVE: " + moved);
        }

        boolean deleted = Files.deleteIfExists(moved);
        log("deleteIfExists(" + moved + ") -> " + deleted);
    }

    /*
    Q: How do you read/write with Files? What are common options?
    A: Use readAllLines, newBufferedReader/Writer, newInputStream/OutputStream; options like CREATE, TRUNCATE_EXISTING, APPEND.
    */
    private static void qa_readWriteAPIs(Path demo) throws IOException {
        Path f = demo.resolve("io.txt");
        Files.write(f, Arrays.asList("line1", "line2"), StandardCharsets.UTF_8, CREATE, TRUNCATE_EXISTING);
        List<String> lines = Files.readAllLines(f, StandardCharsets.UTF_8);
        log("readAllLines: " + lines);

        try (BufferedWriter bw = Files.newBufferedWriter(f, StandardCharsets.UTF_8, APPEND)) {
            bw.write("line3\n");
        }
        try (BufferedReader br = Files.newBufferedReader(f, StandardCharsets.UTF_8)) {
            log("BufferedReader first line: " + br.readLine());
        }

        try (OutputStream os = Files.newOutputStream(f, APPEND)) {
            os.write("bytes\n".getBytes(StandardCharsets.UTF_8));
        }
        try (InputStream is = Files.newInputStream(f)) {
            byte[] buf = new byte[64];
            int n = is.read(buf);
            log("newInputStream read bytes: " + n);
        }
    }

    /*
    Q: How to read/set file attributes? What views exist?
    A: Basic (portable), POSIX (unix), DOS (Windows), owner, ACL. Use Files.readAttributes or *AttributeView.
    */
    private static void qa_attributesAndViews(Path demo) throws IOException {
        Path f = Files.write(demo.resolve("attrs.txt"), Arrays.asList("attrs"), StandardCharsets.UTF_8, CREATE, TRUNCATE_EXISTING);
        BasicFileAttributes basic = Files.readAttributes(f, BasicFileAttributes.class);
        log("Basic: size=" + basic.size() + ", isReg=" + basic.isRegularFile() + ", created=" + basic.creationTime());

        // Set lastModifiedTime
        Files.setLastModifiedTime(f, FileTime.from(Instant.now().minusSeconds(60)));
        log("lastModified=" + Files.getLastModifiedTime(f));

        // Owner
        FileOwnerAttributeView ownerView = Files.getFileAttributeView(f, FileOwnerAttributeView.class);
        if (ownerView != null) {
            UserPrincipal owner = ownerView.getOwner();
            log("Owner=" + owner);
        }

        // Hidden, size, permissions checks
        log("isHidden? " + safeIsHidden(f) + ", readable=" + Files.isReadable(f) + ", writable=" + Files.isWritable(f) + ", executable=" + Files.isExecutable(f));

        // POSIX permissions (if supported)
        if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
            PosixFileAttributeView pv = Files.getFileAttributeView(f, PosixFileAttributeView.class);
            if (pv != null) {
                Set<PosixFilePermission> perms = Files.getPosixFilePermissions(f);
                log("POSIX perms(before): " + perms);
                Set<PosixFilePermission> newPerms = EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
                Files.setPosixFilePermissions(f, newPerms);
                log("POSIX perms(after): " + Files.getPosixFilePermissions(f));
            }
        } else {
            // DOS attributes on Windows
            DosFileAttributeView dv = Files.getFileAttributeView(f, DosFileAttributeView.class);
            if (dv != null) {
                dv.setReadOnly(false); // ensure writable
                log("DOS readOnly=" + dv.readAttributes().isReadOnly());
            }
        }
    }

    private static boolean safeIsHidden(Path p) {
        try { return Files.isHidden(p); } catch (IOException e) { return false; }
    }

    /*
    Q: How to list directories and walk trees?
    A: DirectoryStream (iterator, filter), Files.list/find/walk (streams, be sure to close), maxDepth control.
    */
    private static void qa_directoryListingAndWalking(Path demo) throws IOException {
        Path dir = demo.resolve("list");
        Files.createDirectories(dir);
        Files.write(dir.resolve("a.txt"), Arrays.asList("a"), StandardCharsets.UTF_8);
        Files.write(dir.resolve("b.log"), Arrays.asList("b"), StandardCharsets.UTF_8);
        Files.createDirectories(dir.resolve("sub"));
        Files.write(dir.resolve("sub/c.txt"), Arrays.asList("c"), StandardCharsets.UTF_8);

        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, "*.txt")) {
            for (Path p : ds) log("DirectoryStream *.txt -> " + p.getFileName());
        }

        try (java.util.stream.Stream<Path> s = Files.list(dir)) {
            long count = s.count();
            log("Files.list count=" + count);
        }

        try (java.util.stream.Stream<Path> walk = Files.walk(dir, 3)) {
            long txtCount = walk.filter(p -> p.toString().endsWith(".txt")).count();
            log("Files.walk .txt count=" + txtCount);
        }

        try (java.util.stream.Stream<Path> find = Files.find(dir, 3, (p, attrs) -> attrs.isRegularFile() && p.getFileName().toString().endsWith(".txt"))) {
            List<Path> found = new ArrayList<>();
            find.forEach(found::add);
            log("Files.find .txt -> " + found);
        }
    }

    /*
    Q: How does FileVisitor work? Use cases?
    A: Walk file trees with preVisitDirectory/visitFile/postVisitDirectory to implement bulk ops (copy, delete, search).
    */
    private static void qa_fileVisitorTraverseAndDeleteExample(Path demo) throws IOException {
        Path tree = demo.resolve("tree/a/b/c");
        Files.createDirectories(tree);
        Files.write(tree.resolve("f1.txt"), Arrays.asList("1"), StandardCharsets.UTF_8);
        Files.write(tree.getParent().resolve("f2.txt"), Arrays.asList("2"), StandardCharsets.UTF_8);

        // Count files/dirs
        class CountingVisitor extends SimpleFileVisitor<Path> {
            int files, dirs;
            @Override public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) { dirs++; return FileVisitResult.CONTINUE; }
            @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) { files++; return FileVisitResult.CONTINUE; }
        }
        CountingVisitor v = new CountingVisitor();
        Files.walkFileTree(demo.resolve("tree"), v);
        log("FileVisitor counts: files=" + v.files + ", dirs=" + v.dirs);

        // Delete the subtree
        Files.walkFileTree(demo.resolve("tree"), new SimpleFileVisitor<Path>() {
            @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file); return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir); return FileVisitResult.CONTINUE;
            }
        });
        log("Deleted subtree 'tree'");
    }

    /*
    Q: Symbolic vs Hard links?
    A: Symbolic link points to a path (can cross FS, can dangle). Hard link is another name for the same inode (same FS).
       Use Files.createSymbolicLink, Files.createLink. Use isSymbolicLink and readSymbolicLink to inspect.
    */
    private static void qa_symbolicLinksAndHardLinks(Path demo) throws IOException {
        Path target = Files.write(demo.resolve("link_target.txt"), Arrays.asList("target"), StandardCharsets.UTF_8);
        // Symlink (may require privileges on Windows)
        Path sym = demo.resolve("link_sym.txt");
        try {
            Path relTarget = sym.getParent().relativize(target);
            Files.createSymbolicLink(sym, relTarget);
            log("Created symlink: " + sym + " -> " + Files.readSymbolicLink(sym));
            log("isSymbolicLink? " + Files.isSymbolicLink(sym));
            log("isRegularFile(sym, NOFOLLOW_LINKS)=" + Files.isRegularFile(sym, NOFOLLOW_LINKS));
            log("isSameFile(sym,target)? " + Files.isSameFile(sym, target));
        } catch (UnsupportedOperationException | IOException | SecurityException e) {
            log("Symlink not created: " + e.getClass().getSimpleName());
        }

        // Hard link
        Path hard = demo.resolve("link_hard.txt");
        try {
            Files.createLink(hard, target);
            log("Hard link created: " + hard + ", isSameFile? " + Files.isSameFile(hard, target));
        } catch (UnsupportedOperationException | IOException | SecurityException e) {
            log("Hard link not created: " + e.getClass().getSimpleName());
        }
    }

    /*
    Q: How to watch filesystem events?
    A: Use WatchService, register a directory with ENTRY_CREATE/MODIFY/DELETE, poll/take keys and process events.
    */
    private static void qa_watchService(Path demo) throws Exception {
        Path watched = Files.createDirectories(demo.resolve("watched"));
        try (WatchService ws = FileSystems.getDefault().newWatchService()) {
            watched.register(ws, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);

            Thread stim = new Thread(() -> {
                try {
                    Path f = watched.resolve("watch.txt");
                    Files.write(f, Arrays.asList("x"), StandardCharsets.UTF_8);
                    Thread.sleep(150);
                    Files.write(f, Arrays.asList("y"), StandardCharsets.UTF_8, TRUNCATE_EXISTING);
                    Thread.sleep(150);
                    Files.delete(f);
                } catch (Exception ignored) { }
            });
            stim.start();

            long end = System.currentTimeMillis() + 3000;
            while (System.currentTimeMillis() < end) {
                WatchKey key = ws.poll(500, TimeUnit.MILLISECONDS);
                if (key == null) continue;
                for (WatchEvent<?> ev : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = ev.kind();
                    Path ctx = (Path) ev.context();
                    log("Watch event: " + kind.name() + " -> " + ctx);
                }
                if (!key.reset()) break;
            }
            stim.join();
        }
    }

    /*
    Q: What is SeekableByteChannel and when to use it?
    A: Random access read/write with position control. Create via Files.newByteChannel.
    */
    private static void qa_seekableByteChannel(Path demo) throws IOException {
        Path file = demo.resolve("channel.bin");
        try (SeekableByteChannel ch = Files.newByteChannel(file, EnumSet.of(CREATE, WRITE, READ, TRUNCATE_EXISTING))) {
            ByteBuffer out = ByteBuffer.wrap("HelloChannel".getBytes(StandardCharsets.UTF_8));
            ch.write(out);
            ch.position(0);
            ByteBuffer in = ByteBuffer.allocate(32);
            int n = ch.read(in);
            in.flip();
            byte[] data = new byte[n];
            in.get(data);
            log("SeekableByteChannel read: " + new String(data, StandardCharsets.UTF_8));
        }
    }

    /*
    Q: How to do async file I/O with AsynchronousFileChannel?
    A: Open with AsynchronousFileChannel and use Future or CompletionHandler for non-blocking ops.
    */
    private static void qa_asynchronousFileChannel(Path demo) throws Exception {
        Path file = demo.resolve("async.txt");
        try (AsynchronousFileChannel ch = AsynchronousFileChannel.open(file, CREATE, READ, WRITE)) {
            ByteBuffer toWrite = ByteBuffer.wrap("AsyncIO".getBytes(StandardCharsets.UTF_8));
            Future<Integer> wf = ch.write(toWrite, 0);
            wf.get(); // wait
            ByteBuffer readBuf = ByteBuffer.allocate(16);
            Future<Integer> rf = ch.read(readBuf, 0);
            rf.get();
            readBuf.flip();
            byte[] data = new byte[readBuf.remaining()];
            readBuf.get(data);
            log("AsynchronousFileChannel data: " + new String(data, StandardCharsets.UTF_8));
        }
    }

    /*
    Q: How to match paths with glob/regex?
    A: Use FileSystem.getPathMatcher with "glob:" or "regex:". Example "glob:/.txt".
    */
    private static void qa_pathMatcher(Path demo) throws IOException {
        Path dir = demo.resolve("match");
        Files.createDirectories(dir.resolve("sub"));
        Files.write(dir.resolve("a.txt"), Arrays.asList("a"), StandardCharsets.UTF_8);
        Files.write(dir.resolve("sub/b.txt"), Arrays.asList("b"), StandardCharsets.UTF_8);
        Files.write(dir.resolve("c.log"), Arrays.asList("c"), StandardCharsets.UTF_8);

        PathMatcher m = FileSystems.getDefault().getPathMatcher("glob:**/*.txt");
        try (java.util.stream.Stream<Path> walk = Files.walk(dir)) {
            walk.filter(p -> m.matches(p)).forEach(p -> log("PathMatcher match: " + p));
        }
    }

    /*
    Q: How to use a Zip/Jar as a FileSystem?
    A: newFileSystem(URI "jar:file:/...zip", env). Then use Paths inside it.
    */
    private static void qa_zipFileSystem(Path demo) throws IOException {
        Path zip = demo.resolve("demo.zip");
        Map<String, String> env = new HashMap<>();
        env.put("create", "true");
        URI uri = URI.create("jar:" + zip.toUri());
        try (FileSystem zfs = FileSystems.newFileSystem(uri, env)) {
            Path inside = zfs.getPath("/hello.txt");
            Files.write(inside, Arrays.asList("inside zip"), StandardCharsets.UTF_8, CREATE, TRUNCATE_EXISTING);
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(zfs.getPath("/"))) {
                for (Path p : ds) log("Zip FS entry: " + p);
            }
        }
        log("Zip created: " + zip + ", size=" + Files.size(zip));
    }

    /*
    Q: How to query FileStores (volumes) and space?
    A: FileSystems.getDefault().getFileStores() and methods: totalSpace, usableSpace, unallocatedSpace.
    */
    private static void qa_fileStoresAndSpace() {
        for (FileStore fs : FileSystems.getDefault().getFileStores()) {
            try {
                log("FileStore: " + fs.name() + " [" + fs.type() + "] total=" + fs.getTotalSpace() + " usable=" + fs.getUsableSpace());
            } catch (IOException ignored) { }
        }
    }

    /*
    Q: How to create temp files/dirs safely?
    A: Files.createTempFile/createTempDirectory. Prefer controlled locations and delete after use.
    */
    private static void qa_tempFiles(Path demo) throws IOException {
        Path tempDir = Files.createTempDirectory(demo, "tmpdir_");
        Path tempFile = Files.createTempFile(tempDir, "tmp_", ".txt");
        Files.write(tempFile, Arrays.asList("temp"), StandardCharsets.UTF_8);
        log("Temp created: " + tempFile + ", will delete");
        Files.deleteIfExists(tempFile);
        Files.deleteIfExists(tempDir);
    }

    /*
    Q: How to detect content type (MIME)?
    A: Files.probeContentType (provider/OS-dependent; may return null).
    */
    private static void qa_probeContentType(Path demo) throws IOException {
        Path f = demo.resolve("mime.html");
        Files.write(f, Arrays.asList("<html></html>"), StandardCharsets.UTF_8);
        String type = Files.probeContentType(f);
        log("probeContentType: " + type);
    }

    /*
    Q: Common exceptions you should know?
    A: NoSuchFileException (missing target), FileAlreadyExistsException (create on existing),
       AccessDeniedException (permissions), DirectoryNotEmptyException (delete non-empty).
    */
    private static void qa_commonExceptions(Path demo) throws IOException {
        // FileAlreadyExistsException
        Path f1 = demo.resolve("exists.txt");
        Files.write(f1, Arrays.asList("x"), StandardCharsets.UTF_8);
        try {
            Files.createFile(f1);
        } catch (IOException e) {
            log("createFile on existing -> " + e.getClass().getSimpleName());
        }

        // DirectoryNotEmptyException
        Path nd = demo.resolve("nonempty");
        Files.createDirectories(nd);
        Files.write(nd.resolve("file.txt"), Arrays.asList("y"), StandardCharsets.UTF_8);
        try {
            Files.delete(nd);
        } catch (IOException e) {
            log("delete non-empty dir -> " + e.getClass().getSimpleName());
        }

        // NoSuchFileException
        try {
            Files.size(demo.resolve("no_such.txt"));
        } catch (IOException e) {
            log("size on missing -> " + e.getClass().getSimpleName());
        }
    }
}