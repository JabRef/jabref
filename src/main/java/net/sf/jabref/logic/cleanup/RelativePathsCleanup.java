package net.sf.jabref.logic.cleanup;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import net.sf.jabref.BibDatabaseContext;
import net.sf.jabref.logic.TypedBibEntry;
import net.sf.jabref.logic.util.io.FileUtil;
import net.sf.jabref.model.FieldChange;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.ParsedFileField;

public class RelativePathsCleanup implements CleanupJob {

    private final BibDatabaseContext databaseContext;

    public RelativePathsCleanup(BibDatabaseContext databaseContext) {
        this.databaseContext = Objects.requireNonNull(databaseContext);
    }

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        TypedBibEntry typedEntry = new TypedBibEntry(entry, databaseContext);
        List<ParsedFileField> fileList = typedEntry.getFiles();
        List<ParsedFileField> newFileList = new ArrayList<>();
        boolean changed = false;

        for (ParsedFileField fileEntry : fileList) {
            String oldFileName = fileEntry.getLink();
            String newFileName = FileUtil.shortenFileName(new File(oldFileName), databaseContext.getFileDirectory())
                    .toString();

            ParsedFileField newFileEntry = fileEntry;
            if (!oldFileName.equals(newFileName)) {
                newFileEntry = new ParsedFileField(fileEntry.getDescription(), newFileName, fileEntry.getFileType());
                changed = true;
            }
            newFileList.add(newFileEntry);
        }

        if (changed) {
            Optional<FieldChange> change = typedEntry.setFiles(newFileList);
            if(change.isPresent()) {
                return Collections.singletonList(change.get());
            } else {
                return Collections.emptyList();
            }
        }

        return Collections.emptyList();
    }

}
