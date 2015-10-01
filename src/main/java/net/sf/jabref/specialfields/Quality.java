/*  Copyright (C) 2012 JabRef contributors.
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
package net.sf.jabref.specialfields;

import java.util.ArrayList;

import javax.swing.ImageIcon;

import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.logic.l10n.Localization;

public class Quality extends SpecialField {

    private static Quality INSTANCE;


    private Quality() {
        ArrayList<SpecialFieldValue> values = new ArrayList<SpecialFieldValue>();
        // DO NOT TRANSLATE "qualityAssured" as this makes the produced .bib files non portable
        values.add(new SpecialFieldValue(this, "qualityAssured", "toggleQualityAssured", Localization.lang("Toogle quality assured"), IconTheme.getImage("qualityAssured"), Localization.lang("Toogle quality assured")));
        this.setValues(values);
        TEXT_DONE_PATTERN = "Toggled quality for %0 entries";
    }

    @Override
    public String getFieldName() {
        return SpecialFieldsUtils.FIELDNAME_QUALITY;
    }

    public static Quality getInstance() {
        if (Quality.INSTANCE == null) {
            Quality.INSTANCE = new Quality();
        }
        return Quality.INSTANCE;
    }

    @Override
    public ImageIcon getRepresentingIcon() {
        return this.getValues().get(0).getIcon();
    }

    @Override
    public String getToolTip() {
        return this.getValues().get(0).getToolTipText();
    }

    @Override
    public String getMenuString() {
        return Localization.lang("Quality");
    }

    @Override
    public boolean isSingleValueField() {
        return true;
    }

}
