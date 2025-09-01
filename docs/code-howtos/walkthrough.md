---
parent: Code Howtos
---
# Walkthrough

Currently, all the walkthrough is written using the internal declarative API (see `org.jabref.gui.walkthrough.declarative`) and stored in the `org.jabref.gui.walkthrough.WalkthroughAction` class. Each walkthrough a linear series of steps, where each step is either a UI highlight (`org.jabref.gui.walkthrough.declarative.step.VisibleComponent`) or invisible sideeffect (e.g., opening an example library, `org.jabref.gui.walkthrough.declarative.step.SideEffect`).

# Quick Settings
