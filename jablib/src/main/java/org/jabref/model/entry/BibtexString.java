package org.jabref.model.entry;

import java.util.Locale;
import java.util.Objects;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// This class models a BibTex String ("@String")
@NullMarked
public class BibtexString implements Cloneable {

    private static final Logger LOGGER = LoggerFactory.getLogger(BibtexString.class);

    /// Type of \@String.
    ///
    /// Differentiate a \@String based on its usage:
    ///
    /// - [#AUTHOR]: prefix "a", for author and editor fields.
    /// - [#INSTITUTION: prefix "i", for institution and organization field
    /// - [#PUBLISHER]: prefix "p", for publisher fields
    /// - [#OTHER]: no prefix, for any field
    ///
    /// Examples:
    ///
    /// \@String { aKahle    = "Kahle, Brewster " } -> author
    /// \@String { aStallman = "Stallman, Richard" } -> author
    /// \@String { iMIT      = "{Massachusetts Institute of Technology ({MIT})}" } -> institution
    /// \@String { pMIT      = "{Massachusetts Institute of Technology ({MIT}) press}" } -> publisher
    /// \@String { anct      = "Anecdote" } -> other
    /// \@String { eg        = "for example" } -> other
    /// \@String { et        = " and " } -> other
    /// \@String { lBigMac   = "Big Mac" } -> other
    ///
    /// Usage:
    ///
    /// ```bibtex
    /// @Misc {
    ///   title       = "The GNU Project"
    ///   author      = aStallman # et # aKahle
    ///   institution = iMIT
    ///   publisher   = pMIT
    ///   note        = "Just " # eg
    /// }
    /// ```
    public enum Type {
        AUTHOR("a"),
        INSTITUTION("i"),
        PUBLISHER("p"),
        OTHER("");

        private final String prefix;

        Type(String prefix) {
            this.prefix = prefix;
        }

        public static Type get(String key) {
            if (key.length() <= 1) {
                return OTHER;
            }

            // Second character is not upper case
            // aStallman -> AUTHOR
            // asdf -> OTHER
            if (!String.valueOf(key.charAt(1)).toUpperCase(Locale.ROOT).equals(String.valueOf(key.charAt(1)))) {
                return OTHER;
            }
            for (Type t : Type.values()) {
                if (t.prefix.equals(String.valueOf(key.charAt(0)))) {
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

    /// Default constructor. Use this if in doubt.
    ///
    /// In case this constructor is used - and the library is eventually written, the serialization is generated from scratch (and not some null from parsedSerialization)
    public BibtexString(String name, String content) {
        this.id = IdGenerator.next();
        this.name = name;
        this.content = content;
        this.parsedSerialization = "";
        hasChanged = true;
        type = Type.get(name);
    }

    /// This is used to set the parsed serialization of the string. This is used when the string is read from a BibTeX file.
    /// Do not use if not working with reading BibTeX files (or similar actions).     *
    ///
    /// @param parsedSerialization The serialization read during parsing
    public BibtexString(String name, String content, String parsedSerialization) {
        this(name, content);
        this.parsedSerialization = parsedSerialization;
        hasChanged = false;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
        hasChanged = true;
    }

    /// Returns the name/label of the string
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        hasChanged = true;
        type = Type.get(name);
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
        hasChanged = true;
    }

    public Type getType() {
        return type;
    }

    public String getParsedSerialization() {
        return parsedSerialization;
    }

    public boolean hasChanged() {
        return hasChanged;
    }

    /*
     * Returns user comments (arbitrary text before the string) if there are any. If not returns the empty string
     */
    public String getUserComments() {
        if (!parsedSerialization.isEmpty()) {
            try {
                // get the text before the string
                return parsedSerialization.substring(0, parsedSerialization.indexOf('@'));
            } catch (StringIndexOutOfBoundsException e) {
                // if this occurs a broken parsed serialization has been set, so just do nothing.
                LOGGER.error("Got an unexpected error, ignoring", e);
            }
        }
        return "";
    }

    @Override
    public Object clone() {
        BibtexString clone;
        if (parsedSerialization.isEmpty()) {
            clone = new BibtexString(name, content);
        } else {
            clone = new BibtexString(name, content, parsedSerialization);
        }
        clone.setId(id);
        return clone;
    }

    @Override
    public String toString() {
        return name + "=" + content;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }
        BibtexString that = (BibtexString) o;
        return Objects.equals(hasChanged, that.hasChanged) &&
                Objects.equals(name, that.name) &&
                Objects.equals(content, that.content) &&
                Objects.equals(type, that.type) &&
                Objects.equals(parsedSerialization, that.parsedSerialization);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hasChanged, name, content, type, parsedSerialization);
    }
}
