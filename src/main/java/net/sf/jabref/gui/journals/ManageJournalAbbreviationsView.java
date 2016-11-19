/*  Copyright (C) 2016 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.gui.journals;

import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.DialogPane;
import javafx.stage.Stage;

import net.sf.jabref.gui.FXDialog;
import net.sf.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.FXMLView;

/**
 * This class controls the user interface of the journal abbreviations dialog.
 * The ui elements and their layout are defined in the fxml file in the resource folder.
 */
public class ManageJournalAbbreviationsView extends FXMLView {

    public ManageJournalAbbreviationsView() {
        super();
        bundle = Localization.getMessages();
    }

    public void showAndWait() {
        FXDialog journalAbbreviationsDialog = new FXDialog(AlertType.INFORMATION,
                Localization.lang("Journal abbreviations"));
        journalAbbreviationsDialog.setResizable(true);
        journalAbbreviationsDialog.setDialogPane((DialogPane) this.getView());
        ((Stage) this.getView().getScene().getWindow()).setMinHeight(400);
        ((Stage) this.getView().getScene().getWindow()).setMinWidth(600);
        journalAbbreviationsDialog.showAndWait();
    }

}
