package org.jabref.logic.ai;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import com.google.common.eventbus.EventBus;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import javafx.beans.property.ListProperty;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.gui.search.SearchTextField;
import org.jabref.logic.ai.events.FileIngestedEvent;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.preferences.AiPreferences;

import com.tobiasdiez.easybind.EasyBind;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
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
    public static final String VERSION = "1";

    private final Logger LOGGER = LoggerFactory.getLogger(AiService.class);

    private final AiPreferences aiPreferences;

    private final ObjectProperty<ChatLanguageModel> chatModelProperty = new SimpleObjectProperty<>(null); // <p>
    private final ObjectProperty<EmbeddingModel> embeddingModelProperty = new SimpleObjectProperty<>(new AllMiniLmL6V2EmbeddingModel());

    private static final String STORE_FILE_NAME = "embeddingsStore.mv";
    private static final String INGESTED_FILE_NAME = "ingestedFiles.mv";

    private final MVStore embeddingsMvStore;
    private final EmbeddingStore<TextSegment> embeddingStore;

    private final MVStore ingestedMvStore;
    private final MVMap<String, Long> ingestedMap;

    private final Map<Path, BibDatabaseChats> bibDatabaseChatsMap = new HashMap<>();

    private final EventBus eventBus = new EventBus();

    public AiService(AiPreferences aiPreferences) {
        this.aiPreferences = aiPreferences;

        Path storePath = JabRefDesktop.getEmbeddingsCacheDirectory().resolve(STORE_FILE_NAME);
        Path ingestedPath = JabRefDesktop.getEmbeddingsCacheDirectory().resolve(INGESTED_FILE_NAME);

        try {
            Files.createDirectories(JabRefDesktop.getEmbeddingsCacheDirectory());
        } catch (IOException e) {
            LOGGER.error("An error occurred while creating directories for embedding store. Will use an in-memory store", e);
            storePath = null;
            ingestedPath = null;
        }

        this.embeddingsMvStore = MVStore.open(storePath == null ? null : storePath.toString());
        this.embeddingStore = new MVStoreEmbeddingStore(embeddingsMvStore);

        this.ingestedMvStore = MVStore.open(ingestedPath == null ? null : ingestedPath.toString());
        this.ingestedMap = ingestedMvStore.openMap("ingested");

        if (aiPreferences.getEnableChatWithFiles()) {
            rebuildChatModel();
        }

        bindToPreferences();
    }

    private void bindToPreferences() {
        EasyBind.listen(aiPreferences.enableChatWithFilesProperty(), (property, oldValue, newValue) -> {
            if (newValue) {
                if (!aiPreferences.getOpenAiToken().isEmpty()) {
                    rebuildChatModel();
                }
            } else {
                setChatModel(null);
            }
        });

        EasyBind.listen(aiPreferences.openAiTokenProperty(), (property, oldValue, newValue) -> {
            if (!newValue.isEmpty()) {
                rebuildChatModel();
            } else {
                setChatModel(null);
            }
        });

        EasyBind.listen(aiPreferences.modelProperty(), (property, oldValue, newValue) -> {
            rebuildChatModel();
        });

        EasyBind.listen(aiPreferences.temperatureProperty(), (property, oldValue, newValue) -> {
            rebuildChatModel();
        });
    }

    public void endIngestingFile(String link, long modificationTimeInSeconds) {
        ingestedMap.put(link, modificationTimeInSeconds);
        eventBus.post(new FileIngestedEvent(link));
    }

    public boolean haveIngestedFile(String link) {
        return ingestedMap.get(link) != null;
    }

    public long getIngestedFileModificationTime(String link) {
        return ingestedMap.get(link);
    }

    public boolean haveIngestedFiles(Stream<String> links) {
        return links.allMatch(this::haveIngestedFile);
    }

    public boolean haveIngestedLinkedFiles(Collection<LinkedFile> linkedFiles) {
        return haveIngestedFiles(linkedFiles.stream().map(LinkedFile::getLink));
    }

    public void removeIngestedFile(String link) {
        embeddingStore.removeAll(MetadataFilterBuilder.metadataKey("linkedFile").isEqualTo(link));
        ingestedMap.remove(link);
    }

    public Set<String> getListOfIngestedFilesLinks() {
        return new HashSet<>(ingestedMap.keySet());
    }

    public void registerListener(Object listener) {
        eventBus.register(listener);
    }

    public void close() {
        embeddingsMvStore.close();
        ingestedMvStore.close();

        bibDatabaseChatsMap.values().forEach(BibDatabaseChats::close);
        bibDatabaseChatsMap.clear();
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

    private void rebuildChatModel() {
        ChatLanguageModel chatLanguageModel =
                OpenAiChatModel
                .builder()
                .apiKey(aiPreferences.getOpenAiToken())
                .modelName(aiPreferences.getModel().getName())
                .temperature(aiPreferences.getTemperature())
                .logRequests(true)
                .logResponses(true)
                .build();

        setChatModel(chatLanguageModel);
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
