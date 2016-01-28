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

import net.sf.jabref.JabRefPreferences;

import java.util.EnumSet;

public class CleanupPreset {

    private final EnumSet<CleanupStep> activeJobs;

    public CleanupPreset(EnumSet<CleanupStep> activeJobs) {
        this.activeJobs = activeJobs;
    }

    public CleanupPreset(CleanupStep activeJob) {
        this(EnumSet.of(activeJob));
    }

    public static CleanupPreset loadFromPreferences(JabRefPreferences preferences) {

        EnumSet<CleanupStep> activeJobs = EnumSet.noneOf(CleanupStep.class);

        if (preferences.getBoolean(JabRefPreferences.CLEANUP_SUPERSCRIPTS)) {
            activeJobs.add(CleanupStep.CLEAN_UP_SUPERSCRIPTS);
        }
        if (preferences.getBoolean(JabRefPreferences.CLEANUP_DOI)) {
            activeJobs.add(CleanupStep.CLEAN_UP_DOI);
        }
        if (preferences.getBoolean(JabRefPreferences.CLEANUP_MONTH)) {
            activeJobs.add(CleanupStep.CLEAN_UP_MONTH);
        }
        if (preferences.getBoolean(JabRefPreferences.CLEANUP_PAGE_NUMBERS)) {
            activeJobs.add(CleanupStep.CLEAN_UP_PAGE_NUMBERS);
        }
        if (preferences.getBoolean(JabRefPreferences.CLEANUP_DATE)) {
            activeJobs.add(CleanupStep.CLEAN_UP_DATE);
        }
        if (preferences.getBoolean(JabRefPreferences.CLEANUP_MAKE_PATHS_RELATIVE)) {
            activeJobs.add(CleanupStep.MAKE_PATHS_RELATIVE);
        }
        if (preferences.getBoolean(JabRefPreferences.CLEANUP_RENAME_PDF)) {
            activeJobs.add(CleanupStep.RENAME_PDF);
        }
        if (preferences.getBoolean(JabRefPreferences.CLEANUP_RENAME_PDF_ONLY_RELATIVE_PATHS)) {
            activeJobs.add(CleanupStep.RENAME_PDF_ONLY_RELATIVE_PATHS);
        }
        if (preferences.getBoolean(JabRefPreferences.CLEANUP_UPGRADE_EXTERNAL_LINKS)) {
            activeJobs.add(CleanupStep.CLEAN_UP_UPGRADE_EXTERNAL_LINKS);
        }
        if (preferences.getBoolean(JabRefPreferences.CLEANUP_HTML)) {
            activeJobs.add(CleanupStep.CONVERT_HTML_TO_LATEX);
        }
        if (preferences.getBoolean(JabRefPreferences.CLEANUP_CASE)) {
            activeJobs.add(CleanupStep.CONVERT_CASE);
        }
        if (preferences.getBoolean(JabRefPreferences.CLEANUP_LATEX)) {
            activeJobs.add(CleanupStep.CONVERT_LATEX);
        }
        if (preferences.getBoolean(JabRefPreferences.CLEANUP_UNITS)) {
            activeJobs.add(CleanupStep.CONVERT_UNITS);
        }
        if (preferences.getBoolean(JabRefPreferences.CLEANUP_UNICODE)) {
            activeJobs.add(CleanupStep.CONVERT_UNICODE_TO_LATEX);
        }
        if (preferences.getBoolean(JabRefPreferences.CLEANUP_CONVERT_TO_BIBLATEX)) {
            activeJobs.add(CleanupStep.CONVERT_TO_BIBLATEX);
        }
        if (preferences.getBoolean(JabRefPreferences.CLEANUP_FIX_FILE_LINKS)) {
            activeJobs.add(CleanupStep.FIX_FILE_LINKS);
        }

        return new CleanupPreset(activeJobs);
    }

    public boolean isCleanUpUpgradeExternalLinks() {
        return isActive(CleanupStep.CLEAN_UP_UPGRADE_EXTERNAL_LINKS);
    }

    public boolean isCleanUpSuperscripts() {
        return isActive(CleanupStep.CLEAN_UP_SUPERSCRIPTS);
    }

    public boolean isCleanUpDOI() {
        return isActive(CleanupStep.CLEAN_UP_DOI);
    }

    public boolean isCleanUpMonth() {
        return isActive(CleanupStep.CLEAN_UP_MONTH);
    }

    public boolean isCleanUpPageNumbers() {
        return isActive(CleanupStep.CLEAN_UP_PAGE_NUMBERS);
    }

    public boolean isCleanUpDate() {
        return isActive(CleanupStep.CLEAN_UP_DATE);
    }

    public boolean isFixFileLinks() {
        return isActive(CleanupStep.FIX_FILE_LINKS);
    }

    public boolean isMakePathsRelative() {
        return isActive(CleanupStep.MAKE_PATHS_RELATIVE);
    }

    public boolean isRenamePDF() {
        return isActive(CleanupStep.RENAME_PDF) || isActive(CleanupStep.RENAME_PDF_ONLY_RELATIVE_PATHS);
    }

