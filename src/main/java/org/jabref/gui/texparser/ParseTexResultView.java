package org.jabref.gui.texparser;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
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

public class ParseTexResultView extends BaseDialog<Void> {

    @FXML private ListView<ReferenceWrapper> referenceListView;
    @FXML private ListView<Citation> citationListView;

    private TexParserResult texParserResult;
    private ParseTexResultViewModel viewModel;

    public ParseTexResultView(TexParserResult texParserResult) {
        this.texParserResult = texParserResult;

        this.setTitle(Localization.lang("Bibliographic entries search"));

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);
    }

    @FXML
    private void initialize() {
        viewModel = new ParseTexResultViewModel(texParserResult);

        referenceListView.itemsProperty().setValue(viewModel.getReferenceList());
        referenceListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (observable != null && observable.getValue() != null) {
                viewModel.getCitationList().setAll(observable.getValue().citationListProperty());
            }
        });
        referenceListView.getSelectionModel().selectFirst();
        new ViewModelListCellFactory<ReferenceWrapper>()
                .withText(ref -> ref.toString())
                .install(referenceListView);

        citationListView.setItems(viewModel.getCitationList());
        new ViewModelListCellFactory<Citation>()
                .withGraphic(this::citationToGraphic)
                .install(citationListView);
    }

    private VBox citationToGraphic(Citation item) {
        String contextString = LatexToUnicodeAdapter.format(item.getLineText());
        String fileDataString = String.format("%n%s (%s:%s-%s)%n", item.getPath().getFileName(), item.getLine(),
                item.getColStart(), item.getColEnd());
        String jumpString = Localization.lang("Jump to file");

        Text context = new Text(contextString);
        context.setWrappingWidth(citationListView.getWidth() - 50.0);
        HBox contextBox = new HBox(context);
        contextBox.getStyleClass().add("contextBox");

        Text fileData = new Text(fileDataString);
        fileData.getStyleClass().add("fileData");
        Button jumpButton = new Button(jumpString);
        jumpButton.getStyleClass().add("jumpButton");
        jumpButton.setOnAction(e -> viewModel.jumpToFile(item.getPath(), item.getLine(), item.getColStart()));
        HBox fileBox = new HBox(15, fileData, jumpButton);

        return new VBox(contextBox, fileBox);
    }
}
