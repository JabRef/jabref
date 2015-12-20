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

public class CleanupPreset {

    public static final CleanupPreset DEFAULT;

    static {
        DEFAULT = new CleanupPreset();
        DEFAULT.setCleanUpSuperscripts(true);
        DEFAULT.setCleanUpDOI(true);
        DEFAULT.setCleanUpMonth(true);
        DEFAULT.setCleanUpPageNumbers(true);
        DEFAULT.setCleanUpDate(true);
        DEFAULT.setMakePathsRelative(true);
        DEFAULT.setRenamePDF(true);
        DEFAULT.setConvertHTMLToLatex(true);
        DEFAULT.setConvertCase(true);
        DEFAULT.setConvertLaTeX(true);
        DEFAULT.setConvertUnits(true);
        DEFAULT.setConvertUnicodeToLatex(true);
        DEFAULT.setFixFileLinks(true);
    }


    private boolean cleanUpSuperscripts;
    private boolean cleanUpDOI;
    private boolean cleanUpMonth;
    private boolean cleanUpPageNumbers;
    private boolean cleanUpDate;
    private boolean cleanUpUpgradeExternalLinks;
    private boolean makePathsRelative;
    private boolean renamePDF;
    private boolean renamePdfOnlyRelativePaths;
    private boolean convertHTMLToLatex;
    private boolean convertCase;
    private boolean convertLaTeX;
    private boolean convertUnits;
    private boolean convertUnicodeToLatex;
    private boolean convertToBiblatex;
    private boolean fixFileLinks;


    public boolean isCleanUpSuperscripts() {
        return cleanUpSuperscripts;
    }

    /**
     * Converts the text in 1st, 2nd, ... to real superscripts by wrapping in \textsuperscript{st}, ...
     */
    public void setCleanUpSuperscripts(boolean cleanUpSuperscripts) {
        this.cleanUpSuperscripts = cleanUpSuperscripts;
    }

    /**
     * Removes the http://... for each DOI. Moves DOIs from URL and NOTE filed to DOI field.
     */
    public boolean isCleanUpDOI() {
        return cleanUpDOI;
    }

    public void setCleanUpDOI(boolean cleanUpDOI) {
        this.cleanUpDOI = cleanUpDOI;
    }

    public boolean isCleanUpMonth() {
        return cleanUpMonth;
    }

    public void setCleanUpMonth(boolean cleanUpMonth) {
        this.cleanUpMonth = cleanUpMonth;
    }

    public boolean isCleanUpPageNumbers() {
        return cleanUpPageNumbers;
    }

    public void setCleanUpPageNumbers(boolean cleanUpPageNumbers) {
        this.cleanUpPageNumbers = cleanUpPageNumbers;
    }

    public boolean isCleanUpDate() {
        return cleanUpDate;
    }

    /**
     * Format dates correctly (yyyy-mm-dd or yyyy-mm)
     */
    public void setCleanUpDate(boolean cleanUpDate) {
        this.cleanUpDate = cleanUpDate;
    }

    /**
     * Collects file links from the pdf or ps field, and adds them to the list contained in the file field.
     */
    public boolean isCleanUpUpgradeExternalLinks() {
        return cleanUpUpgradeExternalLinks;
    }

    public void setCleanUpUpgradeExternalLinks(boolean cleanUpUpgradeExternalLinks) {
        this.cleanUpUpgradeExternalLinks = cleanUpUpgradeExternalLinks;
    }

    public boolean isMakePathsRelative() {
        return makePathsRelative;
    }

    public void setMakePathsRelative(boolean makePathsRelative) {
        this.makePathsRelative = makePathsRelative;
    }

    public boolean isRenamePDF() {
        return renamePDF;
    }

    public void setRenamePDF(boolean renamePDF) {
        this.renamePDF = renamePDF;
    }

    public boolean isConvertHTMLToLatex() {
        return convertHTMLToLatex;
    }

    /**
     * Converts HTML code to LaTeX code
     */
    public void setConvertHTMLToLatex(boolean convertHTMLToLatex) {
        this.convertHTMLToLatex = convertHTMLToLatex;
    }

    public boolean isConvertCase() {
        return convertCase;
    }

    /**
     * Adds curly brackets {} around keywords
     */
    public void setConvertCase(boolean convertCase) {
        this.convertCase = convertCase;
    }

    public boolean isConvertLaTeX() {
        return convertLaTeX;
    }


    public void setConvertLaTeX(boolean convertLaTeX) {
        this.convertLaTeX = convertLaTeX;
    }

    public boolean isConvertUnits() {
        return convertUnits;
    }

    public void setConvertUnits(boolean convertUnits) {
        this.convertUnits = convertUnits;
    }

    /**
     * Converts Unicode characters to LaTeX code
     */
    public boolean isConvertUnicodeToLatex() {
        return convertUnicodeToLatex;
    }

    public void setConvertUnicodeToLatex(boolean convertUnicodeToLatex) {
        this.convertUnicodeToLatex = convertUnicodeToLatex;
    }

    /**
     * Converts to BibLatex format
     */
    public boolean isConvertToBiblatex() {
        return convertToBiblatex;
    }

    public void setConvertToBiblatex(boolean convertToBiblatex) {
        this.convertToBiblatex = convertToBiblatex;
    }

    public boolean isRenamePdfOnlyRelativePaths() {
        return renamePdfOnlyRelativePaths;
    }

    public void setRenamePdfOnlyRelativePaths(boolean renamePdfOnlyRelativePaths) {
        this.renamePdfOnlyRelativePaths = renamePdfOnlyRelativePaths;
    }

    public boolean isFixFileLinks() {
        return fixFileLinks;
    }

    public void setFixFileLinks(boolean fixFileLinks) {
        this.fixFileLinks = fixFileLinks;
    }

}
