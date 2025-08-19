package org.jabref.http.server.cayw.format;

import java.util.List;
import java.util.Optional;

import org.jabref.http.server.cayw.CAYWQueryParams;
import org.jabref.http.server.cayw.gui.CAYWEntry;
import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class PandocFormatterTest {

    private CAYWEntry stubEntry(String key) {
        BibEntry bib = mock(BibEntry.class);
        when(bib.getCitationKey()).thenReturn(Optional.of(key));

        CAYWEntry cayw = mock(CAYWEntry.class);
        when(cayw.bibEntry()).thenReturn(bib);
        return cayw;
    }

    @Test
    void testGetFormatNames() {
        PandocFormatter f = new PandocFormatter();
        assertEquals(List.of("pandoc", "markdown"), f.getFormatNames());
    }

    @Test
    @DisplayName("Default command ⇒ '@k1; @k2'")
    void testDefaultFormat() {
        PandocFormatter f = new PandocFormatter();

        CAYWQueryParams qp = mock(CAYWQueryParams.class);
        when(qp.getCommand()).thenReturn(null);

        String out = f.format(qp, List.of(stubEntry("k1"), stubEntry("k2")));
        assertEquals("@k1; @k2", out);
    }

    @Test
    @DisplayName("parencite command ⇒ '[@k1][@k2]…'")
    void testParenciteFormat() {
        PandocFormatter f = new PandocFormatter();

        CAYWQueryParams qp = mock(CAYWQueryParams.class);
        when(qp.getCommand()).thenReturn("parencite");

        String out = f.format(qp, List.of(stubEntry("k1"), stubEntry("k2")));
        assertEquals("[@k1][@k2]", out);
    }
}
