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

package net.sf.jabref.gui.pdfviewer;

import javafx.scene.Scene;
import javafx.scene.control.Alert.AlertType;

import net.sf.jabref.gui.FXAlert;
import net.sf.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.FXMLView;

public class PdfViewerView extends FXMLView {

    public PdfViewerView() {
        super();
        bundle = Localization.getMessages();
    }

    public void show() {
        FXAlert dialog = new FXAlert(AlertType.INFORMATION, Localization.lang("PDF Preview"));
        //dialog.setDialogPane((DialogPane) this.getView());
        dialog.setScene(new Scene(this.getView()));
        //dialog.getDialogPane().setContent(this.getView());
        dialog.setResizable(true);
        dialog.show();
    }
}
