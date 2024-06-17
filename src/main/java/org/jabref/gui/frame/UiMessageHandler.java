package org.jabref.gui.frame;

import java.util.List;

import org.jabref.logic.UiCommand;

/**
 * Specifies an interface that can process either cli or remote commands to the ui
 *
 * See {@link org.jabref.logic.remote.server.RemoteMessageHandler}
 */
public interface UiMessageHandler {

    void handleUiCommands(List<UiCommand> uiCommands);
}
