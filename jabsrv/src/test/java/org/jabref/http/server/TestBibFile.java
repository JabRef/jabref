package org.jabref.http.server;

import java.nio.file.Path;

import org.jabref.logic.util.io.BackupFileUtil;

/// Holds information about test .bib files
///
/// We cannot use a string constant as the path changes from OS to OS. Therefore, we need to dynamically create the expected result.
public enum TestBibFile {
    GENERAL_SERVER_TEST("src/test/resources/org/jabref/http/server/general-server-test.bib"),
    CHOCOLATE_BIB("src/main/resources/chocolate.bib");

    public final Path path;
    public final String id;

    TestBibFile(String locationInSource) {
        this.path = Path.of(locationInSource).toAbsolutePath();
        this.id = path.getFileName() + "-" + BackupFileUtil.getUniqueFilePrefix(path);
    }
}
