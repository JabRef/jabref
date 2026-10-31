package org.jabref.model.entry.identifier;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

import org.jspecify.annotations.NonNull;

public class ISBN implements Identifier {

    private static final Pattern ISBN_PATTERN = Pattern.compile("^(\\d{9}[\\dxX]|\\d{13})$");

    /// Between two digits: any Unicode dash or soft hyphen (optionally followed by whitespace, because
    /// PDF text often wraps lines inside an ISBN), or a single space. May also be absent.
    private static final String SEPARATOR = "(?:[\\p{Pd}\\u00AD]\\s{0,2}|[ \\u00A0])?";

    /// An optional short note in parentheses, such as " (paperback)".
    private static final String QUALIFIER = "(?:\\s?\\([^)\\r\\n]{1,20}\\))?";

    /// The digits of an ISBN-13 (always starts with 978 or 979), without checks on the surrounding text.
    private static final String ISBN_13_NUMBER = "97[89](?:" + SEPARATOR + "\\d){10}";

    /// The digits of an ISBN-10 (the last character may be an X), without checks on the surrounding text.
    private static final String ISBN_10_NUMBER = "\\d(?:" + SEPARATOR + "\\d){8}" + SEPARATOR + "[\\dXx]";

    /// Pass 1: an ISBN-13 anywhere in the text.
    /// The lookbehind and lookahead keep us from cutting a candidate out of a longer number.
    private static final Pattern ISBN_13 = Pattern.compile("(?<!\\d)" + ISBN_13_NUMBER + "(?![\\dXx])");

    /// Pass 2: the label that starts a list, such as "ISBN", "ISBN-10:", "ISBN-13:" or "ISBN (print):".
    /// The match ends where the first number of the list starts.
    /// The "10" or "13" must not be followed by a digit, otherwise it could be the first digits of an
    /// unhyphenated number, as in "ISBN 1305116550".
    private static final Pattern ISBN_LABEL = Pattern.compile("(?i:ISBN)(?:[-\\s]?1[03](?!\\d))?" + QUALIFIER + ":?\\s{0,3}");

    /// Pass 2: one number of a list, either an ISBN-13 or an ISBN-10.
    /// Used with `lookingAt()` on a region, so it needs transparent bounds for its lookbehind and lookahead.
    private static final Pattern LIST_NUMBER = Pattern.compile(
            "(?<!\\d)(?:(?<isbn13>" + ISBN_13_NUMBER + ")|(?<isbn10>" + ISBN_10_NUMBER + "))(?![\\dXx])");

    /// Pass 2: what joins two numbers of a list, a comma, semicolon, slash, "and" or "or",
    /// as in "ISBN 0-596-52068-9 (print), 0-306-40615-2 (ebook)".
    private static final Pattern LIST_SEPARATOR = Pattern.compile(QUALIFIER + "(?:\\s*[,;/]\\s*|\\s+(?i:and|or)\\s+)");

    private static final Pattern NOT_ISBN_CHARACTER = Pattern.compile("[^0-9Xx]");

    private final String isbnString;

    public ISBN(@NonNull String isbnString) {
        this.isbnString = isbnString.trim().replace("-", "");
    }

    public static Optional<ISBN> parse(String input) {
        ISBN isbn = new ISBN(input);
        if (isbn.isValid()) {
            return Optional.of(isbn);
        } else {
            return Optional.empty();
        }
    }

    /// Finds valid ISBNs in free text, such as the text of a book's copyright page.
    ///
    /// ISBN-13s are returned before ISBN-10s, each group in order of appearance, without duplicates.
    /// An ISBN-10 and the ISBN-13 of the same book are returned as two separate entries.
    ///
    /// An ISBN-13 needs no label, as it always starts with 978 or 979.
    ///
    /// An ISBN-10 is only recognised directly after an ISBN label ("ISBN", "ISBN-10", "ISBN 10:", ...),
    /// optionally followed by a short note in parentheses, such as "ISBN (paperback):".
    /// Further ISBN-10s in a list directly after a recognised ISBN ("ISBN 0-596-52068-9, 0-306-40615-2")
    /// are also found. Other ISBN-10s are ignored, because an arbitrary 10-digit number passes the
    /// checksum about one time in eleven.
    ///
    /// The text is scanned in two passes:
    /// 1. every ISBN-13 anywhere in the text,
    /// 2. every label, followed by the list of numbers that starts right after it.
    public static List<ISBN> findAllInText(String text) {
        // Key = compact form, so "978-0-596-52068-7" and "9780596520687" count as duplicates
        Map<String, ISBN> isbn13s = new LinkedHashMap<>();
        Map<String, ISBN> isbn10s = new LinkedHashMap<>();

        Matcher isbn13Matcher = ISBN_13.matcher(text);
        while (isbn13Matcher.find()) {
            addIfValid(isbn13Matcher.group(), isbn13s);
        }

        Matcher labelMatcher = ISBN_LABEL.matcher(text);
        while (labelMatcher.find()) {
            readList(text, labelMatcher.end(), isbn10s);
        }

        List<ISBN> result = new ArrayList<>(isbn13s.size() + isbn10s.size());
        result.addAll(isbn13s.values());
        result.addAll(isbn10s.values());
        return result;
    }

