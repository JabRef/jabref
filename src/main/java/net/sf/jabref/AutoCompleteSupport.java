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
package net.sf.jabref;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.JTextComponent;

import net.sf.jabref.autocompleter.AbstractAutoCompleter;
import net.sf.jabref.autocompleter.AutoCompleter;

/**
 * Based on code by 
 * 	Santhosh Kumar (http://www.jroller.com/santhosh/date/20050620)
 * 	James Lemieux (Glazed Lists AutoCompleteSupport)
 */
public class AutoCompleteSupport<E> {
	AutoCompleteRenderer<E> renderer;
	AutoCompleteFormater<E> formater;
	AutoCompleter<E> autoCompleter;
	JTextComponent textComp;
	JPopupMenu popup = new JPopupMenu();

	/**
	 * <tt>true</tt> if the text in the combobox editor is selected when the
	 * editor gains focus; <tt>false</tt> otherwise.
	 */
	private boolean selectsTextOnFocusGain = true;
	/**
	 * Handles selecting the text in the comboBoxEditorComponent when it gains
	 * focus.
	 */
	private final FocusListener selectTextOnFocusGainHandler = new ComboBoxEditorFocusHandler();

	public AutoCompleteSupport(JTextComponent textComp,
			AutoCompleter<E> autoCompleter,
			AutoCompleteRenderer<E> renderer, AutoCompleteFormater<E> formater) {
		this.renderer = renderer;
		this.formater = formater;
		this.textComp = textComp;
		this.autoCompleter = autoCompleter;

	}

	public AutoCompleteSupport(JTextComponent textComp) {
		this(textComp, null, new DefaultAutoCompletRenderer<E>(), new ToStringAutoCompleteFormater<E>());
	}

	public AutoCompleteSupport(JTextComponent textComp,
			AutoCompleter<E> autoCompleter) {
		this(textComp, autoCompleter, new DefaultAutoCompletRenderer<E>(), new ToStringAutoCompleteFormater<E>());
	}

