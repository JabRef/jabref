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

public class LatexReferencesTab extends EntryEditorTab {

    private final LatexReferencesTabViewModel viewModel;
    private ProgressIndicator progressIndicator;
    private ObservableList<VBox> formattedCitationList;
    private StackPane searchPane;

    public LatexReferencesTab(BibDatabaseContext databaseContext, PreferencesService preferencesService,
                              TaskExecutor taskExecutor) {
        this.viewModel = new LatexReferencesTabViewModel(databaseContext, preferencesService, taskExecutor);

        setText(Localization.lang("LaTeX references"));
        setTooltip(new Tooltip(Localization.lang("Search this reference in the LaTeX file directory")));
        setGraphic(IconTheme.JabRefIcons.APPLICATION_TEXSTUDIO.getGraphicNode());
    }

    private void setupSearchPane() {
        progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(100.0, 100.0);

        formattedCitationList = FXCollections.observableArrayList();

        searchPane = new StackPane();
        searchPane.getStyleClass().add("latexReferences-tab");

        setContent(searchPane);
    }

    @Override
    protected void bindToEntry(BibEntry entry) {
        setupSearchPane();

        progressIndicator.visibleProperty().bindBidirectional(viewModel.searchInProgressProperty());

        viewModel.setEntry(entry);
        viewModel.initSearch();

        EasyBind.subscribe(viewModel.successfulSearchProperty(), success -> {
            searchPane.getChildren().setAll(progressIndicator);
            if (success) {
                formattedCitationList = EasyBind.map(viewModel.getCitationList(), this::citationToVBox);
                searchPane.getChildren().add(getCitationsPane());
            } else {
                searchPane.getChildren().add(getNotFoundPane());
            }
        });
    }

    private VBox citationToVBox(Citation citation) {
        HBox contextBox = new HBox(new Text(LatexToUnicodeAdapter.format(citation.getLineText())));
        contextBox.getStyleClass().add("latexReferences-contextBox");

        Text fileData = new Text(String.format("%n%s (%s:%s-%s)", citation.getPath().toAbsolutePath(),
                citation.getLine(), citation.getColStart(), citation.getColEnd()));
        fileData.getStyleClass().add("latexReferences-fileData");

        return new VBox(contextBox, fileData);
    }

    private ScrollPane getCitationsPane() {
        Text titleText = new Text(Localization.lang("References found:"));
        titleText.getStyleClass().add("latexReferences-title");

        VBox vBox = new VBox(20.0, titleText);
        vBox.getChildren().setAll(formattedCitationList);

        ScrollPane citationsPane = new ScrollPane();
        citationsPane.setContent(vBox);

        return citationsPane;
    }

    private ScrollPane getNotFoundPane() {
        Text descriptionText = new Text(Localization.lang("No references found for this entry."));
        descriptionText.getStyleClass().add("latexReferences-description");

        ScrollPane notFoundPane = new ScrollPane();
        notFoundPane.setContent(descriptionText);

        return notFoundPane;
    }

    @Override
    public boolean shouldShow(BibEntry entry) {
        return viewModel.shouldShow();
    }
}
