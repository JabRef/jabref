package org.jabref.gui.git;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Path;

import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.exporter.AtomicFileWriter;
import org.jabref.logic.exporter.BibDatabaseWriter;
import org.jabref.logic.exporter.BibWriter;
import org.jabref.logic.exporter.SaveException;
import org.jabref.logic.exporter.SelfContainedSaveConfiguration;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.metadata.SelfContainedSaveOrder;

public final class GitMergeSaveSupport {

    private GitMergeSaveSupport() { }

    /**
     * Save the whole database to {@code file} exactly like the regular Save action does:
     * - Same AtomicFileWriter
     * - Same backup policy
     * - Same reformat-on-save and save order flags from preferences
     * - Same line separator
     *
     * This method throws the same SaveException/IOException profile as user-save.
     */
    public static void saveLikeUserSave(BibDatabaseContext context,
                                        Path file,
                                        GuiPreferences preferences) throws SaveException {
        // Keep this in sync with SaveDatabaseAction#saveDatabase(...)
        SelfContainedSaveConfiguration saveConfiguration =
                new SelfContainedSaveConfiguration(
                        new SelfContainedSaveOrder(), // use your default order or pass it in
                        false,
                        BibDatabaseWriter.SaveType.NORMAL,
                        preferences.getLibraryPreferences().shouldAlwaysReformatOnSave());

        Charset encoding = context.getMetaData().getEncoding().orElse(StandardCharsets.UTF_8);

        synchronized (context) {
            try (AtomicFileWriter fileWriter = new AtomicFileWriter(file, encoding, saveConfiguration.shouldMakeBackup())) {
                BibWriter bibWriter = new BibWriter(fileWriter, context.getDatabase().getNewLineSeparator());
                BibDatabaseWriter databaseWriter = new BibDatabaseWriter(
                        bibWriter,
                        saveConfiguration,
                        preferences.getFieldPreferences(),
                        preferences.getCitationKeyPatternPreferences(),
                        // NOTE: use the same entryTypesManager the GUI uses
                        org.jabref.model.entry.types.BibEntryTypesManager.getInstance()
                );

                databaseWriter.saveDatabase(context);

                if (fileWriter.hasEncodingProblems()) {
                    throw new SaveException(String.join("\n", fileWriter.getEncodingProblems()));
                }
            } catch (
                    UnsupportedCharsetException ex) {
                throw new SaveException("Character encoding '" + encoding.displayName() + "' is not supported.", ex);
            } catch (IOException ex) {
                throw new SaveException("Problems saving: " + ex, ex);
            }
        }
    }
}
