package org.jabref.preferences;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jabref.gui.autocompleter.AutoCompletePreferences;
import org.jabref.gui.entryeditor.EntryEditorPreferences;
import org.jabref.gui.groups.GroupViewMode;
import org.jabref.gui.groups.GroupsPreferences;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.maintable.ColumnPreferences;
import org.jabref.gui.maintable.MainTableNameFormatPreferences;
import org.jabref.gui.maintable.MainTablePreferences;
import org.jabref.gui.specialfields.SpecialFieldsPreferences;
import org.jabref.gui.util.Theme;
import org.jabref.logic.bibtex.FieldContentFormatterPreferences;
import org.jabref.logic.bibtex.FieldWriterPreferences;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.citationkeypattern.GlobalCitationKeyPattern;
import org.jabref.logic.cleanup.CleanupPreferences;
import org.jabref.logic.cleanup.CleanupPreset;
import org.jabref.logic.exporter.SavePreferences;
import org.jabref.logic.exporter.TemplateExporter;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.journals.JournalAbbreviationPreferences;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Language;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.layout.format.NameFormatterPreferences;
import org.jabref.logic.net.ProxyPreferences;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.preferences.OwnerPreferences;
import org.jabref.logic.preferences.TimestampPreferences;
import org.jabref.logic.protectedterms.ProtectedTermsLoader;
import org.jabref.logic.remote.RemotePreferences;
import org.jabref.logic.util.io.AutoLinkPreferences;
import org.jabref.logic.util.io.FileHistory;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.Field;
import org.jabref.model.metadata.SaveOrderConfig;

public interface PreferencesService {

    void setProtectedTermsPreferences(ProtectedTermsLoader loader);

    JournalAbbreviationPreferences getJournalAbbreviationPreferences();

    void storeKeyBindingRepository(KeyBindingRepository keyBindingRepository);

    KeyBindingRepository getKeyBindingRepository();

    void storeJournalAbbreviationPreferences(JournalAbbreviationPreferences abbreviationsPreferences);

    FilePreferences getFilePreferences();

    void storeFilePreferences(FilePreferences filePreferences);

    FieldWriterPreferences getFieldWriterPreferences();

    FileHistory getFileHistory();

    void storeFileHistory(FileHistory history);

    FieldContentFormatterPreferences getFieldContentParserPreferences();

    Path getWorkingDir();

    void setWorkingDir(Path dir);

    OpenOfficePreferences getOpenOfficePreferences();

    void setOpenOfficePreferences(OpenOfficePreferences openOfficePreferences);

    List<TemplateExporter> getCustomExportFormats(JournalAbbreviationRepository repository);

    void storeCustomExportFormats(List<TemplateExporter> exporters);

    LayoutFormatterPreferences getLayoutFormatterPreferences(JournalAbbreviationRepository repository);

    ImportFormatPreferences getImportFormatPreferences();

    SavePreferences getSavePreferencesForExport();

    SavePreferences getSavePreferences();

    String getExportWorkingDirectory();

    void setExportWorkingDirectory(String layoutFileDirString);

    Charset getDefaultEncoding();

    String getUser();

    SaveOrderConfig loadExportSaveOrder();

    void storeExportSaveOrder(SaveOrderConfig config);

    boolean shouldWarnAboutDuplicatesForImport();

    void setShouldWarnAboutDuplicatesForImport(boolean value);

    void saveCustomEntryTypes(BibEntryTypesManager entryTypesManager);

    void clearBibEntryTypes(BibDatabaseMode mode);

    List<BibEntryType> loadBibEntryTypes(BibDatabaseMode mode);

    CleanupPreferences getCleanupPreferences(JournalAbbreviationRepository repository);

    CleanupPreset getCleanupPreset();

    void setCleanupPreset(CleanupPreset cleanupPreset);

    @Deprecated
    String getDefaultsDefaultCitationKeyPattern();

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
    // GroupsPreferences
    //*************************************************************************************************************

    Character getKeywordDelimiter();

    GroupsPreferences getGroupsPreferences();

    void storeGroupsPreferences(GroupsPreferences preferences);

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
    // AppearancePreferences
    //*************************************************************************************************************

    Theme getTheme();

    void updateTheme();

    AppearancePreferences getAppearancePreferences();

    void storeAppearancePreference(AppearancePreferences preferences);

    //*************************************************************************************************************
    // File preferences
    //*************************************************************************************************************

    boolean shouldOpenLastFilesOnStartup();

    void storeOpenLastFilesOnStartup(boolean openLastFilesOnStartup);

    NewLineSeparator getNewLineSeparator();

    void storeNewLineSeparator(NewLineSeparator newLineSeparator);

    void storeLastPreferencesExportPath(Path exportFile);

    String getLastPreferencesExportPath();

    AutoLinkPreferences getAutoLinkPreferences();

    void storeAutoLinkPreferences(AutoLinkPreferences autoLinkPreferences);

    ImportExportPreferences getImportExportPreferences();

    void storeImportExportPreferences(ImportExportPreferences importExportPreferences);

    boolean getShouldAutosave();

    void storeShouldAutosave(boolean shouldAutosave);

    //*************************************************************************************************************
    // Preview preferences
    //*************************************************************************************************************

    PreviewPreferences getPreviewPreferences();

    void updatePreviewPreferences();

    void storePreviewPreferences(PreviewPreferences previewPreferences);

    //*************************************************************************************************************
    // Misc preferences
    //*************************************************************************************************************

    XmpPreferences getXmpPreferences();

    void storeXmpPreferences(XmpPreferences preferences);

    NameFormatterPreferences getNameFormatterPreferences();

    void storeNameFormatterPreferences(NameFormatterPreferences preferences);

    AutoCompletePreferences getAutoCompletePreferences();

    void storeAutoCompletePreferences(AutoCompletePreferences autoCompletePreferences);

    SpecialFieldsPreferences getSpecialFieldsPreferences();

    void storeSpecialFieldsPreferences(SpecialFieldsPreferences specialFieldsPreferences);

    SearchPreferences getSearchPreferences();

    void storeSearchPreferences(SearchPreferences preferences);
}
