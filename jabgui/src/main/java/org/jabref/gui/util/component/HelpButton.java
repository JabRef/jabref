package org.jabref.gui.util.component;

import javafx.scene.control.Button;

import org.jabref.gui.DialogService;
import org.jabref.gui.edit.OpenBrowserAction;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.help.HelpFile;

import com.airhacks.afterburner.injection.Injector;
import org.jspecify.annotations.NonNull;

public class HelpButton extends Button {
    public HelpButton() {
        setGraphic(IconTheme.JabRefIcons.HELP.getGraphicNode());
        getStyleClass().add("icon-button");
        setPrefSize(28, 28);
        setMinSize(28, 28);
    }

    public HelpButton(@NonNull String url) {
        this();
        setHelpPage(url);
    }

    public void setHelpPage(@NonNull String helpDocumentationUrl) {
        setOnAction(
                _ -> new OpenBrowserAction(helpDocumentationUrl,
                        Injector.instantiateModelOrService(DialogService.class),
                        Injector.instantiateModelOrService(GuiPreferences.class).getExternalApplicationsPreferences()
                ).execute()
        );
    }

    public void setHelpFile(@NonNull HelpFile helpFile, @NonNull DialogService dialogService, @NonNull ExternalApplicationsPreferences externalApplicationsPreferences) {
        setOnAction(_ -> new HelpAction(helpFile, dialogService, externalApplicationsPreferences).execute());
    }
}
