/*
  Copyright (C) 2003 Morten O. Alver

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

  Note:
  Modified for use in JabRef.

 */

package net.sf.jabref;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.net.URL;
import javax.swing.*;

public class GUIGlobals {

  /*
   * Static variables for graphics files and keyboard shortcuts.
   */

         // for debugging
  static int teller = 0;

  // HashMap containing refs to all open BibtexDatabases.
  //static HashMap frames = new HashMap();

  // Frame titles.
  public static String
      frameTitle = "JabRef",
//      version = "1.8b",
      version = Globals.VERSION,
      stringsTitle = "Strings for database",
      //untitledStringsTitle = stringsTitle + Globals.lang("untitled"),
      untitledTitle = "untitled",
      helpTitle = "JabRef help",
      TYPE_HEADER = "entrytype",
      NUMBER_COL = "#",
      encPrefix = "Encoding: ", // Part of the signature in written bib files.
      linuxDefaultLookAndFeel = "com.jgoodies.plaf.plastic.Plastic3DLookAndFeel",
      //"com.shfarr.ui.plaf.fh.FhLookAndFeel",
//"net.sourceforge.mlf.metouia.MetouiaLookAndFeel",
//"org.compiere.plaf.CompiereLookAndFeel",
      windowsDefaultLookAndFeel = "com.jgoodies.plaf.windows.ExtWindowsLookAndFeel";

  public static Font CURRENTFONT,
      typeNameFont,
      jabRefFont,
      fieldNameFont;

  // Signature written at the top of the .bib file.
  public static final String SIGNATURE =
      "This file was created with JabRef";

  // Size of help window.
  static Dimension
      helpSize = new Dimension(700, 600),
      aboutSize = new Dimension(600, 265),
      searchPaneSize = new Dimension(430, 70),
      searchFieldSize = new Dimension(215, 25);

  // Divider size for BaseFrame split pane. 0 means non-resizable.
  public static final int
      SPLIT_PANE_DIVIDER_SIZE = 4,
      SPLIT_PANE_DIVIDER_LOCATION = 145,
      TABLE_ROW_PADDING = 4,
      KEYBIND_COL_0 = 200,
      KEYBIND_COL_1 = 80, // Added to the font size when determining table
      PREVIEW_PANEL_PADDING = 15, // Extra room given to the preview editor, in addition to its own
      PREVIEW_PANEL_HEIGHT = 200,
      MAX_CONTENT_SELECTOR_WIDTH = 240; // The max width of the combobox for content selectors.
  // calculated preferred size
  //public static final int[] PREVIEW_HEIGHT = {115, 300};
  // row height
  public static final double
      VERTICAL_DIVIDER_LOCATION = 0.4;

  // File names.
  public static String //configFile = "preferences.dat",
      backupExt = ".bak",
      tempExt = ".tmp",
      defaultDir = ".";

  // Image paths.
  public static String
      imageSize = "24",
      extension = ".gif",
      ex = imageSize + extension,
      pre = "/images/",
      helpPre = "/help/",
      fontPath = "/images/font/";

    static HashMap tableIcons = new HashMap(); // Contains table icon mappings. Set up
    // further below.
    public static JLabel incompleteLabel; // JLabel with icon signaling an incomplete entry.
    public static Color activeEditor = new Color(230, 230, 255);
    public static final String[] DEFAULT_INSPECTION_FIELDS = new String[]
        {"author", "title", "year"};

    public static JLabel getTableIcon(String fieldType) {
        Object o = tableIcons.get(fieldType);
        if (o == null) {
            Globals.logger("Error: no table icon defined for type '"+fieldType+"'.");
            return null;
        } else return (JLabel)o;
    }


  public static URL

