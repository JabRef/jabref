---
parent: Decision Records
nav_order: 66
---
# Use org.jfxcore:validation for field validation

## Context and Problem Statement

Field validation used ControlsFX's `Decorator` overlay (via mvvmfx-validation's `ControlsFxVisualizer`), which inserts an overlay node as a sibling of the decorated control and tracks its position via bounds listeners for every decorated control, regardless of validity. This was a suspected UI performance issue and is unsafe for controls that are direct `GridPane` children, since inserting a sibling node breaks `GridPane.rowIndex`/`columnIndex` attachments. Which validation library and decoration mechanism should replace it?

## Considered Options

* Keep ControlsFX's `Decorator`, only work around its `GridPane`/performance issues
* `org.jfxcore:validation`, with a JabRef-owned `Popup`-based decorator
* [ValidatorFX](https://github.com/effad/ValidatorFX), with its built-in `GraphicDecoration`

## Decision Outcome

Chosen option: "`org.jfxcore:validation`, with a JabRef-owned `Popup`-based decorator", because it replaces the always-on, sibling-node-inserting overlay with a `Popup` that never touches the control's parent and only tracks position while a message is actually showing. ValidatorFX's `GraphicDecoration` was checked briefly and rejected: it replaces the scene's root node with a wrapper `StackPane` (`scene.setRoot(stack)`) and tracks `stack.needsLayoutProperty()`, which fires on every layout pass anywhere in the scene — more invasive and broader-scoped than what we were trying to avoid.

### Consequences

* Good, because the decoration is safe for controls that are direct `GridPane` children.
* Good, because position tracking only runs while a control is invalid, not continuously for every decorated control.
* Bad, because `org.jfxcore:validation` is a young (0.1.0), single-maintainer library without an established community; the decorator (`org.jabref.gui.validation.ValidationVisualizer`) is new, JabRef-owned code rather than a port of a proven mechanism.

### More Information

Implementation PR: [#16174](https://github.com/JabRef/jabref/pull/16174)
