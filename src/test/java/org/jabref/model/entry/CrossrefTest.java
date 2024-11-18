package org.jabref.model.entry;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.IEEETranEntryType;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrossrefTest {

    private BibEntry parent;
    private BibEntry child;
    private BibDatabase db;

    @BeforeEach
    void setup() {
        parent = new BibEntry(StandardEntryType.Proceedings);
        parent.setCitationKey("parent");
        parent.setField(StandardField.IDS, "parent_IDS");
        parent.setField(StandardField.XREF, "parent_XREF");
        parent.setField(StandardField.ENTRYSET, "parent_ENTRYSET");
        parent.setField(StandardField.RELATED, "parent_RELATED");
        parent.setField(StandardField.SORTKEY, "parent_SORTKEY");

        parent.setField(StandardField.AUTHOR, "parent_AUTHOR");

        parent.setField(StandardField.TITLE, "parent_TITLE");
        parent.setField(StandardField.SUBTITLE, "parent_SUBTITLE");
        parent.setField(StandardField.TITLEADDON, "parent_TITLEADDON");
        parent.setField(StandardField.SHORTTITLE, "parent_SHORTTITLE");

        child = new BibEntry(StandardEntryType.InProceedings);
        child.setField(StandardField.CROSSREF, "parent");

        db = new BibDatabase(Arrays.asList(parent, child));
    }

    @ParameterizedTest
    @EnumSource(value = StandardField.class, names = {"IDS", "XREF", "ENTRYSET", "RELATED", "SORTKEY"})
    void forbiddenFields(StandardField field) {
        Optional<String> childField = child.getResolvedFieldOrAlias(field, db);
        assertTrue(childField.isEmpty());
    }

    @ParameterizedTest
    @MethodSource("authorInheritanceSource")
    void authorInheritance(EntryType parentType, EntryType childType) {
        parent.setType(parentType);
        child.setType(childType);

        assertEquals(
                parent.getResolvedFieldOrAlias(StandardField.AUTHOR, null),
                child.getResolvedFieldOrAlias(StandardField.AUTHOR, db)
        );

        assertEquals(
                parent.getResolvedFieldOrAlias(StandardField.AUTHOR, null),
                child.getResolvedFieldOrAlias(StandardField.BOOKAUTHOR, db)
        );
    }

    private static Stream<Arguments> authorInheritanceSource() {
        return Stream.of(
                Arguments.of(StandardEntryType.MvBook, StandardEntryType.InBook),
                Arguments.of(StandardEntryType.MvBook, StandardEntryType.BookInBook),
                Arguments.of(StandardEntryType.MvBook, StandardEntryType.SuppBook),
                Arguments.of(StandardEntryType.Book, StandardEntryType.InBook),
                Arguments.of(StandardEntryType.Book, StandardEntryType.BookInBook),
                Arguments.of(StandardEntryType.Book, StandardEntryType.SuppBook)
        );
    }

    @ParameterizedTest
    @MethodSource("mainTitleInheritanceSource")
    void mainTitleInheritance(EntryType parentType, EntryType childType) {
        parent.setType(parentType);
        child.setType(childType);

        assertEquals(
                parent.getResolvedFieldOrAlias(StandardField.TITLE, null),
                child.getResolvedFieldOrAlias(StandardField.MAINTITLE, db)
        );
        assertEquals(
                parent.getResolvedFieldOrAlias(StandardField.SUBTITLE, null),
                child.getResolvedFieldOrAlias(StandardField.MAINSUBTITLE, db)
        );
        assertEquals(
                parent.getResolvedFieldOrAlias(StandardField.TITLEADDON, null),
                child.getResolvedFieldOrAlias(StandardField.MAINTITLEADDON, db)
        );
    }

    private static Stream<Arguments> mainTitleInheritanceSource() {
        return Stream.of(
                Arguments.of(StandardEntryType.MvBook, StandardEntryType.Book),
                Arguments.of(StandardEntryType.MvBook, StandardEntryType.InBook),
                Arguments.of(StandardEntryType.MvBook, StandardEntryType.BookInBook),
                Arguments.of(StandardEntryType.MvBook, StandardEntryType.SuppBook),
                Arguments.of(StandardEntryType.MvCollection, StandardEntryType.Collection),
                Arguments.of(StandardEntryType.MvCollection, StandardEntryType.InCollection),
                Arguments.of(StandardEntryType.MvCollection, StandardEntryType.SuppCollection),
                Arguments.of(StandardEntryType.MvProceedings, StandardEntryType.Proceedings),
                Arguments.of(StandardEntryType.MvProceedings, StandardEntryType.InProceedings),
                Arguments.of(StandardEntryType.MvReference, StandardEntryType.Reference),
                Arguments.of(StandardEntryType.MvReference, StandardEntryType.InReference)
        );
    }

    @ParameterizedTest
    @MethodSource("bookTitleInheritanceSource")
    void bookTitleInheritance(EntryType parentType, EntryType childType) {
        parent.setType(parentType);
        child.setType(childType);

        assertEquals(
                parent.getResolvedFieldOrAlias(StandardField.TITLE, null),
                child.getResolvedFieldOrAlias(StandardField.BOOKTITLE, db)
        );
        assertEquals(
                parent.getResolvedFieldOrAlias(StandardField.SUBTITLE, null),
                child.getResolvedFieldOrAlias(StandardField.BOOKSUBTITLE, db)
        );
        assertEquals(
                parent.getResolvedFieldOrAlias(StandardField.TITLEADDON, null),
                child.getResolvedFieldOrAlias(StandardField.BOOKTITLEADDON, db)
        );
    }

    private static Stream<Arguments> bookTitleInheritanceSource() {
        return Stream.of(
                Arguments.of(StandardEntryType.Book, StandardEntryType.InBook),
                Arguments.of(StandardEntryType.Book, StandardEntryType.BookInBook),
                Arguments.of(StandardEntryType.Book, StandardEntryType.SuppBook),
                Arguments.of(StandardEntryType.Collection, StandardEntryType.InCollection),
                Arguments.of(StandardEntryType.Collection, StandardEntryType.SuppCollection),
                Arguments.of(StandardEntryType.Reference, StandardEntryType.InReference),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings)
        );
    }

    @ParameterizedTest
    @MethodSource("journalTitleInheritanceSource")
    void journalTitleInheritance(EntryType parentType, EntryType childType) {
        parent.setType(parentType);
        child.setType(childType);

        assertEquals(
                parent.getResolvedFieldOrAlias(StandardField.TITLE, null),
                child.getResolvedFieldOrAlias(StandardField.JOURNALTITLE, db)
        );
        assertEquals(
                parent.getResolvedFieldOrAlias(StandardField.SUBTITLE, null),
                child.getResolvedFieldOrAlias(StandardField.JOURNALSUBTITLE, db)
        );
    }

    private static Stream<Arguments> journalTitleInheritanceSource() {
        return Stream.of(
                Arguments.of(IEEETranEntryType.Periodical, StandardEntryType.Article),
                Arguments.of(IEEETranEntryType.Periodical, StandardEntryType.SuppPeriodical)
        );
    }

    @ParameterizedTest
    @MethodSource("noTitleInheritanceSource")
    void noTitleInheritance(EntryType parentType, EntryType childType) {
        parent.setType(parentType);
        child.setType(childType);

        assertTrue(child.getResolvedFieldOrAlias(StandardField.TITLE, db).isEmpty());
        assertTrue(child.getResolvedFieldOrAlias(StandardField.SUBTITLE, db).isEmpty());
        assertTrue(child.getResolvedFieldOrAlias(StandardField.TITLEADDON, db).isEmpty());
        assertTrue(child.getResolvedFieldOrAlias(StandardField.SHORTTITLE, db).isEmpty());
    }

    private static Stream<Arguments> noTitleInheritanceSource() {
        return Stream.of(
                Arguments.of(StandardEntryType.MvBook, StandardEntryType.Book),
                Arguments.of(StandardEntryType.MvBook, StandardEntryType.InBook),
                Arguments.of(StandardEntryType.MvBook, StandardEntryType.BookInBook),
                Arguments.of(StandardEntryType.MvBook, StandardEntryType.SuppBook),
                Arguments.of(StandardEntryType.MvCollection, StandardEntryType.Collection),
                Arguments.of(StandardEntryType.MvCollection, StandardEntryType.InCollection),
                Arguments.of(StandardEntryType.MvCollection, StandardEntryType.SuppCollection),
                Arguments.of(StandardEntryType.MvProceedings, StandardEntryType.Proceedings),
                Arguments.of(StandardEntryType.MvProceedings, StandardEntryType.InProceedings),
                Arguments.of(StandardEntryType.MvReference, StandardEntryType.Reference),
                Arguments.of(StandardEntryType.MvReference, StandardEntryType.InReference),
                Arguments.of(StandardEntryType.Book, StandardEntryType.InBook),
                Arguments.of(StandardEntryType.Book, StandardEntryType.BookInBook),
                Arguments.of(StandardEntryType.Book, StandardEntryType.SuppBook),
                Arguments.of(StandardEntryType.Collection, StandardEntryType.InCollection),
                Arguments.of(StandardEntryType.Collection, StandardEntryType.SuppCollection),
                Arguments.of(StandardEntryType.Reference, StandardEntryType.InReference),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings)
        );
    }

    @ParameterizedTest
    @MethodSource("sameNameInheritance")
    void sameNameInheritance(EntryType parentType, EntryType childType, StandardField field) {
        parent.setType(parentType);
        child.setType(childType);

        assertTrue(parent.setField(field, "parent_FIELD").isPresent());

        assertEquals(
                parent.getResolvedFieldOrAlias(field, null),
                child.getResolvedFieldOrAlias(field, db)
        );
    }

    private static Stream<Arguments> sameNameInheritance() {
        return Stream.of(
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.ABSTRACT),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.ADDENDUM),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.ADDRESS),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.AFTERWORD),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.ANNOTE),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.ANNOTATION),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.ANNOTATOR),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.ARCHIVEPREFIX),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.ASSIGNEE),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.AUTHOR),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.BOOKPAGINATION),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.CHAPTER),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.COMMENTATOR),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.COMMENT),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.DATE),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.DAY),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.DAYFILED),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.DOI),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.EDITION),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.EDITOR),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.EDITORA),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.EDITORB),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.EDITORC),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.EDITORTYPE),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.EDITORATYPE),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.EDITORBTYPE),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.EDITORCTYPE),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.EID),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.EPRINT),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.EPRINTCLASS),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.EPRINTTYPE),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.EVENTDATE),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.EVENTTITLE),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.EVENTTITLEADDON),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.FILE),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.FOREWORD),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.FOLDER),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.GENDER),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.HOLDER),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.HOWPUBLISHED),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.INSTITUTION),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.INTRODUCTION),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.ISBN),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.ISRN),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.ISSN),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.ISSUE),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.ISSUETITLE),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.ISSUESUBTITLE),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.JOURNAL),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.KEY),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.KEYWORDS),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.LANGUAGE),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.LOCATION),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.MONTH),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.MONTHFILED),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.NAMEADDON),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.NATIONALITY),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.NOTE),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.NUMBER),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.ORGANIZATION),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.ORIGDATE),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.ORIGLANGUAGE),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.PAGES),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.PAGETOTAL),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.PAGINATION),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.PART),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.PDF),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.PMID),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.PS),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.PUBLISHER),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.PUBSTATE),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.PRIMARYCLASS),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.REPORTNO),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.REVIEW),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.REVISION),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.SCHOOL),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.SERIES),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.SHORTAUTHOR),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.SHORTEDITOR),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.SORTNAME),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.TRANSLATOR),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.TYPE),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.URI),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.URL),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.URLDATE),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.VENUE),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.VERSION),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.VOLUME),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.VOLUMES),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.YEAR),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.YEARFILED),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.MR_NUMBER),
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings, StandardField.XDATA)
        );
    }
}
