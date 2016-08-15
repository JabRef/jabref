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
package net.sf.jabref.gui.help;

import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.DialogPane;

import net.sf.jabref.gui.FXAlert;
import net.sf.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.FXMLView;

public class AboutDialogView extends FXMLView {

    public AboutDialogView() {
        super();
        bundle = Localization.getMessages();
    }

    public void show() {
        FXAlert aboutDialog = new FXAlert(AlertType.INFORMATION, Localization.lang("About JabRef"), true);
        aboutDialog.setDialogPane((DialogPane) this.getView());
        aboutDialog.show();
    }

}
