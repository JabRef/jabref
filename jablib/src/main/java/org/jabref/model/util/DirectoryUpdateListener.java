package org.jabref.model.util;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import org.jabref.model.groups.DirectoryGroup;

public interface DirectoryUpdateListener {

    Optional<DirectoryGroup> directoryCreated(Path newPath) throws IOException;

    void directoryDeleted();

    void pdfDeleted();
}
