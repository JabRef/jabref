package net.sf.jabref.logic.error;

import java.io.PrintStream;

import javafx.application.Platform;

import net.sf.jabref.logic.logging.LogMessages;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;

/**
 * Allows to eavesdrop on an out and an err stream.
 * <p/>
 * It can be used to listen to any messages to the System.out and System.err.
 */
public class StreamEavesdropper {

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

    /**
     * Return a new {@code PrintStream} which also creates a new log event with {@link Level#WARN} for each message and forwards it to the {@link LogMessages} archive.
     *
     * @return a PrintStream
     */
    public PrintStream getOutStream() {
        return new PrintStream(systemOut) {
            @Override
            public void write(byte[] buf, int off, int len) {
                super.write(buf, off, len);
                String message = new String(buf, off, len);
                addToLog(message, Level.WARN);
            }
        };
    }

    /**
     * Return a new {@code PrintStream} which also creates a new log event with {@link Level#ERROR} for each message and forwards it to the {@link LogMessages} archive.
     *
     * @return a PrintStream
     */
    public PrintStream getErrStream() {
        return new PrintStream(systemErr) {
            @Override
            public void write(byte[] buf, int off, int len) {
                super.write(buf, off, len);
                String message = new String(buf, off, len);
                addToLog(message, Level.ERROR);
            }
        };
    }

    /**
     * Creates a new log event with the given parameters and forwards it to the {@link LogMessages} archive.
     *
     * @param message message of log event
     * @param level   level of log event
     */
    private void addToLog(String message, Level level) {
        if (!message.equals(System.lineSeparator())) {
            String messageFormat = message.replaceAll(System.lineSeparator(), "");
            LogEvent messageWithLevel = Log4jLogEvent.newBuilder().setMessage(new SimpleMessage(messageFormat)).setLevel(level).build();
            Platform.runLater(() -> LogMessages.getInstance().add(messageWithLevel));
        }
    }

}
