# Functional Specification Document

## 1. Introduction

### 1.1 Purpose
<!-- Beschreibt den Zweck und den Leserkreis der Spezifikation. -->


### 1.2 Scope
<!-- Gibt an, wo die Software eingesetzt werden soll und welche wesentlichen Funktionen es haben wird. Wo sinnvoll, sollte auch definiert werden, was die Software nicht leisten wird.
Beschreibt die mit der Software verfolgten Ziele. -->


### 1.3 Definitions
<!-- Dokumentiert alle verwendeten Fachbegriffe und Abkürzungen. Alternativ können Sie auch ein separates Glossar nutzen. -->


### 1.4 References
<!-- Verzeichnet alle Dokumente, auf die in der Spezifikation verwiesen wird. -->


### 1.5 Overview
<!-- Beschreibt, wie der Rest der Spezifikation aufgebaut ist, insbesondere, wie Kapitel 3 strukturiert ist. -->


## 2. General Description

### 2.1 Implementation
<!-- Beschreibt, wie das System in seine Umgebung eingebettet ist und wie die Software mit den umgebenden Komponenten und Systemen zusammenspielt. Dazu werden die Schnittstellen, Kommunikationsprotokolle etc. definiert. -->


### 2.2 Function Overview
<!-- Skizziert die wichtigsten Funktionen -->


### 2.3 User Requirements
<!-- Charakterisiert die Benutzergruppen und die Voraussetzungen die diese jeweils mitbringen (Ausbildung, Know-how, Sprache) -->


### 2.4 Limitations
<!-- Dokumentiert Einschränkungen, die die Freiheit der Entwicklung reduzieren (Basis-Software, Ziel-Hardware, Gesetzliche Grundlagen, ...) -->


### 2.5 Assumptions and Dependencies
<!-- Nennt explizit die Annahmen und externen Voraussetzungen, von denen bei der Spezifikation ausgegangen wurde. -->


### 3. Functional Requirements
<!-- Beschreibt die Anforderung i so genau, dass bei der Verwendung der Spezifikation (im Entwurf usw.) keine Rückfragen dazu notwendig sind.
Identifizieren Sie jede Funktionale Anforderung mit einer Nummer, so dass diese Nachverfolgbar sind. Zusammengehörende Funktionale Anforderungen können durch geeignete Nummerierung angezeigt werden.
Zur Spezifikation der Software sollen Sprachschablonen benutzt werden.
* /F10/ Funktion 1 des Systems
* /F11/ Weitere Detaillierung Funkion 1
* /F20/ Funktion 2 des Systems
Die Funktionalen Anforderungen sollen mithilfe von Use-cases erhoben werden. Die Use-cases sollen in Anhang A detailliert beschrieben werden. -->

- /F60/ Recent Search
- /F61/ Clickable item in global searchbar.
- /F62/ Once clicked, opens dropdown menu.
- /F63/ Dropdown menu contains a list of last search queries.
- /F64/ Selecting a querry performs a search with the selected querry.
- /F64/ Selecting a querry is possible by left clicking on it or by navigating with the up/down keys and then selecting with enter.


### 4. Acceptance Criteria
<!-- Beschreiben Sie hier, wie die Anforderungen bei der Abnahme auf ihre Realisierung überprüft werden können.
Definieren Sie hier mindestens ein Abnahmekriterium
* /A10/ Abnahmekriterium 1
* /A20/ Abnahmekriterium 2 -->
  

# Appendix

## Appendix A. Use-cases
<!-- An dieser Stelle können detaillierte Use-cases angegeben werden
[Diagram](../../slides/images/use-case.png) -->

### Use Case 1:
<!-- * Name: *Name des Use-cases*
* Akteure: *Akteur1, Akteur2, ...*
* Vorbedingungen: *Was muss vor Beginn des Ablaufs gelten*
* Standardablauf
    * Schritt 1
    * Schritt 2
* Nachbedingungen Erfolg: *Was muss nach dem Ende des erfolgreichen Ablaufs gelten*
* Nachbedingung Sonderfall: *Was gilt nach dem Ende, wenn der Ablauf fehlgeschlagen ist* -->

### Use Case 2:
* Name: Make a search
* Stakeholders: Users of JabRef
* Preconditions: JabRef must be started
* Procedure:
  * Click on the global search bar.
  * Choose the fields you would like to search in (i.e. author, title etc.).
  * Click on author field (button or text) in the interface.
  * Type in the author "Beat Schmutz" into the global search bar and press ENTER.
  * Click on the OR field in the interface.
  * Click on the title field in the interface.
  * Type in "Morphological analysis" and press ENTER.
  * Click on an entry match in the main library pane.
* Postconditions: 
  * After selection of the author field an author field shows up in the global search bar. The cursor will be positioned to the right of this field. 
  * After typing in the author and selecting the OR field the position of the cursor is to the right again. 
  * After selecting the title field the cursor position changes to the right of this field once again. Typing in the title and pressing ENTER starts the search query which will be parsed with Lucene and passed on to the complex search query. The matched entries will show up in the main library pane. After clicking on an entry Metadata will show up; "Beat Schmutz" and "Morphological analysis" will be highlighted. 
* Postconditions exception: The search finds no matching entries in the library. in this case the library pane stays empty or a pop-up could suggest that no entries were found.

## Appendix A. Open Questions
- AND/OR buttons?
- 