package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.jabref.gui.util.OptionalObjectProperty;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.FileType;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.logic.util.io.XMLUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class EpubImporter extends Importer {
    private static final char[] EPUB_HEADER_MAGIC_NUMBER = {0x50, 0x4b, 0x03, 0x04};

    private final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    private final DocumentBuilder builder = factory.newDocumentBuilder();

    private final XPathFactory xPathFactory = XPathFactory.newInstance();
    private final XPath xpath = xPathFactory.newXPath();

    private final XPathExpression titlePath = xpath.compile("/package/metadata/title");
    private final XPathExpression creatorPath = xpath.compile("/package/metadata/creator");
    private final XPathExpression identifierPath = xpath.compile("/package/metadata/identifier");
    private final XPathExpression languagePath = xpath.compile("/package/metadata/language");
    private final XPathExpression sourcePath = xpath.compile("/package/metadata/source");
    private final XPathExpression descriptionPath = xpath.compile("/package/metadata/description");
    private final XPathExpression subjectPath = xpath.compile("/package/metadata/subject");

    private BibEntry entry = new BibEntry(StandardEntryType.Book);

    private final ImportFormatPreferences importFormatPreferences;

    public EpubImporter(ImportFormatPreferences importFormatPreferences) throws XPathExpressionException, ParserConfigurationException {
        this.importFormatPreferences = importFormatPreferences;
    }

    // ePUB is a ZIP-based format, so this method will clash with other ZIP-based formats.
    // Currently, only `.ctv6bak` is found.
    @Override
    public boolean isRecognizedFormat(BufferedReader input) throws IOException {
        char[] header = new char[EPUB_HEADER_MAGIC_NUMBER.length];
        int nRead = input.read(header);
        return nRead == EPUB_HEADER_MAGIC_NUMBER.length && Arrays.equals(header, EPUB_HEADER_MAGIC_NUMBER);
    }

    @Override
    public ParserResult importDatabase(Path filePath) throws IOException {
        // Not in functional programming style, but making {@link entry} a local mutable variable makes it easier
        // to write {@link addField}.
        // Potentially, this class won't work properly in concurrent situations.

        entry = new BibEntry(StandardEntryType.Book);

        try (FileSystem fileSystem = FileSystems.newFileSystem(filePath)) {
            OptionalObjectProperty<Path> metadataFilePath = OptionalObjectProperty.empty();

            Files.walkFileTree(fileSystem.getPath("/"), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.toString().endsWith(".opf")) {
                        metadataFilePath.set(Optional.of(file));
                        return FileVisitResult.TERMINATE;
                    }
                    return FileVisitResult.CONTINUE;
                }
            });

            if (metadataFilePath.get().isEmpty()) {
                return ParserResult.fromErrorMessage(Localization.lang("Could not find metadata file. Possibly corrupted ePUB file."));
            }

            File metadataFile = FileUtil.remapZipPath(metadataFilePath.get().get()).toFile();
            Document document = builder.parse(metadataFile);

            Optional<String> title = XMLUtil.getNodeContentByXPath(document, titlePath);
            Optional<String> identifier = XMLUtil.getNodeContentByXPath(document, identifierPath);
            Optional<String> source = XMLUtil.getNodeContentByXPath(document, sourcePath);
            Optional<String> description = XMLUtil.getNodeContentByXPath(document, descriptionPath);

            List<String> authors = XMLUtil.getNodesContentByXPath(document, creatorPath);
            List<String> subjects = XMLUtil.getNodesContentByXPath(document, subjectPath);
            List<String> languages = XMLUtil.getNodesContentByXPath(document, languagePath);

            // TODO: Extract editors.

            addField(StandardField.TITLE, title);
            addField(StandardField.ABSTRACT, description);

            if (source.isPresent()) {
                addField(StandardField.URL, source);
            } else {
                addField(StandardField.URL, identifier);
            }

            addField(StandardField.AUTHOR, Optional.of(String.join(" and ", authors)));

            // Might not be the right way. Leaving, as it still contains information.
            addField(StandardField.LANGUAGE, Optional.of(String.join(" and ", languages)));

            entry.addKeywords(subjects, importFormatPreferences.bibEntryPreferences().getKeywordSeparator());

            entry.addFile(new LinkedFile("", filePath.toAbsolutePath(), StandardFileType.EPUB.getName()));

            return ParserResult.fromEntry(entry);
        } catch (SAXException | XPathExpressionException e) {
            return ParserResult.fromError(e);
        }
    }

    // Tradeoff between conforming to controversial code standard and code simplicity.
    // This refs: https://peps.python.org/pep-0008/#a-foolish-consistency-is-the-hobgoblin-of-little-minds.
    private void addField(Field field, Optional<String> value) {
        value.ifPresent(it -> entry.setField(field, it));
    }

    @Override
    public ParserResult importDatabase(BufferedReader input) throws IOException {
        throw new UnsupportedOperationException("EpubImporter does not support importDatabase(BufferedReader reader). "
                + "Instead use importDatabase(Path filePath).");
    }

    @Override
    public String getId() {
        return "epub";
    }

    @Override
    public String getName() {
        return "ePUB";
    }

    @Override
    public String getDescription() {
        return Localization.lang("Import the popular e-book file format ePUB");
    }

    @Override
    public FileType getFileType() {
        return StandardFileType.EPUB;
    }
}
