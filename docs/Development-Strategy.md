JabRef at the beginning of 2016 had a few issues:

- Most of the code is untested, non-documented, and contains a lot of bugs and issues.
- During the lifetime of JabRef, a lot of features, UI elements and preferences have been added. All of them are loosely wired together in the UI, but the UI lacks consistency and structure.
- This makes working on JabRef interesting as in every part of the program, one can improve something. :smiley:

JabRef 3.x is the effort to try to fix a lot of these issues.
Much has been achieved, but much is still open.

We currently use two approaches:
a) rewrite and put under test to improve quality and fix bugs,
b) increase code quality. This leads to pull requests being reviewed by two JabRef developers to ensure i) code quality, ii) fit within the JabRef architecture, iii) high test coverage.

Code quality includes using latest Java8 features, but also readability.