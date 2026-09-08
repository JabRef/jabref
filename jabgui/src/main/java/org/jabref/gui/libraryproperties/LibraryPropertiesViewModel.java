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

    public void setValues() {
        for (PropertiesTab propertiesTab : propertiesTabs) {
            propertiesTab.setValues();
        }
    }

    /// Stores every tab as one undo step.
    ///
    /// The settings themselves are recorded as a snapshot pair rather than field by field: the
    /// tabs write straight to the [MetaData] and there is no per-setting change to collect, while
    /// a pair of copies describes whatever the dialog did, however many tabs took part. What the
    /// tabs record themselves — the preamble, the string constants, the entries the keyword
    /// separator migration rewrites — joins the same step.
    // [impl->req~logic.undo.library-settings-recorded~1]
    public void storeAllSettings() {
        MetaData before = MetaData.copyOf(databaseContext.getMetaData());
        undoManager.addEdit(Localization.lang("Change library settings"), edit -> {
            for (PropertiesTab propertiesTab : propertiesTabs) {
                propertiesTab.storeSettings();
            }
            // Last, so that undoing puts the settings back before the changes the tabs recorded
            // are reverted on top of them. Nothing is recorded when the dialog is accepted
            // without a setting having been touched.
            if (!before.equals(databaseContext.getMetaData())) {
                edit.addEdit(new UndoableMetaDataChange(databaseContext, before, databaseContext.getMetaData()));
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
