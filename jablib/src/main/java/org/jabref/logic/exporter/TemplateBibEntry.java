package org.jabref.logic.exporter;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.FieldFactory;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// Read-only view of a [BibEntry] for use in export templates (see [VelocityTemplateExporter]).
///
/// All accessors return plain strings — the empty string for unset fields — so that templates
/// can rely on Velocity's empty-string check: `#if( $entry.getField("author") ) ... #end`.
public class TemplateBibEntry {

    private final BibEntry entry;
    private final BibDatabase database;

    public TemplateBibEntry(@NonNull BibEntry entry, @Nullable BibDatabase database) {
        this.entry = entry;
        this.database = database;
    }

    /// Usable as `$entry.type` in templates. Standard entry types are lower case (e.g., `article`).
    public String getType() {
        return entry.getType().getName();
    }

    /// Usable as `$entry.citationKey` in templates.
    public String getCitationKey() {
        return entry.getCitationKey().orElse("");
    }

    /// Returns the value of the given field, resolving field aliases (e.g., `year` from `date`),
    /// BibTeX strings, and `crossref` inheritance.
    public String getField(String fieldName) {
        return entry.getResolvedFieldOrAlias(FieldFactory.parseField(fieldName), database).orElse("");
    }
}
