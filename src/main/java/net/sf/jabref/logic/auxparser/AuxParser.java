package net.sf.jabref.logic.auxparser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;
import net.sf.jabref.model.entry.IdGenerator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * LaTeX Aux to BibTeX Parser
 * <p>
 * Extracts a subset of BibTeX entries from a BibDatabase that are included in an AUX file.
 * Also supports nested AUX files (latex \\include).
 *
 * There exists no specification of the AUX file.
 * Every package, class or document can write to the AUX file.
 * The AUX file consists of LaTeX macros and is read at the \begin{document} and again at the \end{document}.
 *
 * BibTeX citation: \citation{x,y,z}
 * Biblatex citation: \abx@aux@cite{x,y,z}
 * Nested AUX files: \@input{x}
 */
public class AuxParser {
    private static final Log LOGGER = LogFactory.getLog(AuxParser.class);

    private static final Pattern CITE_PATTERN = Pattern.compile("\\\\(citation|abx@aux@cite)\\{(.+)\\}");
    private static final Pattern INPUT_PATTERN = Pattern.compile("\\\\@input\\{(.+)\\}");

    private final String auxFile;
    private final BibDatabase masterDatabase;

    /**
     * Generates a database based on the given AUX file and BibTeX database
     *
     * @param auxFile  Path to the LaTeX AUX file
     * @param database BibTeX database
     */
    public AuxParser(String auxFile, BibDatabase database) {
        this.auxFile = auxFile;
        masterDatabase = database;
    }

    /**
     * Executes the parsing logic and returns a result containing all information and the generated BibDatabase.
     *
     * @return an AuxParserResult containing the generated BibDatabase and parsing statistics
     */
    public AuxParserResult parse() {
        return parseAuxFile();
    }

    private AuxParserResult parseAuxFile() {
        AuxParserResult result = new AuxParserResult(masterDatabase);

        // nested AUX files
        List<String> fileList = new ArrayList<>(1);
        fileList.add(auxFile);

        int fileIndex = 0;

        while (fileIndex < fileList.size()) {
            String file = fileList.get(fileIndex);

            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;

                while ((line = br.readLine()) != null) {
                    matchCitation(result, line);
                    matchNestedAux(result, fileList, line);
                }
            } catch (FileNotFoundException e) {
                LOGGER.info("Cannot locate input file", e);
            } catch (IOException e) {
                LOGGER.warn("Problem opening file", e);
            }

            fileIndex++;
        }
        resolveTags(result);

        return result;
    }

    private void matchNestedAux(AuxParserResult result, List<String> fileList, String line) {
        Matcher inputMatch = INPUT_PATTERN.matcher(line);

        while (inputMatch.find()) {
            String inputString = inputMatch.group(1);

            String inputFile = inputString;
            Path rootPath = new File(auxFile).toPath().getParent();
            if (rootPath != null) {
                inputFile = rootPath.resolve(inputString).toString();
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

    /*
     * Try to find an equivalent BibTeX entry inside the reference database for all keys inside the AUX file.
     */
    private void resolveTags(AuxParserResult result) {
        for (String key : result.getUniqueKeys()) {
            Optional<BibEntry> entry = masterDatabase.getEntryByKey(key);

            if (entry.isPresent()) {
                insertEntry(entry.get(), result);
                resolveCrossReferences(entry.get(), result);
            } else {
                result.getUnresolvedKeys().add(key);
            }
        }

        // Copy database definitions
        if (result.getGeneratedBibDatabase().hasEntries()) {
            result.getGeneratedBibDatabase().copyPreamble(masterDatabase);
            result.getGeneratedBibDatabase().copyStrings(masterDatabase);
        }
    }

    /*
     * Resolves and adds CrossRef entries
     */
    private void resolveCrossReferences(BibEntry entry, AuxParserResult result) {
        entry.getField(FieldName.CROSSREF).ifPresent(crossref -> {
            if (!result.getGeneratedBibDatabase().getEntryByKey(crossref).isPresent()) {
                Optional<BibEntry> refEntry = masterDatabase.getEntryByKey(crossref);

                if (refEntry.isPresent()) {
                    insertEntry(refEntry.get(), result);
                    result.increaseCrossRefEntriesCounter();
                } else {
                    result.getUnresolvedKeys().add(crossref);
                }
            }
        });
    }

    /*
     * Insert a clone of the given entry. The clone is given a new unique ID.
     */
    private void insertEntry(BibEntry entry, AuxParserResult result) {
        BibEntry clonedEntry = (BibEntry) entry.clone();
        clonedEntry.setId(IdGenerator.next());
        result.getGeneratedBibDatabase().insertEntry(clonedEntry);
    }
}
