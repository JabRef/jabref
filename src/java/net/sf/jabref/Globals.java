/*
 Copyright (C) 2003 Nizar N. Batada, Morten O. Alver

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

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import javax.swing.JFileChooser;
import java.util.logging.*;
import java.io.IOException;
import java.awt.Font;
import java.io.*;
import java.awt.*;
import javax.swing.*;
import net.sf.jabref.collab.FileUpdateMonitor;

public class Globals {

  public static int SHORTCUT_MASK,// = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
      FUTURE_YEAR = 2050; // Needs to give a year definitely in the future. Used for guessing the
                          // year field when parsing textual data.  :-)

  private static String resourcePrefix = "resource/JabRef",
      menuResourcePrefix = "resource/Menu";
  private static String logfile = "jabref.log";
  public static ResourceBundle messages, menuTitles;
  public static FileUpdateMonitor fileUpdateMonitor = new FileUpdateMonitor();

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

  public static String[] ENCODINGS = new String[] {"ISO8859_1", "UTF8", "UTF-16", "ASCII",
      "Cp1250", "Cp1251", "Cp1252", "Cp1253", "Cp1254", "Cp1257", "ISO8859_2",
      "ISO8859_4", "ISO8859_5", "ISO8859_7"};

  public static GlobalFocusListener focusListener = new GlobalFocusListener();
  public static JabRefPreferences prefs = null;
  public static NameCache nameCache_lastFirst = new NameCache(1000);
  public static NameCache nameCache_commas = new NameCache(1000);
  public static NameCache nameCache = new NameCache(1000);

  public static String osName = System.getProperty("os.name", "def");
  public static boolean ON_MAC = (osName.equals(MAC)),
      ON_WIN = (osName.startsWith("Windows"));

  public static String[] SKIP_WORDS = {"a", "an", "the", "for"};

  public static void logger(String s) {
    Logger.global.info(s);
  }

  public static void turnOffLogging() { // only log exceptions
    Logger.global.setLevel(java.util.logging.Level.SEVERE);
  }

  // should be only called ones
  public static void turnOnConsoleLogging() {
    Logger.global.addHandler(new java.util.logging.ConsoleHandler());
  }

  public static void turnOnFileLogging() {
    Logger.global.setLevel(java.util.logging.Level.ALL);
    java.util.logging.Handler handler;
    try {
      handler = new FileHandler(logfile); // this will overwrite
    }
    catch (IOException e) { //can't open log file so use console
      handler = new ConsoleHandler();
    }
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
    Locale.setDefault(locale);
    javax.swing.JComponent.setDefaultLocale(locale);
  }

  public static String lang(String key) {
    String translation = null;
    try {
      if (Globals.messages != null) {
        translation = Globals.messages.getString(key.replaceAll(" ", "_"));
      }
    }
    catch (MissingResourceException ex) {
      translation = key;

      //System.err.println("Warning: could not get translation for \""
      //                   + key + "\"");
    }
    if ((translation != null) && (translation.length() != 0)) {
      return translation.replaceAll("_", " ");
    }
    else {
      return key;
    }
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

      //System.err.println("Warning: could not get translation for \""
      //                   + key + "\"");
    }
    if ((translation != null) && (translation.length() != 0)) {
      return translation.replaceAll("_", " ");
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


  //========================================================
  // lot of abreviations in medline
  // PKC etc convert to {PKC} ...
  //========================================================
  static Pattern titleCapitalPattern = Pattern.compile("[A-Z]+");

  public static String putBracesAroundCapitals(String title) {
    StringBuffer buf = new StringBuffer();

    Matcher mcr = Globals.titleCapitalPattern.matcher(title.substring(1));
    boolean found = false;
    while ( (found = mcr.find())) {
      String replaceStr = mcr.group();
      mcr.appendReplacement(buf, "{" + replaceStr + "}");
    }
    mcr.appendTail(buf);
    String titleCap = title.substring(0, 1) + buf.toString();
    return titleCap;
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
    if (extension == null) {
      off = new OpenFileFilter();
    }
    else if (!extension.equals(NONE)) {
      off = new OpenFileFilter(extension);

    }
    if (ON_MAC) {
      return getNewFileForMac(owner, prefs, directory, extension, dialogType,
                              updateWorkingDirectory, dirOnly, off);
    }

    JFileChooser fc;
    fc = new JabRefFileChooser(directory);

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
        !selectedFile.getPath().endsWith(extension)) {

      selectedFile = new File(selectedFile.getPath() + extension);
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
      XML_CHARS = new HashMap();
  static {

    // Start the thread that monitors file time stamps.
    //Util.pr("Starting FileUpdateMonitor thread. Globals line 293.");
    fileUpdateMonitor.start();

    try {
      SHORTCUT_MASK = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    } catch (Throwable t) {

    }

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
    XML_CHARS.put("\\{\\\\\\\uFFFDe\\}", "&#x00E9;");
    XML_CHARS.put("\\{\\\\\\\uFFFDE\\}", "&#x00C9;");
    XML_CHARS.put("\\{\\\\\\\uFFFDi\\}", "&#x00ED;");
    XML_CHARS.put("\\{\\\\\\\uFFFDI\\}", "&#x00CD;");
    XML_CHARS.put("\\{\\\\\\\uFFFDo\\}", "&#x00F3;");
    XML_CHARS.put("\\{\\\\\\\uFFFDO\\}", "&#x00D3;");
    XML_CHARS.put("\\{\\\\\\\uFFFDu\\}", "&#x00FA;");
    XML_CHARS.put("\\{\\\\\\\uFFFDU\\}", "&#x00DA;");
    XML_CHARS.put("\\{\\\\\\\uFFFDa\\}", "&#x00E1;");
    XML_CHARS.put("\\{\\\\\\\uFFFDA\\}", "&#x00C1;");

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

    //XML_CHARS.put("\\u00E1", "&#x00E1;");
  }

}
