package org.jabref.model.entry;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

/**
 * Converts Entry models from BibTex to biblatex and back.
 */
public class EntryConverter {

    public static Map<Field, Field> FIELD_ALIASES_BIBTEX_TO_BIBLATEX;

    public static Map<Field, Field> FIELD_ALIASES_BIBLATEX_TO_BIBTEX;

    // All aliases
    public static Map<Field, Field> FIELD_ALIASES;

    static {
        EntryConverter.FIELD_ALIASES_BIBTEX_TO_BIBLATEX = new HashMap<>();
        EntryConverter.FIELD_ALIASES_BIBTEX_TO_BIBLATEX.put(StandardField.ADDRESS, StandardField.LOCATION);
        EntryConverter.FIELD_ALIASES_BIBTEX_TO_BIBLATEX.put(StandardField.ANNOTE, StandardField.ANNOTATION);
        EntryConverter.FIELD_ALIASES_BIBTEX_TO_BIBLATEX.put(StandardField.ARCHIVEPREFIX, StandardField.EPRINTTYPE);
        EntryConverter.FIELD_ALIASES_BIBTEX_TO_BIBLATEX.put(StandardField.JOURNAL, StandardField.JOURNALTITLE);
        EntryConverter.FIELD_ALIASES_BIBTEX_TO_BIBLATEX.put(StandardField.KEY, StandardField.SORTKEY);
        EntryConverter.FIELD_ALIASES_BIBTEX_TO_BIBLATEX.put(StandardField.PRIMARYCLASS, StandardField.EPRINTCLASS);
        EntryConverter.FIELD_ALIASES_BIBTEX_TO_BIBLATEX.put(StandardField.SCHOOL, StandardField.INSTITUTION);

        // inverse map
        EntryConverter.FIELD_ALIASES_BIBLATEX_TO_BIBTEX = EntryConverter.FIELD_ALIASES_BIBTEX_TO_BIBLATEX
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

        // all aliases
        FIELD_ALIASES = new HashMap<>();
        FIELD_ALIASES.putAll(EntryConverter.FIELD_ALIASES_BIBTEX_TO_BIBLATEX);
        FIELD_ALIASES.putAll(EntryConverter.FIELD_ALIASES_BIBLATEX_TO_BIBTEX);
    }

    private EntryConverter() {
    }
}
