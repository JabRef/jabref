package org.jabref.logic.texparser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.model.texparser.LatexParserResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultLatexParser implements LatexParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultLatexParser.class);
    private static final String TEX_EXT = ".tex";
    private static final String BIB_EXT = ".bib";

    /**
     * It is allowed to add new cite commands for pattern matching. Some valid examples: "citep", "[cC]ite", and
     * "[cC]ite(author|title|year|t|p)?".
     */
    private static final String[] CITE_COMMANDS = {
            "[cC]ite(alt|alp|author|authorfull|date|num|p|t|text|title|url|year|yearpar)?",
            "([aA]|[aA]uto|fnote|foot|footfull|full|no|[nN]ote|[pP]aren|[pP]note|[tT]ext|[sS]mart|super)cite([s*]?)",
            "footcitetext", "(block|text)cquote"
    };
    private static final String CITE_GROUP = "key";
    private static final Pattern CITE_PATTERN = Pattern.compile(
            String.format("\\\\(%s)\\*?(?:\\[(?:[^\\]]*)\\]){0,2}\\{(?<%s>[^\\}]*)\\}(?:\\{[^\\}]*\\})?",
                    String.join("|", CITE_COMMANDS), CITE_GROUP));

    private static final String BIBLIOGRAPHY_GROUP = "bib";
    private static final Pattern BIBLIOGRAPHY_PATTERN = Pattern.compile(
            String.format("\\\\(?:bibliography|addbibresource)\\{(?<%s>[^\\}]*)\\}", BIBLIOGRAPHY_GROUP));

    private static final String INCLUDE_GROUP = "file";
    private static final Pattern INCLUDE_PATTERN = Pattern.compile(
            String.format("\\\\(?:include|input)\\{(?<%s>[^\\}]*)\\}", INCLUDE_GROUP));

    private final LatexParserResult latexParserResult;

    public DefaultLatexParser() {
        this.latexParserResult = new LatexParserResult();
    }

    public LatexParserResult getLatexParserResult() {
        return latexParserResult;
    }

    @Override
    public LatexParserResult parse(String citeString) {
        matchCitation(Path.of(""), 1, citeString);
        return latexParserResult;
    }

    @Override
    public LatexParserResult parse(Path latexFile) {
        return parse(Collections.singletonList(latexFile));
    }

    @Override
    public LatexParserResult parse(List<Path> latexFiles) {
        latexParserResult.addFiles(latexFiles);
        List<Path> referencedFiles = new ArrayList<>();

        for (Path file : latexFiles) {
            if (!file.toFile().exists()) {
                LOGGER.error(String.format("File does not exist: %s", file));
                continue;
            }

            try (
                    InputStream inputStream = Files.newInputStream(file);
                    Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                    LineNumberReader lineNumberReader = new LineNumberReader(reader)) {
                for (String line = lineNumberReader.readLine(); line != null; line = lineNumberReader.readLine()) {
                    // Skip comments and blank lines.
                    if (line.trim().isEmpty() || line.trim().charAt(0) == '%') {
                        continue;
                    }
                    matchCitation(file, lineNumberReader.getLineNumber(), line);
                    matchBibFile(file, line);
                    matchNestedFile(file, latexFiles, referencedFiles, line);
                }
            } catch (ClosedChannelException e) {
                // User changed the underlying LaTeX file
                // We ignore this error and just continue with parsing
                LOGGER.info("Parsing has been interrupted");
            } catch (IOException | UncheckedIOException e) {
                // Some weired error during reading
                // We ignore this error and just continue with parsing
                LOGGER.info("Error while parsing file {}", file, e);
            }
        }

        // Parse all files referenced by TEX files, recursively.
        if (!referencedFiles.isEmpty()) {
            // modifies class variable latexParserResult
            parse(referencedFiles);
        }

        return latexParserResult;
    }

    /**
     * Find cites along a specific line and store them.
     */
    private void matchCitation(Path file, int lineNumber, String line) {
        Matcher citeMatch = CITE_PATTERN.matcher(line);

        while (citeMatch.find()) {
            for (String key : citeMatch.group(CITE_GROUP).split(",")) {
                latexParserResult.addKey(key.trim(), file, lineNumber, citeMatch.start(), citeMatch.end(), line);
            }
        }
    }

    /**
     * Find BIB files along a specific line and store them.
     */
    private void matchBibFile(Path file, String line) {
        Matcher bibliographyMatch = BIBLIOGRAPHY_PATTERN.matcher(line);

        while (bibliographyMatch.find()) {
            for (String bibString : bibliographyMatch.group(BIBLIOGRAPHY_GROUP).split(",")) {
                bibString = bibString.trim();
                Path bibFile = file.getParent().resolve(
                        bibString.endsWith(BIB_EXT)
                                ? bibString
                                : String.format("%s%s", bibString, BIB_EXT));

                if (bibFile.toFile().exists()) {
                    latexParserResult.addBibFile(file, bibFile);
                }
            }
        }
    }

    /**
     * Find inputs and includes along a specific line and store them for parsing later.
     */
    private void matchNestedFile(Path file, List<Path> texFiles, List<Path> referencedFiles, String line) {
        Matcher includeMatch = INCLUDE_PATTERN.matcher(line);

        while (includeMatch.find()) {
            String include = includeMatch.group(INCLUDE_GROUP);

            Path nestedFile = file.getParent().resolve(
                    include.endsWith(TEX_EXT)
                            ? include
                            : String.format("%s%s", include, TEX_EXT));

            if (nestedFile.toFile().exists() && !texFiles.contains(nestedFile)) {
                referencedFiles.add(nestedFile);
            }
        }
    }
}
