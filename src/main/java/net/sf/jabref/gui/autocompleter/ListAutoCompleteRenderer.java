package net.sf.jabref.gui.autocompleter;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * Renders possible autocomplete items in form of a simple list.
 *
 * @param <E> the type of the items
 */
public class ListAutoCompleteRenderer<E> extends AutoCompleteRenderer<E> {

    DefaultListModel<E> model = new DefaultListModel<>();
    JList<E> list = new JList<>(model);
    ActionListener acceptAction;

    /**
     * Every selection change by the user is interpreted as accepting the new item as autocompletion. Thus we need this
     * helper variable to prevent that also programmatical trigger an autocompletion.
     */
    Boolean interpretSelectionChangeAsAccept = true;


    @Override
    public void update(E[] autoCompletions) {
        if (autoCompletions != null) {
            list.setListData(autoCompletions);
            list.clearSelection();
        } else {
            model.removeAllElements();
        }
    }

    @Override
    public Component init(ActionListener newAcceptAction) {
        this.acceptAction = newAcceptAction;

        // Init list
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFocusable(false);
        list.setRequestFocusEnabled(false);
        list.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
        list.addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (interpretSelectionChangeAsAccept && (acceptAction != null)) {
                    acceptAction.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null));
                }
            }
        });

        // Init pane containing the list
        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setFocusable(false);
        scrollPane.setRequestFocusEnabled(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        return scrollPane;
    }

    @Override
    public E getSelectedItem() {
        return list.getSelectedValue();
    }

    @Override
    public void selectItem(int index) {
        interpretSelectionChangeAsAccept = false;
        // Set new index if valid otherwise clean selection
        if ((index >= 0) && (index < list.getModel().getSize())) {
            list.setSelectedIndex(index);
            list.ensureIndexIsVisible(index);
        } else {
            list.clearSelection();
        }
        interpretSelectionChangeAsAccept = true;
    }

    @Override
    public int getSelectedIndex() {
        return list.getSelectedIndex();
    }
}