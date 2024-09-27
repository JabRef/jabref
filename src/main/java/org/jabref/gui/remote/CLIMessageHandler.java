package org.jabref.gui.remote;

import java.util.Arrays;

import javafx.application.Platform;

import org.jabref.cli.ArgumentProcessor;
import org.jabref.gui.frame.UiMessageHandler;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.remote.server.RemoteMessageHandler;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.util.FileUpdateMonitor;

import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CLIMessageHandler implements RemoteMessageHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CLIMessageHandler.class);

    private final GuiPreferences preferences;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final BibEntryTypesManager entryTypesManager;
    private final UiMessageHandler uiMessageHandler;

    public CLIMessageHandler(UiMessageHandler uiMessageHandler,
                             GuiPreferences preferences,
                             FileUpdateMonitor fileUpdateMonitor,
                             BibEntryTypesManager entryTypesManager) {
        this.uiMessageHandler = uiMessageHandler;
        this.preferences = preferences;
        this.fileUpdateMonitor = fileUpdateMonitor;
        this.entryTypesManager = entryTypesManager;
    }

    @Override
    public void handleCommandLineArguments(String[] message) {
        try {
            LOGGER.info("Processing message {}", Arrays.stream(message).toList());
            ArgumentProcessor argumentProcessor = new ArgumentProcessor(
                    message,
                    ArgumentProcessor.Mode.REMOTE_START,
                    preferences,
                    preferences,
                    fileUpdateMonitor,
                    entryTypesManager);
            argumentProcessor.processArguments();
            Platform.runLater(() -> uiMessageHandler.handleUiCommands(argumentProcessor.getUiCommands()));
        } catch (ParseException e) {
            LOGGER.error("Error when parsing CLI args", e);
        }
    }
}
