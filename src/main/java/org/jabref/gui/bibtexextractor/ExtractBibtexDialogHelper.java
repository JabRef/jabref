package org.jabref.gui.bibtexextractor;

import javafx.application.Platform;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;

import org.jabref.gui.ClipBoardManager;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.strings.StringUtil;

public class ExtractBibtexDialogHelper {

    static void initialize(TextArea input, ButtonType parseButtonType, StringProperty viewModelinputTextProperty, DialogPane dialogPane, Runnable parseAction) {
        input.textProperty().bindBidirectional(viewModelinputTextProperty);
        String clipText = ClipBoardManager.getContents();
        if (StringUtil.isBlank(clipText)) {
            input.setPromptText(Localization.lang("Please enter the plain references to extract from separated by double empty lines."));
        } else {
            input.setText(clipText);
            input.selectAll();
        }

        Platform.runLater(() -> {
            input.requestFocus();
            Button buttonParse = (Button) dialogPane.lookupButton(parseButtonType);
            buttonParse.setTooltip(new Tooltip((Localization.lang("Starts the extraction and adds the resulting entries to the currently opened database"))));
            buttonParse.setOnAction(event -> parseAction.run());
            buttonParse.disableProperty().bind(viewModelinputTextProperty.isEmpty());
        });
    }
}
