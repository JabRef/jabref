package net.sf.jabref.gui.autocompleter;

import java.awt.Component;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * Renders the list of possible autocomplete items. Also takes care of the currently selected item.
 *
 * @param <E> the type of the items
 */
public abstract class AutoCompleteRenderer<E> {

    /**
     * Refreshes the list of possible autocomplete items. Clears the currently selected item.
     *
     * @param items list of possible autocomplete items
     */
    public abstract void update(List<E> items);

    /**
     * Creates the control which will be shown in the autocomplete popup.
     *
     * @param acceptAction the action to be performed if the current selection is chosen as the autocompletion
     * @return the control to be added to the autocomplete popup
     */
    public abstract Component init(ActionListener acceptAction);

    /**
     * Selects the item at the given position. If the specified index is not valid, then the selection will be cleared.
     *
     * @param index position of the item
     */
    public abstract void selectItem(int index);

    /**
     * Selects the item relative to the currently selected item. If the specified offset is not valid, then the
     * selection will be cleared.
     *
     * @param offset offset of the item
     */
    public void selectItemRelative(int offset) {
        int newIndex = getSelectedIndex() + offset;
        selectItem(newIndex);
    }

    /**
     * Returns the index of the currently selected item.
     *
     * @return index of the selected item
     */
    public abstract int getSelectedIndex();

    /**
     * Returns the currently selected item.
     *
     * @return selected item
     */
    public abstract E getSelectedItem();
}