package org.jabref.logic.integrity;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.importer.util.FileFieldParser;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.strings.StringUtil;

public class FileChecker implements ValueChecker {

    private final BibDatabaseContext context;
    private final FilePreferences filePreferences;

    public FileChecker(BibDatabaseContext context, FilePreferences filePreferences) {
        this.context = context;
        this.filePreferences = filePreferences;
    }

    @Override
    public Optional<String> checkValue(String value) {
        if (StringUtil.isBlank(value)) {
            return Optional.empty();
        }

        List<LinkedFile> linkedFiles = FileFieldParser
                .parse(value).stream()
                .filter(file -> !file.isOnlineLink())
                .collect(Collectors.toList());

        for (LinkedFile file : linkedFiles) {
            Optional<Path> linkedFile = file.findIn(context, filePreferences);
            if ((linkedFile.isEmpty()) || !Files.exists(linkedFile.get())) {
                return Optional.of(IntegrityIssue.LINK_SHOULD_REFER_TO_A_CORRECT_FILE_PATH.getText());
            }
        }

        return Optional.empty();
    }
}
