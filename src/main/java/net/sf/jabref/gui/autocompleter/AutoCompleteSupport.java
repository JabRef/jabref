/*  Copyright (C) 2003-2015 JabRef contributors.
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
package net.sf.jabref.gui.autocompleter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.JTextComponent;

import net.sf.jabref.logic.autocompleter.AutoCompleter;

/**
 * Endows a textbox with the ability to autocomplete the input. Based on code by Santhosh Kumar
 * (http://www.jroller.com/santhosh/date/20050620) James Lemieux (Glazed Lists AutoCompleteSupport)
 *
 * @param <E> type of items displayed in the autocomplete popup
 */
public class AutoCompleteSupport<E> {

    private final AutoCompleteRenderer<E> renderer;
    private AutoCompleter<E> autoCompleter;
    private final JTextComponent textComp;
    private final JPopupMenu popup = new JPopupMenu();
    private boolean selectsTextOnFocusGain = true;


    /**
     * Constructs a new AutoCompleteSupport for the textbox using the autocompleter and a renderer.
     *
     * @param textComp the textbox component for which autocompletion should be enabled
     * @param autoCompleter the autocompleter providing the data
     * @param renderer the renderer displaying the popup
     */
    public AutoCompleteSupport(JTextComponent textComp, AutoCompleter<E> autoCompleter,
            AutoCompleteRenderer<E> renderer) {
        this.renderer = renderer;
        this.textComp = textComp;
        this.autoCompleter = autoCompleter;

    }

    /**
     * Constructs a new AutoCompleteSupport for the textbox. The possible autocomplete items are displayed as a simple
     * list. The autocompletion items are provided by an AutoCompleter which has to be specified later using
     * {@link setAutoCompleter}.
     *
     * @param textComp the textbox component for which autocompletion should be enabled
     */
    public AutoCompleteSupport(JTextComponent textComp) {
        this(textComp, null, new ListAutoCompleteRenderer<>());
    }

    /**
     * Constructs a new AutoCompleteSupport for the textbox using the autocompleter and a renderer. The possible
     * autocomplete items are displayed as a simple list.
     *
     * @param textComp the textbox component for which autocompletion should be enabled
     * @param autoCompleter the autocompleter providing the data
     */
    public AutoCompleteSupport(JTextComponent textComp, AutoCompleter<E> autoCompleter) {
        this(textComp, autoCompleter, new ListAutoCompleteRenderer<>());
    }

