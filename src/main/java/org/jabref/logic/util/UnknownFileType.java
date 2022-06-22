package org.jabref.logic.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class UnknownFileType implements FileType {

    private final List<String> extensions;

    public UnknownFileType(String... extensions) {
        for (int i = 0; i < extensions.length; i++) {
            if (extensions[i].contains(".")) {
                extensions[i] = extensions[i].substring(extensions[i].indexOf('.') + 1);
            }
            extensions[i] = extensions[i].toLowerCase(Locale.ROOT);
        }
        this.extensions = Arrays.asList(extensions);
    }

    @Override
    public List<String> getExtensions() {
        return extensions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FileType)) {
            return false;
        }
        FileType other = (FileType) o;
        Collections.sort(extensions);
        Collections.sort(other.getExtensions());
        return extensions.equals(other.getExtensions());
    }

    @Override
    public int hashCode() {
        return Objects.hash(extensions);
    }

    @Override
    public String getName() {
        return "Unknown File Type" + extensions.toString();
    }
}
