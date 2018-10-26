package org.jabref.gui.entryeditor;

import java.io.IOException;
import java.util.List;

import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.Text;

import org.jabref.Globals;
import org.jabref.gui.DialogService;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.logic.importer.fetcher.MrDLibFetcher;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.preferences.JabRefPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GUI for tab displaying article recommendations based on the currently selected BibEntry
 */
public class RelatedArticlesTab extends EntryEditorTab {

    private static final Logger LOGGER = LoggerFactory.getLogger(RelatedArticlesTab.class);
    private final EntryEditorPreferences preferences;
    private final DialogService dialogService;

    public RelatedArticlesTab(EntryEditorPreferences preferences, DialogService dialogService) {
        setText(Localization.lang("Related articles"));
        setTooltip(new Tooltip(Localization.lang("Related articles")));
        this.preferences = preferences;
        this.dialogService = dialogService;
    }

    /**
     * Gets a StackPane of related article information to be displayed in the Related Articles tab
     * @param entry The currently selected BibEntry on the JabRef UI.
     * @return A StackPane with related article information to be displayed in the Related Articles tab.
     */
    private StackPane getRelatedArticlesPane(BibEntry entry) {
        StackPane root = new StackPane();
        root.getStyleClass().add("related-articles-tab");
        ProgressIndicator progress = new ProgressIndicator();
        progress.setMaxSize(100, 100);

        MrDLibFetcher fetcher = new MrDLibFetcher(Globals.prefs.get(JabRefPreferences.LANGUAGE),
                                                  Globals.BUILD_INFO.getVersion());
        BackgroundTask
                      .wrap(() -> fetcher.performSearch(entry))
                      .onRunning(() -> progress.setVisible(true))
                      .onSuccess(relatedArticles -> {
                          progress.setVisible(false);
                          root.getChildren().add(getRelatedArticleInfo(relatedArticles));
                      })
                      .executeWith(Globals.TASK_EXECUTOR);

        root.getChildren().add(progress);

        return root;
    }

    /**
     * Creates a VBox of the related article information to be used in the StackPane displayed in the Related Articles tab
     * @param list List of BibEntries of related articles
     * @return VBox of related article descriptions to be displayed in the Related Articles tab
     */
    private VBox getRelatedArticleInfo(List<BibEntry> list) {
        VBox vBox = new VBox();
        vBox.setSpacing(20.0);

        for (BibEntry entry : list) {
            HBox hBox = new HBox();
            hBox.setSpacing(5.0);

            String title = entry.getTitle().orElse("");
            String journal = entry.getField(FieldName.JOURNAL).orElse("");
            String authors = entry.getField(FieldName.AUTHOR).orElse("");
            String year = entry.getField(FieldName.YEAR).orElse("");

            Hyperlink titleLink = new Hyperlink(title);
            Text journalText = new Text(journal);
            journalText.setFont(Font.font(Font.getDefault().getFamily(), FontPosture.ITALIC, Font.getDefault().getSize()));
            Text authorsText = new Text(authors);
            Text yearText = new Text("(" + year + ")");
            titleLink.setOnAction(event -> {
                if (entry.getField(FieldName.URL).isPresent()) {
                    try {
                        JabRefDesktop.openBrowser(entry.getField(FieldName.URL).get());
                    } catch (IOException e) {
                        LOGGER.error("Error opening the browser to: " + entry.getField(FieldName.URL).get(), e);
                        dialogService.showErrorDialogAndWait(e);
                    }
                }
            });

            hBox.getChildren().addAll(titleLink, journalText, authorsText, yearText);
            vBox.getChildren().add(hBox);
        }
        return vBox;
    }

    /**
     * Returns a consent dialog used to ask permission to send data to Mr. DLib.
     * @param entry Currently selected BibEntry. (required to allow reloading of pane if accepted)
     * @return StackPane returned to be placed into Related Articles tab.
     */
    private ScrollPane getPrivacyDialog(BibEntry entry) {
        ScrollPane root = new ScrollPane();
        root.getStyleClass().add("related-articles-tab");
        VBox vbox = new VBox();
        vbox.getStyleClass().add("gdpr-dialog");
        vbox.setSpacing(20.0);

        Button button = new Button(Localization.lang("I Agree"));
        button.setDefaultButton(true);
        Text line1 = new Text(Localization.lang("Mr. DLib is an external service which provides article recommendations based on the currently selected entry. Data about the selected entry must be sent to Mr. DLib in order to provide these recommendations. Do you agree that this data may be sent?"));

        line1.setWrappingWidth(1300.0);
        Text line2 = new Text(Localization.lang("This setting may be changed in preferences at any time."));
        Hyperlink mdlLink = new Hyperlink(Localization.lang("Further information about Mr DLib. for JabRef users."));
        mdlLink.setOnAction(event -> {
            try {
                JabRefDesktop.openBrowser("http://mr-dlib.org/information-for-users/information-about-mr-dlib-for-jabref-users/");
            } catch (IOException e) {
                LOGGER.error("Error opening the browser to Mr. DLib information page.", e);
                dialogService.showErrorDialogAndWait(e);
            }
        });

        button.setOnAction(event -> {
            JabRefPreferences prefs = JabRefPreferences.getInstance();
            prefs.putBoolean(JabRefPreferences.ACCEPT_RECOMMENDATIONS, true);
            dialogService.showWarningDialogAndWait(Localization.lang("Restart"), Localization.lang("Please restart JabRef for preferences to take effect."));
            setContent(getRelatedArticlesPane(entry));
        });

        vbox.getChildren().addAll(line1, mdlLink, line2, button);
        root.setContent(vbox);

        return root;
    }

    @Override
    public boolean shouldShow(BibEntry entry) {
        return preferences.shouldShowRecommendationsTab();
    }

    @Override
    protected void bindToEntry(BibEntry entry) {
        // Ask for consent to send data to Mr. DLib on first time to tab
        if (preferences.isMrdlibAccepted()) {
            setContent(getRelatedArticlesPane(entry));
        } else {
            setContent(getPrivacyDialog(entry));
        }
    }
}
