package net.sf.jabref.gui.worker.bibsonomy;

import java.util.List;

import javax.swing.JComboBox;

import net.sf.jabref.bibsonomy.BibSonomyProperties;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.actions.bibsonomy.ShowSettingsDialogAction;
import net.sf.jabref.gui.bibsonomy.GroupingComboBoxItem;
import net.sf.jabref.logic.l10n.Localization;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bibsonomy.common.enums.GroupingEntity;
import org.bibsonomy.model.Group;
import org.bibsonomy.model.User;
import org.bibsonomy.rest.exceptions.AuthenticationException;

/**
 * Fetch the users groups and add them to the "import posts from..." field
 */
public class UpdateVisibilityWorker extends AbstractBibSonomyWorker {

	private static final Log LOGGER = LogFactory.getLog(UpdateVisibilityWorker.class);

	private JComboBox<? super GroupingComboBoxItem> visibility;
	private List<GroupingComboBoxItem> defaultGroupings;

	public UpdateVisibilityWorker(JabRefFrame jabRefFrame, JComboBox<? super GroupingComboBoxItem> visibility, List<GroupingComboBoxItem> defaultGroupings) {
		super(jabRefFrame);
		this.visibility = visibility;
		this.defaultGroupings = defaultGroupings;
	}

	public void run() {
		GroupingComboBoxItem item = (GroupingComboBoxItem) visibility.getSelectedItem();

		visibility.removeAllItems();
		if (defaultGroupings != null) {
			for (GroupingComboBoxItem defaultGrouping : defaultGroupings) {
				visibility.addItem(defaultGrouping);
			}
		}

		try {
			User user = getLogic().getUserDetails(BibSonomyProperties.getUsername());

			for (Group g : user.getGroups()) {
				visibility.addItem(new GroupingComboBoxItem(GroupingEntity.GROUP, g.getName()));
			}

			if (item != null) {
				int count = visibility.getItemCount();
				for (int i = 0; i < count; i++) {
					GroupingComboBoxItem currentItem = (GroupingComboBoxItem) visibility.getItemAt(i);
					if (currentItem.getValue().equals(item.getValue())) {
						visibility.setSelectedIndex(i);
					}
				}

			}

		} catch (AuthenticationException ex) {
			(new ShowSettingsDialogAction(jabRefFrame)).actionPerformed(null);
		} catch (Exception ex) {
			LOGGER.error(Localization.lang("Failed to get user details for user: %0", BibSonomyProperties.getUsername()), ex);
		}
	}

}
