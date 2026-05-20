package org.jabref.gui.ai.chat;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.fxml.FXML;

import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.ai.chatting.ChatModel;
import org.jabref.logic.ai.ingestion.tasks.generateembeddings.GenerateEmbeddingsTask;
import org.jabref.logic.ai.rag.logic.AnswerEngine;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.ai.identifiers.FullBibEntry;

import com.airhacks.afterburner.views.ViewLoader;

public class AiChatStatusWindow extends BaseDialog<Void> {
    @FXML private AiChatStatusView aiChatStatusView;

    public AiChatStatusWindow() {
        super();

        this.setTitle(Localization.lang("AI Chat Status"));
        this.getDialogPane().getScene().getWindow().setOnCloseRequest(_ -> this.hide());

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);
    }

    public void setAnswerEngine(AnswerEngine answerEngine) {
        aiChatStatusView.setAnswerEngine(answerEngine);
    }

    public ObjectProperty<ChatModel> chatModelProperty() {
        return aiChatStatusView.chatModelProperty();
    }

    public ListProperty<ChatMessage> chatHistoryProperty() {
        return aiChatStatusView.chatHistoryProperty();
    }

    public ObjectProperty<AnswerEngine> answerEngineProperty() {
        return aiChatStatusView.answerEngineProperty();
    }

    public ListProperty<FullBibEntry> entriesProperty() {
        return aiChatStatusView.entriesProperty();
    }

    public ListProperty<GenerateEmbeddingsTask> generateEmbeddingsTasksProperty() {
        return aiChatStatusView.generateEmbeddingsTasksProperty();
    }
}
