package org.jabref.logic.pdf;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.jabref.logic.util.strings.StringUtil;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.TextPosition;
import org.jspecify.annotations.NullMarked;

@NullMarked
final class PaperTitlePages {

    private static final float TOP_OF_PAGE = 0.35F;
    private static final float TITLE_TO_BODY_SIZE = 1.35F;
    private static final int MIN_TITLE_LENGTH = 15;

    private static final Pattern EMAIL_ADDRESS = Pattern.compile("\\S@[\\w-]+(\\.[\\w-]+)+");

    private final PDDocument document;
    private final float minTitleSize;
    private final Set<Integer> pagesWithEmailAddresses;

    private PaperTitlePages(PDDocument document, float bodySize, Set<Integer> pagesWithEmailAddresses) {
        this.document = document;
        this.minTitleSize = TITLE_TO_BODY_SIZE * bodySize;
        this.pagesWithEmailAddresses = pagesWithEmailAddresses;
    }

    /// @param samplePages 1-based page numbers
    static PaperTitlePages of(PDDocument document, List<Integer> samplePages) throws IOException {
        FontSizeCounter counter = new FontSizeCounter();
        for (int page : samplePages) {
            counter.setStartPage(page);
            counter.setEndPage(page);
            counter.getText(document);
        }
        return new PaperTitlePages(document, counter.mostCommonSize(), counter.pagesWithEmailAddresses);
    }

    /// @param page 1-based page number, one of the sample pages
    boolean givesAuthorAddresses(int page) {
        return pagesWithEmailAddresses.contains(page);
    }

    /// @param page 1-based page number
    boolean startsPaper(int page) throws IOException {
        return title(page).length() >= MIN_TITLE_LENGTH;
    }

    /// @param page 1-based page number
    String title(int page) throws IOException {
        TitleMeasurer measurer = new TitleMeasurer(minTitleSize);
        measurer.setStartPage(page);
        measurer.setEndPage(page);
        measurer.getText(document);
        return measurer.title();
    }

    private static class FontSizeCounter extends InterruptiblePDFTextStripper {

        private final Map<Float, Integer> charactersPerSize = new HashMap<>();
        private final Set<Integer> pagesWithEmailAddresses = new HashSet<>();

        @Override
        protected void writeString(String text, List<TextPosition> textPositions) {
            if (EMAIL_ADDRESS.matcher(text).find()) {
                pagesWithEmailAddresses.add(getCurrentPageNo());
            }
            for (TextPosition position : textPositions) {
                if (StringUtil.isNotBlank(position.getUnicode())) {
                    charactersPerSize.merge(Math.round(position.getFontSizeInPt() * 2) / 2f, 1, Integer::sum);
                }
            }
        }

        float mostCommonSize() {
            return charactersPerSize.entrySet().stream()
                                    .max(Map.Entry.comparingByValue())
                                    .map(Map.Entry::getKey)
                                    .orElse(Float.MAX_VALUE);
        }
    }

    private static class TitleMeasurer extends InterruptiblePDFTextStripper {

        private final float minTitleSize;
        private final StringBuilder title = new StringBuilder();

        TitleMeasurer(float minTitleSize) {
            this.minTitleSize = minTitleSize;
        }

        @Override
        protected void writeString(String text, List<TextPosition> textPositions) {
            for (TextPosition position : textPositions) {
                if (StringUtil.isNotBlank(position.getUnicode())
                        && (position.getYDirAdj() <= TOP_OF_PAGE * position.getPageHeight())
                        && (position.getFontSizeInPt() >= minTitleSize)) {
                    title.append(position.getUnicode());
                }
            }
        }

        String title() {
            return title.toString();
        }
    }
}
