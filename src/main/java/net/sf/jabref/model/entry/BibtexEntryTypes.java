package net.sf.jabref.model.entry;

import net.sf.jabref.model.database.BibtexDatabase;

public class BibtexEntryTypes {

    public static final BibtexEntryType OTHER =
            new BibtexEntryType() {

                @Override
                public String getName() {
                    return "Other";
                }

                @Override
                public String[] getOptionalFields() {
                    return new String[0];
                }

                @Override
                public String[] getRequiredFields() {
                    return new String[0];
                }

                @Override
                public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
                    return true;
                }
            };

    public static final BibtexEntryType ARTICLE =
            new BibtexEntryType() {

                @Override
                public String getName() {
                    return "Article";
                }

                @Override
                public String[] getOptionalFields() {
                    return new String[]{
                            "volume", "pages", "number", "month", "note", //- "volume", "pages", "part", "eid"
                    };
                }

                @Override
                public String[] getRequiredFields() {
                    return new String[]{
                            "author", "title", "journal", "year" //+ "volume", "pages"
                    };
                }

                @Override
                public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
                    return entry.allFieldsPresent(new String[]{
                            "author", "title", "journal", "year", "bibtexkey", "volume", "pages"
                    }, database);
                }
            };

    public static final BibtexEntryType BOOKLET =
            new BibtexEntryType() {

                @Override
                public String getName() {
                    return "Booklet";
                }

                @Override
                public String[] getOptionalFields() {
                    return new String[]{
                            "author", "howpublished", "address", "month", "year", "note" //+ "lastchecked"
                    };
                }

                @Override
                public String[] getRequiredFields() {
                    return new String[]{"title"};
                }

                @Override
                public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
                    return entry.allFieldsPresent(new String[]{"title", "bibtexkey"}, database);
                }
            };

    public static final BibtexEntryType INBOOK =
            new BibtexEntryType() {

                @Override
                public String getName() {
                    return "InBook";
                }

                @Override
                public String[] getOptionalFields() {
                    return new String[]
                            {
                                    "volume", "number", "series", "type", "address", "edition",
                                    "month", "note" //+ "pages"
                            };
                }

                @Override
                public String[] getRequiredFields() {
                    return new String[]
                            {
                                    "chapter", "pages", "title", "publisher", "year", "editor",
                                    "author"
                            };
                }

                @Override
                public String[] getRequiredFieldsForCustomization() {
                    return new String[]{"author/editor", "title", "chapter/pages", "year", "publisher"};
                }

                @Override
                public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
                    return entry.allFieldsPresent(new String[]
                            {
                                    "title", "publisher", "year", "bibtexkey"
                            }, database) &&
                            entry.atLeastOnePresent(new String[]{"author", "editor"}, database) &&
                            entry.atLeastOnePresent(new String[]{"chapter", "pages"}, database);
                }
            };

    public static final BibtexEntryType BOOK =
            new BibtexEntryType() {

                @Override
                public String getName() {
                    return "Book";
                }

                @Override
                public String[] getOptionalFields() {
                    return new String[]
                            {
                                    "volume", "number", "series", "address", "edition", "month",
                                    "note" //+ pages
                            };
                }

                @Override
                public String[] getRequiredFields() {
                    return new String[]
                            {
                                    "title", "publisher", "year", "editor", "author"
                            };
                }

                @Override
                public String[] getRequiredFieldsForCustomization() {
                    return new String[]
                            {
                                    "title", "publisher", "year", "author/editor"
                            };
                }

                @Override
                public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
                    return entry.allFieldsPresent(new String[]
                            {
                                    "title", "publisher", "year", "bibtexkey"
                            }, database) &&
                            entry.atLeastOnePresent(new String[]{"author", "editor"}, database);

                }
            };

    public static final BibtexEntryType INCOLLECTION =
            new BibtexEntryType() {

                @Override
                public String getName() {
                    return "InCollection";
                }

                @Override
                public String[] getOptionalFields() {
                    return new String[]
                            {
                                    "editor", "volume", "number", "series", "type", "chapter",
                                    "pages", "address", "edition", "month", "note"
                            };
                }

                @Override
                public String[] getRequiredFields() {
                    return new String[]
                            {
                                    "author", "title", "booktitle", "publisher", "year"
                            };
                }

                @Override
                public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
                    return entry.allFieldsPresent(new String[]
                            {
                                    "author", "title", "booktitle", "publisher", "year",
                                    "bibtexkey"

                            }, database);
                }
            };

    public static final BibtexEntryType CONFERENCE =
            new BibtexEntryType() {

                @Override
                public String getName() {
                    return "Conference";
                }

                @Override
                public String[] getOptionalFields() {
                    return new String[]
                            {
                                    "editor", "volume", "number", "series", "pages",
                                    "address", "month", "organization", "publisher", "note"
                            };
                }

                @Override
                public String[] getRequiredFields() {
                    return new String[]
                            {
                                    "author", "title", "booktitle", "year"
                            };
                }

                @Override
                public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
                    return entry.allFieldsPresent(new String[]
                            {
                                    "author", "title", "booktitle", "year", "bibtexkey"
                            }, database);
                }
            };

    public static final BibtexEntryType INPROCEEDINGS =
            new BibtexEntryType() {

                @Override
                public String getName() {
                    return "InProceedings";
                }

                @Override
                public String[] getOptionalFields() {
                    return new String[]
                            {
                                    "editor", "volume", "number", "series", "pages",
                                    "address", "month", "organization", "publisher", "note"
                            };
                }

                @Override
                public String[] getRequiredFields() {
                    return new String[]
                            {
                                    "author", "title", "booktitle", "year"
                            };
                }

                @Override
                public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
                    return entry.allFieldsPresent(new String[]
                            {
                                    "author", "title", "booktitle", "year", "bibtexkey"
                            }, database);
                }
            };

    public static final BibtexEntryType PROCEEDINGS =
            new BibtexEntryType() {

                @Override
                public String getName() {
                    return "Proceedings";
                }

                @Override
                public String[] getOptionalFields() {
                    return new String[]
                            {
                                    "editor", "volume", "number", "series", "address",
                                    "publisher", "note", "month", "organization"
                            };
                }

                @Override
                public String[] getRequiredFields() {
                    return new String[]
                            {
                                    "title", "year"
                            };
                }

                @Override
                public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
                    return entry.allFieldsPresent(new String[]
                            {
                                    "title", "year", "bibtexkey"
                            }, database);
                }
            };

    public static final BibtexEntryType MANUAL =
            new BibtexEntryType() {

                @Override
                public String getName() {
                    return "Manual";
                }

                @Override
                public String[] getOptionalFields() {
                    return new String[]
                            {
                                    "author", "organization", "address", "edition",
                                    "month", "year", "note"
                            };
                }

                @Override
                public String[] getRequiredFields() {
                    return new String[]
                            {
                                    "title"
                            };
                }

                @Override
                public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
                    return entry.allFieldsPresent(new String[]
                            {
                                    "title", "bibtexkey"
                            }, database);
                }
            };

    public static final BibtexEntryType TECHREPORT =
            new BibtexEntryType() {

                @Override
                public String getName() {
                    return "TechReport";
                }

                @Override
                public String[] getOptionalFields() {
                    return new String[]
                            {
                                    "type", "number", "address", "month", "note"
                            };
                }

                @Override
                public String[] getRequiredFields() {
                    return new String[]
                            {
                                    "author", "title", "institution", "year"
                            };
                }

                @Override
                public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
                    return entry.allFieldsPresent(new String[]
                            {
                                    "author", "title", "institution", "year",
                                    "bibtexkey"
                            }, database);
                }
            };

    public static final BibtexEntryType MASTERSTHESIS =
            new BibtexEntryType() {

                @Override
                public String getName() {
                    return "MastersThesis";
                }

                @Override
                public String[] getOptionalFields() {
                    return new String[]
                            {
                                    "type", "address", "month", "note"
                            };
                }

                @Override
                public String[] getRequiredFields() {
                    return new String[]
                            {
                                    "author", "title", "school", "year"
                            };
                }

                @Override
                public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
                    return entry.allFieldsPresent(new String[]
                            {
                                    "author", "title", "school", "year", "bibtexkey"
                            }, database);
                }
            };

    public static final BibtexEntryType PHDTHESIS =
            new BibtexEntryType() {

                @Override
                public String getName() {
                    return "PhdThesis";
                }

                @Override
                public String[] getOptionalFields() {
                    return new String[]
                            {
                                    "type", "address", "month", "note"
                            };
                }

                @Override
                public String[] getRequiredFields() {
                    return new String[]
                            {
                                    "author", "title", "school", "year"
                            };
                }

                @Override
                public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
                    return entry.allFieldsPresent(new String[]
                            {
                                    "author", "title", "school", "year", "bibtexkey"
                            }, database);
                }
            };

    public static final BibtexEntryType UNPUBLISHED =
            new BibtexEntryType() {

                @Override
                public String getName() {
                    return "Unpublished";
                }

                @Override
                public String[] getOptionalFields() {
                    return new String[]
                            {
                                    "month", "year"
                            };
                }

                @Override
                public String[] getRequiredFields() {
                    return new String[]
                            {
                                    "author", "title", "note"
                            };
                }

                @Override
                public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
                    return entry.allFieldsPresent(new String[]
                            {
                                    "author", "title", "note", "bibtexkey"
                            }, database);
                }
            };

    public static final BibtexEntryType PERIODICAL =
            new BibtexEntryType() {

                @Override
                public String getName() {
                    return "Periodical";
                }

                @Override
                public String[] getOptionalFields() {
                    return new String[]
                            {
                                    "editor", "language", "series", "volume", "number", "organization", "month", "note", "url"
                            };
                }

                @Override
                public String[] getRequiredFields() {
                    return new String[]
                            {
                                    "title", "year"
                            };
                }

                @Override
                public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
                    return entry.allFieldsPresent(new String[]
                            {
                                    "title", "year", "bibtexkey"
                            }, database);
                }
            };

    public static final BibtexEntryType PATENT =
            new BibtexEntryType() {

                @Override
                public String getName() {
                    return "Patent";
                }

                @Override
                public String[] getOptionalFields() {
                    return new String[]
                            {
                                    "author", "title", "language", "assignee", "address", "type", "number", "day", "dayfiled", "month", "monthfiled", "note", "url"
                            };
                }

                @Override
                public String[] getRequiredFields() {
                    return new String[]
                            {
                                    "nationality", "number", "year", "yearfiled"
                            };
                }

                @Override
                public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
                    return entry.allFieldsPresent(new String[]
                            {
                                    "number", "bibtexkey"
                            }, database) &&
                            entry.atLeastOnePresent(new String[]{"year", "yearfiled"}, database);

                }
            };

    public static final BibtexEntryType STANDARD =
            new BibtexEntryType() {

                @Override
                public String getName() {
                    return "Standard";
                }

                @Override
                public String[] getOptionalFields() {
                    return new String[]
                            {
                                    "author", "language", "howpublished", "type", "number", "revision", "address", "month", "year", "note", "url"
                            };
                }

                @Override
                public String[] getRequiredFields() {
                    return new String[]
                            {
                                    "title", "organization", "institution"
                            };
                }

                @Override
                public String[] getRequiredFieldsForCustomization() {
                    return new String[]{"title", "organization/institution"};
                }

                @Override
                public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
                    return entry.allFieldsPresent(new String[]
                            {
                                    "title", "bibtexkey"
                            }, database) &&
                            entry.atLeastOnePresent(new String[]{"organization", "institution"}, database);

                }
            };

    public static final BibtexEntryType ELECTRONIC =
            new BibtexEntryType() {

                @Override
                public String getName() {
                    return "Electronic";
                }

                @Override
                public String[] getOptionalFields() {
                    return new String[]
                            {
                                    "author", "month", "year", "title", "language", "howpublished", "organization", "address", "note", "url"
                            };
                }

                @Override
                public String[] getRequiredFields() {
                    return new String[]{};
                }

                @Override
                public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
                    return entry.allFieldsPresent(new String[]
                            {
                                    "bibtexkey"
                            }, database);
                }
            };

    public static final BibtexEntryType MISC =
            new BibtexEntryType() {

                @Override
                public String getName() {
                    return "Misc";
                }

                @Override
                public String[] getOptionalFields() {
                    return new String[]
                            {
                                    "author", "title", "howpublished", "month", "year", "note"
                            };
                }

                @Override
                public String[] getRequiredFields() {
                    return new String[]{};
                }

                @Override
                public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
                    return entry.allFieldsPresent(new String[]
                            {
                                    "bibtexkey"
                            }, database);
                }
            };

    /**
     * This type is used for IEEEtran.bst to control various
     * be repeated or not. Not a very elegant solution, but it works...
     */
    public static final BibtexEntryType IEEETRANBSTCTL =
            new BibtexEntryType() {

                @Override
                public String getName() {
                    return "IEEEtranBSTCTL";
                }

                @Override
                public String[] getOptionalFields() {
                    return new String[]{
                            "ctluse_article_number", "ctluse_paper", "ctluse_forced_etal",
                            "ctlmax_names_forced_etal", "ctlnames_show_etal", "ctluse_alt_spacing",
                            "ctlalt_stretch_factor", "ctldash_repeated_names", "ctlname_format_string",
                            "ctlname_latex_cmd", "ctlname_url_prefix"
                    };
                }

                @Override
                public String[] getRequiredFields() {
                    return new String[]{};
                }

                @Override
                public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
                    return true;
                }

                @Override
                public boolean isVisibleAtNewEntryDialog() {
                    return false;
                }
            };

    /**
     * This type is provided as an emergency choice if the user makes
     * customization changes that remove the type of an entry.
     */
    public static final BibtexEntryType TYPELESS =
            new BibtexEntryType() {

                @Override
                public String getName() {
                    return "Typeless";
                }

                @Override
                public String[] getOptionalFields() {
                    return new String[]{};
                }

                @Override
                public String[] getRequiredFields() {
                    return new String[]{};
                }

                @Override
                public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
                    return false;
                }
            };
}
