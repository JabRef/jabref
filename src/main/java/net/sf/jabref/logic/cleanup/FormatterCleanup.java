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

import net.sf.jabref.logic.FieldChange;
import net.sf.jabref.logic.formatter.Formatter;
import net.sf.jabref.model.entry.BibEntry;

/**
 * Runs a formatter on every field.
 */
public class FormatterCleanup implements CleanupJob {

    private final Formatter formatter;


    public FormatterCleanup(Formatter formatter) {
        this.formatter = formatter;
    }

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        ArrayList<FieldChange> changes = new ArrayList<>();
        for (String field : entry.getFieldNames()) {
            String oldValue = entry.getField(field);

            // Run formatter
            String newValue = formatter.format(oldValue);

            if (!oldValue.equals(newValue)) {
                entry.setField(field, newValue);
                FieldChange change = new FieldChange(entry, field, oldValue, newValue);
                changes.add(change);
            }
        }
        return changes;
    }

}
