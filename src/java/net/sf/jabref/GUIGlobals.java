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

import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import java.awt.*;
import javax.swing.KeyStroke;
import java.util.*;
import java.net.URL;

public class GUIGlobals {

    /* 
     * Static variables for graphics files and keyboard shortcuts.
     */

    // for debugging
    static int teller = 0;

    // HashMap containing refs to all open BibtexDatabases.
    static HashMap frames = new HashMap();

    // Frame titles.
    static String 
	frameTitle = "JabRef",
	stringsTitle = Globals.lang("Strings for database")+": ",
	untitledStringsTitle = stringsTitle+Globals.lang("untitled"),
	helpTitle = "JabRef help";

    // Signature written at the top of the .bib file.
    public static final String SIGNATURE =
	"This file was created with JabRef.\n\n",
	SHOW_ALL = "All entries";

    // Size of help window.
    static Dimension
	helpSize = new Dimension(700, 600),
	aboutSize = new Dimension(600, 265),
	searchPaneSize = new Dimension(430, 70),
	searchFieldSize = new Dimension(215, 25);

    // Divider size for BaseFrame split pane. 0 means non-resizable.
    public static final int 
	SPLIT_PANE_DIVIDER_SIZE = 2,
	SPLIT_PANE_DIVIDER_LOCATION = 80,
	GROUPS_VISIBLE_ROWS = 8;
    // File names.
    public static String //configFile = "preferences.dat",
	backupExt = ".bak",
	tempExt = ".tmp",
	defaultDir = ".";

    // Image paths.
    public static String 	
	imageSize = "24",
	extension = ".gif",
	ex = imageSize+extension,
	pre = "/images/",
	helpPre = "/help/";

    public static URL 
	//appIconFile = GUIGlobals.class.getResource(pre+"ikon.jpg"),
	openIconFile = GUIGlobals.class.getResource(pre+"Open.gif"),
	saveIconFile = GUIGlobals.class.getResource(pre+"Save.gif"),
	saveAsIconFile = GUIGlobals.class.getResource(pre+"SaveAs.gif"),
	prefsIconFile = GUIGlobals.class.getResource(pre+"Options.gif"),
	newIconFile = GUIGlobals.class.getResource(pre+"New.gif"),
	undoIconFile = GUIGlobals.class.getResource(pre+"Undo.gif"),
	redoIconFile = GUIGlobals.class.getResource(pre+"Redo.gif"),
	preambleIconFile = GUIGlobals.class.getResource(pre+"Preamble.gif"),
	addIconFile = GUIGlobals.class.getResource(pre+"UpdateRow.gif"),
	showReqIconFile = GUIGlobals.class.getResource(pre+"r_icon.gif"),
	showOptIconFile = GUIGlobals.class.getResource(pre+"o_icon.gif"),
	showGenIconFile = GUIGlobals.class.getResource(pre+"g_icon.gif"),
	sourceIconFile = GUIGlobals.class.getResource(pre+"viewsource.gif"),
	copyIconFile = GUIGlobals.class.getResource(pre+"Copy.gif"),
	cutIconFile = GUIGlobals.class.getResource(pre+"Cut.gif"),
	copyKeyIconFile = GUIGlobals.class.getResource(pre+"CopyKey.gif"),
	genKeyIconFile = GUIGlobals.class.getResource(pre+"GenKey.gif"),
	lyxIconFile = GUIGlobals.class.getResource(pre+"LyX.gif"),	
	backIconFile = GUIGlobals.class.getResource(pre+"VCRBack.gif"),
	forwardIconFile = GUIGlobals.class.getResource(pre+"VCRForward.gif"),
	contentsIconFile = GUIGlobals.class.getResource(pre+"Contents.gif"),
	removeIconFile = GUIGlobals.class.getResource(pre+"Delete.gif"),
	upIconFile = GUIGlobals.class.getResource(pre+"Up.gif"),
	downIconFile = GUIGlobals.class.getResource(pre+"Down.gif"),
	stringsIconFile = GUIGlobals.class.getResource(pre+"Strings.gif"),
	groupsIconFile = GUIGlobals.class.getResource(pre+"Groups.gif"),
	closeIconFile = GUIGlobals.class.getResource(pre+"Close.gif"),
	refreshSmallIconFile = GUIGlobals.class.getResource(pre+"GreenFlag.gif"),
	helpSmallIconFile = GUIGlobals.class.getResource(pre+"HelpSmall.gif"),
	helpIconFile = GUIGlobals.class.getResource(pre+"Help.gif"),
	newSmallIconFile = GUIGlobals.class.getResource(pre+"NewSmall.gif"),
	pasteIconFile = GUIGlobals.class.getResource(pre+"Paste.gif"),
	editEntryIconFile = GUIGlobals.class.getResource(pre+"DocumentDraw.gif"),
	searchIconFile = GUIGlobals.class.getResource(pre+"Binocular.gif");
	
