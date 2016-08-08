package net.sf.jabref.model.entry;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Converts Entry models from BibTex to BibLaTex and back.
 */
public class EntryConverter {
    // BibTex to BibLatex
    public static Map<String, String> FIELD_ALIASES_TEX_TO_LTX;
    // BibLatex to BibTex
    public static Map<String, String> FIELD_ALIASES_LTX_TO_TEX;
    // All aliases
    public static Map<String, String> FIELD_ALIASES;

    static {
        EntryConverter.FIELD_ALIASES_TEX_TO_LTX = new HashMap<>();
        EntryConverter.FIELD_ALIASES_TEX_TO_LTX.put(FieldName.ADDRESS, FieldName.LOCATION);
        EntryConverter.FIELD_ALIASES_TEX_TO_LTX.put(FieldName.ANNOTE, FieldName.ANNOTATION);
        EntryConverter.FIELD_ALIASES_TEX_TO_LTX.put("archiveprefix", FieldName.EPRINTTYPE);
        EntryConverter.FIELD_ALIASES_TEX_TO_LTX.put(FieldName.JOURNAL, FieldName.JOURNALTITLE);
        EntryConverter.FIELD_ALIASES_TEX_TO_LTX.put(FieldName.KEY, "sortkey");
        EntryConverter.FIELD_ALIASES_TEX_TO_LTX.put(FieldName.PDF, FieldName.FILE);
        EntryConverter.FIELD_ALIASES_TEX_TO_LTX.put("primaryclass", FieldName.EPRINTCLASS);
        EntryConverter.FIELD_ALIASES_TEX_TO_LTX.put(FieldName.SCHOOL, FieldName.INSTITUTION);

        // inverse map
        EntryConverter.FIELD_ALIASES_LTX_TO_TEX = EntryConverter.FIELD_ALIASES_TEX_TO_LTX.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

        // all aliases
        FIELD_ALIASES = new HashMap<>();
        FIELD_ALIASES.putAll(EntryConverter.FIELD_ALIASES_TEX_TO_LTX);
        FIELD_ALIASES.putAll(EntryConverter.FIELD_ALIASES_LTX_TO_TEX);
    }
}
