package net.sf.jabref.logic.error;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import javafx.application.Platform;

import net.sf.jabref.logic.logging.LogMessages;

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
        PrintStream consoleOut = new PrintStream(outByteStream) {
            @Override
            public void write(byte[] buf, int off, int len) {
                super.write(buf, off, len);
                String message = new String(buf, off, len);
                addToLog(message, MessageType.OUTPUT);
            }
        };
        return new TeeStream(consoleOut, systemOut);
    }

    public TeeStream getErrStream() {
        PrintStream consoleErr = new PrintStream(errByteStream) {
            @Override
            public void write(byte[] buf, int off, int len) {
                super.write(buf, off, len);
                String message = new String(buf, off, len);
                addToLog(message, MessageType.EXCEPTION
                );
            }
        };
        return new TeeStream(consoleErr, systemErr);
    }

    private void addToLog(String s, MessageType priority) {
        if (!s.equals(System.lineSeparator())) {
            LogMessage messageWithPriority = new LogMessage(s.replaceAll(System.lineSeparator(), ""), priority);
            Platform.runLater(() -> LogMessages.getInstance().add(messageWithPriority));
        }
    }

    public String getOutput() {
        return outByteStream.toString();
    }

}
