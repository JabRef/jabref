/*
Copyright (C) 2003 Morten O. Alver, Nizar N. Batada

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

import java.io.*;
import javax.swing.*;

import net.sf.jabref.labelPattern.DefaultLabelPatterns;
import net.sf.jabref.labelPattern.LabelPattern;
import net.sf.jabref.labelPattern.LabelPatternUtil;
import net.sf.jabref.export.CustomExportList;
import java.awt.Point;
import java.awt.Dimension;
import java.util.prefs.*;
import java.util.*;
import java.awt.event.*;
import net.sf.jabref.export.ExportComparator;

public class JabRefPreferences {

    public final String
        CUSTOM_TYPE_NAME = "customTypeName_",
        CUSTOM_TYPE_REQ = "customTypeReq_",
        CUSTOM_TYPE_OPT = "customTypeOpt_",
	CUSTOM_TAB_NAME = "customTabName_",
	CUSTOM_TAB_FIELDS = "customTabFields_";

    Preferences prefs;
    public HashMap defaults = new HashMap(),
        keyBinds = new HashMap(),
        defKeyBinds = new HashMap();
    private static final LabelPattern KEY_PATTERN = new DefaultLabelPatterns();
    private static LabelPattern keyPattern;

    // Object containing custom export formats:
    public CustomExportList customExports;

    // Object containing info about customized entry editor tabs.
    private EntryEditorTabList tabList = null;

    // The only instance of this class:
    private static JabRefPreferences INSTANCE = null;

    public static JabRefPreferences getInstance() {
	if (INSTANCE == null)
	    INSTANCE = new JabRefPreferences();
	return INSTANCE;
    }

    // The constructor is made private to enforce this as a singleton class:
    private JabRefPreferences() {

        prefs = Preferences.userNodeForPackage(JabRef.class);
        //Util.pr(prefs.toString());

        if (Globals.osName.equals(Globals.MAC)) {
            defaults.put("pdfviewer","/Applications/Preview.app");
            defaults.put("psviewer","/Applications/Preview.app");
            defaults.put("htmlviewer","/Applications/Safari.app");
        }
        else if (Globals.osName.toLowerCase().startsWith("windows")) {
          defaults.put("pdfviewer","cmd.exe /c start /b");
          defaults.put("psviewer","cmd.exe /c start /b");
          defaults.put("htmlviewer","cmd.exe /c start /b");
          defaults.put("lookAndFeel", "com.jgoodies.plaf.windows.ExtWindowsLookAndFeel");


        }

        else {
            defaults.put("pdfviewer","acroread");
            defaults.put("psviewer","gv");
            defaults.put("htmlviewer","mozilla");
            defaults.put("lookAndFeel", "com.jgoodies.plaf.plastic.Plastic3DLookAndFeel");

        }
        defaults.put("useDefaultLookAndFeel", Boolean.TRUE);
        defaults.put("lyxpipe", System.getProperty("user.home")+File.separator+".lyx/lyxpipe");
        defaults.put("posX", new Integer(0));
        defaults.put("posY", new Integer(0));
        defaults.put("sizeX", new Integer(840));
        defaults.put("sizeY", new Integer(680));
        defaults.put("autoResizeMode", new Integer(JTable.AUTO_RESIZE_OFF));
        defaults.put("tableColorCodesOn", Boolean.TRUE);
        defaults.put("namesAsIs", Boolean.FALSE);
        defaults.put("namesFf", Boolean.FALSE);
        defaults.put("language", "en");
        defaults.put("priSort", "author");
        defaults.put("priDescending", Boolean.FALSE);
        defaults.put("secSort", "year");
        defaults.put("secDescending", Boolean.TRUE);
        defaults.put("terSort", "author");
        defaults.put("terDescending", Boolean.FALSE);
        defaults.put("columnNames", "entrytype;author;title;year;journal;owner;bibtexkey");
        defaults.put("columnWidths","75;280;400;60;100;100;100");
        defaults.put("numberColWidth",new Integer(GUIGlobals.NUMBER_COL_LENGTH));
        defaults.put("workingDirectory", System.getProperty("user.home"));
        defaults.put("exportWorkingDirectory", System.getProperty("user.home"));
        defaults.put("autoOpenForm", Boolean.TRUE);
        defaults.put("entryTypeFormHeightFactor", new Integer(1));
        defaults.put("entryTypeFormWidth", new Integer(1));
        defaults.put("backup", Boolean.TRUE);
        defaults.put("openLastEdited", Boolean.TRUE);
        defaults.put("lastEdited", (String)null);
        defaults.put("stringsPosX", new Integer(0));
        defaults.put("stringsPosY", new Integer(0));
        defaults.put("stringsSizeX", new Integer(600));
        defaults.put("stringsSizeY", new Integer(400));
        defaults.put("defaultShowSource", Boolean.FALSE);
        defaults.put("defaultAutoSort", Boolean.FALSE);
        defaults.put("enableSourceEditing", Boolean.TRUE);
        defaults.put("caseSensitiveSearch", Boolean.FALSE);
        defaults.put("searchReq", Boolean.TRUE);
        defaults.put("searchOpt", Boolean.TRUE);
        defaults.put("searchGen", Boolean.TRUE);
        defaults.put("searchAll", Boolean.FALSE);
        defaults.put("incrementS", Boolean.TRUE);
        defaults.put("saveInStandardOrder", Boolean.TRUE);
        defaults.put("selectS", Boolean.FALSE);
        defaults.put("regExpSearch", Boolean.TRUE);
        defaults.put("searchPanePosX", new Integer(0));
        defaults.put("searchPanePosY", new Integer(0));
        defaults.put("autoComplete", Boolean.TRUE);
        defaults.put("autoCompFields", new byte[] {0, 1, 28});
        defaults.put("groupSelectorVisible", Boolean.TRUE);
        defaults.put("groupFloatSelections", Boolean.TRUE);
        defaults.put("groupIntersectSelections", Boolean.TRUE);
        defaults.put("groupInvertSelections", Boolean.FALSE);
        defaults.put("groupSelectMatches", Boolean.FALSE);
        defaults.put("groupsDefaultField", "keywords");
        defaults.put("groupSubgroupIndependent", Boolean.FALSE);
        defaults.put("groupSubgroupIntersection", Boolean.TRUE);
        
        defaults.put("defaultEncoding", System.getProperty("file.encoding"));
        defaults.put("winEdtPath", "C:\\Program Files\\WinEdt Team\\WinEdt\\WinEdt.exe");
        defaults.put("groupsVisibleRows", new Integer(8));
        defaults.put("defaultOwner", System.getProperty("user.name"));
        defaults.put("preserveFieldFormatting", Boolean.FALSE);
	// The general fields stuff is made obsolete by the CUSTOM_TAB_... entries.
        defaults.put("generalFields", "crossref;keywords;doi;url;citeseerurl;"+
                     "pdf;comment;owner");

	// Entry editor tab 0:
	defaults.put(CUSTOM_TAB_NAME+"0", Globals.lang("General"));
        defaults.put(CUSTOM_TAB_FIELDS+"0", "crossref;keywords;doi;url;citeseerurl;"+
                     "pdf;comment;owner");

	// Entry editor tab 1:
        defaults.put(CUSTOM_TAB_FIELDS+"1", "abstract");
	defaults.put(CUSTOM_TAB_NAME+"1", Globals.lang("Abstract"));

        //defaults.put("recentFiles", "/home/alver/Documents/bibk_dok/hovedbase.bib");
        defaults.put("historySize", new Integer(5));
        defaults.put("fontFamily", "Times");
        defaults.put("fontStyle", new Integer(java.awt.Font.PLAIN));
        defaults.put("fontSize", new Integer(12));
        defaults.put("menuFontFamily", "Times");
        defaults.put("menuFontStyle", new Integer(java.awt.Font.PLAIN));
        defaults.put("menuFontSize", new Integer(11));

        defaults.put("antialias", Boolean.TRUE);
        defaults.put("ctrlClick", Boolean.FALSE);
        defaults.put("disableOnMultipleSelection", Boolean.FALSE);
        defaults.put("pdfColumn", Boolean.TRUE);
        defaults.put("urlColumn", Boolean.TRUE);
        defaults.put("citeseerColumn", Boolean.FALSE);
        defaults.put("useOwner", Boolean.TRUE);
        defaults.put("allowTableEditing", Boolean.FALSE);
        defaults.put("dialogWarningForDuplicateKey", Boolean.TRUE);
        defaults.put("avoidOverwritingKey", Boolean.FALSE);
        defaults.put("warnBeforeOverwritingKey", Boolean.TRUE);
        defaults.put("confirmDelete", Boolean.TRUE);
        defaults.put("grayOutNonHits", Boolean.TRUE);
        defaults.put("defaultLabelPattern", "[auth][year]");

        defaults.put("preview0", "<font face=\"arial\">"
                     +"<b><i>\\bibtextype</i><a name=\"\\bibtexkey\">\\begin{bibtexkey} (\\bibtexkey)</a>"
                     +"\\end{bibtexkey}</b><br>__NEWLINE__"
                     +"\\begin{author} \\format[AuthorLastFirst,HTMLChars,AuthorAbbreviator,AuthorAndsReplacer]{\\author}<BR>\\end{author}__NEWLINE__"
                     +"\\begin{editor} \\format[AuthorLastFirst,HTMLChars,AuthorAbbreviator,AuthorAndsReplacer]{\\editor} <i>(ed.)</i><BR>\\end{editor}__NEWLINE__"
                     +"\\begin{title} \\format[HTMLChars]{\\title} \\end{title}<BR>__NEWLINE__"
                     +"\\begin{chapter} \\format[HTMLChars]{\\chapter}<BR>\\end{chapter}__NEWLINE__"
                     +"\\begin{journal} <em>\\format[HTMLChars]{\\journal}, </em>\\end{journal}__NEWLINE__"
                     +"\\begin{school} <em>\\format[HTMLChars]{\\school}, </em>\\end{school}__NEWLINE__"
                     +"\\begin{institution} <em>\\format[HTMLChars]{\\institution}, </em>\\end{institution}__NEWLINE__"
                     +"\\begin{publisher} <em>\\format[HTMLChars]{\\publisher}, </em>\\end{publisher}__NEWLINE__"
                     +"\\begin{year}<b>\\year</b>\\end{year} \\begin{volume}<i>, \\volume</i>\\end{volume} "
                     +"\\begin{pages}, \\format[FormatPagesForHTML]{\\pages} \\end{pages}"
                     +"</dd>__NEWLINE__<p></p></font>");

        defaults.put("preview1", "<font face=\"arial\">"
                       +"<b><i>\\bibtextype</i><a name=\"\\bibtexkey\">\\begin{bibtexkey} (\\bibtexkey)</a>"
                       +"\\end{bibtexkey}</b><br>__NEWLINE__"
                       +"\\begin{author} \\format[AuthorLastFirst,HTMLChars,AuthorAbbreviator,AuthorAndsReplacer]{\\author}<BR>\\end{author}__NEWLINE__"
                       +"\\begin{editor} \\format[AuthorLastFirst,HTMLChars,AuthorAbbreviator,AuthorAndsReplacer]{\\editor} <i>(ed.)</i><BR>\\end{editor}__NEWLINE__"
                       +"\\begin{title} \\format[HTMLChars]{\\title} \\end{title}<BR>__NEWLINE__"
                       +"\\begin{chapter} \\format[HTMLChars]{\\chapter}<BR>\\end{chapter}__NEWLINE__"
                       +"\\begin{journal} <em>\\format[HTMLChars]{\\journal}, </em>\\end{journal}__NEWLINE__"
                       +"\\begin{school} <em>\\format[HTMLChars]{\\school}, </em>\\end{school}__NEWLINE__"
                       +"\\begin{institution} <em>\\format[HTMLChars]{\\institution}, </em>\\end{institution}__NEWLINE__"
                       +"\\begin{publisher} <em>\\format[HTMLChars]{\\publisher}, </em>\\end{publisher}__NEWLINE__"
                       +"\\begin{year}<b>\\year</b>\\end{year} \\begin{volume}<i>, \\volume</i>\\end{volume} "
                       +"\\begin{pages}, \\format[FormatPagesForHTML]{\\pages} \\end{pages}__NEWLINE__"
                       +"\\begin{abstract}<BR><BR><b>Abstract: </b> \\format[HTMLChars]{\\abstract} \\end{abstract}"
                       +"</dd>__NEWLINE__<p></p></font>");

        //defaults.put("tempDir", System.getProperty("java.io.tmpdir"));
        //Util.pr(System.getProperty("java.io.tempdir"));

        //defaults.put("keyPattern", new LabelPattern(KEY_PATTERN));

        restoreKeyBindings();

        customExports = new CustomExportList(this, new ExportComparator());

        //defaults.put("oooWarning", Boolean.TRUE);

    }



    public String get(String key) {
        return prefs.get(key, (String)defaults.get(key));
    }

    public String get(String key, String def) {
        return prefs.get(key, def);
    }

    public boolean getBoolean(String key) {
        return prefs.getBoolean(key, ((Boolean)defaults.get(key)).booleanValue());
    }

    public double getDouble(String key) {
        return prefs.getDouble(key, ((Double)defaults.get(key)).doubleValue());
    }

    public int getInt(String key) {
        return prefs.getInt(key, ((Integer)defaults.get(key)).intValue());
    }

    public byte[] getByteArray(String key) {
        return prefs.getByteArray(key, (byte[])defaults.get(key));
    }

    public void put(String key, String value) {
        prefs.put(key, value);
    }

    public void putBoolean(String key, boolean value) {
        prefs.putBoolean(key, value);
    }

    public void putDouble(String key, double value) {
        prefs.putDouble(key, value);
    }

    public void putInt(String key, int value) {
        prefs.putInt(key, value);
    }

    public void putByteArray(String key, byte[] value) {
        prefs.putByteArray(key, value);
    }

    public void remove(String key) {
        prefs.remove(key);
    }

    /**
     * Puts a string array into the Preferences, by linking its elements
     * with ';' into a single string. Escape characters make the process
     * transparent even if strings contain ';'.
     */
    public void putStringArray(String key, String[] value) {
        if (value == null) {
            remove(key);
            return;
        }

        if (value.length > 0) {
            StringBuffer linked = new StringBuffer();
            for (int i=0; i<value.length-1; i++) {
                linked.append(makeEscape(value[i]));
                linked.append(";");
            }
            linked.append(makeEscape(value[value.length-1]));
            put(key, linked.toString());
        } else {
            put(key, "");
        }
    }

    /**
     * Returns a String[] containing the chosen columns.
     */
    public String[] getStringArray(String key) {
        String names = get(key);
        if (names == null)
            return null;
        //Util.pr(key+"\n"+names);
        StringReader rd = new StringReader(names);
        Vector arr = new Vector();
        String rs;
        try {
            while ((rs = getNextUnit(rd)) != null) {
                arr.add(rs);
            }
        } catch (IOException ex) {}
        String[] res = new String[arr.size()];
        for (int i=0; i<res.length; i++)
            res[i] = (String)arr.elementAt(i);

        return res;
    }

    /**
     * Returns the KeyStroke for this binding, as defined by the
     * defaults, or in the Preferences.
     */
    public KeyStroke getKey(String bindName) {

        String s = (String)keyBinds.get(bindName);
        // If the current key bindings don't contain the one asked for,
        // we fall back on the default. This should only happen when a
        // user has his own set in Preferences, and has upgraded to a
        // new version where new bindings have been introduced.
        if (s == null) {
            s = (String)defKeyBinds.get(bindName);
            // So, if this happens, we add the default value to the current
            // hashmap, so this doesn't happen again, and so this binding
            // will appear in the KeyBindingsDialog.
            keyBinds.put(bindName, s);
        }
        if (s == null) {
          Globals.logger("Could not get key binding for \"" + bindName + "\"");
          //throw new RuntimeException("");
        }

        if (Globals.ON_MAC)
          return getKeyForMac(KeyStroke.getKeyStroke(s));
        else
          return KeyStroke.getKeyStroke(s);
    }

    /**
     * Returns the KeyStroke for this binding, as defined by the
     * defaults, or in the Preferences, but adapted for Mac users,
     * with the Command key preferred instead of Control.
     */
    private KeyStroke getKeyForMac(KeyStroke ks) {
      if (ks == null) return null;
      int keyCode = ks.getKeyCode();
      if ((ks.getModifiers() & KeyEvent.CTRL_MASK) == 0) {
        return ks;
      }
      else {
        if ((ks.getModifiers() & KeyEvent.SHIFT_MASK) != 0) {
          return KeyStroke.getKeyStroke(keyCode, Globals.SHORTCUT_MASK+KeyEvent.SHIFT_MASK);
        }
        return KeyStroke.getKeyStroke(keyCode, Globals.SHORTCUT_MASK);
      }
    }

    /**
     * Returns the HashMap containing all key bindings.
     */
    public HashMap getKeyBindings() {
        return keyBinds;
    }

    /**
     * Returns the HashMap containing default key bindings.
     */
    public HashMap getDefaultKeys() {
        return defKeyBinds;
    }

    /**
     * Stores new key bindings into Preferences, provided they
     * actually differ from the old ones.
     */
    public void setNewKeyBindings(HashMap newBindings) {
        if (!newBindings.equals(keyBinds)) {
            // This confirms that the bindings have actually changed.
            String[] bindNames = new String[newBindings.size()],
                bindings = new String[newBindings.size()];
            int index = 0;
            for (Iterator i=newBindings.keySet().iterator();
                 i.hasNext();) {
                String nm = (String)i.next();
                String bnd = (String)newBindings.get(nm);
                bindNames[index] = nm;
                bindings[index] = bnd;
                index++;
            }
            putStringArray("bindNames", bindNames);
            putStringArray("bindings", bindings);
            keyBinds = newBindings;
        }
    }

        public LabelPattern getKeyPattern(){
            // Lazy initialization..... Read overridden definitions from Preferences
            // and add these.
            if (keyPattern != null)
                return keyPattern;
            keyPattern = new LabelPattern(KEY_PATTERN);
            Preferences pre = Preferences.userNodeForPackage
                (net.sf.jabref.labelPattern.LabelPattern.class);
            try {
                String[] keys = pre.keys();
            if (keys.length > 0) for (int i=0; i<keys.length; i++)
                keyPattern.addLabelPattern(keys[i], pre.get(keys[i], null));
            } catch (BackingStoreException ex) {
                Globals.logger("BackingStoreException in JabRefPreferences.getKeyPattern");
            }

            ///
            //keyPattern.addLabelPattern("article", "[author][year]");
            //putKeyPattern(keyPattern);
            ///

            return keyPattern;
        }

        public void putKeyPattern(LabelPattern pattern){
            keyPattern = pattern;
            LabelPattern parent = pattern.getParent();
            if (parent == null)
                return;

            // Store overridden definitions to Preferences.
            Preferences pre = Preferences.userNodeForPackage
                (net.sf.jabref.labelPattern.LabelPattern.class);
            try {
                pre.clear(); // We remove all old entries.
            } catch (BackingStoreException ex) {
                Globals.logger("BackingStoreException in JabRefPreferences.putKeyPattern");
            }

            Iterator i = pattern.keySet().iterator();
            while (i.hasNext()) {
                String s = (String)i.next();
                if (!(pattern.get(s)).equals(parent.get(s)))
                    pre.put(s, pattern.getValue(s).get(0).toString());
            }
        }

    private void restoreKeyBindings() {
        // Define default keybindings.
        defineDefaultKeyBindings();

        // First read the bindings, and their names.
        String[] bindNames = getStringArray("bindNames"),
            bindings = getStringArray("bindings");

        // Then set up the key bindings HashMap.
        if ((bindNames == null) || (bindings == null)
            || (bindNames.length != bindings.length)) {
            // Nothing defined in Preferences, or something is wrong.
            setDefaultKeyBindings();
            return;
        }

        for (int i=0; i<bindNames.length; i++)
            keyBinds.put(bindNames[i], bindings[i]);
    }

    private void setDefaultKeyBindings() {
        keyBinds = defKeyBinds;
    }

    private void defineDefaultKeyBindings() {
      defKeyBinds.put("Push to LyX","ctrl L");
      defKeyBinds.put("Push to WinEdt","ctrl shift W");
        defKeyBinds.put("Quit JabRef", "ctrl Q");
        defKeyBinds.put("Open database", "ctrl O");
        defKeyBinds.put("Save database", "ctrl S");
        defKeyBinds.put("Save database as ...", "ctrl shift S");
        defKeyBinds.put("Close database", "ctrl W");
        defKeyBinds.put("New entry", "ctrl N");
        defKeyBinds.put("Cut", "ctrl X");
        defKeyBinds.put("Copy", "ctrl C");
        defKeyBinds.put("Paste", "ctrl V");
        defKeyBinds.put("Undo", "ctrl Z");
        defKeyBinds.put("Redo", "ctrl Y");
        defKeyBinds.put("Help", "F1");
        defKeyBinds.put("New article", "ctrl shift A");
        defKeyBinds.put("New book", "ctrl shift B");
        defKeyBinds.put("New phdthesis", "ctrl shift T");
        defKeyBinds.put("New inbook", "ctrl shift I");
        defKeyBinds.put("New mastersthesis", "ctrl shift M");
        defKeyBinds.put("New proceedings", "ctrl shift P");
        defKeyBinds.put("New unpublished", "ctrl shift U");
        defKeyBinds.put("Edit strings", "ctrl T");
        defKeyBinds.put("Edit preamble", "ctrl P");
        defKeyBinds.put("Select all", "ctrl A");
        defKeyBinds.put("Toggle groups interface", "ctrl shift G");
        defKeyBinds.put("Autogenerate BibTeX keys", "ctrl G");
        defKeyBinds.put("Search", "ctrl F");
        defKeyBinds.put("Incremental search", "ctrl shift F");
        defKeyBinds.put("Repeat incremental search", "ctrl shift F");
        defKeyBinds.put("Close dialog", "ESCAPE");
        defKeyBinds.put("Close entry editor", "ESCAPE");
        defKeyBinds.put("Close preamble editor", "ESCAPE");
        defKeyBinds.put("Back, help dialog", "LEFT");
        defKeyBinds.put("Forward, help dialog", "RIGHT");
        defKeyBinds.put("Preamble editor, store changes", "alt S");
        defKeyBinds.put("Clear search", "ESCAPE");
        defKeyBinds.put("Entry editor, next panel", "ctrl TAB");//"ctrl PLUS");//"shift Right");
        defKeyBinds.put("Entry editor, previous panel", "ctrl shift TAB");//"ctrl MINUS");
        defKeyBinds.put("Entry editor, next panel 2", "ctrl PLUS");//"ctrl PLUS");//"shift Right");
        defKeyBinds.put("Entry editor, previous panel 2", "ctrl MINUS");//"ctrl MINUS");
        defKeyBinds.put("Entry editor, next entry", "ctrl shift DOWN");
        defKeyBinds.put("Entry editor, previous entry", "ctrl shift UP");
        defKeyBinds.put("Entry editor, store field", "alt S");
        defKeyBinds.put("String dialog, add string", "ctrl N");
        defKeyBinds.put("String dialog, remove string", "shift DELETE");
        defKeyBinds.put("String dialog, move string up", "ctrl UP");
        defKeyBinds.put("String dialog, move string down", "ctrl DOWN");
        defKeyBinds.put("Save session", "F11");
        defKeyBinds.put("Load session", "F12");
        defKeyBinds.put("Copy \\cite{BibTeX key}", "ctrl K");
        defKeyBinds.put("Next tab", "ctrl PAGE_DOWN");
        defKeyBinds.put("Previous tab", "ctrl PAGE_UP");
        defKeyBinds.put("Replace string", "ctrl R");
        defKeyBinds.put("Delete", "DELETE");
        defKeyBinds.put("Open PDF or PS", "F4");
        defKeyBinds.put("Open URL or DOI", "F3");
        defKeyBinds.put("Toggle entry preview", "ctrl F9");
        defKeyBinds.put("Switch preview layout", "F9");
        defKeyBinds.put("Edit entry", "ctrl E");
        defKeyBinds.put("Mark entries", "ctrl M");
        defKeyBinds.put("Unmark entries", "ctrl shift M");
        defKeyBinds.put("Fetch Medline", "F5");
        defKeyBinds.put("Fetch CiteSeer", "F6");
        defKeyBinds.put("New from plain text", "ctrl shift N");
        defKeyBinds.put("Import Fields from CiteSeer", "ctrl shift C");
        defKeyBinds.put("Fetch citations from CiteSeer", "F7");
        //defKeyBinds.put("Select value", "ctrl B");
    }

    private String getNextUnit(Reader data) throws IOException {
        int c;
        boolean escape = false, done = false;
        StringBuffer res = new StringBuffer();
        while (!done && ((c = data.read()) != -1)) {
            if (c == '\\') {
                if (!escape)
                    escape = true;
                else {
                    escape = false;
                    res.append('\\');
                }
            } else {
                if (c == ';') {
                    if (!escape)
                        done = true;
                    else
                        res.append(';');
                } else {
                    res.append((char)c);
                }
                escape = false;
            }
        }
        if (res.length() > 0)
            return res.toString();
        else
            return null;
    }

    private String makeEscape(String s) {
        StringBuffer sb = new StringBuffer();
        int c;
        for (int i=0; i<s.length(); i++) {
            c = s.charAt(i);
            if ((c == '\\') || (c == ';'))
                sb.append('\\');
            sb.append((char)c);
        }
        return sb.toString();
    }

    /**
     * Stores all information about the entry type in preferences, with
     * the tag given by number.
     */
    public void storeCustomEntryType(CustomEntryType tp, int number) {
        String nr = ""+number;
        put(CUSTOM_TYPE_NAME+nr, tp.getName());
        putStringArray(CUSTOM_TYPE_REQ+nr, tp.getRequiredFields());
        putStringArray(CUSTOM_TYPE_OPT+nr, tp.getOptionalFields());

    }

    /**
     * Retrieves all information about the entry type in preferences,
     * with the tag given by number.
     */
    public CustomEntryType getCustomEntryType(int number) {
        String nr = ""+number;
        String
            name = get(CUSTOM_TYPE_NAME+nr);
        String[]
            req = getStringArray(CUSTOM_TYPE_REQ+nr),
            opt = getStringArray(CUSTOM_TYPE_OPT+nr);
        if (name == null)
            return null;
        else return new CustomEntryType
            (Util.nCase(name), req, opt);


    }

    /**
     * Removes all information about custom entry types with tags of
     * @param number or higher.
     */
    public void purgeCustomEntryTypes(int number) {
	purgeSeries(CUSTOM_TYPE_NAME, number);
	purgeSeries(CUSTOM_TYPE_REQ, number);
	purgeSeries(CUSTOM_TYPE_OPT, number);

        /*while (get(CUSTOM_TYPE_NAME+number) != null) {
            remove(CUSTOM_TYPE_NAME+number);
            remove(CUSTOM_TYPE_REQ+number);
            remove(CUSTOM_TYPE_OPT+number);
            number++;
	    }*/
    }

    /**
     * Removes all entries keyed by prefix+number, where number
     * is equal to or higher than the given number.
     * @param number or higher.
     */
    public void purgeSeries(String prefix, int number) {
        while (get(prefix+number) != null) {
            remove(prefix+number);
            number++;
        }
    }

    public EntryEditorTabList getEntryEditorTabList() {
	if (tabList == null)
	    updateEntryEditorTabList();
	return tabList;
    }

    public void updateEntryEditorTabList() {
	tabList = new EntryEditorTabList();
    }

    /**
     * Exports Preferences to an XML file.
     *
     * @param filename String File to export to
     */
    public void exportPreferences(String filename) throws IOException {
      File f = new File(filename);
      OutputStream os = new FileOutputStream(f);
      try {
        prefs.exportSubtree(os);
      } catch (BackingStoreException ex) {
        throw new IOException(ex.getMessage());
      }
    }

      /**
       * Imports Preferences from an XML file.
       *
       * @param filename String File to import from
       */
      public void importPreferences(String filename) throws IOException {
        File f = new File(filename);
        InputStream is = new FileInputStream(f);
        try {
          Preferences.importPreferences(is);
        } catch (InvalidPreferencesFormatException ex) {
          throw new IOException(ex.getMessage());
        }
      }
}
