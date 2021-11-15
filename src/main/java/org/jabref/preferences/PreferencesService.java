package org.jabref.preferences;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.prefs.BackingStoreException;

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
import org.jabref.logic.JabRefException;
import org.jabref.logic.bibtex.FieldContentFormatterPreferences;
import org.jabref.logic.bibtex.FieldWriterPreferences;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.citationkeypattern.GlobalCitationKeyPattern;
import org.jabref.logic.cleanup.CleanupPreferences;
import org.jabref.logic.cleanup.CleanupPreset;
import org.jabref.logic.exporter.SavePreferences;
import org.jabref.logic.exporter.TemplateExporter;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.fileformat.CustomImporter;
import org.jabref.logic.journals.JournalAbbreviationPreferences;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Language;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.layout.format.FileLinkPreferences;
import org.jabref.logic.layout.format.NameFormatterPreferences;
import org.jabref.logic.net.ProxyPreferences;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.preferences.DOIPreferences;
import org.jabref.logic.preferences.OwnerPreferences;
import org.jabref.logic.preferences.TimestampPreferences;
import org.jabref.logic.protectedterms.ProtectedTermsPreferences;
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

    VersionPreferences getVersionPreferences();

    void storeVersionPreferences(VersionPreferences versionPreferences);

    JournalAbbreviationPreferences getJournalAbbreviationPreferences();

    void storeKeyBindingRepository(KeyBindingRepository keyBindingRepository);

    KeyBindingRepository getKeyBindingRepository();

    void storeJournalAbbreviationPreferences(JournalAbbreviationPreferences abbreviationsPreferences);

    FilePreferences getFilePreferences();

    FieldWriterPreferences getFieldWriterPreferences();

    FileHistory getFileHistory();

    void storeFileHistory(FileHistory history);

    FieldContentFormatterPreferences getFieldContentParserPreferences();

    OpenOfficePreferences getOpenOfficePreferences();

    void setOpenOfficePreferences(OpenOfficePreferences openOfficePreferences);

    Map<String, Object> getPreferences();

    Map<String, Object> getDefaults();

    void exportPreferences(Path file) throws JabRefException;

    void importPreferences(Path file) throws JabRefException;

    LayoutFormatterPreferences getLayoutFormatterPreferences(JournalAbbreviationRepository repository);

    ImportFormatPreferences getImportFormatPreferences();

    SavePreferences getSavePreferencesForExport();

    SavePreferences getSavePreferences();

    String getUser();

    SaveOrderConfig getExportSaveOrder();

    void storeExportSaveOrder(SaveOrderConfig config);

    boolean shouldWarnAboutDuplicatesForImport();

    void setShouldWarnAboutDuplicatesForImport(boolean value);

    void clear() throws BackingStoreException;

    void deleteKey(String key) throws IllegalArgumentException;

    void flush();

    List<BibEntryType> getBibEntryTypes(BibDatabaseMode mode);

    void storeCustomEntryTypes(BibEntryTypesManager entryTypesManager);

    void clearBibEntryTypes(BibDatabaseMode mode);

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

    GeneralPreferences getGeneralPreferences();

    TelemetryPreferences getTelemetryPreferences();

    DOIPreferences getDOIPreferences();

    OwnerPreferences getOwnerPreferences();

    TimestampPreferences getTimestampPreferences();

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

    Map<String, Set<Field>> getDefaultTabNamesAndFields();

    List<Field> getAllDefaultTabFieldNames();

    EntryEditorPreferences getEntryEditorPreferences();

    //*************************************************************************************************************
    // Network preferences
    //*************************************************************************************************************

    RemotePreferences getRemotePreferences();

    ProxyPreferences getProxyPreferences();

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

    PushToApplicationPreferences getPushToApplicationPreferences();

    void storePushToApplicationPreferences(PushToApplicationPreferences preferences);

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

    //*************************************************************************************************************
    // File preferences
    //*************************************************************************************************************

    boolean shouldOpenLastFilesOnStartup();

    void storeOpenLastFilesOnStartup(boolean openLastFilesOnStartup);

    AutoLinkPreferences getAutoLinkPreferences();

    boolean shouldAutosave();

    void storeShouldAutosave(boolean shouldAutosave);

    FileLinkPreferences getFileLinkPreferences();

    void storeFileDirforDatabase(List<Path> dirs);

    //*************************************************************************************************************
    // Import/Export preferences
    //*************************************************************************************************************

    ImportExportPreferences getImportExportPreferences();

    List<TemplateExporter> getCustomExportFormats(JournalAbbreviationRepository repository);

    void storeCustomExportFormats(List<TemplateExporter> exporters);

    Set<CustomImporter> getCustomImportFormats();

    void storeCustomImportFormats(Set<CustomImporter> customImporters);

    ImporterPreferences getImporterPreferences();

    //*************************************************************************************************************
    // Preview preferences
    //*************************************************************************************************************

    PreviewPreferences getPreviewPreferences();

    void updatePreviewPreferences();

    void storePreviewPreferences(PreviewPreferences previewPreferences);

    //*************************************************************************************************************
    // SidePanePreferences
    //*************************************************************************************************************

    SidePanePreferences getSidePanePreferences();

    //*************************************************************************************************************
    // GuiPreferences
    //*************************************************************************************************************

    GuiPreferences getGuiPreferences();

    void clearEditedFiles();

    /**
     * Gets the directory for file browsing dialogs. This ensures that each browse dialog starts in the last visited
     * browse directory.
     */
    Path getWorkingDir();

    /**
     * Stores the directory for file browsing dialogs
     */
    void setWorkingDirectory(Path dir);

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

    String getLastPreferencesExportPath();

    void storeLastPreferencesExportPath(Path exportFile);

    Optional<String> getExternalFileTypes();

    void storeExternalFileTypes(String externalFileTypes);

    Optional<String> getMergeDiffMode();

    void storeMergeDiffMode(String diffMode);

    MrDlibPreferences getMrDlibPreferences();

    String getIdBasedFetcherForEntryGenerator();

    void storeIdBasedFetcherForEntryGenerator(String fetcherName);

    ProtectedTermsPreferences getProtectedTermsPreferences();
}
