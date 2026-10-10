package org.jabref.logic.exporter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.fileformat.BibframeImporter;
import org.jabref.logic.importer.fileformat.MarcXmlParser;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// Run with MARC2BIBFRAME2_DIR and BIBFRAME2MARC_DIR set to the pinned source checkouts.
// [utest->req~export.bibframe.rdfxml~1]
@NullMarked
@Tag("converter")
class BibframeConverterIntegrationTest {
    private static final String FORWARD_COMMIT = "ed9abb038214474e8fc8ba4035d01c42fe0246de";
    private static final String REVERSE_COMMIT = "36a96c813437ac714e8c9479b2f7be56dc78671d";
    private static final Optional<Path> FORWARD_DIRECTORY = Optional.ofNullable(System.getenv("MARC2BIBFRAME2_DIR")).map(Path::of);
    private static final Optional<Path> REVERSE_DIRECTORY = Optional.ofNullable(System.getenv("BIBFRAME2MARC_DIR")).map(Path::of);

    @BeforeAll
    static void prepareConverters() throws IOException, InterruptedException {
        if (FORWARD_DIRECTORY.isPresent()) {
            Path forward = FORWARD_DIRECTORY.orElseThrow();
            assertEquals(FORWARD_COMMIT, outputOf("git", "-C", forward.toString(), "rev-parse", "HEAD").trim());
        }
        if (REVERSE_DIRECTORY.isPresent()) {
            Path reverse = REVERSE_DIRECTORY.orElseThrow();
            assertEquals(REVERSE_COMMIT, outputOf("git", "-C", reverse.toString(), "rev-parse", "HEAD").trim());
            assertEquals(0, exitCodeOf("make", "-C", reverse.toString()));
        }
    }

    @Test
    void bookSurvivesConverterChain(@TempDir Path directory) throws Exception {
        checkConverterChain("book", directory);
    }

    @Test
    void articleSurvivesConverterChain(@TempDir Path directory) throws Exception {
        checkConverterChain("article", directory);
    }

    @Test
    void multipleEntriesSurviveSeparateReverseConversions(@TempDir Path directory) throws Exception {
        assumeTrue(REVERSE_DIRECTORY.isPresent(), "Set BIBFRAME2MARC_DIR to run the converter check");
        Path reverse = REVERSE_DIRECTORY.orElseThrow();

        BibframeImporter importer = new BibframeImporter();
        List<BibEntry> entries = new ArrayList<>();
        for (String name : List.of("book", "article")) {
            Path fixture = Path.of(BibframeConverterIntegrationTest.class.getResource(
                    "/org/jabref/logic/importer/bibframe/" + name + ".rdf").toURI());
            entries.add(importer.importDatabase(fixture).getDatabase().getEntries().getFirst());
        }
        Path combined = directory.resolve("combined.rdf");
        new BibframeExporter().export(new BibDatabaseContext(), combined, entries);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document graph = factory.newDocumentBuilder().parse(combined.toFile());
        List<Element> topLevels = new ArrayList<>();
        List<Integer> workIndexes = new ArrayList<>();
        for (int index = 0; index < graph.getDocumentElement().getChildNodes().getLength(); index++) {
            Node child = graph.getDocumentElement().getChildNodes().item(index);
            if (child instanceof Element element) {
                if ("Work".equals(element.getLocalName())) {
                    workIndexes.add(topLevels.size());
                }
                topLevels.add(element);
            }
        }
        assertEquals(entries.size(), workIndexes.size());
        for (int index = 0; index < entries.size(); index++) {
            String name = List.of("book", "article").get(index);
            Document single = factory.newDocumentBuilder().newDocument();
            Element root = (Element) single.importNode(graph.getDocumentElement(), false);
            single.appendChild(root);
            int end = index + 1 < workIndexes.size() ? workIndexes.get(index + 1) : topLevels.size();
            for (int elementIndex = workIndexes.get(index); elementIndex < end; elementIndex++) {
                root.appendChild(single.importNode(topLevels.get(elementIndex), true));
            }
            Path isolated = directory.resolve(index + "-isolated.rdf");
            TransformerFactory.newInstance().newTransformer().transform(new DOMSource(single), new StreamResult(isolated.toFile()));

            Path reversed = directory.resolve(index + "-reversed.xml");
            runToFile(reversed, "xsltproc", "--stringparam", "pRecordId", "split-" + index,
                    "--stringparam", "pGenerationDatestamp", "20200101000000.0",
                    reverse.resolve("bibframe2marc.xsl").toString(), isolated.toString());
            assertEquals(expectedReverseEntry(name), parseStandaloneMarc(reversed));
        }
    }

