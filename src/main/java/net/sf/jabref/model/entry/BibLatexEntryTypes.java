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
import java.util.Optional;

/**
 * This class defines entry types for BibLatex support.
 * @see http://mirrors.concertpass.com/tex-archive/macros/latex/contrib/biblatex/doc/biblatex.pdf
 */
public class BibLatexEntryTypes {

    public static final BibLatexEntryType ARTICLE = new BibLatexEntryType() {

        private final List<String> primaryOptionalFields = Collections.unmodifiableList(
                Arrays.asList(FieldName.SUBTITLE, FieldName.EDITOR, FieldName.SERIES, FieldName.VOLUME, FieldName.NUMBER, FieldName.EID, FieldName.ISSUE, FieldName.PAGES,
                        FieldName.NOTE, FieldName.ISSN, FieldName.DOI, FieldName.EPRINT, FieldName.EPRINTCLASS, FieldName.EPRINTTYPE,
                        FieldName.URL, FieldName.URLDATE));

        {
            addAllRequired(FieldName.AUTHOR, FieldName.TITLE, FieldName.JOURNALTITLE, FieldName.orFields(FieldName.YEAR, FieldName.DATE));
            addAllOptional("translator", "annotator", "commentator", FieldName.SUBTITLE, "titleaddon", FieldName.EDITOR, "editora",
                    "editorb", "editorc", "journalsubtitle", "issuetitle", "issuesubtitle", FieldName.LANGUAGE, "origlanguage",
                    FieldName.SERIES, FieldName.VOLUME, FieldName.NUMBER, FieldName.EID, FieldName.ISSUE, FieldName.MONTH, FieldName.PAGES, FieldName.VERSION, FieldName.NOTE, FieldName.ISSN,
                    "addendum", "pubstate", FieldName.DOI, FieldName.EPRINT, FieldName.EPRINTCLASS, FieldName.EPRINTTYPE, FieldName.URL,
                    FieldName.URLDATE);
        }

        @Override
        public String getName() {
            return "Article";
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BibLatexEntryType BOOK = new BibLatexEntryType() {

        private final List<String> primaryOptionalFields = Collections
                .unmodifiableList(Arrays.asList(FieldName.EDITOR, FieldName.SUBTITLE, "titleaddon", "maintitle",
                        "mainsubtitle", "maintitleaddon", FieldName.VOLUME, FieldName.EDITION, FieldName.PUBLISHER, FieldName.ISBN, FieldName.CHAPTER, FieldName.PAGES,
                        FieldName.PAGETOTAL, FieldName.DOI, FieldName.EPRINT, FieldName.EPRINTCLASS, FieldName.EPRINTTYPE, FieldName.URL,
                        FieldName.URLDATE));


        {
            addAllRequired(FieldName.AUTHOR, FieldName.TITLE, FieldName.orFields(FieldName.YEAR, FieldName.DATE));
            addAllOptional(FieldName.EDITOR, "editora", "editorb", "editorc", "translator", "annotator", "commentator",
                    "introduction", "foreword", "afterword", FieldName.SUBTITLE, "titleaddon", "maintitle", "mainsubtitle",
                    "maintitleaddon", FieldName.LANGUAGE, "origlanguage", FieldName.VOLUME, "part", FieldName.EDITION, "volumes", FieldName.SERIES,
                    FieldName.NUMBER, FieldName.NOTE, FieldName.PUBLISHER, FieldName.LOCATION, FieldName.ISBN, FieldName.CHAPTER, FieldName.PAGES, FieldName.PAGETOTAL,
                    "addendum", "pubstate", FieldName.DOI, FieldName.EPRINT, FieldName.EPRINTCLASS, FieldName.EPRINTTYPE, FieldName.URL,
                    FieldName.URLDATE);
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

    public static final BibLatexEntryType MVBOOK = new BibLatexEntryType() {

        private final List<String> primaryOptionalFields = Collections
                .unmodifiableList(Arrays.asList(FieldName.EDITOR, FieldName.SUBTITLE, "titleaddon", FieldName.EDITION, FieldName.PUBLISHER, FieldName.ISBN, FieldName.PAGETOTAL,
                        FieldName.DOI, FieldName.EPRINT, FieldName.EPRINTCLASS, FieldName.EPRINTTYPE, FieldName.URL, FieldName.URLDATE));

        {
            addAllRequired(FieldName.AUTHOR, FieldName.TITLE, FieldName.orFields(FieldName.YEAR, FieldName.DATE));
            addAllOptional(FieldName.EDITOR, "editora", "editorb", "editorc", "translator", "annotator", "commentator",
                    "introduction", "foreword", "afterword", FieldName.SUBTITLE, "titleaddon", FieldName.LANGUAGE, "origlanguage",
                    FieldName.EDITION, "volumes", FieldName.SERIES, FieldName.NUMBER, FieldName.NOTE, FieldName.PUBLISHER, FieldName.LOCATION, FieldName.ISBN, FieldName.PAGETOTAL,
                    "addendum", "pubstate", FieldName.DOI, FieldName.EPRINT, FieldName.EPRINTCLASS, FieldName.EPRINTTYPE, FieldName.URL,
                    FieldName.URLDATE);
        }

        @Override
        public String getName() {
            return "MvBook";
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BibLatexEntryType INBOOK = new BibLatexEntryType() {

        private final List<String> primaryOptionalFields = Collections.unmodifiableList(Arrays
                .asList("bookauthor", FieldName.EDITOR, FieldName.SUBTITLE, "titleaddon", "maintitle", "mainsubtitle",
                        "maintitleaddon", "booksubtitle", "booktitleaddon", FieldName.VOLUME, FieldName.EDITION, FieldName.PUBLISHER, FieldName.ISBN,
                        FieldName.CHAPTER, FieldName.PAGES, FieldName.DOI, FieldName.EPRINT, FieldName.EPRINTCLASS, FieldName.EPRINTTYPE,
                        FieldName.URL, FieldName.URLDATE));


        {
            addAllRequired(FieldName.AUTHOR, FieldName.TITLE, FieldName.BOOKTITLE, FieldName.orFields(FieldName.YEAR, FieldName.DATE));
            addAllOptional("bookauthor", FieldName.EDITOR, "editora", "editorb", "editorc", "translator", "annotator",
                    "commentator", "introduction", "foreword", "afterword", FieldName.SUBTITLE, "titleaddon", "maintitle",
                    "mainsubtitle", "maintitleaddon", "booksubtitle", "booktitleaddon", FieldName.LANGUAGE, "origlanguage",
                    FieldName.VOLUME, "part", FieldName.EDITION, "volumes", FieldName.SERIES, FieldName.NUMBER, FieldName.NOTE, FieldName.PUBLISHER, FieldName.LOCATION, FieldName.ISBN,
                    FieldName.CHAPTER, FieldName.PAGES, "addendum", "pubstate", FieldName.DOI, FieldName.EPRINT, FieldName.EPRINTCLASS,
                    FieldName.EPRINTTYPE, FieldName.URL,
                    FieldName.URLDATE);
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
                .unmodifiableList(Arrays.asList(FieldName.SUBTITLE, "titleaddon", FieldName.HOWPUBLISHED, FieldName.CHAPTER,
                        FieldName.PAGES, FieldName.DOI, FieldName.EPRINT, FieldName.EPRINTCLASS, FieldName.EPRINTTYPE, FieldName.URL,
                        FieldName.URLDATE));

        {
            addAllRequired(FieldName.orFields(FieldName.AUTHOR, FieldName.EDITOR), FieldName.TITLE, FieldName.orFields(FieldName.YEAR, FieldName.DATE));
            addAllOptional(FieldName.SUBTITLE, "titleaddon", FieldName.LANGUAGE, FieldName.HOWPUBLISHED, FieldName.TYPE, FieldName.NOTE, FieldName.LOCATION, FieldName.CHAPTER,
                    FieldName.PAGES, FieldName.PAGETOTAL, "addendum", "pubstate", FieldName.DOI, FieldName.EPRINT,
                    FieldName.EPRINTCLASS, FieldName.EPRINTTYPE,
                    FieldName.URL, FieldName.URLDATE);
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
                .unmodifiableList(Arrays.asList("translator", FieldName.SUBTITLE, "titleaddon", "maintitle",
                        "mainsubtitle", "maintitleaddon", FieldName.VOLUME, FieldName.EDITION, FieldName.PUBLISHER, FieldName.ISBN, FieldName.CHAPTER, FieldName.PAGES,
                        FieldName.DOI, FieldName.EPRINT, FieldName.EPRINTCLASS, FieldName.EPRINTTYPE, FieldName.URL, FieldName.URLDATE));


        {
            addAllRequired(FieldName.EDITOR, FieldName.TITLE, FieldName.orFields(FieldName.YEAR, FieldName.DATE));
            addAllOptional("editora", "editorb", "editorc", "translator", "annotator", "commentator", "introduction",
                    "foreword", "afterword", FieldName.SUBTITLE, "titleaddon", "maintitle", "mainsubtitle", "maintitleaddon",
                    FieldName.LANGUAGE, "origlanguage", FieldName.VOLUME, "part", FieldName.EDITION, "volumes", FieldName.SERIES, FieldName.NUMBER, FieldName.NOTE,
                    FieldName.PUBLISHER, FieldName.LOCATION, FieldName.ISBN, FieldName.CHAPTER, FieldName.PAGES, FieldName.PAGETOTAL, "addendum", "pubstate", FieldName.DOI,
                    FieldName.EPRINT, FieldName.EPRINTCLASS, FieldName.EPRINTTYPE, FieldName.URL, FieldName.URLDATE);
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

    public static final BibLatexEntryType MVCOLLECTION = new BibLatexEntryType() {

        private final List<String> primaryOptionalFields = Collections
                .unmodifiableList(Arrays.asList("translator", FieldName.SUBTITLE, "titleaddon", FieldName.EDITION, FieldName.PUBLISHER, FieldName.ISBN,
                        FieldName.DOI, FieldName.EPRINT, FieldName.EPRINTCLASS, FieldName.EPRINTTYPE, FieldName.URL, FieldName.URLDATE));


        {
            addAllRequired(FieldName.EDITOR, FieldName.TITLE, FieldName.orFields(FieldName.YEAR, FieldName.DATE));
            addAllOptional("editora", "editorb", "editorc", "translator", "annotator", "commentator", "introduction",
                    "foreword", "afterword", FieldName.SUBTITLE, "titleaddon", FieldName.LANGUAGE, "origlanguage", FieldName.EDITION, "volumes", FieldName.SERIES, FieldName.NUMBER, FieldName.NOTE,
                    FieldName.PUBLISHER, FieldName.LOCATION, FieldName.ISBN, FieldName.PAGETOTAL, "addendum", "pubstate", FieldName.DOI,
                    FieldName.EPRINT, FieldName.EPRINTCLASS, FieldName.EPRINTTYPE, FieldName.URL, FieldName.URLDATE);
        }

        @Override
        public String getName() {
            return "MvCollection";
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BibLatexEntryType INCOLLECTION = new BibLatexEntryType() {

        private final List<String> primaryOptionalFields = Collections.unmodifiableList(
                Arrays.asList("translator", FieldName.SUBTITLE, "titleaddon", "maintitle", "mainsubtitle",
                        "maintitleaddon", "booksubtitle", "booktitleaddon", FieldName.VOLUME, FieldName.EDITION, FieldName.PUBLISHER, FieldName.ISBN,
                        FieldName.CHAPTER, FieldName.PAGES, FieldName.DOI, FieldName.EPRINT, FieldName.EPRINTCLASS, FieldName.EPRINTTYPE,
                        FieldName.URL, FieldName.URLDATE));


        {
            addAllRequired(FieldName.AUTHOR, FieldName.TITLE, FieldName.BOOKTITLE, FieldName.orFields(FieldName.YEAR, FieldName.DATE));
            addAllOptional(FieldName.EDITOR, "editora", "editorb", "editorc", "translator", "annotator", "commentator", "introduction",
                    "foreword", "afterword", FieldName.SUBTITLE, "titleaddon", "maintitle", "mainsubtitle", "maintitleaddon",
                    "booksubtitle", "booktitleaddon", FieldName.LANGUAGE, "origlanguage", FieldName.VOLUME, "part", FieldName.EDITION,
                    "volumes", FieldName.SERIES, FieldName.NUMBER, FieldName.NOTE, FieldName.PUBLISHER, FieldName.LOCATION, FieldName.ISBN, FieldName.CHAPTER, FieldName.PAGES,
                    "addendum", "pubstate", FieldName.DOI, FieldName.EPRINT, FieldName.EPRINTCLASS, FieldName.EPRINTTYPE, FieldName.URL,
                    FieldName.URLDATE);
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
                .unmodifiableList(Arrays.asList(FieldName.SUBTITLE, "titleaddon", FieldName.EDITION, FieldName.PUBLISHER, FieldName.ISBN,
                                FieldName.CHAPTER, FieldName.PAGES, FieldName.DOI, FieldName.EPRINT, FieldName.EPRINTCLASS,
                                FieldName.EPRINTTYPE, FieldName.URL, FieldName.URLDATE));


        {
            addAllRequired(FieldName.orFields(FieldName.AUTHOR, FieldName.EDITOR), FieldName.TITLE, FieldName.orFields(FieldName.YEAR, FieldName.DATE));
            addAllOptional(FieldName.SUBTITLE, "titleaddon", FieldName.LANGUAGE, FieldName.EDITION, FieldName.TYPE, FieldName.SERIES, FieldName.NUMBER, FieldName.VERSION,
                    FieldName.NOTE, FieldName.ORGANIZATION, FieldName.PUBLISHER, FieldName.LOCATION, FieldName.ISBN, FieldName.CHAPTER, FieldName.PAGES, FieldName.PAGETOTAL,
                    "addendum", "pubstate", FieldName.DOI, FieldName.EPRINT, FieldName.EPRINTCLASS, FieldName.EPRINTTYPE, FieldName.URL,
                    FieldName.URLDATE);
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
                .unmodifiableList(Arrays.asList(FieldName.SUBTITLE, "titleaddon", FieldName.HOWPUBLISHED, FieldName.LOCATION,
                        FieldName.DOI, FieldName.EPRINT, FieldName.EPRINTCLASS, FieldName.EPRINTTYPE, FieldName.URL, FieldName.URLDATE));


        {
            addAllRequired(FieldName.orFields(FieldName.AUTHOR, FieldName.EDITOR), FieldName.TITLE, FieldName.orFields(FieldName.YEAR, FieldName.DATE));
            addAllOptional(FieldName.SUBTITLE, "titleaddon", FieldName.LANGUAGE, FieldName.HOWPUBLISHED, FieldName.TYPE, FieldName.VERSION, FieldName.NOTE,
                    FieldName.ORGANIZATION, FieldName.LOCATION, FieldName.MONTH, "addendum", "pubstate", FieldName.DOI,
                    FieldName.EPRINT, FieldName.EPRINTCLASS,
                    FieldName.EPRINTTYPE, FieldName.URL, FieldName.URLDATE);
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
                Arrays.asList(FieldName.SUBTITLE, "titleaddon", FieldName.NOTE, FieldName.ORGANIZATION, FieldName.URLDATE));


        {
            addAllRequired(FieldName.orFields(FieldName.AUTHOR, FieldName.EDITOR), FieldName.TITLE, FieldName.orFields(FieldName.YEAR, FieldName.DATE), FieldName.URL);
            addAllOptional(FieldName.SUBTITLE, "titleaddon", FieldName.LANGUAGE, FieldName.VERSION, FieldName.NOTE, FieldName.ORGANIZATION, FieldName.MONTH, "addendum", "pubstate", FieldName.URLDATE);
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

        private final List<String> primaryOptionalFields = Collections.unmodifiableList(Arrays.asList("holder",
                FieldName.SUBTITLE, "titleaddon", FieldName.DOI, FieldName.EPRINT, FieldName.EPRINTCLASS, FieldName.EPRINTTYPE, FieldName.URL,
                FieldName.URLDATE));


        {
            addAllRequired(FieldName.AUTHOR, FieldName.TITLE, FieldName.NUMBER, FieldName.orFields(FieldName.YEAR, FieldName.DATE));
            addAllOptional("holder", FieldName.SUBTITLE, "titleaddon", FieldName.TYPE, FieldName.VERSION, FieldName.LOCATION, FieldName.NOTE, FieldName.MONTH,
                    "addendum", "pubstate", FieldName.DOI, FieldName.EPRINT, FieldName.EPRINTCLASS, FieldName.EPRINTTYPE, FieldName.URL,
                    FieldName.URLDATE);
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
                .unmodifiableList(Arrays.asList(FieldName.SUBTITLE, "issuetitle", "issuesubtitle", FieldName.ISSN, FieldName.DOI,
                                FieldName.EPRINT, FieldName.EPRINTCLASS, FieldName.EPRINTTYPE, FieldName.URL, FieldName.URLDATE));


        {
            addAllRequired(FieldName.EDITOR, FieldName.TITLE, FieldName.orFields(FieldName.YEAR, FieldName.DATE));
            addAllOptional("editora", "editorb", "editorc", FieldName.SUBTITLE, "issuetitle", "issuesubtitle", FieldName.LANGUAGE,
                    FieldName.SERIES, FieldName.VOLUME, FieldName.NUMBER, FieldName.ISSUE, FieldName.MONTH, FieldName.NOTE, FieldName.ISSN, "addendum", "pubstate",
                    FieldName.DOI, FieldName.EPRINT, FieldName.EPRINTCLASS, FieldName.EPRINTTYPE, FieldName.URL, FieldName.URLDATE);
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
                .unmodifiableList(Arrays.asList(FieldName.SUBTITLE, "titleaddon", "maintitle", "mainsubtitle",
                        "maintitleaddon", "eventtitle", FieldName.VOLUME, FieldName.PUBLISHER, FieldName.ISBN, FieldName.CHAPTER, FieldName.PAGES, FieldName.PAGETOTAL,
                        FieldName.DOI, FieldName.EPRINT, FieldName.EPRINTCLASS, FieldName.EPRINTTYPE, FieldName.URL, FieldName.URLDATE));


        {
            addAllRequired(FieldName.TITLE, FieldName.orFields(FieldName.YEAR, FieldName.DATE));
            addAllOptional(FieldName.EDITOR, FieldName.SUBTITLE, "titleaddon", "maintitle", "mainsubtitle", "maintitleaddon", "eventtitle",
                    "eventtitleaddon", "eventdate", "venue", FieldName.LANGUAGE, FieldName.VOLUME, "part", "volumes", FieldName.SERIES, FieldName.NUMBER, FieldName.NOTE,
                    FieldName.ORGANIZATION, FieldName.PUBLISHER, FieldName.LOCATION, FieldName.MONTH, FieldName.YEAR, FieldName.ISBN, FieldName.CHAPTER, FieldName.PAGES, FieldName.PAGETOTAL,
                    "addendum", "pubstate", FieldName.DOI, FieldName.EPRINT, FieldName.EPRINTCLASS, FieldName.EPRINTTYPE, FieldName.URL,
                    FieldName.URLDATE);
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

    public static final BibLatexEntryType MVPROCEEDINGS = new BibLatexEntryType() {

        private final List<String> primaryOptionalFields = Collections
                .unmodifiableList(Arrays.asList(FieldName.SUBTITLE, "titleaddon", "maintitle", "mainsubtitle",
                        "maintitleaddon", "eventtitle", FieldName.VOLUME, FieldName.PUBLISHER, FieldName.ISBN, FieldName.CHAPTER, FieldName.PAGES, FieldName.PAGETOTAL,
                        FieldName.DOI, FieldName.EPRINT, FieldName.EPRINTCLASS, FieldName.EPRINTTYPE, FieldName.URL, FieldName.URLDATE));


        {
            addAllRequired(FieldName.TITLE, FieldName.orFields(FieldName.YEAR, FieldName.DATE));
            addAllOptional(FieldName.EDITOR, FieldName.SUBTITLE, "titleaddon", "eventtitle",
                    "eventtitleaddon", "eventdate", "venue", FieldName.LANGUAGE, "volumes", FieldName.SERIES, FieldName.NUMBER, FieldName.NOTE,
                    FieldName.ORGANIZATION, FieldName.PUBLISHER, FieldName.LOCATION, FieldName.MONTH, FieldName.ISBN, FieldName.PAGETOTAL,
                    "addendum", "pubstate", FieldName.DOI, FieldName.EPRINT, FieldName.EPRINTCLASS, FieldName.EPRINTTYPE, FieldName.URL,
                    FieldName.URLDATE);
        }

        @Override
        public String getName() {
            return "MvProceedings";
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BibLatexEntryType INPROCEEDINGS = new BibLatexEntryType() {

        private final List<String> primaryOptionalFields = Collections
                .unmodifiableList(Arrays.asList(FieldName.SUBTITLE, "titleaddon", "maintitle", "mainsubtitle",
                        "maintitleaddon", "booksubtitle", "booktitleaddon", "eventtitle", FieldName.VOLUME, FieldName.PUBLISHER, FieldName.ISBN,
                        FieldName.CHAPTER, FieldName.PAGES, FieldName.DOI, FieldName.EPRINT, FieldName.EPRINTCLASS, FieldName.EPRINTTYPE,
                        FieldName.URL, FieldName.URLDATE));


        {
            addAllRequired(FieldName.AUTHOR, FieldName.TITLE, FieldName.BOOKTITLE, FieldName.orFields(FieldName.YEAR, FieldName.DATE));
            addAllOptional(FieldName.EDITOR, FieldName.SUBTITLE, "titleaddon", "maintitle", "mainsubtitle", "maintitleaddon", "booksubtitle",
                    "booktitleaddon", "eventtitle", "eventtitleaddon", "eventdate", "venue", FieldName.LANGUAGE, FieldName.VOLUME, "part", "volumes",
                    FieldName.SERIES, FieldName.NUMBER, FieldName.NOTE, FieldName.ORGANIZATION, FieldName.PUBLISHER, FieldName.LOCATION, FieldName.MONTH, FieldName.ISBN,
                    FieldName.CHAPTER, FieldName.PAGES, "addendum", "pubstate", FieldName.DOI, FieldName.EPRINT, FieldName.EPRINTCLASS,
                    FieldName.EPRINTTYPE, FieldName.URL,
                    FieldName.URLDATE);
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

    public static final BibLatexEntryType MVREFERENCE = new BibLatexEntryType() {

        @Override
        public String getName() {
            return "MvReference";
        }

        // Treated as alias of "MVCOLLECTION" according to Biblatex 1.0:
        @Override
        public List<String> getRequiredFields() {
            return BibLatexEntryTypes.MVCOLLECTION.getRequiredFields();
        }

        @Override
        public List<String> getOptionalFields() {
            return BibLatexEntryTypes.MVCOLLECTION.getOptionalFields();
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return BibLatexEntryTypes.MVCOLLECTION.getPrimaryOptionalFields();
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
                .unmodifiableList(Arrays.asList(FieldName.SUBTITLE, "titleaddon", FieldName.NUMBER, "isrn", FieldName.CHAPTER,
                        FieldName.PAGES, FieldName.PAGETOTAL, FieldName.DOI, FieldName.EPRINT, FieldName.EPRINTCLASS, FieldName.EPRINTTYPE,
                        FieldName.URL, FieldName.URLDATE));


        {
            addAllRequired(FieldName.AUTHOR, FieldName.TITLE, FieldName.TYPE, FieldName.INSTITUTION, FieldName.orFields(FieldName.YEAR, FieldName.DATE));
            addAllOptional(FieldName.SUBTITLE, "titleaddon", FieldName.LANGUAGE, FieldName.NUMBER, FieldName.VERSION, FieldName.NOTE, FieldName.LOCATION, FieldName.MONTH,
                    "isrn", FieldName.CHAPTER, FieldName.PAGES, FieldName.PAGETOTAL, "addendum", "pubstate", FieldName.DOI,
                    FieldName.EPRINT,
                    FieldName.EPRINTCLASS, FieldName.EPRINTTYPE, FieldName.URL, FieldName.URLDATE);
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
            addAllRequired("entryset", FieldName.CROSSREF);
        }

        @Override
        public String getName() {
            return "Set";
        }
    };

    public static final BibLatexEntryType THESIS = new BibLatexEntryType() {

        private final List<String> primaryOptionalFields = Collections
                .unmodifiableList(Arrays.asList(FieldName.SUBTITLE, "titleaddon", FieldName.CHAPTER, FieldName.PAGES, FieldName.PAGETOTAL,
                        FieldName.DOI, FieldName.EPRINT, FieldName.EPRINTCLASS, FieldName.EPRINTTYPE, FieldName.URL, FieldName.URLDATE));


        {
            addAllRequired(FieldName.AUTHOR, FieldName.TITLE, FieldName.TYPE, FieldName.INSTITUTION, FieldName.orFields(FieldName.YEAR, FieldName.DATE));
            addAllOptional(FieldName.SUBTITLE, "titleaddon", FieldName.LANGUAGE, FieldName.NOTE, FieldName.LOCATION, FieldName.MONTH, FieldName.ISBN, FieldName.CHAPTER,
                    FieldName.PAGES, FieldName.PAGETOTAL, "addendum", "pubstate", FieldName.DOI, FieldName.EPRINT,
                    FieldName.EPRINTCLASS, FieldName.EPRINTTYPE, FieldName.URL,
                    FieldName.URLDATE);
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
                Arrays.asList(FieldName.SUBTITLE, "titleaddon", FieldName.HOWPUBLISHED, "pubstate", FieldName.URL, FieldName.URLDATE));


        {
            addAllRequired(FieldName.AUTHOR, FieldName.TITLE, FieldName.orFields(FieldName.YEAR, FieldName.DATE));
            addAllOptional(FieldName.SUBTITLE, "titleaddon", FieldName.LANGUAGE, FieldName.HOWPUBLISHED, FieldName.NOTE, FieldName.LOCATION, FieldName.MONTH,
                    "addendum", "pubstate", FieldName.URL, FieldName.URLDATE);
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
                .unmodifiableList(Arrays.asList(FieldName.SUBTITLE, "titleaddon", FieldName.TYPE, FieldName.CHAPTER, FieldName.PAGES,
                        FieldName.PAGETOTAL, FieldName.DOI, FieldName.EPRINT, FieldName.EPRINTCLASS, FieldName.EPRINTTYPE, FieldName.URL,
                        FieldName.URLDATE));


        {
            // Treated as alias of "THESIS", except FieldName.TYPE field is optional
            addAllRequired(FieldName.AUTHOR, FieldName.TITLE, FieldName.INSTITUTION, FieldName.orFields(FieldName.YEAR, FieldName.DATE));
            addAllOptional(FieldName.TYPE, FieldName.SUBTITLE, "titleaddon", FieldName.LANGUAGE, FieldName.NOTE, FieldName.LOCATION, FieldName.MONTH, FieldName.ISBN, FieldName.CHAPTER,
                    FieldName.PAGES, FieldName.PAGETOTAL, "addendum", "pubstate", FieldName.DOI, FieldName.EPRINT,
                    FieldName.EPRINTCLASS, FieldName.EPRINTTYPE, FieldName.URL,
                    FieldName.URLDATE);
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
                .unmodifiableList(Arrays.asList(FieldName.SUBTITLE, "titleaddon", FieldName.TYPE, FieldName.CHAPTER, FieldName.PAGES,
                        FieldName.PAGETOTAL, FieldName.DOI, FieldName.EPRINT, FieldName.EPRINTCLASS, FieldName.EPRINTTYPE, FieldName.URL,
                        FieldName.URLDATE));


        {
            // Treated as alias of "THESIS", except FieldName.TYPE field is optional
            addAllRequired(FieldName.AUTHOR, FieldName.TITLE, FieldName.INSTITUTION, FieldName.orFields(FieldName.YEAR, FieldName.DATE));
            addAllOptional(FieldName.TYPE, FieldName.SUBTITLE, "titleaddon", FieldName.LANGUAGE, FieldName.NOTE, FieldName.LOCATION, FieldName.MONTH, FieldName.ISBN, FieldName.CHAPTER,
                    FieldName.PAGES, FieldName.PAGETOTAL, "addendum", "pubstate", FieldName.DOI, FieldName.EPRINT,
                    FieldName.EPRINTCLASS, FieldName.EPRINTTYPE, FieldName.URL,
                    FieldName.URLDATE);
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
                Arrays.asList(FieldName.SUBTITLE, "titleaddon", FieldName.TYPE, FieldName.NUMBER, "isrn", FieldName.CHAPTER, FieldName.PAGES,
                        FieldName.PAGETOTAL, FieldName.DOI, FieldName.EPRINT, FieldName.EPRINTCLASS, FieldName.EPRINTTYPE, FieldName.URL,
                        FieldName.URLDATE));


        {
            // Treated as alias of "REPORT", except FieldName.TYPE field is optional
            addAllRequired(FieldName.AUTHOR, FieldName.TITLE, FieldName.INSTITUTION, FieldName.orFields(FieldName.YEAR, FieldName.DATE));
            addAllOptional(FieldName.TYPE, FieldName.SUBTITLE, "titleaddon", FieldName.LANGUAGE, FieldName.NUMBER, FieldName.VERSION, FieldName.NOTE, FieldName.LOCATION, FieldName.MONTH,
                    "isrn", FieldName.CHAPTER, FieldName.PAGES, FieldName.PAGETOTAL, "addendum", "pubstate", FieldName.DOI,
                    FieldName.EPRINT, FieldName.EPRINTCLASS, FieldName.EPRINTTYPE, FieldName.URL, FieldName.URLDATE);
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

    public static final List<EntryType> ALL = Arrays.asList(ARTICLE, BOOK, MVBOOK, INBOOK, BOOKINBOOK, SUPPBOOK,
            BOOKLET, COLLECTION, MVCOLLECTION, INCOLLECTION, SUPPCOLLECTION, MANUAL, MISC, ONLINE, PATENT, PERIODICAL,
            SUPPPERIODICAL, PROCEEDINGS, MVPROCEEDINGS, INPROCEEDINGS, REFERENCE, MVREFERENCE, INREFERENCE, REPORT, SET,
            THESIS, UNPUBLISHED, CONFERENCE, ELECTRONIC, MASTERSTHESIS, PHDTHESIS, TECHREPORT, WWW, IEEETRANBSTCTL);

    public static Optional<EntryType> getType(String name) {
        return ALL.stream().filter(e -> e.getName().equalsIgnoreCase(name)).findFirst();
    }
}
