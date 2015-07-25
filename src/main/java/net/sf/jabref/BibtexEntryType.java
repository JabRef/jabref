/*
Copyright (C) 2003 David Weitzman, Morten O. Alver

All programs in this directory and
subdirectories are published under the GNU General Public License as
described below.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or (at
your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
USA

Further information about the GNU GPL is available at:
http://www.gnu.org/copyleft/gpl.ja.html

Note:
Modified for use in JabRef.

*/
package net.sf.jabref;

import java.util.Iterator;
import java.util.Locale;
import java.util.TreeMap;

/**
 * Provides a list of known entry types
 * <p/>
 * The list of optional and required fields is derived from http://en.wikipedia.org/wiki/BibTeX#Entry_types
 */
public abstract class BibtexEntryType implements Comparable<BibtexEntryType> {

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
                public String describeRequiredFields() {
                    return "";
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
                    return new String[] {
                            "volume", "pages", "number", "month", "note", //- "volume", "pages", "part", "eid"
                    };
                }

                @Override
                public String[] getRequiredFields() {
                    return new String[] {
                            "author", "title", "journal", "year" //+ "volume", "pages"
                    };
                }

                @Override
                public String describeRequiredFields() {
                    return "AUTHOR, TITLE, JOURNAL and YEAR";
                }

