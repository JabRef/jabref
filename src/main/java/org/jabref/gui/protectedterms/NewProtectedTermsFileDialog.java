package org.jabref.gui.protectedterms;

import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.protectedterms.ProtectedTermsLoader;
import org.jabref.logic.util.StandardFileType;

public class NewProtectedTermsFileDialog extends BaseDialog<Void> {

    private final TextField newFile = new TextField();
    private final DialogService dialogService;

    public NewProtectedTermsFileDialog(ProtectedTermsLoader termsLoader, DialogService dialogService) {
        this.dialogService = dialogService;

        this.setTitle(Localization.lang("New protected terms file"));

        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(Localization.lang("Protected terms file"), StandardFileType.TERMS)
                .withDefaultExtension(Localization.lang("Protected terms file"), StandardFileType.TERMS)
                .withInitialDirectory(Globals.prefs.getWorkingDir())
                .build();

        Button browse = new Button(Localization.lang("Browse"));
        browse.setOnAction(event -> {
            this.dialogService.showFileSaveDialog(fileDialogConfiguration)
                              .ifPresent(file -> newFile.setText(file.toAbsolutePath().toString()));
        });

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
                termsLoader.addNewProtectedTermsList(newDescription.getText(), newFile.getText(), true);
            }
            return null;
        });
    }
}