    // Help files (in HTML format):
    public static URL
	baseFrameHelp = GUIGlobals.class.getResource(helpPre+"GUIGlobalsHelp.html"),
	entryEditorHelp = GUIGlobals.class.getResource(helpPre+"EntryEditorHelp.html"),
	stringEditorHelp = GUIGlobals.class.getResource(helpPre+"StringEditorHelp.html"),
	helpContents = GUIGlobals.class.getResource(helpPre+"Contents.html"),
	searchHelp = GUIGlobals.class.getResource(helpPre+"SearchHelp.html"),
	groupsHelp = GUIGlobals.class.getResource(helpPre+"GroupsHelp.html"),
	aboutPage = GUIGlobals.class.getResource(helpPre+"About.html");


    // Keystrokes for Entry editor.
    public static String 
 	openKey = "control O",
 	closeKey = "control Q",
 	storeFieldKey = "control S",
 	copyKeyKey = "control K",
 	showReqKey = "control R",
 	showOptKey = "control O",
	showGenKey = "control G",
	addKey = "control N",
	removeKey = "shift DELETE",
	upKey = "control UP",
	downKey = "control DOWN";

    // The following defines the mnemonic keys for menu items.
    public static Integer 
	openKeyCode = new Integer(KeyEvent.VK_O),
	//	newKeyCode = new Integer(KeyEvent.VK_N),
	saveKeyCode = new Integer(KeyEvent.VK_S),
	copyKeyCode = new Integer(KeyEvent.VK_K),
	closeKeyCode = new Integer(KeyEvent.VK_Q),
	showReqKeyCode = new Integer(KeyEvent.VK_R),
	showOptKeyCode = new Integer(KeyEvent.VK_O),
	showGenKeyCode = new Integer(KeyEvent.VK_G),
	newEntryKeyCode = new Integer(KeyEvent.VK_N),
	newBookKeyCode = new Integer(KeyEvent.VK_B),
	newArticleKeyCode = new Integer(KeyEvent.VK_A),
	newPhdthesisKeyCode = new Integer(KeyEvent.VK_T),
	newInBookKeyCode = new Integer(KeyEvent.VK_I),
	newMasterKeyCode = new Integer(KeyEvent.VK_M),
	newProcKeyCode = new Integer(KeyEvent.VK_P);
    //    	newInProcKeyCode = new Integer(KeyEvent.VK_M);

    
    // The following defines the accelerator keys for menu items,
    // corresponding to the letters for mnemonics.
    public static KeyStroke 
	generateKeyStroke = KeyStroke.getKeyStroke("control G"),
	exitDialog = KeyStroke.getKeyStroke("ESCAPE"),
	copyStroke = KeyStroke.getKeyStroke("control C"),
	pasteStroke = KeyStroke.getKeyStroke("control V"),
	undoStroke = KeyStroke.getKeyStroke("control Z"),
	redoStroke = KeyStroke.getKeyStroke("control Y"),
	selectAllKeyStroke = KeyStroke.getKeyStroke("control A"),
	editEntryKeyStroke = KeyStroke.getKeyStroke("control D"),
	helpKeyStroke = KeyStroke.getKeyStroke("F1"),
	//setupTableKeyStroke = KeyStroke.getKeyStroke(""),
	editPreambleKeyStroke = KeyStroke.getKeyStroke("control P"),
	editStringsKeyStroke = KeyStroke.getKeyStroke("control shift S"),
	simpleSearchKeyStroke = KeyStroke.getKeyStroke("control F"),
	autoCompKeyStroke = KeyStroke.getKeyStroke("control W"),
	showGroupsKeyStroke = KeyStroke.getKeyStroke("control shift G"),
	//newKeyStroke = KeyStroke.getKeyStroke(newKeyCode.intValue(), ActionEvent.CTRL_MASK),
	saveKeyStroke = KeyStroke.getKeyStroke(saveKeyCode.intValue(), ActionEvent.CTRL_MASK),
	openKeyStroke = KeyStroke.getKeyStroke(openKeyCode.intValue(), ActionEvent.CTRL_MASK),
	closeKeyStroke = KeyStroke.getKeyStroke(closeKeyCode.intValue(), ActionEvent.CTRL_MASK),
	newEntryKeyStroke = KeyStroke.getKeyStroke(newEntryKeyCode.intValue(), ActionEvent.CTRL_MASK),
	removeEntryKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, ActionEvent.SHIFT_MASK),
	newBookKeyStroke = KeyStroke.getKeyStroke(newBookKeyCode.intValue(),
						  ActionEvent.SHIFT_MASK | ActionEvent.CTRL_MASK),
	newArticleKeyStroke = KeyStroke.getKeyStroke(newArticleKeyCode.intValue(),
						     ActionEvent.SHIFT_MASK | ActionEvent.CTRL_MASK),
	newPhdthesisKeyStroke = KeyStroke.getKeyStroke(newPhdthesisKeyCode.intValue(),
						       ActionEvent.SHIFT_MASK | ActionEvent.CTRL_MASK),
	newInBookKeyStroke = KeyStroke.getKeyStroke(newInBookKeyCode.intValue(),
						    ActionEvent.SHIFT_MASK | ActionEvent.CTRL_MASK),
	newMasterKeyStroke = KeyStroke.getKeyStroke(newMasterKeyCode.intValue(),
						    ActionEvent.SHIFT_MASK | ActionEvent.CTRL_MASK),
	newProcKeyStroke = KeyStroke.getKeyStroke(newProcKeyCode.intValue(),
						  ActionEvent.SHIFT_MASK | ActionEvent.CTRL_MASK),
	newUnpublKeyStroke = KeyStroke.getKeyStroke("control shift U"),
	switchPanelLeft = KeyStroke.getKeyStroke("control shift LEFT"),
	switchPanelRight = KeyStroke.getKeyStroke("control shift RIGHT");
   

    // Colors.
    public static Color
	nullFieldColor = new Color(100, 100, 150), // Empty field, blue.
	validFieldColor = new Color(75, 130, 75), // Valid field, green.
	invalidFieldColor = new Color(141,0,61), // Invalid field, red.	
