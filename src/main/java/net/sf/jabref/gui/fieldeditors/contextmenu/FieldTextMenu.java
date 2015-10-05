package net.sf.jabref.gui.fieldeditors.contextmenu;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.*;
import javax.swing.text.JTextComponent;

import net.sf.jabref.gui.actions.CopyAction;
import net.sf.jabref.gui.actions.PasteAction;
import net.sf.jabref.gui.fieldeditors.FieldEditor;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.strings.NameListNormalizer;

public class FieldTextMenu implements MouseListener {
    private final FieldEditor field;
    private final JPopupMenu inputMenu = new JPopupMenu();
    private final CopyAction copyAct;

    public FieldTextMenu(FieldEditor fieldComponent) {
        field = fieldComponent;
        copyAct = new CopyAction((JTextComponent) field);
        initMenu();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
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
        if (e.isPopupTrigger()) {
            if (field != null) {
                field.requestFocus();

                // enable/disable copy to clipboard if selected text available
                String txt = field.getSelectedText();
                boolean cStat = false;
                if (txt != null) {
                    if (!txt.isEmpty()) {
                        cStat = true;
                    }
                }
                copyAct.setEnabled(cStat);
                inputMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }

    private void initMenu() {
        inputMenu.add(new PasteAction((Component) field));
        inputMenu.add(copyAct);
        inputMenu.addSeparator();
        inputMenu.add(new ReplaceAction());

        if (field.getTextComponent() instanceof JTextComponent) {
            inputMenu.add(new CaseChangeMenu((JTextComponent) field.getTextComponent()));
            inputMenu.add(new ConversionMenu((JTextComponent) field.getTextComponent()));
        }
    }

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
            field.setText(NameListNormalizer.normalizeAuthorList(input));
        }
    }
}
