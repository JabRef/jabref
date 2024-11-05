package org.jabref.gui.groups;

import jakarta.inject.Inject;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.jabref.gui.DialogService;
import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.groups.*;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

public class RenameGroupView extends BaseDialog<AbstractGroup> {

    @FXML
    private TextField nameField;

    @Inject
    private DialogService dialogService;


    private final BibDatabaseContext currentDatabase;
    private final @Nullable AbstractGroup editedGroup;

    public RenameGroupView(BibDatabaseContext currentDatabase,
                           @Nullable AbstractGroup editedGroup) {
        this.currentDatabase = currentDatabase;
        this.editedGroup = editedGroup;

        // set Width and Height
        setWidth(400);
        setHeight(150);

        // set Title name
        setTitle(Localization.lang("Rename group"));

        // add OK and Cancel buttons
        getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

        nameField = new TextField();
        nameField.setPromptText("Enter new group name");

        // add Input
        VBox vbox = new VBox(new Label("New group name:"), nameField);
        getDialogPane().setContent(vbox);

        // If press OK change name else return null
        setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                return resultConverter(ButtonType.OK).orElse(null);
            } else {
                // Ak sa zvolí Cancel alebo sa dialóg zavrie cez X
                return null;
            }
        });
    }

    protected Optional<AbstractGroup> resultConverter(ButtonType button) {
        if (button != ButtonType.OK) {
            return Optional.empty();
        }

        try {
            // Get new name from Input
            String newGroupName = nameField.getText().trim();

            if (editedGroup != null) {
                //set new input
                editedGroup.nameProperty().set(newGroupName);
                return Optional.of(editedGroup);
            }

            return Optional.empty();
        } catch (Exception exception) {
            dialogService.showErrorDialogAndWait(exception.getLocalizedMessage(), exception);
            return Optional.empty();
        }
    }
}

