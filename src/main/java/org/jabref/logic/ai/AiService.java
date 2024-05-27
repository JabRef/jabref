package org.jabref.logic.ai;

import java.util.ArrayList;
import java.util.List;

import org.jabref.logic.ai.events.ChatModelChangedEvent;
import org.jabref.preferences.AiPreferences;

import com.google.common.eventbus.EventBus;
import com.tobiasdiez.easybind.EasyBind;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.jspecify.annotations.Nullable;

/**
 * This class maintains the connection to AI services.
 * This is a global state of AI, and {@link AiChat}'s use this class.
 * <p>
 * An outer class is responsible for synchronizing objects of this class with {@link org.jabref.preferences.AiPreferences} changes.
 */
public class AiService {
    private @Nullable ChatLanguageModel chatModel = null;
    private final EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();

    private final EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

    private final EventBus eventBus = new EventBus();

    public AiService(AiPreferences aiPreferences) {
        if (aiPreferences.getEnableChatWithFiles() && !aiPreferences.getOpenAiToken().isEmpty()) {
            setOpenAiToken(aiPreferences.getOpenAiToken());
        }

        EasyBind.listen(aiPreferences.enableChatWithFilesProperty(), (property, oldValue, newValue) -> {
            if (newValue) {
                if (!aiPreferences.getOpenAiToken().isEmpty()) {
                    setOpenAiToken(aiPreferences.getOpenAiToken());
                }
            } else {
                setChatModel(null);
            }
        });

        EasyBind.listen(aiPreferences.openAiTokenProperty(), (property, oldValue, newValue) -> {
            if (!newValue.isEmpty()) {
                setOpenAiToken(newValue);
            } else {
                setChatModel(null);
            }
        });
    }

    public void setOpenAiToken(String token) {
        ChatLanguageModel newChatModel = OpenAiChatModel
                .builder()
                .apiKey(token)
                .build();

        setChatModel(newChatModel);
    }

    public void setChatModel(ChatLanguageModel chatModel) {
        this.chatModel = chatModel;
        eventBus.post(new ChatModelChangedEvent(chatModel));
    }

    public void registerListener(Object listener) {
        eventBus.register(listener);
    }

    public @Nullable ChatLanguageModel getChatModel() {
        return chatModel;
    }

    public EmbeddingModel getEmbeddingModel() {
        return embeddingModel;
    }

    public EmbeddingStore<TextSegment> getEmbeddingStore() {
        return embeddingStore;
    }
}
