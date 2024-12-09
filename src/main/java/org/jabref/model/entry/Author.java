package org.jabref.model.entry;

import java.util.Objects;
import java.util.Optional;

import org.jabref.architecture.AllowedToUseLogic;
import org.jabref.logic.formatter.bibtexfields.RemoveWordEnclosingAndOuterEnclosingBracesFormatter;
import org.jabref.model.strings.LatexToUnicodeAdapter;
import org.jabref.model.strings.StringUtil;

/**
 * This is an immutable class that keeps information regarding single author. It is just a container for the information, with very simple methods to access it.
 * <p>
 * Current usage: only methods <code>getLastOnly</code>, <code>getFirstLast</code>, and <code>getLastFirst</code> are used; all other methods are provided for completeness.
 */
@AllowedToUseLogic("because it needs to use formatter")
public class Author {

    /**
     * Object indicating the <code>others</code> author. This is a BibTeX feature mostly rendered in "et al." in LaTeX.
     * Example: <code>authors = {Oliver Kopp and others}</code>. This is then appearing as "Oliver Kopp et al.".
     * In the context of BibTeX key generation, this is "Kopp+" (<code>+</code> for "et al.") and not "KO".
     */
    public static final Author OTHERS = new Author("", "", null, "others", null);

    public static final RemoveWordEnclosingAndOuterEnclosingBracesFormatter FORMATTER = new RemoveWordEnclosingAndOuterEnclosingBracesFormatter();

    private final String givenName;
    private final String givenNameAbbreviated;
    private final String namePrefix;
    private final String familyName;
    private final String nameSuffix;
    private Author latexFreeAuthor;

    /**
     * Creates the Author object. If any part of the name is absent, <CODE>null</CODE> must be passed; otherwise other methods may return erroneous results.
     * <p>
     * In case only the last part is passed, enclosing braces are
     *
     * @param givenName     the first name of the author (may consist of several tokens, like "Charles Louis Xavier Joseph" in "Charles Louis Xavier Joseph de la Vall{\'e}e Poussin")
     * @param givenNameAbbreviated the abbreviated first name of the author (may consist of several tokens, like "C. L. X. J." in "Charles Louis Xavier Joseph de la Vall{\'e}e Poussin"). It is a responsibility of the caller to create a reasonable abbreviation of the first name.
     * @param namePrefix       the von part of the author's name (may consist of several tokens, like "de la" in "Charles Louis Xavier Joseph de la Vall{\'e}e Poussin")
     * @param familyName      the last name of the author (may consist of several tokens, like "Vall{\'e}e Poussin" in "Charles Louis Xavier Joseph de la Vall{\'e}e Poussin")
     * @param nameSuffix        the junior part of the author's name (may consist of several tokens, like "Jr. III" in "Smith, Jr. III, John")
     */
    public Author(String givenName, String givenNameAbbreviated, String namePrefix, String familyName, String nameSuffix) {
        boolean keepBracesAtLastPart = StringUtil.isBlank(givenName) && StringUtil.isBlank(givenNameAbbreviated) && StringUtil.isBlank(namePrefix) && !StringUtil.isBlank(familyName) && StringUtil.isBlank(nameSuffix);

        if (!StringUtil.isBlank(givenName)) {
            this.givenName = addDotIfAbbreviation(FORMATTER.format(givenName));
        } else {
            this.givenName = null;
        }
        if (!StringUtil.isBlank(givenNameAbbreviated)) {
            this.givenNameAbbreviated = FORMATTER.format(givenNameAbbreviated);
        } else {
            this.givenNameAbbreviated = null;
        }
        if (!StringUtil.isBlank(namePrefix)) {
            this.namePrefix = FORMATTER.format(namePrefix);
        } else {
            this.namePrefix = null;
        }
        if (keepBracesAtLastPart) {
            // We do not remove braces here to keep institutions protected
            // https://github.com/JabRef/jabref/issues/10031
            this.familyName = familyName;
        } else {
            if (!StringUtil.isBlank(familyName)) {
                this.familyName = FORMATTER.format(familyName);
            } else {
                this.familyName = null;
            }
        }
        if (!StringUtil.isBlank(nameSuffix)) {
            this.nameSuffix = FORMATTER.format(nameSuffix);
        } else {
            this.nameSuffix = null;
        }
    }

