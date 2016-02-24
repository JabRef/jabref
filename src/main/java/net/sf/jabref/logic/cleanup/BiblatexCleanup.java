/*  Copyright (C) 2003-2015 JabRef contributors.
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

package net.sf.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.sf.jabref.logic.FieldChange;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.EntryConverter;

/**
 * Converts the entry to BibLatex format.
 */
public class BiblatexCleanup implements CleanupJob {

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        ArrayList<FieldChange> changes = new ArrayList<>();
        for (Map.Entry<String, String> alias : EntryConverter.FIELD_ALIASES_TEX_TO_LTX.entrySet()) {
            String oldFieldName = alias.getKey();
            String newFieldName = alias.getValue();
            String oldValue = entry.getField(oldFieldName);
            String newValue = entry.getField(newFieldName);
            if ((oldValue != null) && (!oldValue.isEmpty()) && (newValue == null)) {
                // There is content in the old field and no value in the new, so just copy
                entry.setField(newFieldName, oldValue);
                changes.add(new FieldChange(entry, newFieldName, null, oldValue));

                entry.clearField(oldFieldName);
                changes.add(new FieldChange(entry, oldFieldName, oldValue, null));
            }
        }

        // Dates: create date out of year and month, save it and delete old fields
        if ((!entry.hasField("date")) || (entry.getField("date").isEmpty())) {
            String newDate = entry.getFieldOrAlias("date");
            String oldYear = entry.getField("year");
            String oldMonth = entry.getField("month");

            if (newDate != null) {
                entry.setField("date", newDate);
                entry.clearField("year");
                entry.clearField("month");

                changes.add(new FieldChange(entry, "date", null, newDate));
                changes.add(new FieldChange(entry, "year", oldYear, null));
                changes.add(new FieldChange(entry, "month", oldMonth, null));
            }
        }
        return changes;
    }
}
