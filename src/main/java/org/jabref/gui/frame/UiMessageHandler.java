package org.jabref.gui.frame;

import java.util.List;

import org.jabref.logic.UiCommand;

public interface UiMessageHandler {

    void handleUiCommands(List<UiCommand> uiCommands);
}
