# Code checklist

This is a step-by-step checklist to ensure high-quality of your code.
Ensure that you did all steps before creating a PR.

- [ ] Use JSpecify instead of `== null`.
- [ ] org.jabref.logic.util.strings.StringUtil.isBlank(java.lang.String) instead of `== null || ...isBlank()`
- [ ] `docker run -v $pwd:/github/workspace ghcr.io/leventebajczi/intellij-format:master "*.java" "" ".idea/codeStyles/Project.xml"` executed to ensure proper formatting.
