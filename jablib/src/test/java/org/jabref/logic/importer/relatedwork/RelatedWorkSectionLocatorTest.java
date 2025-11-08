package org.jabref.logic.importer.relatedwork;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RelatedWorkSectionLocatorTest {

    private final RelatedWorkSectionLocator locator = new RelatedWorkSectionLocator();

    @Test
    void findsSimpleRelatedWorkSection() {
        String text = String.join("\n",
                "1 Introduction",
                "This paper studies X.",
                "",
                "2 Related Work",
                "Prior studies (Smith, 2020) show ...",
                "More details (Doe and Roe, 2022).",
                "",
                "3 Methods",
                "We do Y."
        );

        Optional<String> section = locator.locate(text);
        assertTrue(section.isPresent());
        String s = section.get();
        assertTrue(s.contains("Prior studies (Smith, 2020)"));
        assertFalse(s.contains("3 Methods"));
    }

    @Test
    void acceptsMixedCaseAndPunctuation() {
        String text = String.join("\n",
                "INTRODUCTION",
                "Some intro.",
                "2.1 Literature Review:",
                "We review (Bianchi, 2021) and (Vesce et al., 2016).",
                "There is also work by (Lee, 2019).",
                "3 RESULTS"
        );
        Optional<String> section = locator.locate(text);
        assertTrue(section.isPresent());
        String s = section.get();
        assertTrue(s.contains("(Bianchi, 2021)"));
        assertTrue(s.contains("(Vesce et al., 2016)"));
        assertFalse(s.contains("3 RESULTS"));
    }

    @Test
    void matchesCombinedBackgroundAndRelatedWork() {
        String text = String.join("\n",
                "1 Background and Related Work",
                "Foundational approaches (Nash, 2022).",
                "Classic systems (Foo, 2018).",
                "2 Approach",
                "Our approach ..."
        );
        Optional<String> section = locator.locate(text);
        assertTrue(section.isPresent());
        assertTrue(section.get().startsWith("Foundational approaches"));
    }

    @Test
    void returnsEmptyWhenNotPresent() {
        String text = String.join("\n",
                "1 Introduction",
                "No related work header exists here.",
                "2 Methods",
                "3 Results"
        );
        assertTrue(locator.locate(text).isEmpty());
    }

    @Test
    void doesNotConfuseFigureCaptionsForHeaders() {
        String text = String.join("\n",
                "2 Related Work",
                "Prior studies (Smith, 2020).",
                "Figure 2: Related work visualization",
                "This line should still belong to the section.",
                "3 Methods"
        );
        String s = locator.locate(text).orElse("");
        assertTrue(s.contains("This line should still belong"));
        assertFalse(s.contains("3 Methods"));
    }
}
