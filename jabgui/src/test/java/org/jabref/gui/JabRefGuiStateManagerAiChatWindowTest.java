package org.jabref.gui;

import java.util.Optional;

import org.jabref.gui.ai.chat.AiGroupChatWindow;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class JabRefGuiStateManagerAiChatWindowTest {

    private StateManager stateManager;
    private BibDatabaseContext context;
    private AiGroupChatWindow window;

    @BeforeEach
    void setUp() {
        stateManager = new JabRefGuiStateManager();
        context = new BibDatabaseContext();
        window = mock(AiGroupChatWindow.class);
    }

    @Test
    void findsWindowAfterDatabaseContentChanges() {
        String groupName = "My group";
        stateManager.setAiChatWindowForGroup(context, groupName, window);

        context.getDatabase().insertEntry(new BibEntry().withField(StandardField.TITLE, "Changed content"));

        assertEquals(Optional.of(window), stateManager.getAiChatWindowForGroup(context, groupName));
    }

    @Test
    void removesWindowAfterDatabaseContentChanges() {
        String groupName = "My group";
        stateManager.setAiChatWindowForGroup(context, groupName, window);

        context.getDatabase().insertEntry(new BibEntry().withField(StandardField.TITLE, "Changed content"));
        stateManager.removeAiChatWindowForGroup(context, groupName);

        assertEquals(Optional.empty(), stateManager.getAiChatWindowForGroup(context, groupName));
    }

    @Test
    void closingLibraryClosesItsChatWindowsAndReleasesRegistrations() {
        AiGroupChatWindow secondWindow = mock(AiGroupChatWindow.class);
        stateManager.setAiChatWindowForGroup(context, "First", window);
        stateManager.setAiChatWindowForGroup(context, "Second", secondWindow);
        context.getDatabase().insertEntry(new BibEntry().withField(StandardField.TITLE, "Changed content"));

        stateManager.closeAiChatWindows(context);

        verify(window).close();
        verify(secondWindow).close();
        assertEquals(Optional.empty(), stateManager.getAiChatWindowForGroup(context, "First"));
        assertEquals(Optional.empty(), stateManager.getAiChatWindowForGroup(context, "Second"));
    }

    @Test
    void closingLibraryKeepsAnotherLibraryChatWindowOpen() {
        BibDatabaseContext otherContext = new BibDatabaseContext();
        AiGroupChatWindow otherWindow = mock(AiGroupChatWindow.class);
        stateManager.setAiChatWindowForGroup(context, "Shared name", window);
        stateManager.setAiChatWindowForGroup(otherContext, "Shared name", otherWindow);

        stateManager.closeAiChatWindows(context);

        verifyNoInteractions(otherWindow);
        assertEquals(Optional.of(otherWindow), stateManager.getAiChatWindowForGroup(otherContext, "Shared name"));
    }
}
