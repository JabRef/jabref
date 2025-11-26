## Using Regular Expressions in Search and Dynamic Groups

JabRef supports **regular expressions (RegEx)** when searching entries or when creating **dynamic groups**.  
This allows advanced matching of titles, authors, keywords, journals, and other fields.

JabRef uses the Java regular expression engine (`java.util.regex`).  
This means all standard Java/PCRE-style expressions are supported.

### Examples

| Goal | RegEx | Explanation |
|------|--------|-------------|
| Match words starting with “bio” | `^bio` | Matches *biology*, *biophysics*, etc. |
| Case-insensitive search | `(?i)keyword` | Adds case-insensitive flag. |
| Match whole word only | `\bterm\b` | Avoids matching “determine”. |
| Match entries ending with “2023” | `2023$` | `$` anchors at end of field. |
| Match anything in between | `Deep.*Learning` | Matches “Deep Reinforcement Learning”, “Deep Learning”, etc. |

### Notes and Tips

- You do **not** need quotes around the regex, even if it contains spaces.
- Some characters must be escaped: `(` `)` `[` `]` `{` `}` `?` `*` `+` `.` `|`
- If you want to search for a literal dot, use: `\.`  
- Use `.*` to match “any number of characters”.
- Tabs, newlines, and spaces are matched literally.
- RegEx works in:
  - **Global Search**
  - **Search Groups**
  - **Dynamic Groups**

### Dynamic Group Example

To create a dynamic group that contains all entries whose **title starts with "Deep Learning"**, use:

