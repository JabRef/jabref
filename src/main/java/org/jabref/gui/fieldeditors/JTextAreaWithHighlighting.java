package org.jabref.gui.fieldeditors;

import java.awt.event.ActionEvent;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import javax.swing.text.Keymap;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

import org.jabref.Globals;
import org.jabref.gui.actions.Actions;
import org.jabref.gui.util.component.JTextAreaWithPlaceholder;
import org.jabref.logic.search.SearchQueryHighlightListener;
import org.jabref.preferences.JabRefPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JTextAreaWithHighlighting extends JTextAreaWithPlaceholder implements SearchQueryHighlightListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(JTextAreaWithHighlighting.class);

    private Optional<Pattern> highlightPattern = Optional.empty();

    private UndoManager undo;

    public JTextAreaWithHighlighting() {
        this("");
    }

    public JTextAreaWithHighlighting(String text) {
        this(text, "");
    }

    /**
     * Creates a text area with the ability to highlight parts of the content.
     * It also defines a placeholder which will be displayed the content is empty.
     *
     * @param text
     * @param placeholder
     */
    public JTextAreaWithHighlighting(String text, String placeholder) {
        super(text, placeholder);
        setupUndoRedo();
        setupPasteListener();
    }

    private void setupPasteListener() {
        // Bind paste command to KeyBinds.PASTE
        getInputMap().put(Globals.getKeyPrefs().getKey(org.jabref.gui.keyboard.KeyBinding.PASTE), Actions.PASTE);
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
        getInputMap().put(Globals.getKeyPrefs().getKey(org.jabref.gui.keyboard.KeyBinding.UNDO), "Undo");

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
        KeyStroke redoKey = Globals.getKeyPrefs().getKey(org.jabref.gui.keyboard.KeyBinding.REDO);
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
