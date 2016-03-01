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
import net.sf.jabref.logic.formatter.bibtexfields.UnicodeToLatexFormatter;
import net.sf.jabref.model.entry.BibEntry;

/**
 * Converts Unicode characters to LaTeX code.
 */
public class UnicodeCleanup implements CleanupJob {

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        ArrayList<FieldChange> changes = new ArrayList<>();
        final String[] fields = {"title", "author", "abstract"};
        final UnicodeToLatexFormatter unicodeConverter = new UnicodeToLatexFormatter();
        for (String field : fields) {
            if (!entry.hasField(field)) {
                continue;
            }
            String oldValue = entry.getField(field);
            String newValue = unicodeConverter.format(oldValue);
            if (!oldValue.equals(newValue)) {
                entry.setField(field, newValue);
                changes.add(new FieldChange(entry, field, oldValue, newValue));
            }
        }
        return changes;
    }

}
