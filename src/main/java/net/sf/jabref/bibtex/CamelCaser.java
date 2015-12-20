package net.sf.jabref.bibtex;

import java.util.HashMap;
import java.util.Map;

class CamelCaser {

    /**
     * Map that defines camel cased versions of field names
     */
    private static final Map<String, String> nameMap = new HashMap<>();


    static {
        put("bibtexkey", "BibTeXKey");
        put("bookauthor", "BookAuthor");
        put("booksubtitle", "BookSubTitle");
        put("booktitle", "BookTitle");
        put("booktitleaddon", "BookTitleAddon");
        put("crossref", "CrossRef");
        put("ctlalt_stretch_factor", "CtlAlt_Stretch_Factor");
        put("ctldash_repeated_names", "CtlDash_Repeated_Names");
        put("ctlname_format_string", "CtlName_Format_String");
        put("ctlname_latex_cmd", "CtlName_Latex_Cmd");
        put("ctlname_url_prefix", "CtlName_Url_Prefix");
        put("ctlmax_names_forced_etal", "CtlMax_Names_Forced_Etal");
        put("ctlnames_show_etal", "CtlNames_Show_Etal");
        put("ctluse_alt_spacing", "CtlUse_Alt_Spacing");
        put("ctluse_article_number", "CtlUse_Article_Number");
        put("ctluse_forced_etal", "Ctl_Forced_Etal");
        put("ctluse_paper", "CtlUse_Paper");
        put("ctluse_url", "CtlUse_Url");
        put("dayfiled", "DayFiled");
        put("doi", "DOI");
        put("editora", "EditorA");
        put("editorb", "EditorB");
        put("editorc", "EditorC");
        put("ee", "EE");
        put("eid", "EID");
        put("entryset", "EntrySet");
        put("eprint", "Eprint");
        put("eprintclass", "EprintClass");
        put("eprinttype", "EprintType");
        put("eventdate", "EventDate");
        put("eventtitle", "EventTitle");
        put("eventtitleaddon", "EventTitleAddon");
        put("howpublished", "HowPublished");
        put("issuesubtitle", "IssueSubTitle");
        put("issuetitle", "IssueTitle");
        put("journalsubtitle", "JournalSubTitle");
        put("journaltitle", "JournalTitle");
        put("lastchecked", "LastChecked");
        put("isbn", "ISBN");
        put("isrn", "ISRN");
        put("issn", "ISSN");
        put("mainsubtitle", "MainSubTitle");
        put("maintitle", "MainTitle");
        put("maintitleaddon", "MainTitleAddon");
        put("monthfiled", "MonthFiled");
        put("origlanguage", "OrigLanguage");
        put("pagetotal", "PageTotal");
        put("pubstate", "PubState");
        put("subtitle", "SubTitle");
        put("titleaddon", "TitleAddon");
        put("UNKNOWN", "UNKNOWN");
        put("url", "Url");
        put("urldate", "UrlDate");
        put("yearfiled", "YearFiled");
    }

    /**
     * Tries to provide a camel case version of fieldName. If no predefined camel case version can be found, the first letter is turned to upper case
     *
     * @param fieldName
     * @return
     */
    public static String toCamelCase(String fieldName) {
        if ((fieldName == null) || fieldName.isEmpty()) {
            return "";
        }

        String camelCaseName = nameMap.get(fieldName);

        // if there is no mapping for this name, turn the first letter to uppercase
        if (camelCaseName == null) {
            camelCaseName = (String.valueOf(fieldName.charAt(0))).toUpperCase() + fieldName.substring(1);
        }

        return camelCaseName;
    }

    /**
     * Helper method to avoid a direct access to nameMap
     *
     * @param key   a key to put in nameMap
     * @param value the value to put in nameMap for key
     */
    private static void put(String key, String value) {
        nameMap.put(key, value);
    }
}
