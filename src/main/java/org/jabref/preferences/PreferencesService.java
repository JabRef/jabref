package org.jabref.preferences;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jabref.gui.autocompleter.AutoCompletePreferences;
import org.jabref.gui.entryeditor.EntryEditorPreferences;
import org.jabref.gui.groups.GroupViewMode;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.maintable.ColumnPreferences;
import org.jabref.gui.maintable.MainTableNameFormatPreferences;
import org.jabref.gui.maintable.MainTablePreferences;
import org.jabref.gui.specialfields.SpecialFieldsPreferences;
import org.jabref.logic.bibtex.FieldContentFormatterPreferences;
import org.jabref.logic.bibtex.FieldWriterPreferences;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.cleanup.CleanupPreferences;
import org.jabref.logic.cleanup.CleanupPreset;
import org.jabref.logic.exporter.SavePreferences;
import org.jabref.logic.exporter.TemplateExporter;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.journals.JournalAbbreviationPreferences;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Language;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.net.ProxyPreferences;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.preferences.OwnerPreferences;
import org.jabref.logic.preferences.TimestampPreferences;
import org.jabref.logic.protectedterms.ProtectedTermsLoader;
import org.jabref.logic.remote.RemotePreferences;
import org.jabref.logic.util.io.AutoLinkPreferences;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.bibtexkeypattern.GlobalCitationKeyPattern;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.field.Field;
import org.jabref.model.metadata.FilePreferences;
import org.jabref.model.metadata.SaveOrderConfig;

public interface PreferencesService {

    void setProtectedTermsPreferences(ProtectedTermsLoader loader);

    JournalAbbreviationPreferences getJournalAbbreviationPreferences();

    Character getKeywordDelimiter();

    void storeKeyBindingRepository(KeyBindingRepository keyBindingRepository);

    KeyBindingRepository getKeyBindingRepository();

    void storeJournalAbbreviationPreferences(JournalAbbreviationPreferences abbreviationsPreferences);

    FilePreferences getFilePreferences();

    FieldWriterPreferences getFieldWriterPreferences();

    FieldContentFormatterPreferences getFieldContentParserPreferences();

    XmpPreferences getXMPPreferences();

    AutoLinkPreferences getAutoLinkPreferences();

    Path getWorkingDir();

    void setWorkingDir(Path dir);

    OpenOfficePreferences getOpenOfficePreferences();

    void setOpenOfficePreferences(OpenOfficePreferences openOfficePreferences);

    PreviewPreferences getPreviewPreferences();

    List<TemplateExporter> getCustomExportFormats(JournalAbbreviationRepository repository);

    void storeCustomExportFormats(List<TemplateExporter> exporters);

    LayoutFormatterPreferences getLayoutFormatterPreferences(JournalAbbreviationRepository repository);

    ImportFormatPreferences getImportFormatPreferences();

    boolean isKeywordSyncEnabled();

    SavePreferences loadForExportFromPreferences();

    String getExportWorkingDirectory();

    void setExportWorkingDirectory(String layoutFileDirString);

    Charset getDefaultEncoding();

    String getUser();

    String getTheme();

    SaveOrderConfig loadExportSaveOrder();

    void storeExportSaveOrder(SaveOrderConfig config);

    boolean shouldWarnAboutDuplicatesForImport();

    void setShouldWarnAboutDuplicatesForImport(boolean value);

    void saveCustomEntryTypes();

    List<BibEntryType> loadBibEntryTypes(BibDatabaseMode mode);

    CleanupPreferences getCleanupPreferences(JournalAbbreviationRepository repository);

    CleanupPreset getCleanupPreset();

    void setCleanupPreset(CleanupPreset cleanupPreset);

    //*************************************************************************************************************
    // GeneralPreferences
    //*************************************************************************************************************

    Language getLanguage();

    void setLanguage(Language language);

    boolean shouldCollectTelemetry();

    void setShouldCollectTelemetry(boolean value);

    boolean shouldAskToCollectTelemetry();

    void askedToCollectTelemetry();

    String getUnwantedCharacters();

    boolean getAllowIntegerEdition();

    GeneralPreferences getGeneralPreferences();

    void storeGeneralPreferences(GeneralPreferences preferences);

    OwnerPreferences getOwnerPreferences();

    void storeOwnerPreferences(OwnerPreferences preferences);

    TimestampPreferences getTimestampPreferences();

    void storeTimestampPreferences(TimestampPreferences preferences);

    //*************************************************************************************************************
    // ToDo: GroupPreferences
    //*************************************************************************************************************

    GroupViewMode getGroupViewMode();

    void setGroupViewMode(GroupViewMode mode);

    boolean getDisplayGroupCount();

    //*************************************************************************************************************
    // EntryEditorPreferences
    //*************************************************************************************************************

    Map<String, Set<Field>> getEntryEditorTabList();

    void updateEntryEditorTabList();

    Map<String, Set<Field>> getDefaultTabNamesAndFields();

    List<Field> getAllDefaultTabFieldNames();

    void storeEntryEditorTabList(Map<String, Set<Field>> customTabsMap);

    EntryEditorPreferences getEntryEditorPreferences();

    void storeEntryEditorPreferences(EntryEditorPreferences preferences);

    //*************************************************************************************************************
    // Network preferences
    //*************************************************************************************************************

    RemotePreferences getRemotePreferences();

    void storeRemotePreferences(RemotePreferences remotePreferences);

    ProxyPreferences getProxyPreferences();

    void storeProxyPreferences(ProxyPreferences proxyPreferences);

    //*************************************************************************************************************
    // CitationKeyPatternPreferences
    //*************************************************************************************************************

    GlobalCitationKeyPattern getGlobalCitationKeyPattern();

    void updateGlobalCitationKeyPattern();

    CitationKeyPatternPreferences getCitationKeyPatternPreferences();

    void storeCitationKeyPatternPreferences(CitationKeyPatternPreferences preferences);

    //*************************************************************************************************************
    // ExternalApplicationsPreferences
    //*************************************************************************************************************

    ExternalApplicationsPreferences getExternalApplicationsPreferences();

    void storeExternalApplicationsPreferences(ExternalApplicationsPreferences preferences);

    //*************************************************************************************************************
    // MainTablePreferences
    //*************************************************************************************************************

    void updateMainTableColumns();

    ColumnPreferences getColumnPreferences();

    void storeColumnPreferences(ColumnPreferences columnPreferences);

    MainTablePreferences getMainTablePreferences();

    void storeMainTablePreferences(MainTablePreferences mainTablePreferences);

    MainTableNameFormatPreferences getMainTableNameFormatPreferences();

    void storeMainTableNameFormatPreferences(MainTableNameFormatPreferences preferences);

    //*************************************************************************************************************
    // ToDo: Misc preferences
    //*************************************************************************************************************

    AutoCompletePreferences getAutoCompletePreferences();

    void storeAutoCompletePreferences(AutoCompletePreferences autoCompletePreferences);

    SpecialFieldsPreferences getSpecialFieldsPreferences();

    void storeSpecialFieldsPreferences(SpecialFieldsPreferences specialFieldsPreferences);
}
