package org.jabref.gui.fieldeditors;

import java.io.IOException;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import org.jabref.gui.DialogService;
import org.jabref.gui.autocompleter.AutoCompleteSuggestionProvider;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.net.URLUtil;
import org.jabref.model.strings.StringUtil;

import org.fxmisc.easybind.EasyBind;

public class UrlEditorViewModel extends AbstractEditorViewModel {
    private DialogService dialogService;
    private BooleanProperty validUrlIsNotPresent = new SimpleBooleanProperty(true);

    public UrlEditorViewModel(String fieldName, AutoCompleteSuggestionProvider<?> suggestionProvider, DialogService dialogService, FieldCheckers fieldCheckers) {
        super(fieldName, suggestionProvider, fieldCheckers);
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
