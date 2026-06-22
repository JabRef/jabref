package org.jabref.logic.ocr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class OcrPreferences {
    private final StringProperty ocrPath;

    private OcrPreferences() {
        this("ocrmypdf");
    }

    public OcrPreferences(String ocrPath) {
        this.ocrPath = new SimpleStringProperty(ocrPath);
    }

    public String getOcrPath() {
        return ocrPath.get();
    }

    public StringProperty ocrPathProperty() {
        return ocrPath;
    }

    public void setOcrPath(String ocrPath) {
        this.ocrPath.set(ocrPath);
    }

    public static OcrPreferences getDefault() {
        return new OcrPreferences();
    }

    public void setAll(OcrPreferences preferences) {
        this.ocrPath.set(preferences.getOcrPath());
    }

    /// Gets the path of the engine
    ///
    /// @return the path of the engine as a list of strings to be passed to the process builder.
    public ArrayList<String> splitPath(String path) {
        ArrayList<String> result = new ArrayList<>();
        // first check if the path contains a slash or backslash, if so it gets the first space after the last slash
        // then it splits the rest of the string by spaces, otherwise it just splits the whole string by spaces
        if (path.contains("/") || path.contains("\\")) {
            int lastSlash = Math.max(path.lastIndexOf("/"), path.lastIndexOf("\\"));
            lastSlash = path.indexOf(" ", lastSlash);
            if (lastSlash == -1) {
                return new ArrayList<>(List.of(path));
            }
            result.add(path.substring(0, lastSlash));
            for (int i = lastSlash + 1; i < path.length(); i++) {
                int spaceIndex = path.indexOf(' ', i);
                if (spaceIndex == -1) {
                    result.add(path.substring(i));
                    break;
                } else {
                    result.add(path.substring(i, spaceIndex));
                    i = spaceIndex;
                }
            }
        } else {
            return new ArrayList<>(Arrays.asList(path.split(" ")));
        }
        return result;
    }
}
