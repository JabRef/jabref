package net.sf.jabref.gui.databaseProperties;

import javax.swing.*;
import javax.swing.event.ListDataListener;
import java.util.List;

class SaveActionsListModel<SaveAction> implements ListModel<SaveAction> {

    private List<SaveAction> saveActions;

    public SaveActionsListModel(List<SaveAction> saveActions){
        if(saveActions == null){
            throw new IllegalArgumentException("Input data must not be null");
        }

        this.saveActions = saveActions;
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

    }

    @Override
    public void removeListDataListener(ListDataListener l) {

    }
}
