package org.jabref.gui.actions.bibsonomy;

import java.awt.event.ActionEvent;
import java.util.Objects;

import javax.swing.JComboBox;
import javax.swing.JTextField;

import org.jabref.gui.IconTheme;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.bibsonomy.GroupingComboBoxItem;
import org.jabref.gui.bibsonomy.SearchType;
import org.jabref.gui.bibsonomy.SearchTypeComboBoxItem;
import org.jabref.gui.worker.bibsonomy.ImportPostsByCriteriaWorker;

/**
 * Runs the {@link ImportPostsByCriteriaWorker} with the values of the search text box
 */
public class SearchAction extends AbstractBibSonomyAction {

    private JTextField searchTextField;
    private JComboBox<SearchTypeComboBoxItem> searchTypeComboBox;
    private JComboBox<GroupingComboBoxItem> visibilityComboBox;

    public SearchAction(JabRefFrame jabRefFrame,
                        JTextField searchTextField,
                        JComboBox<SearchTypeComboBoxItem> searchTypeComboBox,
                        JComboBox<GroupingComboBoxItem> visibilityComboBox) {
        super(Objects.requireNonNull(jabRefFrame), "", IconTheme.JabRefIcon.SEARCH.getIcon());

        this.searchTextField = Objects.requireNonNull(searchTextField);
        this.searchTypeComboBox = Objects.requireNonNull(searchTypeComboBox);
        this.visibilityComboBox = Objects.requireNonNull(visibilityComboBox);
    }

    public void actionPerformed(ActionEvent e) {
        SearchType searchType = ((SearchTypeComboBoxItem) searchTypeComboBox.getSelectedItem()).getKey();
        String criteria = searchTextField.getText();

        if (criteria != null) {
            ImportPostsByCriteriaWorker worker = new ImportPostsByCriteriaWorker(
                    getJabRefFrame(),
                    criteria,
                    searchType,
                    ((GroupingComboBoxItem) visibilityComboBox.getSelectedItem()).getKey(),
                    ((GroupingComboBoxItem) visibilityComboBox.getSelectedItem()).getValue(),
                    false);
            performAsynchronously(worker);
        }
    }
}
