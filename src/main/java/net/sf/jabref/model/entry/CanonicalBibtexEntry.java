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
    public static String getCanonicalRepresentation(BibEntry e) {
        StringBuilder sb = new StringBuilder();

        // generate first line: type and bibtex key
        String citeKey = Strings.nullToEmpty(e.getCiteKey());
        sb.append(String.format("@%s{%s,", e.getType().toLowerCase(Locale.US), citeKey)).append('\n');

        // we have to introduce a new Map as fields are stored case-sensitive in JabRef (see https://github.com/koppor/jabref/issues/45).
        Map<String, String> mapFieldToValue = new HashMap<>();

        // determine sorted fields -- all fields lower case
        SortedSet<String> sortedFields = new TreeSet<>();
        for (String fieldName : e.getFieldNames()) {
            // JabRef stores the key in the field KEY_FIELD, which must not be serialized
            if (!fieldName.equals(BibEntry.KEY_FIELD)) {
                String lowerCaseFieldName = fieldName.toLowerCase(Locale.US);
                sortedFields.add(lowerCaseFieldName);
                mapFieldToValue.put(lowerCaseFieldName, e.getField(fieldName));
            }
        }

        // generate field entries
        StringJoiner sj = new StringJoiner(",\n", "", "\n");
        for (String fieldName : sortedFields) {
            String line = String.format("  %s = {%s}", fieldName, String.valueOf(mapFieldToValue.get(fieldName)).replaceAll("\\r\\n","\n"));
            sj.add(line);
        }
        sb.append(sj);

        // append the closing entry bracket
        sb.append('}');
        return sb.toString();
    }

}
