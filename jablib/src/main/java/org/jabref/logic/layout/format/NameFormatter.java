package org.jabref.logic.layout.format;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jabref.logic.bst.util.BstNameFormatter;
import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.model.entry.AuthorList;

import org.jspecify.annotations.NonNull;

/// This layout formatter uses the BibTeX `name.format$` method and provides ultimate flexibility.
///
/// The formatter needs a parameter with the following format:
///
/// ```
/// <case1>@<range11>@"<format>"@<range12>@"<format>"@<range13>...@@
/// <case2>@<range21>@...
///```
///
/// Individual cases are separated by `@@` and items in a case by `@`.
///
/// Cases are just integers or the character `*` and tell the formatter to apply the following formats
/// if there are less or equal authors given.
/// The cases must be in strictly increasing order with `*` in the last position.
///
/// Example:
/// - `case1 = 2`
/// - `case2 = 3`
/// - `case3 = *`
///
/// Ranges are either `<integer>..<integer>`, `<integer>` or `*` (using a 1-based index).
/// Negative integers start from the end (`-1` = last author).
///
/// Example with `Joe Doe and Mary Jane and Bruce Bar and Arthur Kay`:
/// - `1..3` → Joe, Mary, Bruce
/// - `4..4` → Arthur
/// - `*` → all authors
/// - `2..-1` → Mary, Bruce, Arthur
///
/// The `<format>` uses the BibTeX formatter syntax:
/// - The letters `v`, `f`, `l`, `j` indicate name parts (von, first, last, jr). Use them inside `{}` for full form.
/// - A single letter (`v`, `f`, `l`, `j`) abbreviates the part.
/// - Quotes must be escaped as `\"` (not fully supported yet).
///
/// Example:
/// - `"{ll},{f}."` → `"Joe Doe"` becomes `"Doe, J."`
///
/// Complete example:
///
/// Input:
/// ```
/// Joe Doe and Mary Jane and Bruce Bar and Arthur Kay
///```
///
/// Output:
/// ```
/// Doe, J., Jane, M., Bar, B. and Kay, A.
///```
///
/// Formatter parameter:
/// ```
/// 1@*@{ll},{f}.@@2@1@{ll},{f}.@2@ and {ll},{f}.@@*@1..-3@{ll},{f}., @-2@{ll},{f}.@-1@ and {ll},{f}.
///```
///
/// This is troublesome to write, but it works.
/// For more examples see the test cases.
public class NameFormatter implements LayoutFormatter {

    public static final String DEFAULT_FORMAT = "1@*@{ff }{vv }{ll}{, jj}@@*@1@{ff }{vv }{ll}{, jj}@*@, {ff }{vv }{ll}{, jj}";

    private String parameter = NameFormatter.DEFAULT_FORMAT;

    private static String format(String toFormat, AuthorList al, String[] formats) {
        StringBuilder sb = new StringBuilder();

        int n = al.getNumberOfAuthors();

        for (int i = 1; i <= al.getNumberOfAuthors(); i++) {
            for (int j = 1; j < formats.length; j += 2) {
                if ("*".equals(formats[j])) {
                    sb.append(BstNameFormatter.formatName(toFormat, i, formats[j + 1]));
                    break;
                } else {
                    String[] range = formats[j].split("\\.\\.");

                    int s;
                    int e;
                    if (range.length == 2) {
                        s = Integer.parseInt(range[0]);
                        e = Integer.parseInt(range[1]);
                    } else {
                        s = e = Integer.parseInt(range[0]);
                    }
                    if (s < 0) {
                        s += n + 1;
                    }
                    if (e < 0) {
                        e += n + 1;
                    }
                    if (e < s) {
                        int temp = e;
                        e = s;
                        s = temp;
                    }

                    if ((s <= i) && (i <= e)) {
                        sb.append(BstNameFormatter.formatName(toFormat, i, formats[j + 1]));
                        break;
                    }
                }
            }
        }
        return sb.toString();
    }

    public String format(String toFormat, String inParameters) {
        AuthorList al = AuthorList.parse(toFormat);
        String parameters;

        if ((inParameters == null) || inParameters.isEmpty()) {
            parameters = "*:*:\"{ff}{vv}{ll}{,jj} \"";
        } else {
            parameters = inParameters;
        }

        String[] cases = parameters.split("@@");
        for (String aCase : cases) {
            String[] formatString = aCase.split("@");

            if (formatString.length < 3) {
                // Error
                return toFormat;
            }

            if ("*".equals(formatString[0])) {
                return format(toFormat, al, formatString);
            } else {
                if (al.getNumberOfAuthors() <= Integer.parseInt(formatString[0])) {
                    return format(toFormat, al, formatString);
                }
            }
        }
        return toFormat;
    }

    @Override
    public String format(String fieldText) {
        return format(fieldText, parameter);
    }

    public void setParameter(String parameter) {
        this.parameter = parameter;
    }

    public static Map<String, String> getNameFormatters(@NonNull NameFormatterPreferences preferences) {
        Map<String, String> result = new HashMap<>();

        List<String> names = preferences.getNameFormatterKey();
        List<String> formats = preferences.getNameFormatterValue();

        for (int i = 0; i < names.size(); i++) {
            if (i < formats.size()) {
                result.put(names.get(i), formats.get(i));
            } else {
                result.put(names.get(i), DEFAULT_FORMAT);
            }
        }

        return result;
    }
}
