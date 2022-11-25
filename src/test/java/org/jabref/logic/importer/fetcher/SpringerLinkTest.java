package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import javafx.collections.FXCollections;

import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.support.DisabledOnCIServer;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@FetcherTest
public class SpringerLinkTest {

    private final ImporterPreferences importerPreferences = mock(ImporterPreferences.class);
    private SpringerLink finder;
    private BibEntry entry;

    @BeforeEach
    public void setUp() {
        when(importerPreferences.getApiKeys()).thenReturn(FXCollections.emptyObservableSet());
        finder = new SpringerLink(importerPreferences);
        entry = new BibEntry();
    }

    @Test
    public void rejectNullParameter() {
        assertThrows(NullPointerException.class, () -> finder.findFullText(null));
    }

    @Test
    public void doiNotPresent() throws IOException {
        assertEquals(Optional.empty(), finder.findFullText(entry));
    }

    @DisabledOnCIServer("Disable on CI Server to not hit the API call limit")
    @Test
    public void findByDOI() throws IOException {
        entry.setField(StandardField.DOI, "10.1186/s13677-015-0042-8");
        assertEquals(
                Optional.of(new URL("http://link.springer.com/content/pdf/10.1186/s13677-015-0042-8.pdf")),
                finder.findFullText(entry));
    }

    @DisabledOnCIServer("Disable on CI Server to not hit the API call limit")
    @Test
    public void notFoundByDOI() throws IOException {
        entry.setField(StandardField.DOI, "10.1186/unknown-doi");

        assertEquals(Optional.empty(), finder.findFullText(entry));
    }

    @Test
    void entityWithoutDoi() throws IOException {
        assertEquals(Optional.empty(), finder.findFullText(entry));
    }

    @Test
    void trustLevel() {
        assertEquals(TrustLevel.PUBLISHER, finder.getTrustLevel());
    }
}
