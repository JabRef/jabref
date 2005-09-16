package net.sf.jabref.journals;

import net.sf.jabref.util.CaseChanger;
import net.sf.jabref.FieldEditor;
import net.sf.jabref.Globals;
import net.sf.jabref.EntryEditor;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.undo.UndoableFieldChange;

import javax.swing.*;
import javax.swing.undo.UndoManager;
import javax.swing.undo.CompoundEdit;
import java.io.*;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Iterator;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.net.URL;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: Sep 16, 2005
 * Time: 10:49:08 PM
 * To change this template use File | Settings | File Templates.
 */
public class JournalAbbreviations {

    static String TOOLTIPTEXT = "<HTML>"+Globals.lang("Switches between full and abbreviated journal name")
            +"<BR>"+Globals.lang("if the journal name is known. Go to (...............)")+"</HTML>";
    TreeMap fullNameKeyed = new TreeMap();
    HashMap abbrNameKeyed = new HashMap();
    CaseChanger caseChanger = new CaseChanger();

    public JournalAbbreviations() {

    }

    public JournalAbbreviations(String resource) {
        readJournalList(resource);
    }

    /**
     * Get an iterator for the known journals in alphabetical order.
     * @return Iterator for journal full names
     */
    public Iterator fullNameIterator() {
        return fullNameKeyed.keySet().iterator();
    }

    public boolean isKnownName(String journalName) {
        String s = journalName.toLowerCase();
        return ((fullNameKeyed.get(s) != null) || (abbrNameKeyed.get(s) != null));
    }

    public boolean isAbbreviatedName(String journalName) {
        String s = journalName.toLowerCase();
        return (abbrNameKeyed.get(s) != null);
    }

    /**
     * Attempts to get the abbreviated name of the journal given. Returns null if no
     * abbreviated name is known.
     * @param journalName The journal name to abbreviate.
     * @param titleCase true if the first character of every word should be capitalized, false
     * if only the first character should be.
     * @return The abbreviated name, or null if it couldn't be found.
     */
    public String getAbbreviatedName(String journalName, boolean titleCase) {
        String s = journalName.toLowerCase();
        Object o = fullNameKeyed.get(s);
        if (o == null)
            return null;
        s = (String)o;
        return titleCase ? caseChanger.changeCase(s, CaseChanger.UPPER_EACH_FIRST)
                    : caseChanger.changeCase(s, CaseChanger.UPPER_FIRST);
    }

    /**
     * Attempts to get the full name of the abbreviation given. Returns null if no
     * full name is known.
     * @param journalName The abbreviation to resolve.
     * @return The full name, or null if it couldn't be found.
     */
    public String getFullName(String journalName) {
        String s = journalName.toLowerCase();
        Object o = abbrNameKeyed.get(s);
        if (o == null) {
            if (fullNameKeyed.containsKey(s))
                o = s;
            else
                return null;
        }
        s = (String)o;
        return caseChanger.changeCase(s, CaseChanger.UPPER_EACH_FIRST);
    }

    public void readJournalList(String resourceFileName) {
        URL url = JournalAbbreviations.class.getResource(resourceFileName);
        try {
            readJournalList(new InputStreamReader(url.openStream()));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void readJournalList(File file) throws FileNotFoundException {
        readJournalList(new FileReader(file));
    }

    /**
     * Read the given file, which should contain a list of journal names and their
     * abbreviations. Each line should be formatted as: "Full Journal Name=Abbr. Journal Name"
     * @param in
     */
    public void readJournalList(Reader in) throws FileNotFoundException {
        BufferedReader reader = new BufferedReader(in);
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("=");
                if (parts.length == 2) {
                    String fullName = parts[0].trim().toLowerCase();
                    String abbrName = parts[1].trim().toLowerCase();
                    if ((fullName.length()>0) && (abbrName.length()>0)) {
                        //System.out.println("'"+fullName+"' : '"+abbrName+"'");
                        fullNameKeyed.put(fullName, abbrName);
                        abbrNameKeyed.put(abbrName, fullName);
                    }
                }
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (IOException ex2) {
                ex2.printStackTrace();
            }
        }
    }

    /**
     * Abbreviate the journal name of the given entry.
     * @param entry The entry to be treated.
     * @param fieldName The field name (e.g. "journal")
     * @param titleCase true if every part should start with a capital.
     * @param ce If the entry is changed, add an edit to this compound.
     * @return true if the entry was changed, false otherwise.
     */
    public boolean abbreviate(BibtexEntry entry, String fieldName, boolean titleCase, CompoundEdit ce) {
        Object o = entry.getField(fieldName);
        if (o == null)
            return false;
        String text = (String)o;
        if (isKnownName(text) && !isAbbreviatedName(text)) {
            String newText = getAbbreviatedName(text, titleCase);
            if (newText == null)
                return false;
            entry.setField(fieldName, newText);
            ce.addEdit(new UndoableFieldChange(entry, fieldName, text, newText));
            return true;
        } else
            return false;
    }

    /**
     * Unabbreviate the journal name of the given entry.
     * @param entry The entry to be treated.
     * @param fieldName The field name (e.g. "journal")
     * @param ce If the entry is changed, add an edit to this compound.
     * @return true if the entry was changed, false otherwise.
     */
    public boolean unabbreviate(BibtexEntry entry, String fieldName, CompoundEdit ce) {
        Object o = entry.getField(fieldName);
        if (o == null)
            return false;
        String text = (String)o;
        if (isKnownName(text) && isAbbreviatedName(text)) {
            String newText = getFullName(text);
            if (newText == null)
                return false;
            entry.setField(fieldName, newText);
            ce.addEdit(new UndoableFieldChange(entry, fieldName, text, newText));
            return true;
        } else
            return false;
    }


    /**
     * Create a control panel for the entry editor's journal field, to toggle
     * abbreviated/full journal name
     * @param editor The FieldEditor for the journal field.
     * @return The control panel for the entry editor.
     */
    public JComponent getNameSwitcher(final EntryEditor entryEditor, final FieldEditor editor,
                                      final UndoManager undoManager) {
        JButton button = new JButton(Globals.lang("Toggle abbreviation"));
        button.setToolTipText(TOOLTIPTEXT);
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                String text = editor.getText();
                if (isKnownName(text)) {
                    String s = null;
                    if (isAbbreviatedName(text))
                        s = getFullName(text);
                    else
                        s = getAbbreviatedName(text, true);

                    if (s != null) {
                        editor.setText(s);
                        entryEditor.storeFieldAction.actionPerformed(new ActionEvent(editor, 0, ""));
                        undoManager.addEdit(new UndoableFieldChange(entryEditor.entry, editor.getFieldName(),
                                text, s));
                    }
                }
            }
        });

        return button;
    }

}

