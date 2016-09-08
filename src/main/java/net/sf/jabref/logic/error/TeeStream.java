package net.sf.jabref.logic.error;

import java.io.PrintStream;

import net.sf.jabref.logic.logging.LogMessages;

/**
 * All writes to this print stream are copied to two print streams
 * <p/>
 * Is based on the command line tool tee
 */
public class TeeStream extends PrintStream {

    private final PrintStream outStream;
    private final MessageType priority;

    public TeeStream(PrintStream out1, PrintStream out2, MessageType priority) {
        super(out1);
        this.outStream = out2;
        this.priority = priority;
    }

    @Override
    public void write(byte[] buf, int off, int len) {
        try {
            super.write(buf, off, len);
            outStream.write(buf, off, len);
            String s = new String(buf, off, len);
            if (!s.equals(System.lineSeparator())) {
                LogMessage messageWithPriority = new LogMessage(s.replaceAll(System.lineSeparator(), ""), priority);
                LogMessages.getInstance().add(messageWithPriority);
            }
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
