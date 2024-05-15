package org.jabref.gui.preferences.ai;

import java.util.regex.Pattern;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.strings.StringUtil;
import org.jabref.preferences.AiPreferences;
import org.jabref.preferences.PreferencesService;

public class AiTabViewModel implements PreferenceTabViewModel {
    private static final String OPENAI_TOKEN_PATTERN_STRING = "sk-[A-Za-z0-9-_]*[A-Za-z0-9]{20}T3BlbkFJ[A-Za-z0-9]{20}";
    private static final Pattern OPENAI_TOKEN_PATTERN = Pattern.compile(OPENAI_TOKEN_PATTERN_STRING);

    private final BooleanProperty useAi = new SimpleBooleanProperty();
    private final StringProperty openAiToken = new SimpleStringProperty();
    private final AiPreferences aiPreferences;
    private final DialogService dialogService;

    public AiTabViewModel(PreferencesService preferencesService, DialogService dialogService) {
        this.aiPreferences = preferencesService.getAiPreferences();
        this.dialogService = dialogService;
    }

    @Override
    public void setValues() {
        useAi.setValue(aiPreferences.isUseAi());
        openAiToken.setValue(aiPreferences.getOpenAiToken());
    }

    @Override
    public void storeSettings() {
        aiPreferences.setUseAi(useAi.get());
        aiPreferences.setOpenAiToken(openAiToken.get());
    }

    @Override
    public boolean validateSettings() {
        if (useAi.get()) {
            return validateOpenAiToken();
        }

        return true;
    }

    private boolean validateOpenAiToken() {
        if (StringUtil.isBlank(openAiToken.get())) {
            dialogService.showErrorDialogAndWait(Localization.lang("Format error"), Localization.lang("The OpenAI token cannot be empty"));
            return false;
        }

        if (!OPENAI_TOKEN_PATTERN.matcher(openAiToken.get()).matches()) {
            dialogService.showErrorDialogAndWait(Localization.lang("Format error"), Localization.lang("The OpenAI token is not valid"));
            return false;
        }

        return true;
    }

    public StringProperty openAiTokenProperty() {
        return openAiToken;
    }

    public BooleanProperty useAiProperty() {
        return useAi;
    }
}
