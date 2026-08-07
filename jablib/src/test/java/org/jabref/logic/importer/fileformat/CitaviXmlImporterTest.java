package org.jabref.logic.importer.fileformat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CitaviXmlImporterTest {

    CitaviXmlImporter citaviXmlImporter = new CitaviXmlImporter();

    public static Stream<Arguments> cleanUpText() {
        return Stream.of(
                Arguments.of("no action", "no action"),
                Arguments.of("\\{action\\}", "{action}"),
                Arguments.of("\\}", "}"));
    }

    @ParameterizedTest
    @MethodSource
    void cleanUpText(String expected, String input) {
        assertEquals(expected, citaviXmlImporter.cleanUpText(input));
    }

    @Test
    void importPreservesCitationKey(@TempDir Path tempDir) throws IOException {
        String xml = """
                <?xml version="1.0" encoding="utf-8"?>
                <CitaviExchangeData Version="6.0.0.0">
                  <References>
                    <Reference id="abc-123">
                      <ReferenceType>Book</ReferenceType>
                      <Title>Some Title</Title>
                      <Year>2020</Year>
                      <CitationKey>Har97</CitationKey>
                    </Reference>
                  </References>
                </CitaviExchangeData>
                """;

        List<BibEntry> entries = importFromXml(xml, tempDir);

        assertEquals(1, entries.size());
        assertEquals(Optional.of("Har97"), entries.getFirst().getCitationKey());
    }

    @Test
    void importIgnoresEmptyCitationKey(@TempDir Path tempDir) throws IOException {
        String xml = """
                <?xml version="1.0" encoding="utf-8"?>
                <CitaviExchangeData Version="6.0.0.0">
                  <References>
                    <Reference id="abc-123">
                      <ReferenceType>Book</ReferenceType>
                      <Title>Some Title</Title>
                      <CitationKey></CitationKey>
                    </Reference>
                  </References>
                </CitaviExchangeData>
                """;

        List<BibEntry> entries = importFromXml(xml, tempDir);

        assertEquals(1, entries.size());
        assertEquals(Optional.empty(), entries.getFirst().getCitationKey());
    }

    @Test
    void importStripsWhitespaceFromCitationKey(@TempDir Path tempDir) throws IOException {
        String xml = """
                <?xml version="1.0" encoding="utf-8"?>
                <CitaviExchangeData Version="6.0.0.0">
                  <References>
                    <Reference id="abc-123">
                      <ReferenceType>Book</ReferenceType>
                      <Title>Some Title</Title>
                      <CitationKey>Doe 2021</CitationKey>
                    </Reference>
                  </References>
                </CitaviExchangeData>
                """;

        List<BibEntry> entries = importFromXml(xml, tempDir);

        assertEquals(1, entries.size());
        assertEquals(Optional.of("Doe2021"), entries.getFirst().getCitationKey());
    }

    @Test
    void importStripsDisallowedCharactersFromCitationKey(@TempDir Path tempDir) throws IOException {
        String xml = """
                <?xml version="1.0" encoding="utf-8"?>
                <CitaviExchangeData Version="6.0.0.0">
                  <References>
                    <Reference id="abc-123">
                      <ReferenceType>Book</ReferenceType>
                      <Title>Some Title</Title>
                      <CitationKey>Smith,2020</CitationKey>
                    </Reference>
                  </References>
                </CitaviExchangeData>
                """;

        List<BibEntry> entries = importFromXml(xml, tempDir);

        assertEquals(1, entries.size());
        assertEquals(Optional.of("Smith2020"), entries.getFirst().getCitationKey());
    }

    private List<BibEntry> importFromXml(String xml, Path tempDir) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            zos.putNextEntry(new ZipEntry("test.xml"));
            zos.write(xml.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        Path ctv6bakFile = tempDir.resolve("test.ctv6bak");
        Files.write(ctv6bakFile, baos.toByteArray());
        return citaviXmlImporter.importDatabase(ctv6bakFile).getDatabase().getEntries();
    }
}
