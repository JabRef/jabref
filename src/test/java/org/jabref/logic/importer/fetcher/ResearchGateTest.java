package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import oracle.jdbc.logging.annotations.DefaultLevel;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.support.DisabledOnCIServer;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@FetcherTest
public class ResearchGateTest {

    private static final String URL_PDF = "https://www.researchgate.net/profile/Abdurrazzak-Gehani/publication/4207355_Paranoid_a_global_secure_file_access_control_system/links/5457747d0cf2cf516480995e/Paranoid-a-global-secure-file-access-control-system.pdf";
    private final String URL_PAGE = "https://www.researchgate.net/publication/4207355_Paranoid_a_global_secure_file_access_control_system";
    private final String URL_TITLE = "https://www.researchgate.net/search/publication?q=Paranoid%253A%2Ba%2Bglobal%2Bsecure%2Bfile%2Baccess%2Bcontrol%2Bsystem&_sg=hAMDrqbG_CEiuRGM6Rk6Ljc__OnhF8x3j3y5p4vnpRQ5zkYbGq6Y5zOhHyM0NzH0-CVuHnSPYkEbcb0xVaZPM-DQ6cA";
    private ResearchGate finder;
    private BibEntry entry;

    @BeforeEach
    public void setUp() {
        finder = new ResearchGate();
        entry = new BibEntry();
        entry.setField(StandardField.DOI, "10.1109/CSAC.2005.42");
        entry.setField(StandardField.TITLE, "Paranoid: a global secure file access control system");
    }

    @Test
    void getDocumentByDOI() throws IOException {
        String source = finder.getURLByDoi(entry.getDOI().get());
        assertEquals(URL_PAGE, source);
    }

    @Test
    void getDocumentByTitle() throws IOException {
        String source = finder.getURLByTitle(entry.getTitle().get());
        assertTrue(source.startsWith(URL_PAGE));
    }

    @Test
    @DisabledOnCIServer("CI server is unreliable")
    void fullTextFoundByDOI() throws IOException {
        assertEquals(
                Optional.of(new URL(URL_PDF)),
                finder.findFullText(entry)
        );
    }

    @Test
    @DisabledOnCIServer("CI server is unreliable")
    void fullTextNotFoundByDOI() throws IOException {
        BibEntry entry2 = new BibEntry();
        entry2.setField(StandardField.DOI, "10.1021/bk-2006-WWW.ch014");

        assertEquals(Optional.empty(), finder.findFullText(entry2));
    }

    @Test
    void trustLevel() {
        assertEquals(TrustLevel.PUBLISHER, finder.getTrustLevel());
    }
}
