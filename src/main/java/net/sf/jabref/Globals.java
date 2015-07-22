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

import java.awt.Toolkit;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.*;
import java.util.ResourceBundle.Control;
import java.util.logging.ConsoleHandler;
import java.util.logging.Filter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import net.sf.jabref.collab.FileUpdateMonitor;
import net.sf.jabref.export.AutoSaveManager;
import net.sf.jabref.help.HelpDialog;
import net.sf.jabref.imports.ImportFormatReader;
import net.sf.jabref.journals.logic.JournalAbbreviationRepository;
import net.sf.jabref.remote.server.RemoteListenerServerLifecycle;
import net.sf.jabref.util.error.StreamEavesdropper;
import net.sf.jabref.util.BuildInfo;
import net.sf.jabref.util.logging.CachebleHandler;
import net.sf.jabref.util.logging.StdoutConsoleHandler;

public class Globals {


    public static final String JOURNALS_IEEE_INTERNAL_LIST = "/resource/IEEEJournalList.txt";
    public static RemoteListenerServerLifecycle remoteListener = new RemoteListenerServerLifecycle();

    /**
     * {@link Control} class allowing properties bundles to be in different encodings.
     * 
     * @see <a href="http://stackoverflow.com/questions/4659929/how-to-use-utf-8-in-resource-properties-with-resourcebundle">utf-8 and property files</a>
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


    private static int SHORTCUT_MASK = -1;
    public static final int
            FUTURE_YEAR = 2050; // Needs to give a year definitely in the future.
            // Used for guessing the
            // year field when parsing textual data. :-)

            public static int STANDARD_EXPORT_COUNT = 5; // The number of standard export formats.
            public static final int METADATA_LINE_LENGTH = 70; // The line length used to wrap metadata.

    // used at highlighting in preview area. 
    // Color chosen similar to JTextComponent.getSelectionColor(), which is
    // used at highlighting words at the editor 
    public static final String highlightColor = "#3399FF";

    private static final String RESOURCE_PREFIX = "resource/JabRef", MENU_RESOURCE_PREFIX = "resource/Menu",
            INTEGRITY_RESOURCE_PREFIX = "resource/IntegrityMessage";

    public static final String JOURNALS_FILE_BUILTIN = "/resource/journalList.txt";
    /*
     * some extra field definitions
     */
    public static final String additionalFields = "/resource/fields/fields.xml";

    private static ResourceBundle messages;
    private static ResourceBundle menuTitles;
    private static ResourceBundle intMessages;

    public static FileUpdateMonitor fileUpdateMonitor;

    public static final ImportFormatReader importFormatReader = new ImportFormatReader();

    public static StreamEavesdropper streamEavesdropper;
    public static CachebleHandler handler;

    public static final BuildInfo BUILD_INFO = new BuildInfo();

    private static Locale locale;

    public static final String FILETYPE_PREFS_EXT = "_dir", SELECTOR_META_PREFIX = "selector_",
            PROTECTED_FLAG_META = "protectedFlag",
            LAYOUT_PREFIX = "/resource/layout/", MAC = "Mac OS X",
            DOI_LOOKUP_PREFIX = "http://dx.doi.org/", NONE = "_non__",
            ARXIV_LOOKUP_PREFIX = "http://arxiv.org/abs/",
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

    public static final boolean ON_MAC = (Globals.osName.equals(Globals.MAC)), ON_WIN = Globals.osName.startsWith("Windows"),
            ON_LINUX = Globals.osName.startsWith("Linux");

    public static final String[] SKIP_WORDS = {"a", "an", "the", "for", "on", "of"};

    public static final String SEPARATING_CHARS = ";,\n ";
    public static final String SEPARATING_CHARS_NOSPACE = ";,\n";

    public static SidePaneManager sidePaneManager;

    // will be overridden in initialization due to feature #857
    public static String NEWLINE = System.getProperty("line.separator");
    public static int NEWLINE_LENGTH = Globals.NEWLINE.length();

    // Instantiate logger:
    private static final Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    /**
     * true if we have unix newlines
     */
    public static final boolean UNIX_NEWLINE = Globals.NEWLINE.equals("\n");

    /**
     * 	"Fieldname" to indicate that a field should be treated as a bibtex 
     * string. Used when writing database to file.
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

    public static void logger(String s) {
        Globals.logger.info(s);
    }

    public static void turnOffLogging() { // only log exceptions
        Globals.logger.setLevel(java.util.logging.Level.SEVERE);
    }

    /**
     * Should be only called once
     */
    public static void turnOnConsoleLogging() {
        Handler consoleHandler = new ConsoleHandler();
        Globals.logger.addHandler(consoleHandler);
    }

    /**
     * Should be only called once
     */
    public static void turnOnFileLogging() {
        Globals.logger.setLevel(java.util.logging.Level.ALL);
        java.util.logging.Handler handler;
        handler = new ConsoleHandler();
        Globals.logger.addHandler(handler);

        handler.setFilter(new Filter() { // select what gets logged

            @Override
            public boolean isLoggable(LogRecord record) {
                return true;
            }
        });
    }

    public static void setLanguage(String language, String country) {
        Globals.locale = new Locale(language, country);
        Globals.messages = ResourceBundle.getBundle(Globals.RESOURCE_PREFIX, Globals.locale, new EncodingControl("UTF-8"));
        Globals.menuTitles = ResourceBundle.getBundle(Globals.MENU_RESOURCE_PREFIX, Globals.locale, new EncodingControl("UTF-8"));
        Globals.intMessages = ResourceBundle.getBundle(Globals.INTEGRITY_RESOURCE_PREFIX, Globals.locale, new EncodingControl("UTF-8"));
        Locale.setDefault(Globals.locale);
        javax.swing.JComponent.setDefaultLocale(Globals.locale);
    }


    public static JournalAbbreviationRepository journalAbbrev;

