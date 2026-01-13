package org.jabref.logic.cleanup;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.transform.TransformerException;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.JabRefException;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.xmp.XmpUtilWriter;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XmpMetadataCleanup implements CleanupJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(XmpMetadataCleanup.class);

    private final BibDatabaseContext databaseContext;
    private final FilePreferences filePreferences;
    private final List<JabRefException> failures;

    public XmpMetadataCleanup(BibDatabaseContext databaseContext, FilePreferences filePreferences) {
        this.databaseContext = databaseContext;
        this.filePreferences = filePreferences;
        this.failures = new ArrayList<>();
    }

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        List<LinkedFile> files = entry.getFiles();
        AtomicBoolean changed = new AtomicBoolean(false);
        for (LinkedFile file : files) {
            Optional<Path> filePath = file.findIn(databaseContext, filePreferences);
            filePath.ifPresent(path -> {
                try {
                    XmpUtilWriter.removeXmpMetadata(path);
                    changed.set(true);
                } catch (IOException | TransformerException e) {
                    LOGGER.error("Problem removing XMP metadata from file {}", path, e);
                    failures.add(new JabRefException(Localization.lang("Problem removing XMP metadata from file: %0", path.toString()), e));
                }
            });
        }

        if (changed.get()) {
            // Since only metadata is removed, no field "changes" but we still return a list
            return Collections.singletonList(new FieldChange(entry, StandardField.FILE, entry.getField(StandardField.FILE).orElse(null), entry.getField(StandardField.FILE).orElse(null)));
        }
        return new ArrayList<>();
    }

    public List<JabRefException> getFailures() {
        return failures;
    }
}
