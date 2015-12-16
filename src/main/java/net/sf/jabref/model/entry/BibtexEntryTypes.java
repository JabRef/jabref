package net.sf.jabref.model.entry;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * This class represents all supported BibTex entry types.
 * <p>
 * Article, Book, Booklet, Conference, Inbook, Incollection, Inproceedings,
 * Manual, Mastersthesis, Misc, Phdthesis, Proceedings, Techreport, Unpublished
 */
public class BibtexEntryTypes {
    /**
     * An article from a journal or magazine.
     * <p>
     * Required fields: author, title, journal, year.
     * Optional fields: volume, number, pages, month, note.
     */
    public static final EntryType ARTICLE = new BibtexEntryType() {

        {
            addAllRequired("author", "title", "journal", "year");
            addAllOptional("volume", "number", "pages", "month", "note");
        }

        @Override
        public String getName() {
            return "Article";
        }
    };

    /**
     * A book with an explicit publisher.
     * <p>
     * Required fields: author or editor, title, publisher, year.
     * Optional fields: volume or number, series, address, edition, month, note.
     */
    public static final EntryType BOOK = new BibtexEntryType() {

        {
            addAllRequired("title", "publisher", "year", "author/editor");
            addAllOptional("volume", "number", "series", "address", "edition", "month", "note");
        }

        @Override
        public String getName() {
            return "Book";
        }
    };

    /**
     * A work that is printed and bound, but without a named publisher or sponsoring institution.
     * <p>
     * Required field: title.
     * Optional fields: author, howpublished, address, month, year, note.
     */
    public static final EntryType BOOKLET = new BibtexEntryType() {

        {
            addAllRequired("title");
            addAllOptional("author", "howpublished", "address", "month", "year", "note");
        }

        @Override
        public String getName() {
            return "Booklet";
        }
    };

    /**
     * An article in a conference proceedings.
     * <p>
     * Required fields: author, title, booktitle, year.
     * Optional fields: editor, volume or number, series, pages, address, month, organization, publisher, note.
     */
    public static final EntryType CONFERENCE = new BibtexEntryType() {

        {
            addAllRequired("author", "title", "booktitle", "year");
            addAllOptional("editor", "volume", "number", "series", "pages", "address", "month", "organization",
                    "publisher", "note");
        }

        @Override
        public String getName() {
            return "Conference";
        }
    };

    /**
     * A part of a book, which may be a chapter (or section or whatever) and/or a range of pages.
     * <p>
     * Required fields: author or editor, title, chapter and/or pages, publisher, year.
     * Optional fields: volume or number, series, type, address, edition, month, note.
     */
    public static final EntryType INBOOK = new BibtexEntryType() {

        {
            addAllRequired("chapter/pages", "title", "publisher", "year", "author/editor");
            addAllOptional("volume", "number", "series", "type", "address", "edition", "month", "note");
        }

        @Override
        public String getName() {
            return "InBook";
        }
    };

    /**
     * A part of a book having its own title.
     * Required fields: author, title, booktitle, publisher, year.
     * Optional fields: editor, volume or number, series, type, chapter, pages, address, edition, month, note.
     */
    public static final EntryType INCOLLECTION = new BibtexEntryType() {

        {
            addAllRequired("author", "title", "booktitle", "publisher", "year");
            addAllOptional("editor", "volume", "number", "series", "type", "chapter", "pages", "address", "edition",
                    "month", "note");
        }

        @Override
        public String getName() {
            return "InCollection";
        }
    };

    /**
     * An article in a conference proceedings.
     * <p>
     * Required fields: author, title, booktitle, year.
     * Optional fields: editor, volume or number, series, pages, address, month, organization, publisher, note.
     */
    public static final EntryType INPROCEEDINGS = new BibtexEntryType() {

        {
            addAllRequired("author", "title", "booktitle", "year");
            addAllOptional("editor", "volume", "number", "series", "pages", "address", "month", "organization",
                    "publisher", "note");
        }

        @Override
        public String getName() {
            return "InProceedings";
        }
    };

    /**
     * Technical documentation.
     * Required field: title.
     * Optional fields: author, organization, address, edition, month, year, note.
     */
    public static final EntryType MANUAL = new BibtexEntryType() {

        {
            addAllRequired("title");
            addAllOptional("author", "organization", "address", "edition", "month", "year", "note");
        }

        @Override
        public String getName() {
            return "Manual";
        }
    };

    /**
     * A Master's thesis.
     * <p>
     * Required fields: author, title, school, year.
     * Optional fields: type, address, month, note.
     */
    public static final EntryType MASTERSTHESIS = new BibtexEntryType() {

        {
            addAllRequired("author", "title", "school", "year");
            addAllOptional("type", "address", "month", "note");
        }

        @Override
        public String getName() {
            return "MastersThesis";
        }
    };

    /**
     * Use this type when nothing else fits.
     * <p>
     * Required fields: none.
     * Optional fields: author, title, howpublished, month, year, note.
     */
    public static final EntryType MISC = new BibtexEntryType() {

        {
            addAllOptional("author", "title", "howpublished", "month", "year", "note");
        }

        @Override
        public String getName() {
            return "Misc";
        }
    };

    /**
     * A PhD thesis.
     * <p>
     * Required fields: author, title, school, year.
     * Optional fields: type, address, month, note.
     */
    public static final EntryType PHDTHESIS = new BibtexEntryType() {

        {
            addAllRequired("author", "title", "school", "year");
            addAllOptional("type", "address", "month", "note");
        }

        @Override
        public String getName() {
            return "PhdThesis";
        }
    };

    /**
     * The proceedings of a conference.
     * <p>
     * Required fields: title, year.
     * Optional fields: editor, volume or number, series, address, month, organization, publisher, note.
     */
    public static final EntryType PROCEEDINGS = new BibtexEntryType() {

        {
            addAllRequired("title", "year");
            addAllOptional("editor", "volume", "number", "series", "address", "publisher", "note", "month",
                    "organization");
        }

        @Override
        public String getName() {
            return "Proceedings";
        }
    };

    /**
     * A report published by a school or other institution, usually numbered within a series.
     * <p>
     * Required fields: author, title, institution, year.
     * Optional fields: type, number, address, month, note.
     */
    public static final EntryType TECHREPORT = new BibtexEntryType() {

        {
            addAllRequired("author", "title", "institution", "year");
            addAllOptional("type", "number", "address", "month", "note");
        }

        @Override
        public String getName() {
            return "TechReport";
        }
    };

    /**
     * A document having an author and title, but not formally published.
     * <p>
     * Required fields: author, title, note.
     * Optional fields: month, year.
     */
    public static final EntryType UNPUBLISHED = new BibtexEntryType() {

        {
            addAllRequired("author", "title", "note");
            addAllOptional("month", "year");
        }

        @Override
        public String getName() {
            return "Unpublished";
        }
    };

    public static final List<EntryType> ALL = Arrays.asList(ARTICLE, INBOOK, BOOK, BOOKLET, INCOLLECTION, CONFERENCE,
            INPROCEEDINGS, PROCEEDINGS, MANUAL, MASTERSTHESIS, PHDTHESIS, TECHREPORT, UNPUBLISHED, MISC);

    public static Optional<EntryType> getType(String name) {
        return ALL.stream().filter(e -> e.getName().equalsIgnoreCase(name)).findFirst();
    }
}