          openIconFile = GUIGlobals.class.getResource(pre + "fldr_obj.gif"),
          editIconFile = GUIGlobals.class.getResource(pre + "edittsk_tsk.gif"),
          saveIconFile = GUIGlobals.class.getResource(pre + "save_edit.gif"),
          saveAsIconFile = GUIGlobals.class.getResource(pre + "saveas_edit.gif"),
          prefsIconFile = GUIGlobals.class.getResource(pre + "configure2.png"),
          newIconFile = GUIGlobals.class.getResource(pre + "new_page.gif"),
          undoIconFile = GUIGlobals.class.getResource(pre + "undo_edit.gif"),
          redoIconFile = GUIGlobals.class.getResource(pre + "redo_edit.gif"),
          preambleIconFile = GUIGlobals.class.getResource(pre + "preamble.png"),
          addIconFile = GUIGlobals.class.getResource(pre + "plus.gif"),
          delRowIconFile = GUIGlobals.class.getResource(pre + "minus.gif"),
          showReqIconFile = GUIGlobals.class.getResource(pre + "reqIcon.png"),
          showOptIconFile = GUIGlobals.class.getResource(pre + "optIcon.png"),
          showGenIconFile = GUIGlobals.class.getResource(pre + "absIcon.png"),
          showAbsIconFile = GUIGlobals.class.getResource(pre + "genIcon.png"),
          sourceIconFile = GUIGlobals.class.getResource(pre + "viewsource.gif"),
          copyIconFile = GUIGlobals.class.getResource(pre + "copy_edit.gif"),
          cutIconFile = GUIGlobals.class.getResource(pre + "cut_edit.gif"),
          copyKeyIconFile = GUIGlobals.class.getResource(pre + "copy_edit.gif"),
          genKeyIconFile = GUIGlobals.class.getResource(pre + "wizard.png"),
          lyxIconFile = GUIGlobals.class.getResource(pre + "lyx2.png"),
          backIconFile = GUIGlobals.class.getResource(pre + "backward_nav.gif"),
          forwardIconFile = GUIGlobals.class.getResource(pre + "forward_nav.gif"),
          contentsIconFile = GUIGlobals.class.getResource(pre + "toc_closed.gif"),
          removeIconFile = GUIGlobals.class.getResource(pre + "delete_edit.gif"),
          upIconFile = GUIGlobals.class.getResource(pre + "prev_nav.gif"),
          downIconFile = GUIGlobals.class.getResource(pre + "next_nav.gif"),
          stringsIconFile = GUIGlobals.class.getResource(pre + "strings.png"),
          groupsIconFile = GUIGlobals.class.getResource(pre + "queue.png"),
          groupsHighlightMatchingAnyFile = GUIGlobals.class.getResource(pre + "groupsHighlightAny.png"),
          groupsHighlightMatchingAllFile = GUIGlobals.class.getResource(pre + "groupsHighlightAll.png"),
          closeIconFile = GUIGlobals.class.getResource(pre + "fileclose.png"),
          close2IconFile = GUIGlobals.class.getResource(pre + "fileclose2.png"),
          refreshSmallIconFile = GUIGlobals.class.getResource(pre + "refresh_nav.gif"),
          helpSmallIconFile = GUIGlobals.class.getResource(pre + "view.gif"),
          helpIconFile = GUIGlobals.class.getResource(pre + "view.gif"),
          aboutIcon = GUIGlobals.class.getResource(pre + "view.gif"),
          helpContentsIconFile = GUIGlobals.class.getResource(pre + "contents2.png"),
          newSmallIconFile = GUIGlobals.class.getResource(pre + "new_page.gif"),
          pasteIconFile = GUIGlobals.class.getResource(pre + "paste_edit.gif"),
          editEntryIconFile = GUIGlobals.class.getResource(pre + "DocumentDraw.gif"),
          searchIconFile = GUIGlobals.class.getResource(pre + "search.gif"),
          previewIconFile = GUIGlobals.class.getResource(pre + "preview.png"),
          autoGroupIcon = GUIGlobals.class.getResource(pre + "addtsk_tsk.gif"),
          wwwIcon = GUIGlobals.class.getResource(pre + "www.png"),
          wwwCiteSeerIcon = GUIGlobals.class.getResource(pre + "wwwciteseer.png"),
          fetchMedlineIcon = GUIGlobals.class.getResource(pre + "goto.png"),
          fetchHourglassIcon = GUIGlobals.class.getResource(pre + "Hourglass.png"),
          pdfIcon = GUIGlobals.class.getResource(pre + "pdf.png"),
          pdfSmallIcon = GUIGlobals.class.getResource(pre + "pdf_small.gif"),
          sheetIcon = GUIGlobals.class.getResource(pre + "defaults_ps.gif"),
          doiIcon = GUIGlobals.class.getResource(pre + "doi.png"),
          doiSmallIcon = GUIGlobals.class.getResource(pre + "doismall.png"),
          psIcon = GUIGlobals.class.getResource(pre + "postscript.png"),
          incompleteIcon = GUIGlobals.class.getResource(pre + "exclamation.gif"),
          winEdtIcon = GUIGlobals.class.getResource(pre + "winedt.png"),
          jabreflogo = GUIGlobals.class.getResource(pre + "JabRef-icon.png"),
          completeTagIcon = GUIGlobals.class.getResource(pre + "completeItem.png"),
          wrongTagIcon = GUIGlobals.class.getResource(pre + "wrongItem.png"),
          clearInputArea = GUIGlobals.class.getResource(pre + "new_page.gif"),
          markIcon = GUIGlobals.class.getResource(pre + "mark.png"),
          unmarkIcon = GUIGlobals.class.getResource(pre + "unmark.png"),
          newBibFile = GUIGlobals.class.getResource(pre + "newBibFile.png"),
          integrityCheck = GUIGlobals.class.getResource(pre + "integrity.png"),
          integrityInfo = GUIGlobals.class.getResource(pre + "messageInfo.png"),
          integrityWarn = GUIGlobals.class.getResource(pre + "messageWarn.png"),
          integrityFail = GUIGlobals.class.getResource(pre + "messageFail.png"),
          duplicateIcon = GUIGlobals.class.getResource(pre + "duplicate.png"),
          emacsIcon = GUIGlobals.class.getResource(pre + "emacs.png");
  
