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
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.model.texparser.LatexParserResult;
import org.jabref.model.texparser.LatexParserResults;

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
            "\\\\(%s)\\*?(?:\\[(?:[^\\]]*)\\]){0,2}\\{(?<%s>[^\\}]*)\\}(?:\\{[^\\}]*\\})?".formatted(
                    String.join("|", CITE_COMMANDS), CITE_GROUP));

    private static final String BIBLIOGRAPHY_GROUP = "bib";
    private static final Pattern BIBLIOGRAPHY_PATTERN = Pattern.compile(
            "\\\\(?:bibliography|addbibresource)\\{(?<%s>[^\\}]*)\\}".formatted(BIBLIOGRAPHY_GROUP));

    private static final String INCLUDE_GROUP = "file";
    private static final Pattern INCLUDE_PATTERN = Pattern.compile(
            "\\\\(?:include|input)\\{(?<%s>[^\\}]*)\\}".formatted(INCLUDE_GROUP));

    private final LatexParserResults latexParserResults;

    public DefaultLatexParser() {
        this.latexParserResults = new LatexParserResults();
    }

    @Override
    public LatexParserResult parse(String citeString) {
        Path path = Path.of("");
        LatexParserResult latexParserResult = new LatexParserResult(path);
        matchCitation(path, 1, citeString, latexParserResult);
        return latexParserResult;
    }

    @Override
    public LatexParserResult parse(Path latexFile) {
        if (!Files.exists(latexFile)) {
            LOGGER.error("File does not exist: {}", latexFile);
            return null;
        }
        LatexParserResult latexParserResult = new LatexParserResult(latexFile);

        try (InputStream inputStream = Files.newInputStream(latexFile);
             Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             LineNumberReader lineNumberReader = new LineNumberReader(reader)) {
            for (String line = lineNumberReader.readLine(); line != null; line = lineNumberReader.readLine()) {
                // Skip comments and blank lines.
                if (line.trim().isEmpty() || line.trim().charAt(0) == '%') {
                    continue;
                }
                matchCitation(latexFile, lineNumberReader.getLineNumber(), line, latexParserResult);
                matchBibFile(latexFile, line, latexParserResult);
                matchNestedFile(latexFile, line, latexParserResult);
            }
        } catch (ClosedChannelException e) {
            // User changed the underlying LaTeX file
            // We ignore this error and just continue with parsing
            LOGGER.info("Parsing has been interrupted");
        } catch (IOException | UncheckedIOException e) {
            // Some weired error during reading
            // We ignore this error and just continue with parsing
            LOGGER.info("Error while parsing file {}", latexFile, e);
        }

        return latexParserResult;
    }

    @Override
    public LatexParserResults parse(List<Path> latexFiles) {
        for (Path latexFile : latexFiles) {
            if (!latexParserResults.isParsed(latexFile)) {
                LatexParserResult parsedTex = parse(latexFile);
                if (parsedTex != null) {
                    latexParserResults.add(latexFile, parsedTex);
                }
            }
        }

        Set<Path> nonParsedNestedFiles = latexParserResults.getNonParsedNestedFiles();
        // Parse all "non-parsed" files referenced by TEX files, recursively.
        if (!nonParsedNestedFiles.isEmpty()) {
            // modifies class variable latexParserResults
            parse(nonParsedNestedFiles.stream().toList());
        }

        return latexParserResults;
    }

    /**
     * Find cites along a specific line and store them.
     */
    private void matchCitation(Path file, int lineNumber, String line, LatexParserResult latexParserResult) {
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
    private void matchBibFile(Path file, String line, LatexParserResult latexParserResult) {
        Matcher bibliographyMatch = BIBLIOGRAPHY_PATTERN.matcher(line);

        while (bibliographyMatch.find()) {
            for (String bibString : bibliographyMatch.group(BIBLIOGRAPHY_GROUP).split(",")) {
                bibString = bibString.trim();
                Path bibFile = file.getParent().resolve(
                        bibString.endsWith(BIB_EXT)
                                ? bibString
                                : "%s%s".formatted(bibString, BIB_EXT));

                if (Files.exists(bibFile)) {
                    latexParserResult.addBibFile(bibFile);
                }
            }
        }
    }

    /**
     * Find inputs and includes along a specific line and store them for parsing later.
     */
    private void matchNestedFile(Path texFile, String line, LatexParserResult latexParserResult) {
        Matcher includeMatch = INCLUDE_PATTERN.matcher(line);

        while (includeMatch.find()) {
            String filenamePassedToInclude = includeMatch.group(INCLUDE_GROUP);
            String texFileName = filenamePassedToInclude.endsWith(TEX_EXT)
                    ? filenamePassedToInclude
                    : "%s%s".formatted(filenamePassedToInclude, TEX_EXT);
            Path nestedFile = texFile.getParent().resolve(texFileName);
            if (Files.exists(nestedFile)) {
                latexParserResult.addNestedFile(nestedFile);
            }
        }
    }
}
