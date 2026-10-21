package org.jabref.logic.pdf.reader;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

import org.jspecify.annotations.NullMarked;

@NullMarked
public abstract class AbstractExternalPdfReader implements ExternalPdfReader {

    @Override
    public boolean matches(Path executable) {
        Path fileName = executable.getFileName();
        if (fileName == null) {
            return false;
        }
        String name = fileName.toString().toLowerCase(Locale.ROOT);
        return getExecutableNames().stream()
                                   .anyMatch(candidate -> name.equals(candidate) || name.equals(candidate + ".exe"));
    }

    protected abstract Set<String> getExecutableNames();
}
