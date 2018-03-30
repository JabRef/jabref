package org.jabref.logic.pdf;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.Objects;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSFloat;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripperByArea;

/**
 * Extracts the text of marked annotations using bounding boxes.
 */
public final class TextExtractor {

    private final COSArray boundingBoxes;
    private final PDPage page;

    /**
     * @param page       the page the annotation is on, must not be null
     * @param boundingBoxes the raw annotation, must not be null
     */
    public TextExtractor(PDPage page, COSArray boundingBoxes) {
        this.page = Objects.requireNonNull(page);
        this.boundingBoxes = Objects.requireNonNull(boundingBoxes);
    }

    /**
     * Extracts the text of a marked annotation such as highlights, underlines, strikeouts etc.
     *
     * @return The text of the annotation
     * @throws IOException If the PDFTextStripperByArea fails to initialize.
     */
    public String extractMarkedText() throws IOException {
        // Text has to be extracted by the rectangle calculated from the marking
        PDFTextStripperByArea stripperByArea = new PDFTextStripperByArea();
        String markedText = "";

        // Iterates over the array of segments. Each segment consists of 8 points forming a bounding box.
        int totalSegments = boundingBoxes.size() / 8;
        for (int currentSegment = 1, segmentPointer = 0; currentSegment <= totalSegments; currentSegment++, segmentPointer += 8) {
            try {
                stripperByArea.addRegion("markedRegion", calculateSegmentBoundingBox(boundingBoxes, segmentPointer));
                stripperByArea.extractRegions(page);

                markedText = markedText.concat(stripperByArea.getTextForRegion("markedRegion"));
            } catch (IllegalArgumentException e) {
                throw new IOException("Cannot read annotation coordinates!", e);
            }
        }

        return markedText.trim();
    }

    private Rectangle2D calculateSegmentBoundingBox(COSArray quadsArray, int segmentPointer) {
        // Extract coordinate values
        float upperLeftX = toFloat(quadsArray.get(segmentPointer));
        float upperLeftY = toFloat(quadsArray.get(segmentPointer + 1));
        float upperRightX = toFloat(quadsArray.get(segmentPointer + 2));
        float upperRightY = toFloat(quadsArray.get(segmentPointer + 3));
        float lowerLeftX = toFloat(quadsArray.get(segmentPointer + 4));
        float lowerLeftY = toFloat(quadsArray.get(segmentPointer + 5));

        // Post-processing of the raw coordinates.
        PDRectangle pageSize = page.getMediaBox();
        float ulx = upperLeftX - 1; // It is magic.
        float uly = pageSize.getHeight() - upperLeftY;
        float width = upperRightX - lowerLeftX;
        float height = upperRightY - lowerLeftY;

        return new Rectangle2D.Float(ulx, uly, width, height);
    }

    private float toFloat(Object cosNumber) {
        if (cosNumber instanceof COSFloat) {
            return ((COSFloat) cosNumber).floatValue();
        }
        if (cosNumber instanceof COSInteger) {
            return ((COSInteger) cosNumber).floatValue();
        }
        throw new IllegalArgumentException("The number type of the annotation is not supported!");
    }
}
