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

    // BibTeX to biblatex
    public static Map<Field, Field> FIELD_ALIASES_TEX_TO_LTX;

    // biblatex to BibTeX
    public static Map<Field, Field> FIELD_ALIASES_LTX_TO_TEX;

    // All aliases
    public static Map<Field, Field> FIELD_ALIASES;

    static {
        EntryConverter.FIELD_ALIASES_TEX_TO_LTX = new HashMap<>();
        EntryConverter.FIELD_ALIASES_TEX_TO_LTX.put(StandardField.ADDRESS, StandardField.LOCATION);
        EntryConverter.FIELD_ALIASES_TEX_TO_LTX.put(StandardField.ANNOTE, StandardField.ANNOTATION);
        EntryConverter.FIELD_ALIASES_TEX_TO_LTX.put(StandardField.ARCHIVEPREFIX, StandardField.EPRINTTYPE);
        EntryConverter.FIELD_ALIASES_TEX_TO_LTX.put(StandardField.JOURNAL, StandardField.JOURNALTITLE);
        EntryConverter.FIELD_ALIASES_TEX_TO_LTX.put(StandardField.KEY, StandardField.SORTKEY);
        EntryConverter.FIELD_ALIASES_TEX_TO_LTX.put(StandardField.PRIMARYCLASS, StandardField.EPRINTCLASS);
        EntryConverter.FIELD_ALIASES_TEX_TO_LTX.put(StandardField.SCHOOL, StandardField.INSTITUTION);

        // inverse map
        EntryConverter.FIELD_ALIASES_LTX_TO_TEX = EntryConverter.FIELD_ALIASES_TEX_TO_LTX
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

        // all aliases
        FIELD_ALIASES = new HashMap<>();
        FIELD_ALIASES.putAll(EntryConverter.FIELD_ALIASES_TEX_TO_LTX);
        FIELD_ALIASES.putAll(EntryConverter.FIELD_ALIASES_LTX_TO_TEX);
    }

    private EntryConverter() {
    }
}
