package org.jabref.gui.remote;

import java.util.Arrays;

import javafx.application.Platform;

import org.jabref.cli.ArgumentProcessor;
import org.jabref.gui.JabRefGUI;
import org.jabref.logic.remote.server.RemoteMessageHandler;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.PreferencesService;

import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CLIMessageHandler implements RemoteMessageHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CLIMessageHandler.class);

    private final PreferencesService preferencesService;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final BibEntryTypesManager entryTypesManager;

    public CLIMessageHandler(PreferencesService preferencesService, FileUpdateMonitor fileUpdateMonitor, BibEntryTypesManager entryTypesManager) {
        this.preferencesService = preferencesService;
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
                    preferencesService,
                    fileUpdateMonitor,
                    entryTypesManager);
            argumentProcessor.processArguments();
            Platform.runLater(() -> JabRefGUI.getMainFrame().handleUiCommands(argumentProcessor.getUiCommands()));
        } catch (ParseException e) {
            LOGGER.error("Error when parsing CLI args", e);
        }
    }
}
