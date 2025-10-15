package _09_01_streams_filereader_bufferedreader;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

/**
 * Streams (FileReader, BufferedReader) – Interview Q&A from Basic to Advanced
 *
 * NOTE:
 * - This class compiles and contains many small, ready-to-use snippets.
 * - The Q&A are written as comments, grouped by difficulty.
 * - Methods show idiomatic usage and pitfalls you might be asked to explain or implement.
 */
public class _03_InterviewQA {

    public static void main(String[] args) throws Exception {
        // Minimal demo that does not depend on external files
        demoOnInMemoryText();
    }

    // ===========================
    // BASIC INTERVIEW Q&A
    // ===========================
    /*
    1) What is FileReader?
       - A convenience Reader to read characters from a file using the platform's default charset.

    2) What is BufferedReader?
       - A Reader wrapper that buffers input to reduce disk/OS calls and provides readLine().

    3) How are FileReader and BufferedReader different?
       - FileReader reads chars from a file directly (no buffering by itself).
       - BufferedReader adds a buffer and line-oriented reading via readLine().

    4) Should you use FileReader directly?
       - Rarely. Prefer wrapping in BufferedReader for performance and readLine().
         Or prefer Files.newBufferedReader(Path, Charset) for charset safety.

    5) Why is buffering important?
       - It minimizes I/O calls by reading larger chunks at once, improving performance.

    6) How do you properly close Readers?
       - Use try-with-resources to automatically close them and avoid resource leaks.

    7) How do you read a file line by line?
       - Use BufferedReader.readLine() in a loop (returns null at EOF).

    8) Does readLine() include the line terminator?
       - No. It strips the terminator; returned string contains no newline chars.

    9) Which line terminators does readLine() recognize?
       - \n, \r, or \r\n

    10) What does read() return?
        - The next character as an int (0..65535) or -1 at EOF.

    11) What is the default buffer size of BufferedReader?
        - Typically 8192 chars (8 KB), but treat as an implementation detail.

    12) Is BufferedReader thread-safe?
        - No. Do not share the same instance across threads without external synchronization.

    13) What is the difference between Reader and InputStream?
        - Reader is for characters (with decoding), InputStream is for bytes.

    14) What is InputStreamReader?
        - A bridge that decodes bytes from an InputStream into characters using a Charset.

    15) Why can FileReader be dangerous?
        - It uses the platform default charset implicitly, which may corrupt text if encoding differs
          (e.g., reading UTF-8 files on a system defaulting to Windows-1252).

    16) How do you specify the charset explicitly?
        - Wrap a FileInputStream in an InputStreamReader with a Charset, or use Files.newBufferedReader(path, charset).

    17) What does BufferedReader.ready() indicate?
        - That reading won't block for at least one character. It does NOT indicate EOF.

    18) Does closing a BufferedReader also close its underlying Reader?
        - Yes.

    19) Can BufferedReader read huge lines (larger than buffer size)?
        - Yes. It refills the buffer and builds the line, but memory usage can grow for extremely long lines.

    20) When should I not use FileReader/BufferedReader?
        - For binary data (use InputStream). For very large/high-performance tasks, consider NIO alternatives.

    21) How do you count lines efficiently?
        - Use BufferedReader and loop or use br.lines().count() with try-with-resources.

    22) How do you skip characters?
        - Use Reader.skip(n). It may skip fewer than requested; check return value.

    23) Do Readers/Writers need flushing?
        - Writers do, Readers do not. Readers only read.

    24) What happens if you call read() after close()?
        - Throws IOException.

    25) Can you read from classpath resources with FileReader?
        - No. FileReader needs a file path. Use getResourceAsStream + InputStreamReader + BufferedReader.
    */

