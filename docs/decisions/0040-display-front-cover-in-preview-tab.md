---
parent: Decision Records
nav_order: 40
---

# Display front cover for book citations in the Preview tab

## Context and Problem Statement

* Users have requested that the front covers of book citations are displayed in JabRef.
* This is discussed on the [JabRef forum](https://discourse.jabref.org/t/display-cover-images-for-books/3647) and raised as a [feature request](https://github.com/JabRef/jabref/issues/10120).
* We need to decide where the book cover should be placed.

## Decision Drivers

* It should not be obtrusive or distracting since the main use of JabRef is for articles not books.
* It should not obstruct the view of existing GUI components, specifically the MainTable or the information in the EntryEditor's tabs.

## Considered Options

Place the book cover in:

1. the existing SidePane
2. a new SidePane
3. the Preview panel of the EntryEditor
4. a SplitPane next to the MainTable

## Decision Outcome

Chosen option: "3. The PreviewPanel of the EntryEditor".

## Pros and Cons of the Options

### 1. Existing SidePane

![Image: Placement in SidePane](https://github.com/user-attachments/assets/7f704b0c-6f0c-4501-8167-4dc6202ca8f6)

* Good, because it would be unobtrusive
* Bad, because it would crowd other panels in the SidePane
* Bad, because changing the size of the SidePane would [affect both the MainTable and the EntryEditor](https://github.com/user-attachments/assets/06a8d35b-1bbc-4066-b92b-44b908656cdf).

### 2. New right-sided SidePane

* Good, if integrated together with entry preview because it would make it [easier to view a citation's preview](https://github.com/JabRef/jabref/issues/10120#issuecomment-2422099269).
* Bad, because an extra SidePane would [make the interface overly complex](https://github.com/JabRef/jabref/issues/10120#issuecomment-2422677378).

### 3. The PreviewPanel of the EntryEditor

![Image: Placement in the Preview Panel](https://github.com/user-attachments/assets/68b9065b-bac6-412b-9815-7d27d2fbe0be)

* Good, because it would not be obtrusive or distracting.
* Bad, because users will have to click multiple times (enable preview tab, open entry editor) to see the book cover.

### 4. SplitPane next to the MainTable

![Image: Placement next to the Main Table](https://github.com/user-attachments/assets/7b193c60-7903-42ff-ad99-29c0f2d8f482)

* Good, because changing the size of this SplitPane would [only affect the MainTable](https://github.com/user-attachments/assets/4e458099-ca5c-41bc-a33b-ce4240d7df82).
* Bad, because it would obstruct some columns in the MainTable.
