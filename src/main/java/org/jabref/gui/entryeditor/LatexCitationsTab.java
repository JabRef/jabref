package org.jabref.gui.entryeditor;

import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import org.jabref.gui.DialogService;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.texparser.CitationsDisplay;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.PreferencesService;

import com.tobiasdiez.easybind.EasyBind;

public class LatexCitationsTab extends EntryEditorTab {

    private final LatexCitationsTabViewModel viewModel;
    private final GridPane searchPane;
    private final ProgressIndicator progressIndicator;
    private final CitationsDisplay citationsDisplay;

    public LatexCitationsTab(BibDatabaseContext databaseContext, PreferencesService preferencesService,
                             TaskExecutor taskExecutor, DialogService dialogService) {
        this.viewModel = new LatexCitationsTabViewModel(databaseContext, preferencesService, taskExecutor, dialogService);
        this.searchPane = new GridPane();
        this.progressIndicator = new ProgressIndicator();
        this.citationsDisplay = new CitationsDisplay();

        setText(Localization.lang("LaTeX Citations"));
        setTooltip(new Tooltip(Localization.lang("Search citations for this entry in LaTeX files")));
        setGraphic(IconTheme.JabRefIcons.LATEX_CITATIONS.getGraphicNode());
        setSearchPane();
    }

    private void setSearchPane() {
        progressIndicator.setMaxSize(100, 100);
        citationsDisplay.basePathProperty().bindBidirectional(viewModel.directoryProperty());
        citationsDisplay.setItems(viewModel.getCitationList());

        RowConstraints mainRow = new RowConstraints();
        mainRow.setVgrow(Priority.ALWAYS);

        RowConstraints bottomRow = new RowConstraints(40);
        bottomRow.setVgrow(Priority.NEVER);

        ColumnConstraints column = new ColumnConstraints();
        column.setPercentWidth(100);
        column.setHalignment(HPos.CENTER);

        searchPane.getColumnConstraints().setAll(column);
        searchPane.getRowConstraints().setAll(mainRow, bottomRow);
        searchPane.setId("citationsPane");
        setContent(searchPane);

        EasyBind.subscribe(viewModel.statusProperty(), status -> {
            searchPane.getChildren().clear();
            switch (status) {
                case IN_PROGRESS:
                    searchPane.add(progressIndicator, 0, 0);
                    break;
                case CITATIONS_FOUND:
                    searchPane.add(getCitationsPane(), 0, 0);
                    break;
                case NO_RESULTS:
                    searchPane.add(getNotFoundPane(), 0, 0);
                    break;
                case ERROR:
                    searchPane.add(getErrorPane(), 0, 0);
                    break;
            }
            searchPane.add(getLatexDirectoryBox(), 0, 1);
        });
    }

    private HBox getLatexDirectoryBox() {
        Text latexDirectoryText = new Text(Localization.lang("Current search directory:"));
        Text latexDirectoryPath = new Text(viewModel.directoryProperty().get().toString());
        latexDirectoryPath.setStyle("-fx-font-family:monospace;-fx-font-weight: bold;");
        Button latexDirectoryButton = new Button(Localization.lang("Set LaTeX file directory"));
        latexDirectoryButton.setGraphic(IconTheme.JabRefIcons.LATEX_FILE_DIRECTORY.getGraphicNode());
        latexDirectoryButton.setOnAction(event -> viewModel.setLatexDirectory());
        HBox latexDirectoryBox = new HBox(10, latexDirectoryText, latexDirectoryPath, latexDirectoryButton);
        latexDirectoryBox.setAlignment(Pos.CENTER);
        return latexDirectoryBox;
    }

    private VBox getCitationsPane() {
        VBox citationsBox = new VBox(30, citationsDisplay);
        citationsBox.setStyle("-fx-padding: 0;");
        return citationsBox;
    }

    private VBox getNotFoundPane() {
        Label titleLabel = new Label(Localization.lang("No citations found"));
        titleLabel.getStyleClass().add("heading");

        Text notFoundText = new Text(Localization.lang("No LaTeX files containing this entry were found."));
        notFoundText.getStyleClass().add("description");

        VBox notFoundBox = new VBox(30, titleLabel, notFoundText);
        notFoundBox.setStyle("-fx-padding: 30 0 0 30;");
        return notFoundBox;
    }

    private VBox getErrorPane() {
        Label titleLabel = new Label(Localization.lang("Error"));
        titleLabel.setStyle("-fx-font-size: 1.5em;-fx-font-weight: bold;-fx-text-fill: -fx-accent;");
        Text errorMessageText = new Text(viewModel.searchErrorProperty().get());
        VBox errorMessageBox = new VBox(30, titleLabel, errorMessageText);
        errorMessageBox.setStyle("-fx-padding: 30 0 0 30;");
        return errorMessageBox;
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
