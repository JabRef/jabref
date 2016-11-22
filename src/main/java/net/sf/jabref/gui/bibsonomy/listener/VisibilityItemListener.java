package net.sf.jabref.gui.bibsonomy.listener;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import net.sf.jabref.bibsonomy.BibSonomyProperties;
import net.sf.jabref.gui.bibsonomy.GroupingComboBoxItem;

/**
 * {@link VisibilityItemListener} saves the current value of "import posts from..." combo box
 *
 * @author Waldemar Biller <biller@cs.uni-kassel.de>
 */
public class VisibilityItemListener implements ItemListener {

	public void itemStateChanged(ItemEvent e) {
		GroupingComboBoxItem item = (GroupingComboBoxItem) e.getItem();
		BibSonomyProperties.setSidePaneVisibilityType(item.getKey());
		BibSonomyProperties.setSidePaneVisibilityName(item.getValue());

		BibSonomyProperties.save();
	}

}
