package org.jabref.model.util;

import java.io.IOException;
import java.nio.file.Path;

public interface DirectoryUpdateListener {

    void directoryCreated(Path newPath) throws IOException;

    void directoryDeleted();

    void fileUpdated();
}