    public static String addDotIfAbbreviation(String name) {
        if ((name == null) || name.isEmpty()) {
            return name;
        }
        // If only one character (uppercase letter), add a dot and return immediately:
        if ((name.length() == 1) && Character.isLetter(name.charAt(0)) &&
                Character.isUpperCase(name.charAt(0))) {
            return name + ".";
        }

        StringBuilder sb = new StringBuilder();
        char lastChar = name.charAt(0);
        for (int i = 0; i < name.length(); i++) {
            if (i > 0) {
                lastChar = name.charAt(i - 1);
            }
            char currentChar = name.charAt(i);
            sb.append(currentChar);

            if (currentChar == '.') {
                // A.A. -> A. A.
                if (((i + 1) < name.length()) && Character.isUpperCase(name.charAt(i + 1))) {
                    sb.append(' ');
                }
            }

            boolean currentIsUppercaseLetter = Character.isLetter(currentChar) && Character.isUpperCase(currentChar);
            if (!currentIsUppercaseLetter) {
                // No uppercase letter, hence nothing to do
                continue;
            }

            boolean lastIsLowercaseLetter = Character.isLetter(lastChar) && Character.isLowerCase(lastChar);
            if (lastIsLowercaseLetter) {
                // previous character was lowercase (probably an acronym like JabRef) -> don't change anything
                continue;
            }

            if ((i + 1) >= name.length()) {
                // Current character is last character in input, so append dot
                sb.append('.');
                continue;
            }

            char nextChar = name.charAt(i + 1);
            if ('-' == nextChar) {
                // A-A -> A.-A.
                sb.append(".");
                continue;
            }
            if ('.' == nextChar) {
                // Dot already there, so nothing to do
                continue;
            }

            // AA -> A. A.
            // Only append ". " if the rest of the 'word' is uppercase
            boolean nextWordIsUppercase = true;
            char furtherChar = Character.MIN_VALUE;
            for (int j = i + 1; j < name.length(); j++) {
                furtherChar = name.charAt(j);
                if (Character.isWhitespace(furtherChar) || (furtherChar == '-') || (furtherChar == '~') || (furtherChar == '.')) {
                    // end of word
                    break;
                }

                boolean furtherIsUppercaseLetter = Character.isLetter(furtherChar) && Character.isUpperCase(furtherChar);
                if (!furtherIsUppercaseLetter) {
                    nextWordIsUppercase = false;
                    break;
                }
            }
            if (nextWordIsUppercase) {
                if (Character.isWhitespace(furtherChar)) {
                    sb.append(".");
                } else {
                    sb.append(". ");
                }
            }
        }

        return sb.toString().trim();
    }

    @Override
    public int hashCode() {
        return Objects.hash(givenNameAbbreviated, givenName, nameSuffix, familyName, namePrefix);
    }

    /**
     * Compare this object with the given one.
     *
     * @return `true` iff the other object is an Author and all fields are `Objects.equals`.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other instanceof Author that) {
            return Objects.equals(givenName, that.givenName)
                    && Objects.equals(givenNameAbbreviated, that.givenNameAbbreviated)
                    && Objects.equals(namePrefix, that.namePrefix)
                    && Objects.equals(familyName, that.familyName)
                    && Objects.equals(nameSuffix, that.nameSuffix);
        }

        return false;
    }

    /**
     * Returns the first name of the author stored in this object ("First").
     *
     * @return first name of the author (may consist of several tokens)
     */
    public Optional<String> getGivenName() {
        return Optional.ofNullable(givenName);
    }

    /**
     * Returns the abbreviated first name of the author stored in this object ("F.").
     *
     * @return abbreviated first name of the author (may consist of several tokens)
     */
    public Optional<String> getGivenNameAbbreviated() {
        return Optional.ofNullable(givenNameAbbreviated);
    }

    /**
     * Returns the von part of the author's name stored in this object ("von", "name prefix").
     *
     * @return von part of the author's name (may consist of several tokens)
     */
    public Optional<String> getNamePrefix() {
        return Optional.ofNullable(namePrefix);
    }

    /**
     * Returns the last name of the author stored in this object ("Last").
     *
     * @return last name of the author (may consist of several tokens)
     */
    public Optional<String> getFamilyName() {
        return Optional.ofNullable(familyName);
    }

