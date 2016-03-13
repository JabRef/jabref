package net.sf.jabref.gui.dbproperties;

import net.sf.jabref.exporter.FieldFormatterCleanups;
import net.sf.jabref.logic.cleanup.FieldFormatterCleanup;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

class SaveActionsListModel implements ListModel<FieldFormatterCleanup> {

    private List<FieldFormatterCleanup> saveActions;

    private List<ListDataListener> listeners;

    public SaveActionsListModel(List<FieldFormatterCleanup> saveActions) {
        Objects.requireNonNull(saveActions);

        this.saveActions = saveActions;
        listeners = new ArrayList<>();
    }

    public void addSaveAction(FieldFormatterCleanup action) {
        if (!saveActions.contains(action)) {
            saveActions.add(action);
            for (ListDataListener listener : listeners) {
                listener.intervalAdded(new ListDataEvent(action, ListDataEvent.INTERVAL_ADDED, saveActions.size(), saveActions.size()));
            }
        }
    }

    public void removeAtIndex(int index) {
        if (index < 0 || index > saveActions.size()) {
            throw new IndexOutOfBoundsException("Index must be within 0 and " + saveActions.size() + " but was " + index);
        }

        FieldFormatterCleanup action = saveActions.remove(index);

        for (ListDataListener listener : listeners) {
            listener.intervalAdded(new ListDataEvent(action, ListDataEvent.INTERVAL_REMOVED, index, index));
        }
    }

    public List<FieldFormatterCleanup> getAllActions() {
        return saveActions;
    }


    @Override
    public int getSize() {
        return saveActions.size();
    }

    @Override
    public FieldFormatterCleanup getElementAt(int index) {
        return saveActions.get(index);
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
        saveActions = new ArrayList<>(defaultFormatters.getConfiguredActions());
        for (ListDataListener listener : listeners) {
            listener.contentsChanged(new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, saveActions.size()));
        }
    }
}
