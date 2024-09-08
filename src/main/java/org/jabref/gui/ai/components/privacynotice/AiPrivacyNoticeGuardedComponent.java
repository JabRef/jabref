package org.jabref.gui.ai.components.privacynotice;

import javafx.scene.Node;

import org.jabref.gui.DialogService;
import org.jabref.gui.FilePreferences;
import org.jabref.gui.ai.components.aichat.AiChatGuardedComponent;
import org.jabref.gui.util.DynamicallyChangeableNode;
import org.jabref.logic.ai.AiPreferences;

/**
 * A class that guards a component, before AI privacy policy is accepted.
 * Remember to call rebuildUi() method after initializing the guarded component. See {@link AiChatGuardedComponent} to look how it works.
 */
public abstract class AiPrivacyNoticeGuardedComponent extends DynamicallyChangeableNode {
    private final AiPreferences aiPreferences;
    private final FilePreferences filePreferences;
    private final DialogService dialogService;

    public AiPrivacyNoticeGuardedComponent(AiPreferences aiPreferences, FilePreferences filePreferences, DialogService dialogService) {
        this.aiPreferences = aiPreferences;
        this.filePreferences = filePreferences;
        this.dialogService = dialogService;

        aiPreferences.enableAiProperty().addListener(observable -> rebuildUi());
    }

    public final void rebuildUi() {
        if (aiPreferences.getEnableAi()) {
            setContent(showPrivacyPolicyGuardedContent());
        } else {
            setContent(
                    new PrivacyNoticeComponent(
                            aiPreferences,
                            this::rebuildUi,
                            filePreferences,
                            dialogService
                    )
            );
        }
    }

    protected abstract Node showPrivacyPolicyGuardedContent();
}
