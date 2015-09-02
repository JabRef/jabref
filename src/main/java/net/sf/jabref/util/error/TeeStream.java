package net.sf.jabref.util.error;

import java.io.PrintStream;

/**
 * All writes to this print stream are copied to two print streams
 * <p/>
 * Is based on the command line tool tee
 */
public class TeeStream extends PrintStream {

    private final PrintStream out;

    public TeeStream(PrintStream out1, PrintStream out2) {
        super(out1);
        this.out = out2;
    }

    @Override
    public void write(byte[] buf, int off, int len) {
        try {
            super.write(buf, off, len);
            out.write(buf, off, len);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void flush() {
        super.flush();
        out.flush();
    }
}