  public static ImageIcon
  	groupRefiningIcon = new ImageIcon(GUIGlobals.class.getResource(pre +"groupRefining.png")),
  	groupIncludingIcon = new ImageIcon(GUIGlobals.class.getResource(pre +"groupIncluding.png")),
  	groupRegularIcon = null;

    /*public static incompleteEntryIcon = new ImageIcon(incompleteIcon);
    static {
      incompleteEntryIcon.setTool
    }*/

// Help files (in HTML format):
  public static String
      baseFrameHelp = "BaseFrameHelp.html",
      entryEditorHelp = "EntryEditorHelp.html",
      stringEditorHelp = "StringEditorHelp.html",
      helpContents = "Contents.html",
      searchHelp = "SearchHelp.html",
      groupsHelp = "GroupsHelp.html",
      customEntriesHelp = "CustomEntriesHelp.html",
      contentSelectorHelp = "ContentSelectorHelp.html",
      labelPatternHelp = "LabelPatterns.html",
      ownerHelp = "OwnerHelp.html",
      timeStampHelp = "TimeStampHelp.html",
        pdfHelp = "ExternalFiles.html",
      exportCustomizationHelp = "CustomExports.html",
      importCustomizationHelp = "CustomImports.html",
      medlineHelp = "MedlineHelp.html",
        citeSeerHelp = "CiteSeerHelp.html",
      generalFieldsHelp = "GeneralFields.html",
//      searchHelp = "SearchHelp.html",
      aboutPage = "About.html",
      shortPlainImport="ShortPlainImport.html",
      importInspectionHelp = "ImportInspectionDialog.html",
      shortIntegrityCheck="ShortIntegrityCheck.html",
        remoteHelp = "RemoteHelp.html",
        journalAbbrHelp = "JournalAbbreviations.html";


// Colors.
  public static Color
      lightGray = new Color(230, 30, 30), // Light gray background
      validFieldColor = new Color(100, 100, 150), // Empty field, blue.
      nullFieldColor = new Color(75, 130, 95), // Valid field, green.
      invalidFieldColor = new Color(141, 0, 61), // Invalid field, red.
//	invalidFieldColor = new Color(210, 70, 70), // Invalid field, red.
      validFieldBackground = Color.white, // Valid field backgnd.
//invalidFieldBackground = new Color(210, 70, 70), // Invalid field backgnd.
      invalidFieldBackground = new Color(255, 100, 100), // Invalid field backgnd.
      gradientGray = new Color(112, 121, 165),  // Title bar gradient color, sidepaneheader
      gradientBlue = new Color(0, 27, 102),  // Title bar gradient color, sidepaneheader
      //activeTabbed = Color.black,  // active Database (JTabbedPane)
      //inActiveTabbed = Color.gray.darker(),  // inactive Database
      activeTabbed = validFieldColor.darker(),  // active Database (JTabbedPane)
      inActiveTabbed = Color.black,  // inactive Database
      infoField = new Color(254, 255, 225) // color for an info field
      ;


