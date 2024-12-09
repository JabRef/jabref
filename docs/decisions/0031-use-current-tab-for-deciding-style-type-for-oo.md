---
nav_order: 31
parent: Decision Records
---
# Use currently active tab in Select style (OO Panel) to decide style type

## Context and Problem Statement

In the Select Style Dialog window of the OpenOffice Panel, in case a style is selected in both the CSL Styles Tab and JStyles Tab, how to decide which of the two will be activated for use?

## Considered Options

* Use toggle in Select Style GUI
* Use toggle in Preferences
* Use Buttons in Select Style GUI
* Use Toggle in Main GUI
* Use currently active Tab in Select Style GUI and add a notification

## Decision Outcome

Chosen option: "Use currently active Tab in Select Style GUI and add a notification", because we already had two tabs indicating a clear separation of choices to the user. It was the most convenient way without adding extra steps to make the user choose "which style type to use" before selecting the style, which would be the case in the other options if chosen. The option is quite intuitive, extensible for working with multiple tabs and make only three to four clicks are necessary to select a style. Furthermore, the notification makes it clear to the user which style type as well as which style is selected.
