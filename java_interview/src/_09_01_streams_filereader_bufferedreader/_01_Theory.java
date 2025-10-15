package _09_01_streams_filereader_bufferedreader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/*
Topic: Java I/O character streams focusing on FileReader and BufferedReader

Core concepts:
- Byte vs character streams:
  - Byte streams (InputStream/OutputStream) move raw bytes.
  - Character streams (Reader/Writer) decode/encode text (chars) using a Charset.
  - FileReader and BufferedReader are character-stream classes.

- FileReader:
  - Convenience Reader for reading characters from a file using the platform default charset.
  - Internally wraps a FileInputStream + InputStreamReader(defaultCharset).
  - Pitfall: default charset is platform-dependent; text may decode incorrectly if the file’s encoding differs.
  - Prefer explicitly specifying a Charset via InputStreamReader or Files.newBufferedReader.

- BufferedReader:
  - Wraps another Reader and buffers chars to reduce disk/OS calls, improving performance.
  - Adds readLine() (returns a line without its line terminator) and lines() (Stream<String> of lines).
  - Supports mark/reset with a char read-ahead limit.
  - Buffer size defaults to 8192 chars; can be customized.

- Encoding and Charset:
  - Always know the file’s encoding. Common: UTF-8.
  - Using the wrong charset yields mojibake (garbled text).
  - InputStreamReader with a specific Charset is the safe choice.
  - By default, decoding errors are replaced with the replacement char '�' (U+FFFD) rather than throwing.

- End-of-stream:
  - Reader.read() returns -1; read(char[]) returns -1; readLine() returns null at EOF.

- Newlines:
  - readLine() recognizes \n (LF), \r (CR), \r\n (CRLF); returns the line without the terminator.
  - The last line without a trailing newline is still returned; null indicates no more lines.

- Blocking and ready():
  - ready() tells if a read won’t block for some data. Not a reliable EOF check; avoid using it as such.

- mark/reset:
  - BufferedReader supports mark/reset; readAheadLimit bounds how far you can read before losing the mark.

- Resources and closing:
  - Always close streams/readers (use try-with-resources). Closing a BufferedReader closes its underlying Reader.
  - Readers don’t have flush(); flushing is for Writers.

- Thread-safety:
  - Readers are not intended for concurrent reading from multiple threads. Synchronize externally if needed.

- Performance best practices:
  - Prefer BufferedReader or read into char[] rather than reading char-by-char.
  - Customize buffer size for very large files or unusual access patterns.
  - For simple line processing, use Files.newBufferedReader(path, charset) or Files.lines(path, charset).

- Alternatives:
  - Files.newBufferedReader(Path, Charset): returns a BufferedReader with explicit charset.
  - Files.lines(Path, Charset): returns a lazily read Stream<String> (must be closed).
  - Avoid FileReader if you need explicit encoding control.

- BOM (Byte Order Mark):
  - UTF-8 BOM (U+FEFF) may appear as the first char if present; Java does not automatically strip it.
  - If necessary, detect and strip '\uFEFF' manually on the first char/line.

- Characters vs code points:
  - Java char is a UTF-16 code unit; some characters (e.g., emoji) use surrogate pairs. Counting chars != counting user-visible characters.

This class demonstrates common patterns and caveats with FileReader and BufferedReader.
*/
public class _01_Theory {

    public static void main(String[] args) throws IOException {
        Path sample = createSampleFile();
        hr("Sample file created at: " + sample.toAbsolutePath());

        hr("1) FileReader (default platform charset) – generally avoid for non-trivial apps");
        demoFileReaderDefaultCharset(sample);

        hr("2) InputStreamReader with explicit UTF-8 (preferred)");
        demoInputStreamReaderWithCharset(sample, StandardCharsets.UTF_8);

        hr("3) BufferedReader readLine() with explicit UTF-8");
        demoBufferedReaderReadLine(sample, StandardCharsets.UTF_8);

        hr("4) BufferedReader with custom buffer size and char[] loop");
        demoBufferedReaderCharArray(sample, StandardCharsets.UTF_8, 64 * 1024);

        hr("5) mark/reset, skip, ready() behavior");
        demoMarkResetSkipReady(sample, StandardCharsets.UTF_8);

        hr("6) Files.newBufferedReader (idiomatic) and Stream<String> lines()");
        demoFilesNewBufferedReaderAndLines(sample, StandardCharsets.UTF_8);

        hr("7) Optional: manually strip UTF-8 BOM if present");
        demoStripUtf8BomIfPresent(sample, StandardCharsets.UTF_8);

        hr("Done");
    }

