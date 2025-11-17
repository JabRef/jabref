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

These tests confirm that JabRef’s internal representations and data transformations work together without throwing exceptions, producing incorrect results, or silently failing.
## Test Case Summary
### Table of System Test Cases

| Test Case Title | Pre-conditions | Test Steps | Expected Results |
|-----------------|----------------|------------|------------------|
| **TC-01: Author Field Processing** | JabRef project builds successfully; `StringUtil` available in classpath. | 1. Create a new `BibEntry`. <br>2. Set the author field to `{john doe}`. <br>3. Retrieve the value. <br>4. Apply `shaveString`. <br>5. Apply `capitalizeFirst`. <br>6. Apply `repeatSpaces` and `quoteForHTML`. | - Field is stored exactly as provided. <br>- `shaveString` returns `john doe`. <br>- `capitalizeFirst("john")` returns `John`. <br>- `repeatSpaces(3)` returns three spaces. <br>- `quoteForHTML("!")` returns `&#33;`. |
| **TC-02: Title Suffix and Brace Behavior** | JabRef project builds successfully; StringUtil in classpath. | 1. Create a `BibEntry`. <br>2. Set TITLE to `a sample title.TXT`. <br>3. Retrieve the value. <br>4. Apply `endsWithIgnoreCase`. <br>5. Apply `removeBracesAroundCapitals`. | - Title value is stored unchanged. <br>- `endsWithIgnoreCase` returns `true` for `"txt"`. <br>- `removeBracesAroundCapitals` returns the original value because no braces are present. |
## Execution and Results
The system tests were executed using:

```bash
./gradlew :jablib:cleanTest :jablib:test --tests "org.jabref.system.BasicModelStringIntegrationTest"
```
## File "InsertTestOptionalFields"

## Test Case:

| Test Case Title | Pre-conditions | Test Steps | Expected Results          |
|-----------------|----------------|------------|---------------------------|
| testOptionalFieldVolume | N/A | SetUp, Asserts, Initalize BibEntry, DataBase | VOL. does not equal VOL.1 |
### Expected Output:
"VOL. 1" for the VOLUME FIELD of the GUI interface.
The Set FieldsWithWords returns "VOL." truncating the next word ("1"). This demonstrates that setFieldWithWords() must be improvered to add ability to have space in words as the other getField() does it correctly.
### Actual Output:
"VOL."

```bash
./gradlew :jabgui:test --tests "org.jabref.gui.system.GUIInsertTestOptionalFields"
```

### Execution Outcome
- Build Successful
- All test cases passed
- No exceptions, regressions, or formatting inconsistencies were observed.
- No unexpected mutations of BibEntry fields occurred.

---
## Group Contributions
| Member   | Contribution                                                                                                                                                                                         |
|----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Geoffrey | Implemented the system test (`BasicModelStringIntegrationTest`), identified invalid StringUtil methods, corrected test compilation errors, executed all tests, and generated this report and README. |
| Vanessa | Implemented test ('InsertOptionalFields')  Findings, Observed error of output from input.                                                                                                            |
