package org.jabref.languageserver;

import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.entry.BibEntryTypesManager;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

@NullMarked
class LspLauncherTest {

    @Test
    void clientHandlerOfGuiServerIsNotStandalone() {
        LspLauncher launcher = new LspLauncher(_ -> {
        }, mock(CliPreferences.class, RETURNS_DEEP_STUBS), mock(JournalAbbreviationRepository.class), new BibEntryTypesManager(), 0);

        assertFalse(launcher.createClientHandler().isStandalone());
    }
}