    /*
    Creates a small UTF-8 text file with mixed line endings and Unicode content.
    - Includes:
      - LF and CRLF to show readLine() normalization.
      - Accented characters and emoji to demonstrate Unicode.
    */
    private static Path createSampleFile() throws IOException {
        String content =
                "alpha\n" +                    // LF
                "beta\r\n" +                   // CRLF
                "gamma delta 😊\n" +
                "naïve café 你好\n";
        Path file = Files.createTempFile("_streams_demo_", ".txt");
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
        file.toFile().deleteOnExit();
        return file;
    }

    /*
    Demonstrates FileReader:
    - Uses the platform default charset implicitly (risky).
    - Suitable only if you are certain the platform default equals the file’s encoding.
    - Pitfall: On different OS/locales, default charset differs; Unicode text may break.
    */
    private static void demoFileReaderDefaultCharset(Path path) {
        File file = path.toFile();
        try (FileReader fr = new FileReader(file)) {
            StringBuilder sb = new StringBuilder();
            int ch;
            // Reading char-by-char is simple but slow for large files; use buffering in real code.
            while ((ch = fr.read()) != -1) {
                sb.append((char) ch);
            }
            System.out.println("Read with FileReader (default charset):");
            System.out.println(limit(sb.toString(), 120));
        } catch (IOException e) {
            System.out.println("Error reading with FileReader: " + e.getMessage());
        }
    }

