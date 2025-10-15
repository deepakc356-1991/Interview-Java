package _09_01_streams_filereader_bufferedreader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * _02_Examples
 *
 * Topic: Character streams with FileReader and BufferedReader
 *
 * This class demonstrates:
 * 1) FileReader reading char-by-char (default platform charset)
 * 2) BufferedReader wrapping FileReader and reading line-by-line
 * 3) BufferedReader reading into a char[] buffer (chunks)
 * 4) FileReader with an explicit Charset (Java 11+)
 * 5) Files.newBufferedReader as a convenient alternative with Charset
 * 6) Using BufferedReader.lines() to get a Stream<String>
 * 7) Counting lines/words/chars with BufferedReader
 * 8) Using ready(), skip(), mark(), reset() on BufferedReader
 * 9) Quick performance comparison: unbuffered vs buffered reading
 *
 * Notes:
 * - FileReader uses the platform default charset unless you pass a Charset (Java 11+).
 *   Prefer specifying a Charset (e.g., UTF-8) to avoid mojibake.
 * - BufferedReader adds efficient buffering and the readLine() convenience method.
 * - Use try-with-resources to ensure streams are closed.
 */
public class _02_Examples {

    public static void main(String[] args) throws IOException {
        System.out.println("Running FileReader & BufferedReader examples...");

        // Create small and bigger sample files for the demos below (self-contained).
        Path sample = createSampleFile();
        Path big = createBigSampleFile();

        demoFileReaderCharByChar(sample);                 // 1
        demoBufferedReaderLineByLine(sample);             // 2
        demoBufferedReaderIntoCharBuffer(sample);         // 3
        demoFileReaderWithExplicitCharset(sample, StandardCharsets.UTF_8); // 4
        demoFilesNewBufferedReader(sample, StandardCharsets.UTF_8);        // 5
        demoLinesStream(sample);                          // 6
        demoCounts(sample);                               // 7
        demoReadySkipMarkReset(sample);                   // 8
        demoPerformanceComparison(big);                   // 9

        System.out.println("\nSample files:");
        System.out.println(" - sample: " + sample);
        System.out.println(" - big   : " + big);
    }

    // ----------------------------------------
    // Utilities
    // ----------------------------------------

    private static void printTitle(String title) {
        System.out.println("\n=== " + title + " ===");
    }

    /**
     * Creates a small UTF-8 text file with multiple lines, including non-ASCII characters.
     */
    private static Path createSampleFile() throws IOException {
        String content = String.join(System.lineSeparator(),
                "Alpha beta GAMMA",
                "Café naïve résumé",
                "Symbols & punctuation: !?.,; '\"",
                "Numbers: 123 456 7890",
                "Mixed languages: Ελληνικά عربى 日本語 한국어",
                "End"
        );
        Path file = Files.createTempFile("_fr_br_demo_", ".txt");
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    /**
     * Creates a bigger UTF-8 text file for a lightweight performance comparison.
     */
    private static Path createBigSampleFile() throws IOException {
        Path file = Files.createTempFile("_fr_br_demo_big_", ".txt");
        String line = "The quick brown fox jumps over the lazy dog — Café 日本語 — 0123456789\n";
        int lines = 20000; // ~1.4–1.8 MB depending on platform line breaks
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            for (int i = 0; i < lines; i++) {
                w.write(line);
            }
        }
        return file;
    }

    // ----------------------------------------
    // 1) FileReader char-by-char (default charset)
    // ----------------------------------------

    /**
     * Reads a file character-by-character using FileReader with the default platform charset.
     * This is simple but can be slow and may mis-decode if the file charset differs.
     */
    private static void demoFileReaderCharByChar(Path path) throws IOException {
        printTitle("1) FileReader (default charset) - read() char by char");
        try (FileReader fr = new FileReader(path.toFile())) { // default charset
            int ch;
            int count = 0;
            StringBuilder preview = new StringBuilder();
            while ((ch = fr.read()) != -1 && count < 120) { // preview only
                preview.append((char) ch);
                count++;
            }
            System.out.println("Preview first " + count + " chars (may be mis-decoded if default charset != UTF-8):");
            System.out.println(preview);
        }
    }

    // ----------------------------------------
    // 2) BufferedReader readLine()
    // ----------------------------------------

    /**
     * Wraps a FileReader in a BufferedReader and reads line-by-line.
     * This is typically the most convenient way to read text files line-wise.
     */
    private static void demoBufferedReaderLineByLine(Path path) throws IOException {
        printTitle("2) BufferedReader around FileReader - readLine()");
        try (BufferedReader br = new BufferedReader(new FileReader(path.toFile(), StandardCharsets.UTF_8))) {
            String line;
            int lineNo = 1;
            while ((line = br.readLine()) != null) {
                System.out.printf("Line %d: %s%n", lineNo++, line);
            }
        }
    }

    // ----------------------------------------
    // 3) BufferedReader read into char[] (chunks)
    // ----------------------------------------

