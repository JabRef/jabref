package org.jabref.gui.entryeditor;

import java.util.List;

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
import org.jabref.logic.ai.AiChatData;
import org.jabref.logic.ai.AiConnection;
import org.jabref.logic.ai.AiIngestor;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.AiPreferences;
import org.jabref.preferences.FilePreferences;
import org.jabref.preferences.PreferencesService;

import com.tobiasdiez.easybind.EasyBind;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;

public class AiChatTab extends EntryEditorTab {
    public static final String NAME = "AI chat";

    private final DialogService dialogService;

    private final FilePreferences filePreferences;
    private final AiPreferences aiPreferences;

    private final BibDatabaseContext bibDatabaseContext;

    private AiConnection aiConnection = null;
    private AiChat aiChat = null;

    private VBox chatVBox = null;

    public AiChatTab(DialogService dialogService, PreferencesService preferencesService, BibDatabaseContext bibDatabaseContext) {
        this.dialogService = dialogService;

        this.filePreferences = preferencesService.getFilePreferences();
        this.aiPreferences = preferencesService.getAiPreferences();

        this.bibDatabaseContext = bibDatabaseContext;

        setText(Localization.lang(NAME));
        setTooltip(new Tooltip(Localization.lang("AI chat with full-text article")));

        setUpAiConnection();
    }

    // Set up the AI connection if AI is used.
    // Also listen for AI preferences changes and update the classes appropriately.
    private void setUpAiConnection() {
        if (aiPreferences.isUseAi()) {
            aiConnection = new AiConnection(aiPreferences.getOpenAiToken());
        }

        EasyBind.listen(aiPreferences.useAiProperty(), (obs, oldValue, newValue) -> {
            if (newValue) {
                aiConnection = new AiConnection(aiPreferences.getOpenAiToken());
                rebuildAiChat();
            } else {
                aiConnection = null;
                // QUESTION: If user chose AI but then unchooses, what should we do with the AI chat?
                aiChat = null;
            }
        });

        EasyBind.listen(aiPreferences.openAiTokenProperty(), (obs, oldValue, newValue) -> {
            if (aiConnection != null) {
                aiConnection = new AiConnection(newValue);
                rebuildAiChat();
            }
        });
    }

    private void rebuildAiChat() {
        if (aiChat != null) {
            AiChatData data = aiChat.getData();
            aiChat = new AiChat(data, aiConnection);
        }
    }

    @Override
    public boolean shouldShow(BibEntry entry) {
        return aiPreferences.isUseAi();
    }

    @Override
    protected void bindToEntry(BibEntry entry) {
        Node node;

        if (entry.getFiles().isEmpty()) {
            node = stateNoFiles();
        } else if (!entry.getFiles().stream().allMatch(file -> "PDF".equals(file.getFileType()))) {
            /*
                QUESTION: What is the type of file.getFileType()????
                I thought it is the part after the dot, but it turns out not.
                I got the "PDF" string by looking at tests.
             */
            node = stateWrongFilesFormat();
        } else {
            configureAiChat(entry);
            node = stateAiChat();
            restoreMessages(aiChat.getData().getChatMemoryStore().getMessages(aiChat.getChatId()));
        }

        setContent(node);
    }

    private Node stateNoFiles() {
        return new Label(Localization.lang("No files attached"));
    }

    private Node stateWrongFilesFormat() {
        return new Label(Localization.lang("Only PDF files are supported"));
    }

    private Node stateAiChat() {
        // Don't bully me for this style.

        VBox aiChatBox = new VBox(10);
        aiChatBox.setPadding(new Insets(10));

            ScrollPane chatScrollPane = new ScrollPane();
            chatScrollPane.setStyle("-fx-border-color: black;");
            chatScrollPane.setPadding(new Insets(10, 10, 0, 10));
            VBox.setVgrow(chatScrollPane, Priority.ALWAYS);

                chatVBox = new VBox(10);

                    // Chat messages will be children of chatVBox.

                chatScrollPane.setContent(chatVBox);

            aiChatBox.getChildren().add(chatScrollPane);

            HBox userPromptHBox = new HBox(10);
            userPromptHBox.setAlignment(Pos.CENTER);

                TextField userPromptTextField = new TextField();
                HBox.setHgrow(userPromptTextField, Priority.ALWAYS);

                userPromptHBox.getChildren().add(userPromptTextField);

                Button userPromptSubmitButton = new Button(Localization.lang("Submit"));
                userPromptSubmitButton.setOnAction(e -> {
                    String userPrompt = userPromptTextField.getText();
                    userPromptTextField.setText("");

                    addMessage(true, userPrompt);

                    String aiMessage = aiChat.execute(userPrompt);

                    addMessage(false, aiMessage);
                });

                userPromptHBox.getChildren().add(userPromptSubmitButton);

            aiChatBox.getChildren().add(userPromptHBox);

        return aiChatBox;
    }

    private void addMessage(boolean isUser, String text) {
        Node messageNode = generateMessage(isUser, text);
        chatVBox.getChildren().add(messageNode);
    }

    private static final String USER_MESSAGE_COLOR = "#7ee3fb";
    private static final String AI_MESSAGE_COLOR = "#bac8cb";

    private static Node generateMessage(boolean isUser, String text) {
        Pane pane = new Pane();

        if (isUser) {
            pane.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        }

        VBox paneVBox = new VBox(10);

        paneVBox.setStyle("-fx-background-color: " + (isUser ? USER_MESSAGE_COLOR : AI_MESSAGE_COLOR) + ";");
        paneVBox.setPadding(new Insets(10));

        Label authorLabel = new Label(Localization.lang(isUser ? "User" : "AI"));
        authorLabel.setStyle("-fx-font-weight: bold");
        paneVBox.getChildren().add(authorLabel);

        Label messageLabel = new Label(text);
        paneVBox.getChildren().add(messageLabel);

        pane.getChildren().add(paneVBox);

        return pane;
    }

    private void configureAiChat(BibEntry entry) {
        aiChat = new AiChat(aiConnection);

        AiIngestor ingestor = new AiIngestor(aiChat.getData().getEmbeddingStore(), aiConnection.getEmbeddingModel());

        for (LinkedFile linkedFile : entry.getFiles()) {
            try {
                ingestor.ingestLinkedFile(linkedFile, bibDatabaseContext, filePreferences);
            } catch (Exception e) {
                dialogService.showErrorDialogAndWait(Localization.lang("Error while loading file"),
                        Localization.lang("An error occurred while loading a file into the AI") + ":\n"
                                + e.getMessage() + "\n"
                                + Localization.lang("This file will be skipped") + ".");
            }
        }
    }

    private void restoreMessages(List<ChatMessage> messages) {
        for (ChatMessage message : messages) {
            if (message instanceof UserMessage userMessage) {
                addMessage(true, userMessage.singleText());
            } else if (message instanceof AiMessage aiMessage) {
                addMessage(false, aiMessage.text());
            }
        }
    }
}
