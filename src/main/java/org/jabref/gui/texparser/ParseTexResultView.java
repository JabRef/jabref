package org.jabref.gui.texparser;

import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.strings.LatexToUnicodeAdapter;
import org.jabref.model.texparser.Citation;
import org.jabref.model.texparser.TexParserResult;

import com.airhacks.afterburner.views.ViewLoader;
import org.fxmisc.easybind.EasyBind;

public class ParseTexResultView extends BaseDialog<Void> {

    private final TexParserResult texParserResult;
    @FXML private ListView<ReferenceViewModel> referenceListView;
    @FXML private ListView<Citation> citationListView;
    private ParseTexResultViewModel viewModel;

    public ParseTexResultView(TexParserResult texParserResult) {
        this.texParserResult = texParserResult;

        this.setTitle(Localization.lang("LaTeX references search results"));

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);
    }

    @FXML
    private void initialize() {
        viewModel = new ParseTexResultViewModel(texParserResult);

        referenceListView.setItems(viewModel.getReferenceList());
        EasyBind.subscribe(referenceListView.getSelectionModel().selectedItemProperty(),
                viewModel::activeReferenceChanged);
        referenceListView.getSelectionModel().selectFirst();
        new ViewModelListCellFactory<ReferenceViewModel>()
                .withText(ReferenceViewModel::getDisplayText)
                .install(referenceListView);

        citationListView.setItems(viewModel.getCitationListByReference());
        new ViewModelListCellFactory<Citation>()
                .withGraphic(this::citationToGraphic)
                .install(citationListView);
    }

    private VBox citationToGraphic(Citation item) {
        String contextString = LatexToUnicodeAdapter.format(item.getLineText());
        String fileDataString = String.format("%n%s (%s:%s-%s)%n", item.getPath().getFileName(), item.getLine(),
                item.getColStart(), item.getColEnd());

        Text context = new Text(contextString);
        context.setWrappingWidth(citationListView.getWidth() - 50.0);
        HBox contextBox = new HBox(context);
        contextBox.getStyleClass().add("contextBox");

        Text fileData = new Text(fileDataString);
        fileData.getStyleClass().add("fileData");

        return new VBox(contextBox, fileData);
    }
}
