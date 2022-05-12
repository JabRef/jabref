package org.jabref.gui.preferences.protectedterms;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.protectedterms.ProtectedTermsList;
import org.jabref.logic.util.StandardFileType;
import org.jabref.preferences.FilePreferences;

public class NewProtectedTermsFileDialog extends BaseDialog<Void> {

    private final TextField newFile = new TextField();
    private final DialogService dialogService;

    public NewProtectedTermsFileDialog(List<ProtectedTermsListItemModel> termsLists, DialogService dialogService, FilePreferences filePreferences) {
        this.dialogService = dialogService;

        this.setTitle(Localization.lang("New protected terms file"));

        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(Localization.lang("Protected terms file"), StandardFileType.TERMS)
                .withDefaultExtension(Localization.lang("Protected terms file"), StandardFileType.TERMS)
                .withInitialDirectory(filePreferences.getWorkingDirectory())
                .build();

        Button browse = new Button(Localization.lang("Browse"));
        browse.setOnAction(event -> this.dialogService.showFileSaveDialog(fileDialogConfiguration)
                                                      .ifPresent(file -> newFile.setText(file.toAbsolutePath().toString())));

        TextField newDescription = new TextField();
        VBox container = new VBox(10,
                new VBox(5, new Label(Localization.lang("Description")), newDescription),
                new VBox(5, new Label(Localization.lang("File")), new HBox(10, newFile, browse))
        );
        getDialogPane().setContent(container);

        getDialogPane().getButtonTypes().setAll(
                ButtonType.OK,
                ButtonType.CANCEL
        );

        setResultConverter(button -> {
            if (button == ButtonType.OK) {
                ProtectedTermsList newList = new ProtectedTermsList(newDescription.getText(), new ArrayList<>(), newFile.getText(), false);
                newList.setEnabled(true);
                newList.createAndWriteHeading(newDescription.getText());
                termsLists.add(new ProtectedTermsListItemModel(newList));
            }
            return null;
        });
    }
}
