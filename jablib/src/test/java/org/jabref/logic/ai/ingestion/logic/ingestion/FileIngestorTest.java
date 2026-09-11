package org.jabref.logic.ai.ingestion.logic.ingestion;

import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.ai.ingestion.logic.parsing.UniversalContentParser;

import dev.langchain4j.data.document.Metadata;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@NullMarked
class FileIngestorTest {

    private UniversalContentParser universalContentParser;
    private TextIngestor textIngestor;
    private FileIngestor fileIngestor;
    private Path testPath;

    @BeforeEach
    void setUp() {
        universalContentParser = mock(UniversalContentParser.class);
        textIngestor = mock(TextIngestor.class);
        fileIngestor = new FileIngestor(universalContentParser, textIngestor);
        testPath = Path.of("test.pdf");
    }

    @Test
    void ingestSinglePageStoresPageNumberMetadata() throws Exception {
        when(universalContentParser.parse(testPath)).thenReturn(List.of("Page 1 content"));

        Metadata metadata = new Metadata();
        metadata.put("fileHash", "abc123hash");

        fileIngestor.ingest(metadata, testPath);

        ArgumentCaptor<Metadata> metadataCaptor = ArgumentCaptor.forClass(Metadata.class);
        verify(textIngestor).ingest(metadataCaptor.capture(), eq("Page 1 content"));

        Metadata capturedMetadata = metadataCaptor.getValue();
        assertEquals(1, capturedMetadata.getInteger(FileIngestor.PAGE_NUMBER_METADATA_KEY));
        assertEquals("abc123hash", capturedMetadata.getString("fileHash"));
    }

    @Test
    void ingestMultiplePagesStoresSequentialPageNumbers() throws Exception {
        when(universalContentParser.parse(testPath)).thenReturn(List.of("Page 1", "Page 2", "Page 3"));

        Metadata metadata = new Metadata();
        fileIngestor.ingest(metadata, testPath);

        ArgumentCaptor<Metadata> metadataCaptor = ArgumentCaptor.forClass(Metadata.class);
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        verify(textIngestor, times(3)).ingest(metadataCaptor.capture(), textCaptor.capture());

        List<Metadata> capturedMetadatas = metadataCaptor.getAllValues();
        List<String> capturedTexts = textCaptor.getAllValues();

        assertEquals(List.of("Page 1", "Page 2", "Page 3"), capturedTexts);
        assertEquals(1, capturedMetadatas.getFirst().getInteger(FileIngestor.PAGE_NUMBER_METADATA_KEY));
        assertEquals(2, capturedMetadatas.get(1).getInteger(FileIngestor.PAGE_NUMBER_METADATA_KEY));
        assertEquals(3, capturedMetadatas.get(2).getInteger(FileIngestor.PAGE_NUMBER_METADATA_KEY));
    }

    @Test
    void ingestSkipsBlankPagesWhilePreservingPageNumbers() throws Exception {
        when(universalContentParser.parse(testPath)).thenReturn(List.of("Page 1", "   ", "Page 3"));

        Metadata metadata = new Metadata();
        fileIngestor.ingest(metadata, testPath);

        ArgumentCaptor<Metadata> metadataCaptor = ArgumentCaptor.forClass(Metadata.class);
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        verify(textIngestor, times(2)).ingest(metadataCaptor.capture(), textCaptor.capture());

        List<Metadata> capturedMetadatas = metadataCaptor.getAllValues();
        List<String> capturedTexts = textCaptor.getAllValues();

        assertEquals(List.of("Page 1", "Page 3"), capturedTexts);
        assertEquals(1, capturedMetadatas.getFirst().getInteger(FileIngestor.PAGE_NUMBER_METADATA_KEY));
        assertEquals(3, capturedMetadatas.get(1).getInteger(FileIngestor.PAGE_NUMBER_METADATA_KEY));
    }

    @Test
    void ingestThrowsExceptionWhenPagesEmpty() {
        when(universalContentParser.parse(testPath)).thenReturn(List.of());

        assertThrows(RuntimeException.class, () -> fileIngestor.ingest(new Metadata(), testPath));
    }

    @Test
    void ingestThrowsExceptionWhenAllPagesBlank() {
        when(universalContentParser.parse(testPath)).thenReturn(List.of("   ", "\t\n"));

        assertThrows(RuntimeException.class, () -> fileIngestor.ingest(new Metadata(), testPath));
    }
}
