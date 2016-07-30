package net.sf.jabref.logic.importer;

import java.nio.charset.Charset;
import java.util.Set;

import net.sf.jabref.logic.bibtex.FieldContentParserPreferences;
import net.sf.jabref.logic.importer.fileformat.CustomImporter;
import net.sf.jabref.logic.labelpattern.LabelPatternPreferences;
import net.sf.jabref.preferences.JabRefPreferences;

public class ImportFormatPreferences {

    private final Set<CustomImporter> customImportList;

    private final Charset defaultEncoding;

    private final String keywordSeparator;

    private final LabelPatternPreferences labelPatternPreferences;

    private final FieldContentParserPreferences fieldContentParserPreferences;


    public ImportFormatPreferences(Set<CustomImporter> customImportList, Charset defaultEncoding,
            String keywordSeparator, LabelPatternPreferences labelPatternPreferences,
            FieldContentParserPreferences fieldContentParserPreferences) {
        this.customImportList = customImportList;
        this.defaultEncoding = defaultEncoding;
        this.keywordSeparator = keywordSeparator;
        this.labelPatternPreferences = labelPatternPreferences;
        this.fieldContentParserPreferences = fieldContentParserPreferences;
    }

    public Set<CustomImporter> getCustomImportList() {
        return customImportList;
    }

    public Charset getDefaultEncoding() {
        return defaultEncoding;
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

    static public ImportFormatPreferences fromPreferences(JabRefPreferences jabRefPreferences) {
        return new ImportFormatPreferences(jabRefPreferences.customImports, jabRefPreferences.getDefaultEncoding(),
                jabRefPreferences.get(JabRefPreferences.KEYWORD_SEPARATOR),
                LabelPatternPreferences.fromPreferences(jabRefPreferences),
                FieldContentParserPreferences.fromPreferences(jabRefPreferences));
    }
}