  public static String META_FLAG = "jabref-meta: ";
  public static String META_FLAG_OLD = "bibkeeper-meta: ";
  public static String ENTRYTYPE_FLAG = "jabref-entrytype: ";
  public static String KEY_FIELD = "bibtexkey";
  public static String[] ALL_FIELDS = new String[] {
      "author",
      "editor",
      "title",
      "year",
      "pages",
      "publisher",
      "journal",
      "volume",
      "month",
      "note",
      "edition",
      "number",
      "chapter",
      "series",
      "type",
      "address",
      "location",
      "annote",
      "booktitle",
      "crossref",
      "howpublished",
      "institution",
      "key",
      "organization",
      "school",
      "abstract",
      "url",
          "citeseerurl",
      "pdf",
      "comment",
      "bibtexkey",
      "keywords",
      "doi",
      "eid",
      "search",
          "citeseercitationcount"
  };

  public static final Map FIELD_DISPLAYS;
  static {
      Arrays.sort(ALL_FIELDS);
          FIELD_DISPLAYS = new HashMap();
          FIELD_DISPLAYS.put("citeseercitationcount","Popularity");
  }


// These are the fields that BibTex might want to treat, so these
// must conform to BibTex rules.
  public static String[] BIBTEX_STANDARD_FIELDS = new String[] {
      "author",
      "editor",
      "title",
      "year",
      "pages",
      "month",
      "note",
      "publisher",
      "journal",
      "volume",
      "edition",
      "number",
      "chapter",
      "series",
      "type",
      "address",
      //? "annote",
      "booktitle",
      "crossref",
      "howpublished",
      "institution",
      "key",
      "organization",
      "school",
      "bibtexkey",
      "doi",
      "eid",
      "date"
  };

  // These fields will not be saved to the .bib file.
  public static String[] NON_WRITABLE_FIELDS = new String[] {
      Globals.SEARCH,
      Globals.GROUPSEARCH
  };

  // These fields will not be shown inside the source editor panel.
  public static String[] NON_DISPLAYABLE_FIELDS = new String[] {
      Globals.MARKED,
      Globals.SEARCH,
      Globals.GROUPSEARCH
  };

     public static boolean isWriteableField(String field) {
       for (int i = 0; i < NON_WRITABLE_FIELDS.length; i++) {
         if (NON_WRITABLE_FIELDS[i].equals(field)) {
           return false;
         }
       }
       return true;
     }

     public static boolean isDisplayableField(String field) {
       for (int i = 0; i < NON_DISPLAYABLE_FIELDS.length; i++) {
         if (NON_DISPLAYABLE_FIELDS[i].equals(field)) {
           return false;
         }
       }
       return true;
     }

  /**
   * Returns true if the given field is a standard Bibtex field.
   *
   * @param field a <code>String</code> value
   * @return a <code>boolean</code> value
   */
  public static boolean isStandardField(String field) {
    for (int i = 0; i < BIBTEX_STANDARD_FIELDS.length; i++) {
      if (BIBTEX_STANDARD_FIELDS[i].equals(field)) {
        return true;
      }
    }
    return false;
  }

