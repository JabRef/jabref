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
package net.sf.jabref.gui.labelpattern;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import net.sf.jabref.*;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.PreviewPanel;
import net.sf.jabref.gui.keyboard.KeyBinding;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Dialog box for resolving duplicate bibte keys
 */
class ResolveDuplicateLabelDialog {

    private final JDialog diag;
    private final List<JCheckBox> cbs = new ArrayList<>();
    private boolean okPressed;

    private static final String LAYOUT = "<font face=\"arial\"><b><i>\\bibtextype</i><a name=\"\\bibtexkey\">\\begin{bibtexkey} (\\bibtexkey)</a>\\end{bibtexkey}</b><br>\n" +
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


    public ResolveDuplicateLabelDialog(BasePanel panel, String key, List<BibEntry> entries) {
        diag = new JDialog(panel.frame(), Localization.lang("Duplicate BibTeX key"), true);

        FormBuilder b = FormBuilder.create().layout(new FormLayout(
                "left:pref, 4dlu, fill:pref", "p"));
        b.add(new JLabel(Localization.lang("Duplicate BibTeX key") + ": " + key)).xyw(1, 1, 3);
        b.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        boolean first = true;
        int row = 3;
        for (BibEntry entry : entries) {
            JCheckBox cb = new JCheckBox(Localization.lang("Generate BibTeX key"), !first);
            //JPanel pan = new JPanel();
            //pan.setLayout(new BorderLayout());
            //pan.add(cb, BorderLayout.NORTH);
            //cb.add(new JPanel(), BorderLayout.CENTER);
            b.appendRows("1dlu, p");
            b.add(cb).xy(1, row);
            PreviewPanel pp = new PreviewPanel(null, entry, null, ResolveDuplicateLabelDialog.LAYOUT);
            pp.setPreferredSize(new Dimension(800, 90));
            //pp.setBorder(BorderFactory.createEtchedBorder());
            b.add(new JScrollPane(pp)).xy(3, row);
            row += 2;
            cbs.add(cb);
            first = false;
        }

        ButtonBarBuilder bb = new ButtonBarBuilder();
        bb.addGlue();
        JButton ok = new JButton(Localization.lang("OK"));
        bb.addButton(ok);
        JButton cancel = new JButton(Localization.lang("Cancel"));
        bb.addButton(cancel);
        bb.addGlue();
        bb.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        diag.getContentPane().add(b.getPanel(), BorderLayout.CENTER);
        diag.getContentPane().add(bb.getPanel(), BorderLayout.SOUTH);

        diag.pack();

        ok.addActionListener(e -> {
                okPressed = true;
                diag.dispose();
        });


        AbstractAction closeAction = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                diag.dispose();
            }
        };

        cancel.addActionListener(closeAction);

        ActionMap am = b.getPanel().getActionMap();
        InputMap im = b.getPanel().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(Globals.getKeyPrefs().getKey(KeyBinding.CLOSE_DIALOG), "close");
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
