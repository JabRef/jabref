package org.jabref.logic.biblog;

import java.nio.file.Path;
import java.util.Optional;

import org.jabref.model.metadata.MetaData;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BibLogPathResolverTest {
    /**
     * Returns the user-defined .blg path if it's set.
     */
    @Test
    void returnsUserDefinedBlgPathIfPresent() {
        MetaData metaData = new MetaData();
        Path userBlgPath = Path.of("/custom/path/output.blg");
        metaData.setBlgFilePath(userBlgPath);

        Optional<Path> result = BibLogPathResolver.resolve(metaData, Optional.of(Path.of("/library.bib")));

        assertEquals(Optional.of(userBlgPath), result);
    }

    /**
     * Falls back to default .blg path (same name as .bib) if no user-defined path is set.
     */
    @Test
    void returnsDefaultBlgPathWhenUserPathIsAbsent() {
        MetaData metaData = new MetaData(); // no blg path set

        Path bibPath = Path.of("/home/user/MyLibrary.bib");
        Optional<Path> result = BibLogPathResolver.resolve(metaData, Optional.of(bibPath));

        assertEquals(Optional.of(Path.of("/home/user/MyLibrary.blg")), result);
    }

    /**
     * Returns empty if neither user-defined path nor .bib path is available.
     */
    @Test
    void returnsEmptyWhenNoUserPathAndNoBibPath() {
        MetaData metaData = new MetaData();
        Optional<Path> result = BibLogPathResolver.resolve(metaData, Optional.empty());

        assertEquals(Optional.empty(), result);
    }
}
