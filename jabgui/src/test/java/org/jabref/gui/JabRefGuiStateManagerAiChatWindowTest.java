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
}
