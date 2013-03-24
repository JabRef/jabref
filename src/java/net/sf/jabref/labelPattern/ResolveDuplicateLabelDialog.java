/*  Copyright (C) 2003-2011 JabRef contributors.
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
package net.sf.jabref.labelPattern;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import net.sf.jabref.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Dialog box for resolving duplicate bibte keys
 */
public class ResolveDuplicateLabelDialog {

    JDialog diag;
    JButton ok = new JButton(Globals.lang("Ok")),
        cancel = new JButton(Globals.lang("Cancel"));
    List<JCheckBox> cbs = new ArrayList<JCheckBox>();
    private boolean okPressed = false;

    final static String layout = "<font face=\"arial\"><b><i>\\bibtextype</i><a name=\"\\bibtexkey\">\\begin{bibtexkey} (\\bibtexkey)</a>\\end{bibtexkey}</b><br>\n" +
            "\\begin{author} \\format[HTMLChars,AuthorAbbreviator,AuthorAndsReplacer]{\\author}<BR>\\end{author}\n" +
            "\\begin{editor} \\format[HTMLChars,AuthorAbbreviator,AuthorAndsReplacer]{\\editor} <i>(\\format[IfPlural(Eds.,Ed.)]{\\editor})</i><BR>\\end{editor}\n" +
            "\\begin{title} \\format[HTMLChars]{\\title} \\end{title}<BR>\n" +
            "\\begin{chapter} \\format[HTMLChars]{\\chapter}<BR>\\end{chapter}\n" +
            "\\begin{journal} <em>\\format[HTMLChars]{\\journal}, </em>\\end{journal}\n" +
            "\\begin{booktitle} <em>\\format[HTMLChars]{\\booktitle}, </em>\\end{booktitle}\n" +
            "\\begin{school} <em>\\format[HTMLChars]{\\school}, </em>\\end{school}\n" +
            "\\begin{institution} <em>\\format[HTMLChars]{\\institution}, </em>\\end{institution}\n" +
            "\\begin{publisher} <em>\\format[HTMLChars]{\\publisher}, </em>\\end{publisher}\n" +
            "\\begin{year}<b>\\year</b>\\end{year}\\begin{volume}<i>, \\volume</i>\\end{volume}\\begin{pages}, \\format[FormatPagesForHTML]{\\pages} \\end{pages}\n" +
            "<p></p></font>";

    public ResolveDuplicateLabelDialog(BasePanel panel, String key,
                                       List<BibtexEntry> entries) {
        diag = new JDialog(panel.frame(), Globals.lang("Duplicate BibTeX key"), true);

        DefaultFormBuilder b = new DefaultFormBuilder(new FormLayout(
                "left:pref, 4dlu, fill:pref", ""));
        b.append(new JLabel(Globals.lang("Duplicate key")+": "+key), 3);
        b.nextLine();
        b.getPanel().setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        boolean first = true;
        for (BibtexEntry entry : entries) {
            JCheckBox cb = new JCheckBox(Globals.lang("Generate key"), !first);
            //JPanel pan = new JPanel();
            //pan.setLayout(new BorderLayout());
            //pan.add(cb, BorderLayout.NORTH);
            //cb.add(new JPanel(), BorderLayout.CENTER);
            b.append(cb);
            PreviewPanel pp = new PreviewPanel(null, entry, null , new MetaData(), layout);
            pp.setPreferredSize(new Dimension(800, 90));
            //pp.setBorder(BorderFactory.createEtchedBorder());
			b.append(new JScrollPane(pp));
            b.nextLine();
            cbs.add(cb);
            first = false;
        }


        ButtonBarBuilder bb = new ButtonBarBuilder();
        bb.addGlue();
        bb.addButton(ok);
        bb.addButton(cancel);
        bb.addGlue();
        bb.getPanel().setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        diag.getContentPane().add(b.getPanel(), BorderLayout.CENTER);
        diag.getContentPane().add(bb.getPanel(), BorderLayout.SOUTH);

        diag.pack();

        ok.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                okPressed = true;
                diag.dispose();
            }
        });
        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                diag.dispose();
            }
        });


        AbstractAction closeAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                diag.dispose();
            }
        };
        ActionMap am = b.getPanel().getActionMap();
        InputMap im = b.getPanel().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(Globals.prefs.getKey("Close dialog"), "close");
        am.put("close", closeAction);
    }

/**
     * After the dialog has been closed, this query answers whether the dialog was okPressed
     * (by cancel button or by closing the dialog directly).
     * @return true if it was okPressed, false if Ok was pressed.
     */
    public boolean isOkPressed() {
        return okPressed;
    }

    /**
     * Get the list of checkboxes where the user has selected which entries to generate
     * new keys for.
     * @return the list of checkboxes
     */
    public List<JCheckBox> getCheckBoxes() {
        return cbs;
    }

    public void show() {
        okPressed = false;
        diag.setLocationRelativeTo(diag.getParent());
        diag.setVisible(true);
    }
}
