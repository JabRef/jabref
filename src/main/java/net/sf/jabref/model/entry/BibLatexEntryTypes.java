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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class defines entry types for BibLatex support.
 *
 * @see http://mirrors.concertpass.com/tex-archive/macros/latex/contrib/biblatex/doc/biblatex.pdf
 */
public class BibLatexEntryTypes {

    /*
        "rare" fields?
            "annotator", "commentator", "titleaddon", "editora", "editorb", "editorc",
            "issuetitle", "issuesubtitle", "origlanguage", "version", "addendum"

     */
    public static final BibLatexEntryType ARTICLE = new BibLatexEntryType() {

        private final List<String> primaryOptionalFields = Collections.unmodifiableList(
                Arrays.asList(new String[]{"subtitle", "editor", "series", "volume", "number", "eid", "issue", "pages",
                        "note", "issn", "doi", "eprint", "eprintclass", "eprinttype", "url", "urldate"}));


        {
            addAllRequired("author", "title", "journaltitle", "date");
            addAllOptional("translator", "annotator", "commentator", "subtitle", "titleaddon", "editor", "editora",
                    "editorb", "editorc", "journalsubtitle", "issuetitle", "issuesubtitle", "language", "origlanguage",
                    "series", "volume", "number", "eid", "issue", "month", "year", "pages", "version", "note", "issn",
                    "addendum", "pubstate", "doi", "eprint", "eprintclass", "eprinttype", "url", "urldate");
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
    };

    public static final BibLatexEntryType BOOK = new BibLatexEntryType() {

        private final List<String> primaryOptionalFields = Collections
                .unmodifiableList(Arrays.asList(new String[]{"editor", "subtitle", "titleaddon", "maintitle",
                        "mainsubtitle", "maintitleaddon", "volume", "edition", "publisher", "isbn", "chapter", "pages",
                        "pagetotal", "doi", "eprint", "eprintclass", "eprinttype", "url", "urldate"}));


        {
            addAllRequired("author", "title", "date");
            addAllOptional("editor", "editora", "editorb", "editorc", "translator", "annotator", "commentator",
                    "introduction", "foreword", "afterword", "subtitle", "titleaddon", "maintitle", "mainsubtitle",
                    "maintitleaddon", "language", "origlanguage", "volume", "part", "edition", "volumes", "series",
                    "number", "month", "year", "note", "publisher", "location", "isbn", "chapter", "pages", "pagetotal",
                    "addendum", "pubstate", "doi", "eprint", "eprintclass", "eprinttype", "url", "urldate");
        }

        @Override
        public String getName() {
            return "Book";
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BibLatexEntryType INBOOK = new BibLatexEntryType() {

        private final List<String> primaryOptionalFields = Collections.unmodifiableList(Arrays
                .asList(new String[]{"bookauthor", "editor", "subtitle", "titleaddon", "maintitle", "mainsubtitle",
                        "maintitleaddon", "booksubtitle", "booktitleaddon", "volume", "edition", "publisher", "isbn",
                        "chapter", "pages", "doi", "eprint", "eprintclass", "eprinttype", "url", "urldate"}));


        {
            addAllRequired("author", "title", "booktitle", "date");
            addAllOptional("bookauthor", "editor", "editora", "editorb", "editorc", "translator", "annotator",
                    "commentator", "introduction", "foreword", "afterword", "subtitle", "titleaddon", "maintitle",
                    "mainsubtitle", "maintitleaddon", "booksubtitle", "booktitleaddon", "language", "origlanguage",
                    "volume", "part", "edition", "volumes", "series", "number", "note", "publisher", "location", "isbn",
                    "chapter", "pages", "addendum", "pubstate", "doi", "eprint", "eprintclass", "eprinttype", "url",
                    "urldate", "year");
        }

        @Override
        public String getName() {
            return "InBook";
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BibLatexEntryType BOOKINBOOK = new BibLatexEntryType() {

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
    };

    public static final BibLatexEntryType SUPPBOOK = new BibLatexEntryType() {

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
    };

    public static final BibLatexEntryType BOOKLET = new BibLatexEntryType() {

        private final List<String> primaryOptionalFields = Collections
                .unmodifiableList(Arrays.asList(new String[]{"subtitle", "titleaddon", "howpublished", "chapter",
                        "pages", "doi", "eprint", "eprintclass", "eprinttype", "url", "urldate"}));


        {
            addAllRequired("author", "editor", "title", "date");
            addAllOptional("subtitle", "titleaddon", "language", "howpublished", "type", "note", "location", "chapter",
                    "year", "pages", "pagetotal", "addendum", "pubstate", "doi", "eprint", "eprintclass", "eprinttype",
                    "url", "urldate");
        }

        @Override
        public String getName() {
            return "Booklet";
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BibLatexEntryType COLLECTION = new BibLatexEntryType() {

        private final List<String> primaryOptionalFields = Collections
                .unmodifiableList(Arrays.asList(new String[]{"translator", "subtitle", "titleaddon", "maintitle",
                        "mainsubtitle", "maintitleaddon", "volume", "edition", "publisher", "isbn", "chapter", "pages",
                        "doi", "eprint", "eprintclass", "eprinttype", "url", "urldate"}));


        {
            addAllRequired("editor", "title", "date");
            addAllOptional("editora", "editorb", "editorc", "translator", "annotator", "commentator", "introduction",
                    "foreword", "afterword", "subtitle", "titleaddon", "maintitle", "mainsubtitle", "maintitleaddon",
                    "language", "origlanguage", "volume", "part", "edition", "volumes", "series", "number", "note",
                    "publisher", "location", "isbn", "chapter", "pages", "pagetotal", "addendum", "pubstate", "doi",
                    "eprint", "eprintclass", "eprinttype", "url", "urldate", "year");
        }

        @Override
        public String getName() {
            return "Collection";
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BibLatexEntryType INCOLLECTION = new BibLatexEntryType() {

        private final List<String> primaryOptionalFields = Collections.unmodifiableList(
                Arrays.asList(new String[]{"translator", "subtitle", "titleaddon", "maintitle", "mainsubtitle",
                        "maintitleaddon", "booksubtitle", "booktitleaddon", "volume", "edition", "publisher", "isbn",
                        "chapter", "pages", "doi", "eprint", "eprintclass", "eprinttype", "url", "urldate"}));


        {
            addAllRequired("author", "editor", "title", "booktitle", "date");
            addAllOptional("editora", "editorb", "editorc", "translator", "annotator", "commentator", "introduction",
                    "foreword", "afterword", "subtitle", "titleaddon", "maintitle", "mainsubtitle", "maintitleaddon",
                    "booksubtitle", "booktitleaddon", "language", "origlanguage", "volume", "part", "edition",
                    "volumes", "series", "number", "note", "publisher", "location", "isbn", "chapter", "pages",
                    "addendum", "pubstate", "doi", "eprint", "eprintclass", "eprinttype", "url", "urldate", "year");
        }

        @Override
        public String getName() {
            return "InCollection";
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BibLatexEntryType SUPPCOLLECTION = new BibLatexEntryType() {

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
    };

    public static final BibLatexEntryType MANUAL = new BibLatexEntryType() {

        private final List<String> primaryOptionalFields = Collections
                .unmodifiableList(Arrays.asList(new String[]{"subtitle", "titleaddon", "edition", "publisher", "isbn",
                        "chapter", "pages", "doi", "eprint", "eprintclass", "eprinttype", "url", "urldate"}));


        {
            addAllRequired("author", "editor", "title", "date");
            addAllOptional("subtitle", "titleaddon", "language", "edition", "type", "series", "number", "version",
                    "note", "organization", "publisher", "location", "isbn", "chapter", "pages", "pagetotal",
                    "addendum", "pubstate", "doi", "eprint", "eprintclass", "eprinttype", "url", "urldate", "year");
        }

        @Override
        public String getName() {
            return "Manual";
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BibLatexEntryType MISC = new BibLatexEntryType() {

        private final List<String> primaryOptionalFields = Collections
                .unmodifiableList(Arrays.asList(new String[]{"subtitle", "titleaddon", "howpublished", "location",
                        "doi", "eprint", "eprintclass", "eprinttype", "url", "urldate"}));


        {
            addAllRequired("author", "editor", "title", "date");
            addAllOptional("subtitle", "titleaddon", "language", "howpublished", "type", "version", "note",
                    "organization", "location", "month", "year", "addendum", "pubstate", "doi", "eprint", "eprintclass",
                    "eprinttype", "url", "urldate");
        }

        @Override
        public String getName() {
            return "Misc";
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BibLatexEntryType ONLINE = new BibLatexEntryType() {

        private final List<String> primaryOptionalFields = Collections.unmodifiableList(
                Arrays.asList(new String[]{"subtitle", "titleaddon", "note", "organization", "urldate"}));


        {
            addAllRequired("author", "editor", "title", "date", "url");
            addAllOptional("subtitle", "titleaddon", "language", "version", "note", "organization", "month", "year",
                    "addendum", "pubstate", "urldate");
        }

        @Override
        public String getName() {
            return "Online";
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BibLatexEntryType PATENT = new BibLatexEntryType() {

        private final List<String> primaryOptionalFields = Collections.unmodifiableList(Arrays.asList(new String[]{
                "holder",
                "subtitle", "titleaddon", "doi", "eprint", "eprintclass", "eprinttype", "url", "urldate"}));


        {
            addAllRequired("author", "title", "number", "date");
            addAllOptional("holder", "subtitle", "titleaddon", "type", "version", "location", "note", "month", "year",
                    "addendum", "pubstate", "doi", "eprint", "eprintclass", "eprinttype", "url", "urldate");
        }

        @Override
        public String getName() {
            return "Patent";
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BibLatexEntryType PERIODICAL = new BibLatexEntryType() {

        private final List<String> primaryOptionalFields = Collections
                .unmodifiableList(Arrays.asList(new String[]{"subtitle", "issuetitle", "issuesubtitle", "issn", "doi",
                        "eprint", "eprintclass", "eprinttype", "url", "urldate"}));


        {
            addAllRequired("editor", "title", "date");
            addAllOptional("editora", "editorb", "editorc", "subtitle", "issuetitle", "issuesubtitle", "language",
                    "series", "volume", "number", "issue", "month", "year", "note", "issn", "addendum", "pubstate",
                    "doi", "eprint", "eprintclass", "eprinttype", "url", "urldate");
        }

        @Override
        public String getName() {
            return "Periodical";
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BibLatexEntryType SUPPPERIODICAL = new BibLatexEntryType() {

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
    };

    public static final BibLatexEntryType PROCEEDINGS = new BibLatexEntryType() {

        private final List<String> primaryOptionalFields = Collections
                .unmodifiableList(Arrays.asList(new String[]{"subtitle", "titleaddon", "maintitle", "mainsubtitle",
                        "maintitleaddon", "eventtitle", "volume", "publisher", "isbn", "chapter", "pages", "pagetotal",
                        "doi", "eprint", "eprintclass", "eprinttype", "url", "urldate"}));


        {
            addAllRequired("editor", "title", "date");
            addAllOptional("subtitle", "titleaddon", "maintitle", "mainsubtitle", "maintitleaddon", "eventtitle",
                    "eventdate", "venue", "language", "volume", "part", "volumes", "series", "number", "note",
                    "organization", "publisher", "location", "month", "year", "isbn", "chapter", "pages", "pagetotal",
                    "addendum", "pubstate", "doi", "eprint", "eprintclass", "eprinttype", "url", "urldate");
        }

        @Override
        public String getName() {
            return "Proceedings";
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BibLatexEntryType INPROCEEDINGS = new BibLatexEntryType() {

        private final List<String> primaryOptionalFields = Collections
                .unmodifiableList(Arrays.asList(new String[]{"subtitle", "titleaddon", "maintitle", "mainsubtitle",
                        "maintitleaddon", "booksubtitle", "booktitleaddon", "eventtitle", "volume", "publisher", "isbn",
                        "chapter", "pages", "doi", "eprint", "eprintclass", "eprinttype", "url", "urldate"}));


        {
            addAllRequired("author", "editor", "title", "booktitle", "date");
            addAllOptional("subtitle", "titleaddon", "maintitle", "mainsubtitle", "maintitleaddon", "booksubtitle",
                    "booktitleaddon", "eventtitle", "eventdate", "venue", "language", "volume", "part", "volumes",
                    "series", "number", "note", "organization", "publisher", "location", "month", "year", "isbn",
                    "chapter", "pages", "addendum", "pubstate", "doi", "eprint", "eprintclass", "eprinttype", "url",
                    "urldate");
        }

        @Override
        public String getName() {
            return "InProceedings";
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BibLatexEntryType REFERENCE = new BibLatexEntryType() {

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
    };

    public static final BibLatexEntryType INREFERENCE = new BibLatexEntryType() {

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
    };

    public static final BibLatexEntryType REPORT = new BibLatexEntryType() {

        private final List<String> primaryOptionalFields = Collections
                .unmodifiableList(Arrays.asList(new String[]{"subtitle", "titleaddon", "number", "isrn", "chapter",
                        "pages", "pagetotal", "doi", "eprint", "eprintclass", "eprinttype", "url", "urldate"}));


        {
            addAllRequired("author", "title", "type", "institution", "date");
            addAllOptional("subtitle", "titleaddon", "language", "number", "version", "note", "location", "month",
                    "year", "isrn", "chapter", "pages", "pagetotal", "addendum", "pubstate", "doi", "eprint",
                    "eprintclass", "eprinttype", "url", "urldate");
        }

        @Override
        public String getName() {
            return "Report";
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BibLatexEntryType SET = new BibLatexEntryType() {

        {
            addAllRequired("entryset", "crossref");
        }

        @Override
        public String getName() {
            return "Set";
        }
    };

    public static final BibLatexEntryType THESIS = new BibLatexEntryType() {

        private final List<String> primaryOptionalFields = Collections
                .unmodifiableList(Arrays.asList(new String[]{"subtitle", "titleaddon", "chapter", "pages", "pagetotal",
                        "doi", "eprint", "eprintclass", "eprinttype", "url", "urldate"}));


        {
            addAllRequired("author", "title", "type", "institution", "date");
            addAllOptional("subtitle", "titleaddon", "language", "note", "location", "month", "year", "chapter",
                    "pages", "pagetotal", "addendum", "pubstate", "doi", "eprint", "eprintclass", "eprinttype", "url",
                    "urldate");
        }

        @Override
        public String getName() {
            return "Thesis";
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BibLatexEntryType UNPUBLISHED = new BibLatexEntryType() {

        private final List<String> primaryOptionalFields = Collections.unmodifiableList(
                Arrays.asList(new String[]{"subtitle", "titleaddon", "howpublished", "pubstate", "url", "urldate"}));


        {
            addAllRequired("author", "title", "date");
            addAllOptional("subtitle", "titleaddon", "language", "howpublished", "note", "location", "month", "year",
                    "addendum", "pubstate", "url", "urldate");
        }

        @Override
        public String getName() {
            return "Unpublished";
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    // === Type aliases: ===

    public static final BibLatexEntryType CONFERENCE = new BibLatexEntryType() {

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
    };

    public static final BibLatexEntryType ELECTRONIC = new BibLatexEntryType() {

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
    };

    public static final BibLatexEntryType MASTERSTHESIS = new BibLatexEntryType() {

        private final List<String> primaryOptionalFields = Collections
                .unmodifiableList(Arrays.asList(new String[]{"subtitle", "titleaddon", "type", "chapter", "pages",
                        "pagetotal", "doi", "eprint", "eprintclass", "eprinttype", "url", "urldate"}));


        {
            // Treated as alias of "THESIS", except "type" field is optional
            addAllRequired("author", "title", "institution", "date");
            addAllOptional("subtitle", "titleaddon", "type", "language", "note", "location", "month", "year", "chapter",
                    "pages", "pagetotal", "addendum", "pubstate", "doi", "eprint", "eprintclass", "eprinttype", "url",
                    "urldate");
        }

        @Override
        public String getName() {
            return "MastersThesis";
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BibLatexEntryType PHDTHESIS = new BibLatexEntryType() {

        private final List<String> primaryOptionalFields = Collections
                .unmodifiableList(Arrays.asList(new String[]{"subtitle", "titleaddon", "type", "chapter", "pages",
                        "pagetotal", "doi", "eprint", "eprintclass", "eprinttype", "url", "urldate"}));


        {
            // Treated as alias of "THESIS", except "type" field is optional
            addAllRequired("author", "title", "institution", "date");
            addAllOptional("subtitle", "titleaddon", "type", "language", "note", "location", "month", "year", "chapter",
                    "pages", "pagetotal", "addendum", "pubstate", "doi", "eprint", "eprintclass", "eprinttype", "url",
                    "urldate");
        }

        @Override
        public String getName() {
            return "PhdThesis";
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BibLatexEntryType TECHREPORT = new BibLatexEntryType() {

        private final List<String> primaryOptionalFields = Collections.unmodifiableList(
                Arrays.asList(new String[]{"subtitle", "titleaddon", "type", "number", "isrn", "chapter", "pages",
                        "pagetotal", "doi", "eprint", "eprintclass", "eprinttype", "url", "urldate"}));


        {
            // Treated as alias of "REPORT", except "type" field is optional
            addAllRequired("author", "title", "institution", "date");
            addAllOptional("subtitle", "titleaddon", "type", "language", "number", "version", "note", "location",
                    "month", "year", "isrn", "chapter", "pages", "pagetotal", "addendum", "pubstate", "doi", "eprint",
                    "eprintclass", "eprinttype", "url", "urldate");
        }

        @Override
        public String getName() {
            return "TechReport";
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BibLatexEntryType WWW = new BibLatexEntryType() {

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
    };

    /**
     * This type is used for IEEEtran.bst to control various
     * be repeated or not. Not a very elegant solution, but it works...
     */
    public static final BibLatexEntryType IEEETRANBSTCTL = new BibLatexEntryType() {

        {
            addAllOptional("ctluse_article_number", "ctluse_paper", "ctluse_forced_etal", "ctluse_url",
                    "ctlmax_names_forced_etal", "ctlnames_show_etal", "ctluse_alt_spacing", "ctlalt_stretch_factor",
                    "ctldash_repeated_names", "ctlname_format_string", "ctlname_latex_cmd", "ctlname_url_prefix");
        }

        @Override
        public String getName() {
            return "IEEEtranBSTCTL";
        }
    };

    public static final List<EntryType> ALL = Arrays.asList(ARTICLE, BOOK, INBOOK, BOOKINBOOK, SUPPBOOK, BOOKLET,
            COLLECTION, INCOLLECTION, SUPPCOLLECTION, MANUAL, MISC, ONLINE, PATENT, PERIODICAL, SUPPPERIODICAL,
            PROCEEDINGS, INPROCEEDINGS, REFERENCE, INREFERENCE, REPORT, SET, THESIS, UNPUBLISHED, CONFERENCE, ELECTRONIC,
            MASTERSTHESIS, PHDTHESIS, TECHREPORT, WWW, IEEETRANBSTCTL);

    public static final List<String> ENTRY_TYPE_NAMES = ALL.stream().map(EntryType::getName).collect(Collectors.toList());
}
