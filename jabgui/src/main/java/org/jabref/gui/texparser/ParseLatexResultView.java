package org.jabref.gui.texparser;

import java.nio.file.Path;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListView;
import javafx.scene.text.Text;

import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ControlHelper;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.texparser.LatexBibEntriesResolverResult;

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;

public class ParseLatexResultView extends BaseDialog<Void> {

    private final LatexBibEntriesResolverResult resolverResult;
    private final BibDatabaseContext databaseContext;
    private final Path basePath;
    @FXML private ListView<ReferenceViewModel> referenceListView;
    @FXML private CitationsDisplay citationsDisplay;
    @FXML private ButtonType importButtonType;
    private ParseLatexResultViewModel viewModel;

    public ParseLatexResultView(LatexBibEntriesResolverResult resolverResult, BibDatabaseContext databaseContext, Path basePath) {
        this.resolverResult = resolverResult;
        this.databaseContext = databaseContext;
        this.basePath = basePath;

        setTitle(Localization.lang("LaTeX Citations Search Results"));

        ViewLoader.view(this).load().setAsDialogPane(this);

        ControlHelper.setAction(importButtonType, getDialogPane(), event -> {
            viewModel.importButtonClicked();
            close();
        });
        Button importButton = (Button) getDialogPane().lookupButton(importButtonType);
        importButton.disableProperty().bind(viewModel.importButtonDisabledProperty());
    }

    @FXML
    private void initialize() {
        viewModel = new ParseLatexResultViewModel(resolverResult, databaseContext);

        referenceListView.setItems(viewModel.getReferenceList());
        referenceListView.getSelectionModel().selectFirst();
        new ViewModelListCellFactory<ReferenceViewModel>()
                .withGraphic(reference -> {
                    Text referenceText = new Text(reference.getDisplayText());
                    if (reference.isHighlighted()) {
                        referenceText.setStyle("-fx-fill: -fx-accent");
                    }
                    return referenceText;
                })
                .install(referenceListView);

        EasyBind.subscribe(referenceListView.getSelectionModel().selectedItemProperty(),
                viewModel::activeReferenceChanged);

        citationsDisplay.basePathProperty().set(basePath);
        citationsDisplay.setItems(viewModel.getCitationListByReference());
    }
}
