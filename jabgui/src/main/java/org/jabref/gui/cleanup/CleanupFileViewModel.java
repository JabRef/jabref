package org.jabref.gui.cleanup;

import java.util.EnumSet;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import org.jabref.logic.cleanup.CleanupPreferences;

public class CleanupFileViewModel {

    public static final EnumSet<CleanupPreferences.CleanupStep> FILE_RELATED_JOBS = EnumSet.of(
            CleanupPreferences.CleanupStep.MOVE_PDF,
            CleanupPreferences.CleanupStep.MAKE_PATHS_RELATIVE,
            CleanupPreferences.CleanupStep.RENAME_PDF,
            CleanupPreferences.CleanupStep.RENAME_PDF_ONLY_RELATIVE_PATHS,
            CleanupPreferences.CleanupStep.CLEAN_UP_UPGRADE_EXTERNAL_LINKS,
            CleanupPreferences.CleanupStep.CLEAN_UP_DELETED_LINKED_FILES
    );

    public final BooleanProperty movePdfSelected = new SimpleBooleanProperty();
    public final BooleanProperty makePathsRelativeSelected = new SimpleBooleanProperty();
    public final BooleanProperty renamePdfSelected = new SimpleBooleanProperty();
    public final BooleanProperty renamePdfOnlyRelativeSelected = new SimpleBooleanProperty();
    public final BooleanProperty upgradeLinksSelected = new SimpleBooleanProperty();
    public final BooleanProperty deleteFilesSelected = new SimpleBooleanProperty();

    public final BooleanProperty movePdfEnabled = new SimpleBooleanProperty(true);

    public CleanupFileViewModel(CleanupPreferences preferences) {
        movePdfSelected.set(preferences.isActive(CleanupPreferences.CleanupStep.MOVE_PDF));
        makePathsRelativeSelected.set(preferences.isActive(CleanupPreferences.CleanupStep.MAKE_PATHS_RELATIVE));
        renamePdfSelected.set(preferences.isActive(CleanupPreferences.CleanupStep.RENAME_PDF));
        renamePdfOnlyRelativeSelected.set(preferences.isActive(CleanupPreferences.CleanupStep.RENAME_PDF_ONLY_RELATIVE_PATHS));
        upgradeLinksSelected.set(preferences.isActive(CleanupPreferences.CleanupStep.CLEAN_UP_UPGRADE_EXTERNAL_LINKS));
        deleteFilesSelected.set(preferences.isActive(CleanupPreferences.CleanupStep.CLEAN_UP_DELETED_LINKED_FILES));

        renamePdfSelected.addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                renamePdfOnlyRelativeSelected.set(false);
            }
        });
    }

    public EnumSet<CleanupPreferences.CleanupStep> getSelectedJobs() {
        EnumSet<CleanupPreferences.CleanupStep> activeJobs = EnumSet.noneOf(CleanupPreferences.CleanupStep.class);
        if (movePdfSelected.get()) {
            activeJobs.add(CleanupPreferences.CleanupStep.MOVE_PDF);
        }
        if (makePathsRelativeSelected.get()) {
            activeJobs.add(CleanupPreferences.CleanupStep.MAKE_PATHS_RELATIVE);
        }
        if (renamePdfSelected.get()) {
            if (renamePdfOnlyRelativeSelected.get()) {
                activeJobs.add(CleanupPreferences.CleanupStep.RENAME_PDF_ONLY_RELATIVE_PATHS);
            } else {
                activeJobs.add(CleanupPreferences.CleanupStep.RENAME_PDF);
            }
        }
        if (upgradeLinksSelected.get()) {
            activeJobs.add(CleanupPreferences.CleanupStep.CLEAN_UP_UPGRADE_EXTERNAL_LINKS);
        }
        if (deleteFilesSelected.get()) {
            activeJobs.add(CleanupPreferences.CleanupStep.CLEAN_UP_DELETED_LINKED_FILES);
        }
        return activeJobs;
    }
}
