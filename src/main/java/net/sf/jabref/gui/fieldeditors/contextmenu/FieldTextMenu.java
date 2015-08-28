/*
 Copyright (C) 2004 R. Nagel

 All programs in this directory and
 subdirectories are published under the GNU General Public License as
 described below.

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or (at
 your option) any later version.

 This program is distributed in the hope that it will be useful, but
 WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 USA

 Further information about the GNU GPL is available at:
 http://www.gnu.org/copyleft/gpl.ja.html

 */

// created by : r.nagel 19.10.2004
//
// function : a popupmenu for bibtex fieldtext editors
//
//
// modified :

package net.sf.jabref.gui.fieldeditors.contextmenu;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.*;
import javax.swing.text.JTextComponent;

import net.sf.jabref.gui.ClipBoardManager;
import net.sf.jabref.gui.GUIGlobals;
import net.sf.jabref.gui.actions.PasteAction;
import net.sf.jabref.gui.fieldeditors.FieldEditor;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.strings.NameListNormalizer;

public class FieldTextMenu implements MouseListener {
    private final FieldEditor myFieldName;
    private final JPopupMenu inputMenu = new JPopupMenu();
    private final CopyAction copyAct = new CopyAction();

    public FieldTextMenu(FieldEditor fieldComponent) {
        myFieldName = fieldComponent;

        // copy/paste Menu
        inputMenu.add(new PasteAction((Component) myFieldName));
        inputMenu.add(copyAct);
        inputMenu.addSeparator();
        inputMenu.add(new ReplaceAction());

        if (myFieldName.getTextComponent() instanceof JTextComponent) {
            inputMenu.add(new CaseChangeMenu((JTextComponent) myFieldName.getTextComponent()));
        }
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
            if (myFieldName != null) {
                myFieldName.requestFocus();

                // enable/disable copy to clipboard if selected text available
                String txt = myFieldName.getSelectedText();
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

    class CopyAction extends AbstractAction {
        public CopyAction() {
            putValue(Action.NAME, Localization.lang("Copy to clipboard"));
            putValue(Action.SHORT_DESCRIPTION, Localization.lang("Copy to clipboard"));
            putValue(Action.SMALL_ICON, GUIGlobals.getImage("copy"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                if (myFieldName != null) {
                    String data = myFieldName.getSelectedText();
                    if (data != null) {
                        if (!data.isEmpty()) {
                            ClipBoardManager.clipBoard.setClipboardContents(data);
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    class ReplaceAction extends AbstractAction {
        public ReplaceAction() {
            putValue(Action.NAME, Localization.lang("Normalize to BibTeX name format"));
            putValue(Action.SHORT_DESCRIPTION, Localization.lang("If possible, normalize this list of names to conform to standard BibTeX name formatting"));
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            if (myFieldName.getText().isEmpty()) {
                return;
            }
            //myFieldName.selectAll();
            String input = myFieldName.getText();
            //myFieldName.setText(input.replaceAll(","," and"));
            myFieldName.setText(NameListNormalizer.normalizeAuthorList(input));
        }
    }
}
