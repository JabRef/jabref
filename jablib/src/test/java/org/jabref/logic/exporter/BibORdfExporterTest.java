package org.jabref.logic.exporter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.metadata.SaveOrder;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [utest->adr~export-ordered-bibo-authors-and-creators~1]
@NullMarked
@ResourceLock("exporter")
class BibORdfExporterTest {
    private static final String RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    private static final String BIBO = "http://purl.org/ontology/bibo/";
    private static final String FOAF = "http://xmlns.com/foaf/0.1/";
    private static final String DCTERMS = "http://purl.org/dc/terms/";

    @ParameterizedTest
    @MethodSource("authors")
    void exportsOrderedAuthors(StandardEntryType type, String authors, List<String> expected, @TempDir Path directory)
            throws IOException, SaveException, ParserConfigurationException, SAXException {
        var exporter = new TemplateExporter("BibO RDF", "bibordf", "bibordf", "", StandardFileType.RDF,
                LayoutFormatterPreferences.getDefault(), SaveOrder.getDefaultSaveOrder());
        BibEntry entry = new BibEntry(type).withField(StandardField.AUTHOR, authors);
        Path output = directory.resolve("export.rdf");
        exporter.export(new BibDatabaseContext(), output, List.of(entry));

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        var document = factory.newDocumentBuilder().parse(output.toFile());
        var creators = document.getElementsByTagNameNS(DCTERMS, "creator");
        assertEquals(expected.size(), creators.getLength());
        Map<String, String> namesById = IntStream.range(0, creators.getLength())
                                                 .mapToObj(index -> (Element) ((Element) creators.item(index)).getElementsByTagNameNS(FOAF, "Person").item(0))
                                                 .collect(Collectors.toMap(person -> person.getAttributeNS(RDF, "nodeID"), person -> person.getAttributeNS(FOAF, "name")));
        var lists = document.getElementsByTagNameNS(BIBO, "authorList");
        assertEquals(1, lists.getLength());
        var sequences = ((Element) lists.item(0)).getElementsByTagNameNS(RDF, "Seq");
        assertEquals(1, sequences.getLength());
        var members = ((Element) sequences.item(0)).getElementsByTagNameNS(RDF, "li");
        List<String> names = IntStream.range(0, members.getLength())
                                      .mapToObj(index -> ((Element) members.item(index)).getAttributeNS(RDF, "nodeID"))
                                      .map(namesById::get)
                                      .toList();
        assertEquals(expected, names);
    }

    private static Stream<Arguments> authors() {
        return Stream.of(
                Arguments.of(StandardEntryType.Article, "Doe, Jane", List.of("Jane Doe")),
                Arguments.of(StandardEntryType.Article, "Doe, Jane and {Research and Development}", List.of("Jane Doe", "Research and Development")),
                Arguments.of(StandardEntryType.Book, "Doe, Jane", List.of("Jane Doe")),
                Arguments.of(StandardEntryType.Book, "Doe, Jane and {Smith & Jones}", List.of("Jane Doe", "Smith & Jones")));
    }
}
