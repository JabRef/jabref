package org.jabref.logic.util.strings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import difflib.Delta;
import difflib.DiffUtils;

public class DiffHighlighting {

    public static final String HTML_START = "<html><body>";
    public static final String HTML_END = "</body></html>";
    private static final String ADDITION_START = "<span class=add>";
    private static final String REMOVAL_START = "<span class=del>";
    private static final String CHANGE_START = "<span class=change>";

    private static final String TAG_END = "</span>";

    private DiffHighlighting() {
    }

    public static String generateDiffHighlighting(String baseString, String modifiedString, String separator) {
        Objects.requireNonNull(separator);
        if ((baseString != null) && (modifiedString != null)) {
            List<String> stringList = new ArrayList<>(Arrays.asList(baseString.split(separator)));
            List<Delta<String>> deltaList = new ArrayList<>(
                    DiffUtils.diff(stringList, Arrays.asList(modifiedString.split(separator))).getDeltas());
            Collections.reverse(deltaList);
            for (Delta<String> delta : deltaList) {
                int startPos = delta.getOriginal().getPosition();
                List<String> lines = delta.getOriginal().getLines();
                int offset = 0;
                switch (delta.getType()) {
                case CHANGE:
                    for (String line : lines) {
                        stringList.set(startPos + offset, (offset == 0 ? DiffHighlighting.REMOVAL_START : "") + line);
                        offset++;
                    }
                    stringList.set((startPos + offset) - 1,
                            stringList.get((startPos + offset) - 1) + DiffHighlighting.TAG_END + separator + DiffHighlighting.ADDITION_START
                                    + String.join(separator, delta.getRevised().getLines()) + DiffHighlighting.TAG_END);
                    break;
                case DELETE:
                    for (String line : lines) {
                        stringList.set(startPos + offset, (offset == 0 ? DiffHighlighting.REMOVAL_START : "") + line);
                        offset++;
                    }
                    stringList.set((startPos + offset) - 1,
                            stringList.get((startPos + offset) - 1) + DiffHighlighting.TAG_END);
                    break;
                case INSERT:
                    stringList.add(delta.getOriginal().getPosition(),
                            DiffHighlighting.ADDITION_START + String.join(separator, delta.getRevised().getLines()) + DiffHighlighting.TAG_END);
                    break;
                default:
                    break;
                }
            }
            return String.join(separator, stringList);
        }
        return modifiedString;
    }

    public static String generateSymmetricHighlighting(String baseString, String modifiedString, String separator) {
        if ((baseString != null) && (modifiedString != null)) {
            List<String> stringList = new ArrayList<>(Arrays.asList(baseString.split(separator)));
            List<Delta<String>> deltaList = new ArrayList<>(DiffUtils
                    .diff(stringList, new ArrayList<>(Arrays.asList(modifiedString.split(separator)))).getDeltas());
            Collections.reverse(deltaList);
            for (Delta<String> delta : deltaList) {
                int startPos = delta.getOriginal().getPosition();
                List<String> lines = delta.getOriginal().getLines();
                int offset = 0;
                switch (delta.getType()) {
                case CHANGE:
                    for (String line : lines) {
                        stringList.set(startPos + offset, (offset == 0 ? DiffHighlighting.CHANGE_START : "") + line);
                        offset++;
                    }
                    stringList.set((startPos + offset) - 1, stringList.get((startPos + offset) - 1) + DiffHighlighting.TAG_END);
                    break;
                case DELETE:
                    for (String line : lines) {
                        stringList.set(startPos + offset, (offset == 0 ? DiffHighlighting.ADDITION_START : "") + line);
                        offset++;
                    }
                    stringList.set((startPos + offset) - 1, stringList.get((startPos + offset) - 1) + DiffHighlighting.TAG_END);
                    break;
                case INSERT:
                    break;
                default:
                    break;
                }
            }
            return String.join(separator, stringList);
        }
        return modifiedString;
    }

}
