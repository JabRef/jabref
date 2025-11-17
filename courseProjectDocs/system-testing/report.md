# System Testing Report

---
# BasicModelStringIntegrationTest

## Test Scope and Coverage
These system tests focus on validating end-to-end behavior across JabRef’s **model layer** and **string utility layer** within the jablib module.
The goal is to verify that BibEntry field manipulation, combined with `StringUtil` transformations, performs correctly when exercised as a full workflow rather than in isolation.

## Features Covered
- Creation of `BibEntry` objects and assignment of metadata fields.
- Retrieval and transformation of field values through StringUtil.
- Correct handling of author and title fields.
- Basic string utilities that JabRef relies on when normalizing or formatting imported metadata:
    - `shaveString`
    - `capitalizeFirst`
    - `repeatSpaces`
    - `quoteForHTML`
    - `endsWithIgnoreCase`
    - `removeBracesAroundCapitals`
  - `isBlank`
  - `isInCurlyBrackets`
  - `boldHTML`

These tests confirm that JabRef’s internal representations and data transformations work together without throwing exceptions, producing incorrect results, or silently failing.
## Test Case Summary
### Table of System Test Cases

| Test Case Title                                | Pre-conditions                                                           | Test Steps                                                                                                                                                                                                                                                                                                     | Expected Results                                                                                                                                                                                                                                                                                                                                          |
|------------------------------------------------|--------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **TC-01: Author Field Processing**             | JabRef project builds successfully; `StringUtil` available in classpath. | 1. Create a new `BibEntry`. <br>2. Set the author field to `{john doe}`. <br>3. Retrieve the value. <br>4. Apply `shaveString`. <br>5. Apply `capitalizeFirst`. <br>6. Apply `repeatSpaces` and `quoteForHTML`.                                                                                                | - Field is stored exactly as provided. <br>- `shaveString` returns `john doe`. <br>- `capitalizeFirst("john")` returns `John`. <br>- `repeatSpaces(3)` returns three spaces. <br>- `quoteForHTML("!")` returns `&#33;`.                                                                                                                                   |
| **TC-02: Title Suffix and Brace Behavior**     | JabRef project builds successfully; StringUtil in classpath.             | 1. Create a `BibEntry`. <br>2. Set TITLE to `a sample title.TXT`. <br>3. Retrieve the value. <br>4. Apply `endsWithIgnoreCase`. <br>5. Apply `removeBracesAroundCapitals`.                                                                                                                                     | - Title value is stored unchanged. <br>- `endsWithIgnoreCase` returns `true` for `"txt"`. <br>- `removeBracesAroundCapitals` returns the original value because no braces are present.                                                                                                                                                                    |
| **TC-03: Year Field with HTML Transformation** | JabRef project builds successfully; StringUtil in classpath.             | 1. Create a `BibEntry`. <br>2. Set YEAR to `  2024  ` (with whitespace). <br>3. Retrieve the value. <br>4. Apply `isBlank` validation. <br>5. Trim whitespace. <br>6. Apply `isInCurlyBrackets` check. <br>7. Apply `boldHTML` transformation. <br>8. Test edge cases: empty strings, null, bracketed content. | - Year value stored with whitespace preserved. <br>- `isBlank` returns `false` for year with spaces. <br>- Trimmed value equals `"2024"`. <br>- `isInCurlyBrackets` returns `false` for plain text. <br>- `boldHTML` returns `<b>2024</b>`. <br>- `isBlank` returns `true` for empty/null strings. <br>- `isInCurlyBrackets` returns `true` for `{2024}`. |
| **TC-04: Verify Optional Field Volume** | JabRef project builds successfully | 1. Create BibEntry <br> 2. Set VOLME Field <br> 3. Create DataBase <br> 4. Append BibEntry to BibDatabase <br> 5. Return set Optional from BibEntry <br> 6. Compare actual to expected | BibEntry stores `VOL.1` for VOLUME StandardField <br> expected value is `VOL.`                                                                                                                                                                                                                                                                             | 
## Execution and Results
The system tests were executed using:

```bash
./gradlew :jablib:cleanTest :jablib:test --tests "org.jabref.system.BasicModelStringIntegrationTest"
```
### Execution Outcome
- Build Successful
- All test cases passed
- No exceptions, regressions, or formatting inconsistencies were observed.
- No unexpected mutations of BibEntry fields occurred.
- Findings for test cases "testOptionalFieldVolume" shows that Set returns a truncated version of "VOL. 1" hinting that the implementation of this function should be addressed.
---
## Group Contributions
| Member   | Contribution                                                                                                                                                                                         |
|----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Geoffrey | Implemented the system test (`BasicModelStringIntegrationTest`), identified invalid StringUtil methods, corrected test compilation errors, executed all tests, and generated this report and README. |
| Lucille  | Implemented the system test case `testYearFieldWithTransformation` in`BasicModelStringIntegrationTest`)                                                                                              |
| Vanessa | Implemented test case `InsertTestOptionalFields` which evaluates a volume being created properly in the StandardField input.                                                                         | 
