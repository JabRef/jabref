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

import javax.swing.*;

import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.logic.l10n.Localization;

public class ReadStatus extends SpecialField {

    private static ReadStatus INSTANCE;

    private final Icon icon = IconTheme.JabRefIcon.READ_STATUS.getSmallIcon();


    private ReadStatus() {
        ArrayList<SpecialFieldValue> values = new ArrayList<>();
        values.add(new SpecialFieldValue(this, null, "clearReadStatus", Localization.lang("Clear read status"), null,
                Localization.lang("No read status information")));
        Icon tmpicon;
        tmpicon = IconTheme.JabRefIcon.READ_STATUS_READ.getSmallIcon();
        // DO NOT TRANSLATE "read" as this makes the produced .bib files non portable
        values.add(new SpecialFieldValue(this, "read", "setReadStatusToRead",
                Localization.lang("Set read status to read"), tmpicon,
                Localization.lang("Read status read")));
        tmpicon = IconTheme.JabRefIcon.READ_STATUS_SKIMMED.getSmallIcon();
        values.add(new SpecialFieldValue(this, "skimmed", "setReadStatusToSkimmed",
                Localization.lang("Set read status to skimmed"), tmpicon,
                Localization.lang("Read status skimmed")));
        this.setValues(values);
        TEXT_DONE_PATTERN = "Set read status to '%0' for %1 entries";
    }

    public static ReadStatus getInstance() {
        if (ReadStatus.INSTANCE == null) {
            ReadStatus.INSTANCE = new ReadStatus();
        }
        return ReadStatus.INSTANCE;
    }

    @Override
    public String getFieldName() {
        return SpecialFieldsUtils.FIELDNAME_READ;
    }

    @Override
    public Icon getRepresentingIcon() {
        return this.icon;
    }

    @Override
    public String getToolTip() {
        return Localization.lang("Read status");
    }

    @Override
    public String getMenuString() {
        return Localization.lang("Read status");
    }
}
