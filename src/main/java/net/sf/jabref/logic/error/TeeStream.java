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

package net.sf.jabref.logic.error;

import java.io.PrintStream;

/**
 * All writes to this print stream are copied to two print streams
 * <p/>
 * Is based on the command line tool tee
 */
public class TeeStream extends PrintStream {

    private final PrintStream outStream;

    public TeeStream(PrintStream out1, PrintStream out2) {
        super(out1);
        this.outStream = out2;
    }

    @Override
    public void write(byte[] buf, int off, int len) {
        try {
            super.write(buf, off, len);
            outStream.write(buf, off, len);
        } catch (Exception ignored) {
            // Ignored
        }
    }

    @Override
    public void flush() {
        super.flush();
        outStream.flush();
    }
}
