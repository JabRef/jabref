package org.jabref.logic.pdf;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jabref.logic.util.strings.StringUtil;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.TextPosition;
import org.jspecify.annotations.NullMarked;

/// Recognizes the first page of a paper by its title: text near the top of the page, set clearly larger than the body text.
/// A running header is too short to count as a title.
@NullMarked
final class PaperTitlePages {

    private static final float TOP_OF_PAGE = 0.35f;
    private static final float TITLE_TO_BODY_SIZE = 1.35f;
    private static final int MIN_TITLE_LENGTH = 15;

    private final PDDocument document;
    private final float minTitleSize;

    private PaperTitlePages(PDDocument document, float bodySize) {
        this.document = document;
        this.minTitleSize = TITLE_TO_BODY_SIZE * bodySize;
    }

    static PaperTitlePages of(PDDocument document) throws IOException {
        FontSizeCounter counter = new FontSizeCounter();
        counter.getText(document);
        return new PaperTitlePages(document, counter.mostCommonSize());
    }

    /// @param page 1-based page number
    boolean startsPaper(int page) throws IOException {
        TitleMeasurer measurer = new TitleMeasurer(minTitleSize);
        measurer.setStartPage(page);
        measurer.setEndPage(page);
        measurer.getText(document);
        return measurer.titleLength() >= MIN_TITLE_LENGTH;
    }

    private static class FontSizeCounter extends InterruptiblePDFTextStripper {

        private final Map<Float, Integer> charactersPerSize = new HashMap<>();

        @Override
        protected void writeString(String text, List<TextPosition> textPositions) {
            for (TextPosition position : textPositions) {
                if (StringUtil.isNotBlank(position.getUnicode())) {
                    // Half points, so that sizes differing only by rounding count as one
                    charactersPerSize.merge(Math.round(position.getFontSizeInPt() * 2) / 2f, 1, Integer::sum);
                }
            }
        }

        /// A document without text has no body size, so no page of it starts a paper
        float mostCommonSize() {
            return charactersPerSize.entrySet().stream()
                                    .max(Map.Entry.comparingByValue())
                                    .map(Map.Entry::getKey)
                                    .orElse(Float.MAX_VALUE);
        }
    }

    private static class TitleMeasurer extends InterruptiblePDFTextStripper {

        private final float minTitleSize;
        private int titleLength;

        TitleMeasurer(float minTitleSize) {
            this.minTitleSize = minTitleSize;
        }

        @Override
        protected void writeString(String text, List<TextPosition> textPositions) {
            for (TextPosition position : textPositions) {
                if (StringUtil.isNotBlank(position.getUnicode())
                        && (position.getYDirAdj() <= TOP_OF_PAGE * position.getPageHeight())
                        && (position.getFontSizeInPt() >= minTitleSize)) {
                    titleLength++;
                }
            }
        }

        int titleLength() {
            return titleLength;
        }
    }
}
