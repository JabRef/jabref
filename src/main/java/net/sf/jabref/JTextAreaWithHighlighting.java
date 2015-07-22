/*  Copyright (C) 2003-2012 JabRef contributors.
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

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.regex.Matcher;

import javax.swing.AbstractAction;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import javax.swing.text.Keymap;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

public class JTextAreaWithHighlighting extends JTextArea implements SearchTextListener {

    private ArrayList<String> wordsToHighlight;

    private UndoManager undo;


    public JTextAreaWithHighlighting() {
        super();
        setupUndoRedo();
    }

    JTextAreaWithHighlighting(String text) {
        super(text);
        setupUndoRedo();
    }

    public JTextAreaWithHighlighting(Document doc) {
        super(doc);
        setupUndoRedo();
    }

    public JTextAreaWithHighlighting(int rows, int columns) {
        super(rows, columns);
        setupUndoRedo();
    }

    public JTextAreaWithHighlighting(String text, int rows, int columns) {
        super(text, rows, columns);
        setupUndoRedo();
    }

    public JTextAreaWithHighlighting(Document doc, String text, int rows,
            int columns) {
        super(doc, text, rows, columns);
        setupUndoRedo();
    }

    private void setupUndoRedo() {
        undo = new UndoManager();
        Document doc = getDocument();

        // Listen for undo and redo events
        doc.addUndoableEditListener(new UndoableEditListener() {

            @Override
            public void undoableEditHappened(UndoableEditEvent evt) {
                undo.addEdit(evt.getEdit());
            }
        });

        // Create an undo action and add it to the text component
        getActionMap().put("Undo",
                new AbstractAction("Undo") {

                    @Override
                    public void actionPerformed(ActionEvent evt) {
                        try {
                            if (undo.canUndo()) {
                                undo.undo();
                            }
                        } catch (CannotUndoException ignored) {
                        }
                    }
                });

        // Bind the undo action to ctl-Z
        getInputMap().put(Globals.prefs.getKey("Undo"), "Undo");

        // Create a redo action and add it to the text component
        getActionMap().put("Redo",
                new AbstractAction("Redo") {

                    @Override
                    public void actionPerformed(ActionEvent evt) {
                        try {
                            if (undo.canRedo()) {
                                undo.redo();
                            }
                        } catch (CannotRedoException ignored) {
                        }
                    }
                });

        // Bind the redo action to ctrl-Y
        boolean bind = true;
        KeyStroke redoKey = Globals.prefs.getKey("Redo");
        if (Globals.prefs.getBoolean(JabRefPreferences.EDITOR_EMACS_KEYBINDINGS)) {
            // If emacs is enabled, check if we have a conflict at keys
            // If yes, do not bind
            // Typically, we have: CTRL+y is "yank" in emacs and REDO in 'normal' settings
            // The Emacs key bindings are stored in the keymap, not in the input map.
            Keymap keymap2 = getKeymap();
            KeyStroke[] keys = keymap2.getBoundKeyStrokes();
            int i = 0;
            while ((i < keys.length) && (!keys[i].equals(redoKey))) {
                i++;
            }
            if (i < keys.length) {
                // conflict found -> do not bind
                bind = false;
            }
        }
        if (bind) {
            getInputMap().put(redoKey, "Redo");
        }
    }

    /**
     * Highlight words in the Textarea
     * 
     * @param words to highlight
     */
    private void highLight(ArrayList<String> words) {
        // highlight all characters that appear in charsToHighlight
        Highlighter h = getHighlighter();
        // myTa.set
        h.removeAllHighlights();

        if ((words == null) || words.isEmpty() || words.get(0).isEmpty()) {
            return;
        }
        String content = getText();
        if (content.isEmpty()) {
            return;
        }

        Matcher matcher = Globals.getPatternForWords(words).matcher(content);

        while (matcher.find()) {
            try {
                h.addHighlight(matcher.start(), matcher.end(), DefaultHighlighter.DefaultPainter);
            } catch (BadLocationException ble) {
                // should not occur if matcher works right
                System.out.println(ble);
            }
        }

    }

    @Override
    public void setText(String t) {
        super.setText(t);
        if (Globals.prefs.getBoolean(JabRefPreferences.HIGH_LIGHT_WORDS)) {
            highLight(wordsToHighlight);
        }
        if (undo != null) {
            undo.discardAllEdits();
        }
    }

    @Override
    public void searchText(ArrayList<String> words) {
        // words have to be stored in class variable as 
        // setText() makes use of them

        if (Globals.prefs.getBoolean(JabRefPreferences.HIGH_LIGHT_WORDS)) {
            this.wordsToHighlight = words;
            highLight(words);
        } else {
            if (this.wordsToHighlight != null) {
                // setting of "highLightWords" seems to have changed.
                // clear all highlights and remember the clearing (by wordsToHighlight = null)
                this.wordsToHighlight = null;
                highLight(null);
            }
        }

    }

}