  public static double DEFAULT_FIELD_WEIGHT = 1,
        MAX_FIELD_WEIGHT = 2;

  public static Double
      SMALL_W = new Double(0.30),
      MEDIUM_W = new Double(0.5),
      LARGE_W = new Double(1.5);
  public static final double PE_HEIGHT = 2;

// Size constants for EntryTypeForm; small, medium and large.
  public static int[] FORM_WIDTH = new int[] {
      500, 650, 820};
  public static int[] FORM_HEIGHT = new int[] {
      90, 110, 130};

// Constants controlling formatted bibtex output.
  public static final int
      INDENT = 4,
      LINE_LENGTH = 65; // Maximum

  public static int DEFAULT_FIELD_LENGTH = 100,
      NUMBER_COL_LENGTH = 32,
      WIDTH_ICON_COL = 19;

  // Column widths for export customization dialog table:
  public static final int
      EXPORT_DIALOG_COL_0_WIDTH = 50,
      EXPORT_DIALOG_COL_1_WIDTH = 200,
      EXPORT_DIALOG_COL_2_WIDTH = 30;

  // Column widths for import customization dialog table:
  public static final int
      IMPORT_DIALOG_COL_0_WIDTH = 200,
      IMPORT_DIALOG_COL_1_WIDTH = 80,
      IMPORT_DIALOG_COL_2_WIDTH = 200,
      IMPORT_DIALOG_COL_3_WIDTH = 200;

  public static final Map FIELD_WEIGHT;
  public static final Map FIELD_EXTRAS, LANGUAGES;
  public static Map fieldLength = new HashMap();
  static {

    LANGUAGES = new HashMap();
    // LANGUAGES contains mappings for supported languages.
    LANGUAGES.put("English", "en");
    LANGUAGES.put("Deutsch", "de");
    LANGUAGES.put("Fran\u00E7ais", "fr");
    LANGUAGES.put("Norsk", "no");

    FIELD_EXTRAS = new HashMap();
    // fieldExtras contains mappings to tell the EntryEditor to add a specific
    // function to this field, for instance a "browse" button for the "pdf" field.
    FIELD_EXTRAS.put("pdf", "browseDoc");
    FIELD_EXTRAS.put("ps", "browseDocZip");
    FIELD_EXTRAS.put("url", "external");
    FIELD_EXTRAS.put("citeseerurl", "external");
    FIELD_EXTRAS.put("doi", "external");
    FIELD_EXTRAS.put("journal", "journalNames");
    //FIELD_EXTRAS.put("keywords", "selector");


    fieldLength.put("author", new Integer(280));
    fieldLength.put("editor", new Integer(280));
    fieldLength.put("title", new Integer(400));
    fieldLength.put("abstract", new Integer(400));
    fieldLength.put("booktitle", new Integer(175));
    fieldLength.put("year", new Integer(60));
    fieldLength.put("volume", new Integer(60));
    fieldLength.put("number", new Integer(60));
    fieldLength.put("entrytype", new Integer(75));
    fieldLength.put("search", new Integer(75));
    fieldLength.put("citeseercitationcount", new Integer(75));
    fieldLength.put(NUMBER_COL, new Integer(32));

    FIELD_WEIGHT = new HashMap();
    FIELD_WEIGHT.put("author", MEDIUM_W);
    FIELD_WEIGHT.put("year", SMALL_W);
    FIELD_WEIGHT.put("pages", SMALL_W);
    FIELD_WEIGHT.put("month", SMALL_W);
    FIELD_WEIGHT.put("url", SMALL_W);
    FIELD_WEIGHT.put("citeseerurl", SMALL_W);
    FIELD_WEIGHT.put("crossref", SMALL_W);
    FIELD_WEIGHT.put("note", MEDIUM_W);
    FIELD_WEIGHT.put("publisher", MEDIUM_W);
    FIELD_WEIGHT.put("journal", SMALL_W);
    FIELD_WEIGHT.put("volume", SMALL_W);
    FIELD_WEIGHT.put("edition", SMALL_W);
    FIELD_WEIGHT.put("keywords", SMALL_W);
    FIELD_WEIGHT.put("doi", SMALL_W);
    FIELD_WEIGHT.put("eid", SMALL_W);
    FIELD_WEIGHT.put("pdf", SMALL_W);
    FIELD_WEIGHT.put("number", SMALL_W);
    FIELD_WEIGHT.put("chapter", SMALL_W);
    FIELD_WEIGHT.put("editor", MEDIUM_W);
    FIELD_WEIGHT.put("series", SMALL_W);
    FIELD_WEIGHT.put("type", SMALL_W);
    FIELD_WEIGHT.put("address", SMALL_W);
    FIELD_WEIGHT.put("howpublished", MEDIUM_W);
    FIELD_WEIGHT.put("institution", MEDIUM_W);
    FIELD_WEIGHT.put("organization", MEDIUM_W);
    FIELD_WEIGHT.put("school", MEDIUM_W);
    FIELD_WEIGHT.put("comment", MEDIUM_W);
    FIELD_WEIGHT.put("abstract", LARGE_W);
    FIELD_WEIGHT.put("annote", LARGE_W);
    FIELD_WEIGHT.put("citeseercitationcount", SMALL_W);
    FIELD_WEIGHT.put("owner", SMALL_W);
    FIELD_WEIGHT.put("timestamp", SMALL_W);
    //FIELD_WEIGHT = Collections.unmodifiableMap(FIELD_WEIGHT);
  }

