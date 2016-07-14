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
package net.sf.jabref.model.entry;

/**
 * This class models a BibTex String ("@String")
 */
public class BibtexString implements Cloneable {

    /**
     * Type of a \@String.
     * <p>
     * Differentiate a \@String based on its usage:
     * <p>
     * - {@link #AUTHOR}: prefix "a", for author and editor fields.
     * - {@link #INSTITUTION}: prefix "i", for institution and organization
     * field
     * - {@link #PUBLISHER}: prefix "p", for publisher fields
     * - {@link #OTHER}: no prefix, for any field
     * <p>
     * Examples:
     * <p>
     * \@String { aKahle    = "Kahle, Brewster " } -> author
     * \@String { aStallman = "Stallman, Richard" } -> author
     * \@String { iMIT      = "{Massachusetts Institute of Technology ({MIT})}" } -> institution
     * \@String { pMIT      = "{Massachusetts Institute of Technology ({MIT}) press}" } -> publisher
     * \@String { anct      = "Anecdote" } -> other
     * \@String { eg        = "for example" } -> other
     * \@String { et        = " and " } -> other
     * \@String { lBigMac   = "Big Mac" } -> other
     * <p>
     * Usage:
     * <p>
     * \@Misc {
     * title       = "The GNU Project"
     * author      = aStallman # et # aKahle
     * institution = iMIT
     * publisher   = pMIT
     * note        = "Just " # eg
     * }
     *
     * @author Jan Kubovy <jan@kubovy.eu>
     */
    public enum Type {
        AUTHOR("a"),
        INSTITUTION("i"),
        PUBLISHER("p"),
        OTHER("");

        private final String prefix;


        Type(String prefix) {
            this.prefix = prefix;
        }

        public static Type get(String name) {
            if (name.length() <= 1) {
                return OTHER;
            }
            // TODO: Figure out what the next check actually does and replace it with something more sensible
            // Second character is not upper case? What about non-letters?
            if (!(String.valueOf(name.charAt(1))).toUpperCase().equals(
                    String.valueOf(name.charAt(1)))) {
                return OTHER;
            }
            for (Type t : Type.values()) {
                if (t.prefix.equals(String.valueOf(name.charAt(0)))) {
                    return t;
                }
            }
            return OTHER;
        }
    }


    private String name;
    private String content;
    private String id;
    private Type type;
    private String parsedSerialization;
    private boolean hasChanged;


    public BibtexString(String id, String name, String content) {
        this.id = id;
        this.name = name;
        this.content = content;
        hasChanged = true;
        type = Type.get(name);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
        hasChanged = true;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        hasChanged = true;
        type = Type.get(name);
    }

    public String getContent() {
        return content == null ? "" : content;
    }

    public void setContent(String content) {
        this.content = content;
        hasChanged = true;
    }

    @Override
    public Object clone() {
        return new BibtexString(id, name, content);
    }

    public Type getType() {
        return type;
    }

    public void setParsedSerialization(String parsedSerialization) {
        this.parsedSerialization = parsedSerialization;
        hasChanged = false;
    }

    public String getParsedSerialization() {
        return parsedSerialization;
    }

    public boolean hasChanged(){
        return hasChanged;
    }

    /*
    * Returns user comments (arbitrary text before the string) if there are any. If not returns the empty string
     */
    public String getUserComments() {
        if(parsedSerialization != null) {

            try {
                // get the text before the string
                String prolog = parsedSerialization.substring(0, parsedSerialization.indexOf('@'));

                // delete trailing whitespaces (between string and text)
                prolog = prolog.replaceFirst("\\s+$", "");
                // if there is any non whitespace text, write it with proper line separation
                if (prolog.length() > 0) {
                    return prolog;
                }
            } catch(StringIndexOutOfBoundsException ignore) {
                // if this occurs a broken parsed serialization has been set, so just do nothing
            }
        }

        return "";
    }
}
