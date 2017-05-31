package org.jabref.gui.fieldeditors;

import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;

import org.jabref.gui.DialogService;
import org.jabref.gui.util.ControlHelper;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import de.jensd.fx.glyphs.materialdesignicons.utils.MaterialDesignIconFactory;

public class LinkedFilesEditor extends HBox implements FieldEditorFX {

    private final String fieldName;
    @FXML private LinkedFilesEditorViewModel viewModel;
    @FXML private ListView<LinkedFileViewModel> listView;

    public LinkedFilesEditor(String fieldName, DialogService dialogService, BibDatabaseContext databaseContext, TaskExecutor taskExecutor) {
        this.fieldName = fieldName;
        this.viewModel = new LinkedFilesEditorViewModel(dialogService, databaseContext, taskExecutor);

        ControlHelper.loadFXMLForControl(this);

        ViewModelListCellFactory<LinkedFileViewModel> cellFactory = new ViewModelListCellFactory<LinkedFileViewModel>()
                .withTooltip(LinkedFileViewModel::getDescription)
                .withGraphic(LinkedFilesEditor::createFileDisplay);
        listView.setCellFactory(cellFactory);
        Bindings.bindContent(listView.itemsProperty().get(), viewModel.filesProperty());
    }

    private static Node createFileDisplay(LinkedFileViewModel linkedFile) {
        Text icon = MaterialDesignIconFactory.get().createIcon(linkedFile.getTypeIcon());
        Text text = new Text(linkedFile.getLink());
        ProgressBar progressIndicator = new ProgressBar();
        progressIndicator.progressProperty().bind(linkedFile.downloadProgressProperty());
        progressIndicator.visibleProperty().bind(linkedFile.downloadOngoingProperty());

        Button acceptAutoLinkedFile = MaterialDesignIconFactory.get().createIconButton(MaterialDesignIcon.BRIEFCASE_CHECK);
        acceptAutoLinkedFile.setTooltip(new Tooltip(Localization.lang("This file was found automatically. Do you want to link it to this entry?")));
        acceptAutoLinkedFile.visibleProperty().bind(linkedFile.isAutomaticallyFoundProperty());
        acceptAutoLinkedFile.setOnAction(event -> linkedFile.acceptAsLinked());
        acceptAutoLinkedFile.getStyleClass().setAll("flatButton");

        HBox container = new HBox(10);
        container.setPrefHeight(Double.NEGATIVE_INFINITY);
        container.getChildren().addAll(icon, text, progressIndicator, acceptAutoLinkedFile);
        return container;
    }

    public LinkedFilesEditorViewModel getViewModel() {
        return viewModel;
    }

    @Override
    public void bindToEntry(BibEntry entry) {
        viewModel.bindToEntry(fieldName, entry);
    }

    @Override
    public Parent getNode() {
        return this;
    }

    @FXML
    private void addNewFile(ActionEvent event) {
        viewModel.addNewFile();
    }

    @FXML
    private void fetchFulltext(ActionEvent event) {
        viewModel.fetchFulltext();
    }

    @FXML
    private void addFromURL(ActionEvent event) {
        viewModel.addFromURL();
    }

}
