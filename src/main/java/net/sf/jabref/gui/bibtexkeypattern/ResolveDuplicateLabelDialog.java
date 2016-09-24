package net.sf.jabref.gui.bibtexkeypattern;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.PreviewPanel;
import net.sf.jabref.gui.keyboard.KeyBinding;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Dialog box for resolving duplicate bibte keys
 */
class ResolveDuplicateLabelDialog {

    private final JDialog diag;
    private final List<JCheckBox> cbs = new ArrayList<>();
    private boolean okPressed;
    private boolean cancelPressed;


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
            b.appendRows("1dlu, p");
            b.add(cb).xy(1, row);
            PreviewPanel pp = new PreviewPanel(null, entry, null);
            pp.setPreferredSize(new Dimension(800, 90));
            b.add(new JScrollPane(pp)).xy(3, row);
            row += 2;
            cbs.add(cb);
            first = false;
        }

        ButtonBarBuilder bb = new ButtonBarBuilder();
        bb.addGlue();
        JButton ok = new JButton(Localization.lang("OK"));
        bb.addButton(ok);
        JButton ignore = new JButton(Localization.lang("Ignore"));
        bb.addButton(ignore);
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

        ignore.addActionListener(e -> diag.dispose());

        AbstractAction closeAction = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                cancelPressed = true;
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

    public boolean isCancelPressed() {
        return cancelPressed;
    }
}
