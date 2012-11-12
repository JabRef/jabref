package net.sf.jabref.sql;

/*  Copyright (C) 2003-2011 JabRef contributors.
 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License along
 with this program; if not, write to the Free Software Foundation, Inc.,
 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import org.w3c.dom.views.AbstractView;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefFrame;

import com.jgoodies.forms.builder.ButtonBarBuilder;

/**
 * 
 * @author ifsteinm
 */

public class DBImportExportDialog implements MouseListener, KeyListener {

	private JDialog diag;
	private JTable table;
	private String dialogTitle;
	private String dialogTopMessage;
	private int tableSelectionModel;

	// IMPORT
	public List<String> listOfDBs = new ArrayList<String>();
	public boolean moreThanOne = false;
	// EXPORT
	public String selectedDB = "";
	public boolean hasDBSelected = false;
	public boolean removeAction = false;
	public int selectedInt = -1;
	private DialogType dialogType;

	public DialogType getDialogType() {
		return dialogType;
	}

	public void setDialogType(DialogType dialogType) {
		this.dialogType = dialogType;
	}

	public enum DialogType {
		IMPORTER("IMPORTER"), EXPORTER("EXPORTER");
		private String dialogType;

		private DialogType(String dialogType) {
			this.dialogType = dialogType;
		}

		public String getDialogType() {
			return this.dialogType;
		}
	}

	private JButton importButton = new JButton(Globals.lang("Import"));
	private JButton exportButton = new JButton(Globals.lang("Export"));
	private JButton cancelButton = new JButton(Globals.lang("Cancel"));
	private JButton removeButton = new JButton(Globals.lang("Remove Selected"));

	public DBImportExportDialog(JabRefFrame frame, Vector<Vector<String>> rows, DialogType dialogType) {
		this.dialogType = dialogType;

		Vector<String> columns = new Vector<String>();
		columns.add("Databases");
		table = new JTable();
		DefaultTableModel model = new DefaultTableModel(rows,columns){
			private static final long serialVersionUID = 1L;
			public boolean isCellEditable(int row, int column){
				return false;
			}
		};
		
		table.setModel (model);

		
		if (dialogType.equals(DialogType.EXPORTER)){
			dialogTitle = Globals.lang("SQL Database Exporter");
			dialogTopMessage = Globals.lang("Select target SQL database:");
			tableSelectionModel = ListSelectionModel.SINGLE_SELECTION;
			table.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put((KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0)), "exportAction");
			table.getActionMap().put("exportAction",new AbstractAction() {
			    public void actionPerformed(ActionEvent e) {
			        exportAction();
			    }
			});
		}
		else{
			this.dialogType = dialogType;
			dialogTitle = Globals.lang("SQL Database Importer");
			dialogTopMessage = Globals.lang("Please select which JabRef databases do you want to import:");
			tableSelectionModel = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION;
			table.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put((KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0)), "importAction");
			table.getActionMap().put("importAction",new AbstractAction() {
			    public void actionPerformed(ActionEvent e) {
			        importAction();
			    }
			});
		}		
		
		diag = new JDialog(frame, dialogTitle, false);
		JPanel pan = new JPanel();
		pan.setLayout(new BorderLayout());

		
		JLabel lab = new JLabel(dialogTopMessage);
		lab.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		pan.add(lab, BorderLayout.NORTH);

		table.setSelectionMode(tableSelectionModel);
		table.setPreferredScrollableViewportSize(new Dimension(100, 100));
		table.setTableHeader(null);
		table.setRowSelectionInterval(0, 0);
		
		pan.add(new JScrollPane(table), BorderLayout.CENTER);
		diag.getContentPane().add(pan, BorderLayout.NORTH);
		pan = new JPanel();
		pan.setLayout(new BorderLayout());

		diag.getContentPane().add(pan, BorderLayout.CENTER);

		ButtonBarBuilder b = new ButtonBarBuilder();
		b.addGlue();
		if (dialogType.equals(DialogType.IMPORTER))
			b.addButton(importButton);
		else
			b.addButton(exportButton);

		b.addRelatedGap();
		b.addButton(cancelButton);
		b.addRelatedGap();
		b.addButton(removeButton);

		b.addGlue();
		b.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		diag.getContentPane().add(b.getPanel(), BorderLayout.SOUTH);
		diag.pack();
		diag.setLocationRelativeTo(frame);
		table.addMouseListener(this);

		importButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				importAction();
			}
		});

		exportButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				exportAction();
			}
		});

		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				moreThanOne = false;
				hasDBSelected = false;
				diag.dispose();
			}
		});
		removeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				moreThanOne = false;
				hasDBSelected = true;
				selectedInt = table.getSelectedRow();
				selectedDB = (String) table.getValueAt(selectedInt, 0); 
				int areYouSure = JOptionPane.showConfirmDialog(diag,"Are you sure you want to remove the already\nexistent SQL DBs?");
				if (areYouSure == JOptionPane.YES_OPTION)
				{
					removeAction = true;
					diag.dispose();
				}
			}
		});
		diag.setModal(true);
		diag.setVisible(true);
	}

	public JDialog getDiag() {
		return this.diag;
	}

	private void exportAction() {
		selectedInt = table.getSelectedRow();
		selectedDB = (String) table.getValueAt(selectedInt, 0);
		hasDBSelected = true;
		diag.dispose();
	}

	private void importAction() {
		int[] selectedInt = table.getSelectedRows();
		for (int i=0; i < selectedInt.length;i++) {
			listOfDBs.add((String)table.getValueAt(selectedInt[i], 0));
			moreThanOne = true;
		}
		diag.dispose();
	}
	

	@Override
	public void mouseClicked(MouseEvent e) {
		if ((e.getClickCount() == 2)
				&& this.dialogType.equals(DialogType.EXPORTER)) {
			this.exportAction();
		}
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mousePressed(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void keyPressed(KeyEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void keyReleased(KeyEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void keyTyped(KeyEvent arg0) {
		// TODO Auto-generated method stub

	}
}