    /**
     * Reads the whole file into a StringBuilder using BufferedReader.read(char[]).
     * More efficient than reading one char at a time.
    */
    private static void demoBufferedReaderIntoCharBuffer(Path path) throws IOException {
        printTitle("3) BufferedReader - read(char[]) in chunks");
        char[] buf = new char[32];
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(path.toFile(), StandardCharsets.UTF_8))) {
            int n;
            while ((n = br.read(buf)) != -1) {
                sb.append(buf, 0, n);
            }
        }
        System.out.println("Whole file read via chunks (trimmed preview 0..120):");
        System.out.println(sb.substring(0, Math.min(sb.length(), 120)) + (sb.length() > 120 ? " ..." : ""));
    }

    // ----------------------------------------
    // 4) FileReader with explicit Charset (Java 11+)
    // ----------------------------------------

    /**
     * Demonstrates passing an explicit Charset to FileReader (available since Java 11).
     * Prefer this or Files.newBufferedReader with StandardCharsets.UTF_8.
     */
    private static void demoFileReaderWithExplicitCharset(Path path, Charset cs) throws IOException {
        printTitle("4) FileReader with explicit Charset (" + cs + ")");
        try (FileReader fr = new FileReader(path.toFile(), cs);
             BufferedReader br = new BufferedReader(fr)) {
            System.out.println("First line: " + br.readLine());
        }
    }

    // ----------------------------------------
    // 5) Files.newBufferedReader(Path, Charset)
    // ----------------------------------------

    /**
     * Uses the NIO utility to open a BufferedReader with a specific Charset.
     * This is concise and explicit about encoding.
     */
    private static void demoFilesNewBufferedReader(Path path, Charset cs) throws IOException {
        printTitle("5) Files.newBufferedReader(Path, Charset)");
        try (BufferedReader br = Files.newBufferedReader(path, cs)) {
            List<String> firstTwo = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                String line = br.readLine();
                if (line == null) break;
                firstTwo.add(line);
            }
            System.out.println("First two lines: " + firstTwo);
        }
    }

    // ----------------------------------------
    // 6) BufferedReader.lines() -> Stream<String>
    // ----------------------------------------

    /**
     * Shows how to get a Stream<String> of lines from BufferedReader for functional processing.
     */
    private static void demoLinesStream(Path path) throws IOException {
        printTitle("6) BufferedReader.lines() to Java Stream");
        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            List<String> upper = br.lines()
                    .filter(s -> !s.isBlank())
                    .map(String::trim)
                    .map(String::toUpperCase)
                    .collect(Collectors.toList());
            System.out.println("Uppercased non-blank lines: " + upper);
        }
    }

    // ----------------------------------------
    // 7) Count lines, words, chars
    // ----------------------------------------

    /**
     * Counts lines using lines(). Then separately reads lines to approximate
     * word and character counts. Character count includes a +1 per line for newline
     * (approximation; last line may not end with a newline).
     */
    private static void demoCounts(Path path) throws IOException {
        printTitle("7) Count lines, words, chars");
        long lineCount;
        long wordCount = 0;
        long charCount = 0;

        // Count lines (consumes the stream)
        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            lineCount = br.lines().count();
        }

        // Count words and characters
        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.isEmpty()) {
                    String[] words = line.trim().split("\\s+");
                    wordCount += words.length;
                }
                charCount += line.length() + 1; // +1 for newline (approx.)
            }
        }

        System.out.printf("Lines=%d, Words≈%d, Chars≈%d%n", lineCount, wordCount, charCount);
    }

    // ----------------------------------------
    // 8) ready(), skip(), mark(), reset()
    // ----------------------------------------

    /**
     * Demonstrates BufferedReader utility methods:
     * - ready(): whether reading a char will block
     * - mark()/reset(): bookmark and return to it (requires markSupported)
     * - skip(n): skip n characters
     */
    private static void demoReadySkipMarkReset(Path path) throws IOException {
        printTitle("8) BufferedReader ready(), skip(), mark(), reset()");
        try (BufferedReader br = new BufferedReader(new FileReader(path.toFile(), StandardCharsets.UTF_8))) {
            System.out.println("markSupported: " + br.markSupported());
            System.out.println("ready (before reading): " + br.ready());

            br.mark(1024); // remember position near the start

            char[] buf = new char[10];
            int n = br.read(buf);
            System.out.println("Read first " + n + " chars: [" + new String(buf, 0, n) + "]");

            br.reset(); // go back to the mark
            long skipped = br.skip(6); // skip first 6 chars
            System.out.println("Skipped " + skipped + " chars after reset.");

            int m = br.read(buf, 0, 10);
            System.out.println("Next " + m + " chars after skip: [" + new String(buf, 0, m) + "]");
        }
    }

    // ----------------------------------------
    // 9) Performance comparison (quick)
    // ----------------------------------------

    /**
     * Very rough comparison of:
     * - FileReader.read() one char at a time (unbuffered at the reader level)
     * - BufferedReader.read(char[]) in blocks
     *
     * Exact numbers vary by machine and file size, but buffered reading is typically much faster.
     */
    private static void demoPerformanceComparison(Path path) throws IOException {
        printTitle("9) Performance: unbuffered read() vs BufferedReader read(char[])");

        // Unbuffered char-by-char (still buffered by the OS, but no Java-level buffer)
        long t1 = System.nanoTime();
        long chars1 = 0;
        try (FileReader fr = new FileReader(path.toFile(), StandardCharsets.UTF_8)) {
            int c;
            while ((c = fr.read()) != -1) chars1++;
        }
        long t2 = System.nanoTime();

        // Buffered reading into a char array
        long chars2 = 0;
        char[] buf = new char[8192];
        try (BufferedReader br = new BufferedReader(new FileReader(path.toFile(), StandardCharsets.UTF_8))) {
            int n;
            while ((n = br.read(buf)) != -1) chars2 += n;
        }
        long t3 = System.nanoTime();

        System.out.printf(Locale.ROOT,
                "Unbuffered: %,d chars in %.3f ms | Buffered: %,d chars in %.3f ms%n",
                chars1, (t2 - t1) / 1_000_000.0, chars2, (t3 - t2) / 1_000_000.0);
    }
}