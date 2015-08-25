/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref;

import net.sf.jabref.collab.FileUpdateMonitor;
import net.sf.jabref.exporter.AutoSaveManager;
import net.sf.jabref.gui.GlobalFocusListener;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.SidePaneManager;
import net.sf.jabref.gui.help.HelpDialog;
import net.sf.jabref.importer.ImportFormatReader;
import net.sf.jabref.logic.error.StreamEavesdropper;
import net.sf.jabref.logic.journals.JournalAbbreviationRepository;
import net.sf.jabref.logic.logging.CacheableHandler;
import net.sf.jabref.logic.remote.server.RemoteListenerServerLifecycle;
import net.sf.jabref.logic.util.BuildInfo;
import net.sf.jabref.model.entry.BibtexEntryType;
import net.sf.jabref.model.entry.BibtexEntryTypes;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class Globals {
    private static final Log LOGGER = LogFactory.getLog(Globals.class);

    public static final String ENTRYTYPE_FLAG = "jabref-entrytype: ";

    // JabRef version info
    public static final BuildInfo BUILD_INFO = new BuildInfo();
    // Signature written at the top of the .bib file.
    public static final String SIGNATURE = "This file was created with JabRef";
    public static final String encPrefix = "Encoding: ";
    // Newlines
    // will be overridden in initialization due to feature #857 @ JabRef.java
    public static String NEWLINE = System.lineSeparator();

    // Remote listener
    public static RemoteListenerServerLifecycle remoteListener = new RemoteListenerServerLifecycle();

    // journal initialization
    public static final String JOURNALS_FILE_BUILTIN = "/journals/journalList.txt";
    public static final String JOURNALS_IEEE_INTERNAL_LIST = "/journals/IEEEJournalList.txt";

    public static JournalAbbreviationRepository journalAbbrev;

    public static void initializeJournalNames() {
        // Read internal lists:
        Globals.journalAbbrev = new JournalAbbreviationRepository();
        Globals.journalAbbrev.readJournalListFromResource(Globals.JOURNALS_FILE_BUILTIN);
        if (Globals.prefs.getBoolean(JabRefPreferences.USE_IEEE_ABRV)) {
            Globals.journalAbbrev.readJournalListFromResource(JOURNALS_IEEE_INTERNAL_LIST);
        }

        // Read external lists, if any (in reverse order, so the upper lists
        // override the lower):
        String[] lists = Globals.prefs.getStringArray(JabRefPreferences.EXTERNAL_JOURNAL_LISTS);
        if (lists != null && lists.length > 0) {
            for (int i = lists.length - 1; i >= 0; i--) {
                try {
                    Globals.journalAbbrev.readJournalListFromFile(new File(lists[i]));
                } catch (FileNotFoundException e) {
                    // The file couldn't be found... should we tell anyone?
                    LOGGER.info("Cannot find file", e);
                }
            }
        }

        // Read personal list, if set up:
        if (Globals.prefs.get(JabRefPreferences.PERSONAL_JOURNAL_LIST) != null) {
            try {
                Globals.journalAbbrev.readJournalListFromFile(new File(Globals.prefs.get(JabRefPreferences.PERSONAL_JOURNAL_LIST)));
            } catch (FileNotFoundException e) {
                LOGGER.info("Personal journal list file '" + Globals.prefs.get(JabRefPreferences.PERSONAL_JOURNAL_LIST)
                        + "' not found.", e);
            }
        }

    }

    // TODO: other stuff
    public static final ImportFormatReader importFormatReader = new ImportFormatReader();

    public static CacheableHandler handler;

    public static final String FILETYPE_PREFS_EXT = "_dir";
    public static final String SELECTOR_META_PREFIX = "selector_";
    public static final String PROTECTED_FLAG_META = "protectedFlag";
    public static final String NONE = "_non__";
    public static final String FORMATTER_PACKAGE = "net.sf.jabref.exporter.layout.format.";

    // In the main program, this field is initialized in JabRef.java
    // Each test case initializes this field if required
    public static JabRefPreferences prefs;

    public static HelpDialog helpDiag;

    public static SidePaneManager sidePaneManager;

    // "Fieldname" to indicate that a field should be treated as a bibtex string. Used when writing database to file.
    public static final String BIBTEX_STRING = "__string";

    public static final String SPECIAL_COMMAND_CHARS = "\"`^~'c=";

    // Background tasks
    public static GlobalFocusListener focusListener;
    public static FileUpdateMonitor fileUpdateMonitor;
    public static StreamEavesdropper streamEavesdropper;

    public static void startBackgroundTasks() {
        Globals.focusListener = new GlobalFocusListener();

        Globals.streamEavesdropper = StreamEavesdropper.eavesdropOnSystem();

        Globals.fileUpdateMonitor = new FileUpdateMonitor();
        JabRefExecutorService.INSTANCE.executeWithLowPriorityInOwnThread(Globals.fileUpdateMonitor, "FileUpdateMonitor");
    }

    // Autosave manager
    public static AutoSaveManager autoSaveManager;

    public static void startAutoSaveManager(JabRefFrame frame) {
        Globals.autoSaveManager = new AutoSaveManager(frame);
        Globals.autoSaveManager.startAutoSaveTimer();
    }

    // Stop the autosave manager if it has been started
    public static void stopAutoSaveManager() {
        if (Globals.autoSaveManager != null) {
            Globals.autoSaveManager.stopAutoSaveTimer();
            Globals.autoSaveManager.clearAutoSaves();
            Globals.autoSaveManager = null;
        }
    }

    // Get an entry type defined in BibtexEntryType
    public static BibtexEntryType getEntryType(String type) {
        // decide which entryType object to return
        Object o = BibtexEntryType.getType(type);
        if (o != null) {
            return (BibtexEntryType) o;
        } else {
            return BibtexEntryTypes.OTHER;
        }
    }

    // Returns a reg exp pattern in the form (w1)|(w2)| ... wi are escaped if no regex search is enabled
    public static Pattern getPatternForWords(ArrayList<String> words) {
        if (words == null || words.isEmpty() || words.get(0).isEmpty()) {
            return Pattern.compile("");
        }

        boolean regExSearch = Globals.prefs.getBoolean(JabRefPreferences.REG_EXP_SEARCH);

        // compile the words to a regex in the form (w1) | (w2) | (w3)
        String searchPattern = "(".concat(regExSearch ? words.get(0) : Pattern.quote(words.get(0))).concat(")");
        for (int i = 1; i < words.size(); i++) {
            searchPattern = searchPattern.concat("|(").concat(regExSearch ? words.get(i) : Pattern.quote(words.get(i))).concat(")");
        }

        Pattern pattern;
        if (Globals.prefs.getBoolean(JabRefPreferences.CASE_SENSITIVE_SEARCH)) {
            pattern = Pattern.compile(searchPattern);
        } else {
            pattern = Pattern.compile(searchPattern, Pattern.CASE_INSENSITIVE);
        }

        return pattern;
    }
}