    // ===========================
    // INTERMEDIATE INTERVIEW Q&A
    // ===========================
    /*
    26) Scanner vs BufferedReader?
        - Scanner tokenizes/parses (regex-based), convenient but slower and heavier.
        - BufferedReader is faster for raw line-oriented reading.

    27) How to handle character encodings robustly?
        - Never rely on default charset. Always specify Charset (e.g., UTF-8).

    28) How to create a BufferedReader with a custom buffer size?
        - new BufferedReader(reader, bufferSize).

    29) How to read lines as a Stream?
        - br.lines() returns Stream<String>. Must be closed to release resources.

    30) What are the semantics of mark() and reset() in BufferedReader?
        - BufferedReader supports marking. mark(readAheadLimit) allows reset() within the limit.
          If the limit is exceeded before reset, reset may fail.

    31) How to read a GZIP text file with a Reader?
        - Wrap FileInputStream in GZIPInputStream, then InputStreamReader with Charset, then BufferedReader.

    32) How to read a file from Path idiomatically?
        - Files.newBufferedReader(path, charset) for a Reader, or Files.lines(path, charset) for a Stream of lines.

    33) Is it safe to mix br.lines() with manual br.readLine()?
        - Avoid mixing. Once you use the Stream, the underlying reader's position is tied to the stream consumption.

    34) How to process huge files without OOM?
        - Stream/process line by line; do not accumulate all lines in memory.

    35) How to detect or handle BOM (Byte Order Mark)?
        - FileReader cannot. Use InputStream, detect BOM manually or rely on libraries.
          InputStreamReader ignores UTF-8 BOM by default in most JDKs? Historically, no—be explicit if BOM handling matters.

    36) How to read partial file from a specific offset with Reader?
        - Reader APIs are sequential. Use RandomAccessFile or NIO channels for random access, then wrap decoding if needed.

    37) What does Reader.skip(n) guarantee?
        - It attempts to skip up to n chars and returns the actual skipped count; not guaranteed to skip all in one call.

    38) How to get predictable performance for large files?
        - Use buffered reading with an appropriate buffer size, avoid per-char operations in tight loops, minimize allocations.

    39) What happens if the input includes invalid byte sequences for the chosen charset?
        - Decoding errors may occur; InputStreamReader uses the charset’s default decoding action (often replacement characters).

    40) How to read from network sockets with BufferedReader?
        - Wrap the socket's InputStream with InputStreamReader and then BufferedReader. Beware of blocking IO and timeouts.

    41) Is closing the Stream from br.lines() enough?
        - Yes. Closing the Stream closes the underlying BufferedReader.

    42) How to handle platform-dependent path/charset issues?
        - Use Path/Files APIs and always specify explicit charsets (e.g., StandardCharsets.UTF_8).

    43) How to unit test Reader logic?
        - Use StringReader to provide in-memory text to methods that accept a Reader.
    */

