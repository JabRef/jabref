package net.sf.jabref.gui.actions.bibsonomy;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.JComboBox;

import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.bibsonomy.GroupingComboBoxItem;
import net.sf.jabref.gui.worker.bibsonomy.UpdateVisibilityWorker;

/**
 * This runs the {@link UpdateVisibilityWorker}
 */
public class UpdateVisibilityAction extends AbstractBibSonomyAction {

    private JComboBox<GroupingComboBoxItem> visibility;
    private List<GroupingComboBoxItem> defaultGroupings;

    public UpdateVisibilityAction(JabRefFrame jabRefFrame, JComboBox<GroupingComboBoxItem> visibility, List<GroupingComboBoxItem> defaultGroupings) {
        super(jabRefFrame, null, null);

        this.visibility = visibility;
        this.defaultGroupings = defaultGroupings;
    }

    public void actionPerformed(ActionEvent e) {
        UpdateVisibilityWorker worker = new UpdateVisibilityWorker(getJabRefFrame(), visibility, defaultGroupings);
        performAsynchronously(worker);
    }

}
