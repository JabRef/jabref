package org.jabref.logic.git.diff;

import java.util.List;

public record LineDiffFiles(String fileName, List<DiffLine> lines) implements DiffFiles {
}
