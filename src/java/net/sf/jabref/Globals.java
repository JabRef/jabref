/* (C) 2003 Nizar N. Batada, Morten O. Alver

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
package net.sf.jabref;

import java.io.* ;
import java.util.* ;
import java.util.List;
import java.util.logging.* ;
import java.util.logging.Filter ;

import java.awt.* ;
import javax.swing.* ;

import net.sf.jabref.collab.* ;
import net.sf.jabref.imports.* ;
import net.sf.jabref.util.* ;
import net.sf.jabref.journals.JournalAbbreviations;

public class Globals {

  public static int SHORTCUT_MASK,// = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
      FUTURE_YEAR = 2050, // Needs to give a year definitely in the future. Used for guessing the
                          // year field when parsing textual data.  :-)

      STANDARD_EXPORT_COUNT = 5, // The number of standard export formats.
      METADATA_LINE_LENGTH = 70; // The line length used to wrap metadata.

  private static String resourcePrefix = "resource/JabRef",
                        menuResourcePrefix = "resource/Menu",
                        integrityResourcePrefix = "resource/IntegrityMessage";
  private static final String buildInfos = "/resource/build.properties" ;
  private static String logfile = "jabref.log";
  public static ResourceBundle messages, menuTitles, intMessages ;
  public static FileUpdateMonitor fileUpdateMonitor = new FileUpdateMonitor();
    public static ImportFormatReader importFormatReader = new ImportFormatReader();



    private final static Map tableCache = new WeakHashMap();

  public static String VERSION,
                       BUILD,
                       BUILD_DATE ;

  static
  {
    TBuildInfo bi = new TBuildInfo(buildInfos) ;
    VERSION = bi.getBUILD_VERSION() ;
    BUILD = bi.getBUILD_NUMBER() ;
    BUILD_DATE = bi.getBUILD_DATE() ;
  }



  //public static ResourceBundle preferences = ResourceBundle.getBundle("resource/defaultPrefs");
  public static Locale locale;
  public static final String FILETYPE_PREFS_EXT = "_dir",
      SELECTOR_META_PREFIX = "selector_",
      LAYOUT_PREFIX = "/resource/layout/",
      MAC = "Mac OS X",
      DOI_LOOKUP_PREFIX = "http://dx.doi.org/",
      NONE = "_non__",
      FORMATTER_PACKAGE = "net.sf.jabref.export.layout.format.";
  public static float duplicateThreshold = 0.75f;
  private static Handler consoleHandler = new java.util.logging.ConsoleHandler();
  public static String[] ENCODINGS = new String[] {"ISO8859_1", "UTF8", "UTF-16", "ASCII",
      "Cp1250", "Cp1251", "Cp1252", "Cp1253", "Cp1254", "Cp1257",
      "JIS", "SJIS", "EUC-JP",      // Added Japanese encodings.
      "Big5", "Big5_HKSCS", "GBK",
      "ISO8859_2", "ISO8859_3", "ISO8859_4", "ISO8859_5", "ISO8859_6",
      "ISO8859_7", "ISO8859_8", "ISO8859_9", "ISO8859_13", "ISO8859_15"};

  // String array that maps from month number to month string label:
  public static String[] MONTHS = new String[] {"jan", "feb", "mar", "apr", "may", "jun",
          "jul", "aug", "sep", "oct", "nov", "dec"};

  // Map that maps from month string labels to
  public static Map MONTH_STRINGS = new HashMap();
  static {
      MONTH_STRINGS.put("jan", "January");
      MONTH_STRINGS.put("feb", "February");
      MONTH_STRINGS.put("mar", "March");
      MONTH_STRINGS.put("apr", "April");
      MONTH_STRINGS.put("may", "May");
      MONTH_STRINGS.put("jun", "June");
      MONTH_STRINGS.put("jul", "July");
      MONTH_STRINGS.put("aug", "August");
      MONTH_STRINGS.put("sep", "September");
      MONTH_STRINGS.put("oct", "October");
      MONTH_STRINGS.put("nov", "November");
      MONTH_STRINGS.put("dec", "December");
  }


  public static GlobalFocusListener focusListener = new GlobalFocusListener();
  public static JabRefPreferences prefs = null;
 public static HelpDialog helpDiag = null;
  public static String osName = System.getProperty("os.name", "def");
  public static boolean ON_MAC = (osName.equals(MAC)),
      ON_WIN = osName.startsWith("Windows");

  // The following set of name parts are treated as part of the last name for
  // display purposes, and as part of the first name for sorting purposes:
  public static Set NAME_PARTICLES = new HashSet(Arrays.asList(
          new String[] {"von", "van", "der", "de", "la", "da", "di"}));
  // The following set of name parts are treated as the trailing part of
  // the last name for both sorting and display purposes:
  public static Set JUNIOR_PARTICLES = new HashSet(Arrays.asList(
          new String[] {"Jr", "jr", "Jr.", "jr."}));


  public static String[] SKIP_WORDS = {"a", "an", "the", "for", "on"};
    public static SidePaneManager sidePaneManager;
  public static final String NEWLINE = System.getProperty("line.separator");
    public static final boolean UNIX_NEWLINE = NEWLINE.equals("\n"); // true if we have unix newlines.

    public static void logger(String s) {
    Logger.global.info(s);
  }

  public static void turnOffLogging() { // only log exceptions
    Logger.global.setLevel(java.util.logging.Level.SEVERE);
  }

  // should be only called ones
  public static void turnOnConsoleLogging() {
    Logger.global.addHandler(consoleHandler);

  }



  public static void turnOnFileLogging() {
    Logger.global.setLevel(java.util.logging.Level.ALL);
    java.util.logging.Handler handler;
    handler = new ConsoleHandler();
    /*try {
      handler = new FileHandler(logfile); // this will overwrite
    }
    catch (IOException e) { //can't open log file so use console
        e.printStackTrace();

    } */
    Logger.global.addHandler(handler);

    handler.setFilter(new Filter() { // select what gets logged
      public boolean isLoggable(LogRecord record) {
        return true;
      }
    });
  }

  /**
   * String constants.
   */
  public static final String
      KEY_FIELD = "bibtexkey",
      SEARCH = "__search",
      GROUPSEARCH = "__groupsearch",
      MARKED = "__markedentry",
      OWNER = "owner",
        // Using this when I have no database open when I read
      // non bibtex file formats (used byte ImportFormatReader.java
      DEFAULT_BIBTEXENTRY_ID = "__ID";

  public static void setLanguage(String language, String country) {
    locale = new Locale(language, country);
    messages = ResourceBundle.getBundle(resourcePrefix, locale);
    menuTitles = ResourceBundle.getBundle(menuResourcePrefix, locale);
    intMessages = ResourceBundle.getBundle(integrityResourcePrefix, locale);
    Locale.setDefault(locale);
    javax.swing.JComponent.setDefaultLocale(locale);
  }

  /**
   * Returns the cached value from the tableChache WeakHashMap.
   */
  public static String getCached(String text) {
      Object res = tableCache.get(text);
      return (String)res;
  }

  /**
   * Caches a new value in the tableCache WeakHashMap.
   */
  public static void cache(String text, String textProcessed) {
      tableCache.put(text, textProcessed);
      //System.out.println(tableCache.size());
  }

    public static JournalAbbreviations journalAbbrev;


  public static String lang(String key, String[] params) {
    String translation = null;
    try {
      if (Globals.messages != null) {
        translation = Globals.messages.getString(key.replaceAll(" ", "_"));
      }
    }
    catch (MissingResourceException ex) {
      translation = key;
      /*logger("Warning: could not get translation for \""
                         + key + "\"");*/
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
                      if (params != null && index >= 0 && index <= params.length)
                          sb.append(params[index]);
                  } catch (NumberFormatException e) {
                      // append literally (for quoting) or insert special symbol
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
      return lang(key, (String[])null);
  }

  public static String lang(String key, String s1) {
      return lang(key, new String[]{s1});
  }

  public static String lang(String key, String s1, String s2) {
      return lang(key, new String[]{s1, s2});
  }

  public static String lang(String key, String s1, String s2, String s3) {
      return lang(key, new String[]{s1, s2, s3});
  }

  public static String menuTitle(String key) {
    String translation = null;
    try {
      if (Globals.messages != null) {
        translation = Globals.menuTitles.getString(key.replaceAll(" ", "_"));
      }
    }
    catch (MissingResourceException ex) {
      translation = key;

//      System.err.println("Warning: could not get menu item translation for \""
//                         + key + "\"");

    }
    if ((translation != null) && (translation.length() != 0)) {
      return translation.replaceAll("_", " ");
    }
    else {
      return key;
    }
  }

  public static String getIntegrityMessage(String key)
  {
    String translation = null;
    try {
      if (Globals.intMessages != null) {
        translation = Globals.intMessages.getString(key);
      }
    }
    catch (MissingResourceException ex) {
      translation = key;

//      System.err.println("Warning: could not get menu item translation for \""
//                         + key + "\"");
    }
    if ((translation != null) && (translation.length() != 0)) {
      return translation ;
    }
    else {
      return key;
    }
  }


  //============================================================
  // Using the hashmap of entry types found in BibtexEntryType
  //============================================================
  public static BibtexEntryType getEntryType(String type) {
    // decide which entryType object to return
    Object o = BibtexEntryType.ALL_TYPES.get(type);
    if (o != null) {
      return (BibtexEntryType) o;
    }
    else {
      return BibtexEntryType.OTHER;
    }
    /*
      if(type.equals("article"))
        return BibtexEntryType.ARTICLE;
      else if(type.equals("book"))
        return BibtexEntryType.BOOK;
      else if(type.equals("inproceedings"))
        return BibtexEntryType.INPROCEEDINGS;
     */
  }

    /**
     * This method provides the correct opening brace to use when writing a field
     * to BibTeX format.
     * @return A String containing the braces to use.
     */
    public static String getOpeningBrace() {
        if (prefs.getBoolean("autoDoubleBraces"))
            return "{{";
        else
            return "{";
    }

    /**
     * This method provides the correct closing brace to use when writing a field
     * to BibTeX format.
     * @return A String containing the braces to use.
     */
    public static String getClosingBrace() {
        if (prefs.getBoolean("autoDoubleBraces"))
            return "}}";
        else
            return "}";
    }

    /*    public static void setupKeyBindings(JabRefPreferences prefs) {
    }*/

  public static String getNewFile(JFrame owner, JabRefPreferences prefs,
                                  File directory, String extension,
                                  int dialogType,
                                  boolean updateWorkingDirectory) {
    return getNewFile(owner, prefs, directory, extension, dialogType,
                      updateWorkingDirectory, false);
  }

  public static String getNewFile(JFrame owner, JabRefPreferences prefs,
                                  File directory, String extension, OpenFileFilter off,
                                  int dialogType,
                                  boolean updateWorkingDirectory) {
    return getNewFile(owner, prefs, directory, extension, off, dialogType,
                      updateWorkingDirectory, false);
  }

  public static String getNewDir(JFrame owner, JabRefPreferences prefs,
                                 File directory, String extension,
                                 int dialogType, boolean updateWorkingDirectory) {
    return getNewFile(owner, prefs, directory, extension, dialogType,
                      updateWorkingDirectory, true);
  }

  private static String getNewFile(JFrame owner, JabRefPreferences prefs,
                                   File directory, String extension,
                                   int dialogType,
                                   boolean updateWorkingDirectory,
                                   boolean dirOnly) {

    OpenFileFilter off = null;

    if (extension == null)
      off = new OpenFileFilter();
    else if (!extension.equals(NONE))
      off = new OpenFileFilter(extension);

    return getNewFile(owner, prefs, directory, extension, off, dialogType, updateWorkingDirectory, dirOnly);
  }

  private static String getNewFile(JFrame owner, JabRefPreferences prefs,
                                   File directory, String extension, OpenFileFilter off,
                                   int dialogType,
                                   boolean updateWorkingDirectory,
                                   boolean dirOnly) {

    if (ON_MAC) {
      return getNewFileForMac(owner, prefs, directory, extension, dialogType,
                              updateWorkingDirectory, dirOnly, off);
    }

    JFileChooser fc = null;
    try {
        fc = new JabRefFileChooser(directory);
    } catch (InternalError errl) {
        // This try/catch clause was added because a user reported an
        // InternalError getting thrown on WinNT, presumably because of a
        // bug in JGoodies Windows PLAF. This clause can be removed if the
        // bug is fixed, but for now we just resort to the native file
        // dialog, using the same method as is always used on Mac:
        return getNewFileForMac(owner, prefs, directory, extension, dialogType,
                                updateWorkingDirectory, dirOnly, off);
    }

    if (dirOnly) {
      fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

    }
    fc.addChoosableFileFilter(off);
    fc.setDialogType(dialogType);
    int dialogResult = JFileChooser.CANCEL_OPTION ;
    if (dialogType == JFileChooser.OPEN_DIALOG) {
      dialogResult = fc.showOpenDialog(null);
    }
    else {
      dialogResult = fc.showSaveDialog(null);
    }

     // the getSelectedFile method returns a valid fileselection
     // (if something is selected) indepentently from dialog return status
    if (dialogResult != JFileChooser.APPROVE_OPTION)
      return null ;

    // okay button
    File selectedFile = fc.getSelectedFile();
    if (selectedFile == null) { // cancel
      return null;
    }

    // If this is a save dialog, and the user has not chosen "All files" as filter
    // we enforce the given extension.
    if ( (dialogType == JFileChooser.SAVE_DIALOG) && (fc.getFileFilter() == off) &&
        !off.accept(selectedFile)) {

      // add the first extension if there are multiple extensions
      selectedFile = new File(selectedFile.getPath() + extension.split("[, ]+",0)[0]);
    }

    if (updateWorkingDirectory) {
      prefs.put("workingDirectory", selectedFile.getPath());
    }
    return selectedFile.getAbsolutePath();
  }

  private static String getNewFileForMac(JFrame owner, JabRefPreferences prefs,
                                         File directory, String extensions,
                                         int dialogType,
                                         boolean updateWorkingDirectory,
                                         boolean dirOnly,
                                         FilenameFilter filter) {

    FileDialog fc = new FileDialog(owner);
    //fc.setFilenameFilter(filter);
    if (directory != null) {
      fc.setDirectory(directory.getParent());
    }
    if (dialogType == JFileChooser.OPEN_DIALOG) {
      fc.setMode(FileDialog.LOAD);
    }
    else {
      fc.setMode(FileDialog.SAVE);
    }

    fc.show();

    if (fc.getFile() != null) {
      prefs.put("workingDirectory", fc.getDirectory() + fc.getFile());
      return fc.getDirectory() + fc.getFile();
    }
    else {
      return null;
    }
  }


  public static String SPECIAL_COMMAND_CHARS = "\"`^~'c";
  public static HashMap HTML_CHARS = new HashMap(),
          HTMLCHARS = new HashMap(),
      XML_CHARS = new HashMap(),
      UNICODE_CHARS = new HashMap(),
      RTFCHARS = new HashMap();
  static {

      // Read built-in journal list:
      journalAbbrev = new JournalAbbreviations("/resource/journalList.txt");

      //System.out.println(journalAbbrev.getAbbreviatedName("Journal of Fish Biology", true));
      //System.out.println(journalAbbrev.getAbbreviatedName("Journal of Fish Biology", false));
      //System.out.println(journalAbbrev.getFullName("Aquaculture Eng."));
      /*for (Iterator i=journalAbbrev.fullNameIterator(); i.hasNext();) {
          String s = (String)i.next();
          System.out.println(journalAbbrev.getFullName(s)+" : "+journalAbbrev.getAbbreviatedName(s, true));
      } */

    // Start the thread that monitors file time stamps.
    //Util.pr("Starting FileUpdateMonitor thread. Globals line 293.");
    fileUpdateMonitor.start();

    try {
      SHORTCUT_MASK = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    } catch (Throwable t) {

    }
    /*
    HTML_CHARS.put("\\{\\\\\\\"\\{a\\}\\}", "&auml;");
    HTML_CHARS.put("\\{\\\\\\\"\\{A\\}\\}", "&Auml;");
    HTML_CHARS.put("\\{\\\\\\\"\\{e\\}\\}", "&euml;");
    HTML_CHARS.put("\\{\\\\\\\"\\{E\\}\\}", "&Euml;");
    HTML_CHARS.put("\\{\\\\\\\"\\{i\\}\\}", "&iuml;");
    HTML_CHARS.put("\\{\\\\\\\"\\{I\\}\\}", "&Iuml;");
    HTML_CHARS.put("\\{\\\\\\\"\\{o\\}\\}", "&ouml;");
    HTML_CHARS.put("\\{\\\\\\\"\\{O\\}\\}", "&Ouml;");
    HTML_CHARS.put("\\{\\\\\\\"\\{u\\}\\}", "&uuml;");
    HTML_CHARS.put("\\{\\\\\\\"\\{U\\}\\}", "&Uuml;");

    HTML_CHARS.put("\\{\\\\\\`\\{e\\}\\}", "&egrave;");
    HTML_CHARS.put("\\{\\\\\\`\\{E\\}\\}", "&Egrave;");
    HTML_CHARS.put("\\{\\\\\\`\\{i\\}\\}", "&igrave;");
    HTML_CHARS.put("\\{\\\\\\`\\{I\\}\\}", "&Igrave;");
    HTML_CHARS.put("\\{\\\\\\`\\{o\\}\\}", "&ograve;");
    HTML_CHARS.put("\\{\\\\\\`\\{O\\}\\}", "&Ograve;");
    HTML_CHARS.put("\\{\\\\\\`\\{u\\}\\}", "&ugrave;");
    HTML_CHARS.put("\\{\\\\\\`\\{U\\}\\}", "&Ugrave;");

    HTML_CHARS.put("\\{\\\\\\'\\{e\\}\\}", "&eacute;");
    HTML_CHARS.put("\\{\\\\\\'\\{E\\}\\}", "&Eacute;");
    HTML_CHARS.put("\\{\\\\\\'\\{i\\}\\}", "&iacute;");
    HTML_CHARS.put("\\{\\\\\\'\\{I\\}\\}", "&Iacute;");
    HTML_CHARS.put("\\{\\\\\\'\\{o\\}\\}", "&oacute;");
    HTML_CHARS.put("\\{\\\\\\'\\{O\\}\\}", "&Oacute;");
    HTML_CHARS.put("\\{\\\\\\'\\{u\\}\\}", "&uacute;");
    HTML_CHARS.put("\\{\\\\\\'\\{U\\}\\}", "&Uacute;");
    HTML_CHARS.put("\\{\\\\\\'\\{a\\}\\}", "&aacute;");
    HTML_CHARS.put("\\{\\\\\\'\\{A\\}\\}", "&Aacute;");
    HTML_CHARS.put("\\{\\\\\\^\\{o\\}\\}", "&ocirc;");
    HTML_CHARS.put("\\{\\\\\\^\\{O\\}\\}", "&Ocirc;");
    HTML_CHARS.put("\\{\\\\\\^\\{u\\}\\}", "&ucirc;");
    HTML_CHARS.put("\\{\\\\\\^\\{U\\}\\}", "&Ucirc;");
    HTML_CHARS.put("\\{\\\\\\^\\{e\\}\\}", "&ecirc;");
    HTML_CHARS.put("\\{\\\\\\^\\{E\\}\\}", "&Ecirc;");
    HTML_CHARS.put("\\{\\\\\\^\\{i\\}\\}", "&icirc;");
    HTML_CHARS.put("\\{\\\\\\^\\{I\\}\\}", "&Icirc;");
    HTML_CHARS.put("\\{\\\\\\~\\{o\\}\\}", "&otilde;");
    HTML_CHARS.put("\\{\\\\\\~\\{O\\}\\}", "&Otilde;");
    HTML_CHARS.put("\\{\\\\\\~\\{n\\}\\}", "&ntilde;");
    HTML_CHARS.put("\\{\\\\\\~\\{N\\}\\}", "&Ntilde;");
    HTML_CHARS.put("\\{\\\\\\~\\{a\\}\\}", "&atilde;");
    HTML_CHARS.put("\\{\\\\\\~\\{A\\}\\}", "&Atilde;");
    */

    HTMLCHARS.put("\"a", "&auml;");
    HTMLCHARS.put("\"A", "&Auml;");
    HTMLCHARS.put("\"e", "&euml;");
    HTMLCHARS.put("\"E", "&Euml;");
    HTMLCHARS.put("\"i", "&iuml;");
    HTMLCHARS.put("\"I", "&Iuml;");
    HTMLCHARS.put("\"o", "&ouml;");
    HTMLCHARS.put("\"O", "&Ouml;");
    HTMLCHARS.put("\"u", "&uuml;");
    HTMLCHARS.put("\"U", "&Uuml;");
    HTMLCHARS.put("`a", "&agrave;");
    HTMLCHARS.put("`A", "&Agrave;");
    HTMLCHARS.put("`e", "&egrave;");
    HTMLCHARS.put("`E", "&Egrave;");
    HTMLCHARS.put("`i", "&igrave;");
    HTMLCHARS.put("`I", "&Igrave;");
    HTMLCHARS.put("`o", "&ograve;");
    HTMLCHARS.put("`O", "&Ograve;");
    HTMLCHARS.put("`u", "&ugrave;");
    HTMLCHARS.put("`U", "&Ugrave;");
    HTMLCHARS.put("'e", "&eacute;");
    HTMLCHARS.put("'E", "&Eacute;");
    HTMLCHARS.put("'i", "&iacute;");
    HTMLCHARS.put("'I", "&Iacute;");
    HTMLCHARS.put("'o", "&oacute;");
    HTMLCHARS.put("'O", "&Oacute;");
    HTMLCHARS.put("'u", "&uacute;");
    HTMLCHARS.put("'U", "&Uacute;");
    HTMLCHARS.put("'a", "&aacute;");
    HTMLCHARS.put("'A", "&Aacute;");
    HTMLCHARS.put("^o", "&ocirc;");
    HTMLCHARS.put("^O", "&Ocirc;");
    HTMLCHARS.put("^u", "&ucirc;");
    HTMLCHARS.put("^U", "&Ucirc;");
    HTMLCHARS.put("^e", "&ecirc;");
    HTMLCHARS.put("^E", "&Ecirc;");
    HTMLCHARS.put("^i", "&icirc;");
    HTMLCHARS.put("^I", "&Icirc;");
    HTMLCHARS.put("~o", "&otilde;");
    HTMLCHARS.put("~O", "&Otilde;");
    HTMLCHARS.put("~n", "&ntilde;");
    HTMLCHARS.put("~N", "&Ntilde;");
    HTMLCHARS.put("~a", "&atilde;");
    HTMLCHARS.put("~A", "&Atilde;");
    HTMLCHARS.put("cc", "&ccedil;");
    HTMLCHARS.put("cC", "&Ccedil;");
    /*
    HTML_CHARS.put("\\{\\\\\\\"a\\}", "&auml;");
    HTML_CHARS.put("\\{\\\\\\\"A\\}", "&Auml;");
    HTML_CHARS.put("\\{\\\\\\\"e\\}", "&euml;");
    HTML_CHARS.put("\\{\\\\\\\"E\\}", "&Euml;");
    HTML_CHARS.put("\\{\\\\\\\"i\\}", "&iuml;");
    HTML_CHARS.put("\\{\\\\\\\"I\\}", "&Iuml;");
    HTML_CHARS.put("\\{\\\\\\\"o\\}", "&ouml;");
    HTML_CHARS.put("\\{\\\\\\\"O\\}", "&Ouml;");
    HTML_CHARS.put("\\{\\\\\\\"u\\}", "&uuml;");
    HTML_CHARS.put("\\{\\\\\\\"U\\}", "&Uuml;");

    HTML_CHARS.put("\\{\\\\\\`e\\}", "&egrave;");
    HTML_CHARS.put("\\{\\\\\\`E\\}", "&Egrave;");
    HTML_CHARS.put("\\{\\\\\\`i\\}", "&igrave;");
    HTML_CHARS.put("\\{\\\\\\`I\\}", "&Igrave;");
    HTML_CHARS.put("\\{\\\\\\`o\\}", "&ograve;");
    HTML_CHARS.put("\\{\\\\\\`O\\}", "&Ograve;");
    HTML_CHARS.put("\\{\\\\\\`u\\}", "&ugrave;");
    HTML_CHARS.put("\\{\\\\\\`U\\}", "&Ugrave;");
    HTML_CHARS.put("\\{\\\\\\'A\\}", "&eacute;");
    HTML_CHARS.put("\\{\\\\\\'E\\}", "&Eacute;");
    HTML_CHARS.put("\\{\\\\\\'i\\}", "&iacute;");
    HTML_CHARS.put("\\{\\\\\\'I\\}", "&Iacute;");
    HTML_CHARS.put("\\{\\\\\\'o\\}", "&oacute;");
    HTML_CHARS.put("\\{\\\\\\'O\\}", "&Oacute;");
    HTML_CHARS.put("\\{\\\\\\'u\\}", "&uacute;");
    HTML_CHARS.put("\\{\\\\\\'U\\}", "&Uacute;");
    HTML_CHARS.put("\\{\\\\\\'a\\}", "&aacute;");
    HTML_CHARS.put("\\{\\\\\\'A\\}", "&Aacute;");

    HTML_CHARS.put("\\{\\\\\\^o\\}", "&ocirc;");
    HTML_CHARS.put("\\{\\\\\\^O\\}", "&Ocirc;");
    HTML_CHARS.put("\\{\\\\\\^u\\}", "&ucirc;");
    HTML_CHARS.put("\\{\\\\\\^U\\}", "&Ucirc;");
    HTML_CHARS.put("\\{\\\\\\^e\\}", "&ecirc;");
    HTML_CHARS.put("\\{\\\\\\^E\\}", "&Ecirc;");
    HTML_CHARS.put("\\{\\\\\\^i\\}", "&icirc;");
    HTML_CHARS.put("\\{\\\\\\^I\\}", "&Icirc;");
    HTML_CHARS.put("\\{\\\\\\~o\\}", "&otilde;");
    HTML_CHARS.put("\\{\\\\\\~O\\}", "&Otilde;");
    HTML_CHARS.put("\\{\\\\\\~n\\}", "&ntilde;");
    HTML_CHARS.put("\\{\\\\\\~N\\}", "&Ntilde;");
    HTML_CHARS.put("\\{\\\\\\~a\\}", "&atilde;");
    HTML_CHARS.put("\\{\\\\\\~A\\}", "&Atilde;");

    HTML_CHARS.put("\\{\\\\c c\\}", "&ccedil;");
    HTML_CHARS.put("\\{\\\\c C\\}", "&Ccedil;");
    */

    XML_CHARS.put("\\{\\\\\\\"\\{a\\}\\}", "&#x00E4;");
    XML_CHARS.put("\\{\\\\\\\"\\{A\\}\\}", "&#x00C4;");
    XML_CHARS.put("\\{\\\\\\\"\\{e\\}\\}", "&#x00EB;");
    XML_CHARS.put("\\{\\\\\\\"\\{E\\}\\}", "&#x00CB;");
    XML_CHARS.put("\\{\\\\\\\"\\{i\\}\\}", "&#x00EF;");
    XML_CHARS.put("\\{\\\\\\\"\\{I\\}\\}", "&#x00CF;");
    XML_CHARS.put("\\{\\\\\\\"\\{o\\}\\}", "&#x00F6;");
    XML_CHARS.put("\\{\\\\\\\"\\{O\\}\\}", "&#x00D6;");
    XML_CHARS.put("\\{\\\\\\\"\\{u\\}\\}", "&#x00FC;");
    XML_CHARS.put("\\{\\\\\\\"\\{U\\}\\}", "&#x00DC;");

    XML_CHARS.put("\\{\\\\\\`\\{e\\}\\}", "&#x00E8;");
    XML_CHARS.put("\\{\\\\\\`\\{E\\}\\}", "&#x00C8;");
    XML_CHARS.put("\\{\\\\\\`\\{i\\}\\}", "&#x00EC;");
    XML_CHARS.put("\\{\\\\\\`\\{I\\}\\}", "&#x00CC;");
    XML_CHARS.put("\\{\\\\\\`\\{o\\}\\}", "&#x00F2;");
    XML_CHARS.put("\\{\\\\\\`\\{O\\}\\}", "&#x00D2;");
    XML_CHARS.put("\\{\\\\\\`\\{u\\}\\}", "&#x00F9;");
    XML_CHARS.put("\\{\\\\\\`\\{U\\}\\}", "&#x00D9;");
    XML_CHARS.put("\\{\\\\\\'\\{e\\}\\}", "&#x00E9;");
    XML_CHARS.put("\\{\\\\\\\uFFFD\\{E\\}\\}", "&#x00C9;");
    XML_CHARS.put("\\{\\\\\\\uFFFD\\{i\\}\\}", "&#x00ED;");
    XML_CHARS.put("\\{\\\\\\\uFFFD\\{I\\}\\}", "&#x00CD;");
    XML_CHARS.put("\\{\\\\\\\uFFFD\\{o\\}\\}", "&#x00F3;");
    XML_CHARS.put("\\{\\\\\\\uFFFD\\{O\\}\\}", "&#x00D3;");
    XML_CHARS.put("\\{\\\\\\\uFFFD\\{u\\}\\}", "&#x00FA;");
    XML_CHARS.put("\\{\\\\\\\uFFFD\\{U\\}\\}", "&#x00DA;");
    XML_CHARS.put("\\{\\\\\\\uFFFD\\{a\\}\\}", "&#x00E1;");
    XML_CHARS.put("\\{\\\\\\\uFFFD\\{A\\}\\}", "&#x00C1;");

    XML_CHARS.put("\\{\\\\\\^\\{o\\}\\}", "&#x00F4;");
    XML_CHARS.put("\\{\\\\\\^\\{O\\}\\}", "&#x00D4;");
    XML_CHARS.put("\\{\\\\\\^\\{u\\}\\}", "&#x00F9;");
    XML_CHARS.put("\\{\\\\\\^\\{U\\}\\}", "&#x00D9;");
    XML_CHARS.put("\\{\\\\\\^\\{e\\}\\}", "&#x00EA;");
    XML_CHARS.put("\\{\\\\\\^\\{E\\}\\}", "&#x00CA;");
    XML_CHARS.put("\\{\\\\\\^\\{i\\}\\}", "&#x00EE;");
    XML_CHARS.put("\\{\\\\\\^\\{I\\}\\}", "&#x00CE;");
    XML_CHARS.put("\\{\\\\\\~\\{o\\}\\}", "&#x00F5;");
    XML_CHARS.put("\\{\\\\\\~\\{O\\}\\}", "&#x00D5;");
    XML_CHARS.put("\\{\\\\\\~\\{n\\}\\}", "&#x00F1;");
    XML_CHARS.put("\\{\\\\\\~\\{N\\}\\}", "&#x00D1;");
    XML_CHARS.put("\\{\\\\\\~\\{a\\}\\}", "&#x00E3;");
    XML_CHARS.put("\\{\\\\\\~\\{A\\}\\}", "&#x00C3;");


    XML_CHARS.put("\\{\\\\\\\"a\\}", "&#x00E4;");
    XML_CHARS.put("\\{\\\\\\\"A\\}", "&#x00C4;");
    XML_CHARS.put("\\{\\\\\\\"e\\}", "&#x00EB;");
    XML_CHARS.put("\\{\\\\\\\"E\\}", "&#x00CB;");
    XML_CHARS.put("\\{\\\\\\\"i\\}", "&#x00EF;");
    XML_CHARS.put("\\{\\\\\\\"I\\}", "&#x00CF;");
    XML_CHARS.put("\\{\\\\\\\"o\\}", "&#x00F6;");
    XML_CHARS.put("\\{\\\\\\\"O\\}", "&#x00D6;");
    XML_CHARS.put("\\{\\\\\\\"u\\}", "&#x00FC;");
    XML_CHARS.put("\\{\\\\\\\"U\\}", "&#x00DC;");

    XML_CHARS.put("\\{\\\\\\`e\\}", "&#x00E8;");
    XML_CHARS.put("\\{\\\\\\`E\\}", "&#x00C8;");
    XML_CHARS.put("\\{\\\\\\`i\\}", "&#x00EC;");
    XML_CHARS.put("\\{\\\\\\`I\\}", "&#x00CC;");
    XML_CHARS.put("\\{\\\\\\`o\\}", "&#x00F2;");
    XML_CHARS.put("\\{\\\\\\`O\\}", "&#x00D2;");
    XML_CHARS.put("\\{\\\\\\`u\\}", "&#x00F9;");
    XML_CHARS.put("\\{\\\\\\`U\\}", "&#x00D9;");
    XML_CHARS.put("\\{\\\\\\'e\\}", "&#x00E9;");
    XML_CHARS.put("\\{\\\\\\'E\\}", "&#x00C9;");
    XML_CHARS.put("\\{\\\\\\'i\\}", "&#x00ED;");
    XML_CHARS.put("\\{\\\\\\'I\\}", "&#x00CD;");
    XML_CHARS.put("\\{\\\\\\'o\\}", "&#x00F3;");
    XML_CHARS.put("\\{\\\\\\'O\\}", "&#x00D3;");
    XML_CHARS.put("\\{\\\\\\'u\\}", "&#x00FA;");
    XML_CHARS.put("\\{\\\\\\'U\\}", "&#x00DA;");
    XML_CHARS.put("\\{\\\\\\'a\\}", "&#x00E1;");
    XML_CHARS.put("\\{\\\\\\'A\\}", "&#x00C1;");

    XML_CHARS.put("\\{\\\\\\^o\\}", "&#x00F4;");
    XML_CHARS.put("\\{\\\\\\^O\\}", "&#x00D4;");
    XML_CHARS.put("\\{\\\\\\^u\\}", "&#x00F9;");
    XML_CHARS.put("\\{\\\\\\^U\\}", "&#x00D9;");
    XML_CHARS.put("\\{\\\\\\^e\\}", "&#x00EA;");
    XML_CHARS.put("\\{\\\\\\^E\\}", "&#x00CA;");
    XML_CHARS.put("\\{\\\\\\^i\\}", "&#x00EE;");
    XML_CHARS.put("\\{\\\\\\^I\\}", "&#x00CE;");
    XML_CHARS.put("\\{\\\\\\~o\\}", "&#x00F5;");
    XML_CHARS.put("\\{\\\\\\~O\\}", "&#x00D5;");
    XML_CHARS.put("\\{\\\\\\~n\\}", "&#x00F1;");
    XML_CHARS.put("\\{\\\\\\~N\\}", "&#x00D1;");
    XML_CHARS.put("\\{\\\\\\~a\\}", "&#x00E3;");
    XML_CHARS.put("\\{\\\\\\~A\\}", "&#x00C3;");

    UNICODE_CHARS.put("\u00C0", "A");
    UNICODE_CHARS.put("\u00C1", "A");
    UNICODE_CHARS.put("\u00C2", "A");
    UNICODE_CHARS.put("\u00C3", "A");
    UNICODE_CHARS.put("\u00C4", "A");
    UNICODE_CHARS.put("\u00C5", "Aa");
    UNICODE_CHARS.put("\u00C6", "Ae");
    UNICODE_CHARS.put("\u00C7", "C");
    UNICODE_CHARS.put("\u00C8", "E");
    UNICODE_CHARS.put("\u00C9", "E");
    UNICODE_CHARS.put("\u00CA", "E");
    UNICODE_CHARS.put("\u00CB", "E");
    UNICODE_CHARS.put("\u00CC", "I");
    UNICODE_CHARS.put("\u00CD", "I");
    UNICODE_CHARS.put("\u00CE", "I");
    UNICODE_CHARS.put("\u00CF", "I");
    UNICODE_CHARS.put("\u00D0", "D");
    UNICODE_CHARS.put("\u00D1", "N");
    UNICODE_CHARS.put("\u00D2", "O");
    UNICODE_CHARS.put("\u00D3", "O");
    UNICODE_CHARS.put("\u00D4", "O");
    UNICODE_CHARS.put("\u00D5", "O");
    UNICODE_CHARS.put("\u00D6", "Oe");
    UNICODE_CHARS.put("\u00D8", "Oe");
    UNICODE_CHARS.put("\u00D9", "U");
    UNICODE_CHARS.put("\u00DA", "U");
    UNICODE_CHARS.put("\u00DB", "U");
    UNICODE_CHARS.put("\u00DC", "Ue"); // U umlaut ..
    UNICODE_CHARS.put("\u00DD", "Y");
    UNICODE_CHARS.put("\u00DF", "ss");
    UNICODE_CHARS.put("\u00E0", "a");
    UNICODE_CHARS.put("\u00E1", "a");
    UNICODE_CHARS.put("\u00E2", "a");
    UNICODE_CHARS.put("\u00E3", "a");
    UNICODE_CHARS.put("\u00E4", "ae");
    UNICODE_CHARS.put("\u00E5", "aa");
    UNICODE_CHARS.put("\u00E6", "ae");
    UNICODE_CHARS.put("\u00E7", "c");
    UNICODE_CHARS.put("\u00E8", "e");
    UNICODE_CHARS.put("\u00E9", "e");
    UNICODE_CHARS.put("\u00EA", "e");
    UNICODE_CHARS.put("\u00EB", "e");
    UNICODE_CHARS.put("\u00EC", "i");
    UNICODE_CHARS.put("\u00ED", "i");
    UNICODE_CHARS.put("\u00EE", "i");
    UNICODE_CHARS.put("\u00EF", "i");
    UNICODE_CHARS.put("\u00F0", "o");
    UNICODE_CHARS.put("\u00F1", "n");
    UNICODE_CHARS.put("\u00F2", "o");
    UNICODE_CHARS.put("\u00F3", "o");
    UNICODE_CHARS.put("\u00F4", "o");
    UNICODE_CHARS.put("\u00F5", "o");
    UNICODE_CHARS.put("\u00F6", "oe");
    UNICODE_CHARS.put("\u00F8", "oe");
    UNICODE_CHARS.put("\u00F9", "u");
    UNICODE_CHARS.put("\u00FA", "u");
    UNICODE_CHARS.put("\u00FB", "u");
    UNICODE_CHARS.put("\u00FC", "ue"); // u umlaut...
    UNICODE_CHARS.put("\u00FD", "y");
    UNICODE_CHARS.put("\u00FF", "y");
    UNICODE_CHARS.put("\u0100", "A");
    UNICODE_CHARS.put("\u0101", "a");
    UNICODE_CHARS.put("\u0102", "A");
    UNICODE_CHARS.put("\u0103", "a");
    UNICODE_CHARS.put("\u0104", "A");
    UNICODE_CHARS.put("\u0105", "a");
    UNICODE_CHARS.put("\u0106", "C");
    UNICODE_CHARS.put("\u0107", "c");
    UNICODE_CHARS.put("\u0108", "C");
    UNICODE_CHARS.put("\u0109", "c");
    UNICODE_CHARS.put("\u010A", "C");
    UNICODE_CHARS.put("\u010B", "c");
    UNICODE_CHARS.put("\u010C", "C");
    UNICODE_CHARS.put("\u010D", "c");
    UNICODE_CHARS.put("\u010E", "D");
    UNICODE_CHARS.put("\u010F", "d");
    UNICODE_CHARS.put("\u0110", "D");
    UNICODE_CHARS.put("\u0111", "d");
    UNICODE_CHARS.put("\u0112", "E");
    UNICODE_CHARS.put("\u0113", "e");
    UNICODE_CHARS.put("\u0114", "E");
    UNICODE_CHARS.put("\u0115", "e");
    UNICODE_CHARS.put("\u0116", "E");
    UNICODE_CHARS.put("\u0117", "e");
    UNICODE_CHARS.put("\u0118", "E");
    UNICODE_CHARS.put("\u0119", "e");
    UNICODE_CHARS.put("\u011A", "E");
    UNICODE_CHARS.put("\u011B", "e");
    UNICODE_CHARS.put("\u011C", "G");
    UNICODE_CHARS.put("\u011D", "g");
    UNICODE_CHARS.put("\u011E", "G");
    UNICODE_CHARS.put("\u011F", "g");
    UNICODE_CHARS.put("\u0120", "G");
    UNICODE_CHARS.put("\u0121", "g");
    UNICODE_CHARS.put("\u0122", "G");
    UNICODE_CHARS.put("\u0123", "g");
    UNICODE_CHARS.put("\u0124", "H");
    UNICODE_CHARS.put("\u0125", "h");
    UNICODE_CHARS.put("\u0127", "h");
    UNICODE_CHARS.put("\u0128", "I");
    UNICODE_CHARS.put("\u0129", "i");
    UNICODE_CHARS.put("\u012A", "I");
    UNICODE_CHARS.put("\u012B", "i");
    UNICODE_CHARS.put("\u012C", "I");
    UNICODE_CHARS.put("\u012D", "i");
    //UNICODE_CHARS.put("\u0100", "");

    RTFCHARS.put("`a", "\\'e0");
    RTFCHARS.put("`e", "\\'e8");
    RTFCHARS.put("`i", "\\'ec");
    RTFCHARS.put("`o", "\\'f2");
    RTFCHARS.put("`u", "\\'f9");
    RTFCHARS.put("?a", "\\'e1");
    RTFCHARS.put("?e", "\\'e9");
    RTFCHARS.put("?i", "\\'ed");
    RTFCHARS.put("?o", "\\'f3");
    RTFCHARS.put("?u", "\\'fa");
    RTFCHARS.put("^a", "\\'e2");
    RTFCHARS.put("^e", "\\'ea");
    RTFCHARS.put("^i", "\\'ee");
    RTFCHARS.put("^o", "\\'f4");
    RTFCHARS.put("^u", "\\'fa");
    RTFCHARS.put("\"a", "\\'e4");
    RTFCHARS.put("\"e", "\\'eb");
    RTFCHARS.put("\"i", "\\'ef");
    RTFCHARS.put("\"o", "\\'f6");
    RTFCHARS.put("\"u", "\\'fc");
    RTFCHARS.put("~n", "\\'f1");
    RTFCHARS.put("`A", "\\'c0");
    RTFCHARS.put("`E", "\\'c8");
    RTFCHARS.put("`I", "\\'cc");
    RTFCHARS.put("`O", "\\'d2");
    RTFCHARS.put("`U", "\\'d9");
    RTFCHARS.put("?A", "\\'c1");
    RTFCHARS.put("?E", "\\'c9");
    RTFCHARS.put("?I", "\\'cd");
    RTFCHARS.put("?O", "\\'d3");
    RTFCHARS.put("?U", "\\'da");
    RTFCHARS.put("^A", "\\'c2");
    RTFCHARS.put("^E", "\\'ca");
    RTFCHARS.put("^I", "\\'ce");
    RTFCHARS.put("^O", "\\'d4");
    RTFCHARS.put("^U", "\\'db");
    RTFCHARS.put("\"A", "\\'c4");
    RTFCHARS.put("\"E", "\\'cb");
    RTFCHARS.put("\"I", "\\'cf");
    RTFCHARS.put("\"O", "\\'d6");
    RTFCHARS.put("\"U", "\\'dc");

    //XML_CHARS.put("\\u00E1", "&#x00E1;");
  }

}
