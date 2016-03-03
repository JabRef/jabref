package net.sf.jabref.wizard.auximport;

import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibtexString;
import net.sf.jabref.model.entry.IdGenerator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LaTeX Aux to BibTeX Parser
 * <p>
 * Extracts a subset of BibTeX entries from a BibDatabase that are included in an aux file.
 */
public class AuxFileParser {
    private static final Log LOGGER = LogFactory.getLog(AuxFileParser.class);

    private static final Pattern CITE_PATTERN = Pattern.compile("\\\\(citation|abx@aux@cite)\\{(.+)\\}");
    private static final Pattern INPUT_PATTERN = Pattern.compile("\\\\@input\\{(.+)\\}");

    private BibDatabase masterDatabase;

    private BibDatabase auxDatabase;
    private final Set<String> uniqueKeys = new HashSet<>();

    private final List<String> unresolvedKeys = new ArrayList<>();
    private int nestedAuxCount;

    private int crossRefEntriesCount;

    /**
     * Generates a database based on the given aux file and BibTeX database
     *
     * @param auxFile  Path to the LaTeX aux file
     * @param database BibTeX database
     */
    public AuxFileParser(String auxFile, BibDatabase database) {
        auxDatabase = new BibDatabase();
        masterDatabase = database;
        parseAuxFile(auxFile);
        resolveTags();
    }

    public BibDatabase getGeneratedBibDatabase() {
        return auxDatabase;
    }

    public List<String> getUnresolvedKeys() {
        return unresolvedKeys;
    }

    public int getFoundKeysInAux() {
        return uniqueKeys.size();
    }

    public int getResolvedKeysCount() {
        return auxDatabase.getEntryCount() - crossRefEntriesCount;
    }

    public int getUnresolvedKeysCount() {
        return unresolvedKeys.size();
    }

    /**
     * Query the number of extra entries pulled in due to crossrefs from other entries.
     *
     * @return The number of additional entries pulled in due to crossref
     */
    public int getCrossRefEntriesCount() {
        return crossRefEntriesCount;
    }

    /**
     * Prints parsing statistics
     *
     * @param includeMissingEntries
     * @return
     */
    public String getInformation(boolean includeMissingEntries) {
        StringBuilder result = new StringBuilder();

        result.append(Localization.lang("keys_in_database")).append(' ').append(masterDatabase.getEntryCount()).append('\n')
                .append(Localization.lang("found_in_aux_file")).append(' ').append(getFoundKeysInAux()).append('\n')
                .append(Localization.lang("resolved")).append(' ').append(getResolvedKeysCount()).append('\n')
                .append(Localization.lang("not_found")).append(' ').append(getUnresolvedKeysCount()).append('\n')
                .append(Localization.lang("crossreferenced entries included")).append(' ')
                .append(getCrossRefEntriesCount()).append('\n');

        if (includeMissingEntries && (getUnresolvedKeysCount() > 0)) {
            for (String entry : unresolvedKeys) {
                result.append(entry).append('\n');
            }
        }
        if (nestedAuxCount > 0) {
            result.append(Localization.lang("nested_aux_files")).append(' ').append(nestedAuxCount);
        }
        return result.toString();
    }

    /*
     * Parses the aux file and extracts all bib keys.
     * Also supports nested aux files (latex \\include).
     *
     * There exists no specification of the aux file.
     * Every package, class or document can write to the aux file.
     * The aux file consists of LaTeX macros and is read at the \begin{document} and again at the \end{document}.
     *
     * BibTeX citation: \citation{x,y,z}
     * Biblatex citation: \abx@aux@cite{x,y,z}
     * Nested aux files: \@input{x}
     */
    private void parseAuxFile(String filename) {
        // nested aux files
        List<String> fileList = new ArrayList<>(1);
        fileList.add(filename);

        int fileIndex = 0;

        while (fileIndex < fileList.size()) {
            String file = fileList.get(fileIndex);

            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;

                while ((line = br.readLine()) != null) {
                    Matcher citeMatch = CITE_PATTERN.matcher(line);

                    while (citeMatch.find()) {
                        String keyString = citeMatch.group(2);
                        String[] keys = keyString.split(",");

                        for (String key : keys) {
                            uniqueKeys.add(key.trim());
                        }
                    }

                    Matcher inputMatch = INPUT_PATTERN.matcher(line);

                    while (inputMatch.find()) {
                        String inputString = inputMatch.group(1);

                        String inputFile = inputString;
                        Path rootPath = new File(filename).toPath().getParent();
                        if (rootPath != null) {
                            inputFile = rootPath.resolve(inputString).toString();
                        }

                        if (!fileList.contains(inputFile)) {
                            fileList.add(inputFile);
                            nestedAuxCount++;
                        }
                    }
                }
            } catch (FileNotFoundException e) {
                LOGGER.info("Cannot locate input file", e);
            } catch (IOException e) {
                LOGGER.warn("Problem opening file", e);
            }

            fileIndex++;
        }
    }

    /*
     * Try to find an equivalent BibTeX entry inside the reference database for all keys inside the aux file.
     */
    private void resolveTags() {
        for (String key : uniqueKeys) {
            BibEntry entry = masterDatabase.getEntryByKey(key);

            if (entry == null) {
                unresolvedKeys.add(key);
            } else {
                insertEntry(entry);
                resolveCrossReferences(entry);
            }
        }

        if (auxDatabase.getEntryCount() > 0) {
            copyDatabaseConfiguration();
        }
    }

    /*
     * Resolves and adds CrossRef entries
     */
    private void resolveCrossReferences(BibEntry entry) {
        entry.getFieldOptional("crossref").ifPresent(crossref -> {
            if (!uniqueKeys.contains(crossref)) {
                BibEntry refEntry = masterDatabase.getEntryByKey(crossref);

                if (refEntry == null) {
                    unresolvedKeys.add(crossref);
                } else {
                    insertEntry(refEntry);
                    crossRefEntriesCount++;
                }
            }
        });
    }

    /*
     * Insert a clone of the given entry. The clone is given a new unique ID.
     */
    private void insertEntry(BibEntry entry) {
        BibEntry clonedEntry = (BibEntry) entry.clone();
        clonedEntry.setId(IdGenerator.next());
        auxDatabase.insertEntry(clonedEntry);
    }

    /*
     *  Copy the database's configuration, i.e., preamble and strings.
     */
    private void copyDatabaseConfiguration() {
        auxDatabase.setPreamble(masterDatabase.getPreamble());
        Set<String> keys = masterDatabase.getStringKeySet();
        for (String key : keys) {
            BibtexString string = masterDatabase.getString(key);
            auxDatabase.addString(string);
        }
    }
}
