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
package net.sf.jabref.gui.actions;

import net.sf.jabref.logic.error.StreamEavesdropper;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.logging.Cache;

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
    private final Cache cache;

    public ErrorConsoleAction(JFrame frame, StreamEavesdropper streamEavesdropper, Cache cache) {
        super(Localization.menuTitle("Show error console"));
        this.streamEavesdropper = streamEavesdropper;
        this.cache = cache;
        putValue(Action.SHORT_DESCRIPTION, Localization.lang("Display all error messages"));
        this.frame = frame;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        displayErrorConsole(frame);
    }

    private void displayErrorConsole(JFrame parent) {
        JTabbedPane tabbed = new JTabbedPane();

        addTextArea(tabbed, Localization.lang("Log"), cache.get());
        addTextArea(tabbed, Localization.lang("Exceptions"), streamEavesdropper.getErrorMessages(),
                Localization.lang("No exceptions have occurred."));
        addTextArea(tabbed, Localization.lang("Output"), streamEavesdropper.getOutput());

        tabbed.setPreferredSize(new Dimension(500, 500));

        JOptionPane.showMessageDialog(parent, tabbed,
                Localization.lang("Program output"), JOptionPane.ERROR_MESSAGE);
    }

    /**
     * @param tabbed  the tabbed pane to add the tab to
     * @param output  the text to display in the tab
     * @param ifEmpty Text to output if textbox is emtpy. may be null
     */
    private static void addTextArea(JTabbedPane tabbed, String title, String output, String ifEmpty) {
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
    private static void addTextArea(JTabbedPane tabbed, String title, String output) {
        addTextArea(tabbed, title, output, null);
    }
}
