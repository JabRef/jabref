/**
 *  
 *  JabRef Bibsonomy Plug-in - Plugin for the reference management 
 * 		software JabRef (http://jabref.sourceforge.net/) 
 * 		to fetch, store and delete entries from BibSonomy.
 *   
 *  Copyright (C) 2008 - 2011 Knowledge & Data Engineering Group, 
 *                            University of Kassel, Germany
 *                            http://www.kde.cs.uni-kassel.de/
 *  
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.bibsonomy.plugin.jabref.gui;

import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.JabRefFrame;
import net.sf.jabref.Util;

import org.bibsonomy.model.Post;
import org.bibsonomy.model.Resource;
import org.bibsonomy.plugin.jabref.util.JabRefModelConverter;

/**
 * @author Waldemar Biller <wbi@cs.uni-kassel.de>
 * 
 */
public class CompareDialog extends JDialog {

	public static final int KEEP_LOCAL = 1000;
	public static final int KEEP_REMOTE = 2000;
	public static final int KEEP_LOCAL_ALWAYS = 3000;
	public static final int KEEP_REMOTE_ALWAYS = 4000;

	private static final long serialVersionUID = 1L;
	private JButton keepLocal = null;
	private JButton keepRemote = null;
	private JButton keepLocalAlways = null;
	private JButton keepRemoteAlways = null;
	private JButton cancel = null;
	private JPanel jContentPane = null;
	private JLabel localLabel = null;
	private JLabel remoteLabel = null;
	private JPanel buttonPanel = null;
	private static int status = 0;
	private JScrollPane jScrollPane = null;
	private static JTextPane remoteEntry = null;
	private JScrollPane jScrollPane1 = null;
	private static JTextPane localEntry = null;

	/**
	 * @param jabRefFrame
	 */
	public CompareDialog(JabRefFrame jabRefFrame) {
		super(jabRefFrame);
		initialize();
	}

	public static int showCompareDialog(JabRefFrame jabRefFrame, BibtexEntry entry,
			Post<? extends Resource> post) {

		CompareDialog cd = new CompareDialog(jabRefFrame);
		Util.placeDialog(cd, jabRefFrame);
			
		//set the source of the local and remote bibtex to the views
		localEntry.setText(generateSource(entry, JabRefModelConverter.convertPost(post)));
		remoteEntry.setText(generateSource(JabRefModelConverter.convertPost(post), entry));

		cd.setModal(true);
		cd.setVisible(true);

		return status;
	}

	private static String generateSource(BibtexEntry src, BibtexEntry comp) {

		if (src != null && comp != null) {

			StringBuffer source = new StringBuffer();

			//append some style to the text, just to let it look like source
			source.append("<span style=\"font: 10pt monospace\">");

			if (!src.getType().getName().equals(comp.getType().getName())
					&& !src.getCiteKey().equals(comp.getCiteKey())) {
				
				//mark entries that differ with yellow
				source.append("<span style=\"background: yellow\">" + "@"
						+ src.getType().getName().toUpperCase() + "{"
						+ src.getCiteKey() + "</span>");
			} else {
				source.append("@" + src.getType().getName().toUpperCase() + "{"
						+ src.getCiteKey());
			}
			
			Set<String> commonFields = src.getAllFields();
			commonFields.addAll(comp.getAllFields());

			for (String field : commonFields) {

				if("owner".equals(field))
					continue;
				
				//fields that should be ignored
				if (!field.startsWith("__") && !field.equals("id")
						&& !field.equals("timestamp")
						&& !field.equals("intrahash")
						&& !field.equals("interhash")
						&& src.getField(field) != null
						&& !src.getField(field).isEmpty()) {
					
					// compare values of src and comp entry
					if (comp.getField(field) != null
							&& !comp.getField(field).isEmpty()
							&& src.getField(field).equals(comp.getField(field))) {
						source.append(",<br>&nbsp;&nbsp;" + field + " = {"
								+ src.getField(field) + "}");
					} else {
						source
								.append(",<br>&nbsp;&nbsp;<span style=\"background: yellow\">"
										+ field
										+ " = {"
										+ src.getField(field)
										+ "}" + "</span>");
					}
				}
			}

			source.append("<br>}</span>");

			return source.toString();
		} else {
			return null;
		}
	}

