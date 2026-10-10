package org.jabref.gui.ai.chat;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.DialogService;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.embedding.AsyncEmbeddingModel;
import org.jabref.logic.ai.embedding.EmbeddingModelCache;
import org.jabref.logic.ai.ingestion.IngestionTaskAggregator;
import org.jabref.logic.ai.ingestion.repositories.IngestedDocumentsRepository;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.ai.identifiers.FullBibEntry;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class AiChatViewModelTest {
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
}
