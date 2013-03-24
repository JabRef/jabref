/*  Copyright (C) 2003-2011 JabRef contributors.
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
package net.sf.jabref.util;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class containing method(s) for normalizing author lists to BibTeX format.
 */
public class NameListNormalizer {

    static Pattern lastFF = Pattern.compile("(\\p{javaUpperCase}[\\p{javaLowerCase}]+) (\\p{javaUpperCase}+)");
    static Pattern lastFdotF = Pattern.compile("(\\p{javaUpperCase}[\\p{javaLowerCase}]+) ([\\. \\p{javaUpperCase}]+)");
    static Pattern FFlast = Pattern.compile("(\\p{javaUpperCase}+) (\\p{javaUpperCase}[\\p{javaLowerCase}]+)");
    static Pattern FdotFlast = Pattern.compile("([\\. \\p{javaUpperCase}]+) (\\p{javaUpperCase}[\\p{javaLowerCase}]+)");
    static Pattern singleName = Pattern.compile("(\\p{javaUpperCase}[\\p{javaLowerCase}]*)");

    /*public static void main(String[] args) {
        normalizeAuthorList("Staci D. Bilbo and Smith SH and Jaclyn M Schwarz");
        //System.out.println(normalizeAuthorList("Ølver MA"));
        //System.out.println(normalizeAuthorList("Ølver MA, GG Øie, Øie GG, Alfredsen JÅÅ, Jo Alfredsen, Olsen Y.Y. and Olsen Y. Y."));
        //System.out.println(normalizeAuthorList("Ølver MA, GG Øie, Øie GG, Alfredsen JÅÅ, Jo Alfredsen, Olsen Y.Y., Olsen Y. Y."));
        //System.out.println(normalizeAuthorList("Alver, Morten and Alver, Morten O and Alfredsen, JA and Olsen, Y.Y."));
        //System.out.println(normalizeAuthorList("Alver, MA; Alfredsen, JA; Olsen Y.Y."));
    }*/

    public static String normalizeAuthorList(String in){
        boolean andSep = false, semicolonSep = false, commaSep = false;
        String author;
        String[] authors = in.split("( |,)and ",-1);
        if (authors.length > 1)
            andSep = true;
        else {
            /*
            If there are no "and" separators in the original string, we assume it either means that
            the author list is comma or semicolon separated or that it contains only a single name.
            If there is a semicolon, we go by that. If not, we assume commas, and count the parts
            separated by commas to determine which it is.
            */
            String[] a2 = in.split("; ");
            if (a2.length > 1) {
                semicolonSep = true;
                authors = a2;
            }
            else {
                a2 = in.split(", ");
                if (a2.length > 3) { // Probably more than a single author, so we split by commas.
                    commaSep = true;
                    authors = a2;
                } else {
                    if (a2.length == 3) {
                        // This could be a BibTeX formatted name containing a Jr particle,
                        // e.g. Smith, Jr., Peter
                        // We check if the middle part is <= 3 characters. If not, we assume we are
                        // dealing with three authors.
                        if (a2[1].length() > 3)
                            authors = a2;
                    }
                }
            }
        }

        // Remove leading and trailing whitespaces from each name:
        for (int i = 0; i < authors.length; i++){
            authors[i] = authors[i].trim();
        }

        // If we found an and separator, there could possibly be semicolon or
        // comma separation before the last separator. If there are two or more
        // and separators, we can dismiss this possibility.
        // If there is only a single and separator, check closer:
        if(andSep && (authors.length == 2)){
            // Check if the first part is semicolon separated:
            String[] semiSep = authors[0].split("; ");
            if (semiSep.length > 1) {
                // Ok, it looks like this is the case. Use separation by semicolons:
                String[] newAuthors = new String[1+semiSep.length];
                for (int i=0; i<semiSep.length; i++) {
                    newAuthors[i] = semiSep[i].trim();
                }
                newAuthors[semiSep.length] = authors[1];
                authors = newAuthors;
            }
            else {
                // Check if there is a comma in the last name. If so, we can assume that comma
                // is not used to separate the names:
                boolean lnfn = (authors[1].indexOf(",") > 0);
                if (!lnfn) {
                    String[] cmSep = authors[0].split(", ");
                    if (cmSep.length > 1) {
                        // This means that the last name doesn't contain a comma, but the first
                        // one contains one or more. This indicates that the names leading up to
                        // the single "and" are comma separated:
                        String[] newAuthors = new String[1+cmSep.length];
                        for (int i=0; i<cmSep.length; i++) {
                            newAuthors[i] = cmSep[i].trim();
                        }
                        newAuthors[cmSep.length] = authors[1];
                        authors = newAuthors;
                    }

                }
            }
        }

        StringBuilder sb = new StringBuilder();
        for (int i=0; i<authors.length; i++) {
            String norm = normalizeName(authors[i]);
            sb.append(norm);
            if (i < authors.length-1)
                sb.append(" and ");
        }
        return sb.toString();
    }


