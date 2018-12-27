package org.jabref.gui.keyboard;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.swing.Action;
import javax.swing.JEditorPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.Keymap;
import javax.swing.text.TextAction;
import javax.swing.text.Utilities;

import org.jabref.preferences.JabRefPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic class which activates Emacs keybindings for java input {@link
 * JTextComponent}s.
 *
 * The inner class actions can also be used independently.
 */
public class EmacsKeyBindings {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmacsKeyBindings.class);

    private static final String KILL_LINE_ACTION = "emacs-kill-line";

    private static final String KILL_RING_SAVE_ACTION = "emacs-kill-ring-save";

    private static final String KILL_REGION_ACTION = "emacs-kill-region";

    private static final String BACKWARD_KILL_WORD_ACTION = "emacs-backward-kill-word";

    private static final String CAPITALIZE_WORD_ACTION = "emacs-capitalize-word";

    private static final String DOWNCASE_WORD_ACTION = "emacs-downcase-word";

    private static final String KILL_WORD_ACTION = "emacs-kill-word";

    private static final String SET_MARK_COMMAND_ACTION = "emacs-set-mark-command";

    private static final String YANK_ACTION = "emacs-yank";

    private static final String YANK_POP_ACTION = "emacs-yank-pop";

    private static final String UPCASE_WORD_ACTION = "emacs-upcase-word";

    private static final JTextComponent.KeyBinding[] EMACS_KEY_BINDINGS_BASE = {
            new JTextComponent.
                    KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_E,
                    InputEvent.CTRL_MASK),
                    DefaultEditorKit.endLineAction),
            new JTextComponent.
                    KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_D,
                    InputEvent.CTRL_MASK),
                    DefaultEditorKit.deleteNextCharAction),
            new JTextComponent.
                    KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_N,
                    InputEvent.CTRL_MASK),
                    DefaultEditorKit.downAction),
            new JTextComponent.
                    KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_P,
                    InputEvent.CTRL_MASK),
                    DefaultEditorKit.upAction),
            new JTextComponent.
                    KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_B,
                    InputEvent.ALT_MASK),
                    DefaultEditorKit.previousWordAction),
            new JTextComponent.
                    KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_LESS,
                    InputEvent.ALT_MASK),
                    DefaultEditorKit.beginAction),
            new JTextComponent.
                    KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_LESS,
                    InputEvent.ALT_MASK
                            + InputEvent.SHIFT_MASK),
                    DefaultEditorKit.endAction),
            new JTextComponent.
                    KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_F,
                    InputEvent.ALT_MASK),
                    DefaultEditorKit.nextWordAction),
            new JTextComponent.
                    KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_B,
                    InputEvent.CTRL_MASK),
                    DefaultEditorKit.backwardAction),
            // CTRL+V and ALT+V are disabled as CTRL+V is also "paste"
            //		new JTextComponent.
            //			KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_V,
            //											  InputEvent.CTRL_MASK),
            //					   DefaultEditorKit.pageDownAction),
            //		new JTextComponent.
            //			KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_V,
            //											  InputEvent.ALT_MASK),
            //					   DefaultEditorKit.pageUpAction),
            new JTextComponent.
                    KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_D,
                    InputEvent.ALT_MASK),
                    EmacsKeyBindings.KILL_WORD_ACTION),
            new JTextComponent.
                    KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE,
                    InputEvent.ALT_MASK),
                    EmacsKeyBindings.BACKWARD_KILL_WORD_ACTION),
            new JTextComponent.
                    KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE,
                    InputEvent.CTRL_MASK),
                    EmacsKeyBindings.SET_MARK_COMMAND_ACTION),
            new JTextComponent.
                    KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_W,
                    InputEvent.ALT_MASK),
                    EmacsKeyBindings.KILL_RING_SAVE_ACTION),
            new JTextComponent.
                    KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_W,
                    InputEvent.CTRL_MASK),
                    EmacsKeyBindings.KILL_REGION_ACTION),

            new JTextComponent.
                    KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_K,
                    InputEvent.CTRL_MASK),
                    EmacsKeyBindings.KILL_LINE_ACTION),

            new JTextComponent.
                    KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_Y,
                    InputEvent.CTRL_MASK),
                    EmacsKeyBindings.YANK_ACTION),

            new JTextComponent.
                    KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_Y,
                    InputEvent.ALT_MASK),
                    EmacsKeyBindings.YANK_POP_ACTION),

            new JTextComponent.
                    KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_C,
                    InputEvent.ALT_MASK),
                    EmacsKeyBindings.CAPITALIZE_WORD_ACTION),

            new JTextComponent.
                    KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_L,
                    InputEvent.ALT_MASK),
                    EmacsKeyBindings.DOWNCASE_WORD_ACTION),

            new JTextComponent.
                    KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_U,
                    InputEvent.ALT_MASK),
                    EmacsKeyBindings.UPCASE_WORD_ACTION),
    };

    private static final JTextComponent.KeyBinding EMACS_KEY_BINDING_C_A = new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_A,
            InputEvent.CTRL_MASK),
            DefaultEditorKit.beginLineAction);

    private static final JTextComponent.KeyBinding EMACS_KEY_BINDING_C_F = new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_F,
            InputEvent.CTRL_MASK),
            DefaultEditorKit.forwardAction);

    private static final TextAction[] EMACS_ACTIONS = {
            new KillWordAction(EmacsKeyBindings.KILL_WORD_ACTION),
            new BackwardKillWordAction(EmacsKeyBindings.BACKWARD_KILL_WORD_ACTION),
            new SetMarkCommandAction(EmacsKeyBindings.SET_MARK_COMMAND_ACTION),
            new KillRingSaveAction(EmacsKeyBindings.KILL_RING_SAVE_ACTION),
            new KillRegionAction(EmacsKeyBindings.KILL_REGION_ACTION),
            new KillLineAction(EmacsKeyBindings.KILL_LINE_ACTION),
            new YankAction(EmacsKeyBindings.YANK_ACTION),
            new YankPopAction(EmacsKeyBindings.YANK_POP_ACTION),
            new CapitalizeWordAction(EmacsKeyBindings.CAPITALIZE_WORD_ACTION),
            new DowncaseWordAction(EmacsKeyBindings.DOWNCASE_WORD_ACTION),
            new UpcaseWordAction(EmacsKeyBindings.UPCASE_WORD_ACTION)
    };

    // components to modify
    private static final JTextComponent[] JTCS = new JTextComponent[]{
            new JTextArea(),
            new JTextPane(),
            new JTextField(),
            new JEditorPane(),
    };

    private EmacsKeyBindings() {
    }

    /**
     * Loads the emacs keybindings for all common <code>JTextComponent</code>s.
     *
     * The shared keymap instances of the concrete subclasses of
     * {@link JTextComponent} are fed with the keybindings.
     *
     * The original keybindings are stored in a backup array.
     */
    public static void load() {
        EmacsKeyBindings.createBackup();
        EmacsKeyBindings.loadEmacsKeyBindings();
    }

    private static void createBackup() {
        Keymap oldBackup = JTextComponent.getKeymap(EmacsKeyBindings.JTCS[0].getClass().getName());
        if (oldBackup != null) {
            // if there is already a backup, do not create a new backup
            return;
        }

        for (JTextComponent jtc : EmacsKeyBindings.JTCS) {
            Keymap orig = jtc.getKeymap();
            Keymap backup = JTextComponent.addKeymap
                    (jtc.getClass().getName(), null);
            Action[] bound = orig.getBoundActions();
            for (Action aBound : bound) {
                KeyStroke[] strokes = orig.getKeyStrokesForAction(aBound);
                for (KeyStroke stroke : strokes) {
                    backup.addActionForKeyStroke(stroke, aBound);
                }
            }
            backup.setDefaultAction(orig.getDefaultAction());
        }
    }

    /**
     * Restores the original keybindings for the concrete subclasses of
     * {@link JTextComponent}.
     */
    public static void unload() {
        for (int i = 0; i < EmacsKeyBindings.JTCS.length; i++) {
            Keymap backup = JTextComponent.getKeymap
                    (EmacsKeyBindings.JTCS[i].getClass().getName());

            if (backup != null) {
                Keymap current = EmacsKeyBindings.JTCS[i].getKeymap();
                current.removeBindings();

                Action[] bound = backup.getBoundActions();
                for (Action aBound : bound) {
                    KeyStroke[] strokes =
                            backup.getKeyStrokesForAction(bound[i]);
                    for (KeyStroke stroke : strokes) {
                        current.addActionForKeyStroke(stroke, aBound);
                    }
                }
                current.setDefaultAction(backup.getDefaultAction());
            }
        }
    }

    /**
     * Activates Emacs keybindings for all text components extending {@link
     * JTextComponent}.
     */
    private static void loadEmacsKeyBindings() {
        EmacsKeyBindings.LOGGER.debug("Loading emacs keybindings");

        for (JTextComponent jtc : EmacsKeyBindings.JTCS) {
            Action[] origActions = jtc.getActions();
            Action[] actions = new Action[origActions.length + EmacsKeyBindings.EMACS_ACTIONS.length];
            System.arraycopy(origActions, 0, actions, 0, origActions.length);
            System.arraycopy(EmacsKeyBindings.EMACS_ACTIONS, 0, actions, origActions.length, EmacsKeyBindings.EMACS_ACTIONS.length);

            Keymap k = jtc.getKeymap();

            JTextComponent.KeyBinding[] keybindings;
            boolean rebindCA = JabRefPreferences.getInstance().getBoolean(JabRefPreferences.EDITOR_EMACS_KEYBINDINGS_REBIND_CA);
            boolean rebindCF = JabRefPreferences.getInstance().getBoolean(JabRefPreferences.EDITOR_EMACS_KEYBINDINGS_REBIND_CF);
            if (rebindCA || rebindCF) {
                // if we additionally rebind C-a or C-f, we have to add the shortcuts to EmacsKeyBindings.EMACS_KEY_BINDINGS_BASE

                // determine size of new array and position of the new key bindings in the array
                int size = EmacsKeyBindings.EMACS_KEY_BINDINGS_BASE.length;
                int posCA = -1;
                int posCF = -1;
                if (rebindCA) {
                    posCA = size;
                    size++;
                }
                if (rebindCF) {
                    posCF = size;
                    size++;
                }

                // generate new array
                keybindings = new JTextComponent.KeyBinding[size];
                System.arraycopy(EmacsKeyBindings.EMACS_KEY_BINDINGS_BASE, 0, keybindings, 0, EmacsKeyBindings.EMACS_KEY_BINDINGS_BASE.length);
                if (rebindCA) {
                    keybindings[posCA] = EmacsKeyBindings.EMACS_KEY_BINDING_C_A;
                }
                if (rebindCF) {
                    keybindings[posCF] = EmacsKeyBindings.EMACS_KEY_BINDING_C_F;
                }
            } else {
                keybindings = EmacsKeyBindings.EMACS_KEY_BINDINGS_BASE;
            }
            JTextComponent.loadKeymap(k, keybindings, actions);
        }
    }


    /**
     * This action kills the next word.
     *
     * It removes the next word on the right side of the cursor from the active
     * text component and adds it to the clipboard.
     */
    @SuppressWarnings("serial")
    public static class KillWordAction extends TextAction {

        public KillWordAction(String nm) {
            super(nm);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JTextComponent jtc = getTextComponent(e);
            if (jtc != null) {
                try {
                    int offs = jtc.getCaretPosition();
                    jtc.setSelectionStart(offs);
                    offs = EmacsKeyBindings.getWordEnd(jtc, offs);
                    jtc.setSelectionEnd(offs);
                    String selectedText = jtc.getSelectedText();
                    if (selectedText != null) {
                        KillRing.getInstance().add(selectedText);
                    }
                    jtc.cut();
                } catch (BadLocationException ble) {
                    jtc.getToolkit().beep();
                }
            }
        }
    }

    /**
     * This action kills the previous word.
     *
     * It removes the previous word on the left side of the cursor from the
     * active text component and adds it to the clipboard.
     */
    @SuppressWarnings("serial")
    public static class BackwardKillWordAction extends TextAction {

        public BackwardKillWordAction(String nm) {
            super(nm);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JTextComponent jtc = getTextComponent(e);
            if (jtc != null) {
                try {
                    int offs = jtc.getCaretPosition();
                    jtc.setSelectionEnd(offs);
                    offs = Utilities.getPreviousWord(jtc, offs);
                    jtc.setSelectionStart(offs);
                    String selectedText = jtc.getSelectedText();
                    if (selectedText != null) {
                        KillRing.getInstance().add(selectedText);
                    }
                    jtc.cut();
                } catch (BadLocationException ble) {
                    jtc.getToolkit().beep();
                }
            }
        }
    }

    /**
     * This action copies the marked region and stores it in the killring.
     */
    @SuppressWarnings("serial")
    public static class KillRingSaveAction extends TextAction {

        public KillRingSaveAction(String nm) {
            super(nm);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JTextComponent jtc = getTextComponent(e);
            EmacsKeyBindings.doCopyOrCut(jtc, true);
        }
    }

    /**
     * This action Kills the marked region and stores it in the killring.
     */
    @SuppressWarnings("serial")
    public static class KillRegionAction extends TextAction {

        public KillRegionAction(String nm) {
            super(nm);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JTextComponent jtc = getTextComponent(e);
            EmacsKeyBindings.doCopyOrCut(jtc, false);
        }
    }

    private static void doCopyOrCut(JTextComponent jtc, boolean copy) {
        if (jtc != null) {
            int caretPosition = jtc.getCaretPosition();
            String text = jtc.getSelectedText();
            if (text != null) {
                // user has manually marked a text without using CTRL+W
                // we obey that selection and copy it.
            } else if (SetMarkCommandAction.isMarked(jtc)) {
                int beginPos = caretPosition;
                int endPos = SetMarkCommandAction.getCaretPosition();
                if (beginPos > endPos) {
                    int tmp = endPos;
                    endPos = beginPos;
                    beginPos = tmp;
                }
                jtc.select(beginPos, endPos);
                SetMarkCommandAction.reset();
            }
            text = jtc.getSelectedText();
            if (text == null) {
                jtc.getToolkit().beep();
            } else {
                if (copy) {
                    jtc.copy();
                    // clear the selection
                    jtc.select(caretPosition, caretPosition);
                } else {
                    int newCaretPos = jtc.getSelectionStart();
                    jtc.cut();
                    // put the cursor to the beginning of the text to cut
                    jtc.setCaretPosition(newCaretPos);
                }
                KillRing.getInstance().add(text);
            }
        }
    }


    /**
     * This actin kills text up to the end of the current line and stores it in
     * the killring.
     */
    @SuppressWarnings("serial")
    public static class KillLineAction extends TextAction {

        public KillLineAction(String nm) {
            super(nm);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JTextComponent jtc = getTextComponent(e);
            if (jtc != null) {
                try {
                    int start = jtc.getCaretPosition();
                    int end = Utilities.getRowEnd(jtc, start);
                    if ((start == end) && jtc.isEditable()) {
                        Document doc = jtc.getDocument();
                        doc.remove(end, 1);
                    } else {
                        jtc.setSelectionStart(start);
                        jtc.setSelectionEnd(end);
                        String selectedText = jtc.getSelectedText();
                        if (selectedText != null) {
                            KillRing.getInstance().add(selectedText);
                        }

                        jtc.cut();
                        // jtc.replaceSelection("");
                    }
                } catch (BadLocationException ble) {
                    jtc.getToolkit().beep();
                }
            }
        }
    }

    /**
     * This action matchers a beginning mark for a selection.
     */
    @SuppressWarnings("serial")
    public static class SetMarkCommandAction extends TextAction {

        private static int position = -1;
        private static JTextComponent jtc;


        public SetMarkCommandAction(String nm) {
            super(nm);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            SetMarkCommandAction.jtc = getTextComponent(e);
            if (SetMarkCommandAction.jtc != null) {
                SetMarkCommandAction.position = SetMarkCommandAction.jtc.getCaretPosition();
            }
        }

        public static boolean isMarked(JTextComponent jt) {
            return (SetMarkCommandAction.jtc == jt) && (SetMarkCommandAction.position != -1);
        }

        public static void reset() {
            SetMarkCommandAction.jtc = null;
            SetMarkCommandAction.position = -1;
        }

        public static int getCaretPosition() {
            return SetMarkCommandAction.position;
        }
    }

    /**
     * This action pastes text from the killring.
     */
    @SuppressWarnings("serial")
    public static class YankAction extends TextAction {

        public static int start = -1;
        public static int end = -1;


        public YankAction(String nm) {
            super(nm);
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            JTextComponent jtc = getTextComponent(event);

            if (jtc != null) {
                try {
                    YankAction.start = jtc.getCaretPosition();
                    jtc.paste();
                    YankAction.end = jtc.getCaretPosition();
                    KillRing.getInstance().add(jtc.getText(YankAction.start, YankAction.end));
                    KillRing.getInstance().setCurrentTextComponent(jtc);
                } catch (BadLocationException e) {
                    LOGGER.info("Bad location when yanking", e);
                }
            }
        }
    }

    /**
     * This action pastes an element from the killring cycling through it.
     */
    @SuppressWarnings("serial")
    public static class YankPopAction extends TextAction {

        public YankPopAction(String nm) {
            super(nm);
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            JTextComponent jtc = getTextComponent(event);
            boolean jtcNotNull = jtc != null;
            boolean jtcIsCurrentTextComponent = KillRing.getInstance().getCurrentTextComponent() == jtc;
            boolean caretPositionIsEndOfLastYank = jtcNotNull && (jtc.getCaretPosition() == YankAction.end);
            boolean killRingNotEmpty = !KillRing.getInstance().isEmpty();
            if (jtcNotNull && jtcIsCurrentTextComponent && caretPositionIsEndOfLastYank && killRingNotEmpty) {
                jtc.setSelectionStart(YankAction.start);
                jtc.setSelectionEnd(YankAction.end);
                String toYank = KillRing.getInstance().next();
                if (toYank == null) {
                    jtc.getToolkit().beep();
                } else {
                    jtc.replaceSelection(toYank);
                    YankAction.end = jtc.getCaretPosition();
                }
            }
        }
    }

    public static class KillRing {

        /**
         * Manages all killed (cut) text pieces in a ring which is accessible
         * through {@link YankPopAction}.
         * <p>
         * Also provides an unmodifiable copy of all cut pieces.
         */
        private static final KillRing INSTANCE = new KillRing();
        private JTextComponent jtc;
        private final LinkedList<String> ring = new LinkedList<>();

        private Iterator<String> iter = ring.iterator();

        public static KillRing getInstance() {
            return KillRing.INSTANCE;
        }

        public void setCurrentTextComponent(JTextComponent jtc) {
            this.jtc = jtc;
        }

        public JTextComponent getCurrentTextComponent() {
            return jtc;
        }

        /**
         * Adds text to the front of the kill ring.
         * <p>
         * Deviating from the Emacs implementation we make sure the
         * exact same text is not somewhere else in the ring.
         */
        public void add(String text) {
            if (text.isEmpty()) {
                return;
            }

            ring.remove(text);
            ring.addFirst(text);
            while (ring.size() > 60) {
                ring.removeLast();
            }
            iter = ring.iterator();
            // skip first entry, the one we just added
            iter.next();
        }

        /**
         * Returns an unmodifiable version of the ring list which contains
         * the killed texts.
         *
         * @return the content of the kill ring
         */
        public List<String> getRing() {
            return Collections.unmodifiableList(ring);
        }

        public boolean isEmpty() {
            return ring.isEmpty();
        }

        /**
         * Returns the next text element which is to be yank-popped.
         *
         * @return <code>null</code> if the ring is empty
         */
        public String next() {
            if (ring.isEmpty()) {
                return null;
            } else if (iter.hasNext()) {
                return iter.next();
            } else {
                iter = ring.iterator();
                // guaranteed to not throw an exception, since ring is not empty
                return iter.next();
            }
        }
    }

    /**
     * This action capitalizes the next word on the right side of the caret.
     */
    @SuppressWarnings("serial")
    public static class CapitalizeWordAction extends TextAction {

        public CapitalizeWordAction(String nm) {
            super(nm);
        }

        /**
         * At first the same code as in {@link
         * EmacsKeyBindings.DowncaseWordAction} is performed, to ensure the
         * word is in lower case, then the first letter is capialized.
         */
        @Override
        public void actionPerformed(ActionEvent event) {
            JTextComponent jtc = getTextComponent(event);

            if (jtc != null) {
                try {
                    /* downcase code */
                    int start = jtc.getCaretPosition();
                    int end = EmacsKeyBindings.getWordEnd(jtc, start);
                    jtc.setSelectionStart(start);
                    jtc.setSelectionEnd(end);
                    String word = jtc.getText(start, end - start);
                    jtc.replaceSelection(word.toLowerCase(Locale.ROOT));

                    /* actual capitalize code */
                    int offs = Utilities.getWordStart(jtc, start);
                    // get first letter
                    String c = jtc.getText(offs, 1);
                    // we're at the end of the previous word
                    if (" ".equals(c)) {
                        /* ugly java workaround to get the beginning of the
                           word.  */
                        offs = Utilities.getWordStart(jtc, ++offs);
                        c = jtc.getText(offs, 1);
                    }
                    if (Character.isLetter(c.charAt(0))) {
                        jtc.setSelectionStart(offs);
                        jtc.setSelectionEnd(offs + 1);
                        jtc.replaceSelection(c.toUpperCase(Locale.ROOT));
                    }
                    end = Utilities.getWordEnd(jtc, offs);
                    jtc.setCaretPosition(end);
                } catch (BadLocationException ble) {
                    jtc.getToolkit().beep();
                }
            }
        }
    }

    /**
     * This action renders all characters of the next word to lowercase.
     */
    @SuppressWarnings("serial")
    public static class DowncaseWordAction extends TextAction {

        public DowncaseWordAction(String nm) {
            super(nm);
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            JTextComponent jtc = getTextComponent(event);

            if (jtc != null) {
                try {
                    int start = jtc.getCaretPosition();
                    int end = EmacsKeyBindings.getWordEnd(jtc, start);
                    jtc.setSelectionStart(start);
                    jtc.setSelectionEnd(end);
                    String word = jtc.getText(start, end - start);
                    jtc.replaceSelection(word.toLowerCase(Locale.ROOT));
                    jtc.setCaretPosition(end);
                } catch (BadLocationException ble) {
                    jtc.getToolkit().beep();
                }
            }
        }
    }

    /**
     * This action renders all characters of the next word to upppercase.
     */
    @SuppressWarnings("serial")
    public static class UpcaseWordAction extends TextAction {

        public UpcaseWordAction(String nm) {
            super(nm);
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            JTextComponent jtc = getTextComponent(event);

            if (jtc != null) {
                try {
                    int start = jtc.getCaretPosition();
                    int end = EmacsKeyBindings.getWordEnd(jtc, start);
                    jtc.setSelectionStart(start);
                    jtc.setSelectionEnd(end);
                    String word = jtc.getText(start, end - start);
                    jtc.replaceSelection(word.toUpperCase(Locale.ROOT));
                    jtc.setCaretPosition(end);
                } catch (BadLocationException ble) {
                    jtc.getToolkit().beep();
                }
            }
        }
    }

    private static int getWordEnd(JTextComponent jtc, int start)
            throws BadLocationException {
        try {
            return Utilities.getNextWord(jtc, start);
        } catch (BadLocationException ble) {
            int end = jtc.getText().length();
            if (start < end) {
                return end;
            } else {
                throw ble;
            }
        }
    }
}
