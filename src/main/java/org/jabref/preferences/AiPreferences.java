package org.jabref.preferences;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class AiPreferences {
    private final BooleanProperty useAi;
    private final StringProperty openAiToken;

    public AiPreferences(boolean useAi, String openAiToken) {
        this.useAi = new SimpleBooleanProperty(useAi);
        this.openAiToken = new SimpleStringProperty(openAiToken);
    }

    public BooleanProperty useAiProperty() {
        return useAi;
    }

    public boolean isUseAi() {
        return useAi.get();
    }

    public void setUseAi(boolean useAi) {
        this.useAi.set(useAi);
    }

    public StringProperty openAiTokenProperty() {
        return openAiToken;
    }

    public String getOpenAiToken() {
        return openAiToken.get();
    }

    public void setOpenAiToken(String openAiToken) {
        this.openAiToken.set(openAiToken);
    }
}
