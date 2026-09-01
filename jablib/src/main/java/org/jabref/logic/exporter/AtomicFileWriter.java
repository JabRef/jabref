package org.jabref.logic.exporter;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import org.jabref.logic.util.io.FileSnapshot;

import org.jspecify.annotations.Nullable;

/// Writer that similar to the built-in [java.io.FileWriter] but uses the [AtomicFileOutputStream] as the
/// underlying output stream. In this way, we make sure that the errors during the write process do not destroy the
/// contents of the target file.
/// Moreover, this writer checks if the chosen encoding supports all text that is written. Characters whose encoding
/// was problematic can be retrieved by [#getEncodingProblems()].
public class AtomicFileWriter extends OutputStreamWriter {

    private final AtomicFileOutputStream outputStream;
    private final CharsetEncoder encoder;
    private final Set<Character> problemCharacters = new TreeSet<>();

    public AtomicFileWriter(Path file, Charset encoding) throws IOException {
        this(file, encoding, false);
    }

    public AtomicFileWriter(Path file, Charset encoding, boolean keepBackup) throws IOException {
        this(file, encoding, keepBackup, null);
    }

    /// @param expectedState see [AtomicFileOutputStream#AtomicFileOutputStream(Path,boolean,FileSnapshot)]
    public AtomicFileWriter(Path file, Charset encoding, boolean keepBackup, @Nullable FileSnapshot expectedState) throws IOException {
        this(new AtomicFileOutputStream(file, keepBackup, expectedState), encoding);
    }

    private AtomicFileWriter(AtomicFileOutputStream outputStream, Charset encoding) {
        super(outputStream, encoding);
        this.outputStream = outputStream;
        encoder = encoding.newEncoder();
    }

    /// See [AtomicFileOutputStream#getCommittedTargetFileState()]; remains accessible after [#close()].
    @Nullable
    public FileSnapshot getCommittedTargetFileState() {
        return outputStream.getCommittedTargetFileState();
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

    /// Aborts the write without replacing the target file.
    public void abort() {
        outputStream.abort();
    }
}
