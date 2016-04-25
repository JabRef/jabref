/*  Copyright (C) 2003-2016 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.
    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/

package net.sf.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import net.sf.jabref.BibDatabaseContext;
import net.sf.jabref.logic.FieldChange;
import net.sf.jabref.logic.formatter.BibtexFieldFormatters;
import net.sf.jabref.logic.journals.JournalAbbreviationRepository;
import net.sf.jabref.model.entry.BibEntry;

public class CleanupWorker {

    private final BibDatabaseContext databaseContext;
    private final JournalAbbreviationRepository repository;
    private int unsuccessfulRenames;

    public CleanupWorker(BibDatabaseContext databaseContext, JournalAbbreviationRepository repository) {
        this.databaseContext = databaseContext;
        this.repository = repository;
    }

    public int getUnsuccessfulRenames() {
        return unsuccessfulRenames;
    }

    public List<FieldChange> cleanup(CleanupPreset preset, BibEntry entry) {
        Objects.requireNonNull(preset);
        Objects.requireNonNull(entry);

        List<CleanupJob> jobs = determineCleanupActions(preset);

        List<FieldChange> changes = new ArrayList<>();
        for (CleanupJob job : jobs) {
            changes.addAll(job.cleanup(entry));
        }

        return changes;
    }

    private List<CleanupJob> determineCleanupActions(CleanupPreset preset) {
        List<CleanupJob> jobs = new ArrayList<>();

        if (preset.isCleanUpUpgradeExternalLinks()) {
            jobs.add(new UpgradePdfPsToFileCleanup(Arrays.asList("pdf", "ps")));
        }
        if (preset.isCleanUpSuperscripts()) {
            jobs.add(new FormatterCleanup(BibtexFieldFormatters.ORDINALS_TO_LATEX_SUPERSCRIPT));
        }
        if (preset.isCleanUpDOI()) {
            jobs.add(new DoiCleanup());
        }
        if (preset.isFixFileLinks()) {
            jobs.add(new FileLinksCleanup());
        }
        if (preset.isMovePDF()) {
            jobs.add(new MoveFilesCleanup(databaseContext));
        }
        if (preset.isMakePathsRelative()) {
            jobs.add(new RelativePathsCleanup(databaseContext));
        }
        if (preset.isRenamePDF()) {
            RenamePdfCleanup cleaner = new RenamePdfCleanup(preset.isRenamePdfOnlyRelativePaths(), databaseContext,
                    repository);
            jobs.add(cleaner);
            unsuccessfulRenames += cleaner.getUnsuccessfulRenames();
        }
        if (preset.isConvertUnicodeToLatex()) {
            jobs.add(new UnicodeCleanup());
        }
        if (preset.isConvertToBiblatex()) {
            jobs.add(new BiblatexCleanup());
        }

        if(preset.getFormatterCleanups().isEnabled()) {
            jobs.addAll(preset.getFormatterCleanups().getConfiguredActions());
        }

        return jobs;
    }
}
