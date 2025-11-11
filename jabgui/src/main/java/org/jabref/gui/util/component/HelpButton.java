package org.jabref.gui.util.component;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Button;

import org.jabref.gui.DialogService;
import org.jabref.gui.edit.OpenBrowserAction;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.logic.help.HelpFile;

import com.airhacks.afterburner.injection.Injector;
import org.jspecify.annotations.NonNull;

public class HelpButton extends Button {
    private final StringProperty helpUrl = new SimpleStringProperty("");
    private boolean isHelpFileSet = false;

    public HelpButton() {
        setGraphic(IconTheme.JabRefIcons.HELP.getGraphicNode());
        getStyleClass().add("icon-button");
        setPrefSize(28, 28);
        setMinSize(28, 28);
        BindingsHelper.subscribeFuture(helpUrl, (url) -> {
            if (url == null || url.isBlank()) {
                throw new IllegalArgumentException("Help URL must be non-null and non-blank");
            }
            if (isHelpFileSet) {
                throw new IllegalStateException("Help file has already been set");
            }

            setOnAction(_ -> new OpenBrowserAction(
                    helpUrl.get(),
                    Injector.instantiateModelOrService(DialogService.class),
                    Injector.instantiateModelOrService(GuiPreferences.class).getExternalApplicationsPreferences()
            ).execute());
        });
    }

    public HelpButton(@NonNull String url) {
        this();
        setHelpUrl(url);
    }

    public void setHelpFile(@NonNull HelpFile helpFile,
                            @NonNull DialogService dialogService,
                            @NonNull ExternalApplicationsPreferences externalApplicationsPreferences) {
        if (isHelpFileSet) {
            throw new IllegalStateException("Help file has already been set");
        }
        isHelpFileSet = true;
        if (!helpUrl.get().isBlank()) {
            throw new IllegalArgumentException("You cannot set both a help URL and a help file");
        }
        setOnAction(_ -> new HelpAction(helpFile, dialogService, externalApplicationsPreferences).execute());
    }

    /// This method is not used by JabRef, but provided for FXML usage.
    public StringProperty helpUrlProperty() {
        return helpUrl;
    }

    /// This method is not used by JabRef, but provided for FXML usage.
    public String getHelpUrl() {
        return helpUrl.get();
    }

    /// This method is used both internally in JabRef's code base, but also
    /// for FXML usage. Do not refactor this.
    public void setHelpUrl(@NonNull String helpUrl) {
        this.helpUrl.setValue(helpUrl);
    }
}
