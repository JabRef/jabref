package org.jabref.gui.entryeditor;

import java.nio.file.Path;

import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import org.jabref.gui.DialogService;
import org.jabref.logic.ai.AiChat;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.AiIngestor;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.AiPreferences;
import org.jabref.preferences.FilePreferences;
import org.jabref.preferences.PreferencesService;

import com.tobiasdiez.easybind.EasyBind;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

public class AiChatTab extends EntryEditorTab {
    public static final String NAME = "AI chat";

    private static final String QA_SYSTEM_MESSAGE = """
            You are an AI research assistant. You read and analyze scientific articles.
            The user will send you a question regarding a paper. You will be supplied also with the relevant information found in the article.
            Answer the question only by using the relevant information. Don't make up the answer.
            If you can't answer the user question using the provided information, then reply that you couldn't do it.""";

    private final DialogService dialogService;
    private final FilePreferences filePreferences;
    private final AiPreferences aiPreferences;
    private final EntryEditorPreferences entryEditorPreferences;
    private final BibDatabaseContext bibDatabaseContext;

    private VBox chatVBox = null;

    private AiService aiService = null;
    private AiChat aiChat = null;

    // TODO: This field should somehow live in bib entry.
    private EmbeddingStore<TextSegment> currentEmbeddingStore = null;

    public AiChatTab(DialogService dialogService, PreferencesService preferencesService,
                     BibDatabaseContext bibDatabaseContext) {
        this.dialogService = dialogService;

        this.filePreferences = preferencesService.getFilePreferences();
        this.aiPreferences = preferencesService.getAiPreferences();
        this.entryEditorPreferences = preferencesService.getEntryEditorPreferences();

        this.bibDatabaseContext = bibDatabaseContext;

        setText(Localization.lang(NAME));
        setTooltip(new Tooltip(Localization.lang("AI chat with full-text article")));

        setUpAiConnection();
    }

    private void setUpAiConnection() {
        if (aiPreferences.getEnableChatWithFiles()) {
            aiService = new AiService(aiPreferences.getOpenAiToken());
        }

        EasyBind.listen(aiPreferences.enableChatWithFilesProperty(), (obs, oldValue, newValue) -> {
            if (newValue && !aiPreferences.getOpenAiToken().isEmpty()) {
                aiService = new AiService(aiPreferences.getOpenAiToken());
                rebuildAiChat();
            } else {
                aiService = null;
                aiChat = null;
            }
        });

        EasyBind.listen(aiPreferences.openAiTokenProperty(), (obs, oldValue, newValue) -> {
            if (!newValue.isEmpty()) {
                aiService = new AiService(newValue);
                rebuildAiChat();
            }
        });
    }

    private void rebuildAiChat() {
        if (aiChat != null && currentEmbeddingStore != null) {
            aiChat = new AiChat(aiService, currentEmbeddingStore);
            aiChat.setSystemMessage(QA_SYSTEM_MESSAGE);
        }
    }

    @Override
    public boolean shouldShow(BibEntry entry) {
        return entryEditorPreferences.shouldShowAiChatTab();
    }

    @Override
    protected void bindToEntry(BibEntry entry) {
        if (!aiPreferences.getEnableChatWithFiles()) {
            setContent(new Label(Localization.lang("JabRef uses OpenAI to enable \"chatting\" with PDF files. OpenAI is an external service. To enable JabRef chatgting with PDF files, the content of the PDF files need to be shared with OpenAI. As soon as you ask a question, the text content of all PDFs attached to the entry are send to OpenAI. The privacy policy of OpenAI applies. You find it at <https://openai.com/policies/privacy-policy/>.")));
        } else if (entry.getFiles().isEmpty()) {
            setContent(new Label(Localization.lang("No files attached")));
        } else if (!entry.getFiles().stream().map(LinkedFile::getLink).map(Path::of).allMatch(FileUtil::isPDFFile)) {
            setContent(new Label(Localization.lang("Only PDF files are supported")));
        } else {
            bindToCorrectEntry(entry);
        }
    }

