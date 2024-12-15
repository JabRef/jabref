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
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.texparser.CitationsDisplay;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.util.DirectoryMonitorManager;

import com.tobiasdiez.easybind.EasyBind;

public class LatexCitationsTab extends EntryEditorTab {

    public static final String NAME = "LaTeX citations";
    private final LatexCitationsTabViewModel viewModel;
    private final GridPane searchPane;
    private final ProgressIndicator progressIndicator;
    private final CitationsDisplay citationsDisplay;

    public LatexCitationsTab(BibDatabaseContext databaseContext,
                             GuiPreferences preferences,
                             DialogService dialogService,
                             DirectoryMonitorManager directoryMonitorManager) {

        this.viewModel = new LatexCitationsTabViewModel(
                databaseContext,
                preferences,
                dialogService,
                directoryMonitorManager);

        this.searchPane = new GridPane();
        this.progressIndicator = new ProgressIndicator();
        this.citationsDisplay = new CitationsDisplay();

        setText(Localization.lang("LaTeX citations"));
        setTooltip(new Tooltip(Localization.lang("Search citations for this entry in LaTeX files")));
        setGraphic(IconTheme.JabRefIcons.LATEX_CITATIONS.getGraphicNode());
        setSearchPane();
    }

    private void setSearchPane() {
        progressIndicator.setMaxSize(100, 100);

        citationsDisplay.basePathProperty().bindBidirectional(viewModel.directoryProperty());
        citationsDisplay.setItems(viewModel.getCitationList());
        citationsDisplay.setOnMouseClicked(event -> viewModel.handleMouseClick(event, citationsDisplay));

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

        HBox latexDirectoryBox = getLatexDirectoryBox();
        VBox citationsPane = getCitationsPane();
        VBox notFoundPane = getNotFoundPane();
        VBox errorPane = getErrorPane();

        EasyBind.subscribe(viewModel.statusProperty(), status -> {
            searchPane.getChildren().clear();
            switch (status) {
                case IN_PROGRESS:
                    searchPane.add(progressIndicator, 0, 0);
                    break;
                case CITATIONS_FOUND:
                    searchPane.add(citationsPane, 0, 0);
                    break;
                case NO_RESULTS:
                    searchPane.add(notFoundPane, 0, 0);
                    break;
                case ERROR:
                    searchPane.add(errorPane, 0, 0);
                    break;
            }
            searchPane.add(latexDirectoryBox, 0, 1);
        });
    }

    private HBox getLatexDirectoryBox() {
        Text latexDirectoryText = new Text(Localization.lang("Current search directory:"));
        Text latexDirectoryPath = new Text();
        latexDirectoryPath.textProperty().bind(viewModel.directoryProperty().asString());
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
        VBox.setVgrow(citationsDisplay, Priority.ALWAYS);
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
        Text errorMessageText = new Text();
        errorMessageText.textProperty().bind(viewModel.searchErrorProperty());
        VBox errorMessageBox = new VBox(30, titleLabel, errorMessageText);
        errorMessageBox.setStyle("-fx-padding: 30 0 0 30;");
        return errorMessageBox;
    }

    @Override
    protected void bindToEntry(BibEntry entry) {
        viewModel.bindToEntry(entry);
    }

    @Override
    public boolean shouldShow(BibEntry entry) {
        return viewModel.shouldShow();
    }
}
