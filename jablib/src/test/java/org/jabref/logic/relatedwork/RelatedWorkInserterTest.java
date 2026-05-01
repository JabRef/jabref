package org.jabref.logic.relatedwork;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.os.OS;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.UserSpecificCommentField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class RelatedWorkInserterTest {

    @Test
    void insertMatchedRelatedWorkWritesUserSpecificCommentField() {
        RelatedWorkInserter inserter = new RelatedWorkInserter();
        BibEntry sourceEntry = new BibEntry(StandardEntryType.Article).withCitationKey("LunaOstos_2024");
        BibEntry matchedLibraryEntry = new BibEntry(StandardEntryType.Misc).withCitationKey("Agency2021");
        RelatedWorkMatchResult matchResult = new RelatedWorkMatchResult(
                "Colombia is a middle-income country with a population of approximately 50 million.",
                "[1]",
                Optional.of(new BibEntry(StandardEntryType.Misc)),
                Optional.of(matchedLibraryEntry)
        );

        List<RelatedWorkInsertionResult> insertionResults = inserter.insertMatchedRelatedWork(sourceEntry, List.of(matchResult), "koppor");
        RelatedWorkInsertionResult.Inserted insertionResult = assertInstanceOf(RelatedWorkInsertionResult.Inserted.class, insertionResults.getFirst());

        assertEquals(1, insertionResults.size());
        assertEquals(matchResult, insertionResult.matchResult());
        assertEquals(
                matchedLibraryEntry,
                insertionResult.fieldChange().getEntry()
        );
        assertEquals("[LunaOstos_2024]: Colombia is a middle-income country with a population of approximately 50 million.",
                matchedLibraryEntry.getField(new UserSpecificCommentField("koppor")).orElseThrow());
    }

    @Test
    void insertMatchedRelatedWorkAppendsToExistingUserSpecificCommentField() {
        RelatedWorkInserter inserter = new RelatedWorkInserter();
        BibEntry sourceEntry = new BibEntry(StandardEntryType.Article).withCitationKey("LunaOstos_2024");
        UserSpecificCommentField userSpecificCommentField = new UserSpecificCommentField("koppor");
        BibEntry matchedLibraryEntry = new BibEntry(StandardEntryType.Misc)
                .withCitationKey("Agency2021")
                .withField(userSpecificCommentField, "[test]: blahblah");
        RelatedWorkMatchResult matchResult = new RelatedWorkMatchResult(
                "Colombia is a middle-income country with a population of approximately 50 million.",
                "[1]",
                Optional.of(new BibEntry(StandardEntryType.Misc)),
                Optional.of(matchedLibraryEntry)
        );

        List<RelatedWorkInsertionResult> insertionResults = inserter.insertMatchedRelatedWork(sourceEntry, List.of(matchResult), "koppor");

        assertInstanceOf(RelatedWorkInsertionResult.Inserted.class, insertionResults.getFirst());
        assertEquals("[test]: blahblah" + OS.NEWLINE + OS.NEWLINE + "[LunaOstos_2024]: Colombia is a middle-income country with a population of approximately 50 million.",
                matchedLibraryEntry.getField(userSpecificCommentField).orElseThrow());
    }
}