    /**
     * Returns the name suffix ("junior") part of the author's name stored in this object ("Jr").
     *
     * @return junior part of the author's name (may consist of several tokens) or null if the author does not have a Jr. Part
     */
    public Optional<String> getNameSuffix() {
        return Optional.ofNullable(nameSuffix);
    }

    /**
     * Returns von-part followed by last name ("von Last"). If both fields were specified as <CODE>null</CODE>, the empty string <CODE>""</CODE> is returned.
     *
     * @return 'von Last'
     */
    public String getNamePrefixAndFamilyName() {
        if (namePrefix == null || namePrefix.isEmpty()) {
            return getFamilyName().orElse("");
        } else {
            return familyName == null ? namePrefix : namePrefix + ' ' + familyName;
        }
    }

    /**
     * Returns the author's name in form 'von Last, Jr., First' with the first name full or abbreviated depending on parameter.
     *
     * @param abbr <CODE>true</CODE> - abbreviate first name, <CODE>false</CODE> - do not abbreviate
     * @return 'von Last, Jr., First' (if <CODE>abbr==false</CODE>) or 'von Last, Jr., F.' (if <CODE>abbr==true</CODE>)
     */
    public String getFamilyGiven(boolean abbr) {
        StringBuilder res = new StringBuilder(getNamePrefixAndFamilyName());
        getNameSuffix().ifPresent(jr -> res.append(", ").append(jr));
        if (abbr) {
            getGivenNameAbbreviated().ifPresent(firstA -> res.append(", ").append(firstA));
        } else {
            getGivenName().ifPresent(first -> res.append(", ").append(first));
        }
        return res.toString();
    }

    /**
     * Returns the author's name in form 'First von Last, Jr.' with the first name full or abbreviated depending on parameter.
     *
     * @param abbr <CODE>true</CODE> - abbreviate first name, <CODE>false</CODE> - do not abbreviate
     * @return 'First von Last, Jr.' (if <CODE>abbr==false</CODE>) or 'F. von Last, Jr.' (if <CODE>abbr==true</CODE>)
     */
    public String getGivenFamily(boolean abbr) {
        StringBuilder res = new StringBuilder();
        if (abbr) {
            getGivenNameAbbreviated().map(firstA -> firstA + ' ').ifPresent(res::append);
        } else {
            getGivenName().map(first -> first + ' ').ifPresent(res::append);
        }
        res.append(getNamePrefixAndFamilyName());
        getNameSuffix().ifPresent(jr -> res.append(", ").append(jr));
        return res.toString();
    }

    @Override
    public String toString() {
        return "Author{" + "givenName='" + givenName + '\'' +
                ", givenNameAbbreviated='" + givenNameAbbreviated + '\'' +
                ", namePrefix='" + namePrefix + '\'' +
                ", familyName='" + familyName + '\'' +
                ", nameSuffix='" + nameSuffix + '\'' +
                '}';
    }

    /**
     * Returns the name as "Last, Jr, F." omitting the von-part and removing starting braces.
     *
     * @return "Last, Jr, F." as described above or "" if all these parts are empty.
     */
    public String getNameForAlphabetization() {
        StringBuilder res = new StringBuilder();
        getFamilyName().ifPresent(res::append);
        getNameSuffix().ifPresent(jr -> res.append(", ").append(jr));
        getGivenNameAbbreviated().ifPresent(firstA -> res.append(", ").append(firstA));
        while ((!res.isEmpty()) && (res.charAt(0) == '{')) {
            res.deleteCharAt(0);
        }
        return res.toString();
    }

    /**
     * Returns a LaTeX-free version of this `Author`.
     */
    public Author latexFree() {
        if (latexFreeAuthor == null) {
            String first = getGivenName().map(LatexToUnicodeAdapter::format).orElse(null);
            String givenNameAbbreviated = getGivenNameAbbreviated().map(LatexToUnicodeAdapter::format).orElse(null);
            String von = getNamePrefix().map(LatexToUnicodeAdapter::format).orElse(null);
            String last = getFamilyName().map(LatexToUnicodeAdapter::format).orElse(null);
            String jr = getNameSuffix().map(LatexToUnicodeAdapter::format).orElse(null);
            latexFreeAuthor = new Author(first, givenNameAbbreviated, von, last, jr);
            latexFreeAuthor.latexFreeAuthor = latexFreeAuthor;
        }
        return latexFreeAuthor;
    }
}
