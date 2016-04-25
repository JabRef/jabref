/*  Copyright (C) 2012-2015 JabRef contributors.
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
import java.util.List;

import javax.swing.Icon;

import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.logic.l10n.Localization;

public class Priority extends SpecialField {

    private static Priority INSTANCE;

    private final Icon icon = IconTheme.JabRefIcon.PRIORITY.getSmallIcon();


    private Priority() {
        List<SpecialFieldValue> values = new ArrayList<>();
        values.add(new SpecialFieldValue(this, null, "clearPriority", Localization.lang("Clear priority"), null,
                Localization.lang("No priority information")));
        Icon tmpicon;
        tmpicon = IconTheme.JabRefIcon.PRIORITY_HIGH.getSmallIcon();
        // DO NOT TRANSLATE "prio1" etc. as this makes the .bib files non portable
        values.add(new SpecialFieldValue(this, "prio1", "setPriority1", Localization.lang("Set priority to high"),
                tmpicon, Localization.lang("Priority high")));
        tmpicon = IconTheme.JabRefIcon.PRIORITY_MEDIUM.getSmallIcon();
        values.add(new SpecialFieldValue(this, "prio2", "setPriority2", Localization.lang("Set priority to medium"),
                tmpicon, Localization.lang("Priority medium")));
        tmpicon = IconTheme.JabRefIcon.PRIORITY_LOW.getSmallIcon();
        values.add(new SpecialFieldValue(this, "prio3", "setPriority3", Localization.lang("Set priority to low"),
                tmpicon, Localization.lang("Priority low")));
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
