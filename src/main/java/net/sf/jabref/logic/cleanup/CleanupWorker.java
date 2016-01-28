/*  Copyright (C) 2003-2015 JabRef contributors.
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
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;

public class CleanupWorker {

    private final CleanupPreset preset;
    private final List<String> paths;
    private final BibDatabase database;
    private int unsuccessfulRenames;

    public CleanupWorker(CleanupPreset preset) {
        this(preset, Collections.emptyList());
    }

    public CleanupWorker(CleanupPreset preset, List<String> paths) {
        this(preset, paths, null);
    }

    public CleanupWorker(CleanupPreset preset, List<String> paths, BibDatabase database) {
        this.preset = Objects.requireNonNull(preset);
        this.paths = Objects.requireNonNull(paths);
        this.database = database;
    }

    public int getUnsuccessfulRenames() {
        return unsuccessfulRenames;
    }

    public List<FieldChange> cleanup(BibEntry entry) {
        Objects.requireNonNull(entry);

        ArrayList<CleanupJob> jobs = determineCleanupActions();

        ArrayList<FieldChange> changes = new ArrayList<>();
        for (CleanupJob job : jobs) {
            changes.addAll(job.cleanup(entry));
        }

        return changes;
    }

    private ArrayList<CleanupJob> determineCleanupActions() {
        ArrayList<CleanupJob> jobs = new ArrayList<>();

        if (preset.isCleanUpUpgradeExternalLinks()) {
            jobs.add(new UpgradePdfPsToFileCleanup(Arrays.asList("pdf", "ps")));
        }
        if (preset.isCleanUpSuperscripts()) {
            jobs.add(new FormatterCleanup(BibtexFieldFormatters.SUPERSCRIPTS));
        }
        if (preset.isCleanUpDOI()) {
            jobs.add(new DoiCleanup());
        }
        if (preset.isCleanUpMonth()) {
            jobs.add(FieldFormatterCleanup.MONTH);
        }
        if (preset.isCleanUpPageNumbers()) {
            jobs.add(FieldFormatterCleanup.PAGE_NUMBERS);
        }
        if (preset.isCleanUpDate()) {
            jobs.add(FieldFormatterCleanup.DATES);
        }
        if (preset.isFixFileLinks()) {
            jobs.add(new FileLinksCleanup());
        }
        if (preset.isMakePathsRelative()) {
            jobs.add(new RelativePathsCleanup(paths));
        }
        if (preset.isRenamePDF()) {
            RenamePdfCleanup cleaner = new RenamePdfCleanup(paths, preset.isRenamePdfOnlyRelativePaths(), database);
            jobs.add(cleaner);
            unsuccessfulRenames += cleaner.getUnsuccessfulRenames();
        }
        if (preset.isConvertHTMLToLatex()) {
            jobs.add(FieldFormatterCleanup.TITLE_HTML);
        }
        if (preset.isConvertUnits()) {
            jobs.add(FieldFormatterCleanup.TITLE_UNITS);
        }
        if (preset.isConvertCase()) {
            jobs.add(FieldFormatterCleanup.TITLE_CASE);
        }
        if (preset.isConvertLaTeX()) {
            jobs.add(FieldFormatterCleanup.TITLE_LATEX);
        }
        if (preset.isConvertUnicodeToLatex()) {
            jobs.add(new UnicodeCleanup());
        }
        if (preset.isConvertToBiblatex()) {
            jobs.add(new BiblatexCleanup());
        }
        return jobs;
    }
}
