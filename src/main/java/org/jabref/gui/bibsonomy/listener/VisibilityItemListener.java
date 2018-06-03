package org.jabref.gui.bibsonomy.listener;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import org.jabref.bibsonomy.BibSonomyProperties;
import org.jabref.gui.bibsonomy.GroupingComboBoxItem;

/**
 * Saves the current value of "import posts from..." combo box
 */
public class VisibilityItemListener implements ItemListener {

	public void itemStateChanged(ItemEvent e) {
		GroupingComboBoxItem item = (GroupingComboBoxItem) e.getItem();
		BibSonomyProperties.setSidePaneVisibilityType(item.getKey());
		BibSonomyProperties.setSidePaneVisibilityName(item.getValue());

		BibSonomyProperties.save();
	}

}
