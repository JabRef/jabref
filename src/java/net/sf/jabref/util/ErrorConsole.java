package net.sf.jabref.util;

import net.sf.jabref.Globals;

import javax.swing.*;
import java.io.PrintStream;
import java.io.ByteArrayOutputStream;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * This class redirects the System.err stream so it goes both the way it normally
 * goes, and into a ByteArrayOutputStream. We can use this stream to display any
 * error messages and stack traces to the user. Such an error console can be
 * useful in getting complete bug reports, especially from Windows users,
 * without asking users to run JabRef in a command window to catch the error info.
 *
 * User: alver
 * Date: Mar 1, 2006
 * Time: 11:13:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class ErrorConsole {

    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
    private static ErrorConsole instance = null;


    public static ErrorConsole getInstance() {
        if (instance == null)
            instance = new ErrorConsole();

        return instance;
    }

    private ErrorConsole() {
        PrintStream myErr = new PrintStream(byteStream);
        PrintStream tee = new TeeStream(System.out, myErr);
        System.setErr(tee);
    }

    public String getErrorMessages() {
        return byteStream.toString();
    }

    public void displayErrorConsole(JFrame parent) {
        JTextArea ta = new JTextArea(getErrorMessages());
        ta.setEditable(false);
        if (ta.getText().length() == 0) {
            ta.setText(Globals.lang("No exceptions have ocurred."));
        }
        JScrollPane sp = new JScrollPane(ta);
        sp.setPreferredSize(new Dimension(500,500));
        JOptionPane.showMessageDialog(parent,  sp,
                Globals.lang("Error messages"), JOptionPane.ERROR_MESSAGE);
    }

    class ErrorConsoleAction extends AbstractAction {
        JFrame frame;
        public ErrorConsoleAction(JFrame frame) {
            super(Globals.menuTitle("Show error console"));
            putValue(SHORT_DESCRIPTION, Globals.lang("Display all error messages"));
            this.frame = frame;
        }

        public void actionPerformed(ActionEvent e) {
            displayErrorConsole(frame);
        }
    }

    public AbstractAction getAction(JFrame parent) {
        return new ErrorConsoleAction(parent);
    }

    // All writes to this print stream are copied to two print streams
    public class TeeStream extends PrintStream {
        PrintStream out;
        public TeeStream(PrintStream out1, PrintStream out2) {
            super(out1);
            this.out = out2;
        }
        public void write(byte buf[], int off, int len) {
            try {
                super.write(buf, off, len);
                out.write(buf, off, len);
            } catch (Exception e) {
            }
        }
        public void flush() {
            super.flush();
            out.flush();
        }
    }
}
