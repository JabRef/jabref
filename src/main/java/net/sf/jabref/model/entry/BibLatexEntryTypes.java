/*  Copyright (C) 2003-2015 JabRef contributors.
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
package net.sf.jabref.model.entry;

import net.sf.jabref.model.database.BibtexDatabase;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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

        private List<String> primaryOptionalFields = Collections.unmodifiableList(Arrays.asList(new String[]{"subtitle",
                "editor", "series", "volume", "number", "eid", "issue", "pages", "note", "issn",
                "doi", "eprint", "eprintclass", "eprinttype", "url", "urldate"}));

        {
            addAllOptional("translator", "annotator", "commentator", "subtitle", "titleaddon",
                    "editor", "editora", "editorb", "editorc", "journalsubtitle", "issuetitle",
                    "issuesubtitle", "language", "origlanguage", "series", "volume", "number",
                    "eid", "issue", "month", "year", "pages", "version", "note", "issn",
                    "addendum", "pubstate", "doi", "eprint", "eprintclass", "eprinttype", "url",
                    "urldate");
            addAllRequired("author", "title", "journaltitle", "date");
        }

        @Override
        public String getName() {
            return "Article";
        }

        // TODO: number vs issue?
        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }

        @Override
        public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
            return entry.allFieldsPresent(getRequiredFields(), database);
        }
    };

    public static final BibtexEntryType BOOK = new BibtexEntryType() {

        private List<String> primaryOptionalFields = Collections.unmodifiableList(Arrays.asList(new String[]{"editor",
                "subtitle", "titleaddon", "maintitle", "mainsubtitle", "maintitleaddon", "volume", "edition",
                "publisher", "isbn", "chapter", "pages", "pagetotal", "doi", "eprint", "eprintclass", "eprinttype",
                "url", "urldate"}));

        {
            addAllOptional("editor", "editora", "editorb", "editorc", "translator",
                    "annotator", "commentator", "introduction",
                    "foreword", "afterword", "subtitle", "titleaddon", "maintitle", "mainsubtitle",
                    "maintitleaddon", "language", "origlanguage", "volume", "part",
                    "edition", "volumes", "series", "number", "month", "year", "note", "publisher",
                    "location", "isbn", "chapter", "pages", "pagetotal", "addendum", "pubstate",
                    "doi", "eprint", "eprintclass", "eprinttype", "url", "urldate");
            addAllRequired("author", "title", "date");
        }

        @Override
        public String getName() {
            return "Book";
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }

        @Override
        public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
            return entry.allFieldsPresent(getRequiredFields(), database);
        }
    };

    public static final BibtexEntryType INBOOK = new BibtexEntryType() {

        private List<String> primaryOptionalFields = Collections.unmodifiableList(Arrays.asList(new String[]{
                "bookauthor", "editor", "subtitle", "titleaddon", "maintitle",
                "mainsubtitle", "maintitleaddon", "booksubtitle", "booktitleaddon", "volume",
                "edition", "publisher", "isbn", "chapter", "pages", "doi", "eprint", "eprintclass",
                "eprinttype", "url", "urldate"}));

        {
            addAllOptional("bookauthor", "editor", "editora", "editorb", "editorc",
                    "translator", "annotator", "commentator", "introduction", "foreword", "afterword",
                    "subtitle", "titleaddon", "maintitle", "mainsubtitle", "maintitleaddon",
                    "booksubtitle", "booktitleaddon", "language", "origlanguage", "volume", "part",
                    "edition", "volumes", "series", "number", "note", "publisher", "location", "isbn",
                    "chapter", "pages", "addendum", "pubstate", "doi", "eprint", "eprintclass",
                    "eprinttype", "url", "urldate", "year");
            addAllRequired("author", "title", "booktitle", "date");
        }

        @Override
        public String getName() {
            return "InBook";
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }

        @Override
        public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
            return entry.allFieldsPresent(getRequiredFields(), database);
        }
    };

    public static final BibtexEntryType BOOKINBOOK = new BibtexEntryType() {

        @Override
        public String getName() {
            return "BookInBook";
        }

        // Same fields as "INBOOK" according to Biblatex 1.0: 
        @Override
        public List<String> getRequiredFields() {
            return BibLatexEntryTypes.INBOOK.getRequiredFields();
        }

        @Override
        public List<String> getOptionalFields() {
            return BibLatexEntryTypes.INBOOK.getOptionalFields();
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return BibLatexEntryTypes.INBOOK.getPrimaryOptionalFields();
        }

        @Override
        public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
            return entry.allFieldsPresent(getRequiredFields(), database);
        }
    };

    public static final BibtexEntryType SUPPBOOK = new BibtexEntryType() {

        @Override
        public String getName() {
            return "SuppBook";
        }

        // Same fields as "INBOOK" according to Biblatex 1.0: 
        @Override
        public List<String> getRequiredFields() {
            return BibLatexEntryTypes.INBOOK.getRequiredFields();
        }

        @Override
        public List<String> getOptionalFields() {
            return BibLatexEntryTypes.INBOOK.getOptionalFields();
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return BibLatexEntryTypes.INBOOK.getPrimaryOptionalFields();
        }

        @Override
        public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
            return entry.allFieldsPresent(getRequiredFields(), database);
        }
    };

    public static final BibtexEntryType BOOKLET = new BibtexEntryType() {

        private List<String> primaryOptionalFields = Collections.unmodifiableList(Arrays.asList(new String[]{"subtitle",
                "titleaddon", "howpublished", "chapter", "pages", "doi", "eprint",
                "eprintclass", "eprinttype", "url", "urldate"}));

        {
            addAllRequired("author", "editor", "title", "date");
            addAllOptional("subtitle", "titleaddon", "language", "howpublished", "type", "note",
                    "location", "chapter", "year", "pages", "pagetotal", "addendum", "pubstate", "doi", "eprint",
                    "eprintclass", "eprinttype", "url", "urldate");
        }

        @Override
        public String getName() {
            return "Booklet";
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }

        @Override
        public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
            return entry.allFieldsPresent(getRequiredFields(), database);
        }
    };

    public static final BibtexEntryType COLLECTION = new BibtexEntryType() {

        private List<String> primaryOptionalFields = Collections.unmodifiableList(Arrays.asList(new String[]{
                "translator", "subtitle", "titleaddon", "maintitle",
                "mainsubtitle", "maintitleaddon", "volume",
                "edition", "publisher", "isbn", "chapter", "pages", "doi", "eprint", "eprintclass",
                "eprinttype", "url", "urldate"}));

        {
            addAllRequired("editor", "title", "date");
            addAllOptional("editora", "editorb", "editorc", "translator", "annotator",
                    "commentator", "introduction", "foreword", "afterword", "subtitle", "titleaddon",
                    "maintitle", "mainsubtitle", "maintitleaddon", "language", "origlanguage", "volume",
                    "part", "edition", "volumes", "series", "number", "note", "publisher", "location", "isbn",
                    "chapter", "pages", "pagetotal", "addendum", "pubstate", "doi", "eprint", "eprintclass",
                    "eprinttype", "url", "urldate", "year");
        }

        @Override
        public String getName() {
            return "Collection";
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }

        @Override
        public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
            return entry.allFieldsPresent(getRequiredFields(), database);
        }
    };

    public static final BibtexEntryType INCOLLECTION = new BibtexEntryType() {

        private List<String> primaryOptionalFields = Collections.unmodifiableList(Arrays.asList(new String[]{
                "translator", "subtitle", "titleaddon", "maintitle",
                "mainsubtitle", "maintitleaddon", "booksubtitle", "booktitleaddon", "volume",
                "edition", "publisher", "isbn", "chapter", "pages", "doi", "eprint", "eprintclass",
                "eprinttype", "url", "urldate"}));

        {
            addAllRequired("author", "editor", "title", "booktitle", "date");
            addAllOptional("editora", "editorb", "editorc", "translator", "annotator",
                    "commentator", "introduction", "foreword", "afterword", "subtitle", "titleaddon",
                    "maintitle", "mainsubtitle", "maintitleaddon", "booksubtitle", "booktitleaddon",
                    "language", "origlanguage", "volume", "part", "edition", "volumes", "series", "number",
                    "note", "publisher", "location", "isbn", "chapter", "pages", "addendum", "pubstate", "doi",
                    "eprint", "eprintclass", "eprinttype", "url", "urldate", "year");
        }

        @Override
        public String getName() {
            return "InCollection";
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }

        @Override
        public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
            return entry.allFieldsPresent(getRequiredFields(), database);
        }
    };

    public static final BibtexEntryType SUPPCOLLECTION = new BibtexEntryType() {

        @Override
        public String getName() {
            return "SuppCollection";
        }

        // Treated as alias of "INCOLLECTION" according to Biblatex 1.0: 
        @Override
        public List<String> getRequiredFields() {
            return BibLatexEntryTypes.INCOLLECTION.getRequiredFields();
        }

        @Override
        public List<String> getOptionalFields() {
            return BibLatexEntryTypes.INCOLLECTION.getOptionalFields();
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return BibLatexEntryTypes.INCOLLECTION.getPrimaryOptionalFields();
        }

        @Override
        public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
            return entry.allFieldsPresent(getRequiredFields(), database);
        }
    };

    public static final BibtexEntryType MANUAL = new BibtexEntryType() {

        private List<String> primaryOptionalFields = Collections.unmodifiableList(Arrays.asList(new String[]{"subtitle",
                "titleaddon", "edition", "publisher", "isbn", "chapter",
                "pages", "doi", "eprint", "eprintclass",
                "eprinttype", "url", "urldate"}));

        {
            addAllRequired("author", "editor", "title", "date");
            addAllOptional("subtitle", "titleaddon", "language", "edition", "type", "series",
                    "number", "version", "note", "organization", "publisher", "location", "isbn", "chapter",
                    "pages", "pagetotal", "addendum", "pubstate", "doi", "eprint", "eprintclass",
                    "eprinttype", "url", "urldate", "year");
        }

        @Override
        public String getName() {
            return "Manual";
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }

        @Override
        public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
            return entry.allFieldsPresent(getRequiredFields(), database);
        }
    };

    public static final BibtexEntryType MISC = new BibtexEntryType() {

        private List<String> primaryOptionalFields = Collections.unmodifiableList(Arrays.asList(new String[]{"subtitle",
                "titleaddon", "howpublished", "location", "doi", "eprint", "eprintclass",
                "eprinttype", "url", "urldate"}));

        {
            addAllRequired("author", "editor", "title", "date");
            addAllOptional("subtitle", "titleaddon", "language", "howpublished", "type",
                    "version", "note", "organization", "location", "month", "year", "addendum",
                    "pubstate", "doi", "eprint", "eprintclass", "eprinttype", "url", "urldate");
        }

        @Override
        public String getName() {
            return "Misc";
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }

        @Override
        public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
            return entry.allFieldsPresent(getRequiredFields(), database);
        }
    };

    public static final BibtexEntryType ONLINE = new BibtexEntryType() {

        private List<String> primaryOptionalFields = Collections.unmodifiableList(Arrays.asList(new String[]{"subtitle",
                "titleaddon", "note", "organization", "urldate"}));

        {
            addAllRequired("author", "editor", "title", "date", "url");
            addAllOptional("subtitle", "titleaddon", "language", "version", "note",
                    "organization", "month", "year", "addendum", "pubstate", "urldate");
        }


        @Override
        public String getName() {
            return "Online";
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }

        @Override
        public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
            return entry.allFieldsPresent(getRequiredFields(), database);
        }
    };

    public static final BibtexEntryType PATENT = new BibtexEntryType() {

        private List<String> primaryOptionalFields = Collections.unmodifiableList(Arrays.asList(new String[]{"holder",
                "subtitle", "titleaddon", "doi", "eprint", "eprintclass", "eprinttype", "url", "urldate"}));

        {
            addAllRequired("author", "title", "number", "date");
            addAllOptional("holder", "subtitle", "titleaddon", "type", "version", "location", "note",
                    "month", "year", "addendum", "pubstate", "doi", "eprint", "eprintclass",
                    "eprinttype", "url", "urldate");
        }

        @Override
        public String getName() {
            return "Patent";
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }

        @Override
        public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
            return entry.allFieldsPresent(getRequiredFields(), database);
        }
    };

    public static final BibtexEntryType PERIODICAL = new BibtexEntryType() {

        private List<String> primaryOptionalFields = Collections.unmodifiableList(Arrays.asList(new String[]{"subtitle",
                "issuetitle", "issuesubtitle", "issn", "doi", "eprint", "eprintclass",
                "eprinttype", "url", "urldate"}));

        {
            addAllRequired("editor", "title", "date");
            addAllOptional("editora", "editorb", "editorc", "subtitle", "issuetitle",
                    "issuesubtitle", "language", "series", "volume", "number", "issue", "month", "year",
                    "note", "issn", "addendum", "pubstate", "doi", "eprint", "eprintclass", "eprinttype", "url",
                    "urldate");
        }

        @Override
        public String getName() {
            return "Periodical";
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }

        @Override
        public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
            return entry.allFieldsPresent(getRequiredFields(), database);
        }
    };

    public static final BibtexEntryType SUPPPERIODICAL = new BibtexEntryType() {

        @Override
        public String getName() {
            return "SuppPeriodical";
        }

        // Treated as alias of "ARTICLE" according to Biblatex 1.0: 
        @Override
        public List<String> getRequiredFields() {
            return BibLatexEntryTypes.ARTICLE.getRequiredFields();
        }

        @Override
        public List<String> getOptionalFields() {
            return BibLatexEntryTypes.ARTICLE.getOptionalFields();
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return BibLatexEntryTypes.ARTICLE.getPrimaryOptionalFields();
        }

        @Override
        public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
            return entry.allFieldsPresent(getRequiredFields(), database);
        }
    };

    public static final BibtexEntryType PROCEEDINGS = new BibtexEntryType() {

        private List<String> primaryOptionalFields = Collections.unmodifiableList(Arrays.asList(new String[]{"subtitle",
                "titleaddon", "maintitle", "mainsubtitle",
                "maintitleaddon", "eventtitle", "volume", "publisher", "isbn", "chapter", "pages",
                "pagetotal", "doi", "eprint", "eprintclass", "eprinttype", "url", "urldate"}));

        {
            addAllRequired("editor", "title", "date");
            addAllOptional("subtitle", "titleaddon", "maintitle", "mainsubtitle",
                    "maintitleaddon", "eventtitle", "eventdate", "venue", "language", "volume", "part",
                    "volumes", "series", "number", "note", "organization", "publisher", "location", "month",
                    "year", "isbn", "chapter", "pages", "pagetotal", "addendum", "pubstate", "doi", "eprint",
                    "eprintclass", "eprinttype", "url", "urldate");
        }

        @Override
        public String getName() {
            return "Proceedings";
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }

        @Override
        public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
            return entry.allFieldsPresent(getRequiredFields(), database);
        }
    };

    public static final BibtexEntryType INPROCEEDINGS = new BibtexEntryType() {

        private List<String> primaryOptionalFields = Collections.unmodifiableList(Arrays.asList(new String[]{"subtitle",
                "titleaddon", "maintitle", "mainsubtitle",
                "maintitleaddon", "booksubtitle", "booktitleaddon", "eventtitle", "volume",
                "publisher", "isbn", "chapter", "pages",
                "doi", "eprint", "eprintclass", "eprinttype", "url", "urldate"}));

        {
            addAllRequired("author", "editor", "title", "booktitle", "date");
            addAllOptional("subtitle", "titleaddon", "maintitle", "mainsubtitle",
                    "maintitleaddon", "booksubtitle", "booktitleaddon", "eventtitle", "eventdate", "venue",
                    "language", "volume", "part", "volumes", "series", "number", "note", "organization",
                    "publisher", "location", "month", "year", "isbn", "chapter", "pages", "addendum",
                    "pubstate", "doi", "eprint", "eprintclass", "eprinttype", "url", "urldate");
        }

        @Override
        public String getName() {
            return "InProceedings";
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
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
        public List<String> getRequiredFields() {
            return BibLatexEntryTypes.COLLECTION.getRequiredFields();
        }

        @Override
        public List<String> getOptionalFields() {
            return BibLatexEntryTypes.COLLECTION.getOptionalFields();
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return BibLatexEntryTypes.COLLECTION.getPrimaryOptionalFields();
        }

        @Override
        public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
            return entry.allFieldsPresent(getRequiredFields(), database);
        }
    };

    public static final BibtexEntryType INREFERENCE = new BibtexEntryType() {

        @Override
        public String getName() {
            return "InReference";
        }

        // Treated as alias of "INCOLLECTION" according to Biblatex 1.0: 
        @Override
        public List<String> getRequiredFields() {
            return BibLatexEntryTypes.INCOLLECTION.getRequiredFields();
        }

        @Override
        public List<String> getOptionalFields() {
            return BibLatexEntryTypes.INCOLLECTION.getOptionalFields();
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return BibLatexEntryTypes.INCOLLECTION.getPrimaryOptionalFields();
        }

        @Override
        public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
            return entry.allFieldsPresent(getRequiredFields(), database);
        }
    };

    public static final BibtexEntryType REPORT = new BibtexEntryType() {

        private List<String> primaryOptionalFields = Collections.unmodifiableList(Arrays.asList(new String[]{"subtitle",
                "titleaddon", "number", "isrn", "chapter", "pages", "pagetotal", "doi",
                "eprint", "eprintclass", "eprinttype", "url", "urldate"}));

        {
            addAllRequired("author", "title", "type", "institution", "date");
            addAllOptional("subtitle", "titleaddon", "language", "number", "version", "note",
                    "location", "month", "year", "isrn", "chapter", "pages", "pagetotal", "addendum", "pubstate", "doi",
                    "eprint", "eprintclass", "eprinttype", "url", "urldate");
        }

        @Override
        public String getName() {
            return "Report";
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }

        @Override
        public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
            return entry.allFieldsPresent(getRequiredFields(), database);
        }
    };

    public static final BibtexEntryType SET = new BibtexEntryType() {

        {
            addAllRequired("entryset", "crossref");
        }

        @Override
        public String getName() {
            return "Set";
        }

        @Override
        public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
            return entry.allFieldsPresent(getRequiredFields(), database);
        }
    };

    public static final BibtexEntryType THESIS = new BibtexEntryType() {

        private List<String> primaryOptionalFields = Collections.unmodifiableList(Arrays.asList(new String[]{"subtitle",
                "titleaddon", "chapter", "pages", "pagetotal", "doi", "eprint",
                "eprintclass", "eprinttype", "url", "urldate"}));

        {
            addAllRequired("author", "title", "type", "institution", "date");
            addAllOptional("subtitle", "titleaddon", "language", "note", "location", "month", "year",
                    "chapter", "pages", "pagetotal", "addendum", "pubstate", "doi", "eprint", "eprintclass",
                    "eprinttype", "url", "urldate");
        }

        @Override
        public String getName() {
            return "Thesis";
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }

        @Override
        public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
            return entry.allFieldsPresent(getRequiredFields(), database);
        }
    };

    public static final BibtexEntryType UNPUBLISHED = new BibtexEntryType() {

        private List<String> primaryOptionalFields = Collections.unmodifiableList(Arrays.asList(new String[]{"subtitle",
                "titleaddon", "howpublished", "pubstate", "url", "urldate"}));

        {
            addAllRequired("author", "title", "date");
            addAllOptional("subtitle", "titleaddon", "language", "howpublished", "note",
                    "location", "month", "year", "addendum", "pubstate", "url", "urldate");
        }

        @Override
        public String getName() {
            return "Unpublished";
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
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
        public List<String> getRequiredFields() {
            return BibLatexEntryTypes.INPROCEEDINGS.getRequiredFields();
        }

        @Override
        public List<String> getOptionalFields() {
            return BibLatexEntryTypes.INPROCEEDINGS.getOptionalFields();
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return BibLatexEntryTypes.INPROCEEDINGS.getPrimaryOptionalFields();
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
        public List<String> getRequiredFields() {
            return BibLatexEntryTypes.ONLINE.getRequiredFields();
        }

        @Override
        public List<String> getOptionalFields() {
            return BibLatexEntryTypes.ONLINE.getOptionalFields();
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return BibLatexEntryTypes.ONLINE.getPrimaryOptionalFields();
        }

        @Override
        public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
            return entry.allFieldsPresent(getRequiredFields(), database);
        }
    };

    public static final BibtexEntryType MASTERSTHESIS = new BibtexEntryType() {

        private List<String> primaryOptionalFields = Collections.unmodifiableList(Arrays.asList(new String[]{"subtitle",
                "titleaddon", "type", "chapter", "pages", "pagetotal", "doi", "eprint",
                "eprintclass", "eprinttype", "url", "urldate"}));

        {
            // Treated as alias of "THESIS", except "type" field is optional
            addAllRequired("author", "title", "institution", "date");
            addAllOptional("subtitle", "titleaddon", "type", "language", "note", "location", "month", "year",
                    "chapter", "pages", "pagetotal", "addendum", "pubstate", "doi", "eprint", "eprintclass",
                    "eprinttype", "url", "urldate");
        }

        @Override
        public String getName() {
            return "MastersThesis";
        }


        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }

        @Override
        public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
            return entry.allFieldsPresent(getRequiredFields(), database);
        }
    };

    public static final BibtexEntryType PHDTHESIS = new BibtexEntryType() {

        private List<String> primaryOptionalFields = Collections.unmodifiableList(Arrays.asList(new String[]{"subtitle",
                "titleaddon", "type", "chapter", "pages", "pagetotal", "doi", "eprint",
                "eprintclass", "eprinttype", "url", "urldate"}));

        {
            // Treated as alias of "THESIS", except "type" field is optional
            addAllRequired("author", "title", "institution", "date");
            addAllOptional("subtitle", "titleaddon", "type", "language", "note", "location", "month", "year",
                    "chapter", "pages", "pagetotal", "addendum", "pubstate", "doi", "eprint", "eprintclass",
                    "eprinttype", "url", "urldate");
        }

        @Override
        public String getName() {
            return "PhdThesis";
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }

        @Override
        public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
            return entry.allFieldsPresent(getRequiredFields(), database);
        }
    };

    public static final BibtexEntryType TECHREPORT = new BibtexEntryType() {

        private List<String> primaryOptionalFields = Collections.unmodifiableList(Arrays.asList(new String[]{"subtitle",
                "titleaddon", "type", "number", "isrn", "chapter", "pages", "pagetotal",
                "doi", "eprint", "eprintclass", "eprinttype", "url", "urldate"}));

        {
            // Treated as alias of "REPORT", except "type" field is optional
            addAllRequired("author", "title", "institution", "date");
            addAllOptional("subtitle", "titleaddon", "type", "language", "number", "version", "note",
                    "location", "month", "year", "isrn", "chapter", "pages", "pagetotal", "addendum", "pubstate",
                    "doi", "eprint", "eprintclass", "eprinttype", "url", "urldate");
        }

        @Override
        public String getName() {
            return "TechReport";
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }

        @Override
        public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
            return entry.allFieldsPresent(getRequiredFields(), database);
        }
    };

    public static final BibtexEntryType WWW = new BibtexEntryType() {

        @Override
        public String getName() {
            return "WWW";
        }

        // Treated as alias of "ONLINE" according to Biblatex 1.0: 
        @Override
        public List<String> getRequiredFields() {
            return BibLatexEntryTypes.ONLINE.getRequiredFields();
        }

        @Override
        public List<String> getOptionalFields() {
            return BibLatexEntryTypes.ONLINE.getOptionalFields();
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return BibLatexEntryTypes.ONLINE.getPrimaryOptionalFields();
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

        {
            addAllOptional("ctluse_article_number", "ctluse_paper", "ctluse_forced_etal",
                    "ctlmax_names_forced_etal", "ctlnames_show_etal", "ctluse_alt_spacing",
                    "ctlalt_stretch_factor", "ctldash_repeated_names", "ctlname_format_string",
                    "ctlname_latex_cmd", "ctlname_url_prefix");
        }

        @Override
        public String getName() {
            return "IEEEtranBSTCTL";
        }

        @Override
        public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
            return true;
        }
    };
}
