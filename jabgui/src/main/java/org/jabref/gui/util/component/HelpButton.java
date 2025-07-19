package org.jabref.gui.util.component;

import javafx.scene.control.Button;

import org.jabref.gui.DialogService;
import org.jabref.gui.edit.OpenBrowserAction;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.preferences.GuiPreferences;

import com.airhacks.afterburner.injection.Injector;
import org.jspecify.annotations.NonNull;

public class HelpButton extends Button {

    public HelpButton(@NonNull String url) {
        setGraphic(IconTheme.JabRefIcons.HELP.getGraphicNode());
        getStyleClass().add("icon-button");
        setPrefSize(28, 28);
        setMinSize(28, 28);
        setOnAction(_ -> new OpenBrowserAction(url,
                Injector.instantiateModelOrService(DialogService.class),
                Injector.instantiateModelOrService(GuiPreferences.class).getExternalApplicationsPreferences()
        ).execute());
    }
}
