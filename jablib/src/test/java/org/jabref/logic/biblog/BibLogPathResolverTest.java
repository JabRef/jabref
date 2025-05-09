package org.jabref.logic.biblog;

import java.nio.file.Path;
import java.util.Optional;

import org.jabref.model.metadata.MetaData;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BibLogPathResolverTest {
    private static final String TEST_USER = "testUser";

    @Test
    void returnsUserDefinedBlgPathIfPresent() {
        MetaData metaData = new MetaData();

        Path userBlgPath = Path.of("/custom/path/output.blg");
        metaData.setBlgFilePath(TEST_USER, userBlgPath);

        Optional<Path> result = BibLogPathResolver.resolve(metaData, Optional.of(Path.of("/library.bib")), TEST_USER);
        assertEquals(Optional.of(userBlgPath), result);
    }

    @Test
    void returnsDefaultBlgPathWhenUserPathIsAbsent() {
        MetaData metaData = new MetaData();

        Path bibPath = Path.of("/home/user/MyLibrary.bib");
        Optional<Path> result = BibLogPathResolver.resolve(metaData, Optional.of(bibPath), TEST_USER);

        assertEquals(Optional.of(Path.of("/home/user/MyLibrary.blg")), result);
    }

    @Test
    void returnsEmptyWhenNoUserPathAndNoBibPath() {
        MetaData metaData = new MetaData();
        Optional<Path> result = BibLogPathResolver.resolve(metaData, Optional.empty(), TEST_USER);
        assertEquals(Optional.empty(), result);
    }
}