    public boolean isConvertHTMLToLatex() {
        return isActive(CleanupStep.CONVERT_HTML_TO_LATEX);
    }

    public boolean isConvertUnits() {
        return isActive(CleanupStep.CONVERT_UNITS);
    }

    public boolean isConvertCase() {
        return isActive(CleanupStep.CONVERT_CASE);
    }

    public boolean isConvertLaTeX() {
        return isActive(CleanupStep.CONVERT_LATEX);
    }

    public boolean isConvertUnicodeToLatex() {
        return isActive(CleanupStep.CONVERT_UNICODE_TO_LATEX);
    }

    public boolean isConvertToBiblatex() {
        return isActive(CleanupStep.CONVERT_TO_BIBLATEX);
    }

    public boolean isRenamePdfOnlyRelativePaths() {
        return isActive(CleanupStep.RENAME_PDF_ONLY_RELATIVE_PATHS);
    }

    public void storeInPreferences(JabRefPreferences preferences) {

        preferences.putBoolean(JabRefPreferences.CLEANUP_SUPERSCRIPTS, isActive(CleanupStep.CLEAN_UP_SUPERSCRIPTS));
        preferences.putBoolean(JabRefPreferences.CLEANUP_DOI, isActive(CleanupStep.CLEAN_UP_DOI));
        preferences.putBoolean(JabRefPreferences.CLEANUP_MONTH, isActive(CleanupStep.CLEAN_UP_MONTH));
        preferences.putBoolean(JabRefPreferences.CLEANUP_PAGE_NUMBERS, isActive(CleanupStep.CLEAN_UP_PAGE_NUMBERS));
        preferences.putBoolean(JabRefPreferences.CLEANUP_DATE, isActive(CleanupStep.CLEAN_UP_DATE));
        preferences.putBoolean(JabRefPreferences.CLEANUP_MAKE_PATHS_RELATIVE, isActive(CleanupStep.MAKE_PATHS_RELATIVE));
        preferences.putBoolean(JabRefPreferences.CLEANUP_RENAME_PDF, isActive(CleanupStep.RENAME_PDF));
        preferences.putBoolean(JabRefPreferences.CLEANUP_RENAME_PDF_ONLY_RELATIVE_PATHS,
                isActive(CleanupStep.RENAME_PDF_ONLY_RELATIVE_PATHS));
        preferences.putBoolean(JabRefPreferences.CLEANUP_UPGRADE_EXTERNAL_LINKS,
                isActive(CleanupStep.CLEAN_UP_UPGRADE_EXTERNAL_LINKS));
        preferences.putBoolean(JabRefPreferences.CLEANUP_HTML, isActive(CleanupStep.CONVERT_HTML_TO_LATEX));
        preferences.putBoolean(JabRefPreferences.CLEANUP_CASE, isActive(CleanupStep.CONVERT_CASE));
        preferences.putBoolean(JabRefPreferences.CLEANUP_LATEX, isActive(CleanupStep.CONVERT_LATEX));
        preferences.putBoolean(JabRefPreferences.CLEANUP_UNITS, isActive(CleanupStep.CONVERT_UNITS));
        preferences.putBoolean(JabRefPreferences.CLEANUP_UNICODE, isActive(CleanupStep.CONVERT_UNICODE_TO_LATEX));
        preferences.putBoolean(JabRefPreferences.CLEANUP_CONVERT_TO_BIBLATEX, isActive(CleanupStep.CONVERT_TO_BIBLATEX));
        preferences.putBoolean(JabRefPreferences.CLEANUP_FIX_FILE_LINKS, isActive(CleanupStep.FIX_FILE_LINKS));
    }

    private Boolean isActive(CleanupStep step) {
        return activeJobs.contains(step);
    }

    public enum CleanupStep {
        /**
         * Converts the text in 1st, 2nd, ... to real superscripts by wrapping in \textsuperscript{st}, ...
         */
        CLEAN_UP_SUPERSCRIPTS,
        /**
         * Removes the http://... for each DOI. Moves DOIs from URL and NOTE filed to DOI field.
         */
        CLEAN_UP_DOI,
        CLEAN_UP_MONTH,
        CLEAN_UP_PAGE_NUMBERS,
        /**
         * Format dates correctly (yyyy-mm-dd or yyyy-mm)
         */
        CLEAN_UP_DATE,
        MAKE_PATHS_RELATIVE,
        RENAME_PDF,
        RENAME_PDF_ONLY_RELATIVE_PATHS,
        /**
         * Collects file links from the pdf or ps field, and adds them to the list contained in the file field.
         */
        CLEAN_UP_UPGRADE_EXTERNAL_LINKS,
        /**
         * Converts HTML code to LaTeX code
         */
        CONVERT_HTML_TO_LATEX,
        /**
         * Adds curly brackets {} around keywords
         */
        CONVERT_CASE,
        CONVERT_LATEX,
        CONVERT_UNITS,
        /**
         * Converts Unicode characters to LaTeX code
         */
        CONVERT_UNICODE_TO_LATEX,
        /**
         * Converts to BibLatex format
         */
        CONVERT_TO_BIBLATEX,
        FIX_FILE_LINKS
    }
}
