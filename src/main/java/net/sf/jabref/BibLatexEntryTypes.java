/*  Copyright (C) 2003-2011 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref;

/**
 * This class defines entry types for BibLatex support.
 */

class BibLatexEntryTypes {

    /*
        "rare" fields?
            "annotator", "commentator", "titleaddon", "editora", "editorb", "editorc",
            "issuetitle", "issuesubtitle", "origlanguage", "version", "addendum"

     */

    public static final BibtexEntryType ARTICLE = new BibtexEntryType() {

        @Override
        public String getName() {
            return "Article";
        }

        @Override
        public String[] getRequiredFields() {
            return new String[] {"author", "title", "journaltitle", "date"};
        }

        @Override
        public String[] getOptionalFields() {
            return new String[] {"translator", "annotator", "commentator", "subtitle", "titleaddon",
                    "editor", "editora", "editorb", "editorc", "journalsubtitle", "issuetitle",
                    "issuesubtitle", "language", "origlanguage", "series", "volume", "number",
                    "eid", "issue", "month", "year", "pages", "version", "note", "issn",
                    "addendum", "pubstate", "doi", "eprint", "eprintclass", "eprinttype", "url",
                    "urldate"};
        }

        // TODO: number vs issue?
        @Override
        public String[] getPrimaryOptionalFields() {
            return new String[] {"subtitle", "editor", "series", "volume", "number",
                    "eid", "issue", "pages", "note", "issn",
                    "doi", "eprint", "eprintclass", "eprinttype", "url", "urldate"};
        }

        @Override
        public String describeRequiredFields() {
            return "";
        }

        @Override
        public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
            return entry.allFieldsPresent(getRequiredFields(), database);
        }
    };

    public static final BibtexEntryType BOOK = new BibtexEntryType() {

        @Override
        public String getName() {
            return "Book";
        }

        @Override
        public String[] getRequiredFields() {
            return new String[] {"author", "title", "date"};
        }

        @Override
        public String[] getOptionalFields() {
            return new String[] {"editor", "editora", "editorb", "editorc", "translator",
                    "annotator", "commentator", "introduction",
                    "foreword", "afterword", "subtitle", "titleaddon", "maintitle", "mainsubtitle",
                    "maintitleaddon", "language", "origlanguage", "volume", "part",
                    "edition", "volumes", "series", "number", "month", "year", "note", "publisher",
                    "location", "isbn", "chapter", "pages", "pagetotal", "addendum", "pubstate",
                    "doi", "eprint", "eprintclass", "eprinttype", "url", "urldate"};
        }

        @Override
        public String[] getPrimaryOptionalFields() {
            return new String[] {"editor", "subtitle", "titleaddon", "maintitle", "mainsubtitle",
                    "maintitleaddon", "volume", "edition", "publisher", "isbn", "chapter", "pages",
                    "pagetotal", "doi", "eprint", "eprintclass", "eprinttype", "url", "urldate"};
        }

        @Override
        public String describeRequiredFields() {
            return "";
        }

        @Override
        public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
            return entry.allFieldsPresent(getRequiredFields(), database);
        }
    };

    public static final BibtexEntryType INBOOK = new BibtexEntryType() {

        @Override
        public String getName() {
            return "Inbook";
        }

        @Override
        public String[] getRequiredFields() {
            return new String[] {"author", "title", "booktitle", "date"};
        }

        @Override
        public String[] getOptionalFields() {
            return new String[] {"bookauthor", "editor", "editora", "editorb", "editorc",
                    "translator", "annotator", "commentator", "introduction", "foreword", "afterword",
                    "subtitle", "titleaddon", "maintitle", "mainsubtitle", "maintitleaddon",
                    "booksubtitle", "booktitleaddon", "language", "origlanguage", "volume", "part",
                    "edition", "volumes", "series", "number", "note", "publisher", "location", "isbn",
                    "chapter", "pages", "addendum", "pubstate", "doi", "eprint", "eprintclass",
                    "eprinttype", "url", "urldate", "year"};
        }

        @Override
        public String[] getPrimaryOptionalFields() {
            return new String[] {"bookauthor", "editor", "subtitle", "titleaddon", "maintitle",
                    "mainsubtitle", "maintitleaddon", "booksubtitle", "booktitleaddon", "volume",
                    "edition", "publisher", "isbn", "chapter", "pages", "doi", "eprint", "eprintclass",
                    "eprinttype", "url", "urldate"};
        }

        @Override
        public String describeRequiredFields() {
            return "";
        }

        @Override
        public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
            return entry.allFieldsPresent(getRequiredFields(), database);
        }
    };

    public static final BibtexEntryType BOOKINBOOK = new BibtexEntryType() {

        @Override
        public String getName() {
            return "Bookinbook";
        }

        // Same fields as "INBOOK" according to Biblatex 1.0: 
        @Override
        public String[] getRequiredFields() {
            return BibLatexEntryTypes.INBOOK.getRequiredFields();
        }

        @Override
        public String[] getOptionalFields() {
            return BibLatexEntryTypes.INBOOK.getOptionalFields();
        }

        @Override
        public String[] getPrimaryOptionalFields() {
            return BibLatexEntryTypes.INBOOK.getPrimaryOptionalFields();
        }

        @Override
        public String describeRequiredFields() {
            return "";
        }

        @Override
        public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
            return entry.allFieldsPresent(getRequiredFields(), database);
        }
    };

    public static final BibtexEntryType SUPPBOOK = new BibtexEntryType() {

        @Override
        public String getName() {
            return "Suppbook";
        }

        // Same fields as "INBOOK" according to Biblatex 1.0: 
        @Override
        public String[] getRequiredFields() {
            return BibLatexEntryTypes.INBOOK.getRequiredFields();
        }

        @Override
        public String[] getOptionalFields() {
            return BibLatexEntryTypes.INBOOK.getOptionalFields();
        }

        @Override
        public String[] getPrimaryOptionalFields() {
            return BibLatexEntryTypes.INBOOK.getPrimaryOptionalFields();
        }

        @Override
        public String describeRequiredFields() {
            return "";
        }

        @Override
        public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
            return entry.allFieldsPresent(getRequiredFields(), database);
        }
    };

    public static final BibtexEntryType BOOKLET = new BibtexEntryType() {

        @Override
        public String getName() {
            return "Booklet";
        }

        @Override
        public String[] getRequiredFields() {
            return new String[] {"author", "editor", "title", "date"};
        }

        @Override
        public String[] getOptionalFields() {
            return new String[] {"subtitle", "titleaddon", "language", "howpublished", "type", "note",
                    "location", "chapter", "year", "pages", "pagetotal", "addendum", "pubstate", "doi", "eprint",
                    "eprintclass", "eprinttype", "url", "urldate"};
        }

        @Override
        public String[] getPrimaryOptionalFields() {
            return new String[] {"subtitle", "titleaddon", "howpublished", "chapter", "pages", "doi", "eprint",
                    "eprintclass", "eprinttype", "url", "urldate"};
        }

        @Override
        public String describeRequiredFields() {
            return "";
        }

        @Override
        public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
            return entry.allFieldsPresent(getRequiredFields(), database);
        }
    };

    public static final BibtexEntryType COLLECTION = new BibtexEntryType() {

        @Override
        public String getName() {
            return "Collection";
        }

        @Override
        public String[] getRequiredFields() {
            return new String[] {"editor", "title", "date"};
        }

        @Override
        public String[] getOptionalFields() {
            return new String[] {"editora", "editorb", "editorc", "translator", "annotator",
                    "commentator", "introduction", "foreword", "afterword", "subtitle", "titleaddon",
                    "maintitle", "mainsubtitle", "maintitleaddon", "language", "origlanguage", "volume",
                    "part", "edition", "volumes", "series", "number", "note", "publisher", "location", "isbn",
                    "chapter", "pages", "pagetotal", "addendum", "pubstate", "doi", "eprint", "eprintclass",
                    "eprinttype", "url", "urldate", "year"};
        }

        @Override
        public String[] getPrimaryOptionalFields() {
            return new String[] {"translator", "subtitle", "titleaddon", "maintitle",
                    "mainsubtitle", "maintitleaddon", "volume",
                    "edition", "publisher", "isbn", "chapter", "pages", "doi", "eprint", "eprintclass",
                    "eprinttype", "url", "urldate"};
        }

        @Override
        public String describeRequiredFields() {
            return "";
        }

        @Override
        public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
            return entry.allFieldsPresent(getRequiredFields(), database);
        }
    };

    public static final BibtexEntryType INCOLLECTION = new BibtexEntryType() {

        @Override
        public String getName() {
            return "Incollection";
        }

        @Override
        public String[] getRequiredFields() {
            return new String[] {"author", "editor", "title", "booktitle", "date"};
        }

        @Override
        public String[] getOptionalFields() {
            return new String[] {"editora", "editorb", "editorc", "translator", "annotator",
                    "commentator", "introduction", "foreword", "afterword", "subtitle", "titleaddon",
                    "maintitle", "mainsubtitle", "maintitleaddon", "booksubtitle", "booktitleaddon",
                    "language", "origlanguage", "volume", "part", "edition", "volumes", "series", "number",
                    "note", "publisher", "location", "isbn", "chapter", "pages", "addendum", "pubstate", "doi",
                    "eprint", "eprintclass", "eprinttype", "url", "urldate", "year"};
        }

        @Override
        public String[] getPrimaryOptionalFields() {
            return new String[] {"translator", "subtitle", "titleaddon", "maintitle",
                    "mainsubtitle", "maintitleaddon", "booksubtitle", "booktitleaddon", "volume",
                    "edition", "publisher", "isbn", "chapter", "pages", "doi", "eprint", "eprintclass",
                    "eprinttype", "url", "urldate"};
        }

        @Override
        public String describeRequiredFields() {
            return "";
        }

        @Override
        public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
            return entry.allFieldsPresent(getRequiredFields(), database);
        }
    };

    public static final BibtexEntryType SUPPCOLLECTION = new BibtexEntryType() {

        @Override
        public String getName() {
            return "Suppcollection";
        }

        // Treated as alias of "INCOLLECTION" according to Biblatex 1.0: 
        @Override
        public String[] getRequiredFields() {
            return BibLatexEntryTypes.INCOLLECTION.getRequiredFields();
        }

        @Override
        public String[] getOptionalFields() {
            return BibLatexEntryTypes.INCOLLECTION.getOptionalFields();
        }

        @Override
        public String[] getPrimaryOptionalFields() {
            return BibLatexEntryTypes.INCOLLECTION.getPrimaryOptionalFields();
        }

        @Override
        public String describeRequiredFields() {
            return "";
        }

        @Override
        public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
            return entry.allFieldsPresent(getRequiredFields(), database);
        }
    };

    public static final BibtexEntryType MANUAL = new BibtexEntryType() {

        @Override
        public String getName() {
            return "Manual";
        }

        @Override
        public String[] getRequiredFields() {
            return new String[] {"author", "editor", "title", "date"};
        }

        @Override
        public String[] getOptionalFields() {
            return new String[] {"subtitle", "titleaddon", "language", "edition", "type", "series",
                    "number", "version", "note", "organization", "publisher", "location", "isbn", "chapter",
                    "pages", "pagetotal", "addendum", "pubstate", "doi", "eprint", "eprintclass",
                    "eprinttype", "url", "urldate", "year"};
        }

        @Override
        public String[] getPrimaryOptionalFields() {
            return new String[] {"subtitle", "titleaddon", "edition", "publisher", "isbn", "chapter",
                    "pages", "doi", "eprint", "eprintclass",
                    "eprinttype", "url", "urldate"};
        }

        @Override
        public String describeRequiredFields() {
            return "";
        }

        @Override
        public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
            return entry.allFieldsPresent(getRequiredFields(), database);
        }
    };

    public static final BibtexEntryType MISC = new BibtexEntryType() {

        @Override
        public String getName() {
            return "Misc";
        }

        @Override
        public String[] getRequiredFields() {
            return new String[] {"author", "editor", "title", "date"};
        }

        @Override
        public String[] getOptionalFields() {
            return new String[] {"subtitle", "titleaddon", "language", "howpublished", "type",
                    "version", "note", "organization", "location", "month", "year", "addendum",
                    "pubstate", "doi", "eprint", "eprintclass", "eprinttype", "url", "urldate"};
        }

        @Override
        public String[] getPrimaryOptionalFields() {
            return new String[] {"subtitle", "titleaddon", "howpublished", "location", "doi", "eprint", "eprintclass",
                    "eprinttype", "url", "urldate"};
        }

        @Override
        public String describeRequiredFields() {
            return "";
        }

        @Override
        public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
            return entry.allFieldsPresent(getRequiredFields(), database);
        }
    };

    public static final BibtexEntryType ONLINE = new BibtexEntryType() {

        @Override
        public String getName() {
            return "Online";
        }

        @Override
        public String[] getRequiredFields() {
            return new String[] {"author", "editor", "title", "date", "url"};
        }

        @Override
        public String[] getOptionalFields() {
            return new String[] {"subtitle", "titleaddon", "language", "version", "note",
                    "organization", "month", "year", "addendum", "pubstate", "urldate"};
        }

        @Override
        public String[] getPrimaryOptionalFields() {
            return new String[] {"subtitle", "titleaddon", "note", "organization", "urldate"};
        }

        @Override
        public String describeRequiredFields() {
            return "";
        }

        @Override
        public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
            return entry.allFieldsPresent(getRequiredFields(), database);
        }
    };

    public static final BibtexEntryType PATENT = new BibtexEntryType() {

        @Override
        public String getName() {
            return "Patent";
        }

        @Override
        public String[] getRequiredFields() {
            return new String[] {"author", "title", "number", "date"};
        }

        @Override
        public String[] getOptionalFields() {
            return new String[] {"holder", "subtitle", "titleaddon", "type", "version", "location", "note",
                    "month", "year", "addendum", "pubstate", "doi", "eprint", "eprintclass",
                    "eprinttype", "url", "urldate"};
        }

        @Override
        public String[] getPrimaryOptionalFields() {
            return new String[] {"holder", "subtitle", "titleaddon", "doi", "eprint", "eprintclass",
                    "eprinttype", "url", "urldate"};
        }

        @Override
        public String describeRequiredFields() {
            return "";
        }

        @Override
        public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
            return entry.allFieldsPresent(getRequiredFields(), database);
        }
    };

    public static final BibtexEntryType PERIODICAL = new BibtexEntryType() {

        @Override
        public String getName() {
            return "Periodical";
        }

        @Override
        public String[] getRequiredFields() {
            return new String[] {"editor", "title", "date"};
        }

        @Override
        public String[] getOptionalFields() {
            return new String[] {"editora", "editorb", "editorc", "subtitle", "issuetitle",
                    "issuesubtitle", "language", "series", "volume", "number", "issue", "month", "year",
                    "note", "issn", "addendum", "pubstate", "doi", "eprint", "eprintclass", "eprinttype", "url",
                    "urldate"};
        }

        @Override
        public String[] getPrimaryOptionalFields() {
            return new String[] {"subtitle", "issuetitle", "issuesubtitle", "issn", "doi", "eprint", "eprintclass",
                    "eprinttype", "url", "urldate"};
        }

        @Override
        public String describeRequiredFields() {
            return "";
        }

        @Override
        public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
            return entry.allFieldsPresent(getRequiredFields(), database);
        }
    };

    public static final BibtexEntryType SUPPPERIODICAL = new BibtexEntryType() {

        @Override
        public String getName() {
            return "Suppperiodical";
        }

        // Treated as alias of "ARTICLE" according to Biblatex 1.0: 
        @Override
        public String[] getRequiredFields() {
            return BibLatexEntryTypes.ARTICLE.getRequiredFields();
        }

        @Override
        public String[] getOptionalFields() {
            return BibLatexEntryTypes.ARTICLE.getOptionalFields();
        }

        @Override
        public String[] getPrimaryOptionalFields() {
            return BibLatexEntryTypes.ARTICLE.getPrimaryOptionalFields();
        }

        @Override
        public String describeRequiredFields() {
            return "";
        }

        @Override
        public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
            return entry.allFieldsPresent(getRequiredFields(), database);
        }
    };

    public static final BibtexEntryType PROCEEDINGS = new BibtexEntryType() {

        @Override
        public String getName() {
            return "Proceedings";
        }

        @Override
        public String[] getRequiredFields() {
            return new String[] {"editor", "title", "date"};
        }

        @Override
        public String[] getOptionalFields() {
            return new String[] {"subtitle", "titleaddon", "maintitle", "mainsubtitle",
                    "maintitleaddon", "eventtitle", "eventdate", "venue", "language", "volume", "part",
                    "volumes", "series", "number", "note", "organization", "publisher", "location", "month",
                    "year", "isbn", "chapter", "pages", "pagetotal", "addendum", "pubstate", "doi", "eprint",
                    "eprintclass", "eprinttype", "url", "urldate"};
        }

        @Override
        public String[] getPrimaryOptionalFields() {
            return new String[] {"subtitle", "titleaddon", "maintitle", "mainsubtitle",
                    "maintitleaddon", "eventtitle", "volume", "publisher", "isbn", "chapter", "pages",
                    "pagetotal", "doi", "eprint", "eprintclass", "eprinttype", "url", "urldate"};
        }

        @Override
        public String describeRequiredFields() {
            return "";
        }

        @Override
        public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
            return entry.allFieldsPresent(getRequiredFields(), database);
        }
    };

    public static final BibtexEntryType INPROCEEDINGS = new BibtexEntryType() {

        @Override
        public String getName() {
            return "Inproceedings";
        }

        @Override
        public String[] getRequiredFields() {
            return new String[] {"author", "editor", "title", "booktitle", "date"};
        }

        @Override
        public String[] getOptionalFields() {
            return new String[] {"subtitle", "titleaddon", "maintitle", "mainsubtitle",
                    "maintitleaddon", "booksubtitle", "booktitleaddon", "eventtitle", "eventdate", "venue",
                    "language", "volume", "part", "volumes", "series", "number", "note", "organization",
                    "publisher", "location", "month", "year", "isbn", "chapter", "pages", "addendum",
                    "pubstate", "doi", "eprint", "eprintclass", "eprinttype", "url", "urldate"};
        }

        @Override
        public String[] getPrimaryOptionalFields() {
            return new String[] {"subtitle", "titleaddon", "maintitle", "mainsubtitle",
                    "maintitleaddon", "booksubtitle", "booktitleaddon", "eventtitle", "volume",
                    "publisher", "isbn", "chapter", "pages",
                    "doi", "eprint", "eprintclass", "eprinttype", "url", "urldate"};
        }

        @Override
        public String describeRequiredFields() {
            return "";
        }

        @Override
        public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
            return entry.allFieldsPresent(getRequiredFields(), database);
        }
    };

    public static final BibtexEntryType REFERENCE = new BibtexEntryType() {

        @Override
        public String getName() {
            return "Reference";
        }

        // Treated as alias of "COLLECTION" according to Biblatex 1.0: 
        @Override
        public String[] getRequiredFields() {
            return BibLatexEntryTypes.COLLECTION.getRequiredFields();
        }

        @Override
        public String[] getOptionalFields() {
            return BibLatexEntryTypes.COLLECTION.getOptionalFields();
        }

        @Override
        public String[] getPrimaryOptionalFields() {
            return BibLatexEntryTypes.COLLECTION.getPrimaryOptionalFields();
        }

        @Override
        public String describeRequiredFields() {
            return "";
        }

        @Override
        public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
            return entry.allFieldsPresent(getRequiredFields(), database);
        }
    };

    public static final BibtexEntryType INREFERENCE = new BibtexEntryType() {

        @Override
        public String getName() {
            return "Inreference";
        }

        // Treated as alias of "INCOLLECTION" according to Biblatex 1.0: 
        @Override
        public String[] getRequiredFields() {
            return BibLatexEntryTypes.INCOLLECTION.getRequiredFields();
        }

        @Override
        public String[] getOptionalFields() {
            return BibLatexEntryTypes.INCOLLECTION.getOptionalFields();
        }

        @Override
        public String[] getPrimaryOptionalFields() {
            return BibLatexEntryTypes.INCOLLECTION.getPrimaryOptionalFields();
        }

        @Override
        public String describeRequiredFields() {
            return "";
        }

        @Override
        public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
            return entry.allFieldsPresent(getRequiredFields(), database);
        }
    };

    public static final BibtexEntryType REPORT = new BibtexEntryType() {

        @Override
        public String getName() {
            return "Report";
        }

        @Override
        public String[] getRequiredFields() {
            return new String[] {"author", "title", "type", "institution", "date"};
        }

        @Override
        public String[] getOptionalFields() {
            return new String[] {"subtitle", "titleaddon", "language", "number", "version", "note",
                    "location", "month", "year", "isrn", "chapter", "pages", "pagetotal", "addendum", "pubstate", "doi",
                    "eprint", "eprintclass", "eprinttype", "url", "urldate"};
        }

        @Override
        public String[] getPrimaryOptionalFields() {
            return new String[] {"subtitle", "titleaddon", "number", "isrn", "chapter", "pages", "pagetotal", "doi",
                    "eprint", "eprintclass", "eprinttype", "url", "urldate"};
        }

        @Override
        public String describeRequiredFields() {
            return "";
        }

        @Override
        public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
            return entry.allFieldsPresent(getRequiredFields(), database);
        }
    };

    public static final BibtexEntryType SET = new BibtexEntryType() {

        @Override
        public String getName() {
            return "Set";
        }

        @Override
        public String[] getRequiredFields() {
            return new String[] {"entryset", "crossref"};
        }

        // These are all the standard entry fields, custom fields and field aliases not included:
        /* Optional fields left out since they take up too much space - I think the set type is mainly supposed
           to fall back on content from the entries contained in the set, so only the required fields are included.*/
        @Override
        public String[] getOptionalFields() {
            return null;
            /*return new String[] {"abstract", "addendum", "afterword", "annotation", "annotator", "author", "authortype",
            	 "bookauthor", "bookpagination", "booksubtitle", "booktitle", "booktitleaddon",
            	 "chapter", "commentator", "date", "doi", "edition", "editor", "editora", "editorb",
            	 "editorc", "editortype", "editoratype", "editorbtype", "editorctype", "eid", "eprint",
            	 "eprintclass", "eprinttype", "eventdate", "eventtitle", "file", "foreword", "holder",
            	 "howpublished", "indextitle", "insitution", "introduction", "isan", "isbn", "ismn",
            	 "isrn", "issn", "issue", "issuesubtitle", "issuetitle", "iswc", "journalsubtitle",
            	 "journaltitle", "label", "language", "library", "location", "mainsubtitle",
            	 "maintitle", "maintitleaddon", "month", "nameaddon", "note", "number", "organization",
            	 "origdate", "origlanguage", "origlocation", "origpublisher", "origtitle", "pages",
            	 "pagetotal", "pagination", "part", "publisher", "pubstate", "reprinttitle", "series",
            	 "shortauthor", "shorteditor", "shorthand", "shorthandintro", "shortjournal",
            	 "shortseries", "shorttitle", "subtitle", "title", "titleaddon", "translator", "type",
            	 "url", "urldate", "venue", "version", "volume", "volumes", "year", "crossref",
            	 "entryset", "entrysubtype", "execute", "gender", "hyphenation", "indexsorttitle",
            	 "keywords", "options", "presort", "sortkey", "sortname", "sorttitle", "sortyear",
            	 "xref"};*/
        }

        // These are just appr. the first half of the above fields:
        @Override
        public String[] getPrimaryOptionalFields() {
            return null;
            /*return new String[] {"abstract", "addendum", "afterword", "annotation", "annotator", "author", "authortype",
            	 "bookauthor", "bookpagination", "booksubtitle", "booktitle", "booktitleaddon",
            	 "chapter", "commentator", "date", "doi", "edition", "editor", "editora", "editorb",
            	 "editorc", "editortype", "editoratype", "editorbtype", "editorctype", "eid", "eprint",
            	 "eprintclass", "eprinttype", "eventdate", "eventtitle", "file", "foreword", "holder",
            	 "howpublished", "indextitle", "insitution", "introduction", "isan", "isbn", "ismn",
            	 "isrn", "issn", "issue", "issuesubtitle", "issuetitle", "iswc", "journalsubtitle",
            	 "journaltitle", "label", "language", "library", "location", "mainsubtitle",
            	 "maintitle", "maintitleaddon", "month", "nameaddon"};*/
        }

        @Override
        public String describeRequiredFields() {
            return "";
        }

        @Override
        public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
            return entry.allFieldsPresent(getRequiredFields(), database);
        }
    };

    public static final BibtexEntryType THESIS = new BibtexEntryType() {

        @Override
        public String getName() {
            return "Thesis";
        }

        @Override
        public String[] getRequiredFields() {
            return new String[] {"author", "title", "type", "institution", "date"};
        }

        @Override
        public String[] getOptionalFields() {
            return new String[] {"subtitle", "titleaddon", "language", "note", "location", "month", "year",
                    "chapter", "pages", "pagetotal", "addendum", "pubstate", "doi", "eprint", "eprintclass",
                    "eprinttype", "url", "urldate"};
        }

        @Override
        public String[] getPrimaryOptionalFields() {
            return new String[] {"subtitle", "titleaddon", "chapter", "pages", "pagetotal", "doi", "eprint",
                    "eprintclass", "eprinttype", "url", "urldate"};
        }

        @Override
        public String describeRequiredFields() {
            return "";
        }

        @Override
        public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
            return entry.allFieldsPresent(getRequiredFields(), database);
        }
    };

    public static final BibtexEntryType UNPUBLISHED = new BibtexEntryType() {

        @Override
        public String getName() {
            return "Unpublished";
        }

        @Override
        public String[] getRequiredFields() {
            return new String[] {"author", "title", "date"};
        }

        @Override
        public String[] getOptionalFields() {
            return new String[] {"subtitle", "titleaddon", "language", "howpublished", "note",
                    "location", "month", "year", "addendum", "pubstate", "url", "urldate"};
        }

        @Override
        public String[] getPrimaryOptionalFields() {
            return new String[] {"subtitle", "titleaddon", "howpublished", "pubstate", "url", "urldate"};
        }

        @Override
        public String describeRequiredFields() {
            return "";
        }

        @Override
        public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
            return entry.allFieldsPresent(getRequiredFields(), database);
        }
    };

    // === Type aliases: ===

    public static final BibtexEntryType CONFERENCE = new BibtexEntryType() {

        @Override
        public String getName() {
            return "Conference";
        }

        // Treated as alias of "INPROCEEDINGS" according to Biblatex 1.0: 
        @Override
        public String[] getRequiredFields() {
            return BibLatexEntryTypes.INPROCEEDINGS.getRequiredFields();
        }

        @Override
        public String[] getOptionalFields() {
            return BibLatexEntryTypes.INPROCEEDINGS.getOptionalFields();
        }

        @Override
        public String[] getPrimaryOptionalFields() {
            return BibLatexEntryTypes.INPROCEEDINGS.getPrimaryOptionalFields();
        }

        @Override
        public String describeRequiredFields() {
            return "";
        }

        @Override
        public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
            return entry.allFieldsPresent(getRequiredFields(), database);
        }
    };

    public static final BibtexEntryType ELECTRONIC = new BibtexEntryType() {

        @Override
        public String getName() {
            return "Electronic";
        }

        // Treated as alias of "ONLINE" according to Biblatex 1.0: 
        @Override
        public String[] getRequiredFields() {
            return BibLatexEntryTypes.ONLINE.getRequiredFields();
        }

        @Override
        public String[] getOptionalFields() {
            return BibLatexEntryTypes.ONLINE.getOptionalFields();
        }

        @Override
        public String[] getPrimaryOptionalFields() {
            return BibLatexEntryTypes.ONLINE.getPrimaryOptionalFields();
        }

        @Override
        public String describeRequiredFields() {
            return "";
        }

        @Override
        public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
            return entry.allFieldsPresent(getRequiredFields(), database);
        }
    };

    public static final BibtexEntryType MASTERSTHESIS = new BibtexEntryType() {

        @Override
        public String getName() {
            return "Mastersthesis";
        }

        // Treated as alias of "THESIS", except "type" field is optional
        @Override
        public String[] getRequiredFields() {
            return new String[] {"author", "title", "institution", "date"};
        }

        @Override
        public String[] getOptionalFields() {
            return new String[] {"subtitle", "titleaddon", "type", "language", "note", "location", "month", "year",
                    "chapter", "pages", "pagetotal", "addendum", "pubstate", "doi", "eprint", "eprintclass",
                    "eprinttype", "url", "urldate"};
        }

        @Override
        public String[] getPrimaryOptionalFields() {
            return new String[] {"subtitle", "titleaddon", "type", "chapter", "pages", "pagetotal", "doi", "eprint",
                    "eprintclass", "eprinttype", "url", "urldate"};
        }

        @Override
        public String describeRequiredFields() {
            return "";
        }

        @Override
        public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
            return entry.allFieldsPresent(getRequiredFields(), database);
        }
    };

    public static final BibtexEntryType PHDTHESIS = new BibtexEntryType() {

        @Override
        public String getName() {
            return "Phdthesis";
        }

        // Treated as alias of "THESIS", except "type" field is optional
        @Override
        public String[] getRequiredFields() {
            return new String[] {"author", "title", "institution", "date"};
        }

        @Override
        public String[] getOptionalFields() {
            return new String[] {"subtitle", "titleaddon", "type", "language", "note", "location", "month", "year",
                    "chapter", "pages", "pagetotal", "addendum", "pubstate", "doi", "eprint", "eprintclass",
                    "eprinttype", "url", "urldate"};
        }

        @Override
        public String[] getPrimaryOptionalFields() {
            return new String[] {"subtitle", "titleaddon", "type", "chapter", "pages", "pagetotal", "doi", "eprint",
                    "eprintclass", "eprinttype", "url", "urldate"};
        }

        @Override
        public String describeRequiredFields() {
            return "";
        }

        @Override
        public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
            return entry.allFieldsPresent(getRequiredFields(), database);
        }
    };

    public static final BibtexEntryType TECHREPORT = new BibtexEntryType() {

        @Override
        public String getName() {
            return "Techreport";
        }

        // Treated as alias of "REPORT", except "type" field is optional
        @Override
        public String[] getRequiredFields() {
            return new String[] {"author", "title", "institution", "date"};
        }

        @Override
        public String[] getOptionalFields() {
            return new String[] {"subtitle", "titleaddon", "type", "language", "number", "version", "note",
                    "location", "month", "year", "isrn", "chapter", "pages", "pagetotal", "addendum", "pubstate",
                    "doi", "eprint", "eprintclass", "eprinttype", "url", "urldate"};
        }

        @Override
        public String[] getPrimaryOptionalFields() {
            return new String[] {"subtitle", "titleaddon", "type", "number", "isrn", "chapter", "pages", "pagetotal",
                    "doi", "eprint", "eprintclass", "eprinttype", "url", "urldate"};
        }

        @Override
        public String describeRequiredFields() {
            return "";
        }

        @Override
        public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
            return entry.allFieldsPresent(getRequiredFields(), database);
        }
    };

    public static final BibtexEntryType WWW = new BibtexEntryType() {

        @Override
        public String getName() {
            return "Www";
        }

        // Treated as alias of "ONLINE" according to Biblatex 1.0: 
        @Override
        public String[] getRequiredFields() {
            return BibLatexEntryTypes.ONLINE.getRequiredFields();
        }

        @Override
        public String[] getOptionalFields() {
            return BibLatexEntryTypes.ONLINE.getOptionalFields();
        }

        @Override
        public String[] getPrimaryOptionalFields() {
            return BibLatexEntryTypes.ONLINE.getPrimaryOptionalFields();
        }

        @Override
        public String describeRequiredFields() {
            return "";
        }

        @Override
        public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
            return entry.allFieldsPresent(getRequiredFields(), database);
        }
    };

    /**
     * This type is used for IEEEtran.bst to control various 
     * be repeated or not. Not a very elegant solution, but it works...
     */
    public static final BibtexEntryType IEEETRANBSTCTL = new BibtexEntryType() {

        @Override
        public String getName()
        {
            return "IEEEtranBSTCTL";
        }

        @Override
        public String[] getOptionalFields()
        {
            return new String[] {
                    "ctluse_article_number", "ctluse_paper", "ctluse_forced_etal",
                    "ctlmax_names_forced_etal", "ctlnames_show_etal", "ctluse_alt_spacing",
                    "ctlalt_stretch_factor", "ctldash_repeated_names", "ctlname_format_string",
                    "ctlname_latex_cmd", "ctlname_url_prefix"
            };
        }

        @Override
        public String[] getRequiredFields()
        {
            return null;
        }

        @Override
        public String describeRequiredFields()
        {
            return "None";
        }

        @Override
        public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database)
        {
            return true;
        }
    };

    // Unsupported types and custom types left out

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
        public String[] getPrimaryOptionalFields() {
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
