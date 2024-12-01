package org.jabref.gui.ai.components.guards.privacynotice;

import javafx.scene.Node;

import org.jabref.gui.DialogService;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.util.guards.ComponentGuard;
import org.jabref.logic.ai.AiPreferences;

public class AiPrivacyNoticeGuard extends ComponentGuard {
    private final AiPreferences aiPreferences;
    private final ExternalApplicationsPreferences externalApplicationsPreferences;
    private final DialogService dialogService;

    public AiPrivacyNoticeGuard(
            AiPreferences aiPreferences,
            ExternalApplicationsPreferences externalApplicationsPreferences,
            DialogService dialogService
    ) {
        this.aiPreferences = aiPreferences;
        this.externalApplicationsPreferences = externalApplicationsPreferences;
        this.dialogService = dialogService;

        this.bind(aiPreferences.enableAiProperty());
    }

    @Override
    public Node getExplanation() {
        return new AiPrivacyNoticeComponent(
                aiPreferences,
                externalApplicationsPreferences,
                dialogService
        );
    }
}
