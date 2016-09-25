package net.sf.jabref.logic.importer;

import java.nio.charset.Charset;
import java.util.Set;

import net.sf.jabref.logic.bibtex.FieldContentParserPreferences;
import net.sf.jabref.logic.bibtexkeypattern.BibtexKeyPatternPreferences;
import net.sf.jabref.logic.importer.fileformat.CustomImporter;

public class ImportFormatPreferences {

    private final Set<CustomImporter> customImportList;

    private final Charset encoding;

    private final Character keywordSeparator;

    private final BibtexKeyPatternPreferences bibtexKeyPatternPreferences;

    private final FieldContentParserPreferences fieldContentParserPreferences;

    private final boolean useCaseKeeperOnSearch;
    private final boolean convertUnitsOnSearch;

    private final boolean keywordSyncEnabled;


    public ImportFormatPreferences(Set<CustomImporter> customImportList, Charset encoding,
            Character keywordSeparator, BibtexKeyPatternPreferences bibtexKeyPatternPreferences,
            FieldContentParserPreferences fieldContentParserPreferences, boolean convertUnitsOnSearch,
            boolean useCaseKeeperOnSearch, boolean keywordSyncEnabled) {
        this.customImportList = customImportList;
        this.encoding = encoding;
        this.keywordSeparator = keywordSeparator;
        this.bibtexKeyPatternPreferences = bibtexKeyPatternPreferences;
        this.fieldContentParserPreferences = fieldContentParserPreferences;
        this.convertUnitsOnSearch = convertUnitsOnSearch;
        this.useCaseKeeperOnSearch = useCaseKeeperOnSearch;
        this.keywordSyncEnabled = keywordSyncEnabled;
    }

    public Set<CustomImporter> getCustomImportList() {
        return customImportList;
    }

    public Charset getEncoding() {
        return encoding;
    }

    public Character getKeywordSeparator() {
        return keywordSeparator;
    }

    public BibtexKeyPatternPreferences getBibtexKeyPatternPreferences() {
        return bibtexKeyPatternPreferences;
    }

    public FieldContentParserPreferences getFieldContentParserPreferences() {
        return fieldContentParserPreferences;
    }

    public boolean isConvertUnitsOnSearch() {
        return convertUnitsOnSearch;
    }

    public boolean isUseCaseKeeperOnSearch() {
        return useCaseKeeperOnSearch;
    }

    public ImportFormatPreferences withEncoding(Charset newEncoding) {
        return new ImportFormatPreferences(customImportList, newEncoding, keywordSeparator, bibtexKeyPatternPreferences,
                fieldContentParserPreferences, convertUnitsOnSearch, useCaseKeeperOnSearch, keywordSyncEnabled);
    }

    public boolean isKeywordSyncEnabled() {
        return keywordSyncEnabled;
    }
}
