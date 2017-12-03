package org.jabref.gui.bibtexkeypattern;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.JabRefDialog;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.keyboard.KeyBinder;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.bibtexkeypattern.AbstractBibtexKeyPattern;
import org.jabref.model.metadata.MetaData;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;

public class BibtexKeyPatternDialog extends JabRefDialog {

    private MetaData metaData;
    private BasePanel panel;
    private final BibtexKeyPatternPanel bibtexKeyPatternPanel;


    public BibtexKeyPatternDialog(JabRefFrame parent, BasePanel panel) {
        super((JFrame) null, Localization.lang("BibTeX key patterns"), true, BibtexKeyPatternDialog.class);
        this.bibtexKeyPatternPanel = new BibtexKeyPatternPanel(panel);
        setPanel(panel);
        init();
    }

    /**
     * Used for updating an existing Dialog
     *
     * @param panel the panel to read the data from
     */
    public void setPanel(BasePanel panel) {
        this.panel = panel;
        this.metaData = panel.getBibDatabaseContext().getMetaData();
        AbstractBibtexKeyPattern keypatterns = metaData.getCiteKeyPattern(Globals.prefs.getKeyPattern());
        bibtexKeyPatternPanel.setValues(keypatterns);
    }

    private void init() {
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(bibtexKeyPatternPanel, BorderLayout.CENTER);

        JButton ok = new JButton(Localization.lang("OK"));
        JButton cancel = new JButton(); // label of "cancel" is set later as the label is overwritten by assigning an action to the button

        JPanel lower = new JPanel();
        lower.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        ButtonBarBuilder bb = new ButtonBarBuilder(lower);
        bb.addGlue();
        bb.addButton(ok);
        bb.addButton(cancel);
        bb.addGlue();

        getContentPane().add(lower, BorderLayout.SOUTH);

        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        getContentPane().setPreferredSize(new Dimension(500, 600));
        pack();

        ok.addActionListener(e -> {
            metaData.setCiteKeyPattern(bibtexKeyPatternPanel.getKeyPatternAsDatabaseBibtexKeyPattern());
            panel.markNonUndoableBaseChanged();
            dispose();
        });

        final JDialog dialog = this;

        Action cancelAction = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.dispatchEvent(new WindowEvent(dialog, WindowEvent.WINDOW_CLOSING));
            }
        };
        cancel.setAction(cancelAction);
        cancel.setText(Localization.lang("Cancel"));

        KeyBinder.bindCloseDialogKeyToCancelAction(this.getRootPane(), cancelAction);
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            super.setVisible(visible);
        }
    }

}
