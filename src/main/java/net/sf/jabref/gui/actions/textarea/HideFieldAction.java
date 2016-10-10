package net.sf.jabref.gui.actions.textarea;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.SwingUtilities;

import net.sf.jabref.JabRefGUI;
import net.sf.jabref.gui.fieldeditors.TextAreaForVisibleField;
import net.sf.jabref.logic.l10n.Localization;


public class HideFieldAction extends AbstractAction {

    private final TextAreaForVisibleField textAreaForVisibleField;


    public HideFieldAction(TextAreaForVisibleField textAreaForVisibleField) {
        this.textAreaForVisibleField = textAreaForVisibleField;
        putValue(Action.NAME, Localization.lang("Hide field"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String text = this.textAreaForVisibleField.getText();
        this.textAreaForVisibleField.setText(null);
        this.textAreaForVisibleField.getTwin().setText(text);
        this.textAreaForVisibleField.getTwin().setVisible(true);
        // by changing the focus to the text field, a storeFieldAction is called storing the new field values in both components
        this.textAreaForVisibleField.getTwin().getTextComponent().requestFocus(false);
        // if setVisible(false) is called directly after the requestFocus, the saving does not work as the field is hidden before the actual focus lost event reaches textAreaForVisibleField
        SwingUtilities.invokeLater(() -> {
            this.textAreaForVisibleField.setVisible(false);
        });

        JabRefGUI.getMainFrame().getCurrentBasePanel().markBaseChanged();
    }

}
