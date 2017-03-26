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

import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.DialogPane;

import org.jabref.gui.AbstractDialogView;
import org.jabref.gui.FXDialog;
import org.jabref.logic.l10n.Localization;

public class DocumentViewerView extends AbstractDialogView {

    @Override
    public void show() {
        FXDialog dialog = new FXDialog(AlertType.INFORMATION, Localization.lang("Document viewer"), false);
        DialogPane dialogPane = (DialogPane) this.getView();

        // Remove button bar at bottom
        dialogPane.getChildren().removeIf(node -> node instanceof ButtonBar);

        dialog.setDialogPane(dialogPane);
        dialog.setResizable(true);
        dialog.show();
    }
}
