package net.sf.jabref.gui.actions;

import net.sf.jabref.gui.ClipBoardManager;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.net.URLUtil;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;

public class PasteAction extends AbstractAction {
    private final Component target;

    public PasteAction(Component target) {
        this.target = target;

        putValue(Action.NAME, Localization.lang("Paste from clipboard"));
        putValue(Action.SHORT_DESCRIPTION, Localization.lang("Paste from clipboard"));
        putValue(Action.SMALL_ICON, IconTheme.JabRefIcon.PASTE.getSmallIcon());
        putValue(Action.LARGE_ICON_KEY, IconTheme.JabRefIcon.PASTE.getIcon());
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
        String data = ClipBoardManager.CLIPBOARD.getClipboardContents();

        if (data.isEmpty()) {
            return;
        }
        // auto corrections
        // clean Google search URLs
        data = URLUtil.cleanGoogleSearchURL(data);

        // caller
        if(target instanceof JTextComponent) {
            JTextComponent textField = (JTextComponent) target;
            // replace text selection
            textField.replaceSelection(data);
        }
    }
}