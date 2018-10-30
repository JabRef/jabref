package org.jabref.gui.mergeentries;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javafx.scene.text.Text;

import difflib.Delta;
import difflib.DiffUtils;

public class DiffHighlighting {

    private DiffHighlighting() {
    }

    public static List<Text> generateDiffHighlighting(String baseString, String modifiedString, String separator) {
        List<String> stringList = Arrays.asList(baseString.split(separator));
        List<Text> result = stringList.stream().map(DiffHighlighting::forUnchanged).collect(Collectors.toList());
        List<Delta<String>> deltaList = DiffUtils.diff(stringList, Arrays.asList(modifiedString.split(separator))).getDeltas();
            Collections.reverse(deltaList);
            for (Delta<String> delta : deltaList) {
                int startPos = delta.getOriginal().getPosition();
                List<String> lines = delta.getOriginal().getLines();
                int offset = 0;
                switch (delta.getType()) {
                case CHANGE:
                    for (String line : lines) {
                        result.set(startPos + offset, forRemoved(line + separator));
                        offset++;
                    }
                    result.set(startPos + offset - 1, forRemoved(stringList.get((startPos + offset) - 1) + separator));
                    result.add(startPos + offset, forAdded(String.join(separator, delta.getRevised().getLines())));
                    break;
                case DELETE:
                    for (String line : lines) {
                        result.set(startPos + offset, forRemoved(line + separator));
                        offset++;
                    }
                    break;
                case INSERT:
                    result.add(delta.getOriginal().getPosition(), forAdded(String.join(separator, delta.getRevised().getLines())));
                    break;
                default:
                    break;
                }
            }
        return result;
    }

    public static Text forChanged(String text) {
        Text node = new Text(text);
        node.getStyleClass().add("text-changed");
        return node;
    }

    public static Text forUnchanged(String text) {
        Text node = new Text(text);
        node.getStyleClass().add("text-unchanged");
        return node;
    }

    public static Text forAdded(String text) {
        Text node = new Text(text);
        node.getStyleClass().add("text-added");
        return node;
    }

    public static Text forRemoved(String text) {
        Text node = new Text(text);
        node.getStyleClass().add("text-removed");
        return node;
    }

    public static List<Text> generateSymmetricHighlighting(String baseString, String modifiedString, String separator) {
        List<String> stringList = Arrays.asList(baseString.split(separator));
        List<Text> result = stringList.stream().map(text -> DiffHighlighting.forUnchanged(text + separator)).collect(Collectors.toList());
        List<Delta<String>> deltaList = DiffUtils.diff(stringList, Arrays.asList(modifiedString.split(separator))).getDeltas();
            Collections.reverse(deltaList);
            for (Delta<String> delta : deltaList) {
                int startPos = delta.getOriginal().getPosition();
                List<String> lines = delta.getOriginal().getLines();
                int offset = 0;
                switch (delta.getType()) {
                case CHANGE:
                    for (String line : lines) {
                        result.set(startPos + offset, forChanged(line + separator));
                        offset++;
                    }
                    break;
                case DELETE:
                    for (String line : lines) {
                        result.set(startPos + offset, forAdded(line + separator));
                        offset++;
                    }
                    break;
                case INSERT:
                    break;
                default:
                    break;
                }
            }

        return result;
    }

}
