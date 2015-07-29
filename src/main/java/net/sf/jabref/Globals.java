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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.ResourceBundle.Control;
import java.util.regex.Pattern;

import net.sf.jabref.collab.FileUpdateMonitor;
import net.sf.jabref.export.AutoSaveManager;
import net.sf.jabref.help.HelpDialog;
import net.sf.jabref.imports.ImportFormatReader;
import net.sf.jabref.journals.logic.JournalAbbreviationRepository;
import net.sf.jabref.remote.server.RemoteListenerServerLifecycle;
import net.sf.jabref.util.BuildInfo;
import net.sf.jabref.util.error.StreamEavesdropper;
import net.sf.jabref.util.logging.CacheableHandler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Globals {

    /**
     * {@link Control} class allowing properties bundles to be in different encodings.
     * 
     * @see <a
     *      href="http://stackoverflow.com/questions/4659929/how-to-use-utf-8-in-resource-properties-with-resourcebundle">utf-8
     *      and property files</a>
     */
    private static class EncodingControl extends Control {

        private final String encoding;


        public EncodingControl(String encoding) {
            this.encoding = encoding;
        }

        @Override
        public ResourceBundle newBundle(String baseName, Locale locale,
                String format, ClassLoader loader, boolean reload)
                throws IllegalAccessException, InstantiationException,
                IOException {
            // The below is a copy of the default implementation.
            String bundleName = toBundleName(baseName, locale);
            String resourceName = toResourceName(bundleName, "properties");
            ResourceBundle bundle = null;
            InputStream stream = null;
            if (reload) {
                URL url = loader.getResource(resourceName);
                if (url != null) {
                    URLConnection connection = url.openConnection();
                    if (connection != null) {
                        connection.setUseCaches(false);
                        stream = connection.getInputStream();
                    }
                }
            } else {
                stream = loader.getResourceAsStream(resourceName);
            }
            if (stream != null) {
                try {
                    // Only this line is changed to make it to read properties files as UTF-8.
                    bundle = new PropertyResourceBundle(new InputStreamReader(stream, encoding));
                } finally {
                    stream.close();
                }
            }
            return bundle;
        }
    }


    public static RemoteListenerServerLifecycle remoteListener = new RemoteListenerServerLifecycle();

    private static final String RESOURCE_PREFIX = "resource/JabRef", MENU_RESOURCE_PREFIX = "resource/Menu",
            INTEGRITY_RESOURCE_PREFIX = "resource/IntegrityMessage";

    public static final String JOURNALS_FILE_BUILTIN = "/resource/journalList.txt";

    public static final String JOURNALS_IEEE_INTERNAL_LIST = "/resource/IEEEJournalList.txt";

    private static ResourceBundle messages;
    private static ResourceBundle menuTitles;
    private static ResourceBundle intMessages;

    public static FileUpdateMonitor fileUpdateMonitor;

    public static final ImportFormatReader importFormatReader = new ImportFormatReader();

    public static StreamEavesdropper streamEavesdropper;
    public static CacheableHandler handler;

    public static final BuildInfo BUILD_INFO = new BuildInfo();

    public static final String FILETYPE_PREFS_EXT = "_dir", SELECTOR_META_PREFIX = "selector_",
            PROTECTED_FLAG_META = "protectedFlag",
            MAC = "Mac OS X",
            DOI_LOOKUP_PREFIX = "http://dx.doi.org/", NONE = "_non__",
            FORMATTER_PACKAGE = "net.sf.jabref.export.layout.format.";

    public static final String[] ENCODINGS;
    private static final String[] ALL_ENCODINGS = // (String[])
    // Charset.availableCharsets().keySet().toArray(new
    // String[]{});
    new String[] {"ISO8859_1", "UTF8", "UTF-16", "ASCII", "Cp1250", "Cp1251", "Cp1252",
            "Cp1253", "Cp1254", "Cp1257", "SJIS",
            "KOI8_R", // Cyrillic
            "EUC_JP", // Added Japanese encodings.
            "Big5", "Big5_HKSCS", "GBK", "ISO8859_2", "ISO8859_3", "ISO8859_4", "ISO8859_5",
            "ISO8859_6", "ISO8859_7", "ISO8859_8", "ISO8859_9", "ISO8859_13", "ISO8859_15"};
    public static final Map<String, String> ENCODING_NAMES_LOOKUP;

    static {
        // Build list of encodings, by filtering out all that are not supported
        // on this system:
        List<String> encodings = new ArrayList<String>();
        for (String ALL_ENCODING : Globals.ALL_ENCODINGS) {
            if (Charset.isSupported(ALL_ENCODING)) {
                encodings.add(ALL_ENCODING);
            }
        }
        ENCODINGS = encodings.toArray(new String[encodings.size()]);
        // Build a map for translating Java encoding names into common encoding names:
        ENCODING_NAMES_LOOKUP = new HashMap<String, String>();
        Globals.ENCODING_NAMES_LOOKUP.put("Cp1250", "windows-1250");
        Globals.ENCODING_NAMES_LOOKUP.put("Cp1251", "windows-1251");
        Globals.ENCODING_NAMES_LOOKUP.put("Cp1252", "windows-1252");
        Globals.ENCODING_NAMES_LOOKUP.put("Cp1253", "windows-1253");
        Globals.ENCODING_NAMES_LOOKUP.put("Cp1254", "windows-1254");
        Globals.ENCODING_NAMES_LOOKUP.put("Cp1257", "windows-1257");
        Globals.ENCODING_NAMES_LOOKUP.put("ISO8859_1", "ISO-8859-1");
        Globals.ENCODING_NAMES_LOOKUP.put("ISO8859_2", "ISO-8859-2");
        Globals.ENCODING_NAMES_LOOKUP.put("ISO8859_3", "ISO-8859-3");
        Globals.ENCODING_NAMES_LOOKUP.put("ISO8859_4", "ISO-8859-4");
        Globals.ENCODING_NAMES_LOOKUP.put("ISO8859_5", "ISO-8859-5");
        Globals.ENCODING_NAMES_LOOKUP.put("ISO8859_6", "ISO-8859-6");
        Globals.ENCODING_NAMES_LOOKUP.put("ISO8859_7", "ISO-8859-7");
        Globals.ENCODING_NAMES_LOOKUP.put("ISO8859_8", "ISO-8859-8");
        Globals.ENCODING_NAMES_LOOKUP.put("ISO8859_9", "ISO-8859-9");
        Globals.ENCODING_NAMES_LOOKUP.put("ISO8859_13", "ISO-8859-13");
        Globals.ENCODING_NAMES_LOOKUP.put("ISO8859_15", "ISO-8859-15");
        Globals.ENCODING_NAMES_LOOKUP.put("KOI8_R", "KOI8-R");
        Globals.ENCODING_NAMES_LOOKUP.put("UTF8", "UTF-8");
        Globals.ENCODING_NAMES_LOOKUP.put("UTF-16", "UTF-16");
        Globals.ENCODING_NAMES_LOOKUP.put("SJIS", "Shift_JIS");
        Globals.ENCODING_NAMES_LOOKUP.put("GBK", "GBK");
        Globals.ENCODING_NAMES_LOOKUP.put("Big5_HKSCS", "Big5-HKSCS");
        Globals.ENCODING_NAMES_LOOKUP.put("Big5", "Big5");
        Globals.ENCODING_NAMES_LOOKUP.put("EUC_JP", "EUC-JP");
        Globals.ENCODING_NAMES_LOOKUP.put("ASCII", "US-ASCII");
    }

    public static GlobalFocusListener focusListener;

    public static AutoSaveManager autoSaveManager = null;

    // In the main program, this field is initialized in JabRef.java
    // Each test case initializes this field if required
    public static JabRefPreferences prefs = null;

    public static HelpDialog helpDiag = null;

    public static final String osName = System.getProperty("os.name", "def");

    public static final boolean ON_MAC = (Globals.osName.equals(Globals.MAC)),
            ON_WIN = Globals.osName.startsWith("Windows"),
            ON_LINUX = Globals.osName.startsWith("Linux");

    public static SidePaneManager sidePaneManager;

    // will be overridden in initialization due to feature #857
    public static String NEWLINE = System.getProperty("line.separator");
    public static int NEWLINE_LENGTH = Globals.NEWLINE.length();

    // Instantiate logger:
    private static final Log LOGGER = LogFactory.getLog(Globals.class);

    public static JournalAbbreviationRepository journalAbbrev;

    /**
     * "Fieldname" to indicate that a field should be treated as a bibtex string. Used when writing database to file.
     */
    public static final String BIBTEX_STRING = "__string";


    public static void startBackgroundTasks() {
        Globals.focusListener = new GlobalFocusListener();

        Globals.streamEavesdropper = StreamEavesdropper.eavesdropOnSystem();

        Globals.fileUpdateMonitor = new FileUpdateMonitor();
        JabRefExecutorService.INSTANCE.executeWithLowPriorityInOwnThread(Globals.fileUpdateMonitor, "FileUpdateMonitor");
    }

    /**
     * Initialize and start the autosave manager.
     * 
     * @param frame The main frame.
     */
    public static void startAutoSaveManager(JabRefFrame frame) {
        Globals.autoSaveManager = new AutoSaveManager(frame);
        Globals.autoSaveManager.startAutoSaveTimer();
    }

    /**
     * Stop the autosave manager if it has been started.
     */
    public static void stopAutoSaveManager() {
        if (Globals.autoSaveManager != null) {
            Globals.autoSaveManager.stopAutoSaveTimer();
            Globals.autoSaveManager.clearAutoSaves();
            Globals.autoSaveManager = null;
        }
    }

    public static void setLanguage(String language, String country) {
        Locale locale = new Locale(language, country);
        Globals.messages = ResourceBundle.getBundle(Globals.RESOURCE_PREFIX, locale, new EncodingControl("UTF-8"));
        Globals.menuTitles = ResourceBundle.getBundle(Globals.MENU_RESOURCE_PREFIX, locale, new EncodingControl("UTF-8"));
        Globals.intMessages = ResourceBundle.getBundle(Globals.INTEGRITY_RESOURCE_PREFIX, locale, new EncodingControl("UTF-8"));
        Locale.setDefault(locale);
        javax.swing.JComponent.setDefaultLocale(locale);
    }

    public static String lang(String key, String... params) {
        String translation = null;
        try {
            if (Globals.messages != null) {
                translation = Globals.messages.getString(key.replaceAll(" ", "_"));
            }
        } catch (MissingResourceException ex) {
            //logger("Warning: could not get translation for \"" + key + "\"");
        }
        if (translation == null) {
            translation = key;
        }

        if ((translation != null) && (!translation.isEmpty())) {
            translation = translation.replaceAll("_", " ");
            StringBuffer sb = new StringBuffer();
            boolean b = false;
            char c;
            for (int i = 0; i < translation.length(); ++i) {
                c = translation.charAt(i);
                if (c == '%') {
                    b = true;
                } else {
                    if (!b) {
                        sb.append(c);
                    } else {
                        b = false;
                        try {
                            int index = Integer.parseInt(String.valueOf(c));
                            if ((params != null) && (index >= 0) && (index <= params.length)) {
                                sb.append(params[index]);
                            }
                        } catch (NumberFormatException e) {
                            // append literally (for quoting) or insert special
                            // symbol
                            switch (c) {
                            case 'c': // colon
                                sb.append(':');
                                break;
                            case 'e': // equal
                                sb.append('=');
                                break;
                            default: // anything else, e.g. %
                                sb.append(c);
                            }
                        }
                    }
                }
            }
            return sb.toString();
        }
        return key;
    }

    public static String lang(String key) {
        return Globals.lang(key, (String[]) null);
    }

    public static String menuTitle(String key) {
        String translation = null;
        try {
            if (Globals.messages != null) {
                translation = Globals.menuTitles.getString(key.replaceAll(" ", "_"));
            }
        } catch (MissingResourceException ex) {
            translation = key;
        }
        if ((translation != null) && (!translation.isEmpty())) {
            return translation.replaceAll("_", " ");
        } else {
            return key;
        }
    }

    public static String getIntegrityMessage(String key) {
        String translation = null;
        try {
            if (Globals.intMessages != null) {
                translation = Globals.intMessages.getString(key);
            }
        } catch (MissingResourceException ex) {
            translation = key;

            // System.err.println("Warning: could not get menu item translation
            // for \""
            // + key + "\"");
        }
        if ((translation != null) && (!translation.isEmpty())) {
            return translation;
        } else {
            return key;
        }
    }

    // ============================================================
    // Using the hashmap of entry types found in BibtexEntryType
    // ============================================================
    public static BibtexEntryType getEntryType(String type) {
        // decide which entryType object to return
        Object o = BibtexEntryType.ALL_TYPES.get(type);
        if (o != null) {
            return (BibtexEntryType) o;
        } else {
            return BibtexEntryType.OTHER;
        }
        /*
         * if(type.equals("article")) return BibtexEntryType.ARTICLE; else
         * if(type.equals("book")) return BibtexEntryType.BOOK; else
         * if(type.equals("inproceedings")) return
         * BibtexEntryType.INPROCEEDINGS;
         */
    }


    public static final String SPECIAL_COMMAND_CHARS = "\"`^~'c=";

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
        if ((lists != null) && (lists.length > 0)) {
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

    /**
     * Returns a reg exp pattern in the form (w1)|(w2)| ... wi are escaped if no regex search is enabled
     */
    public static Pattern getPatternForWords(ArrayList<String> words) {
        if ((words == null) || (words.isEmpty()) || (words.get(0).isEmpty())) {
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
