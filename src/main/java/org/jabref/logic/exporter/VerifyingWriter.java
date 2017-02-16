package org.jabref.logic.exporter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.Set;
import java.util.TreeSet;

/**
 * Writer that extends OutputStreamWriter, but also checks if the chosen
 * encoding supports all text that is written. Currently only a boolean value is
 * stored to remember whether everything has gone well or not.
 */
public class VerifyingWriter extends OutputStreamWriter {

    private final CharsetEncoder encoder;
    private boolean couldEncodeAll = true;
    private final Set<Character> problemCharacters = new TreeSet<>();


    public VerifyingWriter(OutputStream out, Charset encoding) {
        super(out, encoding);
        encoder = encoding.newEncoder();
    }

    @Override
    public void write(String str) throws IOException {
        super.write(str);
        if (!encoder.canEncode(str)) {
            for (int i = 0; i < str.length(); i++) {
                if (!encoder.canEncode(str.charAt(i))) {
                    problemCharacters.add(str.charAt(i));
                }
            }
            couldEncodeAll = false;
        }
    }

    public boolean couldEncodeAll() {
        return couldEncodeAll;
    }

    public String getProblemCharacters() {
        StringBuilder chars = new StringBuilder();
        for (Character ch : problemCharacters) {
            chars.append(ch.charValue());
        }
        return chars.toString();
    }
}