    public static String lang(String key, String[] params) {
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

        if ((translation != null) && (translation.length() != 0)) {
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

    public static String lang(String key, String s1) {
        return Globals.lang(key, new String[] {s1});
    }

    public static String lang(String key, String s1, String s2) {
        return Globals.lang(key, new String[] {s1, s2});
    }

    public static String lang(String key, String s1, String s2, String s3) {
        return Globals.lang(key, new String[] {s1, s2, s3});
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
        if ((translation != null) && (translation.length() != 0)) {
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
        if ((translation != null) && (translation.length() != 0)) {
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

    public static final HashMap<String, String> HTMLCHARS = new HashMap<String, String>();
    public static final HashMap<String, String> XML_CHARS = new HashMap<String, String>();
    public static final HashMap<String, String> ASCII2XML_CHARS = new HashMap<String, String>();
    public static final HashMap<String, String> UNICODE_CHARS = new HashMap<String, String>();
    public static final HashMap<String, String> RTFCHARS = new HashMap<String, String>();
    private static final HashMap<String, String> URL_CHARS = new HashMap<String, String>();


    public static int getShortcutMask() {
        if (Globals.SHORTCUT_MASK == -1) {
            try {
                Globals.SHORTCUT_MASK = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
            } catch (Throwable ignored) {

            }
        }

        return Globals.SHORTCUT_MASK;
    }


    static {

        // Special characters in URLs need to be replaced to ensure that the URL
        // opens properly on all platforms:
        Globals.URL_CHARS.put("<", "%3c");
        Globals.URL_CHARS.put(">", "%3e");
        Globals.URL_CHARS.put("(", "%28");
        Globals.URL_CHARS.put(")", "%29");
        Globals.URL_CHARS.put(" ", "%20");
        Globals.URL_CHARS.put("&", "%26");
        Globals.URL_CHARS.put("$", "%24");

        // Following character definitions contributed by Ervin Kolenovic:
        // HTML named entities from #192 - #255 (UNICODE Latin-1)
        Globals.HTMLCHARS.put("`A", "&Agrave;"); // #192
        Globals.HTMLCHARS.put("'A", "&Aacute;"); // #193
        Globals.HTMLCHARS.put("^A", "&Acirc;"); // #194
        Globals.HTMLCHARS.put("~A", "&Atilde;"); // #195
        Globals.HTMLCHARS.put("\"A", "&Auml;"); // #196
        Globals.HTMLCHARS.put("AA", "&Aring;"); // #197
        Globals.HTMLCHARS.put("AE", "&AElig;"); // #198
        Globals.HTMLCHARS.put("cC", "&Ccedil;"); // #199
        Globals.HTMLCHARS.put("`E", "&Egrave;"); // #200
        Globals.HTMLCHARS.put("'E", "&Eacute;"); // #201
        Globals.HTMLCHARS.put("^E", "&Ecirc;"); // #202
        Globals.HTMLCHARS.put("\"E", "&Euml;"); // #203
        Globals.HTMLCHARS.put("`I", "&Igrave;"); // #204
        Globals.HTMLCHARS.put("'I", "&Iacute;"); // #205
        Globals.HTMLCHARS.put("^I", "&Icirc;"); // #206
        Globals.HTMLCHARS.put("\"I", "&Iuml;"); // #207
        Globals.HTMLCHARS.put("DH", "&ETH;"); // #208
        Globals.HTMLCHARS.put("~N", "&Ntilde;"); // #209
        Globals.HTMLCHARS.put("`O", "&Ograve;"); // #210
        Globals.HTMLCHARS.put("'O", "&Oacute;"); // #211
        Globals.HTMLCHARS.put("^O", "&Ocirc;"); // #212
        Globals.HTMLCHARS.put("~O", "&Otilde;"); // #213
        Globals.HTMLCHARS.put("\"O", "&Ouml;"); // #214
        // According to ISO 8859-1 the "\times" symbol should be placed here
        // (#215).
        // Omitting this, because it is a mathematical symbol.
        Globals.HTMLCHARS.put("O", "&Oslash;"); // #216
        Globals.HTMLCHARS.put("`U", "&Ugrave;"); // #217
        Globals.HTMLCHARS.put("'U", "&Uacute;"); // #218
        Globals.HTMLCHARS.put("^U", "&Ucirc;"); // #219
        Globals.HTMLCHARS.put("\"U", "&Uuml;"); // #220
        Globals.HTMLCHARS.put("'Y", "&Yacute;"); // #221
        Globals.HTMLCHARS.put("TH", "&THORN;"); // #222
        Globals.HTMLCHARS.put("ss", "&szlig;"); // #223
        Globals.HTMLCHARS.put("`a", "&agrave;"); // #224
        Globals.HTMLCHARS.put("'a", "&aacute;"); // #225
        Globals.HTMLCHARS.put("^a", "&acirc;"); // #226
        Globals.HTMLCHARS.put("~a", "&atilde;"); // #227
        Globals.HTMLCHARS.put("\"a", "&auml;"); // #228
        Globals.HTMLCHARS.put("aa", "&aring;"); // #229
        Globals.HTMLCHARS.put("ae", "&aelig;"); // #230
        Globals.HTMLCHARS.put("cc", "&ccedil;"); // #231
        Globals.HTMLCHARS.put("`e", "&egrave;"); // #232
        Globals.HTMLCHARS.put("'e", "&eacute;"); // #233
        Globals.HTMLCHARS.put("^e", "&ecirc;"); // #234
        Globals.HTMLCHARS.put("\"e", "&euml;"); // #235
        Globals.HTMLCHARS.put("`i", "&igrave;"); // #236
        Globals.HTMLCHARS.put("'i", "&iacute;"); // #237
        Globals.HTMLCHARS.put("^i", "&icirc;"); // #238
        Globals.HTMLCHARS.put("\"i", "&iuml;"); // #239
        Globals.HTMLCHARS.put("dh", "&eth;"); // #240
        Globals.HTMLCHARS.put("~n", "&ntilde;"); // #241
        Globals.HTMLCHARS.put("`o", "&ograve;"); // #242
        Globals.HTMLCHARS.put("'o", "&oacute;"); // #243
        Globals.HTMLCHARS.put("^o", "&ocirc;"); // #244
        Globals.HTMLCHARS.put("~o", "&otilde;"); // #245
        Globals.HTMLCHARS.put("\"o", "&ouml;"); // #246
        // According to ISO 8859-1 the "\div" symbol should be placed here
        // (#247).
        // Omitting this, because it is a mathematical symbol.
        Globals.HTMLCHARS.put("o", "&oslash;"); // #248
        Globals.HTMLCHARS.put("`u", "&ugrave;"); // #249
        Globals.HTMLCHARS.put("'u", "&uacute;"); // #250
        Globals.HTMLCHARS.put("^u", "&ucirc;"); // #251
        Globals.HTMLCHARS.put("\"u", "&uuml;"); // #252
        Globals.HTMLCHARS.put("'y", "&yacute;"); // #253
        Globals.HTMLCHARS.put("th", "&thorn;"); // #254
        Globals.HTMLCHARS.put("\"y", "&yuml;"); // #255

        // HTML special characters without names (UNICODE Latin Extended-A),
        // indicated by UNICODE number
        Globals.HTMLCHARS.put("=A", "&#256;"); // "Amacr"
        Globals.HTMLCHARS.put("=a", "&#257;"); // "amacr"
        Globals.HTMLCHARS.put("uA", "&#258;"); // "Abreve"
        Globals.HTMLCHARS.put("ua", "&#259;"); // "abreve"
        Globals.HTMLCHARS.put("kA", "&#260;"); // "Aogon"
        Globals.HTMLCHARS.put("ka", "&#261;"); // "aogon"
        Globals.HTMLCHARS.put("'C", "&#262;"); // "Cacute"
        Globals.HTMLCHARS.put("'c", "&#263;"); // "cacute"
        Globals.HTMLCHARS.put("^C", "&#264;"); // "Ccirc"
        Globals.HTMLCHARS.put("^c", "&#265;"); // "ccirc"
        Globals.HTMLCHARS.put(".C", "&#266;"); // "Cdot"
        Globals.HTMLCHARS.put(".c", "&#267;"); // "cdot"
        Globals.HTMLCHARS.put("vC", "&#268;"); // "Ccaron"
        Globals.HTMLCHARS.put("vc", "&#269;"); // "ccaron"
        Globals.HTMLCHARS.put("vD", "&#270;"); // "Dcaron"
        // Symbol #271 (d) has no special Latex command
        Globals.HTMLCHARS.put("DJ", "&#272;"); // "Dstrok"
        Globals.HTMLCHARS.put("dj", "&#273;"); // "dstrok"
        Globals.HTMLCHARS.put("=E", "&#274;"); // "Emacr"
        Globals.HTMLCHARS.put("=e", "&#275;"); // "emacr"
        Globals.HTMLCHARS.put("uE", "&#276;"); // "Ebreve"
        Globals.HTMLCHARS.put("ue", "&#277;"); // "ebreve"
        Globals.HTMLCHARS.put(".E", "&#278;"); // "Edot"
        Globals.HTMLCHARS.put(".e", "&#279;"); // "edot"
        Globals.HTMLCHARS.put("kE", "&#280;"); // "Eogon"
        Globals.HTMLCHARS.put("ke", "&#281;"); // "eogon"
        Globals.HTMLCHARS.put("vE", "&#282;"); // "Ecaron"
        Globals.HTMLCHARS.put("ve", "&#283;"); // "ecaron"
        Globals.HTMLCHARS.put("^G", "&#284;"); // "Gcirc"
        Globals.HTMLCHARS.put("^g", "&#285;"); // "gcirc"
        Globals.HTMLCHARS.put("uG", "&#286;"); // "Gbreve"
        Globals.HTMLCHARS.put("ug", "&#287;"); // "gbreve"
        Globals.HTMLCHARS.put(".G", "&#288;"); // "Gdot"
        Globals.HTMLCHARS.put(".g", "&#289;"); // "gdot"
        Globals.HTMLCHARS.put("cG", "&#290;"); // "Gcedil"
        Globals.HTMLCHARS.put("'g", "&#291;"); // "gacute"
        Globals.HTMLCHARS.put("^H", "&#292;"); // "Hcirc"
        Globals.HTMLCHARS.put("^h", "&#293;"); // "hcirc"
        Globals.HTMLCHARS.put("Hstrok", "&#294;"); // "Hstrok"
        Globals.HTMLCHARS.put("hstrok", "&#295;"); // "hstrok"
        Globals.HTMLCHARS.put("~I", "&#296;"); // "Itilde"
        Globals.HTMLCHARS.put("~i", "&#297;"); // "itilde"
        Globals.HTMLCHARS.put("=I", "&#298;"); // "Imacr"
        Globals.HTMLCHARS.put("=i", "&#299;"); // "imacr"
        Globals.HTMLCHARS.put("uI", "&#300;"); // "Ibreve"
        Globals.HTMLCHARS.put("ui", "&#301;"); // "ibreve"
        Globals.HTMLCHARS.put("kI", "&#302;"); // "Iogon"
        Globals.HTMLCHARS.put("ki", "&#303;"); // "iogon"
        Globals.HTMLCHARS.put(".I", "&#304;"); // "Idot"
        Globals.HTMLCHARS.put("i", "&#305;"); // "inodot"
        // Symbol #306 (IJ) has no special Latex command
        // Symbol #307 (ij) has no special Latex command
        Globals.HTMLCHARS.put("^J", "&#308;"); // "Jcirc"
        Globals.HTMLCHARS.put("^j", "&#309;"); // "jcirc"
        Globals.HTMLCHARS.put("cK", "&#310;"); // "Kcedil"
        Globals.HTMLCHARS.put("ck", "&#311;"); // "kcedil"
        // Symbol #312 (k) has no special Latex command
        Globals.HTMLCHARS.put("'L", "&#313;"); // "Lacute"
        Globals.HTMLCHARS.put("'l", "&#314;"); // "lacute"
        Globals.HTMLCHARS.put("cL", "&#315;"); // "Lcedil"
        Globals.HTMLCHARS.put("cl", "&#316;"); // "lcedil"
        // Symbol #317 (L) has no special Latex command
        // Symbol #318 (l) has no special Latex command
        Globals.HTMLCHARS.put("Lmidot", "&#319;"); // "Lmidot"
        Globals.HTMLCHARS.put("lmidot", "&#320;"); // "lmidot"
        Globals.HTMLCHARS.put("L", "&#321;"); // "Lstrok"
        Globals.HTMLCHARS.put("l", "&#322;"); // "lstrok"
        Globals.HTMLCHARS.put("'N", "&#323;"); // "Nacute"
        Globals.HTMLCHARS.put("'n", "&#324;"); // "nacute"
        Globals.HTMLCHARS.put("cN", "&#325;"); // "Ncedil"
        Globals.HTMLCHARS.put("cn", "&#326;"); // "ncedil"
        Globals.HTMLCHARS.put("vN", "&#327;"); // "Ncaron"
        Globals.HTMLCHARS.put("vn", "&#328;"); // "ncaron"
        // Symbol #329 (n) has no special Latex command
        Globals.HTMLCHARS.put("NG", "&#330;"); // "ENG"
        Globals.HTMLCHARS.put("ng", "&#331;"); // "eng"
        Globals.HTMLCHARS.put("=O", "&#332;"); // "Omacr"
        Globals.HTMLCHARS.put("=o", "&#333;"); // "omacr"
        Globals.HTMLCHARS.put("uO", "&#334;"); // "Obreve"
        Globals.HTMLCHARS.put("uo", "&#335;"); // "obreve"
        Globals.HTMLCHARS.put("HO", "&#336;"); // "Odblac"
        Globals.HTMLCHARS.put("Ho", "&#337;"); // "odblac"
        Globals.HTMLCHARS.put("OE", "&#338;"); // "OElig"
        Globals.HTMLCHARS.put("oe", "&#339;"); // "oelig"
        Globals.HTMLCHARS.put("'R", "&#340;"); // "Racute"
        Globals.HTMLCHARS.put("'r", "&#341;"); // "racute"
        Globals.HTMLCHARS.put("cR", "&#342;"); // "Rcedil"
        Globals.HTMLCHARS.put("cr", "&#343;"); // "rcedil"
        Globals.HTMLCHARS.put("vR", "&#344;"); // "Rcaron"
        Globals.HTMLCHARS.put("vr", "&#345;"); // "rcaron"
        Globals.HTMLCHARS.put("'S", "&#346;"); // "Sacute"
        Globals.HTMLCHARS.put("'s", "&#347;"); // "sacute"
        Globals.HTMLCHARS.put("^S", "&#348;"); // "Scirc"
        Globals.HTMLCHARS.put("^s", "&#349;"); // "scirc"
        Globals.HTMLCHARS.put("cS", "&#350;"); // "Scedil"
        Globals.HTMLCHARS.put("cs", "&#351;"); // "scedil"
        Globals.HTMLCHARS.put("vS", "&#352;"); // "Scaron"
        Globals.HTMLCHARS.put("vs", "&#353;"); // "scaron"
        Globals.HTMLCHARS.put("cT", "&#354;"); // "Tcedil"
        Globals.HTMLCHARS.put("ct", "&#355;"); // "tcedil"
        Globals.HTMLCHARS.put("vT", "&#356;"); // "Tcaron"
        // Symbol #357 (t) has no special Latex command
        Globals.HTMLCHARS.put("Tstrok", "&#358;"); // "Tstrok"
        Globals.HTMLCHARS.put("tstrok", "&#359;"); // "tstrok"
        Globals.HTMLCHARS.put("~U", "&#360;"); // "Utilde"
        Globals.HTMLCHARS.put("~u", "&#361;"); // "utilde"
        Globals.HTMLCHARS.put("=U", "&#362;"); // "Umacr"
        Globals.HTMLCHARS.put("=u", "&#363;"); // "umacr"
        Globals.HTMLCHARS.put("uU", "&#364;"); // "Ubreve"
        Globals.HTMLCHARS.put("uu", "&#365;"); // "ubreve"
        Globals.HTMLCHARS.put("rU", "&#366;"); // "Uring"
        Globals.HTMLCHARS.put("ru", "&#367;"); // "uring"
        Globals.HTMLCHARS.put("HU", "&#368;"); // "Odblac"
        Globals.HTMLCHARS.put("Hu", "&#369;"); // "odblac"
        Globals.HTMLCHARS.put("kU", "&#370;"); // "Uogon"
        Globals.HTMLCHARS.put("ku", "&#371;"); // "uogon"
        Globals.HTMLCHARS.put("^W", "&#372;"); // "Wcirc"
        Globals.HTMLCHARS.put("^w", "&#373;"); // "wcirc"
        Globals.HTMLCHARS.put("^Y", "&#374;"); // "Ycirc"
        Globals.HTMLCHARS.put("^y", "&#375;"); // "ycirc"
        Globals.HTMLCHARS.put("\"Y", "&#376;"); // "Yuml"
        Globals.HTMLCHARS.put("'Z", "&#377;"); // "Zacute"
        Globals.HTMLCHARS.put("'z", "&#378;"); // "zacute"
        Globals.HTMLCHARS.put(".Z", "&#379;"); // "Zdot"
        Globals.HTMLCHARS.put(".z", "&#380;"); // "zdot"
        Globals.HTMLCHARS.put("vZ", "&#381;"); // "Zcaron"
        Globals.HTMLCHARS.put("vz", "&#382;"); // "zcaron"
        // Symbol #383 (f) has no special Latex command
        Globals.HTMLCHARS.put("%", "%"); // percent sign

        Globals.XML_CHARS.put("\\{\\\\\\\"\\{a\\}\\}", "&#x00E4;");
        Globals.XML_CHARS.put("\\{\\\\\\\"\\{A\\}\\}", "&#x00C4;");
        Globals.XML_CHARS.put("\\{\\\\\\\"\\{e\\}\\}", "&#x00EB;");
        Globals.XML_CHARS.put("\\{\\\\\\\"\\{E\\}\\}", "&#x00CB;");
        Globals.XML_CHARS.put("\\{\\\\\\\"\\{i\\}\\}", "&#x00EF;");
        Globals.XML_CHARS.put("\\{\\\\\\\"\\{I\\}\\}", "&#x00CF;");
        Globals.XML_CHARS.put("\\{\\\\\\\"\\{o\\}\\}", "&#x00F6;");
        Globals.XML_CHARS.put("\\{\\\\\\\"\\{O\\}\\}", "&#x00D6;");
        Globals.XML_CHARS.put("\\{\\\\\\\"\\{u\\}\\}", "&#x00FC;");
        Globals.XML_CHARS.put("\\{\\\\\\\"\\{U\\}\\}", "&#x00DC;");

        //next 2 rows were missing...
        Globals.XML_CHARS.put("\\{\\\\\\`\\{a\\}\\}", "&#x00E0;");
        Globals.XML_CHARS.put("\\{\\\\\\`\\{A\\}\\}", "&#x00C0;");

        Globals.XML_CHARS.put("\\{\\\\\\`\\{e\\}\\}", "&#x00E8;");
        Globals.XML_CHARS.put("\\{\\\\\\`\\{E\\}\\}", "&#x00C8;");
        Globals.XML_CHARS.put("\\{\\\\\\`\\{i\\}\\}", "&#x00EC;");
        Globals.XML_CHARS.put("\\{\\\\\\`\\{I\\}\\}", "&#x00CC;");
        Globals.XML_CHARS.put("\\{\\\\\\`\\{o\\}\\}", "&#x00F2;");
        Globals.XML_CHARS.put("\\{\\\\\\`\\{O\\}\\}", "&#x00D2;");
        Globals.XML_CHARS.put("\\{\\\\\\`\\{u\\}\\}", "&#x00F9;");
        Globals.XML_CHARS.put("\\{\\\\\\`\\{U\\}\\}", "&#x00D9;");

        //corrected these 10 lines below...
        Globals.XML_CHARS.put("\\{\\\\\\'\\{a\\}\\}", "&#x00E1;");
        Globals.XML_CHARS.put("\\{\\\\\\'\\{A\\}\\}", "&#x00C1;");
        Globals.XML_CHARS.put("\\{\\\\\\'\\{e\\}\\}", "&#x00E9;");
        Globals.XML_CHARS.put("\\{\\\\\\'\\{E\\}\\}", "&#x00C9;");
        Globals.XML_CHARS.put("\\{\\\\\\'\\{i\\}\\}", "&#x00ED;");
        Globals.XML_CHARS.put("\\{\\\\\\'\\{I\\}\\}", "&#x00CD;");
        Globals.XML_CHARS.put("\\{\\\\\\'\\{o\\}\\}", "&#x00F3;");
        Globals.XML_CHARS.put("\\{\\\\\\'\\{O\\}\\}", "&#x00D3;");
        Globals.XML_CHARS.put("\\{\\\\\\'\\{u\\}\\}", "&#x00FA;");
        Globals.XML_CHARS.put("\\{\\\\\\'\\{U\\}\\}", "&#x00DA;");
        //added next four chars...
        Globals.XML_CHARS.put("\\{\\\\\\'\\{c\\}\\}", "&#x0107;");
        Globals.XML_CHARS.put("\\{\\\\\\'\\{C\\}\\}", "&#x0106;");
        Globals.XML_CHARS.put("\\{\\\\c\\{c\\}\\}", "&#x00E7;");
        Globals.XML_CHARS.put("\\{\\\\c\\{C\\}\\}", "&#x00C7;");

        Globals.XML_CHARS.put("\\{\\\\\\\uFFFD\\{E\\}\\}", "&#x00C9;");
        Globals.XML_CHARS.put("\\{\\\\\\\uFFFD\\{i\\}\\}", "&#x00ED;");
        Globals.XML_CHARS.put("\\{\\\\\\\uFFFD\\{I\\}\\}", "&#x00CD;");
        Globals.XML_CHARS.put("\\{\\\\\\\uFFFD\\{o\\}\\}", "&#x00F3;");
        Globals.XML_CHARS.put("\\{\\\\\\\uFFFD\\{O\\}\\}", "&#x00D3;");
        Globals.XML_CHARS.put("\\{\\\\\\\uFFFD\\{u\\}\\}", "&#x00FA;");
        Globals.XML_CHARS.put("\\{\\\\\\\uFFFD\\{U\\}\\}", "&#x00DA;");
        Globals.XML_CHARS.put("\\{\\\\\\\uFFFD\\{a\\}\\}", "&#x00E1;");
        Globals.XML_CHARS.put("\\{\\\\\\\uFFFD\\{A\\}\\}", "&#x00C1;");

        //next 2 rows were missing...
        Globals.XML_CHARS.put("\\{\\\\\\^\\{a\\}\\}", "&#x00E2;");
        Globals.XML_CHARS.put("\\{\\\\\\^\\{A\\}\\}", "&#x00C2;");

        Globals.XML_CHARS.put("\\{\\\\\\^\\{o\\}\\}", "&#x00F4;");
        Globals.XML_CHARS.put("\\{\\\\\\^\\{O\\}\\}", "&#x00D4;");
        Globals.XML_CHARS.put("\\{\\\\\\^\\{u\\}\\}", "&#x00F9;");
        Globals.XML_CHARS.put("\\{\\\\\\^\\{U\\}\\}", "&#x00D9;");
        Globals.XML_CHARS.put("\\{\\\\\\^\\{e\\}\\}", "&#x00EA;");
        Globals.XML_CHARS.put("\\{\\\\\\^\\{E\\}\\}", "&#x00CA;");
        Globals.XML_CHARS.put("\\{\\\\\\^\\{i\\}\\}", "&#x00EE;");
        Globals.XML_CHARS.put("\\{\\\\\\^\\{I\\}\\}", "&#x00CE;");

        Globals.XML_CHARS.put("\\{\\\\\\~\\{o\\}\\}", "&#x00F5;");
        Globals.XML_CHARS.put("\\{\\\\\\~\\{O\\}\\}", "&#x00D5;");
        Globals.XML_CHARS.put("\\{\\\\\\~\\{n\\}\\}", "&#x00F1;");
        Globals.XML_CHARS.put("\\{\\\\\\~\\{N\\}\\}", "&#x00D1;");
        Globals.XML_CHARS.put("\\{\\\\\\~\\{a\\}\\}", "&#x00E3;");
        Globals.XML_CHARS.put("\\{\\\\\\~\\{A\\}\\}", "&#x00C3;");

        Globals.XML_CHARS.put("\\{\\\\\\\"a\\}", "&#x00E4;");
        Globals.XML_CHARS.put("\\{\\\\\\\"A\\}", "&#x00C4;");
        Globals.XML_CHARS.put("\\{\\\\\\\"e\\}", "&#x00EB;");
        Globals.XML_CHARS.put("\\{\\\\\\\"E\\}", "&#x00CB;");
        Globals.XML_CHARS.put("\\{\\\\\\\"i\\}", "&#x00EF;");
        Globals.XML_CHARS.put("\\{\\\\\\\"I\\}", "&#x00CF;");
        Globals.XML_CHARS.put("\\{\\\\\\\"o\\}", "&#x00F6;");
        Globals.XML_CHARS.put("\\{\\\\\\\"O\\}", "&#x00D6;");
        Globals.XML_CHARS.put("\\{\\\\\\\"u\\}", "&#x00FC;");
        Globals.XML_CHARS.put("\\{\\\\\\\"U\\}", "&#x00DC;");

        //next 2 rows were missing...
        Globals.XML_CHARS.put("\\{\\\\\\`a\\}", "&#x00E0;");
        Globals.XML_CHARS.put("\\{\\\\\\`A\\}", "&#x00C0;");

        Globals.XML_CHARS.put("\\{\\\\\\`e\\}", "&#x00E8;");
        Globals.XML_CHARS.put("\\{\\\\\\`E\\}", "&#x00C8;");
        Globals.XML_CHARS.put("\\{\\\\\\`i\\}", "&#x00EC;");
        Globals.XML_CHARS.put("\\{\\\\\\`I\\}", "&#x00CC;");
        Globals.XML_CHARS.put("\\{\\\\\\`o\\}", "&#x00F2;");
        Globals.XML_CHARS.put("\\{\\\\\\`O\\}", "&#x00D2;");
        Globals.XML_CHARS.put("\\{\\\\\\`u\\}", "&#x00F9;");
        Globals.XML_CHARS.put("\\{\\\\\\`U\\}", "&#x00D9;");
        Globals.XML_CHARS.put("\\{\\\\\\'e\\}", "&#x00E9;");
        Globals.XML_CHARS.put("\\{\\\\\\'E\\}", "&#x00C9;");
        Globals.XML_CHARS.put("\\{\\\\\\'i\\}", "&#x00ED;");
        Globals.XML_CHARS.put("\\{\\\\\\'I\\}", "&#x00CD;");
        Globals.XML_CHARS.put("\\{\\\\\\'o\\}", "&#x00F3;");
        Globals.XML_CHARS.put("\\{\\\\\\'O\\}", "&#x00D3;");
        Globals.XML_CHARS.put("\\{\\\\\\'u\\}", "&#x00FA;");
        Globals.XML_CHARS.put("\\{\\\\\\'U\\}", "&#x00DA;");
        Globals.XML_CHARS.put("\\{\\\\\\'a\\}", "&#x00E1;");
        Globals.XML_CHARS.put("\\{\\\\\\'A\\}", "&#x00C1;");
        //added next two chars...
        Globals.XML_CHARS.put("\\{\\\\\\'c\\}", "&#x0107;");
        Globals.XML_CHARS.put("\\{\\\\\\'C\\}", "&#x0106;");

        //next two lines were wrong...
        Globals.XML_CHARS.put("\\{\\\\\\^a\\}", "&#x00E2;");
        Globals.XML_CHARS.put("\\{\\\\\\^A\\}", "&#x00C2;");

        Globals.XML_CHARS.put("\\{\\\\\\^o\\}", "&#x00F4;");
        Globals.XML_CHARS.put("\\{\\\\\\^O\\}", "&#x00D4;");
        Globals.XML_CHARS.put("\\{\\\\\\^u\\}", "&#x00F9;");
        Globals.XML_CHARS.put("\\{\\\\\\^U\\}", "&#x00D9;");
        Globals.XML_CHARS.put("\\{\\\\\\^e\\}", "&#x00EA;");
        Globals.XML_CHARS.put("\\{\\\\\\^E\\}", "&#x00CA;");
        Globals.XML_CHARS.put("\\{\\\\\\^i\\}", "&#x00EE;");
        Globals.XML_CHARS.put("\\{\\\\\\^I\\}", "&#x00CE;");
        Globals.XML_CHARS.put("\\{\\\\\\~o\\}", "&#x00F5;");
        Globals.XML_CHARS.put("\\{\\\\\\~O\\}", "&#x00D5;");
        Globals.XML_CHARS.put("\\{\\\\\\~n\\}", "&#x00F1;");
        Globals.XML_CHARS.put("\\{\\\\\\~N\\}", "&#x00D1;");
        Globals.XML_CHARS.put("\\{\\\\\\~a\\}", "&#x00E3;");
        Globals.XML_CHARS.put("\\{\\\\\\~A\\}", "&#x00C3;");

        Globals.ASCII2XML_CHARS.put("<", "&lt;");
        Globals.ASCII2XML_CHARS.put("\"", "&quot;");
        Globals.ASCII2XML_CHARS.put(">", "&gt;");

        Globals.UNICODE_CHARS.put("\u00C0", "A");
        Globals.UNICODE_CHARS.put("\u00C1", "A");
        Globals.UNICODE_CHARS.put("\u00C2", "A");
        Globals.UNICODE_CHARS.put("\u00C3", "A");
        Globals.UNICODE_CHARS.put("\u00C4", "Ae");
        Globals.UNICODE_CHARS.put("\u00C5", "Aa");
        Globals.UNICODE_CHARS.put("\u00C6", "Ae");
        Globals.UNICODE_CHARS.put("\u00C7", "C");
        Globals.UNICODE_CHARS.put("\u00C8", "E");
        Globals.UNICODE_CHARS.put("\u00C9", "E");
        Globals.UNICODE_CHARS.put("\u00CA", "E");
        Globals.UNICODE_CHARS.put("\u00CB", "E");
        Globals.UNICODE_CHARS.put("\u00CC", "I");
        Globals.UNICODE_CHARS.put("\u00CD", "I");
        Globals.UNICODE_CHARS.put("\u00CE", "I");
        Globals.UNICODE_CHARS.put("\u00CF", "I");
        Globals.UNICODE_CHARS.put("\u00D0", "D");
        Globals.UNICODE_CHARS.put("\u00D1", "N");
        Globals.UNICODE_CHARS.put("\u00D2", "O");
        Globals.UNICODE_CHARS.put("\u00D3", "O");
        Globals.UNICODE_CHARS.put("\u00D4", "O");
        Globals.UNICODE_CHARS.put("\u00D5", "O");
        Globals.UNICODE_CHARS.put("\u00D6", "Oe");
        Globals.UNICODE_CHARS.put("\u00D8", "Oe");
        Globals.UNICODE_CHARS.put("\u00D9", "U");
        Globals.UNICODE_CHARS.put("\u00DA", "U");
        Globals.UNICODE_CHARS.put("\u00DB", "U");
        Globals.UNICODE_CHARS.put("\u00DC", "Ue"); // U umlaut ..
        Globals.UNICODE_CHARS.put("\u00DD", "Y");
        Globals.UNICODE_CHARS.put("\u00DF", "ss");
        Globals.UNICODE_CHARS.put("\u00E0", "a");
        Globals.UNICODE_CHARS.put("\u00E1", "a");
        Globals.UNICODE_CHARS.put("\u00E2", "a");
        Globals.UNICODE_CHARS.put("\u00E3", "a");
        Globals.UNICODE_CHARS.put("\u00E4", "ae");
        Globals.UNICODE_CHARS.put("\u00E5", "aa");
        Globals.UNICODE_CHARS.put("\u00E6", "ae");
        Globals.UNICODE_CHARS.put("\u00E7", "c");
        Globals.UNICODE_CHARS.put("\u00E8", "e");
        Globals.UNICODE_CHARS.put("\u00E9", "e");
        Globals.UNICODE_CHARS.put("\u00EA", "e");
        Globals.UNICODE_CHARS.put("\u00EB", "e");
        Globals.UNICODE_CHARS.put("\u00EC", "i");
        Globals.UNICODE_CHARS.put("\u00ED", "i");
        Globals.UNICODE_CHARS.put("\u00EE", "i");
        Globals.UNICODE_CHARS.put("\u00EF", "i");
        Globals.UNICODE_CHARS.put("\u00F0", "o");
        Globals.UNICODE_CHARS.put("\u00F1", "n");
        Globals.UNICODE_CHARS.put("\u00F2", "o");
        Globals.UNICODE_CHARS.put("\u00F3", "o");
        Globals.UNICODE_CHARS.put("\u00F4", "o");
        Globals.UNICODE_CHARS.put("\u00F5", "o");
        Globals.UNICODE_CHARS.put("\u00F6", "oe");
        Globals.UNICODE_CHARS.put("\u00F8", "oe");
        Globals.UNICODE_CHARS.put("\u00F9", "u");
        Globals.UNICODE_CHARS.put("\u00FA", "u");
        Globals.UNICODE_CHARS.put("\u00FB", "u");
        Globals.UNICODE_CHARS.put("\u00FC", "ue"); // u umlaut...
        Globals.UNICODE_CHARS.put("\u00FD", "y");
        Globals.UNICODE_CHARS.put("\u00FF", "y");
        Globals.UNICODE_CHARS.put("\u0100", "A");
        Globals.UNICODE_CHARS.put("\u0101", "a");
        Globals.UNICODE_CHARS.put("\u0102", "A");
        Globals.UNICODE_CHARS.put("\u0103", "a");
        Globals.UNICODE_CHARS.put("\u0104", "A");
        Globals.UNICODE_CHARS.put("\u0105", "a");
        Globals.UNICODE_CHARS.put("\u0106", "C");
        Globals.UNICODE_CHARS.put("\u0107", "c");
        Globals.UNICODE_CHARS.put("\u0108", "C");
        Globals.UNICODE_CHARS.put("\u0109", "c");
        Globals.UNICODE_CHARS.put("\u010A", "C");
        Globals.UNICODE_CHARS.put("\u010B", "c");
        Globals.UNICODE_CHARS.put("\u010C", "C");
        Globals.UNICODE_CHARS.put("\u010D", "c");
        Globals.UNICODE_CHARS.put("\u010E", "D");
        Globals.UNICODE_CHARS.put("\u010F", "d");
        Globals.UNICODE_CHARS.put("\u0110", "D");
        Globals.UNICODE_CHARS.put("\u0111", "d");
        Globals.UNICODE_CHARS.put("\u0112", "E");
        Globals.UNICODE_CHARS.put("\u0113", "e");
        Globals.UNICODE_CHARS.put("\u0114", "E");
        Globals.UNICODE_CHARS.put("\u0115", "e");
        Globals.UNICODE_CHARS.put("\u0116", "E");
        Globals.UNICODE_CHARS.put("\u0117", "e");
        Globals.UNICODE_CHARS.put("\u0118", "E");
        Globals.UNICODE_CHARS.put("\u0119", "e");
        Globals.UNICODE_CHARS.put("\u011A", "E");
        Globals.UNICODE_CHARS.put("\u011B", "e");
        Globals.UNICODE_CHARS.put("\u011C", "G");
        Globals.UNICODE_CHARS.put("\u011D", "g");
        Globals.UNICODE_CHARS.put("\u011E", "G");
        Globals.UNICODE_CHARS.put("\u011F", "g");
        Globals.UNICODE_CHARS.put("\u0120", "G");
        Globals.UNICODE_CHARS.put("\u0121", "g");
        Globals.UNICODE_CHARS.put("\u0122", "G");
        Globals.UNICODE_CHARS.put("\u0123", "g");
        Globals.UNICODE_CHARS.put("\u0124", "H");
        Globals.UNICODE_CHARS.put("\u0125", "h");
        Globals.UNICODE_CHARS.put("\u0127", "h");
        Globals.UNICODE_CHARS.put("\u0128", "I");
        Globals.UNICODE_CHARS.put("\u0129", "i");
        Globals.UNICODE_CHARS.put("\u012A", "I");
        Globals.UNICODE_CHARS.put("\u012B", "i");
        Globals.UNICODE_CHARS.put("\u012C", "I");
        Globals.UNICODE_CHARS.put("\u012D", "i");
        Globals.UNICODE_CHARS.put("\u012E", "I");
        Globals.UNICODE_CHARS.put("\u012F", "i");
        Globals.UNICODE_CHARS.put("\u0130", "I");
        Globals.UNICODE_CHARS.put("\u0131", "i");
        Globals.UNICODE_CHARS.put("\u0132", "IJ");
        Globals.UNICODE_CHARS.put("\u0133", "ij");
        Globals.UNICODE_CHARS.put("\u0134", "J");
        Globals.UNICODE_CHARS.put("\u0135", "j");
        Globals.UNICODE_CHARS.put("\u0136", "K");
        Globals.UNICODE_CHARS.put("\u0137", "k");
        Globals.UNICODE_CHARS.put("\u0138", "k");
        Globals.UNICODE_CHARS.put("\u0139", "L");
        Globals.UNICODE_CHARS.put("\u013A", "l");
        Globals.UNICODE_CHARS.put("\u013B", "L");
        Globals.UNICODE_CHARS.put("\u013C", "l");
        Globals.UNICODE_CHARS.put("\u013D", "L");
        Globals.UNICODE_CHARS.put("\u013E", "l");
        Globals.UNICODE_CHARS.put("\u013F", "L");
        Globals.UNICODE_CHARS.put("\u0140", "l");
        Globals.UNICODE_CHARS.put("\u0141", "L");
        Globals.UNICODE_CHARS.put("\u0142", "l");
        Globals.UNICODE_CHARS.put("\u0143", "N");
        Globals.UNICODE_CHARS.put("\u0144", "n");
        Globals.UNICODE_CHARS.put("\u0145", "N");
        Globals.UNICODE_CHARS.put("\u0146", "n");
        Globals.UNICODE_CHARS.put("\u0147", "N");
        Globals.UNICODE_CHARS.put("\u0148", "n");
        Globals.UNICODE_CHARS.put("\u0149", "n");
        Globals.UNICODE_CHARS.put("\u014A", "N");
        Globals.UNICODE_CHARS.put("\u014B", "n");
        Globals.UNICODE_CHARS.put("\u014C", "O");
        Globals.UNICODE_CHARS.put("\u014D", "o");
        Globals.UNICODE_CHARS.put("\u014E", "O");
        Globals.UNICODE_CHARS.put("\u014F", "o");
        Globals.UNICODE_CHARS.put("\u0150", "Oe");
        Globals.UNICODE_CHARS.put("\u0151", "oe");
        Globals.UNICODE_CHARS.put("\u0152", "OE");
        Globals.UNICODE_CHARS.put("\u0153", "oe");
        Globals.UNICODE_CHARS.put("\u0154", "R");
        Globals.UNICODE_CHARS.put("\u0155", "r");
        Globals.UNICODE_CHARS.put("\u0156", "R");
        Globals.UNICODE_CHARS.put("\u0157", "r");
        Globals.UNICODE_CHARS.put("\u0158", "R");
        Globals.UNICODE_CHARS.put("\u0159", "r");
        Globals.UNICODE_CHARS.put("\u015A", "S");
        Globals.UNICODE_CHARS.put("\u015B", "s");
        Globals.UNICODE_CHARS.put("\u015C", "S");
        Globals.UNICODE_CHARS.put("\u015D", "s");
        Globals.UNICODE_CHARS.put("\u015E", "S");
        Globals.UNICODE_CHARS.put("\u015F", "s");
        Globals.UNICODE_CHARS.put("\u0160", "S");
        Globals.UNICODE_CHARS.put("\u0161", "s");
        Globals.UNICODE_CHARS.put("\u0162", "T");
        Globals.UNICODE_CHARS.put("\u0163", "t");
        Globals.UNICODE_CHARS.put("\u0164", "T");
        Globals.UNICODE_CHARS.put("\u0165", "t");
        Globals.UNICODE_CHARS.put("\u0166", "T");
        Globals.UNICODE_CHARS.put("\u0167", "t");
        Globals.UNICODE_CHARS.put("\u0168", "U");
        Globals.UNICODE_CHARS.put("\u0169", "u");
        Globals.UNICODE_CHARS.put("\u016A", "U");
        Globals.UNICODE_CHARS.put("\u016B", "u");
        Globals.UNICODE_CHARS.put("\u016C", "U");
        Globals.UNICODE_CHARS.put("\u016D", "u");
        Globals.UNICODE_CHARS.put("\u016E", "UU");
        Globals.UNICODE_CHARS.put("\u016F", "uu");
        Globals.UNICODE_CHARS.put("\u0170", "Ue");
        Globals.UNICODE_CHARS.put("\u0171", "ue");
        Globals.UNICODE_CHARS.put("\u0172", "U");
        Globals.UNICODE_CHARS.put("\u0173", "u");
        Globals.UNICODE_CHARS.put("\u0174", "W");
        Globals.UNICODE_CHARS.put("\u0175", "w");
        Globals.UNICODE_CHARS.put("\u0176", "Y");
        Globals.UNICODE_CHARS.put("\u0177", "y");
        Globals.UNICODE_CHARS.put("\u0178", "Y");
        Globals.UNICODE_CHARS.put("\u0179", "Z");
        Globals.UNICODE_CHARS.put("\u017A", "z");
        Globals.UNICODE_CHARS.put("\u017B", "Z");
        Globals.UNICODE_CHARS.put("\u017C", "z");
        Globals.UNICODE_CHARS.put("\u017D", "Z");
        Globals.UNICODE_CHARS.put("\u017E", "z");
        Globals.UNICODE_CHARS.put("\u1EBC", "E");
        Globals.UNICODE_CHARS.put("\u1EBD", "e");
        Globals.UNICODE_CHARS.put("\u1EF8", "Y");
        Globals.UNICODE_CHARS.put("\u1EF9", "y");
        Globals.UNICODE_CHARS.put("\u01CD", "A");
        Globals.UNICODE_CHARS.put("\u01CE", "a");
        Globals.UNICODE_CHARS.put("\u01CF", "I");
        Globals.UNICODE_CHARS.put("\u01D0", "i");
        Globals.UNICODE_CHARS.put("\u01D1", "O");
        Globals.UNICODE_CHARS.put("\u01D2", "o");
        Globals.UNICODE_CHARS.put("\u01D3", "U");
        Globals.UNICODE_CHARS.put("\u01D4", "u");
        Globals.UNICODE_CHARS.put("\u0232", "Y");
        Globals.UNICODE_CHARS.put("\u0233", "y");
        Globals.UNICODE_CHARS.put("\u01EA", "O");
        Globals.UNICODE_CHARS.put("\u01EB", "o");
        Globals.UNICODE_CHARS.put("\u1E0C", "D");
        Globals.UNICODE_CHARS.put("\u1E0D", "d");
        Globals.UNICODE_CHARS.put("\u1E24", "H");
        Globals.UNICODE_CHARS.put("\u1E25", "h");
        Globals.UNICODE_CHARS.put("\u1E36", "L");
        Globals.UNICODE_CHARS.put("\u1E37", "l");
        Globals.UNICODE_CHARS.put("\u1E38", "L");
        Globals.UNICODE_CHARS.put("\u1E39", "l");
        Globals.UNICODE_CHARS.put("\u1E42", "M");
        Globals.UNICODE_CHARS.put("\u1E43", "m");
        Globals.UNICODE_CHARS.put("\u1E46", "N");
        Globals.UNICODE_CHARS.put("\u1E47", "n");
        Globals.UNICODE_CHARS.put("\u1E5A", "R");
        Globals.UNICODE_CHARS.put("\u1E5B", "r");
        Globals.UNICODE_CHARS.put("\u1E5C", "R");
        Globals.UNICODE_CHARS.put("\u1E5D", "r");
        Globals.UNICODE_CHARS.put("\u1E62", "S");
        Globals.UNICODE_CHARS.put("\u1E63", "s");
        Globals.UNICODE_CHARS.put("\u1E6C", "T");
        Globals.UNICODE_CHARS.put("\u1E6D", "t");
        Globals.UNICODE_CHARS.put("\u00CF", "I");

        Globals.UNICODE_CHARS.put("\u008C", "AE"); // doesn't work?
        Globals.UNICODE_CHARS.put("\u016E", "U");
        Globals.UNICODE_CHARS.put("\u016F", "u");

        Globals.UNICODE_CHARS.put("\u0178", "Y");
        Globals.UNICODE_CHARS.put("\u00FE", ""); // thorn character

        // UNICODE_CHARS.put("\u0100", "");

        Globals.RTFCHARS.put("`a", "\\'e0");
        Globals.RTFCHARS.put("`e", "\\'e8");
        Globals.RTFCHARS.put("`i", "\\'ec");
        Globals.RTFCHARS.put("`o", "\\'f2");
        Globals.RTFCHARS.put("`u", "\\'f9");
        Globals.RTFCHARS.put("?a", "\\'e1");
        Globals.RTFCHARS.put("?e", "\\'e9");
        Globals.RTFCHARS.put("?i", "\\'ed");
        Globals.RTFCHARS.put("?o", "\\'f3");
        Globals.RTFCHARS.put("?u", "\\'fa");
        Globals.RTFCHARS.put("^a", "\\'e2");
        Globals.RTFCHARS.put("^e", "\\'ea");
        Globals.RTFCHARS.put("^i", "\\'ee");
        Globals.RTFCHARS.put("^o", "\\'f4");
        Globals.RTFCHARS.put("^u", "\\'fa");
        Globals.RTFCHARS.put("\"a", "\\'e4");
        Globals.RTFCHARS.put("\"e", "\\'eb");
        Globals.RTFCHARS.put("\"i", "\\'ef");
        Globals.RTFCHARS.put("\"o", "\\'f6");
        Globals.RTFCHARS.put("\"u", "\\u252u");
        Globals.RTFCHARS.put("~n", "\\'f1");
        Globals.RTFCHARS.put("`A", "\\'c0");
        Globals.RTFCHARS.put("`E", "\\'c8");
        Globals.RTFCHARS.put("`I", "\\'cc");
        Globals.RTFCHARS.put("`O", "\\'d2");
        Globals.RTFCHARS.put("`U", "\\'d9");
        Globals.RTFCHARS.put("?A", "\\'c1");
        Globals.RTFCHARS.put("?E", "\\'c9");
        Globals.RTFCHARS.put("?I", "\\'cd");
        Globals.RTFCHARS.put("?O", "\\'d3");
        Globals.RTFCHARS.put("?U", "\\'da");
        Globals.RTFCHARS.put("^A", "\\'c2");
        Globals.RTFCHARS.put("^E", "\\'ca");
        Globals.RTFCHARS.put("^I", "\\'ce");
        Globals.RTFCHARS.put("^O", "\\'d4");
        Globals.RTFCHARS.put("^U", "\\'db");
        Globals.RTFCHARS.put("\"A", "\\'c4");
        Globals.RTFCHARS.put("\"E", "\\'cb");
        Globals.RTFCHARS.put("\"I", "\\'cf");
        Globals.RTFCHARS.put("\"O", "\\'d6");
        Globals.RTFCHARS.put("\"U", "\\'dc");

        // Use UNICODE characters for RTF-Chars which can not be found in the
        // standard codepage

        Globals.RTFCHARS.put("`A", "\\u192A"); // "Agrave"
        Globals.RTFCHARS.put("'A", "\\u193A"); // "Aacute"
        Globals.RTFCHARS.put("^A", "\\u194A"); // "Acirc"
        Globals.RTFCHARS.put("~A", "\\u195A"); // "Atilde"
        Globals.RTFCHARS.put("\"A", "\\u196A"); // "Auml"
        Globals.RTFCHARS.put("AA", "\\u197A"); // "Aring"
        // RTFCHARS.put("AE", "{\\uc2\\u198AE}"); // "AElig"
Globals.RTFCHARS.put("AE", "{\\u198A}"); // "AElig"
        Globals.RTFCHARS.put("cC", "\\u199C"); // "Ccedil"
        Globals.RTFCHARS.put("`E", "\\u200E"); // "Egrave"
        Globals.RTFCHARS.put("'E", "\\u201E"); // "Eacute"
        Globals.RTFCHARS.put("^E", "\\u202E"); // "Ecirc"
        Globals.RTFCHARS.put("\"E", "\\u203E"); // "Euml"
        Globals.RTFCHARS.put("`I", "\\u204I"); // "Igrave
        Globals.RTFCHARS.put("'I", "\\u205I"); // "Iacute"
        Globals.RTFCHARS.put("^I", "\\u206I"); // "Icirc"
        Globals.RTFCHARS.put("\"I", "\\u207I"); // "Iuml"
        Globals.RTFCHARS.put("DH", "\\u208D"); // "ETH"
        Globals.RTFCHARS.put("~N", "\\u209N"); // "Ntilde"
        Globals.RTFCHARS.put("`O", "\\u210O"); // "Ograve"
        Globals.RTFCHARS.put("'O", "\\u211O"); // "Oacute"
        Globals.RTFCHARS.put("^O", "\\u212O"); // "Ocirc"
        Globals.RTFCHARS.put("~O", "\\u213O"); // "Otilde"
        Globals.RTFCHARS.put("\"O", "\\u214O"); // "Ouml"
        // According to ISO 8859-1 the "\times" symbol should be placed here
        // (#215).
        // Omitting this, because it is a mathematical symbol.
        Globals.RTFCHARS.put("O", "\\u216O"); // "Oslash"
        //  RTFCHARS.put("O", "\\'d8");
        Globals.RTFCHARS.put("o", "\\'f8");
        Globals.RTFCHARS.put("`U", "\\u217U"); // "Ugrave"
        Globals.RTFCHARS.put("'U", "\\u218U"); // "Uacute"
        Globals.RTFCHARS.put("^U", "\\u219U"); // "Ucirc" 		
        Globals.RTFCHARS.put("\"U", "\\u220U"); // "Uuml" 
        Globals.RTFCHARS.put("'Y", "\\u221Y"); // "Yacute"
        Globals.RTFCHARS.put("TH", "{\\uc2\\u222TH}"); // "THORN"
        Globals.RTFCHARS.put("ss", "{\\uc2\\u223ss}"); // "szlig"
        //RTFCHARS.put("ss", "AFFEN"); // "szlig"
        Globals.RTFCHARS.put("`a", "\\u224a"); // "agrave"
        Globals.RTFCHARS.put("'a", "\\u225a"); // "aacute"
        Globals.RTFCHARS.put("^a", "\\u226a"); // "acirc"
        Globals.RTFCHARS.put("~a", "\\u227a"); // "atilde"
        Globals.RTFCHARS.put("\"a", "\\u228a"); // "auml"
        Globals.RTFCHARS.put("aa", "\\u229a"); // "aring"
        //  RTFCHARS.put("ae", "{\\uc2\\u230ae}"); // "aelig" \\u230e6
Globals.RTFCHARS.put("ae", "{\\u230a}"); // "aelig" \\u230e6
        Globals.RTFCHARS.put("cc", "\\u231c"); // "ccedil"
        Globals.RTFCHARS.put("`e", "\\u232e"); // "egrave"
        Globals.RTFCHARS.put("'e", "\\u233e"); // "eacute"
        Globals.RTFCHARS.put("^e", "\\u234e"); // "ecirc"
        Globals.RTFCHARS.put("\"e", "\\u235e"); // "euml"
        Globals.RTFCHARS.put("`i", "\\u236i"); // "igrave"
        Globals.RTFCHARS.put("'i", "\\u237i"); // "iacute"
        Globals.RTFCHARS.put("^i", "\\u238i"); // "icirc"
        Globals.RTFCHARS.put("\"i", "\\u239i"); // "iuml"
        Globals.RTFCHARS.put("dh", "\\u240d"); // "eth"
        Globals.RTFCHARS.put("~n", "\\u241n"); // "ntilde"
        Globals.RTFCHARS.put("`o", "\\u242o"); // "ograve"
        Globals.RTFCHARS.put("'o", "\\u243o"); // "oacute"
        Globals.RTFCHARS.put("^o", "\\u244o"); // "ocirc"
        Globals.RTFCHARS.put("~o", "\\u245o"); // "otilde"
        Globals.RTFCHARS.put("\"o", "\\u246o"); // "ouml"
        // According to ISO 8859-1 the "\div" symbol should be placed here
        // (#247).
        // Omitting this, because it is a mathematical symbol.
        Globals.RTFCHARS.put("o", "\\u248o"); // "oslash"
        Globals.RTFCHARS.put("`u", "\\u249u"); // "ugrave"
        Globals.RTFCHARS.put("'u", "\\u250u"); // "uacute"
        Globals.RTFCHARS.put("^u", "\\u251u"); // "ucirc"
        // RTFCHARS.put("\"u", "\\u252"); // "uuml" exists in standard
// codepage
        Globals.RTFCHARS.put("'y", "\\u253y"); // "yacute"
        Globals.RTFCHARS.put("th", "{\\uc2\\u254th}"); // "thorn"
        Globals.RTFCHARS.put("\"y", "\\u255y"); // "yuml"

        Globals.RTFCHARS.put("=A", "\\u256A"); // "Amacr"
        Globals.RTFCHARS.put("=a", "\\u257a"); // "amacr"
        Globals.RTFCHARS.put("uA", "\\u258A"); // "Abreve"
        Globals.RTFCHARS.put("ua", "\\u259a"); // "abreve"
        Globals.RTFCHARS.put("kA", "\\u260A"); // "Aogon"
        Globals.RTFCHARS.put("ka", "\\u261a"); // "aogon"
        Globals.RTFCHARS.put("'C", "\\u262C"); // "Cacute"
        Globals.RTFCHARS.put("'c", "\\u263c"); // "cacute"
        Globals.RTFCHARS.put("^C", "\\u264C"); // "Ccirc"
        Globals.RTFCHARS.put("^c", "\\u265c"); // "ccirc"
        Globals.RTFCHARS.put(".C", "\\u266C"); // "Cdot"
        Globals.RTFCHARS.put(".c", "\\u267c"); // "cdot"
        Globals.RTFCHARS.put("vC", "\\u268C"); // "Ccaron"
        Globals.RTFCHARS.put("vc", "\\u269c"); // "ccaron"
        Globals.RTFCHARS.put("vD", "\\u270D"); // "Dcaron"
        // Symbol #271 (d) has no special Latex command
        Globals.RTFCHARS.put("DJ", "\\u272D"); // "Dstrok"
        Globals.RTFCHARS.put("dj", "\\u273d"); // "dstrok"
        Globals.RTFCHARS.put("=E", "\\u274E"); // "Emacr"
        Globals.RTFCHARS.put("=e", "\\u275e"); // "emacr"
        Globals.RTFCHARS.put("uE", "\\u276E"); // "Ebreve"
        Globals.RTFCHARS.put("ue", "\\u277e"); // "ebreve"
        Globals.RTFCHARS.put(".E", "\\u278E"); // "Edot"
        Globals.RTFCHARS.put(".e", "\\u279e"); // "edot"
        Globals.RTFCHARS.put("kE", "\\u280E"); // "Eogon"
        Globals.RTFCHARS.put("ke", "\\u281e"); // "eogon"
        Globals.RTFCHARS.put("vE", "\\u282E"); // "Ecaron"
        Globals.RTFCHARS.put("ve", "\\u283e"); // "ecaron"
        Globals.RTFCHARS.put("^G", "\\u284G"); // "Gcirc"
        Globals.RTFCHARS.put("^g", "\\u285g"); // "gcirc"
        Globals.RTFCHARS.put("uG", "\\u286G"); // "Gbreve"
        Globals.RTFCHARS.put("ug", "\\u287g"); // "gbreve"
        Globals.RTFCHARS.put(".G", "\\u288G"); // "Gdot"
        Globals.RTFCHARS.put(".g", "\\u289g"); // "gdot"
        Globals.RTFCHARS.put("cG", "\\u290G"); // "Gcedil"
        Globals.RTFCHARS.put("'g", "\\u291g"); // "gacute"
        Globals.RTFCHARS.put("^H", "\\u292H"); // "Hcirc"
        Globals.RTFCHARS.put("^h", "\\u293h"); // "hcirc"
        Globals.RTFCHARS.put("Hstrok", "\\u294H"); // "Hstrok"
        Globals.RTFCHARS.put("hstrok", "\\u295h"); // "hstrok"
        Globals.RTFCHARS.put("~I", "\\u296I"); // "Itilde"
        Globals.RTFCHARS.put("~i", "\\u297i"); // "itilde"
        Globals.RTFCHARS.put("=I", "\\u298I"); // "Imacr"
        Globals.RTFCHARS.put("=i", "\\u299i"); // "imacr"
        Globals.RTFCHARS.put("uI", "\\u300I"); // "Ibreve"
        Globals.RTFCHARS.put("ui", "\\u301i"); // "ibreve"
        Globals.RTFCHARS.put("kI", "\\u302I"); // "Iogon"
        Globals.RTFCHARS.put("ki", "\\u303i"); // "iogon"
        Globals.RTFCHARS.put(".I", "\\u304I"); // "Idot"
        Globals.RTFCHARS.put("i", "\\u305i"); // "inodot"
        // Symbol #306 (IJ) has no special Latex command
        // Symbol #307 (ij) has no special Latex command
        Globals.RTFCHARS.put("^J", "\\u308J"); // "Jcirc"
        Globals.RTFCHARS.put("^j", "\\u309j"); // "jcirc"
        Globals.RTFCHARS.put("cK", "\\u310K"); // "Kcedil"
        Globals.RTFCHARS.put("ck", "\\u311k"); // "kcedil"
        // Symbol #312 (k) has no special Latex command
        Globals.RTFCHARS.put("'L", "\\u313L"); // "Lacute"
        Globals.RTFCHARS.put("'l", "\\u314l"); // "lacute"
        Globals.RTFCHARS.put("cL", "\\u315L"); // "Lcedil"
        Globals.RTFCHARS.put("cl", "\\u316l"); // "lcedil"
        // Symbol #317 (L) has no special Latex command
        // Symbol #318 (l) has no special Latex command
        Globals.RTFCHARS.put("Lmidot", "\\u319L"); // "Lmidot"
        Globals.RTFCHARS.put("lmidot", "\\u320l"); // "lmidot"
        Globals.RTFCHARS.put("L", "\\u321L"); // "Lstrok"
        Globals.RTFCHARS.put("l", "\\u322l"); // "lstrok"
        Globals.RTFCHARS.put("'N", "\\u323N"); // "Nacute"
        Globals.RTFCHARS.put("'n", "\\u324n"); // "nacute"
        Globals.RTFCHARS.put("cN", "\\u325N"); // "Ncedil"
        Globals.RTFCHARS.put("cn", "\\u326n"); // "ncedil"
        Globals.RTFCHARS.put("vN", "\\u327N"); // "Ncaron"
        Globals.RTFCHARS.put("vn", "\\u328n"); // "ncaron"
        // Symbol #329 (n) has no special Latex command
        Globals.RTFCHARS.put("NG", "\\u330G"); // "ENG"
        Globals.RTFCHARS.put("ng", "\\u331g"); // "eng"
        Globals.RTFCHARS.put("=O", "\\u332O"); // "Omacr"
        Globals.RTFCHARS.put("=o", "\\u333o"); // "omacr"
        Globals.RTFCHARS.put("uO", "\\u334O"); // "Obreve"
        Globals.RTFCHARS.put("uo", "\\u335o"); // "obreve"
        Globals.RTFCHARS.put("HO", "\\u336?"); // "Odblac"
        Globals.RTFCHARS.put("Ho", "\\u337?"); // "odblac"
        Globals.RTFCHARS.put("OE", "{\\uc2\\u338OE}"); // "OElig"
        Globals.RTFCHARS.put("oe", "{\\uc2\\u339oe}"); // "oelig"
        Globals.RTFCHARS.put("'R", "\\u340R"); // "Racute"
        Globals.RTFCHARS.put("'r", "\\u341r"); // "racute"
        Globals.RTFCHARS.put("cR", "\\u342R"); // "Rcedil"
        Globals.RTFCHARS.put("cr", "\\u343r"); // "rcedil"
        Globals.RTFCHARS.put("vR", "\\u344R"); // "Rcaron"
        Globals.RTFCHARS.put("vr", "\\u345r"); // "rcaron"
        Globals.RTFCHARS.put("'S", "\\u346S"); // "Sacute"
        Globals.RTFCHARS.put("'s", "\\u347s"); // "sacute"
        Globals.RTFCHARS.put("^S", "\\u348S"); // "Scirc"
        Globals.RTFCHARS.put("^s", "\\u349s"); // "scirc"
        Globals.RTFCHARS.put("cS", "\\u350S"); // "Scedil"
        Globals.RTFCHARS.put("cs", "\\u351s"); // "scedil"
        Globals.RTFCHARS.put("vS", "\\u352S"); // "Scaron"
        Globals.RTFCHARS.put("vs", "\\u353s"); // "scaron"
        Globals.RTFCHARS.put("cT", "\\u354T"); // "Tcedil"
        Globals.RTFCHARS.put("ct", "\\u355t"); // "tcedil"
        Globals.RTFCHARS.put("vT", "\\u356T"); // "Tcaron"
        // Symbol #357 (t) has no special Latex command
        Globals.RTFCHARS.put("Tstrok", "\\u358T"); // "Tstrok"
        Globals.RTFCHARS.put("tstrok", "\\u359t"); // "tstrok"
        Globals.RTFCHARS.put("~U", "\\u360U"); // "Utilde"
        Globals.RTFCHARS.put("~u", "\\u361u"); // "utilde"
        Globals.RTFCHARS.put("=U", "\\u362U"); // "Umacr"
        Globals.RTFCHARS.put("=u", "\\u363u"); // "umacr"
        Globals.RTFCHARS.put("uU", "\\u364U"); // "Ubreve"
        Globals.RTFCHARS.put("uu", "\\u365u"); // "ubreve"
        Globals.RTFCHARS.put("rU", "\\u366U"); // "Uring"
        Globals.RTFCHARS.put("ru", "\\u367u"); // "uring"
        Globals.RTFCHARS.put("HU", "\\u368?"); // "Odblac"
        Globals.RTFCHARS.put("Hu", "\\u369?"); // "odblac"
        Globals.RTFCHARS.put("kU", "\\u370U"); // "Uogon"
        Globals.RTFCHARS.put("ku", "\\u371u"); // "uogon"
        Globals.RTFCHARS.put("^W", "\\u372W"); // "Wcirc"
        Globals.RTFCHARS.put("^w", "\\u373w"); // "wcirc"
        Globals.RTFCHARS.put("^Y", "\\u374Y"); // "Ycirc"
        Globals.RTFCHARS.put("^y", "\\u375y"); // "ycirc"
        Globals.RTFCHARS.put("\"Y", "\\u376Y"); // "Yuml"
        Globals.RTFCHARS.put("'Z", "\\u377Z"); // "Zacute"
        Globals.RTFCHARS.put("'z", "\\u378z"); // "zacute"
        Globals.RTFCHARS.put(".Z", "\\u379Z"); // "Zdot"
        Globals.RTFCHARS.put(".z", "\\u380z"); // "zdot"
        Globals.RTFCHARS.put("vZ", "\\u381Z"); // "Zcaron"
        Globals.RTFCHARS.put("vz", "\\u382z"); // "zcaron"
        // Symbol #383 (f) has no special Latex command

        // XML_CHARS.put("\\u00E1", "&#x00E1;");
    }


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
                    Globals.logger(e.getMessage());
                }
            }
        }

        // Read personal list, if set up:
        if (Globals.prefs.get(JabRefPreferences.PERSONAL_JOURNAL_LIST) != null) {
            try {
                Globals.journalAbbrev.readJournalListFromFile(new File(Globals.prefs.get(JabRefPreferences.PERSONAL_JOURNAL_LIST)));
            } catch (FileNotFoundException e) {
                Globals.logger("Personal journal list file '" + Globals.prefs.get(JabRefPreferences.PERSONAL_JOURNAL_LIST)
                        + "' not found.");
            }
        }

    }

    /**
     * Returns a reg exp pattern in the form (w1)|(w2)| ...
     * wi are escaped if no regex search is enabled
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


    public static void setupLogging() {
        // get the root logger. It is NOT GLOBAL_LOGGER_NAME
        Logger rootLogger = Logger.getLogger("");

        // disable console logging by removing all handlers
        Handler[] handlers = rootLogger.getHandlers();
        for (Handler handler : handlers) {
            rootLogger.removeHandler(handler);
        }

        // add new handler logging to System.out
        StdoutConsoleHandler h = new StdoutConsoleHandler();
        rootLogger.addHandler(h);

        Globals.handler = new CachebleHandler();
        rootLogger.addHandler(handler);
    }

}
