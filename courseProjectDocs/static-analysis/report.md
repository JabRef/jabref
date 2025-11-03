# Static Analysis
---

## Tool Used:
SonarQube

---
## Key findings:
The SonarQube static analysis was run on the `jablib` module using the Gradle SonarQube plugin.  
The analysis detected several code quality issues categorized as **Code Smells** and **Maintainability** concerns.

---
## Fix summary
### Fixed Code Smell: Missing Default Clause in Switch Statement

**File:** `Formatters.java`  
**Issue Type:** Code Smell (Sonar Rule S131)  
**Description:**  
SonarQube reported that the `switch` statement in `getFormatterForModifier()` lacked a `default` case.  
Defensive programming best practices recommend including a default branch to handle unexpected values.
**Before:**
```java
public static Optional<Formatter> getFormatterForModifier(@NonNull String modifier) {
    switch (modifier) {
        case "lower":
            return Optional.of(new LowerCaseFormatter());
        case "upper":
            return Optional.of(new UpperCaseFormatter());
        case "capitalize":
            return Optional.of(new CapitalizeFormatter());
        case "titlecase":
            return Optional.of(new TitleCaseFormatter());
        case "sentencecase":
            return Optional.of(new SentenceCaseFormatter());
        case "veryshorttitle":
            return Optional.of(new VeryShortTitleFormatter());
        case "shorttitle":
            return Optional.of(new ShortTitleFormatter());
    }
}
```
**After:**
```java
public static Optional<Formatter> getFormatterForModifier(@NonNull String modifier) {
    switch (modifier) {
        case "lower":
            return Optional.of(new LowerCaseFormatter());
        case "upper":
            return Optional.of(new UpperCaseFormatter());
        case "capitalize":
            return Optional.of(new CapitalizeFormatter());
        case "titlecase":
            return Optional.of(new TitleCaseFormatter());
        case "sentencecase":
            return Optional.of(new SentenceCaseFormatter());
        case "veryshorttitle":
            return Optional.of(new VeryShortTitleFormatter());
        case "shorttitle":
            return Optional.of(new ShortTitleFormatter());
        default:
            // No matching explicit case found; handled below
            break;
    }
}
```
**Fix**  
Added a `default:` branch with a comment and `break;` statement to satisfy SonarQube rule S131.  
This provides defensive handling for future modifier types without altering functionality.

---
## Group Contributions

| Member | Task | Notes                          |
| -------- | ------- |--------------------------------|
| Lucille | tba | tba                            |
| Geoffrey | Fixed Code Smell: Missing Default Clause in Switch Statement | Added simple comment and break |
| Vanessa | Setup SonarQ configurations | In Config files (build.gradle) |


attempt to run by running

On Unix-like systems:
./gradlew build sonar
