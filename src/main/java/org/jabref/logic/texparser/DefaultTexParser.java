package org.jabref.logic.texparser;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.UncheckedIOException;
import java.nio.channels.ClosedChannelException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.model.texparser.TexParser;
import org.jabref.model.texparser.TexParserResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultTexParser implements TexParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultTexParser.class);

    /**
     * It is allowed to add new cite commands for pattern matching.
     *
     * <p>Some valid examples: "citep", "[cC]ite", "[cC]ite(author|title|year|t|p)?"
     *
     * <p>TODO: Add support for multicite commands.
     */
    private static final String[] CITE_COMMANDS = {
            "[cC]ite(alt|alp|author|authorfull|date|num|p|t|text|title|url|year|yearpar)?",
            "([aA]|fnote|foot|footfull|full|no|[nN]ote|[pP]aren|[pP]note|[tT]ext|[sS]mart|super)cite",
            "footcitetext"
    };
    private static final String CITE_GROUP = "key";
    private static final Pattern CITE_PATTERN = Pattern.compile(
            String.format("\\\\(%s)\\*?(?:\\[(?:[^\\]]*)\\]){0,2}\\{(?<%s>[^\\}]*)\\}",
                    String.join("|", CITE_COMMANDS), CITE_GROUP));

    private static final String INCLUDE_GROUP = "file";
    private static final Pattern INCLUDE_PATTERN = Pattern.compile(
            String.format("\\\\(?:include|input)\\{(?<%s>[^\\}]*)\\}", INCLUDE_GROUP));

    private static final String TEX_EXT = ".tex";

    private final TexParserResult texParserResult;

    public DefaultTexParser() {
        this.texParserResult = new TexParserResult();
    }

    public TexParserResult getTexParserResult() {
        return texParserResult;
    }

    @Override
    public TexParserResult parse(String citeString) {
        matchCitation(null, Paths.get(""), 1, citeString);
        return texParserResult;
    }

    @Override
    public TexParserResult parse(Path texFile) {
        return parse(null, Collections.singletonList(texFile));
    }

    @Override
    public TexParserResult parse(List<Path> texFiles) {
        return parse(null, texFiles);
    }

    /**
     * Parse a list of TEX files for searching a given entry.
     *
     * @param entryKey String that contains the entry key we are searching (null for all entries)
     * @param texFiles List of Path objects linked to a TEX file
     * @return a TexParserResult, which contains all data related to the bibliographic entries
     */
    public TexParserResult parse(String entryKey, List<Path> texFiles) {
        texParserResult.addFiles(texFiles);

        List<Path> referencedFiles = new ArrayList<>();

        for (Path file : texFiles) {
            try (LineNumberReader lineNumberReader = new LineNumberReader(Files.newBufferedReader(file))) {
                for (String line = lineNumberReader.readLine(); line != null; line = lineNumberReader.readLine()) {
                    // Skip comments and blank lines.
                    if (line.isEmpty() || line.charAt(0) == '%') {
                        continue;
                    }
                    // Skip the citation matching if the line does not contain the given entry to speed up the parsing.
                    if (entryKey == null || line.contains(entryKey)) {
                        matchCitation(entryKey, file, lineNumberReader.getLineNumber(), line);
                    }
                    matchNestedFile(file, texFiles, referencedFiles, line);
                }
            } catch (ClosedChannelException e) {
                LOGGER.error("Parsing has been interrupted");
                return null;
            } catch (IOException e) {
                LOGGER.error("Error opening a TEX file: IOException");
            } catch (UncheckedIOException e) {
                LOGGER.error("Error searching files: UncheckedIOException");
            }
        }

        // Parse all files referenced by TEX files, recursively.
        if (!referencedFiles.isEmpty()) {
            parse(entryKey, referencedFiles);
        }

        return texParserResult;
    }

    /**
     * Find cites along a specific line and store them.
     */
    private void matchCitation(String entryKey, Path file, int lineNumber, String line) {
        Matcher citeMatch = CITE_PATTERN.matcher(line);

        while (citeMatch.find()) {
            Arrays.stream(citeMatch.group(CITE_GROUP).split(","))
                  .filter(key -> entryKey == null || key.equals(entryKey))
                  .forEach(key -> texParserResult.addKey(key, file, lineNumber, citeMatch.start(), citeMatch.end(), line));
        }
    }

    /**
     * Find inputs and includes along a specific line and store them for parsing later.
     */
    private void matchNestedFile(Path file, List<Path> texFiles, List<Path> referencedFiles, String line) {
        Matcher includeMatch = INCLUDE_PATTERN.matcher(line);

        while (includeMatch.find()) {
            StringBuilder include = new StringBuilder(includeMatch.group(INCLUDE_GROUP));

            if (!include.toString().endsWith(TEX_EXT)) {
                include.append(TEX_EXT);
            }

            Path folder = file.getParent();
            Path inputFile = (folder == null)
                    ? Paths.get(include.toString())
                    : folder.resolve(include.toString());

            if (!texFiles.contains(inputFile)) {
                referencedFiles.add(inputFile);
            }
        }
    }
}
