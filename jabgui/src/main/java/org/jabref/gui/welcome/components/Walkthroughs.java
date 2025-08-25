package org.jabref.gui.welcome.components;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.walkthrough.WalkthroughAction;
import org.jabref.logic.l10n.Localization;

public class Walkthroughs extends VBox {
    private final Stage stage;
    private final LibraryTabContainer tabContainer;
    private final StateManager stateManager;
    private final GuiPreferences preferences;

    private final Label header;
    private boolean isScrollEnabled = true;

    public Walkthroughs(Stage stage, LibraryTabContainer tabContainer, StateManager stateManager, GuiPreferences preferences) {
        this.stage = stage;
        this.tabContainer = tabContainer;
        this.stateManager = stateManager;
        this.preferences = preferences;

        getStyleClass().add("welcome-section");

        header = new Label(Localization.lang("Walkthroughs"));
        header.getStyleClass().add("welcome-header-label");
        enableScroll();
    }

    public void enableScroll() {
        if (isScrollEnabled) {
            return;
        }
        isScrollEnabled = true;
        getChildren().clear();
        getChildren().addAll(header, createScrollPane(createWalkthroughContent()));
    }

    public void disableScroll() {
        if (!isScrollEnabled) {
            return;
        }
        isScrollEnabled = false;
        getChildren().clear();
        getChildren().addAll(header, createWalkthroughContent());
    }

    private VBox createWalkthroughContent() {
        VBox content = new VBox();
        content.getStyleClass().add("walkthroughs-container");

        Button mainFileDirWalkthroughButton = createWalkthroughButton(
                Localization.lang("Set main file directory"),
                IconTheme.JabRefIcons.FOLDER,
                WalkthroughAction.MAIN_FILE_DIRECTORY_WALKTHROUGH_NAME);
        Button entryTableWalkthroughButton = createWalkthroughButton(
                Localization.lang("Customize entry table"),
                IconTheme.JabRefIcons.TOGGLE_GROUPS,
                WalkthroughAction.CUSTOMIZE_ENTRY_TABLE_WALKTHROUGH_NAME);
        Button linkPdfWalkthroughButton = createWalkthroughButton(
                Localization.lang("Link PDF to entries"),
                IconTheme.JabRefIcons.PDF_FILE,
                WalkthroughAction.PDF_LINK_WALKTHROUGH_NAME);
        Button groupButton = createWalkthroughButton(
                Localization.lang("Add group"),
                IconTheme.JabRefIcons.NEW_GROUP,
                WalkthroughAction.GROUP_WALKTHROUGH_NAME);
        Button searchButton = createWalkthroughButton(
                Localization.lang("Search your library"),
                IconTheme.JabRefIcons.SEARCH,
                WalkthroughAction.SEARCH_WALKTHROUGH_NAME);

        content.getChildren().addAll(
                mainFileDirWalkthroughButton,
                entryTableWalkthroughButton,
                linkPdfWalkthroughButton,
                groupButton,
                searchButton);
        return content;
    }

    private ScrollPane createScrollPane(VBox content) {
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("walkthroughs-scroll-pane");
        scrollPane.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        return scrollPane;
    }

    private Button createWalkthroughButton(String text, IconTheme.JabRefIcons icon, String walkthroughId) {
        Button button = new Button(text);
        button.setGraphic(icon.getGraphicNode());
        button.getStyleClass().add("quick-settings-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(_ -> new WalkthroughAction(stage, tabContainer, stateManager, preferences, walkthroughId).execute());
        return button;
    }
}
