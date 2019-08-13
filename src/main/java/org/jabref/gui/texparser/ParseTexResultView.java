package org.jabref.gui.texparser;

import java.nio.file.Path;

import javafx.fxml.FXML;
import javafx.scene.control.ListView;

import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.texparser.TexParserResult;

import com.airhacks.afterburner.views.ViewLoader;
import org.fxmisc.easybind.EasyBind;

public class ParseTexResultView extends BaseDialog<Void> {

    private final TexParserResult texParserResult;
    private final Path basePath;
    @FXML private ListView<ReferenceViewModel> referenceListView;
    @FXML private CitationsDisplay citationsDisplay;
    private ParseTexResultViewModel viewModel;

    public ParseTexResultView(TexParserResult texParserResult, Path basePath) {
        this.texParserResult = texParserResult;
        this.basePath = basePath;

        setTitle(Localization.lang("LaTeX Citations Search Results"));

        ViewLoader.view(this).load().setAsDialogPane(this);
    }

    @FXML
    private void initialize() {
        viewModel = new ParseTexResultViewModel(texParserResult);

        referenceListView.setItems(viewModel.getReferenceList());
        referenceListView.getSelectionModel().selectFirst();
        new ViewModelListCellFactory<ReferenceViewModel>()
                .withText(ReferenceViewModel::getDisplayText)
                .install(referenceListView);

        EasyBind.subscribe(referenceListView.getSelectionModel().selectedItemProperty(),
                viewModel::activeReferenceChanged);

        citationsDisplay.basePathProperty().set(basePath);
        citationsDisplay.setItems(viewModel.getCitationListByReference());
    }
}
