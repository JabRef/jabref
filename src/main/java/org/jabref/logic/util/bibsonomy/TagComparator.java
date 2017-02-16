package org.jabref.logic.util.bibsonomy;

import java.util.Comparator;

import org.bibsonomy.common.enums.GroupingEntity;
import org.bibsonomy.model.Tag;

public class TagComparator implements Comparator<Tag> {

	private static final int LESS_THAN = -100, EQUAL = 0, GREATER_THAN = 100;
	private GroupingEntity grouping;

	public TagComparator(GroupingEntity grouping) {

		this.grouping = grouping;
	}

	public int compare(Tag currentTag, Tag nextTag) {
		switch(grouping) {
			case USER:
				if(currentTag.getUsercount() < nextTag.getUsercount())
					return GREATER_THAN;
				else if(currentTag.getUsercount() > nextTag.getUsercount())
					return LESS_THAN;
				else return EQUAL;
			default:
				if(currentTag.getGlobalcount() < nextTag.getGlobalcount())
					return GREATER_THAN;
				else if(currentTag.getGlobalcount() > nextTag.getGlobalcount())
					return LESS_THAN;
				else return EQUAL;
		}
	}


}
