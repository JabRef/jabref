package org.jabref.gui.push;

import java.nio.file.Path;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.GuiPreferences;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class PushToWinEdtTest {

    private DialogService dialogService;
    private GuiPreferences preferences;

    @BeforeEach
    void setup() {
        dialogService = mock(DialogService.class, Answers.RETURNS_DEEP_STUBS);
        preferences = mock(GuiPreferences.class);
    }

    @Test
    void jumpToLineCommandlineArguments() {
        assertNotNull(new PushToWinEdt(dialogService, preferences).jumpToLineCommandlineArguments(Path.of("test.tex"), 1, 5));
    }
}
