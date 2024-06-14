package org.jabref.logic.ai;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabases;
import org.jabref.preferences.AiPreferences;

import com.tobiasdiez.easybind.EasyBind;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class maintains the connection to AI services.
 * This is a global state of AI, and {@link AiChat}'s use this class.
 * <p>
 * An outer class is responsible for synchronizing objects of this class with {@link org.jabref.preferences.AiPreferences} changes.
 */
public class AiService {
    private final Logger LOGGER = LoggerFactory.getLogger(AiService.class);

    private final ObjectProperty<ChatLanguageModel> chatModelProperty = new SimpleObjectProperty<>(null); // <p>
    private final ObjectProperty<EmbeddingModel> embeddingModelProperty = new SimpleObjectProperty<>(new AllMiniLmL6V2EmbeddingModel());

    private final EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

    private final Map<Path, BibDatabaseChats> bibDatabaseChatsMap = new HashMap<>();

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

    public @Nullable BibDatabaseChats openBibDatabaseChats(BibDatabaseContext bibDatabaseContext) {
        if (bibDatabaseContext.getDatabasePath().isPresent()) {
            Path path = bibDatabaseContext.getDatabasePath().get();

            if (bibDatabaseChatsMap.containsKey(path)) {
                return bibDatabaseChatsMap.get(path);
            }

            // TODO: Error handling??????
            BibDatabaseChats bibDatabaseChats = new BibDatabaseChats(path);

            bibDatabaseChatsMap.put(path, bibDatabaseChats);

            return bibDatabaseChats;
        } else {
            LOGGER.warn("Unable to open (or create) bib database chats file. No database path is present");
            return null;
        }
    }

    public void close() {
        bibDatabaseChatsMap.values().forEach(BibDatabaseChats::close);
        bibDatabaseChatsMap.clear();
    }

    private void setOpenAiToken(String token) {
        ChatLanguageModel newChatModel = OpenAiChatModel
                .builder()
                .apiKey(token)
                .build();

        setChatModel(newChatModel);
    }

    private void setChatModel(ChatLanguageModel chatModel) {
        this.chatModelProperty.set(chatModel);
    }

    public @Nullable ChatLanguageModel getChatModel() {
        return chatModelProperty.get();
    }

    public ObjectProperty<ChatLanguageModel> chatModelProperty() {
        return chatModelProperty;
    }

    public EmbeddingModel getEmbeddingModel() {
        return embeddingModelProperty.get();
    }

    public EmbeddingStore<TextSegment> getEmbeddingStore() {
        return embeddingStore;
    }

    public ObjectProperty<EmbeddingModel> embeddingModelProperty() {
        return embeddingModelProperty;
    }
}
