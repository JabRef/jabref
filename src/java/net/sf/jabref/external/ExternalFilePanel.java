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
package net.sf.jabref.external;

import java.awt.Component;
import java.awt.GridLayout;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.xml.transform.TransformerException;

import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.BibtexFields;
import net.sf.jabref.EntryEditor;
import net.sf.jabref.FieldEditor;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefFrame;
import net.sf.jabref.MetaData;
import net.sf.jabref.OpenFileFilter;
import net.sf.jabref.UrlDragDrop;
import net.sf.jabref.Util;
import net.sf.jabref.gui.FileDialogs;
import net.sf.jabref.net.URLDownload;
import net.sf.jabref.util.XMPUtil;

/**
 * Initial Version:
 * 
 * @author alver
 * @version Date: May 7, 2005 Time: 7:17:42 PM
 * 
 */
public class ExternalFilePanel extends JPanel {

	private static final long serialVersionUID = 3653290879640642718L;

	private JButton browseBut, download, auto, xmp;

	private EntryEditor entryEditor;

    private JabRefFrame frame;

	private OpenFileFilter off;

	private BibtexEntry entry;
	
	private BibtexDatabase database;

	private MetaData metaData;

	public ExternalFilePanel(final String fieldName, final MetaData metaData,
		final BibtexEntry entry, final FieldEditor editor, final OpenFileFilter off) {
		this(null, metaData, null, fieldName, off, editor);
		this.entry = entry;
        this.entryEditor = null;
    }

