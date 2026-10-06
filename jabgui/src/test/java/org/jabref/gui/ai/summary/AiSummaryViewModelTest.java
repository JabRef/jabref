package org.jabref.gui.ai.summary;

import java.util.List;

import org.jabref.model.entry.LinkedFile;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiSummaryViewModelTest {
    private static final LinkedFile LINKED = new LinkedFile("", "https://link.springer.com/content/pdf/10.1007%2F978-3-031-24066-9_5.pdf", "PDF");
    private static final LinkedFile LOCAL = new LinkedFile("", "test.pdf", "PDF");

    @Test
    void emptyList() {
        assertFalse(AiSummaryViewModel.hasOnlineLink(List.of()));
    }

    @Test
    void onlineLink() {
        assertTrue(AiSummaryViewModel.hasOnlineLink(List.of(LINKED)));
    }

    @Test
    void onlineAndLocal() {
        assertFalse(AiSummaryViewModel.hasOnlineLink(List.of(LINKED, LOCAL)));
    }

    @Test
    void LocalLink() {
        assertFalse(AiSummaryViewModel.hasOnlineLink(List.of(LOCAL)));
    }
}
