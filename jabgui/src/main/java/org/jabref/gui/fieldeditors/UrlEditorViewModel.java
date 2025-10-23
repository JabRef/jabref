package org.jabref.gui.fieldeditors;

import java.io.IOException;

import javax.swing.undo.UndoManager;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import org.jabref.gui.DialogService;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.URLUtil;
import org.jabref.model.entry.field.Field;
import org.jabref.logic.util.strings.StringUtil;

import com.tobiasdiez.easybind.EasyBind;

public class UrlEditorViewModel extends AbstractEditorViewModel {
    private final DialogService dialogService;
    private final GuiPreferences preferences;
    private final BooleanProperty validUrlIsNotPresent = new SimpleBooleanProperty(true);

    public UrlEditorViewModel(Field field,
                              SuggestionProvider<?> suggestionProvider,
                              DialogService dialogService,
                              GuiPreferences preferences,
                              FieldCheckers fieldCheckers, UndoManager undoManager) {
        super(field, suggestionProvider, fieldCheckers, undoManager);
        this.dialogService = dialogService;
        this.preferences = preferences;

        validUrlIsNotPresent.bind(
                EasyBind.map(text, input -> StringUtil.isBlank(input) || !URLUtil.isURL(input))
        );
    }

    public boolean isValidUrlIsNotPresent() {
        return validUrlIsNotPresent.get();
    }

    public BooleanProperty validUrlIsNotPresentProperty() {
        return validUrlIsNotPresent;
    }

    public void openExternalLink() {
        if (StringUtil.isBlank(text.get())) {
            return;
        }

        try {
            NativeDesktop.openBrowser(text.get(), preferences.getExternalApplicationsPreferences());
        } catch (IOException ex) {
            dialogService.notify(Localization.lang("Unable to open link."));
        }
    }
}
