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
