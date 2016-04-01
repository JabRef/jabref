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
package net.sf.jabref.logic.formatter.bibtexfields;

import net.sf.jabref.logic.formatter.Formatter;
import net.sf.jabref.logic.l10n.Localization;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Formatter normalizing a list of person names to the BibTeX format.
 */
public class NormalizeNamesFormatter implements Formatter {
    private static final Pattern LAST_F_F = Pattern.compile("(\\p{javaUpperCase}[\\p{javaLowerCase}]+) (\\p{javaUpperCase}+)");
    private static final Pattern LAST_FDOT_F = Pattern.compile("(\\p{javaUpperCase}[\\p{javaLowerCase}]+) ([\\. \\p{javaUpperCase}]+)");
    private static final Pattern F_F_LAST = Pattern.compile("(\\p{javaUpperCase}+) (\\p{javaUpperCase}[\\p{javaLowerCase}]+)");
    private static final Pattern FDOT_F_LAST = Pattern.compile("([\\. \\p{javaUpperCase}]+) (\\p{javaUpperCase}[\\p{javaLowerCase}]+)");
    private static final Pattern SINGLE_NAME = Pattern.compile("(\\p{javaUpperCase}[\\p{javaLowerCase}]*)");

    @Override
    public String getName() {
        return Localization.lang("Normalize names of persons");
    }

    @Override
    public String getKey() {
        return "normalize_names";
    }

