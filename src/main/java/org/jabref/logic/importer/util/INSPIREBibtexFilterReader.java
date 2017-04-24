package org.jabref.logic.importer.util;

import java.io.BufferedReader;
import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;
import java.util.regex.Pattern;

/**
 *
 * Warning -- it is not a generic filter, only read is implemented!
 *
 * Note: this is just a quick port of the original SPIRESBibtexFilterReader.
 *
 * @author Fedor Bezrukov
 * @author Sheer El-Showk
 *
 * @version $Id$
 *
 * TODO: Fix grammar in bibtex entries -- it ma return invalid bibkeys (with space)
 *
 */
public class INSPIREBibtexFilterReader extends FilterReader {

    private static final Pattern PATTERN = Pattern.compile("@Article\\{.*,");

    private final BufferedReader inReader;
    private String line;
    private int pos;

    private boolean pre;


    public INSPIREBibtexFilterReader(final Reader initialReader) {
        super(initialReader);
        inReader = new BufferedReader(initialReader);
        pos = -1;
        pre = false;
    }

    private String readpreLine() throws IOException {
        String l;
        do {
            l = inReader.readLine();
            if (l == null) {
                return null;
            }
            if (l.contains("<pre>")) {
                pre = true;
                l = inReader.readLine();
            }
            if (l == null) {
                return null;
            }
            if (l.contains("</pre>")) {
                pre = false;
            }
        } while (!pre);
        return l;
    }

    private String fixBibkey(final String preliminaryLine) {
        if (preliminaryLine == null) {
            return null;
        }
        if (PATTERN.matcher(preliminaryLine).find()) {
            return preliminaryLine.replace(' ', '_');
        } else {
            return preliminaryLine;
        }
    }

    @Override
    public int read() throws IOException {
        if (pos < 0) {
            line = fixBibkey(readpreLine());
            pos = 0;
            if (line == null) {
                return -1;
            }
        }
        if (pos >= line.length()) {
            pos = -1;
            return '\n';
        }
        return line.charAt(pos++);
    }

}
