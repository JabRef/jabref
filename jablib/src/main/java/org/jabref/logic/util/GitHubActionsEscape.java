package org.jabref.logic.util;

/// Escapes values for the
/// [GitHub Actions workflow command](https://docs.github.com/en/actions/writing-workflows/choosing-what-your-workflow-does/workflow-commands-for-github-actions#example-creating-an-annotation-for-an-error)
/// format. Two escaping flavours are needed:
///
/// - {@link #data(String)} for the message body after `::`.
/// - {@link #property(String)} for `key=value` properties (additionally escapes `:` and `,`,
///   which is critical for Windows-style file paths such as `C:\foo\bar.bib`).
public final class GitHubActionsEscape {

    private GitHubActionsEscape() {
    }

    public static String data(String value) {
        return value.replace("%", "%25")
                    .replace("\r", "%0D")
                    .replace("\n", "%0A");
    }

    public static String property(String value) {
        return data(value)
                .replace(":", "%3A")
                .replace(",", "%2C");
    }
}
