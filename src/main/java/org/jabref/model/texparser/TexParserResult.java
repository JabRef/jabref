package org.jabref.model.texparser;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class TexParserResult {

    private final List<Path> fileList;
    private final List<Path> nestedFiles;
    private final Map<String, List<Citation>> citations;

    public TexParserResult() {
        this.fileList = new ArrayList<>();
        this.nestedFiles = new ArrayList<>();
        this.citations = new HashMap<>();
    }

    public List<Path> getFileList() {
        return fileList;
    }

    public List<Path> getNestedFiles() {
        return nestedFiles;
    }

    public Map<String, List<Citation>> getCitations() {
        return citations;
    }

    @Override
    public String toString() {
        return String.format("%nTexParserResult{fileList=%s // nestedFiles=%s // citations=%s}%n",
                fileList, nestedFiles, citations);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TexParserResult result = (TexParserResult) o;

        return fileList.equals(result.fileList)
                && nestedFiles.equals(result.nestedFiles)
                && citations.equals(result.citations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileList, nestedFiles, citations);
    }
}
