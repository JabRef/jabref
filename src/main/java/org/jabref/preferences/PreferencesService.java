package org.jabref.preferences;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jabref.gui.entryeditor.EntryEditorPreferences;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.logic.exporter.SavePreferences;
import org.jabref.logic.exporter.TemplateExporter;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.journals.JournalAbbreviationPreferences;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.protectedterms.ProtectedTermsLoader;
import org.jabref.logic.util.UpdateFieldPreferences;
import org.jabref.logic.util.io.AutoLinkPreferences;
import org.jabref.logic.xmp.XmpPreferences;
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

    XmpPreferences getXMPPreferences();

    AutoLinkPreferences getAutoLinkPreferences();

    Path getWorkingDir();

    void setWorkingDir(Path dir);

    OpenOfficePreferences getOpenOfficePreferences();

    void setOpenOfficePreferences(OpenOfficePreferences openOfficePreferences);

    PreviewPreferences getPreviewPreferences();

    Map<String, Set<Field>> getEntryEditorTabList();

    boolean getEnforceLegalKeys();

    Map<String, String> getCustomTabsNamesAndFields();

    void setCustomTabsNameAndFields(String name, String fields, int defNumber);

    void purgeSeries(String prefix, int number);

    void updateEntryEditorTabList();

    List<TemplateExporter> getCustomExportFormats(JournalAbbreviationLoader loader);

    void storeCustomExportFormats(List<TemplateExporter> exporters);

    LayoutFormatterPreferences getLayoutFormatterPreferences(JournalAbbreviationLoader loader);

    UpdateFieldPreferences getUpdateFieldPreferences();

    ImportFormatPreferences getImportFormatPreferences();

    boolean isKeywordSyncEnabled();

    SavePreferences loadForExportFromPreferences();

    String getExportWorkingDirectory();

    void setExportWorkingDirectory(String layoutFileDirString);

    Charset getDefaultEncoding();

    void setDefaultEncoding(Charset encoding);

    String getUser();

    String getTheme();

    SaveOrderConfig loadExportSaveOrder();

    void storeExportSaveOrder(SaveOrderConfig config);

    boolean shouldWarnAboutDuplicatesForImport();

    void setShouldWarnAboutDuplicatesForImport(boolean value);

    void saveCustomEntryTypes();

    boolean getAllowIntegerEdition();

    EntryEditorPreferences getEntryEditorPreferences();

    List<BibEntryType> loadBibEntryTypes(BibDatabaseMode mode);
}
