package org.jabref.logic.texparser;

import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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

    private final TexParserResult result;

    public DefaultTexParser() {
        this.result = new TexParserResult();
    }

    public TexParserResult getResult() {
        return result;
    }

    @Override
    public TexParserResult parse(String citeString) {
        matchCitation(Paths.get("foo/bar"), 1, citeString);
        return result;
    }

    @Override
    public TexParserResult parse(Path texFile) {
        return parse(Collections.singletonList(texFile));
    }

    @Override
    public TexParserResult parse(List<Path> texFiles) {
        List<Path> referencedFiles = new ArrayList<>();

        result.addFiles(texFiles);

        for (Path file : texFiles) {
            try (LineNumberReader lnr = new LineNumberReader(Files.newBufferedReader(file))) {
                for (String line = lnr.readLine(); line != null; line = lnr.readLine()) {
                    if (line.isEmpty() || line.charAt(0) == '%') {
                        // Skip comments and blank lines.
                        continue;
                    }

                    matchCitation(file, lnr.getLineNumber(), line);
                    matchNestedFile(file, texFiles, referencedFiles, line);
                }
            } catch (IOException e) {
                LOGGER.warn("Error opening the TEX file", e);
            }
        }

        // Parse all files referenced by TEX files, recursively.
        if (!referencedFiles.isEmpty()) {
            parse(referencedFiles);
        }

        return result;
    }

    /**
     * Find cites along a specific line and store them.
     */
    private void matchCitation(Path file, int lineNumber, String line) {
        Matcher citeMatch = CITE_PATTERN.matcher(line);

        while (citeMatch.find()) {
            String[] keys = citeMatch.group(CITE_GROUP).split(",");

            for (String key : keys) {
                result.addKey(key, file, lineNumber, citeMatch.start(), citeMatch.end(), line);
            }
        }
    }

    /**
     * Find inputs and includes along a specific line and store them for parsing later.
     */
    private void matchNestedFile(Path file, List<Path> texFiles, List<Path> referencedFiles, String line) {
        Matcher includeMatch = INCLUDE_PATTERN.matcher(line);
        StringBuilder include;

        while (includeMatch.find()) {
            include = new StringBuilder(includeMatch.group(INCLUDE_GROUP));

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
