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
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import net.sf.jabref.logic.FieldChange;
import net.sf.jabref.logic.formatter.BibtexFieldFormatters;
import net.sf.jabref.logic.journals.JournalAbbreviationRepository;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;

public class CleanupWorker {

    private final CleanupPreset preset;
    private final List<String> paths;
    private final BibDatabase database;
    private final JournalAbbreviationRepository repository;
    private int unsuccessfulRenames;


    /**
     * This constructor is only used by CleanupWorkerTest. Therefore, the visibility is restricted.
     */
    CleanupWorker(CleanupPreset preset) {
        this(preset, Collections.emptyList());
    }

    /**
     * This constructor is only used by CleanupWorkerTest. Therefore, the visibility is restricted.
     */
    CleanupWorker(CleanupPreset preset, List<String> paths) {
        this(preset, paths, null, null);
    }

    public CleanupWorker(CleanupPreset preset, List<String> paths, BibDatabase database,
            JournalAbbreviationRepository repository) {
        this.preset = Objects.requireNonNull(preset);
        this.paths = Objects.requireNonNull(paths);
        this.database = database;
        this.repository = repository;
    }

    public int getUnsuccessfulRenames() {
        return unsuccessfulRenames;
    }

    public List<FieldChange> cleanup(BibEntry entry) {
        Objects.requireNonNull(entry);

        List<CleanupJob> jobs = determineCleanupActions();

        List<FieldChange> changes = new ArrayList<>();
        for (CleanupJob job : jobs) {
            changes.addAll(job.cleanup(entry));
        }

        return changes;
    }

    private List<CleanupJob> determineCleanupActions() {
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
        if (preset.isMakePathsRelative()) {
            jobs.add(new RelativePathsCleanup(paths));
        }
        if (preset.isRenamePDF()) {
            RenamePdfCleanup cleaner = new RenamePdfCleanup(paths, preset.isRenamePdfOnlyRelativePaths(), database,
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

        jobs.addAll(preset.getFormatterCleanups().getConfiguredActions());

        return jobs;
    }
}
