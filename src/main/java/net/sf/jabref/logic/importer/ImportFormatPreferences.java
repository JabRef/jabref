package net.sf.jabref.logic.importer;

import java.nio.charset.Charset;
import java.util.Set;

import net.sf.jabref.logic.bibtex.FieldContentParserPreferences;
import net.sf.jabref.logic.importer.fileformat.CustomImporter;
import net.sf.jabref.logic.labelpattern.LabelPatternPreferences;
import net.sf.jabref.preferences.JabRefPreferences;

public class ImportFormatPreferences {

    private final Set<CustomImporter> customImportList;

    private final Charset encoding;

    private final String keywordSeparator;

    private final LabelPatternPreferences labelPatternPreferences;

    private final FieldContentParserPreferences fieldContentParserPreferences;

    private final boolean useCaseKeeperOnSearch;
    private final boolean convertUnitsOnSearch;


    public ImportFormatPreferences(Set<CustomImporter> customImportList, Charset encoding,
            String keywordSeparator, LabelPatternPreferences labelPatternPreferences,
            FieldContentParserPreferences fieldContentParserPreferences, boolean convertUnitsOnSearch,
            boolean useCaseKeeperOnSearch) {
        this.customImportList = customImportList;
        this.encoding = encoding;
        this.keywordSeparator = keywordSeparator;
        this.labelPatternPreferences = labelPatternPreferences;
        this.fieldContentParserPreferences = fieldContentParserPreferences;
        this.convertUnitsOnSearch = convertUnitsOnSearch;
        this.useCaseKeeperOnSearch = useCaseKeeperOnSearch;
    }

    public Set<CustomImporter> getCustomImportList() {
        return customImportList;
    }

    public Charset getEncoding() {
        return encoding;
    }

    public String getKeywordSeparator() {
        return keywordSeparator;
    }

    public LabelPatternPreferences getLabelPatternPreferences() {
        return labelPatternPreferences;
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
        return new ImportFormatPreferences(customImportList, newEncoding, keywordSeparator, labelPatternPreferences,
                fieldContentParserPreferences, convertUnitsOnSearch, useCaseKeeperOnSearch);
    }

    static public ImportFormatPreferences fromPreferences(JabRefPreferences jabRefPreferences) {
        return new ImportFormatPreferences(jabRefPreferences.customImports, jabRefPreferences.getDefaultEncoding(),
                jabRefPreferences.get(JabRefPreferences.KEYWORD_SEPARATOR),
                LabelPatternPreferences.fromPreferences(jabRefPreferences),
                FieldContentParserPreferences.fromPreferences(jabRefPreferences),
                jabRefPreferences.getBoolean(JabRefPreferences.USE_UNIT_FORMATTER_ON_SEARCH),
                jabRefPreferences.getBoolean(JabRefPreferences.USE_CASE_KEEPER_ON_SEARCH));
    }
}
