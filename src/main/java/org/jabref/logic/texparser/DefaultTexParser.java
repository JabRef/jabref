package org.jabref.logic.texparser;

import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
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
     */
    private static final String[] CITE_COMMANDS = new String[] {"cite", "citep", "citet"};
    private static final String CITE_REGEX = String.format("\\\\(?:%s)\\{([^\\}]*)\\}",
            String.join("|", CITE_COMMANDS));

    private final BibDatabase masterDatabase;

    public DefaultTexParser(BibDatabase database) {
        masterDatabase = database;
    }

    @Override
    public TexParserResult parse(Path texFile) {
        return parseTexFiles(Arrays.asList(texFile));
    }

    @Override
    public TexParserResult parse(List<Path> texFiles) {
        return parseTexFiles(texFiles);
    }

    private TexParserResult parseTexFiles(List<Path> texFiles) {
        TexParserResult result = new TexParserResult(masterDatabase);
        int fileIndex = 0;

        while (fileIndex < texFiles.size()) {
            Path file = texFiles.get(fileIndex++);

            try (LineNumberReader lnr = new LineNumberReader(Files.newBufferedReader(file))) {
                for (String line = lnr.readLine(); line != null; line = lnr.readLine()) {
                    // Skip comment lines.
                    if (line.startsWith("%")) {
                        continue;
                    }

                    matchCitation(result, file, lnr.getLineNumber(), line);
                }
            } catch (IOException e) {
                LOGGER.warn("Error opening the TEX file", e);
            }
        }

        resolveTags(result);
        return result;
    }

    /*
     * Find cites along a specific line and store them.
     */
    private void matchCitation(TexParserResult result, Path file, int lineNumber, String line) {
        Matcher citeMatch = Pattern.compile(CITE_REGEX).matcher(line);

        while (citeMatch.find()) {
            String[] keys = citeMatch.group(1).split(",");

            for (String key : keys) {
                addKey(result, key.trim(), new Citation(file, lineNumber, citeMatch.start(), citeMatch.end(), line));
            }
        }
    }

    /*
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

    /*
     * Look for an equivalent BibTeX entry within the reference database for all keys inside of the TEX files.
     */
    private void resolveTags(TexParserResult result) {
        Set<String> keySet = result.getUniqueKeys().keySet();

        for (String key : keySet) {
            if (!result.getGeneratedBibDatabase().getEntryByKey(key).isPresent()) {
                Optional<BibEntry> entry = masterDatabase.getEntryByKey(key);

                if (entry.isPresent()) {
                    insertEntry(result, entry.get());
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

    /*
     * Insert into the database a clone of the given entry. The cloned entry has a new unique ID.
     */
    private void insertEntry(TexParserResult result, BibEntry entry) {
        BibEntry clonedEntry = (BibEntry) entry.clone();
        result.getGeneratedBibDatabase().insertEntry(clonedEntry);
    }
}