	public void install() {
		popup.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1,
				Color.LIGHT_GRAY));
		popup.setPopupSize(textComp.getWidth(), 200);
		popup.setLayout(new BorderLayout());
		popup.setFocusable(false);
		popup.setRequestFocusEnabled(false);
		popup.add(renderer.init());

		textComp.getDocument().addDocumentListener(documentListener);

		final Action upAction = new MoveAction(-1);
		final Action downAction = new MoveAction(1);
		final Action hidePopupAction = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				popup.setVisible(false);
			}
		};
		final Action acceptAction = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				E itemToInsert = renderer.getSelectedItem();
				if(itemToInsert == null)
					return;
				
				String toInsert = formater.formatItemToString(itemToInsert);
				
				// In most fields, we are only interested in the currently edited word, so we
	            // seek from the caret backward to the closest space:
	            if (!autoCompleter.isSingleUnitField()) {        	
	            	// Get position of last word seperator (whitespace or comma)
	                int piv = textComp.getText().length() - 1;
	                while ((piv >= 0) && !Character.isWhitespace(textComp.getText().charAt(piv)) && textComp.getText().charAt(piv) != ',') {
	                    piv--;
	                }
	                // priv points to whitespace char or priv is -1
	                // copy everything from the next char up to the end of "upToCaret" 
	                textComp.setText(textComp.getText().substring(0, piv + 1) + toInsert);
	            } else {
		            // For fields such as "journal" it is more reasonable to try to complete on the entire
		            // text field content, so we skip the searching and keep the entire part up to the caret:
		            textComp.setText(toInsert);
	            }
	            textComp.setCaretPosition(textComp.getText().length());
	            popup.setVisible(false);
			}
		};
		renderer.registerAcceptAction(acceptAction);

		textComp.registerKeyboardAction(downAction,
				KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0),
				JComponent.WHEN_FOCUSED);

		textComp.registerKeyboardAction(upAction,
				KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0),
				JComponent.WHEN_FOCUSED);

		// add a FocusListener to the ComboBoxEditor which selects all text when
		// focus is gained
		this.textComp.addFocusListener(selectTextOnFocusGainHandler);

		textComp.registerKeyboardAction(hidePopupAction,
				KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
				JComponent.WHEN_IN_FOCUSED_WINDOW);

		popup.addPopupMenuListener(new PopupMenuListener() {
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				textComp.registerKeyboardAction(acceptAction,
						KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
						JComponent.WHEN_FOCUSED);
			}

			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
				textComp.unregisterKeyboardAction(KeyStroke.getKeyStroke(
						KeyEvent.VK_ENTER, 0));
			}

			public void popupMenuCanceled(PopupMenuEvent e) {
			}
		});

	}

	/**
	 * Returns <tt>true</tt> if the combo box editor text is selected when it
	 * gains focus; <tt>false</tt> otherwise.
	 */
	public boolean getSelectsTextOnFocusGain() {
		return selectsTextOnFocusGain;
	}

	/**
	 * If <code>selectsTextOnFocusGain</code> is <tt>true</tt>, all text in the
	 * editor is selected when the combo box editor gains focus. If it is
	 * <tt>false</tt> the selection state of the editor is not effected by focus
	 * changes.
	 *
	 * @throws IllegalStateException
	 *             if this method is called from any Thread other than the Swing
	 *             Event Dispatch Thread
	 */
	public void setSelectsTextOnFocusGain(boolean selectsTextOnFocusGain) {
		this.selectsTextOnFocusGain = selectsTextOnFocusGain;
	}

	DocumentListener documentListener = new DocumentListener() {
		public void insertUpdate(DocumentEvent e) {
			postProcessDocumentChange();
		}

		public void removeUpdate(DocumentEvent e) {
			postProcessDocumentChange();
		}

		public void changedUpdate(DocumentEvent e) {
		}
	};

	private void postProcessDocumentChange() {
		String text = textComp.getText();

		popup.setVisible(false);
		popup.setPopupSize(textComp.getWidth(), 200);
		if (autoCompleter == null)
			return;
		if (textComp.isEnabled()
				&& renderer.updateListData(autoCompleter.complete(text))) {
			renderer.selectAutoCompleteTerm(text);

			// popup.repaint();

			// Hide and show then, to recalculate height
			// popup.hide();
			popup.show(textComp, 0, textComp.getHeight());
		} else
			popup.setVisible(false);
		if (!textComp.hasFocus())
			textComp.requestFocusInWindow();
	}

	/**
	 * The action invoked by hitting the up or down arrow key.
	 */
	private class MoveAction extends AbstractAction {
		private final int offset;

		public MoveAction(int offset) {
			this.offset = offset;
		}

		public void actionPerformed(ActionEvent e) {

			if (popup.isVisible()) {
				renderer.selectNewItem(offset);
			} else {
				popup.show(textComp, 0, textComp.getHeight());
			}

		}
	}

	/**
	 * When the user selects a value from the popup with the mouse, we want to
	 * honour their selection *without* attempting to autocomplete it to a new
	 * term. Otherwise, it is possible that selections which are prefixes for
	 * values that appear higher in the ComboBoxModel cannot be selected by the
	 * mouse since they can always be successfully autocompleted to another
	 * term.
	 */
	/*
	 * private class PopupMouseHandler extends MouseAdapter {
	 * 
	 * @Override public void mousePressed(MouseEvent e) { doNotAutoComplete =
	 * true; }
	 * 
	 * @Override public void mouseReleased(MouseEvent e) { doNotAutoComplete =
	 * false; } }
	 */

	/**
	 * To emulate Firefox behaviour, all text in the ComboBoxEditor is selected
	 * from beginning to end when the ComboBoxEditor gains focus if the value
	 * returned from {@link AutoCompleteSupport#getSelectsTextOnFocusGain()}
	 * allows this behaviour. In addition, the JPopupMenu is hidden when the
	 * ComboBoxEditor loses focus if the value returned from
	 * {@link AutoCompleteSupport#getHidesPopupOnFocusLost()} allows this
	 * behaviour.
	 */
	private class ComboBoxEditorFocusHandler extends FocusAdapter {
		@Override
		public void focusGained(FocusEvent e) {
			if (getSelectsTextOnFocusGain() && !e.isTemporary())
				textComp.selectAll();
		}

		@Override
		public void focusLost(FocusEvent e) {
		}
	}

	public void setAutoCompleter(AutoCompleter<E> autoCompleter) {
		this.autoCompleter = autoCompleter;
	}
}