package org.jabref.logic.pdf.reader;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntFunction;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record ExternalPdfReader(String displayName, Set<String> executableNames, IntFunction<List<String>> pageArguments) {

    public ExternalPdfReader {
        executableNames = Set.copyOf(executableNames);
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean matches(Path executable) {
        return Optional.ofNullable(executable.getFileName())
                       .map(fileName -> fileName.toString().toLowerCase(Locale.ROOT))
                       .map(fileName -> executableNames.stream()
                                                       .anyMatch(candidate -> fileName.equals(candidate) || fileName.equals(candidate + ".exe")))
                       .orElse(false);
    }

    /// `page` is 1-based, as in [org.jabref.model.pdf.FileAnnotation#getPage].
    public List<String> getCommandLine(Path executable, Path pdfFile, int page) {
        List<String> command = new ArrayList<>();
        command.add(executable.toString());
        command.addAll(pageArguments.apply(page));
        command.add(pdfFile.toString());
        return List.copyOf(command);
    }
}
