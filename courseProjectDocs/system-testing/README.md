# System Testing – Execution Guide

This document explains how to run the JabRef system tests and reproduce the results reported in `report.md`.

---
## How to Execute the System Tests

From the project root, run:

```bash
./gradlew :jablib:cleanTest :jablib:test --tests "org.jabref.system.BasicModelStringIntegrationTest"
./gradlew :jablib:test --tests "org.jabref.system.InsertTestOptionalFields"
```
This command ensures:
- All test classes under jablib test scope are compiled.
- Only the system test executes.
- Old test results are removed.

---
## Expected Output
A successful run should end with:
```nginx
BUILD SUCCESSFUL
```
To view detailed results:
`jablib/build/reports/tests/index.html`