//	invalidFieldColor = new Color(210, 70, 70), // Invalid field, red.
	validFieldBackground = Color.white, // Valid field backgnd.
	//invalidFieldBackground = new Color(210, 70, 70), // Invalid field backgnd.
invalidFieldBackground = new Color(141,0,61), // Invalid field backgnd.
	tableBackground = Color.white, // Background color for the entry table.
	tableReqFieldBackground = new Color(235, 235, 255),
	tableOptFieldBackground = new Color(230, 255, 230),
	tableIncompleteEntryBackground = new Color(250, 175, 175),
	maybeIncompleteEntryBackground = new Color(255, 255, 200),
	grayedOutBackground = new Color(210, 210, 210),
	grayedOutText = new Color(40, 40, 40),
	veryGrayedOutBackground = new Color(180, 180, 180),
	veryGrayedOutText = new Color(40, 40, 40);

    public static String META_FLAG = "bibkeeper-meta: ";
    public static String KEY_FIELD = "bibtexkey";
    public static String[] ALL_FIELDS = new String[] {
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
	"pdf",
	"comment",
	"bibtexkey",
	"keywords",
	"doi",
	"eid",
	"search"
    };

    public static String[] NON_WRITABLE_FIELDS = new String[] {
        "search"
    } ;

    public static boolean isWriteableField(String field){
        for(int i =  0 ; i < NON_WRITABLE_FIELDS.length ; i++){
			if(NON_WRITABLE_FIELDS[i].equals(field)){
				return false ; 
            }
        }
        return true ; 
    }


    public static double DEFAULT_FIELD_WEIGHT = 1;
    public static Double 
	SMALL_W = new Double(0.30),
	MEDIUM_W = new Double(0.5),
	LARGE_W = new Double(1.5);
    public static final double PE_HEIGHT = 2;
    // Size constants for EntryTypeForm; small, medium and large.
    public static int[] FORM_WIDTH = new int[] {500, 650, 820};
    public static int[] FORM_HEIGHT = new int[] {90, 110, 130};


    // Constants controlling formatted bibtex output.
    public static final int
	INDENT = 4,
	LINE_LENGTH = 65; // Maximum

    public static int DEFAULT_FIELD_LENGTH = 100;
    public static final Map FIELD_LENGTH, FIELD_WEIGHT;
    static {
	Map fieldLength = new HashMap();
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

	FIELD_LENGTH = Collections.unmodifiableMap(fieldLength);

	Map fieldWeight = new HashMap();
	fieldWeight.put("author", MEDIUM_W);
	fieldWeight.put("year", SMALL_W);
	fieldWeight.put("pages", SMALL_W);
	fieldWeight.put("month", SMALL_W);
	fieldWeight.put("url", SMALL_W);
	fieldWeight.put("crossref", SMALL_W);
	fieldWeight.put("note", MEDIUM_W);
	fieldWeight.put("publisher", MEDIUM_W);
	fieldWeight.put("journal", SMALL_W);
	fieldWeight.put("volume", SMALL_W);
	fieldWeight.put("edition", SMALL_W);
	fieldWeight.put("keywords", SMALL_W);
	fieldWeight.put("doi", SMALL_W);
	fieldWeight.put("eid", SMALL_W);
	fieldWeight.put("pdf", SMALL_W);
	fieldWeight.put("number", SMALL_W);
	fieldWeight.put("chapter", SMALL_W);
	fieldWeight.put("editor", MEDIUM_W);
	fieldWeight.put("series", MEDIUM_W);
	fieldWeight.put("type", MEDIUM_W);
	fieldWeight.put("howpublished", MEDIUM_W);
	fieldWeight.put("institution", MEDIUM_W);
	fieldWeight.put("organization", MEDIUM_W);
	fieldWeight.put("school", MEDIUM_W);
	fieldWeight.put("comment", MEDIUM_W);
	fieldWeight.put("abstract", LARGE_W);

	FIELD_WEIGHT = Collections.unmodifiableMap(fieldWeight);
    };

    public static int getPreferredFieldLength(String name) {
	int l = DEFAULT_FIELD_LENGTH;
	Object o = FIELD_LENGTH.get(name.toLowerCase());
	if (o != null)
	    l = ((Integer)o).intValue();
	return l;
    }

    public static double getFieldWeight(String name) {
	double l = DEFAULT_FIELD_WEIGHT;
	Object o = FIELD_WEIGHT.get(name.toLowerCase());
	if (o != null)
	    l = ((Double)o).doubleValue();
	return l;
    }

}
