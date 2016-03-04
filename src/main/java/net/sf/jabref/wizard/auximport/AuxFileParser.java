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
 * Latex Aux to Bibtex
 * <p>
 * Description: generates a sub-database which contains only bibtex entries
 * from input aux file</p>
 * Now - the unknown BibTeX entries cannot inserted into the reference
 * database without closing the dialog.
 */
public class AuxFileParser {
    private static final Log LOGGER = LogFactory.getLog(AuxFileParser.class);

    private static final Pattern TAG_PATTERN = Pattern.compile("\\\\(citation|abx@aux@cite)\\{(.+)\\}");

    private BibDatabase masterDatabase;
    private BibDatabase auxDatabase;

    private final Set<String> uniqueKeys = new HashSet<>();
    private final List<String> unresolvedKeys = new ArrayList<>();

    private int nestedAuxCounter;
    private int crossreferencedEntriesCount;

    /**
     * generate Shortcut method for easy generation.
     *
     * @param auxFile String
     * @param database BibDatabase - reference database
     * @return Vector - contains all not resolved bibtex entries
     */
    public final List<String> generateBibDatabase(String auxFile, BibDatabase database) {
        masterDatabase = database;
        parseAuxFile(auxFile);
        resolveTags();

        return unresolvedKeys;
    }

    public BibDatabase getGeneratedBibDatabase() {
        if (auxDatabase == null) {
            auxDatabase = new BibDatabase();
        }

        return auxDatabase;
    }

    public final int getFoundKeysInAux() {
        return uniqueKeys.size();
    }

    public final int getResolvedKeysCount() {
        return auxDatabase.getEntryCount() - crossreferencedEntriesCount;
    }

    public final int getNotResolvedKeysCount() {
        return unresolvedKeys.size();
    }

    /**
     * Query the number of extra entries pulled in due to crossrefs from other entries.
     *
     * @return The number of additional entries pulled in due to crossref
     */
    public final int getCrossreferencedEntriesCount() {
        return crossreferencedEntriesCount;
    }

    /** reset all used data structures */
    public final void clear() {
        uniqueKeys.clear();
        unresolvedKeys.clear();
        crossreferencedEntriesCount = 0;
        nestedAuxCounter = 0;
        masterDatabase = null;
        auxDatabase = null;
    }

    public String getInformation(boolean includeMissingEntries) {
        StringBuilder result = new StringBuilder();
        // print statistics
        result.append(Localization.lang("keys_in_database")).append(' ').append(masterDatabase.getEntryCount()).append('\n')
                .append(Localization.lang("found_in_aux_file")).append(' ').append(getFoundKeysInAux()).append('\n')
                .append(Localization.lang("resolved")).append(' ').append(getResolvedKeysCount()).append('\n')
                .append(Localization.lang("not_found")).append(' ').append(getNotResolvedKeysCount()).append('\n')
                .append(Localization.lang("crossreferenced entries included")).append(' ')
                .append(getCrossreferencedEntriesCount()).append('\n');

        if (includeMissingEntries && (getNotResolvedKeysCount() > 0)) {
            for (String entry : unresolvedKeys) {
                result.append(entry).append('\n');
            }
        }
        if (nestedAuxCounter > 0) {
            result.append(Localization.lang("nested_aux_files")).append(' ').append(nestedAuxCounter);
        }
        return result.toString();
    }

    /**
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

        nestedAuxCounter = -1; // count only the nested reads

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
                nestedAuxCounter++;
            } catch (FileNotFoundException e) {
                LOGGER.info("Cannot locate input file!", e);
            } catch (IOException e) {
                LOGGER.warn("Problem opening file!", e);
            }

            fileIndex++; // load next file
        }

        return true;
    }

    /**
     * resolveTags Try to find an equivalent bibtex entry into reference database for all keys (found in aux file).
     * This method will fill up some intern data structures.
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
                insertEntry(auxDatabase, entry);
                // Check if the entry we just found references another entry which
                // we don't already have in our list of entries to include. If so,
                // pull in that entry as well:
                entry.getFieldOptional("crossref").ifPresent(crossref -> {
                    if (!uniqueKeys.contains(crossref)) {
                        BibEntry refEntry = masterDatabase.getEntryByKey(crossref);
                        /**
                         * [ 1717849 ] Patch for aux import by Kai Eckert
                         */
                        if (refEntry == null) {
                            unresolvedKeys.add(crossref);
                        } else {
                            insertEntry(auxDatabase, refEntry);
                            crossreferencedEntriesCount++;
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

    /**
     * Insert a clone of the given entry. The clone is given a new unique ID.
     *
     * @param database The database to insert into.
     * @param entry The entry to insert a copy of.
     */
    private void insertEntry(BibDatabase database, BibEntry entry) {
        BibEntry clonedEntry = (BibEntry) entry.clone();
        clonedEntry.setId(IdGenerator.next());
        database.insertEntry(clonedEntry);
    }
}
