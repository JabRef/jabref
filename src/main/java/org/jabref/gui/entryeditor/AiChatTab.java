package org.jabref.gui.entryeditor;

import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Optional;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.xmp.XmpUtilReader;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.AiPreferences;
import org.jabref.preferences.FilePreferences;
import org.jabref.preferences.PreferencesService;

import com.tobiasdiez.easybind.EasyBind;
import dev.langchain4j.chain.ConversationalRetrievalChain;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AiChatTab extends EntryEditorTab {
    public static final String NAME = "AI chat";
    private static final Logger LOGGER = LoggerFactory.getLogger(AiChatTab.class.getName());
    private final PreferencesService preferencesService;
    private final BibDatabaseContext bibDatabaseContext;
    private final FilePreferences filePreferences;
    private final AiPreferences aiPreferences;
    private ChatLanguageModel chatModel = null;
    private final EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();
    // Stores embeddings generated from full-text articles.
    // Depends on the embedding model.
    private EmbeddingStore<TextSegment> embeddingStore = null;
    // An object that augments the user prompt with relevant information from full-text articles.
    // Depends on the embedding model and the embedding store.
    private ContentRetriever contentRetriever = null;
    // The actual chat memory.
    private ChatMemoryStore chatMemoryStore = null;
    // An algorithm for manipulating chat memory.
    // Depends on chat memory store.
    private ChatMemory chatMemory = null;
    // Holds and performs the conversation with user. Stores the message history and manages API calls.
    // Depends on the chat language model and content retriever.
    private ConversationalRetrievalChain chain = null;

    /*
        Classes from langchain:
        - Global (depends on preferences changes):
            - ChatModel.
            - EmbeddingsModel
        - Per entry (depends on BibEntry):
            - EmbeddingsStore - stores embeddings of full-text article.
            - ContentRetriever - a thing that augments the user prompt with relevant information.
            - ChatMemoryStore - really stores chat history.
            - ChatMemory - algorithm for manipulating chat memory.
            - ConversationalRetrievalChain - main wrapper between the user and AI. Chat history, API calls.

        We can store only embeddings and chat memory in bib entries, and then reconstruct this classes.
     */

    public AiChatTab(PreferencesService preferencesService, BibDatabaseContext bibDatabaseContext) {
        this.preferencesService = preferencesService;
        this.bibDatabaseContext = bibDatabaseContext;
        this.filePreferences = preferencesService.getFilePreferences();
        this.aiPreferences = preferencesService.getAiPreferences();

        setText(Localization.lang(NAME));
        setTooltip(new Tooltip(Localization.lang("AI chat with full-text article")));

        EasyBind.listen(aiPreferences.useAiProperty(), (obs, oldValue, newValue) -> {
            if (newValue) {
                makeChatModel(aiPreferences.getOpenAiToken());
            }
        });
        EasyBind.listen(aiPreferences.openAiTokenProperty(), (obs, oldValue, newValue) -> makeChatModel(newValue));

        if (aiPreferences.isUseAi()) {
            makeChatModel(aiPreferences.getOpenAiToken());
        }
    }

    private void makeChatModel(String apiKey) {
        chatModel = OpenAiChatModel
                .builder()
                .apiKey(apiKey)
                .build();
    }

    @Override
    public boolean shouldShow(BibEntry entry) {
        return aiPreferences.isUseAi();
    }

    @Override
    protected void bindToEntry(BibEntry entry) {
        if (entry.getFiles().isEmpty()) {
            setContent(new Label(Localization.lang("No files attached")));
        } else if (!entry.getFiles().stream().allMatch(file -> file.getFileType().equals("PDF"))) {
            /*
                QUESTION: What is the type of file.getFileType()????
                I thought it is the part after the dot, but it turns out not.
                I got the "PDF" string by looking at tests.
             */
            setContent(new Label(Localization.lang("Only PDF files are supported")));
        } else {
            bindToEntryRaw(entry);
        }
    }

    private void bindToEntryRaw(BibEntry entry) {
        configureAI(entry);
        makeContent();
    }

    private void makeContent() {
        Label askLabel = new Label(Localization.lang("Ask AI") + ": ");

        TextField promptField = new TextField();

        Button submitButton = new Button(Localization.lang("Submit"));

        HBox promptBox = new HBox(askLabel, promptField, submitButton);

        Label answerLabel = new Label(Localization.lang("Answer") + ": ");

        Label realAnswerLabel = new Label();

        HBox answerBox = new HBox(answerLabel, realAnswerLabel);

        VBox vbox = new VBox(promptBox, answerBox);

        submitButton.setOnAction(e -> {
            // TODO: Check if the prompt is empty.
            realAnswerLabel.setText(chain.execute(promptField.getText()));
        });

        setContent(vbox);
    }

    private void configureAI(BibEntry entry) {
        makeAiObjects();
        ingestFiles(entry);
    }

    private void makeAiObjects() {
        this.embeddingStore = new InMemoryEmbeddingStore<>();

        this.contentRetriever = EmbeddingStoreContentRetriever
                .builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .build();

        this.chatMemoryStore = new InMemoryChatMemoryStore();

        this.chatMemory = MessageWindowChatMemory
                .builder()
                .chatMemoryStore(chatMemoryStore)
                .maxMessages(10) // This was the default value in the original implementation.
                .build();

        this.chain = ConversationalRetrievalChain
                .builder()
                .chatLanguageModel(chatModel)
                .contentRetriever(contentRetriever)
                .chatMemory(chatMemory)
                .build();
    }

    // TODO: Proper error handling.

    private void ingestFiles(BibEntry entry) {
        DocumentSplitter documentSplitter = DocumentSplitters.recursive(300, 0);
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                                                                .embeddingStore(embeddingStore)
                                                                .embeddingModel(embeddingModel) // What if null?
                                                                .documentSplitter(documentSplitter)
                                                                .build();

        for (LinkedFile linkedFile : entry.getFiles()) {
            Optional<Path> path = linkedFile.findIn(bibDatabaseContext, filePreferences);
            if (path.isEmpty()) {
                LOGGER.warn("Could not find file {}", linkedFile.getLink());
                continue;
            }
            String fileContents = readPDFFile(path.get());
            Document document = new Document(fileContents);
            ingestor.ingest(document);
        }
    }

    private String readPDFFile(Path path) {
        try (PDDocument document = new XmpUtilReader().loadWithAutomaticDecryption(path)) {
            PDFTextStripper stripper = new PDFTextStripper();

            int lastPage = document.getNumberOfPages();
            stripper.setStartPage(1);
            stripper.setEndPage(lastPage);
            StringWriter writer = new StringWriter();
            stripper.writeText(document, writer);

            String result = writer.toString();
            LOGGER.trace("PDF content: {}", result);

            return result;
        } catch (
                Exception e) {
            LOGGER.error(e.getMessage(), e);
            return "";
        }
    }
}