	/**
	 * This method initializes keepLocal
	 * 
	 * @return javax.swing.JButton
	 */
	private JButton getKeepLocal() {
		if (keepLocal == null) {
			keepLocal = new JButton();
			keepLocal.setText("Keep Local");
			keepLocal.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {

					status = KEEP_LOCAL;
					CompareDialog.this.setVisible(false);
				}
			});
		}
		return keepLocal;
	}

	/**
	 * This method initializes keepRemote
	 * 
	 * @return javax.swing.JButton
	 */
	private JButton getKeepRemote() {
		if (keepRemote == null) {
			keepRemote = new JButton();
			keepRemote.setText("Keep Remote");
			keepRemote.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {

					status = KEEP_REMOTE;
					CompareDialog.this.setVisible(false);
				}
			});
		}
		return keepRemote;
	}

	/**
	 * This method initializes keepLocalAlways
	 * 
	 * @return javax.swing.JButton
	 */
	private JButton getKeepLocalAlways() {
		if (keepLocalAlways == null) {
			keepLocalAlways = new JButton();
			keepLocalAlways.setText("Always Keep Local");
			keepLocalAlways
					.addActionListener(new java.awt.event.ActionListener() {
						public void actionPerformed(java.awt.event.ActionEvent e) {

							status = KEEP_LOCAL_ALWAYS;
							CompareDialog.this.setVisible(false);
						}
					});
		}
		return keepLocalAlways;
	}

	/**
	 * This method initializes keepRemoteAlways
	 * 
	 * @return javax.swing.JButton
	 */
	private JButton getKeepRemoteAlways() {
		if (keepRemoteAlways == null) {
			keepRemoteAlways = new JButton();
			keepRemoteAlways.setText("Always Keep Remote");
			keepRemoteAlways
					.addActionListener(new java.awt.event.ActionListener() {
						public void actionPerformed(java.awt.event.ActionEvent e) {

							status = KEEP_REMOTE_ALWAYS;
							CompareDialog.this.setVisible(false);
						}
					});
		}
		return keepRemoteAlways;
	}

	/**
	 * This method initializes cancel
	 * 
	 * @return javax.swing.JButton
	 */
	private JButton getCancel() {
		if (cancel == null) {
			cancel = new JButton();
			cancel.setText("Cancel");
			cancel.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {

					status = JOptionPane.CANCEL_OPTION;
					CompareDialog.this.setVisible(false);
				}
			});
		}
		return cancel;
	}

	/**
	 * This method initializes jContentPane
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getJContentPane() {
		if (jContentPane == null) {
			GridBagConstraints gridBagConstraints22 = new GridBagConstraints();
			gridBagConstraints22.fill = GridBagConstraints.BOTH;
			gridBagConstraints22.gridy = 1;
			gridBagConstraints22.weightx = 1.0;
			gridBagConstraints22.weighty = 1.0;
			gridBagConstraints22.insets = new Insets(0, 0, 0, 3);
			gridBagConstraints22.gridx = 0;
			GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
			gridBagConstraints1.fill = GridBagConstraints.BOTH;
			gridBagConstraints1.gridy = 1;
			gridBagConstraints1.weightx = 1.0;
			gridBagConstraints1.weighty = 1.0;
			gridBagConstraints1.insets = new Insets(0, 3, 0, 0);
			gridBagConstraints1.gridx = 1;
			final GridBagConstraints gridBagConstraints21 = new GridBagConstraints();
			gridBagConstraints21.gridx = 1;
			gridBagConstraints21.insets = new Insets(0, 0, 3, 0);
			gridBagConstraints21.gridy = 0;
			remoteLabel = new JLabel();
			remoteLabel.setText("Remote");
			final GridBagConstraints gridBagConstraints11 = new GridBagConstraints();
			gridBagConstraints11.gridx = 0;
			gridBagConstraints11.insets = new Insets(0, 0, 3, 0);
			gridBagConstraints11.gridy = 0;
			localLabel = new JLabel();
			localLabel.setText("Local");
			final GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
			gridBagConstraints2.fill = GridBagConstraints.BOTH;
			gridBagConstraints2.gridy = 2;
			gridBagConstraints2.gridwidth = 2;
			gridBagConstraints2.insets = new Insets(10, 10, 10, 10);
			gridBagConstraints2.gridx = 0;
			jContentPane = new JPanel();
			jContentPane.setLayout(new GridBagLayout());
			jContentPane.add(getButtonPanel(), gridBagConstraints2);
			jContentPane.add(localLabel, gridBagConstraints11);
			jContentPane.add(remoteLabel, gridBagConstraints21);
			jContentPane.add(getJScrollPane(), gridBagConstraints1);
			jContentPane.add(getJScrollPane1(), gridBagConstraints22);
		}
		return jContentPane;
	}

	/**
	 * This method initializes buttonPanel
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getButtonPanel() {
		if (buttonPanel == null) {
			final GridBagConstraints gridBagConstraints4 = new GridBagConstraints();
			gridBagConstraints4.insets = new Insets(0, 10, 0, 0);
			final GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
			gridBagConstraints3.insets = new Insets(0, 10, 0, 0);
			buttonPanel = new JPanel();
			buttonPanel.setLayout(new GridBagLayout());
			buttonPanel.add(getKeepLocal(), new GridBagConstraints());
			buttonPanel.add(getKeepRemote(), new GridBagConstraints());
			buttonPanel.add(getKeepLocalAlways(), gridBagConstraints3);
			buttonPanel.add(getKeepRemoteAlways(), new GridBagConstraints());
			buttonPanel.add(getCancel(), gridBagConstraints4);
		}
		return buttonPanel;
	}

	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
		this.setSize(900, 600);
		this.setModal(true);
		this.setTitle("");
		setContentPane(getJContentPane());
	}

	/**
	 * This method initializes jScrollPane
	 * 
	 * @return javax.swing.JScrollPane
	 */
	private JScrollPane getJScrollPane() {
		if (jScrollPane == null) {
			jScrollPane = new JScrollPane();
			jScrollPane.setViewportView(getRemoteEntry());
		}
		return jScrollPane;
	}

	/**
	 * This method initializes remoteEntry
	 * 
	 * @return javax.swing.JTextPane
	 */
	private JTextPane getRemoteEntry() {
		if (remoteEntry == null) {
			remoteEntry = new JTextPane();
			remoteEntry.setBackground(new Color(153, 255, 153));
			remoteEntry
					.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
			remoteEntry.setFont(new Font("DialogInput", Font.PLAIN, 10));
			remoteEntry.setForeground(Color.black);
			remoteEntry.setContentType("text/html");
			remoteEntry.setEditable(false);
		}
		return remoteEntry;
	}

	/**
	 * This method initializes jScrollPane1
	 * 
	 * @return javax.swing.JScrollPane
	 */
	private JScrollPane getJScrollPane1() {
		if (jScrollPane1 == null) {
			jScrollPane1 = new JScrollPane();
			jScrollPane1.setViewportView(getLocalEntry());
		}
		return jScrollPane1;
	}

	/**
	 * This method initializes localEntry
	 * 
	 * @return javax.swing.JTextPane
	 */
	private JTextPane getLocalEntry() {
		if (localEntry == null) {
			localEntry = new JTextPane();
			localEntry.setBackground(new Color(153, 255, 153));
			localEntry
					.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
			localEntry.setFont(new Font("DialogInput", Font.PLAIN, 10));
			localEntry.setForeground(Color.black);
			localEntry.setContentType("text/html");
			localEntry.setBounds(new Rectangle(0, 0, 355, 336));
			localEntry.setEditable(false);
		}
		return localEntry;
	}

} // @jve:decl-index=0:visual-constraint="10,10"
