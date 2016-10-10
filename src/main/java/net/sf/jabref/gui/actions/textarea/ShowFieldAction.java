package net.sf.jabref.gui.actions.textarea;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.SwingUtilities;

import net.sf.jabref.JabRefGUI;
import net.sf.jabref.gui.fieldeditors.TextAreaForHiddenField;
import net.sf.jabref.logic.l10n.Localization;

public class ShowFieldAction extends AbstractAction {

    private final TextAreaForHiddenField textAreaForHiddenField;


    public ShowFieldAction(TextAreaForHiddenField textAreaForHiddenField) {
        this.textAreaForHiddenField = textAreaForHiddenField;
        putValue(Action.NAME, Localization.lang("Show Field"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String text = this.textAreaForHiddenField.getText();

        this.textAreaForHiddenField.setText(null);
        this.textAreaForHiddenField.getTwin().setText(text);
        this.textAreaForHiddenField.getTwin().setVisible(true);
        // by changing the focus to the text field, a storeFieldAction is called storing the new field values in both components
        this.textAreaForHiddenField.getTwin().getTextComponent().requestFocus(false);
        // if setVisible(false) is called directly after the requestFocus, the saving does not work as the field is hidden before the actual focus lost event reaches textAreaForVisibleField
        SwingUtilities.invokeLater(() -> {
            this.textAreaForHiddenField.setVisible(false);
        });

        JabRefGUI.getMainFrame().getCurrentBasePanel().markBaseChanged();
    }

}
