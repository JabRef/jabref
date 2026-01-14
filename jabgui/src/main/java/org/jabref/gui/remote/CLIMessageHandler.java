package org.jabref.gui.remote;

import java.util.Arrays;
import java.util.List;

import javafx.application.Platform;

import org.jabref.cli.ArgumentProcessor;
import org.jabref.logic.UiMessageHandler;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.UiCommand;
import org.jabref.logic.remote.server.RemoteMessageHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CLIMessageHandler implements RemoteMessageHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CLIMessageHandler.class);

    private final GuiPreferences preferences;
    private final UiMessageHandler uiMessageHandler;

    public CLIMessageHandler(UiMessageHandler uiMessageHandler,
                             GuiPreferences preferences) {
        this.uiMessageHandler = uiMessageHandler;
        this.preferences = preferences;
    }

    @Override
    public void handleCommandLineArguments(String[] message) {
        LOGGER.info("Processing message {}", Arrays.stream(message).toList());
        ArgumentProcessor argumentProcessor = new ArgumentProcessor(
                message,
                ArgumentProcessor.Mode.REMOTE_START,
                preferences);
        List<UiCommand> uiCommands = argumentProcessor.processArguments();
        Platform.runLater(() -> uiMessageHandler.handleUiCommands(uiCommands));
    }

    @Override
    public void handleFocus() {
        Platform.runLater(() -> uiMessageHandler.handleUiCommands(List.of(new UiCommand.Focus())));
    }
}
