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
        EntryConverter.FIELD_ALIASES_TEX_TO_LTX.put("address", "location");
        EntryConverter.FIELD_ALIASES_TEX_TO_LTX.put("annote", "annotation");
        EntryConverter.FIELD_ALIASES_TEX_TO_LTX.put("archiveprefix", "eprinttype");
        EntryConverter.FIELD_ALIASES_TEX_TO_LTX.put("journal", "journaltitle");
        EntryConverter.FIELD_ALIASES_TEX_TO_LTX.put("key", "sortkey");
        EntryConverter.FIELD_ALIASES_TEX_TO_LTX.put("pdf", "file");
        EntryConverter.FIELD_ALIASES_TEX_TO_LTX.put("primaryclass", "eprintclass");
        EntryConverter.FIELD_ALIASES_TEX_TO_LTX.put("school", "institution");

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
