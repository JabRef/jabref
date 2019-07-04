package org.jabref.gui.entryeditor;

import java.io.IOException;
import java.util.List;

import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
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
    private final EntryEditor entryEditor;
    private final DialogService dialogService;

    public RelatedArticlesTab(EntryEditor entryEditor, EntryEditorPreferences preferences, DialogService dialogService) {
        setText(Localization.lang("Related articles"));
        setTooltip(new Tooltip(Localization.lang("Related articles")));
        this.entryEditor = entryEditor;
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
                          root.getChildren().add(getRelatedArticleInfo(relatedArticles, fetcher));
                      })
                      .onFailure(exception -> {
                          LOGGER.error("Error while fetching from Mr. DLib", exception);
                          progress.setVisible(false);
                          root.getChildren().add(getErrorInfo());
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
    private ScrollPane getRelatedArticleInfo(List<BibEntry> list, MrDLibFetcher fetcher) {
        ScrollPane scrollPane = new ScrollPane();

        VBox vBox = new VBox();
        vBox.setSpacing(20.0);

        String heading = fetcher.getHeading();
        Text headingText = new Text(heading);
        headingText.getStyleClass().add("recommendation-heading");
        String description = fetcher.getDescription();
        Text descriptionText = new Text(description);
        descriptionText.getStyleClass().add("recommendation-description");
        vBox.getChildren().add(headingText);
        vBox.getChildren().add(descriptionText);

        for (BibEntry entry : list) {
            HBox hBox = new HBox();
            hBox.setSpacing(5.0);
            hBox.getStyleClass().add("recommendation-item");

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
        scrollPane.setContent(vBox);
        return scrollPane;
    }

    /**
     * Gets a ScrollPane to display error info when recommendations fail.
     * @return ScrollPane to display in place of recommendations
     */
    private ScrollPane getErrorInfo() {
        ScrollPane scrollPane = new ScrollPane();

        VBox vBox = new VBox();
        vBox.setSpacing(20.0);

        Text descriptionText = new Text(Localization.lang("No recommendations received from Mr. DLib for this entry."));
        descriptionText.getStyleClass().add("recommendation-description");
        vBox.getChildren().add(descriptionText);
        scrollPane.setContent(vBox);

        return scrollPane;
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

        Text line1 = new Text(Localization.lang("JabRef requests recommendations from Mr. DLib, which is an external service. To enable Mr. DLib to calculate recommendations, some of your data must be shared with Mr. DLib. Generally, the more data is shared the better recommendations can be calculated. However, we understand that some of your data in JabRef is sensitive, and you may not want to share it. Therefore, Mr. DLib offers a choice of which data you would like to share."));
        line1.setWrappingWidth(1300.0);
        Text line2 = new Text(Localization.lang("Whatever option you choose, Mr. DLib may share its data with research partners to further improve recommendation quality as part of a 'living lab'. Mr. DLib may also release public datasets that may contain anonymized information about you and the recommendations (sensitive information such as metadata of your articles will be anonymised through e.g. hashing). Research partners are obliged to adhere to the same strict data protection policy as Mr. DLib."));
        line2.setWrappingWidth(1300.0);
        Text line3 = new Text(Localization.lang("This setting may be changed in preferences at any time."));
        Hyperlink mdlLink = new Hyperlink(Localization.lang("Further information about Mr DLib. for JabRef users."));
        mdlLink.setOnAction(event -> {
            try {
                JabRefDesktop.openBrowser("http://mr-dlib.org/information-for-users/information-about-mr-dlib-for-jabref-users/");
            } catch (IOException e) {
                LOGGER.error("Error opening the browser to Mr. DLib information page.", e);
                dialogService.showErrorDialogAndWait(e);
            }
        });
        VBox vb = new VBox();
        CheckBox cbTitle = new CheckBox(Localization.lang("Entry Title (Required to deliver recommendations.)"));
        cbTitle.setSelected(true);
        cbTitle.setDisable(true);
        CheckBox cbVersion = new CheckBox(Localization.lang("JabRef Version (Required to ensure backwards compatibility with Mr. DLib's Web Service)"));
        cbVersion.setSelected(true);
        cbVersion.setDisable(true);
        CheckBox cbLanguage = new CheckBox(Localization.lang("JabRef Language (Provides for better recommendations by giving an indication of user's preferred language.)"));
        CheckBox cbOS = new CheckBox(Localization.lang("Operating System (Provides for better recommendations by giving an indication of user's system set-up.)"));
        CheckBox cbTimezone = new CheckBox(Localization.lang("Timezone (Provides for better recommendations by indicating the time of day the request is being made.)"));
        vb.getChildren().addAll(cbTitle, cbVersion, cbLanguage, cbOS, cbTimezone);
        vb.setSpacing(10);

        button.setOnAction(event -> {
            JabRefPreferences prefs = JabRefPreferences.getInstance();
            prefs.putBoolean(JabRefPreferences.ACCEPT_RECOMMENDATIONS, true);
            prefs.putBoolean(JabRefPreferences.SEND_LANGUAGE_DATA, cbLanguage.isSelected());
            prefs.putBoolean(JabRefPreferences.SEND_OS_DATA, cbOS.isSelected());
            prefs.putBoolean(JabRefPreferences.SEND_TIMEZONE_DATA, cbTimezone.isSelected());
            dialogService.showWarningDialogAndWait(Localization.lang("Restart"), Localization.lang("Please restart JabRef for preferences to take effect."));
            setContent(getRelatedArticlesPane(entry));
        });

        vbox.getChildren().addAll(line1, line2, mdlLink, line3, vb, button);
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
