package net.sf.jabref.gui.actions;

import net.sf.jabref.gui.ClipBoardManager;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.logic.l10n.Localization;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.event.ActionEvent;

public class CopyAction extends AbstractAction {
    private final JTextComponent field;

    public CopyAction(JTextComponent field) {
        this.field = field;

        putValue(Action.NAME, Localization.lang("Copy to clipboard"));
        putValue(Action.SHORT_DESCRIPTION, Localization.lang("Copy to clipboard"));
        putValue(Action.SMALL_ICON, IconTheme.JabRefIcon.COPY.getSmallIcon());
        putValue(Action.LARGE_ICON_KEY, IconTheme.JabRefIcon.COPY.getIcon());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (field != null) {
            String selectedText = field.getSelectedText();
            String allText = field.getText();
            if ((selectedText != null) && !selectedText.isEmpty()) {
                ClipBoardManager.CLIPBOARD.setClipboardContents(selectedText);
            } else if ((allText != null) && !allText.isEmpty()) {
                ClipBoardManager.CLIPBOARD.setClipboardContents(allText);
            }
        }
    }
}
