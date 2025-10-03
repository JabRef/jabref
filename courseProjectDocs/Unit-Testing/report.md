## To Run Tests

### New test cases
These test cases were added in order to assist with the lack of code coverage on certain branches: specifically, the citationstyle and formatter modules.

In order to recreate the following: 
- Import assertThrows and assertNotEquals
- JabRefDataItemDataProvider.java
- FormatterTest.java

### Test Cases

JabRefDataItemProvider             | FormatterTest
----------------------------------|---------------------------
| toJsonMultipleAuthorOneEntry       | getFormatTestWhiteSpace |
| toJsonNoEntryType                  | getHugeInputSize |
| -                                 | testNonASCIICharacters |


## To replicate JabRefDataItemProvider: 
- **toJsonMultipleAuthorOneEntry**
  - Create BibDatabase
  - iniatlize entry with 'and' between two authors
  - assertEquals
- **toJsonNoEntryType**
  - Create BibDatabase
  - iniatlize entry, excepted value will contain a field that does not exist in the regular
  - assertNotEquals

## To replicate FormatterTest: 
-  **testNonASCIICharacters**
  - assertNotEquals(" ", result) 
- **getHugeInputSize**
  - Initalize a large input then assertNotNull(result)
- **getFormatTestWhiteSpace**
  - assertNotThrow non-ascii
**Rationale**:
- The Multiple author one entry test was created as the current tests lacked testing of multiple authors in a single entry, only one author per entry (even two authories one entry)
- The no entry type checks that a field that does not match the result field from the jabref object.
- testNonASCIICharacters, they test plain text so far, so added a test to check different unicodes.
- There is no performance tests so far, so added an input one.
- No one tested if the user simply enters a white space, no need to compute power...

## Test Results
![image](test_results.png)
Coverage improvement analysis (compare with Baseline)


## StringUtilTests Added

| Test name | Description / rationale |
|-----------|----------------------------|
| normalizeAuthorName_trimsAndReorders | Test that author names with “Last, First” normalize correctly |
| normalizeAuthorName_handlesSingleName | Ensure single-name strings are left intact |
| normalizeAuthorName_nullOrEmpty_returnsEmpty | Edge / null handling—no NullPointerException |
| generateSafeFilename_replacesIllegalChars | FileNameUtil must sanitize illegal file system characters |
| mergeBibEntries_overwritesOrKeepsNonEmpty | Merging logic should override or fill fields correctly |
| mergeBibEntries_nullOverride_throws | Robustness: null override should cause exception |

## Test Results
![StringUtil Tests Results](StringUtil_results.png)