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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.AiPreferences;

import com.tobiasdiez.easybind.EasyBind;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
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

    private final ObjectProperty<ChatLanguageModel> chatModelProperty = new SimpleObjectProperty<>(null); // <p>
    private final ObjectProperty<EmbeddingModel> embeddingModelProperty = new SimpleObjectProperty<>(new AllMiniLmL6V2EmbeddingModel());

    private static final String STORE_FILE_NAME = "embeddingsStore.mv";
    private static final String INGESTED_FILE_NAME = "ingested.ArrayList";

    private final MVStore mvStore;
    private final EmbeddingStore<TextSegment> embeddingStore;

    private ListProperty<String> ingestedFiles;
    private final List<String> filesUnderIngesting = new ArrayList<>();

    public AiService(AiPreferences aiPreferences) {
        try {
            Files.createDirectories(JabRefDesktop.getEmbeddingsCacheDirectory());
        } catch (IOException e) {
            LOGGER.error("An error occurred while creating directories for embedding store. Will use an in-memory store", e);
        }

        Path storePath = JabRefDesktop.getEmbeddingsCacheDirectory().resolve(STORE_FILE_NAME);
        LOGGER.error("STORE: {}", storePath);
        mvStore = MVStore.open(String.valueOf(storePath));
        embeddingStore = new MVStoreEmbeddingStore(mvStore);

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(String.valueOf(JabRefDesktop.getEmbeddingsCacheDirectory().resolve(INGESTED_FILE_NAME))))){
            ArrayList<String> ingestedFilesList = (ArrayList<String>) ois.readObject();
            ingestedFiles = new SimpleListProperty<>(FXCollections.observableList(ingestedFilesList));
        } catch (FileNotFoundException e) {
            LOGGER.info("No ingested files cache. Will create a new one");
            ingestedFiles = new SimpleListProperty<>();
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.error("An error occurred while reading paths of ingested files", e);
            ingestedFiles = new SimpleListProperty<>();
        }

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

    public void startIngestingFile(String path) {
        filesUnderIngesting.add(path);
    }

    public void endIngestingFile(String path) {
        assert filesUnderIngesting.contains(path);

        filesUnderIngesting.remove(path);
        ingestedFiles.add(path);
    }

    public boolean haveIngestedFile(String path) {
        return ingestedFiles.contains(path);
    }

    public boolean haveIngestedFiles(Collection<String> paths) {
        return new HashSet<>(ingestedFiles).containsAll(paths);
    }

    public boolean haveIngestedLinkedFiles(Collection<LinkedFile> linkedFiles) {
        return haveIngestedFiles(linkedFiles.stream().map(LinkedFile::getLink).toList());
    }

    public ReadOnlyListProperty<String> getIngestedFilesProperty() {
        return ingestedFiles;
    }

    public void removeIngestedFile(String path) {
        embeddingStore.removeAll(MetadataFilterBuilder.metadataKey("linkedFile").isEqualTo(path));
        ingestedFiles.remove(path);
    }

    public void close() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(String.valueOf(JabRefDesktop.getEmbeddingsCacheDirectory().resolve(INGESTED_FILE_NAME))))) {
            oos.writeObject(new ArrayList<>(ingestedFiles.stream().toList()));
        } catch (IOException e) {
            LOGGER.error("An error occurred while saving the paths of ingested files", e);
        }

        mvStore.close();
    }

    public void setOpenAiToken(String token) {
        ChatLanguageModel newChatModel = OpenAiChatModel
                .builder()
                .apiKey(token)
                .logRequests(true)
                .logResponses(true)
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
