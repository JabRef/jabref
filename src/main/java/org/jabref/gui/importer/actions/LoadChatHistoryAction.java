package org.jabref.gui.importer.actions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.logic.ai.AiChatsFile;
import org.jabref.logic.ai.ChatMessage;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoadChatHistoryAction implements GUIPostOpenAction {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoadChatHistoryAction.class);

    public static final String AI_CHAT_HISTORY_EXTENSION = ".aichats";

    @Override
    public boolean isActionNecessary(ParserResult pr) {
        return true;
    }

    @Override
    public void performAction(ParserResult pr, DialogService dialogService) {
        pr.getDatabaseContext().getDatabasePath().ifPresent(databasePath -> readAiChatsFile(pr.getDatabase(), databasePath));
    }

    private void readAiChatsFile(BibDatabase bibDatabase, Path databasePath) {
        Path chatPath = FileUtil.addExtension(databasePath, AI_CHAT_HISTORY_EXTENSION);
        File chatFile = chatPath.toFile();
        try {
            InputStream inputStream = new FileInputStream(chatFile);
            ObjectMapper objectMapper = new ObjectMapper();
            AiChatsFile aiChatFile = objectMapper.readValue(inputStream, AiChatsFile.class);
            loadAiChatHistory(bibDatabase, aiChatFile);
        } catch (FileNotFoundException e) {
            LOGGER.info("There is no " + AI_CHAT_HISTORY_EXTENSION + " file for the opened library.");
        } catch (IOException e) {
            LOGGER.error("An error occurred while reading " + chatPath, e);
        }
    }

    private void loadAiChatHistory(BibDatabase bibDatabase, AiChatsFile aiChatFile) {
        aiChatFile.getChatHistoryMap().forEach((citationKey, chatHistory) -> {
            List<BibEntry> bibEntries = bibDatabase.getEntriesByCitationKey(citationKey);

            if (bibEntries.isEmpty()) {
                LOGGER.warn("Found a chat history for an unknown bib entry with citation key \"" + citationKey + "\"");
            } else if (bibEntries.size() != 1) {
                LOGGER.warn("Found a chat history for an bib entry with citation key \"" + citationKey + "\" but there are several bib entries in the database with the same key");
            } else {
                BibEntry bibEntry = bibEntries.getFirst();
                bibEntry.getAiChatMessages().addAll(chatHistory);
            }
        });
    }
}

