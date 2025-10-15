package _09_02_nio2_paths_files;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.time.Instant;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * NIO.2 (Paths, Files) – compact, commented examples covering:
 * - Path creation and inspection (Path.of, Paths.get, parts, normalize, resolve, relativize)
 * - Files operations (create, copy, move, delete, exists checks)
 * - Reading/writing (strings, bytes, buffered, channels)
 * - Listing, walking, finding
 * - Attributes (basic, POSIX/DOS/owner), times
 * - PathMatcher (glob/regex)
 * - toRealPath, isSameFile, URI
 * - Content type probing, mismatch
 * - FileVisitor (copy tree)
 * - Links (symbolic/hard) – attempted, guarded for OS/permissions
 */
public class _02_Examples {

    public static void main(String[] args) throws Exception {
        Path base = null;
        try {
            base = Files.createTempDirectory("_nio2_paths_files_demo_");
            System.out.println("Demo base: " + base);
            demoPathsBasics(base);
            demoNormalizeResolveRelativize(base);
            demoFilesCreateCopyMoveDelete(base);
            demoReadWrite(base);
            demoListWalkFind(base);
            demoAttributes(base);
            demoPathMatcher(base);
            demoToRealPathAndSameFile(base);
            demoUriAndFileSystems(base);
            demoMismatchAndContentType(base);
            demoFileVisitorCopyTree(base);
            demoLinksSymlinks(base);
        } finally {
            cleanupRecursive(base);
        }
    }

    // 1) Path basics: creation and inspection
    private static void demoPathsBasics(Path base) {
        header("Paths basics");
        Path p1 = Path.of("a", "b", "c.txt");          // Preferred var-args factory
        Path p2 = Paths.get("a/b/c.txt");              // Equivalent (OS-safe, '/' accepted)
        System.out.println("p1: " + p1);
        System.out.println("p2 equals p1: " + p2.equals(p1));

        System.out.println("fileName: " + p1.getFileName());
        System.out.println("nameCount: " + p1.getNameCount());
        for (int i = 0; i < p1.getNameCount(); i++) {
            System.out.println("  part[" + i + "]: " + p1.getName(i));
        }
        System.out.println("parent: " + p1.getParent());
        System.out.println("root (likely null for relative): " + p1.getRoot());
        System.out.println("isAbsolute: " + p1.isAbsolute());
        System.out.println("toAbsolutePath: " + p1.toAbsolutePath());
        System.out.println("subpath(0,2): " + p1.subpath(0, 2)); // a/b

        // startsWith / endsWith compare path elements (not strings)
        System.out.println("startsWith('a'): " + p1.startsWith(Path.of("a")));
        System.out.println("endsWith('c.txt'): " + p1.endsWith(Path.of("c.txt")));

        System.out.println("base root: " + base.getRoot());
    }

