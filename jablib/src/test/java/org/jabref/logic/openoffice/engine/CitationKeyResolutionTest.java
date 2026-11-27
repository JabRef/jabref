package org.jabref.logic.openoffice.engine;

import java.util.List;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.openoffice.style.CitationType;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.jabref.logic.openoffice.engine.RendererTestData.group;
import static org.jabref.logic.openoffice.engine.RendererTestData.request;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@NullMarked
class CitationKeyResolutionTest {

    @Test
    void flatteningSelectedKeysDoesNotReadOtherResolvedEntriesFields() throws RenderException {
        BibEntry needed = new BibEntry().withCitationKey("needed").withField(StandardField.TITLE, "Needed title");
        BibEntry untouched = spy(new BibEntry().withCitationKey("untouched").withField(StandardField.TITLE, "Untouched title"));
        CitationKeyResolution resolution = new CitationKeyResolution(request(
                List.of(group("group", CitationType.AUTHORYEAR_PAR, "needed", "untouched")), List.of(needed, untouched)));
        clearInvocations(untouched);

        List<BibEntry> flattened = resolution.flattenedEntries(List.of("needed"));

        assertEquals(List.of(Optional.of("needed")), flattened.stream().map(BibEntry::getCitationKey).toList());
        assertEquals(Optional.of("Needed title"), flattened.getFirst().getField(StandardField.TITLE));
        verify(untouched, never()).getFields();
    }
}
