package net.sf.jabref.gui.databaseProperties;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.ArrayList;
import java.util.List;

class SaveActionsListModel<SaveAction> implements ListModel<SaveAction> {

    private List<SaveAction> saveActions;

    private List<ListDataListener> listeners;

    public SaveActionsListModel(List<SaveAction> saveActions) {
        if (saveActions == null) {
            throw new IllegalArgumentException("Input data must not be null");
        }

        this.saveActions = saveActions;
        listeners = new ArrayList<>();
    }

    public void addSaveAction(SaveAction action) {
        saveActions.add(action);
        for (ListDataListener listener : listeners) {
            listener.intervalAdded(new ListDataEvent(action, ListDataEvent.INTERVAL_ADDED, saveActions.size(), saveActions.size()));
        }
    }

    public List<SaveAction> getAllActions() {
        return saveActions;
    }


    @Override
    public int getSize() {
        return saveActions.size();
    }

    @Override
    public SaveAction getElementAt(int index) {
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
}
