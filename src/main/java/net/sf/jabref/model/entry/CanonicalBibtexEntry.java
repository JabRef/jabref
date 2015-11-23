package net.sf.jabref.model.entry;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.SortedSet;
import java.util.StringJoiner;
import java.util.TreeSet;

import com.google.common.base.Strings;

public class CanonicalBibtexEntry {

    /**
     * This returns a canonical BibTeX serialization. Special characters such as "{" or "&" are NOT escaped, but written
     * as is
     *
     * Serializes all fields, even the JabRef internal ones. Does NOT serialize "KEY_FIELD" as field, but as key
     */
    public static String getCanonicalRepresentation(BibtexEntry e) {
        StringBuilder sb = new StringBuilder();

        // generate first line: type and bibtex key
        String citeKey = Strings.nullToEmpty(e.getCiteKey());
        sb.append(String.format("@%s{%s,\n", e.getType().getName().toLowerCase(Locale.US), citeKey));

        // we have to introduce a new Map as fields are stored case-sensitive in JabRef (see https://github.com/koppor/jabref/issues/45).
        Map<String, String> mapFieldToValue = new HashMap<>();

        // determine sorted fields -- all fields lower case
        SortedSet<String> sortedFields = new TreeSet<>();
        for (String fieldName : e.getFieldNames()) {
            // JabRef stores the key in the field KEY_FIELD, which must not be serialized
            if (!fieldName.equals(BibtexEntry.KEY_FIELD)) {
                String lowerCaseFieldName = fieldName.toLowerCase(Locale.US);
                sortedFields.add(lowerCaseFieldName);
                mapFieldToValue.put(lowerCaseFieldName, e.getField(fieldName));
            }
        }

        // generate field entries
        StringJoiner sj = new StringJoiner(",\n", "", "\n");
        for (String fieldName : sortedFields) {
            String line = String.format("  %s = {%s}", fieldName, mapFieldToValue.get(fieldName));
            sj.add(line);
        }
        sb.append(sj.toString());

        // append the closing entry bracket
        sb.append("}");
        return sb.toString();
    }

}
