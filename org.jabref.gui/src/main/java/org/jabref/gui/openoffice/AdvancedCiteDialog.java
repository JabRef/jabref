package org.jabref.gui.openoffice;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.jabref.Globals;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.logic.l10n.Localization;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Dialog for adding citation with page number info.
 */
class AdvancedCiteDialog {

    private static boolean defaultInPar = true;
    private boolean okPressed;
    private final JDialog diag;
    private final JTextField pageInfo = new JTextField(15);


    public AdvancedCiteDialog(JabRefFrame parent) {
        diag = new JDialog(parent, Localization.lang("Cite special"), true);
        ButtonGroup bg = new ButtonGroup();
        JRadioButton inPar = new JRadioButton(Localization.lang("Cite selected entries between parenthesis"));
        JRadioButton inText = new JRadioButton(Localization.lang("Cite selected entries with in-text citation"));
        bg.add(inPar);
        bg.add(inText);
        if (defaultInPar) {
            inPar.setSelected(true);
        } else {
            inText.setSelected(true);
        }

        inPar.addChangeListener(changeEvent -> defaultInPar = inPar.isSelected());

        FormBuilder builder = FormBuilder.create()
                .layout(new FormLayout("left:pref, 4dlu, fill:pref", "pref, 4dlu, pref, 4dlu, pref"));
        builder.add(inPar).xyw(1, 1, 3);
        builder.add(inText).xyw(1, 3, 3);
        builder.add(Localization.lang("Extra information (e.g. page number)") + ":").xy(1, 5);
        builder.add(pageInfo).xy(3, 5);
        builder.padding("10dlu, 10dlu, 10dlu, 10dlu");
        diag.getContentPane().add(builder.getPanel(), BorderLayout.CENTER);

        ButtonBarBuilder bb = new ButtonBarBuilder();
        bb.addGlue();
        JButton ok = new JButton(Localization.lang("OK"));
        JButton cancel = new JButton(Localization.lang("Cancel"));
        bb.addButton(ok);
        bb.addButton(cancel);
        bb.addGlue();
        bb.padding("5dlu, 5dlu, 5dlu, 5dlu");
        diag.getContentPane().add(bb.getPanel(), BorderLayout.SOUTH);

        diag.pack();

        ActionListener okAction = actionEvent -> {
                okPressed = true;
                diag.dispose();
        };

        ok.addActionListener(okAction);
        pageInfo.addActionListener(okAction);
        inPar.addActionListener(okAction);
        inText.addActionListener(okAction);
        Action cancelAction = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                okPressed = false;
                diag.dispose();
            }
        };
        cancel.addActionListener(cancelAction);
        builder.getPanel().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(Globals.getKeyPrefs().getKey(KeyBinding.CLOSE_DIALOG), "close");
        builder.getPanel().getActionMap().put("close", cancelAction);

    }

    public void showDialog() {
        diag.setLocationRelativeTo(diag.getParent());
        diag.setVisible(true);
    }

    public boolean canceled() {
        return !okPressed;
    }

    public String getPageInfo() {
        return pageInfo.getText().trim();
    }

    public boolean isInParenthesisCite() {
        return defaultInPar;
    }
}
