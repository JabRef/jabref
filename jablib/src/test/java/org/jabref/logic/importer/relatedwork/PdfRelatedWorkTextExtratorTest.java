package org.jabref.logic.importer.relatedwork;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfRelatedWorkTextExtractorTest {

    // Fake provider for tests (no real PDF parsing)
    static class FakePdfTextProvider implements PdfTextProvider {
        private final Optional<String> toReturn;
        private final boolean throwIo;

        FakePdfTextProvider(Optional<String> toReturn, boolean throwIo) {
            this.toReturn = toReturn;
            this.throwIo = throwIo;
        }

        @Override
        public Optional<String> extractPlainText(Path pdf) throws IOException {
            if (throwIo) {
                throw new IOException("boom");
            }
            return toReturn;
        }
    }

    @TempDir
    Path tmp;

    @Test
    void returnsSectionWhenHeaderPresent() throws Exception {
        String full = String.join("\n",
                "1 Introduction",
                "blah",
                "2 Related Work",
                "Prior studies (Smith, 2020).",
                "More (Doe, 2022).",
                "3 Methods",
                "…"
        );

        Path fakePdf = Files.createFile(tmp.resolve("paper.pdf"));
        PdfTextProvider provider = new FakePdfTextProvider(Optional.of(full), false);
        PdfRelatedWorkTextExtractor adapter = new PdfRelatedWorkTextExtractor(
                provider,
                new RelatedWorkSectionLocator()
        );

        Optional<String> section = adapter.extractRelatedWorkSection(fakePdf);
        assertTrue(section.isPresent());
        assertTrue(section.get().contains("(Smith, 2020)"));
        assertFalse(section.get().contains("3 Methods"));
    }

    @Test
    void emptyWhenNoHeader() throws Exception {
        String full = String.join("\n",
                "1 Introduction",
                "No related work header here.",
                "2 Methods"
        );

        Path fakePdf = Files.createFile(tmp.resolve("no-related.pdf"));
        PdfRelatedWorkTextExtractor adapter = new PdfRelatedWorkTextExtractor(
                new FakePdfTextProvider(Optional.of(full), false),
                new RelatedWorkSectionLocator()
        );

        assertTrue(adapter.extractRelatedWorkSection(fakePdf).isEmpty());
    }

    @Test
    void emptyWhenProviderReturnsEmpty() throws Exception {
        Path fakePdf = Files.createFile(tmp.resolve("empty.pdf"));
        PdfRelatedWorkTextExtractor adapter = new PdfRelatedWorkTextExtractor(
                new FakePdfTextProvider(Optional.empty(), false),
                new RelatedWorkSectionLocator()
        );

        assertTrue(adapter.extractRelatedWorkSection(fakePdf).isEmpty());
    }

    @Test
    void throwsOnIoError() throws Exception {
        Path fakePdf = Files.createFile(tmp.resolve("ioerr.pdf"));
        PdfRelatedWorkTextExtractor adapter = new PdfRelatedWorkTextExtractor(
                new FakePdfTextProvider(Optional.empty(), true),
                new RelatedWorkSectionLocator()
        );

        assertThrows(IOException.class, () -> adapter.extractRelatedWorkSection(fakePdf));
    }

    @Test
    void throwsOnNonFilePath() {
        PdfRelatedWorkTextExtractor adapter = new PdfRelatedWorkTextExtractor(
                new FakePdfTextProvider(Optional.empty(), false),
                new RelatedWorkSectionLocator()
        );

        assertThrows(IllegalArgumentException.class,
                () -> adapter.extractRelatedWorkSection(tmp.resolve("missing.pdf")));
    }
}
