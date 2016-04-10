package net.sf.jabref.gui.fieldeditors.contextmenu;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.*;
import javax.swing.text.JTextComponent;

import net.sf.jabref.gui.ClipBoardManager;
import net.sf.jabref.gui.actions.CopyAction;
import net.sf.jabref.gui.actions.PasteAction;
import net.sf.jabref.gui.fieldeditors.FieldEditor;
import net.sf.jabref.logic.formatter.bibtexfields.NormalizeNamesFormatter;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.strings.StringUtil;

public class FieldTextMenu implements MouseListener {
    private final FieldEditor field;
    private final JPopupMenu inputMenu = new JPopupMenu();
    private final CopyAction copyAction;
    private final PasteAction pasteAction;

    private static final int MAX_PASTE_PREVIEW_LENGTH = 20;


    public FieldTextMenu(FieldEditor fieldComponent) {
        field = fieldComponent;
        copyAction = new CopyAction((JTextComponent) field);
        pasteAction = new PasteAction((JTextComponent) field);
        initMenu();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        // Do nothing
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // Do nothing
    }

    @Override
    public void mouseExited(MouseEvent e) {
        // Do nothing
    }

    @Override
    public void mousePressed(MouseEvent e) {
        maybeShowPopup(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        maybeShowPopup(e);
    }

    private void maybeShowPopup(MouseEvent e) {
        if (e.isPopupTrigger() && (field != null)) {
            field.requestFocus();

            // enable/disable copy to clipboard if selected text available
            String txt = field.getSelectedText();
            String allTxt = field.getText();
            boolean copyStatus = false;
            if (((txt != null) && (!txt.isEmpty())) || ((allTxt != null) && !allTxt.isEmpty())) {
                copyStatus = true;
            }

            copyAction.setEnabled(copyStatus);

            String data = ClipBoardManager.CLIPBOARD.getClipboardContents();
            boolean pasteStatus = false;
            if (!data.isEmpty()) {
                pasteStatus = true;
                pasteAction.putValue(Action.SHORT_DESCRIPTION, Localization.lang("Paste from clipboard") + ": "
                        + StringUtil.limitStringLength(data, MAX_PASTE_PREVIEW_LENGTH));
            } else {
                pasteAction.putValue(Action.SHORT_DESCRIPTION, Localization.lang("Paste from clipboard"));
            }
            pasteAction.setEnabled(pasteStatus);
            inputMenu.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    private void initMenu() {
        inputMenu.add(pasteAction);
        inputMenu.add(copyAction);
        inputMenu.addSeparator();
        inputMenu.add(new ReplaceAction());

        if (field.getTextComponent() instanceof JTextComponent) {
            inputMenu.add(new CaseChangeMenu((JTextComponent) field.getTextComponent()));
            inputMenu.add(new ConversionMenu((JTextComponent) field.getTextComponent()));
        }
    }

    @SuppressWarnings("serial")
    class ReplaceAction extends AbstractAction {
        public ReplaceAction() {
            putValue(Action.NAME, Localization.lang("Normalize to BibTeX name format"));
            putValue(Action.SHORT_DESCRIPTION, Localization.lang("If possible, normalize this list of names to conform to standard BibTeX name formatting"));
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            if (field.getText().isEmpty()) {
                return;
            }
            String input = field.getText();
            field.setText(new NormalizeNamesFormatter().format(input));
        }
    }
}
