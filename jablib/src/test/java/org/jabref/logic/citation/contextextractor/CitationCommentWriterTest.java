package org.jabref.logic.citation.contextextractor;

import java.util.List;
import java.util.Optional;

import org.jabref.model.citation.CitationContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UserSpecificCommentField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CitationCommentWriterTest {

    private CitationCommentWriter defaultWriter;
    private CitationCommentWriter userWriter;
    private BibEntry entry;

    @BeforeEach
    void setUp() {
        defaultWriter = new CitationCommentWriter();
        userWriter = new CitationCommentWriter("testuser");
        entry = new BibEntry();
        entry.setCitationKey("TestEntry2024");
    }

    @Test
    void defaultWriterUsesStandardCommentField() {
        assertEquals(StandardField.COMMENT, defaultWriter.getCommentField());
        assertTrue(defaultWriter.getUsername().isEmpty());
    }

    @Test
    void userWriterUsesUserSpecificField() {
        assertInstanceOf(UserSpecificCommentField.class, userWriter.getCommentField());
        assertEquals(Optional.of("testuser"), userWriter.getUsername());
    }

    @Test
    void formatContext() {
        CitationContext context = new CitationContext(
                "(Smith 2020)",
                "This paper discusses machine learning techniques.",
                "SourcePaper2024"
        );

        String formatted = defaultWriter.formatContext(context);

        assertEquals("[SourcePaper2024]: This paper discusses machine learning techniques.", formatted);
    }

    @Test
    void formatMultipleContexts() {
        List<CitationContext> contexts = List.of(
                new CitationContext("(Smith 2020)", "First context.", "Source1"),
                new CitationContext("(Jones 2019)", "Second context.", "Source2")
        );

        String formatted = defaultWriter.formatContexts(contexts);

        assertTrue(formatted.contains("[Source1]: First context."));
        assertTrue(formatted.contains("[Source2]: Second context."));
        assertTrue(formatted.contains("\n\n")); // Separator
    }

    @Test
    void addContextToEntry() {
        CitationContext context = new CitationContext(
                "(Smith 2020)",
                "Important finding about neural networks.",
                "SourcePaper"
        );

        boolean added = defaultWriter.addContextToEntry(entry, context);

        assertTrue(added);
        Optional<String> comment = entry.getField(StandardField.COMMENT);
        assertTrue(comment.isPresent());
        assertTrue(comment.get().contains("[SourcePaper]:"));
        assertTrue(comment.get().contains("neural networks"));
    }

    @Test
    void addContextToEntryWithExistingComment() {
        entry.setField(StandardField.COMMENT, "Existing comment text.");

        CitationContext context = new CitationContext(
                "(Smith 2020)",
                "New context.",
                "Source"
        );

        defaultWriter.addContextToEntry(entry, context);

        String comment = entry.getField(StandardField.COMMENT).orElse("");
        assertTrue(comment.contains("Existing comment text."));
        assertTrue(comment.contains("[Source]: New context."));
    }

    @Test
    void addDuplicateContextReturnsFalse() {
        CitationContext context = new CitationContext(
                "(Smith 2020)",
                "Same context text.",
                "Source"
        );

        boolean first = defaultWriter.addContextToEntry(entry, context);
        boolean second = defaultWriter.addContextToEntry(entry, context);

        assertTrue(first);
        assertFalse(second);
    }

    @Test
    void addContextsToEntry() {
        List<CitationContext> contexts = List.of(
                new CitationContext("(Smith 2020)", "First context.", "Source1"),
                new CitationContext("(Jones 2019)", "Second context.", "Source2"),
                new CitationContext("(Brown 2021)", "Third context.", "Source3")
        );

        int added = defaultWriter.addContextsToEntry(entry, contexts);

        assertEquals(3, added);
        assertEquals(3, defaultWriter.countContexts(entry));
    }

    @Test
    void removeContextsFromSource() {
        CitationContext ctx1 = new CitationContext("(A)", "Context from source A.", "SourceA");
        CitationContext ctx2 = new CitationContext("(B)", "Context from source B.", "SourceB");

        defaultWriter.addContextToEntry(entry, ctx1);
        defaultWriter.addContextToEntry(entry, ctx2);

        boolean removed = defaultWriter.removeContextsFromSource(entry, "SourceA");

        assertTrue(removed);
        assertFalse(defaultWriter.hasContextsFromSource(entry, "SourceA"));
        assertTrue(defaultWriter.hasContextsFromSource(entry, "SourceB"));
    }

    @Test
    void removeNonExistentSourceReturnsFalse() {
        boolean removed = defaultWriter.removeContextsFromSource(entry, "NonExistent");
        assertFalse(removed);
    }

    @Test
    void getContextsFromSource() {
        defaultWriter.addContextToEntry(entry, new CitationContext("(A)", "First from A.", "SourceA"));
        defaultWriter.addContextToEntry(entry, new CitationContext("(B)", "From B.", "SourceB"));
        defaultWriter.addContextToEntry(entry, new CitationContext("(A2)", "Second from A.", "SourceA"));

        List<String> contextsFromA = defaultWriter.getContextsFromSource(entry, "SourceA");

        assertEquals(2, contextsFromA.size());
        assertTrue(contextsFromA.stream().anyMatch(c -> c.contains("First from A")));
        assertTrue(contextsFromA.stream().anyMatch(c -> c.contains("Second from A")));
    }

    @Test
    void hasContextsFromSource() {
        assertFalse(defaultWriter.hasContextsFromSource(entry, "Source"));

        defaultWriter.addContextToEntry(entry, new CitationContext("(X)", "Context.", "Source"));

        assertTrue(defaultWriter.hasContextsFromSource(entry, "Source"));
    }

    @Test
    void clearComment() {
        defaultWriter.addContextToEntry(entry, new CitationContext("(X)", "Context.", "Source"));
        assertTrue(entry.getField(StandardField.COMMENT).isPresent());

        defaultWriter.clearComment(entry);

        assertTrue(entry.getField(StandardField.COMMENT).isEmpty());
    }

    @Test
    void countContexts() {
        assertEquals(0, defaultWriter.countContexts(entry));

        defaultWriter.addContextToEntry(entry, new CitationContext("(A)", "Context A.", "Source1"));
        assertEquals(1, defaultWriter.countContexts(entry));

        defaultWriter.addContextToEntry(entry, new CitationContext("(B)", "Context B.", "Source2"));
        assertEquals(2, defaultWriter.countContexts(entry));
    }

    @Test
    void userSpecificFieldIsolation() {
        CitationCommentWriter user1Writer = new CitationCommentWriter("user1");
        CitationCommentWriter user2Writer = new CitationCommentWriter("user2");

        user1Writer.addContextToEntry(entry, new CitationContext("(A)", "User1 context.", "Source"));
        user2Writer.addContextToEntry(entry, new CitationContext("(B)", "User2 context.", "Source"));

        assertTrue(user1Writer.hasContextsFromSource(entry, "Source"));
        assertTrue(user2Writer.hasContextsFromSource(entry, "Source"));

        List<String> user1Contexts = user1Writer.getContextsFromSource(entry, "Source");
        List<String> user2Contexts = user2Writer.getContextsFromSource(entry, "Source");

        assertEquals(1, user1Contexts.size());
        assertEquals(1, user2Contexts.size());
        assertTrue(user1Contexts.getFirst().contains("User1 context"));
        assertTrue(user2Contexts.getFirst().contains("User2 context"));
    }

    @Test
    void nullUsernameThrows() {
        assertThrows(NullPointerException.class, () -> new CitationCommentWriter(null));
    }

    @Test
    void blankUsernameThrows() {
        assertThrows(IllegalArgumentException.class, () -> new CitationCommentWriter("  "));
    }

    @Test
    void nullEntryThrows() {
        CitationContext context = new CitationContext("(X)", "Context.", "Source");
        assertThrows(NullPointerException.class, () -> defaultWriter.addContextToEntry(null, context));
    }

    @Test
    void nullContextThrows() {
        assertThrows(NullPointerException.class, () -> defaultWriter.addContextToEntry(entry, null));
    }

    @Test
    void emptyContextsListReturnsZero() {
        int added = defaultWriter.addContextsToEntry(entry, List.of());
        assertEquals(0, added);
    }

    @Test
    void nullContextsListReturnsZero() {
        int added = defaultWriter.addContextsToEntry(entry, null);
        assertEquals(0, added);
    }

    @Test
    void exactDuplicateContextNotAdded() {
        // Exact same context should be detected as duplicate
        defaultWriter.addContextToEntry(entry, new CitationContext(
                "(Smith 2020)",
                "This paper discusses important machine learning techniques.",
                "Source"
        ));

        boolean added = defaultWriter.addContextToEntry(entry, new CitationContext(
                "(Smith 2020)",
                "This paper discusses important machine learning techniques.",
                "Source"
        ));

        assertFalse(added);
    }

    @Test
    void differentContextsFromSameSourceAdded() {
        defaultWriter.addContextToEntry(entry, new CitationContext(
                "(Smith 2020)",
                "First unique context about machine learning.",
                "Source"
        ));

        boolean added = defaultWriter.addContextToEntry(entry, new CitationContext(
                "(Smith 2020)",
                "Second completely different context about deep learning.",
                "Source"
        ));

        assertTrue(added);
    }
}
