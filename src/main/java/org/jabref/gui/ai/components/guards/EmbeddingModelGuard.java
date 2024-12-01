package org.jabref.gui.ai.components.guards;

import javafx.scene.Node;

import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.gui.util.components.ErrorStateComponent;
import org.jabref.gui.util.guards.ComponentGuard;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.ingestion.model.JabRefEmbeddingModel;
import org.jabref.logic.l10n.Localization;

import com.google.common.eventbus.Subscribe;

// Note: it is assumed that ai privacy polict is checked.
public class EmbeddingModelGuard extends ComponentGuard {
    private final AiService aiService;

    public EmbeddingModelGuard(AiService aiService) {
        this.aiService = aiService;

        aiService.getEmbeddingModel().registerListener(this);

        checkEmbeddingModel();
    }

    private void checkEmbeddingModel() {
        set(aiService.getEmbeddingModel().isPresent());
    }

    @Subscribe
    public void listen(JabRefEmbeddingModel.EmbeddingModelBuiltEvent event) {
        UiTaskExecutor.runInJavaFXThread(EmbeddingModelGuard.this::checkEmbeddingModel);
    }

    @Subscribe
    public void listen(JabRefEmbeddingModel.EmbeddingModelBuildingErrorEvent event) {
        UiTaskExecutor.runInJavaFXThread(EmbeddingModelGuard.this::checkEmbeddingModel);
    }

    @Override
    public Node getExplanation() {
        if (aiService.getEmbeddingModel().hadErrorWhileBuildingModel()) {
            return showErrorWhileBuildingEmbeddingModel();
        } else {
            return showBuildingEmbeddingModel();
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
}
