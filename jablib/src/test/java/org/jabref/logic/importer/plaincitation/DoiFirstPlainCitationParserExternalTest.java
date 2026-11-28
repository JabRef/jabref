package org.jabref.logic.importer.plaincitation;

import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.fetcher.DoiFetcher;
import org.jabref.model.entry.field.StandardField;
import org.jabref.support.ExternalServicesTest;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

@ExternalServicesTest
class DoiFirstPlainCitationParserExternalTest {

    @Test
    void resolvesCitationUsingDoiFetcher() throws FetcherException {
        PlainCitationParser delegate = mock(PlainCitationParser.class);
        DoiFetcher fetcher = new DoiFetcher(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS));
        DoiFirstPlainCitationParser parser = new DoiFirstPlainCitationParser(delegate, fetcher);

        var entry = parser.parsePlainCitation("Decker et al. https://doi.org/10.1109/ICWS.2007.59");

        assertEquals(Optional.of("BPEL4Chor: Extending BPEL for Modeling Choreographies"),
                entry.flatMap(result -> result.getField(StandardField.TITLE)));
        verifyNoInteractions(delegate);
    }
}
