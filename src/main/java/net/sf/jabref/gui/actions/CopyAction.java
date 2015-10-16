package net.sf.jabref.gui.actions;

import net.sf.jabref.gui.ClipBoardManager;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.logic.l10n.Localization;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.event.ActionEvent;

public class CopyAction extends AbstractAction {
    private JTextComponent field;

    public CopyAction(JTextComponent field) {
        this.field = field;

        putValue(Action.NAME, Localization.lang("Copy to clipboard"));
        putValue(Action.SHORT_DESCRIPTION, Localization.lang("Copy to clipboard"));
        putValue(Action.SMALL_ICON, IconTheme.JabRefIcon.COPY.getIcon());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (field != null) {
            String data = field.getSelectedText();
            if (data != null) {
                if (!data.isEmpty()) {
                    ClipBoardManager.clipBoard.setClipboardContents(data);
                }
            }
        }
    }
}
