package net.sf.jabref.gui.cleanup;

import net.sf.jabref.exporter.FieldFormatterCleanups;
import net.sf.jabref.logic.cleanup.FieldFormatterCleanup;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CleanupActionsListModel implements ListModel<FieldFormatterCleanup> {

    private List<FieldFormatterCleanup> cleanupActions;
    private final List<ListDataListener> listeners = new ArrayList<>();


    public CleanupActionsListModel(List<FieldFormatterCleanup> cleanupAction) {
        Objects.requireNonNull(cleanupAction);
        this.cleanupActions = cleanupAction;

    }

    public void addCleanupAction(FieldFormatterCleanup action) {
        if (!cleanupActions.contains(action)) {
            cleanupActions.add(action);
            for (ListDataListener listener : listeners) {
                listener.intervalAdded(new ListDataEvent(action, ListDataEvent.INTERVAL_ADDED, cleanupActions.size(),
                        cleanupActions.size()));
            }
        }
    }

    /**
     * Removes the action at the specified index from the list.
     * Removal is only done when index {@code >=0} and index {@code<=} list size
     * @param index The index to remove
     */
    public void removeAtIndex(int index) {

        if ((index >= 0) && (index < cleanupActions.size())) {
            FieldFormatterCleanup action = cleanupActions.remove(index);
            for (ListDataListener listener : listeners) {
                listener.intervalRemoved(new ListDataEvent(action, ListDataEvent.INTERVAL_REMOVED, index, index));
            }
        }
    }

    public List<FieldFormatterCleanup> getAllActions() {
        return cleanupActions;
    }

    @Override
    public int getSize() {
        return cleanupActions.size();
    }

    @Override
    public FieldFormatterCleanup getElementAt(int index) {
        return cleanupActions.get(index);
    }

    @Override
    public void addListDataListener(ListDataListener l) {
        listeners.add(l);
    }

    @Override
    public void removeListDataListener(ListDataListener l) {
        listeners.remove(l);
    }

    public void reset(FieldFormatterCleanups defaultFormatters) {
        cleanupActions = new ArrayList<>(defaultFormatters.getConfiguredActions());
        for (ListDataListener listener : listeners) {
            listener.contentsChanged(new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, cleanupActions.size()));
        }
    }
}