    /*
    public static int getPreferredFieldLength(String name) {
    int l = DEFAULT_FIELD_LENGTH;
    Object o = fieldLength.get(name.toLowerCase());
    if (o != null)
    l = ((Integer)o).intValue();
    return l;
    }*/

  public static double getFieldWeight(String name) {
    double l = DEFAULT_FIELD_WEIGHT;
    Object o = FIELD_WEIGHT.get(name.toLowerCase());
    if (o != null) {
      l = ( (Double) o).doubleValue();
    }
    return l;
  }

  public static void setFieldWeight(String fieldName, double weight) {
      FIELD_WEIGHT.put(fieldName, new Double(weight));
  }

  /** returns the path to language independent help files */
  public static String getLocaleHelpPath()
  {
    JabRefPreferences prefs = JabRefPreferences.getInstance() ;
    String middle = prefs.get("language")+"/";
    if (middle.equals("en/")) middle = ""; // english in base help dir.

    return (helpPre + middle );
  }


  /**
   * Perform initializations that are only used in graphical mode. This is to prevent
   * the "Xlib: connection to ":0.0" refused by server" error when access to the X server
   * on Un*x is unavailable.
   */
  public static void init() {
    typeNameFont = new Font("arial", Font.ITALIC+Font.BOLD, 24);
    fieldNameFont = new Font("arial", Font.ITALIC+Font.BOLD, 14);
    incompleteLabel = new JLabel(new ImageIcon(GUIGlobals.incompleteIcon));
    incompleteLabel.setToolTipText(Globals.lang("Entry is incomplete"));
    JLabel lab;
    lab = new JLabel(new ImageIcon(pdfIcon));
    lab.setToolTipText(Globals.lang("Open")+" PDF");
    tableIcons.put("pdf", lab);
    lab = new JLabel(new ImageIcon(wwwIcon));
    lab.setToolTipText(Globals.lang("Open")+" URL");
    tableIcons.put("url", lab);
    lab = new JLabel(new ImageIcon(wwwCiteSeerIcon));
    lab.setToolTipText(Globals.lang("Open")+" CiteSeer URL");
    tableIcons.put("citeseerurl", lab);
    lab = new JLabel(new ImageIcon(doiSmallIcon));
    lab.setToolTipText(Globals.lang("Open")+" DOI "+Globals.lang("web link"));
    tableIcons.put("doi", lab);
    lab = new JLabel(new ImageIcon(psIcon));
    lab.setToolTipText(Globals.lang("Open")+" PS");
    tableIcons.put("ps", lab);

    //jabRefFont = new Font("arial", Font.ITALIC/*+Font.BOLD*/, 20);
  }

}
