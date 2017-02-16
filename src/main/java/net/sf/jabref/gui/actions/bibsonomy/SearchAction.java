package net.sf.jabref.gui.actions.bibsonomy;

import java.awt.event.ActionEvent;

import javax.swing.JComboBox;
import javax.swing.JTextField;

import net.sf.jabref.gui.bibsonomy.GroupingComboBoxItem;
import net.sf.jabref.gui.bibsonomy.SearchType;
import net.sf.jabref.gui.bibsonomy.SearchTypeComboBoxItem;
import net.sf.jabref.gui.worker.bibsonomy.ImportPostsByCriteriaWorker;

import org.jabref.gui.IconTheme;
import org.jabref.gui.JabRefFrame;

/**
 * Runs the {@link ImportPostsByCriteriaWorker} with the values of the search text box
 */
public class SearchAction extends AbstractBibSonomyAction {

    private JTextField searchTextField;
    private JComboBox<?> searchTypeComboBox;
    private JComboBox<?> groupingComboBox;

    public SearchAction(JabRefFrame jabRefFrame, JTextField searchTextField, JComboBox<?> searchTypeComboBox, JComboBox<?> groupingComboBox) {
        super(jabRefFrame, "", IconTheme.JabRefIcon.SEARCH.getIcon());

        this.searchTextField = searchTextField;
        this.searchTypeComboBox = searchTypeComboBox;
        this.groupingComboBox = groupingComboBox;
    }

    public void actionPerformed(ActionEvent e) {
        SearchType searchType = ((SearchTypeComboBoxItem) searchTypeComboBox.getSelectedItem()).getKey();
        String criteria = searchTextField.getText();

        if (criteria != null) {
            ImportPostsByCriteriaWorker worker = new ImportPostsByCriteriaWorker(getJabRefFrame(), criteria, searchType, ((GroupingComboBoxItem) groupingComboBox.getSelectedItem()).getKey(), ((GroupingComboBoxItem) groupingComboBox.getSelectedItem()).getValue(), false);
            performAsynchronously(worker);
        }
    }

}
