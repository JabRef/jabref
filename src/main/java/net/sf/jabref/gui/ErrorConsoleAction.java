package net.sf.jabref.gui;

import net.sf.jabref.Globals;
import net.sf.jabref.util.error.StreamEavesdropper;
import net.sf.jabref.util.logging.CachebleHandler;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import java.awt.Dimension;
import java.awt.event.ActionEvent;

/**
 * Such an error console can be
 * useful in getting complete bug reports, especially from Windows users,
 * without asking users to run JabRef in a command window to catch the error info.
 * <p/>
 * It offers a separate tab for the log output.
 */
public class ErrorConsoleAction extends AbstractAction {

    private final JFrame frame;
    private final StreamEavesdropper streamEavesdropper;
    private final CachebleHandler logHandler;

    public ErrorConsoleAction(JFrame frame, StreamEavesdropper streamEavesdropper, CachebleHandler logHandler) {
        super(Globals.menuTitle("Show error console"));
        this.streamEavesdropper = streamEavesdropper;
        this.logHandler = logHandler;
        putValue(Action.SHORT_DESCRIPTION, Globals.lang("Display all error messages"));
        this.frame = frame;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        displayErrorConsole(frame);
    }

    private void displayErrorConsole(JFrame parent) {
        JTabbedPane tabbed = new JTabbedPane();

        addTextArea(tabbed, Globals.lang("Output"), streamEavesdropper.getOutput());
        addTextArea(tabbed, Globals.lang("Exceptions"), streamEavesdropper.getErrorMessages(),
                Globals.lang("No exceptions have ocurred."));
        addTextArea(tabbed, Globals.lang("Log"), logHandler.getLog());

        tabbed.setPreferredSize(new Dimension(500, 500));

        JOptionPane.showMessageDialog(parent, tabbed,
                Globals.lang("Program output"), JOptionPane.ERROR_MESSAGE);
    }

    /**
     * @param tabbed  the tabbed pane to add the tab to
     * @param output  the text to display in the tab
     * @param ifEmpty Text to output if textbox is emtpy. may be null
     */
    private void addTextArea(JTabbedPane tabbed, String title, String output, String ifEmpty) {
        JTextArea ta = new JTextArea(output);
        ta.setEditable(false);
        if ((ifEmpty != null) && (ta.getText().isEmpty())) {
            ta.setText(ifEmpty);
        }
        JScrollPane sp = new JScrollPane(ta);
        tabbed.addTab(title, sp);
    }

    /**
     * @param tabbed the tabbed pane to add the tab to
     * @param output the text to display in the tab
     */
    private void addTextArea(JTabbedPane tabbed, String title, String output) {
        addTextArea(tabbed, title, output, null);
    }
}
