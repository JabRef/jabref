package net.sf.jabref.util;

/* Mp3dings - manage mp3 meta-information
 * Copyright (C) 2003 Moritz Ringler
 * $Id$
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */


import java.util.regex.Pattern;
import java.util.regex.Matcher;

/** Class used to manage case changing
 * @author Moritz Ringler
 * @version $Revision$ ($Date$)
 */
public class CaseChanger{
    /** Lowercase */
    public final static int LOWER=0;
    /** Uppercase */
    public final static int UPPER=1;
    /** First letter of string uppercase */
    public final static int UPPER_FIRST=2;
    /** First letter of each word uppercase */
    public final static int UPPER_EACH_FIRST=3;
    private final static Matcher UF_MATCHER =
            //Pattern.compile("(?i)\\b\\w").matcher("");
        Pattern.compile("\\b\\w").matcher("");

    /* you can add more modes here */
    private final static int numModes=4;
    private final static String[] modeNames={"lower", "UPPER", "Upper first", "Upper Each First"};

    public CaseChanger(){
    }

    /** Gets the name of a case changing mode
     * @param mode by default one of LOWER, UPPER, UPPER_FIRST or
     * UPPER_EACH_FIRST
     */
    public String getModeName(int mode){
        return modeNames[mode];
    }

    /** Gets the names of all available case changing modes */
    public String[] getModeNames(){
        return modeNames;
    }

    /** Gets the number of available case changing modes */
    public int getNumModes(){
        return numModes;
    }

    /** Changes the case of the specified strings.
     *  wrapper for {@link #changeCase(String input, int mode)}
     * @see  #changeCase(String input, int mode)
     */
    public String[] changeCase(String[] input,int mode){
        int n=input.length;
        String[] output = new String[n];
        for(int i=0;i<n;i++){
            output[i]=changeCase(input[i], mode);
        }
        return output;
    }

    /** Changes the case of the specified string
     * @param input String to change
     * @param mode by default one of LOWER, UPPER, UPPER_FIRST or
     * UPPER_EACH_FIRST
     * @return casechanged string
     */
    public String changeCase(String input, int mode){
        switch (mode){
            case UPPER:     return input.toUpperCase();
            case LOWER:     return input.toLowerCase();
            case UPPER_FIRST:
                            String s = input.toLowerCase();
                            UF_MATCHER.reset(s);
                            if (UF_MATCHER.find()){
                                return UF_MATCHER.replaceFirst(UF_MATCHER.
                                        group(0).toUpperCase());
                            } else {
                                return input;
                            }
            case UPPER_EACH_FIRST:
                            s               = input.toLowerCase();
                            StringBuffer sb = new StringBuffer();
                            boolean found   = false;
                            UF_MATCHER.reset(s);
                            while (UF_MATCHER.find()) {
                                UF_MATCHER.appendReplacement(sb,
                                    UF_MATCHER.group(0).toUpperCase());
                                found = true;
                            }
                            if (found){
                                UF_MATCHER.appendTail(sb);
                                return sb.toString();
                            } else {
                                return input;
                            }
            default: return input;
        }
    }
}

