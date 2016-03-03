package net.sf.jabref.wizard.auximport;

import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibtexString;
import net.sf.jabref.model.entry.IdGenerator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LaTeX Aux to BibTeX Parser
 *
 * Extracts a subset of BibTeX entries from a BibDatabase that are included in an aux file.
 */
public class AuxFileParser {
    private static final Log LOGGER = LogFactory.getLog(AuxFileParser.class);

    private static final Pattern TAG_PATTERN = Pattern.compile("\\\\(citation|abx@aux@cite)\\{(.+)\\}");

    private BibDatabase masterDatabase;
    private BibDatabase auxDatabase;

    private final Set<String> uniqueKeys = new HashSet<>();
    private final List<String> unresolvedKeys = new ArrayList<>();

    private int nestedAuxCount;
    private int crossRefEntriesCount;

    /**
     * Generates a database based on the given aux file and BibTeX database
     *
     * @param auxFile Path to the LaTeX aux file
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
     * parseAuxFile read the Aux file and fill up some intern data structures. Nested aux files (latex \\include)
     * supported!
     *
     *     // found at comp.text.tex
     //  > Can anyone tell be the information held within a .aux file?  Is there a
     //  > specific format to this file?
     //
     // I don't think there is a particular format. Every package, class
     // or document can write to the aux file. The aux file consists of LaTeX macros
     // and is read at the \begin{document} and again at the \end{document}.
     //
     // It usually contains information about existing labels
     //  \\newlabel{sec:Intro}{{1}{1}}
     // and citations
     //  \citation{hiri:conv:1993}
     // and macros to write information to other files (like toc, lof or lot files)
     //  \@writefile{toc}{\contentsline {section}{\numberline
     // {1}Intro}{1}}
     // but as I said, there can be a lot more

     // aux file :
     //
     // \\citation{x}  x = used reference of bibtex library entry
     //
     // \\@input{x}  x = nested aux file
     //
     // the \\bibdata{x} directive contains information about the
     // bibtex library file -> x = name of bib file
     //
     // \\bibcite{x}{y}
     //   x is a label for an item and y is the index in bibliography
     * @param filename String : Path to LatexAuxFile
     * @return boolean, true = no error occurs
     */
    private boolean parseAuxFile(String filename) {
        // regular expressions
        Matcher matcher;

        // file list, used for nested aux files
        List<String> fileList = new ArrayList<>(5);
        fileList.add(filename);

        // get the file path
        File dummy = new File(filename);
        String path = dummy.getParent();
        if (path == null) {
            path = "";
        } else {
            path = path + File.separator;
        }

        nestedAuxCount = -1; // count only the nested reads

        // index of current file in list
        int fileIndex = 0;

        // while condition
        boolean cont;
        while (fileIndex < fileList.size()) {
            String fName = fileList.get(fileIndex);
            try (BufferedReader br = new BufferedReader(new FileReader(fName))) {
                cont = true;

                while (cont) {
                    Optional<String> maybeLine;
                    try {
                        maybeLine = Optional.ofNullable(br.readLine());
                    } catch (IOException ioe) {
                        maybeLine = Optional.empty();
                    }

                    if (maybeLine.isPresent()) {
                        String line = maybeLine.get();
                        matcher = TAG_PATTERN.matcher(line);

                        while (matcher.find()) {
                            // extract the bibtex-key(s) XXX from \citation{XXX} string
                            int len = matcher.end() - matcher.start();
                            if (len > 11) {
                                String str = matcher.group(2);
                                // could be an comma separated list of keys
                                String[] keys = str.split(",");
                                for (String dummyStr : keys) {
                                    // delete all unnecessary blanks and save key into an set
                                    uniqueKeys.add(dummyStr.trim());
                                }
                            }
                        }
                        // try to find a nested aux file
                        int index = line.indexOf("\\@input{");
                        if (index >= 0) {
                            int start = index + 8;
                            int end = line.indexOf('}', start);
                            if (end > start) {
                                String str = path + line.substring(index + 8, end);

                                // if filename already in file list
                                if (!fileList.contains(str)) {
                                    fileList.add(str); // insert file into file list
                                }
                            }
                        }
                    } else {
                        cont = false;
                    }
                }
                nestedAuxCount++;
            } catch (FileNotFoundException e) {
                LOGGER.info("Cannot locate input file!", e);
            } catch (IOException e) {
                LOGGER.warn("Problem opening file!", e);
            }

            fileIndex++; // load next file
        }

        return true;
    }

    /*
     * Try to find an equivalent BibTeX entry inside the reference database for all keys inside the aux file.
     */
    private void resolveTags() {
        auxDatabase = new BibDatabase();
        unresolvedKeys.clear();

        // for all bibtex keys (found in aux-file) try to find an equivalent
        // entry into reference database
        for (String key : uniqueKeys) {
            BibEntry entry = masterDatabase.getEntryByKey(key);

            if (entry == null) {
                unresolvedKeys.add(key);
            } else {
                insertEntry(entry);
                // Check if the entry we just found references another entry which
                // we don't already have in our list of entries to include. If so,
                // pull in that entry as well:
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
        }

        if (auxDatabase.getEntryCount() > 0) {
            copyDatabaseConfiguration();
        }
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

    /*
     * Insert a clone of the given entry.
     * The clone is given a new unique ID.
     */
    private void insertEntry(BibEntry entry) {
        BibEntry clonedEntry = (BibEntry) entry.clone();
        clonedEntry.setId(IdGenerator.next());
        auxDatabase.insertEntry(clonedEntry);
    }
}
