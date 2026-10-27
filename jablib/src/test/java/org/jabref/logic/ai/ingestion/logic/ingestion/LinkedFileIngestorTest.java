package org.jabref.logic.ai.ingestion.logic.ingestion;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.ingestion.logic.documentsplitting.DocumentSplitter;
import org.jabref.logic.ai.ingestion.repositories.IngestedDocumentsRepository;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.LinkedFile;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@NullMarked
class LinkedFileIngestorTest {

    @Test
    void ingestSkipsOnlineLinks() throws InterruptedException {
        LinkedFileIngestor ingestor = new LinkedFileIngestor(
                mock(FilePreferences.class),
                mock(IngestedDocumentsRepository.class),
                mock(EmbeddingStore.class),
                mock(EmbeddingModel.class),
                mock(DocumentSplitter.class));

        LinkedFile onlineLink = spy(new LinkedFile("", "https://arxiv.org/pdf/2301.00001v1", "PDF"));

        ingestor.ingest(mock(BibDatabaseContext.class), onlineLink);

        verify(onlineLink, never()).findIn(any(BibDatabaseContext.class), any(FilePreferences.class));
    }
}
