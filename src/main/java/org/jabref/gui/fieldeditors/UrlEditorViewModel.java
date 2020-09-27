package org.jabref.gui.fieldeditors;

import java.io.IOException;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import org.jabref.gui.DialogService;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.Field;
import org.jabref.model.strings.StringUtil;

import com.tobiasdiez.easybind.EasyBind;

public class UrlEditorViewModel extends AbstractEditorViewModel {
    private final DialogService dialogService;
    private final BooleanProperty validUrlIsNotPresent = new SimpleBooleanProperty(true);

    public UrlEditorViewModel(Field field, SuggestionProvider<?> suggestionProvider, DialogService dialogService, FieldCheckers fieldCheckers) {
        super(field, suggestionProvider, fieldCheckers);
        this.dialogService = dialogService;

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
            JabRefDesktop.openBrowser(text.get());
        } catch (IOException ex) {
            dialogService.notify(Localization.lang("Unable to open link."));
        }
    }
}
