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
    private ProgressIndicator progressIndicator;
    private ObservableList<VBox> formattedCitationList;
    private StackPane searchPane;

    public LatexCitationsTab(BibDatabaseContext databaseContext, PreferencesService preferencesService,
                              TaskExecutor taskExecutor) {
        this.viewModel = new LatexCitationsTabViewModel(databaseContext, preferencesService, taskExecutor);

        setText(Localization.lang("LaTeX citations"));
        setTooltip(new Tooltip(Localization.lang("Search citations for this entry in LaTeX files")));
        setGraphic(IconTheme.JabRefIcons.APPLICATION_TEXSTUDIO.getGraphicNode());
    }

    private void setupSearchPane() {
        progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(100.0, 100.0);

        formattedCitationList = FXCollections.observableArrayList();

        searchPane = new StackPane();
        searchPane.getStyleClass().add("latex-citations-tab");
        searchPane.getChildren().add(progressIndicator);

        setContent(searchPane);
    }

    @Override
    protected void bindToEntry(BibEntry entry) {
        setupSearchPane();

        viewModel.setEntry(entry);
        viewModel.initSearch();

        EasyBind.subscribe(viewModel.searchInProgressProperty(), stillWorking -> {
            if (!stillWorking) {
                formattedCitationList = EasyBind.map(viewModel.getCitationList(), this::citationToGraphic);
                searchPane.getChildren().setAll(
                        viewModel.successfulSearchProperty().get()
                                ? getCitationsPane()
                                : getNotFoundPane());
            }
        });
    }

    private VBox citationToGraphic(Citation citation) {
        HBox contextBox = new HBox(new Text(LatexToUnicodeAdapter.format(citation.getLineText())));
        contextBox.getStyleClass().add("latex-citations-context-box");

        Text positionText = new Text(String.format("%n%s (%s:%s-%s)", citation.getPath().toAbsolutePath(),
                citation.getLine(), citation.getColStart(), citation.getColEnd()));
        positionText.getStyleClass().add("latex-citations-position-text");

        return new VBox(contextBox, positionText);
    }

    private ScrollPane getCitationsPane() {
        Text titleText = new Text(Localization.lang("Citations found:"));
        titleText.getStyleClass().add("latex-citations-title-text");

        VBox citationsBox = new VBox(20.0, titleText);
        citationsBox.getChildren().setAll(formattedCitationList);

        ScrollPane citationsPane = new ScrollPane();
        citationsPane.setContent(citationsBox);

        return citationsPane;
    }

    private ScrollPane getNotFoundPane() {
        Text notFoundText = new Text(Localization.lang("No citations found for this entry."));
        notFoundText.getStyleClass().add("latex-citations-not-found-text");

        ScrollPane notFoundPane = new ScrollPane();
        notFoundPane.setContent(notFoundText);

        return notFoundPane;
    }

    @Override
    public boolean shouldShow(BibEntry entry) {
        return viewModel.shouldShow();
    }
}
