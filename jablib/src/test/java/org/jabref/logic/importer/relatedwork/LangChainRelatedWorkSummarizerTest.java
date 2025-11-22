package org.jabref.logic.importer.relatedwork;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class LangChainRelatedWorkSummarizerTest {

    private BibEntry sampleEntry() {
        BibEntry e = new BibEntry();
        e.setCitationKey("LunaOstos_2024");
        e.setField(StandardField.TITLE, "Social Life Cycle Assessment in the Chocolate Industry");
        return e;
    }

    @Test
    public void delegatesToClientAndBuildsPromptFromSnippets() {
        AtomicInteger calls = new AtomicInteger(0);

        LangChainRelatedWorkSummarizer.Client fakeClient = prompt -> {
            calls.incrementAndGet();

            // Very lightweight checks: prompt should contain key / title / snippets.
            assertTrue(prompt.contains("LunaOstos_2024"));
            assertTrue(prompt.contains("Social Life Cycle Assessment in the Chocolate Industry"));
            assertTrue(prompt.contains("first snippet"));
            assertTrue(prompt.contains("second snippet"));

            return "compressed summary";
        };

        LangChainRelatedWorkSummarizer summarizer =
                new LangChainRelatedWorkSummarizer(fakeClient);

        Optional<String> result = summarizer.summarize(
                List.of("first snippet", "second snippet"),
                sampleEntry(),
                200
        );

        assertTrue(result.isPresent());
        assertEquals("compressed summary", result.get());
        assertEquals(1, calls.get());
    }

    @Test
    public void returnsEmptyWhenNoSnippets() {
        LangChainRelatedWorkSummarizer.Client neverCalledClient = prompt -> {
            fail("Client should not be called when there are no snippets");
            return "";
        };

        LangChainRelatedWorkSummarizer summarizer =
                new LangChainRelatedWorkSummarizer(neverCalledClient);

        Optional<String> result = summarizer.summarize(
                List.of(),
                sampleEntry(),
                200
        );

        assertFalse(result.isPresent());
    }

    @Test
    public void trimsEmptyModelOutputToEmptyOptional() {
        LangChainRelatedWorkSummarizer.Client emptyClient = prompt -> "   ";

        LangChainRelatedWorkSummarizer summarizer =
                new LangChainRelatedWorkSummarizer(emptyClient);

        Optional<String> result = summarizer.summarize(
                List.of("some snippet"),
                sampleEntry(),
                200
        );

        assertFalse(result.isPresent());
    }
}
