package org.jabref.logic.exporter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.fileformat.BibframeImporter;
import org.jabref.logic.importer.fileformat.MarcXmlParser;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// Run with MARC2BIBFRAME2_DIR and BIBFRAME2MARC_DIR set to the pinned source checkouts.
// [utest->req~export.bibframe.rdfxml~1]
@NullMarked
@Tag("converter")
class BibframeConverterIntegrationTest {
    private static final String FORWARD_COMMIT = "ed9abb038214474e8fc8ba4035d01c42fe0246de";
    private static final String REVERSE_COMMIT = "36a96c813437ac714e8c9479b2f7be56dc78671d";
    private static final Pattern TRAILING_PUNCTUATION = Pattern.compile("[\\s:.,;/]+$");

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
        Optional<String> reverseDirectory = Optional.ofNullable(System.getenv("BIBFRAME2MARC_DIR"));
        assumeTrue(reverseDirectory.isPresent(), "Set BIBFRAME2MARC_DIR to run the converter check");
        Path reverse = Path.of(reverseDirectory.orElseThrow());
        assertEquals(REVERSE_COMMIT, outputOf("git", "-C", reverse.toString(), "rev-parse", "HEAD").trim());
        assertEquals(0, exitCodeOf("make", "-C", reverse.toString()));

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
        for (int index = 0; index < graph.getDocumentElement().getChildNodes().getLength(); index++) {
            Node child = graph.getDocumentElement().getChildNodes().item(index);
            if (child instanceof Element element) {
                topLevels.add(element);
            }
        }
        assertEquals(entries.size() * 2, topLevels.size());
        for (int index = 0; index < entries.size(); index++) {
            Document single = factory.newDocumentBuilder().newDocument();
            Element root = (Element) single.importNode(graph.getDocumentElement(), false);
            single.appendChild(root);
            root.appendChild(single.importNode(topLevels.get(index * 2), true));
            root.appendChild(single.importNode(topLevels.get(index * 2 + 1), true));
            Path isolated = directory.resolve(index + "-isolated.rdf");
            TransformerFactory.newInstance().newTransformer().transform(new DOMSource(single), new StreamResult(isolated.toFile()));

            Path reversed = directory.resolve(index + "-reversed.xml");
            runToFile(reversed, "xsltproc", "--stringparam", "pRecordId", "split-" + index,
                    "--stringparam", "pGenerationDatestamp", "20200101000000.0",
                    reverse.resolve("bibframe2marc.xsl").toString(), isolated.toString());
            assertEquals(normalizedTitle(entries.get(index)), normalizedTitle(parseStandaloneMarc(reversed)));
        }
    }

    private void checkConverterChain(String name, Path directory) throws Exception {
        Optional<String> forwardDirectory = Optional.ofNullable(System.getenv("MARC2BIBFRAME2_DIR"));
        Optional<String> reverseDirectory = Optional.ofNullable(System.getenv("BIBFRAME2MARC_DIR"));
        assumeTrue(forwardDirectory.isPresent() && reverseDirectory.isPresent(),
                "Set MARC2BIBFRAME2_DIR and BIBFRAME2MARC_DIR to run the converter check");

        Path forward = Path.of(forwardDirectory.orElseThrow());
        Path reverse = Path.of(reverseDirectory.orElseThrow());
        assertEquals(FORWARD_COMMIT, outputOf("git", "-C", forward.toString(), "rev-parse", "HEAD").trim());
        assertEquals(REVERSE_COMMIT, outputOf("git", "-C", reverse.toString(), "rev-parse", "HEAD").trim());
        assertEquals(0, exitCodeOf("make", "-C", reverse.toString()));

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
        assertEquals(imported.getType(), reimported.getType());
        assertEquals(imported.getField(StandardField.TITLE), reimported.getField(StandardField.TITLE));
        assertEquals(imported.getField(StandardField.DOI), reimported.getField(StandardField.DOI));

        Path reversedMarc = directory.resolve(name + "-reversed.xml");
        runToFile(reversedMarc, "xsltproc", "--stringparam", "pRecordId", name + "-001",
                "--stringparam", "pGenerationDatestamp", "20200101000000.0",
                reverse.resolve("bibframe2marc.xsl").toString(), exportedRdf.toString());
        BibEntry original = parseStandaloneMarc(source);
        BibEntry finalEntry = parseStandaloneMarc(reversedMarc);

        assertEquals(normalizedTitle(original), normalizedTitle(finalEntry));
        assertEquals(original.getField(StandardField.AUTHOR), finalEntry.getField(StandardField.AUTHOR));
        assertEquals(original.getField(StandardField.DOI), finalEntry.getField(StandardField.DOI));
        if ("book".equals(name)) {
            assertEquals(original.getField(StandardField.ISBN), finalEntry.getField(StandardField.ISBN));
            assertEquals(original.getField(StandardField.ABSTRACT), finalEntry.getField(StandardField.ABSTRACT));
        } else {
            assertEquals(original.getField(StandardField.PAGES), finalEntry.getField(StandardField.PAGES));
            // bibframe2marc emits MARC 773 without $7; MarcXmlParser consequently cannot infer Article or journal.
            assertEquals(StandardEntryType.Misc, finalEntry.getType());
            assertEquals(Optional.empty(), finalEntry.getField(StandardField.JOURNAL));
        }
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

    private static String normalizedTitle(BibEntry entry) {
        return TRAILING_PUNCTUATION.matcher(entry.getField(StandardField.TITLE).orElseThrow()).replaceAll("");
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
