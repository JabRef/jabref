JabRef at the moment has a few issues:

- Most of the code is untested, undocumented and contains a lot of bugs/issues.
- Over the lifetime of JabRef, a lot of features, UI elements and preferences were added. All of them are loosely fit together in the UI, but the UI lacks consistency and structure.
- This makes working on JabRef interesting as in every part of the program, one can improve something. :)

JabRef 3.x is the effort to try to fix a lot of these issues. But with the available man-power we have at the moment, it is not feasible to keep all the existing features and fix/rewrite them so that we can maintain them again. We currently use three approaches: a) rewrite and put under test to improve quality and fix bugs, b) replace preference options with fixed values to reduce the number of available options, and c) remove features that have never worked or are extremely rarely used.