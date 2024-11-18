package org.jabref.gui.groups;

import java.util.Optional;

import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.groups.AbstractGroup;

import jakarta.inject.Inject;

public class RenameGroupView extends BaseDialog<AbstractGroup> {

    @FXML
    private TextField nameField;

    @Inject
    private DialogService dialogService;


    private final BibDatabaseContext currentDatabase;
    private final AbstractGroup editedGroup;

    public RenameGroupView(BibDatabaseContext currentDatabase,
                           AbstractGroup editedGroup) {
        this.currentDatabase = currentDatabase;
        this.editedGroup = editedGroup;

        setWidth(400);
        setHeight(150);

        setTitle(Localization.lang("Rename group"));

        getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

        nameField = new TextField();
        nameField.setPromptText(Localization.lang("Enter new group name"));

        VBox vbox = new VBox(new Label(Localization.lang("New group name")), nameField);
        getDialogPane().setContent(vbox);

        setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                return resultConverter(ButtonType.OK).orElse(null);
            } else {
                return null;
            }
        });
    }

    protected Optional<AbstractGroup> resultConverter(ButtonType button) {
        if (button != ButtonType.OK) {
            return Optional.empty();
        }

        try {
            String newGroupName = nameField.getText().trim();

            if (editedGroup != null) {
                editedGroup.nameProperty().setValue(newGroupName);
                return Optional.of(editedGroup);
            }

            return Optional.empty();
        } catch (Exception exception) {
            dialogService.showErrorDialogAndWait(exception.getLocalizedMessage(), exception);
            return Optional.empty();
        }
    }
}

