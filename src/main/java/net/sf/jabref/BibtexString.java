/*  Copyright (C) 2003-2014 JabRef contributors.
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
package net.sf.jabref;

/**
 * This class models a BibTex String ("@String")
 */
public class BibtexString {

    /**
     * Type of a \@String.
     *
     * Differentiate a \@String based on its usage:
     *
     * - {@link #AUTHOR}: prefix "a", for author and editor fields.
     * - {@link #INSTITUTION}: prefix "i", for institution and organization
     *                         field
     * - {@link #PUBLISHER}: prefix "p", for publisher fields
     * - {@link #OTHER}: no prefix, for any field
     *
     * Examples:
     *
     * \@String { aKahle    = "Kahle, Brewster " } -> author
     * \@String { aStallman = "Stallman, Richard" } -> author
     * \@String { iMIT      = "{Massachusetts Institute of Technology ({MIT})}" } -> institution
     * \@String { pMIT      = "{Massachusetts Institute of Technology ({MIT}) press}" } -> publisher
     * \@String { anct      = "Anecdote" } -> other
     * \@String { eg        = "for example" } -> other
     * \@String { et        = " and " } -> other
     * \@String { lBigMac   = "Big Mac" } -> other
     *
     * Usage:
     *
     * \@Misc {
     *   title       = "The GNU Project"
     *   author      = aStallman # et # aKahle
     *   institution = iMIT
     *   publisher   = pMIT
     *   note        = "Just " # eg
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
            if (!(name.charAt(1) + "").toUpperCase().equals(
                    (name.charAt(1) + ""))) {
                return OTHER;
            }
            for (Type t : Type.values()) {
                if (t.prefix.equals(name.charAt(0) + "")) {
                    return t;
                }
            }
            return OTHER;
        }
    }


    private String _name;
    private String _content;
    private String _id;
    private Type _type;


    public BibtexString(String id, String name, String content) {
        _id = id;
        _name = name;
        _content = content;
        _type = Type.get(name);
    }

    public BibtexString(String id, String name, String content, Type type) {
        _id = id;
        _name = name;
        _content = content;
        _type = type;
    }

    public String getId() {
        return _id;
    }

    public void setId(String id) {
        _id = id;
    }

    public String getName() {
        return _name;
    }

    public void setName(String name) {
        _name = name;
        _type = Type.get(name);
    }

    public String getContent() {
        return ((_content == null) ? "" : _content);
    }

    public void setContent(String content) {
        _content = content;
    }

    @Override
    public Object clone() {
        return new BibtexString(_id, _name, _content);
    }

    public Type getType() {
        return _type;
    }
}
