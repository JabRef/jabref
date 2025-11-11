# Integration Testing Report

## Test Design Summary
### PdfImporter & ParserResult Integration Test:
This integration test verifies the interaction between the **PDF Importer** (`PdfImporter`) module and the **Parser Result** (`ParserResult`) module in the JabRef system.
The goal is to ensure that when a PDF file is imported, the extracted bibliographic data is correctly propagated into the internal database representation.

#### Modules Integrated
- `org.jabref.logic.importer.fileformat.pdf` — handles PDF import logic.
- `org.jabref.logic.importer` — manages parsing results and database contexts.

#### Test Scenario
A simulated PDF import was performed using a mocked `PdfImporter` to produce a bibliographic entry.  
The resulting `ParserResult` was validated to ensure correct data flow and entry creation.

#### Test Data Preparation
A dummy PDF path (`build/resources/test/pdfs/test.pdf`) was used. No actual parsing of a file occurs; the test simulates integration logic at the module interface level.

#### Execution
Executed using:
```bash
.\gradlew :jablib:test --tests "org.jabref.integration.PdfImporterIntegrationTest"
```

#### Results:
- Test passed successfully.
- Verified that ParserResult correctly stores the entry returned from PdfImporter.
- No defects were discovered.

#### Bug reports
No new defects were identified during integration testing.

---
### Integration Test:
Overview of integration test

#### Modules Integrated
which modules were integrated and what interactions were tested.

#### Test Scenario
describe how input/output data was generated or collected.

#### Test Data Preparation
describe how input/output data was generated or collected.

#### Execution
summarize test outcomes, including any discovered defects.
Executed using:
```bash
.\gradlew :jablib:test --tests "org.jabref.integration.PdfImporterIntegrationTest"
```

#### Results:
- No defects were discovered.

#### Bug reports (if any): link or describe issues identified through integration testing.
link or describe issues identified through integration testing.

---
### Integration Test:
Overview of integration test

#### Modules Integrated
which modules were integrated and what interactions were tested.

#### Test Scenario
describe how input/output data was generated or collected.

#### Test Data Preparation
describe how input/output data was generated or collected.

#### Execution
summarize test outcomes, including any discovered defects.
Executed using:
```bash
.\gradlew :jablib:test --tests "org.jabref.integration.PdfImporterIntegrationTest"
```

#### Results:
- No defects were discovered.

#### Bug reports (if any): link or describe issues identified through integration testing.
link or describe issues identified through integration testing.

---
## Group contributions
| Member   | Task/Contribution                                     | Notes                                                         |
|----------|-------------------------------------------------------|---------------------------------------------------------------|
| Geoffrey | Designed and implemented `PdfImporterIntegrationTest` | Verified data flow between importer and parser result modules |
| Vanessa  | X                                                     | X                                                             |
| Lucille  | X                                                     | X                                                             |
