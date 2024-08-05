package org.jabref.gui.importer.actions;

import org.jabref.gui.DialogService;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.importer.ParserResult;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.injection.Injector;

public class ListenForCitationKeyChangeForChatHistoryAction implements GUIPostOpenAction {
    private final AiService aiService = Injector.instantiateModelOrService(AiService.class);

    @Override
    public boolean isActionNecessary(ParserResult pr, PreferencesService preferencesService) {
        return true;
    }

    @Override
    public void performAction(ParserResult pr, DialogService dialogService, PreferencesService preferencesService) {
        // pr.getDatabase().getEntries().forEach(entry -> entry.registerListener(aiService.getChatHistoryManager()));
        pr.getDatabase().registerListener(aiService.getChatHistoryManager());
    }
}