    private void bindToCorrectEntry(BibEntry entry) {
        configureAiChat(entry);
        setContent(createAiChatUI());
    }

    private void configureAiChat(BibEntry entry) {
        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        aiChat = new AiChat(aiService, embeddingStore);
        aiChat.setSystemMessage(QA_SYSTEM_MESSAGE);

        AiIngestor ingestor = new AiIngestor(embeddingStore, aiService.getEmbeddingModel());

        for (LinkedFile linkedFile : entry.getFiles()) {
            try {
                ingestor.ingestLinkedFile(linkedFile, bibDatabaseContext, filePreferences);
            } catch (Exception e) {
                dialogService.notify(Localization.lang("An error occurred while loading a file into the AI")
                        + ":\n"
                        + e.getMessage() + "\n"
                        + Localization.lang("This file will be skipped") + ".");
            }
        }

        currentEmbeddingStore = embeddingStore;
    }

    private Node createAiChatUI() {
        VBox aiChatBox = new VBox(10);
        aiChatBox.setPadding(new Insets(10));

        aiChatBox.getChildren().add(constructChatScrollPane());
        aiChatBox.getChildren().add(constructUserPromptBox());

        return aiChatBox;
    }

    private Node constructChatScrollPane() {
        ScrollPane chatScrollPane = new ScrollPane();
        chatScrollPane.setFitToWidth(true);
        chatScrollPane.setStyle("-fx-border-color: black;");
        VBox.setVgrow(chatScrollPane, Priority.ALWAYS);

        chatVBox = new VBox(10);
        chatVBox.setPadding(new Insets(10, 10, 0, 10));
        aiService.getChatMemoryStore().getMessages(aiChat.getChatId()).forEach(this::addMessage);
        chatScrollPane.setContent(chatVBox);

        return chatScrollPane;
    }

    private Node constructUserPromptBox() {
        HBox userPromptHBox = new HBox(10);
        userPromptHBox.setAlignment(Pos.CENTER);

        TextField userPromptTextField = new TextField();
        HBox.setHgrow(userPromptTextField, Priority.ALWAYS);

        userPromptHBox.getChildren().add(userPromptTextField);

        Button userPromptSubmitButton = new Button(Localization.lang("Submit"));
        userPromptSubmitButton.setOnAction(e -> {
            String userPrompt = userPromptTextField.getText();
            userPromptTextField.setText("");

            addMessage(new UserMessage(userPrompt));

            String aiMessage = aiChat.execute(userPrompt);

            addMessage(new AiMessage(aiMessage));
        });

        userPromptHBox.getChildren().add(userPromptSubmitButton);

        return userPromptHBox;
    }

    private void addMessage(ChatMessage chatMessage) {
        if (chatMessage.type() == ChatMessageType.AI || chatMessage.type() == ChatMessageType.USER) {
            Node messageNode = constructMessageNode(chatMessage);
            chatVBox.getChildren().add(messageNode);
        }
    }

    private static Node constructMessageNode(ChatMessage chatMessage) {
        boolean isUser = chatMessage.type() == ChatMessageType.USER;

        Pane pane = new Pane();

        if (isUser) {
            pane.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        }

        VBox paneVBox = new VBox(10);
        paneVBox.setMaxWidth(500);

        paneVBox.setStyle("-fx-background-color: " + (isUser ? "-jr-ai-message-user" : "-jr-ai-message-ai") + ";");
        paneVBox.setPadding(new Insets(10));

        Label authorLabel = new Label(Localization.lang(isUser ? "User" : "AI"));
        authorLabel.setStyle("-fx-font-weight: bold");
        paneVBox.getChildren().add(authorLabel);

        Label messageLabel = new Label(chatMessage.text());
        paneVBox.getChildren().add(messageLabel);

        pane.getChildren().add(paneVBox);

        return pane;
    }

}
