package net.sf.jabref.logic.integrity;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.io.FileUtil;
import net.sf.jabref.model.database.BibDatabaseContext;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;
import net.sf.jabref.model.entry.FileField;
import net.sf.jabref.model.entry.ParsedFileField;
import net.sf.jabref.model.metadata.FileDirectoryPreferences;

public class FileChecker extends FieldChecker {

    private final BibDatabaseContext context;
    private final FileDirectoryPreferences fileDirectoryPreferences;


    public FileChecker(BibDatabaseContext context, FileDirectoryPreferences fileDirectoryPreferences) {
        super(FieldName.FILE);
        this.context = context;
        this.fileDirectoryPreferences = fileDirectoryPreferences;
    }


    @Override
    protected List<IntegrityMessage> checkValue(String value, BibEntry entry) {
        List<ParsedFileField> parsedFileFields = FileField.parse(value).stream()
                .filter(p -> !(p.getLink().startsWith("http://") || p.getLink().startsWith("https://")))
                .collect(Collectors.toList());

        for (ParsedFileField p : parsedFileFields) {
            Optional<File> file = FileUtil.expandFilename(context, p.getLink(), fileDirectoryPreferences);
            if ((!file.isPresent()) || !file.get().exists()) {
                return Collections.singletonList(new IntegrityMessage(
                        Localization.lang("link should refer to a correct file path"), entry, FieldName.FILE));
            }
        }

        return Collections.emptyList();
    }
}
