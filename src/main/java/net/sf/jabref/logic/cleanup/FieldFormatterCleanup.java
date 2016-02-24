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
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import net.sf.jabref.importer.HTMLConverter;
import net.sf.jabref.logic.FieldChange;
import net.sf.jabref.logic.formatter.BibtexFieldFormatters;
import net.sf.jabref.logic.formatter.Formatter;
import net.sf.jabref.logic.formatter.bibtexfields.LatexFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.MonthFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.UnitFormatter;
import net.sf.jabref.logic.formatter.casechanger.CaseKeeper;
import net.sf.jabref.model.entry.BibEntry;

/**
 * Formats a given entry field with the specified formatter.
 */
public class FieldFormatterCleanup implements CleanupJob {

    private final String field;
    private final Formatter formatter;

    public static final CleanupJob PAGE_NUMBERS = new FieldFormatterCleanup("pages",
            BibtexFieldFormatters.PAGE_NUMBERS);
    public static final CleanupJob DATES = new FieldFormatterCleanup("date", BibtexFieldFormatters.DATE);
    public static final CleanupJob MONTH = new FieldFormatterCleanup("month", new MonthFormatter());
    public static final CleanupJob TITLE_CASE = new FieldFormatterCleanup("title", new CaseKeeper());
    public static final CleanupJob TITLE_UNITS = new FieldFormatterCleanup("title", new UnitFormatter());
    public static final CleanupJob TITLE_LATEX = new FieldFormatterCleanup("title", new LatexFormatter());
    public static final CleanupJob TITLE_HTML = new FieldFormatterCleanup("title", new HTMLConverter());


    public FieldFormatterCleanup(String field, Formatter formatter) {
        this.field = field;
        this.formatter = formatter;
    }

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        if ("all".equals(field)) {
            return cleanupAllFields(entry);
        } else {
            return cleanupSingleField(entry);
        }
    }

    private List<FieldChange> cleanupSingleField(BibEntry entry) {
        if (!entry.hasField(field)) {
            // Not set -> nothing to do
            return new ArrayList<>();
        }
        String oldValue = entry.getField(field);

        // Run formatter
        String newValue = formatter.format(oldValue);

        if (oldValue.equals(newValue)) {
            return new ArrayList<>();
        } else {
            entry.setField(field, newValue);
            FieldChange change = new FieldChange(entry, field, oldValue, newValue);
            return Collections.singletonList(change);
        }
    }

    private List<FieldChange> cleanupAllFields(BibEntry entry) {
        ArrayList<FieldChange> fieldChanges = new ArrayList<>();

        for (String fieldKey : entry.getFieldNames()) {
            String oldValue = entry.getField(fieldKey);
            String newValue = formatter.format(oldValue);

            if (!oldValue.equals(newValue)) {
                entry.setField(fieldKey, newValue);
                FieldChange change = new FieldChange(entry, fieldKey, oldValue, newValue);
                fieldChanges.add(change);
            }
        }

        return fieldChanges;
    }

    public String getField() {
        return field;
    }

    public Formatter getFormatter() {
        return formatter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FieldFormatterCleanup that = (FieldFormatterCleanup) o;
        return Objects.equals(field, that.field) &&
                Objects.equals(formatter, that.formatter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(field, formatter);
    }

    @Override
    public String toString() {
        return field + ": " + formatter.getKey();
    }
}
