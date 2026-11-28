package org.jabref.logic.formatter.casechanger;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.formatter.Formatter;
import org.jabref.logic.formatter.Formatters;
import org.jabref.logic.protectedterms.ProtectedTermsList;
import org.jabref.logic.protectedterms.ProtectedTermsLoader;
import org.jabref.logic.protectedterms.ProtectedTermsPreferences;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Tests in addition to the general tests from [org.jabref.logic.formatter.FormatterTest]
class ProtectTermsFormatterTest {

    private ProtectTermsFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new ProtectTermsFormatter(
                new ProtectedTermsLoader(new ProtectedTermsPreferences(ProtectedTermsLoader.getInternalLists(),
                        List.of(), List.of(), List.of())));
    }

    @Test
    void singleWord() {
        assertEquals("{VLSI}", formatter.format("VLSI"));
    }

    @Test
    void doNotProtectAlreadyProtected() {
        assertEquals("{VLSI}", formatter.format("{VLSI}"));
    }

    @Test
    void caseSensitivity() {
        assertEquals("VLsI", formatter.format("VLsI"));
    }

    @Test
    void formatExample() {
        assertEquals("In {CDMA}", formatter.format(formatter.getExampleInput()));
    }

    @Test
    void correctOrderingOfTerms() {
        assertEquals("{3GPP} {3G}", formatter.format("3GPP 3G"));
    }

    @Test
    void test() {
        assertEquals("{VLSI} {VLSI}", formatter.format("VLSI {VLSI}"));
        assertEquals("{BPEL}", formatter.format("{BPEL}"));
        assertEquals("{Testing {BPEL} Engine Performance: A Survey}",
                formatter.format("{Testing BPEL Engine Performance: A Survey}"));
    }

    @Test
    void noArgConstructorUsesDefaultProtectedTerms() {
        assertEquals("In {CDMA}", new ProtectTermsFormatter().format("In CDMA"));
    }

    @Test
    void withProtectedTermsLoaderUsesBoundLoader(@TempDir Path tempDir) {
        ProtectedTermsLoader customLoader = new ProtectedTermsLoader(
                new ProtectedTermsPreferences(List.of(), List.of(), ProtectedTermsLoader.getInternalLists(), List.of()));
        ProtectedTermsList customList = customLoader.addNewProtectedTermsList(
                "Custom", tempDir.resolve("custom.terms").toString(), true);
        customList.addProtectedTerm("CustomTerm");

        ProtectTermsFormatter unbound = new ProtectTermsFormatter();
        Formatter bound = unbound.withProtectedTermsLoader(customLoader);

        assertEquals("In {CustomTerm}", bound.format("In CustomTerm"));
        assertEquals("In CustomTerm", unbound.format("In CustomTerm"));
    }

    @Test
    void registeredInFormattersUnderProtectTermsKey() {
        Optional<Formatter> resolved = Formatters.getFormatterForKey("protect_terms");
        assertEquals(Optional.of(new ProtectTermsFormatter()), resolved);
        assertEquals("In {CDMA}", resolved.orElseThrow().format("In CDMA"));
    }
}