    @Override
    public String format(String value) {
        boolean andSep = false;
        // String can contain newlines. Convert each to a space
        String noNewlineValue = value.replace("\n", " ");
        String[] authors = noNewlineValue.split("( |,)and ", -1);
        if (authors.length > 1) {
            andSep = true;
        } else {
            /*
            If there are no "and" separators in the original string, we assume it either means that
            the author list is comma or semicolon separated or that it contains only a single name.
            If there is a semicolon, we go by that. If not, we assume commas, and count the parts
            separated by commas to determine which it is.
            */
            String[] authors2 = noNewlineValue.split("; ");
            if (authors2.length > 1) {
                authors = authors2;
            } else {
                authors2 = noNewlineValue.split(", ");
                if (authors2.length > 3) { // Probably more than a single author, so we split by commas.
                    authors = authors2;
                } else {
                    if (authors2.length == 3) {
                        // This could be a BibTeX formatted name containing a Jr particle,
                        // e.g. Smith, Jr., Peter
                        // We check if the middle part is <= 3 characters. If not, we assume we are
                        // dealing with three authors.
                        if (authors2[1].length() > 3) {
                            authors = authors2;
                        }
                    }
                }
            }
        }

        // Remove leading and trailing whitespaces from each name:
        for (int i = 0; i < authors.length; i++) {
            authors[i] = authors[i].trim();
        }

        // If we found an and separator, there could possibly be semicolon or
        // comma separation before the last separator. If there are two or more
        // and separators, we can dismiss this possibility.
        // If there is only a single and separator, check closer:
        if (andSep && (authors.length == 2)) {
            // Check if the first part is semicolon separated:
            String[] semiSep = authors[0].split("; ");
            if (semiSep.length > 1) {
                // Ok, it looks like this is the case. Use separation by semicolons:
                String[] newAuthors = new String[1 + semiSep.length];
                for (int i = 0; i < semiSep.length; i++) {
                    newAuthors[i] = semiSep[i].trim();
                }
                newAuthors[semiSep.length] = authors[1];
                authors = newAuthors;
            } else {
                // Check if there is a comma in the last name. If so, we can assume that comma
                // is not used to separate the names:
                boolean lnfn = authors[1].indexOf(',') >= 1;
                if (!lnfn) {
                    String[] cmSep = authors[0].split(", ");
                    if (cmSep.length > 1) {
                        // This means that the last name doesn't contain a comma, but the first
                        // one contains one or more. This indicates that the names leading up to
                        // the single "and" are comma separated:
                        String[] newAuthors = new String[1 + cmSep.length];
                        for (int i = 0; i < cmSep.length; i++) {
                            newAuthors[i] = cmSep[i].trim();
                        }
                        newAuthors[cmSep.length] = authors[1];
                        authors = newAuthors;
                    }

                }
            }
        }

        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < authors.length; i++) {
            String norm = NormalizeNamesFormatter.normalizeName(authors[i]);
            stringBuilder.append(norm);
            if (i < (authors.length - 1)) {
                stringBuilder.append(" and ");
            }
        }
        return stringBuilder.toString();
    }

    @Override
    public String getDescription() {
        return Localization.lang("Normalizes lists of persons to the BibTeX standard.");
    }

    private static String normalizeName(String oldName) {
        String name = oldName;
        Matcher matcher = NormalizeNamesFormatter.LAST_F_F.matcher(name);
        if (matcher.matches()) {
            String initials = matcher.group(2);
            StringBuilder stringBuilder = new StringBuilder(matcher.group(1));
            stringBuilder.append(", ");
            fixInitials(initials, stringBuilder);
            return stringBuilder.toString();
        }
        matcher = NormalizeNamesFormatter.LAST_FDOT_F.matcher(name);
        if (matcher.matches()) {
            String initials = matcher.group(2).replaceAll("[\\. ]+", "");
            StringBuilder stringBuilder = new StringBuilder(matcher.group(1));
            stringBuilder.append(", ");
            fixInitials(initials, stringBuilder);
            return stringBuilder.toString();
        }

        matcher = NormalizeNamesFormatter.F_F_LAST.matcher(name);
        if (matcher.matches()) {
            String initials = matcher.group(1);
            StringBuilder stringBuilder = new StringBuilder(matcher.group(2));
            stringBuilder.append(", ");
            fixInitials(initials, stringBuilder);
            return stringBuilder.toString();
        }
        matcher = NormalizeNamesFormatter.FDOT_F_LAST.matcher(name);
        if (matcher.matches()) {
            String initials = matcher.group(1).replaceAll("[\\. ]+", "");
            StringBuilder stringBuilder = new StringBuilder(matcher.group(2));
            stringBuilder.append(", ");
            fixInitials(initials, stringBuilder);
            return stringBuilder.toString();
        }

        if (name.indexOf(',') >= 0) {
            // Name contains comma
            int index = name.lastIndexOf(',');
            // If the comma is at the end of the name, just remove it to prevent index error:
            if (index == (name.length() - 1)) {
                name = name.substring(0, name.length() - 1);
            }

            StringBuilder stringBuilder = new StringBuilder(name.substring(0, index));
            stringBuilder.append(", ");
            // Check if the remainder is a single name:
            String firstName = name.substring(index + 1).trim();
            String[] firstNameParts = firstName.split(" ");
            if (firstNameParts.length > 1) {
                // Multiple parts. Add all of them, and add a dot if they are single letter parts:
                for (int i = 0; i < firstNameParts.length; i++) {
                    if (firstNameParts[i].length() == 1) {
                        stringBuilder.append(firstNameParts[i]).append('.');
                    } else {
                        stringBuilder.append(firstNameParts[i]);
                    }
                    if (i < (firstNameParts.length - 1)) {
                        stringBuilder.append(' ');
                    }
                }
            } else {
                // Only a single part. Check if it looks like a name or initials:
                Matcher nameMatcher = NormalizeNamesFormatter.SINGLE_NAME.matcher(firstNameParts[0]);
                if (nameMatcher.matches()) {
                    stringBuilder.append(firstNameParts[0]);
                } else {
                    // It looks like initials.
                    String initials = firstNameParts[0].replaceAll("[\\.]+", "");
                    fixInitials(initials, stringBuilder);
                }

            }
            return stringBuilder.toString();
        } else {
            // Name doesn't contain comma
            String[] parts = name.split(" +");
            boolean allNames = true;
            for (String part : parts) {
                matcher = NormalizeNamesFormatter.SINGLE_NAME.matcher(part);
                if (!matcher.matches()) {
                    allNames = false;
                    break;
                }
            }
            if (allNames) {
                // Looks like a name written in full with first name first.
                // Change into last name first format:
                StringBuilder stringBuilder = new StringBuilder(parts[parts.length - 1]);
                if (parts.length > 1) {
                    stringBuilder.append(',');
                    for (int i = 0; i < (parts.length - 1); i++) {
                        stringBuilder.append(' ').append(parts[i]);
                        if (parts[i].length() == 1) {
                            stringBuilder.append('.');
                        }
                    }
                }
                return stringBuilder.toString();
            }
        }

        return name;
    }

    private static void fixInitials(final String initials, final StringBuilder stringBuilder) {
        for (int i = 0; i < initials.length(); i++) {
            stringBuilder.append(initials.charAt(i));
            stringBuilder.append('.');
            if (i < (initials.length() - 1)) {
                stringBuilder.append(' ');
            }
        }
    }
}
