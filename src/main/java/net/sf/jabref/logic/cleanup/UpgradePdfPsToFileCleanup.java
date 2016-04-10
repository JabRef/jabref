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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.sf.jabref.Globals;
import net.sf.jabref.external.ExternalFileTypes;
import net.sf.jabref.logic.FieldChange;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FileField;
import net.sf.jabref.model.entry.ParsedFileField;

/**
 * Collects file links from the given set of fields, and add them to the list contained in the file field.
 */
public class UpgradePdfPsToFileCleanup implements CleanupJob {

    private final List<String> fields;

    public UpgradePdfPsToFileCleanup(List<String> fields) {
        this.fields = Objects.requireNonNull(fields);
    }

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        List<FieldChange> changes = new ArrayList<>();

        // If there are already links in the file field, keep those on top:
        String oldFileContent = entry.getField(Globals.FILE_FIELD);

        List<ParsedFileField> fileList = new ArrayList<>(FileField.parse(oldFileContent));
        int oldItemCount = fileList.size();
        for (String field : fields) {
            entry.getFieldOptional(field).ifPresent(o -> {
                if (o.trim().isEmpty()) {
                    return;
                }
                File f = new File(o);
                ParsedFileField flEntry = new ParsedFileField(f.getName(), o,
                        ExternalFileTypes.getInstance().getExternalFileTypeNameByExt(field));
                fileList.add(flEntry);

                entry.clearField(field);
                changes.add(new FieldChange(entry, field, o, null));
            });
        }

        if (fileList.size() != oldItemCount) {
            String newValue = FileField.getStringRepresentation(fileList);
            entry.setField(Globals.FILE_FIELD, newValue);
            changes.add(new FieldChange(entry, Globals.FILE_FIELD, oldFileContent, newValue));
        }

        return changes;
    }
}
