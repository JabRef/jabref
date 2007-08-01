package net.sf.jabref.util;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import javax.swing.*;

import net.sf.jabref.Globals;

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
 */
public class ErrorConsole {

    ByteArrayOutputStream errByteStream = new ByteArrayOutputStream();
    ByteArrayOutputStream outByteStream = new ByteArrayOutputStream();
    private static ErrorConsole instance = null;


    public static ErrorConsole getInstance() {
        if (instance == null)
            instance = new ErrorConsole();

        return instance;
    }

    private ErrorConsole() {
        PrintStream myErr = new PrintStream(errByteStream);
        PrintStream tee = new TeeStream(System.err, myErr);
        System.setErr(tee);
        myErr = new PrintStream(outByteStream);
        tee = new TeeStream(System.out, myErr);
        System.setOut(tee);
    }

    public String getErrorMessages() {
        return errByteStream.toString();
    }

    public String getOutput() {
        return outByteStream.toString();
    }

    public void displayErrorConsole(JFrame parent) {
        JTabbedPane tabbed = new JTabbedPane();
        JTextArea ta = new JTextArea(getOutput());
        ta.setEditable(false);
        JScrollPane sp = new JScrollPane(ta);
        tabbed.addTab(Globals.lang("Output"), sp);

        ta = new JTextArea(getErrorMessages());
        ta.setEditable(false);
        if (ta.getText().length() == 0) {
            ta.setText(Globals.lang("No exceptions have ocurred."));
        }
        sp = new JScrollPane(ta);

        tabbed.addTab(Globals.lang("Exceptions"), sp);


        tabbed.setPreferredSize(new Dimension(500,500));

        JOptionPane.showMessageDialog(parent,  tabbed,
                Globals.lang("Program output"), JOptionPane.ERROR_MESSAGE);
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
