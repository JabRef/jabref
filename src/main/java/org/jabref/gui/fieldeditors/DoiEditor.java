package org.jabref.gui.fieldeditors;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;

import org.jabref.gui.FXDialogService;
import org.jabref.gui.util.ControlHelper;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;

public class DoiEditor extends HBox implements FieldEditorFX {

    private final String fieldName;
    @FXML private EditorTextArea textArea;
    @FXML private Button fetchByDoiButton;

    public boolean isDoiIsNotPresent() {
        return doiIsNotPresent.get();
    }

    public BooleanProperty doiIsNotPresentProperty() {
        return doiIsNotPresent;
    }

    private BooleanProperty doiIsNotPresent = new SimpleBooleanProperty(true);

    public DoiEditor(String fieldName) {
        this.fieldName = fieldName;
        ControlHelper.loadFXMLForControl(this);

        doiIsNotPresent.bind(textArea.textProperty().isEmpty());

        fetchByDoiButton.setTooltip(
                new Tooltip(Localization.lang("Get BibTeX data from %0", FieldName.getDisplayName(FieldName.DOI))));
    }

    @Override
    public void bindToEntry(BibEntry entry) {
        textArea.setText(entry.getField(fieldName).orElse(""));
    }

    @Override
    public Parent getNode() {
        return this;
    }

    @FXML
    private void fetchByDoi(ActionEvent event) {
        FXDialogService dialogService = new FXDialogService();
        dialogService.showConfirmationDialogAndWait("test", "test2");
        /*
        BibEntry entry = entryEditor.getEntry();
        new FetchAndMergeEntry(entry, panel, FieldName.DOI);
        */
    }

    @FXML
    private void lookupDoi(ActionEvent event) {
        /*
        try {
            Optional<DOI> doi = WebFetchers.getIdFetcherForIdentifier(DOI.class).findIdentifier(entryEditor.getEntry());
            if (doi.isPresent()) {
                entryEditor.getEntry().setField(FieldName.DOI, doi.get().getDOI());
            } else {
                panel.frame().setStatus(Localization.lang("No %0 found", FieldName.getDisplayName(FieldName.DOI)));
            }
        } catch (FetcherException e) {
            LOGGER.error("Problem fetching DOI", e);
        }
        */
    }

    @FXML
    private void openDoi(ActionEvent event) {
        /*
        try {
            JabRefDesktop.openExternalViewer(panel.getBibDatabaseContext(), fieldEditor.getText(), fieldEditor.getFieldName());
        } catch (IOException ex) {
            panel.output(Localization.lang("Unable to open link."));
        }
        */
    }
}