    /// Reads the list of numbers that starts at `start` and adds the valid ISBN-10s to `isbn10s`.
    ///
    /// ISBN-13s in the list are skipped, as pass 1 has already found them, but they do not end the list.
    /// A number with an invalid checksum is skipped as well, so a typo does not hide the next ISBN.
    /// The list ends at the first position where no number or no separator follows.
    private static void readList(String text, int start, Map<String, ISBN> isbn10s) {
        // Transparent bounds, so the lookbehind and lookahead see the text outside of the region
        Matcher number = LIST_NUMBER.matcher(text).useTransparentBounds(true);
        Matcher separator = LIST_SEPARATOR.matcher(text).useTransparentBounds(true);

        int position = start;
        while (number.region(position, text.length()).lookingAt()) {
            String isbn10 = number.group("isbn10");
            if (isbn10 != null) {
                addIfValid(isbn10, isbn10s);
            }
            if (!separator.region(number.end(), text.length()).lookingAt()) {
                return;
            }
            position = separator.end();
        }
    }

    private static void addIfValid(String number, Map<String, ISBN> isbns) {
        String compact = NOT_ISBN_CHARACTER.matcher(number)
                                           .replaceAll("")
                                           .toUpperCase(Locale.ROOT);
        ISBN isbn = new ISBN(compact);
        if (isbn.isValid()) {
            isbns.putIfAbsent(compact, isbn);
        }
    }

    public boolean isValidFormat() {
        Matcher isbnMatcher = ISBN_PATTERN.matcher(isbnString);
        return isbnMatcher.matches();
    }

    public boolean isValidChecksum() {
        boolean valid;
        if (isbnString.length() == 10) {
            valid = isbn10check();
        } else {
            // length is either 10 or 13 based on regexp so will be 13 here
            valid = isbn13check();
        }
        return valid;
    }

    public boolean isIsbn10() {
        return isbn10check();
    }

    public boolean isIsbn13() {
        return isbn13check();
    }

    // Check that the control digit is correct, see e.g. https://en.wikipedia.org/wiki/International_Standard_Book_Number#Check_digits
    private boolean isbn10check() {
        if (isbnString.length() != 10) {
            return false;
        }

        int sum = 0;
        for (int pos = 0; pos <= 8; pos++) {
            sum += (isbnString.charAt(pos) - '0') * (10 - pos);
        }
        char control = isbnString.charAt(9);
        if ((control == 'x') || (control == 'X')) {
            control = '9' + 1;
        }
        sum += control - '0';
        return (sum % 11) == 0;
    }

    // Check that the control digit is correct, see e.g. https://en.wikipedia.org/wiki/International_Standard_Book_Number#Check_digits
    private boolean isbn13check() {
        if (isbnString.length() != 13) {
            return false;
        }

        int sum = 0;
        for (int pos = 0; pos <= 12; pos++) {
            sum += (isbnString.charAt(pos) - '0') * ((pos % 2) == 0 ? 1 : 3);
        }
        return (sum % 10) == 0;
    }

    public boolean isValid() {
        return isValidFormat() && isValidChecksum();
    }

    @Override
    public Field getDefaultField() {
        return StandardField.ISBN;
    }

    @Override
    public String asString() {
        return isbnString;
    }

    @Override
    public Optional<URI> getExternalURI() {
        try {
            return Optional.of(new URI("https://www.worldcat.org/isbn/" + isbnString));
        } catch (URISyntaxException _) {
            return Optional.empty();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }
        ISBN other = (ISBN) o;
        return isbnString.equalsIgnoreCase(other.isbnString);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isbnString.toLowerCase(Locale.ENGLISH));
    }
}