    // ===========================
    // ADVANCED INTERVIEW Q&A
    // ===========================
    /*
    44) Why is FileReader discouraged in modern code?
        - It locks you to the platform default charset. Prefer Files.newBufferedReader(path, charset) or
          InputStreamReader with explicit Charset for portability and correctness.

    45) How to implement a lookahead parser with Reader?
        - Use BufferedReader.mark/reset or PushbackReader to peek/push back characters.

    46) How to choose a buffer size?
        - Start with defaults. For large sequential reads, 32–128 KB can reduce syscalls.
          Benchmark in your workload; avoid micro-benchmark traps.

    47) Files.lines vs BufferedReader.readLine in a loop?
        - Files.lines is concise and returns a Stream; it may use buffering internally. readLine loop has minimal overhead and is explicit.
          Either is fine if properly closed.

    48) How to process multi-GB files robustly?
        - Stream line-by-line, avoid storing all data, ensure backpressure in pipelines, and handle malformed lines gracefully.

    49) Handling Windows vs Unix newlines?
        - readLine() normalizes by removing terminators. If you need to preserve exact line endings, read raw chars and detect '\r', '\n', '\r\n' manually.

    50) Can you decode bytes to chars without copying?
        - Not with classic Reader APIs. For peak performance, use NIO channels and CharsetDecoder on ByteBuffer/CharBuffer.

    51) When to use PushbackReader?
        - When implementing parsers where you need to unread characters after inspecting them (lookahead).

    52) Are Readers suitable for binary formats?
        - No. Readers are for text. For binary formats, use InputStream/ByteBuffer APIs.

    53) Any gotchas with br.ready()?
        - It’s not for EOF detection and not a reliability guarantee; mainly indicates non-blocking availability.

    54) How to avoid memory spikes with huge lines?
        - Instead of readLine(), read fixed-size char[] chunks and process delimiters yourself to stream out parts incrementally.

    55) How to ensure resources always close with streams?
        - Use try-with-resources around BufferedReader or around the Stream returned by Files.lines.

    56) Why avoid mixing Writers/Readers on the same file concurrently?
        - Can lead to inconsistent state due to buffering and OS caching. Coordinate access or use atomic file strategies.

    57) Does BufferedReader support mark/reset by default?
        - Yes. markSupported() returns true. Ensure sufficient readAheadLimit when marking.

    58) How to read locale-sensitive data?
        - Charset and locale are different. Charset impacts bytes->chars; locale impacts formatting/parsing of human language constructs.
          For text encoding, always specify Charset; for parsing, consider Locale with higher-level APIs.

    59) Any security considerations?
        - Validate paths (avoid traversal), limit file size processed, set timeouts for network sources, and sanitize parsed content.

    60) Why is Scanner slower than BufferedReader?
        - Scanner uses regex and tokenization; BufferedReader readLine() is simpler and faster for raw line reads.

    61) Can multiple threads read the same file with their own Readers?
        - Yes. Each thread should have its own Reader instance. Do not share a single Reader without synchronization.

    62) What are alternatives to FileReader/BufferedReader?
        - Files.newBufferedReader, Files.lines, NIO channels with CharsetDecoder, third-party libraries (Okio, etc.).

    63) How to handle surrogate pairs/unicode correctness?
        - Reader reads chars, preserving surrogate pairs. Avoid splitting a pair manually when chunk-processing; operate on chars, not bytes.

    64) Do I need to flush Readers?
        - No. Flushing applies to Writers/OutputStreams, not Readers/InputStreams.
    */

    // ===========================
    // CODE SNIPPETS YOU MAY BE ASKED TO WRITE
    // ===========================

