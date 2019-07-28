package org.jabref.gui.entryeditor;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.strings.LatexToUnicodeAdapter;
import org.jabref.model.texparser.Citation;
import org.jabref.preferences.PreferencesService;

import org.fxmisc.easybind.EasyBind;

public class LatexCitationsTab extends EntryEditorTab {

    private final LatexCitationsTabViewModel viewModel;
    private final StackPane searchPane;
    private final ProgressIndicator progressIndicator;
    private final ObservableList<VBox> graphicCitationList;

    public LatexCitationsTab(BibDatabaseContext databaseContext, PreferencesService preferencesService,
                             TaskExecutor taskExecutor) {
        this.viewModel = new LatexCitationsTabViewModel(databaseContext, preferencesService, taskExecutor);
        this.searchPane = new StackPane();
        this.progressIndicator = new ProgressIndicator();
        this.graphicCitationList = FXCollections.observableArrayList();

        setText(Localization.lang("LaTeX Citations"));
        setTooltip(new Tooltip(Localization.lang("Search citations for this entry in LaTeX files")));
        setGraphic(IconTheme.JabRefIcons.LATEX_CITATIONS.getGraphicNode());
        setSearchPane();
    }

    private void setSearchPane() {
        progressIndicator.setMaxSize(100.0, 100.0);
        searchPane.getStyleClass().add("latex-citations-tab");

        setContent(searchPane);
    }

    @Override
    protected void bindToEntry(BibEntry entry) {
        EasyBind.subscribe(viewModel.statusProperty(), status -> {
            switch (status) {
                case IN_PROGRESS:
                    searchPane.getChildren().setAll(progressIndicator);
                    break;
                case CITATIONS_FOUND:
                    graphicCitationList.setAll(EasyBind.map(viewModel.getCitationList(), this::citationToGraphic));
                    searchPane.getChildren().setAll(getCitationsPane());
                    break;
                case NO_RESULTS:
                    searchPane.getChildren().setAll(getNotFoundPane());
                    break;
                case ERROR:
                    searchPane.getChildren().setAll(getErrorPane());
                    break;
                case INACTIVE:
                default:
                    searchPane.getChildren().clear();
                    break;
            }
        });
        viewModel.init(entry);
    }

    private VBox citationToGraphic(Citation citation) {
        HBox contextBox = new HBox(new Text(LatexToUnicodeAdapter.format(citation.getContext())));
        contextBox.getStyleClass().add("latex-citations-context-box");

        Text positionText = new Text(String.format("%n%s (%s:%s-%s)", citation.getPath().toAbsolutePath(),
                citation.getLine(), citation.getColStart(), citation.getColEnd()));
        positionText.getStyleClass().add("latex-citations-position-text");

        return new VBox(contextBox, positionText);
    }

    private ScrollPane getCitationsPane() {
        Text titleText = new Text(Localization.lang("Citations found"));
        titleText.getStyleClass().add("latex-citations-title-text");

        VBox citationsBox = new VBox(20.0, titleText);
        citationsBox.getChildren().addAll(graphicCitationList);

        ScrollPane citationsPane = new ScrollPane();
        citationsPane.setContent(citationsBox);

        return citationsPane;
    }

    private ScrollPane getNotFoundPane() {
        Text notFoundTitleText = new Text(Localization.lang("No citations found"));
        notFoundTitleText.getStyleClass().add("latex-citations-title-text");

        Text notFoundText = new Text(Localization.lang("No LaTeX files containing this entry were found."));
        notFoundText.getStyleClass().add("latex-citations-not-found-text");

        Text notFoundAdviceText = new Text(Localization.lang(
                "You can set the LaTeX file directory in the 'Library properties' dialog."));
        notFoundAdviceText.getStyleClass().add("latex-citations-not-found-advice-text");

        VBox notFoundBox = new VBox(20.0, notFoundTitleText, notFoundText, notFoundAdviceText);
        ScrollPane notFoundPane = new ScrollPane();
        notFoundPane.setContent(notFoundBox);

        return notFoundPane;
    }

    private ScrollPane getErrorPane() {
        Text errorTitleText = new Text(Localization.lang("Error"));
        errorTitleText.getStyleClass().add("latex-citations-error-title-text");

        Text errorMessageText = new Text(viewModel.searchErrorProperty().get());
        errorMessageText.getStyleClass().add("latex-citations-error-text");

        VBox errorBox = new VBox(20.0, errorTitleText, errorMessageText);
        ScrollPane errorPane = new ScrollPane();
        errorPane.setContent(errorBox);

        return errorPane;
    }

    @Override
    public boolean shouldShow(BibEntry entry) {
        return viewModel.shouldShow();
    }
}
