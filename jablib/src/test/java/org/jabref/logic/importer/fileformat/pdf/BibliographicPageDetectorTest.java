package org.jabref.logic.importer.fileformat.pdf;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
