package net.sf.jabref;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

public class PreviewPrefsTab extends JPanel implements PrefsTab {

	JabRefPreferences _prefs;

	JabRefFrame _frame;

	JPanel pan = new JPanel();

	GridBagLayout gbl = new GridBagLayout();

	GridBagConstraints con = new GridBagConstraints();

	JTextArea layout1 = new JTextArea("", 1, 1), layout2 = new JTextArea("", 1, 1);

	JButton def1 = new JButton(Globals.lang("Default")),
		def2 = new JButton(Globals.lang("Default")), test1 = new JButton(Globals.lang("Test")),
		test2 = new JButton(Globals.lang("Test")), help;

	JPanel p1 = new JPanel(), p2 = new JPanel();

	JScrollPane sp1 = new JScrollPane(layout1), sp2 = new JScrollPane(layout2);

	private static BibtexEntry entry;

	public PreviewPrefsTab(JabRefPreferences prefs) {
		_prefs = prefs;

		p1.setLayout(gbl);
		p2.setLayout(gbl);

		setLayout(gbl);
		JLabel lab;
		lab = new JLabel(Globals.lang("Preview") + " 1");
		con.anchor = GridBagConstraints.WEST;
		con.gridwidth = GridBagConstraints.REMAINDER;
		con.fill = GridBagConstraints.BOTH;
		con.weightx = 1;
		con.weighty = 0;
		con.insets = new Insets(2, 2, 2, 2);
		gbl.setConstraints(lab, con);
		// p1.add(lab);
		con.weighty = 1;
		gbl.setConstraints(sp1, con);
		p1.add(sp1);
		con.weighty = 0;
		con.gridwidth = 1;
		con.weightx = 0;
		con.fill = GridBagConstraints.NONE;
		con.anchor = GridBagConstraints.WEST;
		gbl.setConstraints(test1, con);
		p1.add(test1);
		gbl.setConstraints(def1, con);
		p1.add(def1);
		con.gridwidth = GridBagConstraints.REMAINDER;
		JPanel pan = new JPanel();
		con.weightx = 1;
		gbl.setConstraints(pan, con);
		p1.add(pan);
		lab = new JLabel(Globals.lang("Preview") + " 2");
		gbl.setConstraints(lab, con);
		// p2.add(lab);
		con.weighty = 1;
		con.fill = GridBagConstraints.BOTH;
		gbl.setConstraints(sp2, con);
		p2.add(sp2);
		con.weighty = 0;
		con.weightx = 0;
		con.fill = GridBagConstraints.NONE;
		con.gridwidth = 1;
		gbl.setConstraints(test2, con);
		p2.add(test2);
		gbl.setConstraints(def2, con);
		p2.add(def2);
		con.gridwidth = 1;
		pan = new JPanel();
		con.weightx = 1;
		gbl.setConstraints(pan, con);
		p2.add(pan);

		{ // Help Button
			HelpAction helpAction = new HelpAction(Globals.helpDiag, GUIGlobals.previewHelp,
				Globals.lang("Help on Preview Settings"), GUIGlobals.getIconUrl("helpSmall"));
			JButton help = helpAction.getIconButton();
			con.weightx = 0;
			con.gridwidth = GridBagConstraints.REMAINDER;
			gbl.setConstraints(help, con);
			p2.add(help);
		}

		con.weightx = 1;
		con.weighty = 0;
		con.fill = GridBagConstraints.BOTH;
		con.gridwidth = GridBagConstraints.REMAINDER;
		lab = new JLabel(Globals.lang("Preview") + " 1");
		gbl.setConstraints(lab, con);
		add(lab);
		con.weighty = 1;
		gbl.setConstraints(p1, con);
		add(p1);
		lab = new JLabel(Globals.lang("Preview") + " 2");
		con.weighty = 0;
		JSeparator sep = new JSeparator(JSeparator.HORIZONTAL);
		gbl.setConstraints(sep, con);
		add(sep);
		gbl.setConstraints(lab, con);
		add(lab);
		con.weighty = 1;
		gbl.setConstraints(p2, con);
		add(p2);

		def1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String tmp = layout1.getText().replaceAll("\n", "__NEWLINE__");
				_prefs.remove("preview0");
				layout1.setText(_prefs.get("preview0").replaceAll("__NEWLINE__", "\n"));
				_prefs.put("preview0", tmp);
			}
		});
		def2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String tmp = layout2.getText().replaceAll("\n", "__NEWLINE__");
				_prefs.remove("preview1");
				layout2.setText(_prefs.get("preview1").replaceAll("__NEWLINE__", "\n"));
				_prefs.put("preview1", tmp);
			}
		});

		test1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				getTestEntry();
				PreviewPanel testPanel = new PreviewPanel(null, entry, null , new MetaData(), layout1.getText());
				testPanel.setPreferredSize(new Dimension(800, 350));
				JOptionPane.showMessageDialog(null, testPanel, Globals.lang("Preview"),
					JOptionPane.PLAIN_MESSAGE);
			}
		});

		test2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				getTestEntry();
				PreviewPanel testPanel = new PreviewPanel(null, entry, null, new MetaData(), layout2.getText());
				testPanel.setPreferredSize(new Dimension(800, 350));
				JOptionPane.showMessageDialog(null, new JScrollPane(testPanel), Globals
					.lang("Preview"), JOptionPane.PLAIN_MESSAGE);
			}
		});
	}

	public static BibtexEntry getTestEntry() {
		if (entry != null)
			return entry;
		entry = new BibtexEntry(Util.createNeutralId(), BibtexEntryType.getType("article"));
		entry.setField(BibtexFields.KEY_FIELD, "conceicao1997");
		entry
			.setField(
				"author",
				"Luis E. C. Conceic{\\~a}o and Terje van der Meeren and Johan A. J. Verreth and M S. Evjen and D. F. Houlihan and H. J. Fyhn");
		entry
			.setField(
				"title",
				"Amino acid metabolism and protein turnover in larval turbot (Scophthalmus maximus) fed natural zooplankton or Artemia");
		entry.setField("year", "1997");
		entry.setField("journal", "Marine Biology");
		entry.setField("month", "January");
		entry.setField("number", "2");
		entry.setField("volume", "123");
		entry.setField("pdf", "conceicao1997.pdf");
		entry.setField("pages", "255--265");
		entry.setField("keywords", "energetics, artemia, metabolism, amino acid, turbot");
		entry.setField("url",
			"http://ejournals.ebsco.com/direct.asp?ArticleID=TYY4NT82XA9H7R8PFPPV");
		entry
			.setField(
				"abstract",
				"Abstract The present paper studied the influence of different food regimes "
					+ "on the free amino acid (FAA) pool, the rate of protein turnover, the flux of amino acids, and "
					+ "their relation to growth of larval turbot (Scophthalmus maximus L.) from first feeding until "
					+ "metamorphosis. The amino acid profile of protein was stable during the larval period although "
					+ "some small, but significant, differences were found. Turbot larvae had proteins which were rich "
					+ "in leucine and aspartate, and poor in glutamate, suggesting a high leucine requirement. The "
					+ "profile of the FAA pool was highly variable and quite different from the amino acid profile in "
					+ "protein. The proportion of essential FAA decreased with development. High contents of free tyrosine "
					+ "and phenylalanine were found on Day 3, while free taurine was present at high levels throughout "
					+ "the experimental period. Larval growth rates were positively correlated with taurine levels, "
					+ "suggesting a dietary dependency for taurine and/or sulphur amino acids.\n\nReduced growth rates in "
					+ "Artemia-fed larvae were associated with lower levels of free methionine, indicating that this diet "
					+ "is deficient in methionine for turbot larvae. Leucine might also be limiting turbot growth as the "
					+ "different diet organisms had lower levels of this amino acid in the free pool than was found in the "
					+ "larval protein. A previously presented model was used to describe the flux of amino acids in growing "
					+ "turbot larvae. The FAA pool was found to be small and variable. It was estimated that the daily dietary "
					+ "amino acid intake might be up to ten times the larval FAA pool. In addition, protein synthesis and "
					+ "protein degradation might daily remove and return, respectively, the equivalent of up to 20 and 10 "
					+ "times the size of the FAA pool. In an early phase (Day 11) high growth rates were associated with a "
					+ "relatively low protein turnover, while at a later stage (Day 17), a much higher turnover was observed.");
		return entry;
	}

	public void setValues() {
		layout1.setText(_prefs.get("preview0").replaceAll("__NEWLINE__", "\n"));
		layout2.setText(_prefs.get("preview1").replaceAll("__NEWLINE__", "\n"));
	}

	public void storeSettings() {
		_prefs.put("preview0", layout1.getText().replaceAll("\n", "__NEWLINE__"));
		_prefs.put("preview1", layout2.getText().replaceAll("\n", "__NEWLINE__"));
	}

	public boolean readyToClose() {
		return true;
	}

	public String getTabName() {
		return Globals.lang("Entry preview");
	}

}
