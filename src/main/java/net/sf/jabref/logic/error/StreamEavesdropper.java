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


    public StreamEavesdropper(PrintStream systemOut, PrintStream systemErr) {
        this.systemOut = systemOut;
        this.systemErr = systemErr;
    }

    public static StreamEavesdropper eavesdropOnSystem() {
        StreamEavesdropper streamEavesdropper = new StreamEavesdropper(System.out, System.err);
        System.setOut(streamEavesdropper.getOutStream());
        System.setErr(streamEavesdropper.getErrStream());
        return streamEavesdropper;
    }

    public TeeStream getOutStream() {
        PrintStream consoleOut = new PrintStream(outByteStream);
        return new TeeStream(consoleOut, systemOut, MessagePriority.MEDIUM);
    }

    public TeeStream getErrStream() {
        PrintStream consoleErr = new PrintStream(errByteStream);
        return new TeeStream(consoleErr, systemErr, MessagePriority.HIGH);
    }

    public String getOutput() {
        return outByteStream.toString();
    }

}
