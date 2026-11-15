package org.jabref.gui.ai.chat;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.DialogService;
import org.jabref.gui.groups.GroupNodeViewModel;
import org.jabref.gui.testutils.JavaFxTest;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.chatting.InMemoryChatHistoryCache;
import org.jabref.logic.ai.embedding.AsyncEmbeddingModel;
import org.jabref.logic.ai.embedding.EmbeddingModelCache;
import org.jabref.logic.ai.ingestion.IngestionTaskAggregator;
import org.jabref.logic.ai.ingestion.repositories.IngestedDocumentsRepository;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.ai.identifiers.FullBibEntry;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.groups.AllEntriesGroup;
import org.jabref.model.groups.GroupTreeNode;

import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class AiChatViewModelTest extends JavaFxTest {
    @Test
    void addingFileToSelectedEntryUpdatesChatState() {
        AiPreferences aiPreferences = spy(AiPreferences.getDefault());
        doReturn(new SimpleBooleanProperty(true)).when(aiPreferences).aiFeaturesEnabledProperty();
        doReturn(new SimpleBooleanProperty(true)).when(aiPreferences).aiFeaturesEnabledCurrentlyProperty();

        EmbeddingModelCache embeddingModelCache = mock(EmbeddingModelCache.class);
        when(embeddingModelCache.getOrCreate(aiPreferences.getEmbeddingModel())).thenReturn(mock(AsyncEmbeddingModel.class));

        AiChatViewModel viewModel = new AiChatViewModel(
                aiPreferences,
                mock(FilePreferences.class),
                mock(DialogService.class),
                mock(IngestionTaskAggregator.class),
                mock(IngestedDocumentsRepository.class),
                mock(EmbeddingStore.class),
                embeddingModelCache,
                mock(TaskExecutor.class)
        );
        BibEntry entry = new BibEntry();
        var observableEntries = FXCollections.<FullBibEntry>observableArrayList(
                identifier -> identifier.entry().getObservables()
        );
        observableEntries.add(new FullBibEntry(new BibDatabaseContext(), entry));
        viewModel.entriesProperty().set(observableEntries);

        assertEquals(AiChatViewModel.State.NO_FILES, viewModel.stateProperty().get());

        entry.addFile(new LinkedFile("Paper", "paper.pdf", "pdf"));

        assertEquals(AiChatViewModel.State.IDLE, viewModel.stateProperty().get());
    }

    @Test
    void previousEntryLoadCannotReplaceTheSelectedChat() {
        AiPreferences preferences = spy(AiPreferences.getDefault());
        doReturn(new SimpleBooleanProperty(true)).when(preferences).aiFeaturesEnabledCurrentlyProperty();
        InMemoryChatHistoryCache cache = mock(InMemoryChatHistoryCache.class);
        BibDatabaseContext context = new BibDatabaseContext();
        BibEntry first = new BibEntry().withCitationKey("First");
        BibEntry second = new BibEntry().withCitationKey("Second");
        CompletableFuture<ObservableList<ChatMessage>> firstLoad = new CompletableFuture<>();
        CompletableFuture<ObservableList<ChatMessage>> secondLoad = new CompletableFuture<>();
        when(cache.getForEntryAsync(context, first)).thenReturn(firstLoad);
        when(cache.getForEntryAsync(context, second)).thenReturn(secondLoad);
        AiEntryChatViewModel viewModel = new AiEntryChatViewModel(preferences, cache);
        ObservableList<ChatMessage> secondHistory = FXCollections.observableArrayList(ChatMessage.userMessage("second"));

        interact(() -> {
            viewModel.selectedEntryProperty().set(new FullBibEntry(context, first));
            ObservableList<FullBibEntry> firstTaskEntries = viewModel.entriesProperty().get();
            viewModel.selectedEntryProperty().set(new FullBibEntry(context, second));
            assertEquals(List.of(new FullBibEntry(context, first)), firstTaskEntries);
            assertFalse(viewModel.historyLoadedProperty().get());
            secondLoad.complete(secondHistory);
            firstLoad.complete(FXCollections.observableArrayList(ChatMessage.userMessage("first")));

            assertSame(secondHistory, viewModel.chatHistoryProperty().get());
            assertTrue(viewModel.historyLoadedProperty().get());
            viewModel.dispose();
        });
    }

    @Test
    void disposingEntryChatIgnoresItsPendingLoad() {
        AiPreferences preferences = spy(AiPreferences.getDefault());
        doReturn(new SimpleBooleanProperty(true)).when(preferences).aiFeaturesEnabledCurrentlyProperty();
        InMemoryChatHistoryCache cache = mock(InMemoryChatHistoryCache.class);
        BibDatabaseContext context = new BibDatabaseContext();
        BibEntry entry = new BibEntry().withCitationKey("Disposed");
        CompletableFuture<ObservableList<ChatMessage>> loading = new CompletableFuture<>();
        when(cache.getForEntryAsync(context, entry)).thenReturn(loading);
        AiEntryChatViewModel viewModel = new AiEntryChatViewModel(preferences, cache);

        interact(() -> {
            viewModel.selectedEntryProperty().set(new FullBibEntry(context, entry));
            ObservableList<FullBibEntry> taskEntries = viewModel.entriesProperty().get();
            viewModel.dispose();
            loading.complete(FXCollections.observableArrayList(ChatMessage.userMessage("stale")));

            assertEquals(List.of(), viewModel.chatHistoryProperty().get());
            assertFalse(viewModel.historyLoadedProperty().get());
            assertEquals(List.of(new FullBibEntry(context, entry)), taskEntries);
        });
    }

    @Test
    void disablingAiIgnoresThePendingEntryChatLoad() {
        AiPreferences preferences = spy(AiPreferences.getDefault());
        SimpleBooleanProperty enabled = new SimpleBooleanProperty(true);
        doReturn(enabled).when(preferences).aiFeaturesEnabledCurrentlyProperty();
        InMemoryChatHistoryCache cache = mock(InMemoryChatHistoryCache.class);
        BibDatabaseContext context = new BibDatabaseContext();
        BibEntry entry = new BibEntry().withCitationKey("Disabled");
        CompletableFuture<ObservableList<ChatMessage>> loading = new CompletableFuture<>();
        when(cache.getForEntryAsync(context, entry)).thenReturn(loading);
        AiEntryChatViewModel viewModel = new AiEntryChatViewModel(preferences, cache);

        interact(() -> {
            viewModel.selectedEntryProperty().set(new FullBibEntry(context, entry));
            enabled.set(false);
            loading.complete(FXCollections.observableArrayList(ChatMessage.userMessage("stale")));

            assertEquals(List.of(), viewModel.chatHistoryProperty().get());
            assertFalse(viewModel.historyLoadedProperty().get());
            viewModel.dispose();
        });
    }

    @Test
    void previousLibraryLoadCannotReplaceTheSelectedGroupChat() {
        AiPreferences preferences = spy(AiPreferences.getDefault());
        doReturn(new SimpleBooleanProperty(true)).when(preferences).aiFeaturesEnabledCurrentlyProperty();
        InMemoryChatHistoryCache cache = mock(InMemoryChatHistoryCache.class);
        AiService service = mock(AiService.class);
        when(service.getChatHistoryCache()).thenReturn(cache);
        GroupTreeNode group = GroupTreeNode.fromGroup(new AllEntriesGroup("Shared name"));
        GroupNodeViewModel groupViewModel = mock(GroupNodeViewModel.class);
        when(groupViewModel.getGroupNode()).thenReturn(group);
        BibDatabaseContext firstContext = new BibDatabaseContext();
        firstContext.getMetaData().setAiLibraryId("first-library");
        BibEntry firstEntry = new BibEntry().withCitationKey("First");
        firstContext.getDatabase().insertEntry(firstEntry);
        BibDatabaseContext secondContext = new BibDatabaseContext();
        secondContext.getMetaData().setAiLibraryId("second-library");
        CompletableFuture<ObservableList<ChatMessage>> firstLoad = new CompletableFuture<>();
        CompletableFuture<ObservableList<ChatMessage>> secondLoad = new CompletableFuture<>();
        when(cache.getForGroupAsync(firstContext, group)).thenReturn(firstLoad);
        when(cache.getForGroupAsync(secondContext, group)).thenReturn(secondLoad);
        AiGroupChatViewModel viewModel = new AiGroupChatViewModel(preferences, service);
        ObservableList<ChatMessage> secondHistory = FXCollections.observableArrayList(ChatMessage.userMessage("second library"));

        interact(() -> {
            viewModel.databaseContextProperty().set(firstContext);
            viewModel.groupNodeProperty().set(groupViewModel);
            ObservableList<FullBibEntry> firstTaskEntries = viewModel.entriesProperty().get();
            viewModel.databaseContextProperty().set(secondContext);
            secondLoad.complete(secondHistory);
            firstLoad.complete(FXCollections.observableArrayList(ChatMessage.userMessage("first library")));

            assertSame(secondHistory, viewModel.chatHistoryProperty().get());
            assertTrue(viewModel.historyLoadedProperty().get());
            assertEquals(List.of(new FullBibEntry(firstContext, firstEntry)), firstTaskEntries);
        });
    }
}