    @Test
    void locDatasetRecordRetainsExtentAndSupplementaryUrl(@TempDir Path directory) throws Exception {
        assumeTrue(FORWARD_DIRECTORY.isPresent(), "Set MARC2BIBFRAME2_DIR to run the converter check");
        Path forward = FORWARD_DIRECTORY.orElseThrow();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document dataset = factory.newDocumentBuilder().parse(forward.resolve("dataset/loc_general.xml").toFile());
        NodeList records = dataset.getElementsByTagNameNS("http://www.loc.gov/MARC21/slim", "record");
        Optional<Element> selected = Optional.empty();
        for (int index = 0; index < records.getLength(); index++) {
            Element record = (Element) records.item(index);
            NodeList fields = record.getElementsByTagNameNS("http://www.loc.gov/MARC21/slim", "datafield");
            for (int fieldIndex = 0; fieldIndex < fields.getLength(); fieldIndex++) {
                if ("856".equals(((Element) fields.item(fieldIndex)).getAttribute("tag"))) {
                    selected = Optional.of(record);
                    break;
                }
            }
            if (selected.isPresent()) {
                break;
            }
        }
        Document source = factory.newDocumentBuilder().newDocument();
        source.appendChild(source.importNode(selected.orElseThrow(), true));
        Path marc = directory.resolve("loc.marcxml");
        TransformerFactory.newInstance().newTransformer().transform(new DOMSource(source), new StreamResult(marc.toFile()));

        Path officialRdf = directory.resolve("loc-official.rdf");
        runToFile(officialRdf, "xsltproc", "--stringparam", "baseuri", "https://example.org/loc/",
                "--stringparam", "idfield", "001", forward.resolve("xsl/marc2bibframe2.xsl").toString(), marc.toString());
        BibframeImporter importer = new BibframeImporter();
        BibEntry imported = importer.importDatabase(officialRdf).getDatabase().getEntries().getFirst();
        assertEquals(Optional.of("267"), imported.getField(StandardField.PAGETOTAL));
        assertEquals(Optional.of("http://www.loc.gov/catdir/enhancements/fy0603/98190991-d.html"),
                imported.getField(StandardField.URL));

        Path exportedRdf = directory.resolve("loc-jabref.rdf");
        new BibframeExporter().export(new BibDatabaseContext(), exportedRdf, List.of(imported));
        assertEquals(imported, importer.importDatabase(exportedRdf).getDatabase().getEntries().getFirst());
    }

    private void checkConverterChain(String name, Path directory) throws Exception {
        assumeTrue(FORWARD_DIRECTORY.isPresent() && REVERSE_DIRECTORY.isPresent(),
                "Set MARC2BIBFRAME2_DIR and BIBFRAME2MARC_DIR to run the converter check");

        Path forward = FORWARD_DIRECTORY.orElseThrow();
        Path reverse = REVERSE_DIRECTORY.orElseThrow();

        Path source = Path.of(BibframeConverterIntegrationTest.class.getResource(
                "/org/jabref/logic/importer/bibframe/" + name + ".marcxml").toURI());
        Path officialRdf = directory.resolve(name + "-official.rdf");
        runToFile(officialRdf, "xsltproc", "--stringparam", "baseuri", "https://example.org/jabref/",
                "--stringparam", "idfield", "001", "--stringparam", "pGenerationDatestamp", "2020-01-01T00:00:00Z",
                "--stringparam", "idsource", "http://id.loc.gov/vocabulary/organizations/dlc",
                forward.resolve("xsl/marc2bibframe2.xsl").toString(), source.toString());

        BibframeImporter importer = new BibframeImporter();
        BibEntry imported = importer.importDatabase(officialRdf).getDatabase().getEntries().getFirst();
        Path exportedRdf = directory.resolve(name + "-jabref.rdf");
        new BibframeExporter().export(new BibDatabaseContext(), exportedRdf, List.of(imported));
        BibEntry reimported = importer.importDatabase(exportedRdf).getDatabase().getEntries().getFirst();
        assertEquals(imported, reimported);

        Path reversedMarc = directory.resolve(name + "-reversed.xml");
        runToFile(reversedMarc, "xsltproc", "--stringparam", "pRecordId", name + "-001",
                "--stringparam", "pGenerationDatestamp", "20200101000000.0",
                reverse.resolve("bibframe2marc.xsl").toString(), exportedRdf.toString());
        BibEntry finalEntry = parseStandaloneMarc(reversedMarc);

        assertEquals(expectedReverseEntry(name), finalEntry);
    }

    private static BibEntry parseStandaloneMarc(Path source) throws IOException, ParseException {
        String xml = Files.readString(source);
        if (xml.startsWith("<?xml")) {
            xml = xml.substring(xml.indexOf("?>") + 2);
        }
        String envelope = "<searchRetrieveResponse><records><record><recordData>"
                + xml + "</recordData></record></records></searchRetrieveResponse>";
        return new MarcXmlParser().parseEntries(new ByteArrayInputStream(envelope.getBytes(StandardCharsets.UTF_8))).getFirst();
    }

    private static BibEntry expectedReverseEntry(String name) throws Exception {
        if ("article".equals(name)) {
            Path source = Path.of(BibframeConverterIntegrationTest.class.getResource(
                    "/org/jabref/logic/importer/bibframe/article.marcxml").toURI());
            return parseStandaloneMarc(source);
        }
        return new BibEntry(StandardEntryType.Misc)
                .withField(StandardField.TITLE, "Marketing Automation with Mailchimp :")
                .withField(StandardField.SUBTITLE, "Expert Tips.")
                .withField(StandardField.AUTHOR, "Caraballo, Margarita J.")
                .withField(StandardField.ADDRESS, "Birmingham :")
                .withField(StandardField.PUBLISHER, "{Packt Publishing,}") // spellchecker:disable-line
                .withField(StandardField.YEAR, "2023")
                .withField(StandardField.ISBN, "9781800567566")
                .withField(StandardField.DOI, "10.1234/book.001")
                .withField(StandardField.ABSTRACT, "A guide to marketing automation.")
                .withField(StandardField.EDITION, "2nd ed.")
                .withField(StandardField.PAGETOTAL, "240")
                .withField(StandardField.SERIES, "Practical computing")
                .withFiles(List.of(new LinkedFile("", "https://example.org/book", StandardFileType.PDF)));
    }

    private static void runToFile(Path output, String... command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).redirectOutput(output.toFile()).start();
        String error = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(0, process.waitFor(), error);
    }

    private static int exitCodeOf(String... command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        process.getInputStream().readAllBytes();
        return process.waitFor();
    }

    private static String outputOf(String... command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(0, process.waitFor(), output);
        return output;
    }
}
