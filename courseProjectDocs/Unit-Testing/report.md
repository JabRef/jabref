## To Run Tests

### New test cases
These test cases were added in order to assist with the lack of code coverage on certain branches: specifcally, the citationstyle and formatter modules.

In order to recreate the following: 
- Import assertThrows and assertNotEquals
- JabRefDataItemDataProvider.java
- FormatterTest.java

### Test Cases

| JabRefDataItemProvider | FormatterTest | 
| toJsonMultipleAuthorOneEntry  | getFormatTestWhiteSpace  |
| toJsonNoEntryType      | getHugeInputSize  |
|       -     |  testNonASCIICharacters  | 

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

## Test Results

Coverage improvement analysis (compare with Baseline)