	public ExternalFilePanel(final JabRefFrame frame, final MetaData metaData,
		final EntryEditor entryEditor, final String fieldName, final OpenFileFilter off,
		final FieldEditor editor) {

		this.frame = frame;
		this.metaData = metaData;
		this.off = off;
		this.entryEditor = entryEditor;

        setLayout(new GridLayout(2, 2));

		browseBut = new JButton(Globals.lang("Browse"));
		download = new JButton(Globals.lang("Download"));
		auto = new JButton(Globals.lang("Auto"));
		xmp = new JButton(Globals.lang("Write XMP"));
		xmp.setToolTipText(Globals.lang("Write BibtexEntry as XMP-metadata to PDF."));

		browseBut.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				browseFile(fieldName, editor);
				// editor.setText(chosenValue);
				entryEditor.storeFieldAction.actionPerformed(new ActionEvent(editor, 0, ""));
			}
		});

		download.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				downLoadFile(fieldName, editor, frame);
			}
		});

		auto.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				autoSetFile(fieldName, editor);
			}
		});
		xmp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				pushXMP(fieldName, editor);
			}
		});

		add(browseBut);
		add(download);
		add(auto);
		add(xmp);

		// Add drag and drop support to the field
		if (editor != null)
			((JComponent) editor).setDropTarget(new DropTarget((Component) editor,
				DnDConstants.ACTION_NONE, new UrlDragDrop(entryEditor, frame, editor)));
	}

	/**
	 * Change which entry this panel is operating on. This is used only when
	 * this panel is not attached to an entry editor.
	 */
	public void setEntry(BibtexEntry entry, BibtexDatabase database) {
		this.entry = entry;
		this.database = database;
	}
	
	public BibtexDatabase getDatabase(){
		return (database != null ? database : entryEditor.getDatabase());
	}

	public BibtexEntry getEntry() {
		return (entry != null ? entry : entryEditor.getEntry());
	}

	protected Object getKey() {
		return getEntry().getField(BibtexFields.KEY_FIELD);
	}

	protected void output(String s) {
		if (frame != null)
			frame.output(s);
	}

	public void pushXMP(final String fieldName, final FieldEditor editor) {


		(new Thread() {
			public void run() {

				output(Globals.lang("Looking for pdf..."));
				
				// Find the default directory for this field type, if any:
				String[] dirs = metaData.getFileDirectory(fieldName);
				File file = null;
				if (dirs.length > 0) {
					File tmp = Util.expandFilename(editor.getText(), dirs);
					if (tmp != null)
						file = tmp;
				}

				if (file == null) {
					file = new File(editor.getText());
				}
				
				final File finalFile = file;
		
				output(Globals.lang("Writing XMP to '%0'...", finalFile.getName()));
				try {
					XMPUtil.writeXMP(finalFile, getEntry(), getDatabase());
					output(Globals.lang("Wrote XMP to '%0'.", finalFile.getName()));
				} catch (IOException e) {
					JOptionPane.showMessageDialog(editor.getParent(), 
            Globals.lang("Error writing XMP to file: %0", e.getLocalizedMessage()), 
					  Globals.lang("Writing XMP"), JOptionPane.ERROR_MESSAGE);
					Globals.logger(Globals.lang("Error writing XMP to file: %0", finalFile
						.getAbsolutePath()));
					output(Globals.lang("Error writing XMP to file: %0", finalFile.getName()));
					
				} catch (TransformerException e) {
					JOptionPane.showMessageDialog(editor.getParent(), 
            Globals.lang("Error converting Bibtex to XMP: %0", e.getLocalizedMessage()), 
            Globals.lang("Writing XMP"), JOptionPane.ERROR_MESSAGE);
					  Globals.logger(Globals.lang("Error while converting BibtexEntry to XMP %0",
						finalFile.getAbsolutePath()));
					output(Globals.lang("Error converting XMP to '%0'...", finalFile.getName()));
				}
			}
		}).start();
	}

	public void browseFile(final String fieldName, final FieldEditor editor) {

		String[] dirs = metaData.getFileDirectory(fieldName);
        String directory = null;
		if (dirs.length > 0)
			directory = dirs[0]; // Default to the first directory in the list

		String dir = editor.getText(), retVal = null;

		if ((directory == null) || !(new File(dir)).isAbsolute()) {
			if (directory != null)
				dir = directory;
			else
				dir = Globals.prefs.get(fieldName + Globals.FILETYPE_PREFS_EXT, "");
		}

		String chosenFile = FileDialogs.getNewFile(frame, new File(dir), "." + fieldName,
			JFileChooser.OPEN_DIALOG, false);

		if (chosenFile != null) {
			File newFile = new File(chosenFile);
			String position = newFile.getParent();

			if ((directory != null) && position.startsWith(directory)) {
				// Construct path relative to pdf base dir
				String relPath = position.substring(directory.length(), position.length())
					+ File.separator + newFile.getName();

				// Remove leading path separator
				if (relPath.startsWith(File.separator)) {
					relPath = relPath.substring(File.separator.length(), relPath.length());

					// Set relative path as field value
				}

				retVal = relPath;
			} else
				retVal = newFile.getPath();

			editor.setText(retVal);
			Globals.prefs.put(fieldName + Globals.FILETYPE_PREFS_EXT, newFile.getPath());
		}
	}

	public void downLoadFile(final String fieldName, final FieldEditor fieldEditor,
		final Component parent) {

		final String res = JOptionPane.showInputDialog(parent, Globals
			.lang("Enter URL to download"));

		if (res == null || res.trim().length() == 0)
			return;

		/*
		 * If this panel belongs in an entry editor, note which entry is
		 * currently shown:
		 */
		final BibtexEntry targetEntry;
		if (entryEditor != null)
			targetEntry = entryEditor.getEntry();
		else
			targetEntry = entry;

		(new Thread() {

			public String getPlannedFileName(String res) {
				String suffix = off.getSuffix(res);
				if (suffix == null)
					suffix = "." + fieldName.toLowerCase();

				String plannedName = null;
				if (getKey() != null)
					plannedName = getKey() + suffix;
				else {
					plannedName = JOptionPane.showInputDialog(parent, Globals
						.lang("BibTeX key not set. Enter a name for the downloaded file"));
					if (plannedName != null && !off.accept(plannedName))
						plannedName += suffix;
				}

				/*
				 * [ 1548875 ] download pdf produces unsupported filename
				 * 
				 * http://sourceforge.net/tracker/index.php?func=detail&aid=1548875&group_id=92314&atid=600306
				 * 
				 */
				if (Globals.ON_WIN) {
					plannedName = plannedName.replaceAll(
						"\\?|\\*|\\<|\\>|\\||\\\"|\\:|\\.$|\\[|\\]", "");
				} else if (Globals.ON_MAC) {
					plannedName = plannedName.replaceAll(":", "");
				}

				return plannedName;
			}

			public void run() {
				String originalText = fieldEditor.getText();
				fieldEditor.setEnabled(false);
				boolean updateEditor = true;

				try {
					fieldEditor.setText(Globals.lang("Downloading..."));
					output(Globals.lang("Downloading..."));
					String plannedName = getPlannedFileName(res);

					// Find the default directory for this field type:
					String[] dirs = metaData.getFileDirectory(fieldName);
                    String directory = null;
                    // Look for the first one in the list that exists:
                    for (int i=0; i<dirs.length; i++) {
                        if (new File(dirs[i]).exists()) {
                            directory = dirs[i];
                            break;
                        }
                    }
					if (directory == null) {
                        if (dirs.length > 0)
                            JOptionPane.showMessageDialog(parent, Globals.lang("Could not find directory for %0-files: %1", fieldName, dirs[0]),
                                Globals.lang("Download file"), JOptionPane.ERROR_MESSAGE);
                        else
                            JOptionPane.showMessageDialog(parent, Globals.lang("No directory defined for %0-files", fieldName),
                                Globals.lang("Download file"), JOptionPane.ERROR_MESSAGE);
						return;
					}
					File file = new File(new File(directory), plannedName);

					URL url = new URL(res);

					URLDownload udl = new URLDownload(parent, url, file);
					try {
						udl.download();
					} catch (IOException e2) {
						JOptionPane.showMessageDialog(parent, Globals.lang("Invalid URL")+": "
							+ e2.getMessage(), Globals.lang("Download file"),
							JOptionPane.ERROR_MESSAGE);
						Globals.logger("Error while downloading " + url.toString());
						return;
					}
					output(Globals.lang("Download completed"));

					String textToSet = file.getPath();
					if (textToSet.startsWith(directory)) {
						// Construct path relative to pdf base dir
						textToSet = textToSet.substring(directory.length(), textToSet.length());

						// Remove leading path separator
						if (textToSet.startsWith(File.separator)) {
							textToSet = textToSet.substring(File.separator.length());
						}
					}

					/*
					 * Check if we should update the editor text field, or
					 * update the target entry directly:
					 */
                    if (entryEditor == null || entryEditor.getEntry() != targetEntry) {
						/*
						 * Editor has probably changed to show a different
						 * entry. So we must update the target entry directly
						 * and not set the text of the editor.
						 */
						targetEntry.setField(fieldName, textToSet);
                        fieldEditor.setText(textToSet);
                        fieldEditor.setEnabled(true);
                        updateEditor = false;
					} else {
						/*
						 * Need to set the fieldEditor first before running
						 * updateField-Action, because otherwise we might get a
						 * race condition.
						 * 
						 * (Hopefully a) Fix for: [ 1545601 ] downloading pdf
						 * corrupts pdf field text
						 * 
						 * http://sourceforge.net/tracker/index.php?func=detail&aid=1545601&group_id=92314&atid=600306
						 */
						fieldEditor.setText(textToSet);
						fieldEditor.setEnabled(true);
						updateEditor = false;
						SwingUtilities.invokeLater(new Thread() {
							public void run() {
								entryEditor.updateField(fieldEditor);
							}
						});
					}

				} catch (MalformedURLException e1) {
					JOptionPane.showMessageDialog(parent, Globals.lang("Invalid URL"), Globals
						.lang("Download file"), JOptionPane.ERROR_MESSAGE);
				} finally {
					// If stuff goes wrong along the road, put back original
					// value
					if (updateEditor) {
						fieldEditor.setText(originalText);
						fieldEditor.setEnabled(true);
					}
                }
			}
		}).start();
	}

	/**
	 * Starts a thread that searches the external file directory for the given
	 * field name, including subdirectories, and looks for files named after the
	 * current entry's bibtex key. Returns a reference to the thread for callers
	 * that may want to wait for the thread to finish (using join()).
	 * 
	 * @param fieldName
	 *            The field to set.
	 * @param editor
	 *            An EntryEditor instance where to set the value found.
	 * @return A reference to the Thread that performs the operation.
	 */
	public Thread autoSetFile(final String fieldName, final FieldEditor editor) {
		Object o = getKey();
		if ((o == null) || (Globals.prefs.get(fieldName + "Directory") == null)) {
			output(Globals.lang("You must set both BibTeX key and %0 directory", fieldName
				.toUpperCase())
				+ ".");
			return null;
		}
		output(Globals.lang("Searching for %0 file", fieldName.toUpperCase()) + " '" + o + "."
			+ fieldName + "'...");
		Thread t = (new Thread() {
			public void run() {
				/*
				 * Find the following directories to look in for:
				 * 
				 * default directory for this field type.
				 * 
				 * directory of bibtex-file. // NOT POSSIBLE at the moment.
				 * 
				 * JabRef-directory.
				 */
				LinkedList<String> list = new LinkedList<String>();
                String[] dirs = metaData.getFileDirectory(fieldName);
                for (int i = 0; i < dirs.length; i++) {
                    list.add(dirs[i]);
                }

				String found = Util.findPdf(getEntry(), fieldName, list
					.toArray(new String[list.size()]));// , off);
                                        
                                
				// To activate findFile:
				// String found = Util.findFile(getEntry(), null, dir,
				// ".*[bibtexkey].*");

				if (found != null) {
					editor.setText(found);
					if (entryEditor != null)
						entryEditor.updateField(editor);
					output(Globals.lang("%0 field set", fieldName.toUpperCase()) + ".");
				} else {
					output(Globals.lang("No %0 found", fieldName.toUpperCase()) + ".");
				}

			}
		});

		t.start();
		return t;

	}

}
