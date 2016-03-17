/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.importer.fetcher;

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
class INSPIREBibtexFilterReader extends FilterReader {

    private final BufferedReader inReader;

    private String line;
    private int pos;
    private boolean pre;

    private static final Pattern PATTERN = Pattern.compile("@Article\\{.*,");

    INSPIREBibtexFilterReader(final Reader initialReader) {
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
