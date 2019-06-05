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

import org.jabref.model.texparser.Citation;
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
    private static final String[] CITE_COMMANDS = new String[] {
            "[cC]ite(alt|alp|author|authorfull|date|num|p|t|text|title|url|year|yearpar)?",
            "([aA]|fnote|foot|footfull|full|no|[nN]ote|[pP]aren|[pP]note|[tT]ext|[sS]mart|super)cite",
            "footcitetext"
    };
    private static final String CITE_REGEX = String.format("\\\\(?:%s)\\*?(?:\\[(?:[^\\]]*)\\]){0,2}\\{(?<key>[^\\}]*)\\}",
            String.join("|", CITE_COMMANDS));

    private static final String INCLUDE_REGEX = "\\\\(?:include|input)\\{(?<file>[^\\}]*)\\}";

    private final TexParserResult result;

    public DefaultTexParser() {
        this.result = new TexParserResult();
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

        if (result.getFileList().isEmpty()) {
            result.getFileList().addAll(texFiles);
        } else {
            result.getNestedFiles().addAll(texFiles);
        }

        for (int fileIndex = 0; fileIndex < texFiles.size(); fileIndex++) {
            Path file = texFiles.get(fileIndex);

            try (LineNumberReader lnr = new LineNumberReader(Files.newBufferedReader(file))) {
                for (String line = lnr.readLine(); line != null; line = lnr.readLine()) {
                    if (line.startsWith("%")) {
                        // Skip comment lines.
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
     * Find cites along a specific line and add them to a map.
     */
    private void matchCitation(Path file, int lineNumber, String line) {
        Matcher citeMatch = Pattern.compile(CITE_REGEX).matcher(line);

        while (citeMatch.find()) {
            String[] keys = citeMatch.group("key").split(",");

            for (String key : keys) {
                Citation citation = new Citation(file, lineNumber, citeMatch.start(), citeMatch.end(), line);

                if (!result.getCitations().containsKey(key)) {
                    result.getCitations().put(key, new ArrayList<>());
                }

                if (!result.getCitations().get(key).contains(citation)) {
                    result.getCitations().get(key).add(citation);
                }
            }
        }
    }

    /**
     * Find inputs and includes along a specific line and store them for parsing later.
     */
    private void matchNestedFile(Path file, List<Path> texFiles, List<Path> referencedFiles, String line) {
        Matcher includeMatch = Pattern.compile(INCLUDE_REGEX).matcher(line);

        while (includeMatch.find()) {
            String include = includeMatch.group("file");

            if (!include.endsWith(".tex")) {
                include += ".tex";
            }

            Path folder = file.getParent();
            Path inputFile = (folder != null)
                    ? folder.resolve(include)
                    : Paths.get(include);

            if (!texFiles.contains(inputFile)) {
                referencedFiles.add(inputFile);
            }
        }
    }
}
