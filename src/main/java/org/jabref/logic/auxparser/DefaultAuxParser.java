package org.jabref.logic.auxparser;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.model.auxparser.AuxParser;
import org.jabref.model.auxparser.AuxParserResult;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LaTeX Aux to BibTeX Parser
 * <p>
 * Extracts a subset of BibTeX entries from a BibDatabase that are included in an AUX file. Also supports nested AUX
 * files (latex \\include).
 *
 * There exists no specification of the AUX file. Every package, class or document can write to the AUX file. The AUX
 * file consists of LaTeX macros and is read at the \begin{document} and again at the \end{document}.
 *
 * BibTeX citation: \citation{x,y,z} Biblatex citation: \abx@aux@cite{x,y,z} Nested AUX files: \@input{x}
 */
public class DefaultAuxParser implements AuxParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultAuxParser.class);

    private static final Pattern CITE_PATTERN = Pattern.compile("\\\\(citation|abx@aux@cite)\\{(.+)\\}");
    private static final Pattern INPUT_PATTERN = Pattern.compile("\\\\@input\\{(.+)\\}");

    private final BibDatabase masterDatabase;

    /**
     * Generates a database based on the given AUX file and BibTeX database
     *
     * @param database BibTeX database
     */
    public DefaultAuxParser(BibDatabase database) {
        masterDatabase = database;
    }

    @Override
    public AuxParserResult parse(Path auxFile) {
        return parseAuxFile(auxFile);
    }

    private AuxParserResult parseAuxFile(Path auxFile) {
        AuxParserResult result = new AuxParserResult(masterDatabase);

        // nested AUX files
        List<Path> fileList = new ArrayList<>(1);
        fileList.add(auxFile);

        int fileIndex = 0;

        while (fileIndex < fileList.size()) {
            Path file = fileList.get(fileIndex);

            try (BufferedReader br = Files.newBufferedReader(file)) {
                String line;

                while ((line = br.readLine()) != null) {
                    matchCitation(result, line);
                    matchNestedAux(auxFile, result, fileList, line);
                }
            } catch (FileNotFoundException e) {
                LOGGER.warn("Cannot locate input file", e);
            } catch (IOException e) {
                LOGGER.warn("Problem opening file", e);
            }

            fileIndex++;
        }
        resolveTags(result);

        return result;
    }

    private void matchNestedAux(Path baseAuxFile, AuxParserResult result, List<Path> fileList, String line) {
        Matcher inputMatch = INPUT_PATTERN.matcher(line);

        while (inputMatch.find()) {
            String inputString = inputMatch.group(1);

            Path inputFile;
            Path rootPath = baseAuxFile.getParent();
            if (rootPath != null) {
                inputFile = rootPath.resolve(inputString);
            } else {
                inputFile = Paths.get(inputString);
            }

            if (!fileList.contains(inputFile)) {
                fileList.add(inputFile);
                result.increaseNestedAuxFilesCounter();
            }
        }
    }

    private void matchCitation(AuxParserResult result, String line) {
        Matcher citeMatch = CITE_PATTERN.matcher(line);

        while (citeMatch.find()) {
            String keyString = citeMatch.group(2);
            String[] keys = keyString.split(",");

            for (String key : keys) {
                result.getUniqueKeys().add(key.trim());
            }
        }
    }

    /**
     * Try to find an equivalent BibTeX entry inside the reference database for all keys inside the AUX file.
     *
     * @param result AUX file
     */
    private void resolveTags(AuxParserResult result) {
        List<BibEntry> entriesToInsert = new ArrayList<>();

        for (String key : result.getUniqueKeys()) {
            if (!result.getGeneratedBibDatabase().getEntryByKey(key).isPresent()) {
                Optional<BibEntry> entry = masterDatabase.getEntryByKey(key);
                if (entry.isPresent()) {
                    entriesToInsert.add(entry.get());
                } else {
                    result.getUnresolvedKeys().add(key);
                }
            }
        }
        insertEntries(entriesToInsert, result);
        resolveCrossReferences(entriesToInsert, result);

        // Copy database definitions
        if (result.getGeneratedBibDatabase().hasEntries()) {
            result.getGeneratedBibDatabase().copyPreamble(masterDatabase);
            result.insertStrings(masterDatabase.getUsedStrings(result.getGeneratedBibDatabase().getEntries()));
        }
    }

    /**
     * Resolves and adds CrossRef entries to insert them in addition to the original entries
     *
     * @param entries Entries to check for CrossRefs
     * @param result AUX file
     */
    private void resolveCrossReferences(List<BibEntry> entries, AuxParserResult result) {
        List<BibEntry> entriesToInsert = new ArrayList<>();
        for (BibEntry entry : entries) {
            entry.getField(StandardField.CROSSREF).ifPresent(crossref -> {
                if (!result.getGeneratedBibDatabase().getEntryByKey(crossref).isPresent()) {
                    Optional<BibEntry> refEntry = masterDatabase.getEntryByKey(crossref);

                    if (refEntry.isPresent()) {
                        if (!entriesToInsert.contains(refEntry.get())) {
                            entriesToInsert.add(refEntry.get());
                            result.increaseCrossRefEntriesCounter();
                        }
                    } else {
                        result.getUnresolvedKeys().add(crossref);
                    }
                }
            });
        }
        insertEntries(entriesToInsert, result);
    }

    /**
     * Insert a clone of each given entry. The clones are each given a new unique ID.
     *
     * @param entries Entries to be cloned
     * @param result AUX file
     */
    private void insertEntries(List<BibEntry> entries, AuxParserResult result) {
        List<BibEntry> clonedEntries = new ArrayList<>();
        for (BibEntry entry : entries) {
            clonedEntries.add((BibEntry) entry.clone());
        }
        result.getGeneratedBibDatabase().insertEntries(clonedEntries);
    }
}
