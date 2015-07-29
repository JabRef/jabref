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

import net.sf.jabref.GUIGlobals;
import net.sf.jabref.Globals;

public class Printed extends SpecialField {

    private static Printed INSTANCE;


    private Printed() {
        ArrayList<SpecialFieldValue> values = new ArrayList<SpecialFieldValue>();
        // DO NOT TRANSLATE "printed" as this makes the produced .bib files non portable
        values.add(new SpecialFieldValue(this, "printed", "togglePrinted", Globals.lang("Toogle print status"), GUIGlobals.getImage("printed"), Globals.lang("Toogle print status")));
        this.setValues(values);
        TEXT_DONE_PATTERN = "Toggled print status for %0 entries";
    }

    @Override
    public String getFieldName() {
        return SpecialFieldsUtils.FIELDNAME_PRINTED;
    }

    public static Printed getInstance() {
        if (Printed.INSTANCE == null) {
            Printed.INSTANCE = new Printed();
        }
        return Printed.INSTANCE;
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
        return Globals.lang("Printed");
    }

    @Override
    public boolean isSingleValueField() {
        return true;
    }

}
