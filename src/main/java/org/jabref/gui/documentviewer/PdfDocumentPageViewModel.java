package org.jabref.gui.documentviewer;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import org.apache.pdfbox.pdmodel.PDPage;

/**
 * Represents the view model of a pdf page backed by a {@link PDPage}.
 */
public class PdfDocumentPageViewModel extends DocumentPageViewModel {

    private final PDPage page;

    public PdfDocumentPageViewModel(PDPage page) {
        this.page = Objects.requireNonNull(page);
    }

    // Taken from http://stackoverflow.com/a/9417836/873661
    private static BufferedImage resize(BufferedImage img, int newWidth, int newHeight) {
        java.awt.Image tmp = img.getScaledInstance(newWidth, newHeight, java.awt.Image.SCALE_SMOOTH);
        BufferedImage dimg = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = dimg.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();

        return dimg;
    }

    @Override
    public Image render() {
        try {
            int resolution = 96;
            BufferedImage image = page.convertToImage(BufferedImage.TYPE_INT_RGB, 2 * resolution);
            return SwingFXUtils.toFXImage(resize(image, 600, 800), null);
        } catch (IOException e) {
            // TODO: LOG
            return null;
        }
    }
}
