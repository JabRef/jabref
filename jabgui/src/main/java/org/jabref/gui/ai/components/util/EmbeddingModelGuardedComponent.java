package org.jabref.gui.ai.components.util;

import javafx.scene.Node;

import org.jabref.gui.DialogService;
import org.jabref.gui.ai.components.privacynotice.AiPrivacyNoticeGuardedComponent;
import org.jabref.gui.ai.components.util.errorstate.ErrorStateComponent;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.ai.AiPreferences;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.ingestion.model.JabRefEmbeddingModel;
import org.jabref.logic.l10n.Localization;

import com.google.common.eventbus.Subscribe;

/**
 * Class that has similar logic to {@link AiPrivacyNoticeGuardedComponent}. It extends from it, so that means,
 * if a component needs embedding model, then it should also be guarded with accepting AI privacy policy.
 */
public abstract class EmbeddingModelGuardedComponent extends AiPrivacyNoticeGuardedComponent {
    private final AiService aiService;

    public EmbeddingModelGuardedComponent(AiService aiService,
                                          AiPreferences aiPreferences,
                                          ExternalApplicationsPreferences externalApplicationsPreferences,
                                          DialogService dialogService
    ) {
        super(aiPreferences, externalApplicationsPreferences, dialogService);

        this.aiService = aiService;

        aiService.getEmbeddingModel().registerListener(this);
    }

    protected abstract Node showEmbeddingModelGuardedContent();

    @Override
    protected final Node showPrivacyPolicyGuardedContent() {
        if (!aiService.getEmbeddingModel().isPresent()) {
            if (aiService.getEmbeddingModel().hadErrorWhileBuildingModel()) {
                return showErrorWhileBuildingEmbeddingModel();
            } else {
                return showBuildingEmbeddingModel();
            }
        } else {
            return showEmbeddingModelGuardedContent();
        }
    }

    private Node showErrorWhileBuildingEmbeddingModel() {
        return ErrorStateComponent.withTextAreaAndButton(
                Localization.lang("Unable to chat"),
                Localization.lang("An error occurred while building the embedding model"),
                aiService.getEmbeddingModel().getErrorWhileBuildingModel(),
                Localization.lang("Rebuild"),
                () -> aiService.getEmbeddingModel().startRebuildingTask()
        );
    }

    public Node showBuildingEmbeddingModel() {
        return ErrorStateComponent.withSpinner(
                Localization.lang("Downloading..."),
                Localization.lang("Downloading embedding model... Afterward, you will be able to chat with your files.")
        );
    }

    @Subscribe
    public void listen(JabRefEmbeddingModel.EmbeddingModelBuiltEvent event) {
        UiTaskExecutor.runInJavaFXThread(EmbeddingModelGuardedComponent.this::rebuildUi);
    }

    @Subscribe
    public void listen(JabRefEmbeddingModel.EmbeddingModelBuildingErrorEvent event) {
        UiTaskExecutor.runInJavaFXThread(EmbeddingModelGuardedComponent.this::rebuildUi);
    }
}
