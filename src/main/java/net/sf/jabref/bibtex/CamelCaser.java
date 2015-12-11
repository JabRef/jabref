package net.sf.jabref.bibtex;

import java.util.HashMap;
import java.util.Map;

class CamelCaser {

    /**
     * Map that defines camel cased versions of field names
     */
    private static final Map<String, String> nameMap = new HashMap<>();


    static {
        // The field name display map.
        nameMap.put("bibtexkey", "BibTeXKey");
        nameMap.put("doi", "DOI");
        nameMap.put("ee", "EE");
        nameMap.put("howpublished", "HowPublished");
        nameMap.put("lastchecked", "LastChecked");
        nameMap.put("isbn", "ISBN");
        nameMap.put("issn", "ISSN");
        nameMap.put("UNKNOWN", "UNKNOWN");
        nameMap.put("url", "URL");
    }

    /**
     * Tries to provide a camel case version of fieldName. If no predefined camel case version can be found, the first letter is turned to upper case
     *
     * @param fieldName
     * @return
     */
    public static String toCamelCase(String fieldName) {
        if (fieldName == null || fieldName == "") {
            return "";
        }

        String camelCaseName = nameMap.get(fieldName);

        // if there is no mapping for this name, turn the first letter to uppercase
        if (camelCaseName == null) {
            camelCaseName = (String.valueOf(fieldName.charAt(0))).toUpperCase() + fieldName.substring(1);
        }

        return camelCaseName;
    }
}
