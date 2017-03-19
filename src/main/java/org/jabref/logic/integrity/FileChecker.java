package org.jabref.logic.integrity;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.FileField;
import org.jabref.model.entry.ParsedFileField;
import org.jabref.model.metadata.FileDirectoryPreferences;

public class FileChecker implements ValueChecker {

    private final BibDatabaseContext context;
    private final FileDirectoryPreferences fileDirectoryPreferences;


    public FileChecker(BibDatabaseContext context, FileDirectoryPreferences fileDirectoryPreferences) {
        this.context = context;
        this.fileDirectoryPreferences = fileDirectoryPreferences;
    }


    @Override
    public Optional<String> checkValue(String value) {
        List<ParsedFileField> parsedFileFields = FileField.parse(value).stream()
                .filter(p -> !(p.getLink().startsWith("http://") || p.getLink().startsWith("https://")))
                .collect(Collectors.toList());

        for (ParsedFileField p : parsedFileFields) {
            Optional<File> file = FileUtil.expandFilename(context, p.getLink(), fileDirectoryPreferences);
            if ((!file.isPresent()) || !file.get().exists()) {
                return Optional.of(Localization.lang("link should refer to a correct file path"));
            }
        }

        return Optional.empty();
    }
}
