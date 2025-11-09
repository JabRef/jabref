# Integration Testing
## PdfImporter & ParserResult Integration Testing
### Description
This integration test verifies the interaction between the `PdfImporter` module and the `ParserResult` / `BibEntry` data model.  
It ensures that the importer correctly creates and connects entries within the parsing result structure.

### How to Run the Tests
From the repository root, run the following command:
```bash
./gradlew :jablib:test --tests "org.jabref.integration.PdfImporterIntegrationTest"
```

### Viewing the Results
After running the tests, open the report generated at:
```
jablib/build/reports/tests/test/index.html
```
The report shows detailed results, including any passing or failing test cases.
---
