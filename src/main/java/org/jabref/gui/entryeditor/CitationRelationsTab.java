package org.jabref.gui.entryeditor;

import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tooltip;
import org.jabref.gui.DialogService;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GUI for tab displaying an articles citation relations based on the currently selected BibEntry
 */
public class CitationRelationsTab extends EntryEditorTab {

    private static final Logger LOGGER = LoggerFactory.getLogger(org.jabref.gui.entryeditor.CitationRelationsTab.class);
    private final EntryEditorPreferences preferences;
    private final EntryEditor entryEditor;
    private final DialogService dialogService;

    public CitationRelationsTab(EntryEditor entryEditor, EntryEditorPreferences preferences, DialogService dialogService) {
        setText(Localization.lang("Citation relations"));
        setTooltip(new Tooltip(Localization.lang("Show articles related by citation")));
        this.entryEditor = entryEditor;
        this.preferences = preferences;
        this.dialogService = dialogService;
    }

    private SplitPane getPane(BibEntry entry) {
        Label citedLabel = new Label("Cited");
        Label citedByLabel = new Label("Cited By");

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setContent(citedLabel);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        ScrollPane scrollPane2 = new ScrollPane();
        scrollPane2.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane2.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane2.setContent(citedByLabel);
        scrollPane2.setFitToWidth(true);
        scrollPane2.setFitToHeight(true);

        SplitPane container = new SplitPane(scrollPane, scrollPane2);
        setContent(container);

        return container;
    }

    @Override
    public boolean shouldShow(BibEntry entry) {
        return preferences.shouldShowCitationRelationsTab();
    }

    @Override
    protected void bindToEntry(BibEntry entry) {
        // Ask for consent to send data
        setContent(getPane(entry));
        /*if (preferences.isMrdlibAccepted()) {
            setContent(getRelatedArticlesPane(entry));
        } else {
            setContent(getPrivacyDialog(entry));
        }*/
    }
}
