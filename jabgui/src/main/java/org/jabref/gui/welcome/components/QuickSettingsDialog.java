package org.jabref.gui.welcome.components;

import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;

import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.gui.util.component.HelpButton;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.injection.Injector;
import org.jspecify.annotations.NonNull;

public class QuickSettingsDialog {
    private final Dialog<ButtonType> dialog;
    private final VBox content;
    private BooleanSupplier validationSupplier = () -> true;
    private List<ObservableValue<?>> dependencies = List.of();
    private final DialogService dialogService;
    private final ThemeManager themeManager;

    public QuickSettingsDialog() {
        this.dialog = new Dialog<>();
        this.content = new VBox();
        this.dialogService = Injector.instantiateModelOrService(DialogService.class);
        this.themeManager = Injector.instantiateModelOrService(ThemeManager.class);
        content.getStyleClass().add("quick-settings-dialog-container");
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    }

    public QuickSettingsDialog title(String titleKey) {
        dialog.setTitle(Localization.lang(titleKey));
        return this;
    }

    public QuickSettingsDialog header(String headerKey) {
        dialog.setHeaderText(Localization.lang(headerKey));
        return this;
    }

    public QuickSettingsDialog validate(BooleanSupplier validationSupplier) {
        this.validationSupplier = validationSupplier;
        return this;
    }

    public QuickSettingsDialog depend(@NonNull List<ObservableValue<?>> dependencies) {
        this.dependencies = dependencies;
        return this;
    }

    public QuickSettingsDialog content(Node... children) {
        content.getChildren().addAll(children);
        return this;
    }

    public Optional<ButtonType> show() {
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setDisable(!validationSupplier.getAsBoolean());
        dependencies.forEach(obs -> obs.addListener((_, _, _) -> okButton.setDisable(!validationSupplier.getAsBoolean())));

        themeManager.updateFontStyle(dialog.getDialogPane().getScene());
        return dialogService.showCustomDialogAndWait(dialog);
    }

    public static HBox createHeaderWithHelp(String localizationKey, String helpUrl, Object... params) {
        Label headerLabel = new Label(Localization.lang(localizationKey, params));
        headerLabel.setWrapText(true);
        return new HBox(headerLabel, new HelpButton(helpUrl));
    }
}