    public static String normalizeName(String name) {
        Matcher m = lastFF.matcher(name);
        if (m.matches()) {
            String initials = m.group(2);
            StringBuilder sb = new StringBuilder(m.group(1));
            sb.append(", ");
            for (int i=0; i<initials.length(); i++) {
                sb.append(initials.charAt(i));
                sb.append('.');
                if (i < initials.length()-1)
                    sb.append(' ');
            }
            return sb.toString();
        }
        m = lastFdotF.matcher(name);
        if (m.matches()) {
            String initials = m.group(2).replaceAll("[\\. ]+", "");
            StringBuilder sb = new StringBuilder(m.group(1));
            sb.append(", ");
            for (int i=0; i<initials.length(); i++) {
                sb.append(initials.charAt(i));
                sb.append('.');
                if (i < initials.length()-1)
                    sb.append(' ');
            }
            return sb.toString();
        }

        m = FFlast.matcher(name);
        if (m.matches()) {
            String initials = m.group(1);
            StringBuilder sb = new StringBuilder(m.group(2));
            sb.append(", ");
            for (int i=0; i<initials.length(); i++) {
                sb.append(initials.charAt(i));
                sb.append('.');
                if (i < initials.length()-1)
                    sb.append(' ');
            }
            return sb.toString();
        }
        m = FdotFlast.matcher(name);
        if (m.matches()) {
            String initials = m.group(1).replaceAll("[\\. ]+", "");
            StringBuilder sb = new StringBuilder(m.group(2));
            sb.append(", ");
            for (int i=0; i<initials.length(); i++) {
                sb.append(initials.charAt(i));
                sb.append('.');
                if (i < initials.length()-1)
                    sb.append(' ');
            }
            return sb.toString();
        }

        if (name.indexOf(',') >= 0) {
            // Name contains comma
            int index = name.lastIndexOf(',');
            // If the comma is at the end of the name, just remove it to prevent index error:
            if (index == name.length() - 1)
                name = name.substring(0, name.length()-1);

            StringBuilder sb = new StringBuilder(name.substring(0, index));
            sb.append(", ");
            // Check if the remainder is a single name:
            String fName = name.substring(index+1).trim();
            String[] fParts = fName.split(" ");
            if (fParts.length > 1) {
                // Multiple parts. Add all of them, and add a dot if they are single letter parts:
                for (int i=0; i<fParts.length; i++) {
                    if (fParts[i].length() == 1)
                        sb.append(fParts[i]+".");
                    else sb.append(fParts[i]);
                    if (i < fParts.length-1)
                        sb.append(" ");
                }
            } else {
                // Only a single part. Check if it looks like a name or initials:
                Matcher m2 = singleName.matcher(fParts[0]);
                if (m2.matches())
                    sb.append(fParts[0]);
                else {
                    // It looks like initials.
                    String initials = fParts[0].replaceAll("[\\.]+", "");
                    for (int i=0; i<initials.length(); i++) {
                        sb.append(initials.charAt(i));
                        sb.append('.');
                        if (i < initials.length()-1)
                            sb.append(' ');
                    }
                }

            }
            return sb.toString();
        } else {
            // Name doesn't contain comma
            String[] parts = name.split(" +");
            boolean allNames = true;
            for (int i = 0; i < parts.length; i++) {
                m = singleName.matcher(parts[i]);
                if (!m.matches()) {
                    allNames = false;
                    break;
                }
            }
            if (allNames) {
                // Looks like a name written in full with first name first.
                // Change into last name first format:
                StringBuilder sb = new StringBuilder(parts[parts.length-1]);
                if (parts.length > 1) {
                    sb.append(",");
                    for (int i = 0; i < parts.length-1; i++) {
                        sb.append(" "+parts[i]);
                        if (parts[i].length() == 1)
                            sb.append(".");
                    }
                }
                return sb.toString();
            }
        }

        return name;
    }
}
