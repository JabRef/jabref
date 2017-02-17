package org.jabref.gui.worker.bibsonomy;

import java.util.List;

import javax.swing.JComboBox;

import org.jabref.bibsonomy.BibSonomyProperties;
import org.jabref.gui.bibsonomy.GroupingComboBoxItem;
import org.jabref.gui.util.bibsonomy.LogicInterfaceFactory;
import org.jabref.gui.worker.AbstractWorker;
import org.jabref.logic.l10n.Localization;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bibsonomy.common.enums.GroupingEntity;
import org.bibsonomy.model.Group;
import org.bibsonomy.model.User;
import org.bibsonomy.model.logic.LogicInterface;

/**
 * Fetch the users groups and add them to the "import posts from..." field
 */
public class UpdateVisibilityWorker extends AbstractWorker {

	private static final Log LOGGER = LogFactory.getLog(UpdateVisibilityWorker.class);

    private final JComboBox<? super GroupingComboBoxItem> visibility;
	private final List<GroupingComboBoxItem> defaultGroupings;
    private final String valueToSet;

    public UpdateVisibilityWorker(JComboBox<? super GroupingComboBoxItem> visibility, List<GroupingComboBoxItem> defaultGroupings) {
        this(visibility, defaultGroupings, null);
	}

    public UpdateVisibilityWorker(JComboBox<? super GroupingComboBoxItem> visibility, List<GroupingComboBoxItem> defaultGroupings, String valueToSet) {
        // super() is not called as multithreading is currently not implemented properly within BibSonomy
        this.visibility = visibility;
        this.defaultGroupings = defaultGroupings;
        this.valueToSet = valueToSet;
    }

    public void run() {
        String newValue;
        if (valueToSet == null) {
            newValue = ((GroupingComboBoxItem) visibility.getSelectedItem()).getValue();
        } else {
            newValue = valueToSet;
        }

		visibility.removeAllItems();
		if (defaultGroupings != null) {
			for (GroupingComboBoxItem defaultGrouping : defaultGroupings) {
				visibility.addItem(defaultGrouping);
			}
		}

		try {
            LogicInterface logic = LogicInterfaceFactory.getLogicWithoutFileSupport();
            User user = logic.getUserDetails(BibSonomyProperties.getUsername());

			for (Group g : user.getGroups()) {
				visibility.addItem(new GroupingComboBoxItem(GroupingEntity.GROUP, g.getName()));
			}

			if (newValue != null) {
				int count = visibility.getItemCount();
				for (int i = 0; i < count; i++) {
					GroupingComboBoxItem currentItem = (GroupingComboBoxItem) visibility.getItemAt(i);
					if (currentItem.getValue().equals(newValue)) {
						visibility.setSelectedIndex(i);
					}
				}
			}
		} catch (Exception ex) {
		    // in case of an AuthenticationException, the settings dialog could be reopened
			LOGGER.error(Localization.lang("Failed to get user details for user: %0", BibSonomyProperties.getUsername()), ex);
		}
	}

}
