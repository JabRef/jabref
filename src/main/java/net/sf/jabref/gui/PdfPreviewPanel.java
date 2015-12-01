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
package net.sf.jabref.gui;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.MetaData;

import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.io.FileUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;

class PdfPreviewPanel extends JPanel {

    private final JLabel picLabel;
    private final MetaData metaData;

    private static final Log LOGGER = LogFactory.getLog(PdfPreviewPanel.class);

    public PdfPreviewPanel(MetaData metaData) {
        this.metaData = metaData;
        picLabel = new JLabel();
        add(picLabel);
    }

    private void renderPDFFile(File file) {

        try (InputStream input = new FileInputStream(file);
                PDDocument document = PDDocument.load(input)) {
            List<PDPage> pages = document.getDocumentCatalog().getAllPages();

            PDPage page = pages.get(0);
            BufferedImage image;
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

        } catch (IOException e) {
            LOGGER.warn("Cannot open file/PDF document", e);
        }
    }

    private BufferedImage resizeImage(BufferedImage originalImage, int width, int height, int type) {
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

    public void updatePanel(BibtexEntry entry) {
        if (entry == null) {
            clearPreview();
            return;
        }
        picLabel.setText("rendering preview...");
        picLabel.setIcon(null);
        FileListTableModel tm = new FileListTableModel();
        tm.setContent(entry.getField("file"));
        FileListEntry flEntry = null;
        for (int i = 0; i < tm.getRowCount(); i++) {
            flEntry = tm.getEntry(i);
            if ("pdf".equals(flEntry.getType().getName().toLowerCase())) {
                break;
            }
        }

        if (flEntry != null) {
            File pdfFile = FileUtil.expandFilename(metaData, flEntry.getLink());
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
        this.picLabel.setText(Localization.lang("No preview available."));
    }

}
