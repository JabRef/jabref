package org.jabref.logic.integrity;

import java.util.List;
import java.util.Optional;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.metadata.FilePreferences;
import org.jabref.model.metadata.MetaData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

public class FileCheckerTest {

    private FileChecker checker;
    private BibEntry entry;
    private BibDatabaseContext bibDatabaseContext;
    private List<IntegrityMessage> messages;
    MetaData metaData = mock(MetaData.class);
    //private FilePreferences f;
    //Map<Field, String> hash;

    @BeforeEach
    void setUp() {
        //hash.put(StandardField.ABSTRACT, "");
        //f = new FilePreferences("", hash, true, "", "");
        bibDatabaseContext = new BibDatabaseContext();
        checker = new FileChecker(bibDatabaseContext, mock(FilePreferences.class));
        entry = new BibEntry();
        Mockito.when(metaData.getDefaultFileDirectory()).thenReturn(Optional.of("."));
        //Mockito.when(metaData.getDefaultFileDirectory()).thenReturn(Optional.empty());
        Mockito.when(metaData.getUserFileDirectory(any(String.class))).thenReturn(Optional.empty());
        // FIXME: must be set as checkBibtexDatabase only activates title checker based on database mode
        Mockito.when(metaData.getMode()).thenReturn(Optional.of(BibDatabaseMode.BIBTEX));
    }

    @Test
    void fileAcceptsRelativePath() {
        assertEquals(Optional.empty(), checker.checkValue(":build.gradle:gradle"));
    }

    @Test
    void fileAcceptsFullPath() {
        assertEquals(Optional.empty(), checker.checkValue("description:build.gradle:gradle"));
    }
    @Test
    void fileDoesNotAcceptWrongPath() {
        assertNotEquals(Optional.empty(), checker.checkValue(":asflakjfwofja:PDF"));
    }

}
