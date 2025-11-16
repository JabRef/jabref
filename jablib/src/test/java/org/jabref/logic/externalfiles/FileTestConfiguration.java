package org.jabref.logic.externalfiles;

import java.nio.file.Path;
import java.util.Optional;

import org.jabref.logic.FilePreferences;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;

import org.jilt.Builder;
import org.jilt.BuilderStyle;
import org.jilt.Opt;
import org.jspecify.annotations.Nullable;

import static org.mockito.Mockito.when;

public class FileTestConfiguration {
    final BibDatabaseContext sourceContext;
    // final BibEntry sourceEntry;

    final BibDatabaseContext targetContext;
    // final BibEntry targetEntry;

    @Nullable private final Path mainFileDir;

    private final BibTestConfiguration sourceBibTestConfiguration;
    private final BibTestConfiguration targetBibTestConfiguration;

    /// @param tempDir the temporary directory to use
    /// @param filePreferences the file preferences to modify
    @Builder(style = BuilderStyle.STAGED_PRESERVING_ORDER)
    public FileTestConfiguration(
            @Opt String mainFileDir,
            Boolean shouldStoreFilesRelativeToBibFile,
            Boolean shouldAdjustOrCopyLinkedFilesOnTransfer,

            BibTestConfiguration sourceBibTestConfiguration,
            BibTestConfiguration targetBibTestConfiguration,

            FilePreferences filePreferences,
            Path tempDir
    ) {
        if (mainFileDir == null) {
            when(filePreferences.getMainFileDirectory()).thenReturn(Optional.empty());
            this.mainFileDir = null;
        } else {
            this.mainFileDir = tempDir.resolve(mainFileDir);
            when(filePreferences.getMainFileDirectory()).thenReturn(Optional.of(this.mainFileDir));
        }

        when(filePreferences.getUserAndHost()).thenReturn("testuser@testhost");
        when(filePreferences.shouldStoreFilesRelativeToBibFile()).thenReturn(shouldStoreFilesRelativeToBibFile);
        when(filePreferences.shouldAdjustOrCopyLinkedFilesOnTransfer()).thenReturn(shouldAdjustOrCopyLinkedFilesOnTransfer);

        this.sourceBibTestConfiguration = sourceBibTestConfiguration;
        this.sourceContext = createContext(sourceBibTestConfiguration, this.mainFileDir);

        this.targetBibTestConfiguration = targetBibTestConfiguration;
        this.targetContext = createContext(targetBibTestConfiguration, this.mainFileDir);
    }

    /// Creates a BibDatabaseContext with a single file linked as wished
    static BibDatabaseContext createContext(BibTestConfiguration bibTestConfiguration, @Nullable Path mainFileDir) {
        BibDatabaseContext context = new BibDatabaseContext(new BibDatabase());
        bibTestConfiguration.updateContext(context, mainFileDir);
        return context;
    }

    enum TestFileLinkMode {
        ABSOLUTE,
        RELATIVE_TO_MAIN_FILE_DIR,
        RELATIVE_TO_BIB,
        RELATIVE_TO_LIBRARY_SPECIFIC_DIR,
        RELATIVE_TO_USER_SPECIFIC_DIR
    }
}
