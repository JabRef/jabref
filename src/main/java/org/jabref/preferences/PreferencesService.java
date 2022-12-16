package org.jabref.preferences;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.prefs.BackingStoreException;

import org.jabref.gui.autocompleter.AutoCompletePreferences;
import org.jabref.gui.entryeditor.EntryEditorPreferences;
import org.jabref.gui.groups.GroupsPreferences;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.maintable.ColumnPreferences;
import org.jabref.gui.maintable.MainTableNameFormatPreferences;
import org.jabref.gui.maintable.MainTablePreferences;
import org.jabref.gui.specialfields.SpecialFieldsPreferences;
import org.jabref.logic.JabRefException;
import org.jabref.logic.bibtex.FieldContentFormatterPreferences;
import org.jabref.logic.bibtex.FieldWriterPreferences;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.citationkeypattern.GlobalCitationKeyPattern;
import org.jabref.logic.exporter.SavePreferences;
import org.jabref.logic.exporter.TemplateExporter;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.fetcher.GrobidPreferences;
import org.jabref.logic.journals.JournalAbbreviationPreferences;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.layout.format.FileLinkPreferences;
import org.jabref.logic.layout.format.NameFormatterPreferences;
import org.jabref.logic.net.ProxyPreferences;
import org.jabref.logic.net.ssl.SSLPreferences;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.preferences.DOIPreferences;
import org.jabref.logic.preferences.OwnerPreferences;
import org.jabref.logic.preferences.TimestampPreferences;
import org.jabref.logic.protectedterms.ProtectedTermsPreferences;
import org.jabref.logic.remote.RemotePreferences;
import org.jabref.logic.util.io.AutoLinkPreferences;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.Field;
import org.jabref.model.metadata.SaveOrderConfig;

public interface PreferencesService {

    InternalPreferences getInternalPreferences();

    JournalAbbreviationPreferences getJournalAbbreviationPreferences();

    void storeKeyBindingRepository(KeyBindingRepository keyBindingRepository);

    KeyBindingRepository getKeyBindingRepository();

    void storeJournalAbbreviationPreferences(JournalAbbreviationPreferences abbreviationsPreferences);

    FilePreferences getFilePreferences();

    FieldWriterPreferences getFieldWriterPreferences();

    FieldContentFormatterPreferences getFieldContentParserPreferences();

    OpenOfficePreferences getOpenOfficePreferences();

    Map<String, Object> getPreferences();

    Map<String, Object> getDefaults();

    void exportPreferences(Path file) throws JabRefException;

    void importPreferences(Path file) throws JabRefException;

    LayoutFormatterPreferences getLayoutFormatterPreferences(JournalAbbreviationRepository repository);

    ImportFormatPreferences getImportFormatPreferences();

    SavePreferences getSavePreferencesForExport();

    SavePreferences getSavePreferences();

    SaveOrderConfig getExportSaveOrder();

    void storeExportSaveOrder(SaveOrderConfig config);

    void clear() throws BackingStoreException;

    void deleteKey(String key) throws IllegalArgumentException;

    void flush();

    List<BibEntryType> getBibEntryTypes(BibDatabaseMode mode);

    void storeCustomEntryTypes(BibEntryTypesManager entryTypesManager);

    void clearBibEntryTypes(BibDatabaseMode mode);

    CleanupPreferences getCleanupPreferences();

    CleanupPreferences getDefaultCleanupPreset();

    @Deprecated
    String getDefaultsDefaultCitationKeyPattern();

    //*************************************************************************************************************
    // GeneralPreferences
    //*************************************************************************************************************

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

    SSLPreferences getSSLPreferences();

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

    ExternalApplicationsPreferences getExternalApplicationsPreferences();

    //*************************************************************************************************************
    // MainTablePreferences
    //*************************************************************************************************************

    void updateMainTableColumns();

    ColumnPreferences getColumnPreferences();

    void storeMainTableColumnPreferences(ColumnPreferences columnPreferences);

    MainTablePreferences getMainTablePreferences();

    void storeMainTablePreferences(MainTablePreferences mainTablePreferences);

    MainTableNameFormatPreferences getMainTableNameFormatPreferences();

    void storeMainTableNameFormatPreferences(MainTableNameFormatPreferences preferences);

    //*************************************************************************************************************
    // SearchDialogColumnPreferences
    //*************************************************************************************************************

    ColumnPreferences getSearchDialogColumnPreferences();

    void storeSearchDialogColumnPreferences(ColumnPreferences columnPreferences);

    //*************************************************************************************************************
    // AppearancePreferences
    //*************************************************************************************************************

    AppearancePreferences getAppearancePreferences();

    //*************************************************************************************************************
    // File preferences
    //*************************************************************************************************************

    AutoLinkPreferences getAutoLinkPreferences();

    FileLinkPreferences getFileLinkPreferences();

    void storeFileDirForDatabase(List<Path> dirs);

    //*************************************************************************************************************
    // Import/Export preferences
    //*************************************************************************************************************

    ImportExportPreferences getImportExportPreferences();

    List<TemplateExporter> getCustomExportFormats(JournalAbbreviationRepository repository);

    void storeCustomExportFormats(List<TemplateExporter> exporters);

    ImporterPreferences getImporterPreferences();

    GrobidPreferences getGrobidPreferences();

    //*************************************************************************************************************
    // Preview preferences
    //*************************************************************************************************************

    PreviewPreferences getPreviewPreferences();

    //*************************************************************************************************************
    // SidePanePreferences
    //*************************************************************************************************************

    SidePanePreferences getSidePanePreferences();

    //*************************************************************************************************************
    // GuiPreferences
    //*************************************************************************************************************

    GuiPreferences getGuiPreferences();

    //*************************************************************************************************************
    // Misc preferences
    //*************************************************************************************************************

    XmpPreferences getXmpPreferences();

    NameFormatterPreferences getNameFormatterPreferences();

    AutoCompletePreferences getAutoCompletePreferences();

    SpecialFieldsPreferences getSpecialFieldsPreferences();

    SearchPreferences getSearchPreferences();

    String getLastPreferencesExportPath();

    void storeLastPreferencesExportPath(Path exportFile);

    MrDlibPreferences getMrDlibPreferences();

    ProtectedTermsPreferences getProtectedTermsPreferences();
}
