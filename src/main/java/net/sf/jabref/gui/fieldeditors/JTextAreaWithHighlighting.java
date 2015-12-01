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
package net.sf.jabref.gui.fieldeditors;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.gui.actions.Actions;
import net.sf.jabref.gui.actions.PasteAction;
import net.sf.jabref.gui.keyboard.KeyBinds;
import net.sf.jabref.logic.search.SearchTextListener;
import net.sf.jabref.util.Util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.awt.event.ActionEvent;
import java.util.List;
import java.util.regex.Matcher;

import javax.swing.AbstractAction;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.text.*;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

public class JTextAreaWithHighlighting extends JTextArea implements SearchTextListener {

    private static final Log LOGGER = LogFactory.getLog(JTextAreaWithHighlighting.class);

    private List<String> wordsToHighlight;

    private UndoManager undo;


    public JTextAreaWithHighlighting() {
        super();
        setupUndoRedo();
        setupPasteListener();
    }

    JTextAreaWithHighlighting(String text) {
        super(text);
        setupUndoRedo();
        setupPasteListener();
    }

    private void setupPasteListener() {
        //register "Paste" action
        getActionMap().put(Actions.PASTE, new PasteAction(this));
        // Bind paste command to KeyBinds.PASTE
        getInputMap().put(Globals.prefs.getKey(KeyBinds.PASTE), Actions.PASTE);
    }

    private void setupUndoRedo() {
        undo = new UndoManager();
        Document doc = getDocument();

        // Listen for undo and redo events
        doc.addUndoableEditListener(evt -> undo.addEdit(evt.getEdit()));

        // Create an undo action and add it to the text component
        getActionMap().put("Undo", new AbstractAction("Undo") {

            @Override
            public void actionPerformed(ActionEvent evt) {
                try {
                    if (undo.canUndo()) {
                        undo.undo();
                    }
                } catch (CannotUndoException ignored) {
                    // Ignored
                }
            }
        });

        // Bind the undo action to ctl-Z
        getInputMap().put(Globals.prefs.getKey(KeyBinds.UNDO), "Undo");

        // Create a redo action and add it to the text component
        getActionMap().put("Redo", new AbstractAction(Actions.REDO) {

            @Override
            public void actionPerformed(ActionEvent evt) {
                try {
                    if (undo.canRedo()) {
                        undo.redo();
                    }
                } catch (CannotRedoException ignored) {
                    // Ignored
                }
            }
        });

        // Bind the redo action to ctrl-Y
        boolean bind = true;
        KeyStroke redoKey = Globals.prefs.getKey(KeyBinds.REDO);
        if (Globals.prefs.getBoolean(JabRefPreferences.EDITOR_EMACS_KEYBINDINGS)) {
            // If emacs is enabled, check if we have a conflict at keys
            // If yes, do not bind
            // Typically, we have: CTRL+y is "yank" in emacs and REDO in 'normal' settings
            // The Emacs key bindings are stored in the keymap, not in the input map.
            Keymap keymap = getKeymap();
            KeyStroke[] keys = keymap.getBoundKeyStrokes();
            int i = 0;
            while ((i < keys.length) && !keys[i].equals(redoKey)) {
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
    private void highLight(List<String> words) {
        // highlight all characters that appear in charsToHighlight
        Highlighter highlighter = getHighlighter();
        highlighter.removeAllHighlights();

        if ((words == null) || words.isEmpty() || words.get(0).isEmpty()) {
            return;
        }
        String content = getText();
        if (content.isEmpty()) {
            return;
        }

        Matcher matcher = Util.getPatternForWords(words).matcher(content);

        while (matcher.find()) {
            try {
                highlighter.addHighlight(matcher.start(), matcher.end(), DefaultHighlighter.DefaultPainter);
            } catch (BadLocationException ble) {
                // should not occur if matcher works right
                LOGGER.warn("Highlighting not possible, bad location", ble);
            }
        }

    }

    @Override
    public void setText(String text) {
        super.setText(text);
        highLight(wordsToHighlight);
        if (undo != null) {
            undo.discardAllEdits();
        }
    }

    @Override
    public void searchText(List<String> words) {
        this.wordsToHighlight = words;
        highLight(words);
    }

}
