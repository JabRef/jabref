/*
 * Copyright (C) 2003-2016 JabRef contributors.
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jabref.gui.documentviewer;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.Icon;

import javafx.application.Platform;

import org.jabref.gui.IconTheme;
import org.jabref.gui.actions.MnemonicAwareAction;
import org.jabref.logic.l10n.Localization;


public class ShowDocumentViewerAction extends MnemonicAwareAction {

    public ShowDocumentViewerAction(String title, String tooltip, Icon iconFile) {
        super(iconFile);
        putValue(Action.NAME, title);
        putValue(Action.SHORT_DESCRIPTION, tooltip);
    }

    public ShowDocumentViewerAction() {
        this(Localization.menuTitle("Show document viewer"), Localization.lang("Show document viewer"),
                IconTheme.JabRefIcon.PDF_FILE.getIcon());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Platform.runLater(() -> new DocumentViewerView().show());
    }

}
