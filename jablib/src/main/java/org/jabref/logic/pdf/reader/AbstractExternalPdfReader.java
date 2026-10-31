package org.jabref.logic.pdf.reader;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.jspecify.annotations.NullMarked;

@NullMarked
public abstract class AbstractExternalPdfReader implements ExternalPdfReader {

    @Override
    public boolean matches(Path executable) {
        return Optional.ofNullable(executable.getFileName())
                       .map(fileName -> fileName.toString().toLowerCase(Locale.ROOT))
                       .map(this::isExecutableName)
                       .orElse(false);
    }

    private boolean isExecutableName(String fileName) {
        return getExecutableNames().stream()
                                   .anyMatch(candidate -> fileName.equals(candidate) || fileName.equals(candidate + ".exe"));
    }

    protected abstract Set<String> getExecutableNames();
}
