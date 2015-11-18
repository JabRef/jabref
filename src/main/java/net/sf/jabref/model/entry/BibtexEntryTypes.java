package net.sf.jabref.model.entry;

import net.sf.jabref.model.database.BibtexDatabase;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * This class represents all supported BibTex entry types.
 *
 * Article, Book, Booklet, Conference, Inbook, Incollection, Inproceedings,
 * Manual, Mastersthesis, Misc, Phdthesis, Proceedings, Techreport, Unpublished
 */
public class BibtexEntryTypes {
    /**
     * An article from a journal or magazine.
     *
     * Required fields: author, title, journal, year.
     * Optional fields: volume, number, pages, month, note.
     */
    public static final EntryType ARTICLE =
            new BibtexEntryType() {
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
     *
     * Required fields: author or editor, title, publisher, year.
     * Optional fields: volume or number, series, address, edition, month, note.
     */
    public static final EntryType BOOK =
            new BibtexEntryType() {
                {
                    addAllRequired("title", "publisher", "year", "author/editor");
                    addAllOptional("volume", "number", "series", "address", "edition", "month",
                            "note");
                }

                @Override
                public String getName() {
                    return "Book";
                }
            };

    /**
     * A work that is printed and bound, but without a named publisher or sponsoring institution.
     *
     * Required field: title.
     * Optional fields: author, howpublished, address, month, year, note.
     */
    public static final EntryType BOOKLET =
            new BibtexEntryType() {
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
     *
     * Required fields: author, title, booktitle, year.
     * Optional fields: editor, volume or number, series, pages, address, month, organization, publisher, note.
     */
    public static final EntryType CONFERENCE =
            new BibtexEntryType() {
                {
                    addAllOptional("editor", "volume", "number", "series", "pages",
                            "address", "month", "organization", "publisher", "note");
                    addAllRequired("author", "title", "booktitle", "year");
                }

                @Override
                public String getName() {
                    return "Conference";
                }
            };

    /**
     * A part of a book, which may be a chapter (or section or whatever) and/or a range of pages.
     *
     * Required fields: author or editor, title, chapter and/or pages, publisher, year.
     * Optional fields: volume or number, series, type, address, edition, month, note.
     */
    public static final EntryType INBOOK =
            new BibtexEntryType() {
                {
                    addAllOptional("volume", "number", "series", "type", "address", "edition",
                            "month", "note");
                    addAllRequired("chapter/pages", "title", "publisher", "year", "author/editor");
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
    public static final EntryType INCOLLECTION =
            new BibtexEntryType() {
                {
                    addAllRequired("author", "title", "booktitle", "publisher", "year");
                    addAllOptional("editor", "volume", "number", "series", "type", "chapter",
                            "pages", "address", "edition", "month", "note");
                }

                @Override
                public String getName() {
                    return "InCollection";
                }
            };

    /**
     * An article in a conference proceedings.
     *
     * Required fields: author, title, booktitle, year.
     * Optional fields: editor, volume or number, series, pages, address, month, organization, publisher, note.
     */
    public static final EntryType INPROCEEDINGS =
            new BibtexEntryType() {
                {
                    addAllOptional("editor", "volume", "number", "series", "pages",
                            "address", "month", "organization", "publisher", "note");
                    addAllRequired("author", "title", "booktitle", "year");
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
    public static final EntryType MANUAL =
            new BibtexEntryType() {

                {
                    addAllOptional("author", "organization", "address", "edition",
                            "month", "year", "note");
                    addAllRequired("title");
                }

                @Override
                public String getName() {
                    return "Manual";
                }
            };

    /**
     * A Master's thesis.
     *
     * Required fields: author, title, school, year.
     * Optional fields: type, address, month, note.
     */
    public static final EntryType MASTERSTHESIS =
            new BibtexEntryType() {

                {
                    addAllOptional("type", "address", "month", "note");
                    addAllRequired("author", "title", "school", "year");
                }

                @Override
                public String getName() {
                    return "MastersThesis";
                }
            };

    /**
     * Use this type when nothing else fits.
     *
     * Required fields: none.
     * Optional fields: author, title, howpublished, month, year, note.
     */
    public static final EntryType MISC =
            new BibtexEntryType() {

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
     *
     * Required fields: author, title, school, year.
     * Optional fields: type, address, month, note.
     */
    public static final EntryType PHDTHESIS =
            new BibtexEntryType() {

                {
                    addAllOptional("type", "address", "month", "note");
                    addAllRequired("author", "title", "school", "year");
                }

                @Override
                public String getName() {
                    return "PhdThesis";
                }
            };

    /**
     * The proceedings of a conference.
     *
     * Required fields: title, year.
     * Optional fields: editor, volume or number, series, address, month, organization, publisher, note.
     */
    public static final EntryType PROCEEDINGS =
            new BibtexEntryType() {

                {
                    addAllOptional("editor", "volume", "number", "series", "address",
                            "publisher", "note", "month", "organization");
                    addAllRequired("title", "year");
                }

                @Override
                public String getName() {
                    return "Proceedings";
                }
            };

    /**
     * A report published by a school or other institution, usually numbered within a series.
     *
     * Required fields: author, title, institution, year.
     * Optional fields: type, number, address, month, note.
     */
    public static final EntryType TECHREPORT =
            new BibtexEntryType() {
                {
                    addAllOptional("type", "number", "address", "month", "note");
                    addAllRequired("author", "title", "institution", "year");
                }

                @Override
                public String getName() {
                    return "TechReport";
                }
            };

    /**
     * A document having an author and title, but not formally published.
     *
     * Required fields: author, title, note.
     * Optional fields: month, year.
     */
    public static final EntryType UNPUBLISHED =
            new BibtexEntryType() {
                {
                    addAllOptional("month", "year");
                    addAllRequired("author", "title", "note");
                }

                @Override
                public String getName() {
                    return "Unpublished";
                }
            };


    /**
     * TODO: internal type
     */
    public static final EntryType OTHER =
            new BibtexEntryType() {

                @Override
                public String getName() {
                    return "Other";
                }
            };

    /**
     * This type is provided as an emergency choice if the user makes
     * customization changes that remove the type of an entry.
     *
     * TODO: internal type, merge with @Other?
     */
    public static final EntryType TYPELESS =
            new BibtexEntryType() {

                @Override
                public String getName() {
                    return "Typeless";
                }
            };

    public static final List<EntryType> ALL = Arrays.asList(
            ARTICLE, INBOOK, BOOK, BOOKLET, INCOLLECTION, CONFERENCE, INPROCEEDINGS,PROCEEDINGS,
            MANUAL, MASTERSTHESIS, PHDTHESIS, TECHREPORT, UNPUBLISHED, MISC
    );
}
