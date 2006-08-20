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
import java.nio.charset.Charset;
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
  public static final String additionalFields = "/resource/fields/fields.xml" ;  // some extra field definitions
  private static String logfile = "jabref.log";
  public static ResourceBundle messages, menuTitles, intMessages ;
  public static FileUpdateMonitor fileUpdateMonitor = new FileUpdateMonitor();
    public static ImportFormatReader importFormatReader = new ImportFormatReader();

  public static ErrorConsole errorConsole;

   public static String VERSION,
                       BUILD,
                       BUILD_DATE ;

  static
  {
    TBuildInfo bi = new TBuildInfo(buildInfos) ;
    VERSION = bi.getBUILD_VERSION() ;
    BUILD = bi.getBUILD_NUMBER() ;
    BUILD_DATE = bi.getBUILD_DATE() ;

      // TODO: Error console initialization here. When should it be used?
      errorConsole = ErrorConsole.getInstance();
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
  public static String[] ENCODINGS,
      ALL_ENCODINGS = //(String[]) Charset.availableCharsets().keySet().toArray(new String[]{});
          new String[] {"ISO8859_1", "UTF8", "UTF-16", "ASCII",
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

      // Build list of encodings, by filtering out all that are not supported
      // on this system:
      List encodings = new ArrayList();
      for (int i=0; i<ALL_ENCODINGS.length; i++) {
          if (Charset.isSupported(ALL_ENCODINGS[i]))
            encodings.add(ALL_ENCODINGS[i]);
      }
      ENCODINGS = (String[])encodings.toArray(new String[0]);

  }


  public static GlobalFocusListener focusListener = new GlobalFocusListener();
  public static JabRefPreferences prefs = null;
 public static HelpDialog helpDiag = null;
  public static String osName = System.getProperty("os.name", "def");
  public static boolean ON_MAC = (osName.equals(MAC)),
      ON_WIN = osName.startsWith("Windows");


  public static String[] SKIP_WORDS = {"a", "an", "the", "for", "on"};
    public static SidePaneManager sidePaneManager;
  public static final String NEWLINE = System.getProperty("line.separator");
    public static final boolean UNIX_NEWLINE = NEWLINE.equals("\n"); // true if we have unix newlines.

    public static final String BIBTEX_STRING = "__string";
    // "Fieldname" to indicate that a field should be treated as a bibtex string. Used when writing database to file.

    public static void logger(String s) {
    Logger.global.info(s);
  }

  public static void turnOffLogging() { // only log exceptions
    Logger.global.setLevel(java.util.logging.Level.SEVERE);
  }

  // should be only called once
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

  public static void setLanguage(String language, String country) {
    locale = new Locale(language, country);
    messages = ResourceBundle.getBundle(resourcePrefix, locale);
    menuTitles = ResourceBundle.getBundle(menuResourcePrefix, locale);
    intMessages = ResourceBundle.getBundle(integrityResourcePrefix, locale);
    Locale.setDefault(locale);
    javax.swing.JComponent.setDefaultLocale(locale);
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
      
      //Thread.dumpStack();  // For debugging
      logger("Warning: could not get translation for \""
                         + key + "\"");
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

      //System.err.println("Warning: could not get menu item translation for \""
      //                   + key + "\"");

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
        return "{";
        /*
            As of version 2.0, storing all fields with double braces is no longer supported, because
            it causes problems with e.g. the author field.

        if (prefs.getBoolean("autoDoubleBraces"))
            return "{{";
        else
            return "{";
        */
    }

    /**
     * This method provides the correct closing brace to use when writing a field
     * to BibTeX format.
     * @return A String containing the braces to use.
     */
    public static String getClosingBrace() {
        return "}";
        /*
            As of version 2.0, storing all fields with double braces is no longer supported, because
            it causes problems with e.g. the author field.

        if (prefs.getBoolean("autoDoubleBraces"))
            return "}}";
        else
        */

    }

    /**
	 * Will return the names of multiple files selected in the given directory
	 * and the given extensions.
	 * 
	 * Will return an empty String array if no entry is found.
	 * 
	 * @param owner
	 * @param directory
	 * @param extension
	 * @param updateWorkingdirectory
	 * @return
	 */
	public static String[] getMultipleFiles(JFrame owner, File directory, String extension,
		boolean updateWorkingdirectory) {

		OpenFileFilter off = null;
		if (extension == null)
			off = new OpenFileFilter();
		else if (!extension.equals(NONE))
			off = new OpenFileFilter(extension);

		Object files = getNewFileImpl(owner, directory, extension, null, off,
			JFileChooser.OPEN_DIALOG, updateWorkingdirectory, false, true);

		if (files instanceof String[]) {
			return (String[]) files;
		}
		// Fix for:
		// http://sourceforge.net/tracker/index.php?func=detail&aid=1538769&group_id=92314&atid=600306
		if (files != null) {
			return new String[] { (String) files };
		}
		return new String[0];
	}

  public static String getNewFile(JFrame owner,
                                  File directory, String extension,
                                  int dialogType,
                                  boolean updateWorkingDirectory) {
    return getNewFile(owner, directory, extension, null, dialogType,
                      updateWorkingDirectory, false);
  }


  public static String getNewFile(JFrame owner,
                                  File directory, String extension,
                                  String description,
                                  int dialogType,
                                  boolean updateWorkingDirectory) {
    return getNewFile(owner, directory, extension, description, dialogType,
                      updateWorkingDirectory, false);
  }


  public static String getNewDir(JFrame owner,
                                 File directory, String extension,
                                 int dialogType, boolean updateWorkingDirectory) {
    return getNewFile(owner, directory, extension, null, dialogType,
                      updateWorkingDirectory, true);
  }

  public static String getNewDir(JFrame owner,
                                 File directory, String extension,
                                 String description,
                                 int dialogType, boolean updateWorkingDirectory) {
    return getNewFile(owner, directory, extension, description, dialogType,
                      updateWorkingDirectory, true);
  }

  private static String getNewFile(JFrame owner,
                                   File directory, String extension,
                                   String description,
                                   int dialogType,
                                   boolean updateWorkingDirectory,
                                   boolean dirOnly) {

    OpenFileFilter off = null;

    if (extension == null)
      off = new OpenFileFilter();
    else if (!extension.equals(NONE))
      off = new OpenFileFilter(extension);

    return (String)getNewFileImpl(owner, directory, extension, description, off,
            dialogType, updateWorkingDirectory, dirOnly, false);
  }

  private static Object getNewFileImpl(JFrame owner,
                                       File directory, String extension,
                                       String description,
                                       OpenFileFilter off,
                                       int dialogType,
                                       boolean updateWorkingDirectory,
                                       boolean dirOnly,
                                       boolean multipleSelection) {

      if (ON_MAC && prefs.getBoolean("useNativeFileDialogOnMac")) {

          return getNewFileForMac(owner, directory, extension, dialogType,
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
          return getNewFileForMac(owner, directory, extension, dialogType,
                  updateWorkingDirectory, dirOnly, off);
      }

      if (dirOnly) {
          fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

      }

      fc.setMultiSelectionEnabled(multipleSelection);

      fc.addChoosableFileFilter(off);
      fc.setDialogType(dialogType);
      int dialogResult;
      if (dialogType == JFileChooser.OPEN_DIALOG) {
          dialogResult = fc.showOpenDialog(owner);
      } else if (dialogType == JFileChooser.SAVE_DIALOG) {
          dialogResult = fc.showSaveDialog(owner);
      } else {
          dialogResult = fc.showDialog(owner, description);
      }

      // the getSelectedFile method returns a valid fileselection
      // (if something is selected) indepentently from dialog return status
      if (dialogResult != JFileChooser.APPROVE_OPTION)
          return null;

      // okay button
      File selectedFile = fc.getSelectedFile();
      if (selectedFile == null) { // cancel
          return null;
      }

      // If this is a save dialog, and the user has not chosen "All files" as filter
      // we enforce the given extension. But only if extension is not null.
      if ((extension != null) && (dialogType == JFileChooser.SAVE_DIALOG) && (fc.getFileFilter() == off) &&
              !off.accept(selectedFile)) {

          // add the first extension if there are multiple extensions
          selectedFile = new File(selectedFile.getPath() + extension.split("[, ]+", 0)[0]);
      }

      if (updateWorkingDirectory) {
          prefs.put("workingDirectory", selectedFile.getPath());
      }

      if (!multipleSelection)
        return selectedFile.getAbsolutePath();
      else {
          File[] files = fc.getSelectedFiles();
          String[] filenames = new String[files.length];
          for (int i=0; i<files.length; i++)
            filenames[i] = files[i].getAbsolutePath();
          return filenames;
      }
  }

  private static String getNewFileForMac(JFrame owner,
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

    fc.setVisible(true); // fc.show(); -> deprecated since 1.5

    if (fc.getFile() != null) {
      Globals.prefs.put("workingDirectory", fc.getDirectory() + fc.getFile());
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
      ASCII2XML_CHARS = new HashMap(),
      UNICODE_CHARS = new HashMap(),
      RTFCHARS = new HashMap(),
        URL_CHARS = new HashMap();
  static {

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

      // Special characters in URLs need to be replaced to ensure that the URL
      // opens properly on all platforms:
      URL_CHARS.put("<", "%3c");
      URL_CHARS.put(">", "%3e");
      URL_CHARS.put("(", "%28");
      URL_CHARS.put(")", "%29");
      URL_CHARS.put(" ", "%20");
      URL_CHARS.put("&", "%26");
      URL_CHARS.put("$", "%24");



//    HTMLCHARS.put("\"a", "&auml;");
//    HTMLCHARS.put("\"A", "&Auml;");
//    HTMLCHARS.put("\"e", "&euml;");
//    HTMLCHARS.put("\"E", "&Euml;");
//    HTMLCHARS.put("\"i", "&iuml;");
//    HTMLCHARS.put("\"I", "&Iuml;");
//    HTMLCHARS.put("\"o", "&ouml;");
//    HTMLCHARS.put("\"O", "&Ouml;");
//    HTMLCHARS.put("\"u", "&uuml;");
//    HTMLCHARS.put("\"U", "&Uuml;");
//    HTMLCHARS.put("`a", "&agrave;");
//    HTMLCHARS.put("`A", "&Agrave;");
//    HTMLCHARS.put("`e", "&egrave;");
//    HTMLCHARS.put("`E", "&Egrave;");
//    HTMLCHARS.put("`i", "&igrave;");
//    HTMLCHARS.put("`I", "&Igrave;");
//    HTMLCHARS.put("`o", "&ograve;");
//    HTMLCHARS.put("`O", "&Ograve;");
//    HTMLCHARS.put("`u", "&ugrave;");
//    HTMLCHARS.put("`U", "&Ugrave;");
//    HTMLCHARS.put("'e", "&eacute;");
//    HTMLCHARS.put("'E", "&Eacute;");
//    HTMLCHARS.put("'i", "&iacute;");
//    HTMLCHARS.put("'I", "&Iacute;");
//    HTMLCHARS.put("'o", "&oacute;");
//    HTMLCHARS.put("'O", "&Oacute;");
//    HTMLCHARS.put("'u", "&uacute;");
//    HTMLCHARS.put("'U", "&Uacute;");
//    HTMLCHARS.put("'a", "&aacute;");
//    HTMLCHARS.put("'A", "&Aacute;");
//    HTMLCHARS.put("^a", "&ocirc;");
//    HTMLCHARS.put("^A", "&Ocirc;");
//    HTMLCHARS.put("^o", "&ocirc;");
//    HTMLCHARS.put("^O", "&Ocirc;");
//    HTMLCHARS.put("^u", "&ucirc;");
//    HTMLCHARS.put("^U", "&Ucirc;");
//    HTMLCHARS.put("^e", "&ecirc;");
//    HTMLCHARS.put("^E", "&Ecirc;");
//    HTMLCHARS.put("^i", "&icirc;");
//    HTMLCHARS.put("^I", "&Icirc;");
//    HTMLCHARS.put("~o", "&otilde;");
//    HTMLCHARS.put("~O", "&Otilde;");
//    HTMLCHARS.put("~n", "&ntilde;");
//    HTMLCHARS.put("~N", "&Ntilde;");
//    HTMLCHARS.put("~a", "&atilde;");
//    HTMLCHARS.put("~A", "&Atilde;");
//    HTMLCHARS.put("cc", "&ccedil;");
//    HTMLCHARS.put("cC", "&Ccedil;");


      // Following character definitions contributed by Ervin Kolenovic:
      // HTML named entities from #192 - #255 (UNICODE Latin-1)
       HTMLCHARS.put("`A", "&Agrave;");    // #192
       HTMLCHARS.put("'A", "&Aacute;");    // #193
       HTMLCHARS.put("^A", "&Acirc;");     // #194
       HTMLCHARS.put("~A", "&Atilde;");    // #195
       HTMLCHARS.put("\"A", "&Auml;");     // #196
       HTMLCHARS.put("AA", "&Aring;");     // #197
       HTMLCHARS.put("AE", "&AElig;");     // #198
       HTMLCHARS.put("cC", "&Ccedil;");    // #199
       HTMLCHARS.put("`E", "&Egrave;");    // #200
       HTMLCHARS.put("'E", "&Eacute;");    // #201
       HTMLCHARS.put("^E", "&Ecirc;");     // #202
       HTMLCHARS.put("\"E", "&Euml;");     // #203
       HTMLCHARS.put("`I", "&Igrave;");    // #204
       HTMLCHARS.put("'I", "&Iacute;");    // #205
       HTMLCHARS.put("^I", "&Icirc;");     // #206
       HTMLCHARS.put("\"I", "&Iuml;");     // #207
       HTMLCHARS.put("DH", "&ETH;");       // #208
       HTMLCHARS.put("~N", "&Ntilde;");    // #209
       HTMLCHARS.put("`O", "&Ograve;");    // #210
       HTMLCHARS.put("'O", "&Oacute;");    // #211
       HTMLCHARS.put("^O", "&Ocirc;");     // #212
       HTMLCHARS.put("~O", "&Otilde;");    // #213
       HTMLCHARS.put("\"O", "&Ouml;");     // #214
       // According to ISO 8859-1 the "\times" symbol should be placed here (#215).
       // Omitting this, because it is a mathematical symbol.
       HTMLCHARS.put("O", "&OSlash;");     // #216
       HTMLCHARS.put("`U", "&Ugrave;");    // #217
       HTMLCHARS.put("'U", "&Uacute;");    // #218
       HTMLCHARS.put("^U", "&Ucirc;");     // #219
       HTMLCHARS.put("\"U", "&Uuml;");     // #220
       HTMLCHARS.put("'Y", "&Yacute;");    // #221
       HTMLCHARS.put("TH", "&THORN;");     // #222
       HTMLCHARS.put("ss", "&szlig;");     // #223
       HTMLCHARS.put("`a", "&agrave;");    // #224
       HTMLCHARS.put("'a", "&aacute;");    // #225
       HTMLCHARS.put("^a", "&acirc;");     // #226
       HTMLCHARS.put("~a", "&atilde;");    // #227
       HTMLCHARS.put("\"a", "&auml;");     // #228
       HTMLCHARS.put("aa", "&aring;");     // #229
       HTMLCHARS.put("ae", "&aelig;");     // #230
       HTMLCHARS.put("cc", "&ccedil;");    // #231
       HTMLCHARS.put("`e", "&egrave;");    // #232
       HTMLCHARS.put("'e", "&eacute;");    // #233
       HTMLCHARS.put("^e", "&ecirc;");     // #234
       HTMLCHARS.put("\"e", "&euml;");     // #235
       HTMLCHARS.put("`i", "&igrave;");    // #236
       HTMLCHARS.put("'i", "&iacute;");    // #237
       HTMLCHARS.put("^i", "&icirc;");     // #238
       HTMLCHARS.put("\"i", "&iuml;");     // #239
       HTMLCHARS.put("dh", "&eth;");       // #240
       HTMLCHARS.put("~n", "&ntilde;");    // #241
       HTMLCHARS.put("`o", "&ograve;");    // #242
       HTMLCHARS.put("'o", "&oacute;");    // #243
       HTMLCHARS.put("^o", "&ocirc;");     // #244
       HTMLCHARS.put("~o", "&otilde;");    // #245
       HTMLCHARS.put("\"o", "&ouml;");     // #246
       // According to ISO 8859-1 the "\div" symbol should be placed here (#247).
       // Omitting this, because it is a mathematical symbol.
       HTMLCHARS.put("o", "&oslash;");     // #248
       HTMLCHARS.put("`u", "&ugrave;");    // #249
       HTMLCHARS.put("'u", "&uacute;");    // #250
       HTMLCHARS.put("^u", "&ucirc;");     // #251
       HTMLCHARS.put("\"u", "&uuml;");     // #252
       HTMLCHARS.put("'y", "&yacute;");    // #253
       HTMLCHARS.put("th", "&thorn;");     // #254
       HTMLCHARS.put("\"y", "&yuml;");     // #255

       // HTML special characters without names (UNICODE Latin Extended-A), indicated by UNICODE number
       HTMLCHARS.put("=A", "&#256;");      // "Amacr"
       HTMLCHARS.put("=a", "&#257;");      // "amacr"
       HTMLCHARS.put("uA", "&#258;");      // "Abreve"
       HTMLCHARS.put("ua", "&#259;");      // "abreve"
       HTMLCHARS.put("kA", "&#260;");      // "Aogon"
       HTMLCHARS.put("ka", "&#261;");      // "aogon"
       HTMLCHARS.put("'C", "&#262;");      // "Cacute"
       HTMLCHARS.put("'c", "&#263;");      // "cacute"
       HTMLCHARS.put("^C", "&#264;");      // "Ccirc"
       HTMLCHARS.put("^c", "&#265;");      // "ccirc"
       HTMLCHARS.put(".C", "&#266;");      // "Cdot"
       HTMLCHARS.put(".c", "&#267;");      // "cdot"
       HTMLCHARS.put("vC", "&#268;");      // "Ccaron"
       HTMLCHARS.put("vc", "&#269;");      // "ccaron"
       HTMLCHARS.put("vD", "&#270;");      // "Dcaron"
       // Symbol #271 (d´) has no special Latex command
       HTMLCHARS.put("DJ", "&#272;");      // "Dstrok"
       HTMLCHARS.put("dj", "&#273;");      // "dstrok"
       HTMLCHARS.put("=E", "&#274;");      // "Emacr"
       HTMLCHARS.put("=e", "&#275;");      // "emacr"
       HTMLCHARS.put("uE", "&#276;");      // "Ebreve"
       HTMLCHARS.put("ue", "&#277;");      // "ebreve"
       HTMLCHARS.put(".E", "&#278;");      // "Edot"
       HTMLCHARS.put(".e", "&#279;");      // "edot"
       HTMLCHARS.put("kE", "&#280;");      // "Eogon"
       HTMLCHARS.put("ke", "&#281;");      // "eogon"
       HTMLCHARS.put("vE", "&#282;");      // "Ecaron"
       HTMLCHARS.put("ve", "&#283;");      // "ecaron"
       HTMLCHARS.put("^G", "&#284;");      // "Gcirc"
       HTMLCHARS.put("^g", "&#285;");      // "gcirc"
       HTMLCHARS.put("uG", "&#286;");      // "Gbreve"
       HTMLCHARS.put("ug", "&#287;");      // "gbreve"
       HTMLCHARS.put(".G", "&#288;");      // "Gdot"
       HTMLCHARS.put(".g", "&#289;");      // "gdot"
       HTMLCHARS.put("cG", "&#290;");      // "Gcedil"
       HTMLCHARS.put("'g", "&#291;");      // "gacute"
       HTMLCHARS.put("^H", "&#292;");      // "Hcirc"
       HTMLCHARS.put("^h", "&#293;");      // "hcirc"
       HTMLCHARS.put("Hstrok", "&#294;");  // "Hstrok"
       HTMLCHARS.put("hstrok", "&#295;");  // "hstrok"
       HTMLCHARS.put("~I", "&#296;");      // "Itilde"
       HTMLCHARS.put("~i", "&#297;");      // "itilde"
       HTMLCHARS.put("=I", "&#298;");      // "Imacr"
       HTMLCHARS.put("=i", "&#299;");      // "imacr"
       HTMLCHARS.put("uI", "&#300;");      // "Ibreve"
       HTMLCHARS.put("ui", "&#301;");      // "ibreve"
       HTMLCHARS.put("kI", "&#302;");      // "Iogon"
       HTMLCHARS.put("ki", "&#303;");      // "iogon"
       HTMLCHARS.put(".I", "&#304;");      // "Idot"
       HTMLCHARS.put("i", "&#305;");       // "inodot"
       // Symbol #306 (IJ) has no special Latex command
       // Symbol #307 (ij) has no special Latex command
       HTMLCHARS.put("^J", "&#308;");      // "Jcirc"
       HTMLCHARS.put("^j", "&#309;");      // "jcirc"
       HTMLCHARS.put("cK", "&#310;");      // "Kcedil"
       HTMLCHARS.put("ck", "&#311;");      // "kcedil"
       // Symbol #312 (k) has no special Latex command
       HTMLCHARS.put("'L", "&#313;");      // "Lacute"
       HTMLCHARS.put("'l", "&#314;");      // "lacute"
       HTMLCHARS.put("cL", "&#315;");      // "Lcedil"
       HTMLCHARS.put("cl", "&#316;");      // "lcedil"
       // Symbol #317 (L´) has no special Latex command
       // Symbol #318 (l´) has no special Latex command
       HTMLCHARS.put("Lmidot", "&#319;");      // "Lmidot"
       HTMLCHARS.put("lmidot", "&#320;");      // "lmidot"
       HTMLCHARS.put("L", "&#321;");      // "Lstrok"
       HTMLCHARS.put("l", "&#322;");      // "lstrok"
       HTMLCHARS.put("'N", "&#323;");      // "Nacute"
       HTMLCHARS.put("'n", "&#324;");      // "nacute"
       HTMLCHARS.put("cN", "&#325;");      // "Ncedil"
       HTMLCHARS.put("cn", "&#326;");      // "ncedil"
       HTMLCHARS.put("vN", "&#327;");      // "Ncaron"
       HTMLCHARS.put("vn", "&#328;");      // "ncaron"
       // Symbol #329 (´n) has no special Latex command
       HTMLCHARS.put("NG", "&#330;");      // "ENG"
       HTMLCHARS.put("ng", "&#331;");      // "eng"
       HTMLCHARS.put("=O", "&#332;");      // "Omacr"
       HTMLCHARS.put("=o", "&#333;");      // "omacr"
       HTMLCHARS.put("uO", "&#334;");      // "Obreve"
       HTMLCHARS.put("uo", "&#335;");      // "obreve"
       HTMLCHARS.put("HO", "&#336;");      // "Odblac"
       HTMLCHARS.put("Ho", "&#337;");      // "odblac"
       HTMLCHARS.put("OE", "&#338;");      // "OElig"
       HTMLCHARS.put("oe", "&#339;");      // "oelig"
       HTMLCHARS.put("'R", "&#340;");      // "Racute"
       HTMLCHARS.put("'r", "&#341;");      // "racute"
       HTMLCHARS.put("cR", "&#342;");      // "Rcedil"
       HTMLCHARS.put("cr", "&#343;");      // "rcedil"
       HTMLCHARS.put("vR", "&#344;");      // "Rcaron"
       HTMLCHARS.put("vr", "&#345;");      // "rcaron"
       HTMLCHARS.put("'S", "&#346;");      // "Sacute"
       HTMLCHARS.put("'s", "&#347;");      // "sacute"
       HTMLCHARS.put("^S", "&#348;");      // "Scirc"
       HTMLCHARS.put("^s", "&#349;");      // "scirc"
       HTMLCHARS.put("cS", "&#350;");      // "Scedil"
       HTMLCHARS.put("cs", "&#351;");      // "scedil"
       HTMLCHARS.put("vS", "&#352;");      // "Scaron"
       HTMLCHARS.put("vs", "&#353;");      // "scaron"
       HTMLCHARS.put("cT", "&#354;");      // "Tcedil"
       HTMLCHARS.put("ct", "&#355;");      // "tcedil"
       HTMLCHARS.put("vT", "&#356;");      // "Tcaron"
       // Symbol #357 (t´) has no special Latex command
       HTMLCHARS.put("Tstrok", "&#358;");  // "Tstrok"
       HTMLCHARS.put("tstrok", "&#359;");  // "tstrok"
       HTMLCHARS.put("~U", "&#360;");      // "Utilde"
       HTMLCHARS.put("~u", "&#361;");      // "utilde"
       HTMLCHARS.put("=U", "&#362;");      // "Umacr"
       HTMLCHARS.put("=u", "&#363;");      // "umacr"
       HTMLCHARS.put("uU", "&#364;");      // "Ubreve"
       HTMLCHARS.put("uu", "&#365;");      // "ubreve"
       HTMLCHARS.put("rU", "&#366;");      // "Uring"
       HTMLCHARS.put("ru", "&#367;");      // "uring"
       HTMLCHARS.put("HU", "&#368;");      // "Odblac"
       HTMLCHARS.put("Hu", "&#369;");      // "odblac"
       HTMLCHARS.put("kU", "&#370;");      // "Uogon"
       HTMLCHARS.put("ku", "&#371;");      // "uogon"
       HTMLCHARS.put("^W", "&#372;");      // "Wcirc"
       HTMLCHARS.put("^w", "&#373;");      // "wcirc"
       HTMLCHARS.put("^Y", "&#374;");      // "Ycirc"
       HTMLCHARS.put("^y", "&#375;");      // "ycirc"
       HTMLCHARS.put("\"Y", "&#376;");     // "Yuml"
       HTMLCHARS.put("'Z", "&#377;");      // "Zacute"
       HTMLCHARS.put("'z", "&#378;");      // "zacute"
       HTMLCHARS.put(".Z", "&#379;");      // "Zdot"
       HTMLCHARS.put(".z", "&#380;");      // "zdot"
       HTMLCHARS.put("vZ", "&#381;");      // "Zcaron"
       HTMLCHARS.put("vz", "&#382;");      // "zcaron"
       // Symbol #383 (f) has no special Latex command


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

    XML_CHARS.put("\\{\\\\\\^a\\}", "&#x00F4;");
    XML_CHARS.put("\\{\\\\\\^A\\}", "&#x00D4;");
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

    ASCII2XML_CHARS.put("<", "&lt;");
    ASCII2XML_CHARS.put("\"", "&quot;");
    ASCII2XML_CHARS.put(">", "&gt;");


    UNICODE_CHARS.put("\u00C0", "A");
    UNICODE_CHARS.put("\u00C1", "A");
    UNICODE_CHARS.put("\u00C2", "A");
    UNICODE_CHARS.put("\u00C3", "A");
    UNICODE_CHARS.put("\u00C4", "Ae");
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
    RTFCHARS.put("\"u", "\\uc0\\u252");
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
    // Use UNICODE characters for RTF-Chars which can not be found in the standard codepage

    //  RTFCHARS.put("`A", "\\uc0\\u192");     // "Agrave" exists in standard codepage  
      RTFCHARS.put("'A", "\\uc0\\u193");     // "Aacute"
//  RTFCHARS.put("^A", "\\uc0\\u194");     // "Acirc"  exists in standard codepage
      RTFCHARS.put("~A", "\\uc0\\u195");     // "Atilde"
//  RTFCHARS.put("\"A", "\\uc0\\u196");    // "Auml"   exists in standard codepage
      RTFCHARS.put("AA", "\\uc0\\u197");     // "Aring"
      RTFCHARS.put("AE", "\\uc0\\u198");     // "AElig"
      RTFCHARS.put("cC", "\\uc0\\u199");     // "Ccedil"
//  RTFCHARS.put("`E", "\\uc0\\u200");     // "Egrave" exists in standard codepage
      RTFCHARS.put("'E", "\\uc0\\u201");     // "Eacute"
//  RTFCHARS.put("^E", "\\uc0\\u202");     // "Ecirc"  exists in standard codepage
//  RTFCHARS.put("\"E", "\\uc0\\u203");    // "Euml"   exists in standard codepage
//  RTFCHARS.put("`I", "\\uc0\\u204");     // "Igrave" exists in standard codepage
      RTFCHARS.put("'I", "\\uc0\\u205");     // "Iacute"
//  RTFCHARS.put("^I", "\\uc0\\u206");     // "Icirc"  exists in standard codepage
//  RTFCHARS.put("\"I", "\\uc0\\u207");    // "Iuml"   exists in standard codepage
      RTFCHARS.put("DH", "\\uc0\\u208");     // "ETH"
      RTFCHARS.put("~N", "\\uc0\\u209");     // "Ntilde"
//  RTFCHARS.put("`O", "\\uc0\\u210");     // "Ograve" exists in standard codepage
      RTFCHARS.put("'O", "\\uc0\\u211");     // "Oacute"
//  RTFCHARS.put("^O", "\\uc0\\u212");     // "Ocirc"  exists in standard codepage
      RTFCHARS.put("~O", "\\uc0\\u213");     // "Otilde"
//  RTFCHARS.put("\"O", "\\uc0\\u214");    // "Ouml"   exists in standard codepage
      // According to ISO 8859-1 the "\times" symbol should be placed here (#215).
      // Omitting this, because it is a mathematical symbol.
      RTFCHARS.put("O", "\\uc0\\u216");      // "OSlash"
//  RTFCHARS.put("`U", "\\uc0\\u217");     // "Ugrave"  exists in standard codepage
      RTFCHARS.put("'U", "\\uc0\\u218");     // "Uacute"
//  RTFCHARS.put("^U", "\\uc0\\u219");     // "Ucirc"   exists in standard codepage
//  RTFCHARS.put("\"U", "\\uc0\\u220");    // "Uuml"    exists in standard codepage
      RTFCHARS.put("'Y", "\\uc0\\u221");     // "Yacute"
      RTFCHARS.put("TH", "\\uc0\\u222");     // "THORN"
      RTFCHARS.put("ss", "\\uc0\\u223");     // "szlig"
//  RTFCHARS.put("`a", "\\uc0\\u224");     // "agrave"  exists in standard codepage
      RTFCHARS.put("'a", "\\uc0\\u225");     // "aacute"
//  RTFCHARS.put("^a", "\\uc0\\u226");     // "acirc"   exists in standard codepage
      RTFCHARS.put("~a", "\\uc0\\u227");     // "atilde"
//  RTFCHARS.put("\"a", "\\uc0\\u228");    // "auml"    exists in standard codepage
      RTFCHARS.put("aa", "\\uc0\\u229");     // "aring"
      RTFCHARS.put("ae", "\\uc0\\u230");     // "aelig"
      RTFCHARS.put("cc", "\\uc0\\u231");     // "ccedil"
//  RTFCHARS.put("`e", "\\uc0\\u232");     // "egrave"  exists in standard codepage
      RTFCHARS.put("'e", "\\uc0\\u233");     // "eacute"
//  RTFCHARS.put("^e", "\\uc0\\u234");     // "ecirc"   exists in standard codepage
//  RTFCHARS.put("\"e", "\\uc0\\u235");    // "euml"    exists in standard codepage
//  RTFCHARS.put("`i", "\\uc0\\u236");     // "igrave"  exists in standard codepage
      RTFCHARS.put("'i", "\\uc0\\u237");     // "iacute"
//  RTFCHARS.put("^i", "\\uc0\\u238");     // "icirc"   exists in standard codepage
//  RTFCHARS.put("\"i", "\\uc0\\u239");    // "iuml"    exists in standard codepage
      RTFCHARS.put("dh", "\\uc0\\u240");     // "eth"
//  RTFCHARS.put("~n", "\\uc0\\u241");     // "ntilde"  exists in standard codepage
//  RTFCHARS.put("`o", "\\uc0\\u242");     // "ograve"  exists in standard codepage
      RTFCHARS.put("'o", "\\uc0\\u243");     // "oacute"
//  RTFCHARS.put("^o", "\\uc0\\u244");     // "ocirc"   exists in standard codepage
      RTFCHARS.put("~o", "\\uc0\\u245");     // "otilde"
//  RTFCHARS.put("\"o", "\\uc0\\u246");    // "ouml"    exists in standard codepage
      // According to ISO 8859-1 the "\div" symbol should be placed here (#247).
      // Omitting this, because it is a mathematical symbol.
      RTFCHARS.put("o", "\\uc0\\u248");       // "oslash"
//  RTFCHARS.put("`u", "\\uc0\\u249");      // "ugrave"  exists in standard codepage
      RTFCHARS.put("'u", "\\uc0\\u250");      // "uacute"
//  RTFCHARS.put("^u", "\\uc0\\u251");      // "ucirc"   exists in standard codepage
//  RTFCHARS.put("\"u", "\\uc0\\u252");     // "uuml"    exists in standard codepage
      RTFCHARS.put("'y", "\\uc0\\u253");      // "yacute"
      RTFCHARS.put("th", "\\uc0\\u254");      // "thorn"
      RTFCHARS.put("\"y", "\\uc0\\u255");     // "yuml"

      RTFCHARS.put("=A", "\\uc0\\u256");      // "Amacr"
      RTFCHARS.put("=a", "\\uc0\\u257");      // "amacr"
      RTFCHARS.put("uA", "\\uc0\\u258");      // "Abreve"
      RTFCHARS.put("ua", "\\uc0\\u259");      // "abreve"
      RTFCHARS.put("kA", "\\uc0\\u260");      // "Aogon"
      RTFCHARS.put("ka", "\\uc0\\u261");      // "aogon"
      RTFCHARS.put("'C", "\\uc0\\u262");      // "Cacute"
      RTFCHARS.put("'c", "\\uc0\\u263");      // "cacute"
      RTFCHARS.put("^C", "\\uc0\\u264");      // "Ccirc"
      RTFCHARS.put("^c", "\\uc0\\u265");      // "ccirc"
      RTFCHARS.put(".C", "\\uc0\\u266");      // "Cdot"
      RTFCHARS.put(".c", "\\uc0\\u267");      // "cdot"
      RTFCHARS.put("vC", "\\uc0\\u268");      // "Ccaron"
      RTFCHARS.put("vc", "\\uc0\\u269");      // "ccaron"
      RTFCHARS.put("vD", "\\uc0\\u270");      // "Dcaron"
      // Symbol #271 (d´) has no special Latex command
      RTFCHARS.put("DJ", "\\uc0\\u272");      // "Dstrok"
      RTFCHARS.put("dj", "\\uc0\\u273");      // "dstrok"
      RTFCHARS.put("=E", "\\uc0\\u274");      // "Emacr"
      RTFCHARS.put("=e", "\\uc0\\u275");      // "emacr"
      RTFCHARS.put("uE", "\\uc0\\u276");      // "Ebreve"
      RTFCHARS.put("ue", "\\uc0\\u277");      // "ebreve"
      RTFCHARS.put(".E", "\\uc0\\u278");      // "Edot"
      RTFCHARS.put(".e", "\\uc0\\u279");      // "edot"
      RTFCHARS.put("kE", "\\uc0\\u280");      // "Eogon"
      RTFCHARS.put("ke", "\\uc0\\u281");      // "eogon"
      RTFCHARS.put("vE", "\\uc0\\u282");      // "Ecaron"
      RTFCHARS.put("ve", "\\uc0\\u283");      // "ecaron"
      RTFCHARS.put("^G", "\\uc0\\u284");      // "Gcirc"
      RTFCHARS.put("^g", "\\uc0\\u285");      // "gcirc"
      RTFCHARS.put("uG", "\\uc0\\u286");      // "Gbreve"
      RTFCHARS.put("ug", "\\uc0\\u287");      // "gbreve"
      RTFCHARS.put(".G", "\\uc0\\u288");      // "Gdot"
      RTFCHARS.put(".g", "\\uc0\\u289");      // "gdot"
      RTFCHARS.put("cG", "\\uc0\\u290");      // "Gcedil"
      RTFCHARS.put("'g", "\\uc0\\u291");      // "gacute"
      RTFCHARS.put("^H", "\\uc0\\u292");      // "Hcirc"
      RTFCHARS.put("^h", "\\uc0\\u293");      // "hcirc"
      RTFCHARS.put("Hstrok", "\\uc0\\u294");  // "Hstrok"
      RTFCHARS.put("hstrok", "\\uc0\\u295");  // "hstrok"
      RTFCHARS.put("~I", "\\uc0\\u296");      // "Itilde"
      RTFCHARS.put("~i", "\\uc0\\u297");      // "itilde"
      RTFCHARS.put("=I", "\\uc0\\u298");      // "Imacr"
      RTFCHARS.put("=i", "\\uc0\\u299");      // "imacr"
      RTFCHARS.put("uI", "\\uc0\\u300");      // "Ibreve"
      RTFCHARS.put("ui", "\\uc0\\u301");      // "ibreve"
      RTFCHARS.put("kI", "\\uc0\\u302");      // "Iogon"
      RTFCHARS.put("ki", "\\uc0\\u303");      // "iogon"
      RTFCHARS.put(".I", "\\uc0\\u304");      // "Idot"
      RTFCHARS.put("i", "\\uc0\\u305");       // "inodot"
      // Symbol #306 (IJ) has no special Latex command
      // Symbol #307 (ij) has no special Latex command
      RTFCHARS.put("^J", "\\uc0\\u308");      // "Jcirc"
      RTFCHARS.put("^j", "\\uc0\\u309");      // "jcirc"
      RTFCHARS.put("cK", "\\uc0\\u310");      // "Kcedil"
      RTFCHARS.put("ck", "\\uc0\\u311");      // "kcedil"
      // Symbol #312 (k) has no special Latex command
      RTFCHARS.put("'L", "\\uc0\\u313");      // "Lacute"
      RTFCHARS.put("'l", "\\uc0\\u314");      // "lacute"
      RTFCHARS.put("cL", "\\uc0\\u315");      // "Lcedil"
      RTFCHARS.put("cl", "\\uc0\\u316");      // "lcedil"
      // Symbol #317 (L´) has no special Latex command
      // Symbol #318 (l´) has no special Latex command
      RTFCHARS.put("Lmidot", "\\uc0\\u319");  // "Lmidot"
      RTFCHARS.put("lmidot", "\\uc0\\u320");  // "lmidot"
      RTFCHARS.put("L", "\\uc0\\u321");      // "Lstrok"
      RTFCHARS.put("l", "\\uc0\\u322");      // "lstrok"
      RTFCHARS.put("'N", "\\uc0\\u323");      // "Nacute"
      RTFCHARS.put("'n", "\\uc0\\u324");      // "nacute"
      RTFCHARS.put("cN", "\\uc0\\u325");      // "Ncedil"
      RTFCHARS.put("cn", "\\uc0\\u326");      // "ncedil"
      RTFCHARS.put("vN", "\\uc0\\u327");      // "Ncaron"
      RTFCHARS.put("vn", "\\uc0\\u328");      // "ncaron"
      // Symbol #329 (´n) has no special Latex command
      RTFCHARS.put("NG", "\\uc0\\u330");      // "ENG"
      RTFCHARS.put("ng", "\\uc0\\u331");      // "eng"
      RTFCHARS.put("=O", "\\uc0\\u332");      // "Omacr"
      RTFCHARS.put("=o", "\\uc0\\u333");      // "omacr"
      RTFCHARS.put("uO", "\\uc0\\u334");      // "Obreve"
      RTFCHARS.put("uo", "\\uc0\\u335");      // "obreve"
      RTFCHARS.put("HO", "\\uc0\\u336");      // "Odblac"
      RTFCHARS.put("Ho", "\\uc0\\u337");      // "odblac"
      RTFCHARS.put("OE", "\\uc0\\u338");      // "OElig"
      RTFCHARS.put("oe", "\\uc0\\u339");      // "oelig"
      RTFCHARS.put("'R", "\\uc0\\u340");      // "Racute"
      RTFCHARS.put("'r", "\\uc0\\u341");      // "racute"
      RTFCHARS.put("cR", "\\uc0\\u342");      // "Rcedil"
      RTFCHARS.put("cr", "\\uc0\\u343");      // "rcedil"
      RTFCHARS.put("vR", "\\uc0\\u344");      // "Rcaron"
      RTFCHARS.put("vr", "\\uc0\\u345");      // "rcaron"
      RTFCHARS.put("'S", "\\uc0\\u346");      // "Sacute"
      RTFCHARS.put("'s", "\\uc0\\u347");      // "sacute"
      RTFCHARS.put("^S", "\\uc0\\u348");      // "Scirc"
      RTFCHARS.put("^s", "\\uc0\\u349");      // "scirc"
      RTFCHARS.put("cS", "\\uc0\\u350");      // "Scedil"
      RTFCHARS.put("cs", "\\uc0\\u351");      // "scedil"
      RTFCHARS.put("vS", "\\uc0\\u352");      // "Scaron"
      RTFCHARS.put("vs", "\\uc0\\u353");      // "scaron"
      RTFCHARS.put("cT", "\\uc0\\u354");      // "Tcedil"
      RTFCHARS.put("ct", "\\uc0\\u355");      // "tcedil"
      RTFCHARS.put("vT", "\\uc0\\u356");      // "Tcaron"
      // Symbol #357 (t´) has no special Latex command
      RTFCHARS.put("Tstrok", "\\uc0\\u358");  // "Tstrok"
      RTFCHARS.put("tstrok", "\\uc0\\u359");  // "tstrok"
      RTFCHARS.put("~U", "\\uc0\\u360");      // "Utilde"
      RTFCHARS.put("~u", "\\uc0\\u361");      // "utilde"
      RTFCHARS.put("=U", "\\uc0\\u362");      // "Umacr"
      RTFCHARS.put("=u", "\\uc0\\u363");      // "umacr"
      RTFCHARS.put("uU", "\\uc0\\u364");      // "Ubreve"
      RTFCHARS.put("uu", "\\uc0\\u365");      // "ubreve"
      RTFCHARS.put("rU", "\\uc0\\u366");      // "Uring"
      RTFCHARS.put("ru", "\\uc0\\u367");      // "uring"
      RTFCHARS.put("HU", "\\uc0\\u368");      // "Odblac"
      RTFCHARS.put("Hu", "\\uc0\\u369");      // "odblac"
      RTFCHARS.put("kU", "\\uc0\\u370");      // "Uogon"
      RTFCHARS.put("ku", "\\uc0\\u371");      // "uogon"
      RTFCHARS.put("^W", "\\uc0\\u372");      // "Wcirc"
      RTFCHARS.put("^w", "\\uc0\\u373");      // "wcirc"
      RTFCHARS.put("^Y", "\\uc0\\u374");      // "Ycirc"
      RTFCHARS.put("^y", "\\uc0\\u375");      // "ycirc"
      RTFCHARS.put("\"Y", "\\uc0\\u376");     // "Yuml"
      RTFCHARS.put("'Z", "\\uc0\\u377");      // "Zacute"
      RTFCHARS.put("'z", "\\uc0\\u378");      // "zacute"
      RTFCHARS.put(".Z", "\\uc0\\u379");      // "Zdot"
      RTFCHARS.put(".z", "\\uc0\\u380");      // "zdot"
      RTFCHARS.put("vZ", "\\uc0\\u381");      // "Zcaron"
      RTFCHARS.put("vz", "\\uc0\\u382");      // "zcaron"
      // Symbol #383 (f) has no special Latex command

    //XML_CHARS.put("\\u00E1", "&#x00E1;");
  }

    public static void initializeJournalNames() {
        journalAbbrev = new JournalAbbreviations();//"/resource/journalList.txt");

        // Read external lists, if any (in reverse order, so the upper lists override the lower):
        String[] lists = prefs.getStringArray("externalJournalLists");
        if ((lists != null) && (lists.length > 0)) {
            for (int i=lists.length-1; i>=0; i--) {
                try {
                    journalAbbrev.readJournalList(new File(lists[i]));
                } catch (FileNotFoundException e) {
                    // The file couldn't be found... should we tell anyone?
                    Globals.logger(e.getMessage());
                }
            }
        }

        // Read personal list, if set up:
        if (prefs.get("personalJournalList") != null) {
            try {
                journalAbbrev.readJournalList(new File(prefs.get("personalJournalList")));
            } catch (FileNotFoundException e) {
                Globals.logger("Personal journal list file '"+prefs.get("personalJournalList")+
                    "' not found.");
            }
        }


    }

}
