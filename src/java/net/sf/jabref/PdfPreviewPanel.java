/*  Copyright (C) 2012 Meltem Meltem Demirköprü, Ahmad Hammoud, Oliver Kopp

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package net.sf.jabref;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.jabref.gui.FileListEntry;
import net.sf.jabref.gui.FileListTableModel;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;

public class PdfPreviewPanel extends JPanel {

	private PDDocument document = null;
	private JLabel picLabel;
    private final MetaData metaData;
	
	public PdfPreviewPanel(MetaData metaData) {
		this.metaData = metaData;
        picLabel = new JLabel();
		add(picLabel);
	}

	private void renderPDFFile(File file) {
		InputStream input;
		try {
			input = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		
		try {
			document = PDDocument.load(input);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		@SuppressWarnings("unchecked")
		List<PDPage> pages = document.getDocumentCatalog().getAllPages();

		PDPage page = pages.get(0);
		BufferedImage image = null;
		try {
			image = page.convertToImage();
		} catch (Exception e1) {
		    // silently ignores all rendering exceptions
		    image = null;
		}
		
		if (image != null) {
			int width = this.getParent().getWidth();
			int height = this.getParent().getHeight();
			BufferedImage resImage = resizeImage(image, width, height, BufferedImage.TYPE_INT_RGB);
			ImageIcon icon = new ImageIcon(resImage);
			picLabel.setText(null);
			picLabel.setIcon(icon);
		} else {
		    clearPreview();
		}
		
		try {
			document.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private BufferedImage resizeImage(BufferedImage originalImage, int width,
			int height, int type) {
		int h = originalImage.getHeight();
		int w = originalImage.getWidth();
		if ((height == 0) || (width == 0)) {
			height = h;
			width = w;
		} else {
			float factorH = (float) height / (float) h;
			float factorW = (float) width / (float) w;

			if (factorH < factorW) {
				// use factorH, only width has to be changed as height is
				// already correct
				width = Math.round(w * factorH);
			} else {
				width = Math.round(h * factorW);
			}
		}
		
		BufferedImage resizedImage = new BufferedImage(width, height, type);
		Graphics2D g = resizedImage.createGraphics();
		g.drawImage(originalImage, 0, 0, width, height, null);
		return resizedImage;
	}
	
	public void updatePanel (BibtexEntry entry) {	
		if (entry == null) {
			clearPreview();
			return;
		}
        picLabel.setText("rendering preview...");
        picLabel.setIcon(null);
		FileListTableModel tm = new FileListTableModel();
        tm.setContent(entry.getField("file"));
        FileListEntry flEntry = null;
        for (int i=0; i< tm.getRowCount(); i++) {
            flEntry = tm.getEntry(i);
            if (flEntry.getType().getName().toLowerCase().equals("pdf")) {
                break;
            }
        }
		
		if (flEntry != null) {
		    File pdfFile = Util.expandFilename(metaData, flEntry.getLink());
		    if (pdfFile != null) {
		        renderPDFFile(pdfFile);
		    } else {
		        clearPreview();
		    }
		} else {
			clearPreview();
		}
	}

	private void clearPreview() {
	    this.picLabel.setIcon(null);
	    this.picLabel.setText(Globals.lang("no preview available"));
	}

}
