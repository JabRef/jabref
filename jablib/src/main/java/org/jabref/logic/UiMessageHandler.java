package org.jabref.logic;

import java.util.List;

/// Specifies an interface that can process either cli or remote commands to the ui
///
/// See {@link org.jabref.logic.remote.server.RemoteMessageHandler}
public interface UiMessageHandler {
    /// Null object bound by the standalone HTTP server, which runs without a GUI.
    ///
    /// The REST resources declare a mandatory `@Inject UiMessageHandler`, so *something* has to be
    /// bound in the HK2 service locator — otherwise HK2 fails to construct the resource (HTTP 500)
    /// before the resource's own "GUI required" check can run. Standalone mode therefore binds this
    /// null object instead of a real handler; resources detect it via [#isGuiConnected()] and reply
    /// with a clear 400 "Only possible in GUI mode." instead of a dependency-injection failure.
    ///
    /// Calling [#handleUiCommands] on it means the GUI check was skipped (a programming error), so it
    /// fails fast rather than silently swallowing the command.
    UiMessageHandler NONE = uiCommands -> {
        throw new UnsupportedOperationException("No GUI is connected to the JabRef HTTP server");
    };

    void handleUiCommands(List<UiCommand> uiCommands);

    /// Whether a GUI is connected. `false` only for the [#NONE] null object used by the standalone
    /// server; any real handler returns `true`.
    default boolean isGuiConnected() {
        return this != NONE;
    }
}
