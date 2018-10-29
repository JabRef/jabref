package org.jabref.logic.exporter;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * Writer that similar to the built-in {@link java.io.FileWriter} but uses the {@link AtomicFileOutputStream} as the
 * underlying output stream. In this way, we make sure that the errors during the write process do not destroy the
 * contents of the target file.
 * Moreover, this writer checks if the chosen encoding supports all text that is written. Characters whose encoding
 * was problematic can be retrieved by {@link #getEncodingProblems()}.
 */
public class AtomicFileWriter extends OutputStreamWriter {

    private final CharsetEncoder encoder;
    private final Set<Character> problemCharacters = new TreeSet<>();

    public AtomicFileWriter(Path file, Charset encoding) throws IOException {
        this(file, encoding, false);
    }

    public AtomicFileWriter(Path file, Charset encoding, boolean keepBackup) throws IOException {
        super(new AtomicFileOutputStream(file, keepBackup), encoding);
        encoder = encoding.newEncoder();
    }

    @Override
    public void write(String str) throws IOException {
        super.write(str);
        if (!encoder.canEncode(str)) {
            for (int i = 0; i < str.length(); i++) {
                char character = str.charAt(i);
                if (!encoder.canEncode(character)) {
                    problemCharacters.add(character);
                }
            }
        }
    }

    public boolean hasEncodingProblems() {
        return !problemCharacters.isEmpty();
    }

    public Set<Character> getEncodingProblems() {
        return Collections.unmodifiableSet(problemCharacters);
    }
}
