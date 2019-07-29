package org.jabref.gui.entryeditor;

import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.PreferencesService;

import org.fxmisc.easybind.EasyBind;

public class LatexCitationsTab extends EntryEditorTab {

    private final LatexCitationsTabViewModel viewModel;
    private final StackPane searchPane;
    private final ProgressIndicator progressIndicator;

    public LatexCitationsTab(BibDatabaseContext databaseContext, PreferencesService preferencesService,
                             TaskExecutor taskExecutor) {
        this.viewModel = new LatexCitationsTabViewModel(databaseContext, preferencesService, taskExecutor);
        this.searchPane = new StackPane();
        this.progressIndicator = new ProgressIndicator();

        setText(Localization.lang("LaTeX Citations"));
        setTooltip(new Tooltip(Localization.lang("Search citations for this entry in LaTeX files")));
        setGraphic(IconTheme.JabRefIcons.LATEX_CITATIONS.getGraphicNode());
        setSearchPane();
    }

    private void setSearchPane() {
        progressIndicator.setMaxSize(100, 100);
        searchPane.getStyleClass().add("related-articles-tab");

        setContent(searchPane);

        EasyBind.subscribe(viewModel.statusProperty(), status -> {
            switch (status) {
                case IN_PROGRESS:
                    searchPane.getChildren().setAll(progressIndicator);
                    break;
                case CITATIONS_FOUND:
                    searchPane.getChildren().setAll(getCitationsPane());
                    break;
                case NO_RESULTS:
                    searchPane.getChildren().setAll(getNotFoundPane());
                    break;
                case ERROR:
                    searchPane.getChildren().setAll(getErrorPane());
                    break;
            }
        });
    }

    private ScrollPane getCitationsPane() {
        Text titleText = new Text(Localization.lang("Citations found"));
        titleText.getStyleClass().add("recommendation-heading");

        VBox citationsBox = new VBox(20, titleText);
        Path basePath = viewModel.directoryProperty().get();
        citationsBox.getChildren().addAll(viewModel.getCitationList().stream().map(
                citation -> citation.getDisplayGraphic(basePath, Optional.empty())).collect(Collectors.toList()));

        ScrollPane citationsPane = new ScrollPane();
        citationsPane.setContent(citationsBox);

        return citationsPane;
    }

    private ScrollPane getNotFoundPane() {
        Text notFoundTitleText = new Text(Localization.lang("No citations found"));
        notFoundTitleText.getStyleClass().add("recommendation-heading");

        Text notFoundText = new Text(Localization.lang("No LaTeX files containing this entry were found."));
        notFoundText.setStyle("-fx-font-size: 110%");

        Text notFoundAdviceText = new Text(Localization.lang(
                "You can set the LaTeX file directory in the 'Library properties' dialog."));
        notFoundAdviceText.setStyle("-fx-font-weight: bold;");

        VBox notFoundBox = new VBox(20, notFoundTitleText, notFoundText, notFoundAdviceText);
        ScrollPane notFoundPane = new ScrollPane();
        notFoundPane.setContent(notFoundBox);

        return notFoundPane;
    }

    private ScrollPane getErrorPane() {
        Text errorTitleText = new Text(Localization.lang("Error"));
        errorTitleText.setStyle("-fx-fill: -fx-accent;");
        errorTitleText.getStyleClass().add("recommendation-heading");

        Text errorMessageText = new Text(viewModel.searchErrorProperty().get());
        errorMessageText.setStyle("-fx-font-family: monospace;-fx-font-size: 120%;");

        VBox errorBox = new VBox(20, errorTitleText, errorMessageText);
        ScrollPane errorPane = new ScrollPane();
        errorPane.setContent(errorBox);

        return errorPane;
    }

    @Override
    protected void bindToEntry(BibEntry entry) {
        viewModel.init(entry);
    }

    @Override
    public boolean shouldShow(BibEntry entry) {
        return viewModel.shouldShow();
    }
}
