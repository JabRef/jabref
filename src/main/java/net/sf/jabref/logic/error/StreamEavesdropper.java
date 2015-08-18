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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * Allows to eavesdrop on an out and an err stream.
 * <p/>
 * It can be used to listen to any messages to the System.out and System.err.
 */
public class StreamEavesdropper {

    private final ByteArrayOutputStream errByteStream = new ByteArrayOutputStream();
    private final ByteArrayOutputStream outByteStream = new ByteArrayOutputStream();

    private final PrintStream systemOut;
    private final PrintStream systemErr;


    public static StreamEavesdropper eavesdropOnSystem() {
        StreamEavesdropper streamEavesdropper = new StreamEavesdropper(System.out, System.err);
        System.setOut(streamEavesdropper.getOutStream());
        System.setErr(streamEavesdropper.getErrStream());
        return streamEavesdropper;
    }

    public StreamEavesdropper(PrintStream systemOut, PrintStream systemErr) {
        this.systemOut = systemOut;
        this.systemErr = systemErr;
    }

    public PrintStream getOutStream() {
        PrintStream consoleOut = new PrintStream(outByteStream);
        return new TeeStream(consoleOut, systemOut);
    }

    public PrintStream getErrStream() {
        PrintStream consoleErr = new PrintStream(errByteStream);
        return new TeeStream(consoleErr, systemErr);
    }

    public String getErrorMessages() {
        return errByteStream.toString();
    }

    public String getOutput() {
        return outByteStream.toString();
    }

}
