package net.sf.jabref.bibsonomy;

import java.awt.Dimension;

import javax.swing.ImageIcon;
import javax.swing.JMenuItem;

import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.SidePaneComponent;
import net.sf.jabref.gui.SidePaneManager;
import net.sf.jabref.logic.l10n.Localization;

import net.sf.jabref.gui.bibsonomy.BibSonomyMenuItem;
import net.sf.jabref.gui.bibsonomy.BibSonomySidePanel;
import net.sf.jabref.gui.bibsonomy.BibSonomyToolBarExtender;
import net.sf.jabref.gui.bibsonomy.EntryEditorTabExtender;
import net.sf.jabref.gui.bibsonomy.listener.BibSonomyDataBaseChangeListener;
import net.sf.jabref.gui.bibsonomy.listener.TabbedPaneChangeListener;

/**
 * {@link BibSonomySidePaneComponent} holds the dimension of the {@link BibSonomySidePanel}.
 * Additionally it sets the icon and the name.
 *
 * @author Waldemar Biller <biller@cs.uni-kassel.de>
 */
public class BibSonomySidePaneComponent extends SidePaneComponent {

	private SidePaneManager manager;
	private JabRefFrame jabRefFrame;

	public BibSonomySidePaneComponent(SidePaneManager manager, JabRefFrame jabRefFrame) {
		// set the icon and the name
		super(manager, new ImageIcon(BibSonomySidePaneComponent.class.getResource("/images/bibsonomy/tag-label.png")), Localization.lang("BibSonomy"));

		this.manager = manager;
		this.jabRefFrame = jabRefFrame;

		init();

		// add the sidepanel
		super.add(new BibSonomySidePanel(jabRefFrame));
	}

	/**
	 * get the jabRefFrame
	 *
	 * @return The {@link JabRefFrame}
	 */
	public JabRefFrame getJabRefFrame() {
		return jabRefFrame;
	}

	/**
	 * get the sidePaneManager
	 *
	 * @return The {@link SidePaneManager}
	 */
	public SidePaneManager getSidePaneManager() {
		return manager;
	}

	@Override
	public Dimension getPreferredSize() {
		final int SPLIT_PANE_DIVIDER_LOCATION = 145 + 15; // + 15 for possible scrollbar.
		return new Dimension(SPLIT_PANE_DIVIDER_LOCATION, 550);
	}

	@Override
	public Dimension getMaximumSize() {
		return getPreferredSize();
	}

	@Override
	public int getRescalingWeight() {
		return 0;
	}

	@Override
	public ToggleAction getToggleAction() {
		return null;
	}

	public void init() {
		// create a ChangeListener to react on newly added entries.
		BibSonomyDataBaseChangeListener bibsonomyDataBaseChangeListener = new BibSonomyDataBaseChangeListener(jabRefFrame);

		// set a ChangeListener of the Tabbed Pane which registers the databasechangelistener to all database tabs that are added later
		jabRefFrame.getTabbedPane().addChangeListener(new TabbedPaneChangeListener(bibsonomyDataBaseChangeListener));
		// ...but maybe we were too late: Tabs are created by another (swing)thread so the initial tab change event after tab(and database) creation may be over already.
		// Therefore add the listener to the database of the current tab if it is already present.
		if (jabRefFrame.getCurrentBasePanel() != null && jabRefFrame.getCurrentBasePanel().getDatabase() != null) {
			jabRefFrame.getCurrentBasePanel().getDatabase().registerListener(bibsonomyDataBaseChangeListener);
		}

		BibSonomyToolBarExtender.extend(jabRefFrame, this);
		EntryEditorTabExtender.extend();
	}

	/**
	 * Returns a JMenuItem to use Bibsonomy methods
	 * @return A JMenuItem containing the Menu of Bibsonomy
	 */
	public JMenuItem getMenuItem() {
		return new BibSonomyMenuItem(this);
	}
}
