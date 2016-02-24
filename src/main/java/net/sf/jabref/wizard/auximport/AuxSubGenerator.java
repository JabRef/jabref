/*
Copyright (C) 2004 R. Nagel
Copyright (C) 2015-2016 T. Denkinger, JabRef Contributors

All programs in this directory and
subdirectories are published under the GNU General Public License as
described below.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or (at
your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
USA

Further information about the GNU GPL is available at:
http://www.gnu.org/copyleft/gpl.ja.html

*/

/**
 * <p>Title: Latex Aux to Bibtex</p>
 * <p>
 * <p>Description: generates a sub-database which contains only bibtex entries
 * from input aux file</p>
 * <p>
 * <p>Copyright: Copyright (c) 2004</p>
 * <p>
 * <p>Company: </p>
 *
 * @version 1.0
 * @author r.nagel
 * @todo Redesign of dialog structure for an assistant like feeling....
 * Now - the unknown BibTeX entries cannot inserted into the reference
 * database without closing the dialog.
 */

// created by : r.nagel 23.08.2004
//
// modified : - 11.04.2005
//              handling \\@input{file.aux} tag in aux files (nested aux files)

package net.sf.jabref.wizard.auximport;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.jabref.model.entry.IdGenerator;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibtexString;

public class AuxSubGenerator {

    private final Set<String> mySet = new HashSet<>(20); // all unique bibtex keys in aux file

    private final List<String> notFoundList = new ArrayList<>(); // all not solved bibtex keys

    private BibDatabase db; // reference database
    private BibDatabase auxDB; // contains only the bibtex keys who found in aux file

    private int nestedAuxCounter; // counts the nested aux files
    private int crossreferencedEntriesCount; // counts entries pulled in due to crossref

    private static final Log LOGGER = LogFactory.getLog(AuxSubGenerator.class);

    private static final Pattern TAG_PATTERN = Pattern.compile("\\\\(citation|abx@aux@cite)\\{(.+)\\}");

    /**
     * parseAuxFile read the Aux file and fill up some intern data structures. Nested aux files (latex \\include)
     * supported!
     *
     * @param filename String : Path to LatexAuxFile
     * @return boolean, true = no error occurs
     */

    // found at comp.text.tex
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
    private boolean parseAuxFile(String filename) {
        // regular expressions
        Matcher matcher;

        // while condition
        boolean cont;

        // return value -> default: no error
        boolean back = true;


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
                                    mySet.add(dummyStr.trim());
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

        return back;
    }

    /**
     * resolveTags Try to find an equivalent bibtex entry into reference database for all keys (found in aux file). This
     * method will fill up some intern data structures.....
     */
    private void resolveTags() {
        auxDB = new BibDatabase();
        notFoundList.clear();

        // for all bibtex keys (found in aux-file) try to find an equivalent
        // entry into reference database
        for (String str : mySet) {
            BibEntry entry = db.getEntryByKey(str);

            if (entry == null) {
                notFoundList.add(str);
            } else {
                insertEntry(auxDB, entry);
                // Check if the entry we just found references another entry which
                // we don't already have in our list of entries to include. If so,
                // pull in that entry as well:
                entry.getFieldOptional("crossref").ifPresent(crossref -> {
                    if (!mySet.contains(crossref)) {
                        BibEntry refEntry = db.getEntryByKey(crossref);
                        /**
                         * [ 1717849 ] Patch for aux import by Kai Eckert
                         */
                        if (refEntry == null) {
                            notFoundList.add(crossref);
                        } else {
                            insertEntry(auxDB, refEntry);
                            crossreferencedEntriesCount++;
                        }
                    }
                });

            }
        }

        // If we have inserted any entries, make sure to copy the source database's preamble and
        // strings:
        if (auxDB.getEntryCount() > 0) {
            auxDB.setPreamble(db.getPreamble());
            Set<String> keys = db.getStringKeySet();
            for (String key : keys) {
                BibtexString string = db.getString(key);
                auxDB.addString(string);
            }
        }
    }

    /**
     * Insert a clone of the given entry. The clone is given a new unique ID.
     *
     * @param bibDB The database to insert into.
     * @param entry The entry to insert a copy of.
     */
    private void insertEntry(BibDatabase bibDB, BibEntry entry) {

        BibEntry clonedEntry = (BibEntry) entry.clone();
        clonedEntry.setId(IdGenerator.next());
        bibDB.insertEntry(clonedEntry);
    }

    /**
     * generate Shortcut method for easy generation.
     *
     * @param auxFileName String
     * @param bibDB BibDatabase - reference database
     * @return Vector - contains all not resolved bibtex entries
     */
    public final List<String> generate(String auxFileName, BibDatabase bibDB) {
        db = bibDB;
        parseAuxFile(auxFileName);
        resolveTags();

        return notFoundList;
    }

    public BibDatabase getGeneratedDatabase() {
        if (auxDB == null) {
            auxDB = new BibDatabase();
        }

        return auxDB;
    }

    public boolean emptyGeneratedDatabase() {
        if (auxDB == null) {
            return true;
        }
        return auxDB.getEntryCount() == 0;

    }

    public final int getFoundKeysInAux() {
        return mySet.size();
    }

    public final int getResolvedKeysCount() {
        return auxDB.getEntryCount() - crossreferencedEntriesCount;
    }

    public final int getNotResolvedKeysCount() {
        return notFoundList.size();
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
        mySet.clear();
        notFoundList.clear();
        crossreferencedEntriesCount = 0;
        nestedAuxCounter = 0;
        db = null;
        auxDB = null;
    }

    /**
     * returns the number of nested aux files, read by the last call of generate method
     */
    public int getNestedAuxCounter() {
        return this.nestedAuxCounter;
    }

    public String getInformation(boolean includeMissingEntries) {
        StringBuilder result = new StringBuilder();
        // print statistics
        result.append(Localization.lang("keys_in_database")).append(' ').append(db.getEntryCount()).append('\n')
                .append(Localization.lang("found_in_aux_file")).append(' ').append(getFoundKeysInAux()).append('\n')
                .append(Localization.lang("resolved")).append(' ').append(getResolvedKeysCount()).append('\n')
                .append(Localization.lang("not_found")).append(' ').append(getNotResolvedKeysCount()).append('\n')
                .append(Localization.lang("crossreferenced entries included")).append(' ')
                .append(getCrossreferencedEntriesCount()).append('\n');

        if (includeMissingEntries && (getNotResolvedKeysCount() > 0)) {
            for (String entry : notFoundList) {
                result.append(entry).append('\n');
            }
        }
        if (nestedAuxCounter > 0) {
            result.append(Localization.lang("nested_aux_files")).append(' ').append(nestedAuxCounter);
        }
        return result.toString();
    }
}
