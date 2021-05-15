package org.jabref.logic.exporter;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

public class JabRefFileWriter extends OutputStreamWriter {

    private final CharsetEncoder encoder;
    private final Set<Character> problemCharacters = new TreeSet<>();

    public JabRefFileWriter(Path file, Charset encoding) throws IOException {
        super(Files.newOutputStream(file));
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
