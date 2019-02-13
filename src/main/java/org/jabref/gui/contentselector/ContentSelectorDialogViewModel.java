package org.jabref.gui.contentselector;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.jabref.gui.DialogService;
import org.jabref.model.entry.FieldName;
import org.jabref.model.metadata.ContentSelectors;
import org.jabref.model.metadata.MetaData;

import java.util.List;

import static com.google.common.collect.ImmutableList.of;

class ContentSelectorDialogViewModel {

    private static final List<String> DEFAULT_FIELD_NAMES = of(FieldName.AUTHOR, FieldName.JOURNAL, FieldName.KEYWORDS, FieldName.PUBLISHER);

    private final MetaData metaData;
    private final DialogService dialogService;

    private ObservableList<String> fieldNames = FXCollections.observableArrayList();

    ContentSelectorDialogViewModel(MetaData metaData, DialogService dialogService) {
        this.metaData = metaData;
        this.dialogService = dialogService;
    }

    ObservableList<String> loadFieldNames() {
        ContentSelectors contentSelectors = metaData.getContentSelectors();
        fieldNames.addAll(contentSelectors.getFieldNamesWithSelectors());

        if (fieldNames.isEmpty()) {
            fieldNames.addAll(DEFAULT_FIELD_NAMES);
        }
        return fieldNames;
    }

    void showInputFieldNameDialog() {
        dialogService.showInputDialogAndWait("Add new field name", "Field name:")
                .ifPresent(fieldNames::add);
    }

    void showRemoveFieldNameConfirmationDialog(String fieldNameToRemove) {
        if (fieldNameToRemove == null) {
            dialogService.showErrorDialogAndWait("No field name selected!");
            return;
        }

        boolean deleteConfirmed = dialogService.showConfirmationDialogAndWait("Remove field name", "Are you sure you want to remove field name: \"" + fieldNameToRemove + "\"");
        if (deleteConfirmed) {
            fieldNames.remove(fieldNameToRemove);
        }
    }
}