    /*
    Demonstrates InputStreamReader with explicit Charset:
    - Prefer this (or Files.newBufferedReader) over FileReader to control encoding.
    - Reads into a char[] buffer for efficiency.
    */
    private static void demoInputStreamReaderWithCharset(Path path, Charset cs) {
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(path.toFile()), cs)) {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[8192];
            int n;
            while ((n = reader.read(buf)) != -1) {
                sb.append(buf, 0, n);
            }
            System.out.println("Read with InputStreamReader(" + cs.name() + "):");
            System.out.println(limit(sb.toString(), 120));
        } catch (IOException e) {
            System.out.println("Error reading with InputStreamReader: " + e.getMessage());
        }
    }

    /*
    Demonstrates BufferedReader.readLine():
    - Removes line terminators (\n, \r, \r\n).
    - Returns null at end-of-stream.
    - Uses explicit UTF-8 to avoid default charset pitfalls.
    */
    private static void demoBufferedReaderReadLine(Path path, Charset cs) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path.toFile()), cs))) {
            String line;
            int lineNo = 1;
            while ((line = br.readLine()) != null) {
                System.out.printf("%02d: %s%n", lineNo++, line);
            }
        } catch (IOException e) {
            System.out.println("Error with BufferedReader.readLine(): " + e.getMessage());
        }
    }

    /*
    Demonstrates:
    - Custom buffer size for BufferedReader.
    - Reading into a char[] in a loop (efficient for large files).
    - Note: A BufferedReader buffers chars (not bytes); InputStreamReader handles decoding.
    */
    private static void demoBufferedReaderCharArray(Path path, Charset cs, int bufferSize) {
        Reader base = new InputStreamReader(safeOpenFis(path), cs);
        if (base == null) return;
        try (BufferedReader br = new BufferedReader(base, bufferSize)) {
            char[] buf = new char[4096];
            StringBuilder sb = new StringBuilder();
            int n;
            while ((n = br.read(buf)) != -1) {
                sb.append(buf, 0, n);
            }
            System.out.println("BufferedReader with custom buffer (" + bufferSize + "):");
            System.out.println(limit(sb.toString(), 120));
        } catch (IOException e) {
            System.out.println("Error in demoBufferedReaderCharArray: " + e.getMessage());
        }
    }

    /*
    Demonstrates:
    - mark/reset: allows peeking ahead within readAheadLimit.
    - skip: skips a number of chars (cannot be negative).
    - ready(): indicates a non-blocking read is possible (not an EOF check).
    */
    private static void demoMarkResetSkipReady(Path path, Charset cs) {
        try (BufferedReader br = Files.newBufferedReader(path, cs)) {
            System.out.println("markSupported: " + br.markSupported());

            // Mark with a small read-ahead; if you read more than this before reset, the mark is invalidated.
            br.mark(16);

            char[] first = new char[5];
            int n1 = br.read(first);
            System.out.println("First 5 chars: " + new String(first, 0, Math.max(0, n1)));

            br.reset(); // Back to the mark
            char[] again = new char[5];
            int n2 = br.read(again);
            System.out.println("After reset, first 5 again: " + new String(again, 0, Math.max(0, n2)));

            // Skip a few characters (e.g., skip 'alpha' and newline)
            long skipped = br.skip(6); // 'alpha' + '\n'
            System.out.println("Skipped chars: " + skipped);

            System.out.println("ready(): " + br.ready());

            // Read a line after skipping
            String nextLine = br.readLine();
            System.out.println("Line after skip: " + nextLine);
        } catch (IOException e) {
            System.out.println("Error in demoMarkResetSkipReady: " + e.getMessage());
        }
    }

    /*
    Demonstrates:
    - Files.newBufferedReader(Path, Charset): concise, explicit charset, already buffered.
    - lines(): returns a Stream<String> of lines; must be closed or used inside try-with-resources.
    - Note: lines() excludes line terminators.
    */
    private static void demoFilesNewBufferedReaderAndLines(Path path, Charset cs) {
        try (BufferedReader br = Files.newBufferedReader(path, cs)) {
            System.out.println("First two lines using readLine():");
            System.out.println("1: " + br.readLine());
            System.out.println("2: " + br.readLine());
        } catch (IOException e) {
            System.out.println("Error with Files.newBufferedReader: " + e.getMessage());
        }

        try (Stream<String> lines = Files.lines(path, cs)) {
            long countWithA = lines.filter(s -> s.contains("a")).count();
            System.out.println("Lines containing 'a': " + countWithA);
        } catch (IOException e) {
            System.out.println("Error with Files.lines: " + e.getMessage());
        }
    }

    /*
    Demonstrates BOM handling:
    - If a UTF-8 file begins with a BOM, Java will not strip it automatically.
    - If the first char is '\uFEFF', remove it manually.
    - This demo just shows the check; the sample file has no BOM by default.
    */
    private static void demoStripUtf8BomIfPresent(Path path, Charset cs) {
        try (BufferedReader br = Files.newBufferedReader(path, cs)) {
            br.mark(1);
            int c = br.read();
            if (c != -1 && c == '\uFEFF') {
                System.out.println("Detected UTF-8 BOM; stripping it.");
            } else {
                br.reset();
            }
            String firstLine = br.readLine();
            System.out.println("First line (after BOM handling if any): " + firstLine);
        } catch (IOException e) {
            System.out.println("Error in BOM demo: " + e.getMessage());
        }
    }

    // Utility: open FileInputStream safely (returns null if error; caller must handle)
    private static FileInputStream safeOpenFis(Path path) {
        try {
            return new FileInputStream(path.toFile());
        } catch (IOException e) {
            System.out.println("Failed to open FileInputStream: " + e.getMessage());
            return null;
        }
    }

    // Utility: print section header
    private static void hr(String title) {
        System.out.println();
        System.out.println("== " + title);
    }

    // Utility: limit printed text length for brevity
    private static String limit(String s, int max) {
        if (s == null) return null;
        if (s.length() <= max) return s;
        return s.substring(0, max) + " ...";
    }
}