                @Override
                public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
                    return entry.allFieldsPresent(new String[] {
                            "author", "title", "journal", "year", "bibtexkey", "volume", "pages"
                    }, database);
                }
            };

    private static final BibtexEntryType BOOKLET =
            new BibtexEntryType() {

                @Override
                public String getName() {
                    return "Booklet";
                }

                @Override
                public String[] getOptionalFields() {
                    return new String[] {
                            "author", "howpublished", "address", "month", "year", "note" //+ "lastchecked"
                    };
                }

                @Override
                public String[] getRequiredFields() {
                    return new String[] {"title"};
                }

                @Override
                public String describeRequiredFields() {
                    return "TITLE";
                }

                @Override
                public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
                    return entry.allFieldsPresent(new String[] {"title", "bibtexkey"}, database);
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
                    return new String[] {"author/editor", "title", "chapter/pages", "year", "publisher"};
                }

                @Override
                public String describeRequiredFields() {
                    return "TITLE, CHAPTER and/or PAGES, PUBLISHER, YEAR, and an "
                            + "EDITOR and/or AUTHOR";
                }

                @Override
                public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
                    return entry.allFieldsPresent(new String[]
                            {
                                    "title", "publisher", "year", "bibtexkey"
                            }, database) &&
                            entry.atLeastOnePresent(new String[] {"author", "editor"}, database) &&
                            entry.atLeastOnePresent(new String[] {"chapter", "pages"}, database);
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
                public String describeRequiredFields() {
                    return "TITLE, PUBLISHER, YEAR, and an EDITOR and/or AUTHOR";
                }

                @Override
                public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
                    return entry.allFieldsPresent(new String[]
                            {
                                    "title", "publisher", "year", "bibtexkey"
                            }, database) &&
                            entry.atLeastOnePresent(new String[] {"author", "editor"}, database);

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
                public String describeRequiredFields() {
                    return "AUTHOR, TITLE, BOOKTITLE, PUBLISHER and YEAR";
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
                public String describeRequiredFields() {
                    return "AUTHOR, TITLE, BOOKTITLE and YEAR";
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
                public String describeRequiredFields() {
                    return "AUTHOR, TITLE, BOOKTITLE and YEAR";
                }

                @Override
                public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
                    return entry.allFieldsPresent(new String[]
                            {
                                    "author", "title", "booktitle", "year", "bibtexkey"
                            }, database);
                }
            };

    private static final BibtexEntryType PROCEEDINGS =
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
                public String describeRequiredFields() {
                    return "TITLE and YEAR";
                }

                @Override
                public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
                    return entry.allFieldsPresent(new String[]
                            {
                                    "title", "year", "bibtexkey"
                            }, database);
                }
            };

    private static final BibtexEntryType MANUAL =
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
                public String describeRequiredFields() {
                    return "TITLE";
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
                public String describeRequiredFields() {
                    return "AUTHOR, TITLE, INSTITUTION and YEAR";
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

    private static final BibtexEntryType MASTERSTHESIS =
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
                public String describeRequiredFields() {
                    return "AUTHOR, TITLE, SCHOOL and YEAR";
                }

                @Override
                public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
                    return entry.allFieldsPresent(new String[]
                            {
                                    "author", "title", "school", "year", "bibtexkey"
                            }, database);
                }
            };

    private static final BibtexEntryType PHDTHESIS =
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
                public String describeRequiredFields() {
                    return "AUTHOR, TITLE, SCHOOL and YEAR";
                }

                @Override
                public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
                    return entry.allFieldsPresent(new String[]
                            {
                                    "author", "title", "school", "year", "bibtexkey"
                            }, database);
                }
            };

    private static final BibtexEntryType UNPUBLISHED =
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
                public String describeRequiredFields() {
                    return "AUTHOR, TITLE and NOTE";
                }

                @Override
                public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
                    return entry.allFieldsPresent(new String[]
                            {
                                    "author", "title", "note", "bibtexkey"
                            }, database);
                }
            };

    private static final BibtexEntryType PERIODICAL =
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
                public String describeRequiredFields() {
                    return "TITLE and YEAR";
                }

                @Override
                public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
                    return entry.allFieldsPresent(new String[]
                            {
                                    "title", "year", "bibtexkey"
                            }, database);
                }
            };

    private static final BibtexEntryType PATENT =
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
                public String describeRequiredFields() {
                    return "NATIONALITY, NUMBER, YEAR or YEARFILED";
                }

                @Override
                public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
                    return entry.allFieldsPresent(new String[]
                            {
                                    "number", "bibtexkey"
                            }, database) &&
                            entry.atLeastOnePresent(new String[] {"year", "yearfiled"}, database);

                }
            };

    private static final BibtexEntryType STANDARD =
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
                    return new String[] {"title", "organization/institution"};
                }

                @Override
                public String describeRequiredFields() {
                    return "TITLE, ORGANIZATION or INSTITUTION";
                }

                @Override
                public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
                    return entry.allFieldsPresent(new String[]
                            {
                                    "title", "bibtexkey"
                            }, database) &&
                            entry.atLeastOnePresent(new String[] {"organization", "institution"}, database);

                }
            };

    private static final BibtexEntryType ELECTRONIC =
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
                    return null;
                }

                @Override
                public String describeRequiredFields() {
                    return "None";
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
                    return null;
                }

                @Override
                public String describeRequiredFields() {
                    return "None";
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
    private static final BibtexEntryType IEEETRANBSTCTL =
            new BibtexEntryType() {

                @Override
                public String getName() {
                    return "IEEEtranBSTCTL";
                }

                @Override
                public String[] getOptionalFields() {
                    return new String[] {
                            "ctluse_article_number", "ctluse_paper", "ctluse_forced_etal",
                            "ctlmax_names_forced_etal", "ctlnames_show_etal", "ctluse_alt_spacing",
                            "ctlalt_stretch_factor", "ctldash_repeated_names", "ctlname_format_string",
                            "ctlname_latex_cmd", "ctlname_url_prefix"
                    };
                }

                @Override
                public String[] getRequiredFields() {
                    return null;
                }

                @Override
                public String describeRequiredFields() {
                    return "None";
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
                    return null;
                }

                @Override
                public String[] getRequiredFields() {
                    return null;
                }

                @Override
                public String describeRequiredFields() {
                    return "None";
                }

                @Override
                public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
                    return false;
                }
            };

    public abstract String getName();

    @Override
    public int compareTo(BibtexEntryType o) {
        return getName().compareTo(o.getName());
    }

    public abstract String[] getOptionalFields();

    public abstract String[] getRequiredFields();

    public String[] getPrimaryOptionalFields() {
        return new String[0];
    }

    public abstract String describeRequiredFields();

    public abstract boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database);

    public String[] getUtilityFields() {
        return new String[] {"search"};
    }

    public boolean isRequired(String field) {
        String[] req = getRequiredFields();
        if (req == null) {
            return false;
        }
        for (String aReq : req) {
            if (aReq.equals(field)) {
                return true;
            }
        }
        return false;
    }

    public boolean isOptional(String field) {
        String[] opt = getOptionalFields();
        if (opt == null) {
            return false;
        }
        for (String anOpt : opt) {
            if (anOpt.equals(field)) {
                return true;
            }
        }
        return false;
    }

    public boolean isVisibleAtNewEntryDialog() {
        return true;
    }

    public static final TreeMap<String, BibtexEntryType> ALL_TYPES = new TreeMap<String, BibtexEntryType>();
    private static final TreeMap<String, BibtexEntryType> STANDARD_TYPES;

    static {
        // Put the standard entry types into the type map.
        if (!Globals.prefs.getBoolean(JabRefPreferences.BIBLATEX_MODE)) {
            BibtexEntryType.ALL_TYPES.put("article", BibtexEntryType.ARTICLE);
            BibtexEntryType.ALL_TYPES.put("inbook", BibtexEntryType.INBOOK);
            BibtexEntryType.ALL_TYPES.put("book", BibtexEntryType.BOOK);
            BibtexEntryType.ALL_TYPES.put("booklet", BibtexEntryType.BOOKLET);
            BibtexEntryType.ALL_TYPES.put("incollection", BibtexEntryType.INCOLLECTION);
            BibtexEntryType.ALL_TYPES.put("conference", BibtexEntryType.CONFERENCE);
            BibtexEntryType.ALL_TYPES.put("inproceedings", BibtexEntryType.INPROCEEDINGS);
            BibtexEntryType.ALL_TYPES.put("proceedings", BibtexEntryType.PROCEEDINGS);
            BibtexEntryType.ALL_TYPES.put("manual", BibtexEntryType.MANUAL);
            BibtexEntryType.ALL_TYPES.put("mastersthesis", BibtexEntryType.MASTERSTHESIS);
            BibtexEntryType.ALL_TYPES.put("phdthesis", BibtexEntryType.PHDTHESIS);
            BibtexEntryType.ALL_TYPES.put("techreport", BibtexEntryType.TECHREPORT);
            BibtexEntryType.ALL_TYPES.put("unpublished", BibtexEntryType.UNPUBLISHED);
            BibtexEntryType.ALL_TYPES.put("patent", BibtexEntryType.PATENT);
            BibtexEntryType.ALL_TYPES.put("standard", BibtexEntryType.STANDARD);
            BibtexEntryType.ALL_TYPES.put("electronic", BibtexEntryType.ELECTRONIC);
            BibtexEntryType.ALL_TYPES.put("periodical", BibtexEntryType.PERIODICAL);
            BibtexEntryType.ALL_TYPES.put("misc", BibtexEntryType.MISC);
            BibtexEntryType.ALL_TYPES.put("other", BibtexEntryType.OTHER);
            BibtexEntryType.ALL_TYPES.put("ieeetranbstctl", BibtexEntryType.IEEETRANBSTCTL);
        } else {
            BibtexEntryType.ALL_TYPES.put("article", BibLatexEntryTypes.ARTICLE);
            BibtexEntryType.ALL_TYPES.put("book", BibLatexEntryTypes.BOOK);
            BibtexEntryType.ALL_TYPES.put("inbook", BibLatexEntryTypes.INBOOK);
            BibtexEntryType.ALL_TYPES.put("bookinbook", BibLatexEntryTypes.BOOKINBOOK);
            BibtexEntryType.ALL_TYPES.put("suppbook", BibLatexEntryTypes.SUPPBOOK);
            BibtexEntryType.ALL_TYPES.put("booklet", BibLatexEntryTypes.BOOKLET);
            BibtexEntryType.ALL_TYPES.put("collection", BibLatexEntryTypes.COLLECTION);
            BibtexEntryType.ALL_TYPES.put("incollection", BibLatexEntryTypes.INCOLLECTION);
            BibtexEntryType.ALL_TYPES.put("suppcollection", BibLatexEntryTypes.SUPPCOLLECTION);
            BibtexEntryType.ALL_TYPES.put("manual", BibLatexEntryTypes.MANUAL);
            BibtexEntryType.ALL_TYPES.put("misc", BibLatexEntryTypes.MISC);
            BibtexEntryType.ALL_TYPES.put("online", BibLatexEntryTypes.ONLINE);
            BibtexEntryType.ALL_TYPES.put("patent", BibLatexEntryTypes.PATENT);
            BibtexEntryType.ALL_TYPES.put("periodical", BibLatexEntryTypes.PERIODICAL);
            BibtexEntryType.ALL_TYPES.put("suppperiodical", BibLatexEntryTypes.SUPPPERIODICAL);
            BibtexEntryType.ALL_TYPES.put("proceedings", BibLatexEntryTypes.PROCEEDINGS);
            BibtexEntryType.ALL_TYPES.put("inproceedings", BibLatexEntryTypes.INPROCEEDINGS);
            BibtexEntryType.ALL_TYPES.put("reference", BibLatexEntryTypes.REFERENCE);
            BibtexEntryType.ALL_TYPES.put("inreference", BibLatexEntryTypes.INREFERENCE);
            BibtexEntryType.ALL_TYPES.put("report", BibLatexEntryTypes.REPORT);
            BibtexEntryType.ALL_TYPES.put("set", BibLatexEntryTypes.SET);
            BibtexEntryType.ALL_TYPES.put("thesis", BibLatexEntryTypes.THESIS);
            BibtexEntryType.ALL_TYPES.put("unpublished", BibLatexEntryTypes.UNPUBLISHED);
            BibtexEntryType.ALL_TYPES.put("conference", BibLatexEntryTypes.CONFERENCE);
            BibtexEntryType.ALL_TYPES.put("electronic", BibLatexEntryTypes.ELECTRONIC);
            BibtexEntryType.ALL_TYPES.put("mastersthesis", BibLatexEntryTypes.MASTERSTHESIS);
            BibtexEntryType.ALL_TYPES.put("phdthesis", BibLatexEntryTypes.PHDTHESIS);
            BibtexEntryType.ALL_TYPES.put("techreport", BibLatexEntryTypes.TECHREPORT);
            BibtexEntryType.ALL_TYPES.put("www", BibLatexEntryTypes.WWW);
            BibtexEntryType.ALL_TYPES.put("ieeetranbstctl", BibLatexEntryTypes.IEEETRANBSTCTL);
        }

        // We need a record of the standard types, in case the user wants
        // to remove a customized version. Therefore we clone the map.
        STANDARD_TYPES = new TreeMap<String, BibtexEntryType>(BibtexEntryType.ALL_TYPES);
    }

    /**
     * This method returns the BibtexEntryType for the name of a type,
     * or null if it does not exist.
     */
    public static BibtexEntryType getType(String name) {
        //Util.pr("'"+name+"'");
        Object o = BibtexEntryType.ALL_TYPES.get(name.toLowerCase(Locale.US));
        if (o == null) {
            return null;
        } else {
            return (BibtexEntryType) o;
        }
    }

    /**
     * This method returns the standard BibtexEntryType for the
     * name of a type, or null if it does not exist.
     */
    public static BibtexEntryType getStandardType(String name) {
        //Util.pr("'"+name+"'");
        Object o = BibtexEntryType.STANDARD_TYPES.get(name.toLowerCase());
        if (o == null) {
            return null;
        } else {
            return (BibtexEntryType) o;
        }
    }

    /**
     * Removes a customized entry type from the type map. If this type
     * overrode a standard type, we reinstate the standard one.
     *
     * @param name The customized entry type to remove.
     */
    public static void removeType(String name) {
        //BibtexEntryType type = getType(name);
        String nm = name.toLowerCase();
        //System.out.println(ALL_TYPES.size());
        BibtexEntryType.ALL_TYPES.remove(nm);
        //System.out.println(ALL_TYPES.size());
        if (BibtexEntryType.STANDARD_TYPES.get(nm) != null) {
            // In this case the user has removed a customized version
            // of a standard type. We reinstate the standard type.
            BibtexEntryType.ALL_TYPES.put(nm, BibtexEntryType.STANDARD_TYPES.get(nm));
        }

    }

    /**
     * Load all custom entry types from preferences. This method is
     * called from JabRef when the program starts.
     */
    public static void loadCustomEntryTypes(JabRefPreferences prefs) {
        int number = 0;
        CustomEntryType type;
        while ((type = prefs.getCustomEntryType(number)) != null) {
            BibtexEntryType.ALL_TYPES.put(type.getName().toLowerCase(), type);
            number++;
        }
    }

    /**
     * Iterate through all entry types, and store those that are
     * custom defined to preferences. This method is called from
     * JabRefFrame when the program closes.
     */
    public static void saveCustomEntryTypes(JabRefPreferences prefs) {
        Iterator<String> i = BibtexEntryType.ALL_TYPES.keySet().iterator();
        int number = 0;
        //Vector customTypes = new Vector(10, 10);
        while (i.hasNext()) {
            Object o = BibtexEntryType.ALL_TYPES.get(i.next());
            if (o instanceof CustomEntryType) {
                // Store this entry type.
                prefs.storeCustomEntryType((CustomEntryType) o, number);
                number++;
            }
        }
        // Then, if there are more 'old' custom types defined, remove these
        // from preferences. This is necessary if the number of custom types
        // has decreased.
        prefs.purgeCustomEntryTypes(number);
    }

    /**
     * Get an array of the required fields in a form appropriate for the entry customization
     * dialog - that is, thie either-or fields together and separated by slashes.
     *
     * @return Array of the required fields in a form appropriate for the entry customization dialog.
     */
    public String[] getRequiredFieldsForCustomization() {
        return getRequiredFields();
    }
}
