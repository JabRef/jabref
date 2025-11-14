package org.jabref.logic.externalfiles;

import org.jilt.Builder;
import org.jilt.BuilderStyle;
import org.jilt.Opt;

/// All paths are relative to tmpDir
@Builder(style = BuilderStyle.STAGED_PRESERVING_ORDER)
public record BibTestConfiguration(
        String bibDir,
        @Opt String librarySpecificFileDirectory,
        @Opt String userSpecificFileDirectory,
        String sourceFileDir,
        FileTestConfiguration.TestFileLinkMode fileLinkMode
) {
}
