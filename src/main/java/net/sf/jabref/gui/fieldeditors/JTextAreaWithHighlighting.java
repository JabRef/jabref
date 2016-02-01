/*  Copyright (C) 2003-2016 JabRef contributors.
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
import net.sf.jabref.logic.search.SearchQueryHighlightListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import java.awt.event.ActionEvent;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JTextAreaWithHighlighting extends JTextArea implements SearchQueryHighlightListener {

    private static final Log LOGGER = LogFactory.getLog(JTextAreaWithHighlighting.class);

    private Optional<Pattern> highlightPattern = Optional.empty();

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
        getInputMap().put(Globals.getKeyPrefs().getKey(net.sf.jabref.gui.keyboard.KeyBinding.PASTE), Actions.PASTE);
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
        getInputMap().put(Globals.getKeyPrefs().getKey(net.sf.jabref.gui.keyboard.KeyBinding.UNDO), "Undo");

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
        KeyStroke redoKey = Globals.getKeyPrefs().getKey(net.sf.jabref.gui.keyboard.KeyBinding.REDO);
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
    private void highLight() {
        // highlight all characters that appear in charsToHighlight
        Highlighter highlighter = getHighlighter();
        highlighter.removeAllHighlights();

        if ((highlightPattern == null) || !highlightPattern.isPresent()) {
            return;
        }
        String content = getText();
        if (content.isEmpty()) {
            return;
        }

        highlightPattern.ifPresent(pattern -> {
            Matcher matcher = pattern.matcher(content);
            while (matcher.find()) {
                try {
                    highlighter.addHighlight(matcher.start(), matcher.end(), DefaultHighlighter.DefaultPainter);
                } catch (BadLocationException ble) {
                    // should not occur if matcher works right
                    LOGGER.warn("Highlighting not possible, bad location", ble);
                }
            }
        });

    }

    @Override
    public void setText(String text) {
        super.setText(text);
        highLight();
        if (undo != null) {
            undo.discardAllEdits();
        }
    }

    @Override
    public void highlightPattern(Optional<Pattern> highlightPattern) {
        this.highlightPattern = highlightPattern;
        highLight();
    }

}