    // 2) Normalize, resolve, relativize
    private static void demoNormalizeResolveRelativize(Path base) throws IOException {
        header("Normalize, resolve, relativize");
        Path messy = Path.of("x", ".", "y", "..", "z");
        System.out.println("messy: " + messy);
        System.out.println("normalized: " + messy.normalize()); // x/z

        Path dir = base.resolve("wildlife/animals");
        Files.createDirectories(dir);
        Path file = dir.resolve("lion.txt");
        Files.writeString(file, "king", StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        System.out.println("resolved file: " + file);

        Path sibling = file.resolveSibling("lion_renamed.txt");
        System.out.println("resolveSibling: " + sibling);

        Path rel = base.relativize(file);
        System.out.println("relativize(base -> file): " + rel);
        try {
            // Mixing absolute and relative -> IllegalArgumentException
            System.out.println("relativize(abs -> rel): " + file.relativize(Path.of("rel")));
        } catch (IllegalArgumentException e) {
            System.out.println("relativize mixed roots fails: " + e.getClass().getSimpleName());
        }
    }

    // 3) Create, copy, move, delete
    private static void demoFilesCreateCopyMoveDelete(Path base) throws IOException {
        header("Create, copy, move, delete");
        Path srcDir = base.resolve("src/sub");
        Files.createDirectories(srcDir);

        Path alpha = base.resolve("src/alpha.txt");
        Path beta = srcDir.resolve("beta.txt");
        Files.writeString(alpha, "alpha\n", StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        Files.writeString(beta, "beta\n", StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        System.out.println("exists alpha: " + Files.exists(alpha));
        System.out.println("isRegularFile beta: " + Files.isRegularFile(beta));
        System.out.println("size(alpha): " + Files.size(alpha));

        Path dstDir = base.resolve("dst");
        Files.createDirectories(dstDir);
        Path alphaCopy = dstDir.resolve("alpha_copy.txt");
        Files.copy(alpha, alphaCopy, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("copied -> " + alphaCopy);

        Path alphaMoved = dstDir.resolve("alpha_moved.txt");
        Files.move(alphaCopy, alphaMoved, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("moved -> " + alphaMoved);

        Path created = dstDir.resolve("new.txt");
        try {
            Files.createFile(created);
            System.out.println("created file: " + created);
        } catch (FileAlreadyExistsException ignore) {}

        try {
            Files.delete(alphaMoved);
            System.out.println("deleted: " + alphaMoved);
        } catch (IOException e) {
            System.out.println("delete failed: " + e.getMessage());
        }

        Path tmpFile = Files.createTempFile(base, "my", ".tmp");
        Path tmpDir = Files.createTempDirectory(base, "mydir_");
        System.out.println("temp file: " + tmpFile.getFileName());
        System.out.println("temp dir: " + tmpDir.getFileName());

        // Attempt to delete non-empty directory (will fail)
        try {
            Files.delete(dstDir);
        } catch (DirectoryNotEmptyException e) {
            System.out.println("delete non-empty dir failed: " + e.getClass().getSimpleName());
        }
    }

    // 4) Reading/writing: strings, bytes, buffered, channel
    private static void demoReadWrite(Path base) throws IOException {
        header("Read & write");
        Path notes = base.resolve("io/notes.txt");
        Files.createDirectories(notes.getParent());

        Files.writeString(notes, "Line1\nLine2\n", StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        Files.writeString(notes, "Appended\n", StandardOpenOption.APPEND);

        String content = Files.readString(notes);
        System.out.println("readString length: " + content.length());

        List<String> lines = Files.readAllLines(notes);
        System.out.println("readAllLines count: " + lines.size());

        try (Stream<String> s = Files.lines(notes)) {
            System.out.println("lines() count: " + s.count());
        }

        byte[] all = Files.readAllBytes(notes);
        System.out.println("readAllBytes length: " + all.length);

        Path bin = base.resolve("io/data.bin");
        try (SeekableByteChannel ch = Files.newByteChannel(bin,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            ch.write(ByteBuffer.wrap(new byte[]{1, 2, 3, 4}));
        }
        System.out.println("binary size: " + Files.size(bin));

        // Buffered reader/writer
        try (BufferedWriter bw = Files.newBufferedWriter(notes, Charset.defaultCharset(), StandardOpenOption.APPEND)) {
            bw.write("Buffered line\n");
        }
        try (BufferedReader br = Files.newBufferedReader(notes)) {
            System.out.println("first char: " + (char) br.read());
        }
    }

    // 5) List, walk, find
    private static void demoListWalkFind(Path base) throws IOException {
        header("List, walk, find");
        try (Stream<Path> s = Files.list(base)) {
            System.out.println("list(base) children: " + s.count());
        }
        try (Stream<Path> s = Files.walk(base, 2)) {
            long files = s.filter(Files::isRegularFile).count();
            System.out.println("walk depth=2 files: " + files);
        }
        try (Stream<Path> s = Files.find(base, 5,
                (p, a) -> a.isRegularFile() && p.getFileName().toString().endsWith(".txt"))) {
            System.out.println("find *.txt (<=5 deep): " + s.count());
        }
    }

    // 6) Attributes: basic, POSIX/DOS/owner, times
    private static void demoAttributes(Path base) throws IOException {
        header("Attributes");
        Path file = base.resolve("io/notes.txt");
        System.out.println("isReadable: " + Files.isReadable(file));
        System.out.println("isWritable: " + Files.isWritable(file));
        System.out.println("isExecutable: " + Files.isExecutable(file));

        try {
            System.out.println("isHidden: " + Files.isHidden(file));
        } catch (IOException e) {
            System.out.println("isHidden check failed: " + e.getClass().getSimpleName());
        }

        BasicFileAttributes basic = Files.readAttributes(file, BasicFileAttributes.class);
        System.out.println("size: " + basic.size());
        System.out.println("creationTime: " + basic.creationTime());
        System.out.println("lastModifiedTime: " + basic.lastModifiedTime());
        System.out.println("isDirectory: " + basic.isDirectory());

        Files.setLastModifiedTime(file, FileTime.from(Instant.now()));
        System.out.println("updated lastModifiedTime: " + Files.getLastModifiedTime(file));

        Object isReg = Files.getAttribute(file, "basic:isRegularFile", LinkOption.NOFOLLOW_LINKS);
        System.out.println("getAttribute basic:isRegularFile: " + isReg);

        // Owner
        FileOwnerAttributeView ownerView = Files.getFileAttributeView(file, FileOwnerAttributeView.class);
        if (ownerView != null) {
            UserPrincipal owner = ownerView.getOwner();
            System.out.println("owner: " + owner.getName());
        }

        // POSIX (Unix/Mac)
        if (isPosixSupported()) {
            Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rw-r-----");
            Files.setPosixFilePermissions(file, perms);
            PosixFileAttributes posix = Files.readAttributes(file, PosixFileAttributes.class);
            System.out.println("posix perms: " + PosixFilePermissions.toString(posix.permissions()));
            System.out.println("posix group: " + posix.group().getName());
        } else {
            System.out.println("POSIX not supported");
        }

        // DOS (Windows)
        if (isDosSupported()) {
            DosFileAttributeView dos = Files.getFileAttributeView(file, DosFileAttributeView.class);
            System.out.println("dos readonly: " + dos.readAttributes().isReadOnly());
        } else {
            System.out.println("DOS not supported");
        }
    }

    // 7) PathMatcher: glob and regex
    private static void demoPathMatcher(Path base) throws IOException {
        header("PathMatcher");
        Path txt1 = base.resolve("src/alpha.txt");
        Path txt2 = base.resolve("io/notes.txt");
        PathMatcher globTxt = FileSystems.getDefault().getPathMatcher("glob:**/*.txt");
        System.out.println("glob **/*.txt alpha: " + globTxt.matches(txt1));
        System.out.println("glob **/*.txt notes: " + globTxt.matches(txt2));

        PathMatcher globGroup = FileSystems.getDefault().getPathMatcher("glob:**/{alpha,beta}.txt");
        System.out.println("glob group {alpha,beta}.txt alpha: " + globGroup.matches(txt1));
        System.out.println("glob group {alpha,beta}.txt notes: " + globGroup.matches(txt2));

        PathMatcher regexTxt = FileSystems.getDefault().getPathMatcher("regex:.*\\.txt");
        System.out.println("regex .*\\.txt alpha: " + regexTxt.matches(txt1));
    }

    // 8) toRealPath, isSameFile
    private static void demoToRealPathAndSameFile(Path base) throws IOException {
        header("toRealPath & isSameFile");
        Path canonical = base.resolve("src/alpha.txt");
        Path disguised = base.resolve("src/../src/./alpha.txt");
        System.out.println("canonical: " + canonical);
        System.out.println("disguised: " + disguised);
        System.out.println("isSameFile: " + Files.isSameFile(canonical, disguised));
        System.out.println("real canonical: " + canonical.toRealPath());
        System.out.println("real disguised: " + disguised.toRealPath(LinkOption.NOFOLLOW_LINKS));
    }

    // 9) URI, FileSystem
    private static void demoUriAndFileSystems(Path base) {
        header("URI and FileSystem");
        Path p = base.resolve("src/alpha.txt");
        URI uri = p.toUri();
        Path back = Paths.get(uri);
        System.out.println("toUri: " + uri);
        System.out.println("Paths.get(uri) equals: " + back.equals(p));

        FileSystem fs = FileSystems.getDefault();
        System.out.println("separator: " + fs.getSeparator());
        System.out.println("supported views: " + fs.supportedFileAttributeViews());
        for (FileStore store : fs.getFileStores()) {
            System.out.println("store: " + store.name() + " (" + store.type() + ")");
            break; // print first only
        }
    }

    // 10) Content type, mismatch
    private static void demoMismatchAndContentType(Path base) throws IOException {
        header("Content type & mismatch");
        Path a = base.resolve("compare/a.txt");
        Path b = base.resolve("compare/b.txt");
        Files.createDirectories(a.getParent());
        Files.writeString(a, "Hello");
        Files.writeString(b, "Hellx");
        long diff = Files.mismatch(a, b); // -1 if equal
        System.out.println("mismatch index (a vs b): " + diff);
        String type = Files.probeContentType(a);
        System.out.println("probeContentType(a): " + type);
    }

    // 11) FileVisitor: copy tree
    private static void demoFileVisitorCopyTree(Path base) throws IOException {
        header("FileVisitor copy tree");
        Path srcRoot = base.resolve("src");
        Path dstRoot = base.resolve("src_tree_copy");
        Files.walkFileTree(srcRoot, EnumSet.noneOf(FileVisitOption.class), Integer.MAX_VALUE,
                new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        Path relative = srcRoot.relativize(dir);
                        Path target = dstRoot.resolve(relative);
                        Files.createDirectories(target);
                        return FileVisitResult.CONTINUE;
                    }
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Path relative = srcRoot.relativize(file);
                        Path target = dstRoot.resolve(relative);
                        Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
                        return FileVisitResult.CONTINUE;
                    }
                });
        try (Stream<Path> s = Files.walk(dstRoot)) {
            System.out.println("copied entries: " + s.count());
        }
    }

    // 12) Links (symbolic, hard) – guarded
    private static void demoLinksSymlinks(Path base) {
        header("Links (symbolic/hard)");
        Path target = base.resolve("src/alpha.txt");
        Path linksDir = base.resolve("links");
        try {
            Files.createDirectories(linksDir);
        } catch (IOException ignore) {}

        // Symbolic link
        Path sym = linksDir.resolve("alpha.sym");
        try {
            Files.deleteIfExists(sym);
            Files.createSymbolicLink(sym, target);
            System.out.println("symlink created: " + sym + " -> " + Files.readSymbolicLink(sym));
            System.out.println("isSymbolicLink: " + Files.isSymbolicLink(sym));
            System.out.println("isSameFile(symlink, target): " + Files.isSameFile(sym, target));
        } catch (UnsupportedOperationException | IOException | SecurityException e) {
            System.out.println("symlink not created: " + e.getClass().getSimpleName());
        }

        // Hard link
        Path hard = linksDir.resolve("alpha.hard");
        try {
            Files.deleteIfExists(hard);
            Files.createLink(hard, target);
            System.out.println("hardlink created: " + hard);
            System.out.println("isSameFile(hard, target): " + Files.isSameFile(hard, target));
        } catch (UnsupportedOperationException | IOException | SecurityException e) {
            System.out.println("hardlink not created: " + e.getClass().getSimpleName());
        }
    }

    // Helpers

    private static void header(String title) {
        System.out.println("\n-- " + title + " --");
    }

    private static boolean isPosixSupported() {
        return FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
    }

    private static boolean isDosSupported() {
        return FileSystems.getDefault().supportedFileAttributeViews().contains("dos");
    }

    private static void cleanupRecursive(Path root) {
        if (root == null) return;
        try (Stream<Path> s = Files.walk(root)) {
            s.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignore) {}
            });
        } catch (IOException ignore) {}
    }
}