package org.jabref.gui.documentviewer;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;

import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

import org.jabref.architecture.AllowedToUseAwt;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

/**
 * Represents the view model of a pdf page backed by a {@link PDPage}.
 */
@AllowedToUseAwt("Requires AWT due to PDFBox")
public class PdfDocumentPageViewModel extends DocumentPageViewModel {

    private final PDPage page;
    private final int pageNumber;
    private final PDDocument document;

    public PdfDocumentPageViewModel(PDPage page, int pageNumber, PDDocument document) {
        this.page = Objects.requireNonNull(page);
        this.pageNumber = pageNumber;
        this.document = document;
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
    // Taken from https://stackoverflow.com/questions/23326562/apache-pdfbox-convert-pdf-to-images
    public Image render(int width, int height) {
        PDFRenderer renderer = new PDFRenderer(document);
        try {
            int resolution = 96;
            BufferedImage image = renderer.renderImageWithDPI(pageNumber, 2 * resolution, ImageType.RGB);
            return convertToFxImage(resize(image, width, height));
        } catch (IOException e) {
            // TODO: LOG
            return null;
        }
    }

    @Override
    public int getPageNumber() {
        return pageNumber + 1;
    }

    @Override
    public double getAspectRatio() {
        PDRectangle mediaBox = page.getMediaBox();
        return mediaBox.getWidth() / mediaBox.getHeight();
    }

    // See https://stackoverflow.com/a/57552025/3450689
    private static Image convertToFxImage(BufferedImage image) {
        WritableImage wr = null;
        if (image != null) {
            wr = new WritableImage(image.getWidth(), image.getHeight());
            PixelWriter pw = wr.getPixelWriter();
            for (int x = 0; x < image.getWidth(); x++) {
                for (int y = 0; y < image.getHeight(); y++) {
                    pw.setArgb(x, y, image.getRGB(x, y));
                }
            }
        }
        return wr;
    }
}
