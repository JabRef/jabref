# Mocking & Stubbing Design Decisions for PdfImporter

This document summarizes the design decisions, new test cases, mocking strategy, and coverage improvement analysis for unit tests of `PdfImporter` and `PdfImporterMockTest`.

---

## 1. New Test Cases & Rationale

### PdfImporterMockTest.java

| Test Case | Description | Rationale |
|-----------|-------------|-----------|
| `testImportDatabase_handlesIOException` | Tests that `PdfImporter` properly handles an `IOException` during PDF parsing. | Ensures the importer gracefully reports errors when PDF reading fails. |
| `testImportDatabase_returnsEntries` | Tests that `PdfImporter` returns expected BibEntry objects. | Confirms that normal import behavior produces correct entries without errors. |

### PdfImporterTest.java

| Test Case | Description | Rationale |
|-----------|-------------|-----------|
| `testSuccessfulImportDatabase` | Verifies that a normal PDF import returns valid entries using a mocked `XmpUtilReader`. | Ensures that the importer works correctly when the underlying reader succeeds. |
| `testEncryptedPdfHandling` | Tests handling of encrypted PDFs by simulating `EncryptedPdfsNotSupportedException`. | Confirms that encrypted PDFs are correctly marked as invalid or produce warnings. |
| `testIOExceptionHandling` | Simulates an `IOException` when loading a PDF. | Ensures error conditions are correctly propagated in the `ParserResult`. |

---

## 2. Mocking Strategy

- **Mocked Dependencies**:
  - `XmpUtilReader`: Mocked to simulate PDF reading behavior, including exceptions and decryption.
  - `PDDocument`: Mocked to avoid actual file I/O.

- **Inline Anonymous Classes**:
  - `PdfImporterMockTest` overrides `importDatabase` to simulate different scenarios without depending on real files.

- **Mockito Usage**:
  - `when(...).thenReturn(...)` and `when(...).thenThrow(...)` are used to control behavior of mocks.
  - `mock()` is used to create `XmpUtilReader` and `PDDocument` instances for unit testing.

- **Advantages**:
  - Isolates `PdfImporter` behavior from the file system.
  - Allows testing error handling scenarios that are hard to reproduce with actual files.
  - Supports verification of edge cases like IO errors and encrypted PDFs.

---

## 3. Coverage Improvement Analysis

| Area | Previous Coverage | Added Coverage |
|------|-----------------|----------------|
| Normal PDF import | Partial | Full: `testImportDatabase_returnsEntries`, `testSuccessfulImportDatabase` |
| Exception handling | Minimal | Full: `testImportDatabase_handlesIOException`, `testIOExceptionHandling`, `testEncryptedPdfHandling` |
| Edge cases | None | Introduced explicit handling of encrypted and bad PDFs |
| Integration with XmpUtilReader | None | Mocked reader allows control over return values and exceptions |

**Summary**:  
The combination of `PdfImporterMockTest` and `PdfImporterTest` significantly improves unit test coverage by:
- Simulating normal, error, and edge-case scenarios.
- Avoiding dependency on external files.
- Ensuring that both expected entries and error reporting are validated.

---

## 4. Conclusion

The applied mocking strategy allows robust and isolated unit testing for `PdfImporter`. These tests ensure that the importer:
- Correctly parses valid PDFs.
- Gracefully handles I/O errors and encrypted files.
- Produces consistent `ParserResult` outcomes for all scenarios.
