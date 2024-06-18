package org.jabref.gui.ai.components;

import one.jpro.platform.mdfx.MarkdownView;
import org.jabref.gui.JabRefGUI;
import org.jabref.preferences.WorkspacePreferences;

import java.net.URL;
import java.util.List;

public class JabRefMarkdownView extends MarkdownView {
    public void applyTheme(WorkspacePreferences workspacePreferences) {
        workspacePreferences.getTheme().getAdditionalStylesheetURL().ifPresent(url -> {
            getStylesheets().add(url);
        });
    }

    @Override
    protected List<String> getDefaultStylesheets() {
        URL base = JabRefGUI.class.getResource("Base.css");
        return base == null ? List.of() : List.of(base.toExternalForm());
    }
}
