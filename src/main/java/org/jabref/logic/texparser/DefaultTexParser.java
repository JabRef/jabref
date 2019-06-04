package org.jabref.logic.texparser;

import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
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
    private static final String CITE_REGEX = String.format("\\\\(?:%s)\\*?(?:\\[(?:[^\\]]*)\\]){0,2}\\{(?<key>[^\\}]*)\\}",
            String.join("|", new String[] {
                    "[cC]ite(alt|alp|author|authorfull|date|num|p|t|text|title|url|year|yearpar)?",
                    "([aA]|fnote|foot|footfull|full|no|[nN]ote|[pP]aren|[pP]note|[tT]ext|[sS]mart|super)cite",
                    "footcitetext"
            }));

    private static final String INCLUDE_REGEX = "\\\\(?:include|input)\\{(?<file>[^\\}]*)\\}";

    private final BibDatabase masterDatabase;

    public DefaultTexParser(BibDatabase database) {
        masterDatabase = database;
    }

    @Override
    public TexParserResult parse(String citeString) {
        TexParserResult result = new TexParserResult(masterDatabase);
        matchCitation(result, Paths.get("foo/bar"), 1, citeString);
        resolveTags(result);
        return result;
    }

    @Override
    public TexParserResult parse(Path texFile) {
        return parse(new TexParserResult(masterDatabase), Collections.singletonList(texFile));
    }

    @Override
    public TexParserResult parse(List<Path> texFiles) {
        return parse(new TexParserResult(masterDatabase), texFiles);
    }

    /**
     * Parse a list of TEX files and, recursively, their referenced files.
     */
    private TexParserResult parse(TexParserResult result, List<Path> texFiles) {
        List<Path> referencedFiles = new ArrayList<>();

        for (int fileIndex = 0; fileIndex < texFiles.size(); fileIndex++) {
            Path file = texFiles.get(fileIndex);

            try (LineNumberReader lnr = new LineNumberReader(Files.newBufferedReader(file))) {
                for (String line = lnr.readLine(); line != null; line = lnr.readLine()) {
                    // Skip comment lines.
                    if (line.startsWith("%")) {
                        continue;
                    }

                    matchCitation(result, file, lnr.getLineNumber(), line);
                    matchNestedFile(result, file, texFiles, referencedFiles, line);
                }
            } catch (IOException e) {
                LOGGER.warn("Error opening the TEX file", e);
            }
        }

        if (!referencedFiles.isEmpty()) {
            parse(result, referencedFiles);
        }

        resolveTags(result);
        return result;
    }

    /**
     * Find cites along a specific line and store them.
     */
    private void matchCitation(TexParserResult result, Path file, int lineNumber, String line) {
        Matcher citeMatch = Pattern.compile(CITE_REGEX).matcher(line);

        while (citeMatch.find()) {
            String[] keys = citeMatch.group("key").split(",");

            for (String key : keys) {
                addKey(result, key.trim(), new Citation(file, lineNumber, citeMatch.start(), citeMatch.end(), line));
            }
        }
    }

    /**
     * Find inputs and includes along a specific line and store them for parsing later.
     */
    private void matchNestedFile(TexParserResult result, Path file, List<Path> texFiles, List<Path> referencedFiles, String line) {
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
                result.increaseNestedFilesCounter();
            }
        }
    }

    /**
     * Add a citation to the uniqueKeys map.
     */
    private void addKey(TexParserResult result, String key, Citation citation) {
        Map<String, List<Citation>> uniqueKeys = result.getUniqueKeys();

        if (!uniqueKeys.containsKey(key)) {
            uniqueKeys.put(key, new ArrayList<>());
        }

        if (!uniqueKeys.get(key).contains(citation)) {
            uniqueKeys.get(key).add(citation);
        }
    }

    /**
     * Look for an equivalent BibTeX entry within the reference database for all keys inside of the TEX files.
     */
    private void resolveTags(TexParserResult result) {
        Set<String> keySet = result.getUniqueKeys().keySet();

        for (String key : keySet) {
            if (!result.getGeneratedBibDatabase().getEntryByKey(key).isPresent()) {
                Optional<BibEntry> entry = masterDatabase.getEntryByKey(key);

                if (entry.isPresent()) {
                    insertEntry(result, entry.get());
                    CrossReferences.resolve(masterDatabase, result, entry.get());
                } else {
                    result.getUnresolvedKeys().add(key);
                }
            }
        }

        // Copy database definitions
        if (result.getGeneratedBibDatabase().hasEntries()) {
            result.getGeneratedBibDatabase().copyPreamble(masterDatabase);
            result.insertStrings(masterDatabase.getUsedStrings(result.getGeneratedBibDatabase().getEntries()));
        }
    }

    /**
     * Insert into the database a clone of the given entry. The cloned entry has a new unique ID.
     */
    private void insertEntry(TexParserResult result, BibEntry entry) {
        BibEntry clonedEntry = (BibEntry) entry.clone();
        result.getGeneratedBibDatabase().insertEntry(clonedEntry);
    }
}
