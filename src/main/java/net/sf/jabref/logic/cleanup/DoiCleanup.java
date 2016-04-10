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
import java.util.Optional;

import net.sf.jabref.logic.FieldChange;
import net.sf.jabref.logic.formatter.bibtexfields.ClearFormatter;
import net.sf.jabref.logic.util.DOI;
import net.sf.jabref.model.entry.BibEntry;

/**
 * Formats the DOI (e.g. removes http part) and also moves DOIs from note, url or ee field to the doi field.
 */
public class DoiCleanup implements CleanupJob {

    /**
     * Fields to check for DOIs.
     */
    private static final String[] FIELDS = {"note", "url", "ee"};

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {

        ArrayList<FieldChange> changes = new ArrayList<>();

        // First check if the Doi Field is empty
        if (entry.hasField("doi")) {
            String doiFieldValue = entry.getField("doi");

            Optional<DOI> doi = DOI.build(doiFieldValue);

            if (doi.isPresent()) {
                String newValue = doi.get().getDOI();
                if (!doiFieldValue.equals(newValue)) {
                    entry.setField("doi", newValue);

                    FieldChange change = new FieldChange(entry, "doi", doiFieldValue, newValue);
                    changes.add(change);
                }

                // Doi field seems to contain Doi -> cleanup note, url, ee field
                for (String field : FIELDS) {
                    DOI.build(entry.getField((field))).ifPresent(unused -> removeFieldValue(entry, field, changes));
                }
            }
        } else {
            // As the Doi field is empty we now check if note, url, or ee field contains a Doi
            for (String field : FIELDS) {
                Optional<DOI> doi = DOI.build(entry.getField(field));

                if (doi.isPresent()) {
                    // update Doi
                    String oldValue = entry.getField("doi");
                    String newValue = doi.get().getDOI();

                    entry.setField("doi", newValue);

                    FieldChange change = new FieldChange(entry, "doi", oldValue, newValue);
                    changes.add(change);

                    removeFieldValue(entry, field, changes);
                }
            }
        }

        return changes;
    }

    private void removeFieldValue(BibEntry entry, String field, List<FieldChange> changes) {
        CleanupJob eraser = new FieldFormatterCleanup(field, new ClearFormatter());
        changes.addAll(eraser.cleanup(entry));
    }
}
