package org.jabref.gui.texparser;

import java.util.Set;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.fxml.FXML;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.texparser.Citation;
import org.jabref.model.texparser.TexParserResult;

import com.airhacks.afterburner.views.ViewLoader;

public class ParseTexResult extends BaseDialog<Void> {

    private final TexParserResult texParserResult;
    @FXML private ListView<ReferenceWrapper> referenceListView;
    @FXML private ListView<Citation> citationListView;

    public ParseTexResult(TexParserResult texParserResult) {
        super();
        this.texParserResult = texParserResult;
        this.setTitle(Localization.lang("Bibliographic entries search"));
        ViewLoader.view(this).load().setAsDialogPane(this);
    }

    @FXML
    private void initialize() {
        ObservableList<ReferenceWrapper> references = getCitationViews();
        ObservableList<Citation> citationListByEntry = FXCollections.observableArrayList();

        referenceListView.setItems(references.sorted());
        referenceListView.setCellFactory(listView -> new ReferenceCell());
        referenceListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (observable != null && observable.getValue() != null) {
                citationListByEntry.clear();
                citationListByEntry.addAll(observable.getValue().citationListProperty());
            }
        });
        referenceListView.getSelectionModel().selectFirst();
        citationListView.setItems(citationListByEntry);
        citationListView.setCellFactory(listView -> new CitationCell());
    }

    private ObservableList<ReferenceWrapper> getCitationViews() {
        ObservableList<ReferenceWrapper> citationViews = FXCollections.observableArrayList();

        for (String key : texParserResult.getCitationsKeySet()) {
            Set<Citation> citations = texParserResult.getCitationsByKey(key);
            ReferenceWrapper entry = new ReferenceWrapper(key, citations.size());

            citations.forEach(entry.citationListProperty()::add);
            citationViews.add(entry);
        }

        return citationViews;
    }

    private class ReferenceCell extends ListCell<ReferenceWrapper> {
        @Override
        public void updateItem(ReferenceWrapper item, boolean empty) {
            super.updateItem(item, empty);

            if (item != null) {
                setText(String.format("%s (%s)", item.getEntry(), item.getCount()));
            }
        }
    }

    private class CitationCell extends ListCell<Citation> {
        @Override
        public void updateItem(Citation item, boolean empty) {
            super.updateItem(item, empty);

            if (item == null || empty) {
                setGraphic(null);
            } else {
                Text text = new Text(String.format("%s", item.getLineText()));
                text.setWrappingWidth(citationListView.getWidth() - 50.0);
                HBox hBox = new HBox(text);
                hBox.getStyleClass().add("context");
                Text data = new Text(String.format("%n%s (%s:%s-%s)", item.getPath().getFileName(),
                        item.getLine(), item.getColStart(), item.getColEnd()));
                data.getStyleClass().add("data");
                VBox vBox = new VBox(hBox, data);
                setGraphic(vBox);
            }
        }
    }

    class ReferenceWrapper {
        private String entry;
        private int count;
        private final ObservableSet<Citation> citationList;

        public ReferenceWrapper(String entry, int count) {
            this.entry = entry;
            this.count = count;
            this.citationList = FXCollections.observableSet();
        }

        public final String getEntry() {
            return entry;
        }

        public final int getCount() {
            return count;
        }

        public ObservableSet<Citation> citationListProperty() {
            return citationList;
        }
    }
}
