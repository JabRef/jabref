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

import javax.swing.*;

import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.logic.l10n.Localization;

public class Priority extends SpecialField {

    private static Priority INSTANCE;

    private final Icon icon = IconTheme.getImage("priority");


    private Priority() {
        ArrayList<SpecialFieldValue> values = new ArrayList<SpecialFieldValue>();
        values.add(new SpecialFieldValue(this, null, "clearPriority", Localization.lang("Clear priority"), null, Localization.lang("No priority information")));
        Icon icon;
        icon = IconTheme.getImage("red");
        // DO NOT TRANSLATE "prio1" etc. as this makes the .bib files non portable
        values.add(new SpecialFieldValue(this, "prio1", "setPriority1", Localization.lang("Set priority to high"), icon, Localization.lang("Priority high")));
        icon = IconTheme.getImage("orange");
        values.add(new SpecialFieldValue(this, "prio2", "setPriority2", Localization.lang("Set priority to medium"), icon, Localization.lang("Priority medium")));
        icon = IconTheme.getImage("green");
        values.add(new SpecialFieldValue(this, "prio3", "setPriority3", Localization.lang("Set priority to low"), icon, Localization.lang("Priority low")));
        this.setValues(values);
        TEXT_DONE_PATTERN = "Set priority to '%0' for %1 entries";
    }

    public static Priority getInstance() {
        if (Priority.INSTANCE == null) {
            Priority.INSTANCE = new Priority();
        }
        return Priority.INSTANCE;
    }

    @Override
    public String getFieldName() {
        return SpecialFieldsUtils.FIELDNAME_PRIORITY;
    }

    @Override
    public Icon getRepresentingIcon() {
        return this.icon;
    }

    @Override
    public String getToolTip() {
        return Localization.lang("Priority");
    }

    @Override
    public String getMenuString() {
        return Localization.lang("Priority");
    }
}