    /**
     * Inits the autocompletion popup. After this method is called, further input in the specified textbox will be
     * autocompleted.
     */
    public void install() {
        // ActionListeners for navigating the suggested autocomplete items with the arrow keys
        final ActionListener upAction = new MoveAction(-1);
        final ActionListener downAction = new MoveAction(1);
        // ActionListener hiding the autocomplete popup
        final ActionListener hidePopupAction = e -> popup.setVisible(false);

        // ActionListener accepting the currently selected item as the autocompletion
        final ActionListener acceptAction = e -> {
            E itemToInsert = renderer.getSelectedItem();
            if (itemToInsert == null) {
                return;
            }

            String toInsert = autoCompleter.getAutoCompleteText(itemToInsert);

            // TODO: The following should be refactored. For example, the autocompleter shouldn't know whether we want to complete one word or multiple.
            // In most fields, we are only interested in the currently edited word, so we
            // seek from the caret backward to the closest space:
            if (!autoCompleter.isSingleUnitField()) {
                // Get position of last word separator (whitespace or comma)
                int priv = textComp.getText().length() - 1;
                while ((priv >= 0) && !Character.isWhitespace(textComp.getText().charAt(priv))
                        && (textComp.getText().charAt(priv) != ',')) {
                    priv--;
                }
                // priv points to whitespace char or priv is -1
                // copy everything from the next char up to the end of "upToCaret"
                textComp.setText(textComp.getText().substring(0, priv + 1) + toInsert);
            } else {
                // For fields such as "journal" it is more reasonable to try to complete on the entire
                // text field content, so we skip the searching and keep the entire part up to the caret:
                textComp.setText(toInsert);
            }
            textComp.setCaretPosition(textComp.getText().length());
            popup.setVisible(false);
        };

        // Create popup
        popup.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.LIGHT_GRAY));
        popup.setPopupSize(textComp.getWidth(), 200);
        popup.setLayout(new BorderLayout());
        popup.setFocusable(false);
        popup.setRequestFocusEnabled(false);
        popup.add(renderer.init(acceptAction));

        // Listen for changes to the text -> update autocomplete suggestions
        textComp.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                postProcessTextChange();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                postProcessTextChange();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                // Do nothing
            }
        });

        // Listen for up/down arrow keys -> move currently selected item up or down
        // We have to reimplement this function here since we cannot be sure that a simple list will be used to display the items
        // So better let the renderer decide what to do.
        // (Moreover, the list does not have the focus so probably would not recognize the keystrokes in the first place.)
        textComp.registerKeyboardAction(downAction, KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0),
                JComponent.WHEN_FOCUSED);

        textComp.registerKeyboardAction(upAction, KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), JComponent.WHEN_FOCUSED);

        // Listen for ESC key -> hide popup
        textComp.registerKeyboardAction(hidePopupAction, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        // Listen to focus events -> select all the text on gaining the focus
        this.textComp.addFocusListener(new ComboBoxEditorFocusHandler());

        // Listen for ENTER key if popup is visible -> accept current autocomplete suggestion
        popup.addPopupMenuListener(new PopupMenuListener() {

            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                textComp.registerKeyboardAction(acceptAction, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
                        JComponent.WHEN_FOCUSED);
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                textComp.unregisterKeyboardAction(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                // Do nothing
            }
        });

    }

    /**
     * Returns whether the text in the textbox is selected when the textbox gains focus. Defaults to true.
     *
     * @return
     */
    public boolean isSelectsTextOnFocusGain() {
        return selectsTextOnFocusGain;
    }

    /**
     * Sets whether the text in the textbox is selected when the textbox gains focus. Default is true.
     *
     * @param selectsTextOnFocusGain new value
     */
    public void setSelectsTextOnFocusGain(boolean selectsTextOnFocusGain) {
        this.selectsTextOnFocusGain = selectsTextOnFocusGain;
    }

    /**
     * The text changed so update autocomplete suggestions accordingly.
     */
    private void postProcessTextChange() {
        if (autoCompleter == null) {
            popup.setVisible(false);
            return;
        }

        String text = textComp.getText();
        List<E> candidates = autoCompleter.complete(text);
        renderer.update(candidates);
        if (textComp.isEnabled() && (!candidates.isEmpty())) {
            renderer.selectItem(0);

            popup.setPopupSize(textComp.getWidth(), 200);
            popup.show(textComp, 0, textComp.getHeight());
        } else {
            popup.setVisible(false);
        }

        if (!textComp.hasFocus()) {
            textComp.requestFocusInWindow();
        }
    }


    /**
     * The action invoked by hitting the up or down arrow key. If the popup is currently shown, that the action is
     * relayed to it. Otherwise the arrow keys trigger the popup.
     */
    private class MoveAction extends AbstractAction {

        private final int offset;


        public MoveAction(int offset) {
            this.offset = offset;
        }

        @Override
        public void actionPerformed(ActionEvent e) {

            if (popup.isVisible()) {
                renderer.selectItemRelative(offset);
            } else {
                popup.show(textComp, 0, textComp.getHeight());
            }

        }
    }

    /**
     * Selects all text when the textbox gains focus. The behavior is controlled by the value returned from
     * {@link AutoCompleteSupport#isSelectsTextOnFocusGain()}.
     */
    private class ComboBoxEditorFocusHandler extends FocusAdapter {

        @Override
        public void focusGained(FocusEvent e) {
            if (isSelectsTextOnFocusGain() && !e.isTemporary()) {
                textComp.selectAll();
            }
        }

        @Override
        public void focusLost(FocusEvent e) {
            // Do nothing
        }
    }


    /**
     * Sets the autocompleter used to present autocomplete suggestions.
     *
     * @param autoCompleter the autocompleter providing the data
     */
    public void setAutoCompleter(AutoCompleter<E> autoCompleter) {
        this.autoCompleter = autoCompleter;
    }
}