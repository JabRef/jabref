package org.jabref.gui.libraryproperties;

import java.util.List;

import org.jabref.gui.libraryproperties.constants.ConstantsPropertiesView;
import org.jabref.gui.libraryproperties.contentselectors.ContentSelectorView;
import org.jabref.gui.libraryproperties.general.GeneralPropertiesView;
import org.jabref.gui.libraryproperties.git.GitPropertiesView;
import org.jabref.gui.libraryproperties.keypattern.KeyPatternPropertiesView;
import org.jabref.gui.libraryproperties.preamble.PreamblePropertiesView;
import org.jabref.gui.libraryproperties.saving.SavingPropertiesView;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.undo.UndoManager;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.undo.UndoableMetaDataChange;

public class LibraryPropertiesViewModel {

    private final List<PropertiesTab> propertiesTabs;
    private final BibDatabaseContext databaseContext;
    private final UndoManager undoManager;

    public LibraryPropertiesViewModel(BibDatabaseContext databaseContext, UndoManager undoManager) {
        this(databaseContext, undoManager, List.of(
                new GeneralPropertiesView(databaseContext),
                new SavingPropertiesView(databaseContext),
                new KeyPatternPropertiesView(databaseContext),
                new ConstantsPropertiesView(databaseContext),
                new ContentSelectorView(databaseContext),
                new PreamblePropertiesView(databaseContext),
                new GitPropertiesView(databaseContext)));
    }

    /// For tests, which cannot build the tab views without a JavaFX toolkit.
    LibraryPropertiesViewModel(BibDatabaseContext databaseContext, UndoManager undoManager, List<PropertiesTab> propertiesTabs) {
        this.databaseContext = databaseContext;
        this.undoManager = undoManager;
        this.propertiesTabs = propertiesTabs;
    }

    /// Fills the tabs from the library's settings as they stand. The copy the tabs write to is
    /// made in [#storeAllSettings], so that it holds whatever the library has when the dialog is
    /// accepted rather than what it had when the dialog was opened.
    public void setValues() {
        for (PropertiesTab propertiesTab : propertiesTabs) {
            propertiesTab.setValues(databaseContext.getMetaData());
        }
    }

    /// Stores every tab as one undo step.
    ///
    /// The tabs write into a copy of the library's settings, and the step installs that copy. Two
    /// things follow from the copy. The dialog reaches the library through one door — the change
    /// record — instead of through a dozen setters, so the library sees one change, correctly
    /// described as journalled, rather than one event per setting that nothing can attribute. And
    /// the settings are recorded as the pair of states they went between, which is the only shape
    /// available: seven tabs write straight into the metadata, so there is no per-setting change
    /// to collect.
    ///
    /// What the tabs record themselves — the preamble, the string constants, the entries the
    /// keyword separator migration rewrites — joins the same step, since those are not settings
    /// and the block collects whatever is recorded inside it.
    // [impl->req~logic.undo.library-settings-recorded~1]
    public void storeAllSettings() {
        MetaData before = MetaData.copyOf(databaseContext.getMetaData());
        MetaData edited = MetaData.copyOf(databaseContext.getMetaData());
        undoManager.addEdit(Localization.lang("Change library settings"), edit -> {
            for (PropertiesTab propertiesTab : propertiesTabs) {
                propertiesTab.storeSettings(edited);
            }
            // Applied through the record rather than written first and described afterwards, so
            // that the one write the library sees is the one the journal can take back. Nothing
            // is recorded, and nothing is written, when no setting was touched.
            if (!before.equals(edited)) {
                edit.applyEdit(new UndoableMetaDataChange(databaseContext, before, edited));
            }
        });
    }

    public boolean validateAllSettings() {
        for (PropertiesTab propertiesTab : propertiesTabs) {
            if (!propertiesTab.validateSettings()) {
                return false;
            }
        }
        return true;
    }

    public List<PropertiesTab> getPropertiesTabs() {
        return propertiesTabs;
    }
}
