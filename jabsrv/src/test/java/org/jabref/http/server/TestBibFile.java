package org.jabref.http.server;

import java.nio.file.Path;

import org.jabref.logic.util.io.BackupFileUtil;

public enum TestBibFile {
    GENERAL_SERVER_TEST("src/test/resources/org/jabref/http/server/general-server-test.bib"),
    JABREF_AUTHORS("src/test/resources/testbib/jabref-authors.bib");

    public final Path path;
    public final String id;

    TestBibFile(String locationInSource) {
        this.path = Path.of(locationInSource).toAbsolutePath();
        this.id = path.getFileName() + "-" + BackupFileUtil.getUniqueFilePrefix(path);
    }
}
