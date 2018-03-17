package org.jabref.gui.filelist;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;

import org.jabref.gui.AbstractReturnDialogView;
import org.jabref.gui.FXDialog;
import org.jabref.logic.l10n.Localization;

public class LinkedFileEditDialogView extends AbstractReturnDialogView<Boolean> {

    public LinkedFileEditDialogView(LinkedFilesWrapper linkedFile) {
        super(createContext(linkedFile));
    }

    private static Function<String, Object> createContext(LinkedFilesWrapper linkedFilesWrapper) {
        Map<String, Object> context = new HashMap<>();
        context.put("linkedFilesWrapper", linkedFilesWrapper);
        return context::get;
    }

    /**
     * @return true if the user accepts the change
     */
    @Override
    public Boolean showAndWait() {
        FXDialog dialog = new FXDialog(AlertType.INFORMATION, Localization.lang("Edit linked file"));
        dialog.setDialogPane((DialogPane) this.getView());
        dialog.getButtonTypes().addAll(ButtonType.APPLY, ButtonType.CANCEL);
        dialog.setResizable(true);
        Optional<ButtonType> buttonPressed = dialog.showAndWait();
        return buttonPressed.equals(Optional.of(ButtonType.APPLY));
    }
}