    // 1) Read a file with FileReader + BufferedReader (default charset) and print lines
    public static void readLinesWithBufferedReader(Path path) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(path.toFile()))) {
            String line;
            while ((line = br.readLine()) != null) {
                // process line
                // System.out.println(line);
            }
        }
    }

    // 2) Read a file with explicit charset (preferred)
    public static void readLinesWithCharset(Path path, Charset charset) throws IOException {
        try (BufferedReader br = Files.newBufferedReader(path, charset)) {
            for (String line; (line = br.readLine()) != null; ) {
                // process line
            }
        }
    }

    // 3) Count lines with Stream API
    public static long countLines(Path path, Charset charset) throws IOException {
        try (Stream<String> lines = Files.lines(path, charset)) {
            return lines.count();
        }
    }

    // 4) Read chunk-by-chunk into a char[] buffer (efficient, no per-char overhead)
    public static void readCharsInChunks(Path path, Charset charset, int bufSize) throws IOException {
        try (Reader r = new BufferedReader(new InputStreamReader(new FileInputStream(path.toFile()), charset), bufSize)) {
            char[] buf = new char[bufSize];
            int n;
            while ((n = r.read(buf, 0, buf.length)) != -1) {
                // process buf[0..n)
            }
        }
    }

    // 5) Demonstrate mark/reset lookahead with BufferedReader
    public static void demoMarkReset(Reader reader) throws IOException {
        try (BufferedReader br = new BufferedReader(reader)) {
            int first = br.read(); // read one char
            br.mark(16);           // mark position with read-ahead limit
            char[] probe = new char[5];
            int n = br.read(probe, 0, probe.length); // read ahead a few chars
            br.reset();            // go back to marked position
            // Now a subsequent read will return the same chars we probed
        }
    }

    // 6) Read a GZIP-compressed text file
    public static void readGzipText(Path gzPath, Charset charset) throws IOException {
        try (GZIPInputStream gz = new GZIPInputStream(new FileInputStream(gzPath.toFile()));
             BufferedReader br = new BufferedReader(new InputStreamReader(gz, charset))) {
            for (String line; (line = br.readLine()) != null; ) {
                // process line
            }
        }
    }

    // 7) Read from classpath resource as text
    public static void readClasspathResource(String resourcePath, Charset charset) throws IOException {
        try (var in = _03_InterviewQA.class.getResourceAsStream(resourcePath)) {
            if (in == null) throw new IOException("Resource not found: " + resourcePath);
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, charset))) {
                for (String line; (line = br.readLine()) != null; ) {
                    // process line
                }
            }
        }
    }

    // 8) Use CharBuffer with Reader (less copying than temporary char[])
    public static void readWithCharBuffer(Path path, Charset charset) throws IOException {
        try (Reader r = new BufferedReader(new InputStreamReader(new FileInputStream(path.toFile()), charset))) {
            CharBuffer cb = CharBuffer.allocate(8192);
            while (true) {
                cb.clear();
                int n = r.read(cb);
                if (n == -1) break;
                cb.flip();
                // process cb (chars from position to limit)
            }
        }
    }

    // 9) Use PushbackReader for simple parsers (unread a character)
    public static void demoPushbackReader(String text) throws IOException {
        try (PushbackReader pr = new PushbackReader(new StringReader(text), 8)) {
            int c = pr.read();
            if (c == '#') {
                // comment line, push back and handle specially
                pr.unread(c);
                // handle comment...
            }
            // continue reading...
        }
    }

    // 10) Skip characters safely
    public static void skipChars(Reader reader, long toSkip) throws IOException {
        long remaining = toSkip;
        while (remaining > 0) {
            long skipped = reader.skip(remaining);
            if (skipped <= 0) {
                // read and discard one char to make progress or detect EOF
                if (reader.read() == -1) break;
                skipped = 1;
            }
            remaining -= skipped;
        }
    }

    // 11) Example: reading with a custom BufferedReader buffer size
    public static void readWithCustomBuffer(Path path, Charset charset, int bufferSize) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path.toFile()), charset), bufferSize)) {
            String line;
            while ((line = br.readLine()) != null) {
                // process
            }
        }
    }

    // 12) Avoid memory blow-up for very long logical records: process char-by-char
    public static void processUntilDelimiter(Path path, Charset charset, char delimiter) throws IOException {
        try (Reader r = new BufferedReader(new InputStreamReader(new FileInputStream(path.toFile()), charset), 64 * 1024)) {
            StringBuilder sb = new StringBuilder(1024);
            int ch;
            while ((ch = r.read()) != -1) {
                if (ch == delimiter) {
                    // process record in sb
                    sb.setLength(0);
                } else {
                    sb.append((char) ch);
                    // Optionally flush partial chunks if sb grows too large
                }
            }
            if (sb.length() > 0) {
                // process last record if needed
            }
        }
    }

    // 13) Lines Stream with filtering and mapping
    public static long countNonEmptyUtf8Lines(Path path) throws IOException {
        try (Stream<String> lines = Files.lines(path, StandardCharsets.UTF_8)) {
            return lines.filter(s -> !s.isBlank()).count();
        }
    }

    // 14) Demonstration on in-memory text (no file system required)
    private static void demoOnInMemoryText() throws IOException {
        String text = "alpha\r\nbeta\n\ngamma\rdelta";
        try (BufferedReader br = new BufferedReader(new StringReader(text))) {
            // Basic readLine loop
            for (String line; (line = br.readLine()) != null; ) {
                // System.out.println("[" + line + "]");
            }
        }
        // Mark/reset demo
        demoMarkReset(new StringReader("ABCDEFG\nHIJ"));
        // Pushback demo
        demoPushbackReader("#comment\nvalue=42");
    }
}