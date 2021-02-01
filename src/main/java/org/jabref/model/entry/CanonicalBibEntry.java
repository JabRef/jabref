package org.jabref.model.entry;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.StringJoiner;
import java.util.TreeSet;

import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.InternalField;

public class CanonicalBibEntry {

    private CanonicalBibEntry() {
    }

    /**
     * This returns a canonical BibTeX serialization. Serializes all fields, even the JabRef internal ones. Does NOT
     * serialize "KEY_FIELD" as field, but as key
     *
     * <ul>
     *     <li>Special characters such as "{" or "&" are NOT escaped, but written as</li>
     *     <li>String constants are not handled. That means, <code>month = apr</code> in a bib file gets <code>month = {#apr#}</code>. This indicates that the month field is correctly stored</li>
     * </ul>
     */
    public static String getCanonicalRepresentation(BibEntry entry) {
        StringBuilder sb = new StringBuilder();

        sb.append(entry.getUserComments());

        // generate first line: type and citation key
        String citeKey = entry.getCitationKey().orElse("");
        sb.append(String.format("@%s{%s,", entry.getType().getName(), citeKey)).append('\n');

        // we have to introduce a new Map as fields are stored case-sensitive in JabRef (see https://github.com/koppor/jabref/issues/45).
        Map<String, String> mapFieldToValue = new HashMap<>();

        // determine sorted fields -- all fields lower case
        SortedSet<String> sortedFields = new TreeSet<>();
        for (Entry<Field, String> field : entry.getFieldMap().entrySet()) {
            Field fieldName = field.getKey();
            String fieldValue = field.getValue();
            // JabRef stores the key in the field KEY_FIELD, which must not be serialized
            if (!fieldName.equals(InternalField.KEY_FIELD)) {
                String lowerCaseFieldName = fieldName.getName().toLowerCase(Locale.US);
                sortedFields.add(lowerCaseFieldName);
                mapFieldToValue.put(lowerCaseFieldName, fieldValue);
            }
        }

        // generate field entries
        StringJoiner sj = new StringJoiner(",\n", "", "\n");
        for (String fieldName : sortedFields) {
            String line = String.format("  %s = {%s}", fieldName, String.valueOf(mapFieldToValue.get(fieldName)).replaceAll("\\r\\n", "\n"));
            sj.add(line);
        }

        sj.add(String.format("  _jabref_shared = {sharedId: %d, version: %d}", entry.getSharedBibEntryData().getSharedID(), entry.getSharedBibEntryData().getVersion()));

        sb.append(sj);

        // append the closing entry bracket
        sb.append('}');
        return sb.toString();
    }
}
