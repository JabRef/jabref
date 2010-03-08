package net.sf.jabref;

/**
 * This class defines entry types for BibLatex support.
 */
public class BibLatexEntryTypes {

    /*
        "rare" fields?
            "annotator", "commentator", "titleaddon", "editora", "editorb", "editorc",
            "issuetitle", "issuesubtitle", "origlanguage", "version", "addendum"

     */

    public static final BibtexEntryType ARTICLE = new BibtexEntryType() {
        public String getName() {
            return "Article";
        }
        public String[] getRequiredFields() {
            return new String[] {"author", "title", "journaltitle", "year", "date"};
        }
        public String[] getOptionalFields() {
            return new String[] {"translator", "annotator", "commentator", "subtitle", "titleaddon",
                "editor", "editora", "editorb", "editorc", "journalsubtitle", "issuetitle",
                "issuesubtitle", "language", "origlanguage", "series", "volume", "number",
                "eid", "issue", "date", "month", "year", "pages", "version", "note", "issn",
                "addendum", "pubstate", "doi", "eprint", "eprinttype", "url", "urldate"};
        }

        // TODO: number vs issue?
        public String[] getPrimaryOptionalFields() {
            return new String[] {"subtitle", "editor", "series", "volume", "number",
                "eid", "issue", "date", "month", "year", "pages", "note", "issn",
                "doi", "eprint", "eprinttype", "url", "urldate"};
        }

        public String describeRequiredFields() {
            return "";
        }
        public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
            return entry.allFieldsPresent(getRequiredFields(), database);
        }
    };

    /*public static final BibtexEntryType ARTICLE = new BibtexEntryType() {
        public String getName() {
            return "Article";
        }
        public String[] getRequiredFields() {
            return new String[] {};
        }
        public String[] getOptionalFields() {
            return new String[] {};
        }
        public String describeRequiredFields() {
            return "";
        }
        public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
            return entry.allFieldsPresent(getRequiredFields(), database);
        }
    };*/
}
