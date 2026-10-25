package org.jabref.logic.importer.fileformat.pdf;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.util.PdfUtils;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@NullMarked
class BibliographicPageDetectorTest {

    private static final String UKRAINIAN_IMPRINT_PAGE = """
            УДК 371.388:53
            ББК 22.3я72
            П30
            Петренко О. В.
            Фізика. 8 клас : збірник задач / О. В. Петренко. — 3-тє вид. — Харків : Веста, 2011. — 176 с.
            ISBN 978-966-08-0000-7
            Підписано до друку 09.06.2011. Формат 60×84/16. Наклад 5000 пр.
            © О. В. Петренко, 2008
            """;

    private static final String ENGLISH_COPYRIGHT_PAGE = """
            Copyright © 2019 by Example Press
            All rights reserved. No part of this book may be reproduced without permission.
            ISBN 978-3-16-148410-0
            Printed in the United States of America
            """;

    private static final String CONFERENCE_ARTICLE = """
            ABSTRACT
            We evaluate a software engineering approach.
            KEYWORDS
            software engineering
            ACM Reference Format:
            Example Author. 2024. Example article.
            In International Conference on Software Engineering.
            Copyright 2024. ISBN 979-8-4007-0217-4
            """;

    // [utest->req~import.pdf.conference-article-exclusion~1]
    @Test
    void rejectsConferenceArticleWithProceedingsIsbn() {
        assertEquals(Optional.empty(), BibliographicPageDetector.findBibliographicPage(List.of(CONFERENCE_ARTICLE)));
    }

    @Test
    void rejectsKeimFirstPage() throws IOException, URISyntaxException {
        var resource = BibliographicPageDetectorTest.class.getResource("/pdfs/PdfContentImporter/Keim2024.pdf");
        assertNotNull(resource);
        try (PDDocument document = Loader.loadPDF(Path.of(resource.toURI()).toFile())) {
            assertEquals(Optional.empty(), BibliographicPageDetector.findBibliographicPage(List.of(PdfUtils.getPageContents(document, 1))));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABSTRACT", "KEYWORDS", "ACM Reference Format:", "International Conference"})
    void doesNotExcludeCandidateWithIncompleteArticleEvidence(String missingEvidence) {
        String page = CONFERENCE_ARTICLE.replace(missingEvidence, "Other information");
        assertEquals(Optional.of(page), BibliographicPageDetector.findBibliographicPage(List.of(page)));
    }

    @Test
    void acceptsBookWithConferenceAndDoi() {
        String page = ENGLISH_COPYRIGHT_PAGE + """
                Proceedings of the International Conference on Software Engineering.
                https://doi.org/10.1145/3597503.3639130
                """;
        assertEquals(Optional.of(page), BibliographicPageDetector.findBibliographicPage(List.of(page)));
    }

    @Test
    void selectsImprintInsteadOfConferenceArticleWithMoreSignals() {
        String article = CONFERENCE_ARTICLE + ENGLISH_COPYRIGHT_PAGE + "Published by ACM";
        assertEquals(Optional.of(ENGLISH_COPYRIGHT_PAGE),
                BibliographicPageDetector.findBibliographicPage(List.of(article, ENGLISH_COPYRIGHT_PAGE)));
    }

    @Test
    void acceptsUkrainianImprintPage() {
        assertEquals(Optional.of(UKRAINIAN_IMPRINT_PAGE), BibliographicPageDetector.findBibliographicPage(List.of(UKRAINIAN_IMPRINT_PAGE)));
    }

    @Test
    void acceptsEnglishCopyrightPage() {
        assertEquals(Optional.of(ENGLISH_COPYRIGHT_PAGE), BibliographicPageDetector.findBibliographicPage(List.of(ENGLISH_COPYRIGHT_PAGE)));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Chapter 1\nMechanics describes how bodies move.",
            "ISBN 978-3-16-148410-0",
            "Preface\nCopyright law is covered in the last chapter."
    })
    void rejectsPageWithOnlyOneKindOfImprintInformation(String pageText) {
        assertEquals(Optional.empty(), BibliographicPageDetector.findBibliographicPage(List.of(pageText)));
    }

    @Test
    void rejectsReferenceList() {
        String references = """
                References
                Knuth, D. E. The Art of Computer Programming. Addison-Wesley Publishing, 1997. ISBN 978-0-201-89683-1
                """;
        assertEquals(Optional.empty(), BibliographicPageDetector.findBibliographicPage(List.of(references)));
    }

    @Test
    void rejectsPublisherCatalogue() {
        String catalogue = """
                Also published by Example Press
                Algebra. ISBN 978-1-940000-00-8
                Geometry. ISBN 978-1-940000-01-5
                Physics. ISBN 978-1-940000-02-2
                Chemistry. ISBN 978-1-940000-03-9
                Biology. ISBN 978-1-940000-04-6
                """;
        assertEquals(Optional.empty(), BibliographicPageDetector.findBibliographicPage(List.of(catalogue)));
    }

    @Test
    void prefersPageWithMoreImprintInformation() {
        String advertisement = "Also published by Example Press: ISBN 978-0-262-03384-8";
        assertEquals(Optional.of(ENGLISH_COPYRIGHT_PAGE), BibliographicPageDetector.findBibliographicPage(List.of(advertisement, ENGLISH_COPYRIGHT_PAGE)));
    }

    @Test
    void prefersEarlierPageOnEqualScore() {
        assertEquals(Optional.of(ENGLISH_COPYRIGHT_PAGE), BibliographicPageDetector.findBibliographicPage(List.of(ENGLISH_COPYRIGHT_PAGE, ENGLISH_COPYRIGHT_PAGE.replace("2019", "2020"))));
    }

    @Test
    void returnsEmptyWithoutPages() {
        assertEquals(Optional.empty(), BibliographicPageDetector.findBibliographicPage(List.of()));
    }
}
