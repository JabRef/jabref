package org.jabref.gui.worker.bibsonomy;

import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import javax.swing.JEditorPane;

import org.jabref.bibsonomy.BibSonomyProperties;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.util.bibsonomy.LogicInterfaceFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.metadata.MetaData;

import org.bibsonomy.common.enums.GroupingEntity;
import org.bibsonomy.model.BibTex;
import org.bibsonomy.model.Tag;
import org.bibsonomy.model.logic.LogicInterface;


/**
 * Fetch tags from the service and display them in a tag cloud
 */
public class RefreshTagListWorker extends AbstractBibSonomyWorker {

	private JEditorPane tagCloud;

	private GroupingEntity grouping;
	private String groupingValue;
	private List<Tag> tags;

	public RefreshTagListWorker(JabRefFrame jabRefFrame, JEditorPane tagCloud, GroupingEntity grouping, String groupingValue) {
		super(jabRefFrame);
		this.tagCloud = tagCloud;
		this.grouping = grouping;
		this.groupingValue = groupingValue;
		this.tags = new LinkedList<>();
	}

	public void run() {
		MetaData metaData = null;
		//Vector<String> keywords = null;
		Vector<String> keywords = null;
		if (jabRefFrame.getCurrentBasePanel() != null) {
			//TODO: Investigate changes in MetaData - zellerdev
			//keywords = metaData.getData(MetaData.SELECTOR_META_PREFIX + "keywords");
			metaData = jabRefFrame.getCurrentBasePanel().getBibDatabaseContext().getMetaData();
			keywords = metaData.getBibsonomyData("keywords");

		}

		int start = 0;
		int end = BibSonomyProperties.getTagCloudSize();
		int max = 1;
		int min = 1;

		//in case of fetching all tags we only get the first 100 most popular
		if (grouping == GroupingEntity.ALL && BibSonomyProperties.getTagCloudSize() > 100)
			end = 100;

        LogicInterface logic = LogicInterfaceFactory.getLogic(jabRefFrame.getCurrentBasePanel().getBibDatabaseContext());
        final List<Tag> result = logic.getTags(BibTex.class, grouping, groupingValue, null, null, null, null, null, BibSonomyProperties.getTagCloudOrder(), null, null, start, end);
		tags.addAll(result);

		for (Tag tag : tags) {
			switch (grouping) {
				case USER:
					max = Math.max(max, tag.getUsercount());
					min = Math.min(min, tag.getUsercount());
					break;
				default:
					max = Math.max(max, tag.getGlobalcount());
					min = Math.min(min, tag.getGlobalcount());
					break;
			}
		}
		if (max == min) {
			max++;
		}

		StringBuffer tagList = new StringBuffer();
		tagList.append("<div style='text-align: center; font-family: Arial, Helvetica, sans;'>");
		int size;

		tagCloud.removeAll();

		// calculate the tag cloud
		for (Tag tag : tags) {

			if (keywords != null)
				keywords.add(tag.getName());
			jabRefFrame.output(Localization.lang("Added tag: %0", tag.getName() ));

			switch (grouping) {
				case USER:
					size = Math.round(12 * (tag.getUsercount() - min) / (max - min)) + 12;
					break;
				default:
					size = Math.round(12 * (tag.getGlobalcount() - min) / (max - min)) + 12;
					break;
			}

			tagList.append("<span style='display: inline'>"
					+ "<a style='color: #006699; display: inline; text-decoration: none; font-size: "
					+ size
					+ "' href='"
					+ tag.getName()
					+ "'>"
					+ tag.getName()
					+ "</a>"
					+ "</span> ");


		}

		tagList.append("</div>");
		tagCloud.setText(tagList.toString());

		jabRefFrame.validate();
		jabRefFrame.repaint();

		if (metaData != null && keywords != null) {
			//TODO: Investigate changes in MetaData - zellerdev
			//metaData.putData(MetaData.SELECTOR_META_PREFIX + "keywords", keywords);
			jabRefFrame.getCurrentBasePanel().getBibDatabaseContext().getMetaData().addBibsonomyData("keywords", keywords);
		}
		jabRefFrame.output(Localization.lang("Done"));
	}

}
