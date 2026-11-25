package org.jabref.languageserver.util;

import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.jabref.logic.JabRefException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.model.entry.BibEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.dataformat.yaml.YAMLMapper;

public class LspParserHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(LspParserHandler.class);

    /// Pandoc-style YAML front matter at the very start of a Markdown document
    private static final Pattern FRONT_MATTER_PATTERN = Pattern.compile("\\A---\\R(?<yaml>.*?)\\R(?:---|\\.\\.\\.)\\h*$", Pattern.DOTALL | Pattern.MULTILINE);
    private static final YAMLMapper YAML_MAPPER = new YAMLMapper();

    /// Keyed by [Path] (and not by the URI string), because clients encode URIs differently
    /// (e.g., VS Code sends `file:///c%3A/...`, whereas [Path#toUri()] yields `file:///C:/...`).
    /// Otherwise, a `.bib` file opened in the editor and referenced from front matter would be held twice.
    private final Map<Path, ParserResult> parserResults;

    /// Libraries whose (possibly unsaved) content comes from the editor; front matter must not replace it by the file on disk
    private final Set<Path> openInEditor = ConcurrentHashMap.newKeySet();

    public LspParserHandler() {
        this.parserResults = new ConcurrentHashMap<>();
    }

    public ParserResult parserResultFromString(String fileUri, String content, ImportFormatPreferences importFormatPreferences) throws JabRefException, IOException {
        // We use BibtexParser directly, because we do not want to add an extra DummyFileMonitor
        // Otherwise, we could use `OpenDatabase.loadDatabase(path, importFormatPreferences, new DummyFileUpdateMonitor())`
        Optional<Path> path = toPath(fileUri);
        if (path.isEmpty()) {
            return ParserResult.fromErrorMessage("Could not convert " + fileUri + " to a path");
        }
        openInEditor.add(path.get());
        return parse(path.get(), content, importFormatPreferences);
    }

    /// Unsaved edits of a closed document are discarded: the library falls back to the file on disk
    public void documentClosed(String fileUri, ImportFormatPreferences importFormatPreferences) {
        toPath(fileUri).filter(openInEditor::remove).ifPresent(path -> {
            parserResults.remove(path);
            loadFromDisk(path, importFormatPreferences);
        });
    }

    private void loadFromDisk(Path path, ImportFormatPreferences importFormatPreferences) {
        try {
            parse(path, Files.readString(path, BibtexImporter.getEncoding(path)), importFormatPreferences);
        } catch (IOException | JabRefException e) {
            parserResults.remove(path);
            LOGGER.debug("Could not load bibliography {}", path, e);
        }
    }

    private ParserResult parse(Path path, String content, ImportFormatPreferences importFormatPreferences) throws JabRefException, IOException {
        BibtexParser parser = new BibtexParser(importFormatPreferences);
        ParserResult parserResult = parser.parse(Reader.of(content));
        parserResult.getDatabaseContext().setDatabasePath(path);
        parserResults.put(path, parserResult);
        return parserResult;
    }

    public Optional<ParserResult> getParserResultForUri(String fileUri) {
        return toPath(fileUri).map(parserResults::get);
    }

    public Map<String, List<BibEntry>> searchForEntryByCitationKey(String citationKey) {
        Map<String, List<BibEntry>> result = new ConcurrentHashMap<>();
        parserResults.forEach((path, parserResult) -> {
            List<BibEntry> entries = parserResult.getDatabase().getEntriesByCitationKey(citationKey);
            if (!entries.isEmpty()) {
                result.put(path.toUri().toString(), entries);
            }
        });
        return result;
    }

    /// Parses the `.bib` files listed in the `bibliography` key of the YAML front matter of the given Markdown document.
    /// Relative paths are resolved against the directory of the Markdown document.
    ///
    /// See [Pandoc: Specifying bibliographic data](https://pandoc.org/MANUAL.html#specifying-bibliographic-data).
    // [impl->req~jabls.markdown.front-matter-bibliography~1]
    public void loadBibliographiesFromFrontMatter(String markdownUri, String content, ImportFormatPreferences importFormatPreferences) {
        Optional<Path> markdownPath = toPath(markdownUri);
        if (markdownPath.isEmpty()) {
            return;
        }
        for (String bibliography : getBibliographiesFromFrontMatter(content)) {
            Path bibPath;
            try {
                bibPath = markdownPath.get().resolveSibling(bibliography).normalize();
            } catch (InvalidPathException e) {
                LOGGER.debug("Invalid bibliography path {} referenced from {}", bibliography, markdownUri, e);
                continue;
            }
            if (!openInEditor.contains(bibPath)) {
                loadFromDisk(bibPath, importFormatPreferences);
            }
        }
    }

    static List<String> getBibliographiesFromFrontMatter(String content) {
        Matcher matcher = FRONT_MATTER_PATTERN.matcher(content);
        if (!matcher.find()) {
            return List.of();
        }
        JsonNode bibliography;
        try {
            JsonNode frontMatter = YAML_MAPPER.readTree(matcher.group("yaml"));
            if (frontMatter == null) {
                return List.of();
            }
            bibliography = frontMatter.path("bibliography");
        } catch (JacksonException e) {
            LOGGER.debug("Could not parse front matter", e);
            return List.of();
        }
        Stream<JsonNode> nodes = bibliography.isArray() ? bibliography.valueStream() : Stream.of(bibliography);
        return nodes.filter(JsonNode::isString)
                    .map(JsonNode::asString)
                    .toList();
    }

    private static Optional<Path> toPath(String fileUri) {
        try {
            return Optional.of(Path.of(new URI(fileUri)).normalize());
        } catch (URISyntaxException | IllegalArgumentException | FileSystemNotFoundException e) {
            LOGGER.debug("Could not convert {} to a path", fileUri, e);
            return Optional.empty();
        }
    }
}
