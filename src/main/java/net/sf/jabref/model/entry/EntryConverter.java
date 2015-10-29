package net.sf.jabref.model.entry;

import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.gui.undo.UndoableFieldChange;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Converts Enntry models from BibTex to BibLaTex and back.
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

    /**
     * Converts to BibLatex format
     */
    public static void convertToBiblatex(BibtexEntry entry, NamedCompound ce) {

        for (Map.Entry<String, String> alias : FIELD_ALIASES_TEX_TO_LTX.entrySet()) {
            String oldFieldName = alias.getKey();
            String newFieldName = alias.getValue();
            String oldValue = entry.getField(oldFieldName);
            String newValue = entry.getField(newFieldName);
            if ((oldValue != null) && (!oldValue.isEmpty()) && (newValue == null))
            {
                // There is content in the old field and no value in the new, so just copy
                entry.setField(newFieldName, oldValue);
                ce.addEdit(new UndoableFieldChange(entry, newFieldName, null, oldValue));

                entry.setField(oldFieldName, null);
                ce.addEdit(new UndoableFieldChange(entry, oldFieldName, oldValue, null));
            }
        }

        // Dates: create date out of year and month, save it and delete old fields
        if ((entry.getField("date") == null) || (entry.getField("date").isEmpty()))
        {
            String newDate = entry.getFieldOrAlias("date");
            String oldYear = entry.getField("year");
            String oldMonth = entry.getField("month");
            entry.setField("date", newDate);
            entry.setField("year", null);
            entry.setField("month", null);

            ce.addEdit(new UndoableFieldChange(entry, "date", null, newDate));
            ce.addEdit(new UndoableFieldChange(entry, "year", oldYear, null));
            ce.addEdit(new UndoableFieldChange(entry, "month", oldMonth, null));
        }
    }
}